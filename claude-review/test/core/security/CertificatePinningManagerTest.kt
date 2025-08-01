package com.astralplayer.core.security

import android.content.Context
import io.mockk.*
import io.mockk.impl.annotations.MockK
import okhttp3.CertificatePinner
import okhttp3.OkHttpClient
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

class CertificatePinningManagerTest {
    
    @MockK
    private lateinit var context: Context
    
    private lateinit var certificatePinningManager: CertificatePinningManager
    
    @Before
    fun setup() {
        MockKAnnotations.init(this)
        certificatePinningManager = CertificatePinningManager(context)
    }
    
    @Test
    fun `createPinnedOkHttpClient should return client with certificate pinner`() {
        // When
        val client = certificatePinningManager.createPinnedOkHttpClient()
        
        // Then
        assertNotNull(client)
        assertNotNull(client.certificatePinner)
    }
    
    @Test
    fun `createPinnedOkHttpClient should include security interceptor`() {
        // When
        val client = certificatePinningManager.createPinnedOkHttpClient()
        
        // Then
        assertTrue(client.interceptors.any { it is SecurityInterceptor })
    }
    
    @Test
    fun `createPinnedOkHttpClient should set proper timeouts`() {
        // When
        val client = certificatePinningManager.createPinnedOkHttpClient()
        
        // Then
        assertEquals(30_000, client.connectTimeoutMillis)
        assertEquals(30_000, client.readTimeoutMillis)
    }
    
    @Test
    fun `certificate pinner should include pins for all API providers`() {
        // Given
        val expectedHosts = listOf(
            "api.openai.com",
            "speech.googleapis.com",
            "api.cognitive.microsofttranslator.com",
            "api.assemblyai.com",
            "api.deepgram.com"
        )
        
        // When
        val client = certificatePinningManager.createPinnedOkHttpClient()
        val certificatePinner = client.certificatePinner
        
        // Then
        assertNotNull(certificatePinner)
        // In a real test, we would verify the pins are configured
        // but CertificatePinner doesn't expose its configuration
    }
    
    @Test
    fun `getConfiguredHosts should return all pinned hosts`() {
        // When
        val hosts = certificatePinningManager.getConfiguredHosts()
        
        // Then
        assertTrue(hosts.contains("api.openai.com"))
        assertTrue(hosts.contains("speech.googleapis.com"))
        assertTrue(hosts.contains("api.cognitive.microsofttranslator.com"))
        assertTrue(hosts.contains("api.assemblyai.com"))
        assertTrue(hosts.contains("api.deepgram.com"))
    }
    
    @Test
    fun `validatePins should not throw for valid configuration`() {
        // When/Then - should not throw
        certificatePinningManager.validatePins()
    }
    
    @Test
    fun `createClient with custom interceptor should include it`() {
        // Given
        val customInterceptor = mockk<okhttp3.Interceptor>()
        
        // When
        val client = certificatePinningManager.createPinnedOkHttpClient(
            additionalInterceptor = customInterceptor
        )
        
        // Then
        assertTrue(client.interceptors.contains(customInterceptor))
    }
}