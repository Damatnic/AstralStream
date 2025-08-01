package com.astralplayer.astralstream.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index
import java.util.UUID

@Entity(
    tableName = "cached_subtitles",
    indices = [
        Index(value = ["videoId", "language"], unique = true),
        Index(value = ["lastAccessTime"]),
        Index(value = ["fileSize"]),
        Index(value = ["source"]),
        Index(value = ["createdTime"])
    ]
)
data class CachedSubtitleEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val videoId: String,
    val videoUrl: String,
    val videoTitle: String,
    val language: String,
    val languageCode: String, // ISO 639-1 code (en, es, fr, etc.)
    val content: String, // Encrypted subtitle content
    val format: SubtitleFormat,
    val isEncrypted: Boolean = true,
    val encryptionIv: String? = null, // Initialization vector for AES
    val createdTime: Long = System.currentTimeMillis(),
    val lastAccessTime: Long = System.currentTimeMillis(),
    val accessCount: Int = 0,
    val fileSize: Long, // Size in bytes
    val checksum: String, // SHA-256 hash for integrity
    val source: SubtitleSource,
    val confidence: Float = 1.0f, // AI confidence score (0.0 - 1.0)
    val syncOffset: Long = 0L, // Timing offset in milliseconds
    val version: Int = 1, // Schema version for migrations
    val compressionType: CompressionType = CompressionType.NONE,
    val originalSize: Long = fileSize, // Size before compression
    val tags: String = "", // JSON array of tags ["auto-generated", "high-quality"]
    val providerName: String = "", // "OpenAI", "Google", "Azure", etc.
    val processingTime: Long = 0L, // Time taken to generate (ms)
    val qualityScore: Float = 1.0f // Quality assessment score
)

enum class SubtitleFormat {
    SRT, VTT, SSA, ASS, TTML, SBV, WEBVTT, SUB
}

enum class SubtitleSource {
    AI_GENERATED, 
    USER_UPLOADED, 
    COMMUNITY_CONTRIBUTED, 
    EMBEDDED, 
    EXTERNAL_API,
    AUTO_DOWNLOADED,
    MANUAL_IMPORT
}

enum class CompressionType {
    NONE, GZIP, DEFLATE
}