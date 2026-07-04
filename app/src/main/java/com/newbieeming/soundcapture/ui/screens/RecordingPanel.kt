package com.newbieeming.soundcapture.ui.screens

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.newbieeming.soundcapture.R
import com.newbieeming.soundcapture.data.model.RecordingConfig
import com.newbieeming.soundcapture.data.repository.RecordingRepository
import com.newbieeming.soundcapture.presentation.RecordingState
import com.newbieeming.soundcapture.ui.components.WaveformView

@Composable
internal fun RecordingPanel(
    modifier: Modifier = Modifier,
    state: RecordingState
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
    ) {
        WaveformView(
            channelLevels = state.channelLevels,
            modifier = Modifier.fillMaxSize(),
            channelCount = state.config.waveformChannelCount,
            isActive = state.isRecording
        )
    }
}

@Composable
internal fun RecordingControlButton(
    isRecording: Boolean,
    onStart: () -> Unit,
    onStop: () -> Unit
) {
    if (isRecording) {
        Button(
            onClick = onStop,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error
            ),
            shape = RoundedCornerShape(32.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                Icons.Default.Stop,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(stringResource(id = R.string.btn_stop_recording))
        }
    } else {
        Button(
            onClick = onStart,
            shape = RoundedCornerShape(32.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                Icons.Default.Mic,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(stringResource(id = R.string.btn_start_recording))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun RecordingParamsInfo(config: RecordingConfig) {
    val params = listOf(
        RecordingRepository.sampleRateToken(config.sampleRate),
        RecordingRepository.audioFormatToken(config.audioFormat),
        RecordingRepository.channelConfigToken(config.channelConfig),
        RecordingRepository.audioSourceToken(config.audioSource),
        RecordingRepository.channelCountToken(config.waveformChannelCount)
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(id = R.string.label_recording_params),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(6.dp))
        FlowRow(
            horizontalArrangement = Arrangement.Center,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            params.forEachIndexed { index, param ->
                Text(
                    text = param,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (index < params.lastIndex) {
                    Text(
                        text = " · ",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}

@SuppressLint("DefaultLocale")
@Composable
internal fun RecordingDuration(
    isRecording: Boolean,
    durationMs: Long
) {
    val totalSeconds = (durationMs / 1000).toInt()
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    val millis = (durationMs % 1000).toInt()
    val timeText = String.format("%02d:%02d.%03d", minutes, seconds, millis)

    Text(
        text = timeText,
        style = MaterialTheme.typography.headlineMedium.copy(
            fontFamily = FontFamily.Serif
        ),
        color = if (isRecording) {
            MaterialTheme.colorScheme.error
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
        }
    )
}
