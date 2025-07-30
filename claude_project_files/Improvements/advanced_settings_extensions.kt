package com.astralplayer.nextplayer.data.repository

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * Extended settings repository with adult content and advanced codec settings
 */
interface AdvancedSettingsRepository : SettingsRepository {
    
    // Adult Content Settings
    fun getAdultContentModeEnabled(): Flow<Boolean>
    suspend fun setAdultContentModeEnabled(enabled: Boolean)
    
    fun getEnhancedCodecSupport(): Flow<Boolean>
    suspend fun setEnhancedCodecSupport(enabled: Boolean)
    
    fun getDtsDecodingEnabled(): Flow<Boolean>
    suspend fun setDtsDecodingEnabled(enabled: Boolean)
    
    fun getAc3DecodingEnabled(): Flow<Boolean>
    suspend fun setAc3DecodingEnabled(enabled: Boolean)
    
    fun getAdvancedVideoProcessing(): Flow<Boolean>
    suspend fun setAdvancedVideoProcessing(enabled: Boolean)
    
    // Network Settings
    fun getNetworkBuffering(): Flow<Boolean>
    suspend fun setNetworkBuffering(enabled: Boolean)
    
    fun getAdaptiveStreaming(): Flow<Boolean>
    suspend fun setAdaptiveStreaming(enabled: Boolean)
    
    fun getMaxBandwidth(): Flow<Int>
    suspend fun setMaxBandwidth(bandwidth: Int)
    
    fun getBufferSize(): Flow<Int>
    suspend fun setBufferSize(sizeMs: Int)
    
    // Video Enhancement Settings
    fun getVideoUpscaling(): Flow<Boolean>
    suspend fun setVideoUpscaling(enabled: Boolean)
    
    fun getColorEnhancement(): Flow<Boolean> 
    suspend fun setColorEnhancement(enabled: Boolean)
    
    fun getNoiseReduction(): Flow<Boolean>
    suspend fun setNoiseReduction(enabled: Boolean)
    
    fun getSharpening(): Flow<Float>
    suspend fun setSharpening(level: Float)
    
    // Audio Enhancement Settings
    fun getAudioNormalization(): Flow<Boolean>
    suspend fun setAudioNormalization(enabled: Boolean)
    
    fun getBassBoost(): Flow<Float>
    suspend fun setBassBoost(level: Float)
    
    fun getTrebleBoost(): Flow<Float>
    suspend fun setTrebleBoost(level: Float)
    
    fun getVirtualSurround(): Flow<Boolean>
    suspend fun setVirtualSurround(enabled: Boolean)
    
    // Privacy and Security
    fun getPrivateMode(): Flow<Boolean>
    suspend fun setPrivateMode(enabled: Boolean)
    
    fun getSecurePlayback(): Flow<Boolean>
    suspend fun setSecurePlayback(enabled: Boolean)
    
    fun getScreenRecordingBlock(): Flow<Boolean>
    suspend fun setScreenRecordingBlock(enabled: Boolean)
    
    // Performance Settings
    fun getGpuAcceleration(): Flow<Boolean>
    suspend fun setGpuAcceleration(enabled: Boolean)
    
    fun getMultiThreadDecoding(): Flow<Boolean>
    suspend fun setMultiThreadDecoding(enabled: Boolean)
    
    fun getMemoryOptimization(): Flow<Boolean>
    suspend fun setMemoryOptimization(enabled: Boolean)
    
    fun getBatteryOptimization(): Flow<Boolean>
    suspend fun setBatteryOptimization(enabled: Boolean)
    
    // Clear sensitive data
    suspend fun clearSensitiveData()
    suspend fun clearVideoSpeedMemory()
}

class AdvancedSettingsRepositoryImpl(
    private val context: Context,
    private val baseRepository: SettingsRepository
) : AdvancedSettingsRepository, SettingsRepository by baseRepository {
    
    companion object {
        // Adult Content Settings Keys
        private val ADULT_CONTENT_MODE = booleanPreferencesKey("adult_content_mode")
        private val ENHANCED_CODEC_SUPPORT = booleanPreferencesKey("enhanced_codec_support")
        private val DTS_DECODING = booleanPreferencesKey("dts_decoding")
        private val AC3_DECODING = booleanPreferencesKey("ac3_decoding")
        private val ADVANCED_VIDEO_PROCESSING = booleanPreferencesKey("advanced_video_processing")
        
        // Network Settings Keys
        private val NETWORK_BUFFERING = booleanPreferencesKey("network_buffering")
        private val ADAPTIVE_STREAMING = booleanPreferencesKey("adaptive_streaming")
        private val MAX_BANDWIDTH = intPreferencesKey("max_bandwidth")
        private val BUFFER_SIZE = intPreferencesKey("buffer_size")
        
        // Video Enhancement Keys
        private val VIDEO_UPSCALING = booleanPreferencesKey("video_upscaling")
        private val COLOR_ENHANCEMENT = booleanPreferencesKey("color_enhancement")
        private val NOISE_REDUCTION = booleanPreferencesKey("noise_reduction")
        private val SHARPENING = floatPreferencesKey("sharpening")
        
        // Audio Enhancement Keys
        private val AUDIO_NORMALIZATION = booleanPreferencesKey("audio_normalization")
        private val BASS_BOOST = floatPreferencesKey("bass_boost")
        private val TREBLE_BOOST = floatPreferencesKey("treble_boost")
        private val VIRTUAL_SURROUND = booleanPreferencesKey("virtual_surround")
        
        // Privacy Keys
        private val PRIVATE_MODE = booleanPreferencesKey("private_mode")
        private val SECURE_PLAYBACK = booleanPreferencesKey("secure_playback")
        private val SCREEN_RECORDING_BLOCK = booleanPreferencesKey("screen_recording_block")
        
        // Performance Keys
        private val GPU_ACCELERATION = booleanPreferencesKey("gpu_acceleration")
        private val MULTI_THREAD_DECODING = booleanPreferencesKey("multi_thread_decoding")
        private val MEMORY_OPTIMIZATION = booleanPreferencesKey("memory_optimization")
        private val BATTERY_OPTIMIZATION = booleanPreferencesKey("battery_optimization")
    }
    
    private val Context.advancedDataStore by androidx.datastore.preferences.preferencesDataStore("advanced_settings")
    
    // Adult Content Settings
    override fun getAdultContentModeEnabled(): Flow<Boolean> =
        context.advancedDataStore.data.map { it[ADULT_CONTENT_MODE] ?: false }
    
    override suspend fun setAdultContentModeEnabled(enabled: Boolean) {
        context.advancedDataStore.edit { it[ADULT_CONTENT_MODE] = enabled }
    }
    
    override fun getEnhancedCodecSupport(): Flow<Boolean> =
        context.advancedDataStore.data.map { it[ENHANCED_CODEC_SUPPORT] ?: true }
    
    override suspend fun setEnhancedCodecSupport(enabled: Boolean) {
        context.advancedDataStore.edit { it[ENHANCED_CODEC_SUPPORT] = enabled }
    }
    
    override fun getDtsDecodingEnabled(): Flow<Boolean> =
        context.advancedDataStore.data.map { it[DTS_DECODING] ?: true }
    
    override suspend fun setDtsDecodingEnabled(enabled: Boolean) {
        context.advancedDataStore.edit { it[DTS_DECODING] = enabled }
    }
    
    override fun getAc3DecodingEnabled(): Flow<Boolean> =
        context.advancedDataStore.data.map { it[AC3_DECODING] ?: true }
    
    override suspend fun setAc3DecodingEnabled(enabled: Boolean) {
        context.advancedDataStore.edit { it[AC3_DECODING] = enabled }
    }
    
    override fun getAdvancedVideoProcessing(): Flow<Boolean> =
        context.advancedDataStore.data.map { it[ADVANCED_VIDEO_PROCESSING] ?: false }
    
    override suspend fun setAdvancedVideoProcessing(enabled: Boolean) {
        context.advancedDataStore.edit { it[ADVANCED_VIDEO_PROCESSING] = enabled }
    }
    
    // Network Settings
    override fun getNetworkBuffering(): Flow<Boolean> =
        context.advancedDataStore.data.map { it[NETWORK_BUFFERING] ?: true }
    
    override suspend fun setNetworkBuffering(enabled: Boolean) {
        context.advancedDataStore.edit { it[NETWORK_BUFFERING] = enabled }
    }
    
    override fun getAdaptiveStreaming(): Flow<Boolean> =
        context.advancedDataStore.data.map { it[ADAPTIVE_STREAMING] ?: true }
    
    override suspend fun setAdaptiveStreaming(enabled: Boolean) {
        context.advancedDataStore.edit { it[ADAPTIVE_STREAMING] = enabled }
    }
    
    override fun getMaxBandwidth(): Flow<Int> =
        context.advancedDataStore.data.map { it[MAX_BANDWIDTH] ?: -1 } // -1 = unlimited
    
    override suspend fun setMaxBandwidth(bandwidth: Int) {
        context.advancedDataStore.edit { it[MAX_BANDWIDTH] = bandwidth }
    }
    
    override fun getBufferSize(): Flow<Int> =
        context.advancedDataStore.data.map { it[BUFFER_SIZE] ?: 50000 } // 50 seconds default
    
    override suspend fun setBufferSize(sizeMs: Int) {
        context.advancedDataStore.edit { it[BUFFER_SIZE] = sizeMs }
    }
    
    // Video Enhancement Settings
    override fun getVideoUpscaling(): Flow<Boolean> =
        context.advancedDataStore.data.map { it[VIDEO_UPSCALING] ?: false }
    
    override suspend fun setVideoUpscaling(enabled: Boolean) {
        context.advancedDataStore.edit { it[VIDEO_UPSCALING] = enabled }
    }
    
    override fun getColorEnhancement(): Flow<Boolean> =
        context.advancedDataStore.data.map { it[COLOR_ENHANCEMENT] ?: false }
    
    override suspend fun setColorEnhancement(enabled: Boolean) {
        context.advancedDataStore.edit { it[COLOR_ENHANCEMENT] = enabled }
    }
    
    override fun getNoiseReduction(): Flow<Boolean> =
        context.advancedDataStore.data.map { it[NOISE_REDUCTION] ?: false }
    
    override suspend fun setNoiseReduction(enabled: Boolean) {
        context.advancedDataStore.edit { it[NOISE_REDUCTION] = enabled }
    }
    
    override fun getSharpening(): Flow<Float> =
        context.advancedDataStore.data.map { it[SHARPENING] ?: 0.0f }
    
    override suspend fun setSharpening(level: Float) {
        context.advancedDataStore.edit { it[SHARPENING] = level.coerceIn(0f, 1f) }
    }
    
    // Audio Enhancement Settings
    override fun getAudioNormalization(): Flow<Boolean> =
        context.advancedDataStore.data.map { it[AUDIO_NORMALIZATION] ?: false }
    
    override suspend fun setAudioNormalization(enabled: Boolean) {
        context.advancedDataStore.edit { it[AUDIO_NORMALIZATION] = enabled }
    }
    
    override fun getBassBoost(): Flow<Float> =
        context.advancedDataStore.data.map { it[BASS_BOOST] ?: 0.0f }
    
    override suspend fun setBassBoost(level: Float) {
        context.advancedDataStore.edit { it[BASS_BOOST] = level.coerceIn(0f, 1f) }
    }
    
    override fun getTrebleBoost(): Flow<Float> =
        context.advancedDataStore.data.map { it[TREBLE_BOOST] ?: 0.0f }
    
    override suspend fun setTrebleBoost(level: Float) {
        context.advancedDataStore.edit { it[TREBLE_BOOST] = level.coerceIn(0f, 1f) }
    }
    
    override fun getVirtualSurround(): Flow<Boolean> =
        context.advancedDataStore.data.map { it[VIRTUAL_SURROUND] ?: false }
    
    override suspend fun setVirtualSurround(enabled: Boolean) {
        context.advancedDataStore.edit { it[VIRTUAL_SURROUND] = enabled }
    }
    
    // Privacy Settings
    override fun getPrivateMode(): Flow<Boolean> =
        context.advancedDataStore.data.map { it[PRIVATE_MODE] ?: false }
    
    override suspend fun setPrivateMode(enabled: Boolean) {
        context.advancedDataStore.edit { it[PRIVATE_MODE] = enabled }
    }
    
    override fun getSecurePlayback(): Flow<Boolean> =
        context.advancedDataStore.data.map { it[SECURE_PLAYBACK] ?: false }
    
    override suspend fun setSecurePlayback(enabled: Boolean) {
        context.advancedDataStore.edit { it[SECURE_PLAYBACK] = enabled }
    }
    
    override fun getScreenRecordingBlock(): Flow<Boolean> =
        context.advancedDataStore.data.map { it[SCREEN_RECORDING_BLOCK] ?: false }
    
    override suspend fun setScreenRecordingBlock(enabled: Boolean) {
        context.advancedDataStore.edit { it[SCREEN_RECORDING_BLOCK] = enabled }
    }
    
    // Performance Settings
    override fun getGpuAcceleration(): Flow<Boolean> =
        context.advancedDataStore.data.map { it[GPU_ACCELERATION] ?: true }
    
    override suspend fun setGpuAcceleration(enabled: Boolean) {
        context.advancedDataStore.edit { it[GPU_ACCELERATION] = enabled }
    }
    
    override fun getMultiThreadDecoding(): Flow<Boolean> =
        context.advancedDataStore.data.map { it[MULTI_THREAD_DECODING] ?: true }
    
    override suspend fun setMultiThreadDecoding(enabled: Boolean) {
        context.advancedDataStore.edit { it[MULTI_THREAD_DECODING] = enabled }
    }
    
    override fun getMemoryOptimization(): Flow<Boolean> =
        context.advancedDataStore.data.map { it[MEMORY_OPTIMIZATION] ?: true }
    
    override suspend fun setMemoryOptimization(enabled: Boolean) {
        context.advancedDataStore.edit { it[MEMORY_OPTIMIZATION] = enabled }
    }
    
    override fun getBatteryOptimization(): Flow<Boolean> =
        context.advancedDataStore.data.map { it[BATTERY_OPTIMIZATION] ?: false }
    
    override suspend fun setBatteryOptimization(enabled: Boolean) {
        context.advancedDataStore.edit { it[BATTERY_OPTIMIZATION] = enabled }
    }
    
    // Data management
    override suspend fun clearSensitiveData() {
        context.advancedDataStore.edit { preferences ->
            preferences.remove(PRIVATE_MODE)
            preferences.remove(SECURE_PLAYBACK)
            // Clear any other sensitive preferences
        }
        
        // Also clear base repository sensitive data
        clearAllData()
    }
    
    override suspend fun clearVideoSpeedMemory() {
        // Clear video speed memory from base repository
        // This would be implemented in the base repository
        try {
            if (baseRepository is SettingsRepositoryImpl) {
                // Access the database or preferences to clear speed memory
                // Implementation depends on how speed memory is stored
            }
        } catch (e: Exception) {
            // Handle error
        }
    }
}