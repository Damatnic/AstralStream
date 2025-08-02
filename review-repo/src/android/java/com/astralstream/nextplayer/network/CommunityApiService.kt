package com.astralstream.nextplayer.network

import com.astralstream.nextplayer.models.SharedPlaylist
import retrofit2.Response
import retrofit2.http.*

interface CommunityApiService {
    
    @GET("playlists/browse")
    suspend fun browsePlaylists(
        @Query("query") query: String,
        @Query("tags") tags: List<String>,
        @Query("sortBy") sortBy: String,
        @Query("page") page: Int,
        @Query("pageSize") pageSize: Int
    ): Response<List<SharedPlaylist>>
    
    @POST("playlists/share")
    suspend fun sharePlaylist(
        @Body playlist: SharedPlaylistRequest
    ): Response<SharePlaylistResponse>
    
    @GET("playlists/{id}")
    suspend fun getPlaylist(
        @Path("id") playlistId: String
    ): Response<SharedPlaylist>
    
    @POST("playlists/{id}/like")
    suspend fun likePlaylist(
        @Path("id") playlistId: String
    ): Response<LikeResponse>
    
    @DELETE("playlists/{id}/like")
    suspend fun unlikePlaylist(
        @Path("id") playlistId: String
    ): Response<LikeResponse>
    
    @GET("users/{id}/playlists")
    suspend fun getUserPlaylists(
        @Path("id") userId: String,
        @Query("page") page: Int = 1,
        @Query("pageSize") pageSize: Int = 20
    ): Response<List<SharedPlaylist>>
    
    @GET("community/stats")
    suspend fun getCommunityStats(): Response<CommunityStats>
    
    @GET("community/activity")
    suspend fun getRecentActivity(
        @Query("limit") limit: Int = 20
    ): Response<List<Activity>>
    
    @GET("community/contributors/top")
    suspend fun getTopContributors(
        @Query("limit") limit: Int = 10
    ): Response<List<Contributor>>
}

data class SharedPlaylistRequest(
    val metadata: String,
    val data: String,
    val isPublic: Boolean
)

data class SharePlaylistResponse(
    val shareLink: String,
    val playlistId: String
)

data class LikeResponse(
    val liked: Boolean,
    val totalLikes: Int
)

data class CommunityStats(
    val totalUsers: Int,
    val sharedPlaylists: Int,
    val contributedSubtitles: Int
)

data class Activity(
    val id: String,
    val userId: String,
    val username: String,
    val type: String,
    val action: String,
    val details: String?,
    val timestamp: Long,
    val actionable: Boolean
)

data class Contributor(
    val userId: String,
    val username: String,
    val contributions: Int,
    val rank: Int
)