package tn.esprit.wayfinder.presentation.notifications

import tn.esprit.wayfinder.models.*
import tn.esprit.wayfinder.network.ApiService

class NotificationsRepository(private val apiService: ApiService) {

    suspend fun getNotifications(unreadOnly: Boolean = false): List<Notification> {
        return apiService.getNotifications(unreadOnly)
    }

    suspend fun getUnreadCount(): Int {
        return apiService.getUnreadCount().count
    }

    suspend fun createNotification(request: CreateNotificationRequest): Notification {
        return apiService.createNotification(request)
    }

    suspend fun markAsRead(id: String): Notification {
        return apiService.markAsRead(id)
    }

    suspend fun markAllAsRead(): Map<String, String> {
        return apiService.markAllAsRead()
    }

    suspend fun deleteNotification(id: String): Map<String, String> {
        return apiService.deleteNotification(id)
    }
}

