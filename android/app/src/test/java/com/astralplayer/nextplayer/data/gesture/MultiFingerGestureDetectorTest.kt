package com.astralplayer.nextplayer.data.gesture

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerId
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

class MultiFingerGestureDetectorTest {

    private lateinit var detector: MultiFingerGestureDetector

    @Before
    fun setup() {
        detector = MultiFingerGestureDetector()
    }

    @Test
    fun `detects three finger tap`() = runTest {
        val changes = createMockPointerChanges(3, listOf(
            Offset(100f, 100f),
            Offset(200f, 100f),
            Offset(300f, 100f)
        ))
        
        detector.processPointerEvent(changes)
        
        val state = detector.gestureState.value
        assertTrue("Should be active", state.isActive)
        assertEquals("Should detect 3 fingers", 3, state.fingerCount)
    }

    @Test
    fun `detects swipe gestures`() = runTest {
        // Start gesture
        val startChanges = createMockPointerChanges(3, listOf(
            Offset(100f, 100f),
            Offset(200f, 100f),
            Offset(300f, 100f)
        ))
        detector.processPointerEvent(startChanges)
        
        // Simulate time passing and movement
        Thread.sleep(300)
        val endChanges = createMockPointerChanges(3, listOf(
            Offset(200f, 100f), // Moved right
            Offset(300f, 100f),
            Offset(400f, 100f)
        ))
        detector.processPointerEvent(endChanges)
        
        val state = detector.gestureState.value
        assertEquals("Should detect right swipe", MultiFingerGestureType.SWIPE_RIGHT, state.gestureType)
    }

    @Test
    fun `ends gesture when fingers lift`() = runTest {
        // Start with 3 fingers
        val startChanges = createMockPointerChanges(3, listOf(
            Offset(100f, 100f),
            Offset(200f, 100f),
            Offset(300f, 100f)
        ))
        detector.processPointerEvent(startChanges)
        assertTrue("Should be active", detector.gestureState.value.isActive)
        
        // Lift all fingers
        val endChanges = emptyList<PointerInputChange>()
        detector.processPointerEvent(endChanges)
        
        val state = detector.gestureState.value
        assertFalse("Should not be active", state.isActive)
        assertTrue("Should be completed", state.isCompleted)
    }

    @Test
    fun `calculates centroid correctly`() = runTest {
        val changes = createMockPointerChanges(3, listOf(
            Offset(0f, 0f),
            Offset(300f, 0f),
            Offset(150f, 300f)
        ))
        
        detector.processPointerEvent(changes)
        
        val state = detector.gestureState.value
        val expectedCentroid = Offset(150f, 100f) // Average of the three points
        assertEquals("Centroid X should be correct", expectedCentroid.x, state.centroid.x, 1f)
        assertEquals("Centroid Y should be correct", expectedCentroid.y, state.centroid.y, 1f)
    }

    @Test
    fun `resets state correctly`() = runTest {
        // Start gesture
        val changes = createMockPointerChanges(3, listOf(
            Offset(100f, 100f),
            Offset(200f, 100f),
            Offset(300f, 100f)
        ))
        detector.processPointerEvent(changes)
        assertTrue("Should be active", detector.gestureState.value.isActive)
        
        // Reset
        detector.reset()
        
        val state = detector.gestureState.value
        assertFalse("Should not be active", state.isActive)
        assertEquals("Should have no fingers", 0, state.fingerCount)
    }

    private fun createMockPointerChanges(count: Int, positions: List<Offset>): List<PointerInputChange> {
        return (0 until count).map { index ->
            mockk<PointerInputChange>(relaxed = true).apply {
                every { id } returns PointerId(index.toLong())
                every { position } returns positions.getOrElse(index) { Offset.Zero }
                every { pressed } returns true
            }
        }
    }
}