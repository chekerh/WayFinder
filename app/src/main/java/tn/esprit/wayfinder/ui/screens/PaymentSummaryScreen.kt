package tn.esprit.wayfinder.ui.screens

import android.app.Application
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import tn.esprit.wayfinder.models.FlightDestination
import tn.esprit.wayfinder.navigation.SELECTED_DESTINATION_KEY
import tn.esprit.wayfinder.presentation.auth.ViewModelFactory
import tn.esprit.wayfinder.ui.theme.WayFinderTheme
import tn.esprit.wayfinder.utils.StringTranslator
import tn.esprit.wayfinder.viewmodels.PaymentViewModel

/**
 * Service fee percentage (configurable)
 */
private const val WAYFINDER_FEE_PERCENT = 5.0

/**
 * Payment Summary Screen
 * Shows complete price breakdown including flight, hotel, and WayFinder service fee
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentSummaryScreen(
    navController: NavController,
    destinationId: String
) {
    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme
    
    val paymentViewModel: PaymentViewModel = viewModel(
        factory = ViewModelFactory(context.applicationContext as Application)
    )
    
    // Get data from navigation
    val destination = remember(destinationId) {
        navController.previousBackStackEntry
            ?.savedStateHandle
            ?.get<FlightDestination>(SELECTED_DESTINATION_KEY)
    }
    
    val savedStateHandle = navController.previousBackStackEntry?.savedStateHandle
    
    // Get accommodation details
    val accommodationName = savedStateHandle?.get<String>("accommodation_name") ?: ""
    val accommodationPrice = savedStateHandle?.get<Double>("accommodation_price") ?: 0.0
    val accommodationType = savedStateHandle?.get<String>("accommodation_type") ?: ""
    
    // Calculate prices
    val flightPrice = destination?.price ?: 0.0
    val currency = destination?.currency ?: "EUR"
    
    // Assume 3 nights for hotel (can be made dynamic)
    val nights = 3
    val hotelTotal = accommodationPrice * nights
    
    val subtotal = flightPrice + hotelTotal
    val serviceFee = subtotal * (WAYFINDER_FEE_PERCENT / 100)
    val totalPrice = subtotal + serviceFee
    
    var isProcessing by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = StringTranslator.translate(context, "Résumé du paiement"),
                        fontWeight = FontWeight.Bold
                    )
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
        containerColor = colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Trip Summary Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            Icons.Filled.FlightTakeoff,
                            contentDescription = null,
                            tint = colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Column {
                            Text(
                                text = StringTranslator.translate(context, "Votre voyage à"),
                                fontSize = 14.sp,
                                color = colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${destination?.name ?: "Destination"}, ${destination?.country ?: ""}",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = colorScheme.onSurface
                            )
                        }
                    }
                    
                    HorizontalDivider()
                    
                    // Flight details
                    if (destination != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = StringTranslator.translate(context, "Vol aller-retour"),
                                    fontSize = 14.sp,
                                    color = colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = destination.airline ?: "Compagnie aérienne",
                                    fontSize = 12.sp,
                                    color = colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            }
                            Text(
                                text = "%.2f %s".format(flightPrice, currency),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = colorScheme.onSurface
                            )
                        }
                    }
                    
                    // Hotel details
                    if (accommodationPrice > 0) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "$accommodationName",
                                    fontSize = 14.sp,
                                    color = colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "$nights ${StringTranslator.translate(context, "nuits")} × %.2f %s".format(accommodationPrice, currency),
                                    fontSize = 12.sp,
                                    color = colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            }
                            Text(
                                text = "%.2f %s".format(hotelTotal, currency),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = colorScheme.onSurface
                            )
                        }
                    }
                }
            }
            
            // Price Breakdown Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = StringTranslator.translate(context, "Détail du prix"),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.onSurface
                    )
                    
                    HorizontalDivider()
                    
                    // Subtotal
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = StringTranslator.translate(context, "Sous-total"),
                            color = colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "%.2f %s".format(subtotal, currency),
                            color = colorScheme.onSurface
                        )
                    }
                    
                    // Service fee
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = StringTranslator.translate(context, "Frais de service WayFinder"),
                                color = colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "(${WAYFINDER_FEE_PERCENT.toInt()}%)",
                                fontSize = 12.sp,
                                color = colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                        Text(
                            text = "%.2f %s".format(serviceFee, currency),
                            color = colorScheme.onSurface
                        )
                    }
                    
                    HorizontalDivider(thickness = 2.dp)
                    
                    // Total
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = StringTranslator.translate(context, "TOTAL"),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = colorScheme.onSurface
                        )
                        Text(
                            text = "%.2f %s".format(totalPrice, currency),
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = colorScheme.primary
                        )
                    }
                }
            }
            
            // Info card about payment
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = colorScheme.secondaryContainer.copy(alpha = 0.5f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        Icons.Filled.Info,
                        contentDescription = null,
                        tint = colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(24.dp)
                    )
                    Column {
                        Text(
                            text = StringTranslator.translate(context, "Paiement sécurisé"),
                            fontWeight = FontWeight.Medium,
                            color = colorScheme.onSecondaryContainer
                        )
                        Text(
                            text = StringTranslator.translate(context, "Vous payez WayFinder qui s'occupe de toutes les réservations. Vos billets et confirmations seront disponibles immédiatement."),
                            fontSize = 12.sp,
                            color = colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Payment methods
            Text(
                text = StringTranslator.translate(context, "Méthode de paiement"),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = colorScheme.onSurface
            )
            
            // PayPal Button
            Button(
                onClick = {
                    if (!isProcessing) {
                        isProcessing = true
                        // Save all booking data including dates and navigate to existing booking flow
                        navController.currentBackStackEntry?.savedStateHandle?.apply {
                            set(SELECTED_DESTINATION_KEY, destination)
                            set("total_price", totalPrice)
                            set("service_fee", serviceFee)
                            set("accommodation_name", accommodationName)
                            set("accommodation_price", hotelTotal)
                            // Pass through dates from LodgingChoiceScreen
                            val checkInDate = navController.previousBackStackEntry?.savedStateHandle?.get<String>("check_in_date")
                            val checkOutDate = navController.previousBackStackEntry?.savedStateHandle?.get<String>("check_out_date")
                            checkInDate?.let { set("check_in_date", it) }
                            checkOutDate?.let { set("check_out_date", it) }
                            // Pass through accommodation details
                            val accommodationId = navController.previousBackStackEntry?.savedStateHandle?.get<String>("accommodation_id")
                            val accommodationCurrency = navController.previousBackStackEntry?.savedStateHandle?.get<String>("accommodation_currency")
                            val accommodationType = navController.previousBackStackEntry?.savedStateHandle?.get<String>("accommodation_type")
                            accommodationId?.let { set("accommodation_id", it) }
                            accommodationCurrency?.let { set("accommodation_currency", it) }
                            accommodationType?.let { set("accommodation_type", it) }
                        }
                        navController.navigate("booking/${destinationId}")
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colorScheme.primary
                ),
                enabled = !isProcessing
            ) {
                if (isProcessing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = colorScheme.onPrimary
                    )
                } else {
                    Icon(
                        Icons.Filled.Payment,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = StringTranslator.translate(context, "Payer avec PayPal"),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            // Alternative payment note
            Text(
                text = StringTranslator.translate(context, "Carte bancaire également acceptée via PayPal"),
                fontSize = 12.sp,
                color = colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PaymentSummaryScreenPreview() {
    WayFinderTheme {
        PaymentSummaryScreen(rememberNavController(), destinationId = "test")
    }
}

