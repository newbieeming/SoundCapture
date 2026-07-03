package com.newbieeming.soundcapture.presentation

import com.newbieeming.soundcapture.data.model.RecordingConfig

sealed class RecordingIntent {
    object StartRecording : RecordingIntent()
    object StopRecording : RecordingIntent()
    object PauseRecording : RecordingIntent()
    object ResumeRecording : RecordingIntent()
    data class DeleteRecording(val id: String) : RecordingIntent()
    data class RenameRecording(val id: String, val newName: String) : RecordingIntent()
    data class UpdateConfig(val config: RecordingConfig) : RecordingIntent()
    object LoadRecordings : RecordingIntent()
}
