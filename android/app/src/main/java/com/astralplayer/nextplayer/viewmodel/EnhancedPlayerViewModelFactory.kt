package com.astralplayer.nextplayer.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.astralplayer.nextplayer.data.PlayerRepository
import com.astralplayer.nextplayer.data.EnhancedGestureManager
import com.astralplayer.nextplayer.data.gesture.GestureSettingsSerializer
import com.astralplayer.nextplayer.data.HapticFeedbackManager

class EnhancedPlayerViewModelFactory(
    private val application: Application,
    private val playerRepository: PlayerRepository,
    private val gestureManager: EnhancedGestureManager,
    private val settingsSerializer: GestureSettingsSerializer,
    private val hapticManager: HapticFeedbackManager
) : ViewModelProvider.Factory {
    
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(EnhancedPlayerViewModel::class.java)) {
            return EnhancedPlayerViewModel(
                application = application,
                playerRepository = playerRepository,
                gestureManager = gestureManager,
                settingsSerializer = settingsSerializer,
                hapticManager = hapticManager
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}