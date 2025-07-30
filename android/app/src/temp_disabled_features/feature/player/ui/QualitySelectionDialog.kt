package com.astralplayer.nextplayer.feature.player.ui

import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector

data class VideoQuality(
    val groupIndex: Int,
    val trackIndex: Int,
    val format: Format,
    val label: String,
    val resolution: String,
    val bitrate: Long,
    val isSelected: Boolean
)

@Composable
fun QualitySelectionDialog(
    player: ExoPlayer?,
    trackSelector: DefaultTrackSelector?,
    onDismiss: () -> Unit
) {
    val videoQualities = remember(player, trackSelector) {
        getVideoQualities(player, trackSelector)
    }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF1A1A2E)
            )
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Video Quality",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Quality options
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Auto option
                    item {
                        QualityOption(
                            title = "Auto",
                            subtitle = "Adaptive quality",
                            isSelected = videoQualities.none { it.isSelected },
                            isRecommended = true,
                            onClick = {
                                // Clear quality restrictions
                                trackSelector?.let { selector ->
                                    selector.parameters = selector.buildUponParameters()
                                        .clearVideoSizeConstraints()
                                        .clearOverridesOfType(C.TRACK_TYPE_VIDEO)
                                        .build()
                                }
                                onDismiss()
                            }
                        )
                    }
                    
                    // Available qualities
                    items(videoQualities.sortedByDescending { it.format.height }) { quality ->
                        QualityOption(
                            title = quality.label,
                            subtitle = formatBitrate(quality.bitrate),
                            isSelected = quality.isSelected,
                            isRecommended = false,
                            onClick = {
                                // Select specific quality
                                val group = player?.currentTracks?.groups?.get(quality.groupIndex)
                                if (group != null && trackSelector != null) {
                                    trackSelector.parameters = trackSelector.buildUponParameters()
                                        .setOverrideForType(
                                            TrackSelectionOverride(
                                                group.mediaTrackGroup,
                                                quality.trackIndex
                                            )
                                        )
                                        .build()
                                }
                                onDismiss()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QualityOption(
    title: String,
    subtitle: String,
    isSelected: Boolean,
    isRecommended: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(
                when {
                    isSelected -> Color(0xFF00BCD4).copy(alpha = 0.2f)
                    isRecommended -> Color(0xFF4CAF50).copy(alpha = 0.1f)
                    else -> Color.White.copy(alpha = 0.05f)
                }
            )
            .clickable { onClick() }
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                )
                if (isRecommended) {
                    Surface(
                        color = Color(0xFF4CAF50),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "RECOMMENDED",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
            Text(
                text = subtitle,
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 14.sp
            )
        }
        
        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Selected",
                tint = Color(0xFF00BCD4),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

private fun getVideoQualities(player: ExoPlayer?, trackSelector: DefaultTrackSelector?): List<VideoQuality> {
    val qualities = mutableListOf<VideoQuality>()
    
    player?.currentTracks?.groups?.forEachIndexed { groupIndex, group ->
        if (group.type == C.TRACK_TYPE_VIDEO) {
            val trackGroup = group.mediaTrackGroup
            val selectedTrackIndex = getSelectedTrackIndex(group)
            
            for (trackIndex in 0 until trackGroup.length) {
                val format = trackGroup.getFormat(trackIndex)
                val resolution = "${format.width}x${format.height}"
                val label = when {
                    format.height >= 2160 -> "4K"
                    format.height >= 1440 -> "2K"
                    format.height >= 1080 -> "1080p"
                    format.height >= 720 -> "720p"
                    format.height >= 480 -> "480p"
                    format.height >= 360 -> "360p"
                    else -> "SD"
                }
                
                qualities.add(
                    VideoQuality(
                        groupIndex = groupIndex,
                        trackIndex = trackIndex,
                        format = format,
                        label = "$label ($resolution)",
                        resolution = resolution,
                        bitrate = format.bitrate.toLong(),
                        isSelected = trackIndex == selectedTrackIndex
                    )
                )
            }
        }
    }
    
    return qualities
}

private fun getSelectedTrackIndex(group: androidx.media3.common.Tracks.Group): Int {
    for (i in 0 until group.length) {
        if (group.isTrackSelected(i)) {
            return i
        }
    }
    return -1
}

private fun formatBitrate(bitrate: Long): String {
    return when {
        bitrate <= 0 -> "Unknown bitrate"
        bitrate < 1_000_000 -> "${bitrate / 1_000} Kbps"
        else -> "%.1f Mbps".format(bitrate / 1_000_000.0)
    }
}