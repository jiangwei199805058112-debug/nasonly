package nasonly.core.utils

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
}