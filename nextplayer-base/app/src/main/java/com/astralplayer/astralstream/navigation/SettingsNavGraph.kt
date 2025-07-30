package com.astralplayer.astralstream.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.navigation
import com.astralplayer.astralstream.settings.Setting
import com.astralplayer.astralstream.settings.navigation.aboutPreferencesScreen
import com.astralplayer.astralstream.settings.navigation.appearancePreferencesScreen
import com.astralplayer.astralstream.settings.navigation.audioPreferencesScreen
import com.astralplayer.astralstream.settings.navigation.decoderPreferencesScreen
import com.astralplayer.astralstream.settings.navigation.folderPreferencesScreen
import com.astralplayer.astralstream.settings.navigation.librariesScreen
import com.astralplayer.astralstream.settings.navigation.mediaLibraryPreferencesScreen
import com.astralplayer.astralstream.settings.navigation.navigateToAboutPreferences
import com.astralplayer.astralstream.settings.navigation.navigateToAppearancePreferences
import com.astralplayer.astralstream.settings.navigation.navigateToAudioPreferences
import com.astralplayer.astralstream.settings.navigation.navigateToDecoderPreferences
import com.astralplayer.astralstream.settings.navigation.navigateToFolderPreferencesScreen
import com.astralplayer.astralstream.settings.navigation.navigateToLibraries
import com.astralplayer.astralstream.settings.navigation.navigateToMediaLibraryPreferencesScreen
import com.astralplayer.astralstream.settings.navigation.navigateToPlayerPreferences
import com.astralplayer.astralstream.settings.navigation.navigateToSubtitlePreferences
import com.astralplayer.astralstream.settings.navigation.playerPreferencesScreen
import com.astralplayer.astralstream.settings.navigation.settingsNavigationRoute
import com.astralplayer.astralstream.settings.navigation.settingsScreen
import com.astralplayer.astralstream.settings.navigation.subtitlePreferencesScreen

const val SETTINGS_ROUTE = "settings_nav_route"

fun NavGraphBuilder.settingsNavGraph(
    navController: NavHostController,
) {
    navigation(
        startDestination = settingsNavigationRoute,
        route = SETTINGS_ROUTE,
    ) {
        settingsScreen(
            onNavigateUp = navController::navigateUp,
            onItemClick = { setting ->
                when (setting) {
                    Setting.APPEARANCE -> navController.navigateToAppearancePreferences()
                    Setting.MEDIA_LIBRARY -> navController.navigateToMediaLibraryPreferencesScreen()
                    Setting.PLAYER -> navController.navigateToPlayerPreferences()
                    Setting.DECODER -> navController.navigateToDecoderPreferences()
                    Setting.AUDIO -> navController.navigateToAudioPreferences()
                    Setting.SUBTITLE -> navController.navigateToSubtitlePreferences()
                    Setting.ABOUT -> navController.navigateToAboutPreferences()
                }
            },
        )
        appearancePreferencesScreen(
            onNavigateUp = navController::navigateUp,
        )
        mediaLibraryPreferencesScreen(
            onNavigateUp = navController::navigateUp,
            onFolderSettingClick = navController::navigateToFolderPreferencesScreen,
        )
        folderPreferencesScreen(
            onNavigateUp = navController::navigateUp,
        )
        playerPreferencesScreen(
            onNavigateUp = navController::navigateUp,
        )
        decoderPreferencesScreen(
            onNavigateUp = navController::navigateUp,
        )
        audioPreferencesScreen(
            onNavigateUp = navController::navigateUp,
        )
        subtitlePreferencesScreen(
            onNavigateUp = navController::navigateUp,
        )
        aboutPreferencesScreen(
            onLibrariesClick = navController::navigateToLibraries,
            onNavigateUp = navController::navigateUp,
        )
        librariesScreen(
            onNavigateUp = navController::navigateUp,
        )
    }
}
