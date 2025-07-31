// ================================
// Premium Streaming Features
// Adaptive bitrate, intelligent pre-buffering, offline downloads
// ================================

// 1. Advanced Streaming Engine
@Singleton
class AdvancedStreamingEngine @Inject constructor(
    private val context: Context,
    private val networkMonitor: NetworkMonitor,
    private val adaptiveBitrateController: AdaptiveBitrateController,
    private val intelligentPreBuffer: IntelligentPreBufferService,
    private val downloadManager: OfflineDownloadManager,
    private val p2pStreamingService: P2PStreamingService
) {
    
    private var currentStreamingSession: StreamingSession? = null
    private var streamingCallbacks: StreamingCallbacks? = null
    
    fun initializeStreaming(
        mediaItem: MediaItem,
        callbacks: StreamingCallbacks
    ): StreamingSession {
        this.streamingCallbacks = callbacks
        
        val session = StreamingSession(
            id = UUID.randomUUID().toString(),
            mediaItem = mediaItem,
            networkCondition = networkMonitor.getCurrentCondition(),
            startTime = System.currentTimeMillis()
        )
        
        currentStreamingSession = session
        
        // Initialize adaptive bitrate based on network
        adaptiveBitrateController.initialize(session, networkMonitor.getCurrentCondition())
        
        // Start intelligent pre-buffering
        intelligentPreBuffer.startPreBuffering(session)
        
        // Check for P2P opportunities
        if (p2pStreamingService.isAvailable()) {
            p2pStreamingService.checkForLocalPeers(mediaItem.localConfiguration?.uri.toString())
        }
        
        return session
    }
    
    fun optimizeForNetworkCondition(condition: NetworkCondition) {
        currentStreamingSession?.let { session ->
            adaptiveBitrateController.adaptToNetworkCondition(condition)
            
            when (condition.quality) {
                NetworkQuality.POOR -> {
                    // Switch to aggressive buffering and lower quality
                    intelligentPreBuffer.setAggressiveMode(true)
                    streamingCallbacks?.onQualityRecommendation(VideoQuality.SD_480P)
                }
                NetworkQuality.GOOD -> {
                    // Balanced approach
                    intelligentPreBuffer.setAggressiveMode(false)
                    streamingCallbacks?.onQualityRecommendation(VideoQuality.HD_720P)
                }
                NetworkQuality.EXCELLENT -> {
                    // Maximum quality and minimal buffering
                    intelligentPreBuffer.setAggressiveMode(false)
                    streamingCallbacks?.onQualityRecommendation(VideoQuality.FHD_1080P)
                }
            }
        }
    }
    
    fun handleBufferingEvent(bufferedPercentage: Int, isBuffering: Boolean) {
        currentStreamingSession?.let { session ->
            session.bufferingHistory.add(
                BufferingEvent(
                    timestamp = System.currentTimeMillis(),
                    bufferedPercentage = bufferedPercentage,
                    isBuffering = isBuffering,
                    networkCondition = networkMonitor.getCurrentCondition()
                )
            )
            
            // Analyze buffering patterns for future optimization
            if (isBuffering) {
                analyzeBufferingCause(session)
            }
        }
    }
    
    private fun analyzeBufferingCause(session: StreamingSession) {
        val recentBufferingEvents = session.bufferingHistory.takeLast(5)
        
        // Check if buffering is due to network issues
        if (recentBufferingEvents.all { it.networkCondition.quality == NetworkQuality.POOR }) {
            streamingCallbacks?.onBufferingAnalysis(
                BufferingCause.POOR_NETWORK,
                "Consider lowering video quality for smoother playback"
            )
        }
        
        // Check if buffering is frequent
        val bufferingFrequency = recentBufferingEvents.count { it.isBuffering }
        if (bufferingFrequency > 3) {
            streamingCallbacks?.onBufferingAnalysis(
                BufferingCause.FREQUENT_BUFFERING,
                "Enabling aggressive pre-buffering"
            )
            intelligentPreBuffer.setAggressiveMode(true)
        }
    }
    
    fun predictOptimalQuality(): VideoQuality {
        val networkCondition = networkMonitor.getCurrentCondition()
        val deviceCapabilities = getDeviceCapabilities()
        
        return adaptiveBitrateController.predictOptimalQuality(networkCondition, deviceCapabilities)
    }
    
    private fun getDeviceCapabilities(): DeviceCapabilities {
        val displayMetrics = context.resources.displayMetrics
        val maxResolution = maxOf(displayMetrics.widthPixels, displayMetrics.heightPixels)
        
        return DeviceCapabilities(
            maxResolution = when {
                maxResolution >= 2160 -> VideoQuality.UHD_4K
                maxResolution >= 1080 -> VideoQuality.FHD_1080P
                maxResolution >= 720 -> VideoQuality.HD_720P
                else -> VideoQuality.SD_480P
            },
            supportsHardwareDecoding = checkHardwareDecodingSupport(),
            availableMemoryMB = getAvailableMemoryMB(),
            cpuCores = Runtime.getRuntime().availableProcessors()
        )
    }
    
    fun getStreamingStats(): StreamingStats {
        return currentStreamingSession?.let { session ->
            StreamingStats(
                sessionDuration = System.currentTimeMillis() - session.startTime,
                totalBufferingTime = calculateTotalBufferingTime(session),
                averageQuality = calculateAverageQuality(session),
                qualitySwitches = session.qualitySwitches.size,
                bytesDownloaded = session.bytesDownloaded,
                averageBitrate = session.averageBitrate
            )
        } ?: StreamingStats()
    }
    
    private fun calculateTotalBufferingTime(session: StreamingSession): Long {
        return session.bufferingHistory
            .filter { it.isBuffering }
            .sumOf { System.currentTimeMillis() - it.timestamp }
    }
    
    private fun calculateAverageQuality(session: StreamingSession): VideoQuality {
        if (session.qualitySwitches.isEmpty()) return VideoQuality.HD_720P
        
        val qualityDurations = mutableMapOf<VideoQuality, Long>()
        var lastSwitchTime = session.startTime
        
        session.qualitySwitches.forEach { switch ->
            val duration = switch.timestamp - lastSwitchTime
            qualityDurations[switch.fromQuality] = 
                qualityDurations.getOrDefault(switch.fromQuality, 0L) + duration
            lastSwitchTime = switch.timestamp
        }
        
        return qualityDurations.maxByOrNull { it.value }?.key ?: VideoQuality.HD_720P
    }
    
    private fun checkHardwareDecodingSupport(): Boolean {
        // Check for hardware decoding capabilities
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP
    }
    
    private fun getAvailableMemoryMB(): Int {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)
        return (memoryInfo.availMem / 1024 / 1024).toInt()
    }
}

// 2. Adaptive Bitrate Controller
@Singleton
class AdaptiveBitrateController @Inject constructor(
    private val networkMonitor: NetworkMonitor
) {
    
    private var currentSession: StreamingSession? = null
    private var qualityLevels = listOf(
        QualityLevel(VideoQuality.SD_360P, 500_000), // 500 Kbps
        QualityLevel(VideoQuality.SD_480P, 1_000_000), // 1 Mbps
        QualityLevel(VideoQuality.HD_720P, 2_500_000), // 2.5 Mbps
        QualityLevel(VideoQuality.FHD_1080P, 5_000_000), // 5 Mbps
        QualityLevel(VideoQuality.UHD_4K, 15_000_000) // 15 Mbps
    )
    
    fun initialize(session: StreamingSession, networkCondition: NetworkCondition) {
        this.currentSession = session
        
        // Set initial quality based on network
        val initialQuality = selectInitialQuality(networkCondition)
        session.currentQuality = initialQuality
    }
    
    fun adaptToNetworkCondition(condition: NetworkCondition) {
        currentSession?.let { session ->
            val optimalQuality = selectOptimalQuality(condition, session.currentQuality)
            
            if (optimalQuality != session.currentQuality) {
                switchQuality(session, optimalQuality, QualitySwitchReason.NETWORK_CHANGE)
            }
        }
    }
    
    fun predictOptimalQuality(
        networkCondition: NetworkCondition,
        deviceCapabilities: DeviceCapabilities
    ): VideoQuality {
        val networkBasedQuality = selectOptimalQuality(networkCondition, null)
        val deviceMaxQuality = deviceCapabilities.maxResolution
        
        return minOf(networkBasedQuality, deviceMaxQuality)
    }
    
    private fun selectInitialQuality(networkCondition: NetworkCondition): VideoQuality {
        return when (networkCondition.quality) {
            NetworkQuality.POOR -> VideoQuality.SD_360P
            NetworkQuality.GOOD -> VideoQuality.HD_720P
            NetworkQuality.EXCELLENT -> VideoQuality.FHD_1080P
        }
    }
    
    private fun selectOptimalQuality(
        condition: NetworkCondition,
        currentQuality: VideoQuality?
    ): VideoQuality {
        val availableBandwidth = condition.downloadSpeedBps
        val safetyMargin = 0.8 // Use 80% of available bandwidth for safety
        val usableBandwidth = (availableBandwidth * safetyMargin).toLong()
        
        // Find the highest quality that fits within bandwidth
        val optimalLevel = qualityLevels
            .filter { it.requiredBitrate <= usableBandwidth }
            .maxByOrNull { it.requiredBitrate }
        
        return optimalLevel?.quality ?: VideoQuality.SD_360P
    }
    
    private fun switchQuality(
        session: StreamingSession,
        newQuality: VideoQuality,
        reason: QualitySwitchReason
    ) {
        val switch = QualitySwitch(
            timestamp = System.currentTimeMillis(),
            fromQuality = session.currentQuality,
            toQuality = newQuality,
            reason = reason
        )
        
        session.qualitySwitches.add(switch)
        session.currentQuality = newQuality
        
        Log.i("AdaptiveBitrate", "Quality switched: ${switch.fromQuality} -> ${switch.toQuality} (${reason})")
    }
}

// 3. Intelligent Pre-Buffer Service
@Singleton
class IntelligentPreBufferService @Inject constructor(
    private val context: Context,
    private val userBehaviorAnalyzer: UserBehaviorAnalyzer,
    private val contentPredictor: ContentPredictor
) {
    
    private var isAggressiveMode = false
    private var preBufferTargets = mutableMapOf<String, PreBufferTarget>()
    
    fun startPreBuffering(session: StreamingSession) {
        val userPatterns = userBehaviorAnalyzer.getUserWatchingPatterns()
        val contentPredictions = contentPredictor.predictNextContent(session.mediaItem)
        
        // Pre-buffer current video segments
        preBufferCurrentVideo(session, userPatterns)
        
        // Pre-buffer likely next videos
        contentPredictions.forEach { prediction ->
            if (prediction.confidence > 0.7f) {
                preBufferVideo(prediction.mediaItem, prediction.confidence)
            }
        }
    }
    
    private fun preBufferCurrentVideo(session: StreamingSession, patterns: UserWatchingPatterns) {
        val currentPosition = session.currentPositionMs
        val videoDuration = session.mediaDurationMs
        
        // Calculate intelligent buffering strategy
        val bufferStrategy = when {
            patterns.averageWatchPercentage > 0.8f -> {
                // User usually watches full videos - buffer more ahead
                BufferStrategy.FULL_VIDEO
            }
            patterns.skipRate > 0.3f -> {
                // User skips a lot - buffer in chunks
                BufferStrategy.CHUNK_BASED
            }
            else -> BufferStrategy.BALANCED
        }
        
        val bufferTargets = calculateBufferTargets(currentPosition, videoDuration, bufferStrategy)
        
        bufferTargets.forEach { target ->
            scheduleBuffering(session.mediaItem.localConfiguration?.uri.toString(), target)
        }
    }
    
    private fun preBufferVideo(mediaItem: MediaItem, confidence: Float) {
        val bufferAmount = when {
            confidence > 0.9f -> 30_000L // 30 seconds
            confidence > 0.8f -> 15_000L // 15 seconds
            else -> 5_000L // 5 seconds
        }
        
        val target = PreBufferTarget(
            startPositionMs = 0L,
            endPositionMs = bufferAmount,
            priority = (confidence * 10).toInt()
        )
        
        mediaItem.localConfiguration?.uri?.toString()?.let { uri ->
            preBufferTargets[uri] = target
            scheduleBuffering(uri, target)
        }
    }
    
    private fun calculateBufferTargets(
        currentPosition: Long,
        videoDuration: Long,
        strategy: BufferStrategy
    ): List<PreBufferTarget> {
        return when (strategy) {
            BufferStrategy.FULL_VIDEO -> {
                listOf(
                    PreBufferTarget(
                        startPositionMs = currentPosition,
                        endPositionMs = videoDuration,
                        priority = 5
                    )
                )
            }
            
            BufferStrategy.CHUNK_BASED -> {
                val chunkSize = if (isAggressiveMode) 60_000L else 30_000L // 30-60 seconds
                val chunks = mutableListOf<PreBufferTarget>()
                
                var chunkStart = currentPosition
                while (chunkStart < videoDuration) {
                    val chunkEnd = minOf(chunkStart + chunkSize, videoDuration)
                    chunks.add(
                        PreBufferTarget(
                            startPositionMs = chunkStart,
                            endPositionMs = chunkEnd,
                            priority = when {
                                chunkStart - currentPosition < 10_000L -> 10 // High priority for immediate chunks
                                chunkStart - currentPosition < 30_000L -> 7  // Medium priority
                                else -> 3 // Low priority for distant chunks
                            }
                        )
                    )
                    chunkStart = chunkEnd
                }
                
                chunks
            }
            
            BufferStrategy.BALANCED -> {
                val immediateBuffer = 15_000L // 15 seconds ahead
                val extendedBuffer = if (isAggressiveMode) 120_000L else 60_000L // 1-2 minutes
                
                listOf(
                    PreBufferTarget(
                        startPositionMs = currentPosition,
                        endPositionMs = minOf(currentPosition + immediateBuffer, videoDuration),
                        priority = 10
                    ),
                    PreBufferTarget(
                        startPositionMs = currentPosition + immediateBuffer,
                        endPositionMs = minOf(currentPosition + extendedBuffer, videoDuration),
                        priority = 5
                    )
                )
            }
        }
    }
    
    private fun scheduleBuffering(videoUri: String, target: PreBufferTarget) {
        // Schedule buffering task with WorkManager
        val bufferRequest = OneTimeWorkRequestBuilder<PreBufferWorker>()
            .setInputData(workDataOf(
                "video_uri" to videoUri,
                "start_position" to target.startPositionMs,
                "end_position" to target.endPositionMs,
                "priority" to target.priority
            ))
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .build()
        
        WorkManager.getInstance(context).enqueue(bufferRequest)
    }
    
    fun setAggressiveMode(enabled: Boolean) {
        isAggressiveMode = enabled
    }
}

// 4. User Behavior Analyzer
@Singleton
class UserBehaviorAnalyzer @Inject constructor(
    private val watchingHistoryRepository: WatchingHistoryRepository
) {
    
    suspend fun getUserWatchingPatterns(): UserWatchingPatterns {
        val recentSessions = watchingHistoryRepository.getRecentSessions(30) // Last 30 days
        
        if (recentSessions.isEmpty()) {
            return UserWatchingPatterns() // Default patterns
        }
        
        val totalSessions = recentSessions.size
        val completedSessions = recentSessions.count { it.watchPercentage > 0.9f }
        val averageWatchPercentage = recentSessions.map { it.watchPercentage }.average().toFloat()
        val skipEvents = recentSessions.sumOf { it.skipEvents }
        val totalSkips = recentSessions.sumOf { it.totalSeekEvents }
        
        val preferredTimes = analyzePreferredWatchingTimes(recentSessions)
        val preferredGenres = analyzePreferredGenres(recentSessions)
        
        return UserWatchingPatterns(
            averageWatchPercentage = averageWatchPercentage,
            completionRate = completedSessions.toFloat() / totalSessions,
            skipRate = if (totalSkips > 0) skipEvents.toFloat() / totalSkips else 0f,
            preferredWatchingTimes = preferredTimes,
            preferredGenres = preferredGenres,
            averageSessionDuration = recentSessions.map { it.duration }.average().toLong()
        )
    }
    
    private fun analyzePreferredWatchingTimes(sessions: List<WatchingSession>): List<Int> {
        return sessions
            .groupBy { Calendar.getInstance().apply { timeInMillis = it.startTime }.get(Calendar.HOUR_OF_DAY) }
            .entries
            .sortedByDescending { it.value.size }
            .take(3)
            .map { it.key }
    }
    
    private fun analyzePreferredGenres(sessions: List<WatchingSession>): List<String> {
        return sessions
            .mapNotNull { it.contentGenre }
            .groupBy { it }
            .entries
            .sortedByDescending { it.value.size }
            .take(5)
            .map { it.key }
    }
}

// 5. Content Predictor
@Singleton
class ContentPredictor @Inject constructor(
    private val userBehaviorAnalyzer: UserBehaviorAnalyzer,
    private val contentRepository: ContentRepository
) {
    
    suspend fun predictNextContent(currentMedia: MediaItem): List<ContentPrediction> {
        val userPatterns = userBehaviorAnalyzer.getUserWatchingPatterns()
        val predictions = mutableListOf<ContentPrediction>()
        
        // Predict based on playlist position
        currentMedia.mediaMetadata.extras?.getString("playlist_id")?.let { playlistId ->
            val nextInPlaylist = contentRepository.getNextInPlaylist(playlistId, currentMedia.mediaId)
            nextInPlaylist?.let { nextItem ->
                predictions.add(
                    ContentPrediction(
                        mediaItem = nextItem,
                        confidence = 0.9f,
                        reason = PredictionReason.PLAYLIST_SEQUENCE
                    )
                )
            }
        }
        
        // Predict based on user preferences
        val similarContent = contentRepository.getSimilarContent(
            currentMedia,
            userPatterns.preferredGenres
        )
        
        similarContent.forEach { content ->
            val confidence = calculateSimilarityConfidence(content, userPatterns)
            if (confidence > 0.5f) {
                predictions.add(
                    ContentPrediction(
                        mediaItem = content,
                        confidence = confidence,
                        reason = PredictionReason.CONTENT_SIMILARITY
                    )
                )
            }
        }
        
        // Predict based on watching history patterns
        val historyBasedPredictions = predictFromHistory(currentMedia, userPatterns)
        predictions.addAll(historyBasedPredictions)
        
        return predictions.sortedByDescending { it.confidence }.take(3)
    }
    
    private fun calculateSimilarityConfidence(
        content: MediaItem,
        userPatterns: UserWatchingPatterns
    ): Float {
        var confidence = 0.5f // Base confidence
        
        // Check genre preference
        content.mediaMetadata.extras?.getString("genre")?.let { genre ->
            if (userPatterns.preferredGenres.contains(genre)) {
                confidence += 0.3f
            }
        }
        
        // Check content duration vs user patterns
        content.mediaMetadata.extras?.getLong("duration")?.let { duration ->
            val durationDiff = abs(duration - userPatterns.averageSessionDuration)
            val durationSimilarity = 1f - (durationDiff.toFloat() / userPatterns.averageSessionDuration)
            confidence += durationSimilarity * 0.2f
        }
        
        return confidence.coerceIn(0f, 1f)
    }
    
    private suspend fun predictFromHistory(
        currentMedia: MediaItem,
        userPatterns: UserWatchingPatterns
    ): List<ContentPrediction> {
        // Implementation for history-based predictions
        // This would analyze what users typically watch after similar content
        return emptyList() // Placeholder
    }
}

// 6. Offline Download Manager
@Singleton
class OfflineDownloadManager @Inject constructor(
    private val context: Context,
    private val downloadRepository: DownloadRepository,
    private val compressionService: VideoCompressionService
) {
    
    fun scheduleDownload(
        mediaItem: MediaItem,
        quality: VideoQuality,
        downloadSettings: DownloadSettings
    ): String {
        val downloadId = UUID.randomUUID().toString()
        
        val downloadRequest = DownloadRequest(
            id = downloadId,
            mediaItem = mediaItem,
            quality = quality,
            estimatedSize = estimateDownloadSize(mediaItem, quality),
            compressionLevel = downloadSettings.compressionLevel,
            downloadOnlyOnWifi = downloadSettings.wifiOnly,
            scheduledTime = if (downloadSettings.scheduleForLater) 
                calculateOptimalDownloadTime() else System.currentTimeMillis()
        )
        
        val workRequest = OneTimeWorkRequestBuilder<DownloadWorker>()
            .setInputData(workDataOf(
                "download_request" to Gson().toJson(downloadRequest)
            ))
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(
                        if (downloadSettings.wifiOnly) NetworkType.UNMETERED else NetworkType.CONNECTED
                    )
                    .setRequiresBatteryNotLow(true)
                    .setRequiresStorageNotLow(true)
                    .build()
            )
            .build()
        
        WorkManager.getInstance(context).enqueue(workRequest)
        
        return downloadId
    }
    
    private fun estimateDownloadSize(mediaItem: MediaItem, quality: VideoQuality): Long {
        val durationMs = mediaItem.mediaMetadata.extras?.getLong("duration") ?: 0L
        val durationSeconds = durationMs / 1000
        
        val bitrate = when (quality) {
            VideoQuality.SD_360P -> 500_000L
            VideoQuality.SD_480P -> 1_000_000L
            VideoQuality.HD_720P -> 2_500_000L
            VideoQuality.FHD_1080P -> 5_000_000L
            VideoQuality.UHD_4K -> 15_000_000L
        }
        
        return (durationSeconds * bitrate) / 8 // Convert to bytes
    }
    
    private fun calculateOptimalDownloadTime(): Long {
        // Calculate the best time to download based on user patterns and network conditions
        val now = Calendar.getInstance()
        val optimalHour = when {
            now.get(Calendar.HOUR_OF_DAY) < 6 -> 2 // Early morning (2 AM)
            now.get(Calendar.HOUR_OF_DAY) > 22 -> 2 // Late night, schedule for next early morning
            else -> 22 // Schedule for evening
        }
        
        return Calendar.getInstance().apply {
            if (optimalHour <= get(Calendar.HOUR_OF_DAY)) {
                add(Calendar.DAY_OF_MONTH, 1) // Next day
            }
            set(Calendar.HOUR_OF_DAY, optimalHour)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }.timeInMillis
    }
}

// 7. P2P Streaming Service
@Singleton
class P2PStreamingService @Inject constructor(
    private val context: Context
) {
    
    private var isAvailable = false
    private val nearbyPeers = mutableMapOf<String, PeerInfo>()
    
    fun isAvailable(): Boolean = isAvailable
    
    suspend fun checkForLocalPeers(contentId: String): List<PeerInfo> {
        return withContext(Dispatchers.IO) {
            // Use Wi-Fi Direct or similar technology to find local peers
            // who have the same content
            discoverNearbyPeers(contentId)
        }
    }
    
    private suspend fun discoverNearbyPeers(contentId: String): List<PeerInfo> {
        // Implementation would use Android's Wi-Fi Direct API
        // to discover nearby devices with the same content
        return emptyList() // Placeholder
    }
    
    fun startP2PSharing(mediaItem: MediaItem) {
        // Start sharing current media with nearby peers
    }
    
    fun stopP2PSharing() {
        // Stop P2P sharing
    }
}

// 8. Data Classes
data class StreamingSession(
    val id: String,
    val mediaItem: MediaItem,
    var currentQuality: VideoQuality = VideoQuality.HD_720P,
    val networkCondition: NetworkCondition,
    val startTime: Long,
    var currentPositionMs: Long = 0L,
    var mediaDurationMs: Long = 0L,
    val bufferingHistory: MutableList<BufferingEvent> = mutableListOf(),
    val qualitySwitches: MutableList<QualitySwitch> = mutableListOf(),
    var bytesDownloaded: Long = 0L,
    var averageBitrate: Int = 0
)

data class BufferingEvent(
    val timestamp: Long,
    val bufferedPercentage: Int,
    val isBuffering: Boolean,
    val networkCondition: NetworkCondition
)

data class QualitySwitch(
    val timestamp: Long,
    val fromQuality: VideoQuality,
    val toQuality: VideoQuality,
    val reason: QualitySwitchReason
)

data class QualityLevel(
    val quality: VideoQuality,
    val requiredBitrate: Long
)

data class DeviceCapabilities(
    val maxResolution: VideoQuality,
    val supportsHardwareDecoding: Boolean,
    val availableMemoryMB: Int,
    val cpuCores: Int
)

data class PreBufferTarget(
    val startPositionMs: Long,
    val endPositionMs: Long,
    val priority: Int
)

data class UserWatchingPatterns(
    val averageWatchPercentage: Float = 0.7f,
    val completionRate: Float = 0.6f,
    val skipRate: Float = 0.2f,
    val preferredWatchingTimes: List<Int> = listOf(19, 20, 21), // 7-9 PM
    val preferredGenres: List<String> = emptyList(),
    val averageSessionDuration: Long = 1800_000L // 30 minutes
)

data class ContentPrediction(
    val mediaItem: MediaItem,
    val confidence: Float,
    val reason: PredictionReason
)

data class DownloadRequest(
    val id: String,
    val mediaItem: MediaItem,
    val quality: VideoQuality,
    val estimatedSize: Long,
    val compressionLevel: CompressionLevel,
    val downloadOnlyOnWifi: Boolean,
    val scheduledTime: Long
)

data class DownloadSettings(
    val compressionLevel: CompressionLevel = CompressionLevel.BALANCED,
    val wifiOnly: Boolean = true,
    val scheduleForLater: Boolean = false
)

data class PeerInfo(
    val deviceId: String,
    val deviceName: String,
    val availableContent: List<String>,
    val signalStrength: Int
)

data class StreamingStats(
    val sessionDuration: Long = 0L,
    val totalBufferingTime: Long = 0L,
    val averageQuality: VideoQuality = VideoQuality.HD_720P,
    val qualitySwitches: Int = 0,
    val bytesDownloaded: Long = 0L,
    val averageBitrate: Int = 0
)

enum class VideoQuality(val displayName: String, val resolution: String) {
    SD_360P("360p", "640x360"),
    SD_480P("480p", "854x480"),
    HD_720P("720p", "1280x720"),
    FHD_1080P("1080p", "1920x1080"),
    UHD_4K("4K", "3840x2160")
}

enum class NetworkQuality { POOR, GOOD, EXCELLENT }

enum class QualitySwitchReason {
    NETWORK_CHANGE, USER_PREFERENCE, DEVICE_PERFORMANCE, BUFFER_HEALTH
}

enum class BufferingCause {
    POOR_NETWORK, FREQUENT_BUFFERING, DEVICE_PERFORMANCE, CONTENT_UNAVAILABLE
}

enum class BufferStrategy { FULL_VIDEO, CHUNK_BASED, BALANCED }

enum class PredictionReason { PLAYLIST_SEQUENCE, CONTENT_SIMILARITY, USER_HISTORY }

enum class CompressionLevel { NONE, LIGHT, BALANCED, AGGRESSIVE }

// 9. Streaming Callbacks Interface
interface StreamingCallbacks {
    fun onQualityRecommendation(quality: VideoQuality)
    fun onBufferingAnalysis(cause: BufferingCause, suggestion: String)
    fun onStreamingStatsUpdate(stats: StreamingStats)
    fun onP2PPeerDiscovered(peer: PeerInfo)
    fun onDownloadProgress(downloadId: String, progress: Float)
    fun onDownloadComplete(downloadId: String, success: Boolean)
}