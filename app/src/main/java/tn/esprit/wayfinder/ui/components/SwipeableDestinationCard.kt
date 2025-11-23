package tn.esprit.wayfinder.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.compose.foundation.clickable
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.util.lerp
import kotlin.math.absoluteValue
import tn.esprit.wayfinder.models.FlightDestination
import tn.esprit.wayfinder.ui.screens.DestinationCardContent
import tn.esprit.wayfinder.utils.HapticFeedbackHelper
import tn.esprit.wayfinder.viewmodels.FavoritesViewModel

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun SwipeableDestinationCard(
    destination: FlightDestination,
    favoritesViewModel: FavoritesViewModel,
    onCardClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var offsetX by remember { mutableStateOf(0f) }
    var isDismissed by remember { mutableStateOf(false) }
    var isSaved by remember { mutableStateOf(false) }
    var isFavorite by remember { mutableStateOf(false) }
    
    // Check if favorite on composition
    LaunchedEffect(destination.id) {
        favoritesViewModel.checkFavorite("flight", destination.id) { favorite ->
            isFavorite = favorite
        }
    }
    
    val swipeThreshold = 150f
    val alpha = remember { derivedStateOf { 1f - (offsetX.absoluteValue / 300f).coerceIn(0f, 1f) } }
    
    // Animate dismissal
    AnimatedVisibility(
        visible = !isDismissed,
        exit = slideOutHorizontally(
            targetOffsetX = { if (offsetX > 0) it else -it },
            animationSpec = tween(300)
        ) + fadeOut()
    ) {
        Box(
            modifier = modifier
                .alpha(alpha.value)
                .zIndex(if (offsetX.absoluteValue > 50f) 10f else 1f)
        ) {
            // Swipe indicator overlay
            if (offsetX.absoluteValue > 20f) {
                SwipeIndicator(
                    offsetX = offsetX,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            
            // Main card (keep original design)
            Card(
                modifier = Modifier
                    .offset(x = offsetX.dp)
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragEnd = {
                                when {
                                    offsetX > swipeThreshold -> {
                                        // Swipe right - Save
                                        isSaved = true
                                        isFavorite = true
                                        HapticFeedbackHelper.triggerSuccess(context)
                                        favoritesViewModel.addFavorite(
                                            "flight",
                                            destination.id,
                                            mapOf(
                                                "name" to destination.name,
                                                "city" to destination.city,
                                                "country" to destination.country,
                                                "imageUrl" to (destination.imageUrl ?: ""),
                                                "price" to (destination.price ?: 0.0),
                                                "currency" to destination.currency,
                                                "airline" to (destination.airline ?: "")
                                            )
                                        )
                                        offsetX = 0f
                                    }
                                    offsetX < -swipeThreshold -> {
                                        // Swipe left - Dismiss
                                        isDismissed = true
                                        HapticFeedbackHelper.triggerSwipe(context)
                                    }
                                    else -> {
                                        // Spring back
                                        offsetX = 0f
                                    }
                                }
                            }
                        ) { change, dragAmount ->
                            offsetX += dragAmount.x
                            // Provide haptic feedback at threshold
                            if (offsetX.absoluteValue > swipeThreshold && offsetX.absoluteValue - dragAmount.x.absoluteValue <= swipeThreshold) {
                                HapticFeedbackHelper.triggerSwipe(context)
                            }
                        }
                    }
                    .clickable {
                        HapticFeedbackHelper.triggerCardSelection(context)
                        onCardClick()
                    },
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
            ) {
                DestinationCardContent(
                    destination = destination,
                    favoritesViewModel = favoritesViewModel
                )
            }
        }
    }
    
    // Show save confirmation overlay
    if (isSaved) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            SaveConfirmation(
                onDismiss = { isSaved = false }
            )
        }
    }
}

@Composable
private fun SwipeIndicator(
    offsetX: Float,
    modifier: Modifier = Modifier
) {
    val isSaving = offsetX > 0
    val isDismissing = offsetX < 0
    val progress = (offsetX.absoluteValue / 150f).coerceIn(0f, 1f)
    
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isSaving) {
            Icon(
                imageVector = Icons.Default.Favorite,
                contentDescription = "Save",
                tint = Color(0xFFFF1744),
                modifier = Modifier
                    .size(32.dp)
                    .alpha(progress)
            )
            Text(
                text = "Save",
                color = Color(0xFFFF1744),
                fontWeight = FontWeight.Bold,
                modifier = Modifier.alpha(progress)
            )
        } else if (isDismissing) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Dismiss",
                tint = Color(0xFFFF5722),
                modifier = Modifier
                    .size(32.dp)
                    .alpha(progress)
            )
            Text(
                text = "Dismiss",
                color = Color(0xFFFF5722),
                fontWeight = FontWeight.Bold,
                modifier = Modifier.alpha(progress)
            )
        }
    }
}

@Composable
private fun SaveConfirmation(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isVisible by remember { mutableStateOf(true) }
    
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(1500)
        isVisible = false
        kotlinx.coroutines.delay(300)
        onDismiss()
    }
    
    AnimatedVisibility(
        visible = isVisible,
        enter = scaleIn() + fadeIn(),
        exit = scaleOut() + fadeOut()
    ) {
        Card(
            modifier = modifier,
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF4CAF50)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color.White
                )
                Text(
                    text = "Saved to favorites!",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

