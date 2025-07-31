// ================================
// WEEK 1 - DAY 1-2: SECURITY & PRIVACY
// ================================

// 1. Biometric Manager
@Singleton
class BiometricManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val executor = ContextCompat.getMainExecutor(context)
    private var biometricPrompt: BiometricPrompt? = null
    
    fun isBiometricAvailable(): Boolean {
        val biometricManager = BiometricManager.from(context)
        return when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)) {
            BiometricManager.BIOMETRIC_SUCCESS -> true
            else -> false
        }
    }
    
    fun authenticate(
        activity: FragmentActivity,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock AstralStream")
            .setSubtitle("Access your private videos")
            .setNegativeButtonText("Cancel")
            .build()
        
        biometricPrompt = BiometricPrompt(activity, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    onSuccess()
                }
                
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    onError(errString.toString())
                }
                
                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    onError("Authentication failed")
                }
            })
        
        biometricPrompt?.authenticate(promptInfo)
    }
    
    fun cancelAuthentication() {
        biometricPrompt?.cancelAuthentication()
    }
}

// 2. Encryption Engine
@Singleton
class EncryptionEngine @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val keyAlias = "AstralStreamKey"
    private val transformation = "AES/GCM/NoPadding"
    private val androidKeyStore = "AndroidKeyStore"
    
    init {
        generateKey()
    }
    
    private fun generateKey() {
        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, androidKeyStore)
        val keyGenParameterSpec = KeyGenParameterSpec.Builder(
            keyAlias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setUserAuthenticationRequired(false) // We handle auth separately
            .build()
        
        keyGenerator.init(keyGenParameterSpec)
        keyGenerator.generateKey()
    }
    
    private fun getKey(): SecretKey {
        val keyStore = KeyStore.getInstance(androidKeyStore)
        keyStore.load(null)
        return keyStore.getKey(keyAlias, null) as SecretKey
    }
    
    suspend fun encryptFile(inputFile: File, outputFile: File): Boolean = withContext(Dispatchers.IO) {
        try {
            val cipher = Cipher.getInstance(transformation)
            cipher.init(Cipher.ENCRYPT_MODE, getKey())
            
            val iv = cipher.iv
            val buffer = ByteArray(8192)
            
            FileInputStream(inputFile).use { input ->
                FileOutputStream(outputFile).use { output ->
                    // Write IV first
                    output.write(iv.size)
                    output.write(iv)
                    
                    // Encrypt file in chunks
                    val cipherOutputStream = CipherOutputStream(output, cipher)
                    var bytesRead: Int
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        cipherOutputStream.write(buffer, 0, bytesRead)
                    }
                    cipherOutputStream.close()
                }
            }
            true
        } catch (e: Exception) {
            Log.e("EncryptionEngine", "Failed to encrypt file", e)
            false
        }
    }
    
    suspend fun decryptFile(inputFile: File, outputFile: File): Boolean = withContext(Dispatchers.IO) {
        try {
            FileInputStream(inputFile).use { input ->
                // Read IV
                val ivSize = input.read()
                val iv = ByteArray(ivSize)
                input.read(iv)
                
                val cipher = Cipher.getInstance(transformation)
                val spec = GCMParameterSpec(128, iv)
                cipher.init(Cipher.DECRYPT_MODE, getKey(), spec)
                
                FileOutputStream(outputFile).use { output ->
                    val cipherInputStream = CipherInputStream(input, cipher)
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    while (cipherInputStream.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                    }
                }
            }
            true
        } catch (e: Exception) {
            Log.e("EncryptionEngine", "Failed to decrypt file", e)
            false
        }
    }
    
    // Fast in-memory encryption for metadata
    fun encryptString(plainText: String): String {
        val cipher = Cipher.getInstance(transformation)
        cipher.init(Cipher.ENCRYPT_MODE, getKey())
        val iv = cipher.iv
        val encryptedBytes = cipher.doFinal(plainText.toByteArray())
        val combined = ByteArray(iv.size + encryptedBytes.size)
        System.arraycopy(iv, 0, combined, 0, iv.size)
        System.arraycopy(encryptedBytes, 0, combined, iv.size, encryptedBytes.size)
        return Base64.encodeToString(combined, Base64.NO_WRAP)
    }
    
    fun decryptString(encryptedText: String): String {
        val combined = Base64.decode(encryptedText, Base64.NO_WRAP)
        val iv = combined.sliceArray(0..11)
        val encrypted = combined.sliceArray(12 until combined.size)
        
        val cipher = Cipher.getInstance(transformation)
        val spec = GCMParameterSpec(128, iv)
        cipher.init(Cipher.DECRYPT_MODE, getKey(), spec)
        
        return String(cipher.doFinal(encrypted))
    }
}

// 3. Hidden Folder Manager
@Singleton
class HiddenFolderManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val encryptionEngine: EncryptionEngine,
    private val securePreferences: SecurePreferences
) {
    private val hiddenFolderName = ".astralstream_hidden"
    private val hiddenFolderPath = File(context.filesDir, hiddenFolderName)
    
    init {
        if (!hiddenFolderPath.exists()) {
            hiddenFolderPath.mkdirs()
        }
    }
    
    data class HiddenVideo(
        val id: String = UUID.randomUUID().toString(),
        val originalPath: String,
        val encryptedPath: String,
        val thumbnailPath: String?,
        val title: String,
        val duration: Long,
        val hiddenAt: Long = System.currentTimeMillis()
    )
    
    suspend fun hideVideo(videoFile: File): HiddenVideo? = withContext(Dispatchers.IO) {
        try {
            val videoId = UUID.randomUUID().toString()
            val encryptedFile = File(hiddenFolderPath, "$videoId.enc")
            
            // Extract metadata before encryption
            val metadata = extractVideoMetadata(videoFile)
            
            // Generate thumbnail
            val thumbnailFile = generateThumbnail(videoFile, videoId)
            
            // Encrypt the video
            if (encryptionEngine.encryptFile(videoFile, encryptedFile)) {
                val hiddenVideo = HiddenVideo(
                    id = videoId,
                    originalPath = videoFile.absolutePath,
                    encryptedPath = encryptedFile.absolutePath,
                    thumbnailPath = thumbnailFile?.absolutePath,
                    title = metadata.title,
                    duration = metadata.duration
                )
                
                // Save to secure storage
                saveHiddenVideo(hiddenVideo)
                
                // Delete original file
                videoFile.delete()
                
                hiddenVideo
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("HiddenFolderManager", "Failed to hide video", e)
            null
        }
    }
    
    suspend fun unhideVideo(hiddenVideo: HiddenVideo): File? = withContext(Dispatchers.IO) {
        try {
            val encryptedFile = File(hiddenVideo.encryptedPath)
            val restoredFile = File(hiddenVideo.originalPath)
            
            if (encryptionEngine.decryptFile(encryptedFile, restoredFile)) {
                // Remove from hidden list
                removeHiddenVideo(hiddenVideo.id)
                
                // Delete encrypted file and thumbnail
                encryptedFile.delete()
                hiddenVideo.thumbnailPath?.let { File(it).delete() }
                
                restoredFile
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("HiddenFolderManager", "Failed to unhide video", e)
            null
        }
    }
    
    fun getHiddenVideos(): List<HiddenVideo> {
        val encryptedList = securePreferences.getEncryptedString("hidden_videos") ?: return emptyList()
        return try {
            Json.decodeFromString(encryptedList)
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    private fun saveHiddenVideo(video: HiddenVideo) {
        val currentList = getHiddenVideos().toMutableList()
        currentList.add(video)
        val json = Json.encodeToString(currentList)
        securePreferences.putEncryptedString("hidden_videos", json)
    }
    
    private fun removeHiddenVideo(videoId: String) {
        val currentList = getHiddenVideos().toMutableList()
        currentList.removeAll { it.id == videoId }
        val json = Json.encodeToString(currentList)
        securePreferences.putEncryptedString("hidden_videos", json)
    }
    
    private fun extractVideoMetadata(file: File): VideoMetadata {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(file.absolutePath)
            VideoMetadata(
                title = file.nameWithoutExtension,
                duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLong() ?: 0
            )
        } catch (e: Exception) {
            VideoMetadata(file.nameWithoutExtension, 0)
        } finally {
            retriever.release()
        }
    }
    
    private suspend fun generateThumbnail(videoFile: File, videoId: String): File? = withContext(Dispatchers.IO) {
        try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(videoFile.absolutePath)
            val bitmap = retriever.getFrameAtTime(1000000) // 1 second
            retriever.release()
            
            bitmap?.let {
                val thumbnailFile = File(hiddenFolderPath, "$videoId.jpg")
                FileOutputStream(thumbnailFile).use { out ->
                    it.compress(Bitmap.CompressFormat.JPEG, 90, out)
                }
                thumbnailFile
            }
        } catch (e: Exception) {
            null
        }
    }
    
    data class VideoMetadata(val title: String, val duration: Long)
}

// 4. App Lock Manager
@Singleton
class AppLockManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val securePreferences: SecurePreferences
) {
    private var lockTimer: CountDownTimer? = null
    private var isLocked = false
    private val lockDelay = 5000L // 5 seconds after backgrounding
    
    fun isAppLocked(): Boolean = isLocked
    
    fun lockApp() {
        isLocked = true
        securePreferences.putBoolean("app_locked", true)
    }
    
    fun unlockApp() {
        isLocked = false
        securePreferences.putBoolean("app_locked", false)
        cancelLockTimer()
    }
    
    fun onAppBackgrounded() {
        if (isLockEnabled()) {
            startLockTimer()
        }
    }
    
    fun onAppForegrounded() {
        if (isLocked || (lockTimer != null && isLockEnabled())) {
            lockApp()
        }
        cancelLockTimer()
    }
    
    private fun startLockTimer() {
        cancelLockTimer()
        lockTimer = object : CountDownTimer(lockDelay, lockDelay) {
            override fun onTick(millisUntilFinished: Long) {}
            override fun onFinish() {
                lockApp()
            }
        }.start()
    }
    
    private fun cancelLockTimer() {
        lockTimer?.cancel()
        lockTimer = null
    }
    
    fun isLockEnabled(): Boolean = securePreferences.getBoolean("lock_enabled", true)
    fun setLockEnabled(enabled: Boolean) = securePreferences.putBoolean("lock_enabled", enabled)
    
    fun isHiddenFolderLocked(): Boolean = securePreferences.getBoolean("hidden_folder_locked", true)
    fun setHiddenFolderLocked(locked: Boolean) = securePreferences.putBoolean("hidden_folder_locked", locked)
}

// 5. Secure Preferences
@Singleton
class SecurePreferences @Inject constructor(
    @ApplicationContext private val context: Context,
    private val encryptionEngine: EncryptionEngine
) {
    private val prefs = context.getSharedPreferences("secure_prefs", Context.MODE_PRIVATE)
    
    fun putEncryptedString(key: String, value: String) {
        val encrypted = encryptionEngine.encryptString(value)
        prefs.edit().putString(key, encrypted).apply()
    }
    
    fun getEncryptedString(key: String): String? {
        val encrypted = prefs.getString(key, null) ?: return null
        return try {
            encryptionEngine.decryptString(encrypted)
        } catch (e: Exception) {
            null
        }
    }
    
    fun putBoolean(key: String, value: Boolean) {
        prefs.edit().putBoolean(key, value).apply()
    }
    
    fun getBoolean(key: String, default: Boolean): Boolean {
        return prefs.getBoolean(key, default)
    }
    
    fun putLong(key: String, value: Long) {
        prefs.edit().putLong(key, value).apply()
    }
    
    fun getLong(key: String, default: Long): Long {
        return prefs.getLong(key, default)
    }
}

// 6. Security UI Components
@Composable
fun SecuritySettingsScreen(
    viewModel: SecurityViewModel = hiltViewModel()
) {
    val securityState by viewModel.securityState.collectAsState()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Security & Privacy",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 24.dp)
        )
        
        // App Lock Settings
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
                            text = "App Lock",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Require authentication to open app",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = securityState.isAppLockEnabled,
                        onCheckedChange = { viewModel.toggleAppLock() }
                    )
                }
                
                AnimatedVisibility(visible = securityState.isAppLockEnabled) {
                    Column(modifier = Modifier.padding(top = 16.dp)) {
                        OutlinedButton(
                            onClick = { viewModel.testBiometric() },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Fingerprint, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Test Biometric")
                        }
                    }
                }
            }
        }
        
        // Hidden Folder Settings
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
                            text = "Hidden Folder",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "${securityState.hiddenVideoCount} videos hidden",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    FilledTonalButton(
                        onClick = { viewModel.openHiddenFolder() }
                    ) {
                        Icon(Icons.Default.Folder, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Open")
                    }
                }
                
                if (securityState.hiddenVideoCount > 0) {
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = securityState.hiddenStorageUsed / securityState.totalStorage,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = "Using ${formatBytes(securityState.hiddenStorageUsed)} of storage",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
        
        // Quick Actions
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Quick Actions",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { viewModel.lockNow() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Lock Now")
                    }
                    
                    OutlinedButton(
                        onClick = { viewModel.clearCache() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Clear Cache")
                    }
                }
            }
        }
    }
}

@Composable
fun HiddenFolderScreen(
    onVideoClick: (HiddenFolderManager.HiddenVideo) -> Unit,
    onBack: () -> Unit,
    viewModel: HiddenFolderViewModel = hiltViewModel()
) {
    val hiddenVideos by viewModel.hiddenVideos.collectAsState()
    val isSelectionMode by viewModel.isSelectionMode.collectAsState()
    val selectedVideos by viewModel.selectedVideos.collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Hidden Videos") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (isSelectionMode) {
                        IconButton(onClick = { viewModel.unhideSelected() }) {
                            Icon(Icons.Default.Visibility, contentDescription = "Unhide")
                        }
                    } else {
                        IconButton(onClick = { viewModel.toggleSelectionMode() }) {
                            Icon(Icons.Default.CheckBoxOutlineBlank, contentDescription = "Select")
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        if (hiddenVideos.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.FolderOff,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "No hidden videos",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 16.dp)
                    )
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(120.dp),
                contentPadding = paddingValues,
                modifier = Modifier.fillMaxSize()
            ) {
                items(hiddenVideos) { video ->
                    HiddenVideoItem(
                        video = video,
                        isSelected = selectedVideos.contains(video.id),
                        isSelectionMode = isSelectionMode,
                        onClick = {
                            if (isSelectionMode) {
                                viewModel.toggleSelection(video.id)
                            } else {
                                onVideoClick(video)
                            }
                        },
                        onLongClick = {
                            if (!isSelectionMode) {
                                viewModel.toggleSelectionMode()
                                viewModel.toggleSelection(video.id)
                            }
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HiddenVideoItem(
    video: HiddenFolderManager.HiddenVideo,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .padding(4.dp)
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Box {
            // Thumbnail
            video.thumbnailPath?.let { path ->
                AsyncImage(
                    model = File(path),
                    contentDescription = video.title,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f),
                    contentScale = ContentScale.Crop
                )
            } ?: Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )
            
            // Selection checkbox
            if (isSelectionMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = null,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                )
            }
            
            // Duration
            Surface(
                color = Color.Black.copy(alpha = 0.7f),
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(4.dp)
            ) {
                Text(
                    text = formatDuration(video.duration),
                    color = Color.White,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                )
            }
        }
        
        // Title
        Text(
            text = video.title,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(8.dp)
        )
    }
}

// 7. Security ViewModel
@HiltViewModel
class SecurityViewModel @Inject constructor(
    private val appLockManager: AppLockManager,
    private val biometricManager: BiometricManager,
    private val hiddenFolderManager: HiddenFolderManager,
    @ApplicationContext private val context: Context
) : ViewModel() {
    
    private val _securityState = MutableStateFlow(SecurityState())
    val securityState: StateFlow<SecurityState> = _securityState.asStateFlow()
    
    init {
        loadSecurityState()
    }
    
    private fun loadSecurityState() {
        viewModelScope.launch {
            val hiddenVideos = hiddenFolderManager.getHiddenVideos()
            val hiddenStorageUsed = hiddenVideos.sumOf { File(it.encryptedPath).length() }
            
            _securityState.value = SecurityState(
                isAppLockEnabled = appLockManager.isLockEnabled(),
                isBiometricAvailable = biometricManager.isBiometricAvailable(),
                hiddenVideoCount = hiddenVideos.size,
                hiddenStorageUsed = hiddenStorageUsed,
                totalStorage = context.filesDir.totalSpace
            )
        }
    }
    
    fun toggleAppLock() {
        val newState = !_securityState.value.isAppLockEnabled
        appLockManager.setLockEnabled(newState)
        _securityState.value = _securityState.value.copy(isAppLockEnabled = newState)
    }
    
    fun testBiometric() {
        // This would be called from the activity
    }
    
    fun openHiddenFolder() {
        // Navigate to hidden folder
    }
    
    fun lockNow() {
        appLockManager.lockApp()
    }
    
    fun clearCache() {
        viewModelScope.launch {
            // Clear cache implementation
        }
    }
    
    data class SecurityState(
        val isAppLockEnabled: Boolean = false,
        val isBiometricAvailable: Boolean = false,
        val hiddenVideoCount: Int = 0,
        val hiddenStorageUsed: Long = 0,
        val totalStorage: Long = 0
    )
}

// 8. Hidden Folder ViewModel
@HiltViewModel
class HiddenFolderViewModel @Inject constructor(
    private val hiddenFolderManager: HiddenFolderManager
) : ViewModel() {
    
    private val _hiddenVideos = MutableStateFlow<List<HiddenFolderManager.HiddenVideo>>(emptyList())
    val hiddenVideos: StateFlow<List<HiddenFolderManager.HiddenVideo>> = _hiddenVideos.asStateFlow()
    
    private val _isSelectionMode = MutableStateFlow(false)
    val isSelectionMode: StateFlow<Boolean> = _isSelectionMode.asStateFlow()
    
    private val _selectedVideos = MutableStateFlow<Set<String>>(emptySet())
    val selectedVideos: StateFlow<Set<String>> = _selectedVideos.asStateFlow()
    
    init {
        loadHiddenVideos()
    }
    
    private fun loadHiddenVideos() {
        _hiddenVideos.value = hiddenFolderManager.getHiddenVideos()
    }
    
    fun toggleSelectionMode() {
        _isSelectionMode.value = !_isSelectionMode.value
        if (!_isSelectionMode.value) {
            _selectedVideos.value = emptySet()
        }
    }
    
    fun toggleSelection(videoId: String) {
        val current = _selectedVideos.value.toMutableSet()
        if (current.contains(videoId)) {
            current.remove(videoId)
        } else {
            current.add(videoId)
        }
        _selectedVideos.value = current
    }
    
    fun unhideSelected() {
        viewModelScope.launch {
            val selected = _selectedVideos.value
            val videos = _hiddenVideos.value.filter { selected.contains(it.id) }
            
            videos.forEach { video ->
                hiddenFolderManager.unhideVideo(video)
            }
            
            loadHiddenVideos()
            toggleSelectionMode()
        }
    }
}

// 9. App Lifecycle Observer
class AppLifecycleObserver @Inject constructor(
    private val appLockManager: AppLockManager
) : DefaultLifecycleObserver {
    
    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)
        appLockManager.onAppForegrounded()
    }
    
    override fun onStop(owner: LifecycleOwner) {
        super.onStop(owner)
        appLockManager.onAppBackgrounded()
    }
}

// 10. Utility Functions
fun formatBytes(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
        else -> "${bytes / (1024 * 1024 * 1024)} GB"
    }
}

fun formatDuration(millis: Long): String {
    val seconds = millis / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    
    return when {
        hours > 0 -> String.format("%d:%02d:%02d", hours, minutes % 60, seconds % 60)
        else -> String.format("%d:%02d", minutes, seconds % 60)
    }
}