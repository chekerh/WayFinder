package tn.esprit.wayfinder.ui.screens

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
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
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import androidx.compose.ui.graphics.toArgb
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import tn.esprit.wayfinder.models.CountryMemory
import tn.esprit.wayfinder.models.SharedTrip
import tn.esprit.wayfinder.network.ApiService
import tn.esprit.wayfinder.presentation.auth.ViewModelFactory
import tn.esprit.wayfinder.utils.StringTranslator
import tn.esprit.wayfinder.viewmodels.SocialViewModel
import android.util.Log

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapMemoriesScreen(navController: NavController) {
    val context = LocalContext.current
    val socialViewModel: SocialViewModel = viewModel(factory = ViewModelFactory(context.applicationContext as Application))
    val mapMemoriesState by socialViewModel.mapMemoriesState.collectAsState()
    
    var selectedCountry by remember { mutableStateOf<CountryMemory?>(null) }
    var showMemoriesDialog by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    
    // Default camera position (Europe)
    val defaultLocation = remember { LatLng(50.0, 10.0) }
    val cameraPositionState = rememberCameraPositionState()

    LaunchedEffect(Unit) {
        try {
            // Load map memories first
            socialViewModel.loadMapMemories()
            // Set initial camera position after a short delay to ensure map is ready
            kotlinx.coroutines.delay(500)
            cameraPositionState.animate(
                CameraUpdateFactory.newLatLngZoom(defaultLocation, 3f),
                0
            )
        } catch (e: Exception) {
            android.util.Log.e("MapMemoriesScreen", "Error initializing map", e)
        }
    }
    
    // Log state changes for debugging
    LaunchedEffect(mapMemoriesState) {
        android.util.Log.d("MapMemoriesScreen", "Map memories state: isLoading=${mapMemoriesState.isLoading}, error=${mapMemoriesState.error}, countries=${mapMemoriesState.memories?.countries?.size ?: 0}")
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        StringTranslator.translate(context, "Mes Memories"),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge
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
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                mapMemoriesState.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                mapMemoriesState.error != null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = mapMemoriesState.error ?: "Erreur",
                                color = MaterialTheme.colorScheme.error
                            )
                            Button(onClick = { socialViewModel.loadMapMemories() }) {
                                Text(StringTranslator.translate(context, "Réessayer"))
                            }
                        }
                    }
                }
                else -> {
                    val countries = mapMemoriesState.memories?.countries ?: emptyList()
                    
                    // Always show the map, even if empty
                        GoogleMap(
                            modifier = Modifier.fillMaxSize(),
                            cameraPositionState = cameraPositionState,
                        onMapClick = { selectedCountry = null },
                        properties = MapProperties(
                            mapType = MapType.NORMAL,
                            isMyLocationEnabled = false
                        ),
                        uiSettings = MapUiSettings(
                            zoomControlsEnabled = true,
                            compassEnabled = true,
                            myLocationButtonEnabled = false
                        )
                    ) {
                        // Add markers only if countries exist
                            countries.forEach { country ->
                                val position = LatLng(country.lat, country.lng)
                                
                                // Get first image from trips for marker
                                val markerImage = country.trips.firstOrNull()?.images?.firstOrNull()
                                
                                Marker(
                                    state = MarkerState(position = position),
                                    title = country.country,
                                    snippet = "${country.count} ${StringTranslator.translate(context, "memories")}",
                                    icon = if (markerImage != null) {
                                        createCustomMarkerIcon(context, markerImage, country.count)
                                    } else {
                                        BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_VIOLET)
                                    },
                                    onClick = {
                                        selectedCountry = country
                                        showMemoriesDialog = true
                                        true
                                    }
                                )
                            }
                        }
                        
                    // Show empty state overlay if no countries
                    if (countries.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth(0.8f)
                                    .padding(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                                ),
                                elevation = CardDefaults.cardElevation(8.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.LocationOn,
                                        contentDescription = null,
                                        modifier = Modifier.size(64.dp),
                                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                                    )
                                    Text(
                                        text = StringTranslator.translate(context, "Aucune mémoire sur la carte"),
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = StringTranslator.translate(context, "Partagez vos voyages pour voir vos memories sur la carte !"),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                        modifier = Modifier.padding(horizontal = 16.dp)
                                    )
                                }
                            }
                        }
                    }
                    
                    // Bottom sheet with countries list (always show if there are countries)
                    if (countries.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                        ) {
                            BottomSheetWithCountries(
                                countries = countries,
                                onCountryClick = { country ->
                                    selectedCountry = country
                                    showMemoriesDialog = true
                                    // Animate camera to country location
                                    coroutineScope.launch {
                                        val countryLocation = LatLng(country.lat, country.lng)
                                        val cameraUpdate = CameraUpdateFactory.newLatLngZoom(countryLocation, 5f)
                                        cameraPositionState.animate(cameraUpdate, 1000)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
    
    // Dialog to show memories for selected country
    if (showMemoriesDialog && selectedCountry != null) {
        CountryMemoriesDialog(
            country = selectedCountry!!,
            onDismiss = {
                showMemoriesDialog = false
                selectedCountry = null
            },
            onMemoryClick = { trip ->
                navController.navigate("journey_detail/${trip.id}")
                showMemoriesDialog = false
                selectedCountry = null
            }
        )
    }
}

@Composable
fun BottomSheetWithCountries(
    countries: List<CountryMemory>,
    onCountryClick: (CountryMemory) -> Unit
) {
    val context = LocalContext.current
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text(
                text = "${countries.size} ${StringTranslator.translate(context, "Pays visités")}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(countries) { country ->
                    CountryItem(
                        country = country,
                        onClick = { onCountryClick(country) }
                    )
                }
            }
        }
    }
}

@Composable
fun CountryItem(
    country: CountryMemory,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.LocationOn,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Column {
                    Text(
                        text = country.country,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "${country.count} ${StringTranslator.translate(context, "memories")}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}

@Composable
fun CountryMemoriesDialog(
    country: CountryMemory,
    onDismiss: () -> Unit,
    onMemoryClick: (SharedTrip) -> Unit
) {
    val context = LocalContext.current
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "${StringTranslator.translate(context, "Memories")} - ${country.country}",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            LazyColumn(
                modifier = Modifier.heightIn(max = 400.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(country.trips) { trip ->
                    MemoryItem(
                        trip = trip,
                        onClick = { onMemoryClick(trip) }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(StringTranslator.translate(context, "Fermer"))
            }
        }
    )
}

fun createCustomMarkerIcon(
    context: android.content.Context,
    imageUrl: String,
    count: Int
): com.google.android.gms.maps.model.BitmapDescriptor {
    return try {
        val imageLoader = ImageLoader(context)
        val request = ImageRequest.Builder(context)
            .data(imageUrl)
            .size(120, 120)
            .build()
        
        val result = kotlinx.coroutines.runBlocking {
            imageLoader.execute(request)
        }
        
        if (result is SuccessResult) {
            val drawable = result.drawable
            val bitmap = if (drawable is BitmapDrawable) {
                drawable.bitmap
            } else {
                val bitmap = Bitmap.createBitmap(120, 120, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)
                drawable.setBounds(0, 0, canvas.width, canvas.height)
                drawable.draw(canvas)
                bitmap
            }
            
            // Scale bitmap to 120x120 if needed
            val scaledBitmap = if (bitmap.width != 120 || bitmap.height != 120) {
                Bitmap.createScaledBitmap(bitmap, 120, 120, true)
            } else {
                bitmap
            }
            
            val markerBitmap = Bitmap.createBitmap(140, 160, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(markerBitmap)
            
            // Draw rounded image with border
            val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG)
            borderPaint.color = Color(0xFF9C27B0).toArgb() // Purple border
            borderPaint.style = Paint.Style.STROKE
            borderPaint.strokeWidth = 4f
            
            // Draw border
            canvas.drawRoundRect(10f, 10f, 130f, 130f, 12f, 12f, borderPaint)
            
            // Draw image
            val roundedBitmap = Bitmap.createBitmap(120, 120, Bitmap.Config.ARGB_8888)
            val roundedCanvas = Canvas(roundedBitmap)
            val roundedPaint = Paint(Paint.ANTI_ALIAS_FLAG)
            roundedPaint.shader = android.graphics.BitmapShader(scaledBitmap, android.graphics.Shader.TileMode.CLAMP, android.graphics.Shader.TileMode.CLAMP)
            roundedCanvas.drawRoundRect(0f, 0f, 120f, 120f, 12f, 12f, roundedPaint)
            canvas.drawBitmap(roundedBitmap, 10f, 10f, null)
            
            // Draw count badge
            if (count > 1) {
                val badgePaint = Paint(Paint.ANTI_ALIAS_FLAG)
                badgePaint.color = Color(0xFF9C27B0).toArgb()
                val badgeRadius = 18f
                canvas.drawCircle(130f, 30f, badgeRadius, badgePaint)
                
                val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)
                textPaint.color = android.graphics.Color.WHITE
                textPaint.textSize = 20f
                textPaint.typeface = Typeface.DEFAULT_BOLD
                textPaint.textAlign = Paint.Align.CENTER
                val countText = if (count > 99) "99+" else count.toString()
                canvas.drawText(countText, 130f, 36f, textPaint)
            }
            
            BitmapDescriptorFactory.fromBitmap(markerBitmap)
        } else {
            BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_VIOLET)
        }
    } catch (e: Exception) {
        android.util.Log.e("MapMemoriesScreen", "Error creating custom marker", e)
        BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_VIOLET)
    }
}

@Composable
fun MemoryItem(
    trip: SharedTrip,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (trip.images.isNotEmpty()) {
                AsyncImage(
                    model = trip.images.first(),
                    contentDescription = trip.title,
                    modifier = Modifier
                        .size(60.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
            }
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = trip.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                if (trip.description != null) {
                    Text(
                        text = trip.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        maxLines = 2
                    )
                }
            }
        }
    }
}

