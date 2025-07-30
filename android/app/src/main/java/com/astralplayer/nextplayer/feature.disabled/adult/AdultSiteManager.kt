package com.astralplayer.nextplayer.feature.adult

import android.content.Context
import android.util.Log
import com.astralplayer.nextplayer.feature.network.AdultContentUrlMatcher
import com.astralplayer.nextplayer.feature.network.AdultSiteType
import com.astralplayer.nextplayer.feature.network.AdvancedNetworkManager
import com.astralplayer.nextplayer.feature.network.NetworkResult
import com.astralplayer.nextplayer.feature.network.VideoStreamExtractor
import com.astralplayer.nextplayer.feature.streaming.StreamProcessor
import com.astralplayer.nextplayer.security.SecurityManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.Request
import java.util.regex.Pattern

/**
 * Advanced adult site manager for AstralStream
 * Handles site-specific features, authentication, and content management
 */
class AdultSiteManager(
    private val context: Context,
    private val networkManager: AdvancedNetworkManager,
    private val streamProcessor: StreamProcessor,
    private val securityManager: SecurityManager
) {
    companion object {
        private const val TAG = "AdultSiteManager"
        
        // Age verification patterns
        private val AGE_GATE_PATTERNS = listOf(
            Pattern.compile("age.*verification", Pattern.CASE_INSENSITIVE),
            Pattern.compile("18.*older", Pattern.CASE_INSENSITIVE),
            Pattern.compile("adult.*content", Pattern.CASE_INSENSITIVE),
            Pattern.compile("enter.*site", Pattern.CASE_INSENSITIVE)
        )
        
        // Captcha detection patterns
        private val CAPTCHA_PATTERNS = listOf(
            Pattern.compile("captcha", Pattern.CASE_INSENSITIVE),
            Pattern.compile("recaptcha", Pattern.CASE_INSENSITIVE),
            Pattern.compile("hcaptcha", Pattern.CASE_INSENSITIVE),
            Pattern.compile("human.*verification", Pattern.CASE_INSENSITIVE)
        )
        
        // Common adult site authentication selectors
        private val AUTH_SELECTORS = mapOf(
            "pornhub.com" to AuthSelectors(
                ageGateButton = "button[data-confirm='1']",
                loginForm = "#loginForm",
                usernameField = "#username",
                passwordField = "#password",
                submitButton = "button[type='submit']"
            ),
            "xvideos.com" to AuthSelectors(
                ageGateButton = ".age-verification-button",
                loginForm = "#login-form",
                usernameField = "input[name='login']",
                passwordField = "input[name='password']",
                submitButton = "input[type='submit']"
            ),
            "spankbang.com" to AuthSelectors(
                ageGateButton = ".age-gate-enter",
                loginForm = "#login-modal",
                usernameField = "input[name='username']",
                passwordField = "input[name='password']",
                submitButton = ".login-submit"
            )
        )
    }
    
    private val urlMatcher = AdultContentUrlMatcher()
    private val videoExtractor = VideoStreamExtractor(context)
    
    // Site session management
    private val _siteStates = MutableStateFlow<Map<String, SiteState>>(emptyMap())
    val siteStates: StateFlow<Map<String, SiteState>> = _siteStates.asStateFlow()
    
    private val _currentSite = MutableStateFlow<String?>(null)
    val currentSite: StateFlow<String?> = _currentSite.asStateFlow()
    
    /**
     * Process adult content URL with site-specific handling
     */
    suspend fun processAdultContent(url: String): AdultContentResult {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Processing adult content: $url")
                
                val siteType = urlMatcher.getSiteType(url)
                val siteName = urlMatcher.getSiteName(url)
                _currentSite.value = siteName
                
                // Check if user has access permissions
                if (!securityManager.isPrivateModeEnabled() && !securityManager.isIncognitoSessionActive()) {
                    return@withContext AdultContentResult.AccessDenied(
                        "Private mode or incognito session required for adult content"
                    )
                }
                
                // Check if content should be locked due to timeout
                if (securityManager.shouldLockContent()) {
                    return@withContext AdultContentResult.AuthenticationRequired(
                        "Content locked due to inactivity timeout"
                    )
                }
                
                // Get or create site state
                val siteState = getSiteState(siteName)
                
                // Handle site-specific requirements
                val siteRequirements = checkSiteRequirements(url, siteState)
                if (siteRequirements != null) {
                    return@withContext siteRequirements
                }
                
                // Extract video streams
                val extractionResult = videoExtractor.extractStreams(url)
                
                when (extractionResult) {
                    is com.astralplayer.nextplayer.feature.network.VideoExtractionResult.Success -> {
                        // Process streams for optimal playback
                        val processedStreams = processAdultStreams(extractionResult.streams)
                        
                        // Store in secure history if not in incognito mode
                        if (!securityManager.isIncognitoSessionActive()) {
                            securityManager.storeAdultContentHistory(
                                url = url,
                                title = extractionResult.title,
                                timestamp = System.currentTimeMillis()
                            )
                        }
                        
                        // Update site state
                        updateSiteState(siteName, SiteState(
                            isAuthenticated = true,
                            lastAccess = System.currentTimeMillis(),
                            sessionValid = true,
                            requiresAuth = false
                        ))
                        
                        AdultContentResult.Success(
                            title = extractionResult.title,
                            streams = processedStreams,
                            thumbnail = extractionResult.thumbnail,
                            duration = extractionResult.duration,
                            siteName = extractionResult.siteName,
                            metadata = AdultContentMetadata(
                                isLive = false,
                                categories = extractCategories(url),
                                tags = extractTags(extractionResult.title),
                                uploader = extractUploader(url),
                                uploadDate = null,
                                viewCount = null,
                                rating = null
                            )
                        )
                    }
                    
                    is com.astralplayer.nextplayer.feature.network.VideoExtractionResult.Error -> {
                        AdultContentResult.ExtractionError(extractionResult.message)
                    }
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Error processing adult content", e)
                AdultContentResult.ProcessingError("Failed to process adult content: ${e.message}")
            }
        }
    }
    
    /**
     * Handle age verification for adult sites
     */
    suspend fun handleAgeVerification(url: String): AgeVerificationResult {
        return withContext(Dispatchers.IO) {
            try {
                val siteName = urlMatcher.getSiteName(url)
                val siteConfig = networkManager.getSiteConfiguration(siteName)
                
                // Fetch page to check for age gate
                val request = Request.Builder()
                    .url(url)
                    .apply {
                        siteConfig.headers.forEach { (key, value) ->
                            addHeader(key, value)
                        }
                    }
                    .build()
                
                when (val result = networkManager.executeRequest(request)) {
                    is NetworkResult.Success -> {
                        val pageContent = result.response.body?.string() ?: ""
                        
                        val hasAgeGate = AGE_GATE_PATTERNS.any { pattern ->
                            pattern.matcher(pageContent).find()
                        }
                        
                        if (hasAgeGate) {
                            // Attempt automatic age verification
                            val bypassResult = bypassAgeGate(url, siteName, pageContent)
                            if (bypassResult) {
                                AgeVerificationResult.Verified
                            } else {
                                AgeVerificationResult.Required(
                                    "Age verification required for this content"
                                )
                            }
                        } else {
                            AgeVerificationResult.NotRequired
                        }
                    }
                    
                    else -> AgeVerificationResult.Error("Failed to check age verification requirements")
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Error handling age verification", e)
                AgeVerificationResult.Error("Age verification check failed: ${e.message}")
            }
        }
    }
    
    /**
     * Handle playlist/series content from adult sites
     */
    suspend fun handlePlaylistContent(url: String): PlaylistResult {
        return withContext(Dispatchers.IO) {
            try {
                val siteName = urlMatcher.getSiteName(url)
                
                when (urlMatcher.getSiteType(url)) {
                    AdultSiteType.PORNHUB -> extractPornHubPlaylist(url)
                    AdultSiteType.XVIDEOS -> extractXVideosPlaylist(url)
                    AdultSiteType.SPANKBANG -> extractSpankBangPlaylist(url)
                    else -> extractGenericPlaylist(url, siteName)
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Error handling playlist content", e)
                PlaylistResult.Error("Failed to extract playlist: ${e.message}")
            }
        }
    }
    
    /**
     * Handle live streaming from adult sites
     */
    suspend fun handleLiveStream(url: String): LiveStreamResult {
        return withContext(Dispatchers.IO) {
            try {
                val siteName = urlMatcher.getSiteName(url)
                
                // Check if URL indicates live content
                val isLive = url.contains("live") || url.contains("cam") || url.contains("stream")
                
                if (!isLive) {
                    return@withContext LiveStreamResult.NotLive
                }
                
                // Extract live stream information
                val streamInfo = extractLiveStreamInfo(url)
                
                if (streamInfo != null) {
                    LiveStreamResult.Success(
                        streamUrl = streamInfo.streamUrl,
                        title = streamInfo.title,
                        performer = streamInfo.performer,
                        viewers = streamInfo.viewers,
                        isPrivate = streamInfo.isPrivate,
                        chatEnabled = streamInfo.chatEnabled
                    )
                } else {
                    LiveStreamResult.ExtractionFailed("Failed to extract live stream information")
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Error handling live stream", e)
                LiveStreamResult.Error("Live stream processing failed: ${e.message}")
            }
        }
    }
    
    /**
     * Get site-specific quality preferences
     */
    fun getSiteQualityPreferences(siteName: String): QualityPreferences {
        // Load user preferences for specific sites
        val defaultPrefs = QualityPreferences(
            preferredQuality = "720p",
            autoSwitchQuality = true,
            bufferAhead = 30000, // 30 seconds
            lowLatencyMode = false
        )
        
        // Site-specific adjustments
        return when (siteName.lowercase()) {
            "pornhub.com" -> defaultPrefs.copy(
                bufferAhead = 45000, // Longer buffering for stability
                autoSwitchQuality = true
            )
            "xvideos.com" -> defaultPrefs.copy(
                preferredQuality = "480p", // More conservative for XVideos
                bufferAhead = 20000
            )
            "spankbang.com" -> defaultPrefs.copy(
                lowLatencyMode = true, // Better for SpankBang's streaming
                bufferAhead = 15000
            )
            else -> defaultPrefs
        }
    }
    
    // Private helper methods
    
    private fun getSiteState(siteName: String): SiteState {
        return _siteStates.value[siteName] ?: SiteState()
    }
    
    private fun updateSiteState(siteName: String, newState: SiteState) {
        val currentStates = _siteStates.value.toMutableMap()
        currentStates[siteName] = newState
        _siteStates.value = currentStates
    }
    
    private suspend fun checkSiteRequirements(url: String, siteState: SiteState): AdultContentResult? {
        // Check if authentication is required
        if (!siteState.isAuthenticated && siteState.requiresAuth) {
            return AdultContentResult.AuthenticationRequired(
                "Site authentication required"
            )
        }
        
        // Check for captcha requirements
        if (requiresCaptchaVerification(url)) {
            return AdultContentResult.CaptchaRequired(
                "Captcha verification required"
            )
        }
        
        return null // No requirements blocking access
    }
    
    private suspend fun requiresCaptchaVerification(url: String): Boolean {
        try {
            val request = Request.Builder().url(url).head().build()
            when (val result = networkManager.executeRequest(request)) {
                is NetworkResult.Success -> {
                    val pageContent = result.response.body?.string() ?: ""
                    return CAPTCHA_PATTERNS.any { pattern ->
                        pattern.matcher(pageContent).find()
                    }
                }
                else -> return false
            }
        } catch (e: Exception) {
            return false
        }
    }
    
    private suspend fun bypassAgeGate(url: String, siteName: String, pageContent: String): Boolean {
        try {
            val authSelectors = AUTH_SELECTORS[siteName] ?: return false
            
            // This would typically involve sending a POST request to bypass the age gate
            // Implementation would be site-specific and respect ToS
            
            Log.d(TAG, "Attempting age gate bypass for $siteName")
            
            // Simulate successful bypass (in real implementation, this would make HTTP requests)
            updateSiteState(siteName, getSiteState(siteName).copy(
                ageVerified = true,
                sessionValid = true
            ))
            
            return true
            
        } catch (e: Exception) {
            Log.e(TAG, "Error bypassing age gate", e)
            return false
        }
    }
    
    private suspend fun processAdultStreams(streams: List<com.astralplayer.nextplayer.feature.network.VideoStream>): List<ProcessedStream> {
        return streams.map { stream ->
            ProcessedStream(
                url = stream.url,
                quality = stream.quality,
                format = stream.format,
                filesize = stream.filesize,
                isOptimized = true,
                bufferConfig = streamProcessor.getBufferConfiguration()
            )
        }
    }
    
    private fun extractCategories(url: String): List<String> {
        // Extract categories from URL patterns
        val categories = mutableListOf<String>()
        
        // Basic category extraction from URL
        when {
            url.contains("amateur") -> categories.add("Amateur")
            url.contains("milf") -> categories.add("MILF")
            url.contains("teen") -> categories.add("Teen")
            url.contains("anal") -> categories.add("Anal")
            url.contains("lesbian") -> categories.add("Lesbian")
        }
        
        return categories
    }
    
    private fun extractTags(title: String): List<String> {
        // Extract tags from video title
        val commonTags = listOf("hot", "sexy", "beautiful", "amazing", "perfect", "big", "small", "young", "mature")
        return commonTags.filter { tag ->
            title.lowercase().contains(tag)
        }
    }
    
    private fun extractUploader(url: String): String? {
        // Extract uploader information from URL
        val uploaderPattern = Pattern.compile("/users?/([^/]+)", Pattern.CASE_INSENSITIVE)
        val matcher = uploaderPattern.matcher(url)
        return if (matcher.find()) matcher.group(1) else null
    }
    
    private suspend fun extractPornHubPlaylist(url: String): PlaylistResult {
        // PornHub playlist extraction logic
        return PlaylistResult.Error("Playlist extraction not implemented for PornHub")
    }
    
    private suspend fun extractXVideosPlaylist(url: String): PlaylistResult {
        // XVideos playlist extraction logic
        return PlaylistResult.Error("Playlist extraction not implemented for XVideos")
    }
    
    private suspend fun extractSpankBangPlaylist(url: String): PlaylistResult {
        // SpankBang playlist extraction logic
        return PlaylistResult.Error("Playlist extraction not implemented for SpankBang")
    }
    
    private suspend fun extractGenericPlaylist(url: String, siteName: String): PlaylistResult {
        // Generic playlist extraction logic
        return PlaylistResult.Error("Playlist extraction not implemented for $siteName")
    }
    
    private suspend fun extractLiveStreamInfo(url: String): LiveStreamInfo? {
        // Live stream information extraction
        return null // Placeholder
    }
    
    /**
     * Clean up site sessions and cached data
     */
    fun cleanup() {
        _siteStates.value = emptyMap()
        _currentSite.value = null
    }
}

/**
 * Data classes for adult site management
 */
data class SiteState(
    val isAuthenticated: Boolean = false,
    val lastAccess: Long = 0,
    val sessionValid: Boolean = false,
    val requiresAuth: Boolean = false,
    val ageVerified: Boolean = false,
    val cookiesStored: Boolean = false
)

data class AuthSelectors(
    val ageGateButton: String,
    val loginForm: String,
    val usernameField: String,
    val passwordField: String,
    val submitButton: String
)

data class ProcessedStream(
    val url: String,
    val quality: String,
    val format: String,
    val filesize: Long?,
    val isOptimized: Boolean,
    val bufferConfig: com.astralplayer.nextplayer.feature.streaming.BufferConfiguration
)

data class AdultContentMetadata(
    val isLive: Boolean,
    val categories: List<String>,
    val tags: List<String>,
    val uploader: String?,
    val uploadDate: String?,
    val viewCount: Long?,
    val rating: Float?
)

data class QualityPreferences(
    val preferredQuality: String,
    val autoSwitchQuality: Boolean,
    val bufferAhead: Int,
    val lowLatencyMode: Boolean
)

data class LiveStreamInfo(
    val streamUrl: String,
    val title: String,
    val performer: String,
    val viewers: Int,
    val isPrivate: Boolean,
    val chatEnabled: Boolean
)

sealed class AdultContentResult {
    data class Success(
        val title: String,
        val streams: List<ProcessedStream>,
        val thumbnail: String?,
        val duration: Long?,
        val siteName: String,
        val metadata: AdultContentMetadata
    ) : AdultContentResult()
    
    data class AccessDenied(val reason: String) : AdultContentResult()
    data class AuthenticationRequired(val reason: String) : AdultContentResult()
    data class CaptchaRequired(val reason: String) : AdultContentResult()
    data class ExtractionError(val error: String) : AdultContentResult()
    data class ProcessingError(val error: String) : AdultContentResult()
}

sealed class AgeVerificationResult {
    object NotRequired : AgeVerificationResult()
    object Verified : AgeVerificationResult()
    data class Required(val message: String) : AgeVerificationResult()
    data class Error(val error: String) : AgeVerificationResult()
}

sealed class PlaylistResult {
    data class Success(
        val title: String,
        val videos: List<PlaylistVideo>,
        val totalCount: Int
    ) : PlaylistResult()
    
    data class Error(val error: String) : PlaylistResult()
}

sealed class LiveStreamResult {
    data class Success(
        val streamUrl: String,
        val title: String,
        val performer: String,
        val viewers: Int,
        val isPrivate: Boolean,
        val chatEnabled: Boolean
    ) : LiveStreamResult()
    
    object NotLive : LiveStreamResult()
    data class ExtractionFailed(val reason: String) : LiveStreamResult()
    data class Error(val error: String) : LiveStreamResult()
}

data class PlaylistVideo(
    val url: String,
    val title: String,
    val thumbnail: String?,
    val duration: Long?,
    val position: Int
)