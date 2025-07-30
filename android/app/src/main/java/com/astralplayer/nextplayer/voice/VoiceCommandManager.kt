package com.astralplayer.nextplayer.voice

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Manages voice commands for video player control
 */
class VoiceCommandManager(private val context: Context) {
    
    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()
    
    private val _lastCommand = MutableStateFlow<VoiceCommand?>(null)
    val lastCommand: StateFlow<VoiceCommand?> = _lastCommand.asStateFlow()
    
    private var commandExecutor: VoiceCommandExecutor? = null
    
    fun setCommandExecutor(executor: VoiceCommandExecutor) {
        commandExecutor = executor
    }
    
    fun startListening() {
        _isListening.value = true
    }
    
    fun stopListening() {
        _isListening.value = false
    }
    
    fun processVoiceInput(text: String) {
        val command = parseVoiceCommand(text.lowercase())
        if (command != null) {
            _lastCommand.value = command
            commandExecutor?.executeVoiceCommand(command)
        }
    }
    
    private fun parseVoiceCommand(text: String): VoiceCommand? {
        return when {
            text.contains("play") && !text.contains("pause") -> VoiceCommand.PLAY
            text.contains("pause") || text.contains("stop") -> VoiceCommand.PAUSE
            text.contains("next") || text.contains("skip") -> VoiceCommand.NEXT
            text.contains("previous") || text.contains("back") -> VoiceCommand.PREVIOUS
            text.contains("faster") || text.contains("speed up") -> VoiceCommand.SPEED_UP
            text.contains("slower") || text.contains("slow down") -> VoiceCommand.SLOW_DOWN
            text.contains("normal speed") -> VoiceCommand.NORMAL_SPEED
            text.contains("volume up") || text.contains("louder") -> VoiceCommand.VOLUME_UP
            text.contains("volume down") || text.contains("quieter") -> VoiceCommand.VOLUME_DOWN
            text.contains("mute") -> VoiceCommand.MUTE
            text.contains("subtitles on") -> VoiceCommand.SUBTITLES_ON
            text.contains("subtitles off") -> VoiceCommand.SUBTITLES_OFF
            text.contains("bookmark") -> VoiceCommand.ADD_BOOKMARK
            text.contains("repeat") -> VoiceCommand.TOGGLE_REPEAT
            else -> null
        }
    }
    
    fun getAvailableCommands(): List<String> {
        return listOf(
            "Play", "Pause", "Stop", "Next", "Previous", "Skip",
            "Faster", "Speed up", "Slower", "Slow down", "Normal speed",
            "Volume up", "Volume down", "Mute",
            "Subtitles on", "Subtitles off",
            "Bookmark", "Repeat"
        )
    }
}

enum class VoiceCommand(val displayName: String) {
    PLAY("Play"),
    PAUSE("Pause"),
    NEXT("Next"),
    PREVIOUS("Previous"),
    SPEED_UP("Speed Up"),
    SLOW_DOWN("Slow Down"),
    NORMAL_SPEED("Normal Speed"),
    VOLUME_UP("Volume Up"),
    VOLUME_DOWN("Volume Down"),
    MUTE("Mute"),
    SUBTITLES_ON("Subtitles On"),
    SUBTITLES_OFF("Subtitles Off"),
    ADD_BOOKMARK("Add Bookmark"),
    TOGGLE_REPEAT("Toggle Repeat")
}

interface VoiceCommandExecutor {
    fun executeVoiceCommand(command: VoiceCommand)
}