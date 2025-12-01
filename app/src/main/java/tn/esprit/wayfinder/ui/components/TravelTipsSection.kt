package tn.esprit.wayfinder.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import tn.esprit.wayfinder.models.TravelTip
import tn.esprit.wayfinder.viewmodels.TravelTipsViewModel
import tn.esprit.wayfinder.viewmodels.TravelTipsUiState
import tn.esprit.wayfinder.utils.StringTranslator

@Composable
fun TravelTipsSection(
    destinationId: String,
    destinationName: String,
    city: String? = null,
    country: String? = null,
    travelTipsViewModel: TravelTipsViewModel,
    navController: NavController,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme
    val uiState by travelTipsViewModel.uiState.collectAsState()

    LaunchedEffect(destinationId) {
        // Only load travel tips if destinationId is valid
        if (destinationId.isNotBlank()) {
            travelTipsViewModel.loadTravelTips(destinationId, limit = 3)
        }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Lightbulb,
                        contentDescription = null,
                        tint = Color(0xFFFFC107),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = StringTranslator.translate(context, "Conseils de voyage"),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.onSurface
                    )
                }
                TextButton(
                    onClick = {
                        navController.currentBackStackEntry?.savedStateHandle?.set("destinationName", destinationName)
                        navController.currentBackStackEntry?.savedStateHandle?.set("city", city)
                        navController.currentBackStackEntry?.savedStateHandle?.set("country", country)
                        navController.navigate("travel_tips/$destinationId")
                    }
                ) {
                    Text(StringTranslator.translate(context, "Voir tout →"), fontSize = 14.sp)
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            when (val state = uiState) {
                is TravelTipsUiState.Loading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(32.dp))
                    }
                }
                is TravelTipsUiState.Success -> {
                    if (state.tips.isEmpty()) {
                        // Generate tips if none exist
                        LaunchedEffect(Unit) {
                            travelTipsViewModel.generateTravelTips(
                                destinationId,
                                destinationName,
                                city,
                                country
                            )
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = StringTranslator.translate(context, "Génération des conseils..."),
                                color = colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(state.tips.take(3), key = { it.id }) { tip ->
                                TravelTipCard(
                                    tip = tip,
                                    onClick = {
                                        navController.currentBackStackEntry?.savedStateHandle?.set("destinationName", destinationName)
                                        navController.currentBackStackEntry?.savedStateHandle?.set("city", city)
                                        navController.currentBackStackEntry?.savedStateHandle?.set("country", country)
                                        navController.navigate("travel_tips/$destinationId")
                                    }
                                )
                            }
                        }
                    }
                }
                is TravelTipsUiState.Error -> {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = StringTranslator.translate(context, "Impossible de charger les conseils"),
                            color = colorScheme.onSurfaceVariant,
                            fontSize = 14.sp
                        )
                        TextButton(onClick = {
                            // Try to generate tips if loading fails
                            travelTipsViewModel.generateTravelTips(
                                destinationId,
                                destinationName,
                                city,
                                country
                            )
                        }) {
                            Text(StringTranslator.translate(context, "Générer les conseils"), fontSize = 12.sp)
                        }
                    }
                }
                else -> {}
            }
        }
    }
}

@Composable
fun TravelTipCard(
    tip: TravelTip,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .width(280.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = getCategoryColor(tip.category).copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = getCategoryIcon(tip.category),
                        contentDescription = tip.category,
                        tint = getCategoryColor(tip.category),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = tip.category.replaceFirstChar { it.uppercase() },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = getCategoryColor(tip.category)
                    )
                }
            }
            Text(
                text = tip.title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = tip.content,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3
            )
            if (tip.helpfulCount > 0) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.ThumbUp,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${tip.helpfulCount}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun getCategoryIcon(category: String): androidx.compose.ui.graphics.vector.ImageVector {
    return when (category.lowercase()) {
        "general" -> Icons.Filled.Info
        "transportation" -> Icons.Filled.DirectionsTransit
        "accommodation" -> Icons.Filled.Hotel
        "food" -> Icons.Filled.Restaurant
        "culture" -> Icons.Filled.Museum
        "safety" -> Icons.Filled.Security
        "budget" -> Icons.Filled.AccountBalanceWallet
        "weather" -> Icons.Filled.WbSunny
        else -> Icons.Filled.Lightbulb
    }
}

fun getCategoryColor(category: String): Color {
    return when (category.lowercase()) {
        "general" -> Color(0xFF2196F3)
        "transportation" -> Color(0xFF4CAF50)
        "accommodation" -> Color(0xFFFF9800)
        "food" -> Color(0xFFE91E63)
        "culture" -> Color(0xFF9C27B0)
        "safety" -> Color(0xFFF44336)
        "budget" -> Color(0xFF00BCD4)
        "weather" -> Color(0xFFFFC107)
        else -> Color(0xFF757575)
    }
}

