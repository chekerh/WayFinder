package tn.esprit.wayfinder.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import tn.esprit.wayfinder.models.FlightDestination
import tn.esprit.wayfinder.navigation.SELECTED_DESTINATION_KEY
import tn.esprit.wayfinder.ui.theme.WayFinderTheme
import tn.esprit.wayfinder.utils.StringTranslator

/**
 * Trip types available for selection - minimalistic version
 */
data class TripTypeOption(
    val id: String,
    val name: String,
    val description: String,
    val icon: ImageVector,
    val cardColor: Color
)

/**
 * Screen for selecting the type of trip
 * Minimalistic design with consistent theme colors
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
                icon = Icons.Outlined.Work,
                cardColor = Color(0xFF1E3A5F) // Dark blue
            ),
            TripTypeOption(
                id = "leisure",
                name = "Loisirs",
                description = "Détente et découverte sans contraintes",
                icon = Icons.Outlined.WbSunny,
                cardColor = Color(0xFF4A90E2) // Light blue
            ),
            TripTypeOption(
                id = "honeymoon",
                name = "Lune de miel",
                description = "Moments romantiques dans des lieux d'exception",
                icon = Icons.Outlined.FavoriteBorder,
                cardColor = Color(0xFFE91E63) // Pink
            ),
            TripTypeOption(
                id = "family",
                name = "Famille",
                description = "Aventures pour petits et grands",
                icon = Icons.Outlined.People,
                cardColor = Color(0xFFFF9800) // Orange
            ),
            TripTypeOption(
                id = "adventure",
                name = "Aventure",
                description = "Exploration et sensations fortes",
                icon = Icons.Outlined.Hiking,
                cardColor = Color(0xFF4CAF50) // Green
            ),
            TripTypeOption(
                id = "solo",
                name = "Solo",
                description = "Liberté totale pour voyageur indépendant",
                icon = Icons.Outlined.Person,
                cardColor = Color(0xFF9C27B0) // Purple
            ),
            TripTypeOption(
                id = "wellness",
                name = "Bien-être",
                description = "Ressourcement corps et esprit",
                icon = Icons.Outlined.Spa,
                cardColor = Color(0xFF009688) // Teal
            ),
            TripTypeOption(
                id = "backpacking",
                name = "Backpacking",
                description = "Voyage économique et authentique",
                icon = Icons.Outlined.Backpack,
                cardColor = Color(0xFF795548) // Brown
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
                                text = "${it.city ?: it.name}, ${it.country}",
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
                    containerColor = colorScheme.background
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
                    Button(
                        onClick = {
                            // Save trip type and destination on this back stack entry,
                            // so Accommodations and Reservation screens can reliably read it
                            navController.currentBackStackEntry
                                ?.savedStateHandle
                                ?.apply {
                                    set("trip_type", selectedType)
                                    destination?.let { set(SELECTED_DESTINATION_KEY, it) }
                                }
                            navController.navigate("accommodations/${destinationId ?: ""}/$selectedType")
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colorScheme.primary
                        )
                    ) {
                        Text(
                            text = StringTranslator.translate(context, "Continuer"),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = StringTranslator.translate(context, "Quel est le but de votre voyage ?"),
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 20.dp)
            )
            
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 100.dp)
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
        targetValue = if (isSelected) 1.05f else 1f,
        animationSpec = tween(durationMillis = 200),
        label = "scale"
    )
    
    val borderWidth by animateFloatAsState(
        targetValue = if (isSelected) 3f else 0f,
        animationSpec = tween(durationMillis = 200),
        label = "borderWidth"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
            .scale(scale)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = tripType.cardColor
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 8.dp else 4.dp
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (isSelected) {
                        Modifier.border(
                            width = borderWidth.dp,
                            color = Color.White,
                            shape = RoundedCornerShape(20.dp)
                        )
                    } else {
                        Modifier
                    }
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Icon
                Icon(
                    imageVector = tripType.icon,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = Color.White
                )
                
                // Title and description
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = tripType.name,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = tripType.description,
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.9f),
                        lineHeight = 14.sp
                    )
                }
            }
            
            // Checkmark overlay when selected
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = "Selected",
                        modifier = Modifier.size(18.dp),
                        tint = tripType.cardColor
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
