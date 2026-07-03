package com.newbieeming.soundcapture.ui.components

import android.media.AudioFormat
import android.media.MediaRecorder
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.newbieeming.soundcapture.R
import com.newbieeming.soundcapture.data.model.AudioFormatExt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioSourceDropdown(
    selected: Int,
    onSelect: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val sources = buildMap {
        putAll(
            mapOf(
                MediaRecorder.AudioSource.DEFAULT to "DEFAULT",
                MediaRecorder.AudioSource.MIC to "MIC",
                MediaRecorder.AudioSource.VOICE_UPLINK to "VOICE_UPLINK",
                MediaRecorder.AudioSource.VOICE_DOWNLINK to "VOICE_DOWNLINK",
                MediaRecorder.AudioSource.VOICE_CALL to "VOICE_CALL",
                MediaRecorder.AudioSource.CAMCORDER to "CAMCORDER",
                MediaRecorder.AudioSource.VOICE_RECOGNITION to "VOICE_RECOGNITION",
                MediaRecorder.AudioSource.VOICE_COMMUNICATION to "VOICE_COMMUNICATION",
                MediaRecorder.AudioSource.REMOTE_SUBMIX to "REMOTE_SUBMIX",
                MediaRecorder.AudioSource.UNPROCESSED to "UNPROCESSED"
            )
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            put(
                MediaRecorder.AudioSource.VOICE_PERFORMANCE,
                "VOICE_PERFORMANCE"
            )
        }
    }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = sources[selected] ?: stringResource(id = R.string.label_unknown),
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            sources.forEach { (value, label) ->
                DropdownMenuItem(
                    text = { Text(label) },
                    onClick = {
                        onSelect(value)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SampleRateDropdown(
    selected: Int,
    onSelect: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val rates = listOf(8000, 16000, 22050, 44100, 48000)

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = stringResource(id = R.string.format_rate_hz, selected),
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            rates.forEach { rate ->
                DropdownMenuItem(
                    text = { Text(stringResource(id = R.string.format_rate_hz, rate)) },
                    onClick = {
                        onSelect(rate)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChannelConfigDropdown(
    selected: Int,
    onSelect: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val channels = mapOf(
        AudioFormat.CHANNEL_IN_DEFAULT to "CHANNEL_IN_DEFAULT",
        AudioFormat.CHANNEL_IN_MONO to "CHANNEL_IN_MONO",
        AudioFormat.CHANNEL_IN_STEREO to "CHANNEL_IN_STEREO",
        AudioFormat.CHANNEL_IN_LEFT to "CHANNEL_IN_LEFT",
        AudioFormat.CHANNEL_IN_RIGHT to "CHANNEL_IN_RIGHT",
        AudioFormat.CHANNEL_IN_BACK to "CHANNEL_IN_BACK",
        AudioFormatExt.CHANNEL_IN_BACK_LEFT to "CHANNEL_IN_BACK_LEFT",
        AudioFormatExt.CHANNEL_IN_BACK_RIGHT to "CHANNEL_IN_BACK_RIGHT",
        AudioFormatExt.CHANNEL_IN_CENTER to "CHANNEL_IN_CENTER",
        AudioFormatExt.CHANNEL_IN_LOW_FREQUENCY to "CHANNEL_IN_LOW_FREQUENCY",
        AudioFormatExt.CHANNEL_IN_TOP_LEFT to "CHANNEL_IN_TOP_LEFT",
        AudioFormatExt.CHANNEL_IN_TOP_RIGHT to "CHANNEL_IN_TOP_RIGHT",
        AudioFormatExt.CHANNEL_IN_2POINT0POINT2 to "CHANNEL_IN_2POINT0POINT2",
        AudioFormatExt.CHANNEL_IN_2POINT1POINT2 to "CHANNEL_IN_2POINT1POINT2",
        AudioFormatExt.CHANNEL_IN_3POINT0POINT2 to "CHANNEL_IN_3POINT0POINT2",
        AudioFormatExt.CHANNEL_IN_3POINT1POINT2 to "CHANNEL_IN_3POINT1POINT2",
        AudioFormatExt.CHANNEL_IN_5POINT1 to "CHANNEL_IN_5POINT1",
        AudioFormatExt.CHANNEL_IN_FRONT_BACK to "CHANNEL_IN_FRONT_BACK",
    )

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = channels[selected] ?: stringResource(id = R.string.label_unknown),
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            channels.forEach { (value, label) ->
                DropdownMenuItem(
                    text = { Text(label) },
                    onClick = {
                        onSelect(value)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WaveformChannelDropdown(
    selected: Int,
    onSelect: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val channels = (1..8).toList()

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = "$selected",
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            channels.forEach { value ->
                DropdownMenuItem(
                    text = { Text("$value") },
                    onClick = {
                        onSelect(value)
                        expanded = false
                    }
                )
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.S)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioFormatDropdown(
    selected: Int,
    onSelect: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val formats = mapOf(
        AudioFormat.ENCODING_PCM_8BIT to "8BIT",
        AudioFormat.ENCODING_PCM_16BIT to "16BIT",
        AudioFormat.ENCODING_PCM_32BIT to "32BIT",
        AudioFormat.ENCODING_PCM_FLOAT to "FLOAT"
    )

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = formats[selected] ?: stringResource(id = R.string.label_unknown),
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            formats.forEach { (value, label) ->
                DropdownMenuItem(
                    text = { Text(label) },
                    onClick = {
                        onSelect(value)
                        expanded = false
                    }
                )
            }
        }
    }
}
