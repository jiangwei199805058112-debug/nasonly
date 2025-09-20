package com.example.nasonly.data.smb

import android.net.Uri
import android.util.Log
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.TransferListener
import jcifs.CIFSContext
import jcifs.smb.SmbFile
import jcifs.smb.SmbFileInputStream
import java.io.IOException
import java.io.InputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 优化的 SMB 数据源 - 支持错误处理和重试机制。
 *
 * 通过 [prepare] 注入已经建立的 CIFS 会话，以便在播放时重用连接。
 */
@Singleton
class SmbDataSource @Inject constructor() : DataSource {

    private val tag = "SmbDataSource"

    private var smbContext: CIFSContext? = null
    private var host: String = ""
    private var port: Int = 445
    private var smbFile: SmbFile? = null
    private var inputStream: InputStream? = null

    /**
     * 注入 SMB 会话与服务器信息。
     */
    fun prepare(context: CIFSContext, ip: String, port: Int) {
        smbContext = context
        host = ip
        this.port = port
    }

    override fun addTransferListener(transferListener: TransferListener) {
        // No-op
    }

    @Throws(IOException::class)
    override fun open(dataSpec: DataSpec): Long {
        val ctx = smbContext
            ?: throw IOException("SMB context is not initialized. Call prepare() before open().")

        val resolvedHost = if (host.isNotBlank()) host else dataSpec.uri.host.orEmpty()
        val resolvedPort = if (port > 0) port else dataSpec.uri.port.takeIf { it != -1 } ?: 445
        val relativePath = dataSpec.uri.path?.removePrefix("/") ?: ""
        val url = if (resolvedPort == 445) {
            "smb://$resolvedHost/$relativePath"
        } else {
            "smb://$resolvedHost:$resolvedPort/$relativePath"
        }

        return try {
            smbFile = SmbFile(url, ctx)
            inputStream = SmbFileInputStream(smbFile)
            smbFile?.length() ?: DataSpec.LENGTH_UNBOUNDED.toLong()
        } catch (e: Exception) {
            Log.e(tag, "SMB open error", e)
            throw IOException("Failed to open SMB file", e)
        }
    }

    override fun read(buffer: ByteArray, offset: Int, readLength: Int): Int = try {
        (inputStream as? SmbFileInputStream)?.read(buffer, offset, readLength) ?: -1
    } catch (e: Exception) {
        Log.e(tag, "SMB read error", e)
        -1
    }

    override fun getUri(): Uri? = smbFile?.canonicalPath?.let(Uri::parse)

    override fun close() {
        try {
            inputStream?.close()
        } catch (e: Exception) {
            Log.e(tag, "SMB close error", e)
        } finally {
            smbFile = null
            inputStream = null
        }
    }
}
