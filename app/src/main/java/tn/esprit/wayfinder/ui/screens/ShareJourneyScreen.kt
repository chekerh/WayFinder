package tn.esprit.wayfinder.ui.screens

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import tn.esprit.wayfinder.ui.theme.WayFinderTheme
import coil.compose.AsyncImage
import tn.esprit.wayfinder.presentation.auth.ViewModelFactory
import tn.esprit.wayfinder.viewmodels.JourneyViewModel
import tn.esprit.wayfinder.viewmodels.JourneyUploadUiState
import tn.esprit.wayfinder.viewmodels.BookingViewModel
import tn.esprit.wayfinder.viewmodels.BookingUiState
import tn.esprit.wayfinder.models.Booking
import tn.esprit.wayfinder.models.BookingStatus
import tn.esprit.wayfinder.utils.StringTranslator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareJourneyScreen(navController: NavController) {
    val context = LocalContext.current
    val journeyViewModel: JourneyViewModel = viewModel(factory = ViewModelFactory(context.applicationContext as Application))
    val bookingViewModel: BookingViewModel = viewModel(factory = ViewModelFactory(context.applicationContext as Application))
    val uploadState by journeyViewModel.uploadUiState.collectAsState()
    val bookingHistoryState by bookingViewModel.bookingHistoryState.collectAsState()
    
    var selectedImages by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var description by remember { mutableStateOf("") }
    var tags by remember { mutableStateOf("") }
    
    // Selected booking state (for destination)
    var selectedBooking by remember { mutableStateOf<Booking?>(null) }
    var destinationDropdownExpanded by remember { mutableStateOf(false) }
    
    // Get confirmed bookings with unique destinations
    val confirmedBookings = remember(bookingHistoryState) {
        when (val state = bookingHistoryState) {
            is BookingUiState.Success -> {
                val confirmed = state.bookings.filter { it.status == BookingStatus.CONFIRMED }
                android.util.Log.d("ShareJourneyScreen", "Loaded ${confirmed.size} confirmed bookings out of ${state.bookings.size} total")
                confirmed.forEach { booking ->
                    android.util.Log.d("ShareJourneyScreen", "Booking ${booking.id}: destination=${booking.tripDetails?.destination ?: "null"}")
                }
                confirmed
            }
            is BookingUiState.Error -> {
                android.util.Log.e("ShareJourneyScreen", "Error loading bookings: ${state.message}")
                emptyList()
            }
            else -> emptyList()
        }
    }
    
    // Extract unique destinations from confirmed bookings
    // First try tripDetails.destination, then use a fallback display name
    val uniqueDestinations = remember(confirmedBookings) {
        confirmedBookings
            .map { booking ->
                // Try to get destination from tripDetails, otherwise use a display name
                val destination = booking.tripDetails?.destination?.takeIf { it.isNotBlank() }
                    ?: "Réservation ${booking.confirmationNumber}"
                Pair(destination, booking) // Pair destination name with booking
            }
            .distinctBy { it.first } // Get unique destinations by name
            .map { it.second } // Return bookings with unique destinations
            .sortedByDescending { it.bookingDate } // Sort by booking date (most recent first)
    }
    
    // Auto-select first booking when bookings are loaded
    LaunchedEffect(uniqueDestinations) {
        if (selectedBooking == null && uniqueDestinations.isNotEmpty()) {
            selectedBooking = uniqueDestinations.first()
            val destination = uniqueDestinations.first().tripDetails?.destination?.takeIf { it.isNotBlank() }
                ?: "Réservation ${uniqueDestinations.first().confirmationNumber}"
            android.util.Log.d("ShareJourneyScreen", "Auto-selected booking: ${uniqueDestinations.first().id} - destination: $destination")
        }
    }
    
    // Load booking history on screen load (retry if error)
    LaunchedEffect(Unit) {
        if (bookingHistoryState is BookingUiState.Idle || bookingHistoryState is BookingUiState.Error) {
            android.util.Log.d("ShareJourneyScreen", "Loading booking history...")
            bookingViewModel.loadBookingHistory()
        }
    }
    
    // Copy images to temporary files immediately after selection to avoid permission issues
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        // Copy URIs to temporary files immediately while we have access
        // This avoids permission issues with Photo Picker URIs
        val tempFiles = mutableListOf<Uri>()
        
        uris.forEach { uri ->
            try {
                // Try to take persistable permission (for Android 13+)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    try {
                        context.contentResolver.takePersistableUriPermission(
                            uri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION
                        )
                    } catch (e: SecurityException) {
                        // If we can't take persistable permission, copy immediately
                        android.util.Log.d("ShareJourneyScreen", "Cannot take persistable permission, copying file immediately")
                    }
                }
                
                // Copy to temporary file while we have access
                val tempFile = File(context.cacheDir, "journey_temp_${System.currentTimeMillis()}_${uri.hashCode()}.jpg")
                context.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(tempFile).use { output ->
                        input.copyTo(output)
                    }
                }
                
                // Use file:// URI for the temporary file
                tempFiles.add(Uri.fromFile(tempFile))
                android.util.Log.d("ShareJourneyScreen", "Copied $uri to ${tempFile.absolutePath}")
            } catch (e: Exception) {
                android.util.Log.e("ShareJourneyScreen", "Error copying URI $uri: ${e.message}", e)
                // Try to use original URI as fallback
                try {
                    context.contentResolver.openInputStream(uri)?.use {
                        tempFiles.add(uri)
                    }
                } catch (e2: Exception) {
                    android.util.Log.e("ShareJourneyScreen", "Cannot access URI $uri at all: ${e2.message}")
                }
            }
        }
        
        selectedImages = (selectedImages + tempFiles).take(20)
        
        if (tempFiles.size < uris.size) {
            android.util.Log.w("ShareJourneyScreen", "Some images could not be processed: ${uris.size - tempFiles.size} failed")
        }
    }
    
    // Handle upload success - redirect to JourneyFeedScreen
    LaunchedEffect(uploadState) {
        if (uploadState is JourneyUploadUiState.Success) {
            // Navigate to journey feed screen to show the shared journeys
            navController.navigate("journey_feed") {
                // Clear back stack so user can't go back to upload screen
                popUpTo("journey_feed") { inclusive = false }
            }
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(StringTranslator.translate(context, "Partager mon voyage")) },
                navigationIcon = {
                    IconButton(onClick = { 
                        // Naviguer vers profile et supprimer share_journey de la pile de navigation
                        navController.navigate("profile") {
                            popUpTo("share_journey") { inclusive = true }
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.surface
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                // Info Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF4A90E2)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "💡 ${StringTranslator.translate(context, "Astuce")}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = StringTranslator.translate(context, "Sélectionnez vos meilleures photos de voyage pour les partager avec la communauté."),
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "🎬 ${StringTranslator.translate(context, "Une vidéo AI sera automatiquement générée avec vos photos et de la musique !")}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.9f),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            item {
                // Image Selection Section
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = StringTranslator.translate(context, "Photos du voyage"),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        if (selectedImages.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFFF5F5F5))
                                    .clickable {
                                        imagePickerLauncher.launch("image/*")
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Add Images",
                                        modifier = Modifier.size(48.dp),
                                        tint = Color(0xFF4A90E2)
                                    )
                                    Text(
                                        text = StringTranslator.translate(context, "Ajouter des photos"),
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = Color(0xFF4A90E2)
                                    )
                                    Text(
                                        text = StringTranslator.translate(context, "Jusqu'à 20 photos"),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.Gray
                                    )
                                }
                            }
                        } else {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(3),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.height(300.dp)
                            ) {
                                items(selectedImages) { uri ->
                                    Box(
                                        modifier = Modifier
                                            .aspectRatio(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                    ) {
                                        AsyncImage(
                                            model = uri,
                                            contentDescription = "Selected Image",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                        IconButton(
                                            onClick = {
                                                selectedImages = selectedImages - uri
                                            },
                                            modifier = Modifier.align(Alignment.TopEnd)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Remove",
                                                tint = Color.White,
                                                modifier = Modifier
                                                    .size(24.dp)
                                                    .background(
                                                        Color.Black.copy(alpha = 0.6f),
                                                        RoundedCornerShape(12.dp)
                                                    )
                                                    .padding(4.dp)
                                            )
                                        }
                                    }
                                }

                                if (selectedImages.size < 20) {
                                    item {
                                        Box(
                                            modifier = Modifier
                                                .aspectRatio(1f)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(Color(0xFFF5F5F5))
                                                .clickable {
                                                    imagePickerLauncher.launch("image/*")
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Add,
                                                contentDescription = "Add More",
                                                tint = Color(0xFF4A90E2),
                                                modifier = Modifier.size(32.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                // Destination Section (Select from confirmed bookings' destinations)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = StringTranslator.translate(context, "Destination du voyage"),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    val currentBookingState = bookingHistoryState
                    
                    // Loading state
                    if (currentBookingState is BookingUiState.Loading) {
                        OutlinedTextField(
                            value = StringTranslator.translate(context, "Chargement des réservations..."),
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(StringTranslator.translate(context, "Destination")) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            enabled = false,
                            trailingIcon = {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp
                                )
                            }
                        )
                    }
                    // Error state
                    else if (currentBookingState is BookingUiState.Error) {
                        OutlinedTextField(
                            value = StringTranslator.translate(context, "Erreur de chargement"),
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(StringTranslator.translate(context, "Destination")) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            enabled = false,
                            colors = OutlinedTextFieldDefaults.colors(
                                disabledTextColor = Color.Red,
                                disabledBorderColor = Color.Red.copy(alpha = 0.5f),
                                disabledLabelColor = Color.Red.copy(alpha = 0.6f)
                            )
                        )
                        Text(
                            text = "Erreur: ${currentBookingState.message}. Appuyez pour réessayer.",
                            color = Color.Red,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier
                                .padding(top = 8.dp)
                                .clickable { bookingViewModel.loadBookingHistory() }
                        )
                    }
                    // Success state with bookings
                    else if (currentBookingState is BookingUiState.Success) {
                        if (confirmedBookings.isEmpty()) {
                            OutlinedTextField(
                                value = StringTranslator.translate(context, "Aucune réservation confirmée"),
                                onValueChange = {},
                                readOnly = true,
                                label = { Text(StringTranslator.translate(context, "Destination")) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                enabled = false,
                                colors = OutlinedTextFieldDefaults.colors(
                                    disabledTextColor = Color(0xFFFF9800),
                                    disabledBorderColor = Color(0xFFFF9800).copy(alpha = 0.5f),
                                    disabledLabelColor = Color(0xFFFF9800).copy(alpha = 0.6f)
                                )
                            )
                            Text(
                                text = StringTranslator.translate(context, "Vous devez avoir une réservation confirmée pour partager votre voyage."),
                                color = Color(0xFFFF9800),
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        } else if (uniqueDestinations.isEmpty()) {
                            OutlinedTextField(
                                value = StringTranslator.translate(context, "Aucune destination trouvée dans les réservations"),
                                onValueChange = {},
                                readOnly = true,
                                label = { Text(StringTranslator.translate(context, "Destination")) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                enabled = false,
                                colors = OutlinedTextFieldDefaults.colors(
                                    disabledTextColor = Color(0xFFFF9800),
                                    disabledBorderColor = Color(0xFFFF9800).copy(alpha = 0.5f),
                                    disabledLabelColor = Color(0xFFFF9800).copy(alpha = 0.6f)
                                )
                            )
                            Text(
                                text = StringTranslator.translate(context, "Vos réservations confirmées n'ont pas de destination spécifiée. Veuillez mettre à jour vos réservations."),
                                color = Color(0xFFFF9800),
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        } else {
                            // Dropdown menu for selecting destination from bookings
                            ExposedDropdownMenuBox(
                                expanded = destinationDropdownExpanded,
                                onExpandedChange = { destinationDropdownExpanded = !destinationDropdownExpanded }
                            ) {
                                val displayValue = selectedBooking?.let { booking ->
                                    booking.tripDetails?.destination?.takeIf { it.isNotBlank() }
                                        ?: "Réservation ${booking.confirmationNumber}"
                                } ?: "Sélectionnez une destination"
                                
                                OutlinedTextField(
                                    value = displayValue,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text(StringTranslator.translate(context, "Sélectionnez votre destination")) },
                                    trailingIcon = {
                                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = destinationDropdownExpanded)
                                    },
                                    modifier = Modifier
                                        .menuAnchor()
                                        .fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = if (selectedBooking != null && selectedBooking?.tripDetails?.destination.isNullOrBlank()) {
                                        OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = Color(0xFFFF9800),
                                            unfocusedTextColor = Color(0xFFFF9800)
                                        )
                                    } else {
                                        OutlinedTextFieldDefaults.colors()
                                    }
                                )
                                ExposedDropdownMenu(
                                    expanded = destinationDropdownExpanded,
                                    onDismissRequest = { destinationDropdownExpanded = false }
                                ) {
                                    uniqueDestinations.forEach { booking ->
                                        val destination = booking.tripDetails?.destination?.takeIf { it.isNotBlank() }
                                        val displayText = destination ?: "Réservation ${booking.confirmationNumber}"
                                        
                                        DropdownMenuItem(
                                            text = { 
                                                Column {
                                                    Text(
                                                        text = displayText,
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        fontWeight = FontWeight.Medium,
                                                        color = if (destination == null) Color(0xFFFF9800) else Color.Unspecified
                                                    )
                                                    Text(
                                                        text = "Confirmation: ${booking.confirmationNumber}",
                                                        style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray)
                                                    )
                                                    if (booking.bookingDate.isNotBlank()) {
                                                        Text(
                                                            text = "Date: ${booking.bookingDate.split("T")[0]}",
                                                            style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray)
                                                        )
                                                    }
                                                    if (destination == null) {
                                                        Text(
                                                            text = "⚠️ Destination non spécifiée",
                                                            style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFFF9800)),
                                                            modifier = Modifier.padding(top = 4.dp)
                                                        )
                                                    }
                                                }
                                            },
                                            onClick = {
                                                selectedBooking = booking
                                                destinationDropdownExpanded = false
                                                android.util.Log.d("ShareJourneyScreen", "Selected booking: ${booking.id} - destination: ${destination ?: "non spécifiée"}")
                                            },
                                            enabled = true
                                        )
                                    }
                                }
                            }
                            
                            // Warning if selected booking has no destination
                            if (selectedBooking != null && selectedBooking?.tripDetails?.destination.isNullOrBlank()) {
                                Text(
                                    text = "⚠️ Cette réservation n'a pas de destination spécifiée. Le système utilisera le nom de la réservation comme destination.",
                                    color = Color(0xFFFF9800),
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                            }
                        }
                    }
                }
            }
            }
            
            item {
                // Description Section
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = StringTranslator.translate(context, "Description (optionnel)"),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = description,
                            onValueChange = { description = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text(StringTranslator.translate(context, "Décrivez votre voyage...")) },
                            maxLines = 4,
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }
            }

            item {
                // Tags Section
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = StringTranslator.translate(context, "Tags (optionnel)"),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = tags,
                            onValueChange = { tags = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text(StringTranslator.translate(context, "Ex: plage, montagne, culture (séparés par des virgules)")) },
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }
            }

            item {
                // Upload Button with Progress
                val currentUploadState = uploadState
                when (currentUploadState) {
                is JourneyUploadUiState.Compressing -> {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        LinearProgressIndicator(
                            progress = { currentUploadState.progress.toFloat() / currentUploadState.total.toFloat() },
                            modifier = Modifier.fillMaxWidth(),
                            color = Color(0xFF4A90E2)
                        )
                        Button(
                            onClick = { },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            enabled = false,
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF4A90E2)
                            )
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Compression: ${currentUploadState.progress}/${currentUploadState.total}",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                is JourneyUploadUiState.Uploading -> {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        LinearProgressIndicator(
                            progress = { currentUploadState.progress.toFloat() / currentUploadState.total.toFloat() },
                            modifier = Modifier.fillMaxWidth(),
                            color = Color(0xFF4A90E2)
                        )
                    Button(
                        onClick = { },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        enabled = false,
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4A90E2)
                        )
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Upload: ${currentUploadState.progress}/${currentUploadState.total}",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                is JourneyUploadUiState.Error -> {
                    val errorMessage = currentUploadState.message
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = errorMessage,
                            modifier = Modifier.padding(16.dp),
                            color = Color(0xFFC62828)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            if (selectedImages.isNotEmpty() && selectedBooking != null) {
                                val tagsList = tags.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                                val destinationName = selectedBooking?.tripDetails?.destination?.takeIf { it.isNotBlank() }
                                journeyViewModel.uploadJourney(
                                    imageUris = selectedImages,
                                    bookingId = selectedBooking?.id,
                                    destination = destinationName,
                                    description = description.takeIf { it.isNotEmpty() },
                                    tags = tagsList.takeIf { it.isNotEmpty() },
                                    isPublic = true,
                                    context = context
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4A90E2)
                        )
                    ) {
                        Text(StringTranslator.translate(context, "Réessayer"), fontWeight = FontWeight.Bold)
                    }
                }
                else -> {
                    Button(
                        onClick = {
                            if (selectedImages.isNotEmpty() && selectedBooking != null) {
                                val tagsList = tags.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                                val destinationName = selectedBooking?.tripDetails?.destination?.takeIf { it.isNotBlank() }
                                journeyViewModel.uploadJourney(
                                    imageUris = selectedImages,
                                    bookingId = selectedBooking?.id,
                                    destination = destinationName,
                                    description = description.takeIf { it.isNotEmpty() },
                                    tags = tagsList.takeIf { it.isNotEmpty() },
                                    isPublic = true,
                                    context = context
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        enabled = selectedImages.isNotEmpty() && selectedBooking != null && currentUploadState !is JourneyUploadUiState.Uploading && currentUploadState !is JourneyUploadUiState.Compressing,
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4A90E2)
                        )
                    ) {
                        Text(StringTranslator.translate(context, "Partager mon voyage"), fontWeight = FontWeight.Bold)
                    }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ShareJourneyScreenPreview() {
    WayFinderTheme {
        ShareJourneyScreen(rememberNavController())
    }
}

