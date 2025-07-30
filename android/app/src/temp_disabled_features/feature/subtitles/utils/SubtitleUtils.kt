package com.astralplayer.nextplayer.feature.subtitles.utils

import androidx.compose.ui.graphics.Color

/**
 * Utility functions for subtitle processing and display
 */
object SubtitleUtils {
    /**
     * Convert a string color representation to a Color object
     */
    fun toColor(colorString: String): Color {
        return try {
            // Handle hex color strings
            if (colorString.startsWith("#")) {
                val hex = colorString.substring(1)
                when (hex.length) {
                    3 -> { // RGB
                        val r = hex.substring(0, 1).repeat(2).toInt(16)
                        val g = hex.substring(1, 2).repeat(2).toInt(16)
                        val b = hex.substring(2, 3).repeat(2).toInt(16)
                        Color(r, g, b)
                    }
                    6 -> { // RRGGBB
                        val r = hex.substring(0, 2).toInt(16)
                        val g = hex.substring(2, 4).toInt(16)
                        val b = hex.substring(4, 6).toInt(16)
                        Color(r, g, b)
                    }
                    8 -> { // AARRGGBB
                        val a = hex.substring(0, 2).toInt(16)
                        val r = hex.substring(2, 4).toInt(16)
                        val g = hex.substring(4, 6).toInt(16)
                        val b = hex.substring(6, 8).toInt(16)
                        Color(r, g, b, a)
                    }
                    else -> Color.White
                }
            } else {
                // Handle named colors
                when (colorString.lowercase()) {
                    "white" -> Color.White
                    "black" -> Color.Black
                    "red" -> Color.Red
                    "green" -> Color.Green
                    "blue" -> Color.Blue
                    "yellow" -> Color.Yellow
                    "cyan" -> Color.Cyan
                    "magenta" -> Color.Magenta
                    "gray", "grey" -> Color.Gray
                    else -> Color.White
                }
            }
        } catch (e: Exception) {
            Color.White // Default fallback
        }
    }

    /**
     * Apply opacity to a color
     */
    fun toColor(colorString: String, opacity: Float): Color {
        val color = toColor(colorString)
        return color.copy(alpha = opacity)
    }
}