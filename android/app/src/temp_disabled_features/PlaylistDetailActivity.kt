package com.astralplayer.nextplayer

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.astralplayer.nextplayer.ui.theme.AstralTheme
import com.astralplayer.nextplayer.ui.VideoSelectionDialog
import kotlinx.coroutines.launch
import org.burnoutcrew.reorderable.*
import java.io.File

class PlaylistDetailActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val playlistId = intent.getLongExtra("playlist_id", -1L)
        if (playlistId == -1L) {
            finish()
            return
        }
        
        setContent {
            AstralTheme {
                val viewModel: PlaylistDetailViewModel = viewModel(
                    factory = PlaylistDetailViewModelFactory(application, playlistId)
                )
                
                PlaylistDetailScreen(
                    viewModel = viewModel,
                    onBackClick = { finish() },
                    onVideoClick = { video ->
                        val intent = Intent(this, VideoPlayerActivity::class.java).apply {
                            data = Uri.parse(video.path)
                            putExtra("video_title", video.title)
                            putExtra("playlist_id", playlistId)
                            putExtra("video_id", video.id)
                        }
                        startActivity(intent)
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun PlaylistDetailScreen(
    viewModel: PlaylistDetailViewModel,
    onBackClick: () -> Unit,
    onVideoClick: (VideoMetadata) -> Unit
) {
    val playlist by viewModel.playlist.collectAsState()
    val videos by viewModel.videos.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    var showOptionsMenu by remember { mutableStateOf(false) }
    var showAddVideosDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var isReorderMode by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    
    val reorderableState = rememberReorderableLazyListState(
        onMove = { from, to ->
            viewModel.reorderVideos(from.index, to.index)
        },
        onDragEnd = { _, _ ->
            viewModel.saveReorderedVideos()
        }
    )
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = playlist?.name ?: "Loading...",
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        playlist?.let {
                            Text(
                                text = "${it.videoIds.size} videos",
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    if (isReorderMode) {
                        TextButton(
                            onClick = {
                                isReorderMode = false
                                viewModel.saveReorderedVideos()
                            }
                        ) {
                            Text("Done", color = Color(0xFF00D4FF))
                        }
                    } else {
                        IconButton(onClick = { showAddVideosDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add Videos"
                            )
                        }
                        
                        IconButton(onClick = { showOptionsMenu = true }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "More Options"
                            )
                        }
                        
                        DropdownMenu(
                            expanded = showOptionsMenu,
                            onDismissRequest = { showOptionsMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Edit Playlist") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = null
                                    )
                                },
                                onClick = {
                                    showOptionsMenu = false
                                    showEditDialog = true
                                }
                            )
                            
                            DropdownMenuItem(
                                text = { Text("Reorder Videos") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Reorder,
                                        contentDescription = null
                                    )
                                },
                                onClick = {
                                    showOptionsMenu = false
                                    isReorderMode = true
                                }
                            )
                            
                            DropdownMenuItem(
                                text = { Text("Play All") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = null
                                    )
                                },
                                onClick = {
                                    showOptionsMenu = false
                                    videos.firstOrNull()?.let { onVideoClick(it) }
                                }
                            )
                            
                            DropdownMenuItem(
                                text = { Text("Shuffle Play") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Shuffle,
                                        contentDescription = null
                                    )
                                },
                                onClick = {
                                    showOptionsMenu = false
                                    videos.shuffled().firstOrNull()?.let { onVideoClick(it) }
                                }
                            )
                            
                            HorizontalDivider()
                            
                            DropdownMenuItem(
                                text = { Text("Clear Playlist", color = Color.Red) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = null,
                                        tint = Color.Red
                                    )
                                },
                                onClick = {
                                    showOptionsMenu = false
                                    scope.launch {
                                        viewModel.clearPlaylist()
                                        snackbarHostState.showSnackbar("Playlist cleared")
                                    }
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF0A0E27),
                    titleContentColor = Color.White
                )
            )
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        },
        containerColor = Color(0xFF0A0E27)
    ) { paddingValues ->
        when {
            isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color(0xFF00D4FF))
                }
            }
            videos.isEmpty() -> {
                EmptyPlaylistDetailView(
                    onAddVideos = { showAddVideosDialog = true },
                    modifier = Modifier.padding(paddingValues)
                )
            }
            else -> {
                LazyColumn(
                    state = reorderableState.listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .reorderable(reorderableState),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(videos, key = { _, video -> video.id }) { index, video ->
                        ReorderableItem(reorderableState, key = video.id) { isDragging ->
                            VideoItemCard(
                                video = video,
                                onClick = { if (!isReorderMode) onVideoClick(video) },
                                onRemove = {
                                    scope.launch {
                                        viewModel.removeVideo(video)
                                        snackbarHostState.showSnackbar(
                                            message = "${video.title} removed",
                                            actionLabel = "Undo"
                                        ).let { result ->
                                            if (result == SnackbarResult.ActionPerformed) {
                                                viewModel.undoRemove()
                                            }
                                        }
                                    }
                                },
                                isReorderMode = isReorderMode,
                                isDragging = isDragging,
                                modifier = if (isReorderMode) {
                                    Modifier.detectReorderAfterLongPress(reorderableState)
                                } else {
                                    Modifier
                                }
                            )
                        }
                    }
                }
            }
        }
    }
    
    if (showEditDialog) {
        playlist?.let { currentPlaylist ->
            EditPlaylistDialog(
                playlist = currentPlaylist,
                onDismiss = { showEditDialog = false },
                onSave = { name, description ->
                    viewModel.updatePlaylist(name, description)
                    showEditDialog = false
                }
            )
        }
    }
    
    if (showAddVideosDialog) {
        VideoSelectionDialog(
            onDismiss = { showAddVideosDialog = false },
            onVideosSelected = { selectedVideoIds ->
                viewModel.addVideosToPlaylist(selectedVideoIds)
                scope.launch {
                    snackbarHostState.showSnackbar(
                        "${selectedVideoIds.size} video${if (selectedVideoIds.size != 1) "s" else ""} added to playlist"
                    )
                }
            },
            existingVideoIds = videos.map { it.id }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoItemCard(
    video: VideoMetadata,
    onClick: () -> Unit,
    onRemove: () -> Unit,
    isReorderMode: Boolean,
    isDragging: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDragging) {
                Color(0xFF00D4FF).copy(alpha = 0.2f)
            } else {
                Color.White.copy(alpha = 0.05f)
            }
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isDragging) 8.dp else 0.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isReorderMode) {
                Icon(
                    imageVector = Icons.Default.DragHandle,
                    contentDescription = "Drag to reorder",
                    tint = Color.White.copy(alpha = 0.5f),
                    modifier = Modifier.padding(end = 8.dp)
                )
            }
            
            // Video thumbnail
            AsyncImage(
                model = video.thumbnailPath ?: File(video.path),
                contentDescription = "Video thumbnail",
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black),
                contentScale = ContentScale.Crop
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = video.title,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Text(
                    text = formatDuration(video.duration),
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 14.sp
                )
                
                Text(
                    text = formatFileSize(video.size),
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 12.sp
                )
            }
            
            if (!isReorderMode) {
                IconButton(onClick = onRemove) {
                    Icon(
                        imageVector = Icons.Default.RemoveCircleOutline,
                        contentDescription = "Remove from playlist",
                        tint = Color.Red.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyPlaylistDetailView(
    onAddVideos: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.VideoLibrary,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.3f),
                modifier = Modifier.size(72.dp)
            )
            
            Text(
                text = "Empty Playlist",
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium
            )
            
            Text(
                text = "Add videos to start watching",
                color = Color.White.copy(alpha = 0.3f),
                fontSize = 14.sp
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = onAddVideos,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF00D4FF)
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add Videos", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditPlaylistDialog(
    playlist: VideoPlaylist,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var name by remember { mutableStateOf(playlist.name) }
    var description by remember { mutableStateOf(playlist.description) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Playlist") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Playlist Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    minLines = 2,
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(name, description) },
                enabled = name.isNotBlank()
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
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
        hours > 0 -> String.format("%d:%02d:%02d", hours, minutes % 60, seconds % 60)
        else -> String.format("%d:%02d", minutes, seconds % 60)
    }
}

private fun formatFileSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
        else -> "%.2f GB".format(bytes / (1024.0 * 1024.0 * 1024.0))
    }
}