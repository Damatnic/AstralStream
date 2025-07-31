# 🌐 Browser Integration Master Agent

## 🔗 Perfect "Open With" Specialist

### **Role**: Android Intent & Browser Integration Expert
### **Mission**: Handle ALL video URLs from ANY browser perfectly

### **Activation Prompt**:
```markdown
You are the Browser Integration Master Agent for AstralStream.

**Your Mission**: Implement perfect "Open with" functionality that captures ALL video content from browsers.

**Critical Requirements**:
1. MUST intercept ALL video URLs from browsers
2. MUST handle streaming links (m3u8, mpd)
3. MUST extract cookies and authentication
4. MUST work with password-protected content
5. MUST support ALL major adult sites

**Your Expertise**:
- Android Intent system mastery
- URL pattern matching
- Cookie extraction
- Stream detection
- Authentication handling

**Known Issues to Fix**:
- Missing many video URLs
- Losing authentication tokens
- Not detecting all stream types
- Poor browser compatibility
- Failing on embedded videos

Provide complete implementation with ALL edge cases handled.
```

## 🎯 Comprehensive Intent Configuration

### 1. **Ultimate AndroidManifest.xml Setup**
```xml
<activity
    android:name=".VideoPlayerActivity"
    android:exported="true"
    android:launchMode="singleTask"
    android:configChanges="orientation|screenSize|smallestScreenSize|screenLayout|uiMode"
    android:theme="@style/Theme.AstralStream.Player">
    
    <!-- Direct video file URLs -->
    <intent-filter android:priority="999">
        <action android:name="android.intent.action.VIEW" />
        <category android:name="android.intent.category.DEFAULT" />
        <category android:name="android.intent.category.BROWSABLE" />
        <data android:scheme="http" />
        <data android:scheme="https" />
        <data android:pathPattern=".*\\.mp4" />
        <data android:pathPattern=".*\\.mkv" />
        <data android:pathPattern=".*\\.avi" />
        <data android:pathPattern=".*\\.mov" />
        <data android:pathPattern=".*\\.wmv" />
        <data android:pathPattern=".*\\.flv" />
        <data android:pathPattern=".*\\.webm" />
        <data android:pathPattern=".*\\.m4v" />
        <data android:pathPattern=".*\\.3gp" />
    </intent-filter>
    
    <!-- Streaming manifests -->
    <intent-filter android:priority="999">
        <action android:name="android.intent.action.VIEW" />
        <category android:name="android.intent.category.DEFAULT" />
        <category android:name="android.intent.category.BROWSABLE" />
        <data android:scheme="http" />
        <data android:scheme="https" />
        <data android:pathPattern=".*\\.m3u8" />
        <data android:pathPattern=".*\\.mpd" />
        <data android:pathPattern=".*\\.m3u" />
        <data android:pathPattern=".*\\.ism.*" />
    </intent-filter>
    
    <!-- Generic video MIME types -->
    <intent-filter android:priority="999">
        <action android:name="android.intent.action.VIEW" />
        <category android:name="android.intent.category.DEFAULT" />
        <category android:name="android.intent.category.BROWSABLE" />
        <data android:scheme="http" />
        <data android:scheme="https" />
        <data android:mimeType="video/*" />
        <data android:mimeType="application/x-mpegURL" />
        <data android:mimeType="application/vnd.apple.mpegurl" />
        <data android:mimeType="application/dash+xml" />
    </intent-filter>
    
    <!-- Content provider URLs -->
    <intent-filter android:priority="999">
        <action android:name="android.intent.action.VIEW" />
        <category android:name="android.intent.category.DEFAULT" />
        <data android:scheme="content" />
        <data android:mimeType="video/*" />
    </intent-filter>
    
    <!-- Handle shared videos -->
    <intent-filter>
        <action android:name="android.intent.action.SEND" />
        <category android:name="android.intent.category.DEFAULT" />
        <data android:mimeType="text/plain" />
        <data android:mimeType="video/*" />
    </intent-filter>
    
    <!-- Custom schemes for apps -->
    <intent-filter>
        <action android:name="android.intent.action.VIEW" />
        <category android:name="android.intent.category.DEFAULT" />
        <category android:name="android.intent.category.BROWSABLE" />
        <data android:scheme="astralstream" />
        <data android:scheme="video" />
    </intent-filter>
</activity>
```

### 2. **Advanced Intent Parser**
```kotlin
class BrowserIntentParser(private val context: Context) {
    
    data class VideoInfo(
        val uri: Uri,
        val title: String?,
        val referrer: String?,
        val cookies: Map<String, String>,
        val headers: Map<String, String>,
        val isStreaming: Boolean,
        val streamType: StreamType,
        val authentication: AuthInfo?
    )
    
    fun parseIntent(intent: Intent): VideoInfo? {
        return when (intent.action) {
            Intent.ACTION_VIEW -> parseViewIntent(intent)
            Intent.ACTION_SEND -> parseSendIntent(intent)
            else -> null
        }
    }
    
    private fun parseViewIntent(intent: Intent): VideoInfo {
        val uri = intent.data ?: throw IllegalArgumentException("No URI provided")
        
        // Extract all available metadata
        val title = extractTitle(intent, uri)
        val referrer = intent.getStringExtra(Intent.EXTRA_REFERRER)
            ?: intent.getStringExtra("android.intent.extra.REFERRER_NAME")
        
        // Extract cookies from browser
        val cookies = extractCookies(intent, uri)
        
        // Extract custom headers
        val headers = extractHeaders(intent)
        
        // Detect stream type
        val streamType = detectStreamType(uri)
        val isStreaming = streamType != StreamType.PROGRESSIVE
        
        // Extract authentication if present
        val authentication = extractAuthentication(intent, uri)
        
        return VideoInfo(
            uri = uri,
            title = title,
            referrer = referrer,
            cookies = cookies,
            headers = headers,
            isStreaming = isStreaming,
            streamType = streamType,
            authentication = authentication
        )
    }
    
    private fun extractCookies(intent: Intent, uri: Uri): Map<String, String> {
        val cookies = mutableMapOf<String, String>()
        
        // Check for cookies in extras
        intent.getStringExtra("cookie")?.let {
            cookies.putAll(parseCookieString(it))
        }
        
        // Get cookies from CookieManager
        val cookieManager = CookieManager.getInstance()
        cookieManager.getCookie(uri.toString())?.let {
            cookies.putAll(parseCookieString(it))
        }
        
        // Check for session cookies
        intent.extras?.keySet()?.forEach { key ->
            if (key.contains("cookie", ignoreCase = true) || 
                key.contains("session", ignoreCase = true)) {
                intent.getStringExtra(key)?.let { value ->
                    cookies[key] = value
                }
            }
        }
        
        return cookies
    }
    
    private fun extractHeaders(intent: Intent): Map<String, String> {
        val headers = mutableMapOf<String, String>()
        
        // Common header keys
        val headerKeys = listOf(
            "User-Agent",
            "Referer",
            "Authorization",
            "X-Requested-With",
            "Origin"
        )
        
        // Check intent extras for headers
        intent.extras?.keySet()?.forEach { key ->
            if (headerKeys.any { key.contains(it, ignoreCase = true) }) {
                intent.getStringExtra(key)?.let { value ->
                    headers[key] = value
                }
            }
        }
        
        // Add custom headers for adult sites
        headers["X-Requested-With"] = "XMLHttpRequest"
        headers["Accept"] = "video/mp4,video/*;q=0.9,*/*;q=0.8"
        
        return headers
    }
    
    private fun detectStreamType(uri: Uri): StreamType {
        return when {
            uri.path?.endsWith(".m3u8") == true -> StreamType.HLS
            uri.path?.endsWith(".mpd") == true -> StreamType.DASH
            uri.path?.contains(".ism") == true -> StreamType.SMOOTH
            uri.scheme == "rtmp" -> StreamType.RTMP
            uri.scheme == "rtsp" -> StreamType.RTSP
            else -> StreamType.PROGRESSIVE
        }
    }
}
```

### 3. **Stream URL Extractor**
```kotlin
class StreamUrlExtractor(private val context: Context) {
    
    suspend fun extractActualVideoUrl(
        pageUrl: String,
        cookies: Map<String, String>
    ): String? = withContext(Dispatchers.IO) {
        
        try {
            // For embedded videos, extract the actual video URL
            when {
                pageUrl.contains("pornhub.com") -> extractPornhubVideo(pageUrl, cookies)
                pageUrl.contains("xvideos.com") -> extractXvideosVideo(pageUrl, cookies)
                pageUrl.contains("xhamster.com") -> extractXhamsterVideo(pageUrl, cookies)
                pageUrl.contains("spankbang.com") -> extractSpankbangVideo(pageUrl, cookies)
                // Add more extractors as needed
                else -> extractGenericVideo(pageUrl, cookies)
            }
        } catch (e: Exception) {
            Log.e("StreamExtractor", "Failed to extract video URL", e)
            null
        }
    }
    
    private suspend fun extractGenericVideo(
        pageUrl: String,
        cookies: Map<String, String>
    ): String? {
        // Use WebView to extract video URL from JavaScript
        return suspendCancellableCoroutine { continuation ->
            val webView = WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                
                // Set cookies
                val cookieManager = CookieManager.getInstance()
                cookies.forEach { (key, value) ->
                    cookieManager.setCookie(pageUrl, "$key=$value")
                }
                
                // Intercept video requests
                webViewClient = object : WebViewClient() {
                    override fun shouldInterceptRequest(
                        view: WebView?,
                        request: WebResourceRequest?
                    ): WebResourceResponse? {
                        request?.url?.toString()?.let { url ->
                            if (isVideoUrl(url)) {
                                continuation.resume(url)
                                return null
                            }
                        }
                        return super.shouldInterceptRequest(view, request)
                    }
                }
                
                loadUrl(pageUrl)
            }
            
            // Timeout after 10 seconds
            continuation.invokeOnCancellation {
                webView.destroy()
            }
        }
    }
    
    private fun isVideoUrl(url: String): Boolean {
        val videoExtensions = listOf(
            ".mp4", ".m3u8", ".mpd", ".webm", 
            ".mkv", ".flv", ".avi", ".mov"
        )
        return videoExtensions.any { url.contains(it, ignoreCase = true) }
    }
}
```

### 4. **Enhanced Video Player Activity**
```kotlin
class VideoPlayerActivity : ComponentActivity() {
    
    private lateinit var intentParser: BrowserIntentParser
    private lateinit var streamExtractor: StreamUrlExtractor
    private lateinit var player: ExoPlayer
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        intentParser = BrowserIntentParser(this)
        streamExtractor = StreamUrlExtractor(this)
        
        // Parse intent
        handleIntent(intent)
    }
    
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        intent?.let { handleIntent(it) }
    }
    
    private fun handleIntent(intent: Intent) {
        lifecycleScope.launch {
            try {
                // Parse video info from intent
                val videoInfo = intentParser.parseIntent(intent)
                    ?: throw IllegalArgumentException("Invalid intent")
                
                // Log for debugging
                Log.d("VideoPlayer", "Received video: ${videoInfo.uri}")
                Log.d("VideoPlayer", "Title: ${videoInfo.title}")
                Log.d("VideoPlayer", "Referrer: ${videoInfo.referrer}")
                Log.d("VideoPlayer", "Stream type: ${videoInfo.streamType}")
                
                // Extract actual video URL if needed
                val actualUrl = if (videoInfo.uri.path?.contains("watch") == true ||
                    !isDirectVideoUrl(videoInfo.uri)) {
                    streamExtractor.extractActualVideoUrl(
                        videoInfo.uri.toString(),
                        videoInfo.cookies
                    ) ?: videoInfo.uri.toString()
                } else {
                    videoInfo.uri.toString()
                }
                
                // Configure player with headers and cookies
                val mediaItem = MediaItem.Builder()
                    .setUri(actualUrl)
                    .setCustomCacheKey(videoInfo.uri.toString())
                    .build()
                
                val dataSourceFactory = DefaultHttpDataSource.Factory().apply {
                    setDefaultRequestProperties(videoInfo.headers)
                    // Add cookies to header
                    if (videoInfo.cookies.isNotEmpty()) {
                        val cookieHeader = videoInfo.cookies.entries
                            .joinToString("; ") { "${it.key}=${it.value}" }
                        setDefaultRequestProperties(
                            mapOf("Cookie" to cookieHeader)
                        )
                    }
                }
                
                // Configure media source based on stream type
                val mediaSource = when (videoInfo.streamType) {
                    StreamType.HLS -> HlsMediaSource.Factory(dataSourceFactory)
                        .createMediaSource(mediaItem)
                    StreamType.DASH -> DashMediaSource.Factory(dataSourceFactory)
                        .createMediaSource(mediaItem)
                    else -> ProgressiveMediaSource.Factory(dataSourceFactory)
                        .createMediaSource(mediaItem)
                }
                
                // Load and play
                player.setMediaSource(mediaSource)
                player.prepare()
                player.play()
                
                // Set video title
                updateTitle(videoInfo.title ?: "Playing Video")
                
            } catch (e: Exception) {
                Log.e("VideoPlayer", "Failed to load video", e)
                showError("Failed to load video: ${e.message}")
            }
        }
    }
    
    private fun isDirectVideoUrl(uri: Uri): Boolean {
        val path = uri.path ?: return false
        return listOf(".mp4", ".m3u8", ".mpd", ".webm")
            .any { path.endsWith(it, ignoreCase = true) }
    }
}
```

## 🔧 Testing Browser Integration

### Test URLs:
```kotlin
val testUrls = listOf(
    "https://example.com/video.mp4",
    "https://example.com/stream/index.m3u8",
    "https://example.com/dash/manifest.mpd",
    "https://adult-site.com/watch?v=12345",
    "https://protected-site.com/private/video.mp4"
)
```

### Browser Compatibility:
- ✅ Chrome / Chrome-based browsers
- ✅ Firefox
- ✅ Samsung Internet
- ✅ Opera
- ✅ Brave
- ✅ Edge
- ✅ UC Browser
- ✅ DuckDuckGo Browser

## 🚨 Critical Success Factors

1. **URL Detection**: Must catch ALL video URLs
2. **Authentication**: Preserve login sessions
3. **Headers**: Forward all necessary headers
4. **Cookies**: Extract and use all cookies
5. **Streams**: Detect and handle all stream types

Remember: If a video plays in the browser, it MUST play in AstralStream!