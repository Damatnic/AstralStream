package com.astralplayer.nextplayer.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo
import java.util.Date

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    @ColumnInfo(name = "name")
    val name: String,
    
    @ColumnInfo(name = "description")
    val description: String? = null,
    
    @ColumnInfo(name = "thumbnail_path")
    val thumbnailPath: String? = null,
    
    @ColumnInfo(name = "created_date")
    val createdDate: Date = Date(),
    
    @ColumnInfo(name = "modified_date")
    val modifiedDate: Date = Date(),
    
    @ColumnInfo(name = "is_favorite")
    val isFavorite: Boolean = false,
    
    @ColumnInfo(name = "play_count")
    val playCount: Int = 0,
    
    @ColumnInfo(name = "total_duration")
    val totalDuration: Long = 0L, // in milliseconds
    
    @ColumnInfo(name = "video_count")
    val videoCount: Int = 0
)