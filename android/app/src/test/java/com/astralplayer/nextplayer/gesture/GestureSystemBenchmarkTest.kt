package com.astralplayer.nextplayer.gesture

import android.content.Context
import android.os.SystemClock
import com.astralplayer.nextplayer.data.GestureType
import com.astralplayer.nextplayer.data.gesture.*
import io.mockk.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import kotlin.system.measureTimeMillis

/**
 * Comprehensive benchmark tests for gesture system performance
 */
@OptIn(ExperimentalCoroutinesApi::class)
class GestureSystemBenchmarkTest {

    private lateinit var mockContext: Context
    private lateinit var testScope: TestScope
    private lateinit var gestureDetector: UltraFastGestureDetector
    private lateinit var performanceMonitor: GesturePerformanceMonitor
    private lateinit var animationEngine: AdvancedAnimationEngine
    private lateinit var memoryManager: MemoryOptimizedStateManager

    @Before
    fun setup() {
        mockContext = mockk(relaxed = true)
        testScope = TestScope()
        
        performanceMonitor = GesturePerformanceMonitor(mockContext, testScope)
        animationEngine = AdvancedAnimationEngine(testScope)
        memoryManager = MemoryOptimizedStateManager(mockContext, testScope)
        
        gestureDetector = UltraFastGestureDetector(
            screenWidth = 1080f,
            screenHeight = 1920f,
            settings = EnhancedGestureSettings(),
            callbacks = mockk(relaxed = true),
            scope = testScope
        )
    }

    @After
    fun tearDown() {
        gestureDetector.cleanup()
        performanceMonitor.cleanup()
        animationEngine.stop()
        memoryManager.cleanup()
    }

    @Test
    fun `benchmark gesture detection latency`() = runTest {
        val iterations = 1000
        val latencies = mutableListOf<Long>()
        
        mockkStatic(SystemClock::class)
        var currentTime = 0L
        every { SystemClock.elapsedRealtimeNanos() } answers { currentTime }
        
        repeat(iterations) { i ->
            val startTime = currentTime
            currentTime += 1000000L // 1ms increment
            
            val mockChange = mockk<androidx.compose.ui.input.pointer.PointerInputChange>(relaxed = true)
            every { mockChange.position } returns androidx.compose.ui.geometry.Offset(500f, 500f)
            every { mockChange.pressed } returns true
            
            val detectionTime = measureTimeMillis {
                gestureDetector.detectGesture(mockChange)
            }
            
            latencies.add(detectionTime)
        }
        
        val averageLatency = latencies.average()
        val maxLatency = latencies.maxOrNull() ?: 0L
        val p95Latency = latencies.sorted()[((iterations * 0.95).toInt())]
        
        println("Gesture Detection Benchmark Results:")
        println("Average latency: ${averageLatency}ms")
        println("Max latency: ${maxLatency}ms")
        println("95th percentile: ${p95Latency}ms")
        
        // Performance assertions
        assertTrue("Average latency should be under 1ms", averageLatency < 1.0)
        assertTrue("Max latency should be under 5ms", maxLatency < 5L)
        assertTrue("95th percentile should be under 2ms", p95Latency < 2L)
        
        unmockkStatic(SystemClock::class)
    }

    @Test
    fun `benchmark memory allocation during gesture processing`() = runTest {
        val iterations = 10000
        val initialMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()
        
        repeat(iterations) {
            val gestureState = memoryManager.obtainFromPool(GestureStateWrapper::class)
            gestureState.initialize(
                GestureType.HORIZONTAL_SEEK,
                androidx.compose.ui.geometry.Offset(100f, 100f),
                androidx.compose.ui.geometry.Offset(200f, 100f),
                System.currentTimeMillis()
            )
            memoryManager.returnToPool(gestureState)
        }
        
        // Force garbage collection
        System.gc()
        Thread.sleep(100)
        
        val finalMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()
        val memoryIncrease = finalMemory - initialMemory
        val memoryPerGesture = memoryIncrease.toFloat() / iterations
        
        println("Memory Allocation Benchmark Results:")
        println("Total memory increase: ${memoryIncrease / 1024}KB")
        println("Memory per gesture: ${memoryPerGesture}bytes")
        
        // Memory efficiency assertions
        assertTrue("Memory increase should be minimal", memoryIncrease < 1024 * 1024) // Less than 1MB
        assertTrue("Memory per gesture should be under 100 bytes", memoryPerGesture < 100f)
    }

    @Test
    fun `benchmark animation frame rate consistency`() = runTest {
        val testDuration = 5000L // 5 seconds
        val frameRates = mutableListOf<Float>()
        
        animationEngine.start()
        
        val startTime = System.currentTimeMillis()
        while (System.currentTimeMillis() - startTime < testDuration) {
            val currentFrameRate = animationEngine.frameRate.value
            if (currentFrameRate > 0) {
                frameRates.add(currentFrameRate)
            }
            delay(16) // ~60fps sampling
        }
        
        animationEngine.stop()
        
        val averageFrameRate = frameRates.average()
        val minFrameRate = frameRates.minOrNull() ?: 0f
        val frameRateVariance = frameRates.map { (it - averageFrameRate).let { diff -> diff * diff } }.average()
        
        println("Animation Frame Rate Benchmark Results:")
        println("Average frame rate: ${averageFrameRate}fps")
        println("Minimum frame rate: ${minFrameRate}fps")
        println("Frame rate variance: $frameRateVariance")
        
        // Frame rate consistency assertions
        assertTrue("Average frame rate should be at least 55fps", averageFrameRate >= 55f)
        assertTrue("Minimum frame rate should be at least 45fps", minFrameRate >= 45f)
        assertTrue("Frame rate variance should be low", frameRateVariance < 25f)
    }

    @Test
    fun `benchmark concurrent gesture processing`() = runTest {
        val concurrentGestures = 10
        val gesturesPerThread = 100
        val jobs = mutableListOf<Job>()
        
        val startTime = System.currentTimeMillis()
        
        repeat(concurrentGestures) { threadIndex ->
            val job = launch {
                repeat(gesturesPerThread) { gestureIndex ->
                    val mockChange = mockk<androidx.compose.ui.input.pointer.PointerInputChange>(relaxed = true)
                    every { mockChange.position } returns androidx.compose.ui.geometry.Offset(
                        (threadIndex * 100f) + gestureIndex,
                        500f
                    )
                    every { mockChange.pressed } returns true
                    
                    gestureDetector.detectGesture(mockChange)
                    yield() // Allow other coroutines to run
                }
            }
            jobs.add(job)
        }
        
        jobs.joinAll()
        val totalTime = System.currentTimeMillis() - startTime
        val totalGestures = concurrentGestures * gesturesPerThread
        val gesturesPerSecond = (totalGestures * 1000f) / totalTime
        
        println("Concurrent Processing Benchmark Results:")
        println("Total gestures: $totalGestures")
        println("Total time: ${totalTime}ms")
        println("Gestures per second: $gesturesPerSecond")
        
        // Concurrent processing assertions
        assertTrue("Should process at least 1000 gestures per second", gesturesPerSecond >= 1000f)
        assertTrue("Total time should be reasonable", totalTime < 5000L) // Less than 5 seconds
    }

    @Test
    fun `benchmark performance degradation detection`() = runTest {
        performanceMonitor.startMonitoring()
        
        // Simulate performance degradation
        val degradationEvents = mutableListOf<PerformanceDegradation>()
        performanceMonitor.setPerformanceDegradationCallback { degradation ->
            degradationEvents.add(degradation)
        }
        
        // Simulate high latency gestures
        repeat(50) {
            performanceMonitor.recordGestureStart("test_gesture_$it")
            delay(100) // Simulate slow processing
            performanceMonitor.recordGestureCompletion("test_gesture_$it")
        }
        
        // Wait for degradation detection
        delay(2000)
        
        performanceMonitor.stopMonitoring()
        
        println("Performance Degradation Benchmark Results:")
        println("Degradation events detected: ${degradationEvents.size}")
        degradationEvents.forEach { event ->
            println("- ${event.type}: ${event.severity} (${event.message})")
        }
        
        // Degradation detection assertions
        assertTrue("Should detect performance degradation", degradationEvents.isNotEmpty())
        assertTrue("Should detect latency increase", 
            degradationEvents.any { it.type == DegradationType.LATENCY_INCREASE })
    }

    @Test
    fun `benchmark memory pressure handling`() = runTest {
        val initialStats = memoryManager.getMemoryUsage()
        
        // Simulate memory pressure by creating many objects
        val objects = mutableListOf<Any>()
        repeat(10000) {
            objects.add(memoryManager.obtainFromPool(GestureStateWrapper::class))
        }
        
        // Trigger memory pressure handling
        memoryManager.handleMemoryPressure(MemoryPressureLevel.CRITICAL)
        
        // Allow cleanup to complete
        delay(1000)
        
        val finalStats = memoryManager.getMemoryUsage()
        val memoryReduction = initialStats.usedMemoryBytes - finalStats.usedMemoryBytes
        
        println("Memory Pressure Benchmark Results:")
        println("Initial memory: ${initialStats.formattedUsedMemory}")
        println("Final memory: ${finalStats.formattedUsedMemory}")
        println("Memory reduction: ${memoryReduction / (1024 * 1024)}MB")
        
        // Memory pressure handling assertions
        assertTrue("Should reduce memory usage", memoryReduction > 0)
        assertTrue("Memory pressure level should improve", 
            finalStats.memoryPressureLevel.ordinal <= initialStats.memoryPressureLevel.ordinal)
    }

    @Test
    fun `benchmark gesture accuracy under stress`() = runTest {
        val totalGestures = 1000
        val correctDetections = mutableListOf<Boolean>()
        
        // Test different gesture types
        val gestureTypes = listOf(
            GestureType.SINGLE_TAP,
            GestureType.DOUBLE_TAP,
            GestureType.HORIZONTAL_SEEK,
            GestureType.VERTICAL_VOLUME,
            GestureType.LONG_PRESS
        )
        
        repeat(totalGestures) { i ->
            val expectedType = gestureTypes[i % gestureTypes.size]
            var detectedType: GestureType? = null
            
            val callbacks = object : GestureCallbacks {
                override fun onSingleTap(position: androidx.compose.ui.geometry.Offset) {
                    detectedType = GestureType.SINGLE_TAP
                }
                override fun onDoubleTap(position: androidx.compose.ui.geometry.Offset, side: com.astralplayer.nextplayer.data.TouchSide) {
                    detectedType = GestureType.DOUBLE_TAP
                }
                override fun onHorizontalSeek(delta: Float, velocity: Float) {
                    detectedType = GestureType.HORIZONTAL_SEEK
                }
                override fun onVerticalVolumeChange(delta: Float, side: com.astralplayer.nextplayer.data.TouchSide) {
                    detectedType = GestureType.VERTICAL_VOLUME
                }
                override fun onVerticalBrightnessChange(delta: Float, side: com.astralplayer.nextplayer.data.TouchSide) {
                    detectedType = GestureType.VERTICAL_BRIGHTNESS
                }
                override fun onLongPressStart(position: androidx.compose.ui.geometry.Offset) {
                    detectedType = GestureType.LONG_PRESS
                }
                override fun onLongPressEnd() {}
                override fun onPinchZoom(scaleFactor: Float, center: androidx.compose.ui.geometry.Offset) {}
            }
            
            // Create detector with callbacks
            val detector = UltraFastGestureDetector(
                screenWidth = 1080f,
                screenHeight = 1920f,
                settings = EnhancedGestureSettings(),
                callbacks = callbacks,
                scope = testScope
            )
            
            // Simulate gesture based on expected type
            val mockChange = createMockGestureForType(expectedType, i)
            detector.detectGesture(mockChange)
            
            // Allow processing time
            delay(10)
            
            correctDetections.add(detectedType == expectedType)
            detector.cleanup()
        }
        
        val accuracy = correctDetections.count { it }.toFloat() / totalGestures
        val falsePositives = correctDetections.count { !it }
        
        println("Gesture Accuracy Benchmark Results:")
        println("Total gestures: $totalGestures")
        println("Correct detections: ${correctDetections.count { it }}")
        println("Accuracy: ${(accuracy * 100).toInt()}%")
        println("False positives: $falsePositives")
        
        // Accuracy assertions
        assertTrue("Accuracy should be at least 95%", accuracy >= 0.95f)
        assertTrue("False positives should be under 5%", falsePositives < totalGestures * 0.05)
    }

    private fun createMockGestureForType(type: GestureType, index: Int): androidx.compose.ui.input.pointer.PointerInputChange {
        val mockChange = mockk<androidx.compose.ui.input.pointer.PointerInputChange>(relaxed = true)
        
        when (type) {
            GestureType.SINGLE_TAP -> {
                every { mockChange.position } returns androidx.compose.ui.geometry.Offset(500f, 500f)
                every { mockChange.pressed } returns true
            }
            GestureType.HORIZONTAL_SEEK -> {
                every { mockChange.position } returns androidx.compose.ui.geometry.Offset(500f + index * 10f, 500f)
                every { mockChange.pressed } returns true
            }
            GestureType.VERTICAL_VOLUME -> {
                every { mockChange.position } returns androidx.compose.ui.geometry.Offset(900f, 500f + index * 10f)
                every { mockChange.pressed } returns true
            }
            else -> {
                every { mockChange.position } returns androidx.compose.ui.geometry.Offset(500f, 500f)
                every { mockChange.pressed } returns true
            }
        }
        
        return mockChange
    }
}