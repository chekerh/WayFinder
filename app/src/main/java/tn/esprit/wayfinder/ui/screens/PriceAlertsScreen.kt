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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import tn.esprit.wayfinder.ui.theme.WayFinderTheme
import tn.esprit.wayfinder.models.PriceAlert
import tn.esprit.wayfinder.presentation.auth.ViewModelFactory
import tn.esprit.wayfinder.viewmodels.PriceAlertsUiState
import tn.esprit.wayfinder.viewmodels.PriceAlertsViewModel
import tn.esprit.wayfinder.utils.StringTranslator
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PriceAlertsScreen(navController: NavController) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val priceAlertsViewModel: PriceAlertsViewModel = viewModel(factory = ViewModelFactory(context.applicationContext as Application))
    val uiState by priceAlertsViewModel.uiState.collectAsState()

    var showActiveOnly by remember { mutableStateOf(true) }

    LaunchedEffect(showActiveOnly) {
        priceAlertsViewModel.loadPriceAlerts(showActiveOnly)
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { /* Navigate to create alert screen */ },
                containerColor = Color(0xFF1976D2)
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = StringTranslator.translate(context, "Ajouter une alerte"),
                    tint = Color.White
                )
            }
        },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        StringTranslator.translate(context, "Alertes de prix"),
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Retour"
                        )
                    }
                },
                actions = {
                    FilterChip(
                        selected = showActiveOnly,
                        onClick = { showActiveOnly = !showActiveOnly },
                        label = { Text(StringTranslator.translate(context, "Actives uniquement")) }
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        when (val state = uiState) {
            is PriceAlertsUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            is PriceAlertsUiState.Success -> {
                if (state.alerts.isEmpty()) {
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
                            Icon(
                                imageVector = Icons.Filled.NotificationsActive,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = Color.Gray
                            )
                            Text(
                                StringTranslator.translate(context, "Aucune alerte de prix"),
                                style = MaterialTheme.typography.titleLarge,
                                color = Color.Gray
                            )
                            Text(
                                StringTranslator.translate(context, "Créez une alerte pour être notifié lorsque le prix change"),
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Gray
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(state.alerts, key = { it.id }) { alert ->
                            PriceAlertCard(
                                alert = alert,
                                onEdit = { /* TODO: Navigate to edit */ },
                                onDelete = {
                                    priceAlertsViewModel.deletePriceAlert(alert.id) {
                                        priceAlertsViewModel.loadPriceAlerts(showActiveOnly)
                                    }
                                },
                                onToggleActive = {
                                    if (alert.isActive) {
                                        priceAlertsViewModel.deactivatePriceAlert(alert.id) {
                                            priceAlertsViewModel.loadPriceAlerts(showActiveOnly)
                                        }
                                    } else {
                                        priceAlertsViewModel.updatePriceAlert(
                                            alert.id,
                                            tn.esprit.wayfinder.models.UpdatePriceAlertRequest(isActive = true)
                                        ) {
                                            priceAlertsViewModel.loadPriceAlerts(showActiveOnly)
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
            is PriceAlertsUiState.Error -> {
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
                        Text(
                            state.message,
                            color = Color.Red,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Button(onClick = { priceAlertsViewModel.loadPriceAlerts(showActiveOnly) }) {
                            Text(StringTranslator.translate(context, "Réessayer"))
                        }
                    }
                }
            }
            else -> {}
        }
    }
}

@Composable
fun PriceAlertCard(
    alert: PriceAlert,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggleActive: () -> Unit
) {
    val context = LocalContext.current
    val itemName = alert.itemData["name"]?.toString()?.trim('"') 
        ?: alert.itemData["destination"]?.toString()?.trim('"')
        ?: alert.itemId

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (alert.isTriggered) Color(0xFFE8F5E9) else Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = getAlertTypeIcon(alert.alertType),
                        contentDescription = alert.alertType,
                        tint = if (alert.isTriggered) Color(0xFF4CAF50) else Color(0xFF1976D2),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = itemName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Type: ${alert.alertType}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                }
                if (alert.isTriggered) {
                    Badge(
                        containerColor = Color(0xFF4CAF50),
                        contentColor = Color.White
                    ) {
                        Text(StringTranslator.translate(context, "Déclenché"), fontSize = 10.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = StringTranslator.translate(context, "Prix cible"),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                    Text(
                        text = "${alert.targetPrice} ${alert.currency}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                if (alert.currentPrice != null) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = StringTranslator.translate(context, "Prix actuel"),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                        Text(
                            text = "${alert.currentPrice} ${alert.currency}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (alert.isTriggered) Color(0xFF4CAF50) else Color(0xFF1976D2)
                        )
                    }
                }
            }

            if (alert.condition == "below") {
                Spacer(modifier = Modifier.height(8.dp))
                AssistChip(
                    onClick = { },
                    label = { Text("${StringTranslator.translate(context, "Alerte si prix ≤")} ${alert.targetPrice} ${alert.currency}") },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = Color(0xFFE3F2FD)
                    )
                )
            }

            if (alert.expiresAt != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Expire le: ${formatTimestamp(alert.expiresAt)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }

            if (alert.triggeredAt != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Déclenché le: ${formatTimestamp(alert.triggeredAt)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF4CAF50),
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row {
                    IconButton(onClick = onToggleActive) {
                        Icon(
                            imageVector = if (alert.isActive) Icons.Filled.NotificationsActive else Icons.Filled.NotificationsOff,
                            contentDescription = if (alert.isActive) StringTranslator.translate(context, "Désactiver") else StringTranslator.translate(context, "Activer"),
                            tint = if (alert.isActive) Color(0xFF1976D2) else Color.Gray
                        )
                    }
                    IconButton(onClick = onEdit) {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = StringTranslator.translate(context, "Modifier"),
                            tint = Color(0xFF1976D2)
                        )
                    }
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = StringTranslator.translate(context, "Supprimer"),
                        tint = Color.Red
                    )
                }
            }
        }
    }
}

@Composable
fun getAlertTypeIcon(alertType: String): androidx.compose.ui.graphics.vector.ImageVector {
    return when (alertType.lowercase()) {
        "flight" -> Icons.Filled.Flight
        "hotel" -> Icons.Filled.Hotel
        "destination" -> Icons.Filled.Place
        "activity" -> Icons.Filled.LocalActivity
        else -> Icons.Filled.NotificationsActive
    }
}

private fun formatTimestamp(timestamp: String): String {
    return try {
        val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
        parser.timeZone = TimeZone.getTimeZone("UTC")
        val date = parser.parse(timestamp)
        val formatter = SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault())
        formatter.format(date ?: Date())
    } catch (e: Exception) {
        timestamp
    }
}

@Preview(showBackground = true)
@Composable
fun PriceAlertsScreenPreview() {
    WayFinderTheme {
        PriceAlertsScreen(rememberNavController())
    }
}

