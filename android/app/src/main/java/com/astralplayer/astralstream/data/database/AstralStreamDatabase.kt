package com.astralplayer.astralstream.data.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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
        
        const val DATABASE_NAME = "astralstream_database"
        
        fun getDatabase(context: Context): AstralStreamDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AstralStreamDatabase::class.java,
                    DATABASE_NAME
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
        
        fun getAllMigrations(): Array<Migration> {
            return arrayOf(
                // Add future migrations here
            )
        }
    }
}