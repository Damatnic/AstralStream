package com.astralstream.nextplayer.database.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "cached_subtitles",
    indices = [
        Index(value = ["videoUri"]),
        Index(value = ["language"]),
        Index(value = ["lastAccessed"]),
        Index(value = ["timestamp"])
    ]
)
data class CachedSubtitleEntity(
    @PrimaryKey
    val cacheKey: String,
    val videoUri: String,
    val language: String,
    val filePath: String,
    val fileSize: Long,
    val sourceType: String,
    val timestamp: Long,
    val lastAccessed: Long,
    val accessCount: Int,
    val confidence: Float,
    val isUserContributed: Boolean = false,
    val contributorId: String? = null,
    val checksum: String? = null
)