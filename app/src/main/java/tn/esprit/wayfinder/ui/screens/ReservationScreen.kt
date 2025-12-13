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
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import tn.esprit.wayfinder.ui.theme.WayFinderTheme
import androidx.compose.ui.platform.LocalContext
import java.util.Locale
import tn.esprit.wayfinder.models.FlightDestination
import tn.esprit.wayfinder.models.Accommodation
import tn.esprit.wayfinder.models.SelectedUpsell
import tn.esprit.wayfinder.navigation.BOOKING_CURRENCY_KEY
import tn.esprit.wayfinder.navigation.BOOKING_DESTINATION_NAME_KEY
import tn.esprit.wayfinder.navigation.BOOKING_TOTAL_KEY
import tn.esprit.wayfinder.navigation.SELECTED_DESTINATION_KEY
import tn.esprit.wayfinder.navigation.BOOKING_CARD_NUMBER_KEY
import tn.esprit.wayfinder.navigation.BOOKING_CARD_NAME_KEY
import tn.esprit.wayfinder.navigation.BOOKING_CARD_EXPIRY_KEY
import tn.esprit.wayfinder.navigation.BOOKING_CARD_CVV_KEY
import tn.esprit.wayfinder.presentation.auth.ViewModelFactory
import tn.esprit.wayfinder.ui.components.CustomBottomNavigationBar
import tn.esprit.wayfinder.viewmodels.*
import tn.esprit.wayfinder.utils.StringTranslator
import tn.esprit.wayfinder.utils.NotificationHelper
import tn.esprit.wayfinder.manager.LanguageManager
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReservationScreen(navController: NavController, destinationId: String) {
    val context = LocalContext.current
    val bookingViewModel: BookingViewModel = viewModel(factory = ViewModelFactory(context.applicationContext as Application))
    val notificationsViewModel: NotificationsViewModel = viewModel(factory = ViewModelFactory(context.applicationContext as Application))
    val reservationState by bookingViewModel.reservationState.collectAsState()
    val comparisonState by bookingViewModel.offerComparisonState.collectAsState()

    val selectedDestination = remember(destinationId) {
        navController.previousBackStackEntry
            ?.savedStateHandle
            ?.get<FlightDestination>(SELECTED_DESTINATION_KEY)
    }
    
    // Get accommodation and upsells from saved state
    // Reconstruct Accommodation from primitive fields (SavedStateHandle doesn't support complex objects)
    val savedStateHandle = remember(destinationId) {
        navController.previousBackStackEntry?.savedStateHandle
    }
    val selectedAccommodation = remember(destinationId) {
        val id = savedStateHandle?.get<String>("accommodation_id")
        val name = savedStateHandle?.get<String>("accommodation_name")
        val type = savedStateHandle?.get<String>("accommodation_type")
        val price = savedStateHandle?.get<Double>("accommodation_price")
        val currency = savedStateHandle?.get<String>("accommodation_currency")
        val location = savedStateHandle?.get<String>("accommodation_location")
        val rating = savedStateHandle?.get<Double>("accommodation_rating")
        val imageUrl = savedStateHandle?.get<String>("accommodation_image_url")
        
        if (id != null && name != null && type != null && price != null && currency != null && location != null && rating != null) {
            Accommodation(
                id = id,
                name = name,
                type = type,
                price = price,
                currency = currency,
                rating = rating,
                imageUrl = imageUrl?.takeIf { it.isNotEmpty() },
                location = location,
                amenities = emptyList() // Amenities not saved, but not critical for display
            )
        } else {
            null
        }
    }
    
    val selectedUpsells = remember(destinationId) {
        navController.previousBackStackEntry
            ?.savedStateHandle
            ?.get<List<SelectedUpsell>>("selected_upsells") ?: emptyList()
    }
    
    var cardNumber by remember { mutableStateOf("") }
    var cardHolderName by remember { mutableStateOf("") }
    var expiryDate by remember { mutableStateOf("") }
    var cvv by remember { mutableStateOf("") }
    
    // Validation errors
    var cardNumberError by remember { mutableStateOf<String?>(null) }
    var cardHolderNameError by remember { mutableStateOf<String?>(null) }
    var expiryDateError by remember { mutableStateOf<String?>(null) }
    var cvvError by remember { mutableStateOf<String?>(null) }
    
    LaunchedEffect(destinationId) {
        if (destinationId.isNotBlank()) {
            bookingViewModel.loadOfferComparison(destinationId)
        }
    }


    val currency = selectedDestination?.currency ?: selectedAccommodation?.currency ?: "EUR"
    val comparison = (comparisonState as? OfferComparisonUiState.Success)?.comparison
    val destinationPrice = selectedDestination?.price ?: 0.0
    val basePrice = if (destinationPrice > 0) destinationPrice else comparison?.basePrice ?: 0.0
    val taxes = comparison?.taxes ?: (basePrice * 0.15)
    val baggage = comparison?.baggage ?: 0.0
    val serviceFees = comparison?.serviceFees ?: 10.0
    
    // Calculate accommodation price (per night, assume 7 nights default)
    val accommodationPrice = selectedAccommodation?.let { it.price * 7 } ?: 0.0
    
    // Calculate upsell total
    val upsellTotal = selectedUpsells.sumOf { it.price * it.quantity }
    
    // Calculate commission
    val commission = selectedUpsells.sumOf { it.commissionAmount * it.quantity }
    
    // Total includes flight, accommodation, upsells, taxes, fees
    val total = basePrice + taxes + baggage + serviceFees + accommodationPrice + upsellTotal
    val displayPrice = basePrice

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(StringTranslator.translate(context, "Réservation"), fontWeight = FontWeight.Bold) },
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
        containerColor = MaterialTheme.colorScheme.surface
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
                    text = StringTranslator.translate(context, "Impossible de charger les détails du vol sélectionné. Veuillez revenir aux résultats."),
                    color = Color.Red,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            
            selectedDestination?.let { destination ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
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
                            text = "${destination.country} • ${destination.airline ?: StringTranslator.translate(context, "Compagnie inconnue")}",
                            color = Color.Gray
                        )
                        if (destination.departureDate != null && destination.arrivalDate != null) {
                            Text(
                                text = "${StringTranslator.translate(context, "Départ :")} ${destination.departureDate.substringBefore("T")} | ${StringTranslator.translate(context, "Retour :")} ${destination.arrivalDate.substringBefore("T")}",
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
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
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
                            text = StringTranslator.translate(context, "Informations de paiement"),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    OutlinedTextField(
                        value = cardNumber,
                        onValueChange = { 
                            // Format card number (add spaces every 4 digits)
                            val digitsOnly = it.filter { it.isDigit() }
                            if (digitsOnly.length <= 16) {
                                val formatted = digitsOnly.chunked(4).joinToString(" ")
                                cardNumber = formatted
                                // Validate: must be 16 digits
                                cardNumberError = if (digitsOnly.length < 16 && digitsOnly.isNotEmpty()) {
                                    StringTranslator.translate(context, "Le numéro de carte doit contenir 16 chiffres")
                                } else null
                            }
                        },
                        label = { Text(StringTranslator.translate(context, "Numéro de carte")) },
                        placeholder = { Text("1234 5678 9012 3456") },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = KeyboardType.Number
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = {
                            Icon(Icons.Filled.CreditCard, contentDescription = null)
                        },
                        isError = cardNumberError != null,
                        supportingText = cardNumberError?.let { { Text(it) } }
                    )
                    
                    OutlinedTextField(
                        value = cardHolderName,
                        onValueChange = { 
                            // Only allow letters, spaces, and common name characters
                            if (it.all { char -> char.isLetter() || char.isWhitespace() || char == '-' || char == '\'' }) {
                                cardHolderName = it
                                // Validate: must not be empty and at least 2 characters
                                cardHolderNameError = if (it.isBlank()) {
                                    StringTranslator.translate(context, "Le nom ne peut pas être vide")
                                } else if (it.trim().length < 2) {
                                    StringTranslator.translate(context, "Le nom doit contenir au moins 2 caractères")
                                } else null
                            }
                        },
                        label = { Text(StringTranslator.translate(context, "Nom sur la carte")) },
                        placeholder = { Text("John Doe") },
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = {
                            Icon(Icons.Filled.Person, contentDescription = null)
                        },
                        isError = cardHolderNameError != null,
                        supportingText = cardHolderNameError?.let { { Text(it) } }
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = expiryDate,
                            onValueChange = { newValue ->
                                // Remove any existing slashes and non-digits
                                val cleanInput = newValue.replace("/", "").filter { it.isDigit() }
                                
                                // Limit to 4 digits
                                if (cleanInput.length <= 4) {
                                    val formatted = when {
                                        cleanInput.isEmpty() -> ""
                                        cleanInput.length <= 2 -> cleanInput
                                        cleanInput.length == 3 -> {
                                            // Format as MM/Y (e.g., "09/2")
                                            "${cleanInput.substring(0, 2)}/${cleanInput.substring(2)}"
                                        }
                                        else -> {
                                            // Format as MM/YY (e.g., "09/26")
                                            "${cleanInput.substring(0, 2)}/${cleanInput.substring(2, 4)}"
                                        }
                                    }
                                    
                                    expiryDate = formatted
                                    
                                    // Validate date only when we have exactly 4 digits
                                    if (cleanInput.length == 4) {
                                        val monthStr = cleanInput.substring(0, 2)
                                        val yearStr = cleanInput.substring(2, 4)
                                        val month = monthStr.toIntOrNull()
                                        val year = yearStr.toIntOrNull()
                                        val currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR) % 100
                                        
                                        expiryDateError = when {
                                            month == null || month < 1 || month > 12 -> {
                                                StringTranslator.translate(context, "Mois invalide (01-12)")
                                            }
                                            year == null || year < currentYear -> {
                                                StringTranslator.translate(context, "L'année doit être dans le futur")
                                            }
                                            else -> null
                                        }
                                    } else {
                                        expiryDateError = null
                                    }
                                }
                            },
                            label = { Text(StringTranslator.translate(context, "Expiration")) },
                            placeholder = { Text("MM/YY") },
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                keyboardType = KeyboardType.Number
                            ),
                            modifier = Modifier.weight(1f),
                            isError = expiryDateError != null,
                            supportingText = expiryDateError?.let { { Text(it) } }
                        )
                        
                        OutlinedTextField(
                            value = cvv,
                            onValueChange = { 
                                val digitsOnly = it.filter { char -> char.isDigit() }
                                if (digitsOnly.length <= 4) {
                                    cvv = digitsOnly
                                    // Validate: must be 3 or 4 digits
                                    cvvError = if (digitsOnly.length < 3 && digitsOnly.isNotEmpty()) {
                                        StringTranslator.translate(context, "Le CVV doit contenir 3 ou 4 chiffres")
                                    } else null
                                }
                            },
                            label = { Text(StringTranslator.translate(context, "CVV")) },
                            placeholder = { Text("123") },
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                keyboardType = KeyboardType.Number
                            ),
                            modifier = Modifier.weight(1f),
                            isError = cvvError != null,
                            supportingText = cvvError?.let { { Text(it) } }
                        )
                    }
                }
            }
            
            // Summary Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = StringTranslator.translate(context, "Résumé"),
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
                        Text(StringTranslator.translate(context, "Prix du vol"), color = Color.Gray)
                        Text(String.format(Locale.getDefault(), "%.2f %s", basePrice, currency), fontWeight = FontWeight.Medium)
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(StringTranslator.translate(context, "Taxes"), color = Color.Gray)
                        Text(String.format(Locale.getDefault(), "%.2f %s", taxes, currency), fontWeight = FontWeight.Medium)
                    }
                    
                    // Accommodation
                    if (accommodationPrice > 0 && selectedAccommodation != null) {
                        HorizontalDivider()
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(StringTranslator.translate(context, "Hébergement"), color = Color.Gray)
                                Text(
                                    text = selectedAccommodation.name,
                                    fontSize = 12.sp,
                                    color = Color.Gray
                                )
                            }
                            Text(
                                String.format(Locale.getDefault(), "%.2f %s", accommodationPrice, currency),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    
                    // Upsells
                    if (upsellTotal > 0) {
                        HorizontalDivider()
                        Column(
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            selectedUpsells.forEach { upsell ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        StringTranslator.translate(context, "Service additionnel"),
                                        color = Color.Gray,
                                        fontSize = 12.sp
                                    )
                                    Text(
                                        String.format(Locale.getDefault(), "%.2f %s", upsell.price * upsell.quantity, upsell.currency),
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    StringTranslator.translate(context, "Total services"),
                                    color = Color.Gray
                                )
                                Text(
                                    String.format(Locale.getDefault(), "%.2f %s", upsellTotal, currency),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(StringTranslator.translate(context, "Bagages & services"), color = Color.Gray)
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
                            text = StringTranslator.translate(context, "Total"),
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
            
            Button(
                onClick = {
                    val cardNumberDigits = cardNumber.replace(" ", "")
                    val isValid = cardNumberDigits.length == 16 &&
                            cardHolderName.trim().length >= 2 &&
                            expiryDate.length == 5 &&
                            (cvv.length == 3 || cvv.length == 4) &&
                            cardNumberError == null &&
                            cardHolderNameError == null &&
                            expiryDateError == null &&
                            cvvError == null

                    if (isValid && selectedDestination != null) {
                        navController.currentBackStackEntry?.savedStateHandle?.apply {
                            set(SELECTED_DESTINATION_KEY, selectedDestination)
                            set(BOOKING_TOTAL_KEY, total)
                            set(BOOKING_CURRENCY_KEY, currency)
                            set(BOOKING_DESTINATION_NAME_KEY, selectedDestination.name)
                            set(BOOKING_CARD_NUMBER_KEY, cardNumberDigits)
                            set(BOOKING_CARD_NAME_KEY, cardHolderName.trim())
                            set(BOOKING_CARD_EXPIRY_KEY, expiryDate)
                            set(BOOKING_CARD_CVV_KEY, cvv)
                            // Save accommodation and upsells (only primitive types)
                            if (selectedAccommodation != null) {
                                // Accommodation object already saved as primitive fields, just save the calculated price
                                set("accommodation_price", accommodationPrice)
                            }
                            if (selectedUpsells.isNotEmpty()) {
                                set("selected_upsells", selectedUpsells)
                                set("upsell_total", upsellTotal)
                                set("commission_total", commission)
                            }
                        }
                        navController.navigate("review_booking/${selectedDestination.id}")
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF1976D2)
                ),
                enabled = selectedDestination != null && run {
                    val cardNumberDigits = cardNumber.replace(" ", "")
                    cardNumberDigits.length == 16 &&
                            cardHolderName.trim().length >= 2 &&
                            expiryDate.length == 5 &&
                            (cvv.length == 3 || cvv.length == 4) &&
                            cardNumberError == null &&
                            cardHolderNameError == null &&
                            expiryDateError == null &&
                            cvvError == null
                }
            ) {
                Text(
                    text = StringTranslator.translate(context, "Continuer"),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ReservationScreenPreview() {
    WayFinderTheme {
        ReservationScreen(rememberNavController(), destinationId = "test-destination-id")
    }
}
