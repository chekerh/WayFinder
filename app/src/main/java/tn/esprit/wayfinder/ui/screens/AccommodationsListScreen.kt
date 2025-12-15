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
import androidx.compose.ui.graphics.Color
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
    
    // Track selected hotel (matching iOS behavior)
    var selectedHotel by remember { mutableStateOf<Hotel?>(null) }
    
    val destination = remember(destinationId) {
        navController.previousBackStackEntry
            ?.savedStateHandle
            ?.get<FlightDestination>(SELECTED_DESTINATION_KEY)
    }
    
    // Get dates from saved state (set in LodgingChoiceScreen)
    val savedStateHandle = navController.previousBackStackEntry?.savedStateHandle
    val checkInDate = savedStateHandle?.get<String>("check_in_date")
    val checkOutDate = savedStateHandle?.get<String>("check_out_date")
    
    // Ensure the selected destination is also stored on this back stack entry
    // so that downstream screens like ReservationScreen can reliably retrieve it
    LaunchedEffect(destinationId, destination) {
        destination?.let {
            navController.currentBackStackEntry
                ?.savedStateHandle
                ?.set(SELECTED_DESTINATION_KEY, it)
        }
    }
    
    // Load hotels from API when screen opens
    LaunchedEffect(accommodationType, destination, checkInDate, checkOutDate) {
        val cityName = destination?.let { 
            // Use city name directly from destination
            it.city ?: it.name ?: ""
        } ?: ""
        
        // Map accommodation types to trip types, or use null if it's not a valid trip type
        // Valid trip types: business, honeymoon, family, adventure, leisure, solo, wellness, backpacking
        val validTripTypes = setOf("business", "honeymoon", "family", "adventure", "leisure", "solo", "wellness", "backpacking")
        val tripType = if (accommodationType in validTripTypes) {
            accommodationType
        } else {
            // For accommodation types (hotel, airbnb, hostel, resort, apartment), don't filter by tripType
            null
        }
        
        android.util.Log.d("AccommodationsListScreen", "Searching hotels for cityName: $cityName, accommodationType: $accommodationType, tripType: $tripType, checkIn: $checkInDate, checkOut: $checkOutDate")
        
        hotelsViewModel.searchHotels(
            cityName = cityName.takeIf { it.isNotBlank() },
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
                        val cityName = destination?.let { 
                            it.city ?: it.name
                        } ?: ""
                        val validTripTypes = setOf("business", "honeymoon", "family", "adventure", "leisure", "solo", "wellness", "backpacking")
                        val tripType = if (accommodationType in validTripTypes) accommodationType else null
                        hotelsViewModel.searchHotels(cityName = cityName.takeIf { it.isNotBlank() }, tripType = tripType, accommodationType = accommodationType, limit = 20)
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
                            val cityName = destination?.let { 
                                it.city ?: it.name
                            } ?: ""
                            val validTripTypes = setOf("business", "honeymoon", "family", "adventure", "leisure", "solo", "wellness", "backpacking")
                            val tripType = if (accommodationType in validTripTypes) accommodationType else null
                            hotelsViewModel.searchHotels(
                                cityName = cityName.takeIf { it.isNotBlank() }, 
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
                                    isSelected = selectedHotel?.hotelId == hotel.hotelId,
                                    onClick = {
                                        // Select hotel and navigate directly to booking so user can reserve
                                        selectedHotel = hotel

                                        navController.currentBackStackEntry
                                            ?.savedStateHandle
                                            ?.apply {
                                                // Save individual fields (SavedStateHandle can't store complex objects)
                                                set("accommodation_id", hotel.hotelId)
                                                set("accommodation_price", hotel.pricePerNight ?: 0.0)
                                                set("accommodation_currency", hotel.currency ?: "EUR")
                                                set("accommodation_name", hotel.name)
                                                set("accommodation_type", hotel.type ?: "hotel")
                                                set("accommodation_location", hotel.address?.cityName ?: "")
                                                set("accommodation_rating", hotel.googleRating ?: hotel.rating ?: 0.0)
                                                set("accommodation_image_url", hotel.media?.firstOrNull()?.uri ?: "")
                                                // Save additional hotel fields for reconstruction
                                                set("hotel_id", hotel.id)
                                                set("hotel_city_code", hotel.cityCode ?: "")
                                                set("hotel_description", hotel.description ?: "")
                                                set("hotel_amenities", hotel.amenities.joinToString(","))
                                                set("hotel_media_uris", hotel.media?.map { it.uri }?.joinToString(",") ?: "")
                                                set("hotel_address_lines", hotel.address?.lines?.joinToString("|") ?: "")
                                                set("hotel_address_city", hotel.address?.cityName ?: "")
                                                set("hotel_address_country", hotel.address?.countryCode ?: "")
                                                set("hotel_address_postal", hotel.address?.postalCode ?: "")
                                                set("hotel_google_rating", hotel.googleRating ?: 0.0)
                                                set("hotel_google_review_count", hotel.googleReviewCount ?: 0)
                                                set("hotel_google_place_id", hotel.googlePlaceId ?: "")
                                            }

                                        navController.navigate("booking/$destinationId")
                                    }
                                )
                            }
                        }
                        
                        // Bottom continue button (matching iOS)
                        if (state.hotels.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = {
                                    // Save selected hotel and navigate to ReservationScreen
                                    selectedHotel?.let { hotel ->
                                        navController.currentBackStackEntry
                                            ?.savedStateHandle
                                            ?.apply {
                                                // Save individual fields (SavedStateHandle can't store complex objects)
                                                set("accommodation_id", hotel.hotelId)
                                                set("accommodation_price", hotel.pricePerNight ?: 0.0)
                                                set("accommodation_currency", hotel.currency ?: "EUR")
                                                set("accommodation_name", hotel.name)
                                                set("accommodation_type", hotel.type ?: "hotel")
                                                set("accommodation_location", hotel.address?.cityName ?: "")
                                                set("accommodation_rating", hotel.googleRating ?: hotel.rating ?: 0.0)
                                                set("accommodation_image_url", hotel.media?.firstOrNull()?.uri ?: "")
                                                // Save additional hotel fields for reconstruction
                                                set("hotel_id", hotel.id)
                                                set("hotel_city_code", hotel.cityCode ?: "")
                                                set("hotel_description", hotel.description ?: "")
                                                set("hotel_amenities", hotel.amenities.joinToString(","))
                                                set("hotel_media_uris", hotel.media?.map { it.uri }?.joinToString(",") ?: "")
                                                set("hotel_address_lines", hotel.address?.lines?.joinToString("|") ?: "")
                                                set("hotel_address_city", hotel.address?.cityName ?: "")
                                                set("hotel_address_country", hotel.address?.countryCode ?: "")
                                                set("hotel_address_postal", hotel.address?.postalCode ?: "")
                                                set("hotel_google_rating", hotel.googleRating ?: 0.0)
                                                set("hotel_google_review_count", hotel.googleReviewCount ?: 0)
                                                set("hotel_google_place_id", hotel.googlePlaceId ?: "")
                                            }
                                        // Navigate directly to ReservationScreen (matching iOS flow)
                                        navController.navigate("booking/$destinationId")
                                    } ?: run {
                                        // Continue without accommodation (matching iOS)
                                        navController.navigate("booking/$destinationId")
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp)
                                    .padding(horizontal = 16.dp, vertical = 16.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (selectedHotel != null) 
                                        Color(0xFF1976D2) 
                                    else 
                                        Color(0xFF1976D2).copy(alpha = 0.6f)
                                )
                            ) {
                                Text(
                                    text = if (selectedHotel != null) {
                                        StringTranslator.translate(context, "Continuer avec cet hébergement")
                                    } else {
                                        StringTranslator.translate(context, "Continuer sans hébergement")
                                    },
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
            
            is HotelsUiState.Idle -> {
                // Idle state - show nothing or skeleton
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = StringTranslator.translate(context, "Sélectionnez une ville pour commencer"),
                        color = colorScheme.onSurfaceVariant
                    )
                }
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
    isSelected: Boolean = false,
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
            containerColor = if (isSelected) 
                Color(0xFF1976D2).copy(alpha = 0.1f) 
            else 
                colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 4.dp else 2.dp),
        border = if (isSelected) {
            androidx.compose.foundation.BorderStroke(2.dp, Color(0xFF1976D2))
        } else null,
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = hotel.name,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) Color(0xFF1976D2) else colorScheme.onSurface,
                        maxLines = 2,
                        modifier = Modifier.weight(1f)
                    )
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF1976D2),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                
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

