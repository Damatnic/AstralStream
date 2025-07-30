package com.astralplayer.nextplayer.feature.settings

import android.content.Context
import com.astralplayer.nextplayer.feature.player.gestures.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
/**
 * Manager for gesture settings with validation and presets
 */
class GestureSettingsManager(
    private val settingsRepository: SettingsRepository
) {
    
    /**
     * Get current gesture settings
     */
    val gestureSettings: Flow<GestureSettings> = settingsRepository.gestureSettings
    
    /**
     * Update gesture settings with validation
     */
    suspend fun updateGestureSettings(settings: GestureSettings) {
        val validatedSettings = validateSettings(settings)
        settingsRepository.updateGestureSettings(validatedSettings)
    }
    
    /**
     * Apply sensitivity preset
     */
    suspend fun applySensitivityPreset(preset: GestureSensitivityPreset) {
        val currentSettings = gestureSettings.first()
        val presetSettings = when (preset) {
            GestureSensitivityPreset.LOW -> currentSettings.copy(
                horizontal = currentSettings.horizontal.copy(sensitivity = 0.5f),
                vertical = currentSettings.vertical.copy(
                    volumeSensitivity = 0.5f,
                    brightnessSensitivity = 0.5f
                )
            )
            GestureSensitivityPreset.MEDIUM -> currentSettings.copy(
                horizontal = currentSettings.horizontal.copy(sensitivity = 1.0f),
                vertical = currentSettings.vertical.copy(
                    volumeSensitivity = 1.0f,
                    brightnessSensitivity = 1.0f
                )
            )
            GestureSensitivityPreset.HIGH -> currentSettings.copy(
                horizontal = currentSettings.horizontal.copy(sensitivity = 1.5f),
                vertical = currentSettings.vertical.copy(
                    volumeSensitivity = 1.5f,
                    brightnessSensitivity = 1.5f
                )
            )
            GestureSensitivityPreset.CUSTOM -> currentSettings // No change for custom
        }
        
        updateGestureSettings(presetSettings)
    }
    
    /**
     * Reset to default settings
     */
    suspend fun resetToDefaults() {
        val defaultSettings = GestureSettings()
        updateGestureSettings(defaultSettings)
    }
    
    /**
     * Enable/disable all gestures
     */
    suspend fun setGesturesEnabled(enabled: Boolean) {
        val currentSettings = gestureSettings.first()
        val updatedSettings = currentSettings.copy(
            general = currentSettings.general.copy(gesturesEnabled = enabled)
        )
        updateGestureSettings(updatedSettings)
    }
    
    /**
     * Enable/disable haptic feedback
     */
    suspend fun setHapticFeedbackEnabled(enabled: Boolean) {
        val currentSettings = gestureSettings.first()
        val updatedSettings = currentSettings.copy(
            general = currentSettings.general.copy(feedbackVibrationsEnabled = enabled)
        )
        updateGestureSettings(updatedSettings)
    }
    
    /**
     * Update long press settings
     */
    suspend fun updateLongPressSettings(longPressSettings: LongPressGestureSettings) {
        val currentSettings = gestureSettings.first()
        val updatedSettings = currentSettings.copy(longPress = longPressSettings)
        updateGestureSettings(updatedSettings)
    }
    
    /**
     * Export settings to simple string format
     */
    suspend fun exportSettings(): String {
        val settings = gestureSettings.first()
        return "GestureSettings exported - feature coming soon"
    }
    
    /**
     * Import settings from string format
     */
    suspend fun importSettings(settingsString: String): Boolean {
        // TODO: Implement proper import/export when serialization is added
        return false
    }
    
    /**
     * Validate gesture settings
     */
    private fun validateSettings(settings: GestureSettings): GestureSettings {
        return settings.copy(
            horizontal = settings.horizontal.copy(
                sensitivity = settings.horizontal.sensitivity.coerceIn(0.1f, 3.0f)
            ),
            vertical = settings.vertical.copy(
                volumeSensitivity = settings.vertical.volumeSensitivity.coerceIn(0.1f, 3.0f),
                brightnessSensitivity = settings.vertical.brightnessSensitivity.coerceIn(0.1f, 3.0f)
            ),
            longPress = settings.longPress.copy(
                duration = settings.longPress.duration.coerceIn(300L, 2000L),
                defaultSpeed = settings.longPress.defaultSpeed.coerceIn(0.5f, 10.0f),
                maxSpeed = settings.longPress.maxSpeed.coerceIn(2.0f, 10.0f)
            )
        )
    }
    
    /**
     * Get recommended settings based on device type
     */
    suspend fun getRecommendedSettings(context: Context): GestureSettings {
        val isTablet = context.resources.configuration.smallestScreenWidthDp >= 600
        
        return if (isTablet) {
            // Tablet settings - less sensitive due to larger screen
            GestureSettings().copy(
                horizontal = HorizontalGestureSettings(sensitivity = 0.8f),
                vertical = VerticalGestureSettings(
                    volumeSensitivity = 0.8f,
                    brightnessSensitivity = 0.8f
                )
            )
        } else {
            // Phone settings - default sensitivity
            GestureSettings()
        }
    }
}

/**
 * Gesture sensitivity presets
 */
enum class GestureSensitivityPreset {
    LOW, MEDIUM, HIGH, CUSTOM
}