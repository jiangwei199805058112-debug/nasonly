package com.example.nasonly.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.example.nasonly.data.db.VideoEntity
import com.example.nasonly.data.repository.NasRepository
import javax.inject.Inject

@HiltViewModel
class MediaLibraryViewModel @Inject constructor(
    private val repository: NasRepository
) : ViewModel() {
    // UI 状态
    private val _uiState = MutableStateFlow(MediaLibraryUiState())
    val uiState: StateFlow<MediaLibraryUiState> = _uiState.asStateFlow()

    // 视频列表
    private val _videos = MutableStateFlow<List<VideoEntity>>(emptyList())
    val videos: StateFlow<List<VideoEntity>> = _videos.asStateFlow()

    // 加载视频
    fun loadVideos() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val needScan = repository.shouldScanAgain()
                if (needScan) {
                    repository.scanAndUpdateMediaLibrary()
                }
                val allVideos = repository.getAllVideos()
                _videos.value = allVideos
                _uiState.update { it.copy(isLoading = false) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = e.message ?: "加载失败")
                }
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            try {
                repository.scanAndUpdateMediaLibrary()
                val allVideos = repository.getAllVideos()
                _videos.value = allVideos
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = e.message ?: "刷新失败")
                }
            }
        }
    }
}

data class MediaLibraryUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)
