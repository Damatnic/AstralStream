package com.astralplayer.nextplayer.feature.subtitles

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import android.content.Context

/**
 * Room database for subtitle storage
 */
@Database(
    entities = [SubtitleEntity::class],
    version = 1,
    exportSchema = false
)
abstract class SubtitleDatabase : RoomDatabase() {
    
    abstract fun subtitleDao(): SubtitleDao
    
    companion object {
        @Volatile
        private var INSTANCE: SubtitleDatabase? = null
        
        fun getDatabase(context: Context): SubtitleDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SubtitleDatabase::class.java,
                    "subtitle_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}