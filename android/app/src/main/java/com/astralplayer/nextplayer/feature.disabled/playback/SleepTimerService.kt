package com.astralplayer.nextplayer.feature.playback

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.os.PowerManager
import kotlinx.coroutines.*

/**
 * Sleep Timer Service
 * Runs in the background to handle sleep timer functionality
 */
class SleepTimerService : Service() {
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var wakeLock: PowerManager.WakeLock? = null
    
    companion object {
        const val ACTION_START_TIMER = "com.astralplayer.nextplayer.START_SLEEP_TIMER"
        const val ACTION_CANCEL_TIMER = "com.astralplayer.nextplayer.CANCEL_SLEEP_TIMER"
        const val EXTRA_DURATION_MILLIS = "duration_millis"
        const val EXTRA_FADE_OUT = "fade_out_enabled"
    }
    
    override fun onCreate() {
        super.onCreate()
        
        // Acquire wake lock to ensure timer runs even when screen is off
        val powerManager = getSystemService(POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "AstralPlayer::SleepTimer"
        )
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_TIMER -> {
                val duration = intent.getLongExtra(EXTRA_DURATION_MILLIS, 0)
                val fadeOut = intent.getBooleanExtra(EXTRA_FADE_OUT, true)
                if (duration > 0) {
                    startTimer(duration, fadeOut)
                }
            }
            ACTION_CANCEL_TIMER -> {
                cancelTimer()
                stopSelf()
            }
        }
        
        return START_NOT_STICKY
    }
    
    private fun startTimer(durationMillis: Long, fadeOut: Boolean) {
        wakeLock?.acquire(durationMillis + 10000) // Add 10 seconds buffer
        
        scope.launch {
            delay(durationMillis)
            
            // Send broadcast to stop playback
            val stopIntent = Intent("com.astralplayer.nextplayer.SLEEP_TIMER_EXPIRED")
            stopIntent.putExtra("fade_out", fadeOut)
            sendBroadcast(stopIntent)
            
            stopSelf()
        }
    }
    
    private fun cancelTimer() {
        scope.coroutineContext.cancelChildren()
        wakeLock?.release()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
        wakeLock?.release()
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
}