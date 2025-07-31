package com.astralplayer.nextplayer

import android.app.Application
import android.os.StrictMode
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.room.Room
import com.astralplayer.nextplayer.data.database.AstralVuDatabase
import com.astralplayer.nextplayer.data.repository.SettingsRepository
import com.astralplayer.nextplayer.data.repository.SettingsRepositoryImpl
import com.astralplayer.nextplayer.utils.CodecManager
import com.astralplayer.nextplayer.config.ApiKeyManager
import com.astralplayer.nextplayer.performance.PerformanceMonitor
import com.astralplayer.nextplayer.performance.StartupOptimizer
import com.astralplayer.nextplayer.security.AppLockManager
import com.google.firebase.FirebaseApp
import androidx.work.Configuration
import androidx.hilt.work.HiltWorkerFactory
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@HiltAndroidApp
class AstralStreamApplication : Application(), Configuration.Provider {
    
    @Inject lateinit var workerFactory: HiltWorkerFactory
    
    @Inject lateinit var startupOptimizer: StartupOptimizer
    @Inject lateinit var performanceMonitor: PerformanceMonitor
    @Inject lateinit var appLockManager: AppLockManager
    
    lateinit var database: AstralVuDatabase
        private set
    
    lateinit var settingsRepository: SettingsRepository
        private set
        
    lateinit var codecManager: CodecManager
        private set
    
    override fun onCreate() {
        super.onCreate()
        
        // Register lifecycle observer for app lock
        ProcessLifecycleOwner.get().lifecycle.addObserver(AppLifecycleObserver())
        
        // Initialize API Key Manager
        ApiKeyManager.initialize(this)
        
        // Initialize Firebase
        FirebaseApp.initializeApp(this)
        
        // Optimize startup
        runBlocking {
            startupOptimizer.optimizeApplicationStartup()
            startupOptimizer.optimize()
        }
        
        // Initialize database
        database = Room.databaseBuilder(
            applicationContext,
            AstralVuDatabase::class.java,
            AstralVuDatabase.DATABASE_NAME
        )
        .addMigrations(*AstralVuDatabase.getAllMigrations())
        .fallbackToDestructiveMigration() // For development - remove in production
        .build()
        
        // Initialize settings repository
        settingsRepository = SettingsRepositoryImpl(this)
        
        // Initialize codec manager
        codecManager = CodecManager(this).apply {
            initializeCodecs()
        }
        
        // Initialize crash reporting
        initializeCrashReporting()
        
        // Setup strict mode in debug
        setupStrictMode()
        
        // Log system info for debugging
        performanceMonitor.logSystemInfo()
    }
    
    private fun initializeCrashReporting() {
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e("AstralStream", "Uncaught exception in thread ${thread.name}", throwable)
            // In production, would send to crash reporting service
        }
    }
    
    private fun setupStrictMode() {
        if (BuildConfig.DEBUG) {
            StrictMode.setThreadPolicy(
                StrictMode.ThreadPolicy.Builder()
                    .detectAll()
                    .penaltyLog()
                    .build()
            )
            StrictMode.setVmPolicy(
                StrictMode.VmPolicy.Builder()
                    .detectAll()
                    .penaltyLog()
                    .build()
            )
        }
    }
    
    inner class AppLifecycleObserver : DefaultLifecycleObserver {
        override fun onStart(owner: LifecycleOwner) {
            super.onStart(owner)
            appLockManager.onAppForegrounded()
        }
        
        override fun onStop(owner: LifecycleOwner) {
            super.onStop(owner)
            appLockManager.onAppBackgrounded()
        }
    }
    
    override fun getWorkManagerConfiguration(): Configuration {
        return Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
    }
}