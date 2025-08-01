package com.astralplayer.community.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.astralplayer.community.data.SubtitleContributionSource
import com.astralplayer.community.repository.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SubtitleContributionViewModel @Inject constructor(
    private val repository: SubtitleContributionRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(SubtitleContributionUiState())
    val uiState: StateFlow<SubtitleContributionUiState> = _uiState.asStateFlow()
    
    fun initializeForVideo(videoHash: String, videoTitle: String, videoDuration: Long) {
        _uiState.update {
            it.copy(
                videoHash = videoHash,
                videoTitle = videoTitle,
                videoDuration = videoDuration
            )
        }
    }
    
    fun selectLanguage(language: String, languageCode: String) {
        _uiState.update {
            it.copy(
                selectedLanguage = language,
                selectedLanguageCode = languageCode
            )
        }
    }
    
    fun selectFormat(format: String) {
        _uiState.update { it.copy(format = format) }
    }
    
    fun updateContent(content: String) {
        _uiState.update { it.copy(content = content) }
        validateContent(content)
        calculateQualityMetrics(content)
    }
    
    fun updateDescription(description: String) {
        _uiState.update { it.copy(description = description) }
    }
    
    fun updateConfidence(confidence: Float) {
        _uiState.update { it.copy(confidence = confidence) }
    }
    
    fun updateTagsInput(tagsInput: String) {
        _uiState.update { it.copy(tagsInput = tagsInput) }
    }
    
    fun contribute() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            val state = _uiState.value
            val tags = state.tagsInput.split(",").map { it.trim() }.filter { it.isNotBlank() }
            
            val result = repository.contributeSubtitle(
                videoHash = state.videoHash,
                videoTitle = state.videoTitle,
                videoDuration = state.videoDuration,
                language = state.selectedLanguage,
                languageCode = state.selectedLanguageCode,
                content = state.content,
                format = state.format,
                confidence = state.confidence,
                description = state.description,
                tags = tags,
                source = SubtitleContributionSource.USER_CREATED
            )
            
            _uiState.update {
                it.copy(
                    isLoading = false,
                    contributionResult = result,
                    error = if (result.isFailure) result.exceptionOrNull()?.message else null
                )
            }
        }
    }
    
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
    
    private fun validateContent(content: String) {
        val errors = mutableListOf<String>()
        
        if (content.isBlank()) {
            errors.add("Subtitle content cannot be empty")
        }
        
        when (_uiState.value.format) {
            "srt" -> {
                if (!content.contains("-->")) {
                    errors.add("Invalid SRT format: missing timestamp markers (-->)")
                }
                if (!content.matches(Regex(".*\\d+\\s*\\n.*", RegexOption.DOT_MATCHES_ALL))) {
                    errors.add("Invalid SRT format: missing subtitle numbers")
                }
            }
            "vtt" -> {
                if (!content.startsWith("WEBVTT")) {
                    errors.add("Invalid VTT format: must start with WEBVTT")
                }
            }
            "ass" -> {
                if (!content.contains("[Script Info]")) {
                    errors.add("Invalid ASS format: missing [Script Info] section")
                }
            }
        }
        
        _uiState.update {
            it.copy(
                validationErrors = errors,
                canContribute = errors.isEmpty() && content.isNotBlank()
            )
        }
    }
    
    private fun calculateQualityMetrics(content: String) {
        if (content.isBlank()) {
            _uiState.update { it.copy(qualityMetrics = null) }
            return
        }
        
        val lines = content.lines()
        val textLines = lines.filter { line ->
            line.isNotBlank() && 
            !line.contains("-->") && 
            !line.matches(Regex("^\\d+$")) &&
            !line.startsWith("WEBVTT") &&
            !line.contains("[") &&
            !line.contains("]")
        }
        
        val lineCount = textLines.size
        val avgLineLength = if (textLines.isNotEmpty()) {
            textLines.sumOf { it.length }.toFloat() / textLines.size
        } else 0f
        
        // Check for timing issues
        val hasTimingIssues = content.contains("00:00:00.000 --> 00:00:00.000")
        
        // Calculate completion percentage based on subtitle density
        val expectedSubtitlesPerMinute = 20
        val durationMinutes = _uiState.value.videoDuration / 60000
        val expectedSubtitles = durationMinutes * expectedSubtitlesPerMinute
        val completionPercentage = if (expectedSubtitles > 0) {
            (lineCount.toFloat() / expectedSubtitles * 100).coerceIn(0f, 100f)
        } else 100f
        
        // Calculate overall quality score
        var score = 0.5f
        if (lineCount > 10) score += 0.1f
        if (avgLineLength in 20f..80f) score += 0.2f
        if (!hasTimingIssues) score += 0.1f
        if (completionPercentage > 80) score += 0.1f
        
        _uiState.update {
            it.copy(
                qualityMetrics = QualityMetrics(
                    lineCount = lineCount,
                    avgLineLength = avgLineLength,
                    hasTimingIssues = hasTimingIssues,
                    hasSpellingIssues = false, // Would use spell checker in production
                    completionPercentage = completionPercentage,
                    overallScore = score.coerceIn(0f, 1f)
                )
            )
        }
    }
}

data class SubtitleContributionUiState(
    val videoHash: String = "",
    val videoTitle: String = "",
    val videoDuration: Long = 0L,
    val selectedLanguage: String = "English",
    val selectedLanguageCode: String = "en",
    val format: String = "srt",
    val content: String = "",
    val description: String = "",
    val confidence: Float = 0.9f,
    val tagsInput: String = "",
    val qualityMetrics: QualityMetrics? = null,
    val validationErrors: List<String> = emptyList(),
    val canContribute: Boolean = false,
    val isLoading: Boolean = false,
    val contributionResult: Result<ContributionResult>? = null,
    val error: String? = null
)