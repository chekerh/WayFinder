package tn.esprit.wayfinder.ui.screens

import android.app.Application
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.compose.ui.platform.LocalContext
import java.util.Locale
import tn.esprit.wayfinder.models.FlightDestination
import tn.esprit.wayfinder.navigation.BOOKING_CURRENCY_KEY
import tn.esprit.wayfinder.navigation.BOOKING_DESTINATION_NAME_KEY
import tn.esprit.wayfinder.navigation.BOOKING_TOTAL_KEY
import tn.esprit.wayfinder.navigation.SELECTED_DESTINATION_KEY
import tn.esprit.wayfinder.presentation.auth.ViewModelFactory
import tn.esprit.wayfinder.ui.components.CustomBottomNavigationBar
import tn.esprit.wayfinder.viewmodels.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReservationScreen(navController: NavController, destinationId: String) {
    val context = LocalContext.current
    val bookingViewModel: BookingViewModel = viewModel(factory = ViewModelFactory(context.applicationContext as Application))
    val reservationState by bookingViewModel.reservationState.collectAsState()
    val comparisonState by bookingViewModel.offerComparisonState.collectAsState()

    val selectedDestination = remember(destinationId) {
        navController.previousBackStackEntry
            ?.savedStateHandle
            ?.get<FlightDestination>(SELECTED_DESTINATION_KEY)
    }
    
    var cardNumber by remember { mutableStateOf("") }
    var cardHolderName by remember { mutableStateOf("") }
    var expiryDate by remember { mutableStateOf("") }
    var cvv by remember { mutableStateOf("") }
    
    LaunchedEffect(destinationId) {
        if (destinationId.isNotBlank()) {
            bookingViewModel.loadOfferComparison(destinationId)
        }
    }

    // Handle successful booking
    LaunchedEffect(reservationState) {
        if (reservationState is ReservationUiState.Success) {
            val booking = (reservationState as ReservationUiState.Success).booking
            navController.currentBackStackEntry?.savedStateHandle?.set(BOOKING_TOTAL_KEY, booking.totalPrice)
            navController.currentBackStackEntry?.savedStateHandle?.set(
                BOOKING_DESTINATION_NAME_KEY,
                selectedDestination?.name
            )
            navController.currentBackStackEntry?.savedStateHandle?.set(
                BOOKING_CURRENCY_KEY,
                selectedDestination?.currency ?: "EUR"
            )
            navController.navigate("booking_confirmation/${booking.confirmationNumber}") {
                popUpTo("home") { inclusive = false }
            }
            bookingViewModel.resetReservationState()
        }
    }

    val currency = selectedDestination?.currency ?: "EUR"
    val comparison = (comparisonState as? OfferComparisonUiState.Success)?.comparison
    val destinationPrice = selectedDestination?.price ?: 0.0
    val basePrice = if (destinationPrice > 0) destinationPrice else comparison?.basePrice ?: 0.0
    val taxes = comparison?.taxes ?: (basePrice * 0.15)
    val baggage = comparison?.baggage ?: 0.0
    val serviceFees = comparison?.serviceFees ?: 10.0
    val total = basePrice + taxes + baggage + serviceFees
    val displayPrice = basePrice

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Réservation", fontWeight = FontWeight.Bold) },
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
        bottomBar = {
            CustomBottomNavigationBar(navController = navController)
        },
        containerColor = Color(0xFFEAF2FF)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            if (selectedDestination == null) {
                Text(
                    text = "Impossible de charger les détails du vol sélectionné. Veuillez revenir aux résultats.",
                    color = Color.Red,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            
            selectedDestination?.let { destination ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = destination.name,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${destination.country} • ${destination.airline ?: "Compagnie inconnue"}",
                            color = Color.Gray
                        )
                        if (destination.departureDate != null && destination.arrivalDate != null) {
                            Text(
                                text = "Départ : ${destination.departureDate.substringBefore("T")} | Retour : ${destination.arrivalDate.substringBefore("T")}",
                                color = Color.Gray
                            )
                        }
                        if (displayPrice > 0) {
                            Text(
                                text = String.format(Locale.getDefault(), "%.2f %s", displayPrice, currency),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
            
            // Payment Information Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CreditCard,
                            contentDescription = null,
                            tint = Color(0xFF1976D2)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Informations de paiement",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    OutlinedTextField(
                        value = cardNumber,
                        onValueChange = { 
                            // Format card number (add spaces every 4 digits)
                            val formatted = it.filter { it.isDigit() }
                                .chunked(4)
                                .joinToString(" ")
                            if (formatted.length <= 19) cardNumber = formatted
                        },
                        label = { Text("Numéro de carte") },
                        placeholder = { Text("1234 5678 9012 3456") },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = KeyboardType.Number
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = {
                            Icon(Icons.Filled.CreditCard, contentDescription = null)
                        }
                    )
                    
                    OutlinedTextField(
                        value = cardHolderName,
                        onValueChange = { cardHolderName = it },
                        label = { Text("Nom sur la carte") },
                        placeholder = { Text("John Doe") },
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = {
                            Icon(Icons.Filled.Person, contentDescription = null)
                        }
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = expiryDate,
                            onValueChange = { 
                                // Format MM/YY
                                val formatted = it.filter { it.isDigit() }
                                if (formatted.length <= 4) {
                                    expiryDate = if (formatted.length > 2) {
                                        "${formatted.substring(0, 2)}/${formatted.substring(2)}"
                                    } else {
                                        formatted
                                    }
                                }
                            },
                            label = { Text("Expiration") },
                            placeholder = { Text("MM/YY") },
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                keyboardType = KeyboardType.Number
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        
                        OutlinedTextField(
                            value = cvv,
                            onValueChange = { 
                                if (it.length <= 3 && it.all { char -> char.isDigit() }) {
                                    cvv = it
                                }
                            },
                            label = { Text("CVV") },
                            placeholder = { Text("123") },
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                keyboardType = KeyboardType.Number
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
            
            // Summary Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Résumé",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    
                    if (comparisonState is OfferComparisonUiState.Loading) {
                        LinearProgressIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp),
                            color = Color(0xFF1976D2)
                        )
                    }
                    
                    HorizontalDivider()
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Prix du vol", color = Color.Gray)
                        Text(String.format(Locale.getDefault(), "%.2f %s", basePrice, currency), fontWeight = FontWeight.Medium)
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Taxes", color = Color.Gray)
                        Text(String.format(Locale.getDefault(), "%.2f %s", taxes, currency), fontWeight = FontWeight.Medium)
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Bagages & services", color = Color.Gray)
                        Text(String.format(Locale.getDefault(), "%.2f %s", baggage + serviceFees, currency), fontWeight = FontWeight.Medium)
                    }

                    if (comparisonState is OfferComparisonUiState.Error) {
                        Text(
                            text = (comparisonState as OfferComparisonUiState.Error).message,
                            color = Color.Red,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    
                    HorizontalDivider()
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Total",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = String.format(Locale.getDefault(), "%.2f %s", total, currency),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1976D2)
                        )
                    }
                }
            }
            
            // Confirm Button
            when (reservationState) {
                is ReservationUiState.Loading -> {
                    Button(
                        onClick = { /* Already processing */ },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF1976D2)
                        ),
                        enabled = false
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color.White
                        )
                    }
                }
                is ReservationUiState.Error -> {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = (reservationState as ReservationUiState.Error).message,
                            color = Color.Red,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                        Button(
                            onClick = {
                                bookingViewModel.confirmBooking(
                                    offerId = destinationId,
                                    cardNumber = cardNumber.replace(" ", ""),
                                    cardHolderName = cardHolderName,
                                    totalPrice = total,
                                    destination = selectedDestination?.name,
                                    destinationCountry = selectedDestination?.country
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF1976D2)
                            )
                        ) {
                            Text(
                                text = "Réessayer",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                else -> {
                    Button(
                        onClick = {
                            if (cardNumber.isNotBlank() && cardHolderName.isNotBlank() && 
                                expiryDate.isNotBlank() && cvv.isNotBlank()) {
                                bookingViewModel.confirmBooking(
                                    offerId = destinationId,
                                    cardNumber = cardNumber.replace(" ", ""),
                                cardHolderName = cardHolderName,
                                    totalPrice = total,
                                    destination = selectedDestination?.name,
                                    destinationCountry = selectedDestination?.country
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF1976D2)
                        ),
                        enabled = cardNumber.isNotBlank() && cardHolderName.isNotBlank() && 
                                  expiryDate.isNotBlank() && cvv.isNotBlank()
                    ) {
                        Text(
                            text = "Confirmer la réservation",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
