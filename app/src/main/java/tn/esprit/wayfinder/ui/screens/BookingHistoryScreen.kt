package tn.esprit.wayfinder.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import android.app.Application
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import tn.esprit.wayfinder.ui.theme.WayFinderTheme
import tn.esprit.wayfinder.models.Booking
import tn.esprit.wayfinder.models.BookingStatus
import tn.esprit.wayfinder.presentation.auth.ViewModelFactory
import tn.esprit.wayfinder.ui.components.CustomBottomNavigationBar
import tn.esprit.wayfinder.viewmodels.BookingViewModel
import tn.esprit.wayfinder.viewmodels.BookingUiState
import tn.esprit.wayfinder.utils.StringTranslator
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingHistoryScreen(navController: NavController) {
    val context = LocalContext.current
    val bookingViewModel: BookingViewModel = viewModel(factory = ViewModelFactory(context.applicationContext as Application))
    val bookingState by bookingViewModel.bookingHistoryState.collectAsState()
    
    // Load booking history on first composition
    LaunchedEffect(Unit) {
        bookingViewModel.loadBookingHistory()
    }
    
    val bookings = when (val state = bookingState) {
        is BookingUiState.Success -> state.bookings
        else -> emptyList()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        StringTranslator.translate(context, "Historique des réservations"),
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = StringTranslator.translate(context, "Voici vos dernières réservations"),
                style = MaterialTheme.typography.bodyLarge,
                color = Color.Gray,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            when (val state = bookingState) {
                is BookingUiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                is BookingUiState.Error -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = state.message,
                                color = Color.Red
                            )
                            Button(onClick = { bookingViewModel.loadBookingHistory() }) {
                                Text(StringTranslator.translate(context, "Réessayer"))
                            }
                        }
                    }
                }
                is BookingUiState.Success -> {
                    // Filter out bookings with past departure dates
                    val activeBookings = remember(bookings) {
                        bookings.filter { booking ->
                            val departureDateStr = booking.tripDetails?.departureDate
                            if (departureDateStr == null || departureDateStr.isBlank()) {
                                // If no departure date, keep the booking (safer default)
                                true
                            } else {
                                try {
                                    val dateFormats = listOf(
                                        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()),
                                        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()),
                                        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.getDefault()),
                                        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
                                    )
                                    
                                    var departureDate: Date? = null
                                    for (format in dateFormats) {
                                        try {
                                            departureDate = format.parse(departureDateStr)
                                            if (departureDate != null) break
                                        } catch (e: Exception) {
                                            // Try next format
                                        }
                                    }
                                    
                                    if (departureDate != null) {
                                        val today = Calendar.getInstance().apply {
                                            set(Calendar.HOUR_OF_DAY, 0)
                                            set(Calendar.MINUTE, 0)
                                            set(Calendar.SECOND, 0)
                                            set(Calendar.MILLISECOND, 0)
                                        }.time
                                        
                                        // Keep booking if departure date is today or in the future
                                        departureDate.after(today) || departureDate.equals(today)
                                    } else {
                                        // If we can't parse the date, keep the booking
                                        true
                                    }
                                } catch (e: Exception) {
                                    // If parsing fails, keep the booking
                                    true
                                }
                            }
                        }
                    }
                    
                    if (activeBookings.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = StringTranslator.translate(context, "Aucune réservation"),
                                color = Color.Gray
                            )
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(activeBookings) { booking ->
                                BookingHistoryCard(
                                    booking = booking,
                                    onClick = {
                                        navController.navigate("booking_detail/${booking.id}")
                                    },
                                    onRebook = {
                                        // Navigate to reservation screen with the same offerId
                                        navController.navigate("booking/${booking.offerId}")
                                    }
                                )
                            }
                        }
                    }
                }
                else -> {}
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = { /* Load more */ },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF1976D2)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(StringTranslator.translate(context, "Voir plus"))
            }
            
            Spacer(modifier = Modifier.height(80.dp)) // Space for bottom nav
        }
    }
}

@Composable
fun BookingHistoryCard(booking: Booking, onClick: () -> Unit, onRebook: () -> Unit) {
    val context = LocalContext.current
    
    // Check if the departure date has NOT passed (can only rebook if date is today or in the future)
    // By default, allow rebook for cancelled bookings unless we can prove the date has passed
    val canRebook = remember(booking.tripDetails?.departureDate) {
        android.util.Log.d("BookingHistoryCard", "Checking rebook for booking ${booking.confirmationNumber}: tripDetails=${booking.tripDetails}, status=${booking.status}")
        
        val departureDateStr = booking.tripDetails?.departureDate
        
        if (departureDateStr == null || departureDateStr.isBlank()) {
            // If no departure date, allow rebooking by default (optimistic approach)
            // This is safer because many bookings don't have departureDate stored
            android.util.Log.d("BookingHistoryCard", "No departure date for booking ${booking.confirmationNumber} (tripDetails=${booking.tripDetails}), allowing rebook by default")
            true
        } else {
            try {
                // Try multiple date formats
                val dateFormats = listOf(
                    SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()),
                    SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()),
                    SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.getDefault()),
                    SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault()),
                    SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()),
                    SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()),
                    SimpleDateFormat("MM/dd/yyyy", Locale.getDefault())
                )
                
                var departureDate: Date? = null
                var usedFormat: SimpleDateFormat? = null
                for (format in dateFormats) {
                    try {
                        format.isLenient = false
                        departureDate = format.parse(departureDateStr)
                        if (departureDate != null) {
                            usedFormat = format
                            android.util.Log.d("BookingHistoryCard", "Successfully parsed date '$departureDateStr' to $departureDate using format ${format.toPattern()}")
                            break
                        }
                    } catch (e: Exception) {
                        // Try next format
                        android.util.Log.v("BookingHistoryCard", "Failed to parse '$departureDateStr' with format ${format.toPattern()}: ${e.message}")
                    }
                }
                
                if (departureDate != null) {
                    val today = Calendar.getInstance().apply {
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    
                    val departureCal = Calendar.getInstance().apply {
                        time = departureDate
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    
                    // Allow rebooking if departure date is today or in the future
                    val isTodayOrFuture = departureCal.after(today) || 
                        (departureCal.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                         departureCal.get(Calendar.MONTH) == today.get(Calendar.MONTH) &&
                         departureCal.get(Calendar.DAY_OF_MONTH) == today.get(Calendar.DAY_OF_MONTH))
                    
                    android.util.Log.d("BookingHistoryCard", "Date comparison: departure=${departureCal.time} (${departureCal.timeInMillis}), today=${today.time} (${today.timeInMillis}), isTodayOrFuture=$isTodayOrFuture")
                    isTodayOrFuture
                } else {
                    // If we can't parse the date, check if it contains a past year
                    val currentYear = Calendar.getInstance().get(Calendar.YEAR)
                    val containsPastYear = (currentYear - 10..currentYear - 1).any { year ->
                        departureDateStr.contains(year.toString())
                    }
                    
                    if (containsPastYear) {
                        android.util.Log.w("BookingHistoryCard", "Could not parse date '$departureDateStr' for booking ${booking.confirmationNumber}, but it contains a past year, cannot rebook")
                        false
                    } else {
                        // If we can't parse and it doesn't contain a past year, allow rebook by default
                        android.util.Log.w("BookingHistoryCard", "Could not parse date '$departureDateStr' for booking ${booking.confirmationNumber} with any format, allowing rebook by default")
                        true
                    }
                }
            } catch (e: Exception) {
                // If parsing fails, don't allow rebooking (safer to be conservative)
                android.util.Log.e("BookingHistoryCard", "Error parsing date '$departureDateStr' for booking ${booking.confirmationNumber}: ${e.message}", e)
                false
            }
        }
    }
    
    android.util.Log.d("BookingHistoryCard", "Final check for booking ${booking.confirmationNumber}: status=${booking.status}, canRebook=$canRebook, departureDate=${booking.tripDetails?.departureDate}, willShowButton=${booking.status == BookingStatus.CANCELLED && canRebook}")
    
    Card(
        modifier = Modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onClick),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    // Display destination name if available, otherwise use confirmation number
                    val destinationDisplay = booking.tripDetails?.destination?.takeIf { it.isNotBlank() }
                        ?: booking.confirmationNumber
                    
                    Text(
                        text = destinationDisplay,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1976D2)
                    )
                    
                    // Show confirmation number below destination if destination is available
                    if (booking.tripDetails?.destination?.isNotBlank() == true) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = booking.confirmationNumber,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = booking.bookingDate.split("T")[0], // Show only date part
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${booking.totalPrice.toInt()} EUR",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF1976D2),
                        fontWeight = FontWeight.Medium
                    )
                }
                
                StatusChip(status = booking.status)
            }
            
            // Rebook button for cancelled bookings - only show if date is NOT passed
            if (booking.status == BookingStatus.CANCELLED && canRebook) {
                android.util.Log.d("BookingHistoryCard", "Showing rebook button for cancelled booking: ${booking.confirmationNumber}, canRebook=$canRebook")
                Button(
                    onClick = onRebook,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF1976D2)
                    )
                ) {
                    Text(
                        text = StringTranslator.translate(context, "Réserver à nouveau"),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun StatusChip(status: BookingStatus) {
    val context = LocalContext.current
    val (text, color) = when (status) {
        BookingStatus.CONFIRMED -> StringTranslator.translate(context, "Confirmed") to Color(0xFF4CAF50)
        BookingStatus.PENDING -> StringTranslator.translate(context, "Pending") to Color(0xFFFF9800)
        BookingStatus.CANCELLED -> StringTranslator.translate(context, "Cancelled") to Color(0xFFF44336)
    }
    
    Box(
        modifier = Modifier
            .background(color.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = text,
            color = color,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold
        )
    }
}

@Preview(showBackground = true)
@Composable
fun BookingHistoryScreenPreview() {
    WayFinderTheme {
        BookingHistoryScreen(rememberNavController())
    }
}
