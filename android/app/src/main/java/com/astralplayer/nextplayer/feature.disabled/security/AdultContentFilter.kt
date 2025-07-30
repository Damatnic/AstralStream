package com.astralplayer.nextplayer.feature.security

import android.content.Context
import android.util.Log
import com.astralplayer.nextplayer.feature.network.AdultContentUrlMatcher
import com.astralplayer.nextplayer.security.SecurityManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.regex.Pattern

/**
 * Advanced adult content filter and classification system
 * Provides content analysis, parental controls, and safe browsing features
 */
class AdultContentFilter(private val context: Context) {
    
    companion object {
        private const val TAG = "AdultContentFilter"
        
        // Content rating levels
        const val RATING_SAFE = 0
        const val RATING_SUGGESTIVE = 1
        const val RATING_MATURE = 2
        const val RATING_EXPLICIT = 3
        const val RATING_ADULT_ONLY = 4
        
        // Adult content keywords for text analysis
        private val ADULT_KEYWORDS = setOf(
            "porn", "sex", "xxx", "adult", "nsfw", "nude", "naked", "explicit",
            "erotic", "fetish", "hardcore", "softcore", "amateur", "milf",
            "teen", "mature", "anal", "oral", "lesbian", "gay", "bdsm",
            "webcam", "cam", "live", "strip", "escort", "dating"
        )
        
        // Safe content indicators
        private val SAFE_KEYWORDS = setOf(
            "education", "tutorial", "news", "documentary", "music", "comedy",
            "sports", "cooking", "travel", "technology", "gaming", "review"
        )
        
        // Suspicious URL patterns
        private val SUSPICIOUS_PATTERNS = listOf(
            Pattern.compile("\\b\\d{2,3}xxx\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\badult[a-z]*\\d+\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bsex[a-z]*\\d+\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bporn[a-z]*\\d+\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bxxx[a-z]*\\d+\\b", Pattern.CASE_INSENSITIVE)
        )
    }
    
    private val urlMatcher = AdultContentUrlMatcher()
    private val securityManager = SecurityManager(context)
    
    /**
     * Analyze content and provide safety rating
     */
    suspend fun analyzeContent(
        url: String,
        title: String = "",
        description: String = "",
        thumbnail: String? = null
    ): ContentAnalysisResult {
        return withContext(Dispatchers.Default) {
            try {
                Log.d(TAG, "Analyzing content: $url")
                
                val urlRating = analyzeUrl(url)
                val textRating = analyzeText(title, description)
                val domainRating = analyzeDomain(url)
                
                // Combine ratings (take the highest/most restrictive)
                val overallRating = maxOf(urlRating, textRating, domainRating)
                
                // Determine if content requires authentication
                val requiresAuth = overallRating >= RATING_MATURE && securityManager.isBiometricEnabled()
                
                // Check if content should be blocked
                val isBlocked = shouldBlockContent(overallRating, url)
                
                ContentAnalysisResult(
                    url = url,
                    title = title,
                    rating = overallRating,
                    ratingLabel = getRatingLabel(overallRating),
                    isAdultContent = overallRating >= RATING_MATURE,
                    requiresAuthentication = requiresAuth,
                    isBlocked = isBlocked,
                    reasons = getAnalysisReasons(urlRating, textRating, domainRating),
                    recommendations = getContentRecommendations(overallRating)
                )
                
            } catch (e: Exception) {
                Log.e(TAG, "Error analyzing content", e)
                ContentAnalysisResult(
                    url = url,
                    title = title,
                    rating = RATING_SAFE,
                    ratingLabel = "Unknown",
                    isAdultContent = false,
                    requiresAuthentication = false,
                    isBlocked = false,
                    reasons = listOf("Analysis failed: ${e.message}"),
                    recommendations = emptyList()
                )
            }
        }
    }
    
    /**
     * Analyze URL patterns for adult content indicators
     */
    private fun analyzeUrl(url: String): Int {
        val lowercaseUrl = url.lowercase()
        
        // Check if it's a known adult site
        if (urlMatcher.isAdultContentUrl(url)) {
            return RATING_ADULT_ONLY
        }
        
        // Check for suspicious patterns
        var suspiciousCount = 0
        SUSPICIOUS_PATTERNS.forEach { pattern ->
            if (pattern.matcher(lowercaseUrl).find()) {
                suspiciousCount++
            }
        }
        
        // Check for adult keywords in URL
        var adultKeywordCount = 0
        ADULT_KEYWORDS.forEach { keyword ->
            if (lowercaseUrl.contains(keyword)) {
                adultKeywordCount++
            }
        }
        
        return when {
            suspiciousCount >= 2 || adultKeywordCount >= 3 -> RATING_ADULT_ONLY
            suspiciousCount >= 1 || adultKeywordCount >= 2 -> RATING_EXPLICIT
            adultKeywordCount >= 1 -> RATING_MATURE
            else -> RATING_SAFE
        }
    }
    
    /**
     * Analyze text content (title, description) for adult indicators
     */
    private fun analyzeText(title: String, description: String): Int {
        val combinedText = "$title $description".lowercase()
        
        var adultKeywordCount = 0
        var safeKeywordCount = 0
        
        ADULT_KEYWORDS.forEach { keyword ->
            if (combinedText.contains(keyword)) {
                adultKeywordCount++
            }
        }
        
        SAFE_KEYWORDS.forEach { keyword ->
            if (combinedText.contains(keyword)) {
                safeKeywordCount++
            }
        }
        
        // Safe content indicators can reduce adult rating
        val adjustedAdultCount = maxOf(0, adultKeywordCount - safeKeywordCount)
        
        return when {
            adjustedAdultCount >= 4 -> RATING_ADULT_ONLY
            adjustedAdultCount >= 3 -> RATING_EXPLICIT
            adjustedAdultCount >= 2 -> RATING_MATURE
            adjustedAdultCount >= 1 -> RATING_SUGGESTIVE
            else -> RATING_SAFE
        }
    }
    
    /**
     * Analyze domain reputation and categorization
     */
    private fun analyzeDomain(url: String): Int {
        try {
            val domain = extractDomain(url)
            
            // Check against known adult domain patterns
            val adultDomainPatterns = listOf(
                Pattern.compile(".*porn.*", Pattern.CASE_INSENSITIVE),
                Pattern.compile(".*xxx.*", Pattern.CASE_INSENSITIVE),
                Pattern.compile(".*sex.*", Pattern.CASE_INSENSITIVE),
                Pattern.compile(".*adult.*", Pattern.CASE_INSENSITIVE),
                Pattern.compile(".*cam.*", Pattern.CASE_INSENSITIVE),
                Pattern.compile(".*tube.*", Pattern.CASE_INSENSITIVE)
            )
            
            var matches = 0
            adultDomainPatterns.forEach { pattern ->
                if (pattern.matcher(domain).matches()) {
                    matches++
                }
            }
            
            return when {
                matches >= 2 -> RATING_ADULT_ONLY
                matches >= 1 -> RATING_EXPLICIT
                else -> RATING_SAFE
            }
            
        } catch (e: Exception) {
            Log.w(TAG, "Error analyzing domain", e)
            return RATING_SAFE
        }
    }
    
    /**
     * Determine if content should be blocked based on current settings
     */
    private fun shouldBlockContent(rating: Int, url: String): Boolean {
        // Block if private mode is enabled and it's adult content
        if (securityManager.isPrivateModeEnabled() && rating >= RATING_MATURE) {
            return !securityManager.isIncognitoSessionActive()
        }
        
        // Block if content lock timeout has expired
        if (rating >= RATING_MATURE && securityManager.shouldLockContent()) {
            return true
        }
        
        return false
    }
    
    /**
     * Get human-readable rating label
     */
    private fun getRatingLabel(rating: Int): String {
        return when (rating) {
            RATING_SAFE -> "Safe"
            RATING_SUGGESTIVE -> "Suggestive"
            RATING_MATURE -> "Mature"
            RATING_EXPLICIT -> "Explicit"
            RATING_ADULT_ONLY -> "Adult Only"
            else -> "Unknown"
        }
    }
    
    /**
     * Get analysis reasons for transparency
     */
    private fun getAnalysisReasons(urlRating: Int, textRating: Int, domainRating: Int): List<String> {
        val reasons = mutableListOf<String>()
        
        if (urlRating >= RATING_MATURE) {
            reasons.add("URL contains adult content indicators")
        }
        
        if (textRating >= RATING_MATURE) {
            reasons.add("Title/description contains mature content keywords")
        }
        
        if (domainRating >= RATING_MATURE) {
            reasons.add("Domain is classified as adult content site")
        }
        
        if (reasons.isEmpty()) {
            reasons.add("Content appears to be safe for general audiences")
        }
        
        return reasons
    }
    
    /**
     * Get content recommendations based on rating
     */
    private fun getContentRecommendations(rating: Int): List<String> {
        return when (rating) {
            RATING_SAFE -> listOf("Content is safe for all audiences")
            RATING_SUGGESTIVE -> listOf(
                "Content may contain suggestive themes",
                "Viewer discretion advised"
            )
            RATING_MATURE -> listOf(
                "Content is intended for mature audiences",
                "Consider enabling private mode",
                "Use biometric lock for privacy"
            )
            RATING_EXPLICIT -> listOf(
                "Content contains explicit material",
                "Enable private mode to hide from history",
                "Use incognito session for temporary viewing",
                "Set up biometric authentication"
            )
            RATING_ADULT_ONLY -> listOf(
                "Content is for adults only",
                "Strongly recommend private mode",
                "Use incognito session",
                "Enable all security features",
                "Be aware of data usage and storage"
            )
            else -> emptyList()
        }
    }
    
    /**
     * Check if user can access content based on current security settings
     */
    fun canAccessContent(contentRating: Int): Boolean {
        return when {
            contentRating < RATING_MATURE -> true
            securityManager.isIncognitoSessionActive() -> true
            securityManager.isPrivateModeEnabled() && !securityManager.shouldLockContent() -> true
            else -> false
        }
    }
    
    /**
     * Get content access requirements
     */
    fun getAccessRequirements(contentRating: Int): List<AccessRequirement> {
        val requirements = mutableListOf<AccessRequirement>()
        
        if (contentRating >= RATING_MATURE) {
            if (securityManager.isBiometricEnabled()) {
                requirements.add(AccessRequirement.BIOMETRIC_AUTH)
            }
            
            if (!securityManager.isPrivateModeEnabled()) {
                requirements.add(AccessRequirement.PRIVATE_MODE)
            }
            
            if (securityManager.shouldLockContent()) {
                requirements.add(AccessRequirement.UNLOCK_TIMEOUT)
            }
        }
        
        return requirements
    }
    
    /**
     * Generate content safety report
     */
    suspend fun generateSafetyReport(urls: List<String>): ContentSafetyReport {
        return withContext(Dispatchers.Default) {
            val analyses = urls.map { url ->
                analyzeContent(url)
            }
            
            val totalContent = analyses.size
            val safeContent = analyses.count { it.rating <= RATING_SUGGESTIVE }
            val matureContent = analyses.count { it.rating >= RATING_MATURE }
            val blockedContent = analyses.count { it.isBlocked }
            
            val ratingDistribution = analyses.groupBy { it.rating }
                .mapValues { it.value.size }
            
            ContentSafetyReport(
                totalAnalyzed = totalContent,
                safeContent = safeContent,
                matureContent = matureContent,
                blockedContent = blockedContent,
                ratingDistribution = ratingDistribution,
                recommendations = generateSafetyRecommendations(analyses)
            )
        }
    }
    
    private fun generateSafetyRecommendations(analyses: List<ContentAnalysisResult>): List<String> {
        val recommendations = mutableListOf<String>()
        
        val matureContentRatio = analyses.count { it.rating >= RATING_MATURE }.toFloat() / analyses.size
        
        when {
            matureContentRatio > 0.7f -> {
                recommendations.add("High amount of mature content detected")
                recommendations.add("Strongly recommend enabling all privacy features")
                recommendations.add("Consider using incognito mode for browsing")
            }
            matureContentRatio > 0.3f -> {
                recommendations.add("Moderate amount of mature content detected")
                recommendations.add("Consider enabling private mode")
                recommendations.add("Set up biometric authentication")
            }
            matureContentRatio > 0.1f -> {
                recommendations.add("Some mature content detected")
                recommendations.add("Review privacy settings")
            }
            else -> {
                recommendations.add("Content appears to be mostly safe")
                recommendations.add("Current privacy settings are adequate")
            }
        }
        
        return recommendations
    }
    
    private fun extractDomain(url: String): String {
        return try {
            val cleanUrl = url.removePrefix("http://").removePrefix("https://")
            cleanUrl.split("/")[0].lowercase()
        } catch (e: Exception) {
            url.lowercase()
        }
    }
}

/**
 * Data classes for content analysis
 */
data class ContentAnalysisResult(
    val url: String,
    val title: String,
    val rating: Int,
    val ratingLabel: String,
    val isAdultContent: Boolean,
    val requiresAuthentication: Boolean,
    val isBlocked: Boolean,
    val reasons: List<String>,
    val recommendations: List<String>
) {
    val ratingColor: androidx.compose.ui.graphics.Color
        get() = when (rating) {
            AdultContentFilter.RATING_SAFE -> androidx.compose.ui.graphics.Color.Green
            AdultContentFilter.RATING_SUGGESTIVE -> androidx.compose.ui.graphics.Color.Yellow
            AdultContentFilter.RATING_MATURE -> androidx.compose.ui.graphics.Color(0xFFFF9800) // Orange
            AdultContentFilter.RATING_EXPLICIT -> androidx.compose.ui.graphics.Color(0xFFFF5722) // Deep Orange
            AdultContentFilter.RATING_ADULT_ONLY -> androidx.compose.ui.graphics.Color.Red
            else -> androidx.compose.ui.graphics.Color.Gray
        }
}

data class ContentSafetyReport(
    val totalAnalyzed: Int,
    val safeContent: Int,
    val matureContent: Int,
    val blockedContent: Int,
    val ratingDistribution: Map<Int, Int>,
    val recommendations: List<String>
)

enum class AccessRequirement {
    BIOMETRIC_AUTH,
    PRIVATE_MODE,
    UNLOCK_TIMEOUT,
    PARENTAL_CONSENT
}