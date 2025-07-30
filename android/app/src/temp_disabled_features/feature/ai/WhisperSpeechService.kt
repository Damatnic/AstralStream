package com.astralplayer.nextplayer.feature.ai

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * OpenAI Whisper API implementation for speech-to-text
 * Simpler to set up than Google Cloud Speech
 */
class WhisperSpeechService(private val context: Context) {
    
    companion object {
        private const val TAG = "WhisperSpeechService"
        private const val WHISPER_API_URL = "https://api.openai.com/v1/audio/transcriptions"
        private const val WHISPER_TRANSLATE_URL = "https://api.openai.com/v1/audio/translations"
        
        // Supported audio formats
        private val SUPPORTED_FORMATS = listOf("mp3", "mp4", "mpeg", "mpga", "m4a", "wav", "webm")
        private const val MAX_FILE_SIZE = 25 * 1024 * 1024 // 25 MB
    }
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()
    
    /**
     * Transcribe audio file to text using Whisper API
     */
    suspend fun transcribeAudio(
        audioFile: File,
        languageCode: String? = null,
        prompt: String? = null
    ): TranscriptionResult = withContext(Dispatchers.IO) {
        try {
            // Check if API key is configured
            if (AIServicesConfig.OPENAI_API_KEY == "YOUR_OPENAI_API_KEY") {
                return@withContext TranscriptionResult(
                    text = "",
                    confidence = 0f,
                    error = "OpenAI API key not configured. Please set it in AIServicesConfig.kt"
                )
            }
            
            // Validate file
            if (!audioFile.exists()) {
                return@withContext TranscriptionResult(
                    text = "",
                    confidence = 0f,
                    error = "Audio file does not exist"
                )
            }
            
            if (audioFile.length() > MAX_FILE_SIZE) {
                return@withContext TranscriptionResult(
                    text = "",
                    confidence = 0f,
                    error = "Audio file too large. Maximum size is 25 MB"
                )
            }
            
            // Build multipart request
            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(
                    "file",
                    audioFile.name,
                    audioFile.asRequestBody("audio/wav".toMediaType())
                )
                .addFormDataPart("model", "whisper-1")
                .apply {
                    // Add optional parameters
                    if (!languageCode.isNullOrEmpty()) {
                        val whisperLang = convertToWhisperLanguageCode(languageCode)
                        addFormDataPart("language", whisperLang)
                    }
                    if (!prompt.isNullOrEmpty()) {
                        addFormDataPart("prompt", prompt)
                    }
                    // Request detailed response with timestamps
                    addFormDataPart("response_format", "verbose_json")
                    addFormDataPart("timestamp_granularities[]", "word")
                }
                .build()
            
            val request = Request.Builder()
                .url(WHISPER_API_URL)
                .addHeader("Authorization", "Bearer ${AIServicesConfig.OPENAI_API_KEY}")
                .post(requestBody)
                .build()
            
            val response = client.newCall(request).execute()
            
            if (response.isSuccessful) {
                val responseBody = response.body?.string() ?: ""
                return@withContext parseWhisperResponse(responseBody)
            } else {
                val errorBody = response.body?.string() ?: "Unknown error"
                Log.e(TAG, "Whisper API error: $errorBody")
                return@withContext TranscriptionResult(
                    text = "",
                    confidence = 0f,
                    error = "API Error (${response.code}): $errorBody"
                )
            }
            
        } catch (e: IOException) {
            Log.e(TAG, "Network error", e)
            TranscriptionResult(
                text = "",
                confidence = 0f,
                error = "Network error: ${e.message}"
            )
        } catch (e: Exception) {
            Log.e(TAG, "Transcription failed", e)
            TranscriptionResult(
                text = "",
                confidence = 0f,
                error = "Transcription failed: ${e.message}"
            )
        }
    }
    
    /**
     * Translate audio to English using Whisper API
     */
    suspend fun translateAudio(
        audioFile: File,
        prompt: String? = null
    ): TranscriptionResult = withContext(Dispatchers.IO) {
        try {
            // Similar to transcribe but uses translation endpoint
            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(
                    "file",
                    audioFile.name,
                    audioFile.asRequestBody("audio/wav".toMediaType())
                )
                .addFormDataPart("model", "whisper-1")
                .apply {
                    if (!prompt.isNullOrEmpty()) {
                        addFormDataPart("prompt", prompt)
                    }
                    addFormDataPart("response_format", "verbose_json")
                }
                .build()
            
            val request = Request.Builder()
                .url(WHISPER_TRANSLATE_URL)
                .addHeader("Authorization", "Bearer ${AIServicesConfig.OPENAI_API_KEY}")
                .post(requestBody)
                .build()
            
            val response = client.newCall(request).execute()
            
            if (response.isSuccessful) {
                val responseBody = response.body?.string() ?: ""
                return@withContext parseWhisperResponse(responseBody)
            } else {
                val errorBody = response.body?.string() ?: "Unknown error"
                return@withContext TranscriptionResult(
                    text = "",
                    confidence = 0f,
                    error = "Translation Error (${response.code}): $errorBody"
                )
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Translation failed", e)
            TranscriptionResult(
                text = "",
                confidence = 0f,
                error = "Translation failed: ${e.message}"
            )
        }
    }
    
    /**
     * Parse Whisper API response
     */
    private fun parseWhisperResponse(response: String): TranscriptionResult {
        return try {
            val json = JSONObject(response)
            val text = json.getString("text")
            val duration = json.optDouble("duration", 0.0)
            
            // Whisper doesn't provide confidence scores, but we can estimate based on response
            val confidence = if (text.isNotEmpty()) 0.95f else 0f
            
            TranscriptionResult(
                text = text,
                confidence = confidence,
                error = null
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse Whisper response", e)
            TranscriptionResult(
                text = "",
                confidence = 0f,
                error = "Failed to parse response: ${e.message}"
            )
        }
    }
    
    /**
     * Transcribe with word-level timestamps
     */
    suspend fun transcribeWithTimestamps(
        audioFile: File,
        languageCode: String? = null
    ): DetailedTranscriptionResult = withContext(Dispatchers.IO) {
        try {
            // Make request with timestamp_granularities parameter
            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(
                    "file",
                    audioFile.name,
                    audioFile.asRequestBody("audio/wav".toMediaType())
                )
                .addFormDataPart("model", "whisper-1")
                .addFormDataPart("response_format", "verbose_json")
                .addFormDataPart("timestamp_granularities[]", "word")
                .apply {
                    if (!languageCode.isNullOrEmpty()) {
                        val whisperLang = convertToWhisperLanguageCode(languageCode)
                        addFormDataPart("language", whisperLang)
                    }
                }
                .build()
            
            val request = Request.Builder()
                .url(WHISPER_API_URL)
                .addHeader("Authorization", "Bearer ${AIServicesConfig.OPENAI_API_KEY}")
                .post(requestBody)
                .build()
            
            val response = client.newCall(request).execute()
            
            if (response.isSuccessful) {
                val responseBody = response.body?.string() ?: ""
                return@withContext parseDetailedWhisperResponse(responseBody)
            } else {
                return@withContext DetailedTranscriptionResult(
                    fullText = "",
                    words = emptyList(),
                    error = "API Error: ${response.code}"
                )
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Detailed transcription failed", e)
            DetailedTranscriptionResult(
                fullText = "",
                words = emptyList(),
                error = "Transcription failed: ${e.message}"
            )
        }
    }
    
    /**
     * Parse detailed Whisper response with timestamps
     */
    private fun parseDetailedWhisperResponse(response: String): DetailedTranscriptionResult {
        return try {
            val json = JSONObject(response)
            val text = json.getString("text")
            val words = mutableListOf<WordInfo>()
            
            // Parse word timestamps if available
            if (json.has("words")) {
                val wordsArray = json.getJSONArray("words")
                for (i in 0 until wordsArray.length()) {
                    val wordObj = wordsArray.getJSONObject(i)
                    words.add(
                        WordInfo(
                            word = wordObj.getString("word"),
                            startTime = (wordObj.getDouble("start") * 1000).toLong(),
                            endTime = (wordObj.getDouble("end") * 1000).toLong(),
                            confidence = 0.95f // Whisper doesn't provide per-word confidence
                        )
                    )
                }
            }
            
            DetailedTranscriptionResult(
                fullText = text,
                words = words,
                error = null
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse detailed response", e)
            DetailedTranscriptionResult(
                fullText = "",
                words = emptyList(),
                error = "Failed to parse response: ${e.message}"
            )
        }
    }
    
    /**
     * Convert language codes to Whisper format
     */
    private fun convertToWhisperLanguageCode(languageCode: String): String {
        // Whisper uses ISO 639-1 codes
        return when (languageCode) {
            "en-US", "en-GB", "en-AU" -> "en"
            "es-ES", "es-MX" -> "es"
            "fr-FR", "fr-CA" -> "fr"
            "de-DE", "de-AT" -> "de"
            "it-IT" -> "it"
            "pt-BR", "pt-PT" -> "pt"
            "zh-CN", "zh-TW" -> "zh"
            "ja-JP" -> "ja"
            "ko-KR" -> "ko"
            "ru-RU" -> "ru"
            "ar-SA" -> "ar"
            "hi-IN" -> "hi"
            else -> languageCode.take(2) // Use first two characters
        }
    }
}

/**
 * Advantages of Whisper API:
 * 
 * 1. EASY SETUP:
 *    - Just need an OpenAI API key
 *    - No complex authentication
 *    - Works immediately
 * 
 * 2. EXCELLENT ACCURACY:
 *    - State-of-the-art model
 *    - Handles accents well
 *    - Good with background noise
 * 
 * 3. MULTILINGUAL:
 *    - 50+ languages supported
 *    - Automatic language detection
 *    - Translation to English
 * 
 * 4. FEATURES:
 *    - Word-level timestamps
 *    - Punctuation and formatting
 *    - Speaker diarization (in prompts)
 * 
 * 5. COST:
 *    - $0.006 per minute (very affordable)
 *    - No free tier, but cheap enough for testing
 */