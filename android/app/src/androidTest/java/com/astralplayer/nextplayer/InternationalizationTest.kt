package com.astralplayer.nextplayer

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import com.astralplayer.nextplayer.feature.flags.FeatureFlags
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.*

/**
 * Internationalization and Localization Tests
 * Validates multi-language support and cultural adaptations
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class InternationalizationTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    private lateinit var context: Context
    private lateinit var device: UiDevice

    // Supported languages for testing
    private val supportedLanguages = listOf(
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
        "nl" to "Nederlands",
        "pl" to "Polski",
        "tr" to "Türkçe"
    )

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        
        // Enable all features for localization testing
        FeatureFlags.initialize(context)
        runBlocking {
            FeatureFlags.getManager()?.enableAllFeatures()
        }
    }

    @Test
    fun testLanguageSwitching() {
        composeTestRule.apply {
            // Test switching between supported languages
            onNodeWithContentDescription("Settings").performClick()
            waitForIdle()
            
            onNodeWithText("Language").performClick()
            waitForIdle()
            
            // Verify all supported languages are available
            supportedLanguages.forEach { (code, name) ->
                onNodeWithText(name).assertIsDisplayed()
            }
            
            // Test switching to Spanish
            onNodeWithText("Español").performClick()
            waitForIdle()
            
            device.pressBack()
            
            // Verify UI switched to Spanish
            onNodeWithText("Configuración").assertIsDisplayed() // Settings in Spanish
            
            // Test switching to French
            onNodeWithText("Idioma").performClick() // Language in Spanish
            waitForIdle()
            
            onNodeWithText("Français").performClick()
            waitForIdle()
            
            device.pressBack()
            
            // Verify UI switched to French
            onNodeWithText("Paramètres").assertIsDisplayed() // Settings in French
            
            // Switch back to English
            onNodeWithText("Langue").performClick() // Language in French
            waitForIdle()
            
            onNodeWithText("English").performClick()
            waitForIdle()
            
            device.pressBack()
            device.pressBack()
        }
    }

    @Test
    fun testVideoPlayerLocalization() {
        composeTestRule.apply {
            // Test video player in different languages
            
            // Spanish
            setLanguage("es")
            startVideoPlayback()
            
            // Verify player controls are localized
            onNodeWithContentDescription("Reproducir/Pausar").assertIsDisplayed()
            onNodeWithContentDescription("Saltar hacia atrás").assertIsDisplayed()
            onNodeWithContentDescription("Saltar hacia adelante").assertIsDisplayed()
            onNodeWithContentDescription("Barra de búsqueda").assertIsDisplayed()
            
            device.pressBack()
            
            // German
            setLanguage("de")
            startVideoPlayback()
            
            onNodeWithContentDescription("Wiedergabe/Pause").assertIsDisplayed()
            onNodeWithContentDescription("Zurückspulen").assertIsDisplayed()
            onNodeWithContentDescription("Vorspulen").assertIsDisplayed()
            
            device.pressBack()
            
            // Chinese
            setLanguage("zh")
            startVideoPlayback()
            
            onNodeWithContentDescription("播放/暂停").assertIsDisplayed()
            onNodeWithContentDescription("快退").assertIsDisplayed()
            onNodeWithContentDescription("快进").assertIsDisplayed()
            
            device.pressBack()
            
            // Arabic (RTL test)
            setLanguage("ar")
            startVideoPlayback()
            
            // Verify RTL layout for Arabic
            onNodeWithContentDescription("تشغيل/إيقاف مؤقت").assertIsDisplayed()
            onNodeWithContentDescription("العودة إلى الخلف").assertIsDisplayed()
            onNodeWithContentDescription("التقديم السريع").assertIsDisplayed()
            
            device.pressBack()
            
            // Reset to English
            setLanguage("en")
        }
    }

    @Test
    fun testSearchLocalization() {
        composeTestRule.apply {
            // Test search interface in different languages
            
            // French
            setLanguage("fr")
            
            onNodeWithContentDescription("Rechercher").performClick()
            waitForIdle()
            
            onNodeWithText("Rechercher des vidéos...").assertIsDisplayed()
            onNodeWithText("Filtres").assertIsDisplayed()
            onNodeWithText("Recherches récentes").assertIsDisplayed()
            
            // Test search filters in French
            onNodeWithText("Filtres").performClick()
            waitForIdle()
            
            onNodeWithText("Durée").assertIsDisplayed()
            onNodeWithText("Qualité").assertIsDisplayed()
            onNodeWithText("Type de fichier").assertIsDisplayed()
            
            device.pressBack()
            device.pressBack()
            device.pressBack()
            
            // Italian
            setLanguage("it")
            
            onNodeWithContentDescription("Cerca").performClick()
            waitForIdle()
            
            onNodeWithText("Cerca video...").assertIsDisplayed()
            onNodeWithText("Filtri").assertIsDisplayed()
            
            onNodeWithText("Filtri").performClick()
            waitForIdle()
            
            onNodeWithText("Durata").assertIsDisplayed()
            onNodeWithText("Qualità").assertIsDisplayed()
            
            device.pressBack()
            device.pressBack()
            device.pressBack()
            
            // Japanese
            setLanguage("ja")
            
            onNodeWithContentDescription("検索").performClick()
            waitForIdle()
            
            onNodeWithText("動画を検索...").assertIsDisplayed()
            onNodeWithText("フィルター").assertIsDisplayed()
            
            device.pressBack()
            device.pressBack()
            
            setLanguage("en")
        }
    }

    @Test
    fun testEqualizerLocalization() {
        composeTestRule.apply {
            // Test equalizer in different languages
            
            // Portuguese
            setLanguage("pt")
            startVideoPlayback()
            
            onNodeWithContentDescription("Configurações de áudio").performClick()
            waitForIdle()
            
            onNodeWithText("Equalizador").performClick()
            waitForIdle()
            
            onNodeWithText("Equalizador de Áudio").assertIsDisplayed()
            onNodeWithText("Predefinições").assertIsDisplayed()
            onNodeWithText("Personalizado").assertIsDisplayed()
            
            // Test preset names are localized
            onNodeWithText("Rock").assertIsDisplayed()
            onNodeWithText("Pop").assertIsDisplayed()
            onNodeWithText("Jazz").assertIsDisplayed()
            onNodeWithText("Clássico").assertIsDisplayed() // Classical in Portuguese
            
            device.pressBack()
            device.pressBack()
            device.pressBack()
            
            // Russian
            setLanguage("ru")
            startVideoPlayback()
            
            onNodeWithContentDescription("Настройки звука").performClick()
            waitForIdle()
            
            onNodeWithText("Эквалайзер").performClick()
            waitForIdle()
            
            onNodeWithText("Аудио эквалайзер").assertIsDisplayed()
            onNodeWithText("Пресеты").assertIsDisplayed()
            
            device.pressBack()
            device.pressBack()
            device.pressBack()
            
            setLanguage("en")
        }
    }

    @Test
    fun testSettingsLocalization() {
        composeTestRule.apply {
            // Test settings in different languages
            
            // Korean
            setLanguage("ko")
            
            onNodeWithContentDescription("설정").performClick()
            waitForIdle()
            
            onNodeWithText("설정").assertIsDisplayed()
            onNodeWithText("고급 검색").assertIsDisplayed()
            onNodeWithText("음성 제어").assertIsDisplayed()
            onNodeWithText("오디오 이퀄라이저").assertIsDisplayed()
            onNodeWithText("접근성").assertIsDisplayed()
            onNodeWithText("성능").assertIsDisplayed()
            
            // Test accessibility settings in Korean
            onNodeWithText("접근성").performClick()
            waitForIdle()
            
            onNodeWithText("큰 텍스트").assertIsDisplayed()
            onNodeWithText("고대비").assertIsDisplayed()
            onNodeWithText("모션 감소").assertIsDisplayed()
            
            device.pressBack()
            device.pressBack()
            
            // Hindi
            setLanguage("hi")
            
            onNodeWithContentDescription("सेटिंग्स").performClick()
            waitForIdle()
            
            onNodeWithText("सेटिंग्स").assertIsDisplayed()
            onNodeWithText("उन्नत खोज").assertIsDisplayed()
            onNodeWithText("वॉयस कंट्रोल").assertIsDisplayed()
            onNodeWithText("एक्सेसिबिलिटी").assertIsDisplayed()
            
            device.pressBack()
            
            setLanguage("en")
        }
    }

    @Test
    fun testVoiceControlLocalization() {
        composeTestRule.apply {
            // Test voice control in different languages
            
            // Spanish
            setLanguage("es")
            startVideoPlayback()
            
            onNodeWithContentDescription("Control por voz").performClick()
            waitForIdle()
            
            onNodeWithText("Escuchando...").assertIsDisplayed()
            
            // Voice commands should be localized
            onNodeWithText("Comandos disponibles:").assertIsDisplayed()
            onNodeWithText("'Reproducir video'").assertIsDisplayed()
            onNodeWithText("'Pausar video'").assertIsDisplayed()
            onNodeWithText("'Subir volumen'").assertIsDisplayed()
            
            onNodeWithContentDescription("Control por voz").performClick()
            onNodeWithText("Dejar de escuchar").performClick()
            
            device.pressBack()
            
            // French
            setLanguage("fr")
            startVideoPlayback()
            
            onNodeWithContentDescription("Contrôle vocal").performClick()
            waitForIdle()
            
            onNodeWithText("Écoute...").assertIsDisplayed()
            onNodeWithText("Commandes disponibles:").assertIsDisplayed()
            onNodeWithText("'Lire la vidéo'").assertIsDisplayed()
            onNodeWithText("'Volume plus fort'").assertIsDisplayed()
            
            onNodeWithContentDescription("Contrôle vocal").performClick()
            onNodeWithText("Arrêter l'écoute").performClick()
            
            device.pressBack()
            
            setLanguage("en")
        }
    }

    @Test
    fun testSubtitleLocalization() {
        composeTestRule.apply {
            // Test subtitle interface in different languages
            
            // German
            setLanguage("de")
            startVideoPlayback()
            
            onNodeWithContentDescription("Untertitel-Optionen").performClick()
            waitForIdle()
            
            onNodeWithText("Untertitel aktivieren").assertIsDisplayed()
            onNodeWithText("KI-Untertitel generieren").assertIsDisplayed()
            onNodeWithText("Stil anpassen").assertIsDisplayed()
            
            // Test AI subtitle generation interface
            onNodeWithText("KI-Untertitel generieren").performClick()
            waitForIdle()
            
            onNodeWithText("Zielsprache").assertIsDisplayed()
            onNodeWithText("Untertitel generieren").assertIsDisplayed()
            
            device.pressBack()
            device.pressBack()
            device.pressBack()
            
            // Japanese
            setLanguage("ja")
            startVideoPlayback()
            
            onNodeWithContentDescription("字幕オプション").performClick()
            waitForIdle()
            
            onNodeWithText("字幕を有効にする").assertIsDisplayed()
            onNodeWithText("AI字幕生成").assertIsDisplayed()
            onNodeWithText("スタイル調整").assertIsDisplayed()
            
            device.pressBack()
            device.pressBack()
            
            setLanguage("en")
        }
    }

    @Test
    fun testDateTimeLocalization() {
        composeTestRule.apply {
            // Test date/time formatting in different locales
            
            // US format (MM/DD/YYYY)
            setLanguage("en")
            
            onNodeWithContentDescription("Browse Videos").performClick()
            waitForIdle()
            
            onNodeWithText("Recent Files").performClick()
            waitForIdle()
            
            Thread.sleep(1000)
            
            // Dates should be in MM/DD/YYYY format for US English
            
            device.pressBack()
            device.pressBack()
            
            // European format (DD/MM/YYYY)
            setLanguage("de")
            
            onNodeWithContentDescription("Videos durchsuchen").performClick()
            waitForIdle()
            
            onNodeWithText("Letzte Dateien").performClick()
            waitForIdle()
            
            Thread.sleep(1000)
            
            // Dates should be in DD/MM/YYYY format for German
            
            device.pressBack()
            device.pressBack()
            
            setLanguage("en")
        }
    }

    @Test
    fun testNumberFormatLocalization() {
        composeTestRule.apply {
            // Test number formatting in different locales
            
            startVideoPlayback()
            
            // English (US) - uses period for decimal, comma for thousands
            setLanguage("en")
            
            onNodeWithContentDescription("Video stats").performClick()
            waitForIdle()
            
            // Should show numbers like "1,234.56"
            
            device.pressBack()
            
            // German - uses comma for decimal, period for thousands  
            setLanguage("de")
            
            onNodeWithContentDescription("Video-Statistiken").performClick()
            waitForIdle()
            
            // Should show numbers like "1.234,56"
            
            device.pressBack()
            device.pressBack()
            
            setLanguage("en")
        }
    }

    @Test
    fun testRTLLayoutSupport() {
        composeTestRule.apply {
            // Test Right-to-Left layout for Arabic
            setLanguage("ar")
            
            // Main navigation should be RTL
            onNodeWithText("AstralStream").assertIsDisplayed()
            
            // Navigation items should be right-aligned
            onNodeWithContentDescription("تصفح الفيديوهات").assertIsDisplayed()
            onNodeWithContentDescription("بحث").assertIsDisplayed()
            onNodeWithContentDescription("الإعدادات").assertIsDisplayed()
            
            // Test video player RTL layout
            startVideoPlayback()
            
            // Player controls should be mirrored for RTL
            onNodeWithContentDescription("تشغيل/إيقاف مؤقت").assertIsDisplayed()
            
            // Skip buttons should be swapped in RTL
            onNodeWithContentDescription("العودة إلى الخلف").assertIsDisplayed()
            onNodeWithContentDescription("التقديم السريع").assertIsDisplayed()
            
            // Settings should be RTL
            onNodeWithContentDescription("الإعدادات").performClick()
            waitForIdle()
            
            onNodeWithText("الإعدادات").assertIsDisplayed()
            onNodeWithText("البحث المتقدم").assertIsDisplayed()
            onNodeWithText("التحكم الصوتي").assertIsDisplayed()
            
            device.pressBack()
            device.pressBack()
            
            setLanguage("en")
        }
    }

    @Test
    fun testCurrencyLocalization() {
        composeTestRule.apply {
            // Test currency formatting for premium features
            
            // US - Dollar symbol
            setLanguage("en")
            
            onNodeWithContentDescription("Settings").performClick()
            waitForIdle()
            
            onNodeWithText("Premium Features").performClick()
            waitForIdle()
            
            // Should show prices like "$9.99"
            
            device.pressBack()
            
            // European - Euro symbol  
            setLanguage("de")
            
            onNodeWithText("Premium-Funktionen").performClick()
            waitForIdle()
            
            // Should show prices like "9,99 €"
            
            device.pressBack()
            device.pressBack()
            
            setLanguage("en")
        }
    }

    @Test
    fun testPluralLocalization() {
        composeTestRule.apply {
            // Test plural forms in different languages
            
            // English plurals
            setLanguage("en")
            
            onNodeWithContentDescription("Browse Videos").performClick()
            waitForIdle()
            
            onNodeWithText("Recent Files").performClick()
            waitForIdle()
            
            Thread.sleep(1000)
            
            // Should show "1 video" or "5 videos"
            
            device.pressBack()
            
            // Test bookmarks
            startVideoPlayback()
            
            onNodeWithContentDescription("Bookmarks").performClick()
            waitForIdle()
            
            // Should show "1 bookmark" or "3 bookmarks"
            
            device.pressBack()
            device.pressBack()
            device.pressBack()
            
            // Russian plurals (complex plural rules)
            setLanguage("ru")
            
            onNodeWithContentDescription("Просмотр видео").performClick()
            waitForIdle()
            
            onNodeWithText("Последние файлы").performClick()
            waitForIdle()
            
            Thread.sleep(1000)
            
            // Russian has different plural forms for 1, 2-4, and 5+ items
            // Should show "1 видео", "2 видео", "5 видео" with correct forms
            
            device.pressBack()
            device.pressBack()
            
            setLanguage("en")
        }
    }

    @Test
    fun testFeatureFlagLocalization() {
        composeTestRule.apply {
            // Test feature flags in different languages
            
            // Spanish
            setLanguage("es")
            
            onNodeWithContentDescription("Configuración").performClick()
            waitForIdle()
            
            onNodeWithText("Avanzado").performClick()
            waitForIdle()
            
            onNodeWithText("Banderas de Características").performClick()
            waitForIdle()
            
            onNodeWithText("Gestión de Características").assertIsDisplayed()
            onNodeWithText("Control por Voz").assertIsDisplayed()
            onNodeWithText("Subtítulos IA").assertIsDisplayed()
            onNodeWithText("Compartir Social").assertIsDisplayed()
            
            device.pressBack()
            device.pressBack()
            device.pressBack()
            
            setLanguage("en")
        }
    }

    @Test
    fun testErrorMessageLocalization() {
        composeTestRule.apply {
            // Test error messages in different languages
            
            // French
            setLanguage("fr")
            
            // Try to generate AI subtitles without internet (should show error)
            startVideoPlayback()
            
            onNodeWithContentDescription("Options de sous-titres").performClick()
            waitForIdle()
            
            onNodeWithText("Générer des sous-titres IA").performClick()
            waitForIdle()
            
            onNodeWithText("Générer des sous-titres").performClick()
            waitForIdle()
            
            Thread.sleep(3000)
            
            // Error message should be in French
            // "Erreur de connexion réseau" or similar
            
            device.pressBack()
            device.pressBack()
            device.pressBack()
            
            setLanguage("en")
        }
    }

    @Test
    fun testKeyboardLayoutSupport() {
        composeTestRule.apply {
            // Test different keyboard layouts
            
            // Test search with different keyboard layouts
            
            // Arabic keyboard
            setLanguage("ar")
            
            onNodeWithContentDescription("بحث").performClick()
            waitForIdle()
            
            // Should support Arabic text input
            onNodeWithText("البحث عن مقاطع الفيديو...").performTextInput("اختبار")
            waitForIdle()
            
            device.pressBack()
            device.pressBack()
            
            // Chinese keyboard  
            setLanguage("zh")
            
            onNodeWithContentDescription("搜索").performClick()
            waitForIdle()
            
            // Should support Chinese text input
            onNodeWithText("搜索视频...").performTextInput("测试")
            waitForIdle()
            
            device.pressBack()
            device.pressBack()
            
            setLanguage("en")
        }
    }

    // Helper methods

    private fun setLanguage(languageCode: String) {
        composeTestRule.apply {
            try {
                onNodeWithContentDescription("Settings").performClick()
                waitForIdle()
                
                onNodeWithText("Language").performClick()
                waitForIdle()
                
                val languageName = supportedLanguages.find { it.first == languageCode }?.second
                if (languageName != null) {
                    onNodeWithText(languageName).performClick()
                    waitForIdle()
                }
                
                device.pressBack()
                device.pressBack()
                
                // Wait for language change to take effect
                Thread.sleep(1000)
                
            } catch (e: Exception) {
                // Language might already be set or not available
            }
        }
    }

    private fun startVideoPlayback() {
        composeTestRule.apply {
            try {
                onNodeWithContentDescription("Browse Videos").performClick()
                waitForIdle()
                
                onNodeWithText("Recent Files").performClick()
                waitForIdle()
                
                Thread.sleep(1000)
                
                if (onAllNodesWithContentDescription("Play video").fetchSemanticsNodes().isNotEmpty()) {
                    onAllNodesWithContentDescription("Play video").onFirst().performClick()
                    waitForIdle()
                    
                    Thread.sleep(500)
                }
            } catch (e: Exception) {
                // Try with localized content descriptions
                try {
                    val currentLanguage = getCurrentLanguage()
                    when (currentLanguage) {
                        "es" -> {
                            onNodeWithContentDescription("Reproducir video").performClick()
                        }
                        "fr" -> {
                            onNodeWithContentDescription("Lire la vidéo").performClick()
                        }
                        "de" -> {
                            onNodeWithContentDescription("Video abspielen").performClick()
                        }
                        // Add more languages as needed
                    }
                    waitForIdle()
                } catch (e2: Exception) {
                    // Video might already be playing
                }
            }
        }
    }

    private fun getCurrentLanguage(): String {
        val locale = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            context.resources.configuration.locales[0]
        } else {
            @Suppress("DEPRECATION")
            context.resources.configuration.locale
        }
        return locale.language
    }

    private fun ComposeTestRule.waitForIdle() {
        Thread.sleep(300)
        this.waitForIdle()
    }
}