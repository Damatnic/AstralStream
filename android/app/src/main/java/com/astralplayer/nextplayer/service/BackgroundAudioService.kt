package com.astralplayer.nextplayer.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.core.app.NotificationCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.astralplayer.nextplayer.R
import com.astralplayer.nextplayer.VideoPlayerActivity
import kotlinx.coroutines.*

@UnstableApi
class BackgroundAudioService : Service() {
    
    companion object {
        const val ACTION_PLAY = "com.astralplayer.nextplayer.ACTION_PLAY"
        const val ACTION_PAUSE = "com.astralplayer.nextplayer.ACTION_PAUSE"
        const val ACTION_NEXT = "com.astralplayer.nextplayer.ACTION_NEXT"
        const val ACTION_PREVIOUS = "com.astralplayer.nextplayer.ACTION_PREVIOUS"
        const val ACTION_STOP = "com.astralplayer.nextplayer.ACTION_STOP"
        
        const val EXTRA_VIDEO_URI = "video_uri"
        const val EXTRA_VIDEO_TITLE = "video_title"
        const val EXTRA_POSITION = "position"
        
        const val NOTIFICATION_ID = 1001
        const val CHANNEL_ID = "background_audio_channel"
    }
    
    private lateinit var exoPlayer: ExoPlayer
    private lateinit var mediaSession: MediaSessionCompat
    private lateinit var notificationManager: NotificationManager
    
    private val binder = LocalBinder()
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
    private var currentVideoUri: Uri? = null
    private var currentVideoTitle: String = "Unknown"
    private var currentThumbnail: Bitmap? = null
    
    inner class LocalBinder : Binder() {
        fun getService(): BackgroundAudioService = this@BackgroundAudioService
    }
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize ExoPlayer
        exoPlayer = ExoPlayer.Builder(this).build()
        
        // Initialize MediaSession
        mediaSession = MediaSessionCompat(this, "BackgroundAudioService").apply {
            setFlags(
                MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or
                MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS
            )
            setCallback(mediaSessionCallback)
            isActive = true
        }
        
        // Initialize notification manager
        notificationManager = getSystemService(NotificationManager::class.java)
        createNotificationChannel()
        
        // Add player listener
        exoPlayer.addListener(playerListener)
    }
    
    override fun onBind(intent: Intent?): IBinder {
        return binder
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let { handleIntent(it) }
        return START_STICKY
    }
    
    private fun handleIntent(intent: Intent) {
        when (intent.action) {
            ACTION_PLAY -> play()
            ACTION_PAUSE -> pause()
            ACTION_NEXT -> next()
            ACTION_PREVIOUS -> previous()
            ACTION_STOP -> stopSelf()
            else -> {
                // Start playback with new video
                intent.getStringExtra(EXTRA_VIDEO_URI)?.let { uriString ->
                    val uri = Uri.parse(uriString)
                    val title = intent.getStringExtra(EXTRA_VIDEO_TITLE) ?: "Unknown"
                    val position = intent.getLongExtra(EXTRA_POSITION, 0L)
                    startPlayback(uri, title, position)
                }
            }
        }
    }
    
    private fun startPlayback(uri: Uri, title: String, position: Long = 0L) {
        currentVideoUri = uri
        currentVideoTitle = title
        
        // Load video thumbnail asynchronously
        scope.launch {
            // TODO: Implement thumbnail loading
            updateNotification()
        }
        
        // Prepare player
        val mediaItem = MediaItem.fromUri(uri)
        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()
        exoPlayer.seekTo(position)
        exoPlayer.playWhenReady = true
        
        // Start foreground service
        startForeground(NOTIFICATION_ID, createNotification())
        updateMediaSession()
    }
    
    private fun play() {
        exoPlayer.play()
        updateNotification()
        updateMediaSession()
    }
    
    private fun pause() {
        exoPlayer.pause()
        updateNotification()
        updateMediaSession()
    }
    
    private fun next() {
        // TODO: Implement playlist navigation
        val currentPosition = exoPlayer.currentPosition
        val newPosition = (currentPosition + 10000).coerceAtMost(exoPlayer.duration)
        exoPlayer.seekTo(newPosition)
    }
    
    private fun previous() {
        // TODO: Implement playlist navigation
        val currentPosition = exoPlayer.currentPosition
        val newPosition = (currentPosition - 10000).coerceAtLeast(0)
        exoPlayer.seekTo(newPosition)
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Background Audio Playback",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Controls for background audio playback"
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    private fun createNotification(): Notification {
        val intent = Intent(this, VideoPlayerActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            currentVideoUri?.let { putExtra(EXTRA_VIDEO_URI, it.toString()) }
        }
        
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val playPauseAction = if (exoPlayer.isPlaying) {
            NotificationCompat.Action(
                R.drawable.ic_pause,
                "Pause",
                createActionPendingIntent(ACTION_PAUSE)
            )
        } else {
            NotificationCompat.Action(
                R.drawable.ic_play,
                "Play",
                createActionPendingIntent(ACTION_PLAY)
            )
        }
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(currentVideoTitle)
            .setContentText("Background playback active")
            .setSmallIcon(R.drawable.ic_play)
            .setLargeIcon(currentThumbnail)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setShowWhen(false)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setStyle(
                androidx.media.app.NotificationCompat.MediaStyle()
                    .setMediaSession(mediaSession.sessionToken)
                    .setShowActionsInCompactView(0, 1, 2)
            )
            .addAction(
                R.drawable.ic_skip_previous,
                "Previous",
                createActionPendingIntent(ACTION_PREVIOUS)
            )
            .addAction(playPauseAction)
            .addAction(
                R.drawable.ic_skip_next,
                "Next",
                createActionPendingIntent(ACTION_NEXT)
            )
            .build()
    }
    
    private fun createActionPendingIntent(action: String): PendingIntent {
        val intent = Intent(this, BackgroundAudioService::class.java).apply {
            this.action = action
        }
        return PendingIntent.getService(
            this,
            action.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
    
    private fun updateNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            notificationManager.notify(NOTIFICATION_ID, createNotification())
        }
    }
    
    private fun updateMediaSession() {
        val state = PlaybackStateCompat.Builder()
            .setState(
                if (exoPlayer.isPlaying) PlaybackStateCompat.STATE_PLAYING
                else PlaybackStateCompat.STATE_PAUSED,
                exoPlayer.currentPosition,
                exoPlayer.playbackParameters.speed
            )
            .setActions(
                PlaybackStateCompat.ACTION_PLAY or
                PlaybackStateCompat.ACTION_PAUSE or
                PlaybackStateCompat.ACTION_PLAY_PAUSE or
                PlaybackStateCompat.ACTION_SEEK_TO or
                PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
            )
            .build()
        
        mediaSession.setPlaybackState(state)
        
        val metadata = MediaMetadataCompat.Builder()
            .putString(MediaMetadataCompat.METADATA_KEY_TITLE, currentVideoTitle)
            .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, exoPlayer.duration)
            .apply {
                currentThumbnail?.let {
                    putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, it)
                }
            }
            .build()
        
        mediaSession.setMetadata(metadata)
    }
    
    private val playerListener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            updateNotification()
            updateMediaSession()
        }
        
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            updateNotification()
            updateMediaSession()
        }
    }
    
    private val mediaSessionCallback = object : MediaSessionCompat.Callback() {
        override fun onPlay() {
            play()
        }
        
        override fun onPause() {
            pause()
        }
        
        override fun onSkipToNext() {
            next()
        }
        
        override fun onSkipToPrevious() {
            previous()
        }
        
        override fun onSeekTo(pos: Long) {
            exoPlayer.seekTo(pos)
        }
    }
    
    fun getPlayer(): ExoPlayer = exoPlayer
    
    fun getCurrentPosition(): Long = exoPlayer.currentPosition
    
    fun getDuration(): Long = exoPlayer.duration
    
    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
        mediaSession.release()
        exoPlayer.release()
    }
}