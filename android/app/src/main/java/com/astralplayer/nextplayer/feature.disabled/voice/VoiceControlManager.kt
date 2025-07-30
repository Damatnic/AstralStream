package com.astralplayer.nextplayer.feature.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import androidx.media3.common.Player
import com.astralplayer.nextplayer.data.RecentFile
import com.astralplayer.nextplayer.feature.playlist.PlaylistManager
import com.astralplayer.nextplayer.feature.search.AdvancedSearchManager
import com.astralplayer.nextplayer.feature.search.SearchFilters
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.*

/**
 * Voice Control Manager
 * Handles voice commands and voice feedback for the video player
 */
class VoiceControlManager(
    private val context: Context,
    private val player: Player? = null,
    private val searchManager: AdvancedSearchManager? = null,
    private val playlistManager: PlaylistManager? = null
) : RecognitionListener, TextToSpeech.OnInitListener {
    
    private var speechRecognizer: SpeechRecognizer? = null
    private var textToSpeech: TextToSpeech? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening
    
    private val _recognizedText = MutableStateFlow("")
    val recognizedText: StateFlow<String> = _recognizedText
    
    private val _commandResult = MutableStateFlow<CommandResult?>(null)
    val commandResult: StateFlow<CommandResult?> = _commandResult
    
    private val _voiceState = MutableStateFlow(VoiceState.IDLE)
    val voiceState: StateFlow<VoiceState> = _voiceState
    
    private var isTtsReady = false
    private var commandCallback: ((VoiceCommand) -> Unit)? = null
    
    // Command patterns
    private val commandPatterns = mapOf(
        // Playback commands
        "play" to VoiceCommand.PLAY,
        "pause" to VoiceCommand.PAUSE,
        "stop" to VoiceCommand.STOP,
        "resume" to VoiceCommand.RESUME,
        "next" to VoiceCommand.NEXT,
        "previous" to VoiceCommand.PREVIOUS,
        "replay" to VoiceCommand.REPLAY,
        
        // Seek commands
        "forward (.+) seconds?" to VoiceCommand.SEEK_FORWARD,
        "backward (.+) seconds?" to VoiceCommand.SEEK_BACKWARD,
        "skip to (.+)" to VoiceCommand.SEEK_TO,
        "jump to (.+)" to VoiceCommand.SEEK_TO,
        "go to (.+)" to VoiceCommand.SEEK_TO,
        
        // Speed commands
        "speed up" to VoiceCommand.SPEED_UP,
        "slow down" to VoiceCommand.SPEED_DOWN,
        "normal speed" to VoiceCommand.SPEED_NORMAL,
        "set speed to (.+)" to VoiceCommand.SET_SPEED,
        
        // Volume commands
        "volume up" to VoiceCommand.VOLUME_UP,
        "volume down" to VoiceCommand.VOLUME_DOWN,
        "mute" to VoiceCommand.MUTE,
        "unmute" to VoiceCommand.UNMUTE,
        "set volume to (.+)" to VoiceCommand.SET_VOLUME,
        
        // Display commands
        "fullscreen" to VoiceCommand.FULLSCREEN,
        "exit fullscreen" to VoiceCommand.EXIT_FULLSCREEN,
        "rotate" to VoiceCommand.ROTATE,
        "zoom in" to VoiceCommand.ZOOM_IN,
        "zoom out" to VoiceCommand.ZOOM_OUT,
        "fit to screen" to VoiceCommand.FIT_SCREEN,
        
        // Subtitle commands
        "show subtitles" to VoiceCommand.SHOW_SUBTITLES,
        "hide subtitles" to VoiceCommand.HIDE_SUBTITLES,
        "next subtitle" to VoiceCommand.NEXT_SUBTITLE,
        "subtitle language (.+)" to VoiceCommand.SUBTITLE_LANGUAGE,
        
        // Search commands
        "search for (.+)" to VoiceCommand.SEARCH,
        "find (.+)" to VoiceCommand.SEARCH,
        "show (.+) videos" to VoiceCommand.FILTER_SEARCH,
        
        // Playlist commands
        "add to playlist" to VoiceCommand.ADD_TO_PLAYLIST,
        "create playlist (.+)" to VoiceCommand.CREATE_PLAYLIST,
        "play playlist (.+)" to VoiceCommand.PLAY_PLAYLIST,
        
        // Navigation commands
        "go home" to VoiceCommand.GO_HOME,
        "go back" to VoiceCommand.GO_BACK,
        "open settings" to VoiceCommand.OPEN_SETTINGS,
        
        // Info commands
        "what's playing" to VoiceCommand.CURRENT_INFO,
        "show info" to VoiceCommand.SHOW_INFO,
        "hide info" to VoiceCommand.HIDE_INFO,
        
        // Special commands
        "bookmark" to VoiceCommand.ADD_BOOKMARK,
        "sleep timer (.+) minutes?" to VoiceCommand.SET_SLEEP_TIMER,
        "cancel sleep timer" to VoiceCommand.CANCEL_SLEEP_TIMER,
        "repeat" to VoiceCommand.TOGGLE_REPEAT,
        "shuffle" to VoiceCommand.TOGGLE_SHUFFLE
    )
    
    init {
        initializeSpeechRecognizer()
        initializeTextToSpeech()
    }
    
    private fun initializeSpeechRecognizer() {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(this@VoiceControlManager)
            }
        }
    }
    
    private fun initializeTextToSpeech() {
        textToSpeech = TextToSpeech(context, this)
    }
    
    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = textToSpeech?.setLanguage(Locale.getDefault())
            isTtsReady = result != TextToSpeech.LANG_MISSING_DATA && 
                        result != TextToSpeech.LANG_NOT_SUPPORTED
        }
    }
    
    /**
     * Start listening for voice commands
     */
    fun startListening(
        continuous: Boolean = false,
        onCommand: ((VoiceCommand) -> Unit)? = null
    ) {
        if (speechRecognizer == null) {
            _commandResult.value = CommandResult(
                success = false,
                message = "Voice recognition not available"
            )
            return
        }
        
        commandCallback = onCommand
        
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            
            if (continuous) {
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1500)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 5000)
            }
        }
        
        speechRecognizer?.startListening(intent)
        _isListening.value = true
        _voiceState.value = VoiceState.LISTENING
    }
    
    /**
     * Stop listening for voice commands
     */
    fun stopListening() {
        speechRecognizer?.stopListening()
        _isListening.value = false
        _voiceState.value = VoiceState.IDLE
    }
    
    /**
     * Process voice command
     */
    private fun processCommand(text: String) {
        scope.launch {
            _voiceState.value = VoiceState.PROCESSING
            
            val lowercaseText = text.lowercase()
            var commandExecuted = false
            
            // Try to match command patterns
            for ((pattern, command) in commandPatterns) {
                val regex = Regex(pattern, RegexOption.IGNORE_CASE)
                val match = regex.find(lowercaseText)
                
                if (match != null) {
                    val parameter = if (match.groupValues.size > 1) {
                        match.groupValues[1]
                    } else null
                    
                    executeCommand(command, parameter)
                    commandExecuted = true
                    break
                }
            }
            
            if (!commandExecuted) {
                // Try natural language processing
                processNaturalLanguage(text)
            }
            
            _voiceState.value = VoiceState.IDLE
        }
    }
    
    /**
     * Execute a voice command
     */
    private suspend fun executeCommand(command: VoiceCommand, parameter: String? = null) {
        // Notify callback if set
        commandCallback?.invoke(command)
        
        val result = when (command) {
            // Playback commands
            VoiceCommand.PLAY -> {
                player?.play()
                speak("Playing")
                CommandResult(true, "Playing video")
            }
            
            VoiceCommand.PAUSE -> {
                player?.pause()
                speak("Paused")
                CommandResult(true, "Video paused")
            }
            
            VoiceCommand.STOP -> {
                player?.stop()
                speak("Stopped")
                CommandResult(true, "Playback stopped")
            }
            
            VoiceCommand.NEXT -> {
                player?.seekToNext()
                speak("Next video")
                CommandResult(true, "Playing next video")
            }
            
            VoiceCommand.PREVIOUS -> {
                player?.seekToPrevious()
                speak("Previous video")
                CommandResult(true, "Playing previous video")
            }
            
            // Seek commands
            VoiceCommand.SEEK_FORWARD -> {
                val seconds = parameter?.extractNumber() ?: 10
                player?.let {
                    it.seekTo(it.currentPosition + (seconds * 1000))
                    speak("Forward $seconds seconds")
                }
                CommandResult(true, "Seeking forward $seconds seconds")
            }
            
            VoiceCommand.SEEK_BACKWARD -> {
                val seconds = parameter?.extractNumber() ?: 10
                player?.let {
                    it.seekTo((it.currentPosition - (seconds * 1000)).coerceAtLeast(0))
                    speak("Backward $seconds seconds")
                }
                CommandResult(true, "Seeking backward $seconds seconds")
            }
            
            VoiceCommand.SEEK_TO -> {
                parameter?.let { timeStr ->
                    val position = parseTimeString(timeStr)
                    if (position != null) {
                        player?.seekTo(position)
                        speak("Jumping to $timeStr")
                        CommandResult(true, "Seeking to $timeStr")
                    } else {
                        speak("Invalid time format")
                        CommandResult(false, "Invalid time format: $timeStr")
                    }
                } ?: CommandResult(false, "No time specified")
            }
            
            // Speed commands
            VoiceCommand.SPEED_UP -> {
                player?.let {
                    val newSpeed = (it.playbackParameters.speed + 0.25f).coerceAtMost(3f)
                    it.setPlaybackSpeed(newSpeed)
                    speak("Speed ${String.format("%.2f", newSpeed)}x")
                }
                CommandResult(true, "Increased playback speed")
            }
            
            VoiceCommand.SPEED_DOWN -> {
                player?.let {
                    val newSpeed = (it.playbackParameters.speed - 0.25f).coerceAtLeast(0.25f)
                    it.setPlaybackSpeed(newSpeed)
                    speak("Speed ${String.format("%.2f", newSpeed)}x")
                }
                CommandResult(true, "Decreased playback speed")
            }
            
            VoiceCommand.SPEED_NORMAL -> {
                player?.setPlaybackSpeed(1f)
                speak("Normal speed")
                CommandResult(true, "Set to normal speed")
            }
            
            VoiceCommand.SET_SPEED -> {
                val speed = parameter?.extractNumber()?.toFloat() ?: 1f
                player?.setPlaybackSpeed(speed.coerceIn(0.25f, 3f))
                speak("Speed ${String.format("%.2f", speed)}x")
                CommandResult(true, "Set speed to ${speed}x")
            }
            
            // Volume commands
            VoiceCommand.VOLUME_UP -> {
                adjustVolume(0.1f)
                CommandResult(true, "Volume increased")
            }
            
            VoiceCommand.VOLUME_DOWN -> {
                adjustVolume(-0.1f)
                CommandResult(true, "Volume decreased")
            }
            
            VoiceCommand.MUTE -> {
                player?.volume = 0f
                speak("Muted")
                CommandResult(true, "Volume muted")
            }
            
            VoiceCommand.UNMUTE -> {
                player?.volume = 1f
                speak("Unmuted")
                CommandResult(true, "Volume unmuted")
            }
            
            VoiceCommand.SET_VOLUME -> {
                val volume = parameter?.extractNumber() ?: 50
                val normalizedVolume = (volume / 100f).coerceIn(0f, 1f)
                player?.volume = normalizedVolume
                speak("Volume $volume percent")
                CommandResult(true, "Set volume to $volume%")
            }
            
            // Search commands
            VoiceCommand.SEARCH -> {
                parameter?.let { query ->
                    searchManager?.search(query, SearchFilters(), emptyList())
                    speak("Searching for $query")
                    CommandResult(true, "Searching for: $query", VoiceCommandAction.SEARCH(query))
                } ?: CommandResult(false, "No search query specified")
            }
            
            VoiceCommand.FILTER_SEARCH -> {
                parameter?.let { filter ->
                    val filters = parseSearchFilter(filter)
                    searchManager?.search("", filters, emptyList())
                    speak("Showing $filter videos")
                    CommandResult(true, "Filtering: $filter", VoiceCommandAction.FILTER(filters))
                } ?: CommandResult(false, "No filter specified")
            }
            
            // Info commands
            VoiceCommand.CURRENT_INFO -> {
                player?.currentMediaItem?.let { item ->
                    val title = item.mediaMetadata.title ?: "Unknown"
                    val position = formatTime(player.currentPosition)
                    val duration = formatTime(player.duration)
                    speak("Playing $title at $position of $duration")
                    CommandResult(true, "Current: $title ($position/$duration)")
                } ?: CommandResult(false, "No video playing")
            }
            
            else -> CommandResult(false, "Command not implemented: $command")
        }
        
        _commandResult.value = result
    }
    
    /**
     * Process natural language commands
     */
    private suspend fun processNaturalLanguage(text: String) {
        val lowercaseText = text.lowercase()
        
        // Try to understand intent
        when {
            lowercaseText.contains("play") && lowercaseText.contains("video") -> {
                // Extract video name
                val videoName = extractVideoName(text)
                if (videoName != null) {
                    searchAndPlay(videoName)
                }
            }
            
            lowercaseText.contains("show me") || lowercaseText.contains("find") -> {
                // Extract search criteria
                val criteria = extractSearchCriteria(text)
                performSmartSearch(criteria)
            }
            
            lowercaseText.contains("what") && lowercaseText.contains("time") -> {
                // Current time in video
                executeCommand(VoiceCommand.CURRENT_INFO)
            }
            
            else -> {
                speak("Sorry, I didn't understand that command")
                _commandResult.value = CommandResult(
                    false, 
                    "Unrecognized command: $text",
                    VoiceCommandAction.UNKNOWN(text)
                )
            }
        }
    }
    
    /**
     * Speak feedback to user
     */
    fun speak(text: String, immediate: Boolean = false) {
        if (isTtsReady) {
            val queueMode = if (immediate) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD
            textToSpeech?.speak(text, queueMode, null, UUID.randomUUID().toString())
        }
    }
    
    /**
     * Set voice feedback enabled
     */
    fun setVoiceFeedbackEnabled(enabled: Boolean) {
        if (!enabled) {
            textToSpeech?.stop()
        }
    }
    
    // RecognitionListener implementation
    
    override fun onReadyForSpeech(params: Bundle?) {
        _voiceState.value = VoiceState.READY
    }
    
    override fun onBeginningOfSpeech() {
        _voiceState.value = VoiceState.RECORDING
    }
    
    override fun onRmsChanged(rmsdB: Float) {
        // Update voice level indicator if needed
    }
    
    override fun onBufferReceived(buffer: ByteArray?) {}
    
    override fun onEndOfSpeech() {
        _voiceState.value = VoiceState.PROCESSING
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
            else -> "Unknown error"
        }
        
        _commandResult.value = CommandResult(false, errorMessage)
        _isListening.value = false
        _voiceState.value = VoiceState.ERROR
        
        // Auto-restart if in continuous mode
        if (error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT) {
            scope.launch {
                delay(500)
                if (_isListening.value) {
                    startListening(true, commandCallback)
                }
            }
        }
    }
    
    override fun onResults(results: Bundle?) {
        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        if (!matches.isNullOrEmpty()) {
            val text = matches[0]
            _recognizedText.value = text
            processCommand(text)
        }
        
        _isListening.value = false
    }
    
    override fun onPartialResults(partialResults: Bundle?) {
        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        if (!matches.isNullOrEmpty()) {
            _recognizedText.value = matches[0]
        }
    }
    
    override fun onEvent(eventType: Int, params: Bundle?) {}
    
    // Helper functions
    
    private fun adjustVolume(delta: Float) {
        player?.let {
            val newVolume = (it.volume + delta).coerceIn(0f, 1f)
            it.volume = newVolume
            val percentage = (newVolume * 100).toInt()
            speak("Volume $percentage percent")
        }
    }
    
    private fun parseTimeString(timeStr: String): Long? {
        return try {
            when {
                timeStr.contains(":") -> {
                    // Format: "1:30" or "1:30:45"
                    val parts = timeStr.split(":")
                    when (parts.size) {
                        2 -> {
                            val minutes = parts[0].toInt()
                            val seconds = parts[1].toInt()
                            ((minutes * 60 + seconds) * 1000).toLong()
                        }
                        3 -> {
                            val hours = parts[0].toInt()
                            val minutes = parts[1].toInt()
                            val seconds = parts[2].toInt()
                            ((hours * 3600 + minutes * 60 + seconds) * 1000).toLong()
                        }
                        else -> null
                    }
                }
                timeStr.contains("minute") -> {
                    val minutes = timeStr.extractNumber() ?: 0
                    (minutes * 60 * 1000).toLong()
                }
                timeStr.contains("second") -> {
                    val seconds = timeStr.extractNumber() ?: 0
                    (seconds * 1000).toLong()
                }
                else -> null
            }
        } catch (e: Exception) {
            null
        }
    }
    
    private fun parseSearchFilter(filter: String): SearchFilters {
        val lowercaseFilter = filter.lowercase()
        return when {
            lowercaseFilter.contains("recent") -> SearchFilters(
                dateRange = com.astralplayer.nextplayer.feature.search.DateRange(
                    System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000,
                    System.currentTimeMillis()
                )
            )
            lowercaseFilter.contains("favorite") -> SearchFilters(favorites = true)
            lowercaseFilter.contains("hd") || lowercaseFilter.contains("high quality") -> {
                SearchFilters(resolutions = setOf("720p", "1080p", "4K"))
            }
            lowercaseFilter.contains("long") -> SearchFilters(minDuration = 30 * 60 * 1000)
            lowercaseFilter.contains("short") -> SearchFilters(maxDuration = 10 * 60 * 1000)
            else -> SearchFilters()
        }
    }
    
    private fun extractVideoName(text: String): String? {
        val patterns = listOf(
            "play (.+)",
            "play video (.+)",
            "play the video (.+)",
            "play (.+) video"
        )
        
        for (pattern in patterns) {
            val regex = Regex(pattern, RegexOption.IGNORE_CASE)
            val match = regex.find(text)
            if (match != null && match.groupValues.size > 1) {
                return match.groupValues[1].trim()
            }
        }
        
        return null
    }
    
    private fun extractSearchCriteria(text: String): String {
        val patterns = listOf(
            "show me (.+)",
            "find (.+)",
            "search for (.+)",
            "look for (.+)"
        )
        
        for (pattern in patterns) {
            val regex = Regex(pattern, RegexOption.IGNORE_CASE)
            val match = regex.find(text)
            if (match != null && match.groupValues.size > 1) {
                return match.groupValues[1].trim()
            }
        }
        
        return text
    }
    
    private suspend fun searchAndPlay(videoName: String) {
        scope.launch {
            searchManager?.search(videoName, SearchFilters(), emptyList())
            delay(500) // Wait for search results
            
            val results = searchManager?.searchResults?.value
            if (!results.isNullOrEmpty()) {
                val topResult = results.first()
                speak("Playing ${topResult.video.fileName}")
                _commandResult.value = CommandResult(
                    true,
                    "Playing: ${topResult.video.fileName}",
                    VoiceCommandAction.PLAY_VIDEO(topResult.video)
                )
            } else {
                speak("No videos found for $videoName")
                _commandResult.value = CommandResult(false, "No videos found for: $videoName")
            }
        }
    }
    
    private suspend fun performSmartSearch(criteria: String) {
        val filters = parseSearchFilter(criteria)
        searchManager?.search(criteria, filters, emptyList())
        speak("Searching for $criteria")
        _commandResult.value = CommandResult(
            true,
            "Smart search: $criteria",
            VoiceCommandAction.SMART_SEARCH(criteria, filters)
        )
    }
    
    private fun formatTime(millis: Long): String {
        val seconds = millis / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        
        return when {
            hours > 0 -> "${hours} hours ${minutes % 60} minutes"
            minutes > 0 -> "${minutes} minutes ${seconds % 60} seconds"
            else -> "${seconds} seconds"
        }
    }
    
    /**
     * Clean up resources
     */
    fun destroy() {
        scope.cancel()
        speechRecognizer?.destroy()
        textToSpeech?.shutdown()
    }
}

// Extension functions

private fun String.extractNumber(): Int? {
    val regex = Regex("\\d+")
    val match = regex.find(this)
    return match?.value?.toIntOrNull()
}

private fun Player.setPlaybackSpeed(speed: Float) {
    setPlaybackParameters(playbackParameters.withSpeed(speed))
}

// Data classes

enum class VoiceCommand {
    // Playback
    PLAY, PAUSE, STOP, RESUME, NEXT, PREVIOUS, REPLAY,
    
    // Seek
    SEEK_FORWARD, SEEK_BACKWARD, SEEK_TO,
    
    // Speed
    SPEED_UP, SPEED_DOWN, SPEED_NORMAL, SET_SPEED,
    
    // Volume
    VOLUME_UP, VOLUME_DOWN, MUTE, UNMUTE, SET_VOLUME,
    
    // Display
    FULLSCREEN, EXIT_FULLSCREEN, ROTATE, ZOOM_IN, ZOOM_OUT, FIT_SCREEN,
    
    // Subtitles
    SHOW_SUBTITLES, HIDE_SUBTITLES, NEXT_SUBTITLE, SUBTITLE_LANGUAGE,
    
    // Search
    SEARCH, FILTER_SEARCH,
    
    // Playlist
    ADD_TO_PLAYLIST, CREATE_PLAYLIST, PLAY_PLAYLIST,
    
    // Navigation
    GO_HOME, GO_BACK, OPEN_SETTINGS,
    
    // Info
    CURRENT_INFO, SHOW_INFO, HIDE_INFO,
    
    // Special
    ADD_BOOKMARK, SET_SLEEP_TIMER, CANCEL_SLEEP_TIMER, TOGGLE_REPEAT, TOGGLE_SHUFFLE
}

enum class VoiceState {
    IDLE,
    READY,
    LISTENING,
    RECORDING,
    PROCESSING,
    ERROR
}

data class CommandResult(
    val success: Boolean,
    val message: String,
    val action: VoiceCommandAction? = null
)

sealed class VoiceCommandAction {
    data class SEARCH(val query: String) : VoiceCommandAction()
    data class FILTER(val filters: SearchFilters) : VoiceCommandAction()
    data class PLAY_VIDEO(val video: RecentFile) : VoiceCommandAction()
    data class SMART_SEARCH(val criteria: String, val filters: SearchFilters) : VoiceCommandAction()
    data class UNKNOWN(val text: String) : VoiceCommandAction()
}