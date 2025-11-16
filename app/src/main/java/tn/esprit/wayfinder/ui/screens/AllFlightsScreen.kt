package tn.esprit.wayfinder.ui.screens

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import tn.esprit.wayfinder.R
import tn.esprit.wayfinder.presentation.auth.ViewModelFactory
import tn.esprit.wayfinder.viewmodels.CatalogViewModel
import tn.esprit.wayfinder.viewmodels.CatalogUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllFlightsScreen(navController: NavController, selectedRegion: String? = null) {
    val context = LocalContext.current
    val catalogViewModel: CatalogViewModel = viewModel(factory = ViewModelFactory(context.applicationContext as Application))
    val uiState by catalogViewModel.uiState.collectAsState()

    // Region to country mapping
    val regionCountries = mapOf(
        "Europe" to listOf("France", "United Kingdom", "Italy", "Spain", "Netherlands", "Germany", "Switzerland", "Belgium", "Portugal", "Greece", "Austria", "Sweden", "Norway", "Denmark", "Finland", "Poland", "Czech Republic", "Hungary", "Ireland"),
        "Asie" to listOf("China", "Japan", "India", "Thailand", "Singapore", "Malaysia", "Indonesia", "South Korea", "Vietnam", "Philippines", "UAE", "Saudi Arabia", "Turkey", "Israel"),
        "Amerique" to listOf("United States", "Canada", "Mexico", "Brazil", "Argentina", "Chile", "Colombia", "Peru"),
        "Australie" to listOf("Australia", "New Zealand", "Fiji")
    )

    // Load all flights on first composition
    LaunchedEffect(Unit) {
        catalogViewModel.loadRecommendedFlights()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tous les vols", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
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
            is CatalogUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            is CatalogUiState.Success -> {
                // Filter destinations based on selected region
                val filteredDestinations = if (selectedRegion != null && selectedRegion != "Préférences") {
                    val filterCountries = regionCountries[selectedRegion] ?: emptyList()
                    if (filterCountries.isNotEmpty()) {
                        state.destinations.filter { destination ->
                            filterCountries.any { country ->
                                destination.country.contains(country, ignoreCase = true)
                            }
                        }
                    } else {
                        state.destinations
                    }
                } else {
                    state.destinations
                }
                
                if (filteredDestinations.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Aucun vol disponible",
                                color = Color.Gray,
                                style = MaterialTheme.typography.bodyLarge
                            )
                            if (selectedRegion != null) {
                                Text(
                                    text = "pour la région sélectionnée",
                                    color = Color.Gray,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(vertical = 16.dp)
                    ) {
                        items(filteredDestinations) { destination ->
                            FlightCard(
                                destination = destination,
                                onClick = {
                                    navController.navigate("flight_detail/${destination.id}")
                                }
                            )
                        }
                    }
                }
            }
            is CatalogUiState.Error -> {
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
                            color = Color.Red,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Button(onClick = { catalogViewModel.loadRecommendedFlights() }) {
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
fun FlightCard(
    destination: tn.esprit.wayfinder.models.FlightDestination,
    onClick: () -> Unit
) {
    var isFavorite by remember { mutableStateOf(false) }
    
    // Better image URL - use city name for more relevant images
    val imageUrl = destination.imageUrl ?: "https://images.unsplash.com/photo-1488646953014-85cb44e25828?w=800&h=600&fit=crop&q=80"
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // City image
            AsyncImage(
                model = imageUrl,
                contentDescription = destination.name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                placeholder = painterResource(id = R.drawable.travel_image),
                error = painterResource(id = R.drawable.travel_image)
            )
            
            // Gradient overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f)),
                            startY = 100f
                        )
                    )
            )
            
            // Content
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp)
            ) {
                Text(
                    text = destination.name,
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = destination.country,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.9f),
                    modifier = Modifier.padding(top = 4.dp)
                )
                if (destination.price != null && destination.price > 0) {
                    Text(
                        text = "${destination.price.toInt()} ${destination.currency}",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
            
            // Favorite button
            IconButton(
                onClick = { isFavorite = !isFavorite },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
            ) {
                Icon(
                    imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    contentDescription = if (isFavorite) "Remove from favorites" else "Add to favorites",
                    tint = if (isFavorite) Color(0xFFFF1744) else Color.White,
                    modifier = Modifier
                        .size(24.dp)
                        .background(
                            Color.Black.copy(alpha = 0.3f),
                            RoundedCornerShape(12.dp)
                        )
                        .padding(4.dp)
                )
            }
        }
    }
}

