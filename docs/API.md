# 📖 AstralStream API Documentation

## Overview

This document provides comprehensive API documentation for AstralStream Elite, including all public interfaces, use cases, and integration examples.

## Table of Contents

1. [Video Player API](#video-player-api)
2. [Use Cases](#use-cases)
3. [Repository Interfaces](#repository-interfaces)
4. [AI Services](#ai-services)
5. [Security APIs](#security-apis)
6. [Performance APIs](#performance-apis)

## Video Player API

### EnhancedVideoPlayerActivity

The main video player activity with elite features.

```kotlin
class EnhancedVideoPlayerActivity : AppCompatActivity() {
    
    /**
     * Set the video URI to play
     * @param uri The video URI (local file or remote URL)
     */
    fun setVideoUri(uri: String)
    
    /**
     * Set the starting position for playback
     * @param position Position in milliseconds
     */
    fun setStartPosition(position: Long)
    
    /**
     * Start video playback
     */
    fun play()
    
    /**
     * Pause video playback
     */
    fun pause()
    
    /**
     * Generate AI-powered subtitles
     * @param language Target language code (default: "en")
     * @param provider AI provider (default: "auto")
     */
    suspend fun generateEliteSubtitles(
        language: String = "en",
        provider: String = "auto"
    ): Result<List<Subtitle>>
    
    /**
     * Set video quality
     * @param quality Quality string ("auto", "480p", "720p", "1080p", "4K")
     */
    fun setEliteVideoQuality(quality: String)
    
    /**
     * Enable picture-in-picture mode
     */
    fun enablePictureInPicture()
    
    /**
     * Set playback speed
     * @param speed Speed multiplier (0.5x to 2.0x)
     */
    fun setPlaybackSpeed(speed: Float)
}
```

### VideoPlayerViewModel

ViewModel for managing video player state.

```kotlin
class VideoPlayerViewModel @Inject constructor(
    private val playVideoUseCase: PlayVideoUseCase,
    private val generateSubtitlesUseCase: GenerateSubtitlesUseCase
) : ViewModel() {
    
    /**
     * Current playback state
     */
    val playbackState: StateFlow<PlaybackState>
    
    /**
     * Video metadata
     */
    val videoMetadata: StateFlow<VideoMetadata?>
    
    /**
     * Subtitle state
     */
    val subtitleState: StateFlow<SubtitleState>
    
    /**
     * Play video from URI
     */
    fun playVideo(uri: String)
    
    /**
     * Control playback
     */
    fun togglePlayPause()
    fun seekTo(position: Long)
    fun skipForward(seconds: Int = 10)
    fun skipBackward(seconds: Int = 10)
    
    /**
     * Generate subtitles
     */
    fun generateSubtitles(
        language: String = "en",
        provider: AIProvider = AIProvider.AUTO
    )
}
```

## Use Cases

### PlayVideoUseCase

Handles video playback business logic.

```kotlin
class PlayVideoUseCase @Inject constructor(
    private val videoRepository: VideoRepository,
    private val playerRepository: PlayerRepository
) {
    /**
     * Execute video playback
     * @param videoUri The video URI to play
     * @return Flow of playback results
     */
    suspend operator fun invoke(videoUri: String): Flow<Result<PlaybackInfo>> {
        return flow {
            emit(Result.Loading)
            
            try {
                // Validate video URI
                val validatedUri = videoRepository.validateUri(videoUri)
                
                // Get video metadata
                val metadata = videoRepository.getVideoMetadata(validatedUri)
                
                // Initialize player
                playerRepository.initializePlayer(metadata)
                
                // Start playback
                val playbackInfo = playerRepository.startPlayback()
                
                emit(Result.Success(playbackInfo))
            } catch (e: Exception) {
                emit(Result.Error(e))
            }
        }
    }
}
```

### GenerateSubtitlesUseCase

Handles AI subtitle generation with multi-provider support.

```kotlin
class GenerateSubtitlesUseCase @Inject constructor(
    private val subtitleRepository: SubtitleRepository,
    private val aiServiceRepository: AIServiceRepository
) {
    /**
     * Generate subtitles for video
     * @param videoUri The video URI
     * @param language Target language
     * @param provider AI provider to use
     * @return Flow of subtitle generation results
     */
    suspend operator fun invoke(
        videoUri: String,
        language: String = "en",
        provider: AIProvider = AIProvider.AUTO
    ): Flow<Result<List<Subtitle>>> {
        return flow {
            emit(Result.Loading)
            
            try {
                // Check cache first
                val cached = subtitleRepository.getCachedSubtitles(videoUri, language)
                if (cached != null) {
                    emit(Result.Success(cached))
                    return@flow
                }
                
                // Generate with AI
                val subtitles = when (provider) {
                    AIProvider.AUTO -> aiServiceRepository.generateWithBestProvider(videoUri, language)
                    else -> aiServiceRepository.generateWithProvider(videoUri, language, provider)
                }
                
                // Save to cache
                subtitleRepository.cacheSubtitles(videoUri, language, subtitles)
                
                emit(Result.Success(subtitles))
            } catch (e: Exception) {
                emit(Result.Error(e))
            }
        }
    }
}
```

## Repository Interfaces

### VideoRepository

```kotlin
interface VideoRepository {
    /**
     * Validate video URI
     */
    suspend fun validateUri(uri: String): String
    
    /**
     * Get video metadata
     */
    suspend fun getVideoMetadata(uri: String): VideoMetadata
    
    /**
     * Get recent videos
     */
    fun getRecentVideos(): Flow<List<Video>>
    
    /**
     * Save playback position
     */
    suspend fun savePlaybackPosition(videoId: String, position: Long)
    
    /**
     * Get playback position
     */
    suspend fun getPlaybackPosition(videoId: String): Long?
}
```

### SubtitleRepository

```kotlin
interface SubtitleRepository {
    /**
     * Get cached subtitles
     */
    suspend fun getCachedSubtitles(
        videoUri: String, 
        language: String
    ): List<Subtitle>?
    
    /**
     * Cache subtitles
     */
    suspend fun cacheSubtitles(
        videoUri: String,
        language: String,
        subtitles: List<Subtitle>
    )
    
    /**
     * Load subtitle file
     */
    suspend fun loadSubtitleFile(filePath: String): List<Subtitle>
    
    /**
     * Export subtitles to file
     */
    suspend fun exportSubtitles(
        subtitles: List<Subtitle>,
        format: SubtitleFormat
    ): File
}
```

## AI Services

### AIServiceRepository

```kotlin
interface AIServiceRepository {
    /**
     * Generate subtitles with best available provider
     */
    suspend fun generateWithBestProvider(
        videoUri: String,
        language: String
    ): List<Subtitle>
    
    /**
     * Generate subtitles with specific provider
     */
    suspend fun generateWithProvider(
        videoUri: String,
        language: String,
        provider: AIProvider
    ): List<Subtitle>
    
    /**
     * Check provider availability
     */
    suspend fun isProviderAvailable(provider: AIProvider): Boolean
    
    /**
     * Get provider capabilities
     */
    fun getProviderCapabilities(provider: AIProvider): ProviderCapabilities
}
```

### AI Provider Enum

```kotlin
enum class AIProvider {
    AUTO,        // Automatically select best provider
    OPENAI,      // OpenAI Whisper
    GOOGLE,      // Google Cloud Speech
    AZURE,       // Azure Speech Services
    ASSEMBLY_AI, // AssemblyAI
    DEEPGRAM     // Deepgram
}
```

## Security APIs

### BiometricAuthManager

```kotlin
class BiometricAuthManager @Inject constructor(
    private val context: Context
) {
    /**
     * Authenticate user with biometrics
     * @param title Authentication prompt title
     * @param subtitle Authentication prompt subtitle
     * @param onSuccess Success callback
     * @param onError Error callback
     */
    fun authenticate(
        title: String,
        subtitle: String? = null,
        onSuccess: () -> Unit,
        onError: (error: BiometricError) -> Unit
    )
    
    /**
     * Check if biometric authentication is available
     */
    fun isBiometricAvailable(): Boolean
    
    /**
     * Encrypt data with biometric protection
     */
    suspend fun encryptWithBiometric(
        data: ByteArray,
        keyAlias: String
    ): EncryptedData
    
    /**
     * Decrypt data with biometric authentication
     */
    suspend fun decryptWithBiometric(
        encryptedData: EncryptedData,
        keyAlias: String
    ): ByteArray
}
```

### SecurityManager

```kotlin
class SecurityManager @Inject constructor(
    private val context: Context
) {
    /**
     * Initialize encrypted storage
     */
    fun initializeEncryptedStorage()
    
    /**
     * Store secure data
     */
    suspend fun storeSecureData(key: String, value: String)
    
    /**
     * Retrieve secure data
     */
    suspend fun getSecureData(key: String): String?
    
    /**
     * Clear all secure data
     */
    suspend fun clearSecureData()
    
    /**
     * Verify app integrity
     */
    fun verifyAppIntegrity(): Boolean
}
```

## Performance APIs

### PerformanceMonitor

```kotlin
class PerformanceMonitor @Inject constructor(
    private val context: Context
) {
    /**
     * Current performance metrics
     */
    val performanceState: StateFlow<PerformanceMetrics>
    
    /**
     * Start performance monitoring
     */
    fun startMonitoring()
    
    /**
     * Stop performance monitoring
     */
    fun stopMonitoring()
    
    /**
     * Get performance report
     */
    fun getPerformanceReport(): PerformanceReport
    
    /**
     * Performance metrics data class
     */
    data class PerformanceMetrics(
        val currentFps: Int,
        val averageFps: Int,
        val memoryUsageMB: Int,
        val cpuUsagePercent: Int,
        val isSmoothPlayback: Boolean
    )
}
```

### StartupPerformanceManager

```kotlin
class StartupPerformanceManager @Inject constructor(
    private val context: Context
) {
    /**
     * Mark app start time
     */
    fun markAppStart()
    
    /**
     * Initialize critical components
     */
    suspend fun initializeCriticalComponents(): Boolean
    
    /**
     * Initialize non-critical components
     */
    fun initializeNonCriticalComponents()
    
    /**
     * Mark first frame rendered
     */
    fun markFirstFrameRendered()
    
    /**
     * Get startup report
     */
    fun getStartupReport(): StartupReport
}
```

## Data Models

### Video

```kotlin
data class Video(
    val id: String,
    val uri: String,
    val title: String,
    val duration: Long,
    val thumbnailUrl: String?,
    val lastPlayedPosition: Long = 0,
    val addedDate: Long = System.currentTimeMillis()
)
```

### VideoMetadata

```kotlin
data class VideoMetadata(
    val uri: String,
    val title: String,
    val duration: Long,
    val width: Int,
    val height: Int,
    val frameRate: Float,
    val bitrate: Long,
    val codec: String,
    val hasAudio: Boolean,
    val audioChannels: Int
)
```

### Subtitle

```kotlin
data class Subtitle(
    val id: String,
    val startTime: Long,
    val endTime: Long,
    val text: String,
    val language: String,
    val confidence: Float? = null
)
```

### PlaybackState

```kotlin
sealed class PlaybackState {
    object Idle : PlaybackState()
    object Loading : PlaybackState()
    data class Playing(
        val position: Long,
        val duration: Long,
        val bufferedPercentage: Int
    ) : PlaybackState()
    data class Paused(val position: Long) : PlaybackState()
    data class Error(val exception: Exception) : PlaybackState()
    object Completed : PlaybackState()
}
```

## Error Handling

### Result Wrapper

```kotlin
sealed class Result<out T> {
    object Loading : Result<Nothing>()
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val exception: Exception) : Result<Nothing>()
}
```

### Common Exceptions

```kotlin
class VideoNotFoundException(message: String) : Exception(message)
class SubtitleGenerationException(message: String) : Exception(message)
class NetworkException(message: String) : Exception(message)
class SecurityException(message: String) : Exception(message)
class PerformanceException(message: String) : Exception(message)
```

## Integration Examples

### Basic Video Playback

```kotlin
// In your Activity or Fragment
class MyVideoActivity : AppCompatActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val videoUri = intent.data?.toString() ?: return
        
        val intent = Intent(this, EnhancedVideoPlayerActivity::class.java).apply {
            putExtra("video_uri", videoUri)
            putExtra("start_position", 0L)
        }
        startActivity(intent)
    }
}
```

### Using ViewModels

```kotlin
@AndroidEntryPoint
class VideoFragment : Fragment() {
    
    private val viewModel: VideoPlayerViewModel by viewModels()
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Observe playback state
        viewModel.playbackState.collectLatest { state ->
            when (state) {
                is PlaybackState.Playing -> updatePlayingUI(state)
                is PlaybackState.Error -> showError(state.exception)
                // Handle other states
            }
        }
        
        // Play video
        viewModel.playVideo("content://path/to/video.mp4")
        
        // Generate subtitles
        viewModel.generateSubtitles(
            language = "es",
            provider = AIProvider.OPENAI
        )
    }
}
```

### Custom Repository Implementation

```kotlin
class VideoRepositoryImpl @Inject constructor(
    private val videoDao: VideoDao,
    private val mediaMetadataRetriever: MediaMetadataRetriever
) : VideoRepository {
    
    override suspend fun getVideoMetadata(uri: String): VideoMetadata {
        return withContext(Dispatchers.IO) {
            mediaMetadataRetriever.apply {
                setDataSource(uri)
            }
            
            VideoMetadata(
                uri = uri,
                title = extractMetadata(METADATA_KEY_TITLE) ?: "Unknown",
                duration = extractMetadata(METADATA_KEY_DURATION)?.toLong() ?: 0L,
                width = extractMetadata(METADATA_KEY_VIDEO_WIDTH)?.toInt() ?: 0,
                height = extractMetadata(METADATA_KEY_VIDEO_HEIGHT)?.toInt() ?: 0,
                // ... other fields
            )
        }
    }
}
```

---

For more examples and detailed implementation guides, see the [sample app](https://github.com/yourusername/AstralStream/tree/main/sample) directory.