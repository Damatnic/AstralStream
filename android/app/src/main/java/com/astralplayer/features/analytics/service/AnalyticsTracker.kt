package com.astralplayer.features.analytics.service

import android.content.Context
import android.util.Log
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.common.text.CueGroup
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.analytics.AnalyticsListener
import androidx.media3.exoplayer.analytics.PlaybackStatsListener
import com.astralplayer.features.analytics.data.*
import com.astralplayer.features.analytics.repository.AnalyticsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

@UnstableApi
@Singleton
class AnalyticsTracker @Inject constructor(
    @ApplicationContext private val context: Context,
    private val analyticsRepository: AnalyticsRepository
) : Player.Listener, AnalyticsListener {
    
    companion object {
        private const val TAG = "AnalyticsTracker"
        private const val MIN_WATCH_TIME_MS = 1000L // Minimum 1 second watch time to track
    }
    
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var currentVideoId: String? = null
    private var currentVideoTitle: String? = null
    private var videoDuration: Long = 0L
    private var sessionStartTime: Long = 0L
    private var totalWatchTime: Long = 0L
    private var lastPlaybackPosition: Long = 0L
    private var isPlaying = AtomicBoolean(false)
    private var playbackSpeed: Float = 1.0f
    private var volume: Float = 1.0f
    private var isFullscreen: Boolean = false
    private var subtitleEnabled: Boolean = false
    private var subtitleLanguage: String? = null
    private var videoQuality: String? = null
    
    // Performance tracking
    private var videoLoadStartTime: Long = 0L
    private var seekStartTime: Long = 0L
    private var bufferingStartTime: Long = 0L
    private var frameDropCount: Int = 0
    
    fun startTracking(videoId: String, videoTitle: String, source: String? = null) {
        Log.d(TAG, "Starting analytics tracking for: $videoTitle")
        
        currentVideoId = videoId
        currentVideoTitle = videoTitle
        sessionStartTime = System.currentTimeMillis()
        totalWatchTime = 0L
        lastPlaybackPosition = 0L
        videoLoadStartTime = System.currentTimeMillis()
        
        analyticsRepository.startSession(videoId, videoTitle, source)
    }
    
    fun stopTracking() {
        if (currentVideoId == null) return
        
        Log.d(TAG, "Stopping analytics tracking")
        
        // Update final watch time
        if (isPlaying.get()) {
            totalWatchTime += System.currentTimeMillis() - sessionStartTime
        }
        
        // Calculate completion percentage
        val completionPercentage = if (videoDuration > 0) {
            (lastPlaybackPosition.toFloat() / videoDuration * 100).coerceIn(0f, 100f)
        } else 0f
        
        analyticsRepository.endSession(
            totalWatchTime = totalWatchTime,
            videoDuration = videoDuration,
            completionPercentage = completionPercentage
        )
        
        currentVideoId = null
        currentVideoTitle = null
    }
    
    // Player.Listener implementation
    override fun onPlaybackStateChanged(playbackState: Int) {
        when (playbackState) {
            Player.STATE_READY -> {
                if (videoLoadStartTime > 0) {
                    val loadTime = System.currentTimeMillis() - videoLoadStartTime
                    trackPerformanceMetric(
                        PerformanceMetricType.VIDEO_LOAD_TIME,
                        loadTime.toFloat(),
                        "ms"
                    )
                    videoLoadStartTime = 0L
                }
            }
            Player.STATE_BUFFERING -> {
                bufferingStartTime = System.currentTimeMillis()
                logPlaybackEvent(PlaybackEventType.BUFFER_START)
            }
            Player.STATE_ENDED -> {
                logPlaybackEvent(PlaybackEventType.SESSION_END)
            }
        }
    }
    
    override fun onIsPlayingChanged(isPlaying: Boolean) {
        this.isPlaying.set(isPlaying)
        
        if (isPlaying) {
            sessionStartTime = System.currentTimeMillis()
            logPlaybackEvent(PlaybackEventType.PLAY)
        } else {
            totalWatchTime += System.currentTimeMillis() - sessionStartTime
            logPlaybackEvent(PlaybackEventType.PAUSE)
        }
    }
    
    override fun onPlaybackParametersChanged(playbackParameters: androidx.media3.common.PlaybackParameters) {
        if (playbackSpeed != playbackParameters.speed) {
            playbackSpeed = playbackParameters.speed
            logPlaybackEvent(PlaybackEventType.SPEED_CHANGE)
        }
    }
    
    override fun onVideoSizeChanged(videoSize: VideoSize) {
        // Track video quality changes
        val quality = "${videoSize.width}x${videoSize.height}"
        if (videoQuality != quality) {
            videoQuality = quality
            logPlaybackEvent(PlaybackEventType.QUALITY_CHANGE)
        }
    }
    
    override fun onCues(cueGroup: CueGroup) {
        // Track subtitle changes
        val hasSubtitles = cueGroup.cues.isNotEmpty()
        if (subtitleEnabled != hasSubtitles) {
            subtitleEnabled = hasSubtitles
            logPlaybackEvent(PlaybackEventType.SUBTITLE_CHANGE)
        }
    }
    
    // AnalyticsListener implementation
    override fun onSeekStarted(
        eventTime: AnalyticsListener.EventTime,
        seekStartPositionMs: Long,
        seekTargetPositionMs: Long
    ) {
        seekStartTime = System.currentTimeMillis()
        logPlaybackEvent(PlaybackEventType.SEEK)
    }
    
    override fun onSeekProcessed(eventTime: AnalyticsListener.EventTime) {
        if (seekStartTime > 0) {
            val seekTime = System.currentTimeMillis() - seekStartTime
            trackPerformanceMetric(
                PerformanceMetricType.SEEK_RESPONSE_TIME,
                seekTime.toFloat(),
                "ms"
            )
            seekStartTime = 0L
        }
    }
    
    override fun onDroppedVideoFrames(
        eventTime: AnalyticsListener.EventTime,
        droppedFrames: Int,
        elapsedMs: Long
    ) {
        frameDropCount += droppedFrames
        val dropRate = droppedFrames.toFloat() / (elapsedMs / 1000f)
        trackPerformanceMetric(
            PerformanceMetricType.FRAME_DROP_RATE,
            dropRate,
            "fps"
        )
    }
    
    override fun onRenderedFirstFrame(
        eventTime: AnalyticsListener.EventTime,
        output: Any,
        renderTimeMs: Long
    ) {
        trackPerformanceMetric(
            PerformanceMetricType.RENDER_TIME,
            renderTimeMs.toFloat(),
            "ms"
        )
    }
    
    // Feature tracking methods
    fun trackFeatureUsage(featureName: String, category: FeatureCategory) {
        analyticsRepository.trackFeatureUsage(featureName, category)
    }
    
    fun trackGestureUsage(gestureType: String) {
        logPlaybackEvent(PlaybackEventType.GESTURE_USED)
        trackFeatureUsage("gesture_$gestureType", FeatureCategory.GESTURE)
    }
    
    fun trackScreenshot() {
        logPlaybackEvent(PlaybackEventType.SCREENSHOT_TAKEN)
        trackFeatureUsage("screenshot", FeatureCategory.ADVANCED)
    }
    
    fun trackSubtitleGeneration(generationTime: Long) {
        trackPerformanceMetric(
            PerformanceMetricType.SUBTITLE_GENERATION_TIME,
            generationTime.toFloat(),
            "ms"
        )
        trackFeatureUsage("ai_subtitle_generation", FeatureCategory.SUBTITLE)
    }
    
    fun trackFullscreenToggle(isEntering: Boolean) {
        isFullscreen = isEntering
        logPlaybackEvent(
            if (isEntering) PlaybackEventType.FULLSCREEN_ENTER 
            else PlaybackEventType.FULLSCREEN_EXIT
        )
    }
    
    fun trackPipToggle(isEntering: Boolean) {
        logPlaybackEvent(
            if (isEntering) PlaybackEventType.PIP_ENTER 
            else PlaybackEventType.PIP_EXIT
        )
        trackFeatureUsage("picture_in_picture", FeatureCategory.UI_NAVIGATION)
    }
    
    fun trackError(error: String) {
        logPlaybackEvent(PlaybackEventType.ERROR, additionalData = mapOf("error" to error))
    }
    
    // Helper methods
    private fun logPlaybackEvent(
        eventType: PlaybackEventType,
        additionalData: Map<String, Any> = emptyMap()
    ) {
        val videoId = currentVideoId ?: return
        val videoTitle = currentVideoTitle ?: return
        
        val data = additionalData + mapOf(
            "speed" to playbackSpeed,
            "volume" to volume,
            "fullscreen" to isFullscreen,
            "subtitleEnabled" to subtitleEnabled,
            "subtitleLanguage" to (subtitleLanguage ?: ""),
            "quality" to (videoQuality ?: "")
        )
        
        analyticsRepository.logPlaybackEvent(
            eventType = eventType,
            videoId = videoId,
            videoTitle = videoTitle,
            videoDuration = videoDuration,
            playbackPosition = lastPlaybackPosition,
            additionalData = data
        )
    }
    
    private fun trackPerformanceMetric(
        type: PerformanceMetricType,
        value: Float,
        unit: String
    ) {
        analyticsRepository.trackPerformanceMetric(
            type = type,
            value = value,
            unit = unit,
            videoId = currentVideoId
        )
    }
    
    fun updatePlaybackPosition(position: Long) {
        lastPlaybackPosition = position
    }
    
    fun updateVideoDuration(duration: Long) {
        videoDuration = duration
    }
    
    fun updateVolume(newVolume: Float) {
        if (volume != newVolume) {
            volume = newVolume
            logPlaybackEvent(PlaybackEventType.VOLUME_CHANGE)
        }
    }
    
    fun updateBrightness(brightness: Float) {
        logPlaybackEvent(PlaybackEventType.BRIGHTNESS_CHANGE, mapOf("brightness" to brightness))
    }
    
    fun updateSubtitleLanguage(language: String?) {
        subtitleLanguage = language
        logPlaybackEvent(PlaybackEventType.SUBTITLE_CHANGE)
    }
    
    fun updateAudioTrack(track: String) {
        logPlaybackEvent(PlaybackEventType.AUDIO_TRACK_CHANGE, mapOf("track" to track))
    }
    
    fun cleanup() {
        scope.cancel()
    }
}