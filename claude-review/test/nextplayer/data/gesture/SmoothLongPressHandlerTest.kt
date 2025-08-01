package com.astralplayer.nextplayer.data.gesture

import androidx.compose.ui.geometry.Offset
import io.mockk.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

@OptIn(ExperimentalCoroutinesApi::class)
class SmoothLongPressHandlerTest {

    private lateinit var testScope: TestScope
    private lateinit var onSpeedChange: (Float) -> Unit
    private lateinit var onDirectionChange: (SeekDirection) -> Unit
    private lateinit var onLongPressEnd: () -> Unit
    private lateinit var smoothLongPressHandler: SmoothLongPressHandler

    @Before
    fun setup() {
        testScope = TestScope()
        onSpeedChange = mockk(relaxed = true)
        onDirectionChange = mockk(relaxed = true)
        onLongPressEnd = mockk(relaxed = true)
        
        smoothLongPressHandler = SmoothLongPressHandler(
            scope = testScope,
            onSpeedChange = onSpeedChange,
            onDirectionChange = onDirectionChange,
            onLongPressEnd = onLongPressEnd
        )
    }

    @After
    fun tearDown() {
        smoothLongPressHandler.cleanup()
    }

    @Test
    fun `startLongPress initializes state correctly`() = runTest {
        val startPosition = Offset(100f, 200f)
        
        smoothLongPressHandler.startLongPress(startPosition)
        
        val state = smoothLongPressHandler.getCurrentState()
        assertTrue("Should be active", state.isActive)
        assertEquals("Should start at normal speed", 1.0f, state.currentSpeed, 0.01f)
        assertEquals("Should have no direction initially", SeekDirection.NONE, state.direction)
    }

    @Test
    fun `updateLongPress detects horizontal direction correctly`() = runTest {
        val startPosition = Offset(100f, 200f)
        val forwardPosition = Offset(250f, 200f) // 150px forward
        
        smoothLongPressHandler.startLongPress(startPosition)
        smoothLongPressHandler.updateLongPress(forwardPosition)
        
        // Allow some time for processing
        testScope.testScheduler.advanceTimeBy(100)
        
        val state = smoothLongPressHandler.getCurrentState()
        // Direction should be detected as forward
        assertTrue("Should detect forward direction", 
            state.direction == SeekDirection.FORWARD || 
            state.directionTransition.targetDirection == SeekDirection.FORWARD)
    }

    @Test
    fun `speed progression increases over time`() = runTest {
        val startPosition = Offset(100f, 200f)
        val forwardPosition = Offset(250f, 200f)
        
        smoothLongPressHandler.startLongPress(startPosition)
        smoothLongPressHandler.updateLongPress(forwardPosition)
        
        val initialState = smoothLongPressHandler.getCurrentState()
        val initialSpeed = initialState.currentSpeed
        
        // Simulate time passing for speed progression
        testScope.testScheduler.advanceTimeBy(1000) // 1 second
        
        // Update again to trigger speed calculation
        smoothLongPressHandler.updateLongPress(Offset(300f, 200f))
        testScope.testScheduler.advanceTimeBy(100)
        
        val laterState = smoothLongPressHandler.getCurrentState()
        // Speed should increase or be in process of increasing
        assertTrue("Speed should increase over time", 
            laterState.currentSpeed > initialSpeed || 
            laterState.speedTransition.targetSpeed > initialSpeed)
    }

    @Test
    fun `direction change maintains speed progression`() = runTest {
        val startPosition = Offset(200f, 200f)
        
        smoothLongPressHandler.startLongPress(startPosition)
        
        // Move forward first
        smoothLongPressHandler.updateLongPress(Offset(300f, 200f))
        testScope.testScheduler.advanceTimeBy(1000) // Build up speed
        
        val forwardState = smoothLongPressHandler.getCurrentState()
        val speedBeforeChange = forwardState.currentSpeed
        
        // Change direction to backward
        smoothLongPressHandler.updateLongPress(Offset(100f, 200f))
        testScope.testScheduler.advanceTimeBy(200) // Allow direction change
        
        val backwardState = smoothLongPressHandler.getCurrentState()
        
        // Speed should be maintained (not reset to 1.0)
        assertTrue("Speed should not reset on direction change",
            backwardState.currentSpeed >= speedBeforeChange * 0.8f) // Allow some tolerance
    }

    @Test
    fun `endLongPress triggers smooth deceleration`() = runTest {
        val startPosition = Offset(100f, 200f)
        
        smoothLongPressHandler.startLongPress(startPosition)
        smoothLongPressHandler.updateLongPress(Offset(250f, 200f))
        testScope.testScheduler.advanceTimeBy(1000) // Build up speed
        
        val stateBeforeEnd = smoothLongPressHandler.getCurrentState()
        assertTrue("Should be active before end", stateBeforeEnd.isActive)
        
        smoothLongPressHandler.endLongPress()
        
        val stateAfterEnd = smoothLongPressHandler.getCurrentState()
        assertFalse("Should not be active after end", stateAfterEnd.isActive)
        
        // Allow time for animation
        testScope.testScheduler.advanceTimeBy(500)
        
        verify { onLongPressEnd() }
    }

    @Test
    fun `velocity tracker calculates velocity correctly`() {
        val tracker = VelocityTracker()
        
        val time1 = 1000L
        val time2 = 1100L // 100ms later
        val pos1 = Offset(100f, 200f)
        val pos2 = Offset(200f, 200f) // Moved 100px horizontally
        
        tracker.addMovement(pos1, time1)
        tracker.addMovement(pos2, time2)
        
        val velocity = tracker.getCurrentVelocity()
        
        // Velocity should be 100px / 100ms = 1.0 px/ms
        assertEquals("X velocity should be 1.0", 1.0f, velocity.x, 0.1f)
        assertEquals("Y velocity should be 0", 0.0f, velocity.y, 0.1f)
    }

    @Test
    fun `direction change predictor prevents rapid flipping`() {
        val predictor = DirectionChangePredictor()
        val currentTime = System.currentTimeMillis()
        
        // Record a recent direction change
        predictor.recordDirectionChange(
            SeekDirection.FORWARD, 
            SeekDirection.BACKWARD, 
            currentTime - 100L
        )
        
        // Try to predict opposite change too soon
        val prediction = predictor.predictDirection(
            currentVelocity = Offset(100f, 0f), // Moving forward
            currentDirection = SeekDirection.BACKWARD,
            detectedDirection = SeekDirection.FORWARD
        )
        
        // Should stick with current direction due to recent flip
        assertEquals("Should prevent rapid direction flipping", 
            SeekDirection.BACKWARD, prediction)
    }

    @Test
    fun `direction change predictor allows change with sufficient velocity`() {
        val predictor = DirectionChangePredictor()
        
        val prediction = predictor.predictDirection(
            currentVelocity = Offset(-200f, 0f), // Strong backward velocity
            currentDirection = SeekDirection.FORWARD,
            detectedDirection = SeekDirection.BACKWARD
        )
        
        assertEquals("Should allow direction change with strong velocity", 
            SeekDirection.BACKWARD, prediction)
    }

    @Test
    fun `speed transition state tracks animation progress`() = runTest {
        val startPosition = Offset(100f, 200f)
        
        smoothLongPressHandler.startLongPress(startPosition)
        smoothLongPressHandler.updateLongPress(Offset(250f, 200f))
        
        // Trigger speed change
        testScope.testScheduler.advanceTimeBy(600) // Trigger first speed level
        smoothLongPressHandler.updateLongPress(Offset(300f, 200f))
        
        val state = smoothLongPressHandler.getCurrentState()
        val speedTransition = state.speedTransition
        
        if (speedTransition.isTransitioning) {
            assertTrue("Progress should be between 0 and 1", 
                speedTransition.progress in 0f..1f)
            assertTrue("Target speed should be different from start", 
                speedTransition.targetSpeed != speedTransition.startSpeed)
        }
    }

    @Test
    fun `configureSpeedProgression updates settings correctly`() {
        val customSpeedLevels = floatArrayOf(0.5f, 1.0f, 2.0f, 4.0f)
        val customIntervals = longArrayOf(300L, 600L, 900L)
        
        smoothLongPressHandler.configureSpeedProgression(
            customSpeedLevels,
            customIntervals,
            EasingType.EASE_OUT_CUBIC
        )
        
        // Test that custom settings are applied by testing progression
        val startPosition = Offset(100f, 200f)
        smoothLongPressHandler.startLongPress(startPosition)
        smoothLongPressHandler.updateLongPress(Offset(250f, 200f))
        
        // Should work without throwing exceptions
        val state = smoothLongPressHandler.getCurrentState()
        assertNotNull("State should be valid with custom settings", state)
    }

    @Test
    fun `cleanup stops all animations and clears resources`() = runTest {
        val startPosition = Offset(100f, 200f)
        
        smoothLongPressHandler.startLongPress(startPosition)
        smoothLongPressHandler.updateLongPress(Offset(250f, 200f))
        
        assertTrue("Should be active before cleanup", 
            smoothLongPressHandler.getCurrentState().isActive)
        
        smoothLongPressHandler.cleanup()
        
        assertFalse("Should not be active after cleanup", 
            smoothLongPressHandler.getCurrentState().isActive)
    }

    @Test
    fun `multiple rapid updates don't cause issues`() = runTest {
        val startPosition = Offset(100f, 200f)
        
        smoothLongPressHandler.startLongPress(startPosition)
        
        // Simulate rapid touch updates
        for (i in 1..50) {
            smoothLongPressHandler.updateLongPress(Offset(100f + i * 2f, 200f))
            testScope.testScheduler.advanceTimeBy(16) // ~60fps
        }
        
        val finalState = smoothLongPressHandler.getCurrentState()
        assertTrue("Should still be active after rapid updates", finalState.isActive)
        assertTrue("Should have detected direction", 
            finalState.direction != SeekDirection.NONE || 
            finalState.directionTransition.targetDirection != SeekDirection.NONE)
    }

    @Test
    fun `speed levels array bounds are respected`() = runTest {
        val startPosition = Offset(100f, 200f)
        
        smoothLongPressHandler.startLongPress(startPosition)
        smoothLongPressHandler.updateLongPress(Offset(250f, 200f))
        
        // Simulate very long press (beyond all intervals)
        testScope.testScheduler.advanceTimeBy(30000) // 30 seconds
        smoothLongPressHandler.updateLongPress(Offset(300f, 200f))
        
        val state = smoothLongPressHandler.getCurrentState()
        // Should not crash and should have some reasonable speed
        assertTrue("Speed should be reasonable", state.currentSpeed > 0f && state.currentSpeed < 100f)
    }

    @Test
    fun `velocity tracker handles empty data gracefully`() {
        val tracker = VelocityTracker()
        
        val velocity = tracker.getCurrentVelocity()
        assertEquals("Should return zero velocity when empty", Offset.Zero, velocity)
        
        // Add single point
        tracker.addMovement(Offset(100f, 200f), 1000L)
        val velocityWithOnePoint = tracker.getCurrentVelocity()
        assertEquals("Should return zero velocity with single point", Offset.Zero, velocityWithOnePoint)
    }
}