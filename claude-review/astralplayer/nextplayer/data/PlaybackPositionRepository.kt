package com.astralplayer.nextplayer.data

import android.content.Context
import android.net.Uri
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

// Extension property to get DataStore instance
private val Context.playbackDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "playback_positions"
)

/**
 * Repository for managing video playback positions
 */
class PlaybackPositionRepository(
    private val context: Context
) {
    companion object {
        // Prefix for playback position keys
        private const val POSITION_PREFIX = "position_"
        private const val DURATION_PREFIX = "duration_"
        private const val TIMESTAMP_PREFIX = "timestamp_"
        
        // Threshold for resuming playback (95% completion = finished)
        private const val COMPLETION_THRESHOLD = 0.95f
        
        // Time threshold - don't resume if last played > 30 days ago
        private const val TIME_THRESHOLD_MS = 30L * 24 * 60 * 60 * 1000 // 30 days
    }
    
    /**
     * Save playback position for a video
     */
    suspend fun savePlaybackPosition(
        videoUri: Uri,
        position: Long,
        duration: Long
    ) {
        // Don't save if video is nearly finished (>95% complete)
        if (duration > 0 && position.toFloat() / duration > COMPLETION_THRESHOLD) {
            // Clear the position instead
            clearPlaybackPosition(videoUri)
            return
        }
        
        val key = videoUri.toString().hashCode().toString()
        
        context.playbackDataStore.edit { preferences ->
            preferences[longPreferencesKey(POSITION_PREFIX + key)] = position
            preferences[longPreferencesKey(DURATION_PREFIX + key)] = duration
            preferences[longPreferencesKey(TIMESTAMP_PREFIX + key)] = System.currentTimeMillis()
        }
    }
    
    /**
     * Get saved playback position for a video
     * Returns null if no position saved or if too old
     */
    fun getPlaybackPosition(videoUri: Uri): Flow<Long?> {
        val key = videoUri.toString().hashCode().toString()
        
        return context.playbackDataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }
            .map { preferences ->
                val position = preferences[longPreferencesKey(POSITION_PREFIX + key)]
                val timestamp = preferences[longPreferencesKey(TIMESTAMP_PREFIX + key)]
                
                // Check if position exists and is recent enough
                if (position != null && timestamp != null) {
                    val age = System.currentTimeMillis() - timestamp
                    if (age < TIME_THRESHOLD_MS) {
                        position
                    } else {
                        // Too old, return null
                        null
                    }
                } else {
                    null
                }
            }
    }
    
    /**
     * Clear playback position for a video
     */
    suspend fun clearPlaybackPosition(videoUri: Uri) {
        val key = videoUri.toString().hashCode().toString()
        
        context.playbackDataStore.edit { preferences ->
            preferences.remove(longPreferencesKey(POSITION_PREFIX + key))
            preferences.remove(longPreferencesKey(DURATION_PREFIX + key))
            preferences.remove(longPreferencesKey(TIMESTAMP_PREFIX + key))
        }
    }
    
    /**
     * Clear all saved playback positions
     */
    suspend fun clearAllPlaybackPositions() {
        context.playbackDataStore.edit { preferences ->
            preferences.clear()
        }
    }
    
    /**
     * Get all saved playback positions (for debugging/stats)
     */
    fun getAllPlaybackPositions(): Flow<Map<String, PlaybackInfo>> {
        return context.playbackDataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }
            .map { preferences ->
                val positions = mutableMapOf<String, PlaybackInfo>()
                
                preferences.asMap().forEach { (key, value) ->
                    if (key.name.startsWith(POSITION_PREFIX)) {
                        val videoKey = key.name.removePrefix(POSITION_PREFIX)
                        val position = value as? Long ?: 0L
                        val duration = preferences[longPreferencesKey(DURATION_PREFIX + videoKey)] ?: 0L
                        val timestamp = preferences[longPreferencesKey(TIMESTAMP_PREFIX + videoKey)] ?: 0L
                        
                        positions[videoKey] = PlaybackInfo(
                            position = position,
                            duration = duration,
                            timestamp = timestamp
                        )
                    }
                }
                
                positions
            }
    }
    
    data class PlaybackInfo(
        val position: Long,
        val duration: Long,
        val timestamp: Long
    )
}