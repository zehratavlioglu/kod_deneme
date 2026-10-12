package com.lideatech.ridelink.audio

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.media.audiofx.AcousticEchoCanceler
import android.media.audiofx.AutomaticGainControl
import android.media.audiofx.NoiseSuppressor

class AudioCaptureManager {
    private var recorder: AudioRecord? = null
    private var aec: AcousticEchoCanceler? = null
    private var ns: NoiseSuppressor? = null
    private var agc: AutomaticGainControl? = null

    fun start(): AudioRecord {
        val minBuffer = AudioRecord.getMinBufferSize(
            AudioCodecManager.SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
        )
        check(minBuffer > 0) { "AUDIO_RECORD_BUFFER_UNAVAILABLE" }
        val record = AudioRecord.Builder()
            .setAudioSource(MediaRecorder.AudioSource.VOICE_COMMUNICATION)
            .setAudioFormat(
                AudioFormat.Builder()
                    .setSampleRate(AudioCodecManager.SAMPLE_RATE)
                    .setChannelMask(AudioFormat.CHANNEL_IN_MONO)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .build(),
            )
            .setBufferSizeInBytes(maxOf(minBuffer * 2, AudioCodecManager.FRAME_BYTES * 4))
            .build()
        check(record.state == AudioRecord.STATE_INITIALIZED) { "AUDIO_RECORD_INITIALIZATION_FAILED" }
        recorder = record
        if (AcousticEchoCanceler.isAvailable()) {
            aec = AcousticEchoCanceler.create(record.audioSessionId)?.apply { enabled = true }
        }
        if (NoiseSuppressor.isAvailable()) {
            ns = NoiseSuppressor.create(record.audioSessionId)?.apply { enabled = true }
        }
        if (AutomaticGainControl.isAvailable()) {
            agc = AutomaticGainControl.create(record.audioSessionId)?.apply { enabled = true }
        }
        record.startRecording()
        check(record.recordingState == AudioRecord.RECORDSTATE_RECORDING) { "AUDIO_RECORD_START_FAILED" }
        return record
    }

    fun stop() {
        runCatching { recorder?.stop() }
        recorder?.release()
        recorder = null
        listOf(aec, ns, agc).forEach { runCatching { it?.release() } }
        aec = null
        ns = null
        agc = null
    }
}
