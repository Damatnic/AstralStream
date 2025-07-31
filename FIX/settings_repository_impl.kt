package com.astralplayer.astralstream.data.repository

import android.content.Context
import kotlinx.coroutines.flow.Flow
import com.astralplayer.astralstream.data.entity.SettingsEntity
import com.astralplayer.astralstream.data.dao.SettingsDao
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val settingsDao: SettingsDao,
    private val context: Context
) : SettingsRepository {
    
    companion object {
        const val DEFAULT_QUALITY = "1080p"
        const val DEFAULT_PLAYBACK_SPEED = 1.0f
        const val DEFAULT_VOLUME = 1.0f
        const val DEFAULT_BRIGHTNESS = 0.5f
        const val DEFAULT_ASPECT_RATIO = "16:9"
        const val DEFAULT_ORIENTATION = "auto"
        const val DEFAULT_BUFFER_SIZE = 30000 // 30 seconds
        const val DEFAULT_NETWORK = "any"
        const val DEFAULT_DECODER = "auto"
        const val DEFAULT_CACHE_SIZE = 100L * 1024 * 1024 // 100MB
        const val DEFAULT_THEME = "system"
        const val DEFAULT_ACCENT = "#6200EE"
        const val DEFAULT_AUDIO_LANGUAGE = "en"
    }
    
    override suspend fun getSettings(): SettingsEntity? {
        var settings = settingsDao.getSettings()
        if (settings == null) {
            settings = createDefaultSettings()
            settingsDao.insert(settings)
        }
        return settings
    }
    
    override fun getSettingsFlow(): Flow<SettingsEntity?> {
        return settingsDao.getSettingsFlow()
    }
    
    override suspend fun updateSettings(settings: SettingsEntity) {
        settingsDao.update(settings)
    }
    
    override suspend fun resetSettings() {
        settingsDao.deleteAll()
        settingsDao.insert(createDefaultSettings())
    }
    
    // Video player settings
    override suspend fun getDefaultQuality(): String {
        return getSettings()?.defaultQuality ?: DEFAULT_QUALITY
    }
    
    override suspend fun setDefaultQuality(quality: String) {
        getSettings()?.let { settings ->
            settingsDao.update(settings.copy(defaultQuality = quality))
        }
    }
    
    override suspend fun getPlaybackSpeed(): Float {
        return getSettings()?.playbackSpeed ?: DEFAULT_PLAYBACK_SPEED
    }
    
    override suspend fun setPlaybackSpeed(speed: Float) {
        getSettings()?.let { settings ->
            settingsDao.update(settings.copy(playbackSpeed = speed))
        }
    }
    
    override suspend fun getAutoPlayEnabled(): Boolean {
        return getSettings()?.autoPlayEnabled ?: true
    }
    
    override suspend fun setAutoPlayEnabled(enabled: Boolean) {
        getSettings()?.let { settings ->
            settingsDao.update(settings.copy(autoPlayEnabled = enabled))
        }
    }
    
    override suspend fun getSubtitlesEnabled(): Boolean {
        return getSettings()?.subtitlesEnabled ?: false
    }
    
    override suspend fun setSubtitlesEnabled(enabled: Boolean) {
        getSettings()?.let { settings ->
            settingsDao.update(settings.copy(subtitlesEnabled = enabled))
        }
    }
    
    // Audio settings
    override suspend fun getVolumeLevel(): Float {
        return getSettings()?.volumeLevel ?: DEFAULT_VOLUME
    }
    
    override suspend fun setVolumeLevel(volume: Float) {
        getSettings()?.let { settings ->
            settingsDao.update(settings.copy(volumeLevel = volume))
        }
    }
    
    override suspend fun getAudioTrackLanguage(): String {
        return getSettings()?.audioTrackLanguage ?: DEFAULT_AUDIO_LANGUAGE
    }
    
    override suspend fun setAudioTrackLanguage(language: String) {
        getSettings()?.let { settings ->
            settingsDao.update(settings.copy(audioTrackLanguage = language))
        }
    }
    
    // Display settings
    override suspend fun getBrightnessLevel(): Float {
        return getSettings()?.brightnessLevel ?: DEFAULT_BRIGHTNESS
    }
    
    override suspend fun setBrightnessLevel(brightness: Float) {
        getSettings()?.let { settings ->
            settingsDao.update(settings.copy(brightnessLevel = brightness))
        }
    }
    
    override suspend fun getAspectRatio(): String {
        return getSettings()?.aspectRatio ?: DEFAULT_ASPECT_RATIO
    }
    
    override suspend fun setAspectRatio(ratio: String) {
        getSettings()?.let { settings ->
            settingsDao.update(settings.copy(aspectRatio = ratio))
        }
    }
    
    override suspend fun getScreenOrientation(): String {
        return getSettings()?.screenOrientation ?: DEFAULT_ORIENTATION
    }
    
    override suspend fun setScreenOrientation(orientation: String) {
        getSettings()?.let { settings ->
            settingsDao.update(settings.copy(screenOrientation = orientation))
        }
    }
    
    // Network settings
    override suspend fun getBufferSize(): Int {
        return getSettings()?.bufferSize ?: DEFAULT_BUFFER_SIZE
    }
    
    override suspend fun setBufferSize(size: Int) {
        getSettings()?.let { settings ->
            settingsDao.update(settings.copy(bufferSize = size))
        }
    }
    
    override suspend fun getPreferredNetwork(): String {
        return getSettings()?.preferredNetwork ?: DEFAULT_NETWORK
    }
    
    override suspend fun setPreferredNetwork(network: String) {
        getSettings()?.let { settings ->
            settingsDao.update(settings.copy(preferredNetwork = network))
        }
    }
    
    override suspend fun getDataSaverEnabled(): Boolean {
        return getSettings()?.dataSaverEnabled ?: false
    }
    
    override suspend fun setDataSaverEnabled(enabled: Boolean) {
        getSettings()?.let { settings ->
            settingsDao.update(settings.copy(dataSaverEnabled = enabled))
        }
    }
    
    // Advanced settings
    override suspend fun getHardwareAccelerationEnabled(): Boolean {
        return getSettings()?.hardwareAccelerationEnabled ?: true
    }
    
    override suspend fun setHardwareAccelerationEnabled(enabled: Boolean) {
        getSettings()?.let { settings ->
            settingsDao.update(settings.copy(hardwareAccelerationEnabled = enabled))
        }
    }
    
    override suspend fun getDecoderType(): String {
        return getSettings()?.decoderType ?: DEFAULT_DECODER
    }
    
    override suspend fun setDecoderType(type: String) {
        getSettings()?.let { settings ->
            settingsDao.update(settings.copy(decoderType = type))
        }
    }
    
    override suspend fun getCacheSize(): Long {
        return getSettings()?.cacheSize ?: DEFAULT_CACHE_SIZE
    }
    
    override suspend fun setCacheSize(size: Long) {
        getSettings()?.let { settings ->
            settingsDao.update(settings.copy(cacheSize = size))
        }
    }
    
    override suspend fun clearCache() {
        // Clear ExoPlayer cache
        val cacheDir = context.cacheDir
        cacheDir.listFiles()?.forEach { file ->
            if (file.name.startsWith("exoplayer")) {
                file.deleteRecursively()
            }
        }
    }
    
    // Theme settings
    override suspend fun getThemeMode(): String {
        return getSettings()?.themeMode ?: DEFAULT_THEME
    }
    
    override suspend fun setThemeMode(mode: String) {
        getSettings()?.let { settings ->
            settingsDao.update(settings.copy(themeMode = mode))
        }
    }
    
    override suspend fun getAccentColor(): String {
        return getSettings()?.accentColor ?: DEFAULT_ACCENT
    }
    
    override suspend fun setAccentColor(color: String) {
        getSettings()?.let { settings ->
            settingsDao.update(settings.copy(accentColor = color))
        }
    }
    
    private fun createDefaultSettings(): SettingsEntity {
        return SettingsEntity(
            id = 1,
            defaultQuality = DEFAULT_QUALITY,
            playbackSpeed = DEFAULT_PLAYBACK_SPEED,
            autoPlayEnabled = true,
            subtitlesEnabled = false,
            volumeLevel = DEFAULT_VOLUME,
            audioTrackLanguage = DEFAULT_AUDIO_LANGUAGE,
            brightnessLevel = DEFAULT_BRIGHTNESS,
            aspectRatio = DEFAULT_ASPECT_RATIO,
            screenOrientation = DEFAULT_ORIENTATION,
            bufferSize = DEFAULT_BUFFER_SIZE,
            preferredNetwork = DEFAULT_NETWORK,
            dataSaverEnabled = false,
            hardwareAccelerationEnabled = true,
            decoderType = DEFAULT_DECODER,
            cacheSize = DEFAULT_CACHE_SIZE,
            themeMode = DEFAULT_THEME,
            accentColor = DEFAULT_ACCENT
        )
    }
}