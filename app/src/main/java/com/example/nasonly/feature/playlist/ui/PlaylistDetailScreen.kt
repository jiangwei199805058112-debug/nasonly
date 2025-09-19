package nasonly.feature.playlist.ui

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
import androidx.compose.material.icons.filled.RepeatOne
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
import nasonly.core.ui.components.CircularProgressIndicator
import nasonly.core.utils.FileUtils
import nasonly.feature.playlist.domain.PlayMode
import nasonly.feature.playlist.viewmodel.PlaylistDetailViewModel
import nasonly.navigation.Screen

// 视频数据类
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
    var selectedVideos by remember { mutableStateOf<Set<String>>(emptySet()) }
    var draggedItemIndex by remember { mutableStateOf<Int?>(null) }
    var targetIndex by remember { mutableStateOf<Int?>(null) }

    // 初始化播放列表数据
    LaunchedEffect(playlistId) {
        viewModel.loadPlaylistData(playlistId)
    }

    // 处理拖动排序
    LaunchedEffect(draggedItemIndex, targetIndex) {
        if (draggedItemIndex != null && targetIndex != null && draggedItemIndex != targetIndex) {
            viewModel.reorderVideos(playlistId, draggedItemIndex!!, targetIndex!!)
            draggedItemIndex = null
            targetIndex = null
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(playlistName) },
                scrollBehavior = scrollBehavior,
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
                        // 编辑模式下的按钮
                        IconButton(onClick = {
                            selectedVideos = videos.map { it.id }.toSet()
                        }) {
                            Icon(
                                imageVector = Icons.Default.SelectAll,
                                contentDescription = "全选"
                            )
                        }
                        IconButton(onClick = {
                            viewModel.removeVideosFromPlaylist(playlistId, selectedVideos.toList())
                            selectedVideos = emptySet()
                        }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "删除所选",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                        IconButton(onClick = {
                            isEditing = false
                            selectedVideos = emptySet()
                        }) {
                            Icon(
                                imageVector = Icons.Default.Done,
                                contentDescription = "完成编辑"
                            )
                        }
                    } else {
                        // 普通模式下的按钮
                        IconButton(onClick = { isEditing = true }) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "编辑"
                            )
                        }
                        IconButton(onClick = { viewModel.clearPlaylist(playlistId) }) {
                            Icon(
                                imageVector = Icons.Default.ClearAll,
                                contentDescription = "清空播放列表"
                            )
                        }
                        IconButton(onClick = {
                            viewModel.deletePlaylist(playlistId)
                            navController.popBackStack()
                        }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "删除播放列表"
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
                .nestedScroll(scrollBehavior.nestedScrollConnection)
        ) {
            when {
                uiState.isLoading -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator()
                        Text("加载中...", modifier = Modifier.padding(top = 16.dp))
                    }
                }

                videos.isEmpty() -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text("播放列表为空")
                        IconButton(
                            onClick = {
                                navController.navigate(Screen.MediaLibrary.createRoute(playlistId))
                            },
                            modifier = Modifier.padding(top = 16.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "添加视频"
                                )
                                Text("添加视频", modifier = Modifier.padding(start = 8.dp))
                            }
                        }
                    }
                }

                else -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // 播放模式设置
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("播放设置")

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { viewModel.togglePlayMode() }) {
                                    Icon(
                                        imageVector = when(playMode) {
                                            PlayMode.SEQUENTIAL -> Icons.Default.Repeat
                                            PlayMode.LOOP_ALL -> Icons.Default.Repeat
                                            PlayMode.LOOP_ONE -> Icons.Default.RepeatOne
                                        },
                                        contentDescription = when(playMode) {
                                            PlayMode.SEQUENTIAL -> "顺序播放"
                                            PlayMode.LOOP_ALL -> "循环播放"
                                            PlayMode.LOOP_ONE -> "单曲循环"
                                        }
                                    )
                                }
                                Text(
                                    text = when(playMode) {
                                        PlayMode.SEQUENTIAL -> "顺序播放"
                                        PlayMode.LOOP_ALL -> "循环播放"
                                        PlayMode.LOOP_ONE -> "单曲循环"
                                    },
                                    style = MaterialTheme.typography.bodySmall
                                )

                                Switch(
                                    checked = playMode != PlayMode.SEQUENTIAL,
                                    onCheckedChange = { viewModel.toggleLoopMode() }
                                )
                            }
                        }

                        // 视频列表（支持拖动排序）
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(videos, key = { it.id }) { video ->
                                val isSelected = selectedVideos.contains(video.id)
                                val isDragging = draggedItemIndex == videos.indexOf(video)
                                val alpha by animateFloatAsState(if (isDragging) 0.5f else 1f)

                                ListItem(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .alpha(alpha)
                                        .background(
                                            if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                            else MaterialTheme.colorScheme.background
                                        )
                                        .pointerInput(Unit) {
                                            if (isEditing) {
                                                detectDragGesturesAfterLongPress(
                                                    onDragStart = {
                                                        draggedItemIndex = videos.indexOf(video)
                                                    },
                                                    onDragEnd = {
                                                        draggedItemIndex = null
                                                        targetIndex = null
                                                    },
                                                    onDrag = { change, dragAmount ->
                                                        change.consume()
                                                        val currentIndex = videos.indexOf(video)
                                                        val newIndex = currentIndex +
                                                                if (dragAmount.y > 0) 1 else -1

                                                        if (newIndex in videos.indices && newIndex != currentIndex) {
                                                            targetIndex = newIndex
                                                        }
                                                    }
                                                )
                                            }
                                        }
                                        .clickable {
                                            if (isEditing) {
                                                selectedVideos = if (isSelected) {
                                                    selectedVideos - video.id
                                                } else {
                                                    selectedVideos + video.id
                                                }
                                            } else {
                                                // 播放视频
                                                navController.navigate(
                                                    Screen.VideoPlayer.createRoute(
                                                        videoId = video.id,
                                                        videoUrl = video.url,
                                                        playlistId = playlistId,
                                                        position = videos.indexOf(video)
                                                    )
                                                )
                                            }
                                        },
                                    headlineContent = {
                                        Text(
                                            text = video.name,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    },
                                    supportingContent = {
                                        Text(
                                            text = FileUtils.formatFileSize(video.size),
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    },
                                    leadingContent = {
                                        if (isEditing) {
                                            Icon(
                                                imageVector = if (isSelected) Icons.Default.CheckCircle
                                                else Icons.Default.CircleOutline,
                                                contentDescription = if (isSelected) "已选择" else "未选择"
                                            )
                                        } else {
                                            Text(
                                                text = "${videos.indexOf(video) + 1}",
                                                modifier = Modifier
                                                    .size(24.dp)
                                                    .padding(2.dp),
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    },
                                    trailingContent = {
                                        if (!isEditing) {
                                            IconButton(onClick = {
                                                navController.navigate(
                                                    Screen.VideoPlayer.createRoute(
                                                        videoId = video.id,
                                                        videoUrl = video.url,
                                                        playlistId = playlistId,
                                                        position = videos.indexOf(video)
                                                    )
                                                )
                                            }) {
                                                Icon(
                                                    imageVector = Icons.Default.PlayArrow,
                                                    contentDescription = "播放"
                                                )
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}