package com.astralplayer.core.security

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.fragment.app.FragmentActivity
import io.mockk.*
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

@ExperimentalCoroutinesApi
class BiometricAuthManagerTest {
    
    @MockK
    private lateinit var context: Context
    
    @MockK
    private lateinit var activity: FragmentActivity
    
    @MockK
    private lateinit var biometricManager: BiometricManager
    
    @MockK
    private lateinit var keyStore: KeyStore
    
    @MockK
    private lateinit var keyGenerator: KeyGenerator
    
    @MockK
    private lateinit var secretKey: SecretKey
    
    @MockK
    private lateinit var cipher: Cipher
    
    private lateinit var biometricAuthManager: BiometricAuthManager
    
    @Before
    fun setup() {
        MockKAnnotations.init(this)
        
        mockkStatic(BiometricManager::class)
        mockkStatic(KeyStore::class)
        mockkStatic(KeyGenerator::class)
        mockkStatic(Cipher::class)
        
        every { BiometricManager.from(context) } returns biometricManager
        every { KeyStore.getInstance("AndroidKeyStore") } returns keyStore
        every { keyStore.load(null) } just Runs
        every { KeyGenerator.getInstance(any(), any<String>()) } returns keyGenerator
        every { Cipher.getInstance(any()) } returns cipher
        
        biometricAuthManager = BiometricAuthManager(context)
    }
    
    @Test
    fun `isBiometricAvailable should return true when biometric is enrolled`() {
        // Given
        every { biometricManager.canAuthenticate(any()) } returns BiometricManager.BIOMETRIC_SUCCESS
        
        // When
        val result = biometricAuthManager.isBiometricAvailable()
        
        // Then
        assertTrue(result)
    }
    
    @Test
    fun `isBiometricAvailable should return false when no biometric enrolled`() {
        // Given
        every { biometricManager.canAuthenticate(any()) } returns BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED
        
        // When
        val result = biometricAuthManager.isBiometricAvailable()
        
        // Then
        assertFalse(result)
    }
    
    @Test
    fun `authenticate should call success callback on successful authentication`() {
        // Given
        var successCalled = false
        val onSuccess = { successCalled = true }
        val onError: (BiometricError) -> Unit = { }
        
        val promptSlot = slot<BiometricPrompt.PromptInfo>()
        val callbackSlot = slot<BiometricPrompt.AuthenticationCallback>()
        
        mockkConstructor(BiometricPrompt::class)
        every { 
            anyConstructed<BiometricPrompt>().authenticate(capture(promptSlot)) 
        } answers {
            callbackSlot.captured.onAuthenticationSucceeded(mockk {
                every { cryptoObject } returns null
            })
        }
        
        every {
            constructedWith<BiometricPrompt>(any(), any(), capture(callbackSlot))
        } returns mockk()
        
        // When
        biometricAuthManager.authenticate(
            activity = activity,
            title = "Test Auth",
            onSuccess = onSuccess,
            onError = onError
        )
        
        // Then
        assertTrue(successCalled)
        assertEquals("Test Auth", promptSlot.captured.title)
    }
    
    @Test
    fun `authenticate should call error callback on authentication error`() {
        // Given
        var errorResult: BiometricError? = null
        val onSuccess = { }
        val onError: (BiometricError) -> Unit = { error -> errorResult = error }
        
        val callbackSlot = slot<BiometricPrompt.AuthenticationCallback>()
        
        mockkConstructor(BiometricPrompt::class)
        every { 
            anyConstructed<BiometricPrompt>().authenticate(any()) 
        } answers {
            callbackSlot.captured.onAuthenticationError(
                BiometricPrompt.ERROR_CANCELED,
                "User canceled"
            )
        }
        
        every {
            constructedWith<BiometricPrompt>(any(), any(), capture(callbackSlot))
        } returns mockk()
        
        // When
        biometricAuthManager.authenticate(
            activity = activity,
            title = "Test Auth",
            onSuccess = onSuccess,
            onError = onError
        )
        
        // Then
        assertNotNull(errorResult)
        assertEquals(BiometricPrompt.ERROR_CANCELED, errorResult?.errorCode)
        assertEquals("User canceled", errorResult?.message)
    }
    
    @Test
    fun `encryptWithBiometric should encrypt data correctly`() = runTest {
        // Given
        val data = "Test data".toByteArray()
        val keyAlias = "test_key"
        val encryptedBytes = "encrypted".toByteArray()
        val iv = "iv".toByteArray()
        
        every { keyStore.containsAlias(keyAlias) } returns true
        every { keyStore.getEntry(keyAlias, null) } returns mockk<KeyStore.SecretKeyEntry> {
            every { secretKey } returns this@BiometricAuthManagerTest.secretKey
        }
        
        every { cipher.init(Cipher.ENCRYPT_MODE, secretKey) } just Runs
        every { cipher.iv } returns iv
        every { cipher.doFinal(data) } returns encryptedBytes
        
        // When
        val result = biometricAuthManager.encryptWithBiometric(data, keyAlias)
        
        // Then
        assertArrayEquals(encryptedBytes, result.encryptedBytes)
        assertArrayEquals(iv, result.iv)
        assertEquals(keyAlias, result.keyAlias)
    }
    
    @Test
    fun `decryptWithBiometric should decrypt data correctly`() = runTest {
        // Given
        val encryptedData = EncryptedData(
            encryptedBytes = "encrypted".toByteArray(),
            iv = "iv".toByteArray(),
            keyAlias = "test_key"
        )
        val decryptedBytes = "decrypted".toByteArray()
        
        every { keyStore.getKey(encryptedData.keyAlias, null) } returns secretKey
        every { cipher.init(eq(Cipher.DECRYPT_MODE), eq(secretKey), any()) } just Runs
        every { cipher.doFinal(encryptedData.encryptedBytes) } returns decryptedBytes
        
        // When
        val result = biometricAuthManager.decryptWithBiometric(encryptedData)
        
        // Then
        assertArrayEquals(decryptedBytes, result)
    }
}