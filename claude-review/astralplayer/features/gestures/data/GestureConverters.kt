package com.astralplayer.features.gestures.data

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class GestureConverters {
    
    private val gson = Gson()
    
    @TypeConverter
    fun fromGestureType(type: GestureType): String {
        return type.name
    }
    
    @TypeConverter
    fun toGestureType(type: String): GestureType {
        return GestureType.valueOf(type)
    }
    
    @TypeConverter
    fun fromGestureZone(zone: GestureZone): String {
        return zone.name
    }
    
    @TypeConverter
    fun toGestureZone(zone: String): GestureZone {
        return GestureZone.valueOf(zone)
    }
    
    @TypeConverter
    fun fromGestureAction(action: GestureAction): String {
        return action.name
    }
    
    @TypeConverter
    fun toGestureAction(action: String): GestureAction {
        return GestureAction.valueOf(action)
    }
    
    @TypeConverter
    fun fromSwipeDirection(direction: SwipeDirection?): String? {
        return direction?.name
    }
    
    @TypeConverter
    fun toSwipeDirection(direction: String?): SwipeDirection? {
        return direction?.let { SwipeDirection.valueOf(it) }
    }
    
    @TypeConverter
    fun fromGestureTypeList(types: List<GestureType>): String {
        return gson.toJson(types.map { it.name })
    }
    
    @TypeConverter
    fun toGestureTypeList(json: String): List<GestureType> {
        val type = object : TypeToken<List<String>>() {}.type
        val names: List<String> = gson.fromJson(json, type) ?: emptyList()
        return names.map { GestureType.valueOf(it) }
    }
    
    @TypeConverter
    fun fromGestureZoneList(zones: List<GestureZone>): String {
        return gson.toJson(zones.map { it.name })
    }
    
    @TypeConverter
    fun toGestureZoneList(json: String): List<GestureZone> {
        val type = object : TypeToken<List<String>>() {}.type
        val names: List<String> = gson.fromJson(json, type) ?: emptyList()
        return names.map { GestureZone.valueOf(it) }
    }
}