package com.example.test_application_for_elder_project

import android.content.Context
import android.content.pm.PackageManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.SurfaceView
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.test_application_for_elder_project.databinding.ActivityWebrtcSetupoBinding
import com.example.test_application_for_elder_project.databinding.MatchedUserProfileLayoutBinding
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import org.webrtc.Camera1Enumerator
import org.webrtc.Camera2Enumerator
import org.webrtc.CameraVideoCapturer
import org.webrtc.DataChannel
import org.webrtc.DefaultVideoDecoderFactory
import org.webrtc.DefaultVideoEncoderFactory
import org.webrtc.EglBase
import org.webrtc.IceCandidate
import org.webrtc.MediaConstraints
import org.webrtc.MediaStream
import org.webrtc.PeerConnection
import org.webrtc.PeerConnectionFactory
import org.webrtc.RendererCommon
import org.webrtc.RtpReceiver
import org.webrtc.SdpObserver
import org.webrtc.SessionDescription
import org.webrtc.SurfaceEglRenderer
import org.webrtc.SurfaceTextureHelper
import org.webrtc.SurfaceViewRenderer
import org.webrtc.VideoCapturer
import org.webrtc.VideoTrack
import java.util.UUID
import java.util.concurrent.Executors


val database = FirebaseDatabase.getInstance()



var url:String = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?"

public lateinit var binding: ActivityWebrtcSetupoBinding

public lateinit var matchedUserProfileLayoutBinding: MatchedUserProfileLayoutBinding



private lateinit var peerConnectionFactory: PeerConnectionFactory
private lateinit var peerConnection: PeerConnection
private lateinit var localVideoTrack: VideoTrack
private lateinit var remoteVideoTrack: VideoTrack
private var videoCapturer: VideoCapturer? =null


lateinit var roomid_global_varriable:String
private val CAMERA_PERMISSION_CODE = 101

class Webrtc_setupo : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_webrtc_setupo)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding = ActivityWebrtcSetupoBinding.inflate(layoutInflater)

        matchedUserProfileLayoutBinding = MatchedUserProfileLayoutBinding.inflate(layoutInflater)

        matchedUserProfileLayoutBinding.mainVideoCallButton.visibility = View.GONE



        joincall()

        CoroutineScope(Dispatchers.Default).launch {
            checkAndRequestPermissions()
        }




            // binding.button5.setOnClickListener({
       //     matching()
       // })


    }

    // Handle permission result
    private fun checkAndRequestPermissions() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.CAMERA),
                CAMERA_PERMISSION_CODE
            )
        } else {
            setupWebRTC(applicationContext)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {


                setupWebRTC(this)

            } else {
                Toast.makeText(this, "Camera permission is required for video call", Toast.LENGTH_SHORT).show()
            }
        }
    }



    // webrtc section


    private fun setupWebRTC(context: Context) {
        val eglBase = EglBase.createEgl14(EglBase.CONFIG_PLAIN)
        val eglBaseContext: EglBase.Context = eglBase.eglBaseContext

        Executors.newSingleThreadExecutor().execute {
            PeerConnectionFactory.initialize(
                PeerConnectionFactory.InitializationOptions.builder(applicationContext)
                    .createInitializationOptions()
            )

            peerConnectionFactory = PeerConnectionFactory.builder()
                .setVideoDecoderFactory(DefaultVideoDecoderFactory(eglBaseContext))
                .setVideoEncoderFactory(DefaultVideoEncoderFactory(eglBaseContext, true, true))
                .createPeerConnectionFactory()

            val iceServers = listOf(
                PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer()
            )
            val rtcConfig = PeerConnection.RTCConfiguration(iceServers)

            peerConnection = peerConnectionFactory.createPeerConnection(rtcConfig, object : PeerConnection.Observer {
                override fun onSignalingChange(state: PeerConnection.SignalingState?) {}

                override fun onIceConnectionChange(state: PeerConnection.IceConnectionState?) {
                    Log.d("WebRTC", "ICE Connection State: $state")
                }

                override fun onIceConnectionReceivingChange(receiving: Boolean) {}

                override fun onIceGatheringChange(state: PeerConnection.IceGatheringState?) {}

                override fun onIceCandidate(candidate: IceCandidate?) {
                    candidate?.let {
                        Log.d("WebRTC", "New ICE Candidate: ${it.sdp}")
                        send_ice_candidate_to_firebase(it)
                    }
                }

                override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>?) {
                    candidates?.let { peerConnection?.removeIceCandidates(it) }
                }

                override fun onAddStream(stream: MediaStream?) {
                    Log.d("WebRTC", "Media Stream Added")
                }

                override fun onRemoveStream(stream: MediaStream?) {}

                override fun onAddTrack(receiver: RtpReceiver?, mediaStreams: Array<out MediaStream>?) {}

                override fun onDataChannel(dataChannel: DataChannel?) {
                    dataChannel?.registerObserver(object : DataChannel.Observer {
                        override fun onMessage(buffer: DataChannel.Buffer?) {
                            buffer?.let {
                                val data = ByteArray(it.data.remaining())
                                it.data.get(data)
                                Log.d("WebRTC", "DataChannel message: ${String(data)}")
                            }
                        }
                        override fun onStateChange() {
                            Log.d("WebRTC", "DataChannel state changed: ${dataChannel.state()}")
                        }
                        override fun onBufferedAmountChange(previousAmount: Long) {}
                    })
                }

                override fun onRenegotiationNeeded() {}
            })!!

            Handler(Looper.getMainLooper()).post {
                setupLocalMedia(context, eglBaseContext)
            }
        }
    }

    private fun setupLocalMedia(context: Context, eglBaseContext: EglBase.Context) {
        val localview = findViewById<SurfaceViewRenderer>(R.id.local_video_view)
        val remoteview = findViewById<SurfaceViewRenderer>(R.id.remote_video_view)

        if (localview == null || remoteview == null) {
            Log.e("WebRTC", "Video views not found!")
            return
        }

        // ✅ Release any previous video capturer before starting a new one
        releaseVideoCapturer()

        // ✅ Use same EGL context for both views
        localview.init(eglBaseContext, null)
        localview.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT)
        localview.setMirror(true)
        localview.setEnableHardwareScaler(true)

        remoteview.init(eglBaseContext, null)
        remoteview.setMirror(false)
        remoteview.setEnableHardwareScaler(true)

        // ✅ Ensure video capturer is initialized properly
        videoCapturer = video_capture_function() ?: run {
            Log.e("WebRTC", "Video capturer initialization failed!")
            return
        }

        val surfaceTextureHelper = SurfaceTextureHelper.create("CaptureThread", eglBaseContext)
        val videoSource = peerConnectionFactory.createVideoSource(videoCapturer!!.isScreencast())
        videoCapturer!!.initialize(surfaceTextureHelper, applicationContext, videoSource.capturerObserver)

        val videoTrack = peerConnectionFactory.createVideoTrack("local_track", videoSource)
        videoTrack.setEnabled(true)
        videoTrack.addSink(localview)

        try {
            videoCapturer!!.startCapture(720, 480, 30)
        } catch (e: Exception) {
            Log.e("WebRTC", "Error starting video capture: ${e.message}")
        }

        val audioSource = peerConnectionFactory.createAudioSource(MediaConstraints())
        val audioTrack = peerConnectionFactory.createAudioTrack("local_audio", audioSource)
        audioTrack.setEnabled(true)

        val localMediaStream = peerConnectionFactory.createLocalMediaStream("localStream")
        localMediaStream.addTrack(videoTrack)
        localMediaStream.addTrack(audioTrack)

        peerConnection.addStream(localMediaStream)
    }

    // ✅ Properly release the previous video capturer
    private fun releaseVideoCapturer() {
        try {
            videoCapturer?.let {
                it.stopCapture()
                it.dispose()
            }
            videoCapturer = null
        } catch (e: Exception) {
            Log.e("WebRTC", "Error releasing video capturer: ${e.message}")
        }
    }

    // ✅ Check if Camera is already in use
    fun isCameraInUse(): Boolean {
        val cameraManager = applicationContext.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        return try {
            for (id in cameraManager.cameraIdList) {
                val state = cameraManager.getCameraCharacteristics(id)
                    .get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL)
                if (state != null) return true
            }
            false
        } catch (e: Exception) {
            Log.e("WebRTC", "Camera check failed: ${e.message}")
            false
        }
    }

    // ✅ Properly stop video capture when activity is destroyed
    override fun onDestroy() {
        super.onDestroy()
        try {
            videoCapturer?.stopCapture()
            videoCapturer?.dispose()
        } catch (e: Exception) {
            Log.e("WebRTC", "Error stopping capture: ${e.message}")
        }
        peerConnection?.dispose()
        peerConnectionFactory?.dispose()
    }



    private fun video_capture_function(): CameraVideoCapturer {
        val enumerator = if (Camera2Enumerator.isSupported(applicationContext)) {
            Camera2Enumerator(applicationContext)
        } else {
            Camera1Enumerator(true)  // Use Camera1 if Camera2 isn't supported
        }

        val eventsHandler = object : CameraVideoCapturer.CameraEventsHandler {
            override fun onCameraError(error: String?) { Log.e("WebRTC", "Camera Error: $error") }
            override fun onCameraDisconnected() { Log.e("WebRTC", "Camera Disconnected") }
            override fun onCameraFreezed(error: String?) { Log.e("WebRTC", "Camera Freeze: $error") }
            override fun onCameraOpening(cameraName: String?) { Log.d("WebRTC", "Camera Opening: $cameraName") }
            override fun onFirstFrameAvailable() { Log.d("WebRTC", "First Frame Available") }
            override fun onCameraClosed() { Log.d("WebRTC", "Camera Closed") }
        }

        for (deviceName in enumerator.deviceNames) {
            if (enumerator.isFrontFacing(deviceName)) {
                return enumerator.createCapturer(deviceName, eventsHandler)
            }
        }
        throw IllegalStateException("No Front Camera Found!")
    }








    public fun joincall(){

        getroomidforuser(UserManager.current_userId.toString()) { roomid ->
            if(roomid != null){
                roomid_global_varriable = roomid

                showExecutionStep(applicationContext,roomid)

                showExecutionStep(applicationContext,"1")
                CoroutineScope(Dispatchers.IO).launch {

                    setupFirebaseListener(roomid)
                }
            }

        }
    }



    public fun getroomidforuser(userid: String, callback: (String) -> Unit) {
        val roomId: String = UUID.randomUUID().toString()
        val dbref = database.reference.child("matched_users")
        dbref.child(userid).push().setValue(roomId).addOnSuccessListener {
            showExecutionStep(this,"2")
            callback(roomId)
        }

    }








    public fun setupFirebaseListener(roomId: String) {
        val dbRef = database.reference.child("calls").child(roomId)

        // ✅ Listen for Offer
        dbRef.child("offer").child(UserManager.current_userId.toString())
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        Log.d("FirebaseDebug", "Offer exists, setting remote description")

                        val offerData = snapshot.value as? Map<String, String>
                        offerData?.let {
                            val offer = SessionDescription(
                                SessionDescription.Type.fromCanonicalForm(it["type"] ?: ""),
                                it["description"] ?: ""
                            )
                            peerConnection.setRemoteDescription(SdpObserverImpl(peerConnection, false), offer)
                            createAnswer(roomId)  // Create answer after setting offer
                        }
                    } else {
                        Log.e("FirebaseDebug", "Offer does NOT exist, creating offer")
                        createOffer(roomId)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("FirebaseDebug", "Firebase error: ${error.message}")
                }
            })

        // ✅ Listen for Answer
        dbRef.child("answer").child(UserManager.current_userId.toString())
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        Log.d("FirebaseDebug", "Answer received, setting remote description")

                        val answerData = snapshot.value as? Map<String, String>
                        answerData?.let {
                            val answer = SessionDescription(
                                SessionDescription.Type.fromCanonicalForm(it["type"] ?: ""),
                                it["description"] ?: ""
                            )
                            peerConnection.setRemoteDescription(SdpObserverImpl(peerConnection, false), answer)
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("FirebaseDebug", "Firebase error: ${error.message}")
                }
            })

        // ✅ Listen for ICE Candidates
        dbRef.child("candidates").child(UserManager.current_userId.toString())
            .addChildEventListener(object : ChildEventListener {
                override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                    val candidateData = snapshot.value as? Map<String, Any>
                    candidateData?.let {
                        val candidate = IceCandidate(
                            it["sdpMid"] as? String,
                            (it["sdpMLineIndex"] as Long).toInt(),
                            it["candidate"] as String
                        )

                        if (peerConnection.remoteDescription != null) {
                            peerConnection.addIceCandidate(candidate)
                            Log.d("FirebaseDebug", "Added ICE Candidate: $candidate")
                        } else {
                            Log.e("FirebaseDebug", "RemoteDescription is null, ICE candidate not added")
                        }
                    }
                }

                override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
                override fun onChildRemoved(snapshot: DataSnapshot) {}
                override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
                override fun onCancelled(error: DatabaseError) {
                    Log.e("FirebaseDebug", "Firebase error: ${error.message}")
                }
            })
    }




    public fun createAnswer(roomId: String) {
        val sdpConstraints = MediaConstraints()
        peerConnection?.createAnswer(object : SdpObserver {
            override fun onCreateSuccess(sessionDescription: SessionDescription) {
                Log.d("WebRTC", "Answer created successfully")

                // ✅ Set local description first before sending answer
                peerConnection?.setLocalDescription(object : SdpObserver {
                    override fun onSetSuccess() {
                        Log.d("WebRTC", "Local description set successfully for Answer")
                        sendAnswerToFirebase(roomId, sessionDescription)
                    }

                    override fun onSetFailure(error: String?) {
                        Log.e("WebRTC", "Failed to set local description for Answer: $error")
                    }

                    override fun onCreateSuccess(sessionDescription: SessionDescription) {}
                    override fun onCreateFailure(error: String?) {}
                }, sessionDescription)
            }

            override fun onCreateFailure(error: String?) {
                Log.e("WebRTC", "Failed to create Answer: $error")
            }

            override fun onSetSuccess() {}
            override fun onSetFailure(error: String?) {}
        }, sdpConstraints)
    }

    private fun sendAnswerToFirebase(roomId: String, answer: SessionDescription) {
        val dbRef = database.reference.child("calls").child(roomId)

        val answerData = mapOf(
            "type" to answer.type.canonicalForm(),
            "description" to answer.description // ✅ Fix incorrect key from "sdp" to "description"
        )

        dbRef.child("answer").child(UserManager.matched_userid.toString())
            .setValue(answerData)
            .addOnSuccessListener {
                Log.d("FirebaseWrite", "Answer successfully written to Firebase: $answerData")
            }
            .addOnFailureListener { e ->
                Log.e("FirebaseWrite", "Failed to write Answer to Firebase", e)
            }
    }

    public fun createOffer(roomId: String) {
        val sdpConstraints = MediaConstraints()
        peerConnection?.createOffer(object : SdpObserver {
            override fun onCreateSuccess(sessionDescription: SessionDescription) {
                Log.d("WebRTC", "Offer created successfully")

                // ✅ Set local description before sending offer
                peerConnection.setLocalDescription(object : SdpObserver {
                    override fun onSetSuccess() {
                        Log.d("WebRTC", "Local description set successfully for Offer")
                        sendOfferToFirebase(roomId, sessionDescription)
                    }

                    override fun onSetFailure(error: String?) {
                        Log.e("WebRTC", "Failed to set local description for Offer: $error")
                    }

                    override fun onCreateSuccess(sessionDescription: SessionDescription) {}
                    override fun onCreateFailure(error: String?) {}
                }, sessionDescription)
            }

            override fun onCreateFailure(error: String?) {
                Log.e("WebRTC", "Failed to create Offer: $error")
            }

            override fun onSetSuccess() {}
            override fun onSetFailure(error: String?) {}
        }, sdpConstraints)
    }

    private fun sendOfferToFirebase(roomId: String, offer: SessionDescription) {
        val dbRef = database.reference.child("calls").child(roomId)

        val offerData = mapOf(
            "type" to offer.type.canonicalForm(),
            "description" to offer.description // ✅ Fix incorrect key from "sdp" to "description"
        )

        dbRef.child("offer").child(UserManager.matched_userid.toString())
            .setValue(offerData)
            .addOnSuccessListener {
                Log.d("FirebaseWrite", "Offer successfully written to Firebase: $offerData")
            }
            .addOnFailureListener { e ->
                Log.e("FirebaseWrite", "Failed to write Offer to Firebase", e)
            }
    }








    // important if any problem occurs in the connection and listening for offers and such things then there is the option of using an incoming offer function which is this


    // private fun onIncomingOffer(offer: SessionDescription) {
    //    peerConnection.setRemoteDescription(object : SdpObserverAdapter() {
    //        override fun onSetSuccess() {
    //            peerConnection.createAnswer(object : SdpObserverAdapter() {
    //                override fun onCreateSuccess(answer: SessionDescription) {
    //                    peerConnection.setLocalDescription(SdpObserverAdapter(), answer)
    //                    sendAnswerToFirebase(roomId, answer)  // ✅ Send answer back
    //                }
    //            }, MediaConstraints())
    //        }
    //    }, offer)
    //}
    // When Might onIncomingOffer() Still Be Needed?
    //There are certain edge cases where it could be useful:
    //
    //Direct SDP Exchange Without Firebase Delay
    //
    //If you're testing a direct WebRTC connection without Firebase, onIncomingOffer() ensures the answer is generated immediately.
    //Firebase introduces a small delay because it depends on network speed and database updates.
    //Fallback for Edge Cases
    //
    //If there are situations where Firebase does not update correctly, onIncomingOffer() might act as a safety mechanism.
    //Alternative Signaling Methods
    //
    //If you later switch signaling from Firebase to WebSocket, onIncomingOffer() could still be used.


    public fun send_ice_candidate_to_firebase(iceCandidate: IceCandidate){
        val dbref = database.reference.child("calls").child(roomid_global_varriable)


        var formatted_ice = hashMapOf(   // ice candidates now will be sent in a structuresd format as the by default they are in the form of objects , which need to be converted to a formatted string format
            "sdpMid" to iceCandidate.sdpMid,
            "sdpMlineIndex" to iceCandidate.sdpMLineIndex,
            "candidate" to iceCandidate.sdp
        )

        dbref.child("candidates").child(UserManager.matched_userid.toString()).push().setValue(formatted_ice)

    }

    fun showExecutionStep(context: Context,stepcounter:String) {
        // Increment step count
        Toast.makeText(context, "Executed till here: $stepcounter", Toast.LENGTH_SHORT).show()
    }



}