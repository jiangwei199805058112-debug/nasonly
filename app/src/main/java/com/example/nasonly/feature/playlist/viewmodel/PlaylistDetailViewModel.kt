package nasonly.feature.playlist.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import nasonly.feature.playlist.domain.PlayMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// 视频数据类（与UI层一致）
data class VideoEntity(
    val id: String,
    val name: String,
    val url: String,
    val size: Long
)

class PlaylistDetailViewModel : ViewModel() {
    // UI状态
    private val _uiState = MutableStateFlow(PlaylistDetailUiState())
    val uiState: StateFlow<PlaylistDetailUiState> = _uiState.asStateFlow()

    // 播放列表名称
    private val _playlistName = MutableStateFlow("我的播放列表")
    val playlistName: StateFlow<String> = _playlistName.asStateFlow()

    // 视频列表
    private val _videos = MutableStateFlow<List<VideoEntity>>(emptyList())
    val videos: StateFlow<List<VideoEntity>> = _videos.asStateFlow()

    // 播放模式
    private val _playMode = MutableStateFlow(PlayMode.SEQUENTIAL)
    val playMode: StateFlow<PlayMode> = _playMode.asStateFlow()

    // 加载播放列表数据
    fun loadPlaylistData(playlistId: String) {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            // 模拟加载延迟
            kotlinx.coroutines.delay(1000)
            _videos.value = listOf(
                VideoEntity(
                    id = "1",
                    name = "测试视频1.mp4",
                    url = "smb://192.168.1.100/videos/test1.mp4",
                    size = 1024 * 1024 * 50 // 50MB
                ),
                VideoEntity(
                    id = "2",
                    name = "测试视频2.mkv",
                    url = "smb://192.168.1.100/videos/test2.mkv",
                    size = 1024 * 1024 * 100 // 100MB
                )
            )
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    // 重新排序视频
    fun reorderVideos(playlistId: String, fromIndex: Int, toIndex: Int) {
        val currentList = _videos.value.toMutableList()
        if (fromIndex in currentList.indices && toIndex in currentList.indices) {
            val item = currentList.removeAt(fromIndex)
            currentList.add(toIndex, item)
            _videos.value = currentList
        }
    }

    // 从播放列表移除视频
    fun removeVideosFromPlaylist(playlistId: String, videoIds: List<String>) {
        _videos.value = _videos.value.filterNot { it.id in videoIds }
    }

    // 清空播放列表
    fun clearPlaylist(playlistId: String) {
        _videos.value = emptyList()
    }

    // 删除播放列表
    fun deletePlaylist(playlistId: String) {
        // 实际项目中应包含数据库删除逻辑
    }

    // 切换播放模式
    fun togglePlayMode() {
        _playMode.value = when (_playMode.value) {
            PlayMode.SEQUENTIAL -> PlayMode.LOOP_ALL
            PlayMode.LOOP_ALL -> PlayMode.LOOP_ONE
            PlayMode.LOOP_ONE -> PlayMode.SEQUENTIAL
        }
    }

    // 切换循环模式
    fun toggleLoopMode() {
        _playMode.value = if (_playMode.value == PlayMode.SEQUENTIAL) {
            PlayMode.LOOP_ALL
        } else {
            PlayMode.SEQUENTIAL
        }
    }
}

// ViewModel状态类
data class PlaylistDetailUiState(
    val isLoading: Boolean = false
)