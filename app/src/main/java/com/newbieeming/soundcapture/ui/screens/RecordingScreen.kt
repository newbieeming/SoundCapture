package com.newbieeming.soundcapture.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.newbieeming.soundcapture.R
import com.newbieeming.soundcapture.data.model.RecordingItem
import com.newbieeming.soundcapture.presentation.RecordingEffect
import com.newbieeming.soundcapture.presentation.RecordingIntent
import com.newbieeming.soundcapture.presentation.RecordingState
import com.newbieeming.soundcapture.presentation.RecordingViewModel
import com.newbieeming.soundcapture.ui.components.ConfigDialog
import com.newbieeming.soundcapture.ui.components.RecordingListItem
import kotlinx.coroutines.flow.conflate

@Stable
private data class RecordingPermissionState(
    val hasRecordPermission: Boolean,
    val isRecordPermanentlyDenied: Boolean,
    val needsFilePermission: Boolean,
    val onRequestRecordPermission: () -> Unit,
    val onOpenAppSettings: () -> Unit,
    val onOpenFileSettings: () -> Unit
)

@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun rememberRecordingPermissionState(
    onOpenFileSettings: () -> Unit,
    onOpenAppSettings: () -> Unit
): RecordingPermissionState {
    var hasFilePermission by remember {
        mutableStateOf(!requiresAllFilesAccessPermission() || Environment.isExternalStorageManager())
    }
    var hasRequestedRecordPermission by remember { mutableStateOf(false) }
    var hasAutoRequested by remember { mutableStateOf(false) }

    // 每次页面回到前台时重新检查文件权限
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasFilePermission =
                    !requiresAllFilesAccessPermission() || Environment.isExternalStorageManager()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val recordPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasRequestedRecordPermission = true
        if (permissions.values.all { it }) {
            hasRequestedRecordPermission = false
        }
    }

    val permissionsState = rememberMultiplePermissionsState(permissions = runtimePermissions())
    val hasRecordPermission = permissionsState.allPermissionsGranted
    val isPermanentlyDenied = hasRequestedRecordPermission
            && !hasRecordPermission
            && !permissionsState.shouldShowRationale

    // 首次进入权限页面且未永久拒绝时，自动请求一次麦克风权限
    LaunchedEffect(hasRecordPermission, isPermanentlyDenied) {
        if (!hasRecordPermission && !isPermanentlyDenied && !hasAutoRequested) {
            hasAutoRequested = true
            recordPermissionLauncher.launch(runtimePermissions().toTypedArray())
        }
    }

    return RecordingPermissionState(
        hasRecordPermission = hasRecordPermission,
        isRecordPermanentlyDenied = isPermanentlyDenied,
        needsFilePermission = !hasFilePermission,
        onRequestRecordPermission = { recordPermissionLauncher.launch(runtimePermissions().toTypedArray()) },
        onOpenAppSettings = onOpenAppSettings,
        onOpenFileSettings = onOpenFileSettings
    )
}

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun RecordingScreen(
    viewModel: RecordingViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showConfigDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    val allFilesAccessLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { }

    val permissionState = rememberRecordingPermissionState(
        onOpenFileSettings = { allFilesAccessLauncher.launch(allFilesAccessIntent(context)) },
        onOpenAppSettings = { context.openAppSettings() }
    )

    ObserveEffects(
        viewModel = viewModel,
        snackbarHostState = snackbarHostState,
        allFilesAccessLauncher = allFilesAccessLauncher
    )

    if (!permissionState.hasRecordPermission || permissionState.needsFilePermission) {
        PermissionScreen(
            hasRecordPermission = permissionState.hasRecordPermission,
            isRecordPermanentlyDenied = permissionState.isRecordPermanentlyDenied,
            needsFilePermission = permissionState.needsFilePermission,
            onRequestRecordPermission = permissionState.onRequestRecordPermission,
            onOpenAppSettings = permissionState.onOpenAppSettings,
            onOpenFileSettings = permissionState.onOpenFileSettings
        )
    } else {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    RecordingPanel(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(0.65f),
                        state = state
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    RecordingsPanel(
                        modifier = Modifier
                            .fillMaxHeight()
                            .weight(0.35f),
                        state = state,
                        viewModel = viewModel,
                        onSettingsClick = { showConfigDialog = true }
                    )
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
}

@Composable
private fun RecordingsPanel(
    state: RecordingState,
    viewModel: RecordingViewModel,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(id = R.string.section_recordings),
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp)
            )

            RecordingsList(
                recordings = state.recordings,
                isAnyPlaying = state.isPlaying,
                currentPlayingId = state.currentPlayingId,
                viewModel = viewModel
            )

            RecordingControlButton(
                isRecording = state.isRecording,
                onStart = { viewModel.handleIntent(RecordingIntent.StartRecording) },
                onStop = { viewModel.handleIntent(RecordingIntent.StopRecording) }
            )

            Spacer(modifier = Modifier.height(8.dp))

            SettingsButton(onClick = onSettingsClick)
        }
    }
}

@Composable
private fun ColumnScope.RecordingsList(
    recordings: List<RecordingItem>,
    isAnyPlaying: Boolean,
    currentPlayingId: String?,
    viewModel: RecordingViewModel
) {
    if (recordings.isEmpty()) {
        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(id = R.string.msg_no_recordings),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    } else {
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(recordings) { recording ->
                RecordingListItem(
                    recording = recording,
                    onDelete = {
                        viewModel.handleIntent(RecordingIntent.DeleteRecording(recording.id))
                    },
                    onRename = { newName ->
                        viewModel.handleIntent(RecordingIntent.RenameRecording(recording.id, newName))
                    },
                    isPlaying = isAnyPlaying && currentPlayingId == recording.id,
                    onPlay = {
                        if (isAnyPlaying && currentPlayingId == recording.id) {
                            viewModel.handleIntent(RecordingIntent.StopPlayback)
                        } else {
                            viewModel.handleIntent(RecordingIntent.PlayRecording(recording))
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun SettingsButton(onClick: () -> Unit) {
    FilledTonalButton(
        onClick = onClick,
        shape = RoundedCornerShape(32.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            Icons.Default.Settings,
            contentDescription = stringResource(id = R.string.btn_param_settings),
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(stringResource(id = R.string.btn_param_settings))
    }
}

@Composable
private fun ObserveEffects(
    viewModel: RecordingViewModel,
    snackbarHostState: SnackbarHostState,
    allFilesAccessLauncher: ActivityResultLauncher<Intent>
) {
    val context = LocalContext.current
    LaunchedEffect(viewModel, snackbarHostState) {
        viewModel.effect.conflate().collect { effect ->
            snackbarHostState.currentSnackbarData?.dismiss()
            when (effect) {
                is RecordingEffect.ShowError -> {
                    snackbarHostState.showSnackbar(effect.message)
                }

                is RecordingEffect.ShowSuccess -> {
                    snackbarHostState.showSnackbar(effect.message)
                }

                is RecordingEffect.ShowSaveSuccess -> {
                    snackbarHostState.showSnackbar(
                        message = context.getString(R.string.msg_save_success, effect.filePath),
                        duration = SnackbarDuration.Short
                    )
                }

                is RecordingEffect.ShowSaveFailure -> {
                    val result = snackbarHostState.showSnackbar(
                        message = context.getString(R.string.msg_save_failed),
                        actionLabel = context.getString(R.string.btn_go_authorize),
                        duration = SnackbarDuration.Short
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        allFilesAccessLauncher.launch(allFilesAccessIntent(context))
                    }
                }
            }
        }
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

private fun Context.openAppSettings() {
    val intent = Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.fromParts("package", packageName, null)
    )
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    startActivity(intent)
}
