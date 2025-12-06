package tn.esprit.wayfinder.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.ui.graphics.graphicsLayer
import tn.esprit.wayfinder.utils.HapticFeedbackHelper
import androidx.navigation.NavController

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
            .shadow(
                elevation = if (isDark) 8.dp else 4.dp,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            )
            .background(
                color = backgroundColor,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            )
            .padding(horizontal = 0.dp, vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .align(Alignment.Center),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Home
            NavBarIcon(
                icon = Icons.Filled.Home,
                isSelected = selectedIndex == 0,
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
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme
    // Detect dark mode by calculating luminance from RGB values
    // Luminance formula: 0.2126 * R + 0.7152 * G + 0.0722 * B
    val bgColor = colorScheme.background
    val luminance = 0.2126f * bgColor.red + 0.7152f * bgColor.green + 0.0722f * bgColor.blue
    val isDark = luminance < 0.5f
    
    // iOS-style bubble gradient (blue gradient)
    val bubbleGradient = Brush.radialGradient(
        colors = listOf(
            Color(0xFF4096FF), // iOS blue
            Color(0xFF1976D2)  // Darker blue
        )
    )
    
    // Animation for the "jump" effect (iOS style)
    var jumpToggle by remember { mutableStateOf(false) }
    val offsetY by animateFloatAsState(
        targetValue = if (isSelected) {
            if (jumpToggle) -4.dp.value else -8.dp.value
        } else {
            0f
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "offsetY"
    )
    
    // Scale animation
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.1f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "scale"
    )
    
    LaunchedEffect(isSelected) {
        if (isSelected) {
            jumpToggle = !jumpToggle
        }
    }
    
    // Icon color (iOS style: white when selected, accent color when not)
    val iconColor = if (isSelected) {
        Color.White
    } else {
        if (isDark) {
            Color(0xFFA0A0A0) // Light gray in dark mode
        } else {
            Color(0xFF4096FF) // iOS blue in light mode
        }
    }
    
    Box(
        modifier = Modifier
            .size(40.dp)
            .clickable {
                HapticFeedbackHelper.triggerButtonPress(context)
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        // Background bubble (iOS style) - only when selected
        if (isSelected) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(bubbleGradient)
                    .shadow(
                        elevation = 8.dp,
                        shape = CircleShape
                    )
                    .graphicsLayer {
                        translationY = offsetY
                        scaleX = scale
                        scaleY = scale
                    }
            )
        }
        
        // Icon
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier
                .size(18.dp)
                .graphicsLayer {
                    translationY = offsetY
                    scaleX = scale
                    scaleY = scale
                }
        )
    }
}

