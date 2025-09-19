package nasonly.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import nasonly.data.db.VideoEntity
import nasonly.data.repository.NasRepository
import javax.inject.Inject

@HiltViewModel
class MediaLibraryViewModel @Inject constructor(
    private val repository: NasRepository
) : ViewModel() {
    // UI状态
    private val _uiState = MutableStateFlow(MediaLibraryUiState())
    val uiState: StateFlow<MediaLibraryUiState> = _uiState.asStateFlow()

    // 视频列表
    private val _videos = MutableStateFlow<List<VideoEntity>>(emptyList())
    val videos = _videos.asStateFlow()

    // 加载视频
    fun loadVideos() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = "") }
            try {
                // 检查是否需要重新扫描
                val needScan = repository.shouldScanAgain()
                if (needScan) {
                    repository.scanAndUpdateMediaLibrary()
                }

                // 从数据库加载视频
                repository.getVideos().collect { videos ->
                    _videos.value = videos
                    _uiState.update { it.copy(isLoading = false) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(
                    isLoading = false,
                    errorMessage = "加载失败: ${e.message}"
                ) }
            }
        }
    }

    // 刷新视频列表（强制重新扫描）
    fun refreshVideos() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = "") }
            try {
                repository.scanAndUpdateMediaLibrary()
                // 重新加载视频
                repository.getVideos().collect { videos ->
                    _videos.value = videos
                    _uiState.update { it.copy(isLoading = false) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(
                    isLoading = false,
                    errorMessage = "刷新失败: ${e.message}"
                ) }
            }
        }
    }

    // 切换视频选中状态
    fun toggleVideoSelection(videoId: String) {
        val currentSelected = _uiState.value.selectedVideoIds.toMutableSet()
        if (currentSelected.contains(videoId)) {
            currentSelected.remove(videoId)
        } else {
            currentSelected.add(videoId)
        }
        _uiState.update { it.copy(selectedVideoIds = currentSelected) }
    }

    // 添加选中视频到播放列表
    fun addSelectedToPlaylist(playlistId: String, onComplete: () -> Unit) {
        val selectedIds = _uiState.value.selectedVideoIds
        if (selectedIds.isEmpty()) return

        viewModelScope.launch {
            try {
                // 获取选中的视频
                val selectedVideos = _videos.value.filter { it.id in selectedIds }
                // 添加到播放列表（实际实现需调用PlaylistRepository）
                // repository.addVideosToPlaylist(playlistId, selectedVideos)
                _uiState.update { it.copy(selectedVideoIds = emptySet()) }
                onComplete()
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "添加失败: ${e.message}") }
            }
        }
    }
}

// UI状态类
data class MediaLibraryUiState(
    val isLoading: Boolean = false,
    val errorMessage: String = "",
    val selectedVideoIds: Set<String> = emptySet()
)