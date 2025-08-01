package com.astralplayer.nextplayer.data

import androidx.media3.common.Player
import io.mockk.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

@OptIn(ExperimentalCoroutinesApi::class)
class ABRepeatManagerTest {

    private lateinit var mockPlayer: Player
    private lateinit var testScope: TestScope
    private lateinit var abRepeatManager: ABRepeatManager

    @Before
    fun setup() {
        mockPlayer = mockk(relaxed = true)
        testScope = TestScope()
        abRepeatManager = ABRepeatManager(mockPlayer, testScope)
        
        every { mockPlayer.currentPosition } returns 5000L
        every { mockPlayer.duration } returns 60000L
    }

    @After
    fun tearDown() {
        abRepeatManager.cleanup()
    }

    @Test
    fun `sets A point correctly`() = runTest {
        every { mockPlayer.currentPosition } returns 10000L
        
        abRepeatManager.setAPoint()
        
        val state = abRepeatManager.repeatState.value
        assertTrue(state.hasAPoint)
        assertEquals(10000L, state.pointA)
    }

    @Test
    fun `sets B point correctly when A point exists`() = runTest {
        every { mockPlayer.currentPosition } returns 10000L
        abRepeatManager.setAPoint()
        
        every { mockPlayer.currentPosition } returns 20000L
        abRepeatManager.setBPoint()
        
        val state = abRepeatManager.repeatState.value
        assertTrue(state.hasBPoint)
        assertEquals(20000L, state.pointB)
    }

    @Test
    fun `does not set B point before A point`() = runTest {
        every { mockPlayer.currentPosition } returns 5000L
        abRepeatManager.setBPoint()
        
        val state = abRepeatManager.repeatState.value
        assertFalse(state.hasBPoint)
    }

    @Test
    fun `clears repeat correctly`() = runTest {
        abRepeatManager.setAPoint()
        abRepeatManager.setBPoint()
        abRepeatManager.clearRepeat()
        
        val state = abRepeatManager.repeatState.value
        assertFalse(state.hasAPoint)
        assertFalse(state.hasBPoint)
        assertFalse(state.isActive)
    }

    @Test
    fun `calculates segment duration correctly`() = runTest {
        every { mockPlayer.currentPosition } returns 10000L
        abRepeatManager.setAPoint()
        
        every { mockPlayer.currentPosition } returns 25000L
        abRepeatManager.setBPoint()
        
        assertEquals(15000L, abRepeatManager.getSegmentDuration())
    }

    @Test
    fun `starts repeat when both points are set`() = runTest {
        every { mockPlayer.currentPosition } returns 10000L
        abRepeatManager.setAPoint()
        
        every { mockPlayer.currentPosition } returns 20000L
        abRepeatManager.setBPoint()
        
        val state = abRepeatManager.repeatState.value
        assertTrue(state.isActive)
    }

    @Test
    fun `toggles repeat state correctly`() = runTest {
        every { mockPlayer.currentPosition } returns 10000L
        abRepeatManager.setAPoint()
        
        every { mockPlayer.currentPosition } returns 20000L
        abRepeatManager.setBPoint()
        
        assertTrue(abRepeatManager.repeatState.value.isActive)
        
        abRepeatManager.toggleRepeat()
        assertFalse(abRepeatManager.repeatState.value.isActive)
        
        abRepeatManager.toggleRepeat()
        assertTrue(abRepeatManager.repeatState.value.isActive)
    }
}