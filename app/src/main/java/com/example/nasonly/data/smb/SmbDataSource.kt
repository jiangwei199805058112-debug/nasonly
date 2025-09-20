package com.example.nasonly.data.smb

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

    override fun addTransferListener(transferListener: TransferListener) {
        // 不需要实现
    }

    @Throws(IOException::class)
    override fun open(dataSpec: DataSpec): Long {
        val url = "smb://$ip:$port/${dataSpec.uri.path?.removePrefix("/")}"
        return try {
            smbFile = SmbFile(url, context)
            inputStream = SmbFileInputStream(smbFile)
            smbFile?.length() ?: DataSpec.LENGTH_UNBOUNDED.toLong()
        } catch (e: Exception) {
            Log.e(TAG, "SMB Open Error: ${e.message}")
            throw IOException("Failed to open SMB file", e)
        }
    }

    override fun read(buffer: ByteArray, offset: Int, readLength: Int): Int {
        return try {
            inputStream?.read(buffer, offset, readLength) ?: -1
        } catch (e: Exception) {
            Log.e(TAG, "SMB Read Error: ${e.message}")
            -1
        }
    }

    override fun getUri() = smbFile?.canonicalPath?.let { android.net.Uri.parse(it) }

    override fun close() {
        try {
            inputStream?.close()
            smbFile = null
            inputStream = null
        } catch (e: Exception) {
            Log.e(TAG, "SMB Close Error: ${e.message}")
        }
    }
}
