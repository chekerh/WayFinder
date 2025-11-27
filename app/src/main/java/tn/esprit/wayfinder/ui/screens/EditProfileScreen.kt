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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import tn.esprit.wayfinder.ui.theme.WayFinderTheme
import coil.compose.AsyncImage
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import tn.esprit.wayfinder.R
import tn.esprit.wayfinder.presentation.auth.ViewModelFactory
import tn.esprit.wayfinder.ui.components.CustomBottomNavigationBar
import tn.esprit.wayfinder.viewmodels.UserViewModel
import tn.esprit.wayfinder.viewmodels.UserUiState
import tn.esprit.wayfinder.utils.StringTranslator
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
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var bio by remember { mutableStateOf("") }
    var selectedPreferences by remember { mutableStateOf<Set<String>>(emptySet()) }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var currentProfileImageUrl by remember { mutableStateOf<String?>(null) }
    var passwordVisible by remember { mutableStateOf(false) }
    
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
                // Get the actual MIME type from the content resolver
                val mimeType = context.contentResolver.getType(it) ?: "image/jpeg"
                // Ensure we have a valid image MIME type
                val validMimeType = when {
                    mimeType.startsWith("image/") -> mimeType
                    else -> "image/jpeg" // Default fallback
                }
                
                // Determine file extension from MIME type
                val extension = when (validMimeType) {
                    "image/png" -> "png"
                    "image/jpeg", "image/jpg" -> "jpg"
                    "image/gif" -> "gif"
                    else -> "jpg"
                }
                
                val file = File(context.cacheDir, "temp_profile_${System.currentTimeMillis()}.$extension")
                inputStream?.use { stream ->
                    file.outputStream().use { output ->
                        stream.copyTo(output)
                    }
                }
                if (file.exists()) {
                    val requestFile = file.asRequestBody(validMimeType.toMediaTypeOrNull())
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
            username = user.username
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
                title = { 
                    Text(
                        StringTranslator.translate(context, "Edit Profile"),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
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
                        enabled = firstName.isNotBlank() && lastName.isNotBlank() && email.isNotBlank()
                    ) {
                        Icon(
                            Icons.Filled.Check,
                            contentDescription = "Save",
                            tint = if (firstName.isNotBlank() && lastName.isNotBlank() && email.isNotBlank()) 
                                Color(0xFF4CAF50) else Color.Gray
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
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
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
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Profile Image Section - Centered
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
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
                                        .size(100.dp)
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
                                        .size(100.dp)
                                        .clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            }
                            
                            // Camera Icon Overlay
                            IconButton(
                                onClick = { imagePickerLauncher.launch("image/*") },
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .size(36.dp)
                                    .background(Color(0xFF1976D2), CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.CameraAlt,
                                    contentDescription = "Change Profile Picture",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Name Field (Combined first and last name or separate)
                    OutlinedTextField(
                        value = "$firstName $lastName".trim(),
                        onValueChange = { 
                            val names = it.trim().split(" ")
                            firstName = names.firstOrNull() ?: ""
                            lastName = names.drop(1).joinToString(" ")
                        },
                        label = { Text(StringTranslator.translate(context, "Name")) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Gray,
                            unfocusedBorderColor = Color.LightGray
                        )
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Email Address
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text(StringTranslator.translate(context, "E mail address")) },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = KeyboardType.Email
                        ),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Gray,
                            unfocusedBorderColor = Color.LightGray
                        )
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // User name
                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text(StringTranslator.translate(context, "User name")) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Gray,
                            unfocusedBorderColor = Color.LightGray
                        ),
                        leadingIcon = {
                            Text(
                                text = "@",
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color.Gray,
                                modifier = Modifier.padding(start = 16.dp)
                            )
                        }
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Password
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text(StringTranslator.translate(context, "Password")) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = KeyboardType.Password
                        ),
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                    contentDescription = if (passwordVisible) "Hide password" else "Show password",
                                    tint = Color.Gray
                                )
                            }
                        },
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Gray,
                            unfocusedBorderColor = Color.LightGray
                        )
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Phone number
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text(StringTranslator.translate(context, "Phone number")) },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = KeyboardType.Phone
                        ),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Gray,
                            unfocusedBorderColor = Color.LightGray
                        ),
                        leadingIcon = {
                            Text(
                                text = "+91",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Gray,
                                modifier = Modifier.padding(start = 16.dp, end = 8.dp)
                            )
                        },
                        placeholder = { Text("6895312") }
                    )
                    
                    Spacer(modifier = Modifier.height(80.dp)) // Space for bottom nav
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun EditProfileScreenPreview() {
    WayFinderTheme {
        EditProfileScreen(rememberNavController())
    }
}

