package tn.esprit.wayfinder.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import tn.esprit.wayfinder.ui.theme.WayFinderTheme
import java.util.Locale
import tn.esprit.wayfinder.navigation.BOOKING_CURRENCY_KEY
import tn.esprit.wayfinder.navigation.BOOKING_DESTINATION_NAME_KEY
import tn.esprit.wayfinder.navigation.BOOKING_TOTAL_KEY
import tn.esprit.wayfinder.ui.components.CustomBottomNavigationBar
import tn.esprit.wayfinder.utils.StringTranslator
import androidx.compose.ui.platform.LocalContext
import android.util.Log
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingConfirmationScreen(navController: NavController, bookingId: String) {
    val context = LocalContext.current
    val previousEntry = navController.previousBackStackEntry
    
    // Debug log
    LaunchedEffect(bookingId) {
        Log.d("BookingConfirmation", "Booking ID: $bookingId")
    }
    val bookingTotal = previousEntry?.savedStateHandle?.get<Double>(BOOKING_TOTAL_KEY)
    val bookingCurrency = previousEntry?.savedStateHandle?.get<String>(BOOKING_CURRENCY_KEY) ?: "EUR"
    val bookingDestination = previousEntry?.savedStateHandle?.get<String>(BOOKING_DESTINATION_NAME_KEY)

    DisposableEffect(Unit) {
        onDispose {
            previousEntry?.savedStateHandle?.remove<Double>(BOOKING_TOTAL_KEY)
            previousEntry?.savedStateHandle?.remove<String>(BOOKING_CURRENCY_KEY)
            previousEntry?.savedStateHandle?.remove<String>(BOOKING_DESTINATION_NAME_KEY)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(StringTranslator.translate(context, "Confirmation"), fontWeight = FontWeight.Bold) },
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
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Spacer(modifier = Modifier.height(40.dp))
            
            // Success Icon
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = "Success",
                modifier = Modifier.size(120.dp),
                tint = Color(0xFF4CAF50)
            )
            
            Text(
                text = StringTranslator.translate(context, "Réservation confirmée!"),
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF4CAF50)
            )
            
            Text(
                text = StringTranslator.translate(context, "Votre réservation a été confirmée avec succès. Vous recevrez un email de confirmation sous peu."),
                fontSize = 16.sp,
                color = Color.Gray,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            
            // Booking Details Card
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
                        text = StringTranslator.translate(context, "Détails de la réservation"),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    
                    HorizontalDivider()
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(StringTranslator.translate(context, "Numéro de confirmation"), color = Color.Gray)
                        Text(bookingId, fontWeight = FontWeight.Medium)
                    }

                    bookingDestination?.let {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(StringTranslator.translate(context, "Destination"), color = Color.Gray)
                            Text(it, fontWeight = FontWeight.Medium)
                        }
                    }
                    
                    bookingTotal?.let {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(StringTranslator.translate(context, "Total payé"), color = Color.Gray)
                            Text(
                                String.format(Locale.getDefault(), "%.2f %s", it, bookingCurrency),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(StringTranslator.translate(context, "Statut"), color = Color.Gray)
                        Text(
                            text = StringTranslator.translate(context, "Confirmé"),
                            color = Color(0xFF4CAF50),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // Outfit Weather Button - Navigate to outfit_selection (same as profile)
            Button(
                onClick = {
                    Log.d("BookingConfirmation", "Vérifier ma tenue clicked, navigating to outfit_selection")
                    try {
                        navController.navigate("outfit_selection") {
                            launchSingleTop = true
                        }
                    } catch (e: Exception) {
                        Log.e("BookingConfirmation", "Navigation error", e)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF4CAF50)
                ),
                enabled = bookingId.isNotEmpty()
            ) {
                Text(
                    text = StringTranslator.translate(context, "Vérifier ma tenue"),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            
            // Action Buttons
            Button(
                onClick = {
                    navController.navigate("booking_history") {
                        popUpTo("home") { inclusive = false }
                    }
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
                    text = StringTranslator.translate(context, "Voir mes réservations"),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            
            OutlinedButton(
                onClick = {
                    navController.navigate("home") {
                        popUpTo("home") { inclusive = true }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = StringTranslator.translate(context, "Retour à l'accueil"),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun BookingConfirmationScreenPreview() {
    WayFinderTheme {
        BookingConfirmationScreen(rememberNavController(), bookingId = "test-booking-id")
    }
}

