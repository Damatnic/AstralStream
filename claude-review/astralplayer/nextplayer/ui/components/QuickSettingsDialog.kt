package com.astralplayer.nextplayer.ui.components

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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.launch

@Composable
fun QuickSettingsDialog(
    currentPlaybackSpeed: Float,
    onPlaybackSpeedChange: (Float) -> Unit,
    isAudioBoostEnabled: Boolean,
    onAudioBoostToggle: (Boolean) -> Unit,
    currentAspectRatio: String,
    onAspectRatioChange: (String) -> Unit,
    isAutoRotateEnabled: Boolean,
    onAutoRotateToggle: (Boolean) -> Unit,
    isRememberBrightnessEnabled: Boolean,
    onRememberBrightnessToggle: (Boolean) -> Unit,
    isHapticFeedbackEnabled: Boolean,
    onHapticFeedbackToggle: (Boolean) -> Unit,
    onSubtitleSettings: () -> Unit,
    onAudioTrackSettings: () -> Unit,
    onGestureSettings: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var selectedTab by remember { mutableStateOf(0) }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.8f),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Header
                Text(
                    text = "Quick Settings",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(16.dp)
                )
                
                // Tabs
                TabRow(selectedTabIndex = selectedTab) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Playback") }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Display") }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = { Text("Controls") }
                    )
                }
                
                // Content
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    when (selectedTab) {
                        0 -> PlaybackSettingsTab(
                            currentPlaybackSpeed = currentPlaybackSpeed,
                            onPlaybackSpeedChange = onPlaybackSpeedChange,
                            isAudioBoostEnabled = isAudioBoostEnabled,
                            onAudioBoostToggle = onAudioBoostToggle,
                            onSubtitleSettings = onSubtitleSettings,
                            onAudioTrackSettings = onAudioTrackSettings
                        )
                        1 -> DisplaySettingsTab(
                            currentAspectRatio = currentAspectRatio,
                            onAspectRatioChange = onAspectRatioChange,
                            isAutoRotateEnabled = isAutoRotateEnabled,
                            onAutoRotateToggle = onAutoRotateToggle,
                            isRememberBrightnessEnabled = isRememberBrightnessEnabled,
                            onRememberBrightnessToggle = onRememberBrightnessToggle
                        )
                        2 -> ControlsSettingsTab(
                            isHapticFeedbackEnabled = isHapticFeedbackEnabled,
                            onHapticFeedbackToggle = onHapticFeedbackToggle,
                            onGestureSettings = onGestureSettings
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PlaybackSettingsTab(
    currentPlaybackSpeed: Float,
    onPlaybackSpeedChange: (Float) -> Unit,
    isAudioBoostEnabled: Boolean,
    onAudioBoostToggle: (Boolean) -> Unit,
    onSubtitleSettings: () -> Unit,
    onAudioTrackSettings: () -> Unit
) {
    // Playback Speed
    QuickSettingItem(
        icon = Icons.Default.Speed,
        title = "Playback Speed",
        subtitle = "${currentPlaybackSpeed}x"
    ) {
        val speeds = listOf(0.25f, 0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f)
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            speeds.forEach { speed ->
                FilterChip(
                    onClick = { onPlaybackSpeedChange(speed) },
                    label = { Text("${speed}x") },
                    selected = speed == currentPlaybackSpeed,
                    modifier = Modifier.padding(horizontal = 2.dp)
                )
            }
        }
    }
    
    // Audio Boost
    QuickSettingItem(
        icon = Icons.Default.VolumeUp,
        title = "Audio Boost",
        subtitle = if (isAudioBoostEnabled) "Enabled" else "Disabled"
    ) {
        Switch(
            checked = isAudioBoostEnabled,
            onCheckedChange = onAudioBoostToggle
        )
    }
    
    // Subtitle Settings
    QuickSettingItem(
        icon = Icons.Default.Subtitles,
        title = "Subtitle Settings",
        subtitle = "Font, size, and appearance",
        onClick = onSubtitleSettings
    )
    
    // Audio Track Settings
    QuickSettingItem(
        icon = Icons.Default.AudioFile,
        title = "Audio Track Settings",
        subtitle = "Track selection and audio settings",
        onClick = onAudioTrackSettings
    )
}

@Composable
private fun DisplaySettingsTab(
    currentAspectRatio: String,
    onAspectRatioChange: (String) -> Unit,
    isAutoRotateEnabled: Boolean,
    onAutoRotateToggle: (Boolean) -> Unit,
    isRememberBrightnessEnabled: Boolean,
    onRememberBrightnessToggle: (Boolean) -> Unit
) {
    // Aspect Ratio
    QuickSettingItem(
        icon = Icons.Default.AspectRatio,
        title = "Aspect Ratio",
        subtitle = when(currentAspectRatio) {
            "fit" -> "Fit to screen"
            "fill" -> "Fill screen"
            "16:9" -> "16:9"
            "4:3" -> "4:3"
            else -> currentAspectRatio
        }
    ) {
        Column {
            val ratios = listOf(
                "fit" to "Fit",
                "fill" to "Fill", 
                "16:9" to "16:9",
                "4:3" to "4:3"
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                ratios.forEach { (ratio, label) ->
                    FilterChip(
                        onClick = { onAspectRatioChange(ratio) },
                        label = { Text(label) },
                        selected = ratio == currentAspectRatio,
                        modifier = Modifier.padding(horizontal = 2.dp)
                    )
                }
            }
        }
    }
    
    // Auto Rotate
    QuickSettingItem(
        icon = Icons.Default.ScreenRotation,
        title = "Auto Rotate",
        subtitle = if (isAutoRotateEnabled) "Enabled" else "Disabled"
    ) {
        Switch(
            checked = isAutoRotateEnabled,
            onCheckedChange = onAutoRotateToggle
        )
    }
    
    // Remember Brightness
    QuickSettingItem(
        icon = Icons.Default.Brightness6,
        title = "Remember Brightness",
        subtitle = if (isRememberBrightnessEnabled) "Enabled" else "Disabled"
    ) {
        Switch(
            checked = isRememberBrightnessEnabled,
            onCheckedChange = onRememberBrightnessToggle
        )
    }
}

@Composable
private fun ControlsSettingsTab(
    isHapticFeedbackEnabled: Boolean,
    onHapticFeedbackToggle: (Boolean) -> Unit,
    onGestureSettings: () -> Unit
) {
    // Haptic Feedback
    QuickSettingItem(
        icon = Icons.Default.Vibration,
        title = "Haptic Feedback",
        subtitle = if (isHapticFeedbackEnabled) "Enabled" else "Disabled"
    ) {
        Switch(
            checked = isHapticFeedbackEnabled,
            onCheckedChange = onHapticFeedbackToggle
        )
    }
    
    // Gesture Settings
    QuickSettingItem(
        icon = Icons.Default.TouchApp,
        title = "Gesture Settings",
        subtitle = "Customize touch controls",
        onClick = onGestureSettings
    )
    
    // Quick Access Info
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Quick Controls",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "• Single tap: Show/hide controls\n• Double tap left/right: Seek ±10s\n• Long press: 2x speed\n• Swipe up/down: Volume/brightness\n• Pinch: Zoom",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun QuickSettingItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: (() -> Unit)? = null,
    content: @Composable (() -> Unit)? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) Modifier.clickable { onClick() }
                else Modifier
            )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (onClick != null) {
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            content?.let {
                Spacer(modifier = Modifier.height(8.dp))
                it()
            }
        }
    }
}