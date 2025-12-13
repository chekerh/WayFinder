package tn.esprit.wayfinder.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import tn.esprit.wayfinder.models.FlightDestination
import tn.esprit.wayfinder.navigation.SELECTED_DESTINATION_KEY
import tn.esprit.wayfinder.ui.components.CustomBottomNavigationBar
import tn.esprit.wayfinder.ui.theme.WayFinderTheme
import tn.esprit.wayfinder.utils.StringTranslator

/**
 * Trip types available for selection
 */
data class TripTypeOption(
    val id: String,
    val name: String,
    val description: String,
    val icon: ImageVector,
    val gradient: List<Color>,
    val recommendedStays: List<String>
)

/**
 * Screen for selecting the type of trip
 * Affects accommodation filtering and activity recommendations
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripTypeSelectionScreen(
    navController: NavController,
    destinationId: String? = null
) {
    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme
    var selectedType by remember { mutableStateOf<String?>(null) }
    
    // Get destination from previous screen
    val destination = remember(destinationId) {
        navController.previousBackStackEntry
            ?.savedStateHandle
            ?.get<FlightDestination>(SELECTED_DESTINATION_KEY)
    }
    
    val tripTypes = remember {
        listOf(
            TripTypeOption(
                id = "business",
                name = "Affaires",
                description = "Voyages professionnels avec confort et efficacité",
                icon = Icons.Filled.Business,
                gradient = listOf(Color(0xFF1A237E), Color(0xFF3949AB)),
                recommendedStays = listOf("Hôtels 4-5★", "Suites", "Business Hotels")
            ),
            TripTypeOption(
                id = "leisure",
                name = "Loisirs",
                description = "Détente et découverte sans contraintes",
                icon = Icons.Filled.BeachAccess,
                gradient = listOf(Color(0xFF0277BD), Color(0xFF4FC3F7)),
                recommendedStays = listOf("Hôtels", "Résorts", "Appartements")
            ),
            TripTypeOption(
                id = "honeymoon",
                name = "Lune de miel",
                description = "Moments romantiques dans des lieux d'exception",
                icon = Icons.Filled.Favorite,
                gradient = listOf(Color(0xFFAD1457), Color(0xFFF48FB1)),
                recommendedStays = listOf("Résorts", "Boutique Hotels", "Suites Romantiques")
            ),
            TripTypeOption(
                id = "family",
                name = "Famille",
                description = "Aventures pour petits et grands",
                icon = Icons.Filled.FamilyRestroom,
                gradient = listOf(Color(0xFFE65100), Color(0xFFFFB74D)),
                recommendedStays = listOf("Appartements", "Hôtels Famille", "Villas")
            ),
            TripTypeOption(
                id = "adventure",
                name = "Aventure",
                description = "Exploration et sensations fortes",
                icon = Icons.Filled.Terrain,
                gradient = listOf(Color(0xFF2E7D32), Color(0xFF81C784)),
                recommendedStays = listOf("Éco-lodges", "Auberges", "Camping")
            ),
            TripTypeOption(
                id = "solo",
                name = "Solo",
                description = "Liberté totale pour voyageur indépendant",
                icon = Icons.Filled.Person,
                gradient = listOf(Color(0xFF6A1B9A), Color(0xFFBA68C8)),
                recommendedStays = listOf("Auberges", "Hôtels", "Hostels")
            ),
            TripTypeOption(
                id = "wellness",
                name = "Bien-être",
                description = "Ressourcement corps et esprit",
                icon = Icons.Filled.Spa,
                gradient = listOf(Color(0xFF00695C), Color(0xFF80CBC4)),
                recommendedStays = listOf("Spa Resorts", "Retreats", "Wellness Hotels")
            ),
            TripTypeOption(
                id = "backpacking",
                name = "Backpacking",
                description = "Voyage économique et authentique",
                icon = Icons.Filled.Backpack,
                gradient = listOf(Color(0xFF795548), Color(0xFFBCAAA4)),
                recommendedStays = listOf("Hostels", "Auberges", "Budget Hotels")
            )
        )
    }

    Scaffold(
        containerColor = colorScheme.background,
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text(
                            text = StringTranslator.translate(context, "Type de voyage"),
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                        destination?.let {
                            Text(
                                text = "${it.name}, ${it.country}",
                                fontSize = 14.sp,
                                color = colorScheme.onSurfaceVariant
                            )
                        }
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
            // Continue button when type is selected
            if (selectedType != null) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shadowElevation = 8.dp,
                    color = colorScheme.surface
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Button(
                            onClick = {
                                // Save trip type and navigate to accommodations
                                navController.currentBackStackEntry
                                    ?.savedStateHandle
                                    ?.set("trip_type", selectedType)
                                navController.navigate("accommodations/${destinationId ?: ""}/$selectedType")
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colorScheme.primary
                            )
                        ) {
                            Text(
                                text = StringTranslator.translate(context, "Continuer"),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            } else {
                CustomBottomNavigationBar(navController = navController)
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = StringTranslator.translate(context, "Quel est le but de votre voyage ?"),
                fontSize = 16.sp,
                color = colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(tripTypes, key = { it.id }) { tripType ->
                    TripTypeCard(
                        tripType = tripType,
                        isSelected = selectedType == tripType.id,
                        onClick = { selectedType = tripType.id }
                    )
                }
            }
        }
    }
}

@Composable
fun TripTypeCard(
    tripType: TripTypeOption,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.02f else 1f,
        animationSpec = spring(),
        label = "scale"
    )
    
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) colorScheme.primary else Color.Transparent,
        label = "borderColor"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .scale(scale)
            .border(
                width = if (isSelected) 3.dp else 0.dp,
                color = borderColor,
                shape = RoundedCornerShape(20.dp)
            )
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 8.dp else 4.dp
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(tripType.gradient)
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Icon and checkmark
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = tripType.icon,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = Color.White
                    )
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = "Selected",
                            modifier = Modifier.size(24.dp),
                            tint = Color.White
                        )
                    }
                }
                
                // Title and description
                Column {
                    Text(
                        text = tripType.name,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = tripType.description,
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.85f),
                        lineHeight = 14.sp,
                        maxLines = 2
                    )
                }
            }
            
            // Recommended stays badge
            if (isSelected) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = Color.White.copy(alpha = 0.2f)
                ) {
                    Text(
                        text = tripType.recommendedStays.first(),
                        fontSize = 10.sp,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun TripTypeSelectionScreenPreview() {
    WayFinderTheme {
        TripTypeSelectionScreen(rememberNavController())
    }
}

