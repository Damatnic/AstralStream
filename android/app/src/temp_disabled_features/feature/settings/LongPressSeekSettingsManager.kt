package com.astralplayer.nextplayer.feature.settings

import android.content.Context

// Minimal settings manager for build compatibility
class LongPressSeekSettingsManager(private val context: Context) {
    
    fun getDefaultSpeed(): Float = 2.0f
    
    fun getMaxSpeed(): Float = 4.0f
    
    fun getDuration(): Long = 500L
    
    fun isEnabled(): Boolean = true
}