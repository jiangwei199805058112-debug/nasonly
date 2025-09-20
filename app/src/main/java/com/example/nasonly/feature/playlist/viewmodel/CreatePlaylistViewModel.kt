package com.example.nasonly.feature.playlist.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class CreatePlaylistViewModel @Inject constructor(
    // TODO: 实际项目中应注入 PlaylistRepository
) : ViewModel() {
    // UI 状态
    private val _uiState = MutableStateFlow(CreatePlaylistUiState())
    val uiState: StateFlow<CreatePlaylistUiState> = _uiState.asStateFlow()

    // 创建播放列表
    fun createPlaylist(name: String, onSuccess: (String) -> Unit = {}) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = "", success = false) }
            try {
                if (name.isBlank()) {
                    throw IllegalArgumentException("播放列表名称不能为空")
                }

                // 模拟生成一个播放列表 ID
                val playlistId = UUID.randomUUID().toString()

                // TODO: 这里调用 PlaylistRepository 插入数据库
                // repository.insertPlaylist(PlaylistEntity(...))

                _uiState.update { it.copy(isLoading = false, success = true) }
                onSuccess(playlistId)
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "未知错误",
                        success = false
                    )
                }
            }
        }
    }
}

data class CreatePlaylistUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val success: Boolean = false
)
