package com.astralplayer.astralstream.data.dao

import androidx.room.*
import com.astralplayer.astralstream.data.entity.SettingsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SettingsDao {
    
    @Query("SELECT * FROM settings WHERE key = :key")
    suspend fun getSetting(key: String): SettingsEntity?
    
    @Query("SELECT * FROM settings WHERE key = :key")
    fun getSettingFlow(key: String): Flow<SettingsEntity?>
    
    @Query("SELECT * FROM settings")
    suspend fun getAllSettings(): List<SettingsEntity>
    
    @Query("SELECT * FROM settings")
    fun getAllSettingsFlow(): Flow<List<SettingsEntity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSetting(setting: SettingsEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSettings(settings: List<SettingsEntity>)
    
    @Update
    suspend fun updateSetting(setting: SettingsEntity)
    
    @Delete
    suspend fun deleteSetting(setting: SettingsEntity)
    
    @Query("DELETE FROM settings WHERE key = :key")
    suspend fun deleteSettingByKey(key: String)
    
    @Query("DELETE FROM settings")
    suspend fun deleteAllSettings()
}