package com.astralplayer.nextplayer.di

import android.content.Context
import androidx.room.Room
import com.astralplayer.nextplayer.data.database.*

// Simplified DatabaseModule without Hilt for now - will re-enable after basic functionality works
object DatabaseModule {
    
    @Volatile
    private var INSTANCE: AstralVuDatabase? = null
    
    fun provideDatabase(context: Context): AstralVuDatabase {
        return INSTANCE ?: synchronized(this) {
            val instance = Room.databaseBuilder(
                context.applicationContext,
                AstralVuDatabase::class.java,
                AstralVuDatabase.DATABASE_NAME
            )
                .addMigrations(*AstralVuDatabase.getAllMigrations())
                .fallbackToDestructiveMigration() // For development - remove in production
                .build()
            INSTANCE = instance
            instance
        }
    }
    
    fun provideRecentFilesDao(context: Context): RecentFilesDao {
        return provideDatabase(context).recentFilesDao()
    }
    
    fun providePlaylistDao(context: Context): PlaylistDao {
        return provideDatabase(context).playlistDao()
    }
    
    fun provideSubtitleDao(context: Context): SubtitleDao {
        return provideDatabase(context).subtitleDao()
    }
    
    fun provideCloudFileDao(context: Context): CloudFileDao {
        return provideDatabase(context).cloudFileDao()
    }
    
    fun provideDownloadQueueDao(context: Context): DownloadQueueDao {
        return provideDatabase(context).downloadQueueDao()
    }
    
    fun provideUserPreferenceDao(context: Context): UserPreferenceDao {
        return provideDatabase(context).userPreferenceDao()
    }
    
    fun providePlaybackHistoryDao(context: Context): PlaybackHistoryDao {
        return provideDatabase(context).playbackHistoryDao()
    }
}