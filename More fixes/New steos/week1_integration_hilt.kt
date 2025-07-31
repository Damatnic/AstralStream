// ================================
// WEEK 1 - DAY 7: INTEGRATION & HILT CONFIGURATION
// ================================

// 1. Complete Hilt Module Configuration
@Module
@InstallIn(SingletonComponent::class)
object Week1Module {
    
    // Security & Privacy Module
    @Provides
    @Singleton
    fun provideBiometricManager(
        @ApplicationContext context: Context
    ): BiometricManager {
        return BiometricManager(context)
    }
    
    @Provides
    @Singleton
    fun provideEncryptionEngine(
        @ApplicationContext context: Context
    ): EncryptionEngine {
        return EncryptionEngine(context)
    }
    
    @Provides
    @Singleton
    fun provideSecurePreferences(
        @ApplicationContext context: Context,
        encryptionEngine: EncryptionEngine
    ): SecurePreferences {
        return SecurePreferences(context, encryptionEngine)
    }
    
    @Provides
    @Singleton
    fun provideHiddenFolderManager(
        @ApplicationContext context: Context,
        encryptionEngine: EncryptionEngine,
        securePreferences: SecurePreferences
    ): HiddenFolderManager {
        return HiddenFolderManager(context, encryptionEngine, securePreferences)
    }
    
    @Provides
    @Singleton
    fun provideAppLockManager(
        @ApplicationContext context: Context,
        securePreferences: SecurePreferences
    ): AppLockManager {
        return AppLockManager(context, securePreferences)
    }
    
    @Provides
    @Singleton
    fun provideAppLifecycleObserver(
        appLockManager: AppLockManager
    ): AppLifecycleObserver {
        return AppLifecycleObserver(appLockManager)
    }
}

// 2. Enhanced Application Class
@HiltAndroidApp
class AstralStreamApplication : Application() {
    
    @Inject lateinit var startupOptimizer: StartupOptimizer
    @Inject lateinit var performanceMonitor: PerformanceMonitor
    @Inject lateinit var memoryManager: MemoryManager
    @Inject lateinit var appLifecycleObserver: AppLifecycleObserver
    
    override fun onCreate() {
        super.onCreate()
        
        // Register lifecycle observer
        ProcessLifecycleOwner.get().lifecycle.addObserver(appLifecycleObserver)
        
        // Optimize startup
        runBlocking {
            startupOptimizer.optimize()
        }
        
        // Register memory callbacks
        memoryManager.registerLowMemoryCallback {
            memoryManager.optimizeMemory()
        }
        
        // Initialize crash reporting (if needed)
        initializeCrashReporting()
        
        // Setup strict mode in debug
        setupStrictMode()
    }
    
    private fun initializeCrashReporting() {
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e("AstralStream", "Uncaught exception in thread ${thread.name}", throwable)
            // In production, would send to crash reporting service
        }
    }
    
    private fun setupStrictMode() {
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

// 3. Main Activity with All Integrations
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    @Inject lateinit var appLockManager: AppLockManager
    @Inject lateinit var biometricManager: BiometricManager
    @Inject lateinit var performanceMonitor: PerformanceMonitor
    @Inject lateinit var batteryOptimizer: BatteryOptimizer
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Track performance
        performanceMonitor.trackFrameMetrics(this)
        
        // Check if app is locked
        if (appLockManager.isAppLocked() && biometricManager.isBiometricAvailable()) {
            showBiometricPrompt()
        } else {
            setupMainUI()
        }
        
        // Register battery optimization callback
        batteryOptimizer.registerPowerModeCallback { mode ->
            updateUIForPowerMode(mode)
        }
    }
    
    private fun showBiometricPrompt() {
        biometricManager.authenticate(
            activity = this,
            onSuccess = {
                appLockManager.unlockApp()
                setupMainUI()
            },
            onError = { error ->
                Toast.makeText(this, error, Toast.LENGTH_SHORT).show()
                finish()
            }
        )
    }
    
    private fun setupMainUI() {
        setContent {
            AstralStreamTheme {
                MainScreen()
            }
        }
    }
    
    private fun updateUIForPowerMode(mode: BatteryOptimizer.PowerMode) {
        // Update UI based on power mode
        when (mode) {
            BatteryOptimizer.PowerMode.POWER_SAVER -> {
                // Reduce animations, lower quality
            }
            BatteryOptimizer.PowerMode.BALANCED -> {
                // Normal operation
            }
            BatteryOptimizer.PowerMode.PERFORMANCE -> {
                // Full quality
            }
        }
    }
}

// 4. Main Screen with Navigation
@Composable
fun MainScreen() {
    val navController = rememberNavController()
    
    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = false,
                    onClick = { navController.navigate("videos") },
                    icon = { Icon(Icons.Default.VideoLibrary, contentDescription = "Videos") },
                    label = { Text("Videos") }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = { navController.navigate("hidden") },
                    icon = { Icon(Icons.Default.Lock, contentDescription = "Hidden") },
                    label = { Text("Hidden") }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = { navController.navigate("settings") },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                    label = { Text("Settings") }
                )
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = "videos",
            modifier = Modifier.padding(paddingValues)
        ) {
            composable("videos") {
                VideoListScreen(
                    onVideoClick = { video ->
                        navController.navigate("player/${video.id}")
                    }
                )
            }
            
            composable("hidden") {
                HiddenFolderAuthScreen(
                    onAuthenticated = {
                        navController.navigate("hidden_videos")
                    }
                )
            }
            
            composable("hidden_videos") {
                HiddenFolderScreen(
                    onVideoClick = { hiddenVideo ->
                        navController.navigate("hidden_player/${hiddenVideo.id}")
                    },
                    onBack = { navController.popBackStack() }
                )
            }
            
            composable("settings") {
                SettingsScreen()
            }
            
            composable(
                "player/{videoId}",
                arguments = listOf(navArgument("videoId") { type = NavType.StringType })
            ) { backStackEntry ->
                val videoId = backStackEntry.arguments?.getString("videoId") ?: ""
                EnhancedVideoPlayerScreen(
                    videoId = videoId,
                    onBack = { navController.popBackStack() }
                )
            }
            
            composable(
                "hidden_player/{videoId}",
                arguments = listOf(navArgument("videoId") { type = NavType.StringType })
            ) { backStackEntry ->
                val videoId = backStackEntry.arguments?.getString("videoId") ?: ""
                HiddenVideoPlayerScreen(
                    videoId = videoId,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}

// 5. Enhanced Video Player Screen
@Composable
fun EnhancedVideoPlayerScreen(
    videoId: String,
    onBack: () -> Unit,
    viewModel: EnhancedVideoPlayerViewModel = hiltViewModel()
) {
    val playerState by viewModel.playerState.collectAsState()
    val audioState by viewModel.audioState.collectAsState()
    
    DisposableEffect(videoId) {
        viewModel.loadVideo(videoId)
        
        onDispose {
            viewModel.releasePlayer()
        }
    }
    
    Box(modifier = Modifier.fillMaxSize()) {
        // Video player surface
        AndroidView(
            factory = { context ->
                PlayerView(context).apply {
                    player = viewModel.player
                    useController = false
                    keepScreenOn = true
                }
            },
            modifier = Modifier.fillMaxSize()
        )
        
        // Custom controls overlay
        VideoControlsOverlay(
            playerState = playerState,
            audioState = audioState,
            onPlayPause = { viewModel.togglePlayPause() },
            onSeek = { viewModel.seek(it) },
            onAudioPresetClick = { viewModel.showAudioPresets() },
            onBack = onBack,
            modifier = Modifier.fillMaxSize()
        )
        
        // Audio preset selector
        if (playerState.showAudioPresets) {
            AudioPresetDialog(
                currentPreset = audioState.currentPreset,
                onPresetSelected = { viewModel.selectAudioPreset(it) },
                onDismiss = { viewModel.hideAudioPresets() }
            )
        }
    }
}

// 6. Enhanced Video Player ViewModel
@HiltViewModel
class EnhancedVideoPlayerViewModel @Inject constructor(
    private val fastVideoLoader: FastVideoLoader,
    private val audioExoPlayerIntegration: AudioExoPlayerIntegration,
    private val audioPresetManager: AudioPresetManager,
    private val batteryOptimizer: BatteryOptimizer,
    private val performanceMonitor: PerformanceMonitor,
    @ApplicationContext private val context: Context
) : ViewModel() {
    
    val player = ExoPlayer.Builder(context)
        .setSeekBackIncrementMs(10000)
        .setSeekForwardIncrementMs(10000)
        .build()
    
    private val _playerState = MutableStateFlow(PlayerState())
    val playerState: StateFlow<PlayerState> = _playerState.asStateFlow()
    
    private val _audioState = MutableStateFlow(AudioState())
    val audioState: StateFlow<AudioState> = _audioState.asStateFlow()
    
    init {
        setupPlayer()
        updateAudioState()
    }
    
    private fun setupPlayer() {
        player.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                _playerState.value = _playerState.value.copy(
                    isPlaying = player.isPlaying,
                    isBuffering = playbackState == Player.STATE_BUFFERING
                )
            }
            
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                mediaItem?.let {
                    _playerState.value = _playerState.value.copy(
                        duration = player.duration,
                        currentPosition = 0
                    )
                }
            }
        })
        
        // Update position periodically
        viewModelScope.launch {
            while (isActive) {
                if (player.isPlaying) {
                    _playerState.value = _playerState.value.copy(
                        currentPosition = player.currentPosition
                    )
                }
                delay(100)
            }
        }
    }
    
    fun loadVideo(videoId: String) {
        viewModelScope.launch {
            val startTime = performanceMonitor.startMeasure("video_load_total")
            
            // Get video URI from ID (simplified)
            val videoUri = Uri.parse("content://media/external/video/media/$videoId")
            
            // Fast load with caching
            when (val result = fastVideoLoader.loadVideo(videoUri)) {
                is FastVideoLoader.LoadResult.Success -> {
                    val mediaItem = MediaItem.fromUri(videoUri)
                    player.setMediaItem(mediaItem)
                    
                    // Setup audio processing
                    audioExoPlayerIntegration.setupWithExoPlayer(player, videoUri.toString())
                    
                    // Optimize quality based on battery
                    val quality = batteryOptimizer.getOptimalVideoQuality()
                    // Apply quality settings to player
                    
                    player.prepare()
                    player.play()
                    
                    _playerState.value = _playerState.value.copy(
                        isLoading = false,
                        videoTitle = "Video $videoId",
                        duration = result.metadata.duration
                    )
                    
                    performanceMonitor.endMeasure("video_load_total", startTime)
                }
                is FastVideoLoader.LoadResult.Error -> {
                    _playerState.value = _playerState.value.copy(
                        isLoading = false,
                        error = result.exception.message
                    )
                    performanceMonitor.endMeasure("video_load_total", startTime)
                }
            }
        }
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
    
    fun showAudioPresets() {
        _playerState.value = _playerState.value.copy(showAudioPresets = true)
    }
    
    fun hideAudioPresets() {
        _playerState.value = _playerState.value.copy(showAudioPresets = false)
    }
    
    fun selectAudioPreset(preset: AudioPresetManager.AudioPreset) {
        audioPresetManager.setPreset(preset)
        updateAudioState()
        hideAudioPresets()
    }
    
    private fun updateAudioState() {
        _audioState.value = AudioState(
            currentPreset = audioPresetManager.getCurrentPreset(),
            availablePresets = audioPresetManager.getAvailablePresets()
        )
    }
    
    fun releasePlayer() {
        player.release()
    }
    
    override fun onCleared() {
        super.onCleared()
        player.release()
    }
    
    data class PlayerState(
        val isLoading: Boolean = true,
        val isPlaying: Boolean = false,
        val isBuffering: Boolean = false,
        val videoTitle: String = "",
        val duration: Long = 0,
        val currentPosition: Long = 0,
        val showAudioPresets: Boolean = false,
        val error: String? = null
    )
    
    data class AudioState(
        val currentPreset: AudioPresetManager.AudioPreset = AudioPresetManager.AudioPreset.Default,
        val availablePresets: List<AudioPresetManager.AudioPreset> = emptyList()
    )
}

// 7. Settings Screen with All Features
@Composable
fun SettingsScreen() {
    val scrollState = rememberScrollState()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
    ) {
        // Section headers with navigation
        SettingsSection(
            title = "Security & Privacy",
            icon = Icons.Default.Security,
            onClick = { /* Navigate to security settings */ }
        )
        
        SettingsSection(
            title = "Performance",
            icon = Icons.Default.Speed,
            onClick = { /* Navigate to performance settings */ }
        )
        
        SettingsSection(
            title = "Audio",
            icon = Icons.Default.VolumeUp,
            onClick = { /* Navigate to audio settings */ }
        )
        
        Divider(modifier = Modifier.padding(vertical = 8.dp))
        
        // Quick settings
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Quick Settings",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                // App lock toggle
                QuickSettingItem(
                    title = "App Lock",
                    subtitle = "Require authentication to open app",
                    icon = Icons.Default.Lock,
                    isChecked = true,
                    onCheckedChange = { /* Toggle app lock */ }
                )
                
                // Volume memory toggle
                QuickSettingItem(
                    title = "Volume Memory",
                    subtitle = "Remember volume for each video",
                    icon = Icons.Default.VolumeUp,
                    isChecked = true,
                    onCheckedChange = { /* Toggle volume memory */ }
                )
                
                // Battery optimization
                QuickSettingItem(
                    title = "Battery Optimization",
                    subtitle = "Adjust quality based on battery",
                    icon = Icons.Default.BatteryFull,
                    isChecked = true,
                    onCheckedChange = { /* Toggle battery optimization */ }
                )
            }
        }
    }
}

@Composable
fun SettingsSection(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp)
            )
            
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun QuickSettingItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Switch(
            checked = isChecked,
            onCheckedChange = onCheckedChange
        )
    }
}

// 8. build.gradle Dependencies
/*
dependencies {
    // Core
    implementation "androidx.core:core-ktx:1.12.0"
    implementation "androidx.lifecycle:lifecycle-runtime-ktx:2.7.0"
    implementation "androidx.activity:activity-compose:1.8.2"
    
    // Compose
    implementation platform("androidx.compose:compose-bom:2024.02.00")
    implementation "androidx.compose.ui:ui"
    implementation "androidx.compose.ui:ui-graphics"
    implementation "androidx.compose.ui:ui-tooling-preview"
    implementation "androidx.compose.material3:material3"
    implementation "androidx.compose.material:material-icons-extended"
    
    // Navigation
    implementation "androidx.navigation:navigation-compose:2.7.7"
    
    // Hilt
    implementation "com.google.dagger:hilt-android:2.48"
    implementation "androidx.hilt:hilt-navigation-compose:1.1.0"
    kapt "com.google.dagger:hilt-compiler:2.48"
    
    // ExoPlayer/Media3
    implementation "androidx.media3:media3-exoplayer:1.2.1"
    implementation "androidx.media3:media3-ui:1.2.1"
    implementation "androidx.media3:media3-session:1.2.1"
    
    // Biometric
    implementation "androidx.biometric:biometric:1.1.0"
    
    // Security
    implementation "androidx.security:security-crypto:1.1.0-alpha06"
    
    // Serialization
    implementation "org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2"
    
    // Coroutines
    implementation "org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3"
    
    // Image Loading
    implementation "io.coil-kt:coil-compose:2.5.0"
    
    // Process Lifecycle
    implementation "androidx.lifecycle:lifecycle-process:2.7.0"
    
    // LruCache
    implementation "androidx.collection:collection-ktx:1.3.0"
}
*/

// 9. AndroidManifest.xml Permissions
/*
<uses-permission android:name="android.permission.USE_BIOMETRIC" />
<uses-permission android:name="android.permission.USE_FINGERPRINT" />
<uses-permission android:name="android.permission.VIBRATE" />
<uses-permission android:name="android.permission.MODIFY_AUDIO_SETTINGS" />
<uses-permission android:name="android.permission.BATTERY_STATS" />
<uses-permission android:name="android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS" />

<application
    android:name=".AstralStreamApplication"
    android:allowBackup="false"
    android:dataExtractionRules="@xml/data_extraction_rules"
    android:fullBackupContent="@xml/backup_rules"
    android:icon="@mipmap/ic_launcher"
    android:label="@string/app_name"
    android:largeHeap="true"
    android:hardwareAccelerated="true"
    android:theme="@style/Theme.AstralStream">
    
    <activity
        android:name=".MainActivity"
        android:exported="true"
        android:configChanges="orientation|screenSize|screenLayout|keyboardHidden"
        android:launchMode="singleTask">
        <intent-filter>
            <action android:name="android.intent.action.MAIN" />
            <category android:name="android.intent.category.LAUNCHER" />
        </intent-filter>
    </activity>
    
</application>
*/