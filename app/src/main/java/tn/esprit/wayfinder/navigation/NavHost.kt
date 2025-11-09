package tn.esprit.wayfinder.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import tn.esprit.wayfinder.ui.screens.*

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    NavHost(navController, startDestination = "splash_screen") {
        composable("splash_screen") { SplashScreen(navController = navController) }
        composable("after_splash_screen") { AfterSplashScreen(navController = navController) }
        composable("login_screen") { LoginScreen(navController = navController) }
        composable("signup_screen") { SignUpScreen(navController = navController) }
        // FIX: The route was pointing to a screen I deleted. This now points to the correct OTP screen.
        composable("otp_screen") { VerificationScreenOTP(navController = navController) } 
        // FIX: The MainApp composable was deleted in my cleanup. This now directly composes your existing HomeScreen.
        composable("main_app") { HomeScreen(navController = navController) }
        composable("ai_onboarding_form") { SurveyScreen(navController = navController) }
    }
}
