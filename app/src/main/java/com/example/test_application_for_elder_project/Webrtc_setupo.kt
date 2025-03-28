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
import androidx.camera.camera2.Camera2Config
import androidx.camera.core.CameraSelector
import androidx.camera.core.CameraXConfig
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.test_application_for_elder_project.databinding.ActivityWebrtcSetupoBinding
import com.example.test_application_for_elder_project.databinding.MatchedUserProfileLayoutBinding
import com.google.common.util.concurrent.ListenableFuture
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
import org.webrtc.CameraEnumerator
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
public  var localview: SurfaceViewRenderer? = null


lateinit var roomid_global_varriable:String
private val CAMERA_PERMISSION_CODE = 101

private lateinit var previewView: PreviewView
private lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>

class Webrtc_setupo : AppCompatActivity(),CameraXConfig.Provider {
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


        previewView = findViewById(R.id.local_video_view)

        startCamera()


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

        Handler(Looper.getMainLooper()).post {
            Toast.makeText(context, "Initializing WebRTC", Toast.LENGTH_SHORT).show()
        }

        Executors.newSingleThreadExecutor().execute {
            PeerConnectionFactory.initialize(
                PeerConnectionFactory.InitializationOptions.builder(context.applicationContext)
                    .createInitializationOptions()
            )

            peerConnectionFactory = PeerConnectionFactory.builder()
                .setVideoDecoderFactory(DefaultVideoDecoderFactory(eglBaseContext))
                .setVideoEncoderFactory(DefaultVideoEncoderFactory(eglBaseContext, true, true))
                .createPeerConnectionFactory()

            Handler(Looper.getMainLooper()).post {
                Toast.makeText(context, "PeerConnectionFactory Created", Toast.LENGTH_SHORT).show()
            }

            val iceServers = listOf(
                PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer()
            )
            val rtcConfig = PeerConnection.RTCConfiguration(iceServers)

            peerConnection = peerConnectionFactory.createPeerConnection(rtcConfig, object : PeerConnection.Observer {
                override fun onSignalingChange(state: PeerConnection.SignalingState?) {}

                override fun onIceConnectionChange(state: PeerConnection.IceConnectionState?) {
                    Log.d("WebRTC", "ICE Connection State: $state")
                    Handler(Looper.getMainLooper()).post {
                        Toast.makeText(context, "ICE Connection State: $state", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onIceConnectionReceivingChange(p0: Boolean) {}

                override fun onIceGatheringChange(p0: PeerConnection.IceGatheringState?) {}

                override fun onIceCandidate(candidate: IceCandidate?) {
                    candidate?.let {
                        Log.d("WebRTC", "New ICE Candidate: ${it.sdp}")
                        send_ice_candidate_to_firebase(it)
                        Handler(Looper.getMainLooper()).post {
                            Toast.makeText(context, "New ICE Candidate Generated", Toast.LENGTH_SHORT).show()
                        }
                    }
                }

                override fun onIceCandidatesRemoved(p0: Array<out IceCandidate>?) {}

                override fun onAddStream(p0: MediaStream?) {
                    p0?.videoTracks?.get(0)?.addSink(localview)
                }

                override fun onRemoveStream(p0: MediaStream?) {}

                override fun onDataChannel(p0: DataChannel?) {}

                override fun onRenegotiationNeeded() {}

                override fun onAddTrack(p0: RtpReceiver?, p1: Array<out MediaStream>?) {}
            })!!

            Handler(Looper.getMainLooper()).post {
                setupLocalMedia(context, eglBaseContext)
            }
        }
    }


    private fun setupLocalMedia(context: Context, eglBaseContext: EglBase.Context) {
       //  localview = findViewById<SurfaceViewRenderer>(R.id.local_video_view)
        val remoteview = findViewById<SurfaceViewRenderer>(R.id.remote_video_view)



        Toast.makeText(context, "Setting up local media", Toast.LENGTH_SHORT).show()

       // localview!!.init(eglBaseContext, null)
        remoteview.init(eglBaseContext, null)

        Log.d("WebRTC", "Initializing Video Capturer...")
        videoCapturer = createVideoCapturer() ?: run {
            Log.e("WebRTC", "Video capturer initialization failed!")
            Toast.makeText(context, "Video Capturer Initialization Failed!", Toast.LENGTH_LONG).show()
            return
        }

        val surfaceTextureHelper = SurfaceTextureHelper.create(Thread.currentThread().name, eglBaseContext)
        if (surfaceTextureHelper == null) {
            Log.e("WebRTC", "SurfaceTextureHelper creation failed!")
            return
        }

        if (!::peerConnectionFactory.isInitialized) {
            Log.e("WebRTC", "PeerConnectionFactory is not initialized!")
            return
        }

        val videoSource = peerConnectionFactory.createVideoSource(videoCapturer!!.isScreencast)
        videoCapturer!!.initialize(surfaceTextureHelper, applicationContext, videoSource.capturerObserver)

        Toast.makeText(context, "Video Capturer Initialized", Toast.LENGTH_SHORT).show()

        val videoTrack = peerConnectionFactory.createVideoTrack("local_track", videoSource)
        videoTrack.setEnabled(true)
       // videoTrack.addSink(localview)

        try {
            Log.d("WebRTC", "Starting video capture...")
            videoCapturer!!.startCapture(720, 480, 20)
            Toast.makeText(context, "Video Capture Started", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e("WebRTC", "Error starting video capture: ${e.message}")
            Toast.makeText(context, "Error starting capture: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    // ✅ Corrected createVideoCapturer() function
    private fun createVideoCapturer(): VideoCapturer? {
        val enumerator: CameraEnumerator = if (Camera2Enumerator.isSupported(applicationContext)) {
            Camera2Enumerator(applicationContext)
        } else {
            Camera1Enumerator(true)
        }

        val cameraEventsHandler = object : CameraVideoCapturer.CameraEventsHandler {
            override fun onCameraError(error: String) {
                Log.e("WebRTC", "Camera error: $error")
            }

            override fun onCameraDisconnected() {
                Log.w("WebRTC", "Camera disconnected")
            }

            override fun onCameraFreezed(error: String) {
                Log.e("WebRTC", "Camera froze: $error")
            }

            override fun onCameraOpening(cameraName: String) {
                Log.d("WebRTC", "Opening camera: $cameraName")
            }

            override fun onFirstFrameAvailable() {
                Log.d("WebRTC", "First frame available")
            }

            override fun onCameraClosed() {
                Log.d("WebRTC", "Camera closed")
            }
        }

        // Try to get the front-facing camera first
        for (deviceName in enumerator.deviceNames) {
            if (enumerator.isFrontFacing(deviceName)) {
                val capturer = enumerator.createCapturer(deviceName, cameraEventsHandler)
                if (capturer != null) {
                    Log.d("WebRTC", "Using front camera: $deviceName")
                    return capturer
                }
            }
        }

        // If no front camera, try any available camera


        Log.e("WebRTC", "No suitable camera found!")
        return null // No camera found
    }



    // ✅ Check if Camera is already in use


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











    public fun joincall(){

        getroomidforuser(UserManager.current_userId.toString()) { roomid ->
            if(roomid != null){
                roomid_global_varriable = roomid

                showExecutionStep(applicationContext,roomid)



                    setupFirebaseListener(roomid)

            }

        }
    }



    public fun getroomidforuser(userid: String, callback: (String) -> Unit) {
        val roomId: String = UUID.randomUUID().toString()
        val dbref = database.reference.child("matched_users")
        dbref.child(userid).push().setValue(roomId).addOnSuccessListener {
            callback(roomId)
        }

    }








    fun setupFirebaseListener(roomId: String) {
        val dbRef = database.reference.child("calls").child(roomId)
        showExecutionStep(this,"firebaslistener")
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
                            createAnswer()  // Create answer after setting offer
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





    public fun createAnswer() {
        val sdpConstraints = MediaConstraints()
        peerConnection.createAnswer(object : SdpObserver {
            override fun onCreateSuccess(sessionDescription: SessionDescription) {
                Log.d("WebRTC", "Answer created successfully")

                // ✅ Set local description first before sending answer
                peerConnection.setLocalDescription(object : SdpObserver {
                    override fun onCreateSuccess(p0: SessionDescription?) {

                    }

                    override fun onSetSuccess() {
                        Log.d("WebRTC", "Local description set successfully for Answer")
                        sendAnswerToFirebase(roomid_global_varriable, sessionDescription)
                    }

                    override fun onCreateFailure(p0: String?) {
                        TODO("Not yet implemented")
                    }


                    override fun onSetFailure(error: String?) {
                        Log.e("WebRTC", "Failed to set local description for Answer: $error")
                    }


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
        peerConnection.createOffer(object : SdpObserver {
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






    // the preview based local video code
    private fun startCamera() {
        cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
            bindCameraToLifecycle(cameraProvider)
        }, ContextCompat.getMainExecutor(this))
    }

    private fun bindCameraToLifecycle(cameraProvider: ProcessCameraProvider) {
        val preview = Preview.Builder().build()
        val cameraSelector = CameraSelector.Builder()
            .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
            .build()

        preview.setSurfaceProvider(previewView.surfaceProvider)

        try {
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(this, cameraSelector, preview)
        } catch (e: Exception) {
            Log.e("CameraX", "Use case binding failed", e)
        }
    }



    override fun getCameraXConfig(): CameraXConfig {
        return Camera2Config.defaultConfig()
    }



}