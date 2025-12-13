package tn.esprit.wayfinder.ui.screens

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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import tn.esprit.wayfinder.R
import tn.esprit.wayfinder.models.FlightDestination
import tn.esprit.wayfinder.models.Accommodation
import tn.esprit.wayfinder.navigation.SELECTED_DESTINATION_KEY
import tn.esprit.wayfinder.ui.components.CustomBottomNavigationBar
import tn.esprit.wayfinder.utils.StringTranslator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccommodationsListScreen(
    navController: NavController,
    destinationId: String,
    accommodationType: String
) {
    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme
    
    val destination = remember(destinationId) {
        navController.previousBackStackEntry
            ?.savedStateHandle
            ?.get<FlightDestination>(SELECTED_DESTINATION_KEY)
    }
    
    // Mock accommodations data - in real app, fetch from API
    val accommodations = remember(accommodationType, destination) {
        generateMockAccommodations(accommodationType, destination?.name ?: destinationId)
    }
    
    val typeName = when (accommodationType) {
        "hotel" -> "Hôtels"
        "airbnb" -> "Airbnbs"
        "hostel" -> "Auberges"
        "resort" -> "Résorts"
        "apartment" -> "Appartements"
        else -> "Logements"
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "$typeName - ${destination?.name ?: destinationId}",
                        fontWeight = FontWeight.Bold
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colorScheme.surface
                )
            )
        },
        bottomBar = {
            CustomBottomNavigationBar(navController = navController)
        },
        containerColor = colorScheme.background
    ) { paddingValues ->
        if (accommodations.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Hotel,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = StringTranslator.translate(context, "Aucun logement disponible pour le moment"),
                    color = colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(accommodations, key = { it.id }) { accommodation ->
                    AccommodationCard(
                        accommodation = accommodation,
                        onClick = {
                            // Save accommodation selection and navigate to upsell screen
                            navController.currentBackStackEntry
                                ?.savedStateHandle
                                ?.apply {
                                    // Only save primitive types - Accommodation object cannot be saved to SavedStateHandle
                                    set("accommodation_id", accommodation.id)
                                    set("accommodation_price", accommodation.price)
                                    set("accommodation_currency", accommodation.currency)
                                    set("accommodation_name", accommodation.name)
                                    set("accommodation_type", accommodation.type)
                                    set("accommodation_location", accommodation.location)
                                    set("accommodation_rating", accommodation.rating)
                                    set("accommodation_image_url", accommodation.imageUrl ?: "")
                                }
                            // Navigate to upsell screen (will be created)
                            navController.navigate("upsells/${destinationId}")
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun AccommodationCard(
    accommodation: Accommodation,
    onClick: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Image
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            ) {
                AsyncImage(
                    model = accommodation.imageUrl ?: "https://via.placeholder.com/400x200",
                    contentDescription = accommodation.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    placeholder = painterResource(id = R.drawable.europe),
                    error = painterResource(id = R.drawable.europe)
                )
                // Rating badge
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = colorScheme.primaryContainer
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Star,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = colorScheme.primary
                        )
                        Text(
                            text = String.format("%.1f", accommodation.rating),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
            
            // Details
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = accommodation.name,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.onSurface
                )
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.LocationOn,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = accommodation.location,
                        fontSize = 14.sp,
                        color = colorScheme.onSurfaceVariant
                    )
                }
                
                // Amenities
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    accommodation.amenities.take(3).forEach { amenity ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = colorScheme.secondaryContainer
                        ) {
                            Text(
                                text = amenity,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                color = colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                // Price
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${accommodation.price.toInt()} ${accommodation.currency} / nuit",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.primary
                    )
                    Button(
                        onClick = onClick,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colorScheme.primary
                        )
                    ) {
                        Text("Réserver")
                    }
                }
            }
        }
    }
}

fun generateMockAccommodations(type: String, destination: String): List<Accommodation> {
    val basePrice = when (type) {
        "hostel" -> 30.0
        "apartment" -> 60.0
        "hotel" -> 100.0
        "airbnb" -> 80.0
        "resort" -> 200.0
        else -> 100.0
    }
    
    val amenities = when (type) {
        "hotel" -> listOf("Wi-Fi", "Petit-déjeuner", "Spa", "Gym")
        "airbnb" -> listOf("Wi-Fi", "Cuisine", "Lave-linge", "Parking")
        "hostel" -> listOf("Wi-Fi", "Cuisine commune", "Salle commune")
        "resort" -> listOf("Wi-Fi", "Piscine", "Spa", "Restaurant", "Plage")
        "apartment" -> listOf("Wi-Fi", "Cuisine", "Lave-linge", "Parking", "Balcon")
        else -> listOf("Wi-Fi", "Parking")
    }
    
    return (1..5).map { index ->
        Accommodation(
            id = "${type}_$index",
            name = when (type) {
                "hotel" -> "Hôtel ${destination} $index"
                "airbnb" -> "Appartement cosy $index"
                "hostel" -> "Auberge ${destination} $index"
                "resort" -> "Résort ${destination} $index"
                "apartment" -> "Appartement moderne $index"
                else -> "Logement $index"
            },
            type = type,
            price = basePrice + (index * 20),
            currency = "EUR",
            rating = 4.0 + (index * 0.2),
            imageUrl = "https://picsum.photos/400/200?random=${type.hashCode() + index}",
            location = "$destination, Centre-ville",
            amenities = amenities
        )
    }
}

