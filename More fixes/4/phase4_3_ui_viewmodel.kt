// ================================
// Phase 4.3: Smart Home UI Components and ViewModel
// ================================

// 7. Smart Home Video Player ViewModel
@HiltViewModel
class SmartHomeVideoViewModel @Inject constructor(
    private val smartHomeEngine: SmartHomeIntegrationEngine,
    private val savedStateHandle: SavedStateHandle,
    @ApplicationContext private val context: Context
) : ViewModel(), SmartHomeCallbacks {
    
    private val _smartHomeState = MutableStateFlow(SmartHomeState())
    val smartHomeState: StateFlow<SmartHomeState> = _smartHomeState.asStateFlow()
    
    private val _connectedDevices = MutableStateFlow<List<SmartDevice>>(emptyList())
    val connectedDevices: StateFlow<List<SmartDevice>> = _connectedDevices.asStateFlow()
    
    private val _voiceTranscription = MutableStateFlow("")
    val voiceTranscription: StateFlow<String> = _voiceTranscription.asStateFlow()
    
    private val _castingState = MutableStateFlow(CastingState())
    val castingState: StateFlow<CastingState> = _castingState.asStateFlow()
    
    private val _lightSyncEnabled = MutableStateFlow(false)
    val lightSyncEnabled: StateFlow<Boolean> = _lightSyncEnabled.asStateFlow()
    
    val player = ExoPlayer.Builder(context).build()
    
    private var currentSession: SmartHomeSession? = null
    
    init {
        viewModelScope.launch {
            smartHomeEngine.initializeSmartHome(this@SmartHomeVideoViewModel)
        }
    }
    
    fun loadVideo(uri: Uri) {
        val mediaItem = MediaItem.fromUri(uri)
        player.setMediaItem(mediaItem)
        player.prepare()
        
        // Start smart home session
        startSmartHomeSession(mediaItem)
    }
    
    private fun startSmartHomeSession(mediaItem: MediaItem) {
        viewModelScope.launch {
            val config = SmartHomeConfiguration(
                enableVoiceControl = _smartHomeState.value.voiceControlEnabled,
                enableCasting = _smartHomeState.value.castingEnabled,
                enableLightSync = _smartHomeState.value.lightSyncEnabled,
                enableAutomation = _smartHomeState.value.automationEnabled,
                targetCastDevice = _castingState.value.selectedDevice
            )
            
            currentSession = smartHomeEngine.startSmartHomeSession(mediaItem, config)
        }
    }
    
    fun toggleVoiceControl() {
        val newState = !_smartHomeState.value.voiceControlEnabled
        _smartHomeState.value = _smartHomeState.value.copy(voiceControlEnabled = newState)
        
        if (newState) {
            checkMicrophonePermission()
        }
    }
    
    fun selectCastDevice(device: CastDevice) {
        _castingState.value = _castingState.value.copy(
            selectedDevice = device,
            isCasting = true
        )
        
        viewModelScope.launch {
            player.currentMediaItem?.let { mediaItem ->
                smartHomeEngine.startCasting(device, mediaItem)
            }
        }
    }
    
    fun toggleLightSync() {
        _lightSyncEnabled.value = !_lightSyncEnabled.value
        
        viewModelScope.launch {
            if (_lightSyncEnabled.value) {
                val lights = connectedDevices.value.filterIsInstance<SmartLight>()
                smartHomeEngine.startLightSync(lights)
            } else {
                smartHomeEngine.stopLightSync()
            }
        }
    }
    
    fun processVoiceCommand(transcription: String) {
        _voiceTranscription.value = transcription
    }
    
    fun refreshDevices() {
        viewModelScope.launch {
            smartHomeEngine.discoverDevices()
        }
    }
    
    // SmartHomeCallbacks implementation
    override fun onSmartHomeInitialized() {
        _smartHomeState.value = _smartHomeState.value.copy(
            isInitialized = true,
            capabilities = smartHomeEngine.getSmartHomeCapabilities()
        )
    }
    
    override fun onDevicesDiscovered(devices: List<SmartDevice>) {
        _connectedDevices.value = devices
        
        // Auto-connect to preferred devices
        devices.find { it.id == getPreferredCastDeviceId() }?.let { device ->
            if (device is CastDevice) {
                selectCastDevice(device)
            }
        }
    }
    
    override fun onSmartHomeSessionStarted(session: SmartHomeSession) {
        _smartHomeState.value = _smartHomeState.value.copy(
            isSessionActive = true,
            currentSessionId = session.id
        )
    }
    
    override fun onVoiceCommandProcessed(command: VoiceCommand) {
        // Show visual feedback for voice command
        _smartHomeState.value = _smartHomeState.value.copy(
            lastVoiceCommand = command
        )
    }
    
    override fun onPlaybackControl(action: PlaybackAction) {
        when (action) {
            PlaybackAction.PLAY -> player.play()
            PlaybackAction.PAUSE -> player.pause()
            PlaybackAction.STOP -> player.stop()
            PlaybackAction.NEXT -> skipToNext()
            PlaybackAction.PREVIOUS -> skipToPrevious()
        }
    }
    
    override fun onSeekRequested(position: Long) {
        val currentPosition = player.currentPosition
        player.seekTo(currentPosition + position)
    }
    
    override fun onVolumeChanged(level: Float) {
        player.volume = level
    }
    
    override fun onContentSearchRequested(query: String) {
        // Handle content search
        viewModelScope.launch {
            searchContent(query)
        }
    }
    
    override fun onPlaybackSpeedChanged(speed: Float) {
        player.setPlaybackSpeed(speed)
    }
    
    override fun onSmartHomeSessionEnded(result: SmartHomeSessionResult) {
        when (result) {
            is SmartHomeSessionResult.Success -> {
                // Log session stats
                logSessionStats(result.stats)
            }
            is SmartHomeSessionResult.Error -> {
                showError(result.message)
            }
        }
        
        _smartHomeState.value = _smartHomeState.value.copy(
            isSessionActive = false,
            currentSessionId = null
        )
    }
    
    override fun onSmartHomeError(error: String) {
        showError(error)
    }
    
    // Helper methods
    private fun checkMicrophonePermission() {
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            _smartHomeState.value = _smartHomeState.value.copy(
                needsMicrophonePermission = true
            )
        }
    }
    
    private fun getPreferredCastDeviceId(): String? {
        return context.getSharedPreferences("smart_home", Context.MODE_PRIVATE)
            .getString("preferred_cast_device", null)
    }
    
    private suspend fun searchContent(query: String) {
        // Implement content search
    }
    
    private fun skipToNext() {
        // Handle next video
    }
    
    private fun skipToPrevious() {
        // Handle previous video
    }
    
    private fun logSessionStats(stats: SmartHomeSessionStats) {
        Log.d("SmartHomeStats", """
            Duration: ${stats.duration}ms
            Voice Commands: ${stats.voiceCommandsUsed}
            Devices Connected: ${stats.devicesConnected}
            Automations Triggered: ${stats.automationTriggered}
        """.trimIndent())
    }
    
    private fun showError(message: String) {
        _smartHomeState.value = _smartHomeState.value.copy(
            error = message
        )
    }
    
    override fun onCleared() {
        super.onCleared()
        player.release()
        viewModelScope.launch {
            smartHomeEngine.endSmartHomeSession()
        }
    }
}

// 8. Smart Home UI Components
@Composable
fun SmartHomeVideoPlayerUI(
    viewModel: SmartHomeVideoViewModel,
    modifier: Modifier = Modifier
) {
    val smartHomeState by viewModel.smartHomeState.collectAsState()
    val connectedDevices by viewModel.connectedDevices.collectAsState()
    val voiceTranscription by viewModel.voiceTranscription.collectAsState()
    val castingState by viewModel.castingState.collectAsState()
    
    Box(modifier = modifier.fillMaxSize()) {
        // Video surface
        AndroidView(
            factory = { context ->
                PlayerView(context).apply {
                    player = viewModel.player
                    useController = false
                }
            },
            modifier = Modifier.fillMaxSize()
        )
        
        // Smart home controls overlay
        SmartHomeControlsOverlay(
            viewModel = viewModel,
            smartHomeState = smartHomeState,
            connectedDevices = connectedDevices,
            castingState = castingState,
            modifier = Modifier.fillMaxSize()
        )
        
        // Voice transcription overlay
        if (voiceTranscription.isNotEmpty()) {
            VoiceTranscriptionOverlay(
                transcription = voiceTranscription,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 100.dp)
            )
        }
        
        // Permission request
        if (smartHomeState.needsMicrophonePermission) {
            MicrophonePermissionDialog(
                onGranted = { viewModel.toggleVoiceControl() },
                onDenied = { /* Handle denial */ }
            )
        }
    }
}

@Composable
fun SmartHomeControlsOverlay(
    viewModel: SmartHomeVideoViewModel,
    smartHomeState: SmartHomeState,
    connectedDevices: List<SmartDevice>,
    castingState: CastingState,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Top controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Voice control toggle
            SmartHomeFeatureChip(
                icon = if (smartHomeState.voiceControlEnabled) Icons.Default.Mic else Icons.Default.MicOff,
                text = "Voice",
                isEnabled = smartHomeState.voiceControlEnabled,
                onClick = { viewModel.toggleVoiceControl() }
            )
            
            // Cast button
            CastButton(
                devices = connectedDevices.filterIsInstance<CastDevice>(),
                currentDevice = castingState.selectedDevice,
                onDeviceSelected = { viewModel.selectCastDevice(it) }
            )
            
            // Light sync toggle
            SmartHomeFeatureChip(
                icon = Icons.Default.Lightbulb,
                text = "Lights",
                isEnabled = viewModel.lightSyncEnabled.collectAsState().value,
                onClick = { viewModel.toggleLightSync() }
            )
            
            // More options
            IconButton(onClick = { /* Show more options */ }) {
                Icon(Icons.Default.MoreVert, contentDescription = "More options")
            }
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        // Bottom controls
        Column {
            // Connected devices indicator
            if (connectedDevices.isNotEmpty()) {
                ConnectedDevicesRow(
                    devices = connectedDevices,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            
            // Playback controls
            SmartHomePlaybackControls(
                viewModel = viewModel,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun SmartHomeFeatureChip(
    icon: ImageVector,
    text: String,
    isEnabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = if (isEnabled) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.9f)
        } else {
            MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
        },
        contentColor = if (isEnabled) {
            MaterialTheme.colorScheme.onPrimary
        } else {
            MaterialTheme.colorScheme.onSurface
        }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = text,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun CastButton(
    devices: List<CastDevice>,
    currentDevice: CastDevice?,
    onDeviceSelected: (CastDevice) -> Unit,
    modifier: Modifier = Modifier
) {
    var showDeviceList by remember { mutableStateOf(false) }
    
    Box(modifier = modifier) {
        IconButton(
            onClick = { showDeviceList = true }
        ) {
            Icon(
                painter = painterResource(
                    if (currentDevice != null) R.drawable.ic_cast_connected
                    else R.drawable.ic_cast
                ),
                contentDescription = "Cast",
                tint = if (currentDevice != null) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )
        }
        
        DropdownMenu(
            expanded = showDeviceList,
            onDismissRequest = { showDeviceList = false }
        ) {
            devices.forEach { device ->
                DropdownMenuItem(
                    text = {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                painter = painterResource(getDeviceIcon(device)),
                                contentDescription = null,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = device.name,
                                    fontWeight = FontWeight.Medium
                                )
                                if (device.capabilities.supports4K) {
                                    Text(
                                        text = "4K HDR",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    },
                    onClick = {
                        onDeviceSelected(device)
                        showDeviceList = false
                    },
                    leadingIcon = if (device == currentDevice) {
                        { Icon(Icons.Default.Check, contentDescription = null) }
                    } else null
                )
            }
            
            Divider()
            
            DropdownMenuItem(
                text = { Text("Stop casting") },
                onClick = {
                    // Stop casting
                    showDeviceList = false
                },
                enabled = currentDevice != null
            )
        }
    }
}

@Composable
fun ConnectedDevicesRow(
    devices: List<SmartDevice>,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(devices) { device ->
            DeviceChip(device = device)
        }
    }
}

@Composable
fun DeviceChip(
    device: SmartDevice,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(getDeviceIcon(device)),
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = if (device.isConnected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = device.name,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun VoiceTranscriptionOverlay(
    transcription: String,
    modifier: Modifier = Modifier
) {
    var visible by remember { mutableStateOf(true) }
    
    LaunchedEffect(transcription) {
        visible = true
        delay(3000)
        visible = false
    }
    
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = fadeIn() + slideInVertically(),
        exit = fadeOut() + slideOutVertically()
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.RecordVoiceOver,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = transcription,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
fun SmartHomePlaybackControls(
    viewModel: SmartHomeVideoViewModel,
    modifier: Modifier = Modifier
) {
    val playerState by viewModel.player.playbackState.collectAsState()
    
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Progress bar
            VideoProgressBar(
                currentPosition = viewModel.player.currentPosition,
                duration = viewModel.player.duration,
                onSeek = { viewModel.player.seekTo(it) },
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Control buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { viewModel.player.seekBack() }) {
                    Icon(Icons.Default.Replay10, contentDescription = "Rewind 10s")
                }
                
                IconButton(
                    onClick = {
                        if (viewModel.player.isPlaying) {
                            viewModel.player.pause()
                        } else {
                            viewModel.player.play()
                        }
                    },
                    modifier = Modifier.size(56.dp)
                ) {
                    Icon(
                        imageVector = if (viewModel.player.isPlaying) {
                            Icons.Default.Pause
                        } else {
                            Icons.Default.PlayArrow
                        },
                        contentDescription = "Play/Pause",
                        modifier = Modifier.size(32.dp)
                    )
                }
                
                IconButton(onClick = { viewModel.player.seekForward() }) {
                    Icon(Icons.Default.Forward10, contentDescription = "Forward 10s")
                }
            }
        }
    }
}

@Composable
fun VideoProgressBar(
    currentPosition: Long,
    duration: Long,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var isSeeking by remember { mutableStateOf(false) }
    var seekPosition by remember { mutableStateOf(0f) }
    
    Column(modifier = modifier) {
        Slider(
            value = if (isSeeking) seekPosition else {
                if (duration > 0) currentPosition.toFloat() / duration else 0f
            },
            onValueChange = { value ->
                isSeeking = true
                seekPosition = value
            },
            onValueChangeFinished = {
                isSeeking = false
                onSeek((seekPosition * duration).toLong())
            },
            modifier = Modifier.fillMaxWidth()
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = formatTime(currentPosition),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = formatTime(duration),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun MicrophonePermissionDialog(
    onGranted: () -> Unit,
    onDenied: () -> Unit
) {
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) onGranted() else onDenied()
    }
    
    AlertDialog(
        onDismissRequest = onDenied,
        title = { Text("Microphone Permission Required") },
        text = {
            Text("Voice control requires microphone access to listen to your commands.")
        },
        confirmButton = {
            TextButton(
                onClick = {
                    launcher.launch(Manifest.permission.RECORD_AUDIO)
                }
            ) {
                Text("Grant Permission")
            }
        },
        dismissButton = {
            TextButton(onClick = onDenied) {
                Text("Cancel")
            }
        }
    )
}

// 9. Data Classes
data class SmartHomeState(
    val isInitialized: Boolean = false,
    val isSessionActive: Boolean = false,
    val currentSessionId: String? = null,
    val capabilities: SmartHomeCapabilities? = null,
    val voiceControlEnabled: Boolean = false,
    val castingEnabled: Boolean = true,
    val lightSyncEnabled: Boolean = false,
    val automationEnabled: Boolean = true,
    val lastVoiceCommand: VoiceCommand? = null,
    val needsMicrophonePermission: Boolean = false,
    val error: String? = null
)

data class CastingState(
    val isCasting: Boolean = false,
    val selectedDevice: CastDevice? = null,
    val castProgress: Float = 0f,
    val castQuality: CastingQuality = CastingQuality.AUTO
)

// 10. Helper Functions
fun getDeviceIcon(device: SmartDevice): Int {
    return when (device.type) {
        SmartDeviceType.VOICE_ASSISTANT -> R.drawable.ic_voice_assistant
        SmartDeviceType.CAST_DEVICE -> R.drawable.ic_cast_device
        SmartDeviceType.SMART_LIGHT -> R.drawable.ic_smart_light
        SmartDeviceType.SMART_SPEAKER -> R.drawable.ic_speaker
        SmartDeviceType.SMART_DISPLAY -> R.drawable.ic_smart_display
        SmartDeviceType.IoT_SENSOR -> R.drawable.ic_sensor
    }
}

fun formatTime(millis: Long): String {
    val totalSeconds = millis / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}

// 11. Hilt Module
@Module
@InstallIn(SingletonComponent::class)
object SmartHomeModule {
    
    @Provides
    @Singleton
    fun provideSmartHomeIntegrationEngine(
        @ApplicationContext context: Context,
        voiceAssistantManager: VoiceAssistantManager,
        smartTVCastingManager: SmartTVCastingManager,
        ioTDeviceManager: IoTDeviceManager,
        homeAutomationController: HomeAutomationController,
        ambientLightingSync: AmbientLightingSync
    ): SmartHomeIntegrationEngine {
        return SmartHomeIntegrationEngine(
            context = context,
            voiceAssistantManager = voiceAssistantManager,
            smartTVCastingManager = smartTVCastingManager,
            ioTDeviceManager = ioTDeviceManager,
            homeAutomationController = homeAutomationController,
            ambientLightingSync = ambientLightingSync
        )
    }
    
    @Provides
    @Singleton
    fun provideVoiceAssistantManager(
        @ApplicationContext context: Context
    ): VoiceAssistantManager {
        return VoiceAssistantManager(context)
    }
    
    @Provides
    @Singleton
    fun provideSmartTVCastingManager(
        @ApplicationContext context: Context
    ): SmartTVCastingManager {
        return SmartTVCastingManager(context)
    }
    
    @Provides
    @Singleton
    fun provideIoTDeviceManager(
        @ApplicationContext context: Context
    ): IoTDeviceManager {
        return IoTDeviceManager(context)
    }
    
    @Provides
    @Singleton
    fun provideHomeAutomationController(
        @ApplicationContext context: Context
    ): HomeAutomationController {
        return HomeAutomationController(context)
    }
    
    @Provides
    @Singleton
    fun provideAmbientLightingSync(
        @ApplicationContext context: Context,
        ioTDeviceManager: IoTDeviceManager
    ): AmbientLightingSync {
        return AmbientLightingSync(context, ioTDeviceManager)
    }
}

// 12. Integration Entry Point
@Composable
fun VideoPlayerWithSmartHome(
    uri: Uri,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val smartHomeViewModel: SmartHomeVideoViewModel = hiltViewModel()
    
    LaunchedEffect(uri) {
        smartHomeViewModel.loadVideo(uri)
    }
    
    SmartHomeVideoPlayerUI(
        viewModel = smartHomeViewModel,
        modifier = modifier
    )
    
    BackHandler {
        onBack()
    }
}

// 13. Dependencies to add to build.gradle
/*
dependencies {
    // Google Cast SDK
    implementation 'com.google.android.gms:play-services-cast-framework:21.4.0'
    
    // Voice Recognition
    implementation 'com.google.android.gms:play-services-speech:20.0.0'
    
    // Smart Home SDKs (optional - for specific brands)
    implementation 'com.philips.hue:sdk:2.0.0' // Philips Hue
    implementation 'com.samsung.smartthings:sdk:1.0.0' // SmartThings
    
    // Network Discovery
    implementation 'com.github.druk:rxdnssd:0.9.13' // mDNS/Bonjour
    implementation 'org.fourthline.cling:cling-core:2.1.2' // UPnP/DLNA
    
    // Permissions
    implementation "com.google.accompanist:accompanist-permissions:$accompanist_version"
}
*/