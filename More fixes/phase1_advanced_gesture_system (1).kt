// ================================
// Advanced Gesture System 2.0
// Fully customizable multi-finger gestures with haptic feedback
// ================================

// 1. Advanced Gesture Manager
@Singleton
class AdvancedGestureManager @Inject constructor(
    private val context: Context,
    private val hapticFeedbackManager: HapticFeedbackManager,
    private val gestureCustomizationRepository: GestureCustomizationRepository,
    private val gestureRecorder: GestureRecorder
) {
    
    private var gestureCallbacks: AdvancedGestureCallbacks? = null
    private var customGestures = mutableMapOf<String, CustomGesture>()
    private var isGestureRecordingMode = false
    
    private val gestureDetector = MultiTouchGestureDetector()
    private val voiceGestureDetector = VoiceGestureDetector(context)
    
    init {
        loadCustomGestures()
    }
    
    fun attachToView(view: View, callbacks: AdvancedGestureCallbacks) {
        this.gestureCallbacks = callbacks
        
        view.setOnTouchListener { _, event ->
            handleTouchEvent(event)
        }
        
        // Initialize voice commands if enabled
        if (gestureCustomizationRepository.isVoiceGesturesEnabled()) {
            voiceGestureDetector.startListening { voiceCommand ->
                handleVoiceGesture(voiceCommand)
            }
        }
    }
    
    private fun handleTouchEvent(event: MotionEvent): Boolean {
        val gestureResult = gestureDetector.detectGesture(event)
        
        return when (gestureResult) {
            is GestureResult.Recognized -> {
                executeGesture(gestureResult.gesture, gestureResult.data)
                true
            }
            is GestureResult.Recording -> {
                if (isGestureRecordingMode) {
                    gestureRecorder.recordGesturePoint(gestureResult.point)
                }
                true
            }
            is GestureResult.NotRecognized -> false
        }
    }
    
    private fun executeGesture(gesture: GestureType, data: GestureData) {
        // Check for custom gesture mapping
        val customAction = customGestures[gesture.name]?.action
        if (customAction != null) {
            executeCustomAction(customAction, data)
            return
        }
        
        // Execute default gesture
        when (gesture) {
            GestureType.SINGLE_TAP -> {
                hapticFeedbackManager.lightTap()
                gestureCallbacks?.onSingleTap(data.position)
            }
            
            GestureType.DOUBLE_TAP -> {
                hapticFeedbackManager.doubleTap()
                val seekAmount = getCustomSeekAmount(data.zone)
                gestureCallbacks?.onDoubleTapSeek(data.zone, seekAmount)
            }
            
            GestureType.LONG_PRESS -> {
                hapticFeedbackManager.longPress()
                gestureCallbacks?.onLongPressSpeedControl(data.position, data.pressure)
            }
            
            GestureType.SWIPE_HORIZONTAL -> {
                hapticFeedbackManager.swipe()
                val seekAmount = calculateSeekAmount(data.distance, data.velocity)
                gestureCallbacks?.onSeek(seekAmount)
            }
            
            GestureType.SWIPE_VERTICAL_LEFT -> {
                hapticFeedbackManager.adjustment()
                gestureCallbacks?.onBrightnessAdjust(data.deltaY)
            }
            
            GestureType.SWIPE_VERTICAL_RIGHT -> {
                hapticFeedbackManager.adjustment()
                gestureCallbacks?.onVolumeAdjust(data.deltaY)
            }
            
            GestureType.PINCH_ZOOM -> {
                gestureCallbacks?.onZoom(data.scaleFactor)
            }
            
            GestureType.TWO_FINGER_ROTATE -> {
                hapticFeedbackManager.lightTap()
                gestureCallbacks?.onRotate(data.rotationAngle)
            }
            
            GestureType.THREE_FINGER_SWIPE_LEFT -> {
                hapticFeedbackManager.success()
                gestureCallbacks?.onPreviousVideo()
            }
            
            GestureType.THREE_FINGER_SWIPE_RIGHT -> {
                hapticFeedbackManager.success()
                gestureCallbacks?.onNextVideo()
            }
            
            GestureType.THREE_FINGER_TAP -> {
                hapticFeedbackManager.notification()
                gestureCallbacks?.onTogglePlaylist()
            }
            
            GestureType.FOUR_FINGER_TAP -> {
                hapticFeedbackManager.warning()
                gestureCallbacks?.onToggleFullscreen()
            }
            
            GestureType.EDGE_SWIPE_FROM_LEFT -> {
                hapticFeedbackManager.lightTap()
                gestureCallbacks?.onShowSidebar()
            }
            
            GestureType.EDGE_SWIPE_FROM_RIGHT -> {
                hapticFeedbackManager.lightTap()
                gestureCallbacks?.onShowQuickSettings()
            }
            
            GestureType.CUSTOM -> {
                // Handle custom recorded gestures
                handleCustomGesture(data.customGestureId)
            }
        }
    }
    
    private fun executeCustomAction(action: GestureAction, data: GestureData) {
        hapticFeedbackManager.customFeedback(action.hapticPattern)
        
        when (action) {
            is GestureAction.PlayPause -> gestureCallbacks?.onPlayPause()
            is GestureAction.SeekTo -> gestureCallbacks?.onSeekTo(action.positionMs)
            is GestureAction.ChangeSpeed -> gestureCallbacks?.onSpeedChange(action.speed)
            is GestureAction.ToggleSubtitles -> gestureCallbacks?.onToggleSubtitles()
            is GestureAction.SwitchAudioTrack -> gestureCallbacks?.onSwitchAudioTrack()
            is GestureAction.TakeScreenshot -> gestureCallbacks?.onTakeScreenshot()
            is GestureAction.OpenSettings -> gestureCallbacks?.onOpenSettings()
            is GestureAction.Custom -> executeCustomScript(action.script)
        }
    }
    
    fun startGestureRecording(gestureName: String) {
        isGestureRecordingMode = true
        gestureRecorder.startRecording(gestureName)
        hapticFeedbackManager.recordingStart()
    }
    
    fun stopGestureRecording(): CustomGesture? {
        isGestureRecordingMode = false
        val recordedGesture = gestureRecorder.stopRecording()
        hapticFeedbackManager.recordingStop()
        
        recordedGesture?.let { gesture ->
            customGestures[gesture.name] = gesture
            gestureCustomizationRepository.saveCustomGesture(gesture)
        }
        
        return recordedGesture
    }
    
    fun getAvailableGestures(): List<GestureDefinition> {
        return GestureType.values().map { type ->
            GestureDefinition(
                type = type,
                name = type.displayName,
                description = type.description,
                isCustomizable = type.isCustomizable,
                currentAction = customGestures[type.name]?.action
            )
        }
    }
    
    fun customizeGesture(gestureType: GestureType, action: GestureAction) {
        val customGesture = CustomGesture(
            id = UUID.randomUUID().toString(),
            name = gestureType.name,
            gestureType = gestureType,
            action = action,
            createdAt = System.currentTimeMillis()
        )
        
        customGestures[gestureType.name] = customGesture
        gestureCustomizationRepository.saveCustomGesture(customGesture)
    }
    
    private fun loadCustomGestures() {
        customGestures.putAll(
            gestureCustomizationRepository.getCustomGestures().associateBy { it.name }
        )
    }
    
    private fun handleVoiceGesture(command: VoiceCommand) {
        hapticFeedbackManager.voiceCommandReceived()
        
        when (command.action) {
            "play" -> gestureCallbacks?.onPlayPause()
            "pause" -> gestureCallbacks?.onPlayPause()
            "next" -> gestureCallbacks?.onNextVideo()
            "previous" -> gestureCallbacks?.onPreviousVideo()
            "volume up" -> gestureCallbacks?.onVolumeAdjust(0.1f)
            "volume down" -> gestureCallbacks?.onVolumeAdjust(-0.1f)
            "faster" -> gestureCallbacks?.onSpeedChange(1.25f)
            "slower" -> gestureCallbacks?.onSpeedChange(0.75f)
            "screenshot" -> gestureCallbacks?.onTakeScreenshot()
        }
    }
    
    private fun calculateSeekAmount(distance: Float, velocity: Float): Long {
        val baseSeekAmount = (distance / 100f) * 10000L // 10 seconds per 100 pixels
        val velocityMultiplier = (velocity / 1000f).coerceIn(0.5f, 3.0f)
        return (baseSeekAmount * velocityMultiplier).toLong()
    }
    
    private fun getCustomSeekAmount(zone: TouchZone): Long {
        return gestureCustomizationRepository.getDoubleTapSeekAmount(zone)
    }
    
    private fun handleCustomGesture(gestureId: String?) {
        gestureId?.let { id ->
            val customGesture = customGestures.values.find { it.id == id }
            customGesture?.action?.let { action ->
                executeCustomAction(action, GestureData())
            }
        }
    }
    
    private fun executeCustomScript(script: String) {
        // Execute custom Lua/JavaScript for advanced automation
        // Placeholder for scripting engine integration
    }
}

// 2. Multi-Touch Gesture Detector
class MultiTouchGestureDetector {
    
    private var gestureStartTime = 0L
    private var lastTouchEvent: MotionEvent? = null
    private val touchPoints = mutableListOf<TouchPoint>()
    private var currentGestureState = GestureState.IDLE
    
    fun detectGesture(event: MotionEvent): GestureResult {
        val currentTime = System.currentTimeMillis()
        
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                gestureStartTime = currentTime
                touchPoints.clear()
                touchPoints.add(TouchPoint(event.x, event.y, currentTime))
                currentGestureState = GestureState.TOUCH_DOWN
            }
            
            MotionEvent.ACTION_POINTER_DOWN -> {
                val pointerIndex = event.actionIndex
                touchPoints.add(TouchPoint(
                    event.getX(pointerIndex), 
                    event.getY(pointerIndex), 
                    currentTime
                ))
                currentGestureState = when (event.pointerCount) {
                    2 -> GestureState.TWO_FINGER
                    3 -> GestureState.THREE_FINGER
                    4 -> GestureState.FOUR_FINGER
                    else -> GestureState.MULTI_FINGER
                }
            }
            
            MotionEvent.ACTION_MOVE -> {
                updateTouchPoints(event, currentTime)
                return analyzeMovement(event, currentTime)
            }
            
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                val result = finalizeGesture(event, currentTime)
                if (event.actionMasked == MotionEvent.ACTION_UP) {
                    currentGestureState = GestureState.IDLE
                    touchPoints.clear()
                }
                return result
            }
        }
        
        lastTouchEvent = MotionEvent.obtain(event)
        return GestureResult.NotRecognized
    }
    
    private fun analyzeMovement(event: MotionEvent, currentTime: Long): GestureResult {
        val duration = currentTime - gestureStartTime
        
        return when (currentGestureState) {
            GestureState.TOUCH_DOWN -> {
                if (duration > LONG_PRESS_THRESHOLD) {
                    recognizeLongPress(event)
                } else {
                    analyzeSingleFingerMovement(event)
                }
            }
            
            GestureState.TWO_FINGER -> analyzeTwoFingerGesture(event)
            GestureState.THREE_FINGER -> analyzeThreeFingerGesture(event)
            GestureState.FOUR_FINGER -> analyzeFourFingerGesture(event)
            else -> GestureResult.NotRecognized
        }
    }
    
    private fun analyzeSingleFingerMovement(event: MotionEvent): GestureResult {
        if (touchPoints.isEmpty()) return GestureResult.NotRecognized
        
        val startPoint = touchPoints.first()
        val deltaX = event.x - startPoint.x
        val deltaY = event.y - startPoint.y
        val distance = sqrt(deltaX * deltaX + deltaY * deltaY)
        
        if (distance < MOVEMENT_THRESHOLD) {
            return GestureResult.NotRecognized
        }
        
        val velocity = calculateVelocity(event)
        
        return when {
            abs(deltaX) > abs(deltaY) -> {
                // Horizontal swipe
                GestureResult.Recognized(
                    GestureType.SWIPE_HORIZONTAL,
                    GestureData(
                        position = PointF(event.x, event.y),
                        distance = distance,
                        velocity = velocity,
                        deltaX = deltaX,
                        deltaY = deltaY
                    )
                )
            }
            
            event.x < 200 -> {
                // Left edge vertical swipe (brightness)
                GestureResult.Recognized(
                    GestureType.SWIPE_VERTICAL_LEFT,
                    GestureData(
                        position = PointF(event.x, event.y),
                        deltaY = deltaY,
                        velocity = velocity
                    )
                )
            }
            
            event.x > Resources.getSystem().displayMetrics.widthPixels - 200 -> {
                // Right edge vertical swipe (volume)
                GestureResult.Recognized(
                    GestureType.SWIPE_VERTICAL_RIGHT,
                    GestureData(
                        position = PointF(event.x, event.y),
                        deltaY = deltaY,
                        velocity = velocity
                    )
                )
            }
            
            else -> GestureResult.NotRecognized
        }
    }
    
    private fun analyzeTwoFingerGesture(event: MotionEvent): GestureResult {
        if (event.pointerCount < 2) return GestureResult.NotRecognized
        
        val finger1 = PointF(event.getX(0), event.getY(0))
        val finger2 = PointF(event.getX(1), event.getY(1))
        
        // Calculate distance between fingers
        val currentDistance = distance(finger1, finger2)
        
        // Calculate rotation
        val currentAngle = angle(finger1, finger2)
        
        // Check if this is pinch/zoom
        if (touchPoints.size >= 2) {
            val initialDistance = distance(
                PointF(touchPoints[0].x, touchPoints[0].y),
                PointF(touchPoints[1].x, touchPoints[1].y)
            )
            
            val scaleFactor = currentDistance / initialDistance
            
            if (abs(scaleFactor - 1.0f) > 0.1f) {
                return GestureResult.Recognized(
                    GestureType.PINCH_ZOOM,
                    GestureData(scaleFactor = scaleFactor)
                )
            }
        }
        
        // Check for rotation
        if (touchPoints.size >= 2) {
            val initialAngle = angle(
                PointF(touchPoints[0].x, touchPoints[0].y),
                PointF(touchPoints[1].x, touchPoints[1].y)
            )
            
            val rotationAngle = currentAngle - initialAngle
            
            if (abs(rotationAngle) > 15f) {
                return GestureResult.Recognized(
                    GestureType.TWO_FINGER_ROTATE,
                    GestureData(rotationAngle = rotationAngle)
                )
            }
        }
        
        return GestureResult.NotRecognized
    }
    
    private fun analyzeThreeFingerGesture(event: MotionEvent): GestureResult {
        if (event.pointerCount < 3) return GestureResult.NotRecognized
        
        // Calculate center point of three fingers
        val centerX = (0 until 3).map { event.getX(it) }.average().toFloat()
        val centerY = (0 until 3).map { event.getY(it) }.average().toFloat()
        
        if (touchPoints.size >= 3) {
            val initialCenterX = touchPoints.take(3).map { it.x }.average().toFloat()
            val initialCenterY = touchPoints.take(3).map { it.y }.average().toFloat()
            
            val deltaX = centerX - initialCenterX
            val deltaY = centerY - initialCenterY
            val distance = sqrt(deltaX * deltaX + deltaY * deltaY)
            
            if (distance > MOVEMENT_THRESHOLD) {
                return when {
                    abs(deltaX) > abs(deltaY) && deltaX < 0 -> {
                        GestureResult.Recognized(GestureType.THREE_FINGER_SWIPE_LEFT, GestureData())
                    }
                    abs(deltaX) > abs(deltaY) && deltaX > 0 -> {
                        GestureResult.Recognized(GestureType.THREE_FINGER_SWIPE_RIGHT, GestureData())
                    }
                    else -> GestureResult.NotRecognized
                }
            }
        }
        
        return GestureResult.NotRecognized
    }
    
    private fun analyzeFourFingerGesture(event: MotionEvent): GestureResult {
        // Four finger gestures are typically for advanced controls
        return GestureResult.NotRecognized
    }
    
    private fun finalizeGesture(event: MotionEvent, currentTime: Long): GestureResult {
        val duration = currentTime - gestureStartTime
        
        return when (currentGestureState) {
            GestureState.TOUCH_DOWN -> {
                if (duration < TAP_THRESHOLD) {
                    recognizeTap(event, duration)
                } else {
                    GestureResult.NotRecognized
                }
            }
            
            GestureState.THREE_FINGER -> {
                if (duration < TAP_THRESHOLD) {
                    GestureResult.Recognized(GestureType.THREE_FINGER_TAP, GestureData())
                } else {
                    GestureResult.NotRecognized
                }
            }
            
            GestureState.FOUR_FINGER -> {
                if (duration < TAP_THRESHOLD) {
                    GestureResult.Recognized(GestureType.FOUR_FINGER_TAP, GestureData())
                } else {
                    GestureResult.NotRecognized
                }
            }
            
            else -> GestureResult.NotRecognized
        }
    }
    
    private fun recognizeTap(event: MotionEvent, duration: Long): GestureResult {
        val zone = getTouchZone(event.x, event.y)
        
        // Check for double tap
        lastTouchEvent?.let { lastEvent ->
            val timeDiff = System.currentTimeMillis() - lastEvent.eventTime
            if (timeDiff < DOUBLE_TAP_THRESHOLD) {
                return GestureResult.Recognized(
                    GestureType.DOUBLE_TAP,
                    GestureData(
                        position = PointF(event.x, event.y),
                        zone = zone
                    )
                )
            }
        }
        
        return GestureResult.Recognized(
            GestureType.SINGLE_TAP,
            GestureData(
                position = PointF(event.x, event.y),
                zone = zone
            )
        )
    }
    
    private fun recognizeLongPress(event: MotionEvent): GestureResult {
        return GestureResult.Recognized(
            GestureType.LONG_PRESS,
            GestureData(
                position = PointF(event.x, event.y),
                pressure = event.pressure
            )
        )
    }
    
    private fun updateTouchPoints(event: MotionEvent, currentTime: Long) {
        for (i in 0 until event.pointerCount) {
            if (i < touchPoints.size) {
                touchPoints[i] = TouchPoint(event.getX(i), event.getY(i), currentTime)
            }
        }
    }
    
    private fun calculateVelocity(event: MotionEvent): Float {
        if (touchPoints.isEmpty()) return 0f
        
        val startPoint = touchPoints.first()
        val deltaTime = (System.currentTimeMillis() - startPoint.timestamp).toFloat()
        val deltaX = event.x - startPoint.x
        val deltaY = event.y - startPoint.y
        val distance = sqrt(deltaX * deltaX + deltaY * deltaY)
        
        return if (deltaTime > 0) distance / deltaTime else 0f
    }
    
    private fun distance(p1: PointF, p2: PointF): Float {
        val dx = p1.x - p2.x
        val dy = p1.y - p2.y
        return sqrt(dx * dx + dy * dy)
    }
    
    private fun angle(p1: PointF, p2: PointF): Float {
        return atan2(p2.y - p1.y, p2.x - p1.x) * 180f / PI.toFloat()
    }
    
    private fun getTouchZone(x: Float, y: Float): TouchZone {
        val screenWidth = Resources.getSystem().displayMetrics.widthPixels
        val screenHeight = Resources.getSystem().displayMetrics.heightPixels
        
        return when {
            x < screenWidth / 3 -> TouchZone.LEFT
            x > screenWidth * 2 / 3 -> TouchZone.RIGHT
            else -> TouchZone.CENTER
        }
    }
    
    companion object {
        private const val TAP_THRESHOLD = 200L
        private const val DOUBLE_TAP_THRESHOLD = 300L
        private const val LONG_PRESS_THRESHOLD = 500L
        private const val MOVEMENT_THRESHOLD = 50f
    }
}

// 3. Haptic Feedback Manager
@Singleton
class HapticFeedbackManager @Inject constructor(
    private val context: Context
) {
    
    private val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        vibratorManager.defaultVibrator
    } else {
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }
    
    private val hapticPatterns = mapOf(
        HapticPattern.LIGHT_TAP to longArrayOf(0, 25),
        HapticPattern.DOUBLE_TAP to longArrayOf(0, 25, 50, 25),
        HapticPattern.LONG_PRESS to longArrayOf(0, 100),
        HapticPattern.SWIPE to longArrayOf(0, 30),
        HapticPattern.ADJUSTMENT to longArrayOf(0, 15, 30, 15),
        HapticPattern.SUCCESS to longArrayOf(0, 50, 100, 50),
        HapticPattern.WARNING to longArrayOf(0, 100, 50, 100, 50, 100),
        HapticPattern.NOTIFICATION to longArrayOf(0, 25, 50, 25, 50, 25),
        HapticPattern.RECORDING_START to longArrayOf(0, 100, 200, 100),
        HapticPattern.RECORDING_STOP to longArrayOf(0, 200, 100, 50),
        HapticPattern.VOICE_COMMAND to longArrayOf(0, 50)
    )
    
    fun lightTap() = performHaptic(HapticPattern.LIGHT_TAP)
    fun doubleTap() = performHaptic(HapticPattern.DOUBLE_TAP)
    fun longPress() = performHaptic(HapticPattern.LONG_PRESS)
    fun swipe() = performHaptic(HapticPattern.SWIPE)
    fun adjustment() = performHaptic(HapticPattern.ADJUSTMENT)
    fun success() = performHaptic(HapticPattern.SUCCESS)
    fun warning() = performHaptic(HapticPattern.WARNING)
    fun notification() = performHaptic(HapticPattern.NOTIFICATION)
    fun recordingStart() = performHaptic(HapticPattern.RECORDING_START)
    fun recordingStop() = performHaptic(HapticPattern.RECORDING_STOP)
    fun voiceCommandReceived() = performHaptic(HapticPattern.VOICE_COMMAND)
    
    fun customFeedback(pattern: HapticPattern) = performHaptic(pattern)
    
    private fun performHaptic(pattern: HapticPattern) {
        if (!isHapticEnabled()) return
        
        val hapticPattern = hapticPatterns[pattern] ?: return
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val vibrationEffect = VibrationEffect.createWaveform(hapticPattern, -1)
            vibrator.vibrate(vibrationEffect)
        } else {
            vibrator.vibrate(hapticPattern, -1)
        }
    }
    
    private fun isHapticEnabled(): Boolean {
        return Settings.System.getInt(
            context.contentResolver,
            Settings.System.HAPTIC_FEEDBACK_ENABLED,
            1
        ) == 1
    }
}

// 4. Voice Gesture Detector
class VoiceGestureDetector(private val context: Context) {
    
    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false
    private var voiceCallback: ((VoiceCommand) -> Unit)? = null
    
    private val voiceCommands = mapOf(
        "play" to VoiceCommand("play", "Play video"),
        "pause" to VoiceCommand("pause", "Pause video"),
        "stop" to VoiceCommand("pause", "Stop video"),
        "next" to VoiceCommand("next", "Next video"),
        "previous" to VoiceCommand("previous", "Previous video"),
        "back" to VoiceCommand("previous", "Go back"),
        "volume up" to VoiceCommand("volume up", "Increase volume"),
        "louder" to VoiceCommand("volume up", "Make louder"),
        "volume down" to VoiceCommand("volume down", "Decrease volume"),
        "quieter" to VoiceCommand("volume down", "Make quieter"),
        "faster" to VoiceCommand("faster", "Increase speed"),
        "slower" to VoiceCommand("slower", "Decrease speed"),
        "screenshot" to VoiceCommand("screenshot", "Take screenshot"),
        "fullscreen" to VoiceCommand("fullscreen", "Toggle fullscreen"),
        "subtitles on" to VoiceCommand("subtitles on", "Enable subtitles"),
        "subtitles off" to VoiceCommand("subtitles off", "Disable subtitles")
    )
    
    fun startListening(callback: (VoiceCommand) -> Unit) {
        this.voiceCallback = callback
        
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            Log.w("VoiceGesture", "Speech recognition not available")
            return
        }
        
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onError(error: Int) {
                Log.w("VoiceGesture", "Speech recognition error: $error")
            }
            
            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                matches?.firstOrNull()?.let { spokenText ->
                    processVoiceCommand(spokenText.lowercase())
                }
            }
            
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
        
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
        
        speechRecognizer?.startListening(intent)
        isListening = true
    }
    
    fun stopListening() {
        speechRecognizer?.stopListening()
        speechRecognizer?.destroy()
        speechRecognizer = null
        isListening = false
    }
    
    private fun processVoiceCommand(spokenText: String) {
        voiceCommands.entries.find { (trigger, _) ->
            spokenText.contains(trigger)
        }?.let { (_, command) ->
            voiceCallback?.invoke(command)
        }
    }
}

// 5. Gesture Recorder
@Singleton
class GestureRecorder @Inject constructor() {
    
    private var isRecording = false
    private var currentGestureName = ""
    private val recordedPoints = mutableListOf<GesturePoint>()
    private var recordingStartTime = 0L
    
    fun startRecording(gestureName: String) {
        isRecording = true
        currentGestureName = gestureName
        recordedPoints.clear()
        recordingStartTime = System.currentTimeMillis()
    }
    
    fun recordGesturePoint(point: GesturePoint) {
        if (!isRecording) return
        
        recordedPoints.add(point.copy(
            timestamp = System.currentTimeMillis() - recordingStartTime
        ))
    }
    
    fun stopRecording(): CustomGesture? {
        if (!isRecording || recordedPoints.isEmpty()) return null
        
        isRecording = false
        
        val gesture = CustomGesture(
            id = UUID.randomUUID().toString(),
            name = currentGestureName,
            gestureType = GestureType.CUSTOM,
            points = recordedPoints.toList(),
            action = GestureAction.Custom(""), // Default empty action
            createdAt = System.currentTimeMillis()
        )
        
        recordedPoints.clear()
        return gesture
    }
}

// 6. Data Classes and Enums
enum class GestureType(
    val displayName: String,
    val description: String,
    val isCustomizable: Boolean = true
) {
    SINGLE_TAP("Single Tap", "Tap once to toggle controls"),
    DOUBLE_TAP("Double Tap", "Tap twice to seek forward/backward"),
    LONG_PRESS("Long Press", "Hold to control playback speed"),
    SWIPE_HORIZONTAL("Horizontal Swipe", "Swipe left/right to seek"),
    SWIPE_VERTICAL_LEFT("Left Edge Swipe", "Swipe up/down on left edge for brightness"),
    SWIPE_VERTICAL_RIGHT("Right Edge Swipe", "Swipe up/down on right edge for volume"),
    PINCH_ZOOM("Pinch to Zoom", "Pinch fingers to zoom in/out"),
    TWO_FINGER_ROTATE("Two Finger Rotate", "Rotate with two fingers"),
    THREE_FINGER_SWIPE_LEFT("Three Finger Swipe Left", "Swipe left with three fingers"),
    THREE_FINGER_SWIPE_RIGHT("Three Finger Swipe Right", "Swipe right with three fingers"),
    THREE_FINGER_TAP("Three Finger Tap", "Tap with three fingers"),
    FOUR_FINGER_TAP("Four Finger Tap", "Tap with four fingers"),
    EDGE_SWIPE_FROM_LEFT("Edge Swipe Left", "Swipe from left edge", false),
    EDGE_SWIPE_FROM_RIGHT("Edge Swipe Right", "Swipe from right edge", false),
    CUSTOM("Custom Gesture", "User-recorded custom gesture")
}

enum class TouchZone { LEFT, CENTER, RIGHT }

enum class GestureState {
    IDLE, TOUCH_DOWN, TWO_FINGER, THREE_FINGER, FOUR_FINGER, MULTI_FINGER
}

enum class HapticPattern {
    LIGHT_TAP, DOUBLE_TAP, LONG_PRESS, SWIPE, ADJUSTMENT,
    SUCCESS, WARNING, NOTIFICATION, RECORDING_START, RECORDING_STOP, VOICE_COMMAND
}

data class TouchPoint(
    val x: Float,
    val y: Float,
    val timestamp: Long
)

data class GesturePoint(
    val x: Float,
    val y: Float,
    val pressure: Float,
    val timestamp: Long
)

data class GestureData(
    val position: PointF = PointF(0f, 0f),
    val distance: Float = 0f,
    val velocity: Float = 0f,
    val deltaX: Float = 0f,
    val deltaY: Float = 0f,
    val scaleFactor: Float = 1f,
    val rotationAngle: Float = 0f,
    val pressure: Float = 0f,
    val zone: TouchZone = TouchZone.CENTER,
    val customGestureId: String? = null
)

data class VoiceCommand(
    val action: String,
    val description: String
)

data class CustomGesture(
    val id: String,
    val name: String,
    val gestureType: GestureType,
    val points: List<GesturePoint> = emptyList(),
    val action: GestureAction,
    val createdAt: Long
)

data class GestureDefinition(
    val type: GestureType,
    val name: String,
    val description: String,
    val isCustomizable: Boolean,
    val currentAction: GestureAction?
)

sealed class GestureAction {
    object PlayPause : GestureAction()
    data class SeekTo(val positionMs: Long) : GestureAction()
    data class ChangeSpeed(val speed: Float) : GestureAction()
    object ToggleSubtitles : GestureAction()
    object SwitchAudioTrack : GestureAction()
    object TakeScreenshot : GestureAction()
    object OpenSettings : GestureAction()
    data class Custom(val script: String) : GestureAction()
    
    val hapticPattern: HapticPattern
        get() = when (this) {
            is PlayPause -> HapticPattern.LIGHT_TAP
            is SeekTo -> HapticPattern.SWIPE
            is ChangeSpeed -> HapticPattern.ADJUSTMENT
            is ToggleSubtitles -> HapticPattern.NOTIFICATION
            is SwitchAudioTrack -> HapticPattern.NOTIFICATION
            is TakeScreenshot -> HapticPattern.SUCCESS
            is OpenSettings -> HapticPattern.LIGHT_TAP
            is Custom -> HapticPattern.LIGHT_TAP
        }
}

sealed class GestureResult {
    object NotRecognized : GestureResult()
    data class Recognized(val gesture: GestureType, val data: GestureData) : GestureResult()
    data class Recording(val point: GesturePoint) : GestureResult()
}

// 7. Advanced Gesture Callbacks Interface
interface AdvancedGestureCallbacks {
    fun onSingleTap(position: PointF)
    fun onDoubleTapSeek(zone: TouchZone, seekAmountMs: Long)
    fun onLongPressSpeedControl(position: PointF, pressure: Float)
    fun onSeek(seekAmountMs: Long)
    fun onSeekTo(positionMs: Long)
    fun onBrightnessAdjust(delta: Float)
    fun onVolumeAdjust(delta: Float)
    fun onZoom(scaleFactor: Float)
    fun onRotate(angle: Float)
    fun onPlayPause()
    fun onSpeedChange(speed: Float)
    fun onPreviousVideo()
    fun onNextVideo()
    fun onTogglePlaylist()
    fun onToggleFullscreen()
    fun onShowSidebar()
    fun onShowQuickSettings()
    fun onToggleSubtitles()
    fun onSwitchAudioTrack()
    fun onTakeScreenshot()
    fun onOpenSettings()
}

// 8. Gesture Customization Repository
@Singleton
class GestureCustomizationRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    
    private val customGesturesKey = stringPreferencesKey("custom_gestures")
    private val voiceGesturesEnabledKey = booleanPreferencesKey("voice_gestures_enabled")
    private val doubleTapSeekAmountsKey = stringPreferencesKey("double_tap_seek_amounts")
    
    suspend fun saveCustomGesture(gesture: CustomGesture) {
        val gson = Gson()
        val existingGestures = getCustomGestures().toMutableList()
        
        // Remove existing gesture with same name
        existingGestures.removeAll { it.name == gesture.name }
        existingGestures.add(gesture)
        
        val gesturesJson = gson.toJson(existingGestures)
        
        dataStore.edit { preferences ->
            preferences[customGesturesKey] = gesturesJson
        }
    }
    
    suspend fun getCustomGestures(): List<CustomGesture> {
        val gson = Gson()
        val gesturesJson = dataStore.data.map { preferences ->
            preferences[customGesturesKey] ?: "[]"
        }.first()
        
        return try {
            val type = object : TypeToken<List<CustomGesture>>() {}.type
            gson.fromJson(gesturesJson, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    suspend fun deleteCustomGesture(gestureName: String) {
        val existingGestures = getCustomGestures().toMutableList()
        existingGestures.removeAll { it.name == gestureName }
        
        val gson = Gson()
        val gesturesJson = gson.toJson(existingGestures)
        
        dataStore.edit { preferences ->
            preferences[customGesturesKey] = gesturesJson
        }
    }
    
    suspend fun isVoiceGesturesEnabled(): Boolean {
        return dataStore.data.map { preferences ->
            preferences[voiceGesturesEnabledKey] ?: false
        }.first()
    }
    
    suspend fun setVoiceGesturesEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[voiceGesturesEnabledKey] = enabled
        }
    }
    
    suspend fun getDoubleTapSeekAmount(zone: TouchZone): Long {
        val seekAmountsJson = dataStore.data.map { preferences ->
            preferences[doubleTapSeekAmountsKey] ?: "{}"
        }.first()
        
        val gson = Gson()
        val seekAmounts: Map<String, Long> = try {
            val type = object : TypeToken<Map<String, Long>>() {}.type
            gson.fromJson(seekAmountsJson, type) ?: emptyMap()
        } catch (e: Exception) {
            emptyMap()
        }
        
        return seekAmounts[zone.name] ?: when (zone) {
            TouchZone.LEFT -> 10000L // 10 seconds backward
            TouchZone.RIGHT -> 10000L // 10 seconds forward
            TouchZone.CENTER -> 5000L // 5 seconds
        }
    }
    
    suspend fun setDoubleTapSeekAmount(zone: TouchZone, amountMs: Long) {
        val existingAmounts = getAllDoubleTapSeekAmounts().toMutableMap()
        existingAmounts[zone.name] = amountMs
        
        val gson = Gson()
        val seekAmountsJson = gson.toJson(existingAmounts)
        
        dataStore.edit { preferences ->
            preferences[doubleTapSeekAmountsKey] = seekAmountsJson
        }
    }
    
    private suspend fun getAllDoubleTapSeekAmounts(): Map<String, Long> {
        val seekAmountsJson = dataStore.data.map { preferences ->
            preferences[doubleTapSeekAmountsKey] ?: "{}"
        }.first()
        
        val gson = Gson()
        return try {
            val type = object : TypeToken<Map<String, Long>>() {}.type
            gson.fromJson(seekAmountsJson, type) ?: emptyMap()
        } catch (e: Exception) {
            emptyMap()
        }
    }
}