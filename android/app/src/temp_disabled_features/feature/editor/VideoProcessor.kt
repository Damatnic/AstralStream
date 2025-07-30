package com.astralplayer.nextplayer.feature.editor

import android.content.Context
import android.media.*
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.ByteBuffer
import kotlin.math.max
import kotlin.math.min

/**
 * Video processor using Android's MediaCodec API for editing operations
 */
class VideoProcessor(private val context: Context) {
    
    companion object {
        private const val TAG = "VideoProcessor"
        private const val TIMEOUT_US = 10000L
        private const val VIDEO_MIME_TYPE = "video/avc"
        private const val AUDIO_MIME_TYPE = "audio/mp4a-latm"
    }
    
    /**
     * Trim video to specified time range
     */
    suspend fun trimVideo(
        inputUri: Uri,
        outputPath: String,
        startTimeUs: Long,
        endTimeUs: Long,
        onProgress: (Float) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val outputFile = File(outputPath)
            
            // Setup extractor
            val extractor = MediaExtractor()
            extractor.setDataSource(context, inputUri, null)
            
            // Setup muxer
            val muxer = MediaMuxer(outputPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            
            // Process video and audio tracks
            val videoTrackIndex = selectTrack(extractor, "video/")
            val audioTrackIndex = selectTrack(extractor, "audio/")
            
            var muxerVideoTrackIndex = -1
            var muxerAudioTrackIndex = -1
            
            // Add video track
            if (videoTrackIndex >= 0) {
                extractor.selectTrack(videoTrackIndex)
                val videoFormat = extractor.getTrackFormat(videoTrackIndex)
                muxerVideoTrackIndex = muxer.addTrack(videoFormat)
            }
            
            // Add audio track
            if (audioTrackIndex >= 0) {
                extractor.selectTrack(audioTrackIndex)
                val audioFormat = extractor.getTrackFormat(audioTrackIndex)
                muxerAudioTrackIndex = muxer.addTrack(audioFormat)
            }
            
            muxer.start()
            
            // Process video
            if (videoTrackIndex >= 0) {
                extractAndMuxTrack(
                    extractor = extractor,
                    muxer = muxer,
                    trackIndex = videoTrackIndex,
                    muxerTrackIndex = muxerVideoTrackIndex,
                    startTimeUs = startTimeUs,
                    endTimeUs = endTimeUs,
                    onProgress = { progress ->
                        onProgress(progress * 0.5f) // Video is 50% of progress
                    }
                )
            }
            
            // Process audio
            if (audioTrackIndex >= 0) {
                extractor.unselectTrack(videoTrackIndex)
                extractor.selectTrack(audioTrackIndex)
                
                extractAndMuxTrack(
                    extractor = extractor,
                    muxer = muxer,
                    trackIndex = audioTrackIndex,
                    muxerTrackIndex = muxerAudioTrackIndex,
                    startTimeUs = startTimeUs,
                    endTimeUs = endTimeUs,
                    onProgress = { progress ->
                        onProgress(0.5f + progress * 0.5f) // Audio is second 50%
                    }
                )
            }
            
            // Cleanup
            muxer.stop()
            muxer.release()
            extractor.release()
            
            onProgress(1.0f)
            Result.success(outputFile)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error trimming video", e)
            Result.failure(e)
        }
    }
    
    /**
     * Merge multiple video clips
     */
    suspend fun mergeVideos(
        inputUris: List<Uri>,
        outputPath: String,
        onProgress: (Float) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val outputFile = File(outputPath)
            val muxer = MediaMuxer(outputPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            
            var muxerVideoTrackIndex = -1
            var muxerAudioTrackIndex = -1
            var videoFormat: MediaFormat? = null
            var audioFormat: MediaFormat? = null
            
            // Get format from first video
            val firstExtractor = MediaExtractor()
            firstExtractor.setDataSource(context, inputUris.first(), null)
            
            val firstVideoTrack = selectTrack(firstExtractor, "video/")
            if (firstVideoTrack >= 0) {
                videoFormat = firstExtractor.getTrackFormat(firstVideoTrack)
                muxerVideoTrackIndex = muxer.addTrack(videoFormat)
            }
            
            val firstAudioTrack = selectTrack(firstExtractor, "audio/")
            if (firstAudioTrack >= 0) {
                audioFormat = firstExtractor.getTrackFormat(firstAudioTrack)
                muxerAudioTrackIndex = muxer.addTrack(audioFormat)
            }
            
            firstExtractor.release()
            muxer.start()
            
            // Process each video
            var currentTimeUs = 0L
            inputUris.forEachIndexed { index, uri ->
                val extractor = MediaExtractor()
                extractor.setDataSource(context, uri, null)
                
                // Process video track
                val videoTrack = selectTrack(extractor, "video/")
                if (videoTrack >= 0 && muxerVideoTrackIndex >= 0) {
                    extractor.selectTrack(videoTrack)
                    appendTrackToMuxer(
                        extractor = extractor,
                        muxer = muxer,
                        muxerTrackIndex = muxerVideoTrackIndex,
                        timeOffsetUs = currentTimeUs
                    )
                }
                
                // Process audio track
                val audioTrack = selectTrack(extractor, "audio/")
                if (audioTrack >= 0 && muxerAudioTrackIndex >= 0) {
                    extractor.unselectTrack(videoTrack)
                    extractor.selectTrack(audioTrack)
                    appendTrackToMuxer(
                        extractor = extractor,
                        muxer = muxer,
                        muxerTrackIndex = muxerAudioTrackIndex,
                        timeOffsetUs = currentTimeUs
                    )
                }
                
                // Update time offset
                val duration = getVideoDuration(extractor)
                currentTimeUs += duration
                
                extractor.release()
                onProgress((index + 1).toFloat() / inputUris.size)
            }
            
            muxer.stop()
            muxer.release()
            
            Result.success(outputFile)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error merging videos", e)
            Result.failure(e)
        }
    }
    
    /**
     * Apply speed change to video
     */
    suspend fun changeVideoSpeed(
        inputUri: Uri,
        outputPath: String,
        speed: Float,
        onProgress: (Float) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            require(speed > 0 && speed <= 4.0f) { "Speed must be between 0 and 4.0" }
            
            val outputFile = File(outputPath)
            val extractor = MediaExtractor()
            extractor.setDataSource(context, inputUri, null)
            
            val muxer = MediaMuxer(outputPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            
            // Process tracks with speed adjustment
            val videoTrackIndex = selectTrack(extractor, "video/")
            val audioTrackIndex = selectTrack(extractor, "audio/")
            
            var muxerVideoTrackIndex = -1
            var muxerAudioTrackIndex = -1
            
            if (videoTrackIndex >= 0) {
                extractor.selectTrack(videoTrackIndex)
                val videoFormat = extractor.getTrackFormat(videoTrackIndex)
                muxerVideoTrackIndex = muxer.addTrack(videoFormat)
            }
            
            if (audioTrackIndex >= 0) {
                extractor.selectTrack(audioTrackIndex)
                val audioFormat = extractor.getTrackFormat(audioTrackIndex)
                muxerAudioTrackIndex = muxer.addTrack(audioFormat)
            }
            
            muxer.start()
            
            // Process with speed adjustment
            val bufferInfo = MediaCodec.BufferInfo()
            val buffer = ByteBuffer.allocate(1024 * 1024)
            
            while (true) {
                val sampleSize = extractor.readSampleData(buffer, 0)
                if (sampleSize < 0) break
                
                bufferInfo.size = sampleSize
                bufferInfo.presentationTimeUs = (extractor.sampleTime / speed).toLong()
                bufferInfo.flags = extractor.sampleFlags
                
                val trackIndex = extractor.sampleTrackIndex
                val muxerTrackIndex = when (trackIndex) {
                    videoTrackIndex -> muxerVideoTrackIndex
                    audioTrackIndex -> muxerAudioTrackIndex
                    else -> -1
                }
                
                if (muxerTrackIndex >= 0) {
                    muxer.writeSampleData(muxerTrackIndex, buffer, bufferInfo)
                }
                
                extractor.advance()
            }
            
            muxer.stop()
            muxer.release()
            extractor.release()
            
            onProgress(1.0f)
            Result.success(outputFile)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error changing video speed", e)
            Result.failure(e)
        }
    }
    
    private fun selectTrack(extractor: MediaExtractor, mimePrefix: String): Int {
        for (i in 0 until extractor.trackCount) {
            val format = extractor.getTrackFormat(i)
            val mime = format.getString(MediaFormat.KEY_MIME) ?: continue
            if (mime.startsWith(mimePrefix)) {
                return i
            }
        }
        return -1
    }
    
    private fun extractAndMuxTrack(
        extractor: MediaExtractor,
        muxer: MediaMuxer,
        trackIndex: Int,
        muxerTrackIndex: Int,
        startTimeUs: Long,
        endTimeUs: Long,
        onProgress: (Float) -> Unit
    ) {
        extractor.seekTo(startTimeUs, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)
        
        val buffer = ByteBuffer.allocate(1024 * 1024)
        val bufferInfo = MediaCodec.BufferInfo()
        
        var lastProgress = 0f
        val duration = endTimeUs - startTimeUs
        
        while (true) {
            val sampleSize = extractor.readSampleData(buffer, 0)
            if (sampleSize < 0) break
            
            val sampleTime = extractor.sampleTime
            if (sampleTime > endTimeUs) break
            
            if (sampleTime >= startTimeUs) {
                bufferInfo.size = sampleSize
                bufferInfo.presentationTimeUs = sampleTime - startTimeUs
                bufferInfo.flags = extractor.sampleFlags
                
                muxer.writeSampleData(muxerTrackIndex, buffer, bufferInfo)
                
                // Update progress
                val progress = ((sampleTime - startTimeUs).toFloat() / duration).coerceIn(0f, 1f)
                if (progress - lastProgress > 0.01f) {
                    onProgress(progress)
                    lastProgress = progress
                }
            }
            
            extractor.advance()
        }
    }
    
    private fun appendTrackToMuxer(
        extractor: MediaExtractor,
        muxer: MediaMuxer,
        muxerTrackIndex: Int,
        timeOffsetUs: Long
    ) {
        val buffer = ByteBuffer.allocate(1024 * 1024)
        val bufferInfo = MediaCodec.BufferInfo()
        
        extractor.seekTo(0, MediaExtractor.SEEK_TO_CLOSEST_SYNC)
        
        while (true) {
            val sampleSize = extractor.readSampleData(buffer, 0)
            if (sampleSize < 0) break
            
            bufferInfo.size = sampleSize
            bufferInfo.presentationTimeUs = extractor.sampleTime + timeOffsetUs
            bufferInfo.flags = extractor.sampleFlags
            
            muxer.writeSampleData(muxerTrackIndex, buffer, bufferInfo)
            extractor.advance()
        }
    }
    
    private fun getVideoDuration(extractor: MediaExtractor): Long {
        var maxDuration = 0L
        
        for (i in 0 until extractor.trackCount) {
            val format = extractor.getTrackFormat(i)
            if (format.containsKey(MediaFormat.KEY_DURATION)) {
                val duration = format.getLong(MediaFormat.KEY_DURATION)
                maxDuration = max(maxDuration, duration)
            }
        }
        
        return maxDuration
    }
}