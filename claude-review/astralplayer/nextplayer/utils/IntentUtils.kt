package com.astralplayer.nextplayer.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.webkit.MimeTypeMap
import com.astralplayer.nextplayer.VideoPlayerActivity
import java.net.URLDecoder

object IntentUtils {
    private const val TAG = "IntentUtils"
    
    // Supported video MIME types
    private val VIDEO_MIME_TYPES = setOf(
        "video/mp4", "video/3gpp", "video/3gpp2", "video/avi",
        "video/x-matroska", "video/webm", "video/x-flv",
        "video/quicktime", "video/x-msvideo", "video/x-ms-wmv",
        "video/mpeg", "video/mpg", "video/m4v", "video/mov",
        "application/x-mpegURL", "application/vnd.apple.mpegurl",
        "application/dash+xml", "video/mp2t", "video/MP2T"
    )
    
    // Supported video extensions
    private val VIDEO_EXTENSIONS = setOf(
        "mp4", "mkv", "avi", "mov", "wmv", "flv", "webm",
        "m4v", "3gp", "3g2", "ts", "m2ts", "mts", "mpg",
        "mpeg", "m3u8", "mpd", "vob", "ogv", "divx", "xvid"
    )
    
    // Streaming protocols
    private val STREAMING_SCHEMES = setOf(
        "rtmp", "rtmps", "rtsp", "rtsps", "mms", "mmsh"
    )
    
    /**
     * Handle incoming video intent
     */
    fun handleVideoIntent(context: Context, intent: Intent): Boolean {
        try {
            val uri = intent.data ?: return false
            logIntentDetails(intent, TAG)
            
            // Check if this is a video intent
            if (!isVideoIntent(intent)) {
                Log.w(TAG, "Not a video intent")
                return false
            }
            
            // Extract video information
            val videoTitle = extractVideoTitle(intent, uri)
            val isStreaming = isStreamingUri(uri)
            val isFromBrowser = isFromBrowser(intent)
            
            // Create player intent
            val playerIntent = Intent(context, VideoPlayerActivity::class.java).apply {
                data = uri
                putExtra("video_title", videoTitle)
                putExtra("from_external", true)
                putExtra("streaming_mode", isStreaming)
                putExtra("from_browser", isFromBrowser)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                
                // Copy over any custom extras
                intent.extras?.let { extras ->
                    putExtras(extras)
                }
            }
            
            context.startActivity(playerIntent)
            return true
            
        } catch (e: Exception) {
            Log.e(TAG, "Error handling video intent", e)
            return false
        }
    }
    
    /**
     * Check if intent is for video playback
     */
    fun isVideoIntent(intent: Intent): Boolean {
        val action = intent.action
        val type = intent.type
        val uri = intent.data
        
        // Check action
        if (action != Intent.ACTION_VIEW && action != Intent.ACTION_SEND) {
            return false
        }
        
        // Check MIME type
        if (!type.isNullOrBlank()) {
            if (type.startsWith("video/") || VIDEO_MIME_TYPES.contains(type)) {
                return true
            }
        }
        
        // Check URI
        if (uri != null) {
            // Check scheme
            val scheme = uri.scheme
            if (STREAMING_SCHEMES.contains(scheme)) {
                return true
            }
            
            // Check file extension
            val path = uri.path
            if (!path.isNullOrBlank()) {
                val extension = getFileExtension(path)
                if (VIDEO_EXTENSIONS.contains(extension.lowercase())) {
                    return true
                }
            }
            
            // Check if it's a streaming URL
            if (isStreamingUri(uri)) {
                return true
            }
        }
        
        return false
    }
    
    /**
     * Check if URI is a video URI
     */
    fun isVideoUri(uri: Uri, mimeType: String?): Boolean {
        // Check MIME type first
        if (!mimeType.isNullOrBlank() && (mimeType.startsWith("video/") || VIDEO_MIME_TYPES.contains(mimeType))) {
            return true
        }
        
        // Check URI scheme
        val scheme = uri.scheme
        if (STREAMING_SCHEMES.contains(scheme)) {
            return true
        }
        
        // Check file extension
        val path = uri.path
        if (!path.isNullOrBlank()) {
            val extension = getFileExtension(path)
            if (VIDEO_EXTENSIONS.contains(extension.lowercase())) {
                return true
            }
        }
        
        // Check if it's a streaming URL
        if (isStreamingUri(uri)) {
            return true
        }
        
        return false
    }
    
    /**
     * Extract video title from intent
     */
    fun extractVideoTitle(intent: Intent, uri: Uri? = null): String {
        // Check intent extras
        intent.getStringExtra("title")?.let { return it }
        intent.getStringExtra("video_title")?.let { return it }
        intent.getStringExtra(Intent.EXTRA_TITLE)?.let { return it }
        intent.getStringExtra(Intent.EXTRA_SUBJECT)?.let { return it }
        
        // Extract from URI
        val targetUri = uri ?: intent.data
        targetUri?.let {
            // Try to get filename from path
            val path = it.path
            if (!path.isNullOrBlank()) {
                val segments = path.split("/")
                val filename = segments.lastOrNull()
                if (!filename.isNullOrBlank() && filename.contains(".")) {
                    return filename.substringBeforeLast(".")
                }
            }
            
            // Use last path segment
            it.lastPathSegment?.let { segment ->
                if (segment.contains(".")) {
                    return segment.substringBeforeLast(".")
                }
                return segment
            }
        }
        
        return "Unknown Video"
    }
    
    /**
     * Check if URI is a streaming URL
     */
    private fun isStreamingUri(uri: Uri): Boolean {
        val scheme = uri.scheme
        val path = uri.path ?: ""
        val host = uri.host ?: ""
        
        // Check streaming schemes
        if (STREAMING_SCHEMES.contains(scheme)) {
            return true
        }
        
        // Check for streaming extensions
        if (path.endsWith(".m3u8", true) || path.endsWith(".mpd", true)) {
            return true
        }
        
        // Check for common streaming patterns
        if (path.contains("stream", true) || 
            path.contains("live", true) ||
            path.contains("playlist", true) ||
            host.contains("stream", true) ||
            host.contains("video", true)) {
            return true
        }
        
        return false
    }
    
    /**
     * Check if intent is from a browser
     */
    private fun isFromBrowser(intent: Intent): Boolean {
        val referrer = intent.getStringExtra(Intent.EXTRA_REFERRER_NAME)
        val callingPackage = intent.getStringExtra("calling_package")
        
        val browserPackages = setOf(
            "com.android.chrome",
            "org.mozilla.firefox",
            "com.opera.browser",
            "com.microsoft.emmx",
            "com.brave.browser",
            "com.kiwibrowser.browser",
            "com.android.browser"
        )
        
        return browserPackages.any { 
            referrer?.contains(it) == true || callingPackage?.contains(it) == true 
        }
    }
    
    /**
     * Get file extension from path
     */
    private fun getFileExtension(path: String): String {
        val decodedPath = try {
            URLDecoder.decode(path, "UTF-8")
        } catch (e: Exception) {
            path
        }
        
        return MimeTypeMap.getFileExtensionFromUrl(decodedPath) ?: 
               decodedPath.substringAfterLast('.', "")
    }
    
    /**
     * Get MIME type from URI
     */
    fun getMimeType(context: Context, uri: Uri): String? {
        return context.contentResolver.getType(uri) ?: run {
            val extension = getFileExtension(uri.path ?: "")
            MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
        }
    }
    
    /**
     * Log intent details for debugging
     */
    fun logIntentDetails(intent: Intent, tag: String) {
        Log.d(tag, "Intent Details:")
        Log.d(tag, "  Action: ${intent.action}")
        Log.d(tag, "  Type: ${intent.type}")
        Log.d(tag, "  Data: ${intent.data}")
        Log.d(tag, "  Scheme: ${intent.data?.scheme}")
        Log.d(tag, "  Categories: ${intent.categories}")
        Log.d(tag, "  Flags: ${intent.flags}")
        
        intent.extras?.let { extras ->
            Log.d(tag, "  Extras:")
            for (key in extras.keySet()) {
                Log.d(tag, "    $key: ${extras.get(key)}")
            }
        }
    }
    

}