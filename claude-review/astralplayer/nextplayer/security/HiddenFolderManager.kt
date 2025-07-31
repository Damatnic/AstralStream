package com.astralplayer.nextplayer.security

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileOutputStream
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HiddenFolderManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val encryptionEngine: EncryptionEngine,
    private val securePreferences: SecurePreferences
) {
    private val hiddenFolderName = ".astralstream_hidden"
    private val hiddenFolderPath = File(context.filesDir, hiddenFolderName)
    
    init {
        if (!hiddenFolderPath.exists()) {
            hiddenFolderPath.mkdirs()
        }
    }
    
    @Serializable
    data class HiddenVideo(
        val id: String = UUID.randomUUID().toString(),
        val originalPath: String,
        val encryptedPath: String,
        val thumbnailPath: String?,
        val title: String,
        val duration: Long,
        val hiddenAt: Long = System.currentTimeMillis()
    )
    
    suspend fun hideVideo(videoFile: File): HiddenVideo? = withContext(Dispatchers.IO) {
        try {
            val videoId = UUID.randomUUID().toString()
            val encryptedFile = File(hiddenFolderPath, "$videoId.enc")
            
            // Extract metadata before encryption
            val metadata = extractVideoMetadata(videoFile)
            
            // Generate thumbnail
            val thumbnailFile = generateThumbnail(videoFile, videoId)
            
            // Encrypt the video
            if (encryptionEngine.encryptFile(videoFile, encryptedFile)) {
                val hiddenVideo = HiddenVideo(
                    id = videoId,
                    originalPath = videoFile.absolutePath,
                    encryptedPath = encryptedFile.absolutePath,
                    thumbnailPath = thumbnailFile?.absolutePath,
                    title = metadata.title,
                    duration = metadata.duration
                )
                
                // Save to secure storage
                saveHiddenVideo(hiddenVideo)
                
                // Delete original file
                videoFile.delete()
                
                hiddenVideo
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("HiddenFolderManager", "Failed to hide video", e)
            null
        }
    }
    
    suspend fun unhideVideo(hiddenVideo: HiddenVideo): File? = withContext(Dispatchers.IO) {
        try {
            val encryptedFile = File(hiddenVideo.encryptedPath)
            val restoredFile = File(hiddenVideo.originalPath)
            
            if (encryptionEngine.decryptFile(encryptedFile, restoredFile)) {
                // Remove from hidden list
                removeHiddenVideo(hiddenVideo.id)
                
                // Delete encrypted file and thumbnail
                encryptedFile.delete()
                hiddenVideo.thumbnailPath?.let { File(it).delete() }
                
                restoredFile
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("HiddenFolderManager", "Failed to unhide video", e)
            null
        }
    }
    
    fun getHiddenVideos(): List<HiddenVideo> {
        val encryptedList = securePreferences.getEncryptedString("hidden_videos") ?: return emptyList()
        return try {
            Json.decodeFromString(encryptedList)
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    private fun saveHiddenVideo(video: HiddenVideo) {
        val currentList = getHiddenVideos().toMutableList()
        currentList.add(video)
        val json = Json.encodeToString(currentList)
        securePreferences.putEncryptedString("hidden_videos", json)
    }
    
    private fun removeHiddenVideo(videoId: String) {
        val currentList = getHiddenVideos().toMutableList()
        currentList.removeAll { it.id == videoId }
        val json = Json.encodeToString(currentList)
        securePreferences.putEncryptedString("hidden_videos", json)
    }
    
    private fun extractVideoMetadata(file: File): VideoMetadata {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(file.absolutePath)
            VideoMetadata(
                title = file.nameWithoutExtension,
                duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLong() ?: 0
            )
        } catch (e: Exception) {
            VideoMetadata(file.nameWithoutExtension, 0)
        } finally {
            retriever.release()
        }
    }
    
    private suspend fun generateThumbnail(videoFile: File, videoId: String): File? = withContext(Dispatchers.IO) {
        try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(videoFile.absolutePath)
            val bitmap = retriever.getFrameAtTime(1000000) // 1 second
            retriever.release()
            
            bitmap?.let {
                val thumbnailFile = File(hiddenFolderPath, "$videoId.jpg")
                FileOutputStream(thumbnailFile).use { out ->
                    it.compress(Bitmap.CompressFormat.JPEG, 90, out)
                }
                thumbnailFile
            }
        } catch (e: Exception) {
            null
        }
    }
    
    data class VideoMetadata(val title: String, val duration: Long)
}