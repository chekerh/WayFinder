package tn.esprit.wayfinder.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import tn.esprit.wayfinder.models.Notification
import tn.esprit.wayfinder.presentation.notifications.NotificationsRepository

sealed class NotificationsUiState {
    object Idle : NotificationsUiState()
    object Loading : NotificationsUiState()
    data class Success(
        val notifications: List<Notification>,
        val unreadCount: Int = 0
    ) : NotificationsUiState()
    data class Error(val message: String) : NotificationsUiState()
}

class NotificationsViewModel(private val notificationsRepository: NotificationsRepository) : ViewModel() {

    private val _uiState = MutableStateFlow<NotificationsUiState>(NotificationsUiState.Idle)
    val uiState: StateFlow<NotificationsUiState> = _uiState.asStateFlow()

    fun loadNotifications(unreadOnly: Boolean = false) {
        viewModelScope.launch {
            _uiState.value = NotificationsUiState.Loading
            try {
                val notifications = notificationsRepository.getNotifications(unreadOnly)
                val unreadCount = notificationsRepository.getUnreadCount()
                _uiState.value = NotificationsUiState.Success(notifications, unreadCount)
            } catch (e: Exception) {
                _uiState.value = NotificationsUiState.Error(e.message ?: "Failed to load notifications")
            }
        }
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

    fun refreshUnreadCount() {
        viewModelScope.launch {
            try {
                val unreadCount = notificationsRepository.getUnreadCount()
                val currentState = _uiState.value
                if (currentState is NotificationsUiState.Success) {
                    _uiState.value = currentState.copy(unreadCount = unreadCount)
                }
            } catch (e: Exception) {
                // Silently fail for unread count refresh
            }
        }
    }
}

