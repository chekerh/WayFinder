package tn.esprit.wayfinder.navigation

import android.app.Application
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.compose.material3.Text
import kotlinx.coroutines.delay
import tn.esprit.wayfinder.manager.TokenManager
import tn.esprit.wayfinder.presentation.auth.ViewModelFactory
import tn.esprit.wayfinder.ui.screens.*
import tn.esprit.wayfinder.viewmodels.NotificationsViewModel

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
    
    // Check for new notifications periodically and show popups
    if (savedToken != null) {
        val notificationsViewModel: NotificationsViewModel = viewModel(
            factory = ViewModelFactory(context.applicationContext as Application)
        )
        
        // Check for new notifications periodically when app is open
        LaunchedEffect(Unit) {
            // Initial check after 2 seconds
            delay(2000)
            notificationsViewModel.loadNotifications(unreadOnly = true, showSystemNotifications = true)
            
            // Then check every 5 seconds
            while (true) {
                delay(5000) // Check every 5 seconds
                notificationsViewModel.loadNotifications(unreadOnly = true, showSystemNotifications = true)
            }
        }
    }

    NavHost(navController, startDestination = startDestination) {
        composable("splash_screen") { SplashScreen(navController = navController) }
        composable("after_splash_screen") { AfterSplashScreen(navController = navController) }
        composable("login") { LoginScreen(navController = navController) }
        composable("signup_screen") { SignUpScreen(navController = navController) }
        composable("otp_screen") { VerificationScreenOTP(navController = navController) }
        composable(
            "email_verification",
            arguments = listOf(
                navArgument("token") { type = NavType.StringType; nullable = true },
                navArgument("email") { type = NavType.StringType; nullable = true }
            )
        ) { backStackEntry ->
            val token = backStackEntry.arguments?.getString("token")
            val email = backStackEntry.arguments?.getString("email")
            EmailVerificationScreen(navController = navController, email = email, token = token)
        } 
        composable("home") { HomeScreen(navController = navController) }
        composable("profile") { ProfileScreen(navController = navController) }
        composable("edit_profile") { EditProfileScreen(navController = navController) }
        composable("favorites") { 
            FavoritesScreen(navController = navController)
        }
        composable("chat") { 
            ChatScreen(navController = navController)
        }
        composable("booking_history") { BookingHistoryScreen(navController = navController) }
        composable(
            "booking_detail/{bookingId}",
            arguments = listOf(navArgument("bookingId") { type = NavType.StringType })
        ) { backStackEntry ->
            val bookingId = backStackEntry.arguments?.getString("bookingId") ?: ""
            BookingDetailScreen(navController = navController, bookingId = bookingId)
        }
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
        composable(
            "booking/{destinationId}",
            arguments = listOf(navArgument("destinationId") { type = NavType.StringType })
        ) { backStackEntry ->
            val destinationId = backStackEntry.arguments?.getString("destinationId") ?: ""
            ReservationScreen(navController = navController, destinationId = destinationId)
        }
        composable(
            "booking_confirmation/{bookingId}",
            arguments = listOf(navArgument("bookingId") { type = NavType.StringType })
        ) { backStackEntry ->
            val bookingId = backStackEntry.arguments?.getString("bookingId") ?: ""
            BookingConfirmationScreen(navController = navController, bookingId = bookingId)
        }
        composable("onboarding") {
            // FIX: Pointing to SurveyScreen which contains the onboarding logic
            SurveyScreen(onComplete = {
                navController.navigate("home") {
                    popUpTo("onboarding") { inclusive = true }
                }
            })
        }
        composable(
            "lodging_choice/{destinationId}",
            arguments = listOf(navArgument("destinationId") { 
                type = NavType.StringType
                nullable = true
            })
        ) { backStackEntry ->
            val destinationId = backStackEntry.arguments?.getString("destinationId")
            LodgingChoiceScreen(navController = navController, destinationId = destinationId)
        }
        composable("during_travel") {
            DuringTravelScreen(navController = navController)
        }
        composable("discussions") {
            DiscussionScreen(navController = navController)
        }
        composable(
            "post_detail/{postId}",
            arguments = listOf(navArgument("postId") { type = NavType.StringType })
        ) { backStackEntry ->
            val postId = backStackEntry.arguments?.getString("postId") ?: ""
            PostDetailScreen(navController = navController, postId = postId)
        }
        composable(
            "error/{code}",
            arguments = listOf(navArgument("code") { 
                type = NavType.StringType
                defaultValue = "404"
            })
        ) { backStackEntry ->
            val errorCode = backStackEntry.arguments?.getString("code") ?: "404"
            ErrorScreen(navController = navController, errorCode = errorCode)
        }
        composable("itineraries") {
            ItineraryListScreen(navController = navController)
        }
        composable(
            "itinerary_detail/{itineraryId}",
            arguments = listOf(navArgument("itineraryId") { type = NavType.StringType })
        ) { backStackEntry ->
            val itineraryId = backStackEntry.arguments?.getString("itineraryId") ?: ""
            ItineraryDetailScreen(navController = navController, itineraryId = itineraryId)
        }
        composable("create_itinerary") {
            CreateItineraryScreen(navController = navController)
        }
        composable("notifications") {
            NotificationsScreen(navController = navController)
        }
        composable("search_history") {
            SearchHistoryScreen(navController = navController)
        }
        composable("price_alerts") {
            PriceAlertsScreen(navController = navController)
        }
        composable(
            "travel_tips/{destinationId}",
            arguments = listOf(navArgument("destinationId") { type = NavType.StringType })
        ) { backStackEntry ->
            val destinationId = backStackEntry.arguments?.getString("destinationId") ?: ""
            val destinationName = backStackEntry.savedStateHandle.get<String>("destinationName")
            val city = backStackEntry.savedStateHandle.get<String>("city")
            val country = backStackEntry.savedStateHandle.get<String>("country")
            TravelTipsScreen(
                navController = navController,
                destinationId = destinationId,
                destinationName = destinationName,
                city = city,
                country = country
            )
        }
        composable("offline_destinations") {
            OfflineDestinationsScreen(navController = navController)
        }
        composable("share_journey") {
            ShareJourneyScreen(navController = navController)
        }
        composable("journey_feed") {
            JourneyFeedScreen(navController = navController)
        }
        composable(
            "journey_detail/{journeyId}",
            arguments = listOf(navArgument("journeyId") { type = NavType.StringType })
        ) { backStackEntry ->
            val journeyId = backStackEntry.arguments?.getString("journeyId") ?: ""
            JourneyDetailScreen(navController = navController, journeyId = journeyId)
        }
    }
}
