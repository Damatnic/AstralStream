package com.astralplayer.nextplayer.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.io.IOException

/**
 * Handles serialization and persistence of enhanced gesture settings
 */
class GestureSettingsSerializer(private val context: Context) {
    
    private val Context.gestureDataStore: DataStore<Preferences> by preferencesDataStore(name = "gesture_settings")
    
    private val json = Json { 
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }
    
    // Preference keys for enhanced gesture settings
    private val GESTURE_SETTINGS_JSON = stringPreferencesKey("enhanced_gesture_settings_json")
    
    // Legacy preference keys for migration
    private val LEGACY_GESTURES_ENABLED = booleanPreferencesKey("enable_gestures")
    private val LEGACY_HORIZONTAL_SEEK = booleanPreferencesKey("horizontal_seek_enabled")
    private val LEGACY_VERTICAL_VOLUME = booleanPreferencesKey("vertical_volume_enabled")
    private val LEGACY_VERTICAL_BRIGHTNESS = booleanPreferencesKey("vertical_brightness_enabled")
    private val LEGACY_DOUBLE_TAP_SEEK = booleanPreferencesKey("double_tap_seek")
    private val LEGACY_LONG_PRESS_SPEED = booleanPreferencesKey("long_press_speed")
    private val LEGACY_SEEK_SENSITIVITY = floatPreferencesKey("seek_sensitivity")
    private val LEGACY_VOLUME_SENSITIVITY = floatPreferencesKey("volume_sensitivity")
    private val LEGACY_BRIGHTNESS_SENSITIVITY = floatPreferencesKey("brightness_sensitivity")
    private val LEGACY_DOUBLE_TAP_SEEK_AMOUNT = longPreferencesKey("double_tap_seek_amount")
    
    /**
     * Saves enhanced gesture settings to persistent storage
     */
    suspend fun saveSettings(settings: EnhancedGestureSettings) {
        try {
            // Validate settings before saving
            val validatedSettings = settings.validate()
            
            // Serialize to JSON
            val settingsJson = json.encodeToString(
                EnhancedGestureSettingsData.fromSettings(validatedSettings)
            )
            
            // Save to DataStore
            context.gestureDataStore.edit { preferences ->
                preferences[GESTURE_SETTINGS_JSON] = settingsJson
            }
        } catch (e: Exception) {
            // Log error but don't crash
            e.printStackTrace()
        }
    }
    
    /**
     * Loads enhanced gesture settings from persistent storage
     */
    val settings: Flow<EnhancedGestureSettings> = context.gestureDataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            try {
                // Try to load from JSON first
                val settingsJson = preferences[GESTURE_SETTINGS_JSON]
                if (settingsJson != null) {
                    val settingsData = json.decodeFromString<EnhancedGestureSettingsData>(settingsJson)
                    settingsData.toSettings().validate()
                } else {
                    // Fall back to migration from legacy settings
                    migrateFromLegacySettings(preferences)
                }
            } catch (e: Exception) {
                // Return default settings on error
                e.printStackTrace()
                EnhancedGestureSettings()
            }
        }
    
    /**
     * Migrates from legacy settings to enhanced settings
     */
    private suspend fun migrateFromLegacySettings(preferences: Preferences): EnhancedGestureSettings {
        val legacySettings = EnhancedGestureSettings(
            general = GeneralGestureSettings(
                isEnabled = preferences[LEGACY_GESTURES_ENABLED] ?: true
            ),
            seeking = SeekingGestureSettings(
                isEnabled = preferences[LEGACY_HORIZONTAL_SEEK] ?: true,
                sensitivity = preferences[LEGACY_SEEK_SENSITIVITY] ?: 1.0f
            ),
            volume = VolumeGestureSettings(
                isEnabled = preferences[LEGACY_VERTICAL_VOLUME] ?: true,
                sensitivity = preferences[LEGACY_VOLUME_SENSITIVITY] ?: 1.0f
            ),
            brightness = BrightnessGestureSettings(
                isEnabled = preferences[LEGACY_VERTICAL_BRIGHTNESS] ?: true,
                sensitivity = preferences[LEGACY_BRIGHTNESS_SENSITIVITY] ?: 1.0f
            ),
            doubleTap = DoubleTapGestureSettings(
                isEnabled = preferences[LEGACY_DOUBLE_TAP_SEEK] ?: true,
                seekAmount = preferences[LEGACY_DOUBLE_TAP_SEEK_AMOUNT] ?: 10000L
            ),
            longPress = LongPressGestureSettings(
                isEnabled = preferences[LEGACY_LONG_PRESS_SPEED] ?: true
            )
        )
        
        // Save migrated settings
        saveSettings(legacySettings)
        
        return legacySettings
    }
    
    /**
     * Clears all gesture settings
     */
    suspend fun clearSettings() {
        context.gestureDataStore.edit { preferences ->
            preferences.clear()
        }
    }
    
    /**
     * Exports settings to JSON string
     */
    suspend fun exportSettings(settings: EnhancedGestureSettings): String {
        val validatedSettings = settings.validate()
        return json.encodeToString(
            EnhancedGestureSettingsData.fromSettings(validatedSettings)
        )
    }
    
    /**
     * Imports settings from JSON string
     */
    suspend fun importSettings(jsonString: String): EnhancedGestureSettings? {
        return try {
            val settingsData = json.decodeFromString<EnhancedGestureSettingsData>(jsonString)
            val settings = settingsData.toSettings().validate()
            saveSettings(settings)
            settings
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}

/**
 * Data class for JSON serialization of enhanced gesture settings
 */
@kotlinx.serialization.Serializable
private data class EnhancedGestureSettingsData(
    val general: GeneralGestureSettingsData,
    val seeking: SeekingGestureSettingsData,
    val volume: VolumeGestureSettingsData,
    val brightness: BrightnessGestureSettingsData,
    val doubleTap: DoubleTapGestureSettingsData,
    val longPress: LongPressGestureSettingsData,
    val pinchZoom: PinchZoomGestureSettingsData
) {
    fun toSettings() = EnhancedGestureSettings(
        general = general.toSettings(),
        seeking = seeking.toSettings(),
        volume = volume.toSettings(),
        brightness = brightness.toSettings(),
        doubleTap = doubleTap.toSettings(),
        longPress = longPress.toSettings(),
        pinchZoom = pinchZoom.toSettings()
    )
    
    companion object {
        fun fromSettings(settings: EnhancedGestureSettings) = EnhancedGestureSettingsData(
            general = GeneralGestureSettingsData.fromSettings(settings.general),
            seeking = SeekingGestureSettingsData.fromSettings(settings.seeking),
            volume = VolumeGestureSettingsData.fromSettings(settings.volume),
            brightness = BrightnessGestureSettingsData.fromSettings(settings.brightness),
            doubleTap = DoubleTapGestureSettingsData.fromSettings(settings.doubleTap),
            longPress = LongPressGestureSettingsData.fromSettings(settings.longPress),
            pinchZoom = PinchZoomGestureSettingsData.fromSettings(settings.pinchZoom)
        )
    }
}

@kotlinx.serialization.Serializable
private data class GeneralGestureSettingsData(
    val isEnabled: Boolean,
    val conflictResolutionEnabled: Boolean,
    val gestureDeadZone: Float,
    val minimumGestureDistance: Float
) {
    fun toSettings() = GeneralGestureSettings(
        isEnabled = isEnabled,
        conflictResolutionEnabled = conflictResolutionEnabled,
        gestureDeadZone = gestureDeadZone,
        minimumGestureDistance = minimumGestureDistance
    )
    
    companion object {
        fun fromSettings(settings: GeneralGestureSettings) = GeneralGestureSettingsData(
            isEnabled = settings.isEnabled,
            conflictResolutionEnabled = settings.conflictResolutionEnabled,
            gestureDeadZone = settings.gestureDeadZone,
            minimumGestureDistance = settings.minimumGestureDistance
        )
    }
}

@kotlinx.serialization.Serializable
private data class SeekingGestureSettingsData(
    val isEnabled: Boolean,
    val sensitivity: Float,
    val showPreviewThumbnails: Boolean,
    val showTimeIndicator: Boolean,
    val minimumSwipeDistance: Float,
    val seekStepSize: Long,
    val enableFineSeek: Boolean,
    val fineSeekThreshold: Float
) {
    fun toSettings() = SeekingGestureSettings(
        isEnabled = isEnabled,
        sensitivity = sensitivity,
        showPreviewThumbnails = showPreviewThumbnails,
        showTimeIndicator = showTimeIndicator,
        minimumSwipeDistance = minimumSwipeDistance,
        seekStepSize = seekStepSize,
        enableFineSeek = enableFineSeek,
        fineSeekThreshold = fineSeekThreshold
    )
    
    companion object {
        fun fromSettings(settings: SeekingGestureSettings) = SeekingGestureSettingsData(
            isEnabled = settings.isEnabled,
            sensitivity = settings.sensitivity,
            showPreviewThumbnails = settings.showPreviewThumbnails,
            showTimeIndicator = settings.showTimeIndicator,
            minimumSwipeDistance = settings.minimumSwipeDistance,
            seekStepSize = settings.seekStepSize,
            enableFineSeek = settings.enableFineSeek,
            fineSeekThreshold = settings.fineSeekThreshold
        )
    }
}

@kotlinx.serialization.Serializable
private data class VolumeGestureSettingsData(
    val isEnabled: Boolean,
    val sensitivity: Float,
    val rightSideOnly: Boolean,
    val showVolumeOverlay: Boolean,
    val systemVolumeIntegration: Boolean
) {
    fun toSettings() = VolumeGestureSettings(
        isEnabled = isEnabled,
        sensitivity = sensitivity,
        rightSideOnly = rightSideOnly,
        showVolumeOverlay = showVolumeOverlay,
        systemVolumeIntegration = systemVolumeIntegration
    )
    
    companion object {
        fun fromSettings(settings: VolumeGestureSettings) = VolumeGestureSettingsData(
            isEnabled = settings.isEnabled,
            sensitivity = settings.sensitivity,
            rightSideOnly = settings.rightSideOnly,
            showVolumeOverlay = settings.showVolumeOverlay,
            systemVolumeIntegration = settings.systemVolumeIntegration
        )
    }
}

@kotlinx.serialization.Serializable
private data class BrightnessGestureSettingsData(
    val isEnabled: Boolean,
    val sensitivity: Float,
    val leftSideOnly: Boolean,
    val showBrightnessOverlay: Boolean,
    val systemBrightnessIntegration: Boolean,
    val minimumBrightness: Float,
    val maximumBrightness: Float
) {
    fun toSettings() = BrightnessGestureSettings(
        isEnabled = isEnabled,
        sensitivity = sensitivity,
        leftSideOnly = leftSideOnly,
        showBrightnessOverlay = showBrightnessOverlay,
        systemBrightnessIntegration = systemBrightnessIntegration,
        minimumBrightness = minimumBrightness,
        maximumBrightness = maximumBrightness
    )
    
    companion object {
        fun fromSettings(settings: BrightnessGestureSettings) = BrightnessGestureSettingsData(
            isEnabled = settings.isEnabled,
            sensitivity = settings.sensitivity,
            leftSideOnly = settings.leftSideOnly,
            showBrightnessOverlay = settings.showBrightnessOverlay,
            systemBrightnessIntegration = settings.systemBrightnessIntegration,
            minimumBrightness = settings.minimumBrightness,
            maximumBrightness = settings.maximumBrightness
        )
    }
}

@kotlinx.serialization.Serializable
private data class DoubleTapGestureSettingsData(
    val isEnabled: Boolean,
    val seekAmount: Long,
    val tapTimeout: Long,
    val maxTapDistance: Float
) {
    fun toSettings() = DoubleTapGestureSettings(
        isEnabled = isEnabled,
        seekAmount = seekAmount,
        tapTimeout = tapTimeout,
        maxTapDistance = maxTapDistance
    )
    
    companion object {
        fun fromSettings(settings: DoubleTapGestureSettings) = DoubleTapGestureSettingsData(
            isEnabled = settings.isEnabled,
            seekAmount = settings.seekAmount,
            tapTimeout = settings.tapTimeout,
            maxTapDistance = settings.maxTapDistance
        )
    }
}

@kotlinx.serialization.Serializable
private data class LongPressGestureSettingsData(
    val isEnabled: Boolean,
    val triggerDuration: Long,
    val speedProgression: List<Float>,
    val speedAccelerationInterval: Long,
    val maxSpeed: Float,
    val enableDirectionChange: Boolean,
    val directionChangeThreshold: Float
) {
    fun toSettings() = LongPressGestureSettings(
        isEnabled = isEnabled,
        triggerDuration = triggerDuration,
        speedProgression = speedProgression,
        speedAccelerationInterval = speedAccelerationInterval,
        maxSpeed = maxSpeed,
        enableDirectionChange = enableDirectionChange,
        directionChangeThreshold = directionChangeThreshold
    )
    
    companion object {
        fun fromSettings(settings: LongPressGestureSettings) = LongPressGestureSettingsData(
            isEnabled = settings.isEnabled,
            triggerDuration = settings.triggerDuration,
            speedProgression = settings.speedProgression,
            speedAccelerationInterval = settings.speedAccelerationInterval,
            maxSpeed = settings.maxSpeed,
            enableDirectionChange = settings.enableDirectionChange,
            directionChangeThreshold = settings.directionChangeThreshold
        )
    }
}

@kotlinx.serialization.Serializable
private data class PinchZoomGestureSettingsData(
    val isEnabled: Boolean,
    val sensitivity: Float,
    val minZoom: Float,
    val maxZoom: Float,
    val showZoomOverlay: Boolean
) {
    fun toSettings() = PinchZoomGestureSettings(
        isEnabled = isEnabled,
        sensitivity = sensitivity,
        minZoom = minZoom,
        maxZoom = maxZoom,
        showZoomOverlay = showZoomOverlay
    )
    
    companion object {
        fun fromSettings(settings: PinchZoomGestureSettings) = PinchZoomGestureSettingsData(
            isEnabled = settings.isEnabled,
            sensitivity = settings.sensitivity,
            minZoom = settings.minZoom,
            maxZoom = settings.maxZoom,
            showZoomOverlay = settings.showZoomOverlay
        )
    }
}