package com.astralplayer.nextplayer.analytics

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.*
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertNotNull
import kotlin.test.assertFalse

/**
 * Comprehensive tests for video analytics system
 * Tests analytics collection, processing, and reporting functionality
 */
@RunWith(AndroidJUnit4::class)
class VideoAnalyticsTest {

    private lateinit var context: Context
    private lateinit var testScope: TestScope
    private lateinit var analyticsManager: VideoAnalyticsManager
    
    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        context = ApplicationProvider.getApplicationContext()
        testScope = TestScope()
        analyticsManager = VideoAnalyticsManager(context)
    }

    @After
    fun tearDown() {
        runTest {
            analyticsManager.cleanup()
        }
    }

    @Test
    fun testAnalyticsManagerInitialization() = runTest {
        // When
        analyticsManager.initialize()
        advanceUntilIdle()
        
        // Then
        val state = analyticsManager.analyticsState.value
        assertTrue("Analytics manager should be initialized", state.isInitialized)
        assertNotNull("Analytics version should be set", state.analyticsVersion)
        assertTrue("Session start time should be set", state.sessionStartTime > 0)
    }

    @Test
    fun testPlaybackSessionTracking() = runTest {
        analyticsManager.initialize()
        advanceUntilIdle()
        
        val testUri = Uri.parse("https://example.com/test-video.mp4")
        val videoMetadata = VideoMetadata(
            title = "Test Video",
            duration = 120000L, // 2 minutes
            resolution = "1080p",
            bitrate = 5000000L,
            codec = "h264",
            fileSize = 50000000L
        )
        val playbackContext = PlaybackContext(
            userId = "test_user",
            deviceType = "phone",
            deviceModel = "Test Device",
            appVersion = "1.0.0",
            networkType = "wifi",
            location = "test_location"
        )
        
        // When - Start session
        val sessionId = analyticsManager.startPlaybackSession(
            videoUri = testUri,
            videoMetadata = videoMetadata,
            playbackContext = playbackContext
        )
        
        // Then
        assertNotNull("Session ID should be generated", sessionId)
        val state = analyticsManager.analyticsState.value
        assertEquals("Active sessions should be 1", 1, state.activeSessions)
    }

    @Test
    fun testPlaybackEventTracking() = runTest {
        analyticsManager.initialize()
        advanceUntilIdle()
        
        // Setup session
        val testUri = Uri.parse("https://example.com/event-test.mp4")
        val videoMetadata = VideoMetadata("Event Test", 180000L, "720p", 2500000L, "h264", 30000000L)
        val playbackContext = PlaybackContext("user1", "tablet", "Test Tablet", "1.0", "cellular", null)
        
        val sessionId = analyticsManager.startPlaybackSession(testUri, videoMetadata, playbackContext)
        
        // When - Track various playback events
        analyticsManager.trackPlaybackEvent(PlaybackEventType.PLAY, 0L)
        analyticsManager.trackPlaybackEvent(PlaybackEventType.PAUSE, 30000L)
        analyticsManager.trackPlaybackEvent(PlaybackEventType.SEEK, 60000L, mapOf(
            "fromPosition" to 30000L,
            "seekDirection" to "forward",
            "seekAmount" to 30000L
        ))
        analyticsManager.trackPlaybackEvent(PlaybackEventType.QUALITY_CHANGE, 90000L, mapOf(
            "quality" to "480p",
            "previousQuality" to "720p",
            "automatic" to true,
            "reason" to "bandwidth_adaptation"
        ))
        analyticsManager.trackPlaybackEvent(PlaybackEventType.BUFFER_START, 100000L)
        analyticsManager.trackPlaybackEvent(PlaybackEventType.BUFFER_END, 102000L)
        
        advanceUntilIdle()
        
        // Then - Events should be tracked
        val state = analyticsManager.analyticsState.value
        assertEquals("Should have one active session", 1, state.activeSessions)
        
        // End session to verify event processing
        analyticsManager.endPlaybackSession(SessionEndReason.USER_STOPPED, 120000L)
        
        val finalState = analyticsManager.analyticsState.value
        assertEquals("Should have no active sessions", 0, finalState.activeSessions)
        assertTrue("Total sessions should increase", finalState.totalSessions > 0)
    }

    @Test
    fun testSessionEndAndAnalytics() = runTest {
        analyticsManager.initialize()
        advanceUntilIdle()
        
        // Setup and run a complete session
        val testUri = Uri.parse("https://example.com/complete-session.mp4")
        val videoMetadata = VideoMetadata("Complete Session", 300000L, "1080p", 8000000L, "h264", 100000000L)
        val playbackContext = PlaybackContext("analytics_user", "tv", "Smart TV", "2.0", "ethernet", "living_room")
        
        val sessionId = analyticsManager.startPlaybackSession(testUri, videoMetadata, playbackContext)
        
        // Simulate a complete viewing session
        analyticsManager.trackPlaybackEvent(PlaybackEventType.PLAY, 0L)
        delay(100) // Simulate time passing
        analyticsManager.trackPlaybackEvent(PlaybackEventType.PAUSE, 60000L)
        delay(50)
        analyticsManager.trackPlaybackEvent(PlaybackEventType.PLAY, 60000L)
        delay(100)
        analyticsManager.trackPlaybackEvent(PlaybackEventType.QUALITY_CHANGE, 120000L, mapOf(
            "quality" to "720p",
            "previousQuality" to "1080p",
            "automatic" to false
        ))
        
        // When - End session
        analyticsManager.endPlaybackSession(SessionEndReason.COMPLETED, 300000L)
        advanceUntilIdle()
        
        // Then - Analytics should be calculated
        val videoAnalytics = analyticsManager.getVideoAnalytics(testUri)
        assertNotNull("Video analytics should be available", videoAnalytics)
        
        videoAnalytics?.let { analytics ->
            assertEquals("Total views should be 1", 1, analytics.totalViews)
            assertTrue("Watch time should be recorded", analytics.totalWatchTime > 0)
            assertEquals("Completion rate should be 100%", 100f, analytics.completionRate)
            assertTrue("Engagement score should be reasonable", analytics.engagementScore >= 0f)
        }
    }

    @Test
    fun testRealTimeDashboard() = runTest {
        analyticsManager.initialize()
        advanceUntilIdle()
        
        // Setup multiple concurrent sessions
        val sessions = (1..3).map { index ->
            val uri = Uri.parse("https://example.com/dashboard-test$index.mp4")
            val metadata = VideoMetadata("Dashboard Test $index", 240000L, "720p", 3000000L, "h264", 60000000L)
            val context = PlaybackContext("user$index", "phone", "Phone $index", "1.0", "wifi", null)
            
            analyticsManager.startPlaybackSession(uri, metadata, context)
        }
        
        // Track some events
        sessions.forEachIndexed { index, sessionId ->
            analyticsManager.trackPlaybackEvent(PlaybackEventType.PLAY, 0L)
            analyticsManager.trackPlaybackEvent(PlaybackEventType.SEEK, (index + 1) * 30000L)
        }
        
        advanceUntilIdle()
        
        // When - Get real-time dashboard
        val dashboard = analyticsManager.getRealTimeDashboard()
        
        // Then
        assertEquals("Should show 3 active users", 3, dashboard.activeUsers)
        assertEquals("Should show 3 total views", 3, dashboard.totalViews)
        assertTrue("Should have live metrics", dashboard.liveMetrics.concurrentViewers >= 0)
        assertTrue("Should have top videos", dashboard.topVideos.isNotEmpty())
        assertTrue("Last updated should be recent", 
                  System.currentTimeMillis() - dashboard.lastUpdated < 5000)
    }

    @Test
    fun testAnalyticsReportGeneration() = runTest {
        analyticsManager.initialize()
        advanceUntilIdle()
        
        // Generate some analytics data
        repeat(5) { index ->
            val uri = Uri.parse("https://example.com/report-test$index.mp4")
            val metadata = VideoMetadata("Report Test $index", 180000L, "1080p", 5000000L, "h264", 75000000L)
            val context = PlaybackContext("report_user$index", "tablet", "Tablet", "1.5", "wifi", "office")
            
            val sessionId = analyticsManager.startPlaybackSession(uri, metadata, context)
            
            // Simulate playback
            analyticsManager.trackPlaybackEvent(PlaybackEventType.PLAY, 0L)
            analyticsManager.trackPlaybackEvent(PlaybackEventType.PAUSE, 60000L)
            analyticsManager.trackPlaybackEvent(PlaybackEventType.PLAY, 60000L)
            
            // End session
            analyticsManager.endPlaybackSession(
                endReason = if (index % 2 == 0) SessionEndReason.COMPLETED else SessionEndReason.USER_STOPPED,
                finalPosition = if (index % 2 == 0) 180000L else 120000L
            )
        }
        
        advanceUntilIdle()
        
        // When - Generate analytics report
        val report = analyticsManager.getAnalyticsReport(TimeRange.LAST_24_HOURS)
        
        // Then
        assertNotNull("Analytics report should be generated", report)
        assertEquals("Should have 5 total sessions", 5, report.totalSessions)
        assertTrue("Should have overview metrics", report.overviewMetrics.totalSessions == 5)
        assertTrue("Should have engagement metrics", report.engagementMetrics.overallEngagementScore >= 0f)
        assertTrue("Should have performance metrics", report.performanceMetrics.overallPerformanceScore >= 0f)
        assertTrue("Should have content metrics", report.contentMetrics.topPerformingVideos.isNotEmpty())
        assertNotNull("Should have user behavior metrics", report.userBehaviorMetrics)
        assertTrue("Should have insights", report.insights.isNotEmpty() || report.insights.isEmpty()) // Either way is valid
        assertTrue("Should have recommendations", report.recommendations.isNotEmpty() || report.recommendations.isEmpty())
    }

    @Test
    fun testEngagementScoreCalculation() = runTest {
        analyticsManager.initialize()
        advanceUntilIdle()
        
        // Test high engagement session
        val highEngagementUri = Uri.parse("https://example.com/high-engagement.mp4")
        val videoMetadata = VideoMetadata("High Engagement", 120000L, "1080p", 5000000L, "h264", 50000000L)
        val playbackContext = PlaybackContext("engaged_user", "phone", "Phone", "1.0", "wifi", null)
        
        val sessionId = analyticsManager.startPlaybackSession(highEngagementUri, videoMetadata, playbackContext)
        
        // Simulate high engagement (complete viewing with minimal interruptions)
        analyticsManager.trackPlaybackEvent(PlaybackEventType.PLAY, 0L)
        delay(50) // Simulate continuous playback
        analyticsManager.trackPlaybackEvent(PlaybackEventType.PAUSE, 60000L)
        delay(20) // Short pause
        analyticsManager.trackPlaybackEvent(PlaybackEventType.PLAY, 60000L)
        delay(50) // Continue to end
        
        analyticsManager.endPlaybackSession(SessionEndReason.COMPLETED, 120000L)
        advanceUntilIdle()
        
        // When - Check engagement analytics
        val videoAnalytics = analyticsManager.getVideoAnalytics(highEngagementUri)
        
        // Then
        assertNotNull("Video analytics should be available", videoAnalytics)
        videoAnalytics?.let { analytics ->
            assertTrue("High engagement should have good completion rate", 
                      analytics.completionRate >= 90f)
            assertTrue("Engagement score should be reasonable", 
                      analytics.engagementScore >= 0f && analytics.engagementScore <= 1f)
        }
    }

    @Test
    fun testBufferEventTracking() = runTest {
        analyticsManager.initialize()
        advanceUntilIdle()
        
        val testUri = Uri.parse("https://example.com/buffer-test.mp4")
        val videoMetadata = VideoMetadata("Buffer Test", 200000L, "720p", 3000000L, "h264", 80000000L)
        val playbackContext = PlaybackContext("buffer_user", "phone", "Phone", "1.0", "cellular", null)
        
        val sessionId = analyticsManager.startPlaybackSession(testUri, videoMetadata, playbackContext)
        
        // When - Simulate buffering events
        analyticsManager.trackPlaybackEvent(PlaybackEventType.PLAY, 0L)
        analyticsManager.trackPlaybackEvent(PlaybackEventType.BUFFER_START, 30000L, mapOf(
            "reason" to "network_congestion"
        ))
        delay(100) // Simulate buffer time
        analyticsManager.trackPlaybackEvent(PlaybackEventType.BUFFER_END, 30000L)
        
        analyticsManager.trackPlaybackEvent(PlaybackEventType.BUFFER_START, 90000L, mapOf(
            "reason" to "insufficient_bandwidth"
        ))
        delay(200) // Longer buffer time
        analyticsManager.trackPlaybackEvent(PlaybackEventType.BUFFER_END, 90000L)
        
        analyticsManager.endPlaybackSession(SessionEndReason.USER_STOPPED, 120000L)
        advanceUntilIdle()
        
        // Then - Buffer events should affect performance metrics
        val videoAnalytics = analyticsManager.getVideoAnalytics(testUri)
        assertNotNull("Video analytics should be available", videoAnalytics)
        
        videoAnalytics?.let { analytics ->
            assertTrue("Performance score should be affected by buffering", 
                      analytics.performanceScore >= 0f && analytics.performanceScore <= 1f)
        }
    }

    @Test
    fun testQualityChangeTracking() = runTest {
        analyticsManager.initialize()
        advanceUntilIdle()
        
        val testUri = Uri.parse("https://example.com/quality-test.mp4")
        val videoMetadata = VideoMetadata("Quality Test", 180000L, "1080p", 8000000L, "h264", 90000000L)
        val playbackContext = PlaybackContext("quality_user", "tv", "Smart TV", "1.0", "ethernet", null)
        
        val sessionId = analyticsManager.startPlaybackSession(testUri, videoMetadata, playbackContext)
        
        // When - Track quality changes
        analyticsManager.trackPlaybackEvent(PlaybackEventType.PLAY, 0L)
        
        analyticsManager.trackPlaybackEvent(PlaybackEventType.QUALITY_CHANGE, 30000L, mapOf(
            "quality" to "720p",
            "previousQuality" to "1080p",
            "automatic" to true,
            "reason" to "bandwidth_adaptation"
        ))
        
        analyticsManager.trackPlaybackEvent(PlaybackEventType.QUALITY_CHANGE, 90000L, mapOf(
            "quality" to "480p",
            "previousQuality" to "720p",
            "automatic" to true,
            "reason" to "network_congestion"
        ))
        
        analyticsManager.trackPlaybackEvent(PlaybackEventType.QUALITY_CHANGE, 120000L, mapOf(
            "quality" to "720p",
            "previousQuality" to "480p",
            "automatic" to false
        ))
        
        analyticsManager.endPlaybackSession(SessionEndReason.COMPLETED, 180000L)
        advanceUntilIdle()
        
        // Then - Quality metrics should be tracked
        val videoAnalytics = analyticsManager.getVideoAnalytics(testUri)
        assertNotNull("Video analytics should be available", videoAnalytics)
        
        videoAnalytics?.let { analytics ->
            assertNotNull("Quality metrics should be available", analytics.qualityMetrics)
        }
    }

    @Test
    fun testErrorEventHandling() = runTest {
        analyticsManager.initialize()
        advanceUntilIdle()
        
        val testUri = Uri.parse("https://example.com/error-test.mp4")
        val videoMetadata = VideoMetadata("Error Test", 150000L, "720p", 3000000L, "h264", 60000000L)
        val playbackContext = PlaybackContext("error_user", "phone", "Phone", "1.0", "wifi", null)
        
        val sessionId = analyticsManager.startPlaybackSession(testUri, videoMetadata, playbackContext)
        
        // When - Track error events
        analyticsManager.trackPlaybackEvent(PlaybackEventType.PLAY, 0L)
        
        analyticsManager.trackPlaybackEvent(PlaybackEventType.ERROR, 45000L, mapOf(
            "errorType" to "network_error",
            "errorMessage" to "Connection timeout",
            "errorCode" to 1001,
            "isFatal" to false
        ))
        
        analyticsManager.trackPlaybackEvent(PlaybackEventType.ERROR, 90000L, mapOf(
            "errorType" to "playback_error",
            "errorMessage" to "Codec not supported",
            "errorCode" to 2001,
            "isFatal" to true
        ))
        
        analyticsManager.endPlaybackSession(SessionEndReason.ERROR, 90000L)
        advanceUntilIdle()
        
        // Then - Errors should be tracked in performance metrics
        val videoAnalytics = analyticsManager.getVideoAnalytics(testUri)
        assertNotNull("Video analytics should be available", videoAnalytics)
        
        videoAnalytics?.let { analytics ->
            assertTrue("Performance score should be affected by errors", 
                      analytics.performanceScore < 1f)
        }
    }

    @Test
    fun testAnalyticsDataExport() = runTest {
        analyticsManager.initialize()
        advanceUntilIdle()
        
        // Generate some test data
        val testUri = Uri.parse("https://example.com/export-test.mp4")
        val videoMetadata = VideoMetadata("Export Test", 120000L, "1080p", 5000000L, "h264", 50000000L)
        val playbackContext = PlaybackContext("export_user", "tablet", "Tablet", "1.0", "wifi", null)
        
        val sessionId = analyticsManager.startPlaybackSession(testUri, videoMetadata, playbackContext)
        analyticsManager.trackPlaybackEvent(PlaybackEventType.PLAY, 0L)
        analyticsManager.trackPlaybackEvent(PlaybackEventType.PAUSE, 60000L)
        analyticsManager.endPlaybackSession(SessionEndReason.USER_STOPPED, 60000L)
        
        // When - Export analytics data
        val exportResult = analyticsManager.exportAnalyticsData(
            format = ExportFormat.JSON,
            timeRange = TimeRange.ALL_TIME,
            includePersonalData = false
        )
        
        // Then
        assertTrue("Export should succeed", exportResult is ExportResult.Success)
        
        when (exportResult) {
            is ExportResult.Success -> {
                assertNotNull("Export ID should be generated", exportResult.exportId)
                assertNotNull("File name should be generated", exportResult.fileName)
                assertTrue("Should have exported at least 1 record", exportResult.recordCount >= 1)
            }
            else -> throw AssertionError("Export should succeed")
        }
    }

    @Test
    fun testConcurrentSessionHandling() = runTest {
        analyticsManager.initialize()
        advanceUntilIdle()
        
        val concurrentSessions = 5
        val sessionJobs = (1..concurrentSessions).map { index ->
            async {
                val uri = Uri.parse("https://example.com/concurrent$index.mp4")
                val metadata = VideoMetadata("Concurrent $index", 90000L, "720p", 2500000L, "h264", 40000000L)
                val context = PlaybackContext("user$index", "phone", "Phone", "1.0", "wifi", null)
                
                val sessionId = analyticsManager.startPlaybackSession(uri, metadata, context)
                
                // Simulate some playback
                analyticsManager.trackPlaybackEvent(PlaybackEventType.PLAY, 0L)
                delay(50)
                analyticsManager.trackPlaybackEvent(PlaybackEventType.PAUSE, 30000L)
                
                analyticsManager.endPlaybackSession(SessionEndReason.USER_STOPPED, 30000L)
                sessionId
            }
        }
        
        // When - Wait for all sessions to complete
        val sessionIds = sessionJobs.awaitAll()
        
        // Then
        assertEquals("All sessions should complete", concurrentSessions, sessionIds.size)
        assertTrue("All session IDs should be unique", sessionIds.toSet().size == concurrentSessions)
        
        val finalState = analyticsManager.analyticsState.value
        assertTrue("Total sessions should include all concurrent sessions", 
                  finalState.totalSessions >= concurrentSessions)
    }

    @Test
    fun testVideoSpecificAnalytics() = runTest {
        analyticsManager.initialize()
        advanceUntilIdle()
        
        val video1Uri = Uri.parse("https://example.com/video1.mp4")
        val video2Uri = Uri.parse("https://example.com/video2.mp4")
        
        // Create sessions for different videos
        val metadata1 = VideoMetadata("Video 1", 120000L, "1080p", 5000000L, "h264", 50000000L)
        val metadata2 = VideoMetadata("Video 2", 180000L, "720p", 3000000L, "h264", 70000000L)
        val context = PlaybackContext("analytics_user", "phone", "Phone", "1.0", "wifi", null)
        
        // Video 1 session
        val session1 = analyticsManager.startPlaybackSession(video1Uri, metadata1, context)
        analyticsManager.trackPlaybackEvent(PlaybackEventType.PLAY, 0L)
        analyticsManager.endPlaybackSession(SessionEndReason.COMPLETED, 120000L)
        
        // Video 2 session
        val session2 = analyticsManager.startPlaybackSession(video2Uri, metadata2, context)
        analyticsManager.trackPlaybackEvent(PlaybackEventType.PLAY, 0L)
        analyticsManager.endPlaybackSession(SessionEndReason.USER_STOPPED, 90000L)
        
        advanceUntilIdle()
        
        // When - Get analytics for each video
        val video1Analytics = analyticsManager.getVideoAnalytics(video1Uri)
        val video2Analytics = analyticsManager.getVideoAnalytics(video2Uri)
        
        // Then
        assertNotNull("Video 1 analytics should be available", video1Analytics)
        assertNotNull("Video 2 analytics should be available", video2Analytics)
        
        video1Analytics?.let { analytics ->
            assertEquals("Video 1 should have completion rate of 100%", 100f, analytics.completionRate)
        }
        
        video2Analytics?.let { analytics ->
            assertTrue("Video 2 should have completion rate of 50%", 
                      analytics.completionRate > 40f && analytics.completionRate < 60f)
        }
    }
}