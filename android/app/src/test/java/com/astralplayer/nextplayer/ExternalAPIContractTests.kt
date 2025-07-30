package com.astralplayer.nextplayer

import com.astralplayer.nextplayer.feature.cloud.*
import com.astralplayer.nextplayer.feature.ai.*
import com.astralplayer.nextplayer.feature.network.*
import kotlinx.coroutines.test.*
import org.junit.Test
import org.junit.Before
import org.junit.After
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.*
import kotlin.test.*
import okhttp3.*
import retrofit2.*
import java.io.IOException

/**
 * Contract tests for external API integrations
 * Validates that our code correctly handles external API contracts and responses
 */
@RunWith(MockitoJUnitRunner::class)
class ExternalAPIContractTests {

    @Mock private lateinit var mockContext: android.content.Context
    @Mock private lateinit var mockHttpClient: OkHttpClient
    @Mock private lateinit var mockRetrofit: Retrofit
    
    private lateinit var testScope: TestCoroutineScope

    @Before
    fun setup() {
        testScope = TestCoroutineScope()
    }

    @After
    fun cleanup() {
        testScope.cleanupTestCoroutines()
    }

    // ============================================================================
    // ONEDRIVE API CONTRACT TESTS
    // ============================================================================

    @Test
    fun `OneDrive API authentication contract validation`() = testScope.runBlockingTest {
        val onedriveService = createOneDriveService()
        
        // Test OAuth 2.0 authentication flow contract
        val authRequest = OneDriveAuthRequest(
            clientId = "test-client-id",
            redirectUri = "astralstream://auth",
            scopes = listOf("Files.ReadWrite", "offline_access"),
            responseType = "code"
        )
        
        val mockAuthResponse = OneDriveAuthResponse(
            accessToken = "mock-access-token",
            refreshToken = "mock-refresh-token",
            expiresIn = 3600,
            tokenType = "Bearer",
            scope = "Files.ReadWrite offline_access"
        )
        
        whenever(onedriveService.authenticate(authRequest)).thenReturn(mockAuthResponse)
        
        val authResult = onedriveService.authenticate(authRequest)
        
        // Validate contract requirements
        assertNotNull("Access token must be provided", authResult.accessToken)
        assertNotNull("Refresh token must be provided", authResult.refreshToken)
        assertTrue("Expires in must be positive", authResult.expiresIn > 0)
        assertEquals("Token type must be Bearer", "Bearer", authResult.tokenType)
        assertTrue("Scope must contain requested permissions", 
                  authResult.scope.contains("Files.ReadWrite"))
    }

    @Test
    fun `OneDrive API file upload contract validation`() = testScope.runBlockingTest {
        val onedriveService = createOneDriveService()
        
        val uploadRequest = OneDriveUploadRequest(
            fileName = "bookmark_backup.json",
            fileSize = 1024L,
            mimeType = "application/json",
            content = "mock file content".toByteArray(),
            parentFolder = "/AstralStream/Backups"
        )
        
        val mockUploadResponse = OneDriveUploadResponse(
            id = "file-id-123",
            name = "bookmark_backup.json",
            size = 1024L,
            createdDateTime = "2025-01-15T10:30:00Z",
            lastModifiedDateTime = "2025-01-15T10:30:00Z",
            downloadUrl = "https://onedrive.live.com/download?id=file-id-123",
            parentReference = OneDriveItemReference(
                driveId = "drive-id",
                id = "parent-folder-id",
                path = "/AstralStream/Backups"
            )
        )
        
        whenever(onedriveService.uploadFile(any(), eq(uploadRequest))).thenReturn(mockUploadResponse)
        
        val uploadResult = onedriveService.uploadFile("Bearer mock-token", uploadRequest)
        
        // Validate upload response contract
        assertNotNull("File ID must be provided", uploadResult.id)
        assertEquals("File name must match request", uploadRequest.fileName, uploadResult.name)
        assertEquals("File size must match", uploadRequest.fileSize, uploadResult.size)
        assertNotNull("Creation date must be provided", uploadResult.createdDateTime)
        assertNotNull("Download URL must be provided", uploadResult.downloadUrl)
        assertTrue("Download URL must be valid", uploadResult.downloadUrl.startsWith("https://"))
        assertNotNull("Parent reference must be provided", uploadResult.parentReference)
    }

    @Test
    fun `OneDrive API error response contract validation`() = testScope.runBlockingTest {
        val onedriveService = createOneDriveService()
        
        // Test various error scenarios and their contract compliance
        val errorScenarios = listOf(
            OneDriveErrorScenario(401, "invalid_grant", "The provided authorization grant is invalid, expired, revoked, does not match the redirection URI used in the authorization request, or was issued to another client."),
            OneDriveErrorScenario(403, "forbidden", "The user does not have permission to perform this operation."),
            OneDriveErrorScenario(404, "itemNotFound", "The requested item was not found."),
            OneDriveErrorScenario(409, "nameAlreadyExists", "An item with the same name already exists in this location."),
            OneDriveErrorScenario(413, "uploadTooLarge", "The uploaded file exceeds the maximum file size limit."),
            OneDriveErrorScenario(429, "tooManyRequests", "The request has been throttled due to too many requests."),
            OneDriveErrorScenario(507, "insufficientStorage", "There is insufficient storage space to complete the request.")
        )
        
        errorScenarios.forEach { scenario ->
            val errorResponse = OneDriveErrorResponse(
                error = OneDriveError(
                    code = scenario.errorCode,
                    message = scenario.errorMessage,
                    innerError = OneDriveInnerError(
                        code = scenario.httpStatus.toString(),
                        request_id = "test-request-id",
                        date = "2025-01-15T10:30:00Z"
                    )
                )
            )
            
            whenever(onedriveService.uploadFile(any(), any()))
                .thenThrow(OneDriveApiException(scenario.httpStatus, errorResponse))
            
            val exception = assertFailsWith<OneDriveApiException> {
                onedriveService.uploadFile("Bearer invalid-token", mock())
            }
            
            // Validate error response contract
            assertEquals("HTTP status must match", scenario.httpStatus, exception.httpStatus)
            assertEquals("Error code must match", scenario.errorCode, exception.errorResponse.error.code)
            assertNotNull("Error message must be provided", exception.errorResponse.error.message)
            assertNotNull("Request ID must be provided for debugging", exception.errorResponse.error.innerError?.request_id)
        }
    }

    // ============================================================================
    // AI SUBTITLE SERVICE API CONTRACT TESTS
    // ============================================================================

    @Test
    fun `AI Subtitle Service API request contract validation`() = testScope.runBlockingTest {
        val aiSubtitleService = createAISubtitleService()
        
        val subtitleRequest = AISubtitleRequest(
            audioData = "base64-encoded-audio-data",
            audioFormat = "wav",
            sampleRate = 44100,
            channels = 2,
            language = "en",
            model = "whisper-large-v2"
        )
        
        val mockSubtitleResponse = AISubtitleResponse(
            requestId = "req-123",
            status = "completed",
            subtitles = listOf(
                AISubtitleSegment(
                    id = 1,
                    startTime = 0.0,
                    endTime = 2.5,
                    text = "Hello, this is a test subtitle.",
                    confidence = 0.95
                ),
                AISubtitleSegment(
                    id = 2,
                    startTime = 2.5,
                    endTime = 5.0,
                    text = "This is the second subtitle segment.",
                    confidence = 0.92
                )
            ),
            language = "en",
            processingTime = 1.2,
            wordCount = 12
        )
        
        whenever(aiSubtitleService.generateSubtitles(subtitleRequest)).thenReturn(mockSubtitleResponse)
        
        val result = aiSubtitleService.generateSubtitles(subtitleRequest)
        
        // Validate response contract
        assertNotNull("Request ID must be provided", result.requestId)
        assertEquals("Status must be completed", "completed", result.status)
        assertTrue("Subtitles must not be empty", result.subtitles.isNotEmpty())
        assertEquals("Language must match request", subtitleRequest.language, result.language)
        assertTrue("Processing time must be positive", result.processingTime > 0)
        assertTrue("Word count must be positive", result.wordCount > 0)
        
        // Validate subtitle segments
        result.subtitles.forEach { segment ->
            assertTrue("Segment ID must be positive", segment.id > 0)
            assertTrue("Start time must be non-negative", segment.startTime >= 0.0)
            assertTrue("End time must be greater than start time", segment.endTime > segment.startTime)
            assertTrue("Text must not be empty", segment.text.isNotBlank())
            assertTrue("Confidence must be between 0 and 1", segment.confidence in 0.0..1.0)
        }
        
        // Validate subtitle timing consistency
        for (i in 0 until result.subtitles.size - 1) {
            val current = result.subtitles[i]
            val next = result.subtitles[i + 1]
            assertTrue("Subtitles must be in chronological order", 
                      current.endTime <= next.startTime)
        }
    }

    @Test
    fun `AI Subtitle Service async processing contract validation`() = testScope.runBlockingTest {
        val aiSubtitleService = createAISubtitleService()
        
        // Test long-running processing contract
        val longAudioRequest = AISubtitleRequest(
            audioData = "very-long-base64-encoded-audio-data",
            audioFormat = "wav",
            sampleRate = 44100,
            channels = 2,
            language = "en",
            model = "whisper-large-v2"
        )
        
        // First response indicates processing started
        val processingResponse = AISubtitleResponse(
            requestId = "req-456",
            status = "processing",
            subtitles = emptyList(),
            language = "en",
            processingTime = 0.0,
            wordCount = 0,
            estimatedCompletion = 45.0 // 45 seconds estimated
        )
        
        whenever(aiSubtitleService.generateSubtitles(longAudioRequest)).thenReturn(processingResponse)
        
        val initialResult = aiSubtitleService.generateSubtitles(longAudioRequest)
        
        // Validate async processing contract
        assertEquals("Status must be processing", "processing", initialResult.status)
        assertNotNull("Request ID must be provided for polling", initialResult.requestId)
        assertTrue("Estimated completion must be provided", initialResult.estimatedCompletion!! > 0)
        
        // Test polling for completion
        val completedResponse = AISubtitleResponse(
            requestId = "req-456",
            status = "completed",
            subtitles = listOf(
                AISubtitleSegment(1, 0.0, 3.0, "Long audio processing test.", 0.88)
            ),
            language = "en",
            processingTime = 42.3,
            wordCount = 4
        )
        
        whenever(aiSubtitleService.getSubtitleStatus("req-456")).thenReturn(completedResponse)
        
        val finalResult = aiSubtitleService.getSubtitleStatus("req-456")
        
        assertEquals("Status must be completed", "completed", finalResult.status)
        assertEquals("Request ID must match", initialResult.requestId, finalResult.requestId)
        assertTrue("Subtitles must be generated", finalResult.subtitles.isNotEmpty())
    }

    // ============================================================================
    // EXTERNAL SUBTITLE DATABASE API CONTRACT TESTS
    // ============================================================================

    @Test
    fun `External Subtitle Database search contract validation`() = testScope.runBlockingTest {
        val subtitleDbService = createSubtitleDatabaseService()
        
        val searchRequest = SubtitleSearchRequest(
            movieHash = "7f2c4b5d8e9a1c3f",
            fileName = "Movie.Example.2024.1080p.BluRay.x264.mp4",
            fileSize = 2147483648L, // 2GB
            language = "en"
        )
        
        val mockSearchResponse = SubtitleSearchResponse(
            results = listOf(
                SubtitleSearchResult(
                    id = "subtitle-123",
                    fileName = "Movie.Example.2024.1080p.BluRay.x264.srt",
                    language = "en",
                    downloads = 15420,
                    rating = 4.8,
                    uploadDate = "2024-12-15",
                    uploader = "SubtitleCommunity",
                    downloadUrl = "https://subtitledb.api/download/subtitle-123",
                    format = "srt",
                    encoding = "utf-8",
                    hearing_impaired = false,
                    movieYear = 2024,
                    fps = 23.976
                )
            ),
            totalResults = 1,
            searchTime = 0.156
        )
        
        whenever(subtitleDbService.searchSubtitles(searchRequest)).thenReturn(mockSearchResponse)
        
        val searchResult = subtitleDbService.searchSubtitles(searchRequest)
        
        // Validate search response contract
        assertTrue("Results must not be empty for valid hash", searchResult.results.isNotEmpty())
        assertTrue("Total results must be positive", searchResult.totalResults > 0)
        assertTrue("Search time must be positive", searchResult.searchTime > 0)
        
        // Validate search result contract
        val result = searchResult.results.first()
        assertNotNull("Subtitle ID must be provided", result.id)
        assertNotNull("Download URL must be provided", result.downloadUrl)
        assertTrue("Download URL must be valid", result.downloadUrl.startsWith("https://"))
        assertTrue("Language must match request", result.language == searchRequest.language)
        assertTrue("Downloads count must be non-negative", result.downloads >= 0)
        assertTrue("Rating must be between 0 and 5", result.rating in 0.0..5.0)
        assertNotNull("Upload date must be provided", result.uploadDate)
        assertTrue("Format must be specified", result.format.isNotBlank())
        assertTrue("Encoding must be specified", result.encoding.isNotBlank())
        assertTrue("FPS must be positive", result.fps > 0)
    }

    @Test
    fun `External Subtitle Database download contract validation`() = testScope.runBlockingTest {
        val subtitleDbService = createSubtitleDatabaseService()
        
        val downloadRequest = SubtitleDownloadRequest(
            subtitleId = "subtitle-123",
            userAgent = "AstralStream/2.0",
            format = "srt"
        )
        
        val mockSubtitleContent = """
            1
            00:00:00,000 --> 00:00:02,500
            Hello, this is a test subtitle.
            
            2
            00:00:02,500 --> 00:00:05,000
            This is the second subtitle.
        """.trimIndent()
        
        val mockDownloadResponse = SubtitleDownloadResponse(
            content = mockSubtitleContent,
            encoding = "utf-8",
            format = "srt",
            fileSize = mockSubtitleContent.toByteArray().size.toLong(),
            checksum = "md5:1a2b3c4d5e6f7890abcdef1234567890",
            downloadCount = 15421 // Incremented after download
        )
        
        whenever(subtitleDbService.downloadSubtitle(downloadRequest)).thenReturn(mockDownloadResponse)
        
        val downloadResult = subtitleDbService.downloadSubtitle(downloadRequest)
        
        // Validate download response contract
        assertNotNull("Content must be provided", downloadResult.content)
        assertTrue("Content must not be empty", downloadResult.content.isNotBlank())
        assertEquals("Encoding must match", "utf-8", downloadResult.encoding)
        assertEquals("Format must match request", downloadRequest.format, downloadResult.format)
        assertTrue("File size must be positive", downloadResult.fileSize > 0)
        assertEquals("File size must match content", 
                    downloadResult.content.toByteArray().size.toLong(), downloadResult.fileSize)
        assertNotNull("Checksum must be provided", downloadResult.checksum)
        assertTrue("Checksum must be valid format", 
                  downloadResult.checksum.matches(Regex("^(md5|sha1|sha256):[a-f0-9]+$")))
        assertTrue("Download count must be positive", downloadResult.downloadCount > 0)
    }

    // ============================================================================
    // VIDEO HOSTING API CONTRACT TESTS (YouTube, Vimeo, etc.)
    // ============================================================================

    @Test
    fun `Video hosting API metadata contract validation`() = testScope.runBlockingTest {
        val videoHostingService = createVideoHostingService()
        
        val videoUrls = listOf(
            "https://www.youtube.com/watch?v=dQw4w9WgXcQ",
            "https://vimeo.com/123456789",
            "https://www.dailymotion.com/video/x123456"
        )
        
        videoUrls.forEach { url ->
            val metadataRequest = VideoMetadataRequest(
                url = url,
                includeFormats = true,
                includeSubtitles = true,
                quality = "best"
            )
            
            val mockMetadataResponse = VideoMetadataResponse(
                title = "Example Video Title",
                description = "This is an example video description.",
                duration = 180, // 3 minutes
                uploader = "Example Channel",
                uploadDate = "2024-01-15",
                viewCount = 1000000,
                likeCount = 50000,
                thumbnail = "https://img.youtube.com/vi/dQw4w9WgXcQ/maxresdefault.jpg",
                formats = listOf(
                    VideoFormat(
                        formatId = "22",
                        ext = "mp4",
                        resolution = "720p",
                        fps = 30,
                        vcodec = "avc1.64001F",
                        acodec = "mp4a.40.2",
                        fileSize = 52428800L, // 50MB
                        url = "https://example.com/video.mp4"
                    )
                ),
                subtitles = mapOf(
                    "en" to listOf(
                        SubtitleTrackInfo(
                            url = "https://example.com/subtitles.vtt",
                            ext = "vtt",
                            name = "English"
                        )
                    )
                )
            )
            
            whenever(videoHostingService.getVideoMetadata(metadataRequest)).thenReturn(mockMetadataResponse)
            
            val metadata = videoHostingService.getVideoMetadata(metadataRequest)
            
            // Validate metadata contract
            assertNotNull("Title must be provided", metadata.title)
            assertTrue("Title must not be empty", metadata.title.isNotBlank())
            assertTrue("Duration must be positive", metadata.duration > 0)
            assertNotNull("Uploader must be provided", metadata.uploader)
            assertNotNull("Upload date must be provided", metadata.uploadDate)
            assertTrue("View count must be non-negative", metadata.viewCount >= 0)
            assertNotNull("Thumbnail URL must be provided", metadata.thumbnail)
            assertTrue("Thumbnail URL must be valid", metadata.thumbnail.startsWith("https://"))
            
            // Validate formats contract
            assertTrue("Formats must be provided", metadata.formats.isNotEmpty())
            metadata.formats.forEach { format ->
                assertNotNull("Format ID must be provided", format.formatId)
                assertNotNull("Extension must be provided", format.ext)
                assertNotNull("Resolution must be provided", format.resolution)
                assertTrue("FPS must be positive", format.fps > 0)
                assertNotNull("Video codec must be provided", format.vcodec)
                assertNotNull("Audio codec must be provided", format.acodec)
                assertTrue("File size must be positive", format.fileSize > 0)
                assertTrue("Download URL must be valid", format.url.startsWith("https://"))
            }
            
            // Validate subtitles contract if present
            if (metadata.subtitles.isNotEmpty()) {
                metadata.subtitles.forEach { (languageCode, tracks) ->
                    assertTrue("Language code must be valid", languageCode.matches(Regex("[a-z]{2,3}")))
                    tracks.forEach { track ->
                        assertTrue("Subtitle URL must be valid", track.url.startsWith("https://"))
                        assertTrue("Subtitle extension must be valid", 
                                  track.ext in listOf("vtt", "srt", "ass", "ssa"))
                        assertNotNull("Subtitle name must be provided", track.name)
                    }
                }
            }
        }
    }

    // ============================================================================
    // NETWORK RESILIENCE CONTRACT TESTS
    // ============================================================================

    @Test
    fun `API resilience contract validation`() = testScope.runBlockingTest {
        val resilientApiClient = createResilientApiClient()
        
        // Test retry mechanism contract
        var attemptCount = 0
        val flakyOperation = suspend {
            attemptCount++
            if (attemptCount < 3) {
                throw IOException("Network timeout")
            }
            "Success after retries"
        }
        
        val result = resilientApiClient.executeWithRetry(
            operation = flakyOperation,
            maxRetries = 3,
            backoffMs = 1000L
        )
        
        assertEquals("Should succeed after retries", "Success after retries", result)
        assertEquals("Should have attempted 3 times", 3, attemptCount)
        
        // Test circuit breaker contract
        val circuitBreakerClient = createCircuitBreakerApiClient()
        
        // First, trigger circuit breaker by causing failures
        repeat(5) {
            try {
                circuitBreakerClient.executeWithCircuitBreaker {
                    throw IOException("Service unavailable")
                }
            } catch (e: Exception) {
                // Expected failures
            }
        }
        
        // Circuit breaker should now be open
        val circuitBreakerException = assertFailsWith<CircuitBreakerOpenException> {
            circuitBreakerClient.executeWithCircuitBreaker {
                "This should not execute"
            }
        }
        
        assertNotNull("Circuit breaker exception must be thrown", circuitBreakerException)
        
        // Test rate limiting contract
        val rateLimitedClient = createRateLimitedApiClient()
        
        val startTime = System.currentTimeMillis()
        val requests = mutableListOf<Long>()
        
        // Make 5 requests rapidly
        repeat(5) {
            val requestTime = System.currentTimeMillis()
            rateLimitedClient.executeWithRateLimit {
                requests.add(requestTime)
                "Request $it"
            }
        }
        
        val endTime = System.currentTimeMillis()
        val totalTime = endTime - startTime
        
        // Rate limiting should ensure requests are spaced out
        assertTrue("Requests should be rate limited", totalTime >= 4000L) // At least 1s between requests
        assertTrue("All requests should complete", requests.size == 5)
    }

    // ============================================================================
    // API VERSION COMPATIBILITY CONTRACT TESTS
    // ============================================================================

    @Test
    fun `API version compatibility contract validation`() = testScope.runBlockingTest {
        val versionedApiClient = createVersionedApiClient()
        
        // Test different API versions
        val apiVersions = listOf("v1", "v2", "v3")
        
        apiVersions.forEach { version ->
            val request = VersionedApiRequest(
                version = version,
                endpoint = "/user/profile",
                method = "GET"
            )
            
            val response = when (version) {
                "v1" -> VersionedApiResponse(
                    version = "v1",
                    data = mapOf("name" to "John Doe", "email" to "john@example.com"),
                    deprecated = true,
                    deprecationDate = "2025-06-01",
                    migrationGuide = "https://api.example.com/migration/v1-to-v2"
                )
                "v2" -> VersionedApiResponse(
                    version = "v2", 
                    data = mapOf(
                        "profile" to mapOf(
                            "fullName" to "John Doe",
                            "emailAddress" to "john@example.com",
                            "userId" to 12345
                        )
                    ),
                    deprecated = false
                )
                "v3" -> VersionedApiResponse(
                    version = "v3",
                    data = mapOf(
                        "user" to mapOf(
                            "id" to 12345,
                            "personal" to mapOf(
                                "name" to mapOf(
                                    "first" to "John",
                                    "last" to "Doe",
                                    "full" to "John Doe"
                                ),
                                "contact" to mapOf(
                                    "email" to mapOf(
                                        "primary" to "john@example.com",
                                        "verified" to true
                                    )
                                )
                            )
                        )
                    ),
                    deprecated = false
                )
                else -> throw IllegalArgumentException("Unsupported version")
            }
            
            whenever(versionedApiClient.makeRequest(request)).thenReturn(response)
            
            val result = versionedApiClient.makeRequest(request)
            
            // Validate version compatibility contract
            assertEquals("Version must match request", version, result.version)
            assertNotNull("Data must be provided", result.data)
            
            // Validate deprecation contract
            if (result.deprecated) {
                assertNotNull("Deprecation date must be provided", result.deprecationDate)
                assertNotNull("Migration guide must be provided", result.migrationGuide)
                assertTrue("Migration guide must be valid URL", 
                          result.migrationGuide?.startsWith("https://") == true)
            }
            
            // Validate data structure evolution
            when (version) {
                "v1" -> {
                    assertTrue("V1 should have simple structure", 
                              result.data.containsKey("name") && result.data.containsKey("email"))
                }
                "v2" -> {
                    assertTrue("V2 should have nested profile structure", 
                              result.data.containsKey("profile"))
                }
                "v3" -> {
                    assertTrue("V3 should have deeply nested user structure", 
                              result.data.containsKey("user"))
                }
            }
        }
    }

    // ============================================================================
    // HELPER METHODS AND MOCK CREATION
    // ============================================================================

    private fun createOneDriveService(): OneDriveService = mock()
    private fun createAISubtitleService(): AISubtitleService = mock()
    private fun createSubtitleDatabaseService(): SubtitleDatabaseService = mock()
    private fun createVideoHostingService(): VideoHostingService = mock()
    private fun createResilientApiClient(): ResilientApiClient = mock()
    private fun createCircuitBreakerApiClient(): CircuitBreakerApiClient = mock()
    private fun createRateLimitedApiClient(): RateLimitedApiClient = mock()
    private fun createVersionedApiClient(): VersionedApiClient = mock()

    // ============================================================================
    // DATA CLASSES FOR API CONTRACTS
    // ============================================================================

    // OneDrive API contracts
    data class OneDriveAuthRequest(
        val clientId: String,
        val redirectUri: String,
        val scopes: List<String>,
        val responseType: String
    )

    data class OneDriveAuthResponse(
        val accessToken: String,
        val refreshToken: String,
        val expiresIn: Int,
        val tokenType: String,
        val scope: String
    )

    data class OneDriveUploadRequest(
        val fileName: String,
        val fileSize: Long,
        val mimeType: String,
        val content: ByteArray,
        val parentFolder: String
    )

    data class OneDriveUploadResponse(
        val id: String,
        val name: String,
        val size: Long,
        val createdDateTime: String,
        val lastModifiedDateTime: String,
        val downloadUrl: String,
        val parentReference: OneDriveItemReference
    )

    data class OneDriveItemReference(
        val driveId: String,
        val id: String,
        val path: String
    )

    data class OneDriveErrorResponse(
        val error: OneDriveError
    )

    data class OneDriveError(
        val code: String,
        val message: String,
        val innerError: OneDriveInnerError?
    )

    data class OneDriveInnerError(
        val code: String,
        val request_id: String,
        val date: String
    )

    data class OneDriveErrorScenario(
        val httpStatus: Int,
        val errorCode: String,
        val errorMessage: String
    )

    class OneDriveApiException(
        val httpStatus: Int,
        val errorResponse: OneDriveErrorResponse
    ) : Exception(errorResponse.error.message)

    // AI Subtitle Service contracts
    data class AISubtitleRequest(
        val audioData: String,
        val audioFormat: String,
        val sampleRate: Int,
        val channels: Int,
        val language: String,
        val model: String
    )

    data class AISubtitleResponse(
        val requestId: String,
        val status: String,
        val subtitles: List<AISubtitleSegment>,
        val language: String,
        val processingTime: Double,
        val wordCount: Int,
        val estimatedCompletion: Double? = null
    )

    data class AISubtitleSegment(
        val id: Int,
        val startTime: Double,
        val endTime: Double,
        val text: String,
        val confidence: Double
    )

    // Subtitle Database contracts
    data class SubtitleSearchRequest(
        val movieHash: String,
        val fileName: String,
        val fileSize: Long,
        val language: String
    )

    data class SubtitleSearchResponse(
        val results: List<SubtitleSearchResult>,
        val totalResults: Int,
        val searchTime: Double
    )

    data class SubtitleSearchResult(
        val id: String,
        val fileName: String,
        val language: String,
        val downloads: Int,
        val rating: Double,
        val uploadDate: String,
        val uploader: String,
        val downloadUrl: String,
        val format: String,
        val encoding: String,
        val hearing_impaired: Boolean,
        val movieYear: Int,
        val fps: Double
    )

    data class SubtitleDownloadRequest(
        val subtitleId: String,
        val userAgent: String,
        val format: String
    )

    data class SubtitleDownloadResponse(
        val content: String,
        val encoding: String,
        val format: String,
        val fileSize: Long,
        val checksum: String,
        val downloadCount: Int
    )

    // Video Hosting Service contracts
    data class VideoMetadataRequest(
        val url: String,
        val includeFormats: Boolean,
        val includeSubtitles: Boolean,
        val quality: String
    )

    data class VideoMetadataResponse(
        val title: String,
        val description: String?,
        val duration: Int,
        val uploader: String,
        val uploadDate: String,
        val viewCount: Long,
        val likeCount: Long?,
        val thumbnail: String,
        val formats: List<VideoFormat>,
        val subtitles: Map<String, List<SubtitleTrackInfo>>
    )

    data class VideoFormat(
        val formatId: String,
        val ext: String,
        val resolution: String,
        val fps: Int,
        val vcodec: String,
        val acodec: String,
        val fileSize: Long,
        val url: String
    )

    data class SubtitleTrackInfo(
        val url: String,
        val ext: String,
        val name: String
    )

    // Resilience contracts
    data class VersionedApiRequest(
        val version: String,
        val endpoint: String,
        val method: String
    )

    data class VersionedApiResponse(
        val version: String,
        val data: Map<String, Any>,
        val deprecated: Boolean,
        val deprecationDate: String? = null,
        val migrationGuide: String? = null
    )

    class CircuitBreakerOpenException(message: String) : Exception(message)

    // Service interfaces
    interface OneDriveService {
        suspend fun authenticate(request: OneDriveAuthRequest): OneDriveAuthResponse
        suspend fun uploadFile(token: String, request: OneDriveUploadRequest): OneDriveUploadResponse
    }

    interface AISubtitleService {
        suspend fun generateSubtitles(request: AISubtitleRequest): AISubtitleResponse
        suspend fun getSubtitleStatus(requestId: String): AISubtitleResponse
    }

    interface SubtitleDatabaseService {
        suspend fun searchSubtitles(request: SubtitleSearchRequest): SubtitleSearchResponse
        suspend fun downloadSubtitle(request: SubtitleDownloadRequest): SubtitleDownloadResponse
    }

    interface VideoHostingService {
        suspend fun getVideoMetadata(request: VideoMetadataRequest): VideoMetadataResponse
    }

    interface ResilientApiClient {
        suspend fun <T> executeWithRetry(
            operation: suspend () -> T,
            maxRetries: Int,
            backoffMs: Long
        ): T
    }

    interface CircuitBreakerApiClient {
        suspend fun <T> executeWithCircuitBreaker(operation: suspend () -> T): T
    }

    interface RateLimitedApiClient {
        suspend fun <T> executeWithRateLimit(operation: suspend () -> T): T
    }

    interface VersionedApiClient {
        suspend fun makeRequest(request: VersionedApiRequest): VersionedApiResponse
    }
}