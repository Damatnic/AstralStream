package com.astralplayer.nextplayer.immersive

import android.content.Context
import android.util.Log
import androidx.media3.common.MediaItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Main engine coordinating immersive media features including VR, AR, and 360° video
 */
@Singleton
class ImmersiveMediaEngine @Inject constructor(
    private val context: Context,
    private val vrRenderer: VRRenderer,
    private val arOverlayManager: AROverlayManager,
    private val sphericalVideoProcessor: SphericalVideoProcessor,
    private val gyroscopeController: GyroscopeController,
    private val immersiveUIManager: ImmersiveUIManager
) {
    
    private var isInitialized = false
    private var callbacks: ImmersiveMediaCallbacks? = null
    private var currentSession: ImmersiveSession? = null
    private val activeSessions = mutableMapOf<String, ImmersiveSession>()
    
    suspend fun initializeImmersiveMedia(callbacks: ImmersiveMediaCallbacks): Boolean = withContext(Dispatchers.IO) {
        try {
            this@ImmersiveMediaEngine.callbacks = callbacks
            
            // Initialize all components
            val vrInitialized = vrRenderer.initialize()
            val arInitialized = arOverlayManager.initialize()
            val sphericalInitialized = sphericalVideoProcessor.initialize()
            val gyroInitialized = gyroscopeController.initialize()
            
            immersiveUIManager.initialize()
            
            isInitialized = vrInitialized && arInitialized && sphericalInitialized && gyroInitialized
            
            if (isInitialized) {
                callbacks.onImmersiveMediaInitialized()
                Log.i("ImmersiveEngine", "Immersive media engine initialized successfully")
            } else {
                Log.e("ImmersiveEngine", "Failed to initialize immersive media components")
            }
            
            isInitialized
        } catch (e: Exception) {
            Log.e("ImmersiveEngine", "Error initializing immersive media", e)
            false
        }
    }
    
    suspend fun startImmersiveSession(
        mediaItem: MediaItem,
        mode: ViewingMode,
        config: ImmersiveConfiguration
    ): ImmersiveSession = withContext(Dispatchers.Main) {
        
        val sessionId = UUID.randomUUID().toString()
        val session = ImmersiveSession(
            id = sessionId,
            mediaItem = mediaItem,
            viewingMode = mode,
            configuration = config,
            startTime = System.currentTimeMillis()
        )
        
        activeSessions[sessionId] = session
        currentSession = session
        
        // Configure components based on viewing mode
        when (mode) {
            ViewingMode.VR_HEADSET -> {
                vrRenderer.enableVRMode()
                gyroscopeController.enableHeadTracking { rotation ->
                    callbacks?.onHeadOrientationChanged(rotation)
                }
                immersiveUIManager.switchToVRMode()
            }
            ViewingMode.VR_CARDBOARD -> {
                vrRenderer.enableCardboardMode()
                gyroscopeController.enableCardboardTracking()
                immersiveUIManager.switchToCardboardMode()
            }
            ViewingMode.SPHERICAL_360 -> {
                sphericalVideoProcessor.enableSphericalProjection(SphericalProjection.EQUIRECTANGULAR)
                gyroscopeController.enableSphericalNavigation { rotation ->
                    sphericalVideoProcessor.updateViewDirection(rotation)
                }
                immersiveUIManager.switchToSphericalMode()
            }
            ViewingMode.AR_OVERLAY -> {
                arOverlayManager.enableARMode()
                immersiveUIManager.switchToARMode()
            }
            ViewingMode.IMMERSIVE_FULLSCREEN -> {
                immersiveUIManager.switchToImmersiveMode()
            }
            ViewingMode.TRADITIONAL -> {
                immersiveUIManager.switchToTraditionalMode()
            }
        }
        
        callbacks?.onImmersiveSessionStarted(session)
        Log.i("ImmersiveEngine", "Started immersive session: $sessionId for mode: $mode")
        
        session
    }
    
    suspend fun switchViewingMode(newMode: ViewingMode) = withContext(Dispatchers.Main) {
        currentSession?.let { session ->
            val oldMode = session.viewingMode
            
            // Disable current mode
            disableCurrentMode(oldMode)
            
            // Enable new mode
            enableMode(newMode, session.configuration)
            
            // Update session
            val updatedSession = session.copy(viewingMode = newMode)
            activeSessions[session.id] = updatedSession
            currentSession = updatedSession
            
            callbacks?.onViewingModeChanged(newMode)
            Log.i("ImmersiveEngine", "Switched from $oldMode to $newMode")
        }
    }
    
    suspend fun updateSphericalRotation(rotation: Quaternion) = withContext(Dispatchers.Main) {
        sphericalVideoProcessor.updateViewDirection(rotation)
    }
    
    suspend fun setFieldOfView(fov: Float) = withContext(Dispatchers.Main) {
        sphericalVideoProcessor.setFieldOfView(fov)
    }
    
    suspend fun addAROverlay(overlay: AROverlay) = withContext(Dispatchers.Main) {
        arOverlayManager.addOverlay(overlay)
        callbacks?.onAROverlayAdded(overlay)
    }
    
    suspend fun updateAROverlay(update: AROverlayUpdate) = withContext(Dispatchers.Main) {
        arOverlayManager.updateOverlay(update.overlayId, update.position, update.rotation, update.scale)
    }
    
    suspend fun removeAROverlay(overlayId: String) = withContext(Dispatchers.Main) {
        arOverlayManager.removeOverlay(overlayId)
    }
    
    suspend fun captureImmersiveScreenshot(): ImmersiveScreenshot = withContext(Dispatchers.IO) {
        val currentMode = currentSession?.viewingMode ?: ViewingMode.TRADITIONAL
        val timestamp = System.currentTimeMillis()
        
        val imageData = when (currentMode) {
            ViewingMode.VR_HEADSET, ViewingMode.VR_CARDBOARD -> vrRenderer.captureVRFrame()
            ViewingMode.SPHERICAL_360 -> sphericalVideoProcessor.captureCurrentView()
            ViewingMode.AR_OVERLAY -> arOverlayManager.captureARView()
            else -> ByteArray(0) // Fallback
        }
        
        ImmersiveScreenshot(
            viewingMode = currentMode,
            timestamp = timestamp,
            imageData = imageData
        )
    }
    
    suspend fun performVRCalibration(): VRCalibrationData = withContext(Dispatchers.IO) {
        val calibrationData = vrRenderer.performCalibration()
        callbacks?.onVRCalibrationComplete(calibrationData)
        calibrationData
    }
    
    suspend fun endImmersiveSession(): ImmersiveSessionResult = withContext(Dispatchers.Main) {
        currentSession?.let { session ->
            val endTime = System.currentTimeMillis()
            val duration = endTime - session.startTime
            
            // Cleanup current mode
            disableCurrentMode(session.viewingMode)
            
            // Create session stats
            val stats = ImmersiveSessionStats(
                duration = duration,
                viewingMode = session.viewingMode,
                headMovements = 0, // Would track actual movements
                modeChanges = 0,   // Would track mode switches
                overlaysUsed = 0   // Would track AR overlays used
            )
            
            val result = ImmersiveSessionResult.Success(stats)
            
            // Cleanup
            activeSessions.remove(session.id)
            currentSession = null
            
            callbacks?.onImmersiveSessionEnded(result)
            Log.i("ImmersiveEngine", "Ended immersive session: ${session.id}")
            
            result
        } ?: run {
            val result = ImmersiveSessionResult.Error("No active session to end")
            callbacks?.onImmersiveSessionEnded(result)
            result
        }
    }
    
    fun getImmersiveCapabilities(): ImmersiveCapabilities {
        return ImmersiveCapabilities(
            supportsVR = vrRenderer.isVRSupported(),
            supportsAR = arOverlayManager.isARSupported(),
            supportsSpherical = sphericalVideoProcessor.isSphericalSupported(),
            supportsGyroscope = gyroscopeController.isGyroscopeAvailable(),
            supportedProjections = listOf(
                SphericalProjection.EQUIRECTANGULAR,
                SphericalProjection.CUBIC,
                SphericalProjection.CYLINDRICAL
            )
        )
    }
    
    private suspend fun enableMode(mode: ViewingMode, config: ImmersiveConfiguration) {
        when (mode) {
            ViewingMode.VR_HEADSET -> {
                vrRenderer.enableVRMode()
                gyroscopeController.enableHeadTracking { rotation ->
                    callbacks?.onHeadOrientationChanged(rotation)
                }
                immersiveUIManager.switchToVRMode()
            }
            ViewingMode.VR_CARDBOARD -> {
                vrRenderer.enableCardboardMode()
                gyroscopeController.enableCardboardTracking()
                immersiveUIManager.switchToCardboardMode()
            }
            ViewingMode.SPHERICAL_360 -> {
                sphericalVideoProcessor.enableSphericalProjection(SphericalProjection.EQUIRECTANGULAR)
                gyroscopeController.enableSphericalNavigation { rotation ->
                    sphericalVideoProcessor.updateViewDirection(rotation)
                }
                immersiveUIManager.switchToSphericalMode()
            }
            ViewingMode.AR_OVERLAY -> {
                arOverlayManager.enableARMode()
                immersiveUIManager.switchToARMode()
            }
            ViewingMode.IMMERSIVE_FULLSCREEN -> {
                immersiveUIManager.switchToImmersiveMode()
            }
            ViewingMode.TRADITIONAL -> {
                immersiveUIManager.switchToTraditionalMode()
            }
        }
    }
    
    private suspend fun disableCurrentMode(mode: ViewingMode) {
        when (mode) {
            ViewingMode.VR_HEADSET -> {
                vrRenderer.disableVRMode()
                gyroscopeController.disableHeadTracking()
            }
            ViewingMode.VR_CARDBOARD -> {
                vrRenderer.disableCardboardMode()
                gyroscopeController.disableHeadTracking()
            }
            ViewingMode.SPHERICAL_360 -> {
                sphericalVideoProcessor.disableProjection()
                gyroscopeController.disableSphericalNavigation()
            }
            ViewingMode.AR_OVERLAY -> {
                arOverlayManager.disableARMode()
            }
            ViewingMode.IMMERSIVE_FULLSCREEN -> {
                // No specific cleanup needed
            }
            ViewingMode.TRADITIONAL -> {
                // No specific cleanup needed
            }
        }
    }
    
    fun cleanup() {
        try {
            currentSession?.let {
                // End current session if active
                endImmersiveSession()
            }
            
            vrRenderer.cleanup()
            arOverlayManager.cleanup()
            sphericalVideoProcessor.cleanup()
            gyroscopeController.cleanup()
            
            callbacks = null
            isInitialized = false
            
            Log.i("ImmersiveEngine", "Immersive media engine cleaned up")
        } catch (e: Exception) {
            Log.e("ImmersiveEngine", "Error during cleanup", e)
        }
    }
}