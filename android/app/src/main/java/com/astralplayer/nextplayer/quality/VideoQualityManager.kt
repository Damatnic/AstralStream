package com.astralplayer.nextplayer.quality

import android.content.Context
import android.net.Uri
import androidx.media3.common.Format
import androidx.media3.common.TrackGroup
import androidx.media3.common.TrackSelection
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.trackselection.AdaptiveTrackSelection
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import com.astralplayer.nextplayer.streaming.NetworkAnalyzer
import com.astralplayer.nextplayer.streaming.NetworkInfo
import com.astralplayer.nextplayer.streaming.ConnectionQuality
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlin.math.*

/**
 * Advanced video quality management with auto-adaptation based on network conditions and device capabilities
 */
class VideoQualityManager(
    private val context: Context,
    private val exoPlayer: ExoPlayer
) {
    
    private val _qualityEvents = MutableSharedFlow<QualityEvent>()
    val qualityEvents: SharedFlow<QualityEvent> = _qualityEvents.asSharedFlow()
    
    private val _currentQuality = MutableStateFlow<VideoQuality?>(null)
    val currentQuality: StateFlow<VideoQuality?> = _currentQuality.asStateFlow()
    
    private val _availableQualities = MutableStateFlow<List<VideoQuality>>(emptyList())
    val availableQualities: StateFlow<List<VideoQuality>> = _availableQualities.asStateFlow()
    
    private val _adaptationSettings = MutableStateFlow(AdaptationSettings())
    val adaptationSettings: StateFlow<AdaptationSettings> = _adaptationSettings.asStateFlow()
    
    private var networkAnalyzer: NetworkAnalyzer? = null
    private var trackSelector: DefaultTrackSelector? = null
    private var qualityHistory = mutableListOf<QualityHistoryEntry>()
    private var deviceCapabilities: DeviceCapabilities? = null
    
    private val qualityScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var isInitialized = false
    private var autoAdaptationEnabled = true
    
    /**
     * Initialize video quality manager
     */
    suspend fun initialize(networkAnalyzer: NetworkAnalyzer) {
        this.networkAnalyzer = networkAnalyzer
        isInitialized = true
        
        // Analyze device capabilities
        deviceCapabilities = analyzeDeviceCapabilities()
        
        // Setup track selector for adaptive streaming
        setupAdaptiveTrackSelector()
        
        // Start quality monitoring
        startQualityMonitoring()
        
        // Register network change listener
        registerNetworkChangeListener()
        
        _qualityEvents.emit(QualityEvent.Initialized(deviceCapabilities!!))
    }
    
    /**
     * Set available video qualities for current content
     */
    suspend fun setAvailableQualities(qualities: List<VideoQuality>) {
        _availableQualities.value = qualities.sortedByDescending { it.bitrate }
        
        // Select initial quality based on current conditions
        val initialQuality = selectOptimalQuality(qualities)
        setQuality(initialQuality, QualityChangeReason.INITIAL_SELECTION)
        
        _qualityEvents.emit(QualityEvent.QualitiesAvailable(qualities))
    }
    
    /**
     * Manually set video quality
     */
    suspend fun setQuality(quality: VideoQuality, reason: QualityChangeReason = QualityChangeReason.MANUAL) {
        val previousQuality = _currentQuality.value
        _currentQuality.value = quality
        
        // Apply quality to player
        applyQualityToPlayer(quality)
        
        // Record quality change
        recordQualityChange(previousQuality, quality, reason)
        
        _qualityEvents.emit(QualityEvent.QualityChanged(previousQuality, quality, reason))
    }
    
    /**
     * Enable or disable auto-adaptation
     */
    suspend fun setAutoAdaptationEnabled(enabled: Boolean) {
        autoAdaptationEnabled = enabled
        
        if (enabled) {
            // Re-evaluate quality based on current conditions
            val networkInfo = networkAnalyzer?.getCurrentNetworkInfo()
            if (networkInfo != null) {
                adaptQualityForNetwork(networkInfo)
            }
        } else {
            // Disable adaptive track selection
            trackSelector?.setParameters(
                trackSelector!!.buildUponParameters()
                    .setForceHighestSupportedBitrate(false)
                    .build()
            )
        }
        
        _qualityEvents.emit(QualityEvent.AutoAdaptationChanged(enabled))
    }
    
    /**
     * Configure adaptation settings
     */
    suspend fun configureAdaptation(settings: AdaptationSettings) {
        _adaptationSettings.value = settings
        
        // Update track selector parameters
        trackSelector?.setParameters(
            trackSelector!!.buildUponParameters()
                .setMaxVideoBitrate(settings.maxBitrate)
                .setMinVideoBitrate(settings.minBitrate)
                .setViewportSize(settings.viewportWidth, settings.viewportHeight, true)
                .build()
        )
        
        _qualityEvents.emit(QualityEvent.AdaptationSettingsChanged(settings))
    }
    
    /**
     * Get quality recommendations based on current conditions
     */
    suspend fun getQualityRecommendations(): List<QualityRecommendation> {
        val networkInfo = networkAnalyzer?.getCurrentNetworkInfo() ?: return emptyList()
        val availableQualities = _availableQualities.value
        val deviceCaps = deviceCapabilities ?: return emptyList()
        
        return availableQualities.map { quality ->
            val score = calculateQualityScore(quality, networkInfo, deviceCaps)
            val recommendation = generateRecommendationReason(quality, networkInfo, deviceCaps, score)
            
            QualityRecommendation(
                quality = quality,
                score = score,
                recommendation = recommendation,
                networkSuitability = calculateNetworkSuitability(quality, networkInfo),
                deviceSuitability = calculateDeviceSuitability(quality, deviceCaps)
            )
        }.sortedByDescending { it.score }
    }
    
    /**
     * Adapt quality based on network conditions
     */
    suspend fun adaptQualityForNetwork(networkInfo: NetworkInfo) {
        if (!autoAdaptationEnabled) return
        
        val currentQuality = _currentQuality.value ?: return
        val availableQualities = _availableQualities.value
        
        val optimalQuality = selectOptimalQualityForNetwork(networkInfo, availableQualities)
        
        if (optimalQuality != currentQuality && shouldChangeQuality(currentQuality, optimalQuality, networkInfo)) {
            setQuality(optimalQuality, QualityChangeReason.NETWORK_ADAPTATION)
        }
    }
    
    /**
     * Adapt quality based on buffer health
     */
    suspend fun adaptQualityForBuffering(bufferHealth: BufferHealth) {
        if (!autoAdaptationEnabled) return
        
        val currentQuality = _currentQuality.value ?: return
        val availableQualities = _availableQualities.value
        
        val newQuality = when (bufferHealth.status) {
            BufferHealthStatus.CRITICAL -> {
                // Aggressive downgrade
                selectLowerQuality(currentQuality, availableQualities, 2)
            }
            BufferHealthStatus.LOW -> {
                // Conservative downgrade
                selectLowerQuality(currentQuality, availableQualities, 1)
            }
            BufferHealthStatus.GOOD -> {
                // Consider upgrade if network allows
                val networkInfo = networkAnalyzer?.getCurrentNetworkInfo()
                if (networkInfo != null && networkInfo.connectionQuality in listOf(ConnectionQuality.GOOD, ConnectionQuality.EXCELLENT)) {
                    selectHigherQuality(currentQuality, availableQualities, 1)
                } else currentQuality
            }
            BufferHealthStatus.EXCELLENT -> {
                // Aggressive upgrade if conditions allow
                val networkInfo = networkAnalyzer?.getCurrentNetworkInfo()
                if (networkInfo != null && networkInfo.connectionQuality == ConnectionQuality.EXCELLENT) {
                    selectHigherQuality(currentQuality, availableQualities, 2)
                } else currentQuality
            }
        }
        
        if (newQuality != currentQuality) {
            setQuality(newQuality, QualityChangeReason.BUFFER_ADAPTATION)
        }
    }
    
    /**
     * Adapt quality based on device thermal state
     */
    suspend fun adaptQualityForThermal(thermalState: ThermalState) {
        if (!autoAdaptationEnabled) return
        
        val currentQuality = _currentQuality.value ?: return
        val availableQualities = _availableQualities.value
        
        val newQuality = when (thermalState) {
            ThermalState.NORMAL -> currentQuality
            ThermalState.LIGHT -> {
                // Slight downgrade to reduce heat
                selectLowerQuality(currentQuality, availableQualities, 1)
            }
            ThermalState.MODERATE -> {
                // Moderate downgrade
                selectLowerQuality(currentQuality, availableQualities, 2)
            }
            ThermalState.SEVERE, ThermalState.CRITICAL -> {
                // Aggressive downgrade to prevent overheating
                selectLowestSafeQuality(availableQualities)
            }
        }
        
        if (newQuality != currentQuality) {
            setQuality(newQuality, QualityChangeReason.THERMAL_ADAPTATION)
        }
    }
    
    /**
     * Get quality statistics and analytics
     */
    fun getQualityStatistics(): QualityStatistics {
        val history = qualityHistory.takeLast(100)
        
        return QualityStatistics(
            currentQuality = _currentQuality.value,
            averageQuality = calculateAverageQuality(history),
            qualityStability = calculateQualityStability(history),
            adaptationCount = history.size,
            networkAdaptations = history.count { it.reason == QualityChangeReason.NETWORK_ADAPTATION },
            bufferAdaptations = history.count { it.reason == QualityChangeReason.BUFFER_ADAPTATION },
            thermalAdaptations = history.count { it.reason == QualityChangeReason.THERMAL_ADAPTATION },
            manualChanges = history.count { it.reason == QualityChangeReason.MANUAL },
            averageAdaptationTime = calculateAverageAdaptationTime(history),
            qualityUpgrades = history.count { it.isUpgrade },
            qualityDowngrades = history.count { !it.isUpgrade }
        )
    }
    
    /**
     * Create quality preset configurations
     */
    fun createQualityPresets(): List<QualityPreset> {
        return listOf(
            QualityPreset(
                name = "Data Saver",
                description = "Optimized for minimal data usage",
                maxBitrate = 500000, // 500 kbps
                preferredHeight = 360,
                adaptationSettings = AdaptationSettings(
                    aggressiveness = AdaptationAggressiveness.CONSERVATIVE,
                    bufferThreshold = 0.8f,
                    networkThreshold = 0.9f
                )
            ),
            QualityPreset(
                name = "Balanced",
                description = "Good quality with reasonable data usage",
                maxBitrate = 2000000, // 2 Mbps
                preferredHeight = 720,
                adaptationSettings = AdaptationSettings(
                    aggressiveness = AdaptationAggressiveness.BALANCED,
                    bufferThreshold = 0.6f,
                    networkThreshold = 0.7f
                )
            ),
            QualityPreset(
                name = "High Quality",
                description = "Best quality, higher data usage",
                maxBitrate = 8000000, // 8 Mbps
                preferredHeight = 1080,
                adaptationSettings = AdaptationSettings(
                    aggressiveness = AdaptationAggressiveness.AGGRESSIVE,
                    bufferThreshold = 0.4f,
                    networkThreshold = 0.5f
                )
            ),
            QualityPreset(
                name = "Auto (Recommended)",
                description = "Automatically adapts based on conditions",
                maxBitrate = Int.MAX_VALUE,
                preferredHeight = 1080,
                adaptationSettings = AdaptationSettings(
                    aggressiveness = AdaptationAggressiveness.INTELLIGENT,
                    bufferThreshold = 0.5f,
                    networkThreshold = 0.6f
                )
            )
        )
    }
    
    // Private implementation methods
    private fun analyzeDeviceCapabilities(): DeviceCapabilities {
        val displayMetrics = context.resources.displayMetrics
        
        return DeviceCapabilities(
            maxSupportedResolution = Resolution(displayMetrics.widthPixels, displayMetrics.heightPixels),
            maxDecodingBitrate = estimateMaxDecodingBitrate(),
            hardwareAcceleration = isHardwareAccelerationAvailable(),
            memoryCapacity = getAvailableMemory(),
            processingPower = estimateProcessingPower(),
            batteryOptimized = isBatteryOptimizationEnabled(),
            thermalCapacity = estimateThermalCapacity()
        )
    }
    
    private fun setupAdaptiveTrackSelector() {
        trackSelector = DefaultTrackSelector(context).apply {
            setParameters(
                buildUponParameters()
                    .setAllowVideoMixedMimeTypeAdaptivity(true)
                    .setAllowVideoNonSeamlessAdaptivity(true)
                    .build()
            )
        }
        
        // Note: In real implementation, you would set this track selector on the ExoPlayer
        // exoPlayer.setTrackSelector(trackSelector)
    }
    
    private fun startQualityMonitoring() {
        qualityScope.launch {
            while (isInitialized && currentCoroutineContext().isActive) {
                try {
                    monitorQuality()
                    delay(5000) // Monitor every 5 seconds
                } catch (e: Exception) {
                    // Handle error but continue monitoring
                }
            }
        }
    }
    
    private fun registerNetworkChangeListener() {
        networkAnalyzer?.networkEvents?.let { events ->
            qualityScope.launch {
                events.collect { event ->
                    when (event) {
                        is com.astralplayer.nextplayer.streaming.NetworkEvent.NetworkChanged -> {
                            adaptQualityForNetwork(event.new)
                        }
                        is com.astralplayer.nextplayer.streaming.NetworkEvent.MetricsUpdate -> {
                            val networkInfo = networkAnalyzer?.getCurrentNetworkInfo()
                            if (networkInfo != null) {
                                adaptQualityForNetwork(networkInfo)
                            }
                        }
                    }
                }
            }
        }
    }
    
    private suspend fun monitorQuality() {
        // Monitor current playback quality and performance
        val currentQuality = _currentQuality.value ?: return
        
        // Check if current quality is performing well
        val performanceMetrics = getPerformanceMetrics()
        
        if (performanceMetrics.bufferHealth.status == BufferHealthStatus.CRITICAL) {
            adaptQualityForBuffering(performanceMetrics.bufferHealth)
        }
        
        // Check thermal state
        val thermalState = getThermalState()
        if (thermalState != ThermalState.NORMAL) {
            adaptQualityForThermal(thermalState)
        }
    }
    
    private fun selectOptimalQuality(qualities: List<VideoQuality>): VideoQuality {
        val networkInfo = networkAnalyzer?.getCurrentNetworkInfo()
        val deviceCaps = deviceCapabilities
        
        if (networkInfo == null || deviceCaps == null) {
            // Fallback to medium quality
            return qualities.find { it.height == 720 } ?: qualities.firstOrNull() ?: VideoQuality.default()
        }
        
        return selectOptimalQualityForNetwork(networkInfo, qualities)
    }
    
    private fun selectOptimalQualityForNetwork(networkInfo: NetworkInfo, qualities: List<VideoQuality>): VideoQuality {
        val availableBandwidth = networkInfo.bandwidth * 0.8f // 80% utilization
        
        return qualities
            .filter { it.bitrate <= availableBandwidth }
            .filter { isQualitySupportedByDevice(it) }
            .maxByOrNull { it.bitrate }
            ?: qualities.minByOrNull { it.bitrate }
            ?: VideoQuality.default()
    }
    
    private fun shouldChangeQuality(
        currentQuality: VideoQuality,
        newQuality: VideoQuality,
        networkInfo: NetworkInfo
    ): Boolean {
        // Avoid frequent quality changes
        val lastChange = qualityHistory.lastOrNull()
        if (lastChange != null && System.currentTimeMillis() - lastChange.timestamp < 10000) {
            return false
        }
        
        // Require significant improvement for upgrades
        if (newQuality.bitrate > currentQuality.bitrate) {
            val improvementRatio = newQuality.bitrate.toFloat() / currentQuality.bitrate
            return improvementRatio >= 1.5f && networkInfo.connectionQuality in listOf(ConnectionQuality.GOOD, ConnectionQuality.EXCELLENT)
        }
        
        // Allow downgrades more freely
        return true
    }
    
    private fun selectLowerQuality(
        currentQuality: VideoQuality,
        availableQualities: List<VideoQuality>,
        steps: Int
    ): VideoQuality {
        val sortedQualities = availableQualities.sortedBy { it.bitrate }
        val currentIndex = sortedQualities.indexOfFirst { it.bitrate >= currentQuality.bitrate }
        
        if (currentIndex == -1) return currentQuality
        
        val targetIndex = maxOf(0, currentIndex - steps)
        return sortedQualities[targetIndex]
    }
    
    private fun selectHigherQuality(
        currentQuality: VideoQuality,
        availableQualities: List<VideoQuality>,
        steps: Int
    ): VideoQuality {
        val sortedQualities = availableQualities.sortedBy { it.bitrate }
        val currentIndex = sortedQualities.indexOfFirst { it.bitrate >= currentQuality.bitrate }
        
        if (currentIndex == -1) return currentQuality
        
        val targetIndex = minOf(sortedQualities.size - 1, currentIndex + steps)
        return sortedQualities[targetIndex]
    }
    
    private fun selectLowestSafeQuality(availableQualities: List<VideoQuality>): VideoQuality {
        return availableQualities.minByOrNull { it.bitrate } ?: VideoQuality.default()
    }
    
    private fun applyQualityToPlayer(quality: VideoQuality) {
        // In real implementation, this would configure the ExoPlayer track selection
        // trackSelector?.setParameters(...)
    }
    
    private fun recordQualityChange(
        previousQuality: VideoQuality?,
        newQuality: VideoQuality,
        reason: QualityChangeReason
    ) {
        val isUpgrade = previousQuality != null && newQuality.bitrate > previousQuality.bitrate
        
        qualityHistory.add(
            QualityHistoryEntry(
                timestamp = System.currentTimeMillis(),
                previousQuality = previousQuality,
                newQuality = newQuality,
                reason = reason,
                isUpgrade = isUpgrade
            )
        )
        
        // Keep history manageable
        if (qualityHistory.size > 200) {
            qualityHistory.removeAt(0)
        }
    }
    
    private fun calculateQualityScore(
        quality: VideoQuality,
        networkInfo: NetworkInfo,
        deviceCaps: DeviceCapabilities
    ): Float {
        var score = 0f
        
        // Network compatibility score (40%)
        val networkScore = if (quality.bitrate <= networkInfo.bandwidth * 0.8f) {
            1.0f - (quality.bitrate / (networkInfo.bandwidth * 0.8f))
        } else {
            0.0f
        }
        score += networkScore * 0.4f
        
        // Device compatibility score (30%)
        val deviceScore = if (isQualitySupportedByDevice(quality)) {
            minOf(1.0f, quality.height.toFloat() / deviceCaps.maxSupportedResolution.height)
        } else {
            0.0f
        }
        score += deviceScore * 0.3f
        
        // Quality preference score (20%)
        val qualityScore = quality.height.toFloat() / 1080f // Normalize to 1080p
        score += qualityScore * 0.2f
        
        // Efficiency score (10%)
        val efficiencyScore = 1.0f - (quality.bitrate.toFloat() / 10000000f) // Normalize to 10 Mbps
        score += efficiencyScore * 0.1f
        
        return score.coerceIn(0f, 1f)
    }
    
    private fun generateRecommendationReason(
        quality: VideoQuality,
        networkInfo: NetworkInfo,
        deviceCaps: DeviceCapabilities,
        score: Float
    ): String {
        return when {
            score > 0.8f -> "Excellent choice for current conditions"
            score > 0.6f -> "Good balance of quality and stability"
            score > 0.4f -> "Acceptable for current network conditions"
            score > 0.2f -> "May experience buffering issues"
            else -> "Not recommended for current conditions"
        }
    }
    
    private fun calculateNetworkSuitability(quality: VideoQuality, networkInfo: NetworkInfo): Float {
        val requiredBandwidth = quality.bitrate * 1.2f // 20% overhead
        return minOf(1.0f, networkInfo.bandwidth / requiredBandwidth)
    }
    
    private fun calculateDeviceSuitability(quality: VideoQuality, deviceCaps: DeviceCapabilities): Float {
        val resolutionScore = minOf(1.0f, deviceCaps.maxSupportedResolution.height.toFloat() / quality.height)
        val bitrateScore = minOf(1.0f, deviceCaps.maxDecodingBitrate.toFloat() / quality.bitrate)
        return (resolutionScore + bitrateScore) / 2f
    }
    
    private fun isQualitySupportedByDevice(quality: VideoQuality): Boolean {
        val deviceCaps = deviceCapabilities ?: return true
        
        return quality.height <= deviceCaps.maxSupportedResolution.height &&
               quality.bitrate <= deviceCaps.maxDecodingBitrate
    }
    
    private fun calculateAverageQuality(history: List<QualityHistoryEntry>): VideoQuality? {
        if (history.isEmpty()) return null
        
        val avgBitrate = history.map { it.newQuality.bitrate }.average().toInt()
        val avgHeight = history.map { it.newQuality.height }.average().toInt()
        val avgWidth = history.map { it.newQuality.width }.average().toInt()
        val avgFrameRate = history.map { it.newQuality.frameRate }.average().toFloat()
        
        return VideoQuality(
            id = "average",
            name = "Average",
            width = avgWidth,
            height = avgHeight,
            bitrate = avgBitrate,
            frameRate = avgFrameRate,
            codec = "various"
        )
    }
    
    private fun calculateQualityStability(history: List<QualityHistoryEntry>): Float {
        if (history.size < 2) return 1.0f
        
        val changes = history.zipWithNext().count { (prev, curr) ->
            prev.newQuality.id != curr.newQuality.id
        }
        
        return 1.0f - (changes.toFloat() / history.size)
    }
    
    private fun calculateAverageAdaptationTime(history: List<QualityHistoryEntry>): Long {
        if (history.size < 2) return 0L
        
        val intervals = history.zipWithNext().map { (prev, curr) ->
            curr.timestamp - prev.timestamp
        }
        
        return intervals.average().toLong()
    }
    
    // Device capability estimation methods
    private fun estimateMaxDecodingBitrate(): Int {
        // Simplified estimation based on device tier
        return 10000000 // 10 Mbps as default
    }
    
    private fun isHardwareAccelerationAvailable(): Boolean {
        // Check if hardware acceleration is available
        return true // Assume available for now
    }
    
    private fun getAvailableMemory(): Long {
        val runtime = Runtime.getRuntime()
        return runtime.maxMemory() - (runtime.totalMemory() - runtime.freeMemory())
    }
    
    private fun estimateProcessingPower(): ProcessingPower {
        // Estimate based on device specs
        return ProcessingPower.MEDIUM
    }
    
    private fun isBatteryOptimizationEnabled(): Boolean {
        // Check battery optimization settings
        return false
    }
    
    private fun estimateThermalCapacity(): ThermalCapacity {
        // Estimate thermal handling capacity
        return ThermalCapacity.NORMAL
    }
    
    private fun getPerformanceMetrics(): PerformanceMetrics {
        // Get current performance metrics
        return PerformanceMetrics(
            bufferHealth = BufferHealth(BufferHealthStatus.GOOD, 0.7f),
            frameDrops = 0,
            cpuUsage = 0.3f,
            memoryUsage = 0.5f,
            thermalState = ThermalState.NORMAL
        )
    }
    
    private fun getThermalState(): ThermalState {
        // Get current thermal state
        return ThermalState.NORMAL
    }
    
    fun cleanup() {
        isInitialized = false
        qualityScope.cancel()
        qualityHistory.clear()
    }
}

// Data classes and enums for video quality management
data class VideoQuality(
    val id: String,
    val name: String,
    val width: Int,
    val height: Int,
    val bitrate: Int,
    val frameRate: Float,
    val codec: String
) {
    companion object {
        fun default() = VideoQuality(
            id = "default",
            name = "Auto",
            width = 1280,
            height = 720,
            bitrate = 2000000,
            frameRate = 30f,
            codec = "h264"
        )
    }
}

data class Resolution(
    val width: Int,
    val height: Int
)

data class DeviceCapabilities(
    val maxSupportedResolution: Resolution,
    val maxDecodingBitrate: Int,
    val hardwareAcceleration: Boolean,
    val memoryCapacity: Long,
    val processingPower: ProcessingPower,
    val batteryOptimized: Boolean,
    val thermalCapacity: ThermalCapacity
)

data class AdaptationSettings(
    val enabled: Boolean = true,
    val aggressiveness: AdaptationAggressiveness = AdaptationAggressiveness.BALANCED,
    val maxBitrate: Int = Int.MAX_VALUE,
    val minBitrate: Int = 200000,
    val viewportWidth: Int = 1920,
    val viewportHeight: Int = 1080,
    val bufferThreshold: Float = 0.5f,
    val networkThreshold: Float = 0.6f,
    val thermalProtection: Boolean = true
)

data class QualityRecommendation(
    val quality: VideoQuality,
    val score: Float,
    val recommendation: String,
    val networkSuitability: Float,
    val deviceSuitability: Float
)

data class BufferHealth(
    val status: BufferHealthStatus,
    val level: Float
)

data class PerformanceMetrics(
    val bufferHealth: BufferHealth,
    val frameDrops: Int,
    val cpuUsage: Float,
    val memoryUsage: Float,
    val thermalState: ThermalState
)

data class QualityHistoryEntry(
    val timestamp: Long,
    val previousQuality: VideoQuality?,
    val newQuality: VideoQuality,
    val reason: QualityChangeReason,
    val isUpgrade: Boolean
)

data class QualityStatistics(
    val currentQuality: VideoQuality?,
    val averageQuality: VideoQuality?,
    val qualityStability: Float,
    val adaptationCount: Int,
    val networkAdaptations: Int,
    val bufferAdaptations: Int,
    val thermalAdaptations: Int,
    val manualChanges: Int,
    val averageAdaptationTime: Long,
    val qualityUpgrades: Int,
    val qualityDowngrades: Int
)

data class QualityPreset(
    val name: String,
    val description: String,
    val maxBitrate: Int,
    val preferredHeight: Int,
    val adaptationSettings: AdaptationSettings
)

enum class QualityChangeReason {
    INITIAL_SELECTION,
    MANUAL,
    NETWORK_ADAPTATION,
    BUFFER_ADAPTATION,
    THERMAL_ADAPTATION,
    DEVICE_ADAPTATION
}

enum class AdaptationAggressiveness { CONSERVATIVE, BALANCED, AGGRESSIVE, INTELLIGENT }
enum class BufferHealthStatus { CRITICAL, LOW, GOOD, EXCELLENT }
enum class ThermalState { NORMAL, LIGHT, MODERATE, SEVERE, CRITICAL }
enum class ProcessingPower { LOW, MEDIUM, HIGH, VERY_HIGH }
enum class ThermalCapacity { LOW, NORMAL, HIGH }

sealed class QualityEvent {
    data class Initialized(val deviceCapabilities: DeviceCapabilities) : QualityEvent()
    data class QualitiesAvailable(val qualities: List<VideoQuality>) : QualityEvent()
    data class QualityChanged(val previous: VideoQuality?, val new: VideoQuality, val reason: QualityChangeReason) : QualityEvent()
    data class AutoAdaptationChanged(val enabled: Boolean) : QualityEvent()
    data class AdaptationSettingsChanged(val settings: AdaptationSettings) : QualityEvent()
    data class RecommendationsUpdated(val recommendations: List<QualityRecommendation>) : QualityEvent()
    data class Error(val message: String, val exception: Exception) : QualityEvent()
}