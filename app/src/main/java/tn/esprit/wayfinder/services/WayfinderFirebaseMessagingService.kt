package tn.esprit.wayfinder.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import tn.esprit.wayfinder.MainActivity
import tn.esprit.wayfinder.WayfinderApp
import tn.esprit.wayfinder.manager.TokenManager
import tn.esprit.wayfinder.network.ApiService
import tn.esprit.wayfinder.presentation.notifications.NotificationsRepository

class WayfinderFirebaseMessagingService : FirebaseMessagingService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val CHANNEL_ID = "wayfinder_notifications"

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        
        // Send token to backend when it's refreshed
        serviceScope.launch {
            try {
                registerFcmToken(token)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        // Handle data payload
        remoteMessage.data.let { data ->
            val title = data["title"] ?: "WayFinder"
            val message = data["message"] ?: ""
            val type = data["type"] ?: "general"
            val notificationId = data["notificationId"] ?: System.currentTimeMillis().toString()
            val actionUrl = data["actionUrl"]

            // Show notification
            showNotification(
                title = title,
                message = message,
                type = type,
                notificationId = notificationId.hashCode(),
                actionUrl = actionUrl
            )
        }

        // Handle notification payload (when app is in foreground, notification payload is handled here)
        remoteMessage.notification?.let { notification ->
            showNotification(
                title = notification.title ?: "WayFinder",
                message = notification.body ?: "",
                type = "general",
                notificationId = System.currentTimeMillis().toInt(),
                actionUrl = null
            )
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "WayFinder Notifications",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications pour les réservations, paiements et alertes"
                enableVibration(true)
                enableLights(true)
                setShowBadge(true)
                vibrationPattern = longArrayOf(0, 250, 250, 250)
            }

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun showNotification(
        title: String,
        message: String,
        type: String,
        notificationId: Int,
        actionUrl: String?
    ) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("notification_type", type)
            actionUrl?.let { putExtra("action_url", it) }
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Determine icon based on type
        val icon = when (type) {
            "booking_confirmed" -> android.R.drawable.ic_dialog_info
            "booking_cancelled" -> android.R.drawable.ic_dialog_alert
            "booking_updated" -> android.R.drawable.ic_dialog_info
            "payment_success" -> android.R.drawable.ic_dialog_info
            "payment_failed" -> android.R.drawable.ic_dialog_alert
            "price_alert" -> android.R.drawable.ic_dialog_alert
            "trip_reminder" -> android.R.drawable.ic_dialog_info
            else -> android.R.drawable.ic_dialog_info
        }

        val notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(icon)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))

        // Add action button if actionUrl is available
        actionUrl?.let { url ->
            val actionIntent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                putExtra("action_url", url)
            }
            val actionPendingIntent = PendingIntent.getActivity(
                this,
                notificationId + 1000,
                actionIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            notificationBuilder.addAction(
                android.R.drawable.ic_menu_view,
                "Ouvrir",
                actionPendingIntent
            )
        }

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(notificationId, notificationBuilder.build())
    }

    private suspend fun registerFcmToken(token: String) {
        try {
            val app = applicationContext as? WayfinderApp
            val apiService = app?.apiService ?: return
            
            val tokenManager = TokenManager(applicationContext)
            val authToken = tokenManager.getToken()
            
            if (authToken != null) {
                // Send token to backend API
                try {
                    val request = tn.esprit.wayfinder.models.FcmTokenRequest(token = token)
                    val response = apiService.registerFcmToken(request)
                    android.util.Log.d("FCM", "FCM token registered: ${response.message}")
                } catch (e: Exception) {
                    android.util.Log.e("FCM", "Error sending FCM token to backend", e)
                }
            } else {
                android.util.Log.d("FCM", "User not logged in, FCM token will be registered on next login")
            }
        } catch (e: Exception) {
            android.util.Log.e("FCM", "Error registering FCM token", e)
        }
    }
}

