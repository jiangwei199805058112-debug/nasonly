package nasonly.navigation

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
            navArgument("videoId") { nullable = false },
            navArgument("videoUrl") { nullable = false },
            navArgument("playlistId") { nullable = true },
            navArgument("position") { defaultValue = 0 }
        )
    ) {
        fun createRoute(
            videoId: String,
            videoUrl: String,
            playlistId: String? = null,
            position: Int = 0
        ): String {
            var route = "video_player?videoId=$videoId&videoUrl=$videoUrl&position=$position"
            if (!playlistId.isNullOrEmpty()) {
                route += "&playlistId=$playlistId"
            }
            return route
        }
    }

    object PlaylistDetail : Screen(
        route = "playlist_detail",
        arguments = listOf(
            navArgument("playlistId") { nullable = false }
        )
    ) {
        fun createRoute(playlistId: String): String {
            return "playlist_detail?playlistId=$playlistId"
        }
    }
}