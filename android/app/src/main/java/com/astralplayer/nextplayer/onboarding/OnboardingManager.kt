package com.astralplayer.nextplayer.onboarding

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Manages user onboarding flow and first-time experience
 */
class OnboardingManager(private val context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences("onboarding", Context.MODE_PRIVATE)
    
    private val _onboardingState = MutableStateFlow(OnboardingState())
    val onboardingState: StateFlow<OnboardingState> = _onboardingState.asStateFlow()
    
    private val _currentStep = MutableStateFlow(OnboardingStep.WELCOME)
    val currentStep: StateFlow<OnboardingStep> = _currentStep.asStateFlow()
    
    init {
        loadOnboardingState()
    }
    
    /**
     * Check if user needs onboarding
     */
    fun needsOnboarding(): Boolean {
        return !prefs.getBoolean(PREF_ONBOARDING_COMPLETED, false) || 
               prefs.getInt(PREF_ONBOARDING_VERSION, 0) < CURRENT_ONBOARDING_VERSION
    }
    
    /**
     * Start onboarding flow
     */
    fun startOnboarding() {
        _currentStep.value = OnboardingStep.WELCOME
        _onboardingState.value = OnboardingState(
            isActive = true,
            currentStepIndex = 0,
            totalSteps = OnboardingStep.values().size,
            completedSteps = emptySet()
        )
    }
    
    /**
     * Move to next onboarding step
     */
    fun nextStep() {
        val current = _currentStep.value
        val currentState = _onboardingState.value
        
        // Mark current step as completed
        val completedSteps = currentState.completedSteps + current
        
        val nextStep = getNextStep(current)
        if (nextStep != null) {
            _currentStep.value = nextStep
            _onboardingState.value = currentState.copy(
                currentStepIndex = currentState.currentStepIndex + 1,
                completedSteps = completedSteps
            )
        } else {
            // Onboarding completed
            completeOnboarding()
        }
    }
    
    /**
     * Go to previous onboarding step
     */
    fun previousStep() {
        val current = _currentStep.value
        val currentState = _onboardingState.value
        
        val previousStep = getPreviousStep(current)
        if (previousStep != null && currentState.currentStepIndex > 0) {
            _currentStep.value = previousStep
            _onboardingState.value = currentState.copy(
                currentStepIndex = currentState.currentStepIndex - 1
            )
        }
    }
    
    /**
     * Skip onboarding
     */
    fun skipOnboarding() {
        completeOnboarding()
    }
    
    /**
     * Mark onboarding as completed
     */
    fun completeOnboarding() {
        prefs.edit()
            .putBoolean(PREF_ONBOARDING_COMPLETED, true)
            .putInt(PREF_ONBOARDING_VERSION, CURRENT_ONBOARDING_VERSION)
            .putLong(PREF_ONBOARDING_COMPLETION_TIME, System.currentTimeMillis())
            .apply()
        
        _onboardingState.value = _onboardingState.value.copy(
            isActive = false,
            isCompleted = true
        )
    }
    
    /**
     * Reset onboarding (for testing or user request)
     */
    fun resetOnboarding() {
        prefs.edit()
            .putBoolean(PREF_ONBOARDING_COMPLETED, false)
            .putInt(PREF_ONBOARDING_VERSION, 0)
            .apply()
        
        _onboardingState.value = OnboardingState()
        _currentStep.value = OnboardingStep.WELCOME
    }
    
    /**
     * Skip to specific step
     */
    fun skipToStep(step: OnboardingStep) {
        val stepIndex = OnboardingStep.values().indexOf(step)
        if (stepIndex >= 0) {
            _currentStep.value = step
            _onboardingState.value = _onboardingState.value.copy(
                currentStepIndex = stepIndex
            )
        }
    }
    
    /**
     * Get onboarding progress percentage
     */
    fun getProgress(): Float {
        val state = _onboardingState.value
        return if (state.totalSteps > 0) {
            state.currentStepIndex.toFloat() / state.totalSteps
        } else 0f
    }
    
    /**
     * Get step configuration
     */
    fun getStepConfig(step: OnboardingStep): OnboardingStepConfig {
        return when (step) {
            OnboardingStep.WELCOME -> OnboardingStepConfig(
                title = "Welcome to AstralStream",
                description = "The most advanced video player with AI-powered features",
                icon = "welcome",
                primaryAction = "Get Started",
                secondaryAction = "Skip",
                showProgress = false
            )
            
            OnboardingStep.PERMISSIONS -> OnboardingStepConfig(
                title = "Permissions",
                description = "AstralStream needs access to your media files to provide the best experience",
                icon = "permissions",
                primaryAction = "Grant Permissions",
                secondaryAction = "Skip",
                showProgress = true,
                permissionsRequired = listOf(
                    android.Manifest.permission.READ_EXTERNAL_STORAGE,
                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
            )
            
            OnboardingStep.GESTURES -> OnboardingStepConfig(
                title = "Gesture Controls",
                description = "Control playback with intuitive gestures:\n• Swipe left/right to seek\n• Swipe up/down for volume/brightness\n• Double tap to skip",
                icon = "gestures",
                primaryAction = "Try Gestures",
                secondaryAction = "Skip",
                showProgress = true,
                hasInteractiveDemo = true
            )
            
            OnboardingStep.AI_FEATURES -> OnboardingStepConfig(
                title = "AI-Powered Features",
                description = "Experience smart subtitle generation, scene detection, and auto-quality adjustment",
                icon = "ai",
                primaryAction = "Enable AI",
                secondaryAction = "Maybe Later",
                showProgress = true
            )
            
            OnboardingStep.CUSTOMIZATION -> OnboardingStepConfig(
                title = "Customize Your Experience",
                description = "Set your preferred theme, quality settings, and player controls",
                icon = "customize",
                primaryAction = "Customize",
                secondaryAction = "Use Defaults",
                showProgress = true
            )
            
            OnboardingStep.COMPLETE -> OnboardingStepConfig(
                title = "You're All Set!",
                description = "AstralStream is ready to provide you with an amazing video experience",
                icon = "complete",
                primaryAction = "Start Watching",
                secondaryAction = null,
                showProgress = false
            )
        }
    }
    
    /**
     * Check if feature was introduced in onboarding
     */
    fun wasFeatureIntroduced(feature: String): Boolean {
        return prefs.getBoolean("feature_introduced_$feature", false)
    }
    
    /**
     * Mark feature as introduced
     */
    fun markFeatureIntroduced(feature: String) {
        prefs.edit().putBoolean("feature_introduced_$feature", true).apply()
    }
    
    /**
     * Get tips for current context
     */
    fun getContextualTips(context: OnboardingContext): List<OnboardingTip> {
        return when (context) {
            OnboardingContext.FIRST_VIDEO_LOAD -> listOf(
                OnboardingTip(
                    id = "gesture_controls",
                    title = "Try Gesture Controls",
                    description = "Swipe left or right to seek through the video",
                    action = "Show Me"
                )
            )
            
            OnboardingContext.FIRST_SUBTITLE_USE -> listOf(
                OnboardingTip(
                    id = "ai_subtitles",
                    title = "AI Subtitle Generation",
                    description = "Generate subtitles automatically using AI",
                    action = "Learn More"
                )
            )
            
            OnboardingContext.FIRST_PLAYLIST_CREATE -> listOf(
                OnboardingTip(
                    id = "smart_playlists",
                    title = "Smart Playlists",
                    description = "Create playlists that automatically organize your videos",
                    action = "Try It"
                )
            )
            
            OnboardingContext.SETTINGS_FIRST_TIME -> listOf(
                OnboardingTip(
                    id = "gesture_customization",
                    title = "Customize Gestures",
                    description = "Adjust gesture sensitivity and behavior to your liking",
                    action = "Customize"
                ),
                OnboardingTip(
                    id = "performance_optimization",
                    title = "Performance Settings",
                    description = "Optimize playback for your device",
                    action = "Optimize"
                )
            )
        }
    }
    
    private fun loadOnboardingState() {
        val isCompleted = prefs.getBoolean(PREF_ONBOARDING_COMPLETED, false)
        val version = prefs.getInt(PREF_ONBOARDING_VERSION, 0)
        
        _onboardingState.value = OnboardingState(
            isCompleted = isCompleted,
            version = version
        )
    }
    
    private fun getNextStep(current: OnboardingStep): OnboardingStep? {
        val steps = OnboardingStep.values()
        val currentIndex = steps.indexOf(current)
        return if (currentIndex < steps.size - 1) {
            steps[currentIndex + 1]
        } else null
    }
    
    private fun getPreviousStep(current: OnboardingStep): OnboardingStep? {
        val steps = OnboardingStep.values()
        val currentIndex = steps.indexOf(current)
        return if (currentIndex > 0) {
            steps[currentIndex - 1]
        } else null
    }
    
    companion object {
        private const val PREF_ONBOARDING_COMPLETED = "onboarding_completed"
        private const val PREF_ONBOARDING_VERSION = "onboarding_version"
        private const val PREF_ONBOARDING_COMPLETION_TIME = "onboarding_completion_time"
        private const val CURRENT_ONBOARDING_VERSION = 1
    }
}

// Data classes and enums for onboarding
data class OnboardingState(
    val isActive: Boolean = false,
    val isCompleted: Boolean = false,
    val currentStepIndex: Int = 0,
    val totalSteps: Int = OnboardingStep.values().size,
    val completedSteps: Set<OnboardingStep> = emptySet(),
    val version: Int = 0
)

enum class OnboardingStep {
    WELCOME,
    PERMISSIONS,
    GESTURES,
    AI_FEATURES,
    CUSTOMIZATION,
    COMPLETE
}

data class OnboardingStepConfig(
    val title: String,
    val description: String,
    val icon: String,
    val primaryAction: String,
    val secondaryAction: String?,
    val showProgress: Boolean = true,
    val permissionsRequired: List<String> = emptyList(),
    val hasInteractiveDemo: Boolean = false
)

enum class OnboardingContext {
    FIRST_VIDEO_LOAD,
    FIRST_SUBTITLE_USE,
    FIRST_PLAYLIST_CREATE,
    SETTINGS_FIRST_TIME
}

data class OnboardingTip(
    val id: String,
    val title: String,
    val description: String,
    val action: String
)