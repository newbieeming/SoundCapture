package com.xmbest.soundcapture.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.MultiplePermissionsState
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.xmbest.soundcapture.R
import com.xmbest.soundcapture.presentation.RecordingEffect
import com.xmbest.soundcapture.presentation.RecordingIntent
import com.xmbest.soundcapture.presentation.RecordingState
import com.xmbest.soundcapture.presentation.RecordingViewModel
import com.xmbest.soundcapture.ui.components.ConfigDialog
import com.xmbest.soundcapture.ui.components.RecordingListItem
import com.xmbest.soundcapture.ui.components.WaveformView
import kotlinx.coroutines.launch

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun RecordingScreen(
    viewModel: RecordingViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    val panelWeight = if (isLandscape) 0.6f else 0.36f
    val listWeight = if (isLandscape) 0.4f else 0.64f
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
        snackbarHostState = snackbarHostState,
        permissionsState = permissionsState
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(id = R.string.title_recording_app)) },
                actions = {
                    IconButton(onClick = { showConfigDialog = true }) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = stringResource(id = R.string.cd_settings)
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            RecordingPanel(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(panelWeight),
                state = state,
                onStart = {
                    if (!permissionsState.allPermissionsGranted) {
                        permissionsState.launchMultiplePermissionRequest()
                        return@RecordingPanel
                    }
                    if (requiresAllFilesAccessPermission() && !Environment.isExternalStorageManager()) {
                        allFilesAccessLauncher.launch(allFilesAccessIntent(context))
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar(
                                context.getString(R.string.msg_grant_all_files_access)
                            )
                        }
                        return@RecordingPanel
                    }
                    viewModel.handleIntent(RecordingIntent.StartRecording)
                },
                onStop = { viewModel.handleIntent(RecordingIntent.StopRecording) }
            )
            RecordingList(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(listWeight),
                state = state,
                onDelete = { id -> viewModel.handleIntent(RecordingIntent.DeleteRecording(id)) },
                onRename = { id, name ->
                    viewModel.handleIntent(RecordingIntent.RenameRecording(id, name))
                }
            )
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

@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun ObserveEffects(
    viewModel: RecordingViewModel,
    snackbarHostState: SnackbarHostState,
    permissionsState: MultiplePermissionsState
) {
    LaunchedEffect(viewModel, snackbarHostState, permissionsState) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is RecordingEffect.ShowError -> snackbarHostState.showSnackbar(effect.message)
                is RecordingEffect.ShowSuccess -> snackbarHostState.showSnackbar(effect.message)
                is RecordingEffect.RequestPermission -> permissionsState.launchMultiplePermissionRequest()
            }
        }
    }
}

@Composable
private fun RecordingPanel(
    modifier: Modifier = Modifier,
    state: RecordingState,
    onStart: () -> Unit,
    onStop: () -> Unit
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            WaveformView(
                channelLevels = state.channelLevels,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 96.dp)
                    .weight(1f),
                maxBars = 100,
                barWidth = 2.dp,
                barGap = 4.dp,
                scrollDurationMs = 120,
                amplitudeScale = state.config.waveformScale,
                channelCount = state.config.waveformChannelCount,
                isActive = state.isRecording
            )
            Spacer(modifier = Modifier.height(8.dp))
            RecordingControlButton(
                isRecording = state.isRecording,
                onStart = onStart,
                onStop = onStop
            )
        }
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

@SuppressLint("SdCardPath")
@Composable
private fun RecordingList(
    modifier: Modifier = Modifier,
    state: RecordingState,
    onDelete: (String) -> Unit,
    onRename: (String, String) -> Unit
) {
    Column(modifier = modifier) {
        Text(
            text = stringResource(
                id = R.string.section_recordings_with_path,
                "/sdcard/SoundCapture"
            ),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(16.dp)
        )
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(state.recordings) { recording ->
                RecordingListItem(
                    recording = recording,
                    onDelete = { onDelete(recording.id) },
                    onRename = { newName -> onRename(recording.id, newName) }
                )
            }
        }
    }
}
