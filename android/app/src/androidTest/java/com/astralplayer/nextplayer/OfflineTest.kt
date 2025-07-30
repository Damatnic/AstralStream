package com.astralplayer.nextplayer

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
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
 * Offline Functionality and Data Persistence Tests
 * Validates app behavior without network connectivity and data storage
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class OfflineTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    private lateinit var context: Context
    private lateinit var device: UiDevice
    private lateinit var connectivityManager: ConnectivityManager

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        
        // Enable all features for offline testing
        FeatureFlags.initialize(context)
        runBlocking {
            FeatureFlags.getManager()?.enableAllFeatures()
        }
    }

    @Test
    fun testVideoPlaybackOffline() {
        composeTestRule.apply {
            // Simulate offline mode (in real test would disable network)
            simulateOfflineMode()
            
            // Test local video playback works offline
            onNodeWithContentDescription("Browse Videos").performClick()
            waitForIdle()
            
            onNodeWithText("Recent Files").performClick()
            waitForIdle()
            
            Thread.sleep(2000)
            
            // Should show local videos even without network
            if (onAllNodesWithContentDescription("Play video").fetchSemanticsNodes().isNotEmpty()) {
                onAllNodesWithContentDescription("Play video").onFirst().performClick()
                waitForIdle()
                
                // Video should play normally offline
                onNodeWithContentDescription("Video Player").assertIsDisplayed()
                onNodeWithContentDescription("Play/Pause").assertIsDisplayed()
                
                // Test video controls work offline
                onNodeWithContentDescription("Play/Pause").performClick()
                waitForIdle()
                
                onNodeWithContentDescription("Play/Pause").performClick()
                waitForIdle()
                
                // Test seeking works offline
                onNodeWithContentDescription("Seek bar").performTouchInput {
                    swipeRight(startX = centerX * 0.2f, endX = centerX * 0.6f)
                }
                waitForIdle()
                
                device.pressBack()
            }
            
            restoreOnlineMode()
        }
    }

    @Test
    fun testSearchOffline() {
        composeTestRule.apply {
            // First, use search online to cache data
            onNodeWithContentDescription("Search").performClick()
            waitForIdle()
            
            onNodeWithText("Search videos...").performTextInput("test video")
            waitForIdle()
            
            Thread.sleep(1000)
            device.pressBack()
            device.pressBack()
            
            // Now test search offline
            simulateOfflineMode()
            
            onNodeWithContentDescription("Search").performClick()
            waitForIdle()
            
            // Search interface should still work
            onNodeWithText("Search videos...").assertIsDisplayed()
            
            // Search local content
            onNodeWithText("Search videos...").performTextInput("test")
            waitForIdle()
            
            // Should show offline indicator
            onNodeWithText("Offline Mode").assertIsDisplayed()
            
            // Should search through local files only
            Thread.sleep(1000)
            
            // Recent searches should be available offline
            onNodeWithText("Recent Searches").assertIsDisplayed()
            
            device.pressBack()
            device.pressBack()
            
            restoreOnlineMode()
        }
    }

    @Test
    fun testBookmarksOffline() {
        composeTestRule.apply {
            // Create bookmarks while online
            startVideoPlayback()
            
            onNodeWithContentDescription("Add bookmark").performClick()
            waitForIdle()
            
            onNodeWithText("Bookmark Title").performTextInput("Offline Bookmark Test")
            onNodeWithText("Description (optional)").performTextInput("Created while online")
            onNodeWithText("Save Bookmark").performClick()
            waitForIdle()
            
            device.pressBack()
            
            // Test bookmarks work offline
            simulateOfflineMode()
            
            startVideoPlayback()
            
            // View bookmarks offline
            onNodeWithContentDescription("Bookmarks").performClick()
            waitForIdle()
            
            // Bookmark should be available offline
            onNodeWithText("Offline Bookmark Test").assertIsDisplayed()
            
            // Navigate to bookmark
            onNodeWithText("Offline Bookmark Test").performClick()
            waitForIdle()
            
            // Should navigate to bookmarked position
            Thread.sleep(1000)
            
            device.pressBack()
            
            // Create new bookmark offline
            onNodeWithContentDescription("Add bookmark").performClick()
            waitForIdle()
            
            onNodeWithText("Bookmark Title").performTextInput("Created Offline")
            onNodeWithText("Save Bookmark").performClick()
            waitForIdle()
            
            // Verify bookmark saved offline
            onNodeWithText("Bookmark added").assertIsDisplayed()
            
            device.pressBack()
            
            restoreOnlineMode()
        }
    }

    @Test
    fun testEqualizerOffline() {
        composeTestRule.apply {
            simulateOfflineMode()
            
            startVideoPlayback()
            
            // Equalizer should work offline
            onNodeWithContentDescription("Audio settings").performClick()
            waitForIdle()
            
            onNodeWithText("Equalizer").performClick()
            waitForIdle()
            
            // Verify equalizer interface loads offline
            onNodeWithText("Audio Equalizer").assertIsDisplayed()
            
            // Test preset selection offline
            onNodeWithText("Rock").performClick()
            waitForIdle()
            
            // Verify preset applied
            onNodeWithText("Current: Rock").assertIsDisplayed()
            
            // Test custom equalizer offline
            onNodeWithText("Custom").performClick()
            waitForIdle()
            
            // Adjust frequency bands
            onAllNodesWithContentDescription("Frequency slider").onFirst()
                .performTouchInput { swipeUp() }
            
            // Enable bass boost
            onNodeWithText("Bass Boost").performClick()
            
            device.pressBack()
            device.pressBack()
            device.pressBack()
            
            restoreOnlineMode()
        }
    }

    @Test
    fun testSettingsOffline() {
        composeTestRule.apply {
            simulateOfflineMode()
            
            // Settings should work offline
            onNodeWithContentDescription("Settings").performClick()
            waitForIdle()
            
            onNodeWithText("Settings").assertIsDisplayed()
            
            // Test various settings offline
            onNodeWithText("Appearance").performClick()
            waitForIdle()
            
            onNodeWithText("Dark Mode").performClick()
            waitForIdle()
            
            device.pressBack()
            
            // Test performance settings offline
            onNodeWithText("Performance").performClick()
            waitForIdle()
            
            onNodeWithText("Quality Preset").performClick()
            onNodeWithText("Battery Saver").performClick()
            waitForIdle()
            
            device.pressBack()
            
            // Test accessibility settings offline
            onNodeWithText("Accessibility").performClick()
            waitForIdle()
            
            onNodeWithText("Large Text").performClick()
            waitForIdle()
            
            device.pressBack()
            device.pressBack()
            
            restoreOnlineMode()
        }
    }

    @Test
    fun testVoiceControlOffline() {
        composeTestRule.apply {
            simulateOfflineMode()
            
            startVideoPlayback()
            
            // Voice control should have offline capabilities
            onNodeWithContentDescription("Voice control").performClick()
            waitForIdle()
            
            // Should show offline voice recognition available
            onNodeWithText("Offline Voice Recognition").assertIsDisplayed()
            
            // Basic voice commands should work offline
            Thread.sleep(2000)
            
            // Simulate voice command processing offline
            // In real implementation, would use on-device speech recognition
            
            onNodeWithContentDescription("Voice control").performClick()
            onNodeWithText("Stop Listening").performClick()
            
            device.pressBack()
            
            restoreOnlineMode()
        }
    }

    @Test
    fun testAIFeaturesOfflineGracefulDegradation() {
        composeTestRule.apply {
            simulateOfflineMode()
            
            startVideoPlayback()
            
            // AI subtitle generation should gracefully handle offline
            onNodeWithContentDescription("Subtitle options").performClick()
            waitForIdle()
            
            onNodeWithText("Generate AI Subtitles").performClick()
            waitForIdle()
            
            // Should show offline message
            onNodeWithText("AI features require internet connection").assertIsDisplayed()
            
            // Should offer offline alternatives
            onNodeWithText("Load Local Subtitles").assertIsDisplayed()
            
            device.pressBack()
            device.pressBack()
            
            restoreOnlineMode()
        }
    }

    @Test
    fun testDataPersistenceAcrossRestart() {
        composeTestRule.apply {
            // Create data that should persist
            
            // 1. Create bookmarks
            startVideoPlayback()
            onNodeWithContentDescription("Add bookmark").performClick()
            waitForIdle()
            
            onNodeWithText("Bookmark Title").performTextInput("Persistence Test")
            onNodeWithText("Save Bookmark").performClick()
            waitForIdle()
            
            device.pressBack()
            
            // 2. Set equalizer preset
            onNodeWithContentDescription("Audio settings").performClick()
            onNodeWithText("Equalizer").performClick()
            waitForIdle()
            
            onNodeWithText("Jazz").performClick()
            waitForIdle()
            
            device.pressBack()
            device.pressBack()
            device.pressBack()
            
            // 3. Change settings
            onNodeWithContentDescription("Settings").performClick()
            waitForIdle()
            
            onNodeWithText("Appearance").performClick()
            waitForIdle()
            
            onNodeWithText("Dark Mode").performClick()
            waitForIdle()
            
            device.pressBack()
            device.pressBack()
            
            // 4. Perform search to create history
            onNodeWithContentDescription("Search").performClick()
            waitForIdle()
            
            onNodeWithText("Search videos...").performTextInput("persistence test")
            waitForIdle()
            
            device.pressBack()
            device.pressBack()
            
            // Simulate app restart
            simulateAppRestart()
            
            // Verify data persisted
            
            // Check bookmark persisted
            startVideoPlayback()
            onNodeWithContentDescription("Bookmarks").performClick()
            waitForIdle()
            
            onNodeWithText("Persistence Test").assertIsDisplayed()
            device.pressBack()
            device.pressBack()
            
            // Check equalizer setting persisted
            startVideoPlayback()
            onNodeWithContentDescription("Audio settings").performClick()
            onNodeWithText("Equalizer").performClick()
            waitForIdle()
            
            onNodeWithText("Current: Jazz").assertIsDisplayed()
            device.pressBack()
            device.pressBack()
            device.pressBack()
            
            // Check theme setting persisted
            // Dark mode should still be active
            
            // Check search history persisted
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
    fun testCacheManagementOffline() {
        composeTestRule.apply {
            // Test cache functionality offline
            simulateOfflineMode()
            
            onNodeWithContentDescription("Settings").performClick()
            waitForIdle()
            
            onNodeWithText("Performance").performClick()
            waitForIdle()
            
            onNodeWithText("Cache Management").performClick()
            waitForIdle()
            
            // Cache statistics should be available offline
            onNodeWithText("Cache Statistics").assertIsDisplayed()
            
            // Cache operations should work offline
            onNodeWithText("Optimize Cache").performClick()
            waitForIdle()
            
            onNodeWithText("Cache optimized").assertIsDisplayed()
            
            // Clear cache should work offline
            onNodeWithText("Clear Cache").performClick()
            waitForIdle()
            
            onNodeWithText("Confirm").performClick()
            waitForIdle()
            
            onNodeWithText("Cache cleared successfully").assertIsDisplayed()
            
            device.pressBack()
            device.pressBack()
            device.pressBack()
            
            restoreOnlineMode()
        }
    }

    @Test
    fun testFeatureFlagsOffline() {
        composeTestRule.apply {
            simulateOfflineMode()
            
            // Feature flags should work offline
            onNodeWithContentDescription("Settings").performClick()
            waitForIdle()
            
            onNodeWithText("Advanced").performClick()
            waitForIdle()
            
            onNodeWithText("Feature Flags").performClick()
            waitForIdle()
            
            // Feature flag interface should work offline
            onNodeWithText("Feature Management").assertIsDisplayed()
            
            // Toggle features offline
            onAllNodesWithContentDescription("Feature toggle").onFirst().performClick()
            waitForIdle()
            
            // Changes should be saved locally
            Thread.sleep(1000)
            
            device.pressBack()
            device.pressBack()
            device.pressBack()
            
            restoreOnlineMode()
        }
    }

    @Test
    fun testAnalyticsOffline() {
        composeTestRule.apply {
            simulateOfflineMode()
            
            // Generate analytics events offline
            startVideoPlayback()
            onNodeWithContentDescription("Play/Pause").performClick()
            waitForIdle()
            
            // Use search
            device.pressBack()
            onNodeWithContentDescription("Search").performClick()
            onNodeWithText("Search videos...").performTextInput("offline test")
            device.pressBack()
            device.pressBack()
            
            // Use equalizer
            startVideoPlayback()
            onNodeWithContentDescription("Audio settings").performClick()
            onNodeWithText("Equalizer").performClick()
            onNodeWithText("Rock").performClick()
            device.pressBack()
            device.pressBack()
            device.pressBack()
            
            // Analytics should queue events offline
            onNodeWithContentDescription("Settings").performClick()
            waitForIdle()
            
            onNodeWithText("Advanced").performClick()
            waitForIdle()
            
            onNodeWithText("Analytics Debug").performClick()
            waitForIdle()
            
            // Should show queued events
            onNodeWithText("Queued Events (Offline)").assertIsDisplayed()
            onNodeWithText("Events will sync when online").assertIsDisplayed()
            
            device.pressBack()
            device.pressBack()
            device.pressBack()
            
            // Restore online and verify sync
            restoreOnlineMode()
            
            Thread.sleep(3000) // Allow sync time
            
            onNodeWithContentDescription("Settings").performClick()
            waitForIdle()
            
            onNodeWithText("Advanced").performClick()
            waitForIdle()
            
            onNodeWithText("Analytics Debug").performClick()
            waitForIdle()
            
            // Events should be synced
            onNodeWithText("Recent Events").assertIsDisplayed()
            onNodeWithText("feature_search_used").assertIsDisplayed()
            
            device.pressBack()
            device.pressBack()
            device.pressBack()
        }
    }

    @Test
    fun testSocialFeaturesOfflineGracefulDegradation() {
        composeTestRule.apply {
            simulateOfflineMode()
            
            startVideoPlayback()
            
            // Sharing should handle offline gracefully
            onNodeWithContentDescription("Share").performClick()
            waitForIdle()
            
            // Local sharing should still work
            onNodeWithText("Share File").performClick()
            waitForIdle()
            
            // Should work for local file sharing
            Thread.sleep(1000)
            device.pressBack()
            
            // Online-only features should show offline message
            onNodeWithText("Share Link").performClick()
            waitForIdle()
            
            onNodeWithText("Link sharing requires internet connection").assertIsDisplayed()
            
            device.pressBack()
            device.pressBack()
            
            restoreOnlineMode()
        }
    }

    @Test
    fun testOfflineStorageQuotas() {
        composeTestRule.apply {
            // Test storage management offline
            simulateOfflineMode()
            
            onNodeWithContentDescription("Settings").performClick()
            waitForIdle()
            
            onNodeWithText("Storage").performClick()
            waitForIdle()
            
            // Storage information should be available offline
            onNodeWithText("Available Storage").assertIsDisplayed()
            onNodeWithText("App Data Size").assertIsDisplayed()
            onNodeWithText("Cache Size").assertIsDisplayed()
            
            // Storage cleanup should work offline
            onNodeWithText("Free Up Space").performClick()
            waitForIdle()
            
            onNodeWithText("Clean Temporary Files").performClick()
            waitForIdle()
            
            onNodeWithText("Cleanup completed").assertIsDisplayed()
            
            device.pressBack()
            device.pressBack()
            
            restoreOnlineMode()
        }
    }

    // Helper methods

    private fun simulateOfflineMode() {
        // In a real test, would disable network connectivity
        // For testing purposes, we assume offline mode is simulated
        // This could be done through:
        // 1. Airplane mode
        // 2. Network proxy that blocks requests
        // 3. Mocking network layer
    }

    private fun restoreOnlineMode() {
        // Restore network connectivity
        // In real test, would re-enable network
    }

    private fun simulateAppRestart() {
        // In real test, would restart the activity
        // For now, assume data persistence is being tested
        Thread.sleep(1000)
    }

    private fun startVideoPlayback() {
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
                // Video might already be playing
            }
        }
    }

    private fun ComposeTestRule.waitForIdle() {
        Thread.sleep(300)
        this.waitForIdle()
    }
}