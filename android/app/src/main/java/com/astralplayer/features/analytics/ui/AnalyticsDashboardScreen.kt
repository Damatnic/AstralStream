package com.astralplayer.features.analytics.ui

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.astralplayer.features.analytics.viewmodel.AnalyticsDashboardViewModel
import com.astralplayer.features.analytics.viewmodel.AnalyticsTab
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsDashboardScreen(
    onNavigateBack: () -> Unit,
    onNavigateToVideoDetails: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AnalyticsDashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val pagerState = rememberPagerState(pageCount = { AnalyticsTab.values().size })
    val coroutineScope = rememberCoroutineScope()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Analytics Dashboard") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::refreshData) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                    
                    Box {
                        var expanded by remember { mutableStateOf(false) }
                        IconButton(onClick = { expanded = true }) {
                            Icon(Icons.Default.DateRange, contentDescription = "Time range")
                        }
                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            listOf(7, 30, 90).forEach { days ->
                                DropdownMenuItem(
                                    text = { Text("Last $days days") },
                                    onClick = {
                                        viewModel.setTimeRange(days)
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            )
        },
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Tab Row
            TabRow(
                selectedTabIndex = pagerState.currentPage,
                modifier = Modifier.fillMaxWidth()
            ) {
                AnalyticsTab.values().forEachIndexed { index, tab ->
                    Tab(
                        selected = pagerState.currentPage == index,
                        onClick = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(index)
                            }
                        },
                        text = { Text(tab.title) },
                        icon = {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = tab.title
                            )
                        }
                    )
                }
            }
            
            // Content
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    when (AnalyticsTab.values()[page]) {
                        AnalyticsTab.OVERVIEW -> OverviewTab(
                            uiState = uiState,
                            onVideoClick = onNavigateToVideoDetails
                        )
                        AnalyticsTab.WATCH_TIME -> WatchTimeTab(uiState = uiState)
                        AnalyticsTab.CONTENT -> ContentTab(
                            uiState = uiState,
                            onVideoClick = onNavigateToVideoDetails
                        )
                        AnalyticsTab.FEATURES -> FeaturesTab(uiState = uiState)
                        AnalyticsTab.PERFORMANCE -> PerformanceTab(uiState = uiState)
                    }
                }
            }
        }
    }
}

@Composable
private fun OverviewTab(
    uiState: com.astralplayer.features.analytics.viewmodel.AnalyticsDashboardUiState,
    onVideoClick: (String) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Key metrics cards
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MetricCard(
                    title = "Total Watch Time",
                    value = formatDuration(uiState.watchTimeStats?.totalWatchTime ?: 0L),
                    icon = Icons.Default.PlayCircle,
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    title = "Sessions",
                    value = uiState.watchTimeStats?.totalSessions?.toString() ?: "0",
                    icon = Icons.Default.Tv,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MetricCard(
                    title = "Avg Session",
                    value = formatDuration(uiState.watchTimeStats?.averageSessionDuration ?: 0L),
                    icon = Icons.Default.Timer,
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    title = "Completed",
                    value = uiState.watchTimeStats?.completedVideos?.toString() ?: "0",
                    icon = Icons.Default.CheckCircle,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        
        // Recent sessions
        if (uiState.recentSessions.isNotEmpty()) {
            item {
                SectionHeader(
                    title = "Recent Sessions",
                    icon = Icons.Default.History
                )
            }
            
            items(uiState.recentSessions.take(5)) { session ->
                SessionCard(
                    session = session,
                    onClick = { onVideoClick(session.videoId) }
                )
            }
        }
        
        // Most watched videos
        if (uiState.mostWatchedVideos.isNotEmpty()) {
            item {
                SectionHeader(
                    title = "Most Watched",
                    icon = Icons.Default.Whatshot
                )
            }
            
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(uiState.mostWatchedVideos) { video ->
                        VideoStatsCard(
                            video = video,
                            onClick = { onVideoClick(video.videoId) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WatchTimeTab(
    uiState: com.astralplayer.features.analytics.viewmodel.AnalyticsDashboardUiState
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Daily watch time chart
        if (uiState.dailyStats.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Daily Watch Time",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        WatchTimeChart(
                            dailyStats = uiState.dailyStats,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                        )
                    }
                }
            }
        }
        
        // Hourly distribution
        item {
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Peak Watching Hours",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    HourlyHeatmap(
                        hourlyData = uiState.hourlyDistribution,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
        
        // Weekly pattern
        item {
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Weekly Pattern",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    WeeklyPatternChart(
                        weeklyData = uiState.weeklyPattern,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
        
        // Playback behavior
        uiState.playbackBehavior?.let { behavior ->
            item {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Playback Behavior",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        BehaviorMetric(
                            label = "Average Playback Speed",
                            value = "${behavior.averagePlaybackSpeed}x"
                        )
                        BehaviorMetric(
                            label = "Seeks per Session",
                            value = String.format("%.1f", behavior.averageSeeksPerSession)
                        )
                        BehaviorMetric(
                            label = "Pauses per Session",
                            value = String.format("%.1f", behavior.averagePausesPerSession)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ContentTab(
    uiState: com.astralplayer.features.analytics.viewmodel.AnalyticsDashboardUiState,
    onVideoClick: (String) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Content type distribution
        if (uiState.contentPreferences?.preferredContentTypes?.isNotEmpty() == true) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Content Types",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        uiState.contentPreferences.preferredContentTypes.forEach { type ->
                            ContentTypeRow(
                                type = type.contentType,
                                watchTime = type.totalTime,
                                count = type.count
                            )
                        }
                    }
                }
            }
        }
        
        // Genre preferences
        if (uiState.contentPreferences?.preferredGenres?.isNotEmpty() == true) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Favorite Genres",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        GenreChart(
                            genres = uiState.contentPreferences.preferredGenres,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                        )
                    }
                }
            }
        }
        
        // Recently watched
        if (uiState.recentlyWatchedVideos.isNotEmpty()) {
            item {
                SectionHeader(
                    title = "Recently Watched",
                    icon = Icons.Default.Schedule
                )
            }
            
            items(uiState.recentlyWatchedVideos) { video ->
                RecentVideoCard(
                    video = video,
                    onClick = { onVideoClick(video.videoId) }
                )
            }
        }
    }
}

@Composable
private fun FeaturesTab(
    uiState: com.astralplayer.features.analytics.viewmodel.AnalyticsDashboardUiState
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (uiState.featureUsage.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Most Used Features",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            
            items(uiState.featureUsage.groupBy { it.featureCategory }.toList()) { (category, features) ->
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = formatCategory(category),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        features.sortedByDescending { it.usageCount }.forEach { feature ->
                            FeatureUsageRow(feature = feature)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PerformanceTab(
    uiState: com.astralplayer.features.analytics.viewmodel.AnalyticsDashboardUiState
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        uiState.performanceMetrics.forEach { (type, metrics) ->
            if (metrics.isNotEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = formatMetricType(type),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            val average = metrics.map { it.value }.average()
                            val min = metrics.minByOrNull { it.value }?.value ?: 0f
                            val max = metrics.maxByOrNull { it.value }?.value ?: 0f
                            val unit = metrics.firstOrNull()?.unit ?: ""
                            
                            PerformanceMetricRow("Average", average, unit)
                            PerformanceMetricRow("Minimum", min, unit)
                            PerformanceMetricRow("Maximum", max, unit)
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            PerformanceChart(
                                metrics = metrics.takeLast(20),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(150.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// Component functions
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MetricCard(
    title: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    icon: ImageVector
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SessionCard(
    session: com.astralplayer.features.analytics.data.ViewingSessionEntity,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = session.videoTitle,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = formatTimestamp(session.startTime),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = formatDuration(session.totalWatchTime),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${session.completionPercentage.roundToInt()}%",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (session.completionPercentage >= 90f) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
        }
    }
}

// Helper functions
private fun formatDuration(millis: Long): String {
    val hours = millis / 3600000
    val minutes = (millis % 3600000) / 60000
    return if (hours > 0) {
        "${hours}h ${minutes}m"
    } else {
        "${minutes}m"
    }
}

private fun formatTimestamp(timestamp: Long): String {
    return SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date(timestamp))
}

private fun formatCategory(category: com.astralplayer.features.analytics.data.FeatureCategory): String {
    return category.name.replace('_', ' ').lowercase()
        .split(' ').joinToString(" ") { it.capitalize() }
}

private fun formatMetricType(type: com.astralplayer.features.analytics.data.PerformanceMetricType): String {
    return type.name.replace('_', ' ').lowercase()
        .split(' ').joinToString(" ") { it.capitalize() }
}