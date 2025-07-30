package com.astralplayer.nextplayer.feature.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.astralplayer.nextplayer.feature.player.gestures.GestureSettings
import com.astralplayer.nextplayer.feature.player.gestures.UISettings
import com.astralplayer.nextplayer.feature.player.gestures.PlayBarPosition
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Extension property for DataStore
private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "player_settings")

/**
 * Repository for managing player settings persistence
 */
class SettingsRepository(private val context: Context) {
    
    // Settings keys
    private object Keys {
        // General settings
        val GESTURES_ENABLED = booleanPreferencesKey("gestures_enabled")
        val TAP_TO_TOGGLE_CONTROLS = booleanPreferencesKey("tap_to_toggle_controls")
        val HAPTIC_FEEDBACK_ENABLED = booleanPreferencesKey("haptic_feedback_enabled")
        val VISUAL_FEEDBACK_ENABLED = booleanPreferencesKey("visual_feedback_enabled")
        
        // Gesture sensitivity
        val HORIZONTAL_SENSITIVITY = floatPreferencesKey("horizontal_sensitivity")
        val VERTICAL_SENSITIVITY = floatPreferencesKey("vertical_sensitivity")
        val ZOOM_SENSITIVITY = floatPreferencesKey("zoom_sensitivity")
        
        // Long press settings
        val LONG_PRESS_ENABLED = booleanPreferencesKey("long_press_enabled")
        val LONG_PRESS_DURATION = longPreferencesKey("long_press_duration")
        val LONG_PRESS_DEFAULT_SPEED = floatPreferencesKey("long_press_default_speed")
        val LONG_PRESS_MAX_SPEED = floatPreferencesKey("long_press_max_speed")
        
        // UI settings
        val LANDSCAPE_PLAY_BAR_POSITION = stringPreferencesKey("landscape_play_bar_position")
        val SHOW_LANDSCAPE_PLAY_BAR_ON_SIDES = booleanPreferencesKey("show_landscape_play_bar_on_sides")
    }
    
    /**
     * Get current gesture settings
     */
    val gestureSettings: Flow<GestureSettings> = context.settingsDataStore.data.map { preferences ->
        GestureSettings(
            general = com.astralplayer.nextplayer.feature.player.gestures.GeneralGestureSettings(
                gesturesEnabled = preferences[Keys.GESTURES_ENABLED] ?: true,
                tapToToggleControls = preferences[Keys.TAP_TO_TOGGLE_CONTROLS] ?: true,
                feedbackVibrationsEnabled = preferences[Keys.HAPTIC_FEEDBACK_ENABLED] ?: true,
                visualFeedbackEnabled = preferences[Keys.VISUAL_FEEDBACK_ENABLED] ?: true
            ),
            horizontal = com.astralplayer.nextplayer.feature.player.gestures.HorizontalGestureSettings(
                sensitivity = preferences[Keys.HORIZONTAL_SENSITIVITY] ?: 1f
            ),
            vertical = com.astralplayer.nextplayer.feature.player.gestures.VerticalGestureSettings(
                volumeSensitivity = preferences[Keys.VERTICAL_SENSITIVITY] ?: 1f,
                brightnessSensitivity = preferences[Keys.VERTICAL_SENSITIVITY] ?: 1f
            ),
            longPress = com.astralplayer.nextplayer.feature.player.gestures.LongPressGestureSettings(
                enabled = preferences[Keys.LONG_PRESS_ENABLED] ?: true,
                duration = preferences[Keys.LONG_PRESS_DURATION] ?: 500L,
                defaultSpeed = preferences[Keys.LONG_PRESS_DEFAULT_SPEED] ?: 2f,
                maxSpeed = preferences[Keys.LONG_PRESS_MAX_SPEED] ?: 5f
            ),
            ui = UISettings(
                landscapePlayBarPosition = try {
                    PlayBarPosition.valueOf(preferences[Keys.LANDSCAPE_PLAY_BAR_POSITION] ?: "BOTTOM")
                } catch (e: IllegalArgumentException) {
                    PlayBarPosition.BOTTOM
                },
                showLandscapePlayBarOnSides = preferences[Keys.SHOW_LANDSCAPE_PLAY_BAR_ON_SIDES] ?: false
            )
        )
    }
    
    /**
     * Update gesture settings
     */
    suspend fun updateGestureSettings(settings: GestureSettings) {
        context.settingsDataStore.edit { preferences ->
            preferences[Keys.GESTURES_ENABLED] = settings.general.gesturesEnabled
            preferences[Keys.TAP_TO_TOGGLE_CONTROLS] = settings.general.tapToToggleControls
            preferences[Keys.HAPTIC_FEEDBACK_ENABLED] = settings.general.feedbackVibrationsEnabled
            preferences[Keys.VISUAL_FEEDBACK_ENABLED] = settings.general.visualFeedbackEnabled
            preferences[Keys.HORIZONTAL_SENSITIVITY] = settings.horizontal.sensitivity
            preferences[Keys.VERTICAL_SENSITIVITY] = settings.vertical.volumeSensitivity
            preferences[Keys.LONG_PRESS_ENABLED] = settings.longPress.enabled
            preferences[Keys.LONG_PRESS_DURATION] = settings.longPress.duration
            preferences[Keys.LONG_PRESS_DEFAULT_SPEED] = settings.longPress.defaultSpeed
            preferences[Keys.LONG_PRESS_MAX_SPEED] = settings.longPress.maxSpeed
            preferences[Keys.LANDSCAPE_PLAY_BAR_POSITION] = settings.ui.landscapePlayBarPosition.name
            preferences[Keys.SHOW_LANDSCAPE_PLAY_BAR_ON_SIDES] = settings.ui.showLandscapePlayBarOnSides
        }
    }
}