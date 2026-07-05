package com.newbieeming.soundcapture.presentation

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.newbieeming.soundcapture.R
import com.newbieeming.soundcapture.data.model.RecordingConfig
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

}
