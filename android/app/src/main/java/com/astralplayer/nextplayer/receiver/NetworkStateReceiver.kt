package com.astralplayer.nextplayer.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.astralplayer.nextplayer.data.NetworkInfo

/**
 * Monitors network connectivity changes for streaming optimization
 */
class NetworkStateReceiver : BroadcastReceiver() {
    
    companion object {
        private const val TAG = "NetworkStateReceiver"
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        if (ConnectivityManager.CONNECTIVITY_ACTION == intent.action) {
            Log.d(TAG, "Network connectivity changed")
            
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val networkInfo = getNetworkInfo(connectivityManager)
            
            when {
                networkInfo.isConnected -> {
                    Log.d(TAG, "Connected to ${networkInfo.connectionType}")
                    handleNetworkConnected(context, networkInfo)
                }
                else -> {
                    Log.d(TAG, "Network disconnected")
                    handleNetworkDisconnected(context)
                }
            }
        }
    }
    
    private fun getNetworkInfo(connectivityManager: ConnectivityManager): NetworkInfo {
        val activeNetwork = connectivityManager.activeNetwork
        val networkCapabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
        
        return when {
            networkCapabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true -> {
                NetworkInfo(
                    connectionType = NetworkInfo.ConnectionType.WIFI,
                    isConnected = true,
                    isMetered = false,
                    bandwidth = estimateWifiBandwidth(networkCapabilities)
                )
            }
            networkCapabilities?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true -> {
                NetworkInfo(
                    connectionType = NetworkInfo.ConnectionType.CELLULAR,
                    isConnected = true,
                    isMetered = true,
                    bandwidth = estimateCellularBandwidth(networkCapabilities)
                )
            }
            networkCapabilities?.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) == true -> {
                NetworkInfo(
                    connectionType = NetworkInfo.ConnectionType.ETHERNET,
                    isConnected = true,
                    isMetered = false,
                    bandwidth = 100_000_000L // Assume 100Mbps for ethernet
                )
            }
            else -> NetworkInfo.DISCONNECTED
        }
    }
    
    private fun estimateWifiBandwidth(capabilities: NetworkCapabilities): Long {
        // Try to get actual bandwidth, fallback to estimate
        return when {
            capabilities.linkDownstreamBandwidthKbps > 0 -> 
                capabilities.linkDownstreamBandwidthKbps * 1000L
            else -> 50_000_000L // Default 50Mbps for WiFi
        }
    }
    
    private fun estimateCellularBandwidth(capabilities: NetworkCapabilities): Long {
        // Try to get actual bandwidth, fallback to estimate
        return when {
            capabilities.linkDownstreamBandwidthKbps > 0 -> 
                capabilities.linkDownstreamBandwidthKbps * 1000L
            else -> 10_000_000L // Default 10Mbps for cellular
        }
    }
    
    private fun handleNetworkConnected(context: Context, networkInfo: NetworkInfo) {
        // TODO: Notify streaming services about network change
        // This could trigger quality adjustment in active streams
        Log.d(TAG, "Network connected: ${networkInfo.getDisplayName()}")
        
        // Could broadcast to streaming optimization service
        val intent = Intent("com.astralplayer.nextplayer.NETWORK_CONNECTED").apply {
            putExtra("network_type", networkInfo.connectionType.name)
            putExtra("is_metered", networkInfo.isMetered)
            putExtra("bandwidth", networkInfo.bandwidth)
        }
        context.sendBroadcast(intent)
    }
    
    private fun handleNetworkDisconnected(context: Context) {
        // TODO: Handle network disconnection
        // Could pause streaming or cache current content
        Log.d(TAG, "Network disconnected")
        
        val intent = Intent("com.astralplayer.nextplayer.NETWORK_DISCONNECTED")
        context.sendBroadcast(intent)
    }
}