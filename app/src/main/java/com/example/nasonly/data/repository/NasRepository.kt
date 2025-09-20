package com.example.nasonly.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.nasonly.data.db.PlaylistDao
import com.example.nasonly.data.db.PlaylistEntity
import com.example.nasonly.data.db.ScanProgress
import com.example.nasonly.data.db.ScanProgressDao
import com.example.nasonly.data.db.VideoDao
import com.example.nasonly.data.db.VideoEntity
import com.example.nasonly.data.smb.SmbConnectionManager
import dagger.hilt.android.qualifiers.ApplicationContext
import jcifs.CIFSContext
import jcifs.smb.SmbFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 统一的数据仓库：
 * - 本地数据库（Room）
 * - SMB 网络访问（委托给你的实现）
 */
@Singleton
class NasRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val playlistDao: PlaylistDao,
    private val videoDao: VideoDao,
    private val scanProgressDao: ScanProgressDao,
    private val smbManager: SmbConnectionManager
) {

    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    // =====================
    // 本地数据库相关
    // =====================

    suspend fun getAllPlaylists(): List<PlaylistEntity> = withContext(Dispatchers.IO) {
        playlistDao.observeAll().firstOrNull() ?: emptyList()
    }

    suspend fun insertPlaylists(vararg entities: PlaylistEntity) = withContext(Dispatchers.IO) {
        entities.forEach { playlistDao.insert(it) }
    }

    suspend fun shouldScanAgain(): Boolean = withContext(Dispatchers.IO) {
        val last = scanProgressDao.getLastScanProgress().firstOrNull() ?: return@withContext true
        val now = System.currentTimeMillis()
        now - last.timestamp > SCAN_VALID_DURATION_MS
    }

    suspend fun scanAndUpdateMediaLibrary() = withContext(Dispatchers.IO) {
        // 这里保留一个简单实现：实际应用中应扫描 SMB 目录并更新数据库。
        val progress = ScanProgress(
            id = 1,
            lastScannedPath = "",
            timestamp = System.currentTimeMillis(),
            totalFiles = 0,
            scannedFiles = 0
        )
        scanProgressDao.updateProgress(progress)
    }

    suspend fun getAllVideos(): List<VideoEntity> = withContext(Dispatchers.IO) {
        videoDao.getAllVideosSortedByName().firstOrNull() ?: emptyList()
    }

    // =====================
    // NAS 配置相关
    // =====================

    suspend fun saveNasConfig(config: NasConfig) = withContext(Dispatchers.IO) {
        prefs.edit().apply {
            putString(KEY_IP, config.ip)
            putInt(KEY_PORT, config.port)
            putString(KEY_USERNAME, config.username)
            putBoolean(KEY_SAVE_CREDENTIALS, config.saveCredentials)
            if (config.saveCredentials) {
                putString(KEY_PASSWORD, config.password)
            } else {
                remove(KEY_PASSWORD)
            }
        }.apply()
    }

    suspend fun getSavedNasConfig(): NasConfig? = withContext(Dispatchers.IO) {
        val ip = prefs.getString(KEY_IP, null) ?: return@withContext null
        val port = prefs.getInt(KEY_PORT, DEFAULT_SMB_PORT)
        val username = prefs.getString(KEY_USERNAME, "") ?: ""
        val saveCredentials = prefs.getBoolean(KEY_SAVE_CREDENTIALS, false)
        val password = if (saveCredentials) {
            prefs.getString(KEY_PASSWORD, "") ?: ""
        } else {
            ""
        }
        NasConfig(ip, port, username, password, saveCredentials)
    }

    suspend fun getSmbContext(): CIFSContext? {
        val config = getSavedNasConfig() ?: return null
        return smbManager
            .getOrCreateContext(config.ip, config.port, config.username, config.password)
            .getOrNull()
    }

    // =====================
    // SMB 相关委托
    // =====================

    suspend fun getOrCreateSmbContext(
        ip: String,
        port: Int,
        username: String,
        password: String
    ): Result<CIFSContext> = smbManager.getOrCreateContext(ip, port, username, password)

    suspend fun <T> executeSmbWithRetry(
        context: CIFSContext,
        ip: String,
        port: Int,
        operation: suspend (CIFSContext) -> T
    ): Result<T> = smbManager.executeWithRetry(context, ip, port, operation)

    suspend fun listDir(path: String, ctx: CIFSContext): Result<Array<SmbFile>> =
        withContext(Dispatchers.IO) {
            try {
                val dir = SmbFile(path, ctx)
                Result.success(dir.listFiles() ?: emptyArray())
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    fun closeSmb(ip: String, port: Int, username: String) {
        smbManager.closeConnection(ip, port, username)
    }

    fun closeAllSmb() {
        smbManager.closeAllConnections()
    }

    companion object {
        private const val PREFS_NAME = "nas_config"
        private const val KEY_IP = "key_ip"
        private const val KEY_PORT = "key_port"
        private const val KEY_USERNAME = "key_username"
        private const val KEY_PASSWORD = "key_password"
        private const val KEY_SAVE_CREDENTIALS = "key_save_credentials"
        private const val DEFAULT_SMB_PORT = 445
        private const val SCAN_VALID_DURATION_MS = 12 * 60 * 60 * 1000L // 12 小时
    }
}
