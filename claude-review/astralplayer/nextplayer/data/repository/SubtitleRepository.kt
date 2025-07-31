package com.astralplayer.nextplayer.data.repository

import android.net.Uri
import com.astralplayer.nextplayer.ai.SubtitleFile
import kotlinx.coroutines.flow.Flow

interface SubtitleRepository {
    suspend fun saveSubtitle(videoUri: Uri, subtitleFile: SubtitleFile)
    suspend fun getSubtitle(videoUri: Uri): SubtitleFile?
    suspend fun getAllSubtitles(): List<SubtitleFile>
    suspend fun deleteSubtitle(videoUri: Uri)
    fun observeSubtitle(videoUri: Uri): Flow<SubtitleFile?>
}