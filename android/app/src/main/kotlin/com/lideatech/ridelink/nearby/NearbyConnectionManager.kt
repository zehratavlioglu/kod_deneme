package com.lideatech.ridelink.nearby

import android.content.Context
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.AdvertisingOptions
import com.google.android.gms.nearby.connection.ConnectionInfo
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback
import com.google.android.gms.nearby.connection.ConnectionResolution
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo
import com.google.android.gms.nearby.connection.DiscoveryOptions
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback
import com.google.android.gms.nearby.connection.Payload
import com.google.android.gms.nearby.connection.PayloadCallback
import com.google.android.gms.nearby.connection.PayloadTransferUpdate
import com.google.android.gms.nearby.connection.Strategy
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class NearbyConnectionManager(
    context: Context,
    private val eventListener: (NearbyEvent) -> Unit,
) {
    private val client = Nearby.getConnectionsClient(context.applicationContext)
    private val peers = ConcurrentHashMap<String, NearbyPeer>()
    private val localToken = UUID.randomUUID().toString().replace("-", "").take(16)
    private var running = false
    private var autoMatch = false
    private var activeEndpoint: String? = null

    fun setAutoMatch(enabled: Boolean) {
        autoMatch = enabled
    }

    fun peers(): List<NearbyPeer> = peers.values.sortedByDescending { it.lastSeen }

    fun sessionId(endpointId: String): UUID? {
        val remote = peers[endpointId]?.token ?: return null
        val material = listOf(localToken, remote).sorted().joinToString(":")
        return UUID.nameUUIDFromBytes(material.toByteArray(Charsets.UTF_8))
    }

    fun start() {
        if (running) return
        running = true
        val endpointName = "RL1|$localToken"
        client.startAdvertising(
            endpointName,
            SERVICE_ID,
            lifecycleCallback,
            AdvertisingOptions.Builder().setStrategy(STRATEGY).build(),
        ).addOnFailureListener { fail("ADVERTISING_FAILED", it) }
        client.startDiscovery(
            SERVICE_ID,
            discoveryCallback,
            DiscoveryOptions.Builder().setStrategy(STRATEGY).build(),
        ).addOnFailureListener { fail("DISCOVERY_FAILED", it) }
    }

    fun stop() {
        running = false
        client.stopAdvertising()
        client.stopDiscovery()
        client.stopAllEndpoints()
        activeEndpoint = null
        peers.clear()
    }

    fun disconnect(endpointId: String? = activeEndpoint) {
        endpointId?.let(client::disconnectFromEndpoint)
        activeEndpoint = null
    }

    fun send(bytes: ByteArray): Boolean {
        val endpoint = activeEndpoint ?: return false
        client.sendPayload(endpoint, Payload.fromBytes(bytes)).addOnFailureListener {
            fail("PAYLOAD_SEND_FAILED", it)
        }
        return true
    }

    private val discoveryCallback = object : EndpointDiscoveryCallback() {
        override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
            val parts = info.endpointName.split('|')
            if (parts.size != 2 || parts[0] != "RL1") return
            val peer = NearbyPeer(
                endpointId = endpointId,
                name = "RideLink ${parts[1].take(4)}",
                token = parts[1],
                state = ConnectionState.DISCOVERED,
            )
            peers[endpointId] = peer
            eventListener(NearbyEvent.PeerChanged(peer))
            // Only the lexicographically smaller session token initiates. This prevents
            // symmetric request races while both devices advertise and discover.
            if (autoMatch && activeEndpoint == null &&
                ConnectionArbitrator.shouldInitiate(localToken, peer.token)
            ) {
                requestConnection(peer)
            }
        }

        override fun onEndpointLost(endpointId: String) {
            peers[endpointId]?.let {
                it.state = ConnectionState.DISCONNECTED
                it.lastSeen = System.currentTimeMillis()
                eventListener(NearbyEvent.PeerChanged(it))
            }
        }
    }

    private fun requestConnection(peer: NearbyPeer) {
        if (!running || activeEndpoint != null) return
        activeEndpoint = peer.endpointId
        peer.state = ConnectionState.CONNECTING
        eventListener(NearbyEvent.PeerChanged(peer))
        client.requestConnection("RL1|$localToken", peer.endpointId, lifecycleCallback)
            .addOnFailureListener {
                activeEndpoint = null
                fail("CONNECTION_REQUEST_FAILED", it)
            }
    }

    private val lifecycleCallback = object : ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(endpointId: String, info: ConnectionInfo) {
            if (!running || !autoMatch || (activeEndpoint != null && activeEndpoint != endpointId)) {
                client.rejectConnection(endpointId)
                return
            }
            activeEndpoint = endpointId
            val peer = peers[endpointId] ?: NearbyPeer(
                endpointId,
                "RideLink sürücüsü",
                info.endpointName.substringAfter('|', "unknown"),
                ConnectionState.AUTHENTICATION_REQUIRED,
            ).also { peers[endpointId] = it }
            peer.state = ConnectionState.AUTHENTICATION_REQUIRED
            eventListener(NearbyEvent.PeerChanged(peer))
            eventListener(NearbyEvent.AuthenticationRequired(endpointId, info.authenticationDigits))
            // Explicit opt-in is collected in Flutter. The digits are surfaced for audit,
            // but silent first-contact auto-match cannot prove the human peer's identity.
            client.acceptConnection(endpointId, payloadCallback).addOnFailureListener {
                fail("CONNECTION_ACCEPT_FAILED", it)
            }
        }

        override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
            val peer = peers[endpointId]
            if (result.status.isSuccess) {
                activeEndpoint = endpointId
                peer?.state = ConnectionState.CONNECTED
                client.stopDiscovery()
                client.stopAdvertising()
                peer?.let { eventListener(NearbyEvent.PeerChanged(it)) }
                eventListener(NearbyEvent.Connected(endpointId, peer?.name ?: "RideLink sürücüsü"))
            } else {
                activeEndpoint = null
                peer?.state = ConnectionState.FAILED
                peer?.let { eventListener(NearbyEvent.PeerChanged(it)) }
                eventListener(NearbyEvent.Error("CONNECTION_FAILED", result.status.statusMessage))
                restartDiscovery()
            }
        }

        override fun onDisconnected(endpointId: String) {
            activeEndpoint = null
            peers[endpointId]?.let {
                it.state = ConnectionState.DISCONNECTED
                eventListener(NearbyEvent.PeerChanged(it))
            }
            eventListener(NearbyEvent.Disconnected(endpointId))
            restartDiscovery()
        }
    }

    private val payloadCallback = object : PayloadCallback() {
        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            payload.asBytes()?.let { eventListener(NearbyEvent.PayloadReceived(endpointId, it)) }
        }

        override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) = Unit
    }

    private fun restartDiscovery() {
        if (!running) return
        client.startAdvertising(
            "RL1|$localToken",
            SERVICE_ID,
            lifecycleCallback,
            AdvertisingOptions.Builder().setStrategy(STRATEGY).build(),
        )
        client.startDiscovery(
            SERVICE_ID,
            discoveryCallback,
            DiscoveryOptions.Builder().setStrategy(STRATEGY).build(),
        )
    }

    private fun fail(code: String, error: Exception) {
        eventListener(NearbyEvent.Error(code, error.localizedMessage))
    }

    companion object {
        const val SERVICE_ID = "com.lideatech.ridelink"
        private val STRATEGY = Strategy.P2P_POINT_TO_POINT
    }
}
