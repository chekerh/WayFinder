package tn.esprit.wayfinder.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.graphics.graphicsLayer
import tn.esprit.wayfinder.models.AchievementType

@Composable
fun AchievementBadge(
    achievementType: AchievementType,
    isUnlocked: Boolean = false,
    progress: Int = 0,
    target: Int = 1,
    modifier: Modifier = Modifier
) {
    val progressValue = if (target > 0) (progress.toFloat() / target.toFloat()).coerceIn(0f, 1f) else 0f
    
    Box(
        modifier = modifier.size(56.dp),
        contentAlignment = Alignment.Center
    ) {
        // Background circle with gradient
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(
                    if (isUnlocked) {
                        Brush.radialGradient(
                            colors = listOf(
                                getAchievementColor(achievementType),
                                getAchievementColor(achievementType).copy(alpha = 0.7f)
                            )
                        )
                    } else {
                        Brush.radialGradient(
                            colors = listOf(
                                Color.Gray.copy(alpha = 0.3f),
                                Color.Gray.copy(alpha = 0.1f)
                            )
                        )
                    }
                )
        )
        
        // Icon
        Icon(
            imageVector = getAchievementIcon(achievementType),
            contentDescription = null,
            tint = if (isUnlocked) Color.White else Color.Gray,
            modifier = Modifier.size(28.dp)
        )
        
        // Progress ring (if not unlocked)
        if (!isUnlocked && progressValue > 0) {
            CircularProgressIndicator(
                progress = { progressValue },
                modifier = Modifier.fillMaxSize(),
                strokeWidth = 3.dp,
                color = getAchievementColor(achievementType),
                trackColor = Color.Transparent
            )
        }
        
        // Checkmark if unlocked
        if (isUnlocked) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = "Unlocked",
                tint = Color.White,
                modifier = Modifier
                    .size(20.dp)
                    .align(Alignment.BottomEnd)
                    .background(Color(0xFF4CAF50), CircleShape)
                    .padding(2.dp)
            )
        }
    }
}

@Composable
fun AchievementCelebration(
    achievement: tn.esprit.wayfinder.models.Achievement,
    onDismiss: () -> Unit
) {
    var isVisible by remember { mutableStateOf(true) }
    val scale by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "scale"
    )
    val alpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(300),
        label = "alpha"
    )

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(3000)
        isVisible = false
        kotlinx.coroutines.delay(300)
        onDismiss()
    }

    if (isVisible) {
        Popup(
            onDismissRequest = { isVisible = false },
            alignment = Alignment.Center,
            properties = PopupProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = true
            )
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .scale(scale)
                    .graphicsLayer { this.alpha = alpha },
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Confetti animation placeholder
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        getAchievementColor(achievement.type),
                                        getAchievementColor(achievement.type).copy(alpha = 0.7f)
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = getAchievementIcon(achievement.type),
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                    
                    Text(
                        text = "🎉 Achievement Unlocked! 🎉",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = getAchievementColor(achievement.type)
                    )
                    
                    Text(
                        text = achievement.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Text(
                        text = achievement.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Button(
                        onClick = { isVisible = false },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = getAchievementColor(achievement.type)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Awesome!")
                    }
                }
            }
        }
    }
}

private fun getAchievementColor(type: AchievementType): Color {
    return when (type) {
        AchievementType.FIRST_BOOKING -> Color(0xFF4CAF50)
        AchievementType.EXPLORER -> Color(0xFF2196F3)
        AchievementType.WORLD_TRAVELER -> Color(0xFF9C27B0)
        AchievementType.FREQUENT_FLYER -> Color(0xFFFF9800)
        AchievementType.BUDGET_SAVER -> Color(0xFF00BCD4)
        AchievementType.EARLY_BIRD -> Color(0xFFE91E63)
        AchievementType.SOCIAL_BUTTERFLY -> Color(0xFF3F51B5)
        AchievementType.REVIEWER -> Color(0xFF795548)
        AchievementType.STREAK_MASTER -> Color(0xFFFF5722)
        AchievementType.POINTS_COLLECTOR -> Color(0xFFFFC107)
    }
}

private fun getAchievementIcon(type: AchievementType): ImageVector {
    return when (type) {
        AchievementType.FIRST_BOOKING -> Icons.Default.FlightTakeoff
        AchievementType.EXPLORER -> Icons.Default.Explore
        AchievementType.WORLD_TRAVELER -> Icons.Default.Public
        AchievementType.FREQUENT_FLYER -> Icons.Default.Flight
        AchievementType.BUDGET_SAVER -> Icons.Default.Savings
        AchievementType.EARLY_BIRD -> Icons.Default.Schedule
        AchievementType.SOCIAL_BUTTERFLY -> Icons.Default.Share
        AchievementType.REVIEWER -> Icons.Default.Star
        AchievementType.STREAK_MASTER -> Icons.Default.LocalFireDepartment
        AchievementType.POINTS_COLLECTOR -> Icons.Default.EmojiEvents
    }
}

