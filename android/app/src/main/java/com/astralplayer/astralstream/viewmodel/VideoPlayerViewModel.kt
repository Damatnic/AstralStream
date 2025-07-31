package com.astralplayer.astralstream.viewmodel

import android.app.Application
import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.Settings
import android.view.WindowManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.astralplayer.astralstream.ai.AISubtitleGenerator
import com.astralplayer.astralstream.data.dao.PlaybackStateDao
import com.astralplayer.astralstream.data.dao.VideoDao
import com.astralplayer.astralstream.data.entity.PlaybackStateEntity
import com.astralplayer.astralstream.data.entity.VideoEntity
import com.astralplayer.astralstream.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class VideoPlayerViewModel @Inject constructor(
    application: Application,
    private val videoDao: VideoDao,
    private val playbackStateDao: PlaybackStateDao,
    private val settingsRepository: SettingsRepository
) : AndroidViewModel(application) {
    
    private val context: Context get() = getApplication<Application>()
    
    // UI State
    private val _isDarkTheme = MutableStateFlow(true)
    val isDarkTheme: StateFlow<Boolean> = _isDarkTheme.asStateFlow()
    
    private val _isFullscreen = MutableStateFlow(false)
    val isFullscreen: StateFlow<Boolean> = _isFullscreen.asStateFlow()
    
    private val _showControls = MutableStateFlow(true)
    val showControls: StateFlow<Boolean> = _showControls.asStateFlow()
    
    // Playback State
    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()
    
    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration.asStateFlow()
    
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()
    
    private val _isBuffering = MutableStateFlow(false)
    val isBuffering: StateFlow<Boolean> = _isBuffering.asStateFlow()
    
    private val _playbackSpeed = MutableStateFlow(1.0f)
    val playbackSpeed: StateFlow<Float> = _playbackSpeed.asStateFlow()
    
    // Video Quality
    private val _videoQuality = MutableStateFlow("Auto")
    val videoQuality: StateFlow<String> = _videoQuality.asStateFlow()
    
    // Subtitles
    private val _subtitles = MutableStateFlow<List<AISubtitleGenerator.Subtitle>>(emptyList())
    val subtitles: StateFlow<List<AISubtitleGenerator.Subtitle>> = _subtitles.asStateFlow()
    
    private val _subtitlesEnabled = MutableStateFlow(false)
    val subtitlesEnabled: StateFlow<Boolean> = _subtitlesEnabled.asStateFlow()
    
    // Picture-in-Picture
    private val _isPictureInPictureMode = MutableStateFlow(false)
    val isPictureInPictureMode: StateFlow<Boolean> = _isPictureInPictureMode.asStateFlow()
    
    // AI Features
    private val _isAISubtitlesEnabled = MutableStateFlow(false)
    private val _aiSubtitleError = MutableStateFlow<String?>(null)
    
    // Display Settings
    private val _brightness = MutableStateFlow(0.5f)
    val brightness: StateFlow<Float> = _brightness.asStateFlow()
    
    private val _volume = MutableStateFlow(1.0f)
    val volume: StateFlow<Float> = _volume.asStateFlow()
    
    private val _zoomLevel = MutableStateFlow(1.0f)
    val zoomLevel: StateFlow<Float> = _zoomLevel.asStateFlow()
    
    // Error Handling
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    // Current Video
    private var currentVideo: VideoEntity? = null
    private var currentVideoId: Long = -1
    
    init {
        loadSettings()
    }
    
    private fun loadSettings() {
        viewModelScope.launch {
            settingsRepository.getSettings()?.let { settings ->
                _isDarkTheme.value = settings.themeMode != "light"
                _playbackSpeed.value = settings.playbackSpeed
                _subtitlesEnabled.value = settings.subtitlesEnabled
                _volume.value = settings.volumeLevel
                _brightness.value = settings.brightnessLevel
            }
        }
    }
    
    fun loadVideo(videoId: Long) {
        currentVideoId = videoId
        viewModelScope.launch {
            currentVideo = videoDao.getVideoById(videoId)
            
            // Load playback state
            playbackStateDao.getPlaybackState(videoId)?.let { state ->
                _currentPosition.value = state.position
                _playbackSpeed.value = state.playbackSpeed
            }
        }
    }
    
    fun toggleFullscreen() {
        _isFullscreen.value = !_isFullscreen.value
    }
    
    fun setControlsVisible(visible: Boolean) {
        _showControls.value = visible
    }
    
    fun updatePosition(position: Long) {
        _currentPosition.value = position
    }
    
    fun setPlaying(playing: Boolean) {
        _isPlaying.value = playing
    }
    
    fun setBuffering(buffering: Boolean) {
        _isBuffering.value = buffering
    }
    
    fun onVideoReady(duration: Long) {
        _duration.value = duration
        _isBuffering.value = false
    }
    
    fun onVideoEnded() {
        _isPlaying.value = false
        _currentPosition.value = _duration.value
        
        // Update last played time
        currentVideo?.let { video ->
            viewModelScope.launch {
                videoDao.updateVideo(
                    video.copy(lastPlayedTime = System.currentTimeMillis())
                )
            }
        }
    }
    
    fun savePlaybackPosition(videoId: Long, position: Long) {
        viewModelScope.launch {
            val state = PlaybackStateEntity(
                videoId = videoId,
                position = position,
                playbackSpeed = _playbackSpeed.value,
                lastUpdated = System.currentTimeMillis()
            )
            playbackStateDao.insertOrUpdate(state)
        }
    }
    
    fun savePlaybackState(videoId: Long, position: Long, speed: Float) {
        viewModelScope.launch {
            val state = PlaybackStateEntity(
                videoId = videoId,
                position = position,
                playbackSpeed = speed,
                lastUpdated = System.currentTimeMillis()
            )
            playbackStateDao.insertOrUpdate(state)
        }
    }
    
    fun setPlaybackSpeed(speed: Float) {
        _playbackSpeed.value = speed
        viewModelScope.launch {
            settingsRepository.setPlaybackSpeed(speed)
        }
    }
    
    fun setVideoQuality(quality: String) {
        _videoQuality.value = quality
        viewModelScope.launch {
            settingsRepository.setDefaultQuality(quality)
        }
    }
    
    fun toggleSubtitles() {
        _subtitlesEnabled.value = !_subtitlesEnabled.value
        viewModelScope.launch {
            settingsRepository.setSubtitlesEnabled(_subtitlesEnabled.value)
        }
    }
    
    fun setSubtitles(subtitles: List<AISubtitleGenerator.Subtitle>) {
        _subtitles.value = subtitles
    }
    
    fun isAISubtitlesEnabled(): Boolean {
        return _isAISubtitlesEnabled.value
    }
    
    fun onSubtitleGenerationError(error: Exception) {
        _aiSubtitleError.value = error.message
    }
    
    fun adjustBrightness(delta: Float) {
        val newBrightness = (_brightness.value + delta).coerceIn(0f, 1f)
        _brightness.value = newBrightness
        
        // Apply system brightness
        try {
            val window = (context as? android.app.Activity)?.window
            window?.attributes?.let { params ->
                params.screenBrightness = newBrightness
                window.attributes = params
            }
        } catch (e: Exception) {
            // Handle error
        }
        
        viewModelScope.launch {
            settingsRepository.setBrightnessLevel(newBrightness)
        }
    }
    
    fun adjustVolume(delta: Float) {
        val newVolume = (_volume.value + delta).coerceIn(0f, 1f)
        _volume.value = newVolume
        
        viewModelScope.launch {
            settingsRepository.setVolumeLevel(newVolume)
        }
    }
    
    fun setZoomLevel(scale: Float) {
        _zoomLevel.value = scale.coerceIn(0.5f, 3.0f)
    }
    
    fun setPictureInPictureMode(enabled: Boolean) {
        _isPictureInPictureMode.value = enabled
        if (enabled) {
            _showControls.value = false
        }
    }
    
    // Error Recovery
    fun retryWithLowerQuality() {
        val qualityLevels = listOf("1080p", "720p", "480p", "360p", "240p")
        val currentIndex = qualityLevels.indexOf(_videoQuality.value)
        if (currentIndex < qualityLevels.size - 1) {
            setVideoQuality(qualityLevels[currentIndex + 1])
            _error.value = "Switching to ${qualityLevels[currentIndex + 1]} quality"
        }
    }
    
    fun switchToSoftwareDecoder() {
        // Implement software decoder switch
        _error.value = "Switching to software decoder"
    }
    
    fun showError(message: String) {
        _error.value = message
    }
    
    fun clearError() {
        _error.value = null
    }
    
    override fun onCleared() {
        super.onCleared()
        // Save final playback state
        if (currentVideoId > 0) {
            savePlaybackPosition(currentVideoId, _currentPosition.value)
        }
    }
}