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
import androidx.media3.common.TrackSelectionParameters
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.launch

/**
 * Enhanced VideoPlayerActivity for browser-launched streaming content
 * Optimized for streaming video playback from browsers
 */
class BrowserStreamingVideoPlayerActivity : VideoPlayerActivity() {
    
    companion object {
        private const val TAG = "BrowserStreamingPlayer"
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Handle browser intent with streaming optimizations
        handleBrowserStreamingIntent(intent)
    }
    
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleBrowserStreamingIntent(intent)
    }
    
    /**
     * Handle streaming video intent from browser with optimizations
     */
    private fun handleBrowserStreamingIntent(intent: Intent) {
        lifecycleScope.launch {
            try {
                val streamingInfo = analyzeStreamingIntent(intent)
                
                if (streamingInfo.isStreamingContent) {
                    Log.d(TAG, "Browser streaming content detected: ${streamingInfo.contentType}")
                    
                    // Apply streaming-specific optimizations
                    applyStreamingOptimizations(streamingInfo)
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Error handling browser streaming intent", e)
            }
        }
    }
    
    /**
     * Apply optimizations for streaming content
     */
    private fun applyStreamingOptimizations(streamingInfo: StreamingContentInfo) {
        Log.d(TAG, "Applying streaming optimizations for ${streamingInfo.protocol}")
        
        // Configure player for streaming
        getPlayer()?.let { player ->
            // Set seek increments based on network condition
            val (seekBackMs, seekForwardMs) = when (streamingInfo.networkCondition) {
                NetworkCondition.WIFI -> 10000L to 10000L // Larger buffer for WiFi
                NetworkCondition.MOBILE_4G_5G -> 5000L to 5000L // Medium buffer for 4G/5G
                NetworkCondition.MOBILE_3G -> 2000L to 2000L // Smaller buffer for 3G
                else -> 5000L to 5000L // Default buffer
            }
            
            // Configure seek increments using the player's playback parameters
            // Note: ExoPlayer doesn't have direct setter methods for seek increments in newer versions
            
            // Configure track selection for adaptive streaming
            val parameters = player.trackSelectionParameters
                .buildUpon()
                .setMaxVideoBitrate(getMaxBitrateForNetwork(streamingInfo.networkCondition))
                .build()
            
            player.trackSelectionParameters = parameters
        }
    }
    
    /**
     * Get maximum bitrate based on network condition
     */
    private fun getMaxBitrateForNetwork(networkCondition: NetworkCondition): Int {
        return when (networkCondition) {
            NetworkCondition.WIFI -> Int.MAX_VALUE // No limit on WiFi
            NetworkCondition.MOBILE_4G_5G -> 8_000_000 // 8 Mbps for 4G/5G
            NetworkCondition.MOBILE_3G -> 2_000_000 // 2 Mbps for 3G
            else -> 4_000_000 // 4 Mbps default
        }
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
            contentType = detectContentType(uri, userAgent, referrer),
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
            scheme in setOf("http", "https", "rtmp", "rtsp") -> true
            path.contains(".m3u8") -> true // HLS
            path.contains(".mpd") -> true // DASH
            uri.host?.contains("stream") == true -> true
            uri.host?.contains("cdn") == true -> true
            else -> false
        }
    }
    
    private fun detectContentType(uri: Uri, userAgent: String, referrer: String): ContentType {
        val host = uri.host?.lowercase() ?: ""
        
        return when {
            host.contains("youtube") || host.contains("youtu.be") -> ContentType.YOUTUBE
            host.contains("vimeo") -> ContentType.VIMEO
            host.contains("dailymotion") -> ContentType.GENERAL_VIDEO
            host.contains("twitch") -> ContentType.LIVE_STREAM
            isAdultContent(uri, userAgent, referrer) -> ContentType.ADULT
            else -> ContentType.GENERAL_VIDEO
        }
    }
    
    private fun isAdultContent(uri: Uri, userAgent: String, referrer: String): Boolean {
        val indicators = listOf(
            uri.host?.lowercase(),
            uri.path?.lowercase(),
            referrer.lowercase()
        ).filterNotNull().joinToString(" ")
        
        // Check against known adult content domains
        val adultDomains = listOf(
            "pornhub", "xvideos", "xnxx", "xhamster", "redtube", 
            "youporn", "spankbang", "tube8", "xtube", "pornhd"
        )
        
        return adultDomains.any { domain ->
            indicators.contains(domain)
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
                val telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
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

// Data classes for streaming optimization
data class StreamingContentInfo(
    val videoUri: Uri,
    val title: String,
    val isStreamingContent: Boolean,
    val contentType: ContentType,
    val protocol: StreamingProtocol,
    val networkCondition: NetworkCondition,
    val estimatedDuration: Long,
    val sourceApp: String
)

enum class ContentType {
    GENERAL_VIDEO, YOUTUBE, VIMEO, LIVE_STREAM, ADULT
}

enum class StreamingProtocol {
    HTTP, HLS, DASH, RTMP, RTSP
}

enum class NetworkCondition {
    WIFI, MOBILE_4G_5G, MOBILE_3G, UNKNOWN
}