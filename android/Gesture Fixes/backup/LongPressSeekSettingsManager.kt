package com.astralplayer.nextplayer.feature.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.astralplayer.nextplayer.feature.player.gestures.GestureSettings
import com.astralplayer.nextplayer.feature.player.gestures.LongPressSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Extension property for DataStore
private val Context.longPressSeekDataStore: DataStore<Preferences> by preferencesDataStore(name = "long_press_seek_settings")

/**
 * Settings manager for long press seek functionality
 */
class LongPressSeekSettingsManager(private val context: Context) {
    
    // DataStore keys for long press seek settings
    private object Keys {
        val IS_ENABLED = booleanPreferencesKey("long_press_seek_enabled")
        val DURATION = longPreferencesKey("long_press_duration")
        val DEFAULT_SPEED = floatPreferencesKey("default_speed")
        val MAX_SPEED = floatPreferencesKey("max_speed")
        val MIN_SPEED = floatPreferencesKey("min_speed")
        val DIRECTION_CHANGE_THRESHOLD = floatPreferencesKey("direction_change_threshold")
        val SPEED_CHANGE_THRESHOLD = floatPreferencesKey("speed_change_threshold")
        val CONTINUOUS_SEEK_INTERVAL = longPreferencesKey("continuous_seek_interval")
        val HAPTIC_FEEDBACK_ENABLED = booleanPreferencesKey("haptic_feedback_enabled")
        val SHOW_SPEED_ZONES = booleanPreferencesKey("show_speed_zones")
        val ADAPTIVE_SPEED = booleanPreferencesKey("adaptive_speed")
        val ENABLE_PREVIEW_PLAYBACK = booleanPreferencesKey("enable_preview_playback")
        val AUTO_RESUME_ON_RELEASE = booleanPreferencesKey("auto_resume_on_release")
    }
    
    /**
     * Get the current long press seek settings as a Flow
     */
    val longPressSeekSettings: Flow<LongPressSettings> = context.longPressSeekDataStore.data.map { preferences ->
        LongPressSettings(
            isEnabled = preferences[Keys.IS_ENABLED] ?: true,
            duration = preferences[Keys.DURATION] ?: 300L,
            defaultSpeed = preferences[Keys.DEFAULT_SPEED] ?: 2.0f,
            maxSpeed = preferences[Keys.MAX_SPEED] ?: 5.0f,
            minSpeed = preferences[Keys.MIN_SPEED] ?: 2.0f,
            directionChangeThreshold = preferences[Keys.DIRECTION_CHANGE_THRESHOLD] ?: 50f,
            speedChangeThreshold = preferences[Keys.SPEED_CHANGE_THRESHOLD] ?: 0.25f,
            continuousSeekInterval = preferences[Keys.CONTINUOUS_SEEK_INTERVAL] ?: 50L,
            hapticFeedbackEnabled = preferences[Keys.HAPTIC_FEEDBACK_ENABLED] ?: true,
            showSpeedZones = preferences[Keys.SHOW_SPEED_ZONES] ?: true,
            adaptiveSpeed = preferences[Keys.ADAPTIVE_SPEED] ?: true,
            enablePreviewPlayback = preferences[Keys.ENABLE_PREVIEW_PLAYBACK] ?: true,
            autoResumeOnRelease = preferences[Keys.AUTO_RESUME_ON_RELEASE] ?: true
        )
    }
    
    /**
     * Update the enabled state of long press seek
     */
    suspend fun setLongPressSeekEnabled(enabled: Boolean) {
        context.longPressSeekDataStore.edit { preferences ->
            preferences[Keys.IS_ENABLED] = enabled
        }
    }
    
    /**
     * Update the long press duration
     */
    suspend fun setLongPressDuration(duration: Long) {
        context.longPressSeekDataStore.edit { preferences ->
            preferences[Keys.DURATION] = duration
        }
    }
    
    /**
     * Update the default speed
     */
    suspend fun setDefaultSpeed(speed: Float) {
        context.longPressSeekDataStore.edit { preferences ->
            preferences[Keys.DEFAULT_SPEED] = speed
        }
    }
    
    /**
     * Update the maximum speed
     */
    suspend fun setMaxSpeed(speed: Float) {
        context.longPressSeekDataStore.edit { preferences ->
            preferences[Keys.MAX_SPEED] = speed
        }
    }
    
    /**
     * Update the minimum speed
     */
    suspend fun setMinSpeed(speed: Float) {
        context.longPressSeekDataStore.edit { preferences ->
            preferences[Keys.MIN_SPEED] = speed
        }
    }
    
    /**
     * Update the direction change threshold
     */
    suspend fun setDirectionChangeThreshold(threshold: Float) {
        context.longPressSeekDataStore.edit { preferences ->
            preferences[Keys.DIRECTION_CHANGE_THRESHOLD] = threshold
        }
    }
    
    /**
     * Update the speed change threshold
     */
    suspend fun setSpeedChangeThreshold(threshold: Float) {
        context.longPressSeekDataStore.edit { preferences ->
            preferences[Keys.SPEED_CHANGE_THRESHOLD] = threshold
        }
    }
    
    /**
     * Update the continuous seek interval
     */
    suspend fun setContinuousSeekInterval(interval: Long) {
        context.longPressSeekDataStore.edit { preferences ->
            preferences[Keys.CONTINUOUS_SEEK_INTERVAL] = interval
        }
    }
    
    /**
     * Update the haptic feedback enabled state
     */
    suspend fun setHapticFeedbackEnabled(enabled: Boolean) {
        context.longPressSeekDataStore.edit { preferences ->
            preferences[Keys.HAPTIC_FEEDBACK_ENABLED] = enabled
        }
    }
    
    /**
     * Update the show speed zones state
     */
    suspend fun setShowSpeedZones(enabled: Boolean) {
        context.longPressSeekDataStore.edit { preferences ->
            preferences[Keys.SHOW_SPEED_ZONES] = enabled
        }
    }
    
    /**
     * Update the adaptive speed state
     */
    suspend fun setAdaptiveSpeed(enabled: Boolean) {
        context.longPressSeekDataStore.edit { preferences ->
            preferences[Keys.ADAPTIVE_SPEED] = enabled
        }
    }
    
    /**
     * Apply a preset configuration
     */
    suspend fun applyPreset(preset: LongPressSeekPreset) {
        context.longPressSeekDataStore.edit { preferences ->
            when (preset) {
                LongPressSeekPreset.MX_PLAYER -> {
                    preferences[Keys.DURATION] = 300L
                    preferences[Keys.DEFAULT_SPEED] = 2.0f
                    preferences[Keys.MAX_SPEED] = 5.0f
                    preferences[Keys.MIN_SPEED] = 2.0f
                    preferences[Keys.DIRECTION_CHANGE_THRESHOLD] = 50f
                    preferences[Keys.SPEED_CHANGE_THRESHOLD] = 0.25f
                    preferences[Keys.CONTINUOUS_SEEK_INTERVAL] = 50L
                    preferences[Keys.ADAPTIVE_SPEED] = true
                    preferences[Keys.ENABLE_PREVIEW_PLAYBACK] = true
                    preferences[Keys.AUTO_RESUME_ON_RELEASE] = true
                }
                LongPressSeekPreset.SMOOTH -> {
                    preferences[Keys.DURATION] = 500L
                    preferences[Keys.DEFAULT_SPEED] = 2.0f
                    preferences[Keys.MAX_SPEED] = 4.0f
                    preferences[Keys.MIN_SPEED] = 2.0f
                    preferences[Keys.DIRECTION_CHANGE_THRESHOLD] = 60f
                    preferences[Keys.SPEED_CHANGE_THRESHOLD] = 0.33f
                    preferences[Keys.CONTINUOUS_SEEK_INTERVAL] = 100L
                    preferences[Keys.ADAPTIVE_SPEED] = false
                    preferences[Keys.ENABLE_PREVIEW_PLAYBACK] = true
                    preferences[Keys.AUTO_RESUME_ON_RELEASE] = true
                }
                LongPressSeekPreset.FAST -> {
                    preferences[Keys.DURATION] = 200L
                    preferences[Keys.DEFAULT_SPEED] = 2.0f
                    preferences[Keys.MAX_SPEED] = 8.0f
                    preferences[Keys.MIN_SPEED] = 2.0f
                    preferences[Keys.DIRECTION_CHANGE_THRESHOLD] = 30f
                    preferences[Keys.SPEED_CHANGE_THRESHOLD] = 0.2f
                    preferences[Keys.CONTINUOUS_SEEK_INTERVAL] = 25L
                    preferences[Keys.ADAPTIVE_SPEED] = true
                    preferences[Keys.ENABLE_PREVIEW_PLAYBACK] = true
                    preferences[Keys.AUTO_RESUME_ON_RELEASE] = true
                }
            }
        }
    }
    
    /**
     * Reset all settings to default values
     */
    suspend fun resetToDefaults() {
        context.longPressSeekDataStore.edit { preferences ->
            preferences.clear()
        }
    }
    
    /**
     * Update all settings at once
     */
    suspend fun updateAllSettings(settings: LongPressSettings) {
        context.longPressSeekDataStore.edit { preferences ->
            preferences[Keys.IS_ENABLED] = settings.isEnabled
            preferences[Keys.DURATION] = settings.duration
            preferences[Keys.DEFAULT_SPEED] = settings.defaultSpeed
            preferences[Keys.MAX_SPEED] = settings.maxSpeed
            preferences[Keys.MIN_SPEED] = settings.minSpeed
            preferences[Keys.DIRECTION_CHANGE_THRESHOLD] = settings.directionChangeThreshold
            preferences[Keys.SPEED_CHANGE_THRESHOLD] = settings.speedChangeThreshold
            preferences[Keys.CONTINUOUS_SEEK_INTERVAL] = settings.continuousSeekInterval
            preferences[Keys.HAPTIC_FEEDBACK_ENABLED] = settings.hapticFeedbackEnabled
            preferences[Keys.SHOW_SPEED_ZONES] = settings.showSpeedZones
            preferences[Keys.ADAPTIVE_SPEED] = settings.adaptiveSpeed
            preferences[Keys.ENABLE_PREVIEW_PLAYBACK] = settings.enablePreviewPlayback
            preferences[Keys.AUTO_RESUME_ON_RELEASE] = settings.autoResumeOnRelease
        }
    }
}

/**
 * Preset configurations for long press seek
 */
enum class LongPressSeekPreset {
    MX_PLAYER,  // MX Player-style configuration
    SMOOTH,     // Smooth and controlled seeking
    FAST        // Fast and responsive seeking
}

/**
 * Extension function to get the settings manager from context
 */
fun Context.getLongPressSeekSettingsManager(): LongPressSeekSettingsManager {
    return LongPressSeekSettingsManager(this)
} 