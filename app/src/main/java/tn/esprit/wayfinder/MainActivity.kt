package tn.esprit.wayfinder

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import tn.esprit.wayfinder.manager.LanguageManager
import tn.esprit.wayfinder.manager.ThemeManager
import tn.esprit.wayfinder.navigation.AppNavigation
import tn.esprit.wayfinder.ui.theme.WayFinderTheme
import tn.esprit.wayfinder.utils.LocaleHelper
import tn.esprit.wayfinder.utils.NotificationHelper

class MainActivity : ComponentActivity() {
    
    // Shared state for notification navigation
    companion object {
        private val _notificationActionUrl = MutableStateFlow<String?>(null)
        val notificationActionUrl: StateFlow<String?> = _notificationActionUrl.asStateFlow()
        
        fun setNotificationActionUrl(url: String?) {
            _notificationActionUrl.value = url
        }
    }
    
    // Request notification permission for Android 13+
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        android.util.Log.d("MainActivity", "Notification permission granted: $isGranted")
    }
    
    override fun attachBaseContext(newBase: Context?) {
        if (newBase != null) {
            val languageManager = LanguageManager(newBase)
            val language = languageManager.getLanguage()
            super.attachBaseContext(LocaleHelper.wrap(newBase, language))
        } else {
            super.attachBaseContext(newBase)
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Create notification channel on app start
        NotificationHelper.createNotificationChannel(this)
        
        // Request notification permission for Android 13+ (API 33+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    android.util.Log.d("MainActivity", "Notification permission already granted")
                }
                else -> {
                    android.util.Log.d("MainActivity", "Requesting notification permission")
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }
        
        // Handle notification click
        handleNotificationIntent(intent)
        
        setContent {
            val context = LocalContext.current
            val themeManager = remember { ThemeManager(context) }
            val systemDarkTheme = isSystemInDarkTheme()
            val followSystem = themeManager.isFollowingSystem()
            val darkTheme = if (followSystem) {
                systemDarkTheme
            } else {
                themeManager.isDarkModeEnabled()
            }
            
            // Handle notification intent when content is set
            LaunchedEffect(Unit) {
                handleNotificationIntent(intent)
            }
            
            WayFinderTheme(darkTheme = darkTheme) {
                // A surface container using the 'background' color from the theme
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    AppNavigation()
                }
            }
        }
    }
    
    private fun handleNotificationIntent(intent: Intent?) {
        intent?.let {
            val notificationId = it.getStringExtra("notification_id")
            val actionUrl = it.getStringExtra("action_url")
            
            android.util.Log.d("MainActivity", "Handling notification intent: actionUrl=$actionUrl, notificationId=$notificationId")
            
            // Set the action URL for AppNavigation to handle
            if (actionUrl != null && actionUrl.isNotBlank()) {
                android.util.Log.d("MainActivity", "Setting notification action URL: $actionUrl")
                setNotificationActionUrl(actionUrl)
            } else {
                android.util.Log.w("MainActivity", "No actionUrl found in notification intent")
            }
        }
    }
    
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleNotificationIntent(intent)
    }
}
