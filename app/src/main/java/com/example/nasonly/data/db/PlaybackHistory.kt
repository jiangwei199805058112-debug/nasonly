package nasonly.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "playback_history")
data class PlaybackHistory(
    @PrimaryKey val videoId: String,
    val videoName: String,
    val videoUrl: String,
    val thumbnailPath: String?,
    val lastPosition: Long,
    val duration: Long,
    val lastPlayedTime: Long,
    val playbackCount: Int = 1
)