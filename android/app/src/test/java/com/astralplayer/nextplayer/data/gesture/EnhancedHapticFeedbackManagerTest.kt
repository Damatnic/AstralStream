package com.astralplayer.nextplayer.data.gesture

import android.content.Context
import android.os.Vibrator
import com.astralplayer.nextplayer.data.GestureType
import io.mockk.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

@OptIn(ExperimentalCoroutinesApi::class)
class EnhancedHapticFeedbackManagerTest {

    private lateinit var mockContext: Context
    private lateinit var mockVibrator: Vibrator
    private lateinit var testScope: TestScope
    private lateinit var hapticManager: EnhancedHapticFeedbackManager

    @Before
    fun setup() {
        mockContext = mockk(relaxed = true)
        mockVibrator = mockk(relaxed = true)
        testScope = TestScope()
        
        every { mockContext.getSystemService(Context.VIBRATOR_SERVICE) } returns mockVibrator
        every { mockVibrator.hasVibrator() } returns true
        
        hapticManager = EnhancedHapticFeedbackManager(mockContext, testScope)
    }

    @After
    fun tearDown() {
        hapticManager.cleanup()
    }

    @Test
    fun `initializes with default settings`() = runTest {
        val settings = hapticManager.settings.value
        
        assertTrue("Should be enabled by default", settings.enabled)
        assertEquals("Default intensity should be 0.8", 0.8f, settings.intensity, 0.01f)
        assertTrue("Speed change should be enabled", settings.enableSpeedChange)
        assertTrue("Direction change should be enabled", settings.enableDirectionChange)
    }

    @Test
    fun `triggers haptic for gesture events`() {
        hapticManager.triggerHaptic(
            gestureType = GestureType.HORIZONTAL_SEEK,
            event = HapticEvent.GESTURE_START,
            intensity = 1.0f
        )
        
        verify { mockVibrator.vibrate(any<Long>()) }
    }

    @Test
    fun `respects enabled setting`() = runTest {
        hapticManager.updateSettings(
            hapticManager.settings.value.copy(enabled = false)
        )
        
        hapticManager.triggerHaptic(
            gestureType = GestureType.HORIZONTAL_SEEK,
            event = HapticEvent.GESTURE_START
        )
        
        verify(exactly = 0) { mockVibrator.vibrate(any<Long>()) }
    }

    @Test
    fun `adjusts intensity correctly`() = runTest {
        hapticManager.updateSettings(
            hapticManager.settings.value.copy(intensity = 0.5f)
        )
        
        hapticManager.triggerHaptic(
            gestureType = GestureType.HORIZONTAL_SEEK,
            event = HapticEvent.GESTURE_START,
            intensity = 1.0f
        )
        
        verify { mockVibrator.vibrate(any<Long>()) }
    }

    @Test
    fun `handles no vibrator gracefully`() {
        every { mockVibrator.hasVibrator() } returns false
        
        hapticManager.triggerHaptic(
            gestureType = GestureType.HORIZONTAL_SEEK,
            event = HapticEvent.GESTURE_START
        )
        
        verify(exactly = 0) { mockVibrator.vibrate(any<Long>()) }
    }

    @Test
    fun `test haptic works`() {
        hapticManager.testHaptic(HapticPattern.SPEED_CHANGE)
        
        verify { mockVibrator.vibrate(any<Long>()) }
    }

    @Test
    fun `checks haptic availability`() {
        every { mockVibrator.hasVibrator() } returns true
        assertTrue("Should detect haptic availability", hapticManager.isHapticAvailable())
        
        every { mockVibrator.hasVibrator() } returns false
        assertFalse("Should detect no haptic", hapticManager.isHapticAvailable())
    }

    @Test
    fun `cleanup cancels vibrations`() {
        hapticManager.cleanup()
        
        verify { mockVibrator.cancel() }
    }
}