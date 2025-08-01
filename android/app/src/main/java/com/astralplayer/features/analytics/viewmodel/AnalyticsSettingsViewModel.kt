package com.astralplayer.features.analytics.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.astralplayer.features.analytics.dao.AnalyticsDao
import com.astralplayer.features.analytics.repository.AnalyticsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

data class AnalyticsSettingsUiState(
    val analyticsEnabled: Boolean = true,
    val trackWatchTime: Boolean = true,
    val trackPlaybackEvents: Boolean = true,
    val trackFeatureUsage: Boolean = true,
    val trackPerformanceMetrics: Boolean = true,
    val trackContentPreferences: Boolean = true,
    val dataRetentionDays: Int = 90,
    val analyticsSummary: AnalyticsSummary? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

data class AnalyticsSummary(
    val totalWatchTime: Long,
    val videosWatched: Int,
    val dataSize: Long,
    val oldestDataTimestamp: Long
)

@HiltViewModel
class AnalyticsSettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val analyticsRepository: AnalyticsRepository,
    private val analyticsDao: AnalyticsDao
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(AnalyticsSettingsUiState())
    val uiState: StateFlow<AnalyticsSettingsUiState> = _uiState.asStateFlow()
    
    init {
        loadSettings()
        loadAnalyticsSummary()
    }
    
    private fun loadSettings() {
        // Load settings from SharedPreferences
        val prefs = context.getSharedPreferences("analytics_settings", Context.MODE_PRIVATE)
        _uiState.update { state ->
            state.copy(
                analyticsEnabled = prefs.getBoolean("analytics_enabled", true),
                trackWatchTime = prefs.getBoolean("track_watch_time", true),
                trackPlaybackEvents = prefs.getBoolean("track_playback_events", true),
                trackFeatureUsage = prefs.getBoolean("track_feature_usage", true),
                trackPerformanceMetrics = prefs.getBoolean("track_performance_metrics", true),
                trackContentPreferences = prefs.getBoolean("track_content_preferences", true),
                dataRetentionDays = prefs.getInt("data_retention_days", 90)
            )
        }
    }
    
    private fun loadAnalyticsSummary() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true) }
                
                // Calculate summary statistics
                val totalWatchTime = analyticsDao.getTotalWatchTime(0) ?: 0L
                val sessions = analyticsDao.getSessionsInTimeRange(0, System.currentTimeMillis())
                val videosWatched = sessions.map { it.videoId }.distinct().size
                
                // Estimate data size (rough calculation)
                val sessionCount = sessions.size
                val eventCount = sessions.sumOf { 
                    analyticsDao.getSessionEvents(it.sessionId).size 
                }
                val dataSize = (sessionCount * 500L) + (eventCount * 200L) // Rough bytes estimate
                
                val oldestSession = sessions.minByOrNull { it.startTime }
                val oldestTimestamp = oldestSession?.startTime ?: System.currentTimeMillis()
                
                val summary = AnalyticsSummary(
                    totalWatchTime = totalWatchTime,
                    videosWatched = videosWatched,
                    dataSize = dataSize,
                    oldestDataTimestamp = oldestTimestamp
                )
                
                _uiState.update { 
                    it.copy(
                        analyticsSummary = summary,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        error = "Failed to load analytics summary"
                    )
                }
            }
        }
    }
    
    fun setAnalyticsEnabled(enabled: Boolean) {
        _uiState.update { it.copy(analyticsEnabled = enabled) }
        savePreference("analytics_enabled", enabled)
        
        if (!enabled) {
            // Disable all tracking when analytics is disabled
            _uiState.update { state ->
                state.copy(
                    trackWatchTime = false,
                    trackPlaybackEvents = false,
                    trackFeatureUsage = false,
                    trackPerformanceMetrics = false,
                    trackContentPreferences = false
                )
            }
            saveAllTrackingPreferences(false)
        }
    }
    
    fun setTrackWatchTime(enabled: Boolean) {
        _uiState.update { it.copy(trackWatchTime = enabled) }
        savePreference("track_watch_time", enabled)
    }
    
    fun setTrackPlaybackEvents(enabled: Boolean) {
        _uiState.update { it.copy(trackPlaybackEvents = enabled) }
        savePreference("track_playback_events", enabled)
    }
    
    fun setTrackFeatureUsage(enabled: Boolean) {
        _uiState.update { it.copy(trackFeatureUsage = enabled) }
        savePreference("track_feature_usage", enabled)
    }
    
    fun setTrackPerformanceMetrics(enabled: Boolean) {
        _uiState.update { it.copy(trackPerformanceMetrics = enabled) }
        savePreference("track_performance_metrics", enabled)
    }
    
    fun setTrackContentPreferences(enabled: Boolean) {
        _uiState.update { it.copy(trackContentPreferences = enabled) }
        savePreference("track_content_preferences", enabled)
    }
    
    fun setDataRetentionDays(days: Int) {
        _uiState.update { it.copy(dataRetentionDays = days) }
        savePreference("data_retention_days", days)
        
        // Schedule cleanup of old data
        viewModelScope.launch {
            analyticsRepository.cleanupOldData(days)
        }
    }
    
    fun exportAnalytics() {
        viewModelScope.launch {
            try {
                // Create export directory
                val exportDir = File(context.getExternalFilesDir(null), "analytics_export")
                exportDir.mkdirs()
                
                // Generate filename with timestamp
                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                    .format(Date())
                val exportFile = File(exportDir, "analytics_$timestamp.json")
                
                // Collect all analytics data
                val sessions = analyticsDao.getSessionsInTimeRange(0, System.currentTimeMillis())
                val dailyStats = analyticsDao.getStatsInDateRange("2000-01-01", "2099-12-31")
                val videoStats = sessions.map { it.videoId }.distinct().mapNotNull { videoId ->
                    analyticsDao.getVideoStats(videoId)
                }
                
                // Create JSON export
                val exportData = buildString {
                    append("{\n")
                    append("  \"exportDate\": \"${Date()}\",\n")
                    append("  \"sessions\": ${sessions.size},\n")
                    append("  \"totalWatchTime\": ${sessions.sumOf { it.totalWatchTime }},\n")
                    append("  \"videosWatched\": ${videoStats.size},\n")
                    append("  \"dailyStats\": [\n")
                    dailyStats.forEachIndexed { index, stat ->
                        append("    {\n")
                        append("      \"date\": \"${stat.date}\",\n")
                        append("      \"watchTime\": ${stat.totalWatchTime},\n")
                        append("      \"sessions\": ${stat.totalSessions}\n")
                        append("    }")
                        if (index < dailyStats.size - 1) append(",")
                        append("\n")
                    }
                    append("  ]\n")
                    append("}")
                }
                
                exportFile.writeText(exportData)
                
                // TODO: Share the file or notify user of successful export
                
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(error = "Failed to export analytics: ${e.message}")
                }
            }
        }
    }
    
    fun clearAllAnalytics() {
        viewModelScope.launch {
            try {
                // Clear all analytics data
                analyticsRepository.cleanupOldData(0) // Delete everything
                
                // Reload summary
                loadAnalyticsSummary()
                
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(error = "Failed to clear analytics: ${e.message}")
                }
            }
        }
    }
    
    private fun savePreference(key: String, value: Boolean) {
        context.getSharedPreferences("analytics_settings", Context.MODE_PRIVATE)
            .edit()
            .putBoolean(key, value)
            .apply()
    }
    
    private fun savePreference(key: String, value: Int) {
        context.getSharedPreferences("analytics_settings", Context.MODE_PRIVATE)
            .edit()
            .putInt(key, value)
            .apply()
    }
    
    private fun saveAllTrackingPreferences(enabled: Boolean) {
        val prefs = context.getSharedPreferences("analytics_settings", Context.MODE_PRIVATE).edit()
        prefs.putBoolean("track_watch_time", enabled)
        prefs.putBoolean("track_playback_events", enabled)
        prefs.putBoolean("track_feature_usage", enabled)
        prefs.putBoolean("track_performance_metrics", enabled)
        prefs.putBoolean("track_content_preferences", enabled)
        prefs.apply()
    }
}