package com.newbieeming.soundcapture.data.model

import android.media.AudioFormat
import android.media.MediaRecorder

data class RecordingConfig(
    val audioSource: Int = MediaRecorder.AudioSource.MIC,
    val sampleRate: Int = 16000,
    val channelConfig: Int = AudioFormat.CHANNEL_IN_MONO,
    val audioFormat: Int = AudioFormat.ENCODING_PCM_16BIT,
    val waveformChannelCount: Int = 1
)
