package com.example.nasonly.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController

@Composable
fun VideoPlayerScreen(
    navController: NavController,
    videoId: String,
    viewModel: VideoPlayerViewModel = hiltViewModel()
) {
    val currentVideo by viewModel.currentVideo.collectAsStateWithLifecycle()
    val lastPosition by viewModel.lastPlayedPosition.collectAsStateWithLifecycle()
    val smbContext by viewModel.smbContext.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = currentVideo?.name ?: "视频播放") }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = "视频 ID: $videoId",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )

            Text(
                text = "当前位置: ${lastPosition / 1000}s",
                style = MaterialTheme.typography.bodyMedium
            )

            Text(
                text = if (smbContext != null) "SMB 会话已就绪" else "正在建立 SMB 会话…",
                style = MaterialTheme.typography.bodyMedium
            )

            currentVideo?.let { video ->
                Text(
                    text = "来源: ${video.url}",
                    style = MaterialTheme.typography.bodySmall
                )

                Text(
                    text = "时长: ${video.duration / 1000}s",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Button(
                onClick = {
                    val duration = currentVideo?.duration ?: 0L
                    viewModel.updatePlaybackPosition(videoId, lastPosition + 10_000, duration)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "模拟更新播放进度")
            }

            Button(
                onClick = {
                    viewModel.onPlaybackCompleted(videoId)
                    navController.popBackStack()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "结束播放并返回")
            }
        }
    }
}
