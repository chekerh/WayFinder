package tn.esprit.wayfinder.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.outlined.*
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.material3.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import androidx.compose.ui.tooling.preview.Preview
import tn.esprit.wayfinder.ui.theme.WayFinderTheme
import android.app.Application
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import tn.esprit.wayfinder.models.FlightDestination
import tn.esprit.wayfinder.navigation.SELECTED_DESTINATION_KEY
import tn.esprit.wayfinder.presentation.auth.ViewModelFactory
import tn.esprit.wayfinder.ui.components.ReviewsSection
import tn.esprit.wayfinder.ui.components.CreatePriceAlertDialog
import tn.esprit.wayfinder.ui.components.TravelTipsSection
import tn.esprit.wayfinder.ui.components.BookingDetailsDialog
import tn.esprit.wayfinder.ui.components.BookingDetails
import tn.esprit.wayfinder.viewmodels.ReviewsViewModel
import tn.esprit.wayfinder.viewmodels.PriceAlertsViewModel
import tn.esprit.wayfinder.viewmodels.TravelTipsViewModel
import tn.esprit.wayfinder.viewmodels.FavoritesViewModel
import tn.esprit.wayfinder.utils.StringTranslator
import tn.esprit.wayfinder.R
import androidx.compose.material.icons.filled.Favorite

@Composable
fun FlightDetailsScreen(navController: NavController, destinationId: String) {
    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme
    val reviewsViewModel: ReviewsViewModel = viewModel(factory = ViewModelFactory(context.applicationContext as Application))
    val priceAlertsViewModel: PriceAlertsViewModel = viewModel(factory = ViewModelFactory(context.applicationContext as Application))
    val travelTipsViewModel: TravelTipsViewModel = viewModel(factory = ViewModelFactory(context.applicationContext as Application))
    val favoritesViewModel: FavoritesViewModel = viewModel(factory = ViewModelFactory(context.applicationContext as Application))
    
    var showPriceAlertDialog by remember { mutableStateOf(false) }
    var showBookingDialog by remember { mutableStateOf(false) }
    var isFavorite by remember { mutableStateOf(false) }
    
    // Check if favorite on composition
    LaunchedEffect(destinationId) {
        favoritesViewModel.checkFavorite("flight", destinationId) { favorite ->
            isFavorite = favorite
        }
    }
    
    val savedDestination = remember(destinationId) {
        navController.previousBackStackEntry
            ?.savedStateHandle
            ?.get<FlightDestination>(SELECTED_DESTINATION_KEY)
    }
    val destination = remember(destinationId, savedDestination, context) {
        savedDestination ?: FlightDestination(
            id = destinationId,
            name = StringTranslator.translate(context, "Destination surprise"),
            city = StringTranslator.translate(context, "À définir"),
            country = "",
            price = 0.0,
            currency = "EUR",
            description = StringTranslator.translate(context, "Impossible de charger les détails depuis la sélection précédente."),
            departureDate = null,
            arrivalDate = null,
            airline = null
        )
    }
    
    LaunchedEffect(destination) {
        navController.currentBackStackEntry
            ?.savedStateHandle
            ?.set(SELECTED_DESTINATION_KEY, destination)
    }

    // Define amenities/features for the destination
    val features = remember(destination.country) {
        listOf(
            StringTranslator.translate(context, "ensoleillé") to Icons.Outlined.WbSunny,
            StringTranslator.translate(context, "Resto") to Icons.Outlined.Restaurant,
            StringTranslator.translate(context, "Wi-Fi gratuit") to Icons.Outlined.Wifi,
            StringTranslator.translate(context, "Café") to Icons.Outlined.LocalCafe,
            StringTranslator.translate(context, "Affaires") to Icons.Outlined.Business
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Header Image
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(380.dp)
        ) {
            // Display destination image if available, otherwise use gradient
            if (!destination.imageUrl.isNullOrBlank()) {
                AsyncImage(
                    model = destination.imageUrl,
                    contentDescription = destination.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    placeholder = painterResource(id = R.drawable.europe),
                    error = painterResource(id = R.drawable.europe)
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxSize().background(
                        Brush.verticalGradient(
                            colors = listOf(Color(0xFF4A90E2), Color(0xFF2E5C8A))
                        )
                    )
                )
            }
        }

        // Content Card
        Card(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 320.dp),
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp)
            ) {

                // Destination Title and Price
                Text(
                    text = destination.name,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.onBackground
                )
                if (destination.country.isNotBlank()) {
                    Text(
                        text = destination.country,
                        style = MaterialTheme.typography.bodyLarge,
                        color = colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                if (destination.price != null) {
                    Text(
                        text = "${destination.price.toInt()} ${destination.currency}",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.primary,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                
                FlightTimeline(
                    originLabel = StringTranslator.translate(context, "Tunis (TUN)"),
                    destinationLabel = destination.city ?: destination.name,
                    airline = destination.airline ?: StringTranslator.translate(context, "Compagnie à confirmer"),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                // Description du pays - TOUJOURS affichée (sans condition)
                val descriptionText = if (destination.description != null && destination.description.isNotBlank()) {
                    destination.description
                } else {
                    // Description par défaut basée sur le pays ou le nom de la destination
                    val countryLower = destination.country.lowercase()
                    val nameLower = destination.name.lowercase()
                    val cityLower = destination.city?.lowercase() ?: ""
                    
                    when {
                        countryLower.contains("france") || nameLower.contains("paris") || cityLower.contains("paris") -> 
                            "Paris, la capitale de la France, est réputée pour sa culture, son art, sa gastronomie et ses monuments emblématiques comme la Tour Eiffel, le Louvre et Notre-Dame. Ville romantique par excellence, elle attire des millions de visiteurs chaque année."
                        countryLower.contains("italie") || countryLower.contains("italy") || nameLower.contains("rome") || cityLower.contains("rome") -> 
                            "Rome, la capitale de l'Italie, est une ville riche en histoire avec ses monuments antiques, ses églises baroques et sa cuisine délicieuse. Découvrez le Colisée, le Forum romain et la Cité du Vatican."
                        countryLower.contains("espagne") || countryLower.contains("spain") || nameLower.contains("madrid") || nameLower.contains("barcelone") || cityLower.contains("madrid") || cityLower.contains("barcelone") -> 
                            "L'Espagne offre une riche diversité culturelle, des plages magnifiques, une architecture unique et une cuisine savoureuse. Découvrez l'art, la danse flamenco et l'histoire fascinante de ce pays méditerranéen."
                        else -> 
                            "${destination.name} est une destination magnifique offrant une expérience de voyage unique avec ses paysages, sa culture et ses attractions touristiques. Explorez cette ville fascinante et découvrez tout ce qu'elle a à offrir."
                    }
                }
                
                // Afficher la description - FORCÉE à être visible (sans condition if)
                Text(
                    text = descriptionText,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, // S'adapte automatiquement au mode sombre
                    fontSize = 16.sp,
                    lineHeight = 24.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                )
                Spacer(modifier = Modifier.height(24.dp))
                
                val addOns = listOf(
                    Triple(
                        StringTranslator.translate(context, "Assurance voyage"),
                        StringTranslator.translate(context, "Protection annulation & bagages"),
                        "19 ${destination.currency}"
                    ),
                    Triple(
                        StringTranslator.translate(context, "Accès salon"),
                        StringTranslator.translate(context, "Confort avant le décollage"),
                        "29 ${destination.currency}"
                    ),
                    Triple(
                        StringTranslator.translate(context, "Siège premium"),
                        StringTranslator.translate(context, "Plus d'espace pour les jambes"),
                        "35 ${destination.currency}"
                    )
                )
                Text(
                    text = StringTranslator.translate(context, "Extras recommandés"),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    addOns.forEach { (title, subtitle, priceLabel) ->
                        FlightAddOnChip(
                            title = title,
                            subtitle = subtitle,
                            price = priceLabel
                        )
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
                
                // Available Amenities Section
                Text(
                    text = StringTranslator.translate(context, "Équipements disponibles"),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    features.forEach { (name, icon) ->
                        FeatureItem(name = name, icon = icon)
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))

                // Flight Information - Glass Effect (Glassmorphism)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = StringTranslator.translate(context, "Informations du vol"),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = colorScheme.onSurface
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = StringTranslator.translate(context, "Départ"),
                                    fontSize = 14.sp,
                                    color = colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = destination.departureDate?.substringBefore("T") ?: "N/A",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colorScheme.onSurface
                                )
                                Text(
                                    text = destination.departureDate?.substringAfter("T")?.substringBefore(":")?.let {
                                        "${it}:${destination.departureDate.substringAfter(":").substringBefore(":")}"
                                    } ?: "N/A",
                                    fontSize = 16.sp,
                                    color = colorScheme.onSurfaceVariant
                                )
                            }

                            Icon(
                                imageVector = Icons.Filled.Flight,
                                contentDescription = null,
                                tint = colorScheme.primary,
                                modifier = Modifier.size(32.dp)
                            )

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = StringTranslator.translate(context, "Arrivée"),
                                    fontSize = 14.sp,
                                    color = colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = destination.arrivalDate?.substringBefore("T") ?: "N/A",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colorScheme.onSurface
                                )
                                Text(
                                    text = destination.arrivalDate?.substringAfter("T")?.substringBefore(":")?.let {
                                        "${it}:${destination.arrivalDate.substringAfter(":").substringBefore(":")}"
                                    } ?: "N/A",
                                    fontSize = 16.sp,
                                    color = colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        HorizontalDivider(
                            color = colorScheme.outline.copy(alpha = 0.3f)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = StringTranslator.translate(context, "Compagnie aérienne"),
                                    fontSize = 14.sp,
                                    color = colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = destination.airline ?: "N/A",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = colorScheme.onSurface
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = StringTranslator.translate(context, "Durée"),
                                    fontSize = 14.sp,
                                    color = colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "~4h 30min",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))


                // Travel Tips Section
                TravelTipsSection(
                    destinationId = destination.id,
                    destinationName = destination.name,
                    city = destination.city,
                    country = destination.country,
                    travelTipsViewModel = travelTipsViewModel,
                    navController = navController
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Reviews Section
                ReviewsSection(
                    itemType = "flight",
                    itemId = destination.id,
                    reviewsViewModel = reviewsViewModel
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Price Alert Section - Glass Effect
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.NotificationsActive,
                                    contentDescription = null,
                                    tint = Color(0xFFFFC107),
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = StringTranslator.translate(context, "Alerte de prix"),
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colorScheme.onSurface
                                )
                            }
                        }
                        Text(
                            text = StringTranslator.translate(context, "Soyez notifié lorsque le prix de ce vol change"),
                            fontSize = 14.sp,
                            color = colorScheme.onSurfaceVariant
                        )
                        Button(
                            onClick = { showPriceAlertDialog = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFFFC107)
                            )
                        ) {
                            Text(
                                text = StringTranslator.translate(context, "Créer une alerte"),
                                color = if (isSystemInDarkTheme()) Color.White else Color.Black,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Button(
                        onClick = { 
                            navController.currentBackStackEntry
                                ?.savedStateHandle
                                ?.set(SELECTED_DESTINATION_KEY, destination)
                            navController.navigate("flight_comparison/${destination.id}")
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colorScheme.primary
                        )
                    ) {
                        Text(
                            text = StringTranslator.translate(context, "Comparer les prix"),
                            color = colorScheme.onPrimary
                        )
                    }
                    Button(
                        onClick = {
                            // Navigate to airline selection first
                            navController.currentBackStackEntry
                                ?.savedStateHandle
                                ?.set(SELECTED_DESTINATION_KEY, destination)
                            navController.navigate("airline_selection/${destination.id}")
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFFC107)
                        )
                    ) {
                        Text(
                            text = StringTranslator.translate(context, "Réserver"),
                            color = if (isSystemInDarkTheme()) Color.White else Color.Black,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Airline Logo Buttons on Right Side
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 16.dp)
                .statusBarsPadding()
                .padding(top = 80.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Airline 1 - Example: Air France
            AirlineLogoButton(
                logoUrl = "https://logos-world.net/wp-content/uploads/2021/02/Air-France-Logo.png",
                airlineName = "Air France",
                onClick = {
                    // Navigate to airline page
                    navController.navigate("airline/AirFrance")
                }
            )
            // Airline 2 - Example: Lufthansa
            AirlineLogoButton(
                logoUrl = "https://logos-world.net/wp-content/uploads/2020/03/Lufthansa-Logo.png",
                airlineName = "Lufthansa",
                onClick = {
                    navController.navigate("airline/Lufthansa")
                }
            )
            // Airline 3 - Example: Emirates
            AirlineLogoButton(
                logoUrl = "https://logos-world.net/wp-content/uploads/2020/06/Emirates-Logo.png",
                airlineName = "Emirates",
                onClick = {
                    navController.navigate("airline/Emirates")
                }
            )
        }

        // Buttons overlay - declared after Card to be on top and clickable
        Box(
            modifier = Modifier
                .fillMaxSize()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(16.dp)
                    .align(Alignment.TopStart),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { 
                        navController.popBackStack() 
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            if (isSystemInDarkTheme()) {
                                Color.White.copy(alpha = 0.2f)
                            } else {
                                Color.Black.copy(alpha = 0.3f)
                            },
                            shape = RoundedCornerShape(24.dp)
                        )
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    IconButton(
                        onClick = { 
                            // Share action - TODO: implement share functionality
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .background(
                                if (isSystemInDarkTheme()) {
                                    Color.White.copy(alpha = 0.2f)
                                } else {
                                    Color.Black.copy(alpha = 0.3f)
                                },
                                shape = RoundedCornerShape(24.dp)
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Share,
                            contentDescription = "Share",
                            tint = Color.White
                        )
                    }
                    IconButton(
                        onClick = { 
                            isFavorite = !isFavorite
                            if (isFavorite) {
                                favoritesViewModel.addFavorite(
                                    "flight",
                                    destination.id,
                                    mapOf(
                                        "name" to destination.name,
                                        "city" to (destination.city ?: ""),
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
                            .size(48.dp)
                            .background(
                                if (isSystemInDarkTheme()) {
                                    Color.White.copy(alpha = 0.2f)
                                } else {
                                    Color.Black.copy(alpha = 0.3f)
                                },
                                shape = RoundedCornerShape(24.dp)
                            )
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

        if (showPriceAlertDialog) {
            CreatePriceAlertDialog(
                destination = destination,
                currentPrice = destination.price,
                currency = destination.currency,
                onDismiss = { showPriceAlertDialog = false },
                onCreate = { request ->
                    priceAlertsViewModel.createPriceAlert(request) {
                        // Show success message or navigate
                    }
                }
            )
        }
        
        if (showBookingDialog) {
            BookingDetailsDialog(
                destinationName = destination.name,
                onDismiss = { showBookingDialog = false },
                onConfirm = { bookingDetails ->
                    showBookingDialog = false
                    // Navigate to accommodation selection instead of payment
                    navController.currentBackStackEntry
                        ?.savedStateHandle
                        ?.apply {
                            set(SELECTED_DESTINATION_KEY, destination)
                            set("booking_details", bookingDetails)
                        }
                    navController.navigate("lodging_choice/${destination.id}")
                }
            )
        }
    }
}

@Composable
fun AirlineLogoButton(
    logoUrl: String,
    airlineName: String,
    onClick: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    Card(
        modifier = Modifier.size(56.dp),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        onClick = onClick
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = logoUrl,
                contentDescription = airlineName,
                modifier = Modifier
                    .size(48.dp)
                    .padding(8.dp),
                contentScale = ContentScale.Fit,
                error = painterResource(id = R.drawable.europe)
            )
        }
    }
}

@Composable
private fun FlightTimeline(
    originLabel: String,
    destinationLabel: String,
    airline: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                        MaterialTheme.colorScheme.secondary.copy(alpha = 0.08f)
                    )
                )
            )
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Départ", 
                    style = MaterialTheme.typography.labelSmall, 
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = originLabel, 
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "Arrivée", 
                    style = MaterialTheme.typography.labelSmall, 
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = destinationLabel, 
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(24.dp),
            contentAlignment = Alignment.Center
        ) {
            HorizontalDivider(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                thickness = 2.dp
            )
            Icon(
                imageVector = Icons.Filled.Flight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        }
        Text(
            text = airline,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun FlightAddOnChip(
    title: String,
    subtitle: String,
    price: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .widthIn(min = 180.dp)
            .heightIn(min = 110.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(4.dp)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = title, 
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.weight(1f, fill = true))
            Text(
                text = price,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun FlightDetailsScreenPreview() {
    WayFinderTheme {
        FlightDetailsScreen(rememberNavController(), destinationId = "test-destination-id")
    }
}

