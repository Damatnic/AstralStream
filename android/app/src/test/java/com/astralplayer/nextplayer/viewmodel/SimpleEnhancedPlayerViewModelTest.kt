package com.astralplayer.nextplayer.viewmodel

import androidx.media3.exoplayer.ExoPlayer
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

@OptIn(ExperimentalCoroutinesApi::class)
class SimpleEnhancedPlayerViewModelTest {

    private lateinit var mockExoPlayer: ExoPlayer
    private lateinit var viewModel: SimpleEnhancedPlayerViewModel
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        mockExoPlayer = mockk(relaxed = true)
        viewModel = SimpleEnhancedPlayerViewModel()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `test setPlayer updates current player`() = runTest {
        viewModel.setPlayer(mockExoPlayer)
        
        assertEquals(mockExoPlayer, viewModel.getCurrentPlayer())
    }

    @Test
    fun `test getCurrentPlayer returns null initially`() = runTest {
        assertNull(viewModel.getCurrentPlayer())
    }

    @Test
    fun `test onStart with player calls play`() = runTest {
        viewModel.setPlayer(mockExoPlayer)
        every { mockExoPlayer.playWhenReady } returns false
        
        viewModel.onStart()
        
        verify { mockExoPlayer.playWhenReady = true }
    }

    @Test
    fun `test onStart without player does nothing`() = runTest {
        // Should not crash when player is null
        viewModel.onStart()
        
        // No player to verify, just ensure no exception
    }

    @Test
    fun `test onStop with player calls pause`() = runTest {
        viewModel.setPlayer(mockExoPlayer)
        
        viewModel.onStop()
        
        verify { mockExoPlayer.playWhenReady = false }
    }

    @Test
    fun `test onStop without player does nothing`() = runTest {
        // Should not crash when player is null
        viewModel.onStop()
        
        // No player to verify, just ensure no exception
    }

    @Test
    fun `test multiple setPlayer calls replace previous player`() = runTest {
        val firstPlayer = mockk<ExoPlayer>(relaxed = true)
        val secondPlayer = mockk<ExoPlayer>(relaxed = true)
        
        viewModel.setPlayer(firstPlayer)
        assertEquals(firstPlayer, viewModel.getCurrentPlayer())
        
        viewModel.setPlayer(secondPlayer)
        assertEquals(secondPlayer, viewModel.getCurrentPlayer())
        assertNotEquals(firstPlayer, viewModel.getCurrentPlayer())
    }

    @Test
    fun `test onStart after onStop restores playback`() = runTest {
        viewModel.setPlayer(mockExoPlayer)
        every { mockExoPlayer.playWhenReady } returns true
        
        // Stop playback
        viewModel.onStop()
        verify { mockExoPlayer.playWhenReady = false }
        
        // Start playback again
        every { mockExoPlayer.playWhenReady } returns false
        viewModel.onStart()
        verify { mockExoPlayer.playWhenReady = true }
    }

    @Test
    fun `test setPlayer with null player`() = runTest {
        viewModel.setPlayer(mockExoPlayer)
        assertEquals(mockExoPlayer, viewModel.getCurrentPlayer())
        
        viewModel.setPlayer(null)
        assertNull(viewModel.getCurrentPlayer())
    }

    @Test
    fun `test lifecycle methods with null player after setPlayer`() = runTest {
        viewModel.setPlayer(mockExoPlayer)
        viewModel.setPlayer(null)
        
        // Should not crash with null player
        viewModel.onStart()
        viewModel.onStop()
        
        // No verification needed as player is null
    }

    @Test
    fun `test onStart only sets playWhenReady if not already playing`() = runTest {
        viewModel.setPlayer(mockExoPlayer)
        every { mockExoPlayer.playWhenReady } returns true // Already playing
        
        viewModel.onStart()
        
        // Should not set playWhenReady if already true
        verify(exactly = 0) { mockExoPlayer.playWhenReady = true }
    }

    @Test
    fun `test viewModel handles player state changes`() = runTest {
        val player1 = mockk<ExoPlayer>(relaxed = true)
        val player2 = mockk<ExoPlayer>(relaxed = true)
        
        // Set first player and start
        viewModel.setPlayer(player1)
        every { player1.playWhenReady } returns false
        viewModel.onStart()
        verify { player1.playWhenReady = true }
        
        // Switch to second player and start
        viewModel.setPlayer(player2)
        every { player2.playWhenReady } returns false
        viewModel.onStart()
        verify { player2.playWhenReady = true }
        
        // Stop should affect current player (player2)
        viewModel.onStop()
        verify { player2.playWhenReady = false }
        
        // player1 should not be affected by the stop
        verify(exactly = 0) { player1.playWhenReady = false }
    }

    @Test
    fun `test concurrent start and stop operations`() = runTest {
        viewModel.setPlayer(mockExoPlayer)
        every { mockExoPlayer.playWhenReady } returns false
        
        // Rapidly start and stop
        viewModel.onStart()
        viewModel.onStop()
        viewModel.onStart()
        
        verify(exactly = 2) { mockExoPlayer.playWhenReady = true }
        verify(exactly = 1) { mockExoPlayer.playWhenReady = false }
    }
}