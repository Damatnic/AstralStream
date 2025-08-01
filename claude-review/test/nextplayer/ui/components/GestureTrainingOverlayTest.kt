package com.astralplayer.nextplayer.ui.components

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Rule
import org.junit.Test

class GestureTrainingOverlayTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun displaysTrainingStepCorrectly() {
        val step = TrainingStep.getTrainingSteps().first()
        var stepCompleted = false
        var skipped = false

        composeTestRule.setContent {
            GestureTrainingOverlay(
                currentStep = step,
                onStepComplete = { stepCompleted = true },
                onSkip = { skipped = true }
            )
        }

        composeTestRule.onNodeWithText("Single Tap").assertExists()
        composeTestRule.onNodeWithText("Tap anywhere to show/hide controls").assertExists()
        composeTestRule.onNodeWithText("Step 1 of 4").assertExists()
    }

    @Test
    fun handlesStepCompletion() {
        val step = TrainingStep.getTrainingSteps().first()
        var stepCompleted = false

        composeTestRule.setContent {
            GestureTrainingOverlay(
                currentStep = step,
                onStepComplete = { stepCompleted = true },
                onSkip = { }
            )
        }

        composeTestRule.onNodeWithText("Next").performClick()
        assert(stepCompleted)
    }

    @Test
    fun handlesSkip() {
        val step = TrainingStep.getTrainingSteps().first()
        var skipped = false

        composeTestRule.setContent {
            GestureTrainingOverlay(
                currentStep = step,
                onStepComplete = { },
                onSkip = { skipped = true }
            )
        }

        composeTestRule.onNodeWithText("Skip Tutorial").performClick()
        assert(skipped)
    }

    @Test
    fun showsFinishOnLastStep() {
        val lastStep = TrainingStep.getTrainingSteps().last()

        composeTestRule.setContent {
            GestureTrainingOverlay(
                currentStep = lastStep,
                onStepComplete = { },
                onSkip = { }
            )
        }

        composeTestRule.onNodeWithText("Finish").assertExists()
    }
}