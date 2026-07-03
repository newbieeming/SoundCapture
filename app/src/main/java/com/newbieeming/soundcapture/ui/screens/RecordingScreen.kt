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
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
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

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun RecordingScreen(
    viewModel: RecordingViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showConfigDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    // 文件权限状态
    var hasFilePermission by remember {
        mutableStateOf(!requiresAllFilesAccessPermission() || Environment.isExternalStorageManager())
    }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                // 每次页面回到前台时重新检查文件权限
                hasFilePermission = !requiresAllFilesAccessPermission() || Environment.isExternalStorageManager()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    val allFilesAccessLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { }

    // 追踪是否已经请求过录音权限（用于判断是否被永久拒绝）
    var hasRequestedRecordPermission by remember { mutableStateOf(false) }

    val recordPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // 请求回调：标记已请求过，权限未全部授予时
        hasRequestedRecordPermission = true
        // 如果授权成功，重置标记
        if (permissions.values.all { it }) {
            hasRequestedRecordPermission = false
        }
    }

    val permissionsState = rememberMultiplePermissionsState(
        permissions = runtimePermissions()
    )

    ObserveEffects(
        viewModel = viewModel,
        snackbarHostState = snackbarHostState,
        allFilesAccessLauncher = allFilesAccessLauncher
    )

    val hasRecordPermission = permissionsState.allPermissionsGranted
    val needsFilePermission = !hasFilePermission
    val isRecordPermanentlyDenied = hasRequestedRecordPermission
        && !hasRecordPermission
        && !permissionsState.shouldShowRationale

    if (!hasRecordPermission || needsFilePermission) {
        PermissionScreen(
            hasRecordPermission = hasRecordPermission,
            isRecordPermanentlyDenied = isRecordPermanentlyDenied,
            needsFilePermission = needsFilePermission,
            onRequestRecordPermission = { recordPermissionLauncher.launch(runtimePermissions().toTypedArray()) },
            onOpenAppSettings = { context.openAppSettings() },
            onOpenFileSettings = { allFilesAccessLauncher.launch(allFilesAccessIntent(context)) }
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
                                onStart = { viewModel.handleIntent(RecordingIntent.StartRecording) },
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
}

@Composable
private fun PermissionScreen(
    hasRecordPermission: Boolean,
    isRecordPermanentlyDenied: Boolean,
    needsFilePermission: Boolean,
    onRequestRecordPermission: () -> Unit,
    onOpenAppSettings: () -> Unit,
    onOpenFileSettings: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        PermissionItem(
            label = stringResource(id = R.string.msg_need_record_permission),
            isGranted = hasRecordPermission,
            actionText = if (isRecordPermanentlyDenied) stringResource(id = R.string.btn_go_authorize) else null,
            onClick = {
                if (isRecordPermanentlyDenied) onOpenAppSettings() else onRequestRecordPermission()
            }
        )
        Spacer(modifier = Modifier.height(24.dp))
        PermissionItem(
            label = stringResource(id = R.string.msg_need_file_permission),
            isGranted = !needsFilePermission,
            actionText = stringResource(id = R.string.btn_go_authorize),
            onClick = onOpenFileSettings
        )
    }
}

@Composable
private fun PermissionItem(
    label: String,
    isGranted: Boolean,
    actionText: String?,
    onClick: () -> Unit
) {
    val textColor = if (isGranted) {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    val textDecoration = if (isGranted) TextDecoration.LineThrough else null

    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            color = textColor,
            textDecoration = textDecoration
        )
        Spacer(modifier = Modifier.width(8.dp))
        if (isGranted) {
            Text(
                text = stringResource(id = R.string.btn_authorized),
                style = MaterialTheme.typography.titleMedium,
                color = textColor,
                textDecoration = textDecoration
            )
        } else {
            Text(
                text = actionText ?: stringResource(id = R.string.btn_grant_permission),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable { onClick() }
            )
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

@Composable
private fun ObserveEffects(
    viewModel: RecordingViewModel,
    snackbarHostState: SnackbarHostState,
    allFilesAccessLauncher: androidx.activity.result.ActivityResultLauncher<Intent>
) {
    val context = LocalContext.current
    LaunchedEffect(viewModel, snackbarHostState) {
        viewModel.effect.collect { effect ->
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
