package com.astralplayer.nextplayer.feature.search

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * Data class representing a search result
 */
data class SearchResult(
    val id: String,
    val title: String,
    val path: String,
    val duration: Long,
    val size: Long,
    val format: String,
    val resolution: String,
    val lastModified: Long,
    val thumbnailPath: String? = null,
    val relevanceScore: Float = 1.0f,
    val matchedFields: List<String> = emptyList()
)

/**
 * Data class for search filters
 */
data class SearchFilters(
    val fileTypes: Set<String> = emptySet(),
    val resolutions: Set<String> = emptySet(),
    val durationRange: Pair<Long, Long>? = null,
    val sizeRange: Pair<Long, Long>? = null,
    val dateRange: Pair<Long, Long>? = null,
    val minRelevance: Float = 0.0f
)

/**
 * Enum for search categories
 */
enum class SearchCategory {
    ALL,
    VIDEOS,
    AUDIO,
    RECENT,
    FAVORITES,
    BOOKMARKS,
    PLAYLISTS
}

/**
 * Data class for search suggestions
 */
data class SearchSuggestion(
    val text: String,
    val type: SuggestionType,
    val count: Int = 0
)

/**
 * Enum for suggestion types
 */
enum class SuggestionType {
    RECENT_SEARCH,
    POPULAR_SEARCH,
    FILE_NAME,
    GENRE,
    ACTOR,
    DIRECTOR
}

/**
 * Manager for advanced search functionality with real file system search
 */
class AdvancedSearchManager(private val context: Context) {
    
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    private val _searchResults = MutableStateFlow<List<SearchResult>>(emptyList())
    val searchResults: StateFlow<List<SearchResult>> = _searchResults.asStateFlow()
    
    private val _searchSuggestions = MutableStateFlow<List<SearchSuggestion>>(emptyList())
    val searchSuggestions: StateFlow<List<SearchSuggestion>> = _searchSuggestions.asStateFlow()
    
    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()
    
    private val _searchHistory = MutableStateFlow<List<String>>(emptyList())
    val searchHistory: StateFlow<List<String>> = _searchHistory.asStateFlow()
    
    private val _currentQuery = MutableStateFlow("")
    val currentQuery: StateFlow<String> = _currentQuery.asStateFlow()
    
    private val _activeFilters = MutableStateFlow(SearchFilters())
    val activeFilters: StateFlow<SearchFilters> = _activeFilters.asStateFlow()
    
    // Supported video extensions
    private val videoExtensions = setOf(
        "mp4", "mkv", "avi", "mov", "wmv", "flv", "webm", 
        "m4v", "mpg", "mpeg", "3gp", "3g2", "m2ts", "mts", "ts", "vob"
    )
    
    // Supported audio extensions
    private val audioExtensions = setOf(
        "mp3", "wav", "flac", "aac", "ogg", "wma", "m4a", "opus", "alac"
    )
    
    init {
        loadSearchHistory()
        scope.launch {
            generateSuggestions("")
        }
    }
    
    /**
     * Perform search with query and filters
     */
    suspend fun search(query: String, filters: SearchFilters = SearchFilters()) {
        _isSearching.value = true
        _currentQuery.value = query
        _activeFilters.value = filters
        
        // Add to search history
        if (query.isNotEmpty()) {
            addToSearchHistory(query)
        }
        
        val results = withContext(Dispatchers.IO) {
            performFileSystemSearch(query, filters)
        }
        
        _searchResults.value = results
        _isSearching.value = false
    }
    
    /**
     * Perform real file system search
     */
    private suspend fun performFileSystemSearch(query: String, filters: SearchFilters): List<SearchResult> {
        val results = mutableListOf<SearchResult>()
        
        // Search using MediaStore for better performance
        searchMediaStore(query, filters, results)
        
        // Also search external storage directories
        searchFileSystem(query, filters, results)
        
        // Sort by relevance
        return results.sortedByDescending { it.relevanceScore }
    }
    
    /**
     * Search using MediaStore
     */
    private fun searchMediaStore(query: String, filters: SearchFilters, results: MutableList<SearchResult>) {
        val lowerQuery = query.lowercase()
        
        // Video query
        val videoProjection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DATA,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.DATE_MODIFIED,
            MediaStore.Video.Media.RESOLUTION
        )
        
        val selection = if (query.isNotEmpty()) {
            "${MediaStore.Video.Media.DISPLAY_NAME} LIKE ?"
        } else null
        
        val selectionArgs = if (query.isNotEmpty()) {
            arrayOf("%$query%")
        } else null
        
        try {
            context.contentResolver.query(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                videoProjection,
                selection,
                selectionArgs,
                "${MediaStore.Video.Media.DATE_MODIFIED} DESC"
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
                val pathColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA)
                val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
                val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
                val dateColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_MODIFIED)
                val resolutionColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.RESOLUTION)
                
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn).toString()
                    val name = cursor.getString(nameColumn) ?: "Unknown"
                    val path = cursor.getString(pathColumn) ?: ""
                    val duration = cursor.getLong(durationColumn)
                    val size = cursor.getLong(sizeColumn)
                    val dateModified = cursor.getLong(dateColumn) * 1000 // Convert to milliseconds
                    val resolution = cursor.getString(resolutionColumn) ?: "Unknown"
                    
                    val file = File(path)
                    val format = file.extension.uppercase()
                    
                    // Calculate relevance
                    val relevanceScore = calculateRelevance(name, path, lowerQuery)
                    
                    // Apply filters
                    if (passesFilters(format, resolution, duration, size, dateModified, filters)) {
                        results.add(
                            SearchResult(
                                id = id,
                                title = name,
                                path = path,
                                duration = duration,
                                size = size,
                                format = format,
                                resolution = parseResolution(resolution),
                                lastModified = dateModified,
                                relevanceScore = relevanceScore,
                                matchedFields = getMatchedFields(name, path, lowerQuery)
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    /**
     * Search file system directly
     */
    private suspend fun searchFileSystem(query: String, filters: SearchFilters, results: MutableList<SearchResult>) {
        val lowerQuery = query.lowercase()
        val searchPaths = listOf(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES),
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            File(Environment.getExternalStorageDirectory(), "Videos"),
            File(Environment.getExternalStorageDirectory(), "Movies"),
            File(Environment.getExternalStorageDirectory(), "Download")
        )
        
        searchPaths.forEach { directory ->
            if (directory.exists() && directory.isDirectory) {
                searchDirectory(directory, lowerQuery, filters, results)
            }
        }
    }
    
    /**
     * Recursively search directory
     */
    private fun searchDirectory(directory: File, query: String, filters: SearchFilters, results: MutableList<SearchResult>) {
        try {
            directory.listFiles()?.forEach { file ->
                if (file.isDirectory) {
                    // Recursively search subdirectories
                    searchDirectory(file, query, filters, results)
                } else if (isVideoFile(file) || isAudioFile(file)) {
                    val name = file.name
                    val path = file.absolutePath
                    
                    // Check if already in results
                    if (results.any { it.path == path }) return@forEach
                    
                    // Calculate relevance
                    val relevanceScore = calculateRelevance(name, path, query)
                    
                    if (query.isEmpty() || relevanceScore > 0) {
                        val format = file.extension.uppercase()
                        val size = file.length()
                        val lastModified = file.lastModified()
                        
                        // Get media info (simplified for now)
                        val duration = 0L // Would need MediaMetadataRetriever for actual duration
                        val resolution = "Unknown" // Would need MediaMetadataRetriever
                        
                        if (passesFilters(format, resolution, duration, size, lastModified, filters)) {
                            results.add(
                                SearchResult(
                                    id = file.hashCode().toString(),
                                    title = name,
                                    path = path,
                                    duration = duration,
                                    size = size,
                                    format = format,
                                    resolution = resolution,
                                    lastModified = lastModified,
                                    relevanceScore = relevanceScore,
                                    matchedFields = getMatchedFields(name, path, query)
                                )
                            )
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    /**
     * Check if file is a video
     */
    private fun isVideoFile(file: File): Boolean {
        return videoExtensions.contains(file.extension.lowercase())
    }
    
    /**
     * Check if file is audio
     */
    private fun isAudioFile(file: File): Boolean {
        return audioExtensions.contains(file.extension.lowercase())
    }
    
    /**
     * Parse resolution string
     */
    private fun parseResolution(resolution: String): String {
        return when {
            resolution.contains("3840") || resolution.contains("2160") -> "4K"
            resolution.contains("1920") || resolution.contains("1080") -> "1080p"
            resolution.contains("1280") || resolution.contains("720") -> "720p"
            resolution.contains("854") || resolution.contains("480") -> "480p"
            resolution.contains("640") || resolution.contains("360") -> "360p"
            else -> resolution
        }
    }
    
    /**
     * Calculate relevance score for search result
     */
    private fun calculateRelevance(name: String, path: String, query: String): Float {
        if (query.isEmpty()) return 1.0f
        
        var score = 0f
        val lowerName = name.lowercase()
        val lowerPath = path.lowercase()
        
        // Exact match
        if (lowerName == query) return 1.0f
        
        // Title contains query
        if (lowerName.contains(query)) {
            score += 0.8f
        }
        
        // Path contains query
        if (lowerPath.contains(query)) {
            score += 0.4f
        }
        
        // Word-by-word matching
        val queryWords = query.split(" ", "_", "-")
        val nameWords = lowerName.split(" ", "_", "-", ".")
        
        for (queryWord in queryWords) {
            if (queryWord.length > 2) {
                for (nameWord in nameWords) {
                    if (nameWord.contains(queryWord)) {
                        score += 0.2f
                    }
                }
            }
        }
        
        return score.coerceAtMost(1.0f)
    }
    
    /**
     * Check if video passes the applied filters
     */
    private fun passesFilters(
        format: String,
        resolution: String,
        duration: Long,
        size: Long,
        lastModified: Long,
        filters: SearchFilters
    ): Boolean {
        // File type filter
        if (filters.fileTypes.isNotEmpty() && !filters.fileTypes.contains(format)) {
            return false
        }
        
        // Resolution filter
        if (filters.resolutions.isNotEmpty() && !filters.resolutions.contains(resolution)) {
            return false
        }
        
        // Duration filter
        filters.durationRange?.let { (min, max) ->
            if (duration < min || duration > max) {
                return false
            }
        }
        
        // Size filter
        filters.sizeRange?.let { (min, max) ->
            if (size < min || size > max) {
                return false
            }
        }
        
        // Date filter
        filters.dateRange?.let { (min, max) ->
            if (lastModified < min || lastModified > max) {
                return false
            }
        }
        
        return true
    }
    
    /**
     * Get matched fields for highlighting
     */
    private fun getMatchedFields(name: String, path: String, query: String): List<String> {
        if (query.isEmpty()) return emptyList()
        
        val matchedFields = mutableListOf<String>()
        
        if (name.lowercase().contains(query)) {
            matchedFields.add("title")
        }
        
        if (path.lowercase().contains(query)) {
            matchedFields.add("path")
        }
        
        return matchedFields
    }
    
    /**
     * Generate search suggestions based on query
     */
    suspend fun generateSuggestions(query: String) {
        val suggestions = mutableListOf<SearchSuggestion>()
        
        // Recent searches
        val recentSearches = _searchHistory.value.filter { 
            it.contains(query, ignoreCase = true) 
        }.take(3)
        
        suggestions.addAll(recentSearches.map { 
            SearchSuggestion(it, SuggestionType.RECENT_SEARCH) 
        })
        
        // File name suggestions from recent results
        if (query.length >= 2) {
            val recentResults = _searchResults.value
                .filter { it.title.contains(query, ignoreCase = true) }
                .take(5)
                .map { SearchSuggestion(it.title, SuggestionType.FILE_NAME) }
            
            suggestions.addAll(recentResults)
        }
        
        _searchSuggestions.value = suggestions.distinctBy { it.text }
    }
    
    /**
     * Add query to search history
     */
    private fun addToSearchHistory(query: String) {
        val currentHistory = _searchHistory.value.toMutableList()
        
        // Remove if already exists
        currentHistory.remove(query)
        
        // Add to beginning
        currentHistory.add(0, query)
        
        // Keep only last 20 searches
        if (currentHistory.size > 20) {
            currentHistory.removeAt(currentHistory.size - 1)
        }
        
        _searchHistory.value = currentHistory
        saveSearchHistory()
    }
    
    /**
     * Load search history from SharedPreferences
     */
    private fun loadSearchHistory() {
        val prefs = context.getSharedPreferences("search_history", Context.MODE_PRIVATE)
        val historySet = prefs.getStringSet("history", emptySet()) ?: emptySet()
        _searchHistory.value = historySet.toList()
    }
    
    /**
     * Save search history to SharedPreferences
     */
    private fun saveSearchHistory() {
        val prefs = context.getSharedPreferences("search_history", Context.MODE_PRIVATE)
        prefs.edit().putStringSet("history", _searchHistory.value.toSet()).apply()
    }
    
    /**
     * Clear search history
     */
    fun clearSearchHistory() {
        _searchHistory.value = emptyList()
        saveSearchHistory()
    }
    
    /**
     * Clear search results
     */
    fun clearResults() {
        _searchResults.value = emptyList()
        _currentQuery.value = ""
    }
    
    /**
     * Update search filters
     */
    fun updateFilters(filters: SearchFilters) {
        _activeFilters.value = filters
        
        // Re-search with new filters if there's an active query
        if (_currentQuery.value.isNotEmpty()) {
            scope.launch {
                search(_currentQuery.value, filters)
            }
        }
    }
    
    /**
     * Get available file types for filtering
     */
    fun getAvailableFileTypes(): List<String> {
        val types = mutableSetOf<String>()
        types.addAll(videoExtensions.map { it.uppercase() })
        types.addAll(audioExtensions.map { it.uppercase() })
        return types.sorted()
    }
    
    /**
     * Get available resolutions for filtering
     */
    fun getAvailableResolutions(): List<String> {
        return listOf("360p", "480p", "720p", "1080p", "4K", "Unknown")
    }
}

/**
 * ViewModel for advanced search
 */
class AdvancedSearchViewModel(private val searchManager: AdvancedSearchManager) : ViewModel() {
    
    val searchResults = searchManager.searchResults
    val searchSuggestions = searchManager.searchSuggestions
    val isSearching = searchManager.isSearching
    val searchHistory = searchManager.searchHistory
    val currentQuery = searchManager.currentQuery
    val activeFilters = searchManager.activeFilters
    
    fun search(query: String, filters: SearchFilters = SearchFilters()) {
        viewModelScope.launch {
            searchManager.search(query, filters)
        }
    }
    
    fun generateSuggestions(query: String) {
        viewModelScope.launch {
            searchManager.generateSuggestions(query)
        }
    }
    
    fun clearSearchHistory() {
        searchManager.clearSearchHistory()
    }
    
    fun clearResults() {
        searchManager.clearResults()
    }
    
    fun updateFilters(filters: SearchFilters) {
        searchManager.updateFilters(filters)
    }
    
    fun getAvailableFileTypes(): List<String> {
        return searchManager.getAvailableFileTypes()
    }
    
    fun getAvailableResolutions(): List<String> {
        return searchManager.getAvailableResolutions()
    }
}

/**
 * Composable for advanced search screen
 */
@Composable
fun AdvancedSearchScreen(
    searchResults: List<SearchResult>,
    searchSuggestions: List<SearchSuggestion>,
    isSearching: Boolean,
    searchHistory: List<String>,
    currentQuery: String,
    activeFilters: SearchFilters,
    onSearch: (String, SearchFilters) -> Unit,
    onSuggestionClick: (String) -> Unit,
    onResultClick: (SearchResult) -> Unit,
    onClearHistory: () -> Unit,
    onUpdateFilters: (SearchFilters) -> Unit,
    availableFileTypes: List<String>,
    availableResolutions: List<String>,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf(currentQuery) }
    var showFilters by remember { mutableStateOf(false) }
    
    Column(modifier = modifier.fillMaxSize()) {
        // Search header
        SearchHeader(
            query = searchQuery,
            onQueryChange = { 
                searchQuery = it
                onSuggestionClick(it) // Generate suggestions
            },
            onSearch = { onSearch(searchQuery, activeFilters) },
            onToggleFilters = { showFilters = !showFilters },
            hasActiveFilters = activeFilters != SearchFilters()
        )
        
        // Filters panel
        if (showFilters) {
            SearchFiltersPanel(
                filters = activeFilters,
                availableFileTypes = availableFileTypes,
                availableResolutions = availableResolutions,
                onFiltersChanged = onUpdateFilters
            )
        }
        
        // Content
        if (searchQuery.isEmpty()) {
            // Search suggestions and history
            SearchSuggestionsContent(
                suggestions = searchSuggestions,
                history = searchHistory,
                onSuggestionClick = { suggestion ->
                    searchQuery = suggestion
                    onSearch(suggestion, activeFilters)
                },
                onClearHistory = onClearHistory
            )
        } else {
            // Search results
            SearchResultsContent(
                results = searchResults,
                isSearching = isSearching,
                query = searchQuery,
                onResultClick = onResultClick
            )
        }
    }
}

/**
 * Search header with input and filters
 */
@Composable
private fun SearchHeader(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onToggleFilters: () -> Unit,
    hasActiveFilters: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.05f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                placeholder = { Text("Search videos, audio, and more...") },
                leadingIcon = {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = "Search",
                        tint = Color(0xFF00BCD4)
                    )
                },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { onQueryChange("") }) {
                            Icon(
                                Icons.Default.Clear,
                                contentDescription = "Clear",
                                tint = Color.White.copy(alpha = 0.7f)
                            )
                        }
                    }
                },
                modifier = Modifier.weight(1f),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF00BCD4),
                    unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                singleLine = true
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            
            // Filters button
            IconButton(
                onClick = onToggleFilters,
                modifier = Modifier
                    .background(
                        if (hasActiveFilters) Color(0xFF00BCD4).copy(alpha = 0.2f)
                        else Color.Transparent,
                        RoundedCornerShape(8.dp)
                    )
            ) {
                Icon(
                    Icons.Default.FilterList,
                    contentDescription = "Filters",
                    tint = if (hasActiveFilters) Color(0xFF00BCD4) else Color.White.copy(alpha = 0.7f)
                )
            }
        }
    }
}

/**
 * Search filters panel
 */
@Composable
private fun SearchFiltersPanel(
    filters: SearchFilters,
    availableFileTypes: List<String>,
    availableResolutions: List<String>,
    onFiltersChanged: (SearchFilters) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.05f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Filters",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // File types
            Text(
                text = "File Types",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(availableFileTypes.take(10)) { fileType ->
                    FilterChip(
                        selected = filters.fileTypes.contains(fileType),
                        onClick = {
                            val newFileTypes = if (filters.fileTypes.contains(fileType)) {
                                filters.fileTypes - fileType
                            } else {
                                filters.fileTypes + fileType
                            }
                            onFiltersChanged(filters.copy(fileTypes = newFileTypes))
                        },
                        label = { Text(fileType) }
                    )
                }
            }
            
            // Resolutions
            Text(
                text = "Resolution",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(availableResolutions) { resolution ->
                    FilterChip(
                        selected = filters.resolutions.contains(resolution),
                        onClick = {
                            val newResolutions = if (filters.resolutions.contains(resolution)) {
                                filters.resolutions - resolution
                            } else {
                                filters.resolutions + resolution
                            }
                            onFiltersChanged(filters.copy(resolutions = newResolutions))
                        },
                        label = { Text(resolution) }
                    )
                }
            }
        }
    }
}

/**
 * Search suggestions and history content
 */
@Composable
private fun SearchSuggestionsContent(
    suggestions: List<SearchSuggestion>,
    history: List<String>,
    onSuggestionClick: (String) -> Unit,
    onClearHistory: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Search suggestions
        if (suggestions.isNotEmpty()) {
            item {
                SuggestionsSection(
                    suggestions = suggestions,
                    onSuggestionClick = onSuggestionClick
                )
            }
        }
        
        // Search history
        if (history.isNotEmpty()) {
            item {
                SearchHistorySection(
                    history = history,
                    onHistoryClick = onSuggestionClick,
                    onClearHistory = onClearHistory
                )
            }
        }
    }
}

/**
 * Search results content
 */
@Composable
private fun SearchResultsContent(
    results: List<SearchResult>,
    isSearching: Boolean,
    query: String,
    onResultClick: (SearchResult) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Loading indicator
        if (isSearching) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color(0xFF00BCD4))
                }
            }
        }
        
        // Results header
        if (!isSearching) {
            item {
                Text(
                    text = "${results.size} results for \"$query\"",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 14.sp
                )
            }
        }
        
        // Results list
        items(results) { result ->
            SearchResultItem(
                result = result,
                onClick = { onResultClick(result) }
            )
        }
        
        // No results message
        if (!isSearching && results.isEmpty() && query.isNotEmpty()) {
            item {
                NoResultsMessage(query = query)
            }
        }
    }
}

/**
 * Suggestions section
 */
@Composable
private fun SuggestionsSection(
    suggestions: List<SearchSuggestion>,
    onSuggestionClick: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.05f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Suggestions",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            suggestions.forEach { suggestion ->
                SuggestionItem(
                    suggestion = suggestion,
                    onClick = { onSuggestionClick(suggestion.text) }
                )
            }
        }
    }
}

/**
 * Search history section
 */
@Composable
private fun SearchHistorySection(
    history: List<String>,
    onHistoryClick: (String) -> Unit,
    onClearHistory: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.05f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent Searches",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                
                TextButton(onClick = onClearHistory) {
                    Text(
                        text = "Clear",
                        color = Color(0xFF00BCD4)
                    )
                }
            }
            
            history.take(5).forEach { query ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onHistoryClick(query) }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.History,
                        contentDescription = "Recent",
                        tint = Color.White.copy(alpha = 0.5f),
                        modifier = Modifier.size(16.dp)
                    )
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    Text(
                        text = query,
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

/**
 * Individual suggestion item
 */
@Composable
private fun SuggestionItem(
    suggestion: SearchSuggestion,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = when (suggestion.type) {
                SuggestionType.RECENT_SEARCH -> Icons.Default.History
                SuggestionType.POPULAR_SEARCH -> Icons.Default.TrendingUp
                SuggestionType.FILE_NAME -> Icons.Default.VideoFile
                else -> Icons.Default.Search
            },
            contentDescription = suggestion.type.name,
            tint = Color(0xFF00BCD4),
            modifier = Modifier.size(16.dp)
        )
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Text(
            text = suggestion.text,
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 14.sp,
            modifier = Modifier.weight(1f)
        )
        
        if (suggestion.count > 0) {
            Text(
                text = "${suggestion.count}",
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 12.sp
            )
        }
    }
}

/**
 * Search result item
 */
@Composable
private fun SearchResultItem(
    result: SearchResult,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.05f)
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Thumbnail placeholder
            Box(
                modifier = Modifier
                    .size(60.dp, 34.dp)
                    .background(
                        Color.White.copy(alpha = 0.1f),
                        RoundedCornerShape(4.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.VideoFile,
                    contentDescription = "Video",
                    tint = Color(0xFF00BCD4),
                    modifier = Modifier.size(20.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Video info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = result.title,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = formatDuration(result.duration),
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 12.sp
                    )
                    
                    Text(
                        text = result.resolution,
                        color = Color(0xFF00BCD4),
                        fontSize = 12.sp
                    )
                    
                    Text(
                        text = formatFileSize(result.size),
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 12.sp
                    )
                }
                
                Text(
                    text = result.path,
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            // Relevance score
            if (result.relevanceScore > 0) {
                Text(
                    text = "${(result.relevanceScore * 100).toInt()}%",
                    color = Color(0xFF4CAF50),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * No results message
 */
@Composable
private fun NoResultsMessage(query: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Default.SearchOff,
            contentDescription = "No results",
            tint = Color.White.copy(alpha = 0.5f),
            modifier = Modifier.size(48.dp)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "No results found for \"$query\"",
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium
        )
        
        Text(
            text = "Try different keywords or adjust your filters",
            color = Color.White.copy(alpha = 0.5f),
            fontSize = 14.sp
        )
    }
}

/**
 * Format duration in milliseconds to readable string
 */
private fun formatDuration(millis: Long): String {
    if (millis == 0L) return "--:--"
    
    val totalSeconds = millis / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    
    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%d:%02d", minutes, seconds)
    }
}

/**
 * Format file size in bytes to readable string
 */
private fun formatFileSize(bytes: Long): String {
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    val gb = mb / 1024.0
    
    return when {
        gb >= 1 -> String.format("%.1f GB", gb)
        mb >= 1 -> String.format("%.1f MB", mb)
        kb >= 1 -> String.format("%.1f KB", kb)
        else -> "$bytes B"
    }
}