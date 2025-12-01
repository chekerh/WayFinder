package tn.esprit.wayfinder.ui.screens

import android.app.Application
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import tn.esprit.wayfinder.models.FlightDestination
import tn.esprit.wayfinder.navigation.SELECTED_DESTINATION_KEY
import tn.esprit.wayfinder.presentation.auth.ViewModelFactory
import tn.esprit.wayfinder.ui.components.CustomBottomNavigationBar
import tn.esprit.wayfinder.utils.StringTranslator
import tn.esprit.wayfinder.viewmodels.CatalogViewModel
import tn.esprit.wayfinder.viewmodels.CatalogUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlightComparisonScreen(
    navController: NavController,
    destinationId: String
) {
    val context = LocalContext.current
    val catalogViewModel: CatalogViewModel = viewModel(factory = ViewModelFactory(context.applicationContext as Application))
    val uiState by catalogViewModel.uiState.collectAsState()
    
    val destination = remember(destinationId) {
        navController.previousBackStackEntry
            ?.savedStateHandle
            ?.get<FlightDestination>(SELECTED_DESTINATION_KEY)
    }
    
    // Load flights for the same destination
    LaunchedEffect(destination) {
        destination?.let { dest ->
            // Extract airport code from city name or use destination name
            val destinationCode = dest.city?.take(3)?.uppercase() ?: "XXX"
            catalogViewModel.loadRecommendedFlights(
                destinationLocationCode = destinationCode,
                maxResults = 20
            )
        }
    }
    
    // Calculate flights list with proper smart casting
    val flights = remember(uiState, destination) {
        when (val state = uiState) {
            is CatalogUiState.Success -> state.destinations.filter { 
                // Filter flights to the same destination city
                it.city?.lowercase() == destination?.city?.lowercase() ||
                it.name.lowercase().contains(destination?.name?.lowercase() ?: "")
            }
            else -> emptyList()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        StringTranslator.translate(context, "Comparer les prix"),
                        fontWeight = FontWeight.Bold
                    ) 
                },
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
        bottomBar = {
            CustomBottomNavigationBar(navController = navController)
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        val currentState = uiState // Store in local variable for smart casting
        when (currentState) {
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
            is CatalogUiState.Error -> {
                val errorMessage = currentState.message // Extract message for smart cast
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { 
                        destination?.let {
                            val destinationCode = it.city?.take(3)?.uppercase() ?: "XXX"
                            catalogViewModel.loadRecommendedFlights(
                                destinationLocationCode = destinationCode,
                                maxResults = 20
                            )
                        }
                    }) {
                        Text(StringTranslator.translate(context, "Réessayer"))
                    }
                }
            }
            is CatalogUiState.Success -> {
                if (flights.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = StringTranslator.translate(context, "Aucun vol trouvé pour cette destination"),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            Text(
                                text = StringTranslator.translate(context, "Vols disponibles pour ${destination?.name ?: destinationId}"),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }
                        items(flights, key = { it.id }) { flight ->
                            FlightComparisonCard(
                                flight = flight,
                                onClick = {
                                    navController.currentBackStackEntry
                                        ?.savedStateHandle
                                        ?.set(SELECTED_DESTINATION_KEY, flight)
                                    navController.navigate("flight_detail/${flight.id}")
                                }
                            )
                        }
                    }
                }
            }
            else -> {}
        }
    }
}

@Composable
fun FlightComparisonCard(
    flight: FlightDestination,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = flight.airline ?: "Compagnie inconnue",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "${flight.city ?: ""} • ${flight.country}",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = "${flight.price?.toInt() ?: 0} ${flight.currency}",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                flight.departureDate?.let {
                    Column {
                        Text(
                            text = "Départ",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = it.substringBefore("T"),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                Icon(
                    imageVector = Icons.Filled.Flight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                flight.arrivalDate?.let {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Arrivée",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = it.substringBefore("T"),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

