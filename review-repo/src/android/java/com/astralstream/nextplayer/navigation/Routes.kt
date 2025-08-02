package com.astralstream.nextplayer.navigation

object Routes {
    // Main routes
    const val HOME = "home"
    const val PLAYER = "player/{videoId}"
    const val VIDEO_PLAYER = "video_player/{videoUri}/{videoTitle}"
    const val SETTINGS = "settings"
    
    // New feature routes
    const val COMMUNITY = "community"
    const val GESTURE_CUSTOMIZATION = "gesture_customization"
    const val ANALYTICS_DASHBOARD = "analytics_dashboard"
    
    // Community sub-routes
    const val SHARED_PLAYLISTS = "shared_playlists"
    const val CONTRIBUTE_SUBTITLES = "contribute_subtitles"
    const val USER_PROFILE = "user_profile/{userId}"
    
    // Helper functions
    fun player(videoId: String) = "player/$videoId"
    fun videoPlayer(videoUri: String, videoTitle: String) = "video_player/${android.net.Uri.encode(videoUri)}/${android.net.Uri.encode(videoTitle)}"
    fun userProfile(userId: String) = "user_profile/$userId"
}