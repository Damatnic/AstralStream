package com.astralstream.nextplayer.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.astralstream.nextplayer.R
import com.astralstream.nextplayer.ui.components.*
import com.astralstream.nextplayer.ui.theme.*
import com.astralstream.nextplayer.viewmodels.AnalyticsDashboardViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsDashboardScreen(
    onNavigateBack: () -> Unit,
    onNavigateToVideoDetails: (String) -> Unit,
    viewModel: AnalyticsDashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val dateRange by viewModel.dateRange.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    
    var showDateRangePicker by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    
    LaunchedEffect(dateRange) {
        viewModel.refreshAnalytics()
    }
    
    Scaffold(
        topBar = {
            AnalyticsDashboardTopBar(
                dateRange = dateRange,
                onNavigateBack = onNavigateBack,
                onDateRangeClick = { showDateRangePicker = true },
                onExportClick = { showExportDialog = true }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (uiState) {
                is AnalyticsDashboardViewModel.UiState.Loading -> {
                    LoadingState()
                }
                is AnalyticsDashboardViewModel.UiState.Success -> {
                    AnalyticsDashboardContent(
                        analytics = uiState.analytics,
                        onVideoClick = onNavigateToVideoDetails,
                        onRefresh = { viewModel.refreshAnalytics() }
                    )
                }
                is AnalyticsDashboardViewModel.UiState.Error -> {
                    ErrorState(
                        message = uiState.message,
                        onRetry = { viewModel.refreshAnalytics() }
                    )
                }
            }
            
            // Date range picker
            if (showDateRangePicker) {
                DateRangePickerModal(
                    currentRange = dateRange,
                    onRangeSelected = { range ->
                        viewModel.updateDateRange(range)
                        showDateRangePicker = false
                    },
                    onDismiss = { showDateRangePicker = false }
                )
            }
            
            // Export dialog
            if (showExportDialog) {
                ExportAnalyticsDialog(
                    onExportFormat = { format ->
                        scope.launch {
                            val result = viewModel.exportAnalytics(format)
                            if (result) {
                                snackbarHostState.showSnackbar("Analytics exported successfully")
                            } else {
                                snackbarHostState.showSnackbar("Export failed")
                            }
                        }
                        showExportDialog = false
                    },
                    onDismiss = { showExportDialog = false }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AnalyticsDashboardTopBar(
    dateRange: AnalyticsDashboardViewModel.DateRange,
    onNavigateBack: () -> Unit,
    onDateRangeClick: () -> Unit,
    onExportClick: () -> Unit
) {
    TopAppBar(
        title = { 
            Column {
                Text(
                    text = "Analytics Dashboard",
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    text = formatDateRange(dateRange),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        },
        navigationIcon = {
            IconButton(onClick = onNavigateBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
            }
        },
        actions = {
            IconButton(onClick = onDateRangeClick) {
                Icon(Icons.Default.CalendarMonth, contentDescription = "Date range")
            }
            IconButton(onClick = onExportClick) {
                Icon(Icons.Default.Download, contentDescription = "Export")
            }
        }
    )
}

@Composable
private fun AnalyticsDashboardContent(
    analytics: AnalyticsDashboardViewModel.Analytics,
    onVideoClick: (String) -> Unit,
    onRefresh: () -> Unit
) {
    val scrollState = rememberScrollState()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Overview cards
        OverviewSection(analytics.overview)
        
        // Watch time chart
        WatchTimeChartCard(analytics.watchTimeData)
        
        // Genre distribution
        GenreDistributionCard(analytics.genreDistribution)
        
        // Performance metrics
        PerformanceMetricsCard(analytics.performanceMetrics)
        
        // Top videos
        TopVideosSection(
            videos = analytics.topVideos,
            onVideoClick = onVideoClick
        )
        
        // Playback quality stats
        PlaybackQualityCard(analytics.playbackQuality)
        
        // Device breakdown
        DeviceBreakdownCard(analytics.deviceBreakdown)
    }
}

@Composable
private fun OverviewSection(overview: AnalyticsDashboardViewModel.OverviewStats) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OverviewCard(
            modifier = Modifier.weight(1f),
            title = "Total Watch Time",
            value = formatDuration(overview.totalWatchTime),
            icon = Icons.Default.PlayCircle,
            color = MaterialTheme.colorScheme.primary,
            change = overview.watchTimeChange
        )
        OverviewCard(
            modifier = Modifier.weight(1f),
            title = "Videos Watched",
            value = overview.videosWatched.toString(),
            icon = Icons.Default.VideoLibrary,
            color = MaterialTheme.colorScheme.secondary,
            change = overview.videosChange
        )
    }
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OverviewCard(
            modifier = Modifier.weight(1f),
            title = "Avg. Session",
            value = formatDuration(overview.avgSessionDuration),
            icon = Icons.Default.Timer,
            color = MaterialTheme.colorScheme.tertiary,
            change = overview.sessionChange
        )
        OverviewCard(
            modifier = Modifier.weight(1f),
            title = "Completion Rate",
            value = "${(overview.completionRate * 100).toInt()}%",
            icon = Icons.Default.CheckCircle,
            color = MaterialTheme.colorScheme.success,
            change = overview.completionChange
        )
    }
}

@Composable
private fun OverviewCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    icon: ImageVector,
    color: Color,
    change: Float? = null
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(color.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            
            change?.let {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (it >= 0) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                        contentDescription = null,
                        tint = if (it >= 0) MaterialTheme.colorScheme.success else MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "${if (it >= 0) "+" else ""}${(it * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (it >= 0) MaterialTheme.colorScheme.success else MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
private fun WatchTimeChartCard(watchTimeData: List<AnalyticsDashboardViewModel.WatchTimeEntry>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                text = "Watch Time Trend",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            if (watchTimeData.isNotEmpty()) {
                WatchTimeChart(
                    data = watchTimeData,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                )
            } else {
                EmptyChartState("No watch time data available")
            }
        }
    }
}

@Composable
private fun WatchTimeChart(
    data: List<AnalyticsDashboardViewModel.WatchTimeEntry>,
    modifier: Modifier = Modifier
) {
    val maxValue = data.maxOfOrNull { it.minutes } ?: 0
    
    Canvas(modifier = modifier) {
        val barWidth = size.width / (data.size * 1.5f)
        val spacing = barWidth * 0.5f
        
        data.forEachIndexed { index, entry ->
            val barHeight = (entry.minutes.toFloat() / maxValue) * size.height * 0.8f
            val x = index * (barWidth + spacing) + spacing
            val y = size.height - barHeight
            
            // Draw bar
            drawRoundRect(
                color = Color(0xFF6750A4),
                topLeft = androidx.compose.ui.geometry.Offset(x, y),
                size = androidx.compose.ui.geometry.Size(barWidth, barHeight),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx())
            )
            
            // Draw label
            drawContext.canvas.nativeCanvas.apply {
                val text = SimpleDateFormat("MMM d", Locale.getDefault()).format(entry.date)
                val paint = android.graphics.Paint().apply {
                    textAlign = android.graphics.Paint.Align.CENTER
                    textSize = 10.sp.toPx()
                    color = android.graphics.Color.GRAY
                }
                drawText(text, x + barWidth / 2, size.height - 4.dp.toPx(), paint)
            }
        }
    }
}

@Composable
private fun GenreDistributionCard(genreData: List<AnalyticsDashboardViewModel.GenreStats>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                text = "Genre Distribution",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            if (genreData.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    PieChart(
                        data = genreData,
                        modifier = Modifier.size(180.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Legend
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(genreData.take(5)) { genre ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(Color(genre.color))
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = genre.name,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
            } else {
                EmptyChartState("No genre data available")
            }
        }
    }
}

@Composable
private fun PieChart(
    data: List<AnalyticsDashboardViewModel.GenreStats>,
    modifier: Modifier = Modifier
) {
    val total = data.sumOf { it.watchTime }
    var startAngle = -90f
    
    Canvas(modifier = modifier) {
        val radius = size.minDimension / 2
        val center = androidx.compose.ui.geometry.Offset(size.width / 2, size.height / 2)
        
        data.forEach { genre ->
            val sweepAngle = (genre.watchTime.toFloat() / total) * 360f
            
            drawArc(
                color = Color(genre.color),
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = true,
                topLeft = androidx.compose.ui.geometry.Offset(
                    center.x - radius,
                    center.y - radius
                ),
                size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2)
            )
            
            startAngle += sweepAngle
        }
        
        // Center hole for donut chart
        drawCircle(
            color = Color.White,
            radius = radius * 0.6f,
            center = center
        )
    }
}

@Composable
private fun TopVideosSection(
    videos: List<AnalyticsDashboardViewModel.VideoStats>,
    onVideoClick: (String) -> Unit
) {
    Column {
        Text(
            text = "Top Videos",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        
        videos.forEach { video ->
            VideoStatsCard(
                video = video,
                onClick = { onVideoClick(video.id) }
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VideoStatsCard(
    video: AnalyticsDashboardViewModel.VideoStats,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Thumbnail placeholder
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = video.title,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${video.views} views • ${formatDuration(video.totalWatchTime)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                
                // Progress bar
                LinearProgressIndicator(
                    progress = video.avgCompletion,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }
            
            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = "${(video.avgCompletion * 100).toInt()}%",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "completion",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
        }
    }
}

// Additional UI components and helper functions...

@Composable
private fun EmptyChartState(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
        )
    }
}

private fun formatDateRange(range: AnalyticsDashboardViewModel.DateRange): String {
    val formatter = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
    return "${formatter.format(range.start)} - ${formatter.format(range.end)}"
}

private fun formatDuration(seconds: Long): String {
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    
    return when {
        hours > 0 -> "${hours}h ${minutes}m"
        else -> "${minutes}m"
    }
}