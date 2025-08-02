package com.astralstream.nextplayer.viewmodels

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.ui.PlayerView
import com.astralstream.nextplayer.analytics.AnalyticsDashboardEngine
import com.astralstream.nextplayer.feature.player.enhancedplayer.EnhancedVideoPlayer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class VideoPlayerViewModel @Inject constructor(
    private val enhancedVideoPlayer: EnhancedVideoPlayer,
    private val analyticsEngine: AnalyticsDashboardEngine
) : ViewModel() {
    
    private var videoStartTime = 0L
    private var currentVideoUri = ""
    private var currentVideoTitle = ""
    
    fun initializePlayer(videoUri: String, videoTitle: String) {
        videoStartTime = System.currentTimeMillis()
        currentVideoUri = videoUri
        currentVideoTitle = videoTitle
        
        // Extract headers if URL contains authentication
        val headers = extractHeaders(videoUri)
        val cleanUri = Uri.parse(videoUri.split("?")[0])
        
        enhancedVideoPlayer.playVideo(cleanUri, videoTitle, headers)
    }
    
    fun setupPlayerView(playerView: PlayerView) {
        enhancedVideoPlayer.initializePlayer(playerView)
    }
    
    fun releasePlayer() {
        // Track analytics
        val watchDuration = System.currentTimeMillis() - videoStartTime
        
        viewModelScope.launch {
            analyticsEngine.recordVideoWatch(
                videoUri = currentVideoUri,
                videoTitle = currentVideoTitle,
                watchedDuration = watchDuration,
                totalDuration = 0L // Will be updated with actual duration in production
            )
        }
        
        enhancedVideoPlayer.release()
    }
    
    private fun extractHeaders(url: String): Map<String, String> {
        val headers = mutableMapOf<String, String>()
        
        // Extract cookies or auth tokens from URL
        if (url.contains("auth=")) {
            val auth = url.substringAfter("auth=").substringBefore("&")
            headers["Authorization"] = "Bearer $auth"
        }
        
        // Add referer for sites that check it
        val baseUrl = try {
            val uri = Uri.parse(url)
            "${uri.scheme}://${uri.host}"
        } catch (e: Exception) {
            ""
        }
        
        if (baseUrl.isNotEmpty()) {
            headers["Referer"] = baseUrl
        }
        
        // Add user agent for better compatibility
        headers["User-Agent"] = "AstralStream/1.0 (Android)"
        
        return headers
    }
}