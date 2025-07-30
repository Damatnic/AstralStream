package com.astralplayer.nextplayer.export

import android.content.Context
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.ByteBuffer
import kotlin.math.max

/**
 * Manages video export functionality with trimming, format conversion, and quality adjustment
 */
class VideoExportManager(
    private val context: Context
) {
    private val _exportState = MutableStateFlow<ExportState>(ExportState.Idle)
    val exportState: StateFlow<ExportState> = _exportState.asStateFlow()
    
    private val _exportProgress = MutableStateFlow(0f)
    val exportProgress: StateFlow<Float> = _exportProgress.asStateFlow()
    
    sealed class ExportState {
        object Idle : ExportState()
        object Preparing : ExportState()
        data class Exporting(val message: String) : ExportState()
        data class Completed(val outputPath: String) : ExportState()
        data class Error(val error: String) : ExportState()
    }
    
    data class ExportOptions(
        val sourceUri: Uri,
        val outputPath: String,
        val format: ExportFormat = ExportFormat.MP4,
        val quality: ExportQuality = ExportQuality.HIGH,
        val trimStartMs: Long = 0L,
        val trimEndMs: Long = -1L, // -1 means end of video
        val includeAudio: Boolean = true,
        val targetWidth: Int = -1, // -1 means keep original
        val targetHeight: Int = -1, // -1 means keep original
        val targetBitrate: Int = -1, // -1 means auto
        val targetFrameRate: Int = -1 // -1 means keep original
    )
    
    enum class ExportFormat(val extension: String, val mimeType: String) {
        MP4(".mp4", "video/mp4"),
        WEBM(".webm", "video/webm"),
        MKV(".mkv", "video/x-matroska")
    }
    
    enum class ExportQuality(val bitrateMultiplier: Float) {
        LOW(0.5f),
        MEDIUM(0.75f),
        HIGH(1.0f),
        ULTRA(1.5f),
        ORIGINAL(1.0f)
    }
    
    /**
     * Export video with specified options
     */
    suspend fun exportVideo(options: ExportOptions): Result<String> = withContext(Dispatchers.IO) {
        try {
            _exportState.value = ExportState.Preparing
            _exportProgress.value = 0f
            
            // Validate output path
            val outputFile = File(options.outputPath)
            outputFile.parentFile?.mkdirs()
            
            // Setup extractor
            val extractor = MediaExtractor()
            extractor.setDataSource(context, options.sourceUri, null)
            
            // Find video and audio tracks
            val videoTrackIndex = findTrack(extractor, "video/")
            val audioTrackIndex = if (options.includeAudio) findTrack(extractor, "audio/") else -1
            
            if (videoTrackIndex == -1) {
                throw IllegalArgumentException("No video track found")
            }
            
            // Get video format
            extractor.selectTrack(videoTrackIndex)
            val videoFormat = extractor.getTrackFormat(videoTrackIndex)
            
            // Calculate output parameters
            val outputFormat = createOutputFormat(videoFormat, options)
            
            // Setup muxer
            val muxer = MediaMuxer(
                outputFile.absolutePath,
                when (options.format) {
                    ExportFormat.MP4 -> MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4
                    ExportFormat.WEBM -> MediaMuxer.OutputFormat.MUXER_OUTPUT_WEBM
                    ExportFormat.MKV -> MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4 // Android doesn't support MKV muxing directly
                }
            )
            
            // Add tracks to muxer
            val videoTrackIndexMuxer = muxer.addTrack(outputFormat)
            var audioTrackIndexMuxer = -1
            
            if (audioTrackIndex != -1) {
                extractor.selectTrack(audioTrackIndex)
                val audioFormat = extractor.getTrackFormat(audioTrackIndex)
                audioTrackIndexMuxer = muxer.addTrack(audioFormat)
            }
            
            muxer.start()
            
            // Process video
            _exportState.value = ExportState.Exporting("Processing video...")
            processTrack(
                extractor = extractor,
                muxer = muxer,
                trackIndex = videoTrackIndex,
                muxerTrackIndex = videoTrackIndexMuxer,
                startTimeUs = options.trimStartMs * 1000,
                endTimeUs = if (options.trimEndMs == -1L) Long.MAX_VALUE else options.trimEndMs * 1000
            ) { progress ->
                _exportProgress.value = if (audioTrackIndex == -1) progress else progress * 0.7f
            }
            
            // Process audio if included
            if (audioTrackIndex != -1) {
                _exportState.value = ExportState.Exporting("Processing audio...")
                extractor.unselectTrack(videoTrackIndex)
                extractor.selectTrack(audioTrackIndex)
                
                processTrack(
                    extractor = extractor,
                    muxer = muxer,
                    trackIndex = audioTrackIndex,
                    muxerTrackIndex = audioTrackIndexMuxer,
                    startTimeUs = options.trimStartMs * 1000,
                    endTimeUs = if (options.trimEndMs == -1L) Long.MAX_VALUE else options.trimEndMs * 1000
                ) { progress ->
                    _exportProgress.value = 0.7f + (progress * 0.3f)
                }
            }
            
            // Cleanup
            muxer.stop()
            muxer.release()
            extractor.release()
            
            _exportProgress.value = 1.0f
            _exportState.value = ExportState.Completed(outputFile.absolutePath)
            
            Result.success(outputFile.absolutePath)
        } catch (e: Exception) {
            _exportState.value = ExportState.Error(e.message ?: "Export failed")
            Result.failure(e)
        }
    }
    
    /**
     * Cancel ongoing export
     */
    fun cancelExport() {
        _exportState.value = ExportState.Idle
        _exportProgress.value = 0f
    }
    
    /**
     * Find track index by mime type prefix
     */
    private fun findTrack(extractor: MediaExtractor, mimePrefix: String): Int {
        val trackCount = extractor.trackCount
        for (i in 0 until trackCount) {
            val format = extractor.getTrackFormat(i)
            val mime = format.getString(MediaFormat.KEY_MIME) ?: continue
            if (mime.startsWith(mimePrefix)) {
                return i
            }
        }
        return -1
    }
    
    /**
     * Create output format based on input format and export options
     */
    private fun createOutputFormat(inputFormat: MediaFormat, options: ExportOptions): MediaFormat {
        val mime = inputFormat.getString(MediaFormat.KEY_MIME) ?: "video/avc"
        val width = if (options.targetWidth > 0) {
            options.targetWidth
        } else {
            inputFormat.getInteger(MediaFormat.KEY_WIDTH)
        }
        val height = if (options.targetHeight > 0) {
            options.targetHeight
        } else {
            inputFormat.getInteger(MediaFormat.KEY_HEIGHT)
        }
        
        val outputFormat = MediaFormat.createVideoFormat(mime, width, height)
        
        // Set bitrate
        val originalBitrate = try {
            inputFormat.getInteger(MediaFormat.KEY_BIT_RATE)
        } catch (e: Exception) {
            // Estimate bitrate based on resolution
            width * height * 3 // Rough estimate: 3 bits per pixel
        }
        
        val targetBitrate = if (options.targetBitrate > 0) {
            options.targetBitrate
        } else {
            (originalBitrate * options.quality.bitrateMultiplier).toInt()
        }
        
        outputFormat.setInteger(MediaFormat.KEY_BIT_RATE, targetBitrate)
        
        // Set frame rate
        val frameRate = if (options.targetFrameRate > 0) {
            options.targetFrameRate
        } else {
            try {
                inputFormat.getInteger(MediaFormat.KEY_FRAME_RATE)
            } catch (e: Exception) {
                30 // Default to 30 fps
            }
        }
        outputFormat.setInteger(MediaFormat.KEY_FRAME_RATE, frameRate)
        
        // Set color format
        outputFormat.setInteger(
            MediaFormat.KEY_COLOR_FORMAT,
            MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface
        )
        
        // Set I-frame interval
        outputFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
        
        return outputFormat
    }
    
    /**
     * Process a track from extractor to muxer
     */
    private fun processTrack(
        extractor: MediaExtractor,
        muxer: MediaMuxer,
        trackIndex: Int,
        muxerTrackIndex: Int,
        startTimeUs: Long,
        endTimeUs: Long,
        onProgress: (Float) -> Unit
    ) {
        val bufferSize = 1024 * 1024 // 1MB buffer
        val buffer = ByteBuffer.allocate(bufferSize)
        val bufferInfo = MediaCodec.BufferInfo()
        
        extractor.selectTrack(trackIndex)
        
        // Seek to start time
        extractor.seekTo(startTimeUs, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)
        
        var firstSampleTime = -1L
        val duration = endTimeUs - startTimeUs
        
        while (true) {
            val sampleSize = extractor.readSampleData(buffer, 0)
            if (sampleSize < 0) break
            
            val sampleTime = extractor.sampleTime
            
            // Skip samples before start time
            if (sampleTime < startTimeUs) {
                extractor.advance()
                continue
            }
            
            // Stop at end time
            if (sampleTime >= endTimeUs) break
            
            if (firstSampleTime == -1L) {
                firstSampleTime = sampleTime
            }
            
            // Update buffer info
            bufferInfo.offset = 0
            bufferInfo.size = sampleSize
            bufferInfo.presentationTimeUs = sampleTime - startTimeUs
            bufferInfo.flags = extractor.sampleFlags
            
            // Write to muxer
            muxer.writeSampleData(muxerTrackIndex, buffer, bufferInfo)
            
            // Update progress
            val progress = (sampleTime - startTimeUs).toFloat() / duration
            onProgress(progress.coerceIn(0f, 1f))
            
            extractor.advance()
        }
    }
    
    /**
     * Get suggested output filename
     */
    fun getSuggestedOutputName(
        originalName: String,
        format: ExportFormat,
        quality: ExportQuality
    ): String {
        val nameWithoutExtension = originalName.substringBeforeLast(".")
        val timestamp = System.currentTimeMillis()
        val qualityTag = when (quality) {
            ExportQuality.LOW -> "_low"
            ExportQuality.MEDIUM -> "_medium"
            ExportQuality.HIGH -> "_high"
            ExportQuality.ULTRA -> "_ultra"
            ExportQuality.ORIGINAL -> ""
        }
        return "${nameWithoutExtension}_export_${timestamp}${qualityTag}${format.extension}"
    }
    
    /**
     * Get estimated file size for export
     */
    fun estimateFileSize(
        sourceUri: Uri,
        options: ExportOptions
    ): Long {
        try {
            val extractor = MediaExtractor()
            extractor.setDataSource(context, sourceUri, null)
            
            val videoTrackIndex = findTrack(extractor, "video/")
            if (videoTrackIndex == -1) return 0L
            
            val format = extractor.getTrackFormat(videoTrackIndex)
            val bitrate = try {
                format.getInteger(MediaFormat.KEY_BIT_RATE)
            } catch (e: Exception) {
                // Estimate based on resolution
                val width = format.getInteger(MediaFormat.KEY_WIDTH)
                val height = format.getInteger(MediaFormat.KEY_HEIGHT)
                width * height * 3
            }
            
            val durationUs = format.getLong(MediaFormat.KEY_DURATION)
            val trimDurationUs = if (options.trimEndMs == -1L) {
                durationUs - (options.trimStartMs * 1000)
            } else {
                (options.trimEndMs - options.trimStartMs) * 1000
            }
            
            val adjustedBitrate = (bitrate * options.quality.bitrateMultiplier).toLong()
            val estimatedSize = (adjustedBitrate * trimDurationUs) / 8000000L // Convert to bytes
            
            extractor.release()
            
            return estimatedSize
        } catch (e: Exception) {
            return 0L
        }
    }
}