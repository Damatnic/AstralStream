package com.astralplayer.community.api

// Generic API response wrapper
data class ApiResponse<T>(
    val success: Boolean,
    val data: T?,
    val message: String?,
    val errorCode: Int? = null
) {
    companion object {
        fun <T> success(data: T): ApiResponse<T> {
            return ApiResponse(success = true, data = data, message = null)
        }
        
        fun <T> error(message: String, errorCode: Int? = null): ApiResponse<T> {
            return ApiResponse(success = false, data = null, message = message, errorCode = errorCode)
        }
    }
}

// Playlist sharing request/response models
data class SharePlaylistRequest(
    val title: String,
    val description: String = "",
    val creatorName: String = "Anonymous",
    val videoIds: List<String>,
    val thumbnailUrl: String? = null,
    val password: String? = null,
    val isPublic: Boolean = true,
    val allowDownloads: Boolean = true,
    val allowComments: Boolean = true,
    val category: String = "GENERAL",
    val language: String = "en",
    val tags: List<String> = emptyList(),
    val expirationDays: Int = 30,
    val maxViews: Int? = null,
    val contentWarning: String? = null
)

data class SharePlaylistResponse(
    val id: String,
    val shareCode: String,
    val title: String,
    val description: String = "",
    val creatorName: String,
    val videoCount: Int,
    val thumbnailUrl: String? = null,
    val shareUrl: String,
    val category: String,
    val language: String,
    val createdAt: Long,
    val expirationTime: Long,
    val viewCount: Int = 0,
    val downloadCount: Int = 0,
    val likeCount: Int = 0,
    val commentCount: Int = 0,
    val isPublic: Boolean,
    val hasPassword: Boolean,
    val avgRating: Float = 0.0f,
    val ratingCount: Int = 0,
    val tags: List<String> = emptyList()
)

data class ImportPlaylistRequest(
    val password: String? = null,
    val importerName: String = "Anonymous"
)

data class ImportPlaylistResponse(
    val playlistId: String,
    val importedVideoCount: Int,
    val message: String
)

data class SharedPlaylistResponse(
    val id: String,
    val shareCode: String,
    val title: String,
    val description: String,
    val creatorName: String,
    val videoCount: Int,
    val totalDuration: Long = 0L,
    val thumbnailUrl: String? = null,
    val shareUrl: String,
    val category: String,
    val language: String,
    val createdAt: Long,
    val updatedAt: Long = createdAt,
    val expirationTime: Long,
    val viewCount: Int,
    val downloadCount: Int,
    val likeCount: Int,
    val commentCount: Int,
    val isPublic: Boolean,
    val hasPassword: Boolean,
    val avgRating: Float = 0.0f,
    val ratingCount: Int = 0,
    val tags: List<String> = emptyList(),
    val videos: List<SharedVideoResponse> = emptyList()
)

data class SharedVideoResponse(
    val id: String,
    val title: String,
    val duration: Long,
    val thumbnailUrl: String? = null,
    val format: String,
    val resolution: String,
    val fileSize: Long,
    val orderIndex: Int
)

// Playlist rating models
data class PlaylistRatingRequest(
    val rating: Float, // 1.0 to 5.0
    val review: String = "",
    val userId: String
)

// Subtitle contribution request/response models
data class ContributeSubtitleRequest(
    val videoHash: String,
    val videoTitle: String,
    val videoDuration: Long,
    val language: String,
    val languageCode: String,
    val content: String,
    val format: String = "srt",
    val contributorName: String = "Anonymous",
    val confidence: Float = 1.0f,
    val description: String = "",
    val tags: List<String> = emptyList(),
    val source: String = "USER_CREATED"
)

data class ContributeSubtitleResponse(
    val subtitleId: String,
    val status: String, // "submitted", "approved", "rejected"
    val message: String
)

data class CommunitySubtitleResponse(
    val id: String,
    val videoHash: String,
    val language: String,
    val languageCode: String = "",
    val contributorName: String,
    val content: String,
    val format: String,
    val createdAt: Long,
    val updatedAt: Long = createdAt,
    val version: Int,
    val upvotes: Int,
    val downvotes: Int,
    val confidence: Float,
    val verificationStatus: String,
    val downloadCount: Int = 0,
    val qualityScore: Float = 0.0f,
    val description: String = "",
    val tags: List<String> = emptyList(),
    val isOfficial: Boolean = false
)

// Subtitle voting models
data class SubtitleVoteRequest(
    val isUpvote: Boolean,
    val userId: String,
    val reason: String = ""
)

// Subtitle reporting models
data class SubtitleReportRequest(
    val reason: String, // INCORRECT_TIMING, WRONG_LANGUAGE, etc.
    val description: String = "",
    val reporterId: String,
    val severity: String = "MEDIUM"
)

// Subtitle download models
data class SubtitleDownloadResponse(
    val content: String,
    val format: String,
    val language: String,
    val lastModified: Long,
    val downloadCount: Int
)

// Community statistics models
data class ContributorResponse(
    val id: String,
    val name: String,
    val contributionCount: Int,
    val averageRating: Float,
    val reputation: Int,
    val joinedAt: Long,
    val isVerified: Boolean = false,
    val specialties: List<String> = emptyList()
)

data class ActivityResponse(
    val id: String,
    val type: String, // "playlist_shared", "subtitle_contributed", "vote_cast", etc.
    val title: String,
    val description: String,
    val actorName: String,
    val timestamp: Long,
    val metadata: Map<String, Any> = emptyMap()
)

// Search and filtering models
data class PlaylistSearchRequest(
    val query: String = "",
    val category: String? = null,
    val language: String? = null,
    val minRating: Float? = null,
    val maxAge: Int? = null, // Days
    val sortBy: String = "relevance", // "relevance", "popularity", "newest", "rating"
    val limit: Int = 20,
    val offset: Int = 0
)

data class SubtitleSearchRequest(
    val videoHash: String? = null,
    val language: String? = null,
    val minConfidence: Float? = null,
    val verificationStatus: String? = null,
    val sortBy: String = "quality", // "quality", "newest", "popularity"
    val limit: Int = 20,
    val offset: Int = 0
)

// Moderation models
data class ModerationAction(
    val action: String, // "approve", "reject", "flag", "hide"
    val reason: String = "",
    val moderatorId: String,
    val timestamp: Long = System.currentTimeMillis()
)

// Analytics models
data class CommunityStats(
    val totalPlaylists: Int,
    val totalSubtitles: Int,
    val totalContributors: Int,
    val totalViews: Long,
    val totalDownloads: Long,
    val topLanguages: List<LanguageStat>,
    val topCategories: List<CategoryStat>,
    val recentActivity: List<ActivityResponse>
)

data class LanguageStat(
    val language: String,
    val languageCode: String,
    val count: Int,
    val percentage: Float
)

data class CategoryStat(
    val category: String,
    val count: Int,
    val percentage: Float
)

// User profile models
data class UserProfile(
    val id: String,
    val name: String,
    val joinedAt: Long,
    val contributionCount: Int,
    val reputation: Int,
    val averageRating: Float,
    val isVerified: Boolean = false,
    val badges: List<String> = emptyList(),
    val specialties: List<String> = emptyList(),
    val recentContributions: List<ContributionSummary> = emptyList()
)

data class ContributionSummary(
    val id: String,
    val type: String, // "playlist", "subtitle"
    val title: String,
    val language: String? = null,
    val rating: Float,
    val votes: Int,
    val createdAt: Long
)

// Notification models
data class CommunityNotification(
    val id: String,
    val type: String, // "vote_received", "comment_added", "verification_status", etc.
    val title: String,
    val message: String,
    val isRead: Boolean = false,
    val timestamp: Long,
    val actionUrl: String? = null,
    val metadata: Map<String, Any> = emptyMap()
)

// Error models
data class ApiError(
    val code: String,
    val message: String,
    val details: Map<String, Any> = emptyMap(),
    val timestamp: Long = System.currentTimeMillis()
)

// Rate limiting models
data class RateLimit(
    val limit: Int,
    val remaining: Int,
    val resetTime: Long,
    val retryAfter: Int? = null
)