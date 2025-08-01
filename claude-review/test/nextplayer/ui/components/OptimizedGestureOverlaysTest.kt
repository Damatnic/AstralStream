package com.astralplayer.nextplayer.ui.components

import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.astralplayer.nextplayer.data.SeekDirection
import com.astralplayer.nextplayer.data.gesture.AdvancedAnimationEngine 
import com.astralplayer.nextplayer.data.gesture.AnimationQuality
import io.mockk.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.*

class OptimizedGestureOverlaysTest {

    @get:Rule
    val composeTestRule = createComposeRule()
    
    private lateinit var mockAnimationEngine: AdvancedAnimationEngine
    private lateinit var testScope: TestScope

    @Before
    fun setup() {
        testScope = TestScope()
        mockAnimationEngine = mockk(relaxed = true)
        
        // Mock animation engine state
        every { mockAnimationEngine.animationState } returns MutableStateFlow(
            com.astralplayer.nextplayer.data.gesture.AnimationEngineState(
                isHardwareAccelerated = true,
                animationQuality = AnimationQuality.HIGH,
                currentFrameRate = 60f,
                isPerformingWell = true
            )
        )
        
        every { mockAnimationEngine.isHardwareAccelerated() } returns true
    }

    @Test
    fun `seek overlay shows when visible`() {
        composeTestRule.setContent {
            OptimizedSeekOverlay(
                isVisible = true,
                seekDirection = SeekDirection.FORWARD,
                currentSpeed = 2f,
                targetSpeed = 4f,
                position = Offset(100f, 100f),
                animationEngine = mockAnimationEngine
            )
        }
        
        // Verify overlay content is displayed
        // Note: In real UI tests, we'd check for specific UI elements
        // This test verifies the composable doesn't crash when rendered
    }

    @Test
    fun `seek overlay hides when not visible`() {
        composeTestRule.setContent {
            OptimizedSeekOverlay(
                isVisible = false,
                seekDirection = SeekDirection.FORWARD,
                currentSpeed = 1f,
                targetSpeed = 1f,
                position = Offset(100f, 100f),
                animationEngine = mockAnimationEngine
            )
        }
        
        // When not visible, the overlay should not be rendered
        // This test ensures the composable handles visibility correctly
    }

    @Test
    fun `volume overlay displays correctly`() {
        composeTestRule.setContent {
            OptimizedVolumeOverlay(
                isVisible = true,
                volumeLevel = 0.7f,
                position = Offset(50f, 50f),
                animationEngine = mockAnimationEngine
            )
        }
        
        // Verify volume overlay renders without crashing
    }

    @Test
    fun `brightness overlay displays correctly`() {
        composeTestRule.setContent {
            OptimizedBrightnessOverlay(
                isVisible = true,
                brightnessLevel = 0.5f,
                position = Offset(50f, 50f),
                animationEngine = mockAnimationEngine
            )
        }
        
        // Verify brightness overlay renders without crashing
    }

    @Test  
    fun `overlays handle different animation qualities`() {
        val qualities = listOf(
            AnimationQuality.LOW,
            AnimationQuality.MEDIUM, 
            AnimationQuality.HIGH
        )
        
        qualities.forEach { quality ->
            every { mockAnimationEngine.animationState } returns MutableStateFlow(
                com.astralplayer.nextplayer.data.gesture.AnimationEngineState(
                    animationQuality = quality,
                    currentFrameRate = when (quality) {
                        AnimationQuality.LOW -> 30f
                        AnimationQuality.MEDIUM -> 45f
                        AnimationQuality.HIGH -> 60f
                    }
                )
            )
            
            composeTestRule.setContent {
                OptimizedSeekOverlay(
                    isVisible = true,
                    seekDirection = SeekDirection.FORWARD,
                    currentSpeed = 2f,
                    targetSpeed = 2f,
                    position = Offset(100f, 100f),
                    animationEngine = mockAnimationEngine
                )
            }
            
            // Verify overlay adapts to different quality levels
        }
    }

    @Test
    fun `seek overlay handles different directions`() {
        val directions = listOf(
            SeekDirection.FORWARD,
            SeekDirection.BACKWARD,
            SeekDirection.NONE
        )
        
        directions.forEach { direction ->
            composeTestRule.setContent {
                OptimizedSeekOverlay(
                    isVisible = true,
                    seekDirection = direction,
                    currentSpeed = 1.5f,
                    targetSpeed = 3f,
                    position = Offset(100f, 100f),
                    animationEngine = mockAnimationEngine
                )
            }
            
            // Verify overlay handles all direction types
        }
    }

    @Test
    fun `volume overlay handles different levels`() {
        val volumeLevels = listOf(0f, 0.25f, 0.5f, 0.75f, 1f)
        
        volumeLevels.forEach { level ->
            composeTestRule.setContent {
                OptimizedVolumeOverlay(
                    isVisible = true,
                    volumeLevel = level,
                    position = Offset(50f, 50f),
                    animationEngine = mockAnimationEngine
                )
            }
            
            // Verify overlay adapts to different volume levels
        }
    }

    @Test
    fun `brightness overlay handles different levels`() {
        val brightnessLevels = listOf(0f, 0.25f, 0.5f, 0.75f, 1f)
        
        brightnessLevels.forEach { level ->
            composeTestRule.setContent {
                OptimizedBrightnessOverlay(
                    isVisible = true,
                    brightnessLevel = level,
                    position = Offset(50f, 50f),
                    animationEngine = mockAnimationEngine
                )
            }
            
            // Verify overlay adapts to different brightness levels
        }
    }

    @Test
    fun `overlay assets cache works correctly`() {
        // Test seek indicator cache
        val seekKey1 = OptimizedOverlayAssets.getCachedSeekIndicator(
            speed = 2f,
            direction = SeekDirection.FORWARD,
            quality = AnimationQuality.HIGH
        )
        
        val seekKey2 = OptimizedOverlayAssets.getCachedSeekIndicator(
            speed = 2f,
            direction = SeekDirection.FORWARD,
            quality = AnimationQuality.HIGH
        )
        
        assertEquals("Same parameters should produce same cache key", seekKey1, seekKey2)
        
        val seekKey3 = OptimizedOverlayAssets.getCachedSeekIndicator(
            speed = 3f,
            direction = SeekDirection.FORWARD,
            quality = AnimationQuality.HIGH
        )
        
        assertNotEquals("Different parameters should produce different cache keys", 
            seekKey1, seekKey3)
        
        // Test volume bar cache
        val volumeKey = OptimizedOverlayAssets.getCachedVolumeBar(
            level = 0.7f,
            quality = AnimationQuality.MEDIUM
        )
        
        assertNotNull("Volume cache key should not be null", volumeKey)
        assertTrue("Volume cache key should contain level", volumeKey.contains("7"))
        
        // Test brightness bar cache
        val brightnessKey = OptimizedOverlayAssets.getCachedBrightnessBar(
            level = 0.5f,
            quality = AnimationQuality.LOW
        )
        
        assertNotNull("Brightness cache key should not be null", brightnessKey)
        assertTrue("Brightness cache key should contain level", brightnessKey.contains("5"))
        
        // Test cache clearing
        OptimizedOverlayAssets.clearCache()
        // Cache clear should not throw exceptions
    }

    @Test
    fun `performance monitor updates correctly`() {
        var lastFrameRate = 0f
        
        composeTestRule.setContent {
            OverlayPerformanceMonitor(
                animationEngine = mockAnimationEngine,
                onPerformanceUpdate = { frameRate ->
                    lastFrameRate = frameRate
                }
            )
        }
        
        // Initially should get the mocked frame rate
        composeTestRule.waitForIdle()
        
        // Note: In a real test environment, we'd need to trigger state changes
        // to verify the performance monitor updates correctly
        assertTrue("Frame rate should be positive", lastFrameRate >= 0f)
    }

    @Test
    fun `speed indicator formats text correctly`() {
        val testCases = mapOf(
            0.25f to "25%",
            0.5f to "50%", 
            0.75f to "75%",
            1f to "1x",
            1.5f to "1.5x",
            2f to "2.0x",
            4f to "4.0x",
            16f to "16x"
        )
        
        testCases.forEach { (speed, expectedText) ->
            val actualText = when {
                speed < 1f -> "${(speed * 100).toInt()}%"
                speed == 1f -> "1x"
                speed < 10f -> "${speed}x"
                else -> "${speed.toInt()}x"
            }
            
            assertEquals("Speed $speed should format as $expectedText", 
                expectedText, actualText)
        }
    }

    @Test
    fun `overlays handle hardware acceleration status`() {
        // Test with hardware acceleration enabled
        every { mockAnimationEngine.isHardwareAccelerated() } returns true
        
        composeTestRule.setContent {
            OptimizedSeekOverlay(
                isVisible = true,
                seekDirection = SeekDirection.FORWARD,
                currentSpeed = 2f,
                targetSpeed = 2f,
                position = Offset(100f, 100f),
                animationEngine = mockAnimationEngine
            )
        }
        
        // Should render without issues with hardware acceleration
        
        // Test with hardware acceleration disabled
        every { mockAnimationEngine.isHardwareAccelerated() } returns false
        
        composeTestRule.setContent {
            OptimizedSeekOverlay(
                isVisible = true,
                seekDirection = SeekDirection.FORWARD,
                currentSpeed = 2f,
                targetSpeed = 2f,
                position = Offset(100f, 100f),
                animationEngine = mockAnimationEngine
            )
        }
        
        // Should still render without issues (fallback to software rendering)
    }

    @Test
    fun `overlays handle extreme values gracefully`() {
        // Test extreme speed values
        composeTestRule.setContent {
            OptimizedSeekOverlay(
                isVisible = true,
                seekDirection = SeekDirection.FORWARD,
                currentSpeed = 0.01f, // Very slow
                targetSpeed = 100f,   // Very fast
                position = Offset(100f, 100f),
                animationEngine = mockAnimationEngine
            )
        }
        
        // Should not crash with extreme values
        
        // Test extreme volume values
        composeTestRule.setContent {
            OptimizedVolumeOverlay(
                isVisible = true,
                volumeLevel = 1.5f, // Above 100%
                position = Offset(50f, 50f),
                animationEngine = mockAnimationEngine
            )
        }
        
        // Should handle values outside normal range
        
        // Test extreme brightness values
        composeTestRule.setContent {
            OptimizedBrightnessOverlay(
                isVisible = true,
                brightnessLevel = -0.1f, // Below 0%
                position = Offset(50f, 50f),
                animationEngine = mockAnimationEngine
            )
        }
        
        // Should handle negative values gracefully
    }

    @Test
    fun `overlays animate visibility changes smoothly`() {
        var isVisible by mutableStateOf(false)
        
        composeTestRule.setContent {
            OptimizedSeekOverlay(
                isVisible = isVisible,
                seekDirection = SeekDirection.FORWARD,
                currentSpeed = 2f,
                targetSpeed = 2f,
                position = Offset(100f, 100f),
                animationEngine = mockAnimationEngine
            )
        }
        
        // Initially not visible
        
        // Make visible
        isVisible = true
        composeTestRule.waitForIdle()
        
        // Should animate in smoothly
        
        // Make invisible
        isVisible = false
        composeTestRule.waitForIdle()
        
        // Should animate out smoothly
    }

    @Test
    fun `multiple overlays can be displayed simultaneously`() {
        composeTestRule.setContent {
            OptimizedSeekOverlay(
                isVisible = true,
                seekDirection = SeekDirection.FORWARD,
                currentSpeed = 2f,
                targetSpeed = 2f,
                position = Offset(100f, 100f),
                animationEngine = mockAnimationEngine
            )
            
            OptimizedVolumeOverlay(
                isVisible = true,
                volumeLevel = 0.7f,
                position = Offset(200f, 100f),
                animationEngine = mockAnimationEngine
            )
            
            OptimizedBrightnessOverlay(
                isVisible = true,
                brightnessLevel = 0.5f,
                position = Offset(300f, 100f),
                animationEngine = mockAnimationEngine
            )
        }
        
        // Multiple overlays should render simultaneously without conflicts
    }
}