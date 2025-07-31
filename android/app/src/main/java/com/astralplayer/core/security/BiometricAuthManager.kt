package com.astralplayer.core.security

import android.content.Context
import android.os.Build
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import javax.crypto.Cipher
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Enhanced biometric authentication manager
 * Implements SecurityAgent requirements for biometric security
 */
@Singleton
class BiometricAuthManager @Inject constructor(
    private val context: Context,
    private val securityManager: SecurityManager
) {
    
    sealed class AuthResult {
        object Success : AuthResult()
        data class Error(val errorCode: Int, val message: String) : AuthResult()
        object Cancelled : AuthResult()
        object NotAvailable : AuthResult()
    }
    
    private val authResultChannel = Channel<AuthResult>()
    
    /**
     * Check if biometric authentication is available and configured
     */
    fun isBiometricAvailable(): BiometricAvailability {
        val biometricManager = BiometricManager.from(context)
        
        return when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)) {
            BiometricManager.BIOMETRIC_SUCCESS -> 
                BiometricAvailability.Available
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> 
                BiometricAvailability.NoHardware
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> 
                BiometricAvailability.HardwareUnavailable
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> 
                BiometricAvailability.NotEnrolled
            BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED -> 
                BiometricAvailability.SecurityUpdateRequired
            else -> BiometricAvailability.Unknown
        }
    }
    
    /**
     * Authenticate user with biometrics for sensitive operations
     */
    suspend fun authenticate(
        activity: FragmentActivity,
        title: String = "Authenticate to access secure content",
        subtitle: String = "Use your biometric credentials",
        description: String? = null,
        requireStrongBiometric: Boolean = true
    ): Flow<AuthResult> {
        val executor = ContextCompat.getMainExecutor(context)
        
        val biometricPrompt = BiometricPrompt(activity, executor, 
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    authResultChannel.trySend(AuthResult.Success)
                    
                    // Update security timestamp
                    securityManager.updateLastAuthTime()
                }
                
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    
                    when (errorCode) {
                        BiometricPrompt.ERROR_USER_CANCELED,
                        BiometricPrompt.ERROR_NEGATIVE_BUTTON -> {
                            authResultChannel.trySend(AuthResult.Cancelled)
                        }
                        else -> {
                            authResultChannel.trySend(
                                AuthResult.Error(errorCode, errString.toString())
                            )
                        }
                    }
                }
                
                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    // Don't send error here, let user retry
                }
            }
        )
        
        val authenticators = if (requireStrongBiometric) {
            BiometricManager.Authenticators.BIOMETRIC_STRONG
        } else {
            BiometricManager.Authenticators.BIOMETRIC_WEAK or 
            BiometricManager.Authenticators.DEVICE_CREDENTIAL
        }
        
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .apply { 
                description?.let { setDescription(it) }
            }
            .setAllowedAuthenticators(authenticators)
            .apply {
                // Only set negative button if device credential is not allowed
                if (authenticators and BiometricManager.Authenticators.DEVICE_CREDENTIAL == 0) {
                    setNegativeButtonText("Cancel")
                }
            }
            .build()
        
        biometricPrompt.authenticate(promptInfo)
        
        return authResultChannel.receiveAsFlow()
    }
    
    /**
     * Authenticate with crypto object for enhanced security
     */
    suspend fun authenticateWithCrypto(
        activity: FragmentActivity,
        cipher: Cipher,
        title: String = "Authenticate to decrypt secure data"
    ): Flow<AuthResult> {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            authResultChannel.trySend(AuthResult.NotAvailable)
            return authResultChannel.receiveAsFlow()
        }
        
        val executor = ContextCompat.getMainExecutor(context)
        
        val biometricPrompt = BiometricPrompt(activity, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    
                    // Crypto object is now authenticated
                    result.cryptoObject?.let {
                        authResultChannel.trySend(AuthResult.Success)
                        securityManager.updateLastAuthTime()
                    } ?: authResultChannel.trySend(
                        AuthResult.Error(-1, "Crypto object not available")
                    )
                }
                
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    authResultChannel.trySend(
                        AuthResult.Error(errorCode, errString.toString())
                    )
                }
                
                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                }
            }
        )
        
        val cryptoObject = BiometricPrompt.CryptoObject(cipher)
        
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle("Use your biometric to decrypt")
            .setNegativeButtonText("Cancel")
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
            .build()
        
        biometricPrompt.authenticate(promptInfo, cryptoObject)
        
        return authResultChannel.receiveAsFlow()
    }
    
    /**
     * Check if biometric authentication is required based on timeout
     */
    fun isAuthenticationRequired(): Boolean {
        if (!securityManager.isBiometricEnabled()) {
            return false
        }
        
        val lastAuthTime = securityManager.getLastAuthTime()
        val currentTime = System.currentTimeMillis()
        val timeoutMillis = securityManager.getBiometricTimeout() * 60 * 1000L
        
        return (currentTime - lastAuthTime) > timeoutMillis
    }
    
    /**
     * Enable or disable biometric authentication
     */
    fun setBiometricEnabled(enabled: Boolean) {
        securityManager.setBiometricEnabled(enabled)
    }
    
    /**
     * Set biometric timeout in minutes
     */
    fun setBiometricTimeout(minutes: Int) {
        securityManager.setBiometricTimeout(minutes)
    }
    
    enum class BiometricAvailability {
        Available,
        NoHardware,
        HardwareUnavailable,
        NotEnrolled,
        SecurityUpdateRequired,
        Unknown
    }
}