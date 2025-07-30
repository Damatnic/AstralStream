package com.astralplayer.nextplayer.audio

import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.PresetReverb
import android.media.audiofx.Virtualizer
import android.media.audiofx.LoudnessEnhancer
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Comprehensive Audio Equalizer Manager
 * Provides advanced audio effects and equalization capabilities
 */
class AudioEqualizerManager(private val audioSessionId: Int = 0) {
    
    // Audio effects instances
    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null
    private var presetReverb: PresetReverb? = null
    private var loudnessEnhancer: LoudnessEnhancer? = null
    
    // State management
    private val _isEnabled = MutableStateFlow(false)
    val isEnabled: StateFlow<Boolean> = _isEnabled.asStateFlow()
    
    private val _currentPreset = MutableStateFlow(EqualizerPreset.NORMAL)
    val currentPreset: StateFlow<EqualizerPreset> = _currentPreset.asStateFlow()
    
    private val _bandLevels = MutableStateFlow<List<BandLevel>>(emptyList())
    val bandLevels: StateFlow<List<BandLevel>> = _bandLevels.asStateFlow()
    
    private val _bassLevel = MutableStateFlow(0)
    val bassLevel: StateFlow<Int> = _bassLevel.asStateFlow()
    
    private val _virtualizerLevel = MutableStateFlow(0)
    val virtualizerLevel: StateFlow<Int> = _virtualizerLevel.asStateFlow()
    
    private val _reverbLevel = MutableStateFlow(0)
    val reverbLevel: StateFlow<Int> = _reverbLevel.asStateFlow()
    
    private val _loudnessLevel = MutableStateFlow(0)
    val loudnessLevel: StateFlow<Int> = _loudnessLevel.asStateFlow()
    
    // Custom presets storage
    private val customPresets = mutableStateListOf<CustomEqualizerPreset>()
    
    init {
        initializeAudioEffects()
    }
    
    /**
     * Initialize all audio effects
     */
    private fun initializeAudioEffects() {
        try {
            // Initialize Equalizer
            equalizer = Equalizer(0, audioSessionId).apply {
                enabled = false
                
                // Get band information
                val bands = mutableListOf<BandLevel>()
                for (i in 0 until numberOfBands) {
                    val centerFreq = getCenterFreq(i.toShort())
                    val range = bandLevelRange
                    bands.add(
                        BandLevel(
                            band = i.toShort(),
                            frequency = centerFreq / 1000, // Convert to kHz
                            minLevel = range[0].toInt(),
                            maxLevel = range[1].toInt(),
                            currentLevel = getBandLevel(i.toShort()).toInt()
                        )
                    )
                }
                _bandLevels.value = bands
            }
            
            // Initialize Bass Boost
            bassBoost = BassBoost(0, audioSessionId).apply {
                enabled = false
            }
            
            // Initialize Virtualizer
            virtualizer = Virtualizer(0, audioSessionId).apply {
                enabled = false
            }
            
            // Initialize Preset Reverb
            presetReverb = PresetReverb(0, audioSessionId).apply {
                enabled = false
                preset = PresetReverb.PRESET_NONE
            }
            
            // Initialize Loudness Enhancer
            loudnessEnhancer = LoudnessEnhancer(audioSessionId).apply {
                enabled = false
            }
            
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback to simulated values if audio effects are not available
            _bandLevels.value = listOf(
                BandLevel(0, 60, -1500, 1500, 0),
                BandLevel(1, 230, -1500, 1500, 0),
                BandLevel(2, 910, -1500, 1500, 0),
                BandLevel(3, 3600, -1500, 1500, 0),
                BandLevel(4, 14000, -1500, 1500, 0)
            )
        }
    }
    
    /**
     * Enable or disable all audio effects
     */
    fun setEnabled(enabled: Boolean) {
        _isEnabled.value = enabled
        
        equalizer?.enabled = enabled
        bassBoost?.enabled = enabled
        virtualizer?.enabled = enabled
        presetReverb?.enabled = enabled
        loudnessEnhancer?.enabled = enabled
    }
    
    /**
     * Apply a preset configuration
     */
    fun applyPreset(preset: EqualizerPreset) {
        _currentPreset.value = preset
        
        when (preset) {
            EqualizerPreset.NORMAL -> applyNormalPreset()
            EqualizerPreset.CLASSICAL -> applyClassicalPreset()
            EqualizerPreset.DANCE -> applyDancePreset()
            EqualizerPreset.FLAT -> applyFlatPreset()
            EqualizerPreset.FOLK -> applyFolkPreset()
            EqualizerPreset.HEAVY_METAL -> applyHeavyMetalPreset()
            EqualizerPreset.HIP_HOP -> applyHipHopPreset()
            EqualizerPreset.JAZZ -> applyJazzPreset()
            EqualizerPreset.POP -> applyPopPreset()
            EqualizerPreset.ROCK -> applyRockPreset()
            EqualizerPreset.MOVIE -> applyMoviePreset()
            EqualizerPreset.MUSIC -> applyMusicPreset()
            EqualizerPreset.VOICE -> applyVoicePreset()
            EqualizerPreset.CUSTOM -> {} // Do nothing, user will adjust manually
        }
    }
    
    /**
     * Set individual band level
     */
    fun setBandLevel(band: Short, level: Int) {
        equalizer?.setBandLevel(band, level.toShort())
        
        _bandLevels.value = _bandLevels.value.map { bandLevel ->
            if (bandLevel.band == band) {
                bandLevel.copy(currentLevel = level)
            } else {
                bandLevel
            }
        }
    }
    
    /**
     * Set bass boost level (0-1000)
     */
    fun setBassBoostLevel(level: Int) {
        val clampedLevel = level.coerceIn(0, 1000)
        _bassLevel.value = clampedLevel
        bassBoost?.setStrength(clampedLevel.toShort())
    }
    
    /**
     * Set virtualizer level (0-1000)
     */
    fun setVirtualizerLevel(level: Int) {
        val clampedLevel = level.coerceIn(0, 1000)
        _virtualizerLevel.value = clampedLevel
        virtualizer?.setStrength(clampedLevel.toShort())
    }
    
    /**
     * Set reverb preset
     */
    fun setReverbPreset(preset: ReverbPreset) {
        _reverbLevel.value = preset.value
        presetReverb?.preset = when (preset) {
            ReverbPreset.NONE -> PresetReverb.PRESET_NONE
            ReverbPreset.SMALL_ROOM -> PresetReverb.PRESET_SMALLROOM
            ReverbPreset.MEDIUM_ROOM -> PresetReverb.PRESET_MEDIUMROOM
            ReverbPreset.LARGE_ROOM -> PresetReverb.PRESET_LARGEROOM
            ReverbPreset.MEDIUM_HALL -> PresetReverb.PRESET_MEDIUMHALL
            ReverbPreset.LARGE_HALL -> PresetReverb.PRESET_LARGEHALL
            ReverbPreset.PLATE -> PresetReverb.PRESET_PLATE
        }
    }
    
    /**
     * Set loudness enhancer gain (0-5000 mB)
     */
    fun setLoudnessGain(gain: Int) {
        val clampedGain = gain.coerceIn(0, 5000)
        _loudnessLevel.value = clampedGain
        loudnessEnhancer?.setTargetGain(clampedGain)
    }
    
    /**
     * Save current settings as custom preset
     */
    fun saveCustomPreset(name: String) {
        val bandSettings = _bandLevels.value.map { it.currentLevel }
        
        val preset = CustomEqualizerPreset(
            name = name,
            bandLevels = bandSettings,
            bassLevel = _bassLevel.value,
            virtualizerLevel = _virtualizerLevel.value,
            reverbLevel = _reverbLevel.value,
            loudnessLevel = _loudnessLevel.value
        )
        
        customPresets.add(preset)
    }
    
    /**
     * Load custom preset
     */
    fun loadCustomPreset(preset: CustomEqualizerPreset) {
        // Apply band levels
        preset.bandLevels.forEachIndexed { index, level ->
            setBandLevel(index.toShort(), level)
        }
        
        // Apply effects
        setBassBoostLevel(preset.bassLevel)
        setVirtualizerLevel(preset.virtualizerLevel)
        setLoudnessGain(preset.loudnessLevel)
        
        _currentPreset.value = EqualizerPreset.CUSTOM
    }
    
    /**
     * Get all custom presets
     */
    fun getCustomPresets(): List<CustomEqualizerPreset> {
        return customPresets.toList()
    }
    
    /**
     * Delete custom preset
     */
    fun deleteCustomPreset(preset: CustomEqualizerPreset) {
        customPresets.remove(preset)
    }
    
    // Preset implementations
    
    private fun applyNormalPreset() {
        equalizer?.usePreset(0)
        setBassBoostLevel(0)
        setVirtualizerLevel(0)
        setReverbPreset(ReverbPreset.NONE)
        setLoudnessGain(0)
    }
    
    private fun applyClassicalPreset() {
        val levels = listOf(500, 300, 0, 200, 400)
        applyBandLevels(levels)
        setBassBoostLevel(0)
        setVirtualizerLevel(200)
        setReverbPreset(ReverbPreset.MEDIUM_HALL)
    }
    
    private fun applyDancePreset() {
        val levels = listOf(600, 0, 200, 400, 500)
        applyBandLevels(levels)
        setBassBoostLevel(600)
        setVirtualizerLevel(0)
        setReverbPreset(ReverbPreset.NONE)
    }
    
    private fun applyFlatPreset() {
        val levels = listOf(0, 0, 0, 0, 0)
        applyBandLevels(levels)
        setBassBoostLevel(0)
        setVirtualizerLevel(0)
        setReverbPreset(ReverbPreset.NONE)
    }
    
    private fun applyFolkPreset() {
        val levels = listOf(300, 0, 0, 200, -100)
        applyBandLevels(levels)
        setBassBoostLevel(150)
        setVirtualizerLevel(0)
        setReverbPreset(ReverbPreset.SMALL_ROOM)
    }
    
    private fun applyHeavyMetalPreset() {
        val levels = listOf(400, 100, -200, 100, 600)
        applyBandLevels(levels)
        setBassBoostLevel(500)
        setVirtualizerLevel(100)
        setReverbPreset(ReverbPreset.NONE)
    }
    
    private fun applyHipHopPreset() {
        val levels = listOf(500, 300, 0, -100, 300)
        applyBandLevels(levels)
        setBassBoostLevel(700)
        setVirtualizerLevel(0)
        setReverbPreset(ReverbPreset.NONE)
    }
    
    private fun applyJazzPreset() {
        val levels = listOf(400, 200, -200, 200, 500)
        applyBandLevels(levels)
        setBassBoostLevel(200)
        setVirtualizerLevel(300)
        setReverbPreset(ReverbPreset.MEDIUM_ROOM)
    }
    
    private fun applyPopPreset() {
        val levels = listOf(-100, 200, 500, 100, -200)
        applyBandLevels(levels)
        setBassBoostLevel(300)
        setVirtualizerLevel(200)
        setReverbPreset(ReverbPreset.NONE)
    }
    
    private fun applyRockPreset() {
        val levels = listOf(500, 300, -200, 300, 500)
        applyBandLevels(levels)
        setBassBoostLevel(400)
        setVirtualizerLevel(200)
        setReverbPreset(ReverbPreset.NONE)
    }
    
    private fun applyMoviePreset() {
        val levels = listOf(300, 200, 100, 200, 300)
        applyBandLevels(levels)
        setBassBoostLevel(400)
        setVirtualizerLevel(500)
        setReverbPreset(ReverbPreset.LARGE_ROOM)
        setLoudnessGain(1000)
    }
    
    private fun applyMusicPreset() {
        val levels = listOf(200, 100, 0, 100, 200)
        applyBandLevels(levels)
        setBassBoostLevel(200)
        setVirtualizerLevel(300)
        setReverbPreset(ReverbPreset.NONE)
    }
    
    private fun applyVoicePreset() {
        val levels = listOf(-500, -200, 200, 300, -300)
        applyBandLevels(levels)
        setBassBoostLevel(0)
        setVirtualizerLevel(0)
        setReverbPreset(ReverbPreset.NONE)
        setLoudnessGain(500)
    }
    
    private fun applyBandLevels(levels: List<Int>) {
        levels.forEachIndexed { index, level ->
            if (index < (_bandLevels.value.size)) {
                setBandLevel(index.toShort(), level)
            }
        }
    }
    
    /**
     * Release all audio effects
     */
    fun release() {
        equalizer?.release()
        bassBoost?.release()
        virtualizer?.release()
        presetReverb?.release()
        loudnessEnhancer?.release()
        
        equalizer = null
        bassBoost = null
        virtualizer = null
        presetReverb = null
        loudnessEnhancer = null
    }
}

// Data classes

data class BandLevel(
    val band: Short,
    val frequency: Int, // in kHz
    val minLevel: Int,
    val maxLevel: Int,
    val currentLevel: Int
)

data class CustomEqualizerPreset(
    val name: String,
    val bandLevels: List<Int>,
    val bassLevel: Int,
    val virtualizerLevel: Int,
    val reverbLevel: Int,
    val loudnessLevel: Int
)

enum class EqualizerPreset(val displayName: String) {
    NORMAL("Normal"),
    CLASSICAL("Classical"),
    DANCE("Dance"),
    FLAT("Flat"),
    FOLK("Folk"),
    HEAVY_METAL("Heavy Metal"),
    HIP_HOP("Hip Hop"),
    JAZZ("Jazz"),
    POP("Pop"),
    ROCK("Rock"),
    MOVIE("Movie"),
    MUSIC("Music"),
    VOICE("Voice"),
    CUSTOM("Custom")
}

enum class ReverbPreset(val displayName: String, val value: Int) {
    NONE("None", 0),
    SMALL_ROOM("Small Room", 1),
    MEDIUM_ROOM("Medium Room", 2),
    LARGE_ROOM("Large Room", 3),
    MEDIUM_HALL("Medium Hall", 4),
    LARGE_HALL("Large Hall", 5),
    PLATE("Plate", 6)
}