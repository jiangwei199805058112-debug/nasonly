package com.example.nasonly.di

import android.content.Context
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
object SmbModule {

    @Provides
    @Singleton
    fun provideSmbConnectionManager(): SmbConnectionManager {
        // 与你原来的单例实现保持一致
        return SmbConnectionManager.getInstance()
    }

    /**
     * 关于 SmbDataSource:
     * - 如果 SmbDataSource 带 @Inject 构造函数，下面这个方法可以删除，不需要手写 Provider。
     * - 如果没有 @Inject 构造函数，并且构造签名是 (context, SmbConnectionManager)，
     *   这个 Provider 就能直接工作；若你的实际签名不同，请把参数顺序/类型按你的类声明改一下。
     */
    @Provides
    @Singleton
    fun provideSmbDataSource(
        @ApplicationContext context: Context,
        smbConnectionManager: SmbConnectionManager
    ): SmbDataSource {
        return SmbDataSource(context, smbConnectionManager)
    }
}
