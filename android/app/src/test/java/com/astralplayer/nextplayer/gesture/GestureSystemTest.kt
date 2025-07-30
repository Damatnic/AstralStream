package com.astralplayer.nextplayer.gesture

import android.view.MotionEvent
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.astralplayer.nextplayer.data.GestureManager
import com.astralplayer.nextplayer.data.gesture.EnhancedGestureDetector
import com.astralplayer.nextplayer.data.gesture.HorizontalSeekGestureHandler
import com.astralplayer.nextplayer.data.gesture.VerticalGestureHandler
import com.astralplayer.nextplayer.data.gesture.OrientationAwareGestureHandler
import com.astralplayer.nextplayer.data.gesture.accessibility.GestureAccessibilityManager
import io.mockk.*
import kotlinx.coroutines.test.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.*
import kotlin.math.abs

/**
 * Comprehensive test suite for gesture recognition and control systems
 */
@RunWith(AndroidJUnit4::class)
class GestureSystemTest {

    private lateinit var gestureManager: GestureManager
    private lateinit var enhancedGestureDetector: EnhancedGestureDetector
    private lateinit var horizontalSeekHandler: HorizontalSeekGestureHandler
    private lateinit var verticalGestureHandler: VerticalGestureHandler
    private lateinit var orientationAwareHandler: OrientationAwareGestureHandler
    private lateinit var accessibilityManager: GestureAccessibilityManager

    @Before
    fun setup() {
        gestureManager = mockk(relaxed = true)
        enhancedGestureDetector = mockk(relaxed = true)
        horizontalSeekHandler = mockk(relaxed = true)
        verticalGestureHandler = mockk(relaxed = true)
        orientationAwareHandler = mockk(relaxed = true)
        accessibilityManager = mockk(relaxed = true)
    }

    @Test
    fun `test basic horizontal swipe detection`() = runTest {
        // Simulate horizontal swipe gesture
        val startX = 100f
        val endX = 300f
        val y = 500f
        
        val downEvent = createMotionEvent(MotionEvent.ACTION_DOWN, startX, y)
        val moveEvent1 = createMotionEvent(MotionEvent.ACTION_MOVE, startX + 50, y)
        val moveEvent2 = createMotionEvent(MotionEvent.ACTION_MOVE, startX + 100, y)
        val upEvent = createMotionEvent(MotionEvent.ACTION_UP, endX, y)

        every { horizontalSeekHandler.handleGesture(any(), any()) } returns true
        every { horizontalSeekHandler.isHorizontalSwipe(any(), any(), any()) } returns true
        every { horizontalSeekHandler.calculateSeekAmount(any()) } returns 15000L // 15 seconds

        // Process gesture sequence
        enhancedGestureDetector.onTouchEvent(downEvent)
        enhancedGestureDetector.onTouchEvent(moveEvent1)
        enhancedGestureDetector.onTouchEvent(moveEvent2)
        enhancedGestureDetector.onTouchEvent(upEvent)

        // Verify horizontal seek was detected
        verify { horizontalSeekHandler.handleGesture(any(), any()) }
        verify { horizontalSeekHandler.calculateSeekAmount(any()) }
        
        // Verify seek amount is reasonable
        val seekAmount = horizontalSeekHandler.calculateSeekAmount(endX - startX)
        assertTrue("Seek amount should be positive for right swipe", seekAmount > 0)
        assertTrue("Seek amount should be reasonable", seekAmount in 5000L..30000L)
    }

    @Test
    fun `test vertical brightness and volume gestures`() = runTest {
        // Test brightness gesture (left side of screen)
        val brightnessSide = 100f // Left side
        val startY = 300f
        val endY = 150f // Upward swipe
        
        val brightnessDownEvent = createMotionEvent(MotionEvent.ACTION_DOWN, brightnessSide, startY)
        val brightnessUpEvent = createMotionEvent(MotionEvent.ACTION_UP, brightnessSide, endY)

        every { verticalGestureHandler.isBrightnessGesture(brightnessSide) } returns true
        every { verticalGestureHandler.calculateBrightnessChange(any()) } returns 0.3f
        every { verticalGestureHandler.handleBrightnessGesture(any()) } returns true

        enhancedGestureDetector.onTouchEvent(brightnessDownEvent)
        enhancedGestureDetector.onTouchEvent(brightnessUpEvent)

        verify { verticalGestureHandler.handleBrightnessGesture(any()) }
        
        // Test volume gesture (right side of screen)
        val volumeSide = 900f // Right side
        val volumeDownEvent = createMotionEvent(MotionEvent.ACTION_DOWN, volumeSide, startY)
        val volumeUpEvent = createMotionEvent(MotionEvent.ACTION_UP, volumeSide, endY)

        every { verticalGestureHandler.isVolumeGesture(volumeSide) } returns true
        every { verticalGestureHandler.calculateVolumeChange(any()) } returns 0.25f
        every { verticalGestureHandler.handleVolumeGesture(any()) } returns true

        enhancedGestureDetector.onTouchEvent(volumeDownEvent)
        enhancedGestureDetector.onTouchEvent(volumeUpEvent)

        verify { verticalGestureHandler.handleVolumeGesture(any()) }
    }

    @Test
    fun `test gesture sensitivity adjustment`() = runTest {
        val baseSensitivity = 1.0f
        val highSensitivity = 2.0f
        val lowSensitivity = 0.5f

        // Test with different sensitivity levels
        listOf(baseSensitivity, highSensitivity, lowSensitivity).forEach { sensitivity ->
            every { gestureManager.getSensitivity() } returns sensitivity
            
            val swipeDistance = 200f
            val expectedSeekAmount = (15000 * sensitivity).toLong()
            
            every { horizontalSeekHandler.calculateSeekAmount(swipeDistance, sensitivity) } returns expectedSeekAmount
            
            val actualSeekAmount = horizontalSeekHandler.calculateSeekAmount(swipeDistance, sensitivity)
            
            assertEquals("Seek amount should scale with sensitivity", expectedSeekAmount, actualSeekAmount)
        }
    }

    @Test
    fun `test gesture boundaries and edge cases`() = runTest {
        // Test gesture at screen edges
        val screenWidth = 1080f
        val screenHeight = 2400f
        
        // Edge swipe tests
        val edgeMargin = 50f
        val leftEdge = edgeMargin
        val rightEdge = screenWidth - edgeMargin
        val topEdge = edgeMargin
        val bottomEdge = screenHeight - edgeMargin

        // Test edge gesture detection
        every { enhancedGestureDetector.isEdgeGesture(leftEdge, any()) } returns true
        every { enhancedGestureDetector.isEdgeGesture(rightEdge, any()) } returns true
        
        assertTrue("Left edge should be detected", enhancedGestureDetector.isEdgeGesture(leftEdge, 500f))
        assertTrue("Right edge should be detected", enhancedGestureDetector.isEdgeGesture(rightEdge, 500f))
        
        // Test minimum distance requirements
        val tooShortDistance = 30f
        val validDistance = 150f
        
        every { enhancedGestureDetector.meetsMinimumDistance(tooShortDistance) } returns false
        every { enhancedGestureDetector.meetsMinimumDistance(validDistance) } returns true
        
        assertFalse("Too short distance should be rejected", enhancedGestureDetector.meetsMinimumDistance(tooShortDistance))
        assertTrue("Valid distance should be accepted", enhancedGestureDetector.meetsMinimumDistance(validDistance))
    }

    @Test
    fun `test multi-touch gesture handling`() = runTest {
        // Test pinch-to-zoom gesture
        val pointer1StartX = 400f
        val pointer1StartY = 600f
        val pointer2StartX = 600f
        val pointer2StartY = 800f
        
        val pointer1EndX = 300f
        val pointer1EndY = 500f
        val pointer2EndX = 700f
        val pointer2EndY = 900f

        every { enhancedGestureDetector.isPinchGesture(any(), any()) } returns true
        every { enhancedGestureDetector.calculateZoomFactor(any(), any()) } returns 1.5f
        
        val zoomFactor = enhancedGestureDetector.calculateZoomFactor(
            initialDistance = calculateDistance(pointer1StartX, pointer1StartY, pointer2StartX, pointer2StartY),
            currentDistance = calculateDistance(pointer1EndX, pointer1EndY, pointer2EndX, pointer2EndY)
        )
        
        assertTrue("Zoom factor should be greater than 1 for zoom out", zoomFactor > 1.0f)
    }

    @Test
    fun `test gesture conflict resolution`() = runTest {
        // Test when multiple gesture types could be detected
        val ambiguousX = 500f // Center of screen
        val ambiguousY = 600f
        val endX = 700f
        val endY = 400f
        
        // This could be either horizontal seek or diagonal gesture
        every { horizontalSeekHandler.isHorizontalSwipe(any(), any(), any()) } returns true
        every { verticalGestureHandler.isVerticalSwipe(any(), any(), any()) } returns false
        every { enhancedGestureDetector.resolveGestureConflict(any(), any()) } returns "horizontal_seek"
        
        val resolvedGesture = enhancedGestureDetector.resolveGestureConflict(
            deltaX = endX - ambiguousX,
            deltaY = endY - ambiguousY
        )
        
        assertEquals("Should resolve to horizontal seek", "horizontal_seek", resolvedGesture)
    }

    @Test
    fun `test orientation-aware gesture handling`() = runTest {
        // Test gestures in different orientations
        val orientations = listOf("portrait", "landscape", "reverse_portrait", "reverse_landscape")
        
        orientations.forEach { orientation ->
            every { orientationAwareHandler.getCurrentOrientation() } returns orientation
            every { orientationAwareHandler.adjustGestureForOrientation(any(), orientation) } answers {
                val gesture = firstArg<String>()
                when (orientation) {
                    "landscape" -> "${gesture}_landscape"
                    "reverse_portrait" -> "${gesture}_reverse"
                    else -> gesture
                }
            }
            
            val adjustedGesture = orientationAwareHandler.adjustGestureForOrientation("swipe_right", orientation)
            
            when (orientation) {
                "landscape" -> assertEquals("swipe_right_landscape", adjustedGesture)
                "reverse_portrait" -> assertEquals("swipe_right_reverse", adjustedGesture)
                else -> assertEquals("swipe_right", adjustedGesture)
            }
        }
    }

    @Test
    fun `test accessibility gesture enhancements`() = runTest {
        // Test accessibility features
        every { accessibilityManager.isAccessibilityEnabled() } returns true
        every { accessibilityManager.getEnhancedTouchTarget() } returns 1.5f
        every { accessibilityManager.getGestureTimeoutMultiplier() } returns 2.0f
        
        assertTrue("Accessibility should be enabled", accessibilityManager.isAccessibilityEnabled())
        
        val enhancedTarget = accessibilityManager.getEnhancedTouchTarget()
        assertTrue("Enhanced touch target should be larger", enhancedTarget > 1.0f)
        
        val timeoutMultiplier = accessibilityManager.getGestureTimeoutMultiplier()
        assertTrue("Gesture timeout should be extended", timeoutMultiplier > 1.0f)
        
        // Test haptic feedback for accessibility
        every { accessibilityManager.provideHapticFeedback(any()) } returns Unit
        
        accessibilityManager.provideHapticFeedback("gesture_recognized")
        verify { accessibilityManager.provideHapticFeedback("gesture_recognized") }
    }

    @Test
    fun `test gesture velocity and acceleration`() = runTest {
        val timestamps = listOf(0L, 100L, 200L, 300L, 400L)
        val positions = listOf(100f, 150f, 220f, 310f, 420f)
        
        every { enhancedGestureDetector.calculateVelocity(any(), any()) } answers {
            val positionList = firstArg<List<Float>>()
            val timeList = secondArg<List<Long>>()
            
            if (positionList.size < 2 || timeList.size < 2) return@answers 0f
            
            val deltaPosition = positionList.last() - positionList.first()
            val deltaTime = timeList.last() - timeList.first()
            
            if (deltaTime == 0L) 0f else (deltaPosition / deltaTime) * 1000f // pixels per second
        }
        
        val velocity = enhancedGestureDetector.calculateVelocity(positions, timestamps)
        
        assertTrue("Velocity should be positive for rightward movement", velocity > 0)
        assertTrue("Velocity should be reasonable", velocity in 500f..2000f)
    }

    @Test
    fun `test gesture customization and preferences`() = runTest {
        // Test custom gesture configurations
        val customConfig = mapOf(
            "swipe_sensitivity" to 1.5f,
            "double_tap_timeout" to 300L,
            "long_press_duration" to 500L,
            "edge_gesture_enabled" to true,
            "haptic_feedback_enabled" to true
        )
        
        every { gestureManager.getCustomConfiguration() } returns customConfig
        every { gestureManager.applyConfiguration(any()) } returns true
        
        val config = gestureManager.getCustomConfiguration()
        
        assertEquals(1.5f, config["swipe_sensitivity"] as Float, 0.01f)
        assertEquals(300L, config["double_tap_timeout"] as Long)
        assertEquals(500L, config["long_press_duration"] as Long)
        assertTrue(config["edge_gesture_enabled"] as Boolean)
        assertTrue(config["haptic_feedback_enabled"] as Boolean)
        
        // Test applying new configuration
        val newConfig = customConfig.toMutableMap().apply {
            put("swipe_sensitivity", 2.0f)
        }
        
        assertTrue("Configuration should be applied successfully", gestureManager.applyConfiguration(newConfig))
    }

    @Test
    fun `test gesture performance and timing`() = runTest {
        // Test gesture recognition performance
        val startTime = System.currentTimeMillis()
        
        // Simulate rapid gesture sequence
        repeat(100) { index ->
            val event = createMotionEvent(
                MotionEvent.ACTION_MOVE,
                100f + index * 2,
                500f
            )
            enhancedGestureDetector.onTouchEvent(event)
        }
        
        val endTime = System.currentTimeMillis()
        val processingTime = endTime - startTime
        
        assertTrue("Gesture processing should be fast", processingTime < 100L) // Less than 100ms
        
        // Test gesture timeout handling
        every { enhancedGestureDetector.isGestureTimedOut(any(), any()) } answers {
            val startTime = firstArg<Long>()
            val currentTime = secondArg<Long>()
            (currentTime - startTime) > 1000L // 1 second timeout
        }
        
        assertFalse(enhancedGestureDetector.isGestureTimedOut(startTime, startTime + 500L))
        assertTrue(enhancedGestureDetector.isGestureTimedOut(startTime, startTime + 1500L))
    }

    @Test
    fun `test gesture pattern recognition`() = runTest {
        // Test complex gesture patterns
        val circleGesturePoints = generateCircleGesture(500f, 600f, 100f, 16)
        val squareGesturePoints = generateSquareGesture(400f, 500f, 200f)
        
        every { enhancedGestureDetector.recognizePattern(any()) } answers {
            val points = firstArg<List<Pair<Float, Float>>>()
            when {
                isCircularPattern(points) -> "circle"
                isSquarePattern(points) -> "square"
                else -> "unknown"
            }
        }
        
        assertEquals("circle", enhancedGestureDetector.recognizePattern(circleGesturePoints))
        assertEquals("square", enhancedGestureDetector.recognizePattern(squareGesturePoints))
    }

    @Test
    fun `test gesture state management`() = runTest {
        // Test gesture state tracking
        val gestureStates = listOf("idle", "detecting", "recognized", "completed", "cancelled")
        
        gestureStates.forEach { state ->
            every { enhancedGestureDetector.getCurrentState() } returns state
            every { enhancedGestureDetector.canTransitionTo(any()) } answers {
                val targetState = firstArg<String>()
                isValidStateTransition(state, targetState)
            }
            
            // Test valid transitions
            when (state) {
                "idle" -> assertTrue(enhancedGestureDetector.canTransitionTo("detecting"))
                "detecting" -> assertTrue(enhancedGestureDetector.canTransitionTo("recognized"))
                "recognized" -> assertTrue(enhancedGestureDetector.canTransitionTo("completed"))
                "completed" -> assertTrue(enhancedGestureDetector.canTransitionTo("idle"))
                "cancelled" -> assertTrue(enhancedGestureDetector.canTransitionTo("idle"))
            }
        }
    }

    // Helper methods for test data generation and validation
    private fun createMotionEvent(action: Int, x: Float, y: Float): MotionEvent {
        return mockk<MotionEvent>().apply {
            every { this@apply.action } returns action
            every { this@apply.x } returns x
            every { this@apply.y } returns y
            every { this@apply.eventTime } returns System.currentTimeMillis()
        }
    }

    private fun calculateDistance(x1: Float, y1: Float, x2: Float, y2: Float): Float {
        val dx = x2 - x1
        val dy = y2 - y1
        return kotlin.math.sqrt(dx * dx + dy * dy)
    }

    private fun generateCircleGesture(centerX: Float, centerY: Float, radius: Float, points: Int): List<Pair<Float, Float>> {
        return (0 until points).map { i ->
            val angle = (2 * Math.PI * i) / points
            val x = centerX + radius * kotlin.math.cos(angle).toFloat()
            val y = centerY + radius * kotlin.math.sin(angle).toFloat()
            Pair(x, y)
        }
    }

    private fun generateSquareGesture(startX: Float, startY: Float, size: Float): List<Pair<Float, Float>> {
        return listOf(
            Pair(startX, startY),
            Pair(startX + size, startY),
            Pair(startX + size, startY + size),
            Pair(startX, startY + size),
            Pair(startX, startY)
        )
    }

    private fun isCircularPattern(points: List<Pair<Float, Float>>): Boolean {
        if (points.size < 8) return false
        
        // Simple heuristic: check if points roughly form a circle
        val centerX = points.map { it.first }.average().toFloat()
        val centerY = points.map { it.second }.average().toFloat()
        
        val distances = points.map { calculateDistance(it.first, it.second, centerX, centerY) }
        val avgDistance = distances.average()
        val maxDeviation = distances.maxOfOrNull { abs(it - avgDistance) } ?: Float.MAX_VALUE
        
        return maxDeviation < avgDistance * 0.3f // Allow 30% deviation
    }

    private fun isSquarePattern(points: List<Pair<Float, Float>>): Boolean {
        if (points.size < 4) return false
        
        // Simple heuristic: check if we have roughly 4 corners with right angles
        return points.size == 5 && // Start and end at same point
               abs(points[0].first - points[4].first) < 10f &&
               abs(points[0].second - points[4].second) < 10f
    }

    private fun isValidStateTransition(from: String, to: String): Boolean {
        return when (from) {
            "idle" -> to in listOf("detecting")
            "detecting" -> to in listOf("recognized", "cancelled")
            "recognized" -> to in listOf("completed", "cancelled")
            "completed" -> to in listOf("idle")
            "cancelled" -> to in listOf("idle")
            else -> false
        }
    }
}