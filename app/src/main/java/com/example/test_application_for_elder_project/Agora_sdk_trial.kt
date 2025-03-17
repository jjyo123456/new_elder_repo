package com.example.test_application_for_elder_project

import android.Manifest
import android.content.pm.PackageManager
import android.os.*
import android.view.SurfaceView
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.firestore.BuildConfig
import io.agora.rtc2.*
import io.agora.rtc2.video.VideoCanvas
import kotlinx.coroutines.*

class AgoraSDKTrial : AppCompatActivity() {

    private val appid = "79b9b18c15b9430e9456b048a58e46dc"
    private val token = "007eJxTYPh/jJn/h+cBAcko9ehDZsuFn85cXXrdPeFUxqFonR9rzxYpMJhbJlkmGVokG5omWZoYG6RampiaJRmYWCSaWqSamKUkv3h8Pb0hkJGhYcUpZkYGCATx+RjKU5OKSpLjkzMS8/JScxgYAOQuJS4="
    private val channelName = "webrtc_channel"

    private var mRtcEngine: RtcEngine? = null
    private var localSurfaceView: SurfaceView? = null
    private var remoteSurfaceView: SurfaceView? = null

    private val uiHandler = Handler(Looper.getMainLooper())
    private val scope = CoroutineScope(Dispatchers.IO)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_agora_sdk_trial)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        if (checkPermissions()) {
            initializeAgora()
        } else {
            requestPermissions()
        }
    }

    private fun checkPermissions(): Boolean {
        return listOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA)
            .all { ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED }
    }

    private fun requestPermissions() {
        if (shouldShowRequestPermissionRationale(Manifest.permission.RECORD_AUDIO) ||
            shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)
        ) {
            AlertDialog.Builder(this)
                .setTitle("Permissions Required")
                .setMessage("This app requires Camera and Audio permissions for video calls.")
                .setPositiveButton("Grant") { _, _ ->
                    ActivityCompat.requestPermissions(
                        this,
                        arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA),
                        22
                    )
                }
                .setNegativeButton("Cancel") { _, _ ->
                    showToast("Permissions denied. Cannot start video call.")
                }
                .show()
        } else {
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA), 22
            )
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 22 && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
            initializeAgora()
        } else {
            showToast("Permissions denied. Cannot start video call.")
        }
    }

    private fun initializeAgora() {
        scope.launch {
            try {
                val config = RtcEngineConfig().apply {
                    mContext = applicationContext
                    mAppId = appid
                    mEventHandler = mRtcEventHandler
                }
                mRtcEngine = RtcEngine.create(config)
                joinChannel()
            } catch (e: Exception) {
                uiHandler.post { showToast("Error initializing RTC: ${e.message}") }
            }
        }
    }

    private fun joinChannel() {
        scope.launch {
            try {
                val options = ChannelMediaOptions().apply {
                    clientRoleType = Constants.CLIENT_ROLE_BROADCASTER
                    channelProfile = Constants.CHANNEL_PROFILE_COMMUNICATION
                    publishMicrophoneTrack = true
                    publishCameraTrack = true
                }

                mRtcEngine?.joinChannel(token, channelName, 0, options)
                uiHandler.post { setupLocalVideo() }
            } catch (e: Exception) {
                uiHandler.post { showToast("Failed to join channel: ${e.message}") }
            }
        }
    }

    private val mRtcEventHandler = object : IRtcEngineEventHandler() {
        override fun onJoinChannelSuccess(channel: String?, uid: Int, elapsed: Int) {
            uiHandler.post { showToast("Joined channel: $channel") }
        }

        override fun onUserJoined(uid: Int, elapsed: Int) {
            uiHandler.post { setupRemoteVideo(uid) }
        }

        override fun onUserOffline(uid: Int, reason: Int) {
            uiHandler.post { showToast("User offline: $uid") }
        }
    }

    private fun setupLocalVideo() {
        val container: FrameLayout = findViewById(R.id.local_video_view_container)
        localSurfaceView = SurfaceView(baseContext).also { container.addView(it) }
        mRtcEngine?.setupLocalVideo(VideoCanvas(localSurfaceView, VideoCanvas.RENDER_MODE_FIT, 0))
    }

    private fun setupRemoteVideo(uid: Int) {
        val container = findViewById<FrameLayout>(R.id.remote_video_view_container)
        remoteSurfaceView = SurfaceView(baseContext).apply { setZOrderMediaOverlay(true) }
        container.addView(remoteSurfaceView)
        mRtcEngine?.setupRemoteVideo(VideoCanvas(remoteSurfaceView, VideoCanvas.RENDER_MODE_FIT, uid))
    }

    private fun showToast(message: String) {
        Toast.makeText(this@AgoraSDKTrial, message, Toast.LENGTH_LONG).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.launch {
            mRtcEngine?.leaveChannel()
            RtcEngine.destroy()
            mRtcEngine = null
        }
        scope.cancel()  // Prevent memory leaks
    }
}
