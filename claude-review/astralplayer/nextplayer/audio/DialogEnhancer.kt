package com.astralplayer.nextplayer.audio

import android.content.Context
import android.media.audiofx.Equalizer
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DialogEnhancer @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var equalizer: Equalizer? = null
    
    fun setupWithAudioSessionId(audioSessionId: Int) {
        try {
            equalizer = Equalizer(0, audioSessionId).apply {
                enabled = true
                // Enhance speech frequencies (300Hz-3kHz)
                val bands = numberOfBands
                for (i in 0 until bands) {
                    val freq = getCenterFreq(i.toShort())
                    val gain = when {
                        freq in 300..3000 -> 300 // +3dB boost for speech
                        freq > 8000 -> -200 // -2dB reduction for high frequencies
                        else -> 0
                    }
                    setBandLevel(i.toShort(), gain.toShort())
                }
            }
        } catch (e: Exception) {
            // Handle audio effect failure
        }
    }
    
    fun setDialogEnhancement(enabled: Boolean) {
        equalizer?.enabled = enabled
    }
    
    fun release() {
        equalizer?.release()
        equalizer = null
    }
}