package nasonly.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import nasonly.core.ui.components.ErrorDialog
import nasonly.core.ui.components.FilterOption
import nasonly.core.ui.components.SmbConnectionState
import nasonly.core.ui.components.SmbConnectionStatus
import nasonly.core.utils.FileUtils
import nasonly.data.repository.NasConfig
import nasonly.data.smb.SmbConnectionManager
import nasonly.navigation.Screen
import nasonly.ui.components.SearchAndFilterBar
import kotlinx.coroutines.delay

@Composable
fun MediaLibraryScreen(
    navController: NavController,
    playlistId: String?,
    viewModel: MediaLibraryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val allVideos by viewModel.videos.collectAsStateWithLifecycle(emptyList())

    // 搜索和筛选状态
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf<FilterOption>(FilterOption.NONE) }

    // 连接状态管理
    var connectionState by remember { mutableStateOf(SmbConnectionState.CONNECTED) }
    var showErrorDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    // 过滤后的视频列表
    val filteredVideos = remember(allVideos, searchQuery, selectedFilter) {
        allVideos.filter { video ->
            // 搜索过滤
            val matchesSearch = searchQuery.isEmpty() ||
                    video.name.contains(searchQuery, ignoreCase = true) ||
                    video.path.contains(searchQuery, ignoreCase = true)

            // 筛选过滤
            val matchesFilter = when (selectedFilter) {
                FilterOption.NONE -> true
                FilterOption.NAME_ASC -> true // 仅影响排序
                FilterOption.NAME_DESC -> true // 仅影响排序
                FilterOption.SIZE_ASC -> true // 仅影响排序
                FilterOption.SIZE_DESC -> true // 仅影响排序
                FilterOption.LARGE_FILES -> video.size >= 1024 * 1024 * 1024 // 1GB
            }

            matchesSearch && matchesFilter
        }.sortedWith(compareBy<VideoEntity> { it.name }.thenBy { it.size }).let { sortedList ->
            // 应用排序
            when (selectedFilter) {
                FilterOption.NAME_DESC -> sortedList.sortedByDescending { it.name }
                FilterOption.SIZE_ASC -> sortedList.sortedBy { it.size }
                FilterOption.SIZE_DESC -> sortedList.sortedByDescending { it.size }
                else -> sortedList
            }
        }
    }

    // 加载视频
    LaunchedEffect(Unit) {
        viewModel.loadVideos()
    }

    // 监听SMB连接状态
    LaunchedEffect(Unit) {
        val config = viewModel.getSavedNasConfig()
        if (config != null) {
            viewModel.registerConnectionListener(object : SmbConnectionManager.SmbConnectionListener {
                override fun onConnected(ip: String, port: Int) {
                    connectionState = SmbConnectionState.CONNECTED
                }

                override fun onConnectionFailed(ip: String, port: Int, error: String) {
                    connectionState = SmbConnectionState.ERROR
                    showErrorDialog = true
                    errorMessage = "连接NAS失败: $error"
                }

                override fun onOperationFailed(error: String, exception: Exception?) {
                    connectionState = SmbConnectionState.DISCONNECTED
                    showErrorDialog = true
                    errorMessage = "NAS操作失败: $error"
                }

                override fun onReconnected(ip: String, port: Int) {
                    connectionState = SmbConnectionState.RECONNECTED
                    // 3秒后恢复为普通连接状态
                    viewModel.viewModelScope.launch {
                        delay(3000)
                        connectionState = SmbConnectionState.CONNECTED
                    }
                }
            })
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("媒体库") },
                actions = {
                    IconButton(onClick = { viewModel.refreshVideos() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "刷新"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            if (playlistId != null) {
                FloatingActionButton(onClick = {
                    // 添加选中视频到播放列表
                    viewModel.addSelectedToPlaylist(playlistId) {
                        navController.popBackStack()
                    }
                }) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "添加到播放列表"
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize()) {
            // 连接状态提示
            SmbConnectionStatus(status = connectionState)

            // 搜索和筛选栏
            SearchAndFilterBar(
                searchQuery = searchQuery,
                onSearchQueryChanged = { searchQuery = it },
                selectedFilter = selectedFilter,
                onFilterSelected = { selectedFilter = it }
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when {
                    uiState.isLoading && allVideos.isEmpty() -> {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator()
                            Text("扫描视频中...", modifier = Modifier.padding(top = 16.dp))
                        }
                    }

                    filteredVideos.isEmpty() && !uiState.isLoading -> {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "未找到匹配的视频",
                                style = MaterialTheme.typography.headlineMedium
                            )
                            Text(
                                text = if (searchQuery.isNotEmpty())
                                    "尝试使用其他搜索关键词"
                                else
                                    "请检查NAS连接或视频路径",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    }

                    else -> {
                        // 视频网格列表
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(minSize = 150.dp),
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(filteredVideos) { video ->
                                VideoGridItem(
                                    video = video,
                                    isSelected = uiState.selectedVideoIds.contains(video.id),
                                    onItemClick = {
                                        if (playlistId != null) {
                                            // 选择模式 - 切换选中状态
                                            viewModel.toggleVideoSelection(video.id)
                                        } else {
                                            // 浏览模式 - 播放视频
                                            navController.navigate(
                                                Screen.VideoPlayer.createRoute(
                                                    videoId = video.id,
                                                    videoUrl = video.url
                                                )
                                            )
                                        }
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
    }

    // 连接错误对话框
    if (showErrorDialog) {
        ErrorDialog(
            title = "NAS连接错误",
            message = errorMessage,
            onDismiss = { showErrorDialog = false },
            onRetry = {
                showErrorDialog = false
                viewModel.refreshVideos() // 重试操作
            }
        )
    }
}

// 视频网格项
@Composable
private fun VideoGridItem(
    video: VideoEntity,
    isSelected: Boolean,
    onItemClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onItemClick() },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 视频缩略图
        Box(
            modifier = Modifier
                .size(120.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = rememberAsyncImagePainter(
                    model = if (video.thumbnailPath.isNullOrEmpty()) {
                        "https://picsum.photos/seed/${video.id}/200/150"
                    } else video.thumbnailPath
                ),
                contentDescription = video.name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            // 选中状态指示器
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .align(Alignment.TopEnd)
                        .background(MaterialTheme.colorScheme.primary)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "已选中",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }

        // 视频信息
        Text(
            text = video.name,
            modifier = Modifier
                .padding(top = 8.dp)
                .fillMaxWidth(),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodyMedium
        )

        Text(
            text = FileUtils.formatFileSize(video.size),
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// 视频实体类
data class VideoEntity(
    val id: String,
    val name: String,
    val url: String,
    val path: String,
    val size: Long,
    val duration: Long,
    val thumbnailPath: String?,
    val lastModified: Long
)

// 媒体库ViewModel接口定义（确保实现这些方法）
abstract class MediaLibraryViewModel {
    abstract val uiState: androidx.lifecycle.StateFlow<MediaLibraryUiState>
    abstract val videos: androidx.lifecycle.StateFlow<List<VideoEntity>>

    abstract fun loadVideos()
    abstract fun refreshVideos()
    abstract fun toggleVideoSelection(videoId: String)
    abstract fun addSelectedToPlaylist(playlistId: String, onComplete: () -> Unit)
    abstract suspend fun getSavedNasConfig(): NasConfig?
    abstract fun registerConnectionListener(listener: SmbConnectionManager.SmbConnectionListener)
    abstract val viewModelScope: kotlinx.coroutines.CoroutineScope
}

// 媒体库UI状态类
data class MediaLibraryUiState(
    val isLoading: Boolean = false,
    val errorMessage: String = "",
    val selectedVideoIds: Set<String> = emptySet()
)
