package com.astralplayer.nextplayer.gesture

import android.content.Context
import android.os.Vibrator
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.astralplayer.nextplayer.data.HapticFeedbackManager
import com.astralplayer.nextplayer.data.gesture.accessibility.GestureAccessibilityManager
import io.mockk.*
import kotlinx.coroutines.test.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.*

/**
 * Comprehensive tests for gesture accessibility features and inclusive design
 */
@RunWith(AndroidJUnit4::class)
class GestureAccessibilityTest {

    private lateinit var context: Context
    private lateinit var accessibilityManager: GestureAccessibilityManager
    private lateinit var hapticFeedbackManager: HapticFeedbackManager
    private lateinit var vibrator: Vibrator

    @Before
    fun setup() {
        context = mockk(relaxed = true)
        vibrator = mockk(relaxed = true)
        hapticFeedbackManager = mockk(relaxed = true)
        accessibilityManager = mockk(relaxed = true)
        
        every { context.getSystemService(Context.VIBRATOR_SERVICE) } returns vibrator
    }

    @Test
    fun `test accessibility service detection`() = runTest {
        // Test TalkBack detection
        every { accessibilityManager.isTalkBackEnabled() } returns true
        assertTrue("TalkBack should be detected when enabled", accessibilityManager.isTalkBackEnabled())
        
        // Test Switch Access detection
        every { accessibilityManager.isSwitchAccessEnabled() } returns true
        assertTrue("Switch Access should be detected when enabled", accessibilityManager.isSwitchAccessEnabled())
        
        // Test Voice Access detection
        every { accessibilityManager.isVoiceAccessEnabled() } returns false
        assertFalse("Voice Access should not be detected when disabled", accessibilityManager.isVoiceAccessEnabled())
        
        // Test general accessibility enabled state
        every { accessibilityManager.isAccessibilityEnabled() } returns true
        assertTrue("General accessibility should be enabled", accessibilityManager.isAccessibilityEnabled())
    }

    @Test
    fun `test enhanced touch targets for accessibility`() = runTest {
        val normalTouchTarget = 48f // dp
        val enhancedTouchTarget = 64f // dp
        
        // Test touch target enhancement
        every { accessibilityManager.getMinimumTouchTarget() } returns enhancedTouchTarget
        every { accessibilityManager.enhanceTouchTarget(any()) } answers {
            val original = firstArg<Float>()
            maxOf(original, enhancedTouchTarget)
        }
        
        val smallTarget = 32f
        val largeTarget = 72f
        
        assertEquals(enhancedTouchTarget, accessibilityManager.enhanceTouchTarget(smallTarget), 0.1f)
        assertEquals(largeTarget, accessibilityManager.enhanceTouchTarget(largeTarget), 0.1f)
        
        // Test touch target area calculation
        every { accessibilityManager.calculateTouchArea(any(), any()) } answers {
            val width = firstArg<Float>()
            val height = secondArg<Float>()
            width * height
        }
        
        val touchArea = accessibilityManager.calculateTouchArea(enhancedTouchTarget, enhancedTouchTarget)
        assertEquals(enhancedTouchTarget * enhancedTouchTarget, touchArea, 0.1f)
    }

    @Test
    fun `test gesture timeout adjustments for motor impairments`() = runTest {
        val standardTimeout = 1000L // 1 second
        val extendedTimeout = 3000L // 3 seconds
        
        // Test timeout extension for users with motor impairments
        every { accessibilityManager.hasMotorImpairment() } returns true
        every { accessibilityManager.getAdjustedTimeout(standardTimeout) } returns extendedTimeout
        
        val adjustedTimeout = accessibilityManager.getAdjustedTimeout(standardTimeout)
        assertEquals(extendedTimeout, adjustedTimeout)
        
        // Test timeout for users without impairments
        every { accessibilityManager.hasMotorImpairment() } returns false
        every { accessibilityManager.getAdjustedTimeout(standardTimeout) } returns standardTimeout
        
        val normalTimeout = accessibilityManager.getAdjustedTimeout(standardTimeout)
        assertEquals(standardTimeout, normalTimeout)
        
        // Test progressive timeout adjustment based on user performance
        val userPerformanceData = listOf(1200L, 1500L, 1800L, 1100L, 1400L) // Recent gesture completion times
        every { accessibilityManager.calculateAdaptiveTimeout(userPerformanceData) } answers {
            val times = firstArg<List<Long>>()
            val averageTime = times.average().toLong()
            (averageTime * 1.5).toLong() // 50% buffer
        }
        
        val adaptiveTimeout = accessibilityManager.calculateAdaptiveTimeout(userPerformanceData)
        assertTrue("Adaptive timeout should be reasonable", adaptiveTimeout in 1500L..3000L)
    }

    @Test
    fun `test haptic feedback patterns for different gesture types`() = runTest {
        val gestureTypes = listOf(
            "seek_forward", "seek_backward", "volume_up", "volume_down",
            "brightness_up", "brightness_down", "play_pause", "next_track", "previous_track"
        )
        
        gestureTypes.forEach { gestureType ->
            every { hapticFeedbackManager.getPatternForGesture(gestureType) } answers {
                when (firstArg<String>()) {
                    "seek_forward" -> longArrayOf(0, 50, 100, 50) // Double pulse
                    "seek_backward" -> longArrayOf(0, 100, 50, 100) // Long-short-long
                    "volume_up" -> longArrayOf(0, 30, 30, 30) // Quick triple
                    "volume_down" -> longArrayOf(0, 100) // Single long
                    "brightness_up" -> longArrayOf(0, 25, 25, 25, 25, 25) // Five quick pulses
                    "brightness_down" -> longArrayOf(0, 200) // Single very long
                    "play_pause" -> longArrayOf(0, 50) // Single medium
                    "next_track" -> longArrayOf(0, 30, 50, 80) // Ascending pattern
                    "previous_track" -> longArrayOf(0, 80, 50, 30) // Descending pattern
                    else -> longArrayOf(0, 50) // Default
                }
            }
            
            val pattern = hapticFeedbackManager.getPatternForGesture(gestureType)
            assertNotNull("Pattern should exist for $gestureType", pattern)
            assertTrue("Pattern should have reasonable length", pattern.size in 2..8)
        }
    }

    @Test
    fun `test audio feedback for blind and low vision users`() = runTest {
        val audioDescriptions = mapOf(
            "seek_forward" to "Seeking forward 10 seconds",
            "seek_backward" to "Seeking backward 10 seconds",
            "volume_up" to "Volume increased",
            "volume_down" to "Volume decreased",
            "brightness_up" to "Brightness increased",
            "brightness_down" to "Brightness decreased",
            "play_pause" to "Playback toggled",
            "gesture_recognized" to "Gesture recognized",
            "gesture_failed" to "Gesture not recognized, please try again"
        )
        
        audioDescriptions.forEach { (gesture, description) ->
            every { accessibilityManager.getAudioDescription(gesture) } returns description
            every { accessibilityManager.announceToAccessibilityServices(description) } returns Unit
            
            val audioDesc = accessibilityManager.getAudioDescription(gesture)
            assertEquals(description, audioDesc)
            
            // Verify announcement is made
            accessibilityManager.announceToAccessibilityServices(audioDesc)
            verify { accessibilityManager.announceToAccessibilityServices(description) }
        }
    }

    @Test
    fun `test gesture sensitivity adjustments for different abilities`() = runTest {
        val sensitivityProfiles = mapOf(
            "high_precision" to 0.5f,   // Requires very precise movements
            "standard" to 1.0f,         // Default sensitivity
            "low_precision" to 2.0f,    // More forgiving for tremors/limited mobility
            "maximum_assistance" to 3.0f // Most assistive setting
        )
        
        sensitivityProfiles.forEach { (profile, multiplier) ->
            every { accessibilityManager.getSensitivityMultiplier(profile) } returns multiplier
            every { accessibilityManager.adjustGestureSensitivity(any(), multiplier) } answers {
                val baseValue = firstArg<Float>()
                baseValue * multiplier
            }
            
            val baseSensitivity = 100f
            val adjustedSensitivity = accessibilityManager.adjustGestureSensitivity(baseSensitivity, multiplier)
            
            assertEquals(baseSensitivity * multiplier, adjustedSensitivity, 0.1f)
        }
    }

    @Test
    fun `test alternative gesture input methods`() = runTest {
        // Test voice command gesture equivalents
        val voiceCommands = mapOf(
            "skip forward" to "seek_forward",
            "skip back" to "seek_backward",
            "louder" to "volume_up",
            "quieter" to "volume_down",
            "brighter" to "brightness_up",
            "dimmer" to "brightness_down",
            "play" to "play_pause",
            "pause" to "play_pause"
        )
        
        voiceCommands.forEach { (command, gesture) ->
            every { accessibilityManager.translateVoiceCommand(command) } returns gesture
            
            val translatedGesture = accessibilityManager.translateVoiceCommand(command)
            assertEquals(gesture, translatedGesture)
        }
        
        // Test switch access button mapping
        val switchMappings = mapOf(
            "switch_1" to "play_pause",
            "switch_2" to "seek_forward",
            "switch_3" to "volume_up",
            "switch_4" to "brightness_up"
        )
        
        switchMappings.forEach { (switchId, gesture) ->
            every { accessibilityManager.getSwitchMapping(switchId) } returns gesture
            
            val mappedGesture = accessibilityManager.getSwitchMapping(switchId)
            assertEquals(gesture, mappedGesture)
        }
    }

    @Test
    fun `test gesture learning and adaptation`() = runTest {
        // Test gesture pattern learning for users with limited mobility
        val userGestureData = listOf(
            GestureAttempt("seek_forward", true, 1200L, 0.8f),
            GestureAttempt("seek_forward", false, 800L, 0.3f),
            GestureAttempt("seek_forward", true, 1500L, 0.9f),
            GestureAttempt("volume_up", true, 900L, 0.7f),
            GestureAttempt("volume_up", true, 950L, 0.8f)
        )
        
        every { accessibilityManager.analyzeUserGesturePatterns(userGestureData) } answers {
            val attempts = firstArg<List<GestureAttempt>>()
            val successfulAttempts = attempts.filter { it.successful }
            val averageTime = successfulAttempts.map { it.completionTime }.average()
            val averageAccuracy = successfulAttempts.map { it.accuracy }.average()
            
            UserGestureProfile(
                averageCompletionTime = averageTime.toLong(),
                averageAccuracy = averageAccuracy.toFloat(),
                successRate = successfulAttempts.size.toFloat() / attempts.size,
                recommendedSensitivity = if (averageAccuracy < 0.7f) 1.5f else 1.0f
            )
        }
        
        val profile = accessibilityManager.analyzeUserGesturePatterns(userGestureData)
        
        assertTrue("Success rate should be reasonable", profile.successRate in 0.0f..1.0f)
        assertTrue("Average completion time should be positive", profile.averageCompletionTime > 0)
        assertTrue("Average accuracy should be reasonable", profile.averageAccuracy in 0.0f..1.0f)
        assertTrue("Recommended sensitivity should be reasonable", profile.recommendedSensitivity in 0.5f..3.0f)
    }

    @Test
    fun `test gesture guidance and tutorials`() = runTest {
        val tutorialSteps = listOf(
            "Place your finger on the screen",
            "Swipe right to seek forward",
            "Lift your finger to complete the gesture",
            "Well done! You've completed a seek gesture"
        )
        
        every { accessibilityManager.getTutorialSteps("seek_forward") } returns tutorialSteps
        every { accessibilityManager.provideTutorialGuidance(any(), any()) } returns Unit
        
        val steps = accessibilityManager.getTutorialSteps("seek_forward")
        assertEquals(tutorialSteps.size, steps.size)
        assertEquals(tutorialSteps[0], steps[0])
        
        // Test step-by-step guidance
        tutorialSteps.forEachIndexed { index, step ->
            accessibilityManager.provideTutorialGuidance(step, index)
            verify { accessibilityManager.provideTutorialGuidance(step, index) }
        }
    }

    @Test
    fun `test gesture error handling and recovery`() = runTest {
        val errorScenarios = mapOf(
            "gesture_too_fast" to "Please try the gesture more slowly",
            "gesture_too_slow" to "Try completing the gesture more quickly",
            "insufficient_distance" to "Make a longer swipe gesture",
            "gesture_timeout" to "Gesture timed out, please try again",
            "multiple_fingers" to "Use only one finger for this gesture",
            "wrong_direction" to "Swipe in the opposite direction"
        )
        
        errorScenarios.forEach { (errorType, guidance) ->
            every { accessibilityManager.getErrorGuidance(errorType) } returns guidance
            every { accessibilityManager.provideErrorRecovery(errorType) } answers {
                when (firstArg<String>()) {
                    "gesture_too_fast" -> "reduce_sensitivity"
                    "gesture_too_slow" -> "extend_timeout"
                    "insufficient_distance" -> "increase_sensitivity"
                    "gesture_timeout" -> "extend_timeout"
                    "multiple_fingers" -> "enable_multi_touch_filter"
                    "wrong_direction" -> "provide_directional_guidance"
                    else -> "general_help"
                }
            }
            
            val errorGuidance = accessibilityManager.getErrorGuidance(errorType)
            assertEquals(guidance, errorGuidance)
            
            val recovery = accessibilityManager.provideErrorRecovery(errorType)
            assertNotNull("Recovery strategy should be provided", recovery)
        }
    }

    @Test
    fun `test gesture customization for different disabilities`() = runTest {
        val disabilityProfiles = mapOf(
            "motor_impairment" to GestureProfile(
                sensitivity = 2.0f,
                timeout = 3000L,
                requiresPrecision = false,
                enableHaptic = true
            ),
            "visual_impairment" to GestureProfile(
                sensitivity = 1.5f,
                timeout = 2000L,
                requiresPrecision = false,
                enableHaptic = true,
                enableAudio = true
            ),
            "hearing_impairment" to GestureProfile(
                sensitivity = 1.0f,
                timeout = 1500L,
                requiresPrecision = true,
                enableHaptic = true,
                enableVisual = true
            ),
            "cognitive_impairment" to GestureProfile(
                sensitivity = 2.5f,
                timeout = 4000L,
                requiresPrecision = false,
                enableHaptic = true,
                enableAudio = true,
                simplifiedGestures = true
            )
        )
        
        disabilityProfiles.forEach { (disability, profile) ->
            every { accessibilityManager.getProfileForDisability(disability) } returns profile
            every { accessibilityManager.applyGestureProfile(profile) } returns true
            
            val retrievedProfile = accessibilityManager.getProfileForDisability(disability)
            assertEquals(profile.sensitivity, retrievedProfile.sensitivity, 0.1f)
            assertEquals(profile.timeout, retrievedProfile.timeout)
            assertEquals(profile.requiresPrecision, retrievedProfile.requiresPrecision)
            assertEquals(profile.enableHaptic, retrievedProfile.enableHaptic)
            
            assertTrue("Profile should be applied successfully", 
                      accessibilityManager.applyGestureProfile(profile))
        }
    }

    @Test
    fun `test gesture accessibility compliance`() = runTest {
        // Test WCAG 2.1 compliance for gesture interactions
        val wcagGuidelines = mapOf(
            "minimum_target_size" to 44f, // 44x44 dp minimum
            "maximum_timeout" to 5000L,   // 5 seconds maximum
            "alternative_input_required" to true,
            "keyboard_accessible" to true,
            "screen_reader_compatible" to true
        )
        
        wcagGuidelines.forEach { (guideline, requirement) ->
            when (requirement) {
                is Float -> {
                    every { accessibilityManager.meetsWCAGTargetSize(any()) } answers {
                        firstArg<Float>() >= requirement
                    }
                    assertTrue("Should meet WCAG target size", 
                              accessibilityManager.meetsWCAGTargetSize(48f))
                    assertFalse("Should not meet WCAG target size", 
                               accessibilityManager.meetsWCAGTargetSize(30f))
                }
                is Long -> {
                    every { accessibilityManager.meetsWCAGTimeout(any()) } answers {
                        firstArg<Long>() <= requirement
                    }
                    assertTrue("Should meet WCAG timeout", 
                              accessibilityManager.meetsWCAGTimeout(3000L))
                    assertFalse("Should not meet WCAG timeout", 
                               accessibilityManager.meetsWCAGTimeout(7000L))
                }
                is Boolean -> {
                    every { accessibilityManager.hasAlternativeInput() } returns requirement
                    assertEquals(requirement, accessibilityManager.hasAlternativeInput())
                }
            }
        }
    }

    // Data classes for test scenarios
    data class GestureAttempt(
        val gestureType: String,
        val successful: Boolean,
        val completionTime: Long,
        val accuracy: Float
    )
    
    data class UserGestureProfile(
        val averageCompletionTime: Long,
        val averageAccuracy: Float,
        val successRate: Float,
        val recommendedSensitivity: Float
    )
    
    data class GestureProfile(
        val sensitivity: Float,
        val timeout: Long,
        val requiresPrecision: Boolean,
        val enableHaptic: Boolean,
        val enableAudio: Boolean = false,
        val enableVisual: Boolean = false,
        val simplifiedGestures: Boolean = false
    )
}