package com.astralplayer.nextplayer

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.astralplayer.nextplayer.data.database.AstralVuDatabase
import com.astralplayer.nextplayer.data.repository.*
import com.astralplayer.nextplayer.ui.theme.AstralPlayerTheme
import com.astralplayer.nextplayer.ui.components.VideoThumbnail
import com.astralplayer.nextplayer.ui.components.LoadingState
import com.astralplayer.nextplayer.ui.components.NoSearchResultsState
import com.astralplayer.nextplayer.utils.ErrorHandler
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.File
import android.util.Log

class SearchActivity : ComponentActivity() {
    
    private lateinit var database: AstralVuDatabase
    private lateinit var recentFilesRepository: RecentFilesRepository
    private lateinit var playlistRepository: PlaylistRepository
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize database and repositories
        database = (application as AstralVuApplication).database
        recentFilesRepository = RecentFilesRepositoryImpl(database.recentFilesDao())
        playlistRepository = PlaylistRepositoryImpl(database.playlistDao())
        
        setContent {
            AstralPlayerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    SearchScreen(
                        recentFilesRepository = recentFilesRepository,
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
fun SearchScreen(
    recentFilesRepository: RecentFilesRepository,
    playlistRepository: PlaylistRepository,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }
    
    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<SearchResults>(SearchResults()) }
    var isSearching by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf(SearchTab.ALL) }
    
    // Perform search when query changes
    LaunchedEffect(searchQuery) {
        if (searchQuery.isNotBlank()) {
            isSearching = true
            val results = performSearch(
                context = context,
                query = searchQuery,
                recentFilesRepository = recentFilesRepository,
                playlistRepository = playlistRepository
            )
            searchResults = results
            isSearching = false
        } else {
            searchResults = SearchResults()
        }
    }
    
    // Focus search field on launch
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    TextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search videos, playlists...") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(
                            onSearch = { keyboardController?.hide() }
                        ),
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search"
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Clear"
                                    )
                                }
                            }
                        },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                            unfocusedIndicatorColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        },
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Search tabs
            if (searchQuery.isNotEmpty() && !isSearching) {
                SearchTabs(
                    selectedTab = selectedTab,
                    onTabSelected = { selectedTab = it },
                    videoCount = searchResults.videos.size,
                    playlistCount = searchResults.playlists.size,
                    fileCount = searchResults.localFiles.size
                )
            }
            
            // Search results
            when {
                isSearching -> {
                    LoadingState(
                        message = "Searching...",
                        modifier = Modifier.fillMaxSize()
                    )
                }
                searchQuery.isEmpty() -> {
                    SearchEmptyState()
                }
                searchResults.isEmpty() -> {
                    NoSearchResultsState(
                        query = searchQuery,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        when (selectedTab) {
                            SearchTab.ALL -> {
                                // Videos section
                                if (searchResults.videos.isNotEmpty()) {
                                    item {
                                        Text(
                                            text = "Videos",
                                            style = MaterialTheme.typography.titleMedium,
                                            modifier = Modifier.padding(vertical = 8.dp)
                                        )
                                    }
                                    items(searchResults.videos.take(5)) { video ->
                                        VideoSearchItem(
                                            video = video,
                                            onClick = {
                                                val intent = Intent(context, VideoPlayerActivity::class.java).apply {
                                                    data = Uri.parse(video.uri)
                                                    putExtra("video_title", video.title)
                                                }
                                                context.startActivity(intent)
                                            }
                                        )
                                    }
                                    if (searchResults.videos.size > 5) {
                                        item {
                                            TextButton(
                                                onClick = { selectedTab = SearchTab.VIDEOS },
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Text("Show all ${searchResults.videos.size} videos")
                                            }
                                        }
                                    }
                                }
                                
                                // Playlists section
                                if (searchResults.playlists.isNotEmpty()) {
                                    item {
                                        Text(
                                            text = "Playlists",
                                            style = MaterialTheme.typography.titleMedium,
                                            modifier = Modifier.padding(vertical = 8.dp)
                                        )
                                    }
                                    items(searchResults.playlists.take(3)) { playlist ->
                                        PlaylistSearchItem(
                                            playlist = playlist,
                                            onClick = {
                                                val intent = Intent(context, PlaylistDetailActivity::class.java).apply {
                                                    putExtra("playlist_id", playlist.id)
                                                    putExtra("playlist_name", playlist.name)
                                                }
                                                context.startActivity(intent)
                                            }
                                        )
                                    }
                                    if (searchResults.playlists.size > 3) {
                                        item {
                                            TextButton(
                                                onClick = { selectedTab = SearchTab.PLAYLISTS },
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Text("Show all ${searchResults.playlists.size} playlists")
                                            }
                                        }
                                    }
                                }
                                
                                // Files section
                                if (searchResults.localFiles.isNotEmpty()) {
                                    item {
                                        Text(
                                            text = "Files",
                                            style = MaterialTheme.typography.titleMedium,
                                            modifier = Modifier.padding(vertical = 8.dp)
                                        )
                                    }
                                    items(searchResults.localFiles.take(5)) { file ->
                                        FileSearchItem(
                                            file = file,
                                            onClick = {
                                                val intent = Intent(context, VideoPlayerActivity::class.java).apply {
                                                    data = Uri.fromFile(File(file.path))
                                                    putExtra("video_title", file.name)
                                                }
                                                context.startActivity(intent)
                                            }
                                        )
                                    }
                                    if (searchResults.localFiles.size > 5) {
                                        item {
                                            TextButton(
                                                onClick = { selectedTab = SearchTab.FILES },
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Text("Show all ${searchResults.localFiles.size} files")
                                            }
                                        }
                                    }
                                }
                            }
                            SearchTab.VIDEOS -> {
                                items(searchResults.videos) { video ->
                                    VideoSearchItem(
                                        video = video,
                                        onClick = {
                                            val intent = Intent(context, VideoPlayerActivity::class.java).apply {
                                                data = Uri.parse(video.uri)
                                                putExtra("video_title", video.title)
                                            }
                                            context.startActivity(intent)
                                        }
                                    )
                                }
                            }
                            SearchTab.PLAYLISTS -> {
                                items(searchResults.playlists) { playlist ->
                                    PlaylistSearchItem(
                                        playlist = playlist,
                                        onClick = {
                                            val intent = Intent(context, PlaylistDetailActivity::class.java).apply {
                                                putExtra("playlist_id", playlist.id)
                                                putExtra("playlist_name", playlist.name)
                                            }
                                            context.startActivity(intent)
                                        }
                                    )
                                }
                            }
                            SearchTab.FILES -> {
                                items(searchResults.localFiles) { file ->
                                    FileSearchItem(
                                        file = file,
                                        onClick = {
                                            val intent = Intent(context, VideoPlayerActivity::class.java).apply {
                                                data = Uri.fromFile(File(file.path))
                                                putExtra("video_title", file.name)
                                            }
                                            context.startActivity(intent)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SearchTabs(
    selectedTab: SearchTab,
    onTabSelected: (SearchTab) -> Unit,
    videoCount: Int,
    playlistCount: Int,
    fileCount: Int,
    modifier: Modifier = Modifier
) {
    ScrollableTabRow(
        selectedTabIndex = selectedTab.ordinal,
        modifier = modifier
    ) {
        Tab(
            selected = selectedTab == SearchTab.ALL,
            onClick = { onTabSelected(SearchTab.ALL) },
            text = { Text("All") }
        )
        Tab(
            selected = selectedTab == SearchTab.VIDEOS,
            onClick = { onTabSelected(SearchTab.VIDEOS) },
            text = { Text("Videos ($videoCount)") }
        )
        Tab(
            selected = selectedTab == SearchTab.PLAYLISTS,
            onClick = { onTabSelected(SearchTab.PLAYLISTS) },
            text = { Text("Playlists ($playlistCount)") }
        )
        Tab(
            selected = selectedTab == SearchTab.FILES,
            onClick = { onTabSelected(SearchTab.FILES) },
            text = { Text("Files ($fileCount)") }
        )
    }
}

@Composable
fun VideoSearchItem(
    video: com.astralplayer.nextplayer.data.RecentFile,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            VideoThumbnail(
                videoUri = Uri.parse(video.uri),
                duration = video.duration,
                modifier = Modifier.size(width = 100.dp, height = 56.dp),
                showDuration = true
            )
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = video.title,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = formatDuration(video.duration),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun PlaylistSearchItem(
    playlist: Playlist,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
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
            Icon(
                imageVector = Icons.AutoMirrored.Filled.PlaylistPlay,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = playlist.name,
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "${playlist.videoCount} videos",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun FileSearchItem(
    file: LocalVideoFile,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.VideoFile,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.secondary
            )
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = file.name,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = file.path,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun SearchEmptyState(
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
                imageVector = Icons.Default.Search,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
            Text(
                text = "Search for videos",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Find videos in your library or playlists",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
fun NoResultsState(
    query: String,
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
                imageVector = Icons.Default.SearchOff,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
            Text(
                text = "No results found",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "No videos or playlists match \"$query\"",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}

data class SearchResults(
    val videos: List<com.astralplayer.nextplayer.data.RecentFile> = emptyList(),
    val playlists: List<Playlist> = emptyList(),
    val localFiles: List<LocalVideoFile> = emptyList()
) {
    fun isEmpty(): Boolean = videos.isEmpty() && playlists.isEmpty() && localFiles.isEmpty()
}

data class LocalVideoFile(
    val path: String,
    val name: String,
    val size: Long = 0L
)

data class Playlist(
    val id: String,
    val name: String,
    val videoCount: Int = 0
)

enum class SearchTab {
    ALL, VIDEOS, PLAYLISTS, FILES
}

suspend fun performSearch(
    context: android.content.Context,
    query: String,
    recentFilesRepository: RecentFilesRepository,
    playlistRepository: PlaylistRepository
): SearchResults {
    val lowerQuery = query.lowercase()
    
    // Search recent files
    val recentVideos = mutableListOf<com.astralplayer.nextplayer.data.RecentFile>()
    recentFilesRepository.getAllRecentFiles().collect { files ->
        recentVideos.clear()
        recentVideos.addAll(files.filter { file ->
            file.title.lowercase().contains(lowerQuery)
        })
    }
    
    // Search playlists
    val playlists = mutableListOf<Playlist>()
    playlistRepository.getAllPlaylists().collect { dbPlaylists ->
        playlists.clear()
        for (dbPlaylist in dbPlaylists) {
            if (dbPlaylist.name.lowercase().contains(lowerQuery)) {
                val videoCount = playlistRepository.getPlaylistVideoCount(dbPlaylist.id)
                playlists.add(
                    Playlist(
                        id = dbPlaylist.id,
                        name = dbPlaylist.name,
                        videoCount = videoCount
                    )
                )
            }
        }
    }
    
    // Search local video files
    val localFiles = searchLocalVideoFiles(context, lowerQuery)
    
    return SearchResults(
        videos = recentVideos,
        playlists = playlists,
        localFiles = localFiles
    )
}

fun searchLocalVideoFiles(context: android.content.Context, query: String): List<LocalVideoFile> {
    val files = mutableListOf<LocalVideoFile>()
    
    val projection = arrayOf(
        MediaStore.Video.Media._ID,
        MediaStore.Video.Media.DISPLAY_NAME,
        MediaStore.Video.Media.DATA,
        MediaStore.Video.Media.SIZE
    )
    
    val selection = "${MediaStore.Video.Media.DISPLAY_NAME} LIKE ?"
    val selectionArgs = arrayOf("%$query%")
    
    try {
        context.contentResolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            "${MediaStore.Video.Media.DATE_ADDED} DESC"
        )?.use { cursor ->
            val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
            val pathColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA)
            val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
            
            while (cursor.moveToNext()) {
                val name = cursor.getString(nameColumn)
                val path = cursor.getString(pathColumn)
                val size = cursor.getLong(sizeColumn)
                
                files.add(LocalVideoFile(path, name, size))
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    
    return files
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