package com.newbieeming.soundcapture.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.newbieeming.soundcapture.R
import com.newbieeming.soundcapture.data.model.RecordingConfig

@Composable
fun ConfigDialog(
    currentConfig: RecordingConfig,
    onDismiss: () -> Unit,
    onConfirm: (RecordingConfig) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedSource by remember(currentConfig.audioSource) { mutableIntStateOf(currentConfig.audioSource) }
    var selectedSampleRate by remember(currentConfig.sampleRate) { mutableIntStateOf(currentConfig.sampleRate) }
    var selectedChannel by remember(currentConfig.channelConfig) { mutableIntStateOf(currentConfig.channelConfig) }
    var selectedWaveformChannelCount by remember(currentConfig.waveformChannelCount) {
        mutableIntStateOf(currentConfig.waveformChannelCount)
    }
    val scrollState = rememberScrollState()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(id = R.string.dialog_recording_config_title)) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(stringResource(id = R.string.label_audio_source), style = MaterialTheme.typography.titleSmall)
                AudioSourceDropdown(
                    selected = selectedSource,
                    onSelect = { selectedSource = it }
                )

                Text(stringResource(id = R.string.label_sample_rate), style = MaterialTheme.typography.titleSmall)
                SampleRateDropdown(
                    selected = selectedSampleRate,
                    onSelect = { selectedSampleRate = it }
                )

                Text(stringResource(id = R.string.label_channel_config), style = MaterialTheme.typography.titleSmall)
                ChannelConfigDropdown(
                    selected = selectedChannel,
                    onSelect = { selectedChannel = it }
                )

                Text(stringResource(id = R.string.label_audio_channel_count), style = MaterialTheme.typography.titleSmall)
                WaveformChannelDropdown(
                    selected = selectedWaveformChannelCount,
                    onSelect = { selectedWaveformChannelCount = it }
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(
                        RecordingConfig(
                            audioSource = selectedSource,
                            sampleRate = selectedSampleRate,
                            channelConfig = selectedChannel,
                            audioFormat = currentConfig.audioFormat,
                            waveformChannelCount = selectedWaveformChannelCount
                        )
                    )
                }
            ) {
                Text(stringResource(id = R.string.btn_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(id = R.string.btn_cancel))
            }
        },
        modifier = modifier
    )
}
