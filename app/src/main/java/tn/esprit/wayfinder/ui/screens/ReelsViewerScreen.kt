package tn.esprit.wayfinder.ui.screens

import android.app.Application
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.unit.Velocity
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Comment
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.foundation.border
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.runtime.derivedStateOf
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import tn.esprit.wayfinder.R
import tn.esprit.wayfinder.manager.TokenManager
import tn.esprit.wayfinder.presentation.auth.ViewModelFactory
import tn.esprit.wayfinder.ui.theme.WayFinderBackground
import tn.esprit.wayfinder.utils.HapticFeedbackHelper
import tn.esprit.wayfinder.utils.StringTranslator
import tn.esprit.wayfinder.viewmodels.ReelContentItem
import tn.esprit.wayfinder.viewmodels.ReelsViewModel
import tn.esprit.wayfinder.viewmodels.ReelsUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReelsViewerScreen(
    navController: NavController,
    initialIndex: Int = 0
) {
    val context = LocalContext.current
    val reelsViewModel: ReelsViewModel = viewModel(
        factory = ViewModelFactory(context.applicationContext as Application)
    )
    val uiState by reelsViewModel.uiState.collectAsState()
    val tokenManager = remember { TokenManager(context) }
    val currentUser = remember { tokenManager.getUser() }
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialIndex)
    val scope = rememberCoroutineScope()
    val colorScheme = MaterialTheme.colorScheme
    
    // Load reels on first composition
    LaunchedEffect(Unit) {
        reelsViewModel.loadReelsFeed(refresh = true)
    }
    
    // Scroll to initial index when data loads
    LaunchedEffect(uiState) {
        val successState = uiState as? ReelsUiState.Success
        if (successState != null && successState.items.isNotEmpty()) {
            scope.launch {
                listState.animateScrollToItem(
                    initialIndex.coerceIn(0, successState.items.size - 1)
                )
            }
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        when (val state = uiState) {
            is ReelsUiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color.White)
                }
            }
            is ReelsUiState.Success -> {
                if (state.items.isEmpty()) {
                    EmptyReelsState(
                        onRetry = { reelsViewModel.retry() },
                        onBack = { navController.popBackStack() }
                    )
                } else {
                    val nestedScrollConnection = remember(listState, scope) {
                        reelsNestedScrollConnection(listState, scope)
                    }
                    
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .nestedScroll(nestedScrollConnection),
                        verticalArrangement = Arrangement.spacedBy(0.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        itemsIndexed(state.items) { index, item ->
                            // Calculate scroll offset for smooth fade/scale animation
                            val scrollOffset = remember(listState) {
                                derivedStateOf {
                                    val layoutInfo = listState.layoutInfo
                                    val visibleItem = layoutInfo.visibleItemsInfo.firstOrNull { it.index == index }
                                    if (visibleItem != null) {
                                        val itemCenter = visibleItem.offset + visibleItem.size / 2
                                        val viewportCenter = layoutInfo.viewportSize.height / 2
                                        (itemCenter - viewportCenter).toFloat() / layoutInfo.viewportSize.height
                                    } else {
                                        0f
                                    }
                                }
                            }
                            
                            val offsetValue = scrollOffset.value
                            val alpha by animateFloatAsState(
                                targetValue = 1f - kotlin.math.abs(offsetValue).coerceIn(0f, 0.5f) * 2f,
                                animationSpec = tween(300),
                                label = "reel_alpha_$index"
                            )
                            
                            val scale by animateFloatAsState(
                                targetValue = 1f - kotlin.math.abs(offsetValue).coerceIn(0f, 0.3f) * 0.15f,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessLow
                                ),
                                label = "reel_scale_$index"
                            )
                            
                            Box(
                                modifier = Modifier
                                    .fillParentMaxHeight()
                                    .fillMaxWidth()
                                    .graphicsLayer {
                                        this.alpha = alpha
                                        this.scaleX = scale
                                        this.scaleY = scale
                                    }
                            ) {
                                ReelItem(
                                    item = item,
                                    index = index,
                                    currentUserId = currentUser?.id,
                                    onLike = { reelsViewModel.likeItem(item, currentUser?.id) },
                                    onComment = {
                                        // Navigate to comments
                                        when (item) {
                                            is ReelContentItem.PostItem -> {
                                                navController.navigate("post_detail/${item.post.id}")
                                            }
                                            is ReelContentItem.JourneyItem -> {
                                                navController.navigate("journey_detail/${item.journey.id}")
                                            }
                                        }
                                    },
                                    onShare = {
                                        // Share functionality
                                        HapticFeedbackHelper.triggerButtonPress(context)
                                    },
                                    onProfileClick = {
                                        // Navigate to profile
                                        when (item) {
                                            is ReelContentItem.PostItem -> {
                                                // Could navigate to user profile if we have that route
                                            }
                                            is ReelContentItem.JourneyItem -> {
                                                // Could navigate to user profile
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
            is ReelsUiState.Error -> {
                ErrorReelsState(
                    message = state.message,
                    onRetry = { reelsViewModel.retry() },
                    onBack = { navController.popBackStack() }
                )
            }
            is ReelsUiState.Idle -> {
                // Show loading while idle
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color.White)
                }
            }
        }
        
        // Top bar with back button
        TopAppBar(
            title = { },
            navigationIcon = {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent
            ),
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.6f),
                            Color.Transparent
                        )
                    )
                )
        )
    }
}

@Composable
fun ReelItem(
    item: ReelContentItem,
    index: Int,
    currentUserId: String?,
    onLike: () -> Unit,
    onComment: () -> Unit,
    onShare: () -> Unit,
    onProfileClick: () -> Unit
) {
    val context = LocalContext.current
    var isLiked by remember { mutableStateOf(false) }
    var showFullCaption by remember { mutableStateOf(false) }
    
    // Determine if liked
    isLiked = when (item) {
        is ReelContentItem.PostItem -> currentUserId != null && item.post.likedBy.contains(currentUserId)
        is ReelContentItem.JourneyItem -> item.journey.isLiked
    }
    
    // Get content details
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
    
    val caption = when (item) {
        is ReelContentItem.PostItem -> item.post.content
        is ReelContentItem.JourneyItem -> item.journey.description ?: item.journey.captionText ?: ""
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
    
    val destination = when (item) {
        is ReelContentItem.PostItem -> item.post.destination
        is ReelContentItem.JourneyItem -> item.journey.destination
    }
    
    val tags = when (item) {
        is ReelContentItem.PostItem -> item.post.tags
        is ReelContentItem.JourneyItem -> item.journey.tags
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        // Main content (image/video)
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
                placeholder = androidx.compose.ui.res.painterResource(id = R.drawable.europe),
                error = androidx.compose.ui.res.painterResource(id = R.drawable.europe)
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
        
        // Video play overlay
        if (isVideo) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = "Play",
                    tint = Color.White,
                    modifier = Modifier.size(64.dp)
                )
            }
        }
        
        // Gradient overlay for text readability
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.4f)
                        ),
                        startY = 600f
                    )
                )
        )
        
        // Right side engagement buttons
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 16.dp, bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Profile picture
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
                        .size(48.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onProfileClick)
                        .border(2.dp, Color.White, CircleShape),
                    contentScale = ContentScale.Crop,
                    placeholder = androidx.compose.ui.res.painterResource(id = R.drawable.europe),
                    error = androidx.compose.ui.res.painterResource(id = R.drawable.europe)
                )
            } else {
                // Placeholder circle
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.3f))
                        .clickable(onClick = onProfileClick)
                        .border(2.dp, Color.White, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Creator",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            
            // Like button
            val likeScale by animateFloatAsState(
                targetValue = if (isLiked) 1.2f else 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                ),
                label = "like_scale"
            )
            
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(
                    onClick = {
                        HapticFeedbackHelper.triggerButtonPress(context)
                        onLike()
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .background(Color.Black.copy(alpha = 0.3f), CircleShape)
                        .scale(likeScale)
                ) {
                    Icon(
                        imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = "Like",
                        tint = if (isLiked) Color(0xFFFF1744) else Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Text(
                    text = formatCount(item.likesCount),
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            
            // Comment button
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(
                    onClick = {
                        HapticFeedbackHelper.triggerButtonPress(context)
                        onComment()
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .background(Color.Black.copy(alpha = 0.3f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Comment,
                        contentDescription = "Comment",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Text(
                    text = formatCount(item.commentsCount),
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            
            // Share button
            IconButton(
                onClick = {
                    HapticFeedbackHelper.triggerButtonPress(context)
                    onShare()
                },
                modifier = Modifier
                    .size(48.dp)
                    .background(Color.Black.copy(alpha = 0.3f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = "Share",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        
        // Bottom content overlay
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Creator info
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = creatorName,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                if (destination != null) {
                    Surface(
                        color = Color.White.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = destination,
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
            
            // Caption
            if (caption.isNotBlank()) {
                val displayCaption = if (showFullCaption || caption.length <= 100) {
                    caption
                } else {
                    caption.take(100) + "..."
                }
                
                Text(
                    text = displayCaption,
                    color = Color.White,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    modifier = Modifier
                        .clickable {
                            if (caption.length > 100) {
                                showFullCaption = !showFullCaption
                            }
                        }
                )
                
                if (caption.length > 100 && !showFullCaption) {
                    Text(
                        text = StringTranslator.translate(context, "Voir plus"),
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.clickable { showFullCaption = true }
                    )
                }
            }
            
            // Tags
            if (tags.isNotEmpty()) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    tags.take(3).forEach { tag ->
                        Surface(
                            color = Color.White.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "#$tag",
                                color = Color.White,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }
        
        // Double tap to like
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures(
                        onDoubleTap = { offset ->
                            HapticFeedbackHelper.triggerButtonPress(context)
                            onLike()
                        }
                    )
                }
        )
    }
}

@Composable
fun EmptyReelsState(
    onRetry: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.VideoLibrary,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.6f),
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = StringTranslator.translate(context, "Aucun contenu disponible"),
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = StringTranslator.translate(context, "Partagez vos voyages pour voir du contenu ici"),
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 14.sp
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onRetry) {
            Text(StringTranslator.translate(context, "Réessayer"))
        }
    }
}

@Composable
fun ErrorReelsState(
    message: String,
    onRetry: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
                    Icon(
                        imageVector = Icons.Outlined.ErrorOutline,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.6f),
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = message,
            color = Color.White,
            fontSize = 16.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onRetry) {
            Text(StringTranslator.translate(context, "Réessayer"))
        }
    }
}

fun formatCount(count: Int): String {
    return when {
        count >= 1_000_000 -> "${(count / 1_000_000f).let { if (it % 1 == 0f) it.toInt() else String.format("%.1f", it) }}M"
        count >= 1_000 -> "${(count / 1_000f).let { if (it % 1 == 0f) it.toInt() else String.format("%.1f", it) }}K"
        else -> count.toString()
    }
}

// Helper function for smooth scrolling behavior with snap
fun reelsNestedScrollConnection(
    listState: LazyListState,
    scope: kotlinx.coroutines.CoroutineScope
): NestedScrollConnection {
    return object : NestedScrollConnection {
        override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
            // Snap to nearest item when scrolling stops
            scope.launch {
                kotlinx.coroutines.delay(50) // Small delay for smoother snap
                val layoutInfo = listState.layoutInfo
                if (layoutInfo.visibleItemsInfo.isNotEmpty()) {
                    val firstVisible = layoutInfo.visibleItemsInfo.first()
                    val viewportHeight = layoutInfo.viewportSize.height
                    val itemCenter = firstVisible.offset + firstVisible.size / 2
                    val viewportCenter = viewportHeight / 2
                    val offset = itemCenter - viewportCenter
                    
                    // Determine which item to snap to based on scroll position
                    val targetIndex = if (kotlin.math.abs(offset) > viewportHeight * 0.3f) {
                        // Scrolled more than 30% of viewport, snap to next/previous
                        if (offset > 0) firstVisible.index + 1 else firstVisible.index
                    } else {
                        // Snap back to current item
                        firstVisible.index
                    }
                    
                    if (targetIndex in 0 until layoutInfo.totalItemsCount) {
                        listState.animateScrollToItem(
                            index = targetIndex,
                            scrollOffset = 0
                        )
                    }
                }
            }
            return Velocity.Zero
        }
    }
}

