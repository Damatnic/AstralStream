package com.astralplayer.nextplayer.feature.editor

import android.content.Context
import android.net.Uri
import android.util.Log
import com.arthenica.mobileffmpeg.Config
import com.arthenica.mobileffmpeg.FFmpeg
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * FFmpeg-based video processor for advanced effects
 */
class FFmpegVideoProcessor(private val context: Context) {
    
    companion object {
        private const val TAG = "FFmpegVideoProcessor"
    }
    
    /**
     * Apply video effect using FFmpeg
     */
    suspend fun applyVideoEffect(
        inputUri: Uri,
        outputPath: String,
        effect: VideoEffect,
        onProgress: (Float) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val inputPath = getPathFromUri(inputUri)
            val outputFile = File(outputPath)
            
            // Build FFmpeg command based on effect type
            val command = when (effect.type) {
                EffectType.FADE_IN -> buildFadeInCommand(inputPath, outputPath, effect)
                EffectType.FADE_OUT -> buildFadeOutCommand(inputPath, outputPath, effect)
                EffectType.BLUR -> buildBlurCommand(inputPath, outputPath, effect)
                EffectType.BRIGHTNESS -> buildBrightnessCommand(inputPath, outputPath, effect)
                EffectType.CONTRAST -> buildContrastCommand(inputPath, outputPath, effect)
                EffectType.SATURATION -> buildSaturationCommand(inputPath, outputPath, effect)
                EffectType.SEPIA -> buildSepiaCommand(inputPath, outputPath)
                EffectType.GRAYSCALE -> buildGrayscaleCommand(inputPath, outputPath)
                EffectType.VIGNETTE -> buildVignetteCommand(inputPath, outputPath, effect)
            }
            
            // Set up progress callback
            Config.enableStatisticsCallback { stats ->
                val duration = effect.duration
                if (duration > 0) {
                    val progress = (stats.time.toFloat() / duration).coerceIn(0f, 1f)
                    onProgress(progress)
                }
            }
            
            // Execute FFmpeg command
            val rc = FFmpeg.execute(command)
            
            if (rc == Config.RETURN_CODE_SUCCESS) {
                onProgress(1.0f)
                Result.success(outputFile)
            } else {
                Result.failure(Exception("FFmpeg execution failed with rc=$rc"))
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error applying video effect", e)
            Result.failure(e)
        }
    }
    
    /**
     * Apply multiple effects in sequence
     */
    suspend fun applyMultipleEffects(
        inputUri: Uri,
        outputPath: String,
        effects: List<VideoEffect>,
        onProgress: (Float) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val inputPath = getPathFromUri(inputUri)
            val outputFile = File(outputPath)
            
            // Build complex filter graph for multiple effects
            val filterComplex = buildFilterComplex(effects)
            
            val command = "-i $inputPath -filter_complex \"$filterComplex\" -c:a copy $outputPath"
            
            // Execute FFmpeg command
            val rc = FFmpeg.execute(command)
            
            if (rc == Config.RETURN_CODE_SUCCESS) {
                onProgress(1.0f)
                Result.success(outputFile)
            } else {
                Result.failure(Exception("FFmpeg execution failed with rc=$rc"))
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error applying multiple effects", e)
            Result.failure(e)
        }
    }
    
    private fun buildFadeInCommand(input: String, output: String, effect: VideoEffect): String {
        val duration = effect.parameters["fadeDuration"] as? Float ?: 1.0f
        return "-i $input -vf fade=t=in:st=0:d=$duration -c:a copy $output"
    }
    
    private fun buildFadeOutCommand(input: String, output: String, effect: VideoEffect): String {
        val duration = effect.parameters["fadeDuration"] as? Float ?: 1.0f
        val startTime = (effect.startTime / 1000000.0f) // Convert to seconds
        return "-i $input -vf fade=t=out:st=$startTime:d=$duration -c:a copy $output"
    }
    
    private fun buildBlurCommand(input: String, output: String, effect: VideoEffect): String {
        val radius = (effect.intensity * 10).toInt().coerceIn(1, 20)
        return "-i $input -vf boxblur=$radius:$radius -c:a copy $output"
    }
    
    private fun buildBrightnessCommand(input: String, output: String, effect: VideoEffect): String {
        val brightness = effect.intensity.coerceIn(-1f, 1f)
        return "-i $input -vf eq=brightness=$brightness -c:a copy $output"
    }
    
    private fun buildContrastCommand(input: String, output: String, effect: VideoEffect): String {
        val contrast = effect.intensity.coerceIn(0f, 2f)
        return "-i $input -vf eq=contrast=$contrast -c:a copy $output"
    }
    
    private fun buildSaturationCommand(input: String, output: String, effect: VideoEffect): String {
        val saturation = effect.intensity.coerceIn(0f, 3f)
        return "-i $input -vf eq=saturation=$saturation -c:a copy $output"
    }
    
    private fun buildSepiaCommand(input: String, output: String): String {
        return "-i $input -vf colorchannelmixer=.393:.769:.189:0:.349:.686:.168:0:.272:.534:.131 -c:a copy $output"
    }
    
    private fun buildGrayscaleCommand(input: String, output: String): String {
        return "-i $input -vf format=gray -c:a copy $output"
    }
    
    private fun buildVignetteCommand(input: String, output: String, effect: VideoEffect): String {
        val intensity = effect.intensity.coerceIn(0f, 1f)
        return "-i $input -vf vignette=PI*$intensity -c:a copy $output"
    }
    
    private fun buildFilterComplex(effects: List<VideoEffect>): String {
        val filters = mutableListOf<String>()
        
        effects.forEach { effect ->
            when (effect.type) {
                EffectType.BLUR -> {
                    val radius = (effect.intensity * 10).toInt().coerceIn(1, 20)
                    filters.add("boxblur=$radius:$radius")
                }
                EffectType.BRIGHTNESS -> {
                    val brightness = effect.intensity.coerceIn(-1f, 1f)
                    filters.add("eq=brightness=$brightness")
                }
                EffectType.CONTRAST -> {
                    val contrast = effect.intensity.coerceIn(0f, 2f)
                    filters.add("eq=contrast=$contrast")
                }
                EffectType.SATURATION -> {
                    val saturation = effect.intensity.coerceIn(0f, 3f)
                    filters.add("eq=saturation=$saturation")
                }
                EffectType.GRAYSCALE -> {
                    filters.add("format=gray")
                }
                else -> {
                    // Skip unsupported effects in filter complex
                }
            }
        }
        
        return filters.joinToString(",")
    }
    
    private fun getPathFromUri(uri: Uri): String {
        // For content:// URIs, we need to copy to a temporary file
        if (uri.scheme == "content") {
            val tempFile = File(context.cacheDir, "temp_video_${System.currentTimeMillis()}.mp4")
            context.contentResolver.openInputStream(uri)?.use { input ->
                tempFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            return tempFile.absolutePath
        }
        
        return uri.path ?: throw IllegalArgumentException("Invalid URI")
    }
    
    /**
     * Add text overlay to video
     */
    suspend fun addTextOverlay(
        inputUri: Uri,
        outputPath: String,
        text: String,
        position: TextPosition,
        fontSize: Int = 24,
        fontColor: String = "white",
        onProgress: (Float) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val inputPath = getPathFromUri(inputUri)
            val outputFile = File(outputPath)
            
            val positionString = when (position) {
                TextPosition.TOP_LEFT -> "x=10:y=10"
                TextPosition.TOP_CENTER -> "x=(w-text_w)/2:y=10"
                TextPosition.TOP_RIGHT -> "x=w-text_w-10:y=10"
                TextPosition.CENTER -> "x=(w-text_w)/2:y=(h-text_h)/2"
                TextPosition.BOTTOM_LEFT -> "x=10:y=h-text_h-10"
                TextPosition.BOTTOM_CENTER -> "x=(w-text_w)/2:y=h-text_h-10"
                TextPosition.BOTTOM_RIGHT -> "x=w-text_w-10:y=h-text_h-10"
            }
            
            val command = "-i $inputPath -vf drawtext=text='$text':$positionString:fontsize=$fontSize:fontcolor=$fontColor -c:a copy $outputPath"
            
            val rc = FFmpeg.execute(command)
            
            if (rc == Config.RETURN_CODE_SUCCESS) {
                onProgress(1.0f)
                Result.success(outputFile)
            } else {
                Result.failure(Exception("FFmpeg execution failed with rc=$rc"))
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error adding text overlay", e)
            Result.failure(e)
        }
    }
    
    /**
     * Extract audio from video
     */
    suspend fun extractAudio(
        inputUri: Uri,
        outputPath: String,
        format: AudioFormat = AudioFormat.MP3,
        onProgress: (Float) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val inputPath = getPathFromUri(inputUri)
            val outputFile = File(outputPath)
            
            val codec = when (format) {
                AudioFormat.MP3 -> "libmp3lame"
                AudioFormat.AAC -> "aac"
                AudioFormat.WAV -> "pcm_s16le"
            }
            
            val command = "-i $inputPath -vn -acodec $codec $outputPath"
            
            val rc = FFmpeg.execute(command)
            
            if (rc == Config.RETURN_CODE_SUCCESS) {
                onProgress(1.0f)
                Result.success(outputFile)
            } else {
                Result.failure(Exception("FFmpeg execution failed with rc=$rc"))
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting audio", e)
            Result.failure(e)
        }
    }
}

enum class TextPosition {
    TOP_LEFT, TOP_CENTER, TOP_RIGHT,
    CENTER,
    BOTTOM_LEFT, BOTTOM_CENTER, BOTTOM_RIGHT
}

enum class AudioFormat {
    MP3, AAC, WAV
}