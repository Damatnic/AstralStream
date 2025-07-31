package com.astralplayer.nextplayer.streaming

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.work.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@UnstableApi
@Singleton
class OfflineDownloadManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val workManager = WorkManager.getInstance(context)
    
    private val _downloads = MutableStateFlow<Map<String, DownloadInfo>>(emptyMap())
    val downloads: StateFlow<Map<String, DownloadInfo>> = _downloads.asStateFlow()
    
    private val _storageStats = MutableStateFlow(StorageStats())
    val storageStats: StateFlow<StorageStats> = _storageStats.asStateFlow()
    
    private val downloadDirectory = File(context.getExternalFilesDir(null), "downloads")
    
    init {
        downloadDirectory.mkdirs()
        updateStorageStats()
    }
    
    suspend fun startDownload(
        mediaItem: MediaItem,
        quality: AdvancedStreamingEngine.VideoQuality,
        onProgress: (Float) -> Unit
    ): AdvancedStreamingEngine.DownloadResult {
        return withContext(Dispatchers.IO) {
            try {
                val downloadId = generateDownloadId(mediaItem)
                val downloadInfo = DownloadInfo(
                    id = downloadId,
                    mediaItem = mediaItem,
                    quality = quality,
                    status = DownloadStatus.QUEUED,
                    progress = 0f,
                    startTime = System.currentTimeMillis()
                )
                
                // Update downloads map
                val currentDownloads = _downloads.value.toMutableMap()
                currentDownloads[downloadId] = downloadInfo
                _downloads.value = currentDownloads
                
                // Create WorkManager request
                val downloadRequest = OneTimeWorkRequestBuilder<DownloadWorker>()
                    .setInputData(workDataOf(
                        "download_id" to downloadId,
                        "media_uri" to mediaItem.localConfiguration?.uri.toString(),
                        "quality" to quality.name
                    ))
                    .setConstraints(
                        Constraints.Builder()
                            .setRequiredNetworkType(NetworkType.CONNECTED)
                            .setRequiresBatteryNotLow(true)
                            .build()
                    )
                    .build()
                
                workManager.enqueue(downloadRequest)
                
                // Monitor progress
                monitorDownloadProgress(downloadId, downloadRequest.id, onProgress)
                
                AdvancedStreamingEngine.DownloadResult(
                    success = true,
                    filePath = getDownloadPath(downloadId)
                )
                
            } catch (e: Exception) {
                AdvancedStreamingEngine.DownloadResult(
                    success = false,
                    error = e.message
                )
            }
        }
    }
    
    private suspend fun monitorDownloadProgress(
        downloadId: String,
        workId: java.util.UUID,
        onProgress: (Float) -> Unit
    ) {
        scope.launch {
            workManager.getWorkInfoByIdLiveData(workId).asFlow()
                .collect { workInfo ->
                    when (workInfo?.state) {
                        WorkInfo.State.RUNNING -> {
                            val progress = workInfo.progress.getFloat("progress", 0f)
                            onProgress(progress)
                            updateDownloadProgress(downloadId, progress, DownloadStatus.DOWNLOADING)
                        }
                        WorkInfo.State.SUCCEEDED -> {
                            onProgress(1f)
                            updateDownloadProgress(downloadId, 1f, DownloadStatus.COMPLETED)
                            updateStorageStats()
                        }
                        WorkInfo.State.FAILED -> {
                            val error = workInfo.outputData.getString("error") ?: "Download failed"
                            updateDownloadStatus(downloadId, DownloadStatus.FAILED, error)
                        }
                        WorkInfo.State.CANCELLED -> {
                            updateDownloadStatus(downloadId, DownloadStatus.CANCELLED)
                        }
                        else -> {}
                    }
                }
        }
    }
    
    private fun updateDownloadProgress(downloadId: String, progress: Float, status: DownloadStatus) {
        val currentDownloads = _downloads.value.toMutableMap()
        currentDownloads[downloadId]?.let { download ->
            currentDownloads[downloadId] = download.copy(
                progress = progress,
                status = status,
                lastUpdateTime = System.currentTimeMillis()
            )
            _downloads.value = currentDownloads
        }
    }
    
    private fun updateDownloadStatus(downloadId: String, status: DownloadStatus, error: String? = null) {
        val currentDownloads = _downloads.value.toMutableMap()
        currentDownloads[downloadId]?.let { download ->
            currentDownloads[downloadId] = download.copy(
                status = status,
                error = error,
                lastUpdateTime = System.currentTimeMillis()
            )
            _downloads.value = currentDownloads
        }
    }
    
    fun pauseDownload(downloadId: String) {
        val download = _downloads.value[downloadId] ?: return
        
        if (download.status == DownloadStatus.DOWNLOADING) {
            // Cancel the WorkManager job
            workManager.cancelAllWorkByTag(downloadId)
            updateDownloadStatus(downloadId, DownloadStatus.PAUSED)
        }
    }
    
    fun resumeDownload(downloadId: String) {
        val download = _downloads.value[downloadId] ?: return
        
        if (download.status == DownloadStatus.PAUSED) {
            // Restart the download
            scope.launch {
                startDownload(download.mediaItem, download.quality) { progress ->
                    // Progress callback handled in startDownload
                }
            }
        }
    }
    
    fun deleteDownload(downloadId: String) {
        val download = _downloads.value[downloadId] ?: return
        
        // Cancel if downloading
        if (download.status == DownloadStatus.DOWNLOADING) {
            workManager.cancelAllWorkByTag(downloadId)
        }
        
        // Delete file
        val file = File(getDownloadPath(downloadId))
        if (file.exists()) {
            file.delete()
        }
        
        // Remove from downloads
        val currentDownloads = _downloads.value.toMutableMap()
        currentDownloads.remove(downloadId)
        _downloads.value = currentDownloads
        
        updateStorageStats()
    }
    
    fun getCompletedDownloads(): List<DownloadInfo> {
        return _downloads.value.values.filter { it.status == DownloadStatus.COMPLETED }
    }
    
    fun getActiveDownloads(): List<DownloadInfo> {
        return _downloads.value.values.filter { 
            it.status in listOf(DownloadStatus.QUEUED, DownloadStatus.DOWNLOADING, DownloadStatus.PAUSED)
        }
    }
    
    private fun generateDownloadId(mediaItem: MediaItem): String {
        return "download_${mediaItem.localConfiguration?.uri.toString().hashCode()}_${System.currentTimeMillis()}"
    }
    
    private fun getDownloadPath(downloadId: String): String {
        return File(downloadDirectory, "$downloadId.mp4").absolutePath
    }
    
    private fun updateStorageStats() {
        val totalFiles = downloadDirectory.listFiles()?.size ?: 0
        val totalSize = downloadDirectory.listFiles()?.sumOf { it.length() } ?: 0L
        val availableSpace = downloadDirectory.usableSpace
        
        _storageStats.value = StorageStats(
            totalDownloads = totalFiles,
            totalSizeBytes = totalSize,
            availableSpaceBytes = availableSpace,
            downloadDirectory = downloadDirectory.absolutePath
        )
    }
    
    /**
     * Clean up old downloads based on storage limits
     */
    fun cleanupOldDownloads(maxStorageBytes: Long = 5_000_000_000L) { // 5GB default
        if (_storageStats.value.totalSizeBytes > maxStorageBytes) {
            val completedDownloads = getCompletedDownloads()
                .sortedBy { it.lastUpdateTime } // Oldest first
            
            var freedSpace = 0L
            val targetCleanup = _storageStats.value.totalSizeBytes - (maxStorageBytes * 0.8).toLong()
            
            for (download in completedDownloads) {
                if (freedSpace >= targetCleanup) break
                
                val file = File(getDownloadPath(download.id))
                freedSpace += file.length()
                deleteDownload(download.id)
            }
        }
    }
    
    fun release() {
        scope.cancel()
    }
    
    // Data classes
    data class DownloadInfo(
        val id: String,
        val mediaItem: MediaItem,
        val quality: AdvancedStreamingEngine.VideoQuality,
        val status: DownloadStatus,
        val progress: Float,
        val startTime: Long,
        val lastUpdateTime: Long = System.currentTimeMillis(),
        val error: String? = null,
        val estimatedSize: Long = 0L
    )
    
    data class StorageStats(
        val totalDownloads: Int = 0,
        val totalSizeBytes: Long = 0L,
        val availableSpaceBytes: Long = 0L,
        val downloadDirectory: String = ""
    )
    
    enum class DownloadStatus {
        QUEUED,
        DOWNLOADING,
        PAUSED,
        COMPLETED,
        FAILED,
        CANCELLED
    }
}

// WorkManager Worker for actual downloading
class DownloadWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    
    override suspend fun doWork(): Result {
        return try {
            val downloadId = inputData.getString("download_id") ?: return Result.failure()
            val mediaUri = inputData.getString("media_uri") ?: return Result.failure()
            val quality = inputData.getString("quality") ?: return Result.failure()
            
            // Simulate download progress
            for (i in 0..100 step 10) {
                delay(1000) // Simulate download time
                setProgress(workDataOf("progress" to i / 100f))
                
                if (isStopped) {
                    return Result.failure()
                }
            }
            
            Result.success()
            
        } catch (e: Exception) {
            Result.failure(workDataOf("error" to (e.message ?: "Download failed")))
        }
    }
}