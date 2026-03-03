package com.xmbest.soundcapture.presentation

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xmbest.soundcapture.R
import com.xmbest.soundcapture.data.model.RecordingConfig
import com.xmbest.soundcapture.data.repository.RecordingRepository
import com.xmbest.soundcapture.data.repository.SettingsRepository
import com.xmbest.soundcapture.domain.AudioRecorder
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RecordingViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val audioRecorder: AudioRecorder,
    private val repository: RecordingRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _state = MutableStateFlow(RecordingState())
    val state = _state.asStateFlow()

    private val _effect = MutableSharedFlow<RecordingEffect>()
    val effect = _effect.asSharedFlow()

    private var recordingJob: Job? = null
    private var startTime: Long = 0

    init {
        observeRecordings()
        observeRecordingConfig()
        refreshRecordings()
    }

    fun handleIntent(intent: RecordingIntent) {
        when (intent) {
            is RecordingIntent.StartRecording -> startRecording()
            is RecordingIntent.StopRecording -> stopRecording()
            is RecordingIntent.PauseRecording -> pauseRecording()
            is RecordingIntent.ResumeRecording -> resumeRecording()
            is RecordingIntent.DeleteRecording -> deleteRecording(intent.id)
            is RecordingIntent.RenameRecording -> renameRecording(intent.id, intent.newName)
            is RecordingIntent.UpdateConfig -> updateConfig(intent.config)
            is RecordingIntent.LoadRecordings -> refreshRecordings()
        }
    }

    private fun startRecording() {
        val currentConfig = _state.value.config
        val outputFile = repository.createRecordingFile(currentConfig)
        startTime = System.currentTimeMillis()

        recordingJob = audioRecorder.startRecording(currentConfig, outputFile)
            .flowOn(Dispatchers.IO)
            .onEach { data ->
                _state.update {
                    it.copy(
                        isRecording = true,
                        isPaused = false,
                        currentDuration = System.currentTimeMillis() - startTime,
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
            }
            .launchIn(viewModelScope)
    }

    private fun stopRecording() {
        audioRecorder.stop()
        recordingJob?.cancel()
        _state.update {
            it.copy(
                isRecording = false,
                isPaused = false,
                currentDuration = 0,
                channelLevels = emptyList()
            )
        }
        refreshRecordings()
    }

    private fun pauseRecording() {
        audioRecorder.pause()
        _state.update { it.copy(isPaused = true) }
    }

    private fun resumeRecording() {
        audioRecorder.resume()
        _state.update { it.copy(isPaused = false) }
    }

    private fun deleteRecording(id: String) {
        viewModelScope.launch {
            if (repository.deleteRecording(id)) {
                _effect.emit(RecordingEffect.ShowSuccess(context.getString(R.string.msg_delete_success)))
            } else {
                _effect.emit(RecordingEffect.ShowError(context.getString(R.string.msg_delete_failed)))
            }
        }
    }

    private fun renameRecording(id: String, newName: String) {
        viewModelScope.launch {
            if (repository.renameRecording(id, newName)) {
                _effect.emit(RecordingEffect.ShowSuccess(context.getString(R.string.msg_rename_success)))
            } else {
                _effect.emit(RecordingEffect.ShowError(context.getString(R.string.msg_rename_failed)))
            }
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

    private fun refreshRecordings() {
        viewModelScope.launch {
            repository.loadRecordings()
        }
    }
}
