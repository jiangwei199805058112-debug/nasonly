package com.example.nasonly.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.nasonly.core.utils.formatTime
import com.example.nasonly.data.db.PlaybackHistory
import com.example.nasonly.navigation.Screen

@Composable
fun PlaybackHistoryScreen(
    navController: NavController,
    viewModel: PlaybackHistoryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val historyList by viewModel.historyList.collectAsStateWithLifecycle(emptyList())
    var showClearAllDialog by remember { mutableStateOf(false) }
    var videoToDelete by remember { mutableStateOf<String?>(null) }

    // 加载播放历史
    LaunchedEffect(Unit) {
        viewModel.loadPlaybackHistory()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("播放历史") },
                actions = {
                    if (historyList.isNotEmpty()) {
                        IconButton(onClick = { showClearAllDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.ClearAll,
                                contentDescription = "清空历史"
                            )
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }

                uiState.errorMessage != null -> {
                    Text(
                        text = uiState.errorMessage ?: "加载失败",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                historyList.isEmpty() -> {
                    Text(
                        text = "暂无播放历史",
                        modifier = Modifier.align(Alignment.Center),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(historyList) { history ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Image(
                                    painter = rememberAsyncImagePainter(history.thumbnailPath),
                                    contentDescription = history.videoName,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(CircleShape)
                                )
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(horizontal = 8.dp)
                                ) {
                                    Text(
                                        text = history.videoName,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                    Text(
                                        text = "上次观看至 ${formatTime(history.lastPosition)} / ${formatTime(history.duration)}",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                                IconButton(onClick = { videoToDelete = history.videoId }) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "删除"
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // 清空所有历史对话框
    if (showClearAllDialog) {
        AlertDialog(
            onDismissRequest = { showClearAllDialog = false },
            title = { Text("确认清空历史") },
            text = { Text("确定要清空所有播放历史吗？") },
            confirmButton = {
                Button(onClick = {
                    viewModel.clearAllHistory()
                    showClearAllDialog = false
                }) {
                    Text("确认")
                }
            },
            dismissButton = {
                Button(onClick = { showClearAllDialog = false }) {
                    Text("取消")
                }
            }
        )
    }

    // 删除单个历史对话框
    if (videoToDelete != null) {
        AlertDialog(
            onDismissRequest = { videoToDelete = null },
            title = { Text("删除历史") },
            text = { Text("确定要删除该条历史记录吗？") },
            confirmButton = {
                Button(onClick = {
                    viewModel.deleteHistory(videoToDelete!!)
                    videoToDelete = null
                }) {
                    Text("确认")
                }
            },
            dismissButton = {
                Button(onClick = { videoToDelete = null }) {
                    Text("取消")
                }
            }
        )
    }
}
