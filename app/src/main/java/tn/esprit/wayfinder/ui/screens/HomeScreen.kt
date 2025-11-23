package tn.esprit.wayfinder.ui.screens

import android.app.Application
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material3.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import tn.esprit.wayfinder.utils.StringTranslator
import tn.esprit.wayfinder.viewmodels.CatalogViewModel
import tn.esprit.wayfinder.viewmodels.CatalogUiState
import tn.esprit.wayfinder.viewmodels.FavoritesViewModel
import tn.esprit.wayfinder.viewmodels.NotificationsViewModel
import tn.esprit.wayfinder.viewmodels.NotificationsUiState

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

    // Selected region state
    var selectedRegion by remember { mutableStateOf<String?>(null) }
    
    // Load notifications count on first composition
    LaunchedEffect(Unit) {
        notificationsViewModel.refreshUnreadCount()
    }
    
    // Load flights on first composition and when region changes
    LaunchedEffect(selectedRegion) {
        catalogViewModel.loadRecommendedFlights(showAll = false)
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
            filterCountries = listOf("China", "Japan", "India", "Thailand", "Singapore", "Malaysia", "Indonesia", "South Korea", "Vietnam", "Philippines", "UAE", "Saudi Arabia", "Turkey", "Israel")
        ),
        Region(
            name = StringTranslator.translate(context, "Amerique"),
            imageRes = R.drawable.america,
            filterCountries = listOf("United States", "Canada", "Mexico", "Brazil", "Argentina", "Chile", "Colombia", "Peru")
        ),
        Region(
            name = StringTranslator.translate(context, "Australie"),
            imageRes = R.drawable.australia,
            filterCountries = listOf("Australia", "New Zealand", "Fiji")
        )
    )

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = { CustomBottomNavigationBar(navController = navController) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = paddingValues.calculateBottomPadding())
        ) {
            Spacer(modifier = Modifier.height(48.dp)) // Status bar padding
            TopBar(context = context, navController = navController, notificationsViewModel = notificationsViewModel)
            Spacer(modifier = Modifier.height(28.dp))
            
            Column {
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        Text(
                            text = StringTranslator.translate(context, "Personnalisé par Gemini"),
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold
                        )
                        // Sparkle icon for personalization
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = Color(0xFFFFC107),
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Text(
                        text = StringTranslator.translate(context, "Voyages adaptés à vos préférences"),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                }
                
                Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                    RegionSection(
                        regions = regions,
                        selectedRegion = selectedRegion,
                        onRegionSelected = { regionName ->
                            selectedRegion = if (selectedRegion == regionName) null else regionName
                        }
                    )
                    Spacer(modifier = Modifier.height(32.dp))
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = StringTranslator.translate(context, "Comparateur avec Gemini"),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = StringTranslator.translate(context, "Voir tous"),
                            color = Color(0xFF1976D2),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.clickable {
                                // Pass selected region as route argument
                                val route = if (selectedRegion != null) {
                                    "all_flights/${selectedRegion}"
                                } else {
                                    "all_flights/null"
                                }
                                navController.navigate(route)
                            }
                        )
                    }
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
                            val filteredDestinations = if (selectedRegion != null && selectedRegion != "Préférences") {
                                val selectedRegionData = regions.find { it.name == selectedRegion }
                                val filterCountries = selectedRegionData?.filterCountries ?: emptyList()
                                if (filterCountries.isNotEmpty()) {
                                    state.destinations.filter { destination ->
                                        filterCountries.any { country ->
                                            destination.country.contains(country, ignoreCase = true)
                                        }
                                    }
                                } else {
                                    state.destinations
                                }
                            } else {
                                // Show all destinations for "Préférences" or no selection
                                state.destinations
                            }
                            
                            // Show at least 5-6 flights on home screen
                            val displayDestinations = filteredDestinations.take(6)

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
                            
                            EnhancedDestinationsSection(
                                destinations = displayDestinations,
                                navController = navController,
                                favoritesViewModel = favoritesViewModel
                            )
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
            
            // Discussion Section - Placed after Comparateur avec Gemini section
            Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                DiscussionCard(navController = navController)
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun TopBar(
    context: android.content.Context,
    navController: NavController,
    notificationsViewModel: NotificationsViewModel
) {
    val tokenManager = remember { TokenManager(context) }
    val user = remember { tokenManager.getUser() }
    val notificationsState by notificationsViewModel.uiState.collectAsState()
    
    // Get user's first name or username, fallback to "Explorateur"
    val userName = user?.firstName?.takeIf { it.isNotBlank() } 
        ?: user?.username?.takeIf { it.isNotBlank() }
        ?: "Explorateur"
    
    // Personalized greeting based on time of day
    val greeting = remember {
        val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        when {
            hour < 12 -> StringTranslator.translate(context, "Bonjour")
            hour < 18 -> StringTranslator.translate(context, "Bon après-midi")
            else -> StringTranslator.translate(context, "Bonsoir")
        }
    }
    
    val unreadCount = when (val state = notificationsState) {
        is NotificationsUiState.Success -> state.unreadCount
        else -> 0
    }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box {
                val userImageUrl = user?.profileImageUrl?.let { url ->
                    if (url.startsWith("http")) url else "https://wayfinder-api-w92x.onrender.com$url"
                }
                if (userImageUrl != null) {
                    AsyncImage(
                        model = userImageUrl,
                        contentDescription = "User Avatar",
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .clickable { 
                HapticFeedbackHelper.triggerButtonPress(context)
                navController.navigate("profile") 
            },
                        contentScale = ContentScale.Crop,
                        placeholder = painterResource(id = R.drawable.europe),
                        error = painterResource(id = R.drawable.europe)
                    )
                } else {
                    Image(
                        painter = painterResource(id = R.drawable.europe),
                        contentDescription = "User Avatar",
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .clickable { 
                HapticFeedbackHelper.triggerButtonPress(context)
                navController.navigate("profile") 
            },
                        contentScale = ContentScale.Crop
                    )
                }
                
                // Show badge if onboarding was skipped
                if (user?.onboardingSkipped == true) {
                    OnboardingReminderBadge(
                        onDismiss = {
                            // Update user to remove skipped flag
                            val updatedUser = user.copy(onboardingSkipped = false)
                            tokenManager.saveUser(updatedUser)
                        },
                        onClick = {
                            navController.navigate("onboarding")
                        },
                        modifier = Modifier.align(Alignment.TopEnd)
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "$greeting, $userName! 👋",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    // Compact points badge
                    if (user?.totalPoints != null && user.totalPoints > 0) {
                        CompactPointsBadge(
                            points = user.totalPoints,
                            modifier = Modifier.scale(0.85f)
                        )
                    }
                }
                Text(
                    text = StringTranslator.translate(context, "Prêt pour votre prochaine aventure?"),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.6f))
                .clickable { 
                    HapticFeedbackHelper.triggerButtonPress(context)
                    navController.navigate("notifications") 
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Notifications,
                contentDescription = "Notifications",
                tint = Color(0xFF0D47A1)
            )
            if (unreadCount > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFF44336)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (unreadCount > 9) "9+" else unreadCount.toString(),
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
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
        horizontalArrangement = Arrangement.spacedBy(16.dp)
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
    Row(
        modifier = Modifier
            .background(
                if (isSelected) Color(0xFF1976D2) else Color.White,
                RoundedCornerShape(20.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Use icon for "Préférences", image for others
        if (region.name == "Préférences") {
            Icon(
                imageVector = Icons.Filled.Star,
                contentDescription = region.name,
                modifier = Modifier.size(24.dp),
                tint = if (isSelected) Color.White else Color(0xFFFFC107)
            )
        } else {
            Image(
                painter = painterResource(id = region.imageRes),
                contentDescription = region.name,
                modifier = Modifier.size(32.dp).clip(CircleShape)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = region.name,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) Color.White else Color.Black
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
    if (destinations.isEmpty()) {
        Text(
            text = StringTranslator.translate(context, "Aucune destination disponible"),
            color = Color.Gray,
            modifier = Modifier.padding(16.dp)
        )
        return
    }

    // Show all destinations - user wants to see multiple flights
    val pagerState = rememberPagerState(pageCount = { destinations.size })

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
    if (destinations.isEmpty()) {
        Text(
            text = StringTranslator.translate(context, "Aucune destination disponible"),
            color = Color.Gray,
            modifier = Modifier.padding(16.dp)
        )
        return
    }

    // Show all destinations - user wants to see multiple flights
    val pagerState = rememberPagerState(pageCount = { destinations.size })

    HorizontalPager(
        state = pagerState,
        contentPadding = PaddingValues(horizontal = 80.dp),
        pageSpacing = (-120).dp
    ) { page ->
        val pageOffset = ((pagerState.currentPage - page) + pagerState.currentPageOffsetFraction).absoluteValue
        val selectedDestination = destinations[page]

        Box(
            modifier = Modifier
                .zIndex(1f - pageOffset)
                .graphicsLayer {
                    alpha = lerp(start = 0.5f, stop = 1f, fraction = 1f - pageOffset.coerceIn(0f, 1f))
                    scaleY = lerp(start = 0.85f, stop = 1f, fraction = 1f - pageOffset.coerceIn(0f, 1f))
                }
                .width(290.dp)
                .height(340.dp)
        ) {
            SwipeableDestinationCard(
                destination = selectedDestination,
                favoritesViewModel = favoritesViewModel,
                onCardClick = {
                    navController.currentBackStackEntry?.savedStateHandle?.set(
                        SELECTED_DESTINATION_KEY,
                        selectedDestination
                    )
                    navController.navigate("flight_detail/${selectedDestination.id}")
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
fun DestinationCardContent(
    destination: tn.esprit.wayfinder.models.FlightDestination,
    favoritesViewModel: FavoritesViewModel
) {
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Text(
                        text = "${destination.price.toInt()} ${destination.currency}",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    // Social proof badge
                    Surface(
                        color = Color(0xFF4CAF50).copy(alpha = 0.9f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "Popular",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
        
        // Favorite button with state
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(12.dp)
        ) {
            IconButton(
                onClick = {
                    val context = LocalContext.current
                    tn.esprit.wayfinder.utils.HapticFeedbackHelper.triggerButtonPress(context)
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
                    .size(40.dp)
                    .background(Color.White.copy(alpha = 0.3f), CircleShape)
            ) {
                Icon(
                    imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    contentDescription = if (isFavorite) "Remove from favorites" else "Add to favorites",
                    tint = if (isFavorite) Color(0xFFFF1744) else Color.White
                )
            }
        }
    }
}


@Composable
fun DiscussionCard(navController: NavController) {
    val context = LocalContext.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                navController.navigate("discussions")
            },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Chat Icon
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(
                            Color(0xFF1976D2).copy(alpha = 0.1f),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Chat,
                        contentDescription = "Discussions",
                        tint = Color(0xFF1976D2),
                        modifier = Modifier.size(28.dp)
                    )
                }
                
                Column {
                    Text(
                        text = StringTranslator.translate(context, "Discussions de la communauté"),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = StringTranslator.translate(context, "Partagez vos expériences et découvrez les conseils des voyageurs"),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            }
            
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "Voir plus",
                tint = Color(0xFF1976D2),
                modifier = Modifier.size(24.dp)
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
                imageVector = Icons.Default.Info,
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
                    imageVector = Icons.Default.Close,
                    contentDescription = "Dismiss",
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
            }
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
