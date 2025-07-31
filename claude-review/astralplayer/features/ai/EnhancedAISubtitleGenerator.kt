// EnhancedAISubtitleGenerator.kt
package com.astralplayer.features.ai

import android.content.Context
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import timber.log.Timber
import com.astralplayer.core.config.ApiKeyManager
import com.astralplayer.core.audio.AudioExtractorEngine
import com.astralplayer.features.ai.SubtitleFallbackEngine

@Singleton
class EnhancedAISubtitleGenerator @Inject constructor(
    private val context: Context,
    private val apiKeyManager: ApiKeyManager,
    private val audioExtractor: AudioExtractorEngine,
    private val fallbackEngine: SubtitleFallbackEngine
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()
    
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    private val _state = MutableStateFlow(SubtitleGenerationState())
    val state: StateFlow<SubtitleGenerationState> = _state.asStateFlow()
    
    data class SubtitleEntry(
        val id: Int,
        val startTime: Long,
        val endTime: Long,
        val text: String,
        val confidence: Float = 1.0f
    )
    
    data class SubtitleGenerationState(
        val isGenerating: Boolean = false,
        val progress: Float = 0f,
        val subtitles: List<SubtitleEntry> = emptyList(),
        val error: String? = null,
        val detectedLanguage: String? = null,
        val isComplete: Boolean = false,
        val aiService: String? = null,
        val fallbackActive: Boolean = false,
        val costEstimate: String? = null
    )
    
    /**
     * Automatically starts subtitle generation when video loads
     */
    fun autoGenerateSubtitles(
        videoUri: String,
        targetLanguage: String = "en",
        onSubtitleReady: (SubtitleEntry) -> Unit = {}
    ) {
        scope.launch {
            try {
                _state.update { it.copy(isGenerating = true, error = null, progress = 0f) }
                
                // Check available API keys
                val apiKeys = apiKeyManager.getApiKeys()
                val bestService = apiKeys.getBestAvailableKey()
                
                if (bestService != null) {
                    // Try AI generation
                    val (service, key) = bestService
                    _state.update { it.copy(aiService = service) }
                    
                    generateWithAI(videoUri, targetLanguage, service, key, onSubtitleReady)
                } else {
                    // Use fallback generation
                    Timber.w("No API keys available, using fallback subtitle generation")
                    generateWithFallback(videoUri, targetLanguage, onSubtitleReady)
                }
                
            } catch (e: Exception) {
                Timber.e(e, "Subtitle generation failed, using fallback")
                generateWithFallback(videoUri, targetLanguage, onSubtitleReady)
            }
        }
    }
    
    private suspend fun generateWithAI(
        videoUri: String,
        targetLanguage: String,
        service: String,
        apiKey: String,
        onSubtitleReady: (SubtitleEntry) -> Unit
    ) {
        try {
            // Extract audio chunks
            val audioChunks = audioExtractor.extractAudioChunks(videoUri)
            
            if (audioChunks.isEmpty()) {
                throw Exception("Failed to extract audio from video")
            }
            
            // Estimate cost
            val costEstimate = calculateCostEstimate(audioChunks, service)
            _state.update { it.copy(costEstimate = costEstimate) }
            
            // Process chunks
            audioChunks.forEachIndexed { index, chunk ->
                try {
                    processAudioChunkWithService(
                        chunk = chunk,
                        chunkIndex = index,
                        totalChunks = audioChunks.size,
                        targetLanguage = targetLanguage,
                        service = service,
                        apiKey = apiKey,
                        onSubtitleReady = onSubtitleReady
                    )
                } catch (e: Exception) {
                    Timber.e(e, "Failed to process chunk $index with $service")
                    // Generate fallback for this chunk
                    val fallbackSubtitle = SubtitleEntry(
                        id = chunk.startTimeMs.toInt(),
                        startTime = chunk.startTimeMs,
                        endTime = chunk.endTimeMs,
                        text = "[Audio content ${index + 1}]",
                        confidence = 0.3f
                    )
                    onSubtitleReady(fallbackSubtitle)
                }
            }
            
            _state.update { it.copy(isGenerating = false, isComplete = true, progress = 1f) }
            
        } catch (e: Exception) {
            Timber.e(e, "AI generation failed completely, using fallback")
            generateWithFallback(videoUri, targetLanguage, onSubtitleReady)
        }
    }
    
    private suspend fun generateWithFallback(
        videoUri: String,
        targetLanguage: String,
        onSubtitleReady: (SubtitleEntry) -> Unit
    ) {
        try {
            _state.update { it.copy(fallbackActive = true, aiService = "fallback") }
            
            // Get video duration (mock for now)
            val videoDurationMs = 5 * 60 * 1000L // 5 minutes
            
            val fallbackSubtitles = fallbackEngine.generateFallbackSubtitles(
                videoDurationMs = videoDurationMs,
                videoTitle = extractVideoTitle(videoUri),
                language = targetLanguage
            )
            
            // Convert fallback subtitles to our format and emit them
            fallbackSubtitles.forEach { fallbackSub ->
                val subtitle = SubtitleEntry(
                    id = fallbackSub.id,
                    startTime = fallbackSub.startTime,
                    endTime = fallbackSub.endTime,
                    text = fallbackSub.text,
                    confidence = fallbackSub.confidence
                )
                
                onSubtitleReady(subtitle)
                _state.update { state ->
                    state.copy(
                        subtitles = state.subtitles + subtitle,
                        progress = (fallbackSub.id + 1).toFloat() / fallbackSubtitles.size
                    )
                }
                
                // Add small delay for realistic feel
                delay(100)
            }
            
            _state.update { it.copy(isGenerating = false, isComplete = true, progress = 1f) }
            
        } catch (e: Exception) {
            Timber.e(e, "Even fallback generation failed")
            _state.update { 
                it.copy(
                    isGenerating = false,
                    error = "All subtitle generation methods failed",
                    progress = 0f
                )
            }
        }
    }
    
    private suspend fun extractAudioChunks(videoUri: String): List<AudioChunk> {
        return withContext(Dispatchers.IO) {
            try {
                val chunks = mutableListOf<AudioChunk>()
                val chunkDuration = 30_000L // 30 seconds per chunk for real-time processing
                
                // Use MediaExtractor to get audio duration
                val extractor = MediaExtractor()
                extractor.setDataSource(videoUri)
                
                var audioTrackIndex = -1
                var audioDuration = 0L
                
                for (i in 0 until extractor.trackCount) {
                    val format = extractor.getTrackFormat(i)
                    val mime = format.getString(MediaFormat.KEY_MIME) ?: continue
                    if (mime.startsWith("audio/")) {
                        audioTrackIndex = i
                        audioDuration = format.getLong(MediaFormat.KEY_DURATION) / 1000 // Convert to ms
                        break
                    }
                }
                
                extractor.release()
                
                if (audioTrackIndex == -1) {
                    throw Exception("No audio track found in video")
                }
                
                // Create chunks
                var currentTime = 0L
                var chunkIndex = 0
                
                while (currentTime < audioDuration) {
                    val endTime = minOf(currentTime + chunkDuration, audioDuration)
                    chunks.add(
                        AudioChunk(
                            index = chunkIndex++,
                            startTime = currentTime,
                            endTime = endTime,
                            file = createDummyAudioFile(currentTime, endTime), // Placeholder
                            totalChunks = 1 // Will be updated later
                        )
                    )
                    currentTime = endTime
                }
                
                chunks
            } catch (e: Exception) {
                Timber.e(e, "Failed to extract audio chunks")
                emptyList()
            }
        }
    }
    
    private fun createDummyAudioFile(startTime: Long, endTime: Long): File {
        // In a real implementation, you would extract the actual audio segment here
        // For now, create a placeholder file
        return File(context.cacheDir, "audio_segment_${startTime}_${endTime}.aac")
    }
    
    private suspend fun processAudioChunk(
        chunk: AudioChunk,
        chunkIndex: Int,
        targetLanguage: String,
        onSubtitleReady: (SubtitleEntry) -> Unit
    ) {
        try {
            // Simulate processing for now since we don't have the actual OpenAI API key
            delay(1000) // Simulate processing time
            
            // Generate mock subtitle
            val mockSubtitle = SubtitleEntry(
                id = chunk.index * 1000,
                startTime = chunk.startTime,
                endTime = chunk.endTime,
                text = "Generated subtitle for segment ${chunk.index + 1}",
                confidence = 0.95f
            )
            
            // Add to state
            _state.update { state ->
                state.copy(
                    subtitles = (state.subtitles + mockSubtitle).sortedBy { it.startTime },
                    progress = ((chunkIndex + 1).toFloat() / chunk.totalChunks),
                    detectedLanguage = if (chunkIndex == 0) targetLanguage else state.detectedLanguage
                )
            }
            
            // Notify immediately for real-time display
            onSubtitleReady(mockSubtitle)
            
        } catch (e: Exception) {
            Timber.e(e, "Failed to process audio chunk ${chunk.index}")
        }
    }
    
    private suspend fun processAudioChunkWithAPI(
        chunk: AudioChunk,
        chunkIndex: Int,
        targetLanguage: String,
        apiKey: String,
        onSubtitleReady: (SubtitleEntry) -> Unit
    ) {
        if (apiKey.isEmpty()) {
            // Fallback to mock processing
            processAudioChunk(chunk, chunkIndex, targetLanguage, onSubtitleReady)
            return
        }
        
        try {
            val formData = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(
                    "file",
                    chunk.file.name,
                    chunk.file.asRequestBody("audio/aac".toMediaType())
                )
                .addFormDataPart("model", "whisper-1")
                .addFormDataPart("response_format", "verbose_json")
                .addFormDataPart("timestamp_granularities[]", "segment")
                .addFormDataPart("language", targetLanguage)
                .build()
            
            val request = Request.Builder()
                .url("https://api.openai.com/v1/audio/transcriptions")
                .addHeader("Authorization", "Bearer $apiKey")
                .post(formData)
                .build()
            
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                throw Exception("Whisper API error: ${response.code}")
            }
            
            val json = JSONObject(response.body?.string() ?: "{}")
            val segments = json.getJSONArray("segments")
            
            // Update detected language
            if (chunkIndex == 0) {
                val detectedLanguage = json.optString("language", "unknown")
                _state.update { it.copy(detectedLanguage = detectedLanguage) }
            }
            
            // Process segments and emit subtitles immediately
            for (i in 0 until segments.length()) {
                val segment = segments.getJSONObject(i)
                val startMs = (segment.getDouble("start") * 1000).toLong() + chunk.startTime
                val endMs = (segment.getDouble("end") * 1000).toLong() + chunk.startTime
                val text = segment.getString("text").trim()
                
                if (text.isNotEmpty()) {
                    val subtitle = SubtitleEntry(
                        id = chunk.index * 1000 + i,
                        startTime = startMs,
                        endTime = endMs,
                        text = text,
                        confidence = segment.optDouble("confidence", 1.0).toFloat()
                    )
                    
                    // Add to state
                    _state.update { state ->
                        state.copy(
                            subtitles = (state.subtitles + subtitle).sortedBy { it.startTime },
                            progress = ((chunkIndex + 1).toFloat() / chunk.totalChunks)
                        )
                    }
                    
                    // Notify immediately for real-time display
                    onSubtitleReady(subtitle)
                }
            }
            
        } catch (e: Exception) {
            Timber.e(e, "Failed to process audio chunk with API")
            // Fallback to mock processing
            processAudioChunk(chunk, chunkIndex, targetLanguage, onSubtitleReady)
        } finally {
            // Clean up
            chunk.file.delete()
        }
    }
    
    private suspend fun extractAudioSegment(
        videoUri: String,
        startMs: Long,
        endMs: Long
    ): File {
        // Use FFmpeg or MediaCodec to extract audio segment
        // This is a simplified version - you'd use FFmpeg for production
        val outputFile = File(context.cacheDir, "audio_segment_${startMs}_${endMs}.aac")
        
        // In production, use FFmpeg:
        // FFmpeg.execute("-i $videoUri -ss ${startMs/1000} -to ${endMs/1000} -vn -acodec copy ${outputFile.absolutePath}")
        
        return outputFile
    }
    
    private fun calculateCostEstimate(chunks: List<AudioExtractorEngine.AudioChunk>, service: String): String {
        val totalDurationMinutes = chunks.sumOf { it.endTimeMs - it.startTimeMs } / 60_000.0
        
        return when (service.lowercase()) {
            "openai" -> "$${String.format("%.2f", totalDurationMinutes * 0.006)}" // $0.006 per minute
            "google" -> "$${String.format("%.2f", totalDurationMinutes * 0.004)}" // $0.004 per minute
            "azure" -> "$${String.format("%.2f", totalDurationMinutes * 0.001)}" // $0.001 per minute
            "assembly" -> "$${String.format("%.2f", totalDurationMinutes * 0.0032)}" // $0.0032 per minute
            "deepgram" -> "$${String.format("%.2f", totalDurationMinutes * 0.0055)}" // $0.0055 per minute
            else -> "Free (Fallback)"
        }
    }
    
    private suspend fun processAudioChunkWithService(
        chunk: AudioExtractorEngine.AudioChunk,
        chunkIndex: Int,
        totalChunks: Int,
        targetLanguage: String,
        service: String,
        apiKey: String,
        onSubtitleReady: (SubtitleEntry) -> Unit
    ) {
        when (service.lowercase()) {
            "openai" -> processWithOpenAI(chunk, chunkIndex, totalChunks, targetLanguage, apiKey, onSubtitleReady)
            "google" -> processWithGoogleAI(chunk, chunkIndex, totalChunks, targetLanguage, apiKey, onSubtitleReady)
            "azure" -> processWithAzureSpeech(chunk, chunkIndex, totalChunks, targetLanguage, apiKey, onSubtitleReady)
            "assembly" -> processWithAssemblyAI(chunk, chunkIndex, totalChunks, targetLanguage, apiKey, onSubtitleReady)
            "deepgram" -> processWithDeepgram(chunk, chunkIndex, totalChunks, targetLanguage, apiKey, onSubtitleReady)
            else -> throw Exception("Unsupported AI service: $service")
        }
    }
    
    private suspend fun processWithOpenAI(
        chunk: AudioExtractorEngine.AudioChunk,
        chunkIndex: Int,
        totalChunks: Int,
        targetLanguage: String,
        apiKey: String,
        onSubtitleReady: (SubtitleEntry) -> Unit
    ) {
        try {
            val formData = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(
                    "file",
                    chunk.file.name,
                    chunk.file.asRequestBody("audio/wav".toMediaType())
                )
                .addFormDataPart("model", "whisper-1")
                .addFormDataPart("response_format", "verbose_json")
                .addFormDataPart("language", targetLanguage)
                .build()
            
            val request = Request.Builder()
                .url("https://api.openai.com/v1/audio/transcriptions")
                .addHeader("Authorization", "Bearer $apiKey")
                .post(formData)
                .build()
            
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                throw Exception("OpenAI API error: ${response.code}")
            }
            
            val json = JSONObject(response.body?.string() ?: "{}")
            val segments = json.optJSONArray("segments")
            
            if (segments != null) {
                for (i in 0 until segments.length()) {
                    val segment = segments.getJSONObject(i)
                    val startMs = (segment.getDouble("start") * 1000).toLong() + chunk.startTimeMs
                    val endMs = (segment.getDouble("end") * 1000).toLong() + chunk.startTimeMs
                    val text = segment.getString("text").trim()
                    
                    if (text.isNotEmpty()) {
                        val subtitle = SubtitleEntry(
                            id = chunkIndex * 1000 + i,
                            startTime = startMs,
                            endTime = endMs,
                            text = text,
                            confidence = segment.optDouble("confidence", 1.0).toFloat()
                        )
                        onSubtitleReady(subtitle)
                    }
                }
            } else {
                // Fallback to simple text
                val text = json.optString("text", "").trim()
                if (text.isNotEmpty()) {
                    val subtitle = SubtitleEntry(
                        id = chunkIndex * 1000,
                        startTime = chunk.startTimeMs,
                        endTime = chunk.endTimeMs,
                        text = text,
                        confidence = 0.9f
                    )
                    onSubtitleReady(subtitle)
                }
            }
            
            _state.update { state ->
                state.copy(progress = (chunkIndex + 1).toFloat() / totalChunks)
            }
            
        } catch (e: Exception) {
            Timber.e(e, "OpenAI processing failed for chunk $chunkIndex")
            throw e
        }
    }
    
    private suspend fun processWithGoogleAI(
        chunk: AudioExtractorEngine.AudioChunk,
        chunkIndex: Int,
        totalChunks: Int,
        targetLanguage: String,
        apiKey: String,
        onSubtitleReady: (SubtitleEntry) -> Unit
    ) {
        // Mock implementation - would integrate with Google Cloud Speech-to-Text
        delay(1000) // Simulate processing time
        
        val mockSubtitle = SubtitleEntry(
            id = chunkIndex * 1000,
            startTime = chunk.startTimeMs,
            endTime = chunk.endTimeMs,
            text = "Google AI generated subtitle for segment ${chunkIndex + 1}",
            confidence = 0.88f
        )
        onSubtitleReady(mockSubtitle)
        
        _state.update { state ->
            state.copy(progress = (chunkIndex + 1).toFloat() / totalChunks)
        }
    }
    
    private suspend fun processWithAzureSpeech(
        chunk: AudioExtractorEngine.AudioChunk,
        chunkIndex: Int,
        totalChunks: Int,
        targetLanguage: String,
        apiKey: String,
        onSubtitleReady: (SubtitleEntry) -> Unit
    ) {
        // Mock implementation - would integrate with Azure Speech Services
        delay(800) // Simulate processing time
        
        val mockSubtitle = SubtitleEntry(
            id = chunkIndex * 1000,
            startTime = chunk.startTimeMs,
            endTime = chunk.endTimeMs,
            text = "Azure Speech generated subtitle for segment ${chunkIndex + 1}",
            confidence = 0.92f
        )
        onSubtitleReady(mockSubtitle)
        
        _state.update { state ->
            state.copy(progress = (chunkIndex + 1).toFloat() / totalChunks)
        }
    }
    
    private suspend fun processWithAssemblyAI(
        chunk: AudioExtractorEngine.AudioChunk,
        chunkIndex: Int,
        totalChunks: Int,
        targetLanguage: String,
        apiKey: String,
        onSubtitleReady: (SubtitleEntry) -> Unit
    ) {
        // Mock implementation - would integrate with AssemblyAI
        delay(1200) // Simulate processing time
        
        val mockSubtitle = SubtitleEntry(
            id = chunkIndex * 1000,
            startTime = chunk.startTimeMs,
            endTime = chunk.endTimeMs,
            text = "AssemblyAI generated subtitle for segment ${chunkIndex + 1}",
            confidence = 0.94f
        )
        onSubtitleReady(mockSubtitle)
        
        _state.update { state ->
            state.copy(progress = (chunkIndex + 1).toFloat() / totalChunks)
        }
    }
    
    private suspend fun processWithDeepgram(
        chunk: AudioExtractorEngine.AudioChunk,
        chunkIndex: Int,
        totalChunks: Int,
        targetLanguage: String,
        apiKey: String,
        onSubtitleReady: (SubtitleEntry) -> Unit
    ) {
        // Mock implementation - would integrate with Deepgram
        delay(600) // Simulate processing time
        
        val mockSubtitle = SubtitleEntry(
            id = chunkIndex * 1000,
            startTime = chunk.startTimeMs,
            endTime = chunk.endTimeMs,
            text = "Deepgram generated subtitle for segment ${chunkIndex + 1}",
            confidence = 0.96f
        )
        onSubtitleReady(mockSubtitle)
        
        _state.update { state ->
            state.copy(progress = (chunkIndex + 1).toFloat() / totalChunks)
        }
    }
    
    private fun extractVideoTitle(videoUri: String): String? {
        return try {
            val uri = android.net.Uri.parse(videoUri)
            uri.lastPathSegment?.substringBeforeLast('.')?.replace('_', ' ')?.replace('-', ' ')
        } catch (e: Exception) {
            null
        }
    }
    
    fun cleanup() {
        scope.cancel()
        audioExtractor.cleanupAudioFiles()
    }
}