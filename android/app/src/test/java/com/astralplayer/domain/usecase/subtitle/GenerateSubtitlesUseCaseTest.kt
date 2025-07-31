package com.astralplayer.domain.usecase.subtitle

import com.astralplayer.domain.model.Subtitle
import com.astralplayer.domain.repository.SubtitleRepository
import com.astralplayer.domain.repository.AIServiceRepository
import com.astralplayer.domain.model.AIProvider
import io.mockk.*
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

@ExperimentalCoroutinesApi
class GenerateSubtitlesUseCaseTest {
    
    @MockK
    private lateinit var subtitleRepository: SubtitleRepository
    
    @MockK
    private lateinit var aiServiceRepository: AIServiceRepository
    
    private lateinit var generateSubtitlesUseCase: GenerateSubtitlesUseCase
    
    @Before
    fun setup() {
        MockKAnnotations.init(this)
        generateSubtitlesUseCase = GenerateSubtitlesUseCase(
            subtitleRepository,
            aiServiceRepository
        )
    }
    
    @Test
    fun `invoke should return cached subtitles when available`() = runTest {
        // Given
        val videoUri = "test.mp4"
        val language = "en"
        val cachedSubtitles = listOf(
            Subtitle(
                id = "1",
                startTime = 0L,
                endTime = 5000L,
                text = "Hello world",
                language = language,
                confidence = 0.95f
            ),
            Subtitle(
                id = "2",
                startTime = 5000L,
                endTime = 10000L,
                text = "This is a test",
                language = language,
                confidence = 0.98f
            )
        )
        
        coEvery { 
            subtitleRepository.getCachedSubtitles(videoUri, language) 
        } returns cachedSubtitles
        
        // When
        val results = generateSubtitlesUseCase(videoUri, language).toList()
        
        // Then
        assertEquals(2, results.size)
        assertTrue(results[0] is Result.Loading)
        assertTrue(results[1] is Result.Success)
        assertEquals(cachedSubtitles, (results[1] as Result.Success).data)
        
        coVerify(exactly = 0) { aiServiceRepository.generateWithBestProvider(any(), any()) }
    }
    
    @Test
    fun `invoke should generate subtitles when cache miss`() = runTest {
        // Given
        val videoUri = "test.mp4"
        val language = "en"
        val generatedSubtitles = listOf(
            Subtitle(
                id = "1",
                startTime = 0L,
                endTime = 3000L,
                text = "Generated subtitle",
                language = language,
                confidence = 0.92f
            )
        )
        
        coEvery { 
            subtitleRepository.getCachedSubtitles(videoUri, language) 
        } returns null
        
        coEvery { 
            aiServiceRepository.generateWithBestProvider(videoUri, language) 
        } returns generatedSubtitles
        
        coEvery { 
            subtitleRepository.cacheSubtitles(any(), any(), any()) 
        } just Runs
        
        // When
        val results = generateSubtitlesUseCase(videoUri, language).toList()
        
        // Then
        assertEquals(2, results.size)
        assertTrue(results[0] is Result.Loading)
        assertTrue(results[1] is Result.Success)
        assertEquals(generatedSubtitles, (results[1] as Result.Success).data)
        
        coVerify { 
            subtitleRepository.cacheSubtitles(videoUri, language, generatedSubtitles) 
        }
    }
    
    @Test
    fun `invoke should use specific provider when specified`() = runTest {
        // Given
        val videoUri = "test.mp4"
        val language = "es"
        val provider = AIProvider.OPENAI
        val generatedSubtitles = listOf(
            Subtitle(
                id = "1",
                startTime = 0L,
                endTime = 4000L,
                text = "Hola mundo",
                language = language,
                confidence = 0.96f
            )
        )
        
        coEvery { 
            subtitleRepository.getCachedSubtitles(videoUri, language) 
        } returns null
        
        coEvery { 
            aiServiceRepository.generateWithProvider(videoUri, language, provider) 
        } returns generatedSubtitles
        
        coEvery { 
            subtitleRepository.cacheSubtitles(any(), any(), any()) 
        } just Runs
        
        // When
        val results = generateSubtitlesUseCase(videoUri, language, provider).toList()
        
        // Then
        assertEquals(2, results.size)
        assertTrue(results[1] is Result.Success)
        assertEquals(generatedSubtitles, (results[1] as Result.Success).data)
        
        coVerify { 
            aiServiceRepository.generateWithProvider(videoUri, language, provider) 
        }
    }
    
    @Test
    fun `invoke should handle generation errors`() = runTest {
        // Given
        val videoUri = "test.mp4"
        val language = "en"
        val exception = RuntimeException("AI service unavailable")
        
        coEvery { 
            subtitleRepository.getCachedSubtitles(videoUri, language) 
        } returns null
        
        coEvery { 
            aiServiceRepository.generateWithBestProvider(videoUri, language) 
        } throws exception
        
        // When
        val results = generateSubtitlesUseCase(videoUri, language).toList()
        
        // Then
        assertEquals(2, results.size)
        assertTrue(results[0] is Result.Loading)
        assertTrue(results[1] is Result.Error)
        assertEquals(exception, (results[1] as Result.Error).exception)
        
        coVerify(exactly = 0) { 
            subtitleRepository.cacheSubtitles(any(), any(), any()) 
        }
    }
    
    @Test
    fun `invoke should handle empty subtitle generation`() = runTest {
        // Given
        val videoUri = "test.mp4"
        val language = "en"
        val emptySubtitles = emptyList<Subtitle>()
        
        coEvery { 
            subtitleRepository.getCachedSubtitles(videoUri, language) 
        } returns null
        
        coEvery { 
            aiServiceRepository.generateWithBestProvider(videoUri, language) 
        } returns emptySubtitles
        
        coEvery { 
            subtitleRepository.cacheSubtitles(any(), any(), any()) 
        } just Runs
        
        // When
        val results = generateSubtitlesUseCase(videoUri, language).toList()
        
        // Then
        assertEquals(2, results.size)
        assertTrue(results[1] is Result.Success)
        assertTrue((results[1] as Result.Success).data.isEmpty())
        
        // Should still cache empty results
        coVerify { 
            subtitleRepository.cacheSubtitles(videoUri, language, emptySubtitles) 
        }
    }
}