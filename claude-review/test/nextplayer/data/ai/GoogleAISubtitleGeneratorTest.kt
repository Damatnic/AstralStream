package com.astralplayer.nextplayer.data.ai

import android.content.Context
import android.net.Uri
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

class GoogleAISubtitleGeneratorTest {

    private lateinit var mockContext: Context
    private lateinit var aiGenerator: GoogleAISubtitleGenerator

    @Before
    fun setup() {
        mockContext = mockk(relaxed = true)
        aiGenerator = GoogleAISubtitleGenerator(mockContext)
    }

    @Test
    fun `initial state is correct`() {
        val state = aiGenerator.state.value
        
        assertFalse("Should not be generating", state.isGenerating)
        assertFalse("Should not be translating", state.isTranslating)
        assertEquals("Progress should be 0", 0f, state.generationProgress, 0.01f)
        assertEquals("Language should be en-US", "en-US", state.currentLanguage)
        assertTrue("Subtitles should be empty", state.generatedSubtitles.isEmpty())
        assertNull("Error should be null", state.error)
    }

    @Test
    fun `generates subtitles successfully`() = runTest {
        val mockUri = mockk<Uri>()
        var progressUpdates = 0
        
        val result = aiGenerator.generateSubtitles(
            videoUri = mockUri,
            language = "en-US"
        ) { progress ->
            progressUpdates++
        }
        
        assertTrue("Should succeed", result.isSuccess)
        assertTrue("Should have progress updates", progressUpdates > 0)
        
        val subtitles = result.getOrThrow()
        assertTrue("Should have subtitles", subtitles.isNotEmpty())
        
        val state = aiGenerator.state.value
        assertFalse("Should not be generating", state.isGenerating)
        assertEquals("Should have generated subtitles", subtitles, state.generatedSubtitles)
    }

    @Test
    fun `returns supported languages`() {
        val languages = aiGenerator.getSupportedLanguages()
        
        assertTrue("Should have languages", languages.isNotEmpty())
        assertTrue("Should contain English", languages.contains("en-US"))
        assertTrue("Should contain Spanish", languages.contains("es-ES"))
        assertTrue("Should contain French", languages.contains("fr-FR"))
    }

    @Test
    fun `subtitle entry has correct properties`() {
        val subtitle = SubtitleEntry(
            startTime = 1000L,
            endTime = 3000L,
            text = "Test subtitle",
            language = "en-US",
            confidence = 0.95f
        )
        
        assertEquals("Start time should match", 1000L, subtitle.startTime)
        assertEquals("End time should match", 3000L, subtitle.endTime)
        assertEquals("Text should match", "Test subtitle", subtitle.text)
        assertEquals("Language should match", "en-US", subtitle.language)
        assertEquals("Confidence should match", 0.95f, subtitle.confidence, 0.01f)
    }
}