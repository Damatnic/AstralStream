package com.astralstream.nextplayer.ai

import android.content.Context
import com.arthenica.mobileffmpeg.FFmpeg
import com.astralstream.nextplayer.models.SubtitleEntry
import com.astralstream.nextplayer.models.SubtitleLine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SubtitleGenerator @Inject constructor(
    private val context: Context,
    private val speechRecognizer: SpeechRecognitionEngine
) {
    
    suspend fun generateSubtitles(
        videoUri: String,
        videoTitle: String,
        language: String = "en"
    ): SubtitleEntry = withContext(Dispatchers.IO) {
        
        // Extract audio track
        val audioFile = extractAudioTrack(videoUri)
        
        try {
            // Perform speech recognition
            val segments = speechRecognizer.transcribe(
                audioFile = audioFile,
                language = language
            )
            
            // Convert to subtitle format
            val subtitleLines = segments.map { segment ->
                SubtitleLine(
                    startTime = segment.startTime,
                    endTime = segment.endTime,
                    text = segment.text
                )
            }
            
            SubtitleEntry(
                entries = subtitleLines,
                language = language,
                confidence = calculateAverageConfidence(segments)
            )
            
        } finally {
            // Clean up temp audio file
            audioFile.delete()
        }
    }
    
    private suspend fun extractAudioTrack(videoUri: String): File {
        val outputFile = File(context.cacheDir, "temp_audio_${System.currentTimeMillis()}.wav")
        
        val command = "-i \"$videoUri\" -vn -acodec pcm_s16le -ar 16000 -ac 1 \"${outputFile.absolutePath}\""
        
        val rc = FFmpeg.execute(command)
        if (rc != 0) {
            throw Exception("Failed to extract audio: FFmpeg returned $rc")
        }
        
        return outputFile
    }
    
    private fun calculateAverageConfidence(segments: List<TranscriptionSegment>): Float {
        if (segments.isEmpty()) return 0f
        return segments.map { it.confidence }.average().toFloat()
    }
}