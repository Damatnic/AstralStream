package com.astralplayer.nextplayer.data.gesture.accessibility

import android.content.Context
import android.os.Build
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityManager
import androidx.compose.ui.semantics.*
import com.astralplayer.nextplayer.data.GestureAction
import com.astralplayer.nextplayer.data.TouchSide
import com.astralplayer.nextplayer.data.GestureType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Manages accessibility features for gesture system
 */
class GestureAccessibilityManager(
    private val context: Context
) {
    private val accessibilityManager = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
    
    private val _accessibilityState = MutableStateFlow(AccessibilityState())
    val accessibilityState: StateFlow<AccessibilityState> = _accessibilityState.asStateFlow()
    
    init {
        updateAccessibilityState()
    }
    
    /**
     * Update accessibility state based on system settings
     */
    fun updateAccessibilityState() {
        _accessibilityState.value = AccessibilityState(
            isScreenReaderEnabled = accessibilityManager.isEnabled && 
                accessibilityManager.isTouchExplorationEnabled,
            isAccessibilityEnabled = accessibilityManager.isEnabled,
            preferReducedMotion = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                context.resources.configuration.fontScale > 1.2f // Approximation
            } else false
        )
    }
    
    /**
     * Announce gesture action to screen reader
     */
    fun announceGestureAction(action: GestureAction) {
        if (!_accessibilityState.value.isScreenReaderEnabled) return
        
        val announcement = when (action) {
            is GestureAction.Seek -> {
                val direction = if (action.deltaMs > 0) "forward" else "backward"
                val seconds = kotlin.math.abs(action.deltaMs / 1000)
                "Seeking $direction $seconds seconds"
            }
            is GestureAction.VolumeChange -> {
                val direction = if (action.delta > 0) "increased" else "decreased"
                val percent = (action.delta * 100).toInt()
                "Volume $direction by $percent percent"
            }
            is GestureAction.BrightnessChange -> {
                val direction = if (action.delta > 0) "increased" else "decreased"
                val percent = (action.delta * 100).toInt()
                "Brightness $direction by $percent percent"
            }
            is GestureAction.DoubleTapSeek -> {
                val direction = if (action.forward) "forward" else "backward"
                val seconds = action.amount / 1000
                "Double tap seek $direction $seconds seconds"
            }
            is GestureAction.TogglePlayPause -> "Play pause toggled"
            is GestureAction.LongPressSeek -> {
                "Long press seek at ${action.speed}x speed"
            }
            is GestureAction.PinchZoom -> {
                val percent = (action.scale * 100).toInt()
                "Zoom $percent percent"
            }
            is GestureAction.GestureConflict -> "Gesture conflict detected"
        }
        
        sendAccessibilityEvent(announcement)
    }
    
    /**
     * Send accessibility event
     */
    private fun sendAccessibilityEvent(text: String) {
        val event = AccessibilityEvent.obtain(AccessibilityEvent.TYPE_ANNOUNCEMENT)
        event.text.add(text)
        event.packageName = context.packageName
        accessibilityManager.sendAccessibilityEvent(event)
    }
    
    /**
     * Create semantic actions for gestures
     */
    fun createGestureSemantics(): SemanticsPropertyReceiver.() -> Unit = {
        // Volume control
        customActions = listOf(
            CustomAccessibilityAction("Increase Volume") {
                announceGestureAction(GestureAction.VolumeChange(0.1f, TouchSide.CENTER))
                true
            },
            CustomAccessibilityAction("Decrease Volume") {
                announceGestureAction(GestureAction.VolumeChange(-0.1f, TouchSide.CENTER))
                true
            },
            CustomAccessibilityAction("Seek Forward 10 seconds") {
                announceGestureAction(GestureAction.Seek(10000L))
                true
            },
            CustomAccessibilityAction("Seek Backward 10 seconds") {
                announceGestureAction(GestureAction.Seek(-10000L))
                true
            },
            CustomAccessibilityAction("Toggle Play/Pause") {
                announceGestureAction(GestureAction.TogglePlayPause)
                true
            }
        )
        
        // Content description
        contentDescription = "Video player with gesture controls. " +
            "Swipe horizontally to seek, " +
            "swipe vertically on right for volume, " +
            "swipe vertically on left for brightness, " +
            "double tap to seek, " +
            "long press for fast seek"
    }
    
    /**
     * Get accessible gesture description
     */
    fun getGestureDescription(gestureType: GestureType): String {
        return when (gestureType) {
            GestureType.HORIZONTAL_SEEK -> "Horizontal swipe to seek through video"
            GestureType.VERTICAL_VOLUME -> "Vertical swipe on right side to adjust volume"
            GestureType.VERTICAL_BRIGHTNESS -> "Vertical swipe on left side to adjust brightness"
            GestureType.SINGLE_TAP -> "Single tap to play or pause"
            GestureType.DOUBLE_TAP -> "Double tap to seek forward or backward"
            GestureType.LONG_PRESS -> "Long press for variable speed seeking"
            GestureType.PINCH_ZOOM -> "Pinch to zoom video"
        }
    }
    
    /**
     * Check if gesture should be simplified for accessibility
     */
    fun shouldSimplifyGesture(gestureType: GestureType): Boolean {
        val state = _accessibilityState.value
        
        return when {
            state.isScreenReaderEnabled -> {
                // Disable complex gestures when screen reader is active
                gestureType in listOf(
                    GestureType.PINCH_ZOOM,
                    GestureType.LONG_PRESS
                )
            }
            state.preferReducedMotion -> {
                // Reduce sensitivity for motion-sensitive users
                gestureType == GestureType.HORIZONTAL_SEEK
            }
            else -> false
        }
    }
    
    /**
     * Get accessibility-adjusted gesture settings
     */
    fun getAccessibilityAdjustedSettings(
        sensitivity: Float,
        gestureType: GestureType
    ): Float {
        val state = _accessibilityState.value
        
        return when {
            state.isScreenReaderEnabled -> {
                // Reduce sensitivity for screen reader users
                sensitivity * 0.7f
            }
            state.preferReducedMotion -> {
                // Further reduce for motion sensitivity
                sensitivity * 0.5f
            }
            else -> sensitivity
        }
    }
    
    data class AccessibilityState(
        val isScreenReaderEnabled: Boolean = false,
        val isAccessibilityEnabled: Boolean = false,
        val preferReducedMotion: Boolean = false
    )
}

/**
 * Alternative gesture input methods for accessibility
 */
class AccessibleGestureInput(
    private val context: Context
) {
    /**
     * Create accessible button controls as alternative to gestures
     */
    fun createAccessibleControls(): AccessibleControls {
        return AccessibleControls(
            seekBackward = AccessibleControl(
                label = "Seek Backward",
                contentDescription = "Seek backward 10 seconds",
                action = { GestureAction.Seek(-10000L) }
            ),
            seekForward = AccessibleControl(
                label = "Seek Forward", 
                contentDescription = "Seek forward 10 seconds",
                action = { GestureAction.Seek(10000L) }
            ),
            volumeUp = AccessibleControl(
                label = "Volume Up",
                contentDescription = "Increase volume",
                action = { GestureAction.VolumeChange(0.1f, TouchSide.CENTER) }
            ),
            volumeDown = AccessibleControl(
                label = "Volume Down",
                contentDescription = "Decrease volume",
                action = { GestureAction.VolumeChange(-0.1f, TouchSide.CENTER) }
            ),
            brightnessUp = AccessibleControl(
                label = "Brightness Up",
                contentDescription = "Increase brightness",
                action = { GestureAction.BrightnessChange(0.1f, TouchSide.CENTER) }
            ),
            brightnessDown = AccessibleControl(
                label = "Brightness Down",
                contentDescription = "Decrease brightness",
                action = { GestureAction.BrightnessChange(-0.1f, TouchSide.CENTER) }
            ),
            playPause = AccessibleControl(
                label = "Play/Pause",
                contentDescription = "Toggle play and pause",
                action = { GestureAction.TogglePlayPause }
            )
        )
    }
    
    data class AccessibleControls(
        val seekBackward: AccessibleControl,
        val seekForward: AccessibleControl,
        val volumeUp: AccessibleControl,
        val volumeDown: AccessibleControl,
        val brightnessUp: AccessibleControl,
        val brightnessDown: AccessibleControl,
        val playPause: AccessibleControl
    )
    
    data class AccessibleControl(
        val label: String,
        val contentDescription: String,
        val action: () -> GestureAction
    )
}

/**
 * Voice command support for gestures
 */
class VoiceGestureCommands {
    private val commands = mapOf(
        "play" to GestureAction.TogglePlayPause,
        "pause" to GestureAction.TogglePlayPause,
        "seek forward" to GestureAction.Seek(10000L),
        "seek backward" to GestureAction.Seek(-10000L),
        "skip forward" to GestureAction.DoubleTapSeek(true, 10000L, TouchSide.RIGHT),
        "skip backward" to GestureAction.DoubleTapSeek(false, 10000L, TouchSide.LEFT),
        "volume up" to GestureAction.VolumeChange(0.1f, TouchSide.CENTER),
        "volume down" to GestureAction.VolumeChange(-0.1f, TouchSide.CENTER),
        "brightness up" to GestureAction.BrightnessChange(0.1f, TouchSide.CENTER),
        "brightness down" to GestureAction.BrightnessChange(-0.1f, TouchSide.CENTER),
        "zoom in" to GestureAction.PinchZoom(1.2f, androidx.compose.ui.geometry.Offset.Zero),
        "zoom out" to GestureAction.PinchZoom(0.8f, androidx.compose.ui.geometry.Offset.Zero)
    )
    
    /**
     * Parse voice command to gesture action
     */
    fun parseCommand(command: String): GestureAction? {
        val normalizedCommand = command.lowercase().trim()
        
        // Direct command match
        commands[normalizedCommand]?.let { return it }
        
        // Pattern matching for seek commands with time
        val seekPattern = Regex("seek (forward|backward) (\\d+) seconds?")
        seekPattern.find(normalizedCommand)?.let { match ->
            val direction = match.groupValues[1]
            val seconds = match.groupValues[2].toIntOrNull() ?: return null
            val deltaMs = seconds * 1000L * if (direction == "forward") 1 else -1
            return GestureAction.Seek(deltaMs)
        }
        
        return null
    }
    
    /**
     * Get available voice commands
     */
    fun getAvailableCommands(): List<String> {
        return commands.keys.toList() + listOf(
            "seek forward [number] seconds",
            "seek backward [number] seconds"
        )
    }
}