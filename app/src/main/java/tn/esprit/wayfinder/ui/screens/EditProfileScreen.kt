package tn.esprit.wayfinder.ui.screens

import android.app.Application
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
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
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.delay
import tn.esprit.wayfinder.ui.theme.WayFinderTheme
import tn.esprit.wayfinder.ui.theme.WayFinderBackground
import tn.esprit.wayfinder.ui.theme.WayFinderSurface
import android.widget.Toast
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
    var showSuccessAnimation by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    
    // Image picker launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedImageUri = it
            // Upload image immediately when selected
            try {
                val inputStream = context.contentResolver.openInputStream(it)
                val mimeType = context.contentResolver.getType(it) ?: "image/jpeg"
                val validMimeType = when {
                    mimeType.startsWith("image/") -> mimeType
                    else -> "image/jpeg"
                }
                
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
                // Handle error
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
    
    // Handle successful update and navigate back
    LaunchedEffect(uiState) {
        if (uiState is UserUiState.Success && isSaving) {
            showSuccessAnimation = true
            isSaving = false
            Toast.makeText(context, StringTranslator.translate(context, "Profile updated successfully!"), Toast.LENGTH_SHORT).show()
            delay(1500) // Show success animation briefly
            showSuccessAnimation = false
            // Navigate back to profile screen after successful save
            navController.popBackStack()
        }
    }
    
    // Handle errors
    LaunchedEffect(uiState) {
        if (uiState is UserUiState.Error && isSaving) {
            isSaving = false
            Toast.makeText(context, (uiState as UserUiState.Error).message, Toast.LENGTH_LONG).show()
        }
    }
    
    val colorScheme = MaterialTheme.colorScheme
    val isValid = firstName.isNotBlank() && lastName.isNotBlank() && email.isNotBlank()
    
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
                    AnimatedSaveButton(
                        enabled = isValid && !isSaving,
                        isSaving = isSaving,
                        showSuccess = showSuccessAnimation,
                        onClick = {
                            isSaving = true
                            userViewModel.updateProfile(
                                firstName = firstName,
                                lastName = lastName,
                                email = email,
                                phone = phone.ifBlank { null },
                                location = location.ifBlank { null },
                                bio = bio.ifBlank { null },
                                preferences = selectedPreferences.toList()
                            )
                        }
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colorScheme.surface
                )
            )
        },
        bottomBar = {
            CustomBottomNavigationBar(navController = navController)
        },
        containerColor = WayFinderBackground
    ) { paddingValues ->
        when (uiState) {
            is UserUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            is UserUiState.Error -> {
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
                            text = (uiState as UserUiState.Error).message,
                            color = MaterialTheme.colorScheme.error
                        )
                        Button(onClick = { userViewModel.loadProfile() }) {
                            Text(StringTranslator.translate(context, "Réessayer"))
                        }
                    }
                }
            }
            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .verticalScroll(rememberScrollState())
                ) {
                    // Profile Picture Section with Animation
                    AnimatedProfilePictureSection(
                        selectedImageUri = selectedImageUri,
                        currentProfileImageUrl = currentProfileImageUrl,
                        onImageClick = { imagePickerLauncher.launch("image/*") }
                    )
                    
                    Spacer(modifier = Modifier.height(32.dp))
                    
                    // Personal Information Card
                    ProfileSectionCard(
                        title = StringTranslator.translate(context, "Personal Information")
                    ) {
                        // Name Field
                        ModernTextField(
                            value = "$firstName $lastName".trim(),
                            onValueChange = { 
                                val names = it.trim().split(" ")
                                firstName = names.firstOrNull() ?: ""
                                lastName = names.drop(1).joinToString(" ")
                            },
                            label = StringTranslator.translate(context, "Name"),
                            leadingIcon = null
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Email Field
                        ModernTextField(
                            value = email,
                            onValueChange = { email = it },
                            label = StringTranslator.translate(context, "E mail address"),
                            keyboardType = KeyboardType.Email,
                            leadingIcon = null
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Username Field
                        ModernTextField(
                            value = username,
                            onValueChange = { username = it },
                            label = StringTranslator.translate(context, "User name"),
                            leadingIcon = {
                                Text(
                                    text = "@",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(start = 4.dp)
                                )
                            }
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Security Card
                    ProfileSectionCard(
                        title = StringTranslator.translate(context, "Security")
                    ) {
                        ModernTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = StringTranslator.translate(context, "Password"),
                            keyboardType = KeyboardType.Password,
                            isPassword = true,
                            passwordVisible = passwordVisible,
                            onPasswordVisibilityToggle = { passwordVisible = !passwordVisible },
                            leadingIcon = null
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Contact Information Card
                    ProfileSectionCard(
                        title = StringTranslator.translate(context, "Contact Information")
                    ) {
                        ModernTextField(
                            value = phone,
                            onValueChange = { phone = it },
                            label = StringTranslator.translate(context, "Phone number"),
                            keyboardType = KeyboardType.Phone,
                            leadingIcon = {
                                Text(
                                    text = "+216",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(start = 4.dp, end = 8.dp)
                                )
                            },
                            placeholder = "20722076"
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(100.dp)) // Space for bottom nav
                }
            }
        }
    }
}

@Composable
fun AnimatedProfilePictureSection(
    selectedImageUri: Uri?,
    currentProfileImageUrl: String?,
    onImageClick: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val imageToShow = selectedImageUri ?: currentProfileImageUrl?.let { url ->
        if (url.startsWith("http")) url else "https://wayfinder-api-w92x.onrender.com$url"
    }
    
    // Animation for profile picture
    val scale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "profile_scale"
    )
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .scale(scale)
                .zIndex(1f)
        ) {
            // Profile Image with gradient border - using app's blue theme
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                tn.esprit.wayfinder.ui.theme.WayFinderBlue,
                                tn.esprit.wayfinder.ui.theme.WayFinderBlueLight,
                                tn.esprit.wayfinder.ui.theme.WayFinderBlue
                            )
                        ),
                        shape = CircleShape
                    )
                    .padding(4.dp)
                    .background(WayFinderSurface, CircleShape)
                    .clickable(onClick = onImageClick)
            ) {
                if (imageToShow != null) {
                    AsyncImage(
                        model = imageToShow.toString(),
                        contentDescription = "Profile Picture",
                        modifier = Modifier
                            .fillMaxSize()
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
                            .fillMaxSize()
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                }
            }
            
            // Camera Icon Overlay with animation
            val cameraScale by animateFloatAsState(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                ),
                label = "camera_scale"
            )
            
            FloatingActionButton(
                onClick = onImageClick,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(40.dp)
                    .scale(cameraScale),
                containerColor = colorScheme.primary,
                contentColor = Color.White
            ) {
                Icon(
                    imageVector = Icons.Filled.CameraAlt,
                    contentDescription = "Change Profile Picture",
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun ProfileSectionCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = WayFinderSurface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 1.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = colorScheme.primary,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            content()
        }
    }
}

@Composable
fun ModernTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    isPassword: Boolean = false,
    passwordVisible: Boolean = false,
    onPasswordVisibilityToggle: (() -> Unit)? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    placeholder: String? = null
) {
    val colorScheme = MaterialTheme.colorScheme
    
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { 
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium
            ) 
        },
        placeholder = placeholder?.let { { Text(it) } },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
            keyboardType = keyboardType
        ),
        visualTransformation = if (isPassword && !passwordVisible) 
            PasswordVisualTransformation() 
        else 
            VisualTransformation.None,
        leadingIcon = leadingIcon,
        trailingIcon = if (isPassword && onPasswordVisibilityToggle != null) {
            {
                IconButton(onClick = onPasswordVisibilityToggle) {
                    Icon(
                        imageVector = if (passwordVisible) 
                            Icons.Filled.Visibility 
                        else 
                            Icons.Filled.VisibilityOff,
                        contentDescription = if (passwordVisible) 
                            "Hide password" 
                        else 
                            "Show password",
                        tint = colorScheme.onSurfaceVariant
                    )
                }
            }
        } else null,
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = colorScheme.primary,
            unfocusedBorderColor = colorScheme.outline.copy(alpha = 0.5f),
            focusedLabelColor = colorScheme.primary,
            unfocusedLabelColor = colorScheme.onSurfaceVariant
        )
    )
}

@Composable
fun AnimatedSaveButton(
    enabled: Boolean,
    isSaving: Boolean,
    showSuccess: Boolean,
    onClick: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    
    // Success animation
    val successScale by animateFloatAsState(
        targetValue = if (showSuccess) 1.2f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "success_scale"
    )
    
    IconButton(
        onClick = onClick,
        enabled = enabled
    ) {
        AnimatedContent(
            targetState = when {
                showSuccess -> SaveButtonState.Success
                isSaving -> SaveButtonState.Saving
                else -> SaveButtonState.Idle
            },
            transitionSpec = {
                fadeIn() + scaleIn() togetherWith fadeOut() + scaleOut()
            },
            label = "save_button_state"
        ) { state ->
            when (state) {
                SaveButtonState.Success -> {
                    Icon(
                        Icons.Filled.CheckCircle,
                        contentDescription = "Saved",
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.scale(successScale)
                    )
                }
                SaveButtonState.Saving -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = colorScheme.primary,
                        strokeWidth = 2.dp
                    )
                }
                SaveButtonState.Idle -> {
                    Icon(
                        Icons.Filled.Check,
                        contentDescription = "Save",
                        tint = if (enabled) 
                            Color(0xFF4CAF50) 
                        else 
                            colorScheme.onSurface.copy(alpha = 0.38f)
                    )
                }
            }
        }
    }
}

enum class SaveButtonState {
    Idle, Saving, Success
}

@Preview(showBackground = true)
@Composable
fun EditProfileScreenPreview() {
    WayFinderTheme {
        EditProfileScreen(rememberNavController())
    }
}