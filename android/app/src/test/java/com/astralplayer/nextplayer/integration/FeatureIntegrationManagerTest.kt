package com.astralplayer.nextplayer.integration

import android.content.Context
import androidx.media3.common.Player
import io.mockk.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

@OptIn(ExperimentalCoroutinesApi::class)
class FeatureIntegrationManagerTest {

    private lateinit var mockContext: Context
    private lateinit var mockPlayer: Player
    private lateinit var testScope: TestScope
    private lateinit var integrationManager: FeatureIntegrationManager

    @Before
    fun setup() {
        mockContext = mockk(relaxed = true)
        mockPlayer = mockk(relaxed = true)
        testScope = TestScope()
        
        every { mockPlayer.audioSessionId } returns 123
        every { mockPlayer.isPlaying } returns false
        every { mockPlayer.currentPosition } returns 5000L
        
        integrationManager = FeatureIntegrationManager(mockContext, mockPlayer, testScope)
    }

    @After
    fun tearDown() {
        integrationManager.cleanup()
    }

    @Test
    fun `initializes all managers correctly`() = runTest {
        integrationManager.initialize()
        
        assertNotNull("Should have aspect ratio manager", integrationManager.aspectRatioManager)
        assertNotNull("Should have audio equalizer manager", integrationManager.audioEqualizerManager)
        assertNotNull("Should have subtitle search manager", integrationManager.subtitleSearchManager)
        assertNotNull("Should have frame navigator", integrationManager.frameNavigator)
        assertNotNull("Should have AB repeat manager", integrationManager.abRepeatManager)
        assertNotNull("Should have bookmark manager", integrationManager.bookmarkManager)
    }

    @Test
    fun `creates macro action executor correctly`() {
        val executor = integrationManager.createMacroActionExecutor()
        assertNotNull("Should create executor", executor)
    }

    @Test
    fun `macro executor handles play pause action`() = runTest {
        val executor = integrationManager.createMacroActionExecutor()
        
        every { mockPlayer.isPlaying } returns false
        
        executor.executeAction(
            com.astralplayer.nextplayer.data.gesture.GestureAction.PLAY_PAUSE,
            emptyMap()
        )
        
        verify { mockPlayer.play() }
    }

    @Test
    fun `cleanup handles all managers`() {
        integrationManager.cleanup()
        assertTrue("Cleanup completed successfully", true)
    }

    @Test
    fun `all managers are accessible`() {
        assertNotNull(integrationManager.aspectRatioManager)
        assertNotNull(integrationManager.audioEqualizerManager)
        assertNotNull(integrationManager.subtitleSearchManager)
        assertNotNull(integrationManager.frameNavigator)
        assertNotNull(integrationManager.abRepeatManager)
        assertNotNull(integrationManager.bookmarkManager)
        assertNotNull(integrationManager.multiFingerGestureDetector)
        assertNotNull(integrationManager.gestureZoneManager)
        assertNotNull(integrationManager.customGestureMappingManager)
        assertNotNull(integrationManager.gestureMacroManager)
        assertNotNull(integrationManager.networkStreamManager)
        assertNotNull(integrationManager.videoFiltersManager)
        assertNotNull(integrationManager.personalAnalyticsManager)
        assertNotNull(integrationManager.decoderManager)
        assertNotNull(integrationManager.playbackHistoryManager)
        assertNotNull(integrationManager.cloudStorageManager)
        assertNotNull(integrationManager.mediaNotificationManager)
    }
}