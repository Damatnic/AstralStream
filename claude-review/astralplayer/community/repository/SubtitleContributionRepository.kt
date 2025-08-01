package com.astralplayer.community.repository

import android.content.Context
import android.util.Log
import androidx.room.withTransaction
import com.astralplayer.astralstream.data.database.AstralStreamDatabase
import com.astralplayer.community.api.*
import com.astralplayer.community.data.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SubtitleContributionRepository @Inject constructor(
    private val apiManager: CommunityApiManager,
    private val database: AstralStreamDatabase,
    @ApplicationContext private val context: Context
) {
    
    companion object {
        private const val TAG = "SubtitleContributionRepo"
        private const val MIN_REPUTATION_FOR_AUTO_VERIFY = 100
        private const val UPVOTES_FOR_COMMUNITY_VERIFY = 10
        private const val REPUTATION_GAIN_PER_UPVOTE = 5
        private const val REPUTATION_LOSS_PER_DOWNVOTE = 2
    }
    
    /**
     * Contribute a subtitle to the community
     */
    suspend fun contributeSubtitle(
        videoHash: String,
        videoTitle: String,
        videoDuration: Long,
        language: String,
        languageCode: String,
        content: String,
        format: String = "srt",
        confidence: Float = 1.0f,
        description: String = "",
        tags: List<String> = emptyList(),
        source: SubtitleContributionSource = SubtitleContributionSource.USER_CREATED
    ): Result<ContributionResult> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Contributing subtitle for video: $videoHash, language: $language")
            
            // Validate subtitle content
            val validationResult = validateSubtitleContent(content, format)
            if (!validationResult.isValid) {
                return@withContext Result.failure(
                    Exception("Invalid subtitle: ${validationResult.errors.joinToString()}")
                )
            }
            
            // Calculate quality metrics
            val qualityMetrics = calculateQualityMetrics(content, format, videoDuration)
            
            val request = ContributeSubtitleRequest(
                videoHash = videoHash,
                videoTitle = videoTitle,
                videoDuration = videoDuration,
                language = language,
                languageCode = languageCode,
                content = content,
                format = format,
                contributorName = getContributorName(),
                confidence = confidence,
                description = description,
                tags = tags,
                source = source.name
            )
            
            val response = apiManager.contributeSubtitle(request)
            
            if (response.success && response.data != null) {
                // Store contribution locally
                val contributionEntity = CommunitySubtitleEntity(
                    id = response.data.subtitleId,
                    videoHash = videoHash,
                    videoTitle = videoTitle,
                    videoDuration = videoDuration,
                    language = language,
                    languageCode = languageCode,
                    content = content,
                    format = format,
                    contributorId = getCurrentUserId(),
                    contributorName = getContributorName(),
                    contributorReputation = getUserReputation(),
                    confidence = confidence,
                    verificationStatus = when {
                        getUserReputation() >= MIN_REPUTATION_FOR_AUTO_VERIFY -> 
                            SubtitleVerificationStatus.AUTO_VERIFIED
                        else -> SubtitleVerificationStatus.PENDING
                    },
                    qualityScore = qualityMetrics.overallScore,
                    description = description,
                    tags = tags.joinToString(","),
                    source = source,
                    checksum = calculateChecksum(content),
                    fileSize = content.toByteArray().size.toLong(),
                    lineCount = qualityMetrics.lineCount,
                    avgLineLength = qualityMetrics.avgLineLength,
                    hasTimingIssues = qualityMetrics.hasTimingIssues,
                    hasSpellingIssues = qualityMetrics.hasSpellingIssues,
                    completionPercentage = qualityMetrics.completionPercentage
                )
                
                database.communitySubtitleDao().insert(contributionEntity)
                
                Result.success(
                    ContributionResult(
                        subtitleId = response.data.subtitleId,
                        status = response.data.status,
                        message = response.data.message,
                        qualityScore = qualityMetrics.overallScore
                    )
                )
            } else {
                Result.failure(Exception(response.message ?: "Failed to contribute subtitle"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to contribute subtitle", e)
            Result.failure(e)
        }
    }
    
    /**
     * Get community subtitles for a video
     */
    fun getSubtitlesForVideo(
        videoHash: String,
        language: String? = null
    ): Flow<List<CommunitySubtitle>> = flow {
        try {
            Log.d(TAG, "Fetching community subtitles for video: $videoHash")
            
            val response = apiManager.getSubtitlesForVideo(videoHash, language)
            
            if (response.success && response.data != null) {
                val subtitles = response.data.map { subtitle ->
                    CommunitySubtitle(
                        id = subtitle.id,
                        videoHash = subtitle.videoHash,
                        language = subtitle.language,
                        languageCode = subtitle.languageCode,
                        contributorName = subtitle.contributorName,
                        upvotes = subtitle.upvotes,
                        downvotes = subtitle.downvotes,
                        confidence = subtitle.confidence,
                        verificationStatus = SubtitleVerificationStatus.valueOf(
                            subtitle.verificationStatus.uppercase()
                        ),
                        downloadCount = subtitle.downloadCount,
                        qualityScore = subtitle.qualityScore,
                        createdAt = subtitle.createdAt,
                        description = subtitle.description,
                        tags = subtitle.tags,
                        isOfficial = subtitle.isOfficial
                    )
                }
                
                // Update local cache
                updateLocalSubtitleCache(videoHash, subtitles)
                
                emit(subtitles)
            } else {
                // Fallback to local cache
                val localSubtitles = database.communitySubtitleDao()
                    .getSubtitlesForVideo(videoHash, language)
                    .map { entity -> entity.toCommunitySubtitle() }
                
                emit(localSubtitles)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching community subtitles", e)
            // Fallback to local cache
            val localSubtitles = database.communitySubtitleDao()
                .getSubtitlesForVideo(videoHash, language)
                .map { entity -> entity.toCommunitySubtitle() }
            
            emit(localSubtitles)
        }
    }.flowOn(Dispatchers.IO)
    
    /**
     * Vote on a subtitle
     */
    suspend fun voteOnSubtitle(
        subtitleId: String,
        isUpvote: Boolean
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Voting on subtitle: $subtitleId, upvote: $isUpvote")
            
            // Check if user already voted
            val existingVote = database.subtitleVoteDao().getUserVote(subtitleId, getCurrentUserId())
            if (existingVote != null) {
                if (existingVote.isUpvote == isUpvote) {
                    return@withContext Result.failure(Exception("Already voted"))
                }
                // Update existing vote
                database.subtitleVoteDao().update(existingVote.copy(isUpvote = isUpvote))
            } else {
                // Create new vote
                val voteEntity = SubtitleVoteEntity(
                    subtitleId = subtitleId,
                    userId = getCurrentUserId(),
                    isUpvote = isUpvote
                )
                database.subtitleVoteDao().insert(voteEntity)
            }
            
            // Send vote to API
            val voteRequest = SubtitleVoteRequest(
                isUpvote = isUpvote,
                userId = getCurrentUserId()
            )
            
            val response = apiManager.voteOnSubtitle(subtitleId, voteRequest)
            
            if (response.success) {
                // Update local subtitle vote counts
                updateLocalVoteCounts(subtitleId, isUpvote)
                
                // Check if subtitle should be community verified
                checkAndUpdateVerificationStatus(subtitleId)
                
                // Update contributor reputation
                updateContributorReputation(subtitleId, isUpvote)
                
                Result.success(Unit)
            } else {
                Result.failure(Exception(response.message ?: "Failed to vote"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to vote on subtitle", e)
            Result.failure(e)
        }
    }
    
    /**
     * Report a problematic subtitle
     */
    suspend fun reportSubtitle(
        subtitleId: String,
        reason: SubtitleReportReason,
        description: String = ""
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Reporting subtitle: $subtitleId, reason: $reason")
            
            val reportEntity = SubtitleReportEntity(
                subtitleId = subtitleId,
                reporterId = getCurrentUserId(),
                reason = reason,
                description = description
            )
            
            database.subtitleReportDao().insert(reportEntity)
            
            val reportRequest = SubtitleReportRequest(
                reason = reason.name,
                description = description,
                reporterId = getCurrentUserId()
            )
            
            val response = apiManager.reportSubtitle(subtitleId, reportRequest)
            
            if (response.success) {
                // Update report count
                val subtitle = database.communitySubtitleDao().getById(subtitleId)
                subtitle?.let {
                    val updatedSubtitle = it.copy(reportCount = it.reportCount + 1)
                    
                    // Auto-hide if too many reports
                    if (updatedSubtitle.reportCount > 3) {
                        updatedSubtitle.verificationStatus = SubtitleVerificationStatus.HIDDEN
                    }
                    
                    database.communitySubtitleDao().update(updatedSubtitle)
                }
                
                Result.success(Unit)
            } else {
                Result.failure(Exception(response.message ?: "Failed to report"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to report subtitle", e)
            Result.failure(e)
        }
    }
    
    /**
     * Download a subtitle
     */
    suspend fun downloadSubtitle(subtitleId: String): Result<DownloadedSubtitle> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Downloading subtitle: $subtitleId")
            
            // First try to get from local cache
            val localSubtitle = database.communitySubtitleDao().getById(subtitleId)
            if (localSubtitle != null) {
                // Record download
                recordDownload(subtitleId)
                
                return@withContext Result.success(
                    DownloadedSubtitle(
                        content = localSubtitle.content,
                        format = localSubtitle.format,
                        language = localSubtitle.language,
                        languageCode = localSubtitle.languageCode
                    )
                )
            }
            
            // Fallback to API
            val response = apiManager.downloadSubtitle(subtitleId)
            
            if (response.success && response.data != null) {
                recordDownload(subtitleId)
                
                Result.success(
                    DownloadedSubtitle(
                        content = response.data.content,
                        format = response.data.format,
                        language = response.data.language,
                        languageCode = "" // Would be in real API response
                    )
                )
            } else {
                Result.failure(Exception(response.message ?: "Failed to download"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to download subtitle", e)
            Result.failure(e)
        }
    }
    
    /**
     * Get top contributors
     */
    suspend fun getTopContributors(): Result<List<Contributor>> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Fetching top contributors")
            
            val response = apiManager.getTopContributors()
            
            if (response.success && response.data != null) {
                val contributors = response.data.map { contributor ->
                    Contributor(
                        id = contributor.id,
                        name = contributor.name,
                        contributionCount = contributor.contributionCount,
                        averageRating = contributor.averageRating,
                        reputation = contributor.reputation,
                        joinedAt = contributor.joinedAt,
                        isVerified = contributor.isVerified,
                        specialties = contributor.specialties
                    )
                }
                
                Result.success(contributors)
            } else {
                Result.failure(Exception(response.message ?: "Failed to fetch contributors"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch top contributors", e)
            Result.failure(e)
        }
    }
    
    /**
     * Get user's contributions
     */
    fun getMyContributions(): Flow<List<MyContribution>> = flow {
        try {
            val contributions = database.communitySubtitleDao()
                .getUserContributions(getCurrentUserId())
            
            val myContributions = contributions.map { entity ->
                MyContribution(
                    id = entity.id,
                    videoTitle = entity.videoTitle,
                    language = entity.language,
                    upvotes = entity.upvotes,
                    downvotes = entity.downvotes,
                    verificationStatus = entity.verificationStatus,
                    downloadCount = entity.downloadCount,
                    createdAt = entity.createdAt,
                    qualityScore = entity.qualityScore
                )
            }
            
            emit(myContributions)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching my contributions", e)
            emit(emptyList())
        }
    }.flowOn(Dispatchers.IO)
    
    // Private helper methods
    
    private fun validateSubtitleContent(content: String, format: String): ValidationResult {
        val errors = mutableListOf<String>()
        
        if (content.isBlank()) {
            errors.add("Subtitle content cannot be empty")
        }
        
        when (format.lowercase()) {
            "srt" -> {
                // Basic SRT validation
                if (!content.contains("-->")) {
                    errors.add("Invalid SRT format: missing timestamp markers")
                }
            }
            "vtt" -> {
                // Basic VTT validation
                if (!content.startsWith("WEBVTT")) {
                    errors.add("Invalid VTT format: must start with WEBVTT")
                }
            }
        }
        
        return ValidationResult(
            isValid = errors.isEmpty(),
            errors = errors
        )
    }
    
    private fun calculateQualityMetrics(
        content: String,
        format: String,
        videoDuration: Long
    ): QualityMetrics {
        val lines = content.lines()
        val textLines = lines.filter { it.isNotBlank() && !it.contains("-->") && !it.matches(Regex("\\d+")) }
        
        val lineCount = textLines.size
        val avgLineLength = if (textLines.isNotEmpty()) {
            textLines.sumOf { it.length }.toFloat() / textLines.size
        } else 0f
        
        // Check for timing issues (basic check)
        val hasTimingIssues = content.contains("00:00:00") && lines.size < 10
        
        // Basic spell check (would use proper library in production)
        val hasSpellingIssues = false
        
        // Calculate completion percentage based on subtitle density
        val expectedSubtitlesPerMinute = 20 // Rough estimate
        val expectedSubtitles = (videoDuration / 60000) * expectedSubtitlesPerMinute
        val completionPercentage = if (expectedSubtitles > 0) {
            (lineCount.toFloat() / expectedSubtitles * 100).coerceIn(0f, 100f)
        } else 100f
        
        // Calculate overall quality score
        var score = 0.5f // Base score
        
        if (lineCount > 10) score += 0.1f
        if (avgLineLength in 20f..80f) score += 0.2f // Good line length
        if (!hasTimingIssues) score += 0.1f
        if (!hasSpellingIssues) score += 0.1f
        
        return QualityMetrics(
            lineCount = lineCount,
            avgLineLength = avgLineLength,
            hasTimingIssues = hasTimingIssues,
            hasSpellingIssues = hasSpellingIssues,
            completionPercentage = completionPercentage,
            overallScore = score.coerceIn(0f, 1f)
        )
    }
    
    private suspend fun updateLocalSubtitleCache(
        videoHash: String,
        subtitles: List<CommunitySubtitle>
    ) {
        try {
            // Update local database with fresh data from API
            database.withTransaction {
                subtitles.forEach { subtitle ->
                    val existing = database.communitySubtitleDao().getById(subtitle.id)
                    if (existing != null) {
                        // Update existing
                        database.communitySubtitleDao().update(
                            existing.copy(
                                upvotes = subtitle.upvotes,
                                downvotes = subtitle.downvotes,
                                downloadCount = subtitle.downloadCount,
                                verificationStatus = subtitle.verificationStatus
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to update local subtitle cache", e)
        }
    }
    
    private suspend fun updateLocalVoteCounts(subtitleId: String, isUpvote: Boolean) {
        try {
            val subtitle = database.communitySubtitleDao().getById(subtitleId)
            subtitle?.let {
                val updated = if (isUpvote) {
                    it.copy(upvotes = it.upvotes + 1)
                } else {
                    it.copy(downvotes = it.downvotes + 1)
                }
                database.communitySubtitleDao().update(updated)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to update local vote counts", e)
        }
    }
    
    private suspend fun checkAndUpdateVerificationStatus(subtitleId: String) {
        try {
            val subtitle = database.communitySubtitleDao().getById(subtitleId)
            subtitle?.let {
                val netVotes = it.upvotes - it.downvotes
                if (netVotes >= UPVOTES_FOR_COMMUNITY_VERIFY && 
                    it.verificationStatus == SubtitleVerificationStatus.PENDING) {
                    val updated = it.copy(
                        verificationStatus = SubtitleVerificationStatus.COMMUNITY_VERIFIED,
                        verifiedAt = System.currentTimeMillis()
                    )
                    database.communitySubtitleDao().update(updated)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to update verification status", e)
        }
    }
    
    private suspend fun updateContributorReputation(subtitleId: String, isUpvote: Boolean) {
        try {
            val subtitle = database.communitySubtitleDao().getById(subtitleId)
            subtitle?.let {
                val reputationChange = if (isUpvote) {
                    REPUTATION_GAIN_PER_UPVOTE
                } else {
                    -REPUTATION_LOSS_PER_DOWNVOTE
                }
                
                // Update stored reputation (in real app would update user profile)
                val newReputation = (getUserReputation() + reputationChange).coerceAtLeast(0)
                saveUserReputation(newReputation)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to update contributor reputation", e)
        }
    }
    
    private suspend fun recordDownload(subtitleId: String) {
        try {
            val downloadEntity = SubtitleDownloadEntity(
                subtitleId = subtitleId,
                userId = getCurrentUserId(),
                userAgent = "AstralStream Android",
                ipAddress = "" // Would hash IP in production
            )
            
            database.subtitleDownloadDao().insert(downloadEntity)
            
            // Update download count
            val subtitle = database.communitySubtitleDao().getById(subtitleId)
            subtitle?.let {
                database.communitySubtitleDao().update(
                    it.copy(downloadCount = it.downloadCount + 1)
                )
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to record download", e)
        }
    }
    
    private fun getCurrentUserId(): String {
        val prefs = context.getSharedPreferences("community_prefs", Context.MODE_PRIVATE)
        var userId = prefs.getString("user_id", null)
        
        if (userId == null) {
            userId = "user_${System.currentTimeMillis()}_${(Math.random() * 1000).toInt()}"
            prefs.edit().putString("user_id", userId).apply()
        }
        
        return userId
    }
    
    private fun getContributorName(): String {
        val prefs = context.getSharedPreferences("community_prefs", Context.MODE_PRIVATE)
        return prefs.getString("contributor_name", "Anonymous") ?: "Anonymous"
    }
    
    private fun getUserReputation(): Int {
        val prefs = context.getSharedPreferences("community_prefs", Context.MODE_PRIVATE)
        return prefs.getInt("user_reputation", 0)
    }
    
    private fun saveUserReputation(reputation: Int) {
        val prefs = context.getSharedPreferences("community_prefs", Context.MODE_PRIVATE)
        prefs.edit().putInt("user_reputation", reputation).apply()
    }
    
    private fun calculateChecksum(content: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(content.toByteArray())
        return hash.joinToString("") { "%02x".format(it) }
    }
}

// Data classes
data class ContributionResult(
    val subtitleId: String,
    val status: String,
    val message: String,
    val qualityScore: Float
)

data class CommunitySubtitle(
    val id: String,
    val videoHash: String,
    val language: String,
    val languageCode: String,
    val contributorName: String,
    val upvotes: Int,
    val downvotes: Int,
    val confidence: Float,
    val verificationStatus: SubtitleVerificationStatus,
    val downloadCount: Int,
    val qualityScore: Float,
    val createdAt: Long,
    val description: String,
    val tags: List<String>,
    val isOfficial: Boolean
)

data class DownloadedSubtitle(
    val content: String,
    val format: String,
    val language: String,
    val languageCode: String
)

data class Contributor(
    val id: String,
    val name: String,
    val contributionCount: Int,
    val averageRating: Float,
    val reputation: Int,
    val joinedAt: Long,
    val isVerified: Boolean,
    val specialties: List<String>
)

data class MyContribution(
    val id: String,
    val videoTitle: String,
    val language: String,
    val upvotes: Int,
    val downvotes: Int,
    val verificationStatus: SubtitleVerificationStatus,
    val downloadCount: Int,
    val createdAt: Long,
    val qualityScore: Float
)

data class ValidationResult(
    val isValid: Boolean,
    val errors: List<String>
)

data class QualityMetrics(
    val lineCount: Int,
    val avgLineLength: Float,
    val hasTimingIssues: Boolean,
    val hasSpellingIssues: Boolean,
    val completionPercentage: Float,
    val overallScore: Float
)

// Extension function
fun CommunitySubtitleEntity.toCommunitySubtitle(): CommunitySubtitle {
    return CommunitySubtitle(
        id = id,
        videoHash = videoHash,
        language = language,
        languageCode = languageCode,
        contributorName = contributorName,
        upvotes = upvotes,
        downvotes = downvotes,
        confidence = confidence,
        verificationStatus = verificationStatus,
        downloadCount = downloadCount,
        qualityScore = qualityScore,
        createdAt = createdAt,
        description = description,
        tags = tags.split(",").filter { it.isNotBlank() },
        isOfficial = isOfficial
    )
}