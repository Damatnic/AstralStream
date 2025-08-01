package com.astralplayer.features.gestures.repository

import android.content.Context
import android.util.Log
import com.astralplayer.features.gestures.dao.GestureDao
import com.astralplayer.features.gestures.data.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GestureRepository @Inject constructor(
    private val gestureDao: GestureDao,
    @ApplicationContext private val context: Context
) {
    
    companion object {
        private const val TAG = "GestureRepository"
        private const val PREF_NAME = "gesture_preferences"
        private const val KEY_FIRST_RUN = "gesture_first_run"
        private const val KEY_TUTORIAL_SHOWN = "gesture_tutorial_shown"
    }
    
    private val preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    
    suspend fun initializeDefaultGestures() = withContext(Dispatchers.IO) {
        if (preferences.getBoolean(KEY_FIRST_RUN, true)) {
            Log.d(TAG, "First run detected, initializing default gestures")
            
            // Create default profile
            val defaultProfile = GestureProfileEntity(
                name = "Default",
                description = "Standard gesture configuration",
                isActive = true,
                isBuiltIn = true,
                iconName = "ic_gesture_default"
            )
            gestureDao.insertProfile(defaultProfile)
            
            // Create default gestures
            val defaultGestures = createDefaultGestures()
            gestureDao.insertGestures(defaultGestures)
            
            // Create profile mappings
            val mappings = defaultGestures.map { gesture ->
                GestureProfileMappingEntity(
                    profileId = defaultProfile.id,
                    gestureId = gesture.id
                )
            }
            gestureDao.insertProfileMappings(mappings)
            
            // Create additional built-in profiles
            createBuiltInProfiles()
            
            preferences.edit().putBoolean(KEY_FIRST_RUN, false).apply()
        }
    }
    
    fun getEnabledGestures(): Flow<List<GestureEntity>> {
        return gestureDao.getEnabledGestures()
    }
    
    fun getAllGestures(): Flow<List<GestureEntity>> {
        return gestureDao.getAllGestures()
    }
    
    suspend fun getGesturesForZone(zone: GestureZone): List<GestureEntity> {
        return gestureDao.getGesturesForZone(zone)
    }
    
    suspend fun getGestureByTypeAndZone(type: GestureType, zone: GestureZone): GestureEntity? {
        return gestureDao.getGestureByTypeAndZone(type, zone)
    }
    
    suspend fun updateGesture(gesture: GestureEntity) {
        gestureDao.updateGesture(gesture.copy(updatedAt = System.currentTimeMillis()))
    }
    
    suspend fun setGestureEnabled(gestureId: String, enabled: Boolean) {
        gestureDao.setGestureEnabled(gestureId, enabled)
    }
    
    suspend fun updateGestureSensitivity(gestureId: String, sensitivity: Float) {
        gestureDao.updateGestureSensitivity(gestureId, sensitivity.coerceIn(0.5f, 2.0f))
    }
    
    suspend fun createCustomGesture(
        type: GestureType,
        zone: GestureZone,
        action: GestureAction,
        sensitivity: Float = 1.0f
    ): Result<GestureEntity> {
        return try {
            // Check if gesture already exists
            val existing = gestureDao.getGestureByTypeAndZone(type, zone)
            if (existing != null) {
                return Result.failure(Exception("Gesture already exists for this zone"))
            }
            
            val gesture = GestureEntity(
                gestureType = type,
                zone = zone,
                action = action,
                sensitivity = sensitivity,
                priority = 0
            )
            
            gestureDao.insertGesture(gesture)
            Result.success(gesture)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create custom gesture", e)
            Result.failure(e)
        }
    }
    
    // Profile management
    fun getAllProfiles(): Flow<List<GestureProfileEntity>> {
        return gestureDao.getAllProfiles()
    }
    
    suspend fun getActiveProfile(): GestureProfileEntity? {
        return gestureDao.getActiveProfile()
    }
    
    suspend fun createProfile(name: String, description: String = ""): Result<GestureProfileEntity> {
        return try {
            val profile = GestureProfileEntity(
                name = name,
                description = description,
                isActive = false,
                isBuiltIn = false
            )
            gestureDao.insertProfile(profile)
            Result.success(profile)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create profile", e)
            Result.failure(e)
        }
    }
    
    suspend fun activateProfile(profileId: String) {
        gestureDao.setActiveProfile(profileId)
    }
    
    suspend fun duplicateProfile(sourceProfileId: String, newName: String): Result<String> {
        return try {
            val newProfileId = gestureDao.duplicateProfile(sourceProfileId, newName)
            Result.success(newProfileId)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to duplicate profile", e)
            Result.failure(e)
        }
    }
    
    suspend fun deleteProfile(profileId: String): Result<Unit> {
        return try {
            val profile = gestureDao.getProfileById(profileId)
            if (profile?.isBuiltIn == true) {
                return Result.failure(Exception("Cannot delete built-in profile"))
            }
            if (profile?.isActive == true) {
                // Switch to default profile before deleting
                val defaultProfile = gestureDao.getAllProfiles()
                    .map { it.firstOrNull { p -> p.isBuiltIn } }
                    .map { it ?: throw Exception("No default profile found") }
                gestureDao.setActiveProfile(defaultProfile.id)
            }
            gestureDao.deleteAllMappingsForProfile(profileId)
            gestureDao.deleteProfile(profile!!)
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete profile", e)
            Result.failure(e)
        }
    }
    
    // Gesture history and analytics
    suspend fun recordGestureUsage(
        type: GestureType,
        zone: GestureZone,
        action: GestureAction,
        startX: Float,
        startY: Float,
        endX: Float,
        endY: Float,
        wasSuccessful: Boolean,
        errorReason: String? = null
    ) {
        val history = GestureHistoryEntity(
            gestureType = type,
            zone = zone,
            action = action,
            startX = startX,
            startY = startY,
            endX = endX,
            endY = endY,
            wasSuccessful = wasSuccessful,
            errorReason = errorReason
        )
        gestureDao.insertGestureHistory(history)
    }
    
    suspend fun getMostUsedGestures(days: Int = 7): List<GestureUsageStats> {
        val since = System.currentTimeMillis() - (days * 24 * 60 * 60 * 1000L)
        return gestureDao.getMostUsedGestures(since)
    }
    
    suspend fun cleanupOldHistory(daysToKeep: Int = 30) {
        val before = System.currentTimeMillis() - (daysToKeep * 24 * 60 * 60 * 1000L)
        gestureDao.deleteOldHistory(before)
    }
    
    // Shortcuts
    fun getEnabledShortcuts(): Flow<List<GestureShortcutEntity>> {
        return gestureDao.getEnabledShortcuts()
    }
    
    suspend fun recordShortcutUsage(shortcutId: String) {
        gestureDao.recordShortcutUsage(shortcutId)
    }
    
    // Tutorial and help
    fun hasShownTutorial(): Boolean {
        return preferences.getBoolean(KEY_TUTORIAL_SHOWN, false)
    }
    
    fun setTutorialShown() {
        preferences.edit().putBoolean(KEY_TUTORIAL_SHOWN, true).apply()
    }
    
    private fun createDefaultGestures(): List<GestureEntity> {
        return listOf(
            // Playback controls
            GestureEntity(
                gestureType = GestureType.TAP,
                zone = GestureZone.CENTER,
                action = GestureAction.PLAY_PAUSE,
                priority = 10
            ),
            GestureEntity(
                gestureType = GestureType.DOUBLE_TAP,
                zone = GestureZone.LEFT_HALF,
                action = GestureAction.SEEK_BACKWARD,
                priority = 9
            ),
            GestureEntity(
                gestureType = GestureType.DOUBLE_TAP,
                zone = GestureZone.RIGHT_HALF,
                action = GestureAction.SEEK_FORWARD,
                priority = 9
            ),
            
            // Volume controls
            GestureEntity(
                gestureType = GestureType.SWIPE_UP,
                zone = GestureZone.RIGHT_HALF,
                action = GestureAction.VOLUME_UP,
                priority = 8
            ),
            GestureEntity(
                gestureType = GestureType.SWIPE_DOWN,
                zone = GestureZone.RIGHT_HALF,
                action = GestureAction.VOLUME_DOWN,
                priority = 8
            ),
            
            // Brightness controls
            GestureEntity(
                gestureType = GestureType.SWIPE_UP,
                zone = GestureZone.LEFT_HALF,
                action = GestureAction.BRIGHTNESS_UP,
                priority = 8
            ),
            GestureEntity(
                gestureType = GestureType.SWIPE_DOWN,
                zone = GestureZone.LEFT_HALF,
                action = GestureAction.BRIGHTNESS_DOWN,
                priority = 8
            ),
            
            // Seek controls
            GestureEntity(
                gestureType = GestureType.SWIPE_LEFT,
                zone = GestureZone.CENTER,
                action = GestureAction.FAST_FORWARD,
                priority = 7
            ),
            GestureEntity(
                gestureType = GestureType.SWIPE_RIGHT,
                zone = GestureZone.CENTER,
                action = GestureAction.REWIND,
                priority = 7
            ),
            
            // Zoom controls
            GestureEntity(
                gestureType = GestureType.PINCH_IN,
                zone = GestureZone.FULL_SCREEN,
                action = GestureAction.ZOOM_OUT,
                priority = 6,
                requiredFingers = 2
            ),
            GestureEntity(
                gestureType = GestureType.PINCH_OUT,
                zone = GestureZone.FULL_SCREEN,
                action = GestureAction.ZOOM_IN,
                priority = 6,
                requiredFingers = 2
            ),
            
            // UI controls
            GestureEntity(
                gestureType = GestureType.LONG_PRESS,
                zone = GestureZone.CENTER,
                action = GestureAction.SHOW_HIDE_CONTROLS,
                priority = 5,
                longPressTimeout = 500L
            ),
            GestureEntity(
                gestureType = GestureType.TWO_FINGER_TAP,
                zone = GestureZone.CENTER,
                action = GestureAction.TOGGLE_FULLSCREEN,
                priority = 5,
                requiredFingers = 2
            ),
            
            // Speed controls
            GestureEntity(
                gestureType = GestureType.LONG_PRESS,
                zone = GestureZone.RIGHT_HALF,
                action = GestureAction.SPEED_UP,
                priority = 4,
                longPressTimeout = 1000L
            ),
            GestureEntity(
                gestureType = GestureType.LONG_PRESS,
                zone = GestureZone.LEFT_HALF,
                action = GestureAction.SPEED_DOWN,
                priority = 4,
                longPressTimeout = 1000L
            )
        )
    }
    
    private suspend fun createBuiltInProfiles() {
        // YouTube-like profile
        val youtubeProfile = GestureProfileEntity(
            name = "YouTube Style",
            description = "Gestures similar to YouTube player",
            isBuiltIn = true,
            iconName = "ic_youtube"
        )
        gestureDao.insertProfile(youtubeProfile)
        
        // Netflix-like profile
        val netflixProfile = GestureProfileEntity(
            name = "Netflix Style",
            description = "Gestures similar to Netflix player",
            isBuiltIn = true,
            iconName = "ic_netflix"
        )
        gestureDao.insertProfile(netflixProfile)
        
        // Minimal profile
        val minimalProfile = GestureProfileEntity(
            name = "Minimal",
            description = "Only essential gestures",
            isBuiltIn = true,
            iconName = "ic_minimal"
        )
        gestureDao.insertProfile(minimalProfile)
        
        // Power user profile
        val powerProfile = GestureProfileEntity(
            name = "Power User",
            description = "All gestures enabled with advanced controls",
            isBuiltIn = true,
            iconName = "ic_power"
        )
        gestureDao.insertProfile(powerProfile)
    }
}