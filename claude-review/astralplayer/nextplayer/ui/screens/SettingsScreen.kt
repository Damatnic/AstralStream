package com.astralplayer.nextplayer.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import com.astralplayer.nextplayer.data.repository.SettingsRepository
import com.astralplayer.nextplayer.ui.components.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settingsRepository: SettingsRepository,
    onNavigateBack: () -> Unit,
    onNavigateToGestureSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    // Playback settings state
    var autoPlayNext by remember { mutableStateOf(true) }
    var rememberPlaybackSpeed by remember { mutableStateOf(true) }
    var defaultPlaybackSpeed by remember { mutableStateOf(1.0f) }
    var rememberBrightness by remember { mutableStateOf(false) }
    
    // Display settings state
    var autoRotate by remember { mutableStateOf(true) }
    var aspectRatio by remember { mutableStateOf("fit") }
    
    // Gesture settings state
    var hapticFeedback by remember { mutableStateOf(true) }
    var longPressSpeedEnabled by remember { mutableStateOf(true) }
    var longPressInitialSpeed by remember { mutableStateOf(2.0f) }
    var longPressProgressiveEnabled by remember { mutableStateOf(true) }
    var speedMemoryEnabled by remember { mutableStateOf(true) }
    
    // Audio settings state
    var volumeBoost by remember { mutableStateOf(false) }
    
    // AI settings state
    var aiSubtitleGeneration by remember { mutableStateOf(true) }
    
    // Network settings state
    var streamOnMobileData by remember { mutableStateOf(true) }
    
    // Privacy settings state
    var saveWatchHistory by remember { mutableStateOf(true) }
    var appLock by remember { mutableStateOf(false) }
    
    // Dialog states
    var showPlaybackSpeedDialog by remember { mutableStateOf(false) }
    var showAspectRatioDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showAudioLanguageDialog by remember { mutableStateOf(false) }
    var showQualityDialog by remember { mutableStateOf(false) }
    var showClearDataDialog by remember { mutableStateOf(false) }
    var showSubtitleAppearanceDialog by remember { mutableStateOf(false) }
    var showLongPressSpeedDialog by remember { mutableStateOf(false) }
    var showClearSpeedMemoryDialog by remember { mutableStateOf(false) }
    
    // Settings state
    var subtitleLanguage by remember { mutableStateOf("auto") }
    var audioLanguage by remember { mutableStateOf("auto") }
    var videoQuality by remember { mutableStateOf("auto") }
    
    // Load initial values from repository
    LaunchedEffect(Unit) {
        // Playback settings
        autoPlayNext = settingsRepository.getAutoPlayNext().first()
        rememberPlaybackSpeed = settingsRepository.getRememberPlaybackSpeed().first()
        defaultPlaybackSpeed = settingsRepository.getDefaultPlaybackSpeed().first()
        rememberBrightness = settingsRepository.getRememberBrightness().first()
        
        // Display settings
        autoRotate = settingsRepository.getScreenOrientation().first() == "auto"
        aspectRatio = settingsRepository.getAspectRatio().first()
        
        // Gesture settings
        hapticFeedback = settingsRepository.getSwipeToSeekEnabled().first() // Using as proxy for haptic feedback
        
        // Long press speed control settings
        longPressSpeedEnabled = settingsRepository.getLongPressSpeedControlEnabled().first()
        longPressInitialSpeed = settingsRepository.getLongPressInitialSpeed().first()
        longPressProgressiveEnabled = settingsRepository.getLongPressProgressiveSpeedEnabled().first()
        speedMemoryEnabled = settingsRepository.getSpeedMemoryEnabled().first()
        
        // Audio settings
        volumeBoost = settingsRepository.getVolumeBoostEnabled().first()
        
        // AI settings
        aiSubtitleGeneration = settingsRepository.getAISubtitleGenerationEnabled().first()
        
        // Language and quality settings  
        subtitleLanguage = settingsRepository.getSubtitleLanguage().first()
        audioLanguage = settingsRepository.getAudioLanguage().first()
        videoQuality = settingsRepository.getVideoQuality().first()
        
        // Network settings
        streamOnMobileData = settingsRepository.getAllowMobileData().first()
        
        // Privacy settings
        saveWatchHistory = !settingsRepository.getIncognitoMode().first()
        appLock = settingsRepository.getAppLockEnabled().first()
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        modifier = modifier
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            // Playback Settings
            item {
                SettingsSectionHeader(title = "Playback")
            }
            
            item {
                SwitchSettingItem(
                    icon = Icons.Default.AutoMode,
                    title = "Auto-play next",
                    subtitle = "Automatically play next video",
                    checked = autoPlayNext,
                    onCheckedChange = { enabled ->
                        autoPlayNext = enabled
                        coroutineScope.launch {
                            settingsRepository.setAutoPlayNext(enabled)
                        }
                    }
                )
            }
            
            item {
                SwitchSettingItem(
                    icon = Icons.Default.Speed,
                    title = "Remember playback speed",
                    subtitle = "Remember speed for each video",
                    checked = rememberPlaybackSpeed,
                    onCheckedChange = { enabled ->
                        rememberPlaybackSpeed = enabled
                        coroutineScope.launch {
                            settingsRepository.setRememberPlaybackSpeed(enabled)
                        }
                    }
                )
            }
            
            item {
                ClickableSettingItem(
                    icon = Icons.Default.Speed,
                    title = "Default playback speed",
                    subtitle = "${defaultPlaybackSpeed}x",
                    onClick = { showPlaybackSpeedDialog = true }
                )
            }
            
            item {
                SwitchSettingItem(
                    icon = Icons.Default.PictureInPicture,
                    title = "Picture-in-Picture",
                    subtitle = "Enable PiP mode",
                    checked = true,
                    onCheckedChange = { _ ->
                        // PiP is handled at the system level, this is just a preference
                        coroutineScope.launch {
                            // Could store this preference if needed
                        }
                    }
                )
            }
            
            // Display Settings
            item {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                SettingsSectionHeader(title = "Display")
            }
            
            item {
                ClickableSettingItem(
                    icon = Icons.Default.AspectRatio,
                    title = "Default aspect ratio",
                    subtitle = when(aspectRatio) {
                        "fit" -> "Fit to screen"
                        "fill" -> "Fill screen"
                        "16:9" -> "16:9"
                        "4:3" -> "4:3"
                        else -> aspectRatio
                    },
                    onClick = { 
                        showAspectRatioDialog = true
                    }
                )
            }
            
            item {
                SwitchSettingItem(
                    icon = Icons.Default.ScreenRotation,
                    title = "Auto-rotate",
                    subtitle = "Rotate video with device orientation",
                    checked = autoRotate,
                    onCheckedChange = { enabled ->
                        autoRotate = enabled
                        coroutineScope.launch {
                            settingsRepository.setScreenOrientation(if (enabled) "auto" else "sensor")
                        }
                    }
                )
            }
            
            item {
                SwitchSettingItem(
                    icon = Icons.Default.Brightness6,
                    title = "Remember brightness",
                    subtitle = "Save brightness settings per video",
                    checked = rememberBrightness,
                    onCheckedChange = { enabled ->
                        rememberBrightness = enabled
                        coroutineScope.launch {
                            settingsRepository.setRememberBrightness(enabled)
                        }
                    }
                )
            }
            
            // Gesture Settings
            item {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                SettingsSectionHeader(title = "Gestures")
            }
            
            item {
                ClickableSettingItem(
                    icon = Icons.Default.TouchApp,
                    title = "Gesture controls",
                    subtitle = "Customize swipe and tap gestures",
                    onClick = onNavigateToGestureSettings
                )
            }
            
            item {
                SwitchSettingItem(
                    icon = Icons.Default.Vibration,
                    title = "Haptic feedback",
                    subtitle = "Vibrate on gestures",
                    checked = hapticFeedback,
                    onCheckedChange = { enabled ->
                        hapticFeedback = enabled
                        coroutineScope.launch {
                            // Store haptic feedback preference
                            // For now, using swipe to seek as a proxy
                            settingsRepository.setSwipeToSeekEnabled(enabled)
                        }
                    }
                )
            }
            
            item {
                ClickableSettingItem(
                    icon = Icons.Default.Speed,
                    title = "Long press speed control",
                    subtitle = if (longPressSpeedEnabled) {
                        "Enabled - ${longPressInitialSpeed}x initial speed"
                    } else {
                        "Disabled"
                    },
                    onClick = { showLongPressSpeedDialog = true }
                )
            }
            
            item {
                SwitchSettingItem(
                    icon = Icons.Default.Memory,
                    title = "Speed memory per video",
                    subtitle = "Remember last used speed for each video",
                    checked = speedMemoryEnabled,
                    onCheckedChange = { enabled ->
                        speedMemoryEnabled = enabled
                        coroutineScope.launch {
                            settingsRepository.setSpeedMemoryEnabled(enabled)
                        }
                    }
                )
            }
            
            if (speedMemoryEnabled) {
                item {
                    ClickableSettingItem(
                        icon = Icons.Default.Delete,
                        title = "Clear all speed memory",
                        subtitle = "Remove saved speeds for all videos",
                        onClick = { showClearSpeedMemoryDialog = true }
                    )
                }
            }
            
            // Subtitle Settings
            item {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                SettingsSectionHeader(title = "Subtitles")
            }
            
            item {
                ClickableSettingItem(
                    icon = Icons.Default.Subtitles,
                    title = "Subtitle appearance",
                    subtitle = "Font, size, and color",
                    onClick = { 
                        showSubtitleAppearanceDialog = true
                    }
                )
            }
            
            item {
                ClickableSettingItem(
                    icon = Icons.Default.Language,
                    title = "Preferred subtitle language",
                    subtitle = when(subtitleLanguage) {
                        "auto" -> "Auto-detect"
                        "en" -> "English"
                        "es" -> "Spanish"
                        "fr" -> "French"
                        "de" -> "German"
                        else -> subtitleLanguage
                    },
                    onClick = { 
                        showLanguageDialog = true
                    }
                )
            }
            
            item {
                SwitchSettingItem(
                    icon = Icons.Default.AutoAwesome,
                    title = "AI subtitle generation",
                    subtitle = "Generate subtitles using AI",
                    checked = aiSubtitleGeneration,
                    onCheckedChange = { enabled ->
                        aiSubtitleGeneration = enabled
                        coroutineScope.launch {
                            settingsRepository.setAISubtitleGenerationEnabled(enabled)
                        }
                    }
                )
            }
            
            // Audio Settings
            item {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                SettingsSectionHeader(title = "Audio")
            }
            
            item {
                ClickableSettingItem(
                    icon = Icons.Default.MusicNote,
                    title = "Preferred audio language",
                    subtitle = when(audioLanguage) {
                        "auto" -> "Original"
                        "en" -> "English"
                        "es" -> "Spanish"
                        "fr" -> "French"
                        "de" -> "German"
                        else -> audioLanguage
                    },
                    onClick = { 
                        showAudioLanguageDialog = true
                    }
                )
            }
            
            item {
                SwitchSettingItem(
                    icon = Icons.AutoMirrored.Filled.VolumeUp,
                    title = "Volume boost",
                    subtitle = "Allow volume above 100%",
                    checked = volumeBoost,
                    onCheckedChange = { enabled ->
                        volumeBoost = enabled
                        coroutineScope.launch {
                            settingsRepository.setVolumeBoostEnabled(enabled)
                        }
                    }
                )
            }
            
            // Network Settings
            item {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                SettingsSectionHeader(title = "Network")
            }
            
            item {
                SwitchSettingItem(
                    icon = Icons.Default.WifiOff,
                    title = "Stream on mobile data",
                    subtitle = "Allow streaming without Wi-Fi",
                    checked = streamOnMobileData,
                    onCheckedChange = { enabled ->
                        streamOnMobileData = enabled
                        coroutineScope.launch {
                            settingsRepository.setAllowMobileData(enabled)
                        }
                    }
                )
            }
            
            item {
                ClickableSettingItem(
                    icon = Icons.Default.HighQuality,
                    title = "Streaming quality",
                    subtitle = when(videoQuality) {
                        "auto" -> "Auto"
                        "2160p" -> "2160p (4K)"
                        "1440p" -> "1440p (2K)"
                        "1080p" -> "1080p (HD)"
                        "720p" -> "720p"
                        "480p" -> "480p"
                        else -> videoQuality
                    },
                    onClick = { 
                        showQualityDialog = true
                    }
                )
            }
            
            // Cloud Storage Settings
            item {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                SettingsSectionHeader(title = "Cloud Storage")
            }
            
            item {
                ClickableSettingItem(
                    icon = Icons.Default.Cloud,
                    title = "Cloud storage",
                    subtitle = "Connect cloud storage accounts",
                    onClick = { 
                        // Navigate to cloud storage activity
                        val intent = android.content.Intent(context, com.astralplayer.nextplayer.CloudStorageActivity::class.java)
                        context.startActivity(intent)
                    }
                )
            }
            
            // Privacy Settings
            item {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                SettingsSectionHeader(title = "Privacy")
            }
            
            item {
                SwitchSettingItem(
                    icon = Icons.Default.History,
                    title = "Save watch history",
                    subtitle = "Remember watched videos",
                    checked = saveWatchHistory,
                    onCheckedChange = { enabled ->
                        saveWatchHistory = enabled
                        coroutineScope.launch {
                            settingsRepository.setIncognitoMode(!enabled)
                        }
                    }
                )
            }
            
            item {
                ClickableSettingItem(
                    icon = Icons.Default.DeleteForever,
                    title = "Clear history",
                    subtitle = "Remove all watch history",
                    onClick = { 
                        showClearDataDialog = true
                    }
                )
            }
            
            item {
                SwitchSettingItem(
                    icon = Icons.Default.Security,
                    title = "App lock",
                    subtitle = "Require authentication to open app",
                    checked = appLock,
                    onCheckedChange = { enabled ->
                        if (enabled) {
                            // Check if biometric hardware is available
                            val biometricManager = androidx.biometric.BiometricManager.from(context)
                            when (biometricManager.canAuthenticate(androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK)) {
                                androidx.biometric.BiometricManager.BIOMETRIC_SUCCESS -> {
                                    appLock = true
                                    coroutineScope.launch {
                                        settingsRepository.setAppLockEnabled(true)
                                    }
                                }
                                androidx.biometric.BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> {
                                    // No biometric hardware
                                    android.widget.Toast.makeText(
                                        context,
                                        "No biometric hardware available",
                                        android.widget.Toast.LENGTH_LONG
                                    ).show()
                                }
                                androidx.biometric.BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> {
                                    // Hardware unavailable
                                    android.widget.Toast.makeText(
                                        context,
                                        "Biometric hardware unavailable",
                                        android.widget.Toast.LENGTH_LONG
                                    ).show()
                                }
                                androidx.biometric.BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                                    // No biometrics enrolled
                                    android.widget.Toast.makeText(
                                        context,
                                        "Please enroll biometrics in system settings",
                                        android.widget.Toast.LENGTH_LONG
                                    ).show()
                                }
                            }
                        } else {
                            appLock = false
                            coroutineScope.launch {
                                settingsRepository.setAppLockEnabled(false)
                            }
                        }
                    }
                )
            }
            
            // About
            item {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                SettingsSectionHeader(title = "About")
            }
            
            item {
                ClickableSettingItem(
                    icon = Icons.Default.Info,
                    title = "Version",
                    subtitle = "1.0.0",
                    onClick = {
                        // Show app info or version details
                        android.widget.Toast.makeText(
                            context,
                            "Astral Vu Player v1.0.0\nBuild: Debug",
                            android.widget.Toast.LENGTH_LONG
                        ).show()
                    }
                )
            }
            
            item {
                ClickableSettingItem(
                    icon = Icons.Default.Policy,
                    title = "Open source licenses",
                    subtitle = "View third-party licenses",
                    onClick = { 
                        val intent = android.content.Intent(context, com.astralplayer.nextplayer.LicensesActivity::class.java)
                        context.startActivity(intent)
                    }
                )
            }
            
            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
    
    // Playback Speed Dialog
    if (showPlaybackSpeedDialog) {
        PlaybackSpeedDialog(
            currentSpeed = defaultPlaybackSpeed,
            onSpeedSelected = { speed ->
                defaultPlaybackSpeed = speed
                coroutineScope.launch {
                    settingsRepository.setDefaultPlaybackSpeed(speed)
                }
                showPlaybackSpeedDialog = false
            },
            onDismiss = { showPlaybackSpeedDialog = false }
        )
    }
    
    // Aspect Ratio Dialog
    if (showAspectRatioDialog) {
        AspectRatioSelectorDialog(
            currentAspectRatio = aspectRatio,
            onAspectRatioSelected = { ratio ->
                aspectRatio = ratio
                coroutineScope.launch {
                    settingsRepository.setAspectRatio(ratio)
                }
            },
            onDismiss = { showAspectRatioDialog = false }
        )
    }
    
    // Language Dialog
    if (showLanguageDialog) {
        LanguageSelectorDialog(
            currentLanguage = subtitleLanguage,
            onLanguageSelected = { language ->
                subtitleLanguage = language
                coroutineScope.launch {
                    settingsRepository.setSubtitleLanguage(language)
                }
            },
            onDismiss = { showLanguageDialog = false }
        )
    }
    
    // Audio Language Dialog
    if (showAudioLanguageDialog) {
        LanguageSelectorDialog(
            currentLanguage = audioLanguage,
            onLanguageSelected = { language ->
                audioLanguage = language
                coroutineScope.launch {
                    settingsRepository.setAudioLanguage(language)
                }
            },
            onDismiss = { showAudioLanguageDialog = false }
        )
    }
    
    // Quality Dialog
    if (showQualityDialog) {
        QualitySelectorDialog(
            currentQuality = videoQuality,
            onQualitySelected = { quality ->
                videoQuality = quality
                coroutineScope.launch {
                    settingsRepository.setVideoQuality(quality)
                }
            },
            onDismiss = { showQualityDialog = false }
        )
    }
    
    // Clear Data Dialog
    if (showClearDataDialog) {
        ClearDataConfirmationDialog(
            onConfirm = {
                coroutineScope.launch {
                    // Clear all data
                    settingsRepository.clearAllData()
                }
            },
            onDismiss = { showClearDataDialog = false }
        )
    }
    
    // Subtitle Appearance Dialog
    if (showSubtitleAppearanceDialog) {
        SubtitleAppearanceDialog(
            onDismiss = { showSubtitleAppearanceDialog = false }
        )
    }
    
    // Long Press Speed Dialog
    if (showLongPressSpeedDialog) {
        LongPressSpeedSettingsDialog(
            enabled = longPressSpeedEnabled,
            initialSpeed = longPressInitialSpeed,
            progressiveEnabled = longPressProgressiveEnabled,
            onEnabledChange = { enabled ->
                longPressSpeedEnabled = enabled
                coroutineScope.launch {
                    settingsRepository.setLongPressSpeedControlEnabled(enabled)
                }
            },
            onInitialSpeedChange = { speed ->
                longPressInitialSpeed = speed
                coroutineScope.launch {
                    settingsRepository.setLongPressInitialSpeed(speed)
                }
            },
            onProgressiveEnabledChange = { enabled ->
                longPressProgressiveEnabled = enabled
                coroutineScope.launch {
                    settingsRepository.setLongPressProgressiveSpeedEnabled(enabled)
                }
            },
            onDismiss = { showLongPressSpeedDialog = false }
        )
    }
    
    // Clear Speed Memory Confirmation Dialog
    if (showClearSpeedMemoryDialog) {
        AlertDialog(
            onDismissRequest = { showClearSpeedMemoryDialog = false },
            title = { 
                Text(
                    "Clear All Speed Memory",
                    style = MaterialTheme.typography.headlineSmall
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "This will permanently remove all saved playback speeds for all videos.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "This action cannot be undone.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Medium
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        coroutineScope.launch {
                            try {
                                settingsRepository.clearVideoSpeedMemory()
                                android.widget.Toast.makeText(
                                    context,
                                    "All speed memory cleared successfully",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            } catch (e: Exception) {
                                android.widget.Toast.makeText(
                                    context,
                                    "Failed to clear speed memory: ${e.message}",
                                    android.widget.Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                        showClearSpeedMemoryDialog = false
                    }
                ) {
                    Text(
                        "Clear All",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearSpeedMemoryDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun SettingsSectionHeader(
    title: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

@Composable
private fun ClickableSettingItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier
                    .size(24.dp)
                    .padding(end = 4.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp
                )
            }
            
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
private fun SwitchSettingItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier
                    .size(24.dp)
                    .padding(end = 4.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp
                )
            }
            
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange
            )
        }
    }
}

@Composable
private fun PlaybackSpeedDialog(
    currentSpeed: Float,
    onSpeedSelected: (Float) -> Unit,
    onDismiss: () -> Unit
) {
    val speeds = listOf(0.25f, 0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f)
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Playback Speed") },
        text = {
            Column {
                speeds.forEach { speed ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSpeedSelected(speed) }
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${speed}x",
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (speed == currentSpeed) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            }
                        )
                        if (speed == currentSpeed) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun LongPressSpeedSettingsDialog(
    enabled: Boolean,
    initialSpeed: Float,
    progressiveEnabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    onInitialSpeedChange: (Float) -> Unit,
    onProgressiveEnabledChange: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val speedOptions = listOf(0.25f, 0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f, 3.0f, 4.0f, 6.0f, 8.0f)
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Text(
                "Long Press Speed Control",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            LazyColumn(
                modifier = Modifier.heightIn(max = 400.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Enable/Disable
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Enable long press speed control",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = "Long press center screen for speed control",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = enabled,
                            onCheckedChange = onEnabledChange
                        )
                    }
                }
                
                if (enabled) {
                    // Initial Speed Selection
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "Initial Speed",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Speed when long press starts",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            
                            // Speed selection grid
                            val chunkedSpeeds = speedOptions.chunked(3)
                            chunkedSpeeds.forEach { row ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    row.forEach { speed ->
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
                                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                        )
                                    }
                                    // Fill remaining space if row is not complete
                                    repeat(3 - row.size) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                    
                    // Progressive Speed Control
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Progressive speed control",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    text = if (progressiveEnabled) "Swipe up/down to change speed" else "Fixed speed only",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = progressiveEnabled,
                                onCheckedChange = onProgressiveEnabledChange
                            )
                        }
                    }
                    
                    // Info section
                    item {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = "How to use",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Text(
                                    text = "Long press the center of the screen to activate speed control. While holding, swipe up to increase speed or swipe down to decrease speed. Release to return to normal playback.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}