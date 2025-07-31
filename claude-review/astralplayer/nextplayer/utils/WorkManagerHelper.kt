package com.astralplayer.nextplayer.utils

import android.content.Context
import androidx.work.*
import com.astralplayer.nextplayer.workers.VideoThumbnailWorker
import java.util.concurrent.TimeUnit

object WorkManagerHelper {
    
    fun scheduleThumbnailGeneration(context: Context, videoPath: String) {
        val inputData = workDataOf(
            "video_path" to videoPath,
            "thumbnail_width" to 320,
            "thumbnail_height" to 180
        )
        
        val thumbnailWork = OneTimeWorkRequestBuilder<VideoThumbnailWorker>()
            .setInputData(inputData)
            .setConstraints(
                Constraints.Builder()
                    .setRequiresStorageNotLow(true)
                    .build()
            )
            .addTag("thumbnail_generation")
            .build()
            
        WorkManager.getInstance(context)
            .enqueueUniqueWork(
                "thumbnail_$videoPath",
                ExistingWorkPolicy.KEEP,
                thumbnailWork
            )
    }
    
    fun scheduleBatchThumbnailGeneration(context: Context, videoPaths: List<String>) {
        val workRequests = videoPaths.map { videoPath ->
            val inputData = workDataOf(
                "video_path" to videoPath,
                "thumbnail_width" to 320,
                "thumbnail_height" to 180
            )
            
            OneTimeWorkRequestBuilder<VideoThumbnailWorker>()
                .setInputData(inputData)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiresStorageNotLow(true)
                        .setRequiresBatteryNotLow(true)
                        .build()
                )
                .addTag("batch_thumbnail_generation")
                .build()
        }
        
        WorkManager.getInstance(context)
            .enqueue(workRequests)
    }
    
    fun cancelThumbnailGeneration(context: Context, videoPath: String) {
        WorkManager.getInstance(context)
            .cancelUniqueWork("thumbnail_$videoPath")
    }
    
    fun cancelAllThumbnailGeneration(context: Context) {
        WorkManager.getInstance(context)
            .cancelAllWorkByTag("thumbnail_generation")
        WorkManager.getInstance(context)
            .cancelAllWorkByTag("batch_thumbnail_generation")
    }
    
    // Example of periodic work - clean old thumbnails
    fun schedulePeriodicThumbnailCleanup(context: Context) {
        val cleanupWork = PeriodicWorkRequestBuilder<ThumbnailCleanupWorker>(
            7, TimeUnit.DAYS, // Run weekly
            1, TimeUnit.HOURS  // Flex interval
        )
            .setConstraints(
                Constraints.Builder()
                    .setRequiresDeviceIdle(true)
                    .setRequiresStorageNotLow(true)
                    .build()
            )
            .addTag("thumbnail_cleanup")
            .build()
            
        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(
                "thumbnail_cleanup",
                ExistingPeriodicWorkPolicy.KEEP,
                cleanupWork
            )
    }
}