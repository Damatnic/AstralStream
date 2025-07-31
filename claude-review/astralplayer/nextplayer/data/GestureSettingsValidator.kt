package com.astralplayer.nextplayer.data

import kotlin.math.max
import kotlin.math.min

/**
 * Validator for gesture settings to ensure all values are within valid bounds
 */
object GestureSettingsValidator {
    
    // Validation bounds
    private const val MIN_SENSITIVITY = 0.1f
    private const val MAX_SENSITIVITY = 3.0f
    private const val MIN_DEAD_ZONE = 0f
    private const val MAX_DEAD_ZONE = 100f
    private const val MIN_GESTURE_DISTANCE = 0f
    private const val MAX_GESTURE_DISTANCE = 200f
    private const val MIN_SEEK_STEP = 100L // 0.1 seconds
    private const val MAX_SEEK_STEP = 60000L // 60 seconds
    private const val MIN_TAP_TIMEOUT = 50L
    private const val MAX_TAP_TIMEOUT = 1000L
    private const val MIN_TRIGGER_DURATION = 100L
    private const val MAX_TRIGGER_DURATION = 2000L
    private const val MIN_ZOOM = 0.1f
    private const val MAX_ZOOM = 10f
    private const val MIN_BRIGHTNESS = 0.01f
    private const val MAX_BRIGHTNESS = 1.0f
    private const val MIN_SPEED_MULTIPLIER = 0.5f
    private const val MAX_SPEED_MULTIPLIER = 64f
    
    /**
     * Validates and corrects the entire gesture settings
     */
    fun validateSettings(settings: EnhancedGestureSettings): EnhancedGestureSettings {
        return settings.copy(
            general = validateGeneralSettings(settings.general),
            seeking = validateSeekingSettings(settings.seeking),
            volume = validateVolumeSettings(settings.volume),
            brightness = validateBrightnessSettings(settings.brightness),
            doubleTap = validateDoubleTapSettings(settings.doubleTap),
            longPress = validateLongPressSettings(settings.longPress),
            pinchZoom = validatePinchZoomSettings(settings.pinchZoom)
        )
    }
    
    /**
     * Validates general gesture settings
     */
    fun validateGeneralSettings(settings: GeneralGestureSettings): GeneralGestureSettings {
        return settings.copy(
            gestureDeadZone = settings.gestureDeadZone.coerceIn(MIN_DEAD_ZONE, MAX_DEAD_ZONE),
            minimumGestureDistance = settings.minimumGestureDistance.coerceIn(MIN_GESTURE_DISTANCE, MAX_GESTURE_DISTANCE)
        )
    }
    
    /**
     * Validates seeking gesture settings
     */
    fun validateSeekingSettings(settings: SeekingGestureSettings): SeekingGestureSettings {
        return settings.copy(
            sensitivity = settings.sensitivity.coerceIn(MIN_SENSITIVITY, MAX_SENSITIVITY),
            minimumSwipeDistance = settings.minimumSwipeDistance.coerceIn(MIN_GESTURE_DISTANCE, MAX_GESTURE_DISTANCE),
            seekStepSize = settings.seekStepSize.coerceIn(MIN_SEEK_STEP, MAX_SEEK_STEP),
            fineSeekThreshold = settings.fineSeekThreshold.coerceIn(0f, 1f)
        )
    }
    
    /**
     * Validates volume gesture settings
     */
    fun validateVolumeSettings(settings: VolumeGestureSettings): VolumeGestureSettings {
        return settings.copy(
            sensitivity = settings.sensitivity.coerceIn(MIN_SENSITIVITY, MAX_SENSITIVITY)
        )
    }
    
    /**
     * Validates brightness gesture settings
     */
    fun validateBrightnessSettings(settings: BrightnessGestureSettings): BrightnessGestureSettings {
        return settings.copy(
            sensitivity = settings.sensitivity.coerceIn(MIN_SENSITIVITY, MAX_SENSITIVITY),
            minimumBrightness = settings.minimumBrightness.coerceIn(MIN_BRIGHTNESS, MAX_BRIGHTNESS),
            maximumBrightness = settings.maximumBrightness.coerceIn(
                settings.minimumBrightness, 
                MAX_BRIGHTNESS
            )
        )
    }
    
    /**
     * Validates double tap gesture settings
     */
    fun validateDoubleTapSettings(settings: DoubleTapGestureSettings): DoubleTapGestureSettings {
        return settings.copy(
            seekAmount = settings.seekAmount.coerceIn(MIN_SEEK_STEP, MAX_SEEK_STEP),
            tapTimeout = settings.tapTimeout.coerceIn(MIN_TAP_TIMEOUT, MAX_TAP_TIMEOUT),
            maxTapDistance = settings.maxTapDistance.coerceIn(MIN_GESTURE_DISTANCE, MAX_GESTURE_DISTANCE)
        )
    }
    
    /**
     * Validates long press gesture settings
     */
    fun validateLongPressSettings(settings: LongPressGestureSettings): LongPressGestureSettings {
        // Validate speed progression list
        val validatedSpeedProgression = settings.speedProgression.map { speed ->
            speed.coerceIn(MIN_SPEED_MULTIPLIER, MAX_SPEED_MULTIPLIER)
        }.sorted() // Ensure speeds are in ascending order
        
        // Ensure we have at least one speed level
        val finalSpeedProgression = if (validatedSpeedProgression.isEmpty()) {
            listOf(1f, 2f, 4f, 8f, 16f, 32f)
        } else {
            validatedSpeedProgression
        }
        
        return settings.copy(
            triggerDuration = settings.triggerDuration.coerceIn(MIN_TRIGGER_DURATION, MAX_TRIGGER_DURATION),
            speedProgression = finalSpeedProgression,
            speedAccelerationInterval = settings.speedAccelerationInterval.coerceIn(100L, 5000L),
            maxSpeed = settings.maxSpeed.coerceIn(MIN_SPEED_MULTIPLIER, MAX_SPEED_MULTIPLIER),
            directionChangeThreshold = settings.directionChangeThreshold.coerceIn(MIN_GESTURE_DISTANCE, MAX_GESTURE_DISTANCE)
        )
    }
    
    /**
     * Validates pinch zoom gesture settings
     */
    fun validatePinchZoomSettings(settings: PinchZoomGestureSettings): PinchZoomGestureSettings {
        // Ensure min zoom is less than max zoom
        val validMinZoom = settings.minZoom.coerceIn(MIN_ZOOM, MAX_ZOOM)
        val validMaxZoom = settings.maxZoom.coerceIn(validMinZoom, MAX_ZOOM)
        
        return settings.copy(
            sensitivity = settings.sensitivity.coerceIn(MIN_SENSITIVITY, MAX_SENSITIVITY),
            minZoom = validMinZoom,
            maxZoom = validMaxZoom
        )
    }
    
    /**
     * Checks if a value is within the valid range
     */
    fun <T : Comparable<T>> isInRange(value: T, min: T, max: T): Boolean {
        return value >= min && value <= max
    }
    
    /**
     * Validates a single sensitivity value
     */
    fun validateSensitivity(sensitivity: Float): Float {
        return sensitivity.coerceIn(MIN_SENSITIVITY, MAX_SENSITIVITY)
    }
    
    /**
     * Validates a duration value (in milliseconds)
     */
    fun validateDuration(duration: Long, min: Long = 0L, max: Long = Long.MAX_VALUE): Long {
        return duration.coerceIn(min, max)
    }
    
    /**
     * Validates a distance value (in pixels)
     */
    fun validateDistance(distance: Float): Float {
        return distance.coerceIn(MIN_GESTURE_DISTANCE, MAX_GESTURE_DISTANCE)
    }
    
    /**
     * Returns validation errors for the given settings
     */
    fun getValidationErrors(settings: EnhancedGestureSettings): List<ValidationError> {
        val errors = mutableListOf<ValidationError>()
        
        // Check general settings
        with(settings.general) {
            if (!isInRange(gestureDeadZone, MIN_DEAD_ZONE, MAX_DEAD_ZONE)) {
                errors.add(ValidationError("General", "gestureDeadZone", gestureDeadZone, MIN_DEAD_ZONE, MAX_DEAD_ZONE))
            }
            if (!isInRange(minimumGestureDistance, MIN_GESTURE_DISTANCE, MAX_GESTURE_DISTANCE)) {
                errors.add(ValidationError("General", "minimumGestureDistance", minimumGestureDistance, MIN_GESTURE_DISTANCE, MAX_GESTURE_DISTANCE))
            }
        }
        
        // Check seeking settings
        with(settings.seeking) {
            if (!isInRange(sensitivity, MIN_SENSITIVITY, MAX_SENSITIVITY)) {
                errors.add(ValidationError("Seeking", "sensitivity", sensitivity, MIN_SENSITIVITY, MAX_SENSITIVITY))
            }
            if (!isInRange(seekStepSize, MIN_SEEK_STEP, MAX_SEEK_STEP)) {
                errors.add(ValidationError("Seeking", "seekStepSize", seekStepSize, MIN_SEEK_STEP, MAX_SEEK_STEP))
            }
        }
        
        // Check brightness settings
        with(settings.brightness) {
            if (minimumBrightness >= maximumBrightness) {
                errors.add(ValidationError("Brightness", "brightness range", 
                    "Min ($minimumBrightness) must be less than Max ($maximumBrightness)"))
            }
        }
        
        // Check pinch zoom settings
        with(settings.pinchZoom) {
            if (minZoom >= maxZoom) {
                errors.add(ValidationError("PinchZoom", "zoom range", 
                    "Min ($minZoom) must be less than Max ($maxZoom)"))
            }
        }
        
        return errors
    }
    
    /**
     * Data class representing a validation error
     */
    data class ValidationError(
        val category: String,
        val field: String,
        val currentValue: Any,
        val minValue: Any? = null,
        val maxValue: Any? = null
    ) {
        constructor(category: String, field: String, message: String) : 
            this(category, field, message, null, null)
        
        override fun toString(): String {
            return if (minValue != null && maxValue != null) {
                "$category.$field: $currentValue is outside valid range [$minValue, $maxValue]"
            } else {
                "$category.$field: $currentValue"
            }
        }
    }
}

/**
 * Extension function to validate settings
 */
fun EnhancedGestureSettings.validate(): EnhancedGestureSettings {
    return GestureSettingsValidator.validateSettings(this)
}

/**
 * Extension function to check if settings are valid
 */
fun EnhancedGestureSettings.isValid(): Boolean {
    return GestureSettingsValidator.getValidationErrors(this).isEmpty()
}

/**
 * Extension function to get validation errors
 */
fun EnhancedGestureSettings.getValidationErrors(): List<GestureSettingsValidator.ValidationError> {
    return GestureSettingsValidator.getValidationErrors(this)
}