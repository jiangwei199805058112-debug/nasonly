package com.example.nasonly.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.nasonly.feature.playlist.ui.CreatePlaylistScreen
import com.example.nasonly.feature.playlist.ui.PlaylistDetailScreen
import com.example.nasonly.feature.playlist.ui.PlaylistListScreen
import com.example.nasonly.ui.screens.MediaLibraryScreen
import com.example.nasonly.ui.screens.NasConfigScreen
import com.example.nasonly.ui.screens.PlaybackHistoryScreen
import com.example.nasonly.ui.screens.VideoPlayerScreen

@Composable
fun NavGraph(navController: NavController) {
    NavHost(
        navController = navController,
        startDestination = "main_navigation" // 主导航入口
    ) {
        // 主导航入口
        composable("main_navigation") {
            // 实际项目中应检查是否有保存的配置
            navController.navigate("playlist_list") {
                popUpTo("main_navigation") { inclusive = true }
            }
        }

        // 播放列表列表
        composable("playlist_list") {
            PlaylistListScreen(navController = navController)
        }

        // 创建播放列表
        composable("create_playlist") {
            CreatePlaylistScreen(navController = navController)
        }

        // 播放列表详情
        composable("playlist_detail/{playlistId}") { backStackEntry ->
            val playlistId = backStackEntry.arguments?.getString("playlistId") ?: ""
            PlaylistDetailScreen(navController = navController, playlistId = playlistId)
        }

        // 媒体库
        composable("media_library") {
            MediaLibraryScreen(navController = navController)
        }

        // NAS 配置
        composable("nas_config") {
            NasConfigScreen(navController = navController)
        }

        // 播放历史
        composable("playback_history") {
            PlaybackHistoryScreen(navController = navController)
        }

        // 视频播放
        composable("video_player/{videoId}") { backStackEntry ->
            val videoId = backStackEntry.arguments?.getString("videoId") ?: ""
            VideoPlayerScreen(navController = navController, videoId = videoId)
        }
    }
}
