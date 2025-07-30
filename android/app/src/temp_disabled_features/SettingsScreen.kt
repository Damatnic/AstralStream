package com.astralplayer.nextplayer

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.astralplayer.nextplayer.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val settings by viewModel.settings.collectAsState()
    var showBufferDialog by remember { mutableStateOf(false) }
    var showVersionDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            item {
                SettingsSection(title = "Playback") {
                    SwitchPreference(
                        title = "Auto-play next",
                        subtitle = "Automatically play next video in folder",
                        checked = settings.autoPlayNext,
                        onCheckedChange = viewModel::updateAutoPlayNext
                    )
                    SwitchPreference(
                        title = "Remember playback position",
                        subtitle = "Resume videos from last position",
                        checked = settings.rememberPosition,
                        onCheckedChange = viewModel::updateRememberPosition
                    )
                    SwitchPreference(
                        title = "Hardware acceleration",
                        subtitle = "Use hardware decoding when available",
                        checked = settings.hardwareAcceleration,
                        onCheckedChange = viewModel::updateHardwareAcceleration
                    )
                }
            }
            
            item {
                SettingsSection(title = "Gestures") {
                    SwitchPreference(
                        title = "Enable gestures",
                        subtitle = "Swipe to control volume, brightness, and seek",
                        checked = settings.enableGestures,
                        onCheckedChange = viewModel::updateEnableGestures
                    )
                    SwitchPreference(
                        title = "Double tap to seek",
                        subtitle = "Double tap sides to skip forward/backward",
                        checked = settings.doubleTapSeek,
                        onCheckedChange = viewModel::updateDoubleTapSeek
                    )
                    SwitchPreference(
                        title = "Long press for speed",
                        subtitle = "Long press to toggle playback speed",
                        checked = settings.longPressSpeed,
                        onCheckedChange = viewModel::updateLongPressSpeed
                    )
                }
            }
            
            item {
                SettingsSection(title = "Display") {
                    SwitchPreference(
                        title = "Keep screen on",
                        subtitle = "Prevent screen from turning off during playback",
                        checked = settings.keepScreenOn,
                        onCheckedChange = viewModel::updateKeepScreenOn
                    )
                    SwitchPreference(
                        title = "Show video info",
                        subtitle = "Display codec and resolution info",
                        checked = settings.showVideoInfo,
                        onCheckedChange = viewModel::updateShowVideoInfo
                    )
                    SwitchPreference(
                        title = "Auto-load subtitles",
                        subtitle = "Automatically load subtitle files",
                        checked = settings.autoLoadSubtitles,
                        onCheckedChange = viewModel::updateAutoLoadSubtitles
                    )
                }
            }
            
            item {
                SettingsSection(title = "Network") {
                    SwitchPreference(
                        title = "Wi-Fi only streaming",
                        subtitle = "Stream videos only on Wi-Fi connection",
                        checked = settings.wifiOnlyStreaming,
                        onCheckedChange = viewModel::updateWifiOnlyStreaming
                    )
                    ClickablePreference(
                        title = "Buffer size",
                        subtitle = settings.bufferSize,
                        onClick = {
                            showBufferDialog = true
                        }
                    )
                }
            }
            
            item {
                SettingsSection(title = "About") {
                    ClickablePreference(
                        title = "Version",
                        subtitle = "1.0.0",
                        onClick = { showVersionDialog = true }
                    )
                    ClickablePreference(
                        title = "Open source licenses",
                        subtitle = "View third-party licenses",
                        onClick = {
                            val intent = android.content.Intent(context, LicensesActivity::class.java)
                            context.startActivity(intent)
                        }
                    )
                    ClickablePreference(
                        title = "Privacy policy",
                        subtitle = "View privacy policy",
                        onClick = {
                            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://astralplayer.com/privacy"))
                            context.startActivity(intent)
                        }
                    )
                }
            }
        }
        
        if (showBufferDialog) {
            BufferSizeDialog(
                currentSize = settings.bufferSize,
                onDismiss = { showBufferDialog = false },
                onSizeSelected = { size ->
                    viewModel.updateBufferSize(size)
                    showBufferDialog = false
                }
            )
        }
        
        if (showVersionDialog) {
            VersionInfoDialog(
                onDismiss = { showVersionDialog = false }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VersionInfoDialog(
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text("Version Information")
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Version info
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Version",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "1.0.0",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Build",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "100",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Release Date",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "January 26, 2025",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
                
                // What's new section
                Text(
                    text = "What's New",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ChangelogItem("✨", "Advanced gesture controls for volume, brightness, and seeking")
                        ChangelogItem("🎬", "Support for all major video formats including MKV, MP4, AVI")
                        ChangelogItem("📂", "Intuitive folder browsing with playlist integration")
                        ChangelogItem("🎯", "Double-tap to seek forward/backward")
                        ChangelogItem("🌐", "Stream videos from URLs and handle browser intents")
                        ChangelogItem("💾", "Remember playback position across sessions")
                        ChangelogItem("🎨", "Beautiful modern UI with smooth animations")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("OK")
            }
        }
    )
}

@Composable
private fun ChangelogItem(
    emoji: String,
    text: String
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = emoji,
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun SettingsSection(
    title: String,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            content()
        }
    }
}

@Composable
fun SwitchPreference(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
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
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
fun ClickablePreference(
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
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
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BufferSizeDialog(
    currentSize: String,
    onDismiss: () -> Unit,
    onSizeSelected: (String) -> Unit
) {
    var selectedSize by remember { mutableStateOf(currentSize) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Buffer Size") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Select buffer size for streaming videos",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                val sizes = listOf("Small", "Medium", "Large", "Extra Large")
                val descriptions = listOf(
                    "Lower memory usage, may buffer more often",
                    "Balanced performance and memory usage",
                    "Better for high-quality streams",
                    "Best for 4K/high bitrate content"
                )
                
                sizes.forEachIndexed { index, size ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedSize = size }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedSize == size,
                            onClick = { selectedSize = size }
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = size,
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = descriptions[index],
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSizeSelected(selectedSize) }
            ) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}