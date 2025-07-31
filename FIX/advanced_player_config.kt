package com.astralplayer.astralstream.player

import android.content.Context
import androidx.media3.common.C
import androidx.media3.common.TrackSelectionParameters
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.LoadControl
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.exoplayer.trackselection.TrackSelector
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Advanced configuration for AstralStream video player
 * DO NOT MODIFY WITHOUT UNDERSTANDING THE IMPLICATIONS
 */
@UnstableApi
@Singleton
class AdvancedPlayerConfiguration @Inject constructor(
    private val context: Context
) {
    
    companion object {
        // Cache configuration
        private const val CACHE_SIZE = 500L * 1024 * 1024 // 500MB
        private const val CACHE_DIR_NAME = "astralstream_cache"
        
        // Buffer configuration
        private const val MIN_BUFFER_MS = 15000 // 15 seconds
        private const val MAX_BUFFER_MS = 60000 // 60 seconds
        private const val MIN_PLAYBACK_START_BUFFER = 2500 // 2.5 seconds
        private const val MIN_PLAYBACK_RESUME_BUFFER = 5000 // 5 seconds
        
        // Network configuration
        private const val CONNECT_TIMEOUT_MS = 15000
        private const val READ_TIMEOUT_MS = 15000
        
        // Player name
        const val PLAYER_USER_AGENT = "AstralStream/1.0 (Linux; Android) ExoPlayer/2.19"
    }
    
    private var simpleCache: SimpleCache? = null
    
    /**
     * Creates an advanced ExoPlayer instance with optimized settings
     */
    fun createAdvancedPlayer(): ExoPlayer {
        val trackSelector = createTrackSelector()
        val loadControl = createLoadControl()
        
        return ExoPlayer.Builder(context)
            .setTrackSelector(trackSelector)
            .setLoadControl(loadControl)
            .setSeekBackIncrementMs(10000) // 10 seconds
            .setSeekForwardIncrementMs(10000) // 10 seconds
            .build().apply {
                // Enable automatic quality adjustment
                trackSelector.parameters = trackSelector.parameters
                    .buildUpon()
                    .setMaxVideoSizeSd()
                    .setAllowVideoMixedMimeTypeAdaptiveness(true)
                    .setAllowAudioMixedMimeTypeAdaptiveness(true)
                    .build()
            }
    }
    
    /**
     * Creates a data source factory with caching support
     */
    fun createDataSourceFactory(): DataSource.Factory {
        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setUserAgent(PLAYER_USER_AGENT)
            .setConnectTimeoutMs(CONNECT_TIMEOUT_MS)
            .setReadTimeoutMs(READ_TIMEOUT_MS)
            .setAllowCrossProtocolRedirects(true)
        
        val cache = getCache()
        
        return CacheDataSource.Factory()
            .setCache(cache)
            .setUpstreamDataSourceFactory(
                DefaultDataSource.Factory(context, httpDataSourceFactory)
            )
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
    }
    
    /**
     * Creates an optimized track selector
     */
    private fun createTrackSelector(): DefaultTrackSelector {
        return DefaultTrackSelector(context).apply {
            setParameters(
                buildUponParameters()
                    .setMaxVideoSizeSd()
                    .setPreferredAudioLanguage("en")
                    .setPreferredTextLanguage("en")
                    .setSelectUndeterminedTextLanguage(true)
                    .setDisabledTextTrackSelectionFlags(C.SELECTION_FLAG_DEFAULT)
                    .build()
            )
        }
    }
    
    /**
     * Creates an optimized load control for smooth playback
     */
    private fun createLoadControl(): LoadControl {
        return DefaultLoadControl.Builder()
            .setAllocator(androidx.media3.exoplayer.DefaultAllocator(true, 16))
            .setBufferDurationsMs(
                MIN_BUFFER_MS,
                MAX_BUFFER_MS,
                MIN_PLAYBACK_START_BUFFER,
                MIN_PLAYBACK_RESUME_BUFFER
            )
            .setTargetBufferBytes(C.LENGTH_UNSET)
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()
    }
    
    /**
     * Gets or creates the cache instance
     */
    private fun getCache(): SimpleCache {
        if (simpleCache == null) {
            val cacheDir = File(context.cacheDir, CACHE_DIR_NAME)
            if (!cacheDir.exists()) {
                cacheDir.mkdirs()
            }
            
            val cacheEvictor = LeastRecentlyUsedCacheEvictor(CACHE_SIZE)
            val databaseProvider = StandaloneDatabaseProvider(context)
            
            simpleCache = SimpleCache(cacheDir, cacheEvictor, databaseProvider)
        }
        return simpleCache!!
    }
    
    /**
     * Clears the cache
     */
    fun clearCache() {
        simpleCache?.release()
        simpleCache = null
        
        val cacheDir = File(context.cacheDir, CACHE_DIR_NAME)
        if (cacheDir.exists()) {
            cacheDir.deleteRecursively()
        }
    }
    
    /**
     * Gets current cache size in bytes
     */
    fun getCacheSize(): Long {
        val cacheDir = File(context.cacheDir, CACHE_DIR_NAME)
        return if (cacheDir.exists()) {
            cacheDir.walkTopDown().sumOf { it.length() }
        } else {
            0L
        }
    }
    
    /**
     * Releases resources
     */
    fun release() {
        simpleCache?.release()
        simpleCache = null
    }
}