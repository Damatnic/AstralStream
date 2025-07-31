package com.astralplayer.nextplayer.gesture

import android.content.Context
import android.media.AudioManager
import android.provider.Settings
import android.view.WindowManager
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.*
import kotlin.math.abs

class EnhancedGestureManager(private val context: Context) {
    
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    
    // Gesture sensitivity settings
    private var swipeSensitivity = 1.0f
    private var longPressThreshold = 800L
    private var doubleTapThreshold = 300L
    
    // Gesture zones (percentages of screen)
    private var leftZonePercent = 0.3f
    private var centerZonePercent = 0.4f
    private var rightZonePercent = 0.3f
    private var topZonePercent = 0.2f
    private var bottomZonePercent = 0.2f
    
    // Callbacks
    var onSeek: ((Long) -> Unit)? = null
    var onVolumeChange: ((Int) -> Unit)? = null
    var onBrightnessChange: ((Float) -> Unit)? = null
    var onSpeedChange: ((Float) -> Unit)? = null
    var onPlayPause: (() -> Unit)? = null
    
    fun handleSwipeGesture(deltaX: Float, deltaY: Float, screenWidth: Float, screenHeight: Float, startX: Float, startY: Float) {
        val zone = getGestureZone(startX, startY, screenWidth, screenHeight)
        
        when (zone) {
            GestureZone.LEFT -> {
                // Brightness control
                if (abs(deltaY) > abs(deltaX)) {
                    val brightnessChange = -deltaY / screenHeight * swipeSensitivity
                    adjustBrightness(brightnessChange)
                }
            }
            GestureZone.CENTER -> {
                // Seek control
                if (abs(deltaX) > abs(deltaY)) {
                    val seekAmount = (deltaX / screenWidth * 60000 * swipeSensitivity).toLong() // 60 seconds max
                    onSeek?.invoke(seekAmount)
                }
            }
            GestureZone.RIGHT -> {
                // Volume control
                if (abs(deltaY) > abs(deltaX)) {
                    val volumeChange = (-deltaY / screenHeight * 15 * swipeSensitivity).toInt() // Max 15 steps
                    adjustVolume(volumeChange)
                }
            }
            else -> {}
        }
    }
    
    fun handleLongPress(screenWidth: Float, screenHeight: Float, x: Float, y: Float) {
        val zone = getGestureZone(x, y, screenWidth, screenHeight)
        if (zone == GestureZone.CENTER) {
            // Speed control - start at 2x speed
            onSpeedChange?.invoke(2.0f)
        }
    }
    
    fun handleLongPressEnd() {
        // Return to normal speed
        onSpeedChange?.invoke(1.0f)
    }
    
    fun handleDoubleTap(screenWidth: Float, x: Float) {
        val seekAmount = if (x < screenWidth / 2) {
            -10000L // Seek backward 10 seconds
        } else {
            10000L // Seek forward 10 seconds
        }
        onSeek?.invoke(seekAmount)
    }
    
    fun handleSingleTap() {
        onPlayPause?.invoke()
    }
    
    private fun getGestureZone(x: Float, y: Float, screenWidth: Float, screenHeight: Float): GestureZone {
        val leftBoundary = screenWidth * leftZonePercent
        val rightBoundary = screenWidth * (leftZonePercent + centerZonePercent)
        val topBoundary = screenHeight * topZonePercent
        val bottomBoundary = screenHeight * (1 - bottomZonePercent)
        
        return when {
            y < topBoundary -> GestureZone.TOP
            y > bottomBoundary -> GestureZone.BOTTOM
            x < leftBoundary -> GestureZone.LEFT
            x < rightBoundary -> GestureZone.CENTER
            else -> GestureZone.RIGHT
        }
    }
    
    private fun adjustBrightness(change: Float) {
        try {
            val currentBrightness = Settings.System.getInt(
                context.contentResolver,
                Settings.System.SCREEN_BRIGHTNESS
            ) / 255.0f
            
            val newBrightness = (currentBrightness + change).coerceIn(0.0f, 1.0f)
            
            Settings.System.putInt(
                context.contentResolver,
                Settings.System.SCREEN_BRIGHTNESS,
                (newBrightness * 255).toInt()
            )
            
            onBrightnessChange?.invoke(newBrightness)
        } catch (e: Exception) {
            // Handle permission issues
        }
    }
    
    private fun adjustVolume(change: Int) {
        val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val newVolume = (currentVolume + change).coerceIn(0, maxVolume)
        
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newVolume, 0)
        onVolumeChange?.invoke(newVolume)
    }
    
    // Update settings
    fun updateSettings(
        swipeSensitivity: Float = this.swipeSensitivity,
        longPressThreshold: Long = this.longPressThreshold,
        doubleTapThreshold: Long = this.doubleTapThreshold,
        leftZonePercent: Float = this.leftZonePercent,
        centerZonePercent: Float = this.centerZonePercent,
        rightZonePercent: Float = this.rightZonePercent,
        topZonePercent: Float = this.topZonePercent,
        bottomZonePercent: Float = this.bottomZonePercent
    ) {
        this.swipeSensitivity = swipeSensitivity
        this.longPressThreshold = longPressThreshold
        this.doubleTapThreshold = doubleTapThreshold
        this.leftZonePercent = leftZonePercent
        this.centerZonePercent = centerZonePercent
        this.rightZonePercent = rightZonePercent
        this.topZonePercent = topZonePercent
        this.bottomZonePercent = bottomZonePercent
    }
}

enum class GestureZone {
    LEFT, CENTER, RIGHT, TOP, BOTTOM
}

@Composable
fun Modifier.mxPlayerGestures(
    gestureManager: EnhancedGestureManager,
    screenWidth: Float,
    screenHeight: Float,
    enabled: Boolean = true
): Modifier {
    if (!enabled) return this
    
    var lastTapTime by remember { mutableStateOf(0L) }
    var longPressJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }
    
    return this.pointerInput(Unit) {
        detectTapGestures(
            onTap = { offset ->
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastTapTime < 300) {
                    // Double tap
                    gestureManager.handleDoubleTap(screenWidth, offset.x)
                } else {
                    // Single tap
                    gestureManager.handleSingleTap()
                }
                lastTapTime = currentTime
            },
            onLongPress = { offset ->
                gestureManager.handleLongPress(screenWidth, screenHeight, offset.x, offset.y)
            }
        )
    }.pointerInput(Unit) {
        detectDragGestures(
            onDragStart = { offset ->
                // Start long press detection
                longPressJob = CoroutineScope(Dispatchers.Main).launch {
                    delay(800)
                    gestureManager.handleLongPress(screenWidth, screenHeight, offset.x, offset.y)
                }
            },
            onDragEnd = {
                longPressJob?.cancel()
                gestureManager.handleLongPressEnd()
            },
            onDrag = { change, dragAmount ->
                longPressJob?.cancel()
                gestureManager.handleSwipeGesture(
                    dragAmount.x,
                    dragAmount.y,
                    screenWidth,
                    screenHeight,
                    change.position.x,
                    change.position.y
                )
            }
        )
    }
}