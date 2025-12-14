package tn.esprit.wayfinder.presentation.notifications

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import tn.esprit.wayfinder.manager.CacheManager
import tn.esprit.wayfinder.models.*
import tn.esprit.wayfinder.network.ApiService

class NotificationsRepository(
    private val apiService: ApiService,
    private val context: Context? = null
) {
    
    private val cacheManager = context?.let { CacheManager(it) }
    private val TAG = "NotificationsRepository"

    suspend fun getNotifications(unreadOnly: Boolean = false): List<Notification> = withContext(Dispatchers.IO) {
        val cacheKey = if (unreadOnly) {
            CacheManager.KEY_NOTIFICATIONS_UNREAD
        } else {
            CacheManager.KEY_NOTIFICATIONS
        }
        
        // Try to get from cache first
        cacheManager?.get<List<Notification>>(cacheKey)?.let { cached ->
            Log.d(TAG, "Returning cached notifications (unreadOnly=$unreadOnly)")
            // Refresh in background
            refreshNotificationsInBackground(unreadOnly, cacheKey)
            return@withContext cached
        }
        
        // Cache miss - fetch from API
        try {
            val notifications = apiService.getNotifications(unreadOnly)
            cacheManager?.put(cacheKey, notifications, CacheManager.TTL_SHORT)
            Log.d(TAG, "Fetched and cached ${notifications.size} notifications from API")
            notifications
        } catch (e: Exception) {
            // Handle 401 Unauthorized - token expired, invalidate cache
            if (e is HttpException && e.code() == 401) {
                Log.w(TAG, "401 Unauthorized - clearing notification cache due to expired token")
                cacheManager?.remove(cacheKey)
                cacheManager?.remove(CacheManager.KEY_UNREAD_COUNT)
            } else {
                Log.e(TAG, "Error fetching notifications from API", e)
            }
            // Return empty list on error (don't throw to prevent UI crashes)
            emptyList()
        }
    }

    suspend fun getUnreadCount(): Int = withContext(Dispatchers.IO) {
        // Try cache first
        cacheManager?.get<Int>(CacheManager.KEY_UNREAD_COUNT)?.let { cached ->
            Log.d(TAG, "Returning cached unread count: $cached")
            // Refresh in background
            refreshUnreadCountInBackground()
            return@withContext cached
        }
        
        // Cache miss - fetch from API
        try {
            val count = apiService.getUnreadCount().count
            cacheManager?.put(CacheManager.KEY_UNREAD_COUNT, count, CacheManager.TTL_SHORT)
            Log.d(TAG, "Fetched and cached unread count from API: $count")
            count
        } catch (e: Exception) {
            // Handle 401 Unauthorized - token expired, invalidate cache
            if (e is HttpException && e.code() == 401) {
                Log.w(TAG, "401 Unauthorized - clearing unread count cache due to expired token")
                cacheManager?.remove(CacheManager.KEY_UNREAD_COUNT)
            } else {
                Log.e(TAG, "Error fetching unread count from API", e)
            }
            0 // Return 0 on error
        }
    }
    
    private suspend fun refreshNotificationsInBackground(unreadOnly: Boolean, cacheKey: String) {
        try {
            val notifications = apiService.getNotifications(unreadOnly)
            cacheManager?.put(cacheKey, notifications, CacheManager.TTL_SHORT)
            Log.d(TAG, "Background refresh: updated ${notifications.size} notifications")
        } catch (e: Exception) {
            // Handle 401 silently - token will be cleared by AuthInterceptor
            if (e is HttpException && e.code() == 401) {
                Log.w(TAG, "Background refresh failed: 401 Unauthorized (token expired)")
                cacheManager?.remove(cacheKey)
            } else {
                Log.w(TAG, "Background refresh failed for notifications", e)
            }
        }
    }
    
    private suspend fun refreshUnreadCountInBackground() {
        try {
            val count = apiService.getUnreadCount().count
            cacheManager?.put(CacheManager.KEY_UNREAD_COUNT, count, CacheManager.TTL_SHORT)
            Log.d(TAG, "Background refresh: updated unread count to $count")
        } catch (e: Exception) {
            // Handle 401 silently - token will be cleared by AuthInterceptor
            if (e is HttpException && e.code() == 401) {
                Log.w(TAG, "Background refresh failed: 401 Unauthorized (token expired)")
                cacheManager?.remove(CacheManager.KEY_UNREAD_COUNT)
            } else {
                Log.w(TAG, "Background refresh failed for unread count", e)
            }
        }
    }

    suspend fun createNotification(request: CreateNotificationRequest): Notification {
        return apiService.createNotification(request)
    }

    suspend fun markAsRead(id: String): Notification {
        val result = apiService.markAsRead(id)
        // Invalidate cache
        cacheManager?.remove(CacheManager.KEY_NOTIFICATIONS)
        cacheManager?.remove(CacheManager.KEY_NOTIFICATIONS_UNREAD)
        cacheManager?.remove(CacheManager.KEY_UNREAD_COUNT)
        return result
    }

    suspend fun markAllAsRead(): Map<String, String> {
        val result = apiService.markAllAsRead()
        // Invalidate cache
        cacheManager?.remove(CacheManager.KEY_NOTIFICATIONS)
        cacheManager?.remove(CacheManager.KEY_NOTIFICATIONS_UNREAD)
        cacheManager?.remove(CacheManager.KEY_UNREAD_COUNT)
        return result
    }

    suspend fun deleteNotification(id: String): Map<String, String> {
        val result = apiService.deleteNotification(id)
        // Invalidate cache
        cacheManager?.remove(CacheManager.KEY_NOTIFICATIONS)
        cacheManager?.remove(CacheManager.KEY_NOTIFICATIONS_UNREAD)
        cacheManager?.remove(CacheManager.KEY_UNREAD_COUNT)
        return result
    }

    suspend fun deleteAllNotifications(): Map<String, String> {
        val result = apiService.deleteAllNotifications()
        // Invalidate cache
        cacheManager?.remove(CacheManager.KEY_NOTIFICATIONS)
        cacheManager?.remove(CacheManager.KEY_NOTIFICATIONS_UNREAD)
        cacheManager?.remove(CacheManager.KEY_UNREAD_COUNT)
        return result
    }
}

