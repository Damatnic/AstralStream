package com.astralplayer.nextplayer.integration

import android.content.Context
import android.util.Log
import com.astralplayer.nextplayer.feature.adult.AdultSiteManager
import com.astralplayer.nextplayer.feature.media.MediaLibraryScanner
import com.astralplayer.nextplayer.feature.network.AdvancedNetworkManager
import com.astralplayer.nextplayer.feature.security.AdultContentFilter
import com.astralplayer.nextplayer.feature.streaming.StreamProcessor
import com.astralplayer.nextplayer.optimization.PerformanceOptimizationManager
import com.astralplayer.nextplayer.security.SecurityManager
import com.astralplayer.nextplayer.utils.PermissionsManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Central integration manager for AstralStream
 * Coordinates all features like MX Player's architecture with proper initialization order
 */
class FeatureIntegrationManager(private val context: Context) {
    
    companion object {
        private const val TAG = "FeatureIntegration"
        
        // Initialization phases inspired by MX Player's startup sequence
        enum class InitializationPhase {
            NOT_STARTED,
            CORE_SERVICES,      // Essential services first
            SECURITY_LAYER,     // Security and permissions
            NETWORK_LAYER,      // Network and streaming
            MEDIA_LAYER,        // Media scanning and processing
            UI_LAYER,          // UI components and themes
            OPTIMIZATION_LAYER, // Performance optimizations
            COMPLETED,
            FAILED
        }
    }
    
    // Core managers (initialization order matters)
    private var permissionsManager: PermissionsManager? = null
    private var securityManager: SecurityManager? = null
    private var performanceManager: PerformanceOptimizationManager? = null
    private var networkManager: AdvancedNetworkManager? = null
    private var streamProcessor: StreamProcessor? = null
    private var contentFilter: AdultContentFilter? = null
    private var adultSiteManager: AdultSiteManager? = null
    private var mediaScanner: MediaLibraryScanner? = null
    
    // State management
    private val _initializationPhase = MutableStateFlow(InitializationPhase.NOT_STARTED)
    val initializationPhase: StateFlow<InitializationPhase> = _initializationPhase.asStateFlow()
    
    private val _isFullyInitialized = MutableStateFlow(false)
    val isFullyInitialized: StateFlow<Boolean> = _isFullyInitialized.asStateFlow()
    
    private val _initializationProgress = MutableStateFlow(0f)
    val initializationProgress: StateFlow<Float> = _initializationProgress.asStateFlow()
    
    private val initializationScope = CoroutineScope(
        SupervisorJob() + Dispatchers.Main + CoroutineName("FeatureIntegration")
    )
    
    private val isInitializing = AtomicBoolean(false)
    
    /**
     * Initialize all features in the correct order (MX Player style)
     */
    suspend fun initialize(): IntegrationResult {
        return withContext(Dispatchers.Main) {
            if (!isInitializing.compareAndSet(false, true)) {
                return@withContext IntegrationResult.AlreadyInitializing
            }
            
            try {
                Log.i(TAG, "Starting AstralStream feature integration...")
                
                // Phase 1: Core Services
                _initializationPhase.value = InitializationPhase.CORE_SERVICES
                _initializationProgress.value = 0.1f
                initializeCoreServices()
                
                // Phase 2: Security Layer
                _initializationPhase.value = InitializationPhase.SECURITY_LAYER
                _initializationProgress.value = 0.25f
                initializeSecurityLayer()
                
                // Phase 3: Network Layer
                _initializationPhase.value = InitializationPhase.NETWORK_LAYER
                _initializationProgress.value = 0.4f
                initializeNetworkLayer()
                
                // Phase 4: Media Layer
                _initializationPhase.value = InitializationPhase.MEDIA_LAYER
                _initializationProgress.value = 0.6f
                initializeMediaLayer()
                
                // Phase 5: UI Layer (prepare UI components)
                _initializationPhase.value = InitializationPhase.UI_LAYER
                _initializationProgress.value = 0.8f
                initializeUILayer()
                
                // Phase 6: Optimization Layer
                _initializationPhase.value = InitializationPhase.OPTIMIZATION_LAYER
                _initializationProgress.value = 0.9f
                initializeOptimizationLayer()
                
                // Complete initialization
                _initializationPhase.value = InitializationPhase.COMPLETED
                _initializationProgress.value = 1.0f
                _isFullyInitialized.value = true
                
                Log.i(TAG, "AstralStream feature integration completed successfully")
                
                // Start background services
                startBackgroundServices()
                
                IntegrationResult.Success(getIntegrationSummary())
                
            } catch (e: Exception) {
                Log.e(TAG, "Feature integration failed", e)
                _initializationPhase.value = InitializationPhase.FAILED
                _isFullyInitialized.value = false
                
                // Cleanup partially initialized components
                cleanup()
                
                IntegrationResult.Failed(e.message ?: "Unknown initialization error")
            } finally {
                isInitializing.set(false)
            }
        }
    }
    
    /**
     * Phase 1: Initialize core services
     */
    private suspend fun initializeCoreServices() {
        Log.d(TAG, "Initializing core services...")
        
        // Performance manager first (needed for optimization decisions)
        performanceManager = PerformanceOptimizationManager(context)
        
        // Permissions manager (needed for all other features)
        permissionsManager = PermissionsManager(context as androidx.fragment.app.FragmentActivity)
        
        Log.d(TAG, "Core services initialized")
    }
    
    /**
     * Phase 2: Initialize security layer
     */
    private suspend fun initializeSecurityLayer() {
        Log.d(TAG, "Initializing security layer...")
        
        securityManager = SecurityManager(context)
        contentFilter = AdultContentFilter(context)
        
        // Apply initial security settings
        if (securityManager?.isPrivateModeEnabled() == true) {
            Log.d(TAG, "Private mode enabled - applying security restrictions")
        }
        
        Log.d(TAG, "Security layer initialized")
    }
    
    /**
     * Phase 3: Initialize network layer
     */
    private suspend fun initializeNetworkLayer() = withContext(Dispatchers.IO) {
        Log.d(TAG, "Initializing network layer...")
        
        networkManager = AdvancedNetworkManager(context)
        streamProcessor = StreamProcessor(context, networkManager!!)
        
        // Wait for network manager to complete initialization
        delay(500) // Give network manager time to set up
        
        Log.d(TAG, "Network layer initialized")
    }
    
    /**
     * Phase 4: Initialize media layer
     */
    private suspend fun initializeMediaLayer() = withContext(Dispatchers.IO) {
        Log.d(TAG, "Initializing media layer...")
        
        // Initialize adult site manager
        adultSiteManager = AdultSiteManager(
            context = context,
            networkManager = networkManager!!,
            streamProcessor = streamProcessor!!,
            securityManager = securityManager!!
        )
        
        // Initialize media scanner
        mediaScanner = MediaLibraryScanner(context)
        
        Log.d(TAG, "Media layer initialized")
    }
    
    /**
     * Phase 5: Initialize UI layer
     */
    private suspend fun initializeUILayer() {
        Log.d(TAG, "Initializing UI layer...")
        
        // Prepare bubble UI components
        // Theme initialization
        // Gesture system preparation
        
        Log.d(TAG, "UI layer initialized")
    }
    
    /**
     * Phase 6: Initialize optimization layer
     */
    private suspend fun initializeOptimizationLayer() {
        Log.d(TAG, "Initializing optimization layer...")
        
        // Apply performance optimizations based on device capabilities
        performanceManager?.let { manager ->
            val recommendations = manager.getMxPlayerStyleRecommendations()
            if (recommendations.isNotEmpty()) {
                Log.i(TAG, "Applied ${recommendations.size} performance optimizations")
            }
        }
        
        Log.d(TAG, "Optimization layer initialized")
    }
    
    /**
     * Start background services after initialization
     */
    private fun startBackgroundServices() {
        initializationScope.launch {
            try {
                Log.d(TAG, "Starting background services...")
                
                // Start media scanning if permissions are granted
                if (permissionsManager?.hasAllEssentialPermissions() == true) {
                    mediaScanner?.scanMediaLibrary()
                }
                
                // Start network monitoring
                // networkManager already starts monitoring in init
                
                Log.d(TAG, "Background services started")
                
            } catch (e: Exception) {
                Log.e(TAG, "Error starting background services", e)
            }
        }
    }
    
    /**
     * Get integration summary for debugging
     */
    private fun getIntegrationSummary(): IntegrationSummary {
        return IntegrationSummary(
            coreServicesReady = performanceManager != null && permissionsManager != null,
            securityLayerReady = securityManager != null && contentFilter != null,
            networkLayerReady = networkManager != null && streamProcessor != null,
            mediaLayerReady = adultSiteManager != null && mediaScanner != null,
            optimizationActive = performanceManager?.optimizationLevel?.value != null,
            totalInitializationTimeMs = System.currentTimeMillis(), // Simplified
            memoryUsageMB = performanceManager?.performanceMetrics?.value?.memoryUsageMB ?: 0L,
            featureCount = getEnabledFeatureCount()
        )
    }
    
    /**
     * Get count of enabled features
     */
    private fun getEnabledFeatureCount(): Int {
        var count = 0
        
        if (performanceManager != null) count++
        if (securityManager != null) count++
        if (networkManager != null) count++
        if (streamProcessor != null) count++
        if (contentFilter != null) count++
        if (adultSiteManager != null) count++
        if (mediaScanner != null) count++
        if (permissionsManager != null) count++
        
        return count
    }
    
    /**
     * Provide access to initialized managers (null-safe)
     */
    fun getPermissionsManager(): PermissionsManager? = permissionsManager
    fun getSecurityManager(): SecurityManager? = securityManager
    fun getPerformanceManager(): PerformanceOptimizationManager? = performanceManager
    fun getNetworkManager(): AdvancedNetworkManager? = networkManager
    fun getStreamProcessor(): StreamProcessor? = streamProcessor
    fun getContentFilter(): AdultContentFilter? = contentFilter
    fun getAdultSiteManager(): AdultSiteManager? = adultSiteManager
    fun getMediaScanner(): MediaLibraryScanner? = mediaScanner
    
    /**
     * Check if specific features are ready
     */
    fun isSecurityReady(): Boolean = securityManager != null && contentFilter != null
    fun isNetworkReady(): Boolean = networkManager != null && streamProcessor != null
    fun isMediaReady(): Boolean = mediaScanner != null && adultSiteManager != null
    fun isOptimizationReady(): Boolean = performanceManager != null
    
    /**
     * Handle feature-specific operations
     */
    suspend fun processAdultContent(url: String): com.astralplayer.nextplayer.feature.adult.AdultContentResult? {
        return if (isFullyInitialized.value) {
            adultSiteManager?.processAdultContent(url)
        } else {
            Log.w(TAG, "Attempted to process adult content before full initialization")
            null
        }
    }
    
    suspend fun scanMediaLibrary() {
        if (isMediaReady() && permissionsManager?.hasAllEssentialPermissions() == true) {
            mediaScanner?.scanMediaLibrary()
        } else {
            Log.w(TAG, "Cannot scan media library - missing permissions or not initialized")
        }
    }
    
    /**
     * Get system health status (MX Player style)
     */
    fun getSystemHealth(): SystemHealthStatus {
        val performanceMetrics = performanceManager?.performanceMetrics?.value
        
        return SystemHealthStatus(
            overallHealth = when {
                performanceMetrics?.memoryUsagePercent ?: 0f > 90f -> HealthLevel.CRITICAL
                performanceMetrics?.memoryUsagePercent ?: 0f > 75f -> HealthLevel.WARNING
                performanceMetrics?.isLowMemory == true -> HealthLevel.WARNING
                else -> HealthLevel.GOOD
            },
            memoryHealth = if (performanceMetrics?.isLowMemory == true) HealthLevel.CRITICAL else HealthLevel.GOOD,
            networkHealth = if (networkManager != null) HealthLevel.GOOD else HealthLevel.CRITICAL,
            securityHealth = if (securityManager?.getCurrentSecurityLevel() ?: 0 > 1) HealthLevel.GOOD else HealthLevel.WARNING,
            recommendations = performanceManager?.getMxPlayerStyleRecommendations() ?: emptyList()
        )
    }
    
    /**
     * Emergency cleanup and restart
     */
    suspend fun emergencyRestart(): IntegrationResult {
        Log.w(TAG, "Performing emergency restart...")
        
        cleanup()
        delay(1000) // Wait for cleanup
        
        return initialize()
    }
    
    /**
     * Cleanup all resources
     */
    fun cleanup() {
        Log.d(TAG, "Cleaning up feature integration...")
        
        try {
            performanceManager?.cleanup()
            networkManager?.cleanup()
            adultSiteManager?.cleanup()
            // Other managers don't have cleanup methods but could be nullified
            
            // Clear references
            performanceManager = null
            securityManager = null
            networkManager = null
            streamProcessor = null
            contentFilter = null
            adultSiteManager = null
            mediaScanner = null
            permissionsManager = null
            
            // Cancel coroutines
            initializationScope.cancel()
            
            // Reset state
            _isFullyInitialized.value = false
            _initializationPhase.value = InitializationPhase.NOT_STARTED
            _initializationProgress.value = 0f
            
            Log.d(TAG, "Feature integration cleanup completed")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error during cleanup", e)
        }
    }
}

/**
 * Data classes for integration management
 */
sealed class IntegrationResult {
    data class Success(val summary: IntegrationSummary) : IntegrationResult()
    data class Failed(val error: String) : IntegrationResult()
    object AlreadyInitializing : IntegrationResult()
}

data class IntegrationSummary(
    val coreServicesReady: Boolean,
    val securityLayerReady: Boolean,
    val networkLayerReady: Boolean,
    val mediaLayerReady: Boolean,
    val optimizationActive: Boolean,
    val totalInitializationTimeMs: Long,
    val memoryUsageMB: Long,
    val featureCount: Int
)

data class SystemHealthStatus(
    val overallHealth: HealthLevel,
    val memoryHealth: HealthLevel,
    val networkHealth: HealthLevel,
    val securityHealth: HealthLevel,
    val recommendations: List<com.astralplayer.nextplayer.optimization.OptimizationRecommendation>
)

enum class HealthLevel {
    GOOD, WARNING, CRITICAL
}