package com.astralplayer.astralstream.data.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import android.content.Context
import com.astralplayer.astralstream.data.entity.VideoEntity
import com.astralplayer.astralstream.data.entity.PlaylistEntity
import com.astralplayer.astralstream.data.entity.PlaybackStateEntity
import com.astralplayer.astralstream.data.entity.SettingsEntity
import com.astralplayer.astralstream.data.dao.VideoDao
import com.astralplayer.astralstream.data.dao.PlaylistDao
import com.astralplayer.astralstream.data.dao.PlaybackStateDao
import com.astralplayer.astralstream.data.dao.SettingsDao

@Database(
    entities = [
        VideoEntity::class,
        PlaylistEntity::class,
        PlaybackStateEntity::class,
        SettingsEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AstralStreamDatabase : RoomDatabase() {
    
    abstract fun videoDao(): VideoDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun playbackStateDao(): PlaybackStateDao
    abstract fun settingsDao(): SettingsDao
    
    companion object {
        @Volatile
        private var INSTANCE: AstralStreamDatabase? = null
        
        fun getDatabase(context: Context): AstralStreamDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AstralStreamDatabase::class.java,
                    "astralstream_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

class Converters {
    @androidx.room.TypeConverter
    fun fromTimestamp(value: Long?): java.util.Date? {
        return value?.let { java.util.Date(it) }
    }
    
    @androidx.room.TypeConverter
    fun dateToTimestamp(date: java.util.Date?): Long? {
        return date?.time
    }
    
    @androidx.room.TypeConverter
    fun fromString(value: String?): List<String>? {
        return value?.split(",")?.map { it.trim() }
    }
    
    @androidx.room.TypeConverter
    fun fromList(list: List<String>?): String? {
        return list?.joinToString(",")
    }
}