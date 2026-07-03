package com.newbieeming.soundcapture.presentation

import com.newbieeming.soundcapture.data.model.RecordingConfig

sealed class RecordingIntent {
    object StartRecording : RecordingIntent()
    object StopRecording : RecordingIntent()
    data class UpdateConfig(val config: RecordingConfig) : RecordingIntent()
}
