package com.astralplayer.nextplayer

import android.content.Intent
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.astralplayer.nextplayer.feature.settings.SettingsActivity
import com.astralplayer.nextplayer.ui.theme.AppTheme
import com.astralplayer.nextplayer.VideoPlayerActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsThemeTest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<SettingsActivity>()

    @Test
    fun testThemeOptionsVisibleAndSelectable() {
        // Verify all theme options present
        listOf(AppTheme.SYSTEM, AppTheme.LIGHT, AppTheme.DARK, AppTheme.COSMIC, AppTheme.HIGH_CONTRAST).forEach { theme ->
            composeTestRule.onNodeWithTag("ThemeOption_${theme.name}")
                .assertExists("${theme.name} option not found")
        }

        // Select each and verify radio button state
        listOf(AppTheme.LIGHT, AppTheme.DARK, AppTheme.COSMIC, AppTheme.HIGH_CONTRAST).forEachIndexed { index, theme ->
            composeTestRule.onNodeWithTag("ThemeOption_${theme.name}").performClick()
            // check radio is selected
            composeTestRule.onNode(
                hasTestTag("ThemeOption_${theme.name}") and hasAnyChild(hasClickAction())
            ).assertIsSelected()
        }
    }

    @Test
    fun testPlayPauseButtonTagExists() {
        // Navigate to player screen
        composeTestRule.activityRule.scenario.onActivity { activity ->
            activity.startActivity(
                Intent(activity, VideoPlayerActivity::class.java)
            )
        }
        // Check play/pause button has test tag
        composeTestRule.onNodeWithTag("PlayPauseButton").assertExists()
    }
}