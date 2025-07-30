package com.astralplayer.nextplayer.feature.settings

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.astralplayer.nextplayer.feature.player.gestures.GestureSettings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    gestureSettings: GestureSettings,
    playerPreferences: PlayerPreferences,
    onGestureSettingsChange: (GestureSettings) -> Unit,
    onPlayerPreferencesChange: (PlayerPreferences) -> Unit,
    onBack: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Gestures", "Playback", "Video", "Audio", "Advanced")
    
    Column(modifier = Modifier.fillMaxSize()) {
        // Top App Bar
        TopAppBar(
            title = { Text("Settings") },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color(0xFF1A1A2E)
            )
        )
        
        // Tab Row
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color(0xFF16213E),
            contentColor = Color.White
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title) }
                )
            }
        }
        
        // Content based on selected tab
        when (selectedTab) {
            0 -> GestureSettingsContent(
                settings = gestureSettings,
                onSettingsChange = onGestureSettingsChange
            )
            1 -> PlaybackSettingsContent(
                preferences = playerPreferences,
                onPreferencesChange = onPlayerPreferencesChange
            )
            2 -> VideoSettingsContent(
                preferences = playerPreferences,
                onPreferencesChange = onPlayerPreferencesChange
            )
            3 -> AudioSettingsContent(
                preferences = playerPreferences,
                onPreferencesChange = onPlayerPreferencesChange
            )
            4 -> AdvancedSettingsContent(
                preferences = playerPreferences,
                onPreferencesChange = onPlayerPreferencesChange
            )
        }
    }
}

@Composable
private fun GestureSettingsContent(
    settings: GestureSettings,
    onSettingsChange: (GestureSettings) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            SettingsSection(title = "General") {
                SwitchSetting(
                    title = "Enable Gestures",
                    subtitle = "Allow gesture controls",
                    checked = settings.general.gesturesEnabled,
                    onCheckedChange = { enabled ->
                        onSettingsChange(
                            settings.copy(
                                general = settings.general.copy(gesturesEnabled = enabled)
                            )
                        )
                    }
                )
                
                SwitchSetting(
                    title = "Haptic Feedback",
                    subtitle = "Vibrate on gesture actions",
                    checked = settings.general.feedbackVibrationsEnabled,
                    onCheckedChange = { enabled ->
                        onSettingsChange(
                            settings.copy(
                                general = settings.general.copy(feedbackVibrationsEnabled = enabled)
                            )
                        )
                    }
                )
            }
        }
        
        item {
            SettingsSection(title = "Sensitivity") {
                SliderSetting(
                    title = "Horizontal Sensitivity",
                    subtitle = "Seek gesture sensitivity",
                    value = settings.horizontal.sensitivity,
                    range = 0.1f..3.0f,
                    onValueChange = { value ->
                        onSettingsChange(
                            settings.copy(
                                horizontal = settings.horizontal.copy(sensitivity = value)
                            )
                        )
                    }
                )
                
                SliderSetting(
                    title = "Volume Sensitivity",
                    subtitle = "Volume gesture sensitivity",
                    value = settings.vertical.volumeSensitivity,
                    range = 0.1f..3.0f,
                    onValueChange = { value ->
                        onSettingsChange(
                            settings.copy(
                                vertical = settings.vertical.copy(volumeSensitivity = value)
                            )
                        )
                    }
                )
                
                SliderSetting(
                    title = "Brightness Sensitivity",
                    subtitle = "Brightness gesture sensitivity",
                    value = settings.vertical.brightnessSensitivity,
                    range = 0.1f..3.0f,
                    onValueChange = { value ->
                        onSettingsChange(
                            settings.copy(
                                vertical = settings.vertical.copy(brightnessSensitivity = value)
                            )
                        )
                    }
                )
            }
        }
        
        item {
            SettingsSection(title = "Long Press") {
                SwitchSetting(
                    title = "Long Press Seek",
                    subtitle = "Variable speed seeking",
                    checked = settings.longPress.enabled,
                    onCheckedChange = { enabled ->
                        onSettingsChange(
                            settings.copy(
                                longPress = settings.longPress.copy(enabled = enabled)
                            )
                        )
                    }
                )
                
                SliderSetting(
                    title = "Default Speed",
                    subtitle = "Initial long press speed",
                    value = settings.longPress.defaultSpeed,
                    range = 0.5f..5.0f,
                    onValueChange = { value ->
                        onSettingsChange(
                            settings.copy(
                                longPress = settings.longPress.copy(defaultSpeed = value)
                            )
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun PlaybackSettingsContent(
    preferences: PlayerPreferences,
    onPreferencesChange: (PlayerPreferences) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            SettingsSection(title = "Playback") {
                SwitchSetting(
                    title = "Auto Play",
                    subtitle = "Start playing automatically",
                    checked = preferences.autoPlay,
                    onCheckedChange = { enabled ->
                        onPreferencesChange(preferences.copy(autoPlay = enabled))
                    }
                )
                
                SwitchSetting(
                    title = "Resume Playback",
                    subtitle = "Resume from last position",
                    checked = preferences.resumePlayback,
                    onCheckedChange = { enabled ->
                        onPreferencesChange(preferences.copy(resumePlayback = enabled))
                    }
                )
                
                SliderSetting(
                    title = "Default Speed",
                    subtitle = "Default playback speed",
                    value = preferences.defaultPlaybackSpeed,
                    range = 0.25f..3.0f,
                    onValueChange = { value ->
                        onPreferencesChange(preferences.copy(defaultPlaybackSpeed = value))
                    }
                )
            }
        }
        
        item {
            SettingsSection(title = "Controls") {
                SwitchSetting(
                    title = "Auto Hide Controls",
                    subtitle = "Hide controls automatically",
                    checked = preferences.autoHideControls,
                    onCheckedChange = { enabled ->
                        onPreferencesChange(preferences.copy(autoHideControls = enabled))
                    }
                )
                
                SliderSetting(
                    title = "Controls Timeout",
                    subtitle = "Time before hiding controls (seconds)",
                    value = (preferences.controlsTimeout / 1000f),
                    range = 1f..10f,
                    onValueChange = { value ->
                        onPreferencesChange(preferences.copy(controlsTimeout = (value * 1000).toLong()))
                    }
                )
            }
        }
    }
}

@Composable
private fun VideoSettingsContent(
    preferences: PlayerPreferences,
    onPreferencesChange: (PlayerPreferences) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            SettingsSection(title = "Video") {
                SwitchSetting(
                    title = "Hardware Acceleration",
                    subtitle = "Use GPU for video decoding",
                    checked = preferences.hardwareAcceleration,
                    onCheckedChange = { enabled ->
                        onPreferencesChange(preferences.copy(hardwareAcceleration = enabled))
                    }
                )
                
                SwitchSetting(
                    title = "Keep Screen On",
                    subtitle = "Prevent screen from turning off",
                    checked = preferences.keepScreenOn,
                    onCheckedChange = { enabled ->
                        onPreferencesChange(preferences.copy(keepScreenOn = enabled))
                    }
                )
            }
        }
    }
}

@Composable
private fun AudioSettingsContent(
    preferences: PlayerPreferences,
    onPreferencesChange: (PlayerPreferences) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            SettingsSection(title = "Audio") {
                SwitchSetting(
                    title = "Audio Focus",
                    subtitle = "Pause when other apps play audio",
                    checked = preferences.audioFocus,
                    onCheckedChange = { enabled ->
                        onPreferencesChange(preferences.copy(audioFocus = enabled))
                    }
                )
                
                SwitchSetting(
                    title = "Volume Boost",
                    subtitle = "Amplify audio volume",
                    checked = preferences.volumeBoost,
                    onCheckedChange = { enabled ->
                        onPreferencesChange(preferences.copy(volumeBoost = enabled))
                    }
                )
            }
        }
        
        item {
            SettingsSection(title = "Subtitles") {
                SwitchSetting(
                    title = "Enable Subtitles",
                    subtitle = "Show subtitles when available",
                    checked = preferences.subtitlesEnabled,
                    onCheckedChange = { enabled ->
                        onPreferencesChange(preferences.copy(subtitlesEnabled = enabled))
                    }
                )
                
                SliderSetting(
                    title = "Subtitle Size",
                    subtitle = "Text size multiplier",
                    value = preferences.subtitleSize,
                    range = 0.5f..2.0f,
                    onValueChange = { value ->
                        onPreferencesChange(preferences.copy(subtitleSize = value))
                    }
                )
            }
        }
    }
}

@Composable
private fun AdvancedSettingsContent(
    preferences: PlayerPreferences,
    onPreferencesChange: (PlayerPreferences) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            SettingsSection(title = "Performance") {
                SliderSetting(
                    title = "Buffer Size",
                    subtitle = "Video buffer size (MB)",
                    value = (preferences.bufferSize / 1000f),
                    range = 10f..200f,
                    onValueChange = { value ->
                        onPreferencesChange(preferences.copy(bufferSize = (value * 1000).toInt()))
                    }
                )
                
                SliderSetting(
                    title = "Network Timeout",
                    subtitle = "Connection timeout (seconds)",
                    value = (preferences.networkTimeout / 1000f),
                    range = 5f..60f,
                    onValueChange = { value ->
                        onPreferencesChange(preferences.copy(networkTimeout = (value * 1000).toLong()))
                    }
                )
            }
        }
        
        item {
            SettingsSection(title = "Privacy") {
                SwitchSetting(
                    title = "Crash Reporting",
                    subtitle = "Send crash reports to improve the app",
                    checked = preferences.crashReporting,
                    onCheckedChange = { enabled ->
                        onPreferencesChange(preferences.copy(crashReporting = enabled))
                    }
                )
                
                SwitchSetting(
                    title = "Analytics",
                    subtitle = "Share usage data",
                    checked = preferences.collectAnalytics,
                    onCheckedChange = { enabled ->
                        onPreferencesChange(preferences.copy(collectAnalytics = enabled))
                    }
                )
            }
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1A1A2E)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF00BCD4)
            )
            content()
        }
    }
}

@Composable
private fun SwitchSetting(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = subtitle,
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 14.sp
            )
        }
        
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color(0xFF00BCD4),
                checkedTrackColor = Color(0xFF00BCD4).copy(alpha = 0.5f)
            )
        )
    }
}

@Composable
private fun SliderSetting(
    title: String,
    subtitle: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = subtitle,
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 14.sp
                )
            }
            
            Text(
                text = String.format("%.1f", value),
                color = Color(0xFF00BCD4),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }
        
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            colors = SliderDefaults.colors(
                thumbColor = Color(0xFF00BCD4),
                activeTrackColor = Color(0xFF00BCD4),
                inactiveTrackColor = Color.White.copy(alpha = 0.3f)
            )
        )
    }
}