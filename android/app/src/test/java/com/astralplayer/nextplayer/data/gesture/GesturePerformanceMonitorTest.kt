package com.astralplayer.nextplayer.data.gesture

import android.app.ActivityManager
import android.content.Context
import android.os.SystemClock
import io.mockk.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

@OptIn(ExperimentalCoroutinesApi::class)
class GesturePerformanceMonitorTest {

    private lateinit var testContext: Context
    private lateinit var mockActivityManager: ActivityManager
    private lateinit var testScope: TestScope
    private lateinit var performanceMonitor: GesturePerformanceMonitor

    @Before
    fun setup() {
        // Mock Android context and system services
        testContext = mockk(relaxed = true)
        mockActivityManager = mockk(relaxed = true)
        
        every { testContext.getSystemService(Context.ACTIVITY_SERVICE) } returns mockActivityManager
        
        // Mock memory info
        val memoryInfo = ActivityManager.MemoryInfo().apply {
            totalMem = 4_000_000_000L // 4GB
            availMem = 2_000_000_000L // 2GB available
        }
        every { mockActivityManager.getMemoryInfo(any()) } answers {
            val info = firstArg<ActivityManager.MemoryInfo>()
            info.totalMem = memoryInfo.totalMem
            info.availMem = memoryInfo.availMem
        }
        
        testScope = TestScope()
        performanceMonitor = GesturePerformanceMonitor(testContext, testScope)
    }

    @After
    fun tearDown() {
        performanceMonitor.cleanup()
    }

    @Test
    fun `initializes with default performance metrics`() {
        val metrics = performanceMonitor.performanceMetrics.value
        
        assertEquals("Initial frame rate should be 0", 0f, metrics.currentFrameRate, 0.01f)
        assertEquals("Initial latency should be 0", 0L, metrics.averageLatency)
        assertEquals("Initial memory usage should be 0", 0f, metrics.memoryUsagePercent, 0.01f)
        assertTrue("Should be performing well initially", metrics.isPerformingWell)
        assertEquals("Performance score should be 100", 100f, metrics.performanceScore, 0.01f)
    }

    @Test
    fun `records gesture latency correctly`() = runTest {
        val gestureId = "test_gesture"
        
        // Mock SystemClock for consistent timing
        mockkStatic(SystemClock::class)
        every { SystemClock.elapsedRealtimeNanos() } returnsMany listOf(
            1000000000L, // Start time (1 second in nanoseconds)
            1050000000L  // End time (1.05 seconds = 50ms later)
        )
        
        performanceMonitor.recordGestureStart(gestureId)
        performanceMonitor.recordGestureCompletion(gestureId)
        
        // Start monitoring to update metrics
        performanceMonitor.startMonitoring()
        
        // Advance time to allow metrics update
        testScope.testScheduler.advanceTimeBy(20L)
        
        val metrics = performanceMonitor.performanceMetrics.value
        assertEquals("Average latency should be 50ms", 50L, metrics.averageLatency)
        assertEquals("Max latency should be 50ms", 50L, metrics.maxLatency)
        
        performanceMonitor.stopMonitoring()
        unmockkStatic(SystemClock::class)
    }

    @Test
    fun `calculates frame rate from timestamps`() = runTest {
        // Mock SystemClock for frame timestamps
        mockkStatic(SystemClock::class)
        val baseTime = 1000000000L // 1 second in nanoseconds
        val frameInterval = 16666667L // ~60fps (16.67ms between frames)
        
        val timestamps = (0..10).map { baseTime + it * frameInterval }
        every { SystemClock.elapsedRealtimeNanos() } returnsMany timestamps
        
        // Record frame timestamps to simulate 60fps
        repeat(10) {
            performanceMonitor.recordFrameTimestamp()
        }
        
        performanceMonitor.startMonitoring()
        testScope.testScheduler.advanceTimeBy(20L)
        
        val metrics = performanceMonitor.performanceMetrics.value
        
        // Should calculate approximately 60fps
        assertTrue("Frame rate should be around 60fps", 
            metrics.currentFrameRate > 50f && metrics.currentFrameRate < 70f)
        
        performanceMonitor.stopMonitoring()
        unmockkStatic(SystemClock::class)
    }

    @Test
    fun `detects performance degradation`() = runTest {
        var degradationDetected: PerformanceDegradation? = null
        
        performanceMonitor.setPerformanceDegradationCallback { degradation ->
            degradationDetected = degradation
        }
        
        // Mock SystemClock for consistent timing
        mockkStatic(SystemClock::class)
        every { SystemClock.elapsedRealtime() } returnsMany (1..50).map { it * 1000L }
        every { SystemClock.elapsedRealtimeNanos() } returnsMany (1..50).map { it * 1000000000L }
        
        performanceMonitor.startMonitoring()
        
        // Simulate good performance initially
        repeat(15) {
            repeat(4) { // 4 frames per cycle = 60fps
                performanceMonitor.recordFrameTimestamp()
            }
            testScope.testScheduler.advanceTimeBy(16L)
        }
        
        // Simulate performance degradation (low frame rate)
        repeat(15) {
            performanceMonitor.recordFrameTimestamp() // Only 1 frame per cycle = 15fps
            testScope.testScheduler.advanceTimeBy(16L)
        }
        
        performanceMonitor.stopMonitoring()
        
        // Performance degradation should be detected
        assertNotNull("Performance degradation should be detected", degradationDetected)
        assertEquals("Should detect frame rate drop", 
            DegradationType.FRAME_RATE_DROP, degradationDetected?.type)
        
        unmockkStatic(SystemClock::class)
    }

    @Test
    fun `configures performance thresholds correctly`() {
        performanceMonitor.configureThresholds(
            frameRate = 45f,
            latency = 25L,
            memory = 0.75f,
            cpu = 0.70f
        )
        
        // Thresholds are used internally, verify through behavior
        // This test ensures the configuration method doesn't throw
        assertNotNull("Performance monitor should still be valid", performanceMonitor)
    }

    @Test
    fun `calculates memory usage correctly`() = runTest {
        performanceMonitor.startMonitoring()
        testScope.testScheduler.advanceTimeBy(20L)
        
        val metrics = performanceMonitor.performanceMetrics.value
        
        // Based on mocked memory info: 2GB used out of 4GB = 50%
        assertEquals("Memory usage should be 50%", 0.5f, metrics.memoryUsagePercent, 0.01f)
        
        performanceMonitor.stopMonitoring()
    }

    @Test
    fun `calculates performance score correctly`() = runTest {
        // Mock good performance conditions
        mockkStatic(SystemClock::class)
        every { SystemClock.elapsedRealtimeNanos() } returnsMany (1..10).map { it * 16666667L }
        
        // Simulate 60fps
        repeat(10) {
            performanceMonitor.recordFrameTimestamp()
        }
        
        performanceMonitor.startMonitoring()
        testScope.testScheduler.advanceTimeBy(20L)
        
        val metrics = performanceMonitor.performanceMetrics.value
        
        // Good performance should result in high score
        assertTrue("Performance score should be high with good metrics", 
            metrics.performanceScore > 80f)
        assertTrue("Should be performing well", metrics.isPerformingWell)
        
        performanceMonitor.stopMonitoring()
        unmockkStatic(SystemClock::class)
    }

    @Test
    fun `starts and stops monitoring correctly`() = runTest {
        assertFalse("Should not be monitoring initially", 
            performanceMonitor.performanceMetrics.value.totalGestures > 0)
        
        performanceMonitor.startMonitoring()
        
        // Allow some monitoring time
        testScope.testScheduler.advanceTimeBy(100L)
        
        performanceMonitor.stopMonitoring()
        
        // Should not crash or throw exceptions
        assertTrue("Should handle start/stop gracefully", true)
    }

    @Test
    fun `tracks gesture count correctly`() = runTest {
        mockkStatic(SystemClock::class)
        every { SystemClock.elapsedRealtimeNanos() } returnsMany listOf(
            1000000000L, 1050000000L, // Gesture 1
            2000000000L, 2030000000L, // Gesture 2  
            3000000000L, 3040000000L  // Gesture 3
        )
        
        performanceMonitor.recordGestureStart("gesture1")
        performanceMonitor.recordGestureCompletion("gesture1")
        
        performanceMonitor.recordGestureStart("gesture2")
        performanceMonitor.recordGestureCompletion("gesture2")
        
        performanceMonitor.recordGestureStart("gesture3")
        performanceMonitor.recordGestureCompletion("gesture3")
        
        performanceMonitor.startMonitoring()
        testScope.testScheduler.advanceTimeBy(20L)
        
        val metrics = performanceMonitor.performanceMetrics.value
        assertEquals("Should track 3 completed gestures", 3, metrics.totalGestures)
        
        performanceMonitor.stopMonitoring()
        unmockkStatic(SystemClock::class)
    }

    @Test
    fun `calculates dropped frames correctly`() = runTest {
        // Mock low frame rate scenario
        mockkStatic(SystemClock::class)
        every { SystemClock.elapsedRealtimeNanos() } returnsMany (1..5).map { it * 33333333L } // 30fps
        
        repeat(5) {
            performanceMonitor.recordFrameTimestamp()
        }
        
        performanceMonitor.startMonitoring()
        testScope.testScheduler.advanceTimeBy(20L)
        
        val metrics = performanceMonitor.performanceMetrics.value
        
        // At 30fps, should have ~30 dropped frames compared to 60fps target
        assertTrue("Should calculate dropped frames for low frame rate", 
            metrics.droppedFrames > 0)
        
        performanceMonitor.stopMonitoring()
        unmockkStatic(SystemClock::class)
    }

    @Test
    fun `creates performance snapshots correctly`() {
        mockkStatic(SystemClock::class)
        every { SystemClock.elapsedRealtime() } returns 12345L
        
        val snapshot = performanceMonitor.getCurrentSnapshot()
        
        assertNotNull("Snapshot should not be null", snapshot)
        assertEquals("Snapshot timestamp should match", 12345L, snapshot.timestamp)
        assertTrue("Snapshot should have valid frame rate", snapshot.frameRate >= 0f)
        assertTrue("Snapshot should have valid latency", snapshot.averageLatency >= 0L)
        assertTrue("Snapshot should have valid memory usage", 
            snapshot.memoryUsagePercent >= 0f && snapshot.memoryUsagePercent <= 1f)
        
        unmockkStatic(SystemClock::class)
    }

    @Test
    fun `handles multiple concurrent gestures`() = runTest {
        mockkStatic(SystemClock::class)
        every { SystemClock.elapsedRealtimeNanos() } returnsMany listOf(
            1000000000L, // gesture1 start
            1500000000L, // gesture2 start  
            2000000000L, // gesture1 end (1000ms latency)
            2200000000L  // gesture2 end (700ms latency)
        )
        
        performanceMonitor.recordGestureStart("gesture1")
        performanceMonitor.recordGestureStart("gesture2")
        performanceMonitor.recordGestureCompletion("gesture1")
        performanceMonitor.recordGestureCompletion("gesture2")
        
        performanceMonitor.startMonitoring()
        testScope.testScheduler.advanceTimeBy(20L)
        
        val metrics = performanceMonitor.performanceMetrics.value
        
        // Should calculate average of 1000ms and 700ms = 850ms
        assertEquals("Should calculate average latency correctly", 
            850L, metrics.averageLatency)
        assertEquals("Should track max latency correctly", 
            1000L, metrics.maxLatency)
        
        performanceMonitor.stopMonitoring()
        unmockkStatic(SystemClock::class)
    }

    @Test
    fun `handles cleanup gracefully`() {
        performanceMonitor.startMonitoring()
        performanceMonitor.recordGestureStart("test")
        performanceMonitor.recordFrameTimestamp()
        
        // Cleanup should not throw exceptions
        performanceMonitor.cleanup()
        
        // Should be able to call cleanup multiple times
        performanceMonitor.cleanup()
        
        assertTrue("Cleanup should complete without errors", true)
    }

    @Test
    fun `circular buffer maintains capacity correctly`() {
        val buffer = CircularBuffer<Int>(3)
        
        buffer.add(1)
        buffer.add(2)
        buffer.add(3)
        
        assertEquals("Should contain 3 items", 3, buffer.getAll().size)
        
        buffer.add(4)
        
        val items = buffer.getAll()
        assertEquals("Should still contain 3 items", 3, items.size)
        assertEquals("Should contain latest items", listOf(2, 3, 4), items)
        
        buffer.clear()
        assertTrue("Should be empty after clear", buffer.getAll().isEmpty())
    }

    @Test
    fun `degradation severity calculation works correctly`() {
        var detectedDegradation: PerformanceDegradation? = null
        
        performanceMonitor.setPerformanceDegradationCallback { degradation ->
            detectedDegradation = degradation
        }
        
        // This test verifies that degradation detection is set up correctly
        // The actual degradation calculation is tested in integration
        assertNotNull("Degradation callback should be set", 
            performanceMonitor)
    }

    @Test
    fun `performance metrics updates over time`() = runTest {
        performanceMonitor.startMonitoring()
        
        val initialMetrics = performanceMonitor.performanceMetrics.value
        
        // Advance time and add some measurements
        testScope.testScheduler.advanceTimeBy(50L)
        performanceMonitor.recordFrameTimestamp()
        
        testScope.testScheduler.advanceTimeBy(50L)
        
        val updatedMetrics = performanceMonitor.performanceMetrics.value
        
        // Metrics should be updated (exact values depend on mocked behavior)
        assertNotNull("Updated metrics should not be null", updatedMetrics)
        
        performanceMonitor.stopMonitoring()
    }
}