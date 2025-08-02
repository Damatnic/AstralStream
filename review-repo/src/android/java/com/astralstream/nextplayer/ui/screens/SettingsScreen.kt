package com.astralstream.nextplayer.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.astralstream.nextplayer.navigation.Routes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    onNavigateBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            // General Settings Section
            item {
                SettingsSection(title = "General")
            }
            
            item {
                SettingsItem(
                    icon = Icons.Default.Language,
                    title = "Language",
                    subtitle = "App language settings",
                    onClick = { /* Navigate to language settings */ }
                )
            }
            
            item {
                SettingsItem(
                    icon = Icons.Default.DarkMode,
                    title = "Theme",
                    subtitle = "Dark mode and appearance",
                    onClick = { /* Navigate to theme settings */ }
                )
            }
            
            // New Features Section
            item {
                SettingsSection(title = "Features")
            }
            
            item {
                SettingsItem(
                    icon = Icons.Default.Group,
                    title = "Community",
                    subtitle = "Share playlists and contribute subtitles",
                    onClick = { navController.navigate(Routes.COMMUNITY) }
                )
            }
            
            item {
                SettingsItem(
                    icon = Icons.Default.TouchApp,
                    title = "Gesture Controls",
                    subtitle = "Customize player gestures",
                    onClick = { navController.navigate(Routes.GESTURE_CUSTOMIZATION) }
                )
            }
            
            item {
                SettingsItem(
                    icon = Icons.Default.Analytics,
                    title = "Analytics Dashboard",
                    subtitle = "View your watching statistics",
                    onClick = { navController.navigate(Routes.ANALYTICS_DASHBOARD) }
                )
            }
            
            // Player Settings Section
            item {
                SettingsSection(title = "Player")
            }
            
            item {
                SettingsItem(
                    icon = Icons.Default.Subtitles,
                    title = "Subtitles",
                    subtitle = "Subtitle preferences and cache",
                    onClick = { /* Navigate to subtitle settings */ }
                )
            }
            
            item {
                SettingsItem(
                    icon = Icons.Default.Speed,
                    title = "Playback",
                    subtitle = "Default playback speed and quality",
                    onClick = { /* Navigate to playback settings */ }
                )
            }
            
            // Storage Section
            item {
                SettingsSection(title = "Storage")
            }
            
            item {
                SettingsItem(
                    icon = Icons.Default.Storage,
                    title = "Cache Management",
                    subtitle = "Clear cache and manage storage",
                    onClick = { /* Navigate to cache management */ }
                )
            }
            
            // About Section
            item {
                SettingsSection(title = "About")
            }
            
            item {
                SettingsItem(
                    icon = Icons.Default.Info,
                    title = "About AstralStream",
                    subtitle = "Version 1.0.0",
                    onClick = { /* Show about dialog */ }
                )
            }
        }
    }
}

@Composable
private fun SettingsSection(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

@Composable
private fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
            
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        }
    }
}