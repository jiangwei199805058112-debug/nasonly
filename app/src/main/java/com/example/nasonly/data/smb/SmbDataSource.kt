package nasonly.data.smb

import android.util.Log
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.TransferListener
import jcifs.CIFSContext
import jcifs.smb.SmbFile
import jcifs.smb.SmbFileInputStream
import kotlinx.coroutines.runBlocking
import java.io.IOException
import java.io.InputStream

/**
 * 优化的SMB数据源 - 支持错误处理和重试机制
 */
class SmbDataSource(
    private val context: CIFSContext,
    private val ip: String,
    private val port: Int,
    private val connectionManager: SmbConnectionManager = SmbConnectionManager.getInstance()
) : DataSource {
    private val TAG = "SmbDataSource"

    private var smbFile: SmbFile? = null
    private var inputStream: SmbFileInputStream? = null
    private var transferListener: TransferListener? = null
    private var currentOffset = 0L

    override fun addTransferListener(listener: TransferListener) {
        this.transferListener = listener
    }

    override fun open(dataSpec: DataSpec): Long {
        return try {
            Log.d(TAG, "打开SMB文件: ${dataSpec.uri}")

            // 执行带重试的SMB操作
            val result = runBlocking {
                connectionManager.executeWithRetry(context, ip, port) { ctx ->
                    val file = SmbFile(dataSpec.uri.toString(), ctx)

                    // 检查文件是否存在且可读取
                    if (!file.exists() || !file.isFile || !file.canRead()) {
                        throw IOException("文件不存在或无法读取: ${dataSpec.uri}")
                    }

                    // 打开文件输入流
                    val stream = SmbFileInputStream(file)

                    // 处理偏移量（支持断点续传）
                    if (dataSpec.position > 0) {
                        stream.skip(dataSpec.position)
                        currentOffset = dataSpec.position
                    }

                    smbFile = file
                    inputStream = stream

                    // 通知传输开始
                    transferListener?.onTransferStart(this@SmbDataSource, dataSpec)

                    // 返回文件大小
                    file.length()
                }
            }

            if (result.isSuccess) {
                result.getOrThrow()
            } else {
                throw result.exceptionOrNull() ?: IOException("无法打开SMB文件")
            }
        } catch (e: Exception) {
            Log.e(TAG, "打开SMB文件失败: ${e.message}", e)
            close()
            throw IOException("SMB打开失败: ${e.message}", e)
        }
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        return try {
            val stream = inputStream ?: throw IOException("SMB流未打开")

            // 执行带重试的读取操作
            val result = runBlocking {
                connectionManager.executeWithRetry(context, ip, port) {
                    val bytesRead = stream.read(buffer, offset, length)
                    if (bytesRead > 0) {
                        currentOffset += bytesRead
                        transferListener?.onBytesTransferred(
                            this@SmbDataSource,
                            buffer,
                            offset,
                            bytesRead
                        )
                    }
                    bytesRead
                }
            }

            if (result.isSuccess) {
                result.getOrThrow()
            } else {
                throw result.exceptionOrNull() ?: IOException("SMB读取失败")
            }
        } catch (e: Exception) {
            Log.e(TAG, "SMB读取错误: ${e.message}", e)
            throw IOException("SMB读取失败: ${e.message}", e)
        }
    }

    override fun getUri() = smbFile?.url ?: null

    override fun close() {
        try {
            inputStream?.close()
            transferListener?.onTransferEnd(this, DataSpec(getUri() ?: ""))
        } catch (e: Exception) {
            Log.w(TAG, "关闭SMB流时出错: ${e.message}")
        } finally {
            inputStream = null
            smbFile = null
        }
    }

    /**
     * 数据源工厂
     */
    class Factory(
        private val context: CIFSContext,
        private val ip: String,
        private val port: Int
    ) : DataSource.Factory {
        override fun createDataSource(): DataSource {
            return SmbDataSource(context, ip, port)
        }
    }
}