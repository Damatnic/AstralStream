package com.astralplayer.nextplayer

import android.app.Application
import android.content.ContentResolver
import android.content.ContentUris
import android.provider.MediaStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PlaylistDetailViewModel(
    application: Application,
    private val playlistId: Long
) : AndroidViewModel(application) {
    
    private val playlistRepository = PlaylistRepository(application)
    private val contentResolver: ContentResolver = application.contentResolver
    
    private val _playlist = MutableStateFlow<VideoPlaylist?>(null)
    val playlist: StateFlow<VideoPlaylist?> = _playlist
    
    private val _videos = MutableStateFlow<List<VideoMetadata>>(emptyList())
    val videos: StateFlow<List<VideoMetadata>> = _videos
    
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading
    
    private var removedVideo: Pair<VideoMetadata, Int>? = null
    private var originalVideoOrder: List<Long>? = null
    
    init {
        loadPlaylist()
    }
    
    private fun loadPlaylist() {
        viewModelScope.launch {
            _isLoading.value = true
            
            val loadedPlaylist = playlistRepository.getPlaylist(playlistId)
            _playlist.value = loadedPlaylist
            
            loadedPlaylist?.let {
                loadVideos(it.videoIds)
            }
            
            _isLoading.value = false
        }
    }
    
    private suspend fun loadVideos(videoIds: List<Long>) = withContext(Dispatchers.IO) {
        val videoMap = mutableMapOf<Long, VideoMetadata>()
        
        // Query all videos from MediaStore
        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.TITLE,
            MediaStore.Video.Media.DATA,
            MediaStore.Video.Media.MIME_TYPE,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.WIDTH,
            MediaStore.Video.Media.HEIGHT,
            MediaStore.Video.Media.DATE_ADDED,
            MediaStore.Video.Media.DATE_MODIFIED
        )
        
        val selection = "${MediaStore.Video.Media._ID} IN (${videoIds.joinToString(",") { "?" }})"
        val selectionArgs = videoIds.map { it.toString() }.toTypedArray()
        
        contentResolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            null
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.TITLE)
            val pathColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA)
            val mimeTypeColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.MIME_TYPE)
            val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
            val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
            val widthColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.WIDTH)
            val heightColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.HEIGHT)
            val dateAddedColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)
            val dateModifiedColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_MODIFIED)
            
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val video = VideoMetadata(
                    id = id,
                    title = cursor.getString(titleColumn) ?: "Unknown",
                    path = cursor.getString(pathColumn) ?: "",
                    mimeType = cursor.getString(mimeTypeColumn) ?: "",
                    size = cursor.getLong(sizeColumn),
                    duration = cursor.getLong(durationColumn),
                    width = cursor.getInt(widthColumn),
                    height = cursor.getInt(heightColumn),
                    dateAdded = cursor.getLong(dateAddedColumn) * 1000,
                    dateModified = cursor.getLong(dateModifiedColumn) * 1000,
                    thumbnailPath = getThumbnailPath(id)
                )
                videoMap[id] = video
            }
        }
        
        // Maintain the order from playlist
        _videos.value = videoIds.mapNotNull { videoMap[it] }
    }
    
    private fun getThumbnailPath(videoId: Long): String? {
        return try {
            val uri = ContentUris.withAppendedId(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                videoId
            )
            uri.toString()
        } catch (e: Exception) {
            null
        }
    }
    
    fun updatePlaylist(name: String, description: String) {
        viewModelScope.launch {
            _playlist.value?.let { currentPlaylist ->
                val updatedPlaylist = currentPlaylist.copy(
                    name = name,
                    description = description
                )
                playlistRepository.updatePlaylist(updatedPlaylist)
                _playlist.value = updatedPlaylist
            }
        }
    }
    
    fun removeVideo(video: VideoMetadata) {
        viewModelScope.launch {
            val currentVideos = _videos.value
            val index = currentVideos.indexOf(video)
            if (index != -1) {
                removedVideo = video to index
                
                // Update UI immediately
                _videos.value = currentVideos.filter { it.id != video.id }
                
                // Update repository
                playlistRepository.removeVideoFromPlaylist(playlistId, video.id)
                
                // Update playlist object
                _playlist.value = _playlist.value?.copy(
                    videoIds = _playlist.value?.videoIds?.filter { it != video.id } ?: emptyList()
                )
            }
        }
    }
    
    fun undoRemove() {
        removedVideo?.let { (video, index) ->
            viewModelScope.launch {
                // Add video back to playlist
                playlistRepository.addVideoToPlaylist(playlistId, video.id)
                
                // Update UI
                val currentVideos = _videos.value.toMutableList()
                currentVideos.add(index.coerceAtMost(currentVideos.size), video)
                _videos.value = currentVideos
                
                // Update playlist object
                _playlist.value = _playlist.value?.copy(
                    videoIds = _videos.value.map { it.id }
                )
                
                removedVideo = null
            }
        }
    }
    
    fun clearPlaylist() {
        viewModelScope.launch {
            _playlist.value?.let { currentPlaylist ->
                val clearedPlaylist = currentPlaylist.copy(videoIds = emptyList())
                playlistRepository.updatePlaylist(clearedPlaylist)
                _playlist.value = clearedPlaylist
                _videos.value = emptyList()
            }
        }
    }
    
    fun reorderVideos(from: Int, to: Int) {
        if (originalVideoOrder == null) {
            originalVideoOrder = _videos.value.map { it.id }
        }
        
        val currentVideos = _videos.value.toMutableList()
        val movedVideo = currentVideos.removeAt(from)
        currentVideos.add(to, movedVideo)
        _videos.value = currentVideos
    }
    
    fun saveReorderedVideos() {
        viewModelScope.launch {
            val newOrder = _videos.value.map { it.id }
            if (originalVideoOrder != newOrder) {
                playlistRepository.reorderVideosInPlaylist(playlistId, newOrder)
                _playlist.value = _playlist.value?.copy(videoIds = newOrder)
            }
            originalVideoOrder = null
        }
    }
    
    fun addVideosToPlaylist(videoIds: List<Long>) {
        viewModelScope.launch {
            // Add each video to the playlist
            videoIds.forEach { videoId ->
                playlistRepository.addVideoToPlaylist(playlistId, videoId)
            }
            
            // Reload the playlist to show new videos
            loadPlaylist()
        }
    }
}

class PlaylistDetailViewModelFactory(
    private val application: Application,
    private val playlistId: Long
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return PlaylistDetailViewModel(application, playlistId) as T
    }
}