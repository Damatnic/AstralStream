package com.astralplayer.nextplayer.workers

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.TimeUnit

@HiltWorker
class ThumbnailCleanupWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters
) : CoroutineWorker(context, params) {
    
    companion object {
        private const val THUMBNAIL_MAX_AGE_DAYS = 30L
    }
    
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val thumbnailDir = File(applicationContext.cacheDir, "thumbnails")
            if (!thumbnailDir.exists()) {
                return@withContext Result.success()
            }
            
            val currentTime = System.currentTimeMillis()
            val maxAge = TimeUnit.DAYS.toMillis(THUMBNAIL_MAX_AGE_DAYS)
            var deletedCount = 0
            
            thumbnailDir.listFiles()?.forEach { file ->
                if (file.isFile && file.name.endsWith("_thumbnail.jpg")) {
                    val fileAge = currentTime - file.lastModified()
                    if (fileAge > maxAge) {
                        if (file.delete()) {
                            deletedCount++
                        }
                    }
                }
            }
            
            Result.success(workDataOf("deleted_count" to deletedCount))
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure()
        }
    }
}