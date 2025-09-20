package com.example.nasonly.di

import android.content.Context
import com.example.nasonly.data.db.PlaylistDao
import com.example.nasonly.data.db.ScanProgressDao
import com.example.nasonly.data.db.VideoDao
import com.example.nasonly.data.repository.NasRepository
import com.example.nasonly.data.smb.SmbConnectionManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideNasRepository(
        @ApplicationContext context: Context,
        playlistDao: PlaylistDao,
        videoDao: VideoDao,
        scanProgressDao: ScanProgressDao,
        smbConnectionManager: SmbConnectionManager
    ): NasRepository = NasRepository(
        context = context,
        playlistDao = playlistDao,
        videoDao = videoDao,
        scanProgressDao = scanProgressDao,
        smbManager = smbConnectionManager
    )
}
