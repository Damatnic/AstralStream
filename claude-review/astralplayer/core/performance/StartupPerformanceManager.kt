package com.astralplayer.core.performance

import android.content.Context
import android.os.SystemClock
import androidx.startup.Initializer
import kotlinx.coroutines.*
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Startup performance optimization manager
 * Implements PerformanceAgent requirements for <1 second startup
 */
@Singleton
class StartupPerformanceManager @Inject constructor(
    private val context: Context
) {
    
    companion object {
        private const val TARGET_STARTUP_TIME_MS = 1000L // 1 second target
        private const val CRITICAL_INIT_TIMEOUT_MS = 500L
    }
    
    private var appStartTime = 0L
    private var coldStartTime = 0L
    private val initTimes = mutableMapOf<String, Long>()
    
    private val performanceScope = CoroutineScope(
        SupervisorJob() + Dispatchers.Default
    )
    
    /**
     * Mark application start time (call in Application.onCreate)
     */
    fun markAppStart() {
        appStartTime = SystemClock.elapsedRealtime()
        coldStartTime = System.currentTimeMillis()
    }
    
    /**
     * Initialize critical components in parallel
     */
    suspend fun initializeCriticalComponents(): Boolean = withContext(Dispatchers.Default) {
        val startTime = SystemClock.elapsedRealtime()
        
        try {
            // Initialize critical components in parallel with timeout
            withTimeout(CRITICAL_INIT_TIMEOUT_MS) {
                coroutineScope {
                    // Group 1: Absolutely critical (needed for first screen)
                    val critical = async {
                        measureInit("Database") { initializeDatabase() }
                    }
                    
                    val theme = async {
                        measureInit("Theme") { initializeTheme() }
                    }
                    
                    // Wait for critical components
                    critical.await()
                    theme.await()
                }
            }
            
            val elapsed = SystemClock.elapsedRealtime() - startTime
            Timber.d("Critical components initialized in ${elapsed}ms")
            
            true
        } catch (e: TimeoutCancellationException) {
            Timber.e("Critical initialization timeout")
            false
        }
    }
    
    /**
     * Initialize non-critical components lazily
     */
    fun initializeNonCriticalComponents() {
        performanceScope.launch {
            // Group 2: Important but not blocking
            launch {
                delay(100) // Small delay to not interfere with UI
                measureInit("Analytics") { initializeAnalytics() }
            }
            
            launch {
                delay(200)
                measureInit("CloudSync") { initializeCloudSync() }
            }
            
            // Group 3: Can be initialized much later
            launch {
                delay(2000)
                measureInit("AIServices") { initializeAIServices() }
            }
            
            launch {
                delay(3000)
                measureInit("CacheCleanup") { performCacheCleanup() }
            }
        }
    }
    
    /**
     * Measure and record initialization time
     */
    private suspend fun <T> measureInit(
        componentName: String, 
        block: suspend () -> T
    ): T {
        val start = SystemClock.elapsedRealtime()
        return try {
            block().also {
                val elapsed = SystemClock.elapsedRealtime() - start
                initTimes[componentName] = elapsed
                Timber.d("$componentName initialized in ${elapsed}ms")
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to initialize $componentName")
            throw e
        }
    }
    
    /**
     * Mark when first frame is rendered
     */
    fun markFirstFrameRendered() {
        val totalStartupTime = SystemClock.elapsedRealtime() - appStartTime
        
        Timber.i("=== STARTUP PERFORMANCE ===")
        Timber.i("Total startup time: ${totalStartupTime}ms")
        Timber.i("Target: ${TARGET_STARTUP_TIME_MS}ms")
        Timber.i("Status: ${if (totalStartupTime <= TARGET_STARTUP_TIME_MS) "✅ PASS" else "❌ FAIL"}")
        
        // Log individual component times
        initTimes.forEach { (component, time) ->
            Timber.i("  $component: ${time}ms")
        }
        
        // Report to analytics
        reportStartupMetrics(totalStartupTime)
    }
    
    /**
     * Get startup performance report
     */
    fun getStartupReport(): StartupReport {
        val totalTime = SystemClock.elapsedRealtime() - appStartTime
        
        return StartupReport(
            coldStartTime = coldStartTime,
            totalStartupTime = totalTime,
            targetMet = totalTime <= TARGET_STARTUP_TIME_MS,
            componentTimes = initTimes.toMap(),
            recommendations = generateRecommendations(totalTime)
        )
    }
    
    private fun generateRecommendations(totalTime: Long): List<String> {
        val recommendations = mutableListOf<String>()
        
        if (totalTime > TARGET_STARTUP_TIME_MS) {
            // Find slowest components
            val slowComponents = initTimes.filter { it.value > 100 }
                .toList()
                .sortedByDescending { it.second }
                .take(3)
            
            slowComponents.forEach { (component, time) ->
                recommendations.add("Optimize $component initialization (${time}ms)")
            }
        }
        
        return recommendations
    }
    
    // Initialization methods (implementations would be in respective classes)
    private suspend fun initializeDatabase() {
        // Database initialization logic
        delay(50) // Simulate
    }
    
    private suspend fun initializeTheme() {
        // Theme initialization logic
        delay(20) // Simulate
    }
    
    private suspend fun initializeAnalytics() {
        // Analytics initialization logic
        delay(100) // Simulate
    }
    
    private suspend fun initializeCloudSync() {
        // Cloud sync initialization logic
        delay(150) // Simulate
    }
    
    private suspend fun initializeAIServices() {
        // AI services initialization logic
        delay(200) // Simulate
    }
    
    private suspend fun performCacheCleanup() {
        // Cache cleanup logic
        delay(100) // Simulate
    }
    
    private fun reportStartupMetrics(totalTime: Long) {
        // Report to analytics service
    }
    
    data class StartupReport(
        val coldStartTime: Long,
        val totalStartupTime: Long,
        val targetMet: Boolean,
        val componentTimes: Map<String, Long>,
        val recommendations: List<String>
    )
}

/**
 * App startup initializer using Jetpack App Startup
 */
class PerformanceInitializer : Initializer<StartupPerformanceManager> {
    override fun create(context: Context): StartupPerformanceManager {
        return StartupPerformanceManager(context).apply {
            markAppStart()
        }
    }
    
    override fun dependencies(): List<Class<out Initializer<*>>> {
        return emptyList() // No dependencies, runs first
    }
}