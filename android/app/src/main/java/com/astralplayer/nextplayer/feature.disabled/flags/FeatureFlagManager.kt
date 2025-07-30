package com.astralplayer.nextplayer.feature.flags

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.util.*

private val Context.featureFlagDataStore: DataStore<Preferences> by preferencesDataStore(name = "feature_flags")

/**
 * Feature Flag Manager
 * Controls gradual rollout and A/B testing of new features
 */
class FeatureFlagManager(private val context: Context) {
    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
    }
    
    companion object {
        private val FEATURE_FLAGS_KEY = stringPreferencesKey("feature_flags")
        private val USER_COHORT_KEY = stringPreferencesKey("user_cohort")
        private val ROLLOUT_CONFIG_KEY = stringPreferencesKey("rollout_config")
    }
    
    private val _featureFlags = MutableStateFlow(getDefaultFeatureFlags())
    val featureFlags: StateFlow<FeatureFlags> = _featureFlags.asStateFlow()
    
    private val _userCohort = MutableStateFlow<UserCohort?>(null)
    val userCohort: StateFlow<UserCohort?> = _userCohort.asStateFlow()
    
    init {
        loadFeatureFlags()
        initializeUserCohort()
    }
    
    /**
     * Check if a feature is enabled for the current user
     */
    fun isFeatureEnabled(feature: Feature): Boolean {
        val flags = _featureFlags.value
        val cohort = _userCohort.value ?: return getDefaultFeatureState(feature)
        
        return when (feature) {
            Feature.ADVANCED_SEARCH -> flags.advancedSearch && 
                    isUserInRollout(feature, cohort)
            Feature.AI_SUBTITLES -> flags.aiSubtitles && 
                    isUserInRollout(feature, cohort)
            Feature.VOICE_CONTROL -> flags.voiceControl && 
                    isUserInRollout(feature, cohort)
            Feature.AUDIO_EQUALIZER -> flags.audioEqualizer && 
                    isUserInRollout(feature, cohort)
            Feature.SLEEP_TIMER -> flags.sleepTimer && 
                    isUserInRollout(feature, cohort)
            Feature.VIDEO_BOOKMARKS -> flags.videoBookmarks && 
                    isUserInRollout(feature, cohort)
            Feature.PERFORMANCE_OPTIMIZATION -> flags.performanceOptimization && 
                    isUserInRollout(feature, cohort)
            Feature.SOCIAL_SHARING -> flags.socialSharing && 
                    isUserInRollout(feature, cohort)
            Feature.COLLABORATIVE_PLAYLISTS -> flags.collaborativePlaylists && 
                    isUserInRollout(feature, cohort)
            Feature.WATCH_PARTIES -> flags.watchParties && 
                    isUserInRollout(feature, cohort)
            Feature.ADVANCED_GESTURES -> flags.advancedGestures
            Feature.PICTURE_IN_PICTURE -> flags.pictureInPicture
            Feature.CAST_INTEGRATION -> flags.castIntegration
            Feature.CLOUD_SYNC -> flags.cloudSync
        }
    }
    
    /**
     * Enable or disable a feature for testing
     */
    suspend fun setFeatureEnabled(feature: Feature, enabled: Boolean) {
        val currentFlags = _featureFlags.value
        val updatedFlags = when (feature) {
            Feature.ADVANCED_SEARCH -> currentFlags.copy(advancedSearch = enabled)
            Feature.AI_SUBTITLES -> currentFlags.copy(aiSubtitles = enabled)
            Feature.VOICE_CONTROL -> currentFlags.copy(voiceControl = enabled)
            Feature.AUDIO_EQUALIZER -> currentFlags.copy(audioEqualizer = enabled)
            Feature.SLEEP_TIMER -> currentFlags.copy(sleepTimer = enabled)
            Feature.VIDEO_BOOKMARKS -> currentFlags.copy(videoBookmarks = enabled)
            Feature.PERFORMANCE_OPTIMIZATION -> currentFlags.copy(performanceOptimization = enabled)
            Feature.SOCIAL_SHARING -> currentFlags.copy(socialSharing = enabled)
            Feature.COLLABORATIVE_PLAYLISTS -> currentFlags.copy(collaborativePlaylists = enabled)
            Feature.WATCH_PARTIES -> currentFlags.copy(watchParties = enabled)
            Feature.ADVANCED_GESTURES -> currentFlags.copy(advancedGestures = enabled)
            Feature.PICTURE_IN_PICTURE -> currentFlags.copy(pictureInPicture = enabled)
            Feature.CAST_INTEGRATION -> currentFlags.copy(castIntegration = enabled)
            Feature.CLOUD_SYNC -> currentFlags.copy(cloudSync = enabled)
        }
        
        _featureFlags.value = updatedFlags
        saveFeatureFlags(updatedFlags)
    }
    
    /**
     * Get feature rollout percentage
     */
    fun getFeatureRolloutPercentage(feature: Feature): Int {
        return when (feature) {
            Feature.ADVANCED_SEARCH -> 100 // Fully rolled out
            Feature.AI_SUBTITLES -> 80
            Feature.VOICE_CONTROL -> 60
            Feature.AUDIO_EQUALIZER -> 100
            Feature.SLEEP_TIMER -> 100
            Feature.VIDEO_BOOKMARKS -> 90
            Feature.PERFORMANCE_OPTIMIZATION -> 100
            Feature.SOCIAL_SHARING -> 70
            Feature.COLLABORATIVE_PLAYLISTS -> 40
            Feature.WATCH_PARTIES -> 30
            Feature.ADVANCED_GESTURES -> 50
            Feature.PICTURE_IN_PICTURE -> 85
            Feature.CAST_INTEGRATION -> 75
            Feature.CLOUD_SYNC -> 20
        }
    }
    
    /**
     * Force enable all features (for testing)
     */
    suspend fun enableAllFeatures() {
        val allEnabledFlags = FeatureFlags(
            advancedSearch = true,
            aiSubtitles = true,
            voiceControl = true,
            audioEqualizer = true,
            sleepTimer = true,
            videoBookmarks = true,
            performanceOptimization = true,
            socialSharing = true,
            collaborativePlaylists = true,
            watchParties = true,
            advancedGestures = true,
            pictureInPicture = true,
            castIntegration = true,
            cloudSync = true
        )
        
        _featureFlags.value = allEnabledFlags
        saveFeatureFlags(allEnabledFlags)
    }
    
    /**
     * Reset to default feature flags
     */
    suspend fun resetToDefaults() {
        val defaultFlags = getDefaultFeatureFlags()
        _featureFlags.value = defaultFlags
        saveFeatureFlags(defaultFlags)
    }
    
    /**
     * Get user's experimentation cohort
     */
    fun getUserExperimentCohort(experiment: Experiment): ExperimentCohort {
        val cohort = _userCohort.value ?: return ExperimentCohort.CONTROL
        
        return when (experiment) {
            Experiment.AI_SUBTITLE_QUALITY -> {
                if (cohort.userId.hashCode() % 3 == 0) ExperimentCohort.VARIANT_A
                else if (cohort.userId.hashCode() % 3 == 1) ExperimentCohort.VARIANT_B
                else ExperimentCohort.CONTROL
            }
            Experiment.VOICE_COMMAND_ACCURACY -> {
                if (cohort.userId.hashCode() % 2 == 0) ExperimentCohort.VARIANT_A
                else ExperimentCohort.CONTROL
            }
            Experiment.SEARCH_ALGORITHM -> {
                if (cohort.installDate > System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000)) {
                    ExperimentCohort.VARIANT_A // New users get new algorithm
                } else ExperimentCohort.CONTROL
            }
            Experiment.UI_REDESIGN -> {
                if (cohort.userId.hashCode() % 4 == 0) ExperimentCohort.VARIANT_A
                else ExperimentCohort.CONTROL
            }
        }
    }
    
    /**
     * Check if user is in beta program
     */
    fun isUserInBetaProgram(): Boolean {
        return _userCohort.value?.isBetaUser == true
    }
    
    /**
     * Enable beta features for user
     */
    suspend fun enableBetaProgram(enable: Boolean) {
        val currentCohort = _userCohort.value ?: createUserCohort()
        val updatedCohort = currentCohort.copy(isBetaUser = enable)
        _userCohort.value = updatedCohort
        saveUserCohort(updatedCohort)
        
        // Enable beta features
        if (enable) {
            setFeatureEnabled(Feature.COLLABORATIVE_PLAYLISTS, true)
            setFeatureEnabled(Feature.WATCH_PARTIES, true)
            setFeatureEnabled(Feature.CLOUD_SYNC, true)
        }
    }
    
    private fun loadFeatureFlags() {
        context.featureFlagDataStore.data
            .map { preferences ->
                val flagsJson = preferences[FEATURE_FLAGS_KEY]
                if (flagsJson != null) {
                    try {
                        json.decodeFromString<FeatureFlags>(flagsJson)
                    } catch (e: Exception) {
                        getDefaultFeatureFlags()
                    }
                } else {
                    getDefaultFeatureFlags()
                }
            }
            .onEach { flags ->
                _featureFlags.value = flags
            }
            .launchIn(kotlinx.coroutines.GlobalScope)
    }
    
    private suspend fun saveFeatureFlags(flags: FeatureFlags) {
        context.featureFlagDataStore.edit { preferences ->
            preferences[FEATURE_FLAGS_KEY] = json.encodeToString(flags)
        }
    }
    
    private fun initializeUserCohort() {
        context.featureFlagDataStore.data
            .map { preferences ->
                val cohortJson = preferences[USER_COHORT_KEY]
                if (cohortJson != null) {
                    try {
                        json.decodeFromString<UserCohort>(cohortJson)
                    } catch (e: Exception) {
                        createUserCohort()
                    }
                } else {
                    createUserCohort()
                }
            }
            .onEach { cohort ->
                _userCohort.value = cohort
                if (cohort.userId.isEmpty()) {
                    val newCohort = createUserCohort()
                    _userCohort.value = newCohort
                    saveUserCohort(newCohort)
                }
            }
            .launchIn(kotlinx.coroutines.GlobalScope)
    }
    
    private fun createUserCohort(): UserCohort {
        return UserCohort(
            userId = UUID.randomUUID().toString(),
            installDate = System.currentTimeMillis(),
            appVersion = getAppVersion(),
            deviceInfo = getDeviceInfo(),
            isBetaUser = false
        )
    }
    
    private suspend fun saveUserCohort(cohort: UserCohort) {
        context.featureFlagDataStore.edit { preferences ->
            preferences[USER_COHORT_KEY] = json.encodeToString(cohort)
        }
    }
    
    private fun isUserInRollout(feature: Feature, cohort: UserCohort): Boolean {
        val rolloutPercentage = getFeatureRolloutPercentage(feature)
        val userHash = (cohort.userId + feature.name).hashCode()
        val userPercentile = kotlin.math.abs(userHash % 100)
        return userPercentile < rolloutPercentage
    }
    
    private fun getDefaultFeatureState(feature: Feature): Boolean {
        return when (feature) {
            Feature.ADVANCED_SEARCH -> true
            Feature.AI_SUBTITLES -> true
            Feature.VOICE_CONTROL -> false // Requires explicit enabling
            Feature.AUDIO_EQUALIZER -> true
            Feature.SLEEP_TIMER -> true
            Feature.VIDEO_BOOKMARKS -> true
            Feature.PERFORMANCE_OPTIMIZATION -> true
            Feature.SOCIAL_SHARING -> false
            Feature.COLLABORATIVE_PLAYLISTS -> false
            Feature.WATCH_PARTIES -> false
            Feature.ADVANCED_GESTURES -> true
            Feature.PICTURE_IN_PICTURE -> true
            Feature.CAST_INTEGRATION -> true
            Feature.CLOUD_SYNC -> false
        }
    }
    
    private fun getDefaultFeatureFlags(): FeatureFlags {
        return FeatureFlags(
            advancedSearch = true,
            aiSubtitles = true,
            voiceControl = false,
            audioEqualizer = true,
            sleepTimer = true,
            videoBookmarks = true,
            performanceOptimization = true,
            socialSharing = false,
            collaborativePlaylists = false,
            watchParties = false,
            advancedGestures = true,
            pictureInPicture = true,
            castIntegration = true,
            cloudSync = false
        )
    }
    
    private fun getAppVersion(): String {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName ?: "unknown"
        } catch (e: Exception) {
            "unknown"
        }
    }
    
    private fun getDeviceInfo(): String {
        return "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL} (API ${android.os.Build.VERSION.SDK_INT})"
    }
}

// Data classes and enums

@Serializable
data class FeatureFlags(
    val advancedSearch: Boolean = true,
    val aiSubtitles: Boolean = true,
    val voiceControl: Boolean = false,
    val audioEqualizer: Boolean = true,
    val sleepTimer: Boolean = true,
    val videoBookmarks: Boolean = true,
    val performanceOptimization: Boolean = true,
    val socialSharing: Boolean = false,
    val collaborativePlaylists: Boolean = false,
    val watchParties: Boolean = false,
    val advancedGestures: Boolean = true,
    val pictureInPicture: Boolean = true,
    val castIntegration: Boolean = true,
    val cloudSync: Boolean = false
)

@Serializable
data class UserCohort(
    val userId: String,
    val installDate: Long,
    val appVersion: String,
    val deviceInfo: String,
    val isBetaUser: Boolean = false
)

enum class Feature {
    ADVANCED_SEARCH,
    AI_SUBTITLES,
    VOICE_CONTROL,
    AUDIO_EQUALIZER,
    SLEEP_TIMER,
    VIDEO_BOOKMARKS,
    PERFORMANCE_OPTIMIZATION,
    SOCIAL_SHARING,
    COLLABORATIVE_PLAYLISTS,
    WATCH_PARTIES,
    ADVANCED_GESTURES,
    PICTURE_IN_PICTURE,
    CAST_INTEGRATION,
    CLOUD_SYNC
}

enum class Experiment {
    AI_SUBTITLE_QUALITY,
    VOICE_COMMAND_ACCURACY,
    SEARCH_ALGORITHM,
    UI_REDESIGN
}

enum class ExperimentCohort {
    CONTROL,
    VARIANT_A,
    VARIANT_B
}