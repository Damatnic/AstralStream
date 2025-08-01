package com.astralplayer.features.gestures.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index
import java.util.UUID

@Entity(
    tableName = "custom_gestures",
    indices = [
        Index(value = ["gestureType", "zone"], unique = true),
        Index(value = ["isEnabled"]),
        Index(value = ["priority"])
    ]
)
data class GestureEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val gestureType: GestureType,
    val zone: GestureZone,
    val action: GestureAction,
    val isEnabled: Boolean = true,
    val sensitivity: Float = 1.0f, // 0.5 to 2.0
    val requiredFingers: Int = 1,
    val direction: SwipeDirection? = null,
    val minimumDistance: Float = 50f, // in pixels
    val doubleTapTimeout: Long = 300L, // milliseconds
    val longPressTimeout: Long = 500L, // milliseconds
    val hapticFeedback: Boolean = true,
    val visualFeedback: Boolean = true,
    val priority: Int = 0, // Higher priority gestures are processed first
    val customParameters: String = "{}", // JSON for additional parameters
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

enum class GestureType {
    TAP,
    DOUBLE_TAP,
    LONG_PRESS,
    SWIPE_UP,
    SWIPE_DOWN,
    SWIPE_LEFT,
    SWIPE_RIGHT,
    PINCH_IN,
    PINCH_OUT,
    TWO_FINGER_TAP,
    TWO_FINGER_SWIPE,
    THREE_FINGER_TAP,
    ROTATION_CLOCKWISE,
    ROTATION_COUNTER_CLOCKWISE,
    DRAG,
    FLING
}

enum class GestureZone {
    FULL_SCREEN,
    LEFT_HALF,
    RIGHT_HALF,
    TOP_HALF,
    BOTTOM_HALF,
    CENTER,
    TOP_LEFT_QUARTER,
    TOP_RIGHT_QUARTER,
    BOTTOM_LEFT_QUARTER,
    BOTTOM_RIGHT_QUARTER,
    LEFT_EDGE,
    RIGHT_EDGE,
    TOP_EDGE,
    BOTTOM_EDGE,
    CUSTOM
}

enum class GestureAction {
    // Playback controls
    PLAY_PAUSE,
    SEEK_FORWARD,
    SEEK_BACKWARD,
    FAST_FORWARD,
    REWIND,
    NEXT_VIDEO,
    PREVIOUS_VIDEO,
    JUMP_TO_BEGINNING,
    JUMP_TO_END,
    
    // Volume controls
    VOLUME_UP,
    VOLUME_DOWN,
    MUTE_UNMUTE,
    
    // Brightness controls
    BRIGHTNESS_UP,
    BRIGHTNESS_DOWN,
    AUTO_BRIGHTNESS,
    
    // Speed controls
    SPEED_UP,
    SPEED_DOWN,
    RESET_SPEED,
    CYCLE_SPEED_PRESETS,
    
    // Subtitle controls
    TOGGLE_SUBTITLES,
    NEXT_SUBTITLE_TRACK,
    PREVIOUS_SUBTITLE_TRACK,
    SUBTITLE_DELAY_INCREASE,
    SUBTITLE_DELAY_DECREASE,
    SUBTITLE_SIZE_INCREASE,
    SUBTITLE_SIZE_DECREASE,
    
    // Audio controls
    NEXT_AUDIO_TRACK,
    PREVIOUS_AUDIO_TRACK,
    AUDIO_DELAY_INCREASE,
    AUDIO_DELAY_DECREASE,
    
    // UI controls
    SHOW_HIDE_CONTROLS,
    TOGGLE_FULLSCREEN,
    TOGGLE_PIP,
    OPEN_SETTINGS,
    OPEN_PLAYLIST,
    TOGGLE_LOCK,
    SCREENSHOT,
    
    // Zoom controls
    ZOOM_IN,
    ZOOM_OUT,
    RESET_ZOOM,
    FIT_TO_SCREEN,
    FILL_SCREEN,
    
    // Chapter controls
    NEXT_CHAPTER,
    PREVIOUS_CHAPTER,
    SHOW_CHAPTERS,
    
    // Custom actions
    CUSTOM_ACTION_1,
    CUSTOM_ACTION_2,
    CUSTOM_ACTION_3,
    
    // No action
    NONE
}

enum class SwipeDirection {
    UP,
    DOWN,
    LEFT,
    RIGHT,
    UP_LEFT,
    UP_RIGHT,
    DOWN_LEFT,
    DOWN_RIGHT
}

@Entity(
    tableName = "gesture_profiles",
    indices = [
        Index(value = ["name"], unique = true),
        Index(value = ["isActive"])
    ]
)
data class GestureProfileEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val description: String = "",
    val isActive: Boolean = false,
    val isBuiltIn: Boolean = false,
    val baseProfileId: String? = null, // For profiles based on built-in presets
    val iconName: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "gesture_profile_mappings",
    indices = [
        Index(value = ["profileId"]),
        Index(value = ["gestureId"])
    ]
)
data class GestureProfileMappingEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val profileId: String,
    val gestureId: String,
    val isOverride: Boolean = false // If true, overrides the default gesture
)

@Entity(
    tableName = "gesture_history",
    indices = [
        Index(value = ["gestureType"]),
        Index(value = ["timestamp"]),
        Index(value = ["wasSuccessful"])
    ]
)
data class GestureHistoryEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val gestureType: GestureType,
    val zone: GestureZone,
    val action: GestureAction,
    val timestamp: Long = System.currentTimeMillis(),
    val startX: Float,
    val startY: Float,
    val endX: Float,
    val endY: Float,
    val velocity: Float = 0f,
    val duration: Long = 0L,
    val fingerCount: Int = 1,
    val wasSuccessful: Boolean = true,
    val errorReason: String? = null
)

@Entity(
    tableName = "gesture_shortcuts",
    indices = [
        Index(value = ["shortcutKey"], unique = true),
        Index(value = ["isEnabled"])
    ]
)
data class GestureShortcutEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val shortcutKey: String, // e.g., "volume_control", "seek_control"
    val name: String,
    val description: String,
    val primaryGestureId: String,
    val alternativeGestureId: String? = null,
    val isEnabled: Boolean = true,
    val showTutorial: Boolean = true,
    val usageCount: Int = 0,
    val lastUsedAt: Long? = null
)