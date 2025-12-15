package tn.esprit.wayfinder.ui.screens

import android.app.Application
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Bed
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.ui.unit.size
import androidx.compose.material3.CircularProgressIndicator
import tn.esprit.wayfinder.viewmodels.ReservationUiState
import kotlin.math.max
import androidx.compose.ui.platform.LocalContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import android.app.DatePickerDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.size
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
        savedStateHandle?.get<List<SelectedUpsell>>("selected_upsells") ?: emptyList()
    }
    
    // Get dates from saved state (set in LodgingChoiceScreen)
    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val checkInDate = savedStateHandle?.get<String>("check_in_date")
    val checkOutDate = savedStateHandle?.get<String>("check_out_date")
    
    // Calculate number of nights
    val nights = remember(checkInDate, checkOutDate) {
        if (checkInDate != null && checkOutDate != null) {
            try {
                val checkIn = dateFormat.parse(checkInDate)
                val checkOut = dateFormat.parse(checkOutDate)
                if (checkIn != null && checkOut != null) {
                    val diff = checkOut.time - checkIn.time
                    max(1, (diff / (1000 * 60 * 60 * 24)).toInt())
                } else 7 // Default to 7 nights
            } catch (e: Exception) {
                7 // Default to 7 nights
            }
        } else 7 // Default to 7 nights if dates not set
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
    val breakdown = comparison?.breakdown
    val basePrice = if (destinationPrice > 0) {
        destinationPrice
    } else {
        breakdown?.basePrice ?: 0.0
    }
    val taxes = breakdown?.taxes ?: (basePrice * 0.15)
    val baggage = breakdown?.baggageFees ?: 0.0
    val serviceFees = breakdown?.serviceFees ?: 10.0
    
    // Calculate accommodation price based on number of nights
    val accommodationPrice = selectedAccommodation?.let { it.price * nights } ?: 0.0
    
    // Calculate upsell total
    val upsellTotal = selectedUpsells.sumOf { it.price * it.quantity }
    
    // Calculate commission on upsells (used for analytics, not added separately to total)
    val commission = selectedUpsells.sumOf { it.commissionAmount * it.quantity }
    
    // Subtotal before WayFinder platform fee
    val subtotal = basePrice + taxes + baggage + serviceFees + accommodationPrice + upsellTotal
    
    // WayFinder platform fee: 5% of subtotal
    val platformFeeRate = 0.05
    val platformFee = subtotal * platformFeeRate
    
    // Total includes flight, accommodation, upsells, taxes, fees, and platform fee
    val total = subtotal + platformFee
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
                // Destination Card (matching iOS)
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
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${destination.country} • ${destination.airline ?: StringTranslator.translate(context, "N/A")}",
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (displayPrice > 0) {
                            Text(
                                text = String.format(Locale.getDefault(), "%.2f %s", displayPrice, currency),
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            }
            
            // Accommodation Choice Card (matching iOS)
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
                            imageVector = Icons.Filled.Bed,
                            contentDescription = null,
                            tint = Color(0xFF1976D2),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = StringTranslator.translate(context, "Hébergement"),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    if (selectedAccommodation != null) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp)
                                .background(
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = selectedAccommodation.name,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Star,
                                            contentDescription = null,
                                            modifier = Modifier.size(14.dp),
                                            tint = Color(0xFFFFC107)
                                        )
                                        Text(
                                            text = String.format("%.1f", selectedAccommodation.rating),
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text("•", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(
                                            text = selectedAccommodation.location,
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Text(
                                    text = String.format("%.2f %s/nuit", selectedAccommodation.price, selectedAccommodation.currency),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            
                            TextButton(
                                onClick = {
                                    // Navigate back to accommodations list to change selection
                                    navController.navigate("accommodations/${destinationId}/${selectedAccommodation.type}")
                                }
                            ) {
                                Text(
                                    text = StringTranslator.translate(context, "Changer d'hébergement"),
                                    color = Color(0xFF1976D2)
                                )
                            }
                        }
                    } else {
                        Button(
                            onClick = {
                                // Navigate to lodging choice
                                navController.navigate("lodging_choice/$destinationId")
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Add,
                                    contentDescription = null,
                                    tint = Color(0xFF1976D2)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = StringTranslator.translate(context, "Choisir un hébergement"),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.weight(1f))
                                Icon(
                                    imageVector = Icons.Filled.ArrowForward,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
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
                    
                    // Accommodation (matching iOS format)
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

                    // WayFinder platform fee (5%)
                    HorizontalDivider()
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = StringTranslator.translate(context, "Frais WayFinder (5%)"),
                            color = Color.Gray
                        )
                        Text(
                            text = String.format(Locale.getDefault(), "%.2f %s", platformFee, currency),
                            fontWeight = FontWeight.Medium
                        )
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
            
            // Confirm Button (matching iOS - confirms booking directly)
            LaunchedEffect(reservationState) {
                when (val state = reservationState) {
                    is ReservationUiState.Success -> {
                        val booking = state.booking
                        // Save booking details
                        navController.currentBackStackEntry?.savedStateHandle?.apply {
                            set(BOOKING_TOTAL_KEY, booking.totalPrice)
                            set(BOOKING_DESTINATION_NAME_KEY, selectedDestination?.name)
                            set(BOOKING_CURRENCY_KEY, currency)
                            set("booking_id", booking.confirmationNumber)
                        }
                        
                        // Show notification
                        val languageManager = LanguageManager(context)
                        val destinationName = selectedDestination?.name ?: StringTranslator.translate(context, "votre destination")
                        val notificationTitle = StringTranslator.translate(context, "Réservation confirmée")
                        val reservationFor = StringTranslator.translate(context, "Votre réservation pour")
                        val hasBeenConfirmed = StringTranslator.translate(context, "a été confirmée")
                        val confirmationNumberLabel = StringTranslator.translate(context, "Numéro de confirmation")
                        val notificationMessage = "$reservationFor $destinationName $hasBeenConfirmed. $confirmationNumberLabel: ${booking.confirmationNumber}"
                        
                        NotificationHelper.showSimpleNotification(
                            context,
                            notificationTitle,
                            notificationMessage,
                            type = "booking_confirmed"
                        )
                        
                        delay(1500)
                        notificationsViewModel.loadNotifications(unreadOnly = true, showSystemNotifications = true)
                        
                        // Navigate to confirmation screen
                        navController.navigate("booking_confirmation/${booking.confirmationNumber}") {
                            popUpTo("home") { inclusive = false }
                        }
                        bookingViewModel.resetReservationState()
                    }
                    is ReservationUiState.Error -> {
                        // Error is handled in UI below
                        android.util.Log.e("ReservationScreen", "Booking confirmation error: ${state.message}")
                    }
                    else -> {
                        // Loading or Idle state - no action needed
                    }
                }
            }
            
            // Show error message if booking confirmation failed
            when (val errorState = reservationState) {
                is ReservationUiState.Error -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Text(
                            text = errorState.message,
                            modifier = Modifier.padding(16.dp),
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                else -> {}
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
                        // Calculate final total (including accommodation and upsells)
                        val checkInDate = navController.previousBackStackEntry?.savedStateHandle?.get<String>("check_in_date")
                        val checkOutDate = navController.previousBackStackEntry?.savedStateHandle?.get<String>("check_out_date")
                        
                        val finalAccommodationPrice = accommodationPrice
                        val finalTotal = basePrice + taxes + baggage + serviceFees + finalAccommodationPrice + upsellTotal
                        
                        // Confirm booking directly (matching iOS flow)
                        bookingViewModel.confirmBooking(
                            offerId = destinationId,
                            cardNumber = cardNumberDigits,
                            cardHolderName = cardHolderName.trim(),
                            totalPrice = finalTotal,
                            destination = selectedDestination.name,
                            destinationCountry = selectedDestination.country,
                            accommodationId = selectedAccommodation?.id,
                            accommodationName = selectedAccommodation?.name,
                            accommodationPrice = if (finalAccommodationPrice > 0) finalAccommodationPrice else null,
                            accommodationCurrency = selectedAccommodation?.currency,
                            checkInDate = checkInDate,
                            checkOutDate = checkOutDate
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
                enabled = selectedDestination != null && 
                    reservationState !is ReservationUiState.Loading && 
                    reservationState !is ReservationUiState.Success && run {
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
                if (reservationState is ReservationUiState.Loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text(
                        text = StringTranslator.translate(context, "Confirmer et payer"),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
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
