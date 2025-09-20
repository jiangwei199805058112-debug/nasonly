package com.example.nasonly.ui.screens

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nasonly.data.db.PlaybackHistory
import com.example.nasonly.data.db.PlaybackHistoryDao
import com.example.nasonly.data.db.VideoDao
import com.example.nasonly.data.db.VideoEntity
import com.example.nasonly.data.repository.NasRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import jcifs.CIFSContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class VideoPlayerViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val repository: NasRepository,
    private val videoDao: VideoDao,
    private val historyDao: PlaybackHistoryDao
) : ViewModel() {

    private val _lastPlayedPosition = MutableStateFlow(0L)
    val lastPlayedPosition: StateFlow<Long> = _lastPlayedPosition.asStateFlow()

    private val _smbContext = MutableStateFlow<CIFSContext?>(null)
    val smbContext: StateFlow<CIFSContext?> = _smbContext.asStateFlow()

    private val _currentVideo = MutableStateFlow<VideoEntity?>(null)
    val currentVideo: StateFlow<VideoEntity?> = _currentVideo.asStateFlow()

    init {
        val videoId = savedStateHandle.get<String>("videoId") ?: ""
        val startPosition = savedStateHandle.get<Int>("position") ?: 0
        if (startPosition > 0) {
            _lastPlayedPosition.value = startPosition.toLong()
        }

        if (videoId.isNotEmpty()) {
            loadVideoInfo(videoId)
            loadPlaybackPosition(videoId)
        }

        viewModelScope.launch {
            _smbContext.value = repository.getSmbContext()
        }
    }

    private fun loadVideoInfo(videoId: String) {
        viewModelScope.launch {
            try {
                _currentVideo.value = videoDao.getVideoById(videoId)
            } catch (_: Exception) {
                // 忽略查询失败
            }
        }
    }

    private fun loadPlaybackPosition(videoId: String) {
        viewModelScope.launch {
            try {
                val history = historyDao.getHistoryForVideo(videoId)
                if (history != null && _lastPlayedPosition.value == 0L) {
                    _lastPlayedPosition.value = history.lastPosition
                }
            } catch (_: Exception) {
                // 忽略查询失败
            }
        }
    }

    fun updatePlaybackPosition(videoId: String, position: Long, duration: Long = 0) {
        viewModelScope.launch {
            delay(30_000)
            try {
                val video = _currentVideo.value ?: return@launch
                val existingHistory = historyDao.getHistoryForVideo(videoId)
                val history = PlaybackHistory(
                    videoId = videoId,
                    videoName = video.name,
                    videoUrl = video.url,
                    thumbnailPath = video.thumbnailPath,
                    lastPosition = position,
                    duration = if (duration > 0) duration else video.duration,
                    lastPlayedTime = System.currentTimeMillis(),
                    playbackCount = existingHistory?.playbackCount?.plus(1) ?: 1
                )
                historyDao.upsertHistory(history)
            } catch (_: Exception) {
                // 忽略写入失败
            }
        }
    }

    fun onPlaybackCompleted(videoId: String) {
        viewModelScope.launch {
            try {
                val video = _currentVideo.value ?: return@launch
                val existingHistory = historyDao.getHistoryForVideo(videoId)
                val history = PlaybackHistory(
                    videoId = videoId,
                    videoName = video.name,
                    videoUrl = video.url,
                    thumbnailPath = video.thumbnailPath,
                    lastPosition = 0,
                    duration = video.duration,
                    lastPlayedTime = System.currentTimeMillis(),
                    playbackCount = existingHistory?.playbackCount?.plus(1) ?: 1
                )
                historyDao.upsertHistory(history)
            } catch (_: Exception) {
                // 忽略写入失败
            }
        }
    }
}
