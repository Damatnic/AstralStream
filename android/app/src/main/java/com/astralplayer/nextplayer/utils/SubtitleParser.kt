package com.astralplayer.nextplayer.utils

import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.InputStreamReader
import java.nio.charset.Charset
import java.util.regex.Pattern

/**
 * Data class representing a subtitle cue
 */
data class SubtitleCue(
    val id: String? = null,
    val startTimeMs: Long,
    val endTimeMs: Long,
    val text: String
)

/**
 * Utility class for parsing subtitle files (SRT, VTT, ASS/SSA)
 */
object SubtitleParser {
    
    private val TIME_PATTERN_SRT = Pattern.compile("(\\d{2}):(\\d{2}):(\\d{2}),(\\d{3})")
    private val TIME_PATTERN_VTT = Pattern.compile("(\\d{2}):(\\d{2}):(\\d{2})\\.(\\d{3})")
    private val TIME_PATTERN_ASS = Pattern.compile("(\\d):(\\d{2}):(\\d{2})\\.(\\d{2})")
    
    /**
     * Parse a subtitle file and return a list of subtitle cues
     */
    fun parseSubtitleFile(file: File, charset: Charset = Charsets.UTF_8): List<SubtitleCue> {
        return when (file.extension.lowercase()) {
            "srt" -> parseSrt(file, charset)
            "vtt", "webvtt" -> parseVtt(file, charset)
            "ass", "ssa" -> parseAss(file, charset)
            else -> emptyList()
        }
    }
    
    /**
     * Parse SRT subtitle format
     */
    private fun parseSrt(file: File, charset: Charset): List<SubtitleCue> {
        val cues = mutableListOf<SubtitleCue>()
        
        BufferedReader(InputStreamReader(FileInputStream(file), charset)).use { reader ->
            var line: String?
            var id: String? = null
            var startTime: Long? = null
            var endTime: Long? = null
            val textBuilder = StringBuilder()
            
            while (reader.readLine().also { line = it } != null) {
                val trimmedLine = line!!.trim()
                
                when {
                    // Subtitle index
                    trimmedLine.toIntOrNull() != null && id == null -> {
                        id = trimmedLine
                    }
                    // Timecodes
                    trimmedLine.contains("-->") && startTime == null -> {
                        val times = trimmedLine.split("-->")
                        if (times.size == 2) {
                            startTime = parseSrtTime(times[0].trim())
                            endTime = parseSrtTime(times[1].trim())
                        }
                    }
                    // Text content
                    trimmedLine.isNotEmpty() && startTime != null -> {
                        if (textBuilder.isNotEmpty()) {
                            textBuilder.append("\n")
                        }
                        textBuilder.append(trimmedLine)
                    }
                    // Empty line - end of subtitle
                    trimmedLine.isEmpty() && startTime != null && endTime != null -> {
                        if (textBuilder.isNotEmpty()) {
                            cues.add(
                                SubtitleCue(
                                    id = id,
                                    startTimeMs = startTime!!,
                                    endTimeMs = endTime!!,
                                    text = textBuilder.toString()
                                )
                            )
                        }
                        // Reset for next subtitle
                        id = null
                        startTime = null
                        endTime = null
                        textBuilder.clear()
                    }
                }
            }
            
            // Handle last subtitle if file doesn't end with empty line
            if (startTime != null && endTime != null && textBuilder.isNotEmpty()) {
                cues.add(
                    SubtitleCue(
                        id = id,
                        startTimeMs = startTime!!,
                        endTimeMs = endTime!!,
                        text = textBuilder.toString()
                    )
                )
            }
        }
        
        return cues
    }
    
    /**
     * Parse WebVTT subtitle format
     */
    private fun parseVtt(file: File, charset: Charset): List<SubtitleCue> {
        val cues = mutableListOf<SubtitleCue>()
        
        BufferedReader(InputStreamReader(FileInputStream(file), charset)).use { reader ->
            var line: String?
            var headerPassed = false
            var id: String? = null
            var startTime: Long? = null
            var endTime: Long? = null
            val textBuilder = StringBuilder()
            
            while (reader.readLine().also { line = it } != null) {
                val trimmedLine = line!!.trim()
                
                // Skip VTT header
                if (!headerPassed) {
                    if (trimmedLine.startsWith("WEBVTT")) {
                        headerPassed = true
                        continue
                    }
                }
                
                when {
                    // Optional cue identifier
                    !trimmedLine.contains("-->") && !trimmedLine.isEmpty() && startTime == null -> {
                        id = trimmedLine
                    }
                    // Timecodes
                    trimmedLine.contains("-->") -> {
                        val times = trimmedLine.split("-->")
                        if (times.size >= 2) {
                            startTime = parseVttTime(times[0].trim())
                            endTime = parseVttTime(times[1].trim().split(" ")[0]) // Remove cue settings
                        }
                    }
                    // Text content
                    trimmedLine.isNotEmpty() && startTime != null -> {
                        if (textBuilder.isNotEmpty()) {
                            textBuilder.append("\n")
                        }
                        textBuilder.append(trimmedLine)
                    }
                    // Empty line - end of subtitle
                    trimmedLine.isEmpty() && startTime != null && endTime != null -> {
                        if (textBuilder.isNotEmpty()) {
                            cues.add(
                                SubtitleCue(
                                    id = id,
                                    startTimeMs = startTime!!,
                                    endTimeMs = endTime!!,
                                    text = cleanVttText(textBuilder.toString())
                                )
                            )
                        }
                        // Reset for next subtitle
                        id = null
                        startTime = null
                        endTime = null
                        textBuilder.clear()
                    }
                }
            }
            
            // Handle last subtitle
            if (startTime != null && endTime != null && textBuilder.isNotEmpty()) {
                cues.add(
                    SubtitleCue(
                        id = id,
                        startTimeMs = startTime!!,
                        endTimeMs = endTime!!,
                        text = cleanVttText(textBuilder.toString())
                    )
                )
            }
        }
        
        return cues
    }
    
    /**
     * Parse ASS/SSA subtitle format
     */
    private fun parseAss(file: File, charset: Charset): List<SubtitleCue> {
        val cues = mutableListOf<SubtitleCue>()
        
        BufferedReader(InputStreamReader(FileInputStream(file), charset)).use { reader ->
            var line: String?
            var inEvents = false
            var formatLine: String? = null
            
            while (reader.readLine().also { line = it } != null) {
                val trimmedLine = line!!.trim()
                
                when {
                    trimmedLine == "[Events]" -> {
                        inEvents = true
                    }
                    inEvents && trimmedLine.startsWith("Format:") -> {
                        formatLine = trimmedLine.substring(7).trim()
                    }
                    inEvents && trimmedLine.startsWith("Dialogue:") && formatLine != null -> {
                        val cue = parseAssDialogue(trimmedLine.substring(9).trim(), formatLine)
                        cue?.let { cues.add(it) }
                    }
                }
            }
        }
        
        return cues
    }
    
    private fun parseAssDialogue(dialogueLine: String, formatLine: String): SubtitleCue? {
        val formatFields = formatLine.split(",").map { it.trim() }
        val dialogueFields = dialogueLine.split(",", limit = formatFields.size)
        
        if (dialogueFields.size < formatFields.size) return null
        
        var startTime: Long? = null
        var endTime: Long? = null
        var text: String? = null
        
        formatFields.forEachIndexed { index, field ->
            when (field) {
                "Start" -> startTime = parseAssTime(dialogueFields[index].trim())
                "End" -> endTime = parseAssTime(dialogueFields[index].trim())
                "Text" -> {
                    // Join remaining fields as they might contain commas
                    text = dialogueFields.subList(index, dialogueFields.size).joinToString(",")
                    return@forEachIndexed
                }
            }
        }
        
        return if (startTime != null && endTime != null && !text.isNullOrEmpty()) {
            SubtitleCue(
                startTimeMs = startTime!!,
                endTimeMs = endTime!!,
                text = cleanAssText(text!!)
            )
        } else {
            null
        }
    }
    
    private fun parseSrtTime(timeString: String): Long? {
        val matcher = TIME_PATTERN_SRT.matcher(timeString)
        return if (matcher.find()) {
            val hours = matcher.group(1)?.toIntOrNull() ?: 0
            val minutes = matcher.group(2)?.toIntOrNull() ?: 0
            val seconds = matcher.group(3)?.toIntOrNull() ?: 0
            val millis = matcher.group(4)?.toIntOrNull() ?: 0
            
            (hours * 3600000L) + (minutes * 60000L) + (seconds * 1000L) + millis
        } else {
            null
        }
    }
    
    private fun parseVttTime(timeString: String): Long? {
        val matcher = TIME_PATTERN_VTT.matcher(timeString)
        return if (matcher.find()) {
            val hours = matcher.group(1)?.toIntOrNull() ?: 0
            val minutes = matcher.group(2)?.toIntOrNull() ?: 0
            val seconds = matcher.group(3)?.toIntOrNull() ?: 0
            val millis = matcher.group(4)?.toIntOrNull() ?: 0
            
            (hours * 3600000L) + (minutes * 60000L) + (seconds * 1000L) + millis
        } else {
            null
        }
    }
    
    private fun parseAssTime(timeString: String): Long? {
        val matcher = TIME_PATTERN_ASS.matcher(timeString)
        return if (matcher.find()) {
            val hours = matcher.group(1)?.toIntOrNull() ?: 0
            val minutes = matcher.group(2)?.toIntOrNull() ?: 0
            val seconds = matcher.group(3)?.toIntOrNull() ?: 0
            val centis = matcher.group(4)?.toIntOrNull() ?: 0
            
            (hours * 3600000L) + (minutes * 60000L) + (seconds * 1000L) + (centis * 10L)
        } else {
            null
        }
    }
    
    private fun cleanVttText(text: String): String {
        // Remove VTT tags like <c.class>, <v Speaker>, etc.
        return text
            .replace(Regex("<[^>]+>"), "")
            .replace(Regex("\\{[^}]+\\}"), "")
            .trim()
    }
    
    private fun cleanAssText(text: String): String {
        // Remove ASS style codes like {\pos(x,y)}, {\i1}, etc.
        return text
            .replace(Regex("\\{[^}]+\\}"), "")
            .replace("\\N", "\n")
            .replace("\\n", "\n")
            .trim()
    }
    
    /**
     * Find subtitle files for a video file
     */
    fun findSubtitleFiles(videoFile: File): List<File> {
        val videoNameWithoutExt = videoFile.nameWithoutExtension
        val parentDir = videoFile.parentFile ?: return emptyList()
        
        return parentDir.listFiles { file ->
            file.isFile && 
            file.nameWithoutExtension.startsWith(videoNameWithoutExt) &&
            file.extension.lowercase() in listOf("srt", "vtt", "webvtt", "ass", "ssa")
        }?.toList() ?: emptyList()
    }
}