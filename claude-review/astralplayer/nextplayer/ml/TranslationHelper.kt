package com.astralplayer.nextplayer.ml

import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlinx.coroutines.tasks.await

class TranslationHelper {
    private var translator: Translator? = null
    
    suspend fun prepareTranslator(
        sourceLanguage: String = TranslateLanguage.ENGLISH,
        targetLanguage: String = TranslateLanguage.SPANISH
    ) {
        val options = TranslatorOptions.Builder()
            .setSourceLanguage(sourceLanguage)
            .setTargetLanguage(targetLanguage)
            .build()
            
        translator = Translation.getClient(options)
        
        // Download the model if needed
        val conditions = DownloadConditions.Builder()
            .requireWifi()
            .build()
            
        translator?.downloadModelIfNeeded(conditions)?.await()
    }
    
    suspend fun translate(text: String): String? {
        return try {
            translator?.translate(text)?.await()
        } catch (e: Exception) {
            null
        }
    }
    
    fun cleanup() {
        translator?.close()
        translator = null
    }
}