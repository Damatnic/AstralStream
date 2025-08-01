package com.astralplayer.cloud

import android.content.Context
import android.content.SharedPreferences
import com.dropbox.core.DbxRequestConfig
import com.dropbox.core.android.Auth
import com.dropbox.core.oauth.DbxCredential
import com.dropbox.core.v2.DbxClientV2
import com.dropbox.core.v2.files.*
import io.mockk.*
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import java.io.InputStream
import java.util.Date

@ExperimentalCoroutinesApi
class DropboxServiceTest {
    
    @MockK
    private lateinit var context: Context
    
    @MockK
    private lateinit var sharedPreferences: SharedPreferences
    
    @MockK
    private lateinit var editor: SharedPreferences.Editor
    
    @MockK
    private lateinit var dbxClient: DbxClientV2
    
    @MockK
    private lateinit var filesRequestBuilder: DbxUserFilesRequests
    
    @MockK
    private lateinit var listFolderResult: ListFolderResult
    
    @MockK
    private lateinit var fileMetadata: FileMetadata
    
    @MockK
    private lateinit var folderMetadata: FolderMetadata
    
    private lateinit var dropboxService: DropboxService
    
    @Before
    fun setup() {
        MockKAnnotations.init(this)
        
        mockkStatic(Auth::class)
        
        every { context.getSharedPreferences("dropbox_auth", Context.MODE_PRIVATE) } returns sharedPreferences
        every { sharedPreferences.edit() } returns editor
        every { editor.putString(any(), any()) } returns editor
        every { editor.putLong(any(), any()) } returns editor
        every { editor.apply() } just Runs
        every { editor.clear() } returns editor
        
        dropboxService = DropboxService(context)
    }
    
    @Test
    fun `authenticate should start OAuth2 authentication`() = runTest {
        // Given
        every { Auth.startOAuth2Authentication(context, any()) } just Runs
        
        // When
        val result = dropboxService.authenticate()
        
        // Then
        assertTrue(result)
        verify { Auth.startOAuth2Authentication(context, any()) }
    }
    
    @Test
    fun `authenticate should return false on exception`() = runTest {
        // Given
        every { Auth.startOAuth2Authentication(context, any()) } throws RuntimeException("Auth failed")
        
        // When
        val result = dropboxService.authenticate()
        
        // Then
        assertFalse(result)
    }
    
    @Test
    fun `isAuthenticated should return true when credentials exist`() = runTest {
        // Given
        every { sharedPreferences.getString("access_token", null) } returns "test_token"
        every { sharedPreferences.getString("refresh_token", null) } returns "test_refresh"
        every { sharedPreferences.getLong("expires_at", 0) } returns System.currentTimeMillis() + 3600000
        
        // When
        val result = dropboxService.isAuthenticated()
        
        // Then
        assertTrue(result)
    }
    
    @Test
    fun `isAuthenticated should return false when no credentials`() = runTest {
        // Given
        every { sharedPreferences.getString("access_token", null) } returns null
        
        // When
        val result = dropboxService.isAuthenticated()
        
        // Then
        assertFalse(result)
    }
    
    @Test
    fun `listFiles should return video files from folder`() = runTest {
        // Given
        setupAuthenticatedClient()
        
        val videoFile = mockk<FileMetadata> {
            every { id } returns "video123"
            every { name } returns "test.mp4"
            every { pathLower } returns "/videos/test.mp4"
            every { size } returns 1024000L
            every { clientModified } returns Date()
        }
        
        val nonVideoFile = mockk<FileMetadata> {
            every { name } returns "document.pdf"
        }
        
        val folder = mockk<FolderMetadata> {
            every { id } returns "folder123"
            every { name } returns "Videos"
            every { pathLower } returns "/videos"
        }
        
        every { dbxClient.files() } returns filesRequestBuilder
        every { filesRequestBuilder.listFolder(any()) } returns mockk {
            every { buildRequest() } returns mockk {
                every { get() } returns listFolderResult
            }
        }
        
        every { listFolderResult.entries } returns listOf(videoFile, nonVideoFile, folder)
        every { listFolderResult.hasMore } returns false
        every { listFolderResult.cursor } returns null
        
        // When
        val files = dropboxService.listFiles("/videos").first()
        
        // Then
        assertEquals(2, files.size) // Video file and folder
        assertTrue(files.any { it.name == "test.mp4" && !it.isDirectory })
        assertTrue(files.any { it.name == "Videos" && it.isDirectory })
    }
    
    @Test
    fun `getStreamingUrl should return temporary link`() = runTest {
        // Given
        setupAuthenticatedClient()
        val fileId = "file123"
        val expectedUrl = "https://dl.dropboxusercontent.com/temporary/file123"
        
        every { dbxClient.files() } returns filesRequestBuilder
        every { filesRequestBuilder.getTemporaryLink(fileId) } returns mockk {
            every { link } returns expectedUrl
        }
        
        // When
        val url = dropboxService.getStreamingUrl(fileId)
        
        // Then
        assertEquals(expectedUrl, url)
    }
    
    @Test
    fun `searchFiles should return matching video files`() = runTest {
        // Given
        setupAuthenticatedClient()
        val query = "vacation"
        
        val searchMatch = mockk<SearchMatchV2> {
            every { metadata } returns mockk {
                every { metadataValue } returns mockk<FileMetadata> {
                    every { id } returns "search123"
                    every { name } returns "vacation.mp4"
                    every { pathLower } returns "/videos/vacation.mp4"
                    every { size } returns 2048000L
                    every { clientModified } returns Date()
                }
            }
        }
        
        every { dbxClient.files() } returns filesRequestBuilder
        every { filesRequestBuilder.searchV2(query) } returns mockk {
            every { buildRequest() } returns mockk {
                every { get() } returns mockk {
                    every { matches } returns listOf(searchMatch)
                }
            }
        }
        
        // When
        val results = dropboxService.searchFiles(query).first()
        
        // Then
        assertEquals(1, results.size)
        assertEquals("vacation.mp4", results[0].name)
    }
    
    @Test
    fun `logout should clear credentials`() = runTest {
        // When
        dropboxService.logout()
        
        // Then
        verify { editor.clear() }
        verify { editor.apply() }
    }
    
    private fun setupAuthenticatedClient() {
        every { sharedPreferences.getString("access_token", null) } returns "test_token"
        every { sharedPreferences.getString("refresh_token", null) } returns "test_refresh"
        every { sharedPreferences.getLong("expires_at", 0) } returns System.currentTimeMillis() + 3600000
        
        // Mock the client initialization (this is simplified)
        mockkConstructor(DbxClientV2::class)
        every { anyConstructed<DbxClientV2>().files() } returns filesRequestBuilder
    }
}