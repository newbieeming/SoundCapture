package com.newbieeming.soundcapture.data.repository

import android.content.Context
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import com.newbieeming.soundcapture.data.model.RecordingConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "app_settings")

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val defaultConfig = RecordingConfig()

    private companion object {
        val AudioSourceKey = intPreferencesKey("audio_source")
        val SampleRateKey = intPreferencesKey("sample_rate")
        val ChannelConfigKey = intPreferencesKey("channel_config")
        val AudioFormatKey = intPreferencesKey("audio_format")
        val WaveformChannelKey = intPreferencesKey("waveform_channel")
    }

    private val preferences = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }

    val recordingConfig: Flow<RecordingConfig> = preferences
        .map { preferences ->
            RecordingConfig(
                audioSource = preferences[AudioSourceKey] ?: defaultConfig.audioSource,
                sampleRate = preferences[SampleRateKey] ?: defaultConfig.sampleRate,
                channelConfig = preferences[ChannelConfigKey] ?: defaultConfig.channelConfig,
                audioFormat = preferences[AudioFormatKey] ?: defaultConfig.audioFormat,
                waveformChannelCount = (preferences[WaveformChannelKey]
                    ?: defaultConfig.waveformChannelCount)
                    .coerceIn(1, 8)
            )
        }

    suspend fun setRecordingConfig(config: RecordingConfig) {
        context.dataStore.edit { preferences ->
            preferences[AudioSourceKey] = config.audioSource
            preferences[SampleRateKey] = config.sampleRate
            preferences[ChannelConfigKey] = config.channelConfig
            preferences[AudioFormatKey] = config.audioFormat
            preferences[WaveformChannelKey] = config.waveformChannelCount.coerceIn(1, 8)
        }
    }
}
