package tn.esprit.wayfinder.ui.screens

import android.app.Application
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import tn.esprit.wayfinder.R
import tn.esprit.wayfinder.models.TravelActivity
import tn.esprit.wayfinder.presentation.auth.ViewModelFactory
import tn.esprit.wayfinder.ui.components.CustomBottomNavigationBar
import tn.esprit.wayfinder.ui.theme.WayFinderTheme
import tn.esprit.wayfinder.viewmodels.ActivitiesUiState
import tn.esprit.wayfinder.viewmodels.ActivitiesViewModel

data class ActivityCategory(
    val name: String,
    val icon: ImageVector,
    val color: Color
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DuringTravelScreen(navController: NavController) {
    val context = LocalContext.current
    val activitiesViewModel: ActivitiesViewModel = viewModel(
        factory = ViewModelFactory(context.applicationContext as Application)
    )
    val uiState by activitiesViewModel.uiState.collectAsState()
    val selectedCategory by activitiesViewModel.selectedCategoryFlow.collectAsState()
    var selectedCity by remember { mutableStateOf("Paris") }

    LaunchedEffect(Unit) {
        activitiesViewModel.loadActivities(city = selectedCity)
    }

    LaunchedEffect(uiState) {
        if (uiState is ActivitiesUiState.Success) {
            selectedCity = (uiState as ActivitiesUiState.Success).city
        }
    }

    val activities = when (val state = uiState) {
        is ActivitiesUiState.Success -> state.activities
        else -> emptyList()
    }

    val categories = when (val state = uiState) {
        is ActivitiesUiState.Success -> state.categories.map { toCategoryChip(it) }
        else -> listOf(
            ActivityCategory("Musées", Icons.Default.Museum, Color(0xFF1976D2)),
            ActivityCategory("Hôtels", Icons.Default.Hotel, Color(0xFFFFC107)),
            ActivityCategory("Restaurants", Icons.Default.Restaurant, Color(0xFF4CAF50)),
            ActivityCategory("Activités", Icons.Default.LocalActivity, Color(0xFFE91E63))
        )
    }

    val availableCities = listOf("Paris", "Rome", "Dubai", "Tunis", "Barcelone")

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Text(
                        "Lors du Voyage",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    ) 
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        bottomBar = { CustomBottomNavigationBar(navController = navController) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { activitiesViewModel.retry() },
                containerColor = Color(0xFFFFC107),
                contentColor = Color.Black
            ) {
                Icon(Icons.Default.Refresh, contentDescription = "Rafraîchir")
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // City selector
                Text(
                    text = "Sélectionnez une ville",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    fontWeight = FontWeight.Medium
                )
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(availableCities) { city ->
                        CityChip(
                            city = city,
                            isSelected = city.equals(selectedCity, ignoreCase = true),
                            onClick = {
                                selectedCity = city
                                activitiesViewModel.onCitySelected(city)
                            }
                        )
                    }
                }

                // Category Filter
                Text(
                    text = "Catégories",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    fontWeight = FontWeight.Medium
                )
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(categories) { category ->
                        CategoryChip(
                            category = category,
                            isSelected = selectedCategory == category.name,
                            onClick = {
                                activitiesViewModel.onCategorySelected(category.name)
                            }
                        )
                    }
                }

                when (val state = uiState) {
                    is ActivitiesUiState.Loading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                    is ActivitiesUiState.Error -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Text(text = state.message, color = Color.Red, fontWeight = FontWeight.Medium)
                                Button(onClick = { activitiesViewModel.retry() }) {
                                    Text("Réessayer")
                                }
                            }
                        }
                    }
                    is ActivitiesUiState.Success -> {
                        if (state.activities.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Aucune activité trouvée pour ${state.city}")
                            }
                        } else {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(2),
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(state.activities) { activity ->
                                    ActivityCard(
                                        activity = activity,
                                        onClick = {
                                            // TODO: Navigate to activity details
                                        }
                                    )
                                }
                            }
                        }
                    }
                    else -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Sélectionnez une ville pour commencer")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CategoryChip(
    category: ActivityCategory,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        selected = isSelected,
        onClick = onClick,
        label = { 
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = category.icon,
                    contentDescription = category.name,
                    modifier = Modifier.size(20.dp)
                )
                Text(category.name)
            }
        },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = category.color.copy(alpha = 0.2f),
            selectedLabelColor = category.color
        )
    )
}

@Composable
fun ActivityCard(
    activity: TravelActivity,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = activity.imageUrl ?: R.drawable.travel_image,
                contentDescription = activity.name,
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
                        androidx.compose.ui.graphics.Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.7f)
                            )
                        )
                    )
            )
            
            // Name
            Text(
                text = activity.name,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp),
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            activity.category.let {
                Text(
                    text = it,
                    color = Color.White,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(12.dp)
                        .background(
                            color = Color.Black.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
fun CityChip(
    city: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    AssistChip(
        onClick = onClick,
        label = { Text(city) },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = city,
                tint = if (isSelected) Color.White else Color(0xFF1976D2)
            )
        },
        colors = AssistChipDefaults.assistChipColors(
            containerColor = if (isSelected) Color(0xFF1976D2) else Color.White,
            labelColor = if (isSelected) Color.White else Color.Black
        )
    )
}

private fun toCategoryChip(category: String): ActivityCategory {
    val normalized = category.lowercase()
    return when {
        normalized.contains("mus") -> ActivityCategory(category, Icons.Default.Museum, Color(0xFF1976D2))
        normalized.contains("hotel") || normalized.contains("hôtel") -> ActivityCategory(category, Icons.Default.Hotel, Color(0xFFFFC107))
        normalized.contains("rest") -> ActivityCategory(category, Icons.Default.Restaurant, Color(0xFF4CAF50))
        normalized.contains("park") -> ActivityCategory(category, Icons.Default.Nature, Color(0xFF81C784))
        else -> ActivityCategory(category, Icons.Default.LocalActivity, Color(0xFFE91E63))
    }
}

@Preview(showBackground = true)
@Composable
fun DuringTravelScreenPreview() {
    WayFinderTheme {
        DuringTravelScreen(rememberNavController())
    }
}

