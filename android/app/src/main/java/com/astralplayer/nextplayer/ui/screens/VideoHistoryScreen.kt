package com.astralplayer.nextplayer.ui.screens

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.astralplayer.nextplayer.data.RecentFile
import com.astralplayer.nextplayer.data.database.PlaybackHistoryEntity
import com.astralplayer.nextplayer.ui.components.VideoThumbnail
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoHistoryScreen(
    recentFilesFlow: Flow<List<RecentFile>>,
    playbackHistoryFlow: Flow<List<PlaybackHistoryEntity>>,
    onVideoClick: (RecentFile) -> Unit,
    onVideoLongClick: (RecentFile) -> Unit = {},
    onBack: () -> Unit,
    onClearHistory: () -> Unit = {},
    onToggleFavorite: (RecentFile) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val recentFiles by recentFilesFlow.collectAsState(initial = emptyList())
    val playbackHistory by playbackHistoryFlow.collectAsState(initial = emptyList())
    
    var selectedFilter by remember { mutableStateOf("all") }
    var showClearDialog by remember { mutableStateOf(false) }
    
    val filteredFiles = remember(recentFiles, selectedFilter) {
        when (selectedFilter) {
            "favorites" -> recentFiles.filter { it.isFavorite }
            "recent" -> recentFiles.take(20)
            "incomplete" -> recentFiles.filter { file ->
                val completionPercentage = file.lastPosition.toFloat() / file.duration.toFloat()
                completionPercentage < 0.9f && completionPercentage > 0.1f
            }
            else -> recentFiles
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Video History") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    val context = LocalContext.current
                    IconButton(onClick = { showClearDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Clear history"
                        )
                    }
                    IconButton(onClick = { 
                        val intent = android.content.Intent(context, com.astralplayer.nextplayer.SearchActivity::class.java)
                        context.startActivity(intent)
                    }) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search history"
                        )
                    }
                }
            )
        },
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Filter chips
            LazyRow(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(
                    listOf(
                        "all" to "All Videos",
                        "recent" to "Recent",
                        "favorites" to "Favorites",
                        "incomplete" to "Continue Watching"
                    )
                ) { (key, label) ->
                    FilterChip(
                        selected = selectedFilter == key,
                        onClick = { selectedFilter = key },
                        label = { Text(label) },
                        leadingIcon = when (key) {
                            "favorites" -> ({ Icon(Icons.Default.Favorite, null, modifier = Modifier.size(16.dp)) })
                            "incomplete" -> ({ Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(16.dp)) })
                            "recent" -> ({ Icon(Icons.Default.Schedule, null, modifier = Modifier.size(16.dp)) })
                            else -> null
                        }
                    )
                }
            }
            
            // Statistics row
            HistoryStatsRow(
                totalVideos = recentFiles.size,
                favoriteCount = recentFiles.count { it.isFavorite },
                totalWatchTime = playbackHistory.sumOf { it.duration },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            
            // Video list
            if (filteredFiles.isEmpty()) {
                EmptyHistoryMessage(
                    filter = selectedFilter,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredFiles) { file ->
                        VideoHistoryItem(
                            recentFile = file,
                            playbackHistory = playbackHistory.filter { it.fileId == file.id },
                            onClick = { onVideoClick(file) },
                            onLongClick = { onVideoLongClick(file) },
                            onToggleFavorite = { onToggleFavorite(file) }
                        )
                    }
                }
            }
        }
    }
    
    // Clear history confirmation dialog
    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Clear History") },
            text = { Text("Are you sure you want to clear all video history? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onClearHistory()
                        showClearDialog = false
                    }
                ) {
                    Text("Clear", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun HistoryStatsRow(
    totalVideos: Int,
    favoriteCount: Int,
    totalWatchTime: Long,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        StatCard(
            title = "Total Videos",
            value = totalVideos.toString(),
            icon = Icons.Default.VideoLibrary
        )
        StatCard(
            title = "Favorites",
            value = favoriteCount.toString(),
            icon = Icons.Default.Favorite
        )
        StatCard(
            title = "Watch Time",
            value = formatWatchTime(totalWatchTime),
            icon = Icons.Default.Schedule
        )
    }
}

@Composable
private fun StatCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun VideoHistoryItem(
    recentFile: RecentFile,
    playbackHistory: List<PlaybackHistoryEntity>,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier
) {
    val completionPercentage = if (recentFile.duration > 0) {
        recentFile.lastPosition.toFloat() / recentFile.duration.toFloat()
    } else 0f
    
    val lastPlayedDate = remember(recentFile.lastPlayed) {
        SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(recentFile.lastPlayed))
    }
    
    val playCount = playbackHistory.size
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .height(120.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            // Thumbnail
            Box(modifier = Modifier.width(160.dp)) {
                VideoThumbnail(
                    videoUri = Uri.parse(recentFile.uri),
                    duration = recentFile.duration,
                    modifier = Modifier.fillMaxSize(),
                    showDuration = false,
                    contentScale = ContentScale.Crop
                )
                
                // Progress bar
                if (completionPercentage > 0) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .height(4.dp)
                            .background(Color.Black.copy(alpha = 0.3f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(completionPercentage.coerceIn(0f, 1f))
                                .fillMaxHeight()
                                .background(MaterialTheme.colorScheme.primary)
                        )
                    }
                }
                
                // Play count badge
                if (playCount > 1) {
                    Card(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(4.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.Black.copy(alpha = 0.7f)
                        )
                    ) {
                        Text(
                            text = "${playCount}x",
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
            
            // Content
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(12.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Title and info
                Column {
                    Text(
                        text = recentFile.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Last played: $lastPlayedDate",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        if (recentFile.isCloudFile) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.Default.Cloud,
                                contentDescription = "Cloud file",
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    
                    if (completionPercentage > 0) {
                        Text(
                            text = "${(completionPercentage * 100).toInt()}% watched",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                
                // Action buttons
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = onToggleFavorite,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = if (recentFile.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = if (recentFile.isFavorite) "Remove from favorites" else "Add to favorites",
                            tint = if (recentFile.isFavorite) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    
                    IconButton(
                        onClick = onLongClick,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "More options",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyHistoryMessage(
    filter: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        val (icon, title, message) = when (filter) {
            "favorites" -> Triple(
                Icons.Default.FavoriteBorder,
                "No Favorites",
                "Videos you mark as favorites will appear here"
            )
            "incomplete" -> Triple(
                Icons.Default.PlayArrow,
                "Nothing to Continue",
                "Videos you've partially watched will appear here"
            )
            "recent" -> Triple(
                Icons.Default.Schedule,
                "No Recent Videos",
                "Videos you've recently played will appear here"
            )
            else -> Triple(
                Icons.Default.VideoLibrary,
                "No Video History",
                "Videos you play will appear here"
            )
        }
        
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
    }
}

private fun formatWatchTime(totalMillis: Long): String {
    val totalMinutes = totalMillis / (1000 * 60)
    return when {
        totalMinutes < 60 -> "${totalMinutes}m"
        totalMinutes < 1440 -> "${totalMinutes / 60}h ${totalMinutes % 60}m"
        else -> "${totalMinutes / 1440}d ${(totalMinutes % 1440) / 60}h"
    }
}

