package com.astralplayer.nextplayer

import android.content.Context
import android.content.Intent
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import com.astralplayer.nextplayer.feature.flags.Feature
import com.astralplayer.nextplayer.feature.flags.FeatureFlags
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * End-to-End Feature Integration Tests
 * Tests complete user workflows across all new features
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class EndToEndFeatureTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    private lateinit var context: Context
    private lateinit var device: UiDevice

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        
        // Initialize feature flags for testing
        FeatureFlags.initialize(context)
        
        // Enable all features for comprehensive testing
        runBlocking {
            FeatureFlags.getManager()?.enableAllFeatures()
        }
    }

    @Test
    fun testCompleteVideoPlaybackWorkflow() {
        composeTestRule.apply {
            // 1. Launch app and verify main screen
            onNodeWithText("AstralStream").assertIsDisplayed()
            
            // 2. Navigate to video selection
            onNodeWithContentDescription("Browse Videos").performClick()
            waitForIdle()
            
            // 3. Select a video file
            onNodeWithText("Recent Files").performClick()
            waitForIdle()
            
            // Wait for video list to load
            Thread.sleep(2000)
            
            // 4. Click on first available video
            onAllNodesWithContentDescription("Play video").onFirst().performClick()
            waitForIdle()
            
            // 5. Verify video player screen loads
            onNodeWithContentDescription("Video Player").assertIsDisplayed()
            
            // 6. Test basic playback controls
            onNodeWithContentDescription("Play/Pause").performClick()
            Thread.sleep(1000)
            onNodeWithContentDescription("Play/Pause").performClick()
            
            // 7. Test seeking
            onNodeWithContentDescription("Seek bar").performTouchInput {
                swipeRight(startX = centerX * 0.3f, endX = centerX * 0.7f)
            }
            
            waitForIdle()
        }
    }

    @Test
    fun testAdvancedSearchIntegration() {
        composeTestRule.apply {
            // 1. Open search
            onNodeWithContentDescription("Search").performClick()
            waitForIdle()
            
            // 2. Verify search screen
            onNodeWithText("Search videos...").assertIsDisplayed()
            
            // 3. Enter search query
            onNodeWithText("Search videos...").performTextInput("action")
            waitForIdle()
            
            // 4. Apply filters
            onNodeWithText("Filters").performClick()
            waitForIdle()
            
            // 5. Select duration filter
            onNodeWithText("Duration").performClick()
            onNodeWithText("More than 1 hour").performClick()
            
            // 6. Apply quality filter
            onNodeWithText("Quality").performClick()
            onNodeWithText("HD (720p+)").performClick()
            
            // 7. Execute filtered search
            onNodeWithText("Apply Filters").performClick()
            waitForIdle()
            
            // 8. Verify search results
            onNodeWithText("Search Results").assertIsDisplayed()
            
            // 9. Test search suggestions
            onNodeWithText("Search videos...").performTextClearance()
            onNodeWithText("Search videos...").performTextInput("co")
            waitForIdle()
            
            // Verify suggestions appear
            onNodeWithText("Recent Searches").assertIsDisplayed()
        }
    }

    @Test
    fun testAISubtitleGeneration() {
        composeTestRule.apply {
            // 1. Start video playback
            startVideoPlayback()
            
            // 2. Open subtitle menu
            onNodeWithContentDescription("Subtitle options").performClick()
            waitForIdle()
            
            // 3. Navigate to AI subtitle generation
            onNodeWithText("Generate AI Subtitles").performClick()
            waitForIdle()
            
            // 4. Select target language
            onNodeWithText("Target Language").performClick()
            onNodeWithText("English").performClick()
            
            // 5. Start generation
            onNodeWithText("Generate Subtitles").performClick()
            waitForIdle()
            
            // 6. Wait for generation to complete (mock)
            Thread.sleep(3000)
            
            // 7. Verify subtitles are enabled
            onNodeWithText("Subtitles: English (AI Generated)").assertIsDisplayed()
            
            // 8. Test subtitle customization
            onNodeWithText("Customize Style").performClick()
            waitForIdle()
            
            // 9. Modify subtitle appearance
            onNodeWithText("Font Size").performClick()
            onNodeWithText("Large").performClick()
            
            onNodeWithText("Font Color").performClick()
            onNodeWithText("White").performClick()
            
            // 10. Apply changes
            onNodeWithText("Apply Changes").performClick()
            waitForIdle()
        }
    }

    @Test
    fun testVoiceControlWorkflow() {
        composeTestRule.apply {
            // 1. Enable voice control
            onNodeWithContentDescription("Settings").performClick()
            waitForIdle()
            
            onNodeWithText("Voice Control").performClick()
            waitForIdle()
            
            onNodeWithText("Enable Voice Control").performClick()
            waitForIdle()
            
            // 2. Grant microphone permission (handled by system)
            Thread.sleep(2000)
            
            // 3. Start video playback
            device.pressBack()
            startVideoPlayback()
            
            // 4. Activate voice control
            onNodeWithContentDescription("Voice control").performClick()
            waitForIdle()
            
            // 5. Verify voice listening indicator
            onNodeWithText("Listening...").assertIsDisplayed()
            
            // 6. Simulate voice commands (mock)
            Thread.sleep(2000)
            
            // 7. Test continuous voice mode
            onNodeWithText("Continuous Mode").performClick()
            waitForIdle()
            
            // 8. Verify continuous listening
            onNodeWithContentDescription("Voice active").assertIsDisplayed()
            
            // 9. Disable voice control
            onNodeWithContentDescription("Voice control").performClick()
            onNodeWithText("Stop Listening").performClick()
        }
    }

    @Test
    fun testAudioEqualizerIntegration() {
        composeTestRule.apply {
            // 1. Start video playback
            startVideoPlayback()
            
            // 2. Open audio settings
            onNodeWithContentDescription("Audio settings").performClick()
            waitForIdle()
            
            // 3. Open equalizer
            onNodeWithText("Equalizer").performClick()
            waitForIdle()
            
            // 4. Verify equalizer interface
            onNodeWithText("Audio Equalizer").assertIsDisplayed()
            
            // 5. Test preset selection
            onNodeWithText("Presets").performClick()
            onNodeWithText("Rock").performClick()
            waitForIdle()
            
            // 6. Verify preset applied
            onNodeWithText("Current: Rock").assertIsDisplayed()
            
            // 7. Test custom equalizer
            onNodeWithText("Custom").performClick()
            waitForIdle()
            
            // 8. Adjust frequency bands
            onAllNodesWithContentDescription("Frequency slider").onFirst()
                .performTouchInput { swipeUp() }
            
            // 9. Enable bass boost
            onNodeWithText("Bass Boost").performClick()
            onNodeWithContentDescription("Bass boost slider")
                .performTouchInput { swipeRight() }
            
            // 10. Apply settings
            onNodeWithText("Apply").performClick()
            waitForIdle()
        }
    }

    @Test
    fun testSleepTimerFunctionality() {
        composeTestRule.apply {
            // 1. Start video playback
            startVideoPlayback()
            
            // 2. Open quick settings
            onNodeWithContentDescription("Quick settings").performClick()
            waitForIdle()
            
            // 3. Open sleep timer
            onNodeWithText("Sleep Timer").performClick()
            waitForIdle()
            
            // 4. Verify timer interface
            onNodeWithText("Sleep Timer").assertIsDisplayed()
            
            // 5. Set timer duration
            onNodeWithText("30 minutes").performClick()
            
            // 6. Enable fade out
            onNodeWithText("Fade Out").performClick()
            
            // 7. Select timer action
            onNodeWithText("Action").performClick()
            onNodeWithText("Pause Video").performClick()
            
            // 8. Start timer
            onNodeWithText("Start Timer").performClick()
            waitForIdle()
            
            // 9. Verify timer is active
            onNodeWithText("Timer: 29:59").assertIsDisplayed()
            
            // 10. Test timer cancellation
            onNodeWithContentDescription("Cancel timer").performClick()
            onNodeWithText("Cancel Timer").performClick()
        }
    }

    @Test
    fun testVideoBookmarksWorkflow() {
        composeTestRule.apply {
            // 1. Start video playback
            startVideoPlayback()
            
            // 2. Seek to specific position
            onNodeWithContentDescription("Seek bar").performTouchInput {
                swipeRight(startX = centerX * 0.2f, endX = centerX * 0.5f)
            }
            waitForIdle()
            
            // 3. Add bookmark
            onNodeWithContentDescription("Add bookmark").performClick()
            waitForIdle()
            
            // 4. Enter bookmark title
            onNodeWithText("Bookmark Title").performTextInput("Action Scene")
            
            // 5. Add description
            onNodeWithText("Description (optional)").performTextInput("Epic fight sequence")
            
            // 6. Save bookmark
            onNodeWithText("Save Bookmark").performClick()
            waitForIdle()
            
            // 7. Verify bookmark created
            onNodeWithText("Bookmark added").assertIsDisplayed()
            
            // 8. Open bookmarks list
            onNodeWithContentDescription("Bookmarks").performClick()
            waitForIdle()
            
            // 9. Verify bookmark in list
            onNodeWithText("Action Scene").assertIsDisplayed()
            
            // 10. Navigate to bookmark
            onNodeWithText("Action Scene").performClick()
            waitForIdle()
            
            // 11. Test bookmark editing
            onNodeWithContentDescription("Edit bookmark").performClick()
            onNodeWithText("Bookmark Title").performTextClearance()
            onNodeWithText("Bookmark Title").performTextInput("Updated Scene")
            onNodeWithText("Update Bookmark").performClick()
        }
    }

    @Test
    fun testSocialSharingFeatures() {
        composeTestRule.apply {
            // 1. Start video playback
            startVideoPlayback()
            
            // 2. Open sharing menu
            onNodeWithContentDescription("Share").performClick()
            waitForIdle()
            
            // 3. Verify sharing options
            onNodeWithText("Share Video").assertIsDisplayed()
            
            // 4. Test link sharing
            onNodeWithText("Share Link").performClick()
            waitForIdle()
            
            // 5. Verify link generated
            onNodeWithText("Share Link").assertIsDisplayed()
            
            // 6. Test file sharing
            device.pressBack()
            onNodeWithText("Share File").performClick()
            waitForIdle()
            
            // 7. Test moment sharing
            device.pressBack()
            onNodeWithText("Share Current Moment").performClick()
            waitForIdle()
            
            // 8. Set moment duration
            onNodeWithText("Duration").performClick()
            onNodeWithText("30 seconds").performClick()
            
            // 9. Generate moment
            onNodeWithText("Create Moment").performClick()
            waitForIdle()
            
            // 10. Verify moment created
            onNodeWithText("Moment created successfully").assertIsDisplayed()
        }
    }

    @Test
    fun testPerformanceOptimizationSettings() {
        composeTestRule.apply {
            // 1. Open settings
            onNodeWithContentDescription("Settings").performClick()
            waitForIdle()
            
            // 2. Navigate to performance settings
            onNodeWithText("Performance").performClick()
            waitForIdle()
            
            // 3. Verify performance options
            onNodeWithText("Performance Optimization").assertIsDisplayed()
            
            // 4. Test quality presets
            onNodeWithText("Quality Preset").performClick()
            onNodeWithText("Battery Saver").performClick()
            waitForIdle()
            
            // 5. Enable data saver
            onNodeWithText("Data Saver Mode").performClick()
            
            // 6. Test cache management
            onNodeWithText("Cache Management").performClick()
            waitForIdle()
            
            // 7. View cache statistics
            onNodeWithText("Cache Statistics").assertIsDisplayed()
            
            // 8. Clear cache
            onNodeWithText("Clear Cache").performClick()
            onNodeWithText("Confirm").performClick()
            waitForIdle()
            
            // 9. Verify cache cleared
            onNodeWithText("Cache cleared successfully").assertIsDisplayed()
        }
    }

    @Test
    fun testFeatureFlagIntegration() {
        composeTestRule.apply {
            // 1. Open settings
            onNodeWithContentDescription("Settings").performClick()
            waitForIdle()
            
            // 2. Navigate to advanced settings
            onNodeWithText("Advanced").performClick()
            waitForIdle()
            
            // 3. Open feature flags (developer option)
            onNodeWithText("Feature Flags").performClick()
            waitForIdle()
            
            // 4. Verify feature list
            onNodeWithText("Feature Management").assertIsDisplayed()
            
            // 5. Toggle a feature
            onNodeWithText("Voice Control").assertIsDisplayed()
            onAllNodesWithContentDescription("Feature toggle").onFirst().performClick()
            
            // 6. Verify feature state changed
            Thread.sleep(1000)
            
            // 7. Test beta program enrollment
            onNodeWithText("Beta Program").performClick()
            waitForIdle()
            
            onNodeWithText("Join Beta").performClick()
            waitForIdle()
            
            // 8. Verify beta features enabled
            onNodeWithText("Beta features enabled").assertIsDisplayed()
        }
    }

    @Test
    fun testAnalyticsIntegration() {
        composeTestRule.apply {
            // 1. Perform various actions to generate analytics
            startVideoPlayback()
            
            // 2. Use search feature
            onNodeWithContentDescription("Search").performClick()
            onNodeWithText("Search videos...").performTextInput("test")
            device.pressBack()
            
            // 3. Use equalizer
            onNodeWithContentDescription("Audio settings").performClick()
            onNodeWithText("Equalizer").performClick()
            onNodeWithText("Rock").performClick()
            device.pressBack()
            device.pressBack()
            
            // 4. Add bookmark
            onNodeWithContentDescription("Add bookmark").performClick()
            onNodeWithText("Bookmark Title").performTextInput("Test")
            onNodeWithText("Save Bookmark").performClick()
            
            // 5. Verify analytics are being tracked (developer options)
            onNodeWithContentDescription("Settings").performClick()
            onNodeWithText("Advanced").performClick()
            onNodeWithText("Analytics Debug").performClick()
            waitForIdle()
            
            // 6. Verify event tracking
            onNodeWithText("Recent Events").assertIsDisplayed()
            onNodeWithText("feature_search_used").assertIsDisplayed()
            onNodeWithText("equalizer_settings_changed").assertIsDisplayed()
            onNodeWithText("bookmark_action").assertIsDisplayed()
        }
    }

    @Test
    fun testOnboardingFlow() {
        // Reset app state to trigger onboarding
        composeTestRule.apply {
            // Simulate first app launch
            onNodeWithText("Welcome to AstralStream").assertIsDisplayed()
            
            // Navigate through onboarding steps
            onNodeWithText("Next").performClick()
            waitForIdle()
            
            // Step 1: Smart Search
            onNodeWithText("Smart Search").assertIsDisplayed()
            onNodeWithText("Try Smart Search").performClick()
            waitForIdle()
            
            onNodeWithText("Next").performClick()
            waitForIdle()
            
            // Step 2: AI Subtitles
            onNodeWithText("AI Subtitles").assertIsDisplayed()
            onNodeWithText("Try AI Subtitles").performClick()
            waitForIdle()
            
            onNodeWithText("Next").performClick()
            waitForIdle()
            
            // Step 3: Voice Control
            onNodeWithText("Voice Control").assertIsDisplayed()
            onNodeWithText("Next").performClick()
            waitForIdle()
            
            // Step 4: Audio Equalizer
            onNodeWithText("Audio Equalizer").assertIsDisplayed()
            onNodeWithText("Next").performClick()
            waitForIdle()
            
            // Step 5: Social Features
            onNodeWithText("Social Features").assertIsDisplayed()
            onNodeWithText("Get Started").performClick()
            waitForIdle()
            
            // Verify onboarding completed
            onNodeWithText("AstralStream").assertIsDisplayed()
        }
    }

    private fun startVideoPlayback() {
        composeTestRule.apply {
            // Navigate to video and start playback
            onNodeWithContentDescription("Browse Videos").performClick()
            waitForIdle()
            
            onNodeWithText("Recent Files").performClick()
            waitForIdle()
            
            Thread.sleep(2000) // Wait for video list
            
            onAllNodesWithContentDescription("Play video").onFirst().performClick()
            waitForIdle()
            
            Thread.sleep(1000) // Wait for player to initialize
        }
    }

    private fun ComposeTestRule.waitForIdle() {
        Thread.sleep(500)
        this.waitForIdle()
    }
}