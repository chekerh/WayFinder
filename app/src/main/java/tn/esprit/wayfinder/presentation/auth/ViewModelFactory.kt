package tn.esprit.wayfinder.presentation.auth

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import tn.esprit.wayfinder.WayfinderApp
import tn.esprit.wayfinder.presentation.auth.OnboardingRepository
import tn.esprit.wayfinder.viewmodels.AuthViewModel
import tn.esprit.wayfinder.viewmodels.OnboardingViewModel

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
            else -> throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
