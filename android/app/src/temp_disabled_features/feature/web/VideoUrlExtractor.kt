package com.astralplayer.nextplayer.feature.web

import android.content.Context
import android.webkit.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import org.json.JSONObject
import java.net.URL
import java.net.URLDecoder
import android.util.Log
import android.os.Build

/**
 * Extracts video URLs from web pages
 */
class VideoUrlExtractor(private val context: Context) {
    
    companion object {
        private val VIDEO_URL_PATTERNS = listOf(
            Regex("https?://[^\"'\\s]+\\.(mp4|m3u8|webm|mkv)", RegexOption.IGNORE_CASE),
            Regex("https?://[^\"'\\s]+/video/[^\"'\\s]+", RegexOption.IGNORE_CASE),
            Regex("https?://[^\"'\\s]+\\.m3u8[^\"'\\s]*", RegexOption.IGNORE_CASE),
            Regex("blob:[^\"'\\s]+", RegexOption.IGNORE_CASE)
        )
        
        private val QUALITY_PATTERNS = mapOf(
            "2160p" to Regex("2160p|4k|uhd", RegexOption.IGNORE_CASE),
            "1080p" to Regex("1080p|fhd|full.*hd", RegexOption.IGNORE_CASE),
            "720p" to Regex("720p|hd", RegexOption.IGNORE_CASE),
            "480p" to Regex("480p|sd", RegexOption.IGNORE_CASE),
            "360p" to Regex("360p|low", RegexOption.IGNORE_CASE)
        )
    }
    
    data class VideoInfo(
        val url: String,
        val quality: String? = null,
        val type: VideoType = VideoType.PROGRESSIVE,
        val headers: Map<String, String> = emptyMap()
    )
    
    enum class VideoType {
        PROGRESSIVE, HLS, DASH
    }
    
    suspend fun extractVideoUrls(pageUrl: String): List<VideoInfo> = withContext(Dispatchers.IO) {
        try {
            val videoUrls = mutableListOf<VideoInfo>()
            val urlChannel = Channel<VideoInfo>()
            
            // Create a coroutine to collect URLs
            val collectJob = launch {
                try {
                    for (video in urlChannel) {
                        videoUrls.add(video)
                    }
                } catch (e: Exception) {
                    Log.e("VideoUrlExtractor", "Error collecting URLs", e)
                }
            }
            
            try {
                // Extract URLs using WebView on Main thread
                withContext(Dispatchers.Main) {
                    withTimeout(10000) { // 10 second timeout
                        extractWithWebView(pageUrl, urlChannel)
                    }
                }
            } catch (e: TimeoutCancellationException) {
                Log.e("VideoUrlExtractor", "WebView extraction timed out", e)
            } catch (e: Exception) {
                Log.e("VideoUrlExtractor", "Error during extraction", e)
            }
            
            // Wait for extraction to complete
            delay(5000) // Give WebView time to load
            urlChannel.close()
            
            try {
                collectJob.join()
            } catch (e: Exception) {
                Log.e("VideoUrlExtractor", "Error joining collect job", e)
            }
            
            // Sort by quality (highest first)
            videoUrls.sortedByDescending { video ->
                when (video.quality) {
                    "2160p" -> 4
                    "1080p" -> 3
                    "720p" -> 2
                    "480p" -> 1
                    else -> 0
                }
            }
        } catch (e: Exception) {
            Log.e("VideoUrlExtractor", "Fatal error extracting video URLs", e)
            emptyList()
        }
    }
    
    @Suppress("DEPRECATION")
    private suspend fun extractWithWebView(
        pageUrl: String,
        urlChannel: Channel<VideoInfo>
    ) = suspendCancellableCoroutine<Unit> { continuation ->
        
        val webView = WebView(context).apply {
            // Enable cookies
            val cookieManager = CookieManager.getInstance()
            cookieManager.setAcceptCookie(true)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                cookieManager.setAcceptThirdPartyCookies(this, true)
            }
            
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                databaseEnabled = true
                mediaPlaybackRequiresUserGesture = false
                mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                userAgentString = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
                cacheMode = WebSettings.LOAD_NO_CACHE
                allowFileAccess = true
                allowContentAccess = true
                blockNetworkImage = false
                loadsImagesAutomatically = true
                javaScriptCanOpenWindowsAutomatically = true
                setSupportMultipleWindows(true)
                loadWithOverviewMode = true
                useWideViewPort = true
                setSupportZoom(true)
                displayZoomControls = false
                builtInZoomControls = true
                setGeolocationEnabled(true)
                @Suppress("DEPRECATION")
                allowUniversalAccessFromFileURLs = true
                @Suppress("DEPRECATION")
                allowFileAccessFromFileURLs = true
            }
            
            // Add JavaScript interface for better extraction
            addJavascriptInterface(object {
                @JavascriptInterface
                fun onVideoFound(url: String, type: String?) {
                    if (isVideoUrl(url)) {
                        GlobalScope.launch {
                            urlChannel.send(analyzeVideoUrl(url))
                        }
                    }
                }
            }, "VideoExtractor")
            
            // Intercept network requests
            webViewClient = object : WebViewClient() {
                override fun shouldInterceptRequest(
                    view: WebView?,
                    request: WebResourceRequest?
                ): WebResourceResponse? {
                    request?.url?.toString()?.let { url ->
                        if (isVideoUrl(url)) {
                            val videoInfo = analyzeVideoUrl(url, request.requestHeaders)
                            GlobalScope.launch {
                                urlChannel.send(videoInfo)
                            }
                        }
                    }
                    return super.shouldInterceptRequest(view, request)
                }
                
                override fun onPageFinished(view: WebView?, url: String?) {
                    // Inject JavaScript to find video elements
                    view?.evaluateJavascript(getVideoFinderScript()) { result ->
                        try {
                            val urls = JSONObject(result).optJSONArray("videos")
                            for (i in 0 until (urls?.length() ?: 0)) {
                                val videoUrl = urls?.getString(i) ?: continue
                                if (isVideoUrl(videoUrl)) {
                                    GlobalScope.launch {
                                        urlChannel.send(analyzeVideoUrl(videoUrl))
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                    
                    // Complete after a delay
                    GlobalScope.launch {
                        delay(3000)
                        if (continuation.isActive) {
                            continuation.resume(Unit) {}
                        }
                    }
                }
            }
            
            // Add console message listener for debugging
            webChromeClient = object : WebChromeClient() {
                override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                    consoleMessage?.message()?.let { msg ->
                        if (msg.contains("video", ignoreCase = true)) {
                            // Parse potential video URLs from console
                            VIDEO_URL_PATTERNS.forEach { pattern ->
                                pattern.findAll(msg).forEach { match ->
                                    GlobalScope.launch {
                                        urlChannel.send(analyzeVideoUrl(match.value))
                                    }
                                }
                            }
                        }
                    }
                    return super.onConsoleMessage(consoleMessage)
                }
            }
            
            // Load the page
            loadUrl(pageUrl)
        }
        
        continuation.invokeOnCancellation {
            webView.destroy()
        }
    }
    
    private fun isVideoUrl(url: String): Boolean {
        return VIDEO_URL_PATTERNS.any { it.containsMatchIn(url) }
    }
    
    private fun analyzeVideoUrl(
        url: String,
        headers: Map<String, String> = emptyMap()
    ): VideoInfo {
        val decodedUrl = try {
            URLDecoder.decode(url, "UTF-8")
        } catch (e: Exception) {
            url
        }
        
        val quality = detectQuality(decodedUrl)
        val type = when {
            decodedUrl.contains(".m3u8", ignoreCase = true) -> VideoType.HLS
            decodedUrl.contains(".mpd", ignoreCase = true) -> VideoType.DASH
            else -> VideoType.PROGRESSIVE
        }
        
        return VideoInfo(
            url = url,
            quality = quality,
            type = type,
            headers = headers
        )
    }
    
    private fun detectQuality(url: String): String? {
        QUALITY_PATTERNS.forEach { (quality, pattern) ->
            if (pattern.containsMatchIn(url)) {
                return quality
            }
        }
        
        // Try to extract quality from URL parameters
        try {
            val urlObj = URL(url)
            val query = urlObj.query ?: return null
            
            // Common quality parameter names
            listOf("quality", "q", "res", "resolution").forEach { param ->
                val value = getQueryParameter(query, param)
                if (value != null) {
                    QUALITY_PATTERNS.forEach { (quality, pattern) ->
                        if (pattern.containsMatchIn(value)) {
                            return quality
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // Ignore URL parsing errors
        }
        
        return null
    }
    
    private fun getQueryParameter(query: String, name: String): String? {
        query.split("&").forEach { param ->
            val parts = param.split("=")
            if (parts.size == 2 && parts[0] == name) {
                return parts[1]
            }
        }
        return null
    }
    
    private fun getVideoFinderScript(): String {
        return """
            (function() {
                var videos = [];
                
                // Find HTML5 video elements
                document.querySelectorAll('video').forEach(function(video) {
                    if (video.src) {
                        videos.push(video.src);
                        if (window.VideoExtractor) {
                            window.VideoExtractor.onVideoFound(video.src, 'video');
                        }
                    }
                    video.querySelectorAll('source').forEach(function(source) {
                        if (source.src) {
                            videos.push(source.src);
                            if (window.VideoExtractor) {
                                window.VideoExtractor.onVideoFound(source.src, 'source');
                            }
                        }
                    });
                });
                
                // Monitor XMLHttpRequest for video URLs
                var open = XMLHttpRequest.prototype.open;
                XMLHttpRequest.prototype.open = function() {
                    this.addEventListener('load', function() {
                        var url = this.responseURL;
                        if (url && (url.includes('.m3u8') || url.includes('.mp4') || url.includes('.mpd'))) {
                            if (window.VideoExtractor) {
                                window.VideoExtractor.onVideoFound(url, 'xhr');
                            }
                        }
                    });
                    open.apply(this, arguments);
                };
                
                // Monitor fetch requests
                var originalFetch = window.fetch;
                window.fetch = function() {
                    return originalFetch.apply(this, arguments).then(function(response) {
                        var url = response.url;
                        if (url && (url.includes('.m3u8') || url.includes('.mp4') || url.includes('.mpd'))) {
                            if (window.VideoExtractor) {
                                window.VideoExtractor.onVideoFound(url, 'fetch');
                            }
                        }
                        return response;
                    });
                };
                
                // Find video URLs in scripts
                var scripts = document.querySelectorAll('script');
                scripts.forEach(function(script) {
                    var content = script.innerHTML;
                    var patterns = [
                        /https?:\/\/[^"'\s]+\.(mp4|m3u8|webm|mpd)/gi,
                        /https?:\/\/[^"'\s]+\/video\/[^"'\s]+/gi,
                        /https?:\/\/[^"'\s]+\/hls\/[^"'\s]+/gi,
                        /https?:\/\/[^"'\s]+\/dash\/[^"'\s]+/gi
                    ];
                    
                    patterns.forEach(function(pattern) {
                        var matches = content.match(pattern);
                        if (matches) {
                            matches.forEach(function(match) {
                                videos.push(match);
                                if (window.VideoExtractor) {
                                    window.VideoExtractor.onVideoFound(match, 'script');
                                }
                            });
                        }
                    });
                });
                
                // Find video URLs in data attributes
                document.querySelectorAll('[data-video-url], [data-src], [data-source], [data-video-src]').forEach(function(elem) {
                    ['data-video-url', 'data-src', 'data-source', 'data-video-src'].forEach(function(attr) {
                        var url = elem.getAttribute(attr);
                        if (url && (url.includes('.mp4') || url.includes('.m3u8') || url.includes('video'))) {
                            videos.push(url);
                            if (window.VideoExtractor) {
                                window.VideoExtractor.onVideoFound(url, 'data-attr');
                            }
                        }
                    });
                });
                
                // Monitor DOM mutations for dynamically added videos
                var observer = new MutationObserver(function(mutations) {
                    mutations.forEach(function(mutation) {
                        mutation.addedNodes.forEach(function(node) {
                            if (node.tagName === 'VIDEO' && node.src) {
                                if (window.VideoExtractor) {
                                    window.VideoExtractor.onVideoFound(node.src, 'mutation');
                                }
                            }
                        });
                    });
                });
                
                observer.observe(document.body, {
                    childList: true,
                    subtree: true
                });
                
                return JSON.stringify({ videos: [...new Set(videos)] });
            })();
        """.trimIndent()
    }
}

/**
 * Site-specific extractors for popular video sites
 */
object SiteSpecificExtractors {
    
    fun extractPornhubUrl(pageContent: String): List<VideoUrlExtractor.VideoInfo> {
        val urls = mutableListOf<VideoUrlExtractor.VideoInfo>()
        
        // Extract from flashvars
        val flashvarsPattern = Regex("flashvars_\\d+\\s*=\\s*\\{([^}]+)\\}", RegexOption.DOT_MATCHES_ALL)
        flashvarsPattern.find(pageContent)?.let { match ->
            val flashvars = match.groupValues[1]
            
            // Extract quality options
            val qualityPattern = Regex("\"quality_?(\\d+p?)\"\\s*:\\s*\"([^\"]+)\"")
            qualityPattern.findAll(flashvars).forEach { qualityMatch ->
                val quality = qualityMatch.groupValues[1]
                val encodedUrl = qualityMatch.groupValues[2]
                val decodedUrl = encodedUrl.replace("\\/", "/")
                
                urls.add(VideoUrlExtractor.VideoInfo(
                    url = decodedUrl,
                    quality = quality,
                    type = VideoUrlExtractor.VideoType.PROGRESSIVE
                ))
            }
        }
        
        return urls
    }
    
    fun extractXvideosUrl(pageContent: String): List<VideoUrlExtractor.VideoInfo> {
        val urls = mutableListOf<VideoUrlExtractor.VideoInfo>()
        
        // Extract HLS URL
        val hlsPattern = Regex("html5player\\.setVideoHLS\\('([^']+)'\\)")
        hlsPattern.find(pageContent)?.let { match ->
            urls.add(VideoUrlExtractor.VideoInfo(
                url = match.groupValues[1],
                type = VideoUrlExtractor.VideoType.HLS
            ))
        }
        
        // Extract MP4 URLs
        val mp4Pattern = Regex("html5player\\.setVideo(\\w+)\\('([^']+)'\\)")
        mp4Pattern.findAll(pageContent).forEach { match ->
            val quality = match.groupValues[1].lowercase()
            val url = match.groupValues[2]
            
            urls.add(VideoUrlExtractor.VideoInfo(
                url = url,
                quality = when {
                    quality.contains("high") -> "720p"
                    quality.contains("low") -> "360p"
                    else -> null
                },
                type = VideoUrlExtractor.VideoType.PROGRESSIVE
            ))
        }
        
        return urls
    }
}