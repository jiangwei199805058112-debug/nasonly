package nasonly.ui.screens

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
import nasonly.data.db.PlaybackHistory
import nasonly.data.db.PlaybackHistoryDao
import nasonly.data.db.VideoDao
import nasonly.data.repository.NasRepository
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

    // SMB上下文
    private val _smbContext = MutableStateFlow<CIFSContext?>(null)
    val smbContext: StateFlow<CIFSContext?> = _smbContext.asStateFlow()

    // 当前视频信息
    private val _currentVideo = MutableStateFlow<VideoEntity?>(null)
    val currentVideo = _currentVideo.asStateFlow()

    init {
        // 加载视频ID并获取播放进度
        val videoId = savedStateHandle.get<String>("videoId") ?: ""
        val startPosition = savedStateHandle.get<Int>("position") ?: 0
        if (startPosition > 0) {
            _lastPlayedPosition.value = startPosition.toLong()
        }

        if (videoId.isNotEmpty()) {
            loadVideoInfo(videoId)
            loadPlaybackPosition(videoId)
        }

        // 获取SMB上下文
        viewModelScope.launch {
            _smbContext.value = repository.getSmbContext()
        }
    }

    // 加载视频信息
    private fun loadVideoInfo(videoId: String) {
        viewModelScope.launch {
            try {
                val video = videoDao.getVideoById(videoId)
                _currentVideo.value = video
            } catch (e: Exception) {
                // 视频信息加载失败，不影响播放
            }
        }
    }

    // 加载播放进度
    private fun loadPlaybackPosition(videoId: String) {
        viewModelScope.launch {
            try {
                val history = historyDao.getHistoryForVideo(videoId)
                if (history != null && _lastPlayedPosition.value == 0L) {
                    _lastPlayedPosition.value = history.lastPosition
                }
            } catch (e: Exception) {
                // 播放进度加载失败，不影响播放
            }
        }
    }

    // 更新播放进度（定期保存，避免频繁IO）
    fun updatePlaybackPosition(videoId: String, position: Long, duration: Long = 0) {
        viewModelScope.launch {
            // 每30秒保存一次，避免频繁写入数据库
            delay(30000)

            try {
                val video = _currentVideo.value
                if (video != null) {
                    // 查询现有记录
                    val existingHistory = historyDao.getHistoryForVideo(videoId)

                    // 创建或更新历史记录
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
                }
            } catch (e: Exception) {
                // 记录播放历史失败，不影响播放
            }
        }
    }

    // 视频播放结束时调用
    fun onPlaybackCompleted(videoId: String) {
        viewModelScope.launch {
            try {
                val video = _currentVideo.value
                if (video != null) {
                    val existingHistory = historyDao.getHistoryForVideo(videoId)

                    // 播放完成时重置位置为0
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
                }
            } catch (e: Exception) {
                // 记录播放历史失败，不影响播放
            }
        }
    }
}