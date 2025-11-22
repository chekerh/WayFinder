package tn.esprit.wayfinder

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import tn.esprit.wayfinder.manager.LanguageManager
import tn.esprit.wayfinder.manager.ThemeManager
import tn.esprit.wayfinder.navigation.AppNavigation
import tn.esprit.wayfinder.ui.theme.WayFinderTheme
import tn.esprit.wayfinder.utils.LocaleHelper

class MainActivity : ComponentActivity() {
    
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
            
            WayFinderTheme(darkTheme = darkTheme) {
                // A surface container using the 'background' color from the theme
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    AppNavigation()
                }
            }
        }
    }
}
