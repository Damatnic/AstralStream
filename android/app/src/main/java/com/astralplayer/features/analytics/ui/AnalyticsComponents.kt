package com.astralplayer.features.analytics.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.astralplayer.features.analytics.dao.*
import com.astralplayer.features.analytics.data.*
import kotlin.math.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoStatsCard(
    video: VideoStatisticsEntity,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.width(200.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = video.videoTitle,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Badge(
                    containerColor = MaterialTheme.colorScheme.tertiary
                ) {
                    Text(
                        text = video.watchCount.toString(),
                        fontSize = 10.sp
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            LinearProgressIndicator(
                progress = video.averageWatchPercentage / 100f,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${video.averageWatchPercentage.roundToInt()}% watched",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (video.completionCount > 0) {
                    Text(
                        text = "✓ ${video.completionCount}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
fun WatchTimeChart(
    dailyStats: List<DailyStatisticsEntity>,
    modifier: Modifier = Modifier
) {
    val animationProgress = remember { Animatable(0f) }
    LaunchedEffect(dailyStats) {
        animationProgress.animateTo(1f, tween(1000))
    }
    
    Canvas(modifier = modifier) {
        if (dailyStats.isEmpty()) return@Canvas
        
        val maxTime = dailyStats.maxOfOrNull { it.totalWatchTime } ?: 1L
        val barWidth = size.width / dailyStats.size
        val padding = barWidth * 0.1f
        
        dailyStats.forEachIndexed { index, stat ->
            val barHeight = (stat.totalWatchTime.toFloat() / maxTime) * size.height * 0.8f * animationProgress.value
            val x = index * barWidth + padding
            
            // Draw bar
            drawRoundRect(
                color = Color(0xFF6200EE),
                topLeft = Offset(x, size.height - barHeight),
                size = Size(barWidth - 2 * padding, barHeight),
                cornerRadius = CornerRadius(4.dp.toPx())
            )
            
            // Draw date label
            drawContext.canvas.nativeCanvas.apply {
                val text = stat.date.substring(5) // MM-DD
                val paint = android.graphics.Paint().apply {
                    textSize = 10.sp.toPx()
                    color = android.graphics.Color.GRAY
                    textAlign = android.graphics.Paint.Align.CENTER
                }
                drawText(
                    text,
                    x + (barWidth - 2 * padding) / 2,
                    size.height - 4.dp.toPx(),
                    paint
                )
            }
        }
    }
}

@Composable
fun HourlyHeatmap(
    hourlyData: List<HourlyDistribution>,
    modifier: Modifier = Modifier
) {
    val hours = (0..23).map { hour ->
        val data = hourlyData.find { it.hour == hour.toString().padStart(2, '0') }
        hour to (data?.count ?: 0)
    }
    val maxCount = hours.maxOfOrNull { it.second } ?: 1
    
    Box(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            hours.forEach { (hour, count) ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(
                                MaterialTheme.colorScheme.primary.copy(
                                    alpha = (count.toFloat() / maxCount).coerceIn(0.1f, 1f)
                                )
                            )
                    )
                    if (hour % 4 == 0) {
                        Text(
                            text = hour.toString(),
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 8.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun WeeklyPatternChart(
    weeklyData: List<WeeklyPattern>,
    modifier: Modifier = Modifier
) {
    val dayNames = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
    val maxTime = weeklyData.maxOfOrNull { it.totalTime } ?: 1L
    
    Column(modifier = modifier) {
        weeklyData.sortedBy { it.dayOfWeek.toIntOrNull() ?: 0 }.forEach { pattern ->
            val dayIndex = pattern.dayOfWeek.toIntOrNull() ?: 0
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = dayNames.getOrNull(dayIndex) ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.width(40.dp)
                )
                
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(24.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(fraction = (pattern.totalTime.toFloat() / maxTime))
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.primary)
                    )
                }
                
                Text(
                    text = formatDuration(pattern.totalTime),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.width(60.dp)
                )
            }
        }
    }
}

@Composable
fun GenreChart(
    genres: List<GenreStats>,
    modifier: Modifier = Modifier
) {
    val total = genres.sumOf { it.totalTime }.toFloat()
    val animationProgress = remember { Animatable(0f) }
    
    LaunchedEffect(genres) {
        animationProgress.animateTo(1f, tween(1000))
    }
    
    Canvas(modifier = modifier) {
        var startAngle = -90f
        val radius = size.minDimension / 2 * 0.8f
        val center = Offset(size.width / 2, size.height / 2)
        
        genres.forEach { genre ->
            val sweep = (genre.totalTime / total * 360f) * animationProgress.value
            val color = generateColorForGenre(genre.genre)
            
            drawArc(
                color = color,
                startAngle = startAngle,
                sweepAngle = sweep,
                useCenter = true,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 2, radius * 2)
            )
            
            startAngle += sweep
        }
    }
}

@Composable
fun ContentTypeRow(
    type: String,
    watchTime: Long,
    count: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(generateColorForGenre(type))
            )
            Text(
                text = type,
                style = MaterialTheme.typography.bodyMedium
            )
        }
        
        Column(
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text = formatDuration(watchTime),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "$count videos",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecentVideoCard(
    video: VideoStatisticsEntity,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = video.videoTitle,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Last watched: ${formatRelativeTime(video.lastWatched)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            CircularProgressIndicator(
                progress = video.averageWatchPercentage / 100f,
                modifier = Modifier.size(40.dp),
                strokeWidth = 3.dp
            )
        }
    }
}

@Composable
fun FeatureUsageRow(
    feature: FeatureUsageEntity
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = formatFeatureName(feature.featureName),
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            text = "${feature.usageCount} uses",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
fun BehaviorMetric(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun PerformanceMetricRow(
    label: String,
    value: Float,
    unit: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "${value.roundToInt()} $unit",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun PerformanceChart(
    metrics: List<PerformanceMetricEntity>,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        if (metrics.size < 2) return@Canvas
        
        val maxValue = metrics.maxOfOrNull { it.value } ?: 1f
        val minValue = metrics.minOfOrNull { it.value } ?: 0f
        val range = maxValue - minValue
        
        val path = Path()
        val strokePath = Path()
        
        metrics.forEachIndexed { index, metric ->
            val x = (index.toFloat() / (metrics.size - 1)) * size.width
            val y = size.height - ((metric.value - minValue) / range * size.height)
            
            if (index == 0) {
                path.moveTo(x, y)
                strokePath.moveTo(x, y)
            } else {
                path.lineTo(x, y)
                strokePath.lineTo(x, y)
            }
        }
        
        // Fill area under curve
        path.lineTo(size.width, size.height)
        path.lineTo(0f, size.height)
        path.close()
        
        drawPath(
            path = path,
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF6200EE).copy(alpha = 0.3f),
                    Color(0xFF6200EE).copy(alpha = 0.0f)
                )
            )
        )
        
        // Draw line
        drawPath(
            path = strokePath,
            color = Color(0xFF6200EE),
            style = Stroke(width = 2.dp.toPx())
        )
        
        // Draw points
        metrics.forEachIndexed { index, _ ->
            val x = (index.toFloat() / (metrics.size - 1)) * size.width
            val y = size.height - ((metrics[index].value - minValue) / range * size.height)
            
            drawCircle(
                color = Color(0xFF6200EE),
                radius = 3.dp.toPx(),
                center = Offset(x, y)
            )
        }
    }
}

// Helper functions
private fun formatDuration(millis: Long): String {
    val hours = millis / 3600000
    val minutes = (millis % 3600000) / 60000
    return when {
        hours > 24 -> "${hours / 24}d"
        hours > 0 -> "${hours}h ${minutes}m"
        else -> "${minutes}m"
    }
}

private fun formatRelativeTime(timestamp: Long): String {
    val diff = System.currentTimeMillis() - timestamp
    return when {
        diff < 3600000 -> "${diff / 60000}m ago"
        diff < 86400000 -> "${diff / 3600000}h ago"
        else -> "${diff / 86400000}d ago"
    }
}

private fun formatFeatureName(name: String): String {
    return name.replace('_', ' ').lowercase()
        .split(' ').joinToString(" ") { it.capitalize() }
}

private fun generateColorForGenre(genre: String): Color {
    val hash = genre.hashCode()
    val hue = (hash and 0xFF) / 255f * 360f
    return Color.hsv(hue, 0.7f, 0.8f)
}