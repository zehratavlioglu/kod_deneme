package com.lideatech.ridelink.audio

import android.media.AudioRecord
import java.util.TreeMap
import java.util.UUID
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

class AudioSessionManager(
    private val devices: AudioDeviceManager,
    private val sender: (ByteArray) -> Boolean,
    private val onTelemetry: (Telemetry) -> Unit,
    private val onError: (String) -> Unit,
) {
    data class Telemetry(
        val sent: Int,
        val received: Int,
        val lost: Int,
        val buffered: Int,
    )

    private val codec = AudioCodecManager()
    private val capture = AudioCaptureManager()
    private val playback = AudioPlaybackManager()
    private val captureExecutor = Executors.newSingleThreadExecutor()
    private val playbackExecutor = Executors.newSingleThreadExecutor()
    private val running = AtomicBoolean(false)
    private val generation = AtomicInteger()
    private val muted = AtomicBoolean(false)
    private val sent = AtomicInteger()
    private val received = AtomicInteger()
    private val lost = AtomicInteger()
    private val jitter = TreeMap<Int, AudioPacket>()
    private var sessionId = UUID.randomUUID()
    private var nextSequence = 0
    private var expectedSequence: Int? = null

    fun isCodecSupported(): Boolean = codec.isSupported()

    fun start(debugPhoneAudio: Boolean, id: UUID) {
        if (!running.compareAndSet(false, true)) return
        val currentGeneration = generation.incrementAndGet()
        sessionId = id
        nextSequence = 0
        expectedSequence = null
        sent.set(0)
        received.set(0)
        lost.set(0)
        synchronized(jitter) { jitter.clear() }
        try {
            check(devices.selectCommunicationRoute(debugPhoneAudio)) { "AUDIO_ROUTE_UNAVAILABLE" }
            codec.start()
            playback.start()
            val recorder = capture.start()
            captureExecutor.execute { captureLoop(recorder, currentGeneration) }
        } catch (error: Throwable) {
            running.set(false)
            stopResources()
            throw error
        }
    }

    fun setMuted(value: Boolean) {
        muted.set(value)
    }

    fun receive(bytes: ByteArray) {
        val packet = AudioPacket.decode(bytes) ?: return
        if (!running.get() || packet.sessionId != sessionId) return
        synchronized(jitter) {
            jitter[packet.sequence] = packet
            while (jitter.size > MAX_JITTER_PACKETS) jitter.pollFirstEntry()
        }
        val currentGeneration = generation.get()
        playbackExecutor.execute { drainJitter(currentGeneration) }
    }

    fun stop() {
        if (!running.compareAndSet(true, false)) return
        generation.incrementAndGet()
        stopResources()
    }

    private fun captureLoop(recorder: AudioRecord, currentGeneration: Int) {
        val frame = ByteArray(AudioCodecManager.FRAME_BYTES)
        while (isCurrent(currentGeneration)) {
            var offset = 0
            while (offset < frame.size && isCurrent(currentGeneration)) {
                val count = recorder.read(frame, offset, frame.size - offset, AudioRecord.READ_BLOCKING)
                if (count <= 0) {
                    onError("AUDIO_RECORD_READ_FAILED:$count")
                    running.set(false)
                    break
                }
                offset += count
            }
            if (!isCurrent(currentGeneration) || muted.get()) continue
            val timestamp = System.nanoTime() / 1_000L
            try {
                codec.encode(frame, timestamp)?.let { opus ->
                    val packet = AudioPacket(sessionId, nextSequence++, timestamp, opus).encode()
                    if (sender(packet)) sent.incrementAndGet()
                    report()
                }
            } catch (error: Throwable) {
                onError(error.message ?: "AUDIO_ENCODE_FAILED")
                running.set(false)
            }
        }
    }

    private fun drainJitter(currentGeneration: Int) {
        while (isCurrent(currentGeneration)) {
            val packet = synchronized(jitter) {
                if (jitter.size < TARGET_JITTER_PACKETS) return
                jitter.pollFirstEntry()?.value
            } ?: return
            val expected = expectedSequence
            if (expected != null && packet.sequence > expected) {
                lost.addAndGet(packet.sequence - expected)
            }
            expectedSequence = packet.sequence + 1
            try {
                codec.decode(packet.opus, packet.timestampMicros)?.let(playback::write)
                received.incrementAndGet()
                report()
            } catch (error: Throwable) {
                onError(error.message ?: "AUDIO_DECODE_FAILED")
                return
            }
        }
    }

    private fun isCurrent(value: Int): Boolean = running.get() && generation.get() == value

    private fun report() = onTelemetry(
        Telemetry(sent.get(), received.get(), lost.get(), synchronized(jitter) { jitter.size }),
    )

    private fun stopResources() {
        capture.stop()
        playback.stop()
        codec.stop()
        devices.clearRoute()
        synchronized(jitter) { jitter.clear() }
    }

    companion object {
        private const val TARGET_JITTER_PACKETS = 2
        private const val MAX_JITTER_PACKETS = 5
    }
}
