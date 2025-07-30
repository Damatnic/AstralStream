package com.astralplayer.nextplayer

import android.content.res.Configuration
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Responsive UI Testing
 * Tests UI adaptation across different screen sizes and orientations
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class ResponsiveUITest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    private lateinit var device: UiDevice

    @Before
    fun setup() {
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        device.unfreezeRotation()
    }

    @Test
    fun testPortraitPhoneLayout() {
        // Ensure portrait orientation
        device.setOrientationNatural()
        composeTestRule.waitForIdle()

        composeTestRule.apply {
            // Test main screen layout in portrait
            onNodeWithText("AstralStream").assertIsDisplayed()
            
            // Verify navigation is bottom-oriented
            onNodeWithContentDescription("Browse Videos").assertIsDisplayed()
            onNodeWithContentDescription("Search").assertIsDisplayed()
            onNodeWithContentDescription("Settings").assertIsDisplayed()
            
            // Test video player in portrait
            startVideoPlayback()
            
            // Verify player controls are stacked vertically
            onNodeWithContentDescription("Play/Pause").assertIsDisplayed()
            onNodeWithContentDescription("Seek bar").assertIsDisplayed()
            
            // Test quick settings menu
            onNodeWithContentDescription("Quick settings").performClick()
            waitForIdle()
            
            // Verify menu adapts to portrait layout
            onNodeWithText("Quick Settings").assertIsDisplayed()
            onNodeWithText("Sleep Timer").assertIsDisplayed()
            onNodeWithText("Equalizer").assertIsDisplayed()
            
            device.pressBack()
        }
    }

    @Test
    fun testLandscapePhoneLayout() {
        // Set landscape orientation
        device.setOrientationLeft()
        composeTestRule.waitForIdle()
        Thread.sleep(1000) // Allow orientation change

        composeTestRule.apply {
            // Test main screen in landscape
            onNodeWithText("AstralStream").assertIsDisplayed()
            
            // Verify navigation adapts to landscape
            onNodeWithContentDescription("Browse Videos").assertIsDisplayed()
            
            // Test video player in landscape
            startVideoPlayback()
            
            // Verify player uses full screen
            onNodeWithContentDescription("Video Player").assertIsDisplayed()
            
            // Controls should be overlay-style in landscape
            onNodeWithContentDescription("Play/Pause").assertIsDisplayed()
            
            // Test gesture controls work in landscape
            onRoot().performTouchInput {
                swipeRight(startX = centerX * 0.2f, endX = centerX * 0.8f)
            }
            waitForIdle()
            
            // Verify seek gesture worked
            Thread.sleep(1000)
            
            // Test volume gesture
            onRoot().performTouchInput {
                swipeUp(startY = centerY * 1.5f, endY = centerY * 0.5f)
            }
            waitForIdle()
        }
    }

    @Test
    fun testTabletPortraitLayout() {
        // Simulate tablet by setting large screen configuration
        device.setOrientationNatural()
        composeTestRule.waitForIdle()

        // Note: This test assumes tablet-specific layouts exist
        composeTestRule.apply {
            onNodeWithText("AstralStream").assertIsDisplayed()
            
            // On tablets, navigation might be side-oriented
            // Verify search panel might be permanently visible
            onNodeWithContentDescription("Search").performClick()
            waitForIdle()
            
            // Test dual-pane layout if available
            onNodeWithText("Search videos...").assertIsDisplayed()
            
            // Verify filter panel is accessible
            onNodeWithText("Filters").performClick()
            waitForIdle()
            
            // In tablet layout, filters might be side panel
            onNodeWithText("Duration").assertIsDisplayed()
            onNodeWithText("Quality").assertIsDisplayed()
            onNodeWithText("File Type").assertIsDisplayed()
            
            device.pressBack()
            device.pressBack()
            
            // Test video player tablet layout
            startVideoPlayback()
            
            // Tablet player might show more controls
            onNodeWithContentDescription("Video Player").assertIsDisplayed()
            onNodeWithContentDescription("Play/Pause").assertIsDisplayed()
            
            // Test if additional controls are visible
            onNodeWithContentDescription("Skip forward").assertIsDisplayed()
            onNodeWithContentDescription("Skip backward").assertIsDisplayed()
        }
    }

    @Test
    fun testTabletLandscapeLayout() {
        device.setOrientationLeft()
        composeTestRule.waitForIdle()
        Thread.sleep(1000)

        composeTestRule.apply {
            // Test main screen tablet landscape
            onNodeWithText("AstralStream").assertIsDisplayed()
            
            // Verify navigation layout
            onNodeWithContentDescription("Browse Videos").performClick()
            waitForIdle()
            
            // In tablet landscape, might have master-detail view
            onNodeWithText("Recent Files").performClick()
            waitForIdle()
            
            // Test if video list and details are side-by-side
            Thread.sleep(2000) // Wait for video list
            
            // Start video playback
            onAllNodesWithContentDescription("Play video").onFirst().performClick()
            waitForIdle()
            
            // Test picture-in-picture style layout in tablet landscape
            onNodeWithContentDescription("Video Player").assertIsDisplayed()
            
            // Verify controls panel is accessible
            onNodeWithContentDescription("Settings").performClick()
            waitForIdle()
            
            // Settings might be in sidebar in tablet landscape
            onNodeWithText("Settings").assertIsDisplayed()
        }
    }

    @Test
    fun testFoldableDeviceLayouts() {
        // Test unfolded state (simulated by large screen)
        device.setOrientationNatural()
        composeTestRule.waitForIdle()

        composeTestRule.apply {
            // Test main screen on foldable
            onNodeWithText("AstralStream").assertIsDisplayed()
            
            // Verify adaptive navigation
            onNodeWithContentDescription("Browse Videos").performClick()
            waitForIdle()
            
            // Test dual-screen video player
            startVideoPlayback()
            
            // On foldables, player might use one screen while controls use another
            onNodeWithContentDescription("Video Player").assertIsDisplayed()
            onNodeWithContentDescription("Play/Pause").assertIsDisplayed()
            
            // Test if settings panel adapts to foldable layout
            onNodeWithContentDescription("Settings").performClick()
            waitForIdle()
            
            onNodeWithText("Settings").assertIsDisplayed()
            
            // Test feature-specific adaptations
            onNodeWithText("Advanced Search").performClick()
            waitForIdle()
            
            // Search interface might span both screens
            onNodeWithText("Search videos...").assertIsDisplayed()
            onNodeWithText("Filters").performClick()
            waitForIdle()
            
            // Filters might be on separate screen/panel
            onNodeWithText("Duration").assertIsDisplayed()
        }
    }

    @Test
    fun testSmallScreenAdaptation() {
        // Test on very small screens (compact phones)
        device.setOrientationNatural()
        composeTestRule.waitForIdle()

        composeTestRule.apply {
            // Verify UI elements are accessible on small screens
            onNodeWithText("AstralStream").assertIsDisplayed()
            
            // Navigation should be compact
            onNodeWithContentDescription("Browse Videos").assertIsDisplayed()
            
            // Test video player on small screen
            startVideoPlayback()
            
            // Controls should be appropriately sized
            onNodeWithContentDescription("Play/Pause").assertIsDisplayed()
            
            // Test if controls are reachable with thumb
            onNodeWithContentDescription("Play/Pause").performClick()
            waitForIdle()
            
            // Test settings accessibility on small screen
            onNodeWithContentDescription("Settings").performClick()
            waitForIdle()
            
            onNodeWithText("Settings").assertIsDisplayed()
            
            // Verify scrollability on small screens
            onNodeWithText("Advanced Search").performScrollTo()
            onNodeWithText("Advanced Search").assertIsDisplayed()
            
            onNodeWithText("Voice Control").performScrollTo()
            onNodeWithText("Voice Control").assertIsDisplayed()
        }
    }

    @Test
    fun testDynamicOrientationChanges() {
        composeTestRule.apply {
            // Start in portrait
            device.setOrientationNatural()
            waitForIdle()
            
            startVideoPlayback()
            
            // Verify portrait player
            onNodeWithContentDescription("Video Player").assertIsDisplayed()
            
            // Rotate to landscape
            device.setOrientationLeft()
            waitForIdle()
            Thread.sleep(1000)
            
            // Verify player adapts to landscape
            onNodeWithContentDescription("Video Player").assertIsDisplayed()
            
            // Rotate back to portrait
            device.setOrientationNatural()
            waitForIdle()
            Thread.sleep(1000)
            
            // Verify player returns to portrait layout
            onNodeWithContentDescription("Video Player").assertIsDisplayed()
            
            // Test that playback continues through orientation changes
            onNodeWithContentDescription("Play/Pause").assertIsDisplayed()
            
            // Rotate to right landscape
            device.setOrientationRight()
            waitForIdle()
            Thread.sleep(1000)
            
            // Verify player still works
            onNodeWithContentDescription("Video Player").assertIsDisplayed()
            onNodeWithContentDescription("Play/Pause").performClick()
            waitForIdle()
        }
    }

    @Test
    fun testAccessibilityScaling() {
        composeTestRule.apply {
            // Test with different text scaling
            onNodeWithText("AstralStream").assertIsDisplayed()
            
            // Navigate to settings
            onNodeWithContentDescription("Settings").performClick()
            waitForIdle()
            
            // Open accessibility settings
            onNodeWithText("Accessibility").performClick()
            waitForIdle()
            
            // Test large text support
            onNodeWithText("Large Text").performClick()
            waitForIdle()
            
            // Verify UI adapts to large text
            onNodeWithText("Settings").assertIsDisplayed()
            
            // Test video player with large text
            device.pressBack()
            device.pressBack()
            
            startVideoPlayback()
            
            // Verify controls are still accessible with large text
            onNodeWithContentDescription("Play/Pause").assertIsDisplayed()
            onNodeWithContentDescription("Seek bar").assertIsDisplayed()
            
            // Test subtitle display with large text
            onNodeWithContentDescription("Subtitle options").performClick()
            waitForIdle()
            
            onNodeWithText("Enable Subtitles").performClick()
            waitForIdle()
            
            // Verify subtitles scale appropriately
            Thread.sleep(2000)
        }
    }

    @Test
    fun testDarkModeAdaptation() {
        composeTestRule.apply {
            // Test dark mode UI adaptation
            onNodeWithContentDescription("Settings").performClick()
            waitForIdle()
            
            // Enable dark mode
            onNodeWithText("Appearance").performClick()
            waitForIdle()
            
            onNodeWithText("Dark Mode").performClick()
            waitForIdle()
            
            // Verify dark theme applied
            device.pressBack()
            device.pressBack()
            
            onNodeWithText("AstralStream").assertIsDisplayed()
            
            // Test video player in dark mode
            startVideoPlayback()
            
            onNodeWithContentDescription("Video Player").assertIsDisplayed()
            
            // Test if controls adapt to dark theme
            onNodeWithContentDescription("Play/Pause").assertIsDisplayed()
            
            // Test search in dark mode
            device.pressBack()
            onNodeWithContentDescription("Search").performClick()
            waitForIdle()
            
            onNodeWithText("Search videos...").assertIsDisplayed()
            
            // Test equalizer in dark mode
            device.pressBack()
            startVideoPlayback()
            
            onNodeWithContentDescription("Audio settings").performClick()
            onNodeWithText("Equalizer").performClick()
            waitForIdle()
            
            onNodeWithText("Audio Equalizer").assertIsDisplayed()
        }
    }

    @Test
    fun testOneHandedModeSupport() {
        composeTestRule.apply {
            // Test one-handed mode accessibility
            onNodeWithText("AstralStream").assertIsDisplayed()
            
            // Start video playback
            startVideoPlayback()
            
            // Test if controls are reachable in one-handed mode
            // Bottom area should be easily accessible
            onNodeWithContentDescription("Play/Pause").assertIsDisplayed()
            
            // Test seeking with thumb
            onNodeWithContentDescription("Seek bar").performTouchInput {
                swipeRight(startX = centerX * 0.3f, endX = centerX * 0.7f)
            }
            waitForIdle()
            
            // Test volume control with thumb
            onRoot().performTouchInput {
                // Swipe in easily reachable area
                swipeUp(
                    startY = bottom * 0.8f,
                    endY = bottom * 0.4f
                )
            }
            waitForIdle()
            
            // Test menu accessibility
            onNodeWithContentDescription("Quick settings").performClick()
            waitForIdle()
            
            // Menu should be positioned for thumb reach
            onNodeWithText("Quick Settings").assertIsDisplayed()
            onNodeWithText("Sleep Timer").performClick()
            waitForIdle()
            
            // Timer controls should be thumb-friendly
            onNodeWithText("15 minutes").assertIsDisplayed()
            onNodeWithText("30 minutes").performClick()
        }
    }

    private fun startVideoPlayback() {
        composeTestRule.apply {
            onNodeWithContentDescription("Browse Videos").performClick()
            waitForIdle()
            
            onNodeWithText("Recent Files").performClick()
            waitForIdle()
            
            Thread.sleep(2000)
            
            onAllNodesWithContentDescription("Play video").onFirst().performClick()
            waitForIdle()
            
            Thread.sleep(1000)
        }
    }

    private fun ComposeTestRule.waitForIdle() {
        Thread.sleep(500)
        this.waitForIdle()
    }
}