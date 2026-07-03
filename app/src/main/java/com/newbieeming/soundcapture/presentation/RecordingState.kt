package com.newbieeming.soundcapture.presentation

import com.newbieeming.soundcapture.data.model.RecordingConfig

data class RecordingState(
    val isRecording: Boolean = false,
    val config: RecordingConfig = RecordingConfig(),
    val channelLevels: List<Float> = emptyList()
)
