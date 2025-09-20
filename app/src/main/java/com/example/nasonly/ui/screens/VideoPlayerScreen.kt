package com.example.nasonly.ui.screens

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import jcifs.CIFSContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.example.nasonly.data.db.PlaybackHistory
import com.example.nasonly.data.db.PlaybackHistoryDao
import com.example.nasonly.data.db.VideoDao
import com.example.nasonly.data.db.VideoEntity
import com.example.nasonly.data.repository.NasRepository
import javax.inject.Inject

@HiltViewModel
class VideoPlayerViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val repository: NasRepository,
    private val videoDao: VideoDao,
    private val historyDao: PlaybackHistoryDao
) : ViewModel() {

    // 播放位置
    private val _lastPlayedPosition = MutableStateFlow(0L)
    val lastPlayedPosition: StateFlow<Long> = _lastPlayedPosition.asStateFlow()

    // SMB 上下文
    private val _smbContext = MutableStateFlow<CIFSContext?>(null)
    val smbContext: StateFlow<CIFSContext?> = _smbContext.asStateFlow()

    // 当前视频信息
    private val _currentVideo = MutableStateFlow<VideoEntity?>(null)
    val currentVideo = _currentVideo.asStateFlow()

    init {
        // 从 SavedStateHandle 恢复参数
        val videoId = savedStateHandle.get<String>("videoId") ?: ""
        val startPosition = savedStateHandle.get<Int>("position") ?: 0

        viewModelScope.launch {
            // 从数据库加载视频信息
            _currentVideo.value = videoDao.getVideoById(videoId)

            // 恢复播放位置
            _lastPlayedPosition.value = startPosition.toLong()

            // 模拟加载 SMB 会话（你可以在这里接 repository 初始化）
            delay(500)
            _smbContext.value = repository.getSmbContext()
        }
    }

    fun updatePlaybackPosition(videoId: String, position: Long, duration: Long) {
        viewModelScope.launch {
            _lastPlayedPosition.value = position
            historyDao.upsertHistory(
                PlaybackHistory(
                    videoId = videoId,
                    videoName = _currentVideo.value?.name ?: "未知视频",
                    videoUrl = _currentVideo.value?.url ?: "",
                    thumbnailPath = _currentVideo.value?.thumbnailPath,
                    lastPosition = position,
                    duration = duration,
                    lastPlayedTime = System.currentTimeMillis()
                )
            )
        }
    }

    fun onPlaybackCompleted(videoId: String) {
        viewModelScope.launch {
            historyDao.deleteHistory(videoId)
        }
    }
}
