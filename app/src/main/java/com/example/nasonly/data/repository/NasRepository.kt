package com.example.nasonly.data.repository

import android.content.Context
import com.example.nasonly.data.db.PlaylistDao
import com.example.nasonly.data.smb.SmbConnectionManager
import com.example.nasonly.data.smb.SmbDataSource
import dagger.hilt.android.qualifiers.ApplicationContext
import jcifs.CIFSContext
import jcifs.smb.SmbFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 统一的数据仓库：
 * - 本地数据库（Room）
 * - SMB 网络访问（委托给你现有的实现）
 *
 * 注意：为保持向后兼容，此类不“精简”功能。SMB 相关逻辑全部透传到
 * 你现在的 SmbDataSource / SmbConnectionManager（原包 nasonly.data.smb）。
 */
@Singleton
class NasRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val playlistDao: PlaylistDao,
    private val smbDataSource: SmbDataSource,
    private val smbManager: SmbConnectionManager
) {
    // ================
    // 本地数据库相关示例（保留/补充）
    // ================

    suspend fun getAllPlaylists() = withContext(Dispatchers.IO) {
        playlistDao.getAll()
    }

    // 这里示例插入一条/多条，方法名尽量通用，不会与现有调用冲突
    suspend fun insertPlaylists(vararg entities: com.example.nasonly.data.db.PlaylistEntity) =
        withContext(Dispatchers.IO) {
            playlistDao.insert(*entities)
        }

    // ================
    // SMB 相关：全部委托给你现有实现
    // ================

    /**
     * 获取（或创建）SMB上下文：透传到 SmbConnectionManager
     */
    suspend fun getOrCreateSmbContext(
        ip: String,
        port: Int,
        username: String,
        password: String
    ): Result<CIFSContext> = smbManager.getOrCreateContext(ip, port, username, password)

    /**
     * 执行 SMB 操作（带自动重试）：透传到 SmbConnectionManager
     */
    suspend fun <T> executeSmbWithRetry(
        context: CIFSContext,
        ip: String,
        port: Int,
        operation: suspend (CIFSContext) -> T
    ): Result<T> = smbManager.executeWithRetry(context, ip, port, operation)

    /**
     * 示例：列目录（如果你已有更细的方法，仍然可以在外层继续用原方法；这里提供一个通用委托）
     */
    suspend fun listDir(path: String, ctx: CIFSContext): Result<Array<SmbFile>> =
        withContext(Dispatchers.IO) {
            try {
                val dir = SmbFile(path, ctx)
                Result.success(dir.listFiles() ?: emptyArray())
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    /**
     * 主动关闭连接（保留原行为）
     */
    fun closeSmb(ip: String, port: Int, username: String) {
        smbManager.closeConnection(ip, port, username)
    }

    fun closeAllSmb() {
        smbManager.closeAllConnections()
    }
}
