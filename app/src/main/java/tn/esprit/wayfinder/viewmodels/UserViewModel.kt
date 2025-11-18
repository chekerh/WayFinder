package tn.esprit.wayfinder.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import tn.esprit.wayfinder.manager.TokenManager
import tn.esprit.wayfinder.models.User
import tn.esprit.wayfinder.presentation.user.UserRepository

sealed class UserUiState {
    object Idle : UserUiState()
    object Loading : UserUiState()
    data class Success(val user: User) : UserUiState()
    data class Error(val message: String) : UserUiState()
}

class UserViewModel(
    private val userRepository: UserRepository,
    private val tokenManager: TokenManager
) : ViewModel() {
    
    private val _uiState = MutableStateFlow<UserUiState>(UserUiState.Idle)
    val uiState: StateFlow<UserUiState> = _uiState.asStateFlow()
    
    fun loadProfile() {
        viewModelScope.launch {
            try {
                _uiState.value = UserUiState.Loading
                val user = userRepository.getProfile()
                cacheUser(user)
                _uiState.value = UserUiState.Success(user)
            } catch (e: Exception) {
                _uiState.value = UserUiState.Error(
                    e.message ?: "Failed to load profile"
                )
            }
        }
    }
    
    fun updateProfile(
        firstName: String? = null,
        lastName: String? = null,
        email: String? = null,
        phone: String? = null,
        location: String? = null,
        bio: String? = null,
        preferences: List<String>? = null
    ) {
        viewModelScope.launch {
            try {
                _uiState.value = UserUiState.Loading
                val updatedUser = userRepository.updateProfile(
                    firstName = firstName,
                    lastName = lastName,
                    email = email,
                    phone = phone,
                    location = location,
                    bio = bio,
                    preferences = preferences
                )
                cacheUser(updatedUser)
                _uiState.value = UserUiState.Success(updatedUser)
            } catch (e: Exception) {
                _uiState.value = UserUiState.Error(
                    e.message ?: "Failed to update profile"
                )
            }
        }
    }

    private fun cacheUser(user: User) {
        try {
            tokenManager.saveUser(user)
        } catch (_: Exception) {
            // Ignore cache errors to avoid blocking UI updates
        }
    }
}

