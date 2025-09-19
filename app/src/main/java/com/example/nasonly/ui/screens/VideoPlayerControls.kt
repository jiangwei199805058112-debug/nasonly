package nasonly.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import nasonly.feature.playlist.domain.PlayMode

/**
 * 视频播放器控制组件
 */
@Composable
fun VideoPlayerControls(
    player: Player?,
    playMode: PlayMode,
    isFullScreen: Boolean,
    showControls: Boolean,
    onFullScreenToggle: () -> Unit,
    onPlayModeToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentPosition by remember { mutableFloatStateOf(0f) }
    val duration by remember { mutableFloatStateOf(0f) }
    val bufferedPosition by remember { mutableFloatStateOf(0f) }
    var volume by remember { mutableFloatStateOf(1f) }
    val isPlaying = player?.isPlaying ?: false

    // 更新播放器状态
    player?.let {
        volume = it.volume
        if (it.duration > 0) {
            currentPosition = it.currentPosition.toFloat() / it.duration
            duration = it.duration.toFloat()
        }
        if (it.bufferedPosition > 0 && it.duration > 0) {
            bufferedPosition = it.bufferedPosition.toFloat() / it.duration
        }
    }

    // 格式化时间显示
    fun formatTime(millis: Long): String {
        if (millis < 0) return "00:00"
        val totalSeconds = millis / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format("%02d:%02d", minutes, seconds)
    }

    AnimatedVisibility(
        visible = showControls,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.6f))
                .padding(16.dp)
        ) {
            // 进度条
            Column(modifier = Modifier.fillMaxWidth()) {
                Slider(
                    value = currentPosition,
                    onValueChange = { value ->
                        player?.seekTo((value * duration).toLong())
                    },
                    valueRange = 0f..1f,
                    modifier = Modifier.fillMaxWidth(),
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = formatTime(player?.currentPosition ?: 0),
                        color = Color.White,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = formatTime(player?.duration ?: 0),
                        color = Color.White,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 控制按钮
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // 音量控制
                Column(
                    modifier = Modifier.width(100.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = when {
                            volume == 0f -> Icons.Default.VolumeMute
                            volume < 0.5f -> Icons.Default.VolumeDown
                            else -> Icons.Default.VolumeUp
                        },
                        contentDescription = "音量",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                    Slider(
                        value = volume,
                        onValueChange = {
                            volume = it
                            player?.volume = it
                        },
                        valueRange = 0f..1f,
                        modifier = Modifier.fillMaxWidth(),
                        colors = SliderDefaults.colors(
                            thumbColor = Color.White,
                            activeTrackColor = Color.White
                        )
                    )
                }

                // 播放控制
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 快退
                    IconButton(
                        onClick = { player?.seekBack() },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FastRewind,
                            contentDescription = "快退",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // 播放/暂停
                    IconButton(
                        onClick = {
                            if (isPlaying) player?.pause() else player?.play()
                        },
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "暂停" else "播放",
                            tint = Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // 快进
                    IconButton(
                        onClick = { player?.seekForward() },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FastForward,
                            contentDescription = "快进",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                // 其他控制
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 播放模式
                    IconButton(
                        onClick = onPlayModeToggle,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = when (playMode) {
                                PlayMode.SEQUENTIAL -> Icons.Default.Repeat
                                PlayMode.LOOP_ALL -> Icons.Default.Repeat
                                PlayMode.LOOP_ONE -> Icons.Default.RepeatOne
                            },
                            contentDescription = when (playMode) {
                                PlayMode.SEQUENTIAL -> "顺序播放"
                                PlayMode.LOOP_ALL -> "循环播放"
                                PlayMode.LOOP_ONE -> "单曲循环"
                            },
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // 全屏切换
                    IconButton(
                        onClick = onFullScreenToggle,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = if (isFullScreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                            contentDescription = if (isFullScreen) "退出全屏" else "进入全屏",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }
}