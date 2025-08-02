package com.astralstream.nextplayer.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import android.net.Uri
import com.astralstream.nextplayer.feature.player.ui.VideoPlayerScreen
import com.astralstream.nextplayer.ui.screens.AnalyticsDashboardScreen
import com.astralstream.nextplayer.ui.screens.CommunityScreen
import com.astralstream.nextplayer.ui.screens.GestureCustomizationScreen
import com.astralstream.nextplayer.ui.screens.SettingsScreen

@Composable
fun AstralStreamNavHost(
    navController: NavHostController = rememberNavController(),
    startDestination: String = Routes.HOME
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // Main screens
        composable(Routes.HOME) {
            // HomeScreen implementation
        }
        
        composable(Routes.PLAYER) { backStackEntry ->
            val videoId = backStackEntry.arguments?.getString("videoId") ?: ""
            // PlayerScreen(videoId = videoId)
        }
        
        // Enhanced Video Player
        composable(
            route = Routes.VIDEO_PLAYER,
            arguments = listOf(
                navArgument("videoUri") { type = NavType.StringType },
                navArgument("videoTitle") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val videoUri = backStackEntry.arguments?.getString("videoUri") ?: ""
            val videoTitle = backStackEntry.arguments?.getString("videoTitle") ?: ""
            
            VideoPlayerScreen(
                videoUri = Uri.decode(videoUri),
                videoTitle = Uri.decode(videoTitle),
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        composable(Routes.SETTINGS) {
            SettingsScreen(
                navController = navController,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        // Community feature
        composable(Routes.COMMUNITY) {
            CommunityScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToSharedPlaylists = { 
                    navController.navigate(Routes.SHARED_PLAYLISTS)
                },
                onNavigateToContributeSubtitles = { 
                    navController.navigate(Routes.CONTRIBUTE_SUBTITLES)
                },
                onNavigateToProfile = { userId ->
                    navController.navigate(Routes.userProfile(userId))
                }
            )
        }
        
        // Gesture customization
        composable(Routes.GESTURE_CUSTOMIZATION) {
            GestureCustomizationScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        // Analytics dashboard
        composable(Routes.ANALYTICS_DASHBOARD) {
            AnalyticsDashboardScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToVideoDetails = { videoId ->
                    navController.navigate(Routes.player(videoId))
                }
            )
        }
        
        // Community sub-routes
        composable(Routes.SHARED_PLAYLISTS) {
            // SharedPlaylistsScreen
        }
        
        composable(Routes.CONTRIBUTE_SUBTITLES) {
            // ContributeSubtitlesScreen
        }
        
        composable(Routes.USER_PROFILE) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId") ?: ""
            // UserProfileScreen(userId = userId)
        }
    }
}