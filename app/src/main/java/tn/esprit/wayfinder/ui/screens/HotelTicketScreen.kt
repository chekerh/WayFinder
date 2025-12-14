package tn.esprit.wayfinder.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Hotel
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import tn.esprit.wayfinder.ui.components.CustomBottomNavigationBar
import tn.esprit.wayfinder.utils.StringTranslator
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HotelTicketScreen(
    navController: NavController,
    bookingId: String? = null
) {
    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme
    
    // Get booking data from saved state
    val savedStateHandle = navController.previousBackStackEntry?.savedStateHandle
    val hotelName = savedStateHandle?.get<String>("accommodation_name") ?: "Hôtel"
    val hotelLocation = savedStateHandle?.get<String>("accommodation_location") ?: ""
    val checkInDate = savedStateHandle?.get<String>("departure_date") ?: ""
    val checkOutDate = savedStateHandle?.get<String>("return_date") ?: ""
    val guestName = savedStateHandle?.get<String>("passenger_name") ?: "Invité"
    val bookingReference = bookingId ?: "HTL-${UUID.randomUUID().toString().substring(0, 8).uppercase()}"
    
    Scaffold(
        containerColor = colorScheme.background,
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        StringTranslator.translate(context, "Confirmation d'hébergement"),
                        fontWeight = FontWeight.Bold
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { /* Share ticket */ }) {
                        Icon(Icons.Filled.Share, contentDescription = "Share")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colorScheme.surface
                )
            )
        },
        bottomBar = {
            CustomBottomNavigationBar(navController = navController)
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            
            // Ticket Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF4CAF50)),
                elevation = CardDefaults.cardElevation(8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // Booking Reference
                    Text(
                        text = bookingReference,
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    
                    HorizontalDivider(color = Color.White.copy(alpha = 0.3f))
                    
                    // Hotel Icon
                    Icon(
                        Icons.Filled.Hotel,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier
                            .size(48.dp)
                            .align(Alignment.CenterHorizontally)
                    )
                    
                    // Hotel Name
                    Text(
                        text = hotelName,
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                    
                    if (hotelLocation.isNotBlank()) {
                        Text(
                            text = hotelLocation,
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 14.sp,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                    }
                    
                    HorizontalDivider(color = Color.White.copy(alpha = 0.3f))
                    
                    // Guest Details
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TicketInfo(
                            title = StringTranslator.translate(context, "Invité"),
                            value = guestName,
                            color = Color.White
                        )
                        TicketInfo(
                            title = StringTranslator.translate(context, "Chambre"),
                            value = "101",
                            color = Color.White
                        )
                    }
                    
                    // Check-in/Check-out
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TicketInfo(
                            title = StringTranslator.translate(context, "Arrivée"),
                            value = formatDate(checkInDate),
                            color = Color.White
                        )
                        TicketInfo(
                            title = StringTranslator.translate(context, "Départ"),
                            value = formatDate(checkOutDate),
                            color = Color.White
                        )
                    }
                    
                    // Nights
                    val nights = calculateNights(checkInDate, checkOutDate)
                    if (nights > 0) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            TicketInfo(
                                title = StringTranslator.translate(context, "Nuits"),
                                value = "$nights",
                                color = Color.White
                            )
                            TicketInfo(
                                title = StringTranslator.translate(context, "Statut"),
                                value = StringTranslator.translate(context, "Confirmé"),
                                color = Color.White
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Barcode placeholder
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp)
                            .background(
                                Color.White.copy(alpha = 0.2f),
                                RoundedCornerShape(8.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "||| || ||| || |||| ||| || ||||",
                            color = Color.White,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 4.sp
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = { /* Save PDF */ },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(StringTranslator.translate(context, "Enregistrer PDF"))
                }
                Button(
                    onClick = { navController.navigate("home") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFFC107)
                    )
                ) {
                    Text(
                        StringTranslator.translate(context, "Terminé"),
                        color = Color.Black
                    )
                }
            }
        }
    }
}

private fun formatDate(dateString: String): String {
    return try {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val outputFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        val date = inputFormat.parse(dateString)
        date?.let { outputFormat.format(it) } ?: dateString
    } catch (e: Exception) {
        dateString
    }
}

private fun calculateNights(checkIn: String, checkOut: String): Int {
    return try {
        val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val checkInDate = format.parse(checkIn) ?: return 0
        val checkOutDate = format.parse(checkOut) ?: return 0
        val diff = checkOutDate.time - checkInDate.time
        (diff / (1000 * 60 * 60 * 24)).toInt()
    } catch (e: Exception) {
        0
    }
}

