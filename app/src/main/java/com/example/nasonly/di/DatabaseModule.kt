package com.example.nasonly.di

import android.content.Context
import androidx.room.Room
import com.example.nasonly.data.db.AppDatabase
import com.example.nasonly.data.db.PlaybackHistoryDao
import com.example.nasonly.data.db.PlaylistDao
import com.example.nasonly.data.db.VideoDao
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
            // 可根据需要添加配置，例如：
            // .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun providePlaylistDao(db: AppDatabase): PlaylistDao = db.playlistDao()

    @Provides
    fun provideVideoDao(db: AppDatabase): VideoDao = db.videoDao()

    @Provides
    fun providePlaybackHistoryDao(db: AppDatabase): PlaybackHistoryDao = db.playbackHistoryDao()
}
