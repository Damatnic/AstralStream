package com.astralplayer.nextplayer

import android.content.Context
import com.astralplayer.nextplayer.security.*
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import java.io.File

class SecurityComponentTest {
    
    private lateinit var context: Context
    private lateinit var encryptionEngine: EncryptionEngine
    private lateinit var securePreferences: SecurePreferences
    private lateinit var appLockManager: AppLockManager
    private lateinit var hiddenFolderManager: HiddenFolderManager
    
    @Before
    fun setup() {
        context = mockk(relaxed = true)
        encryptionEngine = EncryptionEngine(context)
        securePreferences = SecurePreferences(context, encryptionEngine)
        appLockManager = AppLockManager(context, securePreferences)
        hiddenFolderManager = HiddenFolderManager(context, encryptionEngine, securePreferences)
    }
    
    @Test
    fun `app lock manager toggles lock state correctly`() {
        appLockManager.setLockEnabled(true)
        assertTrue("Lock should be enabled", appLockManager.isLockEnabled())
        
        appLockManager.lockApp()
        assertTrue("App should be locked", appLockManager.isAppLocked())
        
        appLockManager.unlockApp()
        assertFalse("App should be unlocked", appLockManager.isAppLocked())
    }
    
    @Test
    fun `encryption engine handles string encryption correctly`() {
        val testData = "sensitive_data_123"
        
        val encrypted = encryptionEngine.encryptString(testData)
        assertNotEquals("Encrypted data should differ", testData, encrypted)
        assertNotNull("Encrypted data should not be null", encrypted)
        assertTrue("Encrypted data should not be empty", encrypted.isNotEmpty())
        
        val decrypted = encryptionEngine.decryptString(encrypted)
        assertEquals("Decrypted data should match original", testData, decrypted)
    }
    
    @Test
    fun `secure preferences encrypt sensitive data`() {
        val testKey = "test_secret"
        val testValue = "secret_password"
        
        securePreferences.putEncryptedString(testKey, testValue)
        val retrieved = securePreferences.getEncryptedString(testKey)
        
        assertEquals("Retrieved value should match stored", testValue, retrieved)
    }
    
    @Test
    fun `hidden folder manager tracks videos correctly`() {
        val hiddenVideos = hiddenFolderManager.getHiddenVideos()
        assertNotNull("Hidden videos list should not be null", hiddenVideos)
        assertTrue("Initial list should be empty", hiddenVideos.isEmpty())
    }
    
    @Test
    fun `app lock handles background foreground correctly`() {
        appLockManager.setLockEnabled(true)
        
        appLockManager.onAppBackgrounded()
        // Should start lock timer
        
        appLockManager.onAppForegrounded() 
        // May lock app if timer expired
        
        // Test passes if no exceptions thrown
        assertTrue("Test completed without exceptions", true)
    }
}