package tn.esprit.wayfinder.ui.components

import android.app.Application
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import androidx.compose.ui.res.painterResource
import tn.esprit.wayfinder.R
import tn.esprit.wayfinder.presentation.auth.ViewModelFactory
import tn.esprit.wayfinder.ui.theme.WayFinderSurface
import tn.esprit.wayfinder.utils.StringTranslator
import tn.esprit.wayfinder.viewmodels.ReelContentItem
import tn.esprit.wayfinder.viewmodels.ReelsViewModel
import tn.esprit.wayfinder.viewmodels.ReelsUiState

/**
 * Embedded preview section for reels feed in HomeScreen
 */
@Composable
fun TravelReelsFeed(
    navController: NavController,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val reelsViewModel: ReelsViewModel = viewModel(
        factory = ViewModelFactory(context.applicationContext as Application)
    )
    val uiState by reelsViewModel.uiState.collectAsState()
    val colorScheme = MaterialTheme.colorScheme
    
    // Load reels on first composition
    LaunchedEffect(Unit) {
        reelsViewModel.loadReelsFeed(refresh = true)
    }
    
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Section header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = StringTranslator.translate(context, "Découvrez les voyages"),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = colorScheme.onSurface
            )
            Text(
                text = StringTranslator.translate(context, "Voir tout"),
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF1976D2),
                modifier = Modifier.clickable {
                    navController.navigate("reels_viewer/0")
                }
            )
        }
        
        when (val state = uiState) {
            is ReelsUiState.Loading -> {
                // Loading skeleton
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp)
                ) {
                    items(3) {
                        ReelPreviewCardSkeleton()
                    }
                }
            }
            is ReelsUiState.Success -> {
                if (state.items.isEmpty()) {
                    // Empty state
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = WayFinderSurface
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = StringTranslator.translate(context, "Aucun contenu disponible pour le moment"),
                                style = MaterialTheme.typography.bodyMedium,
                                color = colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    // Show preview cards (first 4 items)
                    val previewItems = state.items.take(4)
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp)
                    ) {
                        items(previewItems.size) { index ->
                            val item = previewItems[index]
                            ReelPreviewCard(
                                item = item,
                                onClick = {
                                    navController.navigate("reels_viewer/$index")
                                }
                            )
                        }
                    }
                }
            }
            is ReelsUiState.Error -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = colorScheme.errorContainer
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = state.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = colorScheme.onErrorContainer
                        )
                        TextButton(onClick = { reelsViewModel.retry() }) {
                            Text(StringTranslator.translate(context, "Réessayer"))
                        }
                    }
                }
            }
            else -> {}
        }
    }
}

@Composable
fun ReelPreviewCard(
    item: ReelContentItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    
    // Get thumbnail and content info
    val thumbnailUrl = when (item) {
        is ReelContentItem.PostItem -> item.post.imageUrl
        is ReelContentItem.JourneyItem -> {
            item.journey.videoUrl ?: item.journey.imageUrls.firstOrNull()
        }
    }
    
    val isVideo = when (item) {
        is ReelContentItem.PostItem -> false
        is ReelContentItem.JourneyItem -> item.journey.videoUrl != null
    }
    
    val creatorName = when (item) {
        is ReelContentItem.PostItem -> "${item.post.userId.firstName} ${item.post.userId.lastName}".trim()
        is ReelContentItem.JourneyItem -> item.journey.user?.let { 
            "${it.firstName ?: ""} ${it.lastName ?: ""}".trim().ifEmpty { it.username }
        } ?: "Traveler"
    }
    
    val creatorAvatar = when (item) {
        is ReelContentItem.PostItem -> item.post.userId.profileImageUrl
        is ReelContentItem.JourneyItem -> item.journey.user?.profileImageUrl
    }
    
    Card(
        modifier = modifier
            .width(160.dp)
            .height(240.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = WayFinderSurface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Thumbnail/Image
            if (thumbnailUrl != null && thumbnailUrl.isNotBlank()) {
                val imageUrl = if (thumbnailUrl.startsWith("http")) {
                    thumbnailUrl
                } else {
                    "https://wayfinder-api-w92x.onrender.com$thumbnailUrl"
                }
                AsyncImage(
                    model = imageUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    placeholder = painterResource(id = R.drawable.europe),
                    error = painterResource(id = R.drawable.europe)
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFF1976D2),
                                    Color(0xFF42A5F5)
                                )
                            )
                        )
                )
            }
            
            // Gradient overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.6f)
                            ),
                            startY = 150f
                        )
                    )
            )
            
            // Video play icon
            if (isVideo) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = "Play video",
                    tint = Color.White,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(40.dp)
                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                        .padding(8.dp)
                )
            }
            
            // Bottom content
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Creator info
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (creatorAvatar != null && creatorAvatar.isNotBlank()) {
                        val avatarUrl = if (creatorAvatar.startsWith("http")) {
                            creatorAvatar
                        } else {
                            "https://wayfinder-api-w92x.onrender.com$creatorAvatar"
                        }
                        AsyncImage(
                            model = avatarUrl,
                            contentDescription = "Creator",
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop,
                            placeholder = painterResource(id = R.drawable.europe),
                            error = painterResource(id = R.drawable.europe)
                        )
                    }
                    Text(
                        text = creatorName,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1
                    )
                }
                
                // Engagement metrics
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Favorite,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = formatCount(item.likesCount),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ReelPreviewCardSkeleton(
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .width(160.dp)
            .height(240.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(32.dp),
                strokeWidth = 3.dp
            )
        }
    }
}

private fun formatCount(count: Int): String {
    return when {
        count >= 1_000_000 -> "${(count / 1_000_000f).let { if (it % 1 == 0f) it.toInt() else String.format("%.1f", it) }}M"
        count >= 1_000 -> "${(count / 1_000f).let { if (it % 1 == 0f) it.toInt() else String.format("%.1f", it) }}K"
        else -> count.toString()
    }
}

