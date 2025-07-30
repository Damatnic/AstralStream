package com.astralplayer.nextplayer.utils

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

/**
 * Comprehensive permissions manager for AstralStream
 * Handles storage, media, and other sensitive permissions with proper user education
 */
class PermissionsManager(private val activity: FragmentActivity) {
    
    companion object {
        private const val TAG = "PermissionsManager"
        
        // Permission request codes
        const val REQUEST_STORAGE_PERMISSION = 1001
        const val REQUEST_MEDIA_PERMISSION = 1002
        const val REQUEST_WRITE_SETTINGS = 1003
        const val REQUEST_MANAGE_EXTERNAL_STORAGE = 1004
        const val REQUEST_BIOMETRIC_PERMISSION = 1005
        const val REQUEST_CAMERA_PERMISSION = 1006
        const val REQUEST_MICROPHONE_PERMISSION = 1007
        
        // Required permissions based on Android version
        val STORAGE_PERMISSIONS_LEGACY = arrayOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
        
        val MEDIA_PERMISSIONS_ANDROID_13 = arrayOf(
            Manifest.permission.READ_MEDIA_VIDEO,
            Manifest.permission.READ_MEDIA_AUDIO,
            Manifest.permission.READ_MEDIA_IMAGES
        )
        
        val ADDITIONAL_PERMISSIONS = arrayOf(
            Manifest.permission.WRITE_SETTINGS,
            Manifest.permission.USE_BIOMETRIC,
            Manifest.permission.USE_FINGERPRINT,
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.SYSTEM_ALERT_WINDOW
        )
    }
    
    // Activity result launchers
    private var storagePermissionLauncher: ActivityResultLauncher<Array<String>>? = null
    private var manageStorageLauncher: ActivityResultLauncher<Intent>? = null
    private var writeSettingsLauncher: ActivityResultLauncher<Intent>? = null
    
    // Callbacks
    private var onPermissionResult: ((granted: Boolean, permission: String) -> Unit)? = null
    private var onAllPermissionsGranted: (() -> Unit)? = null
    
    init {
        setupPermissionLaunchers()
    }
    
    private fun setupPermissionLaunchers() {
        // Storage permissions launcher
        storagePermissionLauncher = activity.registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            val allGranted = permissions.all { it.value }
            Log.d(TAG, "Storage permissions result: $permissions")
            
            if (allGranted) {
                onPermissionResult?.invoke(true, "storage")
                checkAndRequestManageExternalStorage()
            } else {
                handlePermissionDenied("storage", permissions)
            }
        }
        
        // Manage external storage launcher (Android 11+)
        manageStorageLauncher = activity.registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) {
            val hasPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                Environment.isExternalStorageManager()
            } else true
            
            Log.d(TAG, "Manage external storage permission: $hasPermission")
            onPermissionResult?.invoke(hasPermission, "manage_storage")
            
            if (hasPermission) {
                checkAllPermissionsComplete()
            }
        }
        
        // Write settings launcher
        writeSettingsLauncher = activity.registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) {
            val hasPermission = Settings.System.canWrite(activity)
            Log.d(TAG, "Write settings permission: $hasPermission")
            onPermissionResult?.invoke(hasPermission, "write_settings")
        }
    }
    
    /**
     * Request all necessary permissions for video player functionality
     */
    fun requestAllPermissions(
        onResult: (granted: Boolean, permission: String) -> Unit = { _, _ -> },
        onComplete: () -> Unit = {}
    ) {
        onPermissionResult = onResult
        onAllPermissionsGranted = onComplete
        
        Log.d(TAG, "Starting comprehensive permission request flow")
        requestStoragePermissions()
    }
    
    /**
     * Request storage and media permissions based on Android version
     */
    private fun requestStoragePermissions() {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            MEDIA_PERMISSIONS_ANDROID_13
        } else {
            STORAGE_PERMISSIONS_LEGACY
        }
        
        val missingPermissions = permissions.filter { permission ->
            ContextCompat.checkSelfPermission(activity, permission) != PackageManager.PERMISSION_GRANTED
        }
        
        if (missingPermissions.isEmpty()) {
            Log.d(TAG, "Storage permissions already granted")
            onPermissionResult?.invoke(true, "storage")
            checkAndRequestManageExternalStorage()
            return
        }
        
        // Show educational dialog before requesting permissions
        showPermissionEducationDialog(
            title = "Media Access Required",
            message = "AstralStream needs access to your device's media files to:\n\n" +
                    "• Play videos from your personal library\n" +
                    "• Create thumbnails for better browsing\n" +
                    "• Save playback preferences\n" +
                    "• Manage downloaded content\n\n" +
                    "Your privacy is important - we only access video files you choose to play.",
            onAccept = {
                storagePermissionLauncher?.launch(missingPermissions.toTypedArray())
            },
            onDeny = {
                onPermissionResult?.invoke(false, "storage")
            }
        )
    }
    
    /**
     * Request MANAGE_EXTERNAL_STORAGE permission for Android 11+
     */
    private fun checkAndRequestManageExternalStorage() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                showPermissionEducationDialog(
                    title = "Enhanced File Access",
                    message = "For the best experience with adult content and streaming, AstralStream needs enhanced file access to:\n\n" +
                            "• Access videos from all folders\n" +
                            "• Cache streams efficiently\n" +
                            "• Manage temporary files\n" +
                            "• Handle downloads from adult sites\n\n" +
                            "This ensures smooth playback and better performance.",
                    onAccept = {
                        try {
                            val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                                data = Uri.parse("package:${activity.packageName}")
                            }
                            manageStorageLauncher?.launch(intent)
                        } catch (e: Exception) {
                            Log.e(TAG, "Error opening manage storage settings", e)
                            // Fallback to general settings
                            val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                            manageStorageLauncher?.launch(intent)
                        }
                    },
                    onDeny = {
                        onPermissionResult?.invoke(false, "manage_storage")
                        checkAllPermissionsComplete()
                    }
                )
            } else {
                Log.d(TAG, "Manage external storage already granted")
                onPermissionResult?.invoke(true, "manage_storage")
                checkAllPermissionsComplete()
            }
        } else {
            checkAllPermissionsComplete()
        }
    }
    
    /**
     * Request WRITE_SETTINGS permission for brightness control
     */
    fun requestWriteSettingsPermission(onResult: (granted: Boolean) -> Unit = {}) {
        if (Settings.System.canWrite(activity)) {
            Log.d(TAG, "Write settings permission already granted")
            onResult(true)
            return
        }
        
        showPermissionEducationDialog(
            title = "Brightness Control",
            message = "AstralStream can control screen brightness during video playback for optimal viewing experience.\n\n" +
                    "This feature allows:\n" +
                    "• Automatic brightness adjustment\n" +
                    "• Gesture-based brightness control\n" +
                    "• Better viewing in different lighting conditions",
            onAccept = {
                try {
                    val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
                        data = Uri.parse("package:${activity.packageName}")
                    }
                    onPermissionResult = { granted, _ -> onResult(granted) }
                    writeSettingsLauncher?.launch(intent)
                } catch (e: Exception) {
                    Log.e(TAG, "Error opening write settings", e)
                    onResult(false)
                }
            },
            onDeny = {
                onResult(false)
            }
        )
    }
    
    /**
     * Request biometric authentication permission
     */
    fun requestBiometricPermission(onResult: (granted: Boolean) -> Unit = {}) {
        val permissions = arrayOf(
            Manifest.permission.USE_BIOMETRIC,
            Manifest.permission.USE_FINGERPRINT
        ).filter { permission ->
            ContextCompat.checkSelfPermission(activity, permission) != PackageManager.PERMISSION_GRANTED
        }
        
        if (permissions.isEmpty()) {
            onResult(true)
            return
        }
        
        showPermissionEducationDialog(
            title = "Biometric Authentication",
            message = "Secure your adult content with biometric authentication.\n\n" +
                    "This enables:\n" +
                    "• Fingerprint/face unlock for private mode\n" +
                    "• Secure access to adult content\n" +
                    "• Privacy protection from unauthorized access",
            onAccept = {
                ActivityCompat.requestPermissions(
                    activity,
                    permissions.toTypedArray(),
                    REQUEST_BIOMETRIC_PERMISSION
                )
                onPermissionResult = { granted, _ -> onResult(granted) }
            },
            onDeny = { onResult(false) }
        )
    }
    
    /**
     * Request camera permission for advanced features
     */
    fun requestCameraPermission(onResult: (granted: Boolean) -> Unit = {}) {
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            onResult(true)
            return
        }
        
        showPermissionEducationDialog(
            title = "Camera Access",
            message = "Camera access enables advanced features:\n\n" +
                    "• QR code scanning for streaming links\n" +
                    "• Enhanced gesture recognition\n" +
                    "• Screenshot and sharing features",
            onAccept = {
                ActivityCompat.requestPermissions(
                    activity,
                    arrayOf(Manifest.permission.CAMERA),
                    REQUEST_CAMERA_PERMISSION
                )
                onPermissionResult = { granted, _ -> onResult(granted) }
            },
            onDeny = { onResult(false) }
        )
    }
    
    /**
     * Request microphone permission for voice features
     */
    fun requestMicrophonePermission(onResult: (granted: Boolean) -> Unit = {}) {
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            onResult(true)
            return
        }
        
        showPermissionEducationDialog(
            title = "Microphone Access",
            message = "Microphone access enables:\n\n" +
                    "• Voice commands for hands-free control\n" +
                    "• Audio recording features\n" +
                    "• Enhanced accessibility options",
            onAccept = {
                ActivityCompat.requestPermissions(
                    activity,
                    arrayOf(Manifest.permission.RECORD_AUDIO),
                    REQUEST_MICROPHONE_PERMISSION
                )
                onPermissionResult = { granted, _ -> onResult(granted) }
            },
            onDeny = { onResult(false) }
        )
    }
    
    /**
     * Check if all essential permissions are granted
     */
    fun hasAllEssentialPermissions(): Boolean {
        val storageGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            MEDIA_PERMISSIONS_ANDROID_13.all { permission ->
                ContextCompat.checkSelfPermission(activity, permission) == PackageManager.PERMISSION_GRANTED
            }
        } else {
            STORAGE_PERMISSIONS_LEGACY.all { permission ->
                ContextCompat.checkSelfPermission(activity, permission) == PackageManager.PERMISSION_GRANTED
            }
        }
        
        val manageStorageGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else true
        
        return storageGranted && manageStorageGranted
    }
    
    /**
     * Get permission status summary
     */
    fun getPermissionStatusSummary(): Map<String, Boolean> {
        return mapOf(
            "storage" to hasStoragePermissions(),
            "manage_storage" to hasManageStoragePermission(),
            "write_settings" to Settings.System.canWrite(activity),
            "biometric" to hasBiometricPermission(),
            "camera" to (ContextCompat.checkSelfPermission(activity, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED),
            "microphone" to (ContextCompat.checkSelfPermission(activity, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED)
        )
    }
    
    private fun hasStoragePermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            MEDIA_PERMISSIONS_ANDROID_13.all { permission ->
                ContextCompat.checkSelfPermission(activity, permission) == PackageManager.PERMISSION_GRANTED
            }
        } else {
            STORAGE_PERMISSIONS_LEGACY.all { permission ->
                ContextCompat.checkSelfPermission(activity, permission) == PackageManager.PERMISSION_GRANTED
            }
        }
    }
    
    private fun hasManageStoragePermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else true
    }
    
    private fun hasBiometricPermission(): Boolean {
        return arrayOf(Manifest.permission.USE_BIOMETRIC, Manifest.permission.USE_FINGERPRINT).any { permission ->
            ContextCompat.checkSelfPermission(activity, permission) == PackageManager.PERMISSION_GRANTED
        }
    }
    
    private fun handlePermissionDenied(permissionType: String, results: Map<String, Boolean>) {
        val deniedPermissions = results.filter { !it.value }.keys
        Log.w(TAG, "Permissions denied: $deniedPermissions")
        
        // Check if user selected "Don't ask again"
        val shouldShowRationale = deniedPermissions.any { permission ->
            ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)
        }
        
        if (!shouldShowRationale) {
            showPermissionSettingsDialog(permissionType)
        } else {
            onPermissionResult?.invoke(false, permissionType)
        }
    }
    
    private fun showPermissionSettingsDialog(permissionType: String) {
        AlertDialog.Builder(activity)
            .setTitle("Permission Required")
            .setMessage("$permissionType permission is required for AstralStream to function properly. Please enable it in app settings.")
            .setPositiveButton("Open Settings") { _, _ ->
                openAppSettings()
            }
            .setNegativeButton("Cancel") { _, _ ->
                onPermissionResult?.invoke(false, permissionType)
            }
            .setCancelable(false)
            .show()
    }
    
    private fun showPermissionEducationDialog(
        title: String,
        message: String,
        onAccept: () -> Unit,
        onDeny: () -> Unit
    ) {
        AlertDialog.Builder(activity)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("Grant Permission") { _, _ -> onAccept() }
            .setNegativeButton("Not Now") { _, _ -> onDeny() }
            .setCancelable(false)
            .show()
    }
    
    private fun openAppSettings() {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:${activity.packageName}")
            }
            activity.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Error opening app settings", e)
        }
    }
    
    private fun checkAllPermissionsComplete() {
        if (hasAllEssentialPermissions()) {
            Log.d(TAG, "All essential permissions granted")
            onAllPermissionsGranted?.invoke()
        }
    }
    
    /**
     * Handle permission results from onRequestPermissionsResult
     */
    fun handlePermissionResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        val granted = grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }
        
        when (requestCode) {
            REQUEST_BIOMETRIC_PERMISSION -> {
                onPermissionResult?.invoke(granted, "biometric")
            }
            REQUEST_CAMERA_PERMISSION -> {
                onPermissionResult?.invoke(granted, "camera")
            }
            REQUEST_MICROPHONE_PERMISSION -> {
                onPermissionResult?.invoke(granted, "microphone")
            }
        }
    }
}