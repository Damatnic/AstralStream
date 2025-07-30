package com.astralplayer.nextplayer.player

import android.content.Context
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

@UnstableApi
object PlayerConfig {
    
    fun createExoPlayer(context: Context): ExoPlayer {
        // Configure OkHttpClient for better streaming support
        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .retryOnConnectionFailure(true)
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .addHeader("Accept", "*/*")
                    .addHeader("Accept-Language", "en-US,en;q=0.9")
                    .addHeader("Accept-Encoding", "gzip, deflate")
                    .addHeader("Connection", "keep-alive")
                    .addHeader("Referer", chain.request().url.toString())
                    .build()
                chain.proceed(request)
            }
            .build()
        
        // Create data source factory with OkHttp
        val dataSourceFactory = OkHttpDataSource.Factory(okHttpClient)
            .setUserAgent("AstralPlayer/2.0")
        
        // Configure track selector for quality selection
        val trackSelector = DefaultTrackSelector(context).apply {
            setParameters(
                buildUponParameters()
                    .setMaxVideoSizeSd()
                    .setAllowVideoMixedMimeTypeAdaptiveness(true)
                    .setAllowAudioMixedMimeTypeAdaptiveness(true)
                    .setPreferredVideoMimeTypes(
                        "video/avc",    // H.264
                        "video/hevc",   // H.265
                        "video/av01",   // AV1
                        "video/vp9",    // VP9
                        "video/vp8"     // VP8
                    )
                    .build()
            )
        }
        
        // Create media source factory
        val mediaSourceFactory = DefaultMediaSourceFactory(context)
            .setDataSourceFactory(dataSourceFactory)
        
        // Build ExoPlayer with enhanced configuration
        return ExoPlayer.Builder(context)
            .setMediaSourceFactory(mediaSourceFactory)
            .setTrackSelector(trackSelector)
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .setPauseAtEndOfMediaItems(false)
            .setSeekBackIncrementMs(10000)
            .setSeekForwardIncrementMs(10000)
            .build()
    }
    
    fun createMediaItem(uri: String): MediaItem {
        return MediaItem.Builder()
            .setUri(uri)
            .setMimeType(getMimeType(uri))
            .build()
    }
    
    private fun getMimeType(uri: String): String? {
        return when {
            uri.endsWith(".m3u8", true) -> "application/x-mpegURL"
            uri.endsWith(".mpd", true) -> "application/dash+xml"
            uri.endsWith(".mp4", true) -> "video/mp4"
            uri.endsWith(".mkv", true) -> "video/x-matroska"
            uri.endsWith(".webm", true) -> "video/webm"
            uri.endsWith(".avi", true) -> "video/x-msvideo"
            uri.endsWith(".mov", true) -> "video/quicktime"
            uri.endsWith(".flv", true) -> "video/x-flv"
            uri.endsWith(".wmv", true) -> "video/x-ms-wmv"
            uri.endsWith(".ts", true) -> "video/mp2t"
            else -> null
        }
    }
}