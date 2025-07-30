package com.astralplayer.nextplayer.integration

import android.content.Context
import android.view.MotionEvent
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.astralplayer.nextplayer.data.gesture.EnhancedGestureDetector
import com.astralplayer.nextplayer.data.HapticFeedbackManager
import com.astralplayer.nextplayer.accessibility.AccessibilityManager
import com.astralplayer.nextplayer.accessibility.HapticFeedbackType
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.*
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertNotNull

/**
 * Integration tests for gesture recognition and haptic feedback coordination
 */
@RunWith(AndroidJUnit4::class)
class GestureHapticIntegrationTest {

    private lateinit var context: Context
    private lateinit var testScope: TestScope
    
    @Mock private lateinit var mockMotionEvent: MotionEvent
    
    private lateinit var gestureDetector: EnhancedGestureDetector
    private lateinit var hapticFeedbackManager: HapticFeedbackManager
    private lateinit var accessibilityManager: AccessibilityManager

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        context = ApplicationProvider.getApplicationContext()
        testScope = TestScope()
        
        gestureDetector = EnhancedGestureDetector(context)
        hapticFeedbackManager = HapticFeedbackManager(context)
        accessibilityManager = AccessibilityManager(context)
    }

    @After
    fun tearDown() {
        runTest {
            gestureDetector.cleanup()
            hapticFeedbackManager.cleanup()
            accessibilityManager.cleanup()
        }
    }

    @Test
    fun testGestureHapticCoordination() = runTest {
        // Given - Initialize components
        gestureDetector.initialize()
        hapticFeedbackManager.initialize()
        gestureDetector.enableHapticFeedback(true)
        
        // When - Perform gesture that should trigger haptic feedback
        val gestureResult = gestureDetector.detectGesture(
            startX = 300f,
            startY = 400f,
            endX = 300f,
            endY = 200f,
            velocityY = -1200f
        )
        advanceUntilIdle()
        
        // Then - Verify gesture-haptic coordination
        assertTrue("Gesture should be detected", gestureResult.isRecognized)
        assertEquals("Should be volume up gesture", "volume_up", gestureResult.type)
        
        val hapticEvents = mutableListOf<String>()
        hapticFeedbackManager.feedbackEvents.collect { event ->
            hapticEvents.add(event.toString())
        }
        
        assertTrue("Haptic feedback should be triggered",
                  hapticEvents.any { it.contains("HapticTriggered") })
    }

    @Test
    fun testAccessibilityGestureHapticIntegration() = runTest {
        // Given - Accessibility mode enabled
        gestureDetector.initialize()
        hapticFeedbackManager.initialize()
        accessibilityManager.initialize()
        
        gestureDetector.enableAccessibilityMode(true)
        gestureDetector.setAccessibilityManager(accessibilityManager)
        
        // When - Perform accessibility gesture
        val accessibilityGesture = gestureDetector.handleAccessibilityGesture(
            gestureType = "double_tap",
            startX = 400f,
            startY = 300f,
            endX = 400f,
            endY = 300f
        )
        advanceUntilIdle()
        
        // Then - Verify accessibility-haptic coordination
        assertTrue("Accessibility gesture should be recognized", 
                  accessibilityGesture.isRecognized)
        assertEquals("Should trigger play/pause", "play_pause", accessibilityGesture.action)
        
        // Check for both haptic and accessibility feedback
        val hapticEvents = mutableListOf<String>()
        hapticFeedbackManager.feedbackEvents.collect { event ->
            hapticEvents.add(event.toString())
        }
        
        val accessibilityEvents = mutableListOf<String>()
        accessibilityManager.accessibilityEvents.collect { event ->
            accessibilityEvents.add(event.toString())
        }
        
        assertTrue("Should provide haptic feedback for accessibility gesture",
                  hapticEvents.any { it.contains("HapticTriggered") })
        assertTrue("Should provide audio feedback for accessibility",
                  accessibilityEvents.any { it.contains("AudioFeedbackProvided") })
    }

    @Test
    fun testHapticIntensityGestureIntegration() = runTest {
        // Given - Different gesture intensities
        gestureDetector.initialize()
        hapticFeedbackManager.initialize()
        
        // Configure different haptic intensities for different gestures
        gestureDetector.enableHapticFeedback(true)
        gestureDetector.configureHapticIntensity(
            lightGestures = listOf("tap", "light_swipe"),
            mediumGestures = listOf("swipe", "seek"),
            strongGestures = listOf("long_press", "fast_swipe")
        )
        
        // When - Perform light gesture
        val lightGesture = gestureDetector.detectGesture(
            startX = 200f,
            startY = 300f,
            endX = 250f,
            endY = 300f,
            velocityX = 200f
        )
        advanceUntilIdle()
        
        // Then - Should trigger light haptic
        assertTrue("Light gesture should be detected", lightGesture.isRecognized)
        
        // When - Perform strong gesture
        val strongGesture = gestureDetector.detectGesture(
            startX = 300f,
            startY = 400f,
            endX = 600f,
            endY = 400f,
            velocityX = 2000f
        )
        advanceUntilIdle()
        
        // Then - Should trigger strong haptic
        assertTrue("Strong gesture should be detected", strongGesture.isRecognized)
        
        val hapticEvents = mutableListOf<String>()
        hapticFeedbackManager.feedbackEvents.collect { event ->
            hapticEvents.add(event.toString())
        }
        
        assertTrue("Should have multiple haptic events with different intensities",
                  hapticEvents.size >= 2)
    }

    @Test
    fun testMultiTouchHapticIntegration() = runTest {
        // Given - Multi-touch gesture support
        gestureDetector.initialize()
        hapticFeedbackManager.initialize()
        
        gestureDetector.enableMultiTouchGestures(true)
        gestureDetector.enableHapticFeedback(true)
        
        // When - Perform multi-touch gesture
        val multiTouchGesture = gestureDetector.detectMultiTouchGesture(
            pointer1X = 200f, pointer1Y = 300f,
            pointer2X = 400f, pointer2Y = 300f,
            finalDistance = 600f // Pinch out gesture
        )
        advanceUntilIdle()
        
        // Then - Verify multi-touch haptic feedback
        assertTrue("Multi-touch gesture should be detected", multiTouchGesture.isRecognized)
        assertEquals("Should be zoom gesture", "zoom", multiTouchGesture.type)
        
        val hapticEvents = mutableListOf<String>()
        hapticFeedbackManager.feedbackEvents.collect { event ->
            hapticEvents.add(event.toString())
        }
        
        assertTrue("Should provide haptic feedback for multi-touch",
                  hapticEvents.any { it.contains("HapticTriggered") &&
                                    it.contains("multi_touch", ignoreCase = true) })
    }

    @Test
    fun testGestureSequenceHapticIntegration() = runTest {
        // Given - Gesture sequence detection
        gestureDetector.initialize()
        hapticFeedbackManager.initialize()
        
        gestureDetector.enableGestureSequences(true)
        gestureDetector.enableHapticFeedback(true)
        
        // When - Perform gesture sequence (double tap then swipe)
        val firstGesture = gestureDetector.detectGesture(
            startX = 300f, startY = 300f,
            endX = 300f, endY = 300f,
            velocityX = 0f, velocityY = 0f
        ) // Tap
        
        delay(100) // Short delay
        
        val secondGesture = gestureDetector.detectGesture(
            startX = 300f, startY = 300f,
            endX = 300f, endY = 300f,
            velocityX = 0f, velocityY = 0f
        ) // Second tap
        
        delay(100)
        
        val swipeGesture = gestureDetector.detectGesture(
            startX = 300f, startY = 300f,
            endX = 500f, endY = 300f,
            velocityX = 1000f, velocityY = 0f
        ) // Swipe right
        
        advanceUntilIdle()
        
        // Then - Verify sequence haptic feedback
        assertTrue("Gesture sequence should be detected", 
                  firstGesture.isRecognized && secondGesture.isRecognized && swipeGesture.isRecognized)
        
        val hapticEvents = mutableListOf<String>()
        hapticFeedbackManager.feedbackEvents.collect { event ->
            hapticEvents.add(event.toString())
        }
        
        assertTrue("Should provide haptic feedback for gesture sequence",
                  hapticEvents.size >= 3) // At least one for each gesture
    }

    @Test
    fun testHapticFeedbackCustomization() = runTest {
        // Given - Customized haptic feedback
        gestureDetector.initialize()
        hapticFeedbackManager.initialize()
        
        // Configure custom haptic patterns
        hapticFeedbackManager.configureCustomFeedback(
            gestureType = "seek_forward",
            feedbackType = HapticFeedbackType.SUCCESS,
            intensity = 0.8f,
            duration = 150L
        )
        
        hapticFeedbackManager.configureCustomFeedback(
            gestureType = "seek_backward", 
            feedbackType = HapticFeedbackType.MEDIUM,
            intensity = 0.6f,
            duration = 100L
        )
        
        gestureDetector.enableHapticFeedback(true)
        
        // When - Perform seek gestures
        val forwardSeek = gestureDetector.detectGesture(
            startX = 200f, startY = 300f,
            endX = 400f, endY = 300f,
            velocityX = 800f, velocityY = 0f
        )
        
        delay(200)
        
        val backwardSeek = gestureDetector.detectGesture(
            startX = 400f, startY = 300f,
            endX = 200f, endY = 300f,
            velocityX = -800f, velocityY = 0f
        )
        
        advanceUntilIdle()
        
        // Then - Verify customized haptic feedback
        assertTrue("Forward seek should be detected", forwardSeek.isRecognized)
        assertTrue("Backward seek should be detected", backwardSeek.isRecognized)
        
        val hapticEvents = mutableListOf<String>()
        hapticFeedbackManager.feedbackEvents.collect { event ->
            hapticEvents.add(event.toString())
        }
        
        assertTrue("Should use custom haptic feedback",
                  hapticEvents.any { it.contains("SUCCESS") || it.contains("MEDIUM") })
    }

    @Test
    fun testHapticFeedbackAccessibilityIntegration() = runTest {
        // Given - Accessibility-aware haptic feedback
        gestureDetector.initialize()
        hapticFeedbackManager.initialize()
        accessibilityManager.initialize()
        
        // Enable accessibility mode
        accessibilityManager.configureReducedMotionMode(true)
        hapticFeedbackManager.setAccessibilityMode(true)
        gestureDetector.setAccessibilityManager(accessibilityManager)
        
        // When - Perform gesture with accessibility considerations
        val accessibleGesture = gestureDetector.handleAccessibilityGesture(
            gestureType = "swipe_up",
            startX = 300f, startY = 500f,
            endX = 300f, endY = 200f
        )
        advanceUntilIdle()
        
        // Then - Verify accessibility-aware haptic feedback
        assertTrue("Accessibility gesture should be handled", accessibleGesture.isRecognized)
        assertEquals("Should trigger volume up", "volume_up", accessibleGesture.action)
        
        val hapticEvents = mutableListOf<String>()
        hapticFeedbackManager.feedbackEvents.collect { event ->
            hapticEvents.add(event.toString())
        }
        
        val accessibilityEvents = mutableListOf<String>()
        accessibilityManager.accessibilityEvents.collect { event ->
            accessibilityEvents.add(event.toString())
        }
        
        assertTrue("Should provide enhanced haptic feedback for accessibility",
                  hapticEvents.any { it.contains("accessibility", ignoreCase = true) })
        assertTrue("Should coordinate with accessibility announcements",
                  accessibilityEvents.any { it.contains("AudioFeedbackProvided") })
    }

    @Test
    fun testPerformanceUnderHapticLoad() = runTest {
        // Given - Performance test scenario
        gestureDetector.initialize()
        hapticFeedbackManager.initialize()
        
        gestureDetector.enableHapticFeedback(true)
        
        val startTime = System.currentTimeMillis()
        
        // When - Perform many rapid gestures
        repeat(100) { index ->
            val gesture = gestureDetector.detectGesture(
                startX = (100 + index * 2).toFloat(),
                startY = 300f,
                endX = (200 + index * 2).toFloat(),
                endY = 300f,
                velocityX = 500f,
                velocityY = 0f
            )
            
            if (index % 10 == 0) {
                delay(10) // Brief pause every 10 gestures
            }
        }
        
        val endTime = System.currentTimeMillis()
        val duration = endTime - startTime
        
        // Then - Verify performance under load
        assertTrue("Should handle rapid gestures efficiently", duration < 10_000) // 10 seconds
        
        val hapticEvents = mutableListOf<String>()
        hapticFeedbackManager.feedbackEvents.collect { event ->
            hapticEvents.add(event.toString())
        }
        
        assertTrue("Should provide haptic feedback for multiple gestures",
                  hapticEvents.size >= 50) // At least half should trigger haptic
        
        // Verify system remains responsive
        val finalGesture = gestureDetector.detectGesture(
            startX = 300f, startY = 300f,
            endX = 300f, endY = 100f,
            velocityX = 0f, velocityY = -1000f
        )
        
        assertTrue("System should remain responsive after load", finalGesture.isRecognized)
    }
}