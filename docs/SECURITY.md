# 🔒 AstralStream Security Documentation

## Overview

AstralStream Elite implements enterprise-grade security features to protect user data, ensure secure API communications, and maintain application integrity. This document outlines our security architecture, best practices, and implementation guidelines.

## Security Architecture

### Defense in Depth

We implement multiple layers of security:

```
┌─────────────────────────────────────────┐
│         Application Layer               │
│    (Obfuscation, Anti-tampering)        │
├─────────────────────────────────────────┤
│         Authentication Layer            │
│    (Biometric, Device Credentials)      │
├─────────────────────────────────────────┤
│         Data Layer                      │
│    (AES-256 Encryption, Keystore)       │
├─────────────────────────────────────────┤
│         Network Layer                   │
│    (Certificate Pinning, TLS 1.3)       │
└─────────────────────────────────────────┘
```

## Network Security

### Certificate Pinning

Certificate pinning prevents man-in-the-middle attacks by validating server certificates against known pins.

#### Implementation

```kotlin
class CertificatePinningManager @Inject constructor(
    private val context: Context
) {
    companion object {
        // Certificate pins for API providers
        private val PINS = mapOf(
            "api.openai.com" to listOf(
                "sha256/XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX=",
                "sha256/YYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYY=" // Backup pin
            ),
            "speech.googleapis.com" to listOf(
                "sha256/ZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZ="
            )
        )
    }
    
    fun createPinnedOkHttpClient(): OkHttpClient {
        val certificatePinner = CertificatePinner.Builder().apply {
            PINS.forEach { (hostname, pins) ->
                pins.forEach { pin ->
                    add(hostname, pin)
                }
            }
        }.build()
        
        return OkHttpClient.Builder()
            .certificatePinner(certificatePinner)
            .addInterceptor(SecurityInterceptor())
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }
}
```

#### Updating Pins

1. Monitor certificate expiration dates
2. Test new pins in staging environment
3. Deploy with both old and new pins during transition
4. Remove old pins after verification

### TLS Configuration

```kotlin
class TLSSocketFactory : SSLSocketFactory() {
    
    override fun createSocket(socket: Socket, host: String, port: Int, autoClose: Boolean): Socket {
        return configureSocket(super.createSocket(socket, host, port, autoClose))
    }
    
    private fun configureSocket(socket: Socket): Socket {
        if (socket is SSLSocket) {
            // Enable only TLS 1.2 and 1.3
            socket.enabledProtocols = arrayOf("TLSv1.2", "TLSv1.3")
            
            // Configure cipher suites
            socket.enabledCipherSuites = getSupportedCipherSuites()
        }
        return socket
    }
    
    private fun getSupportedCipherSuites(): Array<String> {
        return arrayOf(
            "TLS_AES_128_GCM_SHA256",
            "TLS_AES_256_GCM_SHA384",
            "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256",
            "TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384"
        )
    }
}
```

## Authentication

### Biometric Authentication

Enhanced biometric authentication with crypto object support:

```kotlin
class BiometricAuthManager @Inject constructor(
    private val context: Context
) {
    private val keyAlias = "AstralStreamBiometricKey"
    private val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
    
    fun authenticateForSensitiveContent(
        activity: FragmentActivity,
        onSuccess: (CryptoObject?) -> Unit,
        onError: (BiometricError) -> Unit
    ) {
        val cryptoObject = createCryptoObject()
        
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Authenticate to access content")
            .setSubtitle("Use your biometric credential")
            .setNegativeButtonText("Cancel")
            .setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or
                BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )
            .build()
        
        val biometricPrompt = BiometricPrompt(
            activity,
            ContextCompat.getMainExecutor(context),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    onSuccess(result.cryptoObject)
                }
                
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    onError(BiometricError(errorCode, errString.toString()))
                }
            }
        )
        
        if (cryptoObject != null) {
            biometricPrompt.authenticate(promptInfo, cryptoObject)
        } else {
            biometricPrompt.authenticate(promptInfo)
        }
    }
    
    private fun createCryptoObject(): BiometricPrompt.CryptoObject? {
        return try {
            val cipher = getCipher()
            val secretKey = getOrCreateSecretKey()
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)
            BiometricPrompt.CryptoObject(cipher)
        } catch (e: Exception) {
            Timber.e(e, "Failed to create crypto object")
            null
        }
    }
    
    private fun getOrCreateSecretKey(): SecretKey {
        return if (keyStore.containsAlias(keyAlias)) {
            (keyStore.getEntry(keyAlias, null) as KeyStore.SecretKeyEntry).secretKey
        } else {
            createSecretKey()
        }
    }
    
    private fun createSecretKey(): SecretKey {
        val keyGenParams = KeyGenParameterSpec.Builder(
            keyAlias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .setUserAuthenticationRequired(true)
            .setUserAuthenticationValidityDurationSeconds(30)
            .build()
        
        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        keyGenerator.init(keyGenParams)
        return keyGenerator.generateKey()
    }
}
```

## Data Encryption

### AES-256 Encryption

Secure data storage using hardware-backed encryption:

```kotlin
class EncryptionManager @Inject constructor(
    private val context: Context
) {
    private val masterKeyAlias = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .setUserAuthenticationRequired(true)
        .setUserAuthenticationValidityDurationSeconds(300)
        .build()
    
    fun encryptSensitiveData(data: ByteArray): EncryptedData {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, getSecretKey())
        
        val iv = cipher.iv
        val encryptedBytes = cipher.doFinal(data)
        
        return EncryptedData(
            encryptedBytes = encryptedBytes,
            iv = iv,
            keyAlias = masterKeyAlias.toString()
        )
    }
    
    fun decryptSensitiveData(encryptedData: EncryptedData): ByteArray {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val spec = GCMParameterSpec(128, encryptedData.iv)
        cipher.init(Cipher.DECRYPT_MODE, getSecretKey(), spec)
        
        return cipher.doFinal(encryptedData.encryptedBytes)
    }
    
    private fun getSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore")
        keyStore.load(null)
        return keyStore.getKey(masterKeyAlias.toString(), null) as SecretKey
    }
}

data class EncryptedData(
    val encryptedBytes: ByteArray,
    val iv: ByteArray,
    val keyAlias: String
)
```

### Encrypted SharedPreferences

```kotlin
class SecurePreferencesManager @Inject constructor(
    private val context: Context
) {
    private val encryptedPrefs = EncryptedSharedPreferences.create(
        context,
        "secure_prefs",
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
    
    fun storeApiKey(provider: String, apiKey: String) {
        encryptedPrefs.edit().putString("api_key_$provider", apiKey).apply()
    }
    
    fun getApiKey(provider: String): String? {
        return encryptedPrefs.getString("api_key_$provider", null)
    }
    
    fun clearAllSecureData() {
        encryptedPrefs.edit().clear().apply()
    }
}
```

## Code Obfuscation

### ProGuard/R8 Configuration

```proguard
# AstralStream Elite ProGuard Rules

# Keep security-critical classes
-keep class com.astralplayer.core.security.** { *; }
-keep class com.astralplayer.core.crypto.** { *; }

# Obfuscate but keep method names for stack traces
-keepattributes SourceFile,LineNumberTable

# Aggressive optimization
-optimizationpasses 5
-allowaccessmodification
-repackageclasses ''

# Remove logging in release
-assumenosideeffects class timber.log.Timber {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}

# Encrypt strings
-obfuscate
-adaptresourcefilenames
-adaptresourcefilecontents

# Keep AI provider interfaces
-keep interface com.astralplayer.ai.** { *; }

# Security checks
-keep class com.astralplayer.core.security.IntegrityChecker { *; }
-keep class com.astralplayer.core.security.RootDetector { *; }
```

### String Encryption

```kotlin
@StringEncryption
object SecureStrings {
    const val OPENAI_BASE_URL = "https://api.openai.com/v1/"
    const val GOOGLE_SPEECH_URL = "https://speech.googleapis.com/"
    
    fun getDecryptedUrl(key: String): String {
        return StringDecryptor.decrypt(key)
    }
}
```

## Anti-Tampering

### App Integrity Verification

```kotlin
class IntegrityChecker @Inject constructor(
    private val context: Context
) {
    
    fun verifyAppIntegrity(): Boolean {
        return checkSignature() && 
               checkPackageName() && 
               checkInstallerSource() &&
               !isDebuggable() &&
               !isRunningInEmulator()
    }
    
    private fun checkSignature(): Boolean {
        val expectedSignature = "SHA256:XX:XX:XX:XX:XX:XX:XX:XX"
        val packageInfo = context.packageManager.getPackageInfo(
            context.packageName,
            PackageManager.GET_SIGNATURES
        )
        
        val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            packageInfo.signingInfo.apkContentsSigners
        } else {
            packageInfo.signatures
        }
        
        return signatures.any { signature ->
            val sha256 = MessageDigest.getInstance("SHA256")
            val digest = sha256.digest(signature.toByteArray())
            val currentSignature = digest.joinToString(":") { "%02X".format(it) }
            currentSignature == expectedSignature
        }
    }
    
    private fun checkPackageName(): Boolean {
        return context.packageName == "com.astralplayer.astralstream"
    }
    
    private fun checkInstallerSource(): Boolean {
        val validInstallers = setOf(
            "com.android.vending", // Google Play
            "com.google.android.packageinstaller"
        )
        val installer = context.packageManager.getInstallerPackageName(context.packageName)
        return installer in validInstallers
    }
    
    private fun isDebuggable(): Boolean {
        return context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
    }
    
    private fun isRunningInEmulator(): Boolean {
        return Build.FINGERPRINT.contains("generic") ||
               Build.MODEL.contains("google_sdk") ||
               Build.MODEL.contains("Emulator")
    }
}
```

### Root Detection

```kotlin
class RootDetector @Inject constructor() {
    
    fun isDeviceRooted(): Boolean {
        return checkRootBinaries() || 
               checkSuExists() || 
               checkBusyBox() ||
               checkRootPackages() ||
               checkRWSystem()
    }
    
    private fun checkRootBinaries(): Boolean {
        val paths = arrayOf(
            "/system/bin/", "/system/xbin/", "/sbin/",
            "/system/sd/xbin/", "/system/bin/failsafe/",
            "/data/local/xbin/", "/data/local/bin/",
            "/data/local/"
        )
        
        val binaries = arrayOf("su", "busybox", "supersu", "magisk")
        
        return paths.any { path ->
            binaries.any { binary ->
                File(path + binary).exists()
            }
        }
    }
    
    private fun checkSuExists(): Boolean {
        return try {
            Runtime.getRuntime().exec("which su")
            true
        } catch (e: Exception) {
            false
        }
    }
    
    private fun checkRootPackages(): Boolean {
        val rootPackages = listOf(
            "com.koushikdutta.superuser",
            "com.topjohnwu.magisk",
            "eu.chainfire.supersu",
            "com.noshufou.android.su"
        )
        
        return rootPackages.any { pkg ->
            isPackageInstalled(pkg)
        }
    }
    
    private fun isPackageInstalled(packageName: String): Boolean {
        return try {
            Runtime.getRuntime().exec("pm list packages $packageName")
            true
        } catch (e: Exception) {
            false
        }
    }
}
```

## Secure API Key Management

### BuildConfig-based Keys

```kotlin
// build.gradle.kts
android {
    buildTypes {
        getByName("release") {
            buildConfigField("String", "OPENAI_API_KEY", "\"${getApiKey("OPENAI_API_KEY")}\"")
            buildConfigField("String", "GOOGLE_API_KEY", "\"${getApiKey("GOOGLE_API_KEY")}\"")
        }
    }
}

fun getApiKey(key: String): String {
    return project.findProperty(key)?.toString() ?: System.getenv(key) ?: ""
}
```

### Runtime Key Rotation

```kotlin
class ApiKeyManager @Inject constructor(
    private val securePrefs: SecurePreferencesManager,
    private val networkManager: NetworkManager
) {
    
    suspend fun getValidApiKey(provider: AIProvider): String? {
        val cachedKey = securePrefs.getApiKey(provider.name)
        
        if (cachedKey != null && isKeyValid(cachedKey, provider)) {
            return cachedKey
        }
        
        // Fetch new key from secure endpoint
        return fetchNewApiKey(provider)
    }
    
    private suspend fun fetchNewApiKey(provider: AIProvider): String? {
        return try {
            val response = networkManager.secureApiCall {
                getApiKeyFromServer(provider)
            }
            
            response?.let { newKey ->
                securePrefs.storeApiKey(provider.name, newKey)
                newKey
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to fetch API key")
            null
        }
    }
    
    private suspend fun isKeyValid(key: String, provider: AIProvider): Boolean {
        // Implement key validation logic
        return true
    }
}
```

## Security Headers

### Network Security Config

```xml
<?xml version="1.0" encoding="utf-8"?>
<network-security-config>
    <!-- Pin certificates for specific domains -->
    <domain-config cleartextTrafficPermitted="false">
        <domain includeSubdomains="true">api.openai.com</domain>
        <pin-set expiration="2025-01-01">
            <pin digest="SHA-256">XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX=</pin>
            <pin digest="SHA-256">YYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYY=</pin>
        </pin-set>
    </domain-config>
    
    <!-- Disable cleartext for all domains -->
    <base-config cleartextTrafficPermitted="false">
        <trust-anchors>
            <certificates src="system" />
        </trust-anchors>
    </base-config>
</network-security-config>
```

## Security Monitoring

### Runtime Security Checks

```kotlin
class SecurityMonitor @Inject constructor(
    private val integrityChecker: IntegrityChecker,
    private val rootDetector: RootDetector,
    private val context: Context
) {
    
    fun startMonitoring() {
        // Check app integrity
        if (!integrityChecker.verifyAppIntegrity()) {
            handleSecurityViolation("App integrity check failed")
        }
        
        // Check for root
        if (rootDetector.isDeviceRooted()) {
            handleSecurityWarning("Device appears to be rooted")
        }
        
        // Monitor for debugger
        if (Debug.isDebuggerConnected()) {
            handleSecurityViolation("Debugger detected")
        }
        
        // Check for hooks
        if (detectHooks()) {
            handleSecurityViolation("Runtime manipulation detected")
        }
    }
    
    private fun detectHooks(): Boolean {
        // Implement hook detection logic
        return false
    }
    
    private fun handleSecurityViolation(message: String) {
        Timber.e("SECURITY VIOLATION: $message")
        // Log to analytics
        // Optionally terminate app
    }
    
    private fun handleSecurityWarning(message: String) {
        Timber.w("SECURITY WARNING: $message")
        // Show warning to user
        // Log to analytics
    }
}
```

## Security Best Practices

### Development Guidelines

1. **Never commit sensitive data**
   - Use `.gitignore` for local properties
   - Store keys in CI/CD secrets
   - Use BuildConfig for API keys

2. **Validate all inputs**
   ```kotlin
   fun validateVideoUri(uri: String): Boolean {
       return uri.matches(Regex("^(content|file|https?)://.*"))
   }
   ```

3. **Use secure random generation**
   ```kotlin
   fun generateSecureToken(): String {
       val random = SecureRandom()
       val bytes = ByteArray(32)
       random.nextBytes(bytes)
       return Base64.encodeToString(bytes, Base64.NO_WRAP)
   }
   ```

4. **Implement rate limiting**
   ```kotlin
   class RateLimiter {
       private val attempts = mutableMapOf<String, AtomicInteger>()
       
       fun checkLimit(key: String, maxAttempts: Int = 5): Boolean {
           val count = attempts.getOrPut(key) { AtomicInteger(0) }
           return count.incrementAndGet() <= maxAttempts
       }
   }
   ```

### Security Checklist

- [ ] Certificate pinning implemented for all API calls
- [ ] Biometric authentication for sensitive features
- [ ] All sensitive data encrypted with AES-256
- [ ] ProGuard rules configured and tested
- [ ] Anti-tampering checks in place
- [ ] Root detection implemented
- [ ] Network security config properly configured
- [ ] API keys stored securely
- [ ] Input validation on all user inputs
- [ ] Security monitoring active

## Incident Response

### Security Incident Handling

1. **Detection**: Monitor for security events
2. **Containment**: Disable affected features
3. **Investigation**: Analyze logs and telemetry
4. **Remediation**: Deploy fixes
5. **Communication**: Notify affected users
6. **Review**: Update security measures

### Contact

For security concerns or vulnerability reports:
- Email: security@astralstream.dev
- PGP Key: [public key]

---

Last Updated: January 2025