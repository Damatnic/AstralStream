package com.astralplayer.nextplayer

import android.content.Context
import com.astralplayer.nextplayer.audio.*
import io.mockk.mockk
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

class AudioEnhancementTest {
    
    private lateinit var context: Context
    private lateinit var volumeNormalizer: VolumeNormalizer
    private lateinit var dialogEnhancer: DialogEnhancer
    private lateinit var audioPresetManager: AudioPresetManager
    
    @Before
    fun setup() {
        context = mockk(relaxed = true)
        volumeNormalizer = VolumeNormalizer(context)
        dialogEnhancer = DialogEnhancer(context)
        audioPresetManager = AudioPresetManager(context)
    }
    
    @Test
    fun `volume normalizer calculates correct adjustments`() {
        // Test quiet audio boost
        val quietAudio = -30.0f
        val boostAmount = volumeNormalizer.normalizeVolume(quietAudio)
        assertTrue("Quiet audio should be boosted", boostAmount > 0)
        assertTrue("Boost should be reasonable", boostAmount <= 6.0f)
        
        // Test loud audio reduction
        val loudAudio = -10.0f
        val reductionAmount = volumeNormalizer.normalizeVolume(loudAudio)
        assertTrue("Loud audio should be reduced", reductionAmount < 0)
        assertTrue("Reduction should be reasonable", reductionAmount >= -6.0f)
    }
    
    @Test
    fun `audio preset manager handles presets correctly`() {
        val availablePresets = audioPresetManager.getAvailablePresets()
        assertNotNull("Available presets should not be null", availablePresets)
        assertTrue("Should have multiple presets", availablePresets.size > 1)
        
        val initialPreset = audioPresetManager.getCurrentPreset()
        assertEquals("Initial preset should be Default", 
            AudioPresetManager.AudioPreset.Default, initialPreset)
        
        // Test setting different presets
        audioPresetManager.setPreset(AudioPresetManager.AudioPreset.Movie)
        assertEquals("Preset should be updated", 
            AudioPresetManager.AudioPreset.Movie, audioPresetManager.getCurrentPreset())
        
        audioPresetManager.setPreset(AudioPresetManager.AudioPreset.Night)
        assertEquals("Preset should be updated again", 
            AudioPresetManager.AudioPreset.Night, audioPresetManager.getCurrentPreset())
    }
    
    @Test
    fun `dialog enhancer can be toggled`() {
        // Test setup doesn't throw exceptions
        dialogEnhancer.setupWithAudioSessionId(1)
        
        // Test toggle functionality doesn't throw exceptions
        dialogEnhancer.setDialogEnhancement(true)
        dialogEnhancer.setDialogEnhancement(false)
        
        // Test cleanup doesn't throw exceptions
        dialogEnhancer.release()
        
        assertTrue("Dialog enhancer operations completed", true)
    }
    
    @Test
    fun `audio preset enum contains expected values`() {
        val presets = AudioPresetManager.AudioPreset.values()
        
        assertTrue("Should contain Default preset", 
            presets.contains(AudioPresetManager.AudioPreset.Default))
        assertTrue("Should contain Movie preset", 
            presets.contains(AudioPresetManager.AudioPreset.Movie))
        assertTrue("Should contain Night preset", 
            presets.contains(AudioPresetManager.AudioPreset.Night))
        assertTrue("Should contain Music preset", 
            presets.contains(AudioPresetManager.AudioPreset.Music))
        assertTrue("Should contain Podcast preset", 
            presets.contains(AudioPresetManager.AudioPreset.Podcast))
    }
    
    @Test
    fun `audio components handle cleanup correctly`() {
        // Setup components
        volumeNormalizer.setupWithAudioSessionId(1)
        dialogEnhancer.setupWithAudioSessionId(1)
        audioPresetManager.setupWithAudioSessionId(1)
        
        // Cleanup should not throw exceptions
        volumeNormalizer.release()
        dialogEnhancer.release()
        audioPresetManager.release()
        
        assertTrue("Audio cleanup completed successfully", true)
    }
}