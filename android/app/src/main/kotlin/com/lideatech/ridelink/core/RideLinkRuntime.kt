package com.lideatech.ridelink.core

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.net.wifi.WifiManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.lideatech.ridelink.audio.AudioDeviceManager
import com.lideatech.ridelink.audio.AudioPacket
import com.lideatech.ridelink.audio.AudioSessionManager
import com.lideatech.ridelink.nearby.NearbyConnectionManager
import com.lideatech.ridelink.nearby.NearbyEvent
import org.json.JSONObject
import java.util.UUID

class RideLinkRuntime private constructor(private val context: Context) {
    private val preferences = context.getSharedPreferences("ridelink", Context.MODE_PRIVATE)
    private val devices = AudioDeviceManager(context)
    private val nearby = NearbyConnectionManager(context, ::onNearbyEvent)
    private val audio = AudioSessionManager(
        devices = devices,
        sender = nearby::send,
        onTelemetry = {
            telemetry = it
            emitStatus("Ses telemetrisi güncellendi")
        },
        onError = { emitError(it) },
    )
    private var listener: ((Map<String, Any?>) -> Unit)? = null
    private var isRiding = false
    private var debugPhoneAudio = false
    private var connectionState = "idle"
    private var peerName: String? = null
    private var activeEndpoint: String? = null
    private var activeSession: UUID? = null
    private var telemetry = AudioSessionManager.Telemetry(0, 0, 0, 0)
    private var autoMatch = preferences.getBoolean(KEY_AUTO_MATCH, false)

    init {
        nearby.setAutoMatch(autoMatch)
    }

    fun setEventListener(value: ((Map<String, Any?>) -> Unit)?) {
        listener = value
    }

    fun validateStart(debugPhoneAudio: Boolean) {
        check(hasMicrophonePermission()) { "PERMISSION_DENIED" }
        check(hasNearbyPermissions()) { "PERMISSION_DENIED" }
        check(playServicesAvailable()) { "NEARBY_UNAVAILABLE" }
        check(debugPhoneAudio || devices.bluetoothCommunicationRoute() != null) {
            "INTERCOM_NOT_CONNECTED"
        }
        check(audio.isCodecSupported()) { "OPUS_CODEC_UNAVAILABLE" }
    }

    fun start(debugPhoneAudio: Boolean) {
        validateStart(debugPhoneAudio)
        this.debugPhoneAudio = debugPhoneAudio
        isRiding = true
        connectionState = "discovering"
        nearby.start()
        emitStatus("Sürüş modu başlatıldı")
    }

    fun stop() {
        isRiding = false
        audio.stop()
        nearby.stop()
        devices.clearRoute()
        connectionState = "idle"
        activeEndpoint = null
        activeSession = null
        peerName = null
        emitStatus("Sürüş modu durduruldu")
    }

    fun setAutoMatch(enabled: Boolean) {
        autoMatch = enabled
        preferences.edit().putBoolean(KEY_AUTO_MATCH, enabled).apply()
        nearby.setAutoMatch(enabled)
        emitStatus(if (enabled) "Otomatik eşleşme açıldı" else "Otomatik eşleşme kapatıldı")
    }

    fun setMuted(muted: Boolean) {
        audio.setMuted(muted)
        emitStatus(if (muted) "Mikrofon susturuldu" else "Mikrofon açıldı")
    }

    fun disconnect(userInitiated: Boolean) {
        sendControl("DISCONNECT")
        audio.stop()
        nearby.disconnect()
        connectionState = "disconnected"
        peerName = null
        activeEndpoint = null
        activeSession = null
        emitStatus(if (userInitiated) "Görüşme sonlandırıldı" else "Bağlantı kesildi")
    }

    fun status(): Map<String, Any?> {
        val intercom = runCatching { devices.bluetoothCommunicationRoute() }.getOrNull()
        return mapOf(
            "isRiding" to isRiding,
            "autoMatch" to autoMatch,
            "bluetoothEnabled" to bluetoothEnabled(),
            "wifiEnabled" to wifiEnabled(),
            "microphoneGranted" to hasMicrophonePermission(),
            "nearbyGranted" to hasNearbyPermissions(),
            "playServicesAvailable" to playServicesAvailable(),
            "intercomName" to intercom?.name,
            "audioRoute" to devices.currentRouteName(),
            "microphoneReady" to (debugPhoneAudio || intercom != null),
            "connectionState" to connectionState,
            "peerName" to peerName,
            "sentPackets" to telemetry.sent,
            "receivedPackets" to telemetry.received,
            "lostPackets" to telemetry.lost,
            "bufferedPackets" to telemetry.buffered,
            "devices" to nearby.peers().map { it.toMap() },
        )
    }

    private fun onNearbyEvent(event: NearbyEvent) {
        when (event) {
            is NearbyEvent.PeerChanged -> emitStatus("Yakındaki cihaz: ${event.peer.state.wireName}")
            is NearbyEvent.AuthenticationRequired -> {
                connectionState = "authenticationRequired"
                listener?.invoke(
                    mapOf(
                        "type" to "authentication",
                        "message" to "Nearby doğrulama kodu: ${event.digits}",
                    ),
                )
                emitStatus("Bağlantı doğrulaması bekleniyor")
            }
            is NearbyEvent.Connected -> {
                activeEndpoint = event.endpointId
                peerName = event.peerName
                connectionState = "connected"
                activeSession = nearby.sessionId(event.endpointId)
                sendControl("HELLO", mapOf("protocolVersion" to PROTOCOL_VERSION))
                emitStatus("RideLink bağlantısı kuruldu")
            }
            is NearbyEvent.Disconnected -> disconnect(userInitiated = false)
            is NearbyEvent.PayloadReceived -> onPayload(event.bytes)
            is NearbyEvent.Error -> emitError("${event.code}: ${event.detail.orEmpty()}")
        }
    }

    private fun onPayload(bytes: ByteArray) {
        if (AudioPacket.looksLikeAudio(bytes)) {
            audio.receive(bytes)
            return
        }
        val message = runCatching { JSONObject(String(bytes, Charsets.UTF_8)) }.getOrNull() ?: return
        if (message.optInt("schemaVersion", -1) != PROTOCOL_VERSION) {
            emitError("PROTOCOL_VERSION_MISMATCH")
            disconnect(userInitiated = false)
            return
        }
        when (message.optString("type")) {
            "HELLO" -> {
                sendControl(
                    "CAPABILITIES",
                    mapOf(
                        "sampleRate" to 16_000,
                        "codec" to "opus",
                        "frameMs" to 20,
                    ),
                )
                sendControl("MATCH_ACCEPT")
                startAudioIfReady()
            }
            "MATCH_ACCEPT" -> startAudioIfReady()
            "AUDIO_STOP", "DISCONNECT" -> disconnect(userInitiated = false)
            "HEARTBEAT", "CAPABILITIES", "SESSION_START", "AUDIO_START" -> Unit
            "MATCH_REQUEST", "MATCH_REJECT" -> Unit
        }
    }

    private fun startAudioIfReady() {
        val session = activeSession ?: return
        runCatching {
            audio.start(debugPhoneAudio, session)
            sendControl("SESSION_START", mapOf("sessionId" to session.toString()))
            sendControl("AUDIO_START")
        }.onFailure { emitError(it.message ?: "AUDIO_INITIALIZATION_FAILED") }
    }

    private fun sendControl(type: String, extras: Map<String, Any?> = emptyMap()) {
        if (activeEndpoint == null) return
        val json = JSONObject()
            .put("schemaVersion", PROTOCOL_VERSION)
            .put("type", type)
            .put("sessionId", activeSession?.toString())
        extras.forEach { (key, value) -> json.put(key, value) }
        nearby.send(json.toString().toByteArray(Charsets.UTF_8))
    }

    private fun emitStatus(message: String) {
        listener?.invoke(status() + mapOf("type" to "status", "message" to message))
    }

    private fun emitError(message: String) {
        listener?.invoke(mapOf("type" to "error", "message" to message))
    }

    private fun hasMicrophonePermission() = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.RECORD_AUDIO,
    ) == PackageManager.PERMISSION_GRANTED

    private fun hasNearbyPermissions(): Boolean {
        val permissions = when {
            Build.VERSION.SDK_INT >= 33 -> listOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_ADVERTISE,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.NEARBY_WIFI_DEVICES,
            )
            Build.VERSION.SDK_INT >= 31 -> listOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_ADVERTISE,
                Manifest.permission.BLUETOOTH_CONNECT,
            )
            Build.VERSION.SDK_INT >= 29 -> listOf(Manifest.permission.ACCESS_FINE_LOCATION)
            else -> listOf(Manifest.permission.ACCESS_COARSE_LOCATION)
        }
        return permissions.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun bluetoothEnabled(): Boolean = runCatching {
        context.getSystemService(BluetoothManager::class.java).adapter?.isEnabled == true
    }.getOrDefault(false)

    private fun wifiEnabled(): Boolean = runCatching {
        context.applicationContext.getSystemService(WifiManager::class.java).isWifiEnabled
    }.getOrDefault(false)

    private fun playServicesAvailable(): Boolean =
        GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context) == ConnectionResult.SUCCESS

    companion object {
        private const val PROTOCOL_VERSION = 1
        private const val KEY_AUTO_MATCH = "auto_match"
        @Volatile private var instance: RideLinkRuntime? = null

        fun initialize(context: Context): RideLinkRuntime = instance ?: synchronized(this) {
            instance ?: RideLinkRuntime(context.applicationContext).also { instance = it }
        }
    }
}
