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

// PlaylistVideo moved to dedicated file

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

