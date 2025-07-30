package com.astralplayer.nextplayer.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.astralplayer.nextplayer.security.SecurityManager
import com.astralplayer.nextplayer.security.SecurityPriority
import com.astralplayer.nextplayer.security.SecurityRecommendation
import com.astralplayer.nextplayer.ui.components.BubbleCard
import com.astralplayer.nextplayer.ui.components.BubbleChip
import kotlinx.coroutines.launch

/**
 * Privacy and Security Settings Screen for AstralStream
 * Manages biometric authentication, private mode, and content security
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacySettingsScreen(
    onNavigateBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val securityManager = remember { SecurityManager(context) }
    val scope = rememberCoroutineScope()
    
    // State variables
    var privateModeEnabled by remember { mutableStateOf(securityManager.isPrivateModeEnabled()) }
    var biometricEnabled by remember { mutableStateOf(securityManager.isBiometricEnabled()) }
    var incognitoActive by remember { mutableStateOf(securityManager.isIncognitoSessionActive()) }
    var contentLockTimeout by remember { mutableStateOf(securityManager.getContentLockTimeout()) }
    var securityRecommendations by remember { mutableStateOf(securityManager.getSecurityRecommendations()) }
    var showTimeoutDialog by remember { mutableStateOf(false) }
    var showClearHistoryDialog by remember { mutableStateOf(false) }
    
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        item {
            Column {
                Text(
                    text = "Privacy & Security",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Protect your adult content and personal data",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        }
        
        // Security Level Indicator
        item {
            SecurityLevelCard(securityLevel = securityManager.getCurrentSecurityLevel())
        }
        
        // Privacy Settings
        item {
            BubbleCard {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Privacy Settings",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    
                    // Private Mode Toggle
                    SettingsRow(
                        icon = Icons.Default.Lock,
                        title = "Private Mode",
                        subtitle = "Hide adult content from history and recent files",
                        trailing = {
                            Switch(
                                checked = privateModeEnabled,
                                onCheckedChange = { enabled ->
                                    privateModeEnabled = enabled
                                    securityManager.setPrivateModeEnabled(enabled)
                                    securityRecommendations = securityManager.getSecurityRecommendations()
                                }
                            )
                        }
                    )
                    
                    Divider()
                    
                    // Biometric Authentication
                    SettingsRow(
                        icon = Icons.Default.Fingerprint,
                        title = "Biometric Lock",
                        subtitle = if (securityManager.isBiometricAvailable()) {
                            "Use fingerprint or face recognition for adult content"
                        } else {
                            "Biometric authentication not available on this device"
                        },
                        trailing = {
                            Switch(
                                checked = biometricEnabled,
                                enabled = securityManager.isBiometricAvailable(),
                                onCheckedChange = { enabled ->
                                    biometricEnabled = enabled
                                    securityManager.setBiometricEnabled(enabled)
                                    securityRecommendations = securityManager.getSecurityRecommendations()
                                }
                            )
                        }
                    )
                    
                    Divider()
                    
                    // Incognito Session
                    SettingsRow(
                        icon = Icons.Default.VisibilityOff,
                        title = "Incognito Session",
                        subtitle = "Temporary session that doesn't save any data",
                        trailing = {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (incognitoActive) {
                                    BubbleChip(
                                        text = "Active",
                                        onClick = { /* Status indicator only */ },
                                        selected = true,
                                        containerColor = MaterialTheme.colorScheme.errorContainer,
                                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                                Switch(
                                    checked = incognitoActive,
                                    onCheckedChange = { active ->
                                        incognitoActive = active
                                        securityManager.setIncognitoSessionActive(active)
                                    }
                                )
                            }
                        }
                    )
                }
            }
        }
        
        // Content Security
        item {
            BubbleCard {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Content Security",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    
                    // Lock Timeout
                    SettingsRow(
                        icon = Icons.Default.Timer,
                        title = "Auto-Lock Timeout",
                        subtitle = "Lock adult content after $contentLockTimeout minutes of inactivity",
                        onClick = { showTimeoutDialog = true }
                    )
                    
                    Divider()
                    
                    // Clear History
                    SettingsRow(
                        icon = Icons.Default.Delete,
                        title = "Clear Adult Content History",
                        subtitle = "Permanently delete all stored adult content history",
                        onClick = { showClearHistoryDialog = true }
                    )
                    
                    Divider()
                    
                    // Security Test
                    SettingsRow(
                        icon = Icons.Default.Security,
                        title = "Test Biometric Authentication",
                        subtitle = "Verify your biometric setup is working correctly",
                        enabled = securityManager.isBiometricAvailable(),
                        onClick = {
                            if (context is FragmentActivity) {
                                securityManager.authenticateForAdultContent(
                                    activity = context,
                                    onSuccess = {
                                        // Show success message
                                    },
                                    onError = { error ->
                                        // Show error message
                                    },
                                    onUserCancel = {
                                        // User cancelled
                                    }
                                )
                            }
                        }
                    )
                }
            }
        }
        
        // Security Recommendations
        if (securityRecommendations.isNotEmpty()) {
            item {
                BubbleCard {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Security Recommendations",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
            
            items(securityRecommendations) { recommendation ->
                SecurityRecommendationCard(recommendation = recommendation)
            }
        }
    }
    
    // Timeout Selection Dialog
    if (showTimeoutDialog) {
        TimeoutSelectionDialog(
            currentTimeout = contentLockTimeout,
            onTimeoutSelected = { timeout ->
                contentLockTimeout = timeout
                securityManager.setContentLockTimeout(timeout)
                showTimeoutDialog = false
            },
            onDismiss = { showTimeoutDialog = false }
        )
    }
    
    // Clear History Confirmation Dialog
    if (showClearHistoryDialog) {
        AlertDialog(
            onDismissRequest = { showClearHistoryDialog = false },
            title = { Text("Clear Adult Content History") },
            text = { 
                Text("This will permanently delete all stored adult content history. This action cannot be undone.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        securityManager.clearAdultContentHistory()
                        showClearHistoryDialog = false
                    }
                ) {
                    Text("Clear", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearHistoryDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun SecurityLevelCard(securityLevel: Int) {
    val (levelText, levelColor, levelIcon) = when (securityLevel) {
        SecurityManager.SECURITY_LEVEL_NONE -> Triple("Basic", MaterialTheme.colorScheme.error, Icons.Default.Warning)
        SecurityManager.SECURITY_LEVEL_PIN -> Triple("Good", MaterialTheme.colorScheme.primary, Icons.Default.Shield)
        SecurityManager.SECURITY_LEVEL_BIOMETRIC -> Triple("Excellent", MaterialTheme.colorScheme.primary, Icons.Default.VerifiedUser)
        SecurityManager.SECURITY_LEVEL_HIGH -> Triple("Maximum", MaterialTheme.colorScheme.primary, Icons.Default.Security)
        else -> Triple("Unknown", MaterialTheme.colorScheme.outline, Icons.Default.Help)
    }
    
    BubbleCard(
        containerColor = levelColor.copy(alpha = 0.1f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = levelIcon,
                contentDescription = null,
                tint = levelColor,
                modifier = Modifier.size(32.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Security Level: $levelText",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Your privacy protection level",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
private fun SecurityRecommendationCard(recommendation: SecurityRecommendation) {
    val priorityColor = when (recommendation.priority) {
        SecurityPriority.CRITICAL -> MaterialTheme.colorScheme.error
        SecurityPriority.HIGH -> MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
        SecurityPriority.MEDIUM -> MaterialTheme.colorScheme.primary
        SecurityPriority.LOW -> MaterialTheme.colorScheme.outline
    }
    
    BubbleCard(
        containerColor = priorityColor.copy(alpha = 0.05f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = priorityColor,
                modifier = Modifier.size(20.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = recommendation.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = recommendation.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
            BubbleChip(
                text = recommendation.priority.name,
                onClick = { /* Status indicator only */ },
                selected = true,
                containerColor = priorityColor.copy(alpha = 0.2f),
                contentColor = priorityColor
            )
        }
    }
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    enabled: Boolean = true,
    trailing: @Composable (() -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (onClick != null && enabled) {
                    Modifier.clickable { onClick() }
                } else Modifier
            )
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
            modifier = Modifier.size(24.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = if (enabled) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
            )
        }
        trailing?.invoke()
    }
}

@Composable
private fun TimeoutSelectionDialog(
    currentTimeout: Int,
    onTimeoutSelected: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val timeoutOptions = listOf(5, 10, 15, 30, 60, 120) // minutes
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Auto-Lock Timeout") },
        text = {
            Column {
                Text("Select how long to wait before locking adult content:")
                Spacer(modifier = Modifier.height(16.dp))
                timeoutOptions.forEach { timeout ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onTimeoutSelected(timeout) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = timeout == currentTimeout,
                            onClick = { onTimeoutSelected(timeout) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = when {
                                timeout < 60 -> "$timeout minutes"
                                timeout == 60 -> "1 hour"
                                else -> "${timeout / 60} hours"
                            }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done")
            }
        }
    )
}