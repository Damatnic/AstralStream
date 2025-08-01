package com.astralplayer.nextplayer.ui.components

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.astralplayer.nextplayer.data.GestureType
import org.junit.Rule
import org.junit.Test

class GestureConflictIndicatorTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun displaysConflictIndicatorWhenVisible() {
        val conflictingGestures = listOf(
            GestureType.HORIZONTAL_SEEK,
            GestureType.LONG_PRESS
        )

        composeTestRule.setContent {
            GestureConflictIndicator(
                conflictingGestures = conflictingGestures,
                visible = true
            )
        }

        composeTestRule.onNodeWithText("Gesture Conflict").assertExists()
        composeTestRule.onNodeWithText("Seek").assertExists()
        composeTestRule.onNodeWithText("Speed").assertExists()
    }

    @Test
    fun hidesConflictIndicatorWhenNotVisible() {
        val conflictingGestures = listOf(
            GestureType.VERTICAL_VOLUME,
            GestureType.VERTICAL_BRIGHTNESS
        )

        composeTestRule.setContent {
            GestureConflictIndicator(
                conflictingGestures = conflictingGestures,
                visible = false
            )
        }

        composeTestRule.onNodeWithText("Gesture Conflict").assertDoesNotExist()
    }

    @Test
    fun showsAppropriateResolutionSuggestion() {
        val conflictingGestures = listOf(
            GestureType.VERTICAL_VOLUME,
            GestureType.VERTICAL_BRIGHTNESS
        )

        composeTestRule.setContent {
            GestureConflictIndicator(
                conflictingGestures = conflictingGestures,
                visible = true
            )
        }

        composeTestRule.onNodeWithText("Use left edge for brightness, right edge for volume")
            .assertExists()
    }

    @Test
    fun displaysAnimatedConflictResolution() {
        val conflictingGestures = listOf(GestureType.PINCH_ZOOM, GestureType.HORIZONTAL_SEEK)
        var resolved = false

        composeTestRule.setContent {
            AnimatedConflictResolution(
                conflictingGestures = conflictingGestures,
                visible = true,
                onResolved = { resolved = true }
            )
        }

        composeTestRule.onNodeWithText("Resolving Conflict...").assertExists()
    }

    @Test
    fun handlesEmptyConflictList() {
        composeTestRule.setContent {
            GestureConflictIndicator(
                conflictingGestures = emptyList(),
                visible = true
            )
        }

        composeTestRule.onNodeWithText("Gesture Conflict").assertExists()
    }

    @Test
    fun callsDismissCallback() {
        var dismissed = false
        val conflictingGestures = listOf(GestureType.DOUBLE_TAP)

        composeTestRule.setContent {
            GestureConflictIndicator(
                conflictingGestures = conflictingGestures,
                visible = true,
                onDismiss = { dismissed = true }
            )
        }

        // Wait for auto-dismiss (3 seconds)
        composeTestRule.mainClock.advanceTimeBy(3100)
        
        assert(dismissed) { "Should call dismiss callback after timeout" }
    }
}