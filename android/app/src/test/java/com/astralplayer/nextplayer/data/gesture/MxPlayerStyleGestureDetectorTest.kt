package com.astralplayer.nextplayer.data.gesture

import android.content.Context
import android.view.MotionEvent
import androidx.compose.ui.geometry.Offset
import io.mockk.*
import kotlinx.coroutines.test.TestScope
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

class MxPlayerStyleGestureDetectorTest {

    private lateinit var mockContext: Context
    private lateinit var testScope: TestScope
    private lateinit var mockCallbacks: MxGestureCallbacks
    private lateinit var gestureDetector: MxPlayerStyleGestureDetector

    @Before
    fun setup() {
        mockContext = mockk(relaxed = true)
        testScope = TestScope()
        mockCallbacks = mockk(relaxed = true)
        
        gestureDetector = MxPlayerStyleGestureDetector(mockContext, testScope, mockCallbacks)
        gestureDetector.setScreenSize(1080f, 1920f)
    }

    @Test
    fun `detects brightness zone correctly`() {
        val motionEvent = createMotionEvent(MotionEvent.ACTION_DOWN, 100f, 960f) // Left 20%
        
        gestureDetector.onTouchEvent(motionEvent)
        
        val state = gestureDetector.gestureState.value
        assertEquals(MxGestureZone.BRIGHTNESS, state.gestureZone)
    }

    @Test
    fun `detects volume zone correctly`() {
        val motionEvent = createMotionEvent(MotionEvent.ACTION_DOWN, 980f, 960f) // Right 20%
        
        gestureDetector.onTouchEvent(motionEvent)
        
        val state = gestureDetector.gestureState.value
        assertEquals(MxGestureZone.VOLUME, state.gestureZone)
    }

    @Test
    fun `detects seek zone correctly`() {
        val motionEvent = createMotionEvent(MotionEvent.ACTION_DOWN, 540f, 960f) // Center
        
        gestureDetector.onTouchEvent(motionEvent)
        
        val state = gestureDetector.gestureState.value
        assertEquals(MxGestureZone.SEEK, state.gestureZone)
    }

    @Test
    fun `handles brightness gesture correctly`() {
        // Start in brightness zone
        val downEvent = createMotionEvent(MotionEvent.ACTION_DOWN, 100f, 960f)
        gestureDetector.onTouchEvent(downEvent)
        
        // Move up (should increase brightness)
        val moveEvent = createMotionEvent(MotionEvent.ACTION_MOVE, 100f, 860f)
        gestureDetector.onTouchEvent(moveEvent)
        
        verify { mockCallbacks.onBrightnessGesture(any()) }
        
        val state = gestureDetector.gestureState.value
        assertEquals(MxGestureType.BRIGHTNESS, state.gestureType)
        assertTrue("Brightness delta should be positive", state.brightnessDelta > 0)
    }

    @Test
    fun `handles volume gesture correctly`() {
        // Start in volume zone
        val downEvent = createMotionEvent(MotionEvent.ACTION_DOWN, 980f, 960f)
        gestureDetector.onTouchEvent(downEvent)
        
        // Move down (should decrease volume)
        val moveEvent = createMotionEvent(MotionEvent.ACTION_MOVE, 980f, 1060f)
        gestureDetector.onTouchEvent(moveEvent)
        
        verify { mockCallbacks.onVolumeGesture(any()) }
        
        val state = gestureDetector.gestureState.value
        assertEquals(MxGestureType.VOLUME, state.gestureType)
        assertTrue("Volume delta should be negative", state.volumeDelta < 0)
    }

    @Test
    fun `handles seek gesture correctly`() {
        // Start in seek zone
        val downEvent = createMotionEvent(MotionEvent.ACTION_DOWN, 540f, 960f)
        gestureDetector.onTouchEvent(downEvent)
        
        // Move right (should seek forward)
        val moveEvent = createMotionEvent(MotionEvent.ACTION_MOVE, 640f, 960f)
        gestureDetector.onTouchEvent(moveEvent)
        
        verify { mockCallbacks.onSeekGesture(any()) }
        
        val state = gestureDetector.gestureState.value
        assertEquals(MxGestureType.SEEK, state.gestureType)
        assertTrue("Seek delta should be positive", state.seekDelta > 0)
    }

    @Test
    fun `handles long press correctly`() {
        // Start in seek zone
        val downEvent = createMotionEvent(MotionEvent.ACTION_DOWN, 540f, 960f)
        gestureDetector.onTouchEvent(downEvent)
        
        // Simulate long press by waiting and minimal movement
        Thread.sleep(600) // Longer than 500ms threshold
        val moveEvent = createMotionEvent(MotionEvent.ACTION_MOVE, 545f, 965f) // Small movement
        gestureDetector.onTouchEvent(moveEvent)
        
        verify { mockCallbacks.onLongPressStart(any()) }
        
        val state = gestureDetector.gestureState.value
        assertTrue("Should be long pressing", state.isLongPress)
        assertEquals(MxGestureType.LONG_PRESS, state.gestureType)
    }

    @Test
    fun `resets state on touch end`() {
        // Start gesture
        val downEvent = createMotionEvent(MotionEvent.ACTION_DOWN, 540f, 960f)
        gestureDetector.onTouchEvent(downEvent)
        
        assertTrue("Should be active", gestureDetector.gestureState.value.isActive)
        
        // End gesture
        val upEvent = createMotionEvent(MotionEvent.ACTION_UP, 540f, 960f)
        gestureDetector.onTouchEvent(upEvent)
        
        val state = gestureDetector.gestureState.value
        assertFalse("Should not be active", state.isActive)
        assertEquals(MxGestureType.NONE, state.gestureType)
        assertEquals(MxGestureZone.NONE, state.gestureZone)
    }

    private fun createMotionEvent(action: Int, x: Float, y: Float): MotionEvent {
        return mockk<MotionEvent>(relaxed = true).apply {
            every { this@apply.action } returns action
            every { this@apply.x } returns x
            every { this@apply.y } returns y
        }
    }
}