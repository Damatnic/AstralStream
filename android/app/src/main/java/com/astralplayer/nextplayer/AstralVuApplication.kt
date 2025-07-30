package com.astralplayer.nextplayer

import android.app.Application
import androidx.room.Room
import com.astralplayer.nextplayer.data.database.AstralVuDatabase
import com.astralplayer.nextplayer.data.repository.SettingsRepository
import com.astralplayer.nextplayer.data.repository.SettingsRepositoryImpl
import com.astralplayer.nextplayer.utils.CodecManager
import com.astralplayer.nextplayer.config.ApiKeyManager
import com.google.firebase.FirebaseApp
class AstralVuApplication : Application() {
    
    lateinit var database: AstralVuDatabase
        private set
    
    lateinit var settingsRepository: SettingsRepository
        private set
        
    lateinit var codecManager: CodecManager
        private set
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize API Key Manager
        ApiKeyManager.initialize(this)
        
        // Initialize Firebase
        FirebaseApp.initializeApp(this)
        
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
    }
}