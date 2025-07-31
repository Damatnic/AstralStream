// BrowserIntentHandler.kt
package com.astralplayer.core.browser

import android.content.Intent
import android.net.Uri
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BrowserIntentHandler @Inject constructor() {
    
    data class BrowserData(
        val sourceApp: String,
        val originalUrl: String,
        val extractedVideoUrl: String? = null,
        val userAgent: String? = null,
        val cookies: Map<String, String> = emptyMap(),
        val headers: Map<String, String> = emptyMap(),
        val referrer: String? = null,
        val isVideoUrl: Boolean = false
    )
    
    private val videoPatterns = listOf(
        // Direct video file extensions
        Regex(".*\\.(mp4|mkv|avi|mov|wmv|flv|webm|m4v|3gp|ts|m2ts)($|\\?|#)", RegexOption.IGNORE_CASE),
        
        // Streaming manifests
        Regex(".*\\.(m3u8|mpd)($|\\?|#)", RegexOption.IGNORE_CASE),
        Regex(".*/(playlist|manifest)\\.(m3u8|mpd|xml)($|\\?|#)", RegexOption.IGNORE_CASE),
        
        // Video service patterns
        Regex(".*\\/(video|stream|media|content|file)\\/.*", RegexOption.IGNORE_CASE),
        Regex(".*\\?.*(&|\\?)file=.*\\.(mp4|mkv|m3u8).*", RegexOption.IGNORE_CASE),
        Regex(".*blob:.*", RegexOption.IGNORE_CASE),
        
        // CDN patterns
        Regex(".*mediadelivery\\.net.*", RegexOption.IGNORE_CASE),
        Regex(".*cdn.*\\/(.*\\/)?.*\\.(mp4|m3u8|mpd).*", RegexOption.IGNORE_CASE),
        Regex(".*cloudfront\\.net.*\\.(mp4|m3u8|mpd).*", RegexOption.IGNORE_CASE),
        
        // Adult site patterns
        Regex(".*\\/(get_media|player_api|embed)\\/.*", RegexOption.IGNORE_CASE),
        Regex(".*\\/media\\/\\d+\\/.*", RegexOption.IGNORE_CASE),
        Regex(".*\\/player\\/\\w+.*", RegexOption.IGNORE_CASE),
        
        // Query parameter patterns
        Regex(".*[?&](video|stream|file|media|url)=.*", RegexOption.IGNORE_CASE),
        Regex(".*[?&]v=.*", RegexOption.IGNORE_CASE), // YouTube-style
        
        // Protocol-based patterns
        Regex("(rtsp|rtmp|mms|rtp)://.*", RegexOption.IGNORE_CASE)
    )
    
    fun extractBrowserData(intent: Intent): BrowserData {
        val sourcePackage = intent.`package` ?: getSourcePackageFromIntent(intent)
        val sourceApp = identifyBrowser(sourcePackage)
        val uri = intent.data ?: return BrowserData(sourceApp, "")
        val originalUrl = uri.toString()
        
        Timber.d("Processing intent from $sourceApp: $originalUrl")
        
        val browserData = when (sourceApp.lowercase()) {
            "chrome" -> extractChromeData(intent, uri, originalUrl)
            "firefox" -> extractFirefoxData(intent, uri, originalUrl)
            "edge" -> extractEdgeData(intent, uri, originalUrl)
            "opera" -> extractOperaData(intent, uri, originalUrl)
            "samsung" -> extractSamsungBrowserData(intent, uri, originalUrl)
            "brave" -> extractBraveData(intent, uri, originalUrl)
            else -> extractGenericBrowserData(intent, uri, originalUrl)
        }.copy(sourceApp = sourceApp)
        
        return browserData.copy(isVideoUrl = isVideoUrl(originalUrl))
    }
    
    private fun getSourcePackageFromIntent(intent: Intent): String? {
        // Try to get the source package from various intent extras
        return intent.getStringExtra("android.intent.extra.REFERRER_NAME") 
            ?: intent.getStringExtra("source_package")
            ?: intent.getStringExtra("calling_package")
    }
    
    private fun identifyBrowser(packageName: String?): String {
        return when {
            packageName?.contains("chrome") == true -> "chrome"
            packageName?.contains("firefox") == true -> "firefox"
            packageName?.contains("edge") == true -> "edge"
            packageName?.contains("opera") == true -> "opera"
            packageName?.contains("samsung") == true -> "samsung"
            packageName?.contains("brave") == true -> "brave"
            packageName?.contains("duckduckgo") == true -> "duckduckgo"
            packageName?.contains("kiwi") == true -> "kiwi"
            else -> "generic"
        }
    }
    
    private fun extractChromeData(intent: Intent, uri: Uri, originalUrl: String): BrowserData {
        val cookies = mutableMapOf<String, String>()
        val headers = mutableMapOf<String, String>()
        
        // Chrome-specific data extraction
        intent.getStringExtra("com.android.browser.application_id")?.let {
            headers["X-Chrome-Application-ID"] = it
        }
        
        // Chrome stores referrer differently
        val referrer = intent.getStringExtra("android.intent.extra.REFERRER") 
            ?: intent.getStringExtra("extra_referrer")
            ?: uri.getQueryParameter("referrer")
        
        // Chrome user agent
        val userAgent = intent.getStringExtra("user_agent") 
            ?: "Mozilla/5.0 (Linux; Android ${android.os.Build.VERSION.RELEASE}) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
        
        // Extract cookies from Chrome's format
        intent.extras?.keySet()?.forEach { key ->
            if (key.startsWith("cookie_") || key.contains("session")) {
                intent.getStringExtra(key)?.let { value ->
                    cookies[key] = value
                }
            }
        }
        
        return BrowserData(
            sourceApp = "chrome",
            originalUrl = originalUrl,
            userAgent = userAgent,
            cookies = cookies,
            headers = headers,
            referrer = referrer
        )
    }
    
    private fun extractFirefoxData(intent: Intent, uri: Uri, originalUrl: String): BrowserData {
        val cookies = mutableMapOf<String, String>()
        val headers = mutableMapOf<String, String>()
        
        // Firefox-specific handling
        val referrer = intent.getStringExtra("org.mozilla.gecko.REFERRER")
            ?: intent.getStringExtra("referrer")
            ?: uri.getQueryParameter("ref")
        
        val userAgent = intent.getStringExtra("user_agent")
            ?: "Mozilla/5.0 (Mobile; rv:120.0) Gecko/120.0 Firefox/120.0"
        
        return BrowserData(
            sourceApp = "firefox",
            originalUrl = originalUrl,
            userAgent = userAgent,
            cookies = cookies,
            headers = headers,
            referrer = referrer
        )
    }
    
    private fun extractEdgeData(intent: Intent, uri: Uri, originalUrl: String): BrowserData {
        val userAgent = intent.getStringExtra("user_agent")
            ?: "Mozilla/5.0 (Linux; Android ${android.os.Build.VERSION.RELEASE}) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36 EdgA/120.0.0.0"
        
        return BrowserData(
            sourceApp = "edge",
            originalUrl = originalUrl,
            userAgent = userAgent,
            referrer = intent.getStringExtra("referrer")
        )
    }
    
    private fun extractOperaData(intent: Intent, uri: Uri, originalUrl: String): BrowserData {
        val userAgent = intent.getStringExtra("user_agent")
            ?: "Mozilla/5.0 (Linux; Android ${android.os.Build.VERSION.RELEASE}) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36 OPR/81.0.0.0"
        
        return BrowserData(
            sourceApp = "opera",
            originalUrl = originalUrl,
            userAgent = userAgent,
            referrer = intent.getStringExtra("referrer")
        )
    }
    
    private fun extractSamsungBrowserData(intent: Intent, uri: Uri, originalUrl: String): BrowserData {
        val userAgent = intent.getStringExtra("user_agent")
            ?: "Mozilla/5.0 (Linux; Android ${android.os.Build.VERSION.RELEASE}) AppleWebKit/537.36 (KHTML, like Gecko) SamsungBrowser/23.0 Chrome/120.0.0.0 Mobile Safari/537.36"
        
        return BrowserData(
            sourceApp = "samsung",
            originalUrl = originalUrl,
            userAgent = userAgent,
            referrer = intent.getStringExtra("referrer")
        )
    }
    
    private fun extractBraveData(intent: Intent, uri: Uri, originalUrl: String): BrowserData {
        val userAgent = intent.getStringExtra("user_agent")
            ?: "Mozilla/5.0 (Linux; Android ${android.os.Build.VERSION.RELEASE}) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
        
        return BrowserData(
            sourceApp = "brave",
            originalUrl = originalUrl,
            userAgent = userAgent,
            referrer = intent.getStringExtra("referrer")
        )
    }
    
    private fun extractGenericBrowserData(intent: Intent, uri: Uri, originalUrl: String): BrowserData {
        val userAgent = intent.getStringExtra("user_agent")
            ?: "Mozilla/5.0 (Linux; Android ${android.os.Build.VERSION.RELEASE}) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
        
        val cookies = mutableMapOf<String, String>()
        val headers = mutableMapOf<String, String>()
        
        // Extract all possible cookie and header data
        intent.extras?.keySet()?.forEach { key ->
            intent.getStringExtra(key)?.let { value ->
                when {
                    key.contains("cookie", ignoreCase = true) -> cookies[key] = value
                    key.contains("header", ignoreCase = true) -> headers[key] = value
                    key.startsWith("http_", ignoreCase = true) -> headers[key] = value
                }
            }
        }
        
        return BrowserData(
            sourceApp = "generic",
            originalUrl = originalUrl,
            userAgent = userAgent,
            cookies = cookies,
            headers = headers,
            referrer = intent.getStringExtra("referrer") ?: intent.getStringExtra("referer")
        )
    }
    
    fun isVideoUrl(url: String): Boolean {
        return videoPatterns.any { it.matches(url) }
    }
    
    fun extractPotentialVideoUrls(content: String): List<String> {
        val urls = mutableListOf<String>()
        
        // URL extraction patterns
        val urlPatterns = listOf(
            Regex("https?://[^\\s\"'<>]+\\.(mp4|mkv|avi|mov|wmv|flv|webm|m4v|3gp|ts|m2ts|m3u8|mpd)(?:[^\\s\"'<>]*)?", RegexOption.IGNORE_CASE),
            Regex("https?://[^\\s\"'<>]*(?:video|stream|media|player|embed)[^\\s\"'<>]*", RegexOption.IGNORE_CASE),
            Regex("blob:[^\\s\"'<>]+", RegexOption.IGNORE_CASE)
        )
        
        urlPatterns.forEach { pattern ->
            pattern.findAll(content).forEach { match ->
                val url = match.value
                if (isVideoUrl(url)) {
                    urls.add(url)
                }
            }
        }
        
        return urls.distinct()
    }
    
    fun shouldForceAppSelection(): Boolean {
        // Always show our app in the selection dialog for video URLs
        return true
    }
}