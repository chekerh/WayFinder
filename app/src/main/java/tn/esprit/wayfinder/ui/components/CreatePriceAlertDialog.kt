package tn.esprit.wayfinder.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.serialization.json.JsonPrimitive
import tn.esprit.wayfinder.models.CreatePriceAlertRequest
import tn.esprit.wayfinder.models.FlightDestination
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun CreatePriceAlertDialog(
    destination: FlightDestination?,
    currentPrice: Double?,
    currency: String = "EUR",
    onDismiss: () -> Unit,
    onCreate: (CreatePriceAlertRequest) -> Unit
) {
    var targetPriceText by remember { mutableStateOf("") }
    var condition by remember { mutableStateOf("below") }
    var showError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.NotificationsActive,
                    contentDescription = null,
                    tint = Color(0xFF1976D2),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Créer une alerte de prix",
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (destination != null) {
                    Text(
                        text = "Destination: ${destination.name}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }

                if (currentPrice != null) {
                    Text(
                        text = "Prix actuel: $currentPrice $currency",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                }

                OutlinedTextField(
                    value = targetPriceText,
                    onValueChange = {
                        targetPriceText = it
                        showError = false
                    },
                    label = { Text("Prix cible ($currency)") },
                    placeholder = { Text("Ex: ${currentPrice?.let { (it * 0.9).toInt() } ?: "500"}") },
                    isError = showError,
                    supportingText = if (showError) {
                        { Text(errorMessage, color = Color.Red) }
                    } else null,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "Condition",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = condition == "below",
                        onClick = { condition = "below" },
                        label = { Text("Prix en dessous") }
                    )
                    FilterChip(
                        selected = condition == "above",
                        onClick = { condition = "above" },
                        label = { Text("Prix au-dessus") }
                    )
                }

                Text(
                    text = if (condition == "below") {
                        "Vous serez notifié lorsque le prix sera ≤ $targetPriceText $currency"
                    } else {
                        "Vous serez notifié lorsque le prix sera ≥ $targetPriceText $currency"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val targetPrice = targetPriceText.toDoubleOrNull()
                    if (targetPrice == null || targetPrice <= 0) {
                        showError = true
                        errorMessage = "Veuillez entrer un prix valide"
                        return@Button
                    }

                    val itemData = destination?.let {
                        mapOf(
                            "name" to JsonPrimitive(it.name),
                            "destination" to JsonPrimitive(it.city),
                            "country" to JsonPrimitive(it.country),
                            "price" to JsonPrimitive(it.price ?: 0.0),
                            "currency" to JsonPrimitive(it.currency)
                        )
                    } ?: emptyMap()

                    val request = CreatePriceAlertRequest(
                        alertType = "flight",
                        itemId = destination?.id ?: "",
                        itemData = itemData,
                        targetPrice = targetPrice,
                        currency = currency,
                        condition = condition,
                        currentPrice = currentPrice,
                        sendNotification = true
                    )

                    onCreate(request)
                    onDismiss()
                }
            ) {
                Text("Créer l'alerte")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler")
            }
        }
    )
}

