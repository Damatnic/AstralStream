package com.astralplayer.features.gestures.ui

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.astralplayer.features.gestures.data.*
import com.astralplayer.features.gestures.viewmodel.GestureCustomizationViewModel
import com.astralplayer.features.gestures.viewmodel.GestureUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GestureCustomizationScreen(
    onNavigateBack: () -> Unit,
    onNavigateToGestureTest: () -> Unit,
    onNavigateToGestureTutorial: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: GestureCustomizationViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showProfileDialog by remember { mutableStateOf(false) }
    var showGestureEditDialog by remember { mutableStateOf(false) }
    var selectedGesture by remember { mutableStateOf<GestureEntity?>(null) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Gesture Customization") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToGestureTutorial) {
                        Icon(Icons.Default.Help, contentDescription = "Tutorial")
                    }
                    IconButton(onClick = onNavigateToGestureTest) {
                        Icon(Icons.Default.TouchApp, contentDescription = "Test Gestures")
                    }
                }
            )
        },
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Profile selector
            ProfileSelector(
                profiles = uiState.profiles,
                activeProfile = uiState.activeProfile,
                onProfileSelected = viewModel::activateProfile,
                onManageProfiles = { showProfileDialog = true }
            )
            
            // Gesture categories
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Grouped gestures by category
                val groupedGestures = uiState.gestures.groupBy { gesture ->
                    when (gesture.action) {
                        GestureAction.PLAY_PAUSE, GestureAction.SEEK_FORWARD, 
                        GestureAction.SEEK_BACKWARD, GestureAction.FAST_FORWARD,
                        GestureAction.REWIND, GestureAction.NEXT_VIDEO,
                        GestureAction.PREVIOUS_VIDEO -> "Playback"
                        
                        GestureAction.VOLUME_UP, GestureAction.VOLUME_DOWN,
                        GestureAction.MUTE_UNMUTE -> "Volume"
                        
                        GestureAction.BRIGHTNESS_UP, GestureAction.BRIGHTNESS_DOWN,
                        GestureAction.AUTO_BRIGHTNESS -> "Brightness"
                        
                        GestureAction.SPEED_UP, GestureAction.SPEED_DOWN,
                        GestureAction.RESET_SPEED, GestureAction.CYCLE_SPEED_PRESETS -> "Speed"
                        
                        GestureAction.TOGGLE_SUBTITLES, GestureAction.NEXT_SUBTITLE_TRACK,
                        GestureAction.PREVIOUS_SUBTITLE_TRACK, GestureAction.SUBTITLE_DELAY_INCREASE,
                        GestureAction.SUBTITLE_DELAY_DECREASE -> "Subtitles"
                        
                        GestureAction.ZOOM_IN, GestureAction.ZOOM_OUT,
                        GestureAction.RESET_ZOOM, GestureAction.FIT_TO_SCREEN -> "Zoom"
                        
                        else -> "Other"
                    }
                }
                
                groupedGestures.forEach { (category, gestures) ->
                    item {
                        GestureCategoryHeader(
                            category = category,
                            gestureCount = gestures.size
                        )
                    }
                    
                    items(gestures) { gesture ->
                        GestureCard(
                            gesture = gesture,
                            onClick = {
                                selectedGesture = gesture
                                showGestureEditDialog = true
                            },
                            onToggleEnabled = {
                                viewModel.toggleGestureEnabled(gesture.id)
                            }
                        )
                    }
                }
                
                // Quick actions section
                item {
                    QuickActionsSection(
                        onResetToDefaults = viewModel::resetToDefaults,
                        onExportSettings = viewModel::exportSettings,
                        onImportSettings = viewModel::importSettings
                    )
                }
            }
        }
    }
    
    // Profile management dialog
    if (showProfileDialog) {
        ProfileManagementDialog(
            profiles = uiState.profiles,
            activeProfile = uiState.activeProfile,
            onDismiss = { showProfileDialog = false },
            onCreateProfile = viewModel::createProfile,
            onDuplicateProfile = viewModel::duplicateProfile,
            onDeleteProfile = viewModel::deleteProfile,
            onActivateProfile = viewModel::activateProfile
        )
    }
    
    // Gesture edit dialog
    if (showGestureEditDialog && selectedGesture != null) {
        GestureEditDialog(
            gesture = selectedGesture!!,
            onDismiss = { 
                showGestureEditDialog = false
                selectedGesture = null
            },
            onSave = { updatedGesture ->
                viewModel.updateGesture(updatedGesture)
                showGestureEditDialog = false
                selectedGesture = null
            }
        )
    }
}

@Composable
private fun ProfileSelector(
    profiles: List<GestureProfileEntity>,
    activeProfile: GestureProfileEntity?,
    onProfileSelected: (String) -> Unit,
    onManageProfiles: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Active Profile",
                    style = MaterialTheme.typography.labelMedium
                )
                Text(
                    text = activeProfile?.name ?: "None",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                activeProfile?.description?.let { desc ->
                    if (desc.isNotBlank()) {
                        Text(
                            text = desc,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Quick profile switcher
                profiles.filter { it.id != activeProfile?.id }.take(3).forEach { profile ->
                    AssistChip(
                        onClick = { onProfileSelected(profile.id) },
                        label = { Text(profile.name) },
                        modifier = Modifier.height(32.dp)
                    )
                }
                
                IconButton(onClick = onManageProfiles) {
                    Icon(Icons.Default.Settings, contentDescription = "Manage profiles")
                }
            }
        }
    }
}

@Composable
private fun GestureCategoryHeader(
    category: String,
    gestureCount: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = category,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "$gestureCount gestures",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GestureCard(
    gesture: GestureEntity,
    onClick: () -> Unit,
    onToggleEnabled: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
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
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Gesture icon
                GestureIcon(
                    gestureType = gesture.gestureType,
                    modifier = Modifier.size(48.dp)
                )
                
                Column {
                    Text(
                        text = formatGestureType(gesture.gestureType),
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        text = "${formatZone(gesture.zone)} → ${formatAction(gesture.action)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (gesture.sensitivity != 1.0f) {
                        Text(
                            text = "Sensitivity: ${(gesture.sensitivity * 100).toInt()}%",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            
            Switch(
                checked = gesture.isEnabled,
                onCheckedChange = { onToggleEnabled() }
            )
        }
    }
}

@Composable
private fun GestureIcon(
    gestureType: GestureType,
    modifier: Modifier = Modifier
) {
    val icon = when (gestureType) {
        GestureType.TAP -> Icons.Default.TouchApp
        GestureType.DOUBLE_TAP -> Icons.Default.DoubleArrow
        GestureType.LONG_PRESS -> Icons.Default.Timer
        GestureType.SWIPE_UP -> Icons.Default.SwipeUp
        GestureType.SWIPE_DOWN -> Icons.Default.SwipeDown
        GestureType.SWIPE_LEFT -> Icons.Default.SwipeLeft
        GestureType.SWIPE_RIGHT -> Icons.Default.SwipeRight
        GestureType.PINCH_IN, GestureType.PINCH_OUT -> Icons.Default.ZoomIn
        GestureType.TWO_FINGER_TAP -> Icons.Default.Gesture
        else -> Icons.Default.TouchApp
    }
    
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
private fun QuickActionsSection(
    onResetToDefaults: () -> Unit,
    onExportSettings: () -> Unit,
    onImportSettings: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Quick Actions",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onResetToDefaults,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.RestartAlt, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Reset")
                }
                
                OutlinedButton(
                    onClick = onExportSettings,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Upload, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Export")
                }
                
                OutlinedButton(
                    onClick = onImportSettings,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Download, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Import")
                }
            }
        }
    }
}

// Helper functions
private fun formatGestureType(type: GestureType): String {
    return type.name.replace('_', ' ').lowercase()
        .split(' ').joinToString(" ") { it.capitalize() }
}

private fun formatZone(zone: GestureZone): String {
    return zone.name.replace('_', ' ').lowercase()
        .split(' ').joinToString(" ") { it.capitalize() }
}

private fun formatAction(action: GestureAction): String {
    return action.name.replace('_', ' ').lowercase()
        .split(' ').joinToString(" ") { it.capitalize() }
}