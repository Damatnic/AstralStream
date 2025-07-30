package com.astralplayer.nextplayer.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.astralplayer.nextplayer.data.RecentFile
import com.astralplayer.nextplayer.data.repository.RecentFilesRepository
import com.astralplayer.nextplayer.feature.search.*
import com.astralplayer.nextplayer.ui.components.*
import kotlinx.coroutines.flow.collectAsState
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * Advanced Search Screen with AI suggestions and filters
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdvancedSearchScreen(
    navController: NavController,
    recentFilesRepository: RecentFilesRepository
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val searchFocusRequester = remember { FocusRequester() }
    
    val searchManager = remember { AdvancedSearchManager(context) }
    
    // State
    var searchQuery by remember { mutableStateOf("") }
    var showFilters by remember { mutableStateOf(false) }
    var currentFilters by remember { mutableStateOf(SearchFilters()) }
    
    val allVideos by recentFilesRepository.getAllFiles().collectAsState(initial = emptyList())
    val searchResults by searchManager.searchResults.collectAsState()
    val suggestions by searchManager.suggestions.collectAsState()
    val isSearching by searchManager.isSearching.collectAsState()
    
    // Load saved filters
    LaunchedEffect(Unit) {
        currentFilters = searchManager.getSearchFilters()
        searchFocusRequester.requestFocus()
    }
    
    // Update suggestions as user types
    LaunchedEffect(searchQuery) {
        if (searchQuery.isNotEmpty()) {
            searchManager.getSuggestions(searchQuery, allVideos)
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Search Header
            SearchHeader(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                onSearch = {
                    scope.launch {
                        focusManager.clearFocus()
                        searchManager.search(searchQuery, currentFilters, allVideos)
                    }
                },
                onBack = { navController.navigateUp() },
                onFilterClick = { showFilters = !showFilters },
                hasActiveFilters = hasActiveFilters(currentFilters),
                focusRequester = searchFocusRequester
            )
            
            // Filter chips
            AnimatedVisibility(
                visible = hasActiveFilters(currentFilters),
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                ActiveFilterChips(
                    filters = currentFilters,
                    onRemoveFilter = { filter ->
                        currentFilters = removeFilter(currentFilters, filter)
                        scope.launch {
                            searchManager.saveSearchFilters(currentFilters)
                            searchManager.search(searchQuery, currentFilters, allVideos)
                        }
                    },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
            
            // Suggestions
            AnimatedVisibility(
                visible = searchQuery.isNotEmpty() && suggestions.isNotEmpty() && searchResults.isEmpty(),
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                SearchSuggestions(
                    suggestions = suggestions,
                    onSuggestionClick = { suggestion ->
                        when (suggestion.type) {
                            SuggestionType.HISTORY, SuggestionType.FILE -> {
                                searchQuery = suggestion.text
                                scope.launch {
                                    searchManager.search(suggestion.text, currentFilters, allVideos)
                                }
                            }
                            SuggestionType.FILTER, SuggestionType.SMART -> {
                                // Apply filter from suggestion
                                currentFilters = applyFilterFromSuggestion(currentFilters, suggestion)
                                scope.launch {
                                    searchManager.saveSearchFilters(currentFilters)
                                    searchManager.search(searchQuery, currentFilters, allVideos)
                                }
                            }
                            SuggestionType.RELATED -> {
                                searchQuery = suggestion.text
                                scope.launch {
                                    searchManager.search(suggestion.text, currentFilters, allVideos)
                                }
                            }
                        }
                    }
                )
            }
            
            // Search Results
            when {
                isSearching -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                searchQuery.isEmpty() && searchResults.isEmpty() -> {
                    // Show recent searches and trending
                    RecentAndTrending(
                        searchManager = searchManager,
                        allVideos = allVideos,
                        onSearchClick = { query ->
                            searchQuery = query
                            scope.launch {
                                searchManager.search(query, currentFilters, allVideos)
                            }
                        }
                    )
                }
                searchResults.isEmpty() -> {
                    NoSearchResults(
                        query = searchQuery,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                else -> {
                    SearchResultsList(
                        results = searchResults,
                        onVideoClick = { video ->
                            navController.navigate("player/${video.id}")
                        }
                    )
                }
            }
        }
        
        // Filter Dialog
        if (showFilters) {
            SearchFiltersDialog(
                currentFilters = currentFilters,
                onFiltersChanged = { filters ->
                    currentFilters = filters
                    scope.launch {
                        searchManager.saveSearchFilters(filters)
                        if (searchQuery.isNotEmpty()) {
                            searchManager.search(searchQuery, filters, allVideos)
                        }
                    }
                },
                onDismiss = { showFilters = false }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchHeader(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onBack: () -> Unit,
    onFilterClick: () -> Unit,
    hasActiveFilters: Boolean,
    focusRequester: FocusRequester
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Back button
            BubbleIconButton(
                onClick = onBack,
                icon = Icons.Default.ArrowBack,
                size = 40,
                iconSize = 20
            )
            
            // Search field
            BubbleTextField(
                value = query,
                onValueChange = onQueryChange,
                placeholder = "Search videos with AI...",
                leadingIcon = Icons.Default.Search,
                trailingIcon = if (query.isNotEmpty()) Icons.Default.Clear else null,
                onTrailingIconClick = if (query.isNotEmpty()) {{ onQueryChange("") }} else null,
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focusRequester),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onSearch() })
            )
            
            // Filter button
            Box {
                BubbleIconButton(
                    onClick = onFilterClick,
                    icon = Icons.Default.FilterList,
                    size = 40,
                    iconSize = 20,
                    containerColor = if (hasActiveFilters) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    },
                    contentColor = if (hasActiveFilters) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
                
                // Active filter indicator
                if (hasActiveFilters) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .offset(x = (-4).dp, y = (-4).dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.error)
                            .align(Alignment.TopEnd)
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchSuggestions(
    suggestions: List<SearchSuggestion>,
    onSuggestionClick: (SearchSuggestion) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(suggestions) { suggestion ->
            SuggestionItem(
                suggestion = suggestion,
                onClick = { onSuggestionClick(suggestion) }
            )
        }
    }
}

@Composable
private fun SuggestionItem(
    suggestion: SearchSuggestion,
    onClick: () -> Unit
) {
    BubbleCard(
        onClick = onClick,
        elevation = 2,
        cornerRadius = 16,
        containerColor = when (suggestion.type) {
            SuggestionType.SMART -> MaterialTheme.colorScheme.primaryContainer
            SuggestionType.FILTER -> MaterialTheme.colorScheme.secondaryContainer
            else -> MaterialTheme.colorScheme.surfaceVariant
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = when (suggestion.icon) {
                    "history" -> Icons.Default.History
                    "video" -> Icons.Default.VideoLibrary
                    "filter" -> Icons.Default.FilterList
                    "tag" -> Icons.Default.Tag
                    "calendar" -> Icons.Default.CalendarToday
                    "timer" -> Icons.Default.Timer
                    "hd" -> Icons.Default.HighQuality
                    else -> Icons.Default.Search
                },
                contentDescription = null,
                tint = when (suggestion.type) {
                    SuggestionType.SMART -> MaterialTheme.colorScheme.onPrimaryContainer
                    SuggestionType.FILTER -> MaterialTheme.colorScheme.onSecondaryContainer
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = suggestion.text,
                    style = MaterialTheme.typography.bodyLarge,
                    color = when (suggestion.type) {
                        SuggestionType.SMART -> MaterialTheme.colorScheme.onPrimaryContainer
                        SuggestionType.FILTER -> MaterialTheme.colorScheme.onSecondaryContainer
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
                
                if (suggestion.type == SuggestionType.SMART || suggestion.type == SuggestionType.FILTER) {
                    Text(
                        text = when (suggestion.type) {
                            SuggestionType.SMART -> "AI Suggestion"
                            SuggestionType.FILTER -> "Quick Filter"
                            else -> ""
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = when (suggestion.type) {
                            SuggestionType.SMART -> MaterialTheme.colorScheme.onPrimaryContainer
                            SuggestionType.FILTER -> MaterialTheme.colorScheme.onSecondaryContainer
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }.copy(alpha = 0.7f)
                    )
                }
            }
            
            if (suggestion.type == SuggestionType.HISTORY) {
                Icon(
                    Icons.Default.NorthWest,
                    contentDescription = "Use suggestion",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun ActiveFilterChips(
    filters: SearchFilters,
    onRemoveFilter: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Duration filter
        if (filters.minDuration != null || filters.maxDuration != null) {
            item {
                FilterChip(
                    label = buildString {
                        append("Duration: ")
                        if (filters.minDuration != null && filters.maxDuration != null) {
                            append("${formatDuration(filters.minDuration)} - ${formatDuration(filters.maxDuration)}")
                        } else if (filters.minDuration != null) {
                            append("> ${formatDuration(filters.minDuration)}")
                        } else {
                            append("< ${formatDuration(filters.maxDuration!!)}")
                        }
                    },
                    onRemove = { onRemoveFilter("duration") }
                )
            }
        }
        
        // Date filter
        filters.dateRange?.let { range ->
            item {
                FilterChip(
                    label = "Date: ${formatDateRange(range)}",
                    onRemove = { onRemoveFilter("date") }
                )
            }
        }
        
        // Resolution filter
        if (filters.resolutions.isNotEmpty()) {
            item {
                FilterChip(
                    label = "Resolution: ${filters.resolutions.joinToString(", ")}",
                    onRemove = { onRemoveFilter("resolution") }
                )
            }
        }
        
        // Format filter
        if (filters.formats.isNotEmpty()) {
            item {
                FilterChip(
                    label = "Format: ${filters.formats.joinToString(", ")}",
                    onRemove = { onRemoveFilter("format") }
                )
            }
        }
        
        // Source filter
        if (filters.sources.isNotEmpty()) {
            item {
                FilterChip(
                    label = "Source: ${filters.sources.joinToString(", ")}",
                    onRemove = { onRemoveFilter("source") }
                )
            }
        }
        
        // Favorites filter
        if (filters.favorites) {
            item {
                FilterChip(
                    label = "Favorites only",
                    onRemove = { onRemoveFilter("favorites") }
                )
            }
        }
    }
}

@Composable
private fun FilterChip(
    label: String,
    onRemove: () -> Unit
) {
    BubbleCard(
        elevation = 0,
        cornerRadius = 20,
        containerColor = MaterialTheme.colorScheme.secondaryContainer
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            
            Icon(
                Icons.Default.Close,
                contentDescription = "Remove filter",
                modifier = Modifier
                    .size(16.dp)
                    .clickable { onRemove() },
                tint = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

@Composable
private fun SearchResultsList(
    results: List<SearchResult>,
    onVideoClick: (RecentFile) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "${results.size} results found",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        items(results) { result ->
            SearchResultItem(
                result = result,
                onClick = { onVideoClick(result.video) }
            )
        }
    }
}

@Composable
private fun SearchResultItem(
    result: SearchResult,
    onClick: () -> Unit
) {
    BubbleCard(
        onClick = onClick,
        elevation = 2,
        cornerRadius = 20
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Thumbnail
            VideoThumbnail(
                thumbnailPath = result.video.thumbnailPath,
                modifier = Modifier.size(width = 120.dp, height = 80.dp),
                cornerRadius = 12.dp
            )
            
            // Video info
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Title with highlighted matches
                Text(
                    text = result.video.fileName,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                
                // Metadata
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if ("filename" in result.matchedFields) {
                        Chip(
                            label = "Name match",
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    
                    if ("path" in result.matchedFields) {
                        Chip(
                            label = "Path match",
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
                
                // Duration and size
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = formatDuration(result.video.duration),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Text(
                        text = formatFileSize(result.video.fileSize),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                // Relevance score indicator
                LinearProgressIndicator(
                    progress = { (result.relevanceScore / 20f).coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .clip(RoundedCornerShape(1.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }
        }
    }
}

@Composable
private fun Chip(
    label: String,
    color: Color
) {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = color.copy(alpha = 0.2f)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
private fun RecentAndTrending(
    searchManager: AdvancedSearchManager,
    allVideos: List<RecentFile>,
    onSearchClick: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    var recentSearches by remember { mutableStateOf<List<SearchSuggestion>>(emptyList()) }
    
    LaunchedEffect(Unit) {
        recentSearches = searchManager.getSuggestions("", allVideos)
    }
    
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Recent searches
        if (recentSearches.isNotEmpty()) {
            item {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Recent Searches",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        
                        TextButton(
                            onClick = {
                                scope.launch {
                                    searchManager.clearSearchHistory()
                                    recentSearches = emptyList()
                                }
                            }
                        ) {
                            Text("Clear")
                        }
                    }
                    
                    recentSearches.forEach { suggestion ->
                        SuggestionItem(
                            suggestion = suggestion,
                            onClick = { onSearchClick(suggestion.text) }
                        )
                    }
                }
            }
        }
        
        // Trending categories
        item {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Trending Categories",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val categories = listOf(
                        "Recently Added" to Icons.Default.NewReleases,
                        "Most Played" to Icons.Default.TrendingUp,
                        "HD Videos" to Icons.Default.HighQuality,
                        "Long Videos" to Icons.Default.MovieFilter,
                        "Downloads" to Icons.Default.Download
                    )
                    
                    items(categories) { (category, icon) ->
                        TrendingCategoryCard(
                            title = category,
                            icon = icon,
                            onClick = { onSearchClick(category) }
                        )
                    }
                }
            }
        }
        
        // Quick actions
        item {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Quick Actions",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    QuickActionButton(
                        text = "Today's Videos",
                        icon = Icons.Default.Today,
                        modifier = Modifier.weight(1f),
                        onClick = { onSearchClick("today") }
                    )
                    
                    QuickActionButton(
                        text = "Favorites",
                        icon = Icons.Default.Favorite,
                        modifier = Modifier.weight(1f),
                        onClick = { onSearchClick("favorites") }
                    )
                }
            }
        }
    }
}

@Composable
private fun TrendingCategoryCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    BubbleCard(
        onClick = onClick,
        elevation = 4,
        cornerRadius = 20,
        containerColor = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier.size(width = 140.dp, height = 100.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(32.dp)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun QuickActionButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    BubbleButton(
        onClick = onClick,
        icon = icon,
        text = text,
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
    )
}

@Composable
private fun NoSearchResults(
    query: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.SearchOff,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "No results for \"$query\"",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Text(
            text = "Try different keywords or adjust filters",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
    }
}

// Helper functions
private fun hasActiveFilters(filters: SearchFilters): Boolean {
    return filters.minDuration != null ||
           filters.maxDuration != null ||
           filters.dateRange != null ||
           filters.minFileSize != null ||
           filters.maxFileSize != null ||
           filters.resolutions.isNotEmpty() ||
           filters.formats.isNotEmpty() ||
           filters.sources.isNotEmpty() ||
           filters.favorites ||
           filters.hasSubtitles != null
}

private fun removeFilter(filters: SearchFilters, filterType: String): SearchFilters {
    return when (filterType) {
        "duration" -> filters.copy(minDuration = null, maxDuration = null)
        "date" -> filters.copy(dateRange = null)
        "resolution" -> filters.copy(resolutions = emptySet())
        "format" -> filters.copy(formats = emptySet())
        "source" -> filters.copy(sources = emptySet())
        "favorites" -> filters.copy(favorites = false)
        else -> filters
    }
}

private fun applyFilterFromSuggestion(
    filters: SearchFilters,
    suggestion: SearchSuggestion
): SearchFilters {
    val filterType = suggestion.metadata["filter"] ?: return filters
    val filterValue = suggestion.metadata["value"] ?: return filters
    
    return when (filterType) {
        "date" -> when (filterValue) {
            "week" -> {
                val now = System.currentTimeMillis()
                val weekAgo = now - (7 * 24 * 60 * 60 * 1000)
                filters.copy(dateRange = DateRange(weekAgo, now))
            }
            else -> filters
        }
        "duration" -> when (filterValue) {
            "short" -> filters.copy(maxDuration = 10 * 60 * 1000) // 10 minutes
            "long" -> filters.copy(minDuration = 30 * 60 * 1000) // 30 minutes
            else -> filters
        }
        "resolution" -> when (filterValue) {
            "hd" -> filters.copy(resolutions = setOf("720p", "1080p", "4K"))
            else -> filters
        }
        else -> filters
    }
}

private fun formatDuration(millis: Long): String {
    val seconds = millis / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    
    return when {
        hours > 0 -> "${hours}h ${minutes % 60}m"
        minutes > 0 -> "${minutes}m"
        else -> "${seconds}s"
    }
}

private fun formatFileSize(bytes: Long): String {
    return when {
        bytes >= 1_000_000_000 -> "%.1f GB".format(bytes / 1_000_000_000.0)
        bytes >= 1_000_000 -> "%.1f MB".format(bytes / 1_000_000.0)
        bytes >= 1_000 -> "%.1f KB".format(bytes / 1_000.0)
        else -> "$bytes B"
    }
}

private fun formatDateRange(range: DateRange): String {
    val formatter = SimpleDateFormat("MMM dd", Locale.getDefault())
    val start = formatter.format(range.startDate)
    val end = formatter.format(range.endDate)
    return "$start - $end"
}