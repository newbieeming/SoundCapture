package com.newbieeming.soundcapture.presentation

import com.newbieeming.soundcapture.data.model.RecordingConfig
import com.newbieeming.soundcapture.data.model.RecordingItem

data class RecordingState(
    val isRecording: Boolean = false,
    val config: RecordingConfig = RecordingConfig(),
    val channelLevels: List<Float> = emptyList(),
    val recordingDurationMs: Long = 0L,
    val recordings: List<RecordingItem> = emptyList(),
    val isPlaying: Boolean = false,
    val currentPlayingId: String? = null
)
