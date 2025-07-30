package com.astralplayer.nextplayer.feature.settings

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.BrightnessHigh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.SlowMotionVideo
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.datastore.preferences.preferencesDataStore
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import com.astralplayer.nextplayer.ui.theme.AppTheme
import com.astralplayer.nextplayer.ui.theme.NextPlayerTheme
import com.astralplayer.nextplayer.feature.player.gestures.LongPressSettings

// Extension property for DataStore
private val Context.dataStore by preferencesDataStore(name = "settings")
// Key for storing theme
private val THEME_KEY = stringPreferencesKey("app_theme")

class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val settingsManager = LongPressSeekSettingsManager(this)
        
        // Observe stored theme
        val themeFlow = dataStore.data
            .map { prefs ->
                prefs[THEME_KEY]?.let { AppTheme.valueOf(it) } ?: AppTheme.SYSTEM
            }

        setContent {
            val currentTheme by themeFlow.collectAsState(initial = AppTheme.SYSTEM)
            NextPlayerTheme(appTheme = currentTheme) {
                SettingsScreen(
                    currentTheme = currentTheme,
                    settingsManager = settingsManager,
                    onThemeChange = { newTheme ->
                        lifecycleScope.launch {
                            dataStore.edit { prefs -> prefs[THEME_KEY] = newTheme.name }
                        }
                    },
                    onBackPressed = { finish() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    currentTheme: AppTheme,
    settingsManager: LongPressSeekSettingsManager,
    onThemeChange: (AppTheme) -> Unit,
    onBackPressed: () -> Unit
) {
    val longPressSettings by settingsManager.longPressSeekSettings.collectAsState(initial = LongPressSettings())
    val scope = rememberCoroutineScope()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                SettingsCategory("Video Playback")
                SettingsSwitch(
                    title = "Auto-play next video",
                    description = "Automatically play the next video in the folder",
                    icon = Icons.Default.PlayArrow,
                    checked = false,
                    onCheckedChange = { /* TODO */ }
                )
                SettingsSwitch(
                    title = "Remember playback position",
                    description = "Resume videos from where you left off",
                    icon = Icons.Default.Restore,
                    checked = true,
                    onCheckedChange = { /* TODO */ }
                )
            }
            item {
                SettingsCategory("Appearance")
                SettingsSwitch(
                    title = "Dark theme",
                    description = "Use dark theme throughout the app",
                    icon = Icons.Default.DarkMode,
                    checked = currentTheme == AppTheme.DARK,
                    onCheckedChange = { if (it) onThemeChange(AppTheme.DARK) }
                )
                SettingsSwitch(
                    title = "Cosmic UI",
                    description = "Enable cosmic theme",
                    icon = Icons.Default.Stars,
                    checked = currentTheme == AppTheme.COSMIC,
                    onCheckedChange = { if (it) onThemeChange(AppTheme.COSMIC) }
                )
            }
            item {
                SettingsCategory("Gestures")
                SettingsSwitch(
                    title = "Enhanced long press seek",
                    description = "Enable accelerated seeking with long press",
                    icon = Icons.Default.FastForward,
                    checked = longPressSettings.isEnabled,
                    onCheckedChange = { enabled ->
                        scope.launch {
                            settingsManager.setLongPressSeekEnabled(enabled)
                        }
                    }
                )
                SettingsSwitch(
                    title = "Volume gesture",
                    description = "Swipe vertically on right side to control volume",
                    icon = Icons.AutoMirrored.Filled.VolumeUp,
                    checked = true,
                    onCheckedChange = { /* TODO */ }
                )
                SettingsSwitch(
                    title = "Brightness gesture",
                    description = "Swipe vertically on left side to control brightness",
                    icon = Icons.Default.BrightnessHigh,
                    checked = true,
                    onCheckedChange = { /* TODO */ }
                )
            }
            item {
                SettingsCategory("Long Press Seek Settings")
                SettingsSwitch(
                    title = "Auto Speed Acceleration",
                    description = "Automatically increase speed over time during long press",
                    icon = Icons.Default.AutoAwesome,
                    checked = longPressSettings.adaptiveSpeed,
                    onCheckedChange = { enabled ->
                        scope.launch {
                            settingsManager.setAdaptiveSpeed(enabled)
                        }
                    }
                )
                SettingsSwitch(
                    title = "Haptic Feedback",
                    description = "Provide haptic feedback during long press seek",
                    icon = Icons.Default.Vibration,
                    checked = longPressSettings.hapticFeedbackEnabled,
                    onCheckedChange = { enabled ->
                        scope.launch {
                            settingsManager.setHapticFeedbackEnabled(enabled)
                        }
                    }
                )
                SettingsSwitch(
                    title = "Show Speed Zones",
                    description = "Display visual speed zone indicators",
                    icon = Icons.Default.Visibility,
                    checked = longPressSettings.showSpeedZones,
                    onCheckedChange = { enabled ->
                        scope.launch {
                            settingsManager.setShowSpeedZones(enabled)
                        }
                    }
                )
                SettingsSlider(
                    title = "Activation Time",
                    description = "Time to hold for long press activation",
                    icon = Icons.Default.Timer,
                    value = longPressSettings.duration.toFloat(),
                    valueRange = 200f..1000f,
                    valueText = "${longPressSettings.duration.toInt()}ms",
                    onValueChange = { value ->
                        scope.launch {
                            settingsManager.setLongPressDuration(value.toLong())
                        }
                    }
                )
                SettingsSlider(
                    title = "Max Seek Speed",
                    description = "Maximum speed multiplier for seeking",
                    icon = Icons.Default.Speed,
                    value = longPressSettings.maxSpeed,
                    valueRange = 3f..10f,
                    valueText = "${longPressSettings.maxSpeed.toInt()}x",
                    onValueChange = { value ->
                        scope.launch {
                            settingsManager.setMaxSpeed(value)
                        }
                    }
                )
                SettingsSlider(
                    title = "Default Seek Speed",
                    description = "Default speed multiplier for seeking",
                    icon = Icons.Default.SlowMotionVideo,
                    value = longPressSettings.defaultSpeed,
                    valueRange = 2f..5f,
                    valueText = "${longPressSettings.defaultSpeed.toInt()}x",
                    onValueChange = { value ->
                        scope.launch {
                            settingsManager.setDefaultSpeed(value)
                        }
                    }
                )
                SettingsSlider(
                    title = "Direction Sensitivity",
                    description = "Sensitivity for direction change detection",
                    icon = Icons.Default.SwapHoriz,
                    value = longPressSettings.directionChangeThreshold,
                    valueRange = 20f..100f,
                    valueText = "${longPressSettings.directionChangeThreshold.toInt()}dp",
                    onValueChange = { value ->
                        scope.launch {
                            settingsManager.setDirectionChangeThreshold(value)
                        }
                    }
                )
                SettingsSlider(
                    title = "Speed Sensitivity",
                    description = "Sensitivity for speed change detection",
                    icon = Icons.Default.Tune,
                    value = longPressSettings.speedChangeThreshold,
                    valueRange = 0.1f..1f,
                    valueText = String.format("%.2f", longPressSettings.speedChangeThreshold),
                    onValueChange = { value ->
                        scope.launch {
                            settingsManager.setSpeedChangeThreshold(value)
                        }
                    }
                )
                SettingsSlider(
                    title = "Update Frequency",
                    description = "Frequency of seek updates during long press",
                    icon = Icons.Default.Refresh,
                    value = longPressSettings.continuousSeekInterval.toFloat(),
                    valueRange = 20f..200f,
                    valueText = "${longPressSettings.continuousSeekInterval}ms",
                    onValueChange = { value ->
                        scope.launch {
                            settingsManager.setContinuousSeekInterval(value.toLong())
                        }
                    }
                )
            }
            item {
                SettingsCategory("Advanced")
                SettingsSwitch(
                    title = "Hardware acceleration",
                    description = "Use hardware acceleration for video playback",
                    icon = Icons.Default.Speed,
                    checked = true,
                    onCheckedChange = { /* TODO */ }
                )
                SettingsSwitch(
                    title = "Background playback",
                    description = "Continue playing audio when app is in background",
                    icon = Icons.Default.Headphones,
                    checked = false,
                    onCheckedChange = { /* TODO */ }
                )
            }
            item {
                SettingsCategory("Theme")
                ThemeOption(
                    label = "System Default",
                    theme = AppTheme.SYSTEM,
                    currentTheme = currentTheme,
                    onThemeChange = onThemeChange
                )
                ThemeOption(
                    label = "Light",
                    theme = AppTheme.LIGHT,
                    currentTheme = currentTheme,
                    onThemeChange = onThemeChange
                )
                ThemeOption(
                    label = "Dark",
                    theme = AppTheme.DARK,
                    currentTheme = currentTheme,
                    onThemeChange = onThemeChange
                )
                ThemeOption(
                    label = "Cosmic",
                    theme = AppTheme.COSMIC,
                    currentTheme = currentTheme,
                    onThemeChange = onThemeChange
                )
                ThemeOption(
                    label = "High Contrast",
                    theme = AppTheme.HIGH_CONTRAST,
                    currentTheme = currentTheme,
                    onThemeChange = onThemeChange
                )
            }
        }
    }
}

@Composable
fun SettingsCategory(title: String) {
    Text(
        text = title,
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(vertical = 8.dp)
    )
}

@Composable
fun SettingsSwitch(
    title: String,
    description: String,
    icon: ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            modifier = Modifier.padding(end = 16.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title)
            Text(text = description, style = MaterialTheme.typography.bodySmall)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
fun ThemeOption(
    label: String,
    theme: AppTheme,
    currentTheme: AppTheme,
    onThemeChange: (AppTheme) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onThemeChange(theme) }
            .testTag("ThemeOption_${theme.name}")
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = currentTheme == theme,
            onClick = { onThemeChange(theme) },
            colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = label, fontSize = 16.sp)
    }
}

@Composable
fun SettingsSlider(
    title: String,
    description: String,
    icon: ImageVector,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    valueText: String,
    onValueChange: (Float) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                modifier = Modifier.padding(end = 16.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title)
                Text(text = description, style = MaterialTheme.typography.bodySmall)
            }
            Text(
                text = valueText,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            modifier = Modifier.fillMaxWidth(),
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary
            )
        )
    }
}