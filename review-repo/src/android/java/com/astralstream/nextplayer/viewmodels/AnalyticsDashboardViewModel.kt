package com.astralstream.nextplayer.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.astralstream.nextplayer.analytics.AnalyticsDashboardEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AnalyticsDashboardViewModel @Inject constructor(
    private val analyticsEngine: AnalyticsDashboardEngine
) : ViewModel() {
    
    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()
    
    private val _dateRange = MutableStateFlow(DateRange.LAST_7_DAYS)
    val dateRange: StateFlow<DateRange> = _dateRange.asStateFlow()
    
    sealed class UiState {
        object Loading : UiState()
        data class Success(val analytics: AnalyticsData) : UiState()
        data class Error(val message: String) : UiState()
    }
    
    enum class DateRange(val displayName: String, val days: Int) {
        TODAY("Today", 1),
        LAST_7_DAYS("Last 7 Days", 7),
        LAST_30_DAYS("Last 30 Days", 30),
        LAST_90_DAYS("Last 90 Days", 90),
        ALL_TIME("All Time", -1)
    }
    
    data class AnalyticsData(
        val overview: OverviewStats,
        val watchTime: WatchTimeData,
        val topVideos: List<VideoStats>,
        val engagementMetrics: EngagementData,
        val userBehavior: UserBehaviorData
    )
    
    data class OverviewStats(
        val totalWatchTime: Long,
        val totalVideosWatched: Int,
        val averageWatchTime: Long,
        val completionRate: Float
    )
    
    data class WatchTimeData(
        val dailyData: List<DailyWatchTime>,
        val hourlyDistribution: List<HourlyData>,
        val categoryBreakdown: Map<String, Long>
    )
    
    data class DailyWatchTime(
        val date: Long,
        val watchTimeMinutes: Int,
        val videosWatched: Int
    )
    
    data class HourlyData(
        val hour: Int,
        val averageWatchTime: Int
    )
    
    data class VideoStats(
        val videoId: String,
        val title: String,
        val watchTime: Long,
        val playCount: Int,
        val completionRate: Float,
        val averageViewDuration: Long
    )
    
    data class EngagementData(
        val seekEvents: Int,
        val pauseEvents: Int,
        val resumeEvents: Int,
        val qualityChanges: Int,
        val subtitleUsage: Float
    )
    
    data class UserBehaviorData(
        val preferredQualities: Map<String, Float>,
        val preferredPlaybackSpeeds: Map<Float, Float>,
        val deviceTypeDistribution: Map<String, Float>,
        val gestureUsage: Map<String, Int>
    )
    
    init {
        refreshAnalytics()
    }
    
    fun refreshAnalytics() {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                val analytics = analyticsEngine.getAnalytics(_dateRange.value)
                _uiState.value = UiState.Success(analytics)
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "Failed to load analytics")
            }
        }
    }
    
    fun updateDateRange(range: DateRange) {
        _dateRange.value = range
        refreshAnalytics()
    }
    
    fun exportAnalytics(): String {
        return analyticsEngine.exportAnalyticsReport(_dateRange.value)
    }
}