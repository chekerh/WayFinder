package tn.esprit.wayfinder.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import tn.esprit.wayfinder.MainActivity
import tn.esprit.wayfinder.R
import tn.esprit.wayfinder.models.Notification as NotificationModel
import tn.esprit.wayfinder.utils.StringTranslator

object NotificationHelper {
    private const val CHANNEL_ID = "wayfinder_notifications"
    private const val CHANNEL_NAME = "WayFinder Notifications"
    private const val CHANNEL_DESCRIPTION = "Notifications pour les réservations, paiements et alertes"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = CHANNEL_DESCRIPTION
                enableVibration(true)
                enableLights(true)
                setShowBadge(true)
                vibrationPattern = longArrayOf(0, 250, 250, 250)
                setSound(
                    RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION),
                    null
                )
            }

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showNotification(
        context: Context,
        notification: NotificationModel,
        notificationId: Int = notification.id.hashCode()
    ) {
        // Create notification channel for Android 8+
        createNotificationChannel(context)

        // Create intent to open app when notification is clicked
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            // Add notification data to intent
            putExtra("notification_id", notification.id)
            putExtra("notification_type", notification.type)
            putExtra("action_url", notification.actionUrl)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Determine icon based on notification type
        val icon = when (notification.type) {
            "booking_confirmed" -> android.R.drawable.ic_dialog_info
            "booking_cancelled" -> android.R.drawable.ic_dialog_alert
            "booking_updated" -> android.R.drawable.ic_dialog_info
            "payment_success" -> android.R.drawable.ic_dialog_info
            "payment_failed" -> android.R.drawable.ic_dialog_alert
            "price_alert" -> android.R.drawable.ic_dialog_alert
            "trip_reminder" -> android.R.drawable.ic_dialog_info
            else -> android.R.drawable.ic_dialog_info
        }

        // Translate notification title and message
        val translatedTitle = StringTranslator.translate(context, notification.title)
        val translatedMessage = StringTranslator.translate(context, notification.message)
        
        // Build notification
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(icon)
            .setContentTitle(translatedTitle)
            .setContentText(translatedMessage)
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText(translatedMessage))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

        // Add action button if actionUrl is available
        notification.actionUrl?.let { actionUrl ->
            val actionIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                putExtra("action_url", actionUrl)
            }
            val actionPendingIntent = PendingIntent.getActivity(
                context,
                notificationId + 1000,
                actionIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(
                android.R.drawable.ic_menu_view,
                "Ouvrir",
                actionPendingIntent
            )
        }

        // Show notification
        try {
            with(NotificationManagerCompat.from(context)) {
                if (areNotificationsEnabled()) {
                    notify(notificationId, builder.build())
                }
            }
        } catch (e: SecurityException) {
            // Notification permission not granted
            e.printStackTrace()
        }
    }

    fun cancelNotification(context: Context, notificationId: Int) {
        val notificationManager = NotificationManagerCompat.from(context)
        notificationManager.cancel(notificationId)
    }

    fun cancelAllNotifications(context: Context) {
        val notificationManager = NotificationManagerCompat.from(context)
        notificationManager.cancelAll()
    }

    fun areNotificationsEnabled(context: Context): Boolean {
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    /**
     * Show a simple notification with title and message
     * Useful for immediate notifications without waiting for API response
     */
    fun showSimpleNotification(
        context: Context,
        title: String,
        message: String,
        type: String = "info",
        notificationId: Int = System.currentTimeMillis().toInt()
    ) {
        android.util.Log.d("NotificationHelper", "showSimpleNotification called: title=$title, message=$message")
        
        // Create notification channel first
        createNotificationChannel(context)

        // Check if notifications are enabled
        val notificationManager = NotificationManagerCompat.from(context)
        if (!notificationManager.areNotificationsEnabled()) {
            android.util.Log.w("NotificationHelper", "Notifications are not enabled. Please grant notification permission in app settings.")
            return
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val icon = when (type) {
            "booking_confirmed" -> android.R.drawable.ic_dialog_info
            "booking_cancelled" -> android.R.drawable.ic_dialog_alert
            "payment_success" -> android.R.drawable.ic_dialog_info
            "payment_failed" -> android.R.drawable.ic_dialog_alert
            else -> android.R.drawable.ic_dialog_info
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
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

        try {
            android.util.Log.d("NotificationHelper", "Attempting to show notification with ID: $notificationId")
            notificationManager.notify(notificationId, builder.build())
            android.util.Log.d("NotificationHelper", "Notification shown successfully!")
        } catch (e: SecurityException) {
            android.util.Log.e("NotificationHelper", "SecurityException: Notification permission not granted", e)
            e.printStackTrace()
        } catch (e: Exception) {
            android.util.Log.e("NotificationHelper", "Unexpected error showing notification", e)
            e.printStackTrace()
        }
    }
}

