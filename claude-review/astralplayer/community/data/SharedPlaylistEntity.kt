package com.astralplayer.community.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index
import java.util.UUID

@Entity(
    tableName = "shared_playlists",
    indices = [
        Index(value = ["shareCode"], unique = true),
        Index(value = ["originalPlaylistId"]),
        Index(value = ["createdAt"]),
        Index(value = ["expirationTime"])
    ]
)
data class SharedPlaylistEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val originalPlaylistId: String, // References local playlist
    val shareCode: String, // 8-character shareable code
    val title: String,
    val description: String = "",
    val creatorName: String = "Anonymous",
    val creatorId: String, // Local user identifier
    val videoCount: Int,
    val totalDuration: Long, // In milliseconds
    val thumbnailUrl: String? = null,
    val shareUrl: String, // Full shareable URL
    val password: String? = null, // Hashed password if protected
    val isPublic: Boolean = true,
    val allowDownloads: Boolean = true,
    val allowComments: Boolean = true,
    val tags: String = "", // JSON array of tags
    val category: PlaylistCategory = PlaylistCategory.GENERAL,
    val language: String = "en",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val expirationTime: Long, // When the share expires
    val maxViews: Int? = null, // Optional view limit
    val viewCount: Int = 0,
    val downloadCount: Int = 0,
    val likeCount: Int = 0,
    val commentCount: Int = 0,
    val shareCount: Int = 0,
    val isActive: Boolean = true, // Can be deactivated without deletion
    val moderationStatus: ModerationStatus = ModerationStatus.APPROVED,
    val reportCount: Int = 0,
    val lastViewedAt: Long? = null,
    val avgRating: Float = 0.0f,
    val ratingCount: Int = 0,
    val fileSize: Long = 0L, // Total size of all videos
    val qualityTags: String = "", // HD, 4K, etc.
    val contentWarning: String? = null // Content warnings if any
)

enum class PlaylistCategory {
    GENERAL,
    ENTERTAINMENT,
    EDUCATION,
    MUSIC,
    SPORTS,
    NEWS,
    TECHNOLOGY,
    GAMING,
    LIFESTYLE,
    DOCUMENTARY,
    COMEDY,
    DRAMA,
    ACTION,
    HORROR,
    TUTORIAL,
    REVIEW,
    VLOG,
    OTHER
}

enum class ModerationStatus {
    PENDING,
    APPROVED,
    REJECTED,
    FLAGGED,
    UNDER_REVIEW,
    SUSPENDED
}

@Entity(
    tableName = "shared_playlist_videos",
    indices = [
        Index(value = ["playlistId"]),
        Index(value = ["orderIndex"])
    ]
)
data class SharedPlaylistVideoEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val playlistId: String, // References SharedPlaylistEntity
    val videoId: String, // Local video reference
    val title: String,
    val url: String,
    val thumbnailUrl: String? = null,
    val duration: Long, // In milliseconds
    val fileSize: Long,
    val format: String, // mp4, mkv, etc.
    val resolution: String, // 1080p, 720p, etc.
    val orderIndex: Int, // Position in playlist
    val description: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val isAvailable: Boolean = true // If video still exists locally
)

@Entity(
    tableName = "playlist_comments",
    indices = [
        Index(value = ["playlistId"]),
        Index(value = ["createdAt"]),
        Index(value = ["parentCommentId"])
    ]
)
data class PlaylistCommentEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val playlistId: String,
    val parentCommentId: String? = null, // For nested comments/replies
    val authorName: String,
    val authorId: String,
    val content: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val likeCount: Int = 0,
    val dislikeCount: Int = 0,
    val replyCount: Int = 0,
    val isEdited: Boolean = false,
    val isDeleted: Boolean = false,
    val moderationStatus: ModerationStatus = ModerationStatus.APPROVED,
    val reportCount: Int = 0
)

@Entity(
    tableName = "playlist_ratings",
    indices = [
        Index(value = ["playlistId", "userId"], unique = true),
        Index(value = ["createdAt"])
    ]
)
data class PlaylistRatingEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val playlistId: String,
    val userId: String,
    val rating: Float, // 1.0 to 5.0
    val review: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "playlist_views",
    indices = [
        Index(value = ["playlistId"]),
        Index(value = ["viewedAt"]),
        Index(value = ["userId"])
    ]
)
data class PlaylistViewEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val playlistId: String,
    val userId: String,
    val viewedAt: Long = System.currentTimeMillis(),
    val viewDuration: Long = 0L, // How long they viewed
    val userAgent: String = "",
    val ipAddress: String = "", // Hashed for privacy
    val location: String = "" // General location for stats
)