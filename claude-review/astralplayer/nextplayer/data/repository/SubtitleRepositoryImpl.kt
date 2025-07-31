package com.astralplayer.nextplayer.data.repository

import android.content.Context
import android.net.Uri
import com.astralplayer.nextplayer.ai.SubtitleFile
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SubtitleRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : SubtitleRepository {
    
    private val gson = Gson()
    private val subtitleDir = File(context.filesDir, "subtitles").apply { mkdirs() }
    private val subtitleCache = mutableMapOf<String, MutableStateFlow<SubtitleFile?>>()
    
    override suspend fun saveSubtitle(videoUri: Uri, subtitleFile: SubtitleFile) {
        withContext(Dispatchers.IO) {
            val fileName = "${videoUri.hashCode()}.json"
            val file = File(subtitleDir, fileName)
            file.writeText(gson.toJson(subtitleFile))
            
            // Update cache
            getOrCreateFlow(videoUri).value = subtitleFile
        }
    }
    
    override suspend fun getSubtitle(videoUri: Uri): SubtitleFile? {
        return withContext(Dispatchers.IO) {
            val fileName = "${videoUri.hashCode()}.json"
            val file = File(subtitleDir, fileName)
            
            if (file.exists()) {
                try {
                    gson.fromJson(file.readText(), SubtitleFile::class.java)
                } catch (e: Exception) {
                    null
                }
            } else {
                null
            }
        }
    }
    
    override suspend fun getAllSubtitles(): List<SubtitleFile> {
        return withContext(Dispatchers.IO) {
            subtitleDir.listFiles()?.mapNotNull { file ->
                try {
                    gson.fromJson(file.readText(), SubtitleFile::class.java)
                } catch (e: Exception) {
                    null
                }
            } ?: emptyList()
        }
    }
    
    override suspend fun deleteSubtitle(videoUri: Uri) {
        withContext(Dispatchers.IO) {
            val fileName = "${videoUri.hashCode()}.json"
            val file = File(subtitleDir, fileName)
            file.delete()
            
            // Update cache
            getOrCreateFlow(videoUri).value = null
        }
    }
    
    override fun observeSubtitle(videoUri: Uri): Flow<SubtitleFile?> {
        return getOrCreateFlow(videoUri).asStateFlow()
    }
    
    private fun getOrCreateFlow(videoUri: Uri): MutableStateFlow<SubtitleFile?> {
        val key = videoUri.toString()
        return subtitleCache.getOrPut(key) {
            MutableStateFlow(null)
        }
    }
}