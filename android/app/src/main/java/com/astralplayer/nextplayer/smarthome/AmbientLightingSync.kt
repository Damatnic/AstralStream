package com.astralplayer.nextplayer.smarthome

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.util.Log
import androidx.media3.common.MediaItem
import kotlinx.coroutines.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Ambient Lighting Sync for real-time video-to-light synchronization
 */
@Singleton
class AmbientLightingSync @Inject constructor(
    private val context: Context,
    private val ioTDeviceManager: IoTDeviceManager
) {
    
    private var isSyncing = false
    private var syncJob: Job? = null
    private var currentLights = listOf<SmartLight>()
    private var frameAnalyzer: VideoFrameAnalyzer? = null
    
    suspend fun initialize(): Boolean {
        return try {
            frameAnalyzer = VideoFrameAnalyzer()
            Log.i("AmbientLighting", "Ambient lighting sync initialized")
            true
        } catch (e: Exception) {
            Log.e("AmbientLighting", "Failed to initialize ambient lighting", e)
            false
        }
    }
    
    suspend fun startSync(mediaItem: MediaItem, devices: List<SmartLight>) {
        if (devices.isEmpty()) {
            Log.w("AmbientLighting", "No smart lights available for sync")
            return
        }
        
        currentLights = devices
        isSyncing = true
        
        Log.i("AmbientLighting", "Starting ambient lighting sync with ${devices.size} lights")
        
        syncJob = CoroutineScope(Dispatchers.Default).launch {
            while (isSyncing) {
                try {
                    // Analyze current video frame
                    val sceneInfo = analyzeCurrentFrame()
                    
                    // Update lights based on scene
                    sceneInfo?.let {
                        updateLightsForScene(it)
                    }
                    
                    // Sync rate: 10 times per second for smooth transitions
                    delay(100)
                } catch (e: Exception) {
                    Log.e("AmbientLighting", "Error during lighting sync", e)
                    delay(1000) // Wait longer on error
                }
            }
        }
    }
    
    suspend fun stopSync() {
        isSyncing = false
        syncJob?.cancel()
        
        Log.i("AmbientLighting", "Stopping ambient lighting sync")
        
        // Restore lights to normal
        restoreLights()
    }
    
    suspend fun updateForScene(sceneInfo: SceneInfo) {
        if (isSyncing) {
            updateLightsForScene(sceneInfo)
        }
    }
    
    private suspend fun updateLightsForScene(sceneInfo: SceneInfo) {
        try {
            // Calculate lighting parameters based on scene
            val adjustedBrightness = calculateOptimalBrightness(sceneInfo)
            val adjustedColor = calculateOptimalColor(sceneInfo)
            
            // Apply mood-based adjustments
            val moodAdjustments = getMoodAdjustments(sceneInfo.mood)
            
            // Update each light
            currentLights.forEach { light ->
                val finalColor = blendColors(adjustedColor, moodAdjustments.color)
                val finalBrightness = (adjustedBrightness * moodAdjustments.brightnessMultiplier)
                    .coerceIn(0.1f, 1.0f)
                
                ioTDeviceManager.controlLight(
                    light = light,
                    color = finalColor,
                    brightness = finalBrightness
                )
            }
            
            Log.d("AmbientLighting", "Updated lights for ${sceneInfo.mood} scene")
        } catch (e: Exception) {
            Log.e("AmbientLighting", "Error updating lights for scene", e)
        }
    }
    
    private fun calculateOptimalBrightness(sceneInfo: SceneInfo): Float {
        // Adjust brightness for optimal viewing experience
        // Darker scenes = dimmer ambient lighting
        return when {
            sceneInfo.brightness < 0.3f -> 0.2f // Very dark scene
            sceneInfo.brightness < 0.5f -> 0.4f // Dark scene
            sceneInfo.brightness < 0.7f -> 0.6f // Normal scene
            else -> 0.3f // Bright scene (keep ambient low to avoid glare)
        }
    }
    
    private fun calculateOptimalColor(sceneInfo: SceneInfo): Int {
        // Extract and adjust dominant color for ambient lighting
        val originalColor = sceneInfo.dominantColor
        
        // Reduce saturation for comfortable ambient lighting
        val hsv = FloatArray(3)
        Color.colorToHSV(originalColor, hsv)
        
        // Reduce saturation by 40% and increase brightness slightly
        hsv[1] = (hsv[1] * 0.6f).coerceIn(0f, 1f) // Reduce saturation
        hsv[2] = (hsv[2] * 1.2f).coerceIn(0f, 1f) // Increase brightness
        
        return Color.HSVToColor(hsv)
    }
    
    private fun getMoodAdjustments(mood: SceneMood): MoodAdjustment {
        return when (mood) {
            SceneMood.ACTION -> MoodAdjustment(
                color = Color.RED,
                brightnessMultiplier = 1.2f,
                transitionSpeed = 150L // Faster transitions
            )
            SceneMood.ROMANTIC -> MoodAdjustment(
                color = Color.MAGENTA,
                brightnessMultiplier = 0.6f,
                transitionSpeed = 300L // Slower transitions
            )
            SceneMood.SUSPENSE -> MoodAdjustment(
                color = Color.BLUE,
                brightnessMultiplier = 0.4f,
                transitionSpeed = 200L
            )
            SceneMood.COMEDY -> MoodAdjustment(
                color = Color.YELLOW,
                brightnessMultiplier = 0.9f,
                transitionSpeed = 100L
            )
            SceneMood.DRAMA -> MoodAdjustment(
                color = Color.WHITE,
                brightnessMultiplier = 0.7f,
                transitionSpeed = 250L
            )
            SceneMood.NEUTRAL -> MoodAdjustment(
                color = Color.WHITE,
                brightnessMultiplier = 0.8f,
                transitionSpeed = 200L
            )
        }
    }
    
    private fun blendColors(color1: Int, color2: Int, ratio: Float = 0.3f): Int {
        val r1 = Color.red(color1)
        val g1 = Color.green(color1)
        val b1 = Color.blue(color1)
        
        val r2 = Color.red(color2)
        val g2 = Color.green(color2)
        val b2 = Color.blue(color2)
        
        val r = (r1 * (1 - ratio) + r2 * ratio).toInt().coerceIn(0, 255)
        val g = (g1 * (1 - ratio) + g2 * ratio).toInt().coerceIn(0, 255)
        val b = (b1 * (1 - ratio) + b2 * ratio).toInt().coerceIn(0, 255)
        
        return Color.rgb(r, g, b)
    }
    
    private suspend fun analyzeCurrentFrame(): SceneInfo? {
        // This would analyze the current video frame
        // For now, return mock data that simulates scene changes
        return generateMockSceneInfo()
    }
    
    private fun generateMockSceneInfo(): SceneInfo {
        // Generate realistic mock scene data
        val colors = listOf(Color.BLUE, Color.RED, Color.GREEN, Color.YELLOW, Color.MAGENTA)
        val moods = SceneMood.values()
        
        return SceneInfo(
            dominantColor = colors.random(),
            brightness = (0.2f..0.9f).random(),
            contrast = (0.3f..0.8f).random(),
            mood = moods.random()
        )
    }
    
    private suspend fun restoreLights() {
        currentLights.forEach { light ->
            try {
                ioTDeviceManager.controlLight(
                    light = light,
                    color = Color.WHITE,
                    brightness = 0.8f // Comfortable default brightness
                )
            } catch (e: Exception) {
                Log.e("AmbientLighting", "Error restoring light: ${light.name}", e)
            }
        }
        
        Log.d("AmbientLighting", "Restored ${currentLights.size} lights to default state")
    }
    
    fun toggleSync() {
        if (isSyncing) {
            CoroutineScope(Dispatchers.Default).launch { stopSync() }
        } else {
            CoroutineScope(Dispatchers.Default).launch { 
                startSync(MediaItem.EMPTY, currentLights) 
            }
        }
    }
    
    suspend fun setBrightness(level: Float) {
        val clampedLevel = level.coerceIn(0.1f, 1.0f)
        
        currentLights.forEach { light ->
            try {
                ioTDeviceManager.controlLight(
                    light = light,
                    brightness = clampedLevel
                )
            } catch (e: Exception) {
                Log.e("AmbientLighting", "Error setting brightness for light: ${light.name}", e)
            }
        }
        
        Log.d("AmbientLighting", "Set brightness to ${(clampedLevel * 100).toInt()}% for ${currentLights.size} lights")
    }
    
    suspend fun setColor(color: Int) {
        currentLights.forEach { light ->
            try {
                ioTDeviceManager.controlLight(
                    light = light,
                    color = color
                )
            } catch (e: Exception) {
                Log.e("AmbientLighting", "Error setting color for light: ${light.name}", e)
            }
        }
        
        Log.d("AmbientLighting", "Set color to #${Integer.toHexString(color)} for ${currentLights.size} lights")
    }
    
    fun isSyncActive(): Boolean = isSyncing
    
    fun getCurrentLights(): List<SmartLight> = currentLights.toList()
    
    suspend fun addLight(light: SmartLight) {
        if (!currentLights.contains(light)) {
            currentLights = currentLights + light
            Log.d("AmbientLighting", "Added light to sync: ${light.name}")
        }
    }
    
    suspend fun removeLight(lightId: String) {
        val originalSize = currentLights.size
        currentLights = currentLights.filter { it.id != lightId }
        
        if (currentLights.size < originalSize) {
            Log.d("AmbientLighting", "Removed light from sync: $lightId")
        }
    }
    
    fun cleanup() {
        try {
            if (isSyncing) {
                runBlocking { stopSync() }
            }
            
            frameAnalyzer = null
            currentLights = emptyList()
            
            Log.i("AmbientLighting", "Ambient lighting sync cleaned up")
        } catch (e: Exception) {
            Log.e("AmbientLighting", "Error during ambient lighting cleanup", e)
        }
    }
}

/**
 * Video Frame Analyzer for extracting scene information
 */
class VideoFrameAnalyzer {
    
    fun analyzeFrame(frame: Bitmap): SceneInfo {
        // Analyze video frame for dominant colors and mood
        return SceneInfo(
            dominantColor = extractDominantColor(frame),
            brightness = calculateBrightness(frame),
            contrast = calculateContrast(frame),
            mood = detectMood(frame)
        )
    }
    
    private fun extractDominantColor(frame: Bitmap): Int {
        // Extract dominant color from frame using color quantization
        val pixels = IntArray(frame.width * frame.height)
        frame.getPixels(pixels, 0, frame.width, 0, 0, frame.width, frame.height)
        
        // Simple dominant color extraction (in real implementation, use k-means clustering)
        val colorCounts = mutableMapOf<Int, Int>()
        
        pixels.forEach { pixel ->
            // Reduce color space for better clustering
            val reducedColor = reduceColorSpace(pixel)
            colorCounts[reducedColor] = (colorCounts[reducedColor] ?: 0) + 1
        }
        
        return colorCounts.maxByOrNull { it.value }?.key ?: Color.BLUE
    }
    
    private fun reduceColorSpace(color: Int): Int {
        // Reduce color space to 64 colors for better clustering
        val r = (Color.red(color) / 64) * 64
        val g = (Color.green(color) / 64) * 64
        val b = (Color.blue(color) / 64) * 64
        return Color.rgb(r, g, b)
    }
    
    private fun calculateBrightness(frame: Bitmap): Float {
        // Calculate average brightness using luminance formula
        val pixels = IntArray(frame.width * frame.height)
        frame.getPixels(pixels, 0, frame.width, 0, 0, frame.width, frame.height)
        
        var totalLuminance = 0.0
        pixels.forEach { pixel ->
            val r = Color.red(pixel)
            val g = Color.green(pixel)
            val b = Color.blue(pixel)
            
            // ITU-R BT.709 luminance formula
            val luminance = 0.2126 * r + 0.7152 * g + 0.0722 * b
            totalLuminance += luminance
        }
        
        return (totalLuminance / (pixels.size * 255)).toFloat()
    }
    
    private fun calculateContrast(frame: Bitmap): Float {
        // Calculate contrast using standard deviation of pixel values
        val pixels = IntArray(frame.width * frame.height)
        frame.getPixels(pixels, 0, frame.width, 0, 0, frame.width, frame.height)
        
        val luminances = pixels.map { pixel ->
            val r = Color.red(pixel)
            val g = Color.green(pixel)
            val b = Color.blue(pixel)
            0.2126 * r + 0.7152 * g + 0.0722 * b
        }
        
        val mean = luminances.average()
        val variance = luminances.map { (it - mean) * (it - mean) }.average()
        val standardDeviation = kotlin.math.sqrt(variance)
        
        return (standardDeviation / 255).toFloat()
    }
    
    private fun detectMood(frame: Bitmap): SceneMood {
        // Simple mood detection based on color analysis
        val dominantColor = extractDominantColor(frame)
        val brightness = calculateBrightness(frame)
        val contrast = calculateContrast(frame)
        
        return when {
            Color.red(dominantColor) > 150 && contrast > 0.5f -> SceneMood.ACTION
            brightness < 0.3f && contrast > 0.4f -> SceneMood.SUSPENSE
            Color.red(dominantColor) > 120 && Color.green(dominantColor) < 100 && brightness < 0.6f -> SceneMood.ROMANTIC
            brightness > 0.7f && Color.yellow(dominantColor) > 150 -> SceneMood.COMEDY
            brightness < 0.5f && contrast < 0.4f -> SceneMood.DRAMA
            else -> SceneMood.NEUTRAL
        }
    }
}

/**
 * Mood adjustment data class
 */
data class MoodAdjustment(
    val color: Int,
    val brightnessMultiplier: Float,
    val transitionSpeed: Long
)