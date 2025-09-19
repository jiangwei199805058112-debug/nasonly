package nasonly.data.smb

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

    // 局域网无加密场景：优先SMB 1.0/2.0（兼容性最佳）
    private val SUPPORTED_SMB_VERSIONS = listOf("2.1", "2.0", "1.0")
    private val SCAN_THREAD_COUNT = 6 // 局域网高并发扫描（4-8线程最佳）

    // 视频文件扩展名
    private val VIDEO_EXTENSIONS = setOf(
        ".mp4", ".mkv", ".avi", ".mov", ".flv",
        ".wmv", ".mpeg", ".mpg", ".m4v", ".webm"
    )

    /**
     * 简化的SMB连接（移除加密逻辑）
     */
    suspend fun connect(ip: String, port: Int, username: String, password: String): Boolean {
        return withContext(Dispatchers.IO) {
            // 尝试局域网优先的SMB版本
            for (version in SUPPORTED_SMB_VERSIONS) {
                try {
                    // 局域网专属配置：简化属性，延长超时
                    val props = createLanSmbProperties(port, version)
                    val config = PropertyConfiguration(props)
                    val baseContext = BaseContext(config)

                    // 仅支持Ntlm认证（局域网最常用）
                    val auth = NtlmPasswordAuthenticator(username, password)
                    val context = baseContext.withCredentials(auth)

                    // 测试连接（局域网可直接访问根目录）
                    val testPath = if (port == 445) "smb://$ip/" else "smb://$ip:$port/"
                    val testFile = SmbFile(testPath, context)

                    // 局域网验证：能列出目录即视为连接成功
                    if (testFile.list() != null) {
                        this@SmbManager.context = context
                        this@SmbManager.currentSmbVersion = version
                        Log.d(TAG, "局域网连接成功，使用SMB $version")
                        return@withContext true
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "SMB $version 连接失败: ${e.message}")
                    continue
                }
            }
            Log.e(TAG, "所有SMB版本均连接失败，请检查NAS配置")
            false
        }
    }

    /**
     * 局域网专属SMB属性配置（移除加密，优化超时）
     */
    private fun createLanSmbProperties(port: Int, version: String): Properties {
        return Properties().apply {
            setProperty("jcifs.smb.client.port", port.toString())
            setProperty("jcifs.smb.client.responseTimeout", "15000") // 局域网延长超时
            setProperty("jcifs.smb.client.soTimeout", "30000")
            setProperty("jcifs.smb.client.minVersion", version)
            setProperty("jcifs.smb.client.maxVersion", version)
            setProperty("jcifs.smb.client.useBatching", "true") // 局域网批量请求优化
            setProperty("jcifs.smb.client.dfs.disabled", "true") // 关闭DFS（局域网无需）
        }
    }

    /**
     * 局域网多线程增量扫描（核心性能优化）
     */
    suspend fun scanVideoFiles(
        rootPath: String,
        lastScannedFiles: Map<String, Pair<Long, Long>> = emptyMap()
    ): List<SmbFile> {
        return withContext(Dispatchers.IO) {
            val videoFiles = mutableListOf<SmbFile>()
            val context = this@SmbManager.context ?: return@withContext emptyList()

            try {
                val rootDir = SmbFile(rootPath, context)
                if (!rootDir.exists() || !rootDir.isDirectory) {
                    Log.e(TAG, "根目录无效: $rootPath")
                    return@withContext emptyList()
                }

                // 局域网多线程扫描：按子目录分配线程
                val executor = Executors.newFixedThreadPool(SCAN_THREAD_COUNT)
                val futureList = mutableListOf<Future<List<SmbFile>>>()

                rootDir.listFiles()?.forEach { file ->
                    if (file.isDirectory) {
                        // 子目录并行扫描
                        futureList.add(executor.submit {
                            val dirVideos = mutableListOf<SmbFile>()
                            scanSingleDirectory(file, dirVideos, lastScannedFiles)
                            dirVideos
                        })
                    } else if (isVideoFile(file.name)) {
                        // 根目录文件直接处理
                        if (isFileUpdated(file, lastScannedFiles)) {
                            videoFiles.add(file)
                        }
                    }
                }

                // 合并线程结果
                futureList.forEach { future ->
                    videoFiles.addAll(future.get())
                }
                executor.shutdown()

            } catch (e: Exception) {
                Log.e(TAG, "扫描失败: ${e.message}", e)
            }
            videoFiles
        }
    }

    /**
     * 单目录扫描（供多线程调用）
     */
    private fun scanSingleDirectory(
        directory: SmbFile,
        videoFiles: MutableList<SmbFile>,
        lastScannedFiles: Map<String, Pair<Long, Long>>
    ) {
        try {
            val files = directory.listFiles() ?: return
            for (file in files) {
                try {
                    if (file.isDirectory) {
                        scanSingleDirectory(file, videoFiles, lastScannedFiles)
                    } else if (isVideoFile(file.name) && isFileUpdated(file, lastScannedFiles)) {
                        videoFiles.add(file)
                    }
                } catch (e: SmbException) {
                    Log.w(TAG, "无权限访问: ${file.path}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "扫描目录失败: ${directory.path}", e)
        }
    }

    /**
     * 简化的文件更新判断（局域网无需复杂校验）
     */
    private fun isFileUpdated(
        file: SmbFile,
        lastScannedFiles: Map<String, Pair<Long, Long>>
    ): Boolean {
        val key = file.path
        val lastInfo = lastScannedFiles[key] ?: return true
        return file.length() != lastInfo.first || file.lastModified() != lastInfo.second
    }

    // 其他原有方法
    fun getCurrentSmbVersion(): String? = currentSmbVersion
    fun getSmbUrl(file: SmbFile): String = file.url
    fun isVideoFile(fileName: String): Boolean =
        VIDEO_EXTENSIONS.any { fileName.lowercase().endsWith(it) }
    fun disconnect() { context = null; currentSmbVersion = null }
}