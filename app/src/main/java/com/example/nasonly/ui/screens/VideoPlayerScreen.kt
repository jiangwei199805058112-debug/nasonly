package nasonly.ui.screens

import android.content.pm.ActivityInfo
import android.view.View
import android.view.WindowManager
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.rememberAsyncImagePainter
import nasonly.core.ui.components.ErrorDialog
import nasonly.core.ui.components.SmbConnectionState
import nasonly.core.ui.components.SmbConnectionStatus
import nasonly.core.utils.formatTime
import nasonly.data.smb.SmbConnectionManager
import androidx.navigation.NavController
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@Composable
fun VideoPlayerScreen(
    videoId: String,
    videoUrl: String,
    position: Int = 0,
    navController: NavController,
    viewModel: VideoPlayerViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentVideo by viewModel.currentVideo.collectAsStateWithLifecycle()
    val lastPlayedPosition by viewModel.lastPlayedPosition.collectAsStateWithLifecycle()
    val smbContext by viewModel.smbContext.collectAsStateWithLifecycle()

    // 播放器状态管理
    var player by remember { mutableStateOf<ExoPlayer?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var currentPosition by remember { mutableStateOf(0L) }
    var videoDuration by remember { mutableStateOf(0L) }
    var showControls by remember { mutableStateOf(true) }
    var isFullScreen by remember { mutableStateOf(false) }
    var volume by remember { mutableFloatStateOf(1.0f) }
    var isMuted by remember { mutableStateOf(false) }
    var buffering by remember { mutableStateOf(false) }

    // SMB连接状态管理
    var connectionState by remember { mutableStateOf(SmbConnectionState.CONNECTED) }
    var showConnectionErrorDialog by remember { mutableStateOf(false) }
    var connectionErrorMessage by remember { mutableStateOf("") }

    // 初始化播放器
    LaunchedEffect(videoUrl, smbContext) {
        if (smbContext != null && player == null) {
            // 创建SMB数据源工厂
            val dataSourceFactory = viewModel.createSmbDataSourceFactory()

            if (dataSourceFactory != null) {
                // 初始化ExoPlayer
                player = ExoPlayer.Builder(context)
                    .setDataSourceFactory(dataSourceFactory)
                    .build()
                    .apply {
                        val mediaItem = MediaItem.fromUri(videoUrl)
                        setMediaItem(mediaItem)
                        prepare()

                        // 如果有指定位置或历史位置，跳转到相应位置
                        val startPosition = if (position > 0) position.toLong()
                        else lastPlayedPosition
                        if (startPosition > 0) {
                            seekTo(startPosition)
                        }

                        // 开始播放
                        playWhenReady = true
                        isPlaying = true

                        // 监听播放器状态
                        addListener(object : Player.Listener {
                            override fun onPlaybackStateChanged(state: Int) {
                                when (state) {
                                    Player.STATE_BUFFERING -> buffering = true
                                    Player.STATE_READY -> {
                                        buffering = false
                                        videoDuration = duration
                                    }
                                    Player.STATE_ENDED -> {
                                        isPlaying = false
                                        viewModel.onPlaybackCompleted(videoId)
                                    }
                                }
                            }

                            override fun onIsPlayingChanged(playing: Boolean) {
                                isPlaying = playing
                                if (!playing) {
                                    // 暂停时保存播放位置
                                    currentPosition?.let {
                                        viewModel.updatePlaybackPosition(videoId, it, videoDuration)
                                    }
                                }
                            }
                        })
                    }
            } else {
                showConnectionErrorDialog = true
                connectionErrorMessage = "无法创建SMB数据源，请检查NAS连接"
            }
        }
    }

    // 监听SMB连接状态
    LaunchedEffect(smbContext) {
        smbContext?.let {
            viewModel.registerConnectionListener(object : SmbConnectionManager.SmbConnectionListener {
                override fun onConnected(ip: String, port: Int) {
                    connectionState = SmbConnectionState.CONNECTED
                }

                override fun onConnectionFailed(ip: String, port: Int, error: String) {
                    connectionState = SmbConnectionState.ERROR
                    connectionErrorMessage = error
                    showConnectionErrorDialog = true
                    player?.pause()
                }

                override fun onOperationFailed(error: String, exception: Exception?) {
                    connectionState = SmbConnectionState.DISCONNECTED
                    connectionErrorMessage = error
                    showConnectionErrorDialog = true
                    player?.pause()
                }

                override fun onReconnected(ip: String, port: Int) {
                    connectionState = SmbConnectionState.RECONNECTED
                    // 尝试恢复播放
                    player?.play()

                    // 3秒后恢复为普通连接状态
                    launch {
                        delay(3000)
                        if (isActive) {
                            connectionState = SmbConnectionState.CONNECTED
                        }
                    }
                }
            })
        }
    }

    // 控制控件自动隐藏
    LaunchedEffect(isPlaying) {
        while (isActive) {
            if (isPlaying && showControls) {
                delay(3000)
                showControls = false
            }
            delay(500)
        }
    }

    // 定期更新播放位置
    LaunchedEffect(player) {
        while (isActive) {
            player?.currentPosition?.let { pos ->
                currentPosition = pos
                // 每30秒更新一次播放记录
                if (pos % 30000 < 500) {
                    viewModel.updatePlaybackPosition(videoId, pos, videoDuration)
                }
            }
            delay(500)
        }
    }

    // 生命周期管理
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    player?.pause()
                    // 保存当前播放位置
                    player?.currentPosition?.let {
                        viewModel.updatePlaybackPosition(videoId, it, videoDuration)
                    }
                }
                Lifecycle.Event.ON_RESUME -> {
                    if (isPlaying) player?.play()
                }
                Lifecycle.Event.ON_DESTROY -> {
                    // 释放播放器资源
                    player?.currentPosition?.let {
                        viewModel.updatePlaybackPosition(videoId, it, videoDuration)
                    }
                    player?.release()
                    player = null
                }
                else -> {}
            }
        }

        val lifecycle = lifecycleOwner.lifecycle
        lifecycle.addObserver(observer)

        onDispose {
            lifecycle.removeObserver(observer)
        }
    }

    // 全屏切换处理
    DisposableEffect(isFullScreen) {
        val activity = context.findActivity()
        if (activity != null) {
            val originalOrientation = activity.requestedOrientation
            val originalFlags = activity.window.attributes.flags

            if (isFullScreen) {
                // 进入全屏
                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                activity.window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
                activity.window.decorView.systemUiVisibility = (
                        View.SYSTEM_UI_FLAG_IMMERSIVE
                                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                                or View.SYSTEM_UI_FLAG_FULLSCREEN
                                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        )
            } else {
                // 退出全屏
                activity.requestedOrientation = originalOrientation
                activity.window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
                activity.window.decorView.systemUiVisibility = originalFlags
            }

            onDispose {
                // 恢复原始状态
                activity.requestedOrientation = originalOrientation
                activity.window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
                activity.window.decorView.systemUiVisibility = originalFlags
            }
        } else {
            onDispose { }
        }
    }

    // 构建UI
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // 视频播放器
        AndroidView(
            factory = { context ->
                PlayerView(context).apply {
                    this.player = player
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                    setShowBuffering(PlayerView.SHOW_BUFFERING_ALWAYS)
                    setControllerVisibilityListener { visibility ->
                        // 控制自定义控件显示
                        showControls = visibility == View.VISIBLE
                    }
                    // 禁用默认控制器
                    useController = false
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // 视频封面（加载前显示）
        if (player == null || (player?.playbackState == Player.STATE_IDLE && currentPosition == 0L)) {
            currentVideo?.thumbnailPath?.let { thumbnailPath ->
                Image(
                    painter = rememberAsyncImagePainter(
                        model = if (thumbnailPath.isEmpty()) {
                            "https://picsum.photos/seed/$videoId/800/450"
                        } else thumbnailPath
                    ),
                    contentDescription = currentVideo?.name ?: "视频封面",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        }

        // 加载指示器
        if (buffering) {
            CircularProgressIndicator(
                modifier = Modifier
                    .size(48.dp)
                    .align(Alignment.Center),
                color = MaterialTheme.colorScheme.primary
            )
        }

        // 播放/暂停大按钮（视频中央）
        if (!isPlaying && !buffering) {
            IconButton(
                onClick = { player?.play() },
                modifier = Modifier
                    .size(96.dp)
                    .align(Alignment.Center)
                    .background(Color.Black.copy(alpha = 0.5f), shape = RectangleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "播放",
                    modifier = Modifier.size(64.dp),
                    tint = Color.White
                )
            }
        }

        // 连接状态提示
        SmbConnectionStatus(
            status = connectionState,
            modifier = Modifier.align(Alignment.TopCenter)
        )

        // 自定义控制器
        Column(
            modifier = Modifier
                .fillMaxSize()
                .alpha(if (showControls) 1f else 0f)
                .background(
                    brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.7f),
                            Color.Black.copy(alpha = 0f),
                            Color.Black.copy(alpha = 0.7f)
                        )
                    )
                )
                .padding(16.dp)
                .clickable { showControls = !showControls },
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // 顶部控制栏
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 返回按钮
                IconButton(
                    onClick = {
                        // 返回前保存播放位置
                        player?.currentPosition?.let {
                            viewModel.updatePlaybackPosition(videoId, it, videoDuration)
                        }
                        navController.popBackStack()
                    },
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color.Black.copy(alpha = 0.5f), shape = RectangleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "返回",
                        tint = Color.White
                    )
                }

                // 视频标题
                Text(
                    text = currentVideo?.name ?: "正在播放",
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).padding(horizontal = 16.dp)
                )
            }

            // 底部控制栏
            Column {
                // 进度条
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formatTime(currentPosition),
                        color = Color.White,
                        modifier = Modifier.width(48.dp),
                        textAlign = TextAlign.Center
                    )

                    Slider(
                        value = currentPosition.toFloat(),
                        onValueChange = { position ->
                            player?.seekTo(position.toLong())
                        },
                        valueRange = 0f..videoDuration.toFloat(),
                        modifier = Modifier.weight(1f),
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary
                    )

                    Text(
                        text = formatTime(videoDuration),
                        color = Color.White,
                        modifier = Modifier.width(48.dp),
                        textAlign = TextAlign.Center
                    )
                }

                // 控制按钮
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 音量控制
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                isMuted = !isMuted
                                player?.volume = if (isMuted) 0f else volume
                            }
                        ) {
                            Icon(
                                imageVector = if (isMuted || volume == 0f) {
                                    Icons.Default.VolumeOff
                                } else {
                                    Icons.Default.VolumeUp
                                },
                                contentDescription = if (isMuted) "取消静音" else "静音",
                                tint = Color.White
                            )
                        }

                        Slider(
                            value = if (isMuted) 0f else volume,
                            onValueChange = { newVolume ->
                                volume = newVolume
                                player?.volume = newVolume
                                isMuted = newVolume == 0f
                            },
                            valueRange = 0f..1f,
                            modifier = Modifier.width(100.dp),
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary
                        )
                    }

                    // 播放/暂停按钮
                    IconButton(
                        onClick = {
                            if (isPlaying) {
                                player?.pause()
                            } else {
                                player?.play()
                            }
                        },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "暂停" else "播放",
                            modifier = Modifier.size(32.dp),
                            tint = Color.White
                        )
                    }

                    // 全屏切换按钮
                    IconButton(
                        onClick = { isFullScreen = !isFullScreen }
                    ) {
                        Icon(
                            imageVector = if (isFullScreen) {
                                Icons.Default.FullscreenExit
                            } else {
                                Icons.Default.Fullscreen
                            },
                            contentDescription = if (isFullScreen) "退出全屏" else "进入全屏",
                            tint = Color.White
                        )
                    }
                }
            }
        }
    }

    // 连接错误对话框
    if (showConnectionErrorDialog) {
        ErrorDialog(
            title = "NAS连接错误",
            message = "无法连接到NAS服务器: $connectionErrorMessage\n请检查网络连接和NAS配置。",
            onDismiss = { showConnectionErrorDialog = false },
            onRetry = {
                showConnectionErrorDialog = false
                // 重新初始化播放器
                player?.release()
                player = null
                // 重新加载视频
                LaunchedEffect(Unit) {
                    viewModel.refreshSmbContext()
                }
            }
        )
    }

    // 通用错误对话框
    if (uiState.errorMessage.isNotEmpty()) {
        ErrorDialog(
            title = "播放错误",
            message = uiState.errorMessage,
            onDismiss = { viewModel.clearError() },
            onRetry = {
                viewModel.clearError()
                player?.release()
                player = null
                LaunchedEffect(Unit) {
                    viewModel.refreshSmbContext()
                }
            }
        )
    }
}

// 辅助函数：从上下文获取Activity
fun android.content.Context.findActivity(): android.app.Activity? {
    var context = this
    while (context is android.content.ContextWrapper) {
        if (context is android.app.Activity) {
            return context
        }
        context = context.baseContext
    }
    return null
}

// 视频播放器ViewModel接口定义
abstract class VideoPlayerViewModel {
    abstract val uiState: androidx.lifecycle.StateFlow<VideoPlayerUiState>
    abstract val currentVideo: androidx.lifecycle.StateFlow<VideoEntity?>
    abstract val lastPlayedPosition: androidx.lifecycle.StateFlow<Long>
    abstract val smbContext: androidx.lifecycle.StateFlow<jcifs.CIFSContext?>

    abstract fun updatePlaybackPosition(videoId: String, position: Long, duration: Long)
    abstract fun onPlaybackCompleted(videoId: String)
    abstract fun registerConnectionListener(listener: SmbConnectionManager.SmbConnectionListener)
    abstract fun createSmbDataSourceFactory(): nasonly.data.smb.SmbDataSource.Factory?
    abstract fun refreshSmbContext()
    abstract fun clearError()
    abstract val viewModelScope: kotlinx.coroutines.CoroutineScope
}

// 视频播放器UI状态类
data class VideoPlayerUiState(
    val isLoading: Boolean = false,
    val errorMessage: String = ""
)
