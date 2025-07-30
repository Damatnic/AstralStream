package com.astralplayer.astralstream.core.data.repository.fake

import android.net.Uri
import com.astralplayer.astralstream.core.data.models.VideoState
import com.astralplayer.astralstream.core.data.repository.MediaRepository
import com.astralplayer.astralstream.core.model.Folder
import com.astralplayer.astralstream.core.model.Video
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class FakeMediaRepository : MediaRepository {

    val videos = mutableListOf<Video>()
    val directories = mutableListOf<Folder>()

    override fun getVideosFlow(): Flow<List<Video>> {
        return flowOf(videos)
    }

    override fun getVideosFlowFromFolderPath(folderPath: String): Flow<List<Video>> {
        return flowOf(videos)
    }

    override fun getFoldersFlow(): Flow<List<Folder>> {
        return flowOf(directories)
    }

    override suspend fun getVideoState(uri: String): VideoState? {
        return null
    }

    override fun updateMediumLastPlayedTime(uri: String, lastPlayedTime: Long) {
    }

    override fun updateMediumPosition(uri: String, position: Long) {
    }

    override fun updateMediumPlaybackSpeed(uri: String, playbackSpeed: Float) {
    }

    override fun updateMediumAudioTrack(uri: String, audioTrackIndex: Int) {
    }

    override fun updateMediumSubtitleTrack(uri: String, subtitleTrackIndex: Int) {
    }

    override fun updateMediumZoom(uri: String, zoom: Float) {
    }

    override fun addExternalSubtitleToMedium(uri: String, subtitleUri: Uri) {
    }
}
