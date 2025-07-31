// Missing UI Components for EliteVideoPlayerScreen.kt
// These components complete the advanced video player interface

package com.astralplayer.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.astralplayer.presentation.player.EnhancedVideoPlayerViewModel

// Settings Dialog Component
@Composable
fun SettingsDialog(
    onDismiss: () -> Unit,
    viewModel: EnhancedVideoPlayerViewModel
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                Text(
                    text = "Player Settings",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                // Auto-subtitle generation toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Auto-generate subtitles")
                    Switch(
                        checked = viewModel.isSubtitleGenerationEnabled,
                        onCheckedChange = { /* Update setting */ }
                    )
                }
                
                // Hardware acceleration toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Hardware acceleration")
                    Switch(
                        checked = true,
                        onCheckedChange = { /* Update setting */ }
                    )
                }
                
                // Gesture controls toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Gesture controls")
                    Switch(
                        checked = true,
                        onCheckedChange = { /* Update setting */ }
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Close")
                    }
                }
            }
        }
    }
}

// Subtitle Menu Component
@Composable
fun SubtitleMenu(
    onDismiss: () -> Unit,
    onGenerateSubtitles: () -> Unit,
    onLoadSubtitles: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                Text(
                    text = "Subtitles",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                // Generate subtitles option
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onGenerateSubtitles() }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 16.dp)
                    )
                    Column {
                        Text(
                            text = "Generate with AI",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Auto-generate subtitles using AI",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                Divider(modifier = Modifier.padding(vertical = 8.dp))
                
                // Load subtitle file option
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onLoadSubtitles() }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Upload,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 16.dp)
                    )
                    Column {
                        Text(
                            text = "Load from file",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Load .srt or .vtt subtitle file",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                }
            }
        }
    }
}

// Quality Menu Component
@Composable
fun QualityMenu(
    currentQuality: String,
    onQualitySelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val qualities = listOf("Auto", "2160p", "1440p", "1080p", "720p", "480p", "360p")
    
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                Text(
                    text = "Video Quality",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                LazyColumn {
                    items(qualities) { quality ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onQualitySelected(quality) }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = currentQuality.equals(quality, ignoreCase = true),
                                onClick = { onQualitySelected(quality) }
                            )
                            Text(
                                text = quality,
                                modifier = Modifier.padding(start = 16.dp),
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                }
            }
        }
    }
}

// Speed Button Component
@Composable
fun SpeedButton(
    currentSpeed: Float,
    onSpeedChange: (Float) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val speeds = listOf(0.25f, 0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f)
    
    Box {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.height(40.dp)
        ) {
            Text(
                text = "${currentSpeed}x",
                color = Color.White,
                fontSize = 12.sp
            )
        }
        
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            speeds.forEach { speed ->
                DropdownMenuItem(
                    text = { Text("${speed}x") },
                    onClick = {
                        onSpeedChange(speed)
                        expanded = false
                    }
                )
            }
        }
    }
}

// Gesture Overlay Indicators
@Composable
fun VolumeIndicator(increase: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.3f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = if (increase) Icons.Default.VolumeUp else Icons.Default.VolumeDown,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(48.dp)
            )
            Text(
                text = if (increase) "Volume +" else "Volume -",
                color = Color.White,
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

@Composable
fun SeekIndicator(forward: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.3f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = if (forward) Icons.Default.FastForward else Icons.Default.FastRewind,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(48.dp)
            )
            Text(
                text = if (forward) "+10s" else "-10s",
                color = Color.White,
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

@Composable
fun ZoomIndicator(zoomLevel: Float) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.3f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.ZoomIn,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(48.dp)
            )
            Text(
                text = "${(zoomLevel * 100).toInt()}%",
                color = Color.White,
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

// Data classes for UI components
data class SubtitleEntry(
    val startTime: Long,
    val endTime: Long,
    val text: String,
    val language: String = "en"
)

// Extension for ViewModel functionality
suspend fun EnhancedVideoPlayerViewModel.generateSubtitles() {
    // Implementation would call the AI subtitle generator
}

fun EnhancedVideoPlayerViewModel.showSeekAnimation(seconds: Int) {
    // Implementation would trigger seek animation
}

fun EnhancedVideoPlayerViewModel.setVideoQuality(quality: String) {
    // Implementation would change video quality
}