package com.astralplayer.nextplayer.ui.dialogs

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.astralplayer.nextplayer.feature.search.*
import com.astralplayer.nextplayer.ui.components.*
import com.astralplayer.nextplayer.ui.theme.glassmorphicSurface
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * Search Filters Dialog
 * Allows users to set advanced search filters
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchFiltersDialog(
    currentFilters: SearchFilters,
    onFiltersChanged: (SearchFilters) -> Unit,
    onDismiss: () -> Unit
) {
    var filters by remember { mutableStateOf(currentFilters) }
    var selectedTab by remember { mutableStateOf(0) }
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.85f)
                .glassmorphicSurface(
                    cornerRadius = 28.dp,
                    glassColor = MaterialTheme.colorScheme.surface,
                    glassAlpha = 0.95f,
                    blurRadius = 16.dp,
                    borderWidth = 1.dp,
                    borderBrush = Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                            MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f)
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Header
                FilterDialogHeader(
                    onDismiss = onDismiss,
                    onReset = { filters = SearchFilters() },
                    onApply = {
                        onFiltersChanged(filters)
                        onDismiss()
                    }
                )
                
                // Tab Row
                BubbleTabRow(
                    selectedTabIndex = selectedTab,
                    tabs = listOf("Basic", "Advanced", "Sort"),
                    onTabSelected = { selectedTab = it },
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Content
                AnimatedContent(
                    targetState = selectedTab,
                    transitionSpec = {
                        slideInHorizontally { width -> width } + fadeIn() with
                        slideOutHorizontally { width -> -width } + fadeOut()
                    }
                ) { tab ->
                    when (tab) {
                        0 -> BasicFiltersTab(
                            filters = filters,
                            onFiltersChanged = { filters = it }
                        )
                        1 -> AdvancedFiltersTab(
                            filters = filters,
                            onFiltersChanged = { filters = it }
                        )
                        2 -> SortOptionsTab(
                            filters = filters,
                            onFiltersChanged = { filters = it }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterDialogHeader(
    onDismiss: () -> Unit,
    onReset: () -> Unit,
    onApply: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Search Filters",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextButton(onClick = onReset) {
                    Text("Reset")
                }
                
                BubbleButton(
                    onClick = onApply,
                    text = "Apply",
                    icon = Icons.Default.Check,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}

@Composable
private fun BasicFiltersTab(
    filters: SearchFilters,
    onFiltersChanged: (SearchFilters) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Duration Filter
        item {
            DurationFilterSection(
                minDuration = filters.minDuration,
                maxDuration = filters.maxDuration,
                onDurationChanged = { min, max ->
                    onFiltersChanged(filters.copy(minDuration = min, maxDuration = max))
                }
            )
        }
        
        // Date Filter
        item {
            DateFilterSection(
                dateRange = filters.dateRange,
                onDateRangeChanged = { range ->
                    onFiltersChanged(filters.copy(dateRange = range))
                }
            )
        }
        
        // Resolution Filter
        item {
            ResolutionFilterSection(
                selectedResolutions = filters.resolutions,
                onResolutionsChanged = { resolutions ->
                    onFiltersChanged(filters.copy(resolutions = resolutions))
                }
            )
        }
        
        // Format Filter
        item {
            FormatFilterSection(
                selectedFormats = filters.formats,
                onFormatsChanged = { formats ->
                    onFiltersChanged(filters.copy(formats = formats))
                }
            )
        }
    }
}

@Composable
private fun AdvancedFiltersTab(
    filters: SearchFilters,
    onFiltersChanged: (SearchFilters) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Source Filter
        item {
            SourceFilterSection(
                selectedSources = filters.sources,
                onSourcesChanged = { sources ->
                    onFiltersChanged(filters.copy(sources = sources))
                }
            )
        }
        
        // File Size Filter
        item {
            FileSizeFilterSection(
                minSize = filters.minFileSize,
                maxSize = filters.maxFileSize,
                onSizeChanged = { min, max ->
                    onFiltersChanged(filters.copy(minFileSize = min, maxFileSize = max))
                }
            )
        }
        
        // Other Filters
        item {
            OtherFiltersSection(
                favorites = filters.favorites,
                hasSubtitles = filters.hasSubtitles,
                onFavoritesChanged = { favorites ->
                    onFiltersChanged(filters.copy(favorites = favorites))
                },
                onSubtitlesChanged = { hasSubtitles ->
                    onFiltersChanged(filters.copy(hasSubtitles = hasSubtitles))
                }
            )
        }
    }
}

@Composable
private fun SortOptionsTab(
    filters: SearchFilters,
    onFiltersChanged: (SearchFilters) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Sort By
        BubbleCard(
            elevation = 2,
            cornerRadius = 20
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Sort By",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                SortBy.values().forEach { sortBy ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .selectable(
                                selected = filters.sortBy == sortBy,
                                onClick = { onFiltersChanged(filters.copy(sortBy = sortBy)) }
                            )
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = filters.sortBy == sortBy,
                            onClick = null
                        )
                        
                        Spacer(modifier = Modifier.width(12.dp))
                        
                        Text(
                            text = when (sortBy) {
                                SortBy.RELEVANCE -> "Relevance"
                                SortBy.NAME -> "Name"
                                SortBy.DATE -> "Date"
                                SortBy.SIZE -> "Size"
                                SortBy.DURATION -> "Duration"
                                SortBy.PLAY_COUNT -> "Play Count"
                            },
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        }
        
        // Sort Order
        BubbleCard(
            elevation = 2,
            cornerRadius = 20
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Sort Order",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SortOrderOption(
                        label = "Ascending",
                        icon = Icons.Default.ArrowUpward,
                        selected = filters.sortOrder == SortOrder.ASCENDING,
                        onClick = { onFiltersChanged(filters.copy(sortOrder = SortOrder.ASCENDING)) },
                        modifier = Modifier.weight(1f)
                    )
                    
                    SortOrderOption(
                        label = "Descending",
                        icon = Icons.Default.ArrowDownward,
                        selected = filters.sortOrder == SortOrder.DESCENDING,
                        onClick = { onFiltersChanged(filters.copy(sortOrder = SortOrder.DESCENDING)) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun DurationFilterSection(
    minDuration: Long?,
    maxDuration: Long?,
    onDurationChanged: (Long?, Long?) -> Unit
) {
    BubbleCard(
        elevation = 2,
        cornerRadius = 20
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Duration",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            // Quick presets
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val presets = listOf(
                    "< 5 min" to (null to 5L * 60 * 1000),
                    "5-30 min" to (5L * 60 * 1000 to 30L * 60 * 1000),
                    "30-60 min" to (30L * 60 * 1000 to 60L * 60 * 1000),
                    "> 60 min" to (60L * 60 * 1000 to null)
                )
                
                items(presets) { (label, range) ->
                    val (min, max) = range
                    FilterChip(
                        selected = minDuration == min && maxDuration == max,
                        onClick = { onDurationChanged(min, max) },
                        label = { Text(label) }
                    )
                }
            }
            
            // Custom range
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                DurationInput(
                    value = minDuration,
                    label = "Min",
                    onValueChange = { onDurationChanged(it, maxDuration) },
                    modifier = Modifier.weight(1f)
                )
                
                Text(
                    text = "to",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
                
                DurationInput(
                    value = maxDuration,
                    label = "Max",
                    onValueChange = { onDurationChanged(minDuration, it) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun DateFilterSection(
    dateRange: DateRange?,
    onDateRangeChanged: (DateRange?) -> Unit
) {
    BubbleCard(
        elevation = 2,
        cornerRadius = 20
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Date Range",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            // Quick presets
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val now = System.currentTimeMillis()
                val presets = listOf(
                    "Today" to DateRange(
                        now - 24 * 60 * 60 * 1000,
                        now
                    ),
                    "This Week" to DateRange(
                        now - 7 * 24 * 60 * 60 * 1000,
                        now
                    ),
                    "This Month" to DateRange(
                        now - 30L * 24 * 60 * 60 * 1000,
                        now
                    ),
                    "This Year" to DateRange(
                        now - 365L * 24 * 60 * 60 * 1000,
                        now
                    )
                )
                
                items(presets) { (label, range) ->
                    FilterChip(
                        selected = dateRange?.start == range.start && dateRange?.end == range.end,
                        onClick = { onDateRangeChanged(range) },
                        label = { Text(label) }
                    )
                }
            }
            
            // Current selection
            if (dateRange != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val formatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                    Text(
                        text = "${formatter.format(dateRange.startDate)} - ${formatter.format(dateRange.endDate)}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    
                    IconButton(onClick = { onDateRangeChanged(null) }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear date range")
                    }
                }
            }
        }
    }
}

@Composable
private fun ResolutionFilterSection(
    selectedResolutions: Set<String>,
    onResolutionsChanged: (Set<String>) -> Unit
) {
    BubbleCard(
        elevation = 2,
        cornerRadius = 20
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Resolution",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            val resolutions = listOf("SD", "480p", "720p", "1080p", "4K", "8K")
            
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(resolutions) { resolution ->
                    FilterChip(
                        selected = resolution in selectedResolutions,
                        onClick = {
                            val newSet = selectedResolutions.toMutableSet()
                            if (resolution in selectedResolutions) {
                                newSet.remove(resolution)
                            } else {
                                newSet.add(resolution)
                            }
                            onResolutionsChanged(newSet)
                        },
                        label = { Text(resolution) }
                    )
                }
            }
        }
    }
}

@Composable
private fun FormatFilterSection(
    selectedFormats: Set<String>,
    onFormatsChanged: (Set<String>) -> Unit
) {
    BubbleCard(
        elevation = 2,
        cornerRadius = 20
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "File Format",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            val formats = listOf("mp4", "mkv", "avi", "mov", "webm", "flv", "wmv", "m4v")
            
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(formats) { format ->
                    FilterChip(
                        selected = format in selectedFormats,
                        onClick = {
                            val newSet = selectedFormats.toMutableSet()
                            if (format in selectedFormats) {
                                newSet.remove(format)
                            } else {
                                newSet.add(format)
                            }
                            onFormatsChanged(newSet)
                        },
                        label = { Text(format.uppercase()) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SourceFilterSection(
    selectedSources: Set<String>,
    onSourcesChanged: (Set<String>) -> Unit
) {
    BubbleCard(
        elevation = 2,
        cornerRadius = 20
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Source",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            val sources = listOf(
                "local" to "Local Storage",
                "downloads" to "Downloads",
                "camera" to "Camera",
                "whatsapp" to "WhatsApp",
                "telegram" to "Telegram",
                "streaming" to "Streaming"
            )
            
            sources.forEach { (key, label) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            val newSet = selectedSources.toMutableSet()
                            if (key in selectedSources) {
                                newSet.remove(key)
                            } else {
                                newSet.add(key)
                            }
                            onSourcesChanged(newSet)
                        }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = key in selectedSources,
                        onCheckedChange = null
                    )
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }
    }
}

@Composable
private fun FileSizeFilterSection(
    minSize: Long?,
    maxSize: Long?,
    onSizeChanged: (Long?, Long?) -> Unit
) {
    BubbleCard(
        elevation = 2,
        cornerRadius = 20
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "File Size",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            // Quick presets
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val presets = listOf(
                    "< 100 MB" to (null to 100L * 1024 * 1024),
                    "100-500 MB" to (100L * 1024 * 1024 to 500L * 1024 * 1024),
                    "500 MB - 1 GB" to (500L * 1024 * 1024 to 1024L * 1024 * 1024),
                    "> 1 GB" to (1024L * 1024 * 1024 to null)
                )
                
                items(presets) { (label, range) ->
                    val (min, max) = range
                    FilterChip(
                        selected = minSize == min && maxSize == max,
                        onClick = { onSizeChanged(min, max) },
                        label = { Text(label) }
                    )
                }
            }
        }
    }
}

@Composable
private fun OtherFiltersSection(
    favorites: Boolean,
    hasSubtitles: Boolean?,
    onFavoritesChanged: (Boolean) -> Unit,
    onSubtitlesChanged: (Boolean?) -> Unit
) {
    BubbleCard(
        elevation = 2,
        cornerRadius = 20
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Other Filters",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            // Favorites
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onFavoritesChanged(!favorites) }
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = favorites,
                    onCheckedChange = null
                )
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Icon(
                    Icons.Default.Favorite,
                    contentDescription = null,
                    tint = if (favorites) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                Text(
                    text = "Favorites only",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
            
            // Subtitles
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Subtitles",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = hasSubtitles == null,
                        onClick = { onSubtitlesChanged(null) },
                        label = { Text("Any") }
                    )
                    
                    FilterChip(
                        selected = hasSubtitles == true,
                        onClick = { onSubtitlesChanged(true) },
                        label = { Text("With subtitles") }
                    )
                    
                    FilterChip(
                        selected = hasSubtitles == false,
                        onClick = { onSubtitlesChanged(false) },
                        label = { Text("No subtitles") }
                    )
                }
            }
        }
    }
}

@Composable
private fun DurationInput(
    value: Long?,
    label: String,
    onValueChange: (Long?) -> Unit,
    modifier: Modifier = Modifier
) {
    var textValue by remember(value) { 
        mutableStateOf(value?.let { formatDurationInput(it) } ?: "") 
    }
    
    OutlinedTextField(
        value = textValue,
        onValueChange = { newValue ->
            textValue = newValue
            val duration = parseDurationInput(newValue)
            onValueChange(duration)
        },
        label = { Text(label) },
        placeholder = { Text("00:00") },
        singleLine = true,
        modifier = modifier
    )
}

@Composable
private fun SortOrderOption(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    BubbleCard(
        onClick = onClick,
        elevation = if (selected) 4 else 1,
        cornerRadius = 16,
        containerColor = if (selected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        },
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
            
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = if (selected) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal
            )
        }
    }
}

// Helper functions
private fun formatDurationInput(millis: Long): String {
    val totalSeconds = millis / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}

private fun parseDurationInput(input: String): Long? {
    if (input.isBlank()) return null
    
    val parts = input.split(":")
    return try {
        when (parts.size) {
            1 -> parts[0].toLong() * 60 * 1000 // Just minutes
            2 -> (parts[0].toLong() * 60 + parts[1].toLong()) * 1000 // Minutes:seconds
            else -> null
        }
    } catch (e: NumberFormatException) {
        null
    }
}