package com.astralplayer.download

import com.astralplayer.data.database.AstralStreamDatabase
import com.astralplayer.data.entity.OfflineVideoEntity
import com.astralplayer.domain.model.VideoMetadata
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for managing offline videos
 */
@Singleton
class OfflineVideoRepository @Inject constructor(
    private val database: AstralStreamDatabase
) {
    
    private val offlineVideoDao = database.offlineVideoDao()
    
    /**
     * Get all offline videos
     */
    fun getOfflineVideos(): Flow<List<OfflineVideo>> {
        return offlineVideoDao.getAllOfflineVideos().map { entities ->
            entities.map { it.toOfflineVideo() }
        }
    }
    
    /**
     * Get offline video by ID
     */
    suspend fun getOfflineVideo(videoId: String): OfflineVideo? {
        return offlineVideoDao.getOfflineVideo(videoId)?.toOfflineVideo()
    }
    
    /**
     * Insert offline video
     */
    suspend fun insertOfflineVideo(
        videoUri: String,
        localPath: String,
        metadata: VideoMetadata,
        fileSize: Long
    ) {
        val entity = OfflineVideoEntity(
            videoId = videoUri.hashCode().toString(),
            originalUri = videoUri,
            localPath = localPath,
            title = metadata.title,
            duration = metadata.duration,
            thumbnailPath = null, // Will be set later
            fileSize = fileSize,
            downloadedAt = System.currentTimeMillis(),
            lastPlayedAt = null,
            watchProgress = 0L
        )
        
        offlineVideoDao.insert(entity)
    }
    
    /**
     * Update watch progress
     */
    suspend fun updateWatchProgress(videoId: String, progress: Long) {
        offlineVideoDao.updateWatchProgress(videoId, progress, System.currentTimeMillis())
    }
    
    /**
     * Delete offline video
     */
    suspend fun deleteOfflineVideo(videoId: String) {
        offlineVideoDao.delete(videoId)
    }
    
    /**
     * Get total storage used
     */
    suspend fun getTotalStorageUsed(): Long {
        return offlineVideoDao.getTotalStorageUsed() ?: 0L
    }
    
    /**
     * Search offline videos
     */
    fun searchOfflineVideos(query: String): Flow<List<OfflineVideo>> {
        return offlineVideoDao.searchVideos("%$query%").map { entities ->
            entities.map { it.toOfflineVideo() }
        }
    }
    
    /**
     * Insert download task
     */
    suspend fun insertDownloadTask(task: DownloadTask) {
        // Implementation would save to a download tasks table
    }
    
    /**
     * Delete download task
     */
    suspend fun deleteDownloadTask(taskId: String) {
        // Implementation would delete from download tasks table
    }
}

/**
 * Offline video model
 */
data class OfflineVideo(
    val videoId: String,
    val originalUri: String,
    val localPath: String,
    val title: String,
    val duration: Long,
    val thumbnailPath: String?,
    val fileSize: Long,
    val downloadedAt: Long,
    val lastPlayedAt: Long?,
    val watchProgress: Long
)

/**
 * Extension to convert entity to domain model
 */
fun OfflineVideoEntity.toOfflineVideo(): OfflineVideo {
    return OfflineVideo(
        videoId = videoId,
        originalUri = originalUri,
        localPath = localPath,
        title = title,
        duration = duration,
        thumbnailPath = thumbnailPath,
        fileSize = fileSize,
        downloadedAt = downloadedAt,
        lastPlayedAt = lastPlayedAt,
        watchProgress = watchProgress
    )
}