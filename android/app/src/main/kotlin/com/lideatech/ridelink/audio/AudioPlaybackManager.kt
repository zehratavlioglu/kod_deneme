package com.lideatech.ridelink.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack

class AudioPlaybackManager {
    private var track: AudioTrack? = null

    @Synchronized
    fun start() {
        val minBuffer = AudioTrack.getMinBufferSize(
            AudioCodecManager.SAMPLE_RATE,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
        )
        check(minBuffer > 0) { "AUDIO_TRACK_BUFFER_UNAVAILABLE" }
        track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build(),
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setSampleRate(AudioCodecManager.SAMPLE_RATE)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .build(),
            )
            .setTransferMode(AudioTrack.MODE_STREAM)
            .setBufferSizeInBytes(maxOf(minBuffer, AudioCodecManager.FRAME_BYTES * 4))
            .build()
            .also {
                check(it.state == AudioTrack.STATE_INITIALIZED) { "AUDIO_TRACK_INITIALIZATION_FAILED" }
                @Suppress("DEPRECATION")
                it.setVolume(AudioManager.ADJUST_SAME.toFloat() + 1f)
                it.play()
            }
    }

    @Synchronized
    fun write(pcm: ByteArray) {
        val written = track?.write(pcm, 0, pcm.size, AudioTrack.WRITE_BLOCKING) ?: -1
        check(written >= 0) { "AUDIO_TRACK_WRITE_FAILED:$written" }
    }

    @Synchronized
    fun stop() {
        runCatching { track?.stop() }
        runCatching { track?.flush() }
        track?.release()
        track = null
    }
}
