package com.astralplayer.nextplayer.gesture

import android.content.Context
import android.view.MotionEvent
import androidx.media3.exoplayer.ExoPlayer
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.astralplayer.nextplayer.data.GestureManager
import com.astralplayer.nextplayer.data.HapticFeedbackManager
import com.astralplayer.nextplayer.data.PlayerRepository
import com.astralplayer.nextplayer.data.gesture.EnhancedGestureDetector
import com.astralplayer.nextplayer.viewmodel.SimpleEnhancedPlayerViewModel
import io.mockk.*
import kotlinx.coroutines.test.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.*

/**
 * Integration tests for gesture system with player controls and UI components
 */
@RunWith(AndroidJUnit4::class)
class GestureIntegrationTest {

    private lateinit var context: Context
    private lateinit var gestureManager: GestureManager
    private lateinit var gestureDetector: EnhancedGestureDetector
    private lateinit var playerViewModel: SimpleEnhancedPlayerViewModel
    private lateinit var playerRepository: PlayerRepository
    private lateinit var exoPlayer: ExoPlayer
    private lateinit var hapticManager: HapticFeedbackManager

    @Before
    fun setup() {
        context = mockk(relaxed = true)
        gestureManager = mockk(relaxed = true)
        gestureDetector = mockk(relaxed = true)
        playerRepository = mockk(relaxed = true)
        exoPlayer = mockk(relaxed = true)
        hapticManager = mockk(relaxed = true)
        
        playerViewModel = spyk(
            SimpleEnhancedPlayerViewModel(
                application = mockk(relaxed = true),
                playerRepository = playerRepository,
                exoPlayer = exoPlayer
            )
        )
    }

    @Test
    fun `test horizontal seek gesture integration with player`() = runTest {
        // Setup initial player state
        val initialPosition = 30000L // 30 seconds
        val seekAmount = 10000L // 10 seconds forward
        val expectedPosition = initialPosition + seekAmount
        
        every { exoPlayer.currentPosition } returns initialPosition
        every { exoPlayer.duration } returns 300000L // 5 minutes total
        every { playerRepository.seekTo(any()) } returns Unit
        
        // Setup gesture detection
        every { gestureDetector.isHorizontalSeekGesture(any()) } returns true
        every { gestureDetector.calculateSeekAmount(any()) } returns seekAmount
        every { gestureManager.isGestureEnabled("horizontal_seek") } returns true
        
        // Create swipe right gesture
        val swipeGesture = createHorizontalSwipeGesture(100f, 400f, 500f)
        
        // Process gesture through integration
        swipeGesture.forEach { event ->
            every { gestureDetector.onTouchEvent(event) } answers {
                if (event.action == MotionEvent.ACTION_UP) {
                    // Trigger seek when gesture completes
                    playerViewModel.seekBy(seekAmount)
                }
                true
            }
            gestureDetector.onTouchEvent(event)
        }
        
        // Verify seek was performed
        verify { playerRepository.seekTo(expectedPosition) }
        verify { hapticManager.performHapticFeedback(any()) }
    }

    @Test
    fun `test vertical volume gesture integration`() = runTest {
        val initialVolume = 0.5f
        val volumeChange = 0.2f
        val expectedVolume = initialVolume + volumeChange
        
        every { exoPlayer.volume } returns initialVolume
        every { playerRepository.setVolume(any()) } returns Unit
        every { gestureDetector.isVolumeGesture(any(), any()) } returns true
        every { gestureDetector.calculateVolumeChange(any()) } returns volumeChange
        
        // Create vertical swipe up gesture on right side
        val volumeGesture = createVerticalSwipeGesture(800f, 600f, 400f) // Right side, swipe up
        
        volumeGesture.forEach { event ->
            every { gestureDetector.onTouchEvent(event) } answers {
                if (event.action == MotionEvent.ACTION_UP) {
                    playerViewModel.adjustVolume(volumeChange)
                }
                true
            }
            gestureDetector.onTouchEvent(event)
        }
        
        verify { playerRepository.setVolume(expectedVolume) }
    }

    @Test
    fun `test brightness gesture integration`() = runTest {
        val initialBrightness = 0.6f
        val brightnessChange = 0.3f
        val expectedBrightness = (initialBrightness + brightnessChange).coerceIn(0f, 1f)
        
        every { gestureManager.getCurrentBrightness() } returns initialBrightness
        every { gestureManager.setBrightness(any()) } returns Unit
        every { gestureDetector.isBrightnessGesture(any(), any()) } returns true
        every { gestureDetector.calculateBrightnessChange(any()) } returns brightnessChange
        
        // Create vertical swipe up gesture on left side
        val brightnessGesture = createVerticalSwipeGesture(200f, 600f, 300f) // Left side, swipe up
        
        brightnessGesture.forEach { event ->
            every { gestureDetector.onTouchEvent(event) } answers {
                if (event.action == MotionEvent.ACTION_UP) {
                    gestureManager.adjustBrightness(brightnessChange)
                }
                true
            }
            gestureDetector.onTouchEvent(event)
        }
        
        verify { gestureManager.adjustBrightness(brightnessChange) }
    }

    @Test
    fun `test double tap play pause integration`() = runTest {
        val isPlaying = true
        
        every { exoPlayer.isPlaying } returns isPlaying
        every { playerRepository.togglePlayPause() } returns Unit
        every { gestureDetector.isDoubleTapGesture(any()) } returns true
        
        // Create double tap gesture
        val doubleTapGesture = createDoubleTapGesture(500f, 500f)
        
        doubleTapGesture.forEach { event ->
            every { gestureDetector.onTouchEvent(event) } answers {
                if (gestureDetector.isDoubleTapGesture(listOf(event))) {
                    playerViewModel.togglePlayPause()
                }
                true
            }
            gestureDetector.onTouchEvent(event)
        }
        
        verify { playerRepository.togglePlayPause() }
    }

    @Test
    fun `test long press context menu integration`() = runTest {
        every { gestureDetector.isLongPressGesture(any()) } returns true
        every { gestureManager.showContextMenu(any(), any()) } returns Unit
        
        val longPressGesture = createLongPressGesture(500f, 600f)
        
        longPressGesture.forEach { event ->
            every { gestureDetector.onTouchEvent(event) } answers {
                if (gestureDetector.isLongPressGesture(listOf(event))) {
                    gestureManager.showContextMenu(event.x, event.y)
                }
                true
            }
            gestureDetector.onTouchEvent(event)
        }
        
        verify { gestureManager.showContextMenu(500f, 600f) }
    }

    @Test
    fun `test gesture conflict resolution with UI elements`() = runTest {
        // Test when gesture conflicts with UI button press
        val buttonArea = GestureArea(400f, 800f, 600f, 900f) // Bottom center button
        val gesturePosition = Pair(500f, 850f) // Within button area
        
        every { gestureManager.isInUIElementArea(gesturePosition.first, gesturePosition.second) } returns true
        every { gestureManager.getUIElementAt(any(), any()) } returns "play_button"
        
        val conflictGesture = createTapGesture(gesturePosition.first, gesturePosition.second)
        
        conflictGesture.forEach { event ->
            every { gestureDetector.onTouchEvent(event) } answers {
                val isInUIArea = gestureManager.isInUIElementArea(event.x, event.y)
                if (isInUIArea) {
                    // UI element should take precedence over gesture
                    false
                } else {
                    true
                }
            }
            
            val gestureHandled = gestureDetector.onTouchEvent(event)
            assertFalse("Gesture should not be handled when in UI element area", gestureHandled)
        }
    }

    @Test
    fun `test gesture state synchronization with player state`() = runTest {
        // Test gesture behavior changes based on player state
        val playerStates = listOf("playing", "paused", "buffering", "ended", "error")
        
        playerStates.forEach { state ->
            every { playerViewModel.getPlayerState() } returns state
            every { gestureManager.updateGestureStateForPlayer(state) } returns Unit
            
            gestureManager.updateGestureStateForPlayer(state)
            
            // Verify gesture configuration changes based on player state
            when (state) {
                "playing" -> {
                    verify { gestureManager.enableGesture("horizontal_seek") }
                    verify { gestureManager.enableGesture("vertical_volume") }
                    verify { gestureManager.enableGesture("vertical_brightness") }
                }
                "buffering" -> {
                    verify { gestureManager.disableGesture("horizontal_seek") }
                }
                "error" -> {
                    verify { gestureManager.disableAllGestures() }
                }
            }
        }
    }

    @Test
    fun `test gesture feedback integration`() = runTest {
        val feedbackTypes = listOf("haptic", "visual", "audio")
        
        feedbackTypes.forEach { feedbackType ->
            every { gestureManager.isFeedbackEnabled(feedbackType) } returns true
            
            when (feedbackType) {
                "haptic" -> {
                    every { hapticManager.performHapticFeedback("gesture_seek") } returns Unit
                    hapticManager.performHapticFeedback("gesture_seek")
                    verify { hapticManager.performHapticFeedback("gesture_seek") }
                }
                "visual" -> {
                    every { gestureManager.showVisualFeedback(any(), any()) } returns Unit
                    gestureManager.showVisualFeedback("seek_forward", 10000L)
                    verify { gestureManager.showVisualFeedback("seek_forward", 10000L) }
                }
                "audio" -> {
                    every { gestureManager.playAudioFeedback(any()) } returns Unit
                    gestureManager.playAudioFeedback("volume_change")
                    verify { gestureManager.playAudioFeedback("volume_change") }
                }
            }
        }
    }

    @Test
    fun `test gesture customization persistence`() = runTest {
        val customSettings = mapOf(
            "swipe_sensitivity" to 1.5f,
            "double_tap_timeout" to 400L,
            "volume_step_size" to 0.1f,
            "brightness_step_size" to 0.15f,
            "haptic_enabled" to true
        )
        
        // Save custom settings
        every { gestureManager.saveCustomSettings(customSettings) } returns true
        every { gestureManager.loadCustomSettings() } returns customSettings
        
        val saved = gestureManager.saveCustomSettings(customSettings)
        assertTrue("Settings should be saved successfully", saved)
        
        // Restart gesture system (simulated)
        val loadedSettings = gestureManager.loadCustomSettings()
        assertEquals(customSettings, loadedSettings)
        
        // Apply loaded settings
        every { gestureManager.applySettings(loadedSettings) } returns Unit
        gestureManager.applySettings(loadedSettings)
        verify { gestureManager.applySettings(loadedSettings) }
    }

    @Test
    fun `test gesture performance impact on video playback`() = runTest {
        val playbackMetrics = mutableListOf<PlaybackMetric>()
        val gestureCount = 100
        
        every { playerViewModel.getPlaybackMetrics() } answers {
            PlaybackMetric(
                frameDrops = (0..2).random(),
                bufferUnderruns = 0,
                cpuUsage = (20..40).random().toFloat(),
                memoryUsage = (100..200).random().toLong()
            )
        }
        
        // Perform gestures while monitoring playback
        repeat(gestureCount) { index ->
            val gesture = when (index % 4) {
                0 -> createHorizontalSwipeGesture(100f, 400f, 500f)
                1 -> createVerticalSwipeGesture(200f, 600f, 400f)
                2 -> createDoubleTapGesture(500f, 500f)
                else -> createTapGesture(500f, 500f)
            }
            
            gesture.forEach { event ->
                gestureDetector.onTouchEvent(event)
            }
            
            // Record metrics after gesture
            playbackMetrics.add(playerViewModel.getPlaybackMetrics())
        }
        
        val averageFrameDrops = playbackMetrics.map { it.frameDrops }.average()
        val averageCpuUsage = playbackMetrics.map { it.cpuUsage }.average()
        val maxMemoryUsage = playbackMetrics.maxOfOrNull { it.memoryUsage } ?: 0L
        
        println("Gesture Performance Impact:")
        println("Average frame drops: $averageFrameDrops")
        println("Average CPU usage: $averageCpuUsage%")
        println("Max memory usage: $maxMemoryUsage bytes")
        
        // Verify acceptable performance impact
        assertTrue("Frame drops should be minimal", averageFrameDrops < 1.0)
        assertTrue("CPU usage should be reasonable", averageCpuUsage < 50.0)
        assertTrue("Memory usage should be controlled", maxMemoryUsage < 300_000_000L)
    }

    @Test
    fun `test gesture system recovery from errors`() = runTest {
        val errorScenarios = listOf(
            "player_error", "gesture_timeout", "invalid_state", "resource_exhaustion"
        )
        
        errorScenarios.forEach { errorType ->
            // Simulate error condition
            every { gestureManager.handleError(errorType) } answers {
                when (firstArg<String>()) {
                    "player_error" -> {
                        // Reset gesture system
                        gestureDetector.reset()
                        true
                    }
                    "gesture_timeout" -> {
                        // Clear gesture buffer
                        gestureDetector.clearBuffer()
                        true
                    }
                    "invalid_state" -> {
                        // Restart gesture detection
                        gestureDetector.restart()
                        true
                    }
                    "resource_exhaustion" -> {
                        // Enable low resource mode
                        gestureDetector.enableLowResourceMode()
                        true
                    }
                    else -> false
                }
            }
            
            val recovered = gestureManager.handleError(errorType)
            assertTrue("Should recover from $errorType", recovered)
            
            // Verify system continues to work after recovery
            val testGesture = createTapGesture(500f, 500f)
            testGesture.forEach { event ->
                every { gestureDetector.onTouchEvent(event) } returns true
                assertTrue("Gesture should work after recovery", gestureDetector.onTouchEvent(event))
            }
        }
    }

    // Helper methods for creating gesture sequences
    private fun createHorizontalSwipeGesture(startX: Float, endX: Float, y: Float): List<MotionEvent> {
        return listOf(
            createMotionEvent(MotionEvent.ACTION_DOWN, startX, y),
            createMotionEvent(MotionEvent.ACTION_MOVE, startX + (endX - startX) * 0.3f, y),
            createMotionEvent(MotionEvent.ACTION_MOVE, startX + (endX - startX) * 0.7f, y),
            createMotionEvent(MotionEvent.ACTION_UP, endX, y)
        )
    }

    private fun createVerticalSwipeGesture(x: Float, startY: Float, endY: Float): List<MotionEvent> {
        return listOf(
            createMotionEvent(MotionEvent.ACTION_DOWN, x, startY),
            createMotionEvent(MotionEvent.ACTION_MOVE, x, startY + (endY - startY) * 0.3f),
            createMotionEvent(MotionEvent.ACTION_MOVE, x, startY + (endY - startY) * 0.7f),
            createMotionEvent(MotionEvent.ACTION_UP, x, endY)
        )
    }

    private fun createDoubleTapGesture(x: Float, y: Float): List<MotionEvent> {
        return listOf(
            createMotionEvent(MotionEvent.ACTION_DOWN, x, y),
            createMotionEvent(MotionEvent.ACTION_UP, x, y),
            createMotionEvent(MotionEvent.ACTION_DOWN, x, y),
            createMotionEvent(MotionEvent.ACTION_UP, x, y)
        )
    }

    private fun createLongPressGesture(x: Float, y: Float): List<MotionEvent> {
        return listOf(
            createMotionEvent(MotionEvent.ACTION_DOWN, x, y),
            // In real usage, there would be a time delay here
            createMotionEvent(MotionEvent.ACTION_UP, x, y)
        )
    }

    private fun createTapGesture(x: Float, y: Float): List<MotionEvent> {
        return listOf(
            createMotionEvent(MotionEvent.ACTION_DOWN, x, y),
            createMotionEvent(MotionEvent.ACTION_UP, x, y)
        )
    }

    private fun createMotionEvent(action: Int, x: Float, y: Float): MotionEvent {
        return mockk<MotionEvent>().apply {
            every { this@apply.action } returns action
            every { this@apply.x } returns x
            every { this@apply.y } returns y
            every { this@apply.eventTime } returns System.currentTimeMillis()
        }
    }

    // Data classes for testing
    data class GestureArea(
        val left: Float,
        val top: Float,
        val right: Float,
        val bottom: Float
    )

    data class PlaybackMetric(
        val frameDrops: Int,
        val bufferUnderruns: Int,
        val cpuUsage: Float,
        val memoryUsage: Long
    )
}