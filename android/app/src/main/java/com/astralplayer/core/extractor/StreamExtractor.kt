package com.astralplayer.core.extractor

import android.content.Context
import android.webkit.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import timber.log.Timber
import java.util.regex.Pattern
import kotlin.coroutines.resume
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StreamExtractor @Inject constructor(
    private val context: Context,
    private val okHttpClient: OkHttpClient
) {
    
    companion object {
        private const val EXTRACTION_TIMEOUT = 15000L // 15 seconds
        
        // Video URL patterns for different sites
        private val VIDEO_PATTERNS = listOf(
            // Generic video URLs
            Pattern.compile("\"(https?://[^\"]*\\.mp4[^\"]*?)\""),
            Pattern.compile("\"(https?://[^\"]*\\.m3u8[^\"]*?)\""),
            Pattern.compile("\"(https?://[^\"]*\\.mpd[^\"]*?)\""),
            Pattern.compile("\"(https?://[^\"]*\\.webm[^\"]*?)\""),
            
            // HTML5 video sources
            Pattern.compile("src\\s*=\\s*[\"']([^\"']*\\.(mp4|m3u8|mpd|webm)[^\"']*)"),
            Pattern.compile("source\\s*:\\s*[\"']([^\"']*\\.(mp4|m3u8|mpd|webm)[^\"']*)"),
            Pattern.compile("file\\s*:\\s*[\"']([^\"']*\\.(mp4|m3u8|mpd|webm)[^\"']*)"),
            
            // Common video player configurations
            Pattern.compile("video_url\\s*:\\s*[\"']([^\"']*)[\"']"),
            Pattern.compile("videoUrl\\s*:\\s*[\"']([^\"']*)[\"']"),
            Pattern.compile("contentUrl\\s*\"\\s*:\\s*\"([^\"]+)\""),
            Pattern.compile("\"url\"\\s*:\\s*\"([^\"]*\\.(mp4|m3u8|mpd|webm)[^\"]*)\""),
            
            // Adult site specific patterns
            Pattern.compile("flashvars_\\d+\\s*=\\s*\\{[^}]*['\"]video_url['\"]\\s*:\\s*['\"]([^'\"]*)['\"]"),
            Pattern.compile("player_quality_\\d+p['\"]\\s*:\\s*['\"]([^'\"]*)['\"]"),
            Pattern.compile("\"(https?://[^\"]*\\/(get_media|player_api|embed)[^\"]*?)\""),
            
            // HLS/DASH manifests
            Pattern.compile("\"(https?://[^\"]*master\\.m3u8[^\"]*?)\""),
            Pattern.compile("\"(https?://[^\"]*playlist\\.m3u8[^\"]*?)\""),
            Pattern.compile("\"(https?://[^\"]*manifest\\.mpd[^\"]*?)\""),
            
            // JavaScript variable assignments
            Pattern.compile("var\\s+\\w*[Uu]rl\\s*=\\s*[\"']([^\"']*\\.(mp4|m3u8|mpd|webm)[^\"']*)[\"']"),
            Pattern.compile("\\w*[Ss]ource\\s*=\\s*[\"']([^\"']*\\.(mp4|m3u8|mpd|webm)[^\"']*)[\"']")
        )
        
        // Patterns for JSON-LD structured data
        private val JSON_LD_PATTERNS = listOf(
            "contentUrl",
            "embedUrl", 
            "url",
            "videoUrl",
            "streamUrl"
        )
    }
    
    suspend fun extractVideoUrl(
        pageUrl: String,
        cookies: Map<String, String> = emptyMap(),
        headers: Map<String, String> = emptyMap()
    ): String? = withContext(Dispatchers.IO) {
        try {
            Timber.d("Extracting video URL from: $pageUrl")
            
            // First try direct extraction from HTML
            val directUrl = extractFromHtml(pageUrl, cookies, headers)
            if (directUrl != null) {
                Timber.d("Direct extraction successful: $directUrl")
                return@withContext directUrl
            }
            
            // If that fails, use WebView for JavaScript execution
            Timber.d("Falling back to WebView extraction")
            withContext(Dispatchers.Main) {
                extractWithWebView(pageUrl, cookies, headers)
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to extract video URL from: $pageUrl")
            null
        }
    }
    
    private suspend fun extractFromHtml(
        pageUrl: String,
        cookies: Map<String, String>,
        headers: Map<String, String>
    ): String? {
        try {
            val request = Request.Builder()
                .url(pageUrl)
                .apply {
                    // Add custom headers
                    headers.forEach { (key, value) -> addHeader(key, value) }
                    
                    // Add cookies as header
                    if (cookies.isNotEmpty()) {
                        val cookieHeader = cookies.entries.joinToString("; ") { "${it.key}=${it.value}" }
                        addHeader("Cookie", cookieHeader)
                    }
                    
                    // Add standard headers
                    addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8")
                    addHeader("Accept-Language", "en-US,en;q=0.5")
                    addHeader("Accept-Encoding", "gzip, deflate")
                    addHeader("DNT", "1")
                    addHeader("Connection", "keep-alive")
                    addHeader("Sec-Fetch-Dest", "document")
                    addHeader("Sec-Fetch-Mode", "navigate")
                    addHeader("Sec-Fetch-Site", "none")
                    addHeader("Upgrade-Insecure-Requests", "1")
                }
                .build()
            
            val response = okHttpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                Timber.w("HTTP request failed: ${response.code}")
                return null
            }
            
            val html = response.body?.string() ?: return null
            Timber.d("Downloaded HTML content: ${html.length} characters")
            
            // Try all regex patterns
            for (pattern in VIDEO_PATTERNS) {
                val matcher = pattern.matcher(html)
                while (matcher.find()) {
                    val url = matcher.group(1)
                    if (url != null && isValidVideoUrl(url)) {
                        Timber.d("Found video URL with pattern: $url")
                        return url
                    }
                }
            }
            
            // Try to find JSON-LD structured data
            val videoUrl = extractFromJsonLd(html)
            if (videoUrl != null) {
                Timber.d("Found video URL in JSON-LD: $videoUrl")
                return videoUrl
            }
            
            // Try to find video URLs in script tags
            val scriptUrl = extractFromScripts(html)
            if (scriptUrl != null) {
                Timber.d("Found video URL in scripts: $scriptUrl")
                return scriptUrl
            }
            
        } catch (e: Exception) {
            Timber.e(e, "Error extracting from HTML")
        }
        
        return null
    }
    
    private fun extractFromJsonLd(html: String): String? {
        val jsonLdPattern = Pattern.compile("<script[^>]*type=\"application/ld\\+json\"[^>]*>([^<]+)</script>", Pattern.CASE_INSENSITIVE)
        val jsonMatcher = jsonLdPattern.matcher(html)
        
        while (jsonMatcher.find()) {
            try {
                val jsonContent = jsonMatcher.group(1)
                val json = JSONObject(jsonContent)
                
                // Try different JSON-LD properties
                for (property in JSON_LD_PATTERNS) {
                    val url = json.optString(property)
                    if (url.isNotEmpty() && isValidVideoUrl(url)) {
                        return url
                    }
                }
                
                // Check nested objects
                if (json.has("video")) {
                    val videoObj = json.getJSONObject("video")
                    for (property in JSON_LD_PATTERNS) {
                        val url = videoObj.optString(property)
                        if (url.isNotEmpty() && isValidVideoUrl(url)) {
                            return url
                        }
                    }
                }
                
            } catch (e: Exception) {
                Timber.w("Failed to parse JSON-LD: ${e.message}")
            }
        }
        
        return null
    }
    
    private fun extractFromScripts(html: String): String? {
        // Extract all script tag contents
        val scriptPattern = Pattern.compile("<script[^>]*>([^<]*)</script>", Pattern.CASE_INSENSITIVE or Pattern.DOTALL)
        val scriptMatcher = scriptPattern.matcher(html)
        
        while (scriptMatcher.find()) {
            val scriptContent = scriptMatcher.group(1)
            
            // Apply video patterns to script content
            for (pattern in VIDEO_PATTERNS) {
                val matcher = pattern.matcher(scriptContent)
                while (matcher.find()) {
                    val url = matcher.group(1)
                    if (url != null && isValidVideoUrl(url)) {
                        return url
                    }
                }
            }
        }
        
        return null
    }
    
    private suspend fun extractWithWebView(
        pageUrl: String,
        cookies: Map<String, String>,
        headers: Map<String, String>
    ): String? = withTimeout(EXTRACTION_TIMEOUT) {
        suspendCancellableCoroutine { continuation ->
            var webView: WebView? = null
            
            try {
                webView = WebView(context).apply {
                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        mediaPlaybackRequiresUserGesture = false
                        mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                        cacheMode = WebSettings.LOAD_NO_CACHE
                        userAgentString = headers["User-Agent"] ?: settings.userAgentString
                    }
                    
                    // Set cookies
                    val cookieManager = CookieManager.getInstance()
                    cookieManager.setAcceptCookie(true)
                    cookieManager.setAcceptThirdPartyCookies(this, true)
                    
                    cookies.forEach { (key, value) ->
                        cookieManager.setCookie(pageUrl, "$key=$value")
                    }
                    
                    // Inject JavaScript to capture video URLs
                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            
                            val js = """
                                (function() {
                                    var foundUrls = [];
                                    
                                    // Check video elements
                                    var videos = document.getElementsByTagName('video');
                                    for (var i = 0; i < videos.length; i++) {
                                        var src = videos[i].src || videos[i].currentSrc;
                                        if (src) foundUrls.push(src);
                                        
                                        // Check source elements
                                        var sources = videos[i].getElementsByTagName('source');
                                        for (var j = 0; j < sources.length; j++) {
                                            if (sources[j].src) foundUrls.push(sources[j].src);
                                        }
                                    }
                                    
                                    // Check common video players
                                    if (typeof jwplayer !== 'undefined' && jwplayer().getPlaylist) {
                                        try {
                                            var playlist = jwplayer().getPlaylist();
                                            if (playlist && playlist[0] && playlist[0].file) {
                                                foundUrls.push(playlist[0].file);
                                            }
                                        } catch(e) {}
                                    }
                                    
                                    if (typeof videojs !== 'undefined' && videojs.players) {
                                        for (var key in videojs.players) {
                                            try {
                                                var player = videojs.players[key];
                                                if (player && player.src) {
                                                    var src = player.src();
                                                    if (typeof src === 'string') foundUrls.push(src);
                                                    else if (src && src.src) foundUrls.push(src.src);
                                                }
                                            } catch(e) {}
                                        }
                                    }
                                    
                                    // Check window.player
                                    if (window.player && typeof window.player.src === 'function') {
                                        try {
                                            var src = window.player.src();
                                            if (src) foundUrls.push(src);
                                        } catch(e) {}
                                    }
                                    
                                    // Search for video URLs in window object
                                    function searchObject(obj, maxDepth) {
                                        if (maxDepth <= 0) return;
                                        try {
                                            for (var key in obj) {
                                                var value = obj[key];
                                                if (typeof value === 'string' && 
                                                    (value.includes('.mp4') || value.includes('.m3u8') || value.includes('.mpd'))) {
                                                    foundUrls.push(value);
                                                } else if (typeof value === 'object' && value !== null) {
                                                    searchObject(value, maxDepth - 1);
                                                }
                                            }
                                        } catch(e) {}
                                    }
                                    
                                    searchObject(window, 2);
                                    
                                    // Return the first valid URL found
                                    for (var k = 0; k < foundUrls.length; k++) {
                                        if (foundUrls[k] && foundUrls[k].startsWith('http')) {
                                            console.log('ASTRAL_VIDEO_URL:' + foundUrls[k]);
                                            break;
                                        }
                                    }
                                })();
                            """.trimIndent()
                            
                            view?.evaluateJavascript(js) { /* ignore result */ }
                        }
                        
                        override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                            super.onReceivedError(view, request, error)
                            Timber.w("WebView error: ${error?.description}")
                        }
                    }
                    
                    webChromeClient = object : WebChromeClient() {
                        override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                            val message = consoleMessage?.message() ?: return false
                            if (message.startsWith("ASTRAL_VIDEO_URL:")) {
                                val url = message.substring(17)
                                if (isValidVideoUrl(url)) {
                                    Timber.d("WebView found video URL: $url")
                                    if (!continuation.isCompleted) {
                                        continuation.resume(url)
                                    }
                                }
                            }
                            return true
                        }
                    }
                    
                    // Load the page with custom headers
                    val additionalHeaders = headers.toMutableMap()
                    loadUrl(pageUrl, additionalHeaders)
                }
                
                continuation.invokeOnCancellation {
                    webView?.destroy()
                }
                
            } catch (e: Exception) {
                Timber.e(e, "WebView extraction error")
                webView?.destroy()
                if (!continuation.isCompleted) {
                    continuation.resume(null)
                }
            }
        }
    }
    
    private fun isValidVideoUrl(url: String): Boolean {
        if (url.length < 10 || !url.startsWith("http")) return false
        
        val cleanUrl = url.trim()
        return (cleanUrl.contains(".mp4") || 
                cleanUrl.contains(".m3u8") || 
                cleanUrl.contains(".mpd") || 
                cleanUrl.contains(".webm") ||
                cleanUrl.contains("/playlist/") ||
                cleanUrl.contains("/manifest/") || 
                cleanUrl.contains("/stream/") ||
                cleanUrl.contains("video") ||
                cleanUrl.contains("media")) &&
                !cleanUrl.contains("thumb") &&
                !cleanUrl.contains("preview") &&
                !cleanUrl.contains("poster")
    }
    
    fun extractFromDirectUrl(url: String): String? {
        return if (isValidVideoUrl(url)) url else null
    }
}