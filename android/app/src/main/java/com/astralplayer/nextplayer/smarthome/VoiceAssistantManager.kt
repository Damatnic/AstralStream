package com.astralplayer.nextplayer.smarthome

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Voice Assistant Manager for speech recognition and voice commands
 */
@Singleton
class VoiceAssistantManager @Inject constructor(
    private val context: Context
) {
    
    private var voiceRecognizer: SpeechRecognizer? = null
    private var commandCallback: ((VoiceCommand) -> Unit)? = null
    private var commandCount = 0
    private var isListening = false
    
    private val speechIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
    }
    
    private val recognitionListener = object : RecognitionListener {
        override fun onResults(results: Bundle?) {
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            matches?.firstOrNull()?.let { processVoiceInput(it) }
            
            if (isListening) {
                startListening() // Continue listening
            }
        }
        
        override fun onPartialResults(partialResults: Bundle?) {
            val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            matches?.firstOrNull()?.let { processPartialInput(it) }
        }
        
        override fun onError(error: Int) {
            Log.e("VoiceAssistant", "Speech recognition error: $error")
            if (isListening && error == SpeechRecognizer.ERROR_NO_MATCH) {
                startListening() // Continue listening
            }
        }
        
        override fun onReadyForSpeech(params: Bundle?) {}
        override fun onBeginningOfSpeech() {}
        override fun onRmsChanged(rmsdB: Float) {}
        override fun onBufferReceived(buffer: ByteArray?) {}
        override fun onEndOfSpeech() {}
        override fun onEvent(eventType: Int, params: Bundle?) {}
    }
    
    suspend fun initialize(): Boolean = withContext(Dispatchers.Main) {
        try {
            if (SpeechRecognizer.isRecognitionAvailable(context)) {
                voiceRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
                voiceRecognizer?.setRecognitionListener(recognitionListener)
                
                Log.i("VoiceAssistant", "Voice assistant manager initialized")
                true
            } else {
                Log.w("VoiceAssistant", "Speech recognition not available")
                false
            }
        } catch (e: Exception) {
            Log.e("VoiceAssistant", "Failed to initialize voice assistant", e)
            false
        }
    }
    
    suspend fun discoverAssistants(): List<VoiceAssistant> {
        val assistants = mutableListOf<VoiceAssistant>()
        
        // Check for Google Assistant
        if (isGoogleAssistantAvailable()) {
            assistants.add(
                VoiceAssistant(
                    id = "google_assistant",
                    name = "Google Assistant",
                    type = SmartDeviceType.VOICE_ASSISTANT,
                    isConnected = true
                )
            )
        }
        
        // Check for Alexa
        if (isAlexaAvailable()) {
            assistants.add(
                VoiceAssistant(
                    id = "alexa",
                    name = "Amazon Alexa",
                    type = SmartDeviceType.VOICE_ASSISTANT,
                    isConnected = checkAlexaConnection()
                )
            )
        }
        
        return assistants
    }
    
    fun enableVoiceControl(callback: (VoiceCommand) -> Unit) {
        commandCallback = callback
        isListening = true
        startListening()
    }
    
    fun disableVoiceControl() {
        isListening = false
        voiceRecognizer?.stopListening()
        commandCallback = null
    }
    
    private fun startListening() {
        voiceRecognizer?.startListening(speechIntent)
    }
    
    private fun processVoiceInput(input: String) {
        val command = parseVoiceCommand(input)
        command?.let {
            commandCount++
            commandCallback?.invoke(it)
        }
    }
    
    private fun processPartialInput(input: String) {
        // Handle partial results for real-time feedback
        // Could show transcription in UI
    }
    
    private fun parseVoiceCommand(input: String): VoiceCommand? {
        val lowerInput = input.lowercase()
        
        return when {
            lowerInput.contains("play") && !lowerInput.contains("pause") -> {
                VoiceCommand(VoiceAction.PLAY, emptyMap())
            }
            lowerInput.contains("pause") || lowerInput.contains("stop") -> {
                VoiceCommand(VoiceAction.PAUSE, emptyMap())
            }
            lowerInput.contains("skip") || lowerInput.contains("forward") -> {
                val seconds = extractTimeFromInput(lowerInput) ?: 10
                VoiceCommand(VoiceAction.SEEK, mapOf("position" to seconds * 1000L))
            }
            lowerInput.contains("rewind") || lowerInput.contains("back") -> {
                val seconds = extractTimeFromInput(lowerInput) ?: 10
                VoiceCommand(VoiceAction.SEEK, mapOf("position" to -seconds * 1000L))
            }
            lowerInput.contains("volume") -> {
                val level = extractVolumeLevel(lowerInput)
                VoiceCommand(VoiceAction.VOLUME, mapOf("level" to level))
            }
            lowerInput.contains("cast to") || lowerInput.contains("play on") -> {
                val device = extractDeviceName(lowerInput)
                VoiceCommand(VoiceAction.CAST_TO, mapOf("device" to device))
            }
            lowerInput.contains("lights") -> {
                val lightParams = extractLightCommand(lowerInput)
                VoiceCommand(VoiceAction.LIGHTS, lightParams)
            }
            lowerInput.contains("find") || lowerInput.contains("search") -> {
                val query = extractSearchQuery(lowerInput)
                VoiceCommand(VoiceAction.FIND_CONTENT, mapOf("query" to query))
            }
            lowerInput.contains("speed") -> {
                val speed = extractPlaybackSpeed(lowerInput)
                VoiceCommand(VoiceAction.CONTROL_PLAYBACK, mapOf("speed" to speed))
            }
            else -> null
        }
    }
    
    private fun extractTimeFromInput(input: String): Int? {
        // Extract seconds from phrases like "skip 30 seconds"
        val regex = """(\d+)\s*seconds?""".toRegex()
        return regex.find(input)?.groupValues?.get(1)?.toIntOrNull()
    }
    
    private fun extractVolumeLevel(input: String): Float {
        return when {
            input.contains("max") || input.contains("full") -> 1.0f
            input.contains("half") -> 0.5f
            input.contains("mute") || input.contains("zero") -> 0.0f
            else -> {
                // Try to extract percentage
                val regex = """(\d+)\s*%?""".toRegex()
                val percent = regex.find(input)?.groupValues?.get(1)?.toFloatOrNull()
                (percent ?: 50f) / 100f
            }
        }
    }
    
    private fun extractDeviceName(input: String): String {
        // Extract device name from phrases like "cast to living room TV"
        val prefixes = listOf("cast to", "play on", "stream to")
        for (prefix in prefixes) {
            val index = input.indexOf(prefix)
            if (index != -1) {
                return input.substring(index + prefix.length).trim()
            }
        }
        return "default"
    }
    
    private fun extractLightCommand(input: String): Map<String, Any> {
        return when {
            input.contains("sync") -> mapOf("action" to "sync")
            input.contains("bright") -> {
                val level = if (input.contains("dim")) 0.3f else 1.0f
                mapOf("action" to "brightness", "level" to level)
            }
            else -> mapOf("action" to "toggle")
        }
    }
    
    private fun extractSearchQuery(input: String): String {
        val keywords = listOf("find", "search for", "look for", "show me")
        for (keyword in keywords) {
            val index = input.indexOf(keyword)
            if (index != -1) {
                return input.substring(index + keyword.length).trim()
            }
        }
        return input
    }
    
    private fun extractPlaybackSpeed(input: String): Float {
        return when {
            input.contains("double") || input.contains("2x") -> 2.0f
            input.contains("half") || input.contains("0.5") -> 0.5f
            input.contains("normal") -> 1.0f
            else -> {
                val regex = """(\d+\.?\d*)\s*x""".toRegex()
                regex.find(input)?.groupValues?.get(1)?.toFloatOrNull() ?: 1.0f
            }
        }
    }
    
    fun isGoogleAssistantAvailable(): Boolean {
        val intent = Intent(Intent.ACTION_VOICE_COMMAND)
        intent.setPackage("com.google.android.googlequicksearchbox")
        return context.packageManager.queryIntentActivities(intent, 0).isNotEmpty()
    }
    
    fun isAlexaAvailable(): Boolean {
        return try {
            context.packageManager.getPackageInfo("com.amazon.dee.app", 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }
    
    private fun checkAlexaConnection(): Boolean {
        // Check if Alexa app is running and connected
        // This would require Alexa SDK integration
        return false
    }
    
    fun getCommandCount(): Int = commandCount
    
    fun cleanup() {
        try {
            disableVoiceControl()
            voiceRecognizer?.destroy()
            voiceRecognizer = null
            Log.i("VoiceAssistant", "Voice assistant manager cleaned up")
        } catch (e: Exception) {
            Log.e("VoiceAssistant", "Error during voice assistant cleanup", e)
        }
    }
}