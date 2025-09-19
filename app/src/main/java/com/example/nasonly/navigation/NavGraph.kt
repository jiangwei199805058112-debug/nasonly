package nasonly.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import nasonly.feature.playlist.ui.CreatePlaylistScreen
import nasonly.feature.playlist.ui.PlaylistDetailScreen
import nasonly.feature.playlist.ui.PlaylistListScreen
import nasonly.ui.screens.MediaLibraryScreen
import nasonly.ui.screens.NasConfigScreen
import nasonly.ui.screens.PlaybackHistoryScreen
import nasonly.ui.screens.VideoPlayerScreen

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

        // 播放历史
        composable("playback_history") {
            PlaybackHistoryScreen(navController = navController)
        }

        // NAS配置界面
        composable(Screen.NasConfig.route) {
            NasConfigScreen(navController = navController)
        }

        // 媒体库界面
        composable(Screen.MediaLibrary.route) { backStackEntry ->
            val playlistId = backStackEntry.arguments?.getString("playlistId")
            MediaLibraryScreen(
                navController = navController,
                playlistId = playlistId
            )
        }

        // 视频播放器界面
        composable(
            route = Screen.VideoPlayer.route,
            arguments = Screen.VideoPlayer.arguments
        ) { backStackEntry ->
            val videoId = backStackEntry.arguments?.getString("videoId") ?: ""
            val videoUrl = backStackEntry.arguments?.getString("videoUrl") ?: ""
            val playlistId = backStackEntry.arguments?.getString("playlistId")
            val position = backStackEntry.arguments?.getInt("position") ?: 0

            VideoPlayerScreen(
                videoId = videoId,
                videoUrl = videoUrl
            )
        }

        // 播放列表详情界面
        composable(
            route = Screen.PlaylistDetail.route,
            arguments = Screen.PlaylistDetail.arguments
        ) { backStackEntry ->
            val playlistId = backStackEntry.arguments?.getString("playlistId") ?: ""
            PlaylistDetailScreen(
                navController = navController,
                playlistId = playlistId
            )
        }
    }
}