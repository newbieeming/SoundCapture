package com.newbieeming.soundcapture.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.newbieeming.soundcapture.R

@Composable
internal fun PermissionScreen(
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
