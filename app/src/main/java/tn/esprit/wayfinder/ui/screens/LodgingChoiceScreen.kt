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
import androidx.compose.material.icons.filled.CalendarToday
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import android.app.DatePickerDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LodgingChoiceScreen(
    navController: NavController,
    destinationId: String? = null
) {
    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme
    var selectedLodgingType by remember { mutableStateOf<String?>(null) }
    
    // Date selection state
    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val calendar = Calendar.getInstance()
    calendar.add(Calendar.DAY_OF_MONTH, 14) // Default: 2 weeks from now
    val defaultCheckIn = dateFormat.format(calendar.time)
    calendar.add(Calendar.DAY_OF_MONTH, 3) // Default: 3 days after check-in
    val defaultCheckOut = dateFormat.format(calendar.time)
    
    var checkInDate by remember { mutableStateOf(defaultCheckIn) }
    var checkOutDate by remember { mutableStateOf(defaultCheckOut) }
    var showCheckInDatePicker by remember { mutableStateOf(false) }
    var showCheckOutDatePicker by remember { mutableStateOf(false) }
    
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
                text = StringTranslator.translate(context, "Sélectionnez les dates de séjour"),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            // Date Selection Cards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Check-in Date
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { showCheckInDatePicker = true },
                    colors = CardDefaults.cardColors(
                        containerColor = colorScheme.surfaceVariant
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CalendarToday,
                            contentDescription = null,
                            tint = colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = StringTranslator.translate(context, "Arrivée"),
                            fontSize = 12.sp,
                            color = colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = formatDateForDisplay(checkInDate, context),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = colorScheme.onSurface
                        )
                    }
                }
                
                // Check-out Date
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { showCheckOutDatePicker = true },
                    colors = CardDefaults.cardColors(
                        containerColor = colorScheme.surfaceVariant
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CalendarToday,
                            contentDescription = null,
                            tint = colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = StringTranslator.translate(context, "Départ"),
                            fontSize = 12.sp,
                            color = colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = formatDateForDisplay(checkOutDate, context),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = colorScheme.onSurface
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = StringTranslator.translate(context, "Sélectionnez le type de logement souhaité"),
                fontSize = 16.sp,
                color = colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            // Date Pickers
            if (showCheckInDatePicker) {
                val checkInCalendar = Calendar.getInstance()
                dateFormat.parse(checkInDate)?.let { checkInCalendar.time = it }
                
                DatePickerDialog(
                    context,
                    { _, year, month, dayOfMonth ->
                        val selectedDate = Calendar.getInstance().apply {
                            set(year, month, dayOfMonth)
                        }
                        checkInDate = dateFormat.format(selectedDate.time)
                        
                        // Ensure check-out is after check-in
                        val checkOutCal = Calendar.getInstance()
                        dateFormat.parse(checkOutDate)?.let { checkOutCal.time = it }
                        if (checkOutCal.before(selectedDate) || checkOutCal == selectedDate) {
                            checkOutCal.time = selectedDate.time
                            checkOutCal.add(Calendar.DAY_OF_MONTH, 1)
                            checkOutDate = dateFormat.format(checkOutCal.time)
                        }
                        
                        showCheckInDatePicker = false
                    },
                    checkInCalendar.get(Calendar.YEAR),
                    checkInCalendar.get(Calendar.MONTH),
                    checkInCalendar.get(Calendar.DAY_OF_MONTH)
                ).apply {
                    datePicker.minDate = Calendar.getInstance().timeInMillis
                }.show()
            }
            
            if (showCheckOutDatePicker) {
                val checkOutCalendar = Calendar.getInstance()
                dateFormat.parse(checkOutDate)?.let { checkOutCalendar.time = it }
                val checkInCal = Calendar.getInstance()
                dateFormat.parse(checkInDate)?.let { checkInCal.time = it }
                
                DatePickerDialog(
                    context,
                    { _, year, month, dayOfMonth ->
                        val selectedDate = Calendar.getInstance().apply {
                            set(year, month, dayOfMonth)
                        }
                        // Ensure check-out is after check-in
                        if (selectedDate.after(checkInCal)) {
                            checkOutDate = dateFormat.format(selectedDate.time)
                        }
                        showCheckOutDatePicker = false
                    },
                    checkOutCalendar.get(Calendar.YEAR),
                    checkOutCalendar.get(Calendar.MONTH),
                    checkOutCalendar.get(Calendar.DAY_OF_MONTH)
                ).apply {
                    datePicker.minDate = checkInCal.timeInMillis + (24 * 60 * 60 * 1000) // At least 1 day after check-in
                }.show()
            }
            
            accommodationTypes.forEach { type ->
                AccommodationTypeCard(
                    type = type,
                    isSelected = selectedLodgingType == type.id,
                    onClick = {
                        selectedLodgingType = type.id
                        // Save dates and accommodation type, then navigate
                        navController.currentBackStackEntry
                            ?.savedStateHandle
                            ?.apply {
                                set("accommodation_type", type.id)
                                set("check_in_date", checkInDate)
                                set("check_out_date", checkOutDate)
                            }
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

@Composable
private fun formatDateForDisplay(dateString: String, context: android.content.Context): String {
    return try {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val outputFormat = SimpleDateFormat("dd MMM", Locale.getDefault())
        val date = inputFormat.parse(dateString)
        date?.let { outputFormat.format(it) } ?: dateString
    } catch (e: Exception) {
        dateString
    }
}

@Preview(showBackground = true)
@Composable
fun LodgingChoiceScreenPreview() {
    WayFinderTheme {
        LodgingChoiceScreen(rememberNavController())
    }
}

