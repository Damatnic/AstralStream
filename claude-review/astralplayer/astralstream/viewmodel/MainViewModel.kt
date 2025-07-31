package com.astralplayer.astralstream.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.astralplayer.astralstream.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    application: Application,
    private val settingsRepository: SettingsRepository
) : AndroidViewModel(application) {
    
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _isDarkTheme = MutableStateFlow(true)
    val isDarkTheme: StateFlow<Boolean> = _isDarkTheme.asStateFlow()
    
    private val _hasUpdate = MutableStateFlow(false)
    val hasUpdate: StateFlow<Boolean> = _hasUpdate.asStateFlow()
    
    private val _updateVersion = MutableStateFlow("")
    val updateVersion: StateFlow<String> = _updateVersion.asStateFlow()
    
    init {
        loadInitialData()
    }
    
    private fun loadInitialData() {
        viewModelScope.launch {
            // Load theme settings
            loadThemeSettings()
            
            // Simulate loading time for splash screen
            delay(1000)
            
            _isLoading.value = false
        }
    }
    
    private suspend fun loadThemeSettings() {
        settingsRepository.getSettings()?.let { settings ->
            _isDarkTheme.value = when (settings.themeMode) {
                "light" -> false
                "dark" -> true
                else -> true // Default to dark theme
            }
        }
    }
    
    fun checkForUpdates() {
        viewModelScope.launch {
            // Check for app updates
            // This is a placeholder - implement actual update check
            _hasUpdate.value = false
            _updateVersion.value = ""
        }
    }
    
    fun toggleTheme() {
        viewModelScope.launch {
            val newTheme = !_isDarkTheme.value
            _isDarkTheme.value = newTheme
            
            settingsRepository.setThemeMode(
                if (newTheme) "dark" else "light"
            )
        }
    }
    
    fun dismissUpdate() {
        _hasUpdate.value = false
    }
}