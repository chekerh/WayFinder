package tn.esprit.wayfinder.navigation

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
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
        composable(
            route = "all_flights/{region}",
            arguments = listOf(
                navArgument("region") {
                    type = NavType.StringType
                    nullable = true
                }
            )
        ) { backStackEntry ->
            val region = backStackEntry.arguments?.getString("region")
            // Convert "null" string to actual null
            val selectedRegion = if (region == "null" || region.isNullOrBlank()) null else region
            AllFlightsScreen(navController = navController, selectedRegion = selectedRegion)
        }
        composable(
            "flight_detail/{destinationId}",
            arguments = listOf(navArgument("destinationId") { type = NavType.StringType })
        ) { backStackEntry ->
            val destinationId = backStackEntry.arguments?.getString("destinationId") ?: ""
            FlightDetailsScreen(navController = navController, destinationId = destinationId)
        }
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
