package com.astralplayer.nextplayer

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.astralplayer.nextplayer.ui.theme.AstralPlayerTheme
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class FolderBrowserActivity : ComponentActivity() {
    
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // Permission granted, refresh the view
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        checkAndRequestPermissions()
        
        setContent {
            AstralPlayerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    FolderBrowserScreen(
                        onNavigateBack = { finish() }
                    )
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
fun FolderBrowserScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var currentPath by remember { 
        mutableStateOf(Environment.getExternalStorageDirectory().absolutePath) 
    }
    var files by remember { mutableStateOf(listOf<FileItem>()) }
    var showHiddenFiles by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }
    
    // Load files for current path
    LaunchedEffect(currentPath, showHiddenFiles) {
        isLoading = true
        files = loadFilesFromPath(currentPath, showHiddenFiles)
        isLoading = false
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text(
                            text = "Folder Browser",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = currentPath.replace(Environment.getExternalStorageDirectory().absolutePath, "Storage"),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showHiddenFiles = !showHiddenFiles }) {
                        Icon(
                            imageVector = if (showHiddenFiles) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (showHiddenFiles) "Hide hidden files" else "Show hidden files"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
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
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
            // Parent directory item
            if (currentPath != Environment.getExternalStorageDirectory().absolutePath) {
                item {
                    FileItemRow(
                        fileItem = FileItem(
                            name = "..",
                            path = File(currentPath).parent ?: currentPath,
                            isDirectory = true,
                            size = 0,
                            lastModified = 0,
                            fileCount = 0
                        ),
                        onClick = {
                            currentPath = File(currentPath).parent ?: currentPath
                        }
                    )
                }
            }
            
            // Quick access folders
            if (currentPath == Environment.getExternalStorageDirectory().absolutePath) {
                item {
                    QuickAccessSection(
                        onFolderSelected = { path ->
                            if (File(path).exists()) {
                                currentPath = path
                            }
                        }
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                }
            }
            
            // File list
            items(files) { fileItem ->
                FileItemRow(
                    fileItem = fileItem,
                    onClick = {
                        if (fileItem.isDirectory) {
                            currentPath = fileItem.path
                        } else if (isVideoFile(fileItem.name)) {
                            val intent = Intent(context, VideoPlayerActivity::class.java).apply {
                                data = Uri.fromFile(File(fileItem.path))
                                putExtra("video_title", fileItem.name)
                            }
                            context.startActivity(intent)
                        }
                    }
                )
            }
            
            if (files.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.FolderOff,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "No files found",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
        }
    }
}

@Composable
fun QuickAccessSection(
    onFolderSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = "Quick Access",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        
        val quickAccessFolders = listOf(
            QuickAccessFolder("Downloads", "${Environment.getExternalStorageDirectory().absolutePath}/Download", Icons.Default.Download),
            QuickAccessFolder("Movies", Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES).absolutePath, Icons.Default.Movie),
            QuickAccessFolder("DCIM", "${Environment.getExternalStorageDirectory().absolutePath}/DCIM", Icons.Default.CameraAlt),
            QuickAccessFolder("WhatsApp", "${Environment.getExternalStorageDirectory().absolutePath}/WhatsApp/Media/WhatsApp Video", Icons.AutoMirrored.Filled.Message),
        )
        
        quickAccessFolders.forEach { folder ->
            QuickAccessItem(
                folder = folder,
                onClick = { onFolderSelected(folder.path) }
            )
        }
    }
}

@Composable
fun QuickAccessItem(
    folder: QuickAccessFolder,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
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
            Icon(
                imageVector = folder.icon,
                contentDescription = null,
                modifier = Modifier
                    .size(40.dp)
                    .clip(MaterialTheme.shapes.small)
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .padding(8.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Text(
                text = folder.name,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

@Composable
fun FileItemRow(
    fileItem: FileItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
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
            Icon(
                imageVector = when {
                    fileItem.name == ".." -> Icons.Default.ArrowUpward
                    fileItem.isDirectory -> Icons.Default.Folder
                    isVideoFile(fileItem.name) -> Icons.Default.VideoFile
                    else -> Icons.AutoMirrored.Filled.InsertDriveFile
                },
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = when {
                    fileItem.name == ".." -> MaterialTheme.colorScheme.primary
                    fileItem.isDirectory -> MaterialTheme.colorScheme.primary
                    isVideoFile(fileItem.name) -> MaterialTheme.colorScheme.secondary
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = fileItem.name,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (fileItem.isDirectory && fileItem.fileCount > 0) {
                        Text(
                            text = "${fileItem.fileCount} items",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else if (!fileItem.isDirectory) {
                        Text(
                            text = formatFileSize(fileItem.size),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    if (fileItem.lastModified > 0) {
                        Text(
                            text = "• ${formatDate(fileItem.lastModified)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

data class FileItem(
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val size: Long,
    val lastModified: Long,
    val fileCount: Int = 0
)

data class QuickAccessFolder(
    val name: String,
    val path: String,
    val icon: ImageVector
)

private fun loadFilesFromPath(path: String, showHidden: Boolean): List<FileItem> {
    val directory = File(path)
    if (!directory.exists() || !directory.isDirectory) {
        return emptyList()
    }
    
    return directory.listFiles()
        ?.filter { file ->
            (showHidden || !file.name.startsWith(".")) &&
            (file.isDirectory || isVideoFile(file.name))
        }
        ?.map { file ->
            FileItem(
                name = file.name,
                path = file.absolutePath,
                isDirectory = file.isDirectory,
                size = if (file.isFile) file.length() else 0,
                lastModified = file.lastModified(),
                fileCount = if (file.isDirectory) {
                    file.listFiles()?.count { 
                        it.isDirectory || isVideoFile(it.name) 
                    } ?: 0
                } else 0
            )
        }
        ?.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
        ?: emptyList()
}

private fun isVideoFile(fileName: String): Boolean {
    val videoExtensions = listOf(
        ".mp4", ".mkv", ".avi", ".mov", ".webm", ".flv", ".wmv",
        ".3gp", ".m4v", ".ts", ".m3u8", ".mpg", ".mpeg", ".m2ts"
    )
    return videoExtensions.any { fileName.lowercase().endsWith(it) }
}

private fun formatFileSize(size: Long): String {
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    var sizeValue = size.toDouble()
    var unitIndex = 0
    
    while (sizeValue >= 1024 && unitIndex < units.size - 1) {
        sizeValue /= 1024
        unitIndex++
    }
    
    return String.format("%.1f %s", sizeValue, units[unitIndex])
}

private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    return sdf.format(Date(timestamp))
}