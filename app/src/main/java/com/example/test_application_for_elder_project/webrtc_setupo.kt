package com.example.test_application_for_elder_project

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.elderprojectfinal.databinding.ActivityWebrtcSetupoBinding
import com.example.elderprojectfinal.databinding.MatchedUserProfileLayoutBinding
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
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
import org.webrtc.RtpReceiver
import org.webrtc.SdpObserver
import org.webrtc.SessionDescription
import org.webrtc.SurfaceTextureHelper
import org.webrtc.VideoCapturer
import org.webrtc.VideoTrack


val database = FirebaseDatabase.getInstance()

val apiKey:String = "AIzaSyDJW69wH1BqmlnSu7XoK9Avhp5v8q_PuE4"

var url:String = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?"

public lateinit var binding: ActivityWebrtcSetupoBinding

public lateinit var matchedUserProfileLayoutBinding: MatchedUserProfileLayoutBinding



private lateinit var peerConnectionFactory: PeerConnectionFactory
private lateinit var peerConnection: PeerConnection
private lateinit var localVideoTrack: VideoTrack
private lateinit var remoteVideoTrack: VideoTrack
private lateinit var videoCapturer: VideoCapturer

lateinit var current_user_id:String
lateinit var  match_id:String

lateinit var roomid_global_varriable:String

class webrtc_setupo : AppCompatActivity() {
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

        GlobalScope.launch {
            setupwebrtc(applicationContext)
        }


       // binding.button5.setOnClickListener({
       //     matching()
       // })




    }



    // webrtc section


    public fun setupwebrtc(context: Context){

        var eglBase = EglBase.create()
        var eglbasecontext: EglBase.Context = eglBase.eglBaseContext

        PeerConnectionFactory.initialize(
            PeerConnectionFactory.InitializationOptions.builder(context).createInitializationOptions()
        )

        peerConnectionFactory = PeerConnectionFactory.builder().setVideoDecoderFactory(
            DefaultVideoDecoderFactory(eglbasecontext)
        ).setVideoEncoderFactory(DefaultVideoEncoderFactory(eglbasecontext,true,true)).createPeerConnectionFactory()


        val iceServers = listOf(
            PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer()
        )
        val rtcConfig = PeerConnection.RTCConfiguration(iceServers)
        peerConnection = peerConnectionFactory.createPeerConnection(
            rtcConfig, // ice server
            object: PeerConnection.Observer{
                override fun onSignalingChange(p0: PeerConnection.SignalingState?) {
                    TODO("Not yet implemented")
                }

                override fun onIceConnectionChange(p0: PeerConnection.IceConnectionState?) {
                    TODO("Not yet implemented")
                }

                override fun onIceConnectionReceivingChange(p0: Boolean) {
                    TODO("Not yet implemented")
                }

                override fun onIceGatheringChange(p0: PeerConnection.IceGatheringState?) {
                    TODO("Not yet implemented")
                }

                override fun onIceCandidate(p0: IceCandidate?) {
                    if (p0 != null) {
                        send_ice_candidate_to_firebase(p0)
                    }
                }

                override fun onIceCandidatesRemoved(p0: Array<out IceCandidate>?) {
                    TODO("Not yet implemented")
                }

                override fun onAddStream(p0: MediaStream?) {
                    TODO("Not yet implemented")
                }

                override fun onRemoveStream(p0: MediaStream?) {
                    TODO("Not yet implemented")
                }

                override fun onDataChannel(p0: DataChannel?) {
                    TODO("Not yet implemented")
                }

                override fun onRenegotiationNeeded() {
                    TODO("Not yet implemented")
                }

                override fun onAddTrack(receiver: RtpReceiver?, mediaStreams: Array<out MediaStream>?) {

                }
            })!!


        // ✅ Initialize local video view
        var localView = binding.localVideoView
        var remoteView = binding.remoteVideoView
        localView.init(eglbasecontext, null)
        remoteView.init(eglbasecontext, null)

        // ✅ Capture Video & Audio
        videoCapturer = video_capture_function()
        var videosource = peerConnectionFactory.createVideoSource(false)
        var video_track_for_camera_related_video = peerConnectionFactory.createVideoTrack("local_track",videosource)

        val audioSource = peerConnectionFactory.createAudioSource(MediaConstraints())
        val audioTrack = peerConnectionFactory.createAudioTrack("local_audio", audioSource)

        // ✅ Attach local stream to UI
        video_track_for_camera_related_video.addSink(localView)

        // ✅ Start capturing video
        videoCapturer.initialize(SurfaceTextureHelper.create("CaptureThread", eglbasecontext), context, videosource.capturerObserver)
        videoCapturer.startCapture(1280, 720, 30)

        // ✅ Create media stream
        val localMediaStream = peerConnectionFactory.createLocalMediaStream("localStream")
        localMediaStream.addTrack(video_track_for_camera_related_video)
        localMediaStream.addTrack(audioTrack)

        peerConnection.addStream(localMediaStream) // Add local stream to peer connection
    }


    private fun video_capture_function(): CameraVideoCapturer {
        val enumerator = Camera2Enumerator(this)
        for (deviceName in enumerator.deviceNames) {
            if (enumerator.isFrontFacing(deviceName)) {
                return enumerator.createCapturer(deviceName, null)
            }
        }
        throw IllegalStateException("No Front Camera Found!")
    }







    public fun joincall(){
        getroomidforuser(current_user_id) { roomid ->
            if(roomid != null){
                roomid_global_varriable = roomid
                GlobalScope.launch {

                }
                GlobalScope.launch {
                    setupfirebaselistener(roomid)
                }
            }

        }
    }



    public fun getroomidforuser(userid: String, callback: (String) -> Unit) {
        val dbref = database.("").child("matched_users")
        dbref.child(userid).get().addOnSuccessListener {
            callback(it.value.toString())
        }

    }






    public fun setupfirebaselistener(roomid:String){
        val dbref = database.reference.child("calls").child(roomid)
        dbref.child("offer").child(current_user_id).addValueEventListener(object : ValueEventListener,
            ChildEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {

            }

            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val offer = snapshot.getValue(SessionDescription::class.java)

                offer?.let {
                    peerConnection.setRemoteDescription(Sdpobserverimpl(),it)
                    createAnswer(roomid)
                }
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                TODO("Not yet implemented")
            }

            override fun onChildRemoved(snapshot: DataSnapshot) {
                TODO("Not yet implemented")
            }

            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {
                TODO("Not yet implemented")
            }

            override fun onCancelled(error: DatabaseError) {

            }

        })
        dbref.child("answer").child(current_user_id).addValueEventListener(object  :
            ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val answer = snapshot.getValue(SessionDescription::class.java)
                answer?.let{
                    peerConnection.setRemoteDescription(Sdpobserverimpl(),it)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                TODO("Not yet implemented")
            }

        })
        dbref.child("candidates").child(current_user_id).addChildEventListener(object :
            ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val candidate = snapshot.getValue(Map::class.java) as? Map<*, *>
                val candidate_final = IceCandidate(
                    candidate?.get("sdpMid") as String?,   // receiving of the ice candidate changef now it will be received as a string and with the help of using a common structure keys can be used to extract the information
                    (candidate?.get("sdpMLineIndex") as Long).toInt(),
                    candidate["candidate"] as String
                )
                if(peerConnection.remoteDescription != null) {
                    candidate_final.let { peerConnection.addIceCandidate(it) }
                }
                else {
                    Log.e("error for ice","error in setting the ics candidate found from firebase")
                }
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onChildRemoved(snapshot: DataSnapshot) {}
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(error: DatabaseError) {}
        })
    }



    public fun createAnswer(roomId:String){

        val sdpConstraints = MediaConstraints()
        peerConnection?.createAnswer(object : SdpObserver {
            override fun onCreateSuccess(sessionDescription: SessionDescription) {
                peerConnection?.setLocalDescription(Sdpobserverimpl(), sessionDescription)
                sendAnswerToFirebase(roomId, sessionDescription)
            }

            override fun onSetSuccess() {}
            override fun onCreateFailure(error: String?) {}
            override fun onSetFailure(error: String?) {}
        }, sdpConstraints)
    }

    private fun sendAnswerToFirebase(roomId: String, answer: SessionDescription) {
        val dbRef = database.reference.child("calls").child(roomId)
        dbRef.child("answer").child(match_id).child(current_user_id).setValue(mapOf("type" to answer.type.canonicalForm(), "sdp" to answer.description))
    }






    public fun createOffer(roomId: String) {
        val sdpConstraints = MediaConstraints()
        peerConnection?.createOffer(object : SdpObserver {
            override fun onCreateSuccess(sessionDescription: SessionDescription) {
                peerConnection?.setLocalDescription(Sdpobserverimpl(), sessionDescription)
                sendOfferToFirebase(roomId, sessionDescription)
            }

            override fun onSetSuccess() {}
            override fun onCreateFailure(error: String?) {}
            override fun onSetFailure(error: String?) {}
        }, sdpConstraints)
    }

    private fun sendOfferToFirebase(roomId: String, offer: SessionDescription) {
        val dbRef = database.reference.child("calls").child(roomId)
        dbRef.child("offer").child(match_id).setValue(mapOf("type" to offer.type.canonicalForm(), "sdp" to offer.description))
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

        dbref.child("candidates").child(match_id).push().setValue(formatted_ice)
        dbref.child("hi")
    }




}