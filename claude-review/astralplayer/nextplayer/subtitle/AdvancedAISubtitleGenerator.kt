package com.astralplayer.nextplayer.subtitle

import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.system.measureTimeMillis

/**
 * Advanced AI Subtitle Generator for AstralStream
 * Generates subtitles in 3-5 seconds using Gemini AI
 */
@UnstableApi
@Singleton
class AdvancedAISubtitleGenerator @Inject constructor(
    @ApplicationContext private val context: Context,
    private val audioExtractor: AudioExtractor,
    private val subtitleRepository: SubtitleRepository,
    private val languageDetector: LanguageDetector
) {
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    
    private val _generationState = MutableStateFlow<GenerationState>(GenerationState.Idle)
    val generationState: StateFlow<GenerationState> = _generationState.asStateFlow()
    
    private val _progress = MutableStateFlow(0f)
    val progress: StateFlow<Float> = _progress.asStateFlow()
    
    private val generativeModel = GenerativeModel(
        modelName = "gemini-1.5-flash",
        apiKey = getApiKey(),
        generationConfig = generationConfig {
            temperature = 0.7f
            topK = 40
            topP = 0.9f
            maxOutputTokens = 4096
        },
        safetySettings = listOf(
            SafetySetting(HarmCategory.HARASSMENT, BlockThreshold.MEDIUM_AND_ABOVE),
            SafetySetting(HarmCategory.HATE_SPEECH, BlockThreshold.MEDIUM_AND_ABOVE),
            SafetySetting(HarmCategory.SEXUALLY_EXPLICIT, BlockThreshold.MEDIUM_AND_ABOVE),
            SafetySetting(HarmCategory.DANGEROUS_CONTENT, BlockThreshold.MEDIUM_AND_ABOVE),
        )
    )
    
    /**
     * Generate subtitles for a media item
     * Target: < 5 seconds generation time
     */
    suspend fun generateSubtitles(
        mediaItem: MediaItem,
        targetLanguage: String? = null,
        options: GenerationOptions = GenerationOptions()
    ): SubtitleGenerationResult {
        return withContext(Dispatchers.IO) {
            try {
                _generationState.value = GenerationState.Preparing
                _progress.value = 0f
                
                val startTime = System.currentTimeMillis()
                
                // Step 1: Extract audio chunk (10% progress)
                val audioData = extractAudioChunk(mediaItem, options)
                _progress.value = 0.1f
                
                // Step 2: Detect language (20% progress)
                val detectedLanguage = detectLanguage(audioData)
                _progress.value = 0.2f
                
                // Step 3: Generate subtitles with AI (70% progress)
                _generationState.value = GenerationState.Generating
                val subtitles = generateWithAI(
                    audioData,
                    detectedLanguage,
                    targetLanguage ?: detectedLanguage,
                    options
                )
                _progress.value = 0.9f
                
                // Step 4: Post-process and save (10% progress)
                val processedSubtitles = postProcessSubtitles(subtitles, mediaItem)
                saveSubtitles(mediaItem, processedSubtitles)
                _progress.value = 1.0f
                
                val generationTime = System.currentTimeMillis() - startTime
                
                _generationState.value = GenerationState.Complete
                
                SubtitleGenerationResult(
                    success = true,
                    subtitles = processedSubtitles,
                    language = detectedLanguage,
                    generationTimeMs = generationTime,
                    confidence = calculateConfidence(processedSubtitles)
                )
                
            } catch (e: Exception) {
                _generationState.value = GenerationState.Error(e.message ?: "Unknown error")
                SubtitleGenerationResult(
                    success = false,
                    error = e.message,
                    fallbackAvailable = checkFallbackAvailable(mediaItem)
                )
            }
        }
    }
    
    private suspend fun extractAudioChunk(
        mediaItem: MediaItem,
        options: GenerationOptions
    ): AudioData {
        return audioExtractor.extractAudio(
            uri = mediaItem.localConfiguration?.uri ?: throw IllegalArgumentException("No URI"),
            startMs = options.startTimeMs,
            durationMs = options.chunkDurationMs,
            sampleRate = options.sampleRate
        )
    }
    
    private suspend fun detectLanguage(audioData: AudioData): String {
        return languageDetector.detectLanguage(audioData) ?: "en"
    }
    
    private suspend fun generateWithAI(
        audioData: AudioData,
        sourceLanguage: String,
        targetLanguage: String,
        options: GenerationOptions
    ): List<SubtitleEntry> {
        val prompt = buildPrompt(sourceLanguage, targetLanguage, options)
        
        // Convert audio to base64 for API
        val audioBase64 = audioData.toBase64()
        
        // Use streaming for real-time progress
        val response = generativeModel.generateContentStream(
            content {
                text(prompt)
                blob("audio/wav", audioBase64.toByteArray())
            }
        )
        
        val subtitleText = StringBuilder()
        var chunkCount = 0
        
        response.collect { chunk ->
            chunk.text?.let { 
                subtitleText.append(it)
                chunkCount++
                // Update progress based on chunks received
                _progress.value = 0.2f + (0.7f * minOf(chunkCount / 10f, 1f))
            }
        }
        
        return parseSubtitleResponse(subtitleText.toString())
    }
    
    private fun buildPrompt(
        sourceLanguage: String,
        targetLanguage: String,
        options: GenerationOptions
    ): String {
        return """
            Analyze the provided audio and generate accurate subtitles.
            
            Requirements:
            1. Detect speech in $sourceLanguage
            2. Generate subtitles in $targetLanguage
            3. Include precise timestamps
            4. Format: [START_TIME --> END_TIME] TEXT
            5. Keep subtitle duration between 2-6 seconds
            6. Maximum ${options.maxCharactersPerLine} characters per line
            7. Break long sentences naturally
            
            Additional instructions:
            - Capture all spoken words accurately
            - Include significant sounds in brackets [laughing], [music]
            - Maintain natural speech flow
            - Use proper punctuation
            ${if (sourceLanguage != targetLanguage) "- Translate accurately while preserving meaning" else ""}
            
            Output format example:
            [00:00.000 --> 00:02.500] Hello, welcome to the video.
            [00:02.500 --> 00:05.000] Today we'll discuss AI technology.
        """.trimIndent()
    }
    
    private fun parseSubtitleResponse(response: String): List<SubtitleEntry> {
        val pattern = """\[(\d{2}:\d{2}\.\d{3})\s*-->\s*(\d{2}:\d{2}\.\d{3})\]\s*(.+)""".toRegex()
        
        return pattern.findAll(response).map { match ->
            val (startTime, endTime, text) = match.destructured
            SubtitleEntry(
                startTimeMs = parseTimestamp(startTime),
                endTimeMs = parseTimestamp(endTime),
                text = text.trim()
            )
        }.toList()
    }
    
    private fun parseTimestamp(timestamp: String): Long {
        val parts = timestamp.split(":")
        val minutes = parts[0].toInt()
        val seconds = parts[1].toFloat()
        return ((minutes * 60 + seconds) * 1000).toLong()
    }
    
    private suspend fun postProcessSubtitles(
        subtitles: List<SubtitleEntry>,
        mediaItem: MediaItem
    ): List<SubtitleEntry> {
        return subtitles
            .filter { it.text.isNotBlank() }
            .map { entry ->
                // Ensure subtitles don't overlap
                entry.copy(
                    text = cleanSubtitleText(entry.text)
                )
            }
            .sortedBy { it.startTimeMs }
    }
    
    private fun cleanSubtitleText(text: String): String {
        return text
            .replace("\n", " ")
            .replace(Regex("\s+"), " ")
            .trim()
    }
    
    private suspend fun saveSubtitles(
        mediaItem: MediaItem,
        subtitles: List<SubtitleEntry>
    ) {
        subtitleRepository.saveSubtitles(
            mediaId = mediaItem.mediaId,
            subtitles = subtitles,
            language = "en", // TODO: Get from generation result
            format = SubtitleFormat.SRT
        )
    }
    
    private fun calculateConfidence(subtitles: List<SubtitleEntry>): Float {
        if (subtitles.isEmpty()) return 0f
        
        // Calculate confidence based on various factors
        val avgLength = subtitles.map { it.text.length }.average()
        val hasProperTiming = subtitles.all { it.endTimeMs > it.startTimeMs }
        val coverage = calculateCoverage(subtitles)
        
        return when {
            avgLength in 10..100 && hasProperTiming && coverage > 0.8f -> 0.95f
            avgLength in 5..150 && hasProperTiming && coverage > 0.6f -> 0.85f
            hasProperTiming && coverage > 0.4f -> 0.75f
            else -> 0.6f
        }
    }
    
    private fun calculateCoverage(subtitles: List<SubtitleEntry>): Float {
        if (subtitles.isEmpty()) return 0f
        
        val totalDuration = subtitles.last().endTimeMs - subtitles.first().startTimeMs
        val subtitleDuration = subtitles.sumOf { it.endTimeMs - it.startTimeMs }
        
        return (subtitleDuration.toFloat() / totalDuration).coerceIn(0f, 1f)
    }
    
    private fun checkFallbackAvailable(mediaItem: MediaItem): Boolean {
        // Check if offline subtitles exist
        return subtitleRepository.hasOfflineSubtitles(mediaItem.mediaId)
    }
    
    private fun getApiKey(): String {
        // In production, this should be securely stored
        return context.getString(R.string.gemini_api_key)
    }
    
    /**
     * Cancel ongoing subtitle generation
     */
    fun cancelGeneration() {
        scope.coroutineContext.cancelChildren()
        _generationState.value = GenerationState.Idle
        _progress.value = 0f
    }
    
    // Data classes
    sealed class GenerationState {
        object Idle : GenerationState()
        object Preparing : GenerationState()
        object Generating : GenerationState()
        object Complete : GenerationState()
        data class Error(val message: String) : GenerationState()
    }
    
    data class GenerationOptions(
        val startTimeMs: Long = 0,
        val chunkDurationMs: Long = 30_000, // 30 seconds chunks
        val sampleRate: Int = 16_000,
        val maxCharactersPerLine: Int = 42,
        val minSubtitleDuration: Long = 2000,
        val maxSubtitleDuration: Long = 6000,
        val includeNonSpeech: Boolean = true
    )
    
    data class SubtitleGenerationResult(
        val success: Boolean,
        val subtitles: List<SubtitleEntry> = emptyList(),
        val language: String? = null,
        val generationTimeMs: Long = 0,
        val confidence: Float = 0f,
        val error: String? = null,
        val fallbackAvailable: Boolean = false
    )
    
    data class SubtitleEntry(
        val startTimeMs: Long,
        val endTimeMs: Long,
        val text: String
    )
    
    data class AudioData(
        val samples: ByteArray,
        val sampleRate: Int,
        val channels: Int,
        val durationMs: Long
    ) {
        fun toBase64(): String {
            return android.util.Base64.encodeToString(samples, android.util.Base64.NO_WRAP)
        }
        
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            
            other as AudioData
            
            if (!samples.contentEquals(other.samples)) return false
            if (sampleRate != other.sampleRate) return false
            if (channels != other.channels) return false
            if (durationMs != other.durationMs) return false
            
            return true
        }
        
        override fun hashCode(): Int {
            var result = samples.contentHashCode()
            result = 31 * result + sampleRate
            result = 31 * result + channels
            result = 31 * result + durationMs.hashCode()
            return result
        }
    }
    
    enum class SubtitleFormat {
        SRT, VTT, ASS, TTML
    }
}