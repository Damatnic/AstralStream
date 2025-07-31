package com.astralplayer.nextplayer.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.delay

data class CastDevice(
    val id: String,
    val name: String,
    val modelName: String,
    val isAvailable: Boolean = true,
    val isConnected: Boolean = false,
    val volume: Float = 0.5f
)

data class CastState(
    val isConnected: Boolean = false,
    val deviceName: String? = null,
    val isCasting: Boolean = false,
    val castError: String? = null,
    val availableDevices: List<CastDevice> = emptyList(),
    val isDiscovering: Boolean = false,
    val currentVolume: Float = 0.5f,
    val currentPosition: Long = 0L,
    val duration: Long = 0L,
    val isPlaying: Boolean = false
)

data class CastMediaInfo(
    val contentId: String,
    val contentType: String,
    val title: String,
    val subtitle: String? = null,
    val imageUrl: String? = null,
    val duration: Long = 0L
)

interface ChromecastService {
    val castState: StateFlow<CastState>
    
    suspend fun startDiscovery(): Result<Unit>
    suspend fun stopDiscovery(): Result<Unit>
    suspend fun connectToDevice(device: CastDevice): Result<Unit>
    suspend fun disconnect(): Result<Unit>
    suspend fun loadMedia(mediaInfo: CastMediaInfo): Result<Unit>
    suspend fun play(): Result<Unit>
    suspend fun pause(): Result<Unit>
    suspend fun seekTo(position: Long): Result<Unit>
    suspend fun setVolume(volume: Float): Result<Unit>
    suspend fun stopCasting(): Result<Unit>
    
    fun getAvailableDevices(): List<CastDevice>
    fun isConnected(): Boolean
    fun getCurrentDevice(): CastDevice?
}

class ChromecastServiceImpl constructor(
    private val context: Context
) : ChromecastService {
    
    private val _castState = MutableStateFlow(CastState())
    override val castState: StateFlow<CastState> = _castState.asStateFlow()
    
    private var currentDevice: CastDevice? = null
    private var isDiscoveryActive = false
    
    override suspend fun startDiscovery(): Result<Unit> {
        return try {
            updateCastState { copy(isDiscovering = true, castError = null) }
            
            // Simulate device discovery
            delay(1500)
            
            val mockDevices = listOf(
                CastDevice(
                    id = "cast_device_1",
                    name = "Living Room TV",
                    modelName = "Chromecast with Google TV",
                    isAvailable = true
                ),
                CastDevice(
                    id = "cast_device_2",
                    name = "Bedroom Chromecast",
                    modelName = "Chromecast (3rd Gen)",
                    isAvailable = true
                )
            )
            
            updateCastState { 
                copy(
                    isDiscovering = false,
                    availableDevices = mockDevices
                )
            }
            
            isDiscoveryActive = true
            Result.success(Unit)
        } catch (e: Exception) {
            updateCastState { 
                copy(
                    isDiscovering = false,
                    castError = "Discovery failed: ${e.message}"
                )
            }
            Result.failure(e)
        }
    }
    
    override suspend fun stopDiscovery(): Result<Unit> {
        return try {
            isDiscoveryActive = false
            updateCastState { 
                copy(
                    isDiscovering = false,
                    availableDevices = emptyList()
                )
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun connectToDevice(device: CastDevice): Result<Unit> {
        return try {
            if (!device.isAvailable) {
                return Result.failure(Exception("Device is not available"))
            }
            
            updateCastState { copy(castError = null) }
            
            // Simulate connection process
            delay(2000)
            
            currentDevice = device.copy(isConnected = true)
            
            updateCastState { 
                copy(
                    isConnected = true,
                    deviceName = device.name,
                    availableDevices = availableDevices.map { 
                        if (it.id == device.id) it.copy(isConnected = true) else it
                    }
                )
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            updateCastState { 
                copy(
                    castError = "Connection failed: ${e.message}"
                )
            }
            Result.failure(e)
        }
    }
    
    override suspend fun disconnect(): Result<Unit> {
        return try {
            delay(500)
            
            val deviceId = currentDevice?.id
            currentDevice = null
            
            updateCastState { 
                copy(
                    isConnected = false,
                    deviceName = null,
                    isCasting = false,
                    isPlaying = false,
                    availableDevices = availableDevices.map { 
                        if (it.id == deviceId) it.copy(isConnected = false) else it
                    }
                )
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            updateCastState { 
                copy(
                    castError = "Disconnection failed: ${e.message}"
                )
            }
            Result.failure(e)
        }
    }
    
    override suspend fun loadMedia(mediaInfo: CastMediaInfo): Result<Unit> {
        return try {
            if (!isConnected()) {
                return Result.failure(Exception("Not connected to any Cast device"))
            }
            
            delay(1500)
            
            updateCastState { 
                copy(
                    isCasting = true,
                    duration = mediaInfo.duration,
                    currentPosition = 0L,
                    isPlaying = true
                )
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            updateCastState { 
                copy(
                    castError = "Media loading failed: ${e.message}"
                )
            }
            Result.failure(e)
        }
    }
    
    override suspend fun play(): Result<Unit> {
        return try {
            if (!isCasting()) {
                return Result.failure(Exception("No media is being cast"))
            }
            
            delay(200)
            updateCastState { copy(isPlaying = true) }
            Result.success(Unit)
        } catch (e: Exception) {
            updateCastState { 
                copy(castError = "Play failed: ${e.message}")
            }
            Result.failure(e)
        }
    }
    
    override suspend fun pause(): Result<Unit> {
        return try {
            if (!isCasting()) {
                return Result.failure(Exception("No media is being cast"))
            }
            
            delay(200)
            updateCastState { copy(isPlaying = false) }
            Result.success(Unit)
        } catch (e: Exception) {
            updateCastState { 
                copy(castError = "Pause failed: ${e.message}")
            }
            Result.failure(e)
        }
    }
    
    override suspend fun seekTo(position: Long): Result<Unit> {
        return try {
            if (!isCasting()) {
                return Result.failure(Exception("No media is being cast"))
            }
            
            delay(300)
            updateCastState { 
                copy(currentPosition = position.coerceIn(0L, duration))
            }
            Result.success(Unit)
        } catch (e: Exception) {
            updateCastState { 
                copy(castError = "Seek failed: ${e.message}")
            }
            Result.failure(e)
        }
    }
    
    override suspend fun setVolume(volume: Float): Result<Unit> {
        return try {
            if (!isConnected()) {
                return Result.failure(Exception("Not connected to any Cast device"))
            }
            
            delay(200)
            val clampedVolume = volume.coerceIn(0f, 1f)
            
            updateCastState { copy(currentVolume = clampedVolume) }
            currentDevice = currentDevice?.copy(volume = clampedVolume)
            
            Result.success(Unit)
        } catch (e: Exception) {
            updateCastState { 
                copy(castError = "Volume change failed: ${e.message}")
            }
            Result.failure(e)
        }
    }
    
    override suspend fun stopCasting(): Result<Unit> {
        return try {
            if (!isCasting()) {
                return Result.failure(Exception("No media is being cast"))
            }
            
            delay(500)
            updateCastState { 
                copy(
                    isCasting = false,
                    isPlaying = false,
                    currentPosition = 0L,
                    duration = 0L
                )
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            updateCastState { 
                copy(castError = "Stop casting failed: ${e.message}")
            }
            Result.failure(e)
        }
    }
    
    override fun getAvailableDevices(): List<CastDevice> {
        return _castState.value.availableDevices
    }
    
    override fun isConnected(): Boolean {
        return _castState.value.isConnected
    }
    
    override fun getCurrentDevice(): CastDevice? {
        return currentDevice
    }
    
    private fun isCasting(): Boolean {
        return _castState.value.isCasting
    }
    
    private fun updateCastState(update: CastState.() -> CastState) {
        _castState.value = _castState.value.update()
    }
}

class CastManager constructor(
    private val chromecastService: ChromecastService
) {
    
    val castState: StateFlow<CastState> = chromecastService.castState
    
    suspend fun initializeCasting(): Result<Unit> {
        return chromecastService.startDiscovery()
    }
    
    suspend fun connectAndCast(
        device: CastDevice,
        videoUri: String,
        title: String,
        subtitle: String? = null,
        imageUrl: String? = null,
        duration: Long = 0L
    ): Result<Unit> {
        return try {
            val connectionResult = chromecastService.connectToDevice(device)
            if (connectionResult.isFailure) {
                return connectionResult
            }
            
            val mediaInfo = CastMediaInfo(
                contentId = videoUri,
                contentType = getContentType(videoUri),
                title = title,
                subtitle = subtitle,
                imageUrl = imageUrl,
                duration = duration
            )
            
            chromecastService.loadMedia(mediaInfo)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun togglePlayPause(): Result<Unit> {
        return if (castState.value.isPlaying) {
            chromecastService.pause()
        } else {
            chromecastService.play()
        }
    }
    
    suspend fun seekTo(position: Long): Result<Unit> {
        return chromecastService.seekTo(position)
    }
    
    suspend fun adjustVolume(volume: Float): Result<Unit> {
        return chromecastService.setVolume(volume)
    }
    
    suspend fun stopCasting(): Result<Unit> {
        return chromecastService.stopCasting()
    }
    
    suspend fun disconnect(): Result<Unit> {
        return chromecastService.disconnect()
    }
    
    fun getAvailableDevices(): List<CastDevice> {
        return chromecastService.getAvailableDevices()
    }
    
    fun isConnected(): Boolean {
        return chromecastService.isConnected()
    }
    
    fun isCasting(): Boolean {
        return castState.value.isCasting
    }
    
    private fun getContentType(videoUri: String): String {
        return when {
            videoUri.contains(".mp4") -> "video/mp4"
            videoUri.contains(".mkv") -> "video/x-matroska"
            videoUri.contains(".webm") -> "video/webm"
            videoUri.contains(".m3u8") -> "application/x-mpegURL"
            videoUri.contains(".mpd") -> "application/dash+xml"
            else -> "video/*"
        }
    }
    
    suspend fun cleanup() {
        try {
            if (isCasting()) {
                stopCasting()
            }
            if (isConnected()) {
                disconnect()
            }
            chromecastService.stopDiscovery()
        } catch (e: Exception) {
            // Log error but don't throw
        }
    }
}