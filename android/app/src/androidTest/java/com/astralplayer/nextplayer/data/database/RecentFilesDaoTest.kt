package com.astralplayer.nextplayer.data.database

import com.astralplayer.nextplayer.BaseIntegrationTest
import kotlinx.coroutines.flow.first
import org.junit.Assert.*
import org.junit.Test

class RecentFilesDaoTest : BaseIntegrationTest() {
    
    private val dao by lazy { database.recentFilesDao() }
    
    @Test
    fun insertAndGetRecentFile() = runTest {
        // Given
        val recentFile = RecentFileEntity(
            id = "test-id",
            uri = "content://test.mp4",
            title = "Test Video",
            duration = 120000L,
            lastPosition = 30000L,
            lastPlayedTime = System.currentTimeMillis()
        )
        
        // When
        dao.insertRecentFile(recentFile)
        val result = dao.getRecentFileById("test-id")
        
        // Then
        assertNotNull(result)
        assertEquals("test-id", result?.id)
        assertEquals("Test Video", result?.title)
        assertEquals(120000L, result?.duration)
        assertEquals(30000L, result?.lastPosition)
    }
    
    @Test
    fun getAllRecentFilesOrderedByLastPlayed() = runTest {
        // Given
        val currentTime = System.currentTimeMillis()
        val file1 = RecentFileEntity(
            id = "1",
            uri = "content://test1.mp4",
            title = "Video 1",
            duration = 120000L,
            lastPlayedTime = currentTime - 2000L // Older
        )
        val file2 = RecentFileEntity(
            id = "2",
            uri = "content://test2.mp4",
            title = "Video 2",
            duration = 180000L,
            lastPlayedTime = currentTime - 1000L // Newer
        )
        
        // When
        dao.insertRecentFile(file1)
        dao.insertRecentFile(file2)
        val result = dao.getAllRecentFiles().first()
        
        // Then
        assertEquals(2, result.size)
        assertEquals("Video 2", result[0].title) // Should be first (most recent)
        assertEquals("Video 1", result[1].title) // Should be second (older)
    }
    
    @Test
    fun getFavoriteFiles() = runTest {
        // Given
        val favoriteFile = RecentFileEntity(
            id = "fav-1",
            uri = "content://favorite.mp4",
            title = "Favorite Video",
            duration = 120000L,
            isFavorite = true
        )
        val normalFile = RecentFileEntity(
            id = "normal-1",
            uri = "content://normal.mp4",
            title = "Normal Video",
            duration = 120000L,
            isFavorite = false
        )
        
        // When
        dao.insertRecentFile(favoriteFile)
        dao.insertRecentFile(normalFile)
        val result = dao.getFavoriteFiles().first()
        
        // Then
        assertEquals(1, result.size)
        assertEquals("Favorite Video", result[0].title)
        assertTrue(result[0].isFavorite)
    }
    
    @Test
    fun getCloudFiles() = runTest {
        // Given
        val cloudFile = RecentFileEntity(
            id = "cloud-1",
            uri = "https://drive.google.com/file/123",
            title = "Cloud Video",
            duration = 120000L,
            isCloudFile = true,
            cloudProvider = "google_drive"
        )
        val localFile = RecentFileEntity(
            id = "local-1",
            uri = "content://local.mp4",
            title = "Local Video",
            duration = 120000L,
            isCloudFile = false
        )
        
        // When
        dao.insertRecentFile(cloudFile)
        dao.insertRecentFile(localFile)
        val result = dao.getCloudFiles().first()
        
        // Then
        assertEquals(1, result.size)
        assertEquals("Cloud Video", result[0].title)
        assertTrue(result[0].isCloudFile)
        assertEquals("google_drive", result[0].cloudProvider)
    }
    
    @Test
    fun searchFiles() = runTest {
        // Given
        val file1 = RecentFileEntity(
            id = "1",
            uri = "content://action.mp4",
            title = "Action Movie",
            duration = 120000L,
            tags = "action,thriller"
        )
        val file2 = RecentFileEntity(
            id = "2",
            uri = "content://comedy.mp4",
            title = "Comedy Show",
            duration = 180000L,
            tags = "comedy,funny"
        )
        val file3 = RecentFileEntity(
            id = "3",
            uri = "content://documentary.mp4",
            title = "Nature Documentary",
            duration = 240000L,
            tags = "nature,educational"
        )
        
        // When
        dao.insertRecentFile(file1)
        dao.insertRecentFile(file2)
        dao.insertRecentFile(file3)
        
        val titleSearchResult = dao.searchFiles("Action").first()
        val tagSearchResult = dao.searchFiles("comedy").first()
        
        // Then
        assertEquals(1, titleSearchResult.size)
        assertEquals("Action Movie", titleSearchResult[0].title)
        
        assertEquals(1, tagSearchResult.size)
        assertEquals("Comedy Show", tagSearchResult[0].title)
    }
    
    @Test
    fun updateLastPosition() = runTest {
        // Given
        val recentFile = RecentFileEntity(
            id = "test-id",
            uri = "content://test.mp4",
            title = "Test Video",
            duration = 120000L,
            lastPosition = 30000L
        )
        dao.insertRecentFile(recentFile)
        
        // When
        dao.updateLastPosition("test-id", 60000L)
        val result = dao.getRecentFileById("test-id")
        
        // Then
        assertNotNull(result)
        assertEquals(60000L, result?.lastPosition)
    }
    
    @Test
    fun incrementPlayCount() = runTest {
        // Given
        val recentFile = RecentFileEntity(
            id = "test-id",
            uri = "content://test.mp4",
            title = "Test Video",
            duration = 120000L,
            playCount = 5
        )
        dao.insertRecentFile(recentFile)
        
        // When
        dao.incrementPlayCount("test-id")
        val result = dao.getRecentFileById("test-id")
        
        // Then
        assertNotNull(result)
        assertEquals(6, result?.playCount)
    }
    
    @Test
    fun deleteRecentFile() = runTest {
        // Given
        val recentFile = RecentFileEntity(
            id = "test-id",
            uri = "content://test.mp4",
            title = "Test Video",
            duration = 120000L
        )
        dao.insertRecentFile(recentFile)
        
        // When
        dao.deleteRecentFile(recentFile)
        val result = dao.getRecentFileById("test-id")
        
        // Then
        assertNull(result)
    }
    
    @Test
    fun deleteOldFiles() = runTest {
        // Given
        val currentTime = System.currentTimeMillis()
        val oldFile = RecentFileEntity(
            id = "old",
            uri = "content://old.mp4",
            title = "Old Video",
            duration = 120000L,
            lastPlayedTime = currentTime - 10000L, // Old
            isFavorite = false
        )
        val recentFile = RecentFileEntity(
            id = "recent",
            uri = "content://recent.mp4",
            title = "Recent Video",
            duration = 120000L,
            lastPlayedTime = currentTime - 1000L, // Recent
            isFavorite = false
        )
        val favoriteFIle = RecentFileEntity(
            id = "favorite",
            uri = "content://favorite.mp4",
            title = "Favorite Video",
            duration = 120000L,
            lastPlayedTime = currentTime - 10000L, // Old but favorite
            isFavorite = true
        )
        
        dao.insertRecentFile(oldFile)
        dao.insertRecentFile(recentFile)
        dao.insertRecentFile(favoriteFIle)
        
        // When - delete files older than 5 seconds ago
        dao.deleteOldFiles(currentTime - 5000L)
        val result = dao.getAllRecentFiles().first()
        
        // Then - only recent file and favorite should remain
        assertEquals(2, result.size)
        val titles = result.map { it.title }
        assertTrue(titles.contains("Recent Video"))
        assertTrue(titles.contains("Favorite Video"))
        assertFalse(titles.contains("Old Video"))
    }
}