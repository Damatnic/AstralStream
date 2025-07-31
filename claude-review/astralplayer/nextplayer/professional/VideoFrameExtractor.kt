package com.astralplayer.nextplayer.professional

import android.graphics.Bitmap
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@UnstableApi
@Singleton
class VideoFrameExtractor @Inject constructor() {
    
    private var mediaMetadataRetriever: MediaMetadataRetriever? = null
    private var currentMediaItem: MediaItem? = null
    private var videoDuration: Long = 0
    private var videoWidth: Int = 0
    private var videoHeight: Int = 0
    
    suspend fun initialize(mediaItem: MediaItem) {
        withContext(Dispatchers.IO) {
            try {
                release() // Clean up previous resources
                
                currentMediaItem = mediaItem
                mediaMetadataRetriever = MediaMetadataRetriever()
                
                val uri = mediaItem.localConfiguration?.uri
                if (uri != null) {
                    mediaMetadataRetriever?.setDataSource(uri.toString())
                    
                    // Get video metadata
                    videoDuration = mediaMetadataRetriever?.extractMetadata(
                        MediaMetadataRetriever.METADATA_KEY_DURATION
                    )?.toLongOrNull() ?: 0L
                    
                    videoWidth = mediaMetadataRetriever?.extractMetadata(
                        MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH
                    )?.toIntOrNull() ?: 0
                    
                    videoHeight = mediaMetadataRetriever?.extractMetadata(
                        MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT
                    )?.toIntOrNull() ?: 0
                }
                
            } catch (e: Exception) {
                release()
                throw e
            }
        }
    }
    
    suspend fun extractFrameAt(timeUs: Long): ProfessionalVideoToolsEngine.VideoFrame? {
        return withContext(Dispatchers.IO) {
            try {
                val retriever = mediaMetadataRetriever ?: return@withContext null
                
                // Extract frame at specific time
                val bitmap = retriever.getFrameAtTime(
                    timeUs * 1000, // Convert to microseconds
                    MediaMetadataRetriever.OPTION_CLOSEST_SYNC
                )
                
                bitmap?.let {
                    ProfessionalVideoToolsEngine.VideoFrame(
                        bitmap = it,
                        timestamp = timeUs,
                        frameNumber = calculateFrameNumber(timeUs)
                    )
                }
                
            } catch (e: Exception) {
                null
            }
        }
    }
    
    suspend fun extractFrameAtFrameNumber(frameNumber: Long): ProfessionalVideoToolsEngine.VideoFrame? {
        return withContext(Dispatchers.IO) {
            try {
                val frameRate = getFrameRate()
                val timeUs = (frameNumber * 1000000L / frameRate).toLong()
                extractFrameAt(timeUs)
                
            } catch (e: Exception) {
                null
            }
        }
    }
    
    suspend fun extractFramesInRange(
        startTimeUs: Long,
        endTimeUs: Long,
        intervalUs: Long = 1000000L // Default 1 second
    ): List<ProfessionalVideoToolsEngine.VideoFrame> {
        return withContext(Dispatchers.IO) {
            val frames = mutableListOf<ProfessionalVideoToolsEngine.VideoFrame>()
            
            try {
                var currentTime = startTimeUs
                while (currentTime <= endTimeUs) {
                    extractFrameAt(currentTime)?.let { frame ->
                        frames.add(frame)
                    }
                    currentTime += intervalUs
                }
                
            } catch (e: Exception) {
                // Return partial results
            }
            
            frames
        }
    }
    
    suspend fun extractThumbnail(
        timeUs: Long,
        width: Int = 320,
        height: Int = 240
    ): Bitmap? {
        return withContext(Dispatchers.IO) {
            try {
                val retriever = mediaMetadataRetriever ?: return@withContext null
                
                val originalBitmap = retriever.getFrameAtTime(
                    timeUs * 1000,
                    MediaMetadataRetriever.OPTION_CLOSEST_SYNC
                )
                
                originalBitmap?.let { bitmap ->
                    Bitmap.createScaledBitmap(bitmap, width, height, true)
                }
                
            } catch (e: Exception) {
                null
            }
        }
    }
    
    private fun calculateFrameNumber(timeUs: Long): Long {
        val frameRate = getFrameRate()
        return (timeUs * frameRate / 1000000L).toLong()
    }
    
    private fun getFrameRate(): Double {
        return try {
            val frameRateString = mediaMetadataRetriever?.extractMetadata(
                MediaMetadataRetriever.METADATA_KEY_CAPTURE_FRAMERATE
            )
            frameRateString?.toDoubleOrNull() ?: 30.0
        } catch (e: Exception) {
            30.0 // Default frame rate
        }
    }
    
    fun getVideoInfo(): VideoInfo {
        return VideoInfo(
            duration = videoDuration,
            width = videoWidth,
            height = videoHeight,
            frameRate = getFrameRate(),
            totalFrames = calculateTotalFrames()
        )
    }
    
    private fun calculateTotalFrames(): Long {
        val frameRate = getFrameRate()
        val durationSeconds = videoDuration / 1000.0
        return (durationSeconds * frameRate).toLong()
    }
    
    fun isInitialized(): Boolean {
        return mediaMetadataRetriever != null && currentMediaItem != null
    }
    
    fun release() {
        try {
            mediaMetadataRetriever?.release()
        } catch (e: Exception) {
            // Ignore release errors
        }
        mediaMetadataRetriever = null
        currentMediaItem = null
        videoDuration = 0
        videoWidth = 0
        videoHeight = 0
    }
    
    data class VideoInfo(
        val duration: Long,
        val width: Int,
        val height: Int,
        val frameRate: Double,
        val totalFrames: Long
    )
}