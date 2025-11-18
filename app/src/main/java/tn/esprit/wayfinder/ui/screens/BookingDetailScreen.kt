package tn.esprit.wayfinder.ui.screens

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import tn.esprit.wayfinder.models.BookingStatus
import tn.esprit.wayfinder.presentation.auth.ViewModelFactory
import tn.esprit.wayfinder.ui.components.CustomBottomNavigationBar
import tn.esprit.wayfinder.viewmodels.BookingViewModel
import tn.esprit.wayfinder.viewmodels.ReservationUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingDetailScreen(navController: NavController, bookingId: String) {
    val context = LocalContext.current
    val bookingViewModel: BookingViewModel = viewModel(factory = ViewModelFactory(context.applicationContext as Application))
    val bookingState by bookingViewModel.singleBookingState.collectAsState()
    var showCancelDialog by remember { mutableStateOf(false) }

    LaunchedEffect(bookingId) {
        bookingViewModel.loadBooking(bookingId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Détails de la réservation", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    when (val state = bookingState) {
                        is ReservationUiState.Success -> {
                            if (state.booking.status != BookingStatus.CANCELLED) {
                                IconButton(onClick = { showCancelDialog = true }) {
                                    Icon(Icons.Filled.Delete, contentDescription = "Cancel", tint = Color.Red)
                                }
                            }
                        }
                        else -> {}
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
        when (val state = bookingState) {
            is ReservationUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            is ReservationUiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(text = state.message, color = Color.Red)
                        Button(onClick = { bookingViewModel.loadBooking(bookingId) }) {
                            Text("Réessayer")
                        }
                    }
                }
            }
            is ReservationUiState.Success -> {
                BookingDetailContent(
                    booking = state.booking,
                    onCancel = {
                        bookingViewModel.cancelBooking(bookingId)
                        showCancelDialog = false
                    },
                    modifier = Modifier.padding(paddingValues)
                )
                
                if (showCancelDialog) {
                    AlertDialog(
                        onDismissRequest = { showCancelDialog = false },
                        title = { Text("Annuler la réservation") },
                        text = { Text("Êtes-vous sûr de vouloir annuler cette réservation ?") },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    bookingViewModel.cancelBooking(bookingId)
                                    showCancelDialog = false
                                }
                            ) {
                                Text("Annuler", color = Color.Red)
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showCancelDialog = false }) {
                                Text("Non")
                            }
                        }
                    )
                }
            }
            else -> {}
        }
    }
}

@Composable
fun BookingDetailContent(
    booking: tn.esprit.wayfinder.models.Booking,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Status Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = when (booking.status) {
                    BookingStatus.CONFIRMED -> Color(0xFF4CAF50).copy(alpha = 0.1f)
                    BookingStatus.PENDING -> Color(0xFFFF9800).copy(alpha = 0.1f)
                    BookingStatus.CANCELLED -> Color(0xFFF44336).copy(alpha = 0.1f)
                }
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Statut",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                    Text(
                        text = when (booking.status) {
                            BookingStatus.CONFIRMED -> "Confirmé"
                            BookingStatus.PENDING -> "En attente"
                            BookingStatus.CANCELLED -> "Annulé"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                StatusChip(status = booking.status)
            }
        }

        // Confirmation Number
        InfoCard(
            title = "Numéro de confirmation",
            value = booking.confirmationNumber
        )

        // Trip Details
        booking.tripDetails?.let { trip ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Détails du voyage",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    trip.origin?.let {
                        InfoRow("Origine", it)
                    }
                    trip.destination?.let {
                        InfoRow("Destination", it)
                    }
                    trip.departureDate?.let {
                        InfoRow("Date de départ", it)
                    }
                    trip.returnDate?.let {
                        InfoRow("Date de retour", it)
                    }
                    trip.travelClass?.let {
                        InfoRow("Classe", it)
                    }
                }
            }
        }

        // Passengers
        if (!booking.passengers.isNullOrEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Passagers",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    booking.passengers.forEach { passenger ->
                        InfoRow("Nom", passenger.fullName)
                        passenger.travelerType?.let {
                            InfoRow("Type", it)
                        }
                    }
                }
            }
        }

        // Price
        InfoCard(
            title = "Prix total",
            value = "${booking.totalPrice} EUR"
        )

        // Notes
        booking.notes?.let { notes ->
            if (notes.isNotBlank()) {
                InfoCard(
                    title = "Notes",
                    value = notes
                )
            }
        }

        // Booking Date
        InfoCard(
            title = "Date de réservation",
            value = booking.bookingDate
        )

        Spacer(modifier = Modifier.height(80.dp))
    }
}

@Composable
fun InfoCard(title: String, value: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

