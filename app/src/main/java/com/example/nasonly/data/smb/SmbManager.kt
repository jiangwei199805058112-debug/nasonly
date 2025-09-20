package com.example.nasonly.data.smb

import android.util.Log
import jcifs.CIFSContext
import jcifs.config.PropertyConfiguration
import jcifs.context.BaseContext
import jcifs.credential.NtlmPasswordAuthenticator
import jcifs.smb.SmbException
import jcifs.smb.SmbFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Properties
import java.util.concurrent.Executors
import java.util.concurrent.Future

class SmbManager {
    private val TAG = "SmbManager"
    private var context: CIFSContext? = null
    private var currentSmbVersion: String? = null

    // 局域网无加密场景：优先 SMB 1.0/2.0/2.1（兼容性最佳）
    private val SUPPORTED_SMB_VERSIONS = listOf("2.1", "2.0", "1.0")
    private val SCAN_THREAD_COUNT = 6 // 局域网高并发扫描（4-8线程最佳）

    // 视频文件扩展名
    private val VIDEO_EXTENSIONS = setOf(
        "mp4", "mkv", "avi", "mov", "flv", "wmv", "ts", "m4v"
    )

    /**
     * 建立 SMB 连接
     */
    fun connect(ip: String, port: Int, username: String, password: String): CIFSContext? {
        for (version in SUPPORTED_SMB_VERSIONS) {
            try {
                val props = Properties().apply {
                    setProperty("jcifs.smb.client.minVersion", version)
                    setProperty("jcifs.smb.client.maxVersion", version)
                }
                val baseContext = BaseContext(PropertyConfiguration(props))
                context = baseContext.withCredentials(NtlmPasswordAuthenticator(ip, username, password))
                currentSmbVersion = version
                Log.i(TAG, "SMB connected with version: $version")
                return context
            } catch (e: Exception) {
                Log.e(TAG, "SMB connect failed with version $version: ${e.message}")
            }
        }
        return null
    }

    /**
     * 异步扫描目录下的视频文件
     */
    suspend fun scanVideos(path: String): List<SmbFile> = withContext(Dispatchers.IO) {
        val results = mutableListOf<SmbFile>()
        val executor = Executors.newFixedThreadPool(SCAN_THREAD_COUNT)

        try {
            val smbFile = SmbFile(path, context)
            val files = smbFile.listFiles() ?: emptyArray()

            val futures = files.map { file ->
                executor.submit<Any> {
                    if (file.isDirectory) {
                        try {
                            results.addAll(scanVideos(file.canonicalPath))
                        } catch (e: SmbException) {
                            Log.e(TAG, "Failed to scan directory: ${file.canonicalPath}")
                        }
                    } else if (isVideoFile(file)) {
                        synchronized(results) {
                            results.add(file)
                        }
                    }
                }
            }

            futures.forEach(Future<*>::get)
        } catch (e: Exception) {
            Log.e(TAG, "SMB scan error: ${e.message}")
        } finally {
            executor.shutdown()
        }
        results
    }

    /**
     * 判断是否为视频文件
     */
    private fun isVideoFile(file: SmbFile): Boolean {
        val ext = file.name.substringAfterLast('.', "").lowercase()
        return ext in VIDEO_EXTENSIONS
    }

    fun getContext(): CIFSContext? = context
    fun getCurrentSmbVersion(): String? = currentSmbVersion
}
