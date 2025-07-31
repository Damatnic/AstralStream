package com.astralplayer.nextplayer.data.gesture

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import com.astralplayer.nextplayer.data.TouchSide
import com.astralplayer.nextplayer.data.EnhancedGestureSettings
import com.astralplayer.nextplayer.data.SeekingGestureSettings
import com.astralplayer.nextplayer.data.VolumeGestureSettings
import com.astralplayer.nextplayer.data.BrightnessGestureSettings
import com.astralplayer.nextplayer.data.DoubleTapGestureSettings
import com.astralplayer.nextplayer.data.LongPressGestureSettings
import com.astralplayer.nextplayer.data.PinchZoomGestureSettings

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "gesture_settings")

class GestureSettingsSerializer(private val context: Context) {
    
    // Gesture enable keys
    private val SWIPE_SEEK_ENABLED = booleanPreferencesKey("swipe_seek_enabled")
    private val VOLUME_GESTURE_ENABLED = booleanPreferencesKey("volume_gesture_enabled")
    private val BRIGHTNESS_GESTURE_ENABLED = booleanPreferencesKey("brightness_gesture_enabled")
    private val DOUBLE_TAP_ENABLED = booleanPreferencesKey("double_tap_enabled")
    private val LONG_PRESS_ENABLED = booleanPreferencesKey("long_press_enabled")
    private val PINCH_ZOOM_ENABLED = booleanPreferencesKey("pinch_zoom_enabled")
    
    // Sensitivity keys
    private val SWIPE_SENSITIVITY = floatPreferencesKey("swipe_sensitivity")
    private val VOLUME_SENSITIVITY = floatPreferencesKey("volume_sensitivity")
    private val BRIGHTNESS_SENSITIVITY = floatPreferencesKey("brightness_sensitivity")
    
    // Seek settings
    private val SEEK_INCREMENT = intPreferencesKey("seek_increment")
    private val LONG_PRESS_SPEED = floatPreferencesKey("long_press_speed")
    
    val gestureSettings: Flow<EnhancedGestureSettings> = context.dataStore.data
        .map { preferences ->
            EnhancedGestureSettings(
                seeking = SeekingGestureSettings(
                    isEnabled = preferences[SWIPE_SEEK_ENABLED] ?: true,
                    sensitivity = preferences[SWIPE_SENSITIVITY] ?: 1.0f,
                    showPreviewThumbnails = true,
                    showTimeIndicator = true,
                    minimumSwipeDistance = 20f,
                    seekStepSize = 5000L,
                    enableFineSeek = true,
                    fineSeekThreshold = 0.3f
                ),
                volume = VolumeGestureSettings(
                    isEnabled = preferences[VOLUME_GESTURE_ENABLED] ?: true,
                    sensitivity = preferences[VOLUME_SENSITIVITY] ?: 1.0f,
                    rightSideOnly = true,
                    showVolumeOverlay = true,
                    systemVolumeIntegration = true
                ),
                brightness = BrightnessGestureSettings(
                    isEnabled = preferences[BRIGHTNESS_GESTURE_ENABLED] ?: true,
                    sensitivity = preferences[BRIGHTNESS_SENSITIVITY] ?: 1.0f,
                    leftSideOnly = true,
                    showBrightnessOverlay = true,
                    systemBrightnessIntegration = true,
                    minimumBrightness = 0.01f,
                    maximumBrightness = 1.0f
                ),
                doubleTap = DoubleTapGestureSettings(
                    isEnabled = preferences[DOUBLE_TAP_ENABLED] ?: true,
                    seekAmount = (preferences[SEEK_INCREMENT] ?: 10) * 1000L,
                    tapTimeout = 300L,
                    maxTapDistance = 50f
                ),
                longPress = LongPressGestureSettings(
                    isEnabled = preferences[LONG_PRESS_ENABLED] ?: true,
                    triggerDuration = 300L,
                    speedProgression = listOf(1f, 2f, 4f, 8f, 16f, 32f),
                    speedAccelerationInterval = 1000L,
                    maxSpeed = 32f,
                    enableDirectionChange = true,
                    directionChangeThreshold = 30f
                ),
                pinchZoom = PinchZoomGestureSettings(
                    isEnabled = preferences[PINCH_ZOOM_ENABLED] ?: true,
                    sensitivity = 1.0f,
                    minZoom = 0.5f,
                    maxZoom = 3.0f,
                    showZoomOverlay = true
                )
            )
        }
    
    suspend fun updateSwipeSeekEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[SWIPE_SEEK_ENABLED] = enabled
        }
    }
    
    suspend fun updateVolumeGestureEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[VOLUME_GESTURE_ENABLED] = enabled
        }
    }
    
    suspend fun updateBrightnessGestureEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[BRIGHTNESS_GESTURE_ENABLED] = enabled
        }
    }
    
    suspend fun updateDoubleTapEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[DOUBLE_TAP_ENABLED] = enabled
        }
    }
    
    suspend fun updateLongPressEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[LONG_PRESS_ENABLED] = enabled
        }
    }
    
    suspend fun updateSeekIncrement(seconds: Int) {
        context.dataStore.edit { preferences ->
            preferences[SEEK_INCREMENT] = seconds
        }
    }
    
    suspend fun updateSwipeSensitivity(sensitivity: Float) {
        context.dataStore.edit { preferences ->
            preferences[SWIPE_SENSITIVITY] = sensitivity
        }
    }
    
    suspend fun updateVolumeSensitivity(sensitivity: Float) {
        context.dataStore.edit { preferences ->
            preferences[VOLUME_SENSITIVITY] = sensitivity
        }
    }
    
    suspend fun updateBrightnessSensitivity(sensitivity: Float) {
        context.dataStore.edit { preferences ->
            preferences[BRIGHTNESS_SENSITIVITY] = sensitivity
        }
    }
}