package com.xmbest.soundcapture.data.model

data class RecordingItem(
    val id: String,
    val name: String,
    val filePath: String,
    val duration: Long,
    val timestamp: Long,
    val sampleRate: Int,
    val channelConfig: Int
)
