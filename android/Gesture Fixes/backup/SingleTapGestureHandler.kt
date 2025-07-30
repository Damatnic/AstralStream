package com.astralplayer.nextplayer.feature.player.gestures

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.*
import kotlin.math.*

/**
 * Enhanced single tap gesture handler with configurable zones and actions
 */
class SingleTapGestureHandler(
    private val onSingleTap: (action: SingleTapAction, position: Offset) -> Unit,
    private val onHapticFeedback: (intensity: HapticIntensity) -> Unit
) {
    
    private var tapJob: Job? = null
    private var isWaitingForDoubleTap = false
    
    suspend fun PointerInputScope.detectSingleTapGesture(
        settings: GeneralGestureSettings,
        doubleTapSettings: DoubleTapGestureSettings
    ) = coroutineScope {
        awaitEachGesture {
            if (!settings.tapToToggleControls) return@awaitEachGesture
            
            val firstDown = awaitFirstDown(requireUnconsumed = false)
            val tapPosition = firstDown.position
            val tapTime = System.currentTimeMillis()
            
            // Wait for tap to complete
            val up = waitForUpOrCancellation()
            if (up == null) return@awaitEachGesture // Gesture was cancelled
            
            // Determine tap zone and action
            val tapZone = determineTapZone(tapPosition, size.width.toFloat(), size.height.toFloat())
            val tapAction = getTapActionForZone(tapZone, settings)
            
            // If double tap is enabled, wait for potential second tap
            if (doubleTapSettings.isEnabled && tapZone != TapZone.DEAD_ZONE) {
                isWaitingForDoubleTap = true
                tapJob?.cancel()
                tapJob = launch {
                    delay(doubleTapSettings.tapTimeout)
                    if (isWaitingForDoubleTap) {
                        isWaitingForDoubleTap = false
                        executeSingleTap(tapAction, tapPosition, settings)
                    }
                }
            } else {
                // Execute single tap immediately
                executeSingleTap(tapAction, tapPosition, settings)
            }
        }
    }
    
    private fun executeSingleTap(action: SingleTapAction, position: Offset, settings: GeneralGestureSettings) {
        if (action != SingleTapAction.NONE) {
            // Provide haptic feedback
            onHapticFeedback(HapticIntensity.LIGHT)
            
            // Execute the action
            onSingleTap(action, position)
        }
    }
    
    private fun determineTapZone(position: Offset, screenWidth: Float, screenHeight: Float): TapZone {
        val x = position.x / screenWidth
        val y = position.y / screenHeight
        
        // Define zones based on screen position
        return when {
            // Dead zones (no action areas)
            x > 0.4f && x < 0.6f && y > 0.4f && y < 0.6f -> TapZone.DEAD_ZONE
            
            // Edge zones
            x < 0.1f -> TapZone.LEFT_EDGE
            x > 0.9f -> TapZone.RIGHT_EDGE
            y < 0.1f -> TapZone.TOP_EDGE
            y > 0.9f -> TapZone.BOTTOM_EDGE
            
            // Corner zones
            x < 0.2f && y < 0.2f -> TapZone.TOP_LEFT
            x > 0.8f && y < 0.2f -> TapZone.TOP_RIGHT
            x < 0.2f && y > 0.8f -> TapZone.BOTTOM_LEFT
            x > 0.8f && y > 0.8f -> TapZone.BOTTOM_RIGHT
            
            // Side zones
            x < 0.3f -> TapZone.LEFT_SIDE
            x > 0.7f -> TapZone.RIGHT_SIDE
            y < 0.3f -> TapZone.TOP_SIDE
            y > 0.7f -> TapZone.BOTTOM_SIDE
            
            // Center zone
            else -> TapZone.CENTER
        }
    }
    
    private fun getTapActionForZone(zone: TapZone, settings: GeneralGestureSettings): SingleTapAction {
        return when (zone) {
            TapZone.CENTER -> SingleTapAction.TOGGLE_CONTROLS
            TapZone.LEFT_SIDE, TapZone.RIGHT_SIDE -> SingleTapAction.TOGGLE_CONTROLS
            TapZone.TOP_SIDE -> SingleTapAction.SHOW_INFO
            TapZone.BOTTOM_SIDE -> SingleTapAction.TOGGLE_CONTROLS
            TapZone.TOP_LEFT -> SingleTapAction.SHOW_SETTINGS
            TapZone.TOP_RIGHT -> SingleTapAction.SHOW_MENU
            TapZone.BOTTOM_LEFT -> SingleTapAction.SHOW_PLAYLIST
            TapZone.BOTTOM_RIGHT -> SingleTapAction.SHOW_INFO
            TapZone.LEFT_EDGE, TapZone.RIGHT_EDGE -> SingleTapAction.NONE
            TapZone.TOP_EDGE, TapZone.BOTTOM_EDGE -> SingleTapAction.NONE
            TapZone.DEAD_ZONE -> SingleTapAction.NONE
        }
    }
    
    fun cancelPendingTap() {
        isWaitingForDoubleTap = false
        tapJob?.cancel()
        tapJob = null
    }
    
    fun isPendingTap(): Boolean = isWaitingForDoubleTap
}

/**
 * Single tap zones for different screen areas
 */
enum class TapZone {
    CENTER,
    LEFT_SIDE, RIGHT_SIDE, TOP_SIDE, BOTTOM_SIDE,
    TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT,
    LEFT_EDGE, RIGHT_EDGE, TOP_EDGE, BOTTOM_EDGE,
    DEAD_ZONE
}

/**
 * Single tap actions
 */
enum class SingleTapAction {
    NONE,
    TOGGLE_CONTROLS,
    SHOW_INFO,
    SHOW_SETTINGS,
    SHOW_MENU,
    SHOW_PLAYLIST,
    TOGGLE_PLAY_PAUSE,
    SHOW_CHAPTERS,
    SHOW_SUBTITLES,
    SHOW_AUDIO_TRACKS,
    TOGGLE_FULLSCREEN
}

/**
 * Single tap visual feedback overlay
 */
@Composable
fun SingleTapFeedbackOverlay(
    isVisible: Boolean,
    action: SingleTapAction,
    position: Offset,
    modifier: Modifier = Modifier
) {
    if (!isVisible || action == SingleTapAction.NONE) return
    
    val density = LocalDensity.current
    
    // Animation states
    val scaleAnimation by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "singleTapScale"
    )
    
    val fadeAnimation by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(300),
        label = "singleTapFade"
    )
    
    val rippleAnimation by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(400, easing = FastOutSlowInEasing),
        label = "singleTapRipple"
    )
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .scale(scaleAnimation)
            .alpha(fadeAnimation)
    ) {
        // Position the feedback at the tap location
        val offsetX = with(density) { position.x.toDp() }
        val offsetY = with(density) { position.y.toDp() }
        
        Box(
            modifier = Modifier
                .offset(x = offsetX - 40.dp, y = offsetY - 40.dp)
                .size(80.dp),
            contentAlignment = Alignment.Center
        ) {
            // Ripple effect
            Canvas(
                modifier = Modifier.fillMaxSize()
            ) {
                drawSingleTapRipple(
                    center = Offset(size.width / 2, size.height / 2),
                    progress = rippleAnimation,
                    action = action
                )
            }
            
            // Action icon
            Card(
                modifier = Modifier.size(50.dp),
                shape = CircleShape,
                colors = CardDefaults.cardColors(
                    containerColor = getActionColor(action).copy(alpha = 0.9f)
                )
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = getActionIcon(action),
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

/**
 * Tap zone visualization for debugging and user education
 */
@Composable
fun TapZoneVisualization(
    isVisible: Boolean,
    modifier: Modifier = Modifier
) {
    if (!isVisible) return
    
    val alpha by animateFloatAsState(
        targetValue = if (isVisible) 0.3f else 0f,
        animationSpec = tween(300),
        label = "tapZoneAlpha"
    )
    
    Canvas(
        modifier = modifier
            .fillMaxSize()
            .alpha(alpha)
    ) {
        drawTapZones()
    }
}

/**
 * Configurable tap zones manager
 */
class TapZoneManager {
    private val customZones = mutableMapOf<TapZone, TapZoneConfig>()
    
    data class TapZoneConfig(
        val bounds: TapZoneBounds,
        val action: SingleTapAction,
        val isEnabled: Boolean = true,
        val hapticFeedback: Boolean = true
    )
    
    data class TapZoneBounds(
        val left: Float,
        val top: Float,
        val right: Float,
        val bottom: Float
    )
    
    fun configureZone(zone: TapZone, config: TapZoneConfig) {
        customZones[zone] = config
    }
    
    fun getZoneConfig(zone: TapZone): TapZoneConfig? {
        return customZones[zone]
    }
    
    fun isZoneEnabled(zone: TapZone): Boolean {
        return customZones[zone]?.isEnabled ?: true
    }
    
    fun getZoneAction(zone: TapZone): SingleTapAction {
        return customZones[zone]?.action ?: getDefaultActionForZone(zone)
    }
    
    private fun getDefaultActionForZone(zone: TapZone): SingleTapAction {
        return when (zone) {
            TapZone.CENTER -> SingleTapAction.TOGGLE_CONTROLS
            TapZone.LEFT_SIDE, TapZone.RIGHT_SIDE -> SingleTapAction.TOGGLE_CONTROLS
            TapZone.TOP_SIDE -> SingleTapAction.SHOW_INFO
            TapZone.BOTTOM_SIDE -> SingleTapAction.TOGGLE_CONTROLS
            TapZone.TOP_LEFT -> SingleTapAction.SHOW_SETTINGS
            TapZone.TOP_RIGHT -> SingleTapAction.SHOW_MENU
            TapZone.BOTTOM_LEFT -> SingleTapAction.SHOW_PLAYLIST
            TapZone.BOTTOM_RIGHT -> SingleTapAction.SHOW_INFO
            else -> SingleTapAction.NONE
        }
    }
    
    fun resetToDefaults() {
        customZones.clear()
    }
    
    fun exportConfiguration(): Map<TapZone, TapZoneConfig> {
        return customZones.toMap()
    }
    
    fun importConfiguration(config: Map<TapZone, TapZoneConfig>) {
        customZones.clear()
        customZones.putAll(config)
    }
}

/**
 * Single tap gesture analytics
 */
class SingleTapGestureAnalytics {
    private val tapData = mutableListOf<SingleTapData>()
    
    data class SingleTapData(
        val timestamp: Long,
        val zone: TapZone,
        val action: SingleTapAction,
        val position: Offset,
        val wasIntended: Boolean, // Whether the tap achieved the user's intended action
        val responseTime: Long // Time from tap to action execution
    )
    
    fun recordTap(
        zone: TapZone,
        action: SingleTapAction,
        position: Offset,
        wasIntended: Boolean,
        responseTime: Long
    ) {
        val data = SingleTapData(
            timestamp = System.currentTimeMillis(),
            zone = zone,
            action = action,
            position = position,
            wasIntended = wasIntended,
            responseTime = responseTime
        )
        
        tapData.add(data)
        
        // Limit data size
        if (tapData.size > 1000) {
            tapData.removeAt(0)
        }
    }
    
    fun getMostUsedZone(): TapZone {
        return if (tapData.isNotEmpty()) {
            tapData.groupBy { it.zone }
                .maxByOrNull { it.value.size }?.key ?: TapZone.CENTER
        } else {
            TapZone.CENTER
        }
    }
    
    fun getMostUsedAction(): SingleTapAction {
        return if (tapData.isNotEmpty()) {
            tapData.groupBy { it.action }
                .maxByOrNull { it.value.size }?.key ?: SingleTapAction.TOGGLE_CONTROLS
        } else {
            SingleTapAction.TOGGLE_CONTROLS
        }
    }
    
    fun getIntentionAccuracy(): Float {
        return if (tapData.isNotEmpty()) {
            tapData.count { it.wasIntended }.toFloat() / tapData.size
        } else {
            1f
        }
    }
    
    fun getAverageResponseTime(): Long {
        return if (tapData.isNotEmpty()) {
            tapData.map { it.responseTime }.average().toLong()
        } else {
            50L
        }
    }
    
    fun getZoneUsageFrequency(): Map<TapZone, Float> {
        if (tapData.isEmpty()) return emptyMap()
        
        val totalTaps = tapData.size
        return tapData.groupBy { it.zone }
            .mapValues { (_, taps) -> taps.size.toFloat() / totalTaps }
    }
    
    fun getSuggestedZoneOptimizations(): List<ZoneOptimization> {
        val optimizations = mutableListOf<ZoneOptimization>()
        val zoneUsage = getZoneUsageFrequency()
        val intentionAccuracy = getIntentionAccuracy()
        
        // Suggest enlarging frequently used zones
        zoneUsage.forEach { (zone, frequency) ->
            if (frequency > 0.3f) { // Used more than 30% of the time
                optimizations.add(
                    ZoneOptimization(
                        zone = zone,
                        type = OptimizationType.ENLARGE_ZONE,
                        reason = "Frequently used zone (${(frequency * 100).toInt()}% of taps)"
                    )
                )
            }
        }
        
        // Suggest disabling rarely used zones
        zoneUsage.forEach { (zone, frequency) ->
            if (frequency < 0.05f) { // Used less than 5% of the time
                optimizations.add(
                    ZoneOptimization(
                        zone = zone,
                        type = OptimizationType.DISABLE_ZONE,
                        reason = "Rarely used zone (${(frequency * 100).toInt()}% of taps)"
                    )
                )
            }
        }
        
        // Suggest improving zones with low intention accuracy
        if (intentionAccuracy < 0.7f) {
            optimizations.add(
                ZoneOptimization(
                    zone = TapZone.CENTER, // Generic suggestion
                    type = OptimizationType.IMPROVE_FEEDBACK,
                    reason = "Low intention accuracy (${(intentionAccuracy * 100).toInt()}%)"
                )
            )
        }
        
        return optimizations
    }
    
    data class ZoneOptimization(
        val zone: TapZone,
        val type: OptimizationType,
        val reason: String
    )
    
    enum class OptimizationType {
        ENLARGE_ZONE, SHRINK_ZONE, DISABLE_ZONE, CHANGE_ACTION, IMPROVE_FEEDBACK
    }
}

// Helper functions
private fun getActionIcon(action: SingleTapAction): androidx.compose.ui.graphics.vector.ImageVector {
    return when (action) {
        SingleTapAction.TOGGLE_CONTROLS -> Icons.Default.TouchApp
        SingleTapAction.SHOW_INFO -> Icons.Default.Info
        SingleTapAction.SHOW_SETTINGS -> Icons.Default.Settings
        SingleTapAction.SHOW_MENU -> Icons.Default.Menu
        SingleTapAction.SHOW_PLAYLIST -> Icons.Default.PlaylistPlay
        SingleTapAction.TOGGLE_PLAY_PAUSE -> Icons.Default.PlayArrow
        SingleTapAction.SHOW_CHAPTERS -> Icons.Default.Bookmark
        SingleTapAction.SHOW_SUBTITLES -> Icons.Default.Subtitles
        SingleTapAction.SHOW_AUDIO_TRACKS -> Icons.Default.AudioFile
        SingleTapAction.TOGGLE_FULLSCREEN -> Icons.Default.Fullscreen
        SingleTapAction.NONE -> Icons.Default.TouchApp
    }
}

private fun getActionColor(action: SingleTapAction): Color {
    return when (action) {
        SingleTapAction.TOGGLE_CONTROLS -> Color(0xFF00BCD4)
        SingleTapAction.SHOW_INFO -> Color(0xFF2196F3)
        SingleTapAction.SHOW_SETTINGS -> Color(0xFF9C27B0)
        SingleTapAction.SHOW_MENU -> Color(0xFF607D8B)
        SingleTapAction.SHOW_PLAYLIST -> Color(0xFF4CAF50)
        SingleTapAction.TOGGLE_PLAY_PAUSE -> Color(0xFFFF5722)
        SingleTapAction.SHOW_CHAPTERS -> Color(0xFFFF9800)
        SingleTapAction.SHOW_SUBTITLES -> Color(0xFF795548)
        SingleTapAction.SHOW_AUDIO_TRACKS -> Color(0xFFE91E63)
        SingleTapAction.TOGGLE_FULLSCREEN -> Color(0xFF3F51B5)
        SingleTapAction.NONE -> Color(0xFF757575)
    }
}

// Custom drawing functions
private fun DrawScope.drawSingleTapRipple(
    center: Offset,
    progress: Float,
    action: SingleTapAction
) {
    val maxRadius = size.minDimension / 2
    val rippleRadius = maxRadius * progress
    val rippleAlpha = (1f - progress) * 0.4f
    
    val color = getActionColor(action)
    
    // Draw ripple effect
    if (rippleRadius > 0f && rippleAlpha > 0f) {
        drawCircle(
            color = color,
            radius = rippleRadius,
            center = center,
            alpha = rippleAlpha
        )
    }
}

private fun DrawScope.drawTapZones() {
    val zoneColors = mapOf(
        TapZone.CENTER to Color(0xFF00BCD4),
        TapZone.LEFT_SIDE to Color(0xFF4CAF50),
        TapZone.RIGHT_SIDE to Color(0xFF4CAF50),
        TapZone.TOP_SIDE to Color(0xFF2196F3),
        TapZone.BOTTOM_SIDE to Color(0xFF2196F3),
        TapZone.TOP_LEFT to Color(0xFF9C27B0),
        TapZone.TOP_RIGHT to Color(0xFF9C27B0),
        TapZone.BOTTOM_LEFT to Color(0xFF9C27B0),
        TapZone.BOTTOM_RIGHT to Color(0xFF9C27B0),
        TapZone.DEAD_ZONE to Color(0xFFFF5722)
    )
    
    val width = size.width
    val height = size.height
    
    // Draw zone boundaries
    zoneColors.forEach { (zone, color) ->
        val bounds = getZoneBounds(zone, width, height)
        drawRect(
            color = color,
            topLeft = Offset(bounds.left, bounds.top),
            size = androidx.compose.ui.geometry.Size(
                bounds.right - bounds.left,
                bounds.bottom - bounds.top
            ),
            alpha = 0.3f
        )
    }
}

private fun getZoneBounds(zone: TapZone, width: Float, height: Float): TapZoneManager.TapZoneBounds {
    return when (zone) {
        TapZone.CENTER -> TapZoneManager.TapZoneBounds(
            width * 0.3f, height * 0.3f, width * 0.7f, height * 0.7f
        )
        TapZone.LEFT_SIDE -> TapZoneManager.TapZoneBounds(
            0f, height * 0.2f, width * 0.3f, height * 0.8f
        )
        TapZone.RIGHT_SIDE -> TapZoneManager.TapZoneBounds(
            width * 0.7f, height * 0.2f, width, height * 0.8f
        )
        TapZone.TOP_SIDE -> TapZoneManager.TapZoneBounds(
            width * 0.2f, 0f, width * 0.8f, height * 0.3f
        )
        TapZone.BOTTOM_SIDE -> TapZoneManager.TapZoneBounds(
            width * 0.2f, height * 0.7f, width * 0.8f, height
        )
        TapZone.TOP_LEFT -> TapZoneManager.TapZoneBounds(
            0f, 0f, width * 0.2f, height * 0.2f
        )
        TapZone.TOP_RIGHT -> TapZoneManager.TapZoneBounds(
            width * 0.8f, 0f, width, height * 0.2f
        )
        TapZone.BOTTOM_LEFT -> TapZoneManager.TapZoneBounds(
            0f, height * 0.8f, width * 0.2f, height
        )
        TapZone.BOTTOM_RIGHT -> TapZoneManager.TapZoneBounds(
            width * 0.8f, height * 0.8f, width, height
        )
        TapZone.DEAD_ZONE -> TapZoneManager.TapZoneBounds(
            width * 0.4f, height * 0.4f, width * 0.6f, height * 0.6f
        )
        else -> TapZoneManager.TapZoneBounds(0f, 0f, 0f, 0f)
    }
}