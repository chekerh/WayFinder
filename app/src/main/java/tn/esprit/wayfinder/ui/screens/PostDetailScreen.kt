package tn.esprit.wayfinder.ui.screens

import android.app.Application
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import tn.esprit.wayfinder.R
import tn.esprit.wayfinder.manager.TokenManager
import tn.esprit.wayfinder.models.DiscussionComment
import tn.esprit.wayfinder.models.DiscussionPost
import tn.esprit.wayfinder.presentation.auth.ViewModelFactory
import tn.esprit.wayfinder.viewmodels.DiscussionViewModel
import tn.esprit.wayfinder.viewmodels.PostDetailUiState
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostDetailScreen(navController: NavController, postId: String) {
    val context = LocalContext.current
    val discussionViewModel: DiscussionViewModel = viewModel(factory = ViewModelFactory(context.applicationContext as Application))
    val uiState by discussionViewModel.postDetailState.collectAsState()
    val tokenManager = remember { TokenManager(context) }
    val currentUser = remember { tokenManager.getUser() }
    
    var commentText by remember { mutableStateOf("") }

    LaunchedEffect(postId) {
        discussionViewModel.loadPostDetail(postId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Discussion", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFEAF2FF)
                )
            )
        },
        containerColor = Color(0xFFEAF2FF)
    ) { paddingValues ->
        when (val state = uiState) {
            is PostDetailUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is PostDetailUiState.Success -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    // Post Content
                    PostDetailCard(
                        post = state.post,
                        currentUserId = currentUser?.id,
                        onLikeClick = { discussionViewModel.likePost(state.post.id) }
                    )
                    
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                    
                    // Comments Section
                    Text(
                        text = "Commentaires (${state.comments.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                    
                    if (state.comments.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Aucun commentaire pour le moment",
                                color = Color.Gray,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(state.comments) { comment ->
                                CommentCard(
                                    comment = comment,
                                    currentUserId = currentUser?.id,
                                    onLikeClick = {
                                        discussionViewModel.likeComment(comment.id, state.post.id)
                                    }
                                )
                            }
                        }
                    }
                    
                    // Comment Input
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = commentText,
                            onValueChange = { commentText = it },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("Ajouter un commentaire...") },
                            singleLine = false,
                            maxLines = 3
                        )
                        IconButton(
                            onClick = {
                                if (commentText.isNotBlank()) {
                                    discussionViewModel.createComment(state.post.id, commentText)
                                    commentText = ""
                                }
                            },
                            enabled = commentText.isNotBlank()
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Send,
                                contentDescription = "Envoyer",
                                tint = if (commentText.isNotBlank()) Color(0xFF1976D2) else Color.Gray
                            )
                        }
                    }
                }
            }
            is PostDetailUiState.Error -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(state.message, color = Color.Red)
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = { discussionViewModel.loadPostDetail(postId) }) {
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
fun PostDetailCard(
    post: DiscussionPost,
    currentUserId: String?,
    onLikeClick: () -> Unit
) {
    val isLiked = currentUserId != null && post.likedBy.contains(currentUserId)
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // User Info
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val userImageUrl = post.userId.profileImageUrl?.let { url ->
                    if (url.startsWith("http")) url else "https://wayfinder-api-w92x.onrender.com$url"
                }
                if (userImageUrl != null) {
                    AsyncImage(
                        model = userImageUrl,
                        contentDescription = "User Avatar",
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop,
                        placeholder = painterResource(id = R.drawable.europe),
                        error = painterResource(id = R.drawable.europe)
                    )
                } else {
                    Image(
                        painter = painterResource(id = R.drawable.europe),
                        contentDescription = "User Avatar",
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "${post.userId.firstName} ${post.userId.lastName}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = formatDate(post.createdAt),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Post Title
            Text(
                text = post.title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Post Content
            Text(
                text = post.content,
                style = MaterialTheme.typography.bodyLarge
            )
            
            // Post Image
            post.imageUrl?.let { imageUrl ->
                Spacer(modifier = Modifier.height(12.dp))
                AsyncImage(
                    model = imageUrl,
                    contentDescription = "Post Image",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop,
                    placeholder = painterResource(id = R.drawable.travel_image),
                    error = painterResource(id = R.drawable.travel_image)
                )
            }
            
            // Tags
            if (post.tags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    post.tags.forEach { tag ->
                        AssistChip(
                            onClick = { },
                            label = { Text(tag, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
            }
            
            // Destination
            post.destination?.let { destination ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "📍 $destination",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF1976D2)
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onLikeClick) {
                        Icon(
                            imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            contentDescription = "Like",
                            tint = if (isLiked) Color(0xFFFF1744) else Color.Gray
                        )
                    }
                    Text(
                        text = "${post.likesCount}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@Composable
fun CommentCard(
    comment: DiscussionComment,
    currentUserId: String?,
    onLikeClick: () -> Unit
) {
    val isLiked = currentUserId != null && comment.likedBy.contains(currentUserId)
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            val userImageUrl = comment.userId.profileImageUrl?.let { url ->
                if (url.startsWith("http")) url else "https://wayfinder-api-w92x.onrender.com$url"
            }
            if (userImageUrl != null) {
                AsyncImage(
                    model = userImageUrl,
                    contentDescription = "User Avatar",
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop,
                    placeholder = painterResource(id = R.drawable.europe),
                    error = painterResource(id = R.drawable.europe)
                )
            } else {
                Image(
                    painter = painterResource(id = R.drawable.europe),
                    contentDescription = "User Avatar",
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${comment.userId.firstName} ${comment.userId.lastName}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = comment.content,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = formatDate(comment.createdAt),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                    IconButton(
                        onClick = onLikeClick,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            contentDescription = "Like",
                            tint = if (isLiked) Color(0xFFFF1744) else Color.Gray,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Text(
                        text = "${comment.likesCount}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            }
        }
    }
}

private fun formatDate(dateString: String): String {
    return try {
        val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
        val formatter = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
        formatter.format(parser.parse(dateString) ?: Date())
    } catch (e: Exception) {
        dateString
    }
}

