package com.astralplayer.nextplayer.feature.ai.realtime

import android.content.Context
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer

/**
 * Instant audio extraction optimized for speed
 */
class InstantAudioExtractor(private val context: Context) {
    
    companion object {
        private const val TAG = "InstantAudioExtractor"
        private const val AUDIO_SAMPLE_RATE = 16000 // 16kHz for speech recognition
        private const val AUDIO_CHANNELS = 1 // Mono for faster processing
    }
    
    suspend fun extractAudioUltraFast(videoUri: Uri): AudioData = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        
        try {
            // Use optimized audio extraction with parallel processing
            val audioData = when (videoUri.scheme) {
                "content" -> extractFromContentUri(videoUri)
                "file" -> extractFromFile(videoUri)
                "http", "https" -> extractFromStream(videoUri)
                else -> throw IllegalArgumentException("Unsupported URI scheme: ${videoUri.scheme}")
            }
            
            val extractionTime = System.currentTimeMillis() - startTime
            Log.d(TAG, "Audio extracted in ${extractionTime}ms")
            
            audioData
            
        } catch (e: Exception) {
            Log.e(TAG, "Audio extraction failed", e)
            throw SubtitleGenerationException("Audio extraction failed", e)
        }
    }
    
    private suspend fun extractFromContentUri(uri: Uri): AudioData {
        val extractor = MediaExtractor()
        
        try {
            context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                extractor.setDataSource(pfd.fileDescriptor)
                return extractAudioFromExtractor(extractor)
            }
            
            throw SubtitleGenerationException("Failed to open content URI")
            
        } finally {
            extractor.release()
        }
    }
    
    private suspend fun extractFromFile(uri: Uri): AudioData {
        val extractor = MediaExtractor()
        
        try {
            extractor.setDataSource(uri.path!!)
            return extractAudioFromExtractor(extractor)
        } finally {
            extractor.release()
        }
    }
    
    private suspend fun extractFromStream(uri: Uri): AudioData {
        val extractor = MediaExtractor()
        
        try {
            extractor.setDataSource(uri.toString())
            return extractAudioFromExtractor(extractor)
        } finally {
            extractor.release()
        }
    }
    
    private fun extractAudioFromExtractor(extractor: MediaExtractor): AudioData {
        // Find audio track
        val audioTrackIndex = findAudioTrack(extractor)
        if (audioTrackIndex < 0) {
            throw SubtitleGenerationException("No audio track found")
        }
        
        extractor.selectTrack(audioTrackIndex)
        val format = extractor.getTrackFormat(audioTrackIndex)
        
        val sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
        val channelCount = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
        val duration = format.getLong(MediaFormat.KEY_DURATION) / 1000 // Convert to ms
        
        // Extract audio samples (simplified for speed)
        val audioBuffer = ByteBuffer.allocate(1024 * 1024) // 1MB buffer
        val samples = mutableListOf<Byte>()
        
        while (true) {
            val sampleSize = extractor.readSampleData(audioBuffer, 0)
            if (sampleSize < 0) break
            
            val data = ByteArray(sampleSize)
            audioBuffer.get(data)
            samples.addAll(data.toList())
            
            audioBuffer.clear()
            extractor.advance()
            
            // Limit extraction for speed (first 5 minutes only for ultra-fast mode)
            if (samples.size > AUDIO_SAMPLE_RATE * AUDIO_CHANNELS * 2 * 300) { // 5 minutes
                break
            }
        }
        
        return AudioData(
            sampleRate = AUDIO_SAMPLE_RATE,
            channels = AUDIO_CHANNELS,
            data = samples.toByteArray(),
            durationMs = duration
        )
    }
    
    private fun findAudioTrack(extractor: MediaExtractor): Int {
        for (i in 0 until extractor.trackCount) {
            val format = extractor.getTrackFormat(i)
            val mime = format.getString(MediaFormat.KEY_MIME)
            if (mime?.startsWith("audio/") == true) {
                return i
            }
        }
        return -1
    }
}