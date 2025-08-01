package com.astralplayer.features.analytics.repository

import android.content.Context
import android.util.Log
import com.astralplayer.features.analytics.dao.AnalyticsDao
import com.astralplayer.features.analytics.data.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AnalyticsRepository @Inject constructor(
    private val analyticsDao: AnalyticsDao,
    @ApplicationContext private val context: Context
) {
    
    companion object {
        private const val TAG = "AnalyticsRepository"
        private const val DATE_FORMAT = "yyyy-MM-dd"
    }
    
    private val dateFormatter = SimpleDateFormat(DATE_FORMAT, Locale.getDefault())
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    private var currentSession: ViewingSessionEntity? = null
    private val sessionEvents = mutableListOf<PlaybackAnalyticsEntity>()
    
    // Session Management
    fun startSession(videoId: String, videoTitle: String, source: String? = null) {
        scope.launch {
            try {
                val sessionId = UUID.randomUUID().toString()
                currentSession = ViewingSessionEntity(
                    sessionId = sessionId,
                    videoId = videoId,
                    videoTitle = videoTitle,
                    source = source
                )
                analyticsDao.insertSession(currentSession!!)
                
                logEvent(
                    PlaybackAnalyticsEntity(
                        sessionId = sessionId,
                        videoId = videoId,
                        videoTitle = videoTitle,
                        videoDuration = 0L, // Will be updated when known
                        eventType = PlaybackEventType.SESSION_START
                    )
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start session", e)
            }
        }
    }
    
    fun endSession(
        totalWatchTime: Long,
        videoDuration: Long,
        completionPercentage: Float
    ) {
        scope.launch {
            try {
                currentSession?.let { session ->
                    val endTime = System.currentTimeMillis()
                    val updatedSession = session.copy(
                        endTime = endTime,
                        totalWatchTime = totalWatchTime,
                        completionPercentage = completionPercentage,
                        seekCount = sessionEvents.count { it.eventType == PlaybackEventType.SEEK },
                        pauseCount = sessionEvents.count { it.eventType == PlaybackEventType.PAUSE },
                        bufferingCount = sessionEvents.count { it.eventType == PlaybackEventType.BUFFER_START },
                        gesturesUsed = sessionEvents.count { it.eventType == PlaybackEventType.GESTURE_USED },
                        errorCount = sessionEvents.count { it.eventType == PlaybackEventType.ERROR }
                    )
                    
                    analyticsDao.updateSession(updatedSession)
                    
                    logEvent(
                        PlaybackAnalyticsEntity(
                            sessionId = session.sessionId,
                            videoId = session.videoId,
                            videoTitle = session.videoTitle,
                            videoDuration = videoDuration,
                            eventType = PlaybackEventType.SESSION_END,
                            playbackPosition = totalWatchTime
                        )
                    )
                    
                    // Update video statistics
                    analyticsDao.incrementVideoStats(
                        videoId = session.videoId,
                        videoTitle = session.videoTitle,
                        videoDuration = videoDuration,
                        watchTime = totalWatchTime,
                        completed = completionPercentage >= 90f,
                        playbackSpeed = session.averageSpeed
                    )
                    
                    // Update daily statistics
                    updateDailyStats(session)
                    
                    currentSession = null
                    sessionEvents.clear()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to end session", e)
            }
        }
    }
    
    // Event Logging
    fun logEvent(event: PlaybackAnalyticsEntity) {
        scope.launch {
            try {
                val finalEvent = if (currentSession != null) {
                    event.copy(sessionId = currentSession!!.sessionId)
                } else event
                
                analyticsDao.insertPlaybackEvent(finalEvent)
                sessionEvents.add(finalEvent)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to log event", e)
            }
        }
    }
    
    fun logPlaybackEvent(
        eventType: PlaybackEventType,
        videoId: String,
        videoTitle: String,
        videoDuration: Long,
        playbackPosition: Long,
        additionalData: Map<String, Any> = emptyMap()
    ) {
        val event = PlaybackAnalyticsEntity(
            sessionId = currentSession?.sessionId ?: "",
            videoId = videoId,
            videoTitle = videoTitle,
            videoDuration = videoDuration,
            eventType = eventType,
            playbackPosition = playbackPosition,
            playbackSpeed = additionalData["speed"] as? Float ?: 1.0f,
            volume = additionalData["volume"] as? Float ?: 1.0f,
            isFullscreen = additionalData["fullscreen"] as? Boolean ?: false,
            subtitleEnabled = additionalData["subtitleEnabled"] as? Boolean ?: false,
            subtitleLanguage = additionalData["subtitleLanguage"] as? String,
            videoQuality = additionalData["quality"] as? String
        )
        
        logEvent(event)
    }
    
    // Feature Usage Tracking
    fun trackFeatureUsage(
        featureName: String,
        category: FeatureCategory,
        usageTime: Long = 0L
    ) {
        scope.launch {
            try {
                analyticsDao.incrementFeatureUsage(featureName)
                // Additional logic for tracking usage time could be added here
            } catch (e: Exception) {
                Log.e(TAG, "Failed to track feature usage", e)
            }
        }
    }
    
    // Performance Tracking
    fun trackPerformanceMetric(
        type: PerformanceMetricType,
        value: Float,
        unit: String,
        videoId: String? = null
    ) {
        scope.launch {
            try {
                val metric = PerformanceMetricEntity(
                    metricType = type,
                    value = value,
                    unit = unit,
                    videoId = videoId,
                    sessionId = currentSession?.sessionId
                )
                analyticsDao.insertPerformanceMetric(metric)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to track performance metric", e)
            }
        }
    }
    
    // Data Retrieval
    fun getRecentSessions(limit: Int = 50): Flow<List<ViewingSessionEntity>> {
        return analyticsDao.getRecentSessions(limit)
    }
    
    fun getDailyStats(days: Int = 30): Flow<List<DailyStatisticsEntity>> {
        return analyticsDao.getRecentDailyStats(days)
    }
    
    fun getMostWatchedVideos(limit: Int = 20): Flow<List<VideoStatisticsEntity>> {
        return analyticsDao.getMostWatchedVideos(limit)
    }
    
    fun getRecentlyWatchedVideos(limit: Int = 20): Flow<List<VideoStatisticsEntity>> {
        return analyticsDao.getRecentlyWatchedVideos(limit)
    }
    
    fun getMostUsedFeatures(): Flow<List<FeatureUsageEntity>> {
        return analyticsDao.getMostUsedFeatures()
    }
    
    // Analytics Calculations
    suspend fun getWatchTimeStats(days: Int = 7): WatchTimeStats {
        val since = System.currentTimeMillis() - (days * 24 * 60 * 60 * 1000L)
        val totalTime = analyticsDao.getTotalWatchTime(since) ?: 0L
        val avgSession = analyticsDao.getAverageSessionDuration(since) ?: 0L
        val sessions = analyticsDao.getSessionsInTimeRange(
            since,
            System.currentTimeMillis()
        )
        
        return WatchTimeStats(
            totalWatchTime = totalTime,
            averageSessionDuration = avgSession,
            totalSessions = sessions.size,
            completedVideos = sessions.count { it.completionPercentage >= 90f }
        )
    }
    
    suspend fun getPlaybackBehavior(days: Int = 30): PlaybackBehavior {
        val since = System.currentTimeMillis() - (days * 24 * 60 * 60 * 1000L)
        val events = analyticsDao.getEventsInTimeRange(since, System.currentTimeMillis())
        
        val seekCount = events.count { it.eventType == PlaybackEventType.SEEK }
        val pauseCount = events.count { it.eventType == PlaybackEventType.PAUSE }
        val speedChanges = events.count { it.eventType == PlaybackEventType.SPEED_CHANGE }
        
        val avgSpeed = events
            .filter { it.eventType == PlaybackEventType.SPEED_CHANGE }
            .map { it.playbackSpeed }
            .average()
            .takeIf { !it.isNaN() } ?: 1.0
        
        return PlaybackBehavior(
            averageSeeksPerSession = if (events.isNotEmpty()) seekCount.toFloat() / events.size else 0f,
            averagePausesPerSession = if (events.isNotEmpty()) pauseCount.toFloat() / events.size else 0f,
            averagePlaybackSpeed = avgSpeed.toFloat(),
            speedChangeFrequency = speedChanges
        )
    }
    
    suspend fun getContentPreferences(): ContentPreferences {
        val contentTypes = analyticsDao.getContentTypeDistribution()
        val genres = analyticsDao.getTopGenres(10)
        
        return ContentPreferences(
            preferredContentTypes = contentTypes,
            preferredGenres = genres
        )
    }
    
    // Private helpers
    private suspend fun updateDailyStats(session: ViewingSessionEntity) {
        try {
            val today = dateFormatter.format(Date())
            val existingStats = analyticsDao.getDailyStats(today)
            
            val updatedStats = if (existingStats != null) {
                existingStats.copy(
                    totalWatchTime = existingStats.totalWatchTime + session.totalWatchTime,
                    videosWatched = existingStats.videosWatched + 1,
                    totalSessions = existingStats.totalSessions + 1,
                    completedVideos = existingStats.completedVideos + 
                        if (session.completionPercentage >= 90f) 1 else 0,
                    totalSeeks = existingStats.totalSeeks + session.seekCount,
                    totalPauses = existingStats.totalPauses + session.pauseCount,
                    totalErrors = existingStats.totalErrors + session.errorCount,
                    totalGesturesUsed = existingStats.totalGesturesUsed + session.gesturesUsed,
                    averageSessionDuration = ((existingStats.averageSessionDuration * existingStats.totalSessions) + 
                        session.totalWatchTime) / (existingStats.totalSessions + 1)
                )
            } else {
                DailyStatisticsEntity(
                    date = today,
                    totalWatchTime = session.totalWatchTime,
                    videosWatched = 1,
                    uniqueVideos = 1,
                    averageSessionDuration = session.totalWatchTime,
                    totalSessions = 1,
                    completedVideos = if (session.completionPercentage >= 90f) 1 else 0,
                    totalSeeks = session.seekCount,
                    totalPauses = session.pauseCount,
                    totalErrors = session.errorCount,
                    totalGesturesUsed = session.gesturesUsed
                )
            }
            
            analyticsDao.insertDailyStats(updatedStats)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update daily stats", e)
        }
    }
    
    // Cleanup
    suspend fun cleanupOldData(daysToKeep: Int = 90) {
        val cutoffTime = System.currentTimeMillis() - (daysToKeep * 24 * 60 * 60 * 1000L)
        analyticsDao.deleteOldPlaybackEvents(cutoffTime)
        analyticsDao.deleteOldSessions(cutoffTime)
        analyticsDao.deleteOldMetrics(cutoffTime)
    }
}

// Data classes for analytics results
data class WatchTimeStats(
    val totalWatchTime: Long,
    val averageSessionDuration: Long,
    val totalSessions: Int,
    val completedVideos: Int
)

data class PlaybackBehavior(
    val averageSeeksPerSession: Float,
    val averagePausesPerSession: Float,
    val averagePlaybackSpeed: Float,
    val speedChangeFrequency: Int
)

data class ContentPreferences(
    val preferredContentTypes: List<ContentTypeStats>,
    val preferredGenres: List<GenreStats>
)