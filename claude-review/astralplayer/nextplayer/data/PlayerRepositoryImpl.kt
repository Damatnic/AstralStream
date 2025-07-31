package com.astralplayer.nextplayer.data

import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import android.util.Log

class PlayerRepositoryImpl(
    override val exoPlayer: ExoPlayer,
    private val context: Context
) : PlayerRepository {
    
    private val scope = CoroutineScope(Dispatchers.Main)
    
    private val _playerState = MutableStateFlow(
        PlayerUiState(
            playbackState = Player.STATE_IDLE,
            isPlaying = false,
            currentPosition = 0L,
            duration = 0L,
            bufferedPercentage = 0,
            playbackSpeed = 1.0f
        )
    )
    override val playerState: StateFlow<PlayerUiState> = _playerState.asStateFlow()
    
    init {
        setupPlayerListener()
        startPositionUpdates()
    }
    
    private fun setupPlayerListener() {
        exoPlayer.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                updatePlayerState()
            }
            
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                updatePlayerState()
            }
            
            override fun onPositionDiscontinuity(
                oldPosition: Player.PositionInfo,
                newPosition: Player.PositionInfo,
                reason: Int
            ) {
                updatePlayerState()
            }
            
            override fun onPlaybackParametersChanged(playbackParameters: androidx.media3.common.PlaybackParameters) {
                updatePlayerState()
            }
        })
    }
    
    private fun startPositionUpdates() {
        scope.launch {
            while (true) {
                updatePlayerState()
                delay(1000) // Update every second
            }
        }
    }
    
    private fun updatePlayerState() {
        _playerState.value = _playerState.value.copy(
            playbackState = exoPlayer.playbackState,
            isPlaying = exoPlayer.isPlaying,
            currentPosition = exoPlayer.currentPosition,
            duration = exoPlayer.duration.takeIf { it > 0 } ?: 0L,
            bufferedPercentage = exoPlayer.bufferedPercentage,
            playbackSpeed = exoPlayer.playbackParameters.speed
        )
    }
    
    // Simplified implementations of all required interface methods
    override suspend fun playVideo(uri: Uri, title: String?): Result<Unit> = 
        try {
            val mediaItem = MediaItem.fromUri(uri)
            exoPlayer.setMediaItem(mediaItem)
            exoPlayer.prepare()
            exoPlayer.playWhenReady = true
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    
    override suspend fun pauseVideo(): Result<Unit> = 
        try {
            exoPlayer.pause()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    
    override suspend fun resumeVideo(): Result<Unit> = 
        try {
            exoPlayer.play()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    
    override suspend fun togglePlayPause(): Result<Unit> = 
        if (exoPlayer.isPlaying) pauseVideo() else resumeVideo()
    
    override suspend fun seekTo(position: Long): Result<Unit> = 
        try {
            exoPlayer.seekTo(position)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    
    override suspend fun seekBy(deltaMs: Long): Result<Unit> = 
        try {
            val newPosition = (exoPlayer.currentPosition + deltaMs).coerceIn(0, exoPlayer.duration)
            exoPlayer.seekTo(newPosition)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    
    override suspend fun stopVideo(): Result<Unit> = 
        try {
            exoPlayer.stop()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    
    override suspend fun setPlaybackSpeed(speed: Float): Result<Unit> = 
        try {
            exoPlayer.setPlaybackSpeed(speed)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    
    override suspend fun setVolume(volume: Float): Result<Unit> = 
        try {
            exoPlayer.volume = volume.coerceIn(0f, 1f)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    
    override suspend fun adjustVolume(delta: Float): Result<Unit> = 
        try {
            val newVolume = (exoPlayer.volume + delta).coerceIn(0f, 1f)
            exoPlayer.volume = newVolume
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    
    override suspend fun setBrightness(brightness: Float): Result<Unit> = Result.success(Unit)
    override suspend fun adjustBrightness(delta: Float): Result<Unit> = Result.success(Unit)
    
    override suspend fun setRepeatMode(repeatMode: Int): Result<Unit> = 
        try {
            exoPlayer.repeatMode = repeatMode
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    
    override suspend fun toggleRepeatMode(): Result<Unit> = 
        try {
            val newMode = when (exoPlayer.repeatMode) {
                Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ONE
                else -> Player.REPEAT_MODE_OFF
            }
            exoPlayer.repeatMode = newMode
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    
    override suspend fun setShuffleMode(enabled: Boolean): Result<Unit> = 
        try {
            exoPlayer.shuffleModeEnabled = enabled
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    
    override suspend fun toggleShuffleMode(): Result<Unit> = 
        try {
            exoPlayer.shuffleModeEnabled = !exoPlayer.shuffleModeEnabled
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    
    override suspend fun setLoopMode(loopMode: LoopMode): Result<Unit> = Result.success(Unit)
    override suspend fun toggleLoopMode(): Result<Unit> = Result.success(Unit)
    override fun getCurrentLoopMode(): LoopMode = LoopMode.OFF
    
    override suspend fun getAvailableQualities(): List<VideoQuality> = emptyList()
    override suspend fun setVideoQuality(quality: VideoQuality): Result<Unit> = Result.success(Unit)
    override suspend fun enableDataSaverMode(enabled: Boolean): Result<Unit> = Result.success(Unit)
    override suspend fun getNetworkInfo(): NetworkInfo = NetworkInfo(NetworkInfo.ConnectionType.WIFI, true, false, 100_000_000L)
    override suspend fun getRecommendedQuality(): VideoQuality? = null
    override suspend fun getSubtitleTracks(): List<SubtitleTrack> = emptyList()
    override suspend fun selectSubtitleTrack(track: SubtitleTrack?): Result<Unit> = Result.success(Unit)
    
    override suspend fun addToQueue(uri: Uri, title: String?): Result<Unit> = Result.success(Unit)
    override suspend fun removeFromQueue(index: Int): Result<Unit> = Result.success(Unit)
    override suspend fun skipToNext(): Result<Unit> = Result.success(Unit)
    override suspend fun skipToPrevious(): Result<Unit> = Result.success(Unit)
    override suspend fun skipToQueueItem(index: Int): Result<Unit> = Result.success(Unit)
    override suspend fun playPlaylist(videos: List<PlaylistVideo>, shuffle: Boolean): Result<Unit> = Result.success(Unit)
    override suspend fun clearQueue(): Result<Unit> = Result.success(Unit)
    
    override fun handleGestureAction(action: GestureAction) {}
    override fun observePlaybackPosition(): Flow<Long> = flowOf(0L)
    override fun observeBufferingState(): Flow<Boolean> = flowOf(false)
    override fun observeVideoSize(): Flow<VideoSize> = flowOf(VideoSize.UNKNOWN)
    
    override fun getVolume(): Float = exoPlayer.volume
    override suspend fun setVolumeBoost(enabled: Boolean): Result<Unit> = Result.success(Unit)
    override fun getMaxVolume(): Int = 100
    override fun getVolumePercentage(): Int = (exoPlayer.volume * 100).toInt()
    
    override fun release() {
        exoPlayer.release()
    }
}