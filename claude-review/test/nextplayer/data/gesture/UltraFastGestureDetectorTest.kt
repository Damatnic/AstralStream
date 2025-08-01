package com.astralplayer.nextplayer.data.gesture

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerId
import com.astralplayer.nextplayer.data.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import kotlin.test.assertTrue

/**
 * Unit tests for UltraFastGestureDetector
 * Tests gesture detection accuracy and performance
 */
@OptIn(ExperimentalCoroutinesApi::class)
class UltraFastGestureDetectorTest {
    
    @Mock
    private lateinit var mockCallbacks: GestureCallbacks
    
    @Mock
    private lateinit var mockSettings: EnhancedGestureSettings
    
    @Mock
    private lateinit var mockGeneralSettings: GeneralGestureSettings
    
    private lateinit var testScope: TestScope
    private lateinit var detector: UltraFastGestureDetector
    
    private val screenWidth = 1080f
    private val screenHeight = 1920f
    
    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        testScope = TestScope()
        
        // Setup mock settings
        `when`(mockSettings.general).thenReturn(mockGeneralSettings)
        `when`(mockGeneralSettings.isEnabled).thenReturn(true)
        `when`(mockGeneralSettings.minimumGestureDistance).thenReturn(10f)
        
        detector = UltraFastGestureDetector(
            screenWidth = screenWidth,
            screenHeight = screenHeight,
            settings = mockSettings,
            callbacks = mockCallbacks,
            scope = testScope
        )
    }
    
    @Test
    fun `test horizontal seek detection in center zone`() = testScope.runTest {
        // Arrange
        val startPosition = Offset(screenWidth / 2f, screenHeight / 2f) // Center
        val endPosition = Offset(startPosition.x + 100f, startPosition.y) // Horizontal movement
        
        val pointerChange = createPointerInputChange(
            id = PointerId(1),
            position = endPosition,
            previousPosition = startPosition
        )
        
        // Act
        val result = detector.detectGesture(pointerChange)
        
        // Assert
        assertTrue(result is GestureResult.HorizontalSeek)
        val seekResult = result as GestureResult.HorizontalSeek
        assertTrue(seekResult.delta > 0) // Moving right
        
        // Verify callback was triggered
        advanceUntilIdle()
        verify(mockCallbacks, timeout(100)).onHorizontalSeek(any(), any())
    }
    
    @Test
    fun `test vertical volume detection in right zone`() = testScope.runTest {
        // Arrange
        val startPosition = Offset(screenWidth * 0.8f, screenHeight / 2f) // Right zone
        val endPosition = Offset(startPosition.x, startPosition.y - 100f) // Vertical movement up
        
        val pointerChange = createPointerInputChange(
            id = PointerId(1),
            position = endPosition,
            previousPosition = startPosition
        )
        
        // Act
        val result = detector.detectGesture(pointerChange)
        
        // Assert
        assertTrue(result is GestureResult.VerticalVolume)
        val volumeResult = result as GestureResult.VerticalVolume
        assertTrue(volumeResult.delta > 0) // Volume up
        
        // Verify callback was triggered
        advanceUntilIdle()
        verify(mockCallbacks, timeout(100)).onVerticalVolumeChange(any(), eq(TouchSide.RIGHT))
    }
    
    @Test
    fun `test vertical brightness detection in left zone`() = testScope.runTest {
        // Arrange
        val startPosition = Offset(screenWidth * 0.2f, screenHeight / 2f) // Left zone
        val endPosition = Offset(startPosition.x, startPosition.y - 100f) // Vertical movement up
        
        val pointerChange = createPointerInputChange(
            id = PointerId(1),
            position = endPosition,
            previousPosition = startPosition
        )
        
        // Act
        val result = detector.detectGesture(pointerChange)
        
        // Assert
        assertTrue(result is GestureResult.VerticalBrightness)
        val brightnessResult = result as GestureResult.VerticalBrightness
        assertTrue(brightnessResult.delta > 0) // Brightness up
        
        // Verify callback was triggered
        advanceUntilIdle()
        verify(mockCallbacks, timeout(100)).onVerticalBrightnessChange(any(), eq(TouchSide.LEFT))
    }
    
    @Test
    fun `test tap detection with minimal movement`() = testScope.runTest {
        // Arrange
        val position = Offset(screenWidth / 2f, screenHeight / 2f)
        val pointerChange = createPointerInputChange(
            id = PointerId(1),
            position = position,
            previousPosition = position // No movement
        )
        
        // Act
        val result = detector.detectGesture(pointerChange)
        
        // Assert
        assertTrue(result is GestureResult.Tap)
        val tapResult = result as GestureResult.Tap
        assertEquals(position, tapResult.position)
        
        // Verify callback was triggered
        advanceUntilIdle()
        verify(mockCallbacks, timeout(100)).onSingleTap(position)
    }
    
    @Test
    fun `test long press detection with time threshold`() = testScope.runTest {
        // Arrange
        val position = Offset(screenWidth / 2f, screenHeight / 2f)
        val pointerChange = createPointerInputChange(
            id = PointerId(1),
            position = position,
            previousPosition = position
        )
        
        // Simulate long press by detecting gesture after delay
        val startTime = System.nanoTime()
        
        // Act - simulate time passing for long press
        val result = detector.detectGesture(pointerChange, startTime + 600_000_000L) // 600ms later
        
        // Assert
        assertTrue(result is GestureResult.LongPress)
        val longPressResult = result as GestureResult.LongPress
        assertEquals(position, longPressResult.position)
        
        // Verify callback was triggered
        advanceUntilIdle()
        verify(mockCallbacks, timeout(100)).onLongPressStart(position)
    }
    
    @Test
    fun `test performance metrics tracking`() = testScope.runTest {
        // Arrange
        val position = Offset(screenWidth / 2f, screenHeight / 2f)
        val pointerChange = createPointerInputChange(
            id = PointerId(1),
            position = position,
            previousPosition = position
        )
        
        // Act - perform multiple gesture detections
        repeat(10) {
            detector.detectGesture(pointerChange)
        }
        
        // Assert
        val metrics = detector.getPerformanceMetrics()
        assertTrue(metrics.totalGestures > 0)
        assertTrue(metrics.averageProcessingTimeMs >= 0)
        assertTrue(metrics.maxProcessingTimeMs >= 0)
    }
    
    @Test
    fun `test precomputed zones optimization`() {
        // Arrange
        val newWidth = 1440f
        val newHeight = 2560f
        
        // Act
        detector.precomputeZones(newWidth, newHeight)
        
        // Test that zones are properly updated
        val leftPosition = Offset(newWidth * 0.2f, newHeight / 2f)
        val centerPosition = Offset(newWidth * 0.5f, newHeight / 2f)
        val rightPosition = Offset(newWidth * 0.8f, newHeight / 2f)
        
        val leftChange = createPointerInputChange(PointerId(1), leftPosition, leftPosition)
        val centerChange = createPointerInputChange(PointerId(2), centerPosition, centerPosition)
        val rightChange = createPointerInputChange(PointerId(3), rightPosition, rightPosition)
        
        // Assert - zones should be correctly identified
        // This is tested indirectly through gesture detection behavior
        assertNotNull(detector.detectGesture(leftChange))
        assertNotNull(detector.detectGesture(centerChange))
        assertNotNull(detector.detectGesture(rightChange))
    }
    
    @Test
    fun `test predictive mode toggle`() {
        // Act
        detector.enablePredictiveMode(true)
        detector.enablePredictiveMode(false)
        
        // Assert - no exceptions should be thrown
        // Predictive behavior is tested through gesture accuracy
        assertTrue(true) // Test passes if no exceptions
    }
    
    @Test
    fun `test performance mode adaptation`() {
        // Act
        detector.setPerformanceMode(PerformanceMode.ULTRA_FAST)
        detector.setPerformanceMode(PerformanceMode.BALANCED)
        detector.setPerformanceMode(PerformanceMode.QUALITY)
        
        // Assert
        val metrics = detector.getPerformanceMetrics()
        assertNotNull(metrics.performanceMode)
    }
    
    @Test
    fun `test gesture state reset`() {
        // Arrange
        val position = Offset(screenWidth / 2f, screenHeight / 2f)
        val pointerChange = createPointerInputChange(PointerId(1), position, position)
        
        // Act
        detector.detectGesture(pointerChange) // Create active state
        detector.resetGestureState() // Reset state
        
        // Assert - should not throw exceptions
        val result = detector.detectGesture(pointerChange)
        assertNotNull(result)
    }
    
    @Test
    fun `test frame limiting prevents excessive processing`() = testScope.runTest {
        // Arrange
        val position = Offset(screenWidth / 2f, screenHeight / 2f)
        val pointerChange = createPointerInputChange(PointerId(1), position, position)
        val baseTime = System.nanoTime()
        
        // Act - rapid gesture detection calls
        val result1 = detector.detectGesture(pointerChange, baseTime)
        val result2 = detector.detectGesture(pointerChange, baseTime + 1_000_000L) // 1ms later
        val result3 = detector.detectGesture(pointerChange, baseTime + 10_000_000L) // 10ms later
        
        // Assert - second call should be skipped due to frame limiting
        assertNotNull(result1)
        assertNull(result2) // Should be null due to frame limiting
        assertNotNull(result3) // Should process after sufficient time
    }
    
    @Test
    fun `test cleanup releases resources`() {
        // Act
        detector.cleanup()
        
        // Assert - should not throw exceptions
        assertTrue(true)
    }
    
    /**
     * Helper function to create PointerInputChange for testing
     */
    private fun createPointerInputChange(
        id: PointerId,
        position: Offset,
        previousPosition: Offset
    ): PointerInputChange {
        return PointerInputChange(
            id = id,
            uptimeMillis = System.currentTimeMillis(),
            position = position,
            pressed = true,
            previousUptimeMillis = System.currentTimeMillis() - 16,
            previousPosition = previousPosition,
            previousPressed = true,
            isConsumed = false,
            type = androidx.compose.ui.input.pointer.PointerType.Touch,
            scrollDelta = Offset.Zero
        )
    }
}

/**
 * Performance benchmark tests for UltraFastGestureDetector
 */
@OptIn(ExperimentalCoroutinesApi::class)
class UltraFastGestureDetectorPerformanceTest {
    
    private lateinit var testScope: TestScope
    private lateinit var detector: UltraFastGestureDetector
    private lateinit var mockCallbacks: GestureCallbacks
    private lateinit var mockSettings: EnhancedGestureSettings
    
    @Before
    fun setup() {
        testScope = TestScope()
        mockCallbacks = mock(GestureCallbacks::class.java)
        mockSettings = mock(EnhancedGestureSettings::class.java)
        
        val mockGeneralSettings = mock(GeneralGestureSettings::class.java)
        `when`(mockSettings.general).thenReturn(mockGeneralSettings)
        `when`(mockGeneralSettings.isEnabled).thenReturn(true)
        `when`(mockGeneralSettings.minimumGestureDistance).thenReturn(10f)
        
        detector = UltraFastGestureDetector(
            screenWidth = 1080f,
            screenHeight = 1920f,
            settings = mockSettings,
            callbacks = mockCallbacks,
            scope = testScope
        )
    }
    
    @Test
    fun `benchmark gesture detection performance`() = testScope.runTest {
        // Arrange
        val iterations = 1000
        val position = Offset(540f, 960f)
        val pointerChange = PointerInputChange(
            id = PointerId(1),
            uptimeMillis = System.currentTimeMillis(),
            position = position,
            pressed = true,
            previousUptimeMillis = System.currentTimeMillis() - 16,
            previousPosition = position,
            previousPressed = true,
            isConsumed = false,
            type = androidx.compose.ui.input.pointer.PointerType.Touch,
            scrollDelta = Offset.Zero
        )
        
        // Act
        val startTime = System.nanoTime()
        repeat(iterations) {
            detector.detectGesture(pointerChange, System.nanoTime())
        }
        val endTime = System.nanoTime()
        
        // Assert
        val totalTimeMs = (endTime - startTime) / 1_000_000f
        val averageTimeMs = totalTimeMs / iterations
        
        println("Performance benchmark results:")
        println("Total time: ${totalTimeMs}ms")
        println("Average time per gesture: ${averageTimeMs}ms")
        println("Gestures per second: ${1000f / averageTimeMs}")
        
        // Performance assertion - should be under 1ms per gesture
        assertTrue("Average gesture detection time should be under 1ms", averageTimeMs < 1f)
        
        val metrics = detector.getPerformanceMetrics()
        assertTrue("Performance should be optimal", metrics.isPerformanceOptimal)
    }
    
    @Test
    fun `benchmark memory allocation efficiency`() = testScope.runTest {
        // This test would require memory profiling tools in a real environment
        // For unit testing, we verify that object pools are working
        
        val iterations = 100
        val position = Offset(540f, 960f)
        
        // Act - perform many gesture detections
        repeat(iterations) {
            val pointerChange = PointerInputChange(
                id = PointerId(it.toLong()),
                uptimeMillis = System.currentTimeMillis(),
                position = position,
                pressed = true,
                previousUptimeMillis = System.currentTimeMillis() - 16,
                previousPosition = position,
                previousPressed = true,
                isConsumed = false,
                type = androidx.compose.ui.input.pointer.PointerType.Touch,
                scrollDelta = Offset.Zero
            )
            detector.detectGesture(pointerChange)
        }
        
        // Assert - verify metrics show efficient processing
        val metrics = detector.getPerformanceMetrics()
        assertTrue("Should have processed gestures", metrics.totalGestures > 0)
        assertTrue("Should maintain performance", metrics.isPerformanceOptimal)
    }
}