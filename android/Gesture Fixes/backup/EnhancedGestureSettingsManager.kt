package com.astralplayer.nextplayer.feature.player.gestures

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString

/**
 * Enhanced gesture settings manager with persistence, validation, and migration
 */
class EnhancedGestureSettingsManager(private val context: Context) {
    
    companion object {
        private const val PREFS_NAME = "enhanced_gesture_settings"
        private const val KEY_SETTINGS = "gesture_settings"
        private const val KEY_USAGE_DATA = "usage_data"
        private const val KEY_ADAPTIVE_PROFILE = "adaptive_profile"
        private const val KEY_SETTINGS_VERSION = "settings_version"
        private const val CURRENT_VERSION = 1
        private const val TAG = "EnhancedGestureSettingsManager"
    }
    
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val json = Json { 
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    
    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<EnhancedGestureSettings> = _settings.asStateFlow()
    
    private val _usageData = MutableStateFlow<Map<GestureType, GestureUsageData>>(loadUsageData())
    val usageData: StateFlow<Map<GestureType, GestureUsageData>> = _usageData.asStateFlow()
    
    private val _adaptiveProfile = MutableStateFlow<AdaptiveGestureProfile?>(loadAdaptiveProfile())
    val adaptiveProfile: StateFlow<AdaptiveGestureProfile?> = _adaptiveProfile.asStateFlow()
    
    init {
        migrateSettingsIfNeeded()
    }
    
    /**
     * Load settings from SharedPreferences with validation and fallback
     */
    private fun loadSettings(): EnhancedGestureSettings {
        return try {
            val settingsJson = prefs.getString(KEY_SETTINGS, null)
            if (settingsJson != null) {
                val settings = json.decodeFromString<EnhancedGestureSettings>(settingsJson)
                settings.validate()
            } else {
                Log.i(TAG, "No saved settings found, using defaults")
                EnhancedGestureSettingsFactory.createDefault()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load settings, using defaults", e)
            EnhancedGestureSettingsFactory.createDefault()
        }
    }
    
    /**
     * Load usage data from SharedPreferences
     */
    private fun loadUsageData(): Map<GestureType, GestureUsageData> {
        return try {
            val usageDataJson = prefs.getString(KEY_USAGE_DATA, null)
            if (usageDataJson != null) {
                json.decodeFromString<Map<GestureType, GestureUsageData>>(usageDataJson)
            } else {
                emptyMap()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load usage data", e)
            emptyMap()
        }
    }
    
    /**
     * Load adaptive profile from SharedPreferences
     */
    private fun loadAdaptiveProfile(): AdaptiveGestureProfile? {
        return try {
            val profileJson = prefs.getString(KEY_ADAPTIVE_PROFILE, null)
            if (profileJson != null) {
                json.decodeFromString<AdaptiveGestureProfile>(profileJson)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load adaptive profile", e)
            null
        }
    }
    
    /**
     * Save settings to SharedPreferences
     */
    suspend fun saveSettings(settings: EnhancedGestureSettings) {
        withContext(Dispatchers.IO) {
            try {
                val validatedSettings = settings.validate()
                val settingsJson = json.encodeToString(validatedSettings)
                prefs.edit()
                    .putString(KEY_SETTINGS, settingsJson)
                    .putInt(KEY_SETTINGS_VERSION, CURRENT_VERSION)
                    .apply()
                
                _settings.value = validatedSettings
                Log.d(TAG, "Settings saved successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to save settings", e)
                throw GestureSettingsException("Failed to save settings", e)
            }
        }
    }
    
    /**
     * Update specific setting category
     */
    suspend fun updateGeneralSettings(generalSettings: GeneralGestureSettings) {
        val currentSettings = _settings.value
        val updatedSettings = currentSettings.copy(general = generalSettings.validate())
        saveSettings(updatedSettings)
    }
    
    suspend fun updateSeekingSettings(seekingSettings: SeekingGestureSettings) {
        val currentSettings = _settings.value
        val updatedSettings = currentSettings.copy(seeking = seekingSettings.validate())
        saveSettings(updatedSettings)
    }
    
    suspend fun updateVolumeSettings(volumeSettings: VolumeGestureSettings) {
        val currentSettings = _settings.value
        val updatedSettings = currentSettings.copy(volume = volumeSettings.validate())
        saveSettings(updatedSettings)
    }
    
    suspend fun updateBrightnessSettings(brightnessSettings: BrightnessGestureSettings) {
        val currentSettings = _settings.value
        val updatedSettings = currentSettings.copy(brightness = brightnessSettings.validate())
        saveSettings(updatedSettings)
    }
    
    suspend fun updateDoubleTapSettings(doubleTapSettings: DoubleTapGestureSettings) {
        val currentSettings = _settings.value
        val updatedSettings = currentSettings.copy(doubleTap = doubleTapSettings.validate())
        saveSettings(updatedSettings)
    }
    
    suspend fun updateLongPressSettings(longPressSettings: LongPressGestureSettings) {
        val currentSettings = _settings.value
        val updatedSettings = currentSettings.copy(longPress = longPressSettings.validate())
        saveSettings(updatedSettings)
    }
    
    suspend fun updatePinchZoomSettings(pinchZoomSettings: PinchZoomGestureSettings) {
        val currentSettings = _settings.value
        val updatedSettings = currentSettings.copy(pinchZoom = pinchZoomSettings.validate())
        saveSettings(updatedSettings)
    }
    
    suspend fun updateAdvancedSettings(advancedSettings: AdvancedGestureSettings) {
        val currentSettings = _settings.value
        val updatedSettings = currentSettings.copy(advanced = advancedSettings.validate())
        saveSettings(updatedSettings)
    }
    
    suspend fun updateAccessibilitySettings(accessibilitySettings: AccessibilityGestureSettings) {
        val currentSettings = _settings.value
        val updatedSettings = currentSettings.copy(accessibility = accessibilitySettings.validate())
        saveSettings(updatedSettings)
    }
    
    /**
     * Record gesture usage for adaptive learning
     */
    suspend fun recordGestureUsage(
        gestureType: GestureType,
        duration: Long,
        distance: Float,
        velocity: Float,
        success: Boolean
    ) {
        withContext(Dispatchers.IO) {
            try {
                val currentUsageData = _usageData.value.toMutableMap()
                val existingData = currentUsageData[gestureType] ?: GestureUsageData(gestureType)
                
                val updatedData = existingData.copy(
                    frequency = existingData.frequency + 1,
                    averageDuration = (existingData.averageDuration + duration) / 2,
                    averageDistance = (existingData.averageDistance + distance) / 2,
                    averageVelocity = (existingData.averageVelocity + velocity) / 2,
                    successRate = if (success) {
                        (existingData.successRate * existingData.frequency + 1.0f) / (existingData.frequency + 1)
                    } else {
                        (existingData.successRate * existingData.frequency) / (existingData.frequency + 1)
                    },
                    lastUsed = System.currentTimeMillis()
                )
                
                currentUsageData[gestureType] = updatedData
                
                val usageDataJson = json.encodeToString(currentUsageData)
                prefs.edit().putString(KEY_USAGE_DATA, usageDataJson).apply()
                
                _usageData.value = currentUsageData
                
                // Update adaptive profile if learning is enabled
                updateAdaptiveProfileIfNeeded(gestureType, updatedData)
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to record gesture usage", e)
            }
        }
    }
    
    /**
     * Update adaptive profile based on usage data
     */
    private suspend fun updateAdaptiveProfileIfNeeded(
        gestureType: GestureType,
        usageData: GestureUsageData
    ) {
        val currentProfile = _adaptiveProfile.value
        val currentSettings = _settings.value
        
        if (!currentSettings.advanced.adaptiveLearningEnabled) return
        
        try {
            val deviceType = detectDeviceType()
            val userId = "default_user" // In a real app, this would be the actual user ID
            
            val profile = currentProfile ?: AdaptiveGestureProfile(
                userId = userId,
                deviceType = deviceType,
                adaptedSettings = currentSettings
            )
            
            // Update usage data in profile
            val updatedUsageData = profile.usageData.toMutableMap()
            updatedUsageData[gestureType] = usageData
            
            // Adapt settings based on usage patterns
            val adaptedSettings = adaptSettingsBasedOnUsage(currentSettings, usageData, gestureType)
            
            val updatedProfile = profile.copy(
                usageData = updatedUsageData,
                adaptedSettings = adaptedSettings,
                lastUpdated = System.currentTimeMillis(),
                confidenceLevel = calculateConfidenceLevel(updatedUsageData)
            )
            
            val profileJson = json.encodeToString(updatedProfile)
            prefs.edit().putString(KEY_ADAPTIVE_PROFILE, profileJson).apply()
            
            _adaptiveProfile.value = updatedProfile
            
            // Apply adapted settings if confidence is high enough
            if (updatedProfile.confidenceLevel > 0.7f) {
                saveSettings(adaptedSettings)
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update adaptive profile", e)
        }
    }
    
    /**
     * Adapt settings based on usage patterns
     */
    private fun adaptSettingsBasedOnUsage(
        currentSettings: EnhancedGestureSettings,
        usageData: GestureUsageData,
        gestureType: GestureType
    ): EnhancedGestureSettings {
        // Simple adaptive logic - in a real implementation, this would be more sophisticated
        return when (gestureType) {
            GestureType.HORIZONTAL_SEEK -> {
                val adaptedSensitivity = if (usageData.averageVelocity > 1000f) {
                    (currentSettings.seeking.sensitivity * 0.9f).coerceIn(0.1f, 3.0f)
                } else {
                    (currentSettings.seeking.sensitivity * 1.1f).coerceIn(0.1f, 3.0f)
                }
                currentSettings.copy(
                    seeking = currentSettings.seeking.copy(sensitivity = adaptedSensitivity)
                )
            }
            GestureType.VERTICAL_VOLUME -> {
                val adaptedSensitivity = if (usageData.averageDistance > 200f) {
                    (currentSettings.volume.sensitivity * 0.9f).coerceIn(0.1f, 3.0f)
                } else {
                    (currentSettings.volume.sensitivity * 1.1f).coerceIn(0.1f, 3.0f)
                }
                currentSettings.copy(
                    volume = currentSettings.volume.copy(sensitivity = adaptedSensitivity)
                )
            }
            GestureType.VERTICAL_BRIGHTNESS -> {
                val adaptedSensitivity = if (usageData.averageDistance > 200f) {
                    (currentSettings.brightness.sensitivity * 0.9f).coerceIn(0.1f, 3.0f)
                } else {
                    (currentSettings.brightness.sensitivity * 1.1f).coerceIn(0.1f, 3.0f)
                }
                currentSettings.copy(
                    brightness = currentSettings.brightness.copy(sensitivity = adaptedSensitivity)
                )
            }
            else -> currentSettings
        }
    }
    
    /**
     * Calculate confidence level based on usage data
     */
    private fun calculateConfidenceLevel(usageData: Map<GestureType, GestureUsageData>): Float {
        if (usageData.isEmpty()) return 0f
        
        val totalUsage = usageData.values.sumOf { it.frequency }
        val averageSuccessRate = usageData.values.map { it.successRate }.average().toFloat()
        
        return when {
            totalUsage < 10 -> 0.2f
            totalUsage < 50 -> 0.5f
            totalUsage < 100 -> 0.7f
            else -> (0.8f + averageSuccessRate * 0.2f).coerceAtMost(1.0f)
        }
    }
    
    /**
     * Detect device type for adaptive settings
     */
    private fun detectDeviceType(): DeviceType {
        val displayMetrics = context.resources.displayMetrics
        val screenWidthDp = displayMetrics.widthPixels / displayMetrics.density
        val screenHeightDp = displayMetrics.heightPixels / displayMetrics.density
        val screenSizeInches = kotlin.math.sqrt(
            (screenWidthDp * screenWidthDp + screenHeightDp * screenHeightDp).toDouble()
        ) / 160.0
        
        return when {
            screenSizeInches >= 7.0 -> DeviceType.TABLET
            screenSizeInches >= 6.5 -> DeviceType.FOLDABLE
            else -> DeviceType.PHONE
        }
    }
    
    /**
     * Reset settings to defaults
     */
    suspend fun resetToDefaults() {
        withContext(Dispatchers.IO) {
            try {
                prefs.edit().clear().apply()
                val defaultSettings = EnhancedGestureSettingsFactory.createDefault()
                _settings.value = defaultSettings
                _usageData.value = emptyMap()
                _adaptiveProfile.value = null
                Log.i(TAG, "Settings reset to defaults")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to reset settings", e)
                throw GestureSettingsException("Failed to reset settings", e)
            }
        }
    }
    
    /**
     * Export settings to JSON string
     */
    fun exportSettings(): String {
        return try {
            val exportData = mapOf(
                "settings" to _settings.value,
                "usageData" to _usageData.value,
                "adaptiveProfile" to _adaptiveProfile.value,
                "version" to CURRENT_VERSION
            )
            json.encodeToString(exportData)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to export settings", e)
            throw GestureSettingsException("Failed to export settings", e)
        }
    }
    
    /**
     * Import settings from JSON string
     */
    suspend fun importSettings(settingsJson: String) {
        withContext(Dispatchers.IO) {
            try {
                val importData = json.decodeFromString<Map<String, Any>>(settingsJson)
                
                // Import settings
                val settingsData = importData["settings"]
                if (settingsData != null) {
                    val settings = json.decodeFromString<EnhancedGestureSettings>(
                        json.encodeToString(settingsData)
                    )
                    saveSettings(settings)
                }
                
                // Import usage data
                val usageDataData = importData["usageData"]
                if (usageDataData != null) {
                    val usageData = json.decodeFromString<Map<GestureType, GestureUsageData>>(
                        json.encodeToString(usageDataData)
                    )
                    val usageDataJson = json.encodeToString(usageData)
                    prefs.edit().putString(KEY_USAGE_DATA, usageDataJson).apply()
                    _usageData.value = usageData
                }
                
                // Import adaptive profile
                val profileData = importData["adaptiveProfile"]
                if (profileData != null) {
                    val profile = json.decodeFromString<AdaptiveGestureProfile>(
                        json.encodeToString(profileData)
                    )
                    val profileJson = json.encodeToString(profile)
                    prefs.edit().putString(KEY_ADAPTIVE_PROFILE, profileJson).apply()
                    _adaptiveProfile.value = profile
                }
                
                Log.i(TAG, "Settings imported successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to import settings", e)
                throw GestureSettingsException("Failed to import settings", e)
            }
        }
    }
    
    /**
     * Migrate settings from older versions
     */
    private fun migrateSettingsIfNeeded() {
        val currentVersion = prefs.getInt(KEY_SETTINGS_VERSION, 0)
        if (currentVersion < CURRENT_VERSION) {
            Log.i(TAG, "Migrating settings from version $currentVersion to $CURRENT_VERSION")
            
            // Migration logic would go here
            // For now, we'll just update the version
            prefs.edit().putInt(KEY_SETTINGS_VERSION, CURRENT_VERSION).apply()
        }
    }
    
    /**
     * Get current settings synchronously (for immediate access)
     */
    fun getCurrentSettings(): EnhancedGestureSettings = _settings.value
    
    /**
     * Check if settings are valid
     */
    fun validateSettings(settings: EnhancedGestureSettings): Boolean {
        return try {
            settings.validate()
            true
        } catch (e: Exception) {
            Log.w(TAG, "Settings validation failed", e)
            false
        }
    }
}

/**
 * Exception thrown when gesture settings operations fail
 */
class GestureSettingsException(message: String, cause: Throwable? = null) : Exception(message, cause)