package com.astralplayer.nextplayer.ui.components

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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.round

data class PlaybackSpeedPreset(
    val speed: Float,
    val name: String,
    val description: String = "",
    val isCustom: Boolean = false
) {
    companion object {
        val DEFAULT_PRESETS = listOf(
            PlaybackSpeedPreset(0.25f, "0.25x", "Very Slow"),
            PlaybackSpeedPreset(0.5f, "0.5x", "Half Speed"),
            PlaybackSpeedPreset(0.75f, "0.75x", "Slow"),
            PlaybackSpeedPreset(1.0f, "Normal", "Default Speed"),
            PlaybackSpeedPreset(1.25f, "1.25x", "Slightly Fast"),
            PlaybackSpeedPreset(1.5f, "1.5x", "Fast"),
            PlaybackSpeedPreset(1.75f, "1.75x", "Very Fast"),
            PlaybackSpeedPreset(2.0f, "2x", "Double Speed"),
            PlaybackSpeedPreset(2.5f, "2.5x", "Super Fast"),
            PlaybackSpeedPreset(3.0f, "3x", "Maximum")
        )
    }
}

@Composable
fun PlaybackSpeedDialog(
    currentSpeed: Float,
    onSpeedSelected: (Float) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    customPresets: List<PlaybackSpeedPreset> = emptyList()
) {
    var selectedSpeed by remember { mutableStateOf(currentSpeed) }
    var showCustomInput by remember { mutableStateOf(false) }
    var customSpeedText by remember { mutableStateOf("") }
    
    val allPresets = remember(customPresets) {
        PlaybackSpeedPreset.DEFAULT_PRESETS + customPresets
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Playback Speed")
                IconButton(
                    onClick = { showCustomInput = !showCustomInput }
                ) {
                    Icon(
                        imageVector = if (showCustomInput) Icons.Default.Close else Icons.Default.Add,
                        contentDescription = if (showCustomInput) "Close custom" else "Add custom speed"
                    )
                }
            }
        },
        text = {
            Column {
                // Current speed indicator
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Current Speed",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = formatSpeed(selectedSpeed),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Custom speed input
                if (showCustomInput) {
                    CustomSpeedInput(
                        value = customSpeedText,
                        onValueChange = { customSpeedText = it },
                        onConfirm = { speed ->
                            if (speed > 0) {
                                selectedSpeed = speed
                                customSpeedText = ""
                                showCustomInput = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                }
                
                // Speed presets
                LazyColumn(
                    modifier = Modifier.height(300.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(allPresets) { preset ->
                        SpeedPresetItem(
                            preset = preset,
                            isSelected = preset.speed == selectedSpeed,
                            onClick = { selectedSpeed = preset.speed }
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Quick adjustment buttons
                QuickSpeedAdjustment(
                    currentSpeed = selectedSpeed,
                    onSpeedChanged = { selectedSpeed = it }
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSpeedSelected(selectedSpeed)
                    onDismiss()
                }
            ) {
                Text("Apply")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        modifier = modifier
    )
}

@Composable
private fun SpeedPresetItem(
    preset: PlaybackSpeedPreset,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) 
                MaterialTheme.colorScheme.primaryContainer 
            else 
                MaterialTheme.colorScheme.surface
        ),
        border = if (isSelected) 
            androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = preset.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) 
                        MaterialTheme.colorScheme.onPrimaryContainer 
                    else 
                        MaterialTheme.colorScheme.onSurface
                )
                if (preset.description.isNotEmpty()) {
                    Text(
                        text = preset.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isSelected) 
                            MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        else 
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }
            
            if (preset.isCustom) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = "Custom preset",
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Selected",
                    modifier = Modifier.size(24.dp),
                    tint = if (isSelected) 
                        MaterialTheme.colorScheme.onPrimaryContainer 
                    else 
                        MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun CustomSpeedInput(
    value: String,
    onValueChange: (String) -> Unit,
    onConfirm: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Custom Speed",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = value,
                    onValueChange = onValueChange,
                    label = { Text("Speed (e.g., 1.5)") },
                    placeholder = { Text("1.0") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                
                Button(
                    onClick = {
                        val speed = value.toFloatOrNull()
                        if (speed != null && speed > 0) {
                            onConfirm(speed)
                        }
                    },
                    enabled = value.toFloatOrNull()?.let { it > 0 } == true
                ) {
                    Text("Add")
                }
            }
            
            Text(
                text = "Range: 0.25x - 4.0x",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
private fun QuickSpeedAdjustment(
    currentSpeed: Float,
    onSpeedChanged: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Quick Adjustment",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                QuickAdjustButton(
                    icon = Icons.Default.Remove,
                    label = "-0.25x",
                    onClick = { 
                        val newSpeed = (currentSpeed - 0.25f).coerceAtLeast(0.25f)
                        onSpeedChanged(round(newSpeed * 100) / 100)
                    }
                )
                
                QuickAdjustButton(
                    icon = Icons.Default.RestartAlt,
                    label = "Reset",
                    onClick = { onSpeedChanged(1.0f) }
                )
                
                QuickAdjustButton(
                    icon = Icons.Default.Add,
                    label = "+0.25x",
                    onClick = { 
                        val newSpeed = (currentSpeed + 0.25f).coerceAtMost(4.0f)
                        onSpeedChanged(round(newSpeed * 100) / 100)
                    }
                )
            }
        }
    }
}

@Composable
private fun QuickAdjustButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Box(
                modifier = Modifier.padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center
        )
    }
}

private fun formatSpeed(speed: Float): String {
    return when {
        speed == 1.0f -> "Normal"
        speed % 1 == 0f -> "${speed.toInt()}x"
        else -> "${speed}x"
    }
}

@Composable
fun PlaybackSpeedIndicator(
    currentSpeed: Float,
    isVisible: Boolean,
    modifier: Modifier = Modifier
) {
    androidx.compose.animation.AnimatedVisibility(
        visible = isVisible && currentSpeed != 1.0f,
        enter = androidx.compose.animation.fadeIn() + androidx.compose.animation.scaleIn(),
        exit = androidx.compose.animation.fadeOut() + androidx.compose.animation.scaleOut(),
        modifier = modifier
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = Color.Black.copy(alpha = 0.8f)
            ),
            shape = RoundedCornerShape(20.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = when {
                        currentSpeed > 1.0f -> Icons.Default.FastForward
                        currentSpeed < 1.0f -> Icons.Default.SlowMotionVideo
                        else -> Icons.Default.PlayArrow
                    },
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = formatSpeed(currentSpeed),
                    color = Color.White,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}