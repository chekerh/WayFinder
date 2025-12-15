package tn.esprit.wayfinder.ui.screens

import android.app.Application
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Group
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.delay
import tn.esprit.wayfinder.manager.LanguageManager
import tn.esprit.wayfinder.models.FlightDestination
import tn.esprit.wayfinder.models.Accommodation
import tn.esprit.wayfinder.models.SelectedUpsell
import tn.esprit.wayfinder.navigation.BOOKING_CARD_CVV_KEY
import tn.esprit.wayfinder.navigation.BOOKING_CARD_EXPIRY_KEY
import tn.esprit.wayfinder.navigation.BOOKING_CARD_NAME_KEY
import tn.esprit.wayfinder.navigation.BOOKING_CARD_NUMBER_KEY
import tn.esprit.wayfinder.navigation.BOOKING_CURRENCY_KEY
import tn.esprit.wayfinder.navigation.BOOKING_DESTINATION_NAME_KEY
import tn.esprit.wayfinder.navigation.BOOKING_TOTAL_KEY
import tn.esprit.wayfinder.navigation.SELECTED_DESTINATION_KEY
import tn.esprit.wayfinder.presentation.auth.ViewModelFactory
import tn.esprit.wayfinder.ui.components.CustomBottomNavigationBar
import tn.esprit.wayfinder.ui.theme.WayFinderTheme
import tn.esprit.wayfinder.utils.NotificationHelper
import tn.esprit.wayfinder.utils.StringTranslator
import tn.esprit.wayfinder.utils.CommissionCalculator
import tn.esprit.wayfinder.viewmodels.BookingViewModel
import tn.esprit.wayfinder.viewmodels.NotificationsViewModel
import tn.esprit.wayfinder.viewmodels.ReservationUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewBookingScreen(navController: NavController, destinationId: String) {
    val context = LocalContext.current
    val bookingViewModel: BookingViewModel = viewModel(factory = ViewModelFactory(context.applicationContext as Application))
    val notificationsViewModel: NotificationsViewModel = viewModel(factory = ViewModelFactory(context.applicationContext as Application))
    val reservationState by bookingViewModel.reservationState.collectAsStateWithLifecycle()

    val selectedDestination = remember(destinationId) {
        navController.previousBackStackEntry
            ?.savedStateHandle
            ?.get<FlightDestination>(SELECTED_DESTINATION_KEY)
    }
    val savedStateHandle = navController.previousBackStackEntry?.savedStateHandle
    val totalPrice = savedStateHandle?.get<Double>(BOOKING_TOTAL_KEY) ?: 0.0
    val currency = savedStateHandle?.get<String>(BOOKING_CURRENCY_KEY) ?: "EUR"
    val cardNumber = savedStateHandle?.get<String>(BOOKING_CARD_NUMBER_KEY)
    val cardHolder = savedStateHandle?.get<String>(BOOKING_CARD_NAME_KEY)
    val cardExpiry = savedStateHandle?.get<String>(BOOKING_CARD_EXPIRY_KEY)
    val cardCvv = savedStateHandle?.get<String>(BOOKING_CARD_CVV_KEY)
    val groupFlightId = savedStateHandle?.get<String>("group_flight_id")
    
    // Get accommodation and upsells
    // Reconstruct Accommodation from primitive fields (SavedStateHandle doesn't support complex objects)
    val selectedAccommodation = remember {
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
    val selectedUpsells = savedStateHandle?.get<List<SelectedUpsell>>("selected_upsells") ?: emptyList()
    val accommodationPrice = savedStateHandle?.get<Double>("accommodation_price") ?: 0.0
    val upsellTotal = savedStateHandle?.get<Double>("upsell_total") ?: 0.0
    val checkInDate = savedStateHandle?.get<String>("check_in_date")
    val checkOutDate = savedStateHandle?.get<String>("check_out_date")

    LaunchedEffect(reservationState) {
        val currentState = reservationState
        when (currentState) {
            is ReservationUiState.Success -> {
                val booking = currentState.booking
            navController.currentBackStackEntry?.savedStateHandle?.set(BOOKING_TOTAL_KEY, booking.totalPrice)
            navController.currentBackStackEntry?.savedStateHandle?.set(
                BOOKING_DESTINATION_NAME_KEY,
                selectedDestination?.name
            )
            navController.currentBackStackEntry?.savedStateHandle?.set(
                BOOKING_CURRENCY_KEY,
                selectedDestination?.currency ?: currency
            )
            // Save accommodation and upsells for ticket generation (only primitive types)
            // Accommodation already saved as primitive fields, no need to save object again
            if (selectedUpsells.isNotEmpty()) {
                navController.currentBackStackEntry?.savedStateHandle?.set("booking_upsells", selectedUpsells)
            }
            // Save trip details for ticket generation
            booking.tripDetails?.let {
                navController.currentBackStackEntry?.savedStateHandle?.set("booking_trip_details", it)
            }
            // Save passengers for ticket generation
            booking.passengers?.let {
                navController.currentBackStackEntry?.savedStateHandle?.set("booking_passengers", it)
            }

            val languageManager = LanguageManager(context)
            val currentLanguage = languageManager.getLanguage()
            android.util.Log.d("ReviewBookingScreen", "Language: $currentLanguage")

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

            // Navigate to flight ticket screen
            navController.currentBackStackEntry?.savedStateHandle?.set("booking_id", booking.confirmationNumber)
            navController.navigate("flight_ticket/${booking.confirmationNumber}") {
                popUpTo("home") { inclusive = false }
            }
            
            // If accommodation exists, also navigate to hotel ticket
            val accommodationId = savedStateHandle?.get<String>("accommodation_id")
            if (accommodationId != null) {
                // Save hotel booking ID (can be same as flight booking or separate)
                navController.currentBackStackEntry?.savedStateHandle?.set("hotel_booking_id", booking.confirmationNumber)
            }
            bookingViewModel.resetReservationState()
            }
            is ReservationUiState.Error -> {
                // Error is handled in UI below
                android.util.Log.e("ReviewBookingScreen", "Booking confirmation error: ${currentState.message}")
            }
            else -> {
                // Loading or Idle state - no action needed
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(StringTranslator.translate(context, "Revoir la réservation"), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    TextButton(onClick = { navController.popBackStack() }) {
                        Text(StringTranslator.translate(context, "Retour"))
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
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            val destination = selectedDestination
            if (destination == null || cardNumber == null || cardHolder == null) {
                Text(
                    text = StringTranslator.translate(context, "Impossible de charger les détails de la réservation."),
                    color = MaterialTheme.colorScheme.error
                )
                return@Column
            }

            // Check if this is a group flight booking
            val isGroupFlight = groupFlightId != null
            
            // Calculate commission breakdown - base price is flight only, accommodation and upsells are separate
            val flightBasePrice = totalPrice - accommodationPrice - upsellTotal
            val priceBreakdown = remember(totalPrice, isGroupFlight, accommodationPrice, upsellTotal) {
                if (isGroupFlight) {
                    // For group flights, the price already includes shared costs
                    CommissionCalculator.calculateBreakdown(
                        basePrice = flightBasePrice,
                        bookingType = "flight"
                    )
                } else {
                    CommissionCalculator.calculateBreakdown(
                        basePrice = flightBasePrice,
                        bookingType = "flight"
                    )
                }
            }
            
            // Calculate commission for accommodation and upsells
            val accommodationCommission = remember(accommodationPrice) {
                if (accommodationPrice > 0) {
                    CommissionCalculator.calculateCommission(
                        basePrice = accommodationPrice,
                        bookingType = "hotel"
                    ).commission
                } else {
                    0.0
                }
            }
            
            val upsellCommission = remember(upsellTotal) {
                if (upsellTotal > 0) {
                    CommissionCalculator.calculateCommission(
                        basePrice = upsellTotal,
                        bookingType = "other"
                    ).commission
                } else {
                    0.0
                }
            }
            
            // Calculate total commission (flight + accommodation + upsells)
            val totalCommission = priceBreakdown.commission + accommodationCommission + upsellCommission
            
            // Calculate final total including accommodation, upsells, and all commissions
            val finalTotal = priceBreakdown.basePrice + accommodationPrice + upsellTotal + totalCommission
            
            SummaryCard(
                destination = destination,
                priceBreakdown = priceBreakdown,
                currency = currency,
                isGroupFlight = isGroupFlight,
                accommodation = selectedAccommodation,
                accommodationPrice = accommodationPrice,
                accommodationCommission = accommodationCommission,
                upsells = selectedUpsells,
                upsellTotal = upsellTotal,
                upsellCommission = upsellCommission,
                totalCommission = totalCommission,
                finalTotal = finalTotal
            )

            PaymentCard(
                cardHolder = cardHolder,
                cardNumber = cardNumber,
                expiry = cardExpiry ?: ""
            )

            // Display error message if booking confirmation failed
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

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = {
                    // Prevent multiple clicks
                    if (reservationState !is ReservationUiState.Loading && reservationState !is ReservationUiState.Success) {
                    // Use final total price with commission, accommodation, and upsells
                    // For hotel-only bookings, use hotel ID as offerId; otherwise use destinationId
                    val bookingOfferId = if (selectedAccommodation != null && accommodationPrice > 0 && (totalPrice - accommodationPrice - upsellTotal) <= 0) {
                        // Hotel-only booking (no flight)
                        selectedAccommodation.id
                    } else {
                        // Flight booking (with or without hotel)
                        destinationId
                    }
                    
                    bookingViewModel.confirmBooking(
                        offerId = bookingOfferId,
                        cardNumber = cardNumber,
                        cardHolderName = cardHolder,
                        totalPrice = finalTotal, // Total includes flight, accommodation, upsells, and commission
                        destination = destination.name,
                        destinationCountry = destination.country,
                        accommodationId = selectedAccommodation?.id,
                        accommodationName = selectedAccommodation?.name,
                        accommodationPrice = if (accommodationPrice > 0) accommodationPrice else null,
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
                enabled = reservationState !is ReservationUiState.Loading && reservationState !is ReservationUiState.Success
            ) {
                if (reservationState is ReservationUiState.Loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text(
                        text = StringTranslator.translate(context, "Confirmer et payer"),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            TextButton(
                onClick = { navController.popBackStack() },
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text(StringTranslator.translate(context, "Modifier les informations"))
            }
        }
    }
}

@Composable
private fun SummaryCard(
    destination: FlightDestination,
    priceBreakdown: tn.esprit.wayfinder.utils.PriceBreakdown,
    currency: String,
    isGroupFlight: Boolean = false,
    accommodation: Accommodation? = null,
    accommodationPrice: Double = 0.0,
    accommodationCommission: Double = 0.0,
    upsells: List<SelectedUpsell> = emptyList(),
    upsellTotal: Double = 0.0,
    upsellCommission: Double = 0.0,
    totalCommission: Double = 0.0,
    finalTotal: Double
) {
    val context = LocalContext.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(text = destination.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(
                        text = destination.country,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    if (isGroupFlight) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                            modifier = Modifier.padding(vertical = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Group,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = StringTranslator.translate(context, "Réservation de groupe - Coûts partagés"),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                    
                    HorizontalDivider()
            
            // Price breakdown
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = StringTranslator.translate(context, "Prix de base (vol)"),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = String.format("%.2f %s", priceBreakdown.basePrice, currency),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Accommodation
            if (accommodationPrice > 0 && accommodation != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = StringTranslator.translate(context, "Hébergement"),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = accommodation.name,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                    Text(
                        text = String.format("%.2f %s", accommodationPrice, currency),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (accommodationCommission > 0) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = StringTranslator.translate(context, "Commission hébergement"),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                        Text(
                            text = String.format("%.2f %s", accommodationCommission, currency),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            }
            
            // Upsells
            if (upsellTotal > 0 && upsells.isNotEmpty()) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    upsells.forEach { upsell ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = StringTranslator.translate(context, "Service additionnel"),
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = String.format("%.2f %s", upsell.price * upsell.quantity, upsell.currency),
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = StringTranslator.translate(context, "Total services"),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = String.format("%.2f %s", upsellTotal, currency),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (upsellCommission > 0) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = StringTranslator.translate(context, "Commission services"),
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                            Text(
                                text = String.format("%.2f %s", upsellCommission, currency),
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = StringTranslator.translate(context, "Commission WayFinder (${priceBreakdown.commissionPercentage}%)"),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = String.format("%.2f %s", priceBreakdown.commission, currency),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Show total commission if there are accommodation or upsells
            if ((accommodationCommission > 0 || upsellCommission > 0) && totalCommission > priceBreakdown.commission) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = StringTranslator.translate(context, "Commission totale"),
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = String.format("%.2f %s", totalCommission, currency),
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            HorizontalDivider()
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = StringTranslator.translate(context, "Total"),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = String.format("%.2f %s", finalTotal, currency),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun PaymentCard(
    cardHolder: String,
    cardNumber: String,
    expiry: String
) {
    val context = LocalContext.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(text = StringTranslator.translate(context, "Méthode de paiement"), fontWeight = FontWeight.Bold)
            Text(text = cardHolder)
            Text(text = maskCard(cardNumber))
            if (expiry.isNotBlank()) {
                Text(text = StringTranslator.translate(context, "Expiration") + ": $expiry")
            }
        }
    }
}

private fun maskCard(card: String): String {
    val digits = card.filter { it.isDigit() }
    if (digits.length < 4) return "****"
    val last4 = digits.takeLast(4)
    return "**** **** **** $last4"
}

@Preview(showBackground = true)
@Composable
fun ReviewBookingScreenPreview() {
    WayFinderTheme {
        ReviewBookingScreen(rememberNavController(), destinationId = "test-destination-id")
    }
}