package com.newbieeming.soundcapture.presentation

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.newbieeming.soundcapture.R
import com.newbieeming.soundcapture.data.model.RecordingConfig
import com.newbieeming.soundcapture.data.model.RecordingItem
import com.newbieeming.soundcapture.data.repository.RecordingRepository
import com.newbieeming.soundcapture.data.repository.SettingsRepository
import com.newbieeming.soundcapture.domain.AudioRecorder
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.FileInputStream
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

@HiltViewModel
class RecordingViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val audioRecorder: AudioRecorder,
    private val repository: RecordingRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _state = MutableStateFlow(RecordingState())
    val state = _state.asStateFlow()

    private val _effect = MutableSharedFlow<RecordingEffect>(
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val effect = _effect.asSharedFlow().distinctUntilChanged()

    private var recordingJob: Job? = null
    private var durationJob: Job? = null
    private var currentOutputFilePath: String? = null
    private var recordingStartTimeMs: Long = 0L
    private var playbackJob: Job? = null
    private var audioTrack: AudioTrack? = null

    init {
        observeRecordingConfig()
        observeRecordings()
        repository.loadRecordings()
    }

    fun handleIntent(intent: RecordingIntent) {
        when (intent) {
            is RecordingIntent.StartRecording -> startRecording()
            is RecordingIntent.StopRecording -> stopRecording()
            is RecordingIntent.UpdateConfig -> updateConfig(intent.config)
            is RecordingIntent.DeleteRecording -> deleteRecording(intent.id)
            is RecordingIntent.RenameRecording -> renameRecording(intent.id, intent.newName)
            is RecordingIntent.LoadRecordings -> repository.loadRecordings()
            is RecordingIntent.PlayRecording -> playRecording(intent.recording)
            is RecordingIntent.StopPlayback -> stopPlayback()
        }
    }

    private fun startRecording() {
        val currentConfig = _state.value.config
        val outputFile = repository.createRecordingFile(currentConfig)
        currentOutputFilePath = outputFile.absolutePath
        recordingStartTimeMs = System.currentTimeMillis()

        recordingJob = audioRecorder.startRecording(currentConfig, outputFile)
            .flowOn(Dispatchers.IO)
            .onEach { data ->
                _state.update {
                    it.copy(
                        isRecording = true,
                        channelLevels = data.channelLevels
                    )
                }
            }
            .catch { e ->
                _effect.emit(
                    RecordingEffect.ShowError(
                        e.message ?: context.getString(R.string.msg_recording_failed)
                    )
                )
                _state.update { it.copy(isRecording = false) }
                currentOutputFilePath = null
                durationJob?.cancel()
            }
            .launchIn(viewModelScope)

        durationJob = viewModelScope.launch {
            while (isActive) {
                val elapsed = System.currentTimeMillis() - recordingStartTimeMs
                _state.update { it.copy(recordingDurationMs = elapsed) }
                delay(100.milliseconds)
            }
        }
    }

    private fun stopRecording() {
        audioRecorder.stop()
        recordingJob?.cancel()
        durationJob?.cancel()
        _state.update {
            it.copy(
                isRecording = false,
                channelLevels = emptyList(),
                recordingDurationMs = 0L
            )
        }
        viewModelScope.launch {
            val filePath = currentOutputFilePath
            if (filePath != null) {
                _effect.emit(RecordingEffect.ShowSaveSuccess(filePath))
                repository.loadRecordings()
            } else {
                _effect.emit(RecordingEffect.ShowSaveFailure)
            }
            currentOutputFilePath = null
        }
    }

    private fun updateConfig(config: RecordingConfig) {
        _state.update { it.copy(config = config) }
        viewModelScope.launch {
            settingsRepository.setRecordingConfig(config)
        }
    }

    private fun observeRecordingConfig() {
        settingsRepository.recordingConfig
            .onEach { config ->
                _state.update { it.copy(config = config) }
            }
            .launchIn(viewModelScope)
    }

    private fun observeRecordings() {
        repository.recordings
            .onEach { recordings ->
                _state.update { it.copy(recordings = recordings) }
            }
            .launchIn(viewModelScope)
    }

    private fun deleteRecording(id: String) {
        // 如果正在播放该录音，先停止播放
        if (_state.value.isPlaying && _state.value.currentPlayingId == id) {
            stopPlayback()
        }
        viewModelScope.launch {
            val deleted = repository.deleteRecording(id)
            if (deleted) {
                _effect.emit(RecordingEffect.ShowSuccess(context.getString(R.string.msg_recording_deleted)))
            } else {
                _effect.emit(RecordingEffect.ShowError(context.getString(R.string.msg_recording_delete_failed)))
            }
        }
    }

    private fun renameRecording(id: String, newName: String) {
        viewModelScope.launch {
            val renamed = repository.renameRecording(id, newName)
            if (renamed) {
                _effect.emit(RecordingEffect.ShowSuccess(context.getString(R.string.msg_recording_renamed)))
            } else {
                _effect.emit(RecordingEffect.ShowError(context.getString(R.string.msg_recording_rename_failed)))
            }
        }
    }

    private fun playRecording(recording: RecordingItem) {
        // 如果当前正在播放同一个录音，则停止
        if (_state.value.isPlaying && _state.value.currentPlayingId == recording.id) {
            stopPlayback()
            return
        }
        // 如果正在播放其他录音，先停止
        if (_state.value.isPlaying) {
            stopPlayback()
        }

        playbackJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                val config = _state.value.config
                val channelCount = config.waveformChannelCount
                val channelIndexMask = RecordingRepository.channelIndexMaskFromCount(channelCount)
                val channelMask = when (channelCount) {
                    1 -> AudioFormat.CHANNEL_OUT_MONO
                    2 -> AudioFormat.CHANNEL_OUT_STEREO
                    else -> AudioFormat.CHANNEL_OUT_STEREO
                }
                val bufferSize = AudioTrack.getMinBufferSize(
                    config.sampleRate,
                    channelMask,
                    config.audioFormat
                )
                if (bufferSize == AudioTrack.ERROR || bufferSize == AudioTrack.ERROR_BAD_VALUE) {
                    withContext(Dispatchers.Main) {
                        _effect.emit(RecordingEffect.ShowError("无法初始化音频播放"))
                    }
                    return@launch
                }

                val track = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(config.audioFormat)
                            .setSampleRate(config.sampleRate)
                            .setChannelIndexMask(channelIndexMask)
                            .build()
                    )
                    .setBufferSizeInBytes(bufferSize)
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build()

                audioTrack = track
                withContext(Dispatchers.Main) {
                    _state.update {
                        it.copy(isPlaying = true, currentPlayingId = recording.id)
                    }
                }

                try {
                    track.play()
                    val buffer = ByteArray(bufferSize)
                    FileInputStream(recording.filePath).use { fis ->
                        var bytesRead: Int
                        while (isActive) {
                            bytesRead = fis.read(buffer)
                            if (bytesRead == -1) break
                            track.write(buffer, 0, bytesRead)
                        }
                    }
                } finally {
                    runCatching {
                        track.stop()
                        track.release()
                    }
                    audioTrack = null
                    withContext(Dispatchers.Main) {
                        _state.update {
                            it.copy(isPlaying = false, currentPlayingId = null)
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _effect.emit(
                        RecordingEffect.ShowError(
                            e.message ?: "播放失败"
                        )
                    )
                }
            }
        }
    }

    private fun stopPlayback() {
        playbackJob?.cancel()
        playbackJob = null
        audioTrack?.let { track ->
            runCatching {
                track.stop()
                track.release()
            }
        }
        audioTrack = null
        _state.update {
            it.copy(isPlaying = false, currentPlayingId = null)
        }
    }

}
