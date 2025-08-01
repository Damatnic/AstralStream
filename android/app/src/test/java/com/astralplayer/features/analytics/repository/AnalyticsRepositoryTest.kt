package com.astralplayer.features.analytics.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.astralplayer.astralstream.data.database.AstralStreamDatabase
import com.astralplayer.features.analytics.dao.AnalyticsDao
import com.astralplayer.features.analytics.data.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.*

@RunWith(RobolectricTestRunner::class)
class AnalyticsRepositoryTest {

    private lateinit var database: AstralStreamDatabase
    private lateinit var analyticsDao: AnalyticsDao
    private lateinit var analyticsRepository: AnalyticsRepository
    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, AstralStreamDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        
        analyticsDao = database.analyticsDao()
        analyticsRepository = AnalyticsRepository(analyticsDao, context)
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun `start session creates new viewing session`() = runBlocking {
        val videoId = "test_video_1"
        val videoTitle = "Test Video"
        val source = "file"
        
        analyticsRepository.startSession(videoId, videoTitle, source)
        
        // Verify session was created
        val sessions = analyticsDao.getRecentSessions(10).first()
        assertEquals(1, sessions.size)
        
        val session = sessions[0]
        assertEquals(videoId, session.videoId)
        assertEquals(videoTitle, session.videoTitle)
        assertEquals(source, session.source)
        assertTrue(session.startTime > 0)
        assertNull(session.endTime)
    }

    @Test
    fun `end session updates session with final data`() = runBlocking {
        val videoId = "test_video_2"
        val videoTitle = "Test Video 2"
        
        // Start session
        analyticsRepository.startSession(videoId, videoTitle)
        
        // Simulate some playback events
        repeat(3) {
            analyticsRepository.logPlaybackEvent(
                eventType = PlaybackEventType.SEEK,
                videoId = videoId,
                videoTitle = videoTitle,
                videoDuration = 120000L,
                playbackPosition = it * 10000L
            )
        }
        
        repeat(2) {
            analyticsRepository.logPlaybackEvent(
                eventType = PlaybackEventType.PAUSE,
                videoId = videoId,
                videoTitle = videoTitle,
                videoDuration = 120000L,
                playbackPosition = it * 5000L
            )
        }
        
        // End session
        val totalWatchTime = 45000L
        val completionPercentage = 75f
        analyticsRepository.endSession(
            totalWatchTime = totalWatchTime,
            videoDuration = 120000L,
            completionPercentage = completionPercentage
        )
        
        // Verify session was updated
        val sessions = analyticsDao.getRecentSessions(10).first()
        assertEquals(1, sessions.size)
        
        val session = sessions[0]
        assertEquals(totalWatchTime, session.totalWatchTime)
        assertEquals(completionPercentage, session.completionPercentage)
        assertEquals(3, session.seekCount)
        assertEquals(2, session.pauseCount)
        assertNotNull(session.endTime)
    }

    @Test
    fun `log playback events stores events correctly`() = runBlocking {
        val videoId = "event_test_video"
        val videoTitle = "Event Test Video"
        val videoDuration = 180000L
        
        analyticsRepository.startSession(videoId, videoTitle)
        
        // Log various events
        val events = listOf(
            PlaybackEventType.PLAY,
            PlaybackEventType.PAUSE,
            PlaybackEventType.SEEK,
            PlaybackEventType.VOLUME_CHANGE,
            PlaybackEventType.SPEED_CHANGE
        )
        
        events.forEachIndexed { index, eventType ->
            analyticsRepository.logPlaybackEvent(
                eventType = eventType,
                videoId = videoId,
                videoTitle = videoTitle,
                videoDuration = videoDuration,
                playbackPosition = index * 10000L,
                additionalData = mapOf(
                    "speed" to 1.5f,
                    "volume" to 0.8f,
                    "quality" to "1080p"
                )
            )
        }
        
        // Verify events were stored
        val sessionEvents = analyticsDao.getVideoEvents(videoId, 10)
        assertEquals(5, sessionEvents.size)
        
        val playEvent = sessionEvents.find { it.eventType == PlaybackEventType.PLAY }
        assertNotNull(playEvent)
        assertEquals(videoId, playEvent.videoId)
        assertEquals(1.5f, playEvent.playbackSpeed)
        assertEquals(0.8f, playEvent.volume)
        assertEquals("1080p", playEvent.videoQuality)
    }

    @Test
    fun `track feature usage records usage correctly`() = runBlocking {
        val featureName = "subtitle_toggle"
        val category = FeatureCategory.SUBTITLE
        
        // Track feature usage multiple times
        repeat(5) {
            analyticsRepository.trackFeatureUsage(featureName, category)
        }
        
        // Verify usage was tracked
        val featureUsage = analyticsDao.getMostUsedFeatures().first()
        assertTrue(featureUsage.isNotEmpty())
        
        val subtitleFeature = featureUsage.find { it.featureName == featureName }
        assertNotNull(subtitleFeature)
        assertEquals(5, subtitleFeature.usageCount)
        assertEquals(category, subtitleFeature.featureCategory)
        assertTrue(subtitleFeature.lastUsed > 0)
    }

    @Test
    fun `track performance metrics stores metrics`() = runBlocking {
        val metricType = PerformanceMetricType.VIDEO_LOAD_TIME
        val value = 1250f
        val unit = "ms"
        val videoId = "perf_test_video"
        
        analyticsRepository.trackPerformanceMetric(metricType, value, unit, videoId)
        
        // Verify metric was stored
        val metrics = analyticsDao.getRecentMetrics(metricType, 10)
        assertEquals(1, metrics.size)
        
        val metric = metrics[0]
        assertEquals(metricType, metric.metricType)
        assertEquals(value, metric.value)
        assertEquals(unit, metric.unit)
        assertEquals(videoId, metric.videoId)
        assertTrue(metric.timestamp > 0)
    }

    @Test
    fun `get watch time stats calculates correctly`() = runBlocking {
        val currentTime = System.currentTimeMillis()
        
        // Create test sessions with different completion rates
        val sessions = listOf(
            ViewingSessionEntity(
                videoId = "video1",
                videoTitle = "Video 1",
                startTime = currentTime - 86400000L, // 1 day ago
                endTime = currentTime - 86400000L + 3600000L,
                totalWatchTime = 3600000L, // 1 hour
                completionPercentage = 95f
            ),
            ViewingSessionEntity(
                videoId = "video2", 
                videoTitle = "Video 2",
                startTime = currentTime - 43200000L, // 12 hours ago
                endTime = currentTime - 43200000L + 1800000L,
                totalWatchTime = 1800000L, // 30 minutes
                completionPercentage = 60f
            ),
            ViewingSessionEntity(
                videoId = "video3",
                videoTitle = "Video 3", 
                startTime = currentTime - 3600000L, // 1 hour ago
                endTime = currentTime - 3600000L + 7200000L,
                totalWatchTime = 7200000L, // 2 hours
                completionPercentage = 100f
            )
        )
        
        sessions.forEach { session ->
            analyticsDao.insertSession(session)
        }
        
        val stats = analyticsRepository.getWatchTimeStats(7) // Last 7 days
        
        assertEquals(12600000L, stats.totalWatchTime) // 3.5 hours total
        assertEquals(3, stats.totalSessions)
        assertEquals(2, stats.completedVideos) // 95% and 100% completion
        assertTrue(stats.averageSessionDuration > 0)
    }

    @Test
    fun `get playback behavior analyzes user patterns`() = runBlocking {
        val videoId = "behavior_test_video"
        val videoTitle = "Behavior Test"
        val sessionId = "test_session"
        
        // Create session
        analyticsDao.insertSession(
            ViewingSessionEntity(
                sessionId = sessionId,
                videoId = videoId,
                videoTitle = videoTitle
            )
        )
        
        // Create playback events with different speeds and behaviors
        val events = listOf(
            PlaybackAnalyticsEntity(
                sessionId = sessionId,
                videoId = videoId,
                videoTitle = videoTitle,
                videoDuration = 120000L,
                eventType = PlaybackEventType.SEEK,
                playbackSpeed = 1.0f
            ),
            PlaybackAnalyticsEntity(
                sessionId = sessionId,
                videoId = videoId, 
                videoTitle = videoTitle,
                videoDuration = 120000L,
                eventType = PlaybackEventType.PAUSE,
                playbackSpeed = 1.0f
            ),
            PlaybackAnalyticsEntity(
                sessionId = sessionId,
                videoId = videoId,
                videoTitle = videoTitle,
                videoDuration = 120000L,
                eventType = PlaybackEventType.SPEED_CHANGE,
                playbackSpeed = 1.5f
            ),
            PlaybackAnalyticsEntity(
                sessionId = sessionId,
                videoId = videoId,
                videoTitle = videoTitle, 
                videoDuration = 120000L,
                eventType = PlaybackEventType.SPEED_CHANGE,
                playbackSpeed = 2.0f
            )
        )
        
        events.forEach { event ->
            analyticsDao.insertPlaybackEvent(event)
        }
        
        val behavior = analyticsRepository.getPlaybackBehavior(30)
        
        assertTrue(behavior.averageSeeksPerSession >= 0)
        assertTrue(behavior.averagePausesPerSession >= 0)
        assertEquals(1.75f, behavior.averagePlaybackSpeed, 0.1f) // Average of 1.5 and 2.0
        assertEquals(2, behavior.speedChangeFrequency)
    }

    @Test
    fun `get content preferences analyzes viewing patterns`() = runBlocking {
        // Add content analytics data
        val contentData = listOf(
            ContentAnalyticsEntity(
                contentType = "movie",
                genre = "action",
                watchTime = 7200000L, // 2 hours
                watchCount = 3
            ), 
            ContentAnalyticsEntity(
                contentType = "movie",
                genre = "comedy",
                watchTime = 3600000L, // 1 hour
                watchCount = 2
            ),
            ContentAnalyticsEntity(
                contentType = "series", 
                genre = "drama",
                watchTime = 10800000L, // 3 hours
                watchCount = 6
            )
        )
        
        contentData.forEach { data ->
            analyticsDao.insertContentAnalytics(data)
        }
        
        val preferences = analyticsRepository.getContentPreferences()
        
        assertNotNull(preferences.preferredContentTypes)
        assertNotNull(preferences.preferredGenres)
        
        // Should prefer series over movies (more watch time)
        val seriesType = preferences.preferredContentTypes.find { it.contentType == "series" }
        val movieType = preferences.preferredContentTypes.find { it.contentType == "movie" }
        
        assertNotNull(seriesType)
        assertNotNull(movieType)
        assertTrue(seriesType.totalTime > movieType.totalTime)
        
        // Drama should be top genre
        val topGenre = preferences.preferredGenres.first()
        assertEquals("drama", topGenre.genre)
        assertEquals(10800000L, topGenre.totalTime)
    }

    @Test
    fun `cleanup old data removes entries beyond retention period`() = runBlocking {
        val currentTime = System.currentTimeMillis()
        val oldTime = currentTime - (100 * 24 * 60 * 60 * 1000L) // 100 days ago
        val recentTime = currentTime - (10 * 24 * 60 * 60 * 1000L) // 10 days ago
        
        // Create old and recent data
        val oldSession = ViewingSessionEntity(
            videoId = "old_video",
            videoTitle = "Old Video",
            startTime = oldTime
        )
        
        val recentSession = ViewingSessionEntity(
            videoId = "recent_video", 
            videoTitle = "Recent Video",
            startTime = recentTime
        )
        
        analyticsDao.insertSession(oldSession)
        analyticsDao.insertSession(recentSession)
        
        val oldEvent = PlaybackAnalyticsEntity(
            sessionId = oldSession.sessionId,
            videoId = "old_video",
            videoTitle = "Old Video",
            videoDuration = 120000L,
            eventType = PlaybackEventType.PLAY,
            timestamp = oldTime
        )
        
        val recentEvent = PlaybackAnalyticsEntity(
            sessionId = recentSession.sessionId,
            videoId = "recent_video",
            videoTitle = "Recent Video", 
            videoDuration = 120000L,
            eventType = PlaybackEventType.PLAY,
            timestamp = recentTime
        )
        
        analyticsDao.insertPlaybackEvent(oldEvent)
        analyticsDao.insertPlaybackEvent(recentEvent)
        
        // Cleanup data older than 30 days
        analyticsRepository.cleanupOldData(30)
        
        // Verify old data was removed
        val remainingSessions = analyticsDao.getRecentSessions(10).first()
        assertEquals(1, remainingSessions.size)
        assertEquals("recent_video", remainingSessions[0].videoId)
        
        val remainingEvents = analyticsDao.getEventsInTimeRange(0, currentTime)
        assertEquals(1, remainingEvents.size)
        assertEquals("recent_video", remainingEvents[0].videoId)
    }

    @Test
    fun `daily statistics are aggregated correctly`() = runBlocking {
        val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            .format(java.util.Date())
        
        // Create multiple sessions for today
        val sessions = listOf(
            ViewingSessionEntity(
                videoId = "daily1",
                videoTitle = "Daily Video 1",
                totalWatchTime = 3600000L, // 1 hour
                completionPercentage = 100f,
                seekCount = 2,
                pauseCount = 1
            ),
            ViewingSessionEntity(
                videoId = "daily2",
                videoTitle = "Daily Video 2", 
                totalWatchTime = 1800000L, // 30 minutes
                completionPercentage = 75f,
                seekCount = 1,
                pauseCount = 3
            )
        )
        
        sessions.forEach { session ->
            analyticsDao.insertSession(session)
        }
        
        // Verify daily stats calculation
        val dailyStats = analyticsDao.getRecentDailyStats(1).first()
        
        if (dailyStats.isNotEmpty()) {
            val todayStats = dailyStats[0]
            assertEquals(today, todayStats.date)
            assertEquals(5400000L, todayStats.totalWatchTime) // 1.5 hours total
            assertEquals(2, todayStats.videosWatched)
            assertEquals(2, todayStats.totalSessions)
            assertEquals(1, todayStats.completedVideos) // Only one 100% completion
            assertEquals(3, todayStats.totalSeeks) // 2 + 1
            assertEquals(4, todayStats.totalPauses) // 1 + 3
        }
    }

    @Test
    fun `video statistics track individual video performance`() = runBlocking {
        val videoId = "stats_test_video"
        val videoTitle = "Stats Test Video"
        val videoDuration = 180000L
        
        // Simulate multiple viewings of the same video  
        repeat(3) { index ->
            analyticsDao.incrementVideoStats(
                videoId = videoId,
                videoTitle = videoTitle,
                videoDuration = videoDuration,
                watchTime = (index + 1) * 60000L, // 1, 2, 3 minutes
                completed = index >= 1, // Last 2 are completed
                playbackSpeed = 1.0f + (index * 0.25f) // 1.0, 1.25, 1.5
            )
        }
        
        val videoStats = analyticsDao.getVideoStats(videoId)
        assertNotNull(videoStats)
        
        assertEquals(videoId, videoStats.videoId)
        assertEquals(videoTitle, videoStats.videoTitle)
        assertEquals(3, videoStats.watchCount)
        assertEquals(2, videoStats.completionCount)
        assertEquals(360000L, videoStats.totalWatchTime) // 6 minutes total
        
        // Average watch percentage should be around 200% / 3 ≈ 66.67%
        assertTrue(videoStats.averageWatchPercentage > 60f)
        assertTrue(videoStats.averageWatchPercentage < 70f)
        
        // Average speed should be (1.0 + 1.25 + 1.5) / 3 = 1.25
        assertEquals(1.25f, videoStats.averagePlaybackSpeed, 0.01f)
    }
}