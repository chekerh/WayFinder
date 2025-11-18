package tn.esprit.wayfinder.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class User(
    @SerialName("_id") val id: String,
    val username: String,
    val email: String,
    @SerialName("first_name") val firstName: String,
    @SerialName("last_name") val lastName: String,
    val phone: String? = null,
    val location: String? = null,
    val bio: String? = null,
    @SerialName("profile_image_url") val profileImageUrl: String? = null,
    val preferences: List<String>,
    val status: String,
    @SerialName("onboarding_completed") val onboardingCompleted: Boolean = false,
    @SerialName("onboarding_preferences") val onboardingPreferences: Map<String, JsonElement>? = null,
    @SerialName("createdAt") val createdAt: String? = null,
    @SerialName("updatedAt") val updatedAt: String? = null
)

@Serializable
data class LoginRequest(
    val username: String,
    val password: String
)

@Serializable
data class LoginResponse(
    @SerialName("access_token") val accessToken: String,
    val user: User,
    @SerialName("onboarding_completed") val onboardingCompleted: Boolean
)

@Serializable
data class UpdateProfileRequest(
    @SerialName("first_name") val firstName: String? = null,
    @SerialName("last_name") val lastName: String? = null,
    val email: String? = null,
    val phone: String? = null,
    val location: String? = null,
    val bio: String? = null,
    val preferences: List<String>? = null
)

@Serializable
data class SignUpRequest(
    val username: String,
    val email: String,
    @SerialName("first_name") val first_name: String,
    @SerialName("last_name") val last_name: String,
    val password: String
)

@Serializable
data class SignUpResponse(
    val message: String,
    val user: User
)
