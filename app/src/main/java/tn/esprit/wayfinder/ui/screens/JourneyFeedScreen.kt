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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Divider
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import tn.esprit.wayfinder.presentation.auth.ViewModelFactory
import tn.esprit.wayfinder.viewmodels.JourneyViewModel
import tn.esprit.wayfinder.viewmodels.JourneyUiState
import tn.esprit.wayfinder.viewmodels.DestinationVideoViewModel
import tn.esprit.wayfinder.viewmodels.DestinationVideoUiState
import tn.esprit.wayfinder.viewmodels.VideoGenerationState
import tn.esprit.wayfinder.models.Journey
import tn.esprit.wayfinder.models.DestinationWithVideoStatus
import tn.esprit.wayfinder.manager.TokenManager
import tn.esprit.wayfinder.presentation.destinationvideo.DestinationVideoRepository
import tn.esprit.wayfinder.network.RetrofitInstance
import android.widget.Toast
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JourneyFeedScreen(navController: NavController) {
    val context = LocalContext.current
    val journeyViewModel: JourneyViewModel = viewModel(factory = ViewModelFactory(context.applicationContext as Application))
    val uiState by journeyViewModel.uiState.collectAsState()
    
    // Get current user ID to check if journey belongs to current user
    val tokenManager = remember { TokenManager(context) }
    val currentUser = remember { tokenManager.getUser() }
    val currentUserId = currentUser?.id
    
    // Destination video ViewModel
    val destinationVideoRepository = remember { 
        DestinationVideoRepository(RetrofitInstance.create(context))
    }
    val destinationVideoViewModel = remember { 
        DestinationVideoViewModel(destinationVideoRepository)
    }
    val destinationVideoState by destinationVideoViewModel.uiState.collectAsState()
    val generationState by destinationVideoViewModel.generationState.collectAsState()
    
    LaunchedEffect(Unit) {
        journeyViewModel.loadJourneys()
        currentUserId?.let {
            destinationVideoViewModel.loadUserDestinations(it)
        }
    }
    
    // Poll video status for processing videos
    LaunchedEffect(destinationVideoState) {
        val currentDestinationState = destinationVideoState
        if (currentDestinationState is DestinationVideoUiState.Success) {
            val destinations = currentDestinationState.destinations
            destinations.forEach { dest ->
                if (dest.videoStatus == "processing" && currentUserId != null) {
                    // Poll every 5 seconds for processing videos
                    while (true) {
                        delay(5000)
                        destinationVideoViewModel.checkVideoStatus(currentUserId, dest.destination)
                        val updatedState = destinationVideoViewModel.uiState.value
                        if (updatedState is DestinationVideoUiState.Success) {
                            val updatedDest = updatedState.destinations.find { it.destination == dest.destination }
                            if (updatedDest?.videoStatus != "processing") {
                                break // Stop polling if status changed
                            }
                        }
                    }
                }
            }
        }
    }
    
    // Show toast for generation state changes
    LaunchedEffect(generationState) {
        val currentState = generationState
        when (currentState) {
            is VideoGenerationState.Success -> {
                Toast.makeText(context, currentState.message, Toast.LENGTH_SHORT).show()
            }
            is VideoGenerationState.Error -> {
                Toast.makeText(context, "Erreur: ${currentState.message}", Toast.LENGTH_LONG).show()
            }
            else -> {}
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Voyages partagés") },
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
            is JourneyUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            is JourneyUiState.Success -> {
                if (state.journeys.isEmpty()) {
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
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = Color.Gray
                            )
                            Text(
                                text = "Aucun voyage partagé",
                                style = MaterialTheme.typography.titleLarge,
                                color = Color.Gray
                            )
                            Text(
                                text = "Soyez le premier à partager votre voyage !",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Gray
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Destination Videos Section (only for current user)
                        val currentDestinationState = destinationVideoState
                        if (currentUserId != null && currentDestinationState is DestinationVideoUiState.Success) {
                            val destinations = currentDestinationState.destinations
                            if (destinations.isNotEmpty()) {
                                item {
                                    Text(
                                        text = "Vidéos par destination",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(vertical = 8.dp)
                                    )
                                }
                                items(destinations) { destination ->
                                    DestinationVideoCard(
                                        destination = destination,
                                        onGenerateClick = {
                                            currentUserId?.let {
                                                destinationVideoViewModel.generateVideo(it, destination.destination)
                                            }
                                        },
                                        onVideoClick = {
                                            destination.videoUrl?.let { url ->
                                                // Navigate to video player or open video
                                                // For now, just show a toast
                                                Toast.makeText(context, "Lecture de la vidéo: $url", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    )
                                }
                                item {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Divider()
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Tous les voyages",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(vertical = 8.dp)
                                    )
                                }
                            }
                        }
                        
                        items(state.journeys) { journey ->
                            JourneyCard(
                                journey = journey,
                                currentUserId = currentUserId,
                                onLikeClick = {
                                    journeyViewModel.likeJourney(journey.id)
                                },
                                onCommentClick = {
                                    navController.navigate("journey_detail/${journey.id}")
                                },
                                onImageClick = { imageUrl ->
                                    navController.navigate("journey_detail/${journey.id}")
                                },
                                onGenerateVideoClick = {
                                    journeyViewModel.regenerateVideo(journey.id)
                                },
                                onDeleteClick = {
                                    journeyViewModel.deleteJourney(journey.id)
                                }
                            )
                        }
                    }
                }
            }
            is JourneyUiState.Error -> {
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
                        Button(onClick = { journeyViewModel.loadJourneys() }) {
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
fun JourneyCard(
    journey: Journey,
    currentUserId: String?,
    onLikeClick: () -> Unit,
    onCommentClick: () -> Unit,
    onImageClick: (String) -> Unit,
    onGenerateVideoClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val isOwnJourney = currentUserId != null && journey.userId == currentUserId
    var showDeleteDialog by remember { mutableStateOf(false) }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
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
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    AsyncImage(
                        model = journey.user?.profileImageUrl?.let { url ->
                            if (url.startsWith("http")) url else "https://wayfinder-api-w92x.onrender.com$url"
                        } ?: "https://i.pravatar.cc/150?img=${journey.userId.hashCode() % 70}",
                        contentDescription = "Profile",
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                    Column {
                        Text(
                            text = "${journey.user?.firstName?.takeIf { it.isNotBlank() } ?: journey.user?.username?.takeIf { it.isNotBlank() } ?: "User"} ${journey.user?.lastName?.takeIf { it.isNotBlank() } ?: ""}",
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
                
                // Delete button (only for own journeys)
                if (isOwnJourney) {
                    IconButton(
                        onClick = { showDeleteDialog = true }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Supprimer",
                            tint = Color(0xFFE91E63),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
            
            // Delete confirmation dialog
            if (showDeleteDialog) {
                AlertDialog(
                    onDismissRequest = { showDeleteDialog = false },
                    title = {
                        Text("Supprimer le voyage")
                    },
                    text = {
                        Text("Êtes-vous sûr de vouloir supprimer ce voyage ? Cette action est irréversible.")
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                showDeleteDialog = false
                                onDeleteClick()
                            },
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = Color(0xFFE91E63)
                            )
                        ) {
                            Text("Supprimer")
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = { showDeleteDialog = false }
                        ) {
                            Text("Annuler")
                        }
                    }
                )
            }
            
            // Description
            if (!journey.description.isNullOrEmpty()) {
                Text(
                    text = journey.description,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            
            // Images Grid
            if (journey.imageUrls.isNotEmpty()) {
                val firstImage = journey.imageUrls.first()
                val baseUrl = "https://wayfinder-api-w92x.onrender.com"
                val imageUrl = if (firstImage.startsWith("http")) firstImage else "$baseUrl$firstImage"
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onImageClick(imageUrl) }
                ) {
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = "Journey Image",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    
                    // Image count badge
                    if (journey.imageUrls.size > 1) {
                        Surface(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp),
                            shape = RoundedCornerShape(8.dp),
                            color = Color.Black.copy(alpha = 0.6f)
                        ) {
                            Text(
                                text = "+${journey.imageUrls.size - 1}",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                color = Color.White,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
            
            // Video Status / Generate Video Button (only for own journeys)
            if (isOwnJourney) {
                if (journey.videoStatus == "completed" && !journey.videoUrl.isNullOrEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF4A90E2).copy(alpha = 0.1f))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Video",
                            tint = Color(0xFF4A90E2),
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "Vidéo AI générée",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF4A90E2)
                        )
                    }
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
                } else if (journey.videoStatus == "pending" || journey.videoStatus == "failed") {
                    // Show "Generate Video" button for own journeys when video is not yet generated or failed
                    Button(
                        onClick = onGenerateVideoClick,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4A90E2)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.VideoLibrary,
                            contentDescription = "Generate Video",
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (journey.videoStatus == "failed") "Régénérer la vidéo" else "Générer ma vidéo",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            } else if (journey.videoStatus == "completed" && !journey.videoUrl.isNullOrEmpty()) {
                // Show video status for other users' journeys
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF4A90E2).copy(alpha = 0.1f))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Video",
                        tint = Color(0xFF4A90E2),
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "Vidéo AI générée",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF4A90E2)
                    )
                }
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
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.clickable { onCommentClick() }
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

