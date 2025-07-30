package com.astralplayer.nextplayer.ui.dialogs

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.astralplayer.nextplayer.feature.subtitle.SubtitleStyle
import com.astralplayer.nextplayer.ui.components.*

/**
 * Subtitle Style Customization Dialog
 * Advanced settings for subtitle appearance with real-time preview
 */
@Composable
fun SubtitleStyleDialog(
    currentStyle: SubtitleStyle,
    onStyleChanged: (SubtitleStyle) -> Unit,
    onDismiss: () -> Unit
) {
    var editingStyle by remember { mutableStateOf(currentStyle) }
    var showColorPicker by remember { mutableStateOf(false) }
    var colorPickerType by remember { mutableStateOf(ColorPickerType.TEXT) }
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        )
    ) {
        BubbleCard(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.9f),
            elevation = 24,
            cornerRadius = 32,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Header
                StyleDialogHeader(
                    onClose = onDismiss,
                    onReset = { editingStyle = SubtitleStyle.Default }
                )
                
                // Preview
                SubtitlePreview(
                    style = editingStyle,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .padding(horizontal = 24.dp)
                )
                
                Divider(
                    modifier = Modifier.padding(vertical = 16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                )
                
                // Style controls
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Text Size
                    item {
                        TextSizeControl(
                            currentSize = editingStyle.fontSize,
                            onSizeChanged = { size ->
                                editingStyle = editingStyle.copy(fontSize = size)
                            }
                        )
                    }
                    
                    // Font Scale
                    item {
                        FontScaleControl(
                            currentScale = editingStyle.fontScale,
                            onScaleChanged = { scale ->
                                editingStyle = editingStyle.copy(fontScale = scale)
                            }
                        )
                    }
                    
                    // Text Style
                    item {
                        TextStyleControl(
                            isBold = editingStyle.isBold,
                            onBoldChanged = { bold ->
                                editingStyle = editingStyle.copy(isBold = bold)
                            }
                        )
                    }
                    
                    // Colors
                    item {
                        ColorControls(
                            textColor = editingStyle.textColor,
                            backgroundColor = editingStyle.backgroundColor,
                            edgeColor = editingStyle.edgeColor,
                            onTextColorClick = {
                                colorPickerType = ColorPickerType.TEXT
                                showColorPicker = true
                            },
                            onBackgroundColorClick = {
                                colorPickerType = ColorPickerType.BACKGROUND
                                showColorPicker = true
                            },
                            onEdgeColorClick = {
                                colorPickerType = ColorPickerType.EDGE
                                showColorPicker = true
                            }
                        )
                    }
                    
                    // Edge Type
                    item {
                        EdgeTypeControl(
                            currentEdgeType = editingStyle.edgeType,
                            onEdgeTypeChanged = { type ->
                                editingStyle = editingStyle.copy(edgeType = type)
                            }
                        )
                    }
                    
                    // Padding
                    item {
                        PaddingControl(
                            horizontalPadding = editingStyle.horizontalPadding,
                            verticalPadding = editingStyle.verticalPadding,
                            onHorizontalPaddingChanged = { padding ->
                                editingStyle = editingStyle.copy(horizontalPadding = padding)
                            },
                            onVerticalPaddingChanged = { padding ->
                                editingStyle = editingStyle.copy(verticalPadding = padding)
                            }
                        )
                    }
                }
                
                // Apply button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                ) {
                    BubbleButton(
                        onClick = {
                            onStyleChanged(editingStyle)
                            onDismiss()
                        },
                        text = "Apply Style",
                        icon = Icons.Default.Check,
                        modifier = Modifier.fillMaxWidth(),
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    }
    
    // Color picker dialog
    if (showColorPicker) {
        ColorPickerDialog(
            currentColor = when (colorPickerType) {
                ColorPickerType.TEXT -> Color(editingStyle.textColor)
                ColorPickerType.BACKGROUND -> Color(editingStyle.backgroundColor)
                ColorPickerType.EDGE -> Color(editingStyle.edgeColor)
            },
            onColorSelected = { color ->
                editingStyle = when (colorPickerType) {
                    ColorPickerType.TEXT -> editingStyle.copy(textColor = color.toArgb())
                    ColorPickerType.BACKGROUND -> editingStyle.copy(backgroundColor = color.toArgb())
                    ColorPickerType.EDGE -> editingStyle.copy(edgeColor = color.toArgb())
                }
                showColorPicker = false
            },
            onDismiss = { showColorPicker = false }
        )
    }
}

@Composable
private fun StyleDialogHeader(
    onClose: () -> Unit,
    onReset: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Subtitle Style",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            BubbleIconButton(
                onClick = onReset,
                icon = Icons.Default.RestartAlt,
                size = 36,
                iconSize = 20
            )
            
            BubbleIconButton(
                onClick = onClose,
                icon = Icons.Default.Close,
                size = 36,
                iconSize = 20
            )
        }
    }
}

@Composable
private fun SubtitlePreview(
    style: SubtitleStyle,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        // Simulated video background
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.DarkGray)
        )
        
        // Subtitle preview
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .background(
                        color = Color(style.backgroundColor),
                        shape = RoundedCornerShape(style.cornerRadius)
                    )
                    .padding(
                        horizontal = style.horizontalPadding,
                        vertical = style.verticalPadding
                    )
            ) {
                Text(
                    text = "This is a preview of your subtitles",
                    fontSize = style.fontSize,
                    color = Color(style.textColor),
                    fontWeight = if (style.isBold) FontWeight.Bold else FontWeight.Normal,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun TextSizeControl(
    currentSize: sp,
    onSizeChanged: (sp) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Text Size",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(14.sp, 16.sp, 18.sp, 20.sp, 22.sp, 24.sp).forEach { size ->
                BubbleChip(
                    text = "${size.value.toInt()}",
                    selected = currentSize == size,
                    onClick = { onSizeChanged(size) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun FontScaleControl(
    currentScale: Float,
    onScaleChanged: (Float) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Font Scale",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium
        )
        
        Slider(
            value = currentScale,
            onValueChange = onScaleChanged,
            valueRange = 0.5f..2.0f,
            steps = 15,
            modifier = Modifier.fillMaxWidth()
        )
        
        Text(
            text = "${(currentScale * 100).toInt()}%",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
    }
}

@Composable
private fun TextStyleControl(
    isBold: Boolean,
    onBoldChanged: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Bold Text",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium
        )
        
        Switch(
            checked = isBold,
            onCheckedChange = onBoldChanged
        )
    }
}

@Composable
private fun ColorControls(
    textColor: Int,
    backgroundColor: Int,
    edgeColor: Int,
    onTextColorClick: () -> Unit,
    onBackgroundColorClick: () -> Unit,
    onEdgeColorClick: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Colors",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium
        )
        
        ColorOption(
            label = "Text Color",
            color = Color(textColor),
            onClick = onTextColorClick
        )
        
        ColorOption(
            label = "Background Color",
            color = Color(backgroundColor),
            onClick = onBackgroundColorClick
        )
        
        ColorOption(
            label = "Edge Color",
            color = Color(edgeColor),
            onClick = onEdgeColorClick
        )
    }
}

@Composable
private fun ColorOption(
    label: String,
    color: Color,
    onClick: () -> Unit
) {
    BubbleCard(
        onClick = onClick,
        elevation = 2,
        cornerRadius = 12,
        containerColor = MaterialTheme.colorScheme.surfaceVariant
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
                style = MaterialTheme.typography.bodyLarge
            )
            
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(color)
                    .border(
                        width = 2.dp,
                        color = MaterialTheme.colorScheme.outline,
                        shape = CircleShape
                    )
            )
        }
    }
}

@Composable
private fun EdgeTypeControl(
    currentEdgeType: Int,
    onEdgeTypeChanged: (Int) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Edge Type",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium
        )
        
        val edgeTypes = listOf(
            0 to "None",
            1 to "Outline", 
            2 to "Drop Shadow",
            3 to "Raised",
            4 to "Depressed"
        )
        
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            edgeTypes.forEach { (type, name) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onEdgeTypeChanged(type) }
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = currentEdgeType == type,
                        onClick = { onEdgeTypeChanged(type) }
                    )
                    
                    Text(
                        text = name,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }
    }
}

@Composable
private fun PaddingControl(
    horizontalPadding: dp,
    verticalPadding: dp,
    onHorizontalPaddingChanged: (dp) -> Unit,
    onVerticalPaddingChanged: (dp) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Padding",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium
        )
        
        // Horizontal padding
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "Horizontal: ${horizontalPadding.value.toInt()}dp",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Slider(
                value = horizontalPadding.value,
                onValueChange = { onHorizontalPaddingChanged(it.dp) },
                valueRange = 0f..32f,
                steps = 32,
                modifier = Modifier.fillMaxWidth()
            )
        }
        
        // Vertical padding
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "Vertical: ${verticalPadding.value.toInt()}dp",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Slider(
                value = verticalPadding.value,
                onValueChange = { onVerticalPaddingChanged(it.dp) },
                valueRange = 0f..16f,
                steps = 16,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun ColorPickerDialog(
    currentColor: Color,
    onColorSelected: (Color) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedColor by remember { mutableStateOf(currentColor) }
    
    Dialog(onDismissRequest = onDismiss) {
        BubbleCard(
            elevation = 16,
            cornerRadius = 24
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Choose Color",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                
                // Preset colors
                val presetColors = listOf(
                    Color.White,
                    Color.Black,
                    Color.Yellow,
                    Color.Red,
                    Color.Green,
                    Color.Blue,
                    Color.Magenta,
                    Color.Cyan,
                    Color(0xFFFF9800), // Orange
                    Color(0xFF9C27B0), // Purple
                    Color(0xFF795548), // Brown
                    Color(0xFF607D8B)  // Blue Grey
                )
                
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    presetColors.chunked(4).forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            row.forEach { color ->
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                        .clip(CircleShape)
                                        .background(color)
                                        .border(
                                            width = if (selectedColor == color) 3.dp else 1.dp,
                                            color = if (selectedColor == color) {
                                                MaterialTheme.colorScheme.primary
                                            } else {
                                                MaterialTheme.colorScheme.outline
                                            },
                                            shape = CircleShape
                                        )
                                        .clickable { selectedColor = color }
                                )
                            }
                        }
                    }
                }
                
                // Alpha slider
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Opacity: ${(selectedColor.alpha * 100).toInt()}%",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    
                    Slider(
                        value = selectedColor.alpha,
                        onValueChange = { alpha ->
                            selectedColor = selectedColor.copy(alpha = alpha)
                        },
                        valueRange = 0f..1f,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                
                // Preview
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(selectedColor)
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outline,
                            shape = RoundedCornerShape(8.dp)
                        )
                )
                
                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    BubbleButton(
                        onClick = onDismiss,
                        text = "Cancel",
                        modifier = Modifier.weight(1f),
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    BubbleButton(
                        onClick = {
                            onColorSelected(selectedColor)
                            onDismiss()
                        },
                        text = "Select",
                        modifier = Modifier.weight(1f),
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    }
}

private enum class ColorPickerType {
    TEXT,
    BACKGROUND,
    EDGE
}