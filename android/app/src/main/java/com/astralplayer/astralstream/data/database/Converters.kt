package com.astralplayer.astralstream.data.database

import androidx.room.TypeConverter
import com.astralplayer.astralstream.data.entity.ChapterInfo
import com.astralplayer.astralstream.data.entity.PlaylistType
import com.astralplayer.astralstream.data.entity.SubtitleFormat
import com.astralplayer.astralstream.data.model.VideoFormat
import com.astralplayer.astralstream.data.model.VideoSource
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class Converters {
    private val gson = Gson()
    
    @TypeConverter
    fun fromStringList(value: String?): List<String> {
        if (value == null) return emptyList()
        val listType = object : TypeToken<List<String>>() {}.type
        return gson.fromJson(value, listType)
    }
    
    @TypeConverter
    fun fromStringList(list: List<String>?): String {
        return gson.toJson(list ?: emptyList())
    }
    
    @TypeConverter
    fun fromChapterList(value: String?): List<ChapterInfo> {
        if (value == null) return emptyList()
        val listType = object : TypeToken<List<ChapterInfo>>() {}.type
        return gson.fromJson(value, listType)
    }
    
    @TypeConverter
    fun fromChapterList(list: List<ChapterInfo>?): String {
        return gson.toJson(list ?: emptyList())
    }
    
    @TypeConverter
    fun fromVideoFormat(format: VideoFormat): String {
        return format.name
    }
    
    @TypeConverter
    fun toVideoFormat(format: String): VideoFormat {
        return VideoFormat.valueOf(format)
    }
    
    @TypeConverter
    fun fromVideoSource(source: VideoSource): String {
        return source.name
    }
    
    @TypeConverter
    fun toVideoSource(source: String): VideoSource {
        return VideoSource.valueOf(source)
    }
    
    @TypeConverter
    fun fromPlaylistType(type: PlaylistType): String {
        return type.name
    }
    
    @TypeConverter
    fun toPlaylistType(type: String): PlaylistType {
        return PlaylistType.valueOf(type)
    }
    
    @TypeConverter
    fun fromSubtitleFormat(format: SubtitleFormat): String {
        return format.name
    }
    
    @TypeConverter
    fun toSubtitleFormat(format: String): SubtitleFormat {
        return SubtitleFormat.valueOf(format)
    }
}