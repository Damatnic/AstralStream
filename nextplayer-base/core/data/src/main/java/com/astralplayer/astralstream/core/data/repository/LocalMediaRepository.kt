package com.astralplayer.astralstream.core.data.repository

import android.net.Uri
import com.astralplayer.astralstream.core.common.di.ApplicationScope
import com.astralplayer.astralstream.core.data.mappers.toFolder
import com.astralplayer.astralstream.core.data.mappers.toVideo
import com.astralplayer.astralstream.core.data.mappers.toVideoState
import com.astralplayer.astralstream.core.data.models.VideoState
import com.astralplayer.astralstream.core.database.converter.UriListConverter
import com.astralplayer.astralstream.core.database.dao.DirectoryDao
import com.astralplayer.astralstream.core.database.dao.MediumDao
import com.astralplayer.astralstream.core.database.relations.DirectoryWithMedia
import com.astralplayer.astralstream.core.database.relations.MediumWithInfo
import com.astralplayer.astralstream.core.model.Folder
import com.astralplayer.astralstream.core.model.Video
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class LocalMediaRepository @Inject constructor(
    private val mediumDao: MediumDao,
    private val directoryDao: DirectoryDao,
    @ApplicationScope private val applicationScope: CoroutineScope,
) : MediaRepository {

    override fun getVideosFlow(): Flow<List<Video>> {
        return mediumDao.getAllWithInfo().map { it.map(MediumWithInfo::toVideo) }
    }

    override fun getVideosFlowFromFolderPath(folderPath: String): Flow<List<Video>> {
        return mediumDao.getAllWithInfoFromDirectory(folderPath).map { it.map(MediumWithInfo::toVideo) }
    }

    override fun getFoldersFlow(): Flow<List<Folder>> {
        return directoryDao.getAllWithMedia().map { it.map(DirectoryWithMedia::toFolder) }
    }

    override suspend fun getVideoState(uri: String): VideoState? {
        return mediumDao.get(uri)?.toVideoState()
    }

    override fun updateMediumLastPlayedTime(uri: String, lastPlayedTime: Long) {
        applicationScope.launch {
            mediumDao.updateMediumLastPlayedTime(uri, lastPlayedTime)
        }
    }

    override fun updateMediumPosition(uri: String, position: Long) {
        applicationScope.launch {
            val duration = mediumDao.get(uri)?.duration ?: position.plus(1)
            mediumDao.updateMediumPosition(
                uri = uri,
                position = position.takeIf { it < duration } ?: Long.MIN_VALUE.plus(1),
            )
            mediumDao.updateMediumLastPlayedTime(uri, System.currentTimeMillis())
        }
    }

    override fun updateMediumPlaybackSpeed(uri: String, playbackSpeed: Float) {
        applicationScope.launch {
            mediumDao.updateMediumPlaybackSpeed(uri, playbackSpeed)
            mediumDao.updateMediumLastPlayedTime(uri, System.currentTimeMillis())
        }
    }

    override fun updateMediumAudioTrack(uri: String, audioTrackIndex: Int) {
        applicationScope.launch {
            mediumDao.updateMediumAudioTrack(uri, audioTrackIndex)
            mediumDao.updateMediumLastPlayedTime(uri, System.currentTimeMillis())
        }
    }

    override fun updateMediumSubtitleTrack(uri: String, subtitleTrackIndex: Int) {
        applicationScope.launch {
            mediumDao.updateMediumSubtitleTrack(uri, subtitleTrackIndex)
            mediumDao.updateMediumLastPlayedTime(uri, System.currentTimeMillis())
        }
    }

    override fun updateMediumZoom(uri: String, zoom: Float) {
        applicationScope.launch {
            mediumDao.updateMediumZoom(uri, zoom)
            mediumDao.updateMediumLastPlayedTime(uri, System.currentTimeMillis())
        }
    }

    override fun addExternalSubtitleToMedium(uri: String, subtitleUri: Uri) {
        applicationScope.launch {
            val currentExternalSubs = getVideoState(uri)?.externalSubs ?: emptyList()
            if (currentExternalSubs.contains(subtitleUri)) return@launch
            mediumDao.addExternalSubtitle(
                mediumUri = uri,
                externalSubs = UriListConverter.fromListToString(urlList = currentExternalSubs + subtitleUri),
            )
        }
    }
}
