package tn.esprit.wayfinder.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import androidx.compose.ui.tooling.preview.Preview
import tn.esprit.wayfinder.ui.theme.WayFinderTheme
import android.app.Application
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import tn.esprit.wayfinder.models.FlightDestination
import tn.esprit.wayfinder.navigation.SELECTED_DESTINATION_KEY
import tn.esprit.wayfinder.presentation.auth.ViewModelFactory
import tn.esprit.wayfinder.ui.components.ReviewsSection
import tn.esprit.wayfinder.ui.components.CreatePriceAlertDialog
import tn.esprit.wayfinder.ui.components.TravelTipsSection
import tn.esprit.wayfinder.viewmodels.ReviewsViewModel
import tn.esprit.wayfinder.viewmodels.PriceAlertsViewModel
import tn.esprit.wayfinder.viewmodels.TravelTipsViewModel
import tn.esprit.wayfinder.data.OfflineDestinationsManager
import tn.esprit.wayfinder.utils.NetworkUtils
import tn.esprit.wayfinder.utils.StringTranslator
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlightDetailsScreen(navController: NavController, destinationId: String) {
    val context = LocalContext.current
    val reviewsViewModel: ReviewsViewModel = viewModel(factory = ViewModelFactory(context.applicationContext as Application))
    val priceAlertsViewModel: PriceAlertsViewModel = viewModel(factory = ViewModelFactory(context.applicationContext as Application))
    val travelTipsViewModel: TravelTipsViewModel = viewModel(factory = ViewModelFactory(context.applicationContext as Application))
    val offlineManager = remember { OfflineDestinationsManager(context) }
    val coroutineScope = rememberCoroutineScope()
    
    var showPriceAlertDialog by remember { mutableStateOf(false) }
    var isDownloaded by remember { mutableStateOf(false) }
    var isDownloading by remember { mutableStateOf(false) }
    
    val savedDestination = remember(destinationId) {
        navController.previousBackStackEntry
            ?.savedStateHandle
            ?.get<FlightDestination>(SELECTED_DESTINATION_KEY)
    }
    val destination = remember(destinationId, savedDestination, context) {
        savedDestination ?: FlightDestination(
            id = destinationId,
            name = StringTranslator.translate(context, "Destination surprise"),
            city = StringTranslator.translate(context, "À définir"),
            country = "",
            price = 0.0,
            currency = "EUR",
            description = StringTranslator.translate(context, "Impossible de charger les détails depuis la sélection précédente."),
            departureDate = null,
            arrivalDate = null,
            airline = null
        )
    }
    
    // Check if destination is already downloaded
    LaunchedEffect(destination.id) {
        isDownloaded = offlineManager.isDownloaded(destination.id)
    }

    LaunchedEffect(destination) {
        navController.currentBackStackEntry
            ?.savedStateHandle
            ?.set(SELECTED_DESTINATION_KEY, destination)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(StringTranslator.translate(context, "Détails du vol")) },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Destination Header Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color(0xFF4A90E2), Color(0xFF2E5C8A))
                            )
                        )
                        .padding(24.dp)
                ) {
                    Text(
                        text = destination.name,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = destination.country,
                        fontSize = 18.sp,
                        color = Color.White.copy(alpha = 0.9f),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    if (destination.price != null) {
                        Text(
                            text = "${destination.price.toInt()} ${destination.currency}",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(top = 16.dp)
                        )
                    }
                }
            }

            // Flight Information
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = StringTranslator.translate(context, "Informations du vol"),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = StringTranslator.translate(context, "Départ"),
                                fontSize = 14.sp,
                                color = Color.Gray
                            )
                            Text(
                                text = destination.departureDate?.substringBefore("T") ?: "N/A",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = destination.departureDate?.substringAfter("T")?.substringBefore(":")?.let {
                                    "${it}:${destination.departureDate.substringAfter(":").substringBefore(":")}"
                                } ?: "N/A",
                                fontSize = 16.sp,
                                color = Color.Gray
                            )
                        }

                        Icon(
                            imageVector = Icons.Filled.Flight,
                            contentDescription = null,
                            tint = Color(0xFF1976D2),
                            modifier = Modifier.size(32.dp)
                        )

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = StringTranslator.translate(context, "Arrivée"),
                                fontSize = 14.sp,
                                color = Color.Gray
                            )
                            Text(
                                text = destination.arrivalDate?.substringBefore("T") ?: "N/A",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = destination.arrivalDate?.substringAfter("T")?.substringBefore(":")?.let {
                                    "${it}:${destination.arrivalDate.substringAfter(":").substringBefore(":")}"
                                } ?: "N/A",
                                fontSize = 16.sp,
                                color = Color.Gray
                            )
                        }
                    }

                    HorizontalDivider()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = StringTranslator.translate(context, "Compagnie aérienne"),
                                fontSize = 14.sp,
                                color = Color.Gray
                            )
                            Text(
                                text = destination.airline ?: "N/A",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = StringTranslator.translate(context, "Durée"),
                                fontSize = 14.sp,
                                color = Color.Gray
                            )
                            Text(
                                text = "~4h 30min",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // Description
            if (destination.description != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(bottom = 12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.LocationOn,
                                contentDescription = null,
                                tint = Color(0xFF1976D2)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = StringTranslator.translate(context, "À propos"),
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = destination.description,
                            fontSize = 16.sp,
                            lineHeight = 24.sp
                        )
                    }
                }
            }

            // Travel Tips Section
            TravelTipsSection(
                destinationId = destination.id,
                destinationName = destination.name,
                city = destination.city,
                country = destination.country,
                travelTipsViewModel = travelTipsViewModel,
                navController = navController
            )

            // Reviews Section
            ReviewsSection(
                itemType = "flight",
                itemId = destination.id,
                reviewsViewModel = reviewsViewModel
            )

            // Download Button
            if (isDownloaded) {
                OutlinedButton(
                    onClick = {
                        coroutineScope.launch {
                            offlineManager.removeFromOffline(destination.id)
                            isDownloaded = false
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFF1976D2)
                    )
                ) {
                    Icon(
                        imageVector = Icons.Filled.CloudDone,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = StringTranslator.translate(context, "Téléchargé - Supprimer"),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            } else {
                OutlinedButton(
                    onClick = {
                        if (!isDownloading) {
                            isDownloading = true
                            coroutineScope.launch {
                                val success = offlineManager.downloadForOffline(destination)
                                if (success) {
                                    isDownloaded = true
                                }
                                isDownloading = false
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(16.dp),
                    enabled = !isDownloading,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFF1976D2)
                    )
                ) {
                    if (isDownloading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Filled.Download,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isDownloading) StringTranslator.translate(context, "Téléchargement...") else StringTranslator.translate(context, "Télécharger pour hors ligne"),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = { showPriceAlertDialog = true },
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.NotificationsActive,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = StringTranslator.translate(context, "Alerte prix"),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                Button(
                    onClick = {
                        // Navigate to booking screen
                        navController.currentBackStackEntry
                            ?.savedStateHandle
                            ?.set(SELECTED_DESTINATION_KEY, destination)
                        navController.navigate("booking/${destination.id}")
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF1976D2)
                    )
                ) {
                    Text(
                        text = StringTranslator.translate(context, "Réserver"),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }

    if (showPriceAlertDialog) {
        CreatePriceAlertDialog(
            destination = destination,
            currentPrice = destination.price,
            currency = destination.currency,
            onDismiss = { showPriceAlertDialog = false },
            onCreate = { request ->
                priceAlertsViewModel.createPriceAlert(request) {
                    // Show success message or navigate
                }
            }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun FlightDetailsScreenPreview() {
    WayFinderTheme {
        FlightDetailsScreen(rememberNavController(), destinationId = "test-destination-id")
    }
}

