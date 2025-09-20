package com.example.nasonly.ui.screens

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
import com.example.nasonly.feature.playlist.domain.PlayMode

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
    var currentPosition by remember { mutableFloatStateOf(0f) }
    var duration by remember { mutableFloatStateOf(0f) }
    var bufferedPosition by remember { mutableFloatStateOf(0f) }
    var volume by remember { mutableFloatStateOf(1f) }
    val isPlaying = player?.isPlaying ?: false

    // 更新播放器状态
    player?.let {
        volume = it.volume
        if (it.duration > 0) {
            currentPosition = it.currentPosition.toFloat() / it.duration
            duration = it.duration.toFloat()
            bufferedPosition = it.bufferedPosition.toFloat() / it.duration
        }
    }

    AnimatedVisibility(
        visible = showControls,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 播放进度条
            val interactionSource = remember { MutableInteractionSource() }
            val isDragging = interactionSource.collectIsDraggedAsState().value

            Slider(
                value = currentPosition,
                onValueChange = { newValue ->
                    currentPosition = newValue
                },
                onValueChangeFinished = {
                    player?.seekTo((currentPosition * duration).toLong())
                },
                modifier = Modifier.fillMaxWidth(),
                valueRange = 0f..1f,
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = Color.Gray
                ),
                interactionSource = interactionSource
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 控制按钮
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceEvenly
            ) {
                IconButton(onClick = { player?.seekBack() }) {
                    Icon(Icons.Default.FastRewind, contentDescription = "快退")
                }

                IconButton(onClick = {
                    if (isPlaying) player?.pause() else player?.play()
                }) {
                    Icon(
                        if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "暂停" else "播放"
                    )
                }

                IconButton(onClick = { player?.seekForward() }) {
                    Icon(Icons.Default.FastForward, contentDescription = "快进")
                }

                IconButton(onClick = onPlayModeToggle) {
                    Icon(
                        when (playMode) {
                            PlayMode.SEQUENTIAL -> Icons.Default.Repeat
                            PlayMode.LOOP_ALL -> Icons.Default.Repeat
                            PlayMode.LOOP_ONE -> Icons.Default.RepeatOne
                        },
                        contentDescription = "播放模式"
                    )
                }

                IconButton(onClick = onFullScreenToggle) {
                    Icon(
                        if (isFullScreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                        contentDescription = if (isFullScreen) "退出全屏" else "全屏"
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 音量控制
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center
            ) {
                Icon(
                    imageVector = when {
                        volume == 0f -> Icons.Default.VolumeMute
                        volume < 0.5f -> Icons.Default.VolumeDown
                        else -> Icons.Default.VolumeUp
                    },
                    contentDescription = "音量",
                    modifier = Modifier.size(24.dp)
                )
                Slider(
                    value = volume,
                    onValueChange = { newValue ->
                        volume = newValue
                        player?.volume = newValue
                    },
                    valueRange = 0f..1f,
                    modifier = Modifier.width(150.dp)
                )
            }
        }
    }
}
