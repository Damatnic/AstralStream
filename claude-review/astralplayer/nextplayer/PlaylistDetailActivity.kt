package com.astralplayer.nextplayer

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.astralplayer.nextplayer.data.database.AstralVuDatabase
import com.astralplayer.nextplayer.data.repository.PlaylistRepository
import com.astralplayer.nextplayer.data.repository.PlaylistRepositoryImpl
import com.astralplayer.nextplayer.ui.theme.AstralPlayerTheme
import com.astralplayer.nextplayer.ui.components.VideoThumbnail
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class PlaylistDetailActivity : ComponentActivity() {
    
    private lateinit var database: AstralVuDatabase
    private lateinit var playlistRepository: PlaylistRepository
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val playlistId = intent.getStringExtra("playlist_id") ?: ""
        val playlistName = intent.getStringExtra("playlist_name") ?: "Playlist"
        
        // Initialize database and repository
        database = (application as AstralVuApplication).database
        playlistRepository = PlaylistRepositoryImpl(database.playlistDao())
        
        setContent {
            AstralPlayerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    PlaylistDetailScreen(
                        playlistId = playlistId,
                        playlistName = playlistName,
                        playlistRepository = playlistRepository,
                        onNavigateBack = { finish() }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistDetailScreen(
    playlistId: String,
    playlistName: String,
    playlistRepository: PlaylistRepository,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var videos by remember { mutableStateOf(listOf<com.astralplayer.nextplayer.data.repository.PlaylistVideo>()) }
    var isPlaying by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }
    var showAddVideoDialog by remember { mutableStateOf(false) }
    var showOptionsMenu by remember { mutableStateOf(false) }
    var selectedVideo by remember { mutableStateOf<com.astralplayer.nextplayer.data.repository.PlaylistVideo?>(null) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showSortDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var editedPlaylistName by remember { mutableStateOf(playlistName) }
    
    // Load playlist videos from database
    LaunchedEffect(playlistId) {
        isLoading = true
        playlistRepository.getPlaylistVideos(playlistId).collectLatest { dbVideos ->
            videos = dbVideos
            isLoading = false
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(playlistName) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    if (!isLoading && videos.isNotEmpty()) {
                        IconButton(onClick = { 
                            if (videos.isNotEmpty()) {
                                coroutineScope.launch {
                                    playAllVideos(context, videos, shuffle = false)
                                }
                            }
                        }) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Play all"
                            )
                        }
                        IconButton(onClick = { 
                            if (videos.isNotEmpty()) {
                                coroutineScope.launch {
                                    playAllVideos(context, videos, shuffle = true)
                                }
                            }
                        }) {
                            Icon(
                                imageVector = Icons.Default.Shuffle,
                                contentDescription = "Shuffle"
                            )
                        }
                    }
                    IconButton(onClick = { showOptionsMenu = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "More options"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        floatingActionButton = {
            if (!isLoading) {
                FloatingActionButton(
                    onClick = { showAddVideoDialog = true }
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add video"
                    )
                }
            }
        },
        modifier = modifier
    ) { paddingValues ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (videos.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.VideoLibrary,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Text(
                        text = "No videos in this playlist",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Add videos to get started",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                    Button(
                        onClick = { showAddVideoDialog = true }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Add Videos")
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Playlist info and controls
                item {
                    PlaylistHeader(
                        videoCount = videos.size,
                        totalDuration = videos.sumOf { it.duration },
                        isPlaying = isPlaying,
                        onPlayAll = {
                            if (videos.isNotEmpty()) {
                                coroutineScope.launch {
                                    playAllVideos(context, videos, shuffle = false)
                                    isPlaying = true
                                }
                            }
                        },
                        onShuffle = {
                            if (videos.isNotEmpty()) {
                                coroutineScope.launch {
                                    playAllVideos(context, videos, shuffle = true)
                                    isPlaying = true
                                }
                            }
                        }
                    )
                }
                
                // Video list
                items(
                    items = videos,
                    key = { it.id }
                ) { video ->
                    PlaylistVideoItem(
                        video = video,
                        position = videos.indexOf(video) + 1,
                        onClick = {
                            val intent = Intent(context, VideoPlayerActivity::class.java).apply {
                                data = android.net.Uri.parse(video.uri)
                                putExtra("video_title", video.title)
                                putExtra("playlist_id", playlistId)
                                putExtra("playlist_position", videos.indexOf(video))
                            }
                            context.startActivity(intent)
                        },
                        onRemove = {
                            coroutineScope.launch {
                                playlistRepository.removeVideoFromPlaylist(playlistId, video.id)
                            }
                        },
                        onMoveUp = {
                            coroutineScope.launch {
                                playlistRepository.moveVideoUp(playlistId, video.id)
                            }
                        },
                        onMoveDown = {
                            coroutineScope.launch {
                                playlistRepository.moveVideoDown(playlistId, video.id)
                            }
                        }
                    )
                }
                
                item {
                    Spacer(modifier = Modifier.height(80.dp)) // Space for FAB
                }
            }
        }
    }
    
    // Playlist options dropdown
    DropdownMenu(
        expanded = showOptionsMenu,
        onDismissRequest = { showOptionsMenu = false }
    ) {
        DropdownMenuItem(
            text = { Text("Edit playlist") },
            onClick = {
                showOptionsMenu = false
                showEditDialog = true
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = null
                )
            }
        )
        DropdownMenuItem(
            text = { Text("Sort by") },
            onClick = {
                showOptionsMenu = false
                showSortDialog = true
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Sort,
                    contentDescription = null
                )
            }
        )
        DropdownMenuItem(
            text = { Text("Delete playlist") },
            onClick = {
                showOptionsMenu = false
                showDeleteConfirmation = true
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            }
        )
    }
    
    if (showAddVideoDialog) {
        AlertDialog(
            onDismissRequest = { showAddVideoDialog = false },
            title = { Text("Add Videos") },
            text = { Text("Browse for videos to add to this playlist") },
            confirmButton = {
                TextButton(onClick = { 
                    showAddVideoDialog = false
                    // Navigate to file browser
                    val intent = Intent(context, FolderBrowserActivity::class.java)
                    intent.putExtra("select_mode", true)
                    intent.putExtra("playlist_id", playlistId)
                    context.startActivity(intent)
                }) {
                    Text("Browse")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddVideoDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
    
    if (showEditDialog) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("Edit Playlist") },
            text = {
                OutlinedTextField(
                    value = editedPlaylistName,
                    onValueChange = { editedPlaylistName = it },
                    label = { Text("Playlist name") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        coroutineScope.launch {
                            // Get the current playlist and update its name
                            playlistRepository.getPlaylist(playlistId)?.let { playlist ->
                                val updatedPlaylist = playlist.copy(name = editedPlaylistName)
                                playlistRepository.updatePlaylist(updatedPlaylist)
                                showEditDialog = false
                            }
                        }
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showEditDialog = false
                    editedPlaylistName = playlistName
                }) {
                    Text("Cancel")
                }
            }
        )
    }
    
    if (showSortDialog) {
        var sortOption by remember { mutableStateOf("date_added") }
        AlertDialog(
            onDismissRequest = { showSortDialog = false },
            title = { Text("Sort by") },
            text = {
                Column {
                    RadioButtonOption("Date added", sortOption == "date_added") { sortOption = "date_added" }
                    RadioButtonOption("Title", sortOption == "title") { sortOption = "title" }
                    RadioButtonOption("Duration", sortOption == "duration") { sortOption = "duration" }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        // Sort videos based on selection
                        videos = when (sortOption) {
                            "title" -> videos.sortedBy { it.title }
                            "duration" -> videos.sortedByDescending { it.duration }
                            else -> videos // Already sorted by date
                        }
                        showSortDialog = false
                    }
                ) {
                    Text("Apply")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSortDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
    
    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("Delete Playlist") },
            text = { Text("Are you sure you want to delete \"$playlistName\"? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        coroutineScope.launch {
                            playlistRepository.deletePlaylist(playlistId)
                            onNavigateBack()
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun RadioButtonOption(text: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(text)
    }
}

@Composable
fun PlaylistHeader(
    videoCount: Int,
    totalDuration: Long,
    isPlaying: Boolean,
    onPlayAll: () -> Unit,
    onShuffle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "$videoCount videos",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Total duration: ${formatDuration(totalDuration)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                }
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilledTonalButton(
                        onClick = onShuffle
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shuffle,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Shuffle")
                    }
                    
                    Button(
                        onClick = onPlayAll
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (isPlaying) "Pause" else "Play All")
                    }
                }
            }
        }
    }
}

@Composable
fun PlaylistVideoItem(
    video: com.astralplayer.nextplayer.data.repository.PlaylistVideo,
    position: Int,
    onClick: () -> Unit,
    onRemove: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }
    
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
            // Position number
            Text(
                text = position.toString(),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(32.dp)
            )
            
            // Video thumbnail
            VideoThumbnail(
                videoUri = android.net.Uri.parse(video.uri),
                duration = video.duration,
                modifier = Modifier
                    .size(width = 80.dp, height = 45.dp)
                    .clip(MaterialTheme.shapes.small),
                showDuration = true
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = video.title,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = formatDuration(video.duration),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "More options"
                    )
                }
                
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Remove from playlist") },
                        onClick = {
                            onRemove()
                            showMenu = false
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Remove,
                                contentDescription = null
                            )
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Move up") },
                        onClick = {
                            onMoveUp()
                            showMenu = false
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.ArrowUpward,
                                contentDescription = null
                            )
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Move down") },
                        onClick = {
                            onMoveDown()
                            showMenu = false
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.ArrowDownward,
                                contentDescription = null
                            )
                        }
                    )
                }
            }
        }
    }
}

private fun formatDuration(totalMillis: Long): String {
    val totalSeconds = totalMillis / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    
    return when {
        hours > 0 -> String.format("%d:%02d:%02d", hours, minutes, seconds)
        else -> String.format("%d:%02d", minutes, seconds)
    }
}

private suspend fun playAllVideos(
    context: android.content.Context,
    videos: List<com.astralplayer.nextplayer.data.repository.PlaylistVideo>,
    shuffle: Boolean
) {
    if (videos.isEmpty()) return
    
    // Convert repository PlaylistVideo to PlayerRepository PlaylistVideo
    val playlistVideos = videos.map { video ->
        com.astralplayer.nextplayer.data.PlaylistVideo(
            playlistId = 0L,
            uri = video.uri,
            title = video.title,
            duration = video.duration
        )
    }
    
    // Start the VideoPlayerActivity with the first video and queue the rest
    val firstVideo = if (shuffle) playlistVideos.shuffled().first() else playlistVideos.first()
    val intent = Intent(context, VideoPlayerActivity::class.java).apply {
        data = android.net.Uri.parse(firstVideo.uri)
        putExtra("video_title", firstVideo.title)
        putExtra("playlist_mode", true)
        putExtra("playlist_shuffle", shuffle)
        putParcelableArrayListExtra("playlist_videos", ArrayList(playlistVideos.map { video ->
            android.os.Bundle().apply {
                putString("uri", video.uri.toString())
                putString("title", video.title)
                putLong("duration", video.duration)
            }
        }))
    }
    context.startActivity(intent)
}