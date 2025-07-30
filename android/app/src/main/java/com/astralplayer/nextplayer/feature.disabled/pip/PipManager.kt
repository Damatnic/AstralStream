package com.astralplayer.nextplayer.feature.pip

import android.app.Activity
import android.app.PictureInPictureParams
import android.content.pm.PackageManager
import android.os.Build
import android.util.Rational
import androidx.annotation.RequiresApi
import android.app.RemoteAction
import android.app.PendingIntent
import android.content.Intent
import android.graphics.drawable.Icon
import android.graphics.Rect
// import androidx.compose.material.icons.Icons
// import androidx.compose.material.icons.filled.*
// import androidx.compose.runtime.*
// import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.astralplayer.nextplayer.R

/**
 * Enhanced Picture-in-Picture manager with comprehensive PiP functionality
 * Includes state management, optimal configuration, and MX Player-like features
 */
class PipManager(private val activity: Activity) {
    
    companion object {
        const val ACTION_PLAY_PAUSE = "com.astralplayer.nextplayer.ACTION_PLAY_PAUSE"
        const val ACTION_SEEK_FORWARD = "com.astralplayer.nextplayer.ACTION_SEEK_FORWARD"
        const val ACTION_SEEK_BACKWARD = "com.astralplayer.nextplayer.ACTION_SEEK_BACKWARD"
        const val ACTION_NEXT = "com.astralplayer.nextplayer.ACTION_NEXT"
        const val ACTION_PREVIOUS = "com.astralplayer.nextplayer.ACTION_PREVIOUS"
    }
    
    // Enhanced state management
    private val _isInPiPMode = MutableStateFlow(false)
    val isInPiPMode: StateFlow<Boolean> = _isInPiPMode.asStateFlow()
    
    private val _isPiPSupported = MutableStateFlow(checkPiPSupport())
    val isPiPSupported: StateFlow<Boolean> = _isPiPSupported.asStateFlow()
    
    // Video dimensions for optimal aspect ratio
    private var videoWidth: Int = 16
    private var videoHeight: Int = 9
    
    // Configuration options
    data class PiPConfiguration(
        val autoEnterEnabled: Boolean = true,
        val seamlessResizeEnabled: Boolean = true,
        val showControlsInPiP: Boolean = true,
        val enableAdvancedControls: Boolean = false
    )
    
    private var config = PiPConfiguration()
    
    /**
     * Check if device supports Picture-in-Picture
     */
    private fun checkPiPSupport(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                activity.packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)
    }
    
    private val isPipSupported: Boolean
        get() = isPiPSupported.value
    
    @RequiresApi(Build.VERSION_CODES.O)
    fun enterPipMode(
        aspectRatio: Rational? = null,
        isPlaying: Boolean = true,
        sourceRectHint: Rect? = null
    ): Boolean {
        if (!isPipSupported) return false
        
        try {
            val optimalAspectRatio = aspectRatio ?: calculateOptimalAspectRatio()
            val builder = PictureInPictureParams.Builder()
                .setAspectRatio(optimalAspectRatio)
                .setExpandedAspectRatio(optimalAspectRatio)
            
            // Set source rect hint for smooth transition
            sourceRectHint?.let { builder.setSourceRectHint(it) }
            
            // Enhanced configuration for Android 12+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                builder.setAutoEnterEnabled(config.autoEnterEnabled)
                    .setSeamlessResizeEnabled(config.seamlessResizeEnabled)
            }
            
            // Add remote actions for PiP controls
            if (config.showControlsInPiP) {
                val actions = createPipActions(isPlaying)
                builder.setActions(actions)
            }
            
            val result = activity.enterPictureInPictureMode(builder.build())
            if (result) {
                _isInPiPMode.value = true
            }
            
            return result
        } catch (e: Exception) {
            android.util.Log.e("PipManager", "Failed to enter PiP mode", e)
            return false
        }
    }
    
    /**
     * Update video dimensions for optimal aspect ratio
     */
    fun updateVideoDimensions(width: Int, height: Int) {
        videoWidth = width
        videoHeight = height
        
        // Update PiP params if already in PiP mode
        if (isInPipMode() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            updatePipParams(calculateOptimalAspectRatio())
        }
    }
    
    /**
     * Calculate optimal aspect ratio for video dimensions
     */
    private fun calculateOptimalAspectRatio(): Rational {
        val width = if (videoWidth > 0) videoWidth else 16
        val height = if (videoHeight > 0) videoHeight else 9
        
        // Clamp aspect ratio to supported range
        val ratio = width.toFloat() / height.toFloat()
        val clampedRatio = ratio.coerceIn(0.42f, 2.39f)
        
        return Rational((clampedRatio * 100).toInt(), 100)
    }
    
    /**
     * Configure PiP behavior
     */
    fun updateConfiguration(newConfig: PiPConfiguration) {
        config = newConfig
    }
    
    @RequiresApi(Build.VERSION_CODES.O)
    fun updatePipParams(
        aspectRatio: Rational? = null,
        isPlaying: Boolean = true
    ) {
        if (!isPipSupported || !activity.isInPictureInPictureMode) return
        
        try {
            val builder = PictureInPictureParams.Builder()
            
            val ratio = aspectRatio ?: calculateOptimalAspectRatio()
            builder.setAspectRatio(ratio)
                .setExpandedAspectRatio(ratio)
            
            // Add enhanced configuration for Android 12+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                builder.setAutoEnterEnabled(config.autoEnterEnabled)
                    .setSeamlessResizeEnabled(config.seamlessResizeEnabled)
            }
            
            // Update remote actions
            if (config.showControlsInPiP) {
                val actions = createPipActions(isPlaying)
                builder.setActions(actions)
            }
            
            activity.setPictureInPictureParams(builder.build())
        } catch (e: Exception) {
            android.util.Log.e("PipManager", "Failed to update PiP params", e)
        }
    }
    
    @RequiresApi(Build.VERSION_CODES.O)
    private fun createPipActions(isPlaying: Boolean): List<RemoteAction> {
        val actions = mutableListOf<RemoteAction>()
        
        // Previous action
        actions.add(
            createRemoteAction(
                ACTION_PREVIOUS,
                R.drawable.ic_skip_previous,
                "Previous"
            )
        )
        
        // Seek backward action
        actions.add(
            createRemoteAction(
                ACTION_SEEK_BACKWARD,
                R.drawable.ic_replay_10,
                "Rewind 10s"
            )
        )
        
        // Play/Pause action
        actions.add(
            createRemoteAction(
                ACTION_PLAY_PAUSE,
                if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play,
                if (isPlaying) "Pause" else "Play"
            )
        )
        
        // Seek forward action
        actions.add(
            createRemoteAction(
                ACTION_SEEK_FORWARD,
                R.drawable.ic_forward_10,
                "Forward 10s"
            )
        )
        
        // Next action
        actions.add(
            createRemoteAction(
                ACTION_NEXT,
                R.drawable.ic_skip_next,
                "Next"
            )
        )
        
        return actions
    }
    
    @RequiresApi(Build.VERSION_CODES.O)
    private fun createRemoteAction(
        action: String,
        iconRes: Int,
        title: String
    ): RemoteAction {
        val intent = Intent(action).apply {
            setPackage(activity.packageName)
        }
        
        val pendingIntent = PendingIntent.getBroadcast(
            activity,
            action.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val icon = Icon.createWithResource(activity, iconRes)
        
        return RemoteAction(icon, title, title, pendingIntent)
    }
    
    fun isInPipMode(): Boolean {
        val actualPipMode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            activity.isInPictureInPictureMode
        } else {
            false
        }
        
        // Update state if it doesn't match
        if (_isInPiPMode.value != actualPipMode) {
            _isInPiPMode.value = actualPipMode
        }
        
        return actualPipMode
    }
    
    /**
     * Handle PiP mode state changes from activity lifecycle
     */
    fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Any? = null
    ) {
        _isInPiPMode.value = isInPictureInPictureMode
        
        android.util.Log.d("PipManager", "PiP mode changed: $isInPictureInPictureMode")
        
        // Handle configuration changes if available
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && newConfig != null) {
            // Handle PiP-specific configuration changes
        }
    }
    
    /**
     * Exit PiP mode programmatically
     */
    fun exitPiPMode() {
        if (isInPipMode()) {
            // Move task to front to exit PiP
            try {
                activity.moveTaskToBack(false)
                _isInPiPMode.value = false
            } catch (e: Exception) {
                android.util.Log.e("PipManager", "Failed to exit PiP mode", e)
            }
        }
    }
    
    /**
     * Check if can enter PiP mode now
     */
    fun canEnterPiPMode(): Boolean {
        return isPipSupported && 
               !isInPipMode() && 
               Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
    }
    
    /**
     * Get optimal PiP window size for screen dimensions
     */
    fun getOptimalPiPSize(): Pair<Int, Int> {
        val displayMetrics = activity.resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val screenHeight = displayMetrics.heightPixels
        
        // PiP window is typically 1/4 of screen size
        val pipWidth = (screenWidth * 0.25f).toInt()
        val aspectRatio = videoWidth.toFloat() / videoHeight.toFloat()
        val pipHeight = (pipWidth / aspectRatio).toInt()
        
        return Pair(pipWidth, pipHeight)
    }
    
    fun calculateAspectRatio(width: Int, height: Int): Rational? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && width > 0 && height > 0) {
            // Ensure aspect ratio is within PiP constraints (between 0.418410 and 2.390000)
            val ratio = width.toFloat() / height.toFloat()
            val adjustedRatio = when {
                ratio < 0.42f -> Rational(42, 100)
                ratio > 2.39f -> Rational(239, 100)
                else -> Rational(width, height)
            }
            
            android.util.Log.d("PipManager", "Video dimensions: ${width}x${height}, aspect ratio: $ratio, adjusted: $adjustedRatio")
            adjustedRatio
        } else {
            android.util.Log.d("PipManager", "Invalid dimensions or API level too low: ${width}x${height}")
            null
        }
    }
    
    @RequiresApi(Build.VERSION_CODES.O)
    fun updateAspectRatioFromVideo(width: Int, height: Int, isPlaying: Boolean = true) {
        if (isInPipMode()) {
            val aspectRatio = calculateAspectRatio(width, height)
            updatePipParams(aspectRatio, isPlaying)
        }
    }
}