package com.astralplayer.nextplayer.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.regex.Pattern

data class BrowserIntegrationState(
    val isProcessingUrl: Boolean = false,
    val sourceUrl: String? = null,
    val extractedVideoUrl: String? = null,
    val extractionError: String? = null,
    val supportedSite: Boolean = true
)

data class VideoPlaybackInfo(
    val uri: Uri,
    val title: String,
    val type: VideoType = VideoType.LOCAL,
    val headers: Map<String, String> = emptyMap(),
    val cookies: Map<String, String> = emptyMap(),
    val cloudProvider: CloudProvider? = null,
    val cloudFileId: String? = null
)

enum class VideoType {
    LOCAL, HLS, DASH, RTSP, RTMP, CLOUD_STREAM, BROWSER_STREAM
}

sealed class VideoUrlAnalysis {
    data class DirectVideo(
        val videoUrl: String,
        val title: String,
        val headers: Map<String, String> = emptyMap(),
        val cookies: Map<String, String> = emptyMap()
    ) : VideoUrlAnalysis()
    
    data class StreamingPlaylist(
        val playlistUrl: String,
        val title: String,
        val headers: Map<String, String> = emptyMap()
    ) : VideoUrlAnalysis()
    
    data class EmbeddedVideo(
        val embedUrl: String,
        val title: String
    ) : VideoUrlAnalysis()
    
    data class UnsupportedSite(
        val reason: String
    ) : VideoUrlAnalysis()
}

data class VideoSources(
    val directUrls: List<String> = emptyList(),
    val hlsUrls: List<String> = emptyList(),
    val dashUrls: List<String> = emptyList(),
    val embedUrls: List<String> = emptyList(),
    val title: String? = null
)

interface VideoUrlAnalyzer {
    suspend fun analyzeUrl(
        url: String,
        cookies: Map<String, String> = emptyMap(),
        headers: Map<String, String> = emptyMap()
    ): VideoUrlAnalysis
}

interface HtmlParser {
    fun extractVideoSources(html: String): VideoSources
}

class VideoUrlAnalyzerImpl constructor(
    private val okHttpClient: OkHttpClient,
    private val htmlParser: HtmlParser
) : VideoUrlAnalyzer {
    
    override suspend fun analyzeUrl(
        url: String,
        cookies: Map<String, String>,
        headers: Map<String, String>
    ): VideoUrlAnalysis {
        return try {
            val request = Request.Builder()
                .url(url)
                .apply {
                    headers.forEach { (key, value) -> addHeader(key, value) }
                    if (cookies.isNotEmpty()) {
                        val cookieString = cookies.entries.joinToString("; ") { "${it.key}=${it.value}" }
                        addHeader("Cookie", cookieString)
                    }
                }
                .build()
            
            val response = okHttpClient.newCall(request).execute()
            val html = response.body?.string() ?: ""
            
            // Parse HTML to find video sources
            val videoSources = htmlParser.extractVideoSources(html)
            
            when {
                videoSources.directUrls.isNotEmpty() -> {
                    VideoUrlAnalysis.DirectVideo(
                        videoUrl = videoSources.directUrls.first(),
                        title = videoSources.title ?: "Video",
                        headers = headers,
                        cookies = cookies
                    )
                }
                videoSources.hlsUrls.isNotEmpty() -> {
                    VideoUrlAnalysis.StreamingPlaylist(
                        playlistUrl = videoSources.hlsUrls.first(),
                        title = videoSources.title ?: "Video",
                        headers = headers
                    )
                }
                videoSources.embedUrls.isNotEmpty() -> {
                    VideoUrlAnalysis.EmbeddedVideo(
                        embedUrl = videoSources.embedUrls.first(),
                        title = videoSources.title ?: "Video"
                    )
                }
                else -> {
                    VideoUrlAnalysis.UnsupportedSite("No video sources found")
                }
            }
        } catch (e: Exception) {
            VideoUrlAnalysis.UnsupportedSite("Error analyzing URL: ${e.message}")
        }
    }
}

class HtmlParserImpl : HtmlParser {
    
    override fun extractVideoSources(html: String): VideoSources {
        val directUrls = mutableListOf<String>()
        val hlsUrls = mutableListOf<String>()
        val dashUrls = mutableListOf<String>()
        val embedUrls = mutableListOf<String>()
        var title: String? = null
        
        // Extract title
        val titlePattern = Pattern.compile("<title[^>]*>([^<]+)</title>", Pattern.CASE_INSENSITIVE)
        val titleMatcher = titlePattern.matcher(html)
        if (titleMatcher.find()) {
            title = titleMatcher.group(1)?.trim()
        }
        
        // Extract direct video URLs
        val videoPattern = Pattern.compile(
            "(?:src|href)=[\"']([^\"']*\\.(?:mp4|mkv|avi|webm|mov|wmv|flv|3gp|m4v)(?:\\?[^\"']*)?)[\"']",
            Pattern.CASE_INSENSITIVE
        )
        val videoMatcher = videoPattern.matcher(html)
        while (videoMatcher.find()) {
            videoMatcher.group(1)?.let { directUrls.add(it) }
        }
        
        // Extract HLS URLs
        val hlsPattern = Pattern.compile(
            "(?:src|href)=[\"']([^\"']*\\.m3u8(?:\\?[^\"']*)?)[\"']",
            Pattern.CASE_INSENSITIVE
        )
        val hlsMatcher = hlsPattern.matcher(html)
        while (hlsMatcher.find()) {
            hlsMatcher.group(1)?.let { hlsUrls.add(it) }
        }
        
        // Extract DASH URLs
        val dashPattern = Pattern.compile(
            "(?:src|href)=[\"']([^\"']*\\.mpd(?:\\?[^\"']*)?)[\"']",
            Pattern.CASE_INSENSITIVE
        )
        val dashMatcher = dashPattern.matcher(html)
        while (dashMatcher.find()) {
            dashMatcher.group(1)?.let { dashUrls.add(it) }
        }
        
        // Extract embedded video URLs
        val embedPattern = Pattern.compile(
            "<(?:iframe|embed)[^>]*src=[\"']([^\"']*(?:youtube|vimeo|dailymotion)[^\"']*)[\"']",
            Pattern.CASE_INSENSITIVE
        )
        val embedMatcher = embedPattern.matcher(html)
        while (embedMatcher.find()) {
            embedMatcher.group(1)?.let { embedUrls.add(it) }
        }
        
        return VideoSources(
            directUrls = directUrls.distinct(),
            hlsUrls = hlsUrls.distinct(),
            dashUrls = dashUrls.distinct(),
            embedUrls = embedUrls.distinct(),
            title = title
        )
    }
}

class BrowserIntegrationManager constructor(
    private val context: Context,
    private val urlAnalyzer: VideoUrlAnalyzer
) {
    
    private val _integrationState = MutableStateFlow(BrowserIntegrationState())
    val integrationState: StateFlow<BrowserIntegrationState> = _integrationState.asStateFlow()
    
    suspend fun handleBrowserIntent(intent: Intent): Result<VideoPlaybackInfo> {
        return when (intent.action) {
            Intent.ACTION_VIEW -> handleViewIntent(intent)
            Intent.ACTION_SEND -> handleSendIntent(intent)
            else -> Result.failure(IllegalArgumentException("Unsupported intent action"))
        }
    }
    
    private suspend fun handleViewIntent(intent: Intent): Result<VideoPlaybackInfo> {
        val uri = intent.data ?: return Result.failure(IllegalArgumentException("No URI provided"))
        
        return when (uri.scheme) {
            "http", "https" -> handleWebUrl(uri, intent)
            "file", "content" -> handleLocalFile(uri)
            else -> Result.failure(IllegalArgumentException("Unsupported URI scheme"))
        }
    }
    
    private suspend fun handleSendIntent(intent: Intent): Result<VideoPlaybackInfo> {
        val text = intent.getStringExtra(Intent.EXTRA_TEXT)
            ?: return Result.failure(IllegalArgumentException("No text provided"))
        
        // Try to extract URL from shared text
        val urlPattern = Pattern.compile("https?://[^\\s]+")
        val matcher = urlPattern.matcher(text)
        
        return if (matcher.find()) {
            val url = matcher.group()
            handleWebUrl(Uri.parse(url), intent)
        } else {
            Result.failure(IllegalArgumentException("No valid URL found in shared text"))
        }
    }
    
    private suspend fun handleWebUrl(uri: Uri, intent: Intent): Result<VideoPlaybackInfo> {
        updateState { copy(isProcessingUrl = true, sourceUrl = uri.toString(), extractionError = null) }
        
        try {
            // Extract cookies and headers from browser context
            val cookies = extractCookiesFromIntent(intent)
            val headers = extractHeadersFromIntent(intent)
            
            // Analyze the URL to determine video source
            val analysisResult = urlAnalyzer.analyzeUrl(uri.toString(), cookies, headers)
            
            val result = when (analysisResult) {
                is VideoUrlAnalysis.DirectVideo -> {
                    Result.success(VideoPlaybackInfo(
                        uri = Uri.parse(analysisResult.videoUrl),
                        title = analysisResult.title,
                        type = VideoType.BROWSER_STREAM,
                        headers = analysisResult.headers,
                        cookies = analysisResult.cookies
                    ))
                }
                is VideoUrlAnalysis.StreamingPlaylist -> {
                    Result.success(VideoPlaybackInfo(
                        uri = Uri.parse(analysisResult.playlistUrl),
                        title = analysisResult.title,
                        type = VideoType.HLS,
                        headers = analysisResult.headers
                    ))
                }
                is VideoUrlAnalysis.EmbeddedVideo -> {
                    // Extract video from embedded player
                    extractEmbeddedVideo(analysisResult.embedUrl, cookies, headers)
                }
                is VideoUrlAnalysis.UnsupportedSite -> {
                    Result.failure(UnsupportedOperationException("Site not supported: ${analysisResult.reason}"))
                }
            }
            
            updateState { 
                copy(
                    isProcessingUrl = false,
                    extractedVideoUrl = if (result.isSuccess) result.getOrNull()?.uri?.toString() else null,
                    extractionError = if (result.isFailure) result.exceptionOrNull()?.message else null,
                    supportedSite = result.isSuccess
                )
            }
            
            return result
        } catch (e: Exception) {
            updateState { 
                copy(
                    isProcessingUrl = false,
                    extractionError = e.message,
                    supportedSite = false
                )
            }
            return Result.failure(e)
        }
    }
    
    private suspend fun handleLocalFile(uri: Uri): Result<VideoPlaybackInfo> {
        return try {
            Result.success(VideoPlaybackInfo(
                uri = uri,
                title = uri.lastPathSegment ?: "Local Video",
                type = VideoType.LOCAL
            ))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private suspend fun extractEmbeddedVideo(
        embedUrl: String,
        cookies: Map<String, String>,
        headers: Map<String, String>
    ): Result<VideoPlaybackInfo> {
        // For embedded videos, we might need to extract the actual video URL
        // This is a simplified implementation
        return try {
            val analysisResult = urlAnalyzer.analyzeUrl(embedUrl, cookies, headers)
            
            when (analysisResult) {
                is VideoUrlAnalysis.DirectVideo -> {
                    Result.success(VideoPlaybackInfo(
                        uri = Uri.parse(analysisResult.videoUrl),
                        title = analysisResult.title,
                        type = VideoType.BROWSER_STREAM,
                        headers = analysisResult.headers,
                        cookies = analysisResult.cookies
                    ))
                }
                else -> {
                    Result.failure(Exception("Could not extract video from embedded URL"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private fun extractCookiesFromIntent(intent: Intent): Map<String, String> {
        val cookies = mutableMapOf<String, String>()
        
        // Try to get cookies from various sources
        intent.getStringExtra("cookies")?.let { cookieString ->
            cookieString.split(";").forEach { cookie ->
                val parts = cookie.trim().split("=", limit = 2)
                if (parts.size == 2) {
                    cookies[parts[0]] = parts[1]
                }
            }
        }
        
        return cookies
    }
    
    private fun extractHeadersFromIntent(intent: Intent): Map<String, String> {
        val headers = mutableMapOf<String, String>()
        
        // Add common headers
        headers["User-Agent"] = "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Mobile Safari/537.36"
        headers["Accept"] = "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8"
        headers["Accept-Language"] = "en-US,en;q=0.5"
        
        // Try to extract additional headers from intent
        intent.getStringExtra("headers")?.let { headerString ->
            headerString.split("\n").forEach { header ->
                val parts = header.trim().split(":", limit = 2)
                if (parts.size == 2) {
                    headers[parts[0].trim()] = parts[1].trim()
                }
            }
        }
        
        return headers
    }
    
    fun getSupportedSites(): List<String> {
        return listOf(
            "youtube.com", "youtu.be",
            "vimeo.com",
            "dailymotion.com",
            "pornhub.com", "xvideos.com", "xnxx.com", "xhamster.com",
            "redtube.com", "youporn.com", "spankbang.com",
            "twitch.tv",
            "facebook.com", "instagram.com",
            "twitter.com", "x.com"
        )
    }
    
    fun isSiteSupported(url: String): Boolean {
        val uri = Uri.parse(url)
        val host = uri.host?.lowercase() ?: return false
        
        return getSupportedSites().any { supportedSite ->
            host == supportedSite || host.endsWith(".$supportedSite")
        }
    }
    
    private fun updateState(update: BrowserIntegrationState.() -> BrowserIntegrationState) {
        _integrationState.value = _integrationState.value.update()
    }
}