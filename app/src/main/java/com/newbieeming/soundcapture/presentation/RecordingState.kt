package com.newbieeming.soundcapture.presentation

import com.newbieeming.soundcapture.data.model.RecordingConfig
import com.newbieeming.soundcapture.data.model.RecordingItem

data class RecordingState(
    val isRecording: Boolean = false,
    val recordings: List<RecordingItem> = emptyList(),
    val config: RecordingConfig = RecordingConfig(),
    val channelLevels: List<Float> = emptyList()
)
