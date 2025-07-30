package com.astralplayer.nextplayer.feature.ai

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Stub implementation of LiveTranslationManager for testing
 * This allows the main app to build without ML Kit translation dependencies
 */
class LiveTranslationManager @Inject constructor(@ApplicationContext private val context: Context) {
    
    companion object {
        private const val TAG = "LiveTranslationManagerStub"
    }
    
    private val _isTranslating = MutableStateFlow(false)
    val isTranslating: StateFlow<Boolean> = _isTranslating.asStateFlow()
    
    private val _currentTranslation = MutableStateFlow<TranslationResult?>(null)
    val currentTranslation: StateFlow<TranslationResult?> = _currentTranslation.asStateFlow()
    
    private val _translationHistory = MutableStateFlow<List<TranslationResult>>(emptyList())
    val translationHistory: StateFlow<List<TranslationResult>> = _translationHistory.asStateFlow()
    
    private val _sourceLanguage = MutableStateFlow("auto")
    val sourceLanguage: StateFlow<String> = _sourceLanguage.asStateFlow()
    
    private val _targetLanguage = MutableStateFlow("en")
    val targetLanguage: StateFlow<String> = _targetLanguage.asStateFlow()
    
    suspend fun startLiveTranslation(
        sourceLanguageCode: String = "auto",
        targetLanguageCode: String = "en"
    ): Boolean {
        Log.d(TAG, "Live translation stub - feature disabled for testing")
        return false
    }
    
    fun stopLiveTranslation() {
        _isTranslating.value = false
        Log.d(TAG, "Live translation stopped (stub)")
    }
    
    suspend fun setSourceLanguage(languageCode: String) {
        _sourceLanguage.value = languageCode
    }
    
    suspend fun setTargetLanguage(languageCode: String) {
        _targetLanguage.value = languageCode
    }
    
    fun clearHistory() {
        _translationHistory.value = emptyList()
    }
    
    fun exportHistory(): String {
        return "Live Translation disabled for testing"
    }
    
    fun release() {
        Log.d(TAG, "Live translation manager released (stub)")
    }
}

data class TranslationResult(
    val sourceText: String,
    val translatedText: String,
    val sourceLanguage: String,
    val targetLanguage: String,
    val timestamp: Long,
    val isPartial: Boolean = false
)