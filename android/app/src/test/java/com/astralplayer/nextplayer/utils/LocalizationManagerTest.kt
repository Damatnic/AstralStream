package com.astralplayer.nextplayer.utils

import org.junit.Test
import org.junit.Assert.*

class LocalizationManagerTest {
    
    @Test
    fun testSupportedLanguagesCount() {
        val languages = LocalizationManager.getSupportedLanguages()
        assertEquals("Should support 15 languages", 15, languages.size)
    }
    
    @Test
    fun testSupportedLanguagesContainEnglish() {
        val languages = LocalizationManager.getSupportedLanguages()
        assertTrue("Should contain English", languages.containsKey("en"))
        assertEquals("English", languages["en"])
    }
    
    @Test
    fun testSupportedLanguagesContainSpanish() {
        val languages = LocalizationManager.getSupportedLanguages()
        assertTrue("Should contain Spanish", languages.containsKey("es"))
        assertEquals("Español", languages["es"])
    }
}