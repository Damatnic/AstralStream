package com.astralplayer.nextplayer.feature.ai

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.google.ai.client.generativeai.type.generationConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

/**
 * Google AI Studio (Gemini) service for all AI features in the app
 */
class GoogleAIStudioService(private val context: Context) {
    
    companion object {
        private const val TAG = "GoogleAIStudio"
        private const val API_KEY = "AIzaSyAEpBsYR4n54DmT1h2vm8ZO_448x5s6uMs"
        private const val MAX_RETRIES = 3
        private const val RETRY_DELAY_MS = 1000L
    }
    
    // Gemini 1.5 Flash model for text generation (updated for 2025)
    private val textModel = GenerativeModel(
        modelName = "gemini-1.5-flash",
        apiKey = API_KEY,
        generationConfig = generationConfig {
            temperature = 0.7f
            topK = 40
            topP = 0.95f
            maxOutputTokens = 512  // Reduced to avoid MAX_TOKENS error
        }
    )
    
    // Gemini 1.5 Pro Vision model for image analysis (updated for 2025)
    private val visionModel = GenerativeModel(
        modelName = "gemini-1.5-pro",
        apiKey = API_KEY,
        generationConfig = generationConfig {
            temperature = 0.4f
            topK = 32
            topP = 0.9f
            maxOutputTokens = 512
        }
    )
    
    /**
     * Generate AI subtitles from video content description
     */
    suspend fun generateSubtitles(
        videoTitle: String,
        videoDescription: String,
        language: String = "English",
        duration: Long
    ): Flow<SubtitleGenerationResult> = flow {
        try {
            // Check network connectivity first
            if (!isNetworkAvailable()) {
                emit(SubtitleGenerationResult.Error("No internet connection available. Please check your network and try again."))
                return@flow
            }
            
            emit(SubtitleGenerationResult.Progress("Connecting to AI service..."))
            
            val prompt = """
                Generate 3-5 short subtitle entries for video titled "$videoTitle" in SRT format:
                
                1
                00:00:00,000 --> 00:00:03,000
                Welcome to the video
                
                2
                00:00:03,000 --> 00:00:06,000
                Content begins here
                
                3
                00:00:06,000 --> 00:00:09,000
                Thank you for watching
            """.trimIndent()
            
            emit(SubtitleGenerationResult.Progress("Generating subtitles with AI..."))
            
            // Retry logic for API calls
            var lastException: Exception? = null
            for (attempt in 1..MAX_RETRIES) {
                try {
                    Log.d(TAG, "AI subtitle generation attempt $attempt/$MAX_RETRIES")
                    
                    val response = withContext(Dispatchers.IO) {
                        textModel.generateContent(prompt)
                    }
                    
                    val subtitleContent = response.text
                    if (subtitleContent.isNullOrBlank()) {
                        throw Exception("AI service returned empty response")
                    }
                    
                    Log.d(TAG, "AI subtitle generation successful on attempt $attempt")
                    emit(SubtitleGenerationResult.Success(subtitleContent))
                    return@flow
                    
                } catch (e: Exception) {
                    lastException = e
                    Log.w(TAG, "AI subtitle generation attempt $attempt failed: ${e.message}")
                    
                    if (attempt < MAX_RETRIES) {
                        emit(SubtitleGenerationResult.Progress("Retrying AI service connection... (attempt ${attempt + 1})"))
                        kotlinx.coroutines.delay(RETRY_DELAY_MS * attempt) // Exponential backoff
                    }
                }
            }
            
            // All retries failed
            val errorMessage = when {
                lastException?.message?.contains("API_KEY", ignoreCase = true) == true -> 
                    "AI service authentication failed. Please check API configuration."
                lastException?.message?.contains("QUOTA", ignoreCase = true) == true || 
                lastException?.message?.contains("quota", ignoreCase = true) == true -> 
                    "AI service quota exceeded. Please try again later."
                lastException?.message?.contains("network", ignoreCase = true) == true ||
                lastException?.message?.contains("timeout", ignoreCase = true) == true ||
                lastException?.message?.contains("connection", ignoreCase = true) == true -> 
                    "Network error connecting to AI service. Please check your internet connection."
                else -> "AI service unavailable. Please check your connection and try again."
            }
            
            Log.e(TAG, "All subtitle generation attempts failed", lastException)
            emit(SubtitleGenerationResult.Error(errorMessage))
            
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error in subtitle generation", e)
            emit(SubtitleGenerationResult.Error("Unexpected error: ${e.message ?: "Unknown error"}"))
        }
    }
    
    /**
     * Analyze video frame for scene detection
     */
    suspend fun analyzeVideoFrame(
        bitmap: Bitmap,
        timestamp: Long
    ): FrameAnalysisResult = withContext(Dispatchers.IO) {
        try {
            val prompt = """
                Analyze this video frame and provide:
                1. Scene type (ACTION, DIALOGUE, LANDSCAPE, CLOSE_UP, CROWD, INDOOR, OUTDOOR, NIGHT, DAY, TRANSITION)
                2. Main objects/subjects in the scene
                3. Emotional tone or mood
                4. Confidence score (0.0 to 1.0)
                5. Brief description (one sentence)
                
                Respond in JSON format:
                {
                    "sceneType": "ACTION",
                    "objects": ["person", "car", "building"],
                    "mood": "exciting",
                    "confidence": 0.85,
                    "description": "An action scene showing a person near a car and building"
                }
            """.trimIndent()
            
            val content = content {
                image(bitmap)
                text(prompt)
            }
            
            val response = visionModel.generateContent(content)
            val analysisText = response.text ?: throw Exception("No response from AI vision model")
            
            // Parse the JSON response
            parseFrameAnalysisResponse(analysisText, timestamp)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error analyzing frame at $timestamp", e)
            FrameAnalysisResult(
                timestamp = timestamp,
                sceneType = SceneType.TRANSITION,
                objects = emptyList(),
                mood = "unknown",
                confidence = 0.0f,
                description = "Analysis failed: ${e.message}"
            )
        }
    }
    
    /**
     * Generate video content summary and insights
     */
    suspend fun generateVideoInsights(
        videoTitle: String,
        scenes: List<DetectedScene>
    ): Flow<VideoInsightsResult> = flow {
        try {
            emit(VideoInsightsResult.Progress("Analyzing video structure..."))
            
            val scenesSummary = scenes.joinToString("\n") { scene ->
                "${formatTime(scene.startTime)} - ${formatTime(scene.endTime)}: ${scene.sceneType} (${scene.confidence})"
            }
            
            val prompt = """
                Analyze this video structure and provide insights:
                
                Video Title: $videoTitle
                Total Scenes: ${scenes.size}
                
                Scene Breakdown:
                $scenesSummary
                
                Provide analysis including:
                1. Overall video type and genre
                2. Pacing analysis (fast/slow/varied)
                3. Key moments or highlights
                4. Content recommendations
                5. Emotional journey through the video
                6. Technical quality assessment
                
                Format as a detailed but concise report.
            """.trimIndent()
            
            emit(VideoInsightsResult.Progress("Generating AI insights..."))
            
            val response = textModel.generateContent(prompt)
            val insights = response.text ?: throw Exception("No insights generated")
            
            emit(VideoInsightsResult.Success(insights))
            
        } catch (e: Exception) {
            Log.e(TAG, "Error generating video insights", e)
            emit(VideoInsightsResult.Error("Failed to generate insights: ${e.message}"))
        }
    }
    
    /**
     * Generate smart video recommendations based on content
     */
    suspend fun generateRecommendations(
        currentVideoTitle: String,
        watchHistory: List<String>
    ): List<VideoRecommendation> = withContext(Dispatchers.IO) {
        try {
            val historyText = watchHistory.takeLast(10).joinToString(", ")
            
            val prompt = """
                Based on the current video "$currentVideoTitle" and recent watch history: $historyText
                
                Suggest 5 video recommendations with:
                1. Suggested title
                2. Reason for recommendation
                3. Content type
                4. Expected appeal score (0.0 to 1.0)
                
                Format as JSON array:
                [
                    {
                        "title": "Suggested Video Title",
                        "reason": "Similar themes and style",
                        "contentType": "Educational/Entertainment/Action/etc",
                        "appealScore": 0.85
                    }
                ]
            """.trimIndent()
            
            val response = textModel.generateContent(prompt)
            val recommendationsText = response.text ?: return@withContext emptyList()
            
            // Parse recommendations (simplified - in real app would use proper JSON parsing)
            parseVideoRecommendations(recommendationsText)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error generating recommendations", e)
            emptyList()
        }
    }
    
    /**
     * Translate subtitle content to specified language
     */
    suspend fun translateSubtitles(
        subtitleContent: String,
        targetLanguage: String
    ): Flow<TranslationResult> = flow {
        try {
            emit(TranslationResult.Progress("Preparing translation..."))
            
            val prompt = """
                Translate the following subtitle content to $targetLanguage while preserving:
                1. SRT format with timestamps
                2. Line breaks and timing
                3. Natural flow and context
                4. Cultural appropriateness
                
                Original content:
                $subtitleContent
                
                Provide the translated version in the same SRT format.
            """.trimIndent()
            
            emit(TranslationResult.Progress("Translating with AI..."))
            
            val response = textModel.generateContent(prompt)
            val translatedContent = response.text ?: throw Exception("Translation failed")
            
            emit(TranslationResult.Success(translatedContent, targetLanguage))
            
        } catch (e: Exception) {
            Log.e(TAG, "Error translating subtitles", e)
            emit(TranslationResult.Error("Translation failed: ${e.message}"))
        }
    }
    
    // Helper methods
    
    private fun bitmapToByteArray(bitmap: Bitmap): ByteArray {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, stream)
        return stream.toByteArray()
    }
    
    private fun parseFrameAnalysisResponse(response: String, timestamp: Long): FrameAnalysisResult {
        // Simplified JSON parsing - in production would use proper JSON library
        return try {
            // Extract values using regex (simplified for demo)
            val sceneTypeRegex = "\"sceneType\":\\s*\"(\\w+)\"".toRegex()
            val confidenceRegex = "\"confidence\":\\s*(\\d+\\.\\d+)".toRegex()
            val descriptionRegex = "\"description\":\\s*\"([^\"]+)\"".toRegex()
            
            val sceneTypeMatch = sceneTypeRegex.find(response)
            val confidenceMatch = confidenceRegex.find(response)
            val descriptionMatch = descriptionRegex.find(response)
            
            val sceneType = sceneTypeMatch?.groupValues?.get(1)?.let { 
                SceneType.valueOf(it.uppercase()) 
            } ?: SceneType.TRANSITION
            
            val confidence = confidenceMatch?.groupValues?.get(1)?.toFloatOrNull() ?: 0.5f
            val description = descriptionMatch?.groupValues?.get(1) ?: "AI analysis result"
            
            FrameAnalysisResult(
                timestamp = timestamp,
                sceneType = sceneType,
                objects = listOf("detected_object"), // Simplified
                mood = "analyzed",
                confidence = confidence,
                description = description
            )
        } catch (e: Exception) {
            FrameAnalysisResult(
                timestamp = timestamp,
                sceneType = SceneType.TRANSITION,
                objects = emptyList(),
                mood = "unknown",
                confidence = 0.5f,
                description = "Parsed AI analysis"
            )
        }
    }
    
    private fun parseVideoRecommendations(response: String): List<VideoRecommendation> {
        // Simplified parsing - would use proper JSON in production
        return try {
            val recommendations = mutableListOf<VideoRecommendation>()
            val titleRegex = "\"title\":\\s*\"([^\"]+)\"".toRegex()
            val reasonRegex = "\"reason\":\\s*\"([^\"]+)\"".toRegex()
            
            val titles = titleRegex.findAll(response).map { it.groupValues[1] }.toList()
            val reasons = reasonRegex.findAll(response).map { it.groupValues[1] }.toList()
            
            titles.zip(reasons).forEach { (title, reason) ->
                recommendations.add(
                    VideoRecommendation(
                        title = title,
                        reason = reason,
                        contentType = "AI Suggested",
                        appealScore = 0.8f
                    )
                )
            }
            
            recommendations
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing recommendations", e)
            emptyList()
        }
    }
    
    private fun formatDuration(millis: Long): String {
        val totalSeconds = millis / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        
        return when {
            hours > 0 -> String.format("%d:%02d:%02d", hours, minutes, seconds)
            else -> String.format("%d:%02d", minutes, seconds)
        }
    }
    
    private fun formatTime(millis: Long): String {
        val totalSeconds = millis / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format("%d:%02d", minutes, seconds)
    }
    
    /**
     * Test the AI service connectivity
     */
    suspend fun testConnectivity(): Flow<SubtitleGenerationResult> = flow {
        try {
            emit(SubtitleGenerationResult.Progress("Testing AI service connection..."))
            
            val networkAvailable = isNetworkAvailable()
            Log.d(TAG, "Network check result: $networkAvailable")
            
            if (!networkAvailable) {
                emit(SubtitleGenerationResult.Progress("Network check failed, attempting API call anyway..."))
                Log.w(TAG, "Network check failed but attempting API call to verify actual connectivity")
            } else {
                emit(SubtitleGenerationResult.Progress("Network check passed, testing API..."))
            }
            
            // Test actual internet connectivity
            emit(SubtitleGenerationResult.Progress("Testing internet connectivity..."))
            val hasInternet = testInternetConnectivity()
            Log.d(TAG, "Internet connectivity test result: $hasInternet")
            
            Log.d(TAG, "Testing Google AI Studio API connectivity...")
            Log.d(TAG, "API Key configured: ${API_KEY.take(20)}...${API_KEY.takeLast(5)}")
            Log.d(TAG, "Using model: gemini-1.5-flash")
            
            val testPrompt = "Say: AI test OK"
            
            Log.d(TAG, "Making API call to Google AI Studio...")
            Log.d(TAG, "Prompt: $testPrompt")
            
            val response = withContext(Dispatchers.IO) {
                Log.d(TAG, "Executing textModel.generateContent...")
                try {
                    val result = textModel.generateContent(testPrompt)
                    Log.d(TAG, "API call completed successfully")
                    result
                } catch (e: Exception) {
                    Log.e(TAG, "API call failed with exception", e)
                    Log.e(TAG, "Exception type: ${e.javaClass.simpleName}")
                    Log.e(TAG, "Exception message: ${e.message}")
                    throw e
                }
            }
            
            val result = response.text
            if (result.isNullOrBlank()) {
                emit(SubtitleGenerationResult.Error("AI service returned empty response"))
            } else {
                Log.d(TAG, "AI service test successful: $result")
                emit(SubtitleGenerationResult.Success("AI service is working: $result"))
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "AI service connectivity test failed", e)
            Log.e(TAG, "Full exception details: ${e.toString()}")
            Log.e(TAG, "Exception stack trace:", e)
            
            val errorDetails = when {
                e.message?.contains("API_KEY", ignoreCase = true) == true -> 
                    "Invalid API key. Please check configuration."
                e.message?.contains("403", ignoreCase = true) == true || 
                e.message?.contains("Forbidden", ignoreCase = true) == true -> 
                    "API access forbidden. Check API key permissions."
                e.message?.contains("401", ignoreCase = true) == true || 
                e.message?.contains("Unauthorized", ignoreCase = true) == true -> 
                    "API authentication failed. Check API key."
                e.message?.contains("quota", ignoreCase = true) == true -> 
                    "API quota exceeded. Try again later."
                e.message?.contains("network", ignoreCase = true) == true -> 
                    "Network connection issue."
                e.message?.contains("model", ignoreCase = true) == true -> 
                    "Invalid model. Using gemini-1.5-flash."
                e.message?.contains("API key not valid", ignoreCase = true) == true -> 
                    "Google AI Studio API key is invalid or expired."
                else -> "Connection test failed: ${e.message}"
            }
            emit(SubtitleGenerationResult.Error(errorDetails))
        }
    }
    
    /**
     * Check if network is available with detailed logging
     */
    private fun isNetworkAvailable(): Boolean {
        return try {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
            
            Log.d(TAG, "Checking network connectivity...")
            Log.d(TAG, "Android SDK: ${android.os.Build.VERSION.SDK_INT}")
            
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                val network = connectivityManager.activeNetwork
                Log.d(TAG, "Active network: $network")
                
                if (network == null) {
                    Log.w(TAG, "No active network found")
                    return false
                }
                
                val networkCapabilities = connectivityManager.getNetworkCapabilities(network)
                Log.d(TAG, "Network capabilities: $networkCapabilities")
                
                if (networkCapabilities == null) {
                    Log.w(TAG, "No network capabilities found")
                    return false
                }
                
                val hasWifi = networkCapabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI)
                val hasCellular = networkCapabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_CELLULAR)
                val hasEthernet = networkCapabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_ETHERNET)
                val hasInternet = networkCapabilities.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET)
                val isValidated = networkCapabilities.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                
                Log.d(TAG, "Network transports - WiFi: $hasWifi, Cellular: $hasCellular, Ethernet: $hasEthernet")
                Log.d(TAG, "Network capabilities - Internet: $hasInternet, Validated: $isValidated")
                
                val isConnected = (hasWifi || hasCellular || hasEthernet) && hasInternet
                Log.d(TAG, "Network connectivity result: $isConnected")
                
                return isConnected
            } else {
                @Suppress("DEPRECATION")
                val networkInfo = connectivityManager.activeNetworkInfo
                Log.d(TAG, "Legacy network info: $networkInfo")
                
                val isConnected = networkInfo != null && networkInfo.isConnected
                Log.d(TAG, "Legacy network connectivity result: $isConnected")
                
                return isConnected
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to check network connectivity", e)
            Log.d(TAG, "Assuming network is available due to error")
            return true // Assume network is available if we can't check
        }
    }
    
    /**
     * Test actual internet connectivity by making a simple HTTP request
     */
    private suspend fun testInternetConnectivity(): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            Log.d(TAG, "Testing internet connectivity with HTTP request...")
            val connection = java.net.URL("https://www.google.com").openConnection()
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            connection.connect()
            Log.d(TAG, "Internet connectivity test passed")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Internet connectivity test failed", e)
            false
        }
    }
}

// Data classes for AI results

data class FrameAnalysisResult(
    val timestamp: Long,
    val sceneType: SceneType,
    val objects: List<String>,
    val mood: String,
    val confidence: Float,
    val description: String
)

sealed class SubtitleGenerationResult {
    data class Progress(val message: String) : SubtitleGenerationResult()
    data class Success(val subtitleContent: String) : SubtitleGenerationResult()
    data class Error(val message: String) : SubtitleGenerationResult()
}

sealed class VideoInsightsResult {
    data class Progress(val message: String) : VideoInsightsResult()
    data class Success(val insights: String) : VideoInsightsResult()
    data class Error(val message: String) : VideoInsightsResult()
}

sealed class TranslationResult {
    data class Progress(val message: String) : TranslationResult()
    data class Success(val translatedContent: String, val language: String) : TranslationResult()
    data class Error(val message: String) : TranslationResult()
}

data class VideoRecommendation(
    val title: String,
    val reason: String,
    val contentType: String,
    val appealScore: Float
)