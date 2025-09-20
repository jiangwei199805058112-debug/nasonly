package com.example.nasonly.navigation

import androidx.navigation.NamedNavArgument
import androidx.navigation.navArgument

sealed class Screen(
    val route: String,
    val arguments: List<NamedNavArgument> = emptyList()
) {
    object NasConfig : Screen("nas_config")

    object MediaLibrary : Screen(
        route = "media_library",
        arguments = listOf(
            navArgument("playlistId") { nullable = true }
        )
    ) {
        fun createRoute(playlistId: String? = null): String {
            return if (playlistId.isNullOrEmpty()) route
            else "$route?playlistId=$playlistId"
        }
    }

    object VideoPlayer : Screen(
        route = "video_player",
        arguments = listOf(
            navArgument("videoId") { nullable = false }
        )
    ) {
        fun createRoute(videoId: String): String = "$route/$videoId"
    }

    object PlaylistList : Screen("playlist_list")

    object CreatePlaylist : Screen("create_playlist")

    object PlaylistDetail : Screen(
        route = "playlist_detail",
        arguments = listOf(
            navArgument("playlistId") { nullable = false }
        )
    ) {
        fun createRoute(playlistId: String): String = "$route/$playlistId"
    }

    object PlaybackHistory : Screen("playback_history")
}
