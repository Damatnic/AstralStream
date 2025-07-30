package com.astralplayer.nextplayer

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.astralplayer.nextplayer.ui.theme.AstralTheme
import com.astralplayer.nextplayer.feature.web.VideoUrlExtractor
import kotlinx.coroutines.launch
import java.io.File
// import dagger.hilt.android.AndroidEntryPoint // Temporarily disabled
import androidx.lifecycle.lifecycleScope
import android.content.pm.PackageManager
import android.Manifest
import androidx.core.content.ContextCompat

// @AndroidEntryPoint // Temporarily disabled
class MainActivity : ComponentActivity() {
    
    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { 
            if (isVideoFile(it)) {
                openVideoPlayer(it)
            } else {
                Toast.makeText(this, "Please select a valid video file", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        // Handle permission result if needed
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Check for notification permission on Android 13+
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        
        // Handle intent if app was opened with a video file
        handleIncomingIntent(intent)
        
        setContent {
            AstralTheme {
                ModernVideoPlayerHome(
                    onOpenFile = { 
                        filePickerLauncher.launch("video/*") 
                    },
                    onOpenUrl = { url -> 
                        if (isValidUrl(url)) {
                            openVideoPlayer(Uri.parse(url))
                        } else {
                            Toast.makeText(this, "Invalid URL", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onOpenFolder = { openFolderBrowser() },
                    onOpenRecent = { openRecentFiles() },
                    onOpenPlaylist = { openPlaylists() },
                    onOpenSettings = { openSettings() }
                )
            }
        }
    }
    
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIncomingIntent(intent)
    }
    
    private fun handleIncomingIntent(intent: Intent) {
        when (intent.action) {
            Intent.ACTION_VIEW -> {
                intent.data?.let { uri ->
                    when {
                        isVideoFile(uri) -> openVideoPlayer(uri)
                        isWebUrl(uri.toString()) -> handleWebUrl(uri.toString())
                    }
                }
            }
            Intent.ACTION_SEND -> {
                intent.getStringExtra(Intent.EXTRA_TEXT)?.let { text ->
                    if (isValidUrl(text)) {
                        if (isWebUrl(text)) {
                            handleWebUrl(text)
                        } else {
                            openVideoPlayer(Uri.parse(text))
                        }
                    }
                }
            }
        }
    }
    
    private fun isWebUrl(url: String): Boolean {
        return url.startsWith("http://") || url.startsWith("https://")
    }
    
    private fun handleWebUrl(url: String) {
        // Show loading dialog
        Toast.makeText(this, "Extracting video URL...", Toast.LENGTH_SHORT).show()
        
        lifecycleScope.launch {
            try {
                val extractor = VideoUrlExtractor(this@MainActivity)
                val videos = extractor.extractVideoUrls(url)
                
                if (videos.isNotEmpty()) {
                    // Use the highest quality video
                    val bestVideo = videos.first()
                    openVideoPlayer(Uri.parse(bestVideo.url))
                } else {
                    // If no video found, try to open URL directly
                    openVideoPlayer(Uri.parse(url))
                }
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "Failed to extract video", Toast.LENGTH_SHORT).show()
                // Fallback: try to open URL directly
                openVideoPlayer(Uri.parse(url))
            }
        }
    }
    
    private fun isVideoFile(uri: Uri): Boolean {
        val mimeType = contentResolver.getType(uri)
        return mimeType?.startsWith("video/") == true || 
               uri.toString().matches(Regex(".*\\.(mp4|mkv|avi|mov|webm|flv|wmv|3gp|m4v)$", RegexOption.IGNORE_CASE))
    }
    
    private fun isValidUrl(url: String): Boolean {
        return url.matches(Regex("^(https?|rtsp|rtmp|file)://.*"))
    }
    
    private fun openVideoPlayer(uri: Uri) {
        val intent = Intent(this, VideoPlayerActivity::class.java).apply {
            data = uri
            putExtra("video_title", getVideoTitle(uri))
        }
        startActivity(intent)
    }
    
    private fun getVideoTitle(uri: Uri): String {
        return try {
            when (uri.scheme) {
                "content" -> {
                    contentResolver.query(uri, arrayOf("_display_name"), null, null, null)?.use { cursor ->
                        if (cursor.moveToFirst()) {
                            cursor.getString(0)
                        } else uri.lastPathSegment ?: "Video"
                    } ?: "Video"
                }
                "file" -> File(uri.path ?: "").name
                else -> uri.lastPathSegment ?: "Stream"
            }
        } catch (e: Exception) {
            "Video"
        }
    }
    
    private fun openFolderBrowser() {
        val intent = Intent(this, FolderBrowserActivity::class.java)
        startActivity(intent)
    }
    
    private fun openRecentFiles() {
        val intent = Intent(this, RecentFilesActivity::class.java)
        startActivity(intent)
    }
    
    private fun openPlaylists() {
        val intent = Intent(this, PlaylistActivity::class.java)
        startActivity(intent)
    }
    
    private fun openSettings() {
        val intent = Intent(this, SettingsActivity::class.java)
        startActivity(intent)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModernVideoPlayerHome(
    onOpenFile: () -> Unit,
    onOpenUrl: (String) -> Unit,
    onOpenFolder: () -> Unit,
    onOpenRecent: () -> Unit,
    onOpenPlaylist: () -> Unit,
    onOpenSettings: () -> Unit
) {
    var showUrlDialog by remember { mutableStateOf(false) }
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = Color(0xFF0A0E27)
            ) {
                DrawerContent(
                    onClose = { scope.launch { drawerState.close() } },
                    onSettings = {
                        scope.launch { drawerState.close() }
                        onOpenSettings()
                    }
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayCircle,
                                contentDescription = null,
                                tint = Color(0xFF00D4FF),
                                modifier = Modifier.size(32.dp)
                            )
                            Text(
                                "Astral Player",
                                fontWeight = FontWeight.Bold,
                                fontSize = 24.sp
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "Menu",
                                tint = Color.White
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = onOpenSettings) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings",
                                tint = Color.White
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color(0xFF0A0E27),
                        titleContentColor = Color.White
                    )
                )
            },
            containerColor = Color(0xFF0A0E27)
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Animated background
                AnimatedBackground()
                
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // Quick Actions Grid
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(quickActions) { action ->
                            QuickActionCard(
                                action = action,
                                onClick = {
                                    when (action.type) {
                                        ActionType.OPEN_FILE -> onOpenFile()
                                        ActionType.OPEN_URL -> showUrlDialog = true
                                        ActionType.FOLDERS -> onOpenFolder()
                                        ActionType.RECENT -> onOpenRecent()
                                        ActionType.PLAYLISTS -> onOpenPlaylist()
                                        ActionType.STREAMING -> showUrlDialog = true
                                    }
                                }
                            )
                        }
                    }
                    
                    // Bottom Features Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        FeatureChip("AI Subtitles", Icons.Default.Subtitles)
                        FeatureChip("HDR Support", Icons.Default.HighQuality)
                        FeatureChip("Cast", Icons.Default.Cast)
                    }
                }
            }
        }
        
        if (showUrlDialog) {
            StreamUrlDialog(
                onDismiss = { showUrlDialog = false },
                onConfirm = { url ->
                    showUrlDialog = false
                    onOpenUrl(url)
                }
            )
        }
    }
}

@Composable
fun AnimatedBackground() {
    val infiniteTransition = rememberInfiniteTransition()
    
    val animatedColor by infiniteTransition.animateColor(
        initialValue = Color(0xFF1A237E),
        targetValue = Color(0xFF311B92),
        animationSpec = infiniteRepeatable(
            animation = tween(3000),
            repeatMode = RepeatMode.Reverse
        )
    )
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(
                        animatedColor.copy(alpha = 0.3f),
                        Color(0xFF0A0E27)
                    ),
                    radius = 800f
                )
            )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickActionCard(
    action: QuickAction,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = action.backgroundColor.copy(alpha = 0.15f)
        ),
        border = BorderStroke(
            width = 1.dp,
            color = action.accentColor.copy(alpha = 0.3f)
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Icon(
                    imageVector = action.icon,
                    contentDescription = action.title,
                    tint = action.accentColor,
                    modifier = Modifier.size(40.dp)
                )
                
                Column {
                    Text(
                        text = action.title,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text(
                        text = action.description,
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 12.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            
            // Glow effect
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .align(Alignment.TopEnd)
                    .blur(30.dp)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                action.accentColor.copy(alpha = 0.3f),
                                Color.Transparent
                            )
                        )
                    )
            )
        }
    }
}

@Composable
fun FeatureChip(
    label: String,
    icon: ImageVector
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color(0xFF1E88E5).copy(alpha = 0.2f),
        border = BorderStroke(1.dp, Color(0xFF1E88E5).copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color(0xFF1E88E5),
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = label,
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun DrawerContent(
    onClose: () -> Unit,
    onSettings: () -> Unit
) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Astral Player",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = onClose) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = Color.White
                )
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        DrawerItem(
            icon = Icons.Default.Info,
            title = "About",
            onClick = { 
                val intent = Intent(context, AboutActivity::class.java)
                context.startActivity(intent)
            }
        )
        
        DrawerItem(
            icon = Icons.Default.Star,
            title = "Rate App",
            onClick = { 
                val uri = android.net.Uri.parse("market://details?id=${context.packageName}")
                val intent = Intent(Intent.ACTION_VIEW, uri)
                try {
                    context.startActivity(intent)
                } catch (e: android.content.ActivityNotFoundException) {
                    // Fallback to web
                    val webUri = android.net.Uri.parse("https://play.google.com/store/apps/details?id=${context.packageName}")
                    context.startActivity(Intent(Intent.ACTION_VIEW, webUri))
                }
            }
        )
        
        DrawerItem(
            icon = Icons.Default.Share,
            title = "Share App",
            onClick = { 
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, "Check out Astral Player - The ultimate video player for Android!\n\nDownload it from: https://play.google.com/store/apps/details?id=${context.packageName}")
                }
                context.startActivity(Intent.createChooser(shareIntent, "Share Astral Player"))
            }
        )
        
        Spacer(modifier = Modifier.weight(1f))
        
        DrawerItem(
            icon = Icons.Default.Settings,
            title = "Settings",
            onClick = onSettings
        )
    }
}

@Composable
fun DrawerItem(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = Color(0xFF00D4FF)
            )
            Text(
                text = title,
                color = Color.White,
                fontSize = 16.sp
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StreamUrlDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var urlText by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1A237E),
        title = {
            Text(
                "Stream from URL",
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    "Enter a video URL (HTTP, HTTPS, RTSP, etc.)",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 14.sp
                )
                
                OutlinedTextField(
                    value = urlText,
                    onValueChange = { urlText = it },
                    label = { Text("Video URL", color = Color.White.copy(alpha = 0.7f)) },
                    placeholder = { Text("https://example.com/video.mp4", color = Color.White.copy(alpha = 0.5f)) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF00D4FF),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = Color(0xFF00D4FF)
                    ),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                
                // Quick presets
                Text(
                    "Quick Presets:",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 12.sp
                )
                
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(streamPresets) { preset ->
                        AssistChip(
                            onClick = { urlText = preset.url },
                            label = { Text(preset.name, fontSize = 12.sp) },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = Color(0xFF00D4FF).copy(alpha = 0.2f),
                                labelColor = Color.White
                            )
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(urlText) },
                enabled = urlText.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF00D4FF)
                )
            ) {
                Text("Play", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color.White.copy(alpha = 0.7f))
            }
        }
    )
}

// Data classes
data class QuickAction(
    val icon: ImageVector,
    val title: String,
    val description: String,
    val type: ActionType,
    val accentColor: Color,
    val backgroundColor: Color = accentColor
)

enum class ActionType {
    OPEN_FILE, OPEN_URL, FOLDERS, RECENT, PLAYLISTS, STREAMING
}

// Data
val quickActions = listOf(
    QuickAction(
        icon = Icons.Default.VideoFile,
        title = "Open File",
        description = "Browse local videos",
        type = ActionType.OPEN_FILE,
        accentColor = Color(0xFF00D4FF)
    ),
    QuickAction(
        icon = Icons.Default.Link,
        title = "Stream URL",
        description = "Play from web",
        type = ActionType.OPEN_URL,
        accentColor = Color(0xFF76FF03)
    ),
    QuickAction(
        icon = Icons.Default.Folder,
        title = "Folders",
        description = "Browse by folder",
        type = ActionType.FOLDERS,
        accentColor = Color(0xFFFF6E40)
    ),
    QuickAction(
        icon = Icons.Default.History,
        title = "Recent",
        description = "Recently played",
        type = ActionType.RECENT,
        accentColor = Color(0xFFE040FB)
    ),
    QuickAction(
        icon = Icons.AutoMirrored.Filled.PlaylistPlay,
        title = "Playlists",
        description = "Your collections",
        type = ActionType.PLAYLISTS,
        accentColor = Color(0xFFFFAB00)
    ),
    QuickAction(
        icon = Icons.Default.Stream,
        title = "Live Streams",
        description = "HLS, DASH, etc.",
        type = ActionType.STREAMING,
        accentColor = Color(0xFFFF5252)
    )
)

val streamPresets = AppConfig.DEMO_STREAMS