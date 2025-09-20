package com.example.nasonly.core.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * SMB连接状态提示组件
 */
@Composable
fun SmbConnectionStatus(
    isVisible: Boolean,
    status: SmbStatus,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically { -it },
        exit = slideOutVertically { -it }
    ) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            when (status) {
                SmbStatus.CONNECTED -> {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Connected",
                        tint = Color.Green
                    )
                    Text(
                        text = "已连接 NAS",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
                SmbStatus.CONNECTING -> {
                    Icon(
                        imageVector = Icons.Default.CloudSync,
                        contentDescription = "Connecting",
                        tint = Color.Blue
                    )
                    Text(
                        text = "正在连接 NAS...",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
                SmbStatus.DISCONNECTED -> {
                    Icon(
                        imageVector = Icons.Default.CloudOff,
                        contentDescription = "Disconnected",
                        tint = Color.Red
                    )
                    Text(
                        text = "连接已断开",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
        }
    }
}

/**
 * SMB状态枚举
 */
enum class SmbStatus {
    CONNECTED,
    CONNECTING,
    DISCONNECTED
}
