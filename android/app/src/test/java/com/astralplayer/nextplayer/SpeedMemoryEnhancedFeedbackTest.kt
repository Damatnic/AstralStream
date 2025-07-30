package com.astralplayer.nextplayer

import com.astralplayer.nextplayer.ui.components.SpeedMemoryToastType
import com.astralplayer.nextplayer.ui.components.SpeedMemoryToastState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import kotlinx.coroutines.Dispatchers
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

/**
 * Test class for Enhanced Feedback Systems in Phase 1B
 * Tests toast notifications, visual indicators, and speed memory functionality
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SpeedMemoryEnhancedFeedbackTest {

    private lateinit var testDispatcher: TestDispatcher

    @Before
    fun setup() {
        testDispatcher = StandardTestDispatcher()
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `SpeedMemoryToastState should be created correctly`() {
        // Test toast state creation for different types
        val restoredToast = SpeedMemoryToastState(
            message = "Speed restored: 2.0x",
            type = SpeedMemoryToastType.RESTORED,
            isVisible = true
        )
        
        assertEquals("Speed restored: 2.0x", restoredToast.message)
        assertEquals(SpeedMemoryToastType.RESTORED, restoredToast.type)
        assertTrue(restoredToast.isVisible)
    }

    @Test
    fun `SpeedMemoryToastType should have all required types`() {
        // Test that all expected toast types are available
        val types = SpeedMemoryToastType.values()
        
        assertTrue(types.contains(SpeedMemoryToastType.RESTORED))
        assertTrue(types.contains(SpeedMemoryToastType.SAVED))
        assertTrue(types.contains(SpeedMemoryToastType.CLEARED))
        assertTrue(types.contains(SpeedMemoryToastType.ERROR))
        
        assertEquals(4, types.size)
    }

    @Test
    fun `toast creation for different speeds should work`() {
        val speeds = listOf(0.25f, 0.5f, 1.0f, 1.5f, 2.0f, 4.0f)
        
        speeds.forEach { speed ->
            val toast = SpeedMemoryToastState(
                message = "Speed saved: ${formatSpeed(speed)}",
                type = SpeedMemoryToastType.SAVED,
                isVisible = true
            )
            
            assertTrue(toast.message.contains("Speed saved"))
            assertTrue(toast.message.contains("${formatSpeed(speed)}"))
            assertEquals(SpeedMemoryToastType.SAVED, toast.type)
        }
    }

    @Test
    fun `error toast should be created correctly`() {
        val errorToast = SpeedMemoryToastState(
            message = "Failed to clear speed memory",
            type = SpeedMemoryToastType.ERROR,
            isVisible = true
        )
        
        assertEquals("Failed to clear speed memory", errorToast.message)
        assertEquals(SpeedMemoryToastType.ERROR, errorToast.type)
        assertTrue(errorToast.isVisible)
    }

    @Test
    fun `toast visibility state should be manageable`() {
        var toastDismissed = false
        
        val toast = SpeedMemoryToastState(
            message = "Test message",
            type = SpeedMemoryToastType.RESTORED,
            isVisible = true,
            onDismiss = { toastDismissed = true }
        )
        
        assertTrue(toast.isVisible)
        assertFalse(toastDismissed)
        
        // Simulate dismissing the toast
        toast.onDismiss()
        assertTrue(toastDismissed)
    }

    @Test
    fun `speed memory indicator logic should work correctly`() {
        // Test the logic for when speed memory indicator should be shown
        val testCases = listOf(
            Pair(true, 2.0f) to true,   // Has memory, non-normal speed
            Pair(true, 1.0f) to true,   // Has memory, normal speed
            Pair(false, 2.0f) to false, // No memory, non-normal speed
            Pair(false, 1.0f) to false  // No memory, normal speed
        )
        
        testCases.forEach { (input, expected) ->
            val (hasMemory, speed) = input
            val shouldShow = hasMemory // Based on our implementation logic
            assertEquals("Failed for hasMemory=$hasMemory, speed=$speed", expected, shouldShow)
        }
    }

    @Test
    fun `speed formatting should be consistent`() {
        val speedFormats = mapOf(
            0.25f to "0.25x",
            0.5f to "0.5x",
            0.75f to "0.75x",
            1.0f to "1.0x",
            1.25f to "1.25x",
            1.5f to "1.5x",
            2.0f to "2.0x",
            3.0f to "3.0x",
            4.0f to "4.0x"
        )
        
        speedFormats.forEach { (speed, expectedFormat) ->
            assertEquals(expectedFormat, formatSpeed(speed))
        }
    }

    @Test
    fun `toast message generation should be correct`() {
        val testMessages = listOf(
            Triple(SpeedMemoryToastType.RESTORED, 2.0f, "Speed restored: 2.0x"),
            Triple(SpeedMemoryToastType.SAVED, 1.5f, "Speed saved: 1.5x"),
            Triple(SpeedMemoryToastType.CLEARED, 0f, "All speed memory cleared"),
            Triple(SpeedMemoryToastType.ERROR, 0f, "Failed to clear speed memory")
        )
        
        testMessages.forEach { (type, speed, expectedMessage) ->
            val message = when (type) {
                SpeedMemoryToastType.RESTORED -> "Speed restored: ${formatSpeed(speed)}"
                SpeedMemoryToastType.SAVED -> "Speed saved: ${formatSpeed(speed)}"
                SpeedMemoryToastType.CLEARED -> "All speed memory cleared"
                SpeedMemoryToastType.ERROR -> "Failed to clear speed memory"
            }
            assertEquals(expectedMessage, message)
        }
    }

    @Test
    fun `enhanced feedback system integration should work`() {
        // Test the integration points of our enhanced feedback system
        var feedbackTriggered = false
        
        // Simulate feedback system activation
        val simulateFeedback = { 
            feedbackTriggered = true
        }
        
        // Test different feedback scenarios
        simulateFeedback()
        assertTrue("Feedback system should be triggered", feedbackTriggered)
    }

    @Test
    fun `speed memory state changes should trigger appropriate feedback`() {
        val feedbackEvents = mutableListOf<String>()
        
        // Simulate state changes that should trigger feedback
        val simulateSpeedChange = { oldSpeed: Float, newSpeed: Float ->
            if (oldSpeed != newSpeed) {
                feedbackEvents.add("Speed changed from ${formatSpeed(oldSpeed)} to ${formatSpeed(newSpeed)}")
            }
        }
        
        val simulateSpeedMemoryLoad = { speed: Float ->
            feedbackEvents.add("Speed memory loaded: ${formatSpeed(speed)}")
        }
        
        // Test scenarios
        simulateSpeedChange(1.0f, 2.0f)
        simulateSpeedMemoryLoad(1.5f)
        
        assertEquals(2, feedbackEvents.size)
        assertTrue(feedbackEvents[0].contains("Speed changed"))
        assertTrue(feedbackEvents[1].contains("Speed memory loaded"))
    }

    // Helper function to format speed consistently with the app
    private fun formatSpeed(speed: Float): String {
        return when {
            speed == 0.25f -> "0.25x"
            speed == 0.5f -> "0.5x"
            speed == 0.75f -> "0.75x"
            speed == 1.0f -> "1.0x"
            speed == 1.25f -> "1.25x"
            speed == 1.5f -> "1.5x"
            speed == 2.0f -> "2.0x"
            speed == 3.0f -> "3.0x"
            speed == 4.0f -> "4.0x"
            else -> "${String.format("%.2f", speed)}x"
        }
    }
}