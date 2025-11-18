package tn.esprit.wayfinder.ui.screens

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import tn.esprit.wayfinder.models.TravelTip
import tn.esprit.wayfinder.presentation.auth.ViewModelFactory
import tn.esprit.wayfinder.ui.components.getCategoryColor
import tn.esprit.wayfinder.ui.components.getCategoryIcon
import tn.esprit.wayfinder.viewmodels.TravelTipsUiState
import tn.esprit.wayfinder.viewmodels.TravelTipsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TravelTipsScreen(
    navController: NavController,
    destinationId: String,
    destinationName: String? = null,
    city: String? = null,
    country: String? = null
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val travelTipsViewModel: TravelTipsViewModel = viewModel(factory = ViewModelFactory(context.applicationContext as Application))
    val uiState by travelTipsViewModel.uiState.collectAsState()

    var selectedCategory by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(destinationId) {
        if (uiState is TravelTipsUiState.Idle || (uiState is TravelTipsUiState.Success && (uiState as TravelTipsUiState.Success).tips.isEmpty())) {
            if (destinationName != null) {
                travelTipsViewModel.generateTravelTips(destinationId, destinationName, city, country)
            } else {
                travelTipsViewModel.loadTravelTips(destinationId)
            }
        }
    }

    Scaffold(
        containerColor = Color(0xFFF0F8FF),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = destinationName ?: "Conseils de voyage",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                        if (city != null || country != null) {
                            Text(
                                text = "${city ?: ""}${if (city != null && country != null) ", " else ""}${country ?: ""}",
                                fontSize = 14.sp,
                                color = Color.Gray
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Retour"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFEAF2FF)
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Category Filter Chips
            val categories = listOf(
                "all" to "Tous",
                "general" to "Général",
                "transportation" to "Transport",
                "accommodation" to "Hébergement",
                "food" to "Nourriture",
                "culture" to "Culture",
                "safety" to "Sécurité",
                "budget" to "Budget",
                "weather" to "Météo"
            )

            HorizontalDivider()

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                categories.forEach { (category, label) ->
                    FilterChip(
                        selected = selectedCategory == category || (selectedCategory == null && category == "all"),
                        onClick = {
                            selectedCategory = if (category == "all") null else category
                            travelTipsViewModel.loadTravelTips(
                                destinationId,
                                category = selectedCategory,
                                limit = 50
                            )
                        },
                        label = { Text(label, fontSize = 12.sp) }
                    )
                }
            }

            HorizontalDivider()

            // Tips List
            when (val state = uiState) {
                is TravelTipsUiState.Loading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                is TravelTipsUiState.Success -> {
                    val filteredTips = if (selectedCategory == null) {
                        state.tips
                    } else {
                        state.tips.filter { it.category == selectedCategory }
                    }

                    if (filteredTips.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Lightbulb,
                                    contentDescription = null,
                                    modifier = Modifier.size(64.dp),
                                    tint = Color.Gray
                                )
                                Text(
                                    "Aucun conseil disponible",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = Color.Gray
                                )
                                Text(
                                    "Les conseils seront générés automatiquement",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.Gray
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(filteredTips, key = { it.id }) { tip ->
                                TravelTipDetailCard(
                                    tip = tip,
                                    onMarkHelpful = {
                                        travelTipsViewModel.markTipHelpful(tip.id)
                                    }
                                )
                            }
                        }
                    }
                }
                is TravelTipsUiState.Error -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                state.message,
                                color = Color.Red,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Button(onClick = {
                                travelTipsViewModel.loadTravelTips(destinationId)
                            }) {
                                Text("Réessayer")
                            }
                        }
                    }
                }
                else -> {}
            }
        }
    }
}

@Composable
fun TravelTipDetailCard(
    tip: TravelTip,
    onMarkHelpful: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isHelpful by remember { mutableStateOf(false) }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(2.dp)
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
                        imageVector = getCategoryIcon(tip.category),
                        contentDescription = tip.category,
                        tint = getCategoryColor(tip.category),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = tip.category.replaceFirstChar { it.uppercase() },
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = getCategoryColor(tip.category)
                    )
                }
                if (tip.tags.isNotEmpty()) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        tip.tags.take(2).forEach { tag ->
                            AssistChip(
                                onClick = { },
                                label = {
                                    Text(tag, fontSize = 10.sp)
                                },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = Color(0xFFE0E0E0)
                                )
                            )
                        }
                    }
                }
            }

            Text(
                text = tip.title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = tip.content,
                fontSize = 15.sp,
                color = Color.Gray,
                lineHeight = 22.sp
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = {
                            if (!isHelpful) {
                                isHelpful = true
                                onMarkHelpful()
                            }
                        },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ThumbUp,
                            contentDescription = "Utile",
                            tint = if (isHelpful) Color(0xFF1976D2) else Color.Gray,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Text(
                        text = "${tip.helpfulCount + if (isHelpful) 1 else 0}",
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                }
            }
        }
    }
}

