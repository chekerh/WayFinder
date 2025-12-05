package tn.esprit.wayfinder.ui.screens

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Group
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import tn.esprit.wayfinder.models.FlightDestination
import tn.esprit.wayfinder.models.FlightOffer
import tn.esprit.wayfinder.navigation.SELECTED_DESTINATION_KEY
import tn.esprit.wayfinder.presentation.auth.ViewModelFactory
import tn.esprit.wayfinder.ui.theme.WayFinderTheme
import tn.esprit.wayfinder.utils.CommissionCalculator
import tn.esprit.wayfinder.utils.StringTranslator
import tn.esprit.wayfinder.viewmodels.CatalogViewModel
import tn.esprit.wayfinder.viewmodels.CatalogUiState
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AirlineSelectionScreen(
    navController: NavController,
    destinationId: String
) {
    val context = LocalContext.current
    val catalogViewModel: CatalogViewModel = viewModel(
        factory = ViewModelFactory(context.applicationContext as Application)
    )
    val uiState by catalogViewModel.uiState.collectAsState()
    
    val destination = remember(destinationId) {
        navController.previousBackStackEntry
            ?.savedStateHandle
            ?.get<FlightDestination>(SELECTED_DESTINATION_KEY)
    }
    
    // Fetch real flight offers when screen loads
    LaunchedEffect(destination) {
        destination?.let { dest ->
            // Extract IATA codes if available, or use defaults
            val originCode = "TUN" // Default from Tunis
            val destCode = when {
                dest.city?.uppercase()?.contains("PARIS") == true -> "CDG"
                dest.city?.uppercase()?.contains("ROME") == true -> "FCO"
                dest.city?.uppercase()?.contains("MADRID") == true -> "MAD"
                dest.city?.uppercase()?.contains("BARCELONE") == true -> "BCN"
                else -> dest.city?.take(3)?.uppercase() ?: "XXX"
            }
            
            catalogViewModel.loadRecommendedFlights(
                originLocationCode = originCode,
                destinationLocationCode = destCode,
                departureDate = dest.departureDate?.substringBefore("T") ?: null,
                maxResults = 20
            )
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        StringTranslator.translate(context, "Compagnies aériennes disponibles"),
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
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
                val flightOffers = state.flightOffers ?: emptyList()
                
                if (flightOffers.isEmpty()) {
                    EmptyAirlinesState(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(flightOffers) { offer ->
            AirlineCard(
                offer = offer,
                destination = destination,
                onSelect = {
                    // Navigate to booking review with selected airline
                    navController.currentBackStackEntry
                        ?.savedStateHandle
                        ?.apply {
                            set(SELECTED_DESTINATION_KEY, destination)
                            set("selected_flight_offer", offer)
                        }
                    navController.navigate("review_booking/${destinationId}")
                },
                onOrganize = {
                    // Navigate to organize group flight
                    navController.currentBackStackEntry
                        ?.savedStateHandle
                        ?.apply {
                            set(SELECTED_DESTINATION_KEY, destination)
                            set("selected_flight_offer", offer)
                        }
                    navController.navigate("organize_flight/${destinationId}")
                }
            )
                        }
                    }
                }
            }
            is CatalogUiState.Error -> {
                ErrorAirlinesState(
                    message = state.message,
                    onRetry = {
                        destination?.let { dest ->
                            catalogViewModel.loadRecommendedFlights(
                                originLocationCode = "TUN",
                                destinationLocationCode = dest.city?.take(3)?.uppercase() ?: "XXX",
                                departureDate = dest.departureDate?.substringBefore("T") ?: null,
                                maxResults = 20
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                )
            }
            else -> {}
        }
    }
}

@Composable
fun AirlineCard(
    offer: FlightOffer,
    destination: FlightDestination?,
    onSelect: () -> Unit,
    onOrganize: () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme
    
    // Extract airline info
    val airlineCode = offer.validatingAirlineCodes?.firstOrNull() ?: "N/A"
    val airlineName = getAirlineName(airlineCode)
    val price = offer.price?.total?.toDoubleOrNull() ?: 0.0
    val currency = offer.price?.currency ?: destination?.currency ?: "EUR"
    
    // Calculate commission
    val priceBreakdown = remember(price) {
        CommissionCalculator.calculateBreakdown(
            basePrice = price,
            bookingType = "flight"
        )
    }
    
    // Extract flight details
    val firstSegment = offer.itineraries?.firstOrNull()?.segments?.firstOrNull()
    val departureTime = firstSegment?.departure?.at
    val arrivalTime = firstSegment?.arrival?.at
    val duration = offer.itineraries?.firstOrNull()?.duration
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Airline header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Airline logo placeholder
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(
                                colorScheme.primary.copy(alpha = 0.1f),
                                RoundedCornerShape(8.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Flight,
                            contentDescription = null,
                            tint = colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Column {
                        Text(
                            text = airlineName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(
                            text = airlineCode,
                            fontSize = 12.sp,
                            color = colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                // Price with commission
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = String.format("%.2f %s", priceBreakdown.totalPrice, currency),
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = colorScheme.primary
                    )
                    Text(
                        text = StringTranslator.translate(context, "Commission incluse"),
                        fontSize = 10.sp,
                        color = colorScheme.onSurfaceVariant
                    )
                }
            }
            
            HorizontalDivider()
            
            // Flight details
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Departure
                Column {
                    Text(
                        text = StringTranslator.translate(context, "Départ"),
                        fontSize = 12.sp,
                        color = colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formatTime(departureTime),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp
                    )
                    Text(
                        text = firstSegment?.departure?.iataCode ?: "TUN",
                        fontSize = 12.sp,
                        color = colorScheme.onSurfaceVariant
                    )
                }
                
                // Duration
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Filled.Flight,
                        contentDescription = null,
                        tint = colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = duration ?: "N/A",
                        fontSize = 12.sp,
                        color = colorScheme.onSurfaceVariant
                    )
                }
                
                // Arrival
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = StringTranslator.translate(context, "Arrivée"),
                        fontSize = 12.sp,
                        color = colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formatTime(arrivalTime),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp
                    )
                    Text(
                        text = firstSegment?.arrival?.iataCode ?: destination?.city ?: "N/A",
                        fontSize = 12.sp,
                        color = colorScheme.onSurfaceVariant
                    )
                }
            }
            
            // Price breakdown
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = StringTranslator.translate(context, "Prix de base"),
                            fontSize = 12.sp,
                            color = colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = String.format("%.2f %s", priceBreakdown.basePrice, currency),
                            fontSize = 12.sp,
                            color = colorScheme.onSurfaceVariant
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = StringTranslator.translate(context, "Commission (${priceBreakdown.commissionPercentage}%)"),
                            fontSize = 12.sp,
                            color = colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = String.format("%.2f %s", priceBreakdown.commission, currency),
                            fontSize = 12.sp,
                            color = colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onOrganize,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Group,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = StringTranslator.translate(context, "Organiser"),
                        fontSize = 12.sp
                    )
                }
                Button(
                    onClick = onSelect,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = StringTranslator.translate(context, "Réserver"),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyAirlinesState(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    Column(
        modifier = modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Filled.Flight,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = StringTranslator.translate(context, "Aucune compagnie disponible"),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = StringTranslator.translate(context, "Aucun vol disponible pour cette destination pour le moment."),
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun ErrorAirlinesState(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    Column(
        modifier = modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = message,
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onRetry) {
            Text(StringTranslator.translate(context, "Réessayer"))
        }
    }
}

private fun getAirlineName(code: String): String {
    return when (code.uppercase()) {
        "AF" -> "Air France"
        "LH" -> "Lufthansa"
        "EK" -> "Emirates"
        "TK" -> "Turkish Airlines"
        "BA" -> "British Airways"
        "FR" -> "Ryanair"
        "U2" -> "easyJet"
        "VY" -> "Vueling"
        "IB" -> "Iberia"
        "SN" -> "Brussels Airlines"
        else -> code
    }
}

private fun formatTime(dateTimeString: String?): String {
    if (dateTimeString == null) return "N/A"
    return try {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
        val outputFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val date = inputFormat.parse(dateTimeString)
        date?.let { outputFormat.format(it) } ?: "N/A"
    } catch (e: Exception) {
        dateTimeString.substringAfter("T").substringBefore(":")
    }
}

