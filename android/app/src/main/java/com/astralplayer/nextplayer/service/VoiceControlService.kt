package com.astralplayer.nextplayer.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.astralplayer.nextplayer.voice.VoiceControlManager
import kotlinx.coroutines.*

/**
 * Voice Control Service
 * Background service for continuous voice command listening
 */
class VoiceControlService : Service() {
    private var voiceControlManager: VoiceControlManager? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
    companion object {
        const val ACTION_START_LISTENING = "com.astralplayer.nextplayer.START_VOICE_CONTROL"
        const val ACTION_STOP_LISTENING = "com.astralplayer.nextplayer.STOP_VOICE_CONTROL"
    }
    
    override fun onCreate() {
        super.onCreate()
        voiceControlManager = VoiceControlManager(this)
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_LISTENING -> {
                startListening()
            }
            ACTION_STOP_LISTENING -> {
                stopListening()
                stopSelf()
            }
        }
        
        return START_STICKY
    }
    
    private fun startListening() {
        scope.launch {
            voiceControlManager?.startListening(
                continuous = true,
                onCommand = { command ->
                    // Send broadcast with recognized command
                    val commandIntent = Intent("com.astralplayer.nextplayer.VOICE_COMMAND")
                    commandIntent.putExtra("command_type", command.type.name)
                    commandIntent.putExtra("raw_text", command.rawText)
                    commandIntent.putExtra("confidence", command.confidence)
                    sendBroadcast(commandIntent)
                }
            )
        }
    }
    
    private fun stopListening() {
        voiceControlManager?.stopListening()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
        voiceControlManager?.release()
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
}