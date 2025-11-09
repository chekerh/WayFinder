package tn.esprit.wayfinder.presentation.auth

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import tn.esprit.wayfinder.WayfinderApp
import tn.esprit.wayfinder.viewmodels.AuthViewModel

class ViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
            val apiService = (application as WayfinderApp).apiService
            val repository = AuthRepository(apiService)
            @Suppress("UNCHECKED_CAST")
            return AuthViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
