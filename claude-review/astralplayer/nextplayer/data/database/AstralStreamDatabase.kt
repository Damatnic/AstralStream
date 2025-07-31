package com.astralplayer.nextplayer.data.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import android.content.Context
import com.astralplayer.nextplayer.data.entities.VideoMetadata
import com.astralplayer.nextplayer.data.entities.PlaybackHistory
import com.astralplayer.nextplayer.data.entities.SubtitleEntry
import com.astralplayer.nextplayer.data.dao.VideoDao
import com.astralplayer.nextplayer.data.dao.HistoryDao

@Database(
    entities = [
        VideoMetadata::class,
        PlaybackHistory::class,
        SubtitleEntry::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AstralStreamDatabase : RoomDatabase() {
    abstract fun videoDao(): VideoDao
    abstract fun historyDao(): HistoryDao
    
    companion object {
        @Volatile
        private var INSTANCE: AstralStreamDatabase? = null
        
        fun getDatabase(context: Context): AstralStreamDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AstralStreamDatabase::class.java,
                    "astralstream_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}