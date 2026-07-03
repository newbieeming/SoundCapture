package com.newbieeming.soundcapture.data.repository

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.MediaRecorder
import com.newbieeming.soundcapture.data.model.AudioFormatExt
import com.newbieeming.soundcapture.data.model.RecordingConfig
import com.newbieeming.soundcapture.data.model.RecordingItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecordingRepository @Inject constructor() {
    private companion object {
        @SuppressLint("SdCardPath")
        private const val RECORDINGS_DIR_PATH = "/sdcard/SoundCapture"
        private const val FILE_EXT = "pcm"
    }

    private val _recordings = MutableStateFlow<List<RecordingItem>>(emptyList())
    val recordings: Flow<List<RecordingItem>> = _recordings.asStateFlow()

    private val recordingsDir: File
        get() = File(RECORDINGS_DIR_PATH).apply { mkdirs() }

    init {
        loadRecordings()
    }

    fun loadRecordings() {
        val items = runCatching {
            recordingsDir.listFiles()?.mapNotNull { file ->
                if (file.extension == FILE_EXT) {
                    RecordingItem(
                        id = file.nameWithoutExtension,
                        name = file.nameWithoutExtension,
                        filePath = file.absolutePath,
                        duration = 0L,
                        timestamp = file.lastModified(),
                        sampleRate = 44100,
                        channelConfig = 1
                    )
                } else {
                    null
                }
            }?.sortedByDescending { it.timestamp } ?: emptyList()
        }.getOrDefault(emptyList())

        _recordings.value = items
    }

    fun createRecordingFile(config: RecordingConfig): File {
        val baseName = buildFileBaseName(config)
        return uniqueRecordingFile(baseName)
    }

    fun deleteRecording(id: String): Boolean {
        val file = File(recordingsDir, "$id.pcm")
        val deleted = runCatching { file.delete() }.getOrDefault(false)
        if (deleted) {
            loadRecordings()
        }
        return deleted
    }

    fun renameRecording(id: String, newName: String): Boolean {
        val oldFile = File(recordingsDir, "$id.pcm")
        val newFile = File(recordingsDir, "$newName.pcm")
        val renamed = runCatching { oldFile.renameTo(newFile) }.getOrDefault(false)
        if (renamed) {
            loadRecordings()
        }
        return renamed
    }

    private fun buildFileBaseName(config: RecordingConfig): String {
        val sampleRateToken = if (config.sampleRate % 1000 == 0) {
            "${config.sampleRate / 1000}K"
        } else {
            "${config.sampleRate}HZ"
        }
        val channelCountToken = "${config.waveformChannelCount.coerceIn(1, 8)}CH"
        val sourceToken = audioSourceToken(config.audioSource)
        val channelConfigToken = channelConfigToken(config.channelConfig)
        val timeToken = SimpleDateFormat("yyMMddHHmmss", Locale.getDefault()).format(Date())
        return "${sampleRateToken}_${channelCountToken}_${sourceToken}_${channelConfigToken}_${timeToken}"
    }

    private fun uniqueRecordingFile(baseName: String): File {
        var candidate = File(recordingsDir, "$baseName.$FILE_EXT")
        var suffix = 1
        while (candidate.exists()) {
            candidate = File(recordingsDir, "${baseName}_$suffix.$FILE_EXT")
            suffix += 1
        }
        return candidate
    }

    private fun audioSourceToken(audioSource: Int): String {
        return when (audioSource) {
            MediaRecorder.AudioSource.MIC -> "MIC"
            MediaRecorder.AudioSource.CAMCORDER -> "CAMCORDER"
            MediaRecorder.AudioSource.VOICE_RECOGNITION -> "VOICE_RECOGNITION"
            MediaRecorder.AudioSource.VOICE_COMMUNICATION -> "VOICE_COMMUNICATION"
            MediaRecorder.AudioSource.VOICE_CALL -> "VOICE_CALL"
            MediaRecorder.AudioSource.VOICE_UPLINK -> "VOICE_UPLINK"
            MediaRecorder.AudioSource.VOICE_DOWNLINK -> "VOICE_DOWNLINK"
            MediaRecorder.AudioSource.REMOTE_SUBMIX -> "REMOTE_SUBMIX"
            MediaRecorder.AudioSource.UNPROCESSED -> "UNPROCESSED"
            MediaRecorder.AudioSource.DEFAULT -> "DEFAULT"
            else -> "s$audioSource"
        }
    }

    private fun channelConfigToken(channelConfig: Int): String {
        return when (channelConfig) {
            AudioFormat.CHANNEL_IN_DEFAULT -> "DEFAULT"
            AudioFormat.CHANNEL_IN_MONO -> "MONO"
            AudioFormat.CHANNEL_IN_STEREO -> "STEREO"
            AudioFormat.CHANNEL_IN_LEFT -> "LEFT"
            AudioFormat.CHANNEL_IN_RIGHT -> "RIGHT"
            AudioFormat.CHANNEL_IN_BACK -> "BACK"
            AudioFormatExt.CHANNEL_IN_BACK_LEFT -> "BACK_LEFT"
            AudioFormatExt.CHANNEL_IN_BACK_RIGHT -> "BACK_RIGHT"
            AudioFormatExt.CHANNEL_IN_CENTER -> "CENTER"
            AudioFormatExt.CHANNEL_IN_LOW_FREQUENCY -> "LOW_FREQUENCY"
            AudioFormatExt.CHANNEL_IN_TOP_LEFT -> "TOP_LEFT"
            AudioFormatExt.CHANNEL_IN_TOP_RIGHT -> "TOP_RIGHT"
            AudioFormatExt.CHANNEL_IN_2POINT0POINT2 -> "2POINT0POINT2"
            AudioFormatExt.CHANNEL_IN_2POINT1POINT2 -> "2POINT1POINT2"
            AudioFormatExt.CHANNEL_IN_3POINT0POINT2 -> "3POINT0POINT2"
            AudioFormatExt.CHANNEL_IN_3POINT1POINT2 -> "3POINT1POINT2"
            AudioFormatExt.CHANNEL_IN_5POINT1 -> "5POINT1"
            AudioFormatExt.CHANNEL_IN_FRONT_BACK -> "FRONT_BACK"
            else -> "c$channelConfig"
        }
    }
}
