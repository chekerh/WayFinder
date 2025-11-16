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
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PersonOutline
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material3.*
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
import tn.esprit.wayfinder.presentation.auth.ViewModelFactory
import tn.esprit.wayfinder.ui.theme.WayFinderTheme
import tn.esprit.wayfinder.viewmodels.CatalogViewModel
import tn.esprit.wayfinder.viewmodels.CatalogUiState

data class Region(val name: String, val imageRes: Int, val filterCountries: List<String> = emptyList())

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavController) {
    val context = LocalContext.current
    val catalogViewModel: CatalogViewModel = viewModel(factory = ViewModelFactory(context.applicationContext as Application))
    val uiState by catalogViewModel.uiState.collectAsState()

    // Selected region state
    var selectedRegion by remember { mutableStateOf<String?>(null) }
    
    // Load flights on first composition
    LaunchedEffect(Unit) {
        catalogViewModel.loadRecommendedFlights()
    }

    // Regions data with country filters
    val regions = listOf(
        Region(
            name = "Préférences",
            imageRes = R.drawable.travel_image, // Not used - we use Star icon instead
            filterCountries = emptyList() // No filter - show personalized preferences
        ),
        Region(
            name = "Europe",
            imageRes = R.drawable.europe,
            filterCountries = listOf("France", "United Kingdom", "Italy", "Spain", "Netherlands", "Germany", "Switzerland", "Belgium", "Portugal", "Greece", "Austria", "Sweden", "Norway", "Denmark", "Finland", "Poland", "Czech Republic", "Hungary", "Ireland")
        ),
        Region(
            name = "Asie",
            imageRes = R.drawable.asia,
            filterCountries = listOf("China", "Japan", "India", "Thailand", "Singapore", "Malaysia", "Indonesia", "South Korea", "Vietnam", "Philippines", "UAE", "Saudi Arabia", "Turkey", "Israel")
        ),
        Region(
            name = "Amerique",
            imageRes = R.drawable.travel_image,
            filterCountries = listOf("United States", "Canada", "Mexico", "Brazil", "Argentina", "Chile", "Colombia", "Peru")
        ),
        Region(
            name = "Australie",
            imageRes = R.drawable.australia,
            filterCountries = listOf("Australia", "New Zealand", "Fiji")
        )
    )

    Scaffold(
        containerColor = Color(0xFFEAF2FF),
        bottomBar = { CustomBottomNavigationBar(navController = navController) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = paddingValues.calculateBottomPadding())
        ) {
            Spacer(modifier = Modifier.height(48.dp)) // Status bar padding
            TopBar(context = context)
            Spacer(modifier = Modifier.height(28.dp))
            
            Column {
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Text(
                        text = "Personnalisé par Gemini",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text(
                        text = "Voyages adaptés à vos préférences",
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
                            text = "Comparateur avec Gemini",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Voir tous",
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
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(340.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
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
                            
                            DestinationsSection(
                                destinations = filteredDestinations,
                                navController = navController
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
                                        Text("Réessayer")
                                    }
                                }
                            }
                        }
                        else -> {}
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun TopBar(context: android.content.Context) {
    val tokenManager = remember { TokenManager(context) }
    val user = remember { tokenManager.getUser() }
    
    // Get user's first name or username, fallback to "Explorateur"
    val userName = user?.firstName?.takeIf { it.isNotBlank() } 
        ?: user?.username?.takeIf { it.isNotBlank() }
        ?: "Explorateur"
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = painterResource(id = R.drawable.europe),
                contentDescription = "User Avatar",
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Salut, $userName",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
        
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.6f))
                .clickable { /* Handle notification click */ },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Notifications,
                contentDescription = "Notifications",
                tint = Color(0xFF0D47A1)
            )
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
fun DestinationsSection(destinations: List<tn.esprit.wayfinder.models.FlightDestination>, navController: NavController) {
    if (destinations.isEmpty()) {
        Text(
            text = "Aucune destination disponible",
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
                    navController.navigate("flight_detail/${destinations[page].id}")
                },
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
        ) {
            DestinationCardContent(destination = destinations[page])
        }
    }
}

@Composable
fun DestinationCardContent(destination: tn.esprit.wayfinder.models.FlightDestination) {
    // State for favorite button
    var isFavorite by remember { mutableStateOf(false) }
    
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
                Text(
                    text = "${destination.price.toInt()} ${destination.currency}",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
        
        // Favorite button with state
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(12.dp)
        ) {
            IconButton(
                onClick = { isFavorite = !isFavorite },
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
fun CustomBottomNavigationBar(navController: NavController? = null) {
    var selectedIndex by remember { mutableStateOf(0) }
    
    NavigationBar(
        containerColor = Color.White,
        tonalElevation = 8.dp
    ) {
        NavigationBarItem(
            selected = selectedIndex == 0,
            onClick = { 
                selectedIndex = 0
                navController?.navigate("home") {
                    popUpTo("home") { inclusive = true }
                }
            },
            icon = {
                Icon(
                    imageVector = Icons.Filled.Home,
                    contentDescription = "Home",
                    tint = if (selectedIndex == 0) Color(0xFF1976D2) else Color(0xFF9E9E9E)
                )
            },
            label = { Text("Accueil", style = MaterialTheme.typography.labelSmall) }
        )
        NavigationBarItem(
            selected = selectedIndex == 1,
            onClick = { 
                selectedIndex = 1
                // TODO: Navigate to favorites screen
            },
            icon = {
                Icon(
                    imageVector = Icons.Default.FavoriteBorder,
                    contentDescription = "Favorites",
                    tint = if (selectedIndex == 1) Color(0xFF1976D2) else Color(0xFF9E9E9E)
                )
            },
            label = { Text("Favoris", style = MaterialTheme.typography.labelSmall) }
        )
        NavigationBarItem(
            selected = selectedIndex == 2,
            onClick = { 
                selectedIndex = 2
                // TODO: Navigate to chat screen
            },
            icon = {
                Icon(
                    imageVector = Icons.Outlined.ChatBubbleOutline,
                    contentDescription = "Chat",
                    tint = if (selectedIndex == 2) Color(0xFF1976D2) else Color(0xFF9E9E9E)
                )
            },
            label = { Text("Chat", style = MaterialTheme.typography.labelSmall) }
        )
        NavigationBarItem(
            selected = selectedIndex == 3,
            onClick = { 
                selectedIndex = 3
                // TODO: Navigate to profile screen
            },
            icon = {
                Icon(
                    imageVector = Icons.Default.PersonOutline,
                    contentDescription = "Profile",
                    tint = if (selectedIndex == 3) Color(0xFF1976D2) else Color(0xFF9E9E9E)
                )
            },
            label = { Text("Profil", style = MaterialTheme.typography.labelSmall) }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    WayFinderTheme {
        HomeScreen(rememberNavController())
    }
}
