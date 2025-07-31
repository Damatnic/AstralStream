// ================================
// Phase 3.4: Immersive Video ViewModel and Integration
// ================================

// 11. Immersive Video ViewModel
@HiltViewModel
class ImmersiveVideoViewModel @Inject constructor(
    private val immersiveMediaEngine: ImmersiveMediaEngine,
    private val savedStateHandle: SavedStateHandle,
    @ApplicationContext private val context: Context
) : ViewModel(), ImmersiveMediaCallbacks {
    
    private val _immersiveState = MutableStateFlow(ImmersiveState())
    val immersiveState: StateFlow<ImmersiveState> = _immersiveState.asStateFlow()
    
    private val _viewingMode = MutableStateFlow(ViewingMode.TRADITIONAL)
    val viewingMode: StateFlow<ViewingMode> = _viewingMode.asStateFlow()
    
    private val _sphericalRotation = MutableStateFlow(Quaternion.identity())
    val sphericalRotation: StateFlow<Quaternion> = _sphericalRotation.asStateFlow()
    
    private val _fieldOfView = MutableStateFlow(90f)
    val fieldOfView: StateFlow<Float> = _fieldOfView.asStateFlow()
    
    private val _arOverlays = MutableStateFlow<List<AROverlay>>(emptyList())
    val arOverlays: StateFlow<List<AROverlay>> = _arOverlays.asStateFlow()
    
    private val _showControls = MutableStateFlow(true)
    val showControls: StateFlow<Boolean> = _showControls.asStateFlow()
    
    val player = ExoPlayer.Builder(context)
        .setVideoScalingMode(C.VIDEO_SCALING_MODE_SCALE_TO_FIT)
        .build()
    
    private var currentSession: ImmersiveSession? = null
    
    init {
        viewModelScope.launch {
            immersiveMediaEngine.initializeImmersiveMedia(this@ImmersiveVideoViewModel)
        }
    }
    
    fun loadVideo(uri: Uri) {
        val mediaItem = MediaItem.fromUri(uri)
        player.setMediaItem(mediaItem)
        player.prepare()
        
        // Check if video supports immersive features
        checkImmersiveCapabilities(mediaItem)
    }
    
    fun switchViewingMode(mode: ViewingMode) {
        viewModelScope.launch {
            if (currentSession != null) {
                immersiveMediaEngine.switchViewingMode(mode)
            } else {
                startImmersiveSession(mode)
            }
            _viewingMode.value = mode
        }
    }
    
    private suspend fun startImmersiveSession(mode: ViewingMode) {
        player.currentMediaItem?.let { mediaItem ->
            val config = createImmersiveConfiguration(mode)
            currentSession = immersiveMediaEngine.startImmersiveSession(
                mediaItem = mediaItem,
                mode = mode,
                config = config
            )
        }
    }
    
    private fun createImmersiveConfiguration(mode: ViewingMode): ImmersiveConfiguration {
        return ImmersiveConfiguration(
            vrConfig = VRConfiguration(
                ipd = getStoredIPD(),
                enableEyeTracking = isEyeTrackingEnabled()
            ),
            arConfig = ARConfiguration(
                enableCameraPassThrough = true,
                trackingMode = ARTrackingMode.WORLD_TRACKING
            ),
            immersiveConfig = ImmersiveViewConfiguration(
                adaptiveBrightness = true,
                hideSystemUI = true
            )
        )
    }
    
    fun togglePlayPause() {
        if (player.isPlaying) {
            player.pause()
        } else {
            player.play()
        }
    }
    
    fun seek(position: Long) {
        player.seekTo(position)
    }
    
    fun rotateSphere(rotation: Quaternion) {
        viewModelScope.launch {
            immersiveMediaEngine.updateSphericalRotation(rotation)
            _sphericalRotation.value = rotation
        }
    }
    
    fun adjustFieldOfView(delta: Float) {
        val newFOV = (_fieldOfView.value + delta).coerceIn(30f, 120f)
        viewModelScope.launch {
            immersiveMediaEngine.setFieldOfView(newFOV)
            _fieldOfView.value = newFOV
        }
    }
    
    fun addTextOverlay() {
        val overlay = AROverlay(
            id = UUID.randomUUID().toString(),
            type = AROverlayType.TEXT,
            content = "Sample Text",
            position = Vector3D(0f, 0f, -2f),
            rotation = Quaternion.identity(),
            scale = Vector3D(1f, 1f, 1f)
        )
        
        viewModelScope.launch {
            immersiveMediaEngine.addAROverlay(overlay)
            _arOverlays.value = _arOverlays.value + overlay
        }
    }
    
    fun addImageOverlay() {
        // Implementation for adding image overlay
    }
    
    fun updateAROverlay(update: AROverlayUpdate) {
        viewModelScope.launch {
            immersiveMediaEngine.updateAROverlay(update)
        }
    }
    
    fun removeAROverlay(id: String) {
        viewModelScope.launch {
            immersiveMediaEngine.removeAROverlay(id)
            _arOverlays.value = _arOverlays.value.filter { it.id != id }
        }
    }
    
    fun toggleARTracking() {
        // Toggle between different AR tracking modes
    }
    
    fun toggleControlsVisibility() {
        _showControls.value = !_showControls.value
    }
    
    fun captureImmersiveScreenshot() {
        viewModelScope.launch {
            val screenshot = immersiveMediaEngine.captureImmersiveScreenshot()
            saveScreenshot(screenshot)
        }
    }
    
    fun calibrateVR() {
        viewModelScope.launch {
            val calibrationData = immersiveMediaEngine.performVRCalibration()
            saveCalibrationData(calibrationData)
        }
    }
    
    // ImmersiveMediaCallbacks implementation
    override fun onImmersiveMediaInitialized() {
        _immersiveState.value = _immersiveState.value.copy(
            isInitialized = true,
            capabilities = immersiveMediaEngine.getImmersiveCapabilities()
        )
    }
    
    override fun onImmersiveSessionStarted(session: ImmersiveSession) {
        _immersiveState.value = _immersiveState.value.copy(
            isSessionActive = true,
            currentSessionId = session.id
        )
    }
    
    override fun onViewingModeChanged(mode: ViewingMode) {
        _viewingMode.value = mode
    }
    
    override fun onHeadOrientationChanged(orientation: Quaternion) {
        // Update UI based on head orientation if needed
    }
    
    override fun onEyeTrackingUpdate(data: EyeTrackingData) {
        // Process eye tracking data
    }
    
    override fun onAROverlayAdded(overlay: AROverlay) {
        _arOverlays.value = _arOverlays.value + overlay
    }
    
    override fun onVRCalibrationComplete(data: VRCalibrationData) {
        saveCalibrationData(data)
    }
    
    override fun onImmersiveSessionEnded(result: ImmersiveSessionResult) {
        when (result) {
            is ImmersiveSessionResult.Success -> {
                // Log session stats
                logSessionStats(result.stats)
            }
            is ImmersiveSessionResult.Error -> {
                // Handle error
                showError(result.message)
            }
        }
        
        _immersiveState.value = _immersiveState.value.copy(
            isSessionActive = false,
            currentSessionId = null
        )
    }
    
    override fun onImmersiveError(error: String) {
        showError(error)
    }
    
    // Helper methods
    private fun checkImmersiveCapabilities(mediaItem: MediaItem) {
        // Check video metadata for immersive features
        val metadata = mediaItem.mediaMetadata
        
        if (metadata.extras?.containsKey("spherical") == true) {
            // Video supports 360° viewing
            _immersiveState.value = _immersiveState.value.copy(
                supports360Video = true
            )
        }
    }
    
    private fun getStoredIPD(): Float {
        // Retrieve stored interpupillary distance from preferences
        return context.getSharedPreferences("vr_settings", Context.MODE_PRIVATE)
            .getFloat("ipd", 63f)
    }
    
    private fun isEyeTrackingEnabled(): Boolean {
        return context.getSharedPreferences("vr_settings", Context.MODE_PRIVATE)
            .getBoolean("eye_tracking", false)
    }
    
    private fun saveCalibrationData(data: VRCalibrationData) {
        context.getSharedPreferences("vr_settings", Context.MODE_PRIVATE)
            .edit()
            .putFloat("ipd", data.ipd)
            .putFloat("screen_to_lens", data.screenToLensDistance)
            .apply()
    }
    
    private fun saveScreenshot(screenshot: ImmersiveScreenshot) {
        // Save screenshot to gallery
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val filename = "immersive_${screenshot.viewingMode.name}_${screenshot.timestamp}.jpg"
                // Implementation for saving to MediaStore
            } catch (e: Exception) {
                showError("Failed to save screenshot: ${e.message}")
            }
        }
    }
    
    private fun logSessionStats(stats: ImmersiveSessionStats) {
        // Log analytics
        Log.d("ImmersiveStats", """
            Session Duration: ${stats.duration}ms
            Viewing Mode: ${stats.viewingMode}
            Head Movements: ${stats.headMovements}
            Mode Changes: ${stats.modeChanges}
            Overlays Used: ${stats.overlaysUsed}
        """.trimIndent())
    }
    
    private fun showError(message: String) {
        // Show error to user
        viewModelScope.launch {
            _immersiveState.value = _immersiveState.value.copy(
                error = message
            )
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        player.release()
        viewModelScope.launch {
            currentSession?.let {
                immersiveMediaEngine.endImmersiveSession()
            }
        }
    }
}

// 12. Immersive State Data Class
data class ImmersiveState(
    val isInitialized: Boolean = false,
    val isSessionActive: Boolean = false,
    val currentSessionId: String? = null,
    val capabilities: ImmersiveCapabilities? = null,
    val supports360Video: Boolean = false,
    val error: String? = null
)

// 13. Integration with Main Video Player
@Composable
fun EnhancedVideoPlayerWithImmersive(
    uri: Uri,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val immersiveViewModel: ImmersiveVideoViewModel = hiltViewModel()
    
    LaunchedEffect(uri) {
        immersiveViewModel.loadVideo(uri)
    }
    
    ImmersiveVideoPlayerUI(
        viewModel = immersiveViewModel,
        modifier = modifier
    )
    
    // Back handler
    BackHandler {
        onBack()
    }
}

// 14. Hilt Module for Immersive Features
@Module
@InstallIn(SingletonComponent::class)
object ImmersiveModule {
    
    @Provides
    @Singleton
    fun provideImmersiveMediaEngine(
        @ApplicationContext context: Context,
        vrRenderer: VRRenderer,
        arOverlayManager: AROverlayManager,
        sphericalVideoProcessor: SphericalVideoProcessor,
        gyroscopeController: GyroscopeController,
        immersiveUIManager: ImmersiveUIManager
    ): ImmersiveMediaEngine {
        return ImmersiveMediaEngine(
            context = context,
            vrRenderer = vrRenderer,
            arOverlayManager = arOverlayManager,
            sphericalVideoProcessor = sphericalVideoProcessor,
            gyroscopeController = gyroscopeController,
            immersiveUIManager = immersiveUIManager
        )
    }
    
    @Provides
    @Singleton
    fun provideVRRenderer(
        @ApplicationContext context: Context
    ): VRRenderer {
        return VRRenderer(context)
    }
    
    @Provides
    @Singleton
    fun provideAROverlayManager(
        @ApplicationContext context: Context
    ): AROverlayManager {
        return AROverlayManager(context)
    }
    
    @Provides
    @Singleton
    fun provideSphericalVideoProcessor(
        @ApplicationContext context: Context
    ): SphericalVideoProcessor {
        return SphericalVideoProcessor(context)
    }
    
    @Provides
    @Singleton
    fun provideGyroscopeController(
        @ApplicationContext context: Context
    ): GyroscopeController {
        return GyroscopeController(context)
    }
    
    @Provides
    @Singleton
    fun provideImmersiveUIManager(
        @ApplicationContext context: Context
    ): ImmersiveUIManager {
        return ImmersiveUIManager(context)
    }
}

// 15. Dependencies to add to build.gradle
/*
dependencies {
    // OpenGL ES for 3D rendering
    implementation 'android.opengl:glu:1.0'
    
    // Google VR SDK (Cardboard)
    implementation 'com.google.vr:sdk-base:1.200.0'
    
    // ARCore for AR features
    implementation 'com.google.ar:core:1.44.0'
    
    // Quaternion math
    implementation 'org.apache.commons:commons-math3:3.6.1'
    
    // ExoPlayer with spherical video support
    implementation "androidx.media3:media3-exoplayer:$media3_version"
    implementation "androidx.media3:media3-exoplayer-dash:$media3_version"
    implementation "androidx.media3:media3-ui:$media3_version"
    
    // Sensor fusion for better tracking
    implementation 'com.github.googlevr:gvr-android-sdk:1.200.0'
}
*/