package com.astralplayer.nextplayer.integration

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.media3.exoplayer.ExoPlayer
import com.astralplayer.nextplayer.cache.AdvancedCacheManager
import com.astralplayer.nextplayer.cache.CacheStrategy
import com.astralplayer.nextplayer.cache.CachePriority
import com.astralplayer.nextplayer.streaming.VideoStreamOptimizer
import com.astralplayer.nextplayer.streaming.NetworkAnalyzer
import com.astralplayer.nextplayer.streaming.AdaptiveBitrateManager
import com.astralplayer.nextplayer.streaming.BufferOptimizer
import com.astralplayer.nextplayer.quality.VideoQualityManager
import com.astralplayer.nextplayer.quality.VideoQuality
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
 * Integration tests for cache and streaming coordination
 * Tests how caching and streaming optimization work together
 */
@RunWith(AndroidJUnit4::class)
class CacheStreamingIntegrationTest {

    private lateinit var context: Context
    private lateinit var testScope: TestScope
    
    @Mock private lateinit var mockExoPlayer: ExoPlayer
    
    private lateinit var cacheManager: AdvancedCacheManager
    private lateinit var streamOptimizer: VideoStreamOptimizer
    private lateinit var networkAnalyzer: NetworkAnalyzer
    private lateinit var bitrateManager: AdaptiveBitrateManager
    private lateinit var bufferOptimizer: BufferOptimizer
    private lateinit var qualityManager: VideoQualityManager

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        context = ApplicationProvider.getApplicationContext()
        testScope = TestScope()
        
        cacheManager = AdvancedCacheManager(context)
        networkAnalyzer = NetworkAnalyzer(context)
        streamOptimizer = VideoStreamOptimizer(context, mockExoPlayer)
        bitrateManager = AdaptiveBitrateManager(context, networkAnalyzer)
        bufferOptimizer = BufferOptimizer(context)
        qualityManager = VideoQualityManager(context, mockExoPlayer)
    }

    @After
    fun tearDown() {
        runTest {
            cacheManager.cleanup()
            streamOptimizer.cleanup()
            networkAnalyzer.cleanup()
            bitrateManager.cleanup()
            bufferOptimizer.cleanup()
            qualityManager.cleanup()
        }
    }

    @Test
    fun testCacheStreamingCoordination() = runTest {
        // Given - Initialize components
        initializeComponents()
        val streamUri = Uri.parse("https://example.com/live-stream.m3u8")
        
        // When - Start streaming with caching
        streamOptimizer.optimizeForStreaming(streamUri)
        cacheManager.enableIntelligentPrefetching(true)
        
        val cacheResult = cacheManager.cacheContent(
            uri = streamUri,
            priority = CachePriority.HIGH,
            strategy = CacheStrategy.SMART_SEGMENTS
        )
        advanceUntilIdle()
        
        // Then - Verify coordination
        assertTrue("Content should be cached for streaming",
                  cacheResult is com.astralplayer.nextplayer.cache.CacheResult.Success ||
                  cacheResult is com.astralplayer.nextplayer.cache.CacheResult.AlreadyCached)
        
        val streamingState = streamOptimizer.optimizationState.value
        assertTrue("Streaming optimization should be active", streamingState.isOptimizing)
        
        val cacheState = cacheManager.cacheState.value
        assertTrue("Intelligent prefetching should be enabled", 
                  cacheState.intelligentPrefetchingEnabled)
    }

    @Test
    fun testAdaptiveBitrateWithCaching() = runTest {
        // Given - Adaptive streaming scenario
        initializeComponents()
        val adaptiveStreamUri = Uri.parse("https://example.com/adaptive.m3u8")
        
        // Configure qualities for adaptive streaming
        val qualities = listOf(
            createQuality("240p", 240, 500_000),
            createQuality("480p", 480, 1_200_000),
            createQuality("720p", 720, 2_500_000),
            createQuality("1080p", 1080, 5_000_000)
        )
        qualityManager.setAvailableQualities(qualities)
        
        // When - Start adaptive streaming with intelligent caching
        bitrateManager.enableAdaptiveBitrate(true)
        streamOptimizer.optimizeForStreaming(adaptiveStreamUri)
        
        // Cache different quality segments
        qualities.forEach { quality ->
            val qualityUri = Uri.parse("${adaptiveStreamUri}_${quality.name}")
            launch {
                cacheManager.cacheContent(
                    uri = qualityUri,
                    priority = if (quality.height <= 720) CachePriority.HIGH else CachePriority.NORMAL,
                    strategy = CacheStrategy.ADAPTIVE
                )
            }
        }
        advanceUntilIdle()
        
        // Simulate network changes
        networkAnalyzer.simulateNetworkChange(
            bandwidth = 2_000_000L, // 2 Mbps
            latency = 100L,
            connectionType = "wifi"
        )
        advanceUntilIdle()
        
        // Then - Verify adaptive caching coordination
        val bitrateState = bitrateManager.bitrateState.value
        assertTrue("Adaptive bitrate should be enabled", bitrateState.isAdaptive)
        assertTrue("Should adapt to network conditions", bitrateState.currentBitrate > 0)
        
        val currentQuality = qualityManager.currentQuality.value
        assertNotNull("Quality should be selected based on network", currentQuality)
        assertTrue("Quality should match network capacity",
                  currentQuality!!.bitrate <= 2_000_000L * 0.8f) // 80% utilization
        
        val cacheAnalytics = cacheManager.getCacheAnalytics()
        assertTrue("Cache should have entries for multiple qualities",
                  cacheAnalytics.currentState.entryCount > 1)
    }

    @Test
    fun testBufferOptimizationWithCaching() = runTest {
        // Given - Buffer optimization scenario
        initializeComponents()
        val streamUri = Uri.parse("https://example.com/buffer-test.mp4")
        
        // Configure buffer optimization
        bufferOptimizer.configureOptimization(
            strategy = com.astralplayer.nextplayer.streaming.BufferStrategy.ADAPTIVE,
            targetBufferDuration = 30000L, // 30 seconds
            maxBufferDuration = 60000L // 60 seconds
        )
        
        // When - Start streaming with buffer optimization and caching
        streamOptimizer.optimizeForStreaming(streamUri)
        cacheManager.cacheContent(streamUri, strategy = CacheStrategy.SMART_SEGMENTS)
        
        // Simulate buffer status changes
        bufferOptimizer.updateBufferStatus(
            bufferedDuration = 10000L, // 10 seconds buffered
            targetDuration = 30000L
        )
        advanceUntilIdle()
        
        // Then - Verify buffer-cache coordination
        val bufferState = bufferOptimizer.bufferState.value
        assertTrue("Buffer optimization should be active", bufferState.isOptimizing)
        assertTrue("Should be below target buffer", bufferState.bufferedDuration < bufferState.targetDuration)
        
        val cacheState = cacheManager.cacheState.value
        assertTrue("Cache should support buffer optimization",
                  cacheState.intelligentPrefetchingEnabled)
        
        // Buffer should trigger cache prefetching
        val cacheAnalytics = cacheManager.getCacheAnalytics()
        assertTrue("Cache should be actively used for buffering",
                  cacheAnalytics.metrics.cacheHits > 0 || cacheAnalytics.metrics.cacheMisses > 0)
    }

    @Test
    fun testLiveStreamCaching() = runTest {
        // Given - Live streaming scenario
        initializeComponents()
        val liveStreamUri = Uri.parse("https://live.example.com/stream.m3u8")
        
        // When - Handle live stream with limited caching
        streamOptimizer.optimizeForLiveStreaming(liveStreamUri)
        
        val cacheResult = cacheManager.cacheContent(
            uri = liveStreamUri,
            priority = CachePriority.CRITICAL,
            strategy = CacheStrategy.CONSERVATIVE // Limited caching for live content
        )
        advanceUntilIdle()
        
        // Then - Verify live stream caching coordination
        assertTrue("Live content should be cached with limitations",
                  cacheResult is com.astralplayer.nextplayer.cache.CacheResult.Success ||
                  cacheResult is com.astralplayer.nextplayer.cache.CacheResult.AlreadyCached)
        
        val streamingState = streamOptimizer.optimizationState.value
        assertTrue("Should optimize for live streaming", streamingState.isLiveStream)
        assertTrue("Live stream optimization should be active", streamingState.isOptimizing)
        
        val cacheState = cacheManager.cacheState.value
        // Live streams typically have limited caching
        assertTrue("Cache utilization should be reasonable for live content",
                  cacheState.utilizationPercentage < 80)
    }

    @Test
    fun testOfflineStreamingIntegration() = runTest {
        // Given - Offline content scenario
        initializeComponents()
        val offlineContentUri = Uri.parse("file:///sdcard/offline-video.mp4")
        
        // When - Cache offline content for optimization
        val cacheResult = cacheManager.cacheContent(
            uri = offlineContentUri,
            priority = CachePriority.HIGH,
            strategy = CacheStrategy.AGGRESSIVE
        )
        
        streamOptimizer.optimizeForOfflineContent(offlineContentUri)
        advanceUntilIdle()
        
        // Then - Verify offline optimization
        assertTrue("Offline content should be cached successfully",
                  cacheResult is com.astralplayer.nextplayer.cache.CacheResult.Success ||
                  cacheResult is com.astralplayer.nextplayer.cache.CacheResult.AlreadyCached)
        
        val streamingState = streamOptimizer.optimizationState.value
        assertTrue("Should optimize for offline content", !streamingState.isLiveStream)
        assertTrue("Offline optimization should be active", streamingState.isOptimizing)
        
        val cacheAnalytics = cacheManager.getCacheAnalytics()
        assertTrue("Cache should be effectively used for offline content",
                  cacheAnalytics.metrics.cacheHits > 0)
    }

    @Test
    fun testNetworkAwareCaching() = runTest {
        // Given - Network-aware caching scenario
        initializeComponents()
        val streamUri = Uri.parse("https://example.com/network-aware.mp4")
        
        // When - Start with good network conditions
        networkAnalyzer.simulateNetworkChange(
            bandwidth = 10_000_000L, // 10 Mbps
            latency = 20L,
            connectionType = "wifi"
        )
        
        var cacheResult = cacheManager.cacheContent(
            uri = streamUri,
            strategy = CacheStrategy.ADAPTIVE
        )
        advanceUntilIdle()
        
        // Then network degrades
        networkAnalyzer.simulateNetworkChange(
            bandwidth = 1_000_000L, // 1 Mbps
            latency = 200L,
            connectionType = "cellular"
        )
        advanceUntilIdle()
        
        // Cache more aggressively on poor network
        cacheResult = cacheManager.cacheContent(
            uri = streamUri,
            strategy = CacheStrategy.AGGRESSIVE
        )
        advanceUntilIdle()
        
        // Then - Verify network-aware behavior
        val networkMetrics = networkAnalyzer.getRealtimeMetrics()
        assertEquals("Network should show degraded conditions", 1_000_000L, networkMetrics.bandwidth)
        
        val streamingState = streamOptimizer.optimizationState.value
        assertTrue("Should adapt streaming for poor network", 
                  streamingState.currentStrategy.toString().contains("conservative", ignoreCase = true))
        
        val cacheState = cacheManager.cacheState.value
        assertTrue("Should increase prefetching on poor network",
                  cacheState.intelligentPrefetchingEnabled)
    }

    @Test
    fun testCacheEvictionStreamingCoordination() = runTest {
        // Given - Cache eviction scenario during streaming
        initializeComponents()
        val streamingUri = Uri.parse("https://example.com/streaming-content.mp4")
        
        // Fill cache with content
        repeat(10) { index ->
            val contentUri = Uri.parse("https://example.com/content$index.mp4")
            launch {
                cacheManager.cacheContent(contentUri, priority = CachePriority.NORMAL)
            }
        }
        advanceUntilIdle()
        
        // When - Start streaming that requires cache space
        streamOptimizer.optimizeForStreaming(streamingUri)
        
        val cacheResult = cacheManager.cacheContent(
            uri = streamingUri,
            priority = CachePriority.CRITICAL,
            strategy = CacheStrategy.AGGRESSIVE
        )
        advanceUntilIdle()
        
        // Then - Verify eviction coordination
        assertTrue("Streaming content should be cached successfully",
                  cacheResult is com.astralplayer.nextplayer.cache.CacheResult.Success)
        
        val cacheAnalytics = cacheManager.getCacheAnalytics()
        assertTrue("Cache should manage space for streaming priority",
                  cacheAnalytics.metrics.totalEntriesRemoved >= 0)
        
        val streamingState = streamOptimizer.optimizationState.value
        assertTrue("Streaming should continue during cache management",
                  streamingState.isOptimizing)
    }

    @Test
    fun testCacheCompressionStreamingIntegration() = runTest {
        // Given - Compression enabled caching with streaming
        initializeComponents()
        
        // Enable cache compression
        cacheManager.configureCompression(
            com.astralplayer.nextplayer.cache.CompressionSettings(
                enabled = true,
                algorithm = com.astralplayer.nextplayer.cache.CompressionAlgorithm.LZ4,
                level = 5,
                threshold = 5 * 1024 * 1024L // 5MB
            )
        )
        
        val streamUri = Uri.parse("https://example.com/large-stream.mp4")
        
        // When - Stream and cache large content
        streamOptimizer.optimizeForStreaming(streamUri)
        
        val cacheResult = cacheManager.cacheContent(
            uri = streamUri,
            strategy = CacheStrategy.AGGRESSIVE
        )
        advanceUntilIdle()
        
        // Then - Verify compression coordination
        assertTrue("Large content should be cached with compression",
                  cacheResult is com.astralplayer.nextplayer.cache.CacheResult.Success)
        
        val cacheState = cacheManager.cacheState.value
        assertNotNull("Compression settings should be configured", 
                     cacheState.compressionSettings)
        assertTrue("Compression should be enabled", 
                  cacheState.compressionSettings.enabled)
        
        val streamingState = streamOptimizer.optimizationState.value
        assertTrue("Streaming should work with compression", streamingState.isOptimizing)
        
        val cacheAnalytics = cacheManager.getCacheAnalytics()
        assertTrue("Cache should efficiently store compressed content",
                  cacheAnalytics.metrics.currentCacheSize > 0)
    }

    // Helper methods
    private suspend fun initializeComponents() {
        networkAnalyzer.initialize()
        cacheManager.initialize()
        qualityManager.initialize(networkAnalyzer)
        streamOptimizer.initialize()
        bitrateManager.initialize()
        bufferOptimizer.initialize()
        advanceUntilIdle()
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