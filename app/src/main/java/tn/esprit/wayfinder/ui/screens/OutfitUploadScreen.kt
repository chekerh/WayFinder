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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import tn.esprit.wayfinder.presentation.auth.ViewModelFactory
import tn.esprit.wayfinder.ui.components.CustomBottomNavigationBar
import tn.esprit.wayfinder.utils.StringTranslator
import tn.esprit.wayfinder.viewmodels.OutfitWeatherViewModel
import tn.esprit.wayfinder.viewmodels.OutfitWeatherUiState
import java.io.File

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

    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var imageFile by remember { mutableStateOf<File?>(null) }

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
        }
    }

    // Navigate to results when upload succeeds
    LaunchedEffect(uiState) {
        val currentState = uiState
        when (currentState) {
            is OutfitWeatherUiState.Success -> {
                val outfit = currentState.outfit
                android.util.Log.d("OutfitUploadScreen", "Upload successful, navigating to results. Outfit ID: ${outfit._id}")
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

