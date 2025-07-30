package com.astralplayer.nextplayer.feature.player.gestures

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import com.astralplayer.nextplayer.feature.player.viewmodel.PlayerViewModel
// GestureType is in the same package, no import needed
import kotlinx.coroutines.launch

/**
 * Comprehensive gesture integration system
 * Integrates all enhanced gesture components into a unified system
 */

/**
 * Main comprehensive gesture system that integrates all enhanced components
 */
@Composable
fun ComprehensiveGestureSystem(
    viewModel: PlayerViewModel,
    gestureSettings: GestureSettings = GestureSettings(),
    overlayStyle: OverlayStyle = OverlayStyle.MODERN,
    hapticConfig: HapticFeedbackConfig = HapticFeedbackConfig(),
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    
    // Initialize haptic feedback manager
    val hapticManager = rememberHapticFeedbackManager(hapticConfig)
    
    // Gesture state management
    var isLongPressing by remember { mutableStateOf(false) }
    var currentGestureType by remember { mutableStateOf(VerticalGestureType.NONE) }
    var longPressSeekState by remember { mutableStateOf(LongPressSeekState()) }
    var showVolumeOverlay by remember { mutableStateOf(false) }
    var showBrightnessOverlay by remember { mutableStateOf(false) }
    var showLongPressOverlay by remember { mutableStateOf(false) }
    
    // Current values for overlays
    var currentVolume by remember { mutableFloatStateOf(0.5f) }
    var currentBrightness by remember { mutableFloatStateOf(0.5f) }
    var currentPosition by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(100000L) }
    
    Box(modifier = modifier.fillMaxSize()) {
        
        // Enhanced vertical gesture handler
        EnhancedVerticalGestureHandler(
            viewModel = viewModel,
            gestureSettings = gestureSettings,
            onGestureStart = {
                scope.launch {
                    hapticManager.provideHapticFeedback(
                        HapticPattern.LIGHT_TAP,
                        when (currentGestureType) {
                            VerticalGestureType.VOLUME -> GestureType.VOLUME
                            VerticalGestureType.BRIGHTNESS -> GestureType.BRIGHTNESS
                            VerticalGestureType.NONE -> null
                        }
                    )
                }
            },
            onGestureEnd = {
                showVolumeOverlay = false
                showBrightnessOverlay = false
                currentGestureType = VerticalGestureType.NONE
            }
        )
        
        // MX Player speed progression handler
        MxPlayerSpeedProgressionHandler(
            viewModel = viewModel,
            gestureSettings = gestureSettings,
            onGestureStart = {
                isLongPressing = true
                showLongPressOverlay = true
                scope.launch {
                    hapticManager.provideHapticFeedback(
                        HapticPattern.LONG_PRESS_START,
                        GestureType.LONG_PRESS
                    )
                }
            },
            onGestureEnd = {
                isLongPressing = false
                showLongPressOverlay = false
                longPressSeekState = longPressSeekState.copy(isActive = false)
            },
            onSpeedChange = { speed, direction ->
                longPressSeekState = longPressSeekState.copy(
                    seekSpeed = speed,
                    direction = direction,
                    isActive = true
                )
                
                // Provide haptic feedback for speed changes
                scope.launch {
                    val speedLevel = when {
                        speed >= 32f -> 5
                        speed >= 16f -> 4
                        speed >= 8f -> 3
                        speed >= 4f -> 2
                        speed >= 2f -> 1
                        else -> 0
                    }
                    hapticManager.provideLongPressSpeedChangeHapticFeedback(speedLevel, 5)
                }
            }
        )
        
        // Direction change handler
        DirectionChangeHandler(
            viewModel = viewModel,
            gestureSettings = gestureSettings,
            onDirectionChange = { direction, confidence ->
                longPressSeekState = longPressSeekState.copy(direction = direction)
                
                // Provide haptic feedback for direction changes
                scope.launch {
                    hapticManager.provideDirectionChangeHapticFeedback(confidence)
                }
            },
            onSpeedChange = { speed, direction ->
                longPressSeekState = longPressSeekState.copy(
                    seekSpeed = speed,
                    direction = direction
                )
            }
        )
        
        // Enhanced direction change detector
        EnhancedDirectionChangeDetector(
            viewModel = viewModel,
            gestureSettings = gestureSettings,
            onDirectionChange = { direction ->
                longPressSeekState = longPressSeekState.copy(direction = direction)
            },
            onSpeedChange = { speed ->
                longPressSeekState = longPressSeekState.copy(seekSpeed = speed)
            },
            onVisualFeedback = { visualState ->
                // Handle visual feedback state updates
                showLongPressOverlay = visualState.isVisible
            }
        )
        
        // Volume overlay (right side)
        Box(
            modifier = Modifier.align(Alignment.CenterEnd)
        ) {
            EnhancedVolumeOverlay(
                volume = currentVolume,
                isVisible = showVolumeOverlay,
                style = overlayStyle
            )
        }
        
        // Brightness overlay (left side)
        Box(
            modifier = Modifier.align(Alignment.CenterStart)
        ) {
            EnhancedBrightnessOverlay(
                brightness = currentBrightness,
                isVisible = showBrightnessOverlay,
                style = overlayStyle
            )
        }
        
        // Long press visual feedback (center)
        Box(
            modifier = Modifier.align(Alignment.Center)
        ) {
            EnhancedLongPressVisualFeedback(
                seekState = longPressSeekState,
                currentPosition = currentPosition,
                duration = duration,
                isVisible = showLongPressOverlay
            )
        }
        
        // Comprehensive long press overlay
        ComprehensiveLongPressOverlay(
            seekState = longPressSeekState,
            viewModel = viewModel,
            isVisible = showLongPressOverlay && isLongPressing
        )
    }
    
    // Update current values from viewModel
    LaunchedEffect(Unit) {
        // In a real implementation, observe viewModel state changes
        // This is a placeholder for demonstration
        currentVolume = viewModel.currentVolume
        currentBrightness = viewModel.currentBrightness
        // currentPosition = viewModel.currentPosition
        // duration = viewModel.duration
    }
    
    // Handle volume changes with haptic feedback
    LaunchedEffect(currentVolume) {
        if (showVolumeOverlay) {
            hapticManager.provideVolumeHapticFeedback(currentVolume, true)
        }
    }
    
    // Handle brightness changes with haptic feedback
    LaunchedEffect(currentBrightness) {
        if (showBrightnessOverlay) {
            hapticManager.provideBrightnessHapticFeedback(currentBrightness, true)
        }
    }
}

/**
 * Gesture system configuration builder
 */
class GestureSystemConfigBuilder {
    private var gestureSettings = GestureSettings()
    private var overlayStyle = OverlayStyle.MODERN
    private var hapticConfig = HapticFeedbackConfig()
    
    fun withGestureSettings(settings: GestureSettings) = apply {
        gestureSettings = settings
    }
    
    fun withOverlayStyle(style: OverlayStyle) = apply {
        overlayStyle = style
    }
    
    fun withHapticConfig(config: HapticFeedbackConfig) = apply {
        hapticConfig = config
    }
    
    fun withVerticalGestures(enabled: Boolean) = apply {
        gestureSettings = gestureSettings.copy(
            vertical = gestureSettings.vertical.copy(
                volumeGestureEnabled = enabled,
                brightnessGestureEnabled = enabled
            )
        )
    }
    
    fun withLongPressGestures(enabled: Boolean) = apply {
        gestureSettings = gestureSettings.copy(
            longPress = gestureSettings.longPress.copy(
                enabled = enabled,
                speedGestureEnabled = gestureSettings.longPress.speedGestureEnabled
            )
        )
    }
    
    fun withHapticFeedback(enabled: Boolean, intensity: HapticIntensity = HapticIntensity.MEDIUM) = apply {
        hapticConfig = hapticConfig.copy(
            globalIntensity = if (enabled) intensity else HapticIntensity.DISABLED
        )
    }
    
    fun build(): GestureSystemConfig {
        return GestureSystemConfig(
            gestureSettings = gestureSettings,
            overlayStyle = overlayStyle,
            hapticConfig = hapticConfig
        )
    }
}

/**
 * Complete gesture system configuration
 */
data class GestureSystemConfig(
    val gestureSettings: GestureSettings,
    val overlayStyle: OverlayStyle,
    val hapticConfig: HapticFeedbackConfig
)

/**
 * Gesture system factory for easy creation
 */
object GestureSystemFactory {
    
    /**
     * Creates a default gesture system configuration
     */
    fun createDefault(): GestureSystemConfig {
        return GestureSystemConfigBuilder().build()
    }
    
    /**
     * Creates a minimal gesture system configuration
     */
    fun createMinimal(): GestureSystemConfig {
        return GestureSystemConfigBuilder()
            .withOverlayStyle(OverlayStyle.MINIMAL)
            .withHapticFeedback(enabled = false)
            .build()
    }
    
    /**
     * Creates a cosmic/futuristic gesture system configuration
     */
    fun createCosmic(): GestureSystemConfig {
        return GestureSystemConfigBuilder()
            .withOverlayStyle(OverlayStyle.COSMIC)
            .withHapticFeedback(enabled = true, HapticIntensity.STRONG)
            .build()
    }
    
    /**
     * Creates a classic gesture system configuration
     */
    fun createClassic(): GestureSystemConfig {
        return GestureSystemConfigBuilder()
            .withOverlayStyle(OverlayStyle.CLASSIC)
            .withHapticFeedback(enabled = true, HapticIntensity.LIGHT)
            .build()
    }
    
    /**
     * Creates a custom gesture system configuration
     */
    fun createCustom(
        enableVerticalGestures: Boolean = true,
        enableLongPressGestures: Boolean = true,
        overlayStyle: OverlayStyle = OverlayStyle.MODERN,
        hapticIntensity: HapticIntensity = HapticIntensity.MEDIUM
    ): GestureSystemConfig {
        return GestureSystemConfigBuilder()
            .withVerticalGestures(enableVerticalGestures)
            .withLongPressGestures(enableLongPressGestures)
            .withOverlayStyle(overlayStyle)
            .withHapticFeedback(enabled = hapticIntensity != HapticIntensity.DISABLED, hapticIntensity)
            .build()
    }
}

/**
 * Gesture system integration helper
 */
object GestureSystemIntegration {
    
    /**
     * Integrates the comprehensive gesture system with the existing player
     */
    @Composable
    fun IntegrateWithPlayer(
        viewModel: PlayerViewModel,
        config: GestureSystemConfig = GestureSystemFactory.createDefault(),
        modifier: Modifier = Modifier
    ) {
        ComprehensiveGestureSystem(
            viewModel = viewModel,
            gestureSettings = config.gestureSettings,
            overlayStyle = config.overlayStyle,
            hapticConfig = config.hapticConfig,
            modifier = modifier
        )
    }
    
    /**
     * Creates a gesture-enabled player screen
     */
    @Composable
    fun GestureEnabledPlayerScreen(
        viewModel: PlayerViewModel,
        config: GestureSystemConfig = GestureSystemFactory.createDefault(),
        onBack: () -> Unit = {},
        modifier: Modifier = Modifier
    ) {
        Box(modifier = modifier.fillMaxSize()) {
            // Player content would go here
            // This is where the existing ModernVideoPlayerScreen or SimpleVideoPlayerScreen would be placed
            
            // Overlay the comprehensive gesture system
            IntegrateWithPlayer(
                viewModel = viewModel,
                config = config
            )
        }
    }
}

/**
 * Usage examples and documentation
 */
object GestureSystemExamples {
    
    /**
     * Example: Basic integration with default settings
     */
    @Composable
    fun BasicIntegrationExample(viewModel: PlayerViewModel) {
        GestureSystemIntegration.IntegrateWithPlayer(
            viewModel = viewModel,
            config = GestureSystemFactory.createDefault()
        )
    }
    
    /**
     * Example: Custom configuration with specific settings
     */
    @Composable
    fun CustomConfigurationExample(viewModel: PlayerViewModel) {
        val customConfig = GestureSystemFactory.createCustom(
            enableVerticalGestures = true,
            enableLongPressGestures = true,
            overlayStyle = OverlayStyle.COSMIC,
            hapticIntensity = HapticIntensity.STRONG
        )
        
        GestureSystemIntegration.IntegrateWithPlayer(
            viewModel = viewModel,
            config = customConfig
        )
    }
    
    /**
     * Example: Minimal gesture system for performance-sensitive scenarios
     */
    @Composable
    fun MinimalGestureSystemExample(viewModel: PlayerViewModel) {
        GestureSystemIntegration.IntegrateWithPlayer(
            viewModel = viewModel,
            config = GestureSystemFactory.createMinimal()
        )
    }
    
    /**
     * Example: Full-featured gesture system with all enhancements
     */
    @Composable
    fun FullFeaturedGestureSystemExample(viewModel: PlayerViewModel) {
        val advancedConfig = GestureSystemConfigBuilder()
            .withGestureSettings(
                GestureSettings(
                    general = GeneralGestureSettings(
                        gesturesEnabled = true,
                        tapToToggleControls = true,
                        feedbackVibrationsEnabled = true,
                        visualFeedbackEnabled = true
                    ),
                    vertical = VerticalGestureSettings(
                        volumeGestureEnabled = true,
                        brightnessGestureEnabled = true,
                        volumeSensitivity = 1.2f,
                        brightnessSensitivity = 1.0f
                    ),
                    longPress = LongPressGestureSettings(
                        enabled = true,
                        speedGestureEnabled = true,
                        minSpeed = 0.5f,
                        maxSpeed = 32f,
                        showSpeedIndicator = true
                    )
                )
            )
            .withOverlayStyle(OverlayStyle.COSMIC)
            .withHapticConfig(
                HapticFeedbackConfig(
                    globalIntensity = HapticIntensity.STRONG,
                    enabledGestures = setOf(
                        GestureType.VOLUME,
                        GestureType.BRIGHTNESS,
                        GestureType.SEEK,
                        GestureType.LONG_PRESS,
                        GestureType.DIRECTION_CHANGE
                    )
                )
            )
            .build()
        
        GestureSystemIntegration.IntegrateWithPlayer(
            viewModel = viewModel,
            config = advancedConfig
        )
    }
}