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
import com.astralplayer.nextplayer.audio.AudioEqualizerManager
import com.astralplayer.nextplayer.audio.EqualizerPreset

/**
 * Audio equalizer dialog with presets and custom controls
 */
@Composable
fun EqualizerDialog(
    equalizerManager: AudioEqualizerManager,
    onDismiss: () -> Unit
) {
    val isEnabled by equalizerManager.isEnabled.collectAsState()
    val currentPreset by equalizerManager.currentPreset.collectAsState()
    val bandLevels by equalizerManager.bandLevels.collectAsState()
    
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
                        text = "Audio Equalizer",
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Switch(
                        checked = isEnabled,
                        onCheckedChange = { equalizerManager.setEnabled(it) }
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
                        items(EqualizerPreset.values()) { preset ->
                            FilterChip(
                                selected = preset == currentPreset,
                                onClick = { equalizerManager.applyPreset(preset) },
                                label = { Text(preset.displayName) }
                            )
                        }
                    }
                    
                    // Band controls
                    if (bandLevels.isNotEmpty()) {
                        Text(
                            text = "Custom EQ",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White
                        )
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            bandLevels.forEachIndexed { index, level ->
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = "${equalizerManager.getBandFrequency(index)}Hz",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White.copy(alpha = 0.7f)
                                    )
                                    
                                    Slider(
                                        value = level,
                                        onValueChange = { newLevel ->
                                            equalizerManager.setBandLevel(index, newLevel)
                                        },
                                        valueRange = 0f..1f,
                                        modifier = Modifier.height(120.dp)
                                    )
                                }
                            }
                        }
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