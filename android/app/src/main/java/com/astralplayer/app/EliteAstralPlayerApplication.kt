package com.astralplayer.app

import android.app.Application
import androidx.multidex.MultiDexApplication
import com.astralplayer.core.codec.CodecManager
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber
import javax.inject.Inject

@HiltAndroidApp
class EliteAstralPlayerApplication : MultiDexApplication() {
    
    @Inject lateinit var codecManager: CodecManager
    
    override fun onCreate() {
        super.onCreate()
        
        initializeLogging()
        initializeCodecManager()
        
        Timber.i("EliteAstralPlayerApplication initialized")
    }
    
    private fun initializeLogging() {
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        } else {
            // In production, you might want to use a different tree
            // that logs to Crashlytics or another crash reporting service
            Timber.plant(object : Timber.Tree() {
                override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
                    // Custom logging logic for production
                }
            })
        }
    }
    
    private fun initializeCodecManager() {
        try {
            codecManager.initialize()
            Timber.d("CodecManager initialized successfully")
        } catch (e: Exception) {
            Timber.e(e, "Failed to initialize CodecManager")
        }
    }
    
    override fun onTerminate() {
        super.onTerminate()
        Timber.i("EliteAstralPlayerApplication terminated")
    }
    
    override fun onLowMemory() {
        super.onLowMemory()
        Timber.w("Low memory warning received")
        // Perform memory cleanup
        codecManager.clearCache()
    }
    
    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        Timber.d("Memory trim requested: level $level")
        
        when (level) {
            TRIM_MEMORY_RUNNING_MODERATE,
            TRIM_MEMORY_RUNNING_LOW,
            TRIM_MEMORY_RUNNING_CRITICAL -> {
                // App is running but system is low on memory
                codecManager.reduceMemoryUsage()
            }
            TRIM_MEMORY_UI_HIDDEN -> {
                // App UI is hidden, safe to release UI-related resources
                codecManager.releaseUIResources()
            }
            TRIM_MEMORY_BACKGROUND,
            TRIM_MEMORY_MODERATE,
            TRIM_MEMORY_COMPLETE -> {
                // App is in background, release non-essential resources
                codecManager.clearCache()
                codecManager.releaseBackgroundResources()
            }
        }
    }
}