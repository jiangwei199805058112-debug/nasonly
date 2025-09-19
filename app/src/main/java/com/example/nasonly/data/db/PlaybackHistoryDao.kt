package nasonly.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaybackHistoryDao {
    // 添加或更新播放记录
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertHistory(history: PlaybackHistory)

    // 获取所有播放记录（按最后播放时间倒序）
    @Query("SELECT * FROM playback_history ORDER BY lastPlayedTime DESC")
    fun getAllHistory(): Flow<List<PlaybackHistory>>

    // 获取最近的N条播放记录
    @Query("SELECT * FROM playback_history ORDER BY lastPlayedTime DESC LIMIT :limit")
    fun getRecentHistory(limit: Int): Flow<List<PlaybackHistory>>

    // 删除单条播放记录
    @Query("DELETE FROM playback_history WHERE videoId = :videoId")
    suspend fun deleteHistory(videoId: String)

    // 清空所有播放记录
    @Query("DELETE FROM playback_history")
    suspend fun clearAllHistory()

    // 获取视频的播放记录
    @Query("SELECT * FROM playback_history WHERE videoId = :videoId LIMIT 1")
    suspend fun getHistoryForVideo(videoId: String): PlaybackHistory?
}