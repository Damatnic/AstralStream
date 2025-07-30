package com.astralplayer.nextplayer.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

// Extension property to get the DataStore instance
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = "astral_vu_settings"
)

interface SettingsRepository {
    // Playback settings
    fun getAutoPlayNext(): Flow<Boolean>
    suspend fun setAutoPlayNext(enabled: Boolean)
    
    fun getDefaultPlaybackSpeed(): Flow<Float>
    suspend fun setDefaultPlaybackSpeed(speed: Float)
    
    fun getRememberBrightness(): Flow<Boolean>
    suspend fun setRememberBrightness(enabled: Boolean)
    
    fun getRememberPlaybackSpeed(): Flow<Boolean>
    suspend fun setRememberPlaybackSpeed(enabled: Boolean)
    
    // Display settings
    fun getScreenOrientation(): Flow<String>
    suspend fun setScreenOrientation(orientation: String)
    
    fun getAspectRatio(): Flow<String>
    suspend fun setAspectRatio(ratio: String)
    
    fun getVideoScaling(): Flow<String>
    suspend fun setVideoScaling(scaling: String)
    
    // Gesture settings
    fun getSwipeToSeekEnabled(): Flow<Boolean>
    suspend fun setSwipeToSeekEnabled(enabled: Boolean)
    
    fun getDoubleTapToSeekEnabled(): Flow<Boolean>
    suspend fun setDoubleTapToSeekEnabled(enabled: Boolean)
    
    fun getLongPressToSeekEnabled(): Flow<Boolean>
    suspend fun setLongPressToSeekEnabled(enabled: Boolean)
    
    fun getLongPressSeekSpeed(): Flow<Float>
    suspend fun setLongPressSeekSpeed(speed: Float)
    
    fun getPinchToZoomEnabled(): Flow<Boolean>
    suspend fun setPinchToZoomEnabled(enabled: Boolean)
    
    fun getSwipeSensitivity(): Flow<Float>
    suspend fun setSwipeSensitivity(sensitivity: Float)
    
    // Long press speed control settings
    fun getLongPressSpeedControlEnabled(): Flow<Boolean>
    suspend fun setLongPressSpeedControlEnabled(enabled: Boolean)
    
    fun getLongPressInitialSpeed(): Flow<Float>
    suspend fun setLongPressInitialSpeed(speed: Float)
    
    fun getLongPressProgressiveSpeedEnabled(): Flow<Boolean>
    suspend fun setLongPressProgressiveSpeedEnabled(enabled: Boolean)
    
    fun getLongPressSwipeSensitivity(): Flow<Float>
    suspend fun setLongPressSwipeSensitivity(sensitivity: Float)
    
    // Custom speed progression
    fun getCustomSpeedProgression(): Flow<String>
    suspend fun setCustomSpeedProgression(progression: String)
    
    fun getLongPressTimeout(): Flow<Long>
    suspend fun setLongPressTimeout(timeout: Long)
    
    // Speed memory per video
    fun getSpeedMemoryEnabled(): Flow<Boolean>
    suspend fun setSpeedMemoryEnabled(enabled: Boolean)
    
    fun getVideoSpeedMemory(videoPath: String): Flow<Float>
    suspend fun setVideoSpeedMemory(videoPath: String, speed: Float)
    
    suspend fun clearVideoSpeedMemory()
    fun getAllVideoSpeedMemory(): Flow<Map<String, Float>>
    
    // Subtitle settings
    fun getSubtitleFontSize(): Flow<Float>
    suspend fun setSubtitleFontSize(size: Float)
    
    fun getSubtitleBackground(): Flow<Boolean>
    suspend fun setSubtitleBackground(enabled: Boolean)
    
    fun getSubtitlePosition(): Flow<String>
    suspend fun setSubtitlePosition(position: String)
    
    // Audio settings
    fun getAudioDelay(): Flow<Float>
    suspend fun setAudioDelay(delay: Float)
    
    fun getVolumeBoostEnabled(): Flow<Boolean>
    suspend fun setVolumeBoostEnabled(enabled: Boolean)
    
    fun getAudioNormalizationEnabled(): Flow<Boolean>
    suspend fun setAudioNormalizationEnabled(enabled: Boolean)
    
    // Network settings
    fun getPreferredQualityWifi(): Flow<String>
    suspend fun setPreferredQualityWifi(quality: String)
    
    fun getPreferredQualityMobile(): Flow<String>
    suspend fun setPreferredQualityMobile(quality: String)
    
    fun getAllowMobileData(): Flow<Boolean>
    suspend fun setAllowMobileData(enabled: Boolean)
    
    // Privacy settings
    fun getIncognitoMode(): Flow<Boolean>
    suspend fun setIncognitoMode(enabled: Boolean)
    
    fun getPauseOnScreenOff(): Flow<Boolean>
    suspend fun setPauseOnScreenOff(enabled: Boolean)
    
    fun getAppLockEnabled(): Flow<Boolean>
    suspend fun setAppLockEnabled(enabled: Boolean)
    
    // Theme settings
    fun getThemeMode(): Flow<String>
    suspend fun setThemeMode(mode: String)
    
    // Subtitle and Audio settings
    fun getSubtitleLanguage(): Flow<String>
    suspend fun setSubtitleLanguage(language: String)
    
    fun getAudioLanguage(): Flow<String>
    suspend fun setAudioLanguage(language: String)
    
    fun getVideoQuality(): Flow<String>
    suspend fun setVideoQuality(quality: String)
    
    // AI Features
    fun getAISubtitleGenerationEnabled(): Flow<Boolean>
    suspend fun setAISubtitleGenerationEnabled(enabled: Boolean)
    
    // Data management
    suspend fun clearAllData()
}

class SettingsRepositoryImpl(
    private val context: Context
) : SettingsRepository {
    
    companion object {
        // Playback keys
        private val AUTO_PLAY_NEXT = booleanPreferencesKey("auto_play_next")
        private val DEFAULT_PLAYBACK_SPEED = floatPreferencesKey("default_playback_speed")
        private val REMEMBER_BRIGHTNESS = booleanPreferencesKey("remember_brightness")
        private val REMEMBER_PLAYBACK_SPEED = booleanPreferencesKey("remember_playback_speed")
        
        // Display keys
        private val SCREEN_ORIENTATION = stringPreferencesKey("screen_orientation")
        private val ASPECT_RATIO = stringPreferencesKey("aspect_ratio")
        private val VIDEO_SCALING = stringPreferencesKey("video_scaling")
        
        // Gesture keys
        private val SWIPE_TO_SEEK_ENABLED = booleanPreferencesKey("swipe_to_seek_enabled") 
        private val DOUBLE_TAP_TO_SEEK_ENABLED = booleanPreferencesKey("double_tap_to_seek_enabled")
        private val LONG_PRESS_TO_SEEK_ENABLED = booleanPreferencesKey("long_press_to_seek_enabled")
        private val LONG_PRESS_SEEK_SPEED = floatPreferencesKey("long_press_seek_speed")
        private val PINCH_TO_ZOOM_ENABLED = booleanPreferencesKey("pinch_to_zoom_enabled")
        private val SWIPE_SENSITIVITY = floatPreferencesKey("swipe_sensitivity")
        
        // Long press speed control keys
        private val LONG_PRESS_SPEED_CONTROL_ENABLED = booleanPreferencesKey("long_press_speed_control_enabled")
        private val LONG_PRESS_INITIAL_SPEED = floatPreferencesKey("long_press_initial_speed")
        private val LONG_PRESS_PROGRESSIVE_SPEED_ENABLED = booleanPreferencesKey("long_press_progressive_speed_enabled")
        private val LONG_PRESS_SWIPE_SENSITIVITY = floatPreferencesKey("long_press_swipe_sensitivity")
        private val CUSTOM_SPEED_PROGRESSION = stringPreferencesKey("custom_speed_progression")
        private val LONG_PRESS_TIMEOUT = longPreferencesKey("long_press_timeout")
        
        // Speed memory per video keys
        private val SPEED_MEMORY_ENABLED = booleanPreferencesKey("speed_memory_enabled")
        private val VIDEO_SPEED_MEMORY_PREFIX = "video_speed_"
        
        // Subtitle keys
        private val SUBTITLE_FONT_SIZE = floatPreferencesKey("subtitle_font_size")
        private val SUBTITLE_BACKGROUND = booleanPreferencesKey("subtitle_background")
        private val SUBTITLE_POSITION = stringPreferencesKey("subtitle_position")
        
        // Audio keys
        private val AUDIO_DELAY = floatPreferencesKey("audio_delay")
        private val VOLUME_BOOST_ENABLED = booleanPreferencesKey("volume_boost_enabled")
        private val AUDIO_NORMALIZATION_ENABLED = booleanPreferencesKey("audio_normalization_enabled")
        
        // Network keys
        private val PREFERRED_QUALITY_WIFI = stringPreferencesKey("preferred_quality_wifi")
        private val PREFERRED_QUALITY_MOBILE = stringPreferencesKey("preferred_quality_mobile")
        private val ALLOW_MOBILE_DATA = booleanPreferencesKey("allow_mobile_data")
        
        // Privacy keys
        private val INCOGNITO_MODE = booleanPreferencesKey("incognito_mode")
        private val PAUSE_ON_SCREEN_OFF = booleanPreferencesKey("pause_on_screen_off")
        private val APP_LOCK_ENABLED = booleanPreferencesKey("app_lock_enabled")
        
        // Theme keys
        private val THEME_MODE = stringPreferencesKey("theme_mode")
        
        // Language and quality keys
        private val SUBTITLE_LANGUAGE = stringPreferencesKey("subtitle_language")
        private val AUDIO_LANGUAGE = stringPreferencesKey("audio_language")
        private val VIDEO_QUALITY = stringPreferencesKey("video_quality")
        
        // AI Features keys
        private val AI_SUBTITLE_GENERATION_ENABLED = booleanPreferencesKey("ai_subtitle_generation_enabled")
    }
    
    private val dataStore = context.dataStore
    
    // Helper function to handle errors
    private fun <T> Flow<T>.handleErrors(defaultValue: T): Flow<T> {
        return this.catch { exception ->
            if (exception is IOException) {
                emit(defaultValue)
            } else {
                throw exception
            }
        }
    }
    
    // Playback settings implementations
    override fun getAutoPlayNext(): Flow<Boolean> = dataStore.data
        .map { preferences -> preferences[AUTO_PLAY_NEXT] ?: true }
        .handleErrors(true)
    
    override suspend fun setAutoPlayNext(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[AUTO_PLAY_NEXT] = enabled
        }
    }
    
    override fun getDefaultPlaybackSpeed(): Flow<Float> = dataStore.data
        .map { preferences -> preferences[DEFAULT_PLAYBACK_SPEED] ?: 1.0f }
        .handleErrors(1.0f)
    
    override suspend fun setDefaultPlaybackSpeed(speed: Float) {
        dataStore.edit { preferences ->
            preferences[DEFAULT_PLAYBACK_SPEED] = speed
        }
    }
    
    override fun getRememberBrightness(): Flow<Boolean> = dataStore.data
        .map { preferences -> preferences[REMEMBER_BRIGHTNESS] ?: false }
        .handleErrors(false)
    
    override suspend fun setRememberBrightness(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[REMEMBER_BRIGHTNESS] = enabled
        }
    }
    
    override fun getRememberPlaybackSpeed(): Flow<Boolean> = dataStore.data
        .map { preferences -> preferences[REMEMBER_PLAYBACK_SPEED] ?: true }
        .handleErrors(true)
    
    override suspend fun setRememberPlaybackSpeed(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[REMEMBER_PLAYBACK_SPEED] = enabled
        }
    }
    
    // Display settings
    override fun getScreenOrientation(): Flow<String> = dataStore.data
        .map { preferences -> preferences[SCREEN_ORIENTATION] ?: "auto" }
        .handleErrors("auto")
    
    override suspend fun setScreenOrientation(orientation: String) {
        dataStore.edit { preferences ->
            preferences[SCREEN_ORIENTATION] = orientation
        }
    }
    
    override fun getAspectRatio(): Flow<String> = dataStore.data
        .map { preferences -> preferences[ASPECT_RATIO] ?: "fit" }
        .handleErrors("fit")
    
    override suspend fun setAspectRatio(ratio: String) {
        dataStore.edit { preferences ->
            preferences[ASPECT_RATIO] = ratio
        }
    }
    
    override fun getVideoScaling(): Flow<String> = dataStore.data
        .map { preferences -> preferences[VIDEO_SCALING] ?: "fit" }
        .handleErrors("fit")
    
    override suspend fun setVideoScaling(scaling: String) {
        dataStore.edit { preferences ->
            preferences[VIDEO_SCALING] = scaling
        }
    }
    
    // Gesture settings
    override fun getSwipeToSeekEnabled(): Flow<Boolean> = dataStore.data
        .map { preferences -> preferences[SWIPE_TO_SEEK_ENABLED] ?: true }
        .handleErrors(true)
    
    override suspend fun setSwipeToSeekEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[SWIPE_TO_SEEK_ENABLED] = enabled
        }
    }
    
    override fun getDoubleTapToSeekEnabled(): Flow<Boolean> = dataStore.data
        .map { preferences -> preferences[DOUBLE_TAP_TO_SEEK_ENABLED] ?: true }
        .handleErrors(true)
    
    override suspend fun setDoubleTapToSeekEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[DOUBLE_TAP_TO_SEEK_ENABLED] = enabled
        }
    }
    
    override fun getLongPressToSeekEnabled(): Flow<Boolean> = dataStore.data
        .map { preferences -> preferences[LONG_PRESS_TO_SEEK_ENABLED] ?: true }
        .handleErrors(true)
    
    override suspend fun setLongPressToSeekEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[LONG_PRESS_TO_SEEK_ENABLED] = enabled
        }
    }
    
    override fun getLongPressSeekSpeed(): Flow<Float> = dataStore.data
        .map { preferences -> preferences[LONG_PRESS_SEEK_SPEED] ?: 2.0f }
        .handleErrors(2.0f)
    
    override suspend fun setLongPressSeekSpeed(speed: Float) {
        dataStore.edit { preferences ->  
            preferences[LONG_PRESS_SEEK_SPEED] = speed
        }
    }
    
    override fun getPinchToZoomEnabled(): Flow<Boolean> = dataStore.data
        .map { preferences -> preferences[PINCH_TO_ZOOM_ENABLED] ?: true }
        .handleErrors(true)
    
    override suspend fun setPinchToZoomEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PINCH_TO_ZOOM_ENABLED] = enabled
        }
    }
    
    override fun getSwipeSensitivity(): Flow<Float> = dataStore.data
        .map { preferences -> preferences[SWIPE_SENSITIVITY] ?: 1.0f }
        .handleErrors(1.0f)
    
    override suspend fun setSwipeSensitivity(sensitivity: Float) {
        dataStore.edit { preferences ->
            preferences[SWIPE_SENSITIVITY] = sensitivity
        }
    }
    
    // Long press speed control settings
    override fun getLongPressSpeedControlEnabled(): Flow<Boolean> = dataStore.data
        .map { preferences -> preferences[LONG_PRESS_SPEED_CONTROL_ENABLED] ?: true }
        .handleErrors(true)
    
    override suspend fun setLongPressSpeedControlEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[LONG_PRESS_SPEED_CONTROL_ENABLED] = enabled
        }
    }
    
    override fun getLongPressInitialSpeed(): Flow<Float> = dataStore.data
        .map { preferences -> preferences[LONG_PRESS_INITIAL_SPEED] ?: 2.0f }
        .handleErrors(2.0f)
    
    override suspend fun setLongPressInitialSpeed(speed: Float) {
        dataStore.edit { preferences ->
            preferences[LONG_PRESS_INITIAL_SPEED] = speed
        }
    }
    
    override fun getLongPressProgressiveSpeedEnabled(): Flow<Boolean> = dataStore.data
        .map { preferences -> preferences[LONG_PRESS_PROGRESSIVE_SPEED_ENABLED] ?: true }
        .handleErrors(true)
    
    override suspend fun setLongPressProgressiveSpeedEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[LONG_PRESS_PROGRESSIVE_SPEED_ENABLED] = enabled
        }
    }
    
    override fun getLongPressSwipeSensitivity(): Flow<Float> = dataStore.data
        .map { preferences -> preferences[LONG_PRESS_SWIPE_SENSITIVITY] ?: 1.0f }
        .handleErrors(1.0f)
    
    override suspend fun setLongPressSwipeSensitivity(sensitivity: Float) {
        dataStore.edit { preferences ->
            preferences[LONG_PRESS_SWIPE_SENSITIVITY] = sensitivity
        }
    }
    
    // Custom speed progression - stored as comma-separated string
    override fun getCustomSpeedProgression(): Flow<String> = dataStore.data
        .map { preferences -> preferences[CUSTOM_SPEED_PROGRESSION] ?: "0.25,0.5,0.75,1.0,1.25,1.5,2.0,3.0,4.0,6.0,8.0" }
        .handleErrors("0.25,0.5,0.75,1.0,1.25,1.5,2.0,3.0,4.0,6.0,8.0")
    
    override suspend fun setCustomSpeedProgression(progression: String) {
        dataStore.edit { preferences ->
            preferences[CUSTOM_SPEED_PROGRESSION] = progression
        }
    }
    
    override fun getLongPressTimeout(): Flow<Long> = dataStore.data
        .map { preferences -> preferences[LONG_PRESS_TIMEOUT] ?: 800L }
        .handleErrors(800L)
    
    override suspend fun setLongPressTimeout(timeout: Long) {
        dataStore.edit { preferences ->
            preferences[LONG_PRESS_TIMEOUT] = timeout
        }
    }
    
    // Speed memory per video implementation
    override fun getSpeedMemoryEnabled(): Flow<Boolean> = dataStore.data
        .map { preferences -> preferences[SPEED_MEMORY_ENABLED] ?: true }
        .handleErrors(true)
    
    override suspend fun setSpeedMemoryEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[SPEED_MEMORY_ENABLED] = enabled
        }
    }
    
    override fun getVideoSpeedMemory(videoPath: String): Flow<Float> = dataStore.data
        .map { preferences ->
            val key = floatPreferencesKey("${VIDEO_SPEED_MEMORY_PREFIX}${videoPath.hashCode()}")
            preferences[key] ?: 1.0f
        }
        .handleErrors(1.0f)
    
    override suspend fun setVideoSpeedMemory(videoPath: String, speed: Float) {
        dataStore.edit { preferences ->
            val key = floatPreferencesKey("${VIDEO_SPEED_MEMORY_PREFIX}${videoPath.hashCode()}")
            preferences[key] = speed
        }
    }
    
    override suspend fun clearVideoSpeedMemory() {
        dataStore.edit { preferences ->
            val keysToRemove = preferences.asMap().keys.filter { key ->
                key.name.startsWith(VIDEO_SPEED_MEMORY_PREFIX)
            }
            keysToRemove.forEach { key ->
                preferences.remove(key)
            }
        }
    }
    
    override fun getAllVideoSpeedMemory(): Flow<Map<String, Float>> = dataStore.data
        .map { preferences ->
            val speedMemoryMap = mutableMapOf<String, Float>()
            preferences.asMap().forEach { (key, value) ->
                if (key.name.startsWith(VIDEO_SPEED_MEMORY_PREFIX) && value is Float) {
                    val videoHash = key.name.removePrefix(VIDEO_SPEED_MEMORY_PREFIX)
                    speedMemoryMap[videoHash] = value
                }
            }
            speedMemoryMap.toMap()
        }
        .handleErrors(emptyMap())
    
    // Subtitle settings
    override fun getSubtitleFontSize(): Flow<Float> = dataStore.data
        .map { preferences -> preferences[SUBTITLE_FONT_SIZE] ?: 16f }
        .handleErrors(16f)
    
    override suspend fun setSubtitleFontSize(size: Float) {
        dataStore.edit { preferences ->
            preferences[SUBTITLE_FONT_SIZE] = size
        }
    }
    
    override fun getSubtitleBackground(): Flow<Boolean> = dataStore.data
        .map { preferences -> preferences[SUBTITLE_BACKGROUND] ?: true }
        .handleErrors(true)
    
    override suspend fun setSubtitleBackground(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[SUBTITLE_BACKGROUND] = enabled
        }
    }
    
    override fun getSubtitlePosition(): Flow<String> = dataStore.data
        .map { preferences -> preferences[SUBTITLE_POSITION] ?: "bottom" }
        .handleErrors("bottom")
    
    override suspend fun setSubtitlePosition(position: String) {
        dataStore.edit { preferences ->
            preferences[SUBTITLE_POSITION] = position
        }
    }
    
    // Audio settings
    override fun getAudioDelay(): Flow<Float> = dataStore.data
        .map { preferences -> preferences[AUDIO_DELAY] ?: 0f }
        .handleErrors(0f)
    
    override suspend fun setAudioDelay(delay: Float) {
        dataStore.edit { preferences ->
            preferences[AUDIO_DELAY] = delay
        }
    }
    
    override fun getVolumeBoostEnabled(): Flow<Boolean> = dataStore.data
        .map { preferences -> preferences[VOLUME_BOOST_ENABLED] ?: false }
        .handleErrors(false)
    
    override suspend fun setVolumeBoostEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[VOLUME_BOOST_ENABLED] = enabled
        }
    }
    
    override fun getAudioNormalizationEnabled(): Flow<Boolean> = dataStore.data
        .map { preferences -> preferences[AUDIO_NORMALIZATION_ENABLED] ?: false }
        .handleErrors(false)
    
    override suspend fun setAudioNormalizationEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[AUDIO_NORMALIZATION_ENABLED] = enabled
        }
    }
    
    // Network settings
    override fun getPreferredQualityWifi(): Flow<String> = dataStore.data
        .map { preferences -> preferences[PREFERRED_QUALITY_WIFI] ?: "highest" }
        .handleErrors("highest")
    
    override suspend fun setPreferredQualityWifi(quality: String) {
        dataStore.edit { preferences ->
            preferences[PREFERRED_QUALITY_WIFI] = quality
        }
    }
    
    override fun getPreferredQualityMobile(): Flow<String> = dataStore.data
        .map { preferences -> preferences[PREFERRED_QUALITY_MOBILE] ?: "720p" }
        .handleErrors("720p")
    
    override suspend fun setPreferredQualityMobile(quality: String) {
        dataStore.edit { preferences ->
            preferences[PREFERRED_QUALITY_MOBILE] = quality
        }
    }
    
    override fun getAllowMobileData(): Flow<Boolean> = dataStore.data
        .map { preferences -> preferences[ALLOW_MOBILE_DATA] ?: false }
        .handleErrors(false)
    
    override suspend fun setAllowMobileData(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[ALLOW_MOBILE_DATA] = enabled
        }
    }
    
    // Privacy settings
    override fun getIncognitoMode(): Flow<Boolean> = dataStore.data
        .map { preferences -> preferences[INCOGNITO_MODE] ?: false }
        .handleErrors(false)
    
    override suspend fun setIncognitoMode(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[INCOGNITO_MODE] = enabled
        }
    }
    
    override fun getPauseOnScreenOff(): Flow<Boolean> = dataStore.data
        .map { preferences -> preferences[PAUSE_ON_SCREEN_OFF] ?: true }
        .handleErrors(true)
    
    override suspend fun setPauseOnScreenOff(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PAUSE_ON_SCREEN_OFF] = enabled
        }
    }
    
    override fun getAppLockEnabled(): Flow<Boolean> = dataStore.data
        .map { preferences -> preferences[APP_LOCK_ENABLED] ?: false }
        .handleErrors(false)
    
    override suspend fun setAppLockEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[APP_LOCK_ENABLED] = enabled
        }
    }
    
    // Theme settings
    override fun getThemeMode(): Flow<String> = dataStore.data
        .map { preferences -> preferences[THEME_MODE] ?: "system" }
        .handleErrors("system")
    
    override suspend fun setThemeMode(mode: String) {
        dataStore.edit { preferences ->
            preferences[THEME_MODE] = mode
        }
    }
    
    // Subtitle and Audio settings
    override fun getSubtitleLanguage(): Flow<String> = dataStore.data
        .map { preferences -> preferences[SUBTITLE_LANGUAGE] ?: "auto" }
        .handleErrors("auto")
    
    override suspend fun setSubtitleLanguage(language: String) {
        dataStore.edit { preferences ->
            preferences[SUBTITLE_LANGUAGE] = language
        }
    }
    
    override fun getAudioLanguage(): Flow<String> = dataStore.data
        .map { preferences -> preferences[AUDIO_LANGUAGE] ?: "auto" }
        .handleErrors("auto")
    
    override suspend fun setAudioLanguage(language: String) {
        dataStore.edit { preferences ->
            preferences[AUDIO_LANGUAGE] = language
        }
    }
    
    override fun getVideoQuality(): Flow<String> = dataStore.data
        .map { preferences -> preferences[VIDEO_QUALITY] ?: "auto" }
        .handleErrors("auto")
    
    override suspend fun setVideoQuality(quality: String) {
        dataStore.edit { preferences ->
            preferences[VIDEO_QUALITY] = quality
        }
    }
    
    // AI Features
    override fun getAISubtitleGenerationEnabled(): Flow<Boolean> = dataStore.data
        .map { preferences -> preferences[AI_SUBTITLE_GENERATION_ENABLED] ?: true }
        .handleErrors(true)
    
    override suspend fun setAISubtitleGenerationEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[AI_SUBTITLE_GENERATION_ENABLED] = enabled
        }
    }
    
    // Data management
    override suspend fun clearAllData() {
        dataStore.edit { preferences ->
            preferences.clear()
        }
    }
}