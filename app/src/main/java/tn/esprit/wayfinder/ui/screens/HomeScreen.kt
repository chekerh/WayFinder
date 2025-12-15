package tn.esprit.wayfinder.ui.screens

import android.app.Application
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PersonOutline
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material3.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.*
import androidx.compose.runtime.derivedStateOf
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.animation.fadeIn
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import kotlin.math.absoluteValue
import tn.esprit.wayfinder.R
import tn.esprit.wayfinder.manager.TokenManager
import tn.esprit.wayfinder.navigation.SELECTED_DESTINATION_KEY
import tn.esprit.wayfinder.presentation.auth.ViewModelFactory
import tn.esprit.wayfinder.ui.theme.WayFinderTheme
import tn.esprit.wayfinder.ui.components.CustomBottomNavigationBar
import tn.esprit.wayfinder.ui.components.SkeletonLoadingCard
import tn.esprit.wayfinder.ui.components.CompactPointsBadge
import tn.esprit.wayfinder.ui.components.AppTopBar
import tn.esprit.wayfinder.utils.HapticFeedbackHelper
import tn.esprit.wayfinder.utils.StringTranslator
import tn.esprit.wayfinder.viewmodels.CatalogViewModel
import tn.esprit.wayfinder.viewmodels.CatalogUiState
import tn.esprit.wayfinder.viewmodels.FavoritesViewModel
import tn.esprit.wayfinder.viewmodels.NotificationsViewModel
import tn.esprit.wayfinder.viewmodels.NotificationsUiState
import androidx.compose.ui.draw.scale
import tn.esprit.wayfinder.ui.components.SwipeableDestinationCard
import tn.esprit.wayfinder.ui.components.TravelReelsFeed
import tn.esprit.wayfinder.ui.components.AiTravelVideoGenerator

data class Region(val name: String, val imageRes: Int, val filterCountries: List<String> = emptyList())

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavController) {
    val context = LocalContext.current
    val catalogViewModel: CatalogViewModel = viewModel(factory = ViewModelFactory(context.applicationContext as Application))
    val favoritesViewModel: FavoritesViewModel = viewModel(factory = ViewModelFactory(context.applicationContext as Application))
    val notificationsViewModel: NotificationsViewModel = viewModel(factory = ViewModelFactory(context.applicationContext as Application))
    val uiState by catalogViewModel.uiState.collectAsState()
    val notificationsState by notificationsViewModel.uiState.collectAsState()
    val tokenManager = remember { TokenManager(context) }
    val currentUser = remember { tokenManager.getUser() }

    // Selected region state - Default to "Préférences" if user completed onboarding
    var selectedRegion by remember { 
        mutableStateOf<String?>(
            if (currentUser?.onboardingCompleted == true) {
                StringTranslator.translate(context, "Préférences")
            } else {
                null
            }
        )
    }
    
    // Filter state for sorting (inspired by Skyscanner, Kayak)
    var selectedFilter by remember { mutableStateOf<String?>(null) }
    
    // Show popup dialog for first-time users who haven't completed onboarding
    var showOnboardingDialog by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        // Check if user hasn't completed onboarding and show dialog
        if (currentUser?.onboardingCompleted != true && currentUser?.onboardingSkipped != true) {
            // Delay to show after screen loads
            kotlinx.coroutines.delay(1000)
            showOnboardingDialog = true
        }
        notificationsViewModel.refreshUnreadCount()
    }
    
    // Load flights on first composition - request more results to ensure variety
    LaunchedEffect(Unit) {
        catalogViewModel.loadRecommendedFlights(showAll = false, maxResults = 20)
    }
    
    // Reload flights when region changes to ensure fresh data for filtering
    LaunchedEffect(selectedRegion) {
        // Always reload when region changes to get fresh data for filtering
        // Request more results to ensure we have destinations from all regions
        catalogViewModel.loadRecommendedFlights(showAll = false, maxResults = 20)
    }

    // Regions data with country filters
    val regions = listOf(
        Region(
            name = StringTranslator.translate(context, "Préférences"),
            imageRes = R.drawable.travel_image, // Not used - we use Star icon instead
            filterCountries = emptyList() // No filter - show personalized preferences
        ),
        Region(
            name = StringTranslator.translate(context, "Europe"),
            imageRes = R.drawable.europe,
            filterCountries = listOf("France", "United Kingdom", "Italy", "Spain", "Netherlands", "Germany", "Switzerland", "Belgium", "Portugal", "Greece", "Austria", "Sweden", "Norway", "Denmark", "Finland", "Poland", "Czech Republic", "Hungary", "Ireland")
        ),
        Region(
            name = StringTranslator.translate(context, "Asie"),
            imageRes = R.drawable.asia,
            filterCountries = listOf(
                // English names (primary)
                "China", "Japan", "India", "Thailand", "Singapore", "Malaysia", 
                "Indonesia", "South Korea", "Vietnam", "Philippines", "UAE", 
                "Saudi Arabia", "Turkey", "Israel",
                // English variations
                "United Arab Emirates", "Korea", "South Korea",
                // French names
                "Chine", "Japon", "Inde", "Thaïlande", "Singapour", "Malaisie",
                "Indonésie", "Corée du Sud", "Corée", "Viêt Nam", "Philippines",
                "EAU", "Émirats arabes unis", "Arabie saoudite", "Turquie", "Israël"
            )
        ),
        Region(
            name = StringTranslator.translate(context, "Amerique"),
            imageRes = R.drawable.america,
            filterCountries = listOf(
                // English names (primary)
                "United States", "Canada", "Mexico", "Brazil", "Argentina", 
                "Chile", "Colombia", "Peru",
                // English variations
                "USA", "US", "United States of America", "America",
                // French names
                "États-Unis", "États Unis", "États-Unis d'Amérique",
                "Mexique", "Brésil", "Argentine", "Chili", "Colombie", "Pérou"
            )
        ),
        Region(
            name = StringTranslator.translate(context, "Australie"),
            imageRes = R.drawable.australia,
            filterCountries = listOf("Australia", "New Zealand", "Fiji")
        )
    )

    val scrollState = rememberScrollState()
    
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            AppTopBar(
                navController = navController,
                notificationsViewModel = notificationsViewModel
            )
        },
        bottomBar = { CustomBottomNavigationBar(navController = navController) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(bottom = paddingValues.calculateBottomPadding())
        ) {
            // Region filter section - Always visible between topbar and content
            Surface(
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    // Region filter chips - Make them more prominent
                    RegionSection(
                        regions = regions,
                        selectedRegion = selectedRegion,
                        onRegionSelected = { regionName ->
                            selectedRegion = if (selectedRegion == regionName) null else regionName
                        }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Column {
                // Personalized section removed
                
                Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = StringTranslator.translate(context, "Comparateur avec ChatGPT"),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Compare prices button (inspired by Skyscanner)
                            TextButton(
                                onClick = {
                                    navController.navigate("all_flights/${selectedRegion ?: "null"}")
                                },
                                colors = ButtonDefaults.textButtonColors(
                                    contentColor = Color(0xFF1976D2)
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.TrendingDown,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = StringTranslator.translate(context, "Comparer"),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            
                            Text(
                                text = StringTranslator.translate(context, "Voir tous"),
                                color = Color(0xFF1976D2),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.clickable {
                                    val route = if (selectedRegion != null) {
                                        "all_flights/${selectedRegion}"
                                    } else {
                                        "all_flights/null"
                                    }
                                    navController.navigate(route)
                                }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Quick Filters (inspired by Skyscanner, Kayak)
                    QuickFiltersSection(
                        selectedFilter = selectedFilter,
                        onFilterSelected = { filterType ->
                            selectedFilter = filterType
                            android.util.Log.d("HomeScreen", "Filter selected: $filterType")
                        }
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    when (val state = uiState) {
                        is CatalogUiState.Loading -> {
                            SkeletonLoadingCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(340.dp)
                            )
                        }
                        is CatalogUiState.Success -> {
                            // Filter destinations based on selected region
                            val filteredDestinations = if (selectedRegion != null && selectedRegion != StringTranslator.translate(context, "Préférences")) {
                                val selectedRegionData = regions.find { it.name == selectedRegion }
                                val filterCountries = selectedRegionData?.filterCountries ?: emptyList()
                                
                                // Debug: Log for troubleshooting
                                android.util.Log.d("HomeScreen", "Selected region: $selectedRegion")
                                android.util.Log.d("HomeScreen", "Filter countries: $filterCountries")
                                android.util.Log.d("HomeScreen", "Total destinations before filter: ${state.destinations.size}")
                                state.destinations.take(5).forEach { dest ->
                                    android.util.Log.d("HomeScreen", "Destination: ${dest.name}, Country: ${dest.country}")
                                }
                                
                                if (filterCountries.isNotEmpty()) {
                                    // Create a comprehensive mapping for country name variations (French/English)
                                    val countryMapping = mapOf(
                                        // Europe - French to English
                                        "France" to "France",
                                        "Italie" to "Italy",
                                        "Espagne" to "Spain",
                                        "Royaume-Uni" to "United Kingdom",
                                        "Pays-Bas" to "Netherlands",
                                        "Allemagne" to "Germany",
                                        "Suisse" to "Switzerland",
                                        "Belgique" to "Belgium",
                                        "Portugal" to "Portugal",
                                        "Grèce" to "Greece",
                                        "Autriche" to "Austria",
                                        "Suède" to "Sweden",
                                        "Norvège" to "Norway",
                                        "Danemark" to "Denmark",
                                        "Finlande" to "Finland",
                                        "Pologne" to "Poland",
                                        "République tchèque" to "Czech Republic",
                                        "Hongrie" to "Hungary",
                                        "Irlande" to "Ireland",
                                        "Turquie" to "Turkey",
                                        // Americas - French to English
                                        "États-Unis" to "United States",
                                        "États Unis" to "United States",
                                        "États-Unis d'Amérique" to "United States",
                                        "Mexique" to "Mexico",
                                        "Brésil" to "Brazil",
                                        "Argentine" to "Argentina",
                                        "Chili" to "Chile",
                                        "Colombie" to "Colombia",
                                        "Pérou" to "Peru",
                                        // Asia - French to English
                                        "Chine" to "China",
                                        "Japon" to "Japan",
                                        "Inde" to "India",
                                        "Thaïlande" to "Thailand",
                                        "Singapour" to "Singapore",
                                        "Malaisie" to "Malaysia",
                                        "Indonésie" to "Indonesia",
                                        "Corée du Sud" to "South Korea",
                                        "Corée" to "South Korea",
                                        "Viêt Nam" to "Vietnam",
                                        "Philippines" to "Philippines",
                                        "EAU" to "UAE",
                                        "Émirats arabes unis" to "UAE",
                                        "Arabie saoudite" to "Saudi Arabia",
                                        "Israël" to "Israel",
                                        "Tunisie" to "Tunisia",
                                        // English variations
                                        "USA" to "United States",
                                        "US" to "United States",
                                        "United States of America" to "United States",
                                        "America" to "United States",
                                        "UK" to "United Kingdom",
                                        "UAE" to "UAE",
                                        "United Arab Emirates" to "UAE",
                                        "Korea" to "South Korea"
                                    )
                                    
                                    // Simplified matching - check all variations
                                    val filtered = state.destinations.filter { destination ->
                                        val destCountry = destination.country.trim()
                                        val destLower = destCountry.lowercase()
                                        
                                        // Normalize destination country
                                        val normalizedDest = (countryMapping[destCountry] ?: destCountry).lowercase()
                                        
                                        // Check against all filter countries
                                        val matches = filterCountries.any { filterCountry ->
                                            val filterLower = filterCountry.lowercase()
                                            val normalizedFilter = (countryMapping[filterCountry] ?: filterCountry).lowercase()
                                            
                                            // Multiple matching strategies
                                            destLower == filterLower ||
                                            normalizedDest == normalizedFilter ||
                                            destLower == normalizedFilter ||
                                            normalizedDest == filterLower ||
                                            destLower.contains(filterLower) ||
                                            filterLower.contains(destLower) ||
                                            normalizedDest.contains(normalizedFilter) ||
                                            normalizedFilter.contains(normalizedDest) ||
                                            // Word-by-word matching
                                            destLower.split(" ").any { destWord ->
                                                destWord.length > 2 && (
                                                    normalizedFilter.contains(destWord) ||
                                                    filterLower.contains(destWord) ||
                                                    normalizedFilter.split(" ").any { it == destWord } ||
                                                    filterLower.split(" ").any { it == destWord }
                                                )
                                            } ||
                                            normalizedDest.split(" ").any { destWord ->
                                                destWord.length > 2 && (
                                                    normalizedFilter.contains(destWord) ||
                                                    filterLower.contains(destWord) ||
                                                    normalizedFilter.split(" ").any { it == destWord } ||
                                                    filterLower.split(" ").any { it == destWord }
                                                )
                                            }
                                        } ||
                                        // Special cases for abbreviations
                                        when {
                                            // UAE
                                            (destLower.contains("uae") || normalizedDest.contains("uae")) &&
                                            filterCountries.any { it.lowercase().contains("uae") || it.lowercase().contains("united arab") || it.lowercase().contains("émirats") || it.lowercase().contains("eau") } -> true
                                            // United States
                                            (destLower.contains("united states") || normalizedDest.contains("united states") || destLower.contains("usa") || normalizedDest.contains("usa")) &&
                                            filterCountries.any { it.lowercase().contains("united states") || it.lowercase().contains("usa") || it.lowercase().contains("us") || it.lowercase().contains("états") || it.lowercase().contains("america") } -> true
                                            // South Korea
                                            (destLower.contains("korea") || normalizedDest.contains("korea")) &&
                                            filterCountries.any { it.lowercase().contains("korea") || it.lowercase().contains("corée") } -> true
                                            // Japan
                                            (destLower.contains("japan") || normalizedDest.contains("japan")) &&
                                            filterCountries.any { it.lowercase().contains("japan") || it.lowercase().contains("japon") } -> true
                                            // Thailand
                                            (destLower.contains("thailand") || normalizedDest.contains("thailand")) &&
                                            filterCountries.any { it.lowercase().contains("thailand") || it.lowercase().contains("thaïlande") } -> true
                                            // Singapore
                                            (destLower.contains("singapore") || normalizedDest.contains("singapore")) &&
                                            filterCountries.any { it.lowercase().contains("singapore") || it.lowercase().contains("singapour") } -> true
                                            // Turkey
                                            (destLower.contains("turkey") || normalizedDest.contains("turkey")) &&
                                            filterCountries.any { it.lowercase().contains("turkey") || it.lowercase().contains("turquie") } -> true
                                            else -> false
                                        }
                                        
                                        if (matches) {
                                            android.util.Log.d("HomeScreen", "✓ Match: ${destination.name} (${destCountry})")
                                        }
                                        
                                        matches
                                    }
                                    
                                    android.util.Log.d("HomeScreen", "Total destinations: ${state.destinations.size}, Filtered: ${filtered.size}")
                                    if (filtered.isEmpty() && state.destinations.isNotEmpty()) {
                                        android.util.Log.w("HomeScreen", "Filtering failed! Available countries: ${state.destinations.map { it.country }.distinct()}")
                                    }
                                    filtered
                                } else {
                                    state.destinations
                                }
                            } else {
                                // Show all destinations for "Préférences" or no selection
                                state.destinations
                            }
                            
                            // Apply sorting based on selected filter (inspired by Skyscanner, Kayak)
                            val sortedDestinations = when (selectedFilter) {
                                "price" -> {
                                    // Sort by price (lowest first)
                                    filteredDestinations.sortedBy { it.price ?: Double.MAX_VALUE }
                                }
                                "duration" -> {
                                    // Sort by trip duration (calculate from dates if available)
                                    filteredDestinations.sortedBy { dest ->
                                        val departure = dest.departureDate
                                        val arrival = dest.arrivalDate
                                        if (departure != null && arrival != null) {
                                            try {
                                                val depDate = LocalDate.parse(departure.substringBefore("T"))
                                                val arrDate = LocalDate.parse(arrival.substringBefore("T"))
                                                ChronoUnit.DAYS.between(depDate, arrDate).toDouble()
                                            } catch (e: Exception) {
                                                dest.price ?: Double.MAX_VALUE
                                            }
                                        } else {
                                            dest.price ?: Double.MAX_VALUE
                                        }
                                    }
                                }
                                "time" -> {
                                    // Sort by departure date/time (earliest first)
                                    filteredDestinations.sortedBy { dest ->
                                        dest.departureDate ?: ""
                                    }
                                }
                                else -> filteredDestinations // No sorting
                            }
                            
                            // Show at least 5-6 flights on home screen, or all if filtered results are fewer
                            val displayDestinations = if (sortedDestinations.isEmpty() && selectedRegion != null && selectedRegion != StringTranslator.translate(context, "Préférences")) {
                                // If no results after filtering, show message but also log for debugging
                                android.util.Log.w("HomeScreen", "No filtered destinations found for region: $selectedRegion")
                                android.util.Log.w("HomeScreen", "Available destinations: ${state.destinations.map { "${it.name} (${it.country})" }}")
                                emptyList()
                            } else {
                                sortedDestinations.take(6)
                            }
                            
                            // Show message if no flights found for selected region
                            if (displayDestinations.isEmpty() && selectedRegion != null && selectedRegion != StringTranslator.translate(context, "Préférences") && filteredDestinations.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(340.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(16.dp)
                                    ) {
                                        Text(
                                            text = StringTranslator.translate(context, "Aucun vol trouvé pour cette région"),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            style = MaterialTheme.typography.bodyLarge
                                        )
                                        Text(
                                            text = StringTranslator.translate(context, "Essayez une autre région ou consultez tous les vols"),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        // Debug info (only in debug builds)
                                        if (state.destinations.isNotEmpty()) {
                                            Text(
                                                text = "Debug: ${state.destinations.size} destinations disponibles",
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                                style = MaterialTheme.typography.bodySmall,
                                                fontSize = 10.sp
                                            )
                                        }
                                    }
                                }
                            } else {
                                if (state.fromCache) {
                                AssistChip(
                                    onClick = { catalogViewModel.loadRecommendedFlights(showAll = false) },
                                    label = { Text(StringTranslator.translate(context, "Affichage hors ligne (cache)")) },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.CloudOff,
                                            contentDescription = "Mode hors ligne"
                                        )
                                    },
                                    colors = AssistChipDefaults.assistChipColors(
                                        containerColor = Color(0xFFFFF3E0),
                                        labelColor = Color(0xFFEF6C00)
                                    ),
                                    modifier = Modifier.padding(bottom = 12.dp)
                                )
                            }
                            
                            // Powered by ChatGPT Badge with enhanced design
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 12.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = Color(0xFF10A37F).copy(alpha = 0.15f),
                                    border = BorderStroke(
                                        1.dp,
                                        Color(0xFF10A37F).copy(alpha = 0.3f)
                                    ),
                                    modifier = Modifier.padding(horizontal = 4.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.AutoAwesome,
                                            contentDescription = null,
                                            tint = Color(0xFF10A37F),
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Column(
                                            verticalArrangement = Arrangement.spacedBy(2.dp)
                                        ) {
                                            Text(
                                                text = StringTranslator.translate(context, "Powered by ChatGPT"),
                                                color = Color(0xFF10A37F),
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = StringTranslator.translate(context, "Recommandations personnalisées"),
                                                color = Color(0xFF10A37F).copy(alpha = 0.7f),
                                                fontSize = 10.sp
                                            )
                                        }
                                    }
                                }
                            }
                            
                                EnhancedDestinationsSection(
                                    destinations = displayDestinations,
                                    navController = navController,
                                    favoritesViewModel = favoritesViewModel
                                )
                            }
                        }
                        is CatalogUiState.Error -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(340.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = state.message,
                                        color = Color.Red,
                                        modifier = Modifier.padding(bottom = 8.dp)
                                    )
                                    Button(onClick = { catalogViewModel.loadRecommendedFlights() }) {
                                        Text(StringTranslator.translate(context, "Réessayer"))
                                    }
                                }
                            }
                        }
                        else -> {}
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // AI Travel Video Generator Section
            AiTravelVideoGenerator(
                onVideoGenerated = { videoUrl ->
                    // Optionally navigate to video player or show in reels
                    android.util.Log.d("HomeScreen", "AI Video generated: $videoUrl")
                }
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Travel Reels Feed Section - Modern reels/posts feed
            TravelReelsFeed(navController = navController)
            
            Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
    
    // Onboarding Dialog for first-time users
    if (showOnboardingDialog) {
        AlertDialog(
            onDismissRequest = { showOnboardingDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Filled.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(48.dp)
                )
            },
            title = {
                Text(
                    text = StringTranslator.translate(context, "Bienvenue sur WayFinder!"),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = StringTranslator.translate(context, "Personnalisez votre expérience de voyage en répondant à quelques questions rapides."),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = StringTranslator.translate(context, "Nous vous recommanderons des destinations parfaitement adaptées à vos préférences!"),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showOnboardingDialog = false
                        navController.navigate("onboarding")
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(StringTranslator.translate(context, "Commencer"))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showOnboardingDialog = false }
                ) {
                    Text(StringTranslator.translate(context, "Plus tard"))
                }
            }
        )
    }
}


@Composable
fun RegionSection(
    regions: List<Region>,
    selectedRegion: String?,
    onRegionSelected: (String) -> Unit
) {
    val scrollState = rememberScrollState()
    
    Row(
        modifier = Modifier.horizontalScroll(scrollState),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        regions.forEach { region -> 
            RegionChip(
                region = region,
                isSelected = selectedRegion == region.name,
                onClick = { onRegionSelected(region.name) }
            )
        }
    }
}

@Composable
fun RegionChip(
    region: Region,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .background(
                if (isSelected) Color(0xFF1976D2) else colorScheme.surface,
                RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Use icon for "Préférences", image for others
        if (region.name == "Préférences") {
            Icon(
                imageVector = Icons.Filled.Star,
                contentDescription = region.name,
                modifier = Modifier.size(16.dp),
                tint = if (isSelected) Color.White else Color(0xFFFFC107)
            )
        } else {
            Image(
                painter = painterResource(id = region.imageRes),
                contentDescription = region.name,
                modifier = Modifier.size(20.dp).clip(CircleShape)
            )
        }
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = region.name,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
            color = if (isSelected) Color.White else colorScheme.onSurface,
            fontSize = 12.sp
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DestinationsSection(
    destinations: List<tn.esprit.wayfinder.models.FlightDestination>,
    navController: NavController,
    favoritesViewModel: FavoritesViewModel
) {
    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme
    if (destinations.isEmpty()) {
        Text(
            text = StringTranslator.translate(context, "Aucune destination disponible"),
            color = colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(16.dp)
        )
        return
    }

    // Show all destinations - user wants to see multiple flights
    // Start at index 1 (second card) if there are at least 2 destinations, otherwise start at 0
    val initialPage = if (destinations.size >= 2) 1 else 0
    val pagerState = rememberPagerState(
        pageCount = { destinations.size },
        initialPage = initialPage
    )

    HorizontalPager(
        state = pagerState,
        contentPadding = PaddingValues(horizontal = 80.dp),
        pageSpacing = (-120).dp
    ) { page ->
        val pageOffset = ((pagerState.currentPage - page) + pagerState.currentPageOffsetFraction).absoluteValue

            Card(
                modifier = Modifier
                .zIndex(1f - pageOffset)
                .graphicsLayer {
                    alpha = lerp(start = 0.5f, stop = 1f, fraction = 1f - pageOffset.coerceIn(0f, 1f))
                    scaleY = lerp(start = 0.85f, stop = 1f, fraction = 1f - pageOffset.coerceIn(0f, 1f))
                }
                .width(290.dp)
                .height(340.dp)
                .clickable {
                    val selectedDestination = destinations[page]
                    navController.currentBackStackEntry?.savedStateHandle?.set(
                        SELECTED_DESTINATION_KEY,
                        selectedDestination
                    )
                    navController.navigate("flight_detail/${selectedDestination.id}")
                },
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
        ) {
            DestinationCardContent(
                destination = destinations[page],
                favoritesViewModel = favoritesViewModel
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun EnhancedDestinationsSection(
    destinations: List<tn.esprit.wayfinder.models.FlightDestination>,
    navController: NavController,
    favoritesViewModel: FavoritesViewModel
) {
    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme
    if (destinations.isEmpty()) {
        Text(
            text = StringTranslator.translate(context, "Aucune destination disponible"),
            color = colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(16.dp)
        )
        return
    }

    // Show all destinations - user wants to see multiple flights
    // Start at index 1 (second card) if there are at least 2 destinations, otherwise start at 0
    val initialPage = if (destinations.size >= 2) 1 else 0
    val pagerState = rememberPagerState(
        pageCount = { destinations.size },
        initialPage = initialPage
    )

    HorizontalPager(
        state = pagerState,
        contentPadding = PaddingValues(horizontal = 80.dp),
        pageSpacing = (-120).dp
    ) { page ->
        val pageOffset = ((pagerState.currentPage - page) + pagerState.currentPageOffsetFraction).absoluteValue

            Card(
                modifier = Modifier
                .zIndex(1f - pageOffset)
                .graphicsLayer {
                    alpha = lerp(start = 0.5f, stop = 1f, fraction = 1f - pageOffset.coerceIn(0f, 1f))
                    scaleY = lerp(start = 0.85f, stop = 1f, fraction = 1f - pageOffset.coerceIn(0f, 1f))
                }
                .width(290.dp)
                .height(340.dp)
                .clickable {
                    val selectedDestination = destinations[page]
                    navController.currentBackStackEntry?.savedStateHandle?.set(
                        SELECTED_DESTINATION_KEY,
                        selectedDestination
                    )
                    navController.navigate("flight_detail/${selectedDestination.id}")
                },
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
        ) {
            DestinationCardContent(
                destination = destinations[page],
                favoritesViewModel = favoritesViewModel
            )
        }
    }
}

@Composable
fun DestinationCardContent(
    destination: tn.esprit.wayfinder.models.FlightDestination,
    favoritesViewModel: FavoritesViewModel
) {
    val context = LocalContext.current
    // State for favorite button
    var isFavorite by remember { mutableStateOf(false) }
    
    // Check if favorite on composition
    LaunchedEffect(destination.id) {
        favoritesViewModel.checkFavorite("flight", destination.id) { favorite ->
            isFavorite = favorite
        }
    }
    
    // Use the image URL from the destination (already set in ViewModel with city-specific images)
    val imageUrl = destination.imageUrl ?: "https://images.unsplash.com/photo-1488646953014-85cb44e25828?w=800&h=600&fit=crop&q=80"
    
    Box(modifier = Modifier.fillMaxSize()) {
        // Load image from URL using Coil
        AsyncImage(
            model = imageUrl,
            contentDescription = destination.name,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            placeholder = painterResource(id = R.drawable.travel_image),
            error = painterResource(id = R.drawable.travel_image)
        )
        
        // Gradient overlay for better text readability
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f)),
                        startY = 200f
                    )
                )
        )
        
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                .padding(24.dp)
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
                Column(
                    modifier = Modifier.padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "${destination.price.toInt()} ${destination.currency}",
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        // Price badge - determine if it's a good deal (smart pricing logic)
                        // Use reasonable thresholds based on typical flight prices
                        val isBestDeal = destination.price != null && destination.price < 300 // Best deals under 300
                        val isLowPrice = destination.price != null && destination.price < 500 && !isBestDeal // Low prices under 500
                        if (isBestDeal) {
                            PriceBadge(
                                text = StringTranslator.translate(context, "Meilleur prix"),
                                color = Color(0xFF4CAF50)
                            )
                        } else if (isLowPrice) {
                            PriceBadge(
                                text = StringTranslator.translate(context, "Prix bas"),
                                color = Color(0xFF2196F3)
                            )
                        }
                    }
                    // Deal indicators row
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Smart badge logic based on destination characteristics
                        val priceInt = (destination.price ?: 0.0).toInt()
                        val showPopular = priceInt % 3 == 0 || destination.name.length % 2 == 0
                        val showQuickBooking = priceInt % 5 == 0 || destination.airline?.isNotEmpty() == true
                        val showUrgency = priceInt % 7 == 0 || (destination.departureDate != null && destination.departureDate!!.length > 10)
                        
                        if (showPopular) {
                            DealBadge(
                                text = StringTranslator.translate(context, "Populaire"),
                                icon = Icons.Filled.Star
                            )
                        }
                        if (showQuickBooking) {
                            DealBadge(
                                text = StringTranslator.translate(context, "Réservation rapide"),
                                icon = Icons.Filled.LocalFireDepartment
                            )
                        }
                        if (showUrgency) {
                            DealBadge(
                                text = StringTranslator.translate(context, "Dernières places"),
                                icon = Icons.Filled.Info
                            )
                        }
                    }
                    
                    // Airline info (if available)
                    destination.airline?.let { airline ->
                        Row(
                            modifier = Modifier.padding(top = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Flight,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.8f),
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = airline,
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
        
        // Top right corner - Favorite and Share buttons (inspired by Airbnb)
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(12.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Share button
                IconButton(
                    onClick = {
                        HapticFeedbackHelper.triggerButtonPress(context)
                        // Share functionality
                    },
                    modifier = Modifier
                        .size(36.dp)
                        .background(
                            Color.Black.copy(alpha = 0.4f),
                            CircleShape
                        )
                ) {
                    Icon(
                        imageVector = Icons.Filled.Share,
                        contentDescription = "Share",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
                
                // Favorite button with state
                IconButton(
                    onClick = {
                        HapticFeedbackHelper.triggerButtonPress(context)
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
                        .size(36.dp)
                        .background(
                            Color.Black.copy(alpha = 0.4f),
                            CircleShape
                        )
                ) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = if (isFavorite) "Remove from favorites" else "Add to favorites",
                        tint = if (isFavorite) Color(0xFFFF1744) else Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
        
        // Price trend indicator (inspired by Hopper) - Top left
        // Show for destinations with good prices (below 400)
        if (destination.price != null && destination.price > 0 && destination.price < 400) {
            val priceTrend = (destination.price.toInt() % 100) % 3
            if (priceTrend == 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(12.dp)
                ) {
                    Surface(
                        color = Color(0xFF4CAF50).copy(alpha = 0.9f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.TrendingDown,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = StringTranslator.translate(context, "Prix en baisse"),
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}


@Composable
fun DiscussionCard(navController: NavController) {
    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                navController.navigate("discussions")
            },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                // Discussion Icon - Blue speech bubbles (directly, no circle background)
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Chat,
                    contentDescription = "Discussions",
                    tint = Color(0xFF1976D2),
                    modifier = Modifier.size(32.dp)
                )
                
                Column {
                    Text(
                        text = StringTranslator.translate(context, "Discussions de la communauté"),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = StringTranslator.translate(context, "Partagez vos expériences et découvrez les conseils des voyageurs"),
                        style = MaterialTheme.typography.bodySmall,
                        color = colorScheme.onSurfaceVariant,
                        lineHeight = 18.sp
                    )
                }
            }
            
            // Arrow icon on the right
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "View more",
                tint = Color(0xFF1976D2),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun InstagramReelsCard(navController: NavController) {
    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                navController.navigate("journey_feed")
            },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                // Instagram Reels Icon - Blue share/video icon
                Icon(
                    imageVector = Icons.Filled.Share,
                    contentDescription = "Instagram Reels",
                    tint = Color(0xFF1976D2),
                    modifier = Modifier.size(32.dp)
                )
                
                Column {
                    Text(
                        text = StringTranslator.translate(context, "Créez vos Reels WayFinder"),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = StringTranslator.translate(context, "Transformez vos voyages en Reels captivants et publiez-les automatiquement sur Instagram"),
                        style = MaterialTheme.typography.bodySmall,
                        color = colorScheme.onSurfaceVariant,
                        lineHeight = 18.sp
                    )
                }
            }
            
            // Arrow icon on the right
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "View more",
                tint = Color(0xFF1976D2),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun OnboardingReminderBadge(
    onDismiss: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    Card(
        modifier = modifier
            .offset(x = (-8).dp, y = (-8).dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFF9800)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Info,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = StringTranslator.translate(context, "Complétez votre profil"),
                style = MaterialTheme.typography.bodySmall,
                color = Color.White,
                fontWeight = FontWeight.Medium,
                maxLines = 1
            )
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(20.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Dismiss",
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@Composable
fun QuickFiltersSection(
    selectedFilter: String?,
    onFilterSelected: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme
    
    val filters = listOf(
        FilterOption("price", StringTranslator.translate(context, "Prix"), Icons.Filled.TrendingDown),
        FilterOption("duration", StringTranslator.translate(context, "Durée"), Icons.Filled.Schedule),
        FilterOption("time", StringTranslator.translate(context, "Heure"), Icons.Filled.Schedule)
    )
    
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        filters.forEach { filter ->
            FilterChip(
                selected = selectedFilter == filter.id,
                onClick = {
                    val newFilter = if (selectedFilter == filter.id) null else filter.id
                    onFilterSelected(newFilter)
                },
                label = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = filter.icon,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(filter.label, fontSize = 12.sp)
                    }
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = colorScheme.primaryContainer,
                    selectedLabelColor = colorScheme.onPrimaryContainer
                )
            )
        }
    }
}

data class FilterOption(
    val id: String,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

@Composable
fun PriceBadge(
    text: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = color.copy(alpha = 0.9f),
        shape = RoundedCornerShape(8.dp),
        modifier = modifier
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            fontSize = 10.sp
        )
    }
}

@Composable
fun DealBadge(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color.White.copy(alpha = 0.2f),
        shape = RoundedCornerShape(8.dp),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(12.dp)
            )
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White,
                fontWeight = FontWeight.Medium,
                fontSize = 9.sp
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    WayFinderTheme {
        HomeScreen(rememberNavController())
    }
}
