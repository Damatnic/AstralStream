// ================================
// WEEK 1 - DAY 3-4: PERFORMANCE OPTIMIZATION
// ================================

// 1. Performance Monitor
@Singleton
class PerformanceMonitor @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val metrics = mutableMapOf<String, PerformanceMetric>()
    private val frameMetricsHandler = Handler(Looper.getMainLooper())
    
    data class PerformanceMetric(
        val name: String,
        var totalTime: Long = 0,
        var count: Int = 0,
        var minTime: Long = Long.MAX_VALUE,
        var maxTime: Long = 0,
        var lastTime: Long = 0
    ) {
        val averageTime: Long get() = if (count > 0) totalTime / count else 0
    }
    
    fun startMeasure(tag: String): Long {
        return System.nanoTime()
    }
    
    fun endMeasure(tag: String, startTime: Long) {
        val duration = (System.nanoTime() - startTime) / 1_000_000 // Convert to ms
        
        val metric = metrics.getOrPut(tag) { PerformanceMetric(tag) }
        metric.totalTime += duration
        metric.count++
        metric.minTime = minOf(metric.minTime, duration)
        metric.maxTime = maxOf(metric.maxTime, duration)
        metric.lastTime = duration
        
        if (BuildConfig.DEBUG) {
            Log.d("Performance", "$tag: ${duration}ms (avg: ${metric.averageTime}ms)")
        }
    }
    
    fun logMetrics() {
        metrics.forEach { (name, metric) ->
            Log.i("Performance", """
                $name:
                  Average: ${metric.averageTime}ms
                  Min: ${metric.minTime}ms
                  Max: ${metric.maxTime}ms
                  Count: ${metric.count}
            """.trimIndent())
        }
    }
    
    fun trackFrameMetrics(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            activity.window.addOnFrameMetricsAvailableListener({ _, frameMetrics, _ ->
                val totalDuration = frameMetrics.getMetric(FrameMetrics.TOTAL_DURATION) / 1_000_000
                if (totalDuration > 16) { // More than 16ms = dropped frame
                    Log.w("Performance", "Dropped frame: ${totalDuration}ms")
                }
            }, frameMetricsHandler)
        }
    }
}

// 2. Startup Optimizer
@Singleton
class StartupOptimizer @Inject constructor(
    @ApplicationContext private val context: Context,
    private val performanceMonitor: PerformanceMonitor,
    private val videoCache: VideoCache,
    private val preloader: VideoPreloader
) {
    
    private val startupTasks = mutableListOf<StartupTask>()
    private val criticalTasks = mutableListOf<StartupTask>()
    
    interface StartupTask {
        val name: String
        val priority: Int
        suspend fun execute()
    }
    
    fun addCriticalTask(task: StartupTask) {
        criticalTasks.add(task)
    }
    
    fun addBackgroundTask(task: StartupTask) {
        startupTasks.add(task)
    }
    
    suspend fun optimize() {
        val startTime = performanceMonitor.startMeasure("app_startup")
        
        // Execute critical tasks first (UI must wait)
        coroutineScope {
            criticalTasks.sortedBy { it.priority }.forEach { task ->
                val taskStart = performanceMonitor.startMeasure("startup_${task.name}")
                task.execute()
                performanceMonitor.endMeasure("startup_${task.name}", taskStart)
            }
        }
        
        // Execute background tasks (UI doesn't wait)
        GlobalScope.launch(Dispatchers.IO) {
            startupTasks.sortedBy { it.priority }.forEach { task ->
                val taskStart = performanceMonitor.startMeasure("background_${task.name}")
                try {
                    task.execute()
                } catch (e: Exception) {
                    Log.e("StartupOptimizer", "Background task failed: ${task.name}", e)
                }
                performanceMonitor.endMeasure("background_${task.name}", taskStart)
            }
        }
        
        performanceMonitor.endMeasure("app_startup", startTime)
    }
    
    init {
        // Add default startup tasks
        addCriticalTask(object : StartupTask {
            override val name = "init_cache"
            override val priority = 1
            override suspend fun execute() {
                videoCache.initialize()
            }
        })
        
        addBackgroundTask(object : StartupTask {
            override val name = "preload_recent"
            override val priority = 10
            override suspend fun execute() {
                preloader.preloadRecentVideos()
            }
        })
        
        addBackgroundTask(object : StartupTask {
            override val name = "cleanup_cache"
            override val priority = 20
            override suspend fun execute() {
                videoCache.cleanupOldCache()
            }
        })
    }
}

// 3. Video Cache Manager
@Singleton
class VideoCache @Inject constructor(
    @ApplicationContext private val context: Context,
    private val performanceMonitor: PerformanceMonitor
) {
    private val cacheDir = File(context.cacheDir, "video_cache")
    private val maxCacheSize = 500 * 1024 * 1024L // 500MB
    private val memoryCacheSize = 50 * 1024 * 1024L // 50MB in memory
    
    private val memoryCache = LruCache<String, CachedVideo>(memoryCacheSize.toInt())
    private val diskCache = DiskLruCache.open(cacheDir, 1, 1, maxCacheSize)
    
    data class CachedVideo(
        val id: String,
        val path: String,
        val thumbnail: Bitmap?,
        val metadata: VideoMetadata,
        val lastAccessed: Long = System.currentTimeMillis()
    )
    
    data class VideoMetadata(
        val duration: Long,
        val width: Int,
        val height: Int,
        val frameRate: Float,
        val bitrate: Int
    )
    
    suspend fun initialize() = withContext(Dispatchers.IO) {
        if (!cacheDir.exists()) {
            cacheDir.mkdirs()
        }
    }
    
    suspend fun getCachedVideo(videoPath: String): CachedVideo? = withContext(Dispatchers.IO) {
        val startTime = performanceMonitor.startMeasure("cache_get")
        
        val key = videoPath.hashCode().toString()
        
        // Check memory cache first
        memoryCache.get(key)?.let {
            performanceMonitor.endMeasure("cache_get", startTime)
            return@withContext it
        }
        
        // Check disk cache
        try {
            diskCache.get(key)?.let { snapshot ->
                val metadata = Json.decodeFromString<VideoMetadata>(
                    snapshot.getInputStream(0).bufferedReader().readText()
                )
                val cachedVideo = CachedVideo(
                    id = key,
                    path = videoPath,
                    thumbnail = null, // Load separately if needed
                    metadata = metadata
                )
                
                // Add to memory cache
                memoryCache.put(key, cachedVideo)
                
                performanceMonitor.endMeasure("cache_get", startTime)
                return@withContext cachedVideo
            }
        } catch (e: Exception) {
            Log.e("VideoCache", "Failed to read from disk cache", e)
        }
        
        performanceMonitor.endMeasure("cache_get", startTime)
        null
    }
    
    suspend fun cacheVideo(videoPath: String): CachedVideo? = withContext(Dispatchers.IO) {
        val startTime = performanceMonitor.startMeasure("cache_put")
        
        try {
            val metadata = extractVideoMetadata(videoPath)
            val thumbnail = generateThumbnail(videoPath)
            
            val cachedVideo = CachedVideo(
                id = videoPath.hashCode().toString(),
                path = videoPath,
                thumbnail = thumbnail,
                metadata = metadata
            )
            
            // Add to memory cache
            memoryCache.put(cachedVideo.id, cachedVideo)
            
            // Add to disk cache
            val editor = diskCache.edit(cachedVideo.id)
            editor?.let {
                it.set(0, Json.encodeToString(metadata))
                it.commit()
            }
            
            performanceMonitor.endMeasure("cache_put", startTime)
            return@withContext cachedVideo
        } catch (e: Exception) {
            Log.e("VideoCache", "Failed to cache video", e)
            performanceMonitor.endMeasure("cache_put", startTime)
            return@withContext null
        }
    }
    
    suspend fun cleanupOldCache() = withContext(Dispatchers.IO) {
        val startTime = performanceMonitor.startMeasure("cache_cleanup")
        
        try {
            // Remove entries older than 7 days
            val cutoffTime = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000)
            
            // This is simplified - real implementation would track access times
            diskCache.flush()
            
            performanceMonitor.endMeasure("cache_cleanup", startTime)
        } catch (e: Exception) {
            Log.e("VideoCache", "Failed to cleanup cache", e)
            performanceMonitor.endMeasure("cache_cleanup", startTime)
        }
    }
    
    private suspend fun extractVideoMetadata(path: String): VideoMetadata = withContext(Dispatchers.IO) {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(path)
            
            VideoMetadata(
                duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLong() ?: 0,
                width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toInt() ?: 0,
                height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toInt() ?: 0,
                frameRate = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CAPTURE_FRAMERATE)?.toFloat() ?: 0f,
                bitrate = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)?.toInt() ?: 0
            )
        } finally {
            retriever.release()
        }
    }
    
    private suspend fun generateThumbnail(path: String): Bitmap? = withContext(Dispatchers.IO) {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(path)
            retriever.getFrameAtTime(1000000)?.let { bitmap ->
                // Scale down for memory efficiency
                Bitmap.createScaledBitmap(bitmap, 320, 180, true)
            }
        } catch (e: Exception) {
            null
        } finally {
            retriever.release()
        }
    }
}

// 4. Video Preloader
@Singleton
class VideoPreloader @Inject constructor(
    @ApplicationContext private val context: Context,
    private val videoCache: VideoCache,
    private val performanceMonitor: PerformanceMonitor,
    private val playbackHistory: PlaybackHistory
) {
    
    private val preloadScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val activePreloads = mutableMapOf<String, Job>()
    
    suspend fun preloadRecentVideos() {
        val startTime = performanceMonitor.startMeasure("preload_recent")
        
        try {
            val recentVideos = playbackHistory.getRecentVideos(limit = 5)
            
            recentVideos.forEach { video ->
                preloadVideo(video.path)
            }
            
            performanceMonitor.endMeasure("preload_recent", startTime)
        } catch (e: Exception) {
            Log.e("VideoPreloader", "Failed to preload recent videos", e)
            performanceMonitor.endMeasure("preload_recent", startTime)
        }
    }
    
    fun preloadVideo(videoPath: String) {
        if (activePreloads.containsKey(videoPath)) return
        
        val job = preloadScope.launch {
            val startTime = performanceMonitor.startMeasure("preload_video")
            
            try {
                // Check if already cached
                if (videoCache.getCachedVideo(videoPath) != null) {
                    performanceMonitor.endMeasure("preload_video", startTime)
                    return@launch
                }
                
                // Cache the video
                videoCache.cacheVideo(videoPath)
                
                // Preload first few seconds for instant playback
                preloadVideoSegment(videoPath, 0, 5000) // First 5 seconds
                
                performanceMonitor.endMeasure("preload_video", startTime)
            } catch (e: Exception) {
                Log.e("VideoPreloader", "Failed to preload video", e)
                performanceMonitor.endMeasure("preload_video", startTime)
            } finally {
                activePreloads.remove(videoPath)
            }
        }
        
        activePreloads[videoPath] = job
    }
    
    private suspend fun preloadVideoSegment(
        videoPath: String,
        startMs: Long,
        endMs: Long
    ) = withContext(Dispatchers.IO) {
        // This would use ExoPlayer's DownloadService in real implementation
        // For now, we'll just simulate preloading
        delay(100)
    }
    
    fun cancelPreload(videoPath: String) {
        activePreloads[videoPath]?.cancel()
        activePreloads.remove(videoPath)
    }
    
    fun cancelAllPreloads() {
        activePreloads.values.forEach { it.cancel() }
        activePreloads.clear()
    }
}

// 5. Memory Manager
@Singleton
class MemoryManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val performanceMonitor: PerformanceMonitor
) {
    
    private val runtime = Runtime.getRuntime()
    private val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    
    data class MemoryInfo(
        val totalMemory: Long,
        val freeMemory: Long,
        val usedMemory: Long,
        val maxMemory: Long,
        val lowMemoryThreshold: Long
    ) {
        val usagePercent: Float get() = (usedMemory.toFloat() / totalMemory) * 100
        val isLowMemory: Boolean get() = freeMemory < lowMemoryThreshold
    }
    
    fun getMemoryInfo(): MemoryInfo {
        val totalMemory = runtime.totalMemory()
        val freeMemory = runtime.freeMemory()
        val maxMemory = runtime.maxMemory()
        
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)
        
        return MemoryInfo(
            totalMemory = totalMemory,
            freeMemory = freeMemory,
            usedMemory = totalMemory - freeMemory,
            maxMemory = maxMemory,
            lowMemoryThreshold = memoryInfo.threshold
        )
    }
    
    fun registerLowMemoryCallback(callback: () -> Unit) {
        context.registerComponentCallbacks(object : ComponentCallbacks2 {
            override fun onConfigurationChanged(newConfig: Configuration) {}
            
            override fun onLowMemory() {
                Log.w("MemoryManager", "Low memory warning")
                callback()
            }
            
            override fun onTrimMemory(level: Int) {
                when (level) {
                    ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW -> {
                        Log.w("MemoryManager", "Memory running low")
                        callback()
                    }
                    ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL -> {
                        Log.e("MemoryManager", "Memory critically low")
                        callback()
                        forceGarbageCollection()
                    }
                }
            }
        })
    }
    
    fun optimizeMemory() {
        val startTime = performanceMonitor.startMeasure("memory_optimize")
        
        // Clear image caches
        Glide.get(context).clearMemory()
        
        // Run garbage collection
        System.gc()
        
        performanceMonitor.endMeasure("memory_optimize", startTime)
        
        Log.i("MemoryManager", "Memory optimized: ${getMemoryInfo()}")
    }
    
    private fun forceGarbageCollection() {
        System.gc()
        System.runFinalization()
        System.gc()
    }
}

// 6. Battery Optimizer
@Singleton
class BatteryOptimizer @Inject constructor(
    @ApplicationContext private val context: Context,
    private val performanceMonitor: PerformanceMonitor
) {
    
    private val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    private val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
    
    enum class PowerMode {
        PERFORMANCE,   // Full quality, all features
        BALANCED,      // Adaptive quality
        POWER_SAVER    // Reduced quality, minimal features
    }
    
    private var currentPowerMode = PowerMode.BALANCED
    private val powerModeCallbacks = mutableListOf<(PowerMode) -> Unit>()
    
    init {
        registerBatteryReceiver()
    }
    
    private fun registerBatteryReceiver() {
        val batteryReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                updatePowerMode()
            }
        }
        
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_BATTERY_CHANGED)
            addAction(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED)
        }
        
        context.registerReceiver(batteryReceiver, filter)
    }
    
    private fun updatePowerMode() {
        val batteryLevel = getBatteryLevel()
        val isPowerSaveMode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            powerManager.isPowerSaveMode
        } else {
            false
        }
        
        currentPowerMode = when {
            isPowerSaveMode -> PowerMode.POWER_SAVER
            batteryLevel < 20 -> PowerMode.POWER_SAVER
            batteryLevel < 50 -> PowerMode.BALANCED
            else -> PowerMode.PERFORMANCE
        }
        
        powerModeCallbacks.forEach { it(currentPowerMode) }
    }
    
    fun getBatteryLevel(): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        } else {
            val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
            if (level != -1 && scale != -1) {
                (level * 100 / scale.toFloat()).toInt()
            } else {
                -1
            }
        }
    }
    
    fun registerPowerModeCallback(callback: (PowerMode) -> Unit) {
        powerModeCallbacks.add(callback)
        callback(currentPowerMode) // Notify current state
    }
    
    fun getOptimalVideoQuality(): VideoQuality {
        return when (currentPowerMode) {
            PowerMode.PERFORMANCE -> VideoQuality.ORIGINAL
            PowerMode.BALANCED -> VideoQuality.HD_720P
            PowerMode.POWER_SAVER -> VideoQuality.SD_480P
        }
    }
    
    fun shouldReduceFrameRate(): Boolean {
        return currentPowerMode == PowerMode.POWER_SAVER
    }
    
    fun shouldDisableEffects(): Boolean {
        return currentPowerMode == PowerMode.POWER_SAVER
    }
    
    enum class VideoQuality {
        ORIGINAL,
        UHD_4K,
        FHD_1080P,
        HD_720P,
        SD_480P
    }
}

// 7. Playback History (for preloading)
@Singleton
class PlaybackHistory @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    private val prefs = context.getSharedPreferences("playback_history", Context.MODE_PRIVATE)
    private val maxHistorySize = 50
    
    data class PlaybackEntry(
        val path: String,
        val title: String,
        val lastPosition: Long,
        val duration: Long,
        val lastPlayed: Long = System.currentTimeMillis(),
        val playCount: Int = 1
    )
    
    fun addToHistory(entry: PlaybackEntry) {
        val history = getHistory().toMutableList()
        
        // Remove existing entry if present
        history.removeAll { it.path == entry.path }
        
        // Add to beginning
        history.add(0, entry)
        
        // Limit size
        if (history.size > maxHistorySize) {
            history.removeAt(history.size - 1)
        }
        
        saveHistory(history)
    }
    
    fun getRecentVideos(limit: Int = 10): List<PlaybackEntry> {
        return getHistory().take(limit)
    }
    
    fun getFrequentlyPlayed(limit: Int = 10): List<PlaybackEntry> {
        return getHistory()
            .sortedByDescending { it.playCount }
            .take(limit)
    }
    
    fun updateProgress(path: String, position: Long) {
        val history = getHistory().toMutableList()
        val index = history.indexOfFirst { it.path == path }
        
        if (index != -1) {
            history[index] = history[index].copy(
                lastPosition = position,
                lastPlayed = System.currentTimeMillis()
            )
            saveHistory(history)
        }
    }
    
    private fun getHistory(): List<PlaybackEntry> {
        val json = prefs.getString("history", null) ?: return emptyList()
        return try {
            Json.decodeFromString(json)
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    private fun saveHistory(history: List<PlaybackEntry>) {
        val json = Json.encodeToString(history)
        prefs.edit().putString("history", json).apply()
    }
}

// 8. Performance Settings UI
@Composable
fun PerformanceSettingsScreen(
    viewModel: PerformanceViewModel = hiltViewModel()
) {
    val performanceState by viewModel.performanceState.collectAsState()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = "Performance",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 24.dp)
        )
        
        // Memory Usage Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Memory Usage",
                    style = MaterialTheme.typography.titleMedium
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                LinearProgressIndicator(
                    progress = performanceState.memoryUsagePercent / 100f,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${performanceState.usedMemoryMB}MB / ${performanceState.totalMemoryMB}MB",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "${performanceState.memoryUsagePercent.toInt()}%",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                
                if (performanceState.isLowMemory) {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                    ) {
                        Text(
                            text = "Low memory - performance may be affected",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }
                
                OutlinedButton(
                    onClick = { viewModel.clearMemory() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                ) {
                    Icon(Icons.Default.CleaningServices, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Clear Memory")
                }
            }
        }
        
        // Battery Optimization Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Battery Optimization",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Battery: ${performanceState.batteryLevel}%",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    Chip(
                        onClick = { },
                        colors = ChipDefaults.chipColors(
                            containerColor = when (performanceState.powerMode) {
                                BatteryOptimizer.PowerMode.PERFORMANCE -> MaterialTheme.colorScheme.primary
                                BatteryOptimizer.PowerMode.BALANCED -> MaterialTheme.colorScheme.secondary
                                BatteryOptimizer.PowerMode.POWER_SAVER -> MaterialTheme.colorScheme.tertiary
                            }
                        )
                    ) {
                        Icon(
                            imageVector = when (performanceState.powerMode) {
                                BatteryOptimizer.PowerMode.PERFORMANCE -> Icons.Default.Speed
                                BatteryOptimizer.PowerMode.BALANCED -> Icons.Default.Balance
                                BatteryOptimizer.PowerMode.POWER_SAVER -> Icons.Default.BatteryAlert
                            },
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(performanceState.powerMode.name)
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Power mode selection
                Text(
                    text = "Power Mode",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    BatteryOptimizer.PowerMode.values().forEach { mode ->
                        FilterChip(
                            selected = performanceState.powerMode == mode,
                            onClick = { viewModel.setPowerMode(mode) },
                            label = { Text(mode.name) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
        
        // Cache Management Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Cache Management",
                    style = MaterialTheme.typography.titleMedium
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Video Cache",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = formatBytes(performanceState.cacheSize),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Cached Videos",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${performanceState.cachedVideoCount} videos",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalArrangement(Arrangement.spacedBy(8.dp))
                ) {
                    OutlinedButton(
                        onClick = { viewModel.clearCache() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Clear")
                    }
                    
                    OutlinedButton(
                        onClick = { viewModel.optimizeCache() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Speed, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Optimize")
                    }
                }
            }
        }
        
        // Performance Stats Card
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Performance Stats",
                    style = MaterialTheme.typography.titleMedium
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // App Startup Time
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("App Startup", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "${performanceState.appStartupTime}ms",
                        style = MaterialTheme.typography.bodyMedium,
                        color = when {
                            performanceState.appStartupTime < 1000 -> MaterialTheme.colorScheme.primary
                            performanceState.appStartupTime < 2000 -> MaterialTheme.colorScheme.secondary
                            else -> MaterialTheme.colorScheme.error
                        }
                    )
                }
                
                // Video Load Time
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Avg Video Load", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "${performanceState.avgVideoLoadTime}ms",
                        style = MaterialTheme.typography.bodyMedium,
                        color = when {
                            performanceState.avgVideoLoadTime < 500 -> MaterialTheme.colorScheme.primary
                            performanceState.avgVideoLoadTime < 1000 -> MaterialTheme.colorScheme.secondary
                            else -> MaterialTheme.colorScheme.error
                        }
                    )
                }
                
                // Frame Rate
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("UI Frame Rate", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "${performanceState.frameRate} FPS",
                        style = MaterialTheme.typography.bodyMedium,
                        color = when {
                            performanceState.frameRate >= 60 -> MaterialTheme.colorScheme.primary
                            performanceState.frameRate >= 30 -> MaterialTheme.colorScheme.secondary
                            else -> MaterialTheme.colorScheme.error
                        }
                    )
                }
            }
        }
    }
}

// 9. Performance ViewModel
@HiltViewModel
class PerformanceViewModel @Inject constructor(
    private val performanceMonitor: PerformanceMonitor,
    private val memoryManager: MemoryManager,
    private val batteryOptimizer: BatteryOptimizer,
    private val videoCache: VideoCache,
    private val startupOptimizer: StartupOptimizer
) : ViewModel() {
    
    private val _performanceState = MutableStateFlow(PerformanceState())
    val performanceState: StateFlow<PerformanceState> = _performanceState.asStateFlow()
    
    init {
        updatePerformanceStats()
        registerBatteryCallback()
        startPerformanceMonitoring()
    }
    
    private fun updatePerformanceStats() {
        viewModelScope.launch {
            val memoryInfo = memoryManager.getMemoryInfo()
            val batteryLevel = batteryOptimizer.getBatteryLevel()
            
            _performanceState.value = _performanceState.value.copy(
                usedMemoryMB = memoryInfo.usedMemory / (1024 * 1024),
                totalMemoryMB = memoryInfo.totalMemory / (1024 * 1024),
                memoryUsagePercent = memoryInfo.usagePercent,
                isLowMemory = memoryInfo.isLowMemory,
                batteryLevel = batteryLevel,
                cacheSize = getCacheSize(),
                cachedVideoCount = getCachedVideoCount()
            )
        }
    }
    
    private fun registerBatteryCallback() {
        batteryOptimizer.registerPowerModeCallback { mode ->
            _performanceState.value = _performanceState.value.copy(powerMode = mode)
        }
    }
    
    private fun startPerformanceMonitoring() {
        viewModelScope.launch {
            while (isActive) {
                updatePerformanceStats()
                delay(5000) // Update every 5 seconds
            }
        }
    }
    
    fun clearMemory() {
        viewModelScope.launch {
            memoryManager.optimizeMemory()
            updatePerformanceStats()
        }
    }
    
    fun clearCache() {
        viewModelScope.launch {
            videoCache.cleanupOldCache()
            updatePerformanceStats()
        }
    }
    
    fun optimizeCache() {
        viewModelScope.launch {
            // Implement cache optimization
            updatePerformanceStats()
        }
    }
    
    fun setPowerMode(mode: BatteryOptimizer.PowerMode) {
        // In real implementation, this would update system settings
        _performanceState.value = _performanceState.value.copy(powerMode = mode)
    }
    
    private suspend fun getCacheSize(): Long = withContext(Dispatchers.IO) {
        val cacheDir = File(viewModelScope.coroutineContext.toString(), "video_cache")
        if (cacheDir.exists()) {
            cacheDir.walkTopDown().sumOf { it.length() }
        } else {
            0L
        }
    }
    
    private suspend fun getCachedVideoCount(): Int = withContext(Dispatchers.IO) {
        // Implementation would count cached videos
        0
    }
    
    data class PerformanceState(
        val usedMemoryMB: Long = 0,
        val totalMemoryMB: Long = 0,
        val memoryUsagePercent: Float = 0f,
        val isLowMemory: Boolean = false,
        val batteryLevel: Int = 100,
        val powerMode: BatteryOptimizer.PowerMode = BatteryOptimizer.PowerMode.BALANCED,
        val cacheSize: Long = 0,
        val cachedVideoCount: Int = 0,
        val appStartupTime: Long = 0,
        val avgVideoLoadTime: Long = 0,
        val frameRate: Int = 60
    )
}

// 10. Fast Video Loading Implementation
@Singleton
class FastVideoLoader @Inject constructor(
    private val context: Context,
    private val videoCache: VideoCache,
    private val preloader: VideoPreloader,
    private val performanceMonitor: PerformanceMonitor
) {
    
    suspend fun loadVideo(uri: Uri): LoadResult {
        val startTime = performanceMonitor.startMeasure("video_load")
        
        return try {
            // Check cache first
            val cachedVideo = videoCache.getCachedVideo(uri.toString())
            if (cachedVideo != null) {
                performanceMonitor.endMeasure("video_load", startTime)
                return LoadResult.Success(
                    uri = uri,
                    metadata = cachedVideo.metadata,
                    loadTime = System.currentTimeMillis() - startTime
                )
            }
            
            // Load and cache
            val metadata = extractMetadataFast(uri)
            
            // Start caching in background
            GlobalScope.launch(Dispatchers.IO) {
                videoCache.cacheVideo(uri.toString())
            }
            
            // Preload next videos
            preloadAdjacentVideos(uri)
            
            performanceMonitor.endMeasure("video_load", startTime)
            
            LoadResult.Success(
                uri = uri,
                metadata = metadata,
                loadTime = System.currentTimeMillis() - startTime
            )
        } catch (e: Exception) {
            performanceMonitor.endMeasure("video_load", startTime)
            LoadResult.Error(e)
        }
    }
    
    private suspend fun extractMetadataFast(uri: Uri): VideoCache.VideoMetadata = withContext(Dispatchers.IO) {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, uri)
            
            // Extract only essential metadata for fast loading
            VideoCache.VideoMetadata(
                duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLong() ?: 0,
                width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toInt() ?: 0,
                height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toInt() ?: 0,
                frameRate = 30f, // Default, extract later if needed
                bitrate = 0 // Extract later if needed
            )
        } finally {
            retriever.release()
        }
    }
    
    private fun preloadAdjacentVideos(currentUri: Uri) {
        // In real implementation, this would preload next/previous videos in playlist
        // For now, just a placeholder
    }
    
    sealed class LoadResult {
        data class Success(
            val uri: Uri,
            val metadata: VideoCache.VideoMetadata,
            val loadTime: Long
        ) : LoadResult()
        
        data class Error(val exception: Exception) : LoadResult()
    }
}

// 11. Application Class Integration
class OptimizedApplication : Application() {
    
    @Inject lateinit var startupOptimizer: StartupOptimizer
    @Inject lateinit var performanceMonitor: PerformanceMonitor
    @Inject lateinit var memoryManager: MemoryManager
    
    override fun onCreate() {
        super.onCreate()
        
        // Optimize startup
        runBlocking {
            startupOptimizer.optimize()
        }
        
        // Register memory callbacks
        memoryManager.registerLowMemoryCallback {
            // Handle low memory
            memoryManager.optimizeMemory()
        }
        
        // Enable strict mode in debug
        if (BuildConfig.DEBUG) {
            StrictMode.setThreadPolicy(
                StrictMode.ThreadPolicy.Builder()
                    .detectAll()
                    .penaltyLog()
                    .build()
            )
            StrictMode.setVmPolicy(
                StrictMode.VmPolicy.Builder()
                    .detectAll()
                    .penaltyLog()
                    .build()
            )
        }
    }
}

// 12. Hilt Module
@Module
@InstallIn(SingletonComponent::class)
object PerformanceModule {
    
    @Provides
    @Singleton
    fun providePerformanceMonitor(
        @ApplicationContext context: Context
    ): PerformanceMonitor {
        return PerformanceMonitor(context)
    }
    
    @Provides
    @Singleton
    fun provideStartupOptimizer(
        @ApplicationContext context: Context,
        performanceMonitor: PerformanceMonitor,
        videoCache: VideoCache,
        preloader: VideoPreloader
    ): StartupOptimizer {
        return StartupOptimizer(context, performanceMonitor, videoCache, preloader)
    }
    
    @Provides
    @Singleton
    fun provideVideoCache(
        @ApplicationContext context: Context,
        performanceMonitor: PerformanceMonitor
    ): VideoCache {
        return VideoCache(context, performanceMonitor)
    }
    
    @Provides
    @Singleton
    fun provideVideoPreloader(
        @ApplicationContext context: Context,
        videoCache: VideoCache,
        performanceMonitor: PerformanceMonitor,
        playbackHistory: PlaybackHistory
    ): VideoPreloader {
        return VideoPreloader(context, videoCache, performanceMonitor, playbackHistory)
    }
    
    @Provides
    @Singleton
    fun provideMemoryManager(
        @ApplicationContext context: Context,
        performanceMonitor: PerformanceMonitor
    ): MemoryManager {
        return MemoryManager(context, performanceMonitor)
    }
    
    @Provides
    @Singleton
    fun provideBatteryOptimizer(
        @ApplicationContext context: Context,
        performanceMonitor: PerformanceMonitor
    ): BatteryOptimizer {
        return BatteryOptimizer(context, performanceMonitor)
    }
    
    @Provides
    @Singleton
    fun providePlaybackHistory(
        @ApplicationContext context: Context
    ): PlaybackHistory {
        return PlaybackHistory(context)
    }
    
    @Provides
    @Singleton
    fun provideFastVideoLoader(
        @ApplicationContext context: Context,
        videoCache: VideoCache,
        preloader: VideoPreloader,
        performanceMonitor: PerformanceMonitor
    ): FastVideoLoader {
        return FastVideoLoader(context, videoCache, preloader, performanceMonitor)
    }
}
                