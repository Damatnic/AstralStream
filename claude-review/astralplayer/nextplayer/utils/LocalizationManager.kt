package com.astralplayer.nextplayer.utils

import android.content.Context
import android.content.res.Configuration
import java.util.*

object LocalizationManager {
    
    private val supportedLanguages = mapOf(
        "en" to "English",
        "es" to "Español", 
        "fr" to "Français",
        "de" to "Deutsch",
        "it" to "Italiano",
        "pt" to "Português",
        "ru" to "Русский",
        "zh" to "中文",
        "ja" to "日本語",
        "ko" to "한국어",
        "ar" to "العربية",
        "hi" to "हिन्दी",
        "tr" to "Türkçe",
        "pl" to "Polski",
        "nl" to "Nederlands"
    )
    
    fun getSupportedLanguages(): Map<String, String> = supportedLanguages
    
    fun setLanguage(context: Context, languageCode: String) {
        val locale = Locale(languageCode)
        Locale.setDefault(locale)
        
        val config = Configuration()
        config.setLocale(locale)
        context.resources.updateConfiguration(config, context.resources.displayMetrics)
    }
    
    fun getCurrentLanguage(context: Context): String {
        return context.resources.configuration.locales[0].language
    }
}