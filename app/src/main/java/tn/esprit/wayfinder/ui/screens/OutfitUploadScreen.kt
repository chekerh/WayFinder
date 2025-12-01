package tn.esprit.wayfinder.ui.screens

import android.app.Application
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.core.content.FileProvider
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import tn.esprit.wayfinder.models.Outfit
import tn.esprit.wayfinder.presentation.auth.ViewModelFactory
import tn.esprit.wayfinder.ui.components.CustomBottomNavigationBar
import tn.esprit.wayfinder.utils.StringTranslator
import tn.esprit.wayfinder.viewmodels.OutfitWeatherViewModel
import tn.esprit.wayfinder.viewmodels.OutfitWeatherUiState
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OutfitUploadScreen(
    navController: NavController,
    bookingId: String
) {
    val context = LocalContext.current
    
    // Debug log
    LaunchedEffect(bookingId) {
        android.util.Log.d("OutfitUploadScreen", "Screen initialized with bookingId: $bookingId")
    }
    
    val viewModel: OutfitWeatherViewModel = viewModel(
        factory = ViewModelFactory(context.applicationContext as Application)
    )
    val uiState by viewModel.uiState.collectAsState()
    val outfitHistory by viewModel.outfitHistory.collectAsState()
    val isLoadingHistory by viewModel.isLoadingHistory.collectAsState()

    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var imageFile by remember { mutableStateOf<File?>(null) }

    // Load outfit history when screen is created
    LaunchedEffect(bookingId) {
        viewModel.loadOutfitHistory(bookingId)
    }

    // Create temp file for camera - recreate each time to avoid conflicts
    fun createTempImageFile(): File {
        val file = File(context.cacheDir, "outfit_temp_${System.currentTimeMillis()}.jpg")
        file.parentFile?.mkdirs()
        return file
    }
    
    fun getTempImageUri(file: File): Uri {
        return try {
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
        } catch (e: Exception) {
            android.util.Log.e("OutfitUploadScreen", "Error creating URI: ${e.message}", e)
            throw e
        }
    }

    // Camera launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && imageFile != null) {
            selectedImageUri = imageFile?.let { getTempImageUri(it) }
            android.util.Log.d("OutfitUploadScreen", "Photo taken: ${imageFile?.name}, size: ${imageFile?.length()}")
        } else {
            android.util.Log.e("OutfitUploadScreen", "Camera capture failed or imageFile is null")
        }
    }

    // Permission launcher for camera
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            try {
                val tempFile = createTempImageFile()
                imageFile = tempFile
                val tempUri = getTempImageUri(tempFile)
                cameraLauncher.launch(tempUri)
            } catch (e: Exception) {
                android.util.Log.e("OutfitUploadScreen", "Error launching camera: ${e.message}", e)
            }
        }
    }

    // Gallery launcher
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                selectedImageUri = it
                val tempFile = File(context.cacheDir, "outfit_${System.currentTimeMillis()}.jpg")
                tempFile.parentFile?.mkdirs()
                context.contentResolver.openInputStream(it)?.use { input ->
                    tempFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                if (tempFile.exists() && tempFile.length() > 0) {
                    imageFile = tempFile
                    android.util.Log.d("OutfitUploadScreen", "Image selected from gallery: ${tempFile.name}, size: ${tempFile.length()}")
                } else {
                    android.util.Log.e("OutfitUploadScreen", "Failed to copy image file")
                }
            } catch (e: Exception) {
                android.util.Log.e("OutfitUploadScreen", "Error processing gallery image: ${e.message}", e)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        StringTranslator.translate(context, "Vérifier ma tenue"),
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            CustomBottomNavigationBar(navController = navController)
        },
        containerColor = MaterialTheme.colorScheme.surface
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Instructions
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = StringTranslator.translate(context, "Prenez une photo de votre tenue"),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = StringTranslator.translate(
                            context,
                            "L'application analysera votre tenue et vous donnera des recommandations basées sur la météo de votre destination."
                        ),
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }
            }

            // Image Preview or Upload Area
            if (selectedImageUri != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(400.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    AsyncImage(
                        model = selectedImageUri,
                        contentDescription = "Selected outfit",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }

                // Upload Button
                Button(
                    onClick = {
                        when {
                            imageFile != null && imageFile!!.exists() -> {
                                val file = imageFile!!
                                android.util.Log.d("OutfitUploadScreen", "Upload button clicked, file: ${file.name}, size: ${file.length()}, bookingId: $bookingId")
                                val requestFile = file.asRequestBody("image/jpeg".toMediaType())
                                val imagePart = MultipartBody.Part.createFormData("image", file.name, requestFile)
                                viewModel.uploadOutfit(imagePart, bookingId)
                            }
                            selectedImageUri != null -> {
                                android.util.Log.e("OutfitUploadScreen", "Image URI exists but file is missing. URI: $selectedImageUri")
                                // Try to recreate file from URI
                                selectedImageUri?.let { uri ->
                                    try {
                                        val tempFile = File(context.cacheDir, "outfit_${System.currentTimeMillis()}.jpg")
                                        tempFile.parentFile?.mkdirs()
                                        context.contentResolver.openInputStream(uri)?.use { input ->
                                            tempFile.outputStream().use { output ->
                                                input.copyTo(output)
                                            }
                                        }
                                        if (tempFile.exists() && tempFile.length() > 0) {
                                            imageFile = tempFile
                                            val requestFile = tempFile.asRequestBody("image/jpeg".toMediaType())
                                            val imagePart = MultipartBody.Part.createFormData("image", tempFile.name, requestFile)
                                            viewModel.uploadOutfit(imagePart, bookingId)
                                        }
                                    } catch (e: Exception) {
                                        android.util.Log.e("OutfitUploadScreen", "Error recreating file from URI: ${e.message}", e)
                                    }
                                }
                            }
                            else -> {
                                android.util.Log.e("OutfitUploadScreen", "No image file or URI selected")
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    enabled = !viewModel.isLoading && (imageFile != null || selectedImageUri != null),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF1976D2)
                    )
                ) {
                    if (viewModel.isLoading) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = Color.White
                            )
                            Text(
                                text = StringTranslator.translate(context, "Analyse en cours..."),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        Text(
                            text = StringTranslator.translate(context, "Analyser ma tenue"),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Change Image Button
                OutlinedButton(
                    onClick = {
                        selectedImageUri = null
                        imageFile = null
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(StringTranslator.translate(context, "Changer la photo"))
                }
            } else {
                // Upload Options
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Camera Button
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .height(200.dp)
                            .clickable {
                                when {
                                    ContextCompat.checkSelfPermission(
                                        context,
                                        android.Manifest.permission.CAMERA
                                    ) == PackageManager.PERMISSION_GRANTED -> {
                                        try {
                                            val tempFile = createTempImageFile()
                                            imageFile = tempFile
                                            val tempUri = getTempImageUri(tempFile)
                                            cameraLauncher.launch(tempUri)
                                        } catch (e: Exception) {
                                            android.util.Log.e("OutfitUploadScreen", "Error launching camera: ${e.message}", e)
                                        }
                                    }
                                    else -> {
                                        permissionLauncher.launch(android.Manifest.permission.CAMERA)
                                    }
                                }
                            },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CameraAlt,
                                contentDescription = "Camera",
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = StringTranslator.translate(context, "Caméra"),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    // Gallery Button
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .height(200.dp)
                            .clickable {
                                galleryLauncher.launch("image/*")
                            },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.PhotoLibrary,
                                contentDescription = "Gallery",
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = StringTranslator.translate(context, "Galerie"),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // Error Message
            viewModel.errorMessage?.let { error ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = error,
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }

            // Outfit History Section
            if (outfitHistory.isNotEmpty() || isLoadingHistory) {
                OutfitHistorySection(
                    outfits = outfitHistory,
                    isLoading = isLoadingHistory,
                    bookingId = bookingId,
                    viewModel = viewModel,
                    onOutfitClick = { outfit ->
                        outfit._id?.let { outfitId ->
                            navController.navigate("outfit_result/$outfitId")
                        }
                    }
                )
            }
        }
    }

    // Navigate to results when upload succeeds and refresh history
    LaunchedEffect(uiState) {
        val currentState = uiState
        when (currentState) {
            is OutfitWeatherUiState.Success -> {
                val outfit = currentState.outfit
                android.util.Log.d("OutfitUploadScreen", "Upload successful, navigating to results. Outfit ID: ${outfit._id}")
                // Refresh history after successful upload
                viewModel.loadOutfitHistory(bookingId)
                // Only pass the outfit ID, not the whole object (SavedStateHandle can't store non-serializable objects)
                val outfitId = outfit._id ?: "unknown"
                navController.navigate("outfit_result/$outfitId") {
                    popUpTo("outfit_upload/$bookingId") { inclusive = true }
                }
            }
            is OutfitWeatherUiState.Error -> {
                android.util.Log.e("OutfitUploadScreen", "Upload error: ${currentState.message}")
            }
            is OutfitWeatherUiState.Loading -> {
                android.util.Log.d("OutfitUploadScreen", "Upload in progress...")
            }
            else -> {}
        }
    }
}

@Composable
fun OutfitHistorySection(
    outfits: List<Outfit>,
    isLoading: Boolean,
    bookingId: String,
    viewModel: OutfitWeatherViewModel,
    onOutfitClick: (Outfit) -> Unit
) {
    var outfitToDelete by remember { mutableStateOf<Outfit?>(null) }
    val context = LocalContext.current
    
    if (isLoading && outfits.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    if (outfits.isEmpty()) {
        return
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Section Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = StringTranslator.translate(context, "Historique des tenues"),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Text(
                            text = "${outfits.size}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
            Text(
                text = "${outfits.size} ${StringTranslator.translate(context, "tenues")}",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }

        // Group outfits by date
        val outfitsByDate = outfits.groupBy { outfit ->
            outfit.createdAt?.let { dateString ->
                try {
                    val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
                    sdf.timeZone = TimeZone.getTimeZone("UTC")
                    val date = sdf.parse(dateString)
                    val dateFormat = SimpleDateFormat("EEEE, MMM dd", Locale.getDefault())
                    dateFormat.format(date ?: Date())
                } catch (e: Exception) {
                    "Date inconnue"
                }
            } ?: "Date inconnue"
        }

        // Display outfits grouped by date
        outfitsByDate.forEach { (date, dayOutfits) ->
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Date Header
                Text(
                    text = date,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    modifier = Modifier.padding(vertical = 4.dp)
                )

                // Outfit Cards Row
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(dayOutfits) { outfit ->
                        OutfitHistoryCard(
                            outfit = outfit,
                            onClick = { onOutfitClick(outfit) },
                            onDeleteClick = { outfitToDelete = outfit }
                        )
                    }
                }
            }
        }
    }

    // Delete Confirmation Dialog
    outfitToDelete?.let { outfit ->
        AlertDialog(
            onDismissRequest = { outfitToDelete = null },
            title = {
                Text(
                    text = StringTranslator.translate(context, "Supprimer cette tenue ?"),
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = StringTranslator.translate(context, "Cette action ne peut pas être annulée.")
                )
            },
            confirmButton = {
                val scope = rememberCoroutineScope()
                
                TextButton(
                    onClick = {
                        outfit._id?.let { outfitId ->
                            android.util.Log.d("OutfitUploadScreen", "Delete confirmed for outfit: $outfitId")
                            viewModel.deleteOutfit(outfitId, bookingId)
                            // Close dialog after a short delay to allow UI update
                            scope.launch {
                                delay(300)
                                outfitToDelete = null
                            }
                        } ?: run {
                            android.util.Log.e("OutfitUploadScreen", "Cannot delete: outfit ID is null")
                            outfitToDelete = null
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = Color(0xFFF44336)
                    )
                ) {
                    Text(
                        text = StringTranslator.translate(context, "Supprimer"),
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { outfitToDelete = null }
                ) {
                    Text(StringTranslator.translate(context, "Annuler"))
                }
            }
        )
    }
}

@Composable
fun OutfitHistoryCard(
    outfit: Outfit,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val context = LocalContext.current
    val recommendation = outfit.recommendation
    val weather = outfit.weather_data
    val score = recommendation?.score ?: 0

    Box(
        modifier = Modifier
            .width(180.dp)
            .height(240.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxSize(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            onClick = onClick
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Image
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                ) {
                    AsyncImage(
                        model = outfit.image_url,
                        contentDescription = "Outfit",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    // Score Badge
                    Surface(
                        shape = CircleShape,
                        color = when {
                            score >= 80 -> Color(0xFF4CAF50)
                            score >= 60 -> Color(0xFFFF9800)
                            else -> Color(0xFFF44336)
                        },
                        modifier = Modifier
                            .padding(8.dp)
                            .size(32.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Text(
                                text = "$score",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
                
                // Info Section
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                // Weather Info
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.WbSunny,
                        contentDescription = "Weather",
                        modifier = Modifier.size(16.dp),
                        tint = Color(0xFFFF9800)
                    )
                    Text(
                        text = "${weather?.temperature ?: 0}°C",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Status
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = "Status",
                        modifier = Modifier.size(16.dp),
                        tint = if (recommendation?.is_suitable == true) Color(0xFF4CAF50) else Color(0xFFFF9800)
                    )
                    Text(
                        text = if (recommendation?.is_suitable == true) "OK" else "À améliorer",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (recommendation?.is_suitable == true) Color(0xFF4CAF50) else Color(0xFFFF9800)
                    )
                }
            }
            }
        }
        // Delete Button - Outside the Card to prevent click propagation
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(4.dp)
                .size(28.dp)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = {
                            onDeleteClick()
                        }
                    )
                }
        ) {
            Surface(
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxSize()
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Delete",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

