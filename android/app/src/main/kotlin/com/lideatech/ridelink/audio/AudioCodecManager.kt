package com.lideatech.ridelink.audio

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaCodecList
import android.media.MediaFormat

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

    fun start() {
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
        decoder = MediaCodec.createDecoderByType(MIME).also {
            it.configure(MediaFormat.createAudioFormat(MIME, SAMPLE_RATE, CHANNELS), null, null, 0)
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

    fun stop() {
        listOf(encoder, decoder).forEach { codec ->
            runCatching { codec?.stop() }
            runCatching { codec?.release() }
        }
        encoder = null
        decoder = null
    }

    companion object {
        const val SAMPLE_RATE = 16_000
        const val CHANNELS = 1
        const val FRAME_SAMPLES = 320
        const val FRAME_BYTES = FRAME_SAMPLES * 2
        const val BIT_RATE = 20_000
        private const val MIME = MediaFormat.MIMETYPE_AUDIO_OPUS
        private const val TIMEOUT_US = 10_000L
    }
}
