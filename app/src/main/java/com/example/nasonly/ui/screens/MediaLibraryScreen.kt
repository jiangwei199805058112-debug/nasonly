package com.example.nasonly.ui.screens

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
import com.example.nasonly.core.ui.components.ErrorDialog
import com.example.nasonly.core.ui.components.FilterOption
import com.example.nasonly.core.ui.components.SmbConnectionState
import com.example.nasonly.core.ui.components.SmbConnectionStatus
import com.example.nasonly.core.utils.FileUtils
import com.example.nasonly.data.repository.NasConfig
import com.example.nasonly.data.smb.SmbConnectionManager
import com.example.nasonly.navigation.Screen
import com.example.nasonly.ui.components.SearchAndFilterBar
import kotlinx.coroutines.delay

@Composable
fun MediaLibraryScreen(
    navController: NavController,
    playlistId: String? = null,
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
            val matchesSearch = searchQuery.isEmpty() ||
                    video.name.contains(searchQuery, ignoreCase = true)
            val matchesFilter = when (selectedFilter) {
                FilterOption.SIZE_ASC -> true // TODO: 实现大小排序
                FilterOption.SIZE_DESC -> true
                else -> true
            }
            matchesSearch && matchesFilter
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("媒体库") },
                actions = {
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = "刷新")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { navController.navigate(Screen.CreatePlaylist.route) }) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "添加到播放列表")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // 搜索和筛选栏
            SearchAndFilterBar(
                searchQuery = searchQuery,
                onSearchQueryChanged = { searchQuery = it },
                selectedFilter = selectedFilter,
                onFilterSelected = { selectedFilter = it }
            )

            when {
                uiState.isLoading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }

                uiState.errorMessage != null -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = uiState.errorMessage,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                else -> {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        contentPadding = PaddingValues(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredVideos) { video ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Image(
                                    painter = rememberAsyncImagePainter(video.thumbnailPath),
                                    contentDescription = video.name,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.size(120.dp)
                                )
                                Text(
                                    text = video.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                                Text(
                                    text = FileUtils.formatFileSize(video.size),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // 连接状态提示
    SmbConnectionStatus(connectionState)

    if (showErrorDialog) {
        ErrorDialog(
            title = "错误",
            message = errorMessage,
            onDismiss = { showErrorDialog = false }
        )
    }

    // 模拟连接状态变化
    LaunchedEffect(Unit) {
        delay(2000)
        connectionState = SmbConnectionState.CONNECTED
    }
}
