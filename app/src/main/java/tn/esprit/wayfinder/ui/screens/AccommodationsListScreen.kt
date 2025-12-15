package tn.esprit.wayfinder.ui.screens

import android.app.Application
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import tn.esprit.wayfinder.R
import tn.esprit.wayfinder.models.FlightDestination
import tn.esprit.wayfinder.models.Accommodation
import tn.esprit.wayfinder.models.Hotel
import tn.esprit.wayfinder.navigation.SELECTED_DESTINATION_KEY
import tn.esprit.wayfinder.presentation.auth.ViewModelFactory
import tn.esprit.wayfinder.ui.components.CustomBottomNavigationBar
import tn.esprit.wayfinder.utils.StringTranslator
import tn.esprit.wayfinder.viewmodels.HotelsViewModel
import tn.esprit.wayfinder.viewmodels.HotelsUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccommodationsListScreen(
    navController: NavController,
    destinationId: String,
    accommodationType: String
) {
    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme
    
    // Get HotelsViewModel
    val hotelsViewModel: HotelsViewModel = viewModel(
        factory = ViewModelFactory(context.applicationContext as Application)
    )
    val hotelsState by hotelsViewModel.hotelsState.collectAsStateWithLifecycle()
    
    val destination = remember(destinationId) {
        navController.previousBackStackEntry
            ?.savedStateHandle
            ?.get<FlightDestination>(SELECTED_DESTINATION_KEY)
    }
    
    // Get dates from saved state (set in LodgingChoiceScreen)
    val savedStateHandle = navController.previousBackStackEntry?.savedStateHandle
    val checkInDate = savedStateHandle?.get<String>("check_in_date")
    val checkOutDate = savedStateHandle?.get<String>("check_out_date")
    
    // Load hotels from API when screen opens
    LaunchedEffect(accommodationType, destination, checkInDate, checkOutDate) {
        val cityCode = destination?.let { 
            // Try to get city code from destination
            val cityName = it.city ?: it.name ?: ""
            if (cityName.isNotBlank()) {
                hotelsViewModel.getCityCode(cityName)
            } else {
                "PAR" // Default to Paris
            }
        } ?: "PAR" // Default to Paris
        
        // Map accommodation types to trip types, or use null if it's not a valid trip type
        // Valid trip types: business, honeymoon, family, adventure, leisure, solo, wellness, backpacking
        val validTripTypes = setOf("business", "honeymoon", "family", "adventure", "leisure", "solo", "wellness", "backpacking")
        val tripType = if (accommodationType in validTripTypes) {
            accommodationType
        } else {
            // For accommodation types (hotel, airbnb, hostel, resort, apartment), don't filter by tripType
            null
        }
        
        android.util.Log.d("AccommodationsListScreen", "Searching hotels for cityCode: $cityCode, accommodationType: $accommodationType, tripType: $tripType, checkIn: $checkInDate, checkOut: $checkOutDate")
        
        hotelsViewModel.searchHotels(
            cityCode = cityCode,
            tripType = tripType,
            accommodationType = accommodationType,
            checkInDate = checkInDate,
            checkOutDate = checkOutDate,
            limit = 20
        )
    }
    
    val typeName = when (accommodationType) {
        "hotel" -> "Hôtels"
        "airbnb" -> "Airbnbs"
        "hostel" -> "Auberges"
        "resort" -> "Résorts"
        "apartment" -> "Appartements"
        "business" -> "Hôtels d'affaires"
        "honeymoon" -> "Hôtels romantiques"
        "family" -> "Hôtels famille"
        "adventure" -> "Éco-lodges"
        "wellness" -> "Spa & Bien-être"
        "backpacking" -> "Auberges & Hostels"
        else -> "Logements"
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                    Text(
                            typeName,
                        fontWeight = FontWeight.Bold
                    ) 
                        Text(
                            destination?.name ?: destinationId,
                            fontSize = 14.sp,
                            color = colorScheme.onSurfaceVariant
                        )
                    }
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
        when (val state = hotelsState) {
            is HotelsUiState.Loading -> {
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
                        CircularProgressIndicator()
                        Text(
                            text = StringTranslator.translate(context, "Recherche des meilleurs hébergements..."),
                            color = colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            is HotelsUiState.Error -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Error,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = state.message,
                        color = colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = {
                        val cityCode = destination?.let { 
                            hotelsViewModel.getCityCode(it.city ?: it.name)
                        } ?: "PAR"
                        val validTripTypes = setOf("business", "honeymoon", "family", "adventure", "leisure", "solo", "wellness", "backpacking")
                        val tripType = if (accommodationType in validTripTypes) accommodationType else null
                        hotelsViewModel.searchHotels(cityCode = cityCode, tripType = tripType, accommodationType = accommodationType, limit = 20)
                    }) {
                        Text(StringTranslator.translate(context, "Réessayer"))
                    }
                }
            }
            
            is HotelsUiState.Success -> {
                if (state.hotels.isEmpty()) {
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
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = StringTranslator.translate(context, "Essayez une autre destination ou un autre type de logement"),
                    fontSize = 14.sp,
                    color = colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = {
                            val cityCode = destination?.let { 
                                hotelsViewModel.getCityCode(it.city ?: it.name)
                            } ?: "PAR"
                            val validTripTypes = setOf("business", "honeymoon", "family", "adventure", "leisure", "solo", "wellness", "backpacking")
                            val tripType = if (accommodationType in validTripTypes) accommodationType else null
                            hotelsViewModel.searchHotels(
                                cityCode = cityCode, 
                                tripType = tripType, 
                                accommodationType = accommodationType,
                                checkInDate = checkInDate,
                                checkOutDate = checkOutDate,
                                limit = 20
                            )
                        }
                    ) {
                        Text(StringTranslator.translate(context, "Réessayer"))
                    }
            }
        } else {
                    Column(modifier = Modifier.padding(paddingValues)) {
                        // Source indicator
                        if (state.source == "fallback") {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                color = colorScheme.tertiaryContainer,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        Icons.Filled.Info,
                                        contentDescription = null,
                                        tint = colorScheme.onTertiaryContainer,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = StringTranslator.translate(context, "Données de démonstration"),
                                        fontSize = 12.sp,
                                        color = colorScheme.onTertiaryContainer
                                    )
                                }
                            }
                        }
                        
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                            items(state.hotels, key = { it.hotelId }) { hotel ->
                                HotelCard(
                                    hotel = hotel,
                        onClick = {
                                        // Save hotel selection and navigate
                            navController.currentBackStackEntry
                                ?.savedStateHandle
                                ?.apply {
                                                set("accommodation_id", hotel.hotelId)
                                                set("accommodation_price", hotel.pricePerNight ?: 0.0)
                                                set("accommodation_currency", hotel.currency ?: "EUR")
                                                set("accommodation_name", hotel.name)
                                                set("accommodation_type", hotel.type ?: "hotel")
                                                set("accommodation_location", hotel.address?.cityName ?: "")
                                                set("accommodation_rating", hotel.googleRating ?: hotel.rating ?: 0.0)
                                                set("accommodation_image_url", hotel.media?.firstOrNull()?.uri ?: "")
                                }
                                        // Navigate to hotel detail screen first
                                        navController.navigate("hotel_detail/${hotel.hotelId}")
                        }
                    )
                }
            }
        }
    }
}

            else -> {
                // Idle state - show nothing or skeleton
            }
        }
    }
}

/**
 * Card for displaying Hotel data from API
 */
@Composable
fun HotelCard(
    hotel: Hotel,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme
    
    // Get the best image URL
    val imageUrl = hotel.media?.firstOrNull()?.uri 
        ?: "https://picsum.photos/400/200?random=${hotel.hotelId.hashCode()}"
    
    // Get rating (prefer Google rating, fallback to hotel rating)
    val rating = hotel.googleRating ?: hotel.rating ?: 0.0
    val reviewCount = hotel.googleReviewCount ?: 0
    
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
                    model = imageUrl,
                    contentDescription = hotel.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    placeholder = painterResource(id = R.drawable.europe),
                    error = painterResource(id = R.drawable.europe)
                )
                
                // Star rating (hotel stars)
                hotel.rating?.let { stars ->
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(12.dp),
                        shape = RoundedCornerShape(8.dp),
                        color = colorScheme.surface.copy(alpha = 0.9f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            repeat(stars.toInt().coerceIn(1, 5)) {
                                Icon(
                                    imageVector = Icons.Filled.Star,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = colorScheme.primary
                                )
                            }
                        }
                    }
                }
                
                // Google rating badge
                if (rating > 0) {
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
                                text = String.format("%.1f", rating),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = colorScheme.onPrimaryContainer
                            )
                            if (reviewCount > 0) {
                                Text(
                                    text = "($reviewCount)",
                                    fontSize = 12.sp,
                                    color = colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                )
                            }
                        }
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
                    text = hotel.name,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.onSurface,
                    maxLines = 2
                )
                
                // Location
                val location = hotel.address?.let { addr ->
                    listOfNotNull(addr.cityName, addr.countryCode).joinToString(", ")
                } ?: hotel.cityCode ?: ""
                
                if (location.isNotBlank()) {
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
                            text = location,
                            fontSize = 14.sp,
                            color = colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                // Amenities
                if (hotel.amenities.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        hotel.amenities.take(3).forEach { amenity ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = colorScheme.secondaryContainer
                            ) {
                                Text(
                                    text = amenity.replace("_", " ").lowercase()
                                        .replaceFirstChar { it.uppercase() },
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    color = colorScheme.onSecondaryContainer,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                // Price and Book button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        hotel.pricePerNight?.let { price ->
                            Text(
                                text = "${price.toInt()} ${hotel.currency ?: "EUR"}",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = colorScheme.primary
                            )
                            Text(
                                text = StringTranslator.translate(context, "par nuit"),
                                fontSize = 12.sp,
                                color = colorScheme.onSurfaceVariant
                            )
                        } ?: Text(
                            text = StringTranslator.translate(context, "Prix sur demande"),
                            fontSize = 16.sp,
                            color = colorScheme.onSurfaceVariant
                        )
                    }
                    Button(
                        onClick = onClick,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colorScheme.primary
                        )
                    ) {
                        Text(StringTranslator.translate(context, "Choisir"))
                    }
                }
            }
        }
    }
}

/**
 * Legacy card for Accommodation objects (backward compatibility)
 */
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

