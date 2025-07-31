package com.astralplayer.nextplayer.smarthome

import androidx.media3.common.MediaItem

/**
 * Data models for smart home integration features
 */

// Smart Device Types
enum class SmartDeviceType {
    VOICE_ASSISTANT,
    CAST_DEVICE,
    SMART_LIGHT,
    SMART_SPEAKER,
    SMART_DISPLAY,
    IoT_SENSOR
}

// Voice Actions
enum class VoiceAction {
    PLAY,
    PAUSE,
    SEEK,
    VOLUME,
    CAST_TO,
    LIGHTS,
    FIND_CONTENT,
    CONTROL_PLAYBACK
}

// Playback Actions
enum class PlaybackAction {
    PLAY,
    PAUSE,
    STOP,
    NEXT,
    PREVIOUS
}

// Casting Quality
enum class CastingQuality {
    AUTO,
    HIGH,
    MEDIUM,
    LOW,
    ORIGINAL
}

// Scene Mood
enum class SceneMood {
    ACTION,
    ROMANTIC,
    SUSPENSE,
    COMEDY,
    DRAMA,
    NEUTRAL
}

// Trigger Types
enum class TriggerType {
    TIME_BASED,
    CONTENT_BASED,
    DEVICE_STATE,
    LOCATION_BASED
}

// Automation Actions
enum class AutomationAction {
    DIM_LIGHTS,
    BRIGHTEN_LIGHTS,
    CLOSE_BLINDS,
    OPEN_BLINDS,
    SET_DO_NOT_DISTURB,
    DISABLE_DO_NOT_DISTURB,
    PAUSE_OTHER_MEDIA,
    ADJUST_THERMOSTAT
}

// Base Smart Device Class
sealed class SmartDevice(
    open val id: String,
    open val name: String,
    open val type: SmartDeviceType,
    open val isConnected: Boolean
)

// Voice Assistant Device
data class VoiceAssistant(
    override val id: String,
    override val name: String,
    override val type: SmartDeviceType = SmartDeviceType.VOICE_ASSISTANT,
    override val isConnected: Boolean
) : SmartDevice(id, name, type, isConnected)

// Cast Device
data class CastDevice(
    override val id: String,
    override val name: String,
    override val type: SmartDeviceType = SmartDeviceType.CAST_DEVICE,
    override val isConnected: Boolean,
    val capabilities: CastCapabilities
) : SmartDevice(id, name, type, isConnected)

// Smart Light Device
data class SmartLight(
    override val id: String,
    override val name: String,
    override val type: SmartDeviceType = SmartDeviceType.SMART_LIGHT,
    override val isConnected: Boolean,
    val currentColor: Int,
    val brightness: Float
) : SmartDevice(id, name, type, isConnected)

// Cast Capabilities
data class CastCapabilities(
    val supports4K: Boolean,
    val supportsHDR: Boolean,
    val supportsDolby: Boolean,
    val maxBitrate: Int
)

// Casting Options
data class CastingOptions(
    val quality: CastingQuality,
    val audioSync: Boolean,
    val subtitles: Boolean
)

// Voice Command
data class VoiceCommand(
    val action: VoiceAction,
    val parameters: Map<String, Any>
)

// Scene Information
data class SceneInfo(
    val dominantColor: Int,
    val brightness: Float,
    val contrast: Float,
    val mood: SceneMood
)

// Automation Trigger
data class AutomationTrigger(
    val id: String,
    val type: TriggerType,
    val condition: String,
    val actions: List<AutomationAction>
)

// Smart Home Session
data class SmartHomeSession(
    val id: String,
    val mediaItem: MediaItem,
    val configuration: SmartHomeConfiguration,
    val startTime: Long,
    val connectedDevices: List<SmartDevice>
)

// Smart Home Configuration
data class SmartHomeConfiguration(
    val enableVoiceControl: Boolean = true,
    val enableCasting: Boolean = true,
    val enableLightSync: Boolean = true,
    val enableAutomation: Boolean = true,
    val targetCastDevice: CastDevice? = null,
    val castingQuality: CastingQuality = CastingQuality.AUTO,
    val enableCastSubtitles: Boolean = true,
    val automationTriggers: List<AutomationTrigger> = emptyList()
)

// Smart Home Session Stats
data class SmartHomeSessionStats(
    val duration: Long,
    val voiceCommandsUsed: Int,
    val devicesConnected: Int,
    val automationTriggered: Int
)

// Smart Home Capabilities
data class SmartHomeCapabilities(
    val supportsGoogleAssistant: Boolean,
    val supportsAlexa: Boolean,
    val supportsChromecast: Boolean,
    val supportsAirPlay: Boolean,
    val supportsHue: Boolean,
    val supportsSmartThings: Boolean,
    val maxSimultaneousCasts: Int
)

// Smart Home Session Result
sealed class SmartHomeSessionResult {
    data class Success(val stats: SmartHomeSessionStats) : SmartHomeSessionResult()
    data class Error(val message: String) : SmartHomeSessionResult()
}

// Callbacks Interface
interface SmartHomeCallbacks {
    fun onSmartHomeInitialized()
    fun onDevicesDiscovered(devices: List<SmartDevice>)
    fun onSmartHomeSessionStarted(session: SmartHomeSession)
    fun onVoiceCommandProcessed(command: VoiceCommand)
    fun onPlaybackControl(action: PlaybackAction)
    fun onSeekRequested(position: Long)
    fun onVolumeChanged(level: Float)
    fun onContentSearchRequested(query: String)
    fun onPlaybackSpeedChanged(speed: Float)
    fun onCastDeviceSwitchRequested(device: CastDevice)
    fun onSmartHomeSessionEnded(result: SmartHomeSessionResult)
    fun onSmartHomeError(error: String)
}