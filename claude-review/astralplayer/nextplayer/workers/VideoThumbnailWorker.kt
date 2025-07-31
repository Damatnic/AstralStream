package com.astralplayer.nextplayer.workers

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.astralplayer.nextplayer.data.dao.VideoDao
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

@HiltWorker
class VideoThumbnailWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val videoDao: VideoDao
) : CoroutineWorker(context, params) {
    
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val videoPath = inputData.getString("video_path") ?: return@withContext Result.failure()
            val thumbnailWidth = inputData.getInt("thumbnail_width", 320)
            val thumbnailHeight = inputData.getInt("thumbnail_height", 180)
            
            // Generate thumbnail
            val thumbnail = generateThumbnail(videoPath, thumbnailWidth, thumbnailHeight)
            
            if (thumbnail != null) {
                // Save thumbnail to cache
                val thumbnailPath = saveThumbnail(videoPath, thumbnail)
                
                // Update database with thumbnail path
                // Note: You'll need to add thumbnailPath field to VideoMetadata entity
                // videoDao.updateThumbnailPath(videoPath, thumbnailPath)
                
                Result.success()
            } else {
                Result.failure()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure()
        }
    }
    
    private fun generateThumbnail(videoPath: String, width: Int, height: Int): Bitmap? {
        return try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(videoPath)
            
            // Get frame at 10% of video duration
            val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0
            val timeUs = (duration * 1000 * 0.1).toLong()
            
            val bitmap = retriever.getFrameAtTime(timeUs)
            retriever.release()
            
            // Scale bitmap if needed
            bitmap?.let {
                Bitmap.createScaledBitmap(it, width, height, true)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    private fun saveThumbnail(videoPath: String, bitmap: Bitmap): String {
        val fileName = "${videoPath.hashCode()}_thumbnail.jpg"
        val thumbnailFile = File(applicationContext.cacheDir, "thumbnails/$fileName")
        
        // Create directory if it doesn't exist
        thumbnailFile.parentFile?.mkdirs()
        
        FileOutputStream(thumbnailFile).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
        }
        
        return thumbnailFile.absolutePath
    }
}