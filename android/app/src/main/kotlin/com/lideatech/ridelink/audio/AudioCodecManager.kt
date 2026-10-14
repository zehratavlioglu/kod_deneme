package com.lideatech.ridelink.audio

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaCodecList
import android.media.MediaFormat
import android.media.AudioFormat
import java.nio.ByteBuffer
import java.nio.ByteOrder

/** Uses a device MediaCodec Opus encoder/decoder. Unsupported devices fail explicitly. */
class AudioCodecManager {
    private var encoder: MediaCodec? = null
    private var decoder: MediaCodec? = null
    private val encoderInfo = MediaCodec.BufferInfo()
    private val decoderInfo = MediaCodec.BufferInfo()

    fun isSupported(): Boolean {
        val codecs = MediaCodecList(MediaCodecList.REGULAR_CODECS).codecInfos
        val encoderFound = codecs.any { info ->
            info.isEncoder && info.supportedTypes.any { it.equals(MIME, ignoreCase = true) }
        }
        val decoderFound = codecs.any { info ->
            !info.isEncoder && info.supportedTypes.any { it.equals(MIME, ignoreCase = true) }
        }
        return encoderFound && decoderFound
    }

    @Synchronized
    fun start() {
        stop()
        check(isSupported()) { "OPUS_CODEC_UNAVAILABLE" }
        val format = MediaFormat.createAudioFormat(MIME, SAMPLE_RATE, CHANNELS).apply {
            setInteger(MediaFormat.KEY_BIT_RATE, BIT_RATE)
            setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, FRAME_BYTES)
            setInteger(MediaFormat.KEY_COMPLEXITY, 5)
        }
        encoder = MediaCodec.createEncoderByType(MIME).also {
            it.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            it.start()
        }
        val decoderFormat = MediaFormat.createAudioFormat(MIME, SAMPLE_RATE, CHANNELS).apply {
            // Android's Opus decoder requires all three codec-specific data buffers.
            // They are normally supplied by MediaExtractor; our live stream has no container.
            setByteBuffer("csd-0", ByteBuffer.wrap(opusIdentificationHeader()))
            setByteBuffer("csd-1", nativeLongBuffer(0L))
            setByteBuffer("csd-2", nativeLongBuffer(OPUS_SEEK_PREROLL_NS))
            setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, MAX_OPUS_PACKET_BYTES)
            setInteger(MediaFormat.KEY_PCM_ENCODING, AudioFormat.ENCODING_PCM_16BIT)
        }
        decoder = MediaCodec.createDecoderByType(MIME).also {
            it.configure(decoderFormat, null, null, 0)
            it.start()
        }
    }

    @Synchronized
    fun encode(pcm: ByteArray, timestampMicros: Long): ByteArray? {
        val codec = encoder ?: return null
        val inputIndex = codec.dequeueInputBuffer(TIMEOUT_US)
        if (inputIndex < 0) return null
        codec.getInputBuffer(inputIndex)?.apply { clear(); put(pcm) }
        codec.queueInputBuffer(inputIndex, 0, pcm.size, timestampMicros, 0)
        while (true) {
            val outputIndex = codec.dequeueOutputBuffer(encoderInfo, TIMEOUT_US)
            if (outputIndex < 0) return null
            val output = codec.getOutputBuffer(outputIndex)
            val isConfig = encoderInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0
            val bytes = if (!isConfig && output != null && encoderInfo.size > 0) {
                ByteArray(encoderInfo.size).also {
                    output.position(encoderInfo.offset)
                    output.limit(encoderInfo.offset + encoderInfo.size)
                    output.get(it)
                }
            } else null
            codec.releaseOutputBuffer(outputIndex, false)
            if (bytes != null) return bytes
        }
    }

    @Synchronized
    fun decode(opus: ByteArray, timestampMicros: Long): ByteArray? {
        val codec = decoder ?: return null
        val inputIndex = codec.dequeueInputBuffer(TIMEOUT_US)
        if (inputIndex < 0) return null
        codec.getInputBuffer(inputIndex)?.apply { clear(); put(opus) }
        codec.queueInputBuffer(inputIndex, 0, opus.size, timestampMicros, 0)
        val outputIndex = codec.dequeueOutputBuffer(decoderInfo, TIMEOUT_US)
        if (outputIndex < 0) return null
        val output = codec.getOutputBuffer(outputIndex)
        val bytes = if (output != null && decoderInfo.size > 0) {
            ByteArray(decoderInfo.size).also {
                output.position(decoderInfo.offset)
                output.limit(decoderInfo.offset + decoderInfo.size)
                output.get(it)
            }
        } else null
        codec.releaseOutputBuffer(outputIndex, false)
        return bytes
    }

    @Synchronized
    fun stop() {
        listOf(encoder, decoder).forEach { codec ->
            runCatching { codec?.stop() }
            runCatching { codec?.release() }
        }
        encoder = null
        decoder = null
    }

    companion object {
        // Opus decoders expose PCM at the codec's fixed 48 kHz clock. Keeping capture,
        // codec and playback on that same clock avoids interpreting 48 kHz PCM as 16 kHz.
        const val SAMPLE_RATE = 48_000
        const val CHANNELS = 1
        const val FRAME_SAMPLES = 960
        const val FRAME_BYTES = FRAME_SAMPLES * 2
        const val BIT_RATE = 20_000
        private const val MIME = MediaFormat.MIMETYPE_AUDIO_OPUS
        private const val TIMEOUT_US = 10_000L
        private const val MAX_OPUS_PACKET_BYTES = 4_096
        private const val OPUS_SEEK_PREROLL_NS = 80_000_000L

        private fun nativeLongBuffer(value: Long): ByteBuffer =
            ByteBuffer.allocate(java.lang.Long.BYTES)
                .order(ByteOrder.nativeOrder())
                .putLong(value)
                .apply { flip() }

        private fun opusIdentificationHeader(): ByteArray =
            ByteBuffer.allocate(19)
                .order(ByteOrder.LITTLE_ENDIAN)
                .apply {
                    put("OpusHead".toByteArray(Charsets.US_ASCII))
                    put(1) // version
                    put(CHANNELS.toByte())
                    putShort(0) // pre-skip; live audio starts immediately
                    putInt(SAMPLE_RATE)
                    putShort(0) // output gain
                    put(0) // channel mapping family for mono/stereo
                }
                .array()
    }
}
