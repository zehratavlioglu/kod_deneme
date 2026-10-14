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
import java.io.BufferedInputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.EOFException
import java.io.PipedInputStream
import java.io.PipedOutputStream
import java.util.UUID
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class NearbyConnectionManager(
    context: Context,
    private val eventListener: (NearbyEvent) -> Unit,
) {
    private val client = Nearby.getConnectionsClient(context.applicationContext)
    private val peers = ConcurrentHashMap<String, NearbyPeer>()
    private val localToken = UUID.randomUUID().toString().replace("-", "").take(16)
    @Volatile private var running = false
    private var autoMatch = false
    @Volatile private var activeEndpoint: String? = null
    private val audioStreamLock = Any()
    private val streamExecutor = Executors.newCachedThreadPool()
    private val audioWriterExecutor = Executors.newSingleThreadExecutor()
    private val outgoingAudioFrames = ArrayBlockingQueue<ByteArray>(AUDIO_QUEUE_CAPACITY)
    private val incomingStreams = ConcurrentHashMap<Long, Payload>()
    private var outgoingAudioEndpoint: String? = null
    private var outgoingAudioPayload: Payload? = null
    private var outgoingAudioData: DataOutputStream? = null
    private var outgoingAudioGeneration = 0

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
        activeEndpoint = null
        resetAudioStreams()
        client.stopAdvertising()
        client.stopDiscovery()
        client.stopAllEndpoints()
        peers.clear()
    }

    fun disconnect(endpointId: String? = activeEndpoint) {
        val endpoint = endpointId ?: activeEndpoint
        activeEndpoint = null
        resetAudioStreams()
        endpoint?.let(client::disconnectFromEndpoint)
    }

    fun send(bytes: ByteArray): Boolean {
        val endpoint = activeEndpoint ?: return false
        client.sendPayload(endpoint, Payload.fromBytes(bytes)).addOnFailureListener {
            fail("PAYLOAD_SEND_FAILED", it)
        }
        return true
    }

    fun sendAudio(bytes: ByteArray): Boolean {
        if (!running) return false
        val endpoint = activeEndpoint ?: return false
        synchronized(audioStreamLock) {
            // The endpoint may have been cleared while this capture frame was waiting
            // for the stream lock during disconnect.
            if (!running || activeEndpoint != endpoint) return false
            if (outgoingAudioData == null || outgoingAudioEndpoint != endpoint) {
                detachOutgoingAudioStreamLocked()?.close()
                val input = PipedInputStream(STREAM_PIPE_BYTES)
                val output = DataOutputStream(PipedOutputStream(input))
                val payload = Payload.fromStream(input)
                val generation = ++outgoingAudioGeneration
                outgoingAudioEndpoint = endpoint
                outgoingAudioPayload = payload
                outgoingAudioData = output
                outgoingAudioFrames.clear()
                audioWriterExecutor.execute {
                    writeAudioStream(endpoint, payload, output, generation)
                }
                client.sendPayload(endpoint, payload).addOnFailureListener {
                    fail("AUDIO_STREAM_SEND_FAILED", it)
                    closeOutgoingAudioStream(generation)
                }
            }
        }
        // Capture must never wait for transport. When Samsung is briefly slower,
        // discard the oldest queued frame and keep the newest real-time audio.
        while (!outgoingAudioFrames.offer(bytes)) outgoingAudioFrames.poll()
        return true
    }

    private fun writeAudioStream(
        endpoint: String,
        payload: Payload,
        output: DataOutputStream,
        generation: Int,
    ) {
        try {
            while (isOutgoingAudioStream(endpoint, generation)) {
                val bytes = outgoingAudioFrames.poll(WRITER_POLL_MS, TimeUnit.MILLISECONDS)
                    ?: continue
                output.writeInt(bytes.size)
                output.write(bytes)
                output.flush()
            }
        } catch (error: Throwable) {
            if (isOutgoingAudioStream(endpoint, generation)) {
                fail("AUDIO_STREAM_WRITE_FAILED", Exception(error))
            }
        } finally {
            closeOutgoingAudioStream(generation)
            runCatching { output.close() }
            runCatching { payload.close() }
        }
    }

    private fun isOutgoingAudioStream(endpoint: String, generation: Int): Boolean =
        synchronized(audioStreamLock) {
            running && activeEndpoint == endpoint && outgoingAudioGeneration == generation
        }

    private data class AudioStreamResources(
        val output: DataOutputStream?,
        val payload: Payload?,
    ) {
        fun close() {
            runCatching { output?.close() }
            runCatching { payload?.close() }
        }
    }

    private fun detachOutgoingAudioStreamLocked(): AudioStreamResources? {
        if (outgoingAudioData == null && outgoingAudioPayload == null) return null
        val resources = AudioStreamResources(outgoingAudioData, outgoingAudioPayload)
        outgoingAudioData = null
        outgoingAudioPayload = null
        outgoingAudioEndpoint = null
        outgoingAudioFrames.clear()
        return resources
    }

    private fun closeOutgoingAudioStream(expectedGeneration: Int? = null) {
        val resources = synchronized(audioStreamLock) {
            if (expectedGeneration != null && outgoingAudioGeneration != expectedGeneration) {
                return
            }
            outgoingAudioGeneration++
            detachOutgoingAudioStreamLocked()
        }
        resources?.close()
    }

    private fun resetAudioStreams() {
        closeOutgoingAudioStream()
        incomingStreams.values.forEach { runCatching { it.close() } }
        incomingStreams.clear()
    }

    private fun readAudioStream(endpointId: String, payload: Payload) {
        incomingStreams[payload.id] = payload
        streamExecutor.execute {
            try {
                val stream = checkNotNull(payload.asStream()) { "AUDIO_STREAM_UNAVAILABLE" }
                val input = DataInputStream(BufferedInputStream(stream.asInputStream()))
                while (running && activeEndpoint == endpointId) {
                    val size = input.readInt()
                    require(size in 1..MAX_STREAM_FRAME_BYTES) { "INVALID_AUDIO_FRAME_SIZE:$size" }
                    val bytes = ByteArray(size)
                    input.readFully(bytes)
                    eventListener(NearbyEvent.PayloadReceived(endpointId, bytes))
                }
            } catch (_: EOFException) {
                // Normal when a peer stops or replaces its stream.
            } catch (error: Throwable) {
                if (running && activeEndpoint == endpointId) {
                    fail("AUDIO_STREAM_READ_FAILED", Exception(error))
                }
            } finally {
                incomingStreams.remove(payload.id)
                runCatching { payload.close() }
            }
        }
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
            resetAudioStreams()
            peers[endpointId]?.let {
                it.state = ConnectionState.DISCONNECTED
                eventListener(NearbyEvent.PeerChanged(it))
            }
            if (running) {
                eventListener(NearbyEvent.Disconnected(endpointId))
                restartDiscovery()
            }
        }
    }

    private val payloadCallback = object : PayloadCallback() {
        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            when (payload.type) {
                Payload.Type.BYTES -> payload.asBytes()?.let {
                    eventListener(NearbyEvent.PayloadReceived(endpointId, it))
                }
                Payload.Type.STREAM -> readAudioStream(endpointId, payload)
            }
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
        private const val STREAM_PIPE_BYTES = 16 * 1024
        private const val MAX_STREAM_FRAME_BYTES = 8 * 1024
        private const val AUDIO_QUEUE_CAPACITY = 3
        private const val WRITER_POLL_MS = 100L
    }
}
