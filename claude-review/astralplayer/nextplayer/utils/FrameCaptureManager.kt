package com.astralplayer.nextplayer.utils

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Environment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

object FrameCaptureManager {
    
    suspend fun captureFrame(context: Context, videoUri: String, timeUs: Long = -1): Bitmap? {
        return withContext(Dispatchers.IO) {
            try {
                val retriever = MediaMetadataRetriever()
                retriever.setDataSource(context, Uri.parse(videoUri))
                
                val bitmap = if (timeUs >= 0) {
                    retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                } else {
                    retriever.getFrameAtTime()
                }
                
                retriever.release()
                bitmap
            } catch (e: Exception) {
                null
            }
        }
    }
    
    suspend fun saveScreenshot(context: Context, bitmap: Bitmap, videoTitle: String = "video"): String? {
        return withContext(Dispatchers.IO) {
            try {
                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                val fileName = "AstralPlayer_${videoTitle}_$timestamp.jpg"
                
                val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                val astralDir = File(picturesDir, "AstralPlayer")
                if (!astralDir.exists()) astralDir.mkdirs()
                
                val file = File(astralDir, fileName)
                FileOutputStream(file).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
                }
                
                file.absolutePath
            } catch (e: Exception) {
                null
            }
        }
    }
    
    suspend fun captureAndSave(context: Context, videoUri: String, videoTitle: String, timeUs: Long = -1): String? {
        val bitmap = captureFrame(context, videoUri, timeUs) ?: return null
        return saveScreenshot(context, bitmap, videoTitle)
    }
}