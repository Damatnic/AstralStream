package com.astralplayer.nextplayer.data

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Tracks
import androidx.media3.exoplayer.ExoPlayer
import com.astralplayer.nextplayer.utils.EnhancedVolumeManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow


data class PlayerUiState(
    val isPlaying: Boolean = false,
    val currentPosition: Long = 0L,
    val duration: Long = 0L,
    val bufferedPercentage: Int = 0,
    val playbackSpeed: Float = 1.0f,
    val volume: Float = 1.0f,
    val brightness: Float = 0.5f, // Screen brightness (0.0 to 1.0)
    val isLoading: Boolean = false,
    val error: String? = null,
    val currentMediaItem: MediaItem? = null,
    val isBuffering: Boolean = false,
    val playbackState: Int = Player.STATE_IDLE,
    val repeatMode: Int = Player.REPEAT_MODE_OFF,
    val shuffleMode: Boolean = false,
    val audioSessionId: Int = 0,
    val videoSize: VideoSize = VideoSize.UNKNOWN,
    val availableQualities: List<String> = emptyList(),
    val currentQuality: String? = null,
    val subtitleTracks: List<SubtitleTrack> = emptyList(),
    val currentSubtitleTrack: SubtitleTrack? = null
)

data class VideoSize(
    val width: Int,
    val height: Int,
    val aspectRatio: Float
) {
    companion object {
        val UNKNOWN = VideoSize(0, 0, 0f)
    }
}

data class SubtitleTrack(
    val id: String,
    val language: String,
    val label: String,
    val isSelected: Boolean = false
)

data class PlaylistVideo(
    val uri: Uri,
    val title: String,
    val duration: Long = 0L
)

interface PlayerRepository {
    val playerState: StateFlow<PlayerUiState>
    val exoPlayer: ExoPlayer // Expose ExoPlayer for UI integration
    
    // Basic playback control
    suspend fun playVideo(uri: Uri, title: String? = null): Result<Unit>
    suspend fun pauseVideo(): Result<Unit>
    suspend fun resumeVideo(): Result<Unit>
    suspend fun togglePlayPause(): Result<Unit>
    suspend fun seekTo(position: Long): Result<Unit>
    suspend fun seekBy(deltaMs: Long): Result<Unit>
    suspend fun stopVideo(): Result<Unit>
    
    // Audio and video control
    suspend fun setPlaybackSpeed(speed: Float): Result<Unit>
    suspend fun setVolume(volume: Float): Result<Unit>
    suspend fun adjustVolume(delta: Float): Result<Unit>
    suspend fun setBrightness(brightness: Float): Result<Unit>
    suspend fun adjustBrightness(delta: Float): Result<Unit>
    
    // Repeat and shuffle
    suspend fun setRepeatMode(repeatMode: Int): Result<Unit>
    suspend fun toggleRepeatMode(): Result<Unit>
    suspend fun setShuffleMode(enabled: Boolean): Result<Unit>
    suspend fun toggleShuffleMode(): Result<Unit>
    
    // Loop mode functionality
    suspend fun setLoopMode(loopMode: LoopMode): Result<Unit>
    suspend fun toggleLoopMode(): Result<Unit>
    fun getCurrentLoopMode(): LoopMode
    
    // Track selection
    suspend fun getAvailableQualities(): List<VideoQuality>
    suspend fun setVideoQuality(quality: VideoQuality): Result<Unit>
    suspend fun enableDataSaverMode(enabled: Boolean): Result<Unit>
    suspend fun getNetworkInfo(): NetworkInfo
    suspend fun getRecommendedQuality(): VideoQuality?
    suspend fun getSubtitleTracks(): List<SubtitleTrack>
    suspend fun selectSubtitleTrack(track: SubtitleTrack?): Result<Unit>
    
    // Playlist management
    suspend fun addToQueue(uri: Uri, title: String? = null): Result<Unit>
    suspend fun removeFromQueue(index: Int): Result<Unit>
    suspend fun skipToNext(): Result<Unit>
    suspend fun skipToPrevious(): Result<Unit>
    suspend fun skipToQueueItem(index: Int): Result<Unit>
    suspend fun playPlaylist(videos: List<PlaylistVideo>, shuffle: Boolean = false): Result<Unit>
    suspend fun clearQueue(): Result<Unit>
    
    // State observation
    fun observePlaybackPosition(): Flow<Long>
    fun observeBufferingState(): Flow<Boolean>
    fun observeVideoSize(): Flow<VideoSize>
    
    // Gesture handling
    fun handleGestureAction(action: GestureAction)
    
    // Get current volume
    fun getVolume(): Float
    
    // Volume boost functionality
    suspend fun setVolumeBoost(enabled: Boolean): Result<Unit>
    fun getMaxVolume(): Int
    fun getVolumePercentage(): Int
    
    // Lifecycle
    fun release()
}

class PlayerRepositoryImpl constructor(
    override val exoPlayer: ExoPlayer,
    private val context: Context
) : PlayerRepository {
    
    private val _playerState = MutableStateFlow(PlayerUiState())
    override val playerState: StateFlow<PlayerUiState> = _playerState.asStateFlow()
    
    // Quality management
    private val streamQualityManager = StreamQualityManager(context, exoPlayer)
    val availableQualities = streamQualityManager.availableQualities
    val currentQuality = streamQualityManager.currentQuality
    val qualitySettings = streamQualityManager.qualitySettings
    
    // Enhanced volume management with LoudnessEnhancer support
    private val enhancedVolumeManager = EnhancedVolumeManager(context, exoPlayer)
    
    // Loop mode state
    private var currentLoopMode = LoopMode.OFF
    
    private val playerListener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            val stateString = when(playbackState) {
                Player.STATE_IDLE -> "IDLE"
                Player.STATE_BUFFERING -> "BUFFERING"
                Player.STATE_READY -> "READY"
                Player.STATE_ENDED -> "ENDED"
                else -> "UNKNOWN"
            }
            Log.d("PlayerRepository", "Playback state changed to: $stateString")
            
            updatePlayerState {
                copy(
                    playbackState = playbackState,
                    isLoading = playbackState == Player.STATE_BUFFERING,
                    isPlaying = playbackState == Player.STATE_READY && exoPlayer.playWhenReady,
                    duration = if (playbackState == Player.STATE_READY && exoPlayer.duration != androidx.media3.common.C.TIME_UNSET) {
                        exoPlayer.duration
                    } else duration
                )
            }
            
            if (playbackState == Player.STATE_READY) {
                Log.d("PlayerRepository", "Video ready - Duration: ${exoPlayer.duration}ms")
                // Initialize LoudnessEnhancer when player is ready
                enhancedVolumeManager.initializeLoudnessEnhancer()
            }
        }
        
        override fun onVideoSizeChanged(videoSize: androidx.media3.common.VideoSize) {
            Log.d("PlayerRepository", "Video size changed: ${videoSize.width}x${videoSize.height}")
            
            val aspectRatio = if (videoSize.height > 0) {
                videoSize.width.toFloat() / videoSize.height.toFloat()
            } else 0f
            
            updatePlayerState {
                copy(
                    videoSize = VideoSize(
                        width = videoSize.width,
                        height = videoSize.height,
                        aspectRatio = aspectRatio
                    )
                )
            }
        }
        
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            updatePlayerState {
                copy(isPlaying = isPlaying)
            }
        }
        
        override fun onPlayerError(error: PlaybackException) {
            updatePlayerState {
                copy(
                    error = error.message ?: "Unknown playback error",
                    isLoading = false,
                    isPlaying = false
                )
            }
        }
        
        override fun onTracksChanged(tracks: Tracks) {
            // Update available video qualities
            streamQualityManager.updateAvailableQualities(tracks)
        }
        
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            updatePlayerState {
                copy(
                    currentMediaItem = mediaItem,
                    duration = if (exoPlayer.duration != androidx.media3.common.C.TIME_UNSET) {
                        exoPlayer.duration
                    } else 0L
                )
            }
        }
    }
    
    init {
        exoPlayer.addListener(playerListener)
    }
    
    override suspend fun playVideo(uri: Uri, title: String?): Result<Unit> {
        return try {
            Log.d("PlayerRepository", "Playing video: $uri")
            val mediaItem = MediaItem.Builder()
                .setUri(uri)
                .apply {
                    title?.let { setMediaMetadata(
                        androidx.media3.common.MediaMetadata.Builder()
                            .setTitle(it)
                            .build()
                    )}
                }
                .build()
            
            Log.d("PlayerRepository", "MediaItem created: ${mediaItem.mediaId}")
            exoPlayer.setMediaItem(mediaItem)
            exoPlayer.prepare()
            exoPlayer.playWhenReady = true
            
            updatePlayerState {
                copy(
                    error = null,
                    isLoading = true,
                    currentMediaItem = mediaItem
                )
            }
            
            Log.d("PlayerRepository", "ExoPlayer prepared and ready to play")
            Result.success(Unit)
        } catch (e: Exception) {
            updatePlayerState {
                copy(
                    error = e.message ?: "Failed to play video",
                    isLoading = false
                )
            }
            Result.failure(e)
        }
    }
    
    override suspend fun pauseVideo(): Result<Unit> {
        return try {
            exoPlayer.playWhenReady = false
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun resumeVideo(): Result<Unit> {
        return try {
            exoPlayer.playWhenReady = true
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun seekTo(position: Long): Result<Unit> {
        return try {
            exoPlayer.seekTo(position)
            updatePlayerState {
                copy(currentPosition = position)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun setPlaybackSpeed(speed: Float): Result<Unit> {
        return try {
            exoPlayer.setPlaybackSpeed(speed)
            updatePlayerState {
                copy(playbackSpeed = speed)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun setVolume(volume: Float): Result<Unit> {
        return try {
            // Use enhanced volume manager for volume control with boost support
            enhancedVolumeManager.setVolume(volume)
            updatePlayerState {
                copy(volume = enhancedVolumeManager.currentVolume)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun togglePlayPause(): Result<Unit> {
        return if (exoPlayer.isPlaying) {
            pauseVideo()
        } else {
            resumeVideo()
        }
    }
    
    override suspend fun seekBy(deltaMs: Long): Result<Unit> {
        return try {
            val newPosition = (exoPlayer.currentPosition + deltaMs).coerceAtLeast(0L)
            exoPlayer.seekTo(newPosition)
            updatePlayerState {
                copy(currentPosition = newPosition)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun adjustVolume(delta: Float): Result<Unit> {
        return try {
            // Use enhanced volume manager for delta adjustments with boost support
            enhancedVolumeManager.adjustVolume(delta)
            updatePlayerState {
                copy(volume = enhancedVolumeManager.currentVolume)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun stopVideo(): Result<Unit> {
        return try {
            exoPlayer.stop()
            exoPlayer.clearMediaItems()
            updatePlayerState {
                PlayerUiState() // Reset to default state
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override fun handleGestureAction(action: GestureAction) {
        // Use a simple approach without coroutines for now
        try {
            when (action) {
                is GestureAction.Seek -> {
                    val newPosition = (exoPlayer.currentPosition + action.deltaMs).coerceAtLeast(0L)
                    exoPlayer.seekTo(newPosition)
                }
                is GestureAction.VolumeChange -> {
                    val newVolume = (exoPlayer.volume + action.delta).coerceIn(0f, 1f)
                    exoPlayer.volume = newVolume
                }
                is GestureAction.BrightnessChange -> {
                    // Brightness control would be handled at the Activity level
                }
                is GestureAction.DoubleTapSeek -> {
                    val seekAmount = if (action.forward) action.amount else -action.amount
                    val newPosition = (exoPlayer.currentPosition + seekAmount).coerceAtLeast(0L)
                    exoPlayer.seekTo(newPosition)
                }
                is GestureAction.TogglePlayPause -> {
                    exoPlayer.playWhenReady = !exoPlayer.playWhenReady
                }
                is GestureAction.LongPressSeek -> {
                    val speed = if (action.direction == SeekDirection.FORWARD) action.speed else -action.speed
                    exoPlayer.setPlaybackSpeed(speed)
                }
                is GestureAction.PinchZoom -> {
                    // Zoom would be handled at the UI level
                }
                is GestureAction.GestureConflict -> {
                    // Log or handle gesture conflicts
                }
            }
        } catch (e: Exception) {
            // Handle errors silently for now
        }
    }
    
    override fun observePlaybackPosition(): Flow<Long> {
        // This would typically use a timer or coroutine to emit position updates
        // For now, return a simple flow
        return kotlinx.coroutines.flow.flow {
            while (true) {
                emit(exoPlayer.currentPosition)
                kotlinx.coroutines.delay(1000) // Update every second
            }
        }
    }
    
    override fun observeBufferingState(): Flow<Boolean> {
        return kotlinx.coroutines.flow.flow {
            while (true) {
                emit(exoPlayer.playbackState == Player.STATE_BUFFERING)
                kotlinx.coroutines.delay(500) // Update every 500ms
            }
        }
    }
    
    // Brightness control (handled at UI level)
    override suspend fun setBrightness(brightness: Float): Result<Unit> {
        return try {
            val clampedBrightness = brightness.coerceIn(0f, 1f)
            updatePlayerState {
                copy(brightness = clampedBrightness)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun adjustBrightness(delta: Float): Result<Unit> {
        return try {
            val newBrightness = (_playerState.value.brightness + delta).coerceIn(0f, 1f)
            updatePlayerState {
                copy(brightness = newBrightness)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Repeat and shuffle modes
    override suspend fun setRepeatMode(repeatMode: Int): Result<Unit> {
        return try {
            exoPlayer.repeatMode = repeatMode
            updatePlayerState {
                copy(repeatMode = repeatMode)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun toggleRepeatMode(): Result<Unit> {
        return try {
            val newMode = when (exoPlayer.repeatMode) {
                Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ONE
                Player.REPEAT_MODE_ONE -> Player.REPEAT_MODE_ALL
                Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_OFF
                else -> Player.REPEAT_MODE_OFF
            }
            exoPlayer.repeatMode = newMode
            updatePlayerState {
                copy(repeatMode = newMode)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun setShuffleMode(enabled: Boolean): Result<Unit> {
        return try {
            exoPlayer.shuffleModeEnabled = enabled
            updatePlayerState {
                copy(shuffleMode = enabled)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun toggleShuffleMode(): Result<Unit> {
        return try {
            val newMode = !exoPlayer.shuffleModeEnabled
            exoPlayer.shuffleModeEnabled = newMode
            updatePlayerState {
                copy(shuffleMode = newMode)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Track selection (simplified implementation)
    override suspend fun getAvailableQualities(): List<VideoQuality> {
        return streamQualityManager.availableQualities.value
    }
    
    override suspend fun setVideoQuality(quality: VideoQuality): Result<Unit> {
        return try {
            streamQualityManager.selectQuality(quality)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun enableDataSaverMode(enabled: Boolean): Result<Unit> {
        return try {
            streamQualityManager.enableDataSaverMode(enabled)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun getNetworkInfo(): NetworkInfo {
        return streamQualityManager.getNetworkInfo()
    }
    
    override suspend fun getRecommendedQuality(): VideoQuality? {
        return streamQualityManager.getRecommendedQuality()
    }
    
    override suspend fun getSubtitleTracks(): List<SubtitleTrack> {
        return try {
            // This would typically analyze available text tracks
            // For now, return empty list
            emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    override suspend fun selectSubtitleTrack(track: SubtitleTrack?): Result<Unit> {
        return try {
            // This would typically set track selection parameters
            // For now, just update the state
            updatePlayerState {
                copy(currentSubtitleTrack = track)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Playlist management
    override suspend fun addToQueue(uri: Uri, title: String?): Result<Unit> {
        return try {
            val mediaItem = MediaItem.Builder()
                .setUri(uri)
                .apply {
                    title?.let { setMediaMetadata(
                        androidx.media3.common.MediaMetadata.Builder()
                            .setTitle(it)
                            .build()
                    )}
                }
                .build()
            
            exoPlayer.addMediaItem(mediaItem)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun removeFromQueue(index: Int): Result<Unit> {
        return try {
            if (index >= 0 && index < exoPlayer.mediaItemCount) {
                exoPlayer.removeMediaItem(index)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun skipToNext(): Result<Unit> {
        return try {
            if (exoPlayer.hasNextMediaItem()) {
                exoPlayer.seekToNext()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun skipToPrevious(): Result<Unit> {
        return try {
            if (exoPlayer.hasPreviousMediaItem()) {
                exoPlayer.seekToPrevious()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun skipToQueueItem(index: Int): Result<Unit> {
        return try {
            if (index >= 0 && index < exoPlayer.mediaItemCount) {
                exoPlayer.seekToDefaultPosition(index)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun playPlaylist(videos: List<PlaylistVideo>, shuffle: Boolean): Result<Unit> {
        return try {
            // Clear current queue
            exoPlayer.clearMediaItems()
            
            // Prepare the video list
            val videoList = if (shuffle) videos.shuffled() else videos
            
            // Add all videos to queue
            val mediaItems = videoList.map { video ->
                MediaItem.Builder()
                    .setUri(video.uri)
                    .setMediaMetadata(
                        androidx.media3.common.MediaMetadata.Builder()
                            .setTitle(video.title)
                            .build()
                    )
                    .build()
            }
            
            exoPlayer.addMediaItems(mediaItems)
            exoPlayer.prepare()
            exoPlayer.playWhenReady = true
            
            // Set shuffle mode
            exoPlayer.shuffleModeEnabled = shuffle
            
            updatePlayerState {
                copy(
                    error = null,
                    isLoading = true,
                    shuffleMode = shuffle
                )
            }
            
            Log.d("PlayerRepository", "Playing playlist with ${videos.size} videos (shuffle: $shuffle)")
            Result.success(Unit)
        } catch (e: Exception) {
            updatePlayerState {
                copy(
                    error = e.message ?: "Failed to play playlist",
                    isLoading = false
                )
            }
            Result.failure(e)
        }
    }
    
    override suspend fun clearQueue(): Result<Unit> {
        return try {
            exoPlayer.clearMediaItems()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Enhanced state observation
    override fun observeVideoSize(): Flow<VideoSize> {
        return kotlinx.coroutines.flow.flow {
            while (true) {
                val videoFormat = exoPlayer.videoFormat
                val size = if (videoFormat != null) {
                    VideoSize(
                        width = videoFormat.width,
                        height = videoFormat.height,
                        aspectRatio = if (videoFormat.height > 0) {
                            videoFormat.width.toFloat() / videoFormat.height.toFloat()
                        } else 0f
                    )
                } else {
                    VideoSize.UNKNOWN
                }
                emit(size)
                kotlinx.coroutines.delay(1000) // Update every second
            }
        }
    }
    
    override fun getVolume(): Float {
        return enhancedVolumeManager.currentVolume
    }
    
    override suspend fun setVolumeBoost(enabled: Boolean): Result<Unit> {
        return try {
            enhancedVolumeManager.setVolumeBoost(enabled)
            Log.d("PlayerRepository", "Volume boost ${if (enabled) "enabled" else "disabled"}")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("PlayerRepository", "Failed to set volume boost", e)
            Result.failure(e)
        }
    }
    
    override fun getMaxVolume(): Int {
        return enhancedVolumeManager.maxVolume
    }
    
    override fun getVolumePercentage(): Int {
        return enhancedVolumeManager.volumePercentage
    }
    
    override suspend fun setLoopMode(loopMode: LoopMode): Result<Unit> {
        return try {
            currentLoopMode = loopMode
            exoPlayer.repeatMode = loopMode.toExoPlayerRepeatMode()
            Log.d("PlayerRepository", "Loop mode set to: ${loopMode.getDisplayName()}")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("PlayerRepository", "Failed to set loop mode", e)
            Result.failure(e)
        }
    }
    
    override suspend fun toggleLoopMode(): Result<Unit> {
        return try {
            val nextLoopMode = currentLoopMode.next()
            setLoopMode(nextLoopMode)
        } catch (e: Exception) {
            Log.e("PlayerRepository", "Failed to toggle loop mode", e)
            Result.failure(e)
        }
    }
    
    override fun getCurrentLoopMode(): LoopMode {
        return currentLoopMode
    }
    
    override fun release() {
        exoPlayer.removeListener(playerListener)
        enhancedVolumeManager.release()
        exoPlayer.release()
    }
    
    private fun updatePlayerState(update: PlayerUiState.() -> PlayerUiState) {
        _playerState.value = _playerState.value.update()
    }
}