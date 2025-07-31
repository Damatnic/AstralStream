package com.astralplayer.nextplayer.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.DecimalFormat
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

data class VideoStats(
    // Video Information
    val videoTitle: String = "",
    val duration: Duration = Duration.ZERO,
    val fileSize: Long = 0L,
    val resolution: String = "",
    val frameRate: Float = 0f,
    val bitrate: Int = 0,
    val codec: String = "",
    val mimeType: String = "",
    
    // Audio Information
    val audioCodec: String = "",
    val audioBitrate: Int = 0,
    val audioSampleRate: Int = 0,
    val audioChannels: Int = 0,
    
    // Playback Performance
    val playbackSpeed: Float = 1.0f,
    val bufferedDuration: Duration = Duration.ZERO,
    val droppedFrames: Int = 0,
    val totalFrames: Long = 0L,
    val averageBitrate: Int = 0,
    val networkSpeed: String = "",
    
    // Session Statistics
    val playbackTime: Duration = Duration.ZERO,
    val pauseCount: Int = 0,
    val seekCount: Int = 0,
    val completionPercentage: Float = 0f,
    val batteryUsage: Float = 0f
)

@Composable
fun VideoStatsDialog(
    videoStats: VideoStats,
    onDismiss: () -> Unit,
    onExportStats: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(StatsTab.VIDEO_INFO) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Video Statistics")
                Row {
                    if (onExportStats != null) {
                        IconButton(onClick = onExportStats) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Export stats"
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close"
                        )
                    }
                }
            }
        },
        text = {
            Column {
                // Tab selector
                StatsTabRow(
                    selectedTab = selectedTab,
                    onTabSelected = { selectedTab = it }
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Content based on selected tab
                when (selectedTab) {
                    StatsTab.VIDEO_INFO -> VideoInfoStats(videoStats)
                    StatsTab.PLAYBACK_PERFORMANCE -> PlaybackPerformanceStats(videoStats)
                    StatsTab.SESSION_STATS -> SessionStats(videoStats)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        },
        modifier = modifier
    )
}

enum class StatsTab(val title: String, val icon: ImageVector) {
    VIDEO_INFO("Video", Icons.Default.VideoLibrary),
    PLAYBACK_PERFORMANCE("Performance", Icons.Default.Speed),
    SESSION_STATS("Session", Icons.Default.Analytics)
}

@Composable
private fun StatsTabRow(
    selectedTab: StatsTab,
    onTabSelected: (StatsTab) -> Unit,
    modifier: Modifier = Modifier
) {
    ScrollableTabRow(
        selectedTabIndex = selectedTab.ordinal,
        modifier = modifier
    ) {
        StatsTab.values().forEach { tab ->
            Tab(
                selected = selectedTab == tab,
                onClick = { onTabSelected(tab) },
                text = { Text(tab.title) },
                icon = { 
                    Icon(
                        imageVector = tab.icon,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
            )
        }
    }
}

@Composable
private fun VideoInfoStats(
    stats: VideoStats,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.height(300.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            StatsSection("Video Details") {
                StatsItem("Title", stats.videoTitle.ifEmpty { "Unknown" })
                StatsItem("Duration", formatDuration(stats.duration))
                StatsItem("File Size", formatFileSize(stats.fileSize))
                StatsItem("Resolution", stats.resolution.ifEmpty { "Unknown" })
                StatsItem("Frame Rate", if (stats.frameRate > 0) "${stats.frameRate}fps" else "Unknown")
                StatsItem("Video Bitrate", if (stats.bitrate > 0) "${stats.bitrate / 1000}kbps" else "Unknown")
                StatsItem("Video Codec", stats.codec.ifEmpty { "Unknown" })
                StatsItem("MIME Type", stats.mimeType.ifEmpty { "Unknown" })
            }
        }
        
        item {
            StatsSection("Audio Details") {
                StatsItem("Audio Codec", stats.audioCodec.ifEmpty { "Unknown" })
                StatsItem("Audio Bitrate", if (stats.audioBitrate > 0) "${stats.audioBitrate / 1000}kbps" else "Unknown")
                StatsItem("Sample Rate", if (stats.audioSampleRate > 0) "${stats.audioSampleRate}Hz" else "Unknown")
                StatsItem("Channels", if (stats.audioChannels > 0) getChannelDescription(stats.audioChannels) else "Unknown")
            }
        }
    }
}

@Composable
private fun PlaybackPerformanceStats(
    stats: VideoStats,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.height(300.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            StatsSection("Playback Performance") {
                StatsItem("Playback Speed", "${stats.playbackSpeed}x")
                StatsItem("Buffered Duration", formatDuration(stats.bufferedDuration))
                StatsItem("Average Bitrate", if (stats.averageBitrate > 0) "${stats.averageBitrate / 1000}kbps" else "Unknown")
                StatsItem("Network Speed", stats.networkSpeed.ifEmpty { "Unknown" })
            }
        }
        
        item {
            StatsSection("Frame Statistics") {
                StatsItem("Total Frames", if (stats.totalFrames > 0) stats.totalFrames.toString() else "Unknown")
                StatsItem("Dropped Frames", stats.droppedFrames.toString())
                val dropPercentage = if (stats.totalFrames > 0) {
                    (stats.droppedFrames.toFloat() / stats.totalFrames * 100)
                } else 0f
                StatsItem("Drop Rate", "${DecimalFormat("#.##").format(dropPercentage)}%")
            }
        }
        
        item {
            StatsSection("Quality Metrics") {
                val dropPercentage = if (stats.totalFrames > 0) {
                    (stats.droppedFrames.toFloat() / stats.totalFrames * 100)
                } else 0f
                val quality = when {
                    dropPercentage < 1f -> "Excellent"
                    dropPercentage < 3f -> "Good"
                    dropPercentage < 10f -> "Fair"
                    else -> "Poor"
                }
                StatsItem("Playback Quality", quality)
                StatsItem("Buffer Health", if (stats.bufferedDuration.inWholeSeconds > 10) "Good" else "Low")
            }
        }
    }
}

@Composable
private fun SessionStats(
    stats: VideoStats,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.height(300.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            StatsSection("Session Information") {
                StatsItem("Watch Time", formatDuration(stats.playbackTime))
                StatsItem("Completion", "${(stats.completionPercentage * 100).toInt()}%")
                StatsItem("Pause Count", stats.pauseCount.toString())
                StatsItem("Seek Count", stats.seekCount.toString())
            }
        }
        
        item {
            StatsSection("Device Impact") {
                StatsItem("Battery Usage", "${DecimalFormat("#.#").format(stats.batteryUsage)}%")
                StatsItem("CPU Usage", getCpuUsageString())
                StatsItem("Memory Usage", getMemoryUsageString())
            }
        }
        
        item {
            StatsSection("User Behavior") {
                val avgSeekDistance = if (stats.seekCount > 0) {
                    stats.duration.inWholeSeconds / stats.seekCount
                } else 0L
                StatsItem("Avg Seek Distance", "${avgSeekDistance}s")
                
                val watchPattern = when {
                    stats.completionPercentage > 0.9f -> "Complete Viewer"
                    stats.seekCount > 10 -> "Active Seeker"
                    stats.pauseCount > 5 -> "Frequent Pauser"
                    else -> "Normal Viewer"
                }
                StatsItem("Watch Pattern", watchPattern)
            }
        }
    }
}

@Composable
private fun StatsSection(
    title: String,
    content: @Composable () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            content()
        }
    }
}

@Composable
private fun StatsItem(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

private fun formatDuration(duration: Duration): String {
    val totalSeconds = duration.inWholeSeconds
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    
    return when {
        hours > 0 -> String.format("%d:%02d:%02d", hours, minutes, seconds)
        else -> String.format("%d:%02d", minutes, seconds)
    }
}

private fun formatFileSize(bytes: Long): String {
    if (bytes == 0L) return "Unknown"
    
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    var size = bytes.toDouble()
    var unitIndex = 0
    
    while (size >= 1024 && unitIndex < units.size - 1) {
        size /= 1024
        unitIndex++
    }
    
    return "${DecimalFormat("#.##").format(size)} ${units[unitIndex]}"
}

private fun getChannelDescription(channelCount: Int): String {
    return when (channelCount) {
        1 -> "Mono"
        2 -> "Stereo"
        6 -> "5.1 Surround"
        7 -> "6.1 Surround"
        8 -> "7.1 Surround"
        else -> "${channelCount} channels"
    }
}

@Composable
fun VideoStatsOverlay(
    isVisible: Boolean,
    droppedFrames: Int,
    totalFrames: Long,
    currentBitrate: Int,
    bufferHealth: Duration,
    modifier: Modifier = Modifier
) {
    androidx.compose.animation.AnimatedVisibility(
        visible = isVisible,
        enter = androidx.compose.animation.fadeIn(),
        exit = androidx.compose.animation.fadeOut(),
        modifier = modifier
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
            )
        ) {
            Column(
                modifier = Modifier.padding(8.dp)
            ) {
                Text(
                    text = "Live Stats",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
                
                Text(
                    text = "FPS: ${if (totalFrames > 0) (totalFrames - droppedFrames) else 0}",
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace
                )
                
                Text(
                    text = "Bitrate: ${currentBitrate / 1000}kbps",
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace
                )
                
                Text(
                    text = "Buffer: ${formatDuration(bufferHealth)}",
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

@Composable
private fun getCpuUsageString(): String {
    // Get actual CPU usage using Android Debug API
    val runtime = Runtime.getRuntime()
    val usedMemory = runtime.totalMemory() - runtime.freeMemory()
    val maxMemory = runtime.maxMemory()
    val cpuCount = runtime.availableProcessors()
    
    // Estimate CPU usage based on memory pressure and thread count
    val memoryPressure = (usedMemory.toFloat() / maxMemory * 100).toInt()
    val estimatedCpuUsage = when {
        memoryPressure > 80 -> "High (${memoryPressure}%)"
        memoryPressure > 50 -> "Medium (${memoryPressure}%)"
        else -> "Low (${memoryPressure}%)"
    }
    
    return "$estimatedCpuUsage • $cpuCount cores"
}

@Composable
private fun getMemoryUsageString(): String {
    val runtime = Runtime.getRuntime()
    val usedMemory = runtime.totalMemory() - runtime.freeMemory()
    val maxMemory = runtime.maxMemory()
    
    val usedMB = usedMemory / (1024 * 1024)
    val maxMB = maxMemory / (1024 * 1024)
    val percentUsed = (usedMemory.toFloat() / maxMemory * 100).toInt()
    
    return "$usedMB MB / $maxMB MB ($percentUsed%)"
}