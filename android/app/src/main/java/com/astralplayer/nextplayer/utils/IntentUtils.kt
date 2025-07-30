package com.astralplayer.nextplayer.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import java.io.File

/**
 * Utility class for handling various intents, especially "Open with" functionality
 */
object IntentUtils {
    
    private const val TAG = "IntentUtils"
    
    // Supported video extensions
    private val VIDEO_EXTENSIONS = setOf(
        "mp4", "mkv", "avi", "mov", "wmv", "flv", "webm", "3gp", "ts", "m4v",
        "mpg", "mpeg", "vob", "rm", "rmvb", "asf", "ogv", "divx", "f4v", "m2ts",
        "mts", "vro", "dat", "nsv", "nuv", "rec", "trp", "tod", "mod", "mxf",
        "r3d", "gxf", "lxf", "wm", "wtv", "mpl", "mlv", "tak", "dav", "psf"
    )
    
    // Streaming extensions
    private val STREAMING_EXTENSIONS = setOf(
        "m3u8", "mpd", "ism", "isml"
    )
    
    // Adult content specific extensions (common formats)
    private val ADULT_CONTENT_EXTENSIONS = setOf(
        "mp4", "mkv", "avi", "wmv", "flv", "mov", "mpg", "mpeg", "vob",
        "rm", "rmvb", "asf", "ogv", "webm", "3gp", "f4v", "m4v"
    )
    
    /**
     * Handle incoming video intent from browser or other apps
     */
    fun handleVideoIntent(context: Context, intent: Intent): Boolean {
        return try {
            Log.d(TAG, "Handling video intent: ${intent.action}")
            Log.d(TAG, "Intent data: ${intent.data}")
            Log.d(TAG, "Intent type: ${intent.type}")
            
            when (intent.action) {
                Intent.ACTION_VIEW -> handleViewIntent(context, intent)
                Intent.ACTION_SEND -> handleSendIntent(context, intent)
                Intent.ACTION_SEND_MULTIPLE -> handleSendMultipleIntent(context, intent)
                else -> {
                    Log.w(TAG, "Unsupported intent action: ${intent.action}")
                    false
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling video intent", e)
            false
        }
    }
    
    private fun handleViewIntent(context: Context, intent: Intent): Boolean {
        val uri = intent.data ?: return false
        val mimeType = intent.type
        
        Log.d(TAG, "Handling VIEW intent for URI: $uri")
        Log.d(TAG, "MIME type: $mimeType")
        
        if (isVideoUri(uri, mimeType)) {
            val videoTitle = extractVideoTitle(context, uri, intent)
            startVideoPlayer(context, uri, videoTitle, intent)
            return true
        }
        
        return false
    }
    
    private fun handleSendIntent(context: Context, intent: Intent): Boolean {
        val uri = intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM) ?: return false
        
        Log.d(TAG, "Handling SEND intent for URI: $uri")
        
        if (isVideoUri(uri, intent.type)) {
            val videoTitle = extractVideoTitle(context, uri, intent)
            startVideoPlayer(context, uri, videoTitle, intent)
            return true
        }
        
        return false
    }
    
    private fun handleSendMultipleIntent(context: Context, intent: Intent): Boolean {
        val uris = intent.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM) ?: return false
        
        Log.d(TAG, "Handling SEND_MULTIPLE intent for ${uris.size} URIs")
        
        val videoUris = uris.filter { isVideoUri(it, null) }
        
        if (videoUris.isNotEmpty()) {
            // For multiple videos, create a playlist
            startVideoPlayerWithPlaylist(context, videoUris, intent)
            return true
        }
        
        return false
    }
    
    /**
     * Check if URI represents a video file
     */
    fun isVideoUri(uri: Uri, mimeType: String? = null): Boolean {
        // Check MIME type first
        if (mimeType != null) {
            if (mimeType.startsWith("video/")) {
                Log.d(TAG, "Video detected by MIME type: $mimeType")
                return true
            }
        }
        
        // Check file extension
        val path = uri.path ?: uri.toString()
        val extension = getFileExtension(path).lowercase()
        
        val isVideo = VIDEO_EXTENSIONS.contains(extension) || 
                     STREAMING_EXTENSIONS.contains(extension)
        
        if (isVideo) {
            Log.d(TAG, "Video detected by extension: $extension")
        }
        
        return isVideo
    }
    
    /**
     * Extract video title from URI and intent
     */
    private fun extractVideoTitle(context: Context, uri: Uri, intent: Intent): String {
        // Try to get title from intent extras
        intent.getStringExtra(Intent.EXTRA_TITLE)?.let { return it }
        intent.getStringExtra(Intent.EXTRA_SUBJECT)?.let { return it }
        intent.getStringExtra("video_title")?.let { return it }
        intent.getStringExtra("title")?.let { return it }
        
        // Try to get title from URI
        return when (uri.scheme) {
            "content" -> {
                // Try to query content resolver for display name
                try {
                    context.contentResolver.query(
                        uri,
                        arrayOf("_display_name", "title"),
                        null,
                        null,
                        null
                    )?.use { cursor ->
                        if (cursor.moveToFirst()) {
                            val displayNameIndex = cursor.getColumnIndex("_display_name")
                            val titleIndex = cursor.getColumnIndex("title")
                            
                            when {
                                displayNameIndex >= 0 -> cursor.getString(displayNameIndex)
                                titleIndex >= 0 -> cursor.getString(titleIndex)
                                else -> null
                            }
                        } else null
                    } ?: extractTitleFromPath(uri.toString())
                } catch (e: Exception) {
                    Log.w(TAG, "Could not query content resolver for title", e)
                    extractTitleFromPath(uri.toString())
                }
            }
            "file" -> {
                val file = File(uri.path ?: "")
                file.nameWithoutExtension
            }
            "http", "https" -> {
                // Extract from URL
                extractTitleFromPath(uri.toString())
            }
            else -> extractTitleFromPath(uri.toString())
        }
    }
    
    private fun extractTitleFromPath(path: String): String {
        return try {
            val fileName = path.substringAfterLast('/').substringAfterLast('\\')
            val nameWithoutExtension = fileName.substringBeforeLast('.')
            
            if (nameWithoutExtension.isNotEmpty()) {
                // Clean up the title
                nameWithoutExtension
                    .replace(Regex("[._-]"), " ")
                    .replace(Regex("\\s+"), " ")
                    .trim()
                    .takeIf { it.isNotEmpty() } ?: "Unknown Video"
            } else {
                "Unknown Video"
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not extract title from path: $path", e)
            "Unknown Video"
        }
    }
    
    private fun getFileExtension(path: String): String {
        return try {
            path.substringAfterLast('.', "")
        } catch (e: Exception) {
            ""
        }
    }
    
    /**
     * Start video player with single video
     */
    private fun startVideoPlayer(context: Context, uri: Uri, title: String, originalIntent: Intent) {
        val playerIntent = Intent(context, com.astralplayer.nextplayer.VideoPlayerActivity::class.java).apply {
            data = uri
            putExtra("video_title", title)
            putExtra("from_external", true)
            
            // Copy relevant extras from original intent
            originalIntent.extras?.let { extras ->
                putExtras(extras)
            }
            
            // Handle special playback modes
            if (isAdultContent(uri, title)) {
                putExtra("adult_content_mode", true)
                putExtra("enhanced_codec_support", true)
            }
            
            if (isStreamingContent(uri)) {
                putExtra("streaming_mode", true)
                putExtra("network_buffering", true)
            }
            
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        
        Log.d(TAG, "Starting video player for: $title")
        context.startActivity(playerIntent)
    }
    
    /**
     * Start video player with playlist
     */
    private fun startVideoPlayerWithPlaylist(context: Context, uris: List<Uri>, originalIntent: Intent) {
        val playlistVideos = uris.mapIndexed { index, uri ->
            android.os.Bundle().apply {
                putString("uri", uri.toString())
                putString("title", extractVideoTitle(context, uri, originalIntent))
                putLong("duration", 0L) // Will be determined during playback
            }
        }
        
        val playerIntent = Intent(context, com.astralplayer.nextplayer.VideoPlayerActivity::class.java).apply {
            data = uris.first() // Start with first video
            putExtra("video_title", extractVideoTitle(context, uris.first(), originalIntent))
            putExtra("playlist_mode", true)
            putParcelableArrayListExtra("playlist_videos", ArrayList(playlistVideos))
            putExtra("from_external", true)
            
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        
        Log.d(TAG, "Starting video player with playlist of ${uris.size} videos")
        context.startActivity(playerIntent)
    }
    
    /**
     * Check if content is likely adult content based on URI and title
     */
    private fun isAdultContent(uri: Uri, title: String): Boolean {
        val path = uri.path?.lowercase() ?: ""
        val titleLower = title.lowercase()
        
        // Basic heuristics - can be enhanced with more sophisticated detection
        val adultIndicators = listOf(
            "adult", "xxx", "porn", "nsfw", "mature", "18+", "explicit"
        )
        
        return adultIndicators.any { indicator ->
            path.contains(indicator) || titleLower.contains(indicator)
        }
    }
    
    /**
     * Check if content is streaming content
     */
    private fun isStreamingContent(uri: Uri): Boolean {
        val scheme = uri.scheme?.lowercase()
        val path = uri.path?.lowercase() ?: ""
        
        return scheme in setOf("http", "https", "rtmp", "rtsp", "mms", "mmsh") ||
               STREAMING_EXTENSIONS.any { path.endsWith(".$it") }
    }
    
    /**
     * Create sharing intent for video file
     */
    fun createShareIntent(context: Context, videoUri: Uri, title: String): Intent {
        return Intent(Intent.ACTION_SEND).apply {
            type = "video/*"
            putExtra(Intent.EXTRA_STREAM, videoUri)
            putExtra(Intent.EXTRA_TITLE, title)
            putExtra(Intent.EXTRA_SUBJECT, title)
        }
    }
    
    /**
     * Create intent to open video with external app
     */
    fun createOpenWithIntent(context: Context, videoUri: Uri, title: String): Intent {
        return Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(videoUri, "video/*")
            putExtra("video_title", title)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
    
    /**
     * Get MIME type for URI
     */
    fun getMimeType(context: Context, uri: Uri): String? {
        return when (uri.scheme) {
            "content" -> context.contentResolver.getType(uri)
            "file" -> {
                val extension = getFileExtension(uri.path ?: "")
                MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.lowercase())
            }
            else -> null
        }
    }
    
    /**
     * Create file provider URI for sharing
     */
    fun createFileProviderUri(context: Context, file: File): Uri {
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }
    
    /**
     * Log intent details for debugging
     */
    fun logIntentDetails(intent: Intent, tag: String = TAG) {
        Log.d(tag, "=== Intent Details ===")
        Log.d(tag, "Action: ${intent.action}")
        Log.d(tag, "Data: ${intent.data}")
        Log.d(tag, "Type: ${intent.type}")
        Log.d(tag, "Scheme: ${intent.scheme}")
        Log.d(tag, "Categories: ${intent.categories}")
        
        intent.extras?.let { extras ->
            Log.d(tag, "Extras:")
            for (key in extras.keySet()) {
                val value = extras.get(key)
                Log.d(tag, "  $key: $value (${value?.javaClass?.simpleName})")
            }
        }
        Log.d(tag, "==================")
    }
}