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
import tn.esprit.wayfinder.presentation.payment.FlouciRepository
import tn.esprit.wayfinder.presentation.user.UserRepository
import tn.esprit.wayfinder.viewmodels.ActivitiesViewModel
import tn.esprit.wayfinder.viewmodels.AuthViewModel
import tn.esprit.wayfinder.viewmodels.BookingViewModel
import tn.esprit.wayfinder.viewmodels.CatalogViewModel
import tn.esprit.wayfinder.viewmodels.OnboardingViewModel
import tn.esprit.wayfinder.viewmodels.PaymentViewModel
import tn.esprit.wayfinder.viewmodels.UserViewModel

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
                val repository = CatalogRepository(apiService)
                val cache = FlightsCache(application.applicationContext)
                @Suppress("UNCHECKED_CAST")
                CatalogViewModel(repository, cache) as T
            }
                   modelClass.isAssignableFrom(UserViewModel::class.java) -> {
                       val repository = UserRepository(apiService)
                       val tokenManager = TokenManager(application.applicationContext)
                       @Suppress("UNCHECKED_CAST")
                       UserViewModel(repository, tokenManager) as T
                   }
            modelClass.isAssignableFrom(BookingViewModel::class.java) -> {
                val repository = BookingRepository(apiService)
                @Suppress("UNCHECKED_CAST")
                BookingViewModel(repository) as T
            }
            modelClass.isAssignableFrom(PaymentViewModel::class.java) -> {
                val repository = FlouciRepository(apiService)
                @Suppress("UNCHECKED_CAST")
                PaymentViewModel(repository) as T
            }
            modelClass.isAssignableFrom(ActivitiesViewModel::class.java) -> {
                val repository = CatalogRepository(apiService)
                @Suppress("UNCHECKED_CAST")
                ActivitiesViewModel(repository) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
