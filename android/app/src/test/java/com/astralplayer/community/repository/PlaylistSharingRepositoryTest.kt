package com.astralplayer.community.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.astralplayer.astralstream.data.database.AstralStreamDatabase
import com.astralplayer.community.api.*
import com.astralplayer.community.dao.*
import com.astralplayer.community.data.*
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.*
import kotlin.test.*

@RunWith(RobolectricTestRunner::class)
class PlaylistSharingRepositoryTest {

    @Mock
    private lateinit var mockApiManager: CommunityApiManager
    
    private lateinit var database: AstralStreamDatabase
    private lateinit var playlistSharingRepository: PlaylistSharingRepository
    private lateinit var context: Context

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, AstralStreamDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        
        playlistSharingRepository = PlaylistSharingRepository(
            apiManager = mockApiManager,
            database = database,
            context = context
        )
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun `share playlist creates share record successfully`() = runBlocking {
        val request = SharePlaylistRequest(
            playlistId = "test_playlist_1",
            title = "My Test Playlist",
            description = "A test playlist for sharing",
            videoIds = listOf("video1", "video2", "video3"),
            isPublic = true,
            password = null,
            expirationDays = 30,
            allowDownloads = true
        )
        
        val mockResponse = SharePlaylistResponse(
            shareId = "share_123",
            shareUrl = "https://astralstream.com/shared/share_123",
            qrCodeUrl = "https://astralstream.com/qr/share_123",
            expiresAt = System.currentTimeMillis() + (30 * 24 * 60 * 60 * 1000L)
        )
        
        whenever(mockApiManager.sharePlaylist(request)).thenReturn(
            ApiResponse.Success(mockResponse)
        )
        
        val result = playlistSharingRepository.sharePlaylist(
            playlistId = request.playlistId,
            title = request.title,
            description = request.description,
            videoFiles = emptyList(), // Mock empty for test
            isPublic = request.isPublic,
            password = request.password,
            expirationDays = request.expirationDays,
            allowDownloads = request.allowDownloads
        )
        
        assertTrue(result.isSuccess)
        val shareResult = result.getOrNull()
        assertNotNull(shareResult)
        assertEquals("share_123", shareResult.shareId)
        assertEquals("https://astralstream.com/shared/share_123", shareResult.shareUrl)
        
        verify(mockApiManager).sharePlaylist(any())
        
        // Verify local database record
        val sharedPlaylist = database.sharedPlaylistDao().getSharedPlaylist("share_123")
        assertNotNull(sharedPlaylist)
        assertEquals(request.playlistId, sharedPlaylist.originalPlaylistId)
        assertEquals(request.title, sharedPlaylist.title)
    }

    @Test
    fun `share playlist with password protection works correctly`() = runBlocking {
        val request = SharePlaylistRequest(
            playlistId = "secure_playlist",
            title = "Protected Playlist",
            description = "Password protected playlist",
            videoIds = listOf("video1"),
            isPublic = false,
            password = "secret123",
            expirationDays = 7,
            allowDownloads = false
        )
        
        val mockResponse = SharePlaylistResponse(
            shareId = "secure_share_456",
            shareUrl = "https://astralstream.com/shared/secure_share_456",
            qrCodeUrl = "https://astralstream.com/qr/secure_share_456",
            expiresAt = System.currentTimeMillis() + (7 * 24 * 60 * 60 * 1000L)
        )
        
        whenever(mockApiManager.sharePlaylist(request)).thenReturn(
            ApiResponse.Success(mockResponse)
        )
        
        val result = playlistSharingRepository.sharePlaylist(
            playlistId = request.playlistId,
            title = request.title,
            description = request.description,
            videoFiles = emptyList(),
            isPublic = false,
            password = "secret123",
            expirationDays = 7,
            allowDownloads = false
        )
        
        assertTrue(result.isSuccess)
        
        // Verify password protection in database
        val sharedPlaylist = database.sharedPlaylistDao().getSharedPlaylist("secure_share_456")
        assertNotNull(sharedPlaylist)
        assertFalse(sharedPlaylist.isPublic)
        assertTrue(sharedPlaylist.hasPassword)
        assertFalse(sharedPlaylist.allowDownloads)
    }

    @Test
    fun `import shared playlist downloads and stores playlist`() = runBlocking {
        val shareId = "import_test_share"
        val mockPlaylist = SharedPlaylistData(
            shareId = shareId,
            title = "Imported Playlist",
            description = "Test imported playlist",
            videoCount = 2,
            duration = 120000L,
            createdBy = "TestUser",
            createdAt = System.currentTimeMillis(),
            videos = listOf(
                SharedVideoInfo("video1", "Video 1", 60000L, "https://example.com/video1.mp4"),
                SharedVideoInfo("video2", "Video 2", 60000L, "https://example.com/video2.mp4")
            ),
            thumbnailUrl = "https://example.com/thumb.jpg",
            tags = listOf("test", "import"),
            isPublic = true,
            allowDownloads = true,
            expiresAt = System.currentTimeMillis() + 86400000L
        )
        
        whenever(mockApiManager.getSharedPlaylist(shareId, null)).thenReturn(
            ApiResponse.Success(mockPlaylist)
        )
        
        val result = playlistSharingRepository.importSharedPlaylist(shareId, null)
        
        assertTrue(result.isSuccess)
        val importResult = result.getOrNull()
        assertNotNull(importResult)
        assertEquals(shareId, importResult.shareId)
        assertEquals("Imported Playlist", importResult.title)
        assertEquals(2, importResult.videoCount)
        
        verify(mockApiManager).getSharedPlaylist(shareId, null)
        
        // Verify playlist was downloaded and stored locally
        val downloadedPlaylist = database.sharedPlaylistDao().getSharedPlaylist(shareId)
        assertNotNull(downloadedPlaylist)
        assertEquals("Imported Playlist", downloadedPlaylist.title)
        assertEqual("TestUser", downloadedPlaylist.createdBy)
    }

    @Test
    fun `get trending playlists returns popular content`() = runBlocking {
        val mockTrendingPlaylists = listOf(
            TrendingPlaylist(
                shareId = "trending1",
                title = "Trending Playlist 1",
                description = "Popular playlist",
                videoCount = 5,
                duration = 300000L,
                createdBy = "PopularUser",
                viewCount = 1000,
                downloadCount = 100,
                rating = 4.5f,
                thumbnailUrl = "https://example.com/thumb1.jpg",
                tags = listOf("trending", "popular"),
                createdAt = System.currentTimeMillis() - 86400000L
            ),
            TrendingPlaylist(
                shareId = "trending2", 
                title = "Trending Playlist 2",
                description = "Another popular playlist",
                videoCount = 3,
                duration = 180000L,
                createdBy = "AnotherUser",
                viewCount = 750,
                downloadCount = 80,
                rating = 4.2f,
                thumbnailUrl = "https://example.com/thumb2.jpg", 
                tags = listOf("trending", "music"),
                createdAt = System.currentTimeMillis() - 172800000L
            )
        )
        
        whenever(mockApiManager.getTrendingPlaylists(limit = 20, category = null)).thenReturn(
            ApiResponse.Success(mockTrendingPlaylists)
        )
        
        val result = playlistSharingRepository.getTrendingPlaylists(limit = 20)
        
        assertTrue(result.isSuccess)
        val trendingPlaylists = result.getOrNull()
        assertNotNull(trendingPlaylists)
        assertEquals(2, trendingPlaylists.size)
        
        val firstPlaylist = trendingPlaylists[0]
        assertEquals("trending1", firstPlaylist.shareId)
        assertEquals("Trending Playlist 1", firstPlaylist.title)
        assertEquals(1000, firstPlaylist.viewCount)
        assertEquals(4.5f, firstPlaylist.rating)
        
        verify(mockApiManager).getTrendingPlaylists(20, null)
    }

    @Test
    fun `search shared playlists finds matching results`() = runBlocking {
        val query = "music"
        val mockSearchResults = listOf(
            SearchResult(
                shareId = "music_playlist_1",
                title = "Best Music Collection",
                description = "Top music videos compilation",
                videoCount = 10,
                duration = 600000L,
                createdBy = "MusicLover",
                viewCount = 500,
                rating = 4.8f,
                thumbnailUrl = "https://example.com/music1.jpg",
                tags = listOf("music", "compilation"),
                relevanceScore = 0.95f,
                createdAt = System.currentTimeMillis() - 432000000L
            )
        )
        
        whenever(mockApiManager.searchSharedPlaylists(
            query = query,
            category = null,
            sortBy = "relevance",
            limit = 20
        )).thenReturn(ApiResponse.Success(mockSearchResults))
        
        val result = playlistSharingRepository.searchSharedPlaylists(
            query = query,
            category = null,
            sortBy = "relevance",
            limit = 20
        )
        
        assertTrue(result.isSuccess)
        val searchResults = result.getOrNull()
        assertNotNull(searchResults)
        assertEquals(1, searchResults.size)
        
        val firstResult = searchResults[0]
        assertEquals("music_playlist_1", firstResult.shareId)
        assertEquals("Best Music Collection", firstResult.title)
        assertTrue(firstResult.title.contains("Music"))
        assertEquals(0.95f, firstResult.relevanceScore)
        
        verify(mockApiManager).searchSharedPlaylists(query, null, "relevance", 20)
    }

    @Test
    fun `rate playlist updates rating successfully`() = runBlocking {
        val shareId = "rate_test_playlist"
        val rating = 4.5f
        val comment = "Great playlist!"
        
        whenever(mockApiManager.ratePlaylist(shareId, rating, comment)).thenReturn(
            ApiResponse.Success(Unit)
        )
        
        val result = playlistSharingRepository.ratePlaylist(shareId, rating, comment)
        
        assertTrue(result.isSuccess)
        verify(mockApiManager).ratePlaylist(shareId, rating, comment)
        
        // Verify rating is stored locally
        val ratingRecord = database.playlistRatingDao().getRating(shareId)
        assertNotNull(ratingRecord)
        assertEquals(rating, ratingRecord.rating)
        assertEquals(comment, ratingRecord.comment)
    }

    @Test
    fun `get my shared playlists returns user's playlists`() = runBlocking {
        val mockUserPlaylists = listOf(
            UserSharedPlaylist(
                shareId = "user_playlist_1",
                originalPlaylistId = "original_1",
                title = "My Shared Playlist",
                description = "Playlist I shared",
                videoCount = 5,
                isPublic = true,
                hasPassword = false,
                allowDownloads = true,
                viewCount = 50,
                downloadCount = 10,
                createdAt = System.currentTimeMillis() - 86400000L,
                expiresAt = System.currentTimeMillis() + 86400000L,
                shareUrl = "https://astralstream.com/shared/user_playlist_1"
            )
        )
        
        whenever(mockApiManager.getUserSharedPlaylists()).thenReturn(
            ApiResponse.Success(mockUserPlaylists)
        )
        
        val result = playlistSharingRepository.getMySharedPlaylists()
        
        assertTrue(result.isSuccess)
        val userPlaylists = result.getOrNull()
        assertNotNull(userPlaylists)
        assertEquals(1, userPlaylists.size)
        
        val playlist = userPlaylists[0]
        assertEquals("user_playlist_1", playlist.shareId)
        assertEquals("My Shared Playlist", playlist.title)
        assertEquals(50, playlist.viewCount)
        assertTrue(playlist.isPublic)
        
        verify(mockApiManager).getUserSharedPlaylists()
    }

    @Test
    fun `delete shared playlist removes from remote and local`() = runBlocking {
        val shareId = "delete_test_playlist"
        
        // First add a playlist to database
        val sharedPlaylist = SharedPlaylistEntity(
            shareId = shareId,
            originalPlaylistId = "original",
            title = "To Delete",
            description = "Will be deleted",
            videoIds = "video1,video2",
            videoCount = 2,
            duration = 120000L,
            isPublic = true,
            hasPassword = false,
            allowDownloads = true,
            createdBy = "TestUser",
            createdAt = System.currentTimeMillis(),
            expiresAt = System.currentTimeMillis() + 86400000L,
            shareUrl = "https://example.com/share",
            qrCodeUrl = "https://example.com/qr",
            viewCount = 0,
            downloadCount = 0
        )
        database.sharedPlaylistDao().insertSharedPlaylist(sharedPlaylist)
        
        whenever(mockApiManager.deleteSharedPlaylist(shareId)).thenReturn(
            ApiResponse.Success(Unit)
        )
        
        val result = playlistSharingRepository.deleteSharedPlaylist(shareId)
        
        assertTrue(result.isSuccess)
        verify(mockApiManager).deleteSharedPlaylist(shareId)
        
        // Verify removed from local database
        val deletedPlaylist = database.sharedPlaylistDao().getSharedPlaylist(shareId)
        assertNull(deletedPlaylist)
    }

    @Test
    fun `handle network error gracefully`() = runBlocking {
        val request = SharePlaylistRequest(
            playlistId = "network_error_test",
            title = "Network Error Test",
            description = "This will fail",
            videoIds = listOf("video1"),
            isPublic = true,
            password = null,
            expirationDays = 30,
            allowDownloads = true
        )
        
        whenever(mockApiManager.sharePlaylist(request)).thenReturn(
            ApiResponse.Error("Network connection failed", 503)
        )
        
        val result = playlistSharingRepository.sharePlaylist(
            playlistId = request.playlistId,
            title = request.title,
            description = request.description,
            videoFiles = emptyList(),
            isPublic = request.isPublic,
            password = request.password,
            expirationDays = request.expirationDays,
            allowDownloads = request.allowDownloads
        )
        
        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertNotNull(exception)
        assertTrue(exception.message?.contains("Network") == true)
    }

    private fun assertEqual(expected: String, actual: String) {
        assertEquals(expected, actual)
    }
}