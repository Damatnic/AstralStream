package com.astralplayer.nextplayer.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.astralplayer.nextplayer.feature.player.gestures.GestureSettings
import com.astralplayer.nextplayer.feature.player.gestures.GeneralGestureSettings
import com.astralplayer.nextplayer.feature.player.gestures.HorizontalGestureSettings
import com.astralplayer.nextplayer.feature.player.gestures.VerticalGestureSettings
import com.astralplayer.nextplayer.feature.player.gestures.LongPressGestureSettings
import com.astralplayer.nextplayer.feature.player.gestures.ZoomGestureSettings
import com.astralplayer.nextplayer.feature.player.gestures.DoubleTapGestureSettings
import com.astralplayer.nextplayer.feature.player.gestures.UISettings
import com.astralplayer.nextplayer.feature.player.gestures.PlayBarPosition

@Composable
fun MxPlayerGestureSettingsScreen(
    gestureSettings: GestureSettings,
    onSettingsChanged: (GestureSettings) -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1A1A2E),
                        Color(0xFF16213E),
                        Color(0xFF0F0F0F)
                    )
                )
            )
    ) {
        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
            Text(
                text = "Gesture Settings",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
        
        LazyColumn(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // General Settings
            item {
                GestureSettingsSection(
                    title = "General",
                    icon = Icons.Default.Settings,
                    content = {
                        GeneralGestureSettingsContent(
                            settings = gestureSettings.general,
                            onSettingsChanged = { newGeneral ->
                                onSettingsChanged(gestureSettings.copy(general = newGeneral))
                            }
                        )
                    }
                )
            }
            
            // Touch Gestures
            item {
                GestureSettingsSection(
                    title = "Touch Gestures",
                    icon = Icons.Default.TouchApp,
                    content = {
                        TouchGestureSettingsContent(
                            doubleTapSettings = gestureSettings.doubleTap,
                            longPressSettings = gestureSettings.longPress,
                            onDoubleTapChanged = { newDoubleTap ->
                                onSettingsChanged(gestureSettings.copy(doubleTap = newDoubleTap))
                            },
                            onLongPressChanged = { newLongPress ->
                                onSettingsChanged(gestureSettings.copy(longPress = newLongPress))
                            }
                        )
                    }
                )
            }
            
            // Swipe Gestures
            item {
                GestureSettingsSection(
                    title = "Swipe Gestures",
                    icon = Icons.Default.SwipeUp,
                    content = {
                        SwipeGestureSettingsContent(
                            horizontalSettings = gestureSettings.horizontal,
                            verticalSettings = gestureSettings.vertical,
                            onHorizontalChanged = { newHorizontal ->
                                onSettingsChanged(gestureSettings.copy(horizontal = newHorizontal))
                            },
                            onVerticalChanged = { newVertical ->
                                onSettingsChanged(gestureSettings.copy(vertical = newVertical))
                            }
                        )
                    }
                )
            }
            
            // Zoom Gestures
            item {
                GestureSettingsSection(
                    title = "Zoom & Pan",
                    icon = Icons.Default.ZoomIn,
                    content = {
                        ZoomGestureSettingsContent(
                            settings = gestureSettings.zoom,
                            onSettingsChanged = { newZoom ->
                                onSettingsChanged(gestureSettings.copy(zoom = newZoom))
                            }
                        )
                    }
                )
            }
            
            // UI Settings
            item {
                GestureSettingsSection(
                    title = "UI & Layout",
                    icon = Icons.Default.AspectRatio,
                    content = {
                        UISettingsContent(
                            settings = gestureSettings.ui,
                            onSettingsChanged = { newUI ->
                                onSettingsChanged(gestureSettings.copy(ui = newUI))
                            }
                        )
                    }
                )
            }
            
            // Reset to defaults
            item {
                Spacer(modifier = Modifier.height(24.dp))
                
                Button(
                    onClick = { onSettingsChanged(GestureSettings()) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF8B5CF6)
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.RestartAlt,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Reset to Defaults", color = Color.White)
                }
            }
        }
    }
}

@Composable
private fun GestureSettingsSection(
    title: String,
    icon: ImageVector,
    content: @Composable () -> Unit
) {
    var isExpanded by remember { mutableStateOf(true) }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF6B46C1),
                            Color(0xFF7C3AED),
                            Color(0xFF5B21B6)
                        )
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color(0xFFE0E7FF),
                    modifier = Modifier.size(24.dp)
                )
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                
                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.7f)
                )
            }
            
            if (isExpanded) {
                Spacer(modifier = Modifier.height(12.dp))
                content()
            }
        }
    }
}

@Composable
private fun GeneralGestureSettingsContent(
    settings: GeneralGestureSettings,
    onSettingsChanged: (GeneralGestureSettings) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SwitchSettingItem(
            title = "Enable Gestures",
            subtitle = "Master toggle for all gesture controls",
            checked = settings.gesturesEnabled,
            onCheckedChange = { onSettingsChanged(settings.copy(gesturesEnabled = it)) }
        )
        
        SwitchSettingItem(
            title = "Tap to Toggle Controls",
            subtitle = "Single tap to show/hide player controls",
            checked = settings.tapToToggleControls,
            onCheckedChange = { onSettingsChanged(settings.copy(tapToToggleControls = it)) },
            enabled = settings.gesturesEnabled
        )
        
        SwitchSettingItem(
            title = "Haptic Feedback",
            subtitle = "Vibrate on gesture recognition",
            checked = settings.feedbackVibrationsEnabled,
            onCheckedChange = { onSettingsChanged(settings.copy(feedbackVibrationsEnabled = it)) },
            enabled = settings.gesturesEnabled
        )
        
        SwitchSettingItem(
            title = "Visual Feedback",
            subtitle = "Show visual indicators for gestures",
            checked = settings.visualFeedbackEnabled,
            onCheckedChange = { onSettingsChanged(settings.copy(visualFeedbackEnabled = it)) },
            enabled = settings.gesturesEnabled
        )
    }
}

@Composable
private fun UISettingsContent(
    settings: UISettings,
    onSettingsChanged: (UISettings) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Landscape Play Bar Position
        Text(
            text = "Landscape Mode",
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium
        )
        
        SwitchSettingItem(
            title = "Side Play Bar",
            subtitle = "Move play bar to screen sides in landscape mode",
            checked = settings.showLandscapePlayBarOnSides,
            onCheckedChange = { onSettingsChanged(settings.copy(showLandscapePlayBarOnSides = it)) }
        )
        
        if (settings.showLandscapePlayBarOnSides) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Play Bar Position",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f)
                )
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        onClick = { onSettingsChanged(settings.copy(landscapePlayBarPosition = PlayBarPosition.LEFT)) },
                        label = { Text("Left", color = Color.White) },
                        selected = settings.landscapePlayBarPosition == PlayBarPosition.LEFT,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF8B5CF6),
                            containerColor = Color.Transparent
                        )
                    )
                    
                    FilterChip(
                        onClick = { onSettingsChanged(settings.copy(landscapePlayBarPosition = PlayBarPosition.RIGHT)) },
                        label = { Text("Right", color = Color.White) },
                        selected = settings.landscapePlayBarPosition == PlayBarPosition.RIGHT,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF8B5CF6),
                            containerColor = Color.Transparent
                        )
                    )
                }
            }
        }
    }
}
@Composable
private fun TouchGestureSettingsContent(
    doubleTapSettings: DoubleTapGestureSettings,
    longPressSettings: LongPressGestureSettings,
    onDoubleTapChanged: (DoubleTapGestureSettings) -> Unit,
    onLongPressChanged: (LongPressGestureSettings) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Double Tap Settings
        Text(
            text = "Double Tap",
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium
        )
        
        SwitchSettingItem(
            title = "Enable Double Tap",
            subtitle = "Double tap to seek forward/backward",
            checked = doubleTapSettings.enabled,
            onCheckedChange = { onDoubleTapChanged(doubleTapSettings.copy(enabled = it)) }
        )
        
        if (doubleTapSettings.enabled) {
            SliderSettingItem(
                title = "Seek Amount",
                subtitle = "${doubleTapSettings.seekAmount / 1000}s per double tap",
                value = doubleTapSettings.seekAmount.toFloat(),
                valueRange = 5000f..30000f,
                steps = 4,
                onValueChange = { onDoubleTapChanged(doubleTapSettings.copy(seekAmount = it.toLong())) }
            )
            
            SwitchSettingItem(
                title = "Center Tap to Pause",
                subtitle = "Double tap center to play/pause",
                checked = doubleTapSettings.centerTapToPause,
                onCheckedChange = { onDoubleTapChanged(doubleTapSettings.copy(centerTapToPause = it)) }
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Long Press Settings
        Text(
            text = "Long Press",
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium
        )
        
        SwitchSettingItem(
            title = "Enable Long Press Seek",
            subtitle = "Long press and swipe for variable speed seeking",
            checked = longPressSettings.enabled,
            onCheckedChange = { onLongPressChanged(longPressSettings.copy(enabled = it)) }
        )
        
        if (longPressSettings.enabled) {
            SliderSettingItem(
                title = "Long Press Duration",
                subtitle = "${longPressSettings.duration}ms to activate",
                value = longPressSettings.duration.toFloat(),
                valueRange = 300f..1500f,
                steps = 7,
                onValueChange = { onLongPressChanged(longPressSettings.copy(duration = it.toLong())) }
            )
            
            SliderSettingItem(
                title = "Maximum Speed",
                subtitle = "${longPressSettings.maxSpeed}x max seek speed",
                value = longPressSettings.maxSpeed,
                valueRange = 2f..10f,
                steps = 7,
                onValueChange = { onLongPressChanged(longPressSettings.copy(maxSpeed = it)) }
            )
        }
    }
}

@Composable
private fun SwipeGestureSettingsContent(
    horizontalSettings: HorizontalGestureSettings,
    verticalSettings: VerticalGestureSettings,
    onHorizontalChanged: (HorizontalGestureSettings) -> Unit,
    onVerticalChanged: (VerticalGestureSettings) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Horizontal Swipe Settings
        Text(
            text = "Horizontal Swipe (Seek)",
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium
        )
        
        SwitchSettingItem(
            title = "Enable Seek Gesture",
            subtitle = "Swipe left/right to seek",
            checked = horizontalSettings.seekGestureEnabled,
            onCheckedChange = { onHorizontalChanged(horizontalSettings.copy(seekGestureEnabled = it)) }
        )
        
        if (horizontalSettings.seekGestureEnabled) {
            SliderSettingItem(
                title = "Seek Sensitivity",
                subtitle = "How responsive seeking feels",
                value = horizontalSettings.sensitivity,
                valueRange = 0.5f..2f,
                steps = 2,
                onValueChange = { onHorizontalChanged(horizontalSettings.copy(sensitivity = it)) }
            )
            
            SwitchSettingItem(
                title = "Show Seek Preview",
                subtitle = "Display preview while seeking",
                checked = horizontalSettings.showSeekPreview,
                onCheckedChange = { onHorizontalChanged(horizontalSettings.copy(showSeekPreview = it)) }
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Vertical Swipe Settings
        Text(
            text = "Vertical Swipe",
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium
        )
        
        SwitchSettingItem(
            title = "Volume Gesture (Right Side)",
            subtitle = "Swipe up/down on right side to adjust volume",
            checked = verticalSettings.volumeGestureEnabled,
            onCheckedChange = { onVerticalChanged(verticalSettings.copy(volumeGestureEnabled = it)) }
        )
        
        SwitchSettingItem(
            title = "Brightness Gesture (Left Side)",
            subtitle = "Swipe up/down on left side to adjust brightness",
            checked = verticalSettings.brightnessGestureEnabled,
            onCheckedChange = { onVerticalChanged(verticalSettings.copy(brightnessGestureEnabled = it)) }
        )
        
        if (verticalSettings.volumeGestureEnabled || verticalSettings.brightnessGestureEnabled) {
            SliderSettingItem(
                title = "Volume Sensitivity",
                subtitle = "Volume adjustment responsiveness",
                value = verticalSettings.volumeSensitivity,
                valueRange = 0.5f..2f,
                steps = 2,
                onValueChange = { onVerticalChanged(verticalSettings.copy(volumeSensitivity = it)) },
                enabled = verticalSettings.volumeGestureEnabled
            )
            
            SliderSettingItem(
                title = "Brightness Sensitivity",
                subtitle = "Brightness adjustment responsiveness",
                value = verticalSettings.brightnessSensitivity,
                valueRange = 0.5f..2f,
                steps = 2,
                onValueChange = { onVerticalChanged(verticalSettings.copy(brightnessSensitivity = it)) },
                enabled = verticalSettings.brightnessGestureEnabled
            )
        }
    }
}

@Composable
private fun ZoomGestureSettingsContent(
    settings: ZoomGestureSettings,
    onSettingsChanged: (ZoomGestureSettings) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SwitchSettingItem(
            title = "Enable Pinch to Zoom",
            subtitle = "Pinch gesture to zoom in/out",
            checked = settings.enabled,
            onCheckedChange = { onSettingsChanged(settings.copy(enabled = it)) }
        )
        
        if (settings.enabled) {
            SliderSettingItem(
                title = "Minimum Zoom",
                subtitle = "${(settings.minZoom * 100).toInt()}% minimum zoom level",
                value = settings.minZoom,
                valueRange = 0.25f..1f,
                steps = 2,
                onValueChange = { onSettingsChanged(settings.copy(minZoom = it)) }
            )
            
            SliderSettingItem(
                title = "Maximum Zoom",
                subtitle = "${(settings.maxZoom * 100).toInt()}% maximum zoom level",
                value = settings.maxZoom,
                valueRange = 1f..5f,
                steps = 7,
                onValueChange = { onSettingsChanged(settings.copy(maxZoom = it)) }
            )
            
            SwitchSettingItem(
                title = "Double Tap to Fit",
                subtitle = "Double tap to fit video to screen",
                checked = settings.doubleTapToFit,
                onCheckedChange = { onSettingsChanged(settings.copy(doubleTapToFit = it)) }
            )
        }
    }
}

@Composable
private fun SwitchSettingItem(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(enabled = enabled) { onCheckedChange(!checked) }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                color = if (enabled) Color.White else Color.White.copy(alpha = 0.5f),
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = subtitle,
                color = if (enabled) Color.White.copy(alpha = 0.7f) else Color.White.copy(alpha = 0.3f),
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
        
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color(0xFFE0E7FF),
                checkedTrackColor = Color(0xFF8B5CF6),
                uncheckedThumbColor = Color.White.copy(alpha = 0.7f),
                uncheckedTrackColor = Color.White.copy(alpha = 0.2f)
            )
        )
    }
}

@Composable
private fun SliderSettingItem(
    title: String,
    subtitle: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int = 0,
    onValueChange: (Float) -> Unit,
    enabled: Boolean = true
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(
            text = title,
            color = if (enabled) Color.White else Color.White.copy(alpha = 0.5f),
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = subtitle,
            color = if (enabled) Color.White.copy(alpha = 0.7f) else Color.White.copy(alpha = 0.3f),
            fontSize = 13.sp,
            modifier = Modifier.padding(top = 2.dp, bottom = 8.dp)
        )
        
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps,
            enabled = enabled,
            colors = SliderDefaults.colors(
                thumbColor = Color(0xFFE0E7FF),
                activeTrackColor = Color(0xFF8B5CF6),
                inactiveTrackColor = Color.White.copy(alpha = 0.2f)
            )
        )
    }
}