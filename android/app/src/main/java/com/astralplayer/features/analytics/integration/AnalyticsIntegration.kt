package com.astralplayer.features.analytics.integration

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.astralplayer.features.analytics.data.FeatureCategory
import com.astralplayer.features.analytics.service.AnalyticsTracker
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

/**
 * Composable helper for integrating analytics tracking into video player screens
 */
@UnstableApi
@Composable
fun rememberAnalyticsTracker(
    player: Player?,
    videoId: String,
    videoTitle: String,
    source: String? = null,
    analyticsTracker: AnalyticsTracker
): AnalyticsTracker {
    val context = LocalContext.current
    
    DisposableEffect(player, videoId) {
        if (player != null) {
            // Start tracking
            analyticsTracker.startTracking(videoId, videoTitle, source)
            
            // Add listeners
            player.addListener(analyticsTracker)
            if (player is ExoPlayer) {
                player.addAnalyticsListener(analyticsTracker)
            }
        }
        
        onDispose {
            if (player != null) {
                // Remove listeners
                player.removeListener(analyticsTracker)
                if (player is ExoPlayer) {
                    player.removeAnalyticsListener(analyticsTracker)
                }
                
                // Stop tracking
                analyticsTracker.stopTracking()
            }
        }
    }
    
    // Track playback position periodically
    LaunchedEffect(player) {
        while (isActive && player != null) {
            player.currentPosition.let { position ->
                analyticsTracker.updatePlaybackPosition(position)
            }
            player.duration.let { duration ->
                if (duration > 0) {
                    analyticsTracker.updateVideoDuration(duration)
                }
            }
            delay(1000) // Update every second
        }
    }
    
    return analyticsTracker
}

/**
 * Extension functions for easy feature tracking
 */
fun AnalyticsTracker.trackPlaylistAction(action: String) {
    trackFeatureUsage("playlist_$action", FeatureCategory.PLAYLIST)
}

fun AnalyticsTracker.trackSettingsChange(setting: String, value: Any) {
    trackFeatureUsage("settings_$setting", FeatureCategory.SETTINGS)
}

fun AnalyticsTracker.trackUINavigation(screen: String) {
    trackFeatureUsage("navigate_$screen", FeatureCategory.UI_NAVIGATION)
}

fun AnalyticsTracker.trackSharingAction(type: String) {
    trackFeatureUsage("share_$type", FeatureCategory.SHARING)
}

fun AnalyticsTracker.trackSubtitleAction(action: String) {
    trackFeatureUsage("subtitle_$action", FeatureCategory.SUBTITLE)
}

fun AnalyticsTracker.trackAudioAction(action: String) {
    trackFeatureUsage("audio_$action", FeatureCategory.AUDIO)
}

fun AnalyticsTracker.trackQualityChange(quality: String) {
    trackFeatureUsage("quality_$quality", FeatureCategory.VIDEO_QUALITY)
}

fun AnalyticsTracker.trackAdvancedFeature(feature: String) {
    trackFeatureUsage(feature, FeatureCategory.ADVANCED)
}

/**
 * Analytics event builder for complex events
 */
class AnalyticsEventBuilder {
    private val data = mutableMapOf<String, Any>()
    
    fun put(key: String, value: Any): AnalyticsEventBuilder {
        data[key] = value
        return this
    }
    
    fun putAll(map: Map<String, Any>): AnalyticsEventBuilder {
        data.putAll(map)
        return this
    }
    
    fun build(): Map<String, Any> = data.toMap()
}

/**
 * Helper for tracking user engagement metrics
 */
class EngagementTracker(
    private val analyticsTracker: AnalyticsTracker
) {
    private var interactionCount = 0
    private var sessionStartTime = System.currentTimeMillis()
    
    fun trackInteraction(type: String) {
        interactionCount++
        analyticsTracker.trackFeatureUsage(
            "interaction_$type",
            FeatureCategory.UI_NAVIGATION
        )
    }
    
    fun getEngagementScore(): Float {
        val sessionDuration = System.currentTimeMillis() - sessionStartTime
        val durationMinutes = sessionDuration / 60000f
        
        // Simple engagement score based on interactions per minute
        return if (durationMinutes > 0) {
            (interactionCount / durationMinutes).coerceIn(0f, 10f)
        } else 0f
    }
    
    fun reset() {
        interactionCount = 0
        sessionStartTime = System.currentTimeMillis()
    }
}