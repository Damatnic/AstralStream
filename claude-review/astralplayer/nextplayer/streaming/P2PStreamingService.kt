package com.astralplayer.nextplayer.streaming

import android.content.Context
import androidx.media3.common.util.UnstableApi
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.net.*
import javax.inject.Inject
import javax.inject.Singleton

@UnstableApi
@Singleton
class P2PStreamingService @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    
    private val _p2pStatus = MutableStateFlow(P2PStatus())
    val p2pStatus: StateFlow<P2PStatus> = _p2pStatus.asStateFlow()
    
    private val _connectedPeers = MutableStateFlow<List<P2PPeer>>(emptyList())
    val connectedPeers: StateFlow<List<P2PPeer>> = _connectedPeers.asStateFlow()
    
    private var isEnabled = false
    private var discoveryJob: Job? = null
    private var serverSocket: ServerSocket? = null
    
    companion object {
        private const val P2P_PORT = 8080
        private const val DISCOVERY_PORT = 8081
        private const val DISCOVERY_INTERVAL = 30000L // 30 seconds
    }
    
    fun isAvailable(): Boolean {
        return isEnabled && isOnLocalNetwork()
    }
    
    fun enable() {
        if (isEnabled) return
        
        isEnabled = true
        startP2PService()
        _p2pStatus.value = _p2pStatus.value.copy(isEnabled = true)
    }
    
    fun disable() {
        if (!isEnabled) return
        
        isEnabled = false
        stopP2PService()
        _p2pStatus.value = _p2pStatus.value.copy(
            isEnabled = false,
            isListening = false
        )
    }
    
    private fun startP2PService() {
        scope.launch {
            try {
                startListening()
                startPeerDiscovery()
                _p2pStatus.value = _p2pStatus.value.copy(isListening = true)
            } catch (e: Exception) {
                _p2pStatus.value = _p2pStatus.value.copy(
                    isListening = false,
                    lastError = e.message
                )
            }
        }
    }
    
    private suspend fun startListening() {
        withContext(Dispatchers.IO) {
            try {
                serverSocket = ServerSocket(P2P_PORT)
                
                while (isEnabled && serverSocket?.isClosed == false) {
                    val clientSocket = serverSocket?.accept()
                    clientSocket?.let { socket ->
                        handlePeerConnection(socket)
                    }
                }
            } catch (e: Exception) {
                if (isEnabled) { // Only log if not intentionally stopped
                    _p2pStatus.value = _p2pStatus.value.copy(lastError = e.message)
                }
            }
        }
    }
    
    private fun handlePeerConnection(socket: Socket) {
        scope.launch {
            try {
                val peer = P2PPeer(
                    id = socket.inetAddress.hostAddress ?: "unknown",
                    address = socket.inetAddress.hostAddress ?: "unknown",
                    port = socket.port,
                    connectionTime = System.currentTimeMillis(),
                    isConnected = true
                )
                
                addPeer(peer)
                
                // Handle peer communication
                val inputStream = socket.getInputStream()
                val outputStream = socket.getOutputStream()
                
                // Simple protocol for P2P communication
                val buffer = ByteArray(1024)
                while (socket.isConnected && !socket.isClosed) {
                    val bytesRead = inputStream.read(buffer)
                    if (bytesRead > 0) {
                        // Process P2P data
                        processPeerData(peer, buffer, bytesRead)
                    }
                }
                
            } catch (e: Exception) {
                // Handle connection error
            } finally {
                socket.close()
            }
        }
    }
    
    private fun processPeerData(peer: P2PPeer, data: ByteArray, length: Int) {
        // Process received data from peer
        val currentPeers = _connectedPeers.value.toMutableList()
        val peerIndex = currentPeers.indexOfFirst { it.id == peer.id }
        
        if (peerIndex >= 0) {
            currentPeers[peerIndex] = currentPeers[peerIndex].copy(
                bytesReceived = currentPeers[peerIndex].bytesReceived + length,
                lastActivity = System.currentTimeMillis()
            )
            _connectedPeers.value = currentPeers
        }
    }
    
    private fun startPeerDiscovery() {
        discoveryJob?.cancel()
        discoveryJob = scope.launch {
            while (isEnabled) {
                try {
                    discoverPeers()
                    delay(DISCOVERY_INTERVAL)
                } catch (e: Exception) {
                    _p2pStatus.value = _p2pStatus.value.copy(lastError = e.message)
                }
            }
        }
    }
    
    private suspend fun discoverPeers() {
        withContext(Dispatchers.IO) {
            try {
                val socket = DatagramSocket()
                socket.broadcast = true
                
                val discoveryMessage = "ASTRALSTREAM_P2P_DISCOVERY".toByteArray()
                val packet = DatagramPacket(
                    discoveryMessage,
                    discoveryMessage.size,
                    InetAddress.getByName("255.255.255.255"),
                    DISCOVERY_PORT
                )
                
                socket.send(packet)
                socket.close()
                
                _p2pStatus.value = _p2pStatus.value.copy(
                    lastDiscovery = System.currentTimeMillis(),
                    discoveryCount = _p2pStatus.value.discoveryCount + 1
                )
                
            } catch (e: Exception) {
                _p2pStatus.value = _p2pStatus.value.copy(lastError = e.message)
            }
        }
    }
    
    fun checkForLocalPeers(contentId: String) {
        if (!isEnabled) return
        
        scope.launch {
            try {
                // Check if any connected peers have the requested content
                val peersWithContent = _connectedPeers.value.filter { peer ->
                    hasContent(peer, contentId)
                }
                
                if (peersWithContent.isNotEmpty()) {
                    _p2pStatus.value = _p2pStatus.value.copy(
                        availableContent = _p2pStatus.value.availableContent + contentId
                    )
                }
                
            } catch (e: Exception) {
                _p2pStatus.value = _p2pStatus.value.copy(lastError = e.message)
            }
        }
    }
    
    private suspend fun hasContent(peer: P2PPeer, contentId: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                // Simple content availability check
                val socket = Socket(peer.address, peer.port)
                val outputStream = socket.getOutputStream()
                val inputStream = socket.getInputStream()
                
                // Send content request
                val request = "HAS_CONTENT:$contentId".toByteArray()
                outputStream.write(request)
                outputStream.flush()
                
                // Read response
                val response = ByteArray(1024)
                val bytesRead = inputStream.read(response)
                
                socket.close()
                
                if (bytesRead > 0) {
                    val responseStr = String(response, 0, bytesRead)
                    responseStr == "YES"
                } else false
                
            } catch (e: Exception) {
                false
            }
        }
    }
    
    private fun addPeer(peer: P2PPeer) {
        val currentPeers = _connectedPeers.value.toMutableList()
        val existingIndex = currentPeers.indexOfFirst { it.id == peer.id }
        
        if (existingIndex >= 0) {
            currentPeers[existingIndex] = peer
        } else {
            currentPeers.add(peer)
        }
        
        _connectedPeers.value = currentPeers
        
        _p2pStatus.value = _p2pStatus.value.copy(
            connectedPeers = currentPeers.size,
            totalPeersDiscovered = _p2pStatus.value.totalPeersDiscovered + 1
        )
    }
    
    private fun removePeer(peerId: String) {
        val currentPeers = _connectedPeers.value.toMutableList()
        currentPeers.removeAll { it.id == peerId }
        _connectedPeers.value = currentPeers
        
        _p2pStatus.value = _p2pStatus.value.copy(
            connectedPeers = currentPeers.size
        )
    }
    
    private fun isOnLocalNetwork(): Boolean {
        return try {
            val networkInterfaces = NetworkInterface.getNetworkInterfaces()
            networkInterfaces.toList().any { networkInterface ->
                networkInterface.inetAddresses.toList().any { address ->
                    !address.isLoopbackAddress && 
                    address.isSiteLocalAddress &&
                    address is Inet4Address
                }
            }
        } catch (e: Exception) {
            false
        }
    }
    
    private fun stopP2PService() {
        discoveryJob?.cancel()
        discoveryJob = null
        
        serverSocket?.close()
        serverSocket = null
        
        _connectedPeers.value = emptyList()
    }
    
    fun disconnect() {
        disable()
    }
    
    /**
     * Get P2P statistics
     */
    fun getP2PStats(): P2PStats {
        val peers = _connectedPeers.value
        val totalBytesReceived = peers.sumOf { it.bytesReceived }
        val totalBytesSent = peers.sumOf { it.bytesSent }
        
        return P2PStats(
            connectedPeers = peers.size,
            totalBytesReceived = totalBytesReceived,
            totalBytesSent = totalBytesSent,
            discoveryCount = _p2pStatus.value.discoveryCount,
            avgConnectionTime = if (peers.isNotEmpty()) {
                peers.map { System.currentTimeMillis() - it.connectionTime }.average().toLong()
            } else 0L
        )
    }
    
    fun release() {
        disable()
        scope.cancel()
    }
    
    // Data classes
    data class P2PStatus(
        val isEnabled: Boolean = false,
        val isListening: Boolean = false,
        val connectedPeers: Int = 0,
        val totalPeersDiscovered: Int = 0,
        val lastDiscovery: Long = 0,
        val discoveryCount: Int = 0,
        val availableContent: List<String> = emptyList(),
        val lastError: String? = null
    )
    
    data class P2PPeer(
        val id: String,
        val address: String,
        val port: Int,
        val connectionTime: Long,
        val lastActivity: Long = System.currentTimeMillis(),
        val isConnected: Boolean = false,
        val bytesReceived: Long = 0,
        val bytesSent: Long = 0
    )
    
    data class P2PStats(
        val connectedPeers: Int,
        val totalBytesReceived: Long,
        val totalBytesSent: Long,
        val discoveryCount: Int,
        val avgConnectionTime: Long
    )
}