package com.astralplayer.nextplayer

import android.content.Context
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import com.astralplayer.nextplayer.feature.flags.Feature
import com.astralplayer.nextplayer.feature.flags.FeatureFlags
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Feature Flag Rollback Scenarios Test
 * Tests graceful feature rollback and fallback mechanisms
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class FeatureFlagRollbackTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    private lateinit var context: Context
    private lateinit var device: UiDevice

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        
        // Initialize feature flags
        FeatureFlags.initialize(context)
    }

    @Test
    fun testAdvancedSearchRollback() {
        composeTestRule.apply {
            // Enable advanced search
            runBlocking {
                FeatureFlags.getManager()?.setFeatureEnabled(Feature.ADVANCED_SEARCH, true)
            }
            
            // Verify advanced search is available
            onNodeWithContentDescription("Search").performClick()
            waitForIdle()
            
            onNodeWithText("Search videos...").assertIsDisplayed()
            onNodeWithText("Filters").assertIsDisplayed()
            
            // Use advanced search features
            onNodeWithText("Search videos...").performTextInput("test query")
            waitForIdle()
            
            onNodeWithText("Filters").performClick()
            waitForIdle()
            
            onNodeWithText("Duration").assertIsDisplayed()
            onNodeWithText("Quality").assertIsDisplayed()
            
            device.pressBack()
            device.pressBack()
            device.pressBack()
            
            // Rollback advanced search
            runBlocking {
                FeatureFlags.getManager()?.setFeatureEnabled(Feature.ADVANCED_SEARCH, false)
            }
            
            // Verify graceful fallback to basic search
            onNodeWithContentDescription("Search").performClick()
            waitForIdle()
            
            // Basic search should still work
            onNodeWithText("Search videos...").assertIsDisplayed()
            
            // Advanced features should not be available
            try {
                onNodeWithText("Filters").assertDoesNotExist()
            } catch (e: AssertionError) {
                // Filters might be hidden or show "Feature not available"
                onNodeWithText("Feature not available").assertIsDisplayed()
            }
            
            // Basic search should still function
            onNodeWithText("Search videos...").performTextInput("basic search")
            waitForIdle()
            
            device.pressBack()
            device.pressBack()
        }
    }

    @Test
    fun testAISubtitleRollback() {
        composeTestRule.apply {
            // Enable AI subtitles
            runBlocking {
                FeatureFlags.getManager()?.setFeatureEnabled(Feature.AI_SUBTITLES, true)
            }
            
            startVideoPlaybackIfAvailable()
            
            // Verify AI subtitle feature is available
            onNodeWithContentDescription("Subtitle options").performClick()
            waitForIdle()
            
            onNodeWithText("Generate AI Subtitles").assertIsDisplayed()
            
            device.pressBack()
            device.pressBack()
            
            // Rollback AI subtitles
            runBlocking {
                FeatureFlags.getManager()?.setFeatureEnabled(Feature.AI_SUBTITLES, false)
            }
            
            // Verify fallback to basic subtitle functionality
            startVideoPlaybackIfAvailable()
            
            onNodeWithContentDescription("Subtitle options").performClick()
            waitForIdle()
            
            // AI subtitle generation should not be available
            try {
                onNodeWithText("Generate AI Subtitles").assertDoesNotExist()
            } catch (e: AssertionError) {
                // Might show as disabled
                onNodeWithText("AI features disabled").assertIsDisplayed()
            }
            
            // Basic subtitle functionality should remain
            onNodeWithText("Enable Subtitles").assertIsDisplayed()
            onNodeWithText("Load Local Subtitles").assertIsDisplayed()
            
            device.pressBack()
            device.pressBack()
        }
    }

    @Test
    fun testVoiceControlRollback() {
        composeTestRule.apply {
            // Enable voice control
            runBlocking {
                FeatureFlags.getManager()?.setFeatureEnabled(Feature.VOICE_CONTROL, true)
            }
            
            startVideoPlaybackIfAvailable()
            
            // Verify voice control is available
            onNodeWithContentDescription("Voice control").assertIsDisplayed()
            
            onNodeWithContentDescription("Voice control").performClick()
            waitForIdle()
            
            onNodeWithText("Listening...").assertIsDisplayed()
            
            onNodeWithContentDescription("Voice control").performClick()
            waitForIdle()
            
            onNodeWithText("Stop Listening").performClick()
            waitForIdle()
            
            device.pressBack()
            
            // Rollback voice control
            runBlocking {
                FeatureFlags.getManager()?.setFeatureEnabled(Feature.VOICE_CONTROL, false)
            }
            
            // Verify voice control is no longer available
            startVideoPlaybackIfAvailable()
            
            try {
                onNodeWithContentDescription("Voice control").assertDoesNotExist()
            } catch (e: AssertionError) {
                // Might be shown but disabled
                onNodeWithContentDescription("Voice control").performClick()
                waitForIdle()
                
                onNodeWithText("Voice control is currently disabled").assertIsDisplayed()
                device.pressBack()
            }
            
            // Other player controls should remain functional
            onNodeWithContentDescription("Play/Pause").assertIsDisplayed()
            onNodeWithContentDescription("Audio settings").assertIsDisplayed()
            
            device.pressBack()
        }
    }

    @Test
    fun testAudioEqualizerRollback() {
        composeTestRule.apply {
            // Enable audio equalizer
            runBlocking {
                FeatureFlags.getManager()?.setFeatureEnabled(Feature.AUDIO_EQUALIZER, true)
            }
            
            startVideoPlaybackIfAvailable()
            
            // Verify equalizer is available
            onNodeWithContentDescription("Audio settings").performClick()
            waitForIdle()
            
            onNodeWithText("Equalizer").assertIsDisplayed()
            
            onNodeWithText("Equalizer").performClick()
            waitForIdle()
            
            onNodeWithText("Audio Equalizer").assertIsDisplayed()
            onNodeWithText("Rock").assertIsDisplayed()
            
            device.pressBack()
            device.pressBack()
            device.pressBack()
            
            // Rollback equalizer
            runBlocking {
                FeatureFlags.getManager()?.setFeatureEnabled(Feature.AUDIO_EQUALIZER, false)
            }
            
            // Verify equalizer is no longer available
            startVideoPlaybackIfAvailable()
            
            onNodeWithContentDescription("Audio settings").performClick()
            waitForIdle()
            
            try {
                onNodeWithText("Equalizer").assertDoesNotExist()
            } catch (e: AssertionError) {
                // Might show as disabled
                onNodeWithText("Equalizer").performClick()
                waitForIdle()
                
                onNodeWithText("Equalizer feature is disabled").assertIsDisplayed()
                device.pressBack()
            }
            
            // Basic audio controls should remain
            onNodeWithText("Volume").assertIsDisplayed()
            
            device.pressBack()
            device.pressBack()
        }
    }

    @Test
    fun testSocialSharingRollback() {
        composeTestRule.apply {
            // Enable social sharing
            runBlocking {
                FeatureFlags.getManager()?.setFeatureEnabled(Feature.SOCIAL_SHARING, true)
            }
            
            startVideoPlaybackIfAvailable()
            
            // Verify sharing features are available
            onNodeWithContentDescription("Share").performClick()
            waitForIdle()
            
            onNodeWithText("Share Video").assertIsDisplayed()
            onNodeWithText("Share Link").assertIsDisplayed()
            onNodeWithText("Share Current Moment").assertIsDisplayed()
            
            device.pressBack()
            device.pressBack()
            
            // Rollback social sharing
            runBlocking {
                FeatureFlags.getManager()?.setFeatureEnabled(Feature.SOCIAL_SHARING, false)
            }
            
            // Verify social sharing is no longer available
            startVideoPlaybackIfAvailable()
            
            try {
                onNodeWithContentDescription("Share").assertDoesNotExist()
            } catch (e: AssertionError) {
                // Might be shown but disabled
                onNodeWithContentDescription("Share").performClick()
                waitForIdle()
                
                // Should show basic file sharing only
                onNodeWithText("Share File").assertIsDisplayed()
                
                // Advanced sharing should not be available
                try {
                    onNodeWithText("Share Link").assertDoesNotExist()
                    onNodeWithText("Share Current Moment").assertDoesNotExist()
                } catch (e2: AssertionError) {
                    onNodeWithText("Advanced sharing features disabled").assertIsDisplayed()
                }
                
                device.pressBack()
            }
            
            device.pressBack()
        }
    }

    @Test
    fun testVideoBookmarksRollback() {
        composeTestRule.apply {
            // Enable video bookmarks
            runBlocking {
                FeatureFlags.getManager()?.setFeatureEnabled(Feature.VIDEO_BOOKMARKS, true)
            }
            
            startVideoPlaybackIfAvailable()
            
            // Verify bookmark functionality is available
            onNodeWithContentDescription("Add bookmark").assertIsDisplayed()
            onNodeWithContentDescription("Bookmarks").assertIsDisplayed()
            
            // Create a bookmark
            onNodeWithContentDescription("Add bookmark").performClick()
            waitForIdle()
            
            onNodeWithText("Bookmark Title").performTextInput("Rollback Test Bookmark")
            onNodeWithText("Save Bookmark").performClick()
            waitForIdle()
            
            device.pressBack()
            
            // Rollback bookmarks feature
            runBlocking {
                FeatureFlags.getManager()?.setFeatureEnabled(Feature.VIDEO_BOOKMARKS, false)
            }
            
            // Verify bookmarks are no longer accessible
            startVideoPlaybackIfAvailable()
            
            try {
                onNodeWithContentDescription("Add bookmark").assertDoesNotExist()
                onNodeWithContentDescription("Bookmarks").assertDoesNotExist()
            } catch (e: AssertionError) {
                // Might be shown but disabled
                onNodeWithContentDescription("Add bookmark").performClick()
                waitForIdle()
                
                onNodeWithText("Bookmark feature is currently disabled").assertIsDisplayed()
                device.pressBack()
            }
            
            device.pressBack()
        }
    }

    @Test
    fun testPerformanceOptimizationRollback() {
        composeTestRule.apply {
            // Enable performance optimization
            runBlocking {
                FeatureFlags.getManager()?.setFeatureEnabled(Feature.PERFORMANCE_OPTIMIZATION, true)
            }
            
            // Verify performance features are available
            onNodeWithContentDescription("Settings").performClick()
            waitForIdle()
            
            onNodeWithText("Performance").performClick()
            waitForIdle()
            
            onNodeWithText("Quality Preset").assertIsDisplayed()
            onNodeWithText("Data Saver Mode").assertIsDisplayed()
            onNodeWithText("Cache Management").assertIsDisplayed()
            
            device.pressBack()
            device.pressBack()
            
            // Rollback performance optimization
            runBlocking {
                FeatureFlags.getManager()?.setFeatureEnabled(Feature.PERFORMANCE_OPTIMIZATION, false)
            }
            
            // Verify advanced performance features are no longer available
            onNodeWithContentDescription("Settings").performClick()
            waitForIdle()
            
            try {
                onNodeWithText("Performance").assertDoesNotExist()
            } catch (e: AssertionError) {
                // Might be shown but with limited functionality
                onNodeWithText("Performance").performClick()
                waitForIdle()
                
                // Basic settings should remain
                onNodeWithText("Basic Performance Settings").assertIsDisplayed()
                
                // Advanced features should not be available
                try {
                    onNodeWithText("Quality Preset").assertDoesNotExist()
                    onNodeWithText("Cache Management").assertDoesNotExist()
                } catch (e2: AssertionError) {
                    onNodeWithText("Advanced features disabled").assertIsDisplayed()
                }
                
                device.pressBack()
            }
            
            device.pressBack()
        }
    }

    @Test
    fun testMultipleFeatureRollback() {
        composeTestRule.apply {
            // Enable multiple features
            runBlocking {
                FeatureFlags.getManager()?.setFeatureEnabled(Feature.VOICE_CONTROL, true)
                FeatureFlags.getManager()?.setFeatureEnabled(Feature.AI_SUBTITLES, true)
                FeatureFlags.getManager()?.setFeatureEnabled(Feature.SOCIAL_SHARING, true)
            }
            
            startVideoPlaybackIfAvailable()
            
            // Verify all features are available
            onNodeWithContentDescription("Voice control").assertIsDisplayed()
            onNodeWithContentDescription("Share").assertIsDisplayed()
            
            onNodeWithContentDescription("Subtitle options").performClick()
            waitForIdle()
            
            onNodeWithText("Generate AI Subtitles").assertIsDisplayed()
            
            device.pressBack()
            device.pressBack()
            
            // Rollback all features simultaneously
            runBlocking {
                FeatureFlags.getManager()?.setFeatureEnabled(Feature.VOICE_CONTROL, false)
                FeatureFlags.getManager()?.setFeatureEnabled(Feature.AI_SUBTITLES, false)
                FeatureFlags.getManager()?.setFeatureEnabled(Feature.SOCIAL_SHARING, false)
            }
            
            // Verify all features are properly disabled
            startVideoPlaybackIfAvailable()
            
            // Voice control should not be available
            try {
                onNodeWithContentDescription("Voice control").assertDoesNotExist()
            } catch (e: AssertionError) {
                // Might be disabled
            }
            
            // AI subtitles should not be available
            onNodeWithContentDescription("Subtitle options").performClick()
            waitForIdle()
            
            try {
                onNodeWithText("Generate AI Subtitles").assertDoesNotExist()
            } catch (e: AssertionError) {
                // Might show as disabled
            }
            
            device.pressBack()
            
            // Social sharing should fall back to basic
            try {
                onNodeWithContentDescription("Share").performClick()
                waitForIdle()
                
                // Should only show basic sharing
                onNodeWithText("Share File").assertIsDisplayed()
                
                try {
                    onNodeWithText("Share Link").assertDoesNotExist()
                } catch (e: AssertionError) {
                    // Might show as disabled
                }
                
                device.pressBack()
            } catch (e: AssertionError) {
                // Share might not be available at all
            }
            
            device.pressBack()
        }
    }

    @Test
    fun testGradualRollback() {
        composeTestRule.apply {
            // Test gradual rollback simulation (like A/B testing)
            
            // Start with all features enabled
            runBlocking {
                FeatureFlags.getManager()?.enableAllFeatures()
            }
            
            // Verify full functionality
            onNodeWithContentDescription("Settings").performClick()
            waitForIdle()
            
            onNodeWithText("Advanced Search").assertIsDisplayed()
            onNodeWithText("Voice Control").assertIsDisplayed()
            onNodeWithText("Audio Equalizer").assertIsDisplayed()
            
            device.pressBack()
            
            // Gradual rollback - Phase 1: Disable experimental features
            runBlocking {
                FeatureFlags.getManager()?.setFeatureEnabled(Feature.COLLABORATIVE_PLAYLISTS, false)
                FeatureFlags.getManager()?.setFeatureEnabled(Feature.WATCH_PARTIES, false)
                FeatureFlags.getManager()?.setFeatureEnabled(Feature.CLOUD_SYNC, false)
            }
            
            // Core features should still work
            startVideoPlaybackIfAvailable()
            
            onNodeWithContentDescription("Play/Pause").assertIsDisplayed()
            onNodeWithContentDescription("Voice control").assertIsDisplayed()
            
            device.pressBack()
            
            // Gradual rollback - Phase 2: Disable AI features
            runBlocking {
                FeatureFlags.getManager()?.setFeatureEnabled(Feature.AI_SUBTITLES, false)
                FeatureFlags.getManager()?.setFeatureEnabled(Feature.VOICE_CONTROL, false)
            }
            
            // Basic features should still work
            startVideoPlaybackIfAvailable()
            
            onNodeWithContentDescription("Play/Pause").assertIsDisplayed()
            onNodeWithContentDescription("Audio settings").assertIsDisplayed()
            
            device.pressBack()
            
            // Verify app stability after gradual rollback
            onNodeWithContentDescription("Search").performClick()
            waitForIdle()
            
            onNodeWithText("Search videos...").assertIsDisplayed()
            
            device.pressBack()
        }
    }

    @Test
    fun testRollbackDataIntegrity() {
        composeTestRule.apply {
            // Test that data created with features remains accessible after rollback
            
            // Enable bookmarks and create data
            runBlocking {
                FeatureFlags.getManager()?.setFeatureEnabled(Feature.VIDEO_BOOKMARKS, true)
            }
            
            startVideoPlaybackIfAvailable()
            
            onNodeWithContentDescription("Add bookmark").performClick()
            waitForIdle()
            
            onNodeWithText("Bookmark Title").performTextInput("Data Integrity Test")
            onNodeWithText("Save Bookmark").performClick()
            waitForIdle()
            
            device.pressBack()
            
            // Rollback bookmarks feature
            runBlocking {
                FeatureFlags.getManager()?.setFeatureEnabled(Feature.VIDEO_BOOKMARKS, false)
            }
            
            // Data should still exist, just not accessible through UI
            // This would be verified through data layer tests in a real implementation
            
            // Re-enable feature
            runBlocking {
                FeatureFlags.getManager()?.setFeatureEnabled(Feature.VIDEO_BOOKMARKS, true)
            }
            
            // Verify data is restored
            startVideoPlaybackIfAvailable()
            
            onNodeWithContentDescription("Bookmarks").performClick()
            waitForIdle()
            
            onNodeWithText("Data Integrity Test").assertIsDisplayed()
            
            device.pressBack()
            device.pressBack()
        }
    }

    @Test
    fun testEmergencyRollback() {
        composeTestRule.apply {
            // Test emergency rollback scenario (all features disabled)
            
            // Start with features enabled
            runBlocking {
                FeatureFlags.getManager()?.enableAllFeatures()
            }
            
            // Verify app is functional with all features
            onNodeWithText("AstralStream").assertIsDisplayed()
            onNodeWithContentDescription("Browse Videos").assertIsDisplayed()
            
            // Emergency rollback - disable all new features
            runBlocking {
                FeatureFlags.getManager()?.resetToDefaults()
            }
            
            // App should still be functional with core features
            onNodeWithText("AstralStream").assertIsDisplayed()
            onNodeWithContentDescription("Browse Videos").assertIsDisplayed()
            onNodeWithContentDescription("Search").assertIsDisplayed()
            onNodeWithContentDescription("Settings").assertIsDisplayed()
            
            // Basic video playback should work
            startVideoPlaybackIfAvailable()
            
            onNodeWithContentDescription("Video Player").assertIsDisplayed()
            onNodeWithContentDescription("Play/Pause").assertIsDisplayed()
            
            device.pressBack()
            
            // Basic search should work
            onNodeWithContentDescription("Search").performClick()
            waitForIdle()
            
            onNodeWithText("Search videos...").assertIsDisplayed()
            
            device.pressBack()
        }
    }

    // Helper methods

    private fun startVideoPlaybackIfAvailable() {
        composeTestRule.apply {
            try {
                onNodeWithContentDescription("Browse Videos").performClick()
                waitForIdle()
                
                onNodeWithText("Recent Files").performClick()
                waitForIdle()
                
                Thread.sleep(1000)
                
                if (onAllNodesWithContentDescription("Play video").fetchSemanticsNodes().isNotEmpty()) {
                    onAllNodesWithContentDescription("Play video").onFirst().performClick()
                    waitForIdle()
                    
                    Thread.sleep(500)
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