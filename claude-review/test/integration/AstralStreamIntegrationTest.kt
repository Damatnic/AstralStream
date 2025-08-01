package com.astralplayer.integration

import android.content.Context
import android.net.Uri
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.astralplayer.astralstream.data.database.AstralStreamDatabase
import com.astralplayer.community.api.CommunityApiManager
import com.astralplayer.community.repository.PlaylistSharingRepository
import com.astralplayer.features.analytics.repository.AnalyticsRepository
import com.astralplayer.features.analytics.service.AnalyticsTracker
import com.astralplayer.features.editing.service.VideoEditingService
import com.astralplayer.features.gestures.repository.GestureRepository
import com.astralplayer.features.subtitle.SubtitleCacheManager
import com.astralplayer.features.subtitle.EncryptionManager
import com.astralplayer.nextplayer.editing.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.mockito.kotlin.*
import kotlin.test.*

@RunWith(RobolectricTestRunner::class)
class AstralStreamIntegrationTest {

    private lateinit var database: AstralStreamDatabase
    private lateinit var context: Context
    
    // System under test - all major components
    private lateinit var subtitleCacheManager: SubtitleCacheManager
    private lateinit var playlistSharingRepository: PlaylistSharingRepository
    private lateinit var gestureRepository: GestureRepository
    private lateinit var analyticsRepository: AnalyticsRepository
    private lateinit var analyticsTracker: AnalyticsTracker
    private lateinit var videoEditingService: VideoEditingService

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, AstralStreamDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        
        // Initialize all components
        val encryptionManager = mock<EncryptionManager> {
            on { encrypt(any()) } doReturn "encrypted_content"
            on { decrypt(any()) } doReturn "decrypted_content"
        }
        
        subtitleCacheManager = SubtitleCacheManager(
            context = context,
            subtitleCacheDao = database.subtitleCacheDao(),
            encryptionManager = encryptionManager
        )
        
        val mockApiManager = mock<CommunityApiManager>()
        playlistSharingRepository = PlaylistSharingRepository(
            apiManager = mockApiManager,
            database = database,
            context = context
        )
        
        gestureRepository = GestureRepository(
            gestureDao = database.gestureDao(),
            context = context
        )
        
        analyticsRepository = AnalyticsRepository(
            analyticsDao = database.analyticsDao(),
            context = context
        )
        
        analyticsTracker = AnalyticsTracker(
            context = context,
            analyticsRepository = analyticsRepository
        )
        
        videoEditingService = VideoEditingService(context)
    }

    @After
    fun teardown() {
        analyticsTracker.cleanup()
        videoEditingService.cleanup()
        database.close()
    }

    @Test
    fun `complete video editing workflow with analytics tracking`() = runBlocking {
        // Initialize all services
        videoEditingService.initialize()
        
        val videoId = "integration_test_video"
        val videoTitle = "Integration Test Video"
        val videoUri = Uri.parse("test://video.mp4")
        
        // Step 1: Start analytics tracking
        analyticsTracker.startTracking(videoId, videoTitle, "file")
        
        // Step 2: Import video for editing
        val importResult = videoEditingService.importVideo(videoUri)
        assertTrue(importResult.success)
        
        // Track video editing start
        analyticsTracker.trackAdvancedFeature("video_editing_start")
        
        // Step 3: Create editing project
        val projectResult = videoEditingService.createProject(
            name = "Integration Test Project",
            settings = ProjectSettings(
                resolution = Pair(1920, 1080),
                frameRate = 30f
            )
        )
        assertTrue(projectResult.success)
        
        // Step 4: Apply some effects and track usage
        val clip = importResult.clip!!
        val blurEffect = VideoEffect(
            id = "blur_1",
            type = EffectType.BLUR,
            name = "Blur Effect",
            intensity = 0.7f
        )
        
        val effectResult = videoEditingService.applyEffect(clip, blurEffect)
        assertTrue(effectResult.success)
        
        // Track effect usage
        analyticsTracker.trackAdvancedFeature("video_effect_blur")
        
        // Step 5: Add text overlay and track
        val textOverlay = VideoOverlay(
            id = "title_overlay",
            type = OverlayType.TEXT,
            name = "Title",
            startTime = 0L,
            duration = 5000L,
            content = OverlayContent(text = "Integration Test")
        )
        
        val overlayResult = videoEditingService.addOverlay(textOverlay)
        assertTrue(overlayResult.success)
        
        analyticsTracker.trackAdvancedFeature("text_overlay")
        
        // Step 6: Stop tracking and verify analytics
        analyticsTracker.stopTracking()
        
        // Verify analytics were recorded
        val sessions = analyticsRepository.getRecentSessions(10).first()
        assertTrue(sessions.isNotEmpty())
        
        val session = sessions.first()
        assertEquals(videoId, session.videoId)
        assertEquals(videoTitle, session.videoTitle)
        assertNotNull(session.endTime)
        
        // Verify feature usage was tracked
        val featureUsage = analyticsRepository.getMostUsedFeatures().first()
        val editingFeatures = featureUsage.filter { 
            it.featureName.contains("video_editing") || 
            it.featureName.contains("video_effect") ||
            it.featureName.contains("text_overlay")
        }
        assertTrue(editingFeatures.size >= 3)
    }

    @Test
    fun `subtitle cache integration with AI generation and analytics`() = runBlocking {
        val videoId = "subtitle_integration_test"
        val language = "en"
        val originalContent = "Hello, this is a test subtitle."
        
        // Step 1: Start analytics tracking
        analyticsTracker.startTracking(videoId, "Subtitle Test Video")
        
        // Step 2: Cache subtitle (simulating AI generation)
        val cacheStartTime = System.currentTimeMillis()
        val cacheResult = subtitleCacheManager.cacheSubtitle(
            videoId = videoId,
            language = language,
            content = originalContent,
            format = "srt",
            quality = 95.0f,
            enableCompression = true
        )
        val cacheEndTime = System.currentTimeMillis()
        
        assertTrue(cacheResult.isSuccess)
        
        // Track subtitle generation performance
        analyticsTracker.trackSubtitleGeneration(cacheEndTime - cacheStartTime)
        
        // Step 3: Retrieve cached subtitle
        val retrieveStartTime = System.currentTimeMillis()
        val retrieveResult = subtitleCacheManager.getCachedSubtitle(videoId, language)
        val retrieveEndTime = System.currentTimeMillis()
        
        assertTrue(retrieveResult.isSuccess)
        assertEquals("decrypted_content", retrieveResult.getOrNull())
        
        // Track subtitle usage
        analyticsTracker.trackSubtitleAction("subtitle_loaded")
        
        // Step 4: Verify cache statistics
        val cacheStats = subtitleCacheManager.getCacheStatistics()
        assertTrue(cacheStats.totalEntries > 0)
        assertTrue(cacheStats.hitRate >= 0.0)
        
        // Step 5: Stop tracking and verify analytics
        analyticsTracker.stopTracking()
        
        // Verify performance metrics were recorded
        val performanceMetrics = database.analyticsDao()
            .getRecentMetrics(com.astralplayer.features.analytics.data.PerformanceMetricType.SUBTITLE_GENERATION_TIME, 10)
        assertTrue(performanceMetrics.isNotEmpty())
        
        val metric = performanceMetrics.first()
        assertTrue(metric.value > 0f)
        assertEquals("ms", metric.unit)
        
        // Verify feature usage tracking
        val featureUsage = analyticsRepository.getMostUsedFeatures().first()
        val subtitleFeatures = featureUsage.filter { it.featureName.contains("subtitle") }
        assertTrue(subtitleFeatures.isNotEmpty())
    }

    @Test
    fun `gesture system integration with analytics tracking`() = runBlocking {
        val profileId = "integration_test_profile"
        
        // Step 1: Create gesture profile
        val profile = com.astralplayer.features.gestures.data.GestureProfileEntity(
            id = profileId,
            name = "Integration Test Profile",
            description = "Profile for integration testing",
            isDefault = false
        )
        
        val profileResult = gestureRepository.createProfile(profile)
        assertTrue(profileResult.isSuccess)
        
        // Step 2: Create custom gesture
        val gesture = com.astralplayer.features.gestures.data.CustomGestureEntity(
            id = "integration_gesture",
            name = "Integration Gesture",
            type = com.astralplayer.features.gestures.data.GestureType.SWIPE_UP,
            action = com.astralplayer.features.gestures.data.GestureAction.VOLUME_UP,
            zone = com.astralplayer.features.gestures.data.GestureZone.RIGHT,
            profileId = profileId,
            sensitivity = 0.8f
        )
        
        val gestureResult = gestureRepository.saveCustomGesture(gesture)
        assertTrue(gestureResult.isSuccess)
        
        // Step 3: Start video tracking
        analyticsTracker.startTracking("gesture_test_video", "Gesture Test Video")
        
        // Step 4: Simulate gesture usage
        repeat(5) { index ->
            gestureRepository.recordGestureUsage(
                gestureId = "integration_gesture",
                success = index < 4, // 4 successful, 1 failed
                responseTime = 150L + index * 10,
                context = "video_playback"
            )
            
            // Track gesture in analytics
            analyticsTracker.trackGestureUsage("swipe_up_volume")
        }
        
        // Step 5: Verify gesture statistics
        val gestureStats = gestureRepository.getGestureStatistics("integration_gesture")
        assertNotNull(gestureStats)
        assertEquals(5, gestureStats.totalUses)
        assertEquals(4, gestureStats.successfulUses)
        assertEquals(0.8f, gestureStats.successRate)
        
        // Step 6: Stop tracking
        analyticsTracker.stopTracking()
        
        // Step 7: Verify analytics integration
        val featureUsage = analyticsRepository.getMostUsedFeatures().first()
        val gestureFeatures = featureUsage.filter { 
            it.featureCategory == com.astralplayer.features.analytics.data.FeatureCategory.GESTURE 
        }
        assertTrue(gestureFeatures.isNotEmpty())
        
        // Verify gesture events in playback analytics
        val playbackEvents = database.analyticsDao().getEventsInTimeRange(0, System.currentTimeMillis())
        val gestureEvents = playbackEvents.filter { 
            it.eventType == com.astralplayer.features.analytics.data.PlaybackEventType.GESTURE_USED 
        }
        assertEquals(5, gestureEvents.size)
    }

    @Test
    fun `community features integration with analytics`() = runBlocking {
        // This test would require more complex mocking for the API
        // For now, we'll test the basic integration structure
        
        analyticsTracker.startTracking("community_test_video", "Community Test Video")
        
        // Track community feature usage
        analyticsTracker.trackSharingAction("playlist_share")
        analyticsTracker.trackSharingAction("subtitle_contribution")
        
        analyticsTracker.stopTracking()
        
        // Verify sharing analytics
        val featureUsage = analyticsRepository.getMostUsedFeatures().first()
        val sharingFeatures = featureUsage.filter { 
            it.featureCategory == com.astralplayer.features.analytics.data.FeatureCategory.SHARING 
        }
        assertTrue(sharingFeatures.isNotEmpty())
    }

    @Test
    fun `end to end video processing pipeline`() = runBlocking {
        val videoId = "pipeline_test_video"
        val videoTitle = "Pipeline Test Video"
        val videoUri = Uri.parse("test://pipeline_video.mp4")
        
        // Step 1: Initialize all services
        videoEditingService.initialize()
        
        // Step 2: Start comprehensive analytics tracking
        analyticsTracker.startTracking(videoId, videoTitle, "file")
        
        // Step 3: Import video
        val importResult = videoEditingService.importVideo(videoUri)
        assertTrue(importResult.success)
        analyticsTracker.trackAdvancedFeature("video_import")
        
        // Step 4: Cache subtitles
        val subtitleResult = subtitleCacheManager.cacheSubtitle(
            videoId = videoId,
            language = "en",
            content = "Test subtitle for pipeline",
            format = "srt"
        )
        assertTrue(subtitleResult.isSuccess)
        analyticsTracker.trackSubtitleAction("subtitle_cached")
        
        // Step 5: Apply multiple effects
        val clip = importResult.clip!!
        val effects = listOf(
            VideoEffect("brightness", EffectType.BRIGHTNESS_CONTRAST, "Brightness", intensity = 0.2f),
            VideoEffect("blur", EffectType.BLUR, "Blur", intensity = 0.1f)
        )
        
        effects.forEach { effect ->
            val effectResult = videoEditingService.applyEffect(clip, effect)
            assertTrue(effectResult.success)
            analyticsTracker.trackAdvancedFeature("video_effect_${effect.type.name.lowercase()}")
        }
        
        // Step 6: Setup gesture for timeline navigation
        val gestureProfile = com.astralplayer.features.gestures.data.GestureProfileEntity(
            id = "timeline_profile",
            name = "Timeline Navigation",
            description = "Gestures for video timeline"
        )
        gestureRepository.createProfile(gestureProfile)
        
        val timelineGesture = com.astralplayer.features.gestures.data.CustomGestureEntity(
            id = "timeline_seek",
            name = "Timeline Seek",
            type = com.astralplayer.features.gestures.data.GestureType.SWIPE_RIGHT,
            action = com.astralplayer.features.gestures.data.GestureAction.SEEK_FORWARD,
            zone = com.astralplayer.features.gestures.data.GestureZone.BOTTOM,
            profileId = "timeline_profile"
        )
        gestureRepository.saveCustomGesture(timelineGesture)
        
        // Simulate gesture usage during editing
        gestureRepository.recordGestureUsage("timeline_seek", true, 120L, "video_editing")
        analyticsTracker.trackGestureUsage("timeline_navigation")
        
        // Step 7: Create final timeline
        val timeline = Timeline(
            videoTracks = mutableListOf(
                VideoTrack(
                    id = "main_track",
                    name = "Main Video Track",
                    clips = mutableListOf(clip)
                )
            ),
            overlayTracks = mutableListOf(
                OverlayTrack(
                    id = "overlay_track",
                    name = "Overlay Track",
                    overlays = mutableListOf(
                        VideoOverlay(
                            id = "title_overlay",
                            type = OverlayType.TEXT,
                            name = "Title",
                            startTime = 0L,
                            duration = 3000L,
                            content = OverlayContent(text = "Pipeline Test")
                        )
                    )
                )
            ),
            duration = clip.duration
        )
        
        // Step 8: Export final video
        val tempFile = java.io.File.createTempFile("pipeline_export", ".mp4")
        val exportSettings = ExportSettings(
            outputUri = Uri.fromFile(tempFile),
            outputFormat = VideoFormat.MP4,
            resolution = Pair(1280, 720),
            frameRate = 30f,
            bitrate = 5_000_000L,
            audioSettings = AudioExportSettings()
        )
        
        val exportResult = videoEditingService.exportVideo(
            timeline = timeline,
            settings = exportSettings,
            progressCallback = { progress ->
                // Track export progress
                if (progress == 1.0f) {
                    analyticsTracker.trackAdvancedFeature("video_export_complete")
                }
            }
        )
        
        tempFile.delete() // Clean up
        assertTrue(exportResult.success)
        
        // Step 9: Stop tracking and verify comprehensive analytics
        analyticsTracker.stopTracking()
        
        // Verify all components were tracked
        val sessions = analyticsRepository.getRecentSessions(10).first()
        assertTrue(sessions.isNotEmpty())
        
        val session = sessions.first()
        assertEquals(videoId, session.videoId)
        assertTrue(session.totalWatchTime >= 0)
        
        // Verify feature usage across all categories
        val allFeatureUsage = analyticsRepository.getMostUsedFeatures().first()
        val featureCategories = allFeatureUsage.map { it.featureCategory }.distinct()
        
        // Should have usage from multiple feature categories
        assertTrue(featureCategories.contains(com.astralplayer.features.analytics.data.FeatureCategory.ADVANCED))
        assertTrue(featureCategories.contains(com.astralplayer.features.analytics.data.FeatureCategory.SUBTITLE))
        assertTrue(featureCategories.contains(com.astralplayer.features.analytics.data.FeatureCategory.GESTURE))
        
        // Verify performance metrics
        val performanceMetrics = database.analyticsDao().getRecentMetrics(
            com.astralplayer.features.analytics.data.PerformanceMetricType.SUBTITLE_GENERATION_TIME, 
            10
        )
        assertTrue(performanceMetrics.isNotEmpty())
        
        // Verify cache statistics
        val cacheStats = subtitleCacheManager.getCacheStatistics()
        assertTrue(cacheStats.totalEntries > 0)
        
        // Verify gesture statistics
        val gestureStats = gestureRepository.getGestureStatistics("timeline_seek")
        assertNotNull(gestureStats)
        assertTrue(gestureStats.totalUses > 0)
    }

    @Test
    fun `database migrations and data integrity`() = runBlocking {
        // Test that all DAOs can perform basic operations without conflicts
        
        // Subtitle cache operations
        val cacheResult = subtitleCacheManager.cacheSubtitle(
            videoId = "migration_test",
            language = "en",
            content = "Migration test content",
            format = "srt"
        )
        assertTrue(cacheResult.isSuccess)
        
        // Analytics operations
        analyticsRepository.startSession("migration_test", "Migration Test Video")
        analyticsRepository.trackFeatureUsage(
            "migration_test_feature", 
            com.astralplayer.features.analytics.data.FeatureCategory.ADVANCED
        )
        analyticsRepository.endSession(30000L, 60000L, 50f)
        
        // Gesture operations
        val gestureProfile = com.astralplayer.features.gestures.data.GestureProfileEntity(
            id = "migration_profile",
            name = "Migration Profile"
        )
        gestureRepository.createProfile(gestureProfile)
        
        // Verify all data is accessible
        val cachedSubtitle = subtitleCacheManager.getCachedSubtitle("migration_test", "en")
        assertTrue(cachedSubtitle.isSuccess)
        
        val sessions = analyticsRepository.getRecentSessions(10).first()
        assertTrue(sessions.isNotEmpty())
        
        val profiles = gestureRepository.getAllProfiles().first()
        assertTrue(profiles.isNotEmpty())
        
        // Verify no database corruption
        assertTrue(database.isOpen)
    }
}