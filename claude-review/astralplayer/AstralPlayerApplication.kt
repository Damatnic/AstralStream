package com.astralplayer

import android.app.Application
import androidx.multidex.MultiDexApplication
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

@HiltAndroidApp
class AstralPlayerApplication : MultiDexApplication() {
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize Timber for logging
        Timber.plant(Timber.DebugTree())
        
        Timber.d("AstralPlayer Application initialized")
    }
}