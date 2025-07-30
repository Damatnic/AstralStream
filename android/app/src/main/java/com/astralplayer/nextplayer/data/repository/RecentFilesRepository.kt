package com.astralplayer.nextplayer.data.repository

import com.astralplayer.nextplayer.data.database.RecentFilesDao
import com.astralplayer.nextplayer.data.database.RecentFileEntity
import com.astralplayer.nextplayer.data.RecentFile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

interface RecentFilesRepository {
    fun getAllRecentFiles(): Flow<List<RecentFile>>
    suspend fun insertRecentFile(recentFile: RecentFile)
    suspend fun updateRecentFile(recentFile: RecentFile)
    suspend fun deleteRecentFile(recentFile: RecentFile)
    suspend fun getRecentFile(id: String): RecentFile?
    suspend fun clearAllRecentFiles()
    suspend fun toggleFavorite(fileId: String)
    fun getFavoriteFiles(): Flow<List<RecentFile>>
}

// @Singleton // Temporarily disabled for Hilt
class RecentFilesRepositoryImpl(
    private val recentFilesDao: RecentFilesDao
) : RecentFilesRepository {
    
    override fun getAllRecentFiles(): Flow<List<RecentFile>> {
        return recentFilesDao.getAllRecentFiles().map { entities ->
            entities.map { entity ->
                RecentFile(
                    id = entity.id,
                    uri = entity.uri,
                    title = entity.title,
                    duration = entity.duration,
                    lastPosition = entity.lastPosition,
                    lastPlayed = entity.lastPlayedTime,
                    thumbnailPath = entity.thumbnailPath,
                    fileSize = entity.fileSize,
                    mimeType = entity.mimeType,
                    isCloudFile = entity.isCloudFile,
                    cloudProvider = entity.cloudProvider,
                    cloudFileId = entity.cloudFileId,
                    playCount = entity.playCount,
                    isFavorite = entity.isFavorite,
                    tags = entity.tags,
                    metadata = entity.metadata
                )
            }
        }
    }
    
    override suspend fun insertRecentFile(recentFile: RecentFile) {
        val entity = RecentFileEntity(
            id = recentFile.id,
            uri = recentFile.uri,
            title = recentFile.title,
            duration = recentFile.duration,
            lastPosition = recentFile.lastPosition,
            lastPlayedTime = recentFile.lastPlayed,
            thumbnailPath = recentFile.thumbnailPath,
            fileSize = recentFile.fileSize,
            mimeType = recentFile.mimeType,
            isCloudFile = recentFile.isCloudFile,
            cloudProvider = recentFile.cloudProvider,
            cloudFileId = recentFile.cloudFileId,
            playCount = recentFile.playCount,
            isFavorite = recentFile.isFavorite,
            tags = recentFile.tags,
            metadata = recentFile.metadata
        )
        recentFilesDao.insertRecentFile(entity)
    }
    
    override suspend fun updateRecentFile(recentFile: RecentFile) {
        val existingEntity = recentFilesDao.getRecentFileById(recentFile.id)
        existingEntity?.let { entity ->
            recentFilesDao.updateRecentFile(
                entity.copy(
                    uri = recentFile.uri,
                    title = recentFile.title,
                    duration = recentFile.duration,
                    lastPosition = recentFile.lastPosition,
                    lastPlayedTime = recentFile.lastPlayed,
                    thumbnailPath = recentFile.thumbnailPath,
                    fileSize = recentFile.fileSize,
                    mimeType = recentFile.mimeType,
                    isCloudFile = recentFile.isCloudFile,
                    cloudProvider = recentFile.cloudProvider,
                    cloudFileId = recentFile.cloudFileId,
                    playCount = recentFile.playCount,
                    isFavorite = recentFile.isFavorite,
                    tags = recentFile.tags,
                    metadata = recentFile.metadata
                )
            )
        }
    }
    
    override suspend fun deleteRecentFile(recentFile: RecentFile) {
        val entity = recentFilesDao.getRecentFileById(recentFile.id)
        entity?.let { recentFilesDao.deleteRecentFile(it) }
    }
    
    override suspend fun getRecentFile(id: String): RecentFile? {
        return recentFilesDao.getRecentFileById(id)?.let { entity ->
            RecentFile(
                id = entity.id,
                uri = entity.uri,
                title = entity.title,
                duration = entity.duration,
                lastPosition = entity.lastPosition,
                lastPlayed = entity.lastPlayedTime,
                thumbnailPath = entity.thumbnailPath,
                fileSize = entity.fileSize,
                mimeType = entity.mimeType,
                isCloudFile = entity.isCloudFile,
                cloudProvider = entity.cloudProvider,
                cloudFileId = entity.cloudFileId,
                playCount = entity.playCount,
                isFavorite = entity.isFavorite,
                tags = entity.tags,
                metadata = entity.metadata
            )
        }
    }
    
    override suspend fun clearAllRecentFiles() {
        // Delete all non-favorite files
        recentFilesDao.deleteOldFiles(System.currentTimeMillis() + 1000L) 
    }
    
    override suspend fun toggleFavorite(fileId: String) {
        val entity = recentFilesDao.getRecentFileById(fileId)
        entity?.let {
            recentFilesDao.updateRecentFile(it.copy(isFavorite = !it.isFavorite))
        }
    }
    
    override fun getFavoriteFiles(): Flow<List<RecentFile>> {
        return recentFilesDao.getFavoriteFiles().map { entities ->
            entities.map { entity ->
                RecentFile(
                    id = entity.id,
                    uri = entity.uri,
                    title = entity.title,
                    duration = entity.duration,
                    lastPosition = entity.lastPosition,
                    lastPlayed = entity.lastPlayedTime,
                    thumbnailPath = entity.thumbnailPath,
                    fileSize = entity.fileSize,
                    mimeType = entity.mimeType,
                    isCloudFile = entity.isCloudFile,
                    cloudProvider = entity.cloudProvider,
                    cloudFileId = entity.cloudFileId,
                    playCount = entity.playCount,
                    isFavorite = entity.isFavorite,
                    tags = entity.tags,
                    metadata = entity.metadata
                )
            }
        }
    }
}