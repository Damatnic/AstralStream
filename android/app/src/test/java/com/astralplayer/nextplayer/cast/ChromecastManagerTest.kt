package com.astralplayer.nextplayer.cast

import android.content.Context
import android.net.Uri
import com.google.android.gms.cast.*
import com.google.android.gms.cast.framework.CastContext
import com.google.android.gms.cast.framework.CastSession
import com.google.android.gms.cast.framework.SessionManager
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

class ChromecastManagerTest {

    private lateinit var mockContext: Context
    private lateinit var mockCastContext: CastContext
    private lateinit var mockSessionManager: SessionManager
    private lateinit var mockCastSession: CastSession
    private lateinit var mockRemoteMediaClient: RemoteMediaClient
    private lateinit var chromecastManager: ChromecastManager

    @Before
    fun setup() {
        mockContext = mockk(relaxed = true)
        mockCastContext = mockk(relaxed = true)
        mockSessionManager = mockk(relaxed = true)
        mockCastSession = mockk(relaxed = true)
        mockRemoteMediaClient = mockk(relaxed = true)
        
        // Mock static methods
        mockkStatic(CastContext::class)
        every { CastContext.getSharedInstance(any()) } returns mockCastContext
        every { mockCastContext.sessionManager } returns mockSessionManager
        every { mockSessionManager.currentCastSession } returns mockCastSession
        every { mockCastSession.remoteMediaClient } returns mockRemoteMediaClient
        
        chromecastManager = ChromecastManager(mockContext)
    }

    @Test
    fun `test initial cast state is not connected`() = runTest {
        every { mockSessionManager.currentCastSession } returns null
        
        val newManager = ChromecastManager(mockContext)
        assertEquals(ChromecastManager.CastState.NOT_CONNECTED, newManager.castState.value)
    }

    @Test
    fun `test isConnected returns true when cast session exists`() = runTest {
        every { mockSessionManager.currentCastSession } returns mockCastSession
        
        assertTrue(chromecastManager.isConnected())
    }

    @Test
    fun `test isConnected returns false when no cast session`() = runTest {
        every { mockSessionManager.currentCastSession } returns null
        
        assertFalse(chromecastManager.isConnected())
    }

    @Test
    fun `test cast state updates when session starts`() = runTest {
        // Simulate session starting
        every { mockSessionManager.currentCastSession } returns mockCastSession
        
        // Create new manager to trigger state update
        val newManager = ChromecastManager(mockContext)
        
        // Verify slot is captured for session listener
        verify { mockSessionManager.addSessionManagerListener(any(), any()) }
    }

    @Test
    fun `test createMediaInfo creates correct media info`() {
        val uri = mockk<Uri>()
        every { uri.toString() } returns "https://example.com/video.mp4"
        
        val mediaInfo = ChromecastManager.createMediaInfo(
            videoUri = uri,
            title = "Test Video",
            description = "Test Description",
            thumbnailUrl = "https://example.com/thumb.jpg"
        )
        
        assertEquals("https://example.com/video.mp4", mediaInfo.contentUrl)
        assertEquals("video/mp4", mediaInfo.contentType)
        assertEquals("Test Video", mediaInfo.metadata.getString(MediaMetadata.KEY_TITLE))
        assertEquals("Test Description", mediaInfo.metadata.getString(MediaMetadata.KEY_SUBTITLE))
    }

    @Test
    fun `test createMediaInfo with default values`() {
        val uri = mockk<Uri>()
        every { uri.toString() } returns "https://example.com/video.mp4"
        
        val mediaInfo = ChromecastManager.createMediaInfo(
            videoUri = uri,
            title = "Test Video"
        )
        
        assertEquals("https://example.com/video.mp4", mediaInfo.contentUrl)
        assertEquals("video/mp4", mediaInfo.contentType)
        assertEquals("Test Video", mediaInfo.metadata.getString(MediaMetadata.KEY_TITLE))
        assertNull(mediaInfo.metadata.getString(MediaMetadata.KEY_SUBTITLE))
    }

    @Test
    fun `test getContentType returns correct MIME type for different extensions`() {
        assertEquals("video/mp4", ChromecastManager.getContentType("video.mp4"))
        assertEquals("video/webm", ChromecastManager.getContentType("video.webm"))
        assertEquals("video/x-matroska", ChromecastManager.getContentType("video.mkv"))
        assertEquals("video/x-msvideo", ChromecastManager.getContentType("video.avi"))
        assertEquals("video/quicktime", ChromecastManager.getContentType("video.mov"))
        assertEquals("video/mp4", ChromecastManager.getContentType("video.m4v"))
        assertEquals("video/mp4", ChromecastManager.getContentType("unknown.xyz")) // default
    }

    @Test
    fun `test cast state enum values`() {
        val states = ChromecastManager.CastState.values()
        
        assertTrue(states.contains(ChromecastManager.CastState.NOT_CONNECTED))
        assertTrue(states.contains(ChromecastManager.CastState.CONNECTING))
        assertTrue(states.contains(ChromecastManager.CastState.CONNECTED))
        assertTrue(states.contains(ChromecastManager.CastState.ERROR))
        assertEquals(4, states.size)
    }

    @Test
    fun `test media info builder uses correct metadata type`() {
        val uri = mockk<Uri>()
        every { uri.toString() } returns "https://example.com/video.mp4"
        
        val mediaInfo = ChromecastManager.createMediaInfo(
            videoUri = uri,
            title = "Test Video"
        )
        
        assertEquals(MediaMetadata.MEDIA_TYPE_MOVIE, mediaInfo.metadata.mediaType)
    }

    @Test
    fun `test media info with streaming detection`() {
        val uri = mockk<Uri>()
        every { uri.toString() } returns "https://example.com/stream.m3u8"
        
        val mediaInfo = ChromecastManager.createMediaInfo(
            videoUri = uri,
            title = "Live Stream"
        )
        
        assertEquals("application/x-mpegURL", ChromecastManager.getContentType("stream.m3u8"))
    }

    @Test
    fun `test media info with thumbnail URL`() {
        val uri = mockk<Uri>()
        every { uri.toString() } returns "https://example.com/video.mp4"
        
        val mediaInfo = ChromecastManager.createMediaInfo(
            videoUri = uri,
            title = "Test Video",
            thumbnailUrl = "https://example.com/thumb.jpg"
        )
        
        val images = mediaInfo.metadata.images
        assertEquals(1, images.size)
        assertEquals("https://example.com/thumb.jpg", images[0].url.toString())
    }

    @Test
    fun `test media info without thumbnail URL`() {
        val uri = mockk<Uri>()
        every { uri.toString() } returns "https://example.com/video.mp4"
        
        val mediaInfo = ChromecastManager.createMediaInfo(
            videoUri = uri,
            title = "Test Video"
        )
        
        val images = mediaInfo.metadata.images
        assertEquals(0, images.size)
    }

    @Test
    fun `test content type detection is case insensitive`() {
        assertEquals("video/mp4", ChromecastManager.getContentType("VIDEO.MP4"))
        assertEquals("video/webm", ChromecastManager.getContentType("Video.WEBM"))
        assertEquals("video/x-matroska", ChromecastManager.getContentType("test.MKV"))
    }

    @Test
    fun `test content type with complex filename`() {
        assertEquals("video/mp4", ChromecastManager.getContentType("My Video File (2024).mp4"))
        assertEquals("video/webm", ChromecastManager.getContentType("path/to/video.file.webm"))
        assertEquals("video/x-matroska", ChromecastManager.getContentType("/storage/emulated/0/Movies/video.mkv"))
    }
}