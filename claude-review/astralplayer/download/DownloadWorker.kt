package com.astralplayer.download

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.astralplayer.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import javax.inject.Inject

/**
 * Worker for downloading videos in background
 */
class DownloadWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    
    companion object {
        private const val CHANNEL_ID = "video_downloads"
        private const val NOTIFICATION_ID = 1001
        private const val BUFFER_SIZE = 8192
    }
    
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val downloadId = inputData.getString("download_id") ?: return@withContext Result.failure()
        val videoUri = inputData.getString("video_uri") ?: return@withContext Result.failure()
        val videoTitle = inputData.getString("video_title") ?: "Video"
        val quality = inputData.getString("quality") ?: "HIGH"
        
        try {
            // Create notification channel
            createNotificationChannel()
            
            // Set foreground for long-running download
            setForeground(createForegroundInfo(videoTitle, 0))
            
            // Prepare download directory
            val downloadDir = File(applicationContext.getExternalFilesDir(null), "downloads")
            if (!downloadDir.exists()) {
                downloadDir.mkdirs()
            }
            
            // Download file
            val outputFile = File(downloadDir, "$downloadId.mp4")
            downloadFile(videoUri, outputFile, videoTitle) { progress, downloaded, total ->
                // Update progress
                setProgress(workDataOf(
                    "progress" to progress,
                    "downloaded" to downloaded,
                    "total" to total
                ))
                
                // Update notification
                setForeground(createForegroundInfo(videoTitle, progress))
            }
            
            // Download completed
            return@withContext Result.success(workDataOf(
                "download_id" to downloadId,
                "file_path" to outputFile.absolutePath,
                "file_size" to outputFile.length()
            ))
            
        } catch (e: Exception) {
            Timber.e(e, "Download failed for $videoUri")
            return@withContext Result.failure(workDataOf(
                "error" to e.message
            ))
        }
    }
    
    /**
     * Download file with progress callback
     */
    private suspend fun downloadFile(
        url: String,
        outputFile: File,
        title: String,
        onProgress: suspend (progress: Int, downloaded: Long, total: Long) -> Unit
    ) = withContext(Dispatchers.IO) {
        var partialFile: File? = null
        
        try {
            partialFile = File(outputFile.parent, "${outputFile.name}.part")
            
            val connection = URL(url).openConnection()
            connection.connect()
            
            val totalSize = connection.contentLength.toLong()
            var downloadedSize = 0L
            
            // Check if partial file exists for resume
            if (partialFile.exists()) {
                downloadedSize = partialFile.length()
                connection.setRequestProperty("Range", "bytes=$downloadedSize-")
            }
            
            connection.inputStream.use { input ->
                FileOutputStream(partialFile, true).use { output ->
                    val buffer = ByteArray(BUFFER_SIZE)
                    var bytesRead: Int
                    var lastProgressUpdate = 0L
                    
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        // Check if cancelled
                        if (isStopped) {
                            throw InterruptedException("Download cancelled")
                        }
                        
                        output.write(buffer, 0, bytesRead)
                        downloadedSize += bytesRead
                        
                        // Update progress every 100KB
                        if (downloadedSize - lastProgressUpdate > 100 * 1024) {
                            val progress = ((downloadedSize * 100) / totalSize).toInt()
                            onProgress(progress, downloadedSize, totalSize)
                            lastProgressUpdate = downloadedSize
                        }
                    }
                }
            }
            
            // Rename to final file
            partialFile.renameTo(outputFile)
            
            // Final progress update
            onProgress(100, totalSize, totalSize)
            
        } catch (e: Exception) {
            if (e !is InterruptedException) {
                partialFile?.delete()
            }
            throw e
        }
    }
    
    /**
     * Create foreground notification info
     */
    private fun createForegroundInfo(title: String, progress: Int): ForegroundInfo {
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setContentTitle("Downloading $title")
            .setContentText("$progress% complete")
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setProgress(100, progress, progress == 0)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
        
        return ForegroundInfo(NOTIFICATION_ID, notification)
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
            
            val notificationManager = applicationContext.getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
}