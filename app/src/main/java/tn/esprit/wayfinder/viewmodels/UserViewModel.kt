package tn.esprit.wayfinder.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.MultipartBody
import tn.esprit.wayfinder.manager.TokenManager
import tn.esprit.wayfinder.models.UserPointsResponse
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

    private val _pointsState = MutableStateFlow<UserPointsResponse?>(null)
    val pointsState: StateFlow<UserPointsResponse?> = _pointsState.asStateFlow()
    
    init {
        // Try to load cached user first to show immediately
        val cachedUser = tokenManager.getUser()
        if (cachedUser != null) {
            _uiState.value = UserUiState.Success(cachedUser)
        }
    }
    
    fun loadProfile() {
        viewModelScope.launch {
            try {
                // Only show loading if we don't have cached data
                val currentState = _uiState.value
                if (currentState !is UserUiState.Success) {
                    _uiState.value = UserUiState.Loading
                }
                val user = userRepository.getProfile()
                cacheUser(user)
                _uiState.value = UserUiState.Success(user)
                // Load latest points summary after profile for accurate score
                loadUserPoints()
            } catch (e: Exception) {
                // If we have cached data, keep showing it even if API call fails
                val cachedUser = tokenManager.getUser()
                if (cachedUser != null) {
                    _uiState.value = UserUiState.Success(cachedUser)
                } else {
                    _uiState.value = UserUiState.Error(
                        e.message ?: "Failed to load profile"
                    )
                }
            }
        }
    }

    private fun loadUserPoints() {
        viewModelScope.launch {
            try {
                _pointsState.value = userRepository.getUserPoints()
            } catch (_: Exception) {
                // Ignore points errors; UI can fall back to profile values
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

    fun uploadProfileImage(imagePart: MultipartBody.Part) {
        viewModelScope.launch {
            try {
                _uiState.value = UserUiState.Loading
                val updatedUser = userRepository.uploadProfileImage(imagePart)
                cacheUser(updatedUser)
                _uiState.value = UserUiState.Success(updatedUser)
            } catch (e: Exception) {
                _uiState.value = UserUiState.Error(
                    e.message ?: "Failed to upload profile image"
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

