package com.astralplayer.nextplayer.feature.cast

import android.content.Context
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.cast.*
import com.google.android.gms.cast.framework.*
import com.google.android.gms.cast.framework.media.RemoteMediaClient
import com.google.android.gms.common.api.ResultCallback
import com.google.android.gms.common.api.Status
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import android.util.Log
import com.google.android.gms.cast.framework.media.MediaIntentReceiver
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Data class representing a Cast device
 */
data class CastDevice(
    val id: String,
    val name: String,
    val modelName: String,
    val isAvailable: Boolean = true,
    val isConnected: Boolean = false,
    val volume: Float = 0.5f,
    val isMuted: Boolean = false,
    val routeInfo: MediaRouter.RouteInfo? = null
)

/**
 * Data class for Cast session information
 */
data class CastSession(
    val device: CastDevice,
    val mediaTitle: String,
    val mediaUri: String,
    val position: Long = 0L,
    val duration: Long = 0L,
    val isPlaying: Boolean = false,
    val volume: Float = 0.5f,
    val isMuted: Boolean = false,
    val startTime: Long = System.currentTimeMillis()
)

/**
 * Enum for Cast connection states
 */
enum class CastState {
    NOT_CONNECTED,
    CONNECTING,
    CONNECTED,
    DISCONNECTING,
    ERROR
}

/**
 * Data class for Cast media metadata
 */
data class CastMediaMetadata(
    val title: String,
    val subtitle: String = "",
    val description: String = "",
    val imageUrl: String = "",
    val contentType: String = "video/mp4",
    val duration: Long = 0L
)

/**
 * Manager for Google Cast (Chromecast) functionality
 */
class ChromecastManager(private val context: Context) {
    
    companion object {
        private const val TAG = "ChromecastManager"
        private const val APP_ID = CastMediaControlIntent.DEFAULT_MEDIA_RECEIVER_APPLICATION_ID
    }
    
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    private val _availableDevices = MutableStateFlow<List<CastDevice>>(emptyList())
    val availableDevices: StateFlow<List<CastDevice>> = _availableDevices.asStateFlow()
    
    private val _castState = MutableStateFlow(CastState.NOT_CONNECTED)
    val castState: StateFlow<CastState> = _castState.asStateFlow()
    
    private val _currentSession = MutableStateFlow<CastSession?>(null)
    val currentSession: StateFlow<CastSession?> = _currentSession.asStateFlow()
    
    private val _castError = MutableStateFlow<String?>(null)
    val castError: StateFlow<String?> = _castError.asStateFlow()
    
    private val _isDiscovering = MutableStateFlow(false)
    val isDiscovering: StateFlow<Boolean> = _isDiscovering.asStateFlow()
    
    private var castContext: CastContext? = null
    private var sessionManager: SessionManager? = null
    private var castSession: com.google.android.gms.cast.framework.CastSession? = null
    private var remoteMediaClient: RemoteMediaClient? = null
    private var mediaRouter: MediaRouter? = null
    private var mediaRouterCallback: MediaRouter.Callback? = null
    
    /**
     * Initialize Cast SDK and start device discovery
     */
    init {
        initializeCastSDK()
    }
    
    /**
     * Initialize Google Cast SDK
     */
    private fun initializeCastSDK() {
        try {
            // Get Cast context
            castContext = CastContext.getSharedInstance(context)
            sessionManager = castContext?.sessionManager
            
            // Set up session listener
            sessionManager?.addSessionManagerListener(sessionManagerListener, com.google.android.gms.cast.framework.CastSession::class.java)
            
            // Initialize MediaRouter for device discovery
            mediaRouter = MediaRouter.getInstance(context)
            
            // Start device discovery
            startDeviceDiscovery()
            
            Log.d(TAG, "Cast SDK initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Cast SDK", e)
            _castError.value = "Cast initialization failed: ${e.message}"
        }
    }
    
    /**
     * Session manager listener
     */
    private val sessionManagerListener = object : SessionManagerListener<com.google.android.gms.cast.framework.CastSession> {
        override fun onSessionStarting(session: com.google.android.gms.cast.framework.CastSession) {
            _castState.value = CastState.CONNECTING
        }
        
        override fun onSessionStarted(session: com.google.android.gms.cast.framework.CastSession, sessionId: String) {
            castSession = session
            remoteMediaClient = session.remoteMediaClient
            _castState.value = CastState.CONNECTED
            updateConnectedDevice(session.castDevice)
        }
        
        override fun onSessionStartFailed(session: com.google.android.gms.cast.framework.CastSession, error: Int) {
            _castState.value = CastState.ERROR
            _castError.value = "Failed to start cast session: $error"
        }
        
        override fun onSessionEnding(session: com.google.android.gms.cast.framework.CastSession) {
            _castState.value = CastState.DISCONNECTING
        }
        
        override fun onSessionEnded(session: com.google.android.gms.cast.framework.CastSession, error: Int) {
            castSession = null
            remoteMediaClient = null
            _castState.value = CastState.NOT_CONNECTED
            _currentSession.value = null
            resetDeviceConnectionStatus()
        }
        
        override fun onSessionResuming(session: com.google.android.gms.cast.framework.CastSession, sessionId: String) {}
        
        override fun onSessionResumed(session: com.google.android.gms.cast.framework.CastSession, wasSuspended: Boolean) {
            castSession = session
            remoteMediaClient = session.remoteMediaClient
            _castState.value = CastState.CONNECTED
        }
        
        override fun onSessionResumeFailed(session: com.google.android.gms.cast.framework.CastSession, error: Int) {
            _castState.value = CastState.ERROR
            _castError.value = "Failed to resume cast session: $error"
        }
        
        override fun onSessionSuspended(session: com.google.android.gms.cast.framework.CastSession, reason: Int) {}
    }
    
    /**
     * MediaRouter callback for device discovery
     */
    private fun createMediaRouterCallback() = object : MediaRouter.Callback() {
        override fun onRouteAdded(router: MediaRouter, route: MediaRouter.RouteInfo) {
            if (route.supportsControlCategory(MediaControlIntent.CATEGORY_REMOTE_PLAYBACK)) {
                addOrUpdateDevice(route)
            }
        }
        
        override fun onRouteRemoved(router: MediaRouter, route: MediaRouter.RouteInfo) {
            removeDevice(route)
        }
        
        override fun onRouteChanged(router: MediaRouter, route: MediaRouter.RouteInfo) {
            if (route.supportsControlCategory(MediaControlIntent.CATEGORY_REMOTE_PLAYBACK)) {
                addOrUpdateDevice(route)
            }
        }
        
        override fun onRouteSelected(router: MediaRouter, route: MediaRouter.RouteInfo) {
            // Route selected - handled by SessionManager
        }
        
        override fun onRouteUnselected(router: MediaRouter, route: MediaRouter.RouteInfo) {
            // Route unselected - handled by SessionManager
        }
    }
    
    /**
     * Start device discovery
     */
    fun startDeviceDiscovery() {
        _isDiscovering.value = true
        
        try {
            // Create media route selector
            val selector = MediaRouteSelector.Builder()
                .addControlCategory(MediaControlIntent.CATEGORY_REMOTE_PLAYBACK)
                .addControlCategory(CastMediaControlIntent.categoryForCast(APP_ID))
                .build()
            
            // Create and add callback
            mediaRouterCallback = createMediaRouterCallback()
            mediaRouter?.addCallback(selector, mediaRouterCallback!!, MediaRouter.CALLBACK_FLAG_REQUEST_DISCOVERY)
            
            // Get current routes
            updateAvailableDevices()
            
            scope.launch {
                delay(2000) // Discovery period
                _isDiscovering.value = false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start device discovery", e)
            _isDiscovering.value = false
            _castError.value = "Device discovery failed: ${e.message}"
        }
    }
    
    /**
     * Update available devices from MediaRouter
     */
    private fun updateAvailableDevices() {
        val routes = mediaRouter?.routes ?: return
        val devices = routes
            .filter { route ->
                route.supportsControlCategory(MediaControlIntent.CATEGORY_REMOTE_PLAYBACK) &&
                !route.isDefault
            }
            .map { route ->
                CastDevice(
                    id = route.id,
                    name = route.name,
                    modelName = route.description ?: "Cast Device",
                    isAvailable = route.isEnabled,
                    isConnected = route.isSelected,
                    volume = route.volume.toFloat() / route.volumeMax,
                    routeInfo = route
                )
            }
        
        _availableDevices.value = devices
    }
    
    /**
     * Add or update device from route info
     */
    private fun addOrUpdateDevice(route: MediaRouter.RouteInfo) {
        val device = CastDevice(
            id = route.id,
            name = route.name,
            modelName = route.description ?: "Cast Device",
            isAvailable = route.isEnabled,
            isConnected = route.isSelected,
            volume = route.volume.toFloat() / route.volumeMax,
            routeInfo = route
        )
        
        val currentDevices = _availableDevices.value.toMutableList()
        val existingIndex = currentDevices.indexOfFirst { it.id == device.id }
        
        if (existingIndex >= 0) {
            currentDevices[existingIndex] = device
        } else {
            currentDevices.add(device)
        }
        
        _availableDevices.value = currentDevices
    }
    
    /**
     * Remove device from list
     */
    private fun removeDevice(route: MediaRouter.RouteInfo) {
        _availableDevices.value = _availableDevices.value.filter { it.id != route.id }
    }
    
    /**
     * Update connected device status
     */
    private fun updateConnectedDevice(googleCastDevice: com.google.android.gms.cast.CastDevice?) {
        if (googleCastDevice == null) return
        
        _availableDevices.value = _availableDevices.value.map { device ->
            device.copy(isConnected = device.name == googleCastDevice.friendlyName)
        }
    }
    
    /**
     * Reset all device connection status
     */
    private fun resetDeviceConnectionStatus() {
        _availableDevices.value = _availableDevices.value.map { device ->
            device.copy(isConnected = false)
        }
    }
    
    /**
     * Connect to a Cast device
     */
    suspend fun connectToDevice(device: CastDevice): Boolean {
        return try {
            _castError.value = null
            _castState.value = CastState.CONNECTING
            
            // Select the route
            device.routeInfo?.let { route ->
                mediaRouter?.selectRoute(route)
            }
            
            // Wait for connection
            delay(2000)
            
            _castState.value == CastState.CONNECTED
        } catch (e: Exception) {
            Log.e(TAG, "Failed to connect to device", e)
            _castError.value = "Failed to connect: ${e.message}"
            _castState.value = CastState.ERROR
            false
        }
    }
    
    /**
     * Disconnect from current Cast device
     */
    suspend fun disconnect() {
        try {
            _castState.value = CastState.DISCONNECTING
            sessionManager?.endCurrentSession(true)
            
            delay(1000)
            
            _currentSession.value = null
            _castState.value = CastState.NOT_CONNECTED
        } catch (e: Exception) {
            Log.e(TAG, "Failed to disconnect", e)
            _castError.value = "Failed to disconnect: ${e.message}"
            _castState.value = CastState.ERROR
        }
    }
    
    /**
     * Cast media to connected device
     */
    suspend fun castMedia(
        mediaUri: Uri,
        metadata: CastMediaMetadata
    ): Boolean = suspendCancellableCoroutine { continuation ->
        try {
            val mediaMetadata = MediaMetadata(MediaMetadata.MEDIA_TYPE_MOVIE).apply {
                putString(MediaMetadata.KEY_TITLE, metadata.title)
                putString(MediaMetadata.KEY_SUBTITLE, metadata.subtitle)
                if (metadata.imageUrl.isNotEmpty()) {
                    addImage(WebImage(Uri.parse(metadata.imageUrl)))
                }
            }
            
            val mediaInfo = MediaInfo.Builder(mediaUri.toString())
                .setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
                .setContentType(metadata.contentType)
                .setMetadata(mediaMetadata)
                .setStreamDuration(metadata.duration)
                .build()
            
            val loadOptions = MediaLoadOptions.Builder()
                .setAutoplay(true)
                .setPlayPosition(0)
                .build()
            
            remoteMediaClient?.load(mediaInfo, loadOptions)?.setResultCallback { mediaChannelResult ->
                if (mediaChannelResult.status.isSuccess) {
                    val connectedDevice = _availableDevices.value.find { it.isConnected }
                    connectedDevice?.let {
                        _currentSession.value = CastSession(
                            device = it,
                            mediaTitle = metadata.title,
                            mediaUri = mediaUri.toString(),
                            duration = metadata.duration,
                            isPlaying = true
                        )
                    }
                    continuation.resume(true)
                } else {
                    _castError.value = "Failed to load media: ${mediaChannelResult.status.statusMessage}"
                    continuation.resume(false)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to cast media", e)
            _castError.value = "Failed to cast media: ${e.message}"
            continuation.resume(false)
        }
    }
    
    /**
     * Control media playback on Cast device
     */
    suspend fun playPause() {
        try {
            val isPlaying = remoteMediaClient?.isPlaying ?: false
            
            if (isPlaying) {
                remoteMediaClient?.pause()
            } else {
                remoteMediaClient?.play()
            }
            
            _currentSession.value = _currentSession.value?.copy(isPlaying = !isPlaying)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to control playback", e)
            _castError.value = "Failed to control playback: ${e.message}"
        }
    }
    
    /**
     * Seek to position on Cast device
     */
    suspend fun seekTo(position: Long) {
        try {
            remoteMediaClient?.seek(
                MediaSeekOptions.Builder()
                    .setPosition(position)
                    .build()
            )
            
            _currentSession.value = _currentSession.value?.copy(position = position)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to seek", e)
            _castError.value = "Failed to seek: ${e.message}"
        }
    }
    
    /**
     * Set volume on Cast device
     */
    suspend fun setVolume(volume: Float) {
        try {
            castSession?.volume = volume.toDouble()
            
            _currentSession.value = _currentSession.value?.copy(volume = volume)
            
            // Update device volume
            _availableDevices.value = _availableDevices.value.map { device ->
                if (device.isConnected) {
                    device.copy(volume = volume)
                } else {
                    device
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set volume", e)
            _castError.value = "Failed to set volume: ${e.message}"
        }
    }
    
    /**
     * Mute/unmute Cast device
     */
    suspend fun toggleMute() {
        try {
            val isMuted = castSession?.isMute ?: false
            castSession?.isMute = !isMuted
            
            _currentSession.value = _currentSession.value?.copy(isMuted = !isMuted)
            
            // Update device mute status
            _availableDevices.value = _availableDevices.value.map { device ->
                if (device.isConnected) {
                    device.copy(isMuted = !isMuted)
                } else {
                    device
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to toggle mute", e)
            _castError.value = "Failed to toggle mute: ${e.message}"
        }
    }
    
    /**
     * Stop casting current media
     */
    suspend fun stopCasting() {
        try {
            remoteMediaClient?.stop()
            _currentSession.value = null
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop casting", e)
            _castError.value = "Failed to stop casting: ${e.message}"
        }
    }
    
    /**
     * Clear Cast error
     */
    fun clearError() {
        _castError.value = null
    }
    
    /**
     * Check if Cast is available
     */
    fun isCastAvailable(): Boolean {
        return _availableDevices.value.any { it.isAvailable }
    }
    
    /**
     * Check if currently connected to a Cast device
     */
    fun isConnected(): Boolean {
        return _castState.value == CastState.CONNECTED
    }
    
    /**
     * Clean up resources
     */
    fun cleanup() {
        mediaRouterCallback?.let {
            mediaRouter?.removeCallback(it)
        }
        sessionManager?.removeSessionManagerListener(sessionManagerListener, com.google.android.gms.cast.framework.CastSession::class.java)
    }
}

/**
 * ViewModel for Cast functionality
 */
class ChromecastViewModel(private val castManager: ChromecastManager) : ViewModel() {
    
    val availableDevices = castManager.availableDevices
    val castState = castManager.castState
    val currentSession = castManager.currentSession
    val castError = castManager.castError
    val isDiscovering = castManager.isDiscovering
    
    fun startDeviceDiscovery() {
        castManager.startDeviceDiscovery()
    }
    
    fun connectToDevice(device: CastDevice) {
        viewModelScope.launch {
            castManager.connectToDevice(device)
        }
    }
    
    fun disconnect() {
        viewModelScope.launch {
            castManager.disconnect()
        }
    }
    
    fun castMedia(mediaUri: Uri, metadata: CastMediaMetadata) {
        viewModelScope.launch {
            castManager.castMedia(mediaUri, metadata)
        }
    }
    
    fun playPause() {
        viewModelScope.launch {
            castManager.playPause()
        }
    }
    
    fun seekTo(position: Long) {
        viewModelScope.launch {
            castManager.seekTo(position)
        }
    }
    
    fun setVolume(volume: Float) {
        viewModelScope.launch {
            castManager.setVolume(volume)
        }
    }
    
    fun toggleMute() {
        viewModelScope.launch {
            castManager.toggleMute()
        }
    }
    
    fun stopCasting() {
        viewModelScope.launch {
            castManager.stopCasting()
        }
    }
    
    fun clearError() {
        castManager.clearError()
    }
    
    override fun onCleared() {
        super.onCleared()
        castManager.cleanup()
    }
}

/**
 * Composable for Cast button in player controls
 */
@Composable
fun CastButton(
    isAvailable: Boolean,
    isConnected: Boolean,
    onCastClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (isAvailable) {
        IconButton(
            onClick = onCastClick,
            modifier = modifier
        ) {
            Icon(
                imageVector = if (isConnected) Icons.Default.CastConnected else Icons.Default.Cast,
                contentDescription = if (isConnected) "Connected to Cast device" else "Cast to device",
                tint = if (isConnected) Color(0xFF00BCD4) else Color.White
            )
        }
    }
}

/**
 * Composable for Cast device selection dialog
 */
@Composable
fun CastDeviceDialog(
    devices: List<CastDevice>,
    castState: CastState,
    isDiscovering: Boolean,
    onDeviceSelected: (CastDevice) -> Unit,
    onRefresh: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Cast to device",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                
                IconButton(onClick = onRefresh) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = "Refresh devices",
                        tint = Color(0xFF00BCD4)
                    )
                }
            }
        },
        text = {
            Column {
                if (isDiscovering) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            color = Color(0xFF00BCD4),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Discovering devices...",
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                }
                
                if (devices.isEmpty() && !isDiscovering) {
                    Text(
                        text = "No Cast devices found",
                        color = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.padding(16.dp)
                    )
                } else {
                    devices.forEach { device ->
                        CastDeviceItem(
                            device = device,
                            isConnecting = castState == CastState.CONNECTING,
                            onClick = { onDeviceSelected(device) }
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

/**
 * Cast device item in selection dialog
 */
@Composable
private fun CastDeviceItem(
    device: CastDevice,
    isConnecting: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = device.isAvailable && !isConnecting) { onClick() }
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (device.isConnected) Color(0xFF00BCD4).copy(alpha = 0.2f)
            else Color.White.copy(alpha = 0.05f)
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Device icon
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        if (device.isConnected) Color(0xFF00BCD4).copy(alpha = 0.3f)
                        else Color.White.copy(alpha = 0.1f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when {
                        device.modelName.contains("TV") -> Icons.Default.Tv
                        device.modelName.contains("Hub") -> Icons.Default.Speaker
                        else -> Icons.Default.Cast
                    },
                    contentDescription = device.modelName,
                    tint = if (device.isConnected) Color(0xFF00BCD4) else Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.size(24.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Device info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = device.name,
                    color = if (device.isAvailable) Color.White else Color.White.copy(alpha = 0.5f),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Text(
                    text = device.modelName,
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            // Connection status
            when {
                device.isConnected -> {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = "Connected",
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(24.dp)
                    )
                }
                isConnecting -> {
                    CircularProgressIndicator(
                        color = Color(0xFF00BCD4),
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                }
                !device.isAvailable -> {
                    Icon(
                        Icons.Default.Block,
                        contentDescription = "Unavailable",
                        tint = Color.White.copy(alpha = 0.3f),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

/**
 * Composable for Cast session controls
 */
@Composable
fun CastSessionControls(
    session: CastSession,
    onPlayPause: () -> Unit,
    onSeek: (Long) -> Unit,
    onVolumeChange: (Float) -> Unit,
    onMuteToggle: () -> Unit,
    onStopCasting: () -> Unit,
    onDisconnect: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color.Black.copy(alpha = 0.8f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header with device info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Casting to ${session.device.name}",
                        color = Color(0xFF00BCD4),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = session.mediaTitle,
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                
                Row {
                    IconButton(onClick = onStopCasting) {
                        Icon(
                            Icons.Default.Stop,
                            contentDescription = "Stop casting",
                            tint = Color.White.copy(alpha = 0.7f)
                        )
                    }
                    
                    IconButton(onClick = onDisconnect) {
                        Icon(
                            Icons.Default.CastConnected,
                            contentDescription = "Disconnect",
                            tint = Color(0xFF00BCD4)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Playback controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { onSeek(session.position - 10000) }) {
                    Icon(
                        Icons.Default.Replay10,
                        contentDescription = "Rewind 10s",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
                
                IconButton(onClick = onPlayPause) {
                    Icon(
                        imageVector = if (session.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (session.isPlaying) "Pause" else "Play",
                        tint = Color.White,
                        modifier = Modifier.size(48.dp)
                    )
                }
                
                IconButton(onClick = { onSeek(session.position + 10000) }) {
                    Icon(
                        Icons.Default.Forward10,
                        contentDescription = "Forward 10s",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Volume control
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onMuteToggle) {
                    Icon(
                        imageVector = if (session.isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                        contentDescription = if (session.isMuted) "Unmute" else "Mute",
                        tint = Color.White
                    )
                }
                
                Slider(
                    value = if (session.isMuted) 0f else session.volume,
                    onValueChange = onVolumeChange,
                    modifier = Modifier.weight(1f),
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFF00BCD4),
                        activeTrackColor = Color(0xFF00BCD4),
                        inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                    )
                )
                
                Text(
                    text = "${(session.volume * 100).toInt()}%",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 12.sp,
                    modifier = Modifier.width(40.dp)
                )
            }
        }
    }
}