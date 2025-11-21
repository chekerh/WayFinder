package tn.esprit.wayfinder.ui.screens

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import tn.esprit.wayfinder.ui.theme.WayFinderTheme
import tn.esprit.wayfinder.models.ItineraryActivity
import tn.esprit.wayfinder.models.DayPlan
import tn.esprit.wayfinder.presentation.auth.ViewModelFactory
import tn.esprit.wayfinder.viewmodels.ItineraryUiState
import tn.esprit.wayfinder.viewmodels.ItineraryViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItineraryDetailScreen(navController: NavController, itineraryId: String) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val itineraryViewModel: ItineraryViewModel = viewModel(factory = ViewModelFactory(context.applicationContext as Application))
    val uiState by itineraryViewModel.uiState.collectAsState()

    LaunchedEffect(itineraryId) {
        itineraryViewModel.loadItinerary(itineraryId)
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Détails de l'itinéraire",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    ) 
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
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        when (val state = uiState) {
            is ItineraryUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            is ItineraryUiState.ItineraryLoaded -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        ItineraryHeaderCard(itinerary = state.itinerary)
                    }
                    
                    // Generate days between start and end date
                    val days = generateDaysBetween(state.itinerary.startDate, state.itinerary.endDate)
                    
                    items(days, key = { it }) { dayDate ->
                        val dayPlan = state.itinerary.days.find { it.date == dayDate }
                        DayPlanCard(
                            dayDate = dayDate,
                            dayPlan = dayPlan,
                            onAddActivity = {
                                // TODO: Navigate to add activity screen
                            }
                        )
                    }
                }
            }
            is ItineraryUiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
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
                        Button(onClick = { itineraryViewModel.loadItinerary(itineraryId) }) {
                            Text("Réessayer")
                        }
                    }
                }
            }
            else -> {}
        }
    }
}

@Composable
fun ItineraryHeaderCard(itinerary: tn.esprit.wayfinder.models.Itinerary) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = itinerary.title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            
            if (itinerary.description != null) {
                Text(
                    text = itinerary.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
            }
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.LocationOn,
                    contentDescription = null,
                    tint = Color(0xFF1976D2),
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = itinerary.destination,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            
            if (itinerary.totalBudget != null) {
                Text(
                    text = "Budget: ${itinerary.totalBudget} ${itinerary.currency ?: "EUR"}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF1976D2)
                )
            }
        }
    }
}

@Composable
fun DayPlanCard(
    dayDate: String,
    dayPlan: DayPlan?,
    onAddActivity: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formatDayDate(dayDate),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onAddActivity) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = "Ajouter activité",
                        tint = Color(0xFF1976D2)
                    )
                }
            }
            
            if (dayPlan?.activities?.isEmpty() != false) {
                Text(
                    text = "Aucune activité planifiée",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            } else {
                dayPlan?.activities?.forEach { activity ->
                    ActivityItem(activity = activity)
                }
            }
        }
    }
}

@Composable
fun ActivityItem(activity: ItineraryActivity) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = activity.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )
                if (activity.cost != null) {
                    Text(
                        text = "${activity.cost} ${activity.currency ?: "EUR"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF1976D2),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            
            if (activity.location != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.LocationOn,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = Color.Gray
                    )
                    Text(
                        text = activity.location,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            }
            
            if (activity.startTime != null || activity.endTime != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Schedule,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = Color.Gray
                    )
                    Text(
                        text = "${activity.startTime ?: "?"} - ${activity.endTime ?: "?"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            }
            
            if (activity.description != null) {
                Text(
                    text = activity.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
        }
    }
}

private fun generateDaysBetween(startDate: String, endDate: String): List<String> {
    val days = mutableListOf<String>()
    try {
        val parser = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val start = parser.parse(startDate) ?: return emptyList()
        val end = parser.parse(endDate) ?: return emptyList()
        
        val calendar = Calendar.getInstance()
        calendar.time = start
        
        while (calendar.time <= end) {
            days.add(parser.format(calendar.time))
            calendar.add(Calendar.DAY_OF_MONTH, 1)
        }
    } catch (e: Exception) {
        // Return empty list on error
    }
    return days
}

private fun formatDayDate(date: String): String {
    return try {
        val parser = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val formatter = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.getDefault())
        val parsed = parser.parse(date)
        formatter.format(parsed ?: Date())
    } catch (e: Exception) {
        date
    }
}

@Preview(showBackground = true)
@Composable
fun ItineraryDetailScreenPreview() {
    WayFinderTheme {
        ItineraryDetailScreen(rememberNavController(), itineraryId = "test-itinerary-id")
    }
}

