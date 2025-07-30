package com.astralplayer.nextplayer.feature.ai

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import android.media.AudioFormat
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
// import com.google.mlkit.nl.languageid.LanguageIdentification // Temporarily disabled
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * Real AI-powered subtitle generator using Speech Recognition
 */
class AISubtitleGenerator @Inject constructor(@ApplicationContext private val context: Context) {
    
    private val speechToTextService = SpeechToTextService(context)
    private val whisperService = WhisperSpeechService(context)
    private val claudeAI = ClaudeAIService(context)
    
    companion object {
        private const val TAG = "AISubtitleGenerator"
        private const val SEGMENT_DURATION_MS = 5000L // 5 seconds per segment
        private const val SAMPLE_RATE = 16000
        private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
    }
    
    private val _generationProgress = MutableStateFlow(0f)
    val generationProgress: StateFlow<Float> = _generationProgress.asStateFlow()
    
    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()
    
    // private val languageIdentifier = LanguageIdentification.getClient() // Temporarily disabled
    private var speechRecognizer: SpeechRecognizer? = null
    
    init {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
        } else {
            Log.e(TAG, "Speech recognition not available on this device")
        }
    }
    
    /**
     * Generate subtitles from video audio track
     */
    suspend fun generateSubtitles(videoUri: Uri): SubtitleGenerationResult = withContext(Dispatchers.IO) {
        _isGenerating.value = true
        _generationProgress.value = 0f
        
        val subtitles = mutableListOf<GeneratedSubtitle>()
        val extractor = MediaExtractor()
        
        try {
            extractor.setDataSource(context, videoUri, null)
            
            // Find audio track
            val audioTrackIndex = findAudioTrack(extractor)
            if (audioTrackIndex < 0) {
                return@withContext SubtitleGenerationResult(
                    subtitles = emptyList(),
                    language = "unknown",
                    error = "No audio track found"
                )
            }
            
            extractor.selectTrack(audioTrackIndex)
            val format = extractor.getTrackFormat(audioTrackIndex)
            val duration = format.getLong(MediaFormat.KEY_DURATION) / 1000 // Convert to ms
            
            // Extract audio segments and transcribe
            val segmentCount = (duration / SEGMENT_DURATION_MS).toInt() + 1
            
            for (i in 0 until segmentCount) {
                val startTime = i * SEGMENT_DURATION_MS
                val endTime = minOf((i + 1) * SEGMENT_DURATION_MS, duration)
                
                // Extract audio segment
                val audioData = extractAudioSegment(extractor, startTime * 1000, endTime * 1000)
                
                if (audioData.isNotEmpty()) {
                    // Transcribe audio segment
                    val transcription = transcribeAudioSegment(audioData, startTime)
                    
                    if (transcription.text.isNotBlank()) {
                        subtitles.add(transcription)
                    }
                }
                
                _generationProgress.value = (i + 1).toFloat() / segmentCount
            }
            
            // Detect language of generated text
            val allText = subtitles.joinToString(" ") { it.text }
            val language = detectLanguage(allText)
            
            extractor.release()
            _isGenerating.value = false
            
            SubtitleGenerationResult(
                subtitles = subtitles,
                language = language,
                error = null
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "Error generating subtitles", e)
            extractor.release()
            _isGenerating.value = false
            
            SubtitleGenerationResult(
                subtitles = emptyList(),
                language = "unknown",
                error = "Failed to generate subtitles: ${e.message}"
            )
        }
    }
    
    /**
     * Find audio track in media file
     */
    private fun findAudioTrack(extractor: MediaExtractor): Int {
        for (i in 0 until extractor.trackCount) {
            val format = extractor.getTrackFormat(i)
            val mime = format.getString(MediaFormat.KEY_MIME) ?: continue
            if (mime.startsWith("audio/")) {
                return i
            }
        }
        return -1
    }
    
    /**
     * Extract audio segment from video
     */
    private fun extractAudioSegment(
        extractor: MediaExtractor,
        startTimeUs: Long,
        endTimeUs: Long
    ): ByteArray {
        val buffer = ByteBuffer.allocate(1024 * 1024) // 1MB buffer
        val audioData = mutableListOf<Byte>()
        
        extractor.seekTo(startTimeUs, MediaExtractor.SEEK_TO_CLOSEST_SYNC)
        
        while (true) {
            val sampleSize = extractor.readSampleData(buffer, 0)
            if (sampleSize < 0) break
            
            val sampleTime = extractor.sampleTime
            if (sampleTime > endTimeUs) break
            
            if (sampleTime >= startTimeUs) {
                val data = ByteArray(sampleSize)
                buffer.get(data)
                audioData.addAll(data.toList())
                buffer.clear()
            }
            
            extractor.advance()
        }
        
        return audioData.toByteArray()
    }
    
    /**
     * Transcribe audio segment using speech recognition
     */
    private suspend fun transcribeAudioSegment(
        audioData: ByteArray,
        startTimeMs: Long
    ): GeneratedSubtitle = withContext(Dispatchers.IO) {
        
        val tempFile = File(context.cacheDir, "temp_audio_${System.currentTimeMillis()}.wav")
        try {
            // Write audio data to WAV file
            writeWavFile(tempFile, audioData)
            
            // Use real Speech-to-Text API
            val transcribedText = performOfflineTranscription(tempFile)
            
            // Enhance transcription with Claude if available
            val enhancedText = try {
                if (AIServicesConfig.CLAUDE_API_KEY != "YOUR_CLAUDE_API_KEY" && transcribedText.isNotBlank()) {
                    claudeAI.enhanceSubtitles(transcribedText, "en-US")
                } else {
                    transcribedText
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to enhance subtitles with Claude", e)
                transcribedText
            }
            
            GeneratedSubtitle(
                startTime = startTimeMs,
                endTime = startTimeMs + SEGMENT_DURATION_MS,
                text = enhancedText,
                confidence = 0.8f
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "Error transcribing audio", e)
            GeneratedSubtitle(
                startTime = startTimeMs,
                endTime = startTimeMs + SEGMENT_DURATION_MS,
                text = "",
                confidence = 0.0f
            )
        } finally {
            tempFile.delete()
        }
    }
    
    /**
     * Write audio data to WAV file
     */
    private fun writeWavFile(file: File, audioData: ByteArray) {
        FileOutputStream(file).use { output ->
            // Write WAV header
            val header = createWavHeader(audioData.size)
            output.write(header)
            // Write audio data
            output.write(audioData)
        }
    }
    
    /**
     * Create WAV file header
     */
    private fun createWavHeader(dataSize: Int): ByteArray {
        val header = ByteArray(44)
        val totalDataLen = dataSize + 36
        val byteRate = SAMPLE_RATE * 2 // 16-bit mono
        
        // RIFF header
        header[0] = 'R'.code.toByte()
        header[1] = 'I'.code.toByte()
        header[2] = 'F'.code.toByte()
        header[3] = 'F'.code.toByte()
        
        // Total file size
        header[4] = (totalDataLen and 0xff).toByte()
        header[5] = ((totalDataLen shr 8) and 0xff).toByte()
        header[6] = ((totalDataLen shr 16) and 0xff).toByte()
        header[7] = ((totalDataLen shr 24) and 0xff).toByte()
        
        // WAVE header
        header[8] = 'W'.code.toByte()
        header[9] = 'A'.code.toByte()
        header[10] = 'V'.code.toByte()
        header[11] = 'E'.code.toByte()
        
        // fmt subchunk
        header[12] = 'f'.code.toByte()
        header[13] = 'm'.code.toByte()
        header[14] = 't'.code.toByte()
        header[15] = ' '.code.toByte()
        
        // Subchunk size (16 for PCM)
        header[16] = 16
        header[17] = 0
        header[18] = 0
        header[19] = 0
        
        // Audio format (1 for PCM)
        header[20] = 1
        header[21] = 0
        
        // Number of channels (1 for mono)
        header[22] = 1
        header[23] = 0
        
        // Sample rate
        header[24] = (SAMPLE_RATE and 0xff).toByte()
        header[25] = ((SAMPLE_RATE shr 8) and 0xff).toByte()
        header[26] = ((SAMPLE_RATE shr 16) and 0xff).toByte()
        header[27] = ((SAMPLE_RATE shr 24) and 0xff).toByte()
        
        // Byte rate
        header[28] = (byteRate and 0xff).toByte()
        header[29] = ((byteRate shr 8) and 0xff).toByte()
        header[30] = ((byteRate shr 16) and 0xff).toByte()
        header[31] = ((byteRate shr 24) and 0xff).toByte()
        
        // Block align
        header[32] = 2
        header[33] = 0
        
        // Bits per sample
        header[34] = 16
        header[35] = 0
        
        // data subchunk
        header[36] = 'd'.code.toByte()
        header[37] = 'a'.code.toByte()
        header[38] = 't'.code.toByte()
        header[39] = 'a'.code.toByte()
        
        // Data size
        header[40] = (dataSize and 0xff).toByte()
        header[41] = ((dataSize shr 8) and 0xff).toByte()
        header[42] = ((dataSize shr 16) and 0xff).toByte()
        header[43] = ((dataSize shr 24) and 0xff).toByte()
        
        return header
    }
    
    /**
     * Perform offline transcription
     * In production, this would use Google Cloud Speech-to-Text or similar
     */
    private suspend fun performOfflineTranscription(audioFile: File): String {
        // Choose speech service based on configuration
        return when (AIServicesConfig.getActiveSpeechService()) {
            AIServicesConfig.SpeechService.GOOGLE -> {
                // Use Google Cloud Speech-to-Text
                if (!speechToTextService.initialize()) {
                    Log.e(TAG, "Failed to initialize Google Speech-to-Text service")
                    return "[Google Speech-to-Text initialization failed]"
                }
                
                val result = speechToTextService.transcribeAudio(audioFile)
                if (result.error == null && result.text.isNotBlank()) {
                    result.text
                } else {
                    Log.e(TAG, "Google transcription error: ${result.error}")
                    "[Transcription failed]"
                }
            }
            
            AIServicesConfig.SpeechService.OPENAI -> {
                // Use OpenAI Whisper
                val result = whisperService.transcribeAudio(audioFile)
                if (result.error == null && result.text.isNotBlank()) {
                    result.text
                } else {
                    Log.e(TAG, "Whisper transcription error: ${result.error}")
                    "[Transcription failed]"
                }
            }
            
            AIServicesConfig.SpeechService.AZURE -> {
                // Azure implementation would go here
                Log.w(TAG, "Azure Speech Service not yet implemented")
                "[Azure Speech Service not implemented]"
            }
            
            AIServicesConfig.SpeechService.NONE -> {
                Log.e(TAG, "No speech service configured. Please set up API keys in AIServicesConfig.kt")
                "[No speech service configured - see AIServicesConfig.kt]"
            }
        }
    }
    
    /**
     * Detect language of text
     */
    private suspend fun detectLanguage(text: String): String {
        // Simplified language detection - return default language
        // TODO: Re-enable ML Kit language detection when dependencies are fixed
        Log.d(TAG, "Language detection temporarily simplified")
        return if (text.isBlank()) "unknown" else "en"
    }
    
    /**
     * Export subtitles to SRT format
     */
    fun exportToSRT(subtitles: List<GeneratedSubtitle>): String {
        return buildString {
            subtitles.forEachIndexed { index, subtitle ->
                append("${index + 1}\n")
                append("${formatTime(subtitle.startTime)} --> ${formatTime(subtitle.endTime)}\n")
                append("${subtitle.text}\n\n")
            }
        }
    }
    
    /**
     * Format time for SRT
     */
    private fun formatTime(timeMs: Long): String {
        val hours = timeMs / 3600000
        val minutes = (timeMs % 3600000) / 60000
        val seconds = (timeMs % 60000) / 1000
        val milliseconds = timeMs % 1000
        
        return String.format("%02d:%02d:%02d,%03d", hours, minutes, seconds, milliseconds)
    }
    
    /**
     * Clean up resources
     */
    fun release() {
        speechRecognizer?.destroy()
        // languageIdentifier.close() // Temporarily disabled
        speechToTextService.release()
        // Whisper service doesn't need explicit release
    }
}

data class SubtitleGenerationResult(
    val subtitles: List<GeneratedSubtitle>,
    val language: String,
    val error: String?
)

data class GeneratedSubtitle(
    val startTime: Long,
    val endTime: Long,
    val text: String,
    val confidence: Float
)