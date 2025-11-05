package tn.esprit.wayfinder

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import tn.esprit.wayfinder.ui.screens.AfterSplashScreen
import tn.esprit.wayfinder.ui.screens.LoginScreen
import tn.esprit.wayfinder.ui.screens.SplashScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppNavigation()
        }
    }
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "splash_screen") {
        composable("splash_screen") {
            SplashScreen(navController = navController)
        }
        composable("after_splash_screen") {
            AfterSplashScreen(navController = navController)
        }
        composable("login_screen") {
            LoginScreen(navController = navController)
        }
        // Define other composables and navigation routes here
    }
}
