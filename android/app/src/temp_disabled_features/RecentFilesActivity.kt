package com.astralplayer.nextplayer

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.SortByAlpha
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.astralplayer.nextplayer.data.RecentFile
import com.astralplayer.nextplayer.data.RecentFilesRepository
import com.astralplayer.nextplayer.ui.theme.AstralTheme
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class RecentFilesActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            AstralTheme {
                val viewModel: RecentFilesViewModel = viewModel(
                    factory = RecentFilesViewModelFactory(application)
                )
                
                RecentFilesScreen(
                    viewModel = viewModel,
                    onBackClick = { finish() },
                    onFileClick = { recentFile ->
                        val intent = Intent(this, VideoPlayerActivity::class.java).apply {
                            data = Uri.parse(recentFile.uri)
                            putExtra("video_title", recentFile.title)
                        }
                        startActivity(intent)
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecentFilesScreen(
    viewModel: RecentFilesViewModel,
    onBackClick: () -> Unit,
    onFileClick: (RecentFile) -> Unit
) {
    val recentFiles by viewModel.recentFiles.collectAsState()
    val groupedFiles by viewModel.groupedFiles.collectAsState()
    var showSearchBar by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Recent Files",
                        fontWeight = FontWeight.Bold
                    )
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
                    IconButton(onClick = { showSearchBar = !showSearchBar }) {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = "Search"
                        )
                    }
                    
                    var showMenu by remember { mutableStateOf(false) }
                    IconButton(onClick = { showMenu = true }) {
                        Icon(
                            imageVector = Icons.Filled.MoreVert,
                            contentDescription = "More options"
                        )
                    }
                    
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Clear All") },
                            onClick = {
                                viewModel.clearAllHistory()
                                showMenu = false
                            },
                            leadingIcon = {
                                Icon(Icons.Filled.Delete, contentDescription = null)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Sort by Date") },
                            onClick = {
                                viewModel.sortByDate()
                                showMenu = false
                            },
                            leadingIcon = {
                                Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = null)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Sort by Name") },
                            onClick = {
                                viewModel.sortByName()
                                showMenu = false
                            },
                            leadingIcon = {
                                Icon(Icons.Filled.SortByAlpha, contentDescription = null)
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Search bar
            AnimatedVisibility(visible = showSearchBar) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { 
                        searchQuery = it
                        viewModel.searchFiles(it)
                    },
                    placeholder = { Text("Search recent files...") },
                    leadingIcon = {
                        Icon(Icons.Filled.Search, contentDescription = null)
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { 
                                searchQuery = ""
                                viewModel.searchFiles("")
                            }) {
                                Icon(Icons.Filled.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF00D4FF),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.3f)
                    ),
                    singleLine = true
                )
            }
            
            if (recentFiles.isEmpty()) {
                EmptyStateView()
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    groupedFiles.forEach { (date, files) ->
                        item {
                            Text(
                                text = date,
                                color = Color(0xFF00D4FF),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                        
                        items(files) { recentFile ->
                            RecentFileItem(
                                recentFile = recentFile,
                                onClick = { onFileClick(recentFile) },
                                onDelete = { viewModel.removeFile(recentFile) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecentFileItem(
    recentFile: RecentFile,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    
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
            // Thumbnail placeholder
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF1E88E5),
                                Color(0xFF00BCD4)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.PlayCircle,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = recentFile.title,
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
                    Text(
                        text = formatDuration(recentFile.duration),
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 12.sp
                    )
                    
                    if (recentFile.lastPosition > 0) {
                        Text(
                            text = " • ",
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 12.sp
                        )
                        
                        Text(
                            text = "${((recentFile.lastPosition.toFloat() / recentFile.duration) * 100).toInt()}% watched",
                            color = Color(0xFF00D4FF),
                            fontSize = 12.sp
                        )
                    }
                }
                
                // Progress bar
                if (recentFile.lastPosition > 0 && recentFile.duration > 0) {
                    LinearProgressIndicator(
                        progress = { recentFile.lastPosition.toFloat() / recentFile.duration },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                            .height(2.dp),
                        color = Color(0xFF00D4FF),
                        trackColor = Color.White.copy(alpha = 0.1f)
                    )
                }
            }
            
            IconButton(onClick = { showDeleteDialog = true }) {
                Icon(
                    imageVector = Icons.Filled.MoreVert,
                    contentDescription = "More options",
                    tint = Color.White.copy(alpha = 0.7f)
                )
            }
        }
    }
    
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Remove from history?") },
            text = { Text("This will remove \"${recentFile.title}\" from your recent files.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showDeleteDialog = false
                    }
                ) {
                    Text("Remove", color = Color(0xFFFF4444))
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
fun EmptyStateView() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.History,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.3f),
                modifier = Modifier.size(64.dp)
            )
            
            Text(
                text = "No recent files",
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 18.sp
            )
            
            Text(
                text = "Videos you play will appear here",
                color = Color.White.copy(alpha = 0.3f),
                fontSize = 14.sp
            )
        }
    }
}

// ViewModel
class RecentFilesViewModel(
    private val repository: RecentFilesRepository
) : ViewModel() {
    
    private val _recentFiles = MutableStateFlow<List<RecentFile>>(emptyList())
    val recentFiles: StateFlow<List<RecentFile>> = _recentFiles.asStateFlow()
    
    private val _groupedFiles = MutableStateFlow<Map<String, List<RecentFile>>>(emptyMap())
    val groupedFiles: StateFlow<Map<String, List<RecentFile>>> = _groupedFiles.asStateFlow()
    
    init {
        loadRecentFiles()
    }
    
    private fun loadRecentFiles() {
        viewModelScope.launch {
            repository.getRecentFiles().collect { files ->
                _recentFiles.value = files
                groupFilesByDate(files)
            }
        }
    }
    
    private fun groupFilesByDate(files: List<RecentFile>) {
        val grouped = files.groupBy { file ->
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = file.lastPlayed
            
            val today = Calendar.getInstance()
            val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
            
            when {
                isSameDay(calendar, today) -> "Today"
                isSameDay(calendar, yesterday) -> "Yesterday"
                else -> SimpleDateFormat("MMMM d, yyyy", Locale.getDefault()).format(calendar.time)
            }
        }
        
        _groupedFiles.value = grouped
    }
    
    private fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
               cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }
    
    fun removeFile(recentFile: RecentFile) {
        viewModelScope.launch {
            repository.removeRecentFile(recentFile.id)
        }
    }
    
    fun clearAllHistory() {
        viewModelScope.launch {
            repository.clearAllHistory()
        }
    }
    
    fun searchFiles(query: String) {
        viewModelScope.launch {
            if (query.isEmpty()) {
                loadRecentFiles()
            } else {
                val filtered = _recentFiles.value.filter { file ->
                    file.title.contains(query, ignoreCase = true)
                }
                groupFilesByDate(filtered)
            }
        }
    }
    
    fun sortByDate() {
        val sorted = _recentFiles.value.sortedByDescending { it.lastPlayed }
        _recentFiles.value = sorted
        groupFilesByDate(sorted)
    }
    
    fun sortByName() {
        val sorted = _recentFiles.value.sortedBy { it.title }
        _recentFiles.value = sorted
        groupFilesByDate(sorted)
    }
}

class RecentFilesViewModelFactory(
    private val application: android.app.Application
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RecentFilesViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return RecentFilesViewModel(
                RecentFilesRepository(application)
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

// Helper functions
private fun formatDuration(milliseconds: Long): String {
    val totalSeconds = milliseconds / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    
    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%d:%02d", minutes, seconds)
    }
}