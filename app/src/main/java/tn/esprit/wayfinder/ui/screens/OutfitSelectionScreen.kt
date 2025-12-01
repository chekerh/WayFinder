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
import androidx.compose.material.icons.filled.CameraAlt
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
import androidx.compose.ui.text.style.TextOverflow
import tn.esprit.wayfinder.models.Booking
import tn.esprit.wayfinder.models.BookingStatus
import tn.esprit.wayfinder.presentation.auth.ViewModelFactory
import tn.esprit.wayfinder.ui.components.CustomBottomNavigationBar
import tn.esprit.wayfinder.viewmodels.BookingViewModel
import tn.esprit.wayfinder.viewmodels.BookingUiState
import tn.esprit.wayfinder.utils.StringTranslator
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OutfitSelectionScreen(navController: NavController) {
    val context = LocalContext.current
    val bookingViewModel: BookingViewModel = viewModel(factory = ViewModelFactory(context.applicationContext as Application))
    val bookingState by bookingViewModel.bookingHistoryState.collectAsState()
    
    // Load booking history on first composition
    LaunchedEffect(Unit) {
        bookingViewModel.loadBookingHistory()
    }
    
    // Filter only confirmed bookings
    val confirmedBookings = when (val state = bookingState) {
        is BookingUiState.Success -> state.bookings.filter { it.status == BookingStatus.CONFIRMED }
        else -> emptyList()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        StringTranslator.translate(context, "Vérifier ma tenue"),
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
                text = StringTranslator.translate(context, "Sélectionnez une réservation pour vérifier votre tenue"),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                                color = MaterialTheme.colorScheme.error
                            )
                            Button(onClick = { bookingViewModel.loadBookingHistory() }) {
                                Text(StringTranslator.translate(context, "Réessayer"))
                            }
                        }
                    }
                }
                is BookingUiState.Success -> {
                    if (confirmedBookings.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.CameraAlt,
                                    contentDescription = null,
                                    modifier = Modifier.size(64.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = StringTranslator.translate(context, "Aucune réservation confirmée"),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = StringTranslator.translate(context, "Vous devez avoir une réservation confirmée pour vérifier votre tenue"),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 32.dp)
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(confirmedBookings) { booking ->
                                OutfitBookingCard(
                                    booking = booking,
                                    onClick = {
                                        navController.navigate("outfit_upload/${booking.id}")
                                    }
                                )
                            }
                        }
                    }
                }
                else -> {}
            }
            
            Spacer(modifier = Modifier.height(80.dp)) // Space for bottom nav
        }
    }
}

@Composable
fun OutfitBookingCard(
    booking: Booking,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val destination = booking.tripDetails?.destination ?: StringTranslator.translate(context, "Destination inconnue")
    val departureDate = booking.tripDetails?.departureDate
    val formattedDate = departureDate?.let {
        try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val outputFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
            val date = inputFormat.parse(it)
            date?.let { outputFormat.format(it) } ?: it
        } catch (e: Exception) {
            it
        }
    } ?: StringTranslator.translate(context, "Date non spécifiée")
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = destination,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = StringTranslator.translate(context, "Départ") + ": $formattedDate",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = StringTranslator.translate(context, "Confirmation") + ": ${booking.confirmationNumber}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Icon(
                imageVector = Icons.Filled.CameraAlt,
                contentDescription = StringTranslator.translate(context, "Vérifier la tenue"),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
        }
    }
}

