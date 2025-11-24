package tn.esprit.wayfinder.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.border

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

