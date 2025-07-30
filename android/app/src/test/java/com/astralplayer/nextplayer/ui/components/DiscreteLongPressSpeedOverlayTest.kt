package com.astralplayer.nextplayer.ui.components

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.astralplayer.nextplayer.data.SeekDirection
import com.astralplayer.nextplayer.data.gesture.AdvancedAnimationEngine
import io.mockk.*
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class DiscreteLongPressSpeedOverlayTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var mockAnimationEngine: AdvancedAnimationEngine

    @Before
    fun setup() {
        mockAnimationEngine = mockk(relaxed = true)
        every { mockAnimationEngine.frameRate } returns MutableStateFlow(60f)
        every { mockAnimationEngine.isHardwareAccelerated() } returns true
    }

    @Test
    fun displaysSpeedOverlayWhenVisible() {
        composeTestRule.setContent {
            DiscreteLongPressSpeedOverlay(
                isVisible = true,
                currentSpeed = 2.0f,
                targetSpeed = 2.0f,
                direction = SeekDirection.FORWARD,
                speedProgression = listOf(0.5f, 1.0f, 2.0f, 4.0f),
                currentSpeedIndex = 2,
                animationEngine = mockAnimationEngine
            )
        }

        // Verify overlay is displayed
        composeTestRule.onNodeWithText("2×").assertExists()
    }

    @Test
    fun hidesOverlayWhenNotVisible() {
        composeTestRule.setContent {
            DiscreteLongPressSpeedOverlay(
                isVisible = false,
                currentSpeed = 2.0f,
                targetSpeed = 2.0f,
                direction = SeekDirection.FORWARD,
                speedProgression = listOf(0.5f, 1.0f, 2.0f, 4.0f),
                currentSpeedIndex = 2,
                animationEngine = mockAnimationEngine
            )
        }

        // Overlay should not be visible
        composeTestRule.onNodeWithText("2×").assertDoesNotExist()
    }

    @Test
    fun displaysCorrectSpeedText() {
        composeTestRule.setContent {
            DiscreteLongPressSpeedOverlay(
                isVisible = true,
                currentSpeed = 0.5f,
                targetSpeed = 0.5f,
                direction = SeekDirection.BACKWARD,
                speedProgression = listOf(0.5f, 1.0f, 2.0f),
                currentSpeedIndex = 0,
                animationEngine = mockAnimationEngine
            )
        }

        // Should display percentage for speeds < 1
        composeTestRule.onNodeWithText("50%").assertExists()
    }

    @Test
    fun showsTargetSpeedWhenDifferent() {
        composeTestRule.setContent {
            DiscreteLongPressSpeedOverlay(
                isVisible = true,
                currentSpeed = 2.0f,
                targetSpeed = 4.0f,
                direction = SeekDirection.FORWARD,
                speedProgression = listOf(1.0f, 2.0f, 4.0f),
                currentSpeedIndex = 1,
                animationEngine = mockAnimationEngine
            )
        }

        composeTestRule.onNodeWithText("2×").assertExists()
        composeTestRule.onNodeWithText("→ 4.0×").assertExists()
    }

    @Test
    fun usesOptimizedVersionWithHardwareAcceleration() {
        every { mockAnimationEngine.isHardwareAccelerated() } returns true

        composeTestRule.setContent {
            OptimizedDiscreteLongPressSpeedOverlay(
                isVisible = true,
                currentSpeed = 2.0f,
                targetSpeed = 2.0f,
                direction = SeekDirection.FORWARD,
                speedProgression = listOf(1.0f, 2.0f, 4.0f),
                currentSpeedIndex = 1,
                animationEngine = mockAnimationEngine
            )
        }

        // Should use hardware acceleration
        verify { mockAnimationEngine.isHardwareAccelerated() }
    }

    @Test
    fun fallsBackToRegularVersionWithoutHardwareAcceleration() {
        every { mockAnimationEngine.isHardwareAccelerated() } returns false

        composeTestRule.setContent {
            OptimizedDiscreteLongPressSpeedOverlay(
                isVisible = true,
                currentSpeed = 2.0f,
                targetSpeed = 2.0f,
                direction = SeekDirection.FORWARD,
                speedProgression = listOf(1.0f, 2.0f, 4.0f),
                currentSpeedIndex = 1,
                animationEngine = mockAnimationEngine
            )
        }

        // Should still display content
        composeTestRule.onNodeWithText("2×").assertExists()
    }

    @Test
    fun handlesHighSpeedValues() {
        composeTestRule.setContent {
            DiscreteLongPressSpeedOverlay(
                isVisible = true,
                currentSpeed = 16.0f,
                targetSpeed = 16.0f,
                direction = SeekDirection.FORWARD,
                speedProgression = listOf(1.0f, 2.0f, 4.0f, 8.0f, 16.0f),
                currentSpeedIndex = 4,
                animationEngine = mockAnimationEngine
            )
        }

        composeTestRule.onNodeWithText("16×").assertExists()
    }

    @Test
    fun handlesEmptySpeedProgression() {
        composeTestRule.setContent {
            DiscreteLongPressSpeedOverlay(
                isVisible = true,
                currentSpeed = 1.0f,
                targetSpeed = 1.0f,
                direction = SeekDirection.NONE,
                speedProgression = emptyList(),
                currentSpeedIndex = 0,
                animationEngine = mockAnimationEngine
            )
        }

        // Should still display speed text
        composeTestRule.onNodeWithText("1×").assertExists()
    }
}