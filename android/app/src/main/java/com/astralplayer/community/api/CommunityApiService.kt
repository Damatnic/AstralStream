package com.astralplayer.community.api

import com.astralplayer.community.data.*
import kotlinx.coroutines.delay
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

interface CommunityApiService {
    
    // Playlist sharing endpoints
    @POST("playlists/share")
    suspend fun sharePlaylist(@Body request: SharePlaylistRequest): ApiResponse<SharePlaylistResponse>
    
    @GET("playlists/{shareCode}")
    suspend fun getSharedPlaylist(@Path("shareCode") shareCode: String): ApiResponse<SharedPlaylistResponse>
    
    @POST("playlists/{shareCode}/import")
    suspend fun importPlaylist(
        @Path("shareCode") shareCode: String,
        @Body request: ImportPlaylistRequest
    ): ApiResponse<ImportPlaylistResponse>
    
    @GET("playlists/trending")
    suspend fun getTrendingPlaylists(
        @Query("limit") limit: Int = 20,
        @Query("category") category: String? = null,
        @Query("language") language: String? = null
    ): ApiResponse<List<SharedPlaylistResponse>>
    
    @POST("playlists/{shareCode}/view")
    suspend fun recordView(@Path("shareCode") shareCode: String): ApiResponse<Unit>
    
    @POST("playlists/{shareCode}/rate")
    suspend fun ratePlaylist(
        @Path("shareCode") shareCode: String,
        @Body rating: PlaylistRatingRequest
    ): ApiResponse<Unit>
    
    // Subtitle contribution endpoints
    @POST("subtitles/contribute")
    suspend fun contributeSubtitle(@Body request: ContributeSubtitleRequest): ApiResponse<ContributeSubtitleResponse>
    
    @GET("subtitles/{videoHash}")
    suspend fun getSubtitlesForVideo(
        @Path("videoHash") videoHash: String,
        @Query("language") language: String? = null
    ): ApiResponse<List<CommunitySubtitleResponse>>
    
    @POST("subtitles/{subtitleId}/vote")
    suspend fun voteOnSubtitle(
        @Path("subtitleId") subtitleId: String,
        @Body vote: SubtitleVoteRequest
    ): ApiResponse<Unit>
    
    @POST("subtitles/{subtitleId}/report")
    suspend fun reportSubtitle(
        @Path("subtitleId") subtitleId: String,
        @Body report: SubtitleReportRequest
    ): ApiResponse<Unit>
    
    @GET("subtitles/{subtitleId}/download")
    suspend fun downloadSubtitle(@Path("subtitleId") subtitleId: String): ApiResponse<SubtitleDownloadResponse>
    
    // Community stats endpoints
    @GET("stats/top-contributors")
    suspend fun getTopContributors(@Query("limit") limit: Int = 10): ApiResponse<List<ContributorResponse>>
    
    @GET("stats/recent-activity")
    suspend fun getRecentActivity(@Query("limit") limit: Int = 20): ApiResponse<List<ActivityResponse>>
}

/**
 * Mock implementation of CommunityApiService for offline development and testing
 */
@Singleton
class MockCommunityApiService @Inject constructor() {
    
    private val sharedPlaylists = mutableMapOf<String, SharedPlaylistResponse>()
    private val communitySubtitles = mutableListOf<CommunitySubtitleResponse>()
    private val mockDelay = 1000L // Simulate network delay
    
    init {
        // Populate with mock data
        initializeMockData()
    }
    
    suspend fun sharePlaylist(request: SharePlaylistRequest): ApiResponse<SharePlaylistResponse> {
        delay(mockDelay)
        
        val shareCode = generateShareCode()
        val response = SharedPlaylistResponse(
            id = java.util.UUID.randomUUID().toString(),
            shareCode = shareCode,
            title = request.title,
            description = request.description,
            creatorName = request.creatorName,
            videoCount = request.videoIds.size,
            thumbnailUrl = request.thumbnailUrl,
            shareUrl = "astralstream://playlist/$shareCode",
            category = request.category,
            language = request.language,
            createdAt = System.currentTimeMillis(),
            expirationTime = System.currentTimeMillis() + request.expirationDays * 24 * 60 * 60 * 1000L,
            isPublic = request.isPublic,
            hasPassword = request.password != null
        )
        
        sharedPlaylists[shareCode] = response
        
        return ApiResponse.success(response)
    }
    
    suspend fun getSharedPlaylist(shareCode: String): ApiResponse<SharedPlaylistResponse> {
        delay(mockDelay / 2)
        
        val playlist = sharedPlaylists[shareCode]
        return if (playlist != null) {
            // Simulate view increment
            val updatedPlaylist = playlist.copy(viewCount = playlist.viewCount + 1)
            sharedPlaylists[shareCode] = updatedPlaylist
            ApiResponse.success(updatedPlaylist)
        } else {
            ApiResponse.error("Playlist not found", 404)
        }
    }
    
    suspend fun importPlaylist(shareCode: String, request: ImportPlaylistRequest): ApiResponse<ImportPlaylistResponse> {
        delay(mockDelay)
        
        val playlist = sharedPlaylists[shareCode]
        return if (playlist != null) {
            // Check password if required
            if (playlist.hasPassword && request.password.isNullOrBlank()) {
                return ApiResponse.error("Password required", 401)
            }
            
            // Simulate download increment
            sharedPlaylists[shareCode] = playlist.copy(downloadCount = playlist.downloadCount + 1)
            
            ApiResponse.success(
                ImportPlaylistResponse(
                    playlistId = java.util.UUID.randomUUID().toString(),
                    importedVideoCount = playlist.videoCount,
                    message = "Playlist imported successfully"
                )
            )
        } else {
            ApiResponse.error("Playlist not found", 404)
        }
    }
    
    suspend fun getTrendingPlaylists(
        limit: Int,
        category: String?,
        language: String?
    ): ApiResponse<List<SharedPlaylistResponse>> {
        delay(mockDelay / 2)
        
        var playlists = sharedPlaylists.values.toList()
        
        // Apply filters
        category?.let { cat ->
            playlists = playlists.filter { it.category == cat }
        }
        
        language?.let { lang ->
            playlists = playlists.filter { it.language == lang }
        }
        
        // Sort by popularity (views + downloads)
        playlists = playlists.sortedByDescending { it.viewCount + it.downloadCount }
            .take(limit)
        
        return ApiResponse.success(playlists)
    }
    
    suspend fun contributeSubtitle(request: ContributeSubtitleRequest): ApiResponse<ContributeSubtitleResponse> {
        delay(mockDelay)
        
        val subtitleId = java.util.UUID.randomUUID().toString()
        val response = CommunitySubtitleResponse(
            id = subtitleId,
            videoHash = request.videoHash,
            language = request.language,
            contributorName = request.contributorName,
            content = request.content,
            format = request.format,
            createdAt = System.currentTimeMillis(),
            upvotes = 0,
            downvotes = 0,
            verificationStatus = "pending",
            confidence = request.confidence,
            version = 1
        )
        
        communitySubtitles.add(response)
        
        return ApiResponse.success(
            ContributeSubtitleResponse(
                subtitleId = subtitleId,
                status = "submitted",
                message = "Subtitle contribution received for review"
            )
        )
    }
    
    suspend fun getSubtitlesForVideo(
        videoHash: String,
        language: String?
    ): ApiResponse<List<CommunitySubtitleResponse>> {
        delay(mockDelay / 3)
        
        var subtitles = communitySubtitles.filter { it.videoHash == videoHash }
        
        language?.let { lang ->
            subtitles = subtitles.filter { it.language.equals(lang, ignoreCase = true) }
        }
        
        return ApiResponse.success(subtitles.sortedByDescending { it.upvotes - it.downvotes })
    }
    
    suspend fun voteOnSubtitle(subtitleId: String, vote: SubtitleVoteRequest): ApiResponse<Unit> {
        delay(mockDelay / 4)
        
        val subtitleIndex = communitySubtitles.indexOfFirst { it.id == subtitleId }
        if (subtitleIndex != -1) {
            val subtitle = communitySubtitles[subtitleIndex]
            communitySubtitles[subtitleIndex] = if (vote.isUpvote) {
                subtitle.copy(upvotes = subtitle.upvotes + 1)
            } else {
                subtitle.copy(downvotes = subtitle.downvotes + 1)
            }
        }
        
        return ApiResponse.success(Unit)
    }
    
    suspend fun getTopContributors(): ApiResponse<List<ContributorResponse>> {
        delay(mockDelay / 2)
        
        val contributors = listOf(
            ContributorResponse(
                id = "contributor1",
                name = "SubtitleMaster",
                contributionCount = 47,
                averageRating = 4.8f,
                reputation = 2340,
                joinedAt = System.currentTimeMillis() - 365 * 24 * 60 * 60 * 1000L
            ),
            ContributorResponse(
                id = "contributor2", 
                name = "CaptionExpert",
                contributionCount = 32,
                averageRating = 4.6f,
                reputation = 1890,
                joinedAt = System.currentTimeMillis() - 200 * 24 * 60 * 60 * 1000L
            ),
            ContributorResponse(
                id = "contributor3",
                name = "TextSynchronizer", 
                contributionCount = 28,
                averageRating = 4.7f,
                reputation = 1650,
                joinedAt = System.currentTimeMillis() - 150 * 24 * 60 * 60 * 1000L
            )
        )
        
        return ApiResponse.success(contributors)
    }
    
    private fun initializeMockData() {
        // Add some sample shared playlists
        val samplePlaylists = listOf(
            SharedPlaylistResponse(
                id = "sample1",
                shareCode = "MOVIE123",
                title = "Best Action Movies 2024",
                description = "A curated collection of the best action movies from 2024",
                creatorName = "MovieBuff",
                videoCount = 15,
                shareUrl = "astralstream://playlist/MOVIE123",
                category = "ENTERTAINMENT",
                language = "en",
                createdAt = System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000L,
                expirationTime = System.currentTimeMillis() + 30 * 24 * 60 * 60 * 1000L,
                viewCount = 342,
                downloadCount = 89,
                likeCount = 127,
                isPublic = true,
                hasPassword = false
            ),
            SharedPlaylistResponse(
                id = "sample2",
                shareCode = "LEARN456", 
                title = "Programming Tutorials",
                description = "Essential programming tutorials for beginners",
                creatorName = "CodeGuru",
                videoCount = 23,
                shareUrl = "astralstream://playlist/LEARN456",
                category = "EDUCATION",
                language = "en", 
                createdAt = System.currentTimeMillis() - 5 * 24 * 60 * 60 * 1000L,
                expirationTime = System.currentTimeMillis() + 60 * 24 * 60 * 60 * 1000L,
                viewCount = 198,
                downloadCount = 54,
                likeCount = 87,
                isPublic = true,
                hasPassword = false
            )
        )
        
        samplePlaylists.forEach { playlist ->
            sharedPlaylists[playlist.shareCode] = playlist
        }
        
        // Add sample community subtitles
        val sampleSubtitles = listOf(
            CommunitySubtitleResponse(
                id = "sub1",
                videoHash = "hash123",
                language = "English",
                contributorName = "SubtitlePro",
                content = "Sample SRT content...",
                format = "srt",
                createdAt = System.currentTimeMillis() - 2 * 24 * 60 * 60 * 1000L,
                upvotes = 24,
                downvotes = 2,
                verificationStatus = "community_verified",
                confidence = 0.95f,
                version = 1
            ),
            CommunitySubtitleResponse(
                id = "sub2",
                videoHash = "hash123",
                language = "Spanish", 
                contributorName = "SubtitulosES",
                content = "Contenido de subtítulos en español...",
                format = "srt",
                createdAt = System.currentTimeMillis() - 1 * 24 * 60 * 60 * 1000L,
                upvotes = 18,
                downvotes = 1,
                verificationStatus = "pending",
                confidence = 0.88f,
                version = 1
            )
        )
        
        communitySubtitles.addAll(sampleSubtitles)
    }
    
    private fun generateShareCode(): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        return (1..8).map { chars.random() }.joinToString("")
    }
}

// HTTP client for real API calls (when implemented)
@Singleton
class CommunityApiClient @Inject constructor() {
    
    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
    
    private val retrofit = Retrofit.Builder()
        .baseUrl("https://api.astralstream.app/v1/community/") // Placeholder URL
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
    
    val api: CommunityApiService = retrofit.create(CommunityApiService::class.java)
}

// Wrapper class to handle both mock and real API
@Singleton
class CommunityApiManager @Inject constructor(
    private val mockApi: MockCommunityApiService,
    private val realApiClient: CommunityApiClient
) {
    
    private val useMockApi = true // Switch to false when real API is available
    
    suspend fun sharePlaylist(request: SharePlaylistRequest): ApiResponse<SharePlaylistResponse> {
        return if (useMockApi) {
            mockApi.sharePlaylist(request)
        } else {
            realApiClient.api.sharePlaylist(request)
        }
    }
    
    suspend fun getSharedPlaylist(shareCode: String): ApiResponse<SharedPlaylistResponse> {
        return if (useMockApi) {
            mockApi.getSharedPlaylist(shareCode)
        } else {
            realApiClient.api.getSharedPlaylist(shareCode)
        }
    }
    
    suspend fun importPlaylist(shareCode: String, request: ImportPlaylistRequest): ApiResponse<ImportPlaylistResponse> {
        return if (useMockApi) {
            mockApi.importPlaylist(shareCode, request)
        } else {
            realApiClient.api.importPlaylist(shareCode, request)
        }
    }
    
    suspend fun getTrendingPlaylists(
        limit: Int = 20,
        category: String? = null,
        language: String? = null
    ): ApiResponse<List<SharedPlaylistResponse>> {
        return if (useMockApi) {
            mockApi.getTrendingPlaylists(limit, category, language)
        } else {
            realApiClient.api.getTrendingPlaylists(limit, category, language)
        }
    }
    
    suspend fun contributeSubtitle(request: ContributeSubtitleRequest): ApiResponse<ContributeSubtitleResponse> {
        return if (useMockApi) {
            mockApi.contributeSubtitle(request)
        } else {
            realApiClient.api.contributeSubtitle(request)
        }
    }
    
    suspend fun getSubtitlesForVideo(videoHash: String, language: String? = null): ApiResponse<List<CommunitySubtitleResponse>> {
        return if (useMockApi) {
            mockApi.getSubtitlesForVideo(videoHash, language)
        } else {
            realApiClient.api.getSubtitlesForVideo(videoHash, language)
        }
    }
    
    suspend fun voteOnSubtitle(subtitleId: String, vote: SubtitleVoteRequest): ApiResponse<Unit> {
        return if (useMockApi) {
            mockApi.voteOnSubtitle(subtitleId, vote)
        } else {
            realApiClient.api.voteOnSubtitle(subtitleId, vote)
        }
    }
    
    suspend fun getTopContributors(): ApiResponse<List<ContributorResponse>> {
        return if (useMockApi) {
            mockApi.getTopContributors()
        } else {
            realApiClient.api.getTopContributors()
        }
    }
}