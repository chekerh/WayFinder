package tn.esprit.wayfinder.ui.screens

import android.app.Application
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import tn.esprit.wayfinder.R
import tn.esprit.wayfinder.presentation.auth.ViewModelFactory
import tn.esprit.wayfinder.ui.components.CustomBottomNavigationBar
import tn.esprit.wayfinder.viewmodels.UserViewModel
import tn.esprit.wayfinder.viewmodels.UserUiState
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(navController: NavController) {
    val context = LocalContext.current
    val userViewModel: UserViewModel = viewModel(factory = ViewModelFactory(context.applicationContext as Application))
    val uiState by userViewModel.uiState.collectAsState()
    
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var bio by remember { mutableStateOf("") }
    var selectedPreferences by remember { mutableStateOf<Set<String>>(emptySet()) }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var currentProfileImageUrl by remember { mutableStateOf<String?>(null) }
    
    val availablePreferences = listOf(
        "Beach", "Mountain", "City", "Culture", "Adventure", "Relaxation",
        "Food", "Nightlife", "Shopping", "Nature", "History", "Art"
    )
    
    // Image picker launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedImageUri = it
            // Upload image immediately when selected
            try {
                val inputStream = context.contentResolver.openInputStream(it)
                val file = File(context.cacheDir, "temp_profile_${System.currentTimeMillis()}.jpg")
                inputStream?.use { stream ->
                    file.outputStream().use { output ->
                        stream.copyTo(output)
                    }
                }
                if (file.exists()) {
                    val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
                    val imagePart = MultipartBody.Part.createFormData("image", file.name, requestFile)
                    userViewModel.uploadProfileImage(imagePart)
                }
            } catch (e: Exception) {
                // Handle error - could show a snackbar
            }
        }
    }
    
    // Load user data
    LaunchedEffect(Unit) {
        userViewModel.loadProfile()
    }
    
    // Update fields when user data is loaded
    LaunchedEffect(uiState) {
        if (uiState is UserUiState.Success) {
            val user = (uiState as UserUiState.Success).user
            firstName = user.firstName
            lastName = user.lastName
            email = user.email
            phone = user.phone.orEmpty()
            location = user.location.orEmpty()
            bio = user.bio.orEmpty()
            selectedPreferences = user.preferences.toSet()
            currentProfileImageUrl = user.profileImageUrl
        }
    }
    
    // Handle successful update
    LaunchedEffect(uiState) {
        if (uiState is UserUiState.Success && firstName.isNotBlank()) {
            // Could show a snackbar or navigate back
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Modifier le profil", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFEAF2FF)
                )
            )
        },
        bottomBar = {
            CustomBottomNavigationBar(navController = navController)
        },
        containerColor = Color(0xFFEAF2FF)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            when (uiState) {
                is UserUiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                is UserUiState.Error -> {
                    Text(
                        text = (uiState as UserUiState.Error).message,
                        color = Color.Red
                    )
                }
                else -> {
                    // Profile Image Section
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Box {
                            // Profile Image
                            val imageToShow = selectedImageUri ?: currentProfileImageUrl?.let { url ->
                                if (url.startsWith("http")) url else "https://wayfinder-api-w92x.onrender.com$url"
                            }
                            if (imageToShow != null) {
                                AsyncImage(
                                    model = imageToShow.toString(),
                                    contentDescription = "Profile Picture",
                                    modifier = Modifier
                                        .size(120.dp)
                                        .clip(CircleShape),
                                    contentScale = ContentScale.Crop,
                                    placeholder = painterResource(id = R.drawable.europe),
                                    error = painterResource(id = R.drawable.europe)
                                )
                            } else {
                                Image(
                                    painter = painterResource(id = R.drawable.europe),
                                    contentDescription = "Profile Picture",
                                    modifier = Modifier
                                        .size(120.dp)
                                        .clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            }
                            
                            // Camera Icon Overlay
                            IconButton(
                                onClick = { imagePickerLauncher.launch("image/*") },
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .size(40.dp)
                                    .background(Color(0xFF1976D2), CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.CameraAlt,
                                    contentDescription = "Change Profile Picture",
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // First Name
                    OutlinedTextField(
                        value = firstName,
                        onValueChange = { firstName = it },
                        label = { Text("Prénom") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    
                    // Last Name
                    OutlinedTextField(
                        value = lastName,
                        onValueChange = { lastName = it },
                        label = { Text("Nom") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    
                    // Email
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = KeyboardType.Email
                        ),
                        singleLine = true
                    )
                    
                    // Phone
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Téléphone") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = KeyboardType.Phone
                        ),
                        singleLine = true,
                        placeholder = { Text("+123456789") }
                    )
                    
                    // Location
                    OutlinedTextField(
                        value = location,
                        onValueChange = { location = it },
                        label = { Text("Localisation") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        placeholder = { Text("Paris, France") }
                    )
                    
                    // Bio
                    OutlinedTextField(
                        value = bio,
                        onValueChange = { bio = it },
                        label = { Text("Bio") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        maxLines = 5
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Preferences Section
                    Text(
                        text = "Préférences de voyage",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Sélectionnez vos intérêts pour des recommandations personnalisées",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Preferences Chips
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Display preferences in rows of 3
                        availablePreferences.chunked(3).forEach { rowPreferences ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                rowPreferences.forEach { preference ->
                                    FilterChip(
                                        selected = selectedPreferences.contains(preference),
                                        onClick = {
                                            selectedPreferences = if (selectedPreferences.contains(preference)) {
                                                selectedPreferences - preference
                                            } else {
                                                selectedPreferences + preference
                                            }
                                        },
                                        label = { Text(preference) },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                // Fill remaining space if row has less than 3 items
                                repeat(3 - rowPreferences.size) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Save Button
                    Button(
                        onClick = {
                            userViewModel.updateProfile(
                                firstName = firstName,
                                lastName = lastName,
                                email = email,
                                phone = phone.ifBlank { null },
                                location = location.ifBlank { null },
                                bio = bio.ifBlank { null },
                                preferences = selectedPreferences.toList()
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF1976D2)
                        ),
                        enabled = firstName.isNotBlank() && lastName.isNotBlank() && email.isNotBlank()
                    ) {
                        if (uiState is UserUiState.Loading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = Color.White
                            )
                        } else {
                            Text(
                                text = "Enregistrer",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    
                    // Show success message
                    if (uiState is UserUiState.Success && firstName.isNotBlank()) {
                        Text(
                            text = "Profil mis à jour avec succès!",
                            color = Color(0xFF4CAF50),
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            }
        }
    }
}

