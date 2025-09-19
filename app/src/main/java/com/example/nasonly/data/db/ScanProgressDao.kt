package nasonly.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ScanProgressDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateProgress(progress: ScanProgress)

    @Query("SELECT * FROM scan_progress LIMIT 1")
    fun getLastScanProgress(): Flow<ScanProgress?>

    @Query("DELETE FROM scan_progress")
    suspend fun clearProgress()
}