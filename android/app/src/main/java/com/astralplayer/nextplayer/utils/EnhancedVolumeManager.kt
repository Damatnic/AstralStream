package com.astralplayer.nextplayer.utils

import android.content.Context
import android.media.AudioManager
import android.media.audiofx.LoudnessEnhancer
import android.util.Log
import androidx.media3.exoplayer.ExoPlayer

/**
 * Enhanced volume manager with LoudnessEnhancer support for 200% volume boost
 * Based on Next Player's implementation but enhanced for Astral Vu
 */
class EnhancedVolumeManager(
    private val context: Context,
    private val exoPlayer: ExoPlayer
) {
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    
    private var loudnessEnhancer: LoudnessEnhancer? = null
    private var isBoostEnabled: Boolean = false
    
    val currentStreamVolume: Int
        get() = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
    
    val maxStreamVolume: Int  
        get() = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
    
    var currentVolume: Float = currentStreamVolume.toFloat()
        private set
    
    // Maximum volume is 2x when boost is available
    val maxVolume: Int
        get() = maxStreamVolume * (if (loudnessEnhancer != null) 2 else 1)
    
    val currentLoudnessGain: Float
        get() = if (currentVolume > maxStreamVolume) {
            (currentVolume - maxStreamVolume) * (MAX_VOLUME_BOOST / maxStreamVolume)
        } else 0f
    
    val volumePercentage: Int
        get() = ((currentVolume / maxVolume.toFloat()) * 100).toInt()
    
    fun initializeLoudnessEnhancer() {
        try {
            val audioSessionId = exoPlayer.audioSessionId
            if (audioSessionId != 0) {
                loudnessEnhancer?.release()
                loudnessEnhancer = LoudnessEnhancer(audioSessionId).apply {
                    enabled = false
                }
                Log.d("VolumeManager", "LoudnessEnhancer initialized with session ID: $audioSessionId")
            } else {
                Log.w("VolumeManager", "Cannot initialize LoudnessEnhancer: invalid audio session ID")
            }
        } catch (e: Exception) {
            Log.e("VolumeManager", "Failed to initialize LoudnessEnhancer", e)
            loudnessEnhancer = null
        }
    }
    
    fun setVolume(volume: Float, showVolumePanel: Boolean = false) {
        currentVolume = volume.coerceIn(0f, maxVolume.toFloat())
        
        if (currentVolume <= maxStreamVolume) {
            // Normal volume range - disable loudness enhancer
            disableLoudnessEnhancer()
            setSystemVolume(currentVolume.toInt(), showVolumePanel)
        } else {
            // Boost range - enable loudness enhancer
            setSystemVolume(maxStreamVolume, showVolumePanel)
            enableLoudnessEnhancer()
        }
        
        Log.d("VolumeManager", "Volume set to: $currentVolume (max: $maxVolume)")
    }
    
    fun setVolumeBoost(enabled: Boolean) {
        isBoostEnabled = enabled
        if (!enabled) {
            // When disabling boost, ensure volume doesn't exceed system max
            if (currentVolume > maxStreamVolume) {
                setVolume(maxStreamVolume.toFloat())
            }
            disableLoudnessEnhancer()
        } else {
            // Re-initialize if needed when enabling boost
            if (loudnessEnhancer == null) {
                initializeLoudnessEnhancer()
            }
        }
        Log.d("VolumeManager", "Volume boost ${if (enabled) "enabled" else "disabled"}")
    }
    
    fun adjustVolume(delta: Float, showVolumePanel: Boolean = false) {
        val maxAllowed = if (isBoostEnabled) maxVolume.toFloat() else maxStreamVolume.toFloat()
        val newVolume = (currentVolume + delta).coerceIn(0f, maxAllowed)
        setVolume(newVolume, showVolumePanel)
    }
    
    fun increaseVolume(showVolumePanel: Boolean = false) {
        adjustVolume(1f, showVolumePanel)
    }
    
    fun decreaseVolume(showVolumePanel: Boolean = false) {
        adjustVolume(-1f, showVolumePanel)
    }
    
    private fun enableLoudnessEnhancer() {
        try {
            loudnessEnhancer?.let { enhancer ->
                enhancer.enabled = true
                enhancer.setTargetGain(currentLoudnessGain.toInt())
                Log.d("VolumeManager", "LoudnessEnhancer enabled with gain: ${currentLoudnessGain.toInt()}")
            }
        } catch (e: Exception) {
            Log.e("VolumeManager", "Failed to enable LoudnessEnhancer", e)
        }
    }
    
    private fun disableLoudnessEnhancer() {
        try {
            loudnessEnhancer?.enabled = false
            Log.d("VolumeManager", "LoudnessEnhancer disabled")
        } catch (e: Exception) {
            Log.e("VolumeManager", "Failed to disable LoudnessEnhancer", e)
        }
    }
    
    @Suppress("DEPRECATION")
    private fun setSystemVolume(volume: Int, showVolumePanel: Boolean) {
        try {
            val flags = if (showVolumePanel && audioManager.isWiredHeadsetOn) {
                AudioManager.FLAG_SHOW_UI
            } else 0
            
            audioManager.setStreamVolume(
                AudioManager.STREAM_MUSIC,
                volume,
                flags
            )
        } catch (e: Exception) {
            Log.e("VolumeManager", "Failed to set system volume", e)
        }
    }
    
    fun release() {
        try {
            loudnessEnhancer?.release()
            loudnessEnhancer = null
            Log.d("VolumeManager", "EnhancedVolumeManager released")
        } catch (e: Exception) {
            Log.e("VolumeManager", "Error releasing EnhancedVolumeManager", e)
        }
    }
    
    companion object {
        private const val MAX_VOLUME_BOOST = 2000f // 20dB boost
    }
}