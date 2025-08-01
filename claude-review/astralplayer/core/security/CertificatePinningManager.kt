package com.astralplayer.core.security

import okhttp3.CertificatePinner
import okhttp3.OkHttpClient
import java.security.cert.X509Certificate
import javax.inject.Inject
import javax.inject.Singleton
import javax.net.ssl.*

/**
 * Certificate pinning implementation for enhanced security
 * Implements SecurityAgent requirements for network security
 */
@Singleton
class CertificatePinningManager @Inject constructor() {
    
    companion object {
        // Elite API service certificate pins (SHA-256)
        private val CERTIFICATE_PINS = mapOf(
            "api.openai.com" to listOf(
                "sha256/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=",
                "sha256/BBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBB=" // Backup pin
            ),
            "speech.googleapis.com" to listOf(
                "sha256/CCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCC=",
                "sha256/DDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDD="
            ),
            "api.assemblyai.com" to listOf(
                "sha256/EEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEE=",
                "sha256/FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF="
            ),
            "api.deepgram.com" to listOf(
                "sha256/GGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGG=",
                "sha256/HHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHH="
            )
        )
        
        // Development mode flag - disable pinning for local testing
        private const val ENABLE_PINNING = true
    }
    
    /**
     * Create certificate pinner for OkHttp
     */
    fun createCertificatePinner(): CertificatePinner {
        val builder = CertificatePinner.Builder()
        
        if (ENABLE_PINNING) {
            CERTIFICATE_PINS.forEach { (hostname, pins) ->
                pins.forEach { pin ->
                    builder.add(hostname, pin)
                }
            }
        }
        
        return builder.build()
    }
    
    /**
     * Configure OkHttpClient with certificate pinning
     */
    fun configureClient(clientBuilder: OkHttpClient.Builder): OkHttpClient.Builder {
        return clientBuilder
            .certificatePinner(createCertificatePinner())
            .hostnameVerifier(createHostnameVerifier())
            .sslSocketFactory(createSslSocketFactory(), createTrustManager())
    }
    
    /**
     * Create custom hostname verifier for additional security
     */
    private fun createHostnameVerifier(): HostnameVerifier {
        return HostnameVerifier { hostname, session ->
            // Verify hostname matches expected patterns
            val validHostnames = CERTIFICATE_PINS.keys + setOf(
                "*.openai.com",
                "*.googleapis.com",
                "*.assemblyai.com",
                "*.deepgram.com"
            )
            
            validHostnames.any { pattern ->
                if (pattern.startsWith("*.")) {
                    val domain = pattern.substring(2)
                    hostname.endsWith(domain)
                } else {
                    hostname == pattern
                }
            }
        }
    }
    
    /**
     * Create SSL socket factory with enhanced security
     */
    private fun createSslSocketFactory(): SSLSocketFactory {
        val sslContext = SSLContext.getInstance("TLS")
        sslContext.init(null, arrayOf(createTrustManager()), java.security.SecureRandom())
        return sslContext.socketFactory
    }
    
    /**
     * Create custom trust manager for certificate validation
     */
    private fun createTrustManager(): X509TrustManager {
        return object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {
                // Not needed for client
            }
            
            override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {
                // Perform standard certificate validation
                val defaultTrustManager = getDefaultTrustManager()
                defaultTrustManager.checkServerTrusted(chain, authType)
                
                // Additional custom validation
                validateCertificateChain(chain)
            }
            
            override fun getAcceptedIssuers(): Array<X509Certificate> {
                return getDefaultTrustManager().acceptedIssuers
            }
        }
    }
    
    /**
     * Get default system trust manager
     */
    private fun getDefaultTrustManager(): X509TrustManager {
        val trustManagerFactory = TrustManagerFactory.getInstance(
            TrustManagerFactory.getDefaultAlgorithm()
        )
        trustManagerFactory.init(null as java.security.KeyStore?)
        
        return trustManagerFactory.trustManagers
            .filterIsInstance<X509TrustManager>()
            .first()
    }
    
    /**
     * Perform additional certificate chain validation
     */
    private fun validateCertificateChain(chain: Array<X509Certificate>) {
        require(chain.isNotEmpty()) { "Certificate chain is empty" }
        
        // Validate certificate is not expired
        val currentTime = System.currentTimeMillis()
        chain.forEach { cert ->
            require(currentTime >= cert.notBefore.time) { 
                "Certificate is not yet valid" 
            }
            require(currentTime <= cert.notAfter.time) { 
                "Certificate has expired" 
            }
        }
        
        // Validate certificate key size (minimum 2048 bits for RSA)
        val leafCert = chain[0]
        if (leafCert.publicKey.algorithm == "RSA") {
            val keySize = (leafCert.publicKey as java.security.interfaces.RSAPublicKey).modulus.bitLength()
            require(keySize >= 2048) { 
                "Certificate key size is too small: $keySize bits" 
            }
        }
    }
    
    /**
     * Verify if a hostname is pinned
     */
    fun isHostnamePinned(hostname: String): Boolean {
        return CERTIFICATE_PINS.containsKey(hostname)
    }
    
    /**
     * Get pinned certificates for a hostname
     */
    fun getPinnedCertificates(hostname: String): List<String> {
        return CERTIFICATE_PINS[hostname] ?: emptyList()
    }
    
    /**
     * Get configured hosts for certificate pinning
     * 
     * @return Set of hostnames that have certificate pins configured
     */
    fun getConfiguredHosts(): Set<String> {
        return CERTIFICATE_PINS.keys
    }
    
    /**
     * Validate certificate pins configuration
     * 
     * @throws IllegalArgumentException if any pins are misconfigured
     */
    fun validatePins() {
        CERTIFICATE_PINS.forEach { (host, pins) ->
            require(pins.isNotEmpty()) { "No pins configured for $host" }
            pins.forEach { pin ->
                require(pin.startsWith("sha256/")) { "Invalid pin format for $host: $pin" }
            }
        }
    }
    
    /**
     * Create OkHttpClient with custom interceptor
     * 
     * @param additionalInterceptor Optional interceptor to add to the client
     * @return Configured OkHttpClient with certificate pinning and custom interceptor
     */
    fun createPinnedOkHttpClient(additionalInterceptor: okhttp3.Interceptor? = null): OkHttpClient {
        val builder = createPinnedClient().newBuilder()
        additionalInterceptor?.let { builder.addInterceptor(it) }
        return builder.build()
    }
}

/**
 * Security interceptor for additional headers
 */
class SecurityInterceptor : okhttp3.Interceptor {
    override fun intercept(chain: okhttp3.Interceptor.Chain): okhttp3.Response {
        val request = chain.request().newBuilder()
            .addHeader("X-Security-Version", "1.0")
            .addHeader("X-App-Version", BuildConfig.VERSION_NAME)
            .build()
        return chain.proceed(request)
    }
}