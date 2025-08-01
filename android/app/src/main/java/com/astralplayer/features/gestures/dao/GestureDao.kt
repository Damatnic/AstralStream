package com.astralplayer.features.gestures.dao

import androidx.room.*
import com.astralplayer.features.gestures.data.*
import kotlinx.coroutines.flow.Flow

@Dao
interface GestureDao {
    
    // Gesture CRUD operations
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGesture(gesture: GestureEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGestures(gestures: List<GestureEntity>)
    
    @Update
    suspend fun updateGesture(gesture: GestureEntity)
    
    @Delete
    suspend fun deleteGesture(gesture: GestureEntity)
    
    @Query("DELETE FROM custom_gestures WHERE id = :gestureId")
    suspend fun deleteGestureById(gestureId: String)
    
    @Query("SELECT * FROM custom_gestures WHERE id = :gestureId")
    suspend fun getGestureById(gestureId: String): GestureEntity?
    
    @Query("SELECT * FROM custom_gestures WHERE gestureType = :type AND zone = :zone LIMIT 1")
    suspend fun getGestureByTypeAndZone(type: GestureType, zone: GestureZone): GestureEntity?
    
    @Query("SELECT * FROM custom_gestures WHERE isEnabled = 1 ORDER BY priority DESC, gestureType")
    fun getEnabledGestures(): Flow<List<GestureEntity>>
    
    @Query("SELECT * FROM custom_gestures ORDER BY zone, gestureType")
    fun getAllGestures(): Flow<List<GestureEntity>>
    
    @Query("SELECT * FROM custom_gestures WHERE zone = :zone AND isEnabled = 1 ORDER BY priority DESC")
    suspend fun getGesturesForZone(zone: GestureZone): List<GestureEntity>
    
    @Query("SELECT * FROM custom_gestures WHERE action = :action AND isEnabled = 1")
    suspend fun getGesturesByAction(action: GestureAction): List<GestureEntity>
    
    @Query("UPDATE custom_gestures SET isEnabled = :enabled WHERE id = :gestureId")
    suspend fun setGestureEnabled(gestureId: String, enabled: Boolean)
    
    @Query("UPDATE custom_gestures SET sensitivity = :sensitivity WHERE id = :gestureId")
    suspend fun updateGestureSensitivity(gestureId: String, sensitivity: Float)
    
    @Query("DELETE FROM custom_gestures")
    suspend fun deleteAllGestures()
    
    // Profile operations
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: GestureProfileEntity)
    
    @Update
    suspend fun updateProfile(profile: GestureProfileEntity)
    
    @Delete
    suspend fun deleteProfile(profile: GestureProfileEntity)
    
    @Query("SELECT * FROM gesture_profiles ORDER BY isActive DESC, name")
    fun getAllProfiles(): Flow<List<GestureProfileEntity>>
    
    @Query("SELECT * FROM gesture_profiles WHERE id = :profileId")
    suspend fun getProfileById(profileId: String): GestureProfileEntity?
    
    @Query("SELECT * FROM gesture_profiles WHERE isActive = 1 LIMIT 1")
    suspend fun getActiveProfile(): GestureProfileEntity?
    
    @Query("UPDATE gesture_profiles SET isActive = 0")
    suspend fun deactivateAllProfiles()
    
    @Query("UPDATE gesture_profiles SET isActive = 1 WHERE id = :profileId")
    suspend fun activateProfile(profileId: String)
    
    @Transaction
    suspend fun setActiveProfile(profileId: String) {
        deactivateAllProfiles()
        activateProfile(profileId)
    }
    
    // Profile mapping operations
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfileMapping(mapping: GestureProfileMappingEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfileMappings(mappings: List<GestureProfileMappingEntity>)
    
    @Delete
    suspend fun deleteProfileMapping(mapping: GestureProfileMappingEntity)
    
    @Query("DELETE FROM gesture_profile_mappings WHERE profileId = :profileId")
    suspend fun deleteAllMappingsForProfile(profileId: String)
    
    @Query("""
        SELECT g.* FROM custom_gestures g
        INNER JOIN gesture_profile_mappings m ON g.id = m.gestureId
        WHERE m.profileId = :profileId
        ORDER BY g.priority DESC, g.gestureType
    """)
    suspend fun getGesturesForProfile(profileId: String): List<GestureEntity>
    
    @Query("""
        SELECT g.* FROM custom_gestures g
        INNER JOIN gesture_profile_mappings m ON g.id = m.gestureId
        WHERE m.profileId = :profileId AND g.isEnabled = 1
        ORDER BY g.priority DESC, g.gestureType
    """)
    fun getEnabledGesturesForProfile(profileId: String): Flow<List<GestureEntity>>
    
    // History operations
    @Insert
    suspend fun insertGestureHistory(history: GestureHistoryEntity)
    
    @Query("SELECT * FROM gesture_history ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentGestureHistory(limit: Int = 100): List<GestureHistoryEntity>
    
    @Query("""
        SELECT gestureType, COUNT(*) as count FROM gesture_history 
        WHERE timestamp > :since AND wasSuccessful = 1
        GROUP BY gestureType 
        ORDER BY count DESC
    """)
    suspend fun getMostUsedGestures(since: Long): List<GestureUsageStats>
    
    @Query("DELETE FROM gesture_history WHERE timestamp < :before")
    suspend fun deleteOldHistory(before: Long)
    
    // Shortcut operations
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShortcut(shortcut: GestureShortcutEntity)
    
    @Update
    suspend fun updateShortcut(shortcut: GestureShortcutEntity)
    
    @Delete
    suspend fun deleteShortcut(shortcut: GestureShortcutEntity)
    
    @Query("SELECT * FROM gesture_shortcuts WHERE isEnabled = 1 ORDER BY usageCount DESC")
    fun getEnabledShortcuts(): Flow<List<GestureShortcutEntity>>
    
    @Query("SELECT * FROM gesture_shortcuts ORDER BY name")
    fun getAllShortcuts(): Flow<List<GestureShortcutEntity>>
    
    @Query("UPDATE gesture_shortcuts SET usageCount = usageCount + 1, lastUsedAt = :timestamp WHERE id = :shortcutId")
    suspend fun recordShortcutUsage(shortcutId: String, timestamp: Long = System.currentTimeMillis())
    
    // Complex queries
    @Query("""
        SELECT * FROM custom_gestures 
        WHERE zone IN (:zones) AND isEnabled = 1 
        ORDER BY priority DESC, gestureType
    """)
    suspend fun getGesturesForMultipleZones(zones: List<GestureZone>): List<GestureEntity>
    
    @Query("""
        SELECT DISTINCT action FROM custom_gestures 
        WHERE isEnabled = 1 
        ORDER BY action
    """)
    suspend fun getAvailableActions(): List<GestureAction>
    
    @Query("""
        SELECT zone, COUNT(*) as gestureCount FROM custom_gestures 
        WHERE isEnabled = 1 
        GROUP BY zone 
        ORDER BY gestureCount DESC
    """)
    suspend fun getGestureCountByZone(): List<ZoneGestureCount>
    
    @Transaction
    suspend fun duplicateProfile(sourceProfileId: String, newName: String): String {
        val sourceProfile = getProfileById(sourceProfileId) ?: return ""
        val newProfile = sourceProfile.copy(
            id = java.util.UUID.randomUUID().toString(),
            name = newName,
            isActive = false,
            isBuiltIn = false,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        insertProfile(newProfile)
        
        val sourceMappings = getGesturesForProfile(sourceProfileId)
        val newMappings = sourceMappings.map { gesture ->
            GestureProfileMappingEntity(
                profileId = newProfile.id,
                gestureId = gesture.id
            )
        }
        insertProfileMappings(newMappings)
        
        return newProfile.id
    }
    
    @Transaction
    suspend fun resetToDefaults() {
        deleteAllGestures()
        // Insert default gestures would be called here
    }
}

data class GestureUsageStats(
    val gestureType: GestureType,
    val count: Int
)

data class ZoneGestureCount(
    val zone: GestureZone,
    val gestureCount: Int
)