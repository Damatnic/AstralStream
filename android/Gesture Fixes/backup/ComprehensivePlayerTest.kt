package com.astralplayer.nextplayer

import android.content.Intent
import android.net.Uri
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.astralplayer.nextplayer.feature.settings.SettingsActivity
import com.astralplayer.nextplayer.ui.theme.AppTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Before
import org.junit.After

/**
 * Comprehensive automated test suite for Astral Player
 * Tests all major features and UI interactions
 */
@RunWith(AndroidJUnit4::class)
class ComprehensivePlayerTest {
    
    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()
    
    private val testVideoUri = "android.resource://com.astralplayer.nextplayer/raw/test_video"
    
    @Before
    fun setup() {
        // Clear any previous state
        InstrumentationRegistry.getInstrumentation().targetContext
            .getSharedPreferences("settings", 0).edit().clear().commit()
    }
    
    @After
    fun tearDown() {
        // Clean up after tests
    }
    
    /**
     * Test 1: Main screen navigation and quick actions
     */
    @Test
    fun testMainScreenNavigation() {
        // Verify main screen loads
        composeTestRule.onNodeWithText("Astral Player").assertExists()
        
        // Test quick action cards
        composeTestRule.onNodeWithText("Open File").assertExists().assertHasClickAction()
        composeTestRule.onNodeWithText("Stream URL").assertExists().assertHasClickAction()
        composeTestRule.onNodeWithText("Folders").assertExists().assertHasClickAction()
        composeTestRule.onNodeWithText("Recent").assertExists().assertHasClickAction()
        composeTestRule.onNodeWithText("Playlists").assertExists().assertHasClickAction()
        composeTestRule.onNodeWithText("Live Streams").assertExists().assertHasClickAction()
        
        // Test navigation drawer
        composeTestRule.onNodeWithContentDescription("Menu").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("About").assertExists()
        composeTestRule.onNodeWithText("Settings").assertExists()
    }
    
    /**
     * Test 2: Video player controls
     */
    @Test
    fun testVideoPlayerControls() {
        // Open video player
        openVideoPlayer()
        
        // Wait for player to load
        composeTestRule.waitForIdle()
        Thread.sleep(1000)
        
        // Test play/pause button
        composeTestRule.onNode(
            hasContentDescription("Play") or hasContentDescription("Pause")
        ).assertExists()
        
        // Test seek controls
        composeTestRule.onNodeWithContentDescription("Seek backward").assertExists()
        composeTestRule.onNodeWithContentDescription("Seek forward").assertExists()
        
        // Test control visibility toggle
        composeTestRule.onRoot().performClick()
        composeTestRule.waitForIdle()
        
        // Test bottom controls
        composeTestRule.onNodeWithText("CC").assertExists()
        composeTestRule.onNodeWithText("Quality").assertExists()
        composeTestRule.onNodeWithText("Audio").assertExists()
        composeTestRule.onNodeWithText("More").assertExists()
    }
    
    /**
     * Test 3: Gesture controls
     */
    @Test
    fun testGestureControls() {
        openVideoPlayer()
        composeTestRule.waitForIdle()
        
        // Test double tap to seek
        val screenBounds = composeTestRule.onRoot().fetchSemanticsNode().boundsInRoot
        val screenWidth = screenBounds.width
        val leftSide = screenWidth / 4
        val rightSide = screenWidth * 3 / 4
        
        // Double tap left side (seek backward)
        composeTestRule.onRoot().performTouchInput {
            doubleClick(position = androidx.compose.ui.geometry.Offset(leftSide, center.y))
        }
        
        // Double tap right side (seek forward)
        composeTestRule.onRoot().performTouchInput {
            doubleClick(position = androidx.compose.ui.geometry.Offset(rightSide, center.y))
        }
        
        // Test long press seek
        composeTestRule.onRoot().performTouchInput {
            longClick(position = center, durationMillis = 2000)
        }
    }
    
    /**
     * Test 4: Theme switching
     */
    @Test
    fun testThemeSwitching() {
        // Navigate to settings
        composeTestRule.onNodeWithContentDescription("Settings").performClick()
        composeTestRule.waitForIdle()
        
        // Scroll to theme section
        composeTestRule.onNodeWithText("Theme").performScrollTo()
        
        // Test each theme option
        listOf("Light", "Dark", "Cosmic", "High Contrast").forEach { themeName ->
            composeTestRule.onNodeWithText(themeName).performClick()
            composeTestRule.waitForIdle()
            
            // Verify theme is selected
            composeTestRule.onNode(
                hasText(themeName) and hasAnyAncestor(hasTestTag("ThemeOption_"))
            ).assertExists()
        }
    }
    
    /**
     * Test 5: Quick settings menu
     */
    @Test
    fun testQuickSettingsMenu() {
        openVideoPlayer()
        composeTestRule.waitForIdle()
        
        // Open quick settings
        composeTestRule.onNodeWithContentDescription("Quick Settings").performClick()
        composeTestRule.waitForIdle()
        
        // Verify quick settings dialog
        composeTestRule.onNodeWithText("Quick Settings").assertExists()
        
        // Test tab navigation
        listOf("Speed", "Gestures", "Visual", "Performance").forEach { tab ->
            composeTestRule.onNodeWithText(tab).performClick()
            composeTestRule.waitForIdle()
        }
        
        // Close dialog
        composeTestRule.onNodeWithContentDescription("Close").performClick()
    }
    
    /**
     * Test 6: AI features panel
     */
    @Test
    fun testAIFeaturesPanel() {
        openVideoPlayer()
        composeTestRule.waitForIdle()
        
        // Open AI features
        composeTestRule.onNodeWithContentDescription("AI Features").performClick()
        composeTestRule.waitForIdle()
        
        // Verify AI features exist
        composeTestRule.onNodeWithText("AI Features").assertExists()
        composeTestRule.onNodeWithText("AI Upscaling").assertExists()
        composeTestRule.onNodeWithText("AI Subtitles").assertExists()
        composeTestRule.onNodeWithText("Live Translation").assertExists()
        composeTestRule.onNodeWithText("Video Summary").assertExists()
    }
    
    /**
     * Test 7: More options dialog
     */
    @Test
    fun testMoreOptionsDialog() {
        openVideoPlayer()
        composeTestRule.waitForIdle()
        
        // Open more options
        composeTestRule.onNodeWithText("More").performClick()
        composeTestRule.waitForIdle()
        
        // Verify options
        listOf(
            "Loop Video",
            "Picture in Picture",
            "Sleep Timer",
            "Equalizer",
            "Video Information",
            "Share",
            "Player Settings"
        ).forEach { option ->
            composeTestRule.onNodeWithText(option).assertExists()
        }
    }
    
    /**
     * Test 8: Screen lock functionality
     */
    @Test
    fun testScreenLock() {
        openVideoPlayer()
        composeTestRule.waitForIdle()
        
        // Lock screen
        composeTestRule.onNodeWithContentDescription("Lock Screen").performClick()
        composeTestRule.waitForIdle()
        
        // Verify lock indicator
        composeTestRule.onNodeWithText("Screen Locked").assertExists()
        
        // Unlock
        composeTestRule.onNodeWithText("Screen Locked").performClick()
        composeTestRule.waitForIdle()
        
        // Verify controls are visible again
        composeTestRule.onNodeWithContentDescription("Lock Screen").assertExists()
    }
    
    /**
     * Test 9: Playback speed control
     */
    @Test
    fun testPlaybackSpeed() {
        openVideoPlayer()
        composeTestRule.waitForIdle()
        
        // Find speed button (shows current speed)
        val speedButton = composeTestRule.onNode(
            hasText("1.0x") or hasText("1x") or hasText("2x")
        )
        
        // Click to cycle through speeds
        repeat(3) {
            speedButton.performClick()
            composeTestRule.waitForIdle()
            Thread.sleep(500)
        }
    }
    
    /**
     * Test 10: Stream URL dialog
     */
    @Test
    fun testStreamUrlDialog() {
        // Click Stream URL card
        composeTestRule.onNodeWithText("Stream URL").performClick()
        composeTestRule.waitForIdle()
        
        // Verify dialog
        composeTestRule.onNodeWithText("Stream from URL").assertExists()
        composeTestRule.onNodeWithText("Video URL").assertExists()
        
        // Test presets
        composeTestRule.onNodeWithText("Test HLS").assertExists()
        composeTestRule.onNodeWithText("Big Buck Bunny").assertExists()
        
        // Cancel dialog
        composeTestRule.onNodeWithText("Cancel").performClick()
    }
    
    /**
     * Test 11: Settings persistence
     */
    @Test
    fun testSettingsPersistence() {
        // Navigate to settings
        composeTestRule.onNodeWithContentDescription("Settings").performClick()
        composeTestRule.waitForIdle()
        
        // Change theme to Cosmic
        composeTestRule.onNodeWithText("Theme").performScrollTo()
        composeTestRule.onNodeWithText("Cosmic").performClick()
        
        // Go back
        composeTestRule.onNodeWithContentDescription("Back").performClick()
        composeTestRule.waitForIdle()
        
        // Re-open settings
        composeTestRule.onNodeWithContentDescription("Settings").performClick()
        composeTestRule.waitForIdle()
        
        // Verify Cosmic is still selected
        composeTestRule.onNodeWithText("Theme").performScrollTo()
        composeTestRule.onNode(
            hasTestTag("ThemeOption_COSMIC") and hasAnyDescendant(isSelected())
        ).assertExists()
    }
    
    /**
     * Test 12: Error handling
     */
    @Test
    fun testErrorHandling() {
        // Open player with invalid URI
        composeTestRule.activityRule.scenario.onActivity { activity ->
            val intent = Intent(activity, VideoPlayerActivity::class.java).apply {
                data = Uri.parse("invalid://uri")
            }
            activity.startActivity(intent)
        }
        
        composeTestRule.waitForIdle()
        Thread.sleep(2000)
        
        // Should show error overlay
        composeTestRule.onNodeWithText("Playback Error").assertExists()
        composeTestRule.onNodeWithText("Retry").assertExists()
    }
    
    // Helper functions
    
    private fun openVideoPlayer() {
        composeTestRule.activityRule.scenario.onActivity { activity ->
            val intent = Intent(activity, VideoPlayerActivity::class.java).apply {
                data = Uri.parse(testVideoUri)
                putExtra("video_title", "Test Video")
            }
            activity.startActivity(intent)
        }
    }
    
    private fun SemanticsNodeInteraction.getBoundsInRoot(): androidx.compose.ui.unit.DpRect {
        return fetchSemanticsNode().boundsInRoot.let {
            androidx.compose.ui.unit.DpRect(
                androidx.compose.ui.unit.Dp(it.left),
                androidx.compose.ui.unit.Dp(it.top),
                androidx.compose.ui.unit.Dp(it.right),
                androidx.compose.ui.unit.Dp(it.bottom)
            )
        }
    }
}

/**
 * Performance test suite
 */
@RunWith(AndroidJUnit4::class)
class PerformanceTest {
    
    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()
    
    @Test
    fun testAppLaunchTime() {
        val startTime = System.currentTimeMillis()
        
        // Wait for main screen
        composeTestRule.onNodeWithText("Astral Player").assertExists()
        
        val launchTime = System.currentTimeMillis() - startTime
        assert(launchTime < 3000) { "App launch took too long: ${launchTime}ms" }
    }
    
    @Test
    fun testVideoLoadTime() {
        val startTime = System.currentTimeMillis()
        
        // Open video
        composeTestRule.activityRule.scenario.onActivity { activity ->
            val intent = Intent(activity, VideoPlayerActivity::class.java).apply {
                data = Uri.parse("android.resource://com.astralplayer.nextplayer/raw/test_video")
            }
            activity.startActivity(intent)
        }
        
        // Wait for player controls
        composeTestRule.waitUntil(5000) {
            try {
                composeTestRule.onNode(
                    hasContentDescription("Play") or hasContentDescription("Pause")
                ).assertExists()
                true
            } catch (e: AssertionError) {
                false
            }
        }
        
        val loadTime = System.currentTimeMillis() - startTime
        assert(loadTime < 5000) { "Video load took too long: ${loadTime}ms" }
    }
}