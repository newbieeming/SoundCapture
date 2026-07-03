package com.newbieeming.soundcapture.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.newbieeming.soundcapture.R
import com.newbieeming.soundcapture.presentation.RecordingEffect
import com.newbieeming.soundcapture.presentation.RecordingIntent
import com.newbieeming.soundcapture.presentation.RecordingState
import com.newbieeming.soundcapture.presentation.RecordingViewModel
import com.newbieeming.soundcapture.ui.components.ConfigDialog
import com.newbieeming.soundcapture.ui.components.WaveformView
import kotlinx.coroutines.launch

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun RecordingScreen(
    viewModel: RecordingViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showConfigDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val allFilesAccessLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { }

    val permissionsState = rememberMultiplePermissionsState(
        permissions = runtimePermissions()
    )

    ObserveEffects(
        viewModel = viewModel,
        snackbarHostState = snackbarHostState
    )

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            Row(modifier = Modifier.fillMaxSize()) {
                RecordingPanel(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(7f),
                    state = state
                )
                Spacer(modifier = Modifier.width(10.dp))
                Card(modifier = Modifier.fillMaxHeight().weight(3f)) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        RecordingControlButton(
                            isRecording = state.isRecording,
                            onStart = {
                                if (!permissionsState.allPermissionsGranted) {
                                    permissionsState.launchMultiplePermissionRequest()
                                    return@RecordingControlButton
                                }
                                if (requiresAllFilesAccessPermission() && !Environment.isExternalStorageManager()) {
                                    allFilesAccessLauncher.launch(allFilesAccessIntent(context))
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar(
                                            context.getString(R.string.msg_grant_all_files_access)
                                        )
                                    }
                                    return@RecordingControlButton
                                }
                                viewModel.handleIntent(RecordingIntent.StartRecording)
                            },
                            onStop = { viewModel.handleIntent(RecordingIntent.StopRecording) }
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        IconButton(onClick = { showConfigDialog = true }) {
                            Icon(
                                Icons.Default.Settings,
                                contentDescription = stringResource(id = R.string.cd_settings)
                            )
                        }
                    }
                }
            }

        }
    }

    if (showConfigDialog) {
        ConfigDialog(
            currentConfig = state.config,
            onDismiss = { showConfigDialog = false },
            onConfirm = { config ->
                viewModel.handleIntent(RecordingIntent.UpdateConfig(config))
                showConfigDialog = false
            }
        )
    }
}

private fun runtimePermissions(): List<String> {
    val permissions = mutableListOf(Manifest.permission.RECORD_AUDIO)
    if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) {
        permissions += Manifest.permission.READ_EXTERNAL_STORAGE
        permissions += Manifest.permission.WRITE_EXTERNAL_STORAGE
    }
    return permissions
}

private fun requiresAllFilesAccessPermission(): Boolean {
    return Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
}

@SuppressLint("InlinedApi", "QueryPermissionsNeeded")
private fun allFilesAccessIntent(context: Context): Intent {
    val appSpecificIntent = Intent(
        Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
        "package:${context.packageName}".toUri()
    )
    val fallbackIntent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
    return if (appSpecificIntent.resolveActivity(context.packageManager) != null) {
        appSpecificIntent
    } else {
        fallbackIntent
    }.apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
}

@Composable
private fun ObserveEffects(
    viewModel: RecordingViewModel,
    snackbarHostState: SnackbarHostState
) {
    LaunchedEffect(viewModel, snackbarHostState) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is RecordingEffect.ShowError -> snackbarHostState.showSnackbar(effect.message)
                is RecordingEffect.ShowSuccess -> snackbarHostState.showSnackbar(effect.message)
            }
        }
    }
}

@Composable
private fun RecordingPanel(
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
private fun RecordingControlButton(
    isRecording: Boolean,
    onStart: () -> Unit,
    onStop: () -> Unit
) {
    if (isRecording) {
        Button(
            onClick = onStop,
        ) { Text(stringResource(id = R.string.btn_stop_recording)) }
    } else {
        Button(
            onClick = onStart,
        ) { Text(stringResource(id = R.string.btn_start_recording)) }
    }
}
