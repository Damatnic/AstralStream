package com.astralplayer.nextplayer.audio

import android.content.Context
import android.media.audiofx.Equalizer
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AudioPresetManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var equalizer: Equalizer? = null
    private var currentPreset = AudioPreset.Default
    
    enum class AudioPreset {
        Default, Movie, Night, Music, Podcast
    }
    
    fun setupWithAudioSessionId(audioSessionId: Int) {
        try {
            equalizer = Equalizer(0, audioSessionId).apply { enabled = true }
        } catch (e: Exception) {
            // Handle failure
        }
    }
    
    fun setPreset(preset: AudioPreset) {
        currentPreset = preset
        equalizer?.let { eq ->
            val bands = eq.numberOfBands
            for (i in 0 until bands) {
                val gain = getGainForBand(preset, i, eq.getCenterFreq(i.toShort()))
                eq.setBandLevel(i.toShort(), gain.toShort())
            }
        }
    }
    
    private fun getGainForBand(preset: AudioPreset, band: Int, freq: Int): Int = when (preset) {
        AudioPreset.Movie -> when {
            freq < 500 -> 400  // Bass boost
            freq in 500..3000 -> 300  // Dialog clarity
            else -> 100
        }
        AudioPreset.Night -> when {
            freq < 200 -> -300  // Reduce bass
            freq in 1000..4000 -> 200  // Enhance dialog
            else -> -100
        }
        AudioPreset.Music -> when {
            freq < 200 -> 300
            freq > 8000 -> 200
            else -> 100
        }
        AudioPreset.Podcast -> when {
            freq in 300..3000 -> 400  // Speech focus
            else -> -200
        }
        else -> 0
    }
    
    fun getCurrentPreset() = currentPreset
    fun getAvailablePresets() = AudioPreset.values().toList()
    
    fun release() {
        equalizer?.release()
        equalizer = null
    }
}