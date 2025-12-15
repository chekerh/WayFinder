package tn.esprit.wayfinder.ui.screens

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import tn.esprit.wayfinder.R
import tn.esprit.wayfinder.models.Accommodation
import tn.esprit.wayfinder.models.AccommodationReview
import tn.esprit.wayfinder.models.FlightDestination
import tn.esprit.wayfinder.models.Hotel
import tn.esprit.wayfinder.models.HotelAddress
import tn.esprit.wayfinder.models.HotelMedia
import tn.esprit.wayfinder.models.HotelReview
import tn.esprit.wayfinder.navigation.SELECTED_DESTINATION_KEY
import tn.esprit.wayfinder.presentation.auth.ViewModelFactory
import tn.esprit.wayfinder.ui.theme.WayFinderTheme
import tn.esprit.wayfinder.utils.StringTranslator
import tn.esprit.wayfinder.viewmodels.HotelDetailUiState
import tn.esprit.wayfinder.viewmodels.HotelsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HotelDetailScreen(
    navController: NavController,
    hotelId: String
) {
    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme
    val scrollState = rememberScrollState()
    
    val hotelsViewModel: HotelsViewModel = viewModel(
        factory = ViewModelFactory(context.applicationContext as Application)
    )
    val hotelDetailState by hotelsViewModel.hotelDetailState.collectAsState()
    
    // Get saved data from navigation
    val savedStateHandle = navController.previousBackStackEntry?.savedStateHandle
    // Reconstruct destination from primitives (can't save complex objects to SavedStateHandle)
    val destination: FlightDestination? = remember(savedStateHandle) {
        val id = savedStateHandle?.get<String>("destination_id")
        val name = savedStateHandle?.get<String>("destination_name")
        val city = savedStateHandle?.get<String>("destination_city")
        val country = savedStateHandle?.get<String>("destination_country")
        val price = savedStateHandle?.get<Double>("destination_price")
        val currency = savedStateHandle?.get<String>("destination_currency") ?: "EUR"
        val imageUrl = savedStateHandle?.get<String>("destination_image_url")
        val airline = savedStateHandle?.get<String>("destination_airline")
        
        if (id != null && name != null && city != null && country != null) {
            FlightDestination(
                id = id,
                name = name,
                city = city,
                country = country,
                imageUrl = imageUrl,
                price = price,
                currency = currency,
                description = null,
                departureDate = null,
                arrivalDate = null,
                airline = airline
            )
        } else {
            null
        }
    }
    // Don't try to get Accommodation directly - it's not Parcelable
    // val accommodation = savedStateHandle?.get<Accommodation>("selected_accommodation")
    
    // Reconstruct Hotel object from saved primitive fields (SavedStateHandle can't store complex objects)
    val savedHotel: Hotel? = remember(savedStateHandle) {
        val id = savedStateHandle?.get<String>("hotel_id")
        val hotelId = savedStateHandle?.get<String>("accommodation_id")
        val name = savedStateHandle?.get<String>("accommodation_name")
        val price = savedStateHandle?.get<Double>("accommodation_price")
        val currency = savedStateHandle?.get<String>("accommodation_currency")
        val rating = savedStateHandle?.get<Double>("accommodation_rating")
        val type = savedStateHandle?.get<String>("accommodation_type")
        val cityCode = savedStateHandle?.get<String>("hotel_city_code")
        val description = savedStateHandle?.get<String>("hotel_description")
        val amenitiesStr = savedStateHandle?.get<String>("hotel_amenities")
        val mediaUrisStr = savedStateHandle?.get<String>("hotel_media_uris")
        val addressLinesStr = savedStateHandle?.get<String>("hotel_address_lines")
        val addressCity = savedStateHandle?.get<String>("hotel_address_city")
        val addressCountry = savedStateHandle?.get<String>("hotel_address_country")
        val addressPostal = savedStateHandle?.get<String>("hotel_address_postal")
        val googleRating = savedStateHandle?.get<Double>("hotel_google_rating")
        val googleReviewCount = savedStateHandle?.get<Int>("hotel_google_review_count")
        val googlePlaceId = savedStateHandle?.get<String>("hotel_google_place_id")
        
        if (id != null && hotelId != null && name != null && price != null && currency != null) {
            Hotel(
                id = id,
                hotelId = hotelId,
                name = name,
                cityCode = cityCode?.takeIf { it.isNotEmpty() },
                rating = rating?.takeIf { it > 0 },
                type = type?.takeIf { it.isNotEmpty() },
                pricePerNight = price,
                currency = currency,
                amenities = amenitiesStr?.split(",")?.filter { it.isNotEmpty() } ?: emptyList(),
                address = if (addressLinesStr != null || addressCity != null) {
                    HotelAddress(
                        lines = addressLinesStr?.split("|")?.filter { it.isNotEmpty() },
                        cityName = addressCity?.takeIf { it.isNotEmpty() },
                        countryCode = addressCountry?.takeIf { it.isNotEmpty() },
                        postalCode = addressPostal?.takeIf { it.isNotEmpty() }
                    )
                } else null,
                description = description?.takeIf { it.isNotEmpty() },
                media = mediaUrisStr?.split(",")?.filter { it.isNotEmpty() }?.map { HotelMedia(uri = it) } ?: emptyList(),
                googleRating = googleRating?.takeIf { it > 0 },
                googleReviewCount = googleReviewCount?.takeIf { it > 0 },
                googlePlaceId = googlePlaceId?.takeIf { it.isNotEmpty() }
            )
        } else {
            null
        }
    }
    
    // Fetch hotel details if not already loaded and we don't have the hotel from list
    LaunchedEffect(hotelId) {
        if (savedHotel == null) {
            hotelsViewModel.getHotelDetails(hotelId)
        }
    }
    
    // Get Hotel object for display (prioritize saved hotel from list, then fetched details)
    val hotelForDisplay: Hotel? = savedHotel ?: when (val state = hotelDetailState) {
        is HotelDetailUiState.Success -> state.hotel
        else -> null
    }
    
    // Convert Hotel to Accommodation for navigation (if needed)
    val accommodationForNav: Accommodation? = hotelForDisplay?.let { h ->
        Accommodation(
            id = h.id,
            name = h.name,
            type = h.type ?: "hotel",
            price = h.pricePerNight ?: 0.0,
            currency = h.currency ?: "EUR",
            rating = h.rating ?: h.googleRating ?: 0.0,
            imageUrl = h.media?.firstOrNull()?.uri,
            location = h.address?.cityName ?: h.cityCode ?: "",
            amenities = h.amenities,
            address = h.address?.lines?.joinToString(", ") ?: h.address?.cityName,
            description = h.description,
            photos = h.media?.map { it.uri } ?: emptyList(),
            reviews = when (val state = hotelDetailState) {
                is HotelDetailUiState.Success -> state.reviews.map { r ->
                    AccommodationReview(
                        authorName = r.authorName ?: "Anonymous",
                        rating = r.rating?.toDouble() ?: 0.0,
                        text = r.text ?: "",
                        time = r.time
                    )
                }
                else -> emptyList()
            },
            userRatingsTotal = h.googleReviewCount
        )
    }
    
    // Use hotelForDisplay for display logic
    val hotel = hotelForDisplay
    
    // Parallax effect calculations
    val imageHeight = 300.dp
    val scrollOffset = scrollState.value
    val parallaxOffset = (scrollOffset * 0.5f).dp
    val alpha = (1f - (scrollOffset / 600f)).coerceIn(0f, 1f)
    
    // Photo gallery state
    var selectedPhotoIndex by remember { mutableStateOf(0) }
    val photos: List<String> = remember(hotel) {
        hotel?.media?.map { it.uri }?.takeIf { it.isNotEmpty() }
            ?: listOf("https://images.unsplash.com/photo-1566073771259-6a8506099945?w=800")
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Hero Image with Parallax
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(imageHeight)
                .graphicsLayer {
                    translationY = parallaxOffset.toPx()
                    this.alpha = alpha
                }
        ) {
            AsyncImage(
                model = photos.getOrElse(selectedPhotoIndex) { photos.first() },
                contentDescription = hotel?.name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                placeholder = painterResource(id = R.drawable.europe),
                error = painterResource(id = R.drawable.europe)
            )
            
            // Gradient overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.3f),
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.5f)
                            )
                        )
                    )
            )
            
            // Back button
            IconButton(
                onClick = { navController.popBackStack() },
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(16.dp)
                    .size(40.dp)
                    .background(Color.Black.copy(alpha = 0.3f), CircleShape)
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
            
            // Photo indicators
            if (photos.size > 1) {
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 40.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    for (index in photos.indices) {
                        Box(
                            modifier = Modifier
                                .size(if (index == selectedPhotoIndex) 10.dp else 8.dp)
                                .clip(CircleShape)
                                .background(
                                    if (index == selectedPhotoIndex) Color.White
                                    else Color.White.copy(alpha = 0.5f)
                                )
                        )
                    }
                }
            }
        }
        
        // Content Card
        Card(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 260.dp),
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
            colors = CardDefaults.cardColors(containerColor = colorScheme.background)
        ) {
            when {
                hotelDetailState is HotelDetailUiState.Loading && hotel == null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                hotel == null -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = StringTranslator.translate(context, "Détails de l'hôtel non disponibles"),
                            color = colorScheme.error
                        )
                    }
                }
                else -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(scrollState)
                            .padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        // Hotel Name and Rating
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = hotel.name,
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = colorScheme.onBackground
                                )
                                
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        Icons.Filled.LocationOn,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = hotel.address?.lines?.firstOrNull() 
                                            ?: hotel.address?.cityName 
                                            ?: "Adresse non disponible",
                                        fontSize = 14.sp,
                                        color = colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            
                            // Rating badge
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = colorScheme.primaryContainer
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        Icons.Filled.Star,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp),
                                        tint = colorScheme.primary
                                    )
                                    Text(
                                        text = String.format("%.1f", hotel.rating ?: hotel.googleRating ?: 0.0),
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        }
                        
                        // Reviews count
                        hotel.googleReviewCount?.let { count ->
                            Text(
                                text = "$count ${StringTranslator.translate(context, "avis")}",
                                fontSize = 14.sp,
                                color = colorScheme.onSurfaceVariant
                            )
                        }
                        
                        // Photo Gallery
                        if (photos.size > 1) {
                            Text(
                                text = StringTranslator.translate(context, "Photos"),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = colorScheme.onBackground
                            )
                            
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(photos.size) { index ->
                                    Card(
                                        modifier = Modifier
                                            .size(100.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        onClick = { selectedPhotoIndex = index }
                                    ) {
                                        AsyncImage(
                                            model = photos[index],
                                            contentDescription = null,
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    }
                                }
                            }
                        }
                        
                        // Description
                        hotel.description?.let { desc ->
                            Text(
                                text = StringTranslator.translate(context, "À propos"),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = colorScheme.onBackground
                            )
                            Text(
                                text = desc,
                                fontSize = 14.sp,
                                color = colorScheme.onSurfaceVariant,
                                lineHeight = 22.sp
                            )
                        }
                        
                        // Amenities
                        if (hotel.amenities.isNotEmpty()) {
                            Text(
                                text = StringTranslator.translate(context, "Équipements"),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = colorScheme.onBackground
                            )
                            
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(hotel.amenities) { amenity ->
                                    AmenityChip(amenity = amenity)
                                }
                            }
                        }
                        
                        // Check-in/Check-out times - Note: These would come from hotel offers, not hotel details
                        // This section is commented out as checkInTime/checkOutTime are not in Hotel model
                        // They would be available in HotelOffer.policies
                        
                        // Reviews Section
                        val reviews: List<HotelReview> = remember(hotelDetailState) {
                            when (val state = hotelDetailState) {
                                is HotelDetailUiState.Success -> state.reviews
                                else -> emptyList()
                            }
                        }
                        if (reviews.isNotEmpty()) {
                            Text(
                                text = StringTranslator.translate(context, "Avis des clients"),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = colorScheme.onBackground
                            )
                            
                            reviews.take(5).forEach { review: HotelReview ->
                                ReviewCard(review = review)
                            }
                        }
                        
                        // Price and Book Button
                        Spacer(modifier = Modifier.height(80.dp)) // Space for bottom bar
                    }
                }
            }
        }
        
        // Bottom booking bar
        if (hotel != null) {
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth(),
                color = colorScheme.surface,
                shadowElevation = 16.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .navigationBarsPadding(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = StringTranslator.translate(context, "Prix par nuit"),
                            fontSize = 12.sp,
                            color = colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${hotel.pricePerNight?.toInt() ?: 0} ${hotel.currency ?: "EUR"}",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = colorScheme.primary
                        )
                    }
                    
                    Button(
                        onClick = {
                            // Save hotel selection and navigate to activities preview
                            // Note: Can't save complex objects like Accommodation or FlightDestination to SavedStateHandle
                            navController.currentBackStackEntry?.savedStateHandle?.apply {
                                // Save destination fields as primitives instead of the object
                                destination?.let { dest ->
                                    set("destination_id", dest.id)
                                    set("destination_name", dest.name)
                                    set("destination_city", dest.city)
                                    set("destination_country", dest.country)
                                    set("destination_price", dest.price ?: 0.0)
                                    set("destination_currency", dest.currency)
                                    set("destination_image_url", dest.imageUrl ?: "")
                                    set("destination_airline", dest.airline ?: "")
                                }
                                // Save all accommodation fields as primitives
                                set("accommodation_id", hotel.hotelId)
                                set("accommodation_name", hotel.name)
                                set("accommodation_price", hotel.pricePerNight ?: 0.0)
                                set("accommodation_currency", hotel.currency ?: "EUR")
                                set("accommodation_type", hotel.type ?: "hotel")
                                set("accommodation_location", hotel.address?.cityName ?: destination?.city ?: "")
                                set("accommodation_rating", hotel.googleRating ?: hotel.rating ?: 0.0)
                                set("accommodation_image_url", hotel.media?.firstOrNull()?.uri ?: "")
                                // Save additional fields for accommodation reconstruction if needed
                                set("accommodation_address", hotel.address?.lines?.joinToString(", ") ?: hotel.address?.cityName ?: "")
                                set("accommodation_description", hotel.description ?: "")
                                set("accommodation_amenities", hotel.amenities.joinToString(","))
                                set("accommodation_photos", hotel.media?.map { it.uri }?.joinToString(",") ?: "")
                            }
                            // Navigate directly to ReservationScreen (matching iOS flow)
                            navController.navigate("booking/${destination?.id ?: hotelId}")
                        },
                        modifier = Modifier.height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colorScheme.primary
                        )
                    ) {
                        Text(
                            text = StringTranslator.translate(context, "Réserver maintenant"),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AmenityChip(amenity: String) {
    val colorScheme = MaterialTheme.colorScheme
    val icon = when (amenity.lowercase()) {
        "wifi", "free_wifi", "wifi_gratuit" -> Icons.Filled.Wifi
        "pool", "swimming_pool", "piscine" -> Icons.Filled.Pool
        "parking", "free_parking" -> Icons.Filled.LocalParking
        "restaurant" -> Icons.Filled.Restaurant
        "spa", "wellness" -> Icons.Filled.Spa
        "gym", "fitness", "fitness_center" -> Icons.Filled.FitnessCenter
        "air_conditioning", "climatisation" -> Icons.Filled.AcUnit
        "bar" -> Icons.Filled.LocalBar
        "room_service" -> Icons.Filled.RoomService
        "breakfast" -> Icons.Filled.FreeBreakfast
        else -> Icons.Filled.CheckCircle
    }
    
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = colorScheme.secondaryContainer
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = colorScheme.onSecondaryContainer
            )
            Text(
                text = amenity.replace("_", " ").replaceFirstChar { it.uppercase() },
                fontSize = 12.sp,
                color = colorScheme.onSecondaryContainer
            )
        }
    }
}

@Composable
fun TimeInfoCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    time: String
) {
    val colorScheme = MaterialTheme.colorScheme
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = colorScheme.primary
            )
            Column {
                Text(
                    text = label,
                    fontSize = 12.sp,
                    color = colorScheme.onSurfaceVariant
                )
                Text(
                    text = time,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
fun ReviewCard(review: HotelReview) {
    val colorScheme = MaterialTheme.colorScheme
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Avatar placeholder
                    Surface(
                        modifier = Modifier.size(40.dp),
                        shape = CircleShape,
                        color = colorScheme.primary.copy(alpha = 0.2f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = review.authorName?.firstOrNull()?.uppercase() ?: "?",
                                fontWeight = FontWeight.Bold,
                                color = colorScheme.primary
                            )
                        }
                    }
                    Text(
                        text = review.authorName ?: "Anonyme",
                        fontWeight = FontWeight.Medium,
                        color = colorScheme.onSurface
                    )
                }
                
                // Rating
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        Icons.Filled.Star,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = Color(0xFFFFC107)
                    )
                    Text(
                        text = String.format("%.1f", review.rating?.toDouble() ?: 0.0),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = colorScheme.onSurface
                    )
                }
            }
            
            Text(
                text = review.text ?: "",
                fontSize = 14.sp,
                color = colorScheme.onSurfaceVariant,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun HotelDetailScreenPreview() {
    WayFinderTheme {
        HotelDetailScreen(rememberNavController(), hotelId = "test")
    }
}

