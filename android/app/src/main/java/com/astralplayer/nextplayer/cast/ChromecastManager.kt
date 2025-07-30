package com.astralplayer.nextplayer.cast

import android.content.Context
import android.net.Uri
import com.google.android.gms.cast.MediaInfo
import com.google.android.gms.cast.MediaLoadRequestData
import com.google.android.gms.cast.MediaMetadata
import com.google.android.gms.cast.MediaStatus
import com.google.android.gms.cast.framework.CastContext
import com.google.android.gms.cast.framework.CastSession
import com.google.android.gms.cast.framework.SessionManager
import com.google.android.gms.cast.framework.SessionManagerListener
import com.google.android.gms.cast.framework.media.RemoteMediaClient
import com.google.android.gms.common.images.WebImage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Manages Chromecast functionality for video casting
 */
class ChromecastManager(
    private val context: Context
) {
    private val castContext: CastContext? = try {
        CastContext.getSharedInstance(context)
    } catch (e: Exception) {
        null // Cast not available
    }
    
    private val sessionManager: SessionManager? = castContext?.sessionManager
    private var castSession: CastSession? = null
    private var remoteMediaClient: RemoteMediaClient? = null
    
    private val _castState = MutableStateFlow(CastState.NOT_CONNECTED)
    val castState: StateFlow<CastState> = _castState.asStateFlow()
    
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()
    
    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()
    
    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration.asStateFlow()
    
    enum class CastState {
        NOT_CONNECTED,
        CONNECTING,
        CONNECTED,
        DISCONNECTING
    }
    
    init {
        setupSessionListener()
    }
    
    private fun setupSessionListener() {
        sessionManager?.addSessionManagerListener(sessionManagerListener, CastSession::class.java)
    }
    
    private val sessionManagerListener = object : SessionManagerListener<CastSession> {
        override fun onSessionStarting(session: CastSession) {
            _castState.value = CastState.CONNECTING
        }
        
        override fun onSessionStarted(session: CastSession, sessionId: String) {
            castSession = session
            remoteMediaClient = session.remoteMediaClient
            setupRemoteMediaClientListener()
            _castState.value = CastState.CONNECTED
        }
        
        override fun onSessionStartFailed(session: CastSession, error: Int) {
            _castState.value = CastState.NOT_CONNECTED
        }
        
        override fun onSessionEnding(session: CastSession) {
            _castState.value = CastState.DISCONNECTING
        }
        
        override fun onSessionEnded(session: CastSession, error: Int) {
            castSession = null
            remoteMediaClient = null
            _castState.value = CastState.NOT_CONNECTED
            _isPlaying.value = false
            _currentPosition.value = 0L
            _duration.value = 0L
        }
        
        override fun onSessionResuming(session: CastSession, sessionId: String) {
            _castState.value = CastState.CONNECTING
        }
        
        override fun onSessionResumed(session: CastSession, wasSuspended: Boolean) {
            castSession = session
            remoteMediaClient = session.remoteMediaClient
            setupRemoteMediaClientListener()
            _castState.value = CastState.CONNECTED
        }
        
        override fun onSessionResumeFailed(session: CastSession, error: Int) {
            _castState.value = CastState.NOT_CONNECTED
        }
        
        override fun onSessionSuspended(session: CastSession, reason: Int) {
            _castState.value = CastState.NOT_CONNECTED
        }
    }
    
    private fun setupRemoteMediaClientListener() {
        remoteMediaClient?.addListener(object : RemoteMediaClient.Listener {
            override fun onStatusUpdated() {
                val mediaStatus = remoteMediaClient?.mediaStatus
                updatePlaybackState(mediaStatus)
            }
            
            override fun onMetadataUpdated() {
                // Handle metadata updates if needed
            }
            
            override fun onQueueStatusUpdated() {
                // Handle queue updates if needed
            }
            
            override fun onPreloadStatusUpdated() {
                // Handle preload status if needed
            }
            
            override fun onSendingRemoteMediaRequest() {
                // Handle sending request if needed
            }
            
            override fun onAdBreakStatusUpdated() {
                // Handle ad break status if needed
            }
        })
    }
    
    private fun updatePlaybackState(mediaStatus: MediaStatus?) {
        mediaStatus?.let { status ->
            _isPlaying.value = status.playerState == MediaStatus.PLAYER_STATE_PLAYING
            _currentPosition.value = status.streamPosition
            status.mediaInfo?.streamDuration?.let { 
                _duration.value = it.toLong()
            }
        }
    }
    
    /**
     * Cast a video to the connected device
     */
    fun castVideo(
        videoUri: Uri,
        title: String,
        subtitle: String? = null,
        thumbnailUri: Uri? = null,
        position: Long = 0L
    ) {
        val remoteMediaClient = castSession?.remoteMediaClient ?: return
        
        // Build media metadata
        val movieMetadata = MediaMetadata(MediaMetadata.MEDIA_TYPE_MOVIE).apply {
            putString(MediaMetadata.KEY_TITLE, title)
            subtitle?.let { putString(MediaMetadata.KEY_SUBTITLE, it) }
            thumbnailUri?.let {
                addImage(WebImage(it))
            }
        }
        
        // Build media info
        val mediaInfo = MediaInfo.Builder(videoUri.toString())
            .setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
            .setContentType("video/mp4") // TODO: Detect actual content type
            .setMetadata(movieMetadata)
            .build()
        
        // Build load request
        val loadRequest = MediaLoadRequestData.Builder()
            .setMediaInfo(mediaInfo)
            .setAutoplay(true)
            .setCurrentTime(position)
            .build()
        
        // Load media
        remoteMediaClient.load(loadRequest)
    }
    
    /**
     * Play/pause the current media
     */
    fun togglePlayPause() {
        remoteMediaClient?.let { client ->
            if (_isPlaying.value) {
                client.pause()
            } else {
                client.play()
            }
        }
    }
    
    /**
     * Seek to a specific position
     */
    fun seek(position: Long) {
        remoteMediaClient?.seek(position)
    }
    
    /**
     * Stop casting
     */
    fun stopCasting() {
        remoteMediaClient?.stop()
        sessionManager?.endCurrentSession(true)
    }
    
    /**
     * Get current cast session
     */
    fun getCurrentSession(): CastSession? = castSession
    
    /**
     * Check if casting is available
     */
    fun isCastAvailable(): Boolean = castContext != null
    
    /**
     * Check if currently connected to a cast device
     */
    fun isConnected(): Boolean = _castState.value == CastState.CONNECTED
    
    /**
     * Show cast dialog
     */
    fun showCastDialog() {
        // TODO: Implement cast dialog using Cast SDK
        // For now, this is a placeholder
    }
    
    /**
     * Clean up resources
     */
    fun release() {
        sessionManager?.removeSessionManagerListener(sessionManagerListener, CastSession::class.java)
        castSession = null
        remoteMediaClient = null
    }
}