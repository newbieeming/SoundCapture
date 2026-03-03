package com.xmbest.soundcapture.presentation

import com.xmbest.soundcapture.data.model.RecordingConfig
import com.xmbest.soundcapture.data.model.RecordingItem

data class RecordingState(
    val isRecording: Boolean = false,
    val isPaused: Boolean = false,
    val currentDuration: Long = 0,
    val recordings: List<RecordingItem> = emptyList(),
    val config: RecordingConfig = RecordingConfig(),
    val channelLevels: List<Float> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)
