package com.astralplayer.nextplayer.feature.player.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.localbroadcastmanager.content.LocalBroadcastManager

/**
 * Broadcast receiver for handling notification actions
 */
class NotificationActionReceiver : BroadcastReceiver() {
    
    companion object {
        const val BROADCAST_ACTION = "com.astralplayer.nextplayer.NOTIFICATION_ACTION"
        const val EXTRA_ACTION_TYPE = "action_type"
    }
    
    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return
        
        val action = intent.action ?: return
        
        // Forward the action to the player via local broadcast
        val localIntent = Intent(BROADCAST_ACTION).apply {
            putExtra(EXTRA_ACTION_TYPE, action)
        }
        
        LocalBroadcastManager.getInstance(context).sendBroadcast(localIntent)
        
        // Handle specific actions
        when (action) {
            CustomNotificationManager.ACTION_PLAY_PAUSE -> {
                handlePlayPause(context)
            }
            CustomNotificationManager.ACTION_PREVIOUS -> {
                handlePrevious(context)
            }
            CustomNotificationManager.ACTION_NEXT -> {
                handleNext(context)
            }
            CustomNotificationManager.ACTION_STOP -> {
                handleStop(context)
            }
            CustomNotificationManager.ACTION_SEEK_FORWARD -> {
                handleSeekForward(context)
            }
            CustomNotificationManager.ACTION_SEEK_BACKWARD -> {
                handleSeekBackward(context)
            }
            CustomNotificationManager.ACTION_REPEAT -> {
                handleRepeat(context)
            }
            CustomNotificationManager.ACTION_SHUFFLE -> {
                handleShuffle(context)
            }
        }
    }
    
    private fun handlePlayPause(context: Context) {
        // Send play/pause command to player
        sendPlayerCommand(context, "PLAY_PAUSE")
    }
    
    private fun handlePrevious(context: Context) {
        // Send previous command to player
        sendPlayerCommand(context, "PREVIOUS")
    }
    
    private fun handleNext(context: Context) {
        // Send next command to player
        sendPlayerCommand(context, "NEXT")
    }
    
    private fun handleStop(context: Context) {
        // Send stop command to player
        sendPlayerCommand(context, "STOP")
    }
    
    private fun handleSeekForward(context: Context) {
        // Send seek forward command to player (10 seconds)
        sendPlayerCommand(context, "SEEK_FORWARD", 10000L)
    }
    
    private fun handleSeekBackward(context: Context) {
        // Send seek backward command to player (10 seconds)
        sendPlayerCommand(context, "SEEK_BACKWARD", 10000L)
    }
    
    private fun handleRepeat(context: Context) {
        // Send repeat toggle command to player
        sendPlayerCommand(context, "TOGGLE_REPEAT")
    }
    
    private fun handleShuffle(context: Context) {
        // Send shuffle toggle command to player
        sendPlayerCommand(context, "TOGGLE_SHUFFLE")
    }
    
    private fun sendPlayerCommand(context: Context, command: String, value: Long = 0L) {
        val intent = Intent("com.astralplayer.nextplayer.PLAYER_COMMAND").apply {
            putExtra("command", command)
            putExtra("value", value)
        }
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
    }
}