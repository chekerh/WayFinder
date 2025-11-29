package tn.esprit.wayfinder.ui.screens

import android.app.Application
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.material3.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import tn.esprit.wayfinder.ui.theme.WayFinderTheme
import coil.compose.AsyncImage
import tn.esprit.wayfinder.R
import tn.esprit.wayfinder.manager.TokenManager
import tn.esprit.wayfinder.manager.ThemeManager
import tn.esprit.wayfinder.manager.LanguageManager
import tn.esprit.wayfinder.presentation.auth.ViewModelFactory
import android.app.Activity
import android.content.Intent
import tn.esprit.wayfinder.ui.components.CustomBottomNavigationBar
import tn.esprit.wayfinder.ui.components.PointsDisplayCard
import tn.esprit.wayfinder.viewmodels.UserViewModel
import tn.esprit.wayfinder.viewmodels.UserUiState
import tn.esprit.wayfinder.viewmodels.JourneyViewModel
import tn.esprit.wayfinder.models.CanShareJourneyResponse
import tn.esprit.wayfinder.utils.StringTranslator


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(navController: NavController) {
    val context = LocalContext.current
    val userViewModel: UserViewModel = viewModel(factory = ViewModelFactory(context.applicationContext as Application))
    val journeyViewModel: JourneyViewModel = viewModel(factory = ViewModelFactory(context.applicationContext as Application))
    val uiState by userViewModel.uiState.collectAsState()
    val canShareState by journeyViewModel.canShareState.collectAsState()
    val tokenManager = remember { TokenManager(context) }
    val themeManager = remember { ThemeManager(context) }
    val languageManager = remember { LanguageManager(context) }
    val currentUser = remember { tokenManager.getUser() }
    val startDestination = remember { navController.graph.startDestinationRoute ?: "home" }
    
    // Dark mode state
    var isDarkModeEnabled by remember { mutableStateOf(themeManager.isDarkModeEnabled()) }
    var followSystemTheme by remember { mutableStateOf(themeManager.isFollowingSystem()) }
    
    // Language state
    var selectedLanguage by remember { mutableStateOf(languageManager.getLanguage()) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    
    // Settings menu state
    var isSettingsMenuExpanded by remember { mutableStateOf(false) }

    // Load profile and check if user can share journey
    LaunchedEffect(Unit) {
        userViewModel.loadProfile()
        // Wait a bit before checking to ensure user is authenticated
        kotlinx.coroutines.delay(500)
        journeyViewModel.checkCanShareJourney()
    }
    
    // Re-check if user can share journey after profile is loaded (in case bookings changed)
    LaunchedEffect(uiState) {
        if (uiState is UserUiState.Success) {
            // Wait a bit to ensure profile is fully loaded
            kotlinx.coroutines.delay(300)
            journeyViewModel.checkCanShareJourney()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        StringTranslator.translate(context, "My Profile"),
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
                    Box {
                        // Settings menu icon
                        IconButton(onClick = { 
                            isSettingsMenuExpanded = true
                        }) {
                            Icon(Icons.Filled.Settings, contentDescription = "Settings")
                        }
                        
                        // Settings dropdown menu
                        DropdownMenu(
                            expanded = isSettingsMenuExpanded,
                            onDismissRequest = { isSettingsMenuExpanded = false }
                        ) {
                            // Dark/Light mode toggle with switch
                            DropdownMenuItem(
                                text = { 
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (isDarkModeEnabled) Icons.Filled.DarkMode else Icons.Filled.LightMode,
                                                contentDescription = null,
                                                tint = Color(0xFF1976D2)
                                            )
                                            Text(
                                                text = if (isDarkModeEnabled) 
                                                    StringTranslator.translate(context, "Mode sombre") 
                                                else 
                                                    StringTranslator.translate(context, "Mode clair")
                                            )
                                        }
                                        Switch(
                                            checked = isDarkModeEnabled,
                                            onCheckedChange = {
                                                isDarkModeEnabled = it
                                                themeManager.setDarkModeEnabled(it)
                                                themeManager.setFollowSystemTheme(false)
                                                (context as? android.app.Activity)?.recreate()
                                            },
                                            colors = SwitchDefaults.colors(
                                                checkedThumbColor = Color.White,
                                                checkedTrackColor = Color(0xFF1976D2),
                                                uncheckedThumbColor = Color.White,
                                                uncheckedTrackColor = Color(0xFFCCCCCC)
                                            )
                                        )
                                    }
                                },
                                onClick = {
                                    // Allow clicking anywhere to toggle
                                    isDarkModeEnabled = !isDarkModeEnabled
                                    themeManager.setDarkModeEnabled(!isDarkModeEnabled)
                                    themeManager.setFollowSystemTheme(false)
                                    (context as? android.app.Activity)?.recreate()
                                }
                            )
                            
                            HorizontalDivider()
                            
                            // Logout
                            DropdownMenuItem(
                                text = { 
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Logout,
                                        contentDescription = null
                                    )
                                        Text(StringTranslator.translate(context, "Se déconnecter"))
                                    }
                                },
                                onClick = {
                                    isSettingsMenuExpanded = false
                                    tokenManager.deleteToken()
                                    navController.navigate("login") {
                                        popUpTo(startDestination) { inclusive = true }
                                        launchSingleTop = true
                                    }
                                }
                            )
                        }
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
        when (val state = uiState) {
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
            is UserUiState.Success -> {
                ProfileContent(
                    user = state.user,
                    currentUser = currentUser,
                    onBookingHistoryClick = {
                        navController.navigate("booking_history")
                    },
                    onShareJourneyClick = {
                        if (canShareState?.canShare == true) {
                            navController.navigate("share_journey")
                        }
                    },
                    canShareJourney = canShareState?.canShare ?: false,
                    canShareState = canShareState,
                    navController = navController,
                    context = context,
                    languageManager = languageManager,
                    selectedLanguage = selectedLanguage,
                    onLanguageSelected = { language ->
                        selectedLanguage = language
                        languageManager.setLanguage(language)
                        // Restart activity to apply language change
                        val activity = context as? Activity
                        activity?.let {
                            // Use recreate() for a cleaner restart
                            it.recreate()
                        }
                    },
                    showLanguageDialog = showLanguageDialog,
                    onShowLanguageDialogChange = { show -> showLanguageDialog = show },
                    modifier = Modifier.padding(paddingValues)
                )
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
                            text = state.message,
                            color = Color.Red
                        )
                        Button(onClick = { userViewModel.loadProfile() }) {
                            Text(StringTranslator.translate(context, "Réessayer"))
                        }
                    }
                }
            }
            else -> {}
        }
    }
}

@Composable
fun ProfileContent(
    user: tn.esprit.wayfinder.models.User,
    currentUser: tn.esprit.wayfinder.models.User?,
    onBookingHistoryClick: () -> Unit,
    onShareJourneyClick: () -> Unit,
    canShareJourney: Boolean,
    canShareState: CanShareJourneyResponse?,
    navController: NavController,
    context: android.content.Context,
    languageManager: LanguageManager,
    selectedLanguage: String,
    onLanguageSelected: (String) -> Unit,
    showLanguageDialog: Boolean,
    onShowLanguageDialogChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val tokenManager = remember { TokenManager(context) }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(24.dp))
        
        // Profile Header - Image on left, info on right
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
            // Profile Picture - Left side
                        AsyncImage(
                model = user.profileImageUrl?.let { url ->
                    if (url.startsWith("http")) url else "https://wayfinder-api-w92x.onrender.com$url"
                } ?: "https://i.pravatar.cc/150?img=${user.id.hashCode() % 70}",
                            contentDescription = "Profile Picture",
                            modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .clickable { navController.navigate("edit_profile") },
                            contentScale = ContentScale.Crop,
                            placeholder = painterResource(id = R.drawable.europe),
                            error = painterResource(id = R.drawable.europe)
                        )
            
            // Name, Username and Edit Button - Right side
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Name
                        Text(
                            text = "${user.firstName} ${user.lastName}",
                            style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                        )
                
                // Username
                        Text(
                            text = "@${user.username}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray
                        )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                // Edit Profile Button - Blue (reduced width)
                Button(
                    onClick = { navController.navigate("edit_profile") },
                    modifier = Modifier
                        .width(200.dp)
                        .height(48.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF1976D2)
                    )
                ) {
                    Text(
                        text = StringTranslator.translate(context, "Edit Profile"),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color.White
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Menu Items List
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            // Favoris
            ProfileMenuItem(
                icon = Icons.Filled.Favorite,
                text = StringTranslator.translate(context, "Favoris"),
                onClick = { navController.navigate("favorites") }
            )
            
            // Language
            ProfileMenuItem(
                icon = Icons.Filled.Language,
                text = StringTranslator.translate(context, "Language"),
                onClick = { onShowLanguageDialogChange(true) }
            )
            
            // Location
            ProfileMenuItem(
                icon = Icons.Filled.LocationOn,
                text = StringTranslator.translate(context, "Localisation"),
                onClick = { /* Empty action */ }
            )
            
            // Historique de réservations
            ProfileMenuItem(
                icon = Icons.Filled.Schedule,
                    text = StringTranslator.translate(context, "Historique des réservations"),
                onClick = { navController.navigate("booking_history") }
            )
            
            // View Shared Journeys
            ProfileMenuItem(
                icon = Icons.Filled.Share,
                        text = StringTranslator.translate(context, "Voir les voyages partagés"),
                onClick = { navController.navigate("journey_feed") }
            )
        }
        
        // Language Selection Dialog
        if (showLanguageDialog) {
            AlertDialog(
                onDismissRequest = { onShowLanguageDialogChange(false) },
                title = {
                    Text(text = context.getString(R.string.select_language))
                },
                text = {
                    Column {
                        LanguageOption(
                            label = context.getString(R.string.language_french),
                            isSelected = selectedLanguage == LanguageManager.LANGUAGE_FRENCH,
                            onClick = {
                                onLanguageSelected(LanguageManager.LANGUAGE_FRENCH)
                                onShowLanguageDialogChange(false)
                            }
                        )
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        LanguageOption(
                            label = context.getString(R.string.language_english),
                            isSelected = selectedLanguage == LanguageManager.LANGUAGE_ENGLISH,
                            onClick = {
                                onLanguageSelected(LanguageManager.LANGUAGE_ENGLISH)
                                onShowLanguageDialogChange(false)
                            }
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = { onShowLanguageDialogChange(false) }) {
                        Text(context.getString(R.string.cancel))
                    }
                }
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Points Display Card (Starbucks/Starbucks-style)
        PointsDisplayCard(
            totalPoints = user.totalPoints,
            lifetimePoints = user.lifetimePoints,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Travel Streak Card (Duolingo-inspired)
        TravelStreakCard(
            currentStreak = user.currentStreak,
            longestStreak = user.longestStreak,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Spacer(modifier = Modifier.height(80.dp)) // Space for bottom nav
    }
}

@Composable
fun ProfileMenuItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    onClick: () -> Unit
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
                    Icon(
                imageVector = icon,
                contentDescription = text,
                tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "Navigate",
            tint = Color.Gray,
                    modifier = Modifier
                .size(20.dp)
                        .graphicsLayer {
                            rotationZ = 180f
                        }
                )
    }
}

@Composable
fun ContactInfoItem(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

data class MemoryAlbum(val title: String, val photoCount: Int, val imageRes: Int)

@Composable
fun MemoryCard(album: MemoryAlbum) {
    Card(
        modifier = Modifier
            .width(150.dp)
            .height(150.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Grid of images
            Image(
                painter = painterResource(id = album.imageRes),
                contentDescription = album.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            
            // Overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.6f)),
                            startY = 100f
                        )
                    )
            )
            
            // Content
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(12.dp)
            ) {
                Text(
                    text = album.title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${album.photoCount} Photos",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.9f)
                )
            }
            
            // Plus icon in bottom right
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = "Add",
                    tint = Color.White,
                    modifier = Modifier
                        .size(24.dp)
                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                        .padding(4.dp)
                )
            }
        }
    }
}

@Composable
fun LanguageOption(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )
        if (isSelected) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = "Selected",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun TravelStreakCard(
    currentStreak: Int,
    longestStreak: Int,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "streak")
    val fireScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "fireScale"
    )
    val colorScheme = MaterialTheme.colorScheme
    val isDark = colorScheme.background.luminance() < 0.5f
    val cardGradient = if (isDark) {
        listOf(
            colorScheme.surfaceVariant.copy(alpha = 0.95f),
            colorScheme.surface.copy(alpha = 0.9f)
        )
    } else {
        listOf(
            Color(0xFFF5F5F5).copy(alpha = 0.9f),
            Color(0xFFE8E8E8).copy(alpha = 0.85f),
            Color(0xFFF5F5F5).copy(alpha = 0.9f)
        )
    }
    val overlayGradient = if (isDark) {
        listOf(
            colorScheme.surfaceVariant.copy(alpha = 0.85f),
            colorScheme.surface.copy(alpha = 0.8f)
        )
    } else {
        listOf(
            Color(0xFFFFFFFF).copy(alpha = 0.6f),
            Color(0xFFFFFFFF).copy(alpha = 0.5f)
        )
    }
    val borderBrush = Brush.linearGradient(
        colors = if (isDark) {
            listOf(
                colorScheme.outlineVariant.copy(alpha = 0.7f),
                colorScheme.outline.copy(alpha = 0.5f),
                colorScheme.outlineVariant.copy(alpha = 0.7f)
            )
        } else {
            listOf(
                Color(0xFFCCCCCC).copy(alpha = 0.5f),
                Color(0xFFDDDDDD).copy(alpha = 0.4f),
                Color(0xFFCCCCCC).copy(alpha = 0.5f)
            )
        }
    )
    val accentColor = if (isDark) colorScheme.primary else Color(0xFFFF5722)
    val textOnCard = colorScheme.onSurface
    val progressTrackColor = if (isDark) colorScheme.primary.copy(alpha = 0.25f) else Color(0xFFFF5722).copy(alpha = 0.2f)

    Box(
        modifier = modifier
            .graphicsLayer {
                compositingStrategy = androidx.compose.ui.graphics.CompositingStrategy.ModulateAlpha
            }
            .background(Brush.verticalGradient(cardGradient), shape = RoundedCornerShape(16.dp))
            .background(Brush.verticalGradient(overlayGradient), shape = RoundedCornerShape(16.dp))
            .border(width = 1.dp, brush = borderBrush, shape = RoundedCornerShape(16.dp))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.LocalFireDepartment,
                    contentDescription = "Streak",
                    tint = accentColor,
                    modifier = Modifier
                        .size(32.dp)
                        .graphicsLayer { scaleX = fireScale; scaleY = fireScale }
                )
                Text(
                    text = "$currentStreak",
                    style = MaterialTheme.typography.displaySmall.copy(
                        fontSize = if (isDark) 32.sp else MaterialTheme.typography.displaySmall.fontSize
                    ),
                    fontWeight = FontWeight.Bold,
                    color = accentColor
                )
            }
            
            Text(
                text = "Day Streak",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = if (isDark) 18.sp else MaterialTheme.typography.titleMedium.fontSize
                ),
                fontWeight = FontWeight.Bold,
                color = textOnCard
            )
            
            Text(
                text = "Longest streak: $longestStreak days",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = if (isDark) 14.sp else MaterialTheme.typography.bodySmall.fontSize
                ),
                color = textOnCard,
                fontWeight = FontWeight.Bold
            )
            
            // Progress bar
            LinearProgressIndicator(
                progress = { (currentStreak.toFloat() / 30f).coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = accentColor,
                trackColor = progressTrackColor
            )
            
            Text(
                text = "${30 - currentStreak} days until next milestone!",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = if (isDark) 14.sp else MaterialTheme.typography.bodySmall.fontSize
                ),
                color = accentColor,
                fontWeight = FontWeight.Bold
            )
        }
    }
}


@Preview(showBackground = true)
@Composable
fun ProfileScreenPreview() {
    WayFinderTheme {
        ProfileScreen(rememberNavController())
    }
}

