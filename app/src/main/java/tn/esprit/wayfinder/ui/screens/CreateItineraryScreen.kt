package tn.esprit.wayfinder.ui.screens

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.LocationOn
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
import tn.esprit.wayfinder.presentation.auth.ViewModelFactory
import tn.esprit.wayfinder.viewmodels.ItineraryViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateItineraryScreen(navController: NavController) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val itineraryViewModel: ItineraryViewModel = viewModel(factory = ViewModelFactory(context.applicationContext as Application))
    
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var destination by remember { mutableStateOf("") }
    var startDate by remember { mutableStateOf("") }
    var endDate by remember { mutableStateOf("") }
    var totalBudget by remember { mutableStateOf("") }
    var isPublic by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = Color(0xFFF0F8FF),
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Nouvel itinéraire",
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
                    containerColor = Color(0xFFEAF2FF)
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Titre de l'itinéraire *") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )
            
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                minLines = 3,
                maxLines = 5
            )
            
            OutlinedTextField(
                value = destination,
                onValueChange = { destination = it },
                label = { Text("Destination *") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                leadingIcon = {
                    Icon(Icons.Filled.LocationOn, contentDescription = null)
                },
                singleLine = true
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = startDate,
                    onValueChange = { startDate = it },
                    label = { Text("Date de début *") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    leadingIcon = {
                        Icon(Icons.Filled.CalendarToday, contentDescription = null)
                    },
                    placeholder = { Text("YYYY-MM-DD") },
                    singleLine = true
                )
                
                OutlinedTextField(
                    value = endDate,
                    onValueChange = { endDate = it },
                    label = { Text("Date de fin *") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    leadingIcon = {
                        Icon(Icons.Filled.CalendarToday, contentDescription = null)
                    },
                    placeholder = { Text("YYYY-MM-DD") },
                    singleLine = true
                )
            }
            
            OutlinedTextField(
                value = totalBudget,
                onValueChange = { totalBudget = it },
                label = { Text("Budget total") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                placeholder = { Text("0.0") },
                singleLine = true
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = isPublic,
                    onCheckedChange = { isPublic = it }
                )
                Text(
                    text = "Rendre cet itinéraire public",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = {
                    if (title.isNotBlank() && destination.isNotBlank() && startDate.isNotBlank() && endDate.isNotBlank()) {
                        val budget = totalBudget.toDoubleOrNull()
                        itineraryViewModel.createItinerary(
                            request = tn.esprit.wayfinder.models.CreateItineraryRequest(
                                title = title,
                                description = description.ifBlank { null },
                                destination = destination,
                                startDate = startDate,
                                endDate = endDate,
                                totalBudget = budget,
                                isPublic = isPublic
                            ),
                            onSuccess = {
                                navController.popBackStack()
                            }
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                enabled = title.isNotBlank() && destination.isNotBlank() && startDate.isNotBlank() && endDate.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Créer l'itinéraire", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

