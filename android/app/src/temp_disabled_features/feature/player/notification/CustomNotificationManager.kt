package com.astralplayer.nextplayer.feature.player.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.support.v4.media.MediaMetadataCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaStyleNotificationHelper
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import com.astralplayer.nextplayer.R
import com.astralplayer.nextplayer.VideoPlayerActivity

/**
 * Custom notification manager for video player controls
 */
class CustomNotificationManager(private val context: Context) {
    
    companion object {
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "astral_player_channel"
        private const val CHANNEL_NAME = "Astral Player"
        private const val CHANNEL_DESCRIPTION = "Media playback controls"
        
        // Action IDs
        const val ACTION_PLAY_PAUSE = "action_play_pause"
        const val ACTION_PREVIOUS = "action_previous"
        const val ACTION_NEXT = "action_next"
        const val ACTION_STOP = "action_stop"
        const val ACTION_SEEK_FORWARD = "action_seek_forward"
        const val ACTION_SEEK_BACKWARD = "action_seek_backward"
        const val ACTION_REPEAT = "action_repeat"
        const val ACTION_SHUFFLE = "action_shuffle"
    }
    
    private val notificationManager = NotificationManagerCompat.from(context)
    private var mediaSession: MediaSessionCompat? = null
    private var currentNotification: Notification? = null
    
    init {
        createNotificationChannel()
        initializeMediaSession()
    }
    
    /**
     * Create notification channel for Android O and above
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = CHANNEL_DESCRIPTION
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }
    
    /**
     * Initialize media session for notification controls
     */
    private fun initializeMediaSession() {
        mediaSession = MediaSessionCompat(context, "AstralPlayer").apply {
            setFlags(
                MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or
                MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS
            )
            isActive = true
        }
    }
    
    /**
     * Create and show custom notification
     */
    fun showNotification(
        title: String,
        artist: String = "",
        album: String = "",
        artwork: Bitmap? = null,
        isPlaying: Boolean = false,
        position: Long = 0,
        duration: Long = 0,
        canSeekForward: Boolean = true,
        canSeekBackward: Boolean = true,
        hasNext: Boolean = false,
        hasPrevious: Boolean = false,
        repeatMode: Int = Player.REPEAT_MODE_OFF,
        shuffleMode: Boolean = false
    ) {
        val notification = buildNotification(
            title = title,
            artist = artist,
            album = album,
            artwork = artwork,
            isPlaying = isPlaying,
            position = position,
            duration = duration,
            canSeekForward = canSeekForward,
            canSeekBackward = canSeekBackward,
            hasNext = hasNext,
            hasPrevious = hasPrevious,
            repeatMode = repeatMode,
            shuffleMode = shuffleMode
        )
        
        currentNotification = notification
        notificationManager.notify(NOTIFICATION_ID, notification)
    }
    
    /**
     * Build the notification with custom controls
     */
    private fun buildNotification(
        title: String,
        artist: String,
        album: String,
        artwork: Bitmap?,
        isPlaying: Boolean,
        position: Long,
        duration: Long,
        canSeekForward: Boolean,
        canSeekBackward: Boolean,
        hasNext: Boolean,
        hasPrevious: Boolean,
        repeatMode: Int,
        shuffleMode: Boolean
    ): Notification {
        
        // Create pending intent for opening the app
        val openAppIntent = Intent(context, VideoPlayerActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            context, 0, openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // Create action pending intents
        val playPauseIntent = createActionIntent(ACTION_PLAY_PAUSE)
        val previousIntent = createActionIntent(ACTION_PREVIOUS)
        val nextIntent = createActionIntent(ACTION_NEXT)
        val stopIntent = createActionIntent(ACTION_STOP)
        val seekForwardIntent = createActionIntent(ACTION_SEEK_FORWARD)
        val seekBackwardIntent = createActionIntent(ACTION_SEEK_BACKWARD)
        val repeatIntent = createActionIntent(ACTION_REPEAT)
        val shuffleIntent = createActionIntent(ACTION_SHUFFLE)
        
        // Build notification
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle(title)
            .setContentText(if (artist.isNotEmpty()) artist else "Astral Player")
            .setSubText(album)
            .setLargeIcon(artwork ?: getDefaultArtwork())
            .setContentIntent(openAppPendingIntent)
            .setDeleteIntent(stopIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
            .setPriority(NotificationCompat.PRIORITY_LOW)
        
        // Add media style with session token
        mediaSession?.let { session ->
            builder.setStyle(
                androidx.media.app.NotificationCompat.MediaStyle()
                    .setMediaSession(session.sessionToken)
                    .setShowActionsInCompactView(0, 1, 2) // Previous, Play/Pause, Next
                    .setShowCancelButton(true)
                    .setCancelButtonIntent(stopIntent)
            )
        }
        
        // Add actions based on capabilities
        if (hasPrevious) {
            builder.addAction(
                android.R.drawable.ic_media_previous,
                "Previous",
                previousIntent
            )
        }
        
        // Seek backward (10s)
        if (canSeekBackward) {
            builder.addAction(
                android.R.drawable.ic_media_rew,
                "Seek -10s",
                seekBackwardIntent
            )
        }
        
        // Play/Pause
        builder.addAction(
            if (isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play,
            if (isPlaying) "Pause" else "Play",
            playPauseIntent
        )
        
        // Seek forward (10s)
        if (canSeekForward) {
            builder.addAction(
                android.R.drawable.ic_media_ff,
                "Seek +10s",
                seekForwardIntent
            )
        }
        
        if (hasNext) {
            builder.addAction(
                android.R.drawable.ic_media_next,
                "Next",
                nextIntent
            )
        }
        
        // Additional controls in expanded view
        builder.addAction(
            getRepeatIcon(repeatMode),
            getRepeatDescription(repeatMode),
            repeatIntent
        )
        
        builder.addAction(
            android.R.drawable.ic_menu_sort_by_size, // Using generic icon for shuffle
            if (shuffleMode) "Shuffle On" else "Shuffle Off",
            shuffleIntent
        )
        
        // Add progress bar if duration is available
        if (duration > 0) {
            builder.setProgress(duration.toInt(), position.toInt(), false)
        }
        
        // Update media session metadata
        updateMediaSessionMetadata(title, artist, album, artwork, duration)
        
        return builder.build()
    }
    
    /**
     * Create action pending intent
     */
    private fun createActionIntent(action: String): PendingIntent {
        val intent = Intent(context, NotificationActionReceiver::class.java).apply {
            this.action = action
        }
        return PendingIntent.getBroadcast(
            context,
            action.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
    
    /**
     * Get repeat mode icon
     */
    private fun getRepeatIcon(repeatMode: Int): Int {
        return when (repeatMode) {
            Player.REPEAT_MODE_ONE -> android.R.drawable.ic_menu_revert
            Player.REPEAT_MODE_ALL -> android.R.drawable.ic_menu_rotate
            else -> android.R.drawable.ic_menu_close_clear_cancel
        }
    }
    
    /**
     * Get repeat mode description
     */
    private fun getRepeatDescription(repeatMode: Int): String {
        return when (repeatMode) {
            Player.REPEAT_MODE_ONE -> "Repeat One"
            Player.REPEAT_MODE_ALL -> "Repeat All"
            else -> "Repeat Off"
        }
    }
    
    /**
     * Get default artwork bitmap
     */
    private fun getDefaultArtwork(): Bitmap {
        // Create a simple colored bitmap as fallback
        val bitmap = Bitmap.createBitmap(200, 200, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(android.graphics.Color.parseColor("#00BCD4"))
        return bitmap
    }
    
    /**
     * Update media session metadata
     */
    private fun updateMediaSessionMetadata(
        title: String,
        artist: String,
        album: String,
        artwork: Bitmap?,
        duration: Long
    ) {
        mediaSession?.setMetadata(
            MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, title)
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, artist)
                .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, album)
                .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, duration)
                .putBitmap(MediaMetadataCompat.METADATA_KEY_ART, artwork)
                .build()
        )
    }
    
    /**
     * Update notification with new playback state
     */
    fun updatePlaybackState(
        isPlaying: Boolean,
        position: Long = 0,
        playbackSpeed: Float = 1.0f
    ) {
        mediaSession?.setPlaybackState(
            PlaybackStateCompat.Builder()
                .setState(
                    if (isPlaying) PlaybackStateCompat.STATE_PLAYING
                    else PlaybackStateCompat.STATE_PAUSED,
                    position,
                    playbackSpeed
                )
                .setActions(
                    PlaybackStateCompat.ACTION_PLAY or
                    PlaybackStateCompat.ACTION_PAUSE or
                    PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                    PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
                    PlaybackStateCompat.ACTION_SEEK_TO or
                    PlaybackStateCompat.ACTION_STOP
                )
                .build()
        )
    }
    
    /**
     * Hide notification
     */
    fun hideNotification() {
        notificationManager.cancel(NOTIFICATION_ID)
        currentNotification = null
    }
    
    /**
     * Set media session callback
     */
    fun setMediaSessionCallback(callback: MediaSessionCompat.Callback) {
        mediaSession?.setCallback(callback)
    }
    
    /**
     * Get current notification
     */
    fun getCurrentNotification(): Notification? = currentNotification
    
    /**
     * Check if notification is showing
     */
    fun isNotificationShowing(): Boolean = currentNotification != null
    
    /**
     * Release resources
     */
    fun release() {
        hideNotification()
        mediaSession?.release()
        mediaSession = null
    }
}