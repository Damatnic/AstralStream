// ApiKeyManager.kt
package com.astralplayer.core.config

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ApiKeyManager @Inject constructor(
    private val context: Context
) {
    companion object {
        private const val ENCRYPTED_PREFS_NAME = "astral_encrypted_prefs"
        private const val OPENAI_API_KEY = "openai_api_key"
        private const val GOOGLE_AI_KEY = "google_ai_key"
        private const val AZURE_SPEECH_KEY = "azure_speech_key"
        private const val ASSEMBLY_AI_KEY = "assembly_ai_key"
        private const val DEEPGRAM_KEY = "deepgram_key"
    }
    
    private val encryptedSharedPreferences by lazy {
        try {
            val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
            EncryptedSharedPreferences.create(
                ENCRYPTED_PREFS_NAME,
                masterKeyAlias,
                context,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            Timber.e(e, "Failed to create encrypted preferences, falling back to regular SharedPreferences")
            context.getSharedPreferences(ENCRYPTED_PREFS_NAME, Context.MODE_PRIVATE)
        }
    }
    
    data class ApiKeyConfig(
        val openAiKey: String? = null,
        val googleAiKey: String? = null,
        val azureSpeechKey: String? = null,
        val assemblyAiKey: String? = null,
        val deepgramKey: String? = null
    ) {
        fun hasAnyKey(): Boolean = listOfNotNull(
            openAiKey, googleAiKey, azureSpeechKey, assemblyAiKey, deepgramKey
        ).any { it.isNotBlank() }
        
        fun getBestAvailableKey(): Pair<String, String>? {
            return when {
                !deepgramKey.isNullOrBlank() -> "deepgram" to deepgramKey
                !assemblyAiKey.isNullOrBlank() -> "assembly" to assemblyAiKey
                !googleAiKey.isNullOrBlank() -> "google" to googleAiKey
                !azureSpeechKey.isNullOrBlank() -> "azure" to azureSpeechKey
                !openAiKey.isNullOrBlank() -> "openai" to openAiKey
                else -> null
            }
        }
    }
    
    fun getApiKeys(): ApiKeyConfig {
        return try {
            ApiKeyConfig(
                openAiKey = encryptedSharedPreferences.getString(OPENAI_API_KEY, null),
                googleAiKey = encryptedSharedPreferences.getString(GOOGLE_AI_KEY, null),
                azureSpeechKey = encryptedSharedPreferences.getString(AZURE_SPEECH_KEY, null),
                assemblyAiKey = encryptedSharedPreferences.getString(ASSEMBLY_AI_KEY, null),
                deepgramKey = encryptedSharedPreferences.getString(DEEPGRAM_KEY, null)
            )
        } catch (e: Exception) {
            Timber.e(e, "Failed to retrieve API keys")
            ApiKeyConfig()
        }
    }
    
    fun setOpenAiKey(key: String?) {
        try {
            encryptedSharedPreferences.edit()
                .putString(OPENAI_API_KEY, key?.takeIf { it.isNotBlank() })
                .apply()
        } catch (e: Exception) {
            Timber.e(e, "Failed to store OpenAI API key")
        }
    }
    
    fun setGoogleAiKey(key: String?) {
        try {
            encryptedSharedPreferences.edit()
                .putString(GOOGLE_AI_KEY, key?.takeIf { it.isNotBlank() })
                .apply()
        } catch (e: Exception) {
            Timber.e(e, "Failed to store Google AI API key")
        }
    }
    
    fun setAzureSpeechKey(key: String?) {
        try {
            encryptedSharedPreferences.edit()
                .putString(AZURE_SPEECH_KEY, key?.takeIf { it.isNotBlank() })
                .apply()
        } catch (e: Exception) {
            Timber.e(e, "Failed to store Azure Speech API key")
        }
    }
    
    fun setAssemblyAiKey(key: String?) {
        try {
            encryptedSharedPreferences.edit()
                .putString(ASSEMBLY_AI_KEY, key?.takeIf { it.isNotBlank() })
                .apply()
        } catch (e: Exception) {
            Timber.e(e, "Failed to store AssemblyAI API key")
        }
    }
    
    fun setDeepgramKey(key: String?) {
        try {
            encryptedSharedPreferences.edit()
                .putString(DEEPGRAM_KEY, key?.takeIf { it.isNotBlank() })
                .apply()
        } catch (e: Exception) {
            Timber.e(e, "Failed to store Deepgram API key")
        }
    }
    
    fun clearAllKeys() {
        try {
            encryptedSharedPreferences.edit().clear().apply()
        } catch (e: Exception) {
            Timber.e(e, "Failed to clear API keys")
        }
    }
    
    fun validateApiKey(service: String, key: String): Boolean {
        return when (service.lowercase()) {
            "openai" -> key.startsWith("sk-") && key.length > 20
            "google" -> key.length > 20 && key.matches(Regex("[A-Za-z0-9_-]+"))
            "azure" -> key.length >= 32
            "assembly" -> key.length >= 32
            "deepgram" -> key.length >= 32
            else -> key.isNotBlank()
        }
    }
}