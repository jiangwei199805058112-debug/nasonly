package com.example.nasonly.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.example.nasonly.data.db.PlaybackHistory
import com.example.nasonly.data.db.PlaybackHistoryDao
import javax.inject.Inject

@HiltViewModel
class PlaybackHistoryViewModel @Inject constructor(
    private val historyDao: PlaybackHistoryDao
) : ViewModel() {
    // UI状态
    private val _uiState = MutableStateFlow(PlaybackHistoryUiState())
    val uiState: StateFlow<PlaybackHistoryUiState> = _uiState.asStateFlow()

    // 历史记录列表
    private val _historyList = MutableStateFlow<List<PlaybackHistory>>(emptyList())
    val historyList = _historyList.asStateFlow()

    // 加载播放历史
    fun loadPlaybackHistory() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = "") }
            try {
                historyDao.getAllHistory().collect { history ->
                    _historyList.value = history
                    _uiState.update { it.copy(isLoading = false) }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = e.message ?: "加载失败")
                }
            }
        }
    }

    // 删除单条历史记录
    fun deleteHistory(videoId: String) {
        viewModelScope.launch {
            try {
                historyDao.deleteHistory(videoId)
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(errorMessage = e.message ?: "删除失败")
                }
            }
        }
    }

    // 清空所有历史记录
    fun clearAllHistory() {
        viewModelScope.launch {
            try {
                historyDao.clearAll()
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(errorMessage = e.message ?: "清空失败")
                }
            }
        }
    }
}

// UI 状态数据类
data class PlaybackHistoryUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)
