package com.astralplayer.community.repository

import android.content.Context
import android.util.Log
import androidx.room.withTransaction
import com.astralplayer.astralstream.data.database.AstralStreamDatabase
import com.astralplayer.community.api.*
import com.astralplayer.community.data.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlaylistSharingRepository @Inject constructor(
    private val apiManager: CommunityApiManager,
    private val database: AstralStreamDatabase,
    @ApplicationContext private val context: Context
) {
    
    companion object {
        private const val TAG = "PlaylistSharingRepository"
    }
    
    /**
     * Share a local playlist to the community
     */
    suspend fun sharePlaylist(
        playlistId: String,
        title: String,
        description: String = "",
        creatorName: String = "Anonymous",
        password: String? = null,
        isPublic: Boolean = true,
        allowDownloads: Boolean = true,
        allowComments: Boolean = true,
        category: PlaylistCategory = PlaylistCategory.GENERAL,
        language: String = "en",
        expirationDays: Int = 30,
        maxViews: Int? = null
    ): Result<ShareResult> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Sharing playlist: $playlistId")
            
            // Get local playlist details
            val localPlaylist = getLocalPlaylistDetails(playlistId)
            if (localPlaylist == null) {
                return@withContext Result.failure(Exception("Local playlist not found"))
            }
            
            // Prepare video IDs (mock for now - would get actual video IDs from local playlist)
            val videoIds = (1..localPlaylist.videoCount).map { "video_$it" }
            
            val request = SharePlaylistRequest(
                title = title,
                description = description,
                creatorName = creatorName,
                videoIds = videoIds,
                thumbnailUrl = localPlaylist.thumbnailUrl,
                password = password,
                isPublic = isPublic,
                allowDownloads = allowDownloads,
                allowComments = allowComments,
                category = category.name,
                language = language,
                expirationDays = expirationDays,
                maxViews = maxViews
            )
            
            val response = apiManager.sharePlaylist(request)
            
            if (response.success && response.data != null) {
                // Store shared playlist info locally
                val sharedEntity = SharedPlaylistEntity(
                    originalPlaylistId = playlistId,
                    shareCode = response.data.shareCode,
                    title = title,
                    description = description,
                    creatorName = creatorName,
                    creatorId = getCurrentUserId(),
                    videoCount = localPlaylist.videoCount,
                    totalDuration = localPlaylist.totalDuration,
                    thumbnailUrl = localPlaylist.thumbnailUrl,
                    shareUrl = response.data.shareUrl,
                    password = password?.let { hashPassword(it) },
                    isPublic = isPublic,
                    allowDownloads = allowDownloads,
                    allowComments = allowComments,
                    category = category,
                    language = language,
                    expirationTime = response.data.expirationTime
                )
                
                // Save to local database
                database.sharedPlaylistDao().insert(sharedEntity)
                
                Result.success(
                    ShareResult(
                        shareCode = response.data.shareCode,
                        shareUrl = response.data.shareUrl,
                        expirationTime = response.data.expirationTime
                    )
                )
            } else {
                Result.failure(Exception(response.message ?: "Failed to share playlist"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to share playlist", e)
            Result.failure(e)
        }
    }
    
    /**
     * Import a shared playlist from share code
     */
    suspend fun importPlaylist(
        shareCode: String,
        password: String? = null
    ): Result<ImportResult> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Importing playlist with code: $shareCode")
            
            // First get playlist details
            val playlistResponse = apiManager.getSharedPlaylist(shareCode)
            if (!playlistResponse.success || playlistResponse.data == null) {
                return@withContext Result.failure(
                    Exception(playlistResponse.message ?: "Playlist not found")
                )
            }
            
            val playlist = playlistResponse.data
            
            // Check if password is required
            if (playlist.hasPassword && password.isNullOrBlank()) {
                return@withContext Result.failure(
                    Exception("Password required for this playlist")
                )
            }
            
            // Import the playlist
            val importRequest = ImportPlaylistRequest(
                password = password,
                importerName = "AstralStream User"
            )
            
            val importResponse = apiManager.importPlaylist(shareCode, importRequest)
            
            if (importResponse.success && importResponse.data != null) {
                // Store import info locally for tracking
                recordPlaylistImport(shareCode, playlist, importResponse.data.playlistId)
                
                Result.success(
                    ImportResult(
                        localPlaylistId = importResponse.data.playlistId,
                        title = playlist.title,
                        videoCount = importResponse.data.importedVideoCount,
                        message = importResponse.data.message
                    )
                )
            } else {
                Result.failure(Exception(importResponse.message ?: "Failed to import playlist"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to import playlist", e)
            Result.failure(e)
        }
    }
    
    /**
     * Get trending playlists
     */
    fun getTrendingPlaylists(
        category: PlaylistCategory? = null,
        language: String? = null,
        limit: Int = 20
    ): Flow<List<TrendingPlaylist>> = flow {
        try {
            Log.d(TAG, "Fetching trending playlists")
            
            val response = apiManager.getTrendingPlaylists(
                limit = limit,
                category = category?.name,
                language = language
            )
            
            if (response.success && response.data != null) {
                val trendingPlaylists = response.data.map { playlist ->
                    TrendingPlaylist(
                        id = playlist.id,
                        shareCode = playlist.shareCode,
                        title = playlist.title,
                        description = playlist.description,
                        creatorName = playlist.creatorName,
                        videoCount = playlist.videoCount,
                        thumbnailUrl = playlist.thumbnailUrl,
                        shareUrl = playlist.shareUrl,
                        category = PlaylistCategory.valueOf(playlist.category),
                        language = playlist.language,
                        viewCount = playlist.viewCount,
                        downloadCount = playlist.downloadCount,
                        likeCount = playlist.likeCount,
                        avgRating = playlist.avgRating,
                        ratingCount = playlist.ratingCount,
                        createdAt = playlist.createdAt,
                        tags = playlist.tags
                    )
                }
                emit(trendingPlaylists)
            } else {
                Log.w(TAG, "Failed to fetch trending playlists: ${response.message}")
                emit(emptyList())
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching trending playlists", e)
            emit(emptyList())
        }
    }.flowOn(Dispatchers.IO)
    
    /**
     * Get user's shared playlists
     */
    fun getMySharedPlaylists(): Flow<List<MySharedPlaylist>> = flow {
        try {
            val sharedPlaylists = database.sharedPlaylistDao().getMySharedPlaylists(getCurrentUserId())
            
            val myPlaylists = sharedPlaylists.map { entity ->
                MySharedPlaylist(
                    id = entity.id,
                    shareCode = entity.shareCode,
                    title = entity.title,
                    description = entity.description,
                    videoCount = entity.videoCount,
                    shareUrl = entity.shareUrl,
                    createdAt = entity.createdAt,
                    expirationTime = entity.expirationTime,
                    viewCount = entity.viewCount,
                    downloadCount = entity.downloadCount,
                    likeCount = entity.likeCount,
                    isActive = entity.isActive,
                    category = entity.category,
                    language = entity.language
                )
            }
            
            emit(myPlaylists)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching my shared playlists", e)
            emit(emptyList())
        }
    }.flowOn(Dispatchers.IO)
    
    /**
     * Revoke a shared playlist
     */
    suspend fun revokeSharedPlaylist(shareCode: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Revoking shared playlist: $shareCode")
            
            // Update local database
            database.withTransaction {
                val entity = database.sharedPlaylistDao().getByShareCode(shareCode)
                if (entity != null) {
                    database.sharedPlaylistDao().update(entity.copy(isActive = false))
                }
            }
            
            // Note: In a real implementation, we would also call the API to revoke on server
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to revoke shared playlist", e)
            Result.failure(e)
        }
    }
    
    /**
     * Rate a shared playlist
     */
    suspend fun ratePlaylist(
        shareCode: String,
        rating: Float,
        review: String = ""
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Rating playlist $shareCode with $rating stars")
            
            val ratingRequest = PlaylistRatingRequest(
                rating = rating,
                review = review,
                userId = getCurrentUserId()
            )
            
            // Note: This would call the real API when available
            // val response = apiManager.ratePlaylist(shareCode, ratingRequest)
            
            // For now, just store locally
            val ratingEntity = PlaylistRatingEntity(
                playlistId = shareCode, // Using shareCode as playlist reference
                userId = getCurrentUserId(),
                rating = rating,
                review = review
            )
            
            database.playlistRatingDao().insert(ratingEntity)
            
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to rate playlist", e)
            Result.failure(e)
        }
    }
    
    /**
     * Search shared playlists
     */
    suspend fun searchPlaylists(
        query: String,
        category: PlaylistCategory? = null,
        language: String? = null,
        minRating: Float? = null,
        sortBy: String = "relevance",
        limit: Int = 20
    ): Result<List<TrendingPlaylist>> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Searching playlists with query: $query")
            
            // For now, filter trending playlists
            // In a real implementation, this would call a search API endpoint
            val trendingResponse = apiManager.getTrendingPlaylists(limit * 2, category?.name, language)
            
            if (trendingResponse.success && trendingResponse.data != null) {
                var results = trendingResponse.data
                
                // Apply search filters
                if (query.isNotBlank()) {
                    results = results.filter { playlist ->
                        playlist.title.contains(query, ignoreCase = true) ||
                        playlist.description.contains(query, ignoreCase = true) ||
                        playlist.tags.any { it.contains(query, ignoreCase = true) }
                    }
                }
                
                minRating?.let { minRat ->
                    results = results.filter { it.avgRating >= minRat }
                }
                
                // Apply sorting
                results = when (sortBy) {
                    "popularity" -> results.sortedByDescending { it.viewCount + it.downloadCount }
                    "newest" -> results.sortedByDescending { it.createdAt }
                    "rating" -> results.sortedByDescending { it.avgRating }
                    else -> results // relevance (default order)
                }
                
                val searchResults = results.take(limit).map { playlist ->
                    TrendingPlaylist(
                        id = playlist.id,
                        shareCode = playlist.shareCode,
                        title = playlist.title,
                        description = playlist.description,
                        creatorName = playlist.creatorName,
                        videoCount = playlist.videoCount,
                        thumbnailUrl = playlist.thumbnailUrl,
                        shareUrl = playlist.shareUrl,
                        category = PlaylistCategory.valueOf(playlist.category),
                        language = playlist.language,
                        viewCount = playlist.viewCount,
                        downloadCount = playlist.downloadCount,
                        likeCount = playlist.likeCount,
                        avgRating = playlist.avgRating,
                        ratingCount = playlist.ratingCount,
                        createdAt = playlist.createdAt,
                        tags = playlist.tags
                    )
                }
                
                Result.success(searchResults)
            } else {
                Result.failure(Exception(trendingResponse.message ?: "Search failed"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Search failed", e)
            Result.failure(e)
        }
    }
    
    // Private helper methods
    
    private suspend fun getLocalPlaylistDetails(playlistId: String): LocalPlaylistInfo? {
        // Mock implementation - in reality, would query local playlist database
        return LocalPlaylistInfo(
            id = playlistId,
            title = "Local Playlist",
            videoCount = 5,
            totalDuration = 3600000L, // 1 hour
            thumbnailUrl = null
        )
    }
    
    private suspend fun recordPlaylistImport(
        shareCode: String,
        playlist: SharedPlaylistResponse,
        localPlaylistId: String
    ) {
        try {
            // Record the import for tracking and analytics
            val importRecord = PlaylistImportRecord(
                shareCode = shareCode,
                originalTitle = playlist.title,
                localPlaylistId = localPlaylistId,
                importedAt = System.currentTimeMillis(),
                videoCount = playlist.videoCount
            )
            
            // In a real implementation, save to database
            Log.d(TAG, "Recorded playlist import: $importRecord")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to record playlist import", e)
        }
    }
    
    private fun getCurrentUserId(): String {
        // Generate or retrieve anonymous user ID
        val prefs = context.getSharedPreferences("community_prefs", Context.MODE_PRIVATE)
        var userId = prefs.getString("user_id", null)
        
        if (userId == null) {
            userId = "user_${System.currentTimeMillis()}_${(Math.random() * 1000).toInt()}"
            prefs.edit().putString("user_id", userId).apply()
        }
        
        return userId
    }
    
    private fun hashPassword(password: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(password.toByteArray())
        return hash.joinToString("") { "%02x".format(it) }
    }
}

// Data classes for repository results
data class ShareResult(
    val shareCode: String,
    val shareUrl: String,
    val expirationTime: Long
)

data class ImportResult(
    val localPlaylistId: String,
    val title: String,
    val videoCount: Int,
    val message: String
)

data class TrendingPlaylist(
    val id: String,
    val shareCode: String,
    val title: String,
    val description: String,
    val creatorName: String,
    val videoCount: Int,
    val thumbnailUrl: String?,
    val shareUrl: String,
    val category: PlaylistCategory,
    val language: String,
    val viewCount: Int,
    val downloadCount: Int,
    val likeCount: Int,
    val avgRating: Float,
    val ratingCount: Int,
    val createdAt: Long,
    val tags: List<String>
)

data class MySharedPlaylist(
    val id: String,
    val shareCode: String,
    val title: String,
    val description: String,
    val videoCount: Int,
    val shareUrl: String,
    val createdAt: Long,
    val expirationTime: Long,
    val viewCount: Int,
    val downloadCount: Int,
    val likeCount: Int,
    val isActive: Boolean,
    val category: PlaylistCategory,
    val language: String
)

data class LocalPlaylistInfo(
    val id: String,
    val title: String,
    val videoCount: Int,
    val totalDuration: Long,
    val thumbnailUrl: String?
)

data class PlaylistImportRecord(
    val shareCode: String,
    val originalTitle: String,
    val localPlaylistId: String,
    val importedAt: Long,
    val videoCount: Int
)