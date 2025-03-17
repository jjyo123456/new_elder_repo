package com.example.test_application_for_elder_project

import android.util.Log
import org.webrtc.SdpObserver
import org.webrtc.SessionDescription
import org.webrtc.PeerConnection

class SdpObserverImpl(private val peerConnection: PeerConnection, private val isLocal: Boolean) : SdpObserver {

    override fun onCreateSuccess(sessionDescription: SessionDescription) {
        Log.d("WebRTC", "SDP Created: ${sessionDescription.description}")

        // Set the SDP (offer or answer) on the peer connection
        peerConnection.setLocalDescription(this, sessionDescription)

        if (isLocal) {
            // Send SDP to the remote peer via signaling (Firebase, WebSocket, etc.)
            sendSdpToRemote(sessionDescription)
        }
    }

    override fun onSetSuccess() {
        Log.d("WebRTC", "SDP successfully set")
    }

    override fun onCreateFailure(error: String?) {
        Log.e("WebRTC", "SDP creation failed: $error")
    }

    override fun onSetFailure(error: String?) {
        Log.e("WebRTC", "SDP setting failed: $error")
    }

    private fun sendSdpToRemote(sessionDescription: SessionDescription) {
        // Implement logic to send SDP to the remote peer via Firebase/WebSocket
        Log.d("WebRTC", "Sending SDP to remote peer")
    }
}
