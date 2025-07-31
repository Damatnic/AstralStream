package com.astralplayer.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity for offline video storage
 */
@Entity(tableName = "offline_videos")
data class OfflineVideoEntity(
    @PrimaryKey
    val videoId: String,
    val originalUri: String,
    val localPath: String,
    val title: String,
    val duration: Long,
    val thumbnailPath: String?,
    val fileSize: Long,
    val downloadedAt: Long,
    val lastPlayedAt: Long?,
    val watchProgress: Long
)