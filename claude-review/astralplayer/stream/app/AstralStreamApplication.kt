package com.astralplayer.stream.app

import android.app.Application
import androidx.multidex.MultiDexApplication
import com.astralplayer.core.codec.CodecManager
import com.google.firebase.FirebaseApp
import com.google.firebase.crashlytics.FirebaseCrashlytics
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber
import javax.inject.Inject

@HiltAndroidApp
class AstralStreamApplication : MultiDexApplication() {
    
    @Inject lateinit var codecManager: CodecManager
    
    override fun onCreate() {
        super.onCreate()
        
        initializeLogging()
        initializeFirebase()
        initializeCodecManager()
        
        Timber.i("AstralStream Professional Video Player initialized")
    }
    
    private fun initializeLogging() {
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        } else {
            Timber.plant(ProductionTree())
        }
    }
    
    private fun initializeFirebase() {
        try {
            FirebaseApp.initializeApp(this)
            FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(!BuildConfig.DEBUG)
        } catch (e: Exception) {
            Timber.e(e, "Failed to initialize Firebase")
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
        Timber.i("AstralStream terminated")
    }
    
    override fun onLowMemory() {
        super.onLowMemory()
        Timber.w("Low memory warning - cleaning up resources")
        codecManager.clearCache()
    }
    
    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        Timber.d("Memory trim requested: level $level")
        
        when (level) {
            TRIM_MEMORY_RUNNING_MODERATE,
            TRIM_MEMORY_RUNNING_LOW,
            TRIM_MEMORY_RUNNING_CRITICAL -> {
                codecManager.reduceMemoryUsage()
            }
            TRIM_MEMORY_UI_HIDDEN -> {
                codecManager.releaseUIResources()
            }
            TRIM_MEMORY_BACKGROUND,
            TRIM_MEMORY_MODERATE,
            TRIM_MEMORY_COMPLETE -> {
                codecManager.clearCache()
                codecManager.releaseBackgroundResources()
            }
        }
    }
    
    private class ProductionTree : Timber.Tree() {
        override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
            if (priority == android.util.Log.VERBOSE || priority == android.util.Log.DEBUG) {
                return
            }
            
            try {
                FirebaseCrashlytics.getInstance().log(message)
                if (t != null) {
                    FirebaseCrashlytics.getInstance().recordException(t)
                }
            } catch (e: Exception) {
                // Silently handle Firebase errors
            }
        }
    }
}