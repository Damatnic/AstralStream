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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.astralplayer.nextplayer.data.database.AstralVuDatabase
import com.astralplayer.nextplayer.data.repository.PlaylistRepository
import com.astralplayer.nextplayer.data.repository.PlaylistRepositoryImpl
import com.astralplayer.nextplayer.ui.theme.AstralPlayerTheme
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class PlaylistActivity : ComponentActivity() {
    
    private lateinit var database: AstralVuDatabase
    private lateinit var playlistRepository: PlaylistRepository
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize database and repository
        database = (application as AstralVuApplication).database
        playlistRepository = PlaylistRepositoryImpl(database.playlistDao())
        
        setContent {
            AstralPlayerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    PlaylistScreen(
                        playlistRepository = playlistRepository,
                        onNavigateBack = { finish() },
                        onPlaylistSelected = { playlist ->
                            val intent = Intent(this, PlaylistDetailActivity::class.java).apply {
                                putExtra("playlist_id", playlist.id)
                                putExtra("playlist_name", playlist.name)
                            }
                            startActivity(intent)
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistScreen(
    playlistRepository: PlaylistRepository,
    onNavigateBack: () -> Unit,
    onPlaylistSelected: (com.astralplayer.nextplayer.data.repository.Playlist) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var playlists by remember { mutableStateOf(listOf<com.astralplayer.nextplayer.data.repository.Playlist>()) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }
    
    // Load playlists from database
    LaunchedEffect(Unit) {
        isLoading = true
        playlistRepository.getAllPlaylists().collectLatest { dbPlaylists ->
            playlists = dbPlaylists
            isLoading = false
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Playlists") },
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
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showCreateDialog = true },
                icon = {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.PlaylistAdd,
                        contentDescription = "Create playlist"
                    )
                },
                text = { Text("Create") }
            )
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
        } else if (playlists.isEmpty()) {
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
                        imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Text(
                        text = "No playlists yet",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Create a playlist to organize your videos",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Quick playlists
                item {
                    Text(
                        text = "Quick Playlists",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
                
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        QuickPlaylistCard(
                            icon = Icons.Default.Favorite,
                            title = "Favorites",
                            color = Color(0xFFE91E63),
                            modifier = Modifier.weight(1f),
                            onClick = { 
                                val intent = Intent(context, RecentFilesActivity::class.java).apply {
                                    putExtra("filter", "favorites")
                                }
                                context.startActivity(intent)
                            }
                        )
                        QuickPlaylistCard(
                            icon = Icons.Default.WatchLater,
                            title = "Watch Later",
                            color = Color(0xFF2196F3),
                            modifier = Modifier.weight(1f),
                            onClick = { 
                                coroutineScope.launch {
                                    // Create or get the Watch Later playlist
                                    val watchLaterPlaylist = playlists.find { it.name == "Watch Later" }
                                    if (watchLaterPlaylist != null) {
                                        val intent = Intent(context, PlaylistDetailActivity::class.java).apply {
                                            putExtra("playlist_id", watchLaterPlaylist.id)
                                            putExtra("playlist_name", watchLaterPlaylist.name)
                                        }
                                        context.startActivity(intent)
                                    } else {
                                        // Create Watch Later playlist
                                        val playlistId = playlistRepository.createPlaylist("Watch Later", "Videos to watch later")
                                        val intent = Intent(context, PlaylistDetailActivity::class.java).apply {
                                            putExtra("playlist_id", playlistId)
                                            putExtra("playlist_name", "Watch Later")
                                        }
                                        context.startActivity(intent)
                                    }
                                }
                            }
                        )
                    }
                }
                
                // User playlists
                item {
                    Text(
                        text = "Your Playlists",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                    )
                }
                
                items(playlists) { playlist ->
                    PlaylistCard(
                        playlist = playlist,
                        onClick = { onPlaylistSelected(playlist) }
                    )
                }
            }
        }
    }
    
    if (showCreateDialog) {
        CreatePlaylistDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { name, description ->
                coroutineScope.launch {
                    playlistRepository.createPlaylist(name, description)
                    showCreateDialog = false
                }
            }
        )
    }
}

@Composable
fun QuickPlaylistCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .height(100.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = color
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = color
            )
        }
    }
}

@Composable
fun PlaylistCard(
    playlist: com.astralplayer.nextplayer.data.repository.Playlist,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val database = (context.applicationContext as AstralVuApplication).database
    val playlistRepository = remember { PlaylistRepositoryImpl(database.playlistDao()) }
    var videoCount by remember { mutableStateOf(0) }
    var showMenu by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var newPlaylistName by remember { mutableStateOf(playlist.name) }
    
    LaunchedEffect(playlist.id) {
        videoCount = playlistRepository.getPlaylistVideoCount(playlist.id)
    }
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Thumbnail or icon
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.VideoLibrary,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = playlist.name,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                if (!playlist.description.isNullOrEmpty()) {
                    Text(
                        text = playlist.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                
                Text(
                    text = "$videoCount video${if (videoCount != 1) "s" else ""}",
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
                        text = { Text("Rename") },
                        onClick = { 
                            showMenu = false
                            showRenameDialog = true
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Edit, contentDescription = null)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete") },
                        onClick = { 
                            showMenu = false
                            showDeleteDialog = true
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Delete, contentDescription = null)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Share") },
                        onClick = { 
                            showMenu = false
                            val shareIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, "Check out my playlist: ${playlist.name}")
                                type = "text/plain"
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Share playlist"))
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Share, contentDescription = null)
                        }
                    )
                }
            }
        }
    }
    
    // Rename dialog
    if (showRenameDialog) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("Rename Playlist") },
            text = {
                TextField(
                    value = newPlaylistName,
                    onValueChange = { newPlaylistName = it },
                    placeholder = { Text("Enter new name") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        coroutineScope.launch {
                            val updatedPlaylist = playlist.copy(name = newPlaylistName)
                            playlistRepository.updatePlaylist(updatedPlaylist)
                            showRenameDialog = false
                        }
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showRenameDialog = false
                    newPlaylistName = playlist.name
                }) {
                    Text("Cancel")
                }
            }
        )
    }
    
    // Delete confirmation dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Playlist") },
            text = { Text("Are you sure you want to delete \"${playlist.name}\"? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        coroutineScope.launch {
                            playlistRepository.deletePlaylist(playlist.id)
                            showDeleteDialog = false
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
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun CreatePlaylistDialog(
    onDismiss: () -> Unit,
    onCreate: (name: String, description: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Playlist") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (optional)") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onCreate(name, description) },
                enabled = name.isNotBlank()
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}