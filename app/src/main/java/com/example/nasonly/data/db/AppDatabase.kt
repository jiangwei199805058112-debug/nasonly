package com.example.nasonly.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        PlaylistEntity::class,
        VideoEntity::class,
        PlaybackHistory::class,
        ScanProgress::class
    ],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun playlistDao(): PlaylistDao

    abstract fun videoDao(): VideoDao

    abstract fun playbackHistoryDao(): PlaybackHistoryDao

    abstract fun scanProgressDao(): ScanProgressDao
}
