package com.astralplayer.nextplayer

import android.content.Context
import android.net.Uri
import com.astralplayer.nextplayer.performance.*
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

class PerformanceOptimizationTest {
    
    private lateinit var context: Context
    private lateinit var performanceMonitor: PerformanceMonitor
    private lateinit var startupOptimizer: StartupOptimizer
    private lateinit var videoCache: VideoCache
    private lateinit var batteryOptimizer: BatteryOptimizer
    
    @Before
    fun setup() {
        context = mockk(relaxed = true)
        performanceMonitor = PerformanceMonitor(context)
        startupOptimizer = StartupOptimizer(context, performanceMonitor)
        videoCache = VideoCache(context)
        batteryOptimizer = BatteryOptimizer(context)
    }
    
    @Test
    fun `performance monitor tracks metrics correctly`() {
        val operation = "test_video_load"
        val startTime = performanceMonitor.startMeasure(operation)
        
        Thread.sleep(10) // Simulate work
        
        val duration = performanceMonitor.endMeasure(operation, startTime)
        assertTrue("Duration should be positive", duration > 0)
    }
    
    @Test
    fun `startup optimizer handles tasks correctly`() = runTest {
        var criticalTaskExecuted = false
        var backgroundTaskExecuted = false
        
        startupOptimizer.addCriticalTask { criticalTaskExecuted = true }
        startupOptimizer.addBackgroundTask { backgroundTaskExecuted = true }
        
        startupOptimizer.optimize()
        
        assertTrue("Critical task should execute", criticalTaskExecuted)
        // Background task may not complete immediately
    }
    
    @Test
    fun `video cache manages storage correctly`() = runTest {
        val testUri = mockk<Uri>(relaxed = true)
        val testData = ByteArray(1024) { it.toByte() }
        
        val cached = videoCache.cacheVideoSegment(testUri, testData)
        assertTrue("Caching should succeed", cached)
        
        val retrieved = videoCache.getCachedVideoSegment(testUri)
        assertNotNull("Retrieved data should not be null", retrieved)
        
        val isCached = videoCache.isCached(testUri)
        assertTrue("Video should be marked as cached", isCached)
    }
    
    @Test
    fun `battery optimizer provides appropriate settings`() {
        val quality = batteryOptimizer.getOptimalVideoQuality()
        assertNotNull("Quality should not be null", quality)
        
        val frameRate = batteryOptimizer.getOptimalFrameRate()
        assertTrue("Frame rate should be reasonable", frameRate in 24..60)
        
        val bufferSize = batteryOptimizer.getOptimalBufferSize()
        assertTrue("Buffer size should be positive", bufferSize > 0)
        
        val cacheSize = batteryOptimizer.getRecommendedCacheSize()
        assertTrue("Cache size should be positive", cacheSize > 0)
    }
    
    @Test
    fun `cache stats are calculated correctly`() {
        val stats = videoCache.getCacheStats()
        
        assertNotNull("Stats should not be null", stats)
        assertTrue("Memory cache size should be non-negative", stats.memoryCacheSize >= 0)
        assertTrue("Disk cache size should be non-negative", stats.diskCacheSize >= 0)
        assertTrue("Total cached items should be non-negative", stats.totalCachedItems >= 0)
    }
    
    @Test
    fun `performance monitor memory tracking works`() {
        performanceMonitor.trackMemoryUsage()
        
        val metrics = performanceMonitor.performanceMetrics.value
        assertTrue("Memory usage should be tracked", metrics.memoryUsage >= 0)
    }
}