package com.astralplayer.astralstream.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.astralplayer.astralstream.ui.screens.*

@Composable
fun AstralStreamNavigation(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route,
        modifier = modifier
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                onVideoClick = { video ->
                    navController.navigate(Screen.VideoPlayer.createRoute(video.id))
                },
                onNavigateToFolder = { folder ->
                    navController.navigate(Screen.Folder.createRoute(folder.id))
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                },
                onNavigateToSearch = {
                    navController.navigate(Screen.Search.route)
                }
            )
        }
        
        composable(Screen.Folder.route) { backStackEntry ->
            val folderId = backStackEntry.arguments?.getString("folderId")?.toLongOrNull() ?: 0
            FolderScreen(
                folderId = folderId,
                onVideoClick = { video ->
                    navController.navigate(Screen.VideoPlayer.createRoute(video.id))
                },
                onBackPressed = {
                    navController.popBackStack()
                }
            )
        }
        
        composable(Screen.VideoPlayer.route) { backStackEntry ->
            val videoId = backStackEntry.arguments?.getString("videoId")?.toLongOrNull() ?: 0
            // This is handled by VideoPlayerActivity
        }
        
        composable(Screen.Settings.route) {
            SettingsScreen(
                onBackPressed = {
                    navController.popBackStack()
                }
            )
        }
        
        composable(Screen.Search.route) {
            SearchScreen(
                onVideoClick = { video ->
                    navController.navigate(Screen.VideoPlayer.createRoute(video.id))
                },
                onBackPressed = {
                    navController.popBackStack()
                }
            )
        }
        
        composable(Screen.Playlists.route) {
            PlaylistsScreen(
                onPlaylistClick = { playlist ->
                    navController.navigate(Screen.PlaylistDetail.createRoute(playlist.id))
                },
                onBackPressed = {
                    navController.popBackStack()
                }
            )
        }
        
        composable(Screen.PlaylistDetail.route) { backStackEntry ->
            val playlistId = backStackEntry.arguments?.getString("playlistId")?.toLongOrNull() ?: 0
            PlaylistDetailScreen(
                playlistId = playlistId,
                onVideoClick = { video ->
                    navController.navigate(Screen.VideoPlayer.createRoute(video.id))
                },
                onBackPressed = {
                    navController.popBackStack()
                }
            )
        }
        
        composable(Screen.CloudStorage.route) {
            CloudStorageScreen(
                onVideoClick = { cloudFile ->
                    // Handle cloud video playback
                },
                onBackPressed = {
                    navController.popBackStack()
                }
            )
        }
    }
}

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Folder : Screen("folder/{folderId}") {
        fun createRoute(folderId: Long) = "folder/$folderId"
    }
    object VideoPlayer : Screen("video/{videoId}") {
        fun createRoute(videoId: Long) = "video/$videoId"
    }
    object Settings : Screen("settings")
    object Search : Screen("search")
    object Playlists : Screen("playlists")
    object PlaylistDetail : Screen("playlist/{playlistId}") {
        fun createRoute(playlistId: Long) = "playlist/$playlistId"
    }
    object CloudStorage : Screen("cloud")
}