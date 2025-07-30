package com.astralplayer.nextplayer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.astralplayer.nextplayer.feature.settings.LongPressSeekSettings
import com.astralplayer.nextplayer.feature.settings.LongPressSeekSettingsManager
import com.astralplayer.nextplayer.ui.theme.AstralTheme
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val settingsManager = LongPressSeekSettingsManager(this)
        
        setContent {
            AstralTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF0A0E27)
                ) {
                    SettingsScreen(
                        settingsManager = settingsManager,
                        onBack = { finish() }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settingsManager: LongPressSeekSettingsManager,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var settings by remember { mutableStateOf(LongPressSeekSettings()) }
    
    // Collect settings
    LaunchedEffect(Unit) {
        settingsManager.longPressSeekSettings.collectLatest { 
            settings = it
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Settings",
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = Color.White
                )
            )
        },
        containerColor = Color.Transparent
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Long Press Seek Settings Section
            SettingsSection(
                title = "Long Press Seek",
                icon = Icons.Default.TouchApp
            ) {
                // Enable/Disable
                SwitchSettingItem(
                    title = "Enable Long Press Seek",
                    subtitle = "Hold to seek at variable speeds",
                    checked = settings.isEnabled,
                    onCheckedChange = { enabled ->
                        scope.launch {
                            settingsManager.updateLongPressSeekSettings(
                                settings.copy(isEnabled = enabled)
                            )
                        }
                    }
                )
                
                Divider(color = Color.White.copy(alpha = 0.1f))
                
                // Default Speed
                SliderSettingItem(
                    title = "Default Seek Speed",
                    subtitle = "Initial speed when long pressing",
                    value = settings.defaultSpeed,
                    valueRange = 1f..5f,
                    steps = 3,
                    displayValue = "${settings.defaultSpeed.toInt()}x",
                    onValueChange = { speed ->
                        scope.launch {
                            settingsManager.updateLongPressSeekSettings(
                                settings.copy(defaultSpeed = speed)
                            )
                        }
                    }
                )
                
                // Max Speed
                SliderSettingItem(
                    title = "Maximum Seek Speed",
                    subtitle = "Maximum speed when swiping",
                    value = settings.maxSpeed,
                    valueRange = 3f..10f,
                    steps = 6,
                    displayValue = "${settings.maxSpeed.toInt()}x",
                    onValueChange = { speed ->
                        scope.launch {
                            settingsManager.updateLongPressSeekSettings(
                                settings.copy(maxSpeed = speed)
                            )
                        }
                    }
                )
                
                Divider(color = Color.White.copy(alpha = 0.1f))
                
                // Haptic Feedback
                SwitchSettingItem(
                    title = "Haptic Feedback",
                    subtitle = "Vibrate on speed changes",
                    checked = settings.hapticFeedbackEnabled,
                    onCheckedChange = { enabled ->
                        scope.launch {
                            settingsManager.updateLongPressSeekSettings(
                                settings.copy(hapticFeedbackEnabled = enabled)
                            )
                        }
                    }
                )
                
                // Speed Indicator
                SwitchSettingItem(
                    title = "Show Speed Indicator",
                    subtitle = "Display current seek speed",
                    checked = settings.showSpeedIndicator,
                    onCheckedChange = { enabled ->
                        scope.launch {
                            settingsManager.updateLongPressSeekSettings(
                                settings.copy(showSpeedIndicator = enabled)
                            )
                        }
                    }
                )
                
                // Preview Playback
                SwitchSettingItem(
                    title = "Preview During Seek",
                    subtitle = "Continue playing video while seeking",
                    checked = settings.enablePreviewPlayback,
                    onCheckedChange = { enabled ->
                        scope.launch {
                            settingsManager.updateLongPressSeekSettings(
                                settings.copy(enablePreviewPlayback = enabled)
                            )
                        }
                    }
                )
            }
            
            // Other Settings Sections
            SettingsSection(
                title = "Playback",
                icon = Icons.Default.PlayCircle
            ) {
                Text(
                    "More playback settings coming soon",
                    color = Color.White.copy(alpha = 0.6f),
                    modifier = Modifier.padding(16.dp)
                )
            }
            
            SettingsSection(
                title = "About",
                icon = Icons.Default.Info
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "Astral Player",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        "Version 1.0.0",
                        color = Color.White.copy(alpha = 0.6f)
                    )
                    Text(
                        "Advanced video player with MX Player style gestures",
                        color = Color.White.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}

@Composable
fun SettingsSection(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.05f)
        )
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xFF00BCD4).copy(alpha = 0.2f),
                                Color(0xFF00D4FF).copy(alpha = 0.1f)
                            )
                        )
                    )
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color(0xFF00D4FF),
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color.White
                )
            }
            content()
        }
    }
}

@Composable
fun SwitchSettingItem(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                fontWeight = FontWeight.Medium,
                color = Color.White
            )
            Text(
                text = subtitle,
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.6f)
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color(0xFF00D4FF),
                checkedTrackColor = Color(0xFF00D4FF).copy(alpha = 0.5f),
                uncheckedThumbColor = Color.Gray,
                uncheckedTrackColor = Color.Gray.copy(alpha = 0.3f)
            )
        )
    }
}

@Composable
fun SliderSettingItem(
    title: String,
    subtitle: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    displayValue: String,
    onValueChange: (Float) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Medium,
                    color = Color.White
                )
                Text(
                    text = subtitle,
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.6f)
                )
            }
            Text(
                text = displayValue,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF00D4FF)
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps,
            colors = SliderDefaults.colors(
                thumbColor = Color(0xFF00D4FF),
                activeTrackColor = Color(0xFF00D4FF),
                inactiveTrackColor = Color.White.copy(alpha = 0.2f)
            )
        )
    }
}