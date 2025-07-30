package com.astralplayer.nextplayer.feature.player.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.astralplayer.nextplayer.feature.player.viewmodel.PlayerViewModel
import com.astralplayer.nextplayer.feature.settings.ComprehensiveSettingsManager

/**
 * Modern, clean quick settings menu with smooth animations and popout submenus
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModernQuickSettingsMenu(
    isVisible: Boolean,
    viewModel: PlayerViewModel,
    settingsManager: ComprehensiveSettingsManager,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var activeSubmenu by remember { mutableStateOf<QuickSettingItem?>(null) }
    val density = LocalDensity.current
    
    // Quick setting items
    val quickSettings = remember {
        listOf(
            QuickSettingItem(
                id = "playback_speed",
                title = "Speed",
                icon = Icons.Default.Speed,
                currentValue = "1x",
                type = SettingType.SPEED
            ),
            QuickSettingItem(
                id = "quality",
                title = "Quality",
                icon = Icons.Default.HighQuality,
                currentValue = "Auto",
                type = SettingType.QUALITY
            ),
            QuickSettingItem(
                id = "audio_track",
                title = "Audio",
                icon = Icons.Default.VolumeUp,
                currentValue = "Track 1",
                type = SettingType.AUDIO_TRACK
            ),
            QuickSettingItem(
                id = "subtitles",
                title = "Subtitles",
                icon = Icons.Default.Subtitles,
                currentValue = "Off",
                type = SettingType.SUBTITLES
            ),
            QuickSettingItem(
                id = "aspect_ratio",
                title = "Aspect",
                icon = Icons.Default.AspectRatio,
                currentValue = "Fit",
                type = SettingType.ASPECT_RATIO
            ),
            QuickSettingItem(
                id = "brightness",
                title = "Brightness",
                icon = Icons.Default.Brightness6,
                currentValue = "50%",
                type = SettingType.BRIGHTNESS,
                hasSlider = true
            ),
            QuickSettingItem(
                id = "volume",
                title = "Volume",
                icon = Icons.Default.VolumeUp,
                currentValue = "75%",
                type = SettingType.VOLUME,
                hasSlider = true
            ),
            QuickSettingItem(
                id = "equalizer",
                title = "Equalizer",
                icon = Icons.Default.Equalizer,
                currentValue = "Off",
                type = SettingType.EQUALIZER
            ),
            QuickSettingItem(
                id = "sleep_timer",
                title = "Sleep Timer",
                icon = Icons.Default.Timer,
                currentValue = "Off",
                type = SettingType.SLEEP_TIMER
            ),
            QuickSettingItem(
                id = "gesture_lock",
                title = "Lock",
                icon = Icons.Default.Lock,
                currentValue = "",
                type = SettingType.GESTURE_LOCK,
                isToggle = true
            )
        )
    }
    
    AnimatedVisibility(
        visible = isVisible,
        enter = slideInHorizontally(
            initialOffsetX = { it },
            animationSpec = tween(300, easing = EaseOutCubic)
        ) + fadeIn(animationSpec = tween(200)),
        exit = slideOutHorizontally(
            targetOffsetX = { it },
            animationSpec = tween(200, easing = EaseInCubic)
        ) + fadeOut(animationSpec = tween(150))
    ) {
        Box(
            modifier = modifier
                .fillMaxHeight()
                .width(280.dp)
                .background(
                    Color.Black.copy(alpha = 0.9f),
                    RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp)
                )
                .padding(16.dp)
        ) {
            Column {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Quick Settings",
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Settings list
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(quickSettings) { setting ->
                        QuickSettingItemView(
                            item = setting,
                            isActive = activeSubmenu == setting,
                            onItemClick = { item ->
                                activeSubmenu = if (activeSubmenu == item) null else item
                            },
                            onValueChange = { /* Handle value changes */ }
                        )
                    }
                }
            }
        }
    }
    
    // Submenu popups
    activeSubmenu?.let { submenu ->
        when (submenu.type) {
            SettingType.SPEED -> SpeedSubmenu(
                onDismiss = { activeSubmenu = null },
                onSpeedSelected = { speed ->
                    viewModel.setPlaybackSpeed(speed)
                    activeSubmenu = null
                }
            )
            SettingType.QUALITY -> QualitySubmenu(
                onDismiss = { activeSubmenu = null },
                onQualitySelected = { quality ->
                    // Handle quality selection
                    activeSubmenu = null
                }
            )
            SettingType.AUDIO_TRACK -> AudioTrackSubmenu(
                onDismiss = { activeSubmenu = null },
                onTrackSelected = { track ->
                    // Handle audio track selection
                    activeSubmenu = null
                }
            )
            SettingType.SUBTITLES -> SubtitleSubmenu(
                onDismiss = { activeSubmenu = null },
                onSubtitleSelected = { subtitle ->
                    // Handle subtitle selection
                    activeSubmenu = null
                }
            )
            SettingType.ASPECT_RATIO -> AspectRatioSubmenu(
                onDismiss = { activeSubmenu = null },
                onAspectSelected = { aspect ->
                    // Handle aspect ratio selection
                    activeSubmenu = null
                }
            )
            SettingType.BRIGHTNESS -> BrightnessSliderPopup(
                onDismiss = { activeSubmenu = null },
                onBrightnessChange = { brightness ->
                    viewModel.adjustBrightness(brightness)
                }
            )
            SettingType.VOLUME -> VolumeSliderPopup(
                onDismiss = { activeSubmenu = null },
                onVolumeChange = { volume ->
                    viewModel.adjustVolume(volume)
                }
            )
            else -> {}
        }
    }
}

@Composable
private fun QuickSettingItemView(
    item: QuickSettingItem,
    isActive: Boolean,
    onItemClick: (QuickSettingItem) -> Unit,
    onValueChange: (Any) -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (isActive) 1.05f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
    )
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clickable { onItemClick(item) },
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) 
                MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
            else 
                Color.White.copy(alpha = 0.1f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    item.icon,
                    contentDescription = item.title,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
                
                Column {
                    Text(
                        text = item.title,
                        color = Color.White,
                        fontWeight = FontWeight.Medium,
                        fontSize = 16.sp
                    )
                    
                    if (item.currentValue.isNotEmpty()) {
                        Text(
                            text = item.currentValue,
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 12.sp
                        )
                    }
                }
            }
            
            if (item.hasSlider) {
                Icon(
                    TuneIcon,
                    contentDescription = "Adjust",
                    tint = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.size(20.dp)
                )
            } else if (item.isToggle) {
                Switch(
                    checked = false, // Get from state
                    onCheckedChange = { onValueChange(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.primary,
                        uncheckedThumbColor = Color.White.copy(alpha = 0.6f)
                    )
                )
            } else {
                Icon(
                    ChevronRight,
                    contentDescription = "Open",
                    tint = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

// Submenu components
@Composable
private fun SpeedSubmenu(
    onDismiss: () -> Unit,
    onSpeedSelected: (Float) -> Unit
) {
    val speeds = listOf(0.25f, 0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f, 2.5f, 3.0f)
    
    Popup(
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = true)
    ) {
        Card(
            modifier = Modifier
                .width(200.dp)
                .padding(8.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.95f)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Playback Speed",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                speeds.forEach { speed ->
                    Text(
                        text = "${speed}x",
                        color = Color.White,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSpeedSelected(speed) }
                            .padding(vertical = 8.dp, horizontal = 4.dp),
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun QualitySubmenu(
    onDismiss: () -> Unit,
    onQualitySelected: (String) -> Unit
) {
    val qualities = listOf("Auto", "2160p", "1440p", "1080p", "720p", "480p", "360p")
    
    Popup(
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = true)
    ) {
        Card(
            modifier = Modifier
                .width(200.dp)
                .padding(8.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.95f)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Video Quality",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                qualities.forEach { quality ->
                    Text(
                        text = quality,
                        color = Color.White,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onQualitySelected(quality) }
                            .padding(vertical = 8.dp, horizontal = 4.dp),
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun AudioTrackSubmenu(
    onDismiss: () -> Unit,
    onTrackSelected: (String) -> Unit
) {
    val tracks = listOf("Track 1 (English)", "Track 2 (Spanish)", "Track 3 (French)")
    
    Popup(
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = true)
    ) {
        Card(
            modifier = Modifier
                .width(220.dp)
                .padding(8.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.95f)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Audio Track",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                tracks.forEach { track ->
                    Text(
                        text = track,
                        color = Color.White,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onTrackSelected(track) }
                            .padding(vertical = 8.dp, horizontal = 4.dp),
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun SubtitleSubmenu(
    onDismiss: () -> Unit,
    onSubtitleSelected: (String) -> Unit
) {
    val subtitles = listOf("Off", "English", "Spanish", "French", "Auto-generated")
    
    Popup(
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = true)
    ) {
        Card(
            modifier = Modifier
                .width(200.dp)
                .padding(8.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.95f)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Subtitles",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                subtitles.forEach { subtitle ->
                    Text(
                        text = subtitle,
                        color = Color.White,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSubtitleSelected(subtitle) }
                            .padding(vertical = 8.dp, horizontal = 4.dp),
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun AspectRatioSubmenu(
    onDismiss: () -> Unit,
    onAspectSelected: (String) -> Unit
) {
    val aspects = listOf("Fit", "Fill", "Stretch", "Crop", "Original")
    
    Popup(
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = true)
    ) {
        Card(
            modifier = Modifier
                .width(200.dp)
                .padding(8.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.95f)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Aspect Ratio",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                aspects.forEach { aspect ->
                    Text(
                        text = aspect,
                        color = Color.White,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onAspectSelected(aspect) }
                            .padding(vertical = 8.dp, horizontal = 4.dp),
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun BrightnessSliderPopup(
    onDismiss: () -> Unit,
    onBrightnessChange: (Float) -> Unit
) {
    var brightness by remember { mutableFloatStateOf(0.5f) }
    
    Popup(
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = true)
    ) {
        Card(
            modifier = Modifier
                .width(250.dp)
                .padding(8.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.95f)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Brightness",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    
                    Text(
                        text = "${(brightness * 100).toInt()}%",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 14.sp
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        Icons.Default.BrightnessLow,
                        contentDescription = "Low",
                        tint = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(20.dp)
                    )
                    
                    Slider(
                        value = brightness,
                        onValueChange = { 
                            brightness = it
                            onBrightnessChange(it)
                        },
                        modifier = Modifier.weight(1f),
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary,
                            inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                        )
                    )
                    
                    Icon(
                        Icons.Default.BrightnessHigh,
                        contentDescription = "High",
                        tint = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun VolumeSliderPopup(
    onDismiss: () -> Unit,
    onVolumeChange: (Float) -> Unit
) {
    var volume by remember { mutableFloatStateOf(0.75f) }
    
    Popup(
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = true)
    ) {
        Card(
            modifier = Modifier
                .width(250.dp)
                .padding(8.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.95f)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Volume",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    
                    Text(
                        text = "${(volume * 100).toInt()}%",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 14.sp
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        Icons.Default.VolumeDown,
                        contentDescription = "Low",
                        tint = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(20.dp)
                    )
                    
                    Slider(
                        value = volume,
                        onValueChange = { 
                            volume = it
                            onVolumeChange(it)
                        },
                        modifier = Modifier.weight(1f),
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary,
                            inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                        )
                    )
                    
                    Icon(
                        Icons.Default.VolumeUp,
                        contentDescription = "High",
                        tint = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

// Data classes
data class QuickSettingItem(
    val id: String,
    val title: String,
    val icon: ImageVector,
    val currentValue: String,
    val type: SettingType,
    val hasSlider: Boolean = false,
    val isToggle: Boolean = false
)

enum class SettingType {
    SPEED, QUALITY, AUDIO_TRACK, SUBTITLES, ASPECT_RATIO, 
    BRIGHTNESS, VOLUME, EQUALIZER, SLEEP_TIMER, GESTURE_LOCK
}

// Extension for missing icons
val TuneIcon: ImageVector = Icons.Default.Tune
val ChevronRight: ImageVector = Icons.Default.KeyboardArrowRight