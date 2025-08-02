package com.astralstream.nextplayer.community

import android.content.Context
import android.net.Uri
import com.astralstream.nextplayer.database.AppDatabase
import com.astralstream.nextplayer.database.entities.PlaylistEntity
import com.astralstream.nextplayer.database.entities.SharedPlaylistEntity
import com.astralstream.nextplayer.models.SharedPlaylist
import com.astralstream.nextplayer.models.SharedPlaylistMetadata
import com.astralstream.nextplayer.network.CommunityApiService
import com.astralstream.nextplayer.security.EncryptionManager
import com.astralstream.nextplayer.utils.NetworkUtils
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import timber.log.Timber
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlaylistSharingService @Inject constructor(
    private val context: Context,
    private val database: AppDatabase,
    private val communityApi: CommunityApiService,
    private val encryptionManager: EncryptionManager,
    private val networkUtils: NetworkUtils
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    // Sharing state
    private val _sharingState = MutableStateFlow<SharingState>(SharingState.Idle)
    val sharingState: StateFlow<SharingState> = _sharingState.asStateFlow()
    
    // Shared playlists cache
    private val _sharedPlaylists = MutableStateFlow<List<SharedPlaylist>>(emptyList())
    val sharedPlaylists: StateFlow<List<SharedPlaylist>> = _sharedPlaylists.asStateFlow()
    
    sealed class SharingState {
        object Idle : SharingState()
        data class Sharing(val progress: Float) : SharingState()
        data class Success(val shareLink: String) : SharingState()
        data class Error(val message: String) : SharingState()
    }
    
    /**
     * Share a playlist with the community
     */
    suspend fun sharePlaylist(
        playlistId: String,
        description: String = "",
        tags: List<String> = emptyList(),
        isPublic: Boolean = true
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            _sharingState.value = SharingState.Sharing(0f)
            
            // Get playlist from database
            val playlist = database.playlistDao().getPlaylistById(playlistId)
                ?: return@withContext Result.failure(Exception("Playlist not found"))
            
            // Get playlist videos
            val videos = database.playlistVideoDao().getVideosForPlaylist(playlistId)
            
            _sharingState.value = SharingState.Sharing(0.2f)
            
            // Create share metadata
            val metadata = SharedPlaylistMetadata(
                id = UUID.randomUUID().toString(),
                originalPlaylistId = playlistId,
                name = playlist.name,
                description = description.ifEmpty { playlist.description ?: "" },
                creatorId = getUserId(),
                creatorName = getUserName(),
                videoCount = videos.size,
                totalDuration = videos.sumOf { it.duration },
                tags = tags,
                isPublic = isPublic,
                createdAt = System.currentTimeMillis(),
                version = 1
            )
            
            _sharingState.value = SharingState.Sharing(0.4f)
            
            // Create shareable data
            val shareData = createShareableData(playlist, videos, metadata)
            
            // Encrypt if private
            val finalData = if (!isPublic) {
                encryptionManager.encrypt(shareData)
            } else {
                shareData
            }
            
            _sharingState.value = SharingState.Sharing(0.6f)
            
            // Upload to community server
            val shareLink = if (networkUtils.isNetworkAvailable()) {
                uploadToServer(metadata, finalData)
            } else {
                generateOfflineShareLink(metadata, finalData)
            }
            
            _sharingState.value = SharingState.Sharing(0.8f)
            
            // Save to local database
            saveSharedPlaylist(metadata, shareLink)
            
            _sharingState.value = SharingState.Success(shareLink)
            
            Timber.d("Playlist shared successfully: $shareLink")
            Result.success(shareLink)
            
        } catch (e: Exception) {
            Timber.e(e, "Failed to share playlist")
            _sharingState.value = SharingState.Error(e.message ?: "Unknown error")
            Result.failure(e)
        } finally {
            // Reset state after delay
            delay(3000)
            _sharingState.value = SharingState.Idle
        }
    }
    
    /**
     * Import a shared playlist
     */
    suspend fun importSharedPlaylist(shareLink: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            // Parse share link
            val shareData = parseShareLink(shareLink)
            
            // Download playlist data
            val playlistData = if (shareData.isOnline) {
                downloadFromServer(shareData.id)
            } else {
                decodeOfflineData(shareData.data)
            }
            
            // Decrypt if needed
            val metadata = playlistData.metadata
            val videos = if (!metadata.isPublic && playlistData.encryptedData != null) {
                val decrypted = encryptionManager.decrypt(playlistData.encryptedData)
                parseVideosFromData(decrypted)
            } else {
                playlistData.videos
            }
            
            // Create new playlist
            val newPlaylistId = UUID.randomUUID().toString()
            val newPlaylist = PlaylistEntity(
                id = newPlaylistId,
                name = "${metadata.name} (Imported)",
                description = metadata.description,
                thumbnailUri = null,
                videoCount = videos.size,
                totalDuration = metadata.totalDuration,
                createdAt = System.currentTimeMillis(),
                lastModified = System.currentTimeMillis(),
                isShared = true,
                sharedFromId = metadata.id
            )
            
            // Save to database
            database.playlistDao().insert(newPlaylist)
            
            // Add videos to playlist
            videos.forEachIndexed { index, video ->
                database.playlistVideoDao().addVideoToPlaylist(
                    playlistId = newPlaylistId,
                    videoId = video.id,
                    position = index
                )
            }
            
            // Track import
            trackPlaylistImport(metadata.id)
            
            Result.success(newPlaylistId)
            
        } catch (e: Exception) {
            Timber.e(e, "Failed to import shared playlist")
            Result.failure(e)
        }
    }
    
    /**
     * Browse community playlists
     */
    suspend fun browseCommunityPlaylists(
        query: String = "",
        tags: List<String> = emptyList(),
        sortBy: SortOption = SortOption.POPULAR,
        page: Int = 0
    ): Result<List<SharedPlaylist>> = withContext(Dispatchers.IO) {
        try {
            val response = communityApi.browsePlaylists(
                query = query,
                tags = tags,
                sortBy = sortBy.value,
                page = page,
                pageSize = 20
            )
            
            if (response.isSuccessful) {
                val playlists = response.body() ?: emptyList()
                _sharedPlaylists.value = if (page == 0) playlists else _sharedPlaylists.value + playlists
                Result.success(playlists)
            } else {
                Result.failure(Exception("Failed to load playlists: ${response.code()}"))
            }
            
        } catch (e: Exception) {
            Timber.e(e, "Failed to browse community playlists")
            Result.failure(e)
        }
    }
    
    /**
     * Like/unlike a shared playlist
     */
    suspend fun togglePlaylistLike(playlistId: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val response = communityApi.toggleLike(playlistId, getUserId())
            
            if (response.isSuccessful) {
                val liked = response.body()?.liked ?: false
                updateLocalPlaylistLike(playlistId, liked)
                Result.success(liked)
            } else {
                Result.failure(Exception("Failed to toggle like"))
            }
            
        } catch (e: Exception) {
            Timber.e(e, "Failed to toggle playlist like")
            Result.failure(e)
        }
    }
    
    /**
     * Report inappropriate playlist
     */
    suspend fun reportPlaylist(
        playlistId: String,
        reason: ReportReason,
        details: String = ""
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val report = PlaylistReport(
                playlistId = playlistId,
                reporterId = getUserId(),
                reason = reason,
                details = details,
                timestamp = System.currentTimeMillis()
            )
            
            val response = communityApi.reportPlaylist(report)
            
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to submit report"))
            }
            
        } catch (e: Exception) {
            Timber.e(e, "Failed to report playlist")
            Result.failure(e)
        }
    }
    
    /**
     * Get user's shared playlists
     */
    fun getUserSharedPlaylists(): Flow<List<SharedPlaylistEntity>> {
        return database.sharedPlaylistDao().getUserPlaylists(getUserId())
    }
    
    /**
     * Delete shared playlist
     */
    suspend fun deleteSharedPlaylist(shareId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // Delete from server
            if (networkUtils.isNetworkAvailable()) {
                val response = communityApi.deletePlaylist(shareId, getUserId())
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("Failed to delete from server"))
                }
            }
            
            // Delete from local database
            database.sharedPlaylistDao().deleteById(shareId)
            
            Result.success(Unit)
            
        } catch (e: Exception) {
            Timber.e(e, "Failed to delete shared playlist")
            Result.failure(e)
        }
    }
    
    // Private helper methods
    
    private fun createShareableData(
        playlist: PlaylistEntity,
        videos: List<VideoEntity>,
        metadata: SharedPlaylistMetadata
    ): ByteArray {
        // Create JSON representation
        val json = buildString {
            append("{")
            append("\"metadata\":${gson.toJson(metadata)},")
            append("\"videos\":[")
            videos.forEachIndexed { index, video ->
                if (index > 0) append(",")
                append("{")
                append("\"id\":\"${video.id}\",")
                append("\"title\":\"${video.title}\",")
                append("\"uri\":\"${video.uri}\",")
                append("\"duration\":${video.duration}")
                append("}")
            }
            append("]")
            append("}")
        }
        return json.toByteArray()
    }
    
    private suspend fun uploadToServer(
        metadata: SharedPlaylistMetadata,
        data: ByteArray
    ): String {
        val response = communityApi.uploadPlaylist(
            metadata = metadata,
            data = data
        )
        
        if (response.isSuccessful) {
            return response.body()?.shareLink ?: throw Exception("No share link returned")
        } else {
            throw Exception("Upload failed: ${response.code()}")
        }
    }
    
    private fun generateOfflineShareLink(
        metadata: SharedPlaylistMetadata,
        data: ByteArray
    ): String {
        val encoded = Base64.getEncoder().encodeToString(data)
        return "astralstream://playlist/${metadata.id}?data=$encoded"
    }
    
    private suspend fun saveSharedPlaylist(
        metadata: SharedPlaylistMetadata,
        shareLink: String
    ) {
        val entity = SharedPlaylistEntity(
            id = metadata.id,
            playlistId = metadata.originalPlaylistId,
            shareLink = shareLink,
            creatorId = metadata.creatorId,
            isPublic = metadata.isPublic,
            sharedAt = metadata.createdAt,
            expiresAt = metadata.createdAt + TimeUnit.DAYS.toMillis(30),
            downloads = 0,
            likes = 0
        )
        database.sharedPlaylistDao().insert(entity)
    }
    
    private fun getUserId(): String {
        // Get from preferences or generate
        return context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
            .getString("user_id", null) ?: UUID.randomUUID().toString().also { id ->
                context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
                    .edit()
                    .putString("user_id", id)
                    .apply()
            }
    }
    
    private fun getUserName(): String {
        return context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
            .getString("user_name", "Anonymous") ?: "Anonymous"
    }
    
    // Data classes
    
    data class ShareData(
        val id: String,
        val isOnline: Boolean,
        val data: String? = null
    )
    
    data class PlaylistReport(
        val playlistId: String,
        val reporterId: String,
        val reason: ReportReason,
        val details: String,
        val timestamp: Long
    )
    
    enum class ReportReason {
        INAPPROPRIATE_CONTENT,
        COPYRIGHT_VIOLATION,
        SPAM,
        MISLEADING,
        OTHER
    }
    
    enum class SortOption(val value: String) {
        POPULAR("popular"),
        RECENT("recent"),
        MOST_LIKED("most_liked"),
        MOST_DOWNLOADED("most_downloaded")
    }
    
    fun release() {
        scope.cancel()
    }
}