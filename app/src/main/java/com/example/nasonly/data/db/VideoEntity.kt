package com.example.nasonly.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "videos")
data class VideoEntity(
    @PrimaryKey val id: String,
    val name: String,
    val url: String,
    val size: Long,
    val lastModified: Long,
    val parentPath: String,
    val thumbnailPath: String? = null,
    val duration: Long = 0 // 视频时长（毫秒）
)
