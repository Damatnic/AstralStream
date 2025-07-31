// ================================
// Professional Broadcasting Tools
// Live streaming, screen recording, multi-camera, real-time effects
// ================================

// 1. Professional Broadcasting Engine
@Singleton
class ProfessionalBroadcastingEngine @Inject constructor(
    private val context: Context,
    private val liveStreamingService: LiveStreamingService,
    private val screenRecordingService: ScreenRecordingService,
    private val multiCameraManager: MultiCameraManager,
    private val realTimeEffectsProcessor: RealTimeEffectsProcessor,
    private val broadcastingAnalytics: BroadcastingAnalyticsService
) {
    
    private var currentBroadcastSession: BroadcastSession? = null
    private var broadcastingCallbacks: BroadcastingCallbacks? = null
    
    suspend fun initializeBroadcasting(callbacks: BroadcastingCallbacks): Boolean {
        this.broadcastingCallbacks = callbacks
        
        return try {
            // Initialize all broadcasting components
            liveStreamingService.initialize()
            screenRecordingService.initialize()
            multiCameraManager.initialize()
            realTimeEffectsProcessor.initialize()
            
            callbacks.onBroadcastingInitialized()
            true
        } catch (e: Exception) {
            Log.e("Broadcasting", "Failed to initialize broadcasting engine", e)
            false
        }
    }
    
    suspend fun startLiveStream(
        streamConfig: LiveStreamConfiguration
    ): LiveStreamSession {
        return withContext(Dispatchers.Default) {
            val session = liveStreamingService.startStream(streamConfig)
            
            currentBroadcastSession = BroadcastSession(
                id = session.id,
                type = BroadcastType.LIVE_STREAM,
                startTime = System.currentTimeMillis(),
                configuration = streamConfig
            )
            
            broadcastingCallbacks?.onLiveStreamStarted(session)
            session
        }
    }
    
    suspend fun startScreenRecording(
        recordingConfig: ScreenRecordingConfiguration
    ): ScreenRecordingSession {
        return withContext(Dispatchers.Default) {
            val session = screenRecordingService.startRecording(recordingConfig)
            
            currentBroadcastSession = BroadcastSession(
                id = session.id,
                type = BroadcastType.SCREEN_RECORDING,
                startTime = System.currentTimeMillis(),
                configuration = recordingConfig
            )
            
            broadcastingCallbacks?.onScreenRecordingStarted(session)
            session
        }
    }
    
    suspend fun startMultiCameraSession(
        cameraConfig: MultiCameraConfiguration
    ): MultiCameraSession {
        return withContext(Dispatchers.Default) {
            val session = multiCameraManager.startMultiCameraSession(cameraConfig)
            
            currentBroadcastSession = BroadcastSession(
                id = session.id,
                type = BroadcastType.MULTI_CAMERA,
                startTime = System.currentTimeMillis(),
                configuration = cameraConfig
            )
            
            broadcastingCallbacks?.onMultiCameraSessionStarted(session)
            session
        }
    }
    
    suspend fun applyRealTimeEffect(effect: BroadcastEffect) {
        currentBroadcastSession?.let { session ->
            realTimeEffectsProcessor.applyEffect(session.id, effect)
            broadcastingCallbacks?.onEffectApplied(effect)
        }
    }
    
    suspend fun removeRealTimeEffect(effectId: String) {
        currentBroadcastSession?.let { session ->
            realTimeEffectsProcessor.removeEffect(session.id, effectId)
            broadcastingCallbacks?.onEffectRemoved(effectId)
        }
    }
    
    suspend fun switchCameraAngle(angleId: String) {
        if (currentBroadcastSession?.type == BroadcastType.MULTI_CAMERA) {
            multiCameraManager.switchToAngle(angleId)
            broadcastingCallbacks?.onCameraAngleSwitched(angleId)
        }
    }
    
    suspend fun addOverlay(overlay: BroadcastOverlay) {
        currentBroadcastSession?.let { session ->
            when (session.type) {
                BroadcastType.LIVE_STREAM -> liveStreamingService.addOverlay(session.id, overlay)
                BroadcastType.SCREEN_RECORDING -> screenRecordingService.addOverlay(session.id, overlay)
                BroadcastType.MULTI_CAMERA -> multiCameraManager.addOverlay(session.id, overlay)
            }
            broadcastingCallbacks?.onOverlayAdded(overlay)
        }
    }
    
    suspend fun updateStreamSettings(settings: LiveStreamSettings) {
        if (currentBroadcastSession?.type == BroadcastType.LIVE_STREAM) {
            liveStreamingService.updateSettings(settings)
            broadcastingCallbacks?.onStreamSettingsUpdated(settings)
        }
    }
    
    suspend fun pauseBroadcast() {
        currentBroadcastSession?.let { session ->
            when (session.type) {
                BroadcastType.LIVE_STREAM -> liveStreamingService.pauseStream(session.id)
                BroadcastType.SCREEN_RECORDING -> screenRecordingService.pauseRecording(session.id)
                BroadcastType.MULTI_CAMERA -> multiCameraManager.pauseSession(session.id)
            }
            broadcastingCallbacks?.onBroadcastPaused()
        }
    }
    
    suspend fun resumeBroadcast() {
        currentBroadcastSession?.let { session ->
            when (session.type) {
                BroadcastType.LIVE_STREAM -> liveStreamingService.resumeStream(session.id)
                BroadcastType.SCREEN_RECORDING -> screenRecordingService.resumeRecording(session.id)
                BroadcastType.MULTI_CAMERA -> multiCameraManager.resumeSession(session.id)
            }
            broadcastingCallbacks?.onBroadcastResumed()
        }
    }
    
    suspend fun stopBroadcast(): BroadcastResult {
        return currentBroadcastSession?.let { session ->
            val result = when (session.type) {
                BroadcastType.LIVE_STREAM -> liveStreamingService.stopStream(session.id)
                BroadcastType.SCREEN_RECORDING -> screenRecordingService.stopRecording(session.id)
                BroadcastType.MULTI_CAMERA -> multiCameraManager.stopSession(session.id)
            }
            
            // Generate analytics
            val analytics = broadcastingAnalytics.generateSessionAnalytics(session)
            
            currentBroadcastSession = null
            broadcastingCallbacks?.onBroadcastStopped(result, analytics)
            
            result
        } ?: BroadcastResult.Error("No active broadcast session")
    }
    
    fun getBroadcastStatus(): BroadcastStatus {
        return currentBroadcastSession?.let { session ->
            BroadcastStatus(
                isActive = true,
                sessionId = session.id,
                type = session.type,
                duration = System.currentTimeMillis() - session.startTime,
                viewerCount = getCurrentViewerCount(),
                bitrate = getCurrentBitrate(),
                networkQuality = getCurrentNetworkQuality()
            )
        } ?: BroadcastStatus()
    }
    
    private fun getCurrentViewerCount(): Int {
        return currentBroadcastSession?.let { session ->
            when (session.type) {
                BroadcastType.LIVE_STREAM -> liveStreamingService.getViewerCount(session.id)
                else -> 0
            }
        } ?: 0
    }
    
    private fun getCurrentBitrate(): Int {
        return currentBroadcastSession?.let { session ->
            when (session.type) {
                BroadcastType.LIVE_STREAM -> liveStreamingService.getCurrentBitrate(session.id)
                BroadcastType.SCREEN_RECORDING -> screenRecordingService.getCurrentBitrate(session.id)
                BroadcastType.MULTI_CAMERA -> multiCameraManager.getCurrentBitrate(session.id)
            }
        } ?: 0
    }
    
    private fun getCurrentNetworkQuality(): NetworkQuality {
        return NetworkQuality.GOOD // Placeholder - would integrate with network monitoring
    }
}

// 2. Live Streaming Service
@Singleton
class LiveStreamingService @Inject constructor(
    private val context: Context,
    private val rtmpClient: RTMPClient,
    private val audioEncoder: AudioEncoder,
    private val videoEncoder: VideoEncoder
) {
    
    private val activeStreams = mutableMapOf<String, ActiveStreamSession>()
    
    suspend fun initialize(): Boolean = withContext(Dispatchers.IO) {
        try {
            rtmpClient.initialize()
            audioEncoder.initialize()
            videoEncoder.initialize()
            Log.i("LiveStreaming", "Live streaming service initialized")
            true
        } catch (e: Exception) {
            Log.e("LiveStreaming", "Failed to initialize live streaming", e)
            false
        }
    }
    
    suspend fun startStream(config: LiveStreamConfiguration): LiveStreamSession {
        return withContext(Dispatchers.Default) {
            val sessionId = UUID.randomUUID().toString()
            
            // Setup encoders
            val videoEncoderConfig = VideoEncoderConfiguration(
                width = config.resolution.width,
                height = config.resolution.height,
                bitrate = config.bitrate,
                frameRate = config.frameRate,
                keyFrameInterval = config.keyFrameInterval
            )
            
            val audioEncoderConfig = AudioEncoderConfiguration(
                sampleRate = config.audioSampleRate,
                bitrate = config.audioBitrate,
                channels = config.audioChannels
            )
            
            // Start encoding
            videoEncoder.startEncoding(sessionId, videoEncoderConfig)
            audioEncoder.startEncoding(sessionId, audioEncoderConfig)
            
            // Connect to streaming server
            val connectionResult = rtmpClient.connect(config.rtmpUrl, config.streamKey)
            
            if (connectionResult.isSuccess) {
                val session = LiveStreamSession(
                    id = sessionId,
                    configuration = config,
                    startTime = System.currentTimeMillis(),
                    status = StreamStatus.LIVE,
                    viewerCount = 0
                )
                
                activeStreams[sessionId] = ActiveStreamSession(
                    session = session,
                    rtmpConnection = connectionResult.connection,
                    videoEncoder = videoEncoder,
                    audioEncoder = audioEncoder
                )
                
                // Start streaming pipeline
                startStreamingPipeline(sessionId)
                
                session
            } else {
                throw Exception("Failed to connect to streaming server: ${connectionResult.error}")
            }
        }
    }
    
    private suspend fun startStreamingPipeline(sessionId: String) {
        val streamSession = activeStreams[sessionId] ?: return
        
        // Start video streaming coroutine
        CoroutineScope(Dispatchers.Default).launch {
            streamSession.videoEncoder.getEncodedFrames(sessionId).collect { encodedFrame ->
                streamSession.rtmpConnection?.sendVideoFrame(encodedFrame)
            }
        }
        
        // Start audio streaming coroutine
        CoroutineScope(Dispatchers.Default).launch {
            streamSession.audioEncoder.getEncodedAudio(sessionId).collect { encodedAudio ->
                streamSession.rtmpConnection?.sendAudioFrame(encodedAudio)
            }
        }
    }
    
    suspend fun updateSettings(settings: LiveStreamSettings) {
        activeStreams.values.forEach { streamSession ->
            // Update bitrate
            if (settings.adaptiveBitrate) {
                val optimalBitrate = calculateOptimalBitrate(settings.networkCondition)
                streamSession.videoEncoder.updateBitrate(streamSession.session.id, optimalBitrate)
            }
            
            // Update frame rate
            streamSession.videoEncoder.updateFrameRate(streamSession.session.id, settings.frameRate)
        }
    }
    
    suspend fun addOverlay(sessionId: String, overlay: BroadcastOverlay) {
        val streamSession = activeStreams[sessionId] ?: return
        streamSession.videoEncoder.addOverlay(sessionId, overlay)
    }
    
    suspend fun pauseStream(sessionId: String) {
        val streamSession = activeStreams[sessionId] ?: return
        streamSession.videoEncoder.pauseEncoding(sessionId)
        streamSession.audioEncoder.pauseEncoding(sessionId)
        streamSession.session.status = StreamStatus.PAUSED
    }
    
    suspend fun resumeStream(sessionId: String) {
        val streamSession = activeStreams[sessionId] ?: return
        streamSession.videoEncoder.resumeEncoding(sessionId)
        streamSession.audioEncoder.resumeEncoding(sessionId)
        streamSession.session.status = StreamStatus.LIVE
    }
    
    suspend fun stopStream(sessionId: String): BroadcastResult {
        return withContext(Dispatchers.Default) {
            val streamSession = activeStreams[sessionId]
            
            if (streamSession != null) {
                try {
                    // Stop encoders
                    streamSession.videoEncoder.stopEncoding(sessionId)
                    streamSession.audioEncoder.stopEncoding(sessionId)
                    
                    // Disconnect from server
                    streamSession.rtmpConnection?.disconnect()
                    
                    // Remove from active streams
                    activeStreams.remove(sessionId)
                    
                    BroadcastResult.Success(
                        "Stream stopped successfully",
                        streamSession.session.copy(
                            status = StreamStatus.ENDED,
                            endTime = System.currentTimeMillis()
                        )
                    )
                } catch (e: Exception) {
                    BroadcastResult.Error("Failed to stop stream: ${e.message}")
                }
            } else {
                BroadcastResult.Error("Stream session not found")
            }
        }
    }
    
    fun getViewerCount(sessionId: String): Int {
        return activeStreams[sessionId]?.rtmpConnection?.getViewerCount() ?: 0
    }
    
    fun getCurrentBitrate(sessionId: String): Int {
        return activeStreams[sessionId]?.videoEncoder?.getCurrentBitrate(sessionId) ?: 0
    }
    
    private fun calculateOptimalBitrate(networkCondition: NetworkCondition): Int {
        return when (networkCondition.quality) {
            NetworkQuality.POOR -> 500_000 // 500 Kbps
            NetworkQuality.GOOD -> 2_500_000 // 2.5 Mbps
            NetworkQuality.EXCELLENT -> 6_000_000 // 6 Mbps
        }
    }
}

// 3. Screen Recording Service
@Singleton
class ScreenRecordingService @Inject constructor(
    private val context: Context
) {
    
    private val activeRecordings = mutableMapOf<String, ScreenRecordingState>()
    private var mediaProjection: MediaProjection? = null
    private var mediaProjectionManager: MediaProjectionManager? = null
    
    suspend fun initialize(): Boolean = withContext(Dispatchers.IO) {
        try {
            mediaProjectionManager = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            Log.i("ScreenRecording", "Screen recording service initialized")
            true
        } catch (e: Exception) {
            Log.e("ScreenRecording", "Failed to initialize screen recording", e)
            false
        }
    }
    
    suspend fun requestRecordingPermission(): Intent? {
        return mediaProjectionManager?.createScreenCaptureIntent()
    }
    
    suspend fun startRecording(config: ScreenRecordingConfiguration): ScreenRecordingSession {
        return withContext(Dispatchers.Default) {
            val sessionId = UUID.randomUUID().toString()
            
            if (mediaProjection == null) {
                throw IllegalStateException("Media projection not initialized. Call requestRecordingPermission first.")
            }
            
            // Setup media recorder
            val mediaRecorder = MediaRecorder().apply {
                setVideoSource(MediaRecorder.VideoSource.SURFACE)
                if (config.includeAudio) {
                    setAudioSource(MediaRecorder.AudioSource.MIC)
                }
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setVideoEncoder(MediaRecorder.VideoEncoder.H264)
                if (config.includeAudio) {
                    setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                }
                setVideoSize(config.resolution.width, config.resolution.height)
                setVideoFrameRate(config.frameRate)
                setVideoBitRate(config.bitrate)
                if (config.includeAudio) {
                    setAudioBitRate(config.audioBitrate)
                    setAudioSamplingRate(config.audioSampleRate)
                }
                setOutputFile(generateOutputFilePath(sessionId, config))
            }
            
            // Create virtual display
            val virtualDisplay = mediaProjection?.createVirtualDisplay(
                "AstralStreamRecording",
                config.resolution.width,
                config.resolution.height,
                context.resources.displayMetrics.densityDpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                mediaRecorder.surface,
                null,
                null
            )
            
            // Start recording
            mediaRecorder.prepare()
            mediaRecorder.start()
            
            val session = ScreenRecordingSession(
                id = sessionId,
                configuration = config,
                startTime = System.currentTimeMillis(),
                outputFile = generateOutputFilePath(sessionId, config),
                status = RecordingStatus.RECORDING
            )
            
            activeRecordings[sessionId] = ScreenRecordingState(
                session = session,
                mediaRecorder = mediaRecorder,
                virtualDisplay = virtualDisplay
            )
            
            session
        }
    }
    
    suspend fun addOverlay(sessionId: String, overlay: BroadcastOverlay) {
        // Add overlay to screen recording
        // This would require a custom surface renderer
    }
    
    suspend fun pauseRecording(sessionId: String) {
        val recordingState = activeRecordings[sessionId] ?: return
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            recordingState.mediaRecorder.pause()
            recordingState.session.status = RecordingStatus.PAUSED
        }
    }
    
    suspend fun resumeRecording(sessionId: String) {
        val recordingState = activeRecordings[sessionId] ?: return
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            recordingState.mediaRecorder.resume()
            recordingState.session.status = RecordingStatus.RECORDING
        }
    }
    
    suspend fun stopRecording(sessionId: String): BroadcastResult {
        return withContext(Dispatchers.Default) {
            val recordingState = activeRecordings[sessionId]
            
            if (recordingState != null) {
                try {
                    // Stop recording
                    recordingState.mediaRecorder.stop()
                    recordingState.mediaRecorder.release()
                    recordingState.virtualDisplay?.release()
                    
                    // Update session
                    recordingState.session.status = RecordingStatus.COMPLETED
                    recordingState.session.endTime = System.currentTimeMillis()
                    
                    // Remove from active recordings
                    activeRecordings.remove(sessionId)
                    
                    BroadcastResult.Success(
                        "Recording completed successfully",
                        recordingState.session
                    )
                } catch (e: Exception) {
                    BroadcastResult.Error("Failed to stop recording: ${e.message}")
                }
            } else {
                BroadcastResult.Error("Recording session not found")
            }
        }
    }
    
    fun getCurrentBitrate(sessionId: String): Int {
        return activeRecordings[sessionId]?.session?.configuration?.bitrate ?: 0
    }
    
    fun setMediaProjection(projection: MediaProjection) {
        this.mediaProjection = projection
    }
    
    private fun generateOutputFilePath(sessionId: String, config: ScreenRecordingConfiguration): String {
        val recordingsDir = File(context.getExternalFilesDir(Environment.DIRECTORY_MOVIES), "ScreenRecordings")
        if (!recordingsDir.exists()) {
            recordingsDir.mkdirs()
        }
        
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "recording_${timestamp}_${sessionId}.mp4"
        
        return File(recordingsDir, fileName).absolutePath
    }
}

// 4. Multi-Camera Manager
@Singleton
class MultiCameraManager @Inject constructor(
    private val context: Context
) {
    
    private val activeSessions = mutableMapOf<String, MultiCameraState>()
    private val availableCameras = mutableListOf<CameraInfo>()
    
    suspend fun initialize(): Boolean = withContext(Dispatchers.IO) {
        try {
            discoverAvailableCameras()
            Log.i("MultiCamera", "Multi-camera manager initialized with ${availableCameras.size} cameras")
            true
        } catch (e: Exception) {
            Log.e("MultiCamera", "Failed to initialize multi-camera manager", e)
            false
        }
    }
    
    private suspend fun discoverAvailableCameras() {
        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        
        try {
            cameraManager.cameraIdList.forEach { cameraId ->
                val characteristics = cameraManager.getCameraCharacteristics(cameraId)
                val facing = characteristics.get(CameraCharacteristics.LENS_FACING)
                val supportedSizes = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                    ?.getOutputSizes(MediaRecorder::class.java)
                
                availableCameras.add(
                    CameraInfo(
                        id = cameraId,
                        name = when (facing) {
                            CameraCharacteristics.LENS_FACING_FRONT -> "Front Camera"
                            CameraCharacteristics.LENS_FACING_BACK -> "Back Camera"
                            CameraCharacteristics.LENS_FACING_EXTERNAL -> "External Camera $cameraId"
                            else -> "Camera $cameraId"
                        },
                        facing = facing ?: CameraCharacteristics.LENS_FACING_BACK,
                        supportedResolutions = supportedSizes?.map { 
                            Resolution(it.width, it.height) 
                        } ?: emptyList()
                    )
                )
            }
        } catch (e: Exception) {
            Log.e("MultiCamera", "Failed to discover cameras", e)
        }
    }
    
    suspend fun startMultiCameraSession(config: MultiCameraConfiguration): MultiCameraSession {
        return withContext(Dispatchers.Default) {
            val sessionId = UUID.randomUUID().toString()
            
            // Validate camera configuration
            val availableCameraIds = availableCameras.map { it.id }
            val requestedCameras = config.cameraAngles.filter { it.cameraId in availableCameraIds }
            
            if (requestedCameras.isEmpty()) {
                throw IllegalArgumentException("No valid cameras found in configuration")
            }
            
            // Setup camera sessions
            val cameraSessionsFlow = requestedCameras.map { angle ->
                setupCameraSession(angle)
            }
            
            val session = MultiCameraSession(
                id = sessionId,
                configuration = config,
                availableAngles = requestedCameras,
                currentAngleId = requestedCameras.first().id,
                startTime = System.currentTimeMillis(),
                status = MultiCameraStatus.ACTIVE
            )
            
            activeSessions[sessionId] = MultiCameraState(
                session = session,
                cameraStreams = cameraSessionsFlow.toMutableList(),
                activeAngleIndex = 0
            )
            
            session
        }
    }
    
    private suspend fun setupCameraSession(angle: CameraAngle): CameraStream {
        // Setup individual camera stream
        return CameraStream(
            angleId = angle.id,
            cameraId = angle.cameraId,
            resolution = angle.resolution,
            // Additional camera setup would go here
        )
    }
    
    suspend fun switchToAngle(angleId: String) {
        activeSessions.values.forEach { state ->
            val angleIndex = state.session.availableAngles.indexOfFirst { it.id == angleId }
            if (angleIndex != -1) {
                state.activeAngleIndex = angleIndex
                state.session.currentAngleId = angleId
            }
        }
    }
    
    suspend fun addOverlay(sessionId: String, overlay: BroadcastOverlay) {
        val sessionState = activeSessions[sessionId] ?: return
        val activeStream = sessionState.cameraStreams[sessionState.activeAngleIndex]
        activeStream.addOverlay(overlay)
    }
    
    suspend fun pauseSession(sessionId: String) {
        val sessionState = activeSessions[sessionId] ?: return
        sessionState.cameraStreams.forEach { it.pause() }
        sessionState.session.status = MultiCameraStatus.PAUSED
    }
    
    suspend fun resumeSession(sessionId: String) {
        val sessionState = activeSessions[sessionId] ?: return
        sessionState.cameraStreams.forEach { it.resume() }
        sessionState.session.status = MultiCameraStatus.ACTIVE
    }
    
    suspend fun stopSession(sessionId: String): BroadcastResult {
        return withContext(Dispatchers.Default) {
            val sessionState = activeSessions[sessionId]
            
            if (sessionState != null) {
                try {
                    // Stop all camera streams
                    sessionState.cameraStreams.forEach { it.stop() }
                    
                    // Update session
                    sessionState.session.status = MultiCameraStatus.ENDED
                    sessionState.session.endTime = System.currentTimeMillis()
                    
                    // Remove from active sessions
                    activeSessions.remove(sessionId)
                    
                    BroadcastResult.Success(
                        "Multi-camera session stopped successfully",
                        sessionState.session
                    )
                } catch (e: Exception) {
                    BroadcastResult.Error("Failed to stop multi-camera session: ${e.message}")
                }
            } else {
                BroadcastResult.Error("Multi-camera session not found")
            }
        }
    }
    
    fun getCurrentBitrate(sessionId: String): Int {
        val sessionState = activeSessions[sessionId] ?: return 0
        val activeStream = sessionState.cameraStreams.getOrNull(sessionState.activeAngleIndex)
        return activeStream?.getCurrentBitrate() ?: 0
    }
    
    fun getAvailableCameras(): List<CameraInfo> = availableCameras.toList()
}

// 5. Real-Time Effects Processor
@Singleton
class RealTimeEffectsProcessor @Inject constructor() {
    
    private val activeEffects = mutableMapOf<String, MutableList<BroadcastEffect>>()
    
    fun initialize() {
        // Initialize effects processing
    }
    
    suspend fun applyEffect(sessionId: String, effect: BroadcastEffect) {
        val effects = activeEffects.getOrPut(sessionId) { mutableListOf() }
        effects.add(effect)
        
        // Apply effect to the video stream
        when (effect.type) {
            EffectType.BLUR -> applyBlurEffect(sessionI