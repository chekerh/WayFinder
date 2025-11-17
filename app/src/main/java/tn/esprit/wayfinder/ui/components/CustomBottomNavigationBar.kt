package tn.esprit.wayfinder.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PersonOutline
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

@Composable
fun CustomBottomNavigationBar(navController: NavController? = null) {
    val currentRoute = navController?.currentBackStackEntry?.destination?.route
    var selectedIndex by remember { 
        mutableStateOf(
            when (currentRoute) {
                "home" -> 0
                "favorites" -> 1
                "chat" -> 2
                "profile" -> 3
                else -> 0
            }
        )
    }
    
    // Update selected index when route changes
    LaunchedEffect(currentRoute) {
        selectedIndex = when (currentRoute) {
            "home" -> 0
            "favorites" -> 1
            "chat" -> 2
            "profile" -> 3
            else -> 0
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(70.dp)
            .background(
                Brush.horizontalGradient(
                    colors = listOf(
                        Color(0xFF1976D2),
                        Color(0xFF64B5F6)
                    )
                )
            )
            .padding(horizontal = 16.dp, vertical = 8.dp)
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
            
            // Favorites
            NavBarIcon(
                icon = Icons.Filled.FavoriteBorder,
                isSelected = selectedIndex == 1,
                onClick = {
                    selectedIndex = 1
                    navController?.navigate("favorites") {
                        popUpTo("home") { inclusive = false }
                    }
                }
            )
            
            // Chat
            NavBarIcon(
                icon = Icons.Outlined.ChatBubbleOutline,
                isSelected = selectedIndex == 2,
                onClick = {
                    selectedIndex = 2
                    navController?.navigate("chat") {
                        popUpTo("home") { inclusive = false }
                    }
                }
            )
            
            // Profile
            NavBarIcon(
                icon = Icons.Filled.PersonOutline,
                isSelected = selectedIndex == 3,
                onClick = {
                    selectedIndex = 3
                    navController?.navigate("profile") {
                        popUpTo("home") { inclusive = false }
                    }
                }
            )
        }
    }
}

@Composable
fun NavBarIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(
                if (isSelected) Color.White else Color.Transparent
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isSelected) Color(0xFF1976D2) else Color.White,
            modifier = Modifier.size(24.dp)
        )
    }
}

