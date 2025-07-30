package com.astralplayer.nextplayer.feature.ai

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Claude AI Service for advanced AI features
 * Uses Claude 3 Haiku (cheapest and fastest model)
 * 
 * SECURITY WARNING: Never commit API keys to source control!
 * For production, use environment variables or secure key storage
 */
class ClaudeAIService(private val context: Context) {
    
    companion object {
        private const val TAG = "ClaudeAIService"
        private const val CLAUDE_API_URL = "https://api.anthropic.com/v1/messages"
        private const val CLAUDE_VERSION = "2023-06-01"
        
        // Claude 3 Haiku - Fastest and most cost-effective
        // Pricing: $0.25 per million input tokens, $1.25 per million output tokens
        private const val MODEL = "claude-3-haiku-20240307"
        
        // Maximum tokens for different operations
        private const val MAX_TOKENS_SUMMARY = 500
        private const val MAX_TOKENS_ANALYSIS = 1000
        private const val MAX_TOKENS_TRANSLATION = 2000
    }
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()
    
    /**
     * Analyze video content and generate insights
     */
    suspend fun analyzeVideoContent(
        videoDescription: String,
        detectedObjects: List<String>,
        extractedText: List<String>,
        duration: Long
    ): VideoAnalysisResult = withContext(Dispatchers.IO) {
        try {
            val prompt = buildString {
                append("Analyze this video content and provide insights:\n\n")
                append("Duration: ${duration / 1000} seconds\n")
                append("Detected objects: ${detectedObjects.joinToString(", ")}\n")
                append("Text found in video: ${extractedText.joinToString(", ")}\n")
                append("Description: $videoDescription\n\n")
                append("Please provide:\n")
                append("1. A brief summary (2-3 sentences)\n")
                append("2. Key topics or themes\n")
                append("3. Suggested tags for categorization\n")
                append("4. Content rating (G, PG, PG-13, R)\n")
                append("5. Any notable insights")
            }
            
            val response = callClaudeAPI(prompt, MAX_TOKENS_ANALYSIS)
            
            if (response != null) {
                parseVideoAnalysisResponse(response)
            } else {
                VideoAnalysisResult(
                    summary = "Analysis failed",
                    topics = emptyList(),
                    tags = emptyList(),
                    rating = "Unknown",
                    insights = emptyList()
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Video analysis failed", e)
            VideoAnalysisResult(
                summary = "Error: ${e.message}",
                topics = emptyList(),
                tags = emptyList(),
                rating = "Unknown",
                insights = emptyList()
            )
        }
    }
    
    /**
     * Generate natural subtitles from transcribed text
     */
    suspend fun enhanceSubtitles(
        rawTranscription: String,
        languageCode: String
    ): String = withContext(Dispatchers.IO) {
        try {
            val prompt = buildString {
                append("Enhance these auto-generated subtitles for better readability:\n\n")
                append("Raw transcription: $rawTranscription\n\n")
                append("Language: $languageCode\n\n")
                append("Please:\n")
                append("1. Add proper punctuation and capitalization\n")
                append("2. Fix any obvious transcription errors\n")
                append("3. Break into natural subtitle segments (max 42 characters per line)\n")
                append("4. Keep the original meaning intact\n")
                append("5. Return only the enhanced text, no explanations")
            }
            
            val response = callClaudeAPI(prompt, MAX_TOKENS_TRANSLATION)
            response ?: rawTranscription
        } catch (e: Exception) {
            Log.e(TAG, "Subtitle enhancement failed", e)
            rawTranscription
        }
    }
    
    /**
     * Enhance a batch of subtitles with context for maximum accuracy
     */
    suspend fun enhanceSubtitleBatch(
        text: String, 
        context: String = "", 
        previousSubtitles: String = ""
    ): String = withContext(Dispatchers.IO) {
        try {
            val prompt = buildString {
                append("You are an expert at creating high-accuracy subtitles. Please enhance the following subtitle text for near-100% accuracy and readability.\n\n")
                if (context.isNotEmpty()) {
                    append("Context: $context\n")
                }
                if (previousSubtitles.isNotEmpty()) {
                    append("Previous subtitles for context: \"$previousSubtitles\"\n")
                }
                append("\nCurrent subtitle text to enhance: \"$text\"\n\n")
                append("Instructions for maximum accuracy:\n")
                append("- Fix all grammar, punctuation, and spelling errors\n")
                append("- Ensure proper capitalization and sentence structure\n")
                append("- Use context from previous subtitles to improve accuracy\n")
                append("- Make it natural and conversational\n")
                append("- Break into proper subtitle-length segments\n")
                append("- Use common words when possible (avoid complex vocabulary)\n")
                append("- Ensure perfect punctuation for readability\n")
                append("- Keep the exact meaning and timing intent\n\n")
                append("Return only the enhanced subtitle text, separated by periods where natural breaks should occur.")
            }
            
            val response = callClaudeAPI(prompt, MAX_TOKENS_TRANSLATION)
            response ?: text
        } catch (e: Exception) {
            Log.e(TAG, "Subtitle batch enhancement failed", e)
            text
        }
    }
    
    /**
     * Generate video summary
     */
    suspend fun generateVideoSummary(
        videoTitle: String,
        keyMoments: List<String>,
        duration: Long
    ): String = withContext(Dispatchers.IO) {
        try {
            val prompt = buildString {
                append("Generate a concise summary for this video:\n\n")
                append("Title: $videoTitle\n")
                append("Duration: ${duration / 60000} minutes\n")
                append("Key moments:\n")
                keyMoments.forEachIndexed { index, moment ->
                    append("${index + 1}. $moment\n")
                }
                append("\nProvide a 2-3 paragraph summary that captures the main content and highlights.")
            }
            
            val response = callClaudeAPI(prompt, MAX_TOKENS_SUMMARY)
            response ?: "Summary generation failed"
        } catch (e: Exception) {
            Log.e(TAG, "Summary generation failed", e)
            "Error generating summary: ${e.message}"
        }
    }
    
    /**
     * Translate text with context awareness
     */
    suspend fun translateWithContext(
        text: String,
        sourceLanguage: String,
        targetLanguage: String,
        context: String? = null
    ): String = withContext(Dispatchers.IO) {
        try {
            val prompt = buildString {
                append("Translate the following text from $sourceLanguage to $targetLanguage:\n\n")
                append("Text: $text\n\n")
                if (!context.isNullOrEmpty()) {
                    append("Context: $context\n\n")
                }
                append("Provide a natural, contextually appropriate translation. Return only the translated text.")
            }
            
            val response = callClaudeAPI(prompt, MAX_TOKENS_TRANSLATION)
            response ?: text
        } catch (e: Exception) {
            Log.e(TAG, "Translation failed", e)
            text
        }
    }
    
    /**
     * Generate scene descriptions for accessibility
     */
    suspend fun generateSceneDescriptions(
        sceneData: List<SceneData>
    ): List<String> = withContext(Dispatchers.IO) {
        try {
            val prompt = buildString {
                append("Generate accessibility descriptions for these video scenes:\n\n")
                sceneData.forEachIndexed { index, scene ->
                    append("Scene ${index + 1}:\n")
                    append("- Time: ${scene.timestamp}ms\n")
                    append("- Objects: ${scene.objects.joinToString(", ")}\n")
                    append("- Action: ${scene.action}\n\n")
                }
                append("For each scene, provide a brief audio description (1-2 sentences) that would help visually impaired users understand what's happening.")
            }
            
            val response = callClaudeAPI(prompt, MAX_TOKENS_ANALYSIS)
            if (response != null) {
                parseSceneDescriptions(response)
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Scene description generation failed", e)
            emptyList()
        }
    }
    
    /**
     * Make API call to Claude
     */
    private suspend fun callClaudeAPI(
        prompt: String,
        maxTokens: Int
    ): String? = withContext(Dispatchers.IO) {
        try {
            // Check if API key is configured
            val apiKey = getApiKey()
            if (apiKey.isEmpty()) {
                Log.e(TAG, "Claude API key not configured")
                return@withContext null
            }
            
            val requestBody = JSONObject().apply {
                put("model", MODEL)
                put("max_tokens", maxTokens)
                put("messages", JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "user")
                        put("content", prompt)
                    })
                })
            }
            
            val request = Request.Builder()
                .url(CLAUDE_API_URL)
                .addHeader("anthropic-version", CLAUDE_VERSION)
                .addHeader("x-api-key", apiKey)
                .addHeader("content-type", "application/json")
                .post(requestBody.toString().toRequestBody("application/json".toMediaType()))
                .build()
            
            val response = client.newCall(request).execute()
            
            if (response.isSuccessful) {
                val responseBody = response.body?.string() ?: ""
                val json = JSONObject(responseBody)
                val content = json.getJSONArray("content")
                    .getJSONObject(0)
                    .getString("text")
                return@withContext content
            } else {
                val error = response.body?.string() ?: "Unknown error"
                Log.e(TAG, "Claude API error: $error")
                return@withContext null
            }
            
        } catch (e: IOException) {
            Log.e(TAG, "Network error calling Claude API", e)
            null
        } catch (e: Exception) {
            Log.e(TAG, "Error calling Claude API", e)
            null
        }
    }
    
    /**
     * Get API key from secure storage
     * WARNING: This is temporary - implement proper secure storage for production
     */
    private fun getApiKey(): String {
        // First check if it's in AIServicesConfig
        if (AIServicesConfig.CLAUDE_API_KEY != "YOUR_CLAUDE_API_KEY") {
            return AIServicesConfig.CLAUDE_API_KEY
        }
        
        // For production, use:
        // 1. Android Keystore
        // 2. Encrypted SharedPreferences
        // 3. Environment variables
        // 4. Server-side proxy
        
        return ""
    }
    
    /**
     * Parse video analysis response
     */
    private fun parseVideoAnalysisResponse(response: String): VideoAnalysisResult {
        // Simple parsing - in production use proper JSON parsing
        val lines = response.split("\n")
        var summary = ""
        val topics = mutableListOf<String>()
        val tags = mutableListOf<String>()
        var rating = "PG"
        val insights = mutableListOf<String>()
        
        var currentSection = ""
        for (line in lines) {
            when {
                line.contains("summary", ignoreCase = true) -> currentSection = "summary"
                line.contains("topics", ignoreCase = true) -> currentSection = "topics"
                line.contains("tags", ignoreCase = true) -> currentSection = "tags"
                line.contains("rating", ignoreCase = true) -> currentSection = "rating"
                line.contains("insights", ignoreCase = true) -> currentSection = "insights"
                else -> {
                    val content = line.trim()
                    if (content.isNotEmpty() && !content.matches(Regex("\\d+\\..*"))) {
                        when (currentSection) {
                            "summary" -> summary += "$content "
                            "topics" -> topics.add(content.removePrefix("- "))
                            "tags" -> tags.add(content.removePrefix("- "))
                            "rating" -> rating = content
                            "insights" -> insights.add(content.removePrefix("- "))
                        }
                    }
                }
            }
        }
        
        return VideoAnalysisResult(
            summary = summary.trim(),
            topics = topics,
            tags = tags,
            rating = rating,
            insights = insights
        )
    }
    
    /**
     * Parse scene descriptions
     */
    private fun parseSceneDescriptions(response: String): List<String> {
        val descriptions = mutableListOf<String>()
        val lines = response.split("\n")
        
        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isNotEmpty() && !trimmed.startsWith("Scene")) {
                descriptions.add(trimmed.removePrefix("- "))
            }
        }
        
        return descriptions
    }
}

/**
 * Data classes for Claude AI
 */
data class VideoAnalysisResult(
    val summary: String,
    val topics: List<String>,
    val tags: List<String>,
    val rating: String,
    val insights: List<String>
)

data class SceneData(
    val timestamp: Long,
    val objects: List<String>,
    val action: String
)