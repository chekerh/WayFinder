package tn.esprit.wayfinder.ui.screens

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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import tn.esprit.wayfinder.R
import tn.esprit.wayfinder.ui.components.CustomBottomNavigationBar
import tn.esprit.wayfinder.ui.theme.WayFinderTheme

data class ActivityCategory(
    val name: String,
    val icon: ImageVector,
    val color: Color
)

data class Activity(
    val id: String,
    val name: String,
    val imageUrl: String,
    val category: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DuringTravelScreen(navController: NavController) {
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    
    val categories = listOf(
        ActivityCategory("Musées", Icons.Default.Museum, Color(0xFF1976D2)),
        ActivityCategory("Hôtels", Icons.Default.Hotel, Color(0xFFFFC107)),
        ActivityCategory("Restaurants", Icons.Default.Restaurant, Color(0xFF4CAF50)),
        ActivityCategory("Activités", Icons.Default.LocalActivity, Color(0xFFE91E63))
    )
    
    // TODO: Load activities from ViewModel/Repository
    val activities = remember {
        listOf(
            Activity("1", "Louvre Museum", "https://images.unsplash.com/photo-1591123720363-2633c8d2b1e4", "Musées"),
            Activity("2", "Eiffel Tower", "https://images.unsplash.com/photo-1511739001486-6bfe10ce785f", "Activités"),
            Activity("3", "Le Jules Verne", "https://images.unsplash.com/photo-1517248135467-4c7edcad34c4", "Restaurants"),
            Activity("4", "Hotel Ritz", "https://images.unsplash.com/photo-1566073771259-6a8506099945", "Hôtels"),
            Activity("5", "Notre-Dame", "https://images.unsplash.com/photo-1502602898669-a90b7b675c70", "Musées"),
            Activity("6", "Seine Cruise", "https://images.unsplash.com/photo-1502602898669-a90b7b675c70", "Activités")
        )
    }
    
    val filteredActivities = if (selectedCategory != null) {
        activities.filter { it.category == selectedCategory }
    } else {
        activities
    }

    Scaffold(
        containerColor = Color(0xFFF0F8FF),
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
                onClick = {
                    // TODO: Show discovery options
                },
                containerColor = Color(0xFFFFC107),
                contentColor = Color.Black
            ) {
                Text("Découvrir", fontWeight = FontWeight.Bold)
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Category Filter
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
                                selectedCategory = if (selectedCategory == category.name) null else category.name
                            }
                        )
                    }
                }
                
                // Activities Grid
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredActivities) { activity ->
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
    activity: Activity,
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
                model = activity.imageUrl,
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
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DuringTravelScreenPreview() {
    WayFinderTheme {
        DuringTravelScreen(rememberNavController())
    }
}

