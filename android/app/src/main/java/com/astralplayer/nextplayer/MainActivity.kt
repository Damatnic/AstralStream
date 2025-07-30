package com.astralplayer.nextplayer

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.ui.Alignment
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Stream
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.astralplayer.nextplayer.ui.theme.AstralPlayerTheme
import com.astralplayer.nextplayer.ui.components.LoadingState
import com.astralplayer.nextplayer.ui.components.NoVideosFoundState
import com.astralplayer.nextplayer.ui.components.ContentTransition
import com.astralplayer.nextplayer.utils.ErrorHandler
import com.astralplayer.nextplayer.feature.ai.GoogleAIStudioService
import com.astralplayer.nextplayer.feature.ai.SubtitleGenerationResult
import android.util.Log
import kotlinx.coroutines.launch
import com.astralplayer.nextplayer.ui.components.VideoThumbnail
import com.astralplayer.nextplayer.ui.components.NetworkStreamDialog

class MainActivity : ComponentActivity() {
    
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // Permission granted, can access videos
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Check permissions
        checkAndRequestPermissions()
        
        setContent {
            AstralPlayerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen()
                }
            }
        }
    }
    
    private fun checkAndRequestPermissions() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_VIDEO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        
        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(permission)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val context = LocalContext.current
    var videos by remember { mutableStateOf(listOf<VideoItem>()) }
    var isLoading by remember { mutableStateOf(true) }
    val coroutineScope = rememberCoroutineScope()
    var testResult by remember { mutableStateOf<String?>(null) }
    var showTestDialog by remember { mutableStateOf(false) }
    var showNetworkStreamDialog by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        isLoading = true
        videos = loadVideos(context)
        isLoading = false
    }
    
    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showNetworkStreamDialog = true },
                icon = {
                    Icon(
                        imageVector = Icons.Default.Stream,
                        contentDescription = "Network Stream"
                    )
                },
                text = { Text("Network Stream") }
            )
        },
        topBar = {
            TopAppBar(
                title = { Text("Astral Player") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                actions = {
                    IconButton(onClick = {
                        val intent = Intent(context, SearchActivity::class.java)
                        context.startActivity(intent)
                    }) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    IconButton(onClick = {
                        val intent = Intent(context, FolderBrowserActivity::class.java)
                        context.startActivity(intent)
                    }) {
                        Icon(
                            imageVector = Icons.Default.Folder,
                            contentDescription = "Browse Folders",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    IconButton(onClick = {
                        val intent = Intent(context, RecentFilesActivity::class.java)
                        context.startActivity(intent)
                    }) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = "Recent Files",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    IconButton(onClick = {
                        val intent = Intent(context, SettingsActivity::class.java)
                        context.startActivity(intent)
                    }) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    
                    // Debug: AI Test Button
                    IconButton(onClick = {
                        showTestDialog = true
                        coroutineScope.launch {
                            testResult = "Testing AI service..."
                            val aiService = GoogleAIStudioService(context)
                            aiService.testConnectivity().collect { result ->
                                testResult = when (result) {
                                    is SubtitleGenerationResult.Progress -> result.message
                                    is SubtitleGenerationResult.Success -> "✅ SUCCESS: ${result.subtitleContent}"
                                    is SubtitleGenerationResult.Error -> "❌ ERROR: ${result.message}"
                                }
                            }
                        }
                    }) {
                        Icon(
                            imageVector = Icons.Default.BugReport,
                            contentDescription = "Test AI Service",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        if (isLoading) {
            LoadingState(
                message = "Loading videos...",
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            )
        } else if (videos.isEmpty()) {
            NoVideosFoundState(
                onOpenFileManager = {
                    val intent = Intent(context, FolderBrowserActivity::class.java)
                    context.startActivity(intent)
                },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(videos) { video ->
                    VideoCard(video) {
                        val intent = Intent(context, VideoPlayerActivity::class.java).apply {
                            data = video.uri
                            putExtra("video_title", video.name)
                        }
                        context.startActivity(intent)
                    }
                }
            }
        }
    }
    
    // AI Test Dialog
    if (showTestDialog) {
        AlertDialog(
            onDismissRequest = { showTestDialog = false },
            title = { Text("AI Service Test") },
            text = { 
                Text(testResult ?: "No result yet")
            },
            confirmButton = {
                TextButton(onClick = { showTestDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
    
    // Network Stream Dialog
    if (showNetworkStreamDialog) {
        NetworkStreamDialog(
            onStreamSelected = { uri, title ->
                val intent = Intent(context, VideoPlayerActivity::class.java).apply {
                    data = uri
                    putExtra("video_title", title)
                }
                context.startActivity(intent)
            },
            onDismiss = { showNetworkStreamDialog = false }
        )
    }
}

@Composable
fun VideoCard(video: VideoItem, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Video thumbnail
            VideoThumbnail(
                videoUri = video.uri,
                duration = video.duration,
                modifier = Modifier
                    .width(120.dp)
                    .fillMaxHeight(),
                showDuration = false,
                useThumbnailService = true
            )
            
            // Video info
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = video.name,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = formatDuration(video.duration),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

data class VideoItem(
    val uri: Uri,
    val name: String,
    val duration: Long
)

fun loadVideos(context: android.content.Context): List<VideoItem> {
    val videos = mutableListOf<VideoItem>()
    
    val projection = arrayOf(
        MediaStore.Video.Media._ID,
        MediaStore.Video.Media.DISPLAY_NAME,
        MediaStore.Video.Media.DURATION
    )
    
    val selection = "${MediaStore.Video.Media.DURATION} > ?"
    val selectionArgs = arrayOf("0")
    val sortOrder = "${MediaStore.Video.Media.DATE_ADDED} DESC"
    
    try {
        context.contentResolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            sortOrder
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
            val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
            
            while (cursor.moveToNext()) {
                try {
                    val id = cursor.getLong(idColumn)
                    val name = cursor.getString(nameColumn)
                    val duration = cursor.getLong(durationColumn)
                    
                    val uri = Uri.withAppendedPath(
                        MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                        id.toString()
                    )
                    
                    videos.add(VideoItem(uri, name, duration))
                } catch (e: Exception) {
                    Log.e("MainActivity", "Error loading video item", e)
                    // Continue with next item
                }
            }
        }
    } catch (e: SecurityException) {
        ErrorHandler.handleError(
            context = context,
            throwable = e,
            userMessage = "Permission denied. Please grant storage permission.",
            errorType = ErrorHandler.ErrorType.PERMISSION,
            showToast = true
        )
    } catch (e: Exception) {
        ErrorHandler.handleError(
            context = context,
            throwable = e,
            userMessage = "Failed to load videos",
            errorType = ErrorHandler.ErrorType.FILE_ACCESS,
            showToast = false
        )
    }
    
    return videos
}

private fun formatDuration(duration: Long): String {
    val seconds = duration / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    
    return when {
        hours > 0 -> String.format("%d:%02d:%02d", hours, minutes % 60, seconds % 60)
        else -> String.format("%d:%02d", minutes, seconds % 60)
    }
}