package tn.esprit.wayfinder.ui.screens

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import tn.esprit.wayfinder.R
import tn.esprit.wayfinder.models.Activity
import tn.esprit.wayfinder.models.FlightDestination
import tn.esprit.wayfinder.navigation.SELECTED_DESTINATION_KEY
import tn.esprit.wayfinder.presentation.auth.ViewModelFactory
import tn.esprit.wayfinder.ui.theme.WayFinderTheme
import tn.esprit.wayfinder.utils.StringTranslator
import tn.esprit.wayfinder.viewmodels.ActivitiesViewModel
import tn.esprit.wayfinder.viewmodels.ActivitiesUiState

/**
 * Preview screen showing activities available at the destination
 * User can choose to "Confirm Booking" or "Explore More"
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivitiesPreviewScreen(
    navController: NavController,
    destinationId: String
) {
    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme
    
    val activitiesViewModel: ActivitiesViewModel = viewModel(
        factory = ViewModelFactory(context.applicationContext as Application)
    )
    val activitiesState by activitiesViewModel.uiState.collectAsStateWithLifecycle()
    
    // Get destination from navigation
    val destination = remember(destinationId) {
        navController.previousBackStackEntry
            ?.savedStateHandle
            ?.get<FlightDestination>(SELECTED_DESTINATION_KEY)
    }
    
    val tripType = remember {
        navController.previousBackStackEntry
            ?.savedStateHandle
            ?.get<String>("trip_type")
    }
    
    // Load activities for the destination
    LaunchedEffect(destination) {
        val city = destination?.city ?: destination?.name ?: "Paris"
        activitiesViewModel.loadActivities(city = city, limit = 12)
    }
    
    // Category filter
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    val categories = listOf(
        "Tous" to null,
        "Culture" to "museums",
        "Gastronomie" to "restaurants",
        "Nature" to "parks",
        "Divertissement" to "entertainment",
        "Shopping" to "shops"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = StringTranslator.translate(context, "Ce qui vous attend"),
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        destination?.let {
                            Text(
                                text = "${it.name}, ${it.country}",
                                fontSize = 14.sp,
                                color = colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colorScheme.surface
                )
            )
        },
        bottomBar = {
            // Action buttons at bottom
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shadowElevation = 8.dp,
                color = colorScheme.surface
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Primary action - Confirm booking
                    Button(
                        onClick = {
                            // Navigate to payment summary
                            navController.navigate("payment_summary/${destinationId}")
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colorScheme.primary
                        )
                    ) {
                        Icon(
                            Icons.Filled.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = StringTranslator.translate(context, "Confirmer la réservation"),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    // Secondary action - Skip activities
                    TextButton(
                        onClick = {
                            // Skip directly to payment
                            navController.navigate("payment_summary/${destinationId}")
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = StringTranslator.translate(context, "Passer et réserver maintenant"),
                            color = colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        containerColor = colorScheme.background
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            // Header info card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = colorScheme.primaryContainer.copy(alpha = 0.3f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            Icons.Filled.Explore,
                            contentDescription = null,
                            tint = colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = StringTranslator.translate(context, "Découvrez votre destination"),
                                fontWeight = FontWeight.Bold,
                                color = colorScheme.onSurface
                            )
                            Text(
                                text = StringTranslator.translate(context, "Ces activités seront disponibles dans le mode Arrive"),
                                fontSize = 12.sp,
                                color = colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            
            // Category filter chips
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    categories.forEach { (label, category) ->
                        FilterChip(
                            selected = selectedCategory == category,
                            onClick = { selectedCategory = category },
                            label = { Text(label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = colorScheme.primary,
                                selectedLabelColor = colorScheme.onPrimary
                            )
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            // Activities content
            when (val state = activitiesState) {
                is ActivitiesUiState.Loading -> {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                }
                
                is ActivitiesUiState.Success -> {
                    // Filter activities by category if selected
                    val filteredActivities = if (selectedCategory == null) {
                        state.activities
                    } else {
                        state.activities.filter { activity ->
                            activity.category?.lowercase()?.contains(selectedCategory!!.lowercase()) == true ||
                            activity.tags?.any { it.lowercase().contains(selectedCategory!!.lowercase()) } == true
                        }
                    }
                    
                    items(filteredActivities.chunked(2)) { rowActivities ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            rowActivities.forEach { activity ->
                                ActivityPreviewCard(
                                    activity = activity,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            // Fill empty space if odd number
                            if (rowActivities.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                    
                    // Empty state for filtered results
                    if (filteredActivities.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        Icons.Filled.SearchOff,
                                        contentDescription = null,
                                        modifier = Modifier.size(48.dp),
                                        tint = colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = StringTranslator.translate(context, "Aucune activité dans cette catégorie"),
                                        color = colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
                
                is ActivitiesUiState.Error -> {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    Icons.Filled.Error,
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                    tint = colorScheme.error
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = state.message,
                                    color = colorScheme.error,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
                
                else -> {}
            }
            
            // Arrive Mode teaser
            item {
                Spacer(modifier = Modifier.height(16.dp))
                ArriveModeTeaser()
            }
        }
    }
}

@Composable
fun ActivityPreviewCard(
    activity: Activity,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    
    Card(
        modifier = modifier.height(180.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Background image
            AsyncImage(
                model = activity.imageUrl ?: "https://picsum.photos/200/200?random=${activity.id.hashCode()}",
                contentDescription = activity.name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                placeholder = painterResource(id = R.drawable.europe),
                error = painterResource(id = R.drawable.europe)
            )
            
            // Gradient overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.7f)
                            ),
                            startY = 50f
                        )
                    )
            )
            
            // Content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Category chip
                activity.category?.let { category ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = colorScheme.primaryContainer.copy(alpha = 0.9f)
                    ) {
                        Text(
                            text = category,
                            fontSize = 10.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            color = colorScheme.onPrimaryContainer
                        )
                    }
                }
                
                // Title and rating
                Column {
                    Text(
                        text = activity.name,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    activity.rating?.let { rating ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                Icons.Filled.Star,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = Color(0xFFFFD700)
                            )
                            Text(
                                text = String.format("%.1f", rating),
                                fontSize = 12.sp,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ArriveModeTeaser() {
    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = colorScheme.tertiaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    Icons.Filled.FlightLand,
                    contentDescription = null,
                    tint = colorScheme.onTertiaryContainer,
                    modifier = Modifier.size(32.dp)
                )
                Column {
                    Text(
                        text = StringTranslator.translate(context, "Mode Arrive"),
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = colorScheme.onTertiaryContainer
                    )
                    Text(
                        text = StringTranslator.translate(context, "Votre compagnon de voyage intelligent"),
                        fontSize = 12.sp,
                        color = colorScheme.onTertiaryContainer.copy(alpha = 0.8f)
                    )
                }
            }
            
            // Features preview
            val features = listOf(
                Icons.Filled.CalendarMonth to "Itinéraires AI personnalisés",
                Icons.Filled.LocationOn to "Recommandations à proximité",
                Icons.Filled.Restaurant to "Réservations de restaurants",
                Icons.Filled.CloudDownload to "Guides hors-ligne"
            )
            
            features.forEach { (icon, text) ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = colorScheme.onTertiaryContainer.copy(alpha = 0.7f),
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = StringTranslator.translate(context, text),
                        fontSize = 14.sp,
                        color = colorScheme.onTertiaryContainer
                    )
                }
            }
            
            Text(
                text = StringTranslator.translate(context, "Activé automatiquement à votre arrivée"),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = colorScheme.onTertiaryContainer.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ActivitiesPreviewScreenPreview() {
    WayFinderTheme {
        ActivitiesPreviewScreen(rememberNavController(), destinationId = "test")
    }
}

