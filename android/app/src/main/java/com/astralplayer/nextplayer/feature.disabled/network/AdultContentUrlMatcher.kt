package com.astralplayer.nextplayer.feature.network

import android.net.Uri
import android.util.Log
import java.util.regex.Pattern

/**
 * Comprehensive URL pattern matcher for adult content sites
 * Handles the top free adult content platforms with video streaming capabilities
 */
class AdultContentUrlMatcher {
    
    companion object {
        private const val TAG = "AdultContentUrlMatcher"
        
        // Adult content site domains and their patterns
        private val ADULT_SITE_PATTERNS = mapOf(
            // PornHub and variants
            "pornhub.com" to listOf(
                Pattern.compile("https?://(?:www\\.)?pornhub\\.com/view_video\\.php\\?viewkey=([^&]+)"),
                Pattern.compile("https?://(?:www\\.)?pornhub\\.com/embed/([^/?]+)"),
                Pattern.compile("https?://rt\\.pornhub\\.com/view_video\\.php\\?viewkey=([^&]+)")
            ),
            
            // XVideos
            "xvideos.com" to listOf(
                Pattern.compile("https?://(?:www\\.)?xvideos\\.com/video([0-9]+)/([^/?]+)"),
                Pattern.compile("https?://(?:www\\.)?xvideos\\.com/embedframe/([0-9]+)")
            ),
            
            // XNXX
            "xnxx.com" to listOf(
                Pattern.compile("https?://(?:www\\.)?xnxx\\.com/video-([^/]+)/([^/?]+)"),
                Pattern.compile("https?://(?:www\\.)?xnxx\\.com/embedframe/([0-9]+)")
            ),
            
            // XHamster
            "xhamster.com" to listOf(
                Pattern.compile("https?://(?:www\\.)?xhamster\\.com/videos/([^/?]+)"),
                Pattern.compile("https?://(?:www\\.)?xhamster\\.com/embed/([0-9]+)")
            ),
            
            // RedTube
            "redtube.com" to listOf(
                Pattern.compile("https?://(?:www\\.)?redtube\\.com/([0-9]+)"),
                Pattern.compile("https?://embed\\.redtube\\.com/\\?id=([0-9]+)")
            ),
            
            // YouPorn
            "youporn.com" to listOf(
                Pattern.compile("https?://(?:www\\.)?youporn\\.com/watch/([0-9]+)/([^/?]+)"),
                Pattern.compile("https?://(?:www\\.)?youporn\\.com/embed/([0-9]+)")
            ),
            
            // SpankBang
            "spankbang.com" to listOf(
                Pattern.compile("https?://(?:www\\.)?spankbang\\.com/([^/]+)/video/([^/?]+)"),
                Pattern.compile("https?://(?:www\\.)?spankbang\\.com/([^/]+)/embed")
            ),
            
            // PornHD
            "pornhd.com" to listOf(
                Pattern.compile("https?://(?:www\\.)?pornhd\\.com/videos/([0-9]+)/([^/?]+)"),
                Pattern.compile("https?://(?:www\\.)?pornhd\\.com/embed/([0-9]+)")
            ),
            
            // Tube8
            "tube8.com" to listOf(
                Pattern.compile("https?://(?:www\\.)?tube8\\.com/([^/]+)/([^/]+)/([0-9]+)"),
                Pattern.compile("https?://(?:www\\.)?tube8\\.com/embed/([^/]+)/([^/]+)/([0-9]+)")
            ),
            
            // Beeg
            "beeg.com" to listOf(
                Pattern.compile("https?://(?:www\\.)?beeg\\.com/([0-9]+)"),
                Pattern.compile("https?://(?:www\\.)?beeg\\.com/embed/([0-9]+)")
            ),
            
            // TNAFlix
            "tnaflix.com" to listOf(
                Pattern.compile("https?://(?:www\\.)?tnaflix\\.com/([^/]+)/video([0-9]+)"),
                Pattern.compile("https?://player\\.tnaflix\\.com/video/([0-9]+)")
            ),
            
            // DrTuber
            "drtuber.com" to listOf(
                Pattern.compile("https?://(?:www\\.)?drtuber\\.com/video/([0-9]+)/([^/?]+)"),
                Pattern.compile("https?://(?:www\\.)?drtuber\\.com/embed/([0-9]+)")
            ),
            
            // Txxx
            "txxx.com" to listOf(
                Pattern.compile("https?://(?:www\\.)?txxx\\.com/videos/([0-9]+)/([^/?]+)"),
                Pattern.compile("https?://(?:www\\.)?txxx\\.com/embed/([0-9]+)")
            ),
            
            // HQPorner
            "hqporner.com" to listOf(
                Pattern.compile("https?://(?:www\\.)?hqporner\\.com/hdporn/([0-9]+)-([^/?]+)\\.html"),
                Pattern.compile("https?://(?:www\\.)?hqporner\\.com/embed/([0-9]+)")
            ),
            
            // EPorner
            "eporner.com" to listOf(
                Pattern.compile("https?://(?:www\\.)?eporner\\.com/video-([^/]+)/([^/?]+)"),
                Pattern.compile("https?://(?:www\\.)?eporner\\.com/embed/([^/]+)")
            ),
            
            // GotPorn
            "gotporn.com" to listOf(
                Pattern.compile("https?://(?:www\\.)?gotporn\\.com/video/([0-9]+)/([^/?]+)"),
                Pattern.compile("https?://(?:www\\.)?gotporn\\.com/embed/([0-9]+)")
            ),
            
            // Fapdu
            "fapdu.com" to listOf(
                Pattern.compile("https?://(?:www\\.)?fapdu\\.com/([^/?]+)"),
                Pattern.compile("https?://(?:www\\.)?fapdu\\.com/embed/([^/?]+)")
            ),
            
            // Upornia
            "upornia.com" to listOf(
                Pattern.compile("https?://(?:www\\.)?upornia\\.com/videos/([0-9]+)/([^/?]+)"),
                Pattern.compile("https?://(?:www\\.)?upornia\\.com/embed/([0-9]+)")
            )
        )
        
        // Direct video file patterns
        private val DIRECT_VIDEO_PATTERNS = listOf(
            Pattern.compile(".*\\.(mp4|mkv|avi|mov|wmv|flv|webm|m4v|3gp|ts)$", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*\\.(m3u8|mpd)$", Pattern.CASE_INSENSITIVE)
        )
        
        // Common video URL indicators
        private val VIDEO_URL_INDICATORS = listOf(
            "video", "watch", "embed", "player", "stream", "play",
            "viewkey", "video_id", "v=", "id=", "watch?v"
        )
    }
    
    /**
     * Check if a URL is from a supported adult content site
     */
    fun isAdultContentUrl(url: String): Boolean {
        return try {
            val uri = Uri.parse(url)
            val host = uri.host?.lowercase() ?: return false
            
            // Remove www. prefix for matching
            val cleanHost = host.removePrefix("www.")
            
            val isAdultSite = ADULT_SITE_PATTERNS.keys.any { domain ->
                cleanHost == domain || cleanHost.endsWith(".$domain")
            }
            
            Log.d(TAG, "URL: $url, Host: $host, IsAdultSite: $isAdultSite")
            isAdultSite
        } catch (e: Exception) {
            Log.e(TAG, "Error checking adult content URL: $url", e)
            false
        }
    }
    
    /**
     * Check if a URL contains video content indicators
     */
    fun hasVideoIndicators(url: String): Boolean {
        val urlLower = url.lowercase()
        return VIDEO_URL_INDICATORS.any { indicator ->
            urlLower.contains(indicator)
        }
    }
    
    /**
     * Check if URL is a direct video file
     */
    fun isDirectVideoFile(url: String): Boolean {
        return DIRECT_VIDEO_PATTERNS.any { pattern ->
            pattern.matcher(url).matches()
        }
    }
    
    /**
     * Extract video ID from supported adult content URLs
     */
    fun extractVideoId(url: String): String? {
        return try {
            val uri = Uri.parse(url)
            val host = uri.host?.lowercase()?.removePrefix("www.") ?: return null
            
            ADULT_SITE_PATTERNS[host]?.forEach { pattern ->
                val matcher = pattern.matcher(url)
                if (matcher.find()) {
                    return matcher.group(1)
                }
            }
            
            null
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting video ID from: $url", e)
            null
        }
    }
    
    /**
     * Get the site type from URL
     */
    fun getSiteType(url: String): AdultSiteType {
        return try {
            val uri = Uri.parse(url)
            val host = uri.host?.lowercase()?.removePrefix("www.") ?: return AdultSiteType.UNKNOWN
            
            when {
                host.contains("pornhub") -> AdultSiteType.PORNHUB
                host.contains("xvideos") -> AdultSiteType.XVIDEOS
                host.contains("xnxx") -> AdultSiteType.XNXX
                host.contains("xhamster") -> AdultSiteType.XHAMSTER
                host.contains("redtube") -> AdultSiteType.REDTUBE
                host.contains("youporn") -> AdultSiteType.YOUPORN
                host.contains("spankbang") -> AdultSiteType.SPANKBANG
                host.contains("pornhd") -> AdultSiteType.PORNHD
                host.contains("tube8") -> AdultSiteType.TUBE8
                host.contains("beeg") -> AdultSiteType.BEEG
                host.contains("tnaflix") -> AdultSiteType.TNAFLIX
                host.contains("drtuber") -> AdultSiteType.DRTUBER
                host.contains("txxx") -> AdultSiteType.TXXX
                host.contains("hqporner") -> AdultSiteType.HQPORNER
                host.contains("eporner") -> AdultSiteType.EPORNER
                host.contains("gotporn") -> AdultSiteType.GOTPORN
                host.contains("fapdu") -> AdultSiteType.FAPDU
                host.contains("upornia") -> AdultSiteType.UPORNIA
                else -> AdultSiteType.UNKNOWN
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error determining site type for: $url", e)
            AdultSiteType.UNKNOWN
        }
    }
    
    /**
     * Check if URL should be handled by the player
     */
    fun shouldHandleUrl(url: String): Boolean {
        return isAdultContentUrl(url) || hasVideoIndicators(url) || isDirectVideoFile(url)
    }
    
    /**
     * Get user-friendly site name
     */
    fun getSiteName(url: String): String {
        return when (getSiteType(url)) {
            AdultSiteType.PORNHUB -> "PornHub"
            AdultSiteType.XVIDEOS -> "XVideos"
            AdultSiteType.XNXX -> "XNXX"
            AdultSiteType.XHAMSTER -> "xHamster"
            AdultSiteType.REDTUBE -> "RedTube"
            AdultSiteType.YOUPORN -> "YouPorn"
            AdultSiteType.SPANKBANG -> "SpankBang"
            AdultSiteType.PORNHD -> "PornHD"
            AdultSiteType.TUBE8 -> "Tube8"
            AdultSiteType.BEEG -> "Beeg"
            AdultSiteType.TNAFLIX -> "TNAFlix"
            AdultSiteType.DRTUBER -> "DrTuber"
            AdultSiteType.TXXX -> "Txxx"
            AdultSiteType.HQPORNER -> "HQPorner"
            AdultSiteType.EPORNER -> "EPorner"
            AdultSiteType.GOTPORN -> "GotPorn"
            AdultSiteType.FAPDU -> "Fapdu"
            AdultSiteType.UPORNIA -> "Upornia"
            AdultSiteType.UNKNOWN -> {
                try {
                    Uri.parse(url).host?.removePrefix("www.") ?: "Unknown Site"
                } catch (e: Exception) {
                    "Unknown Site"
                }
            }
        }
    }
    
    /**
     * Generate expected embed URL pattern for a site
     */
    fun getEmbedUrlPattern(siteType: AdultSiteType, videoId: String): String? {
        return when (siteType) {
            AdultSiteType.PORNHUB -> "https://www.pornhub.com/embed/$videoId"
            AdultSiteType.XVIDEOS -> "https://www.xvideos.com/embedframe/$videoId"
            AdultSiteType.XNXX -> "https://www.xnxx.com/embedframe/$videoId"
            AdultSiteType.XHAMSTER -> "https://www.xhamster.com/embed/$videoId"
            AdultSiteType.REDTUBE -> "https://embed.redtube.com/?id=$videoId"
            AdultSiteType.YOUPORN -> "https://www.youporn.com/embed/$videoId"
            AdultSiteType.SPANKBANG -> "https://www.spankbang.com/$videoId/embed"
            AdultSiteType.PORNHD -> "https://www.pornhd.com/embed/$videoId"
            AdultSiteType.TUBE8 -> null // Tube8 has complex embed patterns
            AdultSiteType.BEEG -> "https://www.beeg.com/embed/$videoId"
            AdultSiteType.TNAFLIX -> "https://player.tnaflix.com/video/$videoId"
            AdultSiteType.DRTUBER -> "https://www.drtuber.com/embed/$videoId"
            AdultSiteType.TXXX -> "https://www.txxx.com/embed/$videoId"
            AdultSiteType.HQPORNER -> "https://www.hqporner.com/embed/$videoId"
            AdultSiteType.EPORNER -> "https://www.eporner.com/embed/$videoId"
            AdultSiteType.GOTPORN -> "https://www.gotporn.com/embed/$videoId"
            AdultSiteType.FAPDU -> "https://www.fapdu.com/embed/$videoId"
            AdultSiteType.UPORNIA -> "https://www.upornia.com/embed/$videoId"
            AdultSiteType.UNKNOWN -> null
        }
    }
}

/**
 * Enum representing different adult content site types
 */
enum class AdultSiteType {
    PORNHUB,
    XVIDEOS,
    XNXX,
    XHAMSTER,
    REDTUBE,
    YOUPORN,
    SPANKBANG,
    PORNHD,
    TUBE8,
    BEEG,
    TNAFLIX,
    DRTUBER,
    TXXX,
    HQPORNER,
    EPORNER,
    GOTPORN,
    FAPDU,
    UPORNIA,
    UNKNOWN
}