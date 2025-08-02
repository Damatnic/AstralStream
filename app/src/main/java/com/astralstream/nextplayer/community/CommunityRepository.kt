package com.astralstream.nextplayer.community

import com.astralstream.nextplayer.network.CommunityApiService
import com.astralstream.nextplayer.viewmodels.CommunityViewModel
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CommunityRepository @Inject constructor(
    private val apiService: CommunityApiService
) {
    
    suspend fun getCommunityStats(): CommunityViewModel.CommunityStats {
        val response = apiService.getCommunityStats()
        return if (response.isSuccessful && response.body() != null) {
            val stats = response.body()!!
            CommunityViewModel.CommunityStats(
                totalUsers = stats.totalUsers,
                sharedPlaylists = stats.sharedPlaylists,
                contributedSubtitles = stats.contributedSubtitles
            )
        } else {
            // Return mock data for now
            CommunityViewModel.CommunityStats(
                totalUsers = 1000,
                sharedPlaylists = 250,
                contributedSubtitles = 500
            )
        }
    }
    
    suspend fun getTopContributors(): List<CommunityViewModel.Contributor> {
        val response = apiService.getTopContributors()
        return if (response.isSuccessful && response.body() != null) {
            response.body()!!.map { contributor ->
                CommunityViewModel.Contributor(
                    userId = contributor.userId,
                    username = contributor.username,
                    contributions = contributor.contributions,
                    rank = contributor.rank
                )
            }
        } else {
            // Return mock data for now
            listOf(
                CommunityViewModel.Contributor("1", "User1", 100, 1),
                CommunityViewModel.Contributor("2", "User2", 80, 2),
                CommunityViewModel.Contributor("3", "User3", 60, 3)
            )
        }
    }
    
    suspend fun getRecentActivity(): List<CommunityViewModel.Activity> {
        val response = apiService.getRecentActivity()
        return if (response.isSuccessful && response.body() != null) {
            response.body()!!.map { activity ->
                CommunityViewModel.Activity(
                    id = activity.id,
                    userId = activity.userId,
                    username = activity.username,
                    type = when (activity.type) {
                        "SHARED_PLAYLIST" -> CommunityViewModel.ActivityType.SHARED_PLAYLIST
                        "CONTRIBUTED_SUBTITLE" -> CommunityViewModel.ActivityType.CONTRIBUTED_SUBTITLE
                        "LIKED_PLAYLIST" -> CommunityViewModel.ActivityType.LIKED_PLAYLIST
                        else -> CommunityViewModel.ActivityType.JOINED_COMMUNITY
                    },
                    action = activity.action,
                    details = activity.details,
                    timestamp = activity.timestamp,
                    actionable = activity.actionable
                )
            }
        } else {
            emptyList()
        }
    }
    
    suspend fun getPopularPlaylists(): List<CommunityViewModel.SharedPlaylist> {
        val response = apiService.browsePlaylists("", emptyList(), "popular", 1, 10)
        return if (response.isSuccessful && response.body() != null) {
            response.body()!!.map { playlist ->
                CommunityViewModel.SharedPlaylist(
                    id = playlist.id,
                    name = playlist.name,
                    description = playlist.description,
                    creatorName = playlist.creatorName,
                    videoCount = playlist.videoCount,
                    totalDuration = playlist.totalDuration,
                    likes = playlist.likes,
                    plays = playlist.plays
                )
            }
        } else {
            emptyList()
        }
    }
    
    suspend fun searchCommunity(query: String) {
        // Implement search functionality
    }
    
    suspend fun sharePlaylist(playlistId: String) {
        // Implement playlist sharing
    }
    
    suspend fun startSubtitleContribution(videoId: String) {
        // Implement subtitle contribution
    }
}