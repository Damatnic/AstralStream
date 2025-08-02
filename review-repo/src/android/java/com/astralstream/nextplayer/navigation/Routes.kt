package com.astralstream.nextplayer.navigation

object Routes {
    // Main routes
    const val HOME = "home"
    const val PLAYER = "player/{videoId}"
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
    fun userProfile(userId: String) = "user_profile/$userId"
}