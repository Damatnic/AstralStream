package com.astralplayer.nextplayer.feature.player.revolutionary

/**
 * Actions that can be performed on the video player
 */
enum class PlayerAction {
    // Basic playback controls
    PLAY,
    PAUSE,
    STOP,
    TOGGLE_PLAY_PAUSE,
    PLAY_PAUSE, // Alias for TOGGLE_PLAY_PAUSE
    
    // Seeking controls
    SEEK_FORWARD,
    SEEK_BACKWARD,
    SEEK_TO_POSITION,
    FAST_FORWARD,
    FAST_REWIND,
    
    // Volume controls
    VOLUME_UP,
    VOLUME_DOWN,
    MUTE,
    UNMUTE,
    TOGGLE_MUTE,
    
    // Brightness controls
    BRIGHTNESS_UP,
    BRIGHTNESS_DOWN,
    
    // UI controls
    SHOW_CONTROLS,
    HIDE_CONTROLS,
    TOGGLE_CONTROLS,
    LOCK_SCREEN,
    UNLOCK_SCREEN,
    TOGGLE_SCREEN_LOCK,
    
    // Display controls
    TOGGLE_FULLSCREEN,
    ENTER_FULLSCREEN,
    EXIT_FULLSCREEN,
    ROTATE_VIDEO,
    ZOOM_IN,
    ZOOM_OUT,
    RESET_ZOOM,
    ZOOM, // General zoom action
    
    // Playback mode controls
    TOGGLE_REPEAT,
    TOGGLE_SHUFFLE,
    CHANGE_PLAYBACK_MODE,
    
    // Speed controls
    INCREASE_SPEED,
    DECREASE_SPEED,
    RESET_SPEED,
    SET_SPEED,
    CHANGE_SPEED, // Alias for speed control
    
    // Audio/Video controls
    NEXT_AUDIO_TRACK,
    PREVIOUS_AUDIO_TRACK,
    TOGGLE_SUBTITLES,
    NEXT_SUBTITLE_TRACK,
    PREVIOUS_SUBTITLE_TRACK,
    
    // Advanced features
    TAKE_SCREENSHOT,
    SCREENSHOT, // Alias for TAKE_SCREENSHOT
    START_RECORDING,
    STOP_RECORDING,
    BOOKMARK_POSITION,
    BOOKMARK, // Alias for BOOKMARK_POSITION
    SHARE_VIDEO,
    
    // Equalizer controls
    OPEN_EQUALIZER,
    CLOSE_EQUALIZER,
    APPLY_EQUALIZER_PRESET,
    
    // AI features
    ANALYZE_VIDEO,
    GENERATE_SUBTITLES,
    ENHANCE_VIDEO,
    SUMMARIZE_VIDEO,
    AI_ENHANCE,
    AI_SUBTITLE,
    AI_TRANSLATE,
    AI_SUMMARY,
    
    // Gesture actions
    GESTURE_SEEK,
    GESTURE_VOLUME,
    GESTURE_BRIGHTNESS,
    GESTURE_ZOOM,
    
    // Menu actions
    SHOW_MORE_OPTIONS,
    SHOW_SETTINGS,
    SHOW_VIDEO_INFO,
    SHOW_SLEEP_TIMER,
    
    // Navigation
    PREVIOUS_VIDEO,
    NEXT_VIDEO,
    GOTO_CHAPTER,
    CHAPTER_NEXT,
    CHAPTER_PREVIOUS,
    
    // Custom actions
    CUSTOM_ACTION_1,
    CUSTOM_ACTION_2,
    CUSTOM_ACTION_3,
    
    // System actions
    ENTER_PIP_MODE,
    EXIT_PIP_MODE,
    PIP_MODE, // General PIP action
    MINIMIZE_PLAYER,
    CLOSE_PLAYER
}

/**
 * Extension functions for PlayerAction
 */
fun PlayerAction.isPlaybackControl(): Boolean {
    return when (this) {
        PlayerAction.PLAY, PlayerAction.PAUSE, PlayerAction.STOP, 
        PlayerAction.TOGGLE_PLAY_PAUSE -> true
        else -> false
    }
}

fun PlayerAction.isSeekControl(): Boolean {
    return when (this) {
        PlayerAction.SEEK_FORWARD, PlayerAction.SEEK_BACKWARD, 
        PlayerAction.SEEK_TO_POSITION, PlayerAction.FAST_FORWARD, 
        PlayerAction.FAST_REWIND -> true
        else -> false
    }
}

fun PlayerAction.isVolumeControl(): Boolean {
    return when (this) {
        PlayerAction.VOLUME_UP, PlayerAction.VOLUME_DOWN, 
        PlayerAction.MUTE, PlayerAction.UNMUTE, 
        PlayerAction.TOGGLE_MUTE -> true
        else -> false
    }
}

fun PlayerAction.isBrightnessControl(): Boolean {
    return when (this) {
        PlayerAction.BRIGHTNESS_UP, PlayerAction.BRIGHTNESS_DOWN -> true
        else -> false
    }
}

fun PlayerAction.isGestureAction(): Boolean {
    return when (this) {
        PlayerAction.GESTURE_SEEK, PlayerAction.GESTURE_VOLUME, 
        PlayerAction.GESTURE_BRIGHTNESS, PlayerAction.GESTURE_ZOOM -> true
        else -> false
    }
}

fun PlayerAction.requiresParameter(): Boolean {
    return when (this) {
        PlayerAction.SEEK_TO_POSITION, PlayerAction.SET_SPEED, 
        PlayerAction.APPLY_EQUALIZER_PRESET, PlayerAction.GOTO_CHAPTER -> true
        else -> false
    }
}

fun PlayerAction.getDisplayName(): String {
    return when (this) {
        PlayerAction.PLAY -> "Play"
        PlayerAction.PAUSE -> "Pause"
        PlayerAction.STOP -> "Stop"
        PlayerAction.TOGGLE_PLAY_PAUSE -> "Play/Pause"
        PlayerAction.SEEK_FORWARD -> "Seek Forward"
        PlayerAction.SEEK_BACKWARD -> "Seek Backward"
        PlayerAction.VOLUME_UP -> "Volume Up"
        PlayerAction.VOLUME_DOWN -> "Volume Down"
        PlayerAction.BRIGHTNESS_UP -> "Brightness Up"
        PlayerAction.BRIGHTNESS_DOWN -> "Brightness Down"
        PlayerAction.TOGGLE_FULLSCREEN -> "Toggle Fullscreen"
        PlayerAction.TAKE_SCREENSHOT -> "Take Screenshot"
        PlayerAction.SHOW_SETTINGS -> "Show Settings"
        else -> name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }
    }
}