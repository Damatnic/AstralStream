package com.astralplayer.nextplayer.feature.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

val Context.longPressSeekDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "long_press_seek_settings"
)

/**
 * Data class representing long press seek settings
 */
data class LongPressSeekSettings(
    val isEnabled: Boolean = true,
    val defaultSpeed: Float = 2.0f,
    val maxSpeed: Float = 5.0f,
    val hapticFeedbackEnabled: Boolean = true,
    val showSpeedIndicator: Boolean = true,
    val enablePreviewPlayback: Boolean = true,
    val swipeThreshold: Float = 50f,
    val continuousSeekInterval: Long = 50L
)

/**
 * Manager class for persisting and retrieving long press seek settings
 */
class LongPressSeekSettingsManager(private val context: Context) {
    
    companion object {
        private val ENABLED_KEY = booleanPreferencesKey("long_press_seek_enabled")
        private val DEFAULT_SPEED_KEY = floatPreferencesKey("default_seek_speed")
        private val MAX_SPEED_KEY = floatPreferencesKey("max_seek_speed")
        private val HAPTIC_FEEDBACK_KEY = booleanPreferencesKey("haptic_feedback_enabled")
        private val SHOW_SPEED_INDICATOR_KEY = booleanPreferencesKey("show_speed_indicator")
        private val PREVIEW_PLAYBACK_KEY = booleanPreferencesKey("enable_preview_playback")
        private val SWIPE_THRESHOLD_KEY = floatPreferencesKey("swipe_threshold")
        private val SEEK_INTERVAL_KEY = longPreferencesKey("continuous_seek_interval")
    }
    
    /**
     * Flow of long press seek settings
     */
    val longPressSeekSettings: Flow<LongPressSeekSettings> = context.longPressSeekDataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            LongPressSeekSettings(
                isEnabled = preferences[ENABLED_KEY] ?: true,
                defaultSpeed = preferences[DEFAULT_SPEED_KEY] ?: 2.0f,
                maxSpeed = preferences[MAX_SPEED_KEY] ?: 5.0f,
                hapticFeedbackEnabled = preferences[HAPTIC_FEEDBACK_KEY] ?: true,
                showSpeedIndicator = preferences[SHOW_SPEED_INDICATOR_KEY] ?: true,
                enablePreviewPlayback = preferences[PREVIEW_PLAYBACK_KEY] ?: true,
                swipeThreshold = preferences[SWIPE_THRESHOLD_KEY] ?: 50f,
                continuousSeekInterval = preferences[SEEK_INTERVAL_KEY] ?: 50L
            )
        }
    
    /**
     * Update long press seek settings
     */
    suspend fun updateLongPressSeekSettings(settings: LongPressSeekSettings) {
        context.longPressSeekDataStore.edit { preferences ->
            preferences[ENABLED_KEY] = settings.isEnabled
            preferences[DEFAULT_SPEED_KEY] = settings.defaultSpeed
            preferences[MAX_SPEED_KEY] = settings.maxSpeed
            preferences[HAPTIC_FEEDBACK_KEY] = settings.hapticFeedbackEnabled
            preferences[SHOW_SPEED_INDICATOR_KEY] = settings.showSpeedIndicator
            preferences[PREVIEW_PLAYBACK_KEY] = settings.enablePreviewPlayback
            preferences[SWIPE_THRESHOLD_KEY] = settings.swipeThreshold
            preferences[SEEK_INTERVAL_KEY] = settings.continuousSeekInterval
        }
    }
    
    /**
     * Update a single setting
     */
    suspend fun updateSetting(update: (LongPressSeekSettings) -> LongPressSeekSettings) {
        val currentSettings = longPressSeekSettings.map { it }.catch { 
            emit(LongPressSeekSettings()) 
        }.collect { settings ->
            updateLongPressSeekSettings(update(settings))
        }
    }
}