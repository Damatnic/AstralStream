package com.astralplayer.astralstream.settings.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.navOptions
import com.astralplayer.astralstream.core.ui.designsystem.animatedComposable
import com.astralplayer.astralstream.settings.screens.medialibrary.FolderPreferencesScreen
import com.astralplayer.astralstream.settings.screens.medialibrary.MediaLibraryPreferencesScreen

const val mediaLibraryPreferencesNavigationRoute = "media_library_preferences_route"
const val folderPreferencesNavigationRoute = "folder_preferences_route"

fun NavController.navigateToMediaLibraryPreferencesScreen(navOptions: NavOptions? = navOptions { launchSingleTop = true }) {
    this.navigate(mediaLibraryPreferencesNavigationRoute, navOptions)
}

fun NavController.navigateToFolderPreferencesScreen(navOptions: NavOptions? = navOptions { launchSingleTop = true }) {
    this.navigate(folderPreferencesNavigationRoute, navOptions)
}

fun NavGraphBuilder.mediaLibraryPreferencesScreen(
    onNavigateUp: () -> Unit,
    onFolderSettingClick: () -> Unit,
) {
    animatedComposable(route = mediaLibraryPreferencesNavigationRoute) {
        MediaLibraryPreferencesScreen(
            onNavigateUp = onNavigateUp,
            onFolderSettingClick = onFolderSettingClick,
        )
    }
}

fun NavGraphBuilder.folderPreferencesScreen(onNavigateUp: () -> Unit) {
    animatedComposable(route = folderPreferencesNavigationRoute) {
        FolderPreferencesScreen(onNavigateUp = onNavigateUp)
    }
}
