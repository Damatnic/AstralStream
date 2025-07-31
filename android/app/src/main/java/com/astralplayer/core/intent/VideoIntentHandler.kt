package com.astralplayer.core.intent

import android.content.Intent
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.media3.common.MimeTypes
import com.astralplayer.core.codec.CodecManager.StreamType
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VideoIntentHandler @Inject constructor() {
    
    data class VideoInfo(
        val uri: Uri,
        val title: String,
        val mimeType: String?,
        val referrer: String?,
        val userAgent: String?,
        val headers: Map<String, String>,
        val isStreaming: Boolean,
        val streamType: StreamType,
        val isAdultContent: Boolean,
        val requiresSpecialHandling: Boolean
    )
    
    fun extractVideoInfo(intent: Intent): VideoInfo {
        val uri = intent.data ?: throw IllegalArgumentException("No URI provided in intent")
        
        val title = extractTitle(intent, uri)
        val mimeType = extractMimeType(intent, uri)
        val referrer = extractReferrer(intent)
        val userAgent = extractUserAgent(intent)
        val headers = extractHeaders(intent)
        
        val isStreaming = isStreamingUrl(uri)
        val streamType = determineStreamType(uri, mimeType)
        val isAdultContent = detectAdultContent(uri, referrer)
        val requiresSpecialHandling = requiresSpecialHandling(uri, headers)
        
        Timber.d("Extracted video info: $title, streaming: $isStreaming, adult: $isAdultContent")
        
        return VideoInfo(
            uri = uri,
            title = title,
            mimeType = mimeType,
            referrer = referrer,
            userAgent = userAgent,
            headers = headers,
            isStreaming = isStreaming,
            streamType = streamType,
            isAdultContent = isAdultContent,
            requiresSpecialHandling = requiresSpecialHandling
        )
    }
    
    private fun extractTitle(intent: Intent, uri: Uri): String {
        // Try to get title from intent extras
        intent.getStringExtra(Intent.EXTRA_TITLE)?.let { return it }
        intent.getStringExtra("android.intent.extra.TITLE")?.let { return it }
        intent.getStringExtra("title")?.let { return it }
        
        // Extract from URI
        uri.lastPathSegment?.let { segment ->
            val fileName = segment.substringBeforeLast('.')
            if (fileName.isNotBlank()) return fileName
        }
        
        // Extract from query parameters
        uri.getQueryParameter("title")?.let { return it }
        uri.getQueryParameter("v")?.let { return "Video $it" }
        
        return "Unknown Video"
    }
    
    private fun extractMimeType(intent: Intent, uri: Uri): String? {
        // Try intent type first
        intent.type?.let { return it }
        
        // Try to determine from URI
        val extension = MimeTypeMap.getFileExtensionFromUrl(uri.toString())
        if (extension.isNotEmpty()) {
            MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)?.let { return it }
        }
        
        // Common video extensions
        return when (uri.toString().substringAfterLast('.').lowercase()) {
            "mp4", "m4v" -> MimeTypes.VIDEO_MP4
            "mkv" -> "video/x-matroska"
            "avi" -> "video/x-msvideo"
            "mov" -> "video/quicktime"
            "wmv" -> "video/x-ms-wmv"
            "flv" -> "video/x-flv"
            "webm" -> MimeTypes.VIDEO_WEBM
            "m3u8" -> MimeTypes.APPLICATION_M3U8
            "mpd" -> MimeTypes.APPLICATION_MPD
            else -> null
        }
    }
    
    private fun extractReferrer(intent: Intent): String? {
        return intent.getStringExtra("android.intent.extra.REFERRER")
            ?: intent.getStringExtra("referrer")
            ?: intent.getStringExtra("Referer")
    }
    
    private fun extractUserAgent(intent: Intent): String? {
        return intent.getStringExtra("user-agent")
            ?: intent.getStringExtra("User-Agent")
    }
    
    private fun extractHeaders(intent: Intent): Map<String, String> {
        val headers = mutableMapOf<String, String>()
        
        // Extract common headers from intent extras
        intent.getStringExtra("cookie")?.let { headers["Cookie"] = it }
        intent.getStringExtra("authorization")?.let { headers["Authorization"] = it }
        extractUserAgent(intent)?.let { headers["User-Agent"] = it }
        extractReferrer(intent)?.let { headers["Referer"] = it }
        
        // Extract headers bundle if present
        intent.getBundleExtra("headers")?.let { bundle ->
            for (key in bundle.keySet()) {
                bundle.getString(key)?.let { value ->
                    headers[key] = value
                }
            }
        }
        
        return headers
    }
    
    private fun isStreamingUrl(uri: Uri): Boolean {
        val uriString = uri.toString().lowercase()
        
        return when {
            uriString.contains(".m3u8") -> true
            uriString.contains(".mpd") -> true
            uriString.contains("manifest") -> true
            uri.scheme in listOf("rtsp", "rtmp", "mms") -> true
            uriString.contains("livestream") -> true
            uriString.contains("stream") && uriString.contains("http") -> true
            else -> false
        }
    }
    
    private fun determineStreamType(uri: Uri, mimeType: String?): StreamType {
        val uriString = uri.toString().lowercase()
        
        return StreamType(
            isHLS = uriString.contains(".m3u8") || mimeType == MimeTypes.APPLICATION_M3U8,
            isDASH = uriString.contains(".mpd") || mimeType == MimeTypes.APPLICATION_MPD,
            isRTMP = uri.scheme == "rtmp",
            isAdultContent = detectAdultContent(uri, null),
            requiresSpecialHandling = requiresSpecialHandling(uri, emptyMap())
        )
    }
    
    private fun detectAdultContent(uri: Uri, referrer: String?): Boolean {
        val uriString = uri.toString().lowercase()
        val referrerString = referrer?.lowercase() ?: ""
        
        val adultIndicators = listOf(
            "adult", "xxx", "porn", "sex", "nsfw", "18+", "mature",
            "xhamster", "pornhub", "xvideos", "redtube", "youporn",
            "tube8", "xtube", "spankbang", "xnxx", "beeg"
        )
        
        return adultIndicators.any { indicator ->
            uriString.contains(indicator) || referrerString.contains(indicator)
        }
    }
    
    private fun requiresSpecialHandling(uri: Uri, headers: Map<String, String>): Boolean {
        return when {
            headers.containsKey("Authorization") -> true
            headers.containsKey("Cookie") -> true
            uri.userInfo != null -> true
            uri.toString().contains("token") -> true
            uri.toString().contains("auth") -> true
            else -> false
        }
    }
    
    fun createVideoUri(urlString: String, headers: Map<String, String> = emptyMap()): Uri {
        return Uri.parse(urlString)
    }
    
    fun isVideoIntent(intent: Intent): Boolean {
        return when (intent.action) {
            Intent.ACTION_VIEW -> {
                val mimeType = intent.type
                val uri = intent.data
                
                when {
                    mimeType?.startsWith("video/") == true -> true
                    mimeType in listOf(
                        MimeTypes.APPLICATION_M3U8,
                        MimeTypes.APPLICATION_MPD,
                        "application/mp4"
                    ) -> true
                    uri?.let { isVideoUri(it) } == true -> true
                    else -> false
                }
            }
            Intent.ACTION_SEND -> {
                intent.type?.startsWith("video/") == true ||
                intent.type == "text/plain" && intent.getStringExtra(Intent.EXTRA_TEXT)?.let {
                    isVideoUri(Uri.parse(it))
                } == true
            }
            else -> false
        }
    }
    
    private fun isVideoUri(uri: Uri): Boolean {
        val path = uri.toString().lowercase()
        val videoExtensions = listOf(
            ".mp4", ".mkv", ".avi", ".mov", ".wmv", ".flv", ".webm",
            ".m4v", ".3gp", ".3g2", ".m3u8", ".mpd"
        )
        
        return videoExtensions.any { path.contains(it) } || 
               uri.scheme in listOf("rtsp", "rtmp", "mms")
    }
}