package com.astralplayer.astralstream.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Data class representing chapter information for videos
 */
@Entity(tableName = "chapters")
data class ChapterInfo(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    @ColumnInfo(name = "video_id")
    val videoId: Long,
    
    @ColumnInfo(name = "title")
    val title: String,
    
    @ColumnInfo(name = "start_time_ms")
    val startTimeMs: Long,
    
    @ColumnInfo(name = "end_time_ms")
    val endTimeMs: Long,
    
    @ColumnInfo(name = "thumbnail_url")
    val thumbnailUrl: String? = null,
    
    @ColumnInfo(name = "description")
    val description: String? = null,
    
    @ColumnInfo(name = "chapter_index")
    val chapterIndex: Int,
    
    @ColumnInfo(name = "is_user_created")
    val isUserCreated: Boolean = false,
    
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)