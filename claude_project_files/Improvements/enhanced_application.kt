package com.astralplayer.nextplayer

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.room.Room
import com.astralplayer.nextplayer.data.database.AstralVuDatabase
import com.astralplayer.nextplayer.data.repository.SettingsRepository
import com.astralplayer.nextplayer.data.repository.SettingsRepositoryImpl
import com.astralplayer.nextplayer.utils.IntentUtils
import com.astralplayer.nextplayer.utils.CodecManager
import com.google.firebase.FirebaseApp
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class AstralVuApplication : Application() {
    
    companion object {
        private lateinit var instance: AstralVuApplication
        
        fun getInstance(): AstralVuApplication = instance
        
        fun getContext(): Context = instance.applicationContext
    }
    
    lateinit var database: AstralVuDatabase
        private set
    
    lateinit var settingsRepository: SettingsRepository
        private set
        
    lateinit var codecManager: CodecManager
        private set
    
    override fun onCreate() {
        super.onCreate()
        instance = this
        
        // Initialize Firebase
        FirebaseApp.initializeApp(this)
        
        // Initialize codec manager first
        codecManager = CodecManager(this)
        
        // Initialize database with migrations
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
        
        // Register intent handlers for "Open with" functionality
        registerIntentHandlers()
        
        // Setup codec support
        setupCodecSupport()
    }
    
    private fun registerIntentHandlers() {
        // This will be handled in MainActivity and VideoPlayerActivity
        // through intent filters in AndroidManifest.xml
    }
    
    private fun setupCodecSupport() {
        // Initialize advanced codec support
        codecManager.initializeCodecs()
    }
    
    /**
     * Handle incoming video intent from browser or other apps
     */
    fun handleVideoIntent(intent: Intent): Boolean {
        return IntentUtils.handleVideoIntent(this, intent)
    }
    
    /**
     * Get video intent for direct playback
     */
    fun createVideoPlaybackIntent(uri: Uri, title: String? = null): Intent {
        return Intent(this, VideoPlayerActivity::class.java).apply {
            data = uri
            title?.let { putExtra("video_title", it) }
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }
}