package nasonly.ui.screens

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
import nasonly.core.utils.formatTime
import nasonly.data.db.PlaybackHistory
import nasonly.navigation.Screen

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
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when {
                uiState.isLoading && historyList.isEmpty() -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator()
                        Text("加载历史记录中...", modifier = Modifier.padding(top = 16.dp))
                    }
                }

                historyList.isEmpty() && !uiState.isLoading -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "暂无播放记录",
                            style = MaterialTheme.typography.headlineMedium
                        )
                        Text(
                            text = "播放视频后会在这里显示历史记录",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }

                else -> {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(historyList) { history ->
                            HistoryItem(
                                history = history,
                                onItemClick = {
                                    navController.navigate(
                                        Screen.VideoPlayer.createRoute(
                                            videoId = history.videoId,
                                            videoUrl = history.videoUrl,
                                            position = history.lastPosition.toInt()
                                        )
                                    )
                                },
                                onDeleteClick = {
                                    videoToDelete = history.videoId
                                }
                            )
                        }
                    }
                }
            }

            // 错误提示
            if (uiState.errorMessage.isNotEmpty()) {
                Text(
                    text = uiState.errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                        .fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }
        }
    }

    // 清空所有历史确认对话框
    if (showClearAllDialog) {
        AlertDialog(
            onDismissRequest = { showClearAllDialog = false },
            title = { Text("清空播放历史") },
            text = { Text("确定要删除所有播放记录吗？此操作不可恢复。") },
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

    // 删除单个历史确认对话框
    videoToDelete?.let { videoId ->
        AlertDialog(
            onDismissRequest = { videoToDelete = null },
            title = { Text("删除记录") },
            text = { Text("确定要删除这条播放记录吗？") },
            confirmButton = {
                Button(onClick = {
                    viewModel.deleteHistory(videoId)
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

@Composable
private fun HistoryItem(
    history: PlaybackHistory,
    onItemClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onItemClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 缩略图
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Image(
                painter = rememberAsyncImagePainter(
                    model = if (history.thumbnailPath.isNullOrEmpty()) {
                        "https://picsum.photos/seed/${history.videoId}/200/150"
                    } else history.thumbnailPath
                ),
                contentDescription = history.videoName,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            // 播放进度指示
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(2.dp)
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
            ) {
                val progress = if (history.duration > 0) {
                    history.lastPosition.toFloat() / history.duration
                } else 0f

                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(progress)
                        .background(MaterialTheme.colorScheme.primary)
                )
            }
        }

        // 视频信息
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp)
        ) {
            Text(
                text = history.videoName,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${formatTime(history.lastPosition)} / ${formatTime(history.duration)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = formatHistoryTime(history.lastPlayedTime),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // 删除按钮
        IconButton(
            onClick = { onDeleteClick() },
            modifier = Modifier.size(40.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "删除记录"
            )
        }
    }
}

// 格式化历史时间显示
fun formatHistoryTime(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    val diffMinutes = diff / (60 * 1000)
    val diffHours = diff / (60 * 60 * 1000)
    val diffDays = diff / (24 * 60 * 60 * 1000)

    return when {
        diffMinutes < 60 -> "${diffMinutes}分钟前"
        diffHours < 24 -> "${diffHours}小时前"
        diffDays < 7 -> "${diffDays}天前"
        else -> "${diffDays / 7}周前"
    }
}