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
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.ElevatedCard
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.newbieeming.soundcapture.R
import com.newbieeming.soundcapture.data.repository.RecordingRepository
import com.newbieeming.soundcapture.presentation.RecordingEffect
import com.newbieeming.soundcapture.presentation.RecordingIntent
import com.newbieeming.soundcapture.presentation.RecordingState
import com.newbieeming.soundcapture.presentation.RecordingViewModel
import com.newbieeming.soundcapture.ui.components.ConfigDialog
import com.newbieeming.soundcapture.ui.components.WaveformView

@RequiresApi(Build.VERSION_CODES.S)
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
                hasFilePermission =
                    !requiresAllFilesAccessPermission() || Environment.isExternalStorageManager()
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
                            .weight(0.75f),
                        state = state
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Card(modifier = Modifier
                        .fillMaxHeight()
                        .weight(0.25f)) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            // 录音参数展示 - 顶部
                            RecordingParamsInfo(config = state.config)

                            // 中间弹性空间
                            Spacer(modifier = Modifier.weight(1f))

                            // 录制时长
                            RecordingDuration(
                                isRecording = state.isRecording,
                                durationMs = state.recordingDurationMs
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // 录音按钮 - 靠近底部
                            RecordingControlButton(
                                isRecording = state.isRecording,
                                onStart = { viewModel.handleIntent(RecordingIntent.StartRecording) },
                                onStop = { viewModel.handleIntent(RecordingIntent.StopRecording) }
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            // 设置按钮 - 最底部
                            FilledTonalButton(
                                onClick = { showConfigDialog = true },
                                shape = RoundedCornerShape(32.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    Icons.Default.Settings,
                                    contentDescription = stringResource(id = R.string.cd_settings),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(stringResource(id = R.string.cd_settings))
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
        // 横屏布局 - 左右排列
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 左侧 - 插画区
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                PermissionIllustration()
            }

            Spacer(modifier = Modifier.width(32.dp))

            // 右侧 - 权限卡片
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                PermissionCards(
                    hasRecordPermission = hasRecordPermission,
                    isRecordPermanentlyDenied = isRecordPermanentlyDenied,
                    needsFilePermission = needsFilePermission,
                    onRequestRecordPermission = onRequestRecordPermission,
                    onOpenAppSettings = onOpenAppSettings,
                    onOpenFileSettings = onOpenFileSettings
                )
            }
        }
}

@Composable
private fun PermissionIllustration() {
    Icon(
        imageVector = Icons.Default.Mic,
        contentDescription = null,
        modifier = Modifier.size(80.dp),
        tint = MaterialTheme.colorScheme.primary
    )
    Spacer(modifier = Modifier.height(24.dp))
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(7) { index ->
            val height = when (index) {
                0, 6 -> 12.dp
                1, 5 -> 24.dp
                2, 4 -> 36.dp
                3 -> 48.dp
                else -> 12.dp
            }
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(height)
                    .clip(MaterialTheme.shapes.small)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.6f))
            )
        }
    }
    Spacer(modifier = Modifier.height(32.dp))
    Text(
        text = stringResource(id = R.string.permission_title),
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.Bold
    )
    Spacer(modifier = Modifier.height(12.dp))
    Text(
        text = stringResource(id = R.string.permission_subtitle),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
    )
    Spacer(modifier = Modifier.height(24.dp))
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Lock,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = stringResource(id = R.string.permission_security_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
    }
}

@Composable
private fun PermissionCards(
    hasRecordPermission: Boolean,
    isRecordPermanentlyDenied: Boolean,
    needsFilePermission: Boolean,
    onRequestRecordPermission: () -> Unit,
    onOpenAppSettings: () -> Unit,
    onOpenFileSettings: () -> Unit
) {
    PermissionCard(
        icon = Icons.Default.Mic,
        title = stringResource(id = R.string.permission_record_title),
        description = stringResource(id = R.string.permission_record_desc),
        isGranted = hasRecordPermission,
        actionText = if (isRecordPermanentlyDenied) stringResource(id = R.string.btn_go_authorize) else null,
        onClick = {
            if (isRecordPermanentlyDenied) onOpenAppSettings() else onRequestRecordPermission()
        }
    )
    PermissionCard(
        icon = Icons.Default.Folder,
        title = stringResource(id = R.string.permission_file_title),
        description = stringResource(id = R.string.permission_file_desc),
        isGranted = !needsFilePermission,
        actionText = stringResource(id = R.string.btn_go_authorize),
        onClick = onOpenFileSettings
    )
}

@Composable
private fun PermissionCard(
    icon: ImageVector,
    title: String,
    description: String,
    isGranted: Boolean,
    actionText: String?,
    onClick: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(20.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge.copy(fontSize = 18.sp),
                    fontWeight = FontWeight.Bold,
                    color = if (isGranted) {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            if (isGranted) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            } else {
                Button(
                    onClick = onClick,
                    shape = RoundedCornerShape(32.dp),
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
                ) {
                    Text(actionText ?: stringResource(id = R.string.btn_grant))
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
private fun RecordingParamsInfo(config: com.newbieeming.soundcapture.data.model.RecordingConfig) {
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
private fun RecordingDuration(
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
