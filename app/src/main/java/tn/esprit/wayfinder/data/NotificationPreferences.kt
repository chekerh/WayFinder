package tn.esprit.wayfinder.data

import android.content.Context
import android.content.SharedPreferences

class NotificationPreferences(context: Context) {
    private val prefs: SharedPreferences = 
        context.getSharedPreferences("notification_prefs", Context.MODE_PRIVATE)
    
    private val KEY_LAST_CHECK_TIME = "last_check_time"
    private val KEY_NOTIFICATION_CHECK_ENABLED = "notification_check_enabled"
    
    fun getLastCheckTime(): Long {
        return prefs.getLong(KEY_LAST_CHECK_TIME, 0L)
    }
    
    fun setLastCheckTime(time: Long) {
        prefs.edit().putLong(KEY_LAST_CHECK_TIME, time).apply()
    }
    
    fun isNotificationCheckEnabled(): Boolean {
        return prefs.getBoolean(KEY_NOTIFICATION_CHECK_ENABLED, true)
    }
    
    fun setNotificationCheckEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_NOTIFICATION_CHECK_ENABLED, enabled).apply()
    }
}

