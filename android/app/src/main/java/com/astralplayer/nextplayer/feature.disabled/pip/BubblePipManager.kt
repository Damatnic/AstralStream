package com.astralplayer.nextplayer.feature.pip

import android.app.Activity
import android.app.PendingIntent
import android.app.PictureInPictureParams
import android.app.RemoteAction
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Rect
import android.graphics.drawable.Icon
import android.os.Build
import android.util.Rational
import androidx.annotation.RequiresApi
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.toAndroidRect
import androidx.compose.ui.unit.IntRect
import androidx.media3.common.Player
import com.astralplayer.nextplayer.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Enhanced Picture-in-Picture Manager with Bubble UI Controls
 * Provides floating video playback with modern controls
 */
class BubblePipManager(
    private val activity: Activity,
    private val player: Player? = null
) {
    companion object {
        // PiP Actions
        const val ACTION_PLAY_PAUSE = "com.astralplayer.nextplayer.pip.PLAY_PAUSE"
        const val ACTION_NEXT = "com.astralplayer.nextplayer.pip.NEXT"
        const val ACTION_PREVIOUS = "com.astralplayer.nextplayer.pip.PREVIOUS"
        const val ACTION_SEEK_FORWARD = "com.astralplayer.nextplayer.pip.SEEK_FORWARD"
        const val ACTION_SEEK_BACKWARD = "com.astralplayer.nextplayer.pip.SEEK_BACKWARD"
        const val ACTION_CLOSE = "com.astralplayer.nextplayer.pip.CLOSE"
        
        // PiP Aspect Ratios
        val RATIO_16_9 = Rational(16, 9)
        val RATIO_4_3 = Rational(4, 3)
        val RATIO_1_1 = Rational(1, 1)
        val RATIO_21_9 = Rational(21, 9)
        
        // Request Codes
        private const val REQUEST_PLAY_PAUSE = 1
        private const val REQUEST_NEXT = 2
        private const val REQUEST_PREVIOUS = 3
        private const val REQUEST_SEEK_FORWARD = 4
        private const val REQUEST_SEEK_BACKWARD = 5
        private const val REQUEST_CLOSE = 6
    }
    
    // State management
    private val _isPipMode = MutableStateFlow(false)
    val isPipMode: StateFlow<Boolean> = _isPipMode.asStateFlow()
    
    private val _pipConfiguration = MutableStateFlow(PipConfiguration())
    val pipConfiguration: StateFlow<PipConfiguration> = _pipConfiguration.asStateFlow()
    
    // Broadcast receiver for PiP actions
    private val pipActionReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                ACTION_PLAY_PAUSE -> handlePlayPause()
                ACTION_NEXT -> handleNext()
                ACTION_PREVIOUS -> handlePrevious()
                ACTION_SEEK_FORWARD -> handleSeekForward()
                ACTION_SEEK_BACKWARD -> handleSeekBackward()
                ACTION_CLOSE -> handleClose()
            }
        }
    }
    
    init {
        // Register broadcast receiver
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val filter = IntentFilter().apply {
                addAction(ACTION_PLAY_PAUSE)
                addAction(ACTION_NEXT)
                addAction(ACTION_PREVIOUS)
                addAction(ACTION_SEEK_FORWARD)
                addAction(ACTION_SEEK_BACKWARD)
                addAction(ACTION_CLOSE)
            }
            activity.registerReceiver(pipActionReceiver, filter)
        }
    }
    
    /**
     * Enter Picture-in-Picture mode with bubble controls
     */
    @RequiresApi(Build.VERSION_CODES.O)
    fun enterPipMode(
        aspectRatio: Rational = RATIO_16_9,
        sourceRect: IntRect? = null,
        autoEnterEnabled: Boolean = true
    ): Boolean {
        if (!activity.packageManager.hasSystemFeature(android.content.pm.PackageManager.FEATURE_PICTURE_IN_PICTURE)) {
            return false
        }
        
        try {
            val params = createPipParams(aspectRatio, sourceRect, autoEnterEnabled)
            val entered = activity.enterPictureInPictureMode(params)
            
            if (entered) {
                _isPipMode.value = true
                updatePipActions()
            }
            
            return entered
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }
    
    /**
     * Update PiP parameters while in PiP mode
     */
    @RequiresApi(Build.VERSION_CODES.O)
    fun updatePipParams(
        aspectRatio: Rational? = null,
        actions: List<RemoteAction>? = null
    ) {
        if (!_isPipMode.value) return
        
        val currentConfig = _pipConfiguration.value
        val newAspectRatio = aspectRatio ?: currentConfig.aspectRatio
        val newActions = actions ?: createRemoteActions()
        
        val params = PictureInPictureParams.Builder()
            .setAspectRatio(newAspectRatio)
            .setActions(newActions)
            .build()
            
        activity.setPictureInPictureParams(params)
        
        _pipConfiguration.value = currentConfig.copy(
            aspectRatio = newAspectRatio,
            actions = newActions
        )
    }
    
    /**
     * Exit PiP mode and return to fullscreen
     */
    fun exitPipMode() {
        _isPipMode.value = false
        // The activity will automatically exit PiP when user taps on it
    }
    
    /**
     * Create PiP parameters with bubble UI actions
     */
    @RequiresApi(Build.VERSION_CODES.O)
    private fun createPipParams(
        aspectRatio: Rational,
        sourceRect: IntRect?,
        autoEnterEnabled: Boolean
    ): PictureInPictureParams {
        val builder = PictureInPictureParams.Builder()
            .setAspectRatio(aspectRatio)
            .setActions(createRemoteActions())
        
        // Set source rectangle hint for smooth transition
        sourceRect?.let {
            builder.setSourceRectHint(it.toAndroidRect())
        }
        
        // Auto-enter PiP on back press (Android 12+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            builder.setAutoEnterEnabled(autoEnterEnabled)
            builder.setSeamlessResizeEnabled(true)
        }
        
        return builder.build()
    }
    
    /**
     * Create remote actions for PiP controls with bubble design
     */
    @RequiresApi(Build.VERSION_CODES.O)
    private fun createRemoteActions(): List<RemoteAction> {
        val actions = mutableListOf<RemoteAction>()
        
        // Previous action
        if (player?.hasPrevious() == true) {
            actions.add(
                createRemoteAction(
                    iconResId = R.drawable.ic_skip_previous,
                    title = "Previous",
                    requestCode = REQUEST_PREVIOUS,
                    action = ACTION_PREVIOUS
                )
            )
        }
        
        // Seek backward action
        actions.add(
            createRemoteAction(
                iconResId = R.drawable.ic_replay_10,
                title = "Rewind 10s",
                requestCode = REQUEST_SEEK_BACKWARD,
                action = ACTION_SEEK_BACKWARD
            )
        )
        
        // Play/Pause action
        val isPlaying = player?.isPlaying ?: false
        actions.add(
            createRemoteAction(
                iconResId = if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play,
                title = if (isPlaying) "Pause" else "Play",
                requestCode = REQUEST_PLAY_PAUSE,
                action = ACTION_PLAY_PAUSE
            )
        )
        
        // Seek forward action
        actions.add(
            createRemoteAction(
                iconResId = R.drawable.ic_forward_10,
                title = "Forward 10s",
                requestCode = REQUEST_SEEK_FORWARD,
                action = ACTION_SEEK_FORWARD
            )
        )
        
        // Next action
        if (player?.hasNext() == true) {
            actions.add(
                createRemoteAction(
                    iconResId = R.drawable.ic_skip_next,
                    title = "Next",
                    requestCode = REQUEST_NEXT,
                    action = ACTION_NEXT
                )
            )
        }
        
        return actions
    }
    
    /**
     * Create a single remote action
     */
    @RequiresApi(Build.VERSION_CODES.O)
    private fun createRemoteAction(
        iconResId: Int,
        title: String,
        requestCode: Int,
        action: String
    ): RemoteAction {
        val intent = Intent(action).apply {
            setPackage(activity.packageName)
        }
        
        val pendingIntent = PendingIntent.getBroadcast(
            activity,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val icon = Icon.createWithResource(activity, iconResId)
        
        return RemoteAction(icon, title, title, pendingIntent)
    }
    
    /**
     * Update PiP actions based on current playback state
     */
    @RequiresApi(Build.VERSION_CODES.O)
    fun updatePipActions() {
        if (!_isPipMode.value) return
        
        updatePipParams(actions = createRemoteActions())
    }
    
    // Action handlers
    private fun handlePlayPause() {
        player?.let {
            if (it.isPlaying) {
                it.pause()
            } else {
                it.play()
            }
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                updatePipActions()
            }
        }
    }
    
    private fun handleNext() {
        player?.seekToNext()
    }
    
    private fun handlePrevious() {
        player?.seekToPrevious()
    }
    
    private fun handleSeekForward() {
        player?.let {
            val newPosition = it.currentPosition + 10000 // 10 seconds
            it.seekTo(newPosition.coerceAtMost(it.duration))
        }
    }
    
    private fun handleSeekBackward() {
        player?.let {
            val newPosition = it.currentPosition - 10000 // 10 seconds
            it.seekTo(newPosition.coerceAtLeast(0))
        }
    }
    
    private fun handleClose() {
        activity.finish()
    }
    
    /**
     * Handle PiP mode changes
     */
    fun onPictureInPictureModeChanged(isInPipMode: Boolean) {
        _isPipMode.value = isInPipMode
        
        if (!isInPipMode) {
            // Returned from PiP to fullscreen
            _pipConfiguration.value = PipConfiguration()
        }
    }
    
    /**
     * Cleanup resources
     */
    fun release() {
        try {
            activity.unregisterReceiver(pipActionReceiver)
        } catch (e: Exception) {
            // Already unregistered
        }
    }
    
    /**
     * Check if PiP is supported
     */
    fun isPipSupported(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                activity.packageManager.hasSystemFeature(android.content.pm.PackageManager.FEATURE_PICTURE_IN_PICTURE)
    }
    
    /**
     * Get optimal aspect ratio based on video dimensions
     */
    fun getOptimalAspectRatio(videoWidth: Int, videoHeight: Int): Rational {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return Rational(16, 9)
        }
        
        val ratio = videoWidth.toFloat() / videoHeight.toFloat()
        
        return when {
            ratio >= 2.3f -> Rational(21, 9)  // Ultra-wide
            ratio >= 1.7f -> Rational(16, 9)  // Standard widescreen
            ratio >= 1.2f -> Rational(4, 3)   // Traditional TV
            else -> Rational(1, 1)             // Square
        }
    }
}

/**
 * PiP Configuration data class
 */
data class PipConfiguration(
    val aspectRatio: Rational = BubblePipManager.RATIO_16_9,
    val actions: List<RemoteAction> = emptyList(),
    val autoEnterEnabled: Boolean = true,
    val seamlessResizeEnabled: Boolean = true
)