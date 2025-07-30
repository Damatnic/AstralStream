package com.astralplayer.astralstream.core.data.repository

import android.net.Uri
import com.astralplayer.astralstream.core.data.models.VideoState
import com.astralplayer.astralstream.core.model.Folder
import com.astralplayer.astralstream.core.model.Video
import kotlinx.coroutines.flow.Flow

interface MediaRepository {
    fun getVideosFlow(): Flow<List<Video>>
    fun getVideosFlowFromFolderPath(folderPath: String): Flow<List<Video>>
    fun getFoldersFlow(): Flow<List<Folder>>

    suspend fun getVideoState(uri: String): VideoState?

    fun updateMediumLastPlayedTime(uri: String, lastPlayedTime: Long)
    fun updateMediumPosition(uri: String, position: Long)
    fun updateMediumPlaybackSpeed(uri: String, playbackSpeed: Float)
    fun updateMediumAudioTrack(uri: String, audioTrackIndex: Int)
    fun updateMediumSubtitleTrack(uri: String, subtitleTrackIndex: Int)
    fun updateMediumZoom(uri: String, zoom: Float)

    fun addExternalSubtitleToMedium(uri: String, subtitleUri: Uri)
}
