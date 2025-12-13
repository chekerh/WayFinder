package tn.esprit.wayfinder.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
    val icon: ImageVector
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
                description = "Voyages professionnels",
                icon = Icons.Outlined.Work
            ),
            TripTypeOption(
                id = "leisure",
                name = "Loisirs",
                description = "Détente et découverte",
                icon = Icons.Outlined.WbSunny
            ),
            TripTypeOption(
                id = "honeymoon",
                name = "Lune de miel",
                description = "Escapade romantique",
                icon = Icons.Outlined.FavoriteBorder
            ),
            TripTypeOption(
                id = "family",
                name = "Famille",
                description = "Aventures en famille",
                icon = Icons.Outlined.People
            ),
            TripTypeOption(
                id = "adventure",
                name = "Aventure",
                description = "Exploration nature",
                icon = Icons.Outlined.Hiking
            ),
            TripTypeOption(
                id = "solo",
                name = "Solo",
                description = "Voyage indépendant",
                icon = Icons.Outlined.Person
            ),
            TripTypeOption(
                id = "wellness",
                name = "Bien-être",
                description = "Repos et relaxation",
                icon = Icons.Outlined.Spa
            ),
            TripTypeOption(
                id = "backpacking",
                name = "Backpacking",
                description = "Voyage économique",
                icon = Icons.Outlined.Backpack
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
                            // Save trip type and navigate to accommodations
                            navController.currentBackStackEntry
                                ?.savedStateHandle
                                ?.set("trip_type", selectedType)
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            item {
                Text(
                    text = StringTranslator.translate(context, "Quel est le but de votre voyage ?"),
                    fontSize = 15.sp,
                    color = colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            
            items(tripTypes, key = { it.id }) { tripType ->
                TripTypeCard(
                    tripType = tripType,
                    isSelected = selectedType == tripType.id,
                    onClick = { selectedType = tripType.id }
                )
            }
            
            // Bottom spacing
            item {
                Spacer(modifier = Modifier.height(16.dp))
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
        animationSpec = tween(durationMillis = 200),
        label = "scale"
    )
    
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) colorScheme.primaryContainer else colorScheme.surfaceVariant,
        animationSpec = tween(durationMillis = 200),
        label = "backgroundColor"
    )
    
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) colorScheme.primary else colorScheme.surfaceVariant,
        animationSpec = tween(durationMillis = 200),
        label = "borderColor"
    )
    
    val iconTint by animateColorAsState(
        targetValue = if (isSelected) colorScheme.primary else colorScheme.onSurfaceVariant,
        animationSpec = tween(durationMillis = 200),
        label = "iconTint"
    )
    
    val textColor by animateColorAsState(
        targetValue = if (isSelected) colorScheme.onPrimaryContainer else colorScheme.onSurface,
        animationSpec = tween(durationMillis = 200),
        label = "textColor"
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clip(RoundedCornerShape(16.dp))
            .border(
                width = 2.dp,
                color = borderColor,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = backgroundColor,
        tonalElevation = if (isSelected) 4.dp else 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon in circle
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(
                        if (isSelected) colorScheme.primary.copy(alpha = 0.15f) 
                        else colorScheme.surface
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = tripType.icon,
                    contentDescription = null,
                    modifier = Modifier.size(26.dp),
                    tint = iconTint
                )
            }
            
            // Title and description
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = tripType.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = textColor
                )
                Text(
                    text = tripType.description,
                    fontSize = 13.sp,
                    color = colorScheme.onSurfaceVariant
                )
            }
            
            // Checkmark when selected
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = "Selected",
                        modifier = Modifier.size(18.dp),
                        tint = colorScheme.onPrimary
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
