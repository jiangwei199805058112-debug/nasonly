package com.example.nasonly.di

import android.content.Context
import androidx.room.Room
import nasonly.data.db.AppDatabase
import nasonly.data.db.PlaybackHistoryDao
import nasonly.data.db.PlaylistDao
import nasonly.data.db.VideoDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context
    ): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "nas_player_db"
        )
            // 保留你原来的可破坏式迁移或其它配置，请按需添加
            //.fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideVideoDao(db: AppDatabase): VideoDao = db.videoDao()

    @Provides
    fun providePlaybackHistoryDao(db: AppDatabase): PlaybackHistoryDao = db.playbackHistoryDao()

    @Provides
    fun providePlaylistDao(db: AppDatabase): PlaylistDao = db.playlistDao()
}
