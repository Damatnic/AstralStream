package com.astralplayer.nextplayer.gesture

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton
import java.util.Locale

/**
 * Handles voice commands for gesture control
 */
@Singleton
class VoiceCommandHandler @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false
    
    private val _voiceCommandState = MutableStateFlow(VoiceCommandState.IDLE)
    val voiceCommandState: StateFlow<VoiceCommandState> = _voiceCommandState
    
    private val _lastCommand = MutableStateFlow<VoiceCommand?>(null)
    val lastCommand: StateFlow<VoiceCommand?> = _lastCommand
    
    private val recognitionListener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            _voiceCommandState.value = VoiceCommandState.READY
        }
        
        override fun onBeginningOfSpeech() {
            _voiceCommandState.value = VoiceCommandState.LISTENING
        }
        
        override fun onRmsChanged(rmsdB: Float) {}
        
        override fun onBufferReceived(buffer: ByteArray?) {}
        
        override fun onEndOfSpeech() {
            _voiceCommandState.value = VoiceCommandState.PROCESSING
        }
        
        override fun onError(error: Int) {
            _voiceCommandState.value = VoiceCommandState.ERROR(getErrorMessage(error))
            isListening = false
        }
        
        override fun onResults(results: Bundle?) {
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            if (!matches.isNullOrEmpty()) {
                processVoiceCommand(matches[0])
            }
            _voiceCommandState.value = VoiceCommandState.IDLE
            isListening = false
        }
        
        override fun onPartialResults(partialResults: Bundle?) {
            val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            if (!matches.isNullOrEmpty()) {
                _voiceCommandState.value = VoiceCommandState.PARTIAL_RESULT(matches[0])
            }
        }
        
        override fun onEvent(eventType: Int, params: Bundle?) {}
    }
    
    fun initialize() {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
            speechRecognizer?.setRecognitionListener(recognitionListener)
        }
    }
    
    fun startListening() {
        if (isListening || speechRecognizer == null) return
        
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
        
        speechRecognizer?.startListening(intent)
        isListening = true
        _voiceCommandState.value = VoiceCommandState.INITIALIZING
    }
    
    fun stopListening() {
        if (!isListening) return
        
        speechRecognizer?.stopListening()
        isListening = false
        _voiceCommandState.value = VoiceCommandState.IDLE
    }
    
    private fun processVoiceCommand(command: String) {
        val lowerCommand = command.toLowerCase(Locale.getDefault())
        
        val voiceCommand = when {
            // Playback commands
            lowerCommand.contains("play") -> VoiceCommand.PLAY
            lowerCommand.contains("pause") -> VoiceCommand.PAUSE
            lowerCommand.contains("stop") -> VoiceCommand.STOP
            
            // Navigation commands
            lowerCommand.contains("next") -> VoiceCommand.NEXT
            lowerCommand.contains("previous") || lowerCommand.contains("back") -> VoiceCommand.PREVIOUS
            
            // Seek commands
            lowerCommand.contains("forward") -> {
                val seconds = extractSeconds(lowerCommand)
                VoiceCommand.SEEK_FORWARD(seconds ?: 10)
            }
            lowerCommand.contains("rewind") || lowerCommand.contains("backward") -> {
                val seconds = extractSeconds(lowerCommand)
                VoiceCommand.SEEK_BACKWARD(seconds ?: 10)
            }
            
            // Volume commands
            lowerCommand.contains("volume up") || lowerCommand.contains("louder") -> VoiceCommand.VOLUME_UP
            lowerCommand.contains("volume down") || lowerCommand.contains("quieter") -> VoiceCommand.VOLUME_DOWN
            lowerCommand.contains("mute") -> VoiceCommand.MUTE
            lowerCommand.contains("unmute") -> VoiceCommand.UNMUTE
            
            // Speed commands
            lowerCommand.contains("faster") -> VoiceCommand.SPEED_UP
            lowerCommand.contains("slower") -> VoiceCommand.SPEED_DOWN
            lowerCommand.contains("normal speed") -> VoiceCommand.NORMAL_SPEED
            
            // Screen commands
            lowerCommand.contains("fullscreen") || lowerCommand.contains("full screen") -> VoiceCommand.FULLSCREEN
            lowerCommand.contains("exit fullscreen") -> VoiceCommand.EXIT_FULLSCREEN
            
            // Subtitle commands
            lowerCommand.contains("subtitles on") || lowerCommand.contains("show subtitles") -> VoiceCommand.SUBTITLES_ON
            lowerCommand.contains("subtitles off") || lowerCommand.contains("hide subtitles") -> VoiceCommand.SUBTITLES_OFF
            
            // Custom gesture commands
            lowerCommand.contains("record gesture") -> VoiceCommand.RECORD_GESTURE
            lowerCommand.contains("save gesture") -> VoiceCommand.SAVE_GESTURE
            lowerCommand.contains("cancel gesture") -> VoiceCommand.CANCEL_GESTURE
            
            else -> VoiceCommand.UNKNOWN(command)
        }
        
        _lastCommand.value = voiceCommand
    }
    
    private fun extractSeconds(command: String): Int? {
        val regex = """(\d+)\s*(second|seconds|sec)""".toRegex()
        val match = regex.find(command)
        return match?.groupValues?.getOrNull(1)?.toIntOrNull()
    }
    
    private fun getErrorMessage(errorCode: Int): String {
        return when (errorCode) {
            SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
            SpeechRecognizer.ERROR_CLIENT -> "Client side error"
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
            SpeechRecognizer.ERROR_NETWORK -> "Network error"
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
            SpeechRecognizer.ERROR_NO_MATCH -> "No match found"
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognition service busy"
            SpeechRecognizer.ERROR_SERVER -> "Server error"
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech input"
            else -> "Unknown error"
        }
    }
    
    fun destroy() {
        speechRecognizer?.destroy()
        speechRecognizer = null
    }
    
    sealed class VoiceCommandState {
        object IDLE : VoiceCommandState()
        object INITIALIZING : VoiceCommandState()
        object READY : VoiceCommandState()
        object LISTENING : VoiceCommandState()
        object PROCESSING : VoiceCommandState()
        data class PARTIAL_RESULT(val text: String) : VoiceCommandState()
        data class ERROR(val message: String) : VoiceCommandState()
    }
    
    sealed class VoiceCommand {
        // Playback commands
        object PLAY : VoiceCommand()
        object PAUSE : VoiceCommand()
        object STOP : VoiceCommand()
        object NEXT : VoiceCommand()
        object PREVIOUS : VoiceCommand()
        
        // Seek commands
        data class SEEK_FORWARD(val seconds: Int) : VoiceCommand()
        data class SEEK_BACKWARD(val seconds: Int) : VoiceCommand()
        
        // Volume commands
        object VOLUME_UP : VoiceCommand()
        object VOLUME_DOWN : VoiceCommand()
        object MUTE : VoiceCommand()
        object UNMUTE : VoiceCommand()
        
        // Speed commands
        object SPEED_UP : VoiceCommand()
        object SPEED_DOWN : VoiceCommand()
        object NORMAL_SPEED : VoiceCommand()
        
        // Screen commands
        object FULLSCREEN : VoiceCommand()
        object EXIT_FULLSCREEN : VoiceCommand()
        
        // Subtitle commands
        object SUBTITLES_ON : VoiceCommand()
        object SUBTITLES_OFF : VoiceCommand()
        
        // Gesture commands
        object RECORD_GESTURE : VoiceCommand()
        object SAVE_GESTURE : VoiceCommand()
        object CANCEL_GESTURE : VoiceCommand()
        
        // Unknown command
        data class UNKNOWN(val command: String) : VoiceCommand()
    }
}