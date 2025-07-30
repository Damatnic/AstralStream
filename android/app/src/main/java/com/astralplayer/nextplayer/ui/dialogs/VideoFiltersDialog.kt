package com.astralplayer.nextplayer.ui.dialogs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.astralplayer.nextplayer.video.VideoFiltersManager
import com.astralplayer.nextplayer.video.VideoFilterPreset

/**
 * Video filters dialog with presets and manual controls
 */
@Composable
fun VideoFiltersDialog(
    filtersManager: VideoFiltersManager,
    onDismiss: () -> Unit
) {
    val isEnabled by filtersManager.isEnabled.collectAsState()
    val filters by filtersManager.filters.collectAsState()
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.9f))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Video Filters",
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Switch(
                        checked = isEnabled,
                        onCheckedChange = { filtersManager.setEnabled(it) }
                    )
                }
                
                if (isEnabled) {
                    // Presets
                    Text(
                        text = "Presets",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )
                    
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(VideoFilterPreset.values()) { preset ->
                            FilterChip(
                                selected = false, // Track current preset state
                                onClick = { filtersManager.applyPreset(preset) },
                                label = { Text(preset.displayName) }
                            )
                        }
                    }
                    
                    // Manual controls
                    Text(
                        text = "Manual Adjustments",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )
                    
                    // Brightness
                    FilterSlider(
                        label = "Brightness",
                        value = filters.brightness,
                        valueRange = -1f..1f,
                        onValueChange = { filtersManager.setBrightness(it) }
                    )
                    
                    // Contrast
                    FilterSlider(
                        label = "Contrast",
                        value = filters.contrast,
                        valueRange = 0f..2f,
                        onValueChange = { filtersManager.setContrast(it) }
                    )
                    
                    // Saturation
                    FilterSlider(
                        label = "Saturation",
                        value = filters.saturation,
                        valueRange = 0f..2f,
                        onValueChange = { filtersManager.setSaturation(it) }
                    )
                    
                    // Hue
                    FilterSlider(
                        label = "Hue",
                        value = filters.hue,
                        valueRange = -180f..180f,
                        onValueChange = { filtersManager.setHue(it) }
                    )
                    
                    // Reset button
                    Button(
                        onClick = { filtersManager.resetToDefaults() },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Red.copy(alpha = 0.2f)
                        )
                    ) {
                        Text("Reset to Defaults", color = Color.Red)
                    }
                }
                
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Close")
                }
            }
        }
    }
}

@Composable
private fun FilterSlider(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "%.2f".format(value),
                color = Color.White.copy(alpha = 0.7f),
                style = MaterialTheme.typography.bodySmall
            )
        }
        
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange
        )
    }
}