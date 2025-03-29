package com.example.test_application_for_elder_project

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.YuvImage
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.Image
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.test_application_for_elder_project.databinding.ActivityWebrtcSetupoBinding
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.framework.image.MPImage
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.core.OutputHandler
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarker
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult
import java.io.ByteArrayOutputStream
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.absoluteValue

class test_googlemedia_pipe : AppCompatActivity() {

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
        enableEdgeToEdge()
        setContentView(R.layout.activity_test_googlemedia_pipe)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupHandLandmarker()
        setupDrawingCanvas()
        startCamera()


        cameraExecutor = Executors.newSingleThreadExecutor()

    }

    private fun setupFirebase() {
        databaseRef = FirebaseDatabase.getInstance().getReference("googlemediapipe_game")
    }

    private fun setupHandLandmarker() {
        surfaceView = findViewById<SurfaceView>(R.id.surfaceView)
        previewView = findViewById(R.id.previewView)

        try {
            val baseOptions =
                BaseOptions.builder().setModelAssetPath("hand_landmarker.task").build()
            val options = HandLandmarker.HandLandmarkerOptions.builder()
                .setBaseOptions(baseOptions)
                .setRunningMode(RunningMode.LIVE_STREAM)
                .setNumHands(1)
                .setMinHandDetectionConfidence(0.5f)
                .setMinHandPresenceConfidence(0.5f)
                .setMinTrackingConfidence(0.5f)
                .setResultListener{ result, _ ->
                Log.d("HandDetection", "Hand detection executed!")

                if (result.landmarks().isNotEmpty()) {
                    Log.d("HandDetection", "Hand detected! Total landmarks: ${result.landmarks().size}")

                    val handLandmarks = result.landmarks()[0] // First detected hand
                    val indexFingerTip = handLandmarks[8] // Index Finger Tip
                    runOnUiThread {
                        val holder = surfaceView.holder
                        if (!holder.surface.isValid) {var a = 0;}

                        var canvas: Canvas? = null
                        try {
                            canvas = holder.lockCanvas()
                            if (canvas == null)

                                if (canvas != null) {
                                    canvas.drawColor(Color.RED)
                                }


                            runOnUiThread {
                                val finalX = indexFingerTip.x().absoluteValue * surfaceView.width
                                val finalY = indexFingerTip.y().absoluteValue * surfaceView.height

                                if (finalX in 0f..surfaceView.width.toFloat() && finalY in 0f..surfaceView.height.toFloat()) {
                                    if (lastX != -1f && lastY != -1f) {
                                        path.lineTo(finalX, finalY)
                                    } else {
                                        path.moveTo(finalX, finalY)
                                    }
                                    lastX = finalX
                                    lastY = finalY

                                    // Draw circle at landmark for debugging
                                    canvas.drawCircle(200f, 200f, 10f, paint);
                                } else {
                                    Log.e("DrawDebug", "Landmark coordinates out of bounds")
                                }

                                canvas.drawPath(path, paint)

                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        } finally {
                            // Always unlock canvas in finally block
                            if (canvas != null) {
                                holder.unlockCanvasAndPost(canvas)
                            }
                        }
                    }
                    Log.d("FingerCoords", "Index Finger Tip: (${indexFingerTip.x()}, ${indexFingerTip.y()})")

                } else {
                    Log.e("HandDetection", "No hand detected!")
                }
            }
                .build()







            handLandmarker = HandLandmarker.createFromOptions(this, options)

            Log.d("innitialize", "HandLandmarker initialized successfully")
        }
        catch (e:Exception){
            Log.e("HandLandmarker", "Failed to initialize HandLandmarker", e)
        }

        //
        //
        //  Toast.makeText(this, "handlandmarker", Toast.LENGTH_LONG).show()

    }

    var resultlistener = OutputHandler.ResultListener<HandLandmarkerResult, MPImage> { result, _ ->
        Log.d("HandLandmarker", "Result received. Landmarks count: ${result.landmarks().size}")
        if (result.landmarks().isEmpty()) {
            Log.e("HandLandmarker", "No landmarks detected in this frame")
        }

        val holder = surfaceView.holder
        if (!holder.surface.isValid || !surfaceView.isShown) {
            Log.d("DrawDebug", "Surface is invalid or not visible, skipping draw")
            return@ResultListener
        }

        val canvas = holder.lockCanvas() ?: return@ResultListener
        canvas.drawColor(Color.WHITE)

        if (result.landmarks().isNotEmpty()) {
            val landmark = result.landmarks()[0][8]
            val x = landmark.x() * surfaceView.width
            val y = landmark.y() * surfaceView.height
            Log.d("DrawDebug", "Landmark position: x=$x, y=$y")
            // Rest of your drawing logic...
        } else {
            Log.e("DrawDebug", "No hand landmarks detected")
            path.reset()
            lastX = -1f
            lastY = -1f
        }
        canvas.drawPath(path, paint)
        holder.unlockCanvasAndPost(canvas)
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
        //  Toast.makeText(this, "setupdrawingcanvas", Toast.LENGTH_LONG).show()
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
                    it.setAnalyzer(cameraExecutor, ImageAnalysis.Analyzer { imageproxy ->
                        processImageProxy(imageproxy)
                    })
                }

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalyzer)
            } catch (exc: Exception) {
                Log.e("CameraX", "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(this))
    }


    private fun processImageProxy(imageProxy: ImageProxy) {
        try {
            val bitmap = imageProxyToBitmap(imageProxy)

            if (bitmap == null) {
                Log.e("ImageDebug", "Failed to convert ImageProxy to Bitmap")
                return
            } else {
                Log.d("ImageDebug", "Bitmap conversion successful, size: ${bitmap.width}x${bitmap.height}")
            }

            Log.d("ImageDebug", "Rotation degrees: ${imageProxy.imageInfo.rotationDegrees}")

            val matrix = Matrix().apply {
                postRotate(imageProxy.imageInfo.rotationDegrees.toFloat()) // Rotate as needed
                postScale(-1f, 1f, bitmap.width / 2f, bitmap.height / 2f) // Mirror image
            }

            val finalBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            val MPimage = BitmapImageBuilder(finalBitmap).build()

            Log.d("ImageDebug", "Processed Image for Hand Detection")

            val timestampMs = System.currentTimeMillis()
            Log.d("ImageDebug", "Bitmap for MPImage: ${finalBitmap.width}x${finalBitmap.height}, config: ${finalBitmap.config}")
            handLandmarker.detectAsync(MPimage, timestampMs)


        } catch (e: Exception) {
            Log.e("ImageProcessing", "Error processing image", e)
        } finally {
            imageProxy.close()
        }
    }


    private fun imageProxyToBitmap(imageProxy: ImageProxy): Bitmap? {
        val image = imageProxy.image ?: return null

        val yBuffer = image.planes[0].buffer
        val uBuffer = image.planes[1].buffer
        val vBuffer = image.planes[2].buffer

        val ySize = yBuffer.remaining()
        val uSize = uBuffer.remaining()
        val vSize = vBuffer.remaining()

        val nv21 = ByteArray(ySize + uSize + vSize)

        yBuffer.get(nv21, 0, ySize)

        // Swap U and V bytes (some devices store them in reverse order)
        vBuffer.get(nv21, ySize, vSize)
        uBuffer.get(nv21, ySize + vSize, uSize)

        val yuvImage = YuvImage(nv21, ImageFormat.NV21, image.width, image.height, null)
        val out = ByteArrayOutputStream()
        yuvImage.compressToJpeg(Rect(0, 0, image.width, image.height), 100, out)

        val imageBytes = out.toByteArray()
        val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)

        // Convert to ARGB_8888 for better processing
        val rgbBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)

        // Rotate image based on sensor rotation (adjust degrees if needed)
        val rotatedBitmap = rotateBitmap(rgbBitmap, imageProxy.imageInfo.rotationDegrees.toFloat())

        // Resize to 224x224 (or required size for ML model)
        return Bitmap.createScaledBitmap(rotatedBitmap, 224, 224, true)
    }

    // Rotate the bitmap to correct orientation
    private fun rotateBitmap(source: Bitmap, rotationDegrees: Float): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(rotationDegrees)
        return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
    }


    private fun drawForCurrentUser(results: HandLandmarkerResult) {
        //   showtext("detect_async")
        val holder = surfaceView.holder
        if (!holder.surface.isValid) return

        val canvas = holder.lockCanvas() ?: return
        canvas.drawColor(Color.WHITE)

        if (results.landmarks().isNotEmpty()) {
            val landmark = results.landmarks()[0][8] // Index 8 is the tip of the index finger

            val x = landmark.x() * surfaceView.width
            val y = landmark.y() * surfaceView.height

            Log.d("DrawDebug", "Landmark position: x=$x, y=$y")

            if (x in 0f..surfaceView.width.toFloat() && y in 0f..surfaceView.height.toFloat()) {
                if (lastX != -1f && lastY != -1f) {
                    path.lineTo(x, y)
                } else {
                    path.moveTo(x, y)
                }
                lastX = x
                lastY = y

                // Draw circle at landmark for debugging
                canvas.drawCircle(x, y, 10f, paint)
            } else {
                Log.e("DrawDebug", "Landmark coordinates out of bounds")
            }
        } else {
            Log.e("DrawDebug", "No hand landmarks detected")
            path.reset() // Clear path when no landmarks are detected
            lastX = -1f
            lastY = -1f
        }

        canvas.drawPath(path, paint)
        holder.unlockCanvasAndPost(canvas)
    }



    public fun showtext(given_text:String) {
        Toast.makeText(this, "$given_text", Toast.LENGTH_SHORT).show()
    }


    // get rotation degrees



    // better for the camera2 api but since our program uses camerax not needed as of now
    fun getCameraId(context: Context, isFrontCamera: Boolean): String? {
        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        for (cameraId in cameraManager.cameraIdList) {
            val characteristics = cameraManager.getCameraCharacteristics(cameraId)
            val lensFacing = characteristics.get(CameraCharacteristics.LENS_FACING)

            if (isFrontCamera && lensFacing == CameraCharacteristics.LENS_FACING_FRONT) {
                return cameraId  // Return front camera ID
            } else if (!isFrontCamera && lensFacing == CameraCharacteristics.LENS_FACING_BACK) {
                return cameraId  // Return back camera ID
            }
        }
        return null // Return null if no camera is found
    }


    public fun calculate_degress(){
        var cameramanager = this.getSystemService(CAMERA_SERVICE) as CameraManager
        var properties = getCameraId(this,true)?.let { cameramanager.getCameraCharacteristics(it) }
        var orientation = properties?.get(CameraCharacteristics.SENSOR_ORIENTATION)

        var windowmanager = this.getSystemService(WINDOW_SERVICE) as WindowManager
        var rotation = windowmanager.defaultDisplay.rotation

        val surfaceRotationDegrees = when (rotation) {
            Surface.ROTATION_0 -> 0     // Portrait (default)
            Surface.ROTATION_90 -> 270  // Landscape Left
            Surface.ROTATION_180 -> 180 // Upside-down Portrait
            Surface.ROTATION_270 -> 90  // Landscape Right
            else -> 0
        }
    }

     private fun just(x: Float, y: Float) {

    }
}