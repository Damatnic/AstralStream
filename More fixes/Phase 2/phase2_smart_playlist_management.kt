// ================================
// Smart Playlist Management System
// AI-curated playlists, cross-device sync, collaborative features
// ================================

// 1. Smart Playlist Management Engine
@Singleton
class SmartPlaylistManagementEngine @Inject constructor(
    private val context: Context,
    private val aiPlaylistCurator: AIPlaylistCurator,
    private val cloudSyncService: CloudSyncService,
    private val collaborativePlaylistService: CollaborativePlaylistService,
    private val smartShuffleEngine: SmartShuffleEngine,
    private val playlistAnalyticsService: PlaylistAnalyticsService,
    private val playlistRepository: PlaylistRepository
) {
    
    private var currentPlaylistSession: PlaylistSession? = null
    private var playlistCallbacks: PlaylistManagementCallbacks? = null
    
    suspend fun initializePlaylistManager(callbacks: PlaylistManagementCallbacks): Boolean {
        this.playlistCallbacks = callbacks
        
        return try {
            // Initialize all components
            aiPlaylistCurator.initialize()
            cloudSyncService.initialize()
            collaborativePlaylistService.initialize()
            smartShuffleEngine.initialize()
            
            // Sync playlists from cloud
            syncPlaylistsFromCloud()
            
            callbacks.onPlaylistManagerInitialized()
            true
        } catch (e: Exception) {
            Log.e("SmartPlaylist", "Failed to initialize playlist manager", e)
            false
        }
    }
    
    suspend fun createAIGeneratedPlaylist(
        request: AIPlaylistRequest
    ): AIGeneratedPlaylist {
        return withContext(Dispatchers.Default) {
            val playlist = aiPlaylistCurator.generatePlaylist(request)
            
            // Save playlist
            val savedPlaylist = playlistRepository.savePlaylist(playlist.toRegularPlaylist())
            
            // Upload to cloud if sync enabled
            if (request.syncToCloud) {
                cloudSyncService.uploadPlaylist(savedPlaylist)
            }
            
            playlistCallbacks?.onPlaylistCreated(savedPlaylist)
            playlist
        }
    }
    
    suspend fun createSmartPlaylist(
        name: String,
        rules: List<SmartPlaylistRule>
    ): SmartPlaylist {
        return withContext(Dispatchers.Default) {
            val smartPlaylist = SmartPlaylist(
                id = UUID.randomUUID().toString(),
                name = name,
                rules = rules,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis(),
                isAutoUpdating = true
            )
            
            // Generate initial content
            val content = generateSmartPlaylistContent(smartPlaylist)
            smartPlaylist.currentContent = content
            
            // Save playlist
            playlistRepository.saveSmartPlaylist(smartPlaylist)
            
            // Sync to cloud
            cloudSyncService.uploadSmartPlaylist(smartPlaylist)
            
            playlistCallbacks?.onSmartPlaylistCreated(smartPlaylist)
            smartPlaylist
        }
    }
    
    suspend fun createCollaborativePlaylist(
        name: String,
        description: String,
        participants: List<String>
    ): CollaborativePlaylist {
        return withContext(Dispatchers.Default) {
            val playlist = collaborativePlaylistService.createCollaborativePlaylist(
                name = name,
                description = description,
                creatorId = getCurrentUserId(),
                participants = participants
            )
            
            playlistCallbacks?.onCollaborativePlaylistCreated(playlist)
            playlist
        }
    }
    
    suspend fun updateSmartPlaylists() {
        withContext(Dispatchers.Default) {
            val smartPlaylists = playlistRepository.getAllSmartPlaylists()
            
            smartPlaylists.forEach { smartPlaylist ->
                if (smartPlaylist.isAutoUpdating) {
                    val newContent = generateSmartPlaylistContent(smartPlaylist)
                    
                    if (newContent != smartPlaylist.currentContent) {
                        smartPlaylist.currentContent = newContent
                        smartPlaylist.updatedAt = System.currentTimeMillis()
                        
                        playlistRepository.updateSmartPlaylist(smartPlaylist)
                        cloudSyncService.uploadSmartPlaylist(smartPlaylist)
                        
                        playlistCallbacks?.onSmartPlaylistUpdated(smartPlaylist)
                    }
                }
            }
        }
    }
    
    suspend fun generateSmartShuffle(
        playlist: Playlist,
        shuffleSettings: SmartShuffleSettings
    ): List<MediaItem> {
        return smartShuffleEngine.generateSmartShuffle(playlist, shuffleSettings)
    }
    
    suspend fun getPlaylistRecommendations(
        userId: String,
        maxRecommendations: Int = 5
    ): List<PlaylistRecommendation> {
        return aiPlaylistCurator.generatePlaylistRecommendations(userId, maxRecommendations)
    }
    
    suspend fun analyzePlaylistPerformance(playlistId: String): PlaylistAnalytics {
        return playlistAnalyticsService.analyzePlaylist(playlistId)
    }
    
    suspend fun syncPlaylistsToCloud(): SyncResult {
        return cloudSyncService.syncAllPlaylists()
    }
    
    private suspend fun syncPlaylistsFromCloud() {
        try {
            val cloudPlaylists = cloudSyncService.downloadAllPlaylists()
            cloudPlaylists.forEach { playlist ->
                playlistRepository.savePlaylist(playlist)
            }
            
            val cloudSmartPlaylists = cloudSyncService.downloadAllSmartPlaylists()
            cloudSmartPlaylists.forEach { smartPlaylist ->
                playlistRepository.saveSmartPlaylist(smartPlaylist)
            }
        } catch (e: Exception) {
            Log.e("SmartPlaylist", "Failed to sync from cloud", e)
        }
    }
    
    private suspend fun generateSmartPlaylistContent(smartPlaylist: SmartPlaylist): List<MediaItem> {
        val allMedia = playlistRepository.getAllMediaItems()
        val matchingMedia = mutableListOf<MediaItem>()
        
        allMedia.forEach { mediaItem ->
            if (mediaMatchesRules(mediaItem, smartPlaylist.rules)) {
                matchingMedia.add(mediaItem)
            }
        }
        
        // Apply sorting and limits
        return applySortingAndLimits(matchingMedia, smartPlaylist.rules)
    }
    
    private fun mediaMatchesRules(mediaItem: MediaItem, rules: List<SmartPlaylistRule>): Boolean {
        return rules.all { rule ->
            when (rule.field) {
                SmartPlaylistField.GENRE -> matchesGenreRule(mediaItem, rule)
                SmartPlaylistField.ARTIST -> matchesArtistRule(mediaItem, rule)
                SmartPlaylistField.DURATION -> matchesDurationRule(mediaItem, rule)
                SmartPlaylistField.DATE_ADDED -> matchesDateRule(mediaItem, rule)
                SmartPlaylistField.PLAY_COUNT -> matchesPlayCountRule(mediaItem, rule)
                SmartPlaylistField.RATING -> matchesRatingRule(mediaItem, rule)
                SmartPlaylistField.FILE_SIZE -> matchesFileSizeRule(mediaItem, rule)
                SmartPlaylistField.RESOLUTION -> matchesResolutionRule(mediaItem, rule)
            }
        }
    }
    
    private fun matchesGenreRule(mediaItem: MediaItem, rule: SmartPlaylistRule): Boolean {
        val genre = mediaItem.mediaMetadata.extras?.getString("genre") ?: ""
        return when (rule.operator) {
            SmartPlaylistOperator.EQUALS -> genre.equals(rule.value, ignoreCase = true)
            SmartPlaylistOperator.CONTAINS -> genre.contains(rule.value, ignoreCase = true)
            SmartPlaylistOperator.NOT_EQUALS -> !genre.equals(rule.value, ignoreCase = true)
            else -> false
        }
    }
    
    private fun matchesArtistRule(mediaItem: MediaItem, rule: SmartPlaylistRule): Boolean {
        val artist = mediaItem.mediaMetadata.artist?.toString() ?: ""
        return when (rule.operator) {
            SmartPlaylistOperator.EQUALS -> artist.equals(rule.value, ignoreCase = true)
            SmartPlaylistOperator.CONTAINS -> artist.contains(rule.value, ignoreCase = true)
            SmartPlaylistOperator.NOT_EQUALS -> !artist.equals(rule.value, ignoreCase = true)
            else -> false
        }
    }
    
    private fun matchesDurationRule(mediaItem: MediaItem, rule: SmartPlaylistRule): Boolean {
        val duration = mediaItem.mediaMetadata.extras?.getLong("duration") ?: 0L
        val ruleValue = rule.value.toLongOrNull() ?: 0L
        
        return when (rule.operator) {
            SmartPlaylistOperator.GREATER_THAN -> duration > ruleValue
            SmartPlaylistOperator.LESS_THAN -> duration < ruleValue
            SmartPlaylistOperator.EQUALS -> duration == ruleValue
            else -> false
        }
    }
    
    private fun matchesDateRule(mediaItem: MediaItem, rule: SmartPlaylistRule): Boolean {
        val dateAdded = mediaItem.mediaMetadata.extras?.getLong("date_added") ?: 0L
        val ruleValue = rule.value.toLongOrNull() ?: 0L
        
        return when (rule.operator) {
            SmartPlaylistOperator.GREATER_THAN -> dateAdded > ruleValue
            SmartPlaylistOperator.LESS_THAN -> dateAdded < ruleValue
            SmartPlaylistOperator.EQUALS -> dateAdded == ruleValue
            else -> false
        }
    }
    
    private fun matchesPlayCountRule(mediaItem: MediaItem, rule: SmartPlaylistRule): Boolean {
        val playCount = mediaItem.mediaMetadata.extras?.getInt("play_count") ?: 0
        val ruleValue = rule.value.toIntOrNull() ?: 0
        
        return when (rule.operator) {
            SmartPlaylistOperator.GREATER_THAN -> playCount > ruleValue
            SmartPlaylistOperator.LESS_THAN -> playCount < ruleValue
            SmartPlaylistOperator.EQUALS -> playCount == ruleValue
            else -> false
        }
    }
    
    private fun matchesRatingRule(mediaItem: MediaItem, rule: SmartPlaylistRule): Boolean {
        val rating = mediaItem.mediaMetadata.extras?.getFloat("rating") ?: 0f
        val ruleValue = rule.value.toFloatOrNull() ?: 0f
        
        return when (rule.operator) {
            SmartPlaylistOperator.GREATER_THAN -> rating > ruleValue
            SmartPlaylistOperator.LESS_THAN -> rating < ruleValue
            SmartPlaylistOperator.EQUALS -> rating == ruleValue
            else -> false
        }
    }
    
    private fun matchesFileSizeRule(mediaItem: MediaItem, rule: SmartPlaylistRule): Boolean {
        val fileSize = mediaItem.mediaMetadata.extras?.getLong("file_size") ?: 0L
        val ruleValue = rule.value.toLongOrNull() ?: 0L
        
        return when (rule.operator) {
            SmartPlaylistOperator.GREATER_THAN -> fileSize > ruleValue
            SmartPlaylistOperator.LESS_THAN -> fileSize < ruleValue
            SmartPlaylistOperator.EQUALS -> fileSize == ruleValue
            else -> false
        }
    }
    
    private fun matchesResolutionRule(mediaItem: MediaItem, rule: SmartPlaylistRule): Boolean {
        val resolution = mediaItem.mediaMetadata.extras?.getString("resolution") ?: ""
        return when (rule.operator) {
            SmartPlaylistOperator.EQUALS -> resolution.equals(rule.value, ignoreCase = true)
            SmartPlaylistOperator.CONTAINS -> resolution.contains(rule.value, ignoreCase = true)
            SmartPlaylistOperator.NOT_EQUALS -> !resolution.equals(rule.value, ignoreCase = true)
            else -> false
        }
    }
    
    private fun applySortingAndLimits(
        mediaItems: List<MediaItem>,
        rules: List<SmartPlaylistRule>
    ): List<MediaItem> {
        val sortRule = rules.find { it.field == SmartPlaylistField.SORT_BY }
        val limitRule = rules.find { it.field == SmartPlaylistField.LIMIT }
        
        var sortedItems = when (sortRule?.value) {
            "title" -> mediaItems.sortedBy { it.mediaMetadata.title.toString() }
            "artist" -> mediaItems.sortedBy { it.mediaMetadata.artist.toString() }
            "duration" -> mediaItems.sortedBy { it.mediaMetadata.extras?.getLong("duration") ?: 0L }
            "date_added" -> mediaItems.sortedByDescending { it.mediaMetadata.extras?.getLong("date_added") ?: 0L }
            "random" -> mediaItems.shuffled()
            else -> mediaItems
        }
        
        val limit = limitRule?.value?.toIntOrNull()
        return if (limit != null && limit > 0) {
            sortedItems.take(limit)
        } else {
            sortedItems
        }
    }
    
    private fun getCurrentUserId(): String {
        // Get current user ID from authentication service
        return "current_user_id" // Placeholder
    }
}

// 2. AI Playlist Curator
@Singleton
class AIPlaylistCurator @Inject constructor(
    private val context: Context,
    private val userBehaviorAnalyzer: UserBehaviorAnalyzer,
    private val contentAnalysisService: ContentAnalysisService,
    private val musicClassificationModel: MusicClassificationModel
) {
    
    private var isInitialized = false
    
    suspend fun initialize(): Boolean = withContext(Dispatchers.IO) {
        try {
            musicClassificationModel.loadModel()
            isInitialized = true
            Log.i("AIPlaylistCurator", "AI Playlist Curator initialized")
            true
        } catch (e: Exception) {
            Log.e("AIPlaylistCurator", "Failed to initialize AI Playlist Curator", e)
            false
        }
    }
    
    suspend fun generatePlaylist(request: AIPlaylistRequest): AIGeneratedPlaylist {
        return withContext(Dispatchers.Default) {
            if (!isInitialized) {
                throw IllegalStateException("AI Playlist Curator not initialized")
            }
            
            val availableMedia = getAvailableMedia(request.sourceType)
            val userPreferences = userBehaviorAnalyzer.getUserPreferences()
            
            val selectedMedia = when (request.curationType) {
                AICurationType.MOOD_BASED -> curateMoodBasedPlaylist(
                    availableMedia, request.mood, request.targetDuration
                )
                AICurationType.ACTIVITY_BASED -> curateActivityBasedPlaylist(
                    availableMedia, request.activity, request.targetDuration
                )
                AICurationType.TIME_BASED -> curateTimeBasedPlaylist(
                    availableMedia, request.timeOfDay, request.targetDuration
                )
                AICurationType.SIMILARITY_BASED -> curateSimilarityBasedPlaylist(
                    availableMedia, request.seedMedia, request.targetDuration
                )
                AICurationType.DISCOVERY_BASED -> curateDiscoveryPlaylist(
                    availableMedia, userPreferences, request.targetDuration
                )
                AICurationType.MIXED_BASED -> curateMixedPlaylist(
                    availableMedia, userPreferences, request
                )
            }
            
            AIGeneratedPlaylist(
                id = UUID.randomUUID().toString(),
                name = generatePlaylistName(request),
                description = generatePlaylistDescription(request, selectedMedia),
                mediaItems = selectedMedia,
                curationType = request.curationType,
                confidence = calculatePlaylistConfidence(selectedMedia, request),
                generatedAt = System.currentTimeMillis(),
                metadata = generatePlaylistMetadata(selectedMedia)
            )
        }
    }
    
    suspend fun generatePlaylistRecommendations(
        userId: String,
        maxRecommendations: Int
    ): List<PlaylistRecommendation> {
        return withContext(Dispatchers.Default) {
            val userPreferences = userBehaviorAnalyzer.getUserPreferences()
            val recommendations = mutableListOf<PlaylistRecommendation>()
            
            // Mood-based recommendations
            val currentMood = detectCurrentMood(userPreferences)
            recommendations.add(
                PlaylistRecommendation(
                    type = AICurationType.MOOD_BASED,
                    title = "Perfect for your ${currentMood.displayName} mood",
                    description = "AI-selected videos to match your current mood",
                    confidence = 0.85f,
                    parameters = mapOf("mood" to currentMood.name)
                )
            )
            
            // Time-based recommendations
            val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            val timeOfDay = getTimeOfDayFromHour(currentHour)
            recommendations.add(
                PlaylistRecommendation(
                    type = AICurationType.TIME_BASED,
                    title = "${timeOfDay.displayName} Favorites",
                    description = "Great videos for this time of day",
                    confidence = 0.75f,
                    parameters = mapOf("timeOfDay" to timeOfDay.name)
                )
            )
            
            // Discovery recommendations
            recommendations.add(
                PlaylistRecommendation(
                    type = AICurationType.DISCOVERY_BASED,
                    title = "Discover Something New",
                    description = "Videos you might love but haven't seen yet",
                    confidence = 0.70f,
                    parameters = emptyMap()
                )
            )
            
            // Activity-based recommendations
            val suggestedActivity = suggestActivityBasedOnContext()
            if (suggestedActivity != null) {
                recommendations.add(
                    PlaylistRecommendation(
                        type = AICurationType.ACTIVITY_BASED,
                        title = "Perfect for ${suggestedActivity.displayName}",
                        description = "Videos selected for your current activity",
                        confidence = 0.80f,
                        parameters = mapOf("activity" to suggestedActivity.name)
                    )
                )
            }
            
            recommendations.take(maxRecommendations)
        }
    }
    
    private suspend fun curateMoodBasedPlaylist(
        availableMedia: List<MediaItem>,
        mood: PlaylistMood,
        targetDuration: Long
    ): List<MediaItem> {
        val moodCompatibleMedia = availableMedia.filter { mediaItem ->
            val mediaAnalysis = contentAnalysisService.getAnalysis(mediaItem.mediaId)
            mediaAnalysis?.let { analysis ->
                isMoodCompatible(analysis.emotionalAnalysis, mood)
            } ?: false
        }
        
        return selectMediaForDuration(moodCompatibleMedia, targetDuration)
    }
    
    private suspend fun curateActivityBasedPlaylist(
        availableMedia: List<MediaItem>,
        activity: PlaylistActivity,
        targetDuration: Long
    ): List<MediaItem> {
        val activityCompatibleMedia = availableMedia.filter { mediaItem ->
            val mediaAnalysis = contentAnalysisService.getAnalysis(mediaItem.mediaId)
            mediaAnalysis?.let { analysis ->
                isActivityCompatible(analysis, activity)
            } ?: false
        }
        
        return selectMediaForDuration(activityCompatibleMedia, targetDuration)
    }
    
    private suspend fun curateTimeBasedPlaylist(
        availableMedia: List<MediaItem>,
        timeOfDay: TimeOfDay,
        targetDuration: Long
    ): List<MediaItem> {
        val userPreferences = userBehaviorAnalyzer.getUserPreferences()
        val timePreferences = userPreferences.timeBasedPreferences[timeOfDay] ?: emptyList()
        
        val timeCompatibleMedia = availableMedia.filter { mediaItem ->
            val genre = mediaItem.mediaMetadata.extras?.getString("genre")
            genre in timePreferences
        }
        
        return selectMediaForDuration(timeCompatibleMedia, targetDuration)
    }
    
    private suspend fun curateSimilarityBasedPlaylist(
        availableMedia: List<MediaItem>,
        seedMedia: List<MediaItem>,
        targetDuration: Long
    ): List<MediaItem> {
        val similarMedia = mutableListOf<Pair<MediaItem, Float>>()
        
        availableMedia.forEach { mediaItem ->
            if (mediaItem !in seedMedia) {
                val similarity = calculateMediaSimilarity(mediaItem, seedMedia)
                if (similarity > 0.6f) {
                    similarMedia.add(Pair(mediaItem, similarity))
                }
            }
        }
        
        val sortedSimilarMedia = similarMedia.sortedByDescending { it.second }.map { it.first }
        return selectMediaForDuration(sortedSimilarMedia, targetDuration)
    }
    
    private suspend fun curateDiscoveryPlaylist(
        availableMedia: List<MediaItem>,
        userPreferences: UserPreferences,
        targetDuration: Long
    ): List<MediaItem> {
        val watchedMedia = userPreferences.watchedMediaIds
        val preferredGenres = userPreferences.preferredGenres
        
        val discoveryMedia = availableMedia.filter { mediaItem ->
            // Not watched and in preferred genres or adjacent genres
            mediaItem.mediaId !in watchedMedia && 
            (isInPreferredGenres(mediaItem, preferredGenres) || 
             isInAdjacentGenres(mediaItem, preferredGenres))
        }
        
        return selectMediaForDuration(discoveryMedia.shuffled(), targetDuration)
    }
    
    private suspend fun curateMixedPlaylist(
        availableMedia: List<MediaItem>,
        userPreferences: UserPreferences,
        request: AIPlaylistRequest
    ): List<MediaItem> {
        val selectedMedia = mutableListOf<MediaItem>()
        val targetDuration = request.targetDuration
        var currentDuration = 0L
        
        // 40% based on user preferences
        val preferredMedia = getPreferredMedia(availableMedia, userPreferences)
        selectedMedia.addAll(
            selectMediaForDuration(preferredMedia, (targetDuration * 0.4f).toLong())
        )
        currentDuration += selectedMedia.sumOf { getMediaDuration(it) }
        
        // 30% discovery content
        val discoveryMedia = curateDiscoveryPlaylist(
            availableMedia.filter { it !in selectedMedia },
            userPreferences,
            (targetDuration * 0.3f).toLong()
        )
        selectedMedia.addAll(discoveryMedia)
        currentDuration += discoveryMedia.sumOf { getMediaDuration(it) }
        
        // 30% trending/popular content
        val trendingMedia = getTrendingMedia(availableMedia.filter { it !in selectedMedia })
        val remainingDuration = targetDuration - currentDuration
        selectedMedia.addAll(selectMediaForDuration(trendingMedia, remainingDuration))
        
        return selectedMedia.shuffled()
    }
    
    private fun calculateMediaSimilarity(mediaItem: MediaItem, seedMedia: List<MediaItem>): Float {
        // Calculate similarity based on multiple factors
        var similarity = 0f
        var factors = 0
        
        seedMedia.forEach { seed ->
            // Genre similarity
            val mediaGenre = mediaItem.mediaMetadata.extras?.getString("genre")
            val seedGenre = seed.mediaMetadata.extras?.getString("genre")
            if (mediaGenre == seedGenre) {
                similarity += 0.3f
                factors++
            }
            
            // Artist similarity
            val mediaArtist = mediaItem.mediaMetadata.artist?.toString()
            val seedArtist = seed.mediaMetadata.artist?.toString()
            if (mediaArtist == seedArtist) {
                similarity += 0.4f
                factors++
            }
            
            // Duration similarity
            val mediaDuration = getMediaDuration(mediaItem)
            val seedDuration = getMediaDuration(seed)
            val durationSimilarity = 1f - (abs(mediaDuration - seedDuration).toFloat() / maxOf(mediaDuration, seedDuration))
            similarity += durationSimilarity * 0.3f
            factors++
        }
        
        return if (factors > 0) similarity / factors else 0f
    }
    
    private fun isMoodCompatible(emotionalAnalysis: EmotionalAnalysis?, mood: PlaylistMood): Boolean {
        return emotionalAnalysis?.let { analysis ->
            when (mood) {
                PlaylistMood.HAPPY -> analysis.dominantEmotion == EmotionType.HAPPY || 
                                    analysis.overallTone == EmotionalTone.POSITIVE
                PlaylistMood.RELAXED -> analysis.overallTone == EmotionalTone.CALM
                PlaylistMood.ENERGETIC -> analysis.dominantEmotion == EmotionType.EXCITED ||
                                        analysis.overallTone == EmotionalTone.ENERGETIC
                PlaylistMood.FOCUSED -> analysis.overallTone == EmotionalTone.NEUTRAL ||
                                      analysis.overallTone == EmotionalTone.CALM
                PlaylistMood.NOSTALGIC -> analysis.dominantEmotion == EmotionType.NEUTRAL
                PlaylistMood.ADVENTUROUS -> analysis.overallTone == EmotionalTone.ENERGETIC
            }
        } ?: false
    }
    
    private fun isActivityCompatible(analysis: ContentAnalysisResults, activity: PlaylistActivity): Boolean {
        return when (activity) {
            PlaylistActivity.WORKOUT -> analysis.contentCategory == ContentCategory.SPORTS ||
                                      analysis.contentCategory == ContentCategory.MUSIC
            PlaylistActivity.STUDY -> analysis.contentCategory == ContentCategory.EDUCATIONAL ||
                                    analysis.contentCategory == ContentCategory.DOCUMENTARY
            PlaylistActivity.RELAXATION -> analysis.contentCategory == ContentCategory.DOCUMENTARY ||
                                         analysis.contentCategory == ContentCategory.TRAVEL
            PlaylistActivity.ENTERTAINMENT -> analysis.contentCategory == ContentCategory.ENTERTAINMENT ||
                                            analysis.contentCategory == ContentCategory.COMEDY
            PlaylistActivity.TRAVEL -> analysis.contentCategory == ContentCategory.TRAVEL ||
                                     analysis.contentCategory == ContentCategory.DOCUMENTARY
            PlaylistActivity.COOKING -> analysis.contentCategory == ContentCategory.EDUCATIONAL
        }
    }
    
    private fun selectMediaForDuration(mediaItems: List<MediaItem>, targetDuration: Long): List<MediaItem> {
        val selected = mutableListOf<MediaItem>()
        var currentDuration = 0L
        
        for (mediaItem in mediaItems) {
            val mediaDuration = getMediaDuration(mediaItem)
            if (currentDuration + mediaDuration <= targetDuration) {
                selected.add(mediaItem)
                currentDuration += mediaDuration
            }
            
            if (currentDuration >= targetDuration * 0.9f) break // 90% of target is good enough
        }
        
        return selected
    }
    
    private fun getMediaDuration(mediaItem: MediaItem): Long {
        return mediaItem.mediaMetadata.extras?.getLong("duration") ?: 300000L // Default 5 minutes
    }
    
    private fun generatePlaylistName(request: AIPlaylistRequest): String {
        return when (request.curationType) {
            AICurationType.MOOD_BASED -> "${request.mood?.displayName ?: "