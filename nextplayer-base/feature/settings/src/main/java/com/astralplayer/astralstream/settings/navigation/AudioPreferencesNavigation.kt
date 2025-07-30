package com.astralplayer.astralstream.settings.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.navOptions
import com.astralplayer.astralstream.core.ui.designsystem.animatedComposable
import com.astralplayer.astralstream.settings.screens.audio.AudioPreferencesScreen

const val audioPreferencesNavigationRoute = "audio_preferences_route"

fun NavController.navigateToAudioPreferences(navOptions: NavOptions? = navOptions { launchSingleTop = true }) {
    this.navigate(audioPreferencesNavigationRoute, navOptions)
}

fun NavGraphBuilder.audioPreferencesScreen(onNavigateUp: () -> Unit) {
    animatedComposable(route = audioPreferencesNavigationRoute) {
        AudioPreferencesScreen(onNavigateUp = onNavigateUp)
    }
}
