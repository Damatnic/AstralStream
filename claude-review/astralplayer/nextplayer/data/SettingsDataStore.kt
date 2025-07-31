package com.astralplayer.nextplayer.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsDataStore constructor(
    private val context: Context
) {
    // Playback preferences
    private val AUTO_PLAY_NEXT = booleanPreferencesKey("auto_play_next")
    private val REMEMBER_POSITION = booleanPreferencesKey("remember_position")
    private val HARDWARE_ACCELERATION = booleanPreferencesKey("hardware_acceleration")
    
    // Gesture preferences
    private val ENABLE_GESTURES = booleanPreferencesKey("enable_gestures")
    private val DOUBLE_TAP_SEEK = booleanPreferencesKey("double_tap_seek")
    private val LONG_PRESS_SPEED = booleanPreferencesKey("long_press_speed")
    private val SWIPE_VERTICAL_LEFT = stringPreferencesKey("swipe_vertical_left")
    private val SWIPE_VERTICAL_RIGHT = stringPreferencesKey("swipe_vertical_right")
    private val SWIPE_HORIZONTAL = stringPreferencesKey("swipe_horizontal")
    private val DOUBLE_TAP_SEEK_DURATION = intPreferencesKey("double_tap_seek_duration")
    
    // Display preferences
    private val KEEP_SCREEN_ON = booleanPreferencesKey("keep_screen_on")
    private val SHOW_VIDEO_INFO = booleanPreferencesKey("show_video_info")
    private val AUTO_LOAD_SUBTITLES = booleanPreferencesKey("auto_load_subtitles")
    private val DEFAULT_ZOOM_MODE = stringPreferencesKey("default_zoom_mode")
    
    // Network preferences
    private val WIFI_ONLY_STREAMING = booleanPreferencesKey("wifi_only_streaming")
    private val BUFFER_SIZE = stringPreferencesKey("buffer_size")
    
    // Audio preferences
    private val AUDIO_DELAY = intPreferencesKey("audio_delay")
    private val DEFAULT_AUDIO_TRACK = stringPreferencesKey("default_audio_track")
    
    // Subtitle preferences
    private val SUBTITLE_SIZE = floatPreferencesKey("subtitle_size")
    private val SUBTITLE_COLOR = stringPreferencesKey("subtitle_color")
    private val SUBTITLE_BACKGROUND = booleanPreferencesKey("subtitle_background")
    
    // UI preferences
    private val THEME_MODE = stringPreferencesKey("theme_mode")
    private val ACCENT_COLOR = stringPreferencesKey("accent_color")
    
    // Playback preferences flows
    val autoPlayNext: Flow<Boolean> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences())
            else throw exception
        }
        .map { preferences ->
            preferences[AUTO_PLAY_NEXT] ?: false
        }
    
    val rememberPosition: Flow<Boolean> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences())
            else throw exception
        }
        .map { preferences ->
            preferences[REMEMBER_POSITION] ?: true
        }
    
    val hardwareAcceleration: Flow<Boolean> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences())
            else throw exception
        }
        .map { preferences ->
            preferences[HARDWARE_ACCELERATION] ?: true
        }
    
    // Gesture preferences flows
    val enableGestures: Flow<Boolean> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences())
            else throw exception
        }
        .map { preferences ->
            preferences[ENABLE_GESTURES] ?: true
        }
    
    val doubleTapSeek: Flow<Boolean> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences())
            else throw exception
        }
        .map { preferences ->
            preferences[DOUBLE_TAP_SEEK] ?: true
        }
    
    val longPressSpeed: Flow<Boolean> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences())
            else throw exception
        }
        .map { preferences ->
            preferences[LONG_PRESS_SPEED] ?: true
        }
    
    val doubleTapSeekDuration: Flow<Int> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences())
            else throw exception
        }
        .map { preferences ->
            preferences[DOUBLE_TAP_SEEK_DURATION] ?: 10
        }
    
    // Display preferences flows
    val keepScreenOn: Flow<Boolean> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences())
            else throw exception
        }
        .map { preferences ->
            preferences[KEEP_SCREEN_ON] ?: true
        }
    
    val showVideoInfo: Flow<Boolean> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences())
            else throw exception
        }
        .map { preferences ->
            preferences[SHOW_VIDEO_INFO] ?: false
        }
    
    val autoLoadSubtitles: Flow<Boolean> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences())
            else throw exception
        }
        .map { preferences ->
            preferences[AUTO_LOAD_SUBTITLES] ?: true
        }
    
    // Network preferences flows
    val wifiOnlyStreaming: Flow<Boolean> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences())
            else throw exception
        }
        .map { preferences ->
            preferences[WIFI_ONLY_STREAMING] ?: false
        }
    
    val bufferSize: Flow<String> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences())
            else throw exception
        }
        .map { preferences ->
            preferences[BUFFER_SIZE] ?: "Medium"
        }
    
    // Update functions
    suspend fun updateAutoPlayNext(value: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[AUTO_PLAY_NEXT] = value
        }
    }
    
    suspend fun updateRememberPosition(value: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[REMEMBER_POSITION] = value
        }
    }
    
    suspend fun updateHardwareAcceleration(value: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[HARDWARE_ACCELERATION] = value
        }
    }
    
    suspend fun updateEnableGestures(value: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[ENABLE_GESTURES] = value
        }
    }
    
    suspend fun updateDoubleTapSeek(value: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[DOUBLE_TAP_SEEK] = value
        }
    }
    
    suspend fun updateLongPressSpeed(value: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[LONG_PRESS_SPEED] = value
        }
    }
    
    suspend fun updateDoubleTapSeekDuration(value: Int) {
        context.dataStore.edit { preferences ->
            preferences[DOUBLE_TAP_SEEK_DURATION] = value
        }
    }
    
    suspend fun updateKeepScreenOn(value: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEEP_SCREEN_ON] = value
        }
    }
    
    suspend fun updateShowVideoInfo(value: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[SHOW_VIDEO_INFO] = value
        }
    }
    
    suspend fun updateAutoLoadSubtitles(value: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[AUTO_LOAD_SUBTITLES] = value
        }
    }
    
    suspend fun updateWifiOnlyStreaming(value: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[WIFI_ONLY_STREAMING] = value
        }
    }
    
    suspend fun updateBufferSize(value: String) {
        context.dataStore.edit { preferences ->
            preferences[BUFFER_SIZE] = value
        }
    }
    
    // Gesture action preferences
    suspend fun updateSwipeVerticalLeft(action: String) {
        context.dataStore.edit { preferences ->
            preferences[SWIPE_VERTICAL_LEFT] = action
        }
    }
    
    suspend fun updateSwipeVerticalRight(action: String) {
        context.dataStore.edit { preferences ->
            preferences[SWIPE_VERTICAL_RIGHT] = action
        }
    }
    
    suspend fun updateSwipeHorizontal(action: String) {
        context.dataStore.edit { preferences ->
            preferences[SWIPE_HORIZONTAL] = action
        }
    }
    
    // Get all preferences as data class
    data class Settings(
        val autoPlayNext: Boolean = false,
        val rememberPosition: Boolean = true,
        val hardwareAcceleration: Boolean = true,
        val enableGestures: Boolean = true,
        val doubleTapSeek: Boolean = true,
        val longPressSpeed: Boolean = true,
        val doubleTapSeekDuration: Int = 10,
        val keepScreenOn: Boolean = true,
        val showVideoInfo: Boolean = false,
        val autoLoadSubtitles: Boolean = true,
        val wifiOnlyStreaming: Boolean = false,
        val bufferSize: String = "Medium",
        val swipeVerticalLeft: String = "volume",
        val swipeVerticalRight: String = "brightness",
        val swipeHorizontal: String = "seek"
    )
    
    val settings: Flow<Settings> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences())
            else throw exception
        }
        .map { preferences ->
            Settings(
                autoPlayNext = preferences[AUTO_PLAY_NEXT] ?: false,
                rememberPosition = preferences[REMEMBER_POSITION] ?: true,
                hardwareAcceleration = preferences[HARDWARE_ACCELERATION] ?: true,
                enableGestures = preferences[ENABLE_GESTURES] ?: true,
                doubleTapSeek = preferences[DOUBLE_TAP_SEEK] ?: true,
                longPressSpeed = preferences[LONG_PRESS_SPEED] ?: true,
                doubleTapSeekDuration = preferences[DOUBLE_TAP_SEEK_DURATION] ?: 10,
                keepScreenOn = preferences[KEEP_SCREEN_ON] ?: true,
                showVideoInfo = preferences[SHOW_VIDEO_INFO] ?: false,
                autoLoadSubtitles = preferences[AUTO_LOAD_SUBTITLES] ?: true,
                wifiOnlyStreaming = preferences[WIFI_ONLY_STREAMING] ?: false,
                bufferSize = preferences[BUFFER_SIZE] ?: "Medium",
                swipeVerticalLeft = preferences[SWIPE_VERTICAL_LEFT] ?: "volume",
                swipeVerticalRight = preferences[SWIPE_VERTICAL_RIGHT] ?: "brightness",
                swipeHorizontal = preferences[SWIPE_HORIZONTAL] ?: "seek"
            )
        }
}