package com.example.test_application_for_elder_project

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import com.example.test_application_for_elder_project.R
import com.example.test_application_for_elder_project.databinding.ActivityWebrtcSetupoBinding
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarker
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class googlemediapipe : AppCompatActivity() {
    private lateinit var surfaceView: SurfaceView
    private lateinit var previewView: PreviewView
    private lateinit var handLandmarker: HandLandmarker
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var databaseRef: DatabaseReference
    private var path = Path()
    private var lastX = -1f
    private var lastY = -1f
    private var turn = true
    private var isGameRunning = true
    private var gameTimer: CountDownTimer? = null

    private lateinit var binding: ActivityWebrtcSetupoBinding
    private val paint = Paint().apply {
        color = Color.RED
        style = Paint.Style.STROKE
        strokeWidth = 10f
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWebrtcSetupoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        surfaceView = findViewById(R.id.surfaceView)
        previewView = findViewById(R.id.previewView)

        setupFirebase()
        setupHandLandmarker()
        setupDrawingCanvas()
        startCamera()
        setupStopButton()

        cameraExecutor = Executors.newSingleThreadExecutor()
        startGame()
    }

    private fun setupFirebase() {
        databaseRef = FirebaseDatabase.getInstance().getReference("googlemediapipe_game")
    }

    private fun setupHandLandmarker() {
        val baseOptions = BaseOptions.builder().setModelAssetPath("hand_landmarker.task").build()
        val options = HandLandmarker.HandLandmarkerOptions.builder()
            .setBaseOptions(baseOptions)
            .setRunningMode(RunningMode.LIVE_STREAM)
            .setNumHands(1)
            .setMinHandDetectionConfidence(0.5f)
            .setMinHandPresenceConfidence(0.5f)
            .setMinTrackingConfidence(0.5f)
            .setResultListener { result, _ ->
                runOnUiThread {
                    if (turn && isGameRunning) {
                        drawForCurrentUser(result)
                    }
                }
            }
            .build()

        handLandmarker = HandLandmarker.createFromOptions(this, options)
    }

    private fun setupDrawingCanvas() {
        surfaceView.holder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {
                val canvas = holder.lockCanvas()
                canvas?.drawColor(Color.WHITE)
                holder.unlockCanvasAndPost(canvas)
            }

            override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {}
            override fun surfaceDestroyed(holder: SurfaceHolder) {}
        })
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            val imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor) { imageProxy ->
                        processImageProxy(imageProxy)
                    }
                }

            val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalyzer)
            } catch (exc: Exception) {
                Log.e("CameraX", "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(this))
    }

    private fun processImageProxy(imageProxy: ImageProxy) {
        val bitmap = imageProxy.toBitmap() ?: return
        processFrame(bitmap)
        imageProxy.close()
    }

    private fun processFrame(bitmap: Bitmap) {
        val mediaPipeImage = BitmapImageBuilder(bitmap).build()
        val timestampMs = System.currentTimeMillis()
        handLandmarker.detectAsync(mediaPipeImage, timestampMs)
    }

    private fun drawForCurrentUser(results: HandLandmarkerResult) {
        val holder = surfaceView.holder
        if (!holder.surface.isValid) return

        val canvas = holder.lockCanvas() ?: return
        canvas.drawColor(Color.WHITE)

        if (results.landmarks().isNotEmpty()) {
            val landmark = results.landmarks()[0][8]
            val x = landmark.x() * surfaceView.width
            val y = landmark.y() * surfaceView.height

            if (lastX != -1f && lastY != -1f) {
                path.lineTo(x, y)
                sendCoordinatesToFirebase(x, y)
            } else {
                path.moveTo(x, y)
            }

            lastX = x
            lastY = y
        }

        canvas.drawPath(path, paint)
        holder.unlockCanvasAndPost(canvas)
    }

    private fun sendCoordinatesToFirebase(x: Float, y: Float) {
        val coordinates = mapOf("x" to x, "y" to y)
        databaseRef.child("coordinates").push().setValue(coordinates)
    }

    private fun startGame() {
        val words = arrayOf("cat", "dog", "bat", "tap", "happy")
        binding.word.text = words.random()

        gameTimer = object : CountDownTimer(60000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                binding.timer.text = "Time Left: ${millisUntilFinished / 1000}s"
            }

            override fun onFinish() {
                swapTurn()
            }
        }.start()
    }

    private fun swapTurn() {
        turn = !turn
        binding.word.visibility = if (turn) View.VISIBLE else View.GONE
        startGame()
    }

    private fun setupStopButton() {
        binding.btnHangup.setOnClickListener {
            stopGame()
        }
    }

    private fun stopGame() {
        isGameRunning = false
        gameTimer?.cancel()
        cameraExecutor.shutdown()
        databaseRef.child("coordinates").removeValue()
    }

    private fun ImageProxy.toBitmap(): Bitmap? {
        val buffer = planes[0].buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }

    override fun onDestroy() {
        super.onDestroy()
        stopGame()
    }
}
