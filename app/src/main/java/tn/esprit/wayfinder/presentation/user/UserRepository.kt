package tn.esprit.wayfinder.presentation.user

import okhttp3.MultipartBody
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
        bio: String? = null,
        preferences: List<String>? = null
    ): User {
        val request = UpdateProfileRequest(
            firstName = firstName,
            lastName = lastName,
            email = email,
            phone = phone,
            location = location,
            bio = bio,
            preferences = preferences
        )
        return apiService.updateProfile(request)
    }
    
    suspend fun uploadProfileImage(imagePart: MultipartBody.Part): User {
        val response = apiService.uploadProfileImage(imagePart)
        // Extract user from response
        val userJson = response["user"] as? Map<*, *>
        return if (userJson != null) {
            // Convert map to User - in production, use proper deserialization
            // For now, return a basic user object
            User(
                id = userJson["_id"] as? String ?: "",
                username = userJson["username"] as? String ?: "",
                email = userJson["email"] as? String ?: "",
                firstName = userJson["first_name"] as? String ?: "",
                lastName = userJson["last_name"] as? String ?: "",
                phone = userJson["phone"] as? String,
                location = userJson["location"] as? String,
                bio = userJson["bio"] as? String,
                profileImageUrl = userJson["profile_image_url"] as? String,
                preferences = (userJson["preferences"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
                status = userJson["status"] as? String ?: "ACTIVE",
                onboardingCompleted = userJson["onboarding_completed"] as? Boolean ?: false
            )
        } else {
            throw Exception("Failed to parse user from upload response")
        }
    }
}

