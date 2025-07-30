package com.astralplayer.nextplayer.security

import android.net.Uri

/**
 * Comprehensive data classes for security management system
 */

// Core security data structures
data class AccessPolicy(
    val authorizedComponents: Set<String>,
    val allowedAccessTypes: Set<AccessType>,
    val accessTimeRestrictions: TimeRestrictions? = null,
    val usageLimits: UsageLimits? = null,
    val geographicRestrictions: GeographicRestrictions? = null,
    val deviceRestrictions: DeviceRestrictions? = null
)

data class TimeRestrictions(
    val validFrom: Long,
    val validUntil: Long,
    val allowedTimeWindows: List<TimeWindow>? = null
)

data class TimeWindow(
    val startHour: Int, // 0-23
    val endHour: Int,   // 0-23
    val daysOfWeek: Set<DayOfWeek>
)

enum class DayOfWeek { MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY }

data class UsageLimits(
    val maxAccesses: Int,
    val maxDailyAccesses: Int? = null,
    val maxConcurrentSessions: Int? = null,
    val cooldownPeriod: Long? = null // milliseconds
)

data class GeographicRestrictions(
    val allowedCountries: Set<String>? = null,
    val blockedCountries: Set<String>? = null,
    val allowedRegions: Set<String>? = null,
    val requiresLocationVerification: Boolean = false
)

data class DeviceRestrictions(
    val allowedDeviceTypes: Set<DeviceType>? = null,
    val requiresDeviceAuthentication: Boolean = false,
    val maxDevicesPerUser: Int? = null,
    val trustedDevicesOnly: Boolean = false
)

enum class DeviceType { PHONE, TABLET, TV, BROWSER, DESKTOP }

// Security configuration
data class SecurityConfiguration(
    val enableEncryption: Boolean = true,
    val enableContentProtection: Boolean = true,
    val enablePrivacyMode: Boolean = true,
    val enableDRM: Boolean = false,
    val enableThreatDetection: Boolean = true,
    val requireUserAuthentication: Boolean = false,
    val authenticationValidityDuration: Int = 300, // seconds
    val accessTokenValidityPeriod: Long = 3600000L, // milliseconds
    val securityLevel: SecurityLevel = SecurityLevel.STANDARD,
    val complianceLevel: ComplianceLevel = ComplianceLevel.STANDARD
)

enum class SecurityLevel { MINIMAL, BASIC, STANDARD, HIGH, MAXIMUM }
enum class ComplianceLevel { BASIC, STANDARD, HIGH, ENTERPRISE }

// Security state and monitoring
data class SecurityState(
    val isInitialized: Boolean = false,
    val encryptionEnabled: Boolean = false,
    val contentProtectionEnabled: Boolean = false,
    val privacyModeEnabled: Boolean = false,
    val drmEnabled: Boolean = false,
    val threatDetectionEnabled: Boolean = false,
    val emergencyMode: Boolean = false,
    val totalProtectedContent: Int = 0,
    val activeThreats: List<SecurityThreat> = emptyList(),
    val lastProtectionActivity: Long = 0L,
    val lastSecurityCheck: Long = 0L,
    val encryptionKeyHealth: Float = 1.0f,
    val drmStatus: DRMStatus? = null,
    val privacySettings: PrivacySettings = PrivacySettings(),
    val securityScore: Float = 1.0f,
    val complianceStatus: ComplianceStatus = ComplianceStatus()
)

// Privacy management
data class PrivacySettings(
    val enabled: Boolean = false,
    val dataMinimization: Boolean = true,
    val anonymizeData: Boolean = true,
    val clearOnExit: Boolean = false,
    val clearExistingData: Boolean = false,
    val trackingPrevention: Boolean = true,
    val dataSharingRestrictions: DataSharingRestrictions = DataSharingRestrictions(),
    val dataRetention: DataRetentionPolicy = DataRetentionPolicy(),
    val userConsent: UserConsentSettings = UserConsentSettings()
)

data class DataSharingRestrictions(
    val allowAnalytics: Boolean = false,
    val allowThirdPartyServices: Boolean = false,
    val allowCrashReporting: Boolean = true,
    val allowPerformanceMetrics: Boolean = true,
    val restrictedDataTypes: Set<SensitiveDataType> = emptySet()
)

data class DataRetentionPolicy(
    val retentionPeriod: Long = 7776000000L, // 90 days in milliseconds
    val autoDelete: Boolean = true,
    val purgeSchedule: PurgeSchedule = PurgeSchedule.WEEKLY,
    val exemptDataTypes: Set<SensitiveDataType> = emptySet()
)

enum class PurgeSchedule { DAILY, WEEKLY, MONTHLY, MANUAL }

data class UserConsentSettings(
    val consentGiven: Boolean = false,
    val consentTimestamp: Long = 0L,
    val consentVersion: String = "1.0",
    val granularConsent: Map<ConsentType, Boolean> = emptyMap(),
    val consentExpiry: Long? = null
)

enum class ConsentType {
    DATA_COLLECTION, ANALYTICS, CRASH_REPORTING, PERFORMANCE_MONITORING,
    PERSONALIZATION, MARKETING, THIRD_PARTY_SHARING
}

// DRM management
data class DRMStatus(
    val isActive: Boolean = false,
    val provider: String? = null,
    val licenseStatus: LicenseStatus = LicenseStatus.UNKNOWN,
    val supportedSchemes: Set<DRMScheme> = emptySet(),
    val activeLineces: List<DRMLicense> = emptyList(),
    val lastLicenseCheck: Long = 0L
)

enum class LicenseStatus { UNKNOWN, VALID, EXPIRED, REVOKED, PENDING }
enum class DRMScheme { WIDEVINE, PLAYREADY, FAIRPLAY, CLEARKEY }

data class DRMLicense(
    val licenseId: String,
    val contentId: String,
    val expiryTime: Long,
    val permissions: Set<DRMPermission>,
    val restrictions: DRMRestrictions? = null
)

enum class DRMPermission { PLAY, PAUSE, SEEK, FAST_FORWARD, REWIND, COPY, SHARE }

data class DRMRestrictions(
    val maxPlayCount: Int? = null,
    val maxPlayDuration: Long? = null,
    val outputRestrictions: Set<OutputRestriction> = emptySet(),
    val hdcpRequired: Boolean = false
)

enum class OutputRestriction { ANALOG_BLOCKED, DIGITAL_PROTECTED, HDCP_REQUIRED }

data class DRMConfiguration(
    val provider: DRMScheme,
    val licenseUrl: String?,
    val serverCertificate: ByteArray? = null,
    val keyRequestProperties: Map<String, String> = emptyMap(),
    val sessionType: DRMSessionType = DRMSessionType.STREAMING
)

enum class DRMSessionType { STREAMING, OFFLINE, RELEASE }

// Security analysis and threats
data class SecurityThreat(
    val id: String,
    val type: ThreatType,
    val level: ThreatLevel,
    val source: String,
    val description: String,
    val timestamp: Long,
    val affectedResources: List<String> = emptyList(),
    val mitigationActions: List<String> = emptyList(),
    val resolved: Boolean = false
)

enum class ThreatType {
    MALWARE, UNAUTHORIZED_ACCESS, DATA_BREACH, INTEGRITY_VIOLATION,
    PRIVILEGE_ESCALATION, DENIAL_OF_SERVICE, SOCIAL_ENGINEERING,
    NETWORK_INTRUSION, SUSPICIOUS_BEHAVIOR
}

enum class ThreatLevel { LOW, MEDIUM, HIGH, CRITICAL }

data class SecurityVulnerability(
    val id: String,
    val type: VulnerabilityType,
    val severity: SecuritySeverity,
    val description: String,
    val affectedComponent: String,
    val cveId: String? = null,
    val discoveryDate: Long,
    val patchAvailable: Boolean = false,
    val workarounds: List<String> = emptyList()
)

enum class VulnerabilityType {
    BUFFER_OVERFLOW, INJECTION, BROKEN_AUTHENTICATION, SENSITIVE_DATA_EXPOSURE,
    XML_EXTERNAL_ENTITIES, BROKEN_ACCESS_CONTROL, SECURITY_MISCONFIGURATION,
    CROSS_SITE_SCRIPTING, INSECURE_DESERIALIZATION, INSUFFICIENT_LOGGING
}

enum class SecuritySeverity { LOW, MEDIUM, HIGH, CRITICAL }

// Content analysis and scanning
data class ContentAnalysisResult(
    val contentId: String,
    val hasVulnerabilities: Boolean,
    val hasMaliciousContent: Boolean,
    val vulnerabilities: List<SecurityVulnerability> = emptyList(),
    val maliciousIndicators: List<String> = emptyList(),
    val riskScore: Float,
    val analysisTimestamp: Long,
    val scanDuration: Long
)

data class SecurityScanResult(
    val scanId: String,
    val contentUri: Uri,
    val scanType: SecurityScanType,
    val hasVulnerabilities: Boolean,
    val hasMaliciousContent: Boolean,
    val vulnerabilities: List<SecurityVulnerability> = emptyList(),
    val maliciousIndicators: List<String> = emptyList(),
    val riskScore: Float,
    val recommendations: List<String> = emptyList(),
    val scanStartTime: Long,
    val scanEndTime: Long
) {
    companion object {
        fun Error(exception: Exception) = SecurityScanResult(
            scanId = "",
            contentUri = Uri.EMPTY,
            scanType = SecurityScanType.BASIC,
            hasVulnerabilities = false,
            hasMaliciousContent = false,
            riskScore = 0f,
            scanStartTime = System.currentTimeMillis(),
            scanEndTime = System.currentTimeMillis()
        )
    }
}

// Access control
data class AccessToken(
    val token: String,
    val expiresAt: Long,
    val permissions: Set<AccessPermission> = emptySet(),
    val restrictions: AccessRestrictions? = null
)

enum class AccessPermission { READ, WRITE, EXECUTE, SHARE, MODIFY, DELETE }

data class AccessRestrictions(
    val ipWhitelist: Set<String>? = null,
    val deviceFingerprint: String? = null,
    val sessionLimit: Int? = null,
    val bandwidth Limit: Long? = null
)

// Compliance and reporting
data class ComplianceStatus(
    val percentage: Int = 100,
    val level: ComplianceLevel = ComplianceLevel.STANDARD,
    val frameworks: Map<ComplianceFramework, ComplianceDetails> = emptyMap(),
    val lastAssessment: Long = 0L,
    val nextAssessmentDue: Long = 0L
)

enum class ComplianceFramework { GDPR, CCPA, COPPA, HIPAA, SOX, PCI_DSS, ISO27001 }

data class ComplianceDetails(
    val compliant: Boolean,
    val score: Float,
    val requirements: Map<String, Boolean> = emptyMap(),
    val gaps: List<String> = emptyList(),
    val lastAudit: Long = 0L
)

// Security reporting
data class SecurityReport(
    val securityState: SecurityState,
    val protectionMetrics: ProtectionMetrics,
    val encryptionMetrics: EncryptionMetrics,
    val privacyMetrics: PrivacyMetrics,
    val threatAnalysis: ThreatAnalysis,
    val complianceStatus: ComplianceStatus,
    val recommendations: List<String>,
    val securityScore: Float,
    val reportTimestamp: Long = System.currentTimeMillis()
)

data class PrivacyMetrics(
    val dataCollected: Map<SensitiveDataType, Long> = emptyMap(),
    val dataShared: Map<SensitiveDataType, Long> = emptyMap(),
    val dataDeleted: Map<SensitiveDataType, Long> = emptyMap(),
    val userConsents: Map<ConsentType, Int> = emptyMap(),
    val privacyViolations: Int = 0,
    val lastPrivacyAudit: Long = 0L
)

data class ThreatAnalysis(
    val activeThreats: List<SecurityThreat>,
    val resolvedThreats: List<SecurityThreat>,
    val threatTrends: Map<ThreatType, Int> = emptyMap(),
    val riskLevel: ThreatLevel,
    val mitigationEffectiveness: Float,
    val lastThreatAssessment: Long = System.currentTimeMillis()
)

// Error handling and results
sealed class SecurityResult<T> {
    data class Success<T>(val data: T) : SecurityResult<T>()
    data class Error<T>(val exception: Exception, val errorCode: SecurityErrorCode? = null) : SecurityResult<T>()
    data class AccessDenied<T>(val reason: String) : SecurityResult<T>()
}

enum class SecurityErrorCode {
    INITIALIZATION_FAILED, ENCRYPTION_FAILED, DECRYPTION_FAILED,
    ACCESS_DENIED, INVALID_TOKEN, EXPIRED_TOKEN, DRM_FAILURE,
    INTEGRITY_VIOLATION, QUARANTINE_REQUIRED, UNKNOWN_ERROR
}

// Privacy violations
data class PrivacyViolation(
    val id: String,
    val type: PrivacyViolationType,
    val severity: PrivacySeverity,
    val description: String,
    val affectedData: Set<SensitiveDataType>,
    val detectionTime: Long,
    val resolved: Boolean = false,
    val remediationActions: List<String> = emptyList()
)

enum class PrivacyViolationType {
    UNAUTHORIZED_COLLECTION, EXCESSIVE_RETENTION, UNAUTHORIZED_SHARING,
    INSUFFICIENT_CONSENT, DATA_BREACH, INADEQUATE_ANONYMIZATION,
    CROSS_BORDER_TRANSFER_VIOLATION, PURPOSE_LIMITATION_VIOLATION
}

enum class PrivacySeverity { LOW, MEDIUM, HIGH, CRITICAL }

// Security events for monitoring
sealed class SecurityEvent {
    data class Initialized(val config: SecurityConfiguration) : SecurityEvent()
    object InitializationFailed : SecurityEvent() {
        fun invoke(e: Exception): InitializationFailedWithException {
            return InitializationFailedWithException(e)
        }
    }
    data class InitializationFailedWithException(val exception: Exception) : SecurityEvent()
    data class ContentProtected(val contentUri: Uri, val level: ContentProtectionLevel) : SecurityEvent()
    data class ContentProtectionFailed(val contentUri: Uri, val error: Exception) : SecurityEvent()
    data class ContentAccessAuthorized(val contentUri: Uri, val component: String) : SecurityEvent()
    data class ContentAccessDenied(val contentUri: Uri, val component: String, val error: Exception) : SecurityEvent()
    data class DataEncrypted(val dataType: SensitiveDataType, val encryptionInfo: EncryptionInfo) : SecurityEvent()
    data class DataDecrypted(val dataType: SensitiveDataType) : SecurityEvent()
    data class EncryptionFailed(val dataType: SensitiveDataType, val error: Exception) : SecurityEvent()
    data class DecryptionFailed(val dataType: SensitiveDataType, val error: Exception) : SecurityEvent()
    data class PrivacySettingsUpdated(val settings: PrivacySettings) : SecurityEvent()
    data class PrivacyDataCleared(val dataTypes: Set<SensitiveDataType> = emptySet()) : SecurityEvent()
    data class PrivacyViolationDetected(val violation: PrivacyViolation) : SecurityEvent()
    data class DRMConfigured(val contentUri: Uri, val provider: DRMScheme) : SecurityEvent()
    data class DRMConfigurationFailed(val contentUri: Uri, val error: Exception) : SecurityEvent()
    data class SecurityScanCompleted(val contentUri: Uri, val result: SecurityScanResult) : SecurityEvent()
    data class SecurityScanFailed(val contentUri: Uri, val error: Exception) : SecurityEvent()
    data class ThreatDetected(val threat: SecurityThreat) : SecurityEvent()
    data class ThreatMitigated(val threatId: String) : SecurityEvent()
    data class SecurityAlertsDetected(val alerts: List<SecurityAlert>) : SecurityEvent()
    data class EmergencyProtocolActivated(val threat: SecurityThreat) : SecurityEvent()
    data class ComplianceViolation(val framework: ComplianceFramework, val violation: String) : SecurityEvent()
}

data class SecurityAlert(
    val id: String,
    val type: AlertType,
    val severity: AlertSeverity,
    val message: String,
    val timestamp: Long,
    val actionRequired: Boolean = false,
    val acknowleded: Boolean = false
)

enum class AlertType {
    THREAT_DETECTED, VULNERABILITY_FOUND, COMPLIANCE_VIOLATION,
    ENCRYPTION_FAILURE, ACCESS_VIOLATION, PRIVACY_BREACH,
    DRM_FAILURE, SUSPICIOUS_ACTIVITY, SYSTEM_COMPROMISE
}

enum class AlertSeverity { INFO, WARNING, ERROR, CRITICAL }