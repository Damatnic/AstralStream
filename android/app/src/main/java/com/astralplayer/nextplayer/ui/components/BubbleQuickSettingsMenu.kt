package com.astralplayer.nextplayer.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.zIndex
import kotlinx.coroutines.delay
import kotlin.time.Duration
import com.astralplayer.nextplayer.data.SubtitleManager.SubtitleTrack

/**
 * MX Player-style bubble quick settings menu
 * Floats in the top right corner with expandable bubble popup
 */
@Composable
fun BubbleQuickSettingsMenu(
    modifier: Modifier = Modifier,
    // Settings state
    currentPlaybackSpeed: Float,
    onPlaybackSpeedChange: (Float) -> Unit,
    currentBrightness: Float,
    onBrightnessChange: (Float) -> Unit,
    currentVolume: Float,
    onVolumeChange: (Float) -> Unit,
    isSubtitlesEnabled: Boolean,
    onSubtitlesToggle: (Boolean) -> Unit,
    availableSubtitles: List<SubtitleTrack> = emptyList(),
    selectedSubtitle: SubtitleTrack? = null,
    onSubtitleSelect: (SubtitleTrack?) -> Unit = {},
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
    audioDelay: Long = 0L,
    onAudioDelayChange: (Long) -> Unit = {},
    sleepTimerActive: Boolean = false,
    onSleepTimerClick: () -> Unit = {},
    onEnterPiP: () -> Unit = {},
    onCodecPacksClick: () -> Unit = {},
    showStatsOverlay: Boolean = false,
    onStatsToggle: (Boolean) -> Unit = {},
    // Long press speed control settings
    longPressSpeedEnabled: Boolean = true,
    onLongPressSpeedEnabledChange: (Boolean) -> Unit = {},
    longPressInitialSpeed: Float = 2.0f,
    onLongPressInitialSpeedChange: (Float) -> Unit = {},
    longPressProgressiveEnabled: Boolean = true,
    onLongPressProgressiveEnabledChange: (Boolean) -> Unit = {},
    longPressSwipeSensitivity: Float = 1.0f,
    onLongPressSwipeSensitivityChange: (Float) -> Unit = {},
    // Speed memory per video settings
    speedMemoryEnabled: Boolean = true,
    onSpeedMemoryEnabledChange: (Boolean) -> Unit = {},
    onClearAllSpeedMemory: () -> Unit = {}
) {
    var isExpanded by remember { mutableStateOf(false) }
    var selectedSetting by remember { mutableStateOf<QuickSettingType?>(null) }
    var showMainMenu by remember { mutableStateOf(false) }
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .zIndex(100f)
    ) {
        // Main floating button - positioned in top right
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 48.dp, end = 16.dp)
        ) {
            // Floating button with proper shadow and styling
            Surface(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .clickable { 
                        isExpanded = !isExpanded
                        if (isExpanded) {
                            showMainMenu = true
                        }
                    },
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.7f),
                shadowElevation = 6.dp
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .border(
                            width = 1.dp,
                            color = Color.White.copy(alpha = 0.2f),
                            shape = CircleShape
                        )
                ) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.Close else Icons.Default.Settings,
                        contentDescription = "Quick Settings",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            
            // Main menu bubble
            AnimatedVisibility(
                visible = isExpanded && showMainMenu,
                enter = scaleIn(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessLow
                    ),
                    initialScale = 0.8f,
                    transformOrigin = androidx.compose.ui.graphics.TransformOrigin(1f, 0f)
                ) + fadeIn(),
                exit = scaleOut(
                    targetScale = 0.8f,
                    transformOrigin = androidx.compose.ui.graphics.TransformOrigin(1f, 0f)
                ) + fadeOut()
            ) {
                Popup(
                    alignment = Alignment.TopEnd,
                    offset = androidx.compose.ui.unit.IntOffset(x = -16, y = 60),
                    onDismissRequest = { 
                        isExpanded = false
                        showMainMenu = false
                        selectedSetting = null
                    },
                    properties = PopupProperties(
                        dismissOnBackPress = true,
                        dismissOnClickOutside = true
                    )
                ) {
                    MainSettingsMenu(
                        onSettingClick = { setting ->
                            selectedSetting = setting
                            showMainMenu = false
                        },
                        sleepTimerActive = sleepTimerActive,
                        showStatsOverlay = showStatsOverlay
                    )
                }
            }
            
            // Setting detail bubble
            selectedSetting?.let { setting ->
                AnimatedVisibility(
                    visible = isExpanded && !showMainMenu,
                    enter = slideInHorizontally(
                        initialOffsetX = { it },
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy
                        )
                    ) + fadeIn(),
                    exit = slideOutHorizontally(
                        targetOffsetX = { it }
                    ) + fadeOut()
                ) {
                    Popup(
                        alignment = Alignment.TopEnd,
                        offset = androidx.compose.ui.unit.IntOffset(x = -16, y = 60),
                        onDismissRequest = { 
                            selectedSetting = null
                            showMainMenu = true
                        },
                        properties = PopupProperties(
                            dismissOnBackPress = true,
                            dismissOnClickOutside = true
                        )
                    ) {
                        SettingDetailBubble(
                            setting = setting,
                            onBack = {
                                selectedSetting = null
                                showMainMenu = true
                            },
                            onDismiss = {
                                isExpanded = false
                                selectedSetting = null
                                showMainMenu = false
                            },
                            // Pass all the settings
                            currentPlaybackSpeed = currentPlaybackSpeed,
                            onPlaybackSpeedChange = onPlaybackSpeedChange,
                            currentBrightness = currentBrightness,
                            onBrightnessChange = onBrightnessChange,
                            currentVolume = currentVolume,
                            onVolumeChange = onVolumeChange,
                            isSubtitlesEnabled = isSubtitlesEnabled,
                            onSubtitlesToggle = onSubtitlesToggle,
                            availableSubtitles = availableSubtitles,
                            selectedSubtitle = selectedSubtitle,
                            onSubtitleSelect = onSubtitleSelect,
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
                            audioDelay = audioDelay,
                            onAudioDelayChange = onAudioDelayChange,
                            onSleepTimerClick = onSleepTimerClick,
                            onEnterPiP = onEnterPiP,
                            onCodecPacksClick = onCodecPacksClick,
                            showStatsOverlay = showStatsOverlay,
                            onStatsToggle = onStatsToggle,
                            // Long press speed control settings
                            longPressSpeedEnabled = longPressSpeedEnabled,
                            onLongPressSpeedEnabledChange = onLongPressSpeedEnabledChange,
                            longPressInitialSpeed = longPressInitialSpeed,
                            onLongPressInitialSpeedChange = onLongPressInitialSpeedChange,
                            longPressProgressiveEnabled = longPressProgressiveEnabled,
                            onLongPressProgressiveEnabledChange = onLongPressProgressiveEnabledChange,
                            longPressSwipeSensitivity = longPressSwipeSensitivity,
                            onLongPressSwipeSensitivityChange = onLongPressSwipeSensitivityChange,
                            // Speed memory per video settings
                            speedMemoryEnabled = speedMemoryEnabled,
                            onSpeedMemoryEnabledChange = onSpeedMemoryEnabledChange,
                            onClearAllSpeedMemory = onClearAllSpeedMemory
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MainSettingsMenu(
    onSettingClick: (QuickSettingType) -> Unit,
    sleepTimerActive: Boolean,
    showStatsOverlay: Boolean
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color.Black.copy(alpha = 0.95f),
        shadowElevation = 8.dp,
        modifier = Modifier
            .width(240.dp)
            .wrapContentHeight()
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp)
        ) {
            QuickSettingType.values().forEach { setting ->
                MainSettingItem(
                    setting = setting,
                    onClick = { onSettingClick(setting) },
                    isActive = when (setting) {
                        QuickSettingType.SLEEP_TIMER -> sleepTimerActive
                        QuickSettingType.STATS -> showStatsOverlay
                        else -> false
                    }
                )
            }
        }
    }
}

@Composable
private fun MainSettingItem(
    setting: QuickSettingType,
    onClick: () -> Unit,
    isActive: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = setting.icon,
            contentDescription = setting.title,
            tint = if (isActive) MaterialTheme.colorScheme.primary else Color.White,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = setting.title,
            color = if (isActive) MaterialTheme.colorScheme.primary else Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.5f),
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
private fun SettingDetailBubble(
    setting: QuickSettingType,
    onBack: () -> Unit,
    onDismiss: () -> Unit,
    // All settings parameters
    currentPlaybackSpeed: Float,
    onPlaybackSpeedChange: (Float) -> Unit,
    currentBrightness: Float,
    onBrightnessChange: (Float) -> Unit,
    currentVolume: Float,
    onVolumeChange: (Float) -> Unit,
    isSubtitlesEnabled: Boolean,
    onSubtitlesToggle: (Boolean) -> Unit,
    availableSubtitles: List<SubtitleTrack>,
    selectedSubtitle: SubtitleTrack?,
    onSubtitleSelect: (SubtitleTrack?) -> Unit,
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
    audioDelay: Long,
    onAudioDelayChange: (Long) -> Unit,
    onSleepTimerClick: () -> Unit,
    onEnterPiP: () -> Unit,
    onCodecPacksClick: () -> Unit,
    showStatsOverlay: Boolean,
    onStatsToggle: (Boolean) -> Unit,
    // Long press speed control settings
    longPressSpeedEnabled: Boolean,
    onLongPressSpeedEnabledChange: (Boolean) -> Unit,
    longPressInitialSpeed: Float,
    onLongPressInitialSpeedChange: (Float) -> Unit,
    longPressProgressiveEnabled: Boolean,
    onLongPressProgressiveEnabledChange: (Boolean) -> Unit,
    longPressSwipeSensitivity: Float,
    onLongPressSwipeSensitivityChange: (Float) -> Unit,
    // Speed memory per video settings
    speedMemoryEnabled: Boolean,
    onSpeedMemoryEnabledChange: (Boolean) -> Unit,
    onClearAllSpeedMemory: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color.Black.copy(alpha = 0.95f),
        shadowElevation = 8.dp,
        modifier = Modifier
            .width(280.dp)
            .wrapContentHeight()
    ) {
        Column {
            // Header with back button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Text(
                    text = setting.title,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            
            Divider(color = Color.White.copy(alpha = 0.1f))
            
            // Setting specific content
            Box(
                modifier = Modifier.padding(16.dp)
            ) {
                when (setting) {
                    QuickSettingType.PLAYBACK_SPEED -> PlaybackSpeedControl(
                        currentSpeed = currentPlaybackSpeed,
                        onSpeedChange = onPlaybackSpeedChange
                    )
                    QuickSettingType.BRIGHTNESS -> BrightnessControl(
                        currentBrightness = currentBrightness,
                        onBrightnessChange = onBrightnessChange
                    )
                    QuickSettingType.VOLUME -> VolumeControl(
                        currentVolume = currentVolume,
                        onVolumeChange = onVolumeChange
                    )
                    QuickSettingType.SUBTITLES -> SubtitlesControl(
                        isEnabled = isSubtitlesEnabled,
                        onToggle = onSubtitlesToggle,
                        availableSubtitles = availableSubtitles,
                        selectedSubtitle = selectedSubtitle,
                        onSubtitleSelect = onSubtitleSelect
                    )
                    QuickSettingType.ASPECT_RATIO -> AspectRatioControl(
                        selectedRatio = selectedAspectRatio,
                        onRatioChange = onAspectRatioChange
                    )
                    QuickSettingType.ROTATION -> AutoRotateControl(
                        isEnabled = isAutoRotateEnabled,
                        onToggle = onAutoRotateToggle
                    )
                    QuickSettingType.VOLUME_BOOST -> VolumeBoostControl(
                        isEnabled = isVolumeBoostEnabled,
                        onToggle = onVolumeBoostToggle
                    )
                    QuickSettingType.LOOP_MODE -> LoopModeControl(
                        currentMode = currentLoopMode,
                        onModeChange = onLoopModeChange
                    )
                    QuickSettingType.AUDIO_DELAY -> AudioDelayControl(
                        delayMs = audioDelay,
                        onDelayChange = onAudioDelayChange
                    )
                    QuickSettingType.SLEEP_TIMER -> SleepTimerControl(
                        onClick = {
                            onSleepTimerClick()
                            onDismiss()
                        }
                    )
                    QuickSettingType.PIP -> PictureInPictureControl(
                        onEnterPiP = {
                            onEnterPiP()
                            onDismiss()
                        }
                    )
                    QuickSettingType.CODEC_PACKS -> CodecPacksControl(
                        onClick = {
                            onCodecPacksClick()
                            onDismiss()
                        }
                    )
                    QuickSettingType.LONG_PRESS_SPEED -> LongPressSpeedControl(
                        enabled = longPressSpeedEnabled,
                        onEnabledChange = onLongPressSpeedEnabledChange,
                        initialSpeed = longPressInitialSpeed,
                        onInitialSpeedChange = onLongPressInitialSpeedChange,
                        progressiveEnabled = longPressProgressiveEnabled,
                        onProgressiveEnabledChange = onLongPressProgressiveEnabledChange,
                        swipeSensitivity = longPressSwipeSensitivity,
                        onSwipeSensitivityChange = onLongPressSwipeSensitivityChange
                    )
                    QuickSettingType.SPEED_MEMORY -> SpeedMemoryControl(
                        enabled = speedMemoryEnabled,
                        onEnabledChange = onSpeedMemoryEnabledChange,
                        onClearAll = onClearAllSpeedMemory
                    )
                    QuickSettingType.STATS -> StatsControl(
                        isEnabled = showStatsOverlay,
                        onToggle = onStatsToggle
                    )
                }
            }
        }
    }
}

// Individual control components
@Composable
private fun PlaybackSpeedControl(
    currentSpeed: Float,
    onSpeedChange: (Float) -> Unit
) {
    val speedOptions = listOf(0.25f, 0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f)
    
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Current speed display
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Current Speed",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 12.sp
            )
            Text(
                text = "${currentSpeed}x",
                color = MaterialTheme.colorScheme.primary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }
        
        // Speed options grid
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            speedOptions.chunked(4).forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    row.forEach { speed ->
                        FilterChip(
                            selected = speed == currentSpeed,
                            onClick = { onSpeedChange(speed) },
                            label = { 
                                Text(
                                    text = "${speed}x",
                                    fontSize = 12.sp
                                )
                            },
                            modifier = Modifier.weight(1f),
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = Color.White.copy(alpha = 0.1f),
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                labelColor = Color.White,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BrightnessControl(
    currentBrightness: Float,
    onBrightnessChange: (Float) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.BrightnessLow,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.7f),
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = "${(currentBrightness * 100).toInt()}%",
                color = MaterialTheme.colorScheme.primary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            Icon(
                imageVector = Icons.Default.BrightnessHigh,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.7f),
                modifier = Modifier.size(20.dp)
            )
        }
        
        Slider(
            value = currentBrightness,
            onValueChange = onBrightnessChange,
            valueRange = 0f..1f,
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = Color.White.copy(alpha = 0.2f)
            )
        )
    }
}

@Composable
private fun VolumeControl(
    currentVolume: Float,
    onVolumeChange: (Float) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.VolumeOff,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.7f),
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = "${(currentVolume * 100).toInt()}%",
                color = MaterialTheme.colorScheme.primary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            Icon(
                imageVector = Icons.Default.VolumeUp,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.7f),
                modifier = Modifier.size(20.dp)
            )
        }
        
        Slider(
            value = currentVolume,
            onValueChange = onVolumeChange,
            valueRange = 0f..1f,
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = Color.White.copy(alpha = 0.2f)
            )
        )
    }
}

@Composable
private fun SubtitlesControl(
    isEnabled: Boolean,
    onToggle: (Boolean) -> Unit,
    availableSubtitles: List<SubtitleTrack>,
    selectedSubtitle: SubtitleTrack?,
    onSubtitleSelect: (SubtitleTrack?) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Subtitles",
                color = Color.White,
                fontSize = 14.sp
            )
            Switch(
                checked = isEnabled,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                    checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                )
            )
        }
        
        // Subtitle selection (if enabled and available)
        if (isEnabled && availableSubtitles.isNotEmpty()) {
            Divider(color = Color.White.copy(alpha = 0.1f))
            
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 150.dp)
            ) {
                availableSubtitles.forEach { subtitle ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSubtitleSelect(subtitle) }
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = subtitle.name ?: "Track ${subtitle.id}",
                            color = if (subtitle == selectedSubtitle) MaterialTheme.colorScheme.primary else Color.White,
                            fontSize = 13.sp
                        )
                        if (subtitle == selectedSubtitle) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AspectRatioControl(
    selectedRatio: String,
    onRatioChange: (String) -> Unit
) {
    val ratioOptions = listOf(
        "Fit" to "Fit to screen",
        "Fill" to "Fill screen",
        "Stretch" to "Stretch to fill",
        "16:9" to "16:9",
        "4:3" to "4:3", 
        "21:9" to "21:9 (Ultrawide)",
        "Original" to "Original"
    )
    
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ratioOptions.forEach { (ratio, label) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onRatioChange(ratio) }
                    .padding(vertical = 8.dp, horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = ratio,
                        color = if (ratio == selectedRatio) MaterialTheme.colorScheme.primary else Color.White,
                        fontSize = 14.sp,
                        fontWeight = if (ratio == selectedRatio) FontWeight.Medium else FontWeight.Normal
                    )
                    Text(
                        text = label,
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 11.sp
                    )
                }
                if (ratio == selectedRatio) {
                    Icon(
                        imageVector = Icons.Default.RadioButtonChecked,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.RadioButtonUnchecked,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.3f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun AutoRotateControl(
    isEnabled: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "Auto Rotate",
                color = Color.White,
                fontSize = 14.sp
            )
            Text(
                text = if (isEnabled) "Screen will rotate automatically" else "Screen rotation locked",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 12.sp
            )
        }
        Switch(
            checked = isEnabled,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.primary,
                checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
            )
        )
    }
}

@Composable
private fun VolumeBoostControl(
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
                    text = "Volume Boost",
                    color = Color.White,
                    fontSize = 14.sp
                )
                Text(
                    text = if (isEnabled) "Boost up to 200%" else "Normal volume",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 12.sp
                )
            }
            Switch(
                checked = isEnabled,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                    checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                )
            )
        }
        
        if (isEnabled) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = Color.Yellow.copy(alpha = 0.1f)
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = Color.Yellow,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "High volume may damage speakers",
                        color = Color.Yellow,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun LoopModeControl(
    currentMode: com.astralplayer.nextplayer.data.LoopMode,
    onModeChange: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onModeChange() },
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = when (currentMode) {
                            com.astralplayer.nextplayer.data.LoopMode.OFF -> Icons.Default.Repeat
                            com.astralplayer.nextplayer.data.LoopMode.ONE -> Icons.Default.RepeatOne
                            com.astralplayer.nextplayer.data.LoopMode.ALL -> Icons.Default.Repeat
                        },
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Column {
                        Text(
                            text = currentMode.getDisplayName(),
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = currentMode.getDescription(),
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 12.sp
                        )
                    }
                }
                Text(
                    text = "Tap to change",
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
private fun AudioDelayControl(
    delayMs: Long,
    onDelayChange: (Long) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Audio Delay",
                color = Color.White,
                fontSize = 14.sp
            )
            Text(
                text = "${delayMs}ms",
                color = MaterialTheme.colorScheme.primary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }
        
        // Quick preset buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(-500L, -100L, 0L, 100L, 500L).forEach { delay ->
                FilterChip(
                    selected = delay == delayMs,
                    onClick = { onDelayChange(delay) },
                    label = { 
                        Text(
                            text = "${delay}ms",
                            fontSize = 11.sp
                        )
                    },
                    modifier = Modifier.weight(1f),
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = Color.White.copy(alpha = 0.1f),
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        labelColor = Color.White,
                        selectedLabelColor = Color.White
                    )
                )
            }
        }
        
        // Fine adjustment slider
        Slider(
            value = delayMs.toFloat(),
            onValueChange = { onDelayChange(it.toLong()) },
            valueRange = -1000f..1000f,
            steps = 19, // 100ms steps
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = Color.White.copy(alpha = 0.2f)
            )
        )
    }
}

@Composable
private fun SleepTimerControl(
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
        )
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
                    imageVector = Icons.Default.Bedtime,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Column {
                    Text(
                        text = "Sleep Timer",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Auto-stop playback",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 12.sp
                    )
                }
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.5f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun PictureInPictureControl(
    onEnterPiP: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEnterPiP() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
        )
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
                    imageVector = Icons.Default.PictureInPictureAlt,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Column {
                    Text(
                        text = "Picture-in-Picture",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Continue watching in mini player",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 12.sp
                    )
                }
            }
            Icon(
                imageVector = Icons.Default.Launch,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.5f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun CodecPacksControl(
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
        )
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
                    imageVector = Icons.Default.Code,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Column {
                    Text(
                        text = "Codec Packs",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Download additional codecs",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 12.sp
                    )
                }
            }
            Icon(
                imageVector = Icons.Default.Download,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.5f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun StatsControl(
    isEnabled: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "Statistics Overlay",
                color = Color.White,
                fontSize = 14.sp
            )
            Text(
                text = if (isEnabled) "Showing video statistics" else "Statistics hidden",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 12.sp
            )
        }
        Switch(
            checked = isEnabled,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.primary,
                checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
            )
        )
    }
}

@Composable
private fun LongPressSpeedControl(
    enabled: Boolean = true,
    onEnabledChange: (Boolean) -> Unit = {},
    initialSpeed: Float = 2.0f,
    onInitialSpeedChange: (Float) -> Unit = {},
    progressiveEnabled: Boolean = true,
    onProgressiveEnabledChange: (Boolean) -> Unit = {},
    swipeSensitivity: Float = 1.0f,
    onSwipeSensitivityChange: (Float) -> Unit = {}
) {
    
    val speedProgression = listOf(0.25f, 0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f, 3.0f, 4.0f, 6.0f, 8.0f)
    
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Enable/Disable Long Press Speed Control
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Long Press Speed Control",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = if (enabled) "Enabled" else "Disabled",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 12.sp
                )
            }
            Switch(
                checked = enabled,
                onCheckedChange = onEnabledChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                    checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                )
            )
        }
        
        if (enabled) {
            Divider(color = Color.White.copy(alpha = 0.1f))
            
            // Initial Speed Selection
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Initial Speed",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Speed when long press starts",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 12.sp
                )
                
                // Speed selection chips
                LazyColumn(
                    modifier = Modifier.heightIn(max = 120.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(speedProgression.chunked(4)) { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            row.forEachIndexed { index, speed ->
                                val speedIndex = speedProgression.indexOf(speed)
                                FilterChip(
                                    selected = speed == initialSpeed,
                                    onClick = { onInitialSpeedChange(speed) },
                                    label = { 
                                        Text(
                                            text = "${speed}x",
                                            fontSize = 12.sp
                                        )
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = FilterChipDefaults.filterChipColors(
                                        containerColor = Color.White.copy(alpha = 0.1f),
                                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                                        labelColor = Color.White,
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }
                            // Fill remaining space if row is not complete
                            repeat(4 - row.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
            
            Divider(color = Color.White.copy(alpha = 0.1f))
            
            // Progressive Speed Control
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Progressive Speed Control",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = if (progressiveEnabled) "Swipe up/down to change speed" else "Fixed speed only",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 12.sp
                    )
                }
                Switch(
                    checked = progressiveEnabled,
                    onCheckedChange = onProgressiveEnabledChange,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.primary,
                        checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                    )
                )
            }
            
            if (progressiveEnabled) {
                // Swipe Sensitivity
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Swipe Sensitivity",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = when {
                                swipeSensitivity < 0.8f -> "Low"
                                swipeSensitivity > 1.2f -> "High"
                                else -> "Normal"
                            },
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    
                    Slider(
                        value = swipeSensitivity,
                        onValueChange = onSwipeSensitivityChange,
                        valueRange = 0.5f..2.0f,
                        steps = 2, // Low, Normal, High
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary,
                            inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                        )
                    )
                }
            }
            
            // Info card
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Column {
                        Text(
                            text = "How to use",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Long press center of screen to activate speed control. While holding, swipe up to increase speed or down to decrease.",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SpeedMemoryControl(
    enabled: Boolean = true,
    onEnabledChange: (Boolean) -> Unit = {},
    onClearAll: () -> Unit = {}
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Enable/Disable Speed Memory
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Speed Memory per Video",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = if (enabled) "Remembering speeds" else "Not remembering speeds",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 12.sp
                )
            }
            Switch(
                checked = enabled,
                onCheckedChange = onEnabledChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                    checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                )
            )
        }
        
        // Clear All Button (only show when enabled)
        if (enabled) {
            var showClearDialog by remember { mutableStateOf(false) }
            
            Button(
                onClick = { showClearDialog = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Red.copy(alpha = 0.2f),
                    contentColor = Color.Red
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Clear All Speed Memory",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            
            // Clear confirmation dialog
            if (showClearDialog) {
                AlertDialog(
                    onDismissRequest = { showClearDialog = false },
                    title = { 
                        Text(
                            "Clear All Speed Memory?",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                    },
                    text = {
                        Text(
                            "This will permanently remove all saved playback speeds for all videos. This action cannot be undone.",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 14.sp
                        )
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                onClearAll()
                                showClearDialog = false
                            }
                        ) {
                            Text(
                                "Clear All",
                                color = Color.Red,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showClearDialog = false }) {
                            Text(
                                "Cancel",
                                color = Color.White
                            )
                        }
                    },
                    containerColor = Color.Black.copy(alpha = 0.9f)
                )
            }
        }
        
        // Info card
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
            ),
            shape = RoundedCornerShape(8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Column {
                    Text(
                        text = "About Speed Memory",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "When enabled, the app remembers the last playback speed used for each video and automatically restores it when you play that video again.",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

// Setting types
enum class QuickSettingType(
    val title: String,
    val icon: ImageVector
) {
    PLAYBACK_SPEED("Playback Speed", Icons.Default.Speed),
    BRIGHTNESS("Brightness", Icons.Default.Brightness6),
    VOLUME("Volume", Icons.Default.VolumeUp),
    SUBTITLES("Subtitles", Icons.Default.Subtitles),
    ASPECT_RATIO("Aspect Ratio", Icons.Default.AspectRatio),
    ROTATION("Screen Rotation", Icons.Default.ScreenRotation),
    VOLUME_BOOST("Volume Boost", Icons.Default.VolumeUp),
    LOOP_MODE("Repeat", Icons.Default.Repeat),
    AUDIO_DELAY("Audio Sync", Icons.Default.Sync),
    SLEEP_TIMER("Sleep Timer", Icons.Default.Bedtime),
    LONG_PRESS_SPEED("Long Press Speed", Icons.Default.TouchApp),
    SPEED_MEMORY("Speed Memory", Icons.Default.Memory),
    PIP("Picture-in-Picture", Icons.Default.PictureInPictureAlt),
    CODEC_PACKS("Codec Packs", Icons.Default.Code),
    STATS("Statistics", Icons.Default.BarChart)
}

