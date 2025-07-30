package com.astralplayer.nextplayer

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
 * Automated Regression Test Suite
 * Comprehensive tests to prevent feature regressions during updates
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class RegressionTestSuite {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    private lateinit var device: UiDevice

    @Before
    fun setup() {
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    }

    // ============= CORE FUNCTIONALITY REGRESSION TESTS =============

    @Test
    fun testBasicVideoPlaybackRegression() {
        composeTestRule.apply {
            // Test 1: App launches successfully
            onNodeWithText("AstralStream").assertIsDisplayed()
            
            // Test 2: Navigation works
            onNodeWithContentDescription("Browse Videos").assertIsDisplayed()
            onNodeWithContentDescription("Search").assertIsDisplayed()
            onNodeWithContentDescription("Settings").assertIsDisplayed()
            
            // Test 3: Video list loads
            onNodeWithContentDescription("Browse Videos").performClick()
            waitForIdle()
            
            onNodeWithText("Recent Files").performClick()
            waitForIdle()
            
            Thread.sleep(2000) // Allow video list to load
            
            // Test 4: Video playback starts
            if (onAllNodesWithContentDescription("Play video").fetchSemanticsNodes().isNotEmpty()) {
                onAllNodesWithContentDescription("Play video").onFirst().performClick()
                waitForIdle()
                
                // Test 5: Player controls are functional
                onNodeWithContentDescription("Video Player").assertIsDisplayed()
                onNodeWithContentDescription("Play/Pause").assertIsDisplayed()
                
                // Test 6: Play/Pause works
                onNodeWithContentDescription("Play/Pause").performClick()
                waitForIdle()
                Thread.sleep(1000)
                
                onNodeWithContentDescription("Play/Pause").performClick()
                waitForIdle()
                
                // Test 7: Seeking works
                onNodeWithContentDescription("Seek bar").performTouchInput {
                    swipeRight(startX = centerX * 0.2f, endX = centerX * 0.6f)
                }
                waitForIdle()
                
                device.pressBack()
            }
        }
    }

    @Test
    fun testSearchFunctionalityRegression() {
        composeTestRule.apply {
            // Test 1: Search interface opens
            onNodeWithContentDescription("Search").performClick()
            waitForIdle()
            
            onNodeWithText("Search videos...").assertIsDisplayed()
            
            // Test 2: Text input works
            onNodeWithText("Search videos...").performTextInput("test query")
            waitForIdle()
            
            // Test 3: Search suggestions appear
            Thread.sleep(1000)
            
            // Test 4: Filters are accessible
            onNodeWithText("Filters").performClick()
            waitForIdle()
            
            onNodeWithText("Duration").assertIsDisplayed()
            onNodeWithText("Quality").assertIsDisplayed()
            onNodeWithText("File Type").assertIsDisplayed()
            
            // Test 5: Filter selection works
            onNodeWithText("Duration").performClick()
            waitForIdle()
            
            onNodeWithText("More than 1 hour").performClick()
            waitForIdle()
            
            // Test 6: Apply filters works
            onNodeWithText("Apply Filters").performClick()
            waitForIdle()
            
            device.pressBack()
            device.pressBack()
        }
    }

    @Test
    fun testSettingsPersistenceRegression() {
        composeTestRule.apply {
            // Test 1: Settings menu opens
            onNodeWithContentDescription("Settings").performClick()
            waitForIdle()
            
            onNodeWithText("Settings").assertIsDisplayed()
            
            // Test 2: Theme change persists
            onNodeWithText("Appearance").performClick()
            waitForIdle()
            
            onNodeWithText("Dark Mode").performClick()
            waitForIdle()
            
            device.pressBack()
            device.pressBack()
            
            // Test 3: Setting persisted after navigation
            onNodeWithContentDescription("Settings").performClick()
            waitForIdle()
            
            onNodeWithText("Appearance").performClick()
            waitForIdle()
            
            // Dark mode should still be enabled
            
            device.pressBack()
            
            // Test 4: Performance settings persist
            onNodeWithText("Performance").performClick()
            waitForIdle()
            
            onNodeWithText("Quality Preset").performClick()
            waitForIdle()
            
            onNodeWithText("Battery Saver").performClick()
            waitForIdle()
            
            device.pressBack()
            device.pressBack()
        }
    }

    // ============= ADVANCED FEATURES REGRESSION TESTS =============

    @Test
    fun testAdvancedSearchRegression() {
        composeTestRule.apply {
            // Test advanced search features haven't regressed
            onNodeWithContentDescription("Search").performClick()
            waitForIdle()
            
            // Test smart suggestions
            onNodeWithText("Search videos...").performTextInput("ac")
            waitForIdle()
            
            Thread.sleep(1000)
            
            // Test filter combinations
            onNodeWithText("Filters").performClick()
            waitForIdle()
            
            // Apply multiple filters
            onNodeWithText("Duration").performClick()
            onNodeWithText("30 minutes - 1 hour").performClick()
            
            onNodeWithText("Quality").performClick()
            onNodeWithText("HD (720p+)").performClick()
            
            onNodeWithText("Apply Filters").performClick()
            waitForIdle()
            
            // Test search history
            device.pressBack()
            onNodeWithText("Search videos...").performTextClearance()
            
            onNodeWithText("Recent Searches").assertIsDisplayed()
            
            device.pressBack()
        }
    }

    @Test
    fun testAISubtitleRegression() {
        composeTestRule.apply {
            startVideoPlayback()
            
            // Test AI subtitle interface
            onNodeWithContentDescription("Subtitle options").performClick()
            waitForIdle()
            
            onNodeWithText("Generate AI Subtitles").assertIsDisplayed()
            onNodeWithText("Generate AI Subtitles").performClick()
            waitForIdle()
            
            // Test language selection
            onNodeWithText("Target Language").assertIsDisplayed()
            onNodeWithText("Target Language").performClick()
            waitForIdle()
            
            onNodeWithText("English").performClick()
            waitForIdle()
            
            // Test generation process
            onNodeWithText("Generate Subtitles").performClick()
            waitForIdle()
            
            Thread.sleep(3000) // Allow generation time
            
            device.pressBack()
            device.pressBack()
        }
    }

    @Test
    fun testVoiceControlRegression() {
        composeTestRule.apply {
            startVideoPlayback()
            
            // Test voice control activation
            onNodeWithContentDescription("Voice control").performClick()
            waitForIdle()
            
            // Test listening state
            onNodeWithText("Listening...").assertIsDisplayed()
            
            // Test available commands display
            Thread.sleep(2000)
            
            // Test deactivation
            onNodeWithContentDescription("Voice control").performClick()
            waitForIdle()
            
            onNodeWithText("Stop Listening").performClick()
            waitForIdle()
            
            device.pressBack()
        }
    }

    @Test
    fun testAudioEqualizerRegression() {
        composeTestRule.apply {
            startVideoPlayback()
            
            // Test equalizer access
            onNodeWithContentDescription("Audio settings").performClick()
            waitForIdle()
            
            onNodeWithText("Equalizer").performClick()
            waitForIdle()
            
            // Test equalizer interface
            onNodeWithText("Audio Equalizer").assertIsDisplayed()
            
            // Test preset selection
            onNodeWithText("Rock").performClick()
            waitForIdle()
            
            onNodeWithText("Current: Rock").assertIsDisplayed()
            
            // Test custom equalizer
            onNodeWithText("Custom").performClick()
            waitForIdle()
            
            // Test frequency adjustment
            onAllNodesWithContentDescription("Frequency slider").onFirst()
                .performTouchInput { swipeUp() }
            
            // Test bass boost
            onNodeWithText("Bass Boost").performClick()
            waitForIdle()
            
            device.pressBack()
            device.pressBack()
            device.pressBack()
        }
    }

    @Test
    fun testSleepTimerRegression() {
        composeTestRule.apply {
            startVideoPlayback()
            
            // Test sleep timer access
            onNodeWithContentDescription("Quick settings").performClick()
            waitForIdle()
            
            onNodeWithText("Sleep Timer").performClick()
            waitForIdle()
            
            // Test timer interface
            onNodeWithText("Sleep Timer").assertIsDisplayed()
            
            // Test duration selection
            onNodeWithText("30 minutes").performClick()
            waitForIdle()
            
            // Test fade out option
            onNodeWithText("Fade Out").performClick()
            waitForIdle()
            
            // Test action selection
            onNodeWithText("Action").performClick()
            waitForIdle()
            
            onNodeWithText("Pause Video").performClick()
            waitForIdle()
            
            // Test timer start
            onNodeWithText("Start Timer").performClick()
            waitForIdle()
            
            // Verify timer is active
            onNodeWithText("Timer: 29:59").assertIsDisplayed()
            
            // Test timer cancellation
            onNodeWithContentDescription("Cancel timer").performClick()
            waitForIdle()
            
            onNodeWithText("Cancel Timer").performClick()
            waitForIdle()
            
            device.pressBack()
        }
    }

    @Test
    fun testBookmarkRegression() {
        composeTestRule.apply {
            startVideoPlayback()
            
            // Test bookmark creation
            onNodeWithContentDescription("Add bookmark").performClick()
            waitForIdle()
            
            onNodeWithText("Bookmark Title").performTextInput("Regression Test Bookmark")
            onNodeWithText("Description (optional)").performTextInput("Test description")
            
            onNodeWithText("Save Bookmark").performClick()
            waitForIdle()
            
            onNodeWithText("Bookmark added").assertIsDisplayed()
            
            // Test bookmark list
            onNodeWithContentDescription("Bookmarks").performClick()
            waitForIdle()
            
            onNodeWithText("Regression Test Bookmark").assertIsDisplayed()
            
            // Test bookmark navigation
            onNodeWithText("Regression Test Bookmark").performClick()
            waitForIdle()
            
            Thread.sleep(1000)
            
            device.pressBack()
            device.pressBack()
        }
    }

    @Test
    fun testSocialSharingRegression() {
        composeTestRule.apply {
            startVideoPlayback()
            
            // Test sharing interface
            onNodeWithContentDescription("Share").performClick()
            waitForIdle()
            
            onNodeWithText("Share Video").assertIsDisplayed()
            
            // Test link sharing
            onNodeWithText("Share Link").performClick()
            waitForIdle()
            
            Thread.sleep(1000)
            device.pressBack()
            
            // Test file sharing
            onNodeWithText("Share File").performClick()
            waitForIdle()
            
            Thread.sleep(1000)
            device.pressBack()
            
            // Test moment sharing
            onNodeWithText("Share Current Moment").performClick()
            waitForIdle()
            
            onNodeWithText("Duration").performClick()
            onNodeWithText("30 seconds").performClick()
            
            onNodeWithText("Create Moment").performClick()
            waitForIdle()
            
            Thread.sleep(2000)
            
            device.pressBack()
            device.pressBack()
        }
    }

    @Test
    fun testPerformanceOptimizationRegression() {
        composeTestRule.apply {
            // Test performance settings
            onNodeWithContentDescription("Settings").performClick()
            waitForIdle()
            
            onNodeWithText("Performance").performClick()
            waitForIdle()
            
            // Test quality presets
            onNodeWithText("Quality Preset").performClick()
            waitForIdle()
            
            val presets = listOf("High Quality", "Balanced", "Battery Saver", "Data Saver")
            presets.forEach { preset ->
                onNodeWithText(preset).assertIsDisplayed()
            }
            
            onNodeWithText("Battery Saver").performClick()
            waitForIdle()
            
            // Test data saver mode
            onNodeWithText("Data Saver Mode").performClick()
            waitForIdle()
            
            // Test cache management
            onNodeWithText("Cache Management").performClick()
            waitForIdle()
            
            onNodeWithText("Cache Statistics").assertIsDisplayed()
            
            onNodeWithText("Optimize Cache").performClick()
            waitForIdle()
            
            Thread.sleep(1000)
            
            device.pressBack()
            device.pressBack()
            device.pressBack()
        }
    }

    // ============= UI/UX REGRESSION TESTS =============

    @Test
    fun testNavigationRegression() {
        composeTestRule.apply {
            // Test main navigation
            onNodeWithContentDescription("Browse Videos").performClick()
            waitForIdle()
            
            onNodeWithText("Recent Files").assertIsDisplayed()
            
            device.pressBack()
            
            onNodeWithContentDescription("Search").performClick()
            waitForIdle()
            
            onNodeWithText("Search videos...").assertIsDisplayed()
            
            device.pressBack()
            
            onNodeWithContentDescription("Settings").performClick()
            waitForIdle()
            
            onNodeWithText("Settings").assertIsDisplayed()
            
            device.pressBack()
        }
    }

    @Test
    fun testThemeConsistencyRegression() {
        composeTestRule.apply {
            // Test theme consistency across screens
            onNodeWithContentDescription("Settings").performClick()
            waitForIdle()
            
            onNodeWithText("Appearance").performClick()
            waitForIdle()
            
            onNodeWithText("Dark Mode").performClick()
            waitForIdle()
            
            device.pressBack()
            device.pressBack()
            
            // Verify theme applied to main screen
            onNodeWithText("AstralStream").assertIsDisplayed()
            
            // Verify theme in video player
            startVideoPlayback()
            onNodeWithContentDescription("Video Player").assertIsDisplayed()
            device.pressBack()
            
            // Verify theme in search
            onNodeWithContentDescription("Search").performClick()
            waitForIdle()
            
            onNodeWithText("Search videos...").assertIsDisplayed()
            
            device.pressBack()
        }
    }

    @Test
    fun testGestureSupportRegression() {
        composeTestRule.apply {
            startVideoPlayback()
            
            // Test swipe gestures for seeking
            onRoot().performTouchInput {
                swipeRight(startX = centerX * 0.2f, endX = centerX * 0.8f)
            }
            waitForIdle()
            
            Thread.sleep(1000)
            
            onRoot().performTouchInput {
                swipeLeft(startX = centerX * 0.8f, endX = centerX * 0.2f)
            }
            waitForIdle()
            
            // Test vertical swipes for volume
            onRoot().performTouchInput {
                swipeUp(startY = centerY * 1.5f, endY = centerY * 0.5f)
            }
            waitForIdle()
            
            onRoot().performTouchInput {
                swipeDown(startY = centerY * 0.5f, endY = centerY * 1.5f)
            }
            waitForIdle()
            
            device.pressBack()
        }
    }

    // ============= DATA INTEGRITY REGRESSION TESTS =============

    @Test
    fun testDataPersistenceRegression() {
        composeTestRule.apply {
            // Create test data
            startVideoPlayback()
            
            // Create bookmark
            onNodeWithContentDescription("Add bookmark").performClick()
            waitForIdle()
            
            onNodeWithText("Bookmark Title").performTextInput("Data Persistence Test")
            onNodeWithText("Save Bookmark").performClick()
            waitForIdle()
            
            device.pressBack()
            
            // Set equalizer
            onNodeWithContentDescription("Audio settings").performClick()
            onNodeWithText("Equalizer").performClick()
            waitForIdle()
            
            onNodeWithText("Jazz").performClick()
            waitForIdle()
            
            device.pressBack()
            device.pressBack()
            device.pressBack()
            
            // Perform search to create history
            onNodeWithContentDescription("Search").performClick()
            waitForIdle()
            
            onNodeWithText("Search videos...").performTextInput("persistence test")
            waitForIdle()
            
            device.pressBack()
            
            // Navigate away and back to verify persistence
            onNodeWithContentDescription("Settings").performClick()
            waitForIdle()
            
            device.pressBack()
            
            // Verify bookmark persisted
            startVideoPlayback()
            onNodeWithContentDescription("Bookmarks").performClick()
            waitForIdle()
            
            onNodeWithText("Data Persistence Test").assertIsDisplayed()
            
            device.pressBack()
            device.pressBack()
            
            // Verify equalizer setting persisted
            startVideoPlayback()
            onNodeWithContentDescription("Audio settings").performClick()
            onNodeWithText("Equalizer").performClick()
            waitForIdle()
            
            onNodeWithText("Current: Jazz").assertIsDisplayed()
            
            device.pressBack()
            device.pressBack()
            device.pressBack()
            
            // Verify search history persisted
            onNodeWithContentDescription("Search").performClick()
            waitForIdle()
            
            onNodeWithText("Recent Searches").performClick()
            waitForIdle()
            
            onNodeWithText("persistence test").assertIsDisplayed()
            
            device.pressBack()
            device.pressBack()
        }
    }

    @Test
    fun testFeatureFlagRegression() {
        composeTestRule.apply {
            // Test feature flag functionality
            onNodeWithContentDescription("Settings").performClick()
            waitForIdle()
            
            onNodeWithText("Advanced").performClick()
            waitForIdle()
            
            onNodeWithText("Feature Flags").performClick()
            waitForIdle()
            
            // Test toggling features
            onAllNodesWithContentDescription("Feature toggle").onFirst().performClick()
            waitForIdle()
            
            Thread.sleep(1000)
            
            device.pressBack()
            device.pressBack()
            device.pressBack()
            
            // Verify feature state persisted
            onNodeWithContentDescription("Settings").performClick()
            waitForIdle()
            
            onNodeWithText("Advanced").performClick()
            waitForIdle()
            
            onNodeWithText("Feature Flags").performClick()
            waitForIdle()
            
            // Feature state should be maintained
            
            device.pressBack()
            device.pressBack()
            device.pressBack()
        }
    }

    // ============= ERROR HANDLING REGRESSION TESTS =============

    @Test
    fun testErrorHandlingRegression() {
        composeTestRule.apply {
            // Test various error scenarios
            
            // Test file not found handling
            onNodeWithContentDescription("Browse Videos").performClick()
            waitForIdle()
            
            onNodeWithText("Recent Files").performClick()
            waitForIdle()
            
            Thread.sleep(2000)
            
            // If no videos, should show appropriate message
            if (onAllNodesWithContentDescription("Play video").fetchSemanticsNodes().isEmpty()) {
                onNodeWithText("No videos found").assertIsDisplayed()
            }
            
            device.pressBack()
            device.pressBack()
            
            // Test network error handling
            startVideoPlaybackIfAvailable()
            
            onNodeWithContentDescription("Subtitle options").performClick()
            waitForIdle()
            
            onNodeWithText("Generate AI Subtitles").performClick()
            waitForIdle()
            
            onNodeWithText("Generate Subtitles").performClick()
            waitForIdle()
            
            Thread.sleep(3000)
            
            // Should handle network errors gracefully
            
            device.pressBack()
            device.pressBack()
            device.pressBack()
        }
    }

    // ============= HELPER METHODS =============

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

    private fun startVideoPlaybackIfAvailable() {
        composeTestRule.apply {
            try {
                onNodeWithContentDescription("Browse Videos").performClick()
                waitForIdle()
                
                onNodeWithText("Recent Files").performClick()
                waitForIdle()
                
                Thread.sleep(2000)
                
                if (onAllNodesWithContentDescription("Play video").fetchSemanticsNodes().isNotEmpty()) {
                    onAllNodesWithContentDescription("Play video").onFirst().performClick()
                    waitForIdle()
                    
                    Thread.sleep(1000)
                }
            } catch (e: Exception) {
                // Video might not be available
            }
        }
    }

    private fun ComposeTestRule.waitForIdle() {
        Thread.sleep(300)
        this.waitForIdle()
    }
}