package tn.esprit.wayfinder.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AllInclusive
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun PointsDisplayCard(
    totalPoints: Int,
    lifetimePoints: Int,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "points")
    val sparkleScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "sparkle"
    )
    
    val colorScheme = MaterialTheme.colorScheme
    val isDark = colorScheme.background.luminance() < 0.5f
    val cardGradient = if (isDark) {
        listOf(
            colorScheme.surfaceVariant.copy(alpha = 0.95f),
            colorScheme.surface.copy(alpha = 0.9f)
        )
    } else {
        listOf(
            Color(0xFFF5F5F5).copy(alpha = 0.9f),
            Color(0xFFE8E8E8).copy(alpha = 0.85f),
            Color(0xFFF5F5F5).copy(alpha = 0.9f)
        )
    }
    val overlayGradient = if (isDark) {
        listOf(
            colorScheme.surfaceVariant.copy(alpha = 0.85f),
            colorScheme.surface.copy(alpha = 0.8f)
        )
    } else {
        listOf(
            Color(0xFFFFFFFF).copy(alpha = 0.6f),
            Color(0xFFFFFFFF).copy(alpha = 0.5f)
        )
    }
    val borderBrush = Brush.linearGradient(
        colors = if (isDark) {
            listOf(
                colorScheme.outlineVariant.copy(alpha = 0.7f),
                colorScheme.outline.copy(alpha = 0.5f),
                colorScheme.outlineVariant.copy(alpha = 0.7f)
            )
        } else {
            listOf(
                Color(0xFFCCCCCC).copy(alpha = 0.5f),
                Color(0xFFDDDDDD).copy(alpha = 0.4f),
                Color(0xFFCCCCCC).copy(alpha = 0.5f)
            )
        }
    )
    val primaryTextColor = if (isDark) colorScheme.onSurface else Color(0xFF111111)
    val accentColor = if (isDark) colorScheme.primary else Color(0xFFFFC107)
    val successColor = if (isDark) colorScheme.tertiary else Color(0xFF4CAF50)
    Box(
        modifier = modifier
            .graphicsLayer {
                compositingStrategy = androidx.compose.ui.graphics.CompositingStrategy.ModulateAlpha
            }
            .background(Brush.verticalGradient(cardGradient), shape = RoundedCornerShape(16.dp))
            .background(Brush.verticalGradient(overlayGradient), shape = RoundedCornerShape(16.dp))
            .border(width = 1.dp, brush = borderBrush, shape = RoundedCornerShape(16.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Trophy icon with animation
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFFFFC107),
                                    Color(0xFFFF9800)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.EmojiEvents,
                        contentDescription = "Points",
                        tint = Color.White,
                        modifier = Modifier
                            .size(28.dp)
                            .scale(sparkleScale)
                    )
                }
                
                Column {
                    Text(
                        text = "$totalPoints",
                        style = MaterialTheme.typography.displaySmall.copy(
                            fontSize = if (isDark) 32.sp else MaterialTheme.typography.displaySmall.fontSize
                        ),
                        fontWeight = FontWeight.Bold,
                        color = accentColor
                    )
                    Text(
                        text = "Points",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = if (isDark) 16.sp else MaterialTheme.typography.bodyMedium.fontSize
                        ),
                        color = primaryTextColor,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = "Lifetime: $lifetimePoints",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = if (isDark) 14.sp else MaterialTheme.typography.bodySmall.fontSize
                    ),
                    color = primaryTextColor,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Available now",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = if (isDark) 14.sp else MaterialTheme.typography.bodySmall.fontSize
                    ),
                    color = successColor,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun CompactPointsBadge(
    points: Int,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = Color(0xFFFFC107).copy(alpha = 0.2f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.Default.EmojiEvents,
                contentDescription = null,
                tint = Color(0xFFFFC107),
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = "$points pts",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFFC107)
            )
        }
    }
}

@Composable
fun ProfileRewardsCard(
    totalPoints: Int,
    lifetimePoints: Int,
    currentStreak: Int,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val isDark = colorScheme.background.luminance() < 0.5f

    val cardGradient = if (isDark) {
        listOf(
            colorScheme.surfaceVariant.copy(alpha = 0.98f),
            colorScheme.surface.copy(alpha = 0.96f)
        )
    } else {
        listOf(
            Color(0xFFFFFFFF),
            Color(0xFFF7F9FC)
        )
    }

    val borderBrush = Brush.linearGradient(
        colors = if (isDark) {
            listOf(
                colorScheme.outlineVariant.copy(alpha = 0.6f),
                colorScheme.outline.copy(alpha = 0.4f),
                colorScheme.outlineVariant.copy(alpha = 0.6f)
            )
        } else {
            listOf(
                Color(0xFFE3E7EF),
                Color(0xFFD5D9E3),
                Color(0xFFE3E7EF)
            )
        }
    )

    // Simple UI-only level calculation similar to iOS card
    val level = (totalPoints / 100).coerceAtLeast(1)
    val levelProgress = ((totalPoints % 100) / 100f).coerceIn(0f, 1f)

    Box(
        modifier = modifier
            .graphicsLayer {
                compositingStrategy = androidx.compose.ui.graphics.CompositingStrategy.ModulateAlpha
            }
            .background(
                brush = Brush.verticalGradient(cardGradient),
                shape = RoundedCornerShape(24.dp)
            )
            .border(
                width = 1.dp,
                brush = borderBrush,
                shape = RoundedCornerShape(24.dp)
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Level & progress row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Level $level",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = colorScheme.onSurface
                    )
                    LinearProgressIndicator(
                        progress = { levelProgress },
                        modifier = Modifier
                            .width(190.dp)
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = Color(0xFF3B82F6),
                        trackColor = Color(0xFFE0EDFF)
                    )
                }
                Text(
                    text = "x10",
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 18.sp),
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF3B82F6)
                )
            }

            // Stats row (Points / Lifetime / Streak)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.EmojiEvents,
                        contentDescription = "Points",
                        tint = Color(0xFFFFC107),
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "$totalPoints",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.onSurface
                    )
                    Text(
                        text = "Points",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.AllInclusive,
                        contentDescription = "Lifetime",
                        tint = Color(0xFF6366F1),
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "$lifetimePoints",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.onSurface
                    )
                    Text(
                        text = "Lifetime",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.LocalFireDepartment,
                        contentDescription = "Day Streak",
                        tint = Color(0xFFFF5722),
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "$currentStreak",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.onSurface
                    )
                    Text(
                        text = "Day Streak",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            }

            // Info text similar to iOS screen
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = Color(0xFF3B82F6),
                    modifier = Modifier
                        .size(18.dp)
                        .padding(top = 2.dp)
                )
                Text(
                    text = "Earn points by using the app: book a flight (+50), share a journey (+30), analyze an outfit (+20), and more. Use your points to unlock badges, access premium features, and get discounts on your bookings.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    lineHeight = 16.sp
                )
            }
        }
    }
}


