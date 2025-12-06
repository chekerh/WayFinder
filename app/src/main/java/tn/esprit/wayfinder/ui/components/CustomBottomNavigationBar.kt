package tn.esprit.wayfinder.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.border
import tn.esprit.wayfinder.utils.HapticFeedbackHelper
import androidx.navigation.NavController
import kotlin.math.sin
import kotlin.math.PI
import kotlinx.coroutines.delay
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.zIndex

@Composable
fun CustomBottomNavigationBar(navController: NavController? = null) {
    val currentRoute = navController?.currentBackStackEntry?.destination?.route
    val colorScheme = MaterialTheme.colorScheme
    // Detect dark mode by calculating luminance from RGB values
    // Luminance formula: 0.2126 * R + 0.7152 * G + 0.0722 * B
    val bgColor = colorScheme.background
    val luminance = 0.2126f * bgColor.red + 0.7152f * bgColor.green + 0.0722f * bgColor.blue
    val isDark = luminance < 0.5f
    var selectedIndex by remember { 
        mutableStateOf(
            when (currentRoute) {
                "home" -> 0
                "chat" -> 1
                "outfit_selection" -> 2
                "map_memories" -> 3
                else -> 0
            }
        )
    }
    
    // Update selected index when route changes
    LaunchedEffect(currentRoute) {
        selectedIndex = when (currentRoute) {
            "home" -> 0
            "chat" -> 1
            "outfit_selection" -> 2
            "map_memories" -> 3
            else -> 0
        }
    }
    
    // iOS-style background color (adaptive to theme)
    val backgroundColor = if (isDark) {
        Color(0xFF000000) // Black background in dark mode
    } else {
        Color(0xFFFFFFFF)
    }
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .zIndex(1f) // Tab Bar background layer
            .shadow(
                elevation = if (isDark) 8.dp else 4.dp,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            )
            .background(
                color = backgroundColor,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            )
            // Remove vertical padding to prevent white band - bubble needs full height visibility
            .padding(horizontal = 0.dp, vertical = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .align(Alignment.Center)
                .zIndex(2f) // Icons layer - above background
                // Ensure no clipping that would hide the bubble animation
                .padding(vertical = 4.dp), // Add padding here instead to center icons
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Home
            NavBarIcon(
                icon = Icons.Filled.Home,
                isSelected = selectedIndex == 0,
                selectedIndex = selectedIndex,
                iconIndex = 0,
                onClick = {
                    selectedIndex = 0
                    navController?.navigate("home") {
                        popUpTo("home") { inclusive = true }
                    }
                }
            )
            
            // Chat
            NavBarIcon(
                icon = Icons.Outlined.ChatBubbleOutline,
                isSelected = selectedIndex == 1,
                selectedIndex = selectedIndex,
                iconIndex = 1,
                onClick = {
                    selectedIndex = 1
                    navController?.navigate("chat") {
                        popUpTo("home") { inclusive = false }
                    }
                }
            )
            
            // Vérifier ma tenue (Outfit Weather)
            NavBarIcon(
                icon = Icons.Filled.CameraAlt,
                isSelected = selectedIndex == 2,
                selectedIndex = selectedIndex,
                iconIndex = 2,
                onClick = {
                    selectedIndex = 2
                    navController?.navigate("outfit_selection") {
                        popUpTo("home") { inclusive = false }
                    }
                }
            )
            
            // Carte (Map)
            NavBarIcon(
                icon = Icons.Filled.Map,
                isSelected = selectedIndex == 3,
                selectedIndex = selectedIndex,
                iconIndex = 3,
                onClick = {
                    selectedIndex = 3
                    navController?.navigate("map_memories") {
                        popUpTo("home") { inclusive = false }
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun NavBarIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    selectedIndex: Int,
    iconIndex: Int,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme
    
    // Optimize: Memoize dark mode calculation to avoid recalculating on every recomposition
    val isDark = remember(colorScheme.background) {
        val bgColor = colorScheme.background
        val luminance = 0.2126f * bgColor.red + 0.7152f * bgColor.green + 0.0722f * bgColor.blue
        luminance < 0.5f
    }
    
    // Optimize: Memoize bubble gradient to avoid recreating on every recomposition
    val bubbleGradient = remember {
        Brush.linearGradient(
            colors = listOf(
                Color(0xFF4096FA), // iOS blue top: rgb(0.25, 0.65, 0.98) = #4096FA
                Color(0xFF1259D1)  // iOS blue bottom: rgb(0.07, 0.35, 0.82) = #1259D1
            ),
            start = androidx.compose.ui.geometry.Offset(0f, 0f),
            end = androidx.compose.ui.geometry.Offset(40f, 40f)
        )
    }
    
    // Continuous bouncing animation - matches iOS bouncy spring behavior
    // iOS uses spring animations with dampingFraction: 0.5-0.7 for bouncy effect
    val infiniteTransition = rememberInfiniteTransition(label = "bouncing")
    
    // Vertical bouncing movement (bouncy up/down oscillation)
    // iOS spring: response: 0.25-0.3, dampingFraction: 0.5-0.65 (very bouncy)
    // Using keyframes to simulate spring bounce effect with pronounced oscillations
    val bouncingOffsetY by infiniteTransition.animateFloat(
        initialValue = -8.dp.value,
        targetValue = -16.dp.value, // Bounce up 8dp from base position (more pronounced like iOS)
        animationSpec = infiniteRepeatable(
            animation = keyframes<Float> {
                durationMillis = 1200 // Cycle duration matching iOS spring timing
                -8.dp.value at 0 with FastOutSlowInEasing // Start position
                -16.dp.value at 300 with FastOutSlowInEasing // Bounce up (fast)
                -8.dp.value at 600 with FastOutSlowInEasing // Bounce down
                -12.dp.value at 800 with FastOutSlowInEasing // Small rebound up
                -8.dp.value at 1000 with FastOutSlowInEasing // Settle
                -9.dp.value at 1100 with FastOutSlowInEasing // Tiny bounce
                -8.dp.value at 1200 with FastOutSlowInEasing // Final settle
            },
            repeatMode = RepeatMode.Restart // Restart for continuous bounce
        ),
        label = "bouncingY"
    )
    
    // Bouncy scale effect during bouncing (1.0 -> 1.15 -> 0.95 -> 1.0)
    // More pronounced scale variations to match iOS bounce effect
    val bouncingScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.15f, // More pronounced scale increase (matches iOS 1.1-1.2 range)
        animationSpec = infiniteRepeatable(
            animation = keyframes<Float> {
                durationMillis = 1200 // Same cycle as offset
                1.0f at 0 with FastOutSlowInEasing // Start scale
                1.15f at 300 with FastOutSlowInEasing // Scale up on bounce up
                0.95f at 500 with FastOutSlowInEasing // Compress on way down
                1.05f at 700 with FastOutSlowInEasing // Small rebound
                0.98f at 900 with FastOutSlowInEasing // Settle down
                1.02f at 1050 with FastOutSlowInEasing // Tiny bounce
                1.0f at 1200 with FastOutSlowInEasing // Final settle
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "bouncingScale"
    )
    
    // Track previous selected index to detect tab changes and trigger bounce
    var previousSelectedIndex by remember { mutableStateOf(selectedIndex) }
    var isBouncing by remember { mutableStateOf(false) }
    
    // Multi-phase bounce animation matching iOS (5 phases)
    // Must be defined before use in transitionProgress and arcOffsetY
    var bouncePhase by remember { mutableStateOf(0) }
    
    // Arc movement is now integrated directly into AnimatedContent transitionSpec
    // No separate calculation needed - the transition handles the arc automatically
    
    // Detect when tab changes to trigger bounce animation and arc movement
    LaunchedEffect(selectedIndex) {
        if (selectedIndex != previousSelectedIndex) {
            if (isSelected && selectedIndex == iconIndex) {
                // This icon is becoming selected - trigger bounce and arc
                isBouncing = true
                previousSelectedIndex = selectedIndex
                // Reset bouncing after animation completes (matches iOS 0.65s timing)
                delay(650)
                isBouncing = false
            } else {
                // Tab changed but this icon is not selected
                previousSelectedIndex = selectedIndex
            }
        }
    }
    
    // Track transition progress for arc animation (0.0 to 1.0)
    // This creates the parabolic arc during horizontal movement
    // The arc must be synchronized with the horizontal transition (400ms)
    // On iOS, matchedGeometryEffect creates the arc automatically during horizontal movement
    // We animate the progress from 0 to 1 during the jump to create the arc
    val transitionProgress by animateFloatAsState(
        targetValue = if (isSelected && isBouncing && bouncePhase == 1) {
            1f // Full arc during jump phase (Phase 1) - synchronized with horizontal transition
        } else {
            0f
        },
        animationSpec = spring(
            dampingRatio = 0.7f, // Matches iOS bounceTransitionAnimation dampingFraction: 0.7
            stiffness = 400f // Matches iOS response: 0.4 (400ms) - same as horizontal transition
        ),
        label = "transitionProgress"
    )
    
    // Arc offset Y: creates parabolic curve (half-circle) during horizontal transition
    // Formula: -amplitude * sin(π * progress) creates smooth arc
    // The arc occurs during Phase 1 (jump up) which is synchronized with horizontal movement
    // This creates a half-circle: starts at 0, peaks at -amplitude in middle, ends at 0
    // The arc creates the VISIBLE "jump" effect as the bubble moves horizontally between icons
    // IMPORTANT: This arc is what makes the bubble "jump" in a curved path (like iOS)
    // Optimize: Memoize arc amplitude calculation
    val arcAmplitude = remember { 60.dp.value } // Height of arc (significantly increased for maximum visibility)
    val arcOffsetY = remember(transitionProgress, isBouncing, bouncePhase) {
        if (isBouncing && bouncePhase == 1 && transitionProgress > 0f) {
            // Parabolic arc: goes up in middle, creating half-circle movement
            // sin(π * progress) gives: 0 at start, 1 at middle, 0 at end
            // Negative gives upward arc: -amplitude at peak
            val progress = transitionProgress.coerceIn(0f, 1f)
            -arcAmplitude * sin(PI * progress).toFloat()
        } else {
            0f
        }
    }
    
    // Phase-based bounce animation matching iOS exactly
    val jumpOffsetY by animateFloatAsState(
        targetValue = when {
            !isSelected -> 0f
            isBouncing && bouncePhase == 1 -> -16.dp.value // Phase 1: Jump up (matches iOS)
            isBouncing && bouncePhase == 2 -> -2.dp.value // Phase 2: Impact position
            isBouncing && bouncePhase == 3 -> -14.dp.value // Phase 3: Rebound upward
            isBouncing && bouncePhase == 4 -> -6.dp.value // Phase 4: Settle bounce
            else -> -8.dp.value // Phase 5: Final resting position
        },
        animationSpec = spring(
            dampingRatio = when {
                bouncePhase == 2 -> 0.5f // Landing bounce (matches iOS dampingFraction: 0.5)
                bouncePhase == 3 || bouncePhase == 4 -> 0.65f // Settle bounce (matches iOS dampingFraction: 0.65)
                else -> 0.7f // Transition bounce (matches iOS dampingFraction: 0.7)
            },
            stiffness = when {
                bouncePhase == 2 -> 300f // Landing (matches iOS response: 0.3)
                bouncePhase == 3 || bouncePhase == 4 -> 250f // Settle (matches iOS response: 0.25)
                else -> 400f // Transition (matches iOS response: 0.4)
            }
        ),
        label = "jumpOffsetY"
    )
    
    // Scale animation for tab change bounce - matches iOS phases exactly
    val bounceScale by animateFloatAsState(
        targetValue = when {
            !isSelected -> 1f
            isBouncing && bouncePhase == 1 -> 1.1f // Phase 1: Slightly grow during jump
            isBouncing && bouncePhase == 2 -> 1.2f // Phase 2: Scale up on impact (15% larger)
            isBouncing && bouncePhase == 3 -> 0.95f // Phase 3: Slightly compress
            isBouncing && bouncePhase == 4 -> 1.05f // Phase 4: Slight overshoot
            else -> 1.0f // Phase 5: Normal size
        },
        animationSpec = spring(
            dampingRatio = when {
                bouncePhase == 2 -> 0.5f // Landing bounce
                bouncePhase == 3 || bouncePhase == 4 -> 0.65f // Settle bounce
                else -> 0.7f // Transition bounce
            },
            stiffness = when {
                bouncePhase == 2 -> 300f // Landing
                bouncePhase == 3 || bouncePhase == 4 -> 250f // Settle
                else -> 400f // Transition
            }
        ),
        label = "bounceScale"
    )
    
    // Trigger bounce phases with delays matching iOS exactly
    LaunchedEffect(isBouncing) {
        if (isBouncing) {
            bouncePhase = 1 // Phase 1: Jump up
            delay(200) // 0.2s (matches iOS)
            bouncePhase = 2 // Phase 2: Land with impact
            delay(150) // 0.35s total
            bouncePhase = 3 // Phase 3: Rebound upward
            delay(150) // 0.5s total
            bouncePhase = 4 // Phase 4: Settle bounce
            delay(150) // 0.65s total
            bouncePhase = 0 // Phase 5: Final position
        }
    }
    
    // Optimize: Memoize combined offsets and scales to avoid recalculating on every recomposition
    // Combine continuous bouncing animation with jump animation when selected
    // Arc movement is applied separately via .offset() modifier for better visibility
    // Only the bubble floats, not the icon
    val bubbleOffsetY = remember(bouncingOffsetY, jumpOffsetY, isSelected) {
        if (isSelected) {
            // Combine: continuous bounce + jump (arc is applied via .offset() modifier)
            bouncingOffsetY + jumpOffsetY
        } else {
            0f
        }
    }
    
    // Combine bouncing scale with bounce scale - only for bubble
    val bubbleScale = remember(bouncingScale, bounceScale, isSelected) {
        if (isSelected) {
            bouncingScale * bounceScale // Combine continuous bounce scale with jump bounce
        } else {
            1f
        }
    }
    
    // Icon stays static - doesn't float, only follows base position
    // Icon doesn't get the continuous bouncing animation, only the jump animation on tab change
    val iconOffsetY = remember(jumpOffsetY, isSelected) {
        if (isSelected) {
            jumpOffsetY // Icon only follows jump animation when tab changes, NOT continuous float
        } else {
            0f
        }
    }
    
    val iconScale = remember(bounceScale, isSelected) {
        if (isSelected) {
            bounceScale * 0.88f // Icon scales with bounce but less than bubble, NO continuous float scale
        } else {
            1f
        }
    }
    
    // Optimize: Memoize icon color calculation
    // Icon color (iOS style: white when selected, accent color when not)
    val iconColor = remember(isSelected, isDark) {
        if (isSelected) {
            Color.White
        } else {
            if (isDark) {
                Color(0xFFA0A0A0) // Light gray in dark mode
            } else {
                Color(0xFF4096FF) // iOS blue in light mode
            }
        }
    }
    
    // Disable ripple effect and any default selection border/outline (no gray square on click)
    val interactionSource = remember { MutableInteractionSource() }
    
    Box(
        modifier = Modifier
            .size(40.dp)
            // Remove any default selection border/outline - completely disable visual feedback
            // No ripple, no focus indicator, no selection border
            .clickable(
                interactionSource = interactionSource,
                indication = null, // Disable ripple effect completely - no gray square
                enabled = true
            ) {
                HapticFeedbackHelper.triggerButtonPress(context)
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        // Background bubble (iOS style) - only when selected
        // Matches iOS: water drop effect with inner glow, outer glow, and dynamic shadow
        // Use AnimatedContent for arc movement (half-circle) when transitioning between icons
        // iOS matchedGeometryEffect creates a smooth arc automatically
        // On Android, we create a custom transition that combines horizontal slide with parabolic arc
        AnimatedContent(
            targetState = isSelected,
            transitionSpec = {
                // Create a spring animation that matches iOS bounceTransitionAnimation
                // Use spring<IntOffset> for slide transitions (they work with IntOffset, not Float)
                val springSpec = spring<IntOffset>(
                    dampingRatio = 0.7f, // Matches iOS bounceTransitionAnimation dampingFraction: 0.7
                    stiffness = 400f // Matches iOS response: 0.4 (400ms)
                )
                
                if (targetState) {
                    // Appearing: slide in horizontally with parabolic arc up (half-circle movement)
                    // The arc is created by arcOffsetY in graphicsLayer, not by slideInVertically
                    // This allows the arc to be a smooth parabolic curve during horizontal movement
                    val enterTransition = slideInHorizontally(
                        initialOffsetX = { fullWidth -> 
                            // Calculate offset based on direction (left or right)
                            val direction = if (iconIndex < selectedIndex) -fullWidth else fullWidth
                            direction
                        },
                        animationSpec = springSpec
                    ) + fadeIn()
                    
                    val exitTransition = slideOutHorizontally(
                        targetOffsetX = { fullWidth -> 
                            val direction = if (iconIndex < selectedIndex) fullWidth else -fullWidth
                            direction
                        },
                        animationSpec = springSpec
                    ) + fadeOut()
                    
                    enterTransition togetherWith exitTransition
                } else {
                    // Disappearing: slide out horizontally
                    val exitTransition = slideOutHorizontally(
                        targetOffsetX = { fullWidth -> 
                            val direction = if (iconIndex < selectedIndex) fullWidth else -fullWidth
                            direction
                        },
                        animationSpec = springSpec
                    ) + fadeOut()
                    
                    val enterTransition = slideInHorizontally(
                        initialOffsetX = { fullWidth -> 
                            val direction = if (iconIndex < selectedIndex) -fullWidth else fullWidth
                            direction
                        },
                        animationSpec = springSpec
                    ) + fadeIn()
                    
                    enterTransition togetherWith exitTransition
                }
            },
            label = "bubbleTransition"
        ) { selected ->
            if (selected) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .zIndex(10f) // Ensure bubble is on top of everything - above Tab Bar background
                        .clip(CircleShape)
                        .background(bubbleGradient)
                        // Dynamic shadow matching iOS (opacity changes with bounce)
                        .shadow(
                            elevation = if (isBouncing) 12.dp else 8.dp,
                            shape = CircleShape,
                            spotColor = Color(0xFF1259D1).copy(alpha = if (isBouncing) 0.7f else 0.5f)
                        )
                        .graphicsLayer {
                            // Apply ALL offsets together: continuous bounce + jump + arc
                            // The arc creates the VISIBLE parabolic "jump" effect during horizontal movement
                            // This is the key: arcOffsetY creates the curved path (half-circle) as bubble moves horizontally
                            // IMPORTANT: No clipping - bubble must be fully visible above Tab Bar
                            // Optimize: Use memoized bubbleOffsetY instead of recalculating here
                            translationY = bubbleOffsetY + arcOffsetY
                            scaleX = bubbleScale
                            scaleY = bubbleScale
                        }
                ) {
                // Optimize: Memoize gradients to avoid recreating on every recomposition
                val innerGlowGradient = remember {
                    Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.4f),
                            Color.White.copy(alpha = 0.1f),
                            Color.Transparent
                        ),
                        center = androidx.compose.ui.geometry.Offset(12f, 12f),
                        radius = 18f
                    )
                }
                
                val outerGlowGradient = remember {
                    Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.3f),
                            Color.Transparent
                        ),
                        start = androidx.compose.ui.geometry.Offset(0f, 0f),
                        end = androidx.compose.ui.geometry.Offset(40f, 40f)
                    )
                }
                
                // Inner radial gradient glow (iOS style)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            innerGlowGradient,
                            shape = CircleShape
                        )
                )
                
                // Outer glow border (iOS style)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .border(
                            width = 1.5.dp,
                            brush = outerGlowGradient,
                            shape = CircleShape
                        )
                )
                }
            } else {
                // Empty when not selected (for AnimatedContent)
                Spacer(modifier = Modifier.size(40.dp))
            }
        }
        
        // Icon
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier
                .size(18.dp)
                .graphicsLayer {
                    translationY = iconOffsetY // Icon follows bubble position but doesn't float continuously
                    scaleX = iconScale
                    scaleY = iconScale
                }
        )
    }
}

