package com.astralplayer.nextplayer

import android.content.Context
import android.os.SystemClock
import androidx.benchmark.junit4.BenchmarkRule
import androidx.benchmark.junit4.measureRepeated
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
import kotlin.system.measureTimeMillis

/**
 * Benchmark Tests for App Startup and Feature Initialization
 * Measures performance metrics for key app operations
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class BenchmarkTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val benchmarkRule = BenchmarkRule()

    private lateinit var context: Context
    private lateinit var device: UiDevice

    // Performance thresholds (in milliseconds)
    private val APP_START_THRESHOLD = 3000L
    private val VIDEO_PLAYER_INIT_THRESHOLD = 2000L
    private val SEARCH_RESPONSE_THRESHOLD = 1000L
    private val EQUALIZER_INIT_THRESHOLD = 500L
    private val SETTINGS_LOAD_THRESHOLD = 800L
    private val FEATURE_TOGGLE_THRESHOLD = 200L

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        
        // Enable all features for benchmarking
        FeatureFlags.initialize(context)
        runBlocking {
            FeatureFlags.getManager()?.enableAllFeatures()
        }
    }

    @Test
    fun benchmarkAppStartupTime() {
        benchmarkRule.measureRepeated {
            val startTime = SystemClock.elapsedRealtime()
            
            // Measure time to main screen fully loaded
            composeTestRule.apply {
                onNodeWithText("AstralStream").assertIsDisplayed()
                onNodeWithContentDescription("Browse Videos").assertIsDisplayed()
                onNodeWithContentDescription("Search").assertIsDisplayed()
                onNodeWithContentDescription("Settings").assertIsDisplayed()
            }
            
            val endTime = SystemClock.elapsedRealtime()
            val startupTime = endTime - startTime
            
            assert(startupTime < APP_START_THRESHOLD) {
                "App startup took ${startupTime}ms, exceeds threshold of ${APP_START_THRESHOLD}ms"
            }
            
            println("App startup time: ${startupTime}ms")
        }
    }

    @Test
    fun benchmarkVideoPlayerInitialization() {
        benchmarkRule.measureRepeated {
            composeTestRule.apply {
                val initTime = measureTimeMillis {
                    // Navigate to video player
                    onNodeWithContentDescription("Browse Videos").performClick()
                    waitForIdle()
                    
                    onNodeWithText("Recent Files").performClick()
                    waitForIdle()
                    
                    Thread.sleep(1000) // Wait for video list
                    
                    if (onAllNodesWithContentDescription("Play video").fetchSemanticsNodes().isNotEmpty()) {
                        onAllNodesWithContentDescription("Play video").onFirst().performClick()
                        waitForIdle()
                        
                        // Wait for player to be fully ready
                        onNodeWithContentDescription("Video Player").assertIsDisplayed()
                        onNodeWithContentDescription("Play/Pause").assertIsDisplayed()
                        onNodeWithContentDescription("Seek bar").assertIsDisplayed()
                    }
                }
                
                assert(initTime < VIDEO_PLAYER_INIT_THRESHOLD) {
                    "Video player init took ${initTime}ms, exceeds threshold of ${VIDEO_PLAYER_INIT_THRESHOLD}ms"
                }
                
                println("Video player initialization time: ${initTime}ms")
                
                device.pressBack()
                device.pressBack()
                device.pressBack()
            }
        }
    }

    @Test
    fun benchmarkSearchPerformance() {
        benchmarkRule.measureRepeated {
            composeTestRule.apply {
                val searchTime = measureTimeMillis {
                    // Open search
                    onNodeWithContentDescription("Search").performClick()
                    waitForIdle()
                    
                    // Perform search
                    onNodeWithText("Search videos...").performTextInput("test query")
                    waitForIdle()
                    
                    // Wait for results
                    Thread.sleep(500)
                    
                    // Verify search interface is responsive
                    onNodeWithText("Search videos...").assertIsDisplayed()
                }
                
                assert(searchTime < SEARCH_RESPONSE_THRESHOLD) {
                    "Search took ${searchTime}ms, exceeds threshold of ${SEARCH_RESPONSE_THRESHOLD}ms"
                }
                
                println("Search response time: ${searchTime}ms")
                
                device.pressBack()
                device.pressBack()
            }
        }
    }

    @Test
    fun benchmarkEqualizerInitialization() {
        benchmarkRule.measureRepeated {
            composeTestRule.apply {
                startVideoPlaybackIfAvailable()
                
                val equalizerTime = measureTimeMillis {
                    // Open equalizer
                    onNodeWithContentDescription("Audio settings").performClick()
                    waitForIdle()
                    
                    onNodeWithText("Equalizer").performClick()
                    waitForIdle()
                    
                    // Verify equalizer is fully loaded
                    onNodeWithText("Audio Equalizer").assertIsDisplayed()
                    onNodeWithText("Rock").assertIsDisplayed()
                    onNodeWithText("Pop").assertIsDisplayed()
                    onNodeWithText("Jazz").assertIsDisplayed()
                }
                
                assert(equalizerTime < EQUALIZER_INIT_THRESHOLD) {
                    "Equalizer init took ${equalizerTime}ms, exceeds threshold of ${EQUALIZER_INIT_THRESHOLD}ms"
                }
                
                println("Equalizer initialization time: ${equalizerTime}ms")
                
                device.pressBack()
                device.pressBack()
                device.pressBack()
            }
        }
    }

    @Test
    fun benchmarkSettingsLoadTime() {
        benchmarkRule.measureRepeated {
            composeTestRule.apply {
                val settingsTime = measureTimeMillis {
                    // Open settings
                    onNodeWithContentDescription("Settings").performClick()
                    waitForIdle()
                    
                    // Verify all settings sections are loaded
                    onNodeWithText("Settings").assertIsDisplayed()
                    onNodeWithText("Advanced Search").assertIsDisplayed()
                    onNodeWithText("Voice Control").assertIsDisplayed()
                    onNodeWithText("Audio Equalizer").assertIsDisplayed()
                    onNodeWithText("Accessibility").assertIsDisplayed()
                    onNodeWithText("Performance").assertIsDisplayed()
                }
                
                assert(settingsTime < SETTINGS_LOAD_THRESHOLD) {
                    "Settings load took ${settingsTime}ms, exceeds threshold of ${SETTINGS_LOAD_THRESHOLD}ms"
                }
                
                println("Settings load time: ${settingsTime}ms")
                
                device.pressBack()
            }
        }
    }

    @Test
    fun benchmarkFeatureFlagToggle() {
        benchmarkRule.measureRepeated {
            composeTestRule.apply {
                // Navigate to feature flags
                onNodeWithContentDescription("Settings").performClick()
                waitForIdle()
                
                onNodeWithText("Advanced").performClick()
                waitForIdle()
                
                onNodeWithText("Feature Flags").performClick()
                waitForIdle()
                
                val toggleTime = measureTimeMillis {
                    // Toggle a feature flag
                    onAllNodesWithContentDescription("Feature toggle").onFirst().performClick()
                    waitForIdle()
                    
                    // Verify toggle completed
                    Thread.sleep(100)
                }
                
                assert(toggleTime < FEATURE_TOGGLE_THRESHOLD) {
                    "Feature toggle took ${toggleTime}ms, exceeds threshold of ${FEATURE_TOGGLE_THRESHOLD}ms"
                }
                
                println("Feature toggle time: ${toggleTime}ms")
                
                device.pressBack()
                device.pressBack()
                device.pressBack()
            }
        }
    }

    @Test
    fun benchmarkAISubtitleInitialization() {
        benchmarkRule.measureRepeated {
            composeTestRule.apply {
                startVideoPlaybackIfAvailable()
                
                val aiSubtitleTime = measureTimeMillis {
                    // Open AI subtitle generation
                    onNodeWithContentDescription("Subtitle options").performClick()
                    waitForIdle()
                    
                    onNodeWithText("Generate AI Subtitles").performClick()
                    waitForIdle()
                    
                    // Verify interface loads
                    onNodeWithText("Target Language").assertIsDisplayed()
                    onNodeWithText("Generate Subtitles").assertIsDisplayed()
                }
                
                println("AI subtitle interface load time: ${aiSubtitleTime}ms")
                
                device.pressBack()
                device.pressBack()
                device.pressBack()
            }
        }
    }

    @Test
    fun benchmarkVoiceControlActivation() {
        benchmarkRule.measureRepeated {
            composeTestRule.apply {
                startVideoPlaybackIfAvailable()
                
                val voiceControlTime = measureTimeMillis {
                    // Activate voice control
                    onNodeWithContentDescription("Voice control").performClick()
                    waitForIdle()
                    
                    // Verify voice control is active
                    onNodeWithText("Listening...").assertIsDisplayed()
                    
                    Thread.sleep(1000)
                    
                    // Deactivate
                    onNodeWithContentDescription("Voice control").performClick()
                    waitForIdle()
                    
                    onNodeWithText("Stop Listening").performClick()
                    waitForIdle()
                }
                
                println("Voice control activation time: ${voiceControlTime}ms")
                
                device.pressBack()
            }
        }
    }

    @Test
    fun benchmarkBookmarkCreation() {
        benchmarkRule.measureRepeated {
            composeTestRule.apply {
                startVideoPlaybackIfAvailable()
                
                val bookmarkTime = measureTimeMillis {
                    // Create bookmark
                    onNodeWithContentDescription("Add bookmark").performClick()
                    waitForIdle()
                    
                    onNodeWithText("Bookmark Title").performTextInput("Benchmark Test")
                    onNodeWithText("Save Bookmark").performClick()
                    waitForIdle()
                    
                    // Verify bookmark created
                    onNodeWithText("Bookmark added").assertIsDisplayed()
                }
                
                println("Bookmark creation time: ${bookmarkTime}ms")
                
                device.pressBack()
            }
        }
    }

    @Test
    fun benchmarkSearchWithFilters() {
        benchmarkRule.measureRepeated {
            composeTestRule.apply {
                val filteredSearchTime = measureTimeMillis {
                    // Open search
                    onNodeWithContentDescription("Search").performClick()
                    waitForIdle()
                    
                    // Enter query
                    onNodeWithText("Search videos...").performTextInput("benchmark")
                    waitForIdle()
                    
                    // Apply filters
                    onNodeWithText("Filters").performClick()
                    waitForIdle()
                    
                    onNodeWithText("Duration").performClick()
                    waitForIdle()
                    
                    onNodeWithText("More than 1 hour").performClick()
                    waitForIdle()
                    
                    onNodeWithText("Quality").performClick()
                    waitForIdle()
                    
                    onNodeWithText("HD (720p+)").performClick()
                    waitForIdle()
                    
                    onNodeWithText("Apply Filters").performClick()
                    waitForIdle()
                    
                    // Wait for filtered results
                    Thread.sleep(500)
                }
                
                println("Filtered search time: ${filteredSearchTime}ms")
                
                device.pressBack()
                device.pressBack()
            }
        }
    }

    @Test
    fun benchmarkThemeSwitch() {
        benchmarkRule.measureRepeated {
            composeTestRule.apply {
                val themeSwitchTime = measureTimeMillis {
                    // Switch to dark mode
                    onNodeWithContentDescription("Settings").performClick()
                    waitForIdle()
                    
                    onNodeWithText("Appearance").performClick()
                    waitForIdle()
                    
                    onNodeWithText("Dark Mode").performClick()
                    waitForIdle()
                    
                    // Verify theme applied
                    Thread.sleep(200)
                    
                    device.pressBack()
                    device.pressBack()
                    
                    // Verify main screen updated
                    onNodeWithText("AstralStream").assertIsDisplayed()
                }
                
                println("Theme switch time: ${themeSwitchTime}ms")
            }
        }
    }

    @Test
    fun benchmarkCacheOperation() {
        benchmarkRule.measureRepeated {
            composeTestRule.apply {
                val cacheOpTime = measureTimeMillis {
                    // Navigate to cache management
                    onNodeWithContentDescription("Settings").performClick()
                    waitForIdle()
                    
                    onNodeWithText("Performance").performClick()
                    waitForIdle()
                    
                    onNodeWithText("Cache Management").performClick()
                    waitForIdle()
                    
                    // Perform cache optimization
                    onNodeWithText("Optimize Cache").performClick()
                    waitForIdle()
                    
                    // Wait for operation to complete
                    Thread.sleep(1000)
                }
                
                println("Cache optimization time: ${cacheOpTime}ms")
                
                device.pressBack()
                device.pressBack()
                device.pressBack()
            }
        }
    }

    @Test
    fun benchmarkMemoryUsagePattern() {
        benchmarkRule.measureRepeated {
            composeTestRule.apply {
                val initialMemory = getMemoryUsage()
                
                // Perform memory-intensive operations
                val operationTime = measureTimeMillis {
                    // Start video playback
                    startVideoPlaybackIfAvailable()
                    
                    // Enable AI subtitles
                    onNodeWithContentDescription("Subtitle options").performClick()
                    waitForIdle()
                    
                    onNodeWithText("Generate AI Subtitles").performClick()
                    waitForIdle()
                    
                    device.pressBack()
                    device.pressBack()
                    
                    // Use equalizer
                    onNodeWithContentDescription("Audio settings").performClick()
                    waitForIdle()
                    
                    onNodeWithText("Equalizer").performClick()
                    waitForIdle()
                    
                    onNodeWithText("Rock").performClick()
                    waitForIdle()
                    
                    device.pressBack()
                    device.pressBack()
                    
                    // Use voice control
                    onNodeWithContentDescription("Voice control").performClick()
                    waitForIdle()
                    
                    Thread.sleep(1000)
                    
                    onNodeWithContentDescription("Voice control").performClick()
                    waitForIdle()
                    
                    onNodeWithText("Stop Listening").performClick()
                    waitForIdle()
                    
                    device.pressBack()
                }
                
                val finalMemory = getMemoryUsage()
                val memoryIncrease = finalMemory - initialMemory
                
                println("Memory usage increased by ${memoryIncrease / (1024 * 1024)}MB during operations")
                println("Operation completion time: ${operationTime}ms")
                
                // Memory increase should be reasonable
                assert(memoryIncrease < 100 * 1024 * 1024) {
                    "Memory increased by ${memoryIncrease / (1024 * 1024)}MB, exceeds 100MB threshold"
                }
            }
        }
    }

    @Test
    fun benchmarkConcurrentFeatureUsage() {
        benchmarkRule.measureRepeated {
            composeTestRule.apply {
                val concurrentTime = measureTimeMillis {
                    startVideoPlaybackIfAvailable()
                    
                    // Enable multiple features simultaneously
                    
                    // 1. Enable equalizer
                    onNodeWithContentDescription("Audio settings").performClick()
                    waitForIdle()
                    
                    onNodeWithText("Equalizer").performClick()
                    waitForIdle()
                    
                    onNodeWithText("Rock").performClick()
                    waitForIdle()
                    
                    device.pressBack()
                    device.pressBack()
                    
                    // 2. Start voice control
                    onNodeWithContentDescription("Voice control").performClick()
                    waitForIdle()
                    
                    Thread.sleep(500)
                    
                    // 3. Create bookmark while voice is active
                    onNodeWithContentDescription("Add bookmark").performClick()
                    waitForIdle()
                    
                    onNodeWithText("Bookmark Title").performTextInput("Concurrent Test")
                    onNodeWithText("Save Bookmark").performClick()
                    waitForIdle()
                    
                    // 4. Stop voice control
                    onNodeWithContentDescription("Voice control").performClick()
                    waitForIdle()
                    
                    onNodeWithText("Stop Listening").performClick()
                    waitForIdle()
                    
                    device.pressBack()
                }
                
                println("Concurrent feature usage time: ${concurrentTime}ms")
                
                // Should handle concurrent features efficiently
                assert(concurrentTime < 10000) {
                    "Concurrent operations took ${concurrentTime}ms, exceeds 10 second threshold"
                }
            }
        }
    }

    // Helper methods

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

    private fun getMemoryUsage(): Long {
        val memoryInfo = android.os.Debug.MemoryInfo()
        android.os.Debug.getMemoryInfo(memoryInfo)
        return (memoryInfo.totalPss * 1024).toLong() // Convert KB to bytes
    }

    private fun ComposeTestRule.waitForIdle() {
        Thread.sleep(200)
        this.waitForIdle()
    }
}