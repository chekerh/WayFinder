package tn.esprit.wayfinder.ui.screens

import android.app.Application
import android.net.Uri
import android.widget.MediaController
import android.widget.VideoView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import tn.esprit.wayfinder.ui.theme.WayFinderTheme
import coil.compose.AsyncImage
import tn.esprit.wayfinder.presentation.auth.ViewModelFactory
import tn.esprit.wayfinder.viewmodels.JourneyViewModel
import tn.esprit.wayfinder.viewmodels.JourneyDetailUiState
import tn.esprit.wayfinder.models.Journey
import tn.esprit.wayfinder.models.JourneyComment

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JourneyDetailScreen(navController: NavController, journeyId: String) {
    val context = LocalContext.current
    val journeyViewModel: JourneyViewModel = viewModel(factory = ViewModelFactory(context.applicationContext as Application))
    val detailState by journeyViewModel.detailUiState.collectAsState()
    val comments by journeyViewModel.comments.collectAsState()
    var commentText by remember { mutableStateOf("") }
    var showCommentDialog by remember { mutableStateOf(false) }
    
    LaunchedEffect(journeyId) {
        journeyViewModel.loadJourneyDetail(journeyId)
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Détails du voyage") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.surface
    ) { paddingValues ->
        when (val state = detailState) {
            is JourneyDetailUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            is JourneyDetailUiState.Success -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .verticalScroll(rememberScrollState())
                ) {
                    JourneyDetailContent(
                        journey = state.journey,
                        comments = comments,
                        commentText = commentText,
                        onCommentTextChange = { commentText = it },
                        onLikeClick = {
                            journeyViewModel.likeJourney(state.journey.id)
                        },
                        onCommentSubmit = { text ->
                            journeyViewModel.addComment(state.journey.id, text)
                            commentText = ""
                        }
                    )
                }
            }
            is JourneyDetailUiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = state.message,
                            color = Color.Red
                        )
                        Button(onClick = { journeyViewModel.loadJourneyDetail(journeyId) }) {
                            Text("Réessayer")
                        }
                    }
                }
            }
            else -> {}
        }
    }
}

@Composable
fun JourneyDetailContent(
    journey: Journey,
    comments: List<JourneyComment>,
    commentText: String,
    onCommentTextChange: (String) -> Unit,
    onLikeClick: () -> Unit,
    onCommentSubmit: (String) -> Unit
) {
    val baseUrl = "https://wayfinder-api-w92x.onrender.com"
    
    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Journey Card
        JourneyDetailCard(
            journey = journey,
            onLikeClick = onLikeClick
        )
        
        // All Images
        if (journey.imageUrls.size > 1) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Toutes les photos (${journey.imageUrls.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        journey.imageUrls.forEach { imageUrl ->
                            val fullUrl = if (imageUrl.startsWith("http")) imageUrl else "$baseUrl$imageUrl"
                            AsyncImage(
                                model = fullUrl,
                                contentDescription = "Journey Image",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(300.dp)
                                    .clip(RoundedCornerShape(12.dp)),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                }
            }
        }
        
        // Comments Section
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Commentaires (${comments.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))
                
                // Comment Input
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = commentText,
                        onValueChange = onCommentTextChange,
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Ajouter un commentaire...") },
                        shape = RoundedCornerShape(24.dp),
                        trailingIcon = {
                            if (commentText.isNotEmpty()) {
                                IconButton(onClick = {
                                    if (commentText.isNotEmpty()) {
                                        onCommentSubmit(commentText)
                                    }
                                }) {
                                    Icon(
                                        imageVector = Icons.Default.Send,
                                        contentDescription = "Send",
                                        tint = Color(0xFF4A90E2)
                                    )
                                }
                            }
                        }
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Comments List
                if (comments.isEmpty()) {
                    Text(
                        text = "Aucun commentaire pour le moment",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                } else {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        comments.forEach { comment ->
                            CommentItem(comment = comment)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun JourneyDetailCard(
    journey: Journey,
    onLikeClick: () -> Unit
) {
    val baseUrl = "https://wayfinder-api-w92x.onrender.com"
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // User Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    AsyncImage(
                        model = journey.user?.profileImageUrl?.let { url ->
                            if (url.startsWith("http")) url else "$baseUrl$url"
                        } ?: "https://i.pravatar.cc/150?img=${journey.userId.hashCode() % 70}",
                        contentDescription = "Profile",
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                    Column {
                        Text(
                            text = "${journey.user?.firstName ?: "User"} ${journey.user?.lastName ?: ""}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = journey.destination,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                }
            }
            
            // Description
            if (!journey.description.isNullOrEmpty()) {
                Text(
                    text = journey.description,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            
            // First Image
            if (journey.imageUrls.isNotEmpty()) {
                val firstImage = journey.imageUrls.first()
                val imageUrl = if (firstImage.startsWith("http")) firstImage else "$baseUrl$firstImage"
                
                AsyncImage(
                    model = imageUrl,
                    contentDescription = "Journey Image",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop
                )
            }
            
            if (journey.videoStatus == "completed" && !journey.videoUrl.isNullOrEmpty()) {
                Text(
                    text = "Vidéo AI générée",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                JourneyVideoPlayer(videoUrl = journey.videoUrl)
            } else if (journey.videoStatus == "processing") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFFFA726).copy(alpha = 0.1f))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = Color(0xFFFFA726),
                        strokeWidth = 2.dp
                    )
                    Text(
                        text = "Génération de la vidéo en cours...",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFFFA726)
                    )
                }
            }
            
            // Tags
            if (journey.tags.isNotEmpty()) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    journey.tags.take(3).forEach { tag ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFE3F2FD)
                        ) {
                            Text(
                                text = "#$tag",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF1976D2)
                            )
                        }
                    }
                }
            }
            
            // Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.clickable { onLikeClick() }
                ) {
                    Icon(
                        imageVector = if (journey.isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Like",
                        tint = if (journey.isLiked) Color(0xFFE91E63) else Color.Gray,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = journey.likesCount.toString(),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Comment,
                        contentDescription = "Comment",
                        tint = Color.Gray,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = journey.commentsCount.toString(),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@Composable
private fun JourneyVideoPlayer(videoUrl: String) {
    val context = LocalContext.current
    AndroidView(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .clip(RoundedCornerShape(12.dp)),
        factory = { ctx ->
            VideoView(ctx).apply {
                val controller = MediaController(ctx)
                controller.setAnchorView(this)
                setMediaController(controller)
                setVideoURI(Uri.parse(videoUrl))
                setOnPreparedListener { player ->
                    player.isLooping = true
                    start()
                }
                tag = videoUrl
            }
        },
        update = { videoView ->
            if (videoView.tag != videoUrl) {
                videoView.tag = videoUrl
                videoView.setVideoURI(Uri.parse(videoUrl))
                videoView.start()
            }
        }
    )
}

@Composable
fun CommentItem(comment: JourneyComment) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        AsyncImage(
            model = comment.user?.profileImageUrl?.let { url ->
                if (url.startsWith("http")) url else "https://wayfinder-api-w92x.onrender.com$url"
            } ?: "https://i.pravatar.cc/150?img=${comment.userId.hashCode() % 70}",
            contentDescription = "Profile",
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape),
            contentScale = ContentScale.Crop
        )
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = "${comment.user?.firstName ?: "User"} ${comment.user?.lastName ?: ""}",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = comment.content,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun JourneyDetailScreenPreview() {
    WayFinderTheme {
        JourneyDetailScreen(rememberNavController(), journeyId = "test-journey-id")
    }
}

