package com.astralplayer.nextplayer.feature.bookmarks

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.util.*

// DataStore extension
private val Context.bookmarksDataStore: DataStore<Preferences> by preferencesDataStore(name = "bookmarks")

/**
 * Data class representing a video bookmark
 */
@Serializable
data class VideoBookmark(
    val id: String,
    val videoUri: String,
    val videoTitle: String,
    val position: Long, // in milliseconds
    val duration: Long,
    val thumbnailPath: String? = null,
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val tags: List<String> = emptyList()
) {
    fun formatPosition(): String {
        val totalSeconds = position / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        
        return if (hours > 0) {
            String.format("%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%d:%02d", minutes, seconds)
        }
    }
    
    fun formatCreatedDate(): String {
        val formatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        return formatter.format(Date(createdAt))
    }
    
    fun getProgressPercentage(): Float {
        return if (duration > 0) (position.toFloat() / duration.toFloat()) * 100f else 0f
    }
}

/**
 * Data class representing a favorite video
 */
@Serializable
data class FavoriteVideo(
    val id: String,
    val videoUri: String,
    val videoTitle: String,
    val duration: Long,
    val thumbnailPath: String? = null,
    val addedAt: Long = System.currentTimeMillis(),
    val lastWatched: Long = 0L,
    val watchCount: Int = 0,
    val rating: Float = 0f, // 0-5 stars
    val tags: List<String> = emptyList(),
    val notes: String = ""
) {
    fun formatDuration(): String {
        val totalSeconds = duration / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        
        return if (hours > 0) {
            String.format("%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%d:%02d", minutes, seconds)
        }
    }
    
    fun formatAddedDate(): String {
        val formatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        return formatter.format(Date(addedAt))
    }
    
    fun formatLastWatched(): String {
        if (lastWatched == 0L) return "Never"
        val formatter = SimpleDateFormat("MMM dd", Locale.getDefault())
        return formatter.format(Date(lastWatched))
    }
}

/**
 * Manager for video bookmarks and favorites
 */
class VideoBookmarkManager(private val context: Context) {
    
    private val json = Json { ignoreUnknownKeys = true }
    
    // DataStore keys
    private object Keys {
        val BOOKMARKS = stringPreferencesKey("bookmarks")
        val FAVORITES = stringPreferencesKey("favorites")
    }
    
    // Bookmarks flow
    val bookmarks: Flow<List<VideoBookmark>> = context.bookmarksDataStore.data
        .map { preferences ->
            val bookmarksJson = preferences[Keys.BOOKMARKS] ?: "[]"
            try {
                json.decodeFromString<List<VideoBookmark>>(bookmarksJson)
            } catch (e: Exception) {
                emptyList()
            }
        }
    
    // Favorites flow
    val favorites: Flow<List<FavoriteVideo>> = context.bookmarksDataStore.data
        .map { preferences ->
            val favoritesJson = preferences[Keys.FAVORITES] ?: "[]"
            try {
                json.decodeFromString<List<FavoriteVideo>>(favoritesJson)
            } catch (e: Exception) {
                emptyList()
            }
        }
    
    /**
     * Add a bookmark
     */
    suspend fun addBookmark(
        videoUri: Uri,
        videoTitle: String,
        position: Long,
        duration: Long,
        note: String = "",
        tags: List<String> = emptyList()
    ) {
        context.bookmarksDataStore.edit { preferences ->
            val currentBookmarks = bookmarks.first()
            val newBookmark = VideoBookmark(
                id = "bookmark_${System.currentTimeMillis()}",
                videoUri = videoUri.toString(),
                videoTitle = videoTitle,
                position = position,
                duration = duration,
                note = note,
                tags = tags
            )
            
            val updatedBookmarks = currentBookmarks + newBookmark
            preferences[Keys.BOOKMARKS] = json.encodeToString(updatedBookmarks)
        }
    }
    
    /**
     * Remove a bookmark
     */
    suspend fun removeBookmark(bookmarkId: String) {
        context.bookmarksDataStore.edit { preferences ->
            val currentBookmarks = bookmarks.first()
            val updatedBookmarks = currentBookmarks.filter { it.id != bookmarkId }
            preferences[Keys.BOOKMARKS] = json.encodeToString(updatedBookmarks)
        }
    }
    
    /**
     * Update bookmark note
     */
    suspend fun updateBookmarkNote(bookmarkId: String, note: String) {
        context.bookmarksDataStore.edit { preferences ->
            val currentBookmarks = bookmarks.first()
            val updatedBookmarks = currentBookmarks.map { bookmark ->
                if (bookmark.id == bookmarkId) {
                    bookmark.copy(note = note)
                } else {
                    bookmark
                }
            }
            preferences[Keys.BOOKMARKS] = json.encodeToString(updatedBookmarks)
        }
    }
    
    /**
     * Add video to favorites
     */
    suspend fun addToFavorites(
        videoUri: Uri,
        videoTitle: String,
        duration: Long,
        rating: Float = 0f,
        tags: List<String> = emptyList(),
        notes: String = ""
    ) {
        context.bookmarksDataStore.edit { preferences ->
            val currentFavorites = favorites.first()
            
            // Check if already exists
            val existingFavorite = currentFavorites.find { it.videoUri == videoUri.toString() }
            
            val updatedFavorites = if (existingFavorite != null) {
                // Update existing favorite
                currentFavorites.map { favorite ->
                    if (favorite.id == existingFavorite.id) {
                        favorite.copy(
                            rating = rating,
                            tags = tags,
                            notes = notes,
                            lastWatched = System.currentTimeMillis(),
                            watchCount = favorite.watchCount + 1
                        )
                    } else {
                        favorite
                    }
                }
            } else {
                // Add new favorite
                val newFavorite = FavoriteVideo(
                    id = "favorite_${System.currentTimeMillis()}",
                    videoUri = videoUri.toString(),
                    videoTitle = videoTitle,
                    duration = duration,
                    rating = rating,
                    tags = tags,
                    notes = notes,
                    watchCount = 1,
                    lastWatched = System.currentTimeMillis()
                )
                currentFavorites + newFavorite
            }
            
            preferences[Keys.FAVORITES] = json.encodeToString(updatedFavorites)
        }
    }
    
    /**
     * Remove from favorites
     */
    suspend fun removeFromFavorites(favoriteId: String) {
        context.bookmarksDataStore.edit { preferences ->
            val currentFavorites = favorites.first()
            val updatedFavorites = currentFavorites.filter { it.id != favoriteId }
            preferences[Keys.FAVORITES] = json.encodeToString(updatedFavorites)
        }
    }
    
    /**
     * Check if video is in favorites
     */
    suspend fun isFavorite(videoUri: Uri): Boolean {
        return favorites.first().any { it.videoUri == videoUri.toString() }
    }
    
    /**
     * Get bookmarks for specific video
     */
    suspend fun getBookmarksForVideo(videoUri: Uri): List<VideoBookmark> {
        return bookmarks.first().filter { it.videoUri == videoUri.toString() }
    }
    
    /**
     * Clear all bookmarks
     */
    suspend fun clearAllBookmarks() {
        context.bookmarksDataStore.edit { preferences ->
            preferences[Keys.BOOKMARKS] = "[]"
        }
    }
    
    /**
     * Clear all favorites
     */
    suspend fun clearAllFavorites() {
        context.bookmarksDataStore.edit { preferences ->
            preferences[Keys.FAVORITES] = "[]"
        }
    }
}

/**
 * ViewModel for bookmarks and favorites
 */
class BookmarkViewModel(private val bookmarkManager: VideoBookmarkManager) : ViewModel() {
    
    val bookmarks = bookmarkManager.bookmarks.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )
    
    val favorites = bookmarkManager.favorites.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )
    
    fun addBookmark(
        videoUri: Uri,
        videoTitle: String,
        position: Long,
        duration: Long,
        note: String = "",
        tags: List<String> = emptyList()
    ) {
        viewModelScope.launch {
            bookmarkManager.addBookmark(videoUri, videoTitle, position, duration, note, tags)
        }
    }
    
    fun removeBookmark(bookmarkId: String) {
        viewModelScope.launch {
            bookmarkManager.removeBookmark(bookmarkId)
        }
    }
    
    fun addToFavorites(
        videoUri: Uri,
        videoTitle: String,
        duration: Long,
        rating: Float = 0f,
        tags: List<String> = emptyList(),
        notes: String = ""
    ) {
        viewModelScope.launch {
            bookmarkManager.addToFavorites(videoUri, videoTitle, duration, rating, tags, notes)
        }
    }
    
    fun removeFromFavorites(favoriteId: String) {
        viewModelScope.launch {
            bookmarkManager.removeFromFavorites(favoriteId)
        }
    }
}

/**
 * Composable for bookmarks overlay
 */
@Composable
fun BookmarksOverlay(
    bookmarks: List<VideoBookmark>,
    onBookmarkClick: (VideoBookmark) -> Unit,
    onAddBookmark: () -> Unit,
    onRemoveBookmark: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showBookmarksList by remember { mutableStateOf(false) }
    
    Column(modifier = modifier) {
        // Bookmarks quick access
        if (bookmarks.isNotEmpty()) {
            BookmarksQuickAccess(
                bookmarks = bookmarks.take(5), // Show only first 5
                onBookmarkClick = onBookmarkClick,
                onShowAll = { showBookmarksList = true }
            )
        }
        
        // Add bookmark button
        FloatingActionButton(
            onClick = onAddBookmark,
            modifier = Modifier
                .size(48.dp)
                .align(Alignment.End),
            containerColor = Color(0xFF00BCD4)
        ) {
            Icon(
                Icons.Default.BookmarkAdd,
                contentDescription = "Add Bookmark",
                tint = Color.White
            )
        }
        
        // Full bookmarks list
        AnimatedVisibility(
            visible = showBookmarksList,
            enter = slideInVertically() + fadeIn(),
            exit = slideOutVertically() + fadeOut()
        ) {
            BookmarksList(
                bookmarks = bookmarks,
                onBookmarkClick = { bookmark ->
                    onBookmarkClick(bookmark)
                    showBookmarksList = false
                },
                onRemoveBookmark = onRemoveBookmark,
                onDismiss = { showBookmarksList = false }
            )
        }
    }
}

/**
 * Quick access bookmarks
 */
@Composable
private fun BookmarksQuickAccess(
    bookmarks: List<VideoBookmark>,
    onBookmarkClick: (VideoBookmark) -> Unit,
    onShowAll: () -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) {
        items(bookmarks) { bookmark ->
            BookmarkQuickItem(
                bookmark = bookmark,
                onClick = { onBookmarkClick(bookmark) }
            )
        }
        
        if (bookmarks.size > 5) {
            item {
                Card(
                    modifier = Modifier
                        .size(60.dp, 34.dp)
                        .clickable { onShowAll() },
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White.copy(alpha = 0.1f)
                    ),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "+${bookmarks.size - 5}",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

/**
 * Quick bookmark item
 */
@Composable
private fun BookmarkQuickItem(
    bookmark: VideoBookmark,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Card(
            modifier = Modifier.size(60.dp, 34.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF00BCD4).copy(alpha = 0.2f)
            ),
            shape = RoundedCornerShape(4.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Bookmark,
                    contentDescription = "Bookmark",
                    tint = Color(0xFF00BCD4),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Text(
            text = bookmark.formatPosition(),
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 10.sp
        )
    }
}

/**
 * Full bookmarks list
 */
@Composable
private fun BookmarksList(
    bookmarks: List<VideoBookmark>,
    onBookmarkClick: (VideoBookmark) -> Unit,
    onRemoveBookmark: (String) -> Unit,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color.Black.copy(alpha = 0.9f)
        ),
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 400.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Bookmarks (${bookmarks.size})",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                
                IconButton(onClick = onDismiss) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color.White
                    )
                }
            }
            
            // Bookmarks list
            LazyColumn {
                items(bookmarks) { bookmark ->
                    BookmarkListItem(
                        bookmark = bookmark,
                        onClick = { onBookmarkClick(bookmark) },
                        onRemove = { onRemoveBookmark(bookmark.id) }
                    )
                }
            }
        }
    }
}

/**
 * Bookmark list item
 */
@Composable
private fun BookmarkListItem(
    bookmark: VideoBookmark,
    onClick: () -> Unit,
    onRemove: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Bookmark icon
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(
                    Color(0xFF00BCD4).copy(alpha = 0.2f),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Bookmark,
                contentDescription = "Bookmark",
                tint = Color(0xFF00BCD4),
                modifier = Modifier.size(20.dp)
            )
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = bookmark.formatPosition(),
                color = Color(0xFF00BCD4),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            
            if (bookmark.note.isNotEmpty()) {
                Text(
                    text = bookmark.note,
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            Text(
                text = bookmark.formatCreatedDate(),
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 10.sp
            )
        }
        
        // Progress indicator
        Box(
            modifier = Modifier
                .width(60.dp)
                .height(4.dp)
                .background(
                    Color.White.copy(alpha = 0.2f),
                    RoundedCornerShape(2.dp)
                )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(bookmark.getProgressPercentage() / 100f)
                    .background(
                        Color(0xFF00BCD4),
                        RoundedCornerShape(2.dp)
                    )
            )
        }
        
        Spacer(modifier = Modifier.width(8.dp))
        
        // Remove button
        IconButton(
            onClick = onRemove,
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                Icons.Default.Delete,
                contentDescription = "Remove",
                tint = Color.Red.copy(alpha = 0.7f),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

/**
 * Favorites screen composable
 */
@Composable
fun FavoritesScreen(
    favorites: List<FavoriteVideo>,
    onVideoClick: (FavoriteVideo) -> Unit,
    onRemoveFromFavorites: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Text(
                text = "Favorites (${favorites.size})",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }
        
        items(favorites) { favorite ->
            FavoriteVideoItem(
                favorite = favorite,
                onClick = { onVideoClick(favorite) },
                onRemove = { onRemoveFromFavorites(favorite.id) }
            )
        }
    }
}

/**
 * Favorite video item
 */
@Composable
private fun FavoriteVideoItem(
    favorite: FavoriteVideo,
    onClick: () -> Unit,
    onRemove: () -> Unit
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
            // Thumbnail placeholder
            Box(
                modifier = Modifier
                    .size(80.dp, 45.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.White.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.VideoFile,
                    contentDescription = "Video",
                    tint = Color.White.copy(alpha = 0.5f),
                    modifier = Modifier.size(24.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = favorite.videoTitle,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = favorite.formatDuration(),
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 12.sp
                    )
                    
                    if (favorite.rating > 0) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Star,
                                contentDescription = "Rating",
                                tint = Color(0xFFFFC107),
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = String.format("%.1f", favorite.rating),
                                color = Color(0xFFFFC107),
                                fontSize = 10.sp
                            )
                        }
                    }
                }
                
                Text(
                    text = "Added ${favorite.formatAddedDate()} • Watched ${favorite.watchCount} times",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 10.sp
                )
            }
            
            // Favorite icon
            Icon(
                Icons.Default.Favorite,
                contentDescription = "Favorite",
                tint = Color.Red,
                modifier = Modifier.size(20.dp)
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            
            // Remove button
            IconButton(
                onClick = onRemove,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Remove",
                    tint = Color.Red.copy(alpha = 0.7f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}