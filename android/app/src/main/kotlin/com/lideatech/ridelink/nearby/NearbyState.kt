package com.lideatech.ridelink.nearby

enum class ConnectionState(val wireName: String) {
    IDLE("idle"),
    DISCOVERED("discovered"),
    CONNECTING("connecting"),
    AUTHENTICATION_REQUIRED("authenticationRequired"),
    CONNECTED("connected"),
    DISCONNECTED("disconnected"),
    FAILED("failed"),
}

data class NearbyPeer(
    val endpointId: String,
    val name: String,
    val token: String,
    var state: ConnectionState,
    var lastSeen: Long = System.currentTimeMillis(),
) {
    fun toMap(): Map<String, Any> = mapOf(
        "endpointId" to endpointId,
        "name" to name,
        "status" to state.wireName,
        "lastSeen" to lastSeen,
    )
}
