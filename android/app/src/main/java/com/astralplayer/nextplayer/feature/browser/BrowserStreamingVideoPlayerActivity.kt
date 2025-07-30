package com.astralplayer.nextplayer.feature.browser

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Bundle
import android.telephony.TelephonyManager
import android.util.Log
import androidx.lifecycle.lifecycleScope
import com.astralplayer.nextplayer.VideoPlayerActivity
import com.astralplayer.nextplayer.feature.ai.realtime.SubtitleEntry
import com.astralplayer.nextplayer.feature.ai.realtime.UltraFastSubtitleSystem
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
            sourceApp = intent.getStringExtra("source_app") ?: "browser",
            contentType = "video/streaming"
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