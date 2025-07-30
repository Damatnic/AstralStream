package com.astralplayer.nextplayer.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.astralplayer.nextplayer.data.SettingsDataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsDataStore: SettingsDataStore
) : ViewModel() {
    
    val settings = settingsDataStore.settings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsDataStore.Settings()
    )
    
    fun updateAutoPlayNext(value: Boolean) {
        viewModelScope.launch {
            settingsDataStore.updateAutoPlayNext(value)
        }
    }
    
    fun updateRememberPosition(value: Boolean) {
        viewModelScope.launch {
            settingsDataStore.updateRememberPosition(value)
        }
    }
    
    fun updateHardwareAcceleration(value: Boolean) {
        viewModelScope.launch {
            settingsDataStore.updateHardwareAcceleration(value)
        }
    }
    
    fun updateEnableGestures(value: Boolean) {
        viewModelScope.launch {
            settingsDataStore.updateEnableGestures(value)
        }
    }
    
    fun updateDoubleTapSeek(value: Boolean) {
        viewModelScope.launch {
            settingsDataStore.updateDoubleTapSeek(value)
        }
    }
    
    fun updateLongPressSpeed(value: Boolean) {
        viewModelScope.launch {
            settingsDataStore.updateLongPressSpeed(value)
        }
    }
    
    fun updateKeepScreenOn(value: Boolean) {
        viewModelScope.launch {
            settingsDataStore.updateKeepScreenOn(value)
        }
    }
    
    fun updateShowVideoInfo(value: Boolean) {
        viewModelScope.launch {
            settingsDataStore.updateShowVideoInfo(value)
        }
    }
    
    fun updateAutoLoadSubtitles(value: Boolean) {
        viewModelScope.launch {
            settingsDataStore.updateAutoLoadSubtitles(value)
        }
    }
    
    fun updateWifiOnlyStreaming(value: Boolean) {
        viewModelScope.launch {
            settingsDataStore.updateWifiOnlyStreaming(value)
        }
    }
    
    fun updateBufferSize(value: String) {
        viewModelScope.launch {
            settingsDataStore.updateBufferSize(value)
        }
    }
}