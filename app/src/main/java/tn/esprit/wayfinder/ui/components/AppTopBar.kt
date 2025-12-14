package tn.esprit.wayfinder.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Notifications
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import tn.esprit.wayfinder.R
import tn.esprit.wayfinder.manager.TokenManager
import tn.esprit.wayfinder.utils.HapticFeedbackHelper
import tn.esprit.wayfinder.utils.StringTranslator
import tn.esprit.wayfinder.viewmodels.NotificationsViewModel
import tn.esprit.wayfinder.viewmodels.NotificationsUiState

/**
 * Reusable top app bar component with user profile, onboarding reminder, and action buttons.
 * Follows Material Design 3 guidelines and best practices.
 */
@Composable
fun AppTopBar(
    navController: NavController,
    notificationsViewModel: NotificationsViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val tokenManager = remember { TokenManager(context) }
    val user by remember { mutableStateOf(tokenManager.getUser()) }
    val notificationsState by notificationsViewModel.uiState.collectAsState()
    
    val userName = remember(user) {
        user?.firstName?.takeIf { it.isNotBlank() }
            ?: user?.username?.takeIf { it.isNotBlank() }
            ?: "Utilisateur"
    }
    
    val unreadCount = remember(notificationsState) {
        when (val state = notificationsState) {
            is NotificationsUiState.Success -> state.unreadCount
            else -> 0
        }
    }
    
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // User profile section
            UserProfileSection(
                user = user,
                userName = userName,
                onProfileClick = {
                    HapticFeedbackHelper.triggerButtonPress(context)
                    navController.navigate("profile")
                },
                onOnboardingClick = {
                    navController.navigate("onboarding")
                }
            )
            
            // Action buttons section
            ActionButtonsSection(
                unreadCount = unreadCount,
                onFavoritesClick = {
                    HapticFeedbackHelper.triggerButtonPress(context)
                    navController.navigate("favorites")
                },
                onNotificationsClick = {
                    HapticFeedbackHelper.triggerButtonPress(context)
                    navController.navigate("notifications")
                }
            )
        }
    }
}

/**
 * User profile section with avatar, name, and onboarding reminder.
 */
@Composable
private fun UserProfileSection(
    user: tn.esprit.wayfinder.models.User?,
    userName: String,
    onProfileClick: () -> Unit,
    onOnboardingClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        UserAvatar(
            user = user,
            onProfileClick = onProfileClick
        )
        
        Spacer(modifier = Modifier.width(12.dp))
        
        UserInfoColumn(
            userName = userName,
            showOnboardingReminder = user?.onboardingSkipped == true,
            onOnboardingClick = onOnboardingClick
        )
    }
}

/**
 * User avatar component with fallback image.
 */
@Composable
private fun UserAvatar(
    user: tn.esprit.wayfinder.models.User?,
    onProfileClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val userImageUrl = remember(user?.profileImageUrl) {
        user?.profileImageUrl?.let { url ->
            if (url.startsWith("http")) url 
            else "https://wayfinder-api-w92x.onrender.com$url"
        }
    }
    
    Box(
        modifier = modifier
            .size(40.dp)
            .clip(CircleShape)
            .clickable(onClick = onProfileClick)
    ) {
        when {
            userImageUrl != null -> {
                AsyncImage(
                    model = userImageUrl,
                    contentDescription = "User Avatar",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    placeholder = painterResource(id = R.drawable.europe),
                    error = painterResource(id = R.drawable.europe)
                )
            }
            else -> {
                Image(
                    painter = painterResource(id = R.drawable.europe),
                    contentDescription = "User Avatar",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        }
    }
}

/**
 * User information column with name and optional onboarding reminder.
 */
@Composable
private fun UserInfoColumn(
    userName: String,
    showOnboardingReminder: Boolean,
    onOnboardingClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = userName,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        if (showOnboardingReminder) {
            OnboardingReminderText(
                onOnboardingClick = onOnboardingClick
            )
        }
    }
}

/**
 * Clickable onboarding reminder text.
 */
@Composable
private fun OnboardingReminderText(
    onOnboardingClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    Text(
        text = StringTranslator.translate(context, "Complétez votre profil"),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier
            .padding(top = 2.dp)
            .clickable(onClick = onOnboardingClick)
    )
}

/**
 * Action buttons section with favorites and notifications.
 */
@Composable
private fun ActionButtonsSection(
    unreadCount: Int,
    onFavoritesClick: () -> Unit,
    onNotificationsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ActionButton(
            icon = Icons.Filled.Favorite,
            contentDescription = "Favoris",
            iconTint = Color(0xFFFF1744),
            onClick = onFavoritesClick
        )
        
        NotificationButton(
            unreadCount = unreadCount,
            onClick = onNotificationsClick
        )
    }
}

/**
 * Reusable action button component.
 */
@Composable
private fun ActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    iconTint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(
                MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = iconTint,
            modifier = Modifier.size(24.dp)
        )
    }
}

/**
 * Notification button with badge indicator.
 */
@Composable
private fun NotificationButton(
    unreadCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(
                MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Filled.Notifications,
            contentDescription = "Notifications",
            tint = Color(0xFF0D47A1),
            modifier = Modifier.size(24.dp)
        )
        
        if (unreadCount > 0) {
            NotificationBadge(
                count = unreadCount,
                modifier = Modifier.align(Alignment.TopEnd)
            )
        }
    }
}

/**
 * Notification badge indicator.
 */
@Composable
private fun NotificationBadge(
    count: Int,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .offset(x = 8.dp, y = (-8).dp)
            .size(18.dp)
            .clip(CircleShape)
            .background(Color(0xFFF44336)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (count > 9) "9+" else count.toString(),
            color = Color.White,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

