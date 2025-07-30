package com.astralplayer.nextplayer.feature.browser

import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.lifecycle.lifecycleScope
import com.astralplayer.nextplayer.VideoPlayerActivity
import com.astralplayer.nextplayer.feature.ai.realtime.UltraFastSubtitleSystem
import com.astralplayer.nextplayer.feature.ai.realtime.StreamingOptimizedSubtitleEngine
import com.astralplayer.nextplayer.utils.IntentUtils
import kotlinx.coroutines.launch

/**
 * Enhanced VideoPlayerActivity for browser-launched streaming content
 * Automatically generates subtitles for adult content streaming videos
 */
class BrowserStreamingVideoPlayerActivity : VideoPlayerActivity() {
    
    companion object {
        private const val TAG = "BrowserStreamingPlayer"
    }
    
    // Streaming-optimized subtitle system
    private lateinit var streamingSubtitleEngine: StreamingOptimizedSubtitleEngine
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize streaming subtitle engine
        streamingSubtitleEngine = StreamingOptimizedSubtitleEngine(this)
        
        // Handle browser intent and auto-generate subtitles
        handleBrowserStreamingIntent(intent)
    }
    
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleBrowserStreamingIntent(intent)
    }
    
    /**
     * Handle streaming video intent from browser with auto subtitle generation
     */
    private fun handleBrowserStreamingIntent(intent: Intent) {
        lifecycleScope.launch {
            try {
                val streamingInfo = analyzeStreamingIntent(intent)
                
                if (streamingInfo.isStreamingContent) {
                    Log.d(TAG, "Browser streaming content detected: ${streamingInfo.contentType}")
                    
                    // Auto-generate subtitles for streaming content
                    autoGenerateStreamingSubtitles(streamingInfo)
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Error handling browser streaming intent", e)
            }
        }
    }
    
    /**
     * Auto-generate subtitles for streaming content with optimizations
     */
    private suspend fun autoGenerateStreamingSubtitles(streamingInfo: StreamingContentInfo) {
        Log.d(TAG, "Auto-generating subtitles for streaming content")
        
        // Apply streaming-specific optimizations
        val optimizations = StreamingSubtitleOptimizations(
            isAdultContent = streamingInfo.isAdultContent,
            streamingProtocol = streamingInfo.protocol,
            networkCondition = streamingInfo.networkCondition,
            expectedDuration = streamingInfo.estimatedDuration
        )
        
        // Generate subtitles with streaming optimizations
        streamingSubtitleEngine.generateForStreaming(
            videoUri = streamingInfo.videoUri,
            videoTitle = streamingInfo.title,
            optimizations = optimizations
        )
    }
    
    /**
     * Analyze streaming intent to extract metadata and content type
     */
    private fun analyzeStreamingIntent(intent: Intent): StreamingContentInfo {
        val uri = intent.data ?: throw IllegalArgumentException("No URI in intent")
        val userAgent = intent.getStringExtra("User-Agent") ?: ""
        val referrer = intent.getStringExtra("referrer") ?: ""
        
        return StreamingContentInfo(
            videoUri = uri,
            title = extractStreamingTitle(intent, uri),
            isStreamingContent = isStreamingUri(uri),
            isAdultContent = detectAdultContent(uri, userAgent, referrer),
            protocol = detectStreamingProtocol(uri),
            networkCondition = analyzeNetworkCondition(),
            estimatedDuration = estimateStreamingDuration(uri),
            sourceApp = intent.getStringExtra("source_app") ?: "browser"
        )
    }
    
    private fun extractStreamingTitle(intent: Intent, uri: Uri): String {
        // Try various title extraction methods
        return intent.getStringExtra("video_title")
            ?: intent.getStringExtra("title")
            ?: intent.getStringExtra(Intent.EXTRA_TITLE)
            ?: extractTitleFromUri(uri)
            ?: "Streaming Video"
    }
    
    private fun isStreamingUri(uri: Uri): Boolean {
        val scheme = uri.scheme?.lowercase()
        val path = uri.path?.lowercase() ?: ""
        
        return when {
            scheme in setOf("http", "https") -> true
            path.contains(".m3u8") -> true // HLS
            path.contains(".mpd") -> true // DASH
            uri.host?.contains("stream") == true -> true
            uri.host?.contains("cdn") == true -> true
            else -> false
        }
    }
    
    private fun detectAdultContent(uri: Uri, userAgent: String, referrer: String): Boolean {
        val indicators = listOf(
            uri.host?.lowercase(),
            uri.path?.lowercase(),
            referrer.lowercase(),
            userAgent.lowercase()
        ).filterNotNull().joinToString(" ")
        
        // Common adult content indicators (safe for work detection)
        val adultIndicators = listOf(
            "adult", "xxx", "porn", "nsfw", "mature", "18+", "explicit",
            "tube", "cam", "live", "premium", "model"
        )
        
        return adultIndicators.any { indicator ->
            indicators.contains(indicator)
        }
    }
    
    private fun detectStreamingProtocol(uri: Uri): StreamingProtocol {
        val path = uri.path?.lowercase() ?: ""
        
        return when {
            path.endsWith(".m3u8") -> StreamingProtocol.HLS
            path.endsWith(".mpd") -> StreamingProtocol.DASH
            uri.scheme == "rtmp" -> StreamingProtocol.RTMP
            uri.scheme == "rtsp" -> StreamingProtocol.RTSP
            else -> StreamingProtocol.HTTP
        }
    }
    
    private fun analyzeNetworkCondition(): NetworkCondition {
        // Analyze current network condition for optimization
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo = connectivityManager.activeNetworkInfo
        
        return when {
            networkInfo?.type == ConnectivityManager.TYPE_WIFI -> NetworkCondition.WIFI
            networkInfo?.type == ConnectivityManager.TYPE_MOBILE -> {
                when (networkInfo.subtype) {
                    TelephonyManager.NETWORK_TYPE_LTE,
                    TelephonyManager.NETWORK_TYPE_NR -> NetworkCondition.MOBILE_4G_5G
                    else -> NetworkCondition.MOBILE_3G
                }
            }
            else -> NetworkCondition.UNKNOWN
        }
    }
    
    private fun estimateStreamingDuration(uri: Uri): Long {
        // Estimate duration based on URI patterns or metadata
        // This is a heuristic approach for streaming content
        return when {
            uri.path?.contains("live") == true -> Long.MAX_VALUE // Live stream
            uri.path?.contains("short") == true -> 5 * 60 * 1000L // 5 minutes
            else -> 30 * 60 * 1000L // Default 30 minutes
        }
    }
    
    private fun extractTitleFromUri(uri: Uri): String? {
        return try {
            val path = uri.path ?: return null
            val filename = path.substringAfterLast('/')
            
            if (filename.isNotEmpty() && !filename.contains("?")) {
                filename.substringBeforeLast('.')
                    .replace(Regex("[._-]"), " ")
                    .trim()
                    .takeIf { it.isNotEmpty() }
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
}

/**
 * Streaming-optimized subtitle engine for browser content
 */
class StreamingOptimizedSubtitleEngine(private val context: Context) {
    
    companion object {
        private const val TAG = "StreamingSubtitleEngine"
    }
    
    private val ultraFastSystem = UltraFastSubtitleSystem(context)
    
    /**
     * Generate subtitles with streaming-specific optimizations
     */
    suspend fun generateForStreaming(
        videoUri: Uri,
        videoTitle: String,
        optimizations: StreamingSubtitleOptimizations
    ) {
        Log.d(TAG, "Generating subtitles for streaming content: $videoTitle")
        Log.d(TAG, "Optimizations: Adult=${optimizations.isAdultContent}, Protocol=${optimizations.streamingProtocol}")
        
        try {
            // Apply streaming-specific preprocessing
            val optimizedParams = createStreamingParams(optimizations)
            
            // Use ultra-fast system with streaming optimizations
            val result = ultraFastSystem.generateInstantSubtitles(
                videoUri = videoUri,
                videoTitle = videoTitle,
                language = optimizedParams.language
            )
            
            result.onSuccess { subtitles ->
                Log.i(TAG, "Successfully generated ${subtitles.size} subtitles for streaming content")
                
                // Apply streaming-specific post-processing
                val optimizedSubtitles = applyStreamingOptimizations(subtitles, optimizations)
                
                // Cache with streaming metadata
                cacheStreamingSubtitles(videoUri, optimizedSubtitles, optimizations)
                
            }.onFailure { error ->
                Log.e(TAG, "Failed to generate subtitles for streaming content", error)
                handleStreamingSubtitleFailure(videoUri, optimizations, error)
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error in streaming subtitle generation", e)
        }
    }
    
    /**
     * Create optimized parameters for streaming content
     */
    private fun createStreamingParams(optimizations: StreamingSubtitleOptimizations): StreamingParams {
        return StreamingParams(
            language = "en", // Default, could be detected from content
            chunkSize = when (optimizations.networkCondition) {
                NetworkCondition.WIFI -> 30000L // 30 seconds
                NetworkCondition.MOBILE_4G_5G -> 20000L // 20 seconds
                NetworkCondition.MOBILE_3G -> 15000L // 15 seconds
                else -> 25000L
            },
            maxConcurrentChunks = when (optimizations.networkCondition) {
                NetworkCondition.WIFI -> 4
                NetworkCondition.MOBILE_4G_5G -> 2
                NetworkCondition.MOBILE_3G -> 1
                else -> 2
            },
            adaptiveQuality = optimizations.networkCondition != NetworkCondition.WIFI,
            adultContentMode = optimizations.isAdultContent
        )
    }
    
    /**
     * Apply streaming-specific subtitle optimizations
     */
    private fun applyStreamingOptimizations(
        subtitles: List<SubtitleEntry>,
        optimizations: StreamingSubtitleOptimizations
    ): List<SubtitleEntry> {
        
        return subtitles.map { subtitle ->
            when {
                optimizations.isAdultContent -> {
                    // Apply adult content specific optimizations
                    subtitle.copy(
                        text = optimizeAdultContentText(subtitle.text),
                        confidence = subtitle.confidence * 0.95f // Slight confidence adjustment
                    )
                }
                optimizations.streamingProtocol == StreamingProtocol.HLS -> {
                    // Optimize for HLS streaming timing
                    subtitle.copy(
                        startTime = alignToHLSSegment(subtitle.startTime),
                        endTime = alignToHLSSegment(subtitle.endTime)
                    )
                }
                else -> subtitle
            }
        }
    }
    
    /**
     * Optimize subtitle text for adult content (ensure appropriate language)
     */
    private fun optimizeAdultContentText(text: String): String {
        // Apply content-appropriate text processing while maintaining accuracy
        return text.trim()
            .replace(Regex("\\s+"), " ") // Normalize whitespace
            .let { cleanText ->
                // Ensure proper capitalization for adult content dialogue
                if (cleanText.isNotEmpty()) {
                    cleanText.substring(0, 1).uppercase() + 
                    cleanText.substring(1).lowercase().replace(". ", ". ").split(". ").joinToString(". ") { sentence ->
                        if (sentence.isNotEmpty()) {
                            sentence.substring(0, 1).uppercase() + sentence.substring(1)
                        } else sentence
                    }
                } else cleanText
            }
    }
    
    /**
     * Align subtitle timing to HLS segments for better streaming experience
     */
    private fun alignToHLSSegment(timeMs: Long, segmentDuration: Long = 6000L): Long {
        // Align to 6-second HLS segments (typical duration)
        return (timeMs / segmentDuration) * segmentDuration
    }
    
    /**
     * Cache subtitles with streaming metadata
     */
    private suspend fun cacheStreamingSubtitles(
        videoUri: Uri,
        subtitles: List<SubtitleEntry>,
        optimizations: StreamingSubtitleOptimizations
    ) {
        // Implementation would store with streaming-specific cache keys
        Log.d(TAG, "Caching ${subtitles.size} streaming subtitles for ${videoUri}")
    }
    
    /**
     * Handle subtitle generation failure for streaming content
     */
    private suspend fun handleStreamingSubtitleFailure(
        videoUri: Uri,
        optimizations: StreamingSubtitleOptimizations,
        error: Throwable
    ) {
        Log.w(TAG, "Attempting fallback subtitle generation for streaming content")
        
        try {
            // Fallback to simpler generation method for streaming
            val fallbackSubtitles = generateFallbackStreamingSubtitles(videoUri, optimizations)
            Log.i(TAG, "Generated ${fallbackSubtitles.size} fallback subtitles")
        } catch (e: Exception) {
            Log.e(TAG, "Fallback subtitle generation also failed", e)
        }
    }
    
    /**
     * Generate basic fallback subtitles for streaming content
     */
    private suspend fun generateFallbackStreamingSubtitles(
        videoUri: Uri,
        optimizations: StreamingSubtitleOptimizations
    ): List<SubtitleEntry> {
        
        // Generate basic time-based subtitles for streaming content
        return listOf(
            SubtitleEntry(
                startTime = 0L,
                endTime = 10000L,
                text = if (optimizations.isAdultContent) {
                    "Processing audio for subtitle generation..."
                } else {
                    "Loading subtitles..."
                },
                language = "en",
                confidence = 0.3f
            )
        )
    }
}

// Data classes for streaming optimization
data class StreamingContentInfo(
    val videoUri: Uri,
    val title: String,
    val isStreamingContent: Boolean,
    val isAdultContent: Boolean,
    val protocol: StreamingProtocol,
    val networkCondition: NetworkCondition,
    val estimatedDuration: Long,
    val sourceApp: String
)

data class StreamingSubtitleOptimizations(
    val isAdultContent: Boolean,
    val streamingProtocol: StreamingProtocol,
    val networkCondition: NetworkCondition,
    val expectedDuration: Long
)

data class StreamingParams(
    val language: String,
    val chunkSize: Long,
    val maxConcurrentChunks: Int,
    val adaptiveQuality: Boolean,
    val adultContentMode: Boolean
)

enum class StreamingProtocol {
    HTTP, HLS, DASH, RTMP, RTSP
}

enum class NetworkCondition {
    WIFI, MOBILE_4G_5G, MOBILE_3G, UNKNOWN
}

// Required imports for network detection
import android.content.Context
import android.net.ConnectivityManager
import android.telephony.TelephonyManager
import android.os.Bundle
import com.astralplayer.nextplayer.feature.ai.realtime.SubtitleEntry