package com.example.nasonly.di

import com.example.nasonly.data.smb.SmbConnectionManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SmbModule {

    @Provides
    @Singleton
    fun provideSmbConnectionManager(): SmbConnectionManager = SmbConnectionManager()
}
