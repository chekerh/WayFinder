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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import tn.esprit.wayfinder.models.*
import tn.esprit.wayfinder.navigation.SELECTED_DESTINATION_KEY
import tn.esprit.wayfinder.presentation.auth.ViewModelFactory
import tn.esprit.wayfinder.ui.components.CustomBottomNavigationBar
import tn.esprit.wayfinder.utils.StringTranslator
import tn.esprit.wayfinder.viewmodels.UpsellViewModel
import tn.esprit.wayfinder.viewmodels.UpsellUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpsellScreen(navController: NavController, destinationId: String) {
    val context = LocalContext.current
    val upsellViewModel: UpsellViewModel = viewModel(factory = ViewModelFactory(context.applicationContext as Application))
    val uiState by upsellViewModel.uiState.collectAsState()
    val selectedUpsells by upsellViewModel.selectedUpsells.collectAsState()
    
    val destination = remember(destinationId) {
        navController.previousBackStackEntry
            ?.savedStateHandle
            ?.get<FlightDestination>(SELECTED_DESTINATION_KEY)
    }
    
    // Get user preferences from TokenManager
    val tokenManager = remember { tn.esprit.wayfinder.manager.TokenManager(context) }
    val currentUser = remember { tokenManager.getUser() }
    val userPreferences = remember(currentUser) {
        currentUser?.onboardingPreferences
    }
    
    // Extract destination city from destination name
    val destinationCity = remember(destination) {
        destination?.name?.split(",")?.firstOrNull()?.trim() ?: destination?.name
    }
    
    LaunchedEffect(destinationId, destinationCity, userPreferences) {
        upsellViewModel.loadUpsellProducts(
            destinationId = destinationId,
            destinationCity = destinationCity,
            userPreferences = userPreferences
        )
    }
    
    val totalUpsellPrice = upsellViewModel.getTotalUpsellPrice()
    val currency = destination?.currency ?: "EUR"
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        StringTranslator.translate(context, "Services additionnels"),
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
            Column {
                if (selectedUpsells.isNotEmpty()) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shadowElevation = 8.dp
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
                                    text = StringTranslator.translate(context, "Total services"),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = String.format("%.2f %s", totalUpsellPrice, currency),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            Button(
                                onClick = {
                                    // Save selected upsells and navigate to payment
                                    navController.currentBackStackEntry
                                        ?.savedStateHandle
                                        ?.set("selected_upsells", selectedUpsells)
                                    navController.navigate("booking/${destinationId}")
                                },
                                modifier = Modifier.height(48.dp)
                            ) {
                                Text(StringTranslator.translate(context, "Continuer"))
                            }
                        }
                    }
                }
                CustomBottomNavigationBar(navController = navController)
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        when (val state = uiState) {
            is UpsellUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            is UpsellUiState.Error -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = state.message,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { upsellViewModel.loadUpsellProducts(destinationId) }) {
                        Text(StringTranslator.translate(context, "Réessayer"))
                    }
                }
            }
            is UpsellUiState.Success -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Text(
                            text = StringTranslator.translate(context, "Améliorez votre voyage avec nos services"),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                        Text(
                            text = StringTranslator.translate(context, "Sélectionnez les services que vous souhaitez ajouter à votre réservation"),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                    }
                    
                    items(state.products) { product ->
                        UpsellProductCard(
                            product = product,
                            isSelected = upsellViewModel.isUpsellSelected(product.id),
                            onToggle = { upsellViewModel.toggleUpsell(product) }
                        )
                    }
                    
                    item {
                        Spacer(modifier = Modifier.height(80.dp)) // Space for bottom bar
                    }
                }
            }
            else -> {}
        }
    }
}

@Composable
private fun UpsellProductCard(
    product: UpsellProduct,
    isSelected: Boolean,
    onToggle: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) 
                colorScheme.primaryContainer 
            else 
                colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 4.dp else 2.dp
        ),
        border = if (isSelected) {
            androidx.compose.foundation.BorderStroke(2.dp, colorScheme.primary)
        } else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Icon based on category
            val icon = when (product.category) {
                UpsellCategory.TRAVEL_INSURANCE -> Icons.Filled.Shield
                UpsellCategory.AIRPORT_TRANSFER -> Icons.Filled.DirectionsCar
                UpsellCategory.CAR_RENTAL -> Icons.Filled.DriveEta
                UpsellCategory.ACTIVITY -> Icons.Filled.LocalActivity
                UpsellCategory.BAGGAGE_INSURANCE -> Icons.Filled.Luggage
                UpsellCategory.SEAT_SELECTION -> Icons.Filled.EventSeat
                UpsellCategory.LOUNGE_ACCESS -> Icons.Filled.BusinessCenter
                UpsellCategory.WIFI_DATA -> Icons.Filled.Wifi
            }
            
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = if (isSelected) colorScheme.onPrimaryContainer else colorScheme.primary
            )
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = product.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) 
                                    colorScheme.onPrimaryContainer 
                                else 
                                    colorScheme.onSurface
                            )
                            if (product.recommended) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = Color(0xFFFFC107)
                                ) {
                                    Text(
                                        text = StringTranslator.translate(LocalContext.current, "Recommandé"),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        color = Color.Black
                                    )
                                }
                            }
                        }
                        Text(
                            text = product.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isSelected) 
                                colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            else 
                                colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                    
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = String.format("%.2f %s", product.price, product.currency),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) 
                                colorScheme.onPrimaryContainer 
                            else 
                                colorScheme.primary
                        )
                        if (isSelected) {
                            Text(
                                text = StringTranslator.translate(LocalContext.current, "Sélectionné"),
                                fontSize = 12.sp,
                                color = colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
                
                // Features
                if (product.features.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        product.features.take(3).forEach { feature ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) 
                                    colorScheme.primary.copy(alpha = 0.2f)
                                else 
                                    colorScheme.secondaryContainer
                            ) {
                                Text(
                                    text = feature,
                                    fontSize = 10.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    color = if (isSelected) 
                                        colorScheme.onPrimaryContainer 
                                    else 
                                        colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }
                }
            }
            
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onToggle() },
                colors = CheckboxDefaults.colors(
                    checkedColor = colorScheme.primary
                )
            )
        }
    }
}

