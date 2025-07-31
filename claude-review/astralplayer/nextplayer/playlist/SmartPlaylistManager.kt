package com.astralplayer.nextplayer.playlist

import android.content.Context
import android.net.Uri
import com.astralplayer.nextplayer.data.PlaylistVideo
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.*
import kotlin.math.*

/**
 * Smart playlist management with dynamic rules and auto-updating content
 */
class SmartPlaylistManager(
    private val context: Context
) {
    
    private val _smartPlaylists = MutableStateFlow<List<SmartPlaylist>>(emptyList())
    val smartPlaylists: StateFlow<List<SmartPlaylist>> = _smartPlaylists.asStateFlow()
    
    private val _updateEvents = MutableSharedFlow<SmartPlaylistUpdateEvent>()
    val updateEvents: SharedFlow<SmartPlaylistUpdateEvent> = _updateEvents.asSharedFlow()
    
    private val playlistRules = mutableMapOf<String, List<PlaylistRule>>()
    private val contentDatabase = ContentDatabase()
    private val ruleEngine = RuleEngine()
    
    private val smartPlaylistScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var isInitialized = false
    
    /**
     * Initialize smart playlist manager
     */
    suspend fun initialize() {
        isInitialized = true
        
        // Load existing smart playlists
        loadSmartPlaylists()
        
        // Initialize content database
        contentDatabase.initialize()
        
        // Start automatic updates
        startAutoUpdates()
        
        // Register content change listeners
        registerContentChangeListeners()
    }
    
    /**
     * Create a new smart playlist with rules
     */
    suspend fun createSmartPlaylist(
        name: String,
        description: String = "",
        rules: List<PlaylistRule>,
        settings: SmartPlaylistSettings = SmartPlaylistSettings()
    ): SmartPlaylist {
        val smartPlaylist = SmartPlaylist(
            id = UUID.randomUUID().toString(),
            name = name,
            description = description,
            rules = rules,
            settings = settings,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            lastEvaluated = 0L,
            videoCount = 0,
            totalDuration = 0L,
            isActive = true
        )
        
        playlistRules[smartPlaylist.id] = rules
        
        // Initial population
        val videos = evaluateRules(rules, settings)
        smartPlaylist.videoCount = videos.size
        smartPlaylist.totalDuration = videos.sumOf { it.duration }
        smartPlaylist.lastEvaluated = System.currentTimeMillis()
        
        updateSmartPlaylistList(smartPlaylist)
        _updateEvents.emit(SmartPlaylistUpdateEvent.Created(smartPlaylist, videos.size))
        
        return smartPlaylist
    }
    
    /**
     * Create pre-defined smart playlists
     */
    suspend fun createPreDefinedSmartPlaylists(): List<SmartPlaylist> {
        val preDefinedPlaylists = mutableListOf<SmartPlaylist>()
        
        // Recently Added
        preDefinedPlaylists.add(createSmartPlaylist(
            name = "Recently Added",
            description = "Videos added in the last 7 days",
            rules = listOf(
                PlaylistRule.DateAdded(
                    operator = ComparisonOperator.GREATER_THAN,
                    value = System.currentTimeMillis() - 604800000L // 7 days
                )
            ),
            settings = SmartPlaylistSettings(maxVideos = 50, sortBy = SortCriteria.DATE_ADDED_DESC)
        ))
        
        // Frequently Watched
        preDefinedPlaylists.add(createSmartPlaylist(
            name = "Frequently Watched",
            description = "Your most watched videos",
            rules = listOf(
                PlaylistRule.PlayCount(
                    operator = ComparisonOperator.GREATER_THAN,
                    value = 3f
                )
            ),
            settings = SmartPlaylistSettings(maxVideos = 30, sortBy = SortCriteria.PLAY_COUNT_DESC)
        ))
        
        // Long Videos
        preDefinedPlaylists.add(createSmartPlaylist(
            name = "Long Form Content",
            description = "Videos longer than 20 minutes",
            rules = listOf(
                PlaylistRule.Duration(
                    operator = ComparisonOperator.GREATER_THAN,
                    value = 1200000f // 20 minutes
                )
            ),
            settings = SmartPlaylistSettings(maxVideos = 25, sortBy = SortCriteria.DURATION_DESC)
        ))
        
        // Quick Watches
        preDefinedPlaylists.add(createSmartPlaylist(
            name = "Quick Watches",
            description = "Short videos under 5 minutes",
            rules = listOf(
                PlaylistRule.Duration(
                    operator = ComparisonOperator.LESS_THAN,
                    value = 300000f // 5 minutes
                )
            ),
            settings = SmartPlaylistSettings(maxVideos = 40, sortBy = SortCriteria.RANDOM)
        ))
        
        // Unwatched
        preDefinedPlaylists.add(createSmartPlaylist(
            name = "Unwatched",
            description = "Videos you haven't watched yet",
            rules = listOf(
                PlaylistRule.PlayCount(
                    operator = ComparisonOperator.EQUALS,
                    value = 0f
                )
            ),
            settings = SmartPlaylistSettings(maxVideos = 100, sortBy = SortCriteria.DATE_ADDED_DESC)
        ))
        
        return preDefinedPlaylists
    }
    
    /**
     * Update smart playlist rules
     */
    suspend fun updateSmartPlaylistRules(
        playlistId: String,
        newRules: List<PlaylistRule>
    ): Boolean {
        val currentPlaylists = _smartPlaylists.value.toMutableList()
        val playlistIndex = currentPlaylists.indexOfFirst { it.id == playlistId }
        
        if (playlistIndex == -1) return false
        
        val updatedPlaylist = currentPlaylists[playlistIndex].copy(
            rules = newRules,
            updatedAt = System.currentTimeMillis(),
            lastEvaluated = 0L // Force re-evaluation
        )
        
        currentPlaylists[playlistIndex] = updatedPlaylist
        playlistRules[playlistId] = newRules
        
        // Re-evaluate with new rules
        val videos = evaluateRules(newRules, updatedPlaylist.settings)
        val finalPlaylist = updatedPlaylist.copy(
            videoCount = videos.size,
            totalDuration = videos.sumOf { it.duration },
            lastEvaluated = System.currentTimeMillis()
        )
        
        currentPlaylists[playlistIndex] = finalPlaylist
        _smartPlaylists.value = currentPlaylists
        
        _updateEvents.emit(SmartPlaylistUpdateEvent.RulesUpdated(finalPlaylist, videos.size))
        
        return true
    }
    
    /**
     * Manually refresh a smart playlist
     */
    suspend fun refreshSmartPlaylist(playlistId: String): RefreshResult {
        val playlist = _smartPlaylists.value.find { it.id == playlistId }
            ?: return RefreshResult.PlaylistNotFound
        
        val rules = playlistRules[playlistId] ?: return RefreshResult.RulesNotFound
        
        return try {
            val oldVideos = getPlaylistVideos(playlistId)
            val newVideos = evaluateRules(rules, playlist.settings)
            
            val updatedPlaylist = playlist.copy(
                videoCount = newVideos.size,
                totalDuration = newVideos.sumOf { it.duration },
                lastEvaluated = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            
            updateSmartPlaylistInList(updatedPlaylist)
            
            val changes = calculatePlaylistChanges(oldVideos, newVideos)
            _updateEvents.emit(SmartPlaylistUpdateEvent.Refreshed(updatedPlaylist, changes))
            
            RefreshResult.Success(updatedPlaylist, changes)
            
        } catch (e: Exception) {
            RefreshResult.Error(e)
        }
    }
    
    /**
     * Get videos for a smart playlist
     */
    suspend fun getSmartPlaylistVideos(playlistId: String): List<PlaylistVideo> {
        val playlist = _smartPlaylists.value.find { it.id == playlistId } ?: return emptyList()
        val rules = playlistRules[playlistId] ?: return emptyList()
        
        return evaluateRules(rules, playlist.settings)
    }
    
    /**
     * Create advanced rules with complex logic
     */
    fun createAdvancedRules(): List<PlaylistRule> {
        return listOf(
            // Composite rule: High-rated action videos longer than 10 minutes
            PlaylistRule.Composite(
                operator = LogicalOperator.AND,
                rules = listOf(
                    PlaylistRule.Genre(
                        operator = ComparisonOperator.EQUALS,
                        value = "Action"
                    ),
                    PlaylistRule.Rating(
                        operator = ComparisonOperator.GREATER_THAN,
                        value = 4.0f
                    ),
                    PlaylistRule.Duration(
                        operator = ComparisonOperator.GREATER_THAN,
                        value = 600000f // 10 minutes
                    )
                )
            ),
            
            // Or rule: Either comedy or recently added
            PlaylistRule.Composite(
                operator = LogicalOperator.OR,
                rules = listOf(
                    PlaylistRule.Genre(
                        operator = ComparisonOperator.EQUALS,
                        value = "Comedy"
                    ),
                    PlaylistRule.DateAdded(
                        operator = ComparisonOperator.GREATER_THAN,
                        value = System.currentTimeMillis() - 86400000L // 1 day
                    )
                )
            )
        )
    }
    
    /**
     * Auto-generate rules based on user behavior
     */
    suspend fun generateBehaviorBasedRules(userId: String): List<PlaylistRule> {
        val userStats = getUserStats(userId)
        val rules = mutableListOf<PlaylistRule>()
        
        // Favorite genres rule
        val favoriteGenres = userStats.topGenres.take(3)
        if (favoriteGenres.isNotEmpty()) {
            rules.add(PlaylistRule.Composite(
                operator = LogicalOperator.OR,
                rules = favoriteGenres.map { genre ->
                    PlaylistRule.Genre(
                        operator = ComparisonOperator.EQUALS,
                        value = genre
                    )
                }
            ))
        }
        
        // Preferred duration range
        val avgDuration = userStats.averageWatchDuration
        val durationTolerance = avgDuration * 0.3f
        rules.add(PlaylistRule.Composite(
            operator = LogicalOperator.AND,
            rules = listOf(
                PlaylistRule.Duration(
                    operator = ComparisonOperator.GREATER_THAN,
                    value = avgDuration - durationTolerance
                ),
                PlaylistRule.Duration(
                    operator = ComparisonOperator.LESS_THAN,
                    value = avgDuration + durationTolerance
                )
            )
        ))
        
        // Exclude frequently skipped content
        if (userStats.frequentlySkippedGenres.isNotEmpty()) {
            rules.add(PlaylistRule.Composite(
                operator = LogicalOperator.NOT,
                rules = userStats.frequentlySkippedGenres.map { genre ->
                    PlaylistRule.Genre(
                        operator = ComparisonOperator.EQUALS,
                        value = genre
                    )
                }
            ))
        }
        
        return rules
    }
    
    /**
     * Create seasonal or time-based rules
     */
    fun createTemporalRules(): List<PlaylistRule> {
        val currentTime = Calendar.getInstance()
        val rules = mutableListOf<PlaylistRule>()
        
        // Holiday-themed content
        when (currentTime.get(Calendar.MONTH)) {
            Calendar.DECEMBER -> {
                rules.add(PlaylistRule.Keywords(
                    operator = ComparisonOperator.CONTAINS,
                    value = listOf("christmas", "holiday", "winter")
                ))
            }
            Calendar.OCTOBER -> {
                rules.add(PlaylistRule.Keywords(
                    operator = ComparisonOperator.CONTAINS,
                    value = listOf("halloween", "horror", "scary")
                ))
            }
        }
        
        // Time of day rules
        val hourOfDay = currentTime.get(Calendar.HOUR_OF_DAY)
        when (hourOfDay) {
            in 6..11 -> { // Morning
                rules.add(PlaylistRule.Genre(
                    operator = ComparisonOperator.EQUALS,
                    value = "Educational"
                ))
            }
            in 18..22 -> { // Evening
                rules.add(PlaylistRule.Genre(
                    operator = ComparisonOperator.EQUALS,
                    value = "Entertainment"
                ))
            }
        }
        
        return rules
    }
    
    /**
     * Get smart playlist statistics
     */
    fun getSmartPlaylistStats(playlistId: String): SmartPlaylistStats? {
        val playlist = _smartPlaylists.value.find { it.id == playlistId } ?: return null
        
        return SmartPlaylistStats(
            totalVideos = playlist.videoCount,
            totalDuration = playlist.totalDuration,
            averageVideoDuration = if (playlist.videoCount > 0) {
                playlist.totalDuration / playlist.videoCount
            } else 0L,
            lastUpdated = playlist.lastEvaluated,
            updateFrequency = calculateUpdateFrequency(playlistId),
            ruleComplexity = calculateRuleComplexity(playlistId),
            contentDiversity = calculateContentDiversity(playlistId)
        )
    }
    
    /**
     * Validate playlist rules
     */
    fun validateRules(rules: List<PlaylistRule>): RuleValidationResult {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()
        
        rules.forEach { rule ->
            when (rule) {
                is PlaylistRule.Composite -> {
                    if (rule.rules.isEmpty()) {
                        errors.add("Composite rule cannot be empty")
                    }
                    if (rule.rules.size == 1 && rule.operator != LogicalOperator.NOT) {
                        warnings.add("Single rule in composite might be unnecessary")
                    }
                }
                is PlaylistRule.Duration -> {
                    if (rule.value < 0) {
                        errors.add("Duration cannot be negative")
                    }
                    if (rule.value > 86400000) { // 24 hours
                        warnings.add("Very long duration filter might exclude most content")
                    }
                }
            }
        }
        
        return RuleValidationResult(
            isValid = errors.isEmpty(),
            errors = errors,
            warnings = warnings
        )
    }
    
    // Private implementation methods
    private suspend fun loadSmartPlaylists() {
        // Load from storage
        delay(100) // Simulate loading
    }
    
    private fun startAutoUpdates() {
        smartPlaylistScope.launch {
            while (isInitialized && currentCoroutineContext().isActive) {
                try {
                    updateAllSmartPlaylists()
                    delay(3600000) // Update every hour
                } catch (e: Exception) {
                    // Handle error but continue
                }
            }
        }
    }
    
    private fun registerContentChangeListeners() {
        // Register for content database changes
        contentDatabase.onContentChanged = { contentType ->
            smartPlaylistScope.launch {
                updateAffectedPlaylists(contentType)
            }
        }
    }
    
    private suspend fun evaluateRules(
        rules: List<PlaylistRule>,
        settings: SmartPlaylistSettings
    ): List<PlaylistVideo> {
        val allVideos = contentDatabase.getAllVideos()
        val matchingVideos = ruleEngine.evaluateRules(rules, allVideos)
        
        // Apply sorting
        val sortedVideos = when (settings.sortBy) {
            SortCriteria.DATE_ADDED_DESC -> matchingVideos.sortedByDescending { it.dateAdded }
            SortCriteria.DATE_ADDED_ASC -> matchingVideos.sortedBy { it.dateAdded }
            SortCriteria.DURATION_DESC -> matchingVideos.sortedByDescending { it.duration }
            SortCriteria.DURATION_ASC -> matchingVideos.sortedBy { it.duration }
            SortCriteria.PLAY_COUNT_DESC -> matchingVideos.sortedByDescending { it.playCount }
            SortCriteria.RANDOM -> matchingVideos.shuffled()
            SortCriteria.ALPHABETICAL -> matchingVideos.sortedBy { it.title }
        }
        
        // Apply limits
        return if (settings.maxVideos != null) {
            sortedVideos.take(settings.maxVideos)
        } else {
            sortedVideos
        }
    }
    
    private suspend fun updateAllSmartPlaylists() {
        val playlists = _smartPlaylists.value
        val updateInterval = 1800000L // 30 minutes
        
        playlists.filter { playlist ->
            playlist.isActive && 
            (System.currentTimeMillis() - playlist.lastEvaluated) > updateInterval
        }.forEach { playlist ->
            refreshSmartPlaylist(playlist.id)
        }
    }
    
    private suspend fun updateAffectedPlaylists(contentType: ContentType) {
        // Update playlists that might be affected by content changes
        val affectedPlaylists = _smartPlaylists.value.filter { playlist ->
            playlistRules[playlist.id]?.any { rule ->
                ruleAffectedByContentType(rule, contentType)
            } ?: false
        }
        
        affectedPlaylists.forEach { playlist ->
            refreshSmartPlaylist(playlist.id)
        }
    }
    
    private fun ruleAffectedByContentType(rule: PlaylistRule, contentType: ContentType): Boolean {
        return when (rule) {
            is PlaylistRule.DateAdded -> contentType == ContentType.NEW_VIDEO
            is PlaylistRule.PlayCount -> contentType == ContentType.PLAYBACK_STATS
            is PlaylistRule.Genre -> contentType == ContentType.METADATA
            is PlaylistRule.Composite -> rule.rules.any { ruleAffectedByContentType(it, contentType) }
            else -> true
        }
    }
    
    private fun updateSmartPlaylistList(playlist: SmartPlaylist) {
        val currentPlaylists = _smartPlaylists.value.toMutableList()
        currentPlaylists.add(playlist)
        _smartPlaylists.value = currentPlaylists
    }
    
    private fun updateSmartPlaylistInList(playlist: SmartPlaylist) {
        val currentPlaylists = _smartPlaylists.value.toMutableList()
        val index = currentPlaylists.indexOfFirst { it.id == playlist.id }
        if (index != -1) {
            currentPlaylists[index] = playlist
            _smartPlaylists.value = currentPlaylists
        }
    }
    
    private suspend fun getPlaylistVideos(playlistId: String): List<PlaylistVideo> {
        // Get current videos for the playlist
        return getSmartPlaylistVideos(playlistId)
    }
    
    private fun calculatePlaylistChanges(
        oldVideos: List<PlaylistVideo>,
        newVideos: List<PlaylistVideo>
    ): PlaylistChanges {
        val oldUris = oldVideos.map { it.uri }.toSet()
        val newUris = newVideos.map { it.uri }.toSet()
        
        val added = newVideos.filter { it.uri !in oldUris }
        val removed = oldVideos.filter { it.uri !in newUris }
        val unchanged = newVideos.filter { it.uri in oldUris }
        
        return PlaylistChanges(
            added = added,
            removed = removed,
            unchanged = unchanged.size
        )
    }
    
    private fun getUserStats(userId: String): UserStats {
        // Get user statistics (simplified)
        return UserStats(
            topGenres = listOf("Action", "Comedy", "Drama"),
            averageWatchDuration = 1800000L, // 30 minutes
            frequentlySkippedGenres = listOf("Horror", "Documentary")
        )
    }
    
    private fun calculateUpdateFrequency(playlistId: String): Float {
        // Calculate how often playlist updates (simplified)
        return 2.5f // updates per day
    }
    
    private fun calculateRuleComplexity(playlistId: String): Int {
        val rules = playlistRules[playlistId] ?: return 0
        return rules.sumOf { rule ->
            when (rule) {
                is PlaylistRule.Composite -> rule.rules.size + 1
                else -> 1
            }
        }
    }
    
    private fun calculateContentDiversity(playlistId: String): Float {
        // Calculate content diversity score (simplified)
        return 0.75f
    }
    
    fun cleanup() {
        isInitialized = false
        smartPlaylistScope.cancel()
        playlistRules.clear()
        contentDatabase.cleanup()
    }
}

// Data classes and enums for smart playlist management  
enum class ComparisonOperator { EQUALS, NOT_EQUALS, GREATER_THAN, LESS_THAN, CONTAINS, NOT_CONTAINS }
enum class LogicalOperator { AND, OR, NOT }
enum class SortCriteria { DATE_ADDED_DESC, DATE_ADDED_ASC, DURATION_DESC, DURATION_ASC, PLAY_COUNT_DESC, RANDOM, ALPHABETICAL }
enum class ContentType { NEW_VIDEO, METADATA, PLAYBACK_STATS, USER_RATING }

data class SmartPlaylist(
    val id: String,
    val name: String,
    val description: String,
    val rules: List<PlaylistRule>,
    val settings: SmartPlaylistSettings,
    val createdAt: Long,
    val updatedAt: Long,
    val lastEvaluated: Long,
    val videoCount: Int,
    val totalDuration: Long,
    val isActive: Boolean
)

data class SmartPlaylistSettings(
    val autoUpdate: Boolean = true,
    val updateInterval: Long = 3600000L, // 1 hour
    val maxVideos: Int? = null,
    val sortBy: SortCriteria = SortCriteria.DATE_ADDED_DESC,
    val includeWatched: Boolean = true,
    val dynamicRules: Boolean = false
)

sealed class PlaylistRule {
    data class Genre(
        val operator: ComparisonOperator,
        val value: String
    ) : PlaylistRule()
    
    data class Duration(
        val operator: ComparisonOperator,
        val value: Float
    ) : PlaylistRule()
    
    data class DateAdded(
        val operator: ComparisonOperator,
        val value: Long
    ) : PlaylistRule()
    
    data class PlayCount(
        val operator: ComparisonOperator,
        val value: Float
    ) : PlaylistRule()
    
    data class Rating(
        val operator: ComparisonOperator,
        val value: Float
    ) : PlaylistRule()
    
    data class Keywords(
        val operator: ComparisonOperator,
        val value: List<String>
    ) : PlaylistRule()
    
    data class FileSize(
        val operator: ComparisonOperator,
        val value: Long
    ) : PlaylistRule()
    
    data class Resolution(
        val operator: ComparisonOperator,
        val value: String
    ) : PlaylistRule()
    
    data class Composite(
        val operator: LogicalOperator,
        val rules: List<PlaylistRule>
    ) : PlaylistRule()
}

data class PlaylistChanges(
    val added: List<PlaylistVideo>,
    val removed: List<PlaylistVideo>,
    val unchanged: Int
)

data class SmartPlaylistStats(
    val totalVideos: Int,
    val totalDuration: Long,
    val averageVideoDuration: Long,
    val lastUpdated: Long,
    val updateFrequency: Float,
    val ruleComplexity: Int,
    val contentDiversity: Float
)

data class UserStats(
    val topGenres: List<String>,
    val averageWatchDuration: Long,
    val frequentlySkippedGenres: List<String>
)

data class RuleValidationResult(
    val isValid: Boolean,
    val errors: List<String>,
    val warnings: List<String>
)

sealed class RefreshResult {
    data class Success(val playlist: SmartPlaylist, val changes: PlaylistChanges) : RefreshResult()
    data class Error(val exception: Exception) : RefreshResult()
    object PlaylistNotFound : RefreshResult()
    object RulesNotFound : RefreshResult()
}

sealed class SmartPlaylistUpdateEvent {
    data class Created(val playlist: SmartPlaylist, val videoCount: Int) : SmartPlaylistUpdateEvent()
    data class Refreshed(val playlist: SmartPlaylist, val changes: PlaylistChanges) : SmartPlaylistUpdateEvent()
    data class RulesUpdated(val playlist: SmartPlaylist, val videoCount: Int) : SmartPlaylistUpdateEvent()
    data class AutoUpdated(val playlistId: String, val changes: PlaylistChanges) : SmartPlaylistUpdateEvent()
}

/**
 * Content database for smart playlist evaluation
 */
class ContentDatabase {
    var onContentChanged: ((ContentType) -> Unit)? = null
    
    suspend fun initialize() {
        // Initialize content database
    }
    
    fun getAllVideos(): List<EnrichedPlaylistVideo> {
        // Return all available videos with metadata
        return listOf(
            EnrichedPlaylistVideo(
                uri = Uri.parse("content://video1"),
                title = "Action Movie",
                duration = 7200000L,
                genre = "Action",
                dateAdded = System.currentTimeMillis() - 86400000L,
                playCount = 5,
                rating = 4.5f,
                fileSize = 1024 * 1024 * 1024L,
                resolution = "1080p"
            )
        )
    }
    
    fun cleanup() {
        // Cleanup resources
    }
}

/**
 * Rule evaluation engine
 */
class RuleEngine {
    fun evaluateRules(rules: List<PlaylistRule>, videos: List<EnrichedPlaylistVideo>): List<PlaylistVideo> {
        return videos.filter { video ->
            evaluateRulesForVideo(rules, video)
        }.map { it.toPlaylistVideo() }
    }
    
    private fun evaluateRulesForVideo(rules: List<PlaylistRule>, video: EnrichedPlaylistVideo): Boolean {
        return rules.all { rule ->
            evaluateRule(rule, video)
        }
    }
    
    private fun evaluateRule(rule: PlaylistRule, video: EnrichedPlaylistVideo): Boolean {
        return when (rule) {
            is PlaylistRule.Genre -> evaluateComparison(video.genre, rule.operator, rule.value)
            is PlaylistRule.Duration -> evaluateComparison(video.duration.toFloat(), rule.operator, rule.value)
            is PlaylistRule.DateAdded -> evaluateComparison(video.dateAdded, rule.operator, rule.value)
            is PlaylistRule.PlayCount -> evaluateComparison(video.playCount.toFloat(), rule.operator, rule.value)
            is PlaylistRule.Rating -> evaluateComparison(video.rating, rule.operator, rule.value)
            is PlaylistRule.Keywords -> evaluateKeywords(video, rule.operator, rule.value)
            is PlaylistRule.FileSize -> evaluateComparison(video.fileSize, rule.operator, rule.value)
            is PlaylistRule.Resolution -> evaluateComparison(video.resolution, rule.operator, rule.value)
            is PlaylistRule.Composite -> evaluateCompositeRule(rule, video)
        }
    }
    
    private fun evaluateComparison(value: Comparable<*>, operator: ComparisonOperator, target: Any): Boolean {
        return when (operator) {
            ComparisonOperator.EQUALS -> value == target
            ComparisonOperator.NOT_EQUALS -> value != target
            ComparisonOperator.GREATER_THAN -> (value as Comparable<Any>) > target
            ComparisonOperator.LESS_THAN -> (value as Comparable<Any>) < target
            ComparisonOperator.CONTAINS -> value.toString().contains(target.toString(), ignoreCase = true)
            ComparisonOperator.NOT_CONTAINS -> !value.toString().contains(target.toString(), ignoreCase = true)
        }
    }
    
    private fun evaluateKeywords(video: EnrichedPlaylistVideo, operator: ComparisonOperator, keywords: List<String>): Boolean {
        val text = "${video.title} ${video.genre}".lowercase()
        return when (operator) {
            ComparisonOperator.CONTAINS -> keywords.any { keyword -> text.contains(keyword.lowercase()) }
            ComparisonOperator.NOT_CONTAINS -> keywords.none { keyword -> text.contains(keyword.lowercase()) }
            else -> false
        }
    }
    
    private fun evaluateCompositeRule(rule: PlaylistRule.Composite, video: EnrichedPlaylistVideo): Boolean {
        return when (rule.operator) {
            LogicalOperator.AND -> rule.rules.all { evaluateRule(it, video) }
            LogicalOperator.OR -> rule.rules.any { evaluateRule(it, video) }
            LogicalOperator.NOT -> rule.rules.none { evaluateRule(it, video) }
        }
    }
}

/**
 * Enriched playlist video with additional metadata for smart playlists
 */
data class EnrichedPlaylistVideo(
    val uri: Uri,
    val title: String,
    val duration: Long,
    val genre: String,
    val dateAdded: Long = System.currentTimeMillis(),
    val playCount: Int = 0,
    val rating: Float = 0f,
    val fileSize: Long = 0L,
    val resolution: String = "Unknown"
) {
    fun toPlaylistVideo(): PlaylistVideo {
        return PlaylistVideo(uri, title, duration, genre)
    }
}