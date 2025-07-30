package com.astralplayer.nextplayer.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.astralplayer.nextplayer.feature.playlist.PlaylistVideo
import com.astralplayer.nextplayer.feature.social.*
import com.astralplayer.nextplayer.ui.components.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * Collaborative Playlist Screen
 * Shows collaborative playlist details and allows real-time collaboration
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollaborativePlaylistScreen(
    navController: NavController,
    playlistId: String,
    sharingManager: SocialSharingManager
) {
    val scope = rememberCoroutineScope()
    val collaborativePlaylists by sharingManager.collaborativePlaylists.collectAsState()
    val playlist = collaborativePlaylists.find { it.id == playlistId }
    
    var showAddDialog by remember { mutableStateOf(false) }
    var showCollaboratorsDialog by remember { mutableStateOf(false) }
    var showShareDialog by remember { mutableStateOf(false) }
    
    if (playlist == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("Playlist not found")
        }
        return
    }
    
    Scaffold(
        topBar = {
            CollaborativePlaylistTopBar(
                playlist = playlist,
                onBack = { navController.navigateUp() },
                onShare = { showShareDialog = true },
                onCollaborators = { showCollaboratorsDialog = true }
            )
        },
        floatingActionButton = {
            if (playlist.permissions.allowAdd) {
                ExtendedFloatingActionButton(
                    onClick = { showAddDialog = true },
                    icon = { Icon(Icons.Default.Add, contentDescription = "Add video") },
                    text = { Text("Add Video") },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Playlist info card
            item {
                PlaylistInfoCard(playlist)
            }
            
            // Sync status
            if (playlist.syncEnabled) {
                item {
                    SyncStatusCard(
                        lastSynced = playlist.lastModified,
                        isOnline = true // In real app, check connection
                    )
                }
            }
            
            // Videos
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Videos (${playlist.videos.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    // Sort button
                    TextButton(onClick = { /* Show sort options */ }) {
                        Icon(Icons.Default.Sort, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Sort")
                    }
                }
            }
            
            items(playlist.videos) { video ->
                CollaborativeVideoCard(
                    video = video,
                    canRemove = playlist.permissions.allowRemove,
                    canReorder = playlist.permissions.allowReorder,
                    onPlay = {
                        // Navigate to player
                        navController.navigate("player/${video.id}")
                    },
                    onRemove = {
                        if (playlist.permissions.allowRemove) {
                            playlist.videos.remove(video)
                        }
                    },
                    onMoveUp = {
                        if (playlist.permissions.allowReorder) {
                            val index = playlist.videos.indexOf(video)
                            if (index > 0) {
                                playlist.videos.removeAt(index)
                                playlist.videos.add(index - 1, video)
                            }
                        }
                    },
                    onMoveDown = {
                        if (playlist.permissions.allowReorder) {
                            val index = playlist.videos.indexOf(video)
                            if (index < playlist.videos.size - 1) {
                                playlist.videos.removeAt(index)
                                playlist.videos.add(index + 1, video)
                            }
                        }
                    }
                )
            }
        }
    }
    
    // Dialogs
    if (showCollaboratorsDialog) {
        CollaboratorsDialog(
            playlist = playlist,
            onDismiss = { showCollaboratorsDialog = false }
        )
    }
    
    if (showShareDialog) {
        ShareCollaborativePlaylistDialog(
            playlist = playlist,
            onDismiss = { showShareDialog = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CollaborativePlaylistTopBar(
    playlist: CollaborativePlaylist,
    onBack: () -> Unit,
    onShare: () -> Unit,
    onCollaborators: () -> Unit
) {
    TopAppBar(
        title = {
            Column {
                Text(
                    text = playlist.originalPlaylist.name,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Collaborative • ${playlist.collaborators.size + 1} members",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
            }
        },
        actions = {
            IconButton(onClick = onCollaborators) {
                Icon(Icons.Default.Group, contentDescription = "Collaborators")
            }
            IconButton(onClick = onShare) {
                Icon(Icons.Default.Share, contentDescription = "Share")
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    )
}

@Composable
private fun PlaylistInfoCard(playlist: CollaborativePlaylist) {
    BubbleCard(
        elevation = 4,
        cornerRadius = 24,
        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Share Code",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = playlist.shareCode,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                
                // QR code placeholder
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                    MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.QrCode,
                        contentDescription = "QR Code",
                        modifier = Modifier.size(40.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            Divider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                PlaylistStat(
                    value = playlist.videos.size.toString(),
                    label = "Videos"
                )
                PlaylistStat(
                    value = formatDuration(playlist.videos.sumOf { it.duration }),
                    label = "Duration"
                )
                PlaylistStat(
                    value = playlist.collaborators.size.toString(),
                    label = "Collaborators"
                )
            }
            
            if (playlist.originalPlaylist.description.isNotEmpty()) {
                Text(
                    text = playlist.originalPlaylist.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun PlaylistStat(
    value: String,
    label: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SyncStatusCard(
    lastSynced: Long,
    isOnline: Boolean
) {
    BubbleCard(
        elevation = 1,
        cornerRadius = 16,
        containerColor = if (isOnline) {
            Color(0xFF4CAF50).copy(alpha = 0.1f)
        } else {
            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f)
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                if (isOnline) Icons.Default.CloudDone else Icons.Default.CloudOff,
                contentDescription = null,
                tint = if (isOnline) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error,
                modifier = Modifier.size(20.dp)
            )
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isOnline) "Synced" else "Offline",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Last updated ${formatRelativeTime(lastSynced)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            if (isOnline) {
                // Sync animation
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CollaborativeVideoCard(
    video: PlaylistVideo,
    canRemove: Boolean,
    canReorder: Boolean,
    onPlay: () -> Unit,
    onRemove: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    
    BubbleCard(
        onClick = onPlay,
        elevation = 2,
        cornerRadius = 20
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Thumbnail
            VideoThumbnail(
                thumbnailPath = null, // Use default
                modifier = Modifier.size(width = 100.dp, height = 60.dp),
                cornerRadius = 12.dp
            )
            
            // Video info
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = video.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = formatDuration(video.duration),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Text(
                        text = "•",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Text(
                        text = "Added ${formatRelativeTime(video.addedDate)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            // Actions
            if (canRemove || canReorder) {
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(
                            Icons.Default.MoreVert,
                            contentDescription = "More options",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        if (canReorder) {
                            DropdownMenuItem(
                                text = { Text("Move up") },
                                onClick = {
                                    onMoveUp()
                                    showMenu = false
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.ArrowUpward, contentDescription = null)
                                }
                            )
                            
                            DropdownMenuItem(
                                text = { Text("Move down") },
                                onClick = {
                                    onMoveDown()
                                    showMenu = false
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.ArrowDownward, contentDescription = null)
                                }
                            )
                        }
                        
                        if (canRemove) {
                            if (canReorder) {
                                Divider()
                            }
                            
                            DropdownMenuItem(
                                text = { Text("Remove") },
                                onClick = {
                                    onRemove()
                                    showMenu = false
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                },
                                colors = MenuDefaults.itemColors(
                                    textColor = MaterialTheme.colorScheme.error
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CollaboratorsDialog(
    playlist: CollaborativePlaylist,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Collaborators")
        },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Owner
                item {
                    CollaboratorItem(
                        name = playlist.owner,
                        role = "Owner",
                        joinedAt = playlist.createdAt,
                        isOwner = true
                    )
                }
                
                // Collaborators
                items(playlist.collaborators) { collaborator ->
                    CollaboratorItem(
                        name = collaborator.name,
                        role = getPermissionString(collaborator.permissions),
                        joinedAt = collaborator.joinedAt,
                        isOwner = false
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
private fun CollaboratorItem(
    name: String,
    role: String,
    joinedAt: Long,
    isOwner: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(
                    if (isOwner) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.secondaryContainer
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = name.first().uppercase(),
                style = MaterialTheme.typography.titleMedium,
                color = if (isOwner) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onSecondaryContainer
                },
                fontWeight = FontWeight.Bold
            )
        }
        
        Column(modifier = Modifier.weight(1f)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                
                if (isOwner) {
                    Icon(
                        Icons.Default.Star,
                        contentDescription = "Owner",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            Text(
                text = "$role • Joined ${formatRelativeTime(joinedAt)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ShareCollaborativePlaylistDialog(
    playlist: CollaborativePlaylist,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Share Playlist")
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Share code
                BubbleCard(
                    elevation = 2,
                    cornerRadius = 16,
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Share Code",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = playlist.shareCode,
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                
                Text(
                    text = "Share this code with others to let them join the playlist",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                // QR code placeholder
                Box(
                    modifier = Modifier
                        .size(200.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                    MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.QrCode,
                        contentDescription = "QR Code",
                        modifier = Modifier.size(80.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
        confirmButton = {
            BubbleButton(
                onClick = { /* Copy to clipboard */ },
                text = "Copy Code",
                icon = Icons.Default.ContentCopy
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

// Helper functions

private fun formatDuration(millis: Long): String {
    val seconds = millis / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    
    return when {
        hours > 0 -> "${hours}h ${minutes % 60}m"
        else -> "${minutes}m"
    }
}

private fun formatRelativeTime(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    val seconds = diff / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    val days = hours / 24
    
    return when {
        days > 0 -> "$days days ago"
        hours > 0 -> "$hours hours ago"
        minutes > 0 -> "$minutes minutes ago"
        else -> "just now"
    }
}

private fun getPermissionString(permissions: Set<Permission>): String {
    return when {
        permissions.contains(Permission.ADMIN) -> "Admin"
        permissions.contains(Permission.REMOVE) -> "Editor"
        permissions.contains(Permission.ADD) -> "Contributor"
        else -> "Viewer"
    }
}