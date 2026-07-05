package com.newbieeming.soundcapture.presentation

import com.newbieeming.soundcapture.data.model.RecordingConfig

sealed class RecordingIntent {
    object StartRecording : RecordingIntent()
    object StopRecording : RecordingIntent()
    data class UpdateConfig(val config: RecordingConfig) : RecordingIntent()
    data class DeleteRecording(val id: String) : RecordingIntent()
    data class RenameRecording(val id: String, val newName: String) : RecordingIntent()
    object LoadRecordings : RecordingIntent()
}
