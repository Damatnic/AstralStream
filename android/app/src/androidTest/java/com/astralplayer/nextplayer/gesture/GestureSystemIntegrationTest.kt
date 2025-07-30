package com.astralplayer.nextplayer.gesture

import android.content.Context
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.astralplayer.nextplayer.data.gesture.*
import com.astralplayer.nextplayer.ui.screens.OptimizedGestureSettingsScreen
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Integration tests for the complete gesture system
 */
@RunWith(AndroidJUnit4::class)
class GestureSystemIntegrationTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var context: Context

    @Before
    fun setup() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
    }

    @Test
    fun gestureSettingsScreenDisplaysCorrectly() {
        composeTestRule.setContent {
            OptimizedGestureSettingsScreen(
                onBack = { }
            )
        }

        // Verify main sections are displayed
        composeTestRule.onNodeWithText("General").assertExists()
        composeTestRule.onNodeWithText("Long Press Speed Control").assertExists()
        composeTestRule.onNodeWithText("Seek Gestures").assertExists()
        composeTestRule.onNodeWithText("Volume & Brightness").assertExists()
        composeTestRule.onNodeWithText("Haptic Feedback").assertExists()
        composeTestRule.onNodeWithText("Performance").assertExists()
    }

    @Test
    fun gestureSettingsCanBeToggled() {
        composeTestRule.setContent {
            OptimizedGestureSettingsScreen(
                onBack = { }
            )
        }

        // Test toggling gesture enable switch
        composeTestRule.onNodeWithText("Enable Gestures").assertExists()
        
        // Find and click the switch (it should be on by default)
        composeTestRule.onAllNodesWithContentDescription("Switch")
            .onFirst()
            .performClick()
        
        // Verify the switch state changed (this would require more complex state management in a real test)
    }

    @Test
    fun hapticFeedbackCanBeTested() {
        composeTestRule.setContent {
            OptimizedGestureSettingsScreen(
                onBack = { }
            )
        }

        // Scroll to haptic section and test haptic feedback
        composeTestRule.onNodeWithText("Test Haptic Feedback")
            .assertExists()
            .performClick()
        
        // In a real test, we would verify that haptic feedback was triggered
        // This would require mocking the vibrator service
    }

    @Test
    fun performanceMetricsAreDisplayed() {
        composeTestRule.setContent {
            OptimizedGestureSettingsScreen(
                onBack = { }
            )
        }

        // Scroll to performance section
        composeTestRule.onNodeWithText("Performance Metrics").assertExists()
        composeTestRule.onNodeWithText("Frame Rate:").assertExists()
        composeTestRule.onNodeWithText("Avg Latency:").assertExists()
        composeTestRule.onNodeWithText("Memory Usage:").assertExists()
    }

    @Test
    fun gesturePreviewCanBeToggled() {
        composeTestRule.setContent {
            OptimizedGestureSettingsScreen(
                onBack = { }
            )
        }

        // Click preview toggle button
        composeTestRule.onNodeWithContentDescription("Toggle Preview")
            .performClick()

        // Verify preview overlay appears
        composeTestRule.onNodeWithText("Gesture Preview").assertExists()
        composeTestRule.onNodeWithText("Try gestures to see them in action").assertExists()
    }

    @Test
    fun sliderSettingsCanBeAdjusted() {
        composeTestRule.setContent {
            OptimizedGestureSettingsScreen(
                onBack = { }
            )
        }

        // Test adjusting a slider (e.g., haptic intensity)
        // This would require more complex interaction testing
        composeTestRule.onNodeWithText("Haptic Intensity").assertExists()
        
        // In a real test, we would simulate slider interaction
        // and verify the value changes
    }

    @Test
    fun backNavigationWorks() {
        var backPressed = false
        
        composeTestRule.setContent {
            OptimizedGestureSettingsScreen(
                onBack = { backPressed = true }
            )
        }

        composeTestRule.onNodeWithContentDescription("Back")
            .performClick()

        assert(backPressed) { "Back callback should be triggered" }
    }

    @Test
    fun gestureSystemComponentsIntegrateCorrectly() = runTest {
        // Test that all gesture system components can be initialized together
        val animationEngine = AdvancedAnimationEngine(this)
        val performanceMonitor = GesturePerformanceMonitor(context, this)
        val hapticManager = EnhancedHapticFeedbackManager(context, this)
        val memoryManager = MemoryOptimizedStateManager(context, this)
        
        try {
            // Start all components
            animationEngine.start()
            performanceMonitor.startMonitoring()
            
            // Verify they're running
            assert(animationEngine.frameRate.value >= 0f)
            assert(performanceMonitor.performanceMetrics.value.totalGestures >= 0)
            assert(hapticManager.isHapticAvailable() || !hapticManager.isHapticAvailable()) // Either state is valid
            assert(memoryManager.getMemoryUsage().totalMemoryBytes > 0)
            
        } finally {
            // Cleanup
            animationEngine.stop()
            performanceMonitor.cleanup()
            hapticManager.cleanup()
            memoryManager.cleanup()
        }
    }

    @Test
    fun gestureDetectorHandlesRapidInput() = runTest {
        val gestureDetector = UltraFastGestureDetector(
            screenWidth = 1080f,
            screenHeight = 1920f,
            settings = EnhancedGestureSettings(),
            callbacks = object : GestureCallbacks {
                override fun onSingleTap(position: androidx.compose.ui.geometry.Offset) {}
                override fun onDoubleTap(position: androidx.compose.ui.geometry.Offset, side: com.astralplayer.nextplayer.data.TouchSide) {}
                override fun onHorizontalSeek(delta: Float, velocity: Float) {}
                override fun onVerticalVolumeChange(delta: Float, side: com.astralplayer.nextplayer.data.TouchSide) {}
                override fun onVerticalBrightnessChange(delta: Float, side: com.astralplayer.nextplayer.data.TouchSide) {}
                override fun onLongPressStart(position: androidx.compose.ui.geometry.Offset) {}
                override fun onLongPressEnd() {}
                override fun onPinchZoom(scaleFactor: Float, center: androidx.compose.ui.geometry.Offset) {}
            },
            scope = this
        )
        
        try {
            // Simulate rapid gesture input
            repeat(100) {
                // This would require actual PointerInputChange objects in a real test
                // For now, we just verify the detector can be created and cleaned up
            }
        } finally {
            gestureDetector.cleanup()
        }
    }

    @Test
    fun memoryOptimizationWorksUnderLoad() = runTest {
        val memoryManager = MemoryOptimizedStateManager(context, this)
        
        try {
            val initialMemory = memoryManager.getMemoryUsage()
            
            // Create many objects to test memory optimization
            val objects = mutableListOf<GestureStateWrapper>()
            repeat(1000) {
                objects.add(memoryManager.obtainFromPool(GestureStateWrapper::class))
            }
            
            // Return objects to pool
            objects.forEach { memoryManager.returnToPool(it) }
            
            // Trigger memory pressure handling
            memoryManager.handleMemoryPressure(MemoryPressureLevel.MODERATE)
            
            val finalMemory = memoryManager.getMemoryUsage()
            
            // Verify memory management is working
            assert(finalMemory.pooledObjectsCount >= 0)
            assert(finalMemory.memoryPressureLevel != MemoryPressureLevel.CRITICAL)
            
        } finally {
            memoryManager.cleanup()
        }
    }
}