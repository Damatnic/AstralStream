package com.astralplayer.nextplayer.data

import android.content.Context
import android.net.Uri
import com.astralplayer.nextplayer.utils.SubtitleCue
import com.astralplayer.nextplayer.utils.SubtitleParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Manages subtitle loading and synchronization
 */
class SubtitleManager(private val context: Context) {
    
    private val _currentSubtitle = MutableStateFlow<SubtitleCue?>(null)
    val currentSubtitle: StateFlow<SubtitleCue?> = _currentSubtitle.asStateFlow()
    
    private val _availableSubtitles = MutableStateFlow<List<SubtitleTrack>>(emptyList())
    val availableSubtitles: StateFlow<List<SubtitleTrack>> = _availableSubtitles.asStateFlow()
    
    private val _selectedTrack = MutableStateFlow<SubtitleTrack?>(null)
    val selectedTrack: StateFlow<SubtitleTrack?> = _selectedTrack.asStateFlow()
    
    private var subtitleCues: List<SubtitleCue> = emptyList()
    private var subtitleOffset: Long = 0L // Offset in milliseconds for manual sync
    
    data class SubtitleTrack(
        val id: String,
        val uri: Uri,
        val name: String,
        val language: String? = null,
        val isEmbedded: Boolean = false
    )
    
    /**
     * Load subtitles for a video URI
     */
    suspend fun loadSubtitlesForVideo(videoUri: Uri) = withContext(Dispatchers.IO) {
        val tracks = mutableListOf<SubtitleTrack>()
        
        // Try to find external subtitle files
        if (videoUri.scheme == "file") {
            val videoFile = File(videoUri.path!!)
            val subtitleFiles = SubtitleParser.findSubtitleFiles(videoFile)
            
            subtitleFiles.forEach { file ->
                tracks.add(
                    SubtitleTrack(
                        id = file.absolutePath,
                        uri = Uri.fromFile(file),
                        name = file.name,
                        language = extractLanguageFromFilename(file.name)
                    )
                )
            }
        }
        
        _availableSubtitles.value = tracks
    }
    
    /**
     * Select a subtitle track
     */
    suspend fun selectSubtitleTrack(track: SubtitleTrack?) = withContext(Dispatchers.IO) {
        _selectedTrack.value = track
        _currentSubtitle.value = null
        subtitleCues = emptyList()
        
        if (track != null) {
            loadSubtitleFile(track.uri)
        }
    }
    
    /**
     * Load a subtitle file
     */
    suspend fun loadSubtitleFile(uri: Uri) = withContext(Dispatchers.IO) {
        try {
            when (uri.scheme) {
                "file" -> {
                    val file = File(uri.path!!)
                    subtitleCues = SubtitleParser.parseSubtitleFile(file)
                }
                "content" -> {
                    // Handle content URIs
                    context.contentResolver.openInputStream(uri)?.use { inputStream ->
                        val tempFile = File.createTempFile("subtitle", ".tmp", context.cacheDir)
                        tempFile.outputStream().use { outputStream ->
                            inputStream.copyTo(outputStream)
                        }
                        subtitleCues = SubtitleParser.parseSubtitleFile(tempFile)
                        tempFile.delete()
                    }
                }
                else -> {
                    // Unsupported URI scheme
                    subtitleCues = emptyList()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            subtitleCues = emptyList()
        }
    }
    
    /**
     * Update current subtitle based on video position
     */
    fun updatePosition(positionMs: Long) {
        val adjustedPosition = positionMs + subtitleOffset
        
        val currentCue = subtitleCues.find { cue ->
            adjustedPosition >= cue.startTimeMs && adjustedPosition <= cue.endTimeMs
        }
        
        if (_currentSubtitle.value != currentCue) {
            _currentSubtitle.value = currentCue
        }
    }
    
    /**
     * Adjust subtitle synchronization offset
     */
    fun adjustSubtitleOffset(offsetMs: Long) {
        subtitleOffset = offsetMs
    }
    
    /**
     * Get subtitle offset
     */
    fun getSubtitleOffset(): Long = subtitleOffset
    
    /**
     * Add an external subtitle file
     */
    suspend fun addExternalSubtitleFile(uri: Uri, filename: String? = null) = withContext(Dispatchers.IO) {
        val name = filename ?: uri.lastPathSegment ?: "External Subtitle"
        val language = extractLanguageFromFilename(name)
        
        val newTrack = SubtitleTrack(
            id = uri.toString(),
            uri = uri,
            name = name,
            language = language,
            isEmbedded = false
        )
        
        // Add to available subtitles list
        val currentTracks = _availableSubtitles.value.toMutableList()
        currentTracks.add(newTrack)
        _availableSubtitles.value = currentTracks
        
        // Automatically select the new track
        selectSubtitleTrack(newTrack)
    }
    
    /**
     * Clear all subtitles
     */
    fun clear() {
        _currentSubtitle.value = null
        _availableSubtitles.value = emptyList()
        _selectedTrack.value = null
        subtitleCues = emptyList()
        subtitleOffset = 0L
    }
    
    /**
     * Extract language code from subtitle filename
     */
    private fun extractLanguageFromFilename(filename: String): String? {
        // Common patterns: movie.en.srt, movie_eng.srt, movie-english.srt
        val patterns = listOf(
            Regex("\\.([a-z]{2})\\.[^.]+$"), // .en.srt
            Regex("\\.([a-z]{3})\\.[^.]+$"), // .eng.srt
            Regex("[._-]([a-z]{2})[._-][^.]+$"), // _en.srt or -en.srt
            Regex("[._-]([a-z]{3})[._-][^.]+$") // _eng.srt or -eng.srt
        )
        
        for (pattern in patterns) {
            val match = pattern.find(filename.lowercase())
            if (match != null) {
                return match.groupValues[1]
            }
        }
        
        // Check for full language names
        val languages = mapOf(
            "english" to "en",
            "spanish" to "es",
            "french" to "fr",
            "german" to "de",
            "italian" to "it",
            "portuguese" to "pt",
            "russian" to "ru",
            "japanese" to "ja",
            "chinese" to "zh",
            "korean" to "ko"
        )
        
        val lowerFilename = filename.lowercase()
        for ((name, code) in languages) {
            if (lowerFilename.contains(name)) {
                return code
            }
        }
        
        return null
    }
}