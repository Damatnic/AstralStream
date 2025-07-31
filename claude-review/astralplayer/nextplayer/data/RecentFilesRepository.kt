package com.astralplayer.nextplayer.data

import com.astralplayer.nextplayer.data.database.RecentFileEntity
import com.astralplayer.nextplayer.data.database.RecentFilesDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

class RecentFilesRepository constructor(
    private val recentFilesDao: RecentFilesDao
) {
    
    fun getRecentFiles(): Flow<List<RecentFile>> {
        return recentFilesDao.getAllRecentFiles().map { entities ->
            entities.map { it.toRecentFile() }
        }
    }
    
    fun getFavoriteFiles(): Flow<List<RecentFile>> {
        return recentFilesDao.getFavoriteFiles().map { entities ->
            entities.map { it.toRecentFile() }
        }
    }
    
    fun getCloudFiles(): Flow<List<RecentFile>> {
        return recentFilesDao.getCloudFiles().map { entities ->
            entities.map { it.toRecentFile() }
        }
    }
    
    fun searchFiles(query: String): Flow<List<RecentFile>> {
        return recentFilesDao.searchFiles(query).map { entities ->
            entities.map { it.toRecentFile() }
        }
    }
    
    suspend fun addRecentFile(
        uri: String,
        title: String,
        duration: Long,
        lastPosition: Long = 0,
        thumbnailPath: String? = null,
        fileSize: Long = 0L,
        mimeType: String? = null,
        isCloudFile: Boolean = false,
        cloudProvider: String? = null,
        cloudFileId: String? = null
    ) {
        val entity = RecentFileEntity(
            id = UUID.randomUUID().toString(),
            uri = uri,
            title = title,
            duration = duration,
            lastPosition = lastPosition,
            lastPlayedTime = System.currentTimeMillis(),
            thumbnailPath = thumbnailPath,
            fileSize = fileSize,
            mimeType = mimeType,
            isCloudFile = isCloudFile,
            cloudProvider = cloudProvider,
            cloudFileId = cloudFileId,
            playCount = 1
        )
        recentFilesDao.insertRecentFile(entity)
    }
    
    suspend fun updateRecentFile(recentFile: RecentFile) {
        val entity = recentFile.toEntity()
        recentFilesDao.updateRecentFile(entity)
    }
    
    suspend fun updateLastPosition(id: String, position: Long) {
        recentFilesDao.updateLastPosition(id, position)
    }
    
    suspend fun incrementPlayCount(id: String) {
        recentFilesDao.incrementPlayCount(id)
    }
    
    suspend fun toggleFavorite(id: String) {
        val entity = recentFilesDao.getRecentFileById(id)
        entity?.let {
            val updated = it.copy(isFavorite = !it.isFavorite)
            recentFilesDao.updateRecentFile(updated)
        }
    }
    
    suspend fun removeRecentFile(id: String) {
        val entity = recentFilesDao.getRecentFileById(id)
        entity?.let {
            recentFilesDao.deleteRecentFile(it)
        }
    }
    
    suspend fun clearAllHistory() {
        // Delete all non-favorite files older than now
        recentFilesDao.deleteOldFiles(System.currentTimeMillis())
    }
    
    suspend fun clearOldHistory(daysToKeep: Int = 30) {
        val cutoffTime = System.currentTimeMillis() - (daysToKeep * 24 * 60 * 60 * 1000L)
        recentFilesDao.deleteOldFiles(cutoffTime)
    }
    
    suspend fun getRecentFileByUri(uri: String): RecentFile? {
        return recentFilesDao.getRecentFileByUri(uri)?.toRecentFile()
    }
    
    suspend fun getRecentFileById(id: String): RecentFile? {
        return recentFilesDao.getRecentFileById(id)?.toRecentFile()
    }
}

// Extension functions to convert between entities and domain models
private fun RecentFileEntity.toRecentFile(): RecentFile {
    return RecentFile(
        id = id,
        uri = uri,
        title = title,
        duration = duration,
        lastPosition = lastPosition,
        lastPlayed = lastPlayedTime
    )
}

private fun RecentFile.toEntity(): RecentFileEntity {
    return RecentFileEntity(
        id = id,
        uri = uri,
        title = title,
        duration = duration,
        lastPosition = lastPosition,
        lastPlayedTime = lastPlayed
    )
}