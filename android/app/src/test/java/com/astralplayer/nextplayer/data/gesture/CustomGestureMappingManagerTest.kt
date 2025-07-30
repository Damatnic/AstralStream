package com.astralplayer.nextplayer.data.gesture

import android.content.Context
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

class CustomGestureMappingManagerTest {

    private lateinit var mockContext: Context
    private lateinit var gestureMappingManager: CustomGestureMappingManager

    @Before
    fun setup() {
        mockContext = mockk(relaxed = true)
        gestureMappingManager = CustomGestureMappingManager(mockContext)
    }

    @Test
    fun `sets gesture mapping correctly`() = runTest {
        gestureMappingManager.setGestureMapping("single_tap", GestureAction.PLAY_PAUSE)
        
        gestureMappingManager.getGestureMapping("single_tap").collect { action ->
            assertEquals(GestureAction.PLAY_PAUSE, action)
        }
    }

    @Test
    fun `removes gesture mapping correctly`() = runTest {
        gestureMappingManager.setGestureMapping("single_tap", GestureAction.PLAY_PAUSE)
        gestureMappingManager.removeGestureMapping("single_tap")
        
        gestureMappingManager.getGestureMapping("single_tap").collect { action ->
            assertNull(action)
        }
    }

    @Test
    fun `returns available gestures`() {
        val gestures = gestureMappingManager.getAvailableGestures()
        
        assertTrue("Should have gestures", gestures.isNotEmpty())
        assertTrue("Should contain single_tap", gestures.contains("single_tap"))
        assertTrue("Should contain double_tap_left", gestures.contains("double_tap_left"))
    }

    @Test
    fun `returns available actions`() {
        val actions = gestureMappingManager.getAvailableActions()
        
        assertTrue("Should have actions", actions.isNotEmpty())
        assertTrue("Should contain PLAY_PAUSE", actions.contains(GestureAction.PLAY_PAUSE))
        assertTrue("Should contain SEEK_FORWARD", actions.contains(GestureAction.SEEK_FORWARD))
    }

    @Test
    fun `resets to defaults correctly`() = runTest {
        // Set custom mapping
        gestureMappingManager.setGestureMapping("single_tap", GestureAction.SPEED_CONTROL)
        
        // Reset to defaults
        gestureMappingManager.resetToDefaults()
        
        // Check that default mapping is restored
        gestureMappingManager.getGestureMapping("single_tap").collect { action ->
            assertEquals(GestureAction.TOGGLE_CONTROLS, action)
        }
    }

    @Test
    fun `gets all mappings correctly`() = runTest {
        gestureMappingManager.setGestureMapping("single_tap", GestureAction.PLAY_PAUSE)
        gestureMappingManager.setGestureMapping("double_tap_left", GestureAction.SEEK_BACKWARD)
        
        gestureMappingManager.getAllMappings().collect { mappings ->
            assertTrue("Should have mappings", mappings.isNotEmpty())
            assertEquals(GestureAction.PLAY_PAUSE, mappings["single_tap"])
            assertEquals(GestureAction.SEEK_BACKWARD, mappings["double_tap_left"])
        }
    }
}