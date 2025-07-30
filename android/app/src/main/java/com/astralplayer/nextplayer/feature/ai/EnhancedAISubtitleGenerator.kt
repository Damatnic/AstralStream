package com.astralplayer.nextplayer.feature.ai

import android.content.Context
import android.net.Uri
import android.util.Log
import com.astralplayer.nextplayer.data.AISubtitleGenerator
import com.astralplayer.nextplayer.data.AISubtitleState
import com.astralplayer.nextplayer.data.SubtitleEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import java.io.File

/**
 * Enhanced AI Subtitle Generator using Google AI Studio (Gemini)
 */
class EnhancedAISubtitleGenerator(
    private val context: Context,
    private val googleAIService: GoogleAIStudioService
) : AISubtitleGenerator {
    
    companion object {
        private const val TAG = "EnhancedAISubtitleGen"
    }
    
    private val _subtitleState = MutableStateFlow(AISubtitleState())
    override val subtitleState: StateFlow<AISubtitleState> = _subtitleState.asStateFlow()
    
    override suspend fun generateSubtitles(
        videoUri: Uri,
        targetLanguage: String,
        onProgress: (Float) -> Unit
    ): Result<List<SubtitleEntry>> {
        return try {
            Log.d(TAG, "Starting AI subtitle generation for: $videoUri")
            
            updateState { 
                copy(
                    isGenerating = true, 
                    error = null, 
                    generationProgress = 0f,
                    currentLanguage = targetLanguage
                ) 
            }
            
            // Extract video metadata
            onProgress(0.1f)
            updateState { copy(generationProgress = 0.1f) }
            
            val videoTitle = extractVideoTitle(videoUri)
            val videoDescription = extractVideoDescription(videoUri)
            val videoDuration = extractVideoDuration(videoUri)
            
            Log.d(TAG, "Video metadata - Title: $videoTitle, Duration: ${videoDuration}ms")
            
            onProgress(0.2f)
            updateState { copy(generationProgress = 0.2f) }
            
            // Generate subtitles using Google AI Studio
            val subtitleEntries = mutableListOf<SubtitleEntry>()
            
            var generationError: String? = null
            
            googleAIService.generateSubtitles(
                videoTitle = videoTitle,
                videoDescription = videoDescription,
                language = getLanguageName(targetLanguage),
                duration = videoDuration
            ).collect { result ->
                when (result) {
                    is SubtitleGenerationResult.Progress -> {
                        Log.d(TAG, "AI Progress: ${result.message}")
                        val progress = 0.2f + (0.6f * (subtitleEntries.size / 10f)) // Estimate progress
                        onProgress(progress)
                        updateState { copy(generationProgress = progress) }
                    }
                    is SubtitleGenerationResult.Success -> {
                        Log.d(TAG, "AI subtitle generation completed")
                        
                        // Parse SRT content to SubtitleEntry objects
                        val parsedSubtitles = parseSRTContent(result.subtitleContent, targetLanguage)
                        subtitleEntries.addAll(parsedSubtitles)
                        
                        onProgress(0.9f)
                        updateState { copy(generationProgress = 0.9f) }
                    }
                    is SubtitleGenerationResult.Error -> {
                        Log.e(TAG, "AI subtitle generation error: ${result.message}")
                        generationError = result.message
                        updateState { 
                            copy(
                                isGenerating = false, 
                                error = result.message
                            ) 
                        }
                    }
                }
            }
            
            if (generationError != null) {
                return Result.failure(Exception(generationError))
            }
            
            // Finalize and cache results
            onProgress(1.0f)
            updateState { 
                copy(
                    isGenerating = false, 
                    generationProgress = 1f,
                    generatedSubtitles = subtitleEntries
                ) 
            }
            
            Log.d(TAG, "Generated ${subtitleEntries.size} subtitle entries")
            
            Result.success(subtitleEntries)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error generating subtitles", e)
            updateState { 
                copy(
                    isGenerating = false, 
                    error = e.message ?: "Unknown error"
                ) 
            }
            Result.failure(e)
        }
    }
    
    override suspend fun translateSubtitles(
        subtitles: List<SubtitleEntry>,
        targetLanguage: String
    ): Result<List<SubtitleEntry>> {
        return try {
            updateState { 
                copy(
                    isTranslating = true, 
                    translationProgress = 0f,
                    currentLanguage = targetLanguage
                ) 
            }
            
            // Convert subtitles to SRT format
            val srtContent = convertSubtitlesToSRT(subtitles)
            
            val translatedEntries = mutableListOf<SubtitleEntry>()
            
            var translationError: String? = null
            
            googleAIService.translateSubtitles(
                subtitleContent = srtContent,
                targetLanguage = getLanguageName(targetLanguage)
            ).collect { result ->
                when (result) {
                    is TranslationResult.Progress -> {
                        val progress = translatedEntries.size.toFloat() / subtitles.size
                        updateState { copy(translationProgress = progress) }
                    }
                    is TranslationResult.Success -> {
                        val translatedSubtitles = parseSRTContent(result.translatedContent, targetLanguage)
                        translatedEntries.addAll(translatedSubtitles)
                        
                        updateState { 
                            copy(
                                isTranslating = false,
                                translationProgress = 1f,
                                generatedSubtitles = translatedEntries
                            ) 
                        }
                    }
                    is TranslationResult.Error -> {
                        translationError = result.message
                        updateState { 
                            copy(
                                isTranslating = false,
                                error = result.message
                            ) 
                        }
                    }
                }
            }
            
            if (translationError != null) {
                return Result.failure(Exception(translationError))
            }
            
            Result.success(translatedEntries)
            
        } catch (e: Exception) {
            updateState { 
                copy(
                    isTranslating = false,
                    error = e.message ?: "Translation failed"
                ) 
            }
            Result.failure(e)
        }
    }
    
    override fun isLanguageSupported(language: String): Boolean {
        return getSupportedLanguages().contains(language)
    }
    
    override fun getSupportedLanguages(): List<String> {
        return listOf(
            "en", "es", "fr", "de", "it", "pt", "ru", "ja", "ko", "zh",
            "ar", "hi", "th", "vi", "tr", "pl", "nl", "sv", "da", "no",
            "fi", "he", "cs", "hu", "ro", "sk", "bg", "hr", "sl", "et",
            "lv", "lt", "mt", "ga", "cy", "eu", "ca", "gl", "is"
        )
    }
    
    override fun clearCache() {
        // Clear any cached data if needed
    }
    
    // Helper methods
    
    private fun extractVideoTitle(videoUri: Uri): String {
        return try {
            // Try to get title from URI path
            val lastPathSegment = videoUri.lastPathSegment
            if (!lastPathSegment.isNullOrEmpty()) {
                // Remove file extension
                lastPathSegment.substringBeforeLast('.').replace(Regex("[_-]"), " ")
            } else {
                "Unknown Video"
            }
        } catch (e: Exception) {
            "Unknown Video"
        }
    }
    
    private fun extractVideoDescription(videoUri: Uri): String {
        return try {
            // Generate a basic description based on URI or metadata
            val path = videoUri.path ?: ""
            when {
                path.contains("movie", ignoreCase = true) -> "A movie or film content"
                path.contains("tv", ignoreCase = true) -> "TV show or series content"
                path.contains("music", ignoreCase = true) -> "Music video or audio content"
                path.contains("sport", ignoreCase = true) -> "Sports or athletic content"
                path.contains("news", ignoreCase = true) -> "News or informational content"
                path.contains("doc", ignoreCase = true) -> "Documentary or educational content"
                else -> "General video content"
            }
        } catch (e: Exception) {
            "General video content"
        }
    }
    
    private fun extractVideoDuration(videoUri: Uri): Long {
        return try {
            // Try to extract duration using MediaMetadataRetriever
            val retriever = android.media.MediaMetadataRetriever()
            retriever.setDataSource(context, videoUri)
            val durationStr = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)
            retriever.release()
            durationStr?.toLongOrNull() ?: 300000L // Default 5 minutes
        } catch (e: Exception) {
            300000L // Default 5 minutes if extraction fails
        }
    }
    
    private fun parseSRTContent(srtContent: String, language: String): List<SubtitleEntry> {
        val subtitleEntries = mutableListOf<SubtitleEntry>()
        
        try {
            // Split by double newlines to separate subtitle blocks
            val blocks = srtContent.split(Regex("\\n\\s*\\n")).filter { it.isNotBlank() }
            
            for (block in blocks) {
                val lines = block.trim().split('\n').filter { it.isNotBlank() }
                if (lines.size >= 3) {
                    // Parse timestamp line (format: 00:00:00,000 --> 00:00:03,000)
                    val timestampLine = lines[1]
                    val timestampMatch = Regex("(\\d{2}:\\d{2}:\\d{2},\\d{3})\\s*-->\\s*(\\d{2}:\\d{2}:\\d{2},\\d{3})").find(timestampLine)
                    
                    if (timestampMatch != null) {
                        val startTime = parseTimestamp(timestampMatch.groupValues[1])
                        val endTime = parseTimestamp(timestampMatch.groupValues[2])
                        val text = lines.drop(2).joinToString(" ").trim()
                        
                        if (text.isNotEmpty()) {
                            subtitleEntries.add(
                                SubtitleEntry(
                                    startTime = startTime,
                                    endTime = endTime,
                                    text = text,
                                    language = language,
                                    confidence = 0.9f // AI generated confidence
                                )
                            )
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing SRT content", e)
        }
        
        return subtitleEntries
    }
    
    private fun parseTimestamp(timestamp: String): Long {
        return try {
            // Format: 00:00:00,000
            val parts = timestamp.split(':', ',')
            if (parts.size == 4) {
                val hours = parts[0].toLong()
                val minutes = parts[1].toLong()
                val seconds = parts[2].toLong()
                val milliseconds = parts[3].toLong()
                
                (hours * 3600 + minutes * 60 + seconds) * 1000 + milliseconds
            } else {
                0L
            }
        } catch (e: Exception) {
            0L
        }
    }
    
    private fun convertSubtitlesToSRT(subtitles: List<SubtitleEntry>): String {
        val srtBuilder = StringBuilder()
        
        subtitles.forEachIndexed { index, subtitle ->
            srtBuilder.append(index + 1).append('\n')
            srtBuilder.append(formatTimestamp(subtitle.startTime))
                .append(" --> ")
                .append(formatTimestamp(subtitle.endTime))
                .append('\n')
            srtBuilder.append(subtitle.text).append('\n')
            srtBuilder.append('\n')
        }
        
        return srtBuilder.toString()
    }
    
    private fun formatTimestamp(millis: Long): String {
        val hours = millis / 3600000
        val minutes = (millis % 3600000) / 60000
        val seconds = (millis % 60000) / 1000
        val milliseconds = millis % 1000
        
        return String.format("%02d:%02d:%02d,%03d", hours, minutes, seconds, milliseconds)
    }
    
    private fun getLanguageName(languageCode: String): String {
        return when (languageCode.lowercase()) {
            "en" -> "English"
            "es" -> "Spanish"
            "fr" -> "French"
            "de" -> "German"
            "it" -> "Italian"
            "pt" -> "Portuguese"
            "ru" -> "Russian"
            "ja" -> "Japanese"
            "ko" -> "Korean"
            "zh" -> "Chinese"
            "ar" -> "Arabic"
            "hi" -> "Hindi"
            "th" -> "Thai"
            "vi" -> "Vietnamese"
            "tr" -> "Turkish"
            "pl" -> "Polish"
            "nl" -> "Dutch"
            "sv" -> "Swedish"
            "da" -> "Danish"
            "no" -> "Norwegian"
            else -> "English" // Default
        }
    }
    
    private fun updateState(update: AISubtitleState.() -> AISubtitleState) {
        _subtitleState.value = _subtitleState.value.update()
    }
}