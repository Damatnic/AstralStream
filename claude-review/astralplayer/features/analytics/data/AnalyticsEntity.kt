package com.astralplayer.features.analytics.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index
import java.util.UUID

@Entity(
    tableName = "playback_analytics",
    indices = [
        Index(value = ["videoId"]),
        Index(value = ["timestamp"]),
        Index(value = ["sessionId"]),
        Index(value = ["eventType"])
    ]
)
data class PlaybackAnalyticsEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val sessionId: String,
    val videoId: String,
    val videoTitle: String,
    val videoDuration: Long, // milliseconds
    val eventType: PlaybackEventType,
    val timestamp: Long = System.currentTimeMillis(),
    val playbackPosition: Long = 0L, // milliseconds
    val playbackSpeed: Float = 1.0f,
    val volume: Float = 1.0f,
    val brightness: Float = 0.5f,
    val isFullscreen: Boolean = false,
    val isPipMode: Boolean = false,
    val subtitleEnabled: Boolean = false,
    val subtitleLanguage: String? = null,
    val audioTrack: String? = null,
    val videoQuality: String? = null,
    val bufferingDuration: Long = 0L, // milliseconds
    val errorMessage: String? = null,
    val networkType: String? = null,
    val batteryLevel: Float? = null,
    val deviceOrientation: String? = null
)

enum class PlaybackEventType {
    SESSION_START,
    SESSION_END,
    PLAY,
    PAUSE,
    SEEK,
    BUFFER_START,
    BUFFER_END,
    QUALITY_CHANGE,
    SUBTITLE_CHANGE,
    AUDIO_TRACK_CHANGE,
    SPEED_CHANGE,
    VOLUME_CHANGE,
    BRIGHTNESS_CHANGE,
    FULLSCREEN_ENTER,
    FULLSCREEN_EXIT,
    PIP_ENTER,
    PIP_EXIT,
    ERROR,
    CHAPTER_SKIP,
    GESTURE_USED,
    SCREENSHOT_TAKEN
}

@Entity(
    tableName = "viewing_sessions",
    indices = [
        Index(value = ["startTime"]),
        Index(value = ["endTime"]),
        Index(value = ["videoId"])
    ]
)
data class ViewingSessionEntity(
    @PrimaryKey
    val sessionId: String = UUID.randomUUID().toString(),
    val videoId: String,
    val videoTitle: String,
    val startTime: Long = System.currentTimeMillis(),
    val endTime: Long? = null,
    val totalWatchTime: Long = 0L, // milliseconds
    val completionPercentage: Float = 0f,
    val averageSpeed: Float = 1.0f,
    val seekCount: Int = 0,
    val pauseCount: Int = 0,
    val bufferingCount: Int = 0,
    val totalBufferingTime: Long = 0L,
    val qualityChanges: Int = 0,
    val subtitleUsageTime: Long = 0L,
    val gesturesUsed: Int = 0,
    val errorCount: Int = 0,
    val source: String? = null, // e.g., "file", "url", "stream"
    val referrer: String? = null
)

@Entity(
    tableName = "daily_statistics",
    indices = [
        Index(value = ["date"], unique = true)
    ]
)
data class DailyStatisticsEntity(
    @PrimaryKey
    val date: String, // YYYY-MM-DD format
    val totalWatchTime: Long = 0L, // milliseconds
    val videosWatched: Int = 0,
    val uniqueVideos: Int = 0,
    val averageSessionDuration: Long = 0L,
    val totalSessions: Int = 0,
    val completedVideos: Int = 0,
    val mostWatchedCategory: String? = null,
    val peakWatchingHour: Int? = null, // 0-23
    val subtitleUsagePercentage: Float = 0f,
    val averagePlaybackSpeed: Float = 1.0f,
    val totalSeeks: Int = 0,
    val totalPauses: Int = 0,
    val totalErrors: Int = 0,
    val totalGesturesUsed: Int = 0
)

@Entity(
    tableName = "video_statistics",
    indices = [
        Index(value = ["videoId"], unique = true),
        Index(value = ["lastWatched"]),
        Index(value = ["watchCount"])
    ]
)
data class VideoStatisticsEntity(
    @PrimaryKey
    val videoId: String,
    val videoTitle: String,
    val videoDuration: Long,
    val totalWatchTime: Long = 0L,
    val watchCount: Int = 0,
    val completionCount: Int = 0,
    val averageWatchPercentage: Float = 0f,
    val lastWatched: Long = System.currentTimeMillis(),
    val firstWatched: Long = System.currentTimeMillis(),
    val averagePlaybackSpeed: Float = 1.0f,
    val seekHeatmap: String = "[]", // JSON array of seek positions
    val pauseHeatmap: String = "[]", // JSON array of pause positions
    val skipCount: Int = 0,
    val favoriteScenes: String = "[]", // JSON array of timestamp ranges
    val rating: Float? = null,
    val tags: String = "" // Comma-separated tags
)

@Entity(
    tableName = "feature_usage",
    indices = [
        Index(value = ["featureName"]),
        Index(value = ["lastUsed"])
    ]
)
data class FeatureUsageEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val featureName: String,
    val featureCategory: FeatureCategory,
    val usageCount: Int = 0,
    val totalUsageTime: Long = 0L, // milliseconds
    val lastUsed: Long = System.currentTimeMillis(),
    val firstUsed: Long = System.currentTimeMillis(),
    val averageUsageTime: Long = 0L,
    val successRate: Float = 1.0f,
    val metadata: String = "{}" // JSON for additional data
)

enum class FeatureCategory {
    PLAYBACK_CONTROL,
    SUBTITLE,
    AUDIO,
    VIDEO_QUALITY,
    GESTURE,
    UI_NAVIGATION,
    SHARING,
    PLAYLIST,
    SETTINGS,
    ADVANCED
}

@Entity(
    tableName = "performance_metrics",
    indices = [
        Index(value = ["timestamp"]),
        Index(value = ["metricType"])
    ]
)
data class PerformanceMetricEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val metricType: PerformanceMetricType,
    val value: Float,
    val unit: String,
    val videoId: String? = null,
    val sessionId: String? = null,
    val additionalInfo: String? = null
)

enum class PerformanceMetricType {
    APP_STARTUP_TIME,
    VIDEO_LOAD_TIME,
    SEEK_RESPONSE_TIME,
    SUBTITLE_GENERATION_TIME,
    MEMORY_USAGE,
    CPU_USAGE,
    BATTERY_DRAIN,
    NETWORK_BANDWIDTH,
    FRAME_DROP_RATE,
    RENDER_TIME
}

@Entity(
    tableName = "user_preferences_analytics",
    indices = [
        Index(value = ["preferenceKey"]),
        Index(value = ["lastUpdated"])
    ]
)
data class UserPreferenceAnalyticsEntity(
    @PrimaryKey
    val preferenceKey: String,
    val preferenceValue: String,
    val changeCount: Int = 0,
    val firstSet: Long = System.currentTimeMillis(),
    val lastUpdated: Long = System.currentTimeMillis(),
    val mostCommonValue: String? = null,
    val valueHistory: String = "[]" // JSON array of historical values
)

@Entity(
    tableName = "content_analytics",
    indices = [
        Index(value = ["contentType"]),
        Index(value = ["lastAccessed"])
    ]
)
data class ContentAnalyticsEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val contentType: String, // e.g., "movie", "series", "documentary"
    val genre: String? = null,
    val language: String? = null,
    val watchTime: Long = 0L,
    val watchCount: Int = 0,
    val completionRate: Float = 0f,
    val averageRating: Float? = null,
    val lastAccessed: Long = System.currentTimeMillis(),
    val metadata: String = "{}" // JSON for additional content metadata
)