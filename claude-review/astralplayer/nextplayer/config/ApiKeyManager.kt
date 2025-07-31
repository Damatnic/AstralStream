package com.astralplayer.nextplayer.config

import android.content.Context
import android.util.Log
import java.io.IOException
import java.util.Properties

/**
 * Secure API Key Manager for handling sensitive API credentials
 * Loads API keys from assets/api_config.properties file
 */
object ApiKeyManager {
    private const val TAG = "ApiKeyManager"
    private const val CONFIG_FILE = "api_config.properties"
    
    private var properties: Properties? = null
    private var isInitialized = false
    
    /**
     * Initialize the API Key Manager with application context
     */
    fun initialize(context: Context) {
        if (isInitialized) return
        
        try {
            properties = Properties().apply {
                context.assets.open(CONFIG_FILE).use { inputStream ->
                    load(inputStream)
                }
            }
            isInitialized = true
            Log.d(TAG, "API Key Manager initialized successfully")
        } catch (e: IOException) {
            Log.e(TAG, "Failed to load API configuration: ${e.message}")
            // In production, you might want to handle this more gracefully
            // For now, we'll initialize with empty properties
            properties = Properties()
            isInitialized = true
        }
    }
    
    /**
     * Get Gemini (Google AI Studio) API Key
     */
    fun getGeminiApiKey(): String? {
        checkInitialized()
        return properties?.getProperty("GEMINI_API_KEY")
    }
    
    /**
     * Get Anthropic Claude API Key
     */
    fun getAnthropicApiKey(): String? {
        checkInitialized()
        return properties?.getProperty("ANTHROPIC_API_KEY")
    }
    
    /**
     * Get OpenAI API Key (if configured)
     */
    fun getOpenAIApiKey(): String? {
        checkInitialized()
        return properties?.getProperty("OPENAI_API_KEY")
    }
    
    /**
     * Get any custom API key by name
     */
    fun getApiKey(keyName: String): String? {
        checkInitialized()
        return properties?.getProperty(keyName)
    }
    
    /**
     * Check if API keys are properly configured
     */
    fun hasRequiredApiKeys(): Boolean {
        checkInitialized()
        return !getGeminiApiKey().isNullOrEmpty() || !getAnthropicApiKey().isNullOrEmpty()
    }
    
    /**
     * Get all available API key names (for debugging)
     */
    fun getAvailableKeys(): Set<String> {
        checkInitialized()
        return properties?.stringPropertyNames() ?: emptySet()
    }
    
    private fun checkInitialized() {
        if (!isInitialized) {
            throw IllegalStateException("ApiKeyManager not initialized. Call initialize(context) first.")
        }
    }
    
    /**
     * Clear all loaded API keys (for security)
     */
    fun clear() {
        properties?.clear()
        properties = null
        isInitialized = false
    }
}