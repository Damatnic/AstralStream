package com.astralplayer.nextplayer.ui.dialogs

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.astralplayer.nextplayer.data.RecentFile
import com.astralplayer.nextplayer.feature.social.*
import com.astralplayer.nextplayer.ui.components.*
import com.astralplayer.nextplayer.ui.theme.glassmorphicSurface
import kotlinx.coroutines.launch

/**
 * Social Sharing Dialog
 * Comprehensive sharing interface for videos and playlists
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SocialSharingDialog(
    video: RecentFile? = null,
    playlist: com.astralplayer.nextplayer.feature.playlist.Playlist? = null,
    playlistVideos: List<com.astralplayer.nextplayer.feature.playlist.PlaylistVideo>? = null,
    sharingManager: SocialSharingManager,
    onDismiss: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.8f)
                .glassmorphicSurface(
                    cornerRadius = 28.dp,
                    glassColor = MaterialTheme.colorScheme.surface,
                    glassAlpha = 0.95f,
                    blurRadius = 16.dp,
                    borderWidth = 1.dp,
                    borderBrush = Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                            MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f)
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Header
                SharingDialogHeader(
                    title = when {
                        video != null -> "Share Video"
                        playlist != null -> "Share Playlist"
                        else -> "Share"
                    },
                    onDismiss = onDismiss
                )
                
                // Tab Row
                if (video != null) {
                    BubbleTabRow(
                        selectedTabIndex = selectedTab,
                        tabs = listOf("Quick Share", "Social Media", "Moment", "Watch Party"),
                        onTabSelected = { selectedTab = it },
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                } else if (playlist != null) {
                    BubbleTabRow(
                        selectedTabIndex = selectedTab,
                        tabs = listOf("Share", "Collaborate", "Export"),
                        onTabSelected = { selectedTab = it },
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Content
                when {
                    video != null -> {
                        VideoSharingContent(
                            video = video,
                            selectedTab = selectedTab,
                            sharingManager = sharingManager,
                            onDismiss = onDismiss
                        )
                    }
                    playlist != null && playlistVideos != null -> {
                        PlaylistSharingContent(
                            playlist = playlist,
                            videos = playlistVideos,
                            selectedTab = selectedTab,
                            sharingManager = sharingManager,
                            onDismiss = onDismiss
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SharingDialogHeader(
    title: String,
    onDismiss: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            
            BubbleIconButton(
                onClick = onDismiss,
                icon = Icons.Default.Close,
                size = 36,
                iconSize = 20
            )
        }
    }
}

@Composable
private fun VideoSharingContent(
    video: RecentFile,
    selectedTab: Int,
    sharingManager: SocialSharingManager,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()
    
    AnimatedContent(
        targetState = selectedTab,
        transitionSpec = {
            slideInHorizontally { width -> width } + fadeIn() with
            slideOutHorizontally { width -> -width } + fadeOut()
        }
    ) { tab ->
        when (tab) {
            0 -> QuickShareTab(
                video = video,
                sharingManager = sharingManager,
                onDismiss = onDismiss
            )
            1 -> SocialMediaTab(
                video = video,
                sharingManager = sharingManager,
                onDismiss = onDismiss
            )
            2 -> MomentShareTab(
                video = video,
                sharingManager = sharingManager,
                onDismiss = onDismiss
            )
            3 -> WatchPartyTab(
                video = video,
                sharingManager = sharingManager,
                onDismiss = onDismiss
            )
        }
    }
}

@Composable
private fun QuickShareTab(
    video: RecentFile,
    sharingManager: SocialSharingManager,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var includeTitle by remember { mutableStateOf(true) }
    var includeDescription by remember { mutableStateOf(true) }
    var customDescription by remember { mutableStateOf("") }
    
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Share options
        item {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Share Options",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ShareOptionCard(
                        icon = Icons.Default.Link,
                        label = "Share Link",
                        description = "Generate shareable link",
                        onClick = {
                            scope.launch {
                                sharingManager.shareVideo(
                                    video,
                                    ShareOptions(
                                        shareType = ShareType.LINK,
                                        includeTitle = includeTitle,
                                        includeDescription = includeDescription,
                                        customDescription = customDescription.ifEmpty { null }
                                    )
                                )
                                onDismiss()
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                    
                    ShareOptionCard(
                        icon = Icons.Default.FileUpload,
                        label = "Share File",
                        description = "Send video file",
                        onClick = {
                            scope.launch {
                                sharingManager.shareVideo(
                                    video,
                                    ShareOptions(
                                        shareType = ShareType.FILE,
                                        includeTitle = includeTitle,
                                        includeDescription = includeDescription,
                                        customDescription = customDescription.ifEmpty { null }
                                    )
                                )
                                onDismiss()
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
                
                ShareOptionCard(
                    icon = Icons.Default.MovieFilter,
                    label = "Share Preview",
                    description = "Create and share 30s preview",
                    onClick = {
                        scope.launch {
                            sharingManager.shareVideo(
                                video,
                                ShareOptions(
                                    shareType = ShareType.PREVIEW,
                                    includeTitle = includeTitle,
                                    includeDescription = includeDescription,
                                    customDescription = customDescription.ifEmpty { null },
                                    previewDuration = 30,
                                    previewQuality = PreviewQuality.MEDIUM
                                )
                            )
                            onDismiss()
                        }
                    }
                )
            }
        }
        
        // Customization options
        item {
            BubbleCard(
                elevation = 2,
                cornerRadius = 20
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Customize Share",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Include video title")
                        Switch(
                            checked = includeTitle,
                            onCheckedChange = { includeTitle = it }
                        )
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Include description")
                        Switch(
                            checked = includeDescription,
                            onCheckedChange = { includeDescription = it }
                        )
                    }
                    
                    if (includeDescription) {
                        OutlinedTextField(
                            value = customDescription,
                            onValueChange = { customDescription = it },
                            label = { Text("Custom description (optional)") },
                            placeholder = { Text("Add a personal message...") },
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 3
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SocialMediaTab(
    video: RecentFile,
    sharingManager: SocialSharingManager,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val platforms = listOf(
        SocialPlatformInfo("Twitter", Icons.Default.Tag, Color(0xFF1DA1F2)),
        SocialPlatformInfo("Facebook", Icons.Default.ThumbUp, Color(0xFF1877F2)),
        SocialPlatformInfo("Instagram", Icons.Default.PhotoCamera, Color(0xFFE4405F)),
        SocialPlatformInfo("WhatsApp", Icons.Default.Chat, Color(0xFF25D366)),
        SocialPlatformInfo("Telegram", Icons.Default.Send, Color(0xFF0088CC))
    )
    
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(platforms) { platform ->
            SocialPlatformCard(
                platform = platform,
                onClick = {
                    scope.launch {
                        sharingManager.shareVideo(
                            video,
                            ShareOptions(
                                shareType = ShareType.LINK,
                                includeTitle = true,
                                includeDescription = true
                            )
                        )
                        onDismiss()
                    }
                }
            )
        }
    }
}

@Composable
private fun MomentShareTab(
    video: RecentFile,
    sharingManager: SocialSharingManager,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var currentTimestamp by remember { mutableStateOf(0L) }
    var note by remember { mutableStateOf("") }
    var includeThumbnail by remember { mutableStateOf(true) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Timestamp selector
        BubbleCard(
            elevation = 2,
            cornerRadius = 20
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Select Moment",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Text(
                    text = "Current: ${formatTimestamp(currentTimestamp)}",
                    style = MaterialTheme.typography.bodyLarge
                )
                
                Slider(
                    value = currentTimestamp.toFloat(),
                    onValueChange = { currentTimestamp = it.toLong() },
                    valueRange = 0f..video.duration.toFloat(),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        
        // Note input
        OutlinedTextField(
            value = note,
            onValueChange = { note = it },
            label = { Text("Add a note (optional)") },
            placeholder = { Text("What's special about this moment?") },
            modifier = Modifier.fillMaxWidth(),
            maxLines = 3
        )
        
        // Options
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Include thumbnail")
            Switch(
                checked = includeThumbnail,
                onCheckedChange = { includeThumbnail = it }
            )
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        // Share button
        BubbleButton(
            onClick = {
                scope.launch {
                    sharingManager.shareMoment(
                        video = video,
                        timestamp = currentTimestamp,
                        note = note.ifEmpty { null },
                        includeThumbnail = includeThumbnail
                    )
                    onDismiss()
                }
            },
            text = "Share Moment",
            icon = Icons.Default.Share,
            modifier = Modifier.fillMaxWidth(),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        )
    }
}

@Composable
private fun WatchPartyTab(
    video: RecentFile,
    sharingManager: SocialSharingManager,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var hostName by remember { mutableStateOf("") }
    var syncPlayback by remember { mutableStateOf(true) }
    var chatEnabled by remember { mutableStateOf(true) }
    var maxParticipants by remember { mutableStateOf("50") }
    var isPrivate by remember { mutableStateOf(false) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Host info
        OutlinedTextField(
            value = hostName,
            onValueChange = { hostName = it },
            label = { Text("Your name") },
            placeholder = { Text("Enter host name") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        
        // Party settings
        BubbleCard(
            elevation = 2,
            cornerRadius = 20
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Party Settings",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                SettingRow(
                    label = "Sync playback",
                    description = "Everyone watches in sync",
                    checked = syncPlayback,
                    onCheckedChange = { syncPlayback = it }
                )
                
                SettingRow(
                    label = "Enable chat",
                    description = "Allow participants to chat",
                    checked = chatEnabled,
                    onCheckedChange = { chatEnabled = it }
                )
                
                SettingRow(
                    label = "Private party",
                    description = "Require invite link to join",
                    checked = isPrivate,
                    onCheckedChange = { isPrivate = it }
                )
                
                OutlinedTextField(
                    value = maxParticipants,
                    onValueChange = { if (it.all { char -> char.isDigit() }) maxParticipants = it },
                    label = { Text("Max participants") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        // Create button
        BubbleButton(
            onClick = {
                if (hostName.isNotEmpty()) {
                    scope.launch {
                        val party = sharingManager.createWatchParty(
                            video = video,
                            partyOptions = WatchPartyOptions(
                                hostName = hostName,
                                syncPlayback = syncPlayback,
                                chatEnabled = chatEnabled,
                                maxParticipants = maxParticipants.toIntOrNull() ?: 50,
                                isPrivate = isPrivate
                            )
                        )
                        // Show party created dialog with invite link
                        onDismiss()
                    }
                }
            },
            text = "Create Watch Party",
            icon = Icons.Default.Groups,
            modifier = Modifier.fillMaxWidth(),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            enabled = hostName.isNotEmpty()
        )
    }
}

@Composable
private fun PlaylistSharingContent(
    playlist: com.astralplayer.nextplayer.feature.playlist.Playlist,
    videos: List<com.astralplayer.nextplayer.feature.playlist.PlaylistVideo>,
    selectedTab: Int,
    sharingManager: SocialSharingManager,
    onDismiss: () -> Unit
) {
    AnimatedContent(
        targetState = selectedTab,
        transitionSpec = {
            slideInHorizontally { width -> width } + fadeIn() with
            slideOutHorizontally { width -> -width } + fadeOut()
        }
    ) { tab ->
        when (tab) {
            0 -> PlaylistShareTab(
                playlist = playlist,
                videos = videos,
                sharingManager = sharingManager,
                onDismiss = onDismiss
            )
            1 -> CollaborativeTab(
                playlist = playlist,
                videos = videos,
                sharingManager = sharingManager,
                onDismiss = onDismiss
            )
            2 -> ExportTab(
                playlist = playlist,
                videos = videos,
                sharingManager = sharingManager,
                onDismiss = onDismiss
            )
        }
    }
}

@Composable
private fun PlaylistShareTab(
    playlist: com.astralplayer.nextplayer.feature.playlist.Playlist,
    videos: List<com.astralplayer.nextplayer.feature.playlist.PlaylistVideo>,
    sharingManager: SocialSharingManager,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()
    
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Playlist info
        item {
            BubbleCard(
                elevation = 2,
                cornerRadius = 20
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = playlist.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${videos.size} videos • ${formatDuration(videos.sumOf { it.duration })}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (playlist.description.isNotEmpty()) {
                        Text(
                            text = playlist.description,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
        
        // Share to social
        item {
            Text(
                text = "Share to",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
        
        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(SocialPlatform.values()) { platform ->
                    SocialChip(
                        platform = platform,
                        onClick = {
                            scope.launch {
                                sharingManager.sharePlaylist(
                                    playlist = playlist,
                                    videos = videos,
                                    shareOptions = PlaylistShareOptions(
                                        format = PlaylistFormat.SOCIAL,
                                        targetPlatform = platform
                                    )
                                )
                                onDismiss()
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun CollaborativeTab(
    playlist: com.astralplayer.nextplayer.feature.playlist.Playlist,
    videos: List<com.astralplayer.nextplayer.feature.playlist.PlaylistVideo>,
    sharingManager: SocialSharingManager,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var ownerName by remember { mutableStateOf("") }
    var allowAdd by remember { mutableStateOf(true) }
    var allowRemove by remember { mutableStateOf(false) }
    var allowReorder by remember { mutableStateOf(true) }
    var generateQR by remember { mutableStateOf(false) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Owner info
        OutlinedTextField(
            value = ownerName,
            onValueChange = { ownerName = it },
            label = { Text("Your name") },
            placeholder = { Text("Playlist owner name") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        
        // Permissions
        BubbleCard(
            elevation = 2,
            cornerRadius = 20
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Collaborator Permissions",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                SettingRow(
                    label = "Can add videos",
                    checked = allowAdd,
                    onCheckedChange = { allowAdd = it }
                )
                
                SettingRow(
                    label = "Can remove videos",
                    checked = allowRemove,
                    onCheckedChange = { allowRemove = it }
                )
                
                SettingRow(
                    label = "Can reorder videos",
                    checked = allowReorder,
                    onCheckedChange = { allowReorder = it }
                )
            }
        }
        
        // Options
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Generate QR code")
            Switch(
                checked = generateQR,
                onCheckedChange = { generateQR = it }
            )
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        // Create button
        BubbleButton(
            onClick = {
                if (ownerName.isNotEmpty()) {
                    scope.launch {
                        val collaborativePlaylist = sharingManager.createCollaborativePlaylist(
                            playlist = playlist,
                            videos = videos,
                            collaborativeOptions = CollaborativeOptions(
                                ownerName = ownerName,
                                permissions = CollaborativePermissions(
                                    allowAdd = allowAdd,
                                    allowRemove = allowRemove,
                                    allowReorder = allowReorder
                                ),
                                generateQR = generateQR
                            )
                        )
                        // Show share code dialog
                        onDismiss()
                    }
                }
            },
            text = "Create Collaborative Playlist",
            icon = Icons.Default.GroupAdd,
            modifier = Modifier.fillMaxWidth(),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            enabled = ownerName.isNotEmpty()
        )
    }
}

@Composable
private fun ExportTab(
    playlist: com.astralplayer.nextplayer.feature.playlist.Playlist,
    videos: List<com.astralplayer.nextplayer.feature.playlist.PlaylistVideo>,
    sharingManager: SocialSharingManager,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()
    
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        val formats = listOf(
            ExportFormat("M3U", "Standard playlist format", PlaylistFormat.M3U),
            ExportFormat("JSON", "Data interchange format", PlaylistFormat.JSON),
            ExportFormat("AstralStream", "Full metadata & thumbnails", PlaylistFormat.ASTRAL)
        )
        
        items(formats) { format ->
            ExportFormatCard(
                format = format,
                onClick = {
                    scope.launch {
                        sharingManager.sharePlaylist(
                            playlist = playlist,
                            videos = videos,
                            shareOptions = PlaylistShareOptions(format = format.playlistFormat)
                        )
                        onDismiss()
                    }
                }
            )
        }
    }
}

// Helper components

@Composable
private fun ShareOptionCard(
    icon: ImageVector,
    label: String,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    BubbleCard(
        onClick = onClick,
        elevation = 2,
        cornerRadius = 20,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun SocialPlatformCard(
    platform: SocialPlatformInfo,
    onClick: () -> Unit
) {
    BubbleCard(
        onClick = onClick,
        elevation = 2,
        cornerRadius = 20
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(platform.color),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = platform.icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            Text(
                text = "Share to ${platform.name}",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            
            Spacer(modifier = Modifier.weight(1f))
            
            Icon(
                Icons.Default.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SocialChip(
    platform: SocialPlatform,
    onClick: () -> Unit
) {
    val (icon, color) = when (platform) {
        SocialPlatform.TWITTER -> Icons.Default.Tag to Color(0xFF1DA1F2)
        SocialPlatform.FACEBOOK -> Icons.Default.ThumbUp to Color(0xFF1877F2)
        SocialPlatform.INSTAGRAM -> Icons.Default.PhotoCamera to Color(0xFFE4405F)
        SocialPlatform.WHATSAPP -> Icons.Default.Chat to Color(0xFF25D366)
        SocialPlatform.TELEGRAM -> Icons.Default.Send to Color(0xFF0088CC)
        SocialPlatform.OTHER -> Icons.Default.Share to MaterialTheme.colorScheme.primary
    }
    
    AssistChip(
        onClick = onClick,
        label = { Text(platform.name) },
        leadingIcon = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
        },
        colors = AssistChipDefaults.assistChipColors(
            containerColor = color.copy(alpha = 0.1f),
            labelColor = color,
            leadingIconContentColor = color
        )
    )
}

@Composable
private fun SettingRow(
    label: String,
    description: String? = null,
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
                text = label,
                style = MaterialTheme.typography.bodyLarge
            )
            if (description != null) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
private fun ExportFormatCard(
    format: ExportFormat,
    onClick: () -> Unit
) {
    BubbleCard(
        onClick = onClick,
        elevation = 2,
        cornerRadius = 20
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = format.name.take(3),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Export as ${format.name}",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = format.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Icon(
                Icons.Default.Download,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

// Data classes

private data class SocialPlatformInfo(
    val name: String,
    val icon: ImageVector,
    val color: Color
)

private data class ExportFormat(
    val name: String,
    val description: String,
    val playlistFormat: PlaylistFormat
)

// Helper functions

private fun formatTimestamp(millis: Long): String {
    val seconds = millis / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    
    return when {
        hours > 0 -> String.format("%02d:%02d:%02d", hours, minutes % 60, seconds % 60)
        else -> String.format("%02d:%02d", minutes, seconds % 60)
    }
}

private fun formatDuration(millis: Long): String {
    val seconds = millis / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    
    return when {
        hours > 0 -> "${hours}h ${minutes % 60}m"
        else -> "${minutes}m"
    }
}