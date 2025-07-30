package com.astralplayer.nextplayer.feature.subtitles

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.util.Log
import android.view.Gravity
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.astralplayer.nextplayer.R
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.nio.charset.StandardCharsets
import java.util.*
import kotlin.collections.ArrayList

/**
 * Advanced subtitle manager with support for various subtitle formats,
 * styling options, and synchronization features.
 */
class AdvancedSubtitleManager(private val context: Context) {
    // Subtitle display settings
    data class SubtitleStyle(
        var textSize: Float = 16f,
        var textColor: Int = Color.WHITE,
        var outlineColor: Int = Color.BLACK,
        var outlineWidth: Float = 2f,
        var backgroundColor: Int = Color.TRANSPARENT,
        var fontFamily: String = "sans-serif",
        var bold: Boolean = false,
        var italic: Boolean = false,
        var position: Int = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL,
        var verticalMargin: Int = 50 // Distance from bottom in dp
    )

    // Subtitle entry with timing and text
    data class SubtitleEntry(
        val startTimeMs: Long,
        val endTimeMs: Long,
        val text: String,
        val styleOverrides: Map<String, String>? = null
    )

    private var currentSubtitles = ArrayList<SubtitleEntry>()
    private var currentStyle = SubtitleStyle()
    private var subtitleOffset = 0L // Time adjustment in milliseconds
    private var listener: SubtitleDisplayListener? = null
    private var isEnabled = true

    /**
     * Loads subtitles from a file.
     * @param file The subtitle file
     * @return true if loading was successful, false otherwise
     */
    fun loadSubtitlesFromFile(file: File): Boolean {
        return try {
            val inputStream = FileInputStream(file)
            val extension = file.extension.lowercase(Locale.getDefault())

            val success = when (extension) {
                "srt" -> parseSrtSubtitles(inputStream)
                "vtt" -> parseVttSubtitles(inputStream)
                "ass" -> parseAssSubtitles(inputStream)
                else -> false
            }

            inputStream.close()
            success
        } catch (e: Exception) {
            Log.e(TAG, "Error loading subtitles: ${e.message}")
            false
        }
    }

    /**
     * Updates subtitle display based on current playback position.
     * @param positionMs Current playback position in milliseconds
     */
    fun updateSubtitles(positionMs: Long) {
        if (!isEnabled || currentSubtitles.isEmpty()) {
            listener?.onSubtitleTextUpdated(null)
            return
        }

        val adjustedPosition = positionMs + subtitleOffset

        // Find all subtitles that should be displayed at the current position
        val applicableSubtitles = currentSubtitles.filter { 
            adjustedPosition >= it.startTimeMs && adjustedPosition <= it.endTimeMs 
        }

        if (applicableSubtitles.isEmpty()) {
            listener?.onSubtitleTextUpdated(null)
        } else {
            // Join multiple subtitle entries if there are overlapping ones
            val text = applicableSubtitles.joinToString("\n") { it.text }
            listener?.onSubtitleTextUpdated(text)
        }
    }

    /**
     * Sets whether subtitles are enabled or disabled.
     */
    fun setEnabled(enabled: Boolean) {
        isEnabled = enabled
        if (!enabled) {
            listener?.onSubtitleTextUpdated(null)
        }
    }

    /**
     * Checks if subtitles are currently enabled.
     */
    fun isEnabled(): Boolean {
        return isEnabled
    }

    /**
     * Adjusts subtitle timing offset.
     * @param offsetMs Offset in milliseconds (positive = delay, negative = advance)
     */
    fun setSubtitleOffset(offsetMs: Long) {
        subtitleOffset = offsetMs
    }

    /**
     * Gets the current subtitle offset.
     */
    fun getSubtitleOffset(): Long {
        return subtitleOffset
    }

    /**
     * Applies the current subtitle style to the provided TextView.
     */
    fun applyStyleToTextView(textView: TextView) {
        textView.setTextSize(currentStyle.textSize)
        textView.setTextColor(currentStyle.textColor)

        // Set shadow for outline effect
        textView.setShadowLayer(
            currentStyle.outlineWidth,
            1.5f,
            1.5f,
            currentStyle.outlineColor
        )

        // Set background
        textView.setBackgroundColor(currentStyle.backgroundColor)

        // Set font style
        var typeface = when (currentStyle.fontFamily) {
            "sans-serif" -> Typeface.SANS_SERIF
            "serif" -> Typeface.SERIF
            "monospace" -> Typeface.MONOSPACE
            else -> Typeface.DEFAULT
        }

        // Apply bold and italic
        var style = Typeface.NORMAL
        if (currentStyle.bold) style = style or Typeface.BOLD
        if (currentStyle.italic) style = style or Typeface.ITALIC

        textView.typeface = Typeface.create(typeface, style)

        // Set position
        (textView.layoutParams as? androidx.constraintlayout.widget.ConstraintLayout.LayoutParams)?.let { params ->
            params.bottomMargin = dpToPx(currentStyle.verticalMargin)
            textView.layoutParams = params
        }

        textView.gravity = currentStyle.position
    }

    /**
     * Gets the current subtitle style.
     */
    fun getSubtitleStyle(): SubtitleStyle {
        return currentStyle
    }

    /**
     * Updates the subtitle style settings.
     */
    fun updateSubtitleStyle(style: SubtitleStyle) {
        currentStyle = style
        listener?.onSubtitleStyleChanged(style)
    }

    /**
     * Sets the listener for subtitle updates.
     */
    fun setListener(listener: SubtitleDisplayListener) {
        this.listener = listener
    }

    /**
     * Clears the current subtitles.
     */
    fun clearSubtitles() {
        currentSubtitles.clear()
        listener?.onSubtitleTextUpdated(null)
    }

    private fun parseSrtSubtitles(inputStream: InputStream): Boolean {
        try {
            val reader = inputStream.bufferedReader(StandardCharsets.UTF_8)
            val lines = reader.readLines()

            currentSubtitles.clear()
            var i = 0

            while (i < lines.size) {
                // Skip empty lines and subtitle numbers
                while (i < lines.size && lines[i].trim().isEmpty()) i++
                if (i >= lines.size) break

                // Skip subtitle number
                i++
                if (i >= lines.size) break

                // Parse time line
                val timeLine = lines[i]
                i++

                val timeMatch = SRT_TIME_PATTERN.find(timeLine) ?: continue
                val startTime = parseTimeString(timeMatch.groupValues[1])
                val endTime = parseTimeString(timeMatch.groupValues[2])

                // Parse text (can be multiple lines)
                val textBuilder = StringBuilder()
                while (i < lines.size && lines[i].isNotBlank()) {
                    if (textBuilder.isNotEmpty()) textBuilder.append("\n")
                    textBuilder.append(lines[i])
                    i++
                }

                currentSubtitles.add(SubtitleEntry(startTime, endTime, textBuilder.toString()))
            }

            return currentSubtitles.isNotEmpty()
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing SRT: ${e.message}")
            return false
        }
    }

    private fun parseVttSubtitles(inputStream: InputStream): Boolean {
        try {
            val reader = inputStream.bufferedReader(StandardCharsets.UTF_8)
            val lines = reader.readLines()

            currentSubtitles.clear()
            var i = 0

            // Skip WEBVTT header
            while (i < lines.size && !lines[i].startsWith("WEBVTT")) i++
            i++

            while (i < lines.size) {
                // Skip empty lines and cue identifiers
                while (i < lines.size && lines[i].trim().isEmpty()) i++
                if (i >= lines.size) break

                // Check if this line is a timestamp or an identifier
                if (!lines[i].contains("-->")) {
                    // This is likely a cue identifier, skip it
                    i++
                    if (i >= lines.size) break
                }

                // Parse time line
                val timeLine = lines[i]
                i++

                val timeMatch = VTT_TIME_PATTERN.find(timeLine) ?: continue
                val startTime = parseTimeString(timeMatch.groupValues[1])
                val endTime = parseTimeString(timeMatch.groupValues[2])

                // Extract style information if present
                val styleMap = mutableMapOf<String, String>()
                val styleSection = timeMatch.groupValues[3].trim()
                if (styleSection.isNotEmpty()) {
                    val styleParts = styleSection.split(" ")
                    for (part in styleParts) {
                        val keyValue = part.split(":", limit = 2)
                        if (keyValue.size == 2) {
                            styleMap[keyValue[0]] = keyValue[1]
                        }
                    }
                }

                // Parse text (can be multiple lines)
                val textBuilder = StringBuilder()
                while (i < lines.size && lines[i].isNotBlank()) {
                    if (textBuilder.isNotEmpty()) textBuilder.append("\n")
                    textBuilder.append(lines[i])
                    i++
                }

                currentSubtitles.add(SubtitleEntry(startTime, endTime, textBuilder.toString(), styleMap))
            }

            return currentSubtitles.isNotEmpty()
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing VTT: ${e.message}")
            return false
        }
    }

    private fun parseAssSubtitles(inputStream: InputStream): Boolean {
        // Basic ASS parsing implementation (could be expanded)
        try {
            val reader = inputStream.bufferedReader(StandardCharsets.UTF_8)
            val lines = reader.readLines()

            currentSubtitles.clear()
            var inEvents = false

            for (line in lines) {
                // Check for Events section
                if (line.startsWith("[Events]")) {
                    inEvents = true
                    continue
                }

                if (!inEvents) continue

                // Parse dialogue lines
                if (line.startsWith("Dialogue:")) {
                    val parts = line.substring("Dialogue:".length).split(",", limit = 10)
                    if (parts.size >= 10) {
                        val startTime = parseAssTimeString(parts[1].trim())
                        val endTime = parseAssTimeString(parts[2].trim())
                        val text = parts[9].trim().replace("\\N", "\n")

                        // Remove ASS formatting codes
                        val cleanText = text.replace(Regex("\\{[^}]*\\}"), "")

                        currentSubtitles.add(SubtitleEntry(startTime, endTime, cleanText))
                    }
                }
            }

            return currentSubtitles.isNotEmpty()
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing ASS: ${e.message}")
            return false
        }
    }

    private fun parseTimeString(timeString: String): Long {
        val parts = timeString.split(":", ".", ",")

        return if (parts.size >= 4) {
            val hours = parts[0].toLong()
            val minutes = parts[1].toLong()
            val seconds = parts[2].toLong()
            val milliseconds = if (parts[3].length == 3) parts[3].toLong() else parts[3].toLong() * 10

            hours * 3600000 + minutes * 60000 + seconds * 1000 + milliseconds
        } else {
            0L
        }
    }

    private fun parseAssTimeString(timeString: String): Long {
        val parts = timeString.split(":", ".")

        return if (parts.size >= 3) {
            val hours = parts[0].toLong()
            val minutes = parts[1].toLong()

            val secondParts = parts[2].split(".", limit = 2)
            val seconds = secondParts[0].toLong()
            val centiseconds = if (secondParts.size > 1) secondParts[1].toLong() else 0L

            hours * 3600000 + minutes * 60000 + seconds * 1000 + centiseconds * 10
        } else {
            0L
        }
    }

    private fun dpToPx(dp: Int): Int {
        val density = context.resources.displayMetrics.density
        return (dp * density).toInt()
    }

    interface SubtitleDisplayListener {
        fun onSubtitleTextUpdated(text: String?)
        fun onSubtitleStyleChanged(style: SubtitleStyle)
    }

    companion object {
        private const val TAG = "AdvancedSubtitleManager"
        private val SRT_TIME_PATTERN = Regex("(\\d{2}:\\d{2}:\\d{2}[,.]\\d{3})\\s+-->\\s+(\\d{2}:\\d{2}:\\d{2}[,.]\\d{3})")
        private val VTT_TIME_PATTERN = Regex("(\\d{2}:\\d{2}:\\d{2}\\.\\d{3})\\s+-->\\s+(\\d{2}:\\d{2}:\\d{2}\\.\\d{3})(.*)")
    }
}
