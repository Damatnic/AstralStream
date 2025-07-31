package com.astralplayer.astralstream

import android.app.Application
import androidx.room.Room
import com.astralplayer.astralstream.data.database.AstralStreamDatabase
import com.astralplayer.astralstream.data.repository.SettingsRepository
import com.astralplayer.astralstream.data.repository.SettingsRepositoryImpl
import com.google.firebase.FirebaseApp
import com.google.firebase.crashlytics.FirebaseCrashlytics
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber
import javax.inject.Inject

@HiltAndroidApp
class AstralStreamApplication : Application() {
    
    // These will be injected by Hilt instead of manual initialization
    @Inject
    lateinit var database: AstralStreamDatabase
    
    @Inject
    lateinit var settingsRepository: SettingsRepository
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize Timber for logging
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        } else {
            // Plant a tree for release that sends logs to Crashlytics
            Timber.plant(CrashlyticsTree())
        }
        
        // Initialize Firebase
        FirebaseApp.initializeApp(this)
        
        // Enable Crashlytics collection
        FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(!BuildConfig.DEBUG)
        
        Timber.d("AstralStreamApplication initialized")
    }
    
    // Custom Timber tree for Crashlytics in release builds
    private class CrashlyticsTree : Timber.Tree() {
        override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
            if (priority == android.util.Log.VERBOSE || priority == android.util.Log.DEBUG) {
                return
            }
            
            FirebaseCrashlytics.getInstance().log(message)
            
            if (t != null) {
                FirebaseCrashlytics.getInstance().recordException(t)
            }
        }
    }
}