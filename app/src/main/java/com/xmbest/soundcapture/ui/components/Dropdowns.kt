package com.xmbest.soundcapture.ui.components

import android.media.AudioFormat
import android.media.MediaRecorder
import android.os.Build
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.xmbest.soundcapture.R

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
                MediaRecorder.AudioSource.DEFAULT to stringResource(id = R.string.audio_source_default),
                MediaRecorder.AudioSource.MIC to stringResource(id = R.string.audio_source_mic),
                MediaRecorder.AudioSource.VOICE_UPLINK to stringResource(id = R.string.audio_source_voice_uplink),
                MediaRecorder.AudioSource.VOICE_DOWNLINK to stringResource(id = R.string.audio_source_voice_downlink),
                MediaRecorder.AudioSource.VOICE_CALL to stringResource(id = R.string.audio_source_voice_call),
                MediaRecorder.AudioSource.CAMCORDER to stringResource(id = R.string.audio_source_camcorder),
                MediaRecorder.AudioSource.VOICE_RECOGNITION to stringResource(id = R.string.audio_source_voice_recognition),
                MediaRecorder.AudioSource.VOICE_COMMUNICATION to stringResource(id = R.string.audio_source_voice_communication),
                MediaRecorder.AudioSource.REMOTE_SUBMIX to stringResource(id = R.string.audio_source_remote_submix),
                MediaRecorder.AudioSource.UNPROCESSED to stringResource(id = R.string.audio_source_unprocessed)
            )
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            put(
                MediaRecorder.AudioSource.VOICE_PERFORMANCE,
                stringResource(id = R.string.audio_source_voice_performance)
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
        AudioFormat.CHANNEL_IN_MONO to stringResource(id = R.string.channel_config_mono),
        AudioFormat.CHANNEL_IN_STEREO to stringResource(id = R.string.channel_config_stereo)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WaveformScaleDropdown(
    selected: Float,
    onSelect: (Float) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val scales = listOf(0.1f, 0.2f, 0.8f, 1f, 5f, 10f, 20f, 40f)

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = stringResource(id = R.string.format_scale, selected.toScaleLabel()),
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
            scales.forEach { value ->
                DropdownMenuItem(
                    text = { Text(stringResource(id = R.string.format_scale, value.toScaleLabel())) },
                    onClick = {
                        onSelect(value)
                        expanded = false
                    }
                )
            }
        }
    }
}

private fun Float.toScaleLabel(): String {
    val intValue = toInt()
    return if (this == intValue.toFloat()) intValue.toString() else toString()
}
