package com.astralplayer.nextplayer.feature.browser

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Build
import android.os.Environment
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.astralplayer.nextplayer.ui.theme.NextPlayerTheme
import com.astralplayer.nextplayer.PlaylistRepository
import com.astralplayer.nextplayer.VideoPlaylist
import com.astralplayer.nextplayer.CreatePlaylistDialog
import com.astralplayer.nextplayer.feature.playlist.PlaylistSelectionDialog
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class FolderBrowserActivity : ComponentActivity() {
    
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // Permission granted, refresh the file list
        } else {
            // Permission denied, show explanation
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            NextPlayerTheme {
                FolderBrowserScreen(
                    onVideoSelected = { videoUri ->
                        // Return the selected video to the calling activity
                        val resultIntent = Intent().apply {
                            data = videoUri
                        }
                        setResult(RESULT_OK, resultIntent)
                        finish()
                    },
                    onBack = { finish() },
                    onRequestPermission = { permission ->
                        requestPermissionLauncher.launch(permission)
                    }
                )
            }
        }
    }
    
    fun shareVideoFile(file: FileItem) {
        val videoFile = File(file.path)
        if (!videoFile.exists()) {
            Toast.makeText(this, "File not found", Toast.LENGTH_SHORT).show()
            return
        }
        
        val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            androidx.core.content.FileProvider.getUriForFile(
                this,
                "${packageName}.fileprovider",
                videoFile
            )
        } else {
            Uri.fromFile(videoFile)
        }
        
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "video/*"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        
        startActivity(Intent.createChooser(shareIntent, "Share video"))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderBrowserScreen(
    onVideoSelected: (Uri) -> Unit,
    onBack: () -> Unit,
    onRequestPermission: (String) -> Unit,
    viewModel: FolderBrowserViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val playlistRepository = remember { PlaylistRepository(context) }
    var showPlaylistDialog by remember { mutableStateOf(false) }
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }
    var selectedFileForPlaylist by remember { mutableStateOf<FileItem?>(null) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    
    // Check permissions on first load
    LaunchedEffect(Unit) {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
        
        if (!hasPermission) {
            onRequestPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
        } else {
            viewModel.loadInitialDirectory()
        }
    }
    
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
    Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
        // Top App Bar
        TopAppBar(
            title = { 
                Text(
                    text = uiState.currentPath.substringAfterLast('/').ifEmpty { "Storage" },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                }
            },
            actions = {
                // View mode toggle
                IconButton(
                    onClick = { viewModel.toggleViewMode() }
                ) {
                    Icon(
                        imageVector = if (uiState.isGridView) Icons.Default.List else Icons.Default.GridView,
                        contentDescription = "Toggle view mode"
                    )
                }
                
                // Sort options
                IconButton(
                    onClick = { viewModel.toggleSortOrder() }
                ) {
                    Icon(
                        imageVector = when (uiState.sortOrder) {
                            SortOrder.NAME_ASC -> Icons.Default.SortByAlpha
                            SortOrder.NAME_DESC -> Icons.Default.SortByAlpha
                            SortOrder.DATE_ASC -> Icons.Default.DateRange
                            SortOrder.DATE_DESC -> Icons.Default.DateRange
                            SortOrder.SIZE_ASC -> Icons.Default.Storage
                            SortOrder.SIZE_DESC -> Icons.Default.Storage
                        },
                        contentDescription = "Sort order"
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color(0xFF1A1A2E)
            )
        )
        
        // Breadcrumb navigation
        if (uiState.currentPath.isNotEmpty()) {
            BreadcrumbNavigation(
                path = uiState.currentPath,
                onPathClick = { path -> viewModel.navigateToPath(path) }
            )
        }
        
        // Content
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color(0xFF00BCD4))
                }
            }
            
            uiState.error != null -> {
                val error = uiState.error
                ErrorMessage(
                    error = error!!,
                    onRetry = { viewModel.refreshCurrentDirectory() }
                )
            }
            
            uiState.files.isEmpty() -> {
                EmptyFolderMessage()
            }
            
            else -> {
                FileList(
                    files = uiState.files,
                    isGridView = uiState.isGridView,
                    onFileClick = { file ->
                        if (file.isDirectory) {
                            viewModel.navigateToDirectory(file.path)
                        } else if (file.isVideoFile) {
                            onVideoSelected(Uri.fromFile(File(file.path)))
                        }
                    },
                    onFileLongClick = { file ->
                        viewModel.showFileOptions(file)
                    }
                )
            }
        }
    }
    
    // File options dialog
    if (uiState.selectedFile != null) {
        val selectedFile = uiState.selectedFile
        FileOptionsDialog(
            file = selectedFile!!,
            onDismiss = { viewModel.hideFileOptions() },
            onPlay = { file ->
                onVideoSelected(Uri.fromFile(File(file.path)))
            },
            onAddToPlaylist = { file ->
                selectedFileForPlaylist = file
                showPlaylistDialog = true
            },
            onShare = { file ->
                val activity = context as? FolderBrowserActivity
                activity?.shareVideoFile(file)
            },
            onDelete = { file ->
                viewModel.deleteFile(file)
            }
        )
    }
    
    // Playlist selection dialog
    if (showPlaylistDialog && selectedFileForPlaylist != null) {
        PlaylistSelectionDialog(
            videoPath = selectedFileForPlaylist!!.path,
            videoName = selectedFileForPlaylist!!.name,
            playlistRepository = playlistRepository,
            onDismiss = { 
                showPlaylistDialog = false
                selectedFileForPlaylist = null
            },
            onPlaylistSelected = { playlist ->
                scope.launch {
                    // Generate a video ID based on file path
                    val videoId = selectedFileForPlaylist!!.path.hashCode().toLong()
                    playlistRepository.addVideoToPlaylist(playlist.id, videoId)
                    snackbarHostState.showSnackbar(
                        message = "Added to ${playlist.name}",
                        duration = SnackbarDuration.Short
                    )
                }
            },
            onCreateNew = {
                showPlaylistDialog = false
                showCreatePlaylistDialog = true
            }
        )
    }
    
    // Create playlist dialog
    if (showCreatePlaylistDialog) {
        CreatePlaylistDialog(
            onDismiss = { 
                showCreatePlaylistDialog = false
                selectedFileForPlaylist = null
            },
            onCreate = { name, description ->
                scope.launch {
                    val playlist = VideoPlaylist(
                        id = System.currentTimeMillis(),
                        name = name,
                        description = description,
                        dateCreated = System.currentTimeMillis(),
                        dateModified = System.currentTimeMillis()
                    )
                    playlistRepository.createPlaylist(playlist)
                    
                    // Add the video to the new playlist if there was a selected file
                    selectedFileForPlaylist?.let { file ->
                        val videoId = file.path.hashCode().toLong()
                        playlistRepository.addVideoToPlaylist(playlist.id, videoId)
                    }
                    
                    snackbarHostState.showSnackbar(
                        message = "Playlist created and video added",
                        duration = SnackbarDuration.Short
                    )
                    
                    showCreatePlaylistDialog = false
                    selectedFileForPlaylist = null
                }
            }
        )
    }
    }
}

@Composable
private fun BreadcrumbNavigation(
    path: String,
    onPathClick: (String) -> Unit
) {
    val pathSegments = path.split('/').filter { it.isNotEmpty() }
    
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF16213E))
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Root item
        item {
            BreadcrumbItem(
                text = "Storage",
                onClick = { onPathClick("") },
                isLast = pathSegments.isEmpty()
            )
        }
        
        // Path segments
        items(pathSegments.size) { index ->
            val segment = pathSegments[index]
            val segmentPath = "/" + pathSegments.take(index + 1).joinToString("/")
            
            BreadcrumbItem(
                text = segment,
                onClick = { onPathClick(segmentPath) },
                isLast = index == pathSegments.size - 1
            )
        }
    }
}

@Composable
private fun BreadcrumbItem(
    text: String,
    onClick: () -> Unit,
    isLast: Boolean
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = text,
            color = if (isLast) Color(0xFF00BCD4) else Color.White.copy(alpha = 0.7f),
            fontSize = 14.sp,
            fontWeight = if (isLast) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.clickable { onClick() }
        )
        
        if (!isLast) {
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.5f),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun FileList(
    files: List<FileItem>,
    isGridView: Boolean,
    onFileClick: (FileItem) -> Unit,
    onFileLongClick: (FileItem) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items(files) { file ->
            FileListItem(
                file = file,
                onClick = { onFileClick(file) },
                onLongClick = { onFileLongClick(file) }
            )
        }
    }
}

@Composable
private fun FileListItem(
    file: FileItem,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.05f)
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // File icon
            Icon(
                imageVector = getFileIcon(file),
                contentDescription = null,
                tint = getFileIconColor(file),
                modifier = Modifier.size(32.dp)
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // File info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = file.name,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (!file.isDirectory) {
                        Text(
                            text = formatFileSize(file.size),
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 12.sp
                        )
                    }
                    
                    Text(
                        text = formatDate(file.lastModified),
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 12.sp
                    )
                }
            }
            
            // More options
            IconButton(
                onClick = onLongClick,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "More options",
                    tint = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun ErrorMessage(
    error: String,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Error,
            contentDescription = "Error",
            tint = Color(0xFFFF4444),
            modifier = Modifier.size(64.dp)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Error loading files",
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = error,
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 14.sp
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF00BCD4)
            )
        ) {
            Text("Retry")
        }
    }
}

@Composable
private fun EmptyFolderMessage() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.FolderOpen,
            contentDescription = "Empty folder",
            tint = Color.White.copy(alpha = 0.5f),
            modifier = Modifier.size(64.dp)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "No files found",
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "This folder doesn't contain any video files",
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 14.sp
        )
    }
}

@Composable
private fun FileOptionsDialog(
    file: FileItem,
    onDismiss: () -> Unit,
    onPlay: (FileItem) -> Unit,
    onAddToPlaylist: (FileItem) -> Unit,
    onShare: (FileItem) -> Unit,
    onDelete: (FileItem) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = file.name,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        text = {
            Column {
                FileOptionItem(
                    icon = Icons.Default.PlayArrow,
                    text = "Play",
                    onClick = { onPlay(file); onDismiss() }
                )
                
                FileOptionItem(
                    icon = Icons.Default.PlaylistAdd,
                    text = "Add to Playlist",
                    onClick = { onAddToPlaylist(file); onDismiss() }
                )
                
                FileOptionItem(
                    icon = Icons.Default.Share,
                    text = "Share",
                    onClick = { onShare(file); onDismiss() }
                )
                
                FileOptionItem(
                    icon = Icons.Default.Delete,
                    text = "Delete",
                    onClick = { onDelete(file); onDismiss() },
                    isDestructive = true
                )
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
private fun FileOptionItem(
    icon: ImageVector,
    text: String,
    onClick: () -> Unit,
    isDestructive: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isDestructive) Color(0xFFFF4444) else Color(0xFF00BCD4),
            modifier = Modifier.size(24.dp)
        )
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Text(
            text = text,
            color = if (isDestructive) Color(0xFFFF4444) else Color.White,
            fontSize = 16.sp
        )
    }
}

// Helper functions
private fun getFileIcon(file: FileItem): ImageVector {
    return when {
        file.isDirectory -> Icons.Default.Folder
        file.isVideoFile -> Icons.Default.VideoFile
        else -> Icons.Default.InsertDriveFile
    }
}

private fun getFileIconColor(file: FileItem): Color {
    return when {
        file.isDirectory -> Color(0xFFFFC107)
        file.isVideoFile -> Color(0xFF00BCD4)
        else -> Color.White.copy(alpha = 0.7f)
    }
}

private fun formatFileSize(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
    return String.format("%.1f %s", bytes / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
}

private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    return sdf.format(Date(timestamp))
}