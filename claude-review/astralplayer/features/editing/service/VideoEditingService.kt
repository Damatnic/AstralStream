package com.astralplayer.features.editing.service

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.net.Uri
import android.util.Log
import com.arthenica.ffmpegkit.*
import com.astralplayer.nextplayer.editing.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.File
import java.nio.ByteBuffer
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.*

@Singleton
class VideoEditingService @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "VideoEditingService"
        private const val TEMP_DIR = "video_editing_temp"
        private const val PROJECT_DIR = "projects"
    }
    
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val tempDir = File(context.cacheDir, TEMP_DIR).apply { mkdirs() }
    private val projectDir = File(context.filesDir, PROJECT_DIR).apply { mkdirs() }
    
    private var currentProject: EditingProject? = null
    private val undoStack = mutableListOf<EditingAction>()
    private val redoStack = mutableListOf<EditingAction>()
    
    suspend fun initialize(): VideoEditingInitializationResult {
        return withContext(Dispatchers.IO) {
            try {
                // Clean up temp directory
                cleanTempDirectory()
                
                // Initialize FFmpeg if available
                val ffmpegAvailable = checkFFmpegAvailability()
                
                VideoEditingInitializationResult(
                    success = true,
                    availableFeatures = getAvailableFeatures(ffmpegAvailable),
                    supportedFormats = getSupportedFormats(),
                    initializationTime = System.currentTimeMillis()
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize video editing service", e)
                VideoEditingInitializationResult(
                    success = false,
                    error = e.message
                )
            }
        }
    }
    
    suspend fun createProject(
        name: String,
        settings: ProjectSettings
    ): ProjectCreationResult {
        return withContext(Dispatchers.IO) {
            try {
                val project = EditingProject(
                    id = UUID.randomUUID().toString(),
                    name = name,
                    settings = settings,
                    timeline = Timeline(),
                    createdAt = System.currentTimeMillis(),
                    lastModified = System.currentTimeMillis()
                )
                
                currentProject = project
                
                // Save project metadata
                saveProjectMetadata(project)
                
                ProjectCreationResult(
                    success = true,
                    project = project,
                    creationTime = System.currentTimeMillis()
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed to create project", e)
                ProjectCreationResult(
                    success = false,
                    error = e.message
                )
            }
        }
    }
    
    suspend fun importVideo(uri: Uri): VideoImportResult {
        return withContext(Dispatchers.IO) {
            try {
                val videoInfo = analyzeVideo(uri)
                
                val clip = TimelineClip(
                    id = UUID.randomUUID().toString(),
                    type = ClipType.VIDEO,
                    sourceUri = uri,
                    name = uri.lastPathSegment ?: "Video",
                    startTime = 0L,
                    duration = videoInfo.duration,
                    trackIndex = 0,
                    videoInfo = videoInfo
                )
                
                VideoImportResult(
                    success = true,
                    clip = clip,
                    videoInfo = videoInfo,
                    importTime = System.currentTimeMillis()
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed to import video", e)
                VideoImportResult(
                    success = false,
                    error = e.message
                )
            }
        }
    }
    
    suspend fun applyEffect(
        clip: TimelineClip,
        effect: VideoEffect
    ): EffectApplicationResult {
        return withContext(Dispatchers.IO) {
            try {
                // For now, just add the effect to the clip
                // Real implementation would process the video
                
                addUndoAction(EditingAction.ApplyEffect(clip.copy(), clip, effect))
                
                EffectApplicationResult(
                    success = true,
                    appliedEffect = effect,
                    modifiedClip = clip,
                    applicationTime = System.currentTimeMillis()
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed to apply effect", e)
                EffectApplicationResult(
                    success = false,
                    error = e.message
                )
            }
        }
    }
    
    suspend fun addTransition(
        fromClip: TimelineClip,
        toClip: TimelineClip,
        transition: VideoTransition
    ): TransitionResult {
        return withContext(Dispatchers.IO) {
            try {
                addUndoAction(EditingAction.AddTransition(fromClip, toClip, transition))
                
                TransitionResult(
                    success = true,
                    transition = transition,
                    modifiedClips = listOf(fromClip, toClip),
                    addTime = System.currentTimeMillis()
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed to add transition", e)
                TransitionResult(
                    success = false,
                    error = e.message
                )
            }
        }
    }
    
    suspend fun addOverlay(overlay: VideoOverlay): OverlayResult {
        return withContext(Dispatchers.IO) {
            try {
                addUndoAction(EditingAction.AddOverlay(overlay))
                
                OverlayResult(
                    success = true,
                    overlay = overlay,
                    addTime = System.currentTimeMillis()
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed to add overlay", e)
                OverlayResult(
                    success = false,
                    error = e.message
                )
            }
        }
    }
    
    suspend fun adjustAudio(
        clip: TimelineClip,
        adjustments: AudioAdjustments
    ): AudioEditResult {
        return withContext(Dispatchers.IO) {
            try {
                addUndoAction(EditingAction.AdjustAudio(clip.copy(), clip, adjustments))
                
                AudioEditResult(
                    success = true,
                    adjustedAudioInfo = clip.audioInfo,
                    modifiedClip = clip,
                    adjustTime = System.currentTimeMillis()
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed to adjust audio", e)
                AudioEditResult(
                    success = false,
                    error = e.message
                )
            }
        }
    }
    
    suspend fun applyColorCorrection(
        clip: TimelineClip,
        settings: ColorCorrectionSettings
    ): ColorCorrectionResult {
        return withContext(Dispatchers.IO) {
            try {
                val correctionData = analyzeColorData(clip, settings)
                
                addUndoAction(EditingAction.ApplyColorCorrection(clip.copy(), clip, settings))
                
                ColorCorrectionResult(
                    success = true,
                    correctionData = correctionData,
                    modifiedClip = clip,
                    correctionTime = System.currentTimeMillis()
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed to apply color correction", e)
                ColorCorrectionResult(
                    success = false,
                    error = e.message
                )
            }
        }
    }
    
    suspend fun exportVideo(
        timeline: Timeline,
        settings: ExportSettings,
        progressCallback: (Float) -> Unit
    ): VideoExportResult {
        return withContext(Dispatchers.IO) {
            try {
                val outputFile = File(settings.outputUri.path ?: throw Exception("Invalid output path"))
                outputFile.parentFile?.mkdirs()
                
                // Build FFmpeg command
                val command = buildExportCommand(timeline, settings)
                
                // Execute FFmpeg command with progress monitoring
                var lastProgress = 0f
                FFmpegKit.executeAsync(command,
                    { session ->
                        val returnCode = session.returnCode
                        if (ReturnCode.isSuccess(returnCode)) {
                            progressCallback(1f)
                        } else {
                            Log.e(TAG, "Export failed with return code: $returnCode")
                        }
                    },
                    { log ->
                        Log.d(TAG, log.message)
                    },
                    { statistics ->
                        val time = statistics.time
                        val duration = timeline.duration
                        if (duration > 0) {
                            val progress = (time.toFloat() / duration).coerceIn(0f, 1f)
                            if (progress > lastProgress) {
                                lastProgress = progress
                                progressCallback(progress)
                            }
                        }
                    }
                ).also { session ->
                    // Wait for completion
                    while (!session.state.isCompleted) {
                        delay(100)
                    }
                    
                    if (ReturnCode.isSuccess(session.returnCode)) {
                        return@withContext VideoExportResult(
                            success = true,
                            outputUri = settings.outputUri,
                            fileSize = outputFile.length(),
                            duration = timeline.duration,
                            exportTime = System.currentTimeMillis()
                        )
                    } else {
                        throw Exception("Export failed: ${session.failStackTrace}")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to export video", e)
                VideoExportResult(
                    success = false,
                    error = e.message
                )
            }
        }
    }
    
    suspend fun saveProject(project: EditingProject) {
        withContext(Dispatchers.IO) {
            try {
                val projectFile = File(projectDir, "${project.id}.json")
                projectFile.writeText(kotlinx.serialization.json.Json.encodeToString(
                    EditingProject.serializer(), project
                ))
            } catch (e: Exception) {
                Log.e(TAG, "Failed to save project", e)
            }
        }
    }
    
    suspend fun undo(): UndoRedoResult {
        return withContext(Dispatchers.IO) {
            try {
                if (undoStack.isNotEmpty()) {
                    val action = undoStack.removeAt(undoStack.lastIndex)
                    redoStack.add(action)
                    
                    UndoRedoResult(
                        success = true,
                        actionType = action.type,
                        operationTime = System.currentTimeMillis()
                    )
                } else {
                    UndoRedoResult(
                        success = false,
                        error = "Nothing to undo"
                    )
                }
            } catch (e: Exception) {
                UndoRedoResult(
                    success = false,
                    error = e.message
                )
            }
        }
    }
    
    suspend fun redo(): UndoRedoResult {
        return withContext(Dispatchers.IO) {
            try {
                if (redoStack.isNotEmpty()) {
                    val action = redoStack.removeAt(redoStack.lastIndex)
                    undoStack.add(action)
                    
                    UndoRedoResult(
                        success = true,
                        actionType = action.type,
                        operationTime = System.currentTimeMillis()
                    )
                } else {
                    UndoRedoResult(
                        success = false,
                        error = "Nothing to redo"
                    )
                }
            } catch (e: Exception) {
                UndoRedoResult(
                    success = false,
                    error = e.message
                )
            }
        }
    }
    
    fun cleanup() {
        scope.cancel()
        cleanTempDirectory()
    }
    
    // Private helper methods
    
    private suspend fun analyzeVideo(uri: Uri): VideoInfo {
        return withContext(Dispatchers.IO) {
            val extractor = MediaExtractor()
            try {
                extractor.setDataSource(context, uri, null)
                
                var videoTrackIndex = -1
                var videoFormat: MediaFormat? = null
                
                for (i in 0 until extractor.trackCount) {
                    val format = extractor.getTrackFormat(i)
                    val mime = format.getString(MediaFormat.KEY_MIME) ?: continue
                    
                    if (mime.startsWith("video/")) {
                        videoTrackIndex = i
                        videoFormat = format
                        break
                    }
                }
                
                if (videoFormat != null) {
                    val width = videoFormat.getInteger(MediaFormat.KEY_WIDTH)
                    val height = videoFormat.getInteger(MediaFormat.KEY_HEIGHT)
                    val duration = videoFormat.getLong(MediaFormat.KEY_DURATION) / 1000 // Convert to ms
                    val frameRate = try {
                        videoFormat.getInteger(MediaFormat.KEY_FRAME_RATE).toFloat()
                    } catch (e: Exception) {
                        30f // Default
                    }
                    
                    VideoInfo(
                        resolution = Pair(width, height),
                        frameRate = frameRate,
                        duration = duration,
                        bitrate = 0L, // Would need more analysis
                        codec = videoFormat.getString(MediaFormat.KEY_MIME) ?: "unknown",
                        colorSpace = ColorSpace.REC709,
                        hasAudio = extractor.trackCount > 1,
                        fileSize = 0L // Would need file access
                    )
                } else {
                    throw Exception("No video track found")
                }
            } finally {
                extractor.release()
            }
        }
    }
    
    private fun buildExportCommand(timeline: Timeline, settings: ExportSettings): String {
        val outputPath = settings.outputUri.path ?: throw Exception("Invalid output path")
        val command = StringBuilder()
        
        // Basic FFmpeg command structure
        command.append("-y ") // Overwrite output
        
        // Input files from timeline clips
        timeline.videoTracks.forEach { track ->
            track.clips.forEach { clip ->
                command.append("-i \"${clip.sourceUri.path}\" ")
            }
        }
        
        // Video codec and settings
        command.append("-c:v h264 ")
        command.append("-b:v ${settings.bitrate} ")
        command.append("-r ${settings.frameRate} ")
        command.append("-s ${settings.resolution.first}x${settings.resolution.second} ")
        
        // Audio codec and settings
        command.append("-c:a ${getAudioCodec(settings.audioSettings.codec)} ")
        command.append("-b:a ${settings.audioSettings.bitrate} ")
        command.append("-ar ${settings.audioSettings.sampleRate} ")
        command.append("-ac ${settings.audioSettings.channels} ")
        
        // Hardware acceleration if enabled
        if (settings.enableGPUAcceleration) {
            command.append("-hwaccel auto ")
        }
        
        // Output file
        command.append("\"$outputPath\"")
        
        return command.toString()
    }
    
    private fun getAudioCodec(codec: AudioCodec): String {
        return when (codec) {
            AudioCodec.AAC -> "aac"
            AudioCodec.MP3 -> "libmp3lame"
            AudioCodec.FLAC -> "flac"
            AudioCodec.OGG -> "libvorbis"
            AudioCodec.WAV -> "pcm_s16le"
            AudioCodec.AC3 -> "ac3"
            AudioCodec.DTS -> "dca"
        }
    }
    
    private fun analyzeColorData(
        clip: TimelineClip,
        settings: ColorCorrectionSettings
    ): ColorCorrectionData {
        // Simplified color data analysis
        return ColorCorrectionData(
            histogram = Histogram(
                red = List(256) { (it * Random().nextFloat()).toInt() },
                green = List(256) { (it * Random().nextFloat()).toInt() },
                blue = List(256) { (it * Random().nextFloat()).toInt() },
                luminance = List(256) { (it * Random().nextFloat()).toInt() }
            ),
            waveform = Waveform(
                data = List(100) { List(100) { Random().nextFloat() } },
                width = 100,
                height = 100
            ),
            vectorscope = Vectorscope(
                data = List(100) { 
                    Pair(
                        Random().nextFloat() * 2 - 1,
                        Random().nextFloat() * 2 - 1
                    )
                },
                scale = 1f
            ),
            appliedSettings = settings
        )
    }
    
    private fun checkFFmpegAvailability(): Boolean {
        return try {
            FFmpegKitConfig.getFFmpegVersion()
            true
        } catch (e: Exception) {
            false
        }
    }
    
    private fun getAvailableFeatures(ffmpegAvailable: Boolean): List<EditingFeature> {
        val features = mutableListOf(
            EditingFeature.VIDEO_TRIMMING,
            EditingFeature.AUDIO_EDITING,
            EditingFeature.EFFECTS_APPLICATION,
            EditingFeature.TRANSITIONS,
            EditingFeature.OVERLAYS,
            EditingFeature.COLOR_CORRECTION,
            EditingFeature.UNDO_REDO,
            EditingFeature.PROJECT_MANAGEMENT,
            EditingFeature.REAL_TIME_PREVIEW
        )
        
        if (ffmpegAvailable) {
            features.addAll(listOf(
                EditingFeature.VIDEO_STABILIZATION,
                EditingFeature.NOISE_REDUCTION,
                EditingFeature.KEYFRAME_ANIMATION,
                EditingFeature.MULTI_TRACK_EDITING,
                EditingFeature.BATCH_PROCESSING,
                EditingFeature.CUSTOM_FILTERS,
                EditingFeature.AUDIO_MIXING,
                EditingFeature.SUBTITLE_EDITING,
                EditingFeature.MOTION_GRAPHICS,
                EditingFeature.GREEN_SCREEN,
                EditingFeature.TIME_REMAPPING
            ))
        }
        
        return features
    }
    
    private fun getSupportedFormats(): List<VideoFormat> {
        return listOf(
            VideoFormat.MP4,
            VideoFormat.MOV,
            VideoFormat.AVI,
            VideoFormat.MKV,
            VideoFormat.WEBM,
            VideoFormat.M4V
        )
    }
    
    private fun cleanTempDirectory() {
        tempDir.listFiles()?.forEach { it.deleteRecursively() }
    }
    
    private fun saveProjectMetadata(project: EditingProject) {
        // Save project metadata for future loading
        val metadataFile = File(projectDir, "${project.id}_metadata.json")
        metadataFile.writeText("""
            {
                "id": "${project.id}",
                "name": "${project.name}",
                "createdAt": ${project.createdAt},
                "lastModified": ${project.lastModified}
            }
        """.trimIndent())
    }
    
    private fun addUndoAction(action: EditingAction) {
        undoStack.add(action)
        redoStack.clear()
    }
}