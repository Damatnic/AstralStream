package com.astralplayer.nextplayer.integration

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.media3.exoplayer.ExoPlayer
import com.astralplayer.nextplayer.quality.VideoQualityManager
import com.astralplayer.nextplayer.quality.AdaptiveQualityController
import com.astralplayer.nextplayer.quality.VideoQuality
import com.astralplayer.nextplayer.streaming.NetworkAnalyzer
import com.astralplayer.nextplayer.streaming.NetworkInfo
import com.astralplayer.nextplayer.streaming.ConnectionQuality
import com.astralplayer.nextplayer.streaming.VideoStreamOptimizer
import com.astralplayer.nextplayer.cache.AdvancedCacheManager
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
 * Integration tests for network-aware quality adaptation
 * Tests coordination between NetworkAnalyzer, QualityManager, and AdaptiveQualityController
 */
@RunWith(AndroidJUnit4::class)
class NetworkQualityIntegrationTest {

    private lateinit var context: Context
    private lateinit var testScope: TestScope
    
    @Mock private lateinit var mockExoPlayer: ExoPlayer
    
    private lateinit var networkAnalyzer: NetworkAnalyzer
    private lateinit var qualityManager: VideoQualityManager
    private lateinit var adaptiveController: AdaptiveQualityController
    private lateinit var streamOptimizer: VideoStreamOptimizer
    private lateinit var cacheManager: AdvancedCacheManager

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        context = ApplicationProvider.getApplicationContext()
        testScope = TestScope()
        
        networkAnalyzer = NetworkAnalyzer(context)
        qualityManager = VideoQualityManager(context, mockExoPlayer)
        adaptiveController = AdaptiveQualityController(context, mockExoPlayer, networkAnalyzer)
        streamOptimizer = VideoStreamOptimizer(context, mockExoPlayer)
        cacheManager = AdvancedCacheManager(context)
    }

    @After
    fun tearDown() {
        runTest {
            networkAnalyzer.cleanup()
            qualityManager.cleanup()
            adaptiveController.cleanup()
            streamOptimizer.cleanup()
            cacheManager.cleanup()
        }
    }

    @Test
    fun testHighQualityNetworkAdaptation() = runTest {
        // Given - Excellent network conditions
        initializeComponents()
        setNetworkConditions(
            bandwidth = 10_000_000L, // 10 Mbps
            latency = 20L,
            connectionQuality = ConnectionQuality.EXCELLENT
        )
        
        // When - System adapts to network
        advanceUntilIdle()
        
        // Then - Should select high quality
        val currentQuality = qualityManager.currentQuality.value
        assertNotNull("Quality should be selected", currentQuality)
        assertTrue("Should select high quality for excellent network", 
                  currentQuality!!.bitrate >= 5_000_000)
        
        val adaptationInsights = adaptiveController.getAdaptationInsights()
        assertEquals("Should use aggressive strategy", 0.8f, 
                    adaptationInsights.adaptationAccuracy, 0.2f)
    }

    @Test
    fun testPoorNetworkAdaptation() = runTest {
        // Given - Poor network conditions
        initializeComponents()
        setNetworkConditions(
            bandwidth = 500_000L, // 500 kbps
            latency = 300L,
            connectionQuality = ConnectionQuality.POOR
        )
        
        // When - System adapts to network
        advanceUntilIdle()
        
        // Then - Should select low quality
        val currentQuality = qualityManager.currentQuality.value
        assertNotNull("Quality should be selected", currentQuality)
        assertTrue("Should select low quality for poor network", 
                  currentQuality!!.bitrate <= 1_000_000)
        
        val streamingState = streamOptimizer.optimizationState.value
        assertTrue("Should use conservative streaming strategy",
                  streamingState.currentStrategy.toString().contains("conservative", ignoreCase = true))
    }

    @Test
    fun testNetworkFluctuationHandling() = runTest {
        // Given - Initial good network
        initializeComponents()
        setNetworkConditions(
            bandwidth = 5_000_000L,
            latency = 50L,
            connectionQuality = ConnectionQuality.GOOD
        )
        advanceUntilIdle()
        
        val initialQuality = qualityManager.currentQuality.value
        assertNotNull("Initial quality should be set", initialQuality)
        
        // When - Network degrades
        setNetworkConditions(
            bandwidth = 800_000L,
            latency = 200L,
            connectionQuality = ConnectionQuality.POOR
        )
        advanceUntilIdle()
        
        val degradedQuality = qualityManager.currentQuality.value
        assertNotNull("Quality should adapt to degraded network", degradedQuality)
        assertTrue("Should downgrade quality", 
                  degradedQuality!!.bitrate < initialQuality!!.bitrate)
        
        // When - Network improves again
        setNetworkConditions(
            bandwidth = 8_000_000L,
            latency = 30L,
            connectionQuality = ConnectionQuality.EXCELLENT
        )
        advanceUntilIdle()
        
        val improvedQuality = qualityManager.currentQuality.value
        assertNotNull("Quality should adapt to improved network", improvedQuality)
        assertTrue("Should upgrade quality gradually", 
                  improvedQuality!!.bitrate > degradedQuality.bitrate)
    }

    @Test
    fun testAdaptiveStreamingIntegration() = runTest {
        // Given - Streaming scenario
        initializeComponents()
        val streamUri = Uri.parse("https://example.com/adaptive-stream.m3u8")
        
        // Start streaming optimization
        streamOptimizer.optimizeForStreaming(streamUri)
        
        // When - Network changes during streaming
        setNetworkConditions(
            bandwidth = 3_000_000L,
            latency = 80L,
            connectionQuality = ConnectionQuality.GOOD
        )
        advanceUntilIdle()
        
        // Simulate buffer status changes
        streamOptimizer.handleBufferUpdate(bufferPercentage = 30)
        advanceUntilIdle()
        
        // Then - Should coordinate optimization
        val bufferState = streamOptimizer.bufferState.value
        assertTrue("Buffer optimization should be active", bufferState.isOptimizing)
        
        val adaptationState = adaptiveController.currentAdaptationState.value
        assertTrue("Should adapt based on buffer health",
                  adaptationState.lastAdaptationTime > 0L)
        
        val currentQuality = qualityManager.currentQuality.value
        assertNotNull("Quality should be optimized for streaming", currentQuality)
    }

    @Test
    fun testCacheNetworkCoordination() = runTest {
        // Given - Content caching scenario
        initializeComponents()
        val testUri = Uri.parse("https://example.com/content.mp4")
        
        // When - Cache with network considerations
        setNetworkConditions(
            bandwidth = 2_000_000L,
            latency = 100L,
            connectionQuality = ConnectionQuality.GOOD
        )
        
        val cacheResult = cacheManager.cacheContent(
            uri = testUri,
            strategy = com.astralplayer.nextplayer.cache.CacheStrategy.ADAPTIVE
        )
        advanceUntilIdle()
        
        // Then - Should coordinate with quality selection
        assertTrue("Content should be cached successfully",
                  cacheResult is com.astralplayer.nextplayer.cache.CacheResult.Success ||
                  cacheResult is com.astralplayer.nextplayer.cache.CacheResult.AlreadyCached)
        
        val currentQuality = qualityManager.currentQuality.value
        assertNotNull("Quality should consider network for caching", currentQuality)
        
        val recommendations = qualityManager.getQualityRecommendations()
        assertTrue("Recommendations should factor in network", recommendations.isNotEmpty())
        
        val networkMetrics = networkAnalyzer.getRealtimeMetrics()
        assertTrue("Network metrics should influence caching", 
                  networkMetrics.bandwidth == 2_000_000L)
    }

    @Test
    fun testMobileNetworkAdaptation() = runTest {
        // Given - Mobile network conditions
        initializeComponents()
        setNetworkConditions(
            bandwidth = 1_500_000L, // Typical mobile
            latency = 150L,
            connectionQuality = ConnectionQuality.FAIR,
            connectionType = "cellular"
        )
        
        // When - Adapt for mobile
        advanceUntilIdle()
        
        // Then - Should use mobile-optimized settings
        val currentQuality = qualityManager.currentQuality.value
        assertNotNull("Quality should be selected for mobile", currentQuality)
        assertTrue("Should prefer lower bitrate for mobile",
                  currentQuality!!.bitrate <= 2_000_000)
        
        val cacheState = cacheManager.cacheState.value
        assertTrue("Should enable intelligent prefetching for mobile",
                  cacheState.intelligentPrefetchingEnabled)
        
        val streamingState = streamOptimizer.optimizationState.value
        assertTrue("Should use mobile-optimized streaming",
                  streamingState.adaptiveBitrateEnabled)
    }

    @Test
    fun testWiFiNetworkOptimization() = runTest {
        // Given - WiFi network conditions
        initializeComponents()
        setNetworkConditions(
            bandwidth = 25_000_000L, // High-speed WiFi
            latency = 10L,
            connectionQuality = ConnectionQuality.EXCELLENT,
            connectionType = "wifi"
        )
        
        // When - Optimize for WiFi
        advanceUntilIdle()
        
        // Then - Should use WiFi-optimized settings
        val currentQuality = qualityManager.currentQuality.value
        assertNotNull("Quality should be selected for WiFi", currentQuality)
        assertTrue("Should select high quality for WiFi",
                  currentQuality!!.bitrate >= 4_000_000)
        
        val adaptationInsights = adaptiveController.getAdaptationInsights()
        assertTrue("Should use aggressive adaptation for WiFi",
                  adaptationInsights.adaptationAccuracy > 0.7f)
        
        val streamingState = streamOptimizer.optimizationState.value
        assertTrue("Should enable high-quality streaming for WiFi",
                  streamingState.qualityOptimizationEnabled)
    }

    @Test
    fun testNetworkTimeoutHandling() = runTest {
        // Given - Network with intermittent connectivity
        initializeComponents()
        
        // When - Simulate network timeout
        networkAnalyzer.simulateNetworkTimeout(duration = 5000L)
        advanceUntilIdle()
        
        // Then - Should handle gracefully
        val adaptationState = adaptiveController.currentAdaptationState.value
        assertTrue("Should maintain last known state during timeout",
                  adaptationState.strategy != null)
        
        val cacheState = cacheManager.cacheState.value
        assertTrue("Cache should remain functional during timeout",
                  cacheState.health.score > 0.2f)
        
        // When - Network recovers
        setNetworkConditions(
            bandwidth = 5_000_000L,
            latency = 50L,
            connectionQuality = ConnectionQuality.GOOD
        )
        advanceUntilIdle()
        
        // Then - Should resume normal operation
        val recoveredQuality = qualityManager.currentQuality.value
        assertNotNull("Should resume quality management after recovery", recoveredQuality)
    }

    @Test
    fun testBandwidthEstimationAccuracy() = runTest {
        // Given - Known network conditions
        initializeComponents()
        val expectedBandwidth = 3_500_000L
        
        setNetworkConditions(
            bandwidth = expectedBandwidth,
            latency = 75L,
            connectionQuality = ConnectionQuality.GOOD
        )
        
        // When - Measure bandwidth over time
        repeat(10) {
            advanceTimeBy(1000) // 1 second intervals
            networkAnalyzer.updateNetworkMetrics()
        }
        
        val metrics = networkAnalyzer.getRealtimeMetrics()
        
        // Then - Should accurately estimate bandwidth
        val estimationError = kotlin.math.abs(metrics.bandwidth - expectedBandwidth).toFloat() / expectedBandwidth
        assertTrue("Bandwidth estimation should be accurate within 10%",
                  estimationError < 0.1f)
        
        val currentQuality = qualityManager.currentQuality.value
        assertNotNull("Quality should be based on accurate bandwidth", currentQuality)
        assertTrue("Quality bitrate should be within bandwidth limits",
                  currentQuality!!.bitrate <= metrics.bandwidth * 0.8f) // 80% utilization
    }

    // Helper methods
    private suspend fun initializeComponents() {
        networkAnalyzer.initialize()
        qualityManager.initialize(networkAnalyzer)
        adaptiveController.initialize()
        streamOptimizer.initialize()
        cacheManager.initialize()
        
        // Set up available qualities
        val qualities = listOf(
            createQuality("240p", 240, 400_000),
            createQuality("360p", 360, 800_000),
            createQuality("480p", 480, 1_200_000),
            createQuality("720p", 720, 2_500_000),
            createQuality("1080p", 1080, 5_000_000),
            createQuality("1440p", 1440, 8_000_000)
        )
        qualityManager.setAvailableQualities(qualities)
        
        advanceUntilIdle()
    }

    private suspend fun setNetworkConditions(
        bandwidth: Long,
        latency: Long,
        connectionQuality: ConnectionQuality,
        connectionType: String = "wifi"
    ) {
        networkAnalyzer.simulateNetworkChange(
            bandwidth = bandwidth,
            latency = latency,
            connectionType = connectionType
        )
        
        // Allow time for adaptation
        advanceTimeBy(2000) // 2 seconds
    }

    private fun createQuality(name: String, height: Int, bitrate: Int) = VideoQuality(
        id = name.lowercase(),
        name = name,
        width = (height * 16) / 9,
        height = height,
        bitrate = bitrate,
        frameRate = 30f,
        codec = "h264"
    )
}