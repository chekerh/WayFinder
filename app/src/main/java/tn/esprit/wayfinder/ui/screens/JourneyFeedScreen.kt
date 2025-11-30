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
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Divider
import androidx.compose.material3.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import tn.esprit.wayfinder.ui.theme.WayFinderTheme
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
import tn.esprit.wayfinder.utils.StringTranslator
import android.widget.Toast
import android.widget.VideoView
import android.widget.MediaController
import android.net.Uri
import android.content.Intent
import android.os.Handler
import android.os.Looper
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JourneyFeedScreen(navController: NavController) {
    val context = LocalContext.current
    var showVideoDialog by remember { mutableStateOf<String?>(null) }
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
    
    // Load journeys only once on initial composition
    LaunchedEffect(Unit) {
        journeyViewModel.loadJourneys(forceRefresh = true)
        currentUserId?.let {
            destinationVideoViewModel.loadUserDestinations(it)
        }
    }
    
    // Poll for journey video status updates (for processing videos)
    // Access state through ViewModel to avoid smart cast issues with delegated property
    LaunchedEffect(Unit) {
        while (true) {
            delay(10000) // Poll every 10 seconds
            when (val currentState = journeyViewModel.uiState.value) {
                is JourneyUiState.Success -> {
                    val journeysToPoll = currentState.journeys.filter { 
                        it.videoStatus == "processing" || it.videoStatus == "pending" 
                    }
                    
                    // Refresh journeys to get updated status
                    journeyViewModel.loadJourneys(forceRefresh = true)
                }
                else -> {
                    // No action needed for other states
                }
            }
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
                title = { Text(StringTranslator.translate(context, "Voyages partagés")) },
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
        containerColor = MaterialTheme.colorScheme.background
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
                                text = StringTranslator.translate(context, "Aucun voyage partagé"),
                                style = MaterialTheme.typography.titleLarge,
                                color = Color.Gray
                            )
                            Text(
                                text = StringTranslator.translate(context, "Soyez le premier à partager votre voyage !"),
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
                                        text = StringTranslator.translate(context, "Vidéos par destination"),
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
                                                showVideoDialog = url
                                            }
                                        }
                                    )
                                }
                                item {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    HorizontalDivider()
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = StringTranslator.translate(context, "Tous les voyages"),
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
                                onVideoClick = { videoUrl ->
                                    showVideoDialog = videoUrl
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
                            Text(StringTranslator.translate(context, "Réessayer"))
                        }
                    }
                }
            }
            else -> {}
        }
        
        // Video dialog
        showVideoDialog?.let { videoUrl ->
            val dialogContext = LocalContext.current
            var isLoading by remember { mutableStateOf(true) }
            var hasError by remember { mutableStateOf(false) }
            var errorMessage by remember { mutableStateOf<String?>(null) }
            
            AlertDialog(
                onDismissRequest = { showVideoDialog = null },
                title = {
                    Text(
                        text = StringTranslator.translate(dialogContext, "Vidéo AI générée"),
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(300.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.Black),
                            contentAlignment = Alignment.Center
                        ) {
                            if (hasError) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.padding(16.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Error,
                                        contentDescription = "Error",
                                        tint = Color.Red,
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Text(
                                        text = errorMessage ?: StringTranslator.translate(dialogContext, "Erreur de chargement"),
                                        color = Color.White,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        text = videoUrl,
                                        color = Color.Gray,
                                        style = MaterialTheme.typography.bodySmall,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Button(
                                        onClick = {
                                            // Open video in external browser/player
                                            try {
                                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(videoUrl))
                                                intent.setDataAndType(Uri.parse(videoUrl), "video/*")
                                                dialogContext.startActivity(intent)
                                            } catch (e: Exception) {
                                                Toast.makeText(dialogContext, "Impossible d'ouvrir la vidéo", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFF4A90E2)
                                        )
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.OpenInBrowser,
                                            contentDescription = "Open in browser",
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(StringTranslator.translate(dialogContext, "Ouvrir dans le navigateur"))
                                    }
                                }
                            } else {
                                // Show loading indicator while video is loading
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isLoading) {
                                        CircularProgressIndicator(
                                            color = Color.White,
                                            modifier = Modifier.size(48.dp)
                                        )
                                    }
                                    
                                    AndroidView(
                                        modifier = Modifier.fillMaxSize(),
                                        factory = { ctx ->
                                            VideoView(ctx).apply {
                                                val controller = MediaController(ctx)
                                                controller.setAnchorView(this)
                                                setMediaController(controller)
                                                
                                                // Add error listener with better error handling
                                                setOnErrorListener { _, what, extra ->
                                                    android.util.Log.e("VideoPlayer", "Video error: what=$what, extra=$extra, url=$videoUrl")
                                                    hasError = true
                                                    isLoading = false
                                                    errorMessage = when (what) {
                                                        android.media.MediaPlayer.MEDIA_ERROR_UNKNOWN -> {
                                                            // More specific error message
                                                            if (videoUrl.contains("sample") || videoUrl.contains("placeholder") || videoUrl.contains("gtv-videos")) {
                                                                StringTranslator.translate(dialogContext, "Vidéo de test non disponible. Configurez un service de génération vidéo.")
                                                            } else {
                                                                StringTranslator.translate(dialogContext, "Impossible de charger la vidéo. Vérifiez votre connexion Internet.")
                                                            }
                                                        }
                                                        android.media.MediaPlayer.MEDIA_ERROR_SERVER_DIED -> StringTranslator.translate(dialogContext, "Serveur vidéo indisponible")
                                                        android.media.MediaPlayer.MEDIA_ERROR_IO -> StringTranslator.translate(dialogContext, "Erreur de connexion réseau")
                                                        android.media.MediaPlayer.MEDIA_ERROR_MALFORMED -> StringTranslator.translate(dialogContext, "Format vidéo non supporté")
                                                        else -> "${StringTranslator.translate(dialogContext, "Erreur de lecture")} (code: $what)"
                                                    }
                                                    true
                                                }
                                                
                                                // Add info listener for better state management
                                                setOnInfoListener { _, what, extra ->
                                                    android.util.Log.d("VideoPlayer", "Video info: what=$what, extra=$extra")
                                                    when (what) {
                                                        android.media.MediaPlayer.MEDIA_INFO_BUFFERING_START -> {
                                                            isLoading = true
                                                        }
                                                        android.media.MediaPlayer.MEDIA_INFO_BUFFERING_END -> {
                                                            isLoading = false
                                                        }
                                                        android.media.MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START -> {
                                                            isLoading = false
                                                            // Video is now rendering, hide loading indicator
                                                        }
                                                        android.media.MediaPlayer.MEDIA_INFO_VIDEO_TRACK_LAGGING -> {
                                                            android.util.Log.w("VideoPlayer", "Video track lagging")
                                                        }
                                                    }
                                                    false
                                                }
                                                
                                                // Parse and set video URI with timeout
                                                try {
                                                    val uri = Uri.parse(videoUrl)
                                                    android.util.Log.d("VideoPlayer", "Loading video from: $videoUrl")
                                                    isLoading = true
                                                    setVideoURI(uri)
                                                    
                                                    setOnPreparedListener { player ->
                                                        android.util.Log.d("VideoPlayer", "Video prepared, starting playback")
                                                        isLoading = false
                                                        player.isLooping = false
                                                        // Wait a bit before starting to ensure surface is ready
                                                        Handler(Looper.getMainLooper()).postDelayed({
                                                            try {
                                                                start()
                                                            } catch (e: Exception) {
                                                                android.util.Log.e("VideoPlayer", "Error starting video: ${e.message}", e)
                                                                hasError = true
                                                                errorMessage = StringTranslator.translate(dialogContext, "Erreur de lecture")
                                                            }
                                                        }, 100)
                                                    }
                                                    
                                                    // Timeout: if video doesn't load in 10 seconds, show error
                                                    Handler(Looper.getMainLooper()).postDelayed({
                                                        if (isLoading) {
                                                            android.util.Log.w("VideoPlayer", "Video loading timeout")
                                                            hasError = true
                                                            isLoading = false
                                                            errorMessage = StringTranslator.translate(dialogContext, "Le chargement de la vidéo prend trop de temps")
                                                        }
                                                    }, 10000)
                                                } catch (e: Exception) {
                                                    android.util.Log.e("VideoPlayer", "Error parsing video URL: ${e.message}", e)
                                                    hasError = true
                                                    isLoading = false
                                                    errorMessage = StringTranslator.translate(dialogContext, "URL vidéo invalide")
                                                }
                                                
                                                tag = videoUrl
                                            }
                                        },
                                        update = { videoView ->
                                            if (videoView.tag != videoUrl) {
                                                videoView.tag = videoUrl
                                                isLoading = true
                                                try {
                                                    val uri = Uri.parse(videoUrl)
                                                    videoView.setVideoURI(uri)
                                                    videoView.start()
                                                } catch (e: Exception) {
                                                    android.util.Log.e("VideoPlayer", "Error updating video: ${e.message}", e)
                                                    hasError = true
                                                    isLoading = false
                                                }
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showVideoDialog = null }) {
                        Text(StringTranslator.translate(dialogContext, "Fermer"))
                    }
                }
            )
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
    onVideoClick: (String) -> Unit,
    onGenerateVideoClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val context = LocalContext.current
    val isOwnJourney = currentUserId != null && journey.userId == currentUserId
    var showDeleteDialog by remember { mutableStateOf(false) }
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
                            contentDescription = StringTranslator.translate(context, "Supprimer"),
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
                        Text(StringTranslator.translate(context, "Supprimer le voyage"))
                    },
                    text = {
                        Text(StringTranslator.translate(context, "Êtes-vous sûr de vouloir supprimer ce voyage ? Cette action est irréversible."))
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
                            Text(StringTranslator.translate(context, "Supprimer"))
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = { showDeleteDialog = false }
                        ) {
                            Text(StringTranslator.translate(context, "Annuler"))
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
            
            // Images Grid - Use slides if available, otherwise use imageUrls
            val imagesList = if (journey.slides.isNotEmpty()) {
                journey.slides.map { it.imageUrl }
            } else {
                journey.imageUrls
            }
            
            if (imagesList.isNotEmpty()) {
                val firstImage = imagesList.first()
                val baseUrl = "https://wayfinder-api-w92x.onrender.com"
                val imageUrl = if (firstImage.startsWith("http")) firstImage else "$baseUrl${if (firstImage.startsWith("/")) firstImage else "/$firstImage"}"
                
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
                    if (imagesList.size > 1) {
                        Surface(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp),
                            shape = RoundedCornerShape(8.dp),
                            color = Color.Black.copy(alpha = 0.6f)
                        ) {
                            Text(
                                text = "+${imagesList.size - 1}",
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
                // Only show video when it's fully ready (completed status AND valid URL)
                // Hide completely during processing to avoid black screens
                if (journey.videoStatus == "completed" && !journey.videoUrl.isNullOrEmpty() && journey.videoUrl.isNotBlank()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF4A90E2).copy(alpha = 0.1f))
                            .clickable {
                                journey.videoUrl?.let { url ->
                                    onVideoClick(url)
                                }
                            }
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
                            text = StringTranslator.translate(context, "Vidéo AI générée - Appuyez pour lire"),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF4A90E2)
                        )
                    }
                }
                // Show processing indicator for better UX
                else if (journey.videoStatus == "processing") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFFF9800).copy(alpha = 0.1f))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color(0xFFFF9800),
                            strokeWidth = 2.dp
                        )
                        Text(
                            text = StringTranslator.translate(context, "Génération de la vidéo en cours..."),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFFF9800)
                        )
                    }
                }
                // Show generate button for pending or failed
                else if (journey.videoStatus == "pending" || journey.videoStatus == "failed") {
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
                            text = if (journey.videoStatus == "failed") StringTranslator.translate(context, "Régénérer la vidéo") else StringTranslator.translate(context, "Générer ma vidéo"),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            } else if (journey.videoStatus == "completed" && !journey.videoUrl.isNullOrEmpty() && journey.videoUrl.isNotBlank()) {
                // Show video status for other users' journeys - only when video is fully ready
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF4A90E2).copy(alpha = 0.1f))
                        .clickable {
                            journey.videoUrl?.let { url ->
                                onVideoClick(url)
                            }
                        }
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
                        text = StringTranslator.translate(context, "Vidéo AI générée - Appuyez pour lire"),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF4A90E2)
                    )
                }
            }
            // Do not show anything for processing, pending, or failed states
            // The video will appear automatically when ready
            
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
                        imageVector = Icons.AutoMirrored.Filled.Comment,
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

@Preview(showBackground = true)
@Composable
fun JourneyFeedScreenPreview() {
    WayFinderTheme {
        JourneyFeedScreen(rememberNavController())
    }
}
