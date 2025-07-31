package com.astralplayer.nextplayer.gesture

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.gestureDataStore: DataStore<Preferences> by preferencesDataStore(name = "gesture_customizations")

/**
 * Repository for storing and retrieving gesture customizations
 */
@Singleton
class GestureCustomizationRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val gson: Gson
) {
    private val dataStore = context.gestureDataStore
    
    companion object {
        private val GESTURE_MAPPINGS_KEY = stringPreferencesKey("gesture_mappings")
        private val CUSTOM_GESTURES_KEY = stringPreferencesKey("custom_gestures")
        private val GESTURE_SETTINGS_KEY = stringPreferencesKey("gesture_settings")
    }
    
    /**
     * Saves gesture mappings
     */
    suspend fun saveGestureMappings(mappings: Map<GestureType, GestureAction>) {
        dataStore.edit { preferences ->
            val json = gson.toJson(mappings.map { (key, value) ->
                GestureMappingData(key.name, serializeGestureAction(value))
            })
            preferences[GESTURE_MAPPINGS_KEY] = json
        }
    }
    
    /**
     * Gets gesture mappings
     */
    fun getGestureMappings(): Flow<Map<GestureType, GestureAction>> {
        return dataStore.data.map { preferences ->
            val json = preferences[GESTURE_MAPPINGS_KEY]
            if (json.isNullOrEmpty()) {
                getDefaultGestureMappings()
            } else {
                val type = object : TypeToken<List<GestureMappingData>>() {}.type
                val mappingDataList = gson.fromJson<List<GestureMappingData>>(json, type)
                mappingDataList.associate { data ->
                    GestureType.valueOf(data.gestureType) to deserializeGestureAction(data.actionData)
                }
            }
        }
    }
    
    /**
     * Saves custom gestures
     */
    suspend fun saveCustomGestures(gestures: List<CustomGesture>) {
        dataStore.edit { preferences ->
            val json = gson.toJson(gestures)
            preferences[CUSTOM_GESTURES_KEY] = json
        }
    }
    
    /**
     * Gets custom gestures
     */
    fun getCustomGestures(): Flow<List<CustomGesture>> {
        return dataStore.data.map { preferences ->
            val json = preferences[CUSTOM_GESTURES_KEY]
            if (json.isNullOrEmpty()) {
                emptyList()
            } else {
                val type = object : TypeToken<List<CustomGesture>>() {}.type
                gson.fromJson(json, type)
            }
        }
    }
    
    /**
     * Saves gesture settings
     */
    suspend fun saveGestureSettings(settings: GestureSettings) {
        dataStore.edit { preferences ->
            val json = gson.toJson(settings)
            preferences[GESTURE_SETTINGS_KEY] = json
        }
    }
    
    /**
     * Gets gesture settings
     */
    fun getGestureSettings(): Flow<GestureSettings> {
        return dataStore.data.map { preferences ->
            val json = preferences[GESTURE_SETTINGS_KEY]
            if (json.isNullOrEmpty()) {
                GestureSettings()
            } else {
                gson.fromJson(json, GestureSettings::class.java)
            }
        }
    }
    
    /**
     * Resets all gesture customizations to defaults
     */
    suspend fun resetToDefaults() {
        dataStore.edit { preferences ->
            preferences.clear()
        }
    }
    
    private fun getDefaultGestureMappings(): Map<GestureType, GestureAction> {
        return mapOf(
            GestureType.TAP to GestureAction.TogglePlayPause,
            GestureType.DOUBLE_TAP to GestureAction.DoubleTapSeek(10),
            GestureType.DOUBLE_TAP_LEFT to GestureAction.DoubleTapSeek(-10),
            GestureType.DOUBLE_TAP_RIGHT to GestureAction.DoubleTapSeek(10),
            GestureType.SWIPE_UP to GestureAction.VolumeChange(0.1f),
            GestureType.SWIPE_DOWN to GestureAction.VolumeChange(-0.1f),
            GestureType.SWIPE_LEFT to GestureAction.Seek(-10000),
            GestureType.SWIPE_RIGHT to GestureAction.Seek(10000),
            GestureType.LONG_PRESS to GestureAction.LongPressSeek(2.0f),
            GestureType.PINCH_ZOOM_IN to GestureAction.PinchZoom(1.2f),
            GestureType.PINCH_ZOOM_OUT to GestureAction.PinchZoom(0.8f),
            GestureType.TWO_FINGER_SWIPE_UP to GestureAction.BrightnessChange(0.1f),
            GestureType.TWO_FINGER_SWIPE_DOWN to GestureAction.BrightnessChange(-0.1f),
            GestureType.THREE_FINGER_TAP to GestureAction.Custom("toggle_settings"),
            GestureType.THREE_FINGER_SWIPE_LEFT to GestureAction.SwipeNavigation("previous_video"),
            GestureType.THREE_FINGER_SWIPE_RIGHT to GestureAction.SwipeNavigation("next_video")
        )
    }
    
    private fun serializeGestureAction(action: GestureAction): String {
        return gson.toJson(ActionData.fromGestureAction(action))
    }
    
    private fun deserializeGestureAction(data: String): GestureAction {
        val actionData = gson.fromJson(data, ActionData::class.java)
        return actionData.toGestureAction()
    }
    
    /**
     * Data classes for serialization
     */
    private data class GestureMappingData(
        val gestureType: String,
        val actionData: String
    )
    
    private data class ActionData(
        val type: String,
        val value: String? = null,
        val floatValue: Float? = null,
        val intValue: Int? = null
    ) {
        companion object {
            fun fromGestureAction(action: GestureAction): ActionData {
                return when (action) {
                    is GestureAction.Seek -> ActionData("seek", intValue = action.milliseconds)
                    is GestureAction.VolumeChange -> ActionData("volume", floatValue = action.delta)
                    is GestureAction.BrightnessChange -> ActionData("brightness", floatValue = action.delta)
                    is GestureAction.DoubleTapSeek -> ActionData("double_tap_seek", intValue = action.seconds)
                    is GestureAction.TogglePlayPause -> ActionData("toggle_play_pause")
                    is GestureAction.LongPressSeek -> ActionData("long_press_seek", floatValue = action.speedMultiplier)
                    is GestureAction.PinchZoom -> ActionData("pinch_zoom", floatValue = action.scaleFactor)
                    is GestureAction.SwipeNavigation -> ActionData("swipe_nav", value = action.direction)
                    is GestureAction.Custom -> ActionData("custom", value = action.action)
                    is GestureAction.GestureConflict -> ActionData("conflict")
                }
            }
        }
        
        fun toGestureAction(): GestureAction {
            return when (type) {
                "seek" -> GestureAction.Seek(intValue ?: 0)
                "volume" -> GestureAction.VolumeChange(floatValue ?: 0f)
                "brightness" -> GestureAction.BrightnessChange(floatValue ?: 0f)
                "double_tap_seek" -> GestureAction.DoubleTapSeek(intValue ?: 10)
                "toggle_play_pause" -> GestureAction.TogglePlayPause
                "long_press_seek" -> GestureAction.LongPressSeek(floatValue ?: 2f)
                "pinch_zoom" -> GestureAction.PinchZoom(floatValue ?: 1f)
                "swipe_nav" -> GestureAction.SwipeNavigation(value ?: "")
                "custom" -> GestureAction.Custom(value ?: "")
                "conflict" -> GestureAction.GestureConflict
                else -> GestureAction.Custom("unknown")
            }
        }
    }
    
    data class CustomGesture(
        val id: String,
        val name: String,
        val gesture: GestureRecorder.RecordedGesture,
        val action: GestureAction
    )
    
    data class GestureSettings(
        val sensitivityMultiplier: Float = 1.0f,
        val hapticFeedbackEnabled: Boolean = true,
        val hapticIntensity: Float = 1.0f,
        val voiceCommandsEnabled: Boolean = false,
        val gestureVisualizationEnabled: Boolean = true,
        val multiFingerGesturesEnabled: Boolean = true,
        val customGesturesEnabled: Boolean = true
    )
}