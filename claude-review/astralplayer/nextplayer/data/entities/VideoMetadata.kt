package com.astralplayer.nextplayer.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "video_metadata")
data class VideoMetadata(
    @PrimaryKey
    val videoPath: String,
    val title: String,
    val duration: Long,
    val size: Long,
    val dateAdded: Date,
    val lastPlayed: Date? = null,
    val playbackPosition: Long = 0,
    val isFavorite: Boolean = false
)