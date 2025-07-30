package com.astralplayer.nextplayer.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.astralplayer.nextplayer.data.PlayerError
import com.astralplayer.nextplayer.data.ErrorRecoveryResult

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ErrorDialog(
    error: PlayerError,
    recoveryResult: ErrorRecoveryResult,
    onDismiss: () -> Unit,
    onRetry: () -> Unit = {},
    onAction: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier
    ) {
        Surface(
            shape = MaterialTheme.shapes.large,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Error icon
                Icon(
                    imageVector = when (error) {
                        is PlayerError.NetworkError -> Icons.Default.WifiOff
                        is PlayerError.VideoLoadError -> Icons.Default.PlayArrow
                        is PlayerError.PermissionError -> Icons.Default.Security
                        is PlayerError.StorageError -> Icons.Default.Storage
                        else -> Icons.Default.Error
                    },
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.error
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Error title
                Text(
                    text = when (error) {
                        is PlayerError.NetworkError -> "Network Error"
                        is PlayerError.VideoLoadError -> "Video Load Error"
                        is PlayerError.PermissionError -> "Permission Required"
                        is PlayerError.StorageError -> "Storage Error"
                        is PlayerError.DatabaseError -> "Database Error"
                        else -> "Error"
                    },
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Error message
                Text(
                    text = when (recoveryResult) {
                        is ErrorRecoveryResult.Retry -> recoveryResult.message
                        is ErrorRecoveryResult.Fallback -> recoveryResult.message
                        is ErrorRecoveryResult.UserAction -> recoveryResult.message
                        is ErrorRecoveryResult.Fatal -> recoveryResult.message
                        is ErrorRecoveryResult.Ignore -> recoveryResult.message
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
                ) {
                    when (recoveryResult) {
                        is ErrorRecoveryResult.Retry -> {
                            TextButton(onClick = onDismiss) {
                                Text("Cancel")
                            }
                            Button(onClick = {
                                onRetry()
                                onDismiss()
                            }) {
                                Text("Retry")
                            }
                        }
                        is ErrorRecoveryResult.UserAction -> {
                            TextButton(onClick = onDismiss) {
                                Text("Cancel")
                            }
                            Button(onClick = {
                                onAction()
                                onDismiss()
                            }) {
                                Text(recoveryResult.actionText)
                            }
                        }
                        is ErrorRecoveryResult.Fatal -> {
                            Button(
                                onClick = onDismiss,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error
                                )
                            ) {
                                Text("OK")
                            }
                        }
                        else -> {
                            Button(onClick = onDismiss) {
                                Text("OK")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ErrorSnackbar(
    error: PlayerError,
    recoveryResult: ErrorRecoveryResult,
    onRetry: () -> Unit = {},
    onAction: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Error,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onErrorContainer
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = error.message ?: "An error occurred",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
            
            when (recoveryResult) {
                is ErrorRecoveryResult.Retry -> {
                    TextButton(onClick = onRetry) {
                        Text(
                            "Retry",
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
                is ErrorRecoveryResult.UserAction -> {
                    TextButton(onClick = onAction) {
                        Text(
                            recoveryResult.actionText,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
                else -> {}
            }
        }
    }
}

@Composable
fun LoadingErrorScreen(
    error: PlayerError,
    onRetry: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = when (error) {
                is PlayerError.NetworkError -> Icons.Default.WifiOff
                is PlayerError.VideoLoadError -> Icons.Default.PlayArrow
                else -> Icons.Default.Error
            },
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.error
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "Failed to load video",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = error.message ?: "An unknown error occurred",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedButton(onClick = onBack) {
                Text("Go Back")
            }
            
            Button(onClick = onRetry) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Retry")
            }
        }
    }
}