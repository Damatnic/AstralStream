package com.astralplayer.nextplayer.playlist

import android.content.Context
import android.net.Uri
import com.astralplayer.nextplayer.data.PlaylistVideo
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlin.math.*

/**
 * Advanced recommendation engine for intelligent playlist suggestions
 */
class PlaylistRecommendationEngine(
    private val context: Context
) {
    
    private val _recommendations = MutableStateFlow<List<RecommendationGroup>>(emptyList())
    val recommendations: StateFlow<List<RecommendationGroup>> = _recommendations.asStateFlow()
    
    private val userPreferences = mutableMapOf<String, Float>()
    private val viewingHistory = mutableListOf<ViewingHistoryEntry>()
    private val contentAnalysis = mutableMapOf<String, ContentFeatures>()
    private val collaborativeData = mutableMapOf<String, UserProfile>()
    
    private val recommendationScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var isInitialized = false
    
    /**
     * Initialize recommendation engine
     */
    suspend fun initialize() {
        isInitialized = true
        
        // Load user preferences
        loadUserPreferences()
        
        // Load viewing history
        loadViewingHistory()
        
        // Initialize content analysis
        initializeContentAnalysis()
        
        // Start recommendation updates
        startRecommendationUpdates()
    }
    
    /**
     * Generate recommendations for a specific playlist
     */
    suspend fun generateRecommendations(
        playlist: AdvancedPlaylist,
        maxRecommendations: Int = 20
    ): List<PlaylistRecommendation> {
        if (!isInitialized) return emptyList()
        
        val recommendations = mutableListOf<PlaylistRecommendation>()
        
        // Content-based recommendations
        recommendations.addAll(generateContentBasedRecommendations(playlist, maxRecommendations / 4))
        
        // Collaborative filtering recommendations
        recommendations.addAll(generateCollaborativeRecommendations(playlist, maxRecommendations / 4))
        
        // Trend-based recommendations
        recommendations.addAll(generateTrendingRecommendations(playlist, maxRecommendations / 4))
        
        // Context-aware recommendations
        recommendations.addAll(generateContextAwareRecommendations(playlist, maxRecommendations / 4))
        
        // Sort by score and remove duplicates
        return recommendations
            .distinctBy { it.video.uri }
            .sortedByDescending { it.score }
            .take(maxRecommendations)
    }
    
    /**
     * Generate smart playlist completion suggestions
     */
    suspend fun generateCompletionSuggestions(
        playlist: AdvancedPlaylist,
        targetDuration: Long? = null,
        targetCount: Int? = null
    ): List<PlaylistRecommendation> {
        val currentDuration = playlist.videos.sumOf { it.duration }
        val currentCount = playlist.videos.size
        
        val remainingDuration = targetDuration?.minus(currentDuration) ?: Long.MAX_VALUE
        val remainingCount = targetCount?.minus(currentCount) ?: Int.MAX_VALUE
        
        if (remainingDuration <= 0 || remainingCount <= 0) {
            return emptyList()
        }
        
        // Generate recommendations that fit the remaining space
        val allRecommendations = generateRecommendations(playlist, 100)
        val completionSuggestions = mutableListOf<PlaylistRecommendation>()
        var accumulatedDuration = 0L
        var addedCount = 0
        
        for (recommendation in allRecommendations) {
            if (addedCount >= remainingCount) break
            if (accumulatedDuration + recommendation.video.duration > remainingDuration) continue
            
            completionSuggestions.add(recommendation.copy(
                reason = "${recommendation.reason} (Fits remaining ${(remainingDuration - accumulatedDuration) / 60000}min)"
            ))
            
            accumulatedDuration += recommendation.video.duration
            addedCount++
        }
        
        return completionSuggestions
    }
    
    /**
     * Generate recommendations based on listening mood
     */
    suspend fun generateMoodBasedRecommendations(
        mood: ListeningMood,
        duration: Long? = null
    ): List<PlaylistRecommendation> {
        val moodFeatures = getMoodFeatures(mood)
        val availableVideos = getAllAvailableVideos()
        
        return availableVideos.mapNotNull { video ->
            val videoFeatures = getContentFeatures(video)
            val compatibilityScore = calculateMoodCompatibility(moodFeatures, videoFeatures)
            
            if (compatibilityScore > 0.6f) {
                PlaylistRecommendation(
                    video = video,
                    score = compatibilityScore,
                    reason = "Matches ${mood.name.lowercase()} mood"
                )
            } else null
        }.sortedByDescending { it.score }
            .let { recommendations ->
                duration?.let { targetDuration ->
                    filterByDuration(recommendations, targetDuration)
                } ?: recommendations
            }
    }
    
    /**
     * Generate activity-based recommendations
     */
    suspend fun generateActivityBasedRecommendations(
        activity: Activity,
        context: RecommendationContext = RecommendationContext()
    ): List<PlaylistRecommendation> {
        val activityProfile = getActivityProfile(activity)
        val availableVideos = getAllAvailableVideos()
        
        return availableVideos.mapNotNull { video ->
            val videoFeatures = getContentFeatures(video)
            val activityScore = calculateActivityCompatibility(activityProfile, videoFeatures)
            val contextScore = calculateContextScore(video, context)
            
            val combinedScore = (activityScore * 0.7f) + (contextScore * 0.3f)
            
            if (combinedScore > 0.5f) {
                PlaylistRecommendation(
                    video = video,
                    score = combinedScore,
                    reason = "Perfect for ${activity.name.lowercase()}"
                )
            } else null
        }.sortedByDescending { it.score }
    }
    
    /**
     * Generate similar playlists recommendations
     */
    suspend fun generateSimilarPlaylistRecommendations(
        sourcePlaylist: AdvancedPlaylist,
        allPlaylists: List<AdvancedPlaylist>
    ): List<SimilarPlaylistRecommendation> {
        val sourceFeatures = extractPlaylistFeatures(sourcePlaylist)
        
        return allPlaylists.filter { it.id != sourcePlaylist.id }
            .map { playlist ->
                val playlistFeatures = extractPlaylistFeatures(playlist)
                val similarity = calculatePlaylistSimilarity(sourceFeatures, playlistFeatures)
                
                SimilarPlaylistRecommendation(
                    playlist = playlist,
                    similarity = similarity,
                    commonFeatures = findCommonFeatures(sourceFeatures, playlistFeatures),
                    reason = generateSimilarityReason(sourceFeatures, playlistFeatures)
                )
            }
            .filter { it.similarity > 0.4f }
            .sortedByDescending { it.similarity }
    }
    
    /**
     * Update user preferences based on interactions
     */
    suspend fun updateUserPreferences(
        video: PlaylistVideo,
        interaction: UserInteraction,
        strength: Float = 1.0f
    ) {
        val videoFeatures = getContentFeatures(video)
        
        // Update genre preferences
        val genreKey = "genre_${video.genre}"
        val currentPreference = userPreferences[genreKey] ?: 0.5f
        val adjustment = when (interaction) {
            UserInteraction.LIKED -> 0.1f * strength
            UserInteraction.DISLIKED -> -0.1f * strength
            UserInteraction.SKIPPED -> -0.05f * strength
            UserInteraction.COMPLETED -> 0.05f * strength
            UserInteraction.REPEATED -> 0.15f * strength
        }
        
        userPreferences[genreKey] = (currentPreference + adjustment).coerceIn(0f, 1f)
        
        // Update duration preferences
        val durationCategory = getDurationCategory(video.duration)
        val durationKey = "duration_$durationCategory"
        val currentDurationPreference = userPreferences[durationKey] ?: 0.5f
        userPreferences[durationKey] = (currentDurationPreference + adjustment * 0.5f).coerceIn(0f, 1f)
        
        // Record viewing history
        recordViewingHistory(video, interaction)
        
        // Trigger recommendation update
        updateRecommendations()
    }
    
    /**
     * Get personalized recommendation groups
     */
    suspend fun getPersonalizedRecommendationGroups(): List<RecommendationGroup> {
        val groups = mutableListOf<RecommendationGroup>()
        
        // Recently liked content
        val recentlyLiked = getRecentlyLikedRecommendations()
        if (recentlyLiked.isNotEmpty()) {
            groups.add(RecommendationGroup(
                title = "More Like Recently Liked",
                recommendations = recentlyLiked,
                type = RecommendationGroupType.RECENTLY_LIKED
            ))
        }
        
        // Trending in preferred genres  
        val trendingInGenres = getTrendingInPreferredGenres()
        if (trendingInGenres.isNotEmpty()) {
            groups.add(RecommendationGroup(
                title = "Trending in Your Genres",
                recommendations = trendingInGenres,
                type = RecommendationGroupType.TRENDING_GENRES
            ))
        }
        
        // Discovery recommendations
        val discovery = getDiscoveryRecommendations()
        if (discovery.isNotEmpty()) {
            groups.add(RecommendationGroup(
                title = "Discover Something New",
                recommendations = discovery,
                type = RecommendationGroupType.DISCOVERY
            ))
        }
        
        // Time-based recommendations
        val timeBasedGroup = getTimeBasedRecommendations()
        if (timeBasedGroup != null) {
            groups.add(timeBasedGroup)
        }
        
        return groups
    }
    
    // Private implementation methods
    private suspend fun loadUserPreferences() {
        // Load from storage or initialize defaults
        userPreferences["genre_Action"] = 0.7f
        userPreferences["genre_Comedy"] = 0.8f
        userPreferences["genre_Drama"] = 0.6f
        userPreferences["duration_short"] = 0.8f
        userPreferences["duration_medium"] = 0.7f
        userPreferences["duration_long"] = 0.5f
    }
    
    private suspend fun loadViewingHistory() {
        // Load viewing history from storage
        delay(50) // Simulate loading
    }
    
    private suspend fun initializeContentAnalysis() {
        // Initialize content analysis system
        delay(50) // Simulate initialization
    }
    
    private fun startRecommendationUpdates() {
        recommendationScope.launch {
            while (isInitialized && currentCoroutineContext().isActive) {
                try {
                    updateRecommendations()
                    delay(1800000) // Update every 30 minutes
                } catch (e: Exception) {
                    // Handle error but continue
                }
            }
        }
    }
    
    private suspend fun generateContentBasedRecommendations(
        playlist: AdvancedPlaylist,
        maxCount: Int
    ): List<PlaylistRecommendation> {
        val playlistFeatures = extractPlaylistFeatures(playlist)
        val availableVideos = getAllAvailableVideos()
        
        return availableVideos.mapNotNull { video ->
            if (playlist.videos.any { it.uri == video.uri }) return@mapNotNull null
            
            val videoFeatures = getContentFeatures(video)
            val similarity = calculateContentSimilarity(playlistFeatures, videoFeatures)
            
            if (similarity > 0.6f) {
                PlaylistRecommendation(
                    video = video,
                    score = similarity,
                    reason = "Similar to your playlist content"
                )
            } else null
        }.sortedByDescending { it.score }
          .take(maxCount)
    }
    
    private suspend fun generateCollaborativeRecommendations(
        playlist: AdvancedPlaylist,
        maxCount: Int
    ): List<PlaylistRecommendation> {
        // Find users with similar playlists
        val similarUsers = findSimilarUsers(playlist)
        val recommendations = mutableListOf<PlaylistRecommendation>()
        
        similarUsers.forEach { user ->
            val userPlaylists = getUserPlaylists(user.userId)
            userPlaylists.forEach { userPlaylist ->
                userPlaylist.videos.forEach { video ->
                    if (playlist.videos.none { it.uri == video.uri }) {
                        recommendations.add(PlaylistRecommendation(
                            video = video,
                            score = user.similarity * 0.8f,
                            reason = "Liked by users with similar taste"
                        ))
                    }
                }
            }
        }
        
        return recommendations.distinctBy { it.video.uri }
            .sortedByDescending { it.score }
            .take(maxCount)
    }
    
    private suspend fun generateTrendingRecommendations(
        playlist: AdvancedPlaylist,
        maxCount: Int
    ): List<PlaylistRecommendation> {
        val trendingVideos = getTrendingVideos()
        val playlistGenres = playlist.videos.map { it.genre }.distinct()
        
        return trendingVideos.filter { video ->
            video.genre in playlistGenres && 
            playlist.videos.none { it.uri == video.uri }
        }.map { video ->
            PlaylistRecommendation(
                video = video,
                score = 0.75f,
                reason = "Currently trending in ${video.genre}"
            )
        }.take(maxCount)
    }
    
    private suspend fun generateContextAwareRecommendations(
        playlist: AdvancedPlaylist,
        maxCount: Int
    ): List<PlaylistRecommendation> {
        val currentTime = System.currentTimeMillis()
        val timeOfDay = getTimeOfDay(currentTime)
        val dayOfWeek = getDayOfWeek(currentTime)
        
        val contextualVideos = getContextualVideos(timeOfDay, dayOfWeek)
        
        return contextualVideos.filter { video ->
            playlist.videos.none { it.uri == video.uri }
        }.map { video ->
            PlaylistRecommendation(
                video = video,
                score = 0.7f,
                reason = "Popular at this time"
            )
        }.take(maxCount)
    }
    
    private fun getMoodFeatures(mood: ListeningMood): MoodFeatures {
        return when (mood) {
            ListeningMood.ENERGETIC -> MoodFeatures(
                energy = 0.9f,
                valence = 0.8f,
                tempo = 0.8f,
                danceability = 0.9f
            )
            ListeningMood.RELAXED -> MoodFeatures(
                energy = 0.2f,
                valence = 0.6f,
                tempo = 0.3f,
                danceability = 0.2f
            )
            ListeningMood.FOCUSED -> MoodFeatures(
                energy = 0.5f,
                valence = 0.5f,
                tempo = 0.4f,
                danceability = 0.3f
            )
            ListeningMood.HAPPY -> MoodFeatures(
                energy = 0.7f,
                valence = 0.9f,
                tempo = 0.7f,
                danceability = 0.7f
            )
            ListeningMood.MELANCHOLIC -> MoodFeatures(
                energy = 0.3f,
                valence = 0.2f,
                tempo = 0.4f,
                danceability = 0.2f
            )
        }
    }
    
    private fun getActivityProfile(activity: Activity): ActivityProfile {
        return when (activity) {
            Activity.WORKOUT -> ActivityProfile(
                energyLevel = 0.9f,
                focusRequired = 0.3f,
                backgroundFriendly = true,
                preferredDuration = 180000L // 3 minutes
            )
            Activity.STUDY -> ActivityProfile(
                energyLevel = 0.4f,
                focusRequired = 0.2f,
                backgroundFriendly = true,
                preferredDuration = 600000L // 10 minutes
            )
            Activity.RELAXATION -> ActivityProfile(
                energyLevel = 0.2f,
                focusRequired = 0.1f,
                backgroundFriendly = true,
                preferredDuration = 900000L // 15 minutes
            )
            Activity.COMMUTE -> ActivityProfile(
                energyLevel = 0.6f,
                focusRequired = 0.5f,
                backgroundFriendly = false,
                preferredDuration = 300000L // 5 minutes
            )
        }
    }
    
    private fun calculateMoodCompatibility(moodFeatures: MoodFeatures, videoFeatures: ContentFeatures): Float {
        val energyMatch = 1f - abs(moodFeatures.energy - videoFeatures.energy)
        val valenceMatch = 1f - abs(moodFeatures.valence - videoFeatures.valence)
        val tempoMatch = 1f - abs(moodFeatures.tempo - videoFeatures.tempo)
        
        return (energyMatch + valenceMatch + tempoMatch) / 3f
    }
    
    private fun calculateActivityCompatibility(activityProfile: ActivityProfile, videoFeatures: ContentFeatures): Float {
        val energyMatch = 1f - abs(activityProfile.energyLevel - videoFeatures.energy)
        val focusMatch = if (activityProfile.backgroundFriendly) {
            1f - videoFeatures.complexity * activityProfile.focusRequired
        } else 1f
        
        return (energyMatch + focusMatch) / 2f
    }
    
    private fun calculateContextScore(video: PlaylistVideo, context: RecommendationContext): Float {
        var score = 0.5f
        
        // Time-based scoring
        val timeOfDay = getTimeOfDay(System.currentTimeMillis())
        score += getTimeOfDayBonus(video, timeOfDay)
        
        // Location-based scoring (if available)
        context.location?.let { location ->
            score += getLocationBonus(video, location)
        }
        
        // Device-based scoring
        score += getDeviceBonus(video, context.deviceType)
        
        return score.coerceIn(0f, 1f)
    }
    
    private fun extractPlaylistFeatures(playlist: AdvancedPlaylist): PlaylistFeatures {
        val videos = playlist.videos
        if (videos.isEmpty()) return PlaylistFeatures()
        
        val genres = videos.map { it.genre }
        val durations = videos.map { it.duration }
        
        return PlaylistFeatures(
            dominantGenres = genres.groupBy { it }.mapValues { it.value.size }
                .toList().sortedByDescending { it.second }.take(3).map { it.first },
            averageDuration = durations.average().toLong(),
            durationVariance = calculateVariance(durations.map { it.toFloat() }),
            totalDuration = durations.sum(),
            videoCount = videos.size,
            genreDiversity = genres.distinct().size.toFloat() / genres.size
        )
    }
    
    private fun calculatePlaylistSimilarity(features1: PlaylistFeatures, features2: PlaylistFeatures): Float {
        val genreSimilarity = calculateGenreSimilarity(features1.dominantGenres, features2.dominantGenres)
        val durationSimilarity = calculateDurationSimilarity(features1.averageDuration, features2.averageDuration)
        val diversitySimilarity = 1f - abs(features1.genreDiversity - features2.genreDiversity)
        
        return (genreSimilarity * 0.5f + durationSimilarity * 0.3f + diversitySimilarity * 0.2f)
    }
    
    private fun calculateGenreSimilarity(genres1: List<String>, genres2: List<String>): Float {
        val intersection = genres1.intersect(genres2.toSet()).size
        val union = genres1.union(genres2).size
        return if (union > 0) intersection.toFloat() / union else 0f
    }
    
    private fun calculateDurationSimilarity(duration1: Long, duration2: Long): Float {
        val maxDuration = maxOf(duration1, duration2)
        val minDuration = minOf(duration1, duration2)
        return minDuration.toFloat() / maxDuration
    }
    
    private fun findCommonFeatures(features1: PlaylistFeatures, features2: PlaylistFeatures): List<String> {
        val commonFeatures = mutableListOf<String>()
        
        val commonGenres = features1.dominantGenres.intersect(features2.dominantGenres.toSet())
        if (commonGenres.isNotEmpty()) {
            commonFeatures.add("Similar genres: ${commonGenres.joinToString(", ")}")
        }
        
        val durationDiff = abs(features1.averageDuration - features2.averageDuration)
        if (durationDiff < 60000) { // Within 1 minute
            commonFeatures.add("Similar video lengths")
        }
        
        return commonFeatures
    }
    
    private fun generateSimilarityReason(features1: PlaylistFeatures, features2: PlaylistFeatures): String {
        val commonFeatures = findCommonFeatures(features1, features2)
        return when {
            commonFeatures.isNotEmpty() -> commonFeatures.first()
            else -> "Similar content style"
        }
    }
    
    private fun getContentFeatures(video: PlaylistVideo): ContentFeatures {
        return contentAnalysis.getOrPut(video.uri.toString()) {
            // Analyze content features (simplified)
            ContentFeatures(
                energy = (0.3f..0.9f).random(),
                valence = (0.2f..0.8f).random(),
                tempo = (0.3f..0.8f).random(),
                complexity = (0.2f..0.7f).random(),
                genre = video.genre,
                duration = video.duration
            )
        }
    }
    
    private fun calculateContentSimilarity(playlistFeatures: PlaylistFeatures, videoFeatures: ContentFeatures): Float {
        val genreMatch = if (videoFeatures.genre in playlistFeatures.dominantGenres) 0.8f else 0.3f
        val durationMatch = calculateDurationMatch(playlistFeatures.averageDuration, videoFeatures.duration)
        
        return (genreMatch + durationMatch) / 2f
    }
    
    private fun calculateDurationMatch(averageDuration: Long, videoDuration: Long): Float {
        val ratio = minOf(averageDuration, videoDuration).toFloat() / maxOf(averageDuration, videoDuration)
        return ratio
    }
    
    private fun recordViewingHistory(video: PlaylistVideo, interaction: UserInteraction) {
        viewingHistory.add(ViewingHistoryEntry(
            video = video,
            interaction = interaction,
            timestamp = System.currentTimeMillis()
        ))
        
        // Keep history manageable
        if (viewingHistory.size > 1000) {
            viewingHistory.removeAt(0)
        }
    }
    
    private suspend fun updateRecommendations() {
        val groups = getPersonalizedRecommendationGroups()
        _recommendations.value = groups
    }
    
    // Helper methods for recommendation generation
    private fun getAllAvailableVideos(): List<PlaylistVideo> {
        // Return available videos from content library
        return listOf(
            PlaylistVideo(Uri.parse("content://video1"), "Action Movie", 7200000L, "Action"),
            PlaylistVideo(Uri.parse("content://video2"), "Comedy Show", 1800000L, "Comedy"),
            PlaylistVideo(Uri.parse("content://video3"), "Drama Series", 3600000L, "Drama")
        )
    }
    
    private fun getTrendingVideos(): List<PlaylistVideo> {
        return getAllAvailableVideos().shuffled().take(10)
    }
    
    private fun getContextualVideos(timeOfDay: TimeOfDay, dayOfWeek: DayOfWeek): List<PlaylistVideo> {
        return getAllAvailableVideos().filter { video ->
            when (timeOfDay) {
                TimeOfDay.MORNING -> video.genre in listOf("News", "Educational")
                TimeOfDay.AFTERNOON -> video.genre in listOf("Documentary", "Drama")
                TimeOfDay.EVENING -> video.genre in listOf("Comedy", "Entertainment")
                TimeOfDay.NIGHT -> video.genre in listOf("Relaxing", "Ambient")
            }
        }
    }
    
    private fun findSimilarUsers(playlist: AdvancedPlaylist): List<SimilarUser> {
        // Simplified similar user finding
        return listOf(
            SimilarUser("user1", 0.8f),
            SimilarUser("user2", 0.7f)
        )
    }
    
    private fun getUserPlaylists(userId: String): List<AdvancedPlaylist> {
        // Return user's playlists
        return emptyList()
    }
    
    private fun getRecentlyLikedRecommendations(): List<PlaylistRecommendation> {
        val recentlyLiked = viewingHistory.filter { 
            it.interaction == UserInteraction.LIKED && 
            System.currentTimeMillis() - it.timestamp < 604800000 // 7 days
        }.map { it.video }
        
        return getAllAvailableVideos().filter { video ->
            recentlyLiked.any { liked -> liked.genre == video.genre && liked.uri != video.uri }
        }.map { video ->
            PlaylistRecommendation(
                video = video,
                score = 0.8f,
                reason = "Similar to recently liked content"
            )
        }.take(10)
    }
    
    private fun getTrendingInPreferredGenres(): List<PlaylistRecommendation> {
        val preferredGenres = userPreferences.filter { it.key.startsWith("genre_") && it.value > 0.6f }
            .map { it.key.removePrefix("genre_") }
        
        return getTrendingVideos().filter { it.genre in preferredGenres }
            .map { video ->
                PlaylistRecommendation(
                    video = video,
                    score = 0.75f,
                    reason = "Trending in ${video.genre}"
                )
            }
    }
    
    private fun getDiscoveryRecommendations(): List<PlaylistRecommendation> {
        val unknownGenres = getAllAvailableVideos().map { it.genre }.distinct()
            .filter { genre -> 
                userPreferences["genre_$genre"]?.let { it < 0.3f } ?: true
            }
        
        return getAllAvailableVideos().filter { it.genre in unknownGenres }
            .shuffled()
            .take(5)
            .map { video ->
                PlaylistRecommendation(
                    video = video,
                    score = 0.6f,
                    reason = "Discover ${video.genre} content"
                )
            }
    }
    
    private fun getTimeBasedRecommendations(): RecommendationGroup? {
        val timeOfDay = getTimeOfDay(System.currentTimeMillis())
        val timeBasedVideos = getContextualVideos(timeOfDay, getDayOfWeek(System.currentTimeMillis()))
        
        if (timeBasedVideos.isEmpty()) return null
        
        val recommendations = timeBasedVideos.take(8).map { video ->
            PlaylistRecommendation(
                video = video,
                score = 0.7f,
                reason = "Perfect for ${timeOfDay.name.lowercase()}"
            )
        }
        
        return RecommendationGroup(
            title = getTimeBasedTitle(timeOfDay),
            recommendations = recommendations,
            type = RecommendationGroupType.TIME_BASED
        )
    }
    
    private fun getTimeOfDay(timestamp: Long): TimeOfDay {
        val hour = java.util.Calendar.getInstance().apply { 
            timeInMillis = timestamp 
        }.get(java.util.Calendar.HOUR_OF_DAY)
        
        return when (hour) {
            in 5..11 -> TimeOfDay.MORNING
            in 12..17 -> TimeOfDay.AFTERNOON
            in 18..22 -> TimeOfDay.EVENING
            else -> TimeOfDay.NIGHT
        }
    }
    
    private fun getDayOfWeek(timestamp: Long): DayOfWeek {
        val day = java.util.Calendar.getInstance().apply { 
            timeInMillis = timestamp 
        }.get(java.util.Calendar.DAY_OF_WEEK)
        
        return when (day) {
            java.util.Calendar.MONDAY -> DayOfWeek.MONDAY
            java.util.Calendar.TUESDAY -> DayOfWeek.TUESDAY
            java.util.Calendar.WEDNESDAY -> DayOfWeek.WEDNESDAY
            java.util.Calendar.THURSDAY -> DayOfWeek.THURSDAY
            java.util.Calendar.FRIDAY -> DayOfWeek.FRIDAY
            java.util.Calendar.SATURDAY -> DayOfWeek.SATURDAY
            else -> DayOfWeek.SUNDAY
        }
    }
    
    private fun getTimeBasedTitle(timeOfDay: TimeOfDay): String {
        return when (timeOfDay) {
            TimeOfDay.MORNING -> "Good Morning Picks"
            TimeOfDay.AFTERNOON -> "Afternoon Favorites"
            TimeOfDay.EVENING -> "Evening Entertainment"
            TimeOfDay.NIGHT -> "Late Night Relaxation"
        }
    }
    
    private fun getDurationCategory(duration: Long): String {
        return when {
            duration < 300000 -> "short" // < 5 minutes
            duration < 1800000 -> "medium" // < 30 minutes
            else -> "long"
        }
    }
    
    private fun getTimeOfDayBonus(video: PlaylistVideo, timeOfDay: TimeOfDay): Float {
        // Simple time-based bonus
        return 0.1f
    }
    
    private fun getLocationBonus(video: PlaylistVideo, location: String): Float {
        // Location-based bonus (simplified)
        return 0.05f
    }
    
    private fun getDeviceBonus(video: PlaylistVideo, deviceType: DeviceType): Float {
        return when (deviceType) {
            DeviceType.PHONE -> if (video.duration < 600000) 0.1f else -0.05f
            DeviceType.TABLET -> 0.05f
            DeviceType.TV -> if (video.duration > 1800000) 0.1f else 0f
        }
    }
    
    private fun filterByDuration(
        recommendations: List<PlaylistRecommendation>,
        targetDuration: Long
    ): List<PlaylistRecommendation> {
        val filtered = mutableListOf<PlaylistRecommendation>()
        var accumulatedDuration = 0L
        
        for (recommendation in recommendations) {
            if (accumulatedDuration + recommendation.video.duration <= targetDuration) {
                filtered.add(recommendation)
                accumulatedDuration += recommendation.video.duration
            }
        }
        
        return filtered
    }
    
    private fun calculateVariance(values: List<Float>): Float {
        if (values.isEmpty()) return 0f
        val mean = values.average().toFloat()
        val variance = values.map { (it - mean).pow(2) }.average().toFloat()
        return variance
    }
    
    fun cleanup() {
        isInitialized = false
        recommendationScope.cancel()
        userPreferences.clear()
        viewingHistory.clear()
        contentAnalysis.clear()
        collaborativeData.clear()
    }
}

// Data classes and enums for recommendation engine
enum class ListeningMood { ENERGETIC, RELAXED, FOCUSED, HAPPY, MELANCHOLIC }
enum class Activity { WORKOUT, STUDY, RELAXATION, COMMUTE }
enum class UserInteraction { LIKED, DISLIKED, SKIPPED, COMPLETED, REPEATED }
enum class TimeOfDay { MORNING, AFTERNOON, EVENING, NIGHT }
enum class DayOfWeek { MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY }
enum class DeviceType { PHONE, TABLET, TV }
enum class RecommendationGroupType { RECENTLY_LIKED, TRENDING_GENRES, DISCOVERY, TIME_BASED, MOOD_BASED, ACTIVITY_BASED }

data class MoodFeatures(
    val energy: Float,
    val valence: Float,
    val tempo: Float,
    val danceability: Float
)

data class ActivityProfile(
    val energyLevel: Float,
    val focusRequired: Float,
    val backgroundFriendly: Boolean,
    val preferredDuration: Long
)

data class ContentFeatures(
    val energy: Float = 0.5f,
    val valence: Float = 0.5f,
    val tempo: Float = 0.5f,
    val complexity: Float = 0.5f,
    val genre: String = "Unknown",
    val duration: Long = 0L
)

data class PlaylistFeatures(
    val dominantGenres: List<String> = emptyList(),
    val averageDuration: Long = 0L,
    val durationVariance: Float = 0f,
    val totalDuration: Long = 0L,
    val videoCount: Int = 0,
    val genreDiversity: Float = 0f
)

data class UserProfile(
    val userId: String,
    val preferences: Map<String, Float>,
    val viewingHistory: List<ViewingHistoryEntry>
)

data class ViewingHistoryEntry(
    val video: PlaylistVideo,
    val interaction: UserInteraction,
    val timestamp: Long
)

data class RecommendationContext(
    val location: String? = null,
    val deviceType: DeviceType = DeviceType.PHONE,
    val networkQuality: String = "good",
    val batteryLevel: Float = 1.0f
)

data class RecommendationGroup(
    val title: String,
    val recommendations: List<PlaylistRecommendation>,
    val type: RecommendationGroupType
)

data class SimilarPlaylistRecommendation(
    val playlist: AdvancedPlaylist,
    val similarity: Float,
    val commonFeatures: List<String>,
    val reason: String
)

data class SimilarUser(
    val userId: String,
    val similarity: Float
)