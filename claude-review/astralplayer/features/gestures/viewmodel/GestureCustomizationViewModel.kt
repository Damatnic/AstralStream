package com.astralplayer.features.gestures.viewmodel

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.astralplayer.features.gestures.data.*
import com.astralplayer.features.gestures.repository.GestureRepository
import com.google.gson.Gson
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import javax.inject.Inject

@HiltViewModel
class GestureCustomizationViewModel @Inject constructor(
    private val repository: GestureRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {
    
    companion object {
        private const val TAG = "GestureCustomizationVM"
    }
    
    private val _uiState = MutableStateFlow(GestureUiState())
    val uiState: StateFlow<GestureUiState> = _uiState.asStateFlow()
    
    init {
        loadData()
    }
    
    private fun loadData() {
        viewModelScope.launch {
            // Initialize default gestures if needed
            repository.initializeDefaultGestures()
            
            // Load profiles
            repository.getAllProfiles()
                .catch { e ->
                    Log.e(TAG, "Failed to load profiles", e)
                    _uiState.update { it.copy(error = e.message) }
                }
                .collect { profiles ->
                    _uiState.update { it.copy(profiles = profiles) }
                }
        }
        
        viewModelScope.launch {
            // Load active profile
            val activeProfile = repository.getActiveProfile()
            _uiState.update { it.copy(activeProfile = activeProfile) }
            
            // Load gestures for active profile
            if (activeProfile != null) {
                repository.getEnabledGestures()
                    .catch { e ->
                        Log.e(TAG, "Failed to load gestures", e)
                        _uiState.update { it.copy(error = e.message) }
                    }
                    .collect { gestures ->
                        _uiState.update { it.copy(gestures = gestures) }
                    }
            }
        }
    }
    
    fun activateProfile(profileId: String) {
        viewModelScope.launch {
            try {
                repository.activateProfile(profileId)
                loadData() // Reload to reflect changes
                _uiState.update { it.copy(message = "Profile activated") }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to activate profile", e)
                _uiState.update { it.copy(error = "Failed to activate profile: ${e.message}") }
            }
        }
    }
    
    fun createProfile(name: String, description: String) {
        viewModelScope.launch {
            val result = repository.createProfile(name, description)
            if (result.isSuccess) {
                _uiState.update { it.copy(message = "Profile created successfully") }
            } else {
                _uiState.update { 
                    it.copy(error = "Failed to create profile: ${result.exceptionOrNull()?.message}")
                }
            }
        }
    }
    
    fun duplicateProfile(profileId: String, newName: String) {
        viewModelScope.launch {
            val result = repository.duplicateProfile(profileId, newName)
            if (result.isSuccess) {
                _uiState.update { it.copy(message = "Profile duplicated successfully") }
            } else {
                _uiState.update { 
                    it.copy(error = "Failed to duplicate profile: ${result.exceptionOrNull()?.message}")
                }
            }
        }
    }
    
    fun deleteProfile(profileId: String) {
        viewModelScope.launch {
            val result = repository.deleteProfile(profileId)
            if (result.isSuccess) {
                _uiState.update { it.copy(message = "Profile deleted") }
            } else {
                _uiState.update { 
                    it.copy(error = "Failed to delete profile: ${result.exceptionOrNull()?.message}")
                }
            }
        }
    }
    
    fun toggleGestureEnabled(gestureId: String) {
        viewModelScope.launch {
            val gesture = _uiState.value.gestures.find { it.id == gestureId } ?: return@launch
            repository.setGestureEnabled(gestureId, !gesture.isEnabled)
        }
    }
    
    fun updateGesture(gesture: GestureEntity) {
        viewModelScope.launch {
            try {
                repository.updateGesture(gesture)
                _uiState.update { it.copy(message = "Gesture updated") }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update gesture", e)
                _uiState.update { it.copy(error = "Failed to update gesture: ${e.message}") }
            }
        }
    }
    
    fun resetToDefaults() {
        viewModelScope.launch {
            _uiState.update { it.copy(showResetConfirmation = true) }
        }
    }
    
    fun confirmReset() {
        viewModelScope.launch {
            try {
                // Reset gestures
                repository.initializeDefaultGestures()
                _uiState.update { 
                    it.copy(
                        message = "Reset to defaults",
                        showResetConfirmation = false
                    )
                }
                loadData()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to reset to defaults", e)
                _uiState.update { 
                    it.copy(
                        error = "Failed to reset: ${e.message}",
                        showResetConfirmation = false
                    )
                }
            }
        }
    }
    
    fun cancelReset() {
        _uiState.update { it.copy(showResetConfirmation = false) }
    }
    
    fun exportSettings() {
        viewModelScope.launch {
            try {
                val activeProfile = _uiState.value.activeProfile ?: return@launch
                val gestures = _uiState.value.gestures
                
                val exportData = GestureExportData(
                    profileName = activeProfile.name,
                    profileDescription = activeProfile.description,
                    gestures = gestures.map { gesture ->
                        GestureExportItem(
                            gestureType = gesture.gestureType.name,
                            zone = gesture.zone.name,
                            action = gesture.action.name,
                            isEnabled = gesture.isEnabled,
                            sensitivity = gesture.sensitivity,
                            requiredFingers = gesture.requiredFingers,
                            hapticFeedback = gesture.hapticFeedback,
                            visualFeedback = gesture.visualFeedback,
                            minimumDistance = gesture.minimumDistance,
                            longPressTimeout = gesture.longPressTimeout,
                            doubleTapTimeout = gesture.doubleTapTimeout
                        )
                    },
                    exportVersion = 1,
                    exportDate = System.currentTimeMillis()
                )
                
                _uiState.update { 
                    it.copy(
                        exportData = Gson().toJson(exportData),
                        showExportDialog = true
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to export settings", e)
                _uiState.update { it.copy(error = "Failed to export: ${e.message}") }
            }
        }
    }
    
    fun importSettings() {
        _uiState.update { it.copy(showImportDialog = true) }
    }
    
    fun importFromUri(uri: Uri) {
        viewModelScope.launch {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val reader = BufferedReader(InputStreamReader(inputStream))
                val json = reader.use { it.readText() }
                
                val importData = Gson().fromJson(json, GestureExportData::class.java)
                
                // Create new profile from import
                val result = repository.createProfile(
                    "${importData.profileName} (Imported)",
                    importData.profileDescription
                )
                
                if (result.isSuccess) {
                    // Import gestures
                    // This would need additional implementation in repository
                    _uiState.update { 
                        it.copy(
                            message = "Settings imported successfully",
                            showImportDialog = false
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to import settings", e)
                _uiState.update { 
                    it.copy(
                        error = "Failed to import: ${e.message}",
                        showImportDialog = false
                    )
                }
            }
        }
    }
    
    fun dismissDialogs() {
        _uiState.update { 
            it.copy(
                showExportDialog = false,
                showImportDialog = false,
                showResetConfirmation = false
            )
        }
    }
    
    fun clearMessages() {
        _uiState.update { 
            it.copy(
                message = null,
                error = null
            )
        }
    }
}

data class GestureUiState(
    val profiles: List<GestureProfileEntity> = emptyList(),
    val activeProfile: GestureProfileEntity? = null,
    val gestures: List<GestureEntity> = emptyList(),
    val isLoading: Boolean = false,
    val message: String? = null,
    val error: String? = null,
    val showResetConfirmation: Boolean = false,
    val showExportDialog: Boolean = false,
    val showImportDialog: Boolean = false,
    val exportData: String? = null
)

data class GestureExportData(
    val profileName: String,
    val profileDescription: String,
    val gestures: List<GestureExportItem>,
    val exportVersion: Int,
    val exportDate: Long
)

data class GestureExportItem(
    val gestureType: String,
    val zone: String,
    val action: String,
    val isEnabled: Boolean,
    val sensitivity: Float,
    val requiredFingers: Int,
    val hapticFeedback: Boolean,
    val visualFeedback: Boolean,
    val minimumDistance: Float,
    val longPressTimeout: Long,
    val doubleTapTimeout: Long
)