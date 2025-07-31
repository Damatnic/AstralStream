package com.astralplayer.nextplayer.feature.cast

import android.content.Context
import com.google.android.gms.cast.CastMediaControlIntent
import com.google.android.gms.cast.LaunchOptions
import com.google.android.gms.cast.framework.CastOptions
import com.google.android.gms.cast.framework.OptionsProvider
import com.google.android.gms.cast.framework.SessionProvider
import com.google.android.gms.cast.framework.media.CastMediaOptions
import com.google.android.gms.cast.framework.media.MediaIntentReceiver
import com.google.android.gms.cast.framework.media.NotificationOptions

/**
 * Provides Cast options for the AstralStream application
 * Configures Chromecast support for video streaming
 */
class CastOptionsProvider : OptionsProvider {
    
    override fun getCastOptions(context: Context): CastOptions {
        // Configure notification options for media controls
        val notificationOptions = NotificationOptions.Builder()
            .setActions(
                listOf(
                    MediaIntentReceiver.ACTION_SKIP_PREV,
                    MediaIntentReceiver.ACTION_TOGGLE_PLAYBACK,
                    MediaIntentReceiver.ACTION_SKIP_NEXT,
                    MediaIntentReceiver.ACTION_STOP_CASTING
                ),
                intArrayOf(0, 1, 2, 3)
            )
            .setTargetActivityClassName("com.astralplayer.nextplayer.VideoPlayerActivity")
            .build()
        
        // Configure media options
        val mediaOptions = CastMediaOptions.Builder()
            .setNotificationOptions(notificationOptions)
            .setExpandedControllerActivityClassName("com.astralplayer.nextplayer.VideoPlayerActivity")
            .build()
        
        // Configure launch options for compatibility
        val launchOptions = LaunchOptions.Builder()
            .setAndroidReceiverCompatible(true)
            .build()
        
        // Build and return Cast options
        return CastOptions.Builder()
            .setReceiverApplicationId(CastMediaControlIntent.DEFAULT_MEDIA_RECEIVER_APPLICATION_ID)
            .setLaunchOptions(launchOptions)
            .setCastMediaOptions(mediaOptions)
            .setEnableReconnectionService(true)
            .setResumeSavedSession(true)
            .build()
    }
    
    override fun getAdditionalSessionProviders(context: Context): List<SessionProvider>? {
        // No additional session providers needed for basic Cast support
        return null
    }
}