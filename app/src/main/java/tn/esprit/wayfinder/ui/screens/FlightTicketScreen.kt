package tn.esprit.wayfinder.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Flight
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
import tn.esprit.wayfinder.models.FlightDestination
import tn.esprit.wayfinder.navigation.SELECTED_DESTINATION_KEY
import tn.esprit.wayfinder.ui.components.CustomBottomNavigationBar
import tn.esprit.wayfinder.utils.StringTranslator
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlightTicketScreen(
    navController: NavController,
    bookingId: String? = null
) {
    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme
    
    // Get booking data from saved state
    val savedStateHandle = navController.previousBackStackEntry?.savedStateHandle
    val destination = savedStateHandle?.get<FlightDestination>(SELECTED_DESTINATION_KEY)
    val departureDate = savedStateHandle?.get<String>("departure_date") ?: ""
    val returnDate = savedStateHandle?.get<String>("return_date") ?: ""
    val passengerName = savedStateHandle?.get<String>("passenger_name") ?: "Passager"
    val bookingReference = bookingId ?: "WF-${UUID.randomUUID().toString().substring(0, 8).uppercase()}"
    
    Scaffold(
        containerColor = colorScheme.background,
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        StringTranslator.translate(context, "Billet d'avion"),
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
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1976D2)),
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
                    
                    // Flight Icon
                    Icon(
                        Icons.Filled.Flight,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier
                            .size(48.dp)
                            .align(Alignment.CenterHorizontally)
                    )
                    
                    // Route
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(horizontalAlignment = Alignment.Start) {
                            Text(
                                text = "TUN", // Origin code - can be extracted from destination if available
                                color = Color.White,
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Tunis", // Origin city
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 14.sp
                            )
                        }
                        
                        Icon(
                            Icons.Filled.Flight,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                        
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = destination?.city?.take(3)?.uppercase() ?: "PAR",
                                color = Color.White,
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = destination?.name ?: "Paris",
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 14.sp
                            )
                        }
                    }
                    
                    HorizontalDivider(color = Color.White.copy(alpha = 0.3f))
                    
                    // Flight Details
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        FlightTicketInfo(
                            title = StringTranslator.translate(context, "Passager"),
                            value = passengerName,
                            color = Color.White
                        )
                        FlightTicketInfo(
                            title = StringTranslator.translate(context, "Date"),
                            value = formatDate(departureDate),
                            color = Color.White
                        )
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        FlightTicketInfo(
                            title = StringTranslator.translate(context, "Heure"),
                            value = destination?.departureDate?.substringAfter("T")?.substringBefore(":")?.let { 
                                "${it.substring(0, 2)}:${it.substring(2, 4)}"
                            } ?: "12:30",
                            color = Color.White
                        )
                        FlightTicketInfo(
                            title = StringTranslator.translate(context, "Porte"),
                            value = "A12",
                            color = Color.White
                        )
                    }
                    
                    if (returnDate.isNotBlank()) {
                        HorizontalDivider(color = Color.White.copy(alpha = 0.3f))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            FlightTicketInfo(
                                title = StringTranslator.translate(context, "Retour"),
                                value = formatDate(returnDate),
                                color = Color.White
                            )
                            FlightTicketInfo(
                                title = StringTranslator.translate(context, "Compagnie"),
                                value = destination?.airline ?: "Airline",
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

@Composable
fun FlightTicketInfo(title: String, value: String, color: Color) {
    Column {
        Text(
            text = title.uppercase(),
            color = color.copy(alpha = 0.7f),
            fontSize = 12.sp
        )
        Text(
            text = value,
            color = color,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
        )
    }
}

private fun formatDate(dateString: String): String {
    return try {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val outputFormat = SimpleDateFormat("dd MMM yy", Locale.getDefault())
        val date = inputFormat.parse(dateString)
        date?.let { outputFormat.format(it) } ?: dateString
    } catch (e: Exception) {
        dateString
    }
}

