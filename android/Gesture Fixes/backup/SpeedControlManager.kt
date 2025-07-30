package com.astralplayer.nextplayer.feature.settings

import android.content.Context
import android.content.SharedPreferences

/**
 * Manages playback speed settings and presets for video playback.
 * Supports persistent storage of user preferences and custom speed presets.
 */
class SpeedControlManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val speedPresets = mutableListOf(0.25f, 0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f)
    private var currentSpeedIndex = 3  // Default to 1.0x (index 3)

    init {
        loadCustomPresets()
        currentSpeedIndex = prefs.getInt(KEY_LAST_SPEED_INDEX, 3)

        // Ensure the index is valid
        if (currentSpeedIndex < 0 || currentSpeedIndex >= speedPresets.size) {
            currentSpeedIndex = 3  // Reset to 1.0x if invalid
        }
    }

    /**
     * Gets the current playback speed
     */
    fun getCurrentSpeed(): Float {
        return speedPresets[currentSpeedIndex]
    }

    /**
     * Sets the playback speed to a specific value
     * @return true if the speed was found in presets, false otherwise
     */
    fun setSpeed(speed: Float): Boolean {
        val index = speedPresets.indexOf(speed)
        if (index >= 0) {
            currentSpeedIndex = index
            saveCurrentSpeedIndex()
            return true
        }
        return false
    }

    /**
     * Cycles to the next speed preset
     * @return The new speed value
     */
    fun nextSpeed(): Float {
        currentSpeedIndex = (currentSpeedIndex + 1) % speedPresets.size
        saveCurrentSpeedIndex()
        return getCurrentSpeed()
    }

    /**
     * Cycles to the previous speed preset
     * @return The new speed value
     */
    fun previousSpeed(): Float {
        currentSpeedIndex = if (currentSpeedIndex > 0) currentSpeedIndex - 1 else speedPresets.size - 1
        saveCurrentSpeedIndex()
        return getCurrentSpeed()
    }

    /**
     * Adds a custom speed preset
     * @return true if added successfully, false if already exists
     */
    fun addCustomPreset(speed: Float): Boolean {
        if (speed <= 0f || speedPresets.contains(speed)) {
            return false
        }

        speedPresets.add(speed)
        speedPresets.sort()
        saveCustomPresets()
        return true
    }

    /**
     * Removes a custom speed preset
     * @return true if removed successfully, false if it's a default preset
     */
    fun removeCustomPreset(speed: Float): Boolean {
        // Don't allow removing default presets
        if (isDefaultPreset(speed)) {
            return false
        }

        val result = speedPresets.remove(speed)
        if (result) {
            saveCustomPresets()
            // If current index is now invalid, reset to 1.0x
            if (currentSpeedIndex >= speedPresets.size) {
                currentSpeedIndex = speedPresets.indexOf(1.0f)
                saveCurrentSpeedIndex()
            }
        }
        return result
    }

    /**
     * Gets all available speed presets
     */
    fun getAllPresets(): List<Float> {
        return speedPresets.toList()
    }

    private fun isDefaultPreset(speed: Float): Boolean {
        return speed == 0.25f || speed == 0.5f || speed == 0.75f || 
               speed == 1.0f || speed == 1.25f || speed == 1.5f || 
               speed == 1.75f || speed == 2.0f
    }

    private fun saveCurrentSpeedIndex() {
        prefs.edit().putInt(KEY_LAST_SPEED_INDEX, currentSpeedIndex).apply()
    }

    private fun loadCustomPresets() {
        val customPresetsString = prefs.getString(KEY_CUSTOM_PRESETS, null) ?: return

        try {
            val customPresets = customPresetsString.split(",")
                .map { it.trim().toFloat() }
                .filter { !isDefaultPreset(it) && it > 0f }

            speedPresets.addAll(customPresets)
            speedPresets.sort()
        } catch (e: Exception) {
            // Ignore parsing errors
        }
    }

    private fun saveCustomPresets() {
        val customPresets = speedPresets.filter { !isDefaultPreset(it) }
        val presetString = customPresets.joinToString(",")
        prefs.edit().putString(KEY_CUSTOM_PRESETS, presetString).apply()
    }

    companion object {
        private const val PREFS_NAME = "speed_control_prefs"
        private const val KEY_LAST_SPEED_INDEX = "last_speed_index"
        private const val KEY_CUSTOM_PRESETS = "custom_presets"
    }
}
