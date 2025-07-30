package com.astralplayer.nextplayer.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import androidx.core.app.NotificationCompat

/**
 * Manages rich media notifications with playback controls
 */
class MediaNotificationManager(private val context: Context) {
    
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    
    init {
        createNotificationChannel()
    }
    
    fun createNotification(
        title: String,
        artist: String = "",
        albumArt: Bitmap? = null,
        isPlaying: Boolean = false
    ): Notification {
        
        val playPauseAction = if (isPlaying) {
            NotificationCompat.Action.Builder(
                android.R.drawable.ic_media_pause,
                "Pause",
                createPendingIntent(ACTION_PAUSE)
            ).build()
        } else {
            NotificationCompat.Action.Builder(
                android.R.drawable.ic_media_play,
                "Play", 
                createPendingIntent(ACTION_PLAY)
            ).build()
        }
        
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(artist)
            .setLargeIcon(albumArt)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .addAction(
                NotificationCompat.Action.Builder(
                    android.R.drawable.ic_media_previous,
                    "Previous",
                    createPendingIntent(ACTION_PREVIOUS)
                ).build()
            )
            .addAction(playPauseAction)
            .addAction(
                NotificationCompat.Action.Builder(
                    android.R.drawable.ic_media_next,
                    "Next",
                    createPendingIntent(ACTION_NEXT)
                ).build()
            )
            .setContentIntent(createContentIntent())
            .setDeleteIntent(createPendingIntent(ACTION_STOP))
            .setOngoing(isPlaying)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Media Playback",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Media playback controls"
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    private fun createPendingIntent(action: String): PendingIntent {
        val intent = Intent(action).apply {
            setPackage(context.packageName)
        }
        return PendingIntent.getBroadcast(
            context,
            action.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
    
    private fun createContentIntent(): PendingIntent {
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        return PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
    
    companion object {
        const val CHANNEL_ID = "media_playback"
        const val NOTIFICATION_ID = 1001
        
        const val ACTION_PLAY = "com.astralplayer.nextplayer.PLAY"
        const val ACTION_PAUSE = "com.astralplayer.nextplayer.PAUSE"
        const val ACTION_NEXT = "com.astralplayer.nextplayer.NEXT"
        const val ACTION_PREVIOUS = "com.astralplayer.nextplayer.PREVIOUS"
        const val ACTION_FORWARD = "com.astralplayer.nextplayer.FORWARD"
        const val ACTION_REWIND = "com.astralplayer.nextplayer.REWIND"
        const val ACTION_STOP = "com.astralplayer.nextplayer.STOP"
    }
}