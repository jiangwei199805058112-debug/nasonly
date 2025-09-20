package com.example.nasonly.core.utils

object FileUtils {
    /**
     * 格式化文件大小显示
     */
    fun formatFileSize(bytes: Long): String {
        return when {
            bytes >= 1024 * 1024 * 1024 -> {
                val gb = bytes.toDouble() / (1024 * 1024 * 1024)
                String.format("%.2f GB", gb)
            }
            bytes >= 1024 * 1024 -> {
                val mb = bytes.toDouble() / (1024 * 1024)
                String.format("%.2f MB", mb)
            }
            bytes >= 1024 -> {
                val kb = bytes.toDouble() / 1024
                String.format("%.2f KB", kb)
            }
            else -> "$bytes B"
        }
    }

    /**
     * 文件大小单位枚举
     */
    enum class FileSizeUnit {
        B, KB, MB, GB;

        fun toKB(bytes: Long): Double = bytes / 1024.0
        fun toMB(bytes: Long): Double = bytes / (1024.0 * 1024.0)
        fun toGB(bytes: Long): Double = bytes / (1024.0 * 1024.0 * 1024.0)
    }
}
