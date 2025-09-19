package nasonly.feature.playlist.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import nasonly.feature.playlist.ui.Playlist
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class PlaylistListViewModel @Inject constructor(
    // 实际项目中应注入PlaylistRepository
) : ViewModel() {
    // UI状态
    private val _uiState = MutableStateFlow(PlaylistListUiState())
    val uiState: StateFlow<PlaylistListUiState> = _uiState.asStateFlow()

    // 播放列表
    private val _playlists = MutableStateFlow<List<Playlist>>(emptyList())
    val playlists = _playlists.asStateFlow()

    // 加载播放列表
    fun loadPlaylists() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = "") }
            try {
                // 实际项目中应从仓库层获取
                // 这里使用模拟数据
                val mockPlaylists = listOf(
                    Playlist(
                        id = UUID.randomUUID().toString(),
                        name = "动作电影",
                        videoCount = 5,
                        createdAt = System.currentTimeMillis() - 86400000 * 2
                    ),
                    Playlist(
                        id = UUID.randomUUID().toString(),
                        name = "纪录片",
                        videoCount = 3,
                        createdAt = System.currentTimeMillis() - 86400000
                    )
                )

                _playlists.value = mockPlaylists
                _uiState.update { it.copy(isLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(
                    isLoading = false,
                    errorMessage = "加载失败: ${e.message}"
                ) }
            }
        }
    }

    // 刷新播放列表
    fun refreshPlaylists() {
        loadPlaylists()
    }
}

// UI状态类
data class PlaylistListUiState(
    val isLoading: Boolean = false,
    val errorMessage: String = ""
)