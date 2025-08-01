package com.astralplayer.features.analytics.dao

import androidx.room.*
import com.astralplayer.features.analytics.data.*
import kotlinx.coroutines.flow.Flow
import java.util.*

@Dao
interface AnalyticsDao {
    
    // Playback Analytics
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaybackEvent(event: PlaybackAnalyticsEntity)
    
    @Query("SELECT * FROM playback_analytics WHERE sessionId = :sessionId ORDER BY timestamp")
    suspend fun getSessionEvents(sessionId: String): List<PlaybackAnalyticsEntity>
    
    @Query("SELECT * FROM playback_analytics WHERE videoId = :videoId ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getVideoEvents(videoId: String, limit: Int = 100): List<PlaybackAnalyticsEntity>
    
    @Query("""
        SELECT * FROM playback_analytics 
        WHERE timestamp BETWEEN :startTime AND :endTime 
        ORDER BY timestamp DESC
    """)
    suspend fun getEventsInTimeRange(startTime: Long, endTime: Long): List<PlaybackAnalyticsEntity>
    
    @Query("""
        SELECT eventType, COUNT(*) as count FROM playback_analytics 
        WHERE timestamp > :since 
        GROUP BY eventType 
        ORDER BY count DESC
    """)
    suspend fun getEventTypeDistribution(since: Long): List<EventTypeCount>
    
    // Viewing Sessions
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: ViewingSessionEntity)
    
    @Update
    suspend fun updateSession(session: ViewingSessionEntity)
    
    @Query("SELECT * FROM viewing_sessions WHERE sessionId = :sessionId")
    suspend fun getSession(sessionId: String): ViewingSessionEntity?
    
    @Query("SELECT * FROM viewing_sessions ORDER BY startTime DESC LIMIT :limit")
    fun getRecentSessions(limit: Int = 50): Flow<List<ViewingSessionEntity>>
    
    @Query("""
        SELECT * FROM viewing_sessions 
        WHERE startTime BETWEEN :startTime AND :endTime 
        ORDER BY startTime DESC
    """)
    suspend fun getSessionsInTimeRange(startTime: Long, endTime: Long): List<ViewingSessionEntity>
    
    @Query("SELECT SUM(totalWatchTime) FROM viewing_sessions WHERE startTime > :since")
    suspend fun getTotalWatchTime(since: Long): Long?
    
    @Query("SELECT AVG(totalWatchTime) FROM viewing_sessions WHERE startTime > :since")
    suspend fun getAverageSessionDuration(since: Long): Long?
    
    // Daily Statistics
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDailyStats(stats: DailyStatisticsEntity)
    
    @Query("SELECT * FROM daily_statistics WHERE date = :date")
    suspend fun getDailyStats(date: String): DailyStatisticsEntity?
    
    @Query("SELECT * FROM daily_statistics ORDER BY date DESC LIMIT :days")
    fun getRecentDailyStats(days: Int = 30): Flow<List<DailyStatisticsEntity>>
    
    @Query("""
        SELECT * FROM daily_statistics 
        WHERE date BETWEEN :startDate AND :endDate 
        ORDER BY date DESC
    """)
    suspend fun getStatsInDateRange(startDate: String, endDate: String): List<DailyStatisticsEntity>
    
    @Query("SELECT SUM(totalWatchTime) FROM daily_statistics WHERE date BETWEEN :startDate AND :endDate")
    suspend fun getTotalWatchTimeInRange(startDate: String, endDate: String): Long?
    
    // Video Statistics
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertVideoStats(stats: VideoStatisticsEntity)
    
    @Update
    suspend fun updateVideoStats(stats: VideoStatisticsEntity)
    
    @Query("SELECT * FROM video_statistics WHERE videoId = :videoId")
    suspend fun getVideoStats(videoId: String): VideoStatisticsEntity?
    
    @Query("SELECT * FROM video_statistics ORDER BY watchCount DESC LIMIT :limit")
    fun getMostWatchedVideos(limit: Int = 20): Flow<List<VideoStatisticsEntity>>
    
    @Query("SELECT * FROM video_statistics ORDER BY lastWatched DESC LIMIT :limit")
    fun getRecentlyWatchedVideos(limit: Int = 20): Flow<List<VideoStatisticsEntity>>
    
    @Query("""
        SELECT * FROM video_statistics 
        WHERE totalWatchTime > 0 
        ORDER BY (CAST(completionCount AS FLOAT) / watchCount) DESC 
        LIMIT :limit
    """)
    suspend fun getMostCompletedVideos(limit: Int = 20): List<VideoStatisticsEntity>
    
    @Transaction
    suspend fun incrementVideoStats(
        videoId: String,
        videoTitle: String,
        videoDuration: Long,
        watchTime: Long,
        completed: Boolean,
        playbackSpeed: Float
    ) {
        val existing = getVideoStats(videoId)
        if (existing == null) {
            insertVideoStats(
                VideoStatisticsEntity(
                    videoId = videoId,
                    videoTitle = videoTitle,
                    videoDuration = videoDuration,
                    totalWatchTime = watchTime,
                    watchCount = 1,
                    completionCount = if (completed) 1 else 0,
                    averageWatchPercentage = (watchTime.toFloat() / videoDuration * 100),
                    averagePlaybackSpeed = playbackSpeed
                )
            )
        } else {
            val newWatchCount = existing.watchCount + 1
            val newTotalWatchTime = existing.totalWatchTime + watchTime
            val newCompletionCount = existing.completionCount + if (completed) 1 else 0
            val newAvgPercentage = ((existing.averageWatchPercentage * existing.watchCount) + 
                (watchTime.toFloat() / videoDuration * 100)) / newWatchCount
            val newAvgSpeed = ((existing.averagePlaybackSpeed * existing.watchCount) + playbackSpeed) / newWatchCount
            
            updateVideoStats(
                existing.copy(
                    totalWatchTime = newTotalWatchTime,
                    watchCount = newWatchCount,
                    completionCount = newCompletionCount,
                    averageWatchPercentage = newAvgPercentage,
                    lastWatched = System.currentTimeMillis(),
                    averagePlaybackSpeed = newAvgSpeed
                )
            )
        }
    }
    
    // Feature Usage
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFeatureUsage(usage: FeatureUsageEntity)
    
    @Query("SELECT * FROM feature_usage ORDER BY usageCount DESC")
    fun getMostUsedFeatures(): Flow<List<FeatureUsageEntity>>
    
    @Query("SELECT * FROM feature_usage WHERE featureCategory = :category ORDER BY usageCount DESC")
    suspend fun getFeaturesByCategory(category: FeatureCategory): List<FeatureUsageEntity>
    
    @Query("UPDATE feature_usage SET usageCount = usageCount + 1, lastUsed = :timestamp WHERE featureName = :featureName")
    suspend fun incrementFeatureUsage(featureName: String, timestamp: Long = System.currentTimeMillis())
    
    // Performance Metrics
    @Insert
    suspend fun insertPerformanceMetric(metric: PerformanceMetricEntity)
    
    @Query("""
        SELECT AVG(value) FROM performance_metrics 
        WHERE metricType = :type AND timestamp > :since
    """)
    suspend fun getAverageMetric(type: PerformanceMetricType, since: Long): Float?
    
    @Query("""
        SELECT * FROM performance_metrics 
        WHERE metricType = :type 
        ORDER BY timestamp DESC 
        LIMIT :limit
    """)
    suspend fun getRecentMetrics(type: PerformanceMetricType, limit: Int = 100): List<PerformanceMetricEntity>
    
    // User Preferences Analytics
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPreferenceAnalytics(pref: UserPreferenceAnalyticsEntity)
    
    @Update
    suspend fun updatePreferenceAnalytics(pref: UserPreferenceAnalyticsEntity)
    
    @Query("SELECT * FROM user_preferences_analytics WHERE changeCount > :minChanges ORDER BY changeCount DESC")
    suspend fun getMostChangedPreferences(minChanges: Int = 5): List<UserPreferenceAnalyticsEntity>
    
    // Content Analytics
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContentAnalytics(content: ContentAnalyticsEntity)
    
    @Query("""
        SELECT contentType, SUM(watchTime) as totalTime, COUNT(*) as count 
        FROM content_analytics 
        GROUP BY contentType 
        ORDER BY totalTime DESC
    """)
    suspend fun getContentTypeDistribution(): List<ContentTypeStats>
    
    @Query("""
        SELECT genre, SUM(watchTime) as totalTime, COUNT(*) as count 
        FROM content_analytics 
        WHERE genre IS NOT NULL 
        GROUP BY genre 
        ORDER BY totalTime DESC 
        LIMIT :limit
    """)
    suspend fun getTopGenres(limit: Int = 10): List<GenreStats>
    
    // Cleanup
    @Query("DELETE FROM playback_analytics WHERE timestamp < :before")
    suspend fun deleteOldPlaybackEvents(before: Long)
    
    @Query("DELETE FROM viewing_sessions WHERE startTime < :before")
    suspend fun deleteOldSessions(before: Long)
    
    @Query("DELETE FROM performance_metrics WHERE timestamp < :before")
    suspend fun deleteOldMetrics(before: Long)
    
    // Complex analytics queries
    @Query("""
        SELECT 
            strftime('%H', datetime(timestamp/1000, 'unixepoch')) as hour,
            COUNT(*) as count
        FROM playback_analytics
        WHERE eventType = 'PLAY' AND timestamp > :since
        GROUP BY hour
        ORDER BY hour
    """)
    suspend fun getHourlyPlaybackDistribution(since: Long): List<HourlyDistribution>
    
    @Query("""
        SELECT 
            strftime('%w', datetime(startTime/1000, 'unixepoch')) as dayOfWeek,
            COUNT(*) as sessionCount,
            SUM(totalWatchTime) as totalTime
        FROM viewing_sessions
        WHERE startTime > :since
        GROUP BY dayOfWeek
        ORDER BY dayOfWeek
    """)
    suspend fun getWeeklyWatchPattern(since: Long): List<WeeklyPattern>
}

// Data classes for complex queries
data class EventTypeCount(
    val eventType: PlaybackEventType,
    val count: Int
)

data class ContentTypeStats(
    val contentType: String,
    val totalTime: Long,
    val count: Int
)

data class GenreStats(
    val genre: String,
    val totalTime: Long,
    val count: Int
)

data class HourlyDistribution(
    val hour: String,
    val count: Int
)

data class WeeklyPattern(
    val dayOfWeek: String,
    val sessionCount: Int,
    val totalTime: Long
)