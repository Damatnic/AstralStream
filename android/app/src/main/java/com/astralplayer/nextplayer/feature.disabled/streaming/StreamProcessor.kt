package com.astralplayer.nextplayer.feature.streaming

import android.content.Context
import android.net.Uri
import android.util.Log
import com.astralplayer.nextplayer.feature.network.AdvancedNetworkManager
import com.astralplayer.nextplayer.feature.network.NetworkResult
import com.astralplayer.nextplayer.feature.network.VideoQuality
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.Request
import java.util.regex.Pattern

/**
 * Advanced stream processor for AstralStream
 * Handles HLS, DASH, progressive download, and adaptive streaming
 */
class StreamProcessor(
    private val context: Context,
    private val networkManager: AdvancedNetworkManager
) {
    companion object {
        private const val TAG = "StreamProcessor"
        
        // Stream format patterns
        private val HLS_PATTERN = Pattern.compile(".*\\.m3u8(\\?.*)?", Pattern.CASE_INSENSITIVE)
        private val DASH_PATTERN = Pattern.compile(".*\\.mpd(\\?.*)?", Pattern.CASE_INSENSITIVE)
        private val MP4_PATTERN = Pattern.compile(".*\\.(mp4|mov|avi|mkv|webm)(\\?.*)?", Pattern.CASE_INSENSITIVE)
        
        // HLS playlist patterns
        private val MASTER_PLAYLIST_PATTERN = Pattern.compile("#EXT-X-STREAM-INF:.*")
        private val MEDIA_PLAYLIST_PATTERN = Pattern.compile("#EXT-X-TARGETDURATION:.*")
        private val SEGMENT_PATTERN = Pattern.compile("^(?!#).*\\.(ts|m4s|mp4|webm).*$")
        
        // Quality extraction patterns
        private val RESOLUTION_PATTERN = Pattern.compile("RESOLUTION=(\\d+)x(\\d+)")
        private val BANDWIDTH_PATTERN = Pattern.compile("BANDWIDTH=(\\d+)")
        private val CODECS_PATTERN = Pattern.compile("CODECS=\"([^\"]+)\"")
        
        // Buffer configuration
        private const val MIN_BUFFER_MS = 15000
        private const val MAX_BUFFER_MS = 50000
        private const val BUFFER_FOR_PLAYBACK_MS = 2500
        private const val BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS = 5000
    }
    
    // Stream state management
    private val _streamState = MutableStateFlow(StreamState.IDLE)
    val streamState: StateFlow<StreamState> = _streamState.asStateFlow()
    
    private val _bufferHealth = MutableStateFlow(0f)
    val bufferHealth: StateFlow<Float> = _bufferHealth.asStateFlow()
    
    private val _currentQuality = MutableStateFlow(VideoQuality.AUTO)
    val currentQuality: StateFlow<VideoQuality> = _currentQuality.asStateFlow()
    
    private val _availableQualities = MutableStateFlow<List<StreamQuality>>(emptyList())
    val availableQualities: StateFlow<List<StreamQuality>> = _availableQualities.asStateFlow()
    
    /**
     * Process and analyze stream URL to determine format and available qualities
     */
    suspend fun processStream(url: String): StreamProcessingResult {
        return withContext(Dispatchers.IO) {
            try {
                _streamState.value = StreamState.ANALYZING
                Log.d(TAG, "Processing stream: $url")
                
                val streamFormat = detectStreamFormat(url)
                Log.d(TAG, "Detected format: $streamFormat")
                
                val result = when (streamFormat) {
                    StreamFormat.HLS -> processHlsStream(url)
                    StreamFormat.DASH -> processDashStream(url)
                    StreamFormat.PROGRESSIVE -> processProgressiveStream(url)
                    StreamFormat.UNKNOWN -> processUnknownStream(url)
                }
                
                _streamState.value = if (result.success) StreamState.READY else StreamState.ERROR
                return@withContext result
                
            } catch (e: Exception) {
                Log.e(TAG, "Error processing stream", e)
                _streamState.value = StreamState.ERROR
                StreamProcessingResult(
                    success = false,
                    streamFormat = StreamFormat.UNKNOWN,
                    playbackUrl = url,
                    error = e.message
                )
            }
        }
    }
    
    /**
     * Detect stream format from URL
     */
    private fun detectStreamFormat(url: String): StreamFormat {
        return when {
            HLS_PATTERN.matcher(url).matches() -> StreamFormat.HLS
            DASH_PATTERN.matcher(url).matches() -> StreamFormat.DASH
            MP4_PATTERN.matcher(url).matches() -> StreamFormat.PROGRESSIVE
            else -> StreamFormat.UNKNOWN
        }
    }
    
    /**
     * Process HLS (HTTP Live Streaming) manifest
     */
    private suspend fun processHlsStream(url: String): StreamProcessingResult {
        try {
            val manifest = fetchManifest(url)
            if (manifest == null) {
                return StreamProcessingResult(
                    success = false,
                    streamFormat = StreamFormat.HLS,
                    playbackUrl = url,
                    error = "Failed to fetch HLS manifest"
                )
            }
            
            val playlistType = determineHlsPlaylistType(manifest)
            
            return when (playlistType) {
                HlsPlaylistType.MASTER -> processMasterPlaylist(url, manifest)
                HlsPlaylistType.MEDIA -> processMediaPlaylist(url, manifest)
                HlsPlaylistType.UNKNOWN -> StreamProcessingResult(
                    success = false,
                    streamFormat = StreamFormat.HLS,
                    playbackUrl = url,
                    error = "Unknown HLS playlist type"
                )
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error processing HLS stream", e)
            return StreamProcessingResult(
                success = false,
                streamFormat = StreamFormat.HLS,
                playbackUrl = url,
                error = "HLS processing failed: ${e.message}"
            )
        }
    }
    
    /**
     * Process HLS master playlist with multiple quality variants
     */
    private fun processMasterPlaylist(url: String, manifest: String): StreamProcessingResult {
        val qualities = mutableListOf<StreamQuality>()
        val baseUrl = getBaseUrl(url)
        
        val lines = manifest.lines()
        var i = 0
        
        while (i < lines.size) {
            val line = lines[i].trim()
            
            if (line.startsWith("#EXT-X-STREAM-INF:")) {
                val streamInfo = line
                val playlistUrl = if (i + 1 < lines.size) lines[i + 1].trim() else ""
                
                if (playlistUrl.isNotEmpty() && !playlistUrl.startsWith("#")) {
                    val quality = parseStreamQuality(streamInfo, baseUrl, playlistUrl)
                    if (quality != null) {
                        qualities.add(quality)
                    }
                }
                i += 2
            } else {
                i++
            }
        }
        
        // Sort qualities by bandwidth (highest first)
        qualities.sortByDescending { it.bandwidth }
        _availableQualities.value = qualities
        
        // Select best quality for current network conditions
        val recommendedQuality = selectOptimalQuality(qualities)
        _currentQuality.value = recommendedQuality?.videoQuality ?: VideoQuality.AUTO
        
        return StreamProcessingResult(
            success = true,
            streamFormat = StreamFormat.HLS,
            playbackUrl = recommendedQuality?.url ?: url,
            qualities = qualities,
            selectedQuality = recommendedQuality,
            isAdaptive = true
        )
    }
    
    /**
     * Process HLS media playlist (single quality)
     */
    private fun processMediaPlaylist(url: String, manifest: String): StreamProcessingResult {
        val segments = extractHlsSegments(url, manifest)
        
        return StreamProcessingResult(
            success = true,
            streamFormat = StreamFormat.HLS,
            playbackUrl = url,
            segments = segments,
            isAdaptive = false
        )
    }
    
    /**
     * Process DASH (Dynamic Adaptive Streaming) manifest
     */
    private suspend fun processDashStream(url: String): StreamProcessingResult {
        try {
            val manifest = fetchManifest(url)
            if (manifest == null) {
                return StreamProcessingResult(
                    success = false,
                    streamFormat = StreamFormat.DASH,
                    playbackUrl = url,
                    error = "Failed to fetch DASH manifest"
                )
            }
            
            val qualities = parseDashManifest(url, manifest)
            _availableQualities.value = qualities
            
            val recommendedQuality = selectOptimalQuality(qualities)
            _currentQuality.value = recommendedQuality?.videoQuality ?: VideoQuality.AUTO
            
            return StreamProcessingResult(
                success = true,
                streamFormat = StreamFormat.DASH,
                playbackUrl = recommendedQuality?.url ?: url,
                qualities = qualities,
                selectedQuality = recommendedQuality,
                isAdaptive = true
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "Error processing DASH stream", e)
            return StreamProcessingResult(
                success = false,
                streamFormat = StreamFormat.DASH,
                playbackUrl = url,
                error = "DASH processing failed: ${e.message}"
            )
        }
    }
    
    /**
     * Process progressive download stream (direct MP4, etc.)
     */
    private suspend fun processProgressiveStream(url: String): StreamProcessingResult {
        try {
            // Probe the stream to get metadata
            val metadata = probeStreamMetadata(url)
            
            val quality = StreamQuality(
                id = "progressive",
                url = url,
                bandwidth = metadata?.bitrate ?: 5000000, // Default 5Mbps
                resolution = metadata?.resolution ?: "Unknown",
                width = metadata?.width ?: 0,
                height = metadata?.height ?: 0,
                fps = metadata?.fps ?: 30.0,
                codecs = metadata?.codecs ?: "Unknown",
                videoQuality = estimateVideoQuality(metadata?.width ?: 0, metadata?.height ?: 0)
            )
            
            _availableQualities.value = listOf(quality)
            _currentQuality.value = quality.videoQuality
            
            return StreamProcessingResult(
                success = true,
                streamFormat = StreamFormat.PROGRESSIVE,
                playbackUrl = url,
                qualities = listOf(quality),
                selectedQuality = quality,
                isAdaptive = false,
                metadata = metadata
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "Error processing progressive stream", e)
            return StreamProcessingResult(
                success = false,
                streamFormat = StreamFormat.PROGRESSIVE,
                playbackUrl = url,
                error = "Progressive stream processing failed: ${e.message}"
            )
        }
    }
    
    /**
     * Process unknown stream format with fallback detection
     */
    private suspend fun processUnknownStream(url: String): StreamProcessingResult {
        try {
            // Try to probe the URL and detect format from response
            val request = Request.Builder().url(url).head().build()
            val result = networkManager.executeRequest(request)
            
            when (result) {
                is NetworkResult.Success -> {
                    val contentType = result.response.header("Content-Type")
                    val detectedFormat = detectFormatFromContentType(contentType)
                    
                    return when (detectedFormat) {
                        StreamFormat.HLS -> processHlsStream(url)
                        StreamFormat.DASH -> processDashStream(url)
                        StreamFormat.PROGRESSIVE -> processProgressiveStream(url)
                        StreamFormat.UNKNOWN -> StreamProcessingResult(
                            success = true,
                            streamFormat = StreamFormat.UNKNOWN,
                            playbackUrl = url,
                            error = "Unknown stream format, will attempt direct playback"
                        )
                    }
                }
                else -> {
                    return StreamProcessingResult(
                        success = false,
                        streamFormat = StreamFormat.UNKNOWN,
                        playbackUrl = url,
                        error = "Failed to probe stream format"
                    )
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error processing unknown stream", e)
            return StreamProcessingResult(
                success = false,
                streamFormat = StreamFormat.UNKNOWN,
                playbackUrl = url,
                error = "Unknown stream processing failed: ${e.message}"
            )
        }
    }
    
    /**
     * Switch to different quality stream
     */
    suspend fun switchQuality(qualityId: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val quality = _availableQualities.value.find { it.id == qualityId }
                if (quality != null) {
                    _currentQuality.value = quality.videoQuality
                    Log.d(TAG, "Switched to quality: ${quality.resolution}")
                    true
                } else {
                    Log.w(TAG, "Quality not found: $qualityId")
                    false
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error switching quality", e)
                false
            }
        }
    }
    
    /**
     * Get optimal buffer configuration based on network conditions
     */
    fun getBufferConfiguration(): BufferConfiguration {
        val networkQuality = networkManager.networkQuality.value
        
        return when (networkQuality) {
            com.astralplayer.nextplayer.feature.network.NetworkQuality.EXCELLENT -> BufferConfiguration(
                minBufferMs = MIN_BUFFER_MS,
                maxBufferMs = MAX_BUFFER_MS,
                bufferForPlaybackMs = BUFFER_FOR_PLAYBACK_MS,
                bufferForPlaybackAfterRebufferMs = BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS
            )
            com.astralplayer.nextplayer.feature.network.NetworkQuality.GOOD -> BufferConfiguration(
                minBufferMs = MIN_BUFFER_MS + 5000,
                maxBufferMs = MAX_BUFFER_MS + 10000,
                bufferForPlaybackMs = BUFFER_FOR_PLAYBACK_MS + 1000,
                bufferForPlaybackAfterRebufferMs = BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS + 2000
            )
            else -> BufferConfiguration(
                minBufferMs = MIN_BUFFER_MS + 10000,
                maxBufferMs = MAX_BUFFER_MS + 20000,
                bufferForPlaybackMs = BUFFER_FOR_PLAYBACK_MS + 2000,
                bufferForPlaybackAfterRebufferMs = BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS + 5000
            )
        }
    }
    
    // Helper functions
    
    private suspend fun fetchManifest(url: String): String? {
        val request = Request.Builder().url(url).build()
        return when (val result = networkManager.executeRequest(request)) {
            is NetworkResult.Success -> result.response.body?.string()
            else -> null
        }
    }
    
    private fun determineHlsPlaylistType(manifest: String): HlsPlaylistType {
        return when {
            MASTER_PLAYLIST_PATTERN.matcher(manifest).find() -> HlsPlaylistType.MASTER
            MEDIA_PLAYLIST_PATTERN.matcher(manifest).find() -> HlsPlaylistType.MEDIA
            else -> HlsPlaylistType.UNKNOWN
        }
    }
    
    private fun parseStreamQuality(streamInfo: String, baseUrl: String, playlistUrl: String): StreamQuality? {
        try {
            val resolutionMatcher = RESOLUTION_PATTERN.matcher(streamInfo)
            val bandwidthMatcher = BANDWIDTH_PATTERN.matcher(streamInfo)
            val codecsMatcher = CODECS_PATTERN.matcher(streamInfo)
            
            val width = if (resolutionMatcher.find()) resolutionMatcher.group(1)?.toInt() ?: 0 else 0
            val height = if (resolutionMatcher.find()) resolutionMatcher.group(2)?.toInt() ?: 0 else 0
            val bandwidth = if (bandwidthMatcher.find()) bandwidthMatcher.group(1)?.toInt() ?: 0 else 0
            val codecs = if (codecsMatcher.find()) codecsMatcher.group(1) ?: "Unknown" else "Unknown"
            
            val fullUrl = if (playlistUrl.startsWith("http")) playlistUrl else "$baseUrl/$playlistUrl"
            
            return StreamQuality(
                id = "${width}x${height}_${bandwidth}",
                url = fullUrl,
                bandwidth = bandwidth,
                resolution = "${width}x${height}",
                width = width,
                height = height,
                fps = 30.0, // Default, would need to be parsed from stream info
                codecs = codecs,
                videoQuality = estimateVideoQuality(width, height)
            )
            
        } catch (e: Exception) {
            Log.w(TAG, "Error parsing stream quality", e)
            return null
        }
    }
    
    private fun extractHlsSegments(baseUrl: String, manifest: String): List<StreamSegment> {
        val segments = mutableListOf<StreamSegment>()
        val lines = manifest.lines()
        
        for (line in lines) {
            val trimmedLine = line.trim()
            if (SEGMENT_PATTERN.matcher(trimmedLine).matches()) {
                val segmentUrl = if (trimmedLine.startsWith("http")) {
                    trimmedLine
                } else {
                    "${getBaseUrl(baseUrl)}/$trimmedLine"
                }
                
                segments.add(StreamSegment(url = segmentUrl, duration = 10.0)) // Default duration
            }
        }
        
        return segments
    }
    
    private fun parseDashManifest(url: String, manifest: String): List<StreamQuality> {
        // Simplified DASH parsing - would need proper XML parsing in production
        val qualities = mutableListOf<StreamQuality>()
        
        // This is a placeholder implementation
        // Real DASH parsing would require XML parsing of the MPD manifest
        qualities.add(
            StreamQuality(
                id = "dash_auto",
                url = url,
                bandwidth = 5000000,
                resolution = "Auto",
                width = 0,
                height = 0,
                fps = 30.0,
                codecs = "Unknown",
                videoQuality = VideoQuality.AUTO
            )
        )
        
        return qualities
    }
    
    private suspend fun probeStreamMetadata(url: String): StreamMetadata? {
        try {
            val request = Request.Builder().url(url).head().build()
            val result = networkManager.executeRequest(request)
            
            return when (result) {
                is NetworkResult.Success -> {
                    val contentLength = result.response.header("Content-Length")?.toLongOrNull()
                    val contentType = result.response.header("Content-Type")
                    
                    StreamMetadata(
                        contentLength = contentLength,
                        contentType = contentType,
                        bitrate = estimateBitrateFromSize(contentLength),
                        resolution = "Unknown",
                        width = 0,
                        height = 0,
                        fps = 30.0,
                        codecs = "Unknown"
                    )
                }
                else -> null
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error probing stream metadata", e)
            return null
        }
    }
    
    private fun detectFormatFromContentType(contentType: String?): StreamFormat {
        return when {
            contentType?.contains("application/vnd.apple.mpegurl") == true -> StreamFormat.HLS
            contentType?.contains("application/dash+xml") == true -> StreamFormat.DASH
            contentType?.contains("video/") == true -> StreamFormat.PROGRESSIVE
            else -> StreamFormat.UNKNOWN
        }
    }
    
    private fun selectOptimalQuality(qualities: List<StreamQuality>): StreamQuality? {
        if (qualities.isEmpty()) return null
        
        val recommendedVideoQuality = networkManager.getRecommendedVideoQuality()
        
        // Find best matching quality for network conditions
        return qualities.find { it.videoQuality == recommendedVideoQuality }
            ?: qualities.minByOrNull { 
                kotlin.math.abs(it.height - getHeightForVideoQuality(recommendedVideoQuality))
            }
            ?: qualities.first()
    }
    
    private fun estimateVideoQuality(width: Int, height: Int): VideoQuality {
        return when {
            height >= 2160 -> VideoQuality.ULTRA_HD_4K
            height >= 1080 -> VideoQuality.FULL_HD_1080P
            height >= 720 -> VideoQuality.HD_720P
            height >= 480 -> VideoQuality.SD_480P
            height >= 360 -> VideoQuality.SD_360P
            height >= 240 -> VideoQuality.SD_240P
            else -> VideoQuality.AUTO
        }
    }
    
    private fun getHeightForVideoQuality(quality: VideoQuality): Int {
        return when (quality) {
            VideoQuality.ULTRA_HD_4K -> 2160
            VideoQuality.FULL_HD_1080P -> 1080
            VideoQuality.HD_720P -> 720
            VideoQuality.SD_480P -> 480
            VideoQuality.SD_360P -> 360
            VideoQuality.SD_240P -> 240
            else -> 720 // Default
        }
    }
    
    private fun estimateBitrateFromSize(contentLength: Long?): Int {
        // Rough estimation based on file size
        return when {
            contentLength == null -> 2000000 // 2Mbps default
            contentLength > 500_000_000 -> 8000000 // >500MB = ~8Mbps
            contentLength > 200_000_000 -> 5000000 // >200MB = ~5Mbps
            contentLength > 100_000_000 -> 3000000 // >100MB = ~3Mbps
            else -> 1500000 // <100MB = ~1.5Mbps
        }
    }
    
    private fun getBaseUrl(url: String): String {
        val uri = Uri.parse(url)
        return "${uri.scheme}://${uri.host}${uri.path?.substringBeforeLast("/") ?: ""}"
    }
}

/**
 * Data classes for stream processing
 */
data class StreamProcessingResult(
    val success: Boolean,
    val streamFormat: StreamFormat,
    val playbackUrl: String,
    val qualities: List<StreamQuality> = emptyList(),
    val selectedQuality: StreamQuality? = null,
    val segments: List<StreamSegment> = emptyList(),
    val isAdaptive: Boolean = false,
    val metadata: StreamMetadata? = null,
    val error: String? = null
)

data class StreamQuality(
    val id: String,
    val url: String,
    val bandwidth: Int,
    val resolution: String,
    val width: Int,
    val height: Int,
    val fps: Double,
    val codecs: String,
    val videoQuality: VideoQuality
)

data class StreamSegment(
    val url: String,
    val duration: Double,
    val byteRange: String? = null
)

data class StreamMetadata(
    val contentLength: Long?,
    val contentType: String?,
    val bitrate: Int?,
    val resolution: String?,
    val width: Int?,
    val height: Int?,
    val fps: Double?,
    val codecs: String?
)

data class BufferConfiguration(
    val minBufferMs: Int,
    val maxBufferMs: Int,
    val bufferForPlaybackMs: Int,
    val bufferForPlaybackAfterRebufferMs: Int
)

enum class StreamFormat {
    HLS, DASH, PROGRESSIVE, UNKNOWN
}

enum class HlsPlaylistType {
    MASTER, MEDIA, UNKNOWN
}

enum class StreamState {
    IDLE, ANALYZING, READY, BUFFERING, PLAYING, PAUSED, ERROR
}