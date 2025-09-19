package com.example.nasonly.di

import android.content.Context
import com.example.nasonly.data.db.PlaylistDao
import com.example.nasonly.data.repository.NasRepository
import com.example.nasonly.data.smb.SmbConnectionManager
import com.example.nasonly.data.smb.SmbDataSource
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
        smbDataSource: SmbDataSource,
        smbConnectionManager: SmbConnectionManager
    ): NasRepository {
        return NasRepository(
            context = context,
            playlistDao = playlistDao,
            smbDataSource = smbDataSource,
            smbManager = smbConnectionManager
        )
    }
}
