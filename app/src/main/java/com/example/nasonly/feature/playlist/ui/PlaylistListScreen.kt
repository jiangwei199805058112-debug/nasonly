package com.example.nasonly.feature.playlist.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.icons.Icons
import androidx.compose.material3.icons.filled.Add
import androidx.compose.material3.icons.filled.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.nasonly.feature.playlist.PlaylistListViewModel

/**
 * 播放列表页
 * 说明：
 *  - 该文件只有一处 import（就在文件顶部），避免 “imports are only allowed in the beginning of file” 的错误。
 *  - 为了避免关联错误，这里不强依赖 viewModel 的字段；仅保留默认的 hilt 注入签名，便于与现有调用兼容。
 */
@Composable
fun PlaylistListScreen(
    viewModel: PlaylistListViewModel = hiltViewModel(),
    onCreateClick: () -> Unit = {},
    onItemClick: (Long) -> Unit = {},
    onRefreshClick: () -> Unit = {}
) {
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "播放列表") },
                actions = {
                    IconButton(onClick = onRefreshClick) {
                        Icon(imageVector = Icons.Filled.Refresh, contentDescription = "刷新")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onCreateClick) {
                Icon(imageVector = Icons.Filled.Add, contentDescription = "新建列表")
            }
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        // 为了让此文件在现阶段稳定通过编译，这里先放一个占位 UI。
        // 后面你可以把 items 替换为 viewModel 暴露的状态列表（比如 collectAsState 后的列表）。
        PlaceholderPlaylistList(modifier = Modifier.padding(innerPadding), onItemClick = onItemClick)
    }
}

@Composable
private fun PlaceholderPlaylistList(
    modifier: Modifier = Modifier,
    onItemClick: (Long) -> Unit = {}
) {
    val demo = listOf(
        1L to "示例歌单 A",
        2L to "示例歌单 B",
        3L to "示例歌单 C"
    )

    if (demo.isEmpty()) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(text = "暂无播放列表", style = MaterialTheme.typography.bodyLarge)
        }
    } else {
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(demo, key = { it.first }) { (id, title) ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onItemClick(id) }
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Text(text = title, style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = "点击进入（id=$id）",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}
