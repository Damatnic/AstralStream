package com.astralplayer.nextplayer.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.astralplayer.nextplayer.data.gesture.error.GestureErrorHandler
import kotlinx.coroutines.delay

/**
 * Error notification banner for gesture errors
 */
@Composable
fun GestureErrorBanner(
    errorState: GestureErrorHandler.ErrorState,
    onDismiss: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = errorState.lastError != null,
        enter = slideInVertically() + fadeIn(),
        exit = slideOutVertically() + fadeOut(),
        modifier = modifier
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = when (errorState.lastError?.type?.severity) {
                    GestureErrorHandler.ErrorSeverity.CRITICAL -> MaterialTheme.colorScheme.error
                    GestureErrorHandler.ErrorSeverity.HIGH -> MaterialTheme.colorScheme.errorContainer
                    else -> MaterialTheme.colorScheme.surfaceVariant
                }
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = when (errorState.lastError?.type?.severity) {
                            GestureErrorHandler.ErrorSeverity.CRITICAL -> Icons.Default.Error
                            GestureErrorHandler.ErrorSeverity.HIGH -> Icons.Default.Warning
                            else -> Icons.Default.Info
                        },
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = if (errorState.lastError?.type?.severity == GestureErrorHandler.ErrorSeverity.CRITICAL) {
                            MaterialTheme.colorScheme.onError
                        } else {
                            LocalContentColor.current
                        }
                    )
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    Column {
                        Text(
                            text = getErrorTitle(errorState),
                            style = MaterialTheme.typography.titleSmall
                        )
                        
                        errorState.lastError?.message?.let { message ->
                            Text(
                                text = message,
                                style = MaterialTheme.typography.bodySmall,
                                color = LocalContentColor.current.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
                
                Row {
                    if (errorState.recoveryAttempts < 3) {
                        TextButton(onClick = onRetry) {
                            Text("Retry")
                        }
                    }
                    
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Dismiss"
                        )
                    }
                }
            }
        }
    }
}

/**
 * Safe mode indicator
 */
@Composable
fun SafeModeIndicator(
    isInSafeMode: Boolean,
    onExitSafeMode: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isInSafeMode,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically(),
        modifier = modifier
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            color = MaterialTheme.colorScheme.warningContainer,
            shape = RoundedCornerShape(8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onWarningContainer,
                        modifier = Modifier.size(20.dp)
                    )
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Text(
                        text = "Safe Mode Active",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onWarningContainer
                    )
                }
                
                TextButton(
                    onClick = onExitSafeMode,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.onWarningContainer
                    )
                ) {
                    Text("Exit", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

/**
 * Error recovery dialog
 */
@Composable
fun GestureErrorRecoveryDialog(
    errorState: GestureErrorHandler.ErrorState,
    onResetSettings: () -> Unit,
    onContinueInSafeMode: () -> Unit,
    onDisableGestures: () -> Unit,
    onDismiss: () -> Unit
) {
    if (errorState.errorCount > 5 || errorState.lastError?.type?.severity == GestureErrorHandler.ErrorSeverity.CRITICAL) {
        AlertDialog(
            onDismissRequest = onDismiss,
            icon = {
                Icon(
                    imageVector = Icons.Default.ErrorOutline,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = {
                Text(
                    text = "Gesture System Error",
                    style = MaterialTheme.typography.headlineSmall
                )
            },
            text = {
                Column {
                    Text(
                        text = "The gesture system is experiencing issues. How would you like to proceed?",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    if (errorState.errorCount > 0) {
                        Text(
                            text = "${errorState.errorCount} errors in the last minute",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = onResetSettings) {
                    Text("Reset Settings")
                }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = onContinueInSafeMode) {
                        Text("Safe Mode")
                    }
                    TextButton(onClick = onDisableGestures) {
                        Text("Disable")
                    }
                }
            }
        )
    }
}

/**
 * Gesture health indicator
 */
@Composable
fun GestureHealthIndicator(
    errorState: GestureErrorHandler.ErrorState,
    modifier: Modifier = Modifier
) {
    val healthColor = when {
        errorState.gesturesDisabled -> Color.Red
        errorState.isInSafeMode -> Color.Yellow
        errorState.errorCount > 3 -> Color(0xFFFFA500) // Orange
        errorState.errorCount > 0 -> Color.Yellow
        else -> Color.Green
    }
    
    val healthText = when {
        errorState.gesturesDisabled -> "Disabled"
        errorState.isInSafeMode -> "Safe Mode"
        errorState.errorCount > 3 -> "Poor"
        errorState.errorCount > 0 -> "Fair"
        else -> "Good"
    }
    
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(healthColor, shape = RoundedCornerShape(4.dp))
        )
        
        Spacer(modifier = Modifier.width(8.dp))
        
        Text(
            text = "Gestures: $healthText",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Error details bottom sheet
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GestureErrorDetailsSheet(
    errorState: GestureErrorHandler.ErrorState,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    
    if (errorState.lastError != null) {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Error Details",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                // Error type
                DetailRow(
                    label = "Type",
                    value = errorState.lastError.type.name.replace("_", " ").lowercase()
                        .replaceFirstChar { it.uppercase() }
                )
                
                // Severity
                DetailRow(
                    label = "Severity",
                    value = errorState.lastError.type.severity.name,
                    valueColor = when (errorState.lastError.type.severity) {
                        GestureErrorHandler.ErrorSeverity.CRITICAL -> MaterialTheme.colorScheme.error
                        GestureErrorHandler.ErrorSeverity.HIGH -> MaterialTheme.colorScheme.tertiary
                        else -> LocalContentColor.current
                    }
                )
                
                // Message
                DetailRow(
                    label = "Message",
                    value = errorState.lastError.message
                )
                
                // Gesture type
                errorState.lastError.gestureType?.let { gestureType ->
                    DetailRow(
                        label = "Gesture",
                        value = gestureType.name.replace("_", " ").lowercase()
                            .replaceFirstChar { it.uppercase() }
                    )
                }
                
                // Recovery attempts
                if (errorState.recoveryAttempts > 0) {
                    DetailRow(
                        label = "Recovery Attempts",
                        value = "${errorState.recoveryAttempts}/3"
                    )
                }
                
                // Timestamp
                DetailRow(
                    label = "Time",
                    value = java.text.SimpleDateFormat(
                        "HH:mm:ss",
                        java.util.Locale.getDefault()
                    ).format(errorState.lastError.timestamp)
                )
                
                // Additional context
                if (errorState.lastError.context.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Additional Context",
                        style = MaterialTheme.typography.titleMedium
                    )
                    
                    errorState.lastError.context.forEach { (key, value) ->
                        DetailRow(label = key, value = value.toString())
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun DetailRow(
    label: String,
    value: String,
    valueColor: Color = LocalContentColor.current
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = valueColor,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f, fill = false)
        )
    }
}

/**
 * Auto-dismissing error toast
 */
@Composable
fun GestureErrorToast(
    message: String,
    severity: GestureErrorHandler.ErrorSeverity,
    durationMs: Long = 3000L,
    onDismiss: () -> Unit
) {
    var visible by remember { mutableStateOf(true) }
    
    LaunchedEffect(message) {
        delay(durationMs)
        visible = false
        delay(300) // Wait for exit animation
        onDismiss()
    }
    
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut()
    ) {
        Surface(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            color = when (severity) {
                GestureErrorHandler.ErrorSeverity.CRITICAL -> MaterialTheme.colorScheme.error
                GestureErrorHandler.ErrorSeverity.HIGH -> MaterialTheme.colorScheme.errorContainer
                else -> MaterialTheme.colorScheme.surfaceVariant
            },
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(
                text = message,
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

private fun getErrorTitle(errorState: GestureErrorHandler.ErrorState): String {
    return when {
        errorState.gesturesDisabled -> "Gestures Disabled"
        errorState.isInSafeMode -> "Safe Mode Active"
        else -> errorState.lastError?.type?.let { type ->
            when (type) {
                GestureErrorHandler.ErrorType.GESTURE_DETECTION_FAILED -> "Detection Failed"
                GestureErrorHandler.ErrorType.SETTINGS_CORRUPTION -> "Settings Error"
                GestureErrorHandler.ErrorType.PERFORMANCE_DEGRADATION -> "Performance Issue"
                GestureErrorHandler.ErrorType.MEMORY_PRESSURE -> "Memory Issue"
                GestureErrorHandler.ErrorType.GESTURE_CONFLICT -> "Gesture Conflict"
                else -> "Gesture Error"
            }
        } ?: "Unknown Error"
    }
}

// Extension for Material3 warning colors
val ColorScheme.warningContainer: Color
    @Composable
    get() = Color(0xFFFFF3CD)

val ColorScheme.onWarningContainer: Color
    @Composable
    get() = Color(0xFF856404)