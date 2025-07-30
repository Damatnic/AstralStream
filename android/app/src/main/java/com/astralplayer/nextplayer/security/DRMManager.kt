package com.astralplayer.nextplayer.security

import android.content.Context
import android.net.Uri
import android.util.Base64
import android.util.Log
import androidx.media3.common.DrmInitData
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.drm.*
import androidx.media3.exoplayer.upstream.DefaultHttpDataSource
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.net.URL
import java.security.SecureRandom
import java.util.concurrent.ConcurrentHashMap
import javax.crypto.KeyGenerator
import javax.crypto.spec.SecretKeySpec

/**
 * Advanced Digital Rights Management (DRM) system
 * Handles content protection, license management, and access control
 */
@UnstableApi
class DRMManager(private val context: Context) {
    
    private val _drmEvents = MutableSharedFlow<DRMEvent>()
    val drmEvents: SharedFlow<DRMEvent> = _drmEvents.asSharedFlow()
    
    private val _drmState = MutableStateFlow(DRMState())
    val drmState: StateFlow<DRMState> = _drmState.asStateFlow()
    
    private val drmScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var isInitialized = false
    
    private val activeLicenses = ConcurrentHashMap<String, DRMLicenseRecord>()
    private val drmSessions = ConcurrentHashMap<String, DRMSessionInfo>()
    private val protectedContent = ConcurrentHashMap<String, DRMProtectedContent>()
    private val licenseRequestQueue = mutableListOf<LicenseRequest>()
    
    private var drmSessionManager: DrmSessionManager? = null
    private val secureRandom = SecureRandom()
    
    companion object {
        private const val TAG = "DRMManager"
        private const val DEFAULT_LICENSE_DURATION = 86400000L // 24 hours
        private const val MAX_CONCURRENT_SESSIONS = 5
        private const val LICENSE_RENEWAL_THRESHOLD = 3600000L // 1 hour before expiry
    }
    
    /**
     * Initialize DRM manager with supported schemes
     */
    suspend fun initialize(supportedSchemes: Set<DRMScheme> = setOf(DRMScheme.WIDEVINE)) {
        isInitialized = true
        
        // Initialize DRM session manager
        initializeDrmSessionManager(supportedSchemes)
        
        // Start license monitoring
        startLicenseMonitoring()
        
        // Load existing licenses
        loadPersistedLicenses()
        
        _drmState.value = _drmState.value.copy(
            isInitialized = true,
            supportedSchemes = supportedSchemes
        )
        
        _drmEvents.emit(DRMEvent.Initialized(supportedSchemes))
        
        Log.d(TAG, "DRM Manager initialized with schemes: $supportedSchemes")
    }
    
    /**
     * Configure DRM for protected content
     */
    suspend fun configureDRM(
        contentUri: Uri,
        drmConfiguration: DRMConfiguration
    ): DRMConfigurationResult = withContext(Dispatchers.IO) {
        try {
            val contentId = generateContentId(contentUri)
            
            // Validate DRM configuration
            if (!validateDRMConfiguration(drmConfiguration)) {
                return@withContext DRMConfigurationResult.InvalidConfiguration(
                    "Invalid DRM configuration parameters"
                )
            }
            
            // Check if scheme is supported
            if (!_drmState.value.supportedSchemes.contains(drmConfiguration.provider)) {
                return@withContext DRMConfigurationResult.UnsupportedScheme(drmConfiguration.provider)
            }
            
            // Create DRM protected content entry
            val protectedContent = DRMProtectedContent(
                contentId = contentId,
                contentUri = contentUri,
                drmScheme = drmConfiguration.provider,
                licenseUrl = drmConfiguration.licenseUrl,
                keyId = generateKeyId(),
                protectionLevel = ContentProtectionLevel.DRM_PROTECTED,
                createdAt = System.currentTimeMillis()
            )
            
            this@DRMManager.protectedContent[contentId] = protectedContent
            
            // Update DRM state
            updateDRMMetrics(drmConfiguration.provider)
            
            _drmEvents.emit(
                DRMEvent.ContentConfigured(contentUri, drmConfiguration.provider)
            )
            
            DRMConfigurationResult.Success(protectedContent)
            
        } catch (e: Exception) {
            Log.e(TAG, "DRM configuration failed", e)
            _drmEvents.emit(DRMEvent.ConfigurationFailed(contentUri, e))
            DRMConfigurationResult.Error(e)
        }
    }
    
    /**
     * Acquire DRM license for protected content
     */
    suspend fun acquireLicense(
        contentUri: Uri,
        licenseChallenge: ByteArray? = null
    ): LicenseAcquisitionResult = withContext(Dispatchers.IO) {
        try {
            val contentId = generateContentId(contentUri)
            val protectedContent = this@DRMManager.protectedContent[contentId]
                ?: return@withContext LicenseAcquisitionResult.ContentNotProtected
            
            // Check if valid license already exists
            val existingLicense = activeLicenses[contentId]
            if (existingLicense != null && !existingLicense.isExpired()) {
                return@withContext LicenseAcquisitionResult.ExistingLicense(existingLicense)
            }
            
            // Generate license request
            val licenseRequest = createLicenseRequest(protectedContent, licenseChallenge)
            
            // Process license acquisition
            val licenseResponse = processLicenseRequest(licenseRequest)
            
            if (licenseResponse.success) {
                val license = createDRMLicense(protectedContent, licenseResponse)
                activeLicenses[contentId] = license
                
                // Create DRM session
                val session = createDRMSession(protectedContent, license)
                drmSessions[session.sessionId] = session
                
                _drmEvents.emit(
                    DRMEvent.LicenseAcquired(contentUri, license.licenseId)
                )
                
                LicenseAcquisitionResult.Success(license)
            } else {
                LicenseAcquisitionResult.LicenseServerError(licenseResponse.errorMessage)
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "License acquisition failed", e)
            _drmEvents.emit(DRMEvent.LicenseAcquisitionFailed(contentUri, e))
            LicenseAcquisitionResult.Error(e)
        }
    }
    
    /**
     * Validate DRM license for content access
     */
    suspend fun validateLicense(
        contentUri: Uri,
        requestedPermissions: Set<DRMPermission>
    ): LicenseValidationResult = withContext(Dispatchers.IO) {
        try {
            val contentId = generateContentId(contentUri)
            val license = activeLicenses[contentId]
                ?: return@withContext LicenseValidationResult.NoLicense
            
            // Check license expiry
            if (license.isExpired()) {
                activeLicenses.remove(contentId)
                return@withContext LicenseValidationResult.LicenseExpired
            }
            
            // Check permission constraints
            val hasPermissions = requestedPermissions.all { permission ->
                license.permissions.contains(permission)
            }
            
            if (!hasPermissions) {
                return@withContext LicenseValidationResult.InsufficientPermissions(
                    requestedPermissions - license.permissions
                )
            }
            
            // Check usage restrictions
            val restrictionCheck = validateUsageRestrictions(license, requestedPermissions)
            if (!restrictionCheck.valid) {
                return@withContext LicenseValidationResult.UsageRestrictionViolation(
                    restrictionCheck.violation
                )
            }
            
            // Update usage tracking
            updateLicenseUsage(license, requestedPermissions)
            
            LicenseValidationResult.Valid(license)
            
        } catch (e: Exception) {
            Log.e(TAG, "License validation failed", e)
            LicenseValidationResult.Error(e)
        }
    }
    
    /**
     * Renew expiring DRM license
     */
    suspend fun renewLicense(contentUri: Uri): LicenseRenewalResult = withContext(Dispatchers.IO) {
        try {
            val contentId = generateContentId(contentUri)
            val currentLicense = activeLicenses[contentId]
                ?: return@withContext LicenseRenewalResult.NoExistingLicense
            
            val protectedContent = this@DRMManager.protectedContent[contentId]
                ?: return@withContext LicenseRenewalResult.ContentNotFound
            
            // Create renewal request
            val renewalRequest = createLicenseRenewalRequest(currentLicense, protectedContent)
            val renewalResponse = processLicenseRequest(renewalRequest)
            
            if (renewalResponse.success) {
                val renewedLicense = createDRMLicense(protectedContent, renewalResponse)
                    .copy(renewalCount = currentLicense.renewalCount + 1)
                
                activeLicenses[contentId] = renewedLicense
                
                _drmEvents.emit(
                    DRMEvent.LicenseRenewed(contentUri, renewedLicense.licenseId)
                )
                
                LicenseRenewalResult.Success(renewedLicense)
            } else {
                LicenseRenewalResult.RenewalFailed(renewalResponse.errorMessage)
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "License renewal failed", e)
            LicenseRenewalResult.Error(e)
        }
    }
    
    /**
     * Revoke DRM license
     */
    suspend fun revokeLicense(
        contentUri: Uri,
        reason: String = "User request"
    ): LicenseRevocationResult = withContext(Dispatchers.IO) {
        try {
            val contentId = generateContentId(contentUri)
            val license = activeLicenses.remove(contentId)
                ?: return@withContext LicenseRevocationResult.NoLicense
            
            // Close associated DRM sessions
            val associatedSessions = drmSessions.values.filter { it.contentId == contentId }
            associatedSessions.forEach { session ->
                closeDRMSession(session.sessionId)
            }
            
            // Notify license server of revocation
            notifyLicenseRevocation(license, reason)
            
            _drmEvents.emit(
                DRMEvent.LicenseRevoked(contentUri, license.licenseId, reason)
            )
            
            LicenseRevocationResult.Success(license.licenseId)
            
        } catch (e: Exception) {
            Log.e(TAG, "License revocation failed", e)
            LicenseRevocationResult.Error(e)
        }
    }
    
    /**
     * Create DRM session for protected playback
     */
    suspend fun createSession(
        contentUri: Uri,
        sessionType: DRMSessionType = DRMSessionType.STREAMING
    ): DRMSessionResult = withContext(Dispatchers.IO) {
        try {
            val contentId = generateContentId(contentUri)
            val license = activeLicenses[contentId]
                ?: return@withContext DRMSessionResult.NoValidLicense
            
            // Check session limits
            if (drmSessions.size >= MAX_CONCURRENT_SESSIONS) {
                cleanupExpiredSessions()
                if (drmSessions.size >= MAX_CONCURRENT_SESSIONS) {
                    return@withContext DRMSessionResult.SessionLimitExceeded
                }
            }
            
            val protectedContent = this@DRMManager.protectedContent[contentId]
                ?: return@withContext DRMSessionResult.ContentNotFound
            
            val session = createDRMSession(protectedContent, license, sessionType)
            drmSessions[session.sessionId] = session
            
            _drmEvents.emit(
                DRMEvent.SessionCreated(contentUri, session.sessionId)
            )
            
            DRMSessionResult.Success(session)
            
        } catch (e: Exception) {
            Log.e(TAG, "DRM session creation failed", e)
            DRMSessionResult.Error(e)
        }
    }
    
    /**
     * Get DRM metrics and status information
     */
    fun getDRMMetrics(): DRMMetrics {
        val currentTime = System.currentTimeMillis()
        
        val activeLicenseCount = activeLicenses.values.count { !it.isExpired() }
        val expiredLicenseCount = activeLicenses.values.count { it.isExpired() }
        val activeSessionCount = drmSessions.values.count { !it.isExpired(currentTime) }
        
        val licensesByScheme = activeLicenses.values
            .groupBy { it.drmScheme }
            .mapValues { it.value.size }
        
        return DRMMetrics(
            activeLicenses = activeLicenseCount,
            expiredLicenses = expiredLicenseCount,
            activeSessions = activeSessionCount,
            totalProtectedContent = protectedContent.size,
            licensesByScheme = licensesByScheme,
            averageLicenseDuration = calculateAverageLicenseDuration(),
            successfulAcquisitions = _drmState.value.metrics.successfulAcquisitions,
            failedAcquisitions = _drmState.value.metrics.failedAcquisitions
        )
    }
    
    // Private implementation methods
    private fun initializeDrmSessionManager(supportedSchemes: Set<DRMScheme>) {
        try {
            val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            
            when {
                supportedSchemes.contains(DRMScheme.WIDEVINE) -> {
                    drmSessionManager = DefaultDrmSessionManager.Builder()
                        .setUuidAndExoMediaDrmProvider(
                            androidx.media3.common.C.WIDEVINE_UUID,
                            androidx.media3.exoplayer.drm.FrameworkMediaDrm.DEFAULT_PROVIDER
                        )
                        .setMultiSession(true)
                        .build(httpDataSourceFactory)
                }
                supportedSchemes.contains(DRMScheme.PLAYREADY) -> {
                    drmSessionManager = DefaultDrmSessionManager.Builder()
                        .setUuidAndExoMediaDrmProvider(
                            androidx.media3.common.C.PLAYREADY_UUID,
                            androidx.media3.exoplayer.drm.FrameworkMediaDrm.DEFAULT_PROVIDER
                        )
                        .build(httpDataSourceFactory)
                }
                // Add other DRM schemes as needed
            }
            
            Log.d(TAG, "DRM session manager initialized")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize DRM session manager", e)
        }
    }
    
    private fun startLicenseMonitoring() {
        drmScope.launch {
            while (isInitialized && currentCoroutineContext().isActive) {
                try {
                    monitorLicenseExpiry()
                    cleanupExpiredSessions()
                    processLicenseRenewalQueue()
                    
                    delay(60000) // Check every minute
                } catch (e: Exception) {
                    Log.e(TAG, "License monitoring error", e)
                }
            }
        }
    }
    
    private suspend fun loadPersistedLicenses() {
        // Implementation would load licenses from secure storage
        Log.d(TAG, "Persisted licenses loaded")
    }
    
    private fun validateDRMConfiguration(config: DRMConfiguration): Boolean {
        return config.licenseUrl?.isNotEmpty() == true &&
               config.provider != null
    }
    
    private fun generateContentId(contentUri: Uri): String {
        val digest = java.security.MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(contentUri.toString().toByteArray())
        return hash.joinToString("") { "%02x".format(it) }
    }
    
    private fun generateKeyId(): String {
        val keyBytes = ByteArray(16)
        secureRandom.nextBytes(keyBytes)
        return Base64.encodeToString(keyBytes, Base64.NO_WRAP)
    }
    
    private fun createLicenseRequest(
        protectedContent: DRMProtectedContent,
        challenge: ByteArray?
    ): LicenseRequest {
        return LicenseRequest(
            contentId = protectedContent.contentId,
            keyId = protectedContent.keyId,
            licenseUrl = protectedContent.licenseUrl ?: "",
            drmScheme = protectedContent.drmScheme,
            challenge = challenge ?: generateChallenge(),
            requestTime = System.currentTimeMillis()
        )
    }
    
    private fun createLicenseRenewalRequest(
        currentLicense: DRMLicenseRecord,
        protectedContent: DRMProtectedContent
    ): LicenseRequest {
        return LicenseRequest(
            contentId = protectedContent.contentId,
            keyId = protectedContent.keyId,
            licenseUrl = protectedContent.licenseUrl ?: "",
            drmScheme = protectedContent.drmScheme,
            challenge = generateChallenge(),
            requestTime = System.currentTimeMillis(),
            renewalRequest = true,
            existingLicenseId = currentLicense.licenseId
        )
    }
    
    private suspend fun processLicenseRequest(request: LicenseRequest): LicenseResponse {
        return try {
            // Simulate license server communication
            // In real implementation, this would make HTTP request to license server
            delay(100) // Simulate network delay
            
            if (request.licenseUrl.isNotEmpty()) {
                LicenseResponse(
                    success = true,
                    licenseData = generateLicenseData(),
                    expiryTime = System.currentTimeMillis() + DEFAULT_LICENSE_DURATION,
                    permissions = getDefaultPermissions(),
                    keyId = request.keyId
                )
            } else {
                LicenseResponse(
                    success = false,
                    errorMessage = "Invalid license URL"
                )
            }
        } catch (e: Exception) {
            LicenseResponse(
                success = false,
                errorMessage = e.message ?: "License request failed"
            )
        }
    }
    
    private fun createDRMLicense(
        protectedContent: DRMProtectedContent,
        response: LicenseResponse
    ): DRMLicenseRecord {
        return DRMLicenseRecord(
            licenseId = generateLicenseId(),
            contentId = protectedContent.contentId,
            keyId = protectedContent.keyId,
            drmScheme = protectedContent.drmScheme,
            licenseData = response.licenseData ?: ByteArray(0),
            issuedAt = System.currentTimeMillis(),
            expiresAt = response.expiryTime,
            permissions = response.permissions,
            restrictions = createDefaultRestrictions(),
            usageCount = 0,
            renewalCount = 0
        )
    }
    
    private fun createDRMSession(
        protectedContent: DRMProtectedContent,
        license: DRMLicenseRecord,
        sessionType: DRMSessionType = DRMSessionType.STREAMING
    ): DRMSessionInfo {
        return DRMSessionInfo(
            sessionId = generateSessionId(),
            contentId = protectedContent.contentId,
            licenseId = license.licenseId,
            sessionType = sessionType,
            createdAt = System.currentTimeMillis(),
            lastActivity = System.currentTimeMillis(),
            state = DRMSessionState.ACTIVE
        )
    }
    
    private fun validateUsageRestrictions(
        license: DRMLicenseRecord,
        permissions: Set<DRMPermission>
    ): RestrictionValidationResult {
        val restrictions = license.restrictions ?: return RestrictionValidationResult(true, null)
        
        // Check play count restriction
        restrictions.maxPlayCount?.let { maxCount ->
            if (license.usageCount >= maxCount) {
                return RestrictionValidationResult(false, "Maximum play count exceeded")
            }
        }
        
        // Check play duration restriction
        restrictions.maxPlayDuration?.let { maxDuration ->
            val totalPlayTime = getTotalPlayTime(license.licenseId)
            if (totalPlayTime >= maxDuration) {
                return RestrictionValidationResult(false, "Maximum play duration exceeded")
            }
        }
        
        // Check output restrictions
        if (restrictions.outputRestrictions.contains(OutputRestriction.ANALOG_BLOCKED)) {
            // In real implementation, check if analog output is being used
        }
        
        return RestrictionValidationResult(true, null)
    }
    
    private fun updateLicenseUsage(license: DRMLicenseRecord, permissions: Set<DRMPermission>) {
        if (permissions.contains(DRMPermission.PLAY)) {
            val updatedLicense = license.copy(
                usageCount = license.usageCount + 1,
                lastUsed = System.currentTimeMillis()
            )
            activeLicenses[license.contentId] = updatedLicense
        }
    }
    
    private suspend fun notifyLicenseRevocation(license: DRMLicenseRecord, reason: String) {
        try {
            // In real implementation, notify license server of revocation
            Log.d(TAG, "License revocation notified: ${license.licenseId}, reason: $reason")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to notify license revocation", e)
        }
    }
    
    private fun closeDRMSession(sessionId: String) {
        val session = drmSessions.remove(sessionId)
        if (session != null) {
            _drmEvents.trySend(
                DRMEvent.SessionClosed(session.contentId, sessionId)
            )
        }
    }
    
    private fun cleanupExpiredSessions() {
        val currentTime = System.currentTimeMillis()
        val expiredSessions = drmSessions.values.filter { it.isExpired(currentTime) }
        
        expiredSessions.forEach { session ->
            drmSessions.remove(session.sessionId)
        }
        
        if (expiredSessions.isNotEmpty()) {
            Log.d(TAG, "Cleaned up ${expiredSessions.size} expired DRM sessions")
        }
    }
    
    private suspend fun monitorLicenseExpiry() {
        val currentTime = System.currentTimeMillis()
        
        activeLicenses.values.forEach { license ->
            val timeToExpiry = license.expiresAt - currentTime
            
            // Schedule renewal for licenses expiring soon
            if (timeToExpiry > 0 && timeToExpiry <= LICENSE_RENEWAL_THRESHOLD) {
                scheduleLicenseRenewal(license)
            }
            
            // Remove expired licenses
            if (license.isExpired()) {
                activeLicenses.remove(license.contentId)
                _drmEvents.emit(
                    DRMEvent.LicenseExpired(license.contentId, license.licenseId)
                )
            }
        }
    }
    
    private fun scheduleLicenseRenewal(license: DRMLicenseRecord) {
        drmScope.launch {
            try {
                val protectedContent = this@DRMManager.protectedContent[license.contentId]
                if (protectedContent != null) {
                    renewLicense(protectedContent.contentUri)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Scheduled license renewal failed", e)
            }
        }
    }
    
    private suspend fun processLicenseRenewalQueue() {
        // Process any queued license renewal requests
        val queuedRequests = licenseRequestQueue.filter { it.renewalRequest }
        queuedRequests.forEach { request ->
            try {
                processLicenseRequest(request)
                licenseRequestQueue.remove(request)
            } catch (e: Exception) {
                Log.e(TAG, "Queued license renewal processing failed", e)
            }
        }
    }
    
    private fun updateDRMMetrics(scheme: DRMScheme) {
        val currentMetrics = _drmState.value.metrics
        _drmState.value = _drmState.value.copy(
            metrics = currentMetrics.copy(
                successfulAcquisitions = currentMetrics.successfulAcquisitions + 1
            )
        )
    }
    
    private fun calculateAverageLicenseDuration(): Long {
        if (activeLicenses.isEmpty()) return 0L
        
        val totalDuration = activeLicenses.values.sumOf { license ->
            license.expiresAt - license.issuedAt
        }
        
        return totalDuration / activeLicenses.size
    }
    
    private fun generateChallenge(): ByteArray {
        val challenge = ByteArray(32)
        secureRandom.nextBytes(challenge)
        return challenge
    }
    
    private fun generateLicenseData(): ByteArray {
        val data = ByteArray(256)
        secureRandom.nextBytes(data)
        return data
    }
    
    private fun generateLicenseId(): String = "LIC_${System.currentTimeMillis()}_${secureRandom.nextInt(1000)}"
    private fun generateSessionId(): String = "SES_${System.currentTimeMillis()}_${secureRandom.nextInt(1000)}"
    
    private fun getDefaultPermissions(): Set<DRMPermission> {
        return setOf(DRMPermission.PLAY, DRMPermission.PAUSE, DRMPermission.SEEK)
    }
    
    private fun createDefaultRestrictions(): DRMRestrictions {
        return DRMRestrictions(
            maxPlayCount = 100,
            maxPlayDuration = 7200000L, // 2 hours
            outputRestrictions = emptySet(),
            hdcpRequired = false
        )
    }
    
    private fun getTotalPlayTime(licenseId: String): Long {
        // Implementation would track total play time for license
        return 0L
    }
    
    fun cleanup() {
        isInitialized = false
        drmScope.cancel()
        activeLicenses.clear()
        drmSessions.clear()
        protectedContent.clear()
        licenseRequestQueue.clear()
        drmSessionManager = null
    }
}

// Data classes for DRM management
data class DRMState(
    val isInitialized: Boolean = false,
    val supportedSchemes: Set<DRMScheme> = emptySet(),
    val metrics: DRMStateMetrics = DRMStateMetrics()
)

data class DRMStateMetrics(
    val successfulAcquisitions: Int = 0,
    val failedAcquisitions: Int = 0,
    val totalSessions: Int = 0
)

data class DRMProtectedContent(
    val contentId: String,
    val contentUri: Uri,
    val drmScheme: DRMScheme,
    val licenseUrl: String?,
    val keyId: String,
    val protectionLevel: ContentProtectionLevel,
    val createdAt: Long
)

data class DRMLicenseRecord(
    val licenseId: String,
    val contentId: String,
    val keyId: String,
    val drmScheme: DRMScheme,
    val licenseData: ByteArray,
    val issuedAt: Long,
    val expiresAt: Long,
    val permissions: Set<DRMPermission>,
    val restrictions: DRMRestrictions?,
    val usageCount: Int = 0,
    val renewalCount: Int = 0,
    val lastUsed: Long = 0L
) {
    fun isExpired(): Boolean = System.currentTimeMillis() > expiresAt
}

data class DRMSessionInfo(
    val sessionId: String,
    val contentId: String,
    val licenseId: String,
    val sessionType: DRMSessionType,
    val createdAt: Long,
    val lastActivity: Long,
    val state: DRMSessionState
) {
    fun isExpired(currentTime: Long): Boolean {
        val sessionTimeout = 1800000L // 30 minutes
        return currentTime - lastActivity > sessionTimeout
    }
}

data class LicenseRequest(
    val contentId: String,
    val keyId: String,
    val licenseUrl: String,
    val drmScheme: DRMScheme,
    val challenge: ByteArray,
    val requestTime: Long,
    val renewalRequest: Boolean = false,
    val existingLicenseId: String? = null
)

data class LicenseResponse(
    val success: Boolean,
    val licenseData: ByteArray? = null,
    val expiryTime: Long = 0L,
    val permissions: Set<DRMPermission> = emptySet(),
    val keyId: String = "",
    val errorMessage: String = ""
)

data class RestrictionValidationResult(
    val valid: Boolean,
    val violation: String?
)

data class DRMMetrics(
    val activeLicenses: Int,
    val expiredLicenses: Int,
    val activeSessions: Int,
    val totalProtectedContent: Int,
    val licensesByScheme: Map<DRMScheme, Int>,
    val averageLicenseDuration: Long,
    val successfulAcquisitions: Int,
    val failedAcquisitions: Int
)

enum class DRMSessionState { ACTIVE, SUSPENDED, CLOSED, ERROR }

// Result classes
sealed class DRMConfigurationResult {
    data class Success(val protectedContent: DRMProtectedContent) : DRMConfigurationResult()
    data class InvalidConfiguration(val reason: String) : DRMConfigurationResult()
    data class UnsupportedScheme(val scheme: DRMScheme) : DRMConfigurationResult()
    data class Error(val exception: Exception) : DRMConfigurationResult()
}

sealed class LicenseAcquisitionResult {
    data class Success(val license: DRMLicenseRecord) : LicenseAcquisitionResult()
    data class ExistingLicense(val license: DRMLicenseRecord) : LicenseAcquisitionResult()
    object ContentNotProtected : LicenseAcquisitionResult()
    data class LicenseServerError(val message: String) : LicenseAcquisitionResult()
    data class Error(val exception: Exception) : LicenseAcquisitionResult()
}

sealed class LicenseValidationResult {
    data class Valid(val license: DRMLicenseRecord) : LicenseValidationResult()
    object NoLicense : LicenseValidationResult()
    object LicenseExpired : LicenseValidationResult()
    data class InsufficientPermissions(val missingPermissions: Set<DRMPermission>) : LicenseValidationResult()
    data class UsageRestrictionViolation(val violation: String) : LicenseValidationResult()
    data class Error(val exception: Exception) : LicenseValidationResult()
}

sealed class LicenseRenewalResult {
    data class Success(val renewedLicense: DRMLicenseRecord) : LicenseRenewalResult()
    object NoExistingLicense : LicenseRenewalResult()
    object ContentNotFound : LicenseRenewalResult()
    data class RenewalFailed(val reason: String) : LicenseRenewalResult()
    data class Error(val exception: Exception) : LicenseRenewalResult()
}

sealed class LicenseRevocationResult {
    data class Success(val licenseId: String) : LicenseRevocationResult()
    object NoLicense : LicenseRevocationResult()
    data class Error(val exception: Exception) : LicenseRevocationResult()
}

sealed class DRMSessionResult {
    data class Success(val session: DRMSessionInfo) : DRMSessionResult()
    object NoValidLicense : DRMSessionResult()
    object SessionLimitExceeded : DRMSessionResult()
    object ContentNotFound : DRMSessionResult()
    data class Error(val exception: Exception) : DRMSessionResult()
}

// Events
sealed class DRMEvent {
    data class Initialized(val supportedSchemes: Set<DRMScheme>) : DRMEvent()
    data class ContentConfigured(val contentUri: Uri, val scheme: DRMScheme) : DRMEvent()
    data class ConfigurationFailed(val contentUri: Uri, val error: Exception) : DRMEvent()
    data class LicenseAcquired(val contentUri: Uri, val licenseId: String) : DRMEvent()
    data class LicenseAcquisitionFailed(val contentUri: Uri, val error: Exception) : DRMEvent()
    data class LicenseRenewed(val contentUri: Uri, val licenseId: String) : DRMEvent()
    data class LicenseExpired(val contentId: String, val licenseId: String) : DRMEvent()
    data class LicenseRevoked(val contentUri: Uri, val licenseId: String, val reason: String) : DRMEvent()
    data class SessionCreated(val contentUri: Uri, val sessionId: String) : DRMEvent()
    data class SessionClosed(val contentId: String, val sessionId: String) : DRMEvent()
}