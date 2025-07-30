package com.astralplayer.nextplayer

import android.app.ActivityManager
import android.content.Context
import android.os.Debug
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import com.astralplayer.nextplayer.feature.flags.FeatureFlags
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.lang.ref.WeakReference

/**
 * Memory Leak Detection and Optimization Tests
 * Identifies and validates fixes for memory leaks in new features
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class MemoryLeakTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    private lateinit var context: Context
    private lateinit var device: UiDevice
    private lateinit var activityManager: ActivityManager

    // Memory tracking
    private val memorySnapshots = mutableListOf<MemorySnapshot>()
    private val weakReferences = mutableListOf<WeakReference<Any>>()

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        
        // Enable all features for memory leak testing
        FeatureFlags.initialize(context)
        runBlocking {
            FeatureFlags.getManager()?.enableAllFeatures()
        }
        
        // Clear previous snapshots
        memorySnapshots.clear()
        weakReferences.clear()
        
        // Take baseline memory snapshot
        takeMemorySnapshot("baseline")
    }

    @Test
    fun testVideoPlayerMemoryLeaks() {
        composeTestRule.apply {
            val initialMemory = getCurrentMemoryUsage()
            
            // Start and stop video playback multiple times
            repeat(5) { iteration ->
                startVideoPlayback()
                
                // Let video play
                Thread.sleep(5000)
                
                // Take memory snapshot during playback
                takeMemorySnapshot("playback_$iteration")
                
                // Stop playback and navigate away
                device.pressBack()
                waitForIdle()
                
                // Force garbage collection
                System.gc()
                Thread.sleep(1000)
                
                // Take memory snapshot after cleanup
                takeMemorySnapshot("after_playback_$iteration")
            }
            
            val finalMemory = getCurrentMemoryUsage()
            val memoryIncrease = finalMemory - initialMemory
            
            // Memory increase should be minimal (under 50MB) after multiple sessions
            assert(memoryIncrease < 50 * 1024 * 1024) {
                "Memory increased by ${memoryIncrease / (1024 * 1024)}MB after video playback cycles"
            }
            
            // Verify memory trend is stable
            validateMemoryTrend("playback")
        }
    }

    @Test
    fun testAISubtitleMemoryLeaks() {
        composeTestRule.apply {
            val initialMemory = getCurrentMemoryUsage()
            
            startVideoPlayback()
            
            // Generate and cleanup AI subtitles multiple times
            repeat(3) { iteration ->
                // Generate AI subtitles
                onNodeWithContentDescription("Subtitle options").performClick()
                waitForIdle()
                
                onNodeWithText("Generate AI Subtitles").performClick()
                waitForIdle()
                
                onNodeWithText("Target Language").performClick()
                onNodeWithText("English").performClick()
                
                onNodeWithText("Generate Subtitles").performClick()
                waitForIdle()
                
                // Wait for generation (simulated)
                Thread.sleep(3000)
                takeMemorySnapshot("ai_subtitle_generation_$iteration")
                
                // Disable subtitles
                onNodeWithText("Disable Subtitles").performClick()
                waitForIdle()
                
                device.pressBack()
                
                // Force cleanup
                System.gc()
                Thread.sleep(1000)
                takeMemorySnapshot("ai_subtitle_cleanup_$iteration")
            }
            
            val finalMemory = getCurrentMemoryUsage()
            val memoryIncrease = finalMemory - initialMemory
            
            // AI subtitle generation shouldn't cause significant memory leaks
            assert(memoryIncrease < 100 * 1024 * 1024) {
                "AI subtitle memory increased by ${memoryIncrease / (1024 * 1024)}MB"
            }
        }
    }

    @Test
    fun testVoiceControlMemoryLeaks() {
        composeTestRule.apply {
            val initialMemory = getCurrentMemoryUsage()
            
            startVideoPlayback()
            
            // Start and stop voice control multiple times
            repeat(5) { iteration ->
                // Enable voice control
                onNodeWithContentDescription("Voice control").performClick()
                waitForIdle()
                
                takeMemorySnapshot("voice_control_start_$iteration")
                
                // Let voice control run
                Thread.sleep(3000)
                
                // Disable voice control
                onNodeWithContentDescription("Voice control").performClick()
                onNodeWithText("Stop Listening").performClick()
                waitForIdle()
                
                // Force cleanup
                System.gc()
                Thread.sleep(1000)
                takeMemorySnapshot("voice_control_stop_$iteration")
            }
            
            val finalMemory = getCurrentMemoryUsage()
            val memoryIncrease = finalMemory - initialMemory
            
            // Voice control shouldn't leak memory
            assert(memoryIncrease < 30 * 1024 * 1024) {
                "Voice control memory increased by ${memoryIncrease / (1024 * 1024)}MB"
            }
        }
    }

    @Test
    fun testEqualizerMemoryLeaks() {
        composeTestRule.apply {
            val initialMemory = getCurrentMemoryUsage()
            
            startVideoPlayback()
            
            // Test equalizer memory usage
            repeat(10) { iteration ->
                // Open equalizer
                onNodeWithContentDescription("Audio settings").performClick()
                onNodeWithText("Equalizer").performClick()
                waitForIdle()
                
                // Switch between presets
                val presets = listOf("Rock", "Pop", "Jazz", "Classical", "Electronic")
                onNodeWithText(presets[iteration % presets.size]).performClick()
                waitForIdle()
                
                takeMemorySnapshot("equalizer_preset_$iteration")
                
                // Close equalizer
                device.pressBack()
                device.pressBack()
                
                System.gc()
                Thread.sleep(500)
                takeMemorySnapshot("equalizer_closed_$iteration")
            }
            
            val finalMemory = getCurrentMemoryUsage()
            val memoryIncrease = finalMemory - initialMemory
            
            // Equalizer preset switching shouldn't leak memory
            assert(memoryIncrease < 20 * 1024 * 1024) {
                "Equalizer memory increased by ${memoryIncrease / (1024 * 1024)}MB"
            }
        }
    }

    @Test
    fun testSearchMemoryLeaks() {
        composeTestRule.apply {
            val initialMemory = getCurrentMemoryUsage()
            
            // Perform multiple searches
            repeat(10) { iteration ->
                // Open search
                onNodeWithContentDescription("Search").performClick()
                waitForIdle()
                
                // Perform search
                val searchQuery = "test query $iteration"
                onNodeWithText("Search videos...").performTextInput(searchQuery)
                waitForIdle()
                
                // Apply filters
                onNodeWithText("Filters").performClick()
                waitForIdle()
                
                onNodeWithText("Duration").performClick()
                onNodeWithText("More than 1 hour").performClick()
                
                onNodeWithText("Apply Filters").performClick()
                waitForIdle()
                
                takeMemorySnapshot("search_with_filters_$iteration")
                
                // Clear search and go back
                device.pressBack()
                device.pressBack()
                
                System.gc()
                Thread.sleep(500)
                takeMemorySnapshot("search_cleared_$iteration")
            }
            
            val finalMemory = getCurrentMemoryUsage()
            val memoryIncrease = finalMemory - initialMemory
            
            // Search operations shouldn't cause memory leaks
            assert(memoryIncrease < 40 * 1024 * 1024) {
                "Search memory increased by ${memoryIncrease / (1024 * 1024)}MB"
            }
        }
    }

    @Test
    fun testSocialSharingMemoryLeaks() {
        composeTestRule.apply {
            val initialMemory = getCurrentMemoryUsage()
            
            startVideoPlayback()
            
            // Test sharing feature memory usage
            repeat(5) { iteration ->
                // Open sharing
                onNodeWithContentDescription("Share").performClick()
                waitForIdle()
                
                // Generate different types of shares
                when (iteration % 3) {
                    0 -> {
                        onNodeWithText("Share Link").performClick()
                        waitForIdle()
                        takeMemorySnapshot("share_link_$iteration")
                    }
                    1 -> {
                        onNodeWithText("Share Current Moment").performClick()
                        waitForIdle()
                        onNodeWithText("30 seconds").performClick()
                        onNodeWithText("Create Moment").performClick()
                        waitForIdle()
                        takeMemorySnapshot("share_moment_$iteration")
                    }
                    2 -> {
                        onNodeWithText("Share File").performClick()
                        waitForIdle()
                        takeMemorySnapshot("share_file_$iteration")
                    }
                }
                
                // Close sharing
                device.pressBack()
                
                System.gc()
                Thread.sleep(1000)
                takeMemorySnapshot("sharing_closed_$iteration")
            }
            
            val finalMemory = getCurrentMemoryUsage()
            val memoryIncrease = finalMemory - initialMemory
            
            // Sharing features shouldn't leak memory
            assert(memoryIncrease < 60 * 1024 * 1024) {
                "Sharing memory increased by ${memoryIncrease / (1024 * 1024)}MB"
            }
        }
    }

    @Test
    fun testBookmarkMemoryLeaks() {
        composeTestRule.apply {
            val initialMemory = getCurrentMemoryUsage()
            
            startVideoPlayback()
            
            // Create and delete bookmarks multiple times
            repeat(20) { iteration ->
                // Seek to random position
                onNodeWithContentDescription("Seek bar").performTouchInput {
                    val randomPosition = (0.1f..0.9f).random()
                    swipeRight(
                        startX = centerX * 0.1f,
                        endX = centerX * randomPosition
                    )
                }
                waitForIdle()
                
                // Add bookmark
                onNodeWithContentDescription("Add bookmark").performClick()
                waitForIdle()
                
                onNodeWithText("Bookmark Title").performTextInput("Bookmark $iteration")
                onNodeWithText("Save Bookmark").performClick()
                waitForIdle()
                
                takeMemorySnapshot("bookmark_added_$iteration")
                
                if (iteration % 5 == 4) {
                    // Every 5 iterations, delete some bookmarks
                    onNodeWithContentDescription("Bookmarks").performClick()
                    waitForIdle()
                    
                    // Delete first bookmark
                    onAllNodesWithContentDescription("Delete bookmark").onFirst().performClick()
                    onNodeWithText("Delete").performClick()
                    waitForIdle()
                    
                    device.pressBack()
                    
                    System.gc()
                    Thread.sleep(500)
                    takeMemorySnapshot("bookmarks_cleaned_$iteration")
                }
            }
            
            val finalMemory = getCurrentMemoryUsage()
            val memoryIncrease = finalMemory - initialMemory
            
            // Bookmark operations shouldn't cause excessive memory growth
            assert(memoryIncrease < 50 * 1024 * 1024) {
                "Bookmark memory increased by ${memoryIncrease / (1024 * 1024)}MB"
            }
        }
    }

    @Test
    fun testPerformanceOptimizerMemoryLeaks() {
        composeTestRule.apply {
            val initialMemory = getCurrentMemoryUsage()
            
            // Test performance optimizer components
            repeat(5) { iteration ->
                // Open performance settings
                onNodeWithContentDescription("Settings").performClick()
                waitForIdle()
                
                onNodeWithText("Performance").performClick()
                waitForIdle()
                
                // Change quality preset
                onNodeWithText("Quality Preset").performClick()
                val presets = listOf("High Quality", "Balanced", "Battery Saver", "Data Saver")
                onNodeWithText(presets[iteration % presets.size]).performClick()
                waitForIdle()
                
                takeMemorySnapshot("performance_preset_$iteration")
                
                // Test cache management
                onNodeWithText("Cache Management").performClick()
                waitForIdle()
                
                onNodeWithText("Optimize Cache").performClick()
                waitForIdle()
                
                takeMemorySnapshot("cache_optimized_$iteration")
                
                // Clear cache periodically
                if (iteration % 2 == 1) {
                    onNodeWithText("Clear Cache").performClick()
                    onNodeWithText("Confirm").performClick()
                    waitForIdle()
                }
                
                device.pressBack()
                device.pressBack()
                device.pressBack()
                
                System.gc()
                Thread.sleep(1000)
                takeMemorySnapshot("performance_settings_closed_$iteration")
            }
            
            val finalMemory = getCurrentMemoryUsage()
            val memoryIncrease = finalMemory - initialMemory
            
            // Performance optimizer shouldn't leak memory
            assert(memoryIncrease < 30 * 1024 * 1024) {
                "Performance optimizer memory increased by ${memoryIncrease / (1024 * 1024)}MB"
            }
        }
    }

    @Test
    fun testAnalyticsMemoryLeaks() {
        composeTestRule.apply {
            val initialMemory = getCurrentMemoryUsage()
            
            // Generate lots of analytics events
            repeat(100) { iteration ->
                // Perform various actions to generate analytics
                when (iteration % 5) {
                    0 -> {
                        // Search action
                        onNodeWithContentDescription("Search").performClick()
                        onNodeWithText("Search videos...").performTextInput("test")
                        device.pressBack()
                        device.pressBack()
                    }
                    1 -> {
                        // Video player action
                        startVideoPlayback()
                        onNodeWithContentDescription("Play/Pause").performClick()
                        device.pressBack()
                    }
                    2 -> {
                        // Settings action
                        onNodeWithContentDescription("Settings").performClick()
                        onNodeWithText("Advanced Search").performClick()
                        device.pressBack()
                        device.pressBack()
                    }
                    3 -> {
                        // Equalizer action
                        if (iteration > 0) { // Only if video is playing
                            startVideoPlayback()
                            onNodeWithContentDescription("Audio settings").performClick()
                            onNodeWithText("Equalizer").performClick()
                            device.pressBack()
                            device.pressBack()
                            device.pressBack()
                        }
                    }
                    4 -> {
                        // Voice control action
                        if (iteration > 0) {
                            startVideoPlayback()
                            onNodeWithContentDescription("Voice control").performClick()
                            Thread.sleep(1000)
                            onNodeWithContentDescription("Voice control").performClick()
                            onNodeWithText("Stop Listening").performClick()
                            device.pressBack()
                        }
                    }
                }
                
                if (iteration % 20 == 19) {
                    takeMemorySnapshot("analytics_events_$iteration")
                    System.gc()
                    Thread.sleep(500)
                }
            }
            
            val finalMemory = getCurrentMemoryUsage()
            val memoryIncrease = finalMemory - initialMemory
            
            // Analytics shouldn't cause memory leaks even with many events
            assert(memoryIncrease < 80 * 1024 * 1024) {
                "Analytics memory increased by ${memoryIncrease / (1024 * 1024)}MB after 100 events"
            }
        }
    }

    @Test
    fun testFeatureFlagMemoryLeaks() {
        composeTestRule.apply {
            val initialMemory = getCurrentMemoryUsage()
            
            // Toggle features repeatedly
            repeat(20) { iteration ->
                // Open feature flags
                onNodeWithContentDescription("Settings").performClick()
                waitForIdle()
                
                onNodeWithText("Advanced").performClick()
                waitForIdle()
                
                onNodeWithText("Feature Flags").performClick()
                waitForIdle()
                
                // Toggle various features
                val features = listOf("Voice Control", "AI Subtitles", "Social Sharing", "Cloud Sync")
                val featureToToggle = features[iteration % features.size]
                
                // Find and toggle the feature
                onNodeWithText(featureToToggle).assertIsDisplayed()
                onAllNodesWithContentDescription("Feature toggle").onFirst().performClick()
                waitForIdle()
                
                takeMemorySnapshot("feature_toggle_$iteration")
                
                device.pressBack()
                device.pressBack()
                device.pressBack()
                
                System.gc()
                Thread.sleep(500)
                takeMemorySnapshot("feature_flags_closed_$iteration")
            }
            
            val finalMemory = getCurrentMemoryUsage()
            val memoryIncrease = finalMemory - initialMemory
            
            // Feature flag toggles shouldn't leak memory
            assert(memoryIncrease < 25 * 1024 * 1024) {
                "Feature flags memory increased by ${memoryIncrease / (1024 * 1024)}MB"
            }
        }
    }

    @Test
    fun testLongRunningSessionMemoryStability() {
        composeTestRule.apply {
            val initialMemory = getCurrentMemoryUsage()
            
            // Simulate a long user session with various activities
            repeat(50) { iteration ->
                when (iteration % 8) {
                    0 -> startAndUseVideoPlayer()
                    1 -> useSearchFeature()
                    2 -> useEqualizerFeature()
                    3 -> useVoiceControlFeature()
                    4 -> useSubtitleFeature()
                    5 -> useSharingFeature()
                    6 -> useBookmarkFeature()
                    7 -> useSettingsFeature()
                }
                
                // Take memory snapshots periodically
                if (iteration % 10 == 9) {
                    takeMemorySnapshot("long_session_$iteration")
                    
                    // Force garbage collection periodically
                    System.gc()
                    Thread.sleep(1000)
                    
                    val currentMemory = getCurrentMemoryUsage()
                    val memoryIncrease = currentMemory - initialMemory
                    
                    // Memory shouldn't grow excessively during long session
                    assert(memoryIncrease < 200 * 1024 * 1024) {
                        "Memory increased by ${memoryIncrease / (1024 * 1024)}MB at iteration $iteration"
                    }
                }
            }
            
            // Final memory check
            val finalMemory = getCurrentMemoryUsage()
            val totalMemoryIncrease = finalMemory - initialMemory
            
            // After long session, memory increase should be reasonable
            assert(totalMemoryIncrease < 150 * 1024 * 1024) {
                "Total memory increased by ${totalMemoryIncrease / (1024 * 1024)}MB after long session"
            }
            
            // Analyze memory trend
            validateMemoryTrend("long_session")
        }
    }

    // Helper methods for different feature usage patterns
    private fun startAndUseVideoPlayer() {
        composeTestRule.apply {
            try {
                startVideoPlayback()
                Thread.sleep(2000)
                onNodeWithContentDescription("Play/Pause").performClick()
                Thread.sleep(1000)
                device.pressBack()
            } catch (e: Exception) {
                // Ignore test failures, focus on memory
            }
        }
    }

    private fun useSearchFeature() {
        composeTestRule.apply {
            try {
                onNodeWithContentDescription("Search").performClick()
                waitForIdle()
                onNodeWithText("Search videos...").performTextInput("test")
                Thread.sleep(500)
                device.pressBack()
                device.pressBack()
            } catch (e: Exception) {
                device.pressBack()
            }
        }
    }

    private fun useEqualizerFeature() {
        composeTestRule.apply {
            try {
                startVideoPlayback()
                onNodeWithContentDescription("Audio settings").performClick()
                onNodeWithText("Equalizer").performClick()
                waitForIdle()
                onNodeWithText("Pop").performClick()
                device.pressBack()
                device.pressBack()
                device.pressBack()
            } catch (e: Exception) {
                device.pressBack()
                device.pressBack()
            }
        }
    }

    private fun useVoiceControlFeature() {
        composeTestRule.apply {
            try {
                startVideoPlayback()
                onNodeWithContentDescription("Voice control").performClick()
                Thread.sleep(1000)
                onNodeWithContentDescription("Voice control").performClick()
                onNodeWithText("Stop Listening").performClick()
                device.pressBack()
            } catch (e: Exception) {
                device.pressBack()
            }
        }
    }

    private fun useSubtitleFeature() {
        composeTestRule.apply {
            try {
                startVideoPlayback()
                onNodeWithContentDescription("Subtitle options").performClick()
                waitForIdle()
                onNodeWithText("Enable Subtitles").performClick()
                Thread.sleep(1000)
                device.pressBack()
                device.pressBack()
            } catch (e: Exception) {
                device.pressBack()
            }
        }
    }

    private fun useSharingFeature() {
        composeTestRule.apply {
            try {
                startVideoPlayback()
                onNodeWithContentDescription("Share").performClick()
                waitForIdle()
                onNodeWithText("Share Link").performClick()
                Thread.sleep(500)
                device.pressBack()
                device.pressBack()
            } catch (e: Exception) {
                device.pressBack()
            }
        }
    }

    private fun useBookmarkFeature() {
        composeTestRule.apply {
            try {
                startVideoPlayback()
                onNodeWithContentDescription("Add bookmark").performClick()
                waitForIdle()
                onNodeWithText("Bookmark Title").performTextInput("Test")
                onNodeWithText("Save Bookmark").performClick()
                Thread.sleep(500)
                device.pressBack()
            } catch (e: Exception) {
                device.pressBack()
            }
        }
    }

    private fun useSettingsFeature() {
        composeTestRule.apply {
            try {
                onNodeWithContentDescription("Settings").performClick()
                waitForIdle()
                onNodeWithText("Performance").performClick()
                waitForIdle()
                device.pressBack()
                device.pressBack()
            } catch (e: Exception) {
                device.pressBack()
            }
        }
    }

    private fun startVideoPlayback() {
        composeTestRule.apply {
            try {
                onNodeWithContentDescription("Browse Videos").performClick()
                waitForIdle()
                
                onNodeWithText("Recent Files").performClick()
                waitForIdle()
                
                Thread.sleep(1000)
                
                onAllNodesWithContentDescription("Play video").onFirst().performClick()
                waitForIdle()
                
                Thread.sleep(500)
            } catch (e: Exception) {
                // Video might already be playing or list might be empty
            }
        }
    }

    private fun getCurrentMemoryUsage(): Long {
        val memoryInfo = Debug.MemoryInfo()
        Debug.getMemoryInfo(memoryInfo)
        return (memoryInfo.totalPss * 1024).toLong() // Convert KB to bytes
    }

    private fun takeMemorySnapshot(label: String) {
        val memoryUsage = getCurrentMemoryUsage()
        val timestamp = System.currentTimeMillis()
        memorySnapshots.add(MemorySnapshot(label, memoryUsage, timestamp))
    }

    private fun validateMemoryTrend(prefix: String) {
        val relevantSnapshots = memorySnapshots.filter { it.label.startsWith(prefix) }
        
        if (relevantSnapshots.size < 3) return
        
        // Check if memory is growing linearly (indicates a leak)
        val memoryValues = relevantSnapshots.map { it.memoryUsage }
        val isGrowingLinearly = isLinearGrowth(memoryValues)
        
        assert(!isGrowingLinearly) {
            "Memory appears to be growing linearly for $prefix operations, indicating a possible leak"
        }
    }

    private fun isLinearGrowth(values: List<Long>): Boolean {
        if (values.size < 4) return false
        
        var increasingCount = 0
        for (i in 1 until values.size) {
            if (values[i] > values[i - 1]) {
                increasingCount++
            }
        }
        
        // If memory increases in more than 80% of measurements, consider it linear growth
        return (increasingCount.toFloat() / (values.size - 1)) > 0.8f
    }

    private fun ComposeTestRule.waitForIdle() {
        Thread.sleep(300)
        this.waitForIdle()
    }

    data class MemorySnapshot(
        val label: String,
        val memoryUsage: Long,
        val timestamp: Long
    )
}