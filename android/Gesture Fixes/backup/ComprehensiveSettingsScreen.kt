package com.astralplayer.nextplayer.feature.settings

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/**
 * Comprehensive settings screen with all advanced options
 * Better than any other Android video player
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComprehensiveSettingsScreen(
    isVisible: Boolean,
    settingsManager: ComprehensiveSettingsManager,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedCategory by remember { mutableStateOf(SettingsCategory.PLAYBACK) }
    var showResetDialog by remember { mutableStateOf(false) }
    
    if (isVisible) {
        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                dismissOnBackPress = true,
                dismissOnClickOutside = true
            )
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Row(modifier = Modifier.fillMaxSize()) {
                    // Categories sidebar
                    SettingsCategorySidebar(
                        selectedCategory = selectedCategory,
                        onCategorySelected = { selectedCategory = it },
                        onResetAll = { showResetDialog = true },
                        onClose = onDismiss,
                        modifier = Modifier
                            .width(200.dp)
                            .fillMaxHeight()
                    )
                    
                    // Settings content
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .padding(start = 16.dp)
                    ) {
                        when (selectedCategory) {
                            SettingsCategory.PLAYBACK -> PlaybackSettingsContent(settingsManager)
                            SettingsCategory.GESTURES -> GestureSettingsContent(settingsManager)
                            SettingsCategory.UI -> UISettingsContent(settingsManager)
                            SettingsCategory.AUDIO -> AudioSettingsContent(settingsManager)
                            SettingsCategory.VIDEO -> VideoSettingsContent(settingsManager)
                            SettingsCategory.SUBTITLES -> SubtitleSettingsContent(settingsManager)
                            SettingsCategory.ADVANCED -> AdvancedSettingsContent(settingsManager)
                        }
                    }
                }
            }
        }
    }
    
    // Reset confirmation dialog
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Reset All Settings") },
            text = { Text("This will reset all settings to their default values. This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        settingsManager.resetAllSettings()
                        showResetDialog = false
                    }
                ) {
                    Text("Reset", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun SettingsCategorySidebar(
    selectedCategory: SettingsCategory,
    onCategorySelected: (SettingsCategory) -> Unit,
    onResetAll: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Settings",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Categories
            val categories = listOf(
                SettingsCategory.PLAYBACK to CategoryInfo("Playback", Icons.Default.PlayArrow),
                SettingsCategory.GESTURES to CategoryInfo("Gestures", Icons.Default.TouchApp),
                SettingsCategory.UI to CategoryInfo("Interface", Icons.Default.Dashboard),
                SettingsCategory.AUDIO to CategoryInfo("Audio", Icons.Default.VolumeUp),
                SettingsCategory.VIDEO to CategoryInfo("Video", Icons.Default.VideoSettings),
                SettingsCategory.SUBTITLES to CategoryInfo("Subtitles", Icons.Default.Subtitles),
                SettingsCategory.ADVANCED to CategoryInfo("Advanced", Icons.Default.Settings)
            )
            
            categories.forEach { (category, info) ->
                CategoryItem(
                    title = info.title,
                    icon = info.icon,
                    isSelected = selectedCategory == category,
                    onClick = { onCategorySelected(category) }
                )
                Spacer(modifier = Modifier.height(4.dp))
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Reset button
            OutlinedButton(
                onClick = onResetAll,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Icon(Icons.Default.RestartAlt, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Reset All")
            }
        }
    }
}

@Composable
private fun CategoryItem(
    title: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) 
                MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
            else 
                Color.Transparent
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                icon,
                contentDescription = title,
                tint = if (isSelected) 
                    MaterialTheme.colorScheme.primary 
                else 
                    MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Text(
                text = title,
                fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
                color = if (isSelected) 
                    MaterialTheme.colorScheme.primary 
                else 
                    MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// Settings content composables
@Composable
private fun PlaybackSettingsContent(settingsManager: ComprehensiveSettingsManager) {
    val playbackSettings by settingsManager.playbackSettings.collectAsState()
    
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(16.dp)
    ) {
        item {
            Text(
                text = "Playback Settings",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        }
        
        item {
            SwitchSetting(
                title = "Auto Play",
                description = "Automatically start playing when video opens",
                checked = playbackSettings.autoPlay,
                onCheckedChange = { 
                    settingsManager.updatePlaybackSettings(playbackSettings.copy(autoPlay = it))
                }
            )
        }
        
        item {
            SwitchSetting(
                title = "Resume Playback",
                description = "Remember position for previously watched videos",
                checked = playbackSettings.resumePlayback,
                onCheckedChange = { 
                    settingsManager.updatePlaybackSettings(playbackSettings.copy(resumePlayback = it))
                }
            )
        }
        
        item {
            SliderSetting(
                title = "Default Playback Speed",
                description = "Set default speed for new videos",
                value = playbackSettings.defaultPlaybackSpeed,
                valueRange = 0.25f..3.0f,
                steps = 10,
                valueFormatter = { "${it}x" },
                onValueChange = { 
                    settingsManager.updatePlaybackSettings(playbackSettings.copy(defaultPlaybackSpeed = it))
                }
            )
        }
        
        item {
            SwitchSetting(
                title = "Skip Intro/Outro",
                description = "Automatically skip intro and outro sections",
                checked = playbackSettings.skipIntroOutro,
                onCheckedChange = { 
                    settingsManager.updatePlaybackSettings(playbackSettings.copy(skipIntroOutro = it))
                }
            )
        }
        
        item {
            SwitchSetting(
                title = "Loop Video",
                description = "Repeat video when it reaches the end",
                checked = playbackSettings.loopVideo,
                onCheckedChange = { 
                    settingsManager.updatePlaybackSettings(playbackSettings.copy(loopVideo = it))
                }
            )
        }
        
        item {
            SwitchSetting(
                title = "Background Playback",
                description = "Continue playing when app is in background",
                checked = playbackSettings.backgroundPlayback,
                onCheckedChange = { 
                    settingsManager.updatePlaybackSettings(playbackSettings.copy(backgroundPlayback = it))
                }
            )
        }
        
        item {
            SwitchSetting(
                title = "Picture-in-Picture Mode",
                description = "Enable PiP mode support",
                checked = playbackSettings.pipMode,
                onCheckedChange = { 
                    settingsManager.updatePlaybackSettings(playbackSettings.copy(pipMode = it))
                }
            )
        }
    }
}

@Composable
private fun GestureSettingsContent(settingsManager: ComprehensiveSettingsManager) {
    val gestureSettings by settingsManager.gestureSettings.collectAsState()
    
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(16.dp)
    ) {
        item {
            Text(
                text = "Gesture Settings",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        }
        
        item {
            SwitchSetting(
                title = "Enable Gestures",
                description = "Master switch for all gesture controls",
                checked = gestureSettings.gesturesEnabled,
                onCheckedChange = { 
                    settingsManager.updateGestureSettings(gestureSettings.copy(gesturesEnabled = it))
                }
            )
        }
        
        item {
            SwitchSetting(
                title = "Double Tap to Seek",
                description = "Seek forward/backward with double tap",
                checked = gestureSettings.doubleTapToSeek,
                enabled = gestureSettings.gesturesEnabled,
                onCheckedChange = { 
                    settingsManager.updateGestureSettings(gestureSettings.copy(doubleTapToSeek = it))
                }
            )
        }
        
        item {
            SliderSetting(
                title = "Seek Amount",
                description = "Seconds to seek with double tap",
                value = gestureSettings.seekAmount.toFloat(),
                valueRange = 5f..60f,
                steps = 11,
                valueFormatter = { "${it.toInt()}s" },
                enabled = gestureSettings.gesturesEnabled && gestureSettings.doubleTapToSeek,
                onValueChange = { 
                    settingsManager.updateGestureSettings(gestureSettings.copy(seekAmount = it.toInt()))
                }
            )
        }
        
        item {
            SwitchSetting(
                title = "Volume Gesture",
                description = "Swipe vertically on right to adjust volume",
                checked = gestureSettings.volumeGesture,
                enabled = gestureSettings.gesturesEnabled,
                onCheckedChange = { 
                    settingsManager.updateGestureSettings(gestureSettings.copy(volumeGesture = it))
                }
            )
        }
        
        item {
            SwitchSetting(
                title = "Brightness Gesture",
                description = "Swipe vertically on left to adjust brightness",
                checked = gestureSettings.brightnessGesture,
                enabled = gestureSettings.gesturesEnabled,
                onCheckedChange = { 
                    settingsManager.updateGestureSettings(gestureSettings.copy(brightnessGesture = it))
                }
            )
        }
        
        item {
            SwitchSetting(
                title = "Long Press Speed Control",
                description = "Hold and drag for variable speed seeking",
                checked = gestureSettings.longPressSpeed,
                enabled = gestureSettings.gesturesEnabled,
                onCheckedChange = { 
                    settingsManager.updateGestureSettings(gestureSettings.copy(longPressSpeed = it))
                }
            )
        }
        
        item {
            SwitchSetting(
                title = "Pinch to Zoom",
                description = "Zoom in/out with pinch gesture",
                checked = gestureSettings.pinchToZoom,
                enabled = gestureSettings.gesturesEnabled,
                onCheckedChange = { 
                    settingsManager.updateGestureSettings(gestureSettings.copy(pinchToZoom = it))
                }
            )
        }
        
        item {
            SliderSetting(
                title = "Gesture Sensitivity",
                description = "Adjust sensitivity of all gestures",
                value = gestureSettings.gestureSensitivity,
                valueRange = 0.1f..3.0f,
                steps = 28,
                valueFormatter = { "${(it * 100).toInt()}%" },
                enabled = gestureSettings.gesturesEnabled,
                onValueChange = { 
                    settingsManager.updateGestureSettings(gestureSettings.copy(gestureSensitivity = it))
                }
            )
        }
        
        item {
            SwitchSetting(
                title = "Haptic Feedback",
                description = "Vibrate on gesture actions",
                checked = gestureSettings.hapticFeedback,
                enabled = gestureSettings.gesturesEnabled,
                onCheckedChange = { 
                    settingsManager.updateGestureSettings(gestureSettings.copy(hapticFeedback = it))
                }
            )
        }
    }
}

@Composable
private fun UISettingsContent(settingsManager: ComprehensiveSettingsManager) {
    val uiSettings by settingsManager.uiSettings.collectAsState()
    
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(16.dp)
    ) {
        item {
            Text(
                text = "Interface Settings",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        }
        
        item {
            DropdownSetting(
                title = "App Theme",
                description = "Choose app appearance",
                selectedValue = uiSettings.theme.name,
                options = Theme.values().map { it.name to it.name.lowercase().replaceFirstChar { char -> char.uppercase() } },
                onValueSelected = { value ->
                    settingsManager.updateUISettings(uiSettings.copy(theme = Theme.valueOf(value)))
                }
            )
        }
        
        item {
            DropdownSetting(
                title = "Player Theme",
                description = "Choose player interface theme",
                selectedValue = uiSettings.playerTheme.name,
                options = PlayerTheme.values().map { it.name to it.name.lowercase().replaceFirstChar { char -> char.uppercase() } },
                onValueSelected = { value ->
                    settingsManager.updateUISettings(uiSettings.copy(playerTheme = PlayerTheme.valueOf(value)))
                }
            )
        }
        
        item {
            SwitchSetting(
                title = "Immersive Mode",
                description = "Hide status and navigation bars in player",
                checked = uiSettings.immersiveMode,
                onCheckedChange = { 
                    settingsManager.updateUISettings(uiSettings.copy(immersiveMode = it))
                }
            )
        }
        
        item {
            SliderSetting(
                title = "Controls Timeout",
                description = "Seconds before controls auto-hide",
                value = uiSettings.controlsTimeout.toFloat(),
                valueRange = 2f..15f,
                steps = 12,
                valueFormatter = { "${it.toInt()}s" },
                onValueChange = { 
                    settingsManager.updateUISettings(uiSettings.copy(controlsTimeout = it.toInt()))
                }
            )
        }
        
        item {
            SwitchSetting(
                title = "Progress Thumbnails",
                description = "Show thumbnail previews when seeking",
                checked = uiSettings.showProgressThumbnails,
                onCheckedChange = { 
                    settingsManager.updateUISettings(uiSettings.copy(showProgressThumbnails = it))
                }
            )
        }
        
        item {
            DropdownSetting(
                title = "Controls Layout",
                description = "Choose player controls layout",
                selectedValue = uiSettings.playerControlsLayout.name,
                options = ControlsLayout.values().map { it.name to it.name.lowercase().replaceFirstChar { char -> char.uppercase() } },
                onValueSelected = { value ->
                    settingsManager.updateUISettings(uiSettings.copy(playerControlsLayout = ControlsLayout.valueOf(value)))
                }
            )
        }
    }
}

@Composable
private fun AudioSettingsContent(settingsManager: ComprehensiveSettingsManager) {
    val audioSettings by settingsManager.audioSettings.collectAsState()
    
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(16.dp)
    ) {
        item {
            Text(
                text = "Audio Settings",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        }
        
        item {
            SwitchSetting(
                title = "Audio Boost",
                description = "Enhance audio volume beyond system maximum",
                checked = audioSettings.audioBoost,
                onCheckedChange = { 
                    settingsManager.updateAudioSettings(audioSettings.copy(audioBoost = it))
                }
            )
        }
        
        item {
            SliderSetting(
                title = "Volume Boost Level",
                description = "Maximum volume boost multiplier",
                value = audioSettings.volumeBoostLevel,
                valueRange = 1.0f..3.0f,
                steps = 19,
                valueFormatter = { "${(it * 100).toInt()}%" },
                enabled = audioSettings.audioBoost,
                onValueChange = { 
                    settingsManager.updateAudioSettings(audioSettings.copy(volumeBoostLevel = it))
                }
            )
        }
        
        item {
            SwitchSetting(
                title = "Equalizer",
                description = "Enable audio equalizer",
                checked = audioSettings.equalizerEnabled,
                onCheckedChange = { 
                    settingsManager.updateAudioSettings(audioSettings.copy(equalizerEnabled = it))
                }
            )
        }
        
        item {
            SliderSetting(
                title = "Bass Boost",
                description = "Enhance low frequency sounds",
                value = audioSettings.bassBoost,
                valueRange = 0f..1000f,
                steps = 19,
                valueFormatter = { "${it.toInt()}" },
                enabled = audioSettings.equalizerEnabled,
                onValueChange = { 
                    settingsManager.updateAudioSettings(audioSettings.copy(bassBoost = it))
                }
            )
        }
        
        item {
            SliderSetting(
                title = "Treble Boost",
                description = "Enhance high frequency sounds",
                value = audioSettings.trebleBoost,
                valueRange = 0f..1000f,
                steps = 19,
                valueFormatter = { "${it.toInt()}" },
                enabled = audioSettings.equalizerEnabled,
                onValueChange = { 
                    settingsManager.updateAudioSettings(audioSettings.copy(trebleBoost = it))
                }
            )
        }
        
        item {
            SwitchSetting(
                title = "Night Mode",
                description = "Reduce volume variations for nighttime viewing",
                checked = audioSettings.nightMode,
                onCheckedChange = { 
                    settingsManager.updateAudioSettings(audioSettings.copy(nightMode = it))
                }
            )
        }
        
        item {
            DropdownSetting(
                title = "Audio Channels",
                description = "Audio channel configuration",
                selectedValue = audioSettings.audioChannels.name,
                options = AudioChannels.values().map { it.name to it.name.lowercase().replaceFirstChar { char -> char.uppercase() } },
                onValueSelected = { value ->
                    settingsManager.updateAudioSettings(audioSettings.copy(audioChannels = AudioChannels.valueOf(value)))
                }
            )
        }
    }
}

@Composable
private fun VideoSettingsContent(settingsManager: ComprehensiveSettingsManager) {
    val videoSettings by settingsManager.videoSettings.collectAsState()
    
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(16.dp)
    ) {
        item {
            Text(
                text = "Video Settings",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        }
        
        item {
            DropdownSetting(
                title = "Default Quality",
                description = "Preferred video quality for playback",
                selectedValue = videoSettings.defaultQuality.name,
                options = VideoQuality.values().map { 
                    it.name to when(it) {
                        VideoQuality.AUTO -> "Auto"
                        VideoQuality.SD_480P -> "480p"
                        VideoQuality.HD_720P -> "720p"
                        VideoQuality.FULL_HD_1080P -> "1080p"
                        VideoQuality.UHD_4K -> "4K"
                    }
                },
                onValueSelected = { value ->
                    settingsManager.updateVideoSettings(videoSettings.copy(defaultQuality = VideoQuality.valueOf(value)))
                }
            )
        }
        
        item {
            SwitchSetting(
                title = "Hardware Acceleration",
                description = "Use GPU for video decoding (recommended)",
                checked = videoSettings.hardwareAcceleration,
                onCheckedChange = { 
                    settingsManager.updateVideoSettings(videoSettings.copy(hardwareAcceleration = it))
                }
            )
        }
        
        item {
            DropdownSetting(
                title = "Aspect Ratio",
                description = "How video fits the screen",
                selectedValue = videoSettings.aspectRatio.name,
                options = AspectRatio.values().map { it.name to it.name.lowercase().replaceFirstChar { char -> char.uppercase() } },
                onValueSelected = { value ->
                    settingsManager.updateVideoSettings(videoSettings.copy(aspectRatio = AspectRatio.valueOf(value)))
                }
            )
        }
        
        item {
            SwitchSetting(
                title = "HDR Mode",
                description = "Enable High Dynamic Range support",
                checked = videoSettings.hdrMode,
                onCheckedChange = { 
                    settingsManager.updateVideoSettings(videoSettings.copy(hdrMode = it))
                }
            )
        }
        
        item {
            SwitchSetting(
                title = "Video Filters",
                description = "Enable post-processing video filters",
                checked = videoSettings.videoFilters,
                onCheckedChange = { 
                    settingsManager.updateVideoSettings(videoSettings.copy(videoFilters = it))
                }
            )
        }
        
        item {
            SliderSetting(
                title = "Color Enhancement",
                description = "Boost color saturation",
                value = videoSettings.colorEnhancement,
                valueRange = 0.5f..2.0f,
                steps = 14,
                valueFormatter = { "${(it * 100).toInt()}%" },
                enabled = videoSettings.videoFilters,
                onValueChange = { 
                    settingsManager.updateVideoSettings(videoSettings.copy(colorEnhancement = it))
                }
            )
        }
        
        item {
            SliderSetting(
                title = "Brightness Adjustment",
                description = "Adjust video brightness",
                value = videoSettings.brightnessAdjustment,
                valueRange = -0.5f..0.5f,
                steps = 19,
                valueFormatter = { "${(it * 100).toInt()}" },
                enabled = videoSettings.videoFilters,
                onValueChange = { 
                    settingsManager.updateVideoSettings(videoSettings.copy(brightnessAdjustment = it))
                }
            )
        }
        
        item {
            SliderSetting(
                title = "Contrast Adjustment",
                description = "Adjust video contrast",
                value = videoSettings.contrastAdjustment,
                valueRange = 0.5f..2.0f,
                steps = 14,
                valueFormatter = { "${(it * 100).toInt()}%" },
                enabled = videoSettings.videoFilters,
                onValueChange = { 
                    settingsManager.updateVideoSettings(videoSettings.copy(contrastAdjustment = it))
                }
            )
        }
    }
}

@Composable
private fun SubtitleSettingsContent(settingsManager: ComprehensiveSettingsManager) {
    val subtitleSettings by settingsManager.subtitleSettings.collectAsState()
    
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(16.dp)
    ) {
        item {
            Text(
                text = "Subtitle Settings",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        }
        
        item {
            SwitchSetting(
                title = "Enable Subtitles",
                description = "Show subtitles when available",
                checked = subtitleSettings.subtitlesEnabled,
                onCheckedChange = { 
                    settingsManager.updateSubtitleSettings(subtitleSettings.copy(subtitlesEnabled = it))
                }
            )
        }
        
        item {
            SliderSetting(
                title = "Font Size",
                description = "Size of subtitle text",
                value = subtitleSettings.fontSize,
                valueRange = 10f..36f,
                steps = 25,
                valueFormatter = { "${it.toInt()}sp" },
                enabled = subtitleSettings.subtitlesEnabled,
                onValueChange = { 
                    settingsManager.updateSubtitleSettings(subtitleSettings.copy(fontSize = it))
                }
            )
        }
        
        item {
            DropdownSetting(
                title = "Position",
                description = "Where subtitles appear on screen",
                selectedValue = subtitleSettings.position.name,
                options = SubtitlePosition.values().map { it.name to it.name.lowercase().replaceFirstChar { char -> char.uppercase() } },
                enabled = subtitleSettings.subtitlesEnabled,
                onValueSelected = { value ->
                    settingsManager.updateSubtitleSettings(subtitleSettings.copy(position = SubtitlePosition.valueOf(value)))
                }
            )
        }
        
        item {
            SwitchSetting(
                title = "Auto Load Subtitles",
                description = "Automatically load subtitle files",
                checked = subtitleSettings.autoLoadSubtitles,
                enabled = subtitleSettings.subtitlesEnabled,
                onCheckedChange = { 
                    settingsManager.updateSubtitleSettings(subtitleSettings.copy(autoLoadSubtitles = it))
                }
            )
        }
        
        item {
            SliderSetting(
                title = "Outline Width",
                description = "Thickness of subtitle outline",
                value = subtitleSettings.outlineWidth,
                valueRange = 0f..5f,
                steps = 9,
                valueFormatter = { "${it.toInt()}px" },
                enabled = subtitleSettings.subtitlesEnabled,
                onValueChange = { 
                    settingsManager.updateSubtitleSettings(subtitleSettings.copy(outlineWidth = it))
                }
            )
        }
        
        item {
            SwitchSetting(
                title = "Drop Shadow",
                description = "Add shadow behind subtitle text",
                checked = subtitleSettings.shadow,
                enabled = subtitleSettings.subtitlesEnabled,
                onCheckedChange = { 
                    settingsManager.updateSubtitleSettings(subtitleSettings.copy(shadow = it))
                }
            )
        }
    }
}

@Composable
private fun AdvancedSettingsContent(settingsManager: ComprehensiveSettingsManager) {
    val advancedSettings by settingsManager.advancedSettings.collectAsState()
    
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(16.dp)
    ) {
        item {
            Text(
                text = "Advanced Settings",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        }
        
        item {
            SliderSetting(
                title = "Buffer Size",
                description = "Amount of video to buffer in seconds",
                value = advancedSettings.bufferSize.toFloat(),
                valueRange = 1000f..30000f,
                steps = 28,
                valueFormatter = { "${(it / 1000).toInt()}s" },
                onValueChange = { 
                    settingsManager.updateAdvancedSettings(advancedSettings.copy(bufferSize = it.toInt()))
                }
            )
        }
        
        item {
            SliderSetting(
                title = "Network Timeout",
                description = "Seconds to wait for network response",
                value = advancedSettings.networkTimeout.toFloat(),
                valueRange = 10f..120f,
                steps = 21,
                valueFormatter = { "${it.toInt()}s" },
                onValueChange = { 
                    settingsManager.updateAdvancedSettings(advancedSettings.copy(networkTimeout = it.toInt()))
                }
            )
        }
        
        item {
            SliderSetting(
                title = "Cache Size",
                description = "Maximum cache size in MB",
                value = advancedSettings.cacheSize.toFloat(),
                valueRange = 50f..1000f,
                steps = 18,
                valueFormatter = { "${it.toInt()}MB" },
                onValueChange = { 
                    settingsManager.updateAdvancedSettings(advancedSettings.copy(cacheSize = it.toInt()))
                }
            )
        }
        
        item {
            SwitchSetting(
                title = "Performance Mode",
                description = "Optimize for maximum performance",
                checked = advancedSettings.performanceMode,
                onCheckedChange = { 
                    settingsManager.updateAdvancedSettings(advancedSettings.copy(performanceMode = it))
                }
            )
        }
        
        item {
            SwitchSetting(
                title = "Battery Optimization",
                description = "Reduce power consumption",
                checked = advancedSettings.batteryOptimization,
                onCheckedChange = { 
                    settingsManager.updateAdvancedSettings(advancedSettings.copy(batteryOptimization = it))
                }
            )
        }
        
        item {
            SwitchSetting(
                title = "Preload Next Video",
                description = "Start loading next video in playlist",
                checked = advancedSettings.preloadNext,
                onCheckedChange = { 
                    settingsManager.updateAdvancedSettings(advancedSettings.copy(preloadNext = it))
                }
            )
        }
        
        item {
            SwitchSetting(
                title = "Thumbnail Cache",
                description = "Cache video thumbnails for faster loading",
                checked = advancedSettings.thumbnailCache,
                onCheckedChange = { 
                    settingsManager.updateAdvancedSettings(advancedSettings.copy(thumbnailCache = it))
                }
            )
        }
        
        item {
            SwitchSetting(
                title = "Debug Mode",
                description = "Show advanced debugging information",
                checked = advancedSettings.debugMode,
                onCheckedChange = { 
                    settingsManager.updateAdvancedSettings(advancedSettings.copy(debugMode = it))
                }
            )
        }
        
        item {
            SwitchSetting(
                title = "Statistics",
                description = "Enable performance statistics collection",
                checked = advancedSettings.statisticsEnabled,
                onCheckedChange = { 
                    settingsManager.updateAdvancedSettings(advancedSettings.copy(statisticsEnabled = it))
                }
            )
        }
    }
}

// Helper composables for different setting types
@Composable
private fun SwitchSetting(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Medium,
                    color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
            
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                enabled = enabled
            )
        }
    }
}

@Composable
private fun SliderSetting(
    title: String,
    description: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    valueFormatter: (Float) -> String,
    onValueChange: (Float) -> Unit,
    enabled: Boolean = true
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = title,
                        fontWeight = FontWeight.Medium,
                        color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
                
                Text(
                    text = valueFormatter(value),
                    fontWeight = FontWeight.Medium,
                    color = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Slider(
                value = value,
                onValueChange = onValueChange,
                valueRange = valueRange,
                steps = steps,
                enabled = enabled
            )
        }
    }
}

@Composable
private fun DropdownSetting(
    title: String,
    description: String,
    selectedValue: String,
    options: List<Pair<String, String>>, // value to display text
    onValueSelected: (String) -> Unit,
    enabled: Boolean = true
) {
    var expanded by remember { mutableStateOf(false) }
    
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = enabled) { expanded = true },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = title,
                        fontWeight = FontWeight.Medium,
                        color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = options.find { it.first == selectedValue }?.second ?: selectedValue,
                        fontWeight = FontWeight.Medium,
                        color = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                    )
                    
                    Icon(
                        Icons.Default.ExpandMore,
                        contentDescription = "Expand",
                        tint = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }
            
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                options.forEach { (value, displayText) ->
                    DropdownMenuItem(
                        text = { Text(displayText) },
                        onClick = {
                            onValueSelected(value)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

// Data class for category info
data class CategoryInfo(
    val title: String,
    val icon: ImageVector
)