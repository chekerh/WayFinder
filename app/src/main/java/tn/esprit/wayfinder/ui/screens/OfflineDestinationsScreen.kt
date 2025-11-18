package tn.esprit.wayfinder.ui.screens

import android.app.Application
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import androidx.compose.ui.res.painterResource
import tn.esprit.wayfinder.R
import tn.esprit.wayfinder.data.OfflineDestinationsManager
import tn.esprit.wayfinder.models.FlightDestination
import tn.esprit.wayfinder.navigation.SELECTED_DESTINATION_KEY
import tn.esprit.wayfinder.presentation.auth.ViewModelFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OfflineDestinationsScreen(navController: NavController) {
    val context = LocalContext.current
    val offlineManager = remember { OfflineDestinationsManager(context) }
    val coroutineScope = rememberCoroutineScope()
    
    var offlineDestinations by remember { mutableStateOf<List<FlightDestination>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var showClearDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        offlineDestinations = offlineManager.getOfflineDestinations()
        isLoading = false
    }

    Scaffold(
        containerColor = Color(0xFFF0F8FF),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.CloudDone,
                            contentDescription = null,
                            tint = Color(0xFF1976D2),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Destinations hors ligne",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                    }
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
                    if (offlineDestinations.isNotEmpty()) {
                        IconButton(onClick = { showClearDialog = true }) {
                            Icon(
                                imageVector = Icons.Filled.Delete,
                                contentDescription = "Supprimer tout",
                                tint = Color.Red
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFEAF2FF)
                )
            )
        }
    ) { paddingValues ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (offlineDestinations.isEmpty()) {
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
                        imageVector = Icons.Filled.CloudOff,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = Color.Gray
                    )
                    Text(
                        "Aucune destination hors ligne",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.Gray
                    )
                    Text(
                        "Téléchargez des destinations depuis leur page de détails pour les consulter hors ligne",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray,
                        modifier = Modifier.padding(horizontal = 32.dp)
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
                item {
                    Text(
                        text = "${offlineDestinations.size} destination${if (offlineDestinations.size > 1) "s" else ""} téléchargée${if (offlineDestinations.size > 1) "s" else ""}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
                items(offlineDestinations, key = { it.id }) { destination ->
                    OfflineDestinationCard(
                        destination = destination,
                        onRemove = {
                            coroutineScope.launch {
                                offlineManager.removeFromOffline(destination.id)
                                offlineDestinations = offlineManager.getOfflineDestinations()
                            }
                        },
                        onClick = {
                            navController.currentBackStackEntry?.savedStateHandle?.set(
                                SELECTED_DESTINATION_KEY,
                                destination
                            )
                            navController.navigate("flight_detail/${destination.id}")
                        }
                    )
                }
            }
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Supprimer toutes les destinations") },
            text = { Text("Êtes-vous sûr de vouloir supprimer toutes les destinations hors ligne ?") },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            offlineManager.clearAllOffline()
                            offlineDestinations = emptyList()
                            showClearDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("Supprimer")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("Annuler")
                }
            }
        )
    }
}

@Composable
fun OfflineDestinationCard(
    destination: FlightDestination,
    onRemove: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Destination Image
            AsyncImage(
                model = destination.imageUrl ?: "",
                contentDescription = destination.name,
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop,
                placeholder = painterResource(id = R.drawable.travel_image),
                error = painterResource(id = R.drawable.travel_image)
            )

            // Destination Info
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = destination.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${destination.city ?: ""}${if (destination.city != null && destination.country.isNotEmpty()) ", " else ""}${destination.country}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
                if (destination.price != null && destination.price > 0) {
                    Text(
                        text = "${destination.price.toInt()} ${destination.currency}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF1976D2)
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.CloudDone,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = Color(0xFF4CAF50)
                    )
                    Text(
                        text = "Disponible hors ligne",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF4CAF50)
                    )
                }
            }

            // Remove Button
            IconButton(onClick = onRemove) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = "Supprimer",
                    tint = Color.Red
                )
            }
        }
    }
}

