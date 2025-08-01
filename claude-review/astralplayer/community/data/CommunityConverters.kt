package com.astralplayer.community.data

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class CommunityConverters {
    
    private val gson = Gson()
    
    @TypeConverter
    fun fromPlaylistCategory(category: PlaylistCategory): String {
        return category.name
    }
    
    @TypeConverter
    fun toPlaylistCategory(category: String): PlaylistCategory {
        return PlaylistCategory.valueOf(category)
    }
    
    @TypeConverter
    fun fromSubtitleVerificationStatus(status: SubtitleVerificationStatus): String {
        return status.name
    }
    
    @TypeConverter
    fun toSubtitleVerificationStatus(status: String): SubtitleVerificationStatus {
        return SubtitleVerificationStatus.valueOf(status)
    }
    
    @TypeConverter
    fun fromSubtitleContributionSource(source: SubtitleContributionSource): String {
        return source.name
    }
    
    @TypeConverter
    fun toSubtitleContributionSource(source: String): SubtitleContributionSource {
        return SubtitleContributionSource.valueOf(source)
    }
    
    @TypeConverter
    fun fromSubtitleReportReason(reason: SubtitleReportReason): String {
        return reason.name
    }
    
    @TypeConverter
    fun toSubtitleReportReason(reason: String): SubtitleReportReason {
        return SubtitleReportReason.valueOf(reason)
    }
    
    @TypeConverter
    fun fromReportStatus(status: ReportStatus): String {
        return status.name
    }
    
    @TypeConverter
    fun toReportStatus(status: String): ReportStatus {
        return ReportStatus.valueOf(status)
    }
    
    @TypeConverter
    fun fromReportSeverity(severity: ReportSeverity): String {
        return severity.name
    }
    
    @TypeConverter
    fun toReportSeverity(severity: String): ReportSeverity {
        return ReportSeverity.valueOf(severity)
    }
    
    @TypeConverter
    fun fromStringMap(map: Map<String, String>): String {
        return gson.toJson(map)
    }
    
    @TypeConverter
    fun toStringMap(json: String): Map<String, String> {
        val type = object : TypeToken<Map<String, String>>() {}.type
        return gson.fromJson(json, type) ?: emptyMap()
    }
    
    @TypeConverter
    fun fromStringList(list: List<String>): String {
        return gson.toJson(list)
    }
    
    @TypeConverter
    fun toStringList(json: String): List<String> {
        val type = object : TypeToken<List<String>>() {}.type
        return gson.fromJson(json, type) ?: emptyList()
    }
}