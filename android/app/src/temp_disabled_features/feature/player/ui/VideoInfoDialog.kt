package com.astralplayer.nextplayer.feature.player.ui

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Format
import androidx.media3.common.VideoSize
import androidx.media3.exoplayer.ExoPlayer
import com.astralplayer.nextplayer.feature.player.viewmodel.PlayerViewModel
import java.io.File
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoInfoDialog(
    viewModel: PlayerViewModel,
    onDismiss: () -> Unit
) {
    val uiState by viewModel.playerState.collectAsState()
    val videoAnalysis by viewModel.videoAnalysis.collectAsState()
    val player = uiState.exoPlayer
    
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("General", "Technical", "Audio", "Analysis")
    
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier
            .widthIn(max = 600.dp)
            .heightIn(max = 600.dp),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = Color(0xFF00D4FF),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Video Information",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                // Tab Row
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color.Transparent,
                    contentColor = Color.White,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = Color(0xFF00D4FF)
                        )
                    }
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = {
                                Text(
                                    text = title,
                                    fontSize = 14.sp,
                                    fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Tab Content
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp)
                ) {
                    when (selectedTab) {
                        0 -> GeneralInfoTab(uiState, player)
                        1 -> TechnicalInfoTab(uiState, player)
                        2 -> AudioInfoTab(player)
                        3 -> AnalysisTab(videoAnalysis)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
fun GeneralInfoTab(
    uiState: com.astralplayer.nextplayer.feature.player.viewmodel.PlayerUiState,
    player: ExoPlayer?
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        InfoCard {
            InfoRow(
                icon = Icons.Default.Movie,
                label = "Title",
                value = uiState.videoTitle.ifEmpty { "Unknown" }
            )
            
            uiState.videoUri?.let { uri ->
                val path = when {
                    uri.scheme == "file" -> uri.path ?: ""
                    else -> uri.toString()
                }
                InfoRow(
                    icon = Icons.Default.Folder,
                    label = "Path",
                    value = path,
                    isScrollable = true
                )
                
                if (uri.scheme == "file") {
                    uri.path?.let { filePath ->
                        val file = File(filePath)
                        if (file.exists()) {
                            InfoRow(
                                icon = Icons.Default.Storage,
                                label = "File Size",
                                value = formatFileSize(file.length())
                            )
                            
                            InfoRow(
                                icon = Icons.Default.DateRange,
                                label = "Last Modified",
                                value = formatDate(file.lastModified())
                            )
                        }
                    }
                }
            }
            
            InfoRow(
                icon = Icons.Default.Schedule,
                label = "Duration",
                value = formatDuration(uiState.duration)
            )
            
            InfoRow(
                icon = Icons.Default.AspectRatio,
                label = "Resolution",
                value = "${uiState.videoWidth} × ${uiState.videoHeight}"
            )
            
            InfoRow(
                icon = Icons.Default.Speed,
                label = "Playback Speed",
                value = "${uiState.playbackSpeed}x"
            )
        }
    }
}

@Composable
fun TechnicalInfoTab(
    uiState: com.astralplayer.nextplayer.feature.player.viewmodel.PlayerUiState,
    player: ExoPlayer?
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        InfoCard {
            player?.videoFormat?.let { format ->
                InfoRow(
                    icon = Icons.Default.VideoLibrary,
                    label = "Video Codec",
                    value = format.codecs ?: "Unknown"
                )
                
                InfoRow(
                    icon = Icons.Default.HighQuality,
                    label = "Bitrate",
                    value = if (format.bitrate > 0) {
                        formatBitrate(format.bitrate)
                    } else "Variable"
                )
                
                InfoRow(
                    icon = Icons.Default.SlowMotionVideo,
                    label = "Frame Rate",
                    value = if (format.frameRate > 0) {
                        "${format.frameRate.roundToInt()} fps"
                    } else "Unknown"
                )
                
                InfoRow(
                    icon = Icons.Default.FormatSize,
                    label = "Container",
                    value = format.containerMimeType ?: "Unknown"
                )
                
                InfoRow(
                    icon = Icons.Default.ColorLens,
                    label = "Color Info",
                    value = format.colorInfo?.toString() ?: "Standard"
                )
            }
            
            InfoRow(
                icon = Icons.Default.AspectRatio,
                label = "Aspect Ratio",
                value = formatAspectRatio(uiState.aspectRatio)
            )
            
            InfoRow(
                icon = Icons.Default.Rotate90DegreesCcw,
                label = "Rotation",
                value = "${uiState.videoRotation.toInt()}°"
            )
            
            InfoRow(
                icon = Icons.Default.ZoomIn,
                label = "Zoom Level",
                value = "${(uiState.zoomLevel * 100).toInt()}%"
            )
        }
    }
}

@Composable
fun AudioInfoTab(player: ExoPlayer?) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        InfoCard {
            player?.audioFormat?.let { format ->
                InfoRow(
                    icon = Icons.Default.Audiotrack,
                    label = "Audio Codec",
                    value = format.codecs ?: "Unknown"
                )
                
                InfoRow(
                    icon = Icons.Default.GraphicEq,
                    label = "Sample Rate",
                    value = if (format.sampleRate > 0) {
                        "${format.sampleRate} Hz"
                    } else "Unknown"
                )
                
                InfoRow(
                    icon = Icons.Default.SurroundSound,
                    label = "Channels",
                    value = when (format.channelCount) {
                        1 -> "Mono"
                        2 -> "Stereo"
                        6 -> "5.1 Surround"
                        8 -> "7.1 Surround"
                        else -> "${format.channelCount} channels"
                    }
                )
                
                InfoRow(
                    icon = Icons.Default.HighQuality,
                    label = "Bitrate",
                    value = if (format.bitrate > 0) {
                        formatBitrate(format.bitrate)
                    } else "Variable"
                )
                
                InfoRow(
                    icon = Icons.Default.Language,
                    label = "Language",
                    value = format.language ?: "Unknown"
                )
            } ?: run {
                Text(
                    text = "No audio information available",
                    color = Color.White.copy(alpha = 0.5f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun AnalysisTab(
    videoAnalysis: com.astralplayer.nextplayer.feature.player.revolutionary.VideoAnalysisInfo?
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (videoAnalysis != null) {
            InfoCard {
                InfoRow(
                    icon = Icons.Default.Category,
                    label = "Content Type",
                    value = videoAnalysis.contentType
                )
                
                InfoRow(
                    icon = Icons.Default.Face,
                    label = "Faces Detected",
                    value = videoAnalysis.facesDetected.toString()
                )
                
                InfoRow(
                    icon = Icons.Default.Palette,
                    label = "Dominant Colors",
                    value = videoAnalysis.dominantColors.joinToString(", ")
                )
                
                InfoRow(
                    icon = Icons.Default.BrightnessHigh,
                    label = "Average Brightness",
                    value = "${(videoAnalysis.averageBrightness * 100).toInt()}%"
                )
                
                InfoRow(
                    icon = Icons.Default.MotionPhotosOn,
                    label = "Motion Level",
                    value = when {
                        videoAnalysis.motionLevel < 0.3f -> "Low"
                        videoAnalysis.motionLevel < 0.7f -> "Medium"
                        else -> "High"
                    }
                )
                
                if (videoAnalysis.objects.isNotEmpty()) {
                    InfoRow(
                        icon = Icons.Default.Label,
                        label = "Objects",
                        value = videoAnalysis.objects.joinToString(", ")
                    )
                }
                
                if (videoAnalysis.scenes.isNotEmpty()) {
                    InfoRow(
                        icon = Icons.Default.Landscape,
                        label = "Scenes",
                        value = videoAnalysis.scenes.joinToString(", ")
                    )
                }
            }
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White.copy(alpha = 0.05f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Analytics,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.3f),
                        modifier = Modifier.size(48.dp)
                    )
                    
                    Text(
                        text = "No analysis available",
                        color = Color.White.copy(alpha = 0.5f),
                        textAlign = TextAlign.Center
                    )
                    
                    Text(
                        text = "Enable AI features to analyze video content",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.3f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
fun InfoCard(
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.05f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content
        )
    }
}

@Composable
fun InfoRow(
    icon: ImageVector,
    label: String,
    value: String,
    isScrollable: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color(0xFF00D4FF),
            modifier = Modifier.size(20.dp)
        )
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.5f)
            )
            
            if (isScrollable) {
                Text(
                    text = value,
                    fontSize = 14.sp,
                    color = Color.White,
                    modifier = Modifier.horizontalScroll(rememberScrollState())
                )
            } else {
                Text(
                    text = value,
                    fontSize = 14.sp,
                    color = Color.White
                )
            }
        }
    }
}

// Helper functions
private fun formatFileSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
        else -> "%.2f GB".format(bytes / (1024.0 * 1024.0 * 1024.0))
    }
}

private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM d, yyyy HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

private fun formatDuration(millis: Long): String {
    val seconds = millis / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    
    return when {
        hours > 0 -> String.format("%d:%02d:%02d", hours, minutes % 60, seconds % 60)
        else -> String.format("%d:%02d", minutes, seconds % 60)
    }
}

private fun formatBitrate(bitrate: Int): String {
    return when {
        bitrate < 1000 -> "$bitrate bps"
        bitrate < 1000000 -> "${bitrate / 1000} kbps"
        else -> "%.1f Mbps".format(bitrate / 1000000.0)
    }
}

private fun formatAspectRatio(ratio: Float): String {
    return when {
        ratio < 1.4f -> "4:3"
        ratio < 1.6f -> "3:2"
        ratio < 1.85f -> "16:9"
        ratio < 2.1f -> "2:1"
        ratio < 2.4f -> "21:9"
        else -> "%.2f:1".format(ratio)
    }
}