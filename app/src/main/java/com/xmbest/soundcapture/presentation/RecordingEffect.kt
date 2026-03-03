package com.xmbest.soundcapture.presentation

sealed class RecordingEffect {
    data class ShowError(val message: String) : RecordingEffect()
    data class ShowSuccess(val message: String) : RecordingEffect()
    object RequestPermission : RecordingEffect()
}
