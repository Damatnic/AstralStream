package com.astralplayer.nextplayer.feature.subtitles

import java.util.regex.Pattern

object SrtParser {
    private val TIME_REGEX = Pattern.compile("(\\d{2}):(\\d{2}):(\\d{2}),(\\d{3})")
    private val SEQUENCE_REGEX = Pattern.compile("^\\d+$")
    
    fun parse(content: String): List<SubtitleCue> {
        val cues = mutableListOf<SubtitleCue>()
        val lines = content.lines()
        var i = 0
        var cueId = 1
        
        while (i < lines.size) {
            val line = lines[i].trim()
            
            // Skip empty lines
            if (line.isEmpty()) {
                i++
                continue
            }
            
            // Check if this is a sequence number
            if (SEQUENCE_REGEX.matcher(line).matches()) {
                i++ // Move to timing line
                
                if (i < lines.size) {
                    val timingLine = lines[i].trim()
                    val timingParts = timingLine.split(" --> ")
                    
                    if (timingParts.size == 2) {
                        val startTime = parseTimeToMillis(timingParts[0].trim())
                        val endTime = parseTimeToMillis(timingParts[1].trim())
                        
                        if (startTime != null && endTime != null) {
                            i++ // Move to text lines
                            
                            val textBuilder = StringBuilder()
                            while (i < lines.size && lines[i].trim().isNotEmpty()) {
                                if (textBuilder.isNotEmpty()) {
                                    textBuilder.append("\n")
                                }
                                textBuilder.append(lines[i].trim())
                                i++
                            }
                            
                            if (textBuilder.isNotEmpty()) {
                                cues.add(
                                    SubtitleCue(
                                        id = "srt_${cueId++}",
                                        startTime = startTime,
                                        endTime = endTime,
                                        text = textBuilder.toString()
                                    )
                                )
                            }
                        }
                    }
                }
            }
            i++
        }
        
        return cues
    }
    
    private fun parseTimeToMillis(time: String): Long? {
        val matcher = TIME_REGEX.matcher(time)
        return if (matcher.find()) {
            val hours = matcher.group(1)?.toLong() ?: 0L
            val minutes = matcher.group(2)?.toLong() ?: 0L
            val seconds = matcher.group(3)?.toLong() ?: 0L
            val millis = matcher.group(4)?.toLong() ?: 0L
            
            hours * 3600000 + minutes * 60000 + seconds * 1000 + millis
        } else {
            null
        }
    }
}

object AssParser {
    private val DIALOGUE_REGEX = Pattern.compile("Dialogue:\\s*\\d+,([^,]+),([^,]+),[^,]+,[^,]+,\\d+,\\d+,\\d+,[^,]*,(.+)")
    
    fun parse(content: String): List<SubtitleCue> {
        val cues = mutableListOf<SubtitleCue>()
        val lines = content.lines()
        var cueId = 1
        
        for (line in lines) {
            if (line.startsWith("Dialogue:")) {
                val matcher = DIALOGUE_REGEX.matcher(line)
                if (matcher.find()) {
                    val startTime = parseAssTime(matcher.group(1) ?: "")
                    val endTime = parseAssTime(matcher.group(2) ?: "")
                    val text = matcher.group(3) ?: ""
                        .replace("\\N", "\n")
                        .replace("\\n", "\n")
                        .replace("{\\i1}", "")
                        .replace("{\\i0}", "")
                        .replace("{\\b1}", "")
                        .replace("{\\b0}", "")
                        .replace(Regex("\\{[^}]*\\}"), "") // Remove all other style tags
                    
                    if (startTime != null && endTime != null) {
                        cues.add(
                            SubtitleCue(
                                id = "ass_${cueId++}",
                                startTime = startTime,
                                endTime = endTime,
                                text = text.trim()
                            )
                        )
                    }
                }
            }
        }
        
        return cues
    }
    
    private fun parseAssTime(time: String): Long? {
        val parts = time.split(":")
        if (parts.size == 3) {
            try {
                val hours = parts[0].toInt()
                val minutes = parts[1].toInt()
                val secondsParts = parts[2].split(".")
                if (secondsParts.size == 2) {
                    val seconds = secondsParts[0].toInt()
                    val centiseconds = secondsParts[1].toInt()
                    return hours * 3600000L + minutes * 60000L + seconds * 1000L + centiseconds * 10L
                }
            } catch (e: NumberFormatException) {
                return null
            }
        }
        return null
    }
}

object TtmlParser {
    fun parse(content: String): List<SubtitleCue> {
        val cues = mutableListOf<SubtitleCue>()
        val pattern = Pattern.compile("<p[^>]*begin=\"([^\"]+)\"[^>]*end=\"([^\"]+)\"[^>]*>([^<]+)</p>")
        val matcher = pattern.matcher(content)
        var cueId = 1
        
        while (matcher.find()) {
            val startTime = parseTtmlTime(matcher.group(1) ?: "")
            val endTime = parseTtmlTime(matcher.group(2) ?: "")
            val text = matcher.group(3) ?: ""
                .replace("<br />", "\n")
                .replace("<br/>", "\n")
                .replace(Regex("<[^>]+>"), "") // Remove HTML tags
                .trim()
            
            if (startTime != null && endTime != null) {
                cues.add(
                    SubtitleCue(
                        id = "ttml_${cueId++}",
                        startTime = startTime,
                        endTime = endTime,
                        text = text
                    )
                )
            }
        }
        
        return cues
    }
    
    private fun parseTtmlTime(time: String): Long? {
        // Parse various TTML time formats
        return when {
            time.endsWith("s") -> {
                // Seconds format: "12.345s"
                try {
                    (time.dropLast(1).toDouble() * 1000).toLong()
                } catch (e: NumberFormatException) {
                    null
                }
            }
            time.contains(":") -> {
                // Clock format: "00:01:23.456" or "00:01:23:456"
                val parts = time.replace(":", ".").split(".")
                if (parts.size >= 3) {
                    try {
                        val hours = parts[0].toLong()
                        val minutes = parts[1].toLong()
                        val seconds = parts[2].toLong()
                        val millis = if (parts.size > 3) parts[3].toLong() else 0
                        hours * 3600000 + minutes * 60000 + seconds * 1000 + millis
                    } catch (e: NumberFormatException) {
                        null
                    }
                } else {
                    null
                }
            }
            else -> null
        }
    }
}

object VttParser {
    private val TIME_REGEX = Pattern.compile("(\\d{2}):(\\d{2}):(\\d{2})\\.(\\d{3})")
    private val SHORT_TIME_REGEX = Pattern.compile("(\\d{2}):(\\d{2})\\.(\\d{3})")
    
    fun parse(content: String): List<SubtitleCue> {
        val cues = mutableListOf<SubtitleCue>()
        val lines = content.lines()
        var i = 0
        var cueId = 1
        
        // Skip WEBVTT header
        if (i < lines.size && lines[i].trim().startsWith("WEBVTT")) {
            i++
        }
        
        while (i < lines.size) {
            val line = lines[i].trim()
            
            // Skip empty lines and comments
            if (line.isEmpty() || line.startsWith("NOTE")) {
                i++
                continue
            }
            
            // Check for timing line
            if (line.contains(" --> ")) {
                val timingParts = line.split(" --> ")
                
                if (timingParts.size >= 2) {
                    val startTime = parseVttTime(timingParts[0].trim())
                    val endTime = parseVttTime(timingParts[1].split(" ")[0].trim()) // Remove cue settings
                    
                    if (startTime != null && endTime != null) {
                        i++ // Move to text lines
                        
                        val textBuilder = StringBuilder()
                        while (i < lines.size && lines[i].trim().isNotEmpty() && !lines[i].contains(" --> ")) {
                            if (textBuilder.isNotEmpty()) {
                                textBuilder.append("\n")
                            }
                            textBuilder.append(lines[i].trim())
                            i++
                        }
                        
                        if (textBuilder.isNotEmpty()) {
                            val text = textBuilder.toString()
                                .replace("<v[^>]*>", "") // Remove voice tags
                                .replace("</v>", "")
                                .replace("<c[^>]*>", "") // Remove class tags
                                .replace("</c>", "")
                                .replace(Regex("<[^>]+>"), "") // Remove other tags
                            
                            cues.add(
                                SubtitleCue(
                                    id = "vtt_${cueId++}",
                                    startTime = startTime,
                                    endTime = endTime,
                                    text = text.trim()
                                )
                            )
                        }
                        continue
                    }
                }
            }
            i++
        }
        
        return cues
    }
    
    private fun parseVttTime(time: String): Long? {
        // Try full format first
        var matcher = TIME_REGEX.matcher(time)
        if (matcher.find()) {
            val hours = matcher.group(1)?.toLong() ?: 0L
            val minutes = matcher.group(2)?.toLong() ?: 0L
            val seconds = matcher.group(3)?.toLong() ?: 0L
            val millis = matcher.group(4)?.toLong() ?: 0L
            
            return hours * 3600000 + minutes * 60000 + seconds * 1000 + millis
        }
        
        // Try short format
        matcher = SHORT_TIME_REGEX.matcher(time)
        if (matcher.find()) {
            val minutes = matcher.group(1)?.toLong() ?: 0L
            val seconds = matcher.group(2)?.toLong() ?: 0L
            val millis = matcher.group(3)?.toLong() ?: 0L
            
            return minutes * 60000 + seconds * 1000 + millis
        }
        
        return null
    }
}