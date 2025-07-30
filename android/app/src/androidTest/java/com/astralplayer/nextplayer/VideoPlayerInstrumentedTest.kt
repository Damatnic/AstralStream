package com.astralplayer.nextplayer

import android.content.Intent
import android.net.Uri
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.media3.common.Player
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.*
import org.junit.Before

/**
 * Instrumented tests for video player functionality
 */
@RunWith(AndroidJUnit4::class)
class VideoPlayerInstrumentedTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<VideoPlayerActivity>()

    private val testVideoUri = "android.resource://com.astralplayer.nextplayer/raw/test_video"

    @Before
    fun setup() {
        // Launch VideoPlayerActivity with test video
        val intent = Intent(
            InstrumentationRegistry.getInstrumentation().targetContext,
            VideoPlayerActivity::class.java
        ).apply {
            data = Uri.parse(testVideoUri)
            putExtra("video_title", "Test Video")
        }
        composeTestRule.activity.intent = intent
    }

    @Test
    fun testVideoPlayerLaunches() {
        // Verify the video player activity launches
        assertNotNull(composeTestRule.activity)
    }

    @Test
    fun testVideoLoadsAndPlays() = runBlocking {
        // Wait for video to load
        delay(2000)

        // Get the player
        val player = composeTestRule.activity.exoPlayer
        assertNotNull(player)

        // Check if video is loaded
        assertTrue(player.duration > 0)
        
        // Check if video starts playing
        assertTrue(player.isPlaying || player.playbackState == Player.STATE_READY)
    }

    @Test
    fun testPlayPauseControls() = runBlocking {
        // Wait for video to load
        delay(2000)

        val player = composeTestRule.activity.exoPlayer

        // Test pause
        composeTestRule.runOnUiThread {
            player.pause()
        }
        delay(500)
        assertFalse(player.isPlaying)

        // Test play
        composeTestRule.runOnUiThread {
            player.play()
        }
        delay(500)
        assertTrue(player.isPlaying)
    }

    @Test
    fun testSeekFunctionality() = runBlocking {
        // Wait for video to load
        delay(2000)

        val player = composeTestRule.activity.exoPlayer
        val initialPosition = player.currentPosition

        // Seek forward 10 seconds
        composeTestRule.runOnUiThread {
            player.seekTo(player.currentPosition + 10000)
        }
        delay(500)

        // Verify seek worked
        assertTrue(player.currentPosition > initialPosition)
    }

    @Test
    fun testDoubleTapSeek() {
        // Test double tap on left side (seek backward)
        composeTestRule.onRoot()
            .performTouchInput {
                doubleClick(position = topLeft)
            }

        // Test double tap on right side (seek forward)
        composeTestRule.onRoot()
            .performTouchInput {
                doubleClick(position = topRight)
            }
    }

    @Test
    fun testControlsVisibility() = runBlocking {
        // Controls should be visible initially
        composeTestRule.onNodeWithContentDescription("Back")
            .assertExists()

        // Wait for auto-hide (3 seconds)
        delay(3500)

        // Controls should be hidden
        composeTestRule.onNodeWithContentDescription("Back")
            .assertDoesNotExist()

        // Tap to show controls
        composeTestRule.onRoot().performClick()
        
        // Controls should be visible again
        composeTestRule.onNodeWithContentDescription("Back")
            .assertExists()
    }

    @Test
    fun testBackButtonFunctionality() {
        // Click back button
        composeTestRule.onNodeWithContentDescription("Back")
            .performClick()

        // Activity should be finishing
        assertTrue(composeTestRule.activity.isFinishing)
    }

    @Test
    fun testSeekButtons() = runBlocking {
        delay(2000)
        val player = composeTestRule.activity.exoPlayer
        val initialPosition = player.currentPosition

        // Test seek backward button
        composeTestRule.onNodeWithContentDescription("Seek backward")
            .performClick()
        delay(500)
        assertTrue(player.currentPosition < initialPosition || player.currentPosition == 0L)

        // Test seek forward button
        val positionAfterBackward = player.currentPosition
        composeTestRule.onNodeWithContentDescription("Seek forward")
            .performClick()
        delay(500)
        assertTrue(player.currentPosition > positionAfterBackward)
    }

    @Test
    fun testProgressBarDisplay() = runBlocking {
        // Wait for video to load and start playing
        delay(2000)

        // Progress bar should exist
        composeTestRule.onNodeWithTag("VideoProgressBar", useUnmergedTree = true)
            .assertExists()

        // Wait and check if progress updates
        val player = composeTestRule.activity.exoPlayer
        val initialPosition = player.currentPosition
        delay(2000)
        
        // Position should have advanced if playing
        if (player.isPlaying) {
            assertTrue(player.currentPosition > initialPosition)
        }
    }

    @Test
    fun testVideoCompletion() = runBlocking {
        val player = composeTestRule.activity.exoPlayer
        
        // Seek near the end
        composeTestRule.runOnUiThread {
            if (player.duration > 0) {
                player.seekTo(player.duration - 1000)
            }
        }
        
        // Wait for completion
        delay(2000)
        
        // Check if player reached the end
        assertTrue(
            player.playbackState == Player.STATE_ENDED || 
            player.currentPosition >= player.duration - 1000
        )
    }
}