package com.astralplayer.nextplayer.ui.dialogs

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.astralplayer.nextplayer.feature.subtitle.*
import com.astralplayer.nextplayer.ui.components.*
import kotlinx.coroutines.launch

/**
 * Subtitle Selection Dialog with AI Features
 * Bubble-themed dialog for selecting, generating, and managing subtitles
 */
@Composable
fun SubtitleSelectionDialog(
    subtitleManager: AdvancedSubtitleManager,
    onDismiss: () -> Unit,
    onSubtitleSelected: (SubtitleTrack?) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // State
    val subtitles by subtitleManager.currentSubtitles.collectAsState()
    val activeSubtitle by subtitleManager.activeSubtitle.collectAsState()
    val isGenerating by subtitleManager.isGeneratingSubtitles.collectAsState()
    val generationProgress by subtitleManager.generationProgress.collectAsState()
    val isTranslating by subtitleManager.isTranslating.collectAsState()
    
    var selectedTab by remember { mutableStateOf(SubtitleTab.AVAILABLE) }
    var showLanguageSelector by remember { mutableStateOf(false) }
    var showOnlineSearch by remember { mutableStateOf(false) }
    var selectedForTranslation by remember { mutableStateOf<SubtitleTrack?>(null) }
    
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
                .fillMaxHeight(0.8f),
            elevation = 24,
            cornerRadius = 32,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Header
                SubtitleDialogHeader(
                    onClose = onDismiss
                )
                
                // Tabs
                SubtitleTabs(
                    selectedTab = selectedTab,
                    onTabSelected = { selectedTab = it }
                )
                
                // Content
                AnimatedContent(
                    targetState = selectedTab,
                    transitionSpec = {
                        slideInHorizontally { it } with slideOutHorizontally { -it }
                    }
                ) { tab ->
                    when (tab) {
                        SubtitleTab.AVAILABLE -> {
                            AvailableSubtitlesContent(
                                subtitles = subtitles,
                                activeSubtitle = activeSubtitle,
                                onSubtitleSelected = { subtitle ->
                                    subtitleManager.activateSubtitle(subtitle)
                                    onSubtitleSelected(subtitle)
                                },
                                onTranslateRequested = { subtitle ->
                                    selectedForTranslation = subtitle
                                    showLanguageSelector = true
                                }
                            )
                        }
                        
                        SubtitleTab.AI_GENERATE -> {
                            AIGenerateContent(
                                isGenerating = isGenerating,
                                generationProgress = generationProgress,
                                onGenerateClick = { language ->
                                    scope.launch {
                                        val result = subtitleManager.generateSubtitlesWithAI(
                                            language = language,
                                            options = AIGenerationOptions(
                                                accuracy = AIAccuracy.HIGH,
                                                includePunctuation = true,
                                                includeTimestamps = true
                                            )
                                        )
                                        result.onSuccess { subtitle ->
                                            subtitleManager.activateSubtitle(subtitle)
                                            onSubtitleSelected(subtitle)
                                        }
                                    }
                                }
                            )
                        }
                        
                        SubtitleTab.ONLINE_SEARCH -> {
                            OnlineSearchContent(
                                onSearchClick = { showOnlineSearch = true }
                            )
                        }
                        
                        SubtitleTab.SETTINGS -> {
                            SubtitleSettingsContent(
                                subtitleManager = subtitleManager
                            )
                        }
                    }
                }
                
                // No subtitles option
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    BubbleButton(
                        onClick = {
                            subtitleManager.activateSubtitle(null)
                            onSubtitleSelected(null)
                            onDismiss()
                        },
                        text = "No Subtitles",
                        icon = Icons.Default.SubtitlesOff,
                        modifier = Modifier.fillMaxWidth(),
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
    
    // Language selector dialog
    if (showLanguageSelector && selectedForTranslation != null) {
        LanguageSelectorDialog(
            onLanguageSelected = { language ->
                scope.launch {
                    selectedForTranslation?.let { subtitle ->
                        subtitleManager.translateSubtitles(subtitle, language)
                    }
                }
                showLanguageSelector = false
            },
            onDismiss = { showLanguageSelector = false }
        )
    }
    
    // Online search dialog
    if (showOnlineSearch) {
        OnlineSubtitleSearchDialog(
            subtitleManager = subtitleManager,
            onDismiss = { showOnlineSearch = false }
        )
    }
}

@Composable
private fun SubtitleDialogHeader(
    onClose: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Subtitles",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        
        BubbleIconButton(
            onClick = onClose,
            icon = Icons.Default.Close,
            size = 36,
            iconSize = 20
        )
    }
}

@Composable
private fun SubtitleTabs(
    selectedTab: SubtitleTab,
    onTabSelected: (SubtitleTab) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SubtitleTab.values().forEach { tab ->
            BubbleChip(
                text = tab.title,
                onClick = { onTabSelected(tab) },
                selected = selectedTab == tab,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun AvailableSubtitlesContent(
    subtitles: List<SubtitleTrack>,
    activeSubtitle: SubtitleTrack?,
    onSubtitleSelected: (SubtitleTrack) -> Unit,
    onTranslateRequested: (SubtitleTrack) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(subtitles) { subtitle ->
            SubtitleTrackItem(
                subtitle = subtitle,
                isActive = subtitle.id == activeSubtitle?.id,
                onSelect = { onSubtitleSelected(subtitle) },
                onTranslate = { onTranslateRequested(subtitle) }
            )
        }
        
        if (subtitles.isEmpty()) {
            item {
                EmptySubtitlesState()
            }
        }
    }
}

@Composable
private fun SubtitleTrackItem(
    subtitle: SubtitleTrack,
    isActive: Boolean,
    onSelect: () -> Unit,
    onTranslate: () -> Unit
) {
    val animatedScale by animateFloatAsState(
        targetValue = if (isActive) 1.02f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        )
    )
    
    BubbleCard(
        modifier = Modifier
            .fillMaxWidth()
            .scale(animatedScale),
        onClick = onSelect,
        elevation = if (isActive) 8 else 4,
        cornerRadius = 16,
        containerColor = if (isActive) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surface
        }
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
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Type icon
                Icon(
                    imageVector = when (subtitle.type) {
                        SubtitleType.EMBEDDED -> Icons.Default.VideoFile
                        SubtitleType.EXTERNAL -> Icons.Default.AttachFile
                        SubtitleType.AI_GENERATED -> Icons.Default.AutoAwesome
                        SubtitleType.AI_TRANSLATED -> Icons.Default.Translate
                        SubtitleType.DOWNLOADED -> Icons.Default.Download
                    },
                    contentDescription = null,
                    tint = if (isActive) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.size(24.dp)
                )
                
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = subtitle.label,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = if (isActive) FontWeight.Medium else FontWeight.Normal,
                        color = if (isActive) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Language
                        Text(
                            text = subtitle.language.displayName,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (isActive) {
                                MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                        
                        // Format
                        Text(
                            text = "• ${subtitle.format.extension.uppercase()}",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (isActive) {
                                MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }
            }
            
            // Actions
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (subtitle.type != SubtitleType.AI_TRANSLATED) {
                    IconButton(
                        onClick = onTranslate,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.Default.Translate,
                            contentDescription = "Translate",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                
                if (isActive) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = "Active",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun AIGenerateContent(
    isGenerating: Boolean,
    generationProgress: Float,
    onGenerateClick: (Language) -> Unit
) {
    var selectedLanguage by remember { mutableStateOf(Language.ENGLISH) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // AI illustration
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.secondary
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(60.dp)
            )
        }
        
        Text(
            text = "Generate Subtitles with AI",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        
        Text(
            text = "Our AI will analyze the audio and generate accurate subtitles in your selected language.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        
        // Language selector
        BubbleDropdownMenu(
            label = "Select Language",
            selectedValue = selectedLanguage.displayName,
            options = Language.getSupportedLanguages(),
            onOptionSelected = { selectedLanguage = it },
            optionLabel = { it.displayName }
        )
        
        Spacer(modifier = Modifier.weight(1f))
        
        // Generate button or progress
        if (isGenerating) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                CircularProgressIndicator(
                    progress = { generationProgress },
                    modifier = Modifier.size(48.dp),
                    strokeWidth = 3.dp
                )
                
                Text(
                    text = "Generating subtitles... ${(generationProgress * 100).toInt()}%",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            BubbleButton(
                onClick = { onGenerateClick(selectedLanguage) },
                text = "Generate Subtitles",
                icon = Icons.Default.AutoAwesome,
                modifier = Modifier.fillMaxWidth(),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
}

@Composable
private fun OnlineSearchContent(
    onSearchClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Search illustration
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF4CAF50),
                            Color(0xFF8BC34A)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.CloudDownload,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(60.dp)
            )
        }
        
        Text(
            text = "Search Online Subtitles",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        
        Text(
            text = "Find and download subtitles from popular online databases.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        
        Spacer(modifier = Modifier.weight(1f))
        
        BubbleButton(
            onClick = onSearchClick,
            text = "Search Subtitles",
            icon = Icons.Default.Search,
            modifier = Modifier.fillMaxWidth(),
            containerColor = Color(0xFF4CAF50),
            contentColor = Color.White
        )
    }
}

@Composable
private fun SubtitleSettingsContent(
    subtitleManager: AdvancedSubtitleManager
) {
    val subtitleStyle by subtitleManager.subtitleStyle.collectAsState()
    val subtitleDelay by subtitleManager.subtitleDelay.collectAsState()
    
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Subtitle Appearance",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }
        
        // Preset styles
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StylePresetChip(
                    text = "Default",
                    isSelected = subtitleStyle == SubtitleStyle.Default,
                    onClick = { subtitleManager.updateSubtitleStyle(SubtitleStyle.Default) }
                )
                
                StylePresetChip(
                    text = "Large",
                    isSelected = subtitleStyle == SubtitleStyle.LargeText,
                    onClick = { subtitleManager.updateSubtitleStyle(SubtitleStyle.LargeText) }
                )
                
                StylePresetChip(
                    text = "High Contrast",
                    isSelected = subtitleStyle == SubtitleStyle.HighContrast,
                    onClick = { subtitleManager.updateSubtitleStyle(SubtitleStyle.HighContrast) }
                )
            }
        }
        
        item {
            Text(
                text = "Subtitle Timing",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }
        
        // Delay adjustment
        item {
            DelayAdjustmentCard(
                currentDelay = subtitleDelay,
                onDelayChanged = { subtitleManager.adjustSubtitleDelay(it) }
            )
        }
    }
}

@Composable
private fun EmptySubtitlesState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            Icons.Default.Subtitles,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.size(64.dp)
        )
        
        Text(
            text = "No subtitles available",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Text(
            text = "Try generating with AI or searching online",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
    }
}

@Composable
private fun StylePresetChip(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    BubbleChip(
        text = text,
        selected = isSelected,
        onClick = onClick,
        modifier = Modifier.weight(1f)
    )
}

@Composable
private fun DelayAdjustmentCard(
    currentDelay: Long,
    onDelayChanged: (Long) -> Unit
) {
    BubbleCard(
        elevation = 4,
        cornerRadius = 16
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Sync Adjustment",
                    style = MaterialTheme.typography.bodyLarge
                )
                
                Text(
                    text = "${currentDelay}ms",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                BubbleButton(
                    onClick = { onDelayChanged(currentDelay - 500) },
                    text = "-500ms",
                    modifier = Modifier.weight(1f),
                    cornerRadius = 12,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
                
                BubbleButton(
                    onClick = { onDelayChanged(currentDelay - 100) },
                    text = "-100ms",
                    modifier = Modifier.weight(1f),
                    cornerRadius = 12,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
                
                BubbleButton(
                    onClick = { onDelayChanged(0) },
                    text = "Reset",
                    modifier = Modifier.weight(1f),
                    cornerRadius = 12,
                    containerColor = MaterialTheme.colorScheme.primary
                )
                
                BubbleButton(
                    onClick = { onDelayChanged(currentDelay + 100) },
                    text = "+100ms",
                    modifier = Modifier.weight(1f),
                    cornerRadius = 12,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
                
                BubbleButton(
                    onClick = { onDelayChanged(currentDelay + 500) },
                    text = "+500ms",
                    modifier = Modifier.weight(1f),
                    cornerRadius = 12,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }
        }
    }
}

enum class SubtitleTab(val title: String) {
    AVAILABLE("Available"),
    AI_GENERATE("AI Generate"),
    ONLINE_SEARCH("Online"),
    SETTINGS("Settings")
}

// Additional helper dialogs

@Composable
private fun LanguageSelectorDialog(
    onLanguageSelected: (Language) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        BubbleCard(
            elevation = 16,
            cornerRadius = 24
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Select Translation Language",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                
                LazyColumn(
                    modifier = Modifier.heightIn(max = 400.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(Language.getSupportedLanguages()) { language ->
                        BubbleCard(
                            onClick = {
                                onLanguageSelected(language)
                                onDismiss()
                            },
                            elevation = 2,
                            cornerRadius = 12,
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = language.displayName,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                
                                Text(
                                    text = language.nativeName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OnlineSubtitleSearchDialog(
    subtitleManager: AdvancedSubtitleManager,
    onDismiss: () -> Unit
) {
    // Implementation for online subtitle search
    // This would include search field, results list, etc.
    Dialog(onDismissRequest = onDismiss) {
        BubbleCard(
            elevation = 16,
            cornerRadius = 24
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Search Online Subtitles",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                
                // Search implementation would go here
                Text(
                    text = "Online subtitle search coming soon!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}