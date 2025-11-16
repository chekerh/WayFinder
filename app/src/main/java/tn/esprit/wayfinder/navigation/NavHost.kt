package tn.esprit.wayfinder.navigation

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import tn.esprit.wayfinder.manager.TokenManager
import tn.esprit.wayfinder.ui.screens.*

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val tokenManager = remember { TokenManager(context) }
    val savedToken = tokenManager.getToken()
    val savedUser = tokenManager.getUser()
    val startDestination = when {
        savedToken == null -> "splash_screen"
        savedUser?.onboardingCompleted == true -> "home"
        else -> "onboarding"
    }

    NavHost(navController, startDestination = startDestination) {
        composable("splash_screen") { SplashScreen(navController = navController) }
        composable("after_splash_screen") { AfterSplashScreen(navController = navController) }
        composable("login") { LoginScreen(navController = navController) }
        composable("signup_screen") { SignUpScreen(navController = navController) }
        composable("otp_screen") { VerificationScreenOTP(navController = navController) } 
        composable("home") { HomeScreen(navController = navController) }
        composable("onboarding") {
            // FIX: Pointing to SurveyScreen which contains the onboarding logic
            SurveyScreen(onComplete = {
                navController.navigate("home") {
                    popUpTo("onboarding") { inclusive = true }
                }
            })
        }
    }
}
