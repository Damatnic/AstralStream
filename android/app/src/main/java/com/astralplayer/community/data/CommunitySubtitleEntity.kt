package com.astralplayer.community.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index
import java.util.UUID

@Entity(
    tableName = "community_subtitles",
    indices = [
        Index(value = ["videoHash", "language"]),
        Index(value = ["contributorId"]),
        Index(value = ["createdAt"]),
        Index(value = ["verificationStatus"]),
        Index(value = ["upvotes"]),
        Index(value = ["confidence"])
    ]
)
data class CommunitySubtitleEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val videoHash: String, // Hash of video content for identification
    val videoTitle: String,
    val videoDuration: Long, // In milliseconds
    val language: String,
    val languageCode: String, // ISO 639-1 code
    val content: String, // Subtitle content in SRT format
    val format: String = "srt", // srt, vtt, ass, etc.
    val contributorId: String, // Anonymous hash of contributor
    val contributorName: String = "Anonymous",
    val contributorReputation: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val version: Int = 1, // For subtitle revisions
    val parentSubtitleId: String? = null, // If this is a revision
    val upvotes: Int = 0,
    val downvotes: Int = 0,
    val confidence: Float = 1.0f, // Quality confidence score
    val verificationStatus: SubtitleVerificationStatus = SubtitleVerificationStatus.PENDING,
    val verifiedAt: Long? = null,
    val verifiedBy: String? = null, // Moderator ID
    val downloadCount: Int = 0,
    val reportCount: Int = 0,
    val qualityScore: Float = 0.0f, // Computed quality score
    val syncOffset: Long = 0L, // Timing offset in milliseconds
    val description: String = "", // Description of changes/notes
    val tags: String = "", // JSON array of tags like ["corrected", "professional"]
    val isOfficial: Boolean = false, // Marked as official/verified
    val source: SubtitleContributionSource = SubtitleContributionSource.USER_CREATED,
    val checksum: String, // Content integrity check
    val fileSize: Long,
    val encoding: String = "UTF-8",
    val lineCount: Int = 0,
    val avgLineLength: Float = 0.0f,
    val hasTimingIssues: Boolean = false,
    val hasSpellingIssues: Boolean = false,
    val completionPercentage: Float = 100.0f // Percentage of video covered
)

enum class SubtitleVerificationStatus {
    PENDING,
    AUTO_VERIFIED,
    COMMUNITY_VERIFIED,
    MODERATOR_VERIFIED,
    REJECTED,
    FLAGGED,
    HIDDEN,
    UNDER_REVIEW
}

enum class SubtitleContributionSource {
    USER_CREATED,
    AI_GENERATED,
    IMPORTED,
    COLLABORATIVE,
    PROFESSIONAL,
    COMMUNITY_EDITED
}

@Entity(
    tableName = "subtitle_votes",
    indices = [
        Index(value = ["subtitleId", "userId"], unique = true),
        Index(value = ["createdAt"])
    ]
)
data class SubtitleVoteEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val subtitleId: String,
    val userId: String, // Anonymous hash of user
    val isUpvote: Boolean, // true for upvote, false for downvote
    val createdAt: Long = System.currentTimeMillis(),
    val reason: String = "", // Optional reason for vote
    val weight: Float = 1.0f // Vote weight based on user reputation
)

@Entity(
    tableName = "subtitle_reports",
    indices = [
        Index(value = ["subtitleId"]),
        Index(value = ["reporterId"]),
        Index(value = ["createdAt"]),
        Index(value = ["status"])
    ]
)
data class SubtitleReportEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val subtitleId: String,
    val reporterId: String,
    val reason: SubtitleReportReason,
    val description: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val status: ReportStatus = ReportStatus.PENDING,
    val resolvedAt: Long? = null,
    val resolvedBy: String? = null,
    val resolution: String = "",
    val severity: ReportSeverity = ReportSeverity.MEDIUM
)

enum class SubtitleReportReason {
    INCORRECT_TIMING,
    WRONG_LANGUAGE,
    SPAM,
    INAPPROPRIATE_CONTENT,
    COPYRIGHT_VIOLATION,
    POOR_QUALITY,
    DUPLICATE,
    MISLEADING,
    OFFENSIVE,
    OTHER
}

enum class ReportStatus {
    PENDING,
    UNDER_REVIEW,
    RESOLVED,
    DISMISSED,
    ESCALATED
}

enum class ReportSeverity {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
}

@Entity(
    tableName = "subtitle_downloads",
    indices = [
        Index(value = ["subtitleId"]),
        Index(value = ["userId"]),
        Index(value = ["downloadedAt"])
    ]
)
data class SubtitleDownloadEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val subtitleId: String,
    val userId: String,
    val downloadedAt: Long = System.currentTimeMillis(),
    val userAgent: String = "",
    val ipAddress: String = "", // Hashed for privacy
    val success: Boolean = true
)

@Entity(
    tableName = "subtitle_quality_reports",
    indices = [
        Index(value = ["subtitleId"]),
        Index(value = ["reportedAt"])
    ]
)
data class SubtitleQualityReportEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val subtitleId: String,
    val reportedAt: Long = System.currentTimeMillis(),
    val timingAccuracy: Float, // 0.0 to 1.0
    val languageAccuracy: Float, // 0.0 to 1.0
    val completeness: Float, // 0.0 to 1.0
    val readability: Float, // 0.0 to 1.0
    val overallScore: Float, // Computed from above metrics
    val detectedIssues: String = "", // JSON array of detected issues
    val suggestedFixes: String = "", // JSON array of suggested improvements
    val automaticScore: Boolean = true // If generated automatically
)