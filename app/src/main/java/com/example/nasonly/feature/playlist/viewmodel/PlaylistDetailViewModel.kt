package com.example.nasonly.feature.playlist.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nasonly.feature.playlist.domain.PlayMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// 视频数据类（与 UI 层一致）
data class VideoEntity(
    val id: String,
    val name: String,
    val url: String,
    val size: Long
)

data class PlaylistDetailUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class PlaylistDetailViewModel : ViewModel() {
    // UI 状态
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

    // 模拟加载视频
    fun loadVideos() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                // TODO: 实际应从数据库或 Repository 加载视频
                val dummyVideos = listOf(
                    VideoEntity("1", "示例视频1", "smb://192.168.1.1/video1.mp4", 1024 * 1024 * 200),
                    VideoEntity("2", "示例视频2", "smb://192.168.1.1/video2.mkv", 1024 * 1024 * 500)
                )
                _videos.update { dummyVideos }
                _uiState.update { it.copy(isLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message) }
            }
        }
    }

    fun deleteVideo(videoId: String) {
        _videos.update { it.filterNot { video -> video.id == videoId } }
    }

    fun setPlayMode(mode: PlayMode) {
        _playMode.value = mode
    }
}
