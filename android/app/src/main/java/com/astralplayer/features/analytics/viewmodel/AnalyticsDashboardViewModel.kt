package com.astralplayer.features.analytics.viewmodel

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.astralplayer.features.analytics.dao.*
import com.astralplayer.features.analytics.data.*
import com.astralplayer.features.analytics.repository.AnalyticsRepository
import com.astralplayer.features.analytics.repository.ContentPreferences
import com.astralplayer.features.analytics.repository.PlaybackBehavior
import com.astralplayer.features.analytics.repository.WatchTimeStats
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

enum class AnalyticsTab(val title: String, val icon: ImageVector) {
    OVERVIEW("Overview", Icons.Default.Dashboard),
    WATCH_TIME("Watch Time", Icons.Default.Schedule),
    CONTENT("Content", Icons.Default.VideoLibrary),
    FEATURES("Features", Icons.Default.TouchApp),
    PERFORMANCE("Performance", Icons.Default.Speed)
}

data class AnalyticsDashboardUiState(
    val isLoading: Boolean = true,
    val timeRangeDays: Int = 7,
    val watchTimeStats: WatchTimeStats? = null,
    val playbackBehavior: PlaybackBehavior? = null,
    val contentPreferences: ContentPreferences? = null,
    val recentSessions: List<ViewingSessionEntity> = emptyList(),
    val dailyStats: List<DailyStatisticsEntity> = emptyList(),
    val mostWatchedVideos: List<VideoStatisticsEntity> = emptyList(),
    val recentlyWatchedVideos: List<VideoStatisticsEntity> = emptyList(),
    val featureUsage: List<FeatureUsageEntity> = emptyList(),
    val performanceMetrics: Map<PerformanceMetricType, List<PerformanceMetricEntity>> = emptyMap(),
    val hourlyDistribution: List<HourlyDistribution> = emptyList(),
    val weeklyPattern: List<WeeklyPattern> = emptyList(),
    val error: String? = null
)

@HiltViewModel
class AnalyticsDashboardViewModel @Inject constructor(
    private val analyticsRepository: AnalyticsRepository,
    private val analyticsDao: AnalyticsDao
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(AnalyticsDashboardUiState())
    val uiState: StateFlow<AnalyticsDashboardUiState> = _uiState.asStateFlow()
    
    private val timeRangeDays = MutableStateFlow(7)
    
    init {
        loadAnalyticsData()
        observeAnalyticsData()
    }
    
    private fun loadAnalyticsData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            try {
                val days = timeRangeDays.value
                val since = System.currentTimeMillis() - (days * 24 * 60 * 60 * 1000L)
                
                // Load basic stats
                val watchTimeStats = analyticsRepository.getWatchTimeStats(days)
                val playbackBehavior = analyticsRepository.getPlaybackBehavior(days)
                val contentPreferences = analyticsRepository.getContentPreferences()
                
                // Load distribution data
                val hourlyDistribution = analyticsDao.getHourlyPlaybackDistribution(since)
                val weeklyPattern = analyticsDao.getWeeklyWatchPattern(since)
                
                // Load performance metrics
                val performanceMetrics = PerformanceMetricType.values().associateWith { type ->
                    analyticsDao.getRecentMetrics(type, 50)
                }
                
                _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        timeRangeDays = days,
                        watchTimeStats = watchTimeStats,
                        playbackBehavior = playbackBehavior,
                        contentPreferences = contentPreferences,
                        hourlyDistribution = hourlyDistribution,
                        weeklyPattern = weeklyPattern,
                        performanceMetrics = performanceMetrics
                    )
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        error = "Failed to load analytics: ${e.message}"
                    )
                }
            }
        }
    }
    
    private fun observeAnalyticsData() {
        // Observe recent sessions
        viewModelScope.launch {
            analyticsRepository.getRecentSessions(20)
                .collect { sessions ->
                    _uiState.update { it.copy(recentSessions = sessions) }
                }
        }
        
        // Observe daily stats
        viewModelScope.launch {
            timeRangeDays.flatMapLatest { days ->
                analyticsRepository.getDailyStats(days)
            }.collect { stats ->
                _uiState.update { it.copy(dailyStats = stats) }
            }
        }
        
        // Observe most watched videos
        viewModelScope.launch {
            analyticsRepository.getMostWatchedVideos(10)
                .collect { videos ->
                    _uiState.update { it.copy(mostWatchedVideos = videos) }
                }
        }
        
        // Observe recently watched videos
        viewModelScope.launch {
            analyticsRepository.getRecentlyWatchedVideos(10)
                .collect { videos ->
                    _uiState.update { it.copy(recentlyWatchedVideos = videos) }
                }
        }
        
        // Observe feature usage
        viewModelScope.launch {
            analyticsRepository.getMostUsedFeatures()
                .collect { features ->
                    _uiState.update { it.copy(featureUsage = features) }
                }
        }
    }
    
    fun setTimeRange(days: Int) {
        timeRangeDays.value = days
        loadAnalyticsData()
    }
    
    fun refreshData() {
        loadAnalyticsData()
    }
    
    fun exportAnalytics() {
        viewModelScope.launch {
            // TODO: Implement analytics export functionality
            // This could export to CSV, JSON, or generate a PDF report
        }
    }
    
    fun clearAnalytics() {
        viewModelScope.launch {
            // TODO: Implement analytics clearing with user confirmation
            // This should clear all analytics data from the database
        }
    }
    
    fun trackDashboardUsage() {
        analyticsRepository.trackFeatureUsage(
            featureName = "analytics_dashboard",
            category = FeatureCategory.ADVANCED
        )
    }
    
    override fun onCleared() {
        super.onCleared()
        // Track how long the user spent in the analytics dashboard
        analyticsRepository.trackFeatureUsage(
            featureName = "analytics_dashboard_session_end",
            category = FeatureCategory.ADVANCED
        )
    }
}