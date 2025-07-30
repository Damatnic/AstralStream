package com.astralplayer.nextplayer.feature.player.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.astralplayer.nextplayer.feature.player.viewmodel.PlayerViewModel
import com.astralplayer.nextplayer.feature.player.gestures.LongPressSettings
import com.astralplayer.nextplayer.feature.settings.LongPressSeekSettingsManager
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@Composable
fun QuickSettingsDialog(
    viewModel: PlayerViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settingsManager = remember { LongPressSeekSettingsManager(context) }
    
    var selectedTab by remember { mutableStateOf(0) }
    var longPressSettings by remember { mutableStateOf(LongPressSettings()) }
    
    // Collect long press settings
    LaunchedEffect(Unit) {
        settingsManager.longPressSeekSettings.collectLatest { 
            longPressSettings = it
        }
    }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 600.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.Transparent
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF6B46C1),
                                Color(0xFF7C3AED),
                                Color(0xFF5B21B6)
                            )
                        ),
                        shape = RoundedCornerShape(24.dp)
                    )
                    .animateContentSize()
            ) {
                // Header
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color(0xFF8B5CF6).copy(alpha = 0.3f),
                                    Color(0xFF7C3AED).copy(alpha = 0.2f)
                                )
                            )
                        )
                        .padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Quick Settings",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        IconButton(onClick = onDismiss) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Close",
                                tint = Color.White.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
                
                // Tab Row
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color.Transparent,
                    contentColor = Color.White,
                    indicator = { tabPositions ->
                        TabRowDefaults.Indicator(
                            modifier = Modifier,
                            color = Color(0xFF00D4FF)
                        )
                    }
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Playback") }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Long Press") }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = { Text("Display") }
                    )
                }
                
                // Content
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    when (selectedTab) {
                        0 -> PlaybackSettingsQuick(viewModel)
                        1 -> LongPressQuickSettings(
                            settings = longPressSettings,
                            onSettingsChange = { newSettings ->
                                scope.launch {
                                    settingsManager.updateAllSettings(newSettings)
                                }
                            }
                        )
                        2 -> DisplaySettingsQuick(viewModel)
                    }
                }
            }
        }
    }
}

@Composable
private fun PlaybackSettingsQuick(viewModel: PlayerViewModel) {
    val uiState by viewModel.playerState.collectAsState()
    
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Playback Speed
        SettingRowQuick(
            icon = Icons.Default.Speed,
            title = "Playback Speed",
            subtitle = "${uiState.playbackSpeed}x"
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f).forEach { speed ->
                    FilterChip(
                        selected = uiState.playbackSpeed == speed,
                        onClick = { viewModel.setPlaybackSpeed(speed) },
                        label = { Text("${speed}x") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF00D4FF),
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }
        }
        
        // Loop
        val isLooping by viewModel.isLooping.collectAsState()
        SettingRowQuick(
            icon = Icons.Default.Loop,
            title = "Loop Video",
            subtitle = if (isLooping) "On" else "Off"
        ) {
            Switch(
                checked = isLooping,
                onCheckedChange = { /* TODO: Add toggle looping method */ },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color(0xFF00D4FF),
                    checkedTrackColor = Color(0xFF00D4FF).copy(alpha = 0.5f)
                )
            )
        }
    }
}

@Composable
private fun LongPressQuickSettings(
    settings: LongPressSettings,
    onSettingsChange: (LongPressSettings) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Enable/Disable
        SettingRowQuick(
            icon = Icons.Default.TouchApp,
            title = "Long Press Seek",
            subtitle = if (settings.isEnabled) "Enabled" else "Disabled"
        ) {
            Switch(
                checked = settings.isEnabled,
                onCheckedChange = { enabled ->
                    onSettingsChange(settings.copy(isEnabled = enabled))
                },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color(0xFF00D4FF),
                    checkedTrackColor = Color(0xFF00D4FF).copy(alpha = 0.5f)
                )
            )
        }
        
        // Default Speed
        SettingRowQuick(
            icon = Icons.Default.FastForward,
            title = "Default Speed",
            subtitle = "${settings.defaultSpeed.toInt()}x"
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(2f, 3f, 4f, 5f).forEach { speed ->
                    FilterChip(
                        selected = settings.defaultSpeed == speed,
                        onClick = { 
                            onSettingsChange(settings.copy(defaultSpeed = speed))
                        },
                        label = { Text("${speed.toInt()}x") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF00D4FF),
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }
        }
        
        // Haptic Feedback
        SettingRowQuick(
            icon = Icons.Default.Vibration,
            title = "Haptic Feedback",
            subtitle = if (settings.hapticFeedbackEnabled) "On" else "Off"
        ) {
            Switch(
                checked = settings.hapticFeedbackEnabled,
                onCheckedChange = { enabled ->
                    onSettingsChange(settings.copy(hapticFeedbackEnabled = enabled))
                },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color(0xFF00D4FF),
                    checkedTrackColor = Color(0xFF00D4FF).copy(alpha = 0.5f)
                )
            )
        }
        
        // Show Speed Zones
        SettingRowQuick(
            icon = Icons.Default.ShowChart,
            title = "Speed Zones",
            subtitle = if (settings.showSpeedZones) "Visible" else "Hidden"
        ) {
            Switch(
                checked = settings.showSpeedZones,
                onCheckedChange = { enabled ->
                    onSettingsChange(settings.copy(showSpeedZones = enabled))
                },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color(0xFF00D4FF),
                    checkedTrackColor = Color(0xFF00D4FF).copy(alpha = 0.5f)
                )
            )
        }
    }
}

@Composable
private fun DisplaySettingsQuick(viewModel: PlayerViewModel) {
    val uiState by viewModel.playerState.collectAsState()
    
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Zoom Level
        SettingRowQuick(
            icon = Icons.Default.ZoomIn,
            title = "Zoom Level",
            subtitle = "${(uiState.zoomLevel * 100).toInt()}%"
        ) {
            Slider(
                value = uiState.zoomLevel,
                onValueChange = { viewModel.setZoomLevel(it) },
                valueRange = 1f..3f,
                colors = SliderDefaults.colors(
                    thumbColor = Color(0xFF00D4FF),
                    activeTrackColor = Color(0xFF00D4FF)
                ),
                modifier = Modifier.width(150.dp)
            )
        }
        
        // Rotation
        SettingRowQuick(
            icon = Icons.Default.RotateRight,
            title = "Rotation",
            subtitle = "${uiState.videoRotation.toInt()}°"
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(onClick = { viewModel.rotateVideo(-90f) }) {
                    Icon(Icons.Default.RotateLeft, contentDescription = "Rotate Left")
                }
                IconButton(onClick = { viewModel.rotateVideo(90f) }) {
                    Icon(Icons.Default.RotateRight, contentDescription = "Rotate Right")
                }
            }
        }
    }
}

@Composable
private fun SettingRowQuick(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    content: @Composable () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color(0xFF00D4FF),
                modifier = Modifier.size(24.dp)
            )
            Column {
                Text(
                    text = title,
                    fontWeight = FontWeight.Medium,
                    color = Color.White
                )
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.6f)
                )
            }
        }
        content()
    }
}