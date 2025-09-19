package nasonly.feature.playlist.viewmodel

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
    // 实际项目中应注入PlaylistRepository
) : ViewModel() {
    // UI状态
    private val _uiState = MutableStateFlow(CreatePlaylistUiState())
    val uiState: StateFlow<CreatePlaylistUiState> = _uiState.asStateFlow()

    // 创建播放列表
    fun createPlaylist(name: String, onSuccess: (String) -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = "") }
            try {
                // 生成唯一ID
                val playlistId = UUID.randomUUID().toString()

                // 实际项目中应调用仓库层保存到数据库
                // repository.createPlaylist(playlistId, name)

                // 模拟网络延迟
                kotlinx.coroutines.delay(500)

                _uiState.update { it.copy(isLoading = false) }
                onSuccess(playlistId)
            } catch (e: Exception) {
                _uiState.update { it.copy(
                    isLoading = false,
                    errorMessage = "创建失败: ${e.message}"
                ) }
            }
        }
    }
}

// UI状态类
data class CreatePlaylistUiState(
    val isLoading: Boolean = false,
    val errorMessage: String = ""
)