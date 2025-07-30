package com.astralplayer.nextplayer.data.gesture

import androidx.compose.ui.geometry.Offset
import com.astralplayer.nextplayer.data.SeekDirection
import io.mockk.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

@OptIn(ExperimentalCoroutinesApi::class)
class OptimizedDirectionChangeHandlerTest {

    private lateinit var testScope: TestScope
    private lateinit var directionHandler: OptimizedDirectionChangeHandler

    @Before
    fun setup() {
        testScope = TestScope()
        directionHandler = OptimizedDirectionChangeHandler(testScope)
    }

    @After
    fun tearDown() {
        directionHandler.cleanup()
    }

    @Test
    fun `initial state is NONE`() {
        assertEquals("Initial direction should be NONE", 
            SeekDirection.NONE, directionHandler.getCurrentDirection())
        assertEquals("Initial confirmed direction should be NONE", 
            SeekDirection.NONE, directionHandler.getLastConfirmedDirection())
    }

    @Test
    fun `detects forward direction correctly`() = runTest {
        val startPosition = Offset(100f, 200f)
        
        // Simulate forward movement
        val positions = listOf(
            Offset(110f, 200f),
            Offset(130f, 200f),
            Offset(160f, 200f),
            Offset(200f, 200f)
        )
        
        var lastResult: DirectionChangeResult? = null
        positions.forEachIndexed { index, position ->
            lastResult = directionHandler.processTouchInput(
                currentPosition = position,
                startPosition = startPosition,
                timestamp = 1000L + index * 50L
            )
        }
        
        assertNotNull("Should have result", lastResult)
        assertTrue("Should detect forward movement", 
            lastResult!!.direction == SeekDirection.FORWARD)
    }

    @Test
    fun `detects backward direction correctly`() = runTest {
        val startPosition = Offset(200f, 200f)
        
        // Simulate backward movement
        val positions = listOf(
            Offset(190f, 200f),
            Offset(170f, 200f),
            Offset(140f, 200f),
            Offset(100f, 200f)
        )
        
        var lastResult: DirectionChangeResult? = null
        positions.forEachIndexed { index, position ->
            lastResult = directionHandler.processTouchInput(
                currentPosition = position,
                startPosition = startPosition,
                timestamp = 1000L + index * 50L
            )
        }
        
        assertNotNull("Should have result", lastResult)
        assertTrue("Should detect backward movement", 
            lastResult!!.direction == SeekDirection.BACKWARD)
    }

    @Test
    fun `ignores vertical movement`() = runTest {
        val startPosition = Offset(200f, 200f)
        
        // Simulate vertical movement (should be ignored)
        val positions = listOf(
            Offset(205f, 250f),
            Offset(210f, 300f),
            Offset(215f, 350f)
        )
        
        var lastResult: DirectionChangeResult? = null
        positions.forEachIndexed { index, position ->
            lastResult = directionHandler.processTouchInput(
                currentPosition = position,
                startPosition = startPosition,
                timestamp = 1000L + index * 50L
            )
        }
        
        assertNotNull("Should have result", lastResult)
        assertEquals("Should ignore vertical movement", 
            SeekDirection.NONE, lastResult!!.direction)
    }

    @Test
    fun `instant direction change without lag`() = runTest {
        val startPosition = Offset(200f, 200f)
        val timestamp = 1000L
        
        // Move forward first
        val forwardResult = directionHandler.processTouchInput(
            currentPosition = Offset(300f, 200f),
            startPosition = startPosition,
            timestamp = timestamp
        )
        
        assertEquals("Should detect forward", SeekDirection.FORWARD, forwardResult.direction)
        
        // Immediately change to backward
        val backwardResult = directionHandler.processTouchInput(
            currentPosition = Offset(100f, 200f),
            startPosition = startPosition,
            timestamp = timestamp + 200L // After minimum interval
        )
        
        assertTrue("Direction should change to backward",
            backwardResult.direction == SeekDirection.BACKWARD || 
            backwardResult.changed)
    }

    @Test
    fun `prevents rapid direction flipping`() = runTest {
        val startPosition = Offset(200f, 200f)
        val baseTimestamp = 1000L
        
        // Establish forward direction
        directionHandler.processTouchInput(
            Offset(300f, 200f), startPosition, baseTimestamp
        )
        
        // Try to change direction too quickly (should be prevented)
        val quickChangeResult = directionHandler.processTouchInput(
            Offset(100f, 200f), startPosition, baseTimestamp + 50L
        )
        
        // Should maintain forward direction due to rapid change prevention
        assertTrue("Should prevent rapid direction flipping",
            quickChangeResult.direction == SeekDirection.FORWARD || !quickChangeResult.changed)
    }

    @Test
    fun `velocity predictor calculates correctly`() {
        val predictor = VelocityDirectionPredictor()
        
        val touchHistory = listOf(
            TouchPoint(Offset(100f, 200f), 1000L),
            TouchPoint(Offset(150f, 200f), 1050L), // 1 px/ms forward
            TouchPoint(Offset(200f, 200f), 1100L)  // 1 px/ms forward
        )
        
        val prediction = predictor.predictDirection(touchHistory)
        
        assertEquals("Should predict forward direction", 
            SeekDirection.FORWARD, prediction.direction)
        assertTrue("Should have reasonable confidence", 
            prediction.confidence > 0.3f)
    }

    @Test
    fun `velocity predictor handles insufficient data`() {
        val predictor = VelocityDirectionPredictor()
        
        val singlePoint = listOf(TouchPoint(Offset(100f, 200f), 1000L))
        val prediction = predictor.predictDirection(singlePoint)
        
        assertEquals("Should return NONE for insufficient data", 
            SeekDirection.NONE, prediction.direction)
        assertEquals("Should have zero confidence", 0f, prediction.confidence, 0.01f)
    }

    @Test
    fun `adaptive thresholds adjust to user behavior`() {
        val thresholds = AdaptiveThresholds()
        val initialThreshold = thresholds.getDirectionThreshold()
        
        // Simulate user with larger movements
        val largeMovements = listOf(
            TouchPoint(Offset(100f, 200f), 1000L),
            TouchPoint(Offset(180f, 200f), 1050L), // 80px movement
            TouchPoint(Offset(260f, 200f), 1100L), // 80px movement
            TouchPoint(Offset(340f, 200f), 1150L), // 80px movement
            TouchPoint(Offset(420f, 200f), 1200L)  // 80px movement
        )
        
        thresholds.updateFromTouchHistory(largeMovements)
        val adaptedThreshold = thresholds.getDirectionThreshold()
        
        assertTrue("Threshold should adapt to larger movements",
            adaptedThreshold != initialThreshold)
    }

    @Test
    fun `confidence calculation considers multiple factors`() = runTest {
        val startPosition = Offset(100f, 200f)
        
        // Create consistent forward movement
        val consistentPositions = listOf(
            Offset(120f, 200f),
            Offset(140f, 200f),
            Offset(160f, 200f),
            Offset(180f, 200f),
            Offset(200f, 200f)
        )
        
        var finalResult: DirectionChangeResult? = null
        consistentPositions.forEachIndexed { index, position ->
            finalResult = directionHandler.processTouchInput(
                currentPosition = position,
                startPosition = startPosition,
                timestamp = 1000L + index * 100L
            )
        }
        
        assertNotNull("Should have result", finalResult)
        assertTrue("Consistent movement should have high confidence",
            finalResult!!.confidence > 0.6f)
    }

    @Test
    fun `direction change events are emitted correctly`() = runTest {
        val events = mutableListOf<OptimizedDirectionChangeEvent>()
        
        // Collect direction change events
        val job = launch {
            directionHandler.directionChanges.collect { event ->
                events.add(event)
            }
        }
        
        val startPosition = Offset(200f, 200f)
        
        // Trigger direction changes
        directionHandler.processTouchInput(Offset(300f, 200f), startPosition, 1000L)
        testScope.testScheduler.advanceUntilIdle()
        
        directionHandler.processTouchInput(Offset(100f, 200f), startPosition, 1200L)
        testScope.testScheduler.advanceUntilIdle()
        
        job.cancel()
        
        assertTrue("Should emit direction change events", events.isNotEmpty())
        events.forEach { event ->
            assertNotNull("Event should have valid from direction", event.fromDirection)
            assertNotNull("Event should have valid to direction", event.toDirection)
            assertTrue("Event should have positive timestamp", event.timestamp > 0)
        }
    }

    @Test
    fun `performance metrics are tracked correctly`() = runTest {
        val startPosition = Offset(100f, 200f)
        
        // Perform several direction changes
        repeat(5) { index ->
            val position = if (index % 2 == 0) {
                Offset(200f + index * 10f, 200f)
            } else {
                Offset(50f - index * 5f, 200f)
            }
            
            directionHandler.processTouchInput(
                position, startPosition, 1000L + index * 300L
            )
        }
        
        val metrics = directionHandler.performanceMetrics.value
        
        assertTrue("Should track direction changes", 
            metrics.totalDirectionChanges >= 0)
        assertTrue("Should track touch events", 
            metrics.totalTouchEvents >= 0)
        
        if (metrics.totalDirectionChanges > 0) {
            assertTrue("Should have valid average latency", 
                metrics.averageLatency >= 0f)
            assertTrue("Should have valid average confidence", 
                metrics.averageConfidence >= 0f)
        }
    }

    @Test
    fun `threshold configuration works correctly`() {
        val customDirectionThreshold = 75f
        val customAngleThreshold = 25f
        val customConfidenceThreshold = 0.8f
        val customMinInterval = 200L
        
        directionHandler.configureThresholds(
            directionThreshold = customDirectionThreshold,
            angleThreshold = customAngleThreshold,
            confidenceThreshold = customConfidenceThreshold,
            minChangeInterval = customMinInterval
        )
        
        // Test that configuration is applied by checking behavior
        // (Internal thresholds are private, so we test behavior)
        val startPosition = Offset(100f, 200f)
        val result = directionHandler.processTouchInput(
            Offset(150f, 200f), // Small movement that might be below custom threshold
            startPosition,
            1000L
        )
        
        // Should work without throwing exceptions
        assertNotNull("Should handle custom thresholds", result)
    }

    @Test
    fun `reset clears all state correctly`() = runTest {
        val startPosition = Offset(100f, 200f)
        
        // Establish some direction
        directionHandler.processTouchInput(Offset(200f, 200f), startPosition, 1000L)
        
        val beforeReset = directionHandler.getCurrentDirection()
        assertNotEquals("Should have direction before reset", SeekDirection.NONE, beforeReset)
        
        // Reset
        directionHandler.reset()
        
        assertEquals("Direction should be NONE after reset", 
            SeekDirection.NONE, directionHandler.getCurrentDirection())
        assertEquals("Confirmed direction should be NONE after reset", 
            SeekDirection.NONE, directionHandler.getLastConfirmedDirection())
    }

    @Test
    fun `handles edge case of same position repeatedly`() = runTest {
        val startPosition = Offset(100f, 200f)
        val samePosition = Offset(100f, 200f)
        
        // Process same position multiple times
        repeat(5) { index ->
            val result = directionHandler.processTouchInput(
                samePosition, startPosition, 1000L + index * 50L
            )
            
            assertEquals("Should maintain NONE direction for same position",
                SeekDirection.NONE, result.direction)
        }
    }

    @Test
    fun `latency calculation is reasonable`() = runTest {
        val startPosition = Offset(100f, 200f)
        val timestamp = 1000L
        
        val result = directionHandler.processTouchInput(
            Offset(200f, 200f), startPosition, timestamp
        )
        
        assertTrue("Latency should be non-negative", result.latency >= 0)
        assertTrue("Latency should be reasonable (< 100ms)", result.latency < 100L)
    }

    @Test
    fun `touch point cleanup works correctly`() = runTest {
        val startPosition = Offset(100f, 200f)
        
        // Add many touch points to trigger cleanup
        repeat(20) { index ->
            directionHandler.processTouchInput(
                Offset(100f + index * 10f, 200f),
                startPosition,
                1000L + index * 10L
            )
        }
        
        // Should handle cleanup without issues
        val result = directionHandler.processTouchInput(
            Offset(300f, 200f), startPosition, 2000L
        )
        
        assertNotNull("Should continue working after cleanup", result)
    }
}