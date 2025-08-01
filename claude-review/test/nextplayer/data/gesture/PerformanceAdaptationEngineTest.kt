package com.astralplayer.nextplayer.data.gesture

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import io.mockk.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

/**
 * Comprehensive tests for PerformanceAdaptationEngine
 * Tests adaptation logic and user experience impact
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PerformanceAdaptationEngineTest {

    private lateinit var mockContext: Context
    private lateinit var mockPerformanceMonitor: GesturePerformanceMonitor
    private lateinit var mockActivityManager: ActivityManager
    private lateinit var testScope: TestScope
    private lateinit var adaptationEngine: PerformanceAdaptationEngine

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        
        mockContext = mockk(relaxed = true)
        mockPerformanceMonitor = mockk(relaxed = true)
        mockActivityManager = mockk(relaxed = true)
        testScope = TestScope(testDispatcher)

        // Mock system services
        every { mockContext.getSystemService(Context.ACTIVITY_SERVICE) } returns mockActivityManager
        every { mockContext.getSystemService(Context.VIBRATOR_SERVICE) } returns mockk(relaxed = true)
        every { mockContext.packageName } returns "com.astralplayer.test"

        // Mock ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo().apply {
            totalMem = 8L * 1024 * 1024 * 1024 // 8GB
        }
        every { mockActivityManager.getMemoryInfo(any()) } answers {
            val info = firstArg<ActivityManager.MemoryInfo>()
            info.totalMem = memoryInfo.totalMem
        }
        every { mockActivityManager.memoryClass } returns 512
        every { mockActivityManager.largeMemoryClass } returns 1024

        // Mock performance monitor
        val defaultMetrics = PerformanceMetrics(
            currentFrameRate = 60f,
            averageLatency = 16L,
            memoryUsagePercent = 0.5f,
            performanceScore = 80f,
            isPerformingWell = true
        )
        every { mockPerformanceMonitor.performanceMetrics } returns MutableStateFlow(defaultMetrics)

        adaptationEngine = PerformanceAdaptationEngine(mockContext, mockPerformanceMonitor, testScope)
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
        adaptationEngine.cleanup()
    }

    @Test
    fun `initialization profiles device correctly`() = testScope.runTest {
        // When
        adaptationEngine.initialize()
        advanceUntilIdle()

        // Then
        val state = adaptationEngine.adaptationState.value
        assertNotNull(state.currentQuality)
        assertTrue(state.adaptationTimestamp > 0)
        assertEquals("Initial setup", state.lastAdaptationReason)
    }

    @Test
    fun `high-end device gets high quality initially`() = testScope.runTest {
        // Given - Mock high-end device
        mockHighEndDevice()

        // When
        adaptationEngine.initialize()
        advanceUntilIdle()

        // Then
        val state = adaptationEngine.adaptationState.value
        assertTrue(state.currentQuality == AdaptiveQuality.HIGH || state.currentQuality == AdaptiveQuality.MEDIUM)
    }

    @Test
    fun `low-end device gets appropriate quality`() = testScope.runTest {
        // Given - Mock low-end device
        mockLowEndDevice()

        // When
        adaptationEngine.initialize()
        advanceUntilIdle()

        // Then
        val state = adaptationEngine.adaptationState.value
        assertTrue(state.currentQuality == AdaptiveQuality.LOW || state.currentQuality == AdaptiveQuality.MINIMAL)
    }

    @Test
    fun `adaptation responds to performance degradation`() = testScope.runTest {
        // Given
        adaptationEngine.initialize()
        adaptationEngine.startAdaptation()
        advanceUntilIdle()

        val initialQuality = adaptationEngine.adaptationState.value.currentQuality

        // When - Simulate performance degradation
        val degradedMetrics = PerformanceMetrics(
            currentFrameRate = 25f,
            averageLatency = 60L,
            memoryUsagePercent = 0.9f,
            performanceScore = 30f,
            isPerformingWell = false
        )
        
        every { mockPerformanceMonitor.performanceMetrics } returns MutableStateFlow(degradedMetrics)
        advanceTimeBy(6000L) // Wait past minimum adaptation interval
        advanceUntilIdle()

        // Then
        val finalState = adaptationEngine.adaptationState.value
        assertTrue("Quality should be reduced due to poor performance", 
                  finalState.currentQuality.ordinal <= initialQuality.ordinal)
        assertTrue(finalState.lastAdaptationReason.contains("low frame rate") ||
                  finalState.lastAdaptationReason.contains("high latency") ||
                  finalState.lastAdaptationReason.contains("high memory usage"))
    }

    @Test
    fun `adaptation responds to performance improvement`() = testScope.runTest {
        // Given - Start with poor performance
        val poorMetrics = PerformanceMetrics(
            currentFrameRate = 20f,
            averageLatency = 80L,
            memoryUsagePercent = 0.8f,
            performanceScore = 25f,
            isPerformingWell = false
        )
        every { mockPerformanceMonitor.performanceMetrics } returns MutableStateFlow(poorMetrics)

        adaptationEngine.initialize()
        adaptationEngine.startAdaptation()
        advanceUntilIdle()

        val initialQuality = adaptationEngine.adaptationState.value.currentQuality

        // When - Simulate performance improvement
        val improvedMetrics = PerformanceMetrics(
            currentFrameRate = 55f,
            averageLatency = 18L,
            memoryUsagePercent = 0.4f,
            performanceScore = 85f,
            isPerformingWell = true
        )
        
        every { mockPerformanceMonitor.performanceMetrics } returns MutableStateFlow(improvedMetrics)
        advanceTimeBy(6000L) // Wait past minimum adaptation interval
        advanceUntilIdle()

        // Then
        val finalState = adaptationEngine.adaptationState.value
        assertTrue("Quality should be improved due to better performance",
                  finalState.currentQuality.ordinal >= initialQuality.ordinal)
    }

    @Test
    fun `manual override prevents automatic adaptation`() = testScope.runTest {
        // Given
        adaptationEngine.initialize()
        adaptationEngine.startAdaptation()
        advanceUntilIdle()

        // When - Force quality and then simulate poor performance
        adaptationEngine.forceQualityLevel(AdaptiveQuality.HIGH, "Manual test")
        
        val poorMetrics = PerformanceMetrics(
            currentFrameRate = 15f,
            averageLatency = 100L,
            memoryUsagePercent = 0.95f,
            performanceScore = 10f,
            isPerformingWell = false
        )
        every { mockPerformanceMonitor.performanceMetrics } returns MutableStateFlow(poorMetrics)
        advanceTimeBy(6000L)
        advanceUntilIdle()

        // Then
        val state = adaptationEngine.adaptationState.value
        assertEquals(AdaptiveQuality.HIGH, state.currentQuality)
        assertTrue(state.isManuallyOverridden)
        assertEquals("Manual test", state.lastAdaptationReason)
    }

    @Test
    fun `reset to automatic allows adaptation again`() = testScope.runTest {
        // Given - Manual override
        adaptationEngine.initialize()
        adaptationEngine.forceQualityLevel(AdaptiveQuality.HIGH, "Manual test")
        adaptationEngine.startAdaptation()
        advanceUntilIdle()

        // When - Reset to automatic and simulate poor performance
        adaptationEngine.resetToAutomatic()
        
        val poorMetrics = PerformanceMetrics(
            currentFrameRate = 20f,
            averageLatency = 80L,
            memoryUsagePercent = 0.9f,
            performanceScore = 20f,
            isPerformingWell = false
        )
        every { mockPerformanceMonitor.performanceMetrics } returns MutableStateFlow(poorMetrics)
        advanceTimeBy(6000L)
        advanceUntilIdle()

        // Then
        val state = adaptationEngine.adaptationState.value
        assertFalse(state.isManuallyOverridden)
        assertTrue(state.currentQuality.ordinal < AdaptiveQuality.HIGH.ordinal)
    }

    @Test
    fun `gesture adaptations match quality level`() = testScope.runTest {
        adaptationEngine.initialize()
        advanceUntilIdle()

        // Test HIGH quality adaptations
        adaptationEngine.forceQualityLevel(AdaptiveQuality.HIGH)
        val highAdaptations = adaptationEngine.getGestureAdaptations()
        
        assertTrue(highAdaptations.enableHardwareAcceleration)
        assertTrue(highAdaptations.enableAdvancedAnimations)
        assertTrue(highAdaptations.enablePredictiveGestures)
        assertEquals(60f, highAdaptations.animationFrameRate)
        assertEquals(8, highAdaptations.maxConcurrentAnimations)

        // Test MINIMAL quality adaptations
        adaptationEngine.forceQualityLevel(AdaptiveQuality.MINIMAL)
        val minimalAdaptations = adaptationEngine.getGestureAdaptations()
        
        assertFalse(minimalAdaptations.enableHardwareAcceleration)
        assertFalse(minimalAdaptations.enableAdvancedAnimations)
        assertFalse(minimalAdaptations.enablePredictiveGestures)
        assertEquals(24f, minimalAdaptations.animationFrameRate)
        assertEquals(2, minimalAdaptations.maxConcurrentAnimations)
    }

    @Test
    fun `quality settings are appropriate for level`() = testScope.runTest {
        adaptationEngine.initialize()
        advanceUntilIdle()

        // Test HIGH quality settings
        adaptationEngine.forceQualityLevel(AdaptiveQuality.HIGH)
        val highSettings = adaptationEngine.getCurrentQualitySettings()
        
        assertEquals(AnimationQuality.HIGH, highSettings.animationQuality)
        assertEquals(60f, highSettings.targetFrameRate)
        assertTrue(highSettings.enableAdvancedEffects)
        assertEquals(8, highSettings.maxConcurrentOperations)

        // Test LOW quality settings
        adaptationEngine.forceQualityLevel(AdaptiveQuality.LOW)
        val lowSettings = adaptationEngine.getCurrentQualitySettings()
        
        assertEquals(AnimationQuality.LOW, lowSettings.animationQuality)
        assertEquals(30f, lowSettings.targetFrameRate)
        assertFalse(lowSettings.enableAdvancedEffects)
        assertEquals(4, lowSettings.maxConcurrentOperations)
    }

    @Test
    fun `adaptation history is recorded correctly`() = testScope.runTest {
        // Given
        adaptationEngine.initialize()
        adaptationEngine.startAdaptation()
        advanceUntilIdle()

        // When - Force multiple adaptations
        adaptationEngine.forceQualityLevel(AdaptiveQuality.HIGH, "Test 1")
        adaptationEngine.forceQualityLevel(AdaptiveQuality.LOW, "Test 2")
        adaptationEngine.forceQualityLevel(AdaptiveQuality.MEDIUM, "Test 3")

        // Then
        val history = adaptationEngine.getAdaptationHistory()
        assertTrue(history.size >= 3)
        
        val lastRecord = history.last()
        assertEquals(AdaptiveQuality.MEDIUM, lastRecord.quality)
        assertEquals("Test 3", lastRecord.reason)
        assertTrue(lastRecord.wasForced)
        assertTrue(lastRecord.timestamp > 0)
    }

    @Test
    fun `adaptation respects minimum interval`() = testScope.runTest {
        // Given
        val config = AdaptationConfiguration(minAdaptationIntervalMs = 10000L) // 10 seconds
        adaptationEngine.configureAdaptation(config)
        adaptationEngine.initialize()
        adaptationEngine.startAdaptation()
        advanceUntilIdle()

        val initialQuality = adaptationEngine.adaptationState.value.currentQuality

        // When - Try to adapt quickly
        val poorMetrics = PerformanceMetrics(
            currentFrameRate = 10f,
            averageLatency = 100L,
            memoryUsagePercent = 0.95f,
            performanceScore = 5f,
            isPerformingWell = false
        )
        every { mockPerformanceMonitor.performanceMetrics } returns MutableStateFlow(poorMetrics)
        
        advanceTimeBy(5000L) // Only 5 seconds - should not adapt
        advanceUntilIdle()

        // Then
        assertEquals(initialQuality, adaptationEngine.adaptationState.value.currentQuality)

        // When - Wait full interval
        advanceTimeBy(6000L) // Total 11 seconds - should adapt now
        advanceUntilIdle()

        // Then
        assertNotEquals(initialQuality, adaptationEngine.adaptationState.value.currentQuality)
    }

    @Test
    fun `device profiling works correctly`() = testScope.runTest {
        // Given - Mock specific device characteristics
        mockMidRangeDevice()

        // When
        adaptationEngine.initialize()
        advanceUntilIdle()

        // Then - Should detect medium performance tier
        val adaptations = adaptationEngine.getGestureAdaptations()
        assertTrue(adaptations.animationFrameRate >= 30f && adaptations.animationFrameRate <= 60f)
        assertTrue(adaptations.maxConcurrentAnimations >= 4 && adaptations.maxConcurrentAnimations <= 8)
    }

    @Test
    fun `cleanup stops adaptation properly`() = testScope.runTest {
        // Given
        adaptationEngine.initialize()
        adaptationEngine.startAdaptation()
        advanceUntilIdle()

        // When
        adaptationEngine.cleanup()

        // Then - Should not crash and history should be cleared
        val history = adaptationEngine.getAdaptationHistory()
        assertTrue(history.isEmpty())
    }

    @Test
    fun `adaptation score calculation is accurate`() = testScope.runTest {
        adaptationEngine.initialize()
        adaptationEngine.startAdaptation()
        advanceUntilIdle()

        // Test with perfect metrics
        val perfectMetrics = PerformanceMetrics(
            currentFrameRate = 60f,
            averageLatency = 10L,
            memoryUsagePercent = 0.3f,
            performanceScore = 100f,
            isPerformingWell = true
        )
        every { mockPerformanceMonitor.performanceMetrics } returns MutableStateFlow(perfectMetrics)
        advanceTimeBy(6000L)
        advanceUntilIdle()

        val state = adaptationEngine.adaptationState.value
        assertTrue("Adaptation score should be high for perfect metrics", state.adaptationScore > 80f)

        // Test with poor metrics
        val poorMetrics = PerformanceMetrics(
            currentFrameRate = 15f,
            averageLatency = 100L,
            memoryUsagePercent = 0.95f,
            performanceScore = 10f,
            isPerformingWell = false
        )
        every { mockPerformanceMonitor.performanceMetrics } returns MutableStateFlow(poorMetrics)
        advanceTimeBy(6000L)
        advanceUntilIdle()

        val finalState = adaptationEngine.adaptationState.value
        assertTrue("Adaptation score should be low for poor metrics", finalState.adaptationScore < 40f)
    }

    // Helper methods for mocking different device types
    private fun mockHighEndDevice() {
        val memoryInfo = ActivityManager.MemoryInfo().apply {
            totalMem = 12L * 1024 * 1024 * 1024 // 12GB
        }
        every { mockActivityManager.getMemoryInfo(any()) } answers {
            val info = firstArg<ActivityManager.MemoryInfo>()
            info.totalMem = memoryInfo.totalMem
        }
        every { mockActivityManager.memoryClass } returns 1024
        every { mockActivityManager.largeMemoryClass } returns 2048

        // Mock high-end performance metrics
        val highEndMetrics = PerformanceMetrics(
            currentFrameRate = 60f,
            averageLatency = 12L,
            memoryUsagePercent = 0.4f,
            performanceScore = 95f,
            isPerformingWell = true
        )
        every { mockPerformanceMonitor.performanceMetrics } returns MutableStateFlow(highEndMetrics)
    }

    private fun mockMidRangeDevice() {
        val memoryInfo = ActivityManager.MemoryInfo().apply {
            totalMem = 6L * 1024 * 1024 * 1024 // 6GB
        }
        every { mockActivityManager.getMemoryInfo(any()) } answers {
            val info = firstArg<ActivityManager.MemoryInfo>()
            info.totalMem = memoryInfo.totalMem
        }
        every { mockActivityManager.memoryClass } returns 384
        every { mockActivityManager.largeMemoryClass } returns 768

        val midRangeMetrics = PerformanceMetrics(
            currentFrameRate = 45f,
            averageLatency = 24L,
            memoryUsagePercent = 0.6f,
            performanceScore = 65f,
            isPerformingWell = true
        )
        every { mockPerformanceMonitor.performanceMetrics } returns MutableStateFlow(midRangeMetrics)
    }

    private fun mockLowEndDevice() {
        val memoryInfo = ActivityManager.MemoryInfo().apply {
            totalMem = 3L * 1024 * 1024 * 1024 // 3GB
        }
        every { mockActivityManager.getMemoryInfo(any()) } answers {
            val info = firstArg<ActivityManager.MemoryInfo>()
            info.totalMem = memoryInfo.totalMem
        }
        every { mockActivityManager.memoryClass } returns 192
        every { mockActivityManager.largeMemoryClass } returns 384

        val lowEndMetrics = PerformanceMetrics(
            currentFrameRate = 30f,
            averageLatency = 40L,
            memoryUsagePercent = 0.8f,
            performanceScore = 35f,
            isPerformingWell = false
        )
        every { mockPerformanceMonitor.performanceMetrics } returns MutableStateFlow(lowEndMetrics)
    }
}

/**
 * Animation quality levels for testing
 */
enum class AnimationQuality {
    LOW,
    MEDIUM,
    HIGH
}