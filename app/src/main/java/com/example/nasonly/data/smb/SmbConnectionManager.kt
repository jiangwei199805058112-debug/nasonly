package com.example.nasonly.data.smb

import android.util.Log
import jcifs.CIFSContext
import jcifs.config.PropertyConfiguration
import jcifs.context.BaseContext
import jcifs.smb.NtlmPasswordAuthenticator
import jcifs.smb.SmbException
import jcifs.smb.SmbFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.SocketException
import java.util.Properties
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * SMB 连接管理器
 * - 连接池：按 ip:port:username 复用 CIFSContext
 * - 自动校验、必要时重连
 * - 可选监听回调
 * - 与 Hilt 集成：@Singleton + @Inject 构造
 *
 * 说明：
 * 1) 与你原始代码保持同等的功能结构；删除了对 context.credentials 的直接访问（jcifs-ng 未公开该 API），
 *    改为我们在连接池中自行保存 username/password 以便重连。
 * 2) 移除了手写 getInstance() 单例，改用 Hilt 管理单例。如果历史代码里有 getInstance() 调用，
 *    请改为构造函数/字段注入：e.g. `@Inject lateinit var smb: SmbConnectionManager`
 */
@Singleton
class SmbConnectionManager @Inject constructor() {

    private val TAG = "SmbConnectionManager"

    // 连接池：key = ip:port:username
    private data class PoolEntry(
        val context: CIFSContext,
        val username: String,
        val password: String
    )
    private val connectionPool = ConcurrentHashMap<String, PoolEntry>()

    // 连接状态监听
    private var connectionListener: SmbConnectionListener? = null

    // 重试配置
    private val MAX_RETRIES = 3
    private val RETRY_DELAY = 1_000L // 初始重试间隔（毫秒）

    // 超时配置（毫秒）
    private val CONNECTION_TIMEOUT = 10_000
    private val READ_TIMEOUT = 30_000

    /**
     * 获取或创建 SMB CIFSContext
     */
    suspend fun getOrCreateContext(
        ip: String,
        port: Int,
        username: String,
        password: String
    ): Result<CIFSContext> = withContext(Dispatchers.IO) {
        val key = poolKey(ip, port, username)

        // 命中连接池且有效
        connectionPool[key]?.let { entry ->
            if (isConnectionValid(entry.context, ip, port)) {
                Log.d(TAG, "使用缓存连接：$key")
                return@withContext Result.success(entry.context)
            } else {
                Log.d(TAG, "缓存连接无效，移除并重建：$key")
                connectionPool.remove(key)
            }
        }

        // 新建配置
        val props = Properties().apply {
            setProperty("jcifs.smb.client.connTimeout", CONNECTION_TIMEOUT.toString())
            setProperty("jcifs.smb.client.responseTimeout", READ_TIMEOUT.toString())
            setProperty("jcifs.smb.client.soTimeout", READ_TIMEOUT.toString())
            setProperty("jcifs.smb.client.retries", "2")
            setProperty("jcifs.netbios.cachePolicy", "600")             // 10 分钟缓存
            setProperty("jcifs.smb.client.sessionTimeout", "300000")    // 5 分钟会话超时
            // 如需限定 SMB 版本，可按需启用：
            // setProperty("jcifs.smb.client.minVersion", "SMB202")
            // setProperty("jcifs.smb.client.maxVersion", "SMB311")
        }
        val config = PropertyConfiguration(props)
        val base = BaseContext(config)

        // 凭据（用户名/密码都空时按匿名访问处理）
        val auth = if (username.isBlank() && password.isBlank()) {
            NtlmPasswordAuthenticator.ANONYMOUS
        } else {
            // domain 为空字符串即可
            NtlmPasswordAuthenticator("", username, password)
        }

        val context = base.withCredentials(auth)

        // 验证连通性
        return@withContext try {
            if (isConnectionValid(context, ip, port)) {
                connectionPool[key] = PoolEntry(context, username, password)
                Log.d(TAG, "创建连接成功：$key")
                connectionListener?.onConnected(ip, port)
                Result.success(context)
            } else {
                val msg = "连接验证失败：$key"
                Log.e(TAG, msg)
                connectionListener?.onConnectionFailed(ip, port, msg)
                Result.failure(IOException(msg))
            }
        } catch (e: Exception) {
            val msg = "创建 SMB 连接失败：${e.message}"
            Log.e(TAG, msg, e)
            connectionListener?.onConnectionFailed(ip, port, msg)
            Result.failure(e)
        }
    }

    /**
     * 验证连接是否有效：访问根目录
     */
    private suspend fun isConnectionValid(
        context: CIFSContext,
        ip: String,
        port: Int
    ): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            val testPath = if (port == 445) "smb://$ip/" else "smb://$ip:$port/"
            val root = SmbFile(testPath, context)
            root.list() != null
        } catch (e: Exception) {
            Log.w(TAG, "连接验证失败：${e.message}")
            false
        }
    }

    /**
     * 执行 SMB 操作，并带自动重试。
     *
     * 注意：如需自动重建连接，我们会尝试通过“连接池中已保存的 username/password”重建；
     * 若找不到对应记录，将返回失败并提示你先调用 getOrCreateContext 获取新的 context。
     */
    suspend fun <T> executeWithRetry(
        context: CIFSContext,
        ip: String,
        port: Int,
        operation: suspend (CIFSContext) -> T
    ): Result<T> {
        var last: Exception? = null

        val keyFromPool = findKeyByContext(context)

        for (attempt in 1..MAX_RETRIES) {
            try {
                // 1) 校验连接
                if (!isConnectionValid(context, ip, port)) {
                    Log.w(TAG, "连接失效，尝试重建（$attempt/$MAX_RETRIES）")

                    if (keyFromPool == null) {
                        val msg = "无法重建：未在连接池中找到凭据，请先调用 getOrCreateContext()。"
                        Log.e(TAG, msg)
                        return Result.failure(IllegalStateException(msg))
                    }

                    val entry = connectionPool[keyFromPool]
                        ?: return Result.failure(IllegalStateException("连接记录不存在，需重新登录。"))

                    val newCtx = getOrCreateContext(ip, port, entry.username, entry.password)
                        .getOrElse { throw it }

                    connectionListener?.onReconnected(ip, port)
                    return Result.success(operation(newCtx))
                }

                // 2) 执行操作
                return Result.success(operation(context))
            } catch (e: Exception) {
                last = e
                Log.e(TAG, "SMB 操作失败（$attempt/$MAX_RETRIES）：${e.message}", e)

                if (!shouldRetry(e) || attempt == MAX_RETRIES) break

                val backoff = RETRY_DELAY * (1 shl (attempt - 1))
                delay(backoff)
            }
        }

        val msg = "SMB 操作在重试 $MAX_RETRIES 次后仍失败：${last?.message}"
        connectionListener?.onOperationFailed(msg, last)
        return Result.failure(last ?: IOException(msg))
    }

    /**
     * 根据异常类型判断是否应重试
     */
    private fun shouldRetry(e: Exception): Boolean {
        return when (e) {
            is SmbException -> when (e.status) {
                // 登录失败等不重试
                SmbException.NT_STATUS_LOGON_FAILURE -> false
                // 连接类问题可重试
                SmbException.NT_STATUS_NETWORK_SESSION_EXPIRED,
                SmbException.NT_STATUS_CONNECTION_DISCONNECTED -> true
                else -> true
            }
            is SocketException -> true
            is IOException -> {
                val msg = e.message?.lowercase().orEmpty()
                "timeout" in msg || "reset" in msg || "closed" in msg
            }
            else -> false
        }
    }

    /**
     * 关闭指定连接
     */
    fun closeConnection(ip: String, port: Int, username: String) {
        val key = poolKey(ip, port, username)
        if (connectionPool.remove(key) != null) {
            Log.d(TAG, "关闭 SMB 连接：$key")
        }
    }

    /**
     * 关闭所有连接
     */
    fun closeAllConnections() {
        connectionPool.clear()
        Log.d(TAG, "已清空所有 SMB 连接")
    }

    /**
     * 设置监听器
     */
    fun setConnectionListener(listener: SmbConnectionListener) {
        this.connectionListener = listener
    }

    interface SmbConnectionListener {
        fun onConnected(ip: String, port: Int)
        fun onConnectionFailed(ip: String, port: Int, error: String)
        fun onOperationFailed(error: String, exception: Exception?)
        fun onReconnected(ip: String, port: Int)
    }

    // —— 私有工具 —— //

    private fun poolKey(ip: String, port: Int, username: String) =
        "$ip:$port:$username"

    /** 通过 context 找回连接池 key（用于重连时取凭据） */
    private fun findKeyByContext(context: CIFSContext): String? {
        // 连接池一般很小，线性扫描足够
        return connectionPool.entries.firstOrNull { it.value.context === context }?.key
    }
}
