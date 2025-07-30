package com.astralplayer.nextplayer.ui.dialogs

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.astralplayer.nextplayer.feature.playback.PlaybackSchedule
import com.astralplayer.nextplayer.feature.playback.PlaybackScheduler
import com.astralplayer.nextplayer.feature.playback.RepeatMode
import com.astralplayer.nextplayer.ui.components.*
import kotlinx.coroutines.flow.collectAsState
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * Playback Schedule Management Dialog
 * Allows users to view and manage scheduled video playback
 */
@Composable
fun PlaybackScheduleDialog(
    scheduler: PlaybackScheduler,
    currentVideoUri: String? = null,
    currentVideoTitle: String? = null,
    onDismiss: () -> Unit
) {
    val schedules by scheduler.getAllSchedules().collectAsState(initial = emptyList())
    var showAddSchedule by remember { mutableStateOf(false) }
    var editingSchedule by remember { mutableStateOf<PlaybackSchedule?>(null) }
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        )
    ) {
        BubbleCard(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.85f),
            elevation = 24,
            cornerRadius = 32,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Header
                ScheduleDialogHeader(
                    onClose = onDismiss,
                    onAdd = { showAddSchedule = true },
                    canAdd = currentVideoUri != null
                )
                
                if (schedules.isEmpty()) {
                    // Empty state
                    EmptyScheduleState(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp)
                    )
                } else {
                    // Schedule list
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentPadding = PaddingValues(24.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(schedules.sortedBy { it.scheduledTime }) { schedule ->
                            ScheduleItem(
                                schedule = schedule,
                                onToggle = { enabled ->
                                    scheduler.updateSchedule(
                                        schedule.id,
                                        enabled = enabled
                                    )
                                },
                                onEdit = { editingSchedule = schedule },
                                onDelete = {
                                    scheduler.cancelSchedule(schedule.id)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
    
    // Add/Edit schedule dialog
    if (showAddSchedule || editingSchedule != null) {
        AddEditScheduleDialog(
            scheduler = scheduler,
            existingSchedule = editingSchedule,
            defaultVideoUri = currentVideoUri,
            defaultVideoTitle = currentVideoTitle,
            onDismiss = {
                showAddSchedule = false
                editingSchedule = null
            }
        )
    }
}

@Composable
private fun ScheduleDialogHeader(
    onClose: () -> Unit,
    onAdd: () -> Unit,
    canAdd: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Schedule,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
            
            Text(
                text = "Scheduled Playback",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        }
        
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (canAdd) {
                BubbleIconButton(
                    onClick = onAdd,
                    icon = Icons.Default.Add,
                    size = 36,
                    iconSize = 20,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            }
            
            BubbleIconButton(
                onClick = onClose,
                icon = Icons.Default.Close,
                size = 36,
                iconSize = 20
            )
        }
    }
}

@Composable
private fun EmptyScheduleState(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.EventNote,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "No Scheduled Playback",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Text(
            text = "Schedule videos to play automatically at specific times",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
    }
}

@Composable
private fun ScheduleItem(
    schedule: PlaybackSchedule,
    onToggle: suspend (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: suspend () -> Unit
) {
    val scope = rememberCoroutineScope()
    val scheduledTime = LocalDateTime.parse(schedule.scheduledTime)
    val formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
    
    BubbleCard(
        elevation = 2,
        cornerRadius = 20,
        containerColor = if (schedule.enabled) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = schedule.videoTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.AccessTime,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Text(
                        text = scheduledTime.format(formatter),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        when (schedule.repeatMode) {
                            RepeatMode.ONCE -> Icons.Default.EventAvailable
                            RepeatMode.DAILY -> Icons.Default.Today
                            RepeatMode.WEEKLY -> Icons.Default.DateRange
                            RepeatMode.WEEKDAYS -> Icons.Default.Work
                            RepeatMode.WEEKENDS -> Icons.Default.Weekend
                        },
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    
                    Text(
                        text = when (schedule.repeatMode) {
                            RepeatMode.ONCE -> "Once"
                            RepeatMode.DAILY -> "Daily"
                            RepeatMode.WEEKLY -> "Weekly"
                            RepeatMode.WEEKDAYS -> "Weekdays"
                            RepeatMode.WEEKENDS -> "Weekends"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Toggle switch
                Switch(
                    checked = schedule.enabled,
                    onCheckedChange = { enabled ->
                        scope.launch { onToggle(enabled) }
                    }
                )
                
                // More options
                Box {
                    var showMenu by remember { mutableStateOf(false) }
                    
                    BubbleIconButton(
                        onClick = { showMenu = true },
                        icon = Icons.Default.MoreVert,
                        size = 32,
                        iconSize = 18
                    )
                    
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Edit") },
                            onClick = {
                                showMenu = false
                                onEdit()
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Edit, contentDescription = null)
                            }
                        )
                        
                        DropdownMenuItem(
                            text = { 
                                Text(
                                    "Delete",
                                    color = MaterialTheme.colorScheme.error
                                )
                            },
                            onClick = {
                                showMenu = false
                                scope.launch { onDelete() }
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddEditScheduleDialog(
    scheduler: PlaybackScheduler,
    existingSchedule: PlaybackSchedule?,
    defaultVideoUri: String?,
    defaultVideoTitle: String?,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val isEditing = existingSchedule != null
    
    var selectedDate by remember { 
        mutableStateOf(
            existingSchedule?.let { LocalDateTime.parse(it.scheduledTime).toLocalDate() }
                ?: LocalDateTime.now().toLocalDate()
        )
    }
    
    var selectedTime by remember {
        mutableStateOf(
            existingSchedule?.let { LocalDateTime.parse(it.scheduledTime).toLocalTime() }
                ?: LocalDateTime.now().toLocalTime()
        )
    }
    
    var repeatMode by remember { 
        mutableStateOf(existingSchedule?.repeatMode ?: RepeatMode.ONCE)
    }
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        BubbleCard(
            elevation = 16,
            cornerRadius = 24,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Header
                Text(
                    text = if (isEditing) "Edit Schedule" else "New Schedule",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                
                // Video info
                if (existingSchedule != null || defaultVideoTitle != null) {
                    BubbleCard(
                        elevation = 1,
                        cornerRadius = 12,
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.VideoLibrary,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            
                            Text(
                                text = existingSchedule?.videoTitle ?: defaultVideoTitle ?: "",
                                style = MaterialTheme.typography.bodyLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
                
                // Date & Time selection
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Date picker
                    BubbleCard(
                        onClick = { /* TODO: Show date picker */ },
                        elevation = 2,
                        cornerRadius = 12,
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.CalendarToday,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            
                            Text(
                                text = selectedDate.format(
                                    DateTimeFormatter.ofPattern("MMM dd, yyyy")
                                ),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                    
                    // Time picker
                    BubbleCard(
                        onClick = { /* TODO: Show time picker */ },
                        elevation = 2,
                        cornerRadius = 12,
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.AccessTime,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            
                            Text(
                                text = selectedTime.format(
                                    DateTimeFormatter.ofPattern("hh:mm a")
                                ),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
                
                // Repeat mode selection
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Repeat",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        RepeatMode.values().forEach { mode ->
                            BubbleChip(
                                text = when (mode) {
                                    RepeatMode.ONCE -> "Once"
                                    RepeatMode.DAILY -> "Daily"
                                    RepeatMode.WEEKLY -> "Weekly"
                                    RepeatMode.WEEKDAYS -> "Weekdays"
                                    RepeatMode.WEEKENDS -> "Weekends"
                                },
                                selected = repeatMode == mode,
                                onClick = { repeatMode = mode },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
                
                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    BubbleButton(
                        onClick = onDismiss,
                        text = "Cancel",
                        modifier = Modifier.weight(1f),
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    BubbleButton(
                        onClick = {
                            scope.launch {
                                val scheduledTime = LocalDateTime.of(selectedDate, selectedTime)
                                
                                if (isEditing && existingSchedule != null) {
                                    scheduler.updateSchedule(
                                        existingSchedule.id,
                                        scheduledTime = scheduledTime,
                                        repeatMode = repeatMode
                                    )
                                } else if (defaultVideoUri != null && defaultVideoTitle != null) {
                                    scheduler.schedulePlayback(
                                        videoUri = defaultVideoUri,
                                        videoTitle = defaultVideoTitle,
                                        scheduledTime = scheduledTime,
                                        repeatMode = repeatMode
                                    )
                                }
                                
                                onDismiss()
                            }
                        },
                        text = if (isEditing) "Update" else "Schedule",
                        icon = Icons.Default.Check,
                        modifier = Modifier.weight(1f),
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    }
}