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
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.text.style.TextAlign
import java.text.SimpleDateFormat
import java.util.*
import tn.esprit.wayfinder.models.DiscussionComment
import tn.esprit.wayfinder.models.JourneyComment
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
import androidx.compose.ui.res.painterResource
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
import tn.esprit.wayfinder.viewmodels.DiscussionViewModel
import tn.esprit.wayfinder.viewmodels.JourneyViewModel
import tn.esprit.wayfinder.models.DiscussionUser

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
    val discussionViewModel: DiscussionViewModel = viewModel(
        factory = ViewModelFactory(context.applicationContext as Application)
    )
    val journeyViewModel: JourneyViewModel = viewModel(
        factory = ViewModelFactory(context.applicationContext as Application)
    )
    val uiState by reelsViewModel.uiState.collectAsState()
    val tokenManager = remember { TokenManager(context) }
    val currentUser = remember { tokenManager.getUser() }
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialIndex)
    val scope = rememberCoroutineScope()
    val colorScheme = MaterialTheme.colorScheme
    
    // State for comment dialog
    var showCommentDialog by remember { mutableStateOf(false) }
    var commentItem by remember { mutableStateOf<ReelContentItem?>(null) }
    var commentText by remember { mutableStateOf("") }
    var isSubmittingComment by remember { mutableStateOf(false) }
    
    // Load reels on first composition
    LaunchedEffect(Unit) {
        reelsViewModel.loadReelsFeed(refresh = true)
    }
    
    // Track if we've already scrolled to initial position
    var hasScrolledToInitial by remember { mutableStateOf(false) }
    
    // Scroll to initial index only once when data first loads
    LaunchedEffect(uiState) {
        val successState = uiState as? ReelsUiState.Success
        if (successState != null && successState.items.isNotEmpty() && !hasScrolledToInitial) {
            scope.launch {
                listState.animateScrollToItem(
                    initialIndex.coerceIn(0, successState.items.size - 1)
                )
                hasScrolledToInitial = true
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
                                        // Open comment dialog
                                        commentItem = item
                                        commentText = ""
                                        showCommentDialog = true
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
        
        // Comments Bottom Sheet
        if (showCommentDialog && commentItem != null) {
            CommentsBottomSheet(
                item = commentItem!!,
                onDismiss = {
                    showCommentDialog = false
                    commentText = ""
                    commentItem = null
                },
                discussionViewModel = discussionViewModel,
                journeyViewModel = journeyViewModel,
                reelsViewModel = reelsViewModel,
                currentUser = currentUser,
                scope = scope
            )
        }
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
    var showFullCaption by remember { mutableStateOf(false) }
    
    // Initialize liked state from item data, but maintain local state for immediate UI updates
    val initialIsLiked = remember(item.id) {
        when (item) {
            is ReelContentItem.PostItem -> currentUserId != null && item.post.likedBy.contains(currentUserId)
            is ReelContentItem.JourneyItem -> item.journey.isLiked
        }
    }
    
    var isLiked by remember(item.id) { mutableStateOf(initialIsLiked) }
    var likesCount by remember(item.id) { mutableStateOf(item.likesCount) }
    
    // Sync with item data when item ID changes (new item), but preserve local optimistic updates
    LaunchedEffect(item.id) {
        val currentIsLiked = when (item) {
            is ReelContentItem.PostItem -> currentUserId != null && item.post.likedBy.contains(currentUserId)
            is ReelContentItem.JourneyItem -> item.journey.isLiked
        }
        isLiked = currentIsLiked
        likesCount = item.likesCount
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
            // Profile picture - Display only the actual profile photo
            val avatarUrl = if (creatorAvatar != null && creatorAvatar.isNotBlank()) {
                if (creatorAvatar.startsWith("http")) {
                    creatorAvatar
                } else {
                    "https://wayfinder-api-w92x.onrender.com$creatorAvatar"
                }
            } else {
                null
            }
            
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onProfileClick)
                    .border(2.dp, Color.White, CircleShape)
            ) {
                if (avatarUrl != null) {
                    AsyncImage(
                        model = avatarUrl,
                        contentDescription = "Creator",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop,
                        placeholder = painterResource(id = R.drawable.europe),
                        error = painterResource(id = R.drawable.europe)
                    )
                } else {
                    // Only show placeholder icon if no image URL is available
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.White.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Creator",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
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
                        // Update UI immediately (optimistic update)
                        val wasLiked = isLiked
                        isLiked = !isLiked
                        likesCount = if (wasLiked) likesCount - 1 else likesCount + 1
                        // Then trigger the actual like action
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
                    text = formatCount(likesCount),
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
                        imageVector = Icons.AutoMirrored.Outlined.Comment,
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

@Composable
fun InstagramStyleCommentCard(
    comment: DiscussionComment,
    currentUserId: String?,
    onLikeClick: () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme
    val isLiked = currentUserId != null && comment.likedBy.contains(currentUserId)
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.Top
    ) {
        // Profile Picture
        val userImageUrl = comment.userId.profileImageUrl?.let { url ->
            if (url.startsWith("http")) url else "https://wayfinder-api-w92x.onrender.com$url"
        }
        AsyncImage(
            model = userImageUrl ?: "",
            contentDescription = "User Avatar",
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape),
            contentScale = ContentScale.Crop,
            placeholder = painterResource(id = R.drawable.europe),
            error = painterResource(id = R.drawable.europe)
        )
        
        Spacer(modifier = Modifier.width(12.dp))
        
        // Comment Content
        Column(modifier = Modifier.weight(1f)) {
            // User Name and Comment Text (inline)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "${comment.userId.firstName} ${comment.userId.lastName}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.onSurface
                )
                Text(
                    text = comment.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colorScheme.onSurface
                )
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            // Date, Like, Reply (second line)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = formatDate(comment.createdAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = colorScheme.onSurfaceVariant
                )
                TextButton(
                    onClick = onLikeClick,
                    contentPadding = PaddingValues(0.dp),
                    modifier = Modifier.height(24.dp)
                ) {
                    Text(
                        text = StringTranslator.translate(context, if (isLiked) "J'aime" else "Aimer"),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isLiked) Color(0xFFFF1744) else colorScheme.onSurfaceVariant,
                        fontWeight = if (isLiked) FontWeight.Bold else FontWeight.Normal
                    )
                }
                if (comment.likesCount > 0) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Favorite,
                            contentDescription = "Likes",
                            tint = Color(0xFFFF1744),
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = "${comment.likesCount}",
                            style = MaterialTheme.typography.bodySmall,
                            color = colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun InstagramStyleJourneyCommentCard(
    comment: JourneyComment,
    currentUserId: String?,
    onLikeClick: () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.Top
    ) {
        // Profile Picture
        val userImageUrl = comment.user?.profileImageUrl?.let { url ->
            if (url.startsWith("http")) url else "https://wayfinder-api-w92x.onrender.com$url"
        }
        AsyncImage(
            model = userImageUrl ?: "",
            contentDescription = "User Avatar",
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape),
            contentScale = ContentScale.Crop,
            placeholder = painterResource(id = R.drawable.europe),
            error = painterResource(id = R.drawable.europe)
        )
        
        Spacer(modifier = Modifier.width(12.dp))
        
        // Comment Content
        Column(modifier = Modifier.weight(1f)) {
            // User Name and Comment Text (inline)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = comment.user?.let { "${it.firstName ?: ""} ${it.lastName ?: ""}".trim().ifEmpty { it.username } } ?: "User",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.onSurface
                )
                Text(
                    text = comment.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colorScheme.onSurface
                )
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            // Date (second line)
            if (comment.createdAt != null) {
                Text(
                    text = formatDate(comment.createdAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun formatDate(dateString: String?): String {
    if (dateString == null) return ""
    return try {
        val formats = listOf(
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd'T'HH:mm:ss",
            "yyyy-MM-dd HH:mm:ss"
        )
        var parsedDate: Date? = null
        for (format in formats) {
            try {
                val parser = SimpleDateFormat(format, Locale.getDefault())
                parsedDate = parser.parse(dateString)
                if (parsedDate != null) break
            } catch (_: Exception) {
                continue
            }
        }
        parsedDate?.let {
            val formatter = SimpleDateFormat("dd MMM", Locale.getDefault())
            formatter.format(it)
        } ?: dateString
    } catch (_: Exception) {
        dateString
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommentsBottomSheet(
    item: ReelContentItem,
    onDismiss: () -> Unit,
    discussionViewModel: DiscussionViewModel,
    journeyViewModel: JourneyViewModel,
    reelsViewModel: ReelsViewModel,
    currentUser: tn.esprit.wayfinder.models.User?,
    scope: kotlinx.coroutines.CoroutineScope
) {
    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var commentText by remember { mutableStateOf("") }
    var isSubmittingComment by remember { mutableStateOf(false) }
    
    // Get comments based on item type
    val postDetailState by discussionViewModel.postDetailState.collectAsState()
    val journeyComments by journeyViewModel.comments.collectAsState()
    
    // Load comments based on item type - ensure they're loaded when sheet opens
    LaunchedEffect(item.id) {
        when (item) {
            is ReelContentItem.PostItem -> {
                android.util.Log.d("CommentsBottomSheet", "Loading post detail for post ID: ${item.post.id}")
                discussionViewModel.loadPostDetail(item.post.id)
            }
            is ReelContentItem.JourneyItem -> {
                android.util.Log.d("CommentsBottomSheet", "Loading comments for journey ID: ${item.journey.id}")
                journeyViewModel.loadComments(item.journey.id)
            }
        }
    }
    
    // Check if the loaded post matches the current item
    val postComments = when (item) {
        is ReelContentItem.PostItem -> {
            val successState = postDetailState as? tn.esprit.wayfinder.viewmodels.PostDetailUiState.Success
            if (successState != null && successState.post.id == item.post.id) {
                android.util.Log.d("CommentsBottomSheet", "Post ID matches! Comments count: ${successState.comments.size}")
                successState.comments
            } else {
                android.util.Log.d("CommentsBottomSheet", "Post ID doesn't match or state is not Success. State: ${postDetailState::class.simpleName}, Post ID in state: ${(successState?.post?.id)}, Item post ID: ${item.post.id}")
                emptyList()
            }
        }
        else -> emptyList<DiscussionComment>()
    }
    
    val journeyCommentsList = when (item) {
        is ReelContentItem.JourneyItem -> journeyComments
        else -> emptyList<JourneyComment>()
    }
    
    // Debug: Log state for troubleshooting
    LaunchedEffect(postDetailState, item.id) {
        when (item) {
            is ReelContentItem.PostItem -> {
                android.util.Log.d("CommentsBottomSheet", "Post ID: ${item.post.id}, State: ${postDetailState::class.simpleName}, Comments count: ${
                    (postDetailState as? tn.esprit.wayfinder.viewmodels.PostDetailUiState.Success)?.comments?.size ?: 0
                }")
            }
            is ReelContentItem.JourneyItem -> {
                android.util.Log.d("CommentsBottomSheet", "Journey ID: ${item.journey.id}, Comments count: ${journeyComments.size}")
            }
        }
    }
    
    val commentsCount = when (item) {
        is ReelContentItem.PostItem -> postComments.size
        is ReelContentItem.JourneyItem -> journeyCommentsList.size
    }
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colorScheme.surface,
        dragHandle = {
            Box(
                modifier = Modifier
                    .width(40.dp)
                    .height(4.dp)
                    .padding(vertical = 12.dp)
                    .background(
                        colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        RoundedCornerShape(2.dp)
                    )
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 600.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = StringTranslator.translate(context, "Commentaires"),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Fermer",
                        tint = colorScheme.onSurface
                    )
                }
            }
            
            HorizontalDivider()
            
            // Comments List
            when (item) {
                is ReelContentItem.PostItem -> {
                    val currentPostState = postDetailState
                    when (currentPostState) {
                        is tn.esprit.wayfinder.viewmodels.PostDetailUiState.Loading -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        }
                        is tn.esprit.wayfinder.viewmodels.PostDetailUiState.Error -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = currentPostState.message,
                                    color = colorScheme.error,
                                    style = MaterialTheme.typography.bodyMedium,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                        is tn.esprit.wayfinder.viewmodels.PostDetailUiState.Success -> {
                            // Verify post ID matches
                            val postIdMatches = currentPostState.post.id == item.post.id
                            
                            // If post ID doesn't match, reload for the correct post
                            if (!postIdMatches) {
                                LaunchedEffect(item.post.id) {
                                    discussionViewModel.loadPostDetail(item.post.id)
                                }
                            }
                            
                            // Use postComments which already has the correct filtering logic
                            val actualComments = postComments
                            
                            if (actualComments.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f)
                                        .padding(32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = StringTranslator.translate(context, "Aucun commentaire pour le moment"),
                                        color = colorScheme.onSurfaceVariant,
                                        style = MaterialTheme.typography.bodyMedium,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            } else {
                                LazyColumn(
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    items(actualComments) { comment ->
                                        InstagramStyleCommentCard(
                                            comment = comment,
                                            currentUserId = currentUser?.id,
                                            onLikeClick = {
                                                discussionViewModel.likeComment(
                                                    comment.id,
                                                    item.post.id,
                                                    currentUser?.id
                                                )
                                            }
                                        )
                                        
                                        // Display replies if any
                                        if (comment.replies.isNotEmpty()) {
                                            comment.replies.forEach { reply ->
                                                Spacer(modifier = Modifier.height(4.dp))
                                                InstagramStyleCommentCard(
                                                    comment = reply,
                                                    currentUserId = currentUser?.id,
                                                    onLikeClick = {
                                                        discussionViewModel.likeComment(
                                                            reply.id,
                                                            item.post.id,
                                                            currentUser?.id
                                                        )
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        is tn.esprit.wayfinder.viewmodels.PostDetailUiState.Idle -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        }
                    }
                }
                is ReelContentItem.JourneyItem -> {
                    if (journeyCommentsList.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = StringTranslator.translate(context, "Aucun commentaire pour le moment"),
                                color = colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(journeyCommentsList) { comment ->
                                InstagramStyleJourneyCommentCard(
                                    comment = comment,
                                    currentUserId = currentUser?.id,
                                    onLikeClick = {
                                        // Handle journey comment like if needed
                                    }
                                )
                            }
                        }
                    }
                }
            }
            
            HorizontalDivider()
            
            // Comment Input (Instagram style)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Current User Profile Picture
                val currentUserImageUrl = currentUser?.profileImageUrl?.let { url ->
                    if (url.startsWith("http")) url else "https://wayfinder-api-w92x.onrender.com$url"
                }
                if (currentUserImageUrl != null) {
                    AsyncImage(
                        model = currentUserImageUrl,
                        contentDescription = "Your Avatar",
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop,
                        placeholder = painterResource(id = R.drawable.europe),
                        error = painterResource(id = R.drawable.europe)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Avatar",
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(colorScheme.surfaceVariant, CircleShape)
                            .padding(8.dp),
                        tint = colorScheme.onSurfaceVariant
                    )
                }
                
                // Text Input
                OutlinedTextField(
                    value = commentText,
                    onValueChange = { commentText = it },
                    modifier = Modifier.weight(1f),
                    placeholder = {
                        Text(StringTranslator.translate(context, "Ajoutez un commentaire..."))
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent
                    ),
                    trailingIcon = {
                        if (commentText.isNotBlank()) {
                            TextButton(
                                onClick = {
                                    if (!isSubmittingComment) {
                                        isSubmittingComment = true
                                        val currentUserObj = currentUser?.let {
                                            DiscussionUser(
                                                id = it.id,
                                                username = it.username ?: "",
                                                firstName = it.firstName ?: "",
                                                lastName = it.lastName ?: "",
                                                profileImageUrl = it.profileImageUrl
                                            )
                                        }
                                        
                                        scope.launch {
                                            val textToSubmit = commentText
                                            try {
                                                commentText = "" // Clear immediately for better UX
                                                reelsViewModel.incrementCommentCount(item.id)
                                                
                                                when (item) {
                                                    is ReelContentItem.PostItem -> {
                                                        // Create comment (has optimistic update if state is loaded)
                                                        discussionViewModel.createComment(
                                                            item.post.id,
                                                            textToSubmit,
                                                            null,
                                                            currentUserObj
                                                        )
                                                        // Reload after a short delay to sync with server
                                                        kotlinx.coroutines.delay(500)
                                                        discussionViewModel.loadPostDetail(item.post.id)
                                                    }
                                                    is ReelContentItem.JourneyItem -> {
                                                        // Add comment
                                                        journeyViewModel.addComment(
                                                            item.journey.id,
                                                            textToSubmit,
                                                            null
                                                        )
                                                        // Reload after delay to sync
                                                        kotlinx.coroutines.delay(1000)
                                                        journeyViewModel.loadComments(item.journey.id)
                                                    }
                                                }
                                            } catch (e: Exception) {
                                                // On error, restore text and reload to get current state
                                                commentText = textToSubmit
                                                when (item) {
                                                    is ReelContentItem.PostItem -> {
                                                        discussionViewModel.loadPostDetail(item.post.id)
                                                    }
                                                    is ReelContentItem.JourneyItem -> {
                                                        journeyViewModel.loadComments(item.journey.id)
                                                    }
                                                }
                                            } finally {
                                                isSubmittingComment = false
                                            }
                                        }
                                    }
                                },
                                enabled = !isSubmittingComment
                            ) {
                                if (isSubmittingComment) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Text(
                                        text = StringTranslator.translate(context, "Publier"),
                                        color = Color(0xFF1976D2),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(
                        onSend = {
                            if (commentText.isNotBlank() && !isSubmittingComment) {
                                // Trigger the same action as the button
                                val currentUserObj = currentUser?.let {
                                    DiscussionUser(
                                        id = it.id,
                                        username = it.username ?: "",
                                        firstName = it.firstName ?: "",
                                        lastName = it.lastName ?: "",
                                        profileImageUrl = it.profileImageUrl
                                    )
                                }
                                
                                scope.launch {
                                    val textToSubmit = commentText
                                    try {
                                        commentText = ""
                                        reelsViewModel.incrementCommentCount(item.id)
                                        
                                        when (item) {
                                            is ReelContentItem.PostItem -> {
                                                discussionViewModel.createComment(
                                                    item.post.id,
                                                    textToSubmit,
                                                    null,
                                                    currentUserObj
                                                )
                                                kotlinx.coroutines.delay(1500)
                                                discussionViewModel.loadPostDetail(item.post.id)
                                            }
                                            is ReelContentItem.JourneyItem -> {
                                                journeyViewModel.addComment(
                                                    item.journey.id,
                                                    textToSubmit,
                                                    null
                                                )
                                                kotlinx.coroutines.delay(1500)
                                                journeyViewModel.loadComments(item.journey.id)
                                            }
                                        }
                                    } catch (e: Exception) {
                                        commentText = textToSubmit
                                        when (item) {
                                            is ReelContentItem.PostItem -> {
                                                discussionViewModel.loadPostDetail(item.post.id)
                                            }
                                            is ReelContentItem.JourneyItem -> {
                                                journeyViewModel.loadComments(item.journey.id)
                                            }
                                        }
                                    } finally {
                                        isSubmittingComment = false
                                    }
                                }
                            }
                        }
                    )
                )
            }
        }
    }
}

