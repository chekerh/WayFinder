package tn.esprit.wayfinder.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import tn.esprit.wayfinder.models.Notification
import tn.esprit.wayfinder.presentation.notifications.NotificationsRepository
import tn.esprit.wayfinder.utils.NotificationHelper

sealed class NotificationsUiState {
    object Idle : NotificationsUiState()
    object Loading : NotificationsUiState()
    data class Success(
        val notifications: List<Notification>,
        val unreadCount: Int = 0
    ) : NotificationsUiState()
    data class Error(val message: String) : NotificationsUiState()
}

class NotificationsViewModel(
    private val notificationsRepository: NotificationsRepository,
    private val context: Context? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow<NotificationsUiState>(NotificationsUiState.Idle)
    val uiState: StateFlow<NotificationsUiState> = _uiState.asStateFlow()
    
    private var lastNotificationIds = emptySet<String>()

    init {
        // Initialize with empty list and 0 count
        _uiState.value = NotificationsUiState.Success(emptyList(), 0)
        
        // Create notification channel when ViewModel is created
        context?.let { NotificationHelper.createNotificationChannel(it) }
    }

    fun loadNotifications(unreadOnly: Boolean = false, showSystemNotifications: Boolean = true) {
        viewModelScope.launch {
            try {
                val notifications = notificationsRepository.getNotifications(unreadOnly)
                val unreadCount = notificationsRepository.getUnreadCount()
                
                android.util.Log.d("NotificationsViewModel", "Loaded ${notifications.size} notifications, ${unreadCount} unread")
                
                // Show system notifications for new unread notifications
                if (showSystemNotifications && context != null) {
                    showNewNotifications(notifications.filter { !it.isRead })
                }
                
                // Only update UI state if it's not already loading (to avoid flickering)
                if (_uiState.value !is NotificationsUiState.Loading) {
                    _uiState.value = NotificationsUiState.Success(notifications, unreadCount)
                } else {
                    _uiState.value = NotificationsUiState.Success(notifications, unreadCount)
                }
            } catch (e: Exception) {
                // Handle 401 Unauthorized gracefully - user may not be logged in yet
                if (e.message?.contains("401") == true || e.message?.contains("Unauthorized") == true) {
                    android.util.Log.d("NotificationsViewModel", "User not authenticated, returning empty notifications list")
                    _uiState.value = NotificationsUiState.Success(emptyList(), 0)
                } else {
                    android.util.Log.e("NotificationsViewModel", "Error loading notifications: ${e.message}", e)
                    _uiState.value = NotificationsUiState.Error(e.message ?: "Failed to load notifications")
                }
            }
        }
    }
    
    private fun showNewNotifications(notifications: List<Notification>) {
        val currentNotificationIds = notifications.map { it.id }.toSet()
        val newNotifications = notifications.filter { it.id !in lastNotificationIds }
        
        android.util.Log.d("NotificationsViewModel", "Found ${newNotifications.size} new notifications out of ${notifications.size} total")
        
        // Show system notifications for new unread notifications
        context?.let { ctx ->
            newNotifications.forEach { notification ->
                android.util.Log.d("NotificationsViewModel", "Showing notification: ${notification.title} - ${notification.message}")
                NotificationHelper.showNotification(ctx, notification)
            }
        }
        
        lastNotificationIds = currentNotificationIds
    }

    fun markAsRead(id: String, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                notificationsRepository.markAsRead(id)
                loadNotifications() // Refresh list
                onSuccess()
            } catch (e: Exception) {
                _uiState.value = NotificationsUiState.Error(e.message ?: "Failed to mark as read")
            }
        }
    }

    fun markAllAsRead(onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                notificationsRepository.markAllAsRead()
                loadNotifications() // Refresh list
                onSuccess()
            } catch (e: Exception) {
                _uiState.value = NotificationsUiState.Error(e.message ?: "Failed to mark all as read")
            }
        }
    }

    fun deleteNotification(id: String, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                notificationsRepository.deleteNotification(id)
                loadNotifications() // Refresh list
                onSuccess()
            } catch (e: Exception) {
                _uiState.value = NotificationsUiState.Error(e.message ?: "Failed to delete notification")
            }
        }
    }

    fun deleteAllNotifications(onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                notificationsRepository.deleteAllNotifications()
                loadNotifications() // Refresh list
                onSuccess()
            } catch (e: Exception) {
                _uiState.value = NotificationsUiState.Error(e.message ?: "Failed to delete all notifications")
            }
        }
    }

    fun refreshUnreadCount() {
        viewModelScope.launch {
            try {
                val unreadCount = notificationsRepository.getUnreadCount()
                val currentState = _uiState.value
                when (currentState) {
                    is NotificationsUiState.Success -> {
                        _uiState.value = currentState.copy(unreadCount = unreadCount)
                    }
                    else -> {
                        // If state is not Success, create a new Success state with the count
                        _uiState.value = NotificationsUiState.Success(emptyList(), unreadCount)
                    }
                }
            } catch (e: Exception) {
                // Handle 401 Unauthorized gracefully - user may not be logged in
                if (e.message?.contains("401") == true || e.message?.contains("Unauthorized") == true) {
                    android.util.Log.d("NotificationsViewModel", "User not authenticated, setting unread count to 0")
                    val currentState = _uiState.value
                    if (currentState !is NotificationsUiState.Success) {
                        _uiState.value = NotificationsUiState.Success(emptyList(), 0)
                    } else {
                        _uiState.value = currentState.copy(unreadCount = 0)
                    }
                } else {
                    // On other errors, ensure we have a Success state (even with 0 count) so UI doesn't break
                    android.util.Log.e("NotificationsViewModel", "Error refreshing unread count: ${e.message}", e)
                    val currentState = _uiState.value
                    if (currentState !is NotificationsUiState.Success) {
                        _uiState.value = NotificationsUiState.Success(emptyList(), 0)
                    }
                }
            }
        }
    }
}

