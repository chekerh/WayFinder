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
    @SerialName("onboarding_skipped") val onboardingSkipped: Boolean = false,
    @SerialName("onboarding_preferences") val onboardingPreferences: Map<String, JsonElement>? = null,
    @SerialName("total_points") val totalPoints: Int = 0,
    @SerialName("lifetime_points") val lifetimePoints: Int = 0,
    @SerialName("current_streak") val currentStreak: Int = 0,
    @SerialName("longest_streak") val longestStreak: Int = 0,
    @SerialName("createdAt") val createdAt: String? = null,
    @SerialName("updatedAt") val updatedAt: String? = null
)

@Serializable
data class LoginRequest(
    val email: String, // Changed from username to email
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
data class UploadProfileImageResponse(
    val message: String,
    @SerialName("profile_image_url") val profileImageUrl: String,
    val user: User
)

@Serializable
data class SignUpRequest(
    val email: String,
    @SerialName("first_name") val first_name: String,
    @SerialName("last_name") val last_name: String,
    val password: String
)

@Serializable
data class SendOTPForRegistrationRequest(
    val email: String
)

@Serializable
data class SendOTPForRegistrationResponse(
    val message: String
)

@Serializable
data class RegisterWithOTPRequest(
    val username: String, // Generated from email
    val email: String,
    @SerialName("first_name") val first_name: String,
    @SerialName("last_name") val last_name: String,
    val password: String,
    @SerialName("otp_code") val otp_code: String
)

@Serializable
data class RegisterWithOTPResponse(
    val message: String,
    val user: User
)

@Serializable
data class SignUpResponse(
    val message: String,
    val user: User
)

@Serializable
data class GoogleSignInRequest(
    @SerialName("id_token") val idToken: String,
    @SerialName("client_type") val clientType: String = "android"
)

@Serializable
data class GoogleSignInResponse(
    @SerialName("access_token") val accessToken: String,
    val user: User,
    @SerialName("onboarding_completed") val onboardingCompleted: Boolean,
    @SerialName("email_verified") val emailVerified: Boolean
)

@Serializable
data class VerifyEmailRequest(
    val token: String
)

@Serializable
data class VerifyEmailResponse(
    val message: String,
    val user: User
)

@Serializable
data class ResendVerificationRequest(
    val email: String
)

@Serializable
data class ResendVerificationResponse(
    val message: String
)

@Serializable
data class FcmTokenRequest(
    val token: String
)

@Serializable
data class FcmTokenResponse(
    val message: String
)

@Serializable
data class GoogleMapsApiKeyResponse(
    @SerialName("apiKey") val apiKey: String? = null
)
