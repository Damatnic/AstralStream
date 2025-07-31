package com.astralplayer.domain.usecase.player

import com.astralplayer.domain.model.VideoMetadata
import com.astralplayer.domain.repository.PlayerRepository
import com.astralplayer.domain.repository.VideoRepository
import io.mockk.*
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

@ExperimentalCoroutinesApi
class PlayVideoUseCaseTest {
    
    @MockK
    private lateinit var videoRepository: VideoRepository
    
    @MockK
    private lateinit var playerRepository: PlayerRepository
    
    private lateinit var playVideoUseCase: PlayVideoUseCase
    
    @Before
    fun setup() {
        MockKAnnotations.init(this)
        playVideoUseCase = PlayVideoUseCase(videoRepository, playerRepository)
    }
    
    @Test
    fun `invoke should emit loading then success when video plays successfully`() = runTest {
        // Given
        val videoUri = "content://test/video.mp4"
        val validatedUri = "validated://test/video.mp4"
        val metadata = VideoMetadata(
            uri = validatedUri,
            title = "Test Video",
            duration = 120000L,
            width = 1920,
            height = 1080,
            frameRate = 30f,
            bitrate = 5000000L,
            codec = "h264",
            hasAudio = true,
            audioChannels = 2
        )
        val playbackInfo = PlaybackInfo(
            uri = validatedUri,
            duration = 120000L,
            currentPosition = 0L,
            isPlaying = true
        )
        
        coEvery { videoRepository.validateUri(videoUri) } returns validatedUri
        coEvery { videoRepository.getVideoMetadata(validatedUri) } returns metadata
        coEvery { playerRepository.initializePlayer(metadata) } just Runs
        coEvery { playerRepository.startPlayback() } returns playbackInfo
        
        // When
        val results = playVideoUseCase(videoUri).toList()
        
        // Then
        assertEquals(2, results.size)
        assertTrue(results[0] is Result.Loading)
        assertTrue(results[1] is Result.Success)
        assertEquals(playbackInfo, (results[1] as Result.Success).data)
        
        coVerifySequence {
            videoRepository.validateUri(videoUri)
            videoRepository.getVideoMetadata(validatedUri)
            playerRepository.initializePlayer(metadata)
            playerRepository.startPlayback()
        }
    }
    
    @Test
    fun `invoke should emit error when video validation fails`() = runTest {
        // Given
        val videoUri = "invalid://uri"
        val exception = IllegalArgumentException("Invalid video URI")
        
        coEvery { videoRepository.validateUri(videoUri) } throws exception
        
        // When
        val results = playVideoUseCase(videoUri).toList()
        
        // Then
        assertEquals(2, results.size)
        assertTrue(results[0] is Result.Loading)
        assertTrue(results[1] is Result.Error)
        assertEquals(exception, (results[1] as Result.Error).exception)
    }
    
    @Test
    fun `invoke should emit error when metadata retrieval fails`() = runTest {
        // Given
        val videoUri = "content://test/video.mp4"
        val validatedUri = "validated://test/video.mp4"
        val exception = RuntimeException("Failed to get metadata")
        
        coEvery { videoRepository.validateUri(videoUri) } returns validatedUri
        coEvery { videoRepository.getVideoMetadata(validatedUri) } throws exception
        
        // When
        val results = playVideoUseCase(videoUri).toList()
        
        // Then
        assertEquals(2, results.size)
        assertTrue(results[0] is Result.Loading)
        assertTrue(results[1] is Result.Error)
        assertEquals(exception, (results[1] as Result.Error).exception)
    }
    
    @Test
    fun `invoke should emit error when player initialization fails`() = runTest {
        // Given
        val videoUri = "content://test/video.mp4"
        val validatedUri = "validated://test/video.mp4"
        val metadata = createTestMetadata(validatedUri)
        val exception = RuntimeException("Player initialization failed")
        
        coEvery { videoRepository.validateUri(videoUri) } returns validatedUri
        coEvery { videoRepository.getVideoMetadata(validatedUri) } returns metadata
        coEvery { playerRepository.initializePlayer(metadata) } throws exception
        
        // When
        val results = playVideoUseCase(videoUri).toList()
        
        // Then
        assertEquals(2, results.size)
        assertTrue(results[0] is Result.Loading)
        assertTrue(results[1] is Result.Error)
        assertEquals(exception, (results[1] as Result.Error).exception)
    }
    
    @Test
    fun `invoke should handle network URIs correctly`() = runTest {
        // Given
        val videoUri = "https://example.com/video.mp4"
        val metadata = createTestMetadata(videoUri)
        val playbackInfo = PlaybackInfo(
            uri = videoUri,
            duration = 180000L,
            currentPosition = 0L,
            isPlaying = true
        )
        
        coEvery { videoRepository.validateUri(videoUri) } returns videoUri
        coEvery { videoRepository.getVideoMetadata(videoUri) } returns metadata
        coEvery { playerRepository.initializePlayer(metadata) } just Runs
        coEvery { playerRepository.startPlayback() } returns playbackInfo
        
        // When
        val results = playVideoUseCase(videoUri).toList()
        
        // Then
        assertEquals(2, results.size)
        assertTrue(results[1] is Result.Success)
        assertEquals(playbackInfo, (results[1] as Result.Success).data)
    }
    
    private fun createTestMetadata(uri: String) = VideoMetadata(
        uri = uri,
        title = "Test Video",
        duration = 120000L,
        width = 1920,
        height = 1080,
        frameRate = 30f,
        bitrate = 5000000L,
        codec = "h264",
        hasAudio = true,
        audioChannels = 2
    )
}