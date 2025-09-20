package com.example.nasonly.feature.playlist.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.CircleOutline
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.nasonly.core.ui.components.CircularProgressIndicator
import com.example.nasonly.core.utils.FileUtils
import com.example.nasonly.feature.playlist.domain.PlayMode
import com.example.nasonly.feature.playlist.viewmodel.PlaylistDetailViewModel
import com.example.nasonly.navigation.Screen

// 视频数据类（UI层使用的简化版）
data class VideoEntity(
    val id: String,
    val name: String,
    val url: String,
    val size: Long
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun PlaylistDetailScreen(
    navController: NavController,
    playlistId: String,
    viewModel: PlaylistDetailViewModel = hiltViewModel()
) {
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val playlistName by viewModel.playlistName.collectAsStateWithLifecycle()
    val videos by viewModel.videos.collectAsStateWithLifecycle(emptyList())
    val playMode by viewModel.playMode.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

    // 状态管理
    var isEditing by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(playlistName) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                },
                actions = {
                    if (isEditing) {
                        IconButton(onClick = { isEditing = false }) {
                            Icon(Icons.Default.Done, contentDescription = "完成编辑")
                        }
                    } else {
                        IconButton(onClick = { isEditing = true }) {
                            Icon(Icons.Default.Edit, contentDescription = "编辑")
                        }
                    }
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                uiState.isLoading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                uiState.errorMessage != null -> {
                    Text(
                        text = uiState.errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }
                else -> {
                    LazyColumn(
                        state = listState,
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(videos, key = { it.id }) { video ->
                            ListItem(
                                headlineContent = { Text(video.name) },
                                supportingContent = {
                                    Text(
                                        FileUtils.formatFileSize(video.size),
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                },
                                leadingContent = {
                                    Icon(
                                        imageVector = if (isEditing) Icons.Default.CircleOutline else Icons.Default.PlayArrow,
                                        contentDescription = null
                                    )
                                },
                                trailingContent = {
                                    if (isEditing) {
                                        IconButton(onClick = { viewModel.deleteVideo(video.id) }) {
                                            Icon(Icons.Default.Delete, contentDescription = "删除")
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .pointerInput(Unit) {
                                        detectDragGesturesAfterLongPress { _, _ ->
                                            // 拖拽排序逻辑可扩展
                                        }
                                    }
                            )
                        }
                    }
                }
            }
        }
    }

    // 播放模式控制
    LaunchedEffect(playMode) {
        // 这里可以加上根据 playMode 切换播放逻辑
    }
}
