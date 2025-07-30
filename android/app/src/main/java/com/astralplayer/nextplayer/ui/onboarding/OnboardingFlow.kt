package com.astralplayer.nextplayer.ui.onboarding

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.astralplayer.nextplayer.analytics.FeatureAnalytics
import com.astralplayer.nextplayer.feature.flags.Feature
import com.astralplayer.nextplayer.feature.flags.FeatureFlags
import com.astralplayer.nextplayer.ui.components.BubbleCard
import com.astralplayer.nextplayer.ui.components.BubbleButton
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Onboarding Flow for New Features
 * Interactive introduction to AstralStream's advanced capabilities
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingFlow(
    onComplete: () -> Unit,
    onSkip: () -> Unit
) {
    val context = LocalContext.current
    val analytics = remember { FeatureAnalytics(context) }
    val scope = rememberCoroutineScope()
    
    val onboardingSteps = remember { getOnboardingSteps() }
    val pagerState = rememberPagerState(pageCount = { onboardingSteps.size })
    
    var currentStep by remember { mutableIntStateOf(0) }
    
    LaunchedEffect(pagerState.currentPage) {
        currentStep = pagerState.currentPage
        
        // Track onboarding progress
        analytics.trackFeatureDiscovery(
            featureName = onboardingSteps[currentStep].feature.name,
            discoveryMethod = "onboarding"
        )
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        MaterialTheme.colorScheme.secondary.copy(alpha = 0.05f)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Header
            OnboardingHeader(
                currentStep = currentStep,
                totalSteps = onboardingSteps.size,
                onSkip = {
                    analytics.trackFeatureAbandon(
                        featureName = "onboarding",
                        timeSpent = 0L,
                        reason = "skipped",
                        completedAction = false
                    )
                    onSkip()
                }
            )
            
            // Content Pager
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f)
            ) { page ->
                OnboardingStepContent(
                    step = onboardingSteps[page],
                    analytics = analytics
                )
            }
            
            // Navigation Controls
            OnboardingNavigation(
                currentStep = currentStep,
                totalSteps = onboardingSteps.size,
                onPrevious = {
                    scope.launch {
                        pagerState.animateScrollToPage(currentStep - 1)
                    }
                },
                onNext = {
                    if (currentStep < onboardingSteps.size - 1) {
                        scope.launch {
                            pagerState.animateScrollToPage(currentStep + 1)
                        }
                    } else {
                        // Complete onboarding
                        analytics.trackEngagementMetrics(
                            sessionDuration = 0L,
                            featuresUsed = listOf("onboarding_completed"),
                            videosWatched = 0,
                            interactionCount = currentStep + 1
                        )
                        onComplete()
                    }
                },
                onSkip = {
                    analytics.trackFeatureAbandon(
                        featureName = "onboarding",
                        timeSpent = 0L,
                        reason = "skipped",
                        completedAction = false
                    )
                    onSkip()
                }
            )
        }
    }
}

@Composable
private fun OnboardingHeader(
    currentStep: Int,
    totalSteps: Int,
    onSkip: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Progress indicator
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(totalSteps) { index ->
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(
                            if (index <= currentStep) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                            }
                        )
                )
            }
        }
        
        // Skip button
        TextButton(onClick = onSkip) {
            Text(
                "Skip",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun OnboardingStepContent(
    step: OnboardingStep,
    analytics: FeatureAnalytics
) {
    var hasInteracted by remember { mutableStateOf(false) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Feature Icon with Animation
        AnimatedFeatureIcon(
            icon = step.icon,
            color = step.accentColor
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Title
        Text(
            text = step.title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Description
        Text(
            text = step.description,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 24.sp
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Interactive Demo
        BubbleCard(
            elevation = 8,
            cornerRadius = 24,
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                step.demoContent()
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Try It Button
                if (FeatureFlags.isEnabled(step.feature)) {
                    BubbleButton(
                        onClick = {
                            hasInteracted = true
                            analytics.trackFeatureDiscovery(
                                featureName = step.feature.name,
                                discoveryMethod = "onboarding_demo"
                            )
                            step.onTryFeature()
                        },
                        text = "Try ${step.title}",
                        icon = Icons.Default.PlayArrow,
                        containerColor = step.accentColor,
                        contentColor = Color.White
                    )
                } else {
                    Text(
                        text = "This feature will be available soon!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Benefits List
        step.benefits.forEach { benefit ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = step.accentColor,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = benefit,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun AnimatedFeatureIcon(
    icon: ImageVector,
    color: Color
) {
    val infiniteTransition = rememberInfiniteTransition(label = "icon_animation")
    
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale_animation"
    )
    
    val rotation by infiniteTransition.animateFloat(
        initialValue = -5f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "rotation_animation"
    )
    
    Box(
        modifier = Modifier
            .size(120.dp)
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(
                        color.copy(alpha = 0.2f),
                        Color.Transparent
                    )
                ),
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier
                .size(64.dp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    rotationZ = rotation
                }
        )
    }
}

@Composable
private fun OnboardingNavigation(
    currentStep: Int,
    totalSteps: Int,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSkip: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Previous Button
        if (currentStep > 0) {
            BubbleButton(
                onClick = onPrevious,
                text = "Previous",
                icon = Icons.Default.ArrowBack,
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Spacer(modifier = Modifier.width(100.dp))
        }
        
        // Step Indicator
        Text(
            text = "${currentStep + 1} of $totalSteps",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        // Next/Complete Button
        BubbleButton(
            onClick = onNext,
            text = if (currentStep == totalSteps - 1) "Get Started" else "Next",
            icon = if (currentStep == totalSteps - 1) Icons.Default.Check else Icons.Default.ArrowForward,
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        )
    }
}

// Data class for onboarding steps
data class OnboardingStep(
    val feature: Feature,
    val title: String,
    val description: String,
    val icon: ImageVector,
    val accentColor: Color,
    val benefits: List<String>,
    val demoContent: @Composable () -> Unit,
    val onTryFeature: () -> Unit
)

// Onboarding steps configuration
@Composable
private fun getOnboardingSteps(): List<OnboardingStep> {
    return listOf(
        OnboardingStep(
            feature = Feature.ADVANCED_SEARCH,
            title = "Smart Search",
            description = "Find any video instantly with AI-powered search and intelligent filters.",
            icon = Icons.Default.Search,
            accentColor = Color(0xFF2196F3),
            benefits = listOf(
                "Search by duration, quality, or content type",
                "Smart suggestions based on your history",
                "Lightning-fast results with relevance scoring"
            ),
            demoContent = {
                SearchDemoContent()
            },
            onTryFeature = {
                // Navigate to search screen
            }
        ),
        
        OnboardingStep(
            feature = Feature.AI_SUBTITLES,
            title = "AI Subtitles",
            description = "Automatically generate and translate subtitles for any video using advanced AI.",
            icon = Icons.Default.Subtitles,
            accentColor = Color(0xFF9C27B0),
            benefits = listOf(
                "Generate subtitles in 50+ languages",
                "Real-time translation and customization",
                "Works offline with high accuracy"
            ),
            demoContent = {
                SubtitleDemoContent()
            },
            onTryFeature = {
                // Show subtitle generation dialog
            }
        ),
        
        OnboardingStep(
            feature = Feature.VOICE_CONTROL,
            title = "Voice Control",
            description = "Control your videos entirely hands-free with 40+ voice commands.",
            icon = Icons.Default.Mic,
            accentColor = Color(0xFF4CAF50),
            benefits = listOf(
                "40+ natural voice commands",
                "Works in multiple languages",
                "Perfect for accessibility and convenience"
            ),
            demoContent = {
                VoiceControlDemoContent()
            },
            onTryFeature = {
                // Enable voice control
            }
        ),
        
        OnboardingStep(
            feature = Feature.AUDIO_EQUALIZER,
            title = "Audio Equalizer",
            description = "Professional-grade audio enhancement with presets and custom effects.",
            icon = Icons.Default.Equalizer,
            accentColor = Color(0xFFFF9800),
            benefits = listOf(
                "14 professional presets",
                "5-band custom equalizer",
                "Bass boost and audio effects"
            ),
            demoContent = {
                EqualizerDemoContent()
            },
            onTryFeature = {
                // Open equalizer settings
            }
        ),
        
        OnboardingStep(
            feature = Feature.SOCIAL_SHARING,
            title = "Social Features",
            description = "Share videos, create collaborative playlists, and host watch parties.",
            icon = Icons.Default.Share,
            accentColor = Color(0xFFE91E63),
            benefits = listOf(
                "Smart sharing with previews",
                "Collaborative playlist editing",
                "Synchronized watch parties"
            ),
            demoContent = {
                SocialDemoContent()
            },
            onTryFeature = {
                // Show sharing options
            }
        )
    )
}

// Demo content composables
@Composable
private fun SearchDemoContent() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            "Try searching: \"action movies longer than 1 hour\"",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        // Mock search bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .background(
                    MaterialTheme.colorScheme.surface,
                    RoundedCornerShape(24.dp)
                )
                .border(
                    1.dp,
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                    RoundedCornerShape(24.dp)
                ),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    "Search videos...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SubtitleDemoContent() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            "Generate subtitles in any language",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )
        
        // Mock subtitle preview
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Color.Black.copy(alpha = 0.8f),
                    RoundedCornerShape(8.dp)
                )
        ) {
            Text(
                "Hello, welcome to AstralStream!",
                modifier = Modifier.padding(12.dp),
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun VoiceControlDemoContent() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            "Say: \"Play the video\" or \"Search for comedy\"",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )
        
        // Voice wave animation
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(5) { index ->
                VoiceWaveBar(delay = index * 100)
            }
        }
    }
}

@Composable
private fun VoiceWaveBar(delay: Int) {
    val infiniteTransition = rememberInfiniteTransition(label = "voice_wave")
    
    val height by infiniteTransition.animateFloat(
        initialValue = 4f,
        targetValue = 20f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, delayMillis = delay, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "wave_height"
    )
    
    Box(
        modifier = Modifier
            .width(4.dp)
            .height(height.dp)
            .background(
                MaterialTheme.colorScheme.primary,
                RoundedCornerShape(2.dp)
            )
    )
}

@Composable
private fun EqualizerDemoContent() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            "Choose from 14 professional presets",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )
        
        // Mock equalizer bars
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            val heights = listOf(12, 20, 16, 24, 18)
            heights.forEach { height ->
                Box(
                    modifier = Modifier
                        .width(8.dp)
                        .height(height.dp)
                        .background(
                            MaterialTheme.colorScheme.primary,
                            RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)
                        )
                )
            }
        }
    }
}

@Composable
private fun SocialDemoContent() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            "Share with friends and collaborate",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )
        
        // Mock sharing icons
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            listOf(
                Icons.Default.Share,
                Icons.Default.Group,
                Icons.Default.PlayArrow
            ).forEach { icon ->
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            MaterialTheme.colorScheme.primaryContainer,
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}