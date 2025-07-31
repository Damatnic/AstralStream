package com.astralplayer.core.intent

import android.content.Intent
import android.net.Uri
import android.webkit.CookieManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URLDecoder

class VideoIntentHandler {
    
    data class VideoInfo(
        val uri: Uri,
        val originalUrl: String? = null,
        val title: String? = null,
        val referrer: String? = null,
        val cookies: Map<String, String> = emptyMap(),
        val headers: Map<String, String> = emptyMap(),
        val isStreaming: Boolean = false,
        val streamType: StreamType = StreamType.PROGRESSIVE,
        val isAdultContent: Boolean = false,
        val requiresExtraction: Boolean = false
    )
    
    enum class StreamType {
        PROGRESSIVE, HLS, DASH, SMOOTH, RTMP, RTSP
    }
    
    suspend fun extractVideoInfo(intent: Intent): VideoInfo = withContext(Dispatchers.IO) {
        val uri = intent.data ?: intent.getParcelableExtra(Intent.EXTRA_STREAM) 
            ?: throw IllegalArgumentException("No URI found in intent")
        
        // Extract all metadata
        val originalUrl = intent.getStringExtra("original_url") ?: uri.toString()
        val title = extractTitle(intent, uri)
        val referrer = extractReferrer(intent)
        val cookies = extractCookies(intent, uri)
        val headers = extractHeaders(intent)
        
        // Detect if it's adult content
        val isAdultContent = detectAdultContent(uri, referrer)
        
        // Detect stream type
        val streamType = detectStreamType(uri)
        val isStreaming = streamType != StreamType.PROGRESSIVE
        
        // Check if needs extraction (embedded videos)
        val requiresExtraction = needsExtraction(uri)
        
        VideoInfo(
            uri = uri,
            originalUrl = originalUrl,
            title = title,
            referrer = referrer,
            cookies = cookies,
            headers = headers,
            isStreaming = isStreaming,
            streamType = streamType,
            isAdultContent = isAdultContent,
            requiresExtraction = requiresExtraction
        )
    }
    
    private fun extractTitle(intent: Intent, uri: Uri): String? {
        // Try intent extras first
        intent.getStringExtra(Intent.EXTRA_TITLE)?.let { return it }
        intent.getStringExtra("title")?.let { return it }
        intent.getStringExtra("android.intent.extra.TITLE")?.let { return it }
        
        // Extract from URI
        uri.lastPathSegment?.let { segment ->
            val fileName = segment.substringBeforeLast('.')
            if (fileName.isNotBlank()) return fileName
        }
        
        // Extract from query parameters
        uri.getQueryParameter("title")?.let { return URLDecoder.decode(it, "UTF-8") }
        uri.getQueryParameter("v")?.let { return "Video $it" }
        
        return null
    }
    
    private fun extractReferrer(intent: Intent): String? {
        return intent.getStringExtra("android.intent.extra.REFERRER")
            ?: intent.getStringExtra("referrer")
            ?: intent.getStringExtra("Referer")
            ?: intent.getStringExtra("referer")
    }
    
    private fun extractCookies(intent: Intent, uri: Uri): Map<String, String> {
        val cookies = mutableMapOf<String, String>()
        
        // Get cookies from intent extras
        intent.getStringExtra("cookie")?.let {
            cookies.putAll(parseCookieString(it))
        }
        
        // Get cookies from CookieManager
        CookieManager.getInstance().getCookie(uri.toString())?.let {
            cookies.putAll(parseCookieString(it))
        }
        
        // Check all intent extras for cookie-like data
        intent.extras?.keySet()?.forEach { key ->
            when {
                key.contains("cookie", ignoreCase = true) ||
                key.contains("session", ignoreCase = true) ||
                key.contains("auth", ignoreCase = true) -> {
                    intent.getStringExtra(key)?.let { value ->
                        if (key.contains("cookie", ignoreCase = true)) {
                            cookies.putAll(parseCookieString(value))
                        } else {
                            cookies[key] = value
                        }
                    }
                }
            }
        }
        
        return cookies
    }
    
    private fun extractHeaders(intent: Intent): Map<String, String> {
        val headers = mutableMapOf<String, String>()
        
        // Standard headers
        headers["User-Agent"] = intent.getStringExtra("user_agent") 
            ?: "Mozilla/5.0 (Linux; Android ${android.os.Build.VERSION.RELEASE}) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
        
        intent.getStringExtra("referer")?.let { headers["Referer"] = it }
        intent.getStringExtra("origin")?.let { headers["Origin"] = it }
        
        // Extract all header-like extras
        intent.extras?.keySet()?.forEach { key ->
            if (key.startsWith("http_", ignoreCase = true) || 
                key.contains("header", ignoreCase = true)) {
                intent.getStringExtra(key)?.let { headers[key] = it }
            }
        }
        
        // Add required headers for video streaming
        headers["Accept"] = "video/webm,video/ogg,video/*;q=0.9,application/x-mpegURL,application/vnd.apple.mpegurl,application/dash+xml,*/*;q=0.8"
        headers["Accept-Language"] = "en-US,en;q=0.9"
        headers["Cache-Control"] = "no-cache"
        headers["Connection"] = "keep-alive"
        headers["Sec-Fetch-Dest"] = "video"
        headers["Sec-Fetch-Mode"] = "no-cors"
        headers["Sec-Fetch-Site"] = "same-origin"
        
        return headers
    }
    
    private fun detectStreamType(uri: Uri): StreamType {
        val uriString = uri.toString().lowercase()
        val path = uri.path?.lowercase() ?: ""
        
        return when {
            uriString.contains(".m3u8") || path.endsWith(".m3u8") -> StreamType.HLS
            uriString.contains(".mpd") || path.endsWith(".mpd") -> StreamType.DASH
            uriString.contains(".ism") || path.contains(".ism") -> StreamType.SMOOTH
            uri.scheme == "rtmp" -> StreamType.RTMP
            uri.scheme == "rtsp" -> StreamType.RTSP
            uriString.contains("manifest") && uriString.contains("dash") -> StreamType.DASH
            uriString.contains("manifest") && uriString.contains("hls") -> StreamType.HLS
            uriString.contains("playlist") -> StreamType.HLS
            else -> StreamType.PROGRESSIVE
        }
    }
    
    private fun detectAdultContent(uri: Uri, referrer: String?): Boolean {
        val adultDomains = listOf(
            "pornhub", "xvideos", "xhamster", "spankbang", "redtube",
            "youporn", "tube8", "xnxx", "porn", "xxx", "adult", "beeg",
            "xtube", "eporner", "xmovies", "chaturbate", "cam4",
            "onlyfans", "manyvids", "clips4sale"
        )
        
        val urlString = uri.toString().lowercase()
        val referrerString = referrer?.lowercase() ?: ""
        val host = uri.host?.lowercase() ?: ""
        
        return adultDomains.any { domain ->
            urlString.contains(domain) || 
            referrerString.contains(domain) ||
            host.contains(domain)
        }
    }
    
    private fun needsExtraction(uri: Uri): Boolean {
        val path = uri.path?.lowercase() ?: ""
        val query = uri.query?.lowercase() ?: ""
        
        // Direct video files don't need extraction
        val directVideoExtensions = listOf(".mp4", ".m3u8", ".mpd", ".webm", ".mkv", ".avi", ".mov")
        if (directVideoExtensions.any { path.endsWith(it) }) {
            return false
        }
        
        // URLs that typically need extraction
        return path.contains("/watch") || 
               path.contains("/video/") ||
               path.contains("/embed/") ||
               path.contains("/player/") ||
               path.contains("/view/") ||
               query.contains("v=") ||
               query.contains("video=") ||
               query.contains("id=")
    }
    
    private fun parseCookieString(cookieString: String): Map<String, String> {
        return cookieString.split(";")
            .map { it.trim() }
            .filter { it.contains("=") }
            .associate {
                val parts = it.split("=", limit = 2)
                if (parts.size == 2) {
                    parts[0].trim() to parts[1].trim()
                } else {
                    parts[0].trim() to ""
                }
            }
    }
    
    fun isVideoIntent(intent: Intent): Boolean {
        return when (intent.action) {
            Intent.ACTION_VIEW -> {
                val mimeType = intent.type
                val uri = intent.data
                
                when {
                    mimeType?.startsWith("video/") == true -> true
                    mimeType in listOf(
                        "application/x-mpegURL",
                        "application/vnd.apple.mpegurl",
                        "application/dash+xml",
                        "application/mp4"
                    ) -> true
                    uri?.let { isVideoUri(it) } == true -> true
                    else -> false
                }
            }
            Intent.ACTION_SEND -> {
                intent.type?.startsWith("video/") == true ||
                (intent.type == "text/plain" && intent.getStringExtra(Intent.EXTRA_TEXT)?.let {
                    isVideoUri(Uri.parse(it))
                } == true)
            }
            else -> false
        }
    }
    
    private fun isVideoUri(uri: Uri): Boolean {
        val path = uri.toString().lowercase()
        val videoExtensions = listOf(
            ".mp4", ".mkv", ".avi", ".mov", ".wmv", ".flv", ".webm",
            ".m4v", ".3gp", ".3g2", ".m3u8", ".mpd", ".ts", ".m2ts"
        )
        
        val videoKeywords = listOf(
            "video", "watch", "play", "stream", "media", "player",
            "embed", "view", "movie", "clip"
        )
        
        return videoExtensions.any { path.contains(it) } || 
               videoKeywords.any { path.contains(it) } ||
               uri.scheme in listOf("rtsp", "rtmp", "mms")
    }
}