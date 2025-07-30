package com.astralplayer.nextplayer.feature.player.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.mutableStateOf
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
import com.astralplayer.nextplayer.feature.player.viewmodel.PlayerViewModel

@Composable
fun MxPlayerSettingsMenu(
    viewModel: PlayerViewModel,
    onDismiss: () -> Unit,
    onOpenAdvancedSettings: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .wrapContentHeight(),
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF1A1A2E)
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Text(
                    text = "Player Settings",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                LazyColumn {
                    items(getSettingsItems()) { item ->
                        MxPlayerSettingsItem(
                            icon = item.icon,
                            title = item.title,
                            subtitle = item.subtitle,
                            onClick = {
                                when (item.id) {
                                    "gestures" -> onOpenAdvancedSettings()
                                    "aspect_ratio" -> {
                                        viewModel.toggleShowAspectRatioMenu()
                                        onDismiss()
                                    }
                                    "screen_orientation" -> {
                                        viewModel.toggleScreenOrientationMenu()
                                        onDismiss()
                                    }
                                    "sleep_timer" -> {
                                        viewModel.toggleShowSleepTimer()
                                        onDismiss()
                                    }
                                    "equalizer" -> {
                                        viewModel.toggleShowEqualizer()
                                        onDismiss()
                                    }
                                    "video_info" -> {
                                        viewModel.toggleShowVideoInfo()
                                        onDismiss()
                                    }
                                    "repeat_mode" -> {
                                        viewModel.toggleShowRepeatMode()
                                        onDismiss()
                                    }
                                    "subtitle_sync" -> {
                                        viewModel.toggleShowSubtitleSync()
                                        onDismiss()
                                    }
                                }
                            }
                        )
                    }
                }
                
                Divider(
                    color = Color.White.copy(alpha = 0.2f),
                    modifier = Modifier.padding(vertical = 12.dp)
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Close", color = Color(0xFF00BCD4))
                    }
                }
            }
        }
    }
}

@Composable
private fun MxPlayerSettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = Color(0xFF00BCD4),
            modifier = Modifier.size(24.dp)
        )
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
            subtitle?.let {
                Text(
                    text = it,
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 2.dp)
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

@Composable
fun MxPlayerSpeedMenu(
    currentSpeed: Float,
    onSpeedSelected: (Float) -> Unit,
    onDismiss: () -> Unit
) {
    val speeds = listOf(0.25f, 0.5f, 0.75f, 1f, 1.25f, 1.5f, 1.75f, 2f, 2.5f, 3f)
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(dismissOnClickOutside = true)
    ) {
        Surface(
            modifier = Modifier.wrapContentSize(),
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF1A1A2E)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Playback Speed",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                
                LazyColumn {
                    items(speeds) { speed ->
                        val isSelected = speed == currentSpeed
                        
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    onSpeedSelected(speed)
                                    onDismiss()
                                }
                                .background(
                                    if (isSelected) Color(0xFF00BCD4).copy(alpha = 0.2f)
                                    else Color.Transparent
                                )
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = null,
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = Color(0xFF00BCD4),
                                    unselectedColor = Color.White.copy(alpha = 0.6f)
                                )
                            )
                            
                            Spacer(modifier = Modifier.width(12.dp))
                            
                            Text(
                                text = "${speed}x",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MxPlayerAspectRatioMenu(
    currentRatio: String,
    onRatioSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val aspectRatios = listOf(
        "Original" to "Original",
        "16:9" to "Widescreen",
        "4:3" to "Standard", 
        "21:9" to "Cinema",
        "1:1" to "Square",
        "Fit" to "Fit to screen",
        "Fill" to "Fill screen",
        "Zoom" to "Zoom"
    )
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(dismissOnClickOutside = true)
    ) {
        Surface(
            modifier = Modifier.wrapContentSize(),
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF1A1A2E)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Aspect Ratio",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                
                LazyColumn {
                    items(aspectRatios) { (ratio, name) ->
                        val isSelected = ratio == currentRatio
                        
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    onRatioSelected(ratio)
                                    onDismiss()
                                }
                                .background(
                                    if (isSelected) Color(0xFF00BCD4).copy(alpha = 0.2f)
                                    else Color.Transparent
                                )
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = null,
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = Color(0xFF00BCD4),
                                    unselectedColor = Color.White.copy(alpha = 0.6f)
                                )
                            )
                            
                            Spacer(modifier = Modifier.width(12.dp))
                            
                            Text(
                                text = name,
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MxPlayerScreenOrientationMenu(
    currentOrientation: String,
    onOrientationSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val orientations = listOf(
        "auto" to "Auto-rotate",
        "portrait" to "Portrait",
        "landscape" to "Landscape",
        "reverse_portrait" to "Reverse Portrait",
        "reverse_landscape" to "Reverse Landscape",
        "sensor" to "Follow Sensor"
    )
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(dismissOnClickOutside = true)
    ) {
        Surface(
            modifier = Modifier.wrapContentSize(),
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF1A1A2E)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Screen Orientation",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                
                LazyColumn {
                    items(orientations) { (orientation, name) ->
                        val isSelected = orientation == currentOrientation
                        
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    onOrientationSelected(orientation)
                                    onDismiss()
                                }
                                .background(
                                    if (isSelected) Color(0xFF00BCD4).copy(alpha = 0.2f)
                                    else Color.Transparent
                                )
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = null,
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = Color(0xFF00BCD4),
                                    unselectedColor = Color.White.copy(alpha = 0.6f)
                                )
                            )
                            
                            Spacer(modifier = Modifier.width(12.dp))
                            
                            Text(
                                text = name,
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MxPlayerRepeatModeMenu(
    currentMode: String,
    onModeSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val repeatModes = listOf(
        "off" to "No Repeat",
        "one" to "Repeat One",
        "all" to "Repeat All",
        "shuffle" to "Shuffle"
    )
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(dismissOnClickOutside = true)
    ) {
        Surface(
            modifier = Modifier.wrapContentSize(),
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF1A1A2E)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Repeat Mode",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                
                LazyColumn {
                    items(repeatModes) { (mode, name) ->
                        val isSelected = mode == currentMode
                        
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    onModeSelected(mode)
                                    onDismiss()
                                }
                                .background(
                                    if (isSelected) Color(0xFF00BCD4).copy(alpha = 0.2f)
                                    else Color.Transparent
                                )
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Repeat,
                                contentDescription = name,
                                tint = if (isSelected) Color(0xFF00BCD4) else Color.White.copy(alpha = 0.6f),
                                modifier = Modifier.size(24.dp)
                            )
                            
                            Spacer(modifier = Modifier.width(12.dp))
                            
                            Text(
                                text = name,
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MxPlayerSubtitleSyncMenu(
    currentOffset: Long,
    onOffsetChanged: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    var offsetMillis by remember { mutableStateOf(currentOffset) }
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(dismissOnClickOutside = true)
    ) {
        Surface(
            modifier = Modifier.wrapContentSize(),
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF1A1A2E)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Subtitle Sync",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                Text(
                    text = "Adjust subtitle timing",
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.padding(bottom = 24.dp)
                )
                
                // Current offset display
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White.copy(alpha = 0.1f)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "${if (offsetMillis >= 0) "+" else ""}${offsetMillis / 1000.0}s",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF00BCD4),
                        modifier = Modifier.padding(16.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Quick adjustment buttons
                Text(
                    text = "Quick Adjust",
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedButton(
                        onClick = { offsetMillis -= 500 },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color.White
                        )
                    ) {
                        Text("-0.5s")
                    }
                    
                    OutlinedButton(
                        onClick = { offsetMillis -= 100 },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color.White
                        )
                    ) {
                        Text("-0.1s")
                    }
                    
                    OutlinedButton(
                        onClick = { offsetMillis += 100 },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color.White
                        )
                    ) {
                        Text("+0.1s")
                    }
                    
                    OutlinedButton(
                        onClick = { offsetMillis += 500 },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color.White
                        )
                    ) {
                        Text("+0.5s")
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Slider for fine adjustment
                Slider(
                    value = offsetMillis.toFloat(),
                    onValueChange = { offsetMillis = it.toLong() },
                    valueRange = -5000f..5000f,
                    steps = 99,
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFF00BCD4),
                        activeTrackColor = Color(0xFF00BCD4),
                        inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { 
                            offsetMillis = 0
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color.White
                        )
                    ) {
                        Text("Reset")
                    }
                    
                    Button(
                        onClick = {
                            onOffsetChanged(offsetMillis)
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF00BCD4)
                        )
                    ) {
                        Text("Apply", color = Color.Black)
                    }
                }
            }
        }
    }
}

@Composable
fun MxPlayerKidsLockDialog(
    onEnableKidsLock: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.wrapContentSize(),
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF1A1A2E)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.ChildCare,
                    contentDescription = null,
                    tint = Color(0xFF00BCD4),
                    modifier = Modifier.size(48.dp)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Kids Lock",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Enable Kids Lock to prevent accidental touches and restrict access to other apps while watching videos.",
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.8f),
                    lineHeight = 20.sp
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color.White
                        )
                    ) {
                        Text("Cancel")
                    }
                    
                    Button(
                        onClick = {
                            onEnableKidsLock()
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF00BCD4)
                        )
                    ) {
                        Text("Enable", color = Color.Black)
                    }
                }
            }
        }
    }
}

private fun getSettingsItems() = listOf(
    SettingsItem("gestures", Icons.Default.Gesture, "Gesture Controls", "Configure touch gestures"),
    SettingsItem("aspect_ratio", Icons.Default.AspectRatio, "Aspect Ratio", "Adjust video aspect ratio"),
    SettingsItem("screen_orientation", Icons.Default.ScreenRotation, "Screen Orientation", "Lock or auto-rotate"),
    SettingsItem("sleep_timer", Icons.Default.Bedtime, "Sleep Timer", "Auto-pause after time"),
    SettingsItem("equalizer", Icons.Default.Equalizer, "Audio Equalizer", "Adjust audio settings"),
    SettingsItem("video_info", Icons.Default.Info, "Video Info", "View technical details"),
    SettingsItem("repeat_mode", Icons.Default.Repeat, "Repeat Mode", "Loop playback options"),
    SettingsItem("subtitle_sync", Icons.Default.Sync, "Subtitle Sync", "Adjust subtitle timing")
)

private data class SettingsItem(
    val id: String,
    val icon: ImageVector,
    val title: String,
    val subtitle: String
)