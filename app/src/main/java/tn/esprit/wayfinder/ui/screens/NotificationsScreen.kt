package tn.esprit.wayfinder.ui.screens

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import tn.esprit.wayfinder.models.Notification
import tn.esprit.wayfinder.presentation.auth.ViewModelFactory
import tn.esprit.wayfinder.viewmodels.NotificationsUiState
import tn.esprit.wayfinder.viewmodels.NotificationsViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(navController: NavController) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val notificationsViewModel: NotificationsViewModel = viewModel(factory = ViewModelFactory(context.applicationContext as Application))
    val uiState by notificationsViewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        notificationsViewModel.loadNotifications()
    }

    Scaffold(
        containerColor = Color(0xFFF0F8FF),
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Notifications",
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
                actions = {
                    if (uiState is NotificationsUiState.Success && (uiState as NotificationsUiState.Success).notifications.any { !it.isRead }) {
                        TextButton(
                            onClick = {
                                notificationsViewModel.markAllAsRead()
                            }
                        ) {
                            Text("Tout marquer comme lu", fontSize = 12.sp)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFEAF2FF)
                )
            )
        }
    ) { paddingValues ->
        when (val state = uiState) {
            is NotificationsUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            is NotificationsUiState.Success -> {
                if (state.notifications.isEmpty()) {
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
                            Icon(
                                imageVector = Icons.Filled.Notifications,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = Color.Gray
                            )
                            Text(
                                "Aucune notification",
                                style = MaterialTheme.typography.titleLarge,
                                color = Color.Gray
                            )
                            Text(
                                "Vous n'avez pas de notifications pour le moment",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Gray
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(state.notifications, key = { it.id }) { notification ->
                            NotificationCard(
                                notification = notification,
                                onRead = {
                                    if (!notification.isRead) {
                                        notificationsViewModel.markAsRead(notification.id)
                                    }
                                },
                                onDelete = {
                                    notificationsViewModel.deleteNotification(notification.id)
                                },
                                onClick = {
                                    if (!notification.isRead) {
                                        notificationsViewModel.markAsRead(notification.id)
                                    }
                                    // Navigate based on actionUrl if available
                                    notification.actionUrl?.let { url ->
                                        when {
                                            url.startsWith("/booking_detail/") -> {
                                                val bookingId = url.removePrefix("/booking_detail/")
                                                navController.navigate("booking_detail/$bookingId")
                                            }
                                            url.startsWith("/flight_detail/") -> {
                                                val destinationId = url.removePrefix("/flight_detail/")
                                                navController.navigate("flight_detail/$destinationId")
                                            }
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
            is NotificationsUiState.Error -> {
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
                            state.message,
                            color = Color.Red,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Button(onClick = { notificationsViewModel.loadNotifications() }) {
                            Text("Réessayer")
                        }
                    }
                }
            }
            else -> {}
        }
    }
}

@Composable
fun NotificationCard(
    notification: Notification,
    onRead: () -> Unit,
    onDelete: () -> Unit,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (notification.isRead) Color.White else Color(0xFFE3F2FD)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Notification Icon
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        when (notification.type) {
                            "booking_confirmed", "payment_success" -> Color(0xFF4CAF50)
                            "booking_cancelled", "payment_failed" -> Color(0xFFF44336)
                            "price_alert" -> Color(0xFFFF9800)
                            else -> Color(0xFF1976D2)
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (notification.type) {
                        "booking_confirmed", "payment_success" -> Icons.Filled.CheckCircle
                        "price_alert" -> Icons.Filled.NotificationsActive
                        else -> Icons.Filled.Notifications
                    },
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            // Content
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = notification.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (!notification.isRead) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF1976D2))
                        )
                    }
                }
                Text(
                    text = notification.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
                Text(
                    text = formatNotificationTime(notification.createdAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }

            // Delete button
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = "Supprimer",
                    tint = Color.Gray,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

private fun formatNotificationTime(timestamp: String): String {
    return try {
        val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
        parser.timeZone = TimeZone.getTimeZone("UTC")
        val date = parser.parse(timestamp)
        val now = Date()
        val diff = now.time - (date?.time ?: 0)
        
        when {
            diff < 60000 -> "À l'instant"
            diff < 3600000 -> "${diff / 60000} min"
            diff < 86400000 -> "${diff / 3600000} h"
            diff < 604800000 -> "${diff / 86400000} j"
            else -> {
                val formatter = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                formatter.format(date ?: Date())
            }
        }
    } catch (e: Exception) {
        timestamp
    }
}

