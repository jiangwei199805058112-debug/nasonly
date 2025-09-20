package com.example.nasonly.feature.playlist.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.example.nasonly.feature.playlist.ui.Playlist
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class PlaylistListViewModel @Inject constructor(
    // TODO: 实际项目中应注入 PlaylistRepository
) : ViewModel() {
    // UI 状态
    private val _uiState = MutableStateFlow(PlaylistListUiState())
    val uiState: StateFlow<PlaylistListUiState> = _uiState.asStateFlow()

    // 播放列表
    private val _playlists = MutableStateFlow<List<Playlist>>(emptyList())
    val playlists: StateFlow<List<Playlist>> = _playlists.asStateFlow()

    // 加载播放列表
    fun loadPlaylists() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                // 模拟数据，实际应从仓库层获取
                val mockPlaylists = listOf(
                    Playlist(
                        id = UUID.randomUUID().toString(),
                        name = "示例播放列表 1"
                    ),
                    Playlist(
                        id = UUID.randomUUID().toString(),
                        name = "示例播放列表 2"
                    )
                )
                _playlists.update { mockPlaylists }
                _uiState.update { it.copy(isLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message) }
            }
        }
    }
}

data class PlaylistListUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)
