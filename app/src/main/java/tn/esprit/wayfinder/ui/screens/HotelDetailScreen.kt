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
import tn.esprit.wayfinder.navigation.SELECTED_DESTINATION_KEY
import tn.esprit.wayfinder.presentation.auth.ViewModelFactory
import tn.esprit.wayfinder.ui.theme.WayFinderTheme
import tn.esprit.wayfinder.utils.StringTranslator
import tn.esprit.wayfinder.viewmodels.HotelDetailUiState
import tn.esprit.wayfinder.viewmodels.HotelsViewModel
import tn.esprit.wayfinder.models.HotelReview

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
    val destination = savedStateHandle?.get<FlightDestination>(SELECTED_DESTINATION_KEY)
    val accommodation = savedStateHandle?.get<Accommodation>("selected_accommodation")
    
    // Fetch hotel details if not already loaded
    LaunchedEffect(hotelId) {
        if (accommodation == null) {
            hotelsViewModel.getHotelDetails(hotelId)
        }
    }
    
    // Convert Hotel to Accommodation for display
    val hotel: Accommodation? = accommodation ?: run {
        when (val state = hotelDetailState) {
            is HotelDetailUiState.Success -> {
                val h = state.hotel
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
                    reviews = state.reviews.map { r ->
                        AccommodationReview(
                            authorName = r.authorName ?: "Anonymous",
                            rating = r.rating?.toDouble() ?: 0.0,
                            text = r.text ?: "",
                            time = r.time
                        )
                    },
                    userRatingsTotal = h.googleReviewCount
                )
            }
            else -> null
        }
    }
    
    // Parallax effect calculations
    val imageHeight = 300.dp
    val scrollOffset = scrollState.value
    val parallaxOffset = (scrollOffset * 0.5f).dp
    val alpha = (1f - (scrollOffset / 600f)).coerceIn(0f, 1f)
    
    // Photo gallery state
    var selectedPhotoIndex by remember { mutableStateOf(0) }
    val photos = hotel?.photos?.takeIf { it.isNotEmpty() } 
        ?: listOf("https://images.unsplash.com/photo-1566073771259-6a8506099945?w=800")

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
                                        text = hotel.address ?: hotel.location,
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
                                        text = String.format("%.1f", hotel.rating),
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        }
                        
                        // Reviews count
                        hotel.userRatingsTotal?.let { count ->
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
                        if (hotel.amenitiesList.isNotEmpty()) {
                            Text(
                                text = StringTranslator.translate(context, "Équipements"),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = colorScheme.onBackground
                            )
                            
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(hotel.amenitiesList) { amenity ->
                                    AmenityChip(amenity = amenity)
                                }
                            }
                        }
                        
                        // Check-in/Check-out times
                        if (hotel.checkInTime != null || hotel.checkOutTime != null) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                hotel.checkInTime?.let {
                                    TimeInfoCard(
                                        icon = Icons.Filled.Login,
                                        label = StringTranslator.translate(context, "Check-in"),
                                        time = it
                                    )
                                }
                                hotel.checkOutTime?.let {
                                    TimeInfoCard(
                                        icon = Icons.Filled.Logout,
                                        label = StringTranslator.translate(context, "Check-out"),
                                        time = it
                                    )
                                }
                            }
                        }
                        
                        // Reviews Section
                        if (hotel.reviews.isNotEmpty()) {
                            Text(
                                text = StringTranslator.translate(context, "Avis des clients"),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = colorScheme.onBackground
                            )
                            
                            for (review in hotel.reviews.take(5)) {
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
                            text = "${hotel.price.toInt()} ${hotel.currency}",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = colorScheme.primary
                        )
                    }
                    
                    Button(
                        onClick = {
                            // Save hotel selection and navigate to activities preview
                            navController.currentBackStackEntry?.savedStateHandle?.apply {
                                set(SELECTED_DESTINATION_KEY, destination)
                                set("selected_accommodation", hotel)
                                set("accommodation_name", hotel.name)
                                set("accommodation_price", hotel.price)
                                set("accommodation_type", hotel.type)
                            }
                            navController.navigate("activities_preview/${destination?.id ?: hotelId}")
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
fun ReviewCard(review: AccommodationReview) {
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
                                text = review.authorName.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                                fontWeight = FontWeight.Bold,
                                color = colorScheme.primary
                            )
                        }
                    }
                    Text(
                        text = review.authorName,
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
                        text = String.format("%.1f", review.rating),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = colorScheme.onSurface
                    )
                }
            }
            
            Text(
                text = review.text,
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

