package com.astralplayer.nextplayer.feature.subtitles

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import java.util.Date

/**
 * Type converters for Room database
 */
class Converters {
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }

    @TypeConverter
    fun fromSubtitleFormat(format: SubtitleFormat): String {
        return format.name
    }

    @TypeConverter
    fun toSubtitleFormat(formatName: String): SubtitleFormat {
        return SubtitleFormat.valueOf(formatName)
    }
}

// SubtitleFormat enum moved to SubtitleFormat.kt

/**
 * Entity class for subtitle database storage
 */
@Entity(tableName = "subtitles")
@TypeConverters(Converters::class)
data class SubtitleEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val language: String,
    val format: SubtitleFormat,
    val content: String,
    val mediaId: String,
    val isExternal: Boolean = false,
    val isDefault: Boolean = false,
    val generatedBy: String? = null,
    val createdAt: Date = Date()
)