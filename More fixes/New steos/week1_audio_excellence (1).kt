// ================================
// WEEK 1 - DAY 5-6: AUDIO EXCELLENCE
// ================================

// 1. Audio Processing Engine
@Singleton
class AudioProcessingEngine @Inject constructor(
    @ApplicationContext private val context: Context,
    private val performanceMonitor: PerformanceMonitor
) {
    
    private var audioProcessor: AudioProcessor? = null
    private var audioTrack: AudioTrack? = null
    private val processingScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    
    // Audio processing parameters
    private var normalizationEnabled = true
    private var dialogEnhancementEnabled = false
    private var nightModeEnabled = false
    private var volumeBoost = 1.0f
    
    data class AudioConfig(
        val sampleRate: Int = 48000,
        val channelCount: Int = 2,
        val bitDepth: Int = 16,
        val bufferSize: Int = 4096
    )
    
    fun initialize(config: AudioConfig = AudioConfig()) {
        val channelConfig = if (config.channelCount == 2) {
            AudioFormat.CHANNEL_OUT_STEREO
        } else {
            AudioFormat.CHANNEL_OUT_MONO
        }
        
        val minBufferSize = AudioTrack.getMinBufferSize(
            config.sampleRate,
            channelConfig,
            AudioFormat.ENCODING_PCM_16BIT
        )
        
        audioTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MOVIE)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setSampleRate(config.sampleRate)
                    .setChannelMask(channelConfig)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .build()
            )
            .setBufferSizeInBytes(maxOf(minBufferSize, config.bufferSize))
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()
        
        audioProcessor = AudioProcessor(config)
    }
    
    suspend fun processAudioBuffer(
        inputBuffer: ShortArray,
        outputBuffer: ShortArray
    ): Int = withContext(Dispatchers.Default) {
        val startTime = performanceMonitor.startMeasure("audio_process")
        
        var processedSamples = inputBuffer.size
        
        // Copy input to output
        inputBuffer.copyInto(outputBuffer, 0, 0, processedSamples)
        
        // Apply processing chain
        if (normalizationEnabled) {
            processedSamples = normalizeAudio(outputBuffer, processedSamples)
        }
        
        if (dialogEnhancementEnabled) {
            processedSamples = enhanceDialog(outputBuffer, processedSamples)
        }
        
        if (nightModeEnabled) {
            processedSamples = applyNightMode(outputBuffer, processedSamples)
        }
        
        // Apply volume boost
        if (volumeBoost != 1.0f) {
            applyGain(outputBuffer, processedSamples, volumeBoost)
        }
        
        performanceMonitor.endMeasure("audio_process", startTime)
        
        processedSamples
    }
    
    private fun normalizeAudio(buffer: ShortArray, samples: Int): Int {
        // Calculate RMS
        var sum = 0.0
        for (i in 0 until samples) {
            val sample = buffer[i].toFloat() / Short.MAX_VALUE
            sum += sample * sample
        }
        val rms = sqrt(sum / samples)
        
        // Target RMS level (0.25 = -12 dB)
        val targetRms = 0.25
        
        if (rms > 0.001) { // Avoid division by zero
            val gain = (targetRms / rms).coerceIn(0.5f, 3.0f) // Limit gain range
            applyGain(buffer, samples, gain)
        }
        
        return samples
    }
    
    private fun enhanceDialog(buffer: ShortArray, samples: Int): Int {
        // Simple dialog enhancement using mid-frequency boost
        // In real implementation, would use FFT for frequency analysis
        
        val enhancedBuffer = ShortArray(samples)
        
        // High-pass filter to reduce low frequencies
        var prevSample = 0.0f
        val cutoffFreq = 200f // Hz
        val rc = 1.0f / (2.0f * Math.PI.toFloat() * cutoffFreq)
        val dt = 1.0f / 48000f // Sample period
        val alpha = dt / (rc + dt)
        
        for (i in 0 until samples) {
            val sample = buffer[i].toFloat() / Short.MAX_VALUE
            val filtered = alpha * (prevSample + sample - buffer.getOrElse(i - 1) { 0 }.toFloat() / Short.MAX_VALUE)
            prevSample = filtered
            
            // Mix original with filtered (boost mids/highs)
            val enhanced = sample * 0.5f + filtered * 0.5f
            enhancedBuffer[i] = (enhanced * Short.MAX_VALUE).toInt().toShort()
        }
        
        enhancedBuffer.copyInto(buffer, 0, 0, samples)
        return samples
    }
    
    private fun applyNightMode(buffer: ShortArray, samples: Int): Int {
        // Dynamic range compression for night mode
        val threshold = 0.5f // -6 dB
        val ratio = 4.0f // 4:1 compression
        val makeupGain = 1.5f
        
        for (i in 0 until samples) {
            val sample = buffer[i].toFloat() / Short.MAX_VALUE
            val absSample = abs(sample)
            
            val compressedSample = if (absSample > threshold) {
                val excess = absSample - threshold
                val compressedExcess = excess / ratio
                val compressedAbs = threshold + compressedExcess
                sample.sign * compressedAbs * makeupGain
            } else {
                sample * makeupGain
            }
            
            buffer[i] = (compressedSample.coerceIn(-1f, 1f) * Short.MAX_VALUE).toInt().toShort()
        }
        
        return samples
    }
    
    private fun applyGain(buffer: ShortArray, samples: Int, gain: Float) {
        for (i in 0 until samples) {
            val amplified = buffer[i] * gain
            buffer[i] = amplified.toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
    }
    
    fun setNormalizationEnabled(enabled: Boolean) {
        normalizationEnabled = enabled
    }
    
    fun setDialogEnhancement(enabled: Boolean) {
        dialogEnhancementEnabled = enabled
    }
    
    fun setNightMode(enabled: Boolean) {
        nightModeEnabled = enabled
    }
    
    fun setVolumeBoost(boost: Float) {
        volumeBoost = boost.coerceIn(0.5f, 2.0f)
    }
    
    fun release() {
        audioTrack?.release()
        audioTrack = null
        audioProcessor = null
        processingScope.cancel()
    }
}

// 2. Volume Normalizer
@Singleton
class VolumeNormalizer @Inject constructor(
    private val context: Context,
    private val audioPreferences: AudioPreferences
) {
    
    private val volumeHistory = mutableMapOf<String, VolumeProfile>()
    private val targetLoudness = -23.0f // LUFS (EBU R128 standard)
    
    data class VolumeProfile(
        val videoId: String,
        val averageLoudness: Float,
        val peakLevel: Float,
        val dynamicRange: Float,
        val recommendedGain: Float,
        val lastUpdated: Long = System.currentTimeMillis()
    )
    
    suspend fun analyzeAudio(videoPath: String): VolumeProfile = withContext(Dispatchers.IO) {
        val videoId = videoPath.hashCode().toString()
        
        // Check if we already have a profile
        volumeHistory[videoId]?.let { profile ->
            if (System.currentTimeMillis() - profile.lastUpdated < 30 * 24 * 60 * 60 * 1000) { // 30 days
                return@withContext profile
            }
        }
        
        // Analyze audio
        val audioData = extractAudioData(videoPath)
        val loudness = calculateLoudness(audioData)
        val peak = findPeakLevel(audioData)
        val range = calculateDynamicRange(audioData)
        
        val gain = targetLoudness - loudness
        
        val profile = VolumeProfile(
            videoId = videoId,
            averageLoudness = loudness,
            peakLevel = peak,
            dynamicRange = range,
            recommendedGain = gain.coerceIn(-12f, 12f) // Limit gain range
        )
        
        // Save profile
        volumeHistory[videoId] = profile
        audioPreferences.saveVolumeProfile(profile)
        
        profile
    }
    
    private suspend fun extractAudioData(videoPath: String): FloatArray = withContext(Dispatchers.IO) {
        // In real implementation, would use MediaExtractor to get audio samples
        // For now, return dummy data
        FloatArray(48000) // 1 second of audio at 48kHz
    }
    
    private fun calculateLoudness(audioData: FloatArray): Float {
        // Simplified loudness calculation
        // Real implementation would use ITU-R BS.1770 algorithm
        
        val squares = audioData.map { it * it }
        val meanSquare = squares.average()
        val rms = sqrt(meanSquare).toFloat()
        
        // Convert to dB
        return 20 * log10(rms + 0.00001f)
    }
    
    private fun findPeakLevel(audioData: FloatArray): Float {
        return audioData.maxOf { abs(it) }
    }
    
    private fun calculateDynamicRange(audioData: FloatArray): Float {
        // Calculate difference between loud and quiet parts
        val sortedData = audioData.map { abs(it) }.sorted()
        val percentile95 = sortedData[(sortedData.size * 0.95).toInt()]
        val percentile10 = sortedData[(sortedData.size * 0.10).toInt()]
        
        return if (percentile10 > 0) {
            20 * log10(percentile95 / percentile10)
        } else {
            0f
        }
    }
    
    fun getRecommendedVolume(videoPath: String): Float {
        val videoId = videoPath.hashCode().toString()
        val profile = volumeHistory[videoId] ?: audioPreferences.loadVolumeProfile(videoId)
        
        return profile?.recommendedGain ?: 0f
    }
    
    fun clearHistory() {
        volumeHistory.clear()
        audioPreferences.clearAllProfiles()
    }
}

// 3. Dialog Enhancer
@Singleton
class DialogEnhancer @Inject constructor(
    private val context: Context
) {
    
    private var enhancementLevel = EnhancementLevel.MEDIUM
    
    enum class EnhancementLevel(val boost: Float, val clarity: Float) {
        OFF(0f, 0f),
        LOW(0.3f, 0.2f),
        MEDIUM(0.5f, 0.4f),
        HIGH(0.7f, 0.6f),
        MAXIMUM(1.0f, 0.8f)
    }
    
    fun setEnhancementLevel(level: EnhancementLevel) {
        enhancementLevel = level
    }
    
    fun enhanceDialog(
        inputBuffer: FloatArray,
        sampleRate: Int,
        channelCount: Int
    ): FloatArray {
        if (enhancementLevel == EnhancementLevel.OFF) {
            return inputBuffer
        }
        
        val outputBuffer = FloatArray(inputBuffer.size)
        
        // Frequency bands for dialog (typically 100Hz - 8kHz)
        val dialogLowFreq = 100f
        val dialogHighFreq = 8000f
        
        // Apply bandpass filter and enhancement
        val filtered = bandpassFilter(inputBuffer, sampleRate, dialogLowFreq, dialogHighFreq)
        
        // Mix enhanced dialog with original
        for (i in inputBuffer.indices) {
            val original = inputBuffer[i]
            val enhanced = filtered[i] * enhancementLevel.boost
            
            // Add clarity by slight high-frequency boost
            val clarityBoost = if (i > 0) {
                (inputBuffer[i] - inputBuffer[i - 1]) * enhancementLevel.clarity
            } else {
                0f
            }
            
            outputBuffer[i] = (original + enhanced + clarityBoost).coerceIn(-1f, 1f)
        }
        
        return outputBuffer
    }
    
    private fun bandpassFilter(
        input: FloatArray,
        sampleRate: Int,
        lowFreq: Float,
        highFreq: Float
    ): FloatArray {
        // Simple butterworth bandpass filter
        val output = FloatArray(input.size)
        
        // Calculate filter coefficients
        val wcLow = 2 * Math.PI * lowFreq / sampleRate
        val wcHigh = 2 * Math.PI * highFreq / sampleRate
        
        // Simplified filter implementation
        var y1 = 0.0
        var y2 = 0.0
        var x1 = 0.0
        var x2 = 0.0
        
        for (i in input.indices) {
            val x = input[i].toDouble()
            
            // Apply filter equation
            val y = 0.5 * (x - x2) + 0.9 * y1 - 0.4 * y2
            
            output[i] = y.toFloat()
            
            // Update state
            x2 = x1
            x1 = x
            y2 = y1
            y1 = y
        }
        
        return output
    }
}

// 4. Audio Preset Manager
@Singleton
class AudioPresetManager @Inject constructor(
    private val context: Context,
    private val audioPreferences: AudioPreferences,
    private val audioProcessingEngine: AudioProcessingEngine,
    private val dialogEnhancer: DialogEnhancer
) {
    
    sealed class AudioPreset(
        val name: String,
        val normalization: Boolean,
        val dialogEnhancement: DialogEnhancer.EnhancementLevel,
        val nightMode: Boolean,
        val volumeBoost: Float
    ) {
        object Default : AudioPreset("Default", true, DialogEnhancer.EnhancementLevel.OFF, false, 1.0f)
        object Movie : AudioPreset("Movie", true, DialogEnhancer.EnhancementLevel.MEDIUM, false, 1.1f)
        object Night : AudioPreset("Night", true, DialogEnhancer.EnhancementLevel.HIGH, true, 0.9f)
        object Music : AudioPreset("Music", false, DialogEnhancer.EnhancementLevel.OFF, false, 1.0f)
        object Podcast : AudioPreset("Podcast", true, DialogEnhancer.EnhancementLevel.MAXIMUM, false, 1.2f)
        object Custom : AudioPreset("Custom", true, DialogEnhancer.EnhancementLevel.MEDIUM, false, 1.0f)
    }
    
    private var currentPreset: AudioPreset = AudioPreset.Default
    private val customSettings = mutableMapOf<String, Any>()
    
    init {
        loadSavedPreset()
    }
    
    private fun loadSavedPreset() {
        val savedPresetName = audioPreferences.getSavedPreset()
        currentPreset = when (savedPresetName) {
            "Movie" -> AudioPreset.Movie
            "Night" -> AudioPreset.Night
            "Music" -> AudioPreset.Music
            "Podcast" -> AudioPreset.Podcast
            "Custom" -> loadCustomPreset()
            else -> AudioPreset.Default
        }
        
        applyPreset(currentPreset)
    }
    
    private fun loadCustomPreset(): AudioPreset {
        return AudioPreset.Custom.copy(
            normalization = audioPreferences.getCustomSetting("normalization", true),
            dialogEnhancement = DialogEnhancer.EnhancementLevel.valueOf(
                audioPreferences.getCustomSetting("dialog", "MEDIUM")
            ),
            nightMode = audioPreferences.getCustomSetting("night", false),
            volumeBoost = audioPreferences.getCustomSetting("boost", 1.0f)
        )
    }
    
    fun setPreset(preset: AudioPreset) {
        currentPreset = preset
        applyPreset(preset)
        audioPreferences.savePreset(preset.name)
    }
    
    private fun applyPreset(preset: AudioPreset) {
        audioProcessingEngine.setNormalizationEnabled(preset.normalization)
        audioProcessingEngine.setDialogEnhancement(preset.dialogEnhancement != DialogEnhancer.EnhancementLevel.OFF)
        audioProcessingEngine.setNightMode(preset.nightMode)
        audioProcessingEngine.setVolumeBoost(preset.volumeBoost)
        
        dialogEnhancer.setEnhancementLevel(preset.dialogEnhancement)
    }
    
    fun customizeCurrentPreset(
        normalization: Boolean? = null,
        dialogLevel: DialogEnhancer.EnhancementLevel? = null,
        nightMode: Boolean? = null,
        volumeBoost: Float? = null
    ) {
        val custom = AudioPreset.Custom.copy(
            normalization = normalization ?: currentPreset.normalization,
            dialogEnhancement = dialogLevel ?: currentPreset.dialogEnhancement,
            nightMode = nightMode ?: currentPreset.nightMode,
            volumeBoost = volumeBoost ?: currentPreset.volumeBoost
        )
        
        setPreset(custom)
        saveCustomSettings(custom)
    }
    
    private fun saveCustomSettings(preset: AudioPreset) {
        audioPreferences.saveCustomSetting("normalization", preset.normalization)
        audioPreferences.saveCustomSetting("dialog", preset.dialogEnhancement.name)
        audioPreferences.saveCustomSetting("night", preset.nightMode)
        audioPreferences.saveCustomSetting("boost", preset.volumeBoost)
    }
    
    fun getCurrentPreset(): AudioPreset = currentPreset
    
    fun getAvailablePresets(): List<AudioPreset> = listOf(
        AudioPreset.Default,
        AudioPreset.Movie,
        AudioPreset.Night,
        AudioPreset.Music,
        AudioPreset.Podcast,
        AudioPreset.Custom
    )
}

// 5. Audio Track Manager
@Singleton
class AudioTrackManager @Inject constructor(
    private val context: Context
) {
    
    data class AudioTrackInfo(
        val index: Int,
        val language: String?,
        val title: String?,
        val codec: String?,
        val channelCount: Int,
        val sampleRate: Int,
        val bitrate: Int?
    )
    
    private val availableTracks = mutableListOf<AudioTrackInfo>()
    private var selectedTrackIndex = 0
    
    suspend fun loadAudioTracks(videoPath: String): List<AudioTrackInfo> = withContext(Dispatchers.IO) {
        availableTracks.clear()
        
        val extractor = MediaExtractor()
        try {
            extractor.setDataSource(videoPath)
            
            for (i in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: continue
                
                if (mime.startsWith("audio/")) {
                    val track = AudioTrackInfo(
                        index = i,
                        language = format.getString(MediaFormat.KEY_LANGUAGE),
                        title = format.getString(MediaFormat.KEY_TITLE),
                        codec = mime.substringAfter("audio/"),
                        channelCount = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT),
                        sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE),
                        bitrate = format.getIntegerOrNull(MediaFormat.KEY_BIT_RATE)
                    )
                    
                    availableTracks.add(track)
                }
            }
            
            availableTracks
        } finally {
            extractor.release()
        }
    }
    
    fun selectTrack(index: Int) {
        if (index in availableTracks.indices) {
            selectedTrackIndex = index
        }
    }
    
    fun selectTrackByLanguage(language: String) {
        availableTracks.firstOrNull { it.language == language }?.let {
            selectedTrackIndex = availableTracks.indexOf(it)
        }
    }
    
    fun getCurrentTrack(): AudioTrackInfo? {
        return availableTracks.getOrNull(selectedTrackIndex)
    }
    
    fun getAvailableTracks(): List<AudioTrackInfo> = availableTracks.toList()
    
    private fun MediaFormat.getIntegerOrNull(key: String): Int? {
        return try {
            getInteger(key)
        } catch (e: Exception) {
            null
        }
    }
}

// 6. Audio Preferences
@Singleton
class AudioPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    private val prefs = context.getSharedPreferences("audio_preferences", Context.MODE_PRIVATE)
    private val volumeProfilesPrefs = context.getSharedPreferences("volume_profiles", Context.MODE_PRIVATE)
    
    fun saveVolumeProfile(profile: VolumeNormalizer.VolumeProfile) {
        val json = Json.encodeToString(profile)
        volumeProfilesPrefs.edit().putString(profile.videoId, json).apply()
    }
    
    fun loadVolumeProfile(videoId: String): VolumeNormalizer.VolumeProfile? {
        val json = volumeProfilesPrefs.getString(videoId, null) ?: return null
        return try {
            Json.decodeFromString(json)
        } catch (e: Exception) {
            null
        }
    }
    
    fun clearAllProfiles() {
        volumeProfilesPrefs.edit().clear().apply()
    }
    
    fun getSavedPreset(): String {
        return prefs.getString("audio_preset", "Default") ?: "Default"
    }
    
    fun savePreset(presetName: String) {
        prefs.edit().putString("audio_preset", presetName).apply()
    }
    
    fun <T> getCustomSetting(key: String, default: T): T {
        @Suppress("UNCHECKED_CAST")
        return when (default) {
            is Boolean -> prefs.getBoolean("custom_$key", default) as T
            is Float -> prefs.getFloat("custom_$key", default) as T
            is String -> prefs.getString("custom_$key", default) as T
            else -> default
        }
    }
    
    fun <T> saveCustomSetting(key: String, value: T) {
        prefs.edit().apply {
            when (value) {
                is Boolean -> putBoolean("custom_$key", value)
                is Float -> putFloat("custom_$key", value)
                is String -> putString("custom_$key", value)
            }
        }.apply()
    }
    
    fun getVolumeMemoryEnabled(): Boolean {
        return prefs.getBoolean("volume_memory", true)
    }
    
    fun setVolumeMemoryEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("volume_memory", enabled).apply()
    }
    
    fun saveVideoVolume(videoId: String, volume: Float) {
        if (getVolumeMemoryEnabled()) {
            prefs.edit().putFloat("volume_$videoId", volume).apply()
        }
    }
    
    fun getVideoVolume(videoId: String): Float? {
        return if (getVolumeMemoryEnabled() && prefs.contains("volume_$videoId")) {
            prefs.getFloat("volume_$videoId", 1.0f)
        } else {
            null
        }
    }
}

// 7. Audio UI Components
@Composable
fun AudioSettingsScreen(
    viewModel: AudioViewModel = hiltViewModel()
) {
    val audioState by viewModel.audioState.collectAsState()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = "Audio Settings",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 24.dp)
        )
        
        // Audio Preset Selection
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Audio Preset",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(audioState.availablePresets) { preset ->
                        FilterChip(
                            selected = audioState.currentPreset.name == preset.name,
                            onClick = { viewModel.selectPreset(preset) },
                            label = { 
                                Text(preset.name)
                            },
                            leadingIcon = if (audioState.currentPreset.name == preset.name) {
                                {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            } else null
                        )
                    }
                }
            }
        }
        
        // Custom Audio Controls
        AnimatedVisibility(
            visible = audioState.currentPreset.name == "Custom",
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Custom Settings",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    
                    // Volume Normalization
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Volume Normalization")
                        Switch(
                            checked = audioState.normalizationEnabled,
                            onCheckedChange = { viewModel.setNormalization(it) }
                        )
                    }
                    
                    // Dialog Enhancement
                    Column(modifier = Modifier.padding(vertical = 8.dp)) {
                        Text(
                            text = "Dialog Enhancement",
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        
                        Slider(
                            value = audioState.dialogEnhancementLevel.ordinal.toFloat(),
                            onValueChange = { viewModel.setDialogEnhancement(it.toInt()) },
                            valueRange = 0f..4f,
                            steps = 3,
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Off", style = MaterialTheme.typography.bodySmall)
                            Text("Max", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    
                    // Night Mode
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Night Mode")
                            Text(
                                text = "Reduces loud sounds",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = audioState.nightModeEnabled,
                            onCheckedChange = { viewModel.setNightMode(it) }
                        )
                    }
                    
                    // Volume Boost
                    Column(modifier = Modifier.padding(vertical = 8.dp)) {
                        Text(
                            text = "Volume Boost: ${(audioState.volumeBoost * 100).toInt()}%",
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        
                        Slider(
                            value = audioState.volumeBoost,
                            onValueChange = { viewModel.setVolumeBoost(it) },
                            valueRange = 0.5f..2.0f,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
        
        // Volume Memory
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Volume Memory",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Remember volume for each video",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = audioState.volumeMemoryEnabled,
                    onCheckedChange = { viewModel.setVolumeMemory(it) }
                )
            }
        }
        
        // Audio Track Selection (if available)
        if (audioState.availableTracks.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Audio Track",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    
                    audioState.availableTracks.forEach { track ->
                        RadioButton(
                            selected = track.index == audioState.selectedTrackIndex,
                            onClick = { viewModel.selectAudioTrack(track.index) },
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                        Text(
                            text = buildString {
                                append(track.language ?: "Track ${track.index + 1}")
                                track.title?.let { append(" - $it") }
                                append(" (${track.codec}, ${track.channelCount}ch)")
                            },
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }
        }
    }
}

// 8. Audio Quick Controls Component
@Composable
fun AudioQuickControls(
    currentPreset: AudioPresetManager.AudioPreset,
    onPresetClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Current preset chip
        AssistChip(
            onClick = onPresetClick,
            label = { Text(currentPreset.name) },
            leadingIcon = {
                Icon(
                    imageVector = when (currentPreset) {
                        is AudioPresetManager.AudioPreset.Movie -> Icons.Default.Movie
                        is AudioPresetManager.AudioPreset.Night -> Icons.Default.NightsStay
                        is AudioPresetManager.AudioPreset.Music -> Icons.Default.MusicNote
                        is AudioPresetManager.AudioPreset.Podcast -> Icons.Default.Mic
                        else -> Icons.Default.VolumeUp
                    },
                    contentDescription = null
                )
            }
        )
        
        // Quick toggles
        if (currentPreset.nightMode) {
            AssistChip(
                onClick = { },
                label = { Text("Night") },
                leadingIcon = {
                    Icon(
                        Icons.Default.NightsStay,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            )
        }
        
        if (currentPreset.dialogEnhancement != DialogEnhancer.EnhancementLevel.OFF) {
            AssistChip(
                onClick = { },
                label = { Text("Dialog+") },
                leadingIcon = {
                    Icon(
                        Icons.Default.RecordVoiceOver,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            )
        }
    }
}

// 9. Audio ViewModel
@HiltViewModel
class AudioViewModel @Inject constructor(
    private val audioPresetManager: AudioPresetManager,
    private val volumeNormalizer: VolumeNormalizer,
    private val audioTrackManager: AudioTrackManager,
    private val audioPreferences: AudioPreferences,
    private val dialogEnhancer: DialogEnhancer
) : ViewModel() {
    
    private val _audioState = MutableStateFlow(AudioState())
    val audioState: StateFlow<AudioState> = _audioState.asStateFlow()
    
    init {
        loadAudioState()
    }
    
    private fun loadAudioState() {
        val currentPreset = audioPresetManager.getCurrentPreset()
        
        _audioState.value = AudioState(
            currentPreset = currentPreset,
            availablePresets = audioPresetManager.getAvailablePresets(),
            normalizationEnabled = currentPreset.normalization,
            dialogEnhancementLevel = currentPreset.dialogEnhancement,
            nightModeEnabled = currentPreset.nightMode,
            volumeBoost = currentPreset.volumeBoost,
            volumeMemoryEnabled = audioPreferences.getVolumeMemoryEnabled(),
            availableTracks = audioTrackManager.getAvailableTracks(),
            selectedTrackIndex = audioTrackManager.getCurrentTrack()?.index ?: 0
        )
    }
    
    fun selectPreset(preset: AudioPresetManager.AudioPreset) {
        audioPresetManager.setPreset(preset)
        loadAudioState()
    }
    
    fun setNormalization(enabled: Boolean) {
        audioPresetManager.customizeCurrentPreset(normalization = enabled)
        loadAudioState()
    }
    
    fun setDialogEnhancement(level: Int) {
        val enhancementLevel = DialogEnhancer.EnhancementLevel.values()[level]
        audioPresetManager.customizeCurrentPreset(dialogLevel = enhancementLevel)
        loadAudioState()
    }
    
    fun setNightMode(enabled: Boolean) {
        audioPresetManager.customizeCurrentPreset(nightMode = enabled)
        loadAudioState()
    }
    
    fun setVolumeBoost(boost: Float) {
        audioPresetManager.customizeCurrentPreset(volumeBoost = boost)
        loadAudioState()
    }
    
    fun setVolumeMemory(enabled: Boolean) {
        audioPreferences.setVolumeMemoryEnabled(enabled)
        _audioState.value = _audioState.value.copy(volumeMemoryEnabled = enabled)
    }
    
    fun selectAudioTrack(index: Int) {
        audioTrackManager.selectTrack(index)
        _audioState.value = _audioState.value.copy(selectedTrackIndex = index)
    }
    
    fun loadAudioTracks(videoPath: String) {
        viewModelScope.launch {
            val tracks = audioTrackManager.loadAudioTracks(videoPath)
            _audioState.value = _audioState.value.copy(availableTracks = tracks)
        }
    }
    
    data class AudioState(
        val currentPreset: AudioPresetManager.AudioPreset = AudioPresetManager.AudioPreset.Default,
        val availablePresets: List<AudioPresetManager.AudioPreset> = emptyList(),
        val normalizationEnabled: Boolean = true,
        val dialogEnhancementLevel: DialogEnhancer.EnhancementLevel = DialogEnhancer.EnhancementLevel.OFF,
        val nightModeEnabled: Boolean = false,
        val volumeBoost: Float = 1.0f,
        val volumeMemoryEnabled: Boolean = true,
        val availableTracks: List<AudioTrackManager.AudioTrackInfo> = emptyList(),
        val selectedTrackIndex: Int = 0
    )
}

// 10. Integration with ExoPlayer
class AudioExoPlayerIntegration @Inject constructor(
    private val audioProcessingEngine: AudioProcessingEngine,
    private val volumeNormalizer: VolumeNormalizer,
    private val audioPreferences: AudioPreferences
) {
    
    fun setupWithExoPlayer(exoPlayer: ExoPlayer, videoPath: String) {
        // Set up audio processor
        val audioProcessor = object : AudioProcessor {
            private var isConfigured = false
            private lateinit var inputAudioFormat: AudioProcessor.AudioFormat
            
            override fun configure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
                this.inputAudioFormat = inputAudioFormat
                isConfigured = true
                return inputAudioFormat // Output same format
            }
            
            override fun isActive(): Boolean = true
            
            override fun queueInput(inputBuffer: ByteBuffer) {
                if (!isConfigured) return
                
                // Process audio
                val shortBuffer = inputBuffer.asShortBuffer()
                val samples = ShortArray(shortBuffer.remaining())
                shortBuffer.get(samples)
                
                val outputSamples = ShortArray(samples.size)
                
                // Apply processing
                runBlocking {
                    audioProcessingEngine.processAudioBuffer(samples, outputSamples)
                }
                
                // Write back
                shortBuffer.rewind()
                shortBuffer.put(outputSamples)
            }
            
            override fun queueEndOfStream() {
                // Handle end of stream
            }
            
            override fun getOutput(): ByteBuffer = ByteBuffer.allocate(0)
            
            override fun isEnded(): Boolean = false
            
            override fun flush() {
                // Flush processor
            }
            
            override fun reset() {
                isConfigured = false
            }
        }
        
        // Apply volume normalization
        viewModelScope.launch {
            val volumeProfile = volumeNormalizer.analyzeAudio(videoPath)
            val recommendedGain = volumeProfile.recommendedGain
            
            // Apply gain
            exoPlayer.volume = (exoPlayer.volume + recommendedGain / 20f).coerceIn(0f, 1f)
        }
        
        // Load saved volume if available
        val videoId = videoPath.hashCode().toString()
        audioPreferences.getVideoVolume(videoId)?.let { savedVolume ->
            exoPlayer.volume = savedVolume
        }
        
        // Save volume changes
        exoPlayer.addListener(object : Player.Listener {
            override fun onVolumeChanged(volume: Float) {
                audioPreferences.saveVideoVolume(videoId, volume)
            }
        })
    }
}

// 11. Hilt Module
@Module
@InstallIn(SingletonComponent::class)
object AudioModule {
    
    @Provides
    @Singleton
    fun provideAudioProcessingEngine(
        @ApplicationContext context: Context,
        performanceMonitor: PerformanceMonitor
    ): AudioProcessingEngine {
        return AudioProcessingEngine(context, performanceMonitor)
    }
    
    @Provides
    @Singleton
    fun provideVolumeNormalizer(
        @ApplicationContext context: Context,
        audioPreferences: AudioPreferences
    ): VolumeNormalizer {
        return VolumeNormalizer(context, audioPreferences)
    }
    
    @Provides
    @Singleton
    fun provideDialogEnhancer(
        @ApplicationContext context: Context
    ): DialogEnhancer {
        return DialogEnhancer(context)
    }
    
    @Provides
    @Singleton
    fun provideAudioPresetManager(
        @ApplicationContext context: Context,
        audioPreferences: AudioPreferences,
        audioProcessingEngine: AudioProcessingEngine,
        dialogEnhancer: DialogEnhancer
    ): AudioPresetManager {
        return AudioPresetManager(context, audioPreferences, audioProcessingEngine, dialogEnhancer)
    }
    
    @Provides
    @Singleton
    fun provideAudioTrackManager(
        @ApplicationContext context: Context
    ): AudioTrackManager {
        return AudioTrackManager(context)
    }
    
    @Provides
    @Singleton
    fun provideAudioPreferences(
        @ApplicationContext context: Context
    ): AudioPreferences {
        return AudioPreferences(context)
    }
}