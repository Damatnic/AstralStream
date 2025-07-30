package com.astralplayer.nextplayer.ui.dialogs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.astralplayer.nextplayer.data.VideoFiltersManager

@Composable
fun VideoFiltersDialog(
    filterState: VideoFiltersManager.VideoFilterState,
    onBrightnessChange: (Float) -> Unit,
    onContrastChange: (Float) -> Unit,
    onSaturationChange: (Float) -> Unit,
    onHueChange: (Float) -> Unit,
    onRotationChange: (Float) -> Unit,
    onZoomChange: (Float) -> Unit,
    onGrayscaleToggle: () -> Unit,
    onInvertToggle: () -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit
) {
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
                .fillMaxHeight(0.9f)
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
                        text = "Video Adjustments",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Row {
                        // Reset button
                        IconButton(onClick = onReset) {
                            Icon(
                                imageVector = Icons.Filled.RestartAlt,
                                contentDescription = "Reset",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        
                        // Close button
                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "Close"
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Scrollable content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    // Brightness
                    FilterSlider(
                        label = "Brightness",
                        value = filterState.brightness,
                        onValueChange = onBrightnessChange,
                        valueRange = -1f..1f,
                        icon = Icons.Filled.Brightness7,
                        formatValue = { "${(it * 100).toInt()}%" }
                    )
                    
                    // Contrast
                    FilterSlider(
                        label = "Contrast",
                        value = filterState.contrast,
                        onValueChange = onContrastChange,
                        valueRange = 0f..2f,
                        icon = Icons.Filled.Contrast,
                        formatValue = { "${(it * 100).toInt()}%" }
                    )
                    
                    // Saturation
                    FilterSlider(
                        label = "Saturation",
                        value = filterState.saturation,
                        onValueChange = onSaturationChange,
                        valueRange = 0f..2f,
                        icon = Icons.Filled.Palette,
                        formatValue = { "${(it * 100).toInt()}%" }
                    )
                    
                    // Hue
                    FilterSlider(
                        label = "Hue",
                        value = filterState.hue,
                        onValueChange = onHueChange,
                        valueRange = -180f..180f,
                        icon = Icons.Filled.ColorLens,
                        formatValue = { "${it.toInt()}°" }
                    )
                    
                    // Zoom
                    FilterSlider(
                        label = "Zoom",
                        value = filterState.zoom,
                        onValueChange = onZoomChange,
                        valueRange = 0.5f..3f,
                        icon = Icons.Filled.ZoomIn,
                        formatValue = { "${(it * 100).toInt()}%" }
                    )
                    
                    // Rotation buttons
                    Column {
                        Text(
                            text = "Rotation",
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            RotationButton(
                                rotation = 0f,
                                currentRotation = filterState.rotation,
                                onClick = { onRotationChange(0f) }
                            )
                            RotationButton(
                                rotation = 90f,
                                currentRotation = filterState.rotation,
                                onClick = { onRotationChange(90f) }
                            )
                            RotationButton(
                                rotation = 180f,
                                currentRotation = filterState.rotation,
                                onClick = { onRotationChange(180f) }
                            )
                            RotationButton(
                                rotation = 270f,
                                currentRotation = filterState.rotation,
                                onClick = { onRotationChange(270f) }
                            )
                        }
                    }
                    
                    // Toggle filters
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterToggle(
                            label = "Grayscale",
                            checked = filterState.isGrayscale,
                            onCheckedChange = { onGrayscaleToggle() },
                            icon = Icons.Filled.FilterBAndW
                        )
                        
                        FilterToggle(
                            label = "Invert Colors",
                            checked = filterState.isInverted,
                            onCheckedChange = { onInvertToggle() },
                            icon = Icons.Filled.InvertColors
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Apply button
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Apply")
                }
            }
        }
    }
}

@Composable
fun FilterSlider(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    formatValue: (Float) -> String
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge
                )
            }
            
            Text(
                text = formatValue(value),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun RotationButton(
    rotation: Float,
    currentRotation: Float,
    onClick: () -> Unit
) {
    val isSelected = rotation == currentRotation
    
    FilterChip(
        selected = isSelected,
        onClick = onClick,
        label = { Text("${rotation.toInt()}°") },
        leadingIcon = {
            Icon(
                imageVector = Icons.Filled.RotateRight,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
        }
    )
}

@Composable
fun FilterToggle(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (checked) 
                MaterialTheme.colorScheme.primaryContainer 
            else 
                MaterialTheme.colorScheme.surface
        ),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (checked)
                        MaterialTheme.colorScheme.onPrimaryContainer
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (checked) FontWeight.Medium else FontWeight.Normal
                )
            }
            
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange
            )
        }
    }
}