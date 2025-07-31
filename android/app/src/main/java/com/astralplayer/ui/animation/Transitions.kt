package com.astralplayer.ui.animation

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment

/**
 * Elite animation transitions for AstralStream
 */
object Transitions {
    
    /**
     * Smooth fade and slide transition for screens
     */
    val screenEnterTransition = fadeIn(
        animationSpec = tween(
            durationMillis = 300,
            easing = FastOutSlowInEasing
        )
    ) + slideInHorizontally(
        initialOffsetX = { it / 3 },
        animationSpec = tween(
            durationMillis = 300,
            easing = FastOutSlowInEasing
        )
    )
    
    val screenExitTransition = fadeOut(
        animationSpec = tween(
            durationMillis = 250,
            easing = FastOutSlowInEasing
        )
    ) + slideOutHorizontally(
        targetOffsetX = { -it / 3 },
        animationSpec = tween(
            durationMillis = 250,
            easing = FastOutSlowInEasing
        )
    )
    
    /**
     * Elegant scale and fade for dialogs
     */
    val dialogEnterTransition = scaleIn(
        initialScale = 0.9f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        )
    ) + fadeIn(
        animationSpec = tween(200)
    )
    
    val dialogExitTransition = scaleOut(
        targetScale = 0.95f,
        animationSpec = tween(150)
    ) + fadeOut(
        animationSpec = tween(150)
    )
    
    /**
     * Slide up transition for bottom sheets
     */
    val bottomSheetEnterTransition = slideInVertically(
        initialOffsetY = { it },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        )
    ) + fadeIn(
        animationSpec = tween(150)
    )
    
    val bottomSheetExitTransition = slideOutVertically(
        targetOffsetY = { it },
        animationSpec = tween(200)
    ) + fadeOut(
        animationSpec = tween(200)
    )
    
    /**
     * Expand transition for cards
     */
    val cardExpandTransition = expandIn(
        expandFrom = Alignment.Center,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessLow
        )
    ) + fadeIn(
        animationSpec = tween(150)
    )
    
    /**
     * Shared element transition spec
     */
    val sharedElementTransitionSpec = tween<Float>(
        durationMillis = 400,
        easing = FastOutSlowInEasing
    )
    
    /**
     * Video player control fade
     */
    val controlsFadeSpec = tween<Float>(
        durationMillis = 300,
        easing = LinearEasing
    )
    
    /**
     * List item animation
     */
    @Composable
    fun listItemTransition(index: Int) = fadeIn(
        animationSpec = tween(
            durationMillis = 200,
            delayMillis = index * 50,
            easing = FastOutSlowInEasing
        )
    ) + slideInVertically(
        initialOffsetY = { it / 10 },
        animationSpec = tween(
            durationMillis = 300,
            delayMillis = index * 50,
            easing = FastOutSlowInEasing
        )
    )
}

/**
 * Animation specs for various UI elements
 */
object AnimationSpecs {
    
    /**
     * Quick animation for immediate feedback
     */
    val quick = tween<Float>(
        durationMillis = 150,
        easing = FastOutSlowInEasing
    )
    
    /**
     * Standard animation for most transitions
     */
    val standard = tween<Float>(
        durationMillis = 300,
        easing = FastOutSlowInEasing
    )
    
    /**
     * Slow animation for emphasis
     */
    val slow = tween<Float>(
        durationMillis = 500,
        easing = FastOutSlowInEasing
    )
    
    /**
     * Spring animation for playful elements
     */
    val bouncy = spring<Float>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMedium
    )
    
    /**
     * Smooth spring for natural movement
     */
    val smooth = spring<Float>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessLow
    )
}

/**
 * Page transition specs for navigation
 */
object PageTransitions {
    
    @OptIn(ExperimentalAnimationApi::class)
    val slideLeftTransition = AnimatedContentTransitionScope<Any>.() -> ContentTransform {
        slideIntoContainer(
            towards = AnimatedContentTransitionScope.SlideDirection.Left,
            animationSpec = tween(300)
        ) + fadeIn(
            animationSpec = tween(300)
        ) with slideOutOfContainer(
            towards = AnimatedContentTransitionScope.SlideDirection.Left,
            animationSpec = tween(300)
        ) + fadeOut(
            animationSpec = tween(300)
        )
    }
    
    @OptIn(ExperimentalAnimationApi::class)
    val fadeTransition = AnimatedContentTransitionScope<Any>.() -> ContentTransform {
        fadeIn(
            animationSpec = tween(300)
        ) with fadeOut(
            animationSpec = tween(300)
        )
    }
    
    @OptIn(ExperimentalAnimationApi::class)
    val scaleTransition = AnimatedContentTransitionScope<Any>.() -> ContentTransform {
        scaleIn(
            initialScale = 0.92f,
            animationSpec = tween(300)
        ) + fadeIn(
            animationSpec = tween(300)
        ) with scaleOut(
            targetScale = 1.08f,
            animationSpec = tween(300)
        ) + fadeOut(
            animationSpec = tween(300)
        )
    }
}