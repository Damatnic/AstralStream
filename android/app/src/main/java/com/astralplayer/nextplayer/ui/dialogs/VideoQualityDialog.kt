package com.astralplayer.nextplayer.ui.dialogs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.common.Format
import androidx.media3.common.Tracks
import com.astralplayer.nextplayer.data.VideoQuality
import kotlinx.coroutines.launch

data class QualityOption(
    val quality: VideoQuality,
    val format: Format? = null,
    val isSelected: Boolean = false,
    val isAvailable: Boolean = true
)

@Composable
fun VideoQualityDialog(
    exoPlayer: ExoPlayer,
    onQualitySelected: (VideoQuality) -> Unit,
    onDismiss: () -> Unit
) {
    var qualityOptions by remember { mutableStateOf<List<QualityOption>>(emptyList()) }
    var selectedQuality by remember { mutableStateOf(VideoQuality.AUTO) }
    var isAutoQuality by remember { mutableStateOf(true) }
    
    // Extract available qualities from ExoPlayer
    LaunchedEffect(exoPlayer) {
        val tracks = exoPlayer.currentTracks
        val qualities = mutableListOf<QualityOption>()
        
        // Add AUTO option
        qualities.add(
            QualityOption(
                quality = VideoQuality.AUTO,
                isSelected = isAutoQuality
            )
        )
        
        // Extract video qualities
        val videoFormats = mutableSetOf<Format>()
        for (trackGroup in tracks.groups) {
            if (trackGroup.type == androidx.media3.common.C.TRACK_TYPE_VIDEO) {
                val group = trackGroup.mediaTrackGroup
                for (i in 0 until group.length) {
                    val format = group.getFormat(i)
                    if (format.height > 0) {
                        videoFormats.add(format)
                    }
                }
            }
        }
        
        // Sort formats by height (quality)
        val sortedFormats = videoFormats.sortedByDescending { it.height }
        
        // Map formats to quality options
        for (format in sortedFormats) {
            val quality = VideoQuality.fromHeight(format.height)
            
            // Avoid duplicates
            if (qualities.none { it.quality == quality }) {
                qualities.add(
                    QualityOption(
                        quality = quality,
                        format = format,
                        isSelected = !isAutoQuality && quality == selectedQuality
                    )
                )
            }
        }
        
        // Add standard qualities if not available in stream
        val standardQualities = VideoQuality.DEFAULT_QUALITIES.filter { it != VideoQuality.AUTO }
        
        for (standardQuality in standardQualities) {
            if (qualities.none { it.quality == standardQuality }) {
                qualities.add(
                    QualityOption(
                        quality = standardQuality,
                        isAvailable = false,
                        isSelected = false
                    )
                )
            }
        }
        
        qualityOptions = qualities.sortedByDescending { option ->
            if (option.quality.isAdaptive) Int.MAX_VALUE else option.quality.height
        }
    }
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                // Title
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Video Quality",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Close"
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Network info card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Wifi,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Network Quality",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Recommended: 720p",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Quality options
                LazyColumn {
                    items(qualityOptions) { option ->
                        QualityOptionItem(
                            option = option,
                            onClick = {
                                if (option.isAvailable) {
                                    selectedQuality = option.quality
                                    isAutoQuality = option.quality == VideoQuality.AUTO
                                    onQualitySelected(option.quality)
                                    onDismiss()
                                }
                            }
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Data saver toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.DataSaverOn,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Data Saver",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                    
                    Switch(
                        checked = false, // TODO: Implement data saver preference
                        onCheckedChange = { /* TODO: Implement data saver */ }
                    )
                }
            }
        }
    }
}

@Composable
fun QualityOptionItem(
    option: QualityOption,
    onClick: () -> Unit
) {
    val qualityText = if (option.quality.isAdaptive) {
        "Auto"
    } else {
        when (option.quality.height) {
            2160 -> "2160p (4K)"
            1440 -> "1440p (2K)"
            1080 -> "1080p (Full HD)"
            720 -> "720p (HD)"
            else -> "${option.quality.height}p"
        }
    }
    
    val bitrateText = option.format?.let {
        val bitrateMbps = it.bitrate / 1_000_000f
        if (bitrateMbps > 0) {
            String.format("%.1f Mbps", bitrateMbps)
        } else null
    }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = option.isAvailable) { onClick() }
            .alpha(if (option.isAvailable) 1f else 0.5f)
            .padding(vertical = 12.dp, horizontal = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = qualityText,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (option.isSelected) FontWeight.Bold else FontWeight.Normal
                )
                
                if (option.quality.isAdaptive) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Badge(
                        containerColor = MaterialTheme.colorScheme.primary
                    ) {
                        Text("Recommended", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
            
            if (bitrateText != null || !option.isAvailable) {
                Text(
                    text = if (!option.isAvailable) "Not available" else bitrateText!!,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        if (option.isSelected) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = "Selected",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}