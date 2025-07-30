package com.astralplayer.nextplayer.feature.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Extension property for DataStore
private val Context.playerPreferencesDataStore: DataStore<Preferences> by preferencesDataStore(name = "player_preferences")

/**
 * Player preferences for general settings
 */
data class PlayerPreferences(
    // Playback settings
    val autoPlay: Boolean = true,
    val resumePlayback: Boolean = true,
    val defaultPlaybackSpeed: Float = 1.0f,
    val skipSilence: Boolean = false,
    
    // UI settings
    val keepScreenOn: Boolean = true,
    val showSystemUI: Boolean = false,
    val autoHideControls: Boolean = true,
    val controlsTimeout: Long = 4000L, // 4 seconds
    
    // Video settings
    val defaultAspectRatio: String = "fit_screen",
    val hardwareAcceleration: Boolean = true,
    val backgroundPlayback: Boolean = false,
    
    // Audio settings
    val volumeBoost: Boolean = false,
    val audioFocus: Boolean = true,
    val ducking: Boolean = true,
    
    // Subtitle settings
    val subtitlesEnabled: Boolean = true,
    val subtitleSize: Float = 1.0f,
    val subtitleLanguage: String = "auto",
    
    // Advanced settings
    val bufferSize: Int = 50000, // 50MB
    val networkTimeout: Long = 30000L, // 30 seconds
    val retryCount: Int = 3,
    
    // Privacy settings
    val collectAnalytics: Boolean = false,
    val crashReporting: Boolean = true
)

/**
 * Repository for player preferences
 */
class PlayerPreferencesRepository(private val context: Context) {
    
    private object Keys {
        // Playback
        val AUTO_PLAY = booleanPreferencesKey("auto_play")
        val RESUME_PLAYBACK = booleanPreferencesKey("resume_playback")
        val DEFAULT_PLAYBACK_SPEED = floatPreferencesKey("default_playback_speed")
        val SKIP_SILENCE = booleanPreferencesKey("skip_silence")
        
        // UI
        val KEEP_SCREEN_ON = booleanPreferencesKey("keep_screen_on")
        val SHOW_SYSTEM_UI = booleanPreferencesKey("show_system_ui")
        val AUTO_HIDE_CONTROLS = booleanPreferencesKey("auto_hide_controls")
        val CONTROLS_TIMEOUT = longPreferencesKey("controls_timeout")
        
        // Video
        val DEFAULT_ASPECT_RATIO = stringPreferencesKey("default_aspect_ratio")
        val HARDWARE_ACCELERATION = booleanPreferencesKey("hardware_acceleration")
        val BACKGROUND_PLAYBACK = booleanPreferencesKey("background_playback")
        
        // Audio
        val VOLUME_BOOST = booleanPreferencesKey("volume_boost")
        val AUDIO_FOCUS = booleanPreferencesKey("audio_focus")
        val DUCKING = booleanPreferencesKey("ducking")
        
        // Subtitles
        val SUBTITLES_ENABLED = booleanPreferencesKey("subtitles_enabled")
        val SUBTITLE_SIZE = floatPreferencesKey("subtitle_size")
        val SUBTITLE_LANGUAGE = stringPreferencesKey("subtitle_language")
        
        // Advanced
        val BUFFER_SIZE = intPreferencesKey("buffer_size")
        val NETWORK_TIMEOUT = longPreferencesKey("network_timeout")
        val RETRY_COUNT = intPreferencesKey("retry_count")
        
        // Privacy
        val COLLECT_ANALYTICS = booleanPreferencesKey("collect_analytics")
        val CRASH_REPORTING = booleanPreferencesKey("crash_reporting")
    }
    
    /**
     * Get current player preferences
     */
    val playerPreferences: Flow<PlayerPreferences> = context.playerPreferencesDataStore.data.map { preferences ->
        PlayerPreferences(
            // Playback
            autoPlay = preferences[Keys.AUTO_PLAY] ?: true,
            resumePlayback = preferences[Keys.RESUME_PLAYBACK] ?: true,
            defaultPlaybackSpeed = preferences[Keys.DEFAULT_PLAYBACK_SPEED] ?: 1.0f,
            skipSilence = preferences[Keys.SKIP_SILENCE] ?: false,
            
            // UI
            keepScreenOn = preferences[Keys.KEEP_SCREEN_ON] ?: true,
            showSystemUI = preferences[Keys.SHOW_SYSTEM_UI] ?: false,
            autoHideControls = preferences[Keys.AUTO_HIDE_CONTROLS] ?: true,
            controlsTimeout = preferences[Keys.CONTROLS_TIMEOUT] ?: 4000L,
            
            // Video
            defaultAspectRatio = preferences[Keys.DEFAULT_ASPECT_RATIO] ?: "fit_screen",
            hardwareAcceleration = preferences[Keys.HARDWARE_ACCELERATION] ?: true,
            backgroundPlayback = preferences[Keys.BACKGROUND_PLAYBACK] ?: false,
            
            // Audio
            volumeBoost = preferences[Keys.VOLUME_BOOST] ?: false,
            audioFocus = preferences[Keys.AUDIO_FOCUS] ?: true,
            ducking = preferences[Keys.DUCKING] ?: true,
            
            // Subtitles
            subtitlesEnabled = preferences[Keys.SUBTITLES_ENABLED] ?: true,
            subtitleSize = preferences[Keys.SUBTITLE_SIZE] ?: 1.0f,
            subtitleLanguage = preferences[Keys.SUBTITLE_LANGUAGE] ?: "auto",
            
            // Advanced
            bufferSize = preferences[Keys.BUFFER_SIZE] ?: 50000,
            networkTimeout = preferences[Keys.NETWORK_TIMEOUT] ?: 30000L,
            retryCount = preferences[Keys.RETRY_COUNT] ?: 3,
            
            // Privacy
            collectAnalytics = preferences[Keys.COLLECT_ANALYTICS] ?: false,
            crashReporting = preferences[Keys.CRASH_REPORTING] ?: true
        )
    }
    
    /**
     * Update player preferences
     */
    suspend fun updatePreferences(preferences: PlayerPreferences) {
        context.playerPreferencesDataStore.edit { prefs ->
            // Playback
            prefs[Keys.AUTO_PLAY] = preferences.autoPlay
            prefs[Keys.RESUME_PLAYBACK] = preferences.resumePlayback
            prefs[Keys.DEFAULT_PLAYBACK_SPEED] = preferences.defaultPlaybackSpeed
            prefs[Keys.SKIP_SILENCE] = preferences.skipSilence
            
            // UI
            prefs[Keys.KEEP_SCREEN_ON] = preferences.keepScreenOn
            prefs[Keys.SHOW_SYSTEM_UI] = preferences.showSystemUI
            prefs[Keys.AUTO_HIDE_CONTROLS] = preferences.autoHideControls
            prefs[Keys.CONTROLS_TIMEOUT] = preferences.controlsTimeout
            
            // Video
            prefs[Keys.DEFAULT_ASPECT_RATIO] = preferences.defaultAspectRatio
            prefs[Keys.HARDWARE_ACCELERATION] = preferences.hardwareAcceleration
            prefs[Keys.BACKGROUND_PLAYBACK] = preferences.backgroundPlayback
            
            // Audio
            prefs[Keys.VOLUME_BOOST] = preferences.volumeBoost
            prefs[Keys.AUDIO_FOCUS] = preferences.audioFocus
            prefs[Keys.DUCKING] = preferences.ducking
            
            // Subtitles
            prefs[Keys.SUBTITLES_ENABLED] = preferences.subtitlesEnabled
            prefs[Keys.SUBTITLE_SIZE] = preferences.subtitleSize
            prefs[Keys.SUBTITLE_LANGUAGE] = preferences.subtitleLanguage
            
            // Advanced
            prefs[Keys.BUFFER_SIZE] = preferences.bufferSize
            prefs[Keys.NETWORK_TIMEOUT] = preferences.networkTimeout
            prefs[Keys.RETRY_COUNT] = preferences.retryCount
            
            // Privacy
            prefs[Keys.COLLECT_ANALYTICS] = preferences.collectAnalytics
            prefs[Keys.CRASH_REPORTING] = preferences.crashReporting
        }
    }
    
    /**
     * Reset to default preferences
     */
    suspend fun resetToDefaults() {
        updatePreferences(PlayerPreferences())
    }
}