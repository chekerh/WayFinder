package tn.esprit.wayfinder.navigation

import android.app.Application
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.flow.collectLatest
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import kotlinx.coroutines.delay
import tn.esprit.wayfinder.MainActivity
import tn.esprit.wayfinder.manager.TokenManager
import tn.esprit.wayfinder.presentation.auth.ViewModelFactory
import tn.esprit.wayfinder.ui.screens.*
import tn.esprit.wayfinder.ui.screens.MapMemoriesScreen
import tn.esprit.wayfinder.ui.screens.FlightComparisonScreen
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
        
        // Handle notification navigation - observe StateFlow changes
        LaunchedEffect(Unit) {
            MainActivity.notificationActionUrl.collectLatest { actionUrl ->
                actionUrl?.let { url ->
                    android.util.Log.d("AppNavigation", "Navigating to notification action: $url")
                    // Small delay to ensure navigation is ready
                    delay(100)
                    // Parse actionUrl and navigate
                    try {
                        when {
                            url.startsWith("/post_detail/") -> {
                                val postId = url.removePrefix("/post_detail/")
                                android.util.Log.d("AppNavigation", "Navigating to post_detail: $postId")
                                navController.navigate("post_detail/$postId") {
                                    popUpTo("home") { inclusive = false }
                                }
                            }
                            url.startsWith("/journey_detail/") -> {
                                val journeyId = url.removePrefix("/journey_detail/")
                                android.util.Log.d("AppNavigation", "Navigating to journey_detail: $journeyId")
                                navController.navigate("journey_detail/$journeyId") {
                                    popUpTo("home") { inclusive = false }
                                }
                            }
                            url.startsWith("/booking_detail/") -> {
                                val bookingId = url.removePrefix("/booking_detail/")
                                android.util.Log.d("AppNavigation", "Navigating to booking_detail: $bookingId")
                                navController.navigate("booking_detail/$bookingId") {
                                    popUpTo("home") { inclusive = false }
                                }
                            }
                            else -> {
                                android.util.Log.w("AppNavigation", "Unknown actionUrl format: $url")
                            }
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("AppNavigation", "Error navigating to $url", e)
                    }
                    // Clear the action URL after handling
                    MainActivity.setNotificationActionUrl(null)
                }
            }
        }
        
        // Check for new notifications periodically when app is open
        LaunchedEffect(Unit) {
            // Initial check after 2 seconds
            delay(2000)
            notificationsViewModel.loadNotifications(unreadOnly = true, showSystemNotifications = true)
            
            // Then check every 30 seconds (reduced from 5 seconds to prevent rate limiting)
            while (true) {
                delay(30000) // Check every 30 seconds
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
            "airline_selection/{destinationId}",
            arguments = listOf(navArgument("destinationId") { type = NavType.StringType })
        ) { backStackEntry ->
            val destinationId = backStackEntry.arguments?.getString("destinationId") ?: ""
            AirlineSelectionScreen(navController = navController, destinationId = destinationId)
        }
        composable(
            "organize_flight/{destinationId}",
            arguments = listOf(navArgument("destinationId") { type = NavType.StringType })
        ) { backStackEntry ->
            val destinationId = backStackEntry.arguments?.getString("destinationId") ?: ""
            OrganizeFlightScreen(navController = navController, destinationId = destinationId)
        }
        composable(
            "group_flight_detail/{groupId}",
            arguments = listOf(navArgument("groupId") { type = NavType.StringType })
        ) { backStackEntry ->
            val groupId = backStackEntry.arguments?.getString("groupId") ?: ""
            GroupFlightDetailScreen(navController = navController, groupId = groupId)
        }
        composable(
            "flight_comparison/{destinationId}",
            arguments = listOf(navArgument("destinationId") { type = NavType.StringType })
        ) { backStackEntry ->
            val destinationId = backStackEntry.arguments?.getString("destinationId") ?: ""
            FlightComparisonScreen(navController = navController, destinationId = destinationId)
        }
        composable(
            "booking/{destinationId}",
            arguments = listOf(navArgument("destinationId") { type = NavType.StringType })
        ) { backStackEntry ->
            val destinationId = backStackEntry.arguments?.getString("destinationId") ?: ""
            ReservationScreen(navController = navController, destinationId = destinationId)
        }
        composable(
            "review_booking/{destinationId}",
            arguments = listOf(navArgument("destinationId") { type = NavType.StringType })
        ) { backStackEntry ->
            val destinationId = backStackEntry.arguments?.getString("destinationId") ?: ""
            ReviewBookingScreen(navController = navController, destinationId = destinationId)
        }
        composable(
            "booking_confirmation/{bookingId}",
            arguments = listOf(navArgument("bookingId") { type = NavType.StringType })
        ) { backStackEntry ->
            val bookingId = backStackEntry.arguments?.getString("bookingId") ?: ""
            BookingConfirmationScreen(navController = navController, bookingId = bookingId)
        }
        composable(
            "tickets/{bookingId}",
            arguments = listOf(navArgument("bookingId") { type = NavType.StringType })
        ) { backStackEntry ->
            val bookingId = backStackEntry.arguments?.getString("bookingId") ?: ""
            TicketsScreen(navController = navController, bookingId = bookingId)
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
        composable(
            "accommodations/{destinationId}/{type}",
            arguments = listOf(
                navArgument("destinationId") { type = NavType.StringType },
                navArgument("type") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val destinationId = backStackEntry.arguments?.getString("destinationId") ?: ""
            val type = backStackEntry.arguments?.getString("type") ?: ""
            AccommodationsListScreen(navController = navController, destinationId = destinationId, accommodationType = type)
        }
        composable(
            "upsells/{destinationId}",
            arguments = listOf(navArgument("destinationId") { type = NavType.StringType })
        ) { backStackEntry ->
            val destinationId = backStackEntry.arguments?.getString("destinationId") ?: ""
            UpsellScreen(navController = navController, destinationId = destinationId)
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
        composable("outfit_selection") {
            OutfitSelectionScreen(navController = navController)
        }
        composable(
            "outfit_upload/{bookingId}",
            arguments = listOf(navArgument("bookingId") { type = NavType.StringType })
        ) { backStackEntry ->
            val bookingId = backStackEntry.arguments?.getString("bookingId") ?: ""
            OutfitUploadScreen(navController = navController, bookingId = bookingId)
        }
        composable(
            "outfit_result/{outfitId}",
            arguments = listOf(navArgument("outfitId") { type = NavType.StringType })
        ) { backStackEntry ->
            val outfitId = backStackEntry.arguments?.getString("outfitId") ?: ""
            OutfitResultScreen(navController = navController, outfitId = outfitId)
        }
        composable("map_memories") {
            MapMemoriesScreen(navController = navController)
        }
        composable(
            "reels_viewer/{initialIndex}",
            arguments = listOf(navArgument("initialIndex") { 
                type = NavType.IntType
                defaultValue = 0
            })
        ) { backStackEntry ->
            val initialIndex = backStackEntry.arguments?.getInt("initialIndex") ?: 0
            ReelsViewerScreen(navController = navController, initialIndex = initialIndex)
        }
        // Personalized Results Screen - for showing search/recommendation results
        composable("personalized_results") {
            PersonalizedResultsScreen(navController = navController)
        }
        // PayPal Payment Screen - for processing PayPal payments
        composable(
            "paypal_payment",
            arguments = listOf(
                navArgument("approvalUrl") { type = NavType.StringType },
                navArgument("bookingId") { type = NavType.StringType; nullable = true }
            )
        ) { backStackEntry ->
            val approvalUrl = backStackEntry.arguments?.getString("approvalUrl") ?: ""
            val bookingId = backStackEntry.arguments?.getString("bookingId")
            PaypalPaymentScreen(
                navController = navController,
                approvalUrl = approvalUrl,
                onPaymentSuccess = { paymentId ->
                    // Navigate to booking confirmation after successful payment
                    if (bookingId != null) {
                        navController.navigate("booking_confirmation/$bookingId") {
                            popUpTo("home") { inclusive = false }
                        }
                    } else {
                        navController.navigate("booking_history") {
                            popUpTo("home") { inclusive = false }
                        }
                    }
                },
                onPaymentFailed = {
                    // Navigate back or show error
                    navController.popBackStack()
                }
            )
        }
        // Detail Screen - for showing destination/activity details
        composable(
            "detail/{type}",
            arguments = listOf(
                navArgument("type") { type = NavType.StringType },
                navArgument("id") { type = NavType.StringType; nullable = true },
                navArgument("title") { type = NavType.StringType; nullable = true }
            )
        ) { backStackEntry ->
            val type = backStackEntry.arguments?.getString("type") ?: "destination"
            val id = backStackEntry.arguments?.getString("id")
            val title = backStackEntry.arguments?.getString("title")
            DetailScreen(navController = navController)
        }
        // Alternative Confirmation Screen (if needed for different confirmation flows)
        composable("confirmation") {
            ConfirmationScreen(navController = navController)
        }
        // Legacy Ticket Screen (keeping for backward compatibility, but TicketsScreen is preferred)
        composable("ticket") {
            TicketScreen(navController = navController)
        }
    }
}
