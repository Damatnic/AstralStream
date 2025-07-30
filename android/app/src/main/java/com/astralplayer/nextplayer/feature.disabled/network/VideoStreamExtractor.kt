package com.astralplayer.nextplayer.feature.network

import android.content.Context
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import java.io.IOException
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

/**
 * Advanced video stream extractor for adult content sites
 * Implements extraction logic similar to youtube-dl for popular adult platforms
 */
class VideoStreamExtractor(private val context: Context) {
    
    companion object {
        private const val TAG = "VideoStreamExtractor"
        private const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
    }
    
    private val urlMatcher = AdultContentUrlMatcher()
    
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .addInterceptor(UserAgentInterceptor())
        .addInterceptor(HeaderInterceptor())
        .build()
    
    /**
     * Extract video stream URLs from a given page URL
     */
    suspend fun extractStreams(url: String): VideoExtractionResult {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Extracting streams from: $url")
                
                val siteType = urlMatcher.getSiteType(url)
                val siteName = urlMatcher.getSiteName(url)
                
                when (siteType) {
                    AdultSiteType.PORNHUB -> extractPornHubStreams(url)
                    AdultSiteType.XVIDEOS -> extractXVideosStreams(url)
                    AdultSiteType.XNXX -> extractXNXXStreams(url)
                    AdultSiteType.XHAMSTER -> extractXHamsterStreams(url)
                    AdultSiteType.SPANKBANG -> extractSpankBangStreams(url)
                    AdultSiteType.REDTUBE -> extractRedTubeStreams(url)
                    AdultSiteType.YOUPORN -> extractYouPornStreams(url)
                    else -> extractGenericStreams(url, siteName)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error extracting streams from $url", e)
                VideoExtractionResult.Error("Failed to extract video streams: ${e.message}")
            }
        }
    }
    
    private suspend fun extractPornHubStreams(url: String): VideoExtractionResult {
        return try {
            val pageContent = fetchPageContent(url)
            
            // Extract title
            val titlePattern = Pattern.compile("<title>([^<]+)</title>", Pattern.CASE_INSENSITIVE)
            val titleMatcher = titlePattern.matcher(pageContent)
            val title = if (titleMatcher.find()) {
                titleMatcher.group(1)?.replace(" - Pornhub.com", "") ?: "Unknown Video"
            } else "Unknown Video"
            
            // Extract video sources - PornHub uses flashvars
            val flashvarsPattern = Pattern.compile("var flashvars[^=]*=\\s*([^;]+);")
            val flashvarsMatcher = flashvarsPattern.matcher(pageContent)
            
            val streams = mutableListOf<VideoStream>()
            
            if (flashvarsMatcher.find()) {
                val flashvarsData = flashvarsMatcher.group(1) ?: ""
                
                // Extract different quality streams
                val qualityPatterns = mapOf(
                    "1080p" to Pattern.compile("\"quality_1080p\":\"([^\"]+)\""),
                    "720p" to Pattern.compile("\"quality_720p\":\"([^\"]+)\""),
                    "480p" to Pattern.compile("\"quality_480p\":\"([^\"]+)\""),
                    "360p" to Pattern.compile("\"quality_360p\":\"([^\"]+)\""),
                    "240p" to Pattern.compile("\"quality_240p\":\"([^\"]+)\"")
                )
                
                qualityPatterns.forEach { (quality, pattern) ->
                    val matcher = pattern.matcher(flashvarsData)
                    if (matcher.find()) {
                        val streamUrl = matcher.group(1)?.replace("\\", "") ?: ""
                        if (streamUrl.isNotEmpty()) {
                            streams.add(VideoStream(
                                url = streamUrl,
                                quality = quality,
                                format = "mp4",
                                filesize = null
                            ))
                        }
                    }
                }
            }
            
            // Fallback: Look for HLS streams
            if (streams.isEmpty()) {
                val hlsPattern = Pattern.compile("\"(https://[^\"]+\\.m3u8[^\"]*)")
                val hlsMatcher = hlsPattern.matcher(pageContent)
                if (hlsMatcher.find()) {
                    val hlsUrl = hlsMatcher.group(1) ?: ""
                    streams.add(VideoStream(
                        url = hlsUrl,
                        quality = "auto",
                        format = "m3u8",
                        filesize = null
                    ))
                }
            }
            
            if (streams.isNotEmpty()) {
                VideoExtractionResult.Success(
                    title = title,
                    streams = streams,
                    thumbnail = extractThumbnail(pageContent),
                    duration = extractDuration(pageContent),
                    siteName = "PornHub"
                )
            } else {
                VideoExtractionResult.Error("No video streams found")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting PornHub streams", e)
            VideoExtractionResult.Error("PornHub extraction failed: ${e.message}")
        }
    }
    
    private suspend fun extractXVideosStreams(url: String): VideoExtractionResult {
        return try {
            val pageContent = fetchPageContent(url)
            
            // Extract title
            val titlePattern = Pattern.compile("<title>([^<]+) - XVIDEOS.COM</title>")
            val titleMatcher = titlePattern.matcher(pageContent)
            val title = if (titleMatcher.find()) {
                titleMatcher.group(1) ?: "Unknown Video"
            } else "Unknown Video"
            
            val streams = mutableListOf<VideoStream>()
            
            // XVideos uses setVideoHLS for stream URLs
            val hlsPattern = Pattern.compile("setVideoHLS\\('([^']+)'\\)")
            val hlsMatcher = hlsPattern.matcher(pageContent)
            
            if (hlsMatcher.find()) {
                val hlsUrl = hlsMatcher.group(1) ?: ""
                streams.add(VideoStream(
                    url = hlsUrl,
                    quality = "auto",
                    format = "m3u8",
                    filesize = null
                ))
            }
            
            // Also look for direct MP4 URLs
            val mp4Pattern = Pattern.compile("setVideoUrlLow\\('([^']+)'\\)|setVideoUrlHigh\\('([^']+)'\\)")
            val mp4Matcher = mp4Pattern.matcher(pageContent)
            
            while (mp4Matcher.find()) {
                val lowUrl = mp4Matcher.group(1)
                val highUrl = mp4Matcher.group(2)
                
                lowUrl?.let {
                    streams.add(VideoStream(url = it, quality = "360p", format = "mp4", filesize = null))
                }
                highUrl?.let {
                    streams.add(VideoStream(url = it, quality = "720p", format = "mp4", filesize = null))
                }
            }
            
            if (streams.isNotEmpty()) {
                VideoExtractionResult.Success(
                    title = title,
                    streams = streams,
                    thumbnail = extractThumbnail(pageContent),
                    duration = extractDuration(pageContent),
                    siteName = "XVideos"
                )
            } else {
                VideoExtractionResult.Error("No video streams found")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting XVideos streams", e)
            VideoExtractionResult.Error("XVideos extraction failed: ${e.message}")
        }
    }
    
    private suspend fun extractSpankBangStreams(url: String): VideoExtractionResult {
        return try {
            val pageContent = fetchPageContent(url)
            
            // Extract title
            val titlePattern = Pattern.compile("<title>([^<]+) - SpankBang</title>")
            val titleMatcher = titlePattern.matcher(pageContent)
            val title = if (titleMatcher.find()) {
                titleMatcher.group(1) ?: "Unknown Video"
            } else "Unknown Video"
            
            val streams = mutableListOf<VideoStream>()
            
            // SpankBang uses stream_data JSON
            val streamDataPattern = Pattern.compile("stream_data\\s*=\\s*(\\{[^}]+\\})")
            val streamMatcher = streamDataPattern.matcher(pageContent)
            
            if (streamMatcher.find()) {
                val streamData = streamMatcher.group(1) ?: ""
                
                // Extract different quality streams from JSON-like data
                val qualityPatterns = mapOf(
                    "1080p" to Pattern.compile("\"1080p\":\\s*\\[\"([^\"]+)\""),
                    "720p" to Pattern.compile("\"720p\":\\s*\\[\"([^\"]+)\""),
                    "480p" to Pattern.compile("\"480p\":\\s*\\[\"([^\"]+)\""),
                    "320p" to Pattern.compile("\"320p\":\\s*\\[\"([^\"]+)\""),
                    "240p" to Pattern.compile("\"240p\":\\s*\\[\"([^\"]+)\"")
                )
                
                qualityPatterns.forEach { (quality, pattern) ->
                    val matcher = pattern.matcher(streamData)
                    if (matcher.find()) {
                        val streamUrl = matcher.group(1) ?: ""
                        if (streamUrl.isNotEmpty()) {
                            streams.add(VideoStream(
                                url = streamUrl,
                                quality = quality,
                                format = "mp4",
                                filesize = null
                            ))
                        }
                    }
                }
            }
            
            if (streams.isNotEmpty()) {
                VideoExtractionResult.Success(
                    title = title,
                    streams = streams,
                    thumbnail = extractThumbnail(pageContent),
                    duration = extractDuration(pageContent),
                    siteName = "SpankBang"
                )
            } else {
                VideoExtractionResult.Error("No video streams found")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting SpankBang streams", e)
            VideoExtractionResult.Error("SpankBang extraction failed: ${e.message}")
        }
    }
    
    private suspend fun extractXNXXStreams(url: String): VideoExtractionResult {
        // XNXX uses similar patterns to XVideos since they're related
        return extractXVideosStreams(url.replace("xnxx.com", "xvideos.com"))
    }
    
    private suspend fun extractXHamsterStreams(url: String): VideoExtractionResult {
        return try {
            val pageContent = fetchPageContent(url)
            
            val titlePattern = Pattern.compile("<title>([^<]+) - xHamster</title>")
            val titleMatcher = titlePattern.matcher(pageContent)
            val title = if (titleMatcher.find()) {
                titleMatcher.group(1) ?: "Unknown Video"
            } else "Unknown Video"
            
            val streams = mutableListOf<VideoStream>()
            
            // xHamster uses various patterns for video sources
            val sourcePatterns = listOf(
                Pattern.compile("\"(https://[^\"]+\\.mp4[^\"]*\""),
                Pattern.compile("\"(https://[^\"]+\\.m3u8[^\"]*\"")
            )
            
            sourcePatterns.forEach { pattern ->
                val matcher = pattern.matcher(pageContent)
                while (matcher.find()) {
                    val streamUrl = matcher.group(1)?.replace("\"", "") ?: ""
                    if (streamUrl.isNotEmpty()) {
                        val format = if (streamUrl.contains(".m3u8")) "m3u8" else "mp4"
                        val quality = if (streamUrl.contains("1080")) "1080p" 
                                    else if (streamUrl.contains("720")) "720p"
                                    else if (streamUrl.contains("480")) "480p"
                                    else "auto"
                        
                        streams.add(VideoStream(
                            url = streamUrl,
                            quality = quality,
                            format = format,
                            filesize = null
                        ))
                    }
                }
            }
            
            if (streams.isNotEmpty()) {
                VideoExtractionResult.Success(
                    title = title,
                    streams = streams.distinctBy { it.url }, // Remove duplicates
                    thumbnail = extractThumbnail(pageContent),
                    duration = extractDuration(pageContent),
                    siteName = "xHamster"
                )
            } else {
                VideoExtractionResult.Error("No video streams found")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting xHamster streams", e)
            VideoExtractionResult.Error("xHamster extraction failed: ${e.message}")
        }
    }
    
    private suspend fun extractRedTubeStreams(url: String): VideoExtractionResult {
        return try {
            val pageContent = fetchPageContent(url)
            
            val titlePattern = Pattern.compile("<title>([^<]+) - RedTube</title>")
            val titleMatcher = titlePattern.matcher(pageContent)
            val title = if (titleMatcher.find()) {
                titleMatcher.group(1) ?: "Unknown Video"
            } else "Unknown Video"
            
            val streams = mutableListOf<VideoStream>()
            
            // RedTube often uses medias array in JavaScript
            val mediasPattern = Pattern.compile("medias\\s*=\\s*(\\[[^\\]]+\\])")
            val mediasMatcher = mediasPattern.matcher(pageContent)
            
            if (mediasMatcher.find()) {
                val mediasData = mediasMatcher.group(1) ?: ""
                
                val urlPattern = Pattern.compile("\"(https://[^\"]+\\.mp4[^\"]*)")
                val urlMatcher = urlPattern.matcher(mediasData)
                
                while (urlMatcher.find()) {
                    val streamUrl = urlMatcher.group(1) ?: ""
                    if (streamUrl.isNotEmpty()) {
                        val quality = when {
                            streamUrl.contains("1080") -> "1080p"
                            streamUrl.contains("720") -> "720p"
                            streamUrl.contains("480") -> "480p"
                            streamUrl.contains("360") -> "360p"
                            else -> "auto"
                        }
                        
                        streams.add(VideoStream(
                            url = streamUrl,
                            quality = quality,
                            format = "mp4",
                            filesize = null
                        ))
                    }
                }
            }
            
            if (streams.isNotEmpty()) {
                VideoExtractionResult.Success(
                    title = title,
                    streams = streams,
                    thumbnail = extractThumbnail(pageContent),
                    duration = extractDuration(pageContent),
                    siteName = "RedTube"
                )
            } else {
                VideoExtractionResult.Error("No video streams found")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting RedTube streams", e)
            VideoExtractionResult.Error("RedTube extraction failed: ${e.message}")
        }
    }
    
    private suspend fun extractYouPornStreams(url: String): VideoExtractionResult {
        return try {
            val pageContent = fetchPageContent(url)
            
            val titlePattern = Pattern.compile("<title>([^<]+) - YouPorn</title>")
            val titleMatcher = titlePattern.matcher(pageContent)
            val title = if (titleMatcher.find()) {
                titleMatcher.group(1) ?: "Unknown Video"
            } else "Unknown Video"
            
            val streams = mutableListOf<VideoStream>()
            
            // YouPorn uses videoSrc in JavaScript
            val videoSrcPattern = Pattern.compile("videoSrc\\s*:\\s*\"([^\"]+)\"")
            val srcMatcher = videoSrcPattern.matcher(pageContent)
            
            if (srcMatcher.find()) {
                val streamUrl = srcMatcher.group(1) ?: ""
                if (streamUrl.isNotEmpty()) {
                    streams.add(VideoStream(
                        url = streamUrl,
                        quality = "auto",
                        format = if (streamUrl.contains(".m3u8")) "m3u8" else "mp4",
                        filesize = null
                    ))
                }
            }
            
            if (streams.isNotEmpty()) {
                VideoExtractionResult.Success(
                    title = title,
                    streams = streams,
                    thumbnail = extractThumbnail(pageContent),
                    duration = extractDuration(pageContent),
                    siteName = "YouPorn"
                )
            } else {
                VideoExtractionResult.Error("No video streams found")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting YouPorn streams", e)
            VideoExtractionResult.Error("YouPorn extraction failed: ${e.message}")
        }
    }
    
    private suspend fun extractGenericStreams(url: String, siteName: String): VideoExtractionResult {
        return try {
            val pageContent = fetchPageContent(url)
            
            val titlePattern = Pattern.compile("<title>([^<]+)</title>")
            val titleMatcher = titlePattern.matcher(pageContent)
            val title = if (titleMatcher.find()) {
                titleMatcher.group(1) ?: "Unknown Video"
            } else "Unknown Video"
            
            val streams = mutableListOf<VideoStream>()
            
            // Generic patterns for video URLs
            val genericPatterns = listOf(
                Pattern.compile("\"(https://[^\"]+\\.mp4[^\"]*\""),
                Pattern.compile("\"(https://[^\"]+\\.m3u8[^\"]*\""),
                Pattern.compile("\"(https://[^\"]+\\.webm[^\"]*\""),
                Pattern.compile("src\\s*=\\s*\"([^\"]+\\.(mp4|m3u8|webm))\"")
            )
            
            genericPatterns.forEach { pattern ->
                val matcher = pattern.matcher(pageContent)
                while (matcher.find()) {
                    val streamUrl = matcher.group(1)?.replace("\"", "") ?: ""
                    if (streamUrl.isNotEmpty() && streamUrl.startsWith("http")) {
                        val format = when {
                            streamUrl.contains(".m3u8") -> "m3u8"
                            streamUrl.contains(".webm") -> "webm"
                            else -> "mp4"
                        }
                        
                        streams.add(VideoStream(
                            url = streamUrl,
                            quality = "auto",
                            format = format,
                            filesize = null
                        ))
                    }
                }
            }
            
            if (streams.isNotEmpty()) {
                VideoExtractionResult.Success(
                    title = title,
                    streams = streams.distinctBy { it.url },
                    thumbnail = extractThumbnail(pageContent),
                    duration = extractDuration(pageContent),
                    siteName = siteName
                )
            } else {
                VideoExtractionResult.Error("No video streams found using generic extraction")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting generic streams", e)
            VideoExtractionResult.Error("Generic extraction failed: ${e.message}")
        }
    }
    
    private suspend fun fetchPageContent(url: String): String {
        return withContext(Dispatchers.IO) {
            val request = Request.Builder()
                .url(url)
                .addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8")
                .addHeader("Accept-Language", "en-US,en;q=0.5")
                .addHeader("Accept-Encoding", "gzip, deflate")
                .addHeader("DNT", "1")
                .addHeader("Connection", "keep-alive")
                .addHeader("Upgrade-Insecure-Requests", "1")
                .build()
            
            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                throw IOException("HTTP ${response.code}")
            }
            
            response.body?.string() ?: throw IOException("Empty response body")
        }
    }
    
    private fun extractThumbnail(pageContent: String): String? {
        val patterns = listOf(
            Pattern.compile("\"image_url\"\\s*:\\s*\"([^\"]+)\""),
            Pattern.compile("\"thumb\"\\s*:\\s*\"([^\"]+)\""),
            Pattern.compile("\"thumbnail\"\\s*:\\s*\"([^\"]+)\""),
            Pattern.compile("<meta property=\"og:image\" content=\"([^\"]+)\""),
            Pattern.compile("<meta name=\"twitter:image\" content=\"([^\"]+)\"")
        )
        
        patterns.forEach { pattern ->
            val matcher = pattern.matcher(pageContent)
            if (matcher.find()) {
                return matcher.group(1)?.replace("\\", "")
            }
        }
        
        return null
    }
    
    private fun extractDuration(pageContent: String): Long? {
        val patterns = listOf(
            Pattern.compile("\"duration\"\\s*:\\s*([0-9]+)"),
            Pattern.compile("duration\\s*=\\s*([0-9]+)"),
            Pattern.compile("<meta property=\"video:duration\" content=\"([0-9]+)\"")
        )
        
        patterns.forEach { pattern ->
            val matcher = pattern.matcher(pageContent)
            if (matcher.find()) {
                return matcher.group(1)?.toLongOrNull()
            }
        }
        
        return null
    }
    
    private inner class UserAgentInterceptor : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val originalRequest = chain.request()
            val requestWithUserAgent = originalRequest.newBuilder()
                .header("User-Agent", USER_AGENT)
                .build()
            return chain.proceed(requestWithUserAgent)
        }
    }
    
    private inner class HeaderInterceptor : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val originalRequest = chain.request()
            val host = originalRequest.url.host
            
            val requestBuilder = originalRequest.newBuilder()
            
            // Add referer for some sites that require it
            when {
                host.contains("pornhub") -> {
                    requestBuilder.addHeader("Referer", "https://www.pornhub.com/")
                }
                host.contains("xvideos") -> {
                    requestBuilder.addHeader("Referer", "https://www.xvideos.com/")
                }
                host.contains("xnxx") -> {
                    requestBuilder.addHeader("Referer", "https://www.xnxx.com/")
                }
                host.contains("spankbang") -> {
                    requestBuilder.addHeader("Referer", "https://spankbang.com/")
                }
            }
            
            return chain.proceed(requestBuilder.build())
        }
    }
}

/**
 * Data classes for video extraction results
 */
sealed class VideoExtractionResult {
    data class Success(
        val title: String,
        val streams: List<VideoStream>,
        val thumbnail: String? = null,
        val duration: Long? = null,
        val siteName: String
    ) : VideoExtractionResult()
    
    data class Error(val message: String) : VideoExtractionResult()
}

data class VideoStream(
    val url: String,
    val quality: String,
    val format: String,
    val filesize: Long? = null
)