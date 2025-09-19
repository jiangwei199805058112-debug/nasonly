package nasonly.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "scan_progress")
data class ScanProgress(
    @PrimaryKey val id: Int = 1, // 单条记录
    val lastScannedPath: String,
    val timestamp: Long,
    val totalFiles: Int,
    val scannedFiles: Int
)