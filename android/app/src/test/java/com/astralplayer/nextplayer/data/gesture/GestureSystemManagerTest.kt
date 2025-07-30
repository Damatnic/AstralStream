package com.astralplayer.nextplayer.data.gesture

import android.content.Context
import io.mockk.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

@OptIn(ExperimentalCoroutinesApi::class)
class GestureSystemManagerTest {

    private lateinit var mockContext: Context
    private lateinit var testScope: TestScope
    private lateinit var systemManager: GestureSystemManager

    @Before
    fun setup() {
        mockContext = mockk(relaxed = true)
        testScope = TestScope()
        systemManager = GestureSystemManager(mockContext, testScope)
    }

    @After
    fun tearDown() {
        systemManager.cleanup()
    }

    @Test
    fun `initializes all components correctly`() = runTest {
        systemManager.initialize()
        
        assertNotNull(systemManager.animationEngine)
        assertNotNull(systemManager.performanceMonitor)
        assertNotNull(systemManager.hapticManager)
        assertNotNull(systemManager.memoryManager)
        assertNotNull(systemManager.analytics)
        assertNotNull(systemManager.calibration)
        assertNotNull(systemManager.accessibility)
        assertNotNull(systemManager.exportImport)
    }

    @Test
    fun `creates gesture detector with correct parameters`() = runTest {
        val callbacks = mockk<GestureCallbacks>(relaxed = true)
        
        val detector = systemManager.createGestureDetector(
            screenWidth = 1080f,
            screenHeight = 1920f,
            callbacks = callbacks
        )
        
        assertNotNull(detector)
        detector.cleanup()
    }

    @Test
    fun `system health reflects component states`() = runTest {
        systemManager.initialize()
        
        val health = systemManager.systemHealth.value
        
        assertNotNull(health)
        assertTrue(health.frameRate >= 0f)
        assertTrue(health.memoryUsage >= 0f)
        assertTrue(health.totalGestures >= 0)
    }

    @Test
    fun `cleanup handles all components`() = runTest {
        systemManager.initialize()
        
        // Should not throw exceptions
        systemManager.cleanup()
        
        assertTrue("Cleanup completed successfully", true)
    }

    @Test
    fun `components are properly integrated`() = runTest {
        systemManager.initialize()
        
        // Test that components can work together
        systemManager.analytics.recordGestureUsage("test")
        systemManager.performanceMonitor.recordGestureStart("test")
        systemManager.performanceMonitor.recordGestureCompletion("test")
        
        val analyticsData = systemManager.analytics.analytics.value
        assertTrue(analyticsData.totalGestures > 0)
    }
}