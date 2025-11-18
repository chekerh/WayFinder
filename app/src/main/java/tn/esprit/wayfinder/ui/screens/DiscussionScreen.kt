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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
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
import tn.esprit.wayfinder.models.DiscussionPost
import tn.esprit.wayfinder.presentation.auth.ViewModelFactory
import tn.esprit.wayfinder.ui.components.CustomBottomNavigationBar
import tn.esprit.wayfinder.viewmodels.DiscussionUiState
import tn.esprit.wayfinder.viewmodels.DiscussionViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscussionScreen(navController: NavController) {
    val context = LocalContext.current
    val discussionViewModel: DiscussionViewModel = viewModel(factory = ViewModelFactory(context.applicationContext as Application))
    val uiState by discussionViewModel.uiState.collectAsState()
    val tokenManager = remember { TokenManager(context) }
    val currentUser = remember { tokenManager.getUser() }
    
    var showCreatePostDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        discussionViewModel.loadPosts(refresh = true)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Discussions", fontWeight = FontWeight.Bold) },
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
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreatePostDialog = true },
                containerColor = Color(0xFF1976D2)
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Create Post", tint = Color.White)
            }
        },
        bottomBar = {
            CustomBottomNavigationBar(navController = navController)
        },
        containerColor = Color(0xFFEAF2FF)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val state = uiState) {
                is DiscussionUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is DiscussionUiState.Success -> {
                    if (state.posts.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "Aucune discussion pour le moment",
                                    color = Color.Gray,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(onClick = { showCreatePostDialog = true }) {
                                    Text("Créer la première discussion")
                                }
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(state.posts) { post ->
                                PostCard(
                                    post = post,
                                    currentUserId = currentUser?.id,
                                    onLikeClick = { discussionViewModel.likePost(post.id) },
                                    onClick = {
                                        navController.navigate("post_detail/${post.id}")
                                    }
                                )
                            }
                            if (state.hasMore) {
                                item {
                                    Button(
                                        onClick = { discussionViewModel.loadPosts() },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("Charger plus")
                                    }
                                }
                            }
                        }
                    }
                }
                is DiscussionUiState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(state.message, color = Color.Red)
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(onClick = { discussionViewModel.retry() }) {
                                Text("Réessayer")
                            }
                        }
                    }
                }
                else -> {}
            }
        }
    }

    if (showCreatePostDialog) {
        CreatePostDialog(
            onDismiss = { showCreatePostDialog = false },
            onCreatePost = { title, content, tags, destination ->
                discussionViewModel.createPost(title, content, tags, destination)
                showCreatePostDialog = false
            }
        )
    }
}

@Composable
fun PostCard(
    post: DiscussionPost,
    currentUserId: String?,
    onLikeClick: () -> Unit,
    onClick: () -> Unit
) {
    val isLiked = currentUserId != null && post.likedBy.contains(currentUserId)
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
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
                            .size(40.dp)
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
                            .size(40.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "${post.userId.firstName} ${post.userId.lastName}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = formatDate(post.createdAt),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Post Title
            Text(
                text = post.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Post Content
            Text(
                text = post.content,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )
            
            // Post Image
            post.imageUrl?.let { imageUrl ->
                Spacer(modifier = Modifier.height(12.dp))
                AsyncImage(
                    model = imageUrl,
                    contentDescription = "Post Image",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop,
                    placeholder = painterResource(id = R.drawable.travel_image),
                    error = painterResource(id = R.drawable.travel_image)
                )
            }
            
            // Tags
            if (post.tags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    post.tags.take(3).forEach { tag ->
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
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF1976D2)
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
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
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Text(
                    text = "${post.commentsCount} commentaires",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
        }
    }
}

@Composable
fun CreatePostDialog(
    onDismiss: () -> Unit,
    onCreatePost: (String, String, List<String>?, String?) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var destination by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Créer une discussion", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Titre") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("Contenu") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 4,
                    maxLines = 8
                )
                OutlinedTextField(
                    value = destination,
                    onValueChange = { destination = it },
                    label = { Text("Destination (optionnel)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank() && content.isNotBlank()) {
                        onCreatePost(
                            title,
                            content,
                            null,
                            destination.ifBlank { null }
                        )
                    }
                },
                enabled = title.isNotBlank() && content.isNotBlank()
            ) {
                Text("Publier")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler")
            }
        }
    )
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

