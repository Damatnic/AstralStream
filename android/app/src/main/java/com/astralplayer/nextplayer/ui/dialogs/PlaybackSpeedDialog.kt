package com.astralplayer.nextplayer.ui.dialogs

import androidx.compose.animation.*
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.astralplayer.nextplayer.ui.components.*

/**
 * Playback Speed Selection Dialog
 * Allows users to select video playback speed
 */
@Composable
fun PlaybackSpeedDialog(
    currentSpeed: Float,
    onSpeedSelected: (Float) -> Unit,
    onDismiss: () -> Unit
) {
    val presetSpeeds = listOf(
        0.25f to "0.25x",
        0.5f to "0.5x",
        0.75f to "0.75x",
        1.0f to "Normal",
        1.25f to "1.25x",
        1.5f to "1.5x",
        1.75f to "1.75x",
        2.0f to "2x",
        2.5f to "2.5x",
        3.0f to "3x"
    )
    
    var customSpeed by remember { mutableStateOf(currentSpeed) }
    var showCustomSpeed by remember { mutableStateOf(false) }
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        BubbleCard(
            elevation = 16,
            cornerRadius = 24,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Playback Speed",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    
                    BubbleIconButton(
                        onClick = onDismiss,
                        icon = Icons.Default.Close,
                        size = 32,
                        iconSize = 18
                    )
                }
                
                // Preset speeds
                LazyColumn(
                    modifier = Modifier.heightIn(max = 400.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(presetSpeeds) { (speed, label) ->
                        SpeedOption(
                            speed = speed,
                            label = label,
                            isSelected = currentSpeed == speed,
                            onSelect = {
                                onSpeedSelected(speed)
                                onDismiss()
                            }
                        )
                    }
                    
                    // Custom speed option
                    item {
                        BubbleCard(
                            onClick = { showCustomSpeed = !showCustomSpeed },
                            elevation = if (showCustomSpeed) 4 else 2,
                            cornerRadius = 16,
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.Tune,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = "Custom Speed",
                                            style = MaterialTheme.typography.bodyLarge
                                        )
                                    }
                                    
                                    Icon(
                                        imageVector = if (showCustomSpeed) {
                                            Icons.Default.ExpandLess
                                        } else {
                                            Icons.Default.ExpandMore
                                        },
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                
                                AnimatedVisibility(visible = showCustomSpeed) {
                                    Column(
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Text(
                                            text = "${String.format("%.2f", customSpeed)}x",
                                            style = MaterialTheme.typography.headlineMedium,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        
                                        Slider(
                                            value = customSpeed,
                                            onValueChange = { customSpeed = it },
                                            valueRange = 0.1f..4f,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                        
                                        BubbleButton(
                                            onClick = {
                                                onSpeedSelected(customSpeed)
                                                onDismiss()
                                            },
                                            text = "Apply Custom Speed",
                                            modifier = Modifier.fillMaxWidth(),
                                            containerColor = MaterialTheme.colorScheme.primary,
                                            contentColor = MaterialTheme.colorScheme.onPrimary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SpeedOption(
    speed: Float,
    label: String,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    BubbleCard(
        onClick = onSelect,
        elevation = if (isSelected) 4 else 2,
        cornerRadius = 16,
        containerColor = if (isSelected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = if (isSelected) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
            
            if (isSelected) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}