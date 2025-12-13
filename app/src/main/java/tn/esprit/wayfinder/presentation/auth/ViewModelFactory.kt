package tn.esprit.wayfinder.presentation.auth

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import tn.esprit.wayfinder.WayfinderApp
import tn.esprit.wayfinder.data.FlightsCache
import tn.esprit.wayfinder.manager.TokenManager
import tn.esprit.wayfinder.presentation.auth.OnboardingRepository
import tn.esprit.wayfinder.presentation.booking.BookingRepository
import tn.esprit.wayfinder.presentation.catalog.CatalogRepository
import tn.esprit.wayfinder.presentation.payment.PaypalRepository
import tn.esprit.wayfinder.presentation.user.UserRepository
import tn.esprit.wayfinder.presentation.discussion.DiscussionRepository
import tn.esprit.wayfinder.presentation.favorites.FavoritesRepository
import tn.esprit.wayfinder.presentation.reviews.ReviewsRepository
import tn.esprit.wayfinder.presentation.itinerary.ItineraryRepository
import tn.esprit.wayfinder.presentation.notifications.NotificationsRepository
import tn.esprit.wayfinder.presentation.social.SocialRepository
import tn.esprit.wayfinder.presentation.searchhistory.SearchHistoryRepository
import tn.esprit.wayfinder.presentation.pricealerts.PriceAlertsRepository
import tn.esprit.wayfinder.presentation.traveltips.TravelTipsRepository
import tn.esprit.wayfinder.presentation.journey.JourneyRepository
import tn.esprit.wayfinder.presentation.chat.ChatRepository
import tn.esprit.wayfinder.viewmodels.ActivitiesViewModel
import tn.esprit.wayfinder.viewmodels.AuthViewModel
import tn.esprit.wayfinder.viewmodels.BookingViewModel
import tn.esprit.wayfinder.viewmodels.CatalogViewModel
import tn.esprit.wayfinder.viewmodels.DiscussionViewModel
import tn.esprit.wayfinder.viewmodels.FavoritesViewModel
import tn.esprit.wayfinder.viewmodels.OnboardingViewModel
import tn.esprit.wayfinder.viewmodels.ReviewsViewModel
import tn.esprit.wayfinder.viewmodels.PaymentViewModel
import tn.esprit.wayfinder.viewmodels.UserViewModel
import tn.esprit.wayfinder.viewmodels.ItineraryViewModel
import tn.esprit.wayfinder.viewmodels.NotificationsViewModel
import tn.esprit.wayfinder.viewmodels.SocialViewModel
import tn.esprit.wayfinder.viewmodels.SearchHistoryViewModel
import tn.esprit.wayfinder.viewmodels.PriceAlertsViewModel
import tn.esprit.wayfinder.viewmodels.TravelTipsViewModel
import tn.esprit.wayfinder.viewmodels.JourneyViewModel
import tn.esprit.wayfinder.viewmodels.ChatViewModel
import tn.esprit.wayfinder.viewmodels.OutfitWeatherViewModel
import tn.esprit.wayfinder.viewmodels.ReelsViewModel
import tn.esprit.wayfinder.viewmodels.GroupFlightViewModel
import tn.esprit.wayfinder.viewmodels.UpsellViewModel
import tn.esprit.wayfinder.viewmodels.HotelsViewModel
import tn.esprit.wayfinder.viewmodels.AiTravelVideoViewModel
import tn.esprit.wayfinder.presentation.groupflight.GroupFlightRepository
import tn.esprit.wayfinder.presentation.upsells.UpsellRepository

class ViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val apiService = (application as WayfinderApp).apiService

        return when {
            modelClass.isAssignableFrom(AuthViewModel::class.java) -> {
                val repository = AuthRepository(apiService)
                @Suppress("UNCHECKED_CAST")
                AuthViewModel(repository) as T
            }
            modelClass.isAssignableFrom(OnboardingViewModel::class.java) -> {
                val repository = OnboardingRepository(apiService)
                @Suppress("UNCHECKED_CAST")
                OnboardingViewModel(repository) as T
            }
            modelClass.isAssignableFrom(CatalogViewModel::class.java) -> {
                val repository = CatalogRepository(apiService, application.applicationContext)
                val cache = FlightsCache(application.applicationContext)
                @Suppress("UNCHECKED_CAST")
                CatalogViewModel(repository, cache) as T
            }
                   modelClass.isAssignableFrom(UserViewModel::class.java) -> {
                       val repository = UserRepository(apiService, application.applicationContext)
                       val tokenManager = TokenManager(application.applicationContext)
                       @Suppress("UNCHECKED_CAST")
                       UserViewModel(repository, tokenManager) as T
                   }
            modelClass.isAssignableFrom(BookingViewModel::class.java) -> {
                val repository = BookingRepository(apiService, application.applicationContext)
                @Suppress("UNCHECKED_CAST")
                BookingViewModel(repository) as T
            }
            modelClass.isAssignableFrom(PaymentViewModel::class.java) -> {
                val repository = PaypalRepository(apiService)
                @Suppress("UNCHECKED_CAST")
                PaymentViewModel(repository) as T
            }
            modelClass.isAssignableFrom(ActivitiesViewModel::class.java) -> {
                val repository = CatalogRepository(apiService, application.applicationContext)
                @Suppress("UNCHECKED_CAST")
                ActivitiesViewModel(repository) as T
            }
            modelClass.isAssignableFrom(DiscussionViewModel::class.java) -> {
                val repository = DiscussionRepository(apiService)
                @Suppress("UNCHECKED_CAST")
                DiscussionViewModel(repository) as T
            }
            modelClass.isAssignableFrom(FavoritesViewModel::class.java) -> {
                val repository = FavoritesRepository(apiService)
                @Suppress("UNCHECKED_CAST")
                FavoritesViewModel(repository) as T
            }
            modelClass.isAssignableFrom(ReviewsViewModel::class.java) -> {
                val repository = ReviewsRepository(apiService)
                @Suppress("UNCHECKED_CAST")
                ReviewsViewModel(repository) as T
            }
            modelClass.isAssignableFrom(ItineraryViewModel::class.java) -> {
                val repository = ItineraryRepository(apiService)
                @Suppress("UNCHECKED_CAST")
                ItineraryViewModel(repository) as T
            }
            modelClass.isAssignableFrom(NotificationsViewModel::class.java) -> {
                val repository = NotificationsRepository(apiService, application.applicationContext)
                @Suppress("UNCHECKED_CAST")
                NotificationsViewModel(repository, application.applicationContext) as T
            }
            modelClass.isAssignableFrom(SocialViewModel::class.java) -> {
                val repository = SocialRepository(apiService)
                @Suppress("UNCHECKED_CAST")
                SocialViewModel(repository) as T
            }
            modelClass.isAssignableFrom(SearchHistoryViewModel::class.java) -> {
                val repository = SearchHistoryRepository(apiService)
                @Suppress("UNCHECKED_CAST")
                SearchHistoryViewModel(repository) as T
            }
            modelClass.isAssignableFrom(PriceAlertsViewModel::class.java) -> {
                val repository = PriceAlertsRepository(apiService)
                @Suppress("UNCHECKED_CAST")
                PriceAlertsViewModel(repository) as T
            }
            modelClass.isAssignableFrom(TravelTipsViewModel::class.java) -> {
                val repository = TravelTipsRepository(apiService)
                @Suppress("UNCHECKED_CAST")
                TravelTipsViewModel(repository) as T
            }
            modelClass.isAssignableFrom(JourneyViewModel::class.java) -> {
                val repository = JourneyRepository(apiService)
                @Suppress("UNCHECKED_CAST")
                JourneyViewModel(repository) as T
            }
            modelClass.isAssignableFrom(ChatViewModel::class.java) -> {
                val repository = ChatRepository(apiService)
                @Suppress("UNCHECKED_CAST")
                ChatViewModel(repository, application.applicationContext) as T
            }
            modelClass.isAssignableFrom(OutfitWeatherViewModel::class.java) -> {
                @Suppress("UNCHECKED_CAST")
                OutfitWeatherViewModel(apiService) as T
            }
            modelClass.isAssignableFrom(ReelsViewModel::class.java) -> {
                val discussionRepository = DiscussionRepository(apiService)
                val journeyRepository = JourneyRepository(apiService)
                @Suppress("UNCHECKED_CAST")
                ReelsViewModel(discussionRepository, journeyRepository) as T
            }
            modelClass.isAssignableFrom(GroupFlightViewModel::class.java) -> {
                val repository = GroupFlightRepository(apiService)
                @Suppress("UNCHECKED_CAST")
                GroupFlightViewModel(repository) as T
            }
            modelClass.isAssignableFrom(UpsellViewModel::class.java) -> {
                val catalogRepository = CatalogRepository(apiService, application.applicationContext)
                val repository = UpsellRepository(apiService, catalogRepository, application.applicationContext)
                @Suppress("UNCHECKED_CAST")
                UpsellViewModel(repository) as T
            }
            modelClass.isAssignableFrom(HotelsViewModel::class.java) -> {
                val repository = CatalogRepository(apiService, application.applicationContext)
                @Suppress("UNCHECKED_CAST")
                HotelsViewModel(repository) as T
            }
            modelClass.isAssignableFrom(AiTravelVideoViewModel::class.java) -> {
                @Suppress("UNCHECKED_CAST")
                AiTravelVideoViewModel(apiService) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
