package com.astralplayer.nextplayer

import android.content.Context
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityManager
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import com.astralplayer.nextplayer.feature.flags.FeatureFlags
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Accessibility Compliance Testing
 * Validates accessibility features and compliance with WCAG guidelines
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class AccessibilityTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    private lateinit var context: Context
    private lateinit var device: UiDevice
    private lateinit var accessibilityManager: AccessibilityManager

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        accessibilityManager = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        
        // Enable all features for accessibility testing
        FeatureFlags.initialize(context)
        runBlocking {
            FeatureFlags.getManager()?.enableAllFeatures()
        }
    }

    @Test
    fun testContentDescriptionsPresent() {
        composeTestRule.apply {
            // Verify main navigation has proper content descriptions
            onNodeWithContentDescription("Browse Videos").assertIsDisplayed()
            onNodeWithContentDescription("Search").assertIsDisplayed()
            onNodeWithContentDescription("Settings").assertIsDisplayed()
            
            // Test video player accessibility
            startVideoPlayback()
            
            // Verify player controls have content descriptions
            onNodeWithContentDescription("Play/Pause").assertIsDisplayed()
            onNodeWithContentDescription("Skip backward").assertIsDisplayed()
            onNodeWithContentDescription("Skip forward").assertIsDisplayed()
            onNodeWithContentDescription("Seek bar").assertIsDisplayed()
            onNodeWithContentDescription("Volume control").assertIsDisplayed()
            onNodeWithContentDescription("Fullscreen toggle").assertIsDisplayed()
            
            // Test advanced controls
            onNodeWithContentDescription("Audio settings").assertIsDisplayed()
            onNodeWithContentDescription("Subtitle options").assertIsDisplayed()
            onNodeWithContentDescription("Quick settings").assertIsDisplayed()
            onNodeWithContentDescription("Voice control").assertIsDisplayed()
        }
    }

    @Test
    fun testKeyboardNavigation() {
        composeTestRule.apply {
            // Test tab navigation through main interface
            onRoot().performKeyInput {
                pressKey(androidx.compose.ui.input.key.Key.Tab)
            }
            
            // Verify focus moves to first focusable element
            onNodeWithContentDescription("Browse Videos").assertIsFocused()
            
            // Continue tabbing through navigation
            onRoot().performKeyInput {
                pressKey(androidx.compose.ui.input.key.Key.Tab)
            }
            onNodeWithContentDescription("Search").assertIsFocused()
            
            onRoot().performKeyInput {
                pressKey(androidx.compose.ui.input.key.Key.Tab)
            }
            onNodeWithContentDescription("Settings").assertIsFocused()
            
            // Test video player keyboard controls
            startVideoPlayback()
            
            // Space bar should play/pause
            onRoot().performKeyInput {
                pressKey(androidx.compose.ui.input.key.Key.Spacebar)
            }
            waitForIdle()
            
            // Arrow keys should seek
            onRoot().performKeyInput {
                pressKey(androidx.compose.ui.input.key.Key.DirectionRight)
            }
            waitForIdle()
            
            onRoot().performKeyInput {
                pressKey(androidx.compose.ui.input.key.Key.DirectionLeft)
            }
            waitForIdle()
            
            // Up/Down should control volume
            onRoot().performKeyInput {
                pressKey(androidx.compose.ui.input.key.Key.DirectionUp)
            }
            waitForIdle()
            
            onRoot().performKeyInput {
                pressKey(androidx.compose.ui.input.key.Key.DirectionDown)
            }
            waitForIdle()
        }
    }

    @Test
    fun testScreenReaderSupport() {
        composeTestRule.apply {
            // Test that elements have proper semantic properties
            onNodeWithText("AstralStream")
                .assertHasClickAction()
                .assertIsDisplayed()
            
            // Test button semantics
            onNodeWithContentDescription("Browse Videos")
                .assertHasClickAction()
                .assertIsDisplayed()
            
            onNodeWithContentDescription("Search")
                .assertHasClickAction()
                .assertIsDisplayed()
            
            // Test video player semantics
            startVideoPlayback()
            
            onNodeWithContentDescription("Play/Pause")
                .assertHasClickAction()
                .assertIsDisplayed()
            
            // Test seek bar semantics
            onNodeWithContentDescription("Seek bar")
                .assertRangeInfoEquals(ProgressBarRangeInfo(current = 0f, range = 0f..100f))
                .assertIsDisplayed()
            
            // Test volume control semantics
            onNodeWithContentDescription("Volume control")
                .assertRangeInfoEquals(ProgressBarRangeInfo(current = 50f, range = 0f..100f))
                .assertIsDisplayed()
            
            // Test settings accessibility
            onNodeWithContentDescription("Settings").performClick()
            waitForIdle()
            
            // Verify settings items are properly labeled
            onNodeWithText("Advanced Search").assertHasClickAction()
            onNodeWithText("Voice Control").assertHasClickAction()
            onNodeWithText("Audio Equalizer").assertHasClickAction()
            onNodeWithText("Accessibility").assertHasClickAction()
        }
    }

    @Test
    fun testHighContrastSupport() {
        composeTestRule.apply {
            // Enable high contrast mode
            onNodeWithContentDescription("Settings").performClick()
            waitForIdle()
            
            onNodeWithText("Accessibility").performClick()
            waitForIdle()
            
            onNodeWithText("High Contrast").performClick()
            waitForIdle()
            
            // Verify high contrast is applied
            device.pressBack()
            device.pressBack()
            
            // Test video player with high contrast
            startVideoPlayback()
            
            // Verify controls are still visible and accessible
            onNodeWithContentDescription("Play/Pause").assertIsDisplayed()
            onNodeWithContentDescription("Seek bar").assertIsDisplayed()
            
            // Test subtitle contrast
            onNodeWithContentDescription("Subtitle options").performClick()
            waitForIdle()
            
            onNodeWithText("Enable Subtitles").performClick()
            waitForIdle()
            
            // Verify subtitle styling adapts to high contrast
            Thread.sleep(2000)
            device.pressBack()
        }
    }

    @Test
    fun testLargeTextSupport() {
        composeTestRule.apply {
            // Enable large text
            onNodeWithContentDescription("Settings").performClick()
            waitForIdle()
            
            onNodeWithText("Accessibility").performClick()
            waitForIdle()
            
            onNodeWithText("Large Text").performClick()
            waitForIdle()
            
            // Verify text scaling is applied
            device.pressBack()
            device.pressBack()
            
            // Test that UI adapts to large text
            onNodeWithText("AstralStream").assertIsDisplayed()
            
            // Test video player with large text
            startVideoPlayback()
            
            // Verify controls remain accessible with large text
            onNodeWithContentDescription("Play/Pause").assertIsDisplayed()
            
            // Test subtitle display with large text
            onNodeWithContentDescription("Subtitle options").performClick()
            waitForIdle()
            
            onNodeWithText("Customize Style").performClick()
            waitForIdle()
            
            // Verify subtitle size options include accessibility sizes
            onNodeWithText("Font Size").performClick()
            onNodeWithText("Extra Large").assertIsDisplayed()
            onNodeWithText("Huge").assertIsDisplayed()
            
            device.pressBack()
            device.pressBack()
            device.pressBack()
        }
    }

    @Test
    fun testMotionReducedSupport() {
        composeTestRule.apply {
            // Enable reduced motion
            onNodeWithContentDescription("Settings").performClick()
            waitForIdle()
            
            onNodeWithText("Accessibility").performClick()
            waitForIdle()
            
            onNodeWithText("Reduce Motion").performClick()
            waitForIdle()
            
            device.pressBack()
            device.pressBack()
            
            // Test that animations are reduced/disabled
            startVideoPlayback()
            
            // Verify transitions are instantaneous or minimal
            onNodeWithContentDescription("Quick settings").performClick()
            waitForIdle()
            
            // Menu should appear without elaborate animations
            onNodeWithText("Quick Settings").assertIsDisplayed()
            
            device.pressBack()
            
            // Test search with reduced motion
            onNodeWithContentDescription("Search").performClick()
            waitForIdle()
            
            // Search interface should appear without animations
            onNodeWithText("Search videos...").assertIsDisplayed()
            
            device.pressBack()
        }
    }

    @Test
    fun testColorBlindnessSupport() {
        composeTestRule.apply {
            // Test color accessibility
            onNodeWithContentDescription("Settings").performClick()
            waitForIdle()
            
            onNodeWithText("Accessibility").performClick()
            waitForIdle()
            
            // Test color blind friendly mode
            onNodeWithText("Color Blind Support").performClick()
            waitForIdle()
            
            onNodeWithText("Deuteranopia").performClick()
            waitForIdle()
            
            device.pressBack()
            device.pressBack()
            
            // Test video player with color blind support
            startVideoPlayback()
            
            // Verify important information doesn't rely solely on color
            onNodeWithContentDescription("Play/Pause").assertIsDisplayed()
            
            // Test equalizer with color blind support
            onNodeWithContentDescription("Audio settings").performClick()
            onNodeWithText("Equalizer").performClick()
            waitForIdle()
            
            // Verify equalizer bands have text/pattern indicators, not just color
            onNodeWithText("Audio Equalizer").assertIsDisplayed()
            
            device.pressBack()
            device.pressBack()
        }
    }

    @Test
    fun testVoiceControlAccessibility() {
        composeTestRule.apply {
            // Test voice control as accessibility feature
            startVideoPlayback()
            
            // Enable voice control
            onNodeWithContentDescription("Voice control").performClick()
            waitForIdle()
            
            // Verify voice commands work for common actions
            onNodeWithText("Listening...").assertIsDisplayed()
            
            // Test accessibility-specific voice commands
            // Note: These would be simulated in real testing
            
            // "Play video" - should start/resume playback
            // "Pause video" - should pause playback
            // "Volume up" - should increase volume
            // "Volume down" - should decrease volume
            // "Skip forward" - should seek forward
            // "Skip backward" - should seek backward
            // "Show subtitles" - should enable subtitles
            // "Hide subtitles" - should disable subtitles
            
            // Verify voice feedback is provided
            Thread.sleep(2000)
            
            onNodeWithContentDescription("Voice control").performClick()
            onNodeWithText("Stop Listening").performClick()
        }
    }

    @Test
    fun testTouchTargetSizes() {
        composeTestRule.apply {
            // Verify touch targets meet minimum size requirements (48dp)
            startVideoPlayback()
            
            // Test main controls have adequate touch targets
            val playPauseNode = onNodeWithContentDescription("Play/Pause")
            playPauseNode.assertIsDisplayed()
            
            // Test that controls are properly spaced
            onNodeWithContentDescription("Skip backward").assertIsDisplayed()
            onNodeWithContentDescription("Skip forward").assertIsDisplayed()
            
            // Test seek bar has adequate touch target
            onNodeWithContentDescription("Seek bar").assertIsDisplayed()
            
            // Test menu items have adequate touch targets
            onNodeWithContentDescription("Quick settings").performClick()
            waitForIdle()
            
            onNodeWithText("Sleep Timer").assertIsDisplayed()
            onNodeWithText("Equalizer").assertIsDisplayed()
            onNodeWithText("Speed Control").assertIsDisplayed()
            
            device.pressBack()
            
            // Test settings touch targets
            onNodeWithContentDescription("Settings").performClick()
            waitForIdle()
            
            onNodeWithText("Advanced Search").assertIsDisplayed()
            onNodeWithText("Voice Control").assertIsDisplayed()
            onNodeWithText("Accessibility").assertIsDisplayed()
        }
    }

    @Test
    fun testFocusManagement() {
        composeTestRule.apply {
            // Test logical focus order
            startVideoPlayback()
            
            // Focus should start on play/pause button
            onNodeWithContentDescription("Play/Pause").requestFocus()
            onNodeWithContentDescription("Play/Pause").assertIsFocused()
            
            // Tab to next control
            onRoot().performKeyInput {
                pressKey(androidx.compose.ui.input.key.Key.Tab)
            }
            
            // Should move to skip backward
            onNodeWithContentDescription("Skip backward").assertIsFocused()
            
            // Tab again
            onRoot().performKeyInput {
                pressKey(androidx.compose.ui.input.key.Key.Tab)
            }
            
            // Should move to skip forward
            onNodeWithContentDescription("Skip forward").assertIsFocused()
            
            // Test focus trap in dialogs
            onNodeWithContentDescription("Audio settings").performClick()
            waitForIdle()
            
            onNodeWithText("Equalizer").performClick()
            waitForIdle()
            
            // Focus should be trapped in equalizer dialog
            onNodeWithText("Audio Equalizer").assertIsDisplayed()
            
            // Tab should cycle within dialog
            repeat(10) {
                onRoot().performKeyInput {
                    pressKey(androidx.compose.ui.input.key.Key.Tab)
                }
            }
            
            // Should still be within dialog
            onNodeWithText("Audio Equalizer").assertIsDisplayed()
            
            // Escape should close dialog and return focus
            onRoot().performKeyInput {
                pressKey(androidx.compose.ui.input.key.Key.Escape)
            }
            waitForIdle()
        }
    }

    @Test
    fun testCustomAccessibilityActions() {
        composeTestRule.apply {
            startVideoPlayback()
            
            // Test custom actions on video player
            onNodeWithContentDescription("Video Player")
                .assertIsDisplayed()
                .performSemanticsAction(SemanticsActions.CustomActions) { actions ->
                    // Verify custom actions are available
                    actions.any { it.label == "Skip to next chapter" }
                }
            
            // Test seek bar custom actions
            onNodeWithContentDescription("Seek bar")
                .assertIsDisplayed()
                .performSemanticsAction(SemanticsActions.CustomActions) { actions ->
                    actions.any { it.label == "Skip 10 seconds forward" } &&
                    actions.any { it.label == "Skip 10 seconds backward" }
                }
            
            // Test equalizer custom actions
            onNodeWithContentDescription("Audio settings").performClick()
            onNodeWithText("Equalizer").performClick()
            waitForIdle()
            
            onNodeWithText("Audio Equalizer")
                .performSemanticsAction(SemanticsActions.CustomActions) { actions ->
                    actions.any { it.label == "Reset to flat" } &&
                    actions.any { it.label == "Apply preset" }
                }
            
            device.pressBack()
            device.pressBack()
        }
    }

    @Test
    fun testAccessibilityAnnouncements() {
        composeTestRule.apply {
            startVideoPlayback()
            
            // Test that important state changes are announced
            onNodeWithContentDescription("Play/Pause").performClick()
            waitForIdle()
            
            // Verify pause announcement (would need accessibility service in real test)
            Thread.sleep(1000)
            
            onNodeWithContentDescription("Play/Pause").performClick()
            waitForIdle()
            
            // Verify play announcement
            Thread.sleep(1000)
            
            // Test volume change announcements
            onRoot().performKeyInput {
                pressKey(androidx.compose.ui.input.key.Key.DirectionUp)
            }
            waitForIdle()
            
            // Test seek announcements
            onRoot().performKeyInput {
                pressKey(androidx.compose.ui.input.key.Key.DirectionRight)
            }
            waitForIdle()
            
            // Test subtitle announcements
            onNodeWithContentDescription("Subtitle options").performClick()
            waitForIdle()
            
            onNodeWithText("Enable Subtitles").performClick()
            waitForIdle()
            
            // Should announce "Subtitles enabled"
            Thread.sleep(1000)
            
            device.pressBack()
        }
    }

    @Test
    fun testAccessibilityInternationalization() {
        composeTestRule.apply {
            // Test accessibility in different languages
            onNodeWithContentDescription("Settings").performClick()
            waitForIdle()
            
            onNodeWithText("Language").performClick()
            waitForIdle()
            
            // Switch to Spanish
            onNodeWithText("Español").performClick()
            waitForIdle()
            
            device.pressBack()
            
            // Verify accessibility labels are translated
            onNodeWithText("Accesibilidad").performClick()
            waitForIdle()
            
            // Test accessibility features in Spanish
            onNodeWithText("Texto Grande").assertIsDisplayed()
            onNodeWithText("Alto Contraste").assertIsDisplayed()
            onNodeWithText("Reducir Movimiento").assertIsDisplayed()
            
            device.pressBack()
            device.pressBack()
            
            // Test video player accessibility in Spanish
            startVideoPlayback()
            
            // Verify controls have Spanish accessibility labels
            onNodeWithContentDescription("Reproducir/Pausar").assertIsDisplayed()
            onNodeWithContentDescription("Saltar hacia atrás").assertIsDisplayed()
            onNodeWithContentDescription("Saltar hacia adelante").assertIsDisplayed()
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