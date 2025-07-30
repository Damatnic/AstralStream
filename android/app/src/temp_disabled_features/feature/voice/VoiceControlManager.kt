package com.astralplayer.nextplayer.feature.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import java.util.*

/**
 * Data class representing a voice command
 */
data class VoiceCommand(
    val id: String,
    val command: String,
    val action: VoiceAction,
    val parameters: Map<String, Any> = emptyMap(),
    val confidence: Float = 1.0f,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Enum for voice actions
 */
enum class VoiceAction {
    PLAY,
    PAUSE,
    STOP,
    NEXT,
    PREVIOUS,
    SEEK_FORWARD,
    SEEK_BACKWARD,
    VOLUME_UP,
    VOLUME_DOWN,
    MUTE,
    UNMUTE,
    FULLSCREEN,
    EXIT_FULLSCREEN,
    OPEN_FILE,
    SEARCH,
    REPEAT,
    SHUFFLE,
    SPEED_UP,
    SLOW_DOWN,
    BRIGHTNESS_UP,
    BRIGHTNESS_DOWN,
    SUBTITLE_ON,
    SUBTITLE_OFF,
    CAST,
    BOOKMARK,
    UNKNOWN
}

/**
 * Data class for voice recognition state
 */
data class VoiceRecognitionState(
    val isListening: Boolean = false,
    val isProcessing: Boolean = false,
    val recognizedText: String = "",
    val confidence: Float = 0f,
    val error: String? = null
)

/**
 * Manager for voice control functionality
 */
class VoiceControlManager(private val context: Context) {
    
    companion object {
        private const val TAG = "VoiceControlManager"
    }
    
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    private val _voiceState = MutableStateFlow(VoiceRecognitionState())
    val voiceState: StateFlow<VoiceRecognitionState> = _voiceState.asStateFlow()
    
    private val _lastCommand = MutableStateFlow<VoiceCommand?>(null)
    val lastCommand: StateFlow<VoiceCommand?> = _lastCommand.asStateFlow()
    
    private val _commandHistory = MutableStateFlow<List<VoiceCommand>>(emptyList())
    val commandHistory: StateFlow<List<VoiceCommand>> = _commandHistory.asStateFlow()
    
    private val _isVoiceEnabled = MutableStateFlow(true)
    val isVoiceEnabled: StateFlow<Boolean> = _isVoiceEnabled.asStateFlow()
    
    private var speechRecognizer: SpeechRecognizer? = null
    private var recognizerIntent: Intent? = null
    
    init {
        setupSpeechRecognizer()
    }
    
    // Voice command patterns
    private val commandPatterns = mapOf(
        VoiceAction.PLAY to listOf("play", "start", "resume", "continue"),
        VoiceAction.PAUSE to listOf("pause", "stop playing", "hold"),
        VoiceAction.STOP to listOf("stop", "end", "quit"),
        VoiceAction.NEXT to listOf("next", "skip", "forward", "next video"),
        VoiceAction.PREVIOUS to listOf("previous", "back", "last", "previous video"),
        VoiceAction.SEEK_FORWARD to listOf("fast forward", "skip ahead", "jump forward", "forward 10", "forward 30"),
        VoiceAction.SEEK_BACKWARD to listOf("rewind", "go back", "jump back", "back 10", "back 30"),
        VoiceAction.VOLUME_UP to listOf("volume up", "louder", "increase volume", "turn up"),
        VoiceAction.VOLUME_DOWN to listOf("volume down", "quieter", "decrease volume", "turn down"),
        VoiceAction.MUTE to listOf("mute", "silence", "turn off sound"),
        VoiceAction.UNMUTE to listOf("unmute", "sound on", "turn on sound"),
        VoiceAction.FULLSCREEN to listOf("fullscreen", "full screen", "maximize"),
        VoiceAction.EXIT_FULLSCREEN to listOf("exit fullscreen", "minimize", "window mode"),
        VoiceAction.SEARCH to listOf("search", "find", "look for"),
        VoiceAction.REPEAT to listOf("repeat", "loop", "replay"),
        VoiceAction.SHUFFLE to listOf("shuffle", "random", "mix"),
        VoiceAction.SPEED_UP to listOf("speed up", "faster", "increase speed", "2x speed"),
        VoiceAction.SLOW_DOWN to listOf("slow down", "slower", "decrease speed", "half speed"),
        VoiceAction.BRIGHTNESS_UP to listOf("brightness up", "brighter", "increase brightness"),
        VoiceAction.BRIGHTNESS_DOWN to listOf("brightness down", "darker", "decrease brightness"),
        VoiceAction.SUBTITLE_ON to listOf("subtitles on", "show subtitles", "enable subtitles"),
        VoiceAction.SUBTITLE_OFF to listOf("subtitles off", "hide subtitles", "disable subtitles"),
        VoiceAction.CAST to listOf("cast", "chromecast", "cast to tv"),
        VoiceAction.BOOKMARK to listOf("bookmark", "save", "mark", "remember this")
    )
    
    /**
     * Setup speech recognizer
     */
    private fun setupSpeechRecognizer() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            Log.e(TAG, "Speech recognition not available")
            _voiceState.value = _voiceState.value.copy(
                error = "Speech recognition not available on this device"
            )
            return
        }
        
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
        speechRecognizer?.setRecognitionListener(recognitionListener)
        
        recognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
    }
    
    /**
     * Recognition listener for handling speech recognition events
     */
    private val recognitionListener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            Log.d(TAG, "Ready for speech")
            _voiceState.value = _voiceState.value.copy(
                isListening = true,
                error = null
            )
        }
        
        override fun onBeginningOfSpeech() {
            Log.d(TAG, "Speech started")
        }
        
        override fun onRmsChanged(rmsdB: Float) {
            // Audio level changed
        }
        
        override fun onBufferReceived(buffer: ByteArray?) {
            // Buffer received
        }
        
        override fun onEndOfSpeech() {
            Log.d(TAG, "Speech ended")
            _voiceState.value = _voiceState.value.copy(
                isListening = false,
                isProcessing = true
            )
        }
        
        override fun onError(error: Int) {
            val errorMessage = when (error) {
                SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                SpeechRecognizer.ERROR_CLIENT -> "Client side error"
                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
                SpeechRecognizer.ERROR_NETWORK -> "Network error"
                SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
                SpeechRecognizer.ERROR_NO_MATCH -> "No match found"
                SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognition service busy"
                SpeechRecognizer.ERROR_SERVER -> "Server error"
                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech input"
                else -> "Unknown error: $error"
            }
            
            Log.e(TAG, "Speech recognition error: $errorMessage")
            _voiceState.value = _voiceState.value.copy(
                isListening = false,
                isProcessing = false,
                error = errorMessage
            )
        }
        
        override fun onResults(results: Bundle?) {
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val scores = results?.getFloatArray(SpeechRecognizer.CONFIDENCE_SCORES)
            
            if (!matches.isNullOrEmpty()) {
                val recognizedText = matches[0]
                val confidence = scores?.getOrNull(0) ?: 0.5f
                
                Log.d(TAG, "Recognized: $recognizedText (confidence: $confidence)")
                
                val command = parseVoiceCommand(recognizedText)
                
                _voiceState.value = _voiceState.value.copy(
                    isProcessing = false,
                    recognizedText = recognizedText,
                    confidence = confidence
                )
                
                _lastCommand.value = command
                addToHistory(command)
            }
        }
        
        override fun onPartialResults(partialResults: Bundle?) {
            val partial = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            if (!partial.isNullOrEmpty()) {
                _voiceState.value = _voiceState.value.copy(
                    recognizedText = partial[0]
                )
            }
        }
        
        override fun onEvent(eventType: Int, params: Bundle?) {
            Log.d(TAG, "Speech event: $eventType")
        }
    }
    
    /**
     * Check if we have audio permission
     */
    fun hasAudioPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    /**
     * Start voice recognition
     */
    fun startListening() {
        if (!_isVoiceEnabled.value) return
        
        if (!hasAudioPermission()) {
            _voiceState.value = _voiceState.value.copy(
                error = "Microphone permission required"
            )
            return
        }
        
        try {
            speechRecognizer?.startListening(recognizerIntent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start listening", e)
            _voiceState.value = _voiceState.value.copy(
                error = "Failed to start voice recognition: ${e.message}"
            )
        }
    }
    
    /**
     * Stop voice recognition
     */
    fun stopListening() {
        try {
            speechRecognizer?.stopListening()
            _voiceState.value = _voiceState.value.copy(
                isListening = false,
                isProcessing = false
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop listening", e)
        }
    }
    
    /**
     * Cancel voice recognition
     */
    fun cancelListening() {
        try {
            speechRecognizer?.cancel()
            _voiceState.value = _voiceState.value.copy(
                isListening = false,
                isProcessing = false,
                recognizedText = ""
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to cancel listening", e)
        }
    }
    
    /**
     * Parse voice command from recognized text
     */
    private fun parseVoiceCommand(text: String): VoiceCommand {
        val lowerText = text.lowercase()
        
        for ((action, patterns) in commandPatterns) {
            for (pattern in patterns) {
                if (lowerText.contains(pattern)) {
                    val parameters = extractParameters(lowerText, action)
                    return VoiceCommand(
                        id = "cmd_${System.currentTimeMillis()}",
                        command = text,
                        action = action,
                        parameters = parameters,
                        confidence = calculateConfidence(lowerText, pattern)
                    )
                }
            }
        }
        
        return VoiceCommand(
            id = "cmd_${System.currentTimeMillis()}",
            command = text,
            action = VoiceAction.UNKNOWN,
            confidence = 0.1f
        )
    }
    
    /**
     * Extract parameters from voice command
     */
    private fun extractParameters(text: String, action: VoiceAction): Map<String, Any> {
        val parameters = mutableMapOf<String, Any>()
        
        when (action) {
            VoiceAction.SEEK_FORWARD, VoiceAction.SEEK_BACKWARD -> {
                val seconds = extractTimeValue(text)
                if (seconds > 0) {
                    parameters["seconds"] = seconds
                }
            }
            VoiceAction.SEARCH -> {
                val query = extractSearchQuery(text)
                if (query.isNotEmpty()) {
                    parameters["query"] = query
                }
            }
            VoiceAction.SPEED_UP, VoiceAction.SLOW_DOWN -> {
                val speed = extractSpeedValue(text)
                if (speed > 0) {
                    parameters["speed"] = speed
                }
            }
            else -> {}
        }
        
        return parameters
    }
    
    /**
     * Extract time value from text (e.g., "forward 30" -> 30)
     */
    private fun extractTimeValue(text: String): Int {
        val regex = Regex("\\b(\\d+)\\b")
        val match = regex.find(text)
        return match?.value?.toIntOrNull() ?: 10 // Default to 10 seconds
    }
    
    /**
     * Extract search query from text
     */
    private fun extractSearchQuery(text: String): String {
        val searchPrefixes = listOf("search for", "find", "look for")
        for (prefix in searchPrefixes) {
            val index = text.indexOf(prefix)
            if (index != -1) {
                return text.substring(index + prefix.length).trim()
            }
        }
        return ""
    }
    
    /**
     * Extract speed value from text
     */
    private fun extractSpeedValue(text: String): Float {
        return when {
            text.contains("2x") || text.contains("double") -> 2.0f
            text.contains("half") || text.contains("0.5") -> 0.5f
            text.contains("1.5") -> 1.5f
            else -> 1.0f
        }
    }
    
    /**
     * Calculate confidence score for pattern match
     */
    private fun calculateConfidence(text: String, pattern: String): Float {
        val exactMatch = text == pattern
        val containsPattern = text.contains(pattern)
        val wordCount = text.split(" ").size
        
        return when {
            exactMatch -> 1.0f
            containsPattern && wordCount <= 3 -> 0.9f
            containsPattern && wordCount <= 5 -> 0.8f
            containsPattern -> 0.7f
            else -> 0.5f
        }
    }
    
    /**
     * Add command to history
     */
    private fun addToHistory(command: VoiceCommand) {
        val currentHistory = _commandHistory.value.toMutableList()
        currentHistory.add(0, command) // Add to beginning
        
        // Keep only last 50 commands
        if (currentHistory.size > 50) {
            currentHistory.removeAt(currentHistory.size - 1)
        }
        
        _commandHistory.value = currentHistory
    }
    
    /**
     * Enable/disable voice control
     */
    fun setVoiceEnabled(enabled: Boolean) {
        _isVoiceEnabled.value = enabled
        if (!enabled) {
            stopListening()
        }
    }
    
    /**
     * Clear command history
     */
    fun clearHistory() {
        _commandHistory.value = emptyList()
    }
    
    /**
     * Get available voice commands
     */
    fun getAvailableCommands(): List<String> {
        return commandPatterns.values.flatten().distinct().sorted()
    }
    
    /**
     * Clean up resources
     */
    fun cleanup() {
        try {
            speechRecognizer?.destroy()
            speechRecognizer = null
        } catch (e: Exception) {
            Log.e(TAG, "Failed to cleanup", e)
        }
    }
}

/**
 * ViewModel for voice control
 */
class VoiceControlViewModel(private val voiceManager: VoiceControlManager) : ViewModel() {
    
    val voiceState = voiceManager.voiceState
    val lastCommand = voiceManager.lastCommand
    val commandHistory = voiceManager.commandHistory
    val isVoiceEnabled = voiceManager.isVoiceEnabled
    
    fun startListening() {
        voiceManager.startListening()
    }
    
    fun stopListening() {
        voiceManager.stopListening()
    }
    
    fun setVoiceEnabled(enabled: Boolean) {
        voiceManager.setVoiceEnabled(enabled)
    }
    
    fun clearHistory() {
        voiceManager.clearHistory()
    }
    
    fun getAvailableCommands(): List<String> {
        return voiceManager.getAvailableCommands()
    }
    
    override fun onCleared() {
        super.onCleared()
        voiceManager.cleanup()
    }
}

/**
 * Composable for voice control screen
 */
@Composable
fun VoiceControlScreen(
    voiceState: VoiceRecognitionState,
    lastCommand: VoiceCommand?,
    commandHistory: List<VoiceCommand>,
    isVoiceEnabled: Boolean,
    onStartListening: () -> Unit,
    onStopListening: () -> Unit,
    onToggleVoice: (Boolean) -> Unit,
    onClearHistory: () -> Unit,
    availableCommands: List<String>,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Voice Control",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            
            Switch(
                checked = isVoiceEnabled,
                onCheckedChange = onToggleVoice,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color(0xFF00BCD4),
                    checkedTrackColor = Color(0xFF00BCD4).copy(alpha = 0.5f)
                )
            )
        }
        
        // Voice control interface
        VoiceControlInterface(
            voiceState = voiceState,
            lastCommand = lastCommand,
            isEnabled = isVoiceEnabled,
            onStartListening = onStartListening,
            onStopListening = onStopListening
        )
        
        // Command history and available commands
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                CommandHistorySection(
                    history = commandHistory,
                    onClearHistory = onClearHistory
                )
            }
            
            item {
                AvailableCommandsSection(
                    commands = availableCommands
                )
            }
        }
    }
}

/**
 * Voice control interface
 */
@Composable
private fun VoiceControlInterface(
    voiceState: VoiceRecognitionState,
    lastCommand: VoiceCommand?,
    isEnabled: Boolean,
    onStartListening: () -> Unit,
    onStopListening: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.05f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Microphone button
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .background(
                        color = when {
                            !isEnabled -> Color.Gray.copy(alpha = 0.3f)
                            voiceState.isListening -> Color(0xFF00BCD4).copy(alpha = 0.3f)
                            voiceState.isProcessing -> Color(0xFFFFC107).copy(alpha = 0.3f)
                            else -> Color.White.copy(alpha = 0.1f)
                        },
                        shape = CircleShape
                    )
                    .clickable(enabled = isEnabled) {
                        if (voiceState.isListening) {
                            onStopListening()
                        } else {
                            onStartListening()
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when {
                        voiceState.isListening -> Icons.Default.MicOff
                        voiceState.isProcessing -> Icons.Default.Sync
                        else -> Icons.Default.Mic
                    },
                    contentDescription = "Voice Control",
                    tint = when {
                        !isEnabled -> Color.Gray
                        voiceState.isListening -> Color(0xFF00BCD4)
                        voiceState.isProcessing -> Color(0xFFFFC107)
                        else -> Color.White
                    },
                    modifier = Modifier.size(48.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Status text
            Text(
                text = when {
                    !isEnabled -> "Voice control disabled"
                    voiceState.isListening -> "Listening..."
                    voiceState.isProcessing -> "Processing..."
                    voiceState.recognizedText.isNotEmpty() -> "\"${voiceState.recognizedText}\""
                    else -> "Tap to start voice control"
                },
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
            
            // Last command
            lastCommand?.let { command ->
                Spacer(modifier = Modifier.height(8.dp))
                
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF00BCD4).copy(alpha = 0.2f)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Last Command",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 12.sp
                        )
                        
                        Text(
                            text = command.action.name.replace("_", " ").lowercase()
                                .replaceFirstChar { it.uppercase() },
                            color = Color(0xFF00BCD4),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Text(
                            text = "${(command.confidence * 100).toInt()}% confidence",
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }
    }
}

/**
 * Command history section
 */
@Composable
private fun CommandHistorySection(
    history: List<VoiceCommand>,
    onClearHistory: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.05f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Command History",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                
                if (history.isNotEmpty()) {
                    TextButton(onClick = onClearHistory) {
                        Text(
                            text = "Clear",
                            color = Color(0xFF00BCD4)
                        )
                    }
                }
            }
            
            if (history.isEmpty()) {
                Text(
                    text = "No commands yet",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            } else {
                history.take(5).forEach { command ->
                    CommandHistoryItem(command = command)
                }
            }
        }
    }
}

/**
 * Command history item
 */
@Composable
private fun CommandHistoryItem(command: VoiceCommand) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = when (command.action) {
                VoiceAction.PLAY -> Icons.Default.PlayArrow
                VoiceAction.PAUSE -> Icons.Default.Pause
                VoiceAction.VOLUME_UP -> Icons.Default.VolumeUp
                VoiceAction.SEARCH -> Icons.Default.Search
                else -> Icons.Default.RecordVoiceOver
            },
            contentDescription = command.action.name,
            tint = Color(0xFF00BCD4),
            modifier = Modifier.size(16.dp)
        )
        
        Spacer(modifier = Modifier.width(8.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = command.command,
                color = Color.White,
                fontSize = 12.sp
            )
            
            Text(
                text = command.action.name.replace("_", " ").lowercase(),
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 10.sp
            )
        }
        
        Text(
            text = "${(command.confidence * 100).toInt()}%",
            color = Color(0xFF4CAF50),
            fontSize = 10.sp
        )
    }
}

/**
 * Available commands section
 */
@Composable
private fun AvailableCommandsSection(commands: List<String>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.05f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Available Commands",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            commands.take(10).forEach { command ->
                Text(
                    text = "• $command",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(vertical = 2.dp)
                )
            }
            
            if (commands.size > 10) {
                Text(
                    text = "... and ${commands.size - 10} more",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 10.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}