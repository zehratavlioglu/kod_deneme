package com.lideatech.ridelink.nearby

sealed interface NearbyEvent {
    data class PeerChanged(val peer: NearbyPeer) : NearbyEvent
    data class AuthenticationRequired(val endpointId: String, val digits: String) : NearbyEvent
    data class Connected(val endpointId: String, val peerName: String) : NearbyEvent
    data class Disconnected(val endpointId: String) : NearbyEvent
    data class PayloadReceived(val endpointId: String, val bytes: ByteArray) : NearbyEvent
    data class Error(val code: String, val detail: String?) : NearbyEvent
}
