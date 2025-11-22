package tn.esprit.wayfinder.services

import android.app.job.JobParameters
import android.app.job.JobService
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import tn.esprit.wayfinder.WayfinderApp
import tn.esprit.wayfinder.data.NotificationPreferences
import tn.esprit.wayfinder.presentation.notifications.NotificationsRepository
import tn.esprit.wayfinder.utils.NotificationHelper

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
class NotificationCheckService : JobService() {
    
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var isJobRunning = false
    
    override fun onStartJob(params: JobParameters?): Boolean {
        if (isJobRunning) {
            return false
        }
        
        isJobRunning = true
        serviceScope.launch {
            try {
                checkForNewNotifications()
                jobFinished(params, false)
            } catch (e: Exception) {
                e.printStackTrace()
                jobFinished(params, true) // Reschedule on error
            } finally {
                isJobRunning = false
            }
        }
        
        return true // Job is running asynchronously
    }
    
    override fun onStopJob(params: JobParameters?): Boolean {
        isJobRunning = false
        return false // Don't reschedule if job is stopped
    }
    
    private suspend fun checkForNewNotifications() {
        val context = applicationContext
        val prefs = NotificationPreferences(context)
        
        // Skip if notifications are disabled
        if (!prefs.isNotificationCheckEnabled()) {
            return
        }
        
        // Skip if permission not granted
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) {
            return
        }
        
        try {
            val app = application as? WayfinderApp
            val apiService = app?.apiService ?: return
            
            val repository = NotificationsRepository(apiService)
            val lastCheckTime = prefs.getLastCheckTime()
            val notifications = repository.getNotifications(unreadOnly = true)
            
            // Filter notifications newer than last check
            val newNotifications = notifications.filter { notification ->
                try {
                    // Parse createdAt string to timestamp
                    val notificationTime = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US).apply {
                        timeZone = java.util.TimeZone.getTimeZone("UTC")
                    }.parse(notification.createdAt)?.time ?: 0L
                    
                    notificationTime > lastCheckTime
                } catch (e: Exception) {
                    // If parsing fails, show all unread notifications
                    true
                }
            }
            
            // Show system notifications for new unread notifications
            newNotifications.forEach { notification ->
                NotificationHelper.showNotification(context, notification)
            }
            
            // Update last check time
            if (notifications.isNotEmpty()) {
                prefs.setLastCheckTime(System.currentTimeMillis())
            }
            
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

