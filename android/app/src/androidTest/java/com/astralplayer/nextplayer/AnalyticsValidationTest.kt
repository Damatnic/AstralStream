package com.astralplayer.nextplayer

import android.content.Context
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import com.astralplayer.nextplayer.analytics.FeatureAnalytics
import com.astralplayer.nextplayer.feature.flags.FeatureFlags
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Analytics Data Accuracy and Completeness Validation Tests
 * Verifies that analytics events are correctly tracked and contain accurate data
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class AnalyticsValidationTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    private lateinit var context: Context
    private lateinit var device: UiDevice
    private lateinit var analytics: FeatureAnalytics

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        analytics = FeatureAnalytics(context)
        
        // Enable all features for analytics testing
        FeatureFlags.initialize(context)
        runBlocking {
            FeatureFlags.getManager()?.enableAllFeatures()
        }
        
        // Enable analytics
        analytics.enableAnalytics(true)
    }

    @Test
    fun testSearchAnalyticsAccuracy() {
        composeTestRule.apply {
            // Clear any existing analytics
            analytics.clearLocalAnalytics()
            
            // Perform search with specific parameters
            onNodeWithContentDescription("Search").performClick()
            waitForIdle()
            
            val searchQuery = "analytics test query"
            val startTime = System.currentTimeMillis()
            
            onNodeWithText("Search videos...").performTextInput(searchQuery)
            waitForIdle()
            
            // Apply filters
            onNodeWithText("Filters").performClick()
            waitForIdle()
            
            onNodeWithText("Duration").performClick()
            onNodeWithText("More than 1 hour").performClick()
            
            onNodeWithText("Quality").performClick()
            onNodeWithText("HD (720p+)").performClick()
            
            onNodeWithText("Apply Filters").performClick()
            waitForIdle()
            
            val endTime = System.currentTimeMillis()
            val searchDuration = endTime - startTime
            
            Thread.sleep(1000) // Allow analytics to process
            
            // Verify search analytics were captured
            val analyticsData = analytics.getLocalAnalytics()
            val searchEvents = analyticsData["feature_search_used"] ?: emptyList()
            
            assert(searchEvents.isNotEmpty()) {
                "Search analytics events should be captured"
            }
            
            val latestSearchEvent = searchEvents.last()
            val searchParams = latestSearchEvent.parameters
            
            // Verify search query is captured (truncated if needed)
            val capturedQuery = searchParams["search_query"] as? String
            assert(capturedQuery?.contains("analytics test") == true) {
                "Search query should be captured in analytics"
            }
            
            // Verify filter usage is tracked
            val filterUsed = searchParams["filter_used"] as? Boolean
            assert(filterUsed == true) {
                "Filter usage should be tracked as true"
            }
            
            // Verify search duration is reasonable
            val capturedDuration = searchParams["search_duration_ms"] as? Long
            assert(capturedDuration != null && capturedDuration > 0) {
                "Search duration should be captured and positive"
            }
            
            device.pressBack()
            device.pressBack()
        }
    }

    @Test
    fun testVideoPlaybackAnalytics() {
        composeTestRule.apply {
            analytics.clearLocalAnalytics()
            
            // Start video playback
            val playbackStartTime = System.currentTimeMillis()
            
            startVideoPlaybackIfAvailable()
            
            // Perform playback actions
            onNodeWithContentDescription("Play/Pause").performClick()
            waitForIdle()
            Thread.sleep(2000) // Play for 2 seconds
            
            onNodeWithContentDescription("Play/Pause").performClick()
            waitForIdle() // Pause
            
            // Seek in video
            onNodeWithContentDescription("Seek bar").performTouchInput {
                swipeRight(startX = centerX * 0.2f, endX = centerX * 0.6f)
            }
            waitForIdle()
            
            val playbackEndTime = System.currentTimeMillis()
            
            Thread.sleep(1000) // Allow analytics to process
            
            // Verify playback analytics
            val analyticsData = analytics.getLocalAnalytics()
            
            // Check for engagement metrics
            val engagementEvents = analyticsData["engagement_session"] ?: emptyList()
            if (engagementEvents.isNotEmpty()) {
                val engagementEvent = engagementEvents.last()
                val engagementParams = engagementEvent.parameters
                
                val videosWatched = engagementParams["videos_watched"] as? Int
                assert(videosWatched != null && videosWatched >= 0) {
                    "Videos watched count should be tracked"
                }
                
                val interactionCount = engagementParams["interaction_count"] as? Int
                assert(interactionCount != null && interactionCount > 0) {
                    "User interactions should be counted"
                }
            }
            
            device.pressBack()
        }
    }

    @Test
    fun testEqualizerAnalyticsAccuracy() {
        composeTestRule.apply {
            analytics.clearLocalAnalytics()
            
            startVideoPlaybackIfAvailable()
            
            // Use equalizer
            onNodeWithContentDescription("Audio settings").performClick()
            waitForIdle()
            
            onNodeWithText("Equalizer").performClick()
            waitForIdle()
            
            // Select a preset
            val selectedPreset = "Rock"
            onNodeWithText(selectedPreset).performClick()
            waitForIdle()
            
            // Enable bass boost
            onNodeWithText("Bass Boost").performClick()
            waitForIdle()
            
            Thread.sleep(1000) // Allow analytics to process
            
            // Verify equalizer analytics
            val analyticsData = analytics.getLocalAnalytics()
            val equalizerEvents = analyticsData["equalizer_settings_changed"] ?: emptyList()
            
            assert(equalizerEvents.isNotEmpty()) {
                "Equalizer usage should be tracked"
            }
            
            val equalizerEvent = equalizerEvents.last()
            val equalizerParams = equalizerEvent.parameters
            
            // Verify preset name is captured
            val capturedPreset = equalizerParams["equalizer_preset"] as? String
            assert(capturedPreset == selectedPreset) {
                "Selected equalizer preset should be tracked correctly"
            }
            
            // Verify bass boost level is tracked
            val bassBoostLevel = equalizerParams["bass_boost_level"] as? Int
            assert(bassBoostLevel != null && bassBoostLevel >= 0) {
                "Bass boost level should be tracked"
            }
            
            device.pressBack()
            device.pressBack()
            device.pressBack()
        }
    }

    @Test
    fun testVoiceControlAnalyticsAccuracy() {
        composeTestRule.apply {
            analytics.clearLocalAnalytics()
            
            startVideoPlaybackIfAvailable()
            
            // Start voice control session
            val sessionStartTime = System.currentTimeMillis()
            
            onNodeWithContentDescription("Voice control").performClick()
            waitForIdle()
            
            // Simulate voice commands (in real test would use actual voice)
            Thread.sleep(3000) // Simulate listening time
            
            onNodeWithContentDescription("Voice control").performClick()
            waitForIdle()
            
            onNodeWithText("Stop Listening").performClick()
            waitForIdle()
            
            val sessionEndTime = System.currentTimeMillis()
            val sessionDuration = sessionEndTime - sessionStartTime
            
            Thread.sleep(1000) // Allow analytics to process
            
            // Verify voice control analytics
            val analyticsData = analytics.getLocalAnalytics()
            val voiceSessionEvents = analyticsData["voice_control_session_ended"] ?: emptyList()
            
            if (voiceSessionEvents.isNotEmpty()) {
                val sessionEvent = voiceSessionEvents.last()
                val sessionParams = sessionEvent.parameters
                
                // Verify session duration is tracked
                val capturedDuration = sessionParams["session_duration_ms"] as? Long
                assert(capturedDuration != null && capturedDuration > 0) {
                    "Voice control session duration should be tracked"
                }
                
                // Verify command counts are tracked
                val totalCommands = sessionParams["total_commands"] as? Int
                val successfulCommands = sessionParams["successful_commands"] as? Int
                
                assert(totalCommands != null && totalCommands >= 0) {
                    "Total voice commands should be tracked"
                }
                
                assert(successfulCommands != null && successfulCommands >= 0) {
                    "Successful voice commands should be tracked"
                }
                
                // Verify success rate is calculated
                val successRate = sessionParams["success_rate"] as? Float
                assert(successRate != null && successRate >= 0.0f && successRate <= 1.0f) {
                    "Voice command success rate should be between 0 and 1"
                }
            }
            
            device.pressBack()
        }
    }

    @Test
    fun testAISubtitleAnalyticsAccuracy() {
        composeTestRule.apply {
            analytics.clearLocalAnalytics()
            
            startVideoPlaybackIfAvailable()
            
            // Attempt AI subtitle generation
            onNodeWithContentDescription("Subtitle options").performClick()
            waitForIdle()
            
            onNodeWithText("Generate AI Subtitles").performClick()
            waitForIdle()
            
            val generationStartTime = System.currentTimeMillis()
            
            onNodeWithText("Target Language").performClick()
            val selectedLanguage = "English"
            onNodeWithText(selectedLanguage).performClick()
            
            onNodeWithText("Generate Subtitles").performClick()
            waitForIdle()
            
            // Wait for generation (simulated)
            Thread.sleep(3000)
            
            val generationEndTime = System.currentTimeMillis()
            val generationDuration = generationEndTime - generationStartTime
            
            Thread.sleep(1000) // Allow analytics to process
            
            // Verify AI subtitle analytics
            val analyticsData = analytics.getLocalAnalytics()
            val subtitleEvents = analyticsData["subtitle_generation_completed"] ?: emptyList()
            
            if (subtitleEvents.isNotEmpty()) {
                val subtitleEvent = subtitleEvents.last()
                val subtitleParams = subtitleEvent.parameters
                
                // Verify target language is captured
                val capturedLanguage = subtitleParams["target_language"] as? String
                assert(capturedLanguage == selectedLanguage) {
                    "Target language should be tracked correctly"
                }
                
                // Verify generation time is tracked
                val capturedGenerationTime = subtitleParams["generation_time_ms"] as? Long
                assert(capturedGenerationTime != null && capturedGenerationTime > 0) {
                    "Subtitle generation time should be tracked"
                }
                
                // Verify success/failure is tracked
                val generationSuccess = subtitleParams["generation_success"] as? Boolean
                assert(generationSuccess != null) {
                    "Subtitle generation success status should be tracked"
                }
            }
            
            device.pressBack()
            device.pressBack()
            device.pressBack()
        }
    }

    @Test
    fun testBookmarkAnalyticsAccuracy() {
        composeTestRule.apply {
            analytics.clearLocalAnalytics()
            
            startVideoPlaybackIfAvailable()
            
            // Seek to specific position
            onNodeWithContentDescription("Seek bar").performTouchInput {
                swipeRight(startX = centerX * 0.2f, endX = centerX * 0.5f)
            }
            waitForIdle()
            
            Thread.sleep(1000) // Wait for seek to complete
            
            // Add bookmark
            onNodeWithContentDescription("Add bookmark").performClick()
            waitForIdle()
            
            onNodeWithText("Bookmark Title").performTextInput("Analytics Test Bookmark")
            onNodeWithText("Save Bookmark").performClick()
            waitForIdle()
            
            Thread.sleep(1000) // Allow analytics to process
            
            // Verify bookmark analytics
            val analyticsData = analytics.getLocalAnalytics()
            val bookmarkEvents = analyticsData["bookmark_action"] ?: emptyList()
            
            assert(bookmarkEvents.isNotEmpty()) {
                "Bookmark creation should be tracked"
            }
            
            val bookmarkEvent = bookmarkEvents.last()
            val bookmarkParams = bookmarkEvent.parameters
            
            // Verify bookmark action is tracked
            val bookmarkAction = bookmarkParams["bookmark_action"] as? String
            assert(bookmarkAction == "add") {
                "Bookmark action should be tracked as 'add'"
            }
            
            // Verify bookmark type is tracked
            val bookmarkType = bookmarkParams["bookmark_type"] as? String
            assert(bookmarkType == "manual") {
                "Manual bookmark type should be tracked correctly"
            }
            
            // Verify video position is tracked
            val videoPosition = bookmarkParams["video_position"] as? Long
            assert(videoPosition != null && videoPosition >= 0) {
                "Video position should be tracked for bookmarks"
            }
            
            // Verify position percentage is calculated
            val positionPercentage = bookmarkParams["position_percentage"] as? Float
            assert(positionPercentage != null && positionPercentage >= 0.0f && positionPercentage <= 1.0f) {
                "Bookmark position percentage should be between 0 and 1"
            }
            
            device.pressBack()
        }
    }

    @Test
    fun testSharingAnalyticsAccuracy() {
        composeTestRule.apply {
            analytics.clearLocalAnalytics()
            
            startVideoPlaybackIfAvailable()
            
            // Test video sharing
            onNodeWithContentDescription("Share").performClick()
            waitForIdle()
            
            onNodeWithText("Share Link").performClick()
            waitForIdle()
            
            Thread.sleep(2000) // Allow sharing to complete
            
            device.pressBack()
            
            // Test moment sharing
            onNodeWithText("Share Current Moment").performClick()
            waitForIdle()
            
            onNodeWithText("Duration").performClick()
            val selectedDuration = "30 seconds"
            onNodeWithText(selectedDuration).performClick()
            
            onNodeWithText("Create Moment").performClick()
            waitForIdle()
            
            Thread.sleep(2000) // Allow moment creation
            
            Thread.sleep(1000) // Allow analytics to process
            
            // Verify sharing analytics
            val analyticsData = analytics.getLocalAnalytics()
            val sharingEvents = analyticsData["content_shared"] ?: emptyList()
            
            if (sharingEvents.isNotEmpty()) {
                val sharingEvent = sharingEvents.last()
                val sharingParams = sharingEvent.parameters
                
                // Verify share type is tracked
                val shareType = sharingParams["share_type"] as? String
                assert(shareType != null) {
                    "Share type should be tracked"
                }
                
                // Verify content type is tracked
                val contentType = sharingParams["content_type"] as? String
                assert(contentType != null) {
                    "Content type should be tracked"
                }
                
                // Verify success status is tracked
                val shareSuccess = sharingParams["share_success"] as? Boolean
                assert(shareSuccess != null) {
                    "Share success status should be tracked"
                }
            }
            
            device.pressBack()
            device.pressBack()
        }
    }

    @Test
    fun testPerformanceAnalyticsAccuracy() {
        composeTestRule.apply {
            analytics.clearLocalAnalytics()
            
            // Change performance settings
            onNodeWithContentDescription("Settings").performClick()
            waitForIdle()
            
            onNodeWithText("Performance").performClick()
            waitForIdle()
            
            // Change quality preset
            onNodeWithText("Quality Preset").performClick()
            waitForIdle()
            
            val selectedPreset = "Battery Saver"
            onNodeWithText(selectedPreset).performClick()
            waitForIdle()
            
            // Enable data saver
            onNodeWithText("Data Saver Mode").performClick()
            waitForIdle()
            
            Thread.sleep(1000) // Allow analytics to process
            
            // Verify performance analytics
            val analyticsData = analytics.getLocalAnalytics()
            val performanceEvents = analyticsData["performance_settings_changed"] ?: emptyList()
            
            if (performanceEvents.isNotEmpty()) {
                val performanceEvent = performanceEvents.last()
                val performanceParams = performanceEvent.parameters
                
                // Verify quality preset is tracked
                val capturedPreset = performanceParams["quality_preset"] as? String
                assert(capturedPreset == selectedPreset) {
                    "Quality preset should be tracked correctly"
                }
                
                // Verify data saver mode is tracked
                val dataSaverMode = performanceParams["data_saver_mode"] as? Boolean
                assert(dataSaverMode == true) {
                    "Data saver mode setting should be tracked"
                }
                
                // Verify battery optimization is tracked
                val batteryOptimization = performanceParams["battery_optimization"] as? Boolean
                assert(batteryOptimization != null) {
                    "Battery optimization setting should be tracked"
                }
            }
            
            device.pressBack()
            device.pressBack()
        }
    }

    @Test
    fun testFeatureDiscoveryAnalytics() {
        composeTestRule.apply {
            analytics.clearLocalAnalytics()
            
            // Discover features through different methods
            
            // Menu discovery
            onNodeWithContentDescription("Settings").performClick()
            waitForIdle()
            
            val discoveryStartTime = System.currentTimeMillis()
            
            onNodeWithText("Voice Control").performClick()
            waitForIdle()
            
            val discoveryEndTime = System.currentTimeMillis()
            val discoveryTime = discoveryEndTime - discoveryStartTime
            
            device.pressBack()
            device.pressBack()
            
            Thread.sleep(1000) // Allow analytics to process
            
            // Verify feature discovery analytics
            val analyticsData = analytics.getLocalAnalytics()
            val discoveryEvents = analyticsData["feature_discovered"] ?: emptyList()
            
            if (discoveryEvents.isNotEmpty()) {
                val discoveryEvent = discoveryEvents.last()
                val discoveryParams = discoveryEvent.parameters
                
                // Verify feature name is tracked
                val featureName = discoveryParams["feature_name"] as? String
                assert(featureName != null) {
                    "Discovered feature name should be tracked"
                }
                
                // Verify discovery method is tracked
                val discoveryMethod = discoveryParams["discovery_method"] as? String
                assert(discoveryMethod == "menu") {
                    "Feature discovery method should be tracked"
                }
                
                // Verify time to discover is tracked if available
                val timeToDiscover = discoveryParams["time_to_discover_ms"] as? Long
                if (timeToDiscover != null) {
                    assert(timeToDiscover > 0) {
                        "Time to discover should be positive if tracked"
                    }
                }
            }
        }
    }

    @Test
    fun testAnalyticsDataCompleteness() {
        composeTestRule.apply {
            analytics.clearLocalAnalytics()
            
            // Perform a comprehensive user session
            performComprehensiveUserSession()
            
            Thread.sleep(2000) // Allow all analytics to process
            
            // Verify comprehensive analytics were captured
            val analyticsData = analytics.getLocalAnalytics()
            
            // Check that multiple event types were captured
            val expectedEventTypes = listOf(
                "feature_search_used",
                "equalizer_settings_changed", 
                "bookmark_action",
                "engagement_session"
            )
            
            expectedEventTypes.forEach { eventType ->
                val events = analyticsData[eventType] ?: emptyList()
                assert(events.isNotEmpty()) {
                    "Analytics should capture $eventType events"
                }
                
                // Verify each event has required fields
                events.forEach { event ->
                    assert(event.name.isNotEmpty()) {
                        "Event name should not be empty"
                    }
                    
                    assert(event.timestamp > 0) {
                        "Event timestamp should be valid"
                    }
                    
                    assert(event.parameters.isNotEmpty()) {
                        "Event should have parameters"
                    }
                }
            }
            
            // Verify no sensitive data is logged
            analyticsData.values.flatten().forEach { event ->
                event.parameters.forEach { (key, value) ->
                    val valueString = value.toString().lowercase()
                    
                    // Check for common sensitive data patterns
                    assert(!valueString.contains("password")) {
                        "Analytics should not contain passwords"
                    }
                    
                    assert(!valueString.contains("api_key")) {
                        "Analytics should not contain API keys"
                    }
                    
                    assert(!valueString.contains("token")) {
                        "Analytics should not contain tokens"
                    }
                    
                    // Check for email patterns
                    assert(!valueString.contains("@") || !valueString.contains(".com")) {
                        "Analytics should not contain email addresses"
                    }
                }
            }
        }
    }

    @Test
    fun testAnalyticsOptOut() {
        composeTestRule.apply {
            // Enable analytics initially
            analytics.enableAnalytics(true)
            analytics.clearLocalAnalytics()
            
            // Perform action that should be tracked
            onNodeWithContentDescription("Search").performClick()
            waitForIdle()
            
            onNodeWithText("Search videos...").performTextInput("opt out test")
            waitForIdle()
            
            device.pressBack()
            device.pressBack()
            
            Thread.sleep(1000)
            
            // Verify analytics were captured
            var analyticsData = analytics.getLocalAnalytics()
            var searchEvents = analyticsData["feature_search_used"] ?: emptyList()
            
            assert(searchEvents.isNotEmpty()) {
                "Analytics should be captured when enabled"
            }
            
            // Disable analytics
            analytics.enableAnalytics(false)
            analytics.clearLocalAnalytics()
            
            // Perform another action
            onNodeWithContentDescription("Search").performClick()
            waitForIdle()
            
            onNodeWithText("Search videos...").performTextInput("should not track")
            waitForIdle()
            
            device.pressBack()
            device.pressBack()
            
            Thread.sleep(1000)
            
            // Verify analytics were not captured
            analyticsData = analytics.getLocalAnalytics()
            searchEvents = analyticsData["feature_search_used"] ?: emptyList()
            
            assert(searchEvents.isEmpty()) {
                "Analytics should not be captured when disabled"
            }
        }
    }

    // Helper methods

    private fun performComprehensiveUserSession() {
        composeTestRule.apply {
            // Search
            onNodeWithContentDescription("Search").performClick()
            waitForIdle()
            
            onNodeWithText("Search videos...").performTextInput("comprehensive test")
            waitForIdle()
            
            device.pressBack()
            device.pressBack()
            
            // Video playback
            startVideoPlaybackIfAvailable()
            
            // Equalizer
            onNodeWithContentDescription("Audio settings").performClick()
            waitForIdle()
            
            onNodeWithText("Equalizer").performClick()
            waitForIdle()
            
            onNodeWithText("Jazz").performClick()
            waitForIdle()
            
            device.pressBack()
            device.pressBack()
            
            // Bookmark
            onNodeWithContentDescription("Add bookmark").performClick()
            waitForIdle()
            
            onNodeWithText("Bookmark Title").performTextInput("Session Test")
            onNodeWithText("Save Bookmark").performClick()
            waitForIdle()
            
            device.pressBack()
        }
    }

    private fun startVideoPlaybackIfAvailable() {
        composeTestRule.apply {
            try {
                onNodeWithContentDescription("Browse Videos").performClick()
                waitForIdle()
                
                onNodeWithText("Recent Files").performClick()
                waitForIdle()
                
                Thread.sleep(1000)
                
                if (onAllNodesWithContentDescription("Play video").fetchSemanticsNodes().isNotEmpty()) {
                    onAllNodesWithContentDescription("Play video").onFirst().performClick()
                    waitForIdle()
                    
                    Thread.sleep(500)
                }
            } catch (e: Exception) {
                // Video might not be available
            }
        }
    }

    private fun ComposeTestRule.waitForIdle() {
        Thread.sleep(300)
        this.waitForIdle()
    }
}