package com.astralplayer.download

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.*
import com.astralplayer.domain.model.VideoMetadata
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import java.io.File
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages video downloads with queue, progress tracking, and background service
 */
@Singleton
class DownloadManager @Inject constructor(
    private val context: Context,
    private val offlineVideoRepository: OfflineVideoRepository
) {
    
    companion object {
        private const val CHANNEL_ID = "video_downloads"
        private const val NOTIFICATION_ID = 1001
        private const val MAX_CONCURRENT_DOWNLOADS = 3
    }
    
    private val workManager = WorkManager.getInstance(context)
    
    private val _downloadQueue = MutableStateFlow<List<DownloadTask>>(emptyList())
    val downloadQueue: StateFlow<List<DownloadTask>> = _downloadQueue.asStateFlow()
    
    private val _activeDownloads = MutableStateFlow<Map<String, DownloadProgress>>(emptyMap())
    val activeDownloads: StateFlow<Map<String, DownloadProgress>> = _activeDownloads.asStateFlow()
    
    init {
        createNotificationChannel()
        observeDownloadWorkers()
    }
    
    /**
     * Add video to download queue
     */
    suspend fun queueDownload(
        videoUri: String,
        metadata: VideoMetadata,
        quality: VideoQuality = VideoQuality.HIGH
    ): String {
        val downloadId = UUID.randomUUID().toString()
        
        val task = DownloadTask(
            id = downloadId,
            videoUri = videoUri,
            metadata = metadata,
            quality = quality,
            status = DownloadStatus.QUEUED,
            createdAt = System.currentTimeMillis()
        )
        
        // Add to queue
        _downloadQueue.value = _downloadQueue.value + task
        
        // Save to database
        offlineVideoRepository.insertDownloadTask(task)
        
        // Schedule download work
        scheduleDownload(task)
        
        Timber.d("Download queued: ${metadata.title}")
        return downloadId
    }
    
    /**
     * Pause download
     */
    fun pauseDownload(downloadId: String) {
        workManager.cancelWorkById(UUID.fromString(downloadId))
        updateDownloadStatus(downloadId, DownloadStatus.PAUSED)
    }
    
    /**
     * Resume download
     */
    fun resumeDownload(downloadId: String) {
        val task = _downloadQueue.value.find { it.id == downloadId } ?: return
        scheduleDownload(task)
        updateDownloadStatus(downloadId, DownloadStatus.QUEUED)
    }
    
    /**
     * Cancel download
     */
    suspend fun cancelDownload(downloadId: String) {
        workManager.cancelWorkById(UUID.fromString(downloadId))
        
        // Remove from queue
        _downloadQueue.value = _downloadQueue.value.filter { it.id != downloadId }
        
        // Delete from database
        offlineVideoRepository.deleteDownloadTask(downloadId)
        
        // Delete partial file if exists
        deletePartialFile(downloadId)
    }
    
    /**
     * Clear completed downloads
     */
    suspend fun clearCompleted() {
        val completed = _downloadQueue.value.filter { 
            it.status == DownloadStatus.COMPLETED 
        }
        
        completed.forEach { task ->
            _downloadQueue.value = _downloadQueue.value - task
            offlineVideoRepository.deleteDownloadTask(task.id)
        }
    }
    
    /**
     * Get download progress
     */
    fun getProgress(downloadId: String): DownloadProgress? {
        return _activeDownloads.value[downloadId]
    }
    
    /**
     * Schedule download work
     */
    private fun scheduleDownload(task: DownloadTask) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresStorageNotLow(true)
            .build()
        
        val downloadData = workDataOf(
            "download_id" to task.id,
            "video_uri" to task.videoUri,
            "video_title" to task.metadata.title,
            "quality" to task.quality.name
        )
        
        val downloadRequest = OneTimeWorkRequestBuilder<DownloadWorker>()
            .setId(UUID.fromString(task.id))
            .setConstraints(constraints)
            .setInputData(downloadData)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                WorkRequest.MIN_BACKOFF_MILLIS,
                TimeUnit.MILLISECONDS
            )
            .build()
        
        workManager.enqueueUniqueWork(
            "download_${task.id}",
            ExistingWorkPolicy.KEEP,
            downloadRequest
        )
    }
    
    /**
     * Observe download workers
     */
    private fun observeDownloadWorkers() {
        workManager.getWorkInfosForUniqueWorkLiveData("downloads").observeForever { workInfos ->
            workInfos.forEach { workInfo ->
                val downloadId = workInfo.outputData.getString("download_id") ?: return@forEach
                
                when (workInfo.state) {
                    WorkInfo.State.RUNNING -> {
                        val progress = workInfo.progress.getInt("progress", 0)
                        val total = workInfo.progress.getLong("total", 0L)
                        val downloaded = workInfo.progress.getLong("downloaded", 0L)
                        
                        updateProgress(downloadId, progress, downloaded, total)
                    }
                    
                    WorkInfo.State.SUCCEEDED -> {
                        updateDownloadStatus(downloadId, DownloadStatus.COMPLETED)
                        _activeDownloads.value = _activeDownloads.value - downloadId
                    }
                    
                    WorkInfo.State.FAILED -> {
                        updateDownloadStatus(downloadId, DownloadStatus.FAILED)
                        _activeDownloads.value = _activeDownloads.value - downloadId
                    }
                    
                    else -> {}
                }
            }
        }
    }
    
    /**
     * Update download progress
     */
    private fun updateProgress(
        downloadId: String,
        progress: Int,
        downloadedBytes: Long,
        totalBytes: Long
    ) {
        _activeDownloads.value = _activeDownloads.value + (
            downloadId to DownloadProgress(
                downloadId = downloadId,
                progress = progress,
                downloadedBytes = downloadedBytes,
                totalBytes = totalBytes,
                downloadSpeed = calculateSpeed(downloadId, downloadedBytes)
            )
        )
    }
    
    /**
     * Update download status
     */
    private fun updateDownloadStatus(downloadId: String, status: DownloadStatus) {
        _downloadQueue.value = _downloadQueue.value.map { task ->
            if (task.id == downloadId) {
                task.copy(status = status)
            } else {
                task
            }
        }
    }
    
    /**
     * Calculate download speed
     */
    private fun calculateSpeed(downloadId: String, currentBytes: Long): Long {
        val previousProgress = _activeDownloads.value[downloadId]
        return if (previousProgress != null) {
            val timeDiff = System.currentTimeMillis() - previousProgress.timestamp
            if (timeDiff > 0) {
                ((currentBytes - previousProgress.downloadedBytes) * 1000) / timeDiff
            } else {
                0L
            }
        } else {
            0L
        }
    }
    
    /**
     * Delete partial download file
     */
    private fun deletePartialFile(downloadId: String) {
        val downloadDir = File(context.getExternalFilesDir(null), "downloads")
        val partialFile = File(downloadDir, "$downloadId.part")
        if (partialFile.exists()) {
            partialFile.delete()
        }
    }
    
    /**
     * Create notification channel
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Video Downloads",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows video download progress"
            }
            
            val notificationManager = context.getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    /**
     * Get storage info
     */
    fun getStorageInfo(): StorageInfo {
        val downloadDir = File(context.getExternalFilesDir(null), "downloads")
        val totalSpace = downloadDir.totalSpace
        val freeSpace = downloadDir.freeSpace
        val usedSpace = totalSpace - freeSpace
        
        return StorageInfo(
            totalSpace = totalSpace,
            usedSpace = usedSpace,
            freeSpace = freeSpace,
            downloadCount = _downloadQueue.value.count { 
                it.status == DownloadStatus.COMPLETED 
            }
        )
    }
}

/**
 * Download task data
 */
data class DownloadTask(
    val id: String,
    val videoUri: String,
    val metadata: VideoMetadata,
    val quality: VideoQuality,
    val status: DownloadStatus,
    val createdAt: Long,
    val completedAt: Long? = null,
    val filePath: String? = null,
    val fileSize: Long? = null
)

/**
 * Download progress data
 */
data class DownloadProgress(
    val downloadId: String,
    val progress: Int,
    val downloadedBytes: Long,
    val totalBytes: Long,
    val downloadSpeed: Long,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Download status
 */
enum class DownloadStatus {
    QUEUED,
    DOWNLOADING,
    PAUSED,
    COMPLETED,
    FAILED,
    CANCELLED
}

/**
 * Video quality options
 */
enum class VideoQuality {
    LOW,    // 480p
    MEDIUM, // 720p
    HIGH,   // 1080p
    ULTRA   // 4K
}

/**
 * Storage information
 */
data class StorageInfo(
    val totalSpace: Long,
    val usedSpace: Long,
    val freeSpace: Long,
    val downloadCount: Int
)