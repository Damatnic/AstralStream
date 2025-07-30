package com.astralplayer.nextplayer

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.astralplayer.nextplayer.ui.theme.AstralTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class FolderBrowserActivity : ComponentActivity() {
    
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // Permission granted, proceed with folder browsing
        } else {
            // Permission denied, show explanation
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            AstralTheme {
                val viewModel: FolderBrowserViewModel = viewModel()
                
                FolderBrowserScreen(
                    viewModel = viewModel,
                    onBackClick = { finish() },
                    onVideoClick = { file ->
                        val intent = Intent(this, VideoPlayerActivity::class.java).apply {
                            data = Uri.fromFile(file)
                            putExtra("video_title", file.name)
                        }
                        startActivity(intent)
                    },
                    onRequestPermission = { requestStoragePermission() }
                )
            }
        }
        
        // Check permission on start
        checkStoragePermission()
    }
    
    private fun checkStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                intent.data = Uri.parse("package:$packageName")
                startActivity(intent)
            }
        } else {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }
    }
    
    private fun requestStoragePermission() {
        checkStoragePermission()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderBrowserScreen(
    viewModel: FolderBrowserViewModel,
    onBackClick: () -> Unit,
    onVideoClick: (File) -> Unit,
    onRequestPermission: () -> Unit
) {
    val currentPath by viewModel.currentPath.collectAsState()
    val files by viewModel.files.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val hasPermission by viewModel.hasPermission.collectAsState()
    var showSortMenu by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Folder Browser",
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = currentPath,
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.7f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (!viewModel.navigateUp()) {
                            onBackClick()
                        }
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.goToRoot() }) {
                        Icon(
                            imageVector = Icons.Default.Home,
                            contentDescription = "Go to root"
                        )
                    }
                    
                    IconButton(onClick = { showSortMenu = true }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Sort,
                            contentDescription = "Sort"
                        )
                    }
                    
                    DropdownMenu(
                        expanded = showSortMenu,
                        onDismissRequest = { showSortMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Sort by Name") },
                            onClick = {
                                viewModel.sortBy(SortType.NAME)
                                showSortMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Sort by Date") },
                            onClick = {
                                viewModel.sortBy(SortType.DATE)
                                showSortMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Sort by Size") },
                            onClick = {
                                viewModel.sortBy(SortType.SIZE)
                                showSortMenu = false
                            }
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
        when {
            !hasPermission -> {
                PermissionRequestView(
                    onRequestPermission = onRequestPermission,
                    modifier = Modifier.padding(paddingValues)
                )
            }
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
            files.isEmpty() -> {
                EmptyFolderView(
                    modifier = Modifier.padding(paddingValues)
                )
            }
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Quick access shortcuts
                    item {
                        QuickAccessRow(
                            onNavigate = { path -> viewModel.navigateToPath(path) }
                        )
                    }
                    
                    // Files and folders
                    items(files) { fileItem ->
                        FileItemCard(
                            fileItem = fileItem,
                            onClick = {
                                if (fileItem.isDirectory) {
                                    viewModel.navigateToFolder(fileItem.file)
                                } else if (fileItem.isVideo) {
                                    onVideoClick(fileItem.file)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun QuickAccessRow(onNavigate: (String) -> Unit) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(bottom = 16.dp)
    ) {
        val quickPaths = listOf(
            QuickPath("Internal", Environment.getExternalStorageDirectory().absolutePath, Icons.Default.Storage),
            QuickPath("Downloads", Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).absolutePath, Icons.Default.Download),
            QuickPath("Movies", Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES).absolutePath, Icons.Default.Movie),
            QuickPath("DCIM", Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).absolutePath, Icons.Default.Camera)
        )
        
        items(quickPaths) { path ->
            AssistChip(
                onClick = { onNavigate(path.path) },
                label = { Text(path.name) },
                leadingIcon = {
                    Icon(
                        imageVector = path.icon,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = Color(0xFF00D4FF).copy(alpha = 0.2f),
                    labelColor = Color.White
                )
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileItemCard(
    fileItem: FileItem,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.05f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        color = when {
                            fileItem.isDirectory -> Color(0xFF1E88E5).copy(alpha = 0.2f)
                            fileItem.isVideo -> Color(0xFF00BCD4).copy(alpha = 0.2f)
                            else -> Color.Gray.copy(alpha = 0.2f)
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when {
                        fileItem.isDirectory -> Icons.Default.Folder
                        fileItem.isVideo -> Icons.Default.VideoFile
                        else -> Icons.AutoMirrored.Filled.InsertDriveFile
                    },
                    contentDescription = null,
                    tint = when {
                        fileItem.isDirectory -> Color(0xFF1E88E5)
                        fileItem.isVideo -> Color(0xFF00BCD4)
                        else -> Color.Gray
                    },
                    modifier = Modifier.size(24.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // File info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = fileItem.name,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    if (fileItem.isDirectory) {
                        Text(
                            text = "${fileItem.itemCount} items",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 12.sp
                        )
                    } else {
                        Text(
                            text = formatFileSize(fileItem.size),
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 12.sp
                        )
                        
                        Text(
                            text = " • ",
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 12.sp
                        )
                        
                        Text(
                            text = formatDate(fileItem.lastModified),
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 12.sp
                        )
                    }
                }
            }
            
            if (!fileItem.isDirectory) {
                Icon(
                    imageVector = Icons.Default.PlayCircle,
                    contentDescription = "Play",
                    tint = Color(0xFF00D4FF),
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}

@Composable
fun PermissionRequestView(
    onRequestPermission: () -> Unit,
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
                imageVector = Icons.Default.FolderOpen,
                contentDescription = null,
                tint = Color(0xFF00D4FF),
                modifier = Modifier.size(64.dp)
            )
            
            Text(
                text = "Storage Permission Required",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            
            Text(
                text = "To browse folders and play videos,\nplease grant storage permission",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 14.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = onRequestPermission,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF00D4FF)
                )
            ) {
                Text("Grant Permission", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun EmptyFolderView(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.FolderOff,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.3f),
                modifier = Modifier.size(64.dp)
            )
            
            Text(
                text = "Empty Folder",
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 18.sp
            )
            
            Text(
                text = "No files or folders found here",
                color = Color.White.copy(alpha = 0.3f),
                fontSize = 14.sp
            )
        }
    }
}

// ViewModel
class FolderBrowserViewModel : ViewModel() {
    
    private val _currentPath = MutableStateFlow(Environment.getExternalStorageDirectory().absolutePath)
    val currentPath: StateFlow<String> = _currentPath
    
    private val _files = MutableStateFlow<List<FileItem>>(emptyList())
    val files: StateFlow<List<FileItem>> = _files
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading
    
    private val _hasPermission = MutableStateFlow(true)
    val hasPermission: StateFlow<Boolean> = _hasPermission
    
    private val pathHistory = mutableListOf<String>()
    private var currentSortType = SortType.NAME
    
    private val videoExtensions = listOf("mp4", "mkv", "avi", "mov", "webm", "flv", "wmv", "3gp", "m4v", "ts", "m3u8")
    
    init {
        loadCurrentPath()
    }
    
    private fun loadCurrentPath() {
        viewModelScope.launch {
            _isLoading.value = true
            
            val filesList = withContext(Dispatchers.IO) {
                val directory = File(_currentPath.value)
                if (directory.exists() && directory.canRead()) {
                    directory.listFiles()?.mapNotNull { file ->
                        if (file.isHidden) return@mapNotNull null
                        
                        FileItem(
                            file = file,
                            name = file.name,
                            isDirectory = file.isDirectory,
                            isVideo = isVideoFile(file),
                            size = if (file.isFile) file.length() else 0,
                            lastModified = file.lastModified(),
                            itemCount = if (file.isDirectory) {
                                file.listFiles()?.size ?: 0
                            } else 0
                        )
                    } ?: emptyList()
                } else {
                    emptyList()
                }
            }
            
            _files.value = sortFiles(filesList)
            _isLoading.value = false
        }
    }
    
    private fun isVideoFile(file: File): Boolean {
        if (file.isDirectory) return false
        val extension = file.extension.lowercase()
        return videoExtensions.contains(extension)
    }
    
    private fun sortFiles(files: List<FileItem>): List<FileItem> {
        val (folders, videoFiles) = files.partition { it.isDirectory }
        
        val sortedFolders = when (currentSortType) {
            SortType.NAME -> folders.sortedBy { it.name.lowercase() }
            SortType.DATE -> folders.sortedByDescending { it.lastModified }
            SortType.SIZE -> folders.sortedBy { it.name.lowercase() } // Folders by name
        }
        
        val sortedFiles = when (currentSortType) {
            SortType.NAME -> videoFiles.sortedBy { it.name.lowercase() }
            SortType.DATE -> videoFiles.sortedByDescending { it.lastModified }
            SortType.SIZE -> videoFiles.sortedByDescending { it.size }
        }
        
        return sortedFolders + sortedFiles
    }
    
    fun navigateToFolder(folder: File) {
        if (folder.exists() && folder.isDirectory && folder.canRead()) {
            pathHistory.add(_currentPath.value)
            _currentPath.value = folder.absolutePath
            loadCurrentPath()
        }
    }
    
    fun navigateToPath(path: String) {
        val folder = File(path)
        if (folder.exists() && folder.isDirectory && folder.canRead()) {
            pathHistory.clear()
            _currentPath.value = path
            loadCurrentPath()
        }
    }
    
    fun navigateUp(): Boolean {
        return if (pathHistory.isNotEmpty()) {
            _currentPath.value = pathHistory.removeAt(pathHistory.lastIndex)
            loadCurrentPath()
            true
        } else {
            val parent = File(_currentPath.value).parentFile
            if (parent != null && parent.canRead()) {
                _currentPath.value = parent.absolutePath
                loadCurrentPath()
                true
            } else {
                false
            }
        }
    }
    
    fun goToRoot() {
        pathHistory.clear()
        _currentPath.value = Environment.getExternalStorageDirectory().absolutePath
        loadCurrentPath()
    }
    
    fun sortBy(sortType: SortType) {
        currentSortType = sortType
        _files.value = sortFiles(_files.value)
    }
}

// Data classes
data class FileItem(
    val file: File,
    val name: String,
    val isDirectory: Boolean,
    val isVideo: Boolean,
    val size: Long,
    val lastModified: Long,
    val itemCount: Int
)

data class QuickPath(
    val name: String,
    val path: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

enum class SortType {
    NAME, DATE, SIZE
}

// Helper functions
private fun formatFileSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
        else -> "%.2f GB".format(bytes / (1024.0 * 1024.0 * 1024.0))
    }
}

private fun formatDate(timestamp: Long): String {
    val calendar = Calendar.getInstance()
    calendar.timeInMillis = timestamp
    
    val today = Calendar.getInstance()
    val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
    
    return when {
        isSameDay(calendar, today) -> "Today"
        isSameDay(calendar, yesterday) -> "Yesterday"
        else -> SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(calendar.time)
    }
}

private fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
           cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
}