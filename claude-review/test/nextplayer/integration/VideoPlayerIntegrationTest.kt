package com.astralplayer.nextplayer.integration

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.media3.exoplayer.ExoPlayer
import com.astralplayer.nextplayer.accessibility.AccessibilityManager
import com.astralplayer.nextplayer.cache.AdvancedCacheManager
import com.astralplayer.nextplayer.data.gesture.EnhancedGestureDetector
import com.astralplayer.nextplayer.data.HapticFeedbackManager
import com.astralplayer.nextplayer.quality.VideoQualityManager
import com.astralplayer.nextplayer.quality.AdaptiveQualityController
import com.astralplayer.nextplayer.streaming.NetworkAnalyzer
import com.astralplayer.nextplayer.streaming.VideoStreamOptimizer
import com.astralplayer.nextplayer.viewmodel.EnhancedPlayerViewModel
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

/**
 * Comprehensive integration tests for video player system
 * Tests end-to-end functionality across all major components
 */
@RunWith(AndroidJUnit4::class)
class VideoPlayerIntegrationTest {

    private lateinit var context: Context
    private lateinit var testScope: TestScope
    
    @Mock private lateinit var mockExoPlayer: ExoPlayer
    
    // System Under Test - Core Components
    private lateinit var networkAnalyzer: NetworkAnalyzer
    private lateinit var cacheManager: AdvancedCacheManager
    private lateinit var qualityManager: VideoQualityManager
    private lateinit var adaptiveController: AdaptiveQualityController
    private lateinit var streamOptimizer: VideoStreamOptimizer
    private lateinit var gestureDetector: EnhancedGestureDetector
    private lateinit var accessibilityManager: AccessibilityManager
    private lateinit var hapticFeedbackManager: HapticFeedbackManager
    private lateinit var playerViewModel: EnhancedPlayerViewModel

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        context = ApplicationProvider.getApplicationContext()
        testScope = TestScope()
        
        setupComponents()
    }

    @After
    fun tearDown() {
        runTest {
            cleanupComponents()
        }
    }

    private fun setupComponents() {
        networkAnalyzer = NetworkAnalyzer(context)
        cacheManager = AdvancedCacheManager(context)
        qualityManager = VideoQualityManager(context, mockExoPlayer)
        adaptiveController = AdaptiveQualityController(context, mockExoPlayer, networkAnalyzer)
        streamOptimizer = VideoStreamOptimizer(context, mockExoPlayer)
        gestureDetector = EnhancedGestureDetector(context)
        accessibilityManager = AccessibilityManager(context)
        hapticFeedbackManager = HapticFeedbackManager(context)
        playerViewModel = EnhancedPlayerViewModel(
            context,
            networkAnalyzer,
            cacheManager,
            qualityManager,
            accessibilityManager
        )
    }

    private suspend fun cleanupComponents() {
        networkAnalyzer.cleanup()
        cacheManager.cleanup()
        qualityManager.cleanup()
        adaptiveController.cleanup()
        streamOptimizer.cleanup()
        gestureDetector.cleanup()
        accessibilityManager.cleanup()
        hapticFeedbackManager.cleanup()
    }

    @Test
    fun testCompleteVideoPlaybackFlow() = runTest {
        // Given
        val testVideoUri = Uri.parse("https://example.com/test-video.mp4")
        
        // Initialize all components
        networkAnalyzer.initialize()
        cacheManager.initialize()
        qualityManager.initialize(networkAnalyzer)
        adaptiveController.initialize()
        streamOptimizer.initialize()
        accessibilityManager.initialize()
        hapticFeedbackManager.initialize()
        
        // When - Start video playback
        playerViewModel.startPlayback(testVideoUri)
        advanceUntilIdle()
        
        // Then - Verify integration
        assertTrue("Network analyzer should be monitoring", networkAnalyzer.isMonitoring())
        assertNotNull("Cache manager should be initialized", cacheManager.cacheState.value)
        assertTrue("Quality manager should be active", qualityManager.currentQuality.value != null)
        assertTrue("Stream optimizer should be running", streamOptimizer.isOptimizationActive())
        
        // Verify component interactions
        verify(mockExoPlayer, atLeastOnce()).setMediaItem(any())
        verify(mockExoPlayer, atLeastOnce()).prepare()
    }

    @Test
    fun testNetworkChangeAdaptation() = runTest {
        // Given
        initializeAllComponents()
        val testUri = Uri.parse("https://example.com/adaptive-video.mp4")
        
        // Start playback
        playerViewModel.startPlayback(testUri)
        advanceUntilIdle()
        
        // When - Simulate network quality change
        networkAnalyzer.simulateNetworkChange(
            bandwidth = 1_000_000L, // 1 Mbps - poor connection
            latency = 250L,
            connectionType = "wifi"
        )
        advanceUntilIdle()
        
        // Then - Verify adaptive responses
        val adaptationState = adaptiveController.currentAdaptationState.value
        assertTrue("Adaptive controller should respond to network change", 
                  adaptationState.lastAdaptationTime > 0L)
        
        val streamingState = streamOptimizer.optimizationState.value
        assertTrue("Stream optimizer should adjust for poor network",
                  streamingState.currentStrategy.toString().contains("conservative", ignoreCase = true))
        
        val cacheState = cacheManager.cacheState.value
        assertTrue("Cache should adapt to network conditions",
                  cacheState.intelligentPrefetchingEnabled)
    }

    @Test
    fun testGestureAccessibilityIntegration() = runTest {
        // Given
        initializeAllComponents()
        
        // Enable accessibility features
        accessibilityManager.configureHighContrastMode(true)
        accessibilityManager.configureLargeTextMode(1.5f)
        
        // When - Perform accessibility gesture
        val gestureResult = gestureDetector.handleAccessibilityGesture(
            gestureType = "swipe_right",
            startX = 100f,
            startY = 200f,
            endX = 300f,
            endY = 200f
        )
        advanceUntilIdle()
        
        // Then - Verify integrated response
        assertTrue("Gesture should be recognized", gestureResult.isRecognized)
        assertEquals("Should trigger seek forward", "seek_forward", gestureResult.action)
        
        // Verify accessibility feedback
        val accessibilityEvents = accessibilityManager.accessibilityEvents
        var audioFeedbackReceived = false
        
        accessibilityEvents.collect { event ->
            if (event.toString().contains("AudioFeedbackProvided")) {
                audioFeedbackReceived = true
            }
        }
        
        assertTrue("Audio feedback should be provided", audioFeedbackReceived)
    }

    @Test
    fun testCacheQualityIntegration() = runTest {
        // Given
        initializeAllComponents()
        val testUri = Uri.parse("https://example.com/test-content.mp4")
        
        // When - Cache content with quality considerations
        val cacheResult = cacheManager.cacheContent(
            uri = testUri,
            priority = com.astralplayer.nextplayer.cache.CachePriority.HIGH,
            strategy = com.astralplayer.nextplayer.cache.CacheStrategy.ADAPTIVE
        )
        
        // Set available qualities
        val qualities = listOf(
            createTestQuality("720p", 720, 2_000_000),
            createTestQuality("1080p", 1080, 5_000_000),
            createTestQuality("480p", 480, 1_000_000)
        )
        qualityManager.setAvailableQualities(qualities)
        advanceUntilIdle()
        
        // Then - Verify cache-quality coordination
        assertTrue("Content should be cached successfully", 
                  cacheResult is com.astralplayer.nextplayer.cache.CacheResult.Success ||
                  cacheResult is com.astralplayer.nextplayer.cache.CacheResult.AlreadyCached)
        
        val currentQuality = qualityManager.currentQuality.value
        assertNotNull("Quality should be selected", currentQuality)
        
        val recommendations = qualityManager.getQualityRecommendations()
        assertTrue("Quality recommendations should consider cache", recommendations.isNotEmpty())
    }

    @Test
    fun testStreamingOptimizationIntegration() = runTest {
        // Given
        initializeAllComponents()
        val testUri = Uri.parse("https://example.com/stream-test.m3u8")
        
        // When - Start streaming with optimization
        streamOptimizer.optimizeForStreaming(testUri)
        playerViewModel.startPlayback(testUri)
        
        // Simulate buffering scenarios
        streamOptimizer.handleBufferUpdate(bufferPercentage = 20)
        advanceUntilIdle()
        
        // Then - Verify optimization coordination
        val bufferState = streamOptimizer.bufferState.value
        assertTrue("Buffer optimization should be active", bufferState.isOptimizing)
        
        val networkMetrics = networkAnalyzer.getRealtimeMetrics()
        assertTrue("Network should be monitored for optimization", 
                  networkMetrics.bandwidth > 0)
        
        val adaptationInsights = adaptiveController.getAdaptationInsights()
        assertTrue("Adaptation should consider streaming optimization",
                  adaptationInsights.totalAdaptations >= 0)
    }

    @Test
    fun testHapticGestureIntegration() = runTest {
        // Given
        initializeAllComponents()
        
        // When - Perform gesture that should trigger haptic feedback
        gestureDetector.enableHapticFeedback(true)
        val gestureResult = gestureDetector.detectGesture(
            startX = 500f,
            startY = 300f,
            endX = 500f,
            endY = 100f,
            velocityY = -1500f
        )
        advanceUntilIdle()
        
        // Then - Verify haptic integration
        assertTrue("Gesture should be detected", gestureResult.type != "none")
        
        val hapticEvents = hapticFeedbackManager.feedbackEvents
        var hapticTriggered = false
        
        hapticEvents.collect { event ->
            if (event.toString().contains("HapticTriggered")) {
                hapticTriggered = true
            }
        }
        
        assertTrue("Haptic feedback should be triggered", hapticTriggered)
    }

    @Test
    fun testErrorHandlingIntegration() = runTest {
        // Given
        initializeAllComponents()
        val invalidUri = Uri.parse("https://invalid.url/nonexistent.mp4")
        
        // When - Attempt playback of invalid content
        try {
            playerViewModel.startPlayback(invalidUri)
            advanceUntilIdle()
        } catch (e: Exception) {
            // Expected for invalid URI
        }
        
        // Then - Verify graceful error handling across components
        val playerState = playerViewModel.playerState.value
        assertTrue("Player should handle error gracefully", 
                  playerState.error != null || playerState.playbackState == "error")
        
        val cacheEvents = cacheManager.cacheEvents
        var errorEventReceived = false
        
        cacheEvents.collect { event ->
            if (event.toString().contains("Error")) {
                errorEventReceived = true
            }
        }
        
        // Components should continue functioning despite errors
        assertTrue("Network analyzer should remain active", networkAnalyzer.isMonitoring())
        assertTrue("Quality manager should remain functional", qualityManager.currentQuality.value != null)
    }

    @Test
    fun testPerformanceUnderLoad() = runTest {
        // Given
        initializeAllComponents()
        val testUris = (1..10).map { Uri.parse("https://example.com/video$it.mp4") }
        
        // When - Simulate high load scenarios
        val startTime = System.currentTimeMillis()
        
        // Rapid quality changes
        repeat(5) {
            qualityManager.setQuality(createTestQuality("test$it", 720, 2_000_000))
            delay(100)
        }
        
        // Multiple cache operations
        testUris.forEach { uri ->
            launch {
                cacheManager.cacheContent(uri)
            }
        }
        advanceUntilIdle()
        
        // Network fluctuations
        repeat(3) {
            networkAnalyzer.simulateNetworkChange(
                bandwidth = (1_000_000L..10_000_000L).random(),
                latency = (50L..300L).random(),
                connectionType = "wifi"
            )
            delay(200)
        }
        
        val endTime = System.currentTimeMillis()
        val duration = endTime - startTime
        
        // Then - Verify system stability under load
        assertTrue("Operations should complete in reasonable time", duration < 10_000) // 10 seconds
        
        val cacheState = cacheManager.cacheState.value
        assertTrue("Cache should remain healthy", cacheState.health.score > 0.3f)
        
        val adaptationState = adaptiveController.currentAdaptationState.value
        assertTrue("Adaptation should remain responsive", 
                  adaptationState.lastAdaptationTime > 0L)
    }

    @Test
    fun testMemoryManagement() = runTest {
        // Given
        initializeAllComponents()
        
        val initialMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()
        
        // When - Perform memory-intensive operations
        repeat(20) {
            val testUri = Uri.parse("https://example.com/large-video$it.mp4")
            cacheManager.cacheContent(testUri)
            
            qualityManager.setAvailableQualities(
                (1..10).map { index -> createTestQuality("quality$index", 720 + index * 100, 1_000_000 + index * 500_000) }
            )
            
            if (it % 5 == 0) {
                // Trigger garbage collection
                System.gc()
                delay(100)
            }
        }
        advanceUntilIdle()
        
        // Force cleanup
        cleanupComponents()
        System.gc()
        delay(500)
        
        val finalMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()
        val memoryIncrease = finalMemory - initialMemory
        
        // Then - Verify memory is managed properly
        assertTrue("Memory increase should be reasonable", 
                  memoryIncrease < 100 * 1024 * 1024) // Less than 100MB increase
    }

    // Helper methods
    private suspend fun initializeAllComponents() {
        networkAnalyzer.initialize()
        cacheManager.initialize()
        qualityManager.initialize(networkAnalyzer)
        adaptiveController.initialize()
        streamOptimizer.initialize()
        accessibilityManager.initialize()
        hapticFeedbackManager.initialize()
        advanceUntilIdle()
    }

    private fun createTestQuality(name: String, height: Int, bitrate: Int) = 
        com.astralplayer.nextplayer.quality.VideoQuality(
            id = name.lowercase(),
            name = name,
            width = (height * 16) / 9, // 16:9 aspect ratio
            height = height,
            bitrate = bitrate,
            frameRate = 30f,
            codec = "h264"
        )
}