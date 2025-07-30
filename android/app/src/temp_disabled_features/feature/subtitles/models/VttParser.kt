package com.astralplayer.nextplayer.feature.subtitles.models

/**
 * Parser for WebVTT format
 */
object VttParser {
    fun parse(content: String): List<SubtitleCue> {
        val cues = mutableListOf<SubtitleCue>()
        // Skip the WEBVTT header
        val contentWithoutHeader = content.substringAfter("WEBVTT").trim()

        val blocks = contentWithoutHeader.split("\n\n").filter { it.isNotBlank() }
        var index = 1

        for (block in blocks) {
            val lines = block.lines().filter { it.isNotBlank() }
            if (lines.size < 2) continue

            try {
                // Check if the first line is an index or timestamp
                var startLineIndex = 0
                if (!lines[0].contains("-->")) {
                    // This is a cue identifier, skip it
                    startLineIndex = 1
                }

                if (startLineIndex >= lines.size) continue

                val timecodes = lines[startLineIndex].split(" --> ")
                if (timecodes.size != 2) continue

                val startTime = parseTimecode(timecodes[0])
                val endTime = parseTimecode(timecodes[1])
                val text = lines.subList(startLineIndex + 1, lines.size).joinToString("\n")

                cues.add(SubtitleCue(index.toString(), startTime, endTime, text))
                index++
            } catch (e: Exception) {
                // Skip malformed entries
                continue
            }
        }

        return cues
    }

    private fun parseTimecode(timecode: String): Long {
        val cleanTimecode = timecode.trim()

        // Handle both 00:00:00.000 and 00:00.000 formats
        return if (cleanTimecode.count { it == ':' } == 1) {
            // Format is MM:SS.mmm
            val parts = cleanTimecode.split(":", ".")
            val minutes = parts[0].toLong()
            val seconds = parts[1].toLong()
            val milliseconds = if (parts.size > 2) parts[2].toLong() else 0

            minutes * 60000 + seconds * 1000 + milliseconds
        } else {
            // Format is HH:MM:SS.mmm
            val parts = cleanTimecode.split(":", ".")
            val hours = parts[0].toLong()
            val minutes = parts[1].toLong()
            val seconds = parts[2].toLong()
            val milliseconds = if (parts.size > 3) parts[3].toLong() else 0

            hours * 3600000 + minutes * 60000 + seconds * 1000 + milliseconds
        }
    }
}