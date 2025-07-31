package com.astralplayer.nextplayer.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun QualitySelectionDialog(
    availableQualities: List<VideoQuality>,
    currentQuality: VideoQuality?,
    onQualitySelected: (VideoQuality) -> Unit,
    onDismiss: () -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Video Quality")
                IconButton(onClick = onSettingsClick) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Quality settings"
                    )
                }
            }
        },
        text = {
            LazyColumn {
                // Auto quality option
                item {
                    QualityItem(
                        quality = null,
                        isSelected = currentQuality == null,
                        onClick = { 
                            onQualitySelected(VideoQuality.AUTO)
                            onDismiss()
                        }
                    )
                }
                
                if (availableQualities.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalDivider()
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
                
                // Available quality options
                items(availableQualities) { quality ->
                    QualityItem(
                        quality = quality,
                        isSelected = quality == currentQuality,
                        onClick = { 
                            onQualitySelected(quality)
                            onDismiss()
                        }
                    )
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

@Composable
private fun QualityItem(
    quality: VideoQuality?,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = quality?.label ?: "Auto",
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
            if (quality != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Resolution info
                    Text(
                        text = "${quality.height}p",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    // Bitrate info
                    if (quality.bitrate > 0) {
                        Text(
                            text = "• ${formatBitrate(quality.bitrate)}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    // Codec info
                    if (quality.codec.isNotEmpty()) {
                        Text(
                            text = "• ${quality.codec}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                Text(
                    text = "Automatically select best quality",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Selected",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun QualitySettingsSheet(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var preferHigherQuality by remember { mutableStateOf(true) }
    var allowMobileData by remember { mutableStateOf(false) }
    var maxQualityMobile by remember { mutableStateOf("720p") }
    var maxQualityWifi by remember { mutableStateOf("4K") }
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = "Quality Settings",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            // Prefer higher quality
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Prefer Higher Quality")
                    Text(
                        text = "Use highest available quality when bandwidth allows",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = preferHigherQuality,
                    onCheckedChange = { preferHigherQuality = it }
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(8.dp))
            
            // Mobile data usage
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Stream on Mobile Data")
                    Text(
                        text = "Allow video playback using mobile data",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = allowMobileData,
                    onCheckedChange = { allowMobileData = it }
                )
            }
            
            if (allowMobileData) {
                Spacer(modifier = Modifier.height(16.dp))
                
                // Max quality on mobile
                Text(
                    text = "Max Quality on Mobile Data",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    listOf("480p", "720p", "1080p").forEach { quality ->
                        FilterChip(
                            selected = maxQualityMobile == quality,
                            onClick = { maxQualityMobile = quality },
                            label = { Text(quality) }
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Max quality on WiFi
            Text(
                text = "Max Quality on WiFi",
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                listOf("1080p", "1440p", "4K").forEach { quality ->
                    FilterChip(
                        selected = maxQualityWifi == quality,
                        onClick = { maxQualityWifi = quality },
                        label = { Text(quality) }
                    )
                }
            }
        }
    }
}

data class VideoQuality(
    val id: String,
    val label: String,
    val width: Int,
    val height: Int,
    val bitrate: Int,
    val codec: String = "",
    val fps: Int = 0
) {
    companion object {
        val AUTO = VideoQuality(
            id = "auto",
            label = "Auto",
            width = 0,
            height = 0,
            bitrate = 0
        )
    }
}

private fun formatBitrate(bitrate: Int): String {
    return when {
        bitrate >= 1_000_000 -> "${bitrate / 1_000_000}Mbps"
        bitrate >= 1_000 -> "${bitrate / 1_000}Kbps"
        else -> "${bitrate}bps"
    }
}