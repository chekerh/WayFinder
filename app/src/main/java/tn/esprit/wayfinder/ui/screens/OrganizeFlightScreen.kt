package tn.esprit.wayfinder.ui.screens

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
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
import tn.esprit.wayfinder.models.FlightDestination
import tn.esprit.wayfinder.models.FlightOffer
import tn.esprit.wayfinder.models.CreateGroupFlightRequest
import tn.esprit.wayfinder.models.SharedHotelRequest
import tn.esprit.wayfinder.navigation.SELECTED_DESTINATION_KEY
import tn.esprit.wayfinder.presentation.auth.ViewModelFactory
import tn.esprit.wayfinder.ui.theme.WayFinderTheme
import tn.esprit.wayfinder.utils.CommissionCalculator
import tn.esprit.wayfinder.utils.SharedCostCalculator
import tn.esprit.wayfinder.utils.StringTranslator
import tn.esprit.wayfinder.viewmodels.GroupFlightViewModel
import tn.esprit.wayfinder.viewmodels.CreateGroupFlightUiState
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrganizeFlightScreen(
    navController: NavController,
    destinationId: String
) {
    val context = LocalContext.current
    val groupFlightViewModel: GroupFlightViewModel = viewModel(
        factory = ViewModelFactory(context.applicationContext as Application)
    )
    val createState by groupFlightViewModel.createUiState.collectAsState()
    
    val destination = remember(destinationId) {
        navController.previousBackStackEntry
            ?.savedStateHandle
            ?.get<FlightDestination>(SELECTED_DESTINATION_KEY)
    }
    
    val selectedFlightOffer = remember(destinationId) {
        navController.previousBackStackEntry
            ?.savedStateHandle
            ?.get<FlightOffer>("selected_flight_offer")
    }
    
    var maxMembers by remember { mutableStateOf(4) }
    var includeSharedHotel by remember { mutableStateOf(false) }
    var hotelName by remember { mutableStateOf("") }
    var hotelRoomType by remember { mutableStateOf("double") }
    var hotelTotalCost by remember { mutableStateOf("") }
    var checkInDate by remember { mutableStateOf("") }
    var checkOutDate by remember { mutableStateOf("") }
    var nights by remember { mutableStateOf(1) }
    
    // Calculate costs
    val costBreakdown = remember(selectedFlightOffer, includeSharedHotel, hotelTotalCost, maxMembers) {
        selectedFlightOffer?.let { offer ->
            val hotel = if (includeSharedHotel && hotelTotalCost.isNotBlank()) {
                SharedHotelRequest(
                    name = hotelName.ifEmpty { "Shared Hotel" },
                    roomType = hotelRoomType,
                    totalCost = hotelTotalCost.toDoubleOrNull() ?: 0.0,
                    checkIn = checkInDate,
                    checkOut = checkOutDate,
                    nights = nights
                )
            } else null
            
            SharedCostCalculator.calculateGroupCosts(
                flightOffer = offer,
                sharedHotel = hotel?.let {
                    tn.esprit.wayfinder.models.SharedHotel(
                        hotelId = null,
                        name = it.name,
                        roomType = it.roomType,
                        totalCost = it.totalCost,
                        costPerPerson = SharedCostCalculator.calculateHotelCostPerPerson(
                            it.totalCost,
                            it.roomType,
                            maxMembers
                        ),
                        checkIn = it.checkIn,
                        checkOut = it.checkOut,
                        nights = it.nights
                    )
                },
                memberCount = maxMembers
            )
        }
    }
    
    // Handle success navigation
    LaunchedEffect(createState) {
        if (createState is CreateGroupFlightUiState.Success) {
            val groupFlight = (createState as CreateGroupFlightUiState.Success).groupFlight
            navController.navigate("group_flight_detail/${groupFlight.id}") {
                popUpTo("flight_detail/$destinationId") { inclusive = false }
            }
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        StringTranslator.translate(context, "Organiser un vol de groupe"),
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Flight Info Card
            destination?.let { dest ->
                selectedFlightOffer?.let { offer ->
                    FlightInfoCard(
                        destination = dest,
                        flightOffer = offer
                    )
                }
            }
            
            // Group Size Selection
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = StringTranslator.translate(context, "Taille du groupe"),
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { if (maxMembers > 2) maxMembers-- },
                            enabled = maxMembers > 2
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = "Decrease")
                        }
                        Text(
                            text = "$maxMembers ${StringTranslator.translate(context, "personnes")}",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        IconButton(
                            onClick = { if (maxMembers < 8) maxMembers++ }
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Increase")
                        }
                    }
                }
            }
            
            // Shared Hotel Option
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = StringTranslator.translate(context, "Hôtel partagé"),
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                            Text(
                                text = StringTranslator.translate(context, "Réduisez les coûts en partageant une chambre"),
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = includeSharedHotel,
                            onCheckedChange = { includeSharedHotel = it }
                        )
                    }
                    
                    if (includeSharedHotel) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedTextField(
                                value = hotelName,
                                onValueChange = { hotelName = it },
                                label = { Text(StringTranslator.translate(context, "Nom de l'hôtel")) },
                                modifier = Modifier.fillMaxWidth()
                            )
                            
                            var expanded by remember { mutableStateOf(false) }
                            ExposedDropdownMenuBox(
                                expanded = expanded,
                                onExpandedChange = { expanded = !expanded }
                            ) {
                                OutlinedTextField(
                                    value = hotelRoomType,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text(StringTranslator.translate(context, "Type de chambre")) },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .menuAnchor()
                                )
                                ExposedDropdownMenu(
                                    expanded = expanded,
                                    onDismissRequest = { expanded = false }
                                ) {
                                    listOf("single", "double", "triple", "quad").forEach { type ->
                                        DropdownMenuItem(
                                            text = { Text(StringTranslator.translate(context, type)) },
                                            onClick = {
                                                hotelRoomType = type
                                                expanded = false
                                            }
                                        )
                                    }
                                }
                            }
                            
                            OutlinedTextField(
                                value = hotelTotalCost.toString(),
                                onValueChange = { hotelTotalCost = it },
                                label = { Text(StringTranslator.translate(context, "Coût total de l'hôtel")) },
                                modifier = Modifier.fillMaxWidth(),
                                prefix = { Text("${destination?.currency ?: "EUR"} ") }
                            )
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                OutlinedTextField(
                                    value = checkInDate,
                                    onValueChange = { checkInDate = it },
                                    label = { Text(StringTranslator.translate(context, "Check-in (YYYY-MM-DD)")) },
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = checkOutDate,
                                    onValueChange = { checkOutDate = it },
                                    label = { Text(StringTranslator.translate(context, "Check-out (YYYY-MM-DD)")) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }
            
            // Cost Breakdown
            costBreakdown?.let { breakdown ->
                CostBreakdownCard(breakdown = breakdown)
            }
            
            // Error message
            if (createState is CreateGroupFlightUiState.Error) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = (createState as CreateGroupFlightUiState.Error).message,
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
            
            // Create Button
            Button(
                onClick = {
                    destination?.let { dest ->
                        selectedFlightOffer?.let { offer ->
                            val request = CreateGroupFlightRequest(
                                destinationId = dest.id,
                                flightOfferId = offer.id,
                                maxMembers = maxMembers,
                                departureDate = dest.departureDate,
                                returnDate = dest.arrivalDate,
                                sharedHotel = if (includeSharedHotel && hotelTotalCost.isNotBlank()) {
                                    SharedHotelRequest(
                                        name = hotelName.ifEmpty { "Shared Hotel" },
                                        roomType = hotelRoomType,
                                        totalCost = hotelTotalCost.toDoubleOrNull() ?: 0.0,
                                        checkIn = checkInDate,
                                        checkOut = checkOutDate,
                                        nights = nights
                                    )
                                } else null,
                                inviteUserIds = emptyList() // Can be added later
                            )
                            groupFlightViewModel.createGroupFlight(request)
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = createState !is CreateGroupFlightUiState.Loading && destination != null && selectedFlightOffer != null,
                shape = RoundedCornerShape(16.dp)
            ) {
                if (createState is CreateGroupFlightUiState.Loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text(
                        text = StringTranslator.translate(context, "Créer le vol de groupe"),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun FlightInfoCard(
    destination: FlightDestination,
    flightOffer: FlightOffer
) {
    val context = LocalContext.current
    val price = flightOffer.price?.total?.toDoubleOrNull() ?: 0.0
    val currency = flightOffer.price?.currency ?: destination.currency
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = destination.name,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )
            Text(
                text = "${destination.city}, ${destination.country}",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = StringTranslator.translate(context, "Prix du vol"),
                    fontSize = 14.sp
                )
                Text(
                    text = String.format("%.2f %s", price, currency),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }
    }
}

@Composable
fun CostBreakdownCard(breakdown: SharedCostCalculator.GroupCostBreakdown) {
    val context = LocalContext.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = StringTranslator.translate(context, "Répartition des coûts"),
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
            
            HorizontalDivider()
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = StringTranslator.translate(context, "Vol par personne"),
                    fontSize = 14.sp
                )
                Text(
                    text = String.format("%.2f %s", breakdown.flightCostPerPerson, breakdown.currency),
                    fontSize = 14.sp
                )
            }
            
            if (breakdown.hotelCostPerPerson > 0) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = StringTranslator.translate(context, "Hôtel par personne"),
                        fontSize = 14.sp
                    )
                    Text(
                        text = String.format("%.2f %s", breakdown.hotelCostPerPerson, breakdown.currency),
                        fontSize = 14.sp
                    )
                }
            }
            
            HorizontalDivider()
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = StringTranslator.translate(context, "Total par personne"),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Text(
                    text = String.format("%.2f %s", breakdown.totalWithSharing, breakdown.currency),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            if (breakdown.savingsPerPerson > 0) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF4CAF50).copy(alpha = 0.1f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = StringTranslator.translate(context, "Économies par personne"),
                                fontSize = 14.sp,
                                color = Color(0xFF4CAF50)
                            )
                            Text(
                                text = String.format("%.2f %s", breakdown.savingsPerPerson, breakdown.currency),
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Color(0xFF4CAF50)
                            )
                        }
                        Text(
                            text = StringTranslator.translate(context, "Total économisé: %.2f %s").format(breakdown.totalSavings, breakdown.currency),
                            fontSize = 12.sp,
                            color = Color(0xFF4CAF50)
                        )
                    }
                }
            }
        }
    }
}

