package com.astralplayer.nextplayer.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.astralplayer.nextplayer.data.repository.SettingsRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GestureSettingsScreen(
    settingsRepository: SettingsRepository,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    
    // Gesture settings state
    var swipeToSeekEnabled by remember { mutableStateOf(true) }
    var doubleTapToSeekEnabled by remember { mutableStateOf(true) }
    var longPressToSeekEnabled by remember { mutableStateOf(true) }
    var longPressSeekSpeed by remember { mutableStateOf(2.0f) }
    var pinchToZoomEnabled by remember { mutableStateOf(true) }
    var swipeSensitivity by remember { mutableStateOf(1.0f) }
    
    // Load initial values
    LaunchedEffect(Unit) {
        swipeToSeekEnabled = settingsRepository.getSwipeToSeekEnabled().first()
        doubleTapToSeekEnabled = settingsRepository.getDoubleTapToSeekEnabled().first()
        longPressToSeekEnabled = settingsRepository.getLongPressToSeekEnabled().first()
        longPressSeekSpeed = settingsRepository.getLongPressSeekSpeed().first()
        pinchToZoomEnabled = settingsRepository.getPinchToZoomEnabled().first()
        swipeSensitivity = settingsRepository.getSwipeSensitivity().first()
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Gesture Controls") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        modifier = modifier
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Gesture preview
            item {
                GesturePreviewCard()
            }
            
            // Swipe controls
            item {
                Text(
                    text = "Swipe Gestures",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            item {
                SwitchSettingItem(
                    icon = Icons.Default.SwipeVertical,
                    title = "Swipe to adjust brightness/volume",
                    subtitle = "Swipe up/down on left/right side",
                    checked = swipeToSeekEnabled,
                    onCheckedChange = { enabled ->
                        swipeToSeekEnabled = enabled
                        coroutineScope.launch {
                            settingsRepository.setSwipeToSeekEnabled(enabled)
                        }
                    }
                )
            }
            
            item {
                SliderSettingItem(
                    icon = Icons.Default.Speed,
                    title = "Swipe sensitivity",
                    subtitle = "Adjust swipe gesture sensitivity",
                    value = swipeSensitivity,
                    onValueChange = { value ->
                        swipeSensitivity = value
                        coroutineScope.launch {
                            settingsRepository.setSwipeSensitivity(value)
                        }
                    },
                    enabled = swipeToSeekEnabled
                )
            }
            
            // Tap controls
            item {
                HorizontalDivider()
                Text(
                    text = "Tap Gestures",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            
            item {
                SwitchSettingItem(
                    icon = Icons.Default.TouchApp,
                    title = "Double tap to seek",
                    subtitle = "Double tap left/right to seek 10s",
                    checked = doubleTapToSeekEnabled,
                    onCheckedChange = { enabled ->
                        doubleTapToSeekEnabled = enabled
                        coroutineScope.launch {
                            settingsRepository.setDoubleTapToSeekEnabled(enabled)
                        }
                    }
                )
            }
            
            item {
                SwitchSettingItem(
                    icon = Icons.Default.PanTool,
                    title = "Long press to seek",
                    subtitle = "Hold to seek forward/backward at adjustable speed",
                    checked = longPressToSeekEnabled,
                    onCheckedChange = { enabled ->
                        longPressToSeekEnabled = enabled
                        coroutineScope.launch {
                            settingsRepository.setLongPressToSeekEnabled(enabled)
                        }
                    }
                )
            }
            
            item {
                SliderSettingItem(
                    icon = Icons.Default.Speed,
                    title = "Long press seek speed",
                    subtitle = "Speed multiplier for long press seeking (${String.format("%.1f", longPressSeekSpeed)}x)",
                    value = longPressSeekSpeed / 5.0f, // Normalize to 0-1 range
                    enabled = longPressToSeekEnabled,
                    onValueChange = { normalizedValue ->
                        longPressSeekSpeed = 1.0f + (normalizedValue * 4.0f) // Convert back to 1-5 range
                        coroutineScope.launch {
                            settingsRepository.setLongPressSeekSpeed(longPressSeekSpeed)
                        }
                    }
                )
            }
            
            // Pinch controls
            item {
                HorizontalDivider()
                Text(
                    text = "Pinch Gestures",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            
            item {
                SwitchSettingItem(
                    icon = Icons.Default.ZoomIn,
                    title = "Pinch to zoom",
                    subtitle = "Pinch to zoom in/out video",
                    checked = pinchToZoomEnabled,
                    onCheckedChange = { enabled ->
                        pinchToZoomEnabled = enabled
                        coroutineScope.launch {
                            settingsRepository.setPinchToZoomEnabled(enabled)
                        }
                    }
                )
            }
            
            // Additional gesture options
            item {
                HorizontalDivider()
                Text(
                    text = "Additional Options",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            
            item {
                InfoCard(
                    text = "Long press on video to play at 2x speed. Release to resume normal speed."
                )
            }
            
            item {
                InfoCard(
                    text = "Swipe horizontally anywhere to seek forward/backward."
                )
            }
        }
    }
}

@Composable
private fun GesturePreviewCard(
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .padding(16.dp)
        ) {
            // Video preview placeholder
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                // Draw gesture indicators
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeWidth = 3.dp.toPx()
                    val dashEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                    
                    // Left side brightness indicator
                    drawLine(
                        color = Color.White.copy(alpha = 0.5f),
                        start = Offset(size.width * 0.25f, size.height * 0.2f),
                        end = Offset(size.width * 0.25f, size.height * 0.8f),
                        strokeWidth = strokeWidth,
                        pathEffect = dashEffect
                    )
                    
                    // Right side volume indicator
                    drawLine(
                        color = Color.White.copy(alpha = 0.5f),
                        start = Offset(size.width * 0.75f, size.height * 0.2f),
                        end = Offset(size.width * 0.75f, size.height * 0.8f),
                        strokeWidth = strokeWidth,
                        pathEffect = dashEffect
                    )
                    
                    // Double tap areas
                    val path = Path().apply {
                        // Left double tap area
                        moveTo(size.width * 0.1f, size.height * 0.3f)
                        lineTo(size.width * 0.1f, size.height * 0.7f)
                        lineTo(size.width * 0.35f, size.height * 0.7f)
                        lineTo(size.width * 0.35f, size.height * 0.3f)
                        close()
                    }
                    
                    drawPath(
                        path = path,
                        color = Color.White.copy(alpha = 0.3f),
                        style = Stroke(
                            width = strokeWidth,
                            pathEffect = dashEffect
                        )
                    )
                }
                
                // Icons overlay
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Brightness6,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.size(24.dp)
                        )
                        Icon(
                            imageVector = Icons.Default.FastRewind,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.size(24.dp)
                        )
                        Icon(
                            imageVector = Icons.Default.FastForward,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SwitchSettingItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp
                )
            }
            
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange
            )
        }
    }
}

@Composable
private fun SliderSettingItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = if (enabled) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                }
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (enabled) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    }
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (enabled) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    },
                    fontSize = 14.sp
                )
            }
        }
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 40.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Low",
                style = MaterialTheme.typography.bodySmall,
                color = if (enabled) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                }
            )
            
            Slider(
                value = value,
                onValueChange = onValueChange,
                valueRange = 0.5f..2.0f,
                enabled = enabled,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp)
            )
            
            Text(
                text = "High",
                style = MaterialTheme.typography.bodySmall,
                color = if (enabled) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                }
            )
        }
    }
}

@Composable
private fun InfoCard(
    text: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}