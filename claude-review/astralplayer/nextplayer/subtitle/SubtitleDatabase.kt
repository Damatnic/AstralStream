package com.astralplayer.nextplayer.subtitle

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * Room database for storing subtitle data
 */
@Database(
    entities = [SubtitleEntity::class],
    version = 1,
    exportSchema = true
)
abstract class SubtitleDatabase : RoomDatabase() {
    abstract fun subtitleDao(): SubtitleDao
}