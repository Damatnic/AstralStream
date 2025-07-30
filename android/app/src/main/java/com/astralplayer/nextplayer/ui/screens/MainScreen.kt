package com.astralplayer.nextplayer.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.astralplayer.nextplayer.MainActivity
import com.astralplayer.nextplayer.ui.components.*
import com.astralplayer.nextplayer.SearchActivity
import com.astralplayer.nextplayer.SettingsActivity
import com.astralplayer.nextplayer.RecentFilesActivity  
import com.astralplayer.nextplayer.FolderBrowserActivity
import com.astralplayer.nextplayer.PlaylistActivity
import com.astralplayer.nextplayer.CloudStorageActivity
import com.astralplayer.nextplayer.VideoPlayerActivity
import com.astralplayer.nextplayer.data.database.AstralVuDatabase
import com.astralplayer.nextplayer.data.repository.RecentFilesRepository
import com.astralplayer.nextplayer.data.repository.RecentFilesRepositoryImpl
import com.astralplayer.nextplayer.data.repository.SettingsRepository
import com.astralplayer.nextplayer.data.repository.SettingsRepositoryImpl
import com.astralplayer.nextplayer.security.SecurityManager
import com.astralplayer.nextplayer.feature.security.AdultContentFilter
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@Composable
fun ModernMainScreen() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Recent", "Library", "Cloud", "Playlists")
    
    // Initialize repositories and managers
    val database = remember { com.astralplayer.nextplayer.di.DatabaseModule.getDatabase(context) }
    val recentFilesRepository = remember { RecentFilesRepositoryImpl(database.recentFilesDao()) }
    val settingsRepository = remember { SettingsRepositoryImpl(context) }
    val securityManager = remember { SecurityManager(context) }
    val contentFilter = remember { AdultContentFilter(context) }
    
    // Privacy and security states
    var isPrivateModeEnabled by remember { mutableStateOf(false) }
    var adultContentFilterLevel by remember { mutableStateOf(2) } // Safe by default
    var showPrivacyMenu by remember { mutableStateOf(false) }
    
    // Load settings
    LaunchedEffect(Unit) {
        launch {
            settingsRepository.getIncognitoMode().collectLatest { enabled ->
                isPrivateModeEnabled = enabled
            }
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
                    )
                )
            )
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Modern Header with Privacy Controls
            BubbleTopAppBar(
                title = "AstralStream",
                actions = {
                    // Privacy Mode Toggle
                    AnimatedVisibility(
                        visible = true,
                        enter = fadeIn() + slideInHorizontally(),
                        exit = fadeOut() + slideOutHorizontally()
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Privacy Mode Indicator
                            BubbleIconButton(
                                onClick = { showPrivacyMenu = !showPrivacyMenu },
                                icon = if (isPrivateModeEnabled) Icons.Default.Lock else Icons.Default.LockOpen,
                                size = 40,
                                iconSize = 18,
                                containerColor = if (isPrivateModeEnabled) 
                                    MaterialTheme.colorScheme.error.copy(alpha = 0.8f) else 
                                    MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (isPrivateModeEnabled) 
                                    MaterialTheme.colorScheme.onError else 
                                    MaterialTheme.colorScheme.onSurfaceVariant,
                                contentDescription = if (isPrivateModeEnabled) "Private mode on" else "Private mode off"
                            )
                            
                            // Search Button
                            BubbleIconButton(
                                onClick = {
                                    val intent = Intent(context, SearchActivity::class.java)
                                    context.startActivity(intent)
                                },
                                icon = Icons.Default.Search,
                                size = 40,
                                iconSize = 18,
                                contentDescription = "Search"
                            )
                            
                            // Settings Button
                            BubbleIconButton(
                                onClick = {
                                    val intent = Intent(context, SettingsActivity::class.java)
                                    context.startActivity(intent)
                                },
                                icon = Icons.Default.Settings,
                                size = 40,
                                iconSize = 18,
                                contentDescription = "Settings"
                            )
                        }
                    }
                }
            )
            
            // Privacy Menu Dropdown
            AnimatedVisibility(
                visible = showPrivacyMenu,
                enter = slideInVertically() + expandVertically() + fadeIn(),
                exit = slideOutVertically() + shrinkVertically() + fadeOut()
            ) {
                PrivacyControlsMenu(
                    isPrivateModeEnabled = isPrivateModeEnabled,
                    contentFilterLevel = adultContentFilterLevel,
                    onPrivateModeToggle = { enabled ->
                        isPrivateModeEnabled = enabled
                        coroutineScope.launch {
                            settingsRepository.setIncognitoMode(enabled)
                            securityManager.setPrivateModeEnabled(enabled)
                        }
                    },
                    onContentFilterChange = { level ->
                        adultContentFilterLevel = level
                    },
                    onDismiss = { showPrivacyMenu = false }
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Modern Tab Row
            BubbleTabRow(
                selectedTabIndex = selectedTab,
                tabs = tabs,
                onTabSelected = { selectedTab = it },
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Tab Content
            when (selectedTab) {
                0 -> ModernRecentVideosContent(
                    recentFilesRepository = recentFilesRepository,
                    isPrivateMode = isPrivateModeEnabled
                )
                1 -> ModernLibraryContent()
                2 -> ModernCloudContent()
                3 -> ModernPlaylistsContent()
            }
        }
    }
}

@Composable
private fun PrivacyControlsMenu(
    isPrivateModeEnabled: Boolean,
    contentFilterLevel: Int,
    onPrivateModeToggle: (Boolean) -> Unit,
    onContentFilterChange: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    GlassmorphismCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        cornerRadius = 16,
        glassStrength = 0.15f
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Privacy Controls",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                BubbleIconButton(
                    onClick = onDismiss,
                    icon = Icons.Default.Close,
                    size = 32,
                    iconSize = 16,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            }
            
            // Private Mode Toggle
            BubbleListItem(
                headlineText = "Private Mode",
                supportingText = if (isPrivateModeEnabled) "Secure browsing enabled" else "Normal browsing mode",
                leadingContent = {
                    Icon(
                        imageVector = if (isPrivateModeEnabled) Icons.Default.Lock else Icons.Default.LockOpen,
                        contentDescription = null,
                        tint = if (isPrivateModeEnabled) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                },
                trailingContent = {
                    Switch(
                        checked = isPrivateModeEnabled,
                        onCheckedChange = onPrivateModeToggle,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.onError,
                            checkedTrackColor = MaterialTheme.colorScheme.error
                        )
                    )
                },
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
            )
            
            // Content Filter Level
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Content Filter Level",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                val filterLabels = listOf("Off", "Mild", "Moderate", "Strict", "Maximum")
                
                BubbleCard(
                    cornerRadius = 12,
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Slider(
                            value = contentFilterLevel.toFloat(),
                            onValueChange = { onContentFilterChange(it.toInt()) },
                            valueRange = 0f..4f,
                            steps = 3,
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.primary,
                                activeTrackColor = MaterialTheme.colorScheme.primary
                            )
                        )
                        
                        Text(
                            text = "Current: ${filterLabels[contentFilterLevel]}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

// Update the original MainScreen function to use the new modern version
@Composable
fun MainScreen() {
    ModernMainScreen()
}

@Composable
fun ModernRecentVideosContent(
    recentFilesRepository: RecentFilesRepository,
    isPrivateMode: Boolean
) {
    val context = LocalContext.current
    var recentFiles by remember { mutableStateOf(emptyList<com.astralplayer.nextplayer.data.RecentFile>()) }
    var isLoading by remember { mutableStateOf(true) }
    
    LaunchedEffect(Unit) {
        recentFilesRepository.getAllRecentFiles().collect { files ->
            recentFiles = files.take(6) // Show only latest 6
            isLoading = false
        }
    }
    
    if (isLoading) {
        BubbleLoadingState(
            message = "Loading recent videos...",
            modifier = Modifier.fillMaxSize()
        )
    } else if (recentFiles.isEmpty()) {
        BubbleEmptyState(
            icon = Icons.Default.VideoLibrary,
            title = "No Recent Videos",
            subtitle = "Videos you play will appear here. Try playing a video or browse your library.",
            actionButton = {
                BubbleButton(
                    onClick = {
                        val intent = Intent(context, RecentFilesActivity::class.java)
                        context.startActivity(intent)
                    },
                    text = "Browse Library",
                    icon = Icons.Default.VideoLibrary
                )
            }
        )
    } else {
        BubbleLazyColumn {
            // Privacy mode indicator
            if (isPrivateMode) {
                item {
                    BubbleCard(
                        cornerRadius = 12,
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                Icons.Default.Lock,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Private Mode Active - Adult content accessible",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
            
            // Recent videos grid
            items(recentFiles.chunked(2)) { rowFiles ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    rowFiles.forEach { file ->
                        ModernVideoCard(
                            recentFile = file,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                val intent = Intent(context, VideoPlayerActivity::class.java).apply {
                                    data = Uri.parse(file.uri)
                                    putExtra("video_title", file.title)
                                }
                                context.startActivity(intent)
                            }
                        )
                    }
                    // Fill remaining space if odd number
                    if (rowFiles.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
            
            // Show more button
            if (recentFiles.size >= 6) {
                item {
                    BubbleButton(
                        onClick = {
                            val intent = Intent(context, RecentFilesActivity::class.java)
                            context.startActivity(intent)
                        },
                        text = "View All Recent Videos",
                        icon = Icons.Default.ArrowForward,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun ModernVideoCard(
    recentFile: com.astralplayer.nextplayer.data.RecentFile,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    BubbleCard(
        modifier = modifier,
        onClick = onClick,
        elevation = 6,
        cornerRadius = 16
    ) {
        Column {
            // Thumbnail
            BubbleVideoThumbnail(
                uri = recentFile.uri,
                duration = recentFile.duration,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                showDuration = true,
                showPlayIcon = true,
                cornerRadius = 12
            )
            
            // Content
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = recentFile.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Text(
                    text = formatTime(recentFile.duration),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                // Progress indicator
                if (recentFile.lastPosition > 0 && recentFile.duration > 0) {
                    val progress = recentFile.lastPosition.toFloat() / recentFile.duration
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .clip(RoundedCornerShape(1.5.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    )
                }
            }
        }
    }
}

private fun formatTime(timeMs: Long): String {
    val totalSeconds = timeMs / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    
    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%d:%02d", minutes, seconds)
    }
}

@Composable
fun RecentVideosContent() {
    val context = LocalContext.current
    val database = remember { com.astralplayer.nextplayer.di.DatabaseModule.getDatabase(context) }
    val recentFilesRepository = remember { RecentFilesRepositoryImpl(database.recentFilesDao()) }
    
    ModernRecentVideosContent(
        recentFilesRepository = recentFilesRepository,
        isPrivateMode = false
    )
}

@Composable
fun ModernLibraryContent() {
    val context = LocalContext.current
    
    val requestPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            val intent = Intent(context, FolderBrowserActivity::class.java)
            context.startActivity(intent)
        }
    }
    
    val hasPermission = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_VIDEO) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        }
    }
    
    if (hasPermission) {
        BubbleLazyColumn {
            // Quick Actions
            item {
                Text(
                    text = "Quick Actions",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
            
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    BubbleCard(
                        modifier = Modifier.weight(1f),
                        onClick = {
                            val intent = Intent(context, FolderBrowserActivity::class.java)
                            context.startActivity(intent)
                        },
                        elevation = 6,
                        cornerRadius = 16
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.VideoLibrary,
                                contentDescription = null,
                                modifier = Modifier.size(32.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Browse Videos",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    
                    BubbleCard(
                        modifier = Modifier.weight(1f),
                        onClick = {
                            val intent = Intent(context, SearchActivity::class.java)
                            context.startActivity(intent)
                        },
                        elevation = 6,
                        cornerRadius = 16
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = null,
                                modifier = Modifier.size(32.dp),
                                tint = MaterialTheme.colorScheme.secondary
                            )
                            Text(
                                text = "Search",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
            
            // Library Statistics
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Library Stats",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
            
            item {
                BubbleCard(
                    cornerRadius = 16,
                    elevation = 4
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        LibraryStatItem(
                            icon = Icons.Default.Movie,
                            label = "Total Videos",
                            value = "Scanning..."
                        )
                        
                        LibraryStatItem(
                            icon = Icons.Default.Folder,
                            label = "Folders",
                            value = "Scanning..."
                        )
                        
                        LibraryStatItem(
                            icon = Icons.Default.Storage,
                            label = "Total Size",
                            value = "Calculating..."
                        )
                    }
                }
            }
        }
    } else {
        BubbleEmptyState(
            icon = Icons.Default.Movie,
            title = "Storage Permission Needed",
            subtitle = "Grant storage permission to access your video library and enjoy your content.",
            actionButton = {
                BubbleButton(
                    onClick = {
                        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            Manifest.permission.READ_MEDIA_VIDEO
                        } else {
                            Manifest.permission.READ_EXTERNAL_STORAGE
                        }
                        requestPermissionLauncher.launch(permission)
                    },
                    text = "Grant Permission",
                    icon = Icons.Default.Lock
                )
            }
        )
    }
}

@Composable
private fun LibraryStatItem(
    icon: ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium
            )
        }
        
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
fun AllVideosContent() {
    ModernLibraryContent()
}

@Composable
fun ModernCloudContent() {
    val context = LocalContext.current
    
    BubbleLazyColumn {
        // Header
        item {
            Text(
                text = "Cloud Storage",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }
        
        // Cloud providers
        item {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Google Drive
                BubbleListItem(
                    headlineText = "Google Drive",
                    supportingText = "Access videos from your Google Drive",
                    leadingContent = {
                        Icon(
                            Icons.Default.CloudQueue,
                            contentDescription = null,
                            modifier = Modifier.size(32.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    trailingContent = {
                        BubbleIconButton(
                            onClick = {
                                val intent = Intent(context, CloudStorageActivity::class.java).apply {
                                    putExtra("provider", "google_drive")
                                }
                                context.startActivity(intent)
                            },
                            icon = Icons.Default.ArrowForward,
                            size = 36,
                            iconSize = 16
                        )
                    }
                )
                
                // OneDrive
                BubbleListItem(
                    headlineText = "OneDrive",
                    supportingText = "Access videos from Microsoft OneDrive",
                    leadingContent = {
                        Icon(
                            Icons.Default.Cloud,
                            contentDescription = null,
                            modifier = Modifier.size(32.dp),
                            tint = MaterialTheme.colorScheme.secondary
                        )
                    },
                    trailingContent = {
                        BubbleIconButton(
                            onClick = {
                                val intent = Intent(context, CloudStorageActivity::class.java).apply {
                                    putExtra("provider", "onedrive")
                                }
                                context.startActivity(intent)
                            },
                            icon = Icons.Default.ArrowForward,
                            size = 36,
                            iconSize = 16
                        )
                    }
                )
                
                // Dropbox
                BubbleListItem(
                    headlineText = "Dropbox",
                    supportingText = "Access videos from Dropbox",
                    leadingContent = {
                        Icon(
                            Icons.Default.CloudDownload,
                            contentDescription = null,
                            modifier = Modifier.size(32.dp),
                            tint = MaterialTheme.colorScheme.tertiary
                        )
                    },
                    trailingContent = {
                        BubbleIconButton(
                            onClick = {
                                val intent = Intent(context, CloudStorageActivity::class.java).apply {
                                    putExtra("provider", "dropbox")
                                }
                                context.startActivity(intent)
                            },
                            icon = Icons.Default.ArrowForward,
                            size = 36,
                            iconSize = 16
                        )
                    }
                )
            }
        }
        
        // Network streaming
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Network Streaming",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }
        
        item {
            BubbleListItem(
                headlineText = "Stream from URL",
                supportingText = "Play videos directly from web URLs",
                leadingContent = {
                    Icon(
                        Icons.Default.Stream,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                onClick = {
                    // Show URL input dialog
                }
            )
        }
        
        // Adult content integration info
        item {
            Spacer(modifier = Modifier.height(16.dp))
            BubbleCard(
                cornerRadius = 12,
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(24.dp)
                    )
                    Column {
                        Text(
                            text = "Adult Content Support",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "Open adult videos from browsers with 'Open with' feature. Enable private mode for secure access.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FoldersContent() {
    ModernCloudContent()
}

@Composable
fun ModernPlaylistsContent() {
    val context = LocalContext.current
    
    BubbleLazyColumn {
        // Header with Create button
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "My Playlists",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                BubbleButton(
                    onClick = {
                        val intent = Intent(context, PlaylistActivity::class.java).apply {
                            putExtra("action", "create")
                        }
                        context.startActivity(intent)
                    },
                    text = "Create",
                    icon = Icons.Default.Add
                )
            }
        }
        
        // Smart playlists
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Smart Playlists",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }
        
        item {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Recently Added
                BubbleListItem(
                    headlineText = "Recently Added",
                    supportingText = "Latest videos from your library",
                    leadingContent = {
                        Icon(
                            Icons.Default.NewReleases,
                            contentDescription = null,
                            modifier = Modifier.size(28.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    trailingContent = {
                        Text(
                            text = "Auto",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                    },
                    onClick = {
                        val intent = Intent(context, PlaylistActivity::class.java).apply {
                            putExtra("playlist_type", "recently_added")
                        }
                        context.startActivity(intent)
                    }
                )
                
                // Most Watched
                BubbleListItem(
                    headlineText = "Most Watched",
                    supportingText = "Your frequently played videos",
                    leadingContent = {
                        Icon(
                            Icons.Default.Whatshot,
                            contentDescription = null,
                            modifier = Modifier.size(28.dp),
                            tint = MaterialTheme.colorScheme.secondary
                        )
                    },
                    trailingContent = {
                        Text(
                            text = "Auto",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary,
                            fontWeight = FontWeight.Medium
                        )
                    },
                    onClick = {
                        val intent = Intent(context, PlaylistActivity::class.java).apply {
                            putExtra("playlist_type", "most_watched")
                        }
                        context.startActivity(intent)
                    }
                )
                
                // Favorites
                BubbleListItem(
                    headlineText = "Favorites",
                    supportingText = "Your starred and bookmarked videos",
                    leadingContent = {
                        Icon(
                            Icons.Default.Favorite,
                            contentDescription = null,
                            modifier = Modifier.size(28.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                    },
                    trailingContent = {
                        Text(
                            text = "0",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    onClick = {
                        val intent = Intent(context, PlaylistActivity::class.java).apply {
                            putExtra("playlist_type", "favorites")
                        }
                        context.startActivity(intent)
                    }
                )
            }
        }
        
        // Empty state for custom playlists
        item {
            Spacer(modifier = Modifier.height(24.dp))
            BubbleEmptyState(
                icon = Icons.Default.PlaylistPlay,
                title = "No Custom Playlists",
                subtitle = "Create playlists to organize your videos by genre, mood, or any category you like.",
                actionButton = {
                    BubbleButton(
                        onClick = {
                            val intent = Intent(context, PlaylistActivity::class.java)
                            context.startActivity(intent)
                        },
                        text = "Create Playlist",
                        icon = Icons.Default.Add
                    )
                }
            )
        }
    }
}

@Composable
fun PlaylistsContent() {
    ModernPlaylistsContent()
}