package com.astralplayer.nextplayer.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.astralplayer.nextplayer.onboarding.*
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun OnboardingScreen(
    onboardingManager: OnboardingManager,
    onComplete: () -> Unit
) {
    val onboardingState by onboardingManager.onboardingState.collectAsState()
    val currentStep by onboardingManager.currentStep.collectAsState()
    
    val context = LocalContext.current
    
    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            onboardingManager.nextStep()
        }
    }
    
    LaunchedEffect(onboardingState.isCompleted) {
        if (onboardingState.isCompleted) {
            onComplete()
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        MaterialTheme.colorScheme.surface
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Progress indicator
            if (onboardingState.showProgress) {
                OnboardingProgressIndicator(
                    currentStep = onboardingState.currentStepIndex,
                    totalSteps = onboardingState.totalSteps,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Main content with animation
            AnimatedContent(
                targetState = currentStep,
                transitionSpec = {
                    slideInHorizontally { it } + fadeIn() with
                    slideOutHorizontally { -it } + fadeOut()
                }
            ) { step ->
                OnboardingStepContent(
                    step = step,
                    stepConfig = onboardingManager.getStepConfig(step),
                    onPrimaryAction = {
                        when (step) {
                            OnboardingStep.PERMISSIONS -> {
                                val config = onboardingManager.getStepConfig(step)
                                if (config.permissionsRequired.isNotEmpty()) {
                                    permissionLauncher.launch(config.permissionsRequired.toTypedArray())
                                } else {
                                    onboardingManager.nextStep()
                                }
                            }
                            OnboardingStep.COMPLETE -> onboardingManager.completeOnboarding()
                            else -> onboardingManager.nextStep()
                        }
                    },
                    onSecondaryAction = {
                        when (step) {
                            OnboardingStep.WELCOME -> onboardingManager.skipOnboarding()
                            else -> onboardingManager.nextStep()
                        }
                    },
                    modifier = Modifier.weight(1f)
                )
            }
            
            // Navigation buttons
            OnboardingNavigationButtons(
                currentStep = currentStep,
                canGoBack = onboardingState.currentStepIndex > 0,
                onBack = { onboardingManager.previousStep() },
                onSkip = { onboardingManager.skipOnboarding() }
            )
        }
    }
}

@Composable
private fun OnboardingProgressIndicator(
    currentStep: Int,
    totalSteps: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(totalSteps) { index ->
            val isActive = index <= currentStep
            val isCurrentStep = index == currentStep
            
            Box(
                modifier = Modifier
                    .size(if (isCurrentStep) 12.dp else 8.dp)
                    .clip(CircleShape)
                    .background(
                        if (isActive) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                        }
                    )
            )
            
            if (index < totalSteps - 1) {
                Box(
                    modifier = Modifier
                        .width(24.dp)
                        .height(2.dp)
                        .background(
                            if (index < currentStep) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                            }
                        )
                )
            }
        }
    }
}

@Composable
private fun OnboardingStepContent(
    step: OnboardingStep,
    stepConfig: OnboardingStepConfig,
    onPrimaryAction: () -> Unit,
    onSecondaryAction: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Icon
        OnboardingStepIcon(
            step = step,
            modifier = Modifier.size(120.dp)
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Title
        Text(
            text = stepConfig.title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Description
        Text(
            text = stepConfig.description,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 24.sp
        )
        
        Spacer(modifier = Modifier.height(48.dp))
        
        // Interactive demo for gestures
        if (stepConfig.hasInteractiveDemo && step == OnboardingStep.GESTURES) {
            GestureDemo(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .padding(16.dp)
            )
            
            Spacer(modifier = Modifier.height(32.dp))
        }
        
        // Primary action button
        Button(
            onClick = onPrimaryAction,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(28.dp)
        ) {
            Text(
                text = stepConfig.primaryAction,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
        }
        
        // Secondary action button
        stepConfig.secondaryAction?.let { secondaryText ->
            Spacer(modifier = Modifier.height(16.dp))
            
            TextButton(
                onClick = onSecondaryAction,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = secondaryText,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun OnboardingStepIcon(
    step: OnboardingStep,
    modifier: Modifier = Modifier
) {
    val icon: ImageVector = when (step) {
        OnboardingStep.WELCOME -> Icons.Filled.PlayCircle
        OnboardingStep.PERMISSIONS -> Icons.Filled.Security
        OnboardingStep.GESTURES -> Icons.Filled.TouchApp
        OnboardingStep.AI_FEATURES -> Icons.Filled.AutoAwesome
        OnboardingStep.CUSTOMIZATION -> Icons.Filled.Tune
        OnboardingStep.COMPLETE -> Icons.Filled.CheckCircle
    }
    
    val backgroundColor = when (step) {
        OnboardingStep.WELCOME -> MaterialTheme.colorScheme.primary
        OnboardingStep.PERMISSIONS -> MaterialTheme.colorScheme.secondary
        OnboardingStep.GESTURES -> MaterialTheme.colorScheme.tertiary
        OnboardingStep.AI_FEATURES -> MaterialTheme.colorScheme.primary
        OnboardingStep.CUSTOMIZATION -> MaterialTheme.colorScheme.secondary
        OnboardingStep.COMPLETE -> MaterialTheme.colorScheme.tertiary
    }
    
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(backgroundColor.copy(alpha = 0.1f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = backgroundColor,
            modifier = Modifier.size(60.dp)
        )
    }
}

@Composable
private fun GestureDemo(
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Animated gesture indicators
                Row(
                    horizontalArrangement = Arrangement.spacedBy(32.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    GestureIndicator(
                        icon = Icons.Filled.SwipeLeft,
                        label = "Seek Back"
                    )
                    
                    GestureIndicator(
                        icon = Icons.Filled.SwipeRight,
                        label = "Seek Forward"
                    )
                }
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(32.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    GestureIndicator(
                        icon = Icons.Filled.VolumeUp,
                        label = "Volume"
                    )
                    
                    GestureIndicator(
                        icon = Icons.Filled.Brightness6,
                        label = "Brightness"
                    )
                }
            }
        }
    }
}

@Composable
private fun GestureIndicator(
    icon: ImageVector,
    label: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(32.dp)
        )
        
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun OnboardingNavigationButtons(
    currentStep: OnboardingStep,
    canGoBack: Boolean,
    onBack: () -> Unit,
    onSkip: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Back button
        if (canGoBack) {
            TextButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Filled.ArrowBack,
                    contentDescription = "Back",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Back")
            }
        } else {
            Spacer(modifier = Modifier.width(1.dp)) // Placeholder
        }
        
        // Skip button (only show for non-final steps)
        if (currentStep != OnboardingStep.COMPLETE) {
            TextButton(onClick = onSkip) {
                Text("Skip")
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Filled.ArrowForward,
                    contentDescription = "Skip",
                    modifier = Modifier.size(18.dp)
                )
            }
        } else {
            Spacer(modifier = Modifier.width(1.dp)) // Placeholder
        }
    }
}