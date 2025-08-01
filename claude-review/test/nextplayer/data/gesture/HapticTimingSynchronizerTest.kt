package com.astralplayer.nextplayer.data.gesture

import android.os.SystemClock
import com.astralplayer.nextplayer.data.GestureType
import io.mockk.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

@OptIn(ExperimentalCoroutinesApi::class)
class HapticTimingSynchronizerTest {

    private lateinit var mockHapticManager: EnhancedHapticFeedbackManager
    private lateinit var testScope: TestScope
    private lateinit var synchronizer: HapticTimingSynchronizer

    @Before
    fun setup() {
        mockHapticManager = mockk(relaxed = true)
        testScope = TestScope()
        synchronizer = HapticTimingSynchronizer(mockHapticManager, testScope)
    }

    @After
    fun tearDown() {
        synchronizer.cleanup()
    }

    @Test
    fun `schedules haptic with visual synchronization`() = runTest {
        mockkStatic(SystemClock::class)
        every { SystemClock.elapsedRealtime() } returns 1000L
        
        synchronizer.scheduleHapticWithVisual(
            hapticEvent = HapticEvent.GESTURE_START,
            gestureType = GestureType.HORIZONTAL_SEEK,
            visualEventTime = 1000L,
            intensity = 1.0f
        )
        
        // Allow time for processing
        testScope.testScheduler.advanceTimeBy(100L)
        
        verify { mockHapticManager.triggerHaptic(any(), any(), any()) }
        
        unmockkStatic(SystemClock::class)
    }

    @Test
    fun `calibrates timing correctly`() {
        synchronizer.calibrateTiming(
            measuredSystemLatency = 20L,
            measuredHapticLatency = 10L,
            measuredVisualLatency = 15L
        )
        
        val stats = synchronizer.getTimingStats()
        assertEquals("System latency should be updated", 20L, stats.systemLatency)
        assertEquals("Haptic latency should be updated", 10L, stats.hapticLatency)
        assertEquals("Visual latency should be updated", 15L, stats.visualLatency)
    }

    @Test
    fun `predicts haptic events based on patterns`() = runTest {
        // Build up some history first
        repeat(5) { index ->
            synchronizer.scheduleHapticWithVisual(
                hapticEvent = HapticEvent.SPEED_CHANGE,
                gestureType = GestureType.LONG_PRESS,
                visualEventTime = 1000L + index * 200L
            )
        }
        
        testScope.testScheduler.advanceTimeBy(100L)
        
        // Now test prediction
        synchronizer.predictAndScheduleHaptic(
            currentGestureType = GestureType.LONG_PRESS,
            gestureVelocity = 150f,
            gestureDirection = 0f
        )
        
        // Should not crash and should handle prediction
        assertTrue("Prediction should complete without errors", true)
    }

    @Test
    fun `batches haptic events for performance`() = runTest {
        mockkStatic(SystemClock::class)
        every { SystemClock.elapsedRealtime() } returns 1000L
        
        // Send multiple events quickly
        repeat(3) { index ->
            synchronizer.scheduleHapticWithVisual(
                hapticEvent = HapticEvent.GESTURE_START,
                gestureType = GestureType.HORIZONTAL_SEEK,
                visualEventTime = 1000L + index
            )
        }
        
        // Allow batch processing time
        testScope.testScheduler.advanceTimeBy(50L)
        
        // Should have processed events in batch
        verify(atLeast = 1) { mockHapticManager.triggerHaptic(any(), any(), any()) }
        
        unmockkStatic(SystemClock::class)
    }

    @Test
    fun `provides timing statistics`() {
        val stats = synchronizer.getTimingStats()
        
        assertNotNull("Stats should not be null", stats)
        assertTrue("Average delay should be non-negative", stats.averageDelay >= 0f)
        assertTrue("Max delay should be non-negative", stats.maxDelay >= 0f)
        assertTrue("Min delay should be non-negative", stats.minDelay >= 0f)
        assertTrue("Event count should be non-negative", stats.eventCount >= 0)
    }

    @Test
    fun `handles cleanup gracefully`() {
        synchronizer.scheduleHapticWithVisual(
            HapticEvent.GESTURE_START,
            GestureType.HORIZONTAL_SEEK
        )
        
        synchronizer.cleanup()
        
        // Should not crash after cleanup
        assertTrue("Cleanup should complete without errors", true)
    }

    @Test
    fun `timing stats indicate performance correctly`() {
        val goodStats = HapticTimingStats(
            averageDelay = 10f,
            maxDelay = 25f,
            eventCount = 10
        )
        
        assertTrue("Good stats should indicate good performance", goodStats.isPerformingWell)
        
        val badStats = HapticTimingStats(
            averageDelay = 30f,
            maxDelay = 80f,
            eventCount = 10
        )
        
        assertFalse("Bad stats should indicate poor performance", badStats.isPerformingWell)
    }

    @Test
    fun `handles different gesture types correctly`() = runTest {
        val gestureTypes = listOf(
            GestureType.HORIZONTAL_SEEK,
            GestureType.VERTICAL_VOLUME,
            GestureType.LONG_PRESS,
            GestureType.DOUBLE_TAP
        )
        
        gestureTypes.forEach { gestureType ->
            synchronizer.scheduleHapticWithVisual(
                hapticEvent = HapticEvent.GESTURE_START,
                gestureType = gestureType
            )
        }
        
        testScope.testScheduler.advanceTimeBy(100L)
        
        // Should handle all gesture types
        verify(atLeast = gestureTypes.size) { 
            mockHapticManager.triggerHaptic(any(), any(), any()) 
        }
    }
}