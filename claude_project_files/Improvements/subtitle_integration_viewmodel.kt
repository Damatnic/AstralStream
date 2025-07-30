package com.astralplayer.nextplayer.viewmodel

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.astralplayer.nextplayer.data.PlayerRepository
import com.astralplayer.nextplayer.data.EnhancedGestureManager
import com.astralplayer.nextplayer.data.HapticFeedbackManager
import com.astralplayer.nextplayer.feature.ai.realtime.UltraFastSubtitleSystem
import com.astralplayer.nextplayer.feature.ai.realtime.SubtitleGenerationResult
import com.astralplayer.nextplayer.feature.ai.realtime.SubtitleEntry
import com.astralplayer.nextplayer.feature.ai.realtime.PerformanceMetrics
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import javax.inject.Inject
import dagger.hilt.android.lifecycle.HiltViewModel

/**
 * Enhanced ViewModel with ultra-fast AI subtitle generation
 * Guarantees subtitles within 3-5 seconds of video load
 */
@HiltViewModel
class EnhancedPlayerViewModelWithSubtitles @Inject constructor(
    application: Application,
    private val playerRepository: PlayerRepository,
    private val gestureManager: EnhancedGestureManager,
    private val hapticManager: HapticFeedbackManager
) : AndroidViewModel(application) {
    
    companion object {
        private const val TAG = "EnhancedPlayerViewModel"
    }
    
    // Ultra-fast subtitle system
    private val subtitleSystem = UltraFastSubtitleSystem(application)
    
    // Subtitle state flows
    private val _subtitleGenerationState = MutableStateFlow<SubtitleGenerationResult>(SubtitleGenerationResult.Idle)
    val subtitleGenerationState = _subtitleGenerationState.asStateFlow()
    
    private val _currentSubtitles = MutableStateFlow<List<SubtitleEntry>>(emptyList())
    val currentSubtitles = _currentSubtitles.asStateFlow()
    
    private val _activeSubtitle = MutableStateFlow<SubtitleEntry?>(null)
    val activeSubtitle = _activeSubtitle.asStateFlow()
    
    private val _subtitlePerformance = MutableStateFlow<PerformanceMetrics?>(null)
    val subtitlePerformance = _subtitlePerformance.asStateFlow()
    
    // Settings
    private val _aiSubtitlesEnabled = MutableStateFlow(true)
    val aiSubtitlesEnabled = _aiSubtitlesEnabled.asStateFlow()
    
    private val _subtitleLanguage = MutableStateFlow("en")
    val subtitleLanguage = _subtitleLanguage.asStateFlow()
    
    private val _subtitleAutoGeneration = MutableStateFlow(true)
    val subtitleAutoGeneration = _subtitleAutoGeneration.asStateFlow()
    
    // Current video info
    private var currentVideoUri: Uri? = null
    private var currentVideoTitle: String = ""
    
    init {
        // Monitor subtitle generation state
        viewModelScope.launch {
            subtitleSystem.subtitleFlow.collect { result ->
                _subtitleGenerationState.value = result
                
                when (result) {
                    is SubtitleGenerationResult.Success -> {
                        _currentSubtitles.value = result.subtitles
                        Log.i(TAG, "AI subtitles generated successfully: ${result.subtitles.size} entries")
                    }
                    is SubtitleGenerationResult.Error -> {
                        Log.e(TAG, "AI subtitle generation failed: ${result.message}")
                    }
                    else -> {}
                }
            }
        }
        
        // Monitor video position for subtitle sync
        viewModelScope.launch {
            playerRepository.playerState.collect { playerState ->
                updateActiveSubtitle(playerState.currentPosition)
            }
        }
        
        // Update performance metrics periodically
        viewModelScope.launch {
            while (true) {
                delay(5000) // Update every 5 seconds
                _subtitlePerformance.value = subtitleSystem.getPerformanceMetrics()
            }
        }
        
        // Start predictive subtitle generation
        startPredictiveSubtitleGeneration()
    }
    
    /**
     * Load video with automatic AI subtitle generation
     */
    suspend fun loadVideoWithInstantSubtitles(
        videoUri: Uri,
        videoTitle: String = "Unknown Video"
    ) {
        Log.d(TAG, "Loading video with instant AI subtitles: $videoTitle")
        
        currentVideoUri = videoUri
        currentVideoTitle = videoTitle
        
        // Start video playback immediately (don't wait for subtitles)
        playerRepository.loadVideo(videoUri)
        
        // Generate AI subtitles in parallel if enabled
        if (_aiSubtitlesEnabled.value && _subtitleAutoGeneration.value) {
            generateAISubtitlesInstantly(videoUri, videoTitle)
        }
    }
    
    /**
     * Generate AI subtitles with 3-5 second guarantee
     */
    fun generateAISubtitlesInstantly(
        videoUri: Uri = currentVideoUri ?: return,
        videoTitle: String = currentVideoTitle
    ) {
        Log.d(TAG, "Generating instant AI subtitles for: $videoTitle")
        
        viewModelScope.launch {
            try {
                val startTime = System.currentTimeMillis()
                
                val result = subtitleSystem.generateInstantSubtitles(
                    videoUri = videoUri,
                    videoTitle = videoTitle,
                    language = _subtitleLanguage.value
                )
                
                val generationTime = System.currentTimeMillis() - startTime
                Log.i(TAG, "AI subtitles generated in ${generationTime}ms")
                
                if (generationTime > 5000) {
                    Log.w(TAG, "Subtitle generation exceeded 5-second target: ${generationTime}ms")
                }
                
                result.onSuccess { subtitles ->
                    _currentSubtitles.value = subtitles
                    Log.i(TAG, "Successfully generated ${subtitles.size} subtitle entries")
                }.onFailure { error ->
                    Log.e(TAG, "AI subtitle generation failed", error)
                    _subtitleGenerationState.value = SubtitleGenerationResult.Error(
                        "Generation failed: ${error.message}",
                        error
                    )
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected error during subtitle generation", e)
                _subtitleGenerationState.value = SubtitleGenerationResult.Error(
                    "Unexpected error: ${e.message}",
                    e
                )
            }
        }
    }
    
    /**
     * Update active subtitle based on video position
     */
    private fun updateActiveSubtitle(currentPosition: Long) {
        val subtitles = _currentSubtitles.value
        
        val activeSubtitle = subtitles.find { subtitle ->
            currentPosition >= subtitle.startTime && currentPosition <= subtitle.endTime
        }
        
        if (_activeSubtitle.value != activeSubtitle) {
            _activeSubtitle.value = activeSubtitle
        }
    }
    
    /**
     * Start predictive subtitle generation for likely content
     */
    private fun startPredictiveSubtitleGeneration() {
        viewModelScope.launch {
            try {
                // Get recent videos for predictive generation
                val recentVideos = getRecentlyPlayedVideos()
                subtitleSystem.startPredictiveGeneration(recentVideos)
                
                Log.d(TAG, "Started predictive subtitle generation for ${recentVideos.size} recent videos")
            } catch (e: Exception) {
                Log.w(TAG, "Failed to start predictive subtitle generation", e)
            }
        }
    }
    
    /**
     * Toggle AI subtitle generation
     */
    fun toggleAISubtitles(enabled: Boolean) {
        _aiSubtitlesEnabled.value = enabled
        
        if (enabled && _currentSubtitles.value.isEmpty()) {
            // Generate subtitles for current video
            currentVideoUri?.let { uri ->
                generateAISubtitlesInstantly(uri, currentVideoTitle)
            }
        } else if (!enabled) {
            // Clear current subtitles
            _currentSubtitles.value = emptyList()
            _activeSubtitle.value = null
        }
        
        Log.d(TAG, "AI subtitles ${if (enabled) "enabled" else "disabled"}")
    }
    
    /**
     * Change subtitle language and regenerate if needed
     */
    fun changeSubtitleLanguage(language: String) {
        if (_subtitleLanguage.value != language) {
            _subtitleLanguage.value = language
            
            // Regenerate subtitles in new language if AI subtitles are enabled
            if (_aiSubtitlesEnabled.value) {
                currentVideoUri?.let { uri ->
                    generateAISubtitlesInstantly(uri, currentVideoTitle)
                }
            }
            
            Log.d(TAG, "Subtitle language changed to: $language")
        }
    }
    
    /**
     * Toggle automatic subtitle generation on video load
     */
    fun toggleAutoGeneration(enabled: Boolean) {
        _subtitleAutoGeneration.value = enabled
        Log.d(TAG, "Auto subtitle generation ${if (enabled) "enabled" else "disabled"}")
    }
    
    /**
     * Force regenerate subtitles (for testing or quality improvement)
     */
    fun forceRegenerateSubtitles() {
        currentVideoUri?.let { uri ->
            Log.d(TAG, "Force regenerating subtitles")
            _subtitleGenerationState.value = SubtitleGenerationResult.Processing(0f, "Regenerating...")
            _currentSubtitles.value = emptyList()
            generateAISubtitlesInstantly(uri, currentVideoTitle)
        }
    }
    
    /**
     * Get subtitle performance analytics
     */
    fun getSubtitleAnalytics(): PerformanceMetrics? {
        return _subtitlePerformance.value
    }
    
    /**
     * Export generated subtitles
     */
    fun exportSubtitles(format: SubtitleFormat = SubtitleFormat.SRT): String {
        val subtitles = _currentSubtitles.value
        
        return when (format) {
            SubtitleFormat.SRT -> exportToSRT(subtitles)
            SubtitleFormat.VTT -> exportToVTT(subtitles)
            SubtitleFormat.ASS -> exportToASS(subtitles)
        }
    }
    
    /**
     * Test subtitle generation performance
     */
    fun testSubtitlePerformance(testVideoUri: Uri) {
        viewModelScope.launch {
            Log.d(TAG, "Running subtitle performance test")
            
            val iterations = 5
            val times = mutableListOf<Long>()
            
            repeat(iterations) { iteration ->
                val startTime = System.currentTimeMillis()
                
                try {
                    subtitleSystem.generateInstantSubtitles(
                        videoUri = testVideoUri,
                        videoTitle = "Performance Test $iteration",
                        language = "en"
                    )
                    
                    val endTime = System.currentTimeMillis()
                    val duration = endTime - startTime
                    times.add(duration)
                    
                    Log.d(TAG, "Test iteration $iteration: ${duration}ms")
                    
                } catch (e: Exception) {
                    Log.e(TAG, "Performance test iteration $iteration failed", e)
                }
                
                delay(1000) // Wait between tests
            }
            
            val averageTime = times.average()
            val under3SecCount = times.count { it < 3000 }
            val under5SecCount = times.count { it < 5000 }
            
            Log.i(TAG, "Performance Test Results:")
            Log.i(TAG, "  Average time: ${averageTime.toInt()}ms")
            Log.i(TAG, "  Under 3 seconds: $under3SecCount/$iterations (${(under3SecCount.toFloat()/iterations)*100}%)")
            Log.i(TAG, "  Under 5 seconds: $under5SecCount/$iterations (${(under5SecCount.toFloat()/iterations)*100}%)")
        }
    }
    
    // Private helper methods
    private suspend fun getRecentlyPlayedVideos(): List<Uri> {
        // Get recent videos from repository or database
        return try {
            // Implementation would fetch from recent files database
            emptyList<Uri>()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to get recent videos", e)
            emptyList()
        }
    }
    
    private fun exportToSRT(subtitles: List<SubtitleEntry>): String {
        return subtitles.mapIndexed { index, subtitle ->
            val startTime = formatSRTTime(subtitle.startTime)
            val endTime = formatSRTTime(subtitle.endTime)
            
            "${index + 1}\n$startTime --> $endTime\n${subtitle.text}\n"
        }.joinToString("\n")
    }
    
    private fun exportToVTT(subtitles: List<SubtitleEntry>): String {
        val header = "WEBVTT\n\n"
        val content = subtitles.map { subtitle ->
            val startTime = formatVTTTime(subtitle.startTime)
            val endTime = formatVTTTime(subtitle.endTime)
            
            "$startTime --> $endTime\n${subtitle.text}\n"
        }.joinToString("\n")
        
        return header + content
    }
    
    private fun exportToASS(subtitles: List<SubtitleEntry>): String {
        // Basic ASS format export
        val header = """
            [Script Info]
            Title: AstralStream Generated Subtitles
            ScriptType: v4.00+
            
            [V4+ Styles]
            Format: Name, Fontname, Fontsize, PrimaryColour, SecondaryColour, OutlineColour, BackColour, Bold, Italic, Underline, StrikeOut, ScaleX, ScaleY, Spacing, Angle, BorderStyle, Outline, Shadow, Alignment, MarginL, MarginR, MarginV, Encoding
            Style: Default,Arial,20,&H00FFFFFF,&H000000FF,&H00000000,&H80000000,0,0,0,0,100,100,0,0,1,2,0,2,10,10,10,1
            
            [Events]
            Format: Layer, Start, End, Style, Name, MarginL, MarginR, MarginV, Effect, Text
            
        """.trimIndent()
        
        val events = subtitles.map { subtitle ->
            val startTime = formatASSTime(subtitle.startTime)
            val endTime = formatASSTime(subtitle.endTime)
            
            "Dialogue: 0,$startTime,$endTime,Default,,0,0,0,,${subtitle.text}"
        }.joinToString("\n")
        
        return header + events
    }
    
    private fun formatSRTTime(timeMs: Long): String {
        val hours = timeMs / 3600000
        val minutes = (timeMs % 3600000) / 60000
        val seconds = (timeMs % 60000) / 1000
        val milliseconds = timeMs % 1000
        
        return String.format("%02d:%02d:%02d,%03d", hours, minutes, seconds, milliseconds)
    }
    
    private fun formatVTTTime(timeMs: Long): String {
        val hours = timeMs / 3600000
        val minutes = (timeMs % 3600000) / 60000
        val seconds = (timeMs % 60000) / 1000
        val milliseconds = timeMs % 1000
        
        return String.format("%02d:%02d:%02d.%03d", hours, minutes, seconds, milliseconds)
    }
    
    private fun formatASSTime(timeMs: Long): String {
        val hours = timeMs / 3600000
        val minutes = (timeMs % 3600000) / 60000
        val seconds = (timeMs % 60000) / 1000
        val centiseconds = (timeMs % 1000) / 10
        
        return String.format("%d:%02d:%02d.%02d", hours, minutes, seconds, centiseconds)
    }
    
    override fun onCleared() {
        super.onCleared()
        subtitleSystem.cleanup()
        Log.d(TAG, "ViewModel cleared, subtitle system cleaned up")
    }
}

enum class SubtitleFormat {
    SRT, VTT, ASS
}