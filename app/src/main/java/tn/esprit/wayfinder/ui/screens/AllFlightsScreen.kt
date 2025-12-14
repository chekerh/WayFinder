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
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.AirplanemodeActive
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Euro
import androidx.compose.material3.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import tn.esprit.wayfinder.ui.theme.WayFinderTheme
import coil.compose.AsyncImage
import tn.esprit.wayfinder.R
import tn.esprit.wayfinder.presentation.auth.ViewModelFactory
import tn.esprit.wayfinder.viewmodels.CatalogViewModel
import tn.esprit.wayfinder.viewmodels.CatalogUiState
import tn.esprit.wayfinder.viewmodels.FavoritesViewModel
import tn.esprit.wayfinder.utils.StringTranslator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllFlightsScreen(navController: NavController, selectedRegion: String? = null) {
    val context = LocalContext.current
    val catalogViewModel: CatalogViewModel = viewModel(factory = ViewModelFactory(context.applicationContext as Application))
    val favoritesViewModel: FavoritesViewModel = viewModel(factory = ViewModelFactory(context.applicationContext as Application))
    val uiState by catalogViewModel.uiState.collectAsState()
    
    var currentFilterRegion by remember { mutableStateOf(selectedRegion) }
    var showFilterMenu by remember { mutableStateOf(false) }
    var showFilterDrawer by remember { mutableStateOf(false) }
    
    // Filter state
    var minPrice by remember { mutableStateOf(0f) }
    var maxPrice by remember { mutableStateOf(2000f) }
    var selectedAirlines by remember { mutableStateOf<Set<String>>(emptySet()) }
    var maxDurationHours by remember { mutableStateOf(24f) }
    var travelClass by remember { mutableStateOf<String?>(null) }

    // Region to country mapping (same as HomeScreen) - Keep keys in French for logic
    val regions = listOf(
        "Préférences" to emptyList<String>(),
        "Europe" to listOf("France", "United Kingdom", "Italy", "Spain", "Netherlands", "Germany", "Switzerland", "Belgium", "Portugal", "Greece", "Austria", "Sweden", "Norway", "Denmark", "Finland", "Poland", "Czech Republic", "Hungary", "Ireland"),
        "Asie" to listOf(
            "China", "Japan", "India", "Thailand", "Singapore", "Malaysia", 
            "Indonesia", "South Korea", "Vietnam", "Philippines", "UAE", 
            "Saudi Arabia", "Turkey", "Israel",
            // Variations
            "United Arab Emirates", "Korea", "South Korea", "Corée du Sud",
            "Thaïlande", "Singapour", "Corée", "EAU", "Émirats arabes unis"
        ),
        "Amerique" to listOf(
            "United States", "Canada", "Mexico", "Brazil", "Argentina", 
            "Chile", "Colombia", "Peru",
            // Variations
            "USA", "US", "États-Unis", "États Unis", "United States of America"
        ),
        "Australie" to listOf("Australia", "New Zealand", "Fiji")
    )
    
    val regionCountries = regions.associate { it }

    // Load all flights on first composition (showAll = true to get all flights)
    LaunchedEffect(Unit) {
        catalogViewModel.loadRecommendedFlights(showAll = true)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(StringTranslator.translate(context, "Tous les vols"), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = StringTranslator.translate(context, "Retour"))
                    }
                },
                actions = {
                    // Region filter button
                    IconButton(onClick = { showFilterMenu = true }) {
                        Icon(
                            imageVector = Icons.Filled.LocationOn,
                            contentDescription = StringTranslator.translate(context, "Filtrer par région"),
                            tint = if (currentFilterRegion != null) Color(0xFF1976D2) else Color.Gray
                        )
                    }
                    // Advanced filter button
                    IconButton(onClick = { showFilterDrawer = true }) {
                        Icon(
                            imageVector = Icons.Filled.Tune,
                            contentDescription = StringTranslator.translate(context, "Filtres avancés"),
                            tint = if (hasActiveFilters(minPrice, maxPrice, selectedAirlines, maxDurationHours, travelClass)) Color(0xFF1976D2) else Color.Gray
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.surface
    ) { paddingValues ->
        // Filter dropdown menu
        DropdownMenu(
            expanded = showFilterMenu,
            onDismissRequest = { showFilterMenu = false }
        ) {
            DropdownMenuItem(
                text = { Text(StringTranslator.translate(context, "Tous")) },
                onClick = {
                    currentFilterRegion = null
                    showFilterMenu = false
                }
            )
            regions.forEach { (regionName, _) ->
                if (regionName != "Préférences") {
                    DropdownMenuItem(
                        text = { Text(StringTranslator.translate(context, regionName)) },
                        onClick = {
                            currentFilterRegion = regionName
                            showFilterMenu = false
                        }
                    )
                }
            }
        }
        when (val state = uiState) {
            is CatalogUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            is CatalogUiState.Success -> {
                if (state.fromCache) {
                    AssistChip(
                        onClick = { catalogViewModel.loadRecommendedFlights(showAll = true) },
                        label = { Text(StringTranslator.translate(context, "Résultats hors ligne (cache)")) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Filled.CloudOff,
                                contentDescription = StringTranslator.translate(context, "Mode hors ligne")
                            )
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = Color(0xFFFFF3E0),
                            labelColor = Color(0xFFEF6C00)
                        ),
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .padding(top = 8.dp)
                    )
                }
                // Filter destinations based on current filter region
                var filteredDestinations = if (currentFilterRegion != null && currentFilterRegion != "Préférences") {
                    val filterCountries = regionCountries[currentFilterRegion] ?: emptyList()
                    if (filterCountries.isNotEmpty()) {
                        // Create a mapping for country name variations (French/English)
                        val countryMapping = mapOf(
                            // French to English
                            "France" to "France",
                            "Italie" to "Italy",
                            "Espagne" to "Spain",
                            "Royaume-Uni" to "United Kingdom",
                            "États-Unis" to "United States",
                            "États Unis" to "United States",
                            "EAU" to "UAE",
                            "Émirats arabes unis" to "UAE",
                            "Tunisie" to "Tunisia",
                            "Corée du Sud" to "South Korea",
                            "Corée" to "South Korea",
                            "Thaïlande" to "Thailand",
                            "Singapour" to "Singapore",
                            // English variations
                            "USA" to "United States",
                            "US" to "United States",
                            "United States of America" to "United States",
                            "UK" to "United Kingdom",
                            "UAE" to "UAE",
                            "United Arab Emirates" to "UAE",
                            "Korea" to "South Korea"
                        )
                        
                        state.destinations.filter { destination ->
                            val destCountry = destination.country.trim()
                            val normalizedDestCountry = countryMapping[destCountry] ?: destCountry
                            
                            filterCountries.any { filterCountry ->
                                val normalizedFilterCountry = countryMapping[filterCountry] ?: filterCountry
                                
                                // More robust country matching
                                normalizedDestCountry.equals(normalizedFilterCountry, ignoreCase = true) ||
                                normalizedDestCountry.contains(normalizedFilterCountry, ignoreCase = true) ||
                                normalizedFilterCountry.contains(normalizedDestCountry, ignoreCase = true) ||
                                // Also check original names
                                destCountry.equals(filterCountry, ignoreCase = true) ||
                                destCountry.contains(filterCountry, ignoreCase = true) ||
                                filterCountry.contains(destCountry, ignoreCase = true) ||
                                // Partial word matching for compound names
                                normalizedDestCountry.split(" ").any { word ->
                                    normalizedFilterCountry.split(" ").any { filterWord ->
                                        word.equals(filterWord, ignoreCase = true) && word.length > 3
                                    }
                                }
                            }
                        }
                    } else {
                        state.destinations
                    }
                } else {
                    state.destinations
                }
                
                // Get available airlines from all destinations (before filtering)
                val allAvailableAirlines = state.destinations.mapNotNull { it.airline }.distinct()
                
                // Apply advanced filters
                filteredDestinations = filteredDestinations.filter { destination ->
                    // Price filter
                    val price = destination.price ?: 0.0
                    if (price < minPrice || price > maxPrice) return@filter false
                    
                    // Airline filter
                    if (selectedAirlines.isNotEmpty() && destination.airline != null) {
                        if (!selectedAirlines.contains(destination.airline)) return@filter false
                    }
                    
                    // Duration filter (if we had duration data, we'd check it here)
                    // For now, we'll skip this as FlightDestination doesn't have duration
                    
                    // Travel class filter (if we had class data, we'd check it here)
                    // For now, we'll skip this as FlightDestination doesn't have travel class
                    
                    true
                }
                
                if (filteredDestinations.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = StringTranslator.translate(context, "Aucun vol disponible"),
                                color = Color.Gray,
                                style = MaterialTheme.typography.bodyLarge
                            )
                            if (selectedRegion != null) {
                                Text(
                                    text = StringTranslator.translate(context, "pour la région sélectionnée"),
                                    color = Color.Gray,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(vertical = 16.dp)
                    ) {
                        items(filteredDestinations) { destination ->
                            FlightCard(
                                destination = destination,
                                favoritesViewModel = favoritesViewModel,
                                onClick = {
                                    navController.navigate("flight_detail/${destination.id}")
                                }
                            )
                        }
                    }
                }
            }
            is CatalogUiState.Error -> {
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
                            text = state.message,
                            color = Color.Red,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Button(onClick = { catalogViewModel.loadRecommendedFlights() }) {
                            Text(StringTranslator.translate(context, "Réessayer"))
                        }
                    }
                }
            }
            else -> {}
        }
        
        // Advanced Filter Drawer
        if (showFilterDrawer) {
            FilterDrawer(
                onDismiss = { showFilterDrawer = false },
                minPrice = minPrice,
                maxPrice = maxPrice,
                onPriceRangeChange = { min, max ->
                    minPrice = min
                    maxPrice = max
                },
                selectedAirlines = selectedAirlines,
                onAirlinesChange = { selectedAirlines = it },
                maxDurationHours = maxDurationHours,
                onDurationChange = { maxDurationHours = it },
                travelClass = travelClass,
                onTravelClassChange = { travelClass = it },
                availableAirlines = if (uiState is CatalogUiState.Success) {
                    (uiState as CatalogUiState.Success).destinations.mapNotNull { it.airline }.distinct()
                } else {
                    emptyList()
                },
                onReset = {
                    minPrice = 0f
                    maxPrice = 2000f
                    selectedAirlines = emptySet()
                    maxDurationHours = 24f
                    travelClass = null
                }
            )
        }
    }
}

@Composable
fun hasActiveFilters(
    minPrice: Float,
    maxPrice: Float,
    selectedAirlines: Set<String>,
    maxDurationHours: Float,
    travelClass: String?
): Boolean {
    return minPrice > 0f || maxPrice < 2000f || selectedAirlines.isNotEmpty() || maxDurationHours < 24f || travelClass != null
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterDrawer(
    onDismiss: () -> Unit,
    minPrice: Float,
    maxPrice: Float,
    onPriceRangeChange: (Float, Float) -> Unit,
    selectedAirlines: Set<String>,
    onAirlinesChange: (Set<String>) -> Unit,
    maxDurationHours: Float,
    onDurationChange: (Float) -> Unit,
    travelClass: String?,
    onTravelClassChange: (String?) -> Unit,
    availableAirlines: List<String>,
    onReset: () -> Unit
) {
    val context = LocalContext.current
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Tune,
                        contentDescription = null,
                        tint = Color(0xFF1976D2),
                        modifier = Modifier.size(28.dp)
                    )
                    Text(
                        text = StringTranslator.translate(context, "Filtres avancés"),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                Row {
                    TextButton(onClick = onReset) {
                        Text(StringTranslator.translate(context, "Réinitialiser"), color = Color(0xFF1976D2), fontWeight = FontWeight.Medium)
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = StringTranslator.translate(context, "Fermer"), tint = Color.Gray)
                    }
                }
            }
            
            HorizontalDivider()
            
            // Price Range Filter
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Euro,
                        contentDescription = null,
                        tint = Color(0xFF1976D2),
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = StringTranslator.translate(context, "Prix"),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "${minPrice.toInt()} EUR",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1976D2),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                    Text(
                        text = "—",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.Gray
                    )
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "${maxPrice.toInt()} EUR",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1976D2),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
                RangeSlider(
                    value = minPrice..maxPrice,
                    onValueChange = { range ->
                        onPriceRangeChange(range.start, range.endInclusive)
                    },
                    valueRange = 0f..2000f,
                    steps = 19
                )
            }
            
            HorizontalDivider()
            
            // Airline Filter
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.AirplanemodeActive,
                        contentDescription = null,
                        tint = Color(0xFF1976D2),
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = StringTranslator.translate(context, "Compagnie aérienne"),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                if (availableAirlines.isEmpty()) {
                    Text(
                        text = StringTranslator.translate(context, "Aucune compagnie disponible"),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.height(150.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(availableAirlines) { airline ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        val newSet = if (selectedAirlines.contains(airline)) {
                                            selectedAirlines - airline
                                        } else {
                                            selectedAirlines + airline
                                        }
                                        onAirlinesChange(newSet)
                                    },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = selectedAirlines.contains(airline),
                                    onCheckedChange = { checked ->
                                        val newSet = if (checked) {
                                            selectedAirlines + airline
                                        } else {
                                            selectedAirlines - airline
                                        }
                                        onAirlinesChange(newSet)
                                    }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = airline,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }
            }
            
            HorizontalDivider()
            
            // Duration Filter
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.AccessTime,
                        contentDescription = null,
                        tint = Color(0xFF1976D2),
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = StringTranslator.translate(context, "Durée maximale"),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "${maxDurationHours.toInt()} ${StringTranslator.translate(context, "heures")}",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1976D2),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
                Slider(
                    value = maxDurationHours,
                    onValueChange = onDurationChange,
                    valueRange = 1f..48f,
                    steps = 23
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "1h",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                    Text(
                        text = "48h",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            }
            
            HorizontalDivider()
            
            // Travel Class Filter
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.AirplanemodeActive,
                        contentDescription = null,
                        tint = Color(0xFF1976D2),
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = StringTranslator.translate(context, "Classe de voyage"),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                val travelClasses = listOf("Économique", "Premium Économique", "Affaires", "Première")
                travelClasses.chunked(2).forEach { rowClasses ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowClasses.forEach { className ->
                            FilterChip(
                                selected = travelClass == className,
                                onClick = {
                                    onTravelClassChange(if (travelClass == className) null else className)
                                },
                                label = { Text(StringTranslator.translate(context, className)) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        // Fill remaining space if odd number
                        if (rowClasses.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Apply Button
            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF1976D2)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = StringTranslator.translate(context, "Appliquer les filtres"),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun FlightCard(
    destination: tn.esprit.wayfinder.models.FlightDestination,
    favoritesViewModel: FavoritesViewModel,
    onClick: () -> Unit
) {
    var isFavorite by remember { mutableStateOf(false) }
    
    // Check if favorite on composition
    LaunchedEffect(destination.id) {
        favoritesViewModel.checkFavorite("flight", destination.id) { favorite ->
            isFavorite = favorite
        }
    }
    
    // Better image URL - use city name for more relevant images
    val imageUrl = destination.imageUrl ?: "https://images.unsplash.com/photo-1488646953014-85cb44e25828?w=800&h=600&fit=crop&q=80"
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // City image
            AsyncImage(
                model = imageUrl,
                contentDescription = destination.name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                placeholder = painterResource(id = R.drawable.travel_image),
                error = painterResource(id = R.drawable.travel_image)
            )
            
            // Gradient overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f)),
                            startY = 100f
                        )
                    )
            )
            
            // Content
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp)
            ) {
                Text(
                    text = destination.name,
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = destination.country,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.9f),
                    modifier = Modifier.padding(top = 4.dp)
                )
                if (destination.price != null && destination.price > 0) {
                    Text(
                        text = "${destination.price.toInt()} ${destination.currency}",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
            
            // Favorite button
            IconButton(
                onClick = {
                    isFavorite = !isFavorite
                    if (isFavorite) {
                        favoritesViewModel.addFavorite(
                            "flight",
                            destination.id,
                            mapOf(
                                "name" to destination.name,
                                "city" to destination.city,
                                "country" to destination.country,
                                "imageUrl" to (destination.imageUrl ?: ""),
                                "price" to (destination.price ?: 0.0),
                                "currency" to destination.currency,
                                "airline" to (destination.airline ?: "")
                            )
                        )
                    } else {
                        favoritesViewModel.removeFavorite("flight", destination.id)
                    }
                },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
            ) {
                val context = LocalContext.current
                Icon(
                    imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    contentDescription = if (isFavorite) StringTranslator.translate(context, "Remove from favorites") else StringTranslator.translate(context, "Add to favorites"),
                    tint = if (isFavorite) Color(0xFFFF1744) else Color.White,
                    modifier = Modifier
                        .size(24.dp)
                        .background(
                            Color.Black.copy(alpha = 0.3f),
                            RoundedCornerShape(12.dp)
                        )
                        .padding(4.dp)
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AllFlightsScreenPreview() {
    WayFinderTheme {
        AllFlightsScreen(rememberNavController())
    }
}
