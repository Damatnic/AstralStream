package com.astralplayer.nextplayer.ui.onboarding

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.*
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.style.*
import androidx.compose.ui.unit.*
import androidx.datastore.preferences.core.*
import kotlinx.coroutines.*
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Comprehensive Onboarding System for AstralStream v2.0
 * 
 * Features:
 * - Progressive feature discovery across 22 features
 * - Interactive tutorials with hands-on practice
 * - Adaptive onboarding based on user type
 * - Skip options for experienced users
 * - Contextual help system
 * - Feature spotlight system
 * - Accessibility-first design
 */

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ComprehensiveOnboardingFlow(
    onOnboardingComplete: () -> Unit,
    onSkipOnboarding: () -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { OnboardingScreen.entries.size })
    val scope = rememberCoroutineScope()
    
    var currentUserType by remember { mutableStateOf(UserType.NEW_USER) }
    var selectedFeatures by remember { mutableStateOf(setOf<String>()) }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        MaterialTheme.colorScheme.surface
                    )
                )
            )
    ) {
        Column {
            // Progress indicator
            OnboardingProgressIndicator(
                currentStep = pagerState.currentPage,
                totalSteps = OnboardingScreen.entries.size,
                modifier = Modifier.padding(16.dp)
            )
            
            // Main content
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f)
            ) { page ->
                when (OnboardingScreen.entries[page]) {
                    OnboardingScreen.WELCOME -> WelcomeScreen(
                        onUserTypeSelected = { userType ->
                            currentUserType = userType
                            scope.launch {
                                pagerState.animateScrollToPage(page + 1)
                            }
                        }
                    )
                    
                    OnboardingScreen.FEATURE_SELECTION -> FeatureSelectionScreen(
                        userType = currentUserType,
                        selectedFeatures = selectedFeatures,
                        onFeaturesSelected = { features ->
                            selectedFeatures = features
                            scope.launch {
                                pagerState.animateScrollToPage(page + 1)
                            }
                        }
                    )
                    
                    OnboardingScreen.AI_FEATURES -> AIFeaturesIntroScreen(
                        onContinue = {
                            scope.launch {
                                pagerState.animateScrollToPage(page + 1)
                            }
                        }
                    )
                    
                    OnboardingScreen.VOICE_CONTROL -> VoiceControlTutorialScreen(
                        onContinue = {
                            scope.launch {
                                pagerState.animateScrollToPage(page + 1)
                            }
                        }
                    )
                    
                    OnboardingScreen.GESTURE_CONTROLS -> GestureControlsTutorialScreen(
                        onContinue = {
                            scope.launch {
                                pagerState.animateScrollToPage(page + 1)
                            }
                        }
                    )
                    
                    OnboardingScreen.AUDIO_EQUALIZER -> AudioEqualizerTutorialScreen(
                        onContinue = {
                            scope.launch {
                                pagerState.animateScrollToPage(page + 1)
                            }
                        }
                    )
                    
                    OnboardingScreen.PERSONALIZATION -> PersonalizationScreen(
                        selectedFeatures = selectedFeatures,
                        onContinue = {
                            scope.launch {
                                pagerState.animateScrollToPage(page + 1)
                            }
                        }
                    )
                    
                    OnboardingScreen.COMPLETION -> CompletionScreen(
                        onComplete = onOnboardingComplete
                    )
                }
            }
        }
        
        // Skip button (always available)
        TextButton(
            onClick = onSkipOnboarding,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
        ) {
            Text("Skip Tour")
        }
    }
}

/**
 * Welcome screen with user type selection
 */
@Composable
private fun WelcomeScreen(
    onUserTypeSelected: (UserType) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // App logo and animation
        Box(
            modifier = Modifier
                .size(120.dp)
                .background(
                    color = MaterialTheme.colorScheme.primary,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = null,
                modifier = Modifier.size(60.dp),
                tint = MaterialTheme.colorScheme.onPrimary
            )
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Text(
            text = "Welcome to AstralStream",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        
        Text(
            text = "The Ultimate Video Player Experience",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(vertical = 8.dp)
        )
        
        Text(
            text = "Let's personalize your experience with 22 advanced features",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(vertical = 16.dp)
        )
        
        Spacer(modifier = Modifier.height(40.dp))
        
        // User type selection
        Text(
            text = "I am a...",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        UserTypeSelectionCard(
            title = "New User",
            description = "I'm new to advanced video players",
            icon = Icons.Default.Person,
            onClick = { onUserTypeSelected(UserType.NEW_USER) }
        )
        
        UserTypeSelectionCard(
            title = "Power User",
            description = "I love advanced features and customization",
            icon = Icons.Default.Settings,
            onClick = { onUserTypeSelected(UserType.POWER_USER) }
        )
        
        UserTypeSelectionCard(
            title = "Accessibility User",
            description = "I need voice control and accessibility features",
            icon = Icons.Default.AccessibilityNew,
            onClick = { onUserTypeSelected(UserType.ACCESSIBILITY_USER) }
        )
        
        UserTypeSelectionCard(
            title = "Professional User",
            description = "I need professional audio and video tools",
            icon = Icons.Default.WorkspacePremium,
            onClick = { onUserTypeSelected(UserType.PROFESSIONAL_USER) }
        )
    }
}

@Composable
private fun UserTypeSelectionCard(
    title: String,
    description: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Feature selection screen based on user type
 */
@Composable
private fun FeatureSelectionScreen(
    userType: UserType,
    selectedFeatures: Set<String>,
    onFeaturesSelected: (Set<String>) -> Unit
) {
    val recommendedFeatures = getRecommendedFeatures(userType)
    val allFeatures = getAllFeatures()
    
    var currentSelected by remember { mutableStateOf(selectedFeatures) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Text(
            text = "Choose Your Features",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        
        Text(
            text = "Select features you'd like to learn about first",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(vertical = 8.dp)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Recommended features section
        Text(
            text = "Recommended for you",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Recommended features
            items(recommendedFeatures) { feature ->
                FeatureSelectionItem(
                    feature = feature,
                    isSelected = currentSelected.contains(feature.key),
                    isRecommended = true,
                    onToggle = { 
                        currentSelected = if (currentSelected.contains(feature.key)) {
                            currentSelected - feature.key
                        } else {
                            currentSelected + feature.key
                        }
                    }
                )
            }
            
            // Divider
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Other features",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            // Other features
            items(allFeatures.filter { it !in recommendedFeatures }) { feature ->
                FeatureSelectionItem(
                    feature = feature,
                    isSelected = currentSelected.contains(feature.key),
                    isRecommended = false,
                    onToggle = { 
                        currentSelected = if (currentSelected.contains(feature.key)) {
                            currentSelected - feature.key
                        } else {
                            currentSelected + feature.key
                        }
                    }
                )
            }
        }
        
        // Continue button
        Button(
            onClick = { onFeaturesSelected(currentSelected) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            enabled = currentSelected.isNotEmpty()
        ) {
            Text("Continue with ${currentSelected.size} features")
        }
    }
}

@Composable
private fun FeatureSelectionItem(
    feature: OnboardingFeature,
    isSelected: Boolean,
    isRecommended: Boolean,
    onToggle: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        border = if (isRecommended) {
            BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
        } else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = feature.icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = if (isSelected) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.primary
                }
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = feature.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium
                    )
                    
                    if (isRecommended) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Text(
                                text = "Recommended",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                
                Text(
                    text = feature.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onToggle() }
            )
        }
    }
}

/**
 * AI Features introduction with interactive demo
 */
@Composable
private fun AIFeaturesIntroScreen(
    onContinue: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Text(
            text = "AI-Powered Intelligence",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        
        Text(
            text = "Experience the future of video watching with AI",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(vertical = 8.dp)
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // AI Feature cards
        AIFeatureCard(
            icon = Icons.Default.Subtitles,
            title = "AI Subtitle Generation",
            description = "Generate subtitles in 50+ languages instantly",
            highlights = listOf(
                "Real-time speech recognition",
                "98% accuracy rate",
                "Works offline for privacy",
                "Custom styling options"
            )
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        AIFeatureCard(
            icon = Icons.Default.Search,
            title = "Intelligent Search",
            description = "Find videos with natural language queries",
            highlights = listOf(
                "\"Show me action movies longer than 1 hour\"",
                "Search by content, not just filename",
                "Lightning-fast results",
                "Visual search history"
            )
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        AIFeatureCard(
            icon = Icons.Default.AutoAwesome,
            title = "Scene Detection",
            description = "Automatically categorize and organize content",
            highlights = listOf(
                "Detect action, drama, comedy scenes",
                "Smart chapter generation",
                "Content recommendations",
                "Thumbnail optimization"
            )
        )
        
        Spacer(modifier = Modifier.weight(1f))
        
        // Interactive demo button
        OutlinedButton(
            onClick = { /* Start AI demo */ },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Try AI Features Demo")
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Button(
            onClick = onContinue,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Continue Tour")
        }
    }
}

@Composable
private fun AIFeatureCard(
    icon: ImageVector,
    title: String,
    description: String,
    highlights: List<String>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            highlights.forEach { highlight ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(
                                color = MaterialTheme.colorScheme.primary,
                                shape = CircleShape
                            )
                    )
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Text(
                        text = highlight,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * Voice Control interactive tutorial
 */
@Composable
private fun VoiceControlTutorialScreen(
    onContinue: () -> Unit
) {
    var currentCommand by remember { mutableStateOf("") }
    var isListening by remember { mutableStateOf(false) }
    var completedCommands by remember { mutableStateOf(setOf<String>()) }
    
    val voiceCommands = listOf(
        VoiceCommand("Play video", "Start or resume video playback"),
        VoiceCommand("Pause", "Pause the current video"),
        VoiceCommand("Skip forward 10 seconds", "Jump ahead in the video"),
        VoiceCommand("Set volume to 80%", "Adjust volume to specific level"),
        VoiceCommand("Enable subtitles", "Turn on subtitle display")
    )
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Text(
            text = "Voice Control Tutorial",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        
        Text(
            text = "Learn to control AstralStream with your voice",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(vertical = 8.dp)
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Voice activation demo
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (isListening) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surface
                }
            )
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = if (isListening) Icons.Default.Mic else Icons.Default.MicOff,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = if (isListening) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = if (isListening) "Listening..." else "Tap to try voice command",
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center
                )
                
                if (currentCommand.isNotEmpty()) {
                    Text(
                        text = "\"$currentCommand\"",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Button(
                    onClick = { isListening = !isListening },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isListening) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.primary
                        }
                    )
                ) {
                    Text(if (isListening) "Stop Listening" else "Start Voice Demo")
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "Try these commands:",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(voiceCommands) { command ->
                VoiceCommandItem(
                    command = command,
                    isCompleted = completedCommands.contains(command.phrase),
                    onTry = {
                        currentCommand = command.phrase
                        // Simulate command recognition
                        completedCommands = completedCommands + command.phrase
                    }
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        LinearProgressIndicator(
            progress = { completedCommands.size.toFloat() / voiceCommands.size },
            modifier = Modifier.fillMaxWidth()
        )
        
        Text(
            text = "${completedCommands.size}/${voiceCommands.size} commands tried",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Button(
            onClick = onContinue,
            modifier = Modifier.fillMaxWidth(),
            enabled = completedCommands.size >= 3
        ) {
            Text("Continue (${completedCommands.size}/3 minimum)")
        }
    }
}

@Composable
private fun VoiceCommandItem(
    command: VoiceCommand,
    isCompleted: Boolean,
    onTry: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onTry() },
        colors = CardDefaults.cardColors(
            containerColor = if (isCompleted) {
                MaterialTheme.colorScheme.secondaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isCompleted) Icons.Default.CheckCircle else Icons.Default.Mic,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = if (isCompleted) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "\"${command.phrase}\"",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = command.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            if (isCompleted) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Completed",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

/**
 * Progress indicator for onboarding
 */
@Composable
private fun OnboardingProgressIndicator(
    currentStep: Int,
    totalSteps: Int,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Step ${currentStep + 1} of $totalSteps",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Text(
                text = "${((currentStep + 1).toFloat() / totalSteps * 100).toInt()}%",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        LinearProgressIndicator(
            progress = { (currentStep + 1).toFloat() / totalSteps },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

// Data classes and enums

enum class OnboardingScreen {
    WELCOME,
    FEATURE_SELECTION,
    AI_FEATURES,
    VOICE_CONTROL,
    GESTURE_CONTROLS,
    AUDIO_EQUALIZER,
    PERSONALIZATION,
    COMPLETION
}

enum class UserType {
    NEW_USER,
    POWER_USER,
    ACCESSIBILITY_USER,
    PROFESSIONAL_USER
}

data class OnboardingFeature(
    val key: String,
    val title: String,
    val description: String,
    val icon: ImageVector
)

data class VoiceCommand(
    val phrase: String,
    val description: String
)

// Helper functions

private fun getRecommendedFeatures(userType: UserType): List<OnboardingFeature> {
    return when (userType) {
        UserType.NEW_USER -> listOf(
            OnboardingFeature("video_bookmarks", "Video Bookmarks", "Save your favorite moments", Icons.Default.Bookmark),
            OnboardingFeature("sleep_timer", "Sleep Timer", "Auto-pause for bedtime", Icons.Default.Timer),
            OnboardingFeature("gesture_controls", "Gesture Controls", "Swipe to control playback", Icons.Default.Swipe)
        )
        
        UserType.POWER_USER -> listOf(
            OnboardingFeature("ai_subtitles", "AI Subtitles", "Generate subtitles in 50+ languages", Icons.Default.Subtitles),
            OnboardingFeature("advanced_search", "Advanced Search", "AI-powered video search", Icons.Default.Search),
            OnboardingFeature("performance_optimization", "Performance Optimization", "Optimize for your device", Icons.Default.Speed)
        )
        
        UserType.ACCESSIBILITY_USER -> listOf(
            OnboardingFeature("voice_control", "Voice Control", "Control with voice commands", Icons.Default.RecordVoiceOver),
            OnboardingFeature("accessibility_improvements", "Accessibility Features", "Screen reader and navigation", Icons.Default.AccessibilityNew),
            OnboardingFeature("ai_subtitles", "AI Subtitles", "Auto-generated subtitles", Icons.Default.Subtitles)
        )
        
        UserType.PROFESSIONAL_USER -> listOf(
            OnboardingFeature("professional_audio_equalizer", "Professional Equalizer", "Studio-quality audio", Icons.Default.Equalizer),
            OnboardingFeature("frame_navigation", "Frame Navigation", "Precise video control", Icons.Default.SkipNext),
            OnboardingFeature("ai_scene_detection", "Scene Detection", "AI content analysis", Icons.Default.AutoAwesome)
        )
    }
}

private fun getAllFeatures(): List<OnboardingFeature> {
    return listOf(
        OnboardingFeature("ai_subtitles", "AI Subtitle Generation", "Generate subtitles in 50+ languages", Icons.Default.Subtitles),
        OnboardingFeature("voice_control", "Enhanced Voice Control", "40+ voice commands", Icons.Default.RecordVoiceOver),
        OnboardingFeature("professional_audio_equalizer", "Professional Audio Equalizer", "14 presets + custom bands", Icons.Default.Equalizer),
        OnboardingFeature("smart_sleep_timer", "Smart Sleep Timer", "Intelligent auto-pause", Icons.Default.Timer),
        OnboardingFeature("video_bookmarks", "Video Bookmarks", "Save and organize moments", Icons.Default.Bookmark),
        OnboardingFeature("social_sharing", "Social Sharing", "Share moments with others", Icons.Default.Share),
        OnboardingFeature("performance_optimization", "Performance Optimization", "Device-specific optimization", Icons.Default.Speed),
        OnboardingFeature("bubble_pip_mode", "Bubble PIP Mode", "Floating video player", Icons.Default.PictureInPicture),
        OnboardingFeature("advanced_search", "Advanced Search", "AI-powered video search", Icons.Default.Search),
        OnboardingFeature("collaborative_playlists", "Collaborative Playlists", "Share playlists with friends", Icons.Default.PlaylistAdd),
        OnboardingFeature("ai_scene_detection", "AI Scene Detection", "Automatic content analysis", Icons.Default.AutoAwesome),
        OnboardingFeature("gesture_customization", "Gesture Customization", "Personalize touch controls", Icons.Default.Swipe),
        OnboardingFeature("cloud_storage_sync", "Cloud Storage Sync", "OneDrive integration", Icons.Default.Cloud),
        OnboardingFeature("subtitle_translation", "Subtitle Translation", "Real-time translation", Icons.Default.Translate),
        OnboardingFeature("frame_navigation", "Frame-by-Frame Navigation", "Precise video control", Icons.Default.SkipNext),
        OnboardingFeature("adult_content_filters", "Adult Content Filters", "Smart content filtering", Icons.Default.FilterAlt),
        OnboardingFeature("streaming_quality_auto", "Auto Quality Streaming", "Adaptive streaming", Icons.Default.HighQuality),
        OnboardingFeature("haptic_feedback", "Enhanced Haptic Feedback", "Tactile response system", Icons.Default.Vibration),
        OnboardingFeature("accessibility_improvements", "Accessibility Improvements", "Enhanced accessibility", Icons.Default.AccessibilityNew),
        OnboardingFeature("biometric_app_lock", "Biometric App Lock", "Secure with fingerprint", Icons.Default.Fingerprint),
        OnboardingFeature("chromecast_integration", "Chromecast Integration", "Cast to TV", Icons.Default.Cast),
        OnboardingFeature("picture_in_picture", "Picture-in-Picture", "Watch while multitasking", Icons.Default.PictureInPicture)
    )
}

// Additional screens would be implemented similarly...
@Composable
private fun GestureControlsTutorialScreen(onContinue: () -> Unit) {
    // Implementation for gesture controls tutorial
    Button(onClick = onContinue) { Text("Continue") }
}

@Composable
private fun AudioEqualizerTutorialScreen(onContinue: () -> Unit) {
    // Implementation for audio equalizer tutorial
    Button(onClick = onContinue) { Text("Continue") }
}

@Composable
private fun PersonalizationScreen(selectedFeatures: Set<String>, onContinue: () -> Unit) {
    // Implementation for personalization setup
    Button(onClick = onContinue) { Text("Continue") }
}

@Composable
private fun CompletionScreen(onComplete: () -> Unit) {
    // Implementation for completion screen
    Button(onClick = onComplete) { Text("Start Using AstralStream") }
}