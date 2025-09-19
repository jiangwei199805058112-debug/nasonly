package nasonly.core.ui.components

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
    status: SmbConnectionState,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = status != SmbConnectionState.CONNECTED || status == SmbConnectionState.RECONNECTED,
        enter = slideInVertically(initialOffsetY = { -it }),
        exit = slideOutVertically(targetOffsetY = { -it }),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    when (status) {
                        SmbConnectionState.CONNECTING -> Color(0xFF6495ED) // 蓝色
                        SmbConnectionState.CONNECTED,
                        SmbConnectionState.RECONNECTED -> Color(0xFF32CD32) // 绿色
                        SmbConnectionState.DISCONNECTED,
                        SmbConnectionState.ERROR -> Color(0xFFFF6347) // 红色
                    }
                )
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = when (status) {
                    SmbConnectionState.CONNECTING -> Icons.Default.CloudSync
                    SmbConnectionState.CONNECTED,
                    SmbConnectionState.RECONNECTED -> Icons.Default.CheckCircle
                    SmbConnectionState.DISCONNECTED,
                    SmbConnectionState.ERROR -> Icons.Default.CloudOff
                },
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )

            Text(
                text = when (status) {
                    SmbConnectionState.CONNECTING -> "正在连接到NAS..."
                    SmbConnectionState.CONNECTED -> "已连接到NAS"
                    SmbConnectionState.RECONNECTED -> "已重新连接到NAS"
                    SmbConnectionState.DISCONNECTED -> "已与NAS断开连接"
                    SmbConnectionState.ERROR -> "NAS连接错误"
                },
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}

// SMB连接状态枚举
enum class SmbConnectionState {
    CONNECTING,    // 正在连接
    CONNECTED,     // 已连接
    RECONNECTED,   // 已重新连接
    DISCONNECTED,  // 已断开
    ERROR          // 错误
}