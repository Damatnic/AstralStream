package com.astralplayer.nextplayer.di

import android.content.Context
import com.astralplayer.nextplayer.data.*
import com.astralplayer.nextplayer.data.database.*
import com.astralplayer.nextplayer.feature.codec.CodecPackManager

// Simplified AppModule without Hilt for now - will re-enable after basic functionality works
object AppModule {
    
    fun provideSettingsDataStore(context: Context): SettingsDataStore {
        return SettingsDataStore(context)
    }
    
    fun provideCodecPackManager(context: Context): CodecPackManager {
        return CodecPackManager(context)
    }
}