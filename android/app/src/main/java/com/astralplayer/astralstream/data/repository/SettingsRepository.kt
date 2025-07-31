package com.astralplayer.astralstream.data.repository

import kotlinx.coroutines.flow.Flow
import com.astralplayer.astralstream.data.entity.SettingsEntity

interface SettingsRepository {
    // Core settings
    suspend fun getSettings(): SettingsEntity?
    fun getSettingsFlow(): Flow<SettingsEntity?>
    suspend fun updateSettings(settings: SettingsEntity)
    suspend fun resetSettings()
    
    // Video player settings
    suspend fun getDefaultQuality(): String
    suspend fun setDefaultQuality(quality: String)
    suspend fun getPlaybackSpeed(): Float
    suspend fun setPlaybackSpeed(speed: Float)
    suspend fun getAutoPlayEnabled(): Boolean
    suspend fun setAutoPlayEnabled(enabled: Boolean)
    suspend fun getSubtitlesEnabled(): Boolean
    suspend fun setSubtitlesEnabled(enabled: Boolean)
    
    // Audio settings
    suspend fun getVolumeLevel(): Float
    suspend fun setVolumeLevel(volume: Float)
    suspend fun getAudioTrackLanguage(): String
    suspend fun setAudioTrackLanguage(language: String)
    
    // Display settings
    suspend fun getBrightnessLevel(): Float
    suspend fun setBrightnessLevel(brightness: Float)
    suspend fun getAspectRatio(): String
    suspend fun setAspectRatio(ratio: String)
    suspend fun getScreenOrientation(): String
    suspend fun setScreenOrientation(orientation: String)
    
    // Network settings
    suspend fun getBufferSize(): Int
    suspend fun setBufferSize(size: Int)
    suspend fun getPreferredNetwork(): String
    suspend fun setPreferredNetwork(network: String)
    suspend fun getDataSaverEnabled(): Boolean
    suspend fun setDataSaverEnabled(enabled: Boolean)
    
    // Advanced settings
    suspend fun getHardwareAccelerationEnabled(): Boolean
    suspend fun setHardwareAccelerationEnabled(enabled: Boolean)
    suspend fun getDecoderType(): String
    suspend fun setDecoderType(type: String)
    suspend fun getCacheSize(): Long
    suspend fun setCacheSize(size: Long)
    suspend fun clearCache()
    
    // Theme settings
    suspend fun getThemeMode(): String
    suspend fun setThemeMode(mode: String)
    suspend fun getAccentColor(): String
    suspend fun setAccentColor(color: String)
}