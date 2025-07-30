package com.astralplayer.nextplayer.feature.settings

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.mutableStateOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Comprehensive settings manager that handles all player settings
 * with real-time updates and persistence
 */
class ComprehensiveSettingsManager(private val context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences("astral_player_settings", Context.MODE_PRIVATE)
    
    // Settings categories
    private val _playbackSettings = MutableStateFlow(PlaybackSettings())
    val playbackSettings: StateFlow<PlaybackSettings> = _playbackSettings.asStateFlow()
    
    private val _gestureSettings = MutableStateFlow(GestureSettingsState())
    val gestureSettings: StateFlow<GestureSettingsState> = _gestureSettings.asStateFlow()
    
    private val _uiSettings = MutableStateFlow(UISettingsState())
    val uiSettings: StateFlow<UISettingsState> = _uiSettings.asStateFlow()
    
    private val _audioSettings = MutableStateFlow(AudioSettingsState())
    val audioSettings: StateFlow<AudioSettingsState> = _audioSettings.asStateFlow()
    
    private val _videoSettings = MutableStateFlow(VideoSettingsState())
    val videoSettings: StateFlow<VideoSettingsState> = _videoSettings.asStateFlow()
    
    private val _subtitleSettings = MutableStateFlow(SubtitleSettingsState())
    val subtitleSettings: StateFlow<SubtitleSettingsState> = _subtitleSettings.asStateFlow()
    
    private val _advancedSettings = MutableStateFlow(AdvancedSettingsState())
    val advancedSettings: StateFlow<AdvancedSettingsState> = _advancedSettings.asStateFlow()
    
    init {
        loadAllSettings()
    }
    
    // Load all settings from SharedPreferences
    private fun loadAllSettings() {
        _playbackSettings.value = PlaybackSettings(
            autoPlay = prefs.getBoolean("auto_play", true),
            resumePlayback = prefs.getBoolean("resume_playback", true),
            defaultPlaybackSpeed = prefs.getFloat("default_playback_speed", 1.0f),
            skipIntroOutro = prefs.getBoolean("skip_intro_outro", false),
            loopVideo = prefs.getBoolean("loop_video", false),
            shufflePlaylist = prefs.getBoolean("shuffle_playlist", false),
            backgroundPlayback = prefs.getBoolean("background_playback", false),
            pipMode = prefs.getBoolean("pip_mode", true),
            autoRotate = prefs.getBoolean("auto_rotate", true)
        )
        
        _gestureSettings.value = GestureSettingsState(
            gesturesEnabled = prefs.getBoolean("gestures_enabled", true),
            doubleTapToSeek = prefs.getBoolean("double_tap_seek", true),
            seekAmount = prefs.getInt("seek_amount", 10),
            volumeGesture = prefs.getBoolean("volume_gesture", true),
            brightnessGesture = prefs.getBoolean("brightness_gesture", true),
            longPressSpeed = prefs.getBoolean("long_press_speed", true),
            pinchToZoom = prefs.getBoolean("pinch_zoom", true),
            swipeToSeek = prefs.getBoolean("swipe_seek", true),
            gestureSensitivity = prefs.getFloat("gesture_sensitivity", 1.0f),
            hapticFeedback = prefs.getBoolean("haptic_feedback", true)
        )
        
        _uiSettings.value = UISettingsState(
            theme = Theme.valueOf(prefs.getString("theme", "SYSTEM") ?: "SYSTEM"),
            playerTheme = PlayerTheme.valueOf(prefs.getString("player_theme", "DARK") ?: "DARK"),
            showSystemBars = prefs.getBoolean("show_system_bars", false),
            immersiveMode = prefs.getBoolean("immersive_mode", true),
            controlsTimeout = prefs.getInt("controls_timeout", 5),
            showProgressThumbnails = prefs.getBoolean("progress_thumbnails", true),
            showVideoTitle = prefs.getBoolean("show_video_title", true),
            showVideoInfo = prefs.getBoolean("show_video_info", true),
            playerControlsLayout = ControlsLayout.valueOf(prefs.getString("controls_layout", "DEFAULT") ?: "DEFAULT"),
            bottomControlsHeight = prefs.getFloat("bottom_controls_height", 1.0f)
        )
        
        _audioSettings.value = AudioSettingsState(
            audioBoost = prefs.getBoolean("audio_boost", false),
            equalizerEnabled = prefs.getBoolean("equalizer_enabled", false),
            bassBoost = prefs.getFloat("bass_boost", 0f),
            trebleBoost = prefs.getFloat("treble_boost", 0f),
            audioDelay = prefs.getInt("audio_delay", 0),
            preferredAudioLanguage = prefs.getString("preferred_audio_language", "en") ?: "en",
            audioChannels = AudioChannels.valueOf(prefs.getString("audio_channels", "STEREO") ?: "STEREO"),
            volumeBoostLevel = prefs.getFloat("volume_boost_level", 1.0f),
            nightMode = prefs.getBoolean("night_mode", false)
        )
        
        _videoSettings.value = VideoSettingsState(
            defaultQuality = VideoQuality.valueOf(prefs.getString("default_quality", "AUTO") ?: "AUTO"),
            hardwareAcceleration = prefs.getBoolean("hardware_acceleration", true),
            videoDelay = prefs.getInt("video_delay", 0),
            aspectRatio = AspectRatio.valueOf(prefs.getString("aspect_ratio", "FIT") ?: "FIT"),
            videoScaling = VideoScaling.valueOf(prefs.getString("video_scaling", "FIT") ?: "FIT"),
            hdrMode = prefs.getBoolean("hdr_mode", true),
            videoFilters = prefs.getBoolean("video_filters", false),
            colorEnhancement = prefs.getFloat("color_enhancement", 1.0f),
            brightnessAdjustment = prefs.getFloat("brightness_adjustment", 0f),
            contrastAdjustment = prefs.getFloat("contrast_adjustment", 1.0f)
        )
        
        _subtitleSettings.value = SubtitleSettingsState(
            subtitlesEnabled = prefs.getBoolean("subtitles_enabled", true),
            fontSize = prefs.getFloat("subtitle_font_size", 16f),
            fontColor = prefs.getInt("subtitle_font_color", 0xFFFFFFFF.toInt()),
            backgroundColor = prefs.getInt("subtitle_background_color", 0x80000000.toInt()),
            position = SubtitlePosition.valueOf(prefs.getString("subtitle_position", "BOTTOM") ?: "BOTTOM"),
            encoding = prefs.getString("subtitle_encoding", "UTF-8") ?: "UTF-8",
            autoLoadSubtitles = prefs.getBoolean("auto_load_subtitles", true),
            preferredLanguage = prefs.getString("preferred_subtitle_language", "en") ?: "en",
            outlineWidth = prefs.getFloat("subtitle_outline_width", 2f),
            shadow = prefs.getBoolean("subtitle_shadow", true)
        )
        
        _advancedSettings.value = AdvancedSettingsState(
            bufferSize = prefs.getInt("buffer_size", 5000),
            networkTimeout = prefs.getInt("network_timeout", 30),
            retryCount = prefs.getInt("retry_count", 3),
            cacheSize = prefs.getInt("cache_size", 100),
            debugMode = prefs.getBoolean("debug_mode", false),
            performanceMode = prefs.getBoolean("performance_mode", false),
            batteryOptimization = prefs.getBoolean("battery_optimization", true),
            preloadNext = prefs.getBoolean("preload_next", true),
            thumbnailCache = prefs.getBoolean("thumbnail_cache", true),
            statisticsEnabled = prefs.getBoolean("statistics_enabled", false)
        )
    }
    
    // Save methods for each category
    fun updatePlaybackSettings(settings: PlaybackSettings) {
        _playbackSettings.value = settings
        with(prefs.edit()) {
            putBoolean("auto_play", settings.autoPlay)
            putBoolean("resume_playback", settings.resumePlayback)
            putFloat("default_playback_speed", settings.defaultPlaybackSpeed)
            putBoolean("skip_intro_outro", settings.skipIntroOutro)
            putBoolean("loop_video", settings.loopVideo)
            putBoolean("shuffle_playlist", settings.shufflePlaylist)
            putBoolean("background_playback", settings.backgroundPlayback)
            putBoolean("pip_mode", settings.pipMode)
            putBoolean("auto_rotate", settings.autoRotate)
            apply()
        }
    }
    
    fun updateGestureSettings(settings: GestureSettingsState) {
        _gestureSettings.value = settings
        with(prefs.edit()) {
            putBoolean("gestures_enabled", settings.gesturesEnabled)
            putBoolean("double_tap_seek", settings.doubleTapToSeek)
            putInt("seek_amount", settings.seekAmount)
            putBoolean("volume_gesture", settings.volumeGesture)
            putBoolean("brightness_gesture", settings.brightnessGesture)
            putBoolean("long_press_speed", settings.longPressSpeed)
            putBoolean("pinch_zoom", settings.pinchToZoom)
            putBoolean("swipe_seek", settings.swipeToSeek)
            putFloat("gesture_sensitivity", settings.gestureSensitivity)
            putBoolean("haptic_feedback", settings.hapticFeedback)
            apply()
        }
    }
    
    fun updateUISettings(settings: UISettingsState) {
        _uiSettings.value = settings
        with(prefs.edit()) {
            putString("theme", settings.theme.name)
            putString("player_theme", settings.playerTheme.name)
            putBoolean("show_system_bars", settings.showSystemBars)
            putBoolean("immersive_mode", settings.immersiveMode)
            putInt("controls_timeout", settings.controlsTimeout)
            putBoolean("progress_thumbnails", settings.showProgressThumbnails)
            putBoolean("show_video_title", settings.showVideoTitle)
            putBoolean("show_video_info", settings.showVideoInfo)
            putString("controls_layout", settings.playerControlsLayout.name)
            putFloat("bottom_controls_height", settings.bottomControlsHeight)
            apply()
        }
    }
    
    fun updateAudioSettings(settings: AudioSettingsState) {
        _audioSettings.value = settings
        with(prefs.edit()) {
            putBoolean("audio_boost", settings.audioBoost)
            putBoolean("equalizer_enabled", settings.equalizerEnabled)
            putFloat("bass_boost", settings.bassBoost)
            putFloat("treble_boost", settings.trebleBoost)
            putInt("audio_delay", settings.audioDelay)
            putString("preferred_audio_language", settings.preferredAudioLanguage)
            putString("audio_channels", settings.audioChannels.name)
            putFloat("volume_boost_level", settings.volumeBoostLevel)
            putBoolean("night_mode", settings.nightMode)
            apply()
        }
    }
    
    fun updateVideoSettings(settings: VideoSettingsState) {
        _videoSettings.value = settings
        with(prefs.edit()) {
            putString("default_quality", settings.defaultQuality.name)
            putBoolean("hardware_acceleration", settings.hardwareAcceleration)
            putInt("video_delay", settings.videoDelay)
            putString("aspect_ratio", settings.aspectRatio.name)
            putString("video_scaling", settings.videoScaling.name)
            putBoolean("hdr_mode", settings.hdrMode)
            putBoolean("video_filters", settings.videoFilters)
            putFloat("color_enhancement", settings.colorEnhancement)
            putFloat("brightness_adjustment", settings.brightnessAdjustment)
            putFloat("contrast_adjustment", settings.contrastAdjustment)
            apply()
        }
    }
    
    fun updateSubtitleSettings(settings: SubtitleSettingsState) {
        _subtitleSettings.value = settings
        with(prefs.edit()) {
            putBoolean("subtitles_enabled", settings.subtitlesEnabled)
            putFloat("subtitle_font_size", settings.fontSize)
            putInt("subtitle_font_color", settings.fontColor)
            putInt("subtitle_background_color", settings.backgroundColor)
            putString("subtitle_position", settings.position.name)
            putString("subtitle_encoding", settings.encoding)
            putBoolean("auto_load_subtitles", settings.autoLoadSubtitles)
            putString("preferred_subtitle_language", settings.preferredLanguage)
            putFloat("subtitle_outline_width", settings.outlineWidth)
            putBoolean("subtitle_shadow", settings.shadow)
            apply()
        }
    }
    
    fun updateAdvancedSettings(settings: AdvancedSettingsState) {
        _advancedSettings.value = settings
        with(prefs.edit()) {
            putInt("buffer_size", settings.bufferSize)
            putInt("network_timeout", settings.networkTimeout)
            putInt("retry_count", settings.retryCount)
            putInt("cache_size", settings.cacheSize)
            putBoolean("debug_mode", settings.debugMode)
            putBoolean("performance_mode", settings.performanceMode)
            putBoolean("battery_optimization", settings.batteryOptimization)
            putBoolean("preload_next", settings.preloadNext)
            putBoolean("thumbnail_cache", settings.thumbnailCache)
            putBoolean("statistics_enabled", settings.statisticsEnabled)
            apply()
        }
    }
    
    // Reset methods
    fun resetAllSettings() {
        prefs.edit().clear().apply()
        loadAllSettings()
    }
    
    fun resetToDefaults(category: SettingsCategory) {
        when (category) {
            SettingsCategory.PLAYBACK -> updatePlaybackSettings(PlaybackSettings())
            SettingsCategory.GESTURES -> updateGestureSettings(GestureSettingsState())
            SettingsCategory.UI -> updateUISettings(UISettingsState())
            SettingsCategory.AUDIO -> updateAudioSettings(AudioSettingsState())
            SettingsCategory.VIDEO -> updateVideoSettings(VideoSettingsState())
            SettingsCategory.SUBTITLES -> updateSubtitleSettings(SubtitleSettingsState())
            SettingsCategory.ADVANCED -> updateAdvancedSettings(AdvancedSettingsState())
        }
    }
}

// Settings data classes
data class PlaybackSettings(
    val autoPlay: Boolean = true,
    val resumePlayback: Boolean = true,
    val defaultPlaybackSpeed: Float = 1.0f,
    val skipIntroOutro: Boolean = false,
    val loopVideo: Boolean = false,
    val shufflePlaylist: Boolean = false,
    val backgroundPlayback: Boolean = false,
    val pipMode: Boolean = true,
    val autoRotate: Boolean = true
)

data class GestureSettingsState(
    val gesturesEnabled: Boolean = true,
    val doubleTapToSeek: Boolean = true,
    val seekAmount: Int = 10,
    val volumeGesture: Boolean = true,
    val brightnessGesture: Boolean = true,
    val longPressSpeed: Boolean = true,
    val pinchToZoom: Boolean = true,
    val swipeToSeek: Boolean = true,
    val gestureSensitivity: Float = 1.0f,
    val hapticFeedback: Boolean = true
)

data class UISettingsState(
    val theme: Theme = Theme.SYSTEM,
    val playerTheme: PlayerTheme = PlayerTheme.DARK,
    val showSystemBars: Boolean = false,
    val immersiveMode: Boolean = true,
    val controlsTimeout: Int = 5,
    val showProgressThumbnails: Boolean = true,
    val showVideoTitle: Boolean = true,
    val showVideoInfo: Boolean = true,
    val playerControlsLayout: ControlsLayout = ControlsLayout.DEFAULT,
    val bottomControlsHeight: Float = 1.0f
)

data class AudioSettingsState(
    val audioBoost: Boolean = false,
    val equalizerEnabled: Boolean = false,
    val bassBoost: Float = 0f,
    val trebleBoost: Float = 0f,
    val audioDelay: Int = 0,
    val preferredAudioLanguage: String = "en",
    val audioChannels: AudioChannels = AudioChannels.STEREO,
    val volumeBoostLevel: Float = 1.0f,
    val nightMode: Boolean = false
)

data class VideoSettingsState(
    val defaultQuality: VideoQuality = VideoQuality.AUTO,
    val hardwareAcceleration: Boolean = true,
    val videoDelay: Int = 0,
    val aspectRatio: AspectRatio = AspectRatio.FIT,
    val videoScaling: VideoScaling = VideoScaling.FIT,
    val hdrMode: Boolean = true,
    val videoFilters: Boolean = false,
    val colorEnhancement: Float = 1.0f,
    val brightnessAdjustment: Float = 0f,
    val contrastAdjustment: Float = 1.0f
)

data class SubtitleSettingsState(
    val subtitlesEnabled: Boolean = true,
    val fontSize: Float = 16f,
    val fontColor: Int = 0xFFFFFFFF.toInt(),
    val backgroundColor: Int = 0x80000000.toInt(),
    val position: SubtitlePosition = SubtitlePosition.BOTTOM,
    val encoding: String = "UTF-8",
    val autoLoadSubtitles: Boolean = true,
    val preferredLanguage: String = "en",
    val outlineWidth: Float = 2f,
    val shadow: Boolean = true
)

data class AdvancedSettingsState(
    val bufferSize: Int = 5000,
    val networkTimeout: Int = 30,
    val retryCount: Int = 3,
    val cacheSize: Int = 100,
    val debugMode: Boolean = false,
    val performanceMode: Boolean = false,
    val batteryOptimization: Boolean = true,
    val preloadNext: Boolean = true,
    val thumbnailCache: Boolean = true,
    val statisticsEnabled: Boolean = false
)

// Enums
enum class SettingsCategory {
    PLAYBACK, GESTURES, UI, AUDIO, VIDEO, SUBTITLES, ADVANCED
}

enum class Theme {
    LIGHT, DARK, SYSTEM
}

enum class PlayerTheme {
    LIGHT, DARK, OLED, AUTO
}

enum class ControlsLayout {
    DEFAULT, MINIMAL, EXTENDED, CUSTOM
}

enum class AudioChannels {
    MONO, STEREO, SURROUND
}

enum class VideoQuality {
    AUTO, SD_480P, HD_720P, FULL_HD_1080P, UHD_4K
}

enum class AspectRatio {
    FIT, FILL, STRETCH, CROP, ORIGINAL
}

enum class VideoScaling {
    FIT, FILL, ZOOM, CROP
}

enum class SubtitlePosition {
    TOP, CENTER, BOTTOM
}