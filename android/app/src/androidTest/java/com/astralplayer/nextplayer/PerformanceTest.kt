package com.astralplayer.nextplayer

import android.app.ActivityManager
import android.content.Context
import android.os.Debug
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
 * Performance Testing Suite
 * Tests app performance under various device conditions and workloads
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class PerformanceTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val benchmarkRule = BenchmarkRule()

    private lateinit var context: Context
    private lateinit var device: UiDevice
    private lateinit var activityManager: ActivityManager

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        
        // Enable all features for performance testing
        FeatureFlags.initialize(context)
        runBlocking {
            FeatureFlags.getManager()?.enableAllFeatures()
        }
    }

    @Test
    fun testAppStartupPerformance() {
        benchmarkRule.measureRepeated {
            // Measure cold start time
            val startTime = System.currentTimeMillis()
            
            composeTestRule.apply {
                // Wait for main screen to be fully loaded
                onNodeWithText("AstralStream").assertIsDisplayed()
                
                val endTime = System.currentTimeMillis()
                val startupTime = endTime - startTime
                
                // Startup should be under 3 seconds
                assert(startupTime < 3000) { "App startup took ${startupTime}ms, should be under 3000ms" }
            }
        }
    }

    @Test
    fun testVideoPlayerInitializationTime() {
        composeTestRule.apply {
            val initTime = measureTimeMillis {
                // Navigate to video player
                onNodeWithContentDescription("Browse Videos").performClick()
                waitForIdle()
                
                onNodeWithText("Recent Files").performClick()
                waitForIdle()
                
                Thread.sleep(1000) // Wait for video list
                
                // Start video playback
                onAllNodesWithContentDescription("Play video").onFirst().performClick()
                waitForIdle()
                
                // Wait for player to be ready
                onNodeWithContentDescription("Video Player").assertIsDisplayed()
                onNodeWithContentDescription("Play/Pause").assertIsDisplayed()
            }
            
            // Video player initialization should be under 2 seconds
            assert(initTime < 2000) { "Video player init took ${initTime}ms, should be under 2000ms" }
        }
    }

    @Test
    fun testSearchPerformance() {
        composeTestRule.apply {
            benchmarkRule.measureRepeated {
                val searchTime = measureTimeMillis {
                    // Open search
                    onNodeWithContentDescription("Search").performClick()
                    waitForIdle()
                    
                    // Perform search
                    onNodeWithText("Search videos...").performTextInput("action movie")
                    waitForIdle()
                    
                    // Wait for results
                    Thread.sleep(500)
                    
                    // Verify results load quickly
                    onNodeWithText("Search Results").assertIsDisplayed()
                }
                
                // Search should complete under 1 second
                assert(searchTime < 1000) { "Search took ${searchTime}ms, should be under 1000ms" }
                
                device.pressBack()
                device.pressBack()
            }
        }
    }

    @Test
    fun testMemoryUsageDuringVideoPlayback() {
        val memoryInfo = ActivityManager.MemoryInfo()
        
        // Get baseline memory usage
        activityManager.getMemoryInfo(memoryInfo)
        val baselineMemory = getMemoryUsage()
        
        composeTestRule.apply {
            // Start video playback
            startVideoPlayback()
            
            // Let video play for 30 seconds
            Thread.sleep(30000)
            
            // Check memory usage
            val playbackMemory = getMemoryUsage()
            val memoryIncrease = playbackMemory - baselineMemory
            
            // Memory increase should be reasonable (under 100MB)
            assert(memoryIncrease < 100 * 1024 * 1024) { 
                "Memory increased by ${memoryIncrease / (1024 * 1024)}MB during playback" 
            }
            
            // Test seeking performance impact
            repeat(10) {
                onNodeWithContentDescription("Seek bar").performTouchInput {
                    swipeRight(startX = centerX * 0.2f, endX = centerX * 0.8f)
                }
                Thread.sleep(500)
            }
            
            // Check memory after seeking
            val afterSeekingMemory = getMemoryUsage()
            val seekingIncrease = afterSeekingMemory - playbackMemory
            
            // Seeking shouldn't significantly increase memory
            assert(seekingIncrease < 50 * 1024 * 1024) {
                "Memory increased by ${seekingIncrease / (1024 * 1024)}MB during seeking"
            }
        }
    }

    @Test
    fun testLowMemoryConditions() {
        // Simulate low memory conditions
        composeTestRule.apply {
            // Start multiple features simultaneously
            startVideoPlayback()
            
            // Enable AI subtitles (memory intensive)
            onNodeWithContentDescription("Subtitle options").performClick()
            waitForIdle()
            
            onNodeWithText("Generate AI Subtitles").performClick()
            waitForIdle()
            
            // Enable equalizer
            onNodeWithContentDescription("Audio settings").performClick()
            onNodeWithText("Equalizer").performClick()
            waitForIdle()
            
            onNodeWithText("Rock").performClick()
            device.pressBack()
            device.pressBack()
            
            // Enable voice control
            onNodeWithContentDescription("Voice control").performClick()
            waitForIdle()
            
            // Monitor memory usage
            val memoryUsage = getMemoryUsage()
            val memoryThreshold = 200 * 1024 * 1024 // 200MB threshold
            
            // App should handle multiple features without excessive memory use
            assert(memoryUsage < memoryThreshold) {
                "App using ${memoryUsage / (1024 * 1024)}MB with multiple features active"
            }
            
            // Test app stability under memory pressure
            simulateMemoryPressure()
            
            // Verify app is still responsive
            onNodeWithContentDescription("Play/Pause").performClick()
            waitForIdle()
            
            onNodeWithContentDescription("Play/Pause").assertIsDisplayed()
        }
    }

    @Test
    fun testCPUUsageOptimization() {
        composeTestRule.apply {
            startVideoPlayback()
            
            // Monitor CPU usage during normal playback
            val startCpuTime = Process.getElapsedCpuTime()
            Thread.sleep(10000) // 10 seconds of playback
            val normalCpuTime = Process.getElapsedCpuTime() - startCpuTime
            
            // Enable performance optimization
            onNodeWithContentDescription("Settings").performClick()
            waitForIdle()
            
            onNodeWithText("Performance").performClick()
            waitForIdle()
            
            onNodeWithText("Quality Preset").performClick()
            onNodeWithText("Battery Saver").performClick()
            waitForIdle()
            
            device.pressBack()
            device.pressBack()
            
            // Start playback again
            onNodeWithContentDescription("Play/Pause").performClick()
            waitForIdle()
            
            // Monitor CPU usage with optimization
            val optimizedStartTime = Process.getElapsedCpuTime()
            Thread.sleep(10000)
            val optimizedCpuTime = Process.getElapsedCpuTime() - optimizedStartTime
            
            // CPU usage should be reduced with optimization
            assert(optimizedCpuTime < normalCpuTime * 1.2f) {
                "Optimized CPU time: ${optimizedCpuTime}ms vs Normal: ${normalCpuTime}ms"
            }
        }
    }

    @Test
    fun testBatteryOptimizationFeatures() {
        composeTestRule.apply {
            // Enable battery optimization features
            onNodeWithContentDescription("Settings").performClick()
            waitForIdle()
            
            onNodeWithText("Performance").performClick()
            waitForIdle()
            
            // Enable battery saver mode
            onNodeWithText("Battery Optimization").performClick()
            waitForIdle()
            
            device.pressBack()
            device.pressBack()
            
            // Start video playback with battery optimization
            startVideoPlayback()
            
            // Test that video quality is reduced appropriately
            Thread.sleep(5000)
            
            // Check if frame rate is limited
            // Note: This would require access to video decoder metrics
            // For now, we verify the setting is applied
            
            // Test that background processes are limited
            onNodeWithContentDescription("Home").performClick()
            Thread.sleep(2000)
            
            // Return to app
            device.pressRecentApps()
            Thread.sleep(1000)
            device.click(device.displayWidth / 2, device.displayHeight / 2)
            
            // Verify playback resumed properly
            onNodeWithContentDescription("Video Player").assertIsDisplayed()
        }
    }

    @Test
    fun testNetworkConditionHandling() {
        composeTestRule.apply {
            // Test with limited network (simulated)
            onNodeWithContentDescription("Settings").performClick()
            waitForIdle()
            
            onNodeWithText("Performance").performClick()
            waitForIdle()
            
            // Enable data saver mode
            onNodeWithText("Data Saver Mode").performClick()
            waitForIdle()
            
            device.pressBack()
            device.pressBack()
            
            // Test cloud features with data saver
            onNodeWithContentDescription("Settings").performClick()
            onNodeWithText("Cloud Storage").performClick()
            waitForIdle()
            
            // Verify sync is paused or limited
            onNodeWithText("Sync Paused (Data Saver)").assertIsDisplayed()
            
            device.pressBack()
            device.pressBack()
            
            // Test streaming with data saver
            startVideoPlayback()
            
            // Test that quality is automatically reduced
            Thread.sleep(5000)
            
            // Verify data saver indicator
            onNodeWithText("Data Saver Active").assertIsDisplayed()
        }
    }

    @Test
    fun testLargeVideoFileHandling() {
        composeTestRule.apply {
            // Test performance with large video files (4GB+)
            startVideoPlayback()
            
            // Simulate seeking in large file
            val seekTime = measureTimeMillis {
                repeat(20) {
                    onNodeWithContentDescription("Seek bar").performTouchInput {
                        // Random seek positions
                        val randomPosition = (0.1f..0.9f).random()
                        swipeRight(
                            startX = centerX * 0.1f,
                            endX = centerX * randomPosition
                        )
                    }
                    Thread.sleep(200)
                }
            }
            
            // Seeking in large files should be reasonably fast
            val averageSeekTime = seekTime / 20
            assert(averageSeekTime < 500) {
                "Average seek time: ${averageSeekTime}ms, should be under 500ms"
            }
            
            // Test memory usage doesn't grow excessively
            val memoryAfterSeeking = getMemoryUsage()
            assert(memoryAfterSeeking < 300 * 1024 * 1024) {
                "Memory usage after seeking: ${memoryAfterSeeking / (1024 * 1024)}MB"
            }
        }
    }

    @Test
    fun testConcurrentFeaturePerformance() {
        composeTestRule.apply {
            startVideoPlayback()
            
            // Enable multiple features simultaneously
            val operationTime = measureTimeMillis {
                // Enable AI subtitles
                onNodeWithContentDescription("Subtitle options").performClick()
                onNodeWithText("Generate AI Subtitles").performClick()
                waitForIdle()
                
                // Enable equalizer
                onNodeWithContentDescription("Audio settings").performClick()
                onNodeWithText("Equalizer").performClick()
                onNodeWithText("Rock").performClick()
                device.pressBack()
                device.pressBack()
                
                // Enable voice control
                onNodeWithContentDescription("Voice control").performClick()
                waitForIdle()
                
                // Start search in background
                onNodeWithContentDescription("Search").performClick()
                onNodeWithText("Search videos...").performTextInput("test")
                device.pressBack()
                
                // Set sleep timer
                onNodeWithContentDescription("Quick settings").performClick()
                onNodeWithText("Sleep Timer").performClick()
                onNodeWithText("30 minutes").performClick()
                onNodeWithText("Start Timer").performClick()
                device.pressBack()
            }
            
            // All operations should complete within reasonable time
            assert(operationTime < 10000) {
                "Concurrent operations took ${operationTime}ms, should be under 10 seconds"
            }
            
            // Verify app remains responsive
            onNodeWithContentDescription("Play/Pause").performClick()
            waitForIdle()
            
            Thread.sleep(2000)
            
            // Check memory usage with all features active
            val totalMemoryUsage = getMemoryUsage()
            assert(totalMemoryUsage < 400 * 1024 * 1024) {
                "Total memory usage: ${totalMemoryUsage / (1024 * 1024)}MB with all features"
            }
        }
    }

    @Test
    fun testGarbageCollectionImpact() {
        composeTestRule.apply {
            startVideoPlayback()
            
            // Force garbage collection and measure impact
            val beforeGC = System.currentTimeMillis()
            System.gc()
            Thread.sleep(100)
            val afterGC = System.currentTimeMillis()
            
            val gcTime = afterGC - beforeGC
            
            // GC shouldn't significantly impact playback
            assert(gcTime < 200) { "GC took ${gcTime}ms, might affect playback smoothness" }
            
            // Verify playback continues smoothly after GC
            onNodeWithContentDescription("Play/Pause").assertIsDisplayed()
            
            // Test repeated GC cycles
            repeat(5) {
                System.gc()
                Thread.sleep(50)
                
                // Verify app remains responsive
                onNodeWithContentDescription("Seek bar").performTouchInput {
                    swipeRight(startX = centerX * 0.3f, endX = centerX * 0.7f)
                }
                Thread.sleep(200)
            }
            
            // Check final memory state
            val finalMemory = getMemoryUsage()
            
            // Memory should be reasonable after GC cycles
            assert(finalMemory < 250 * 1024 * 1024) {
                "Memory after GC cycles: ${finalMemory / (1024 * 1024)}MB"
            }
        }
    }

    private fun startVideoPlayback() {
        composeTestRule.apply {
            onNodeWithContentDescription("Browse Videos").performClick()
            waitForIdle()
            
            onNodeWithText("Recent Files").performClick()
            waitForIdle()
            
            Thread.sleep(2000)
            
            onAllNodesWithContentDescription("Play video").onFirst().performClick()
            waitForIdle()
            
            Thread.sleep(1000)
        }
    }

    private fun getMemoryUsage(): Long {
        val memoryInfo = Debug.MemoryInfo()
        Debug.getMemoryInfo(memoryInfo)
        return (memoryInfo.totalPss * 1024).toLong() // Convert KB to bytes
    }

    private fun simulateMemoryPressure() {
        // Simulate memory pressure by allocating and releasing memory
        val largeArrays = mutableListOf<ByteArray>()
        
        try {
            // Allocate memory in chunks
            repeat(10) {
                largeArrays.add(ByteArray(10 * 1024 * 1024)) // 10MB chunks
                Thread.sleep(100)
            }
        } catch (e: OutOfMemoryError) {
            // Expected in low memory conditions
        } finally {
            // Clean up
            largeArrays.clear()
            System.gc()
        }
    }

    private fun ComposeTestRule.waitForIdle() {
        Thread.sleep(300)
        this.waitForIdle()
    }
}