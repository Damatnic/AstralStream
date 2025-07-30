package com.astralplayer.nextplayer.ai

import android.content.Context
import android.net.Uri
import androidx.media3.exoplayer.ExoPlayer
import com.astralplayer.nextplayer.ai.audio.AdvancedAudioProcessor
import com.astralplayer.nextplayer.ai.speech.AdvancedSpeechRecognizer
import com.astralplayer.nextplayer.ai.subtitle.SubtitleOptimizer
import com.astralplayer.nextplayer.ai.translation.AdvancedTranslator
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.File

/**
 * Comprehensive AI subtitle service integrating all advanced components
 */
class AISubtitleService(
    private val context: Context
) {
    
    private val audioProcessor = AdvancedAudioProcessor(context)
    private val speechRecognizer = AdvancedSpeechRecognizer(context)
    private val subtitleOptimizer = SubtitleOptimizer()
    private val translator = AdvancedTranslator(context)
    
    private val _serviceStatus = MutableStateFlow(AISubtitleServiceStatus.Idle)
    val serviceStatus: StateFlow<AISubtitleServiceStatus> = _serviceStatus.asStateFlow()
    
    private val _progress = MutableSharedFlow<AISubtitleProgress>()
    val progress: SharedFlow<AISubtitleProgress> = _progress.asSharedFlow()
    
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    /**
     * Generate comprehensive subtitles with all advanced features
     */
    suspend fun generateAdvancedSubtitles(
        videoUri: Uri,
        config: AISubtitleConfig = AISubtitleConfig()
    ): Result<AISubtitleResult> = withContext(Dispatchers.IO) {
        
        try {
            _serviceStatus.value = AISubtitleServiceStatus.Processing
            _progress.emit(AISubtitleProgress.Started(videoUri, config))
            
            // Phase 1: Audio Processing
            _progress.emit(AISubtitleProgress.AudioProcessing())
            val audioData = extractAndProcessAudio(videoUri, config.audioConfig)
            
            // Phase 2: Speech Recognition
            _progress.emit(AISubtitleProgress.SpeechRecognition())
            val transcription = performSpeechRecognition(audioData, config.speechConfig)
            
            // Phase 3: Subtitle Optimization
            _progress.emit(AISubtitleProgress.SubtitleOptimization())
            val optimizedSubtitles = optimizeSubtitles(transcription, audioData, config.optimizationConfig)
            
            // Phase 4: Translation (if requested)
            val finalSubtitles = if (config.translationConfig.enabled) {
                _progress.emit(AISubtitleProgress.Translation(config.translationConfig.targetLanguages))
                translateSubtitles(optimizedSubtitles, config.translationConfig)
            } else {
                mapOf("original" to optimizedSubtitles)
            }
            
            // Phase 5: Quality Analysis
            _progress.emit(AISubtitleProgress.QualityAnalysis())
            val qualityMetrics = analyzeSubtitleQuality(optimizedSubtitles, audioData)
            
            // Phase 6: Export Processing
            _progress.emit(AISubtitleProgress.Export())
            val exportResults = if (config.exportConfig.enabled) {
                exportSubtitles(finalSubtitles, config.exportConfig)
            } else emptyMap()
            
            val result = AISubtitleResult(
                originalSubtitles = optimizedSubtitles,
                translatedSubtitles = finalSubtitles,
                qualityMetrics = qualityMetrics,
                audioAnalysis = extractAudioAnalysis(audioData),
                exportPaths = exportResults,
                processingStats = createProcessingStats(config)
            )
            
            _serviceStatus.value = AISubtitleServiceStatus.Completed
            _progress.emit(AISubtitleProgress.Completed(result))
            
            Result.success(result)
            
        } catch (e: Exception) {
            _serviceStatus.value = AISubtitleServiceStatus.Error(e)
            _progress.emit(AISubtitleProgress.Error(e))
            Result.failure(e)
        }
    }
    
    /**
     * Start real-time subtitle generation
     */
    suspend fun startRealtimeSubtitles(
        player: ExoPlayer,
        config: RealtimeAIConfig = RealtimeAIConfig()
    ): Flow<RealtimeSubtitle> = flow {
        
        _serviceStatus.value = AISubtitleServiceStatus.Realtime
        
        try {
            while (player.isPlaying && currentCoroutineContext().isActive) {
                // Extract current audio chunk
                val audioChunk = extractCurrentAudio(player, config.chunkSize)
                
                if (audioChunk != null) {
                    // Process audio in real-time
                    val processedAudio = audioProcessor.processRealtimeAudio(
                        audioChunk,
                        player.audioFormat?.sampleRate ?: 44100,
                        config.audioConfig
                    )
                    
                    if (processedAudio.speechDetected) {
                        // Perform real-time speech recognition
                        val recognitionResult = speechRecognizer.processRealtimeAudio(
                            processedAudio.processedSamples,
                            config.speechConfig
                        )
                        
                        if (recognitionResult.isNotBlank()) {
                            val currentTime = player.currentPosition
                            
                            // Translate if needed
                            val translatedText = if (config.enableTranslation) {
                                translator.translateRealtime(
                                    recognitionResult,
                                    config.targetLanguage,
                                    config.sourceLanguage
                                )
                            } else recognitionResult
                            
                            // Create real-time subtitle
                            val realtimeSubtitle = RealtimeSubtitle(
                                text = translatedText,
                                originalText = recognitionResult,
                                startTimeMs = currentTime - config.chunkSize,
                                endTimeMs = currentTime,
                                confidence = processedAudio.qualityScore,
                                isFinal = false,
                                language = if (config.enableTranslation) config.targetLanguage else config.sourceLanguage
                            )
                            
                            emit(realtimeSubtitle)
                        }
                    }
                }
                
                delay(config.processingInterval)
            }
            
        } catch (e: Exception) {
            _serviceStatus.value = AISubtitleServiceStatus.Error(e)
            throw e
        } finally {
            _serviceStatus.value = AISubtitleServiceStatus.Idle
        }
    }
    
    /**
     * Analyze existing subtitles and provide improvements
     */
    suspend fun analyzeAndImproveSubtitles(
        subtitles: List<SubtitleSegment>,
        videoUri: Uri? = null,
        config: SubtitleAnalysisConfig = SubtitleAnalysisConfig()
    ): SubtitleAnalysisResult = withContext(Dispatchers.Default) {
        
        // Quality analysis
        val qualityMetrics = calculateDetailedQualityMetrics(subtitles)
        
        // Readability analysis
        val readabilityAnalysis = analyzeReadability(subtitles)
        
        // Timing analysis
        val timingAnalysis = analyzeTimingQuality(subtitles)
        
        // Content analysis
        val contentAnalysis = analyzeContent(subtitles, config.language)
        
        // Generate improvement suggestions
        val suggestions = generateImprovementSuggestions(
            subtitles,
            qualityMetrics,
            readabilityAnalysis,
            timingAnalysis,
            contentAnalysis
        )
        
        // Apply automatic improvements if requested
        val improvedSubtitles = if (config.applyImprovements) {
            applyAutomaticImprovements(subtitles, suggestions)
        } else subtitles
        
        SubtitleAnalysisResult(
            originalSubtitles = subtitles,
            improvedSubtitles = improvedSubtitles,
            qualityMetrics = qualityMetrics,
            readabilityAnalysis = readabilityAnalysis,
            timingAnalysis = timingAnalysis,
            contentAnalysis = contentAnalysis,
            suggestions = suggestions
        )
    }
    
    /**
     * Batch process multiple videos
     */
    suspend fun batchProcessVideos(
        videos: List<Uri>,
        config: BatchProcessingConfig
    ): BatchProcessingResult = withContext(Dispatchers.IO) {
        
        val results = mutableListOf<Pair<Uri, Result<AISubtitleResult>>>()
        val errors = mutableListOf<Pair<Uri, Exception>>()
        
        _progress.emit(AISubtitleProgress.BatchStarted(videos.size))
        
        videos.forEachIndexed { index, videoUri ->
            try {
                _progress.emit(AISubtitleProgress.BatchProgress(index + 1, videos.size, videoUri))
                
                val result = generateAdvancedSubtitles(videoUri, config.subtitleConfig)
                results.add(videoUri to result)
                
                if (result.isFailure) {
                    errors.add(videoUri to (result.exceptionOrNull() as? Exception ?: Exception("Unknown error")))
                }
                
                // Respect processing limits
                if (config.delayBetweenVideos > 0) {
                    delay(config.delayBetweenVideos)
                }
                
            } catch (e: Exception) {
                errors.add(videoUri to e)
                results.add(videoUri to Result.failure(e))
            }
        }
        
        _progress.emit(AISubtitleProgress.BatchCompleted(results.size, errors.size))
        
        BatchProcessingResult(
            processedVideos = results,
            errors = errors,
            successCount = results.count { it.second.isSuccess },
            failureCount = errors.size,
            totalProcessingTime = System.currentTimeMillis() // Placeholder
        )
    }
    
    // Private implementation methods
    private suspend fun extractAndProcessAudio(
        videoUri: Uri,
        config: AudioProcessingConfig
    ): ProcessedAudioData {
        // Extract audio from video
        val rawAudioData = extractAudioFromVideo(videoUri)
        
        // Process with advanced audio processing
        return audioProcessor.processAudio(
            rawAudioData.samples,
            rawAudioData.sampleRate,
            config
        )
    }
    
    private suspend fun performSpeechRecognition(
        audioData: ProcessedAudioData,
        config: SpeechRecognitionConfig
    ): List<TranscriptionSegment> {
        return speechRecognizer.transcribeWithTimestamps(
            audioData = AudioData(
                samples = audioData.samples,
                sampleRate = audioData.sampleRate,
                durationMs = (audioData.samples.size * 1000L) / audioData.sampleRate
            ),
            language = config.language,
            enableSpeakerDiarization = config.enableSpeakerDiarization,
            enhanceAccuracy = config.enhanceAccuracy
        )
    }
    
    private suspend fun optimizeSubtitles(
        transcription: List<TranscriptionSegment>,
        audioData: ProcessedAudioData,
        config: OptimizationConfig
    ): List<SubtitleSegment> {
        return subtitleOptimizer.optimizeTiming(
            transcription = transcription,
            audioData = AudioData(
                samples = audioData.samples,
                sampleRate = audioData.sampleRate,
                durationMs = (audioData.samples.size * 1000L) / audioData.sampleRate
            ),
            minDuration = config.minDuration,
            maxDuration = config.maxDuration,
            maxWordsPerSegment = config.maxWordsPerSegment
        )
    }
    
    private suspend fun translateSubtitles(
        subtitles: List<SubtitleSegment>,
        config: TranslationConfig
    ): Map<String, List<SubtitleSegment>> {
        val results = mutableMapOf<String, List<SubtitleSegment>>()
        
        // Add original
        results["original"] = subtitles
        
        // Translate to each target language
        config.targetLanguages.forEach { targetLanguage ->
            val texts = subtitles.map { it.text }
            val translations = translator.translateBatch(
                texts = texts,
                targetLanguage = targetLanguage,
                sourceLanguage = config.sourceLanguage,
                preserveFormatting = config.preserveFormatting
            )
            
            val translatedSubtitles = subtitles.mapIndexed { index, subtitle ->
                subtitle.copy(
                    text = translations.getOrElse(index) { subtitle.text },
                    language = targetLanguage,
                    isTranslated = true
                )
            }
            
            results[targetLanguage] = translatedSubtitles
        }
        
        return results
    }
    
    private suspend fun analyzeSubtitleQuality(
        subtitles: List<SubtitleSegment>,
        audioData: ProcessedAudioData
    ): QualityMetrics {
        return QualityMetrics(
            overallScore = subtitles.map { it.qualityScore }.average().toFloat(),
            readabilityScore = subtitles.map { it.readabilityScore }.average().toFloat(),
            timingScore = calculateTimingScore(subtitles),
            consistencyScore = calculateConsistencyScore(subtitles),
            completenessScore = calculateCompletenessScore(subtitles, audioData),
            technicalScore = calculateTechnicalScore(subtitles, audioData)
        )
    }
    
    private fun extractAudioAnalysis(audioData: ProcessedAudioData): AudioAnalysis {
        return AudioAnalysis(
            duration = (audioData.samples.size * 1000L) / audioData.sampleRate,
            sampleRate = audioData.sampleRate,
            channels = 1, // Assuming mono for subtitle processing
            qualityScore = audioData.features.qualityScore,
            speechRatio = audioData.features.speechProbability,
            noiseLevel = 1.0f - audioData.features.qualityScore,
            dynamicRange = calculateDynamicRange(audioData.samples)
        )
    }
    
    private suspend fun exportSubtitles(
        subtitles: Map<String, List<SubtitleSegment>>,
        config: ExportConfig
    ): Map<String, String> {
        val exportResults = mutableMapOf<String, String>()
        
        subtitles.forEach { (language, segments) ->
            config.formats.forEach { format ->
                val filename = "${config.baseFilename}_${language}.${format.lowercase()}"
                val filepath = File(config.outputDirectory, filename).absolutePath
                
                when (format.uppercase()) {
                    "SRT" -> exportToSRT(segments, filepath)
                    "VTT" -> exportToVTT(segments, filepath)
                    "ASS" -> exportToASS(segments, filepath)
                    "TTML" -> exportToTTML(segments, filepath)
                }
                
                exportResults["${language}_${format}"] = filepath
            }
        }
        
        return exportResults
    }
    
    // Utility methods for analysis and processing
    private fun calculateDetailedQualityMetrics(subtitles: List<SubtitleSegment>): DetailedQualityMetrics {
        return DetailedQualityMetrics(
            averageConfidence = subtitles.map { it.confidence }.average().toFloat(),
            averageReadability = subtitles.map { it.readabilityScore }.average().toFloat(),
            averageQuality = subtitles.map { it.qualityScore }.average().toFloat(),
            segmentCount = subtitles.size,
            totalWords = subtitles.sumOf { countWords(it.text) },
            averageWordsPerSegment = subtitles.map { countWords(it.text) }.average().toFloat(),
            averageDuration = subtitles.map { it.durationMs }.average().toFloat(),
            timingConsistency = calculateTimingConsistency(subtitles)
        )
    }
    
    private fun analyzeReadability(subtitles: List<SubtitleSegment>): ReadabilityAnalysis {
        return ReadabilityAnalysis(
            averageReadingSpeed = calculateAverageReadingSpeed(subtitles),
            complexityScore = calculateTextComplexity(subtitles),
            lengthDistribution = analyzeLengthDistribution(subtitles),
            readabilityIssues = identifyReadabilityIssues(subtitles)
        )
    }
    
    private fun analyzeTimingQuality(subtitles: List<SubtitleSegment>): TimingAnalysis {
        return TimingAnalysis(
            averageGap = calculateAverageGap(subtitles),
            gapDistribution = analyzeGapDistribution(subtitles),
            overlapCount = countOverlaps(subtitles),
            timingIssues = identifyTimingIssues(subtitles)
        )
    }
    
    private fun analyzeContent(subtitles: List<SubtitleSegment>, language: String): ContentAnalysis {
        return ContentAnalysis(
            language = language,
            vocabulary = extractVocabulary(subtitles),
            topics = identifyTopics(subtitles),
            sentimentDistribution = analyzeSentiment(subtitles),
            speakerCount = countSpeakers(subtitles)
        )
    }
    
    // Export format implementations
    private suspend fun exportToSRT(subtitles: List<SubtitleSegment>, filepath: String) {
        val srtContent = buildString {
            subtitles.forEachIndexed { index, subtitle ->
                appendLine(index + 1)
                appendLine("${formatSRTTime(subtitle.startTimeMs)} --> ${formatSRTTime(subtitle.endTimeMs)}")
                appendLine(subtitle.text)
                appendLine()
            }
        }
        
        File(filepath).writeText(srtContent)
    }
    
    private suspend fun exportToVTT(subtitles: List<SubtitleSegment>, filepath: String) {
        val vttContent = buildString {
            appendLine("WEBVTT")
            appendLine()
            
            subtitles.forEach { subtitle ->
                appendLine("${formatVTTTime(subtitle.startTimeMs)} --> ${formatVTTTime(subtitle.endTimeMs)}")
                appendLine(subtitle.text)
                appendLine()
            }
        }
        
        File(filepath).writeText(vttContent)
    }
    
    private suspend fun exportToASS(subtitles: List<SubtitleSegment>, filepath: String) {
        // Advanced SubStation Alpha format implementation
        val assContent = buildString {
            appendLine("[Script Info]")
            appendLine("Title: AstralStream Generated Subtitles")
            appendLine("ScriptType: v4.00+")
            appendLine()
            appendLine("[V4+ Styles]")
            appendLine("Format: Name, Fontname, Fontsize, PrimaryColour, SecondaryColour, OutlineColour, BackColour, Bold, Italic, Underline, StrikeOut, ScaleX, ScaleY, Spacing, Angle, BorderStyle, Outline, Shadow, Alignment, MarginL, MarginR, MarginV, Encoding")
            appendLine("Style: Default,Arial,20,&H00FFFFFF,&H000000FF,&H00000000,&H80000000,0,0,0,0,100,100,0,0,1,2,0,2,10,10,10,1")
            appendLine()
            appendLine("[Events]")
            appendLine("Format: Layer, Start, End, Style, Name, MarginL, MarginR, MarginV, Effect, Text")
            
            subtitles.forEach { subtitle ->
                appendLine("Dialogue: 0,${formatASSTime(subtitle.startTimeMs)},${formatASSTime(subtitle.endTimeMs)},Default,,0,0,0,,${subtitle.text}")
            }
        }
        
        File(filepath).writeText(assContent)
    }
    
    private suspend fun exportToTTML(subtitles: List<SubtitleSegment>, filepath: String) {
        // TTML (Timed Text Markup Language) implementation
        val ttmlContent = buildString {
            appendLine("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
            appendLine("<tt xmlns=\"http://www.w3.org/ns/ttml\" xml:lang=\"en\">")
            appendLine("  <head>")
            appendLine("    <styling>")
            appendLine("      <style xml:id=\"defaultStyle\" tts:fontFamily=\"Arial\" tts:fontSize=\"100%\" tts:color=\"white\"/>")
            appendLine("    </styling>")
            appendLine("  </head>")
            appendLine("  <body>")
            appendLine("    <div>")
            
            subtitles.forEach { subtitle ->
                appendLine("      <p begin=\"${formatTTMLTime(subtitle.startTimeMs)}\" end=\"${formatTTMLTime(subtitle.endTimeMs)}\" style=\"defaultStyle\">${subtitle.text}</p>")
            }
            
            appendLine("    </div>")
            appendLine("  </body>")
            appendLine("</tt>")
        }
        
        File(filepath).writeText(ttmlContent)
    }
    
    // Time formatting utilities
    private fun formatSRTTime(timeMs: Long): String {
        val hours = timeMs / 3600000
        val minutes = (timeMs % 3600000) / 60000
        val seconds = (timeMs % 60000) / 1000
        val milliseconds = timeMs % 1000
        return String.format("%02d:%02d:%02d,%03d", hours, minutes, seconds, milliseconds)
    }
    
    private fun formatVTTTime(timeMs: Long): String = formatSRTTime(timeMs).replace(',', '.')
    
    private fun formatASSTime(timeMs: Long): String {
        val hours = timeMs / 3600000
        val minutes = (timeMs % 3600000) / 60000
        val seconds = (timeMs % 60000) / 1000
        val centiseconds = (timeMs % 1000) / 10
        return String.format("%d:%02d:%02d.%02d", hours, minutes, seconds, centiseconds)
    }
    
    private fun formatTTMLTime(timeMs: Long): String {
        val seconds = timeMs / 1000.0
        return String.format("%.3fs", seconds)
    }
    
    // Placeholder implementations for complex analysis functions
    private fun extractAudioFromVideo(videoUri: Uri): AudioData = AudioData(floatArrayOf(), 44100, 0L)
    private fun extractCurrentAudio(player: ExoPlayer, chunkSize: Long): FloatArray? = null
    private fun calculateTimingScore(subtitles: List<SubtitleSegment>): Float = 0.8f
    private fun calculateConsistencyScore(subtitles: List<SubtitleSegment>): Float = 0.8f
    private fun calculateCompletenessScore(subtitles: List<SubtitleSegment>, audioData: ProcessedAudioData): Float = 0.8f
    private fun calculateTechnicalScore(subtitles: List<SubtitleSegment>, audioData: ProcessedAudioData): Float = 0.8f
    private fun calculateDynamicRange(samples: FloatArray): Float = 0.8f
    private fun countWords(text: String): Int = text.split("\\s+".toRegex()).size
    private fun createProcessingStats(config: AISubtitleConfig): ProcessingStats = ProcessingStats()
    private fun calculateTimingConsistency(subtitles: List<SubtitleSegment>): Float = 0.8f
    private fun calculateAverageReadingSpeed(subtitles: List<SubtitleSegment>): Float = 180.0f
    private fun calculateTextComplexity(subtitles: List<SubtitleSegment>): Float = 0.5f
    private fun analyzeLengthDistribution(subtitles: List<SubtitleSegment>): Map<String, Int> = emptyMap()
    private fun identifyReadabilityIssues(subtitles: List<SubtitleSegment>): List<String> = emptyList()
    private fun calculateAverageGap(subtitles: List<SubtitleSegment>): Long = 300L
    private fun analyzeGapDistribution(subtitles: List<SubtitleSegment>): Map<String, Int> = emptyMap()
    private fun countOverlaps(subtitles: List<SubtitleSegment>): Int = 0
    private fun identifyTimingIssues(subtitles: List<SubtitleSegment>): List<String> = emptyList()
    private fun extractVocabulary(subtitles: List<SubtitleSegment>): Set<String> = emptySet()
    private fun identifyTopics(subtitles: List<SubtitleSegment>): List<String> = emptyList()
    private fun analyzeSentiment(subtitles: List<SubtitleSegment>): Map<String, Float> = emptyMap()
    private fun countSpeakers(subtitles: List<SubtitleSegment>): Int = subtitles.mapNotNull { it.speakerId }.distinct().size
    private fun generateImprovementSuggestions(subtitles: List<SubtitleSegment>, quality: DetailedQualityMetrics, readability: ReadabilityAnalysis, timing: TimingAnalysis, content: ContentAnalysis): List<String> = emptyList()
    private fun applyAutomaticImprovements(subtitles: List<SubtitleSegment>, suggestions: List<String>): List<SubtitleSegment> = subtitles
    
    fun cleanup() {
        serviceScope.cancel()
        audioProcessor.cleanup()
        speechRecognizer.cleanup()
        translator.cleanup()
    }
}

// Data classes for comprehensive service
data class AISubtitleConfig(
    val audioConfig: AudioProcessingConfig = AudioProcessingConfig(),
    val speechConfig: SpeechRecognitionConfig = SpeechRecognitionConfig(),
    val optimizationConfig: OptimizationConfig = OptimizationConfig(),
    val translationConfig: TranslationConfig = TranslationConfig(),
    val exportConfig: ExportConfig = ExportConfig()
)

data class AISubtitleResult(
    val originalSubtitles: List<SubtitleSegment>,
    val translatedSubtitles: Map<String, List<SubtitleSegment>>,
    val qualityMetrics: QualityMetrics,
    val audioAnalysis: AudioAnalysis,
    val exportPaths: Map<String, String>,
    val processingStats: ProcessingStats
)

data class RealtimeAIConfig(
    val chunkSize: Long = 1000L,
    val processingInterval: Long = 500L,
    val enableTranslation: Boolean = false,
    val sourceLanguage: String = "auto",
    val targetLanguage: String = "en",
    val audioConfig: com.astralplayer.nextplayer.ai.audio.RealtimeAudioConfig = com.astralplayer.nextplayer.ai.audio.RealtimeAudioConfig(),
    val speechConfig: com.astralplayer.nextplayer.ai.speech.RealtimeRecognitionConfig = com.astralplayer.nextplayer.ai.speech.RealtimeRecognitionConfig()
)

data class RealtimeSubtitle(
    val text: String,
    val originalText: String,
    val startTimeMs: Long,
    val endTimeMs: Long,
    val confidence: Float,
    val isFinal: Boolean,
    val language: String
)

// Additional configuration classes
data class AudioProcessingConfig(
    val enableNoiseReduction: Boolean = true,
    val enableSpeechEnhancement: Boolean = true,
    val normalizeVolume: Boolean = true,
    val enableEchoRemoval: Boolean = true
)

data class SpeechRecognitionConfig(
    val language: String = "auto",
    val enableSpeakerDiarization: Boolean = true,
    val enhanceAccuracy: Boolean = true
)

data class OptimizationConfig(
    val minDuration: Long = 1000L,
    val maxDuration: Long = 6000L,
    val maxWordsPerSegment: Int = 12
)

data class TranslationConfig(
    val enabled: Boolean = false,
    val sourceLanguage: String = "auto",
    val targetLanguages: List<String> = emptyList(),
    val preserveFormatting: Boolean = true
)

data class ExportConfig(
    val enabled: Boolean = true,
    val formats: List<String> = listOf("SRT", "VTT"),
    val outputDirectory: File = File("/tmp"),
    val baseFilename: String = "subtitles"
)

data class BatchProcessingConfig(
    val subtitleConfig: AISubtitleConfig = AISubtitleConfig(),
    val delayBetweenVideos: Long = 1000L,
    val maxConcurrentProcessing: Int = 2
)

data class SubtitleAnalysisConfig(
    val language: String = "en",
    val applyImprovements: Boolean = false
)

// Result classes
data class BatchProcessingResult(
    val processedVideos: List<Pair<Uri, Result<AISubtitleResult>>>,
    val errors: List<Pair<Uri, Exception>>,
    val successCount: Int,
    val failureCount: Int,
    val totalProcessingTime: Long
)

data class SubtitleAnalysisResult(
    val originalSubtitles: List<SubtitleSegment>,
    val improvedSubtitles: List<SubtitleSegment>,
    val qualityMetrics: DetailedQualityMetrics,
    val readabilityAnalysis: ReadabilityAnalysis,
    val timingAnalysis: TimingAnalysis,
    val contentAnalysis: ContentAnalysis,
    val suggestions: List<String>
)

// Analysis result classes
data class QualityMetrics(
    val overallScore: Float,
    val readabilityScore: Float,
    val timingScore: Float,
    val consistencyScore: Float,
    val completenessScore: Float,
    val technicalScore: Float
)

data class DetailedQualityMetrics(
    val averageConfidence: Float,
    val averageReadability: Float,
    val averageQuality: Float,
    val segmentCount: Int,
    val totalWords: Int,
    val averageWordsPerSegment: Float,
    val averageDuration: Float,
    val timingConsistency: Float
)

data class ReadabilityAnalysis(
    val averageReadingSpeed: Float,
    val complexityScore: Float,
    val lengthDistribution: Map<String, Int>,
    val readabilityIssues: List<String>
)

data class TimingAnalysis(
    val averageGap: Long,
    val gapDistribution: Map<String, Int>,
    val overlapCount: Int,
    val timingIssues: List<String>
)

data class ContentAnalysis(
    val language: String,
    val vocabulary: Set<String>,
    val topics: List<String>,
    val sentimentDistribution: Map<String, Float>,
    val speakerCount: Int
)

data class AudioAnalysis(
    val duration: Long,
    val sampleRate: Int,
    val channels: Int,
    val qualityScore: Float,
    val speechRatio: Float,
    val noiseLevel: Float,
    val dynamicRange: Float
)

data class ProcessingStats(
    val totalProcessingTime: Long = 0L,
    val audioProcessingTime: Long = 0L,
    val speechRecognitionTime: Long = 0L,
    val optimizationTime: Long = 0L,
    val translationTime: Long = 0L,
    val exportTime: Long = 0L
)

// Status and progress classes
sealed class AISubtitleServiceStatus {
    object Idle : AISubtitleServiceStatus()
    object Processing : AISubtitleServiceStatus()
    object Realtime : AISubtitleServiceStatus()
    object Completed : AISubtitleServiceStatus()
    data class Error(val exception: Exception) : AISubtitleServiceStatus()
}

sealed class AISubtitleProgress {
    data class Started(val videoUri: Uri, val config: AISubtitleConfig) : AISubtitleProgress()
    object AudioProcessing : AISubtitleProgress()
    object SpeechRecognition : AISubtitleProgress()
    object SubtitleOptimization : AISubtitleProgress()
    data class Translation(val languages: List<String>) : AISubtitleProgress()
    object QualityAnalysis : AISubtitleProgress()
    object Export : AISubtitleProgress()
    data class Completed(val result: AISubtitleResult) : AISubtitleProgress()
    data class Error(val error: Throwable) : AISubtitleProgress()
    data class BatchStarted(val totalVideos: Int) : AISubtitleProgress()
    data class BatchProgress(val current: Int, val total: Int, val currentVideo: Uri) : AISubtitleProgress()
    data class BatchCompleted(val successCount: Int, val errorCount: Int) : AISubtitleProgress()
}