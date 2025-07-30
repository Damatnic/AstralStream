package com.astralplayer.nextplayer.data.gesture.error

import android.content.Context
import android.util.Log
import androidx.compose.runtime.Stable
import com.astralplayer.nextplayer.data.GestureAction
import com.astralplayer.nextplayer.data.EnhancedGestureSettings
import com.astralplayer.nextplayer.data.GestureType
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.concurrent.atomic.AtomicInteger

/**
 * Comprehensive error handling for gesture system
 */
@Stable
class GestureErrorHandler(
    private val context: Context,
    private val scope: CoroutineScope
) {
    companion object {
        private const val TAG = "GestureErrorHandler"
        private const val MAX_ERRORS_PER_MINUTE = 10
        private const val ERROR_WINDOW_MS = 60000L
        private const val MAX_RECOVERY_ATTEMPTS = 3
    }
    
    // Error tracking
    private val errorCount = AtomicInteger(0)
    private val errorHistory = mutableListOf<ErrorRecord>()
    private val _errorState = MutableStateFlow(ErrorState())
    val errorState: StateFlow<ErrorState> = _errorState.asStateFlow()
    
    // Recovery strategies
    private val recoveryStrategies = mapOf(
        ErrorType.GESTURE_DETECTION_FAILED to ::recoverFromDetectionFailure,
        ErrorType.SETTINGS_CORRUPTION to ::recoverFromSettingsCorruption,
        ErrorType.PERFORMANCE_DEGRADATION to ::recoverFromPerformanceDegradation,
        ErrorType.MEMORY_PRESSURE to ::recoverFromMemoryPressure,
        ErrorType.GESTURE_CONFLICT to ::recoverFromGestureConflict
    )
    
    init {
        // Start error monitoring
        scope.launch {
            monitorErrorRate()
        }
    }
    
    /**
     * Handle gesture error with recovery
     */
    fun handleError(
        error: GestureError,
        onRecovery: suspend () -> Unit = {}
    ) {
        scope.launch {
            try {
                // Log error
                logError(error)
                
                // Track error
                trackError(error)
                
                // Check if we should attempt recovery
                if (shouldAttemptRecovery(error)) {
                    attemptRecovery(error, onRecovery)
                } else {
                    // Fallback to safe mode
                    enterSafeMode(error)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in error handler", e)
                // Last resort - disable gestures
                disableGestures()
            }
        }
    }
    
    /**
     * Handle uncaught exceptions in gesture processing
     */
    fun handleUncaughtException(
        exception: Exception,
        gestureType: GestureType? = null
    ) {
        val error = GestureError(
            type = ErrorType.UNCAUGHT_EXCEPTION,
            message = exception.message ?: "Unknown error",
            exception = exception,
            gestureType = gestureType,
            timestamp = System.currentTimeMillis()
        )
        
        handleError(error)
    }
    
    /**
     * Track error for rate limiting
     */
    private fun trackError(error: GestureError) {
        errorCount.incrementAndGet()
        errorHistory.add(
            ErrorRecord(
                error = error,
                timestamp = System.currentTimeMillis()
            )
        )
        
        // Clean old errors
        val cutoffTime = System.currentTimeMillis() - ERROR_WINDOW_MS
        errorHistory.removeAll { it.timestamp < cutoffTime }
        
        // Update error state
        _errorState.update { state ->
            state.copy(
                lastError = error,
                errorCount = errorHistory.size,
                isInSafeMode = state.isInSafeMode,
                recoveryAttempts = state.recoveryAttempts
            )
        }
    }
    
    /**
     * Check if recovery should be attempted
     */
    private fun shouldAttemptRecovery(error: GestureError): Boolean {
        val state = _errorState.value
        
        return when {
            state.isInSafeMode -> false
            state.recoveryAttempts >= MAX_RECOVERY_ATTEMPTS -> false
            errorHistory.size > MAX_ERRORS_PER_MINUTE -> false
            error.type == ErrorType.UNCAUGHT_EXCEPTION -> false
            else -> true
        }
    }
    
    /**
     * Attempt recovery based on error type
     */
    private suspend fun attemptRecovery(
        error: GestureError,
        onRecovery: suspend () -> Unit
    ) {
        _errorState.update { it.copy(recoveryAttempts = it.recoveryAttempts + 1) }
        
        val strategy = recoveryStrategies[error.type]
        if (strategy != null) {
            try {
                strategy(error)
                onRecovery()
                
                // Reset recovery attempts on success
                _errorState.update { it.copy(recoveryAttempts = 0) }
            } catch (e: Exception) {
                Log.e(TAG, "Recovery failed", e)
                enterSafeMode(error)
            }
        } else {
            // No specific recovery strategy
            performGenericRecovery(error)
        }
    }
    
    /**
     * Recovery strategies
     */
    
    private suspend fun recoverFromDetectionFailure(error: GestureError) {
        Log.d(TAG, "Recovering from detection failure")
        
        // Reset gesture state
        delay(100)
        
        // Clear any stuck gestures
        // Reinitialize detectors if needed
    }
    
    private suspend fun recoverFromSettingsCorruption(error: GestureError) {
        Log.d(TAG, "Recovering from settings corruption")
        
        // Load default settings
        // Validate and fix corrupted values
        // Save corrected settings
    }
    
    private suspend fun recoverFromPerformanceDegradation(error: GestureError) {
        Log.d(TAG, "Recovering from performance degradation")
        
        // Reduce gesture sensitivity
        // Disable complex gestures temporarily
        // Clear caches
    }
    
    private suspend fun recoverFromMemoryPressure(error: GestureError) {
        Log.d(TAG, "Recovering from memory pressure")
        
        // Release unused resources
        // Reduce history sizes
        // Trigger garbage collection
        System.gc()
    }
    
    private suspend fun recoverFromGestureConflict(error: GestureError) {
        Log.d(TAG, "Recovering from gesture conflict")
        
        // Reset gesture priorities
        // Clear conflicting gesture states
    }
    
    private suspend fun performGenericRecovery(error: GestureError) {
        Log.d(TAG, "Performing generic recovery")
        
        // Wait a bit
        delay(500)
        
        // Try to continue
    }
    
    /**
     * Enter safe mode with limited gestures
     */
    private fun enterSafeMode(error: GestureError) {
        Log.w(TAG, "Entering safe mode due to: ${error.message}")
        
        _errorState.update { it.copy(isInSafeMode = true) }
        
        // Notify user
        scope.launch {
            showErrorNotification("Gesture system in safe mode")
        }
    }
    
    /**
     * Disable gestures completely
     */
    private fun disableGestures() {
        Log.e(TAG, "Disabling gesture system")
        
        _errorState.update { 
            it.copy(
                isInSafeMode = true,
                gesturesDisabled = true
            )
        }
        
        // Notify user
        scope.launch {
            showErrorNotification("Gestures temporarily disabled")
        }
    }
    
    /**
     * Monitor error rate
     */
    private suspend fun monitorErrorRate() {
        while (scope.isActive) {
            delay(10000) // Check every 10 seconds
            
            val recentErrors = errorHistory.filter {
                System.currentTimeMillis() - it.timestamp < ERROR_WINDOW_MS
            }
            
            if (recentErrors.size > MAX_ERRORS_PER_MINUTE) {
                Log.w(TAG, "High error rate detected: ${recentErrors.size} errors")
                enterSafeMode(
                    GestureError(
                        type = ErrorType.HIGH_ERROR_RATE,
                        message = "Too many errors detected",
                        timestamp = System.currentTimeMillis()
                    )
                )
            }
        }
    }
    
    /**
     * Log error for debugging
     */
    private fun logError(error: GestureError) {
        when (error.type.severity) {
            ErrorSeverity.LOW -> Log.d(TAG, error.toString())
            ErrorSeverity.MEDIUM -> Log.w(TAG, error.toString())
            ErrorSeverity.HIGH -> Log.e(TAG, error.toString(), error.exception)
            ErrorSeverity.CRITICAL -> {
                Log.e(TAG, "CRITICAL ERROR: $error", error.exception)
                // Could also send to crash reporting service
            }
        }
    }
    
    /**
     * Show error notification to user
     */
    private suspend fun showErrorNotification(message: String) {
        // Implementation depends on UI framework
        // Could emit to a shared flow that UI observes
    }
    
    /**
     * Reset error state
     */
    fun resetErrorState() {
        errorCount.set(0)
        errorHistory.clear()
        _errorState.value = ErrorState()
    }
    
    /**
     * Get safe mode settings
     */
    fun getSafeModeSettings(baseSettings: EnhancedGestureSettings): EnhancedGestureSettings {
        return if (_errorState.value.isInSafeMode) {
            baseSettings.copy(
                general = baseSettings.general.copy(
                    minimumGestureDistance = baseSettings.general.minimumGestureDistance * 1.5f
                ),
                seeking = baseSettings.seeking.copy(
                    enableFineSeek = false
                ),
                longPress = baseSettings.longPress.copy(
                    isEnabled = false // Disable complex gestures
                ),
                pinchZoom = baseSettings.pinchZoom.copy(
                    isEnabled = false
                )
            )
        } else {
            baseSettings
        }
    }
    
    // Data classes
    
    data class GestureError(
        val type: ErrorType,
        val message: String,
        val exception: Exception? = null,
        val gestureType: GestureType? = null,
        val timestamp: Long,
        val context: Map<String, Any> = emptyMap()
    )
    
    enum class ErrorType(val severity: ErrorSeverity) {
        GESTURE_DETECTION_FAILED(ErrorSeverity.MEDIUM),
        SETTINGS_CORRUPTION(ErrorSeverity.HIGH),
        PERFORMANCE_DEGRADATION(ErrorSeverity.MEDIUM),
        MEMORY_PRESSURE(ErrorSeverity.HIGH),
        GESTURE_CONFLICT(ErrorSeverity.LOW),
        UNCAUGHT_EXCEPTION(ErrorSeverity.CRITICAL),
        HIGH_ERROR_RATE(ErrorSeverity.HIGH),
        INVALID_INPUT(ErrorSeverity.LOW),
        INITIALIZATION_FAILED(ErrorSeverity.CRITICAL),
        PERMISSION_DENIED(ErrorSeverity.HIGH)
    }
    
    enum class ErrorSeverity {
        LOW, MEDIUM, HIGH, CRITICAL
    }
    
    data class ErrorRecord(
        val error: GestureError,
        val timestamp: Long
    )
    
    data class ErrorState(
        val lastError: GestureError? = null,
        val errorCount: Int = 0,
        val isInSafeMode: Boolean = false,
        val gesturesDisabled: Boolean = false,
        val recoveryAttempts: Int = 0
    )
}

/**
 * Resilient gesture action executor
 */
class ResilientGestureExecutor(
    private val errorHandler: GestureErrorHandler,
    private val scope: CoroutineScope
) {
    /**
     * Execute gesture action with error handling
     */
    suspend fun <T> executeGestureAction(
        action: GestureAction,
        block: suspend () -> T
    ): Result<T> {
        return try {
            Result.success(block())
        } catch (e: CancellationException) {
            throw e // Don't catch coroutine cancellation
        } catch (e: Exception) {
            errorHandler.handleUncaughtException(e, getGestureType(action))
            Result.failure(e)
        }
    }
    
    /**
     * Execute with retry
     */
    suspend fun <T> executeWithRetry(
        maxRetries: Int = 3,
        delayMs: Long = 100,
        block: suspend () -> T
    ): Result<T> {
        var lastException: Exception? = null
        
        repeat(maxRetries) { attempt ->
            try {
                return Result.success(block())
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                lastException = e
                if (attempt < maxRetries - 1) {
                    delay(delayMs * (attempt + 1))
                }
            }
        }
        
        return Result.failure(lastException ?: Exception("Unknown error"))
    }
    
    private fun getGestureType(action: GestureAction): GestureType? {
        return when (action) {
            is GestureAction.Seek -> GestureType.HORIZONTAL_SEEK
            is GestureAction.VolumeChange -> GestureType.VERTICAL_VOLUME
            is GestureAction.BrightnessChange -> GestureType.VERTICAL_BRIGHTNESS
            is GestureAction.DoubleTapSeek -> GestureType.DOUBLE_TAP
            is GestureAction.TogglePlayPause -> GestureType.SINGLE_TAP
            is GestureAction.LongPressSeek -> GestureType.LONG_PRESS
            is GestureAction.PinchZoom -> GestureType.PINCH_ZOOM
            else -> null
        }
    }
}

/**
 * Gesture validation to prevent errors
 */
object GestureValidator {
    /**
     * Validate gesture input
     */
    fun validateGestureInput(
        gestureType: GestureType,
        input: Any
    ): ValidationResult {
        return when (gestureType) {
            GestureType.HORIZONTAL_SEEK -> validateSeekInput(input)
            GestureType.VERTICAL_VOLUME -> validateVolumeInput(input)
            GestureType.VERTICAL_BRIGHTNESS -> validateBrightnessInput(input)
            GestureType.DOUBLE_TAP -> validateDoubleTapInput(input)
            GestureType.LONG_PRESS -> validateLongPressInput(input)
            GestureType.PINCH_ZOOM -> validatePinchInput(input)
            else -> ValidationResult.Valid
        }
    }
    
    private fun validateSeekInput(input: Any): ValidationResult {
        if (input !is Float) return ValidationResult.Invalid("Expected Float")
        if (input.isNaN() || input.isInfinite()) {
            return ValidationResult.Invalid("Invalid seek value")
        }
        return ValidationResult.Valid
    }
    
    private fun validateVolumeInput(input: Any): ValidationResult {
        if (input !is Float) return ValidationResult.Invalid("Expected Float")
        if (input !in -1f..1f) {
            return ValidationResult.Invalid("Volume delta out of range")
        }
        return ValidationResult.Valid
    }
    
    private fun validateBrightnessInput(input: Any): ValidationResult {
        if (input !is Float) return ValidationResult.Invalid("Expected Float")
        if (input !in -1f..1f) {
            return ValidationResult.Invalid("Brightness delta out of range")
        }
        return ValidationResult.Valid
    }
    
    private fun validateDoubleTapInput(input: Any): ValidationResult {
        // Validate double tap parameters
        return ValidationResult.Valid
    }
    
    private fun validateLongPressInput(input: Any): ValidationResult {
        // Validate long press parameters
        return ValidationResult.Valid
    }
    
    private fun validatePinchInput(input: Any): ValidationResult {
        if (input !is Float) return ValidationResult.Invalid("Expected Float")
        if (input !in 0.1f..10f) {
            return ValidationResult.Invalid("Zoom scale out of range")
        }
        return ValidationResult.Valid
    }
    
    sealed class ValidationResult {
        object Valid : ValidationResult()
        data class Invalid(val reason: String) : ValidationResult()
    }
}