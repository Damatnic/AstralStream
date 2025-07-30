package com.astralplayer.nextplayer

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
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
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.astralplayer.nextplayer.ui.theme.AstralPlayerTheme
import com.astralplayer.nextplayer.ui.components.LoadingState
import com.astralplayer.nextplayer.ui.components.NoVideosFoundState
import com.astralplayer.nextplayer.ui.components.ContentTransition
import com.astralplayer.nextplayer.ui.components.VideoThumbnail
import com.astralplayer.nextplayer.ui.components.NetworkStreamDialog
import com.astralplayer.nextplayer.ui.components.IntentHandlerDialog
import com.astralplayer.nextplayer.utils.ErrorHandler
import com.astralplayer.nextplayer.utils.IntentUtils
import com.astralplayer.nextplayer.utils.CodecManager
import com.astralplayer.nextplayer.feature.ai.GoogleAIStudioService
import com.astralplayer.nextplayer.feature.ai.SubtitleGenerationResult
import kotlinx.coroutines.launch
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    companion object {
        private const val TAG = "MainActivity"
    }
    
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Log.d(TAG, "Storage permission granted")
        } else {
            Log.w(TAG, "Storage permission denied")
            showPermissionDeniedDialog()
        }
    }
    
    private val requestMultiplePermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            Log.d(TAG, "All permissions granted")
        } else {
            Log.w(TAG, "Some permissions denied: $permissions")
            showPermissionDeniedDialog()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        Log.d(TAG, "MainActivity onCreate")
        
        // Handle intent if this activity was launched with a video intent
        if (handleVideoIntent(intent)) {
            // Intent was handled, finish this activity
            return
        }
        
        // Check and request permissions
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
    
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        Log.d(TAG, "MainActivity onNewIntent")
        IntentUtils.logIntentDetails(intent, TAG)
        
        // Handle new intent
        if (handleVideoIntent(intent)) {
            // Intent was handled, finish this activity if appropriate
            return
        }
    }
    
    private fun handleVideoIntent(intent: Intent): Boolean {
        return try {
            Log.d(TAG, "Checking for video intent")
            
            when (intent.action) {
                Intent.ACTION_VIEW,
                Intent.ACTION_SEND,
                Intent.ACTION_SEND_MULTIPLE -> {
                    val application = application as AstralVuApplication
                    if (application.handleVideoIntent(intent)) {
                        Log.d(TAG, "Video intent handled successfully")
                        finish() // Close MainActivity since we're opening VideoPlayerActivity
                        return true
                    }
                }
            }
            
            false
        } catch (e: Exception) {
            Log.e(TAG, "Error handling video intent", e)
            ErrorHandler.handleError(
                context = this,
                throwable = e,
                userMessage = "Failed to open video",
                errorType = ErrorHandler.ErrorType.FILE_ACCESS,
                showToast = true
            )
            false
        }
    }
    
    private fun checkAndRequestPermissions() {
        val permissionsToRequest = mutableListOf<String>()
        
        // Storage permissions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_VIDEO) 
                != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.READ_MEDIA_VIDEO)
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_AUDIO) 
                != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.READ_MEDIA_AUDIO)
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) 
                != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }
        
        // Other useful permissions
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) 
            != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.RECORD_AUDIO)
        }
        
        if (permissionsToRequest.isNotEmpty()) {
            Log.d(TAG, "Requesting permissions: $permissionsToRequest")
            if (permissionsToRequest.size == 1) {
                requestPermissionLauncher.launch(permissionsToRequest.first())
            } else {
                requestMultiplePermissionsLauncher.launch(permissionsToRequest.toTypedArray())
            }
        }
    }
    
    private fun showPermissionDeniedDialog() {
        // Show dialog explaining why permissions are needed
        // This could be implemented as a Compose dialog
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val context = LocalContext.current
    var videos by remember { mutableStateOf(listOf<VideoItem>()) }
    var isLoading by remember { mutableStateOf(true) }
    var showCodecInfo by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    var testResult by remember { mutableStateOf<String?>(null) }
    var showTestDialog by remember { mutableStateOf(false) }
    var showNetworkStreamDialog by remember { mutableStateOf(false) }
    var showIntentHandlerDialog by remember { mutableStateOf(false) }
    
    // Get codec manager from application
    val application = context.applicationContext as AstralVuApplication
    val codecManager = application.codecManager
    val codecInfo by codecManager.codecInfo.collectAsState()
    
    LaunchedEffect(Unit) {
        isLoading = true
        videos = loadVideos(context)
        isLoading = false
    }
    
    Scaffold(
        floatingActionButton = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Network stream FAB
                FloatingActionButton(
                    onClick = { showNetworkStreamDialog = true },
                    modifier = Modifier.size(56.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Stream,
                        contentDescription = "Network Stream"
                    )
                }
                
                // Intent handler FAB (for testing)
                FloatingActionButton(
                    onClick = { showIntentHandlerDialog = true },
                    modifier = Modifier.size(48.dp),
                    containerColor = MaterialTheme.colorScheme.secondary
                ) {
                    Icon(
                        imageVector = Icons.Default.VideoLibrary,
                        contentDescription = "Test Intent Handler"
                    )
                }
            }
        },
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text("Astral Player")
                        if (codecInfo.hardwareDecodersAvailable.isNotEmpty()) {
                            Text(
                                text = "HW Decoders: ${codecInfo.hardwareDecodersAvailable.size}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                actions = {
                    // Codec info button
                    IconButton(onClick = { showCodecInfo = true }) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Codec Info",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    
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
                    EnhancedVideoCard(video) {
                        val intent = Intent(context, VideoPlayerActivity::class.java).apply {
                            data = video.uri
                            putExtra("video_title", video.name)
                            putExtra("from_external", false)
                        }
                        context.startActivity(intent)
                    }
                }
            }
        }
    }
    
    // Codec Info Dialog
    if (showCodecInfo) {
        AlertDialog(
            onDismissRequest = { showCodecInfo = false },
            title = { Text("Codec Information") },
            text = {
                LazyColumn(
                    modifier = Modifier.height(400.dp)
                ) {
                    item {
                        Text(
                            text = codecManager.getCodecInfoString(),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showCodecInfo = false }) {
                    Text("Close")
                }
            }
        )
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
                    putExtra("streaming_mode", true)
                    putExtra("from_external", false)
                }
                context.startActivity(intent)
            },
            onDismiss = { showNetworkStreamDialog = false }
        )
    }
    
    // Intent Handler Test Dialog
    if (showIntentHandlerDialog) {
        IntentHandlerDialog(
            onIntentTest = { testUri, testTitle ->
                // Create a test intent
                val testIntent = Intent(Intent.ACTION_VIEW).apply {
                    data = testUri
                    type = "video/*"
                    putExtra("video_title", testTitle)
                }
                
                // Test the intent handler
                val application = context.applicationContext as AstralVuApplication
                if (application.handleVideoIntent(testIntent)) {
                    showIntentHandlerDialog = false
                }
            },
            onDismiss = { showIntentHandlerDialog = false }
        )
    }
}

@Composable
fun EnhancedVideoCard(video: VideoItem, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(90.dp),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Video thumbnail with enhanced features
            VideoThumbnail(
                videoUri = video.uri,
                duration = video.duration,
                modifier = Modifier
                    .width(160.dp)
                    .fillMaxHeight(),
                showDuration = true,
                useThumbnailService = true
            )
            
            // Video info with additional metadata
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = video.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = formatDuration(video.duration),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    // Show format info if available
                    video.format?.let { format ->
                        Text(
                            text = format.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                
                // Additional metadata
                if (video.resolution != null || video.bitrate != null) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        video.resolution?.let { res ->
                            Text(
                                text = res,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        video.bitrate?.let { bitrate ->
                            Text(
                                text = "${bitrate}kbps",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

data class VideoItem(
    val uri: Uri,
    val name: String,
    val duration: Long,
    val format: String? = null,
    val resolution: String? = null,
    val bitrate: Int? = null,
    val size: Long = 0L
)

fun loadVideos(context: android.content.Context): List<VideoItem> {
    val videos = mutableListOf<VideoItem>()
    
    val projection = arrayOf(
        MediaStore.Video.Media._ID,
        MediaStore.Video.Media.DISPLAY_NAME,
        MediaStore.Video.Media.DURATION,
        MediaStore.Video.Media.SIZE,
        MediaStore.Video.Media.RESOLUTION,
        MediaStore.Video.Media.BITRATE,
        MediaStore.Video.Media.MIME_TYPE
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
            val sizeColumn = cursor.getColumnIndex(MediaStore.Video.Media.SIZE)
            val resolutionColumn = cursor.getColumnIndex(MediaStore.Video.Media.RESOLUTION)
            val bitrateColumn = cursor.getColumnIndex(MediaStore.Video.Media.BITRATE)
            val mimeTypeColumn = cursor.getColumnIndex(MediaStore.Video.Media.MIME_TYPE)
            
            while (cursor.moveToNext()) {
                try {
                    val id = cursor.getLong(idColumn)
                    val name = cursor.getString(nameColumn)
                    val duration = cursor.getLong(durationColumn)
                    
                    val size = if (sizeColumn >= 0) cursor.getLong(sizeColumn) else 0L
                    val resolution = if (resolutionColumn >= 0) cursor.getString(resolutionColumn) else null
                    val bitrate = if (bitrateColumn >= 0) cursor.getInt(bitrateColumn) else null
                    val mimeType = if (mimeTypeColumn >= 0) cursor.getString(mimeTypeColumn) else null
                    
                    val uri = Uri.withAppendedPath(
                        MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                        id.toString()
                    )
                    
                    // Extract format from MIME type
                    val format = mimeType?.substringAfter("/")
                    
                    videos.add(
                        VideoItem(
                            uri = uri,
                            name = name,
                            duration = duration,
                            format = format,
                            resolution = resolution,
                            bitrate = bitrate,
                            size = size
                        )
                    )
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