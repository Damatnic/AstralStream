package com.astralplayer.nextplayer.ui.components

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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties

/**
 * Modern expandable quick settings menu for video player
 * Features bubble-style UI with smooth animations
 */
@Composable
fun ModernQuickSettingsMenu(
    isExpanded: Boolean,
    onToggleExpansion: () -> Unit,
    onDismiss: () -> Unit,
    // Settings state
    currentPlaybackSpeed: Float,
    onPlaybackSpeedChange: (Float) -> Unit,
    currentBrightness: Float,
    onBrightnessChange: (Float) -> Unit,
    currentVolume: Float,
    onVolumeChange: (Float) -> Unit,
    isSubtitlesEnabled: Boolean,
    onSubtitlesToggle: (Boolean) -> Unit,
    selectedAspectRatio: String,
    onAspectRatioChange: (String) -> Unit,
    isAutoRotateEnabled: Boolean,
    onAutoRotateToggle: (Boolean) -> Unit,
    isHapticFeedbackEnabled: Boolean,
    onHapticFeedbackToggle: (Boolean) -> Unit,
    isVolumeBoostEnabled: Boolean,
    onVolumeBoostToggle: (Boolean) -> Unit,
    currentLoopMode: com.astralplayer.nextplayer.data.LoopMode,
    onLoopModeChange: () -> Unit,
    onEnterPiP: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var selectedSetting by remember { mutableStateOf<QuickSetting?>(null) }
    
    Box(modifier = modifier) {
        // Main quick settings button
        FloatingActionButton(
            onClick = onToggleExpansion,
            modifier = Modifier
                .size(56.dp)
                .animateContentSize(),
            containerColor = Color.Black.copy(alpha = 0.8f),
            contentColor = Color.White
        ) {
            Icon(
                imageVector = if (isExpanded) Icons.Default.Close else Icons.Default.Tune,
                contentDescription = if (isExpanded) "Close Settings" else "Quick Settings",
                modifier = Modifier.scale(if (isExpanded) 1.2f else 1.0f)
            )
        }
        
        // Expanded settings menu
        AnimatedVisibility(
            visible = isExpanded,
            enter = scaleIn(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            ) + fadeIn(),
            exit = scaleOut() + fadeOut()
        ) {
            QuickSettingsBubbleMenu(
                onSettingClick = { setting ->
                    selectedSetting = setting
                },
                onDismiss = onDismiss,
                modifier = Modifier.offset(x = (-200).dp, y = (-50).dp)
            )
        }
        
        // Individual setting adjustment bubble
        selectedSetting?.let { setting ->
            SettingAdjustmentBubble(
                setting = setting,
                onDismiss = { selectedSetting = null },
                currentPlaybackSpeed = currentPlaybackSpeed,
                onPlaybackSpeedChange = onPlaybackSpeedChange,
                currentBrightness = currentBrightness,
                onBrightnessChange = onBrightnessChange,
                currentVolume = currentVolume,
                onVolumeChange = onVolumeChange,
                isSubtitlesEnabled = isSubtitlesEnabled,
                onSubtitlesToggle = onSubtitlesToggle,
                selectedAspectRatio = selectedAspectRatio,
                onAspectRatioChange = onAspectRatioChange,
                isAutoRotateEnabled = isAutoRotateEnabled,
                onAutoRotateToggle = onAutoRotateToggle,
                isHapticFeedbackEnabled = isHapticFeedbackEnabled,
                onHapticFeedbackToggle = onHapticFeedbackToggle,
                isVolumeBoostEnabled = isVolumeBoostEnabled,
                onVolumeBoostToggle = onVolumeBoostToggle,
                currentLoopMode = currentLoopMode,
                onLoopModeChange = onLoopModeChange,
                onEnterPiP = onEnterPiP,
                modifier = Modifier.offset(x = (-350).dp, y = (-100).dp)
            )
        }
    }
}

@Composable
private fun QuickSettingsBubbleMenu(
    onSettingClick: (QuickSetting) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val settings = remember {
        listOf(
            QuickSetting.PlaybackSpeed,
            QuickSetting.Brightness,
            QuickSetting.Volume,
            QuickSetting.VolumeBoost,
            QuickSetting.Subtitles,
            QuickSetting.AspectRatio,
            QuickSetting.AutoRotate,
            QuickSetting.HapticFeedback,
            QuickSetting.LoopMode,
            QuickSetting.CodecPacks,
            QuickSetting.SleepTimer,
            QuickSetting.PictureInPicture
        )
    }
    
    Popup(
        onDismissRequest = onDismiss,
        properties = PopupProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Card(
            modifier = modifier
                .width(200.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.Black.copy(alpha = 0.9f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            LazyColumn(
                modifier = Modifier.padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(settings) { setting ->
                    QuickSettingItem(
                        setting = setting,
                        onClick = { onSettingClick(setting) }
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickSettingItem(
    setting: QuickSetting,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = setting.icon,
            contentDescription = setting.title,
            tint = Color.White,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = setting.title,
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.weight(1f))
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = "Expand",
            tint = Color.White.copy(alpha = 0.7f),
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
private fun SettingAdjustmentBubble(
    setting: QuickSetting,
    onDismiss: () -> Unit,
    // All the setting parameters
    currentPlaybackSpeed: Float,
    onPlaybackSpeedChange: (Float) -> Unit,
    currentBrightness: Float,
    onBrightnessChange: (Float) -> Unit,
    currentVolume: Float,
    onVolumeChange: (Float) -> Unit,
    isSubtitlesEnabled: Boolean,
    onSubtitlesToggle: (Boolean) -> Unit,
    selectedAspectRatio: String,
    onAspectRatioChange: (String) -> Unit,
    isAutoRotateEnabled: Boolean,
    onAutoRotateToggle: (Boolean) -> Unit,
    isHapticFeedbackEnabled: Boolean,
    onHapticFeedbackToggle: (Boolean) -> Unit,
    isVolumeBoostEnabled: Boolean,
    onVolumeBoostToggle: (Boolean) -> Unit,
    currentLoopMode: com.astralplayer.nextplayer.data.LoopMode,
    onLoopModeChange: () -> Unit,
    onEnterPiP: () -> Unit,
    modifier: Modifier = Modifier
) {
    Popup(
        onDismissRequest = onDismiss,
        properties = PopupProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Card(
            modifier = modifier
                .width(280.dp)
                .animateContentSize(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.Black.copy(alpha = 0.9f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = setting.icon,
                        contentDescription = setting.title,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = setting.title,
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                
                // Setting-specific content
                when (setting) {
                    QuickSetting.PlaybackSpeed -> PlaybackSpeedControl(
                        currentSpeed = currentPlaybackSpeed,
                        onSpeedChange = onPlaybackSpeedChange
                    )
                    QuickSetting.Brightness -> BrightnessControl(
                        currentBrightness = currentBrightness,
                        onBrightnessChange = onBrightnessChange
                    )
                    QuickSetting.Volume -> VolumeControl(
                        currentVolume = currentVolume,
                        onVolumeChange = onVolumeChange
                    )
                    QuickSetting.Subtitles -> SubtitlesToggleControl(
                        isEnabled = isSubtitlesEnabled,
                        onToggle = onSubtitlesToggle
                    )
                    QuickSetting.AspectRatio -> AspectRatioControl(
                        selectedRatio = selectedAspectRatio,
                        onRatioChange = onAspectRatioChange
                    )
                    QuickSetting.AutoRotate -> AutoRotateToggleControl(
                        isEnabled = isAutoRotateEnabled,
                        onToggle = onAutoRotateToggle
                    )
                    QuickSetting.HapticFeedback -> HapticFeedbackToggleControl(
                        isEnabled = isHapticFeedbackEnabled,
                        onToggle = onHapticFeedbackToggle
                    )
                    QuickSetting.VolumeBoost -> VolumeBoostToggleControl(
                        isEnabled = isVolumeBoostEnabled,
                        onToggle = onVolumeBoostToggle
                    )
                    QuickSetting.LoopMode -> LoopModeControl(
                        currentMode = currentLoopMode,
                        onModeChange = onLoopModeChange
                    )
                    QuickSetting.CodecPacks -> CodecPacksControl()
                    QuickSetting.SleepTimer -> SleepTimerControl()
                    QuickSetting.PictureInPicture -> PictureInPictureControl(onEnterPiP)
                }
            }
        }
    }
}

// Setting control components
@Composable
private fun PlaybackSpeedControl(
    currentSpeed: Float,
    onSpeedChange: (Float) -> Unit
) {
    val speedOptions = listOf(0.25f, 0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
    
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "${currentSpeed}x",
            color = Color.White,
            fontSize = 14.sp
        )
        
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            speedOptions.forEach { speed ->
                FilterChip(
                    onClick = { onSpeedChange(speed) },
                    label = { Text("${speed}x") },
                    selected = speed == currentSpeed,
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = Color.Transparent,
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        labelColor = Color.White,
                        selectedLabelColor = Color.White
                    )
                )
            }
        }
    }
}

@Composable
private fun BrightnessControl(
    currentBrightness: Float,
    onBrightnessChange: (Float) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "${(currentBrightness * 100).toInt()}%",
            color = Color.White,
            fontSize = 14.sp
        )
        
        Slider(
            value = currentBrightness,
            onValueChange = onBrightnessChange,
            valueRange = 0f..1f,
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = Color.White.copy(alpha = 0.3f)
            )
        )
    }
}

@Composable
private fun VolumeControl(
    currentVolume: Float,
    onVolumeChange: (Float) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "${(currentVolume * 100).toInt()}%",
            color = Color.White,
            fontSize = 14.sp
        )
        
        Slider(
            value = currentVolume,
            onValueChange = onVolumeChange,
            valueRange = 0f..1f,
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = Color.White.copy(alpha = 0.3f)
            )
        )
    }
}

@Composable
private fun SubtitlesToggleControl(
    isEnabled: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = if (isEnabled) "Enabled" else "Disabled",
            color = Color.White,
            fontSize = 14.sp
        )
        
        Switch(
            checked = isEnabled,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.primary,
                checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = Color.White.copy(alpha = 0.3f)
            )
        )
    }
}

@Composable
private fun AspectRatioControl(
    selectedRatio: String,
    onRatioChange: (String) -> Unit
) {
    val ratioOptions = listOf("16:9", "4:3", "21:9", "Fit", "Fill", "Stretch")
    
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = selectedRatio,
            color = Color.White,
            fontSize = 14.sp
        )
        
        LazyColumn(
            modifier = Modifier.height(120.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(ratioOptions) { ratio ->
                FilterChip(
                    onClick = { onRatioChange(ratio) },
                    label = { Text(ratio) },
                    selected = ratio == selectedRatio,
                    modifier = Modifier.fillMaxWidth(),
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = Color.Transparent,
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        labelColor = Color.White,
                        selectedLabelColor = Color.White
                    )
                )
            }
        }
    }
}

@Composable
private fun AutoRotateToggleControl(
    isEnabled: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = if (isEnabled) "Enabled" else "Disabled",
            color = Color.White,
            fontSize = 14.sp
        )
        
        Switch(
            checked = isEnabled,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.primary,
                checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = Color.White.copy(alpha = 0.3f)
            )
        )
    }
}

@Composable
private fun HapticFeedbackToggleControl(
    isEnabled: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = if (isEnabled) "Enabled" else "Disabled",
            color = Color.White,
            fontSize = 14.sp
        )
        
        Switch(
            checked = isEnabled,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.primary,
                checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = Color.White.copy(alpha = 0.3f)
            )
        )
    }
}

@Composable
private fun VolumeBoostToggleControl(
    isEnabled: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = if (isEnabled) "Enabled (200%)" else "Disabled",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Uses LoudnessEnhancer for enhanced audio",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 12.sp
                )
            }
            
            Switch(
                checked = isEnabled,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                    checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                    uncheckedThumbColor = Color.White,
                    uncheckedTrackColor = Color.White.copy(alpha = 0.3f)
                )
            )
        }
    }
}

@Composable
private fun PictureInPictureControl(
    onEnterPiP: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Watch in Picture-in-Picture mode",
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
        
        Text(
            text = "Continue watching while using other apps",
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 12.sp
        )
        
        Button(
            onClick = onEnterPiP,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White
            )
        ) {
            Icon(
                imageVector = Icons.Default.PictureInPictureAlt,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Enter PiP")
        }
    }
}

@Composable
private fun LoopModeControl(
    currentMode: com.astralplayer.nextplayer.data.LoopMode,
    onModeChange: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = currentMode.getDisplayName(),
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
        
        Text(
            text = currentMode.getDescription(),
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 12.sp
        )
        
        Button(
            onClick = onModeChange,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White
            )
        ) {
            Icon(
                imageVector = when (currentMode) {
                    com.astralplayer.nextplayer.data.LoopMode.OFF -> Icons.Default.RepeatOn
                    com.astralplayer.nextplayer.data.LoopMode.ONE -> Icons.Default.RepeatOneOn
                    com.astralplayer.nextplayer.data.LoopMode.ALL -> Icons.Default.Repeat
                },
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Toggle Loop Mode")
        }
    }
}

// Data classes
sealed class QuickSetting(
    val title: String,
    val icon: ImageVector
) {
    object PlaybackSpeed : QuickSetting("Playback Speed", Icons.Default.Speed)
    object Brightness : QuickSetting("Brightness", Icons.Default.Brightness6)
    object Volume : QuickSetting("Volume", Icons.Default.VolumeUp)
    object VolumeBoost : QuickSetting("Volume Boost", Icons.Default.VolumeUp)
    object Subtitles : QuickSetting("Subtitles", Icons.Default.Subtitles)
    object AspectRatio : QuickSetting("Aspect Ratio", Icons.Default.AspectRatio)
    object AutoRotate : QuickSetting("Auto Rotate", Icons.Default.ScreenRotation)
    object HapticFeedback : QuickSetting("Haptic Feedback", Icons.Default.Vibration)
    object LoopMode : QuickSetting("Loop Mode", Icons.Default.Repeat)
    object CodecPacks : QuickSetting("Codec Packs", Icons.Default.Code)
    object SleepTimer : QuickSetting("Sleep Timer", Icons.Default.Bedtime)
    object PictureInPicture : QuickSetting("Picture-in-Picture", Icons.Default.PictureInPictureAlt)
}

// Additional control components
@Composable
private fun CodecPacksControl() {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Manage codec packs for enhanced format support",
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 12.sp
        )
        
        Button(
            onClick = { /* Open codec packs dialog */ },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White
            )
        ) {
            Icon(
                imageVector = Icons.Default.Download,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Manage Codec Packs")
        }
    }
}

@Composable
private fun SleepTimerControl() {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Auto-stop playback after specified time",
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 12.sp
        )
        
        Button(
            onClick = { /* Open sleep timer dialog */ },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White
            )
        ) {
            Icon(
                imageVector = Icons.Default.Timer,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Set Sleep Timer")
        }
    }
}

