package com.example.nasonly.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface VideoDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(videos: List<VideoEntity>)

    @Query("SELECT * FROM videos ORDER BY name ASC")
    fun getAllVideosSortedByName(): Flow<List<VideoEntity>>

    @Query("SELECT * FROM videos WHERE id = :videoId LIMIT 1")
    suspend fun getVideoById(videoId: String): VideoEntity?

    @Query("DELETE FROM videos WHERE id NOT IN (:keepIds)")
    suspend fun deleteVideosNotIn(keepIds: List<String>)

    @Query("DELETE FROM videos")
    suspend fun clearAll()
}
