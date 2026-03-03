package com.xmbest.soundcapture.data.repository

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.MediaRecorder
import com.xmbest.soundcapture.data.model.RecordingConfig
import com.xmbest.soundcapture.data.model.RecordingItem
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
            "${config.sampleRate / 1000}k"
        } else {
            "${config.sampleRate}hz"
        }
        val channelCountToken = "${config.waveformChannelCount.coerceIn(1, 8)}channel"
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
            MediaRecorder.AudioSource.MIC -> "mic"
            MediaRecorder.AudioSource.CAMCORDER -> "camcorder"
            MediaRecorder.AudioSource.VOICE_RECOGNITION -> "voice_recognition"
            MediaRecorder.AudioSource.VOICE_COMMUNICATION -> "voice_communication"
            MediaRecorder.AudioSource.VOICE_CALL -> "voice_call"
            MediaRecorder.AudioSource.VOICE_UPLINK -> "voice_up"
            MediaRecorder.AudioSource.VOICE_DOWNLINK -> "voice_dn"
            MediaRecorder.AudioSource.REMOTE_SUBMIX -> "remote_submix"
            MediaRecorder.AudioSource.UNPROCESSED -> "unprocessed"
            MediaRecorder.AudioSource.DEFAULT -> "default"
            else -> "s$audioSource"
        }
    }

    private fun channelConfigToken(channelConfig: Int): String {
        return when (channelConfig) {
            AudioFormat.CHANNEL_IN_MONO -> "mono"
            AudioFormat.CHANNEL_IN_STEREO -> "stereo"
            else -> "c$channelConfig"
        }
    }
}
