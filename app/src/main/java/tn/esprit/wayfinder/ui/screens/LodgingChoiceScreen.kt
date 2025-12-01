package tn.esprit.wayfinder.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import tn.esprit.wayfinder.ui.components.CustomBottomNavigationBar
import tn.esprit.wayfinder.ui.theme.WayFinderTheme
import tn.esprit.wayfinder.utils.StringTranslator
import androidx.compose.ui.platform.LocalContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LodgingChoiceScreen(
    navController: NavController,
    destinationId: String? = null
) {
    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme
    var selectedLodgingType by remember { mutableStateOf<String?>(null) }
    
    val accommodationTypes = listOf(
        AccommodationType("hotel", "Hôtel", Icons.Filled.Hotel, "Confort et service professionnel"),
        AccommodationType("airbnb", "Airbnb", Icons.Filled.Home, "Expérience locale authentique"),
        AccommodationType("hostel", "Auberge", Icons.Filled.People, "Économique et convivial"),
        AccommodationType("resort", "Résort", Icons.Filled.BeachAccess, "Luxe et détente"),
        AccommodationType("apartment", "Appartement", Icons.Filled.Apartment, "Indépendance et espace")
    )

    Scaffold(
        containerColor = colorScheme.background,
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        StringTranslator.translate(context, "Choisir le type de logement"),
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    ) 
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
            CustomBottomNavigationBar(navController = navController)
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = StringTranslator.translate(context, "Sélectionnez le type de logement souhaité"),
                fontSize = 16.sp,
                color = colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            accommodationTypes.forEach { type ->
                AccommodationTypeCard(
                    type = type,
                    isSelected = selectedLodgingType == type.id,
                    onClick = {
                        selectedLodgingType = type.id
                        // Navigate to accommodation listing screen
                        navController.currentBackStackEntry
                            ?.savedStateHandle
                            ?.set("accommodation_type", type.id)
                        navController.navigate("accommodations/${destinationId ?: ""}/${type.id}")
                    }
                )
            }
        }
    }
}

data class AccommodationType(
    val id: String,
    val name: String,
    val icon: ImageVector,
    val description: String
)

@Composable
fun AccommodationTypeCard(
    type: AccommodationType,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    Card(
                modifier = Modifier
                    .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) 
                colorScheme.primaryContainer 
            else 
                colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 4.dp else 2.dp
        )
    ) {
        Row(
                modifier = Modifier
                    .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = type.icon,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = if (isSelected) colorScheme.onPrimaryContainer else colorScheme.primary
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = type.name,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) 
                        colorScheme.onPrimaryContainer 
                    else 
                        colorScheme.onSurface
                )
                Text(
                    text = type.description,
                    fontSize = 14.sp,
                    color = if (isSelected) 
                        colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    else 
                        colorScheme.onSurfaceVariant
                )
            }
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun LodgingChoiceScreenPreview() {
    WayFinderTheme {
        LodgingChoiceScreen(rememberNavController())
    }
}

