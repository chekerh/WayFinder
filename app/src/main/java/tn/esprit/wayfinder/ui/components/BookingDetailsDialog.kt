package tn.esprit.wayfinder.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.FlightClass
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import tn.esprit.wayfinder.utils.StringTranslator
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingDetailsDialog(
    destinationName: String,
    onDismiss: () -> Unit,
    onConfirm: (BookingDetails) -> Unit
) {
    val context = LocalContext.current
    var departureDate by remember { mutableStateOf("") }
    var returnDate by remember { mutableStateOf("") }
    var numberOfPassengers by remember { mutableStateOf(1) }
    var travelClass by remember { mutableStateOf("ECONOMY") }
    var specialRequests by remember { mutableStateOf("") }
    
    val travelClasses = listOf("ECONOMY", "PREMIUM_ECONOMY", "BUSINESS", "FIRST")
    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val minDate = Calendar.getInstance().apply { add(Calendar.DAY_OF_MONTH, 1) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = StringTranslator.translate(context, "Détails de réservation"),
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = StringTranslator.translate(context, "Destination: $destinationName"),
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary
                )
                
                HorizontalDivider()
                
                // Departure Date
                OutlinedTextField(
                    value = departureDate,
                    onValueChange = { departureDate = it },
                    label = { Text(StringTranslator.translate(context, "Date de départ")) },
                    placeholder = { Text("YYYY-MM-DD") },
                    leadingIcon = { Icon(Icons.Filled.CalendarToday, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                // Return Date
                OutlinedTextField(
                    value = returnDate,
                    onValueChange = { returnDate = it },
                    label = { Text(StringTranslator.translate(context, "Date de retour (optionnel)")) },
                    placeholder = { Text("YYYY-MM-DD") },
                    leadingIcon = { Icon(Icons.Filled.CalendarToday, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                // Number of Passengers
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                ) {
                    Text(
                        text = StringTranslator.translate(context, "Nombre de passagers"),
                        modifier = Modifier.weight(1f)
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { if (numberOfPassengers > 1) numberOfPassengers-- },
                            enabled = numberOfPassengers > 1
                        ) {
                            Text("-", fontSize = 20.sp)
                        }
                        Text(
                            text = "$numberOfPassengers",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                        IconButton(
                            onClick = { if (numberOfPassengers < 9) numberOfPassengers++ },
                            enabled = numberOfPassengers < 9
                        ) {
                            Text("+", fontSize = 20.sp)
                        }
                    }
                }
                
                // Travel Class
                Text(
                    text = StringTranslator.translate(context, "Classe de voyage"),
                    fontWeight = FontWeight.Medium
                )
                travelClasses.forEach { className ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        RadioButton(
                            selected = travelClass == className,
                            onClick = { travelClass = className }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = StringTranslator.translate(context, className.replace("_", " ")),
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
                
                // Special Requests
                OutlinedTextField(
                    value = specialRequests,
                    onValueChange = { specialRequests = it },
                    label = { Text(StringTranslator.translate(context, "Demandes spéciales (optionnel)")) },
                    placeholder = { Text(StringTranslator.translate(context, "Ex: Siège fenêtre, repas végétarien...")) },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (departureDate.isNotBlank()) {
                        onConfirm(
                            BookingDetails(
                                departureDate = departureDate,
                                returnDate = returnDate.takeIf { it.isNotBlank() },
                                numberOfPassengers = numberOfPassengers,
                                travelClass = travelClass,
                                specialRequests = specialRequests.takeIf { it.isNotBlank() }
                            )
                        )
                    }
                },
                enabled = departureDate.isNotBlank()
            ) {
                Text(StringTranslator.translate(context, "Continuer"))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(StringTranslator.translate(context, "Annuler"))
            }
        }
    )
}

data class BookingDetails(
    val departureDate: String,
    val returnDate: String? = null,
    val numberOfPassengers: Int = 1,
    val travelClass: String = "ECONOMY",
    val specialRequests: String? = null
)

