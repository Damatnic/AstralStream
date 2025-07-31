// ================================
// AR/VR Integration Engine
// 360° video support, VR headset compatibility, AR overlays, immersive viewing
// ================================

// 1. Immersive Media Engine
@Singleton
class ImmersiveMediaEngine @Inject constructor(
    private val context: Context,
    private val vrRenderer: VRRenderer,
    private val arOverlayManager: AROverlayManager,
    private val sphericalVideoProcessor: SphericalVideoProcessor,
    private val gyroscopeController: GyroscopeController,
    private val immersiveUIManager: ImmersiveUIManager
) {
    
    private var currentImmersiveSession: ImmersiveSession? = null
    private var immersiveCallbacks: ImmersiveMediaCallbacks? = null
    private var currentViewingMode = ViewingMode.TRADITIONAL
    
    suspend fun initializeImmersiveMedia(callbacks: ImmersiveMediaCallbacks): Boolean {
        this.immersiveCallbacks = callbacks
        
        return try {
            // Initialize all immersive components
            vrRenderer.initialize()
            arOverlayManager.initialize()
            sphericalVideoProcessor.initialize()
            gyroscopeController.initialize()
            immersiveUIManager.initialize()
            
            callbacks.onImmersiveMediaInitialized()
            true
        } catch (e: Exception) {
            Log.e("ImmersiveMedia", "Failed to initialize immersive media engine", e)
            false
        }
    }
    
    suspend fun startImmersiveSession(
        mediaItem: MediaItem,
        mode: ViewingMode,
        config: ImmersiveConfiguration
    ): ImmersiveSession {
        return withContext(Dispatchers.Default) {
            val session = ImmersiveSession(
                id = UUID.randomUUID().toString(),
                mediaItem = mediaItem,
                viewingMode = mode,
                configuration = config,
                startTime = System.currentTimeMillis()
            )
            
            currentImmersiveSession = session
            currentViewingMode = mode
            
            when (mode) {
                ViewingMode.VR_HEADSET -> initializeVRSession(session)
                ViewingMode.VR_CARDBOARD -> initializeCardboardSession(session)
                ViewingMode.SPHERICAL_360 -> initializeSphericalSession(session)
                ViewingMode.AR_OVERLAY -> initializeARSession(session)
                ViewingMode.IMMERSIVE_FULLSCREEN -> initializeImmersiveSession(session)
                ViewingMode.TRADITIONAL -> { /* No special initialization needed */ }
            }
            
            immersiveCallbacks?.onImmersiveSessionStarted(session)
            session
        }
    }
    
    private suspend fun initializeVRSession(session: ImmersiveSession) {
        // Setup VR rendering pipeline
        vrRenderer.setupVRDisplay(session.configuration.vrConfig)
        
        // Initialize head tracking
        gyroscopeController.enableHeadTracking { orientation ->
            vrRenderer.updateViewOrientation(orientation)
            immersiveCallbacks?.onHeadOrientationChanged(orientation)
        }
        
        // Setup VR-specific UI
        immersiveUIManager.switchToVRMode()
    }
    
    private suspend fun initializeCardboardSession(session: ImmersiveSession) {
        // Setup Google Cardboard SDK integration
        val cardboardConfig = CardboardConfiguration(
            screenToLensDistance = session.configuration.cardboardConfig.screenToLensDistance,
            interLensDistance = session.configuration.cardboardConfig.interLensDistance,
            leftEyeFOV = session.configuration.cardboardConfig.leftEyeFOV,
            rightEyeFOV = session.configuration.cardboardConfig.rightEyeFOV
        )
        
        vrRenderer.setupCardboardDisplay(cardboardConfig)
        gyroscopeController.enableCardboardTracking()
        immersiveUIManager.switchToCardboardMode()
    }
    
    private suspend fun initializeSphericalSession(session: ImmersiveSession) {
        // Process spherical/360° video
        sphericalVideoProcessor.setupSphericalProjection(session.mediaItem)
        
        // Enable gyroscope control for 360° navigation
        gyroscopeController.enableSphericalNavigation { rotation ->
            sphericalVideoProcessor.updateViewDirection(rotation)
        }
        
        // Setup 360° UI controls
        immersiveUIManager.switchToSphericalMode()
    }
    
    private suspend fun initializeARSession(session: ImmersiveSession) {
        // Initialize AR overlay system
        arOverlayManager.startARSession(session.configuration.arConfig)
        
        // Setup camera pass-through if needed
        if (session.configuration.arConfig.enableCameraPassThrough) {
            arOverlayManager.enableCameraPassThrough()
        }
        
        immersiveUIManager.switchToARMode()
    }
    
    private suspend fun initializeImmersiveSession(session: ImmersiveSession) {
        // Setup immersive fullscreen mode with enhanced UI
        immersiveUIManager.switchToImmersiveMode()
        
        // Enable ambient lighting control
        if (session.configuration.immersiveConfig.adaptiveBrightness) {
            enableAdaptiveBrightness()
        }
        
        // Setup gesture-based navigation
        gyroscopeController.enableImmersiveGestures()
    }
    
    suspend fun switchViewingMode(newMode: ViewingMode) {
        currentImmersiveSession?.let { session ->
            // Cleanup current mode
            cleanupCurrentMode()
            
            // Switch to new mode
            session.viewingMode = newMode
            currentViewingMode = newMode
            
            when (newMode) {
                ViewingMode.VR_HEADSET -> initializeVRSession(session)
                ViewingMode.VR_CARDBOARD -> initializeCardboardSession(session)
                ViewingMode.SPHERICAL_360 -> initializeSphericalSession(session)
                ViewingMode.AR_OVERLAY -> initializeARSession(session)
                ViewingMode.IMMERSIVE_FULLSCREEN -> initializeImmersiveSession(session)
                ViewingMode.TRADITIONAL -> immersiveUIManager.switchToTraditionalMode()
            }
            
            immersiveCallbacks?.onViewingModeChanged(newMode)
        }
    }
    
    suspend fun addAROverlay(overlay: AROverlay) {
        if (currentViewingMode == ViewingMode.AR_OVERLAY) {
            arOverlayManager.addOverlay(overlay)
            immersiveCallbacks?.onAROverlayAdded(overlay)
        }
    }
    
    suspend fun updateSpatialPosition(position: Vector3D, rotation: Quaternion) {
        when (currentViewingMode) {
            ViewingMode.VR_HEADSET, ViewingMode.VR_CARDBOARD -> {
                vrRenderer.updateViewerPosition(position, rotation)
            }
            ViewingMode.SPHERICAL_360 -> {
                sphericalVideoProcessor.updateViewDirection(rotation)
            }
            ViewingMode.AR_OVERLAY -> {
                arOverlayManager.updateViewerTransform(position, rotation)
            }
            else -> { /* No spatial updates needed */ }
        }
    }
    
    suspend fun enableEyeTracking(enable: Boolean) {
        if (currentViewingMode in listOf(ViewingMode.VR_HEADSET, ViewingMode.VR_CARDBOARD)) {
            vrRenderer.enableEyeTracking(enable)
            
            if (enable) {
                vrRenderer.onEyeTrackingData { eyeData ->
                    immersiveCallbacks?.onEyeTrackingUpdate(eyeData)
                }
            }
        }
    }
    
    suspend fun calibrateVRHeadset() {
        if (currentViewingMode in listOf(ViewingMode.VR_HEADSET, ViewingMode.VR_CARDBOARD)) {
            val calibrationData = vrRenderer.performCalibration()
            immersiveCallbacks?.onVRCalibrationComplete(calibrationData)
        }
    }
    
    suspend fun captureImmersiveScreenshot(): ImmersiveScreenshot? {
        return currentImmersiveSession?.let { session ->
            when (currentViewingMode) {
                ViewingMode.VR_HEADSET, ViewingMode.VR_CARDBOARD -> {
                    vrRenderer.captureVRScreenshot()
                }
                ViewingMode.SPHERICAL_360 -> {
                    sphericalVideoProcessor.captureSphericalScreenshot()
                }
                ViewingMode.AR_OVERLAY -> {
                    arOverlayManager.captureARScreenshot()
                }
                else -> {
                    captureTraditionalScreenshot()
                }
            }
        }
    }
    
    suspend fun stopImmersiveSession(): ImmersiveSessionResult {
        return currentImmersiveSession?.let { session ->
            try {
                // Cleanup current mode
                cleanupCurrentMode()
                
                // Stop all immersive components
                vrRenderer.cleanup()
                arOverlayManager.stopARSession()
                sphericalVideoProcessor.cleanup()
                gyroscopeController.disable()
                immersiveUIManager.switchToTraditionalMode()
                
                // Calculate session statistics
                val sessionDuration = System.currentTimeMillis() - session.startTime
                val sessionStats = ImmersiveSessionStats(
                    duration = sessionDuration,
                    viewingMode = session.viewingMode,
                    headMovements = gyroscopeController.getMovementCount(),
                    modeChanges = session.modeChangeCount,
                    overlaysUsed = session.overlaysUsed.size
                )
                
                currentImmersiveSession = null
                currentViewingMode = ViewingMode.TRADITIONAL
                
                val result = ImmersiveSessionResult.Success(sessionStats)
                immersiveCallbacks?.onImmersiveSessionEnded(result)
                result
                
            } catch (e: Exception) {
                val result = ImmersiveSessionResult.Error("Failed to stop immersive session: ${e.message}")
                immersiveCallbacks?.onImmersiveSessionEnded(result)
                result
            }
        } ?: ImmersiveSessionResult.Error("No active immersive session")
    }
    
    private suspend fun cleanupCurrentMode() {
        when (currentViewingMode) {
            ViewingMode.VR_HEADSET, ViewingMode.VR_CARDBOARD -> {
                vrRenderer.disableVRDisplay()
                gyroscopeController.disableHeadTracking()
            }
            ViewingMode.SPHERICAL_360 -> {
                sphericalVideoProcessor.disableSphericalProjection()
                gyroscopeController.disableSphericalNavigation()
            }
            ViewingMode.AR_OVERLAY -> {
                arOverlayManager.clearAllOverlays()
                if (arOverlayManager.isCameraPassThroughEnabled()) {
                    arOverlayManager.disableCameraPassThrough()
                }
            }
            ViewingMode.IMMERSIVE_FULLSCREEN -> {
                disableAdaptiveBrightness()
                gyroscopeController.disableImmersiveGestures()
            }
            ViewingMode.TRADITIONAL -> { /* No cleanup needed */ }
        }
    }
    
    private fun enableAdaptiveBrightness() {
        // Implement adaptive brightness based on content
    }
    
    private fun disableAdaptiveBrightness() {
        // Restore original brightness settings
    }
    
    private fun captureTraditionalScreenshot(): ImmersiveScreenshot {
        // Capture regular screenshot
        return ImmersiveScreenshot(
            id = UUID.randomUUID().toString(),
            viewingMode = ViewingMode.TRADITIONAL,
            timestamp = System.currentTimeMillis(),
            imageData = ByteArray(0), // Placeholder
            metadata = emptyMap()
        )
    }
    
    fun getCurrentViewingMode(): ViewingMode = currentViewingMode
    
    fun getImmersiveCapabilities(): ImmersiveCapabilities {
        return ImmersiveCapabilities(
            supportsVR = vrRenderer.isVRSupported(),
            supportsAR = arOverlayManager.isARSupported(),
            supports360Video = sphericalVideoProcessor.isSphericalSupported(),
            supportsEyeTracking = vrRenderer.isEyeTrackingSupported(),
            supportsGyroscope = gyroscopeController.isGyroscopeAvailable(),
            supportedVRHeadsets = vrRenderer.getSupportedHeadsets(),
            maxAROverlays = arOverlayManager.getMaxOverlays()
        )
    }
}

// 2. VR Renderer
@Singleton
class VRRenderer @Inject constructor(
    private val context: Context
) {
    
    private var vrDisplay: VRDisplay? = null
    private var eyeTracker: EyeTracker? = null
    private var isVRModeActive = false
    
    suspend fun initialize(): Boolean = withContext(Dispatchers.IO) {
        try {
            // Initialize VR rendering system
            vrDisplay = VRDisplay(context)
            eyeTracker = EyeTracker(context)
            
            Log.i("VRRenderer", "VR renderer initialized")
            true
        } catch (e: Exception) {
            Log.e("VRRenderer", "Failed to initialize VR renderer", e)
            false
        }
    }
    
    suspend fun setupVRDisplay(config: VRConfiguration) {
        vrDisplay?.setup(config)
        isVRModeActive = true
    }
    
    suspend fun setupCardboardDisplay(config: CardboardConfiguration) {
        vrDisplay?.setupCardboard(config)
        isVRModeActive = true
    }
    
    suspend fun updateViewOrientation(orientation: Quaternion) {
        vrDisplay?.updateOrientation(orientation)
    }
    
    suspend fun updateViewerPosition(position: Vector3D, rotation: Quaternion) {
        vrDisplay?.updateViewerTransform(position, rotation)
    }
    
    suspend fun enableEyeTracking(enable: Boolean) {
        if (enable && isEyeTrackingSupported()) {
            eyeTracker?.startTracking()
        } else {
            eyeTracker?.stopTracking()
        }
    }
    
    suspend fun onEyeTrackingData(callback: (EyeTrackingData) -> Unit) {
        eyeTracker?.setDataCallback(callback)
    }
    
    suspend fun performCalibration(): VRCalibrationData {
        return vrDisplay?.performCalibration() ?: VRCalibrationData.default()
    }
    
    suspend fun captureVRScreenshot(): ImmersiveScreenshot {
        val leftEyeImage = vrDisplay?.captureLeftEye() ?: ByteArray(0)
        val rightEyeImage = vrDisplay?.captureRightEye() ?: ByteArray(0)
        
        return ImmersiveScreenshot(
            id = UUID.randomUUID().toString(),
            viewingMode = ViewingMode.VR_HEADSET,
            timestamp = System.currentTimeMillis(),
            imageData = combineStereoscopicImages(leftEyeImage, rightEyeImage),
            metadata = mapOf(
                "left_eye_data" to leftEyeImage,
                "right_eye_data" to rightEyeImage,
                "stereo_mode" to "side_by_side"
            )
        )
    }
    
    suspend fun disableVRDisplay() {
        vrDisplay?.disable()
        isVRModeActive = false
    }
    
    suspend fun cleanup() {
        vrDisplay?.cleanup()
        eyeTracker?.cleanup()
        isVRModeActive = false
    }
    
    fun isVRSupported(): Boolean {
        return context.packageManager.hasSystemFeature(PackageManager.FEATURE_VR_MODE) ||
               context.packageManager.hasSystemFeature(PackageManager.FEATURE_VR_MODE_HIGH_PERFORMANCE)
    }
    
    fun isEyeTrackingSupported(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
               context.packageManager.hasSystemFeature("android.hardware.vr.high_performance")
    }
    
    fun getSupportedHeadsets(): List<String> {
        return listOf(
            "Google Daydream",
            "Samsung Gear VR",
            "Google Cardboard",
            "Oculus Go",
            "Generic VR Headset"
        )
    }
    
    private fun combineStereoscopicImages(leftEye: ByteArray, rightEye: ByteArray): ByteArray {
        // Combine left and right eye images into side-by-side format
        return ByteArray(0) // Placeholder implementation
    }
}

// 3. AR Overlay Manager
@Singleton
class AROverlayManager @Inject constructor(
    private val context: Context
) {
    
    private var arSession: ARSession? = null
    private val activeOverlays = mutableMapOf<String, AROverlay>()
    private var cameraPassThroughEnabled = false
    private val maxOverlays = 10
    
    suspend fun initialize(): Boolean = withContext(Dispatchers.IO) {
        try {
            // Check AR support
            if (!isARSupported()) {
                Log.w("AROverlayManager", "AR not supported on this device")
                return@withContext false
            }
            
            Log.i("AROverlayManager", "AR overlay manager initialized")
            true
        } catch (e: Exception) {
            Log.e("AROverlayManager", "Failed to initialize AR overlay manager", e)
            false
        }
    }
    
    suspend fun startARSession(config: ARConfiguration) {
        if (!isARSupported()) return
        
        arSession = ARSession(context, config)
        arSession?.start()
    }
    
    suspend fun addOverlay(overlay: AROverlay): Boolean {
        if (activeOverlays.size >= maxOverlays) {
            Log.w("AROverlayManager", "Maximum number of overlays reached")
            return false
        }
        
        activeOverlays[overlay.id] = overlay
        arSession?.addOverlay(overlay)
        return true
    }
    
    suspend fun removeOverlay(overlayId: String): Boolean {
        val overlay = activeOverlays.remove(overlayId)
        return if (overlay != null) {
            arSession?.removeOverlay(overlay)
            true
        } else {
            false
        }
    }
    
    suspend fun updateOverlay(overlayId: String, updates: AROverlayUpdate) {
        activeOverlays[overlayId]?.let { overlay ->
            overlay.applyUpdate(updates)
            arSession?.updateOverlay(overlay)
        }
    }
    
    suspend fun enableCameraPassThrough() {
        if (isARSupported()) {
            arSession?.enableCameraPassThrough()
            cameraPassThroughEnabled = true
        }
    }
    
    suspend fun disableCameraPassThrough() {
        arSession?.disableCameraPassThrough()
        cameraPassThroughEnabled = false
    }
    
    suspend fun updateViewerTransform(position: Vector3D, rotation: Quaternion) {
        arSession?.updateViewerTransform(position, rotation)
    }
    
    suspend fun captureARScreenshot(): ImmersiveScreenshot {
        val arFrame = arSession?.captureFrame() ?: ByteArray(0)
        
        return ImmersiveScreenshot(
            id = UUID.randomUUID().toString(),
            viewingMode = ViewingMode.AR_OVERLAY,
            timestamp = System.currentTimeMillis(),
            imageData = arFrame,
            metadata = mapOf(
                "overlays_count" to activeOverlays.size,
                "camera_passthrough" to cameraPassThroughEnabled,
                "ar_tracking_state" to (arSession?.getTrackingState() ?: "UNKNOWN")
            )
        )
    }
    
    suspend fun clearAllOverlays() {
        activeOverlays.clear()
        arSession?.clearAllOverlays()
    }
    
    suspend fun stopARSession() {
        arSession?.stop()
        arSession = null
        activeOverlays.clear()
        cameraPassThroughEnabled = false
    }
    
    fun isARSupported(): Boolean {
        return context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA) &&
               Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
    }
    
    fun isCameraPassThroughEnabled(): Boolean = cameraPassThroughEnabled
    
    fun getMaxOverlays(): Int = maxOverlays
    
    fun getActiveOverlays(): List<AROverlay> = activeOverlays.values.toList()
}

// 4. Spherical Video Processor
@Singleton
class SphericalVideoProcessor @Inject constructor(
    private val context: Context
) {
    
    private var sphericalRenderer: SphericalRenderer? = null
    private var currentProjection = SphericalProjection.EQUIRECTANGULAR
    private var isSphericalModeActive = false
    
    suspend fun initialize(): Boolean = withContext(Dispatchers.IO) {
        try {
            sphericalRenderer = SphericalRenderer(context)
            sphericalRenderer?.initialize()
            
            Log.i("SphericalVideo", "Spherical video processor initialized")
            true
        } catch (e: Exception) {
            Log.e("SphericalVideo", "Failed to initialize spherical video processor", e)
            false
        }
    }
    
    suspend fun setupSphericalProjection(mediaItem: MediaItem) {
        // Detect spherical video format from metadata
        val projection = detectSphericalProjection(mediaItem)
        currentProjection = projection
        
        sphericalRenderer?.setupProjection(projection)
        isSphericalModeActive = true
    }
    
    suspend fun updateViewDirection(rotation: Quaternion) {
        sphericalRenderer?.updateViewDirection(rotation)
    }
    
    suspend fun setFieldOfView(fov: Float) {
        sphericalRenderer?.setFieldOfView(fov)
    }
    
    suspend fun enableInteractiveNavigation(enable: Boolean) {
        sphericalRenderer?.enableInteractiveNavigation(enable)
    }
    
    suspend fun captureSphericalScreenshot(): ImmersiveScreenshot {
        val sphericalFrame = sphericalRenderer?.captureCurrentView() ?: ByteArray(0)
        val currentFOV = sphericalRenderer?.getCurrentFOV() ?: 90f
        val viewDirection = sphericalRenderer?.getCurrentViewDirection() ?: Quaternion.identity()
        
        return ImmersiveScreenshot(
            id = UUID.randomUUID().toString(),
            viewingMode = ViewingMode.SPHERICAL_360,
            timestamp = System.currentTimeMillis(),
            imageData = sphericalFrame,
            metadata = mapOf(
                "projection_type" to currentProjection.name,
                "field_of_view" to currentFOV,
                "view_direction" to viewDirection.toString(),
                "interactive_mode" to (sphericalRenderer?.isInteractiveModeEnabled() ?: false)
            )
        )
    }
    
    suspend fun disableSphericalProjection() {
        sphericalRenderer?.disableProjection()
        isSphericalModeActive = false
    }
    
    suspend fun cleanup() {
        sphericalRenderer?.cleanup()
        isSphericalModeActive = false
    }
    
    private fun detectSphericalProjection(mediaItem: MediaItem): SphericalProjection {
        // Analyze video metadata to detect spherical format
        val metadata = mediaItem.mediaMetadata.extras
        
        return when {
            metadata?.getString("spherical_projection")?.contains("equirectangular", true) == true -> 
                SphericalProjection.EQUIRECTANGULAR
            metadata?.getString("spherical_projection")?.contains("cubic", true) == true -> 
                SphericalProjection.CUBIC
            metadata?.getString("projection_type")?.contains("360", true) == true -> 
                SphericalProjection.EQUIRECTANGULAR
            else -> SphericalProjection.EQUIRECTANGULAR // Default
        }
    }
    
    fun isSphericalSupported(): Boolean {
        return context.packageManager.hasSystemFeature(PackageManager.FEATURE_OPENGL_ES_3_0)
    }
    
    fun getSupportedProjections(): List<SphericalProjection> {
        return SphericalProjection.values().toList()
    }
}

// 5. Gyroscope Controller
@Singleton
class GyroscopeController @Inject constructor(
    private val context: Context
) {
    
    private var sensorManager: SensorManager? = null
    private var gyroscope: Sensor? = null
    private var accelerometer: Sensor? = null
    private var magnetometer: Sensor? = null
    
    private var headTrackingCallback: ((Quaternion) -> Unit)? = null
    private var sphericalNavigationCallback: ((Quaternion) -> Unit)? = null
    private var movementCount = 0
    
    private val rotationMatrix = FloatArray(16)
    private val orientation = FloatArray(3)
    
    suspend fun initialize(): Boolean = withContext(Dispatchers.IO) {
        try {
            sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
            gyroscope = sensorManager?.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
            accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
            magnetometer = sensorManager?.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
            
            Log.i("GyroscopeController", "Gyroscope controller initialized")
            true
        } catch (e: Exception) {
            Log.e("GyroscopeController", "Failed to initialize gyroscope controller", e)
            false
        }
    }
    
    suspend fun enableHeadTracking(callback: (Quaternion) -> Unit) {
        headTrackingCallback = callback
        startSensorListening()
    }
    
    suspend fun disableHeadTracking() {
        headTrackingCallback = null
        stopSensorListening()
    }
    
    suspend fun enableSphericalNavigation(callback: (Quaternion) -> Unit) {
        sphericalNavigationCallback = callback
        startSensorListening()
    }
    
    suspend fun disableSphericalNavigation() {
        sphericalNavigationCallback = null
        stopSensorListening()
    }
    
    suspend fun enableCardboardTracking() {
        // Specific tracking for Google Cardboard
        enableHeadTracking { orientation ->
            // Apply Cardboard-specific transformations
            val cardboardOrientation = applyCardboardCorrection(orientation)
            headTrackingCallback?.invoke(cardboardOrientation)
        }
    }
    
    suspend fun enableImmersiveGestures() {
        // Enable gesture-based navigation for immersive mode
        startSensorListening()
    }
    
    suspend fun disableImmersiveGestures() {
        stopSensorListening()
    }
    
    private fun startSensorListening() {
        sensorManager?.let { sm ->
            gyroscope?.let { gyro ->
                sm.registerListener(sensorEventListener, gyro, SensorManager.SENSOR_DELAY_GAME)
            }
            accelerometer?.let { accel ->
                sm.registerListener(sensorEventListener, accel, SensorManager.SENSOR_DELAY_GAME)
            }
            magnetometer?.let { mag ->
                sm.registerListener(sensorEventListener, mag, SensorManager.SENSOR_DELAY_GAME)
            }
        }
    }
    
    private fun stopSensorListening() {
        sensorManager?.unregisterListener(sensorEventListener)
    }
    
    private val sensorEventListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent?) {
            event?.let { processEvent(it) }
        }
        
        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
            // Handle accuracy changes if needed
        }
    }
    
    private fun processEvent(event: SensorEvent) {
        when (event.sensor.type) {
            Sensor.TYPE_ROTATION_VECTOR -> {
                // Convert rotation vector to quaternion
                val quaternion = rotationVectorToQuaternion(event.values)
                
                headTrackingCallback?.invoke(quaternion)
                sphericalNavigationCallback?.invoke(quaternion)
                
                movementCount++
            }
            
            Sensor.TYPE_GYROSCOPE -> {
                // Process gyroscope data for rotation tracking
                val rotationRate = Vector3D(event.values[0], event.values[1], event.values[2])
                processRotationData(rotationRate)
            }
        }
    }
    
    private fun rotationVectorToQuaternion(rotationVector: FloatArray): Quaternion {
        val q = FloatArray(4)
        SensorManager.getQuaternionFromVector(q, rotationVector)
        return Quaternion(q[1], q[2], q[3], q[0]) // Convert to our quaternion format
    }
    
    private fun processRotationData(rotationRate: Vector3D) {
        // Process gyroscope rotation data
        // Apply integration and filtering as needed
    }
    
    private fun applyCardboardCorrection(orientation: Quaternion): Quaternion {
        // Apply specific corrections for Google Cardboard
        // This would include lens distortion correction and IPD adjustments
        return orientation // Placeholder
    }
    
    suspend fun disable() {
        stopSensorListening()
        headTrackingCallback = null
        sphericalNavigationCallback = null
    }
    
    fun isGyroscopeAvailable(): Boolean {
        return gyroscope != null || 
               sensorManager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR) != null
    }
    
    fun getMovementCount(): Int = movementCount
    
    fun resetMovementCount() {
        movementCount = 0
    }
}

// 6. Data Classes
data class ImmersiveSession(
    val id: String,
    val mediaItem: MediaItem,
    var viewingMode: ViewingMode,
    val configuration: ImmersiveConfiguration,
    val startTime: Long,
    var modeChangeCount: Int = 0,
    val overlaysUsed: MutableSet<String> = mutableSetOf()
)

data class ImmersiveConfiguration(
    val vrConfig: VRConfiguration = VRConfiguration(),
    val cardboardConfig: CardboardConfiguration = CardboardConfiguration(),
    val arConfig: ARConfiguration = ARConfiguration(),
    val immersiveConfig: ImmersiveViewConfiguration = ImmersiveViewConfiguration()
)

data class VRConfiguration(
    val ipd: Float = 63f, // Interpupillary distance in mm
    val screenToLensDistance: Float = 50f,
    val enableEyeTracking: Boolean = false,
    val renderResolution: Resolution = Resolution(2160, 1200)
)

data class CardboardConfiguration(
    val screenToLensDistance: Float = 39.5f,
    val interLensDistance: Float = 62f,
    val leftEyeFOV: FieldOfView = FieldOfView(45f, 45f, 50f, 50f),
    val rightEyeFOV: FieldOfView = FieldOfView(45f, 45f, 50f, 50f)
)

data class ARConfiguration(
    val enableCameraPassThrough: Boolean = true,
    val overlayOpacity: Float = 0.8f,
    val trackingMode: ARTrackingMode = ARTrackingMode.WORLD_TRACKING
)

data class ImmersiveViewConfiguration(
    val adaptiveBrightness: Boolean = true,
    val hideSystemUI: Boolean = true,
    val enableAmbientLighting: Boolean = false
)

data class FieldOfView(
    val left: Float,
    val right: Float,
    val top: Float,
    val bottom: Float
)

data class AROverlay(
    val id: String,
    val type: AROverlayType,
    val content: String,
    val position: Vector3D,
    val rotation: Quaternion,
    val scale: Vector3D,
    val opacity: Float = 1f,
    val isVisible: Boolean = true
) {
    fun applyUpdate(update: AROverlayUpdate) {
        // Apply updates to overlay properties
    }
}

data class AROverlayUpdate(
    val position: Vector3D? = null,
    val rotation: Quaternion? = null,
    val scale: Vector3D? = null,
    val opacity: Float? = null,
    val content: String? = null
)

data class Vector3D(
    val x: Float,
    val y: Float,
    val z: Float
) {
    companion object {
        fun zero() = Vector3D(0f, 0f, 0f)
        fun up() = Vector3D(0f, 1f, 0f)
        fun forward() = Vector3D(0f, 0f, 1f)
    }
}

data class Quaternion(
    val x: Float,
    val y: Float,
    val z: Float,
    val w: Float
) {
    companion object {
        fun identity() = Quaternion(0f, 0f, 0f, 1f)
    }
    
    override fun toString(): String = "($x, $y, $z, $w)"
}

data class ImmersiveScreenshot(
    val id: String,
    val viewingMode: ViewingMode,
    val timestamp: Long,
    val imageData: ByteArray,
    val metadata: Map<String, Any>
)

data class ImmersiveSessionStats(
    val duration: Long,
    val viewingMode: ViewingMode,
    val headMovements: Int,
    val modeChanges: Int,
    val overlaysUsed: Int
)

data class VRCalibrationData(
    val ipd: Float,
    val screenToLensDistance: Float,
    val lensDistortionCorrection: FloatArray
) {
    companion object {
        fun default() = VRCalibrationData(63f, 50f, floatArrayOf(0f, 0f, 0f, 0f))
    }
}

data class EyeTrackingData(
    val leftEyePosition: Vector3D,
    val rightEyePosition: Vector3D,
    val leftEyeDirection: Vector3D,
    val rightEyeDirection: Vector3D,
    val pupilDilation: Float,
    val blinkState: BlinkState
)

data class ImmersiveCapabilities(
    val supportsVR: Boolean,
    val supportsAR: Boolean,
    val supports360Video: Boolean,
    val supportsEyeTracking: Boolean,
    val supportsGyroscope: Boolean,
    val supportedVRHeadsets: List<String>,
    val maxAROverlays: Int
)

// Enums
enum class ViewingMode {
    TRADITIONAL,
    VR_HEADSET,
    VR_CARDBOARD,
    SPHERICAL_360,
    AR_OVERLAY,
    IMMERSIVE_FULLSCREEN
}

enum class SphericalProjection {
    EQUIRECTANGULAR,
    CUBIC,
    CYLINDRICAL
}

enum class AROverlayType {
    TEXT,
    IMAGE,
    VIDEO,
    MODEL_3D,
    UI_ELEMENT
}

enum class ARTrackingMode {
    WORLD_TRACKING,
    ORIENTATION_TRACKING,
    FACE_TRACKING
}

enum class BlinkState {
    OPEN,
    CLOSED,
    HALF_CLOSED
}

sealed class ImmersiveSessionResult {
    data class Success(val stats: ImmersiveSessionStats) : ImmersiveSessionResult()
    data class Error(val message: String) : ImmersiveSessionResult()
}

// 7. Supporting Classes (Simplified Implementations)
class VRDisplay(private val context: Context) {
    fun setup(config: VRConfiguration) {}
    fun setupCardboard(config: CardboardConfiguration) {}
    fun updateOrientation(orientation: Quaternion) {}
    fun updateViewerTransform(position: Vector3D, rotation: Quaternion) {}
    fun performCalibration(): VRCalibrationData = VRCalibrationData.default()
    fun captureLeftEye(): ByteArray = ByteArray(0)
    fun captureRightEye(): ByteArray = ByteArray(0)
    fun disable() {}
    fun cleanup() {}
}

class EyeTracker(private val context: Context) {
    fun startTracking() {}
    fun stopTracking() {}
    fun setDataCallback(callback: (EyeTrackingData) -> Unit) {}
    fun cleanup() {}
}

class ARSession(private val context: Context, private val config: ARConfiguration) {
    fun start() {}
    fun stop() {}
    fun addOverlay(overlay: AROverlay) {}
    fun removeOverlay(overlay: AROverlay) {}
    fun updateOverlay(overlay: AROverlay) {}
    fun enableCameraPassThrough() {}
    fun disableCameraPassThrough() {}
    fun updateViewerTransform(position: Vector3D, rotation: Quaternion) {}
    fun captureFrame(): ByteArray = ByteArray(0)
    fun clearAllOverlays() {}
    fun getTrackingState(): String = "TRACKING"
}

class SphericalRenderer(private val context: Context) {
    fun initialize() {}
    fun setupProjection(projection: SphericalProjection) {}
    fun updateViewDirection(rotation: Quaternion) {}
    fun setFieldOfView(fov: Float) {}
    fun enableInteractiveNavigation(enable: Boolean) {}
    fun captureCurrentView(): ByteArray = ByteArray(0)
    fun getCurrentFOV(): Float = 90f
    fun getCurrentViewDirection(): Quaternion = Quaternion.identity()
    fun isInteractiveModeEnabled(): Boolean = true
    fun disableProjection() {}
    fun cleanup() {}
}

class ImmersiveUIManager {
    fun initialize() {}
    fun switchToVRMode() {}
    fun switchToCardboardMode() {}
    fun switchToSphericalMode() {}
    fun switchToARMode() {}
    fun switchToImmersiveMode() {}
    fun switchToTraditionalMode() {}
}

// 8. Immersive Media Callbacks Interface
interface ImmersiveMediaCallbacks {
    fun onImmersiveMediaInitialized()
    fun onImmersiveSessionStarted(session: ImmersiveSession)
    fun onViewingModeChanged(mode: ViewingMode)
    fun onHeadOrientationChanged(orientation: Quaternion)
    fun onEyeTrackingUpdate(data: EyeTrackingData)
    fun onAROverlayAdded(overlay: AROverlay)
    fun onVRCalibrationComplete(data: VRCalibrationData)
    fun onImmersiveSessionEnded(result: ImmersiveSessionResult)
    fun onImmersiveError(error: String)
}