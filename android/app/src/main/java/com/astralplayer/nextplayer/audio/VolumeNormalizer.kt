package com.astralplayer.nextplayer.audio

import android.content.Context
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VolumeNormalizer @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var loudnessEnhancer: LoudnessEnhancer? = null
    private var targetLUFS = -23.0f // EBU R128 standard
    
    fun setupWithAudioSessionId(audioSessionId: Int) {
        try {
            loudnessEnhancer = LoudnessEnhancer(audioSessionId).apply {
                enabled = true
                setTargetGain(300) // 3dB boost
            }
        } catch (e: Exception) {
            // Handle audio effect initialization failure
        }
    }
    
    fun normalizeVolume(currentLUFS: Float): Float {
        return if (currentLUFS < targetLUFS) {
            // Boost quiet audio
            (targetLUFS - currentLUFS).coerceAtMost(6.0f)
        } else {
            // Reduce loud audio
            (targetLUFS - currentLUFS).coerceAtLeast(-6.0f)
        }
    }
    
    fun release() {
        loudnessEnhancer?.release()
        loudnessEnhancer = null
    }
}