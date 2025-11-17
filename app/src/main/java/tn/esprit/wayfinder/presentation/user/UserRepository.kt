package tn.esprit.wayfinder.presentation.user

import tn.esprit.wayfinder.models.UpdateProfileRequest
import tn.esprit.wayfinder.models.User
import tn.esprit.wayfinder.network.ApiService

class UserRepository(private val apiService: ApiService) {
    
    suspend fun getProfile(): User {
        return apiService.getProfile()
    }
    
    suspend fun updateProfile(
        firstName: String? = null,
        lastName: String? = null,
        email: String? = null,
        phone: String? = null,
        location: String? = null,
        bio: String? = null
    ): User {
        val request = UpdateProfileRequest(
            firstName = firstName,
            lastName = lastName,
            email = email,
            phone = phone,
            location = location,
            bio = bio
        )
        return apiService.updateProfile(request)
    }
}

