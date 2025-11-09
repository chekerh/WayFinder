package tn.esprit.wayfinder.models

import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// --- USER & AUTH --- //

@Serializable
data class User(
    @SerialName("_id") val id: String,
    val username: String,
    val email: String,
    @SerialName("first_name") val firstName: String,
    @SerialName("last_name") val lastName: String,
    val preferences: List<String>,
    val status: String,
    @SerialName("onboarding_completed") val onboardingCompleted: Boolean = false,
    @SerialName("createdAt") val createdAt: String? = null,
    @SerialName("updatedAt") val updatedAt: String? = null
)

@Serializable
data class LoginResponse(
    @SerialName("access_token") val accessToken: String,
    val user: User,
    @SerialName("onboarding_completed") val onboardingCompleted: Boolean
)

@Serializable
data class SignUpRequest(
    val username: String,
    val email: String,
    val first_name: String,
    val last_name: String,
    val password: String
)

@Serializable
data class SignUpResponse(
    val message: String,
    val user: User
)


// --- ONBOARDING --- //

@Serializable
data class OnboardingQuestion(
    val id: String,
    val type: String,
    val text: String,
    val options: List<QuestionOption>? = null,
    val required: Boolean = true,
    @SerialName("min_selections") val minSelections: Int? = null,
    @SerialName("max_selections") val maxSelections: Int? = null
)

@Serializable
data class QuestionOption(
    val value: String,
    val label: String,
    val min: Double? = null,
    val max: Double? = null
)

@Serializable
data class OnboardingResponse(
    @SerialName("session_id") val sessionId: String,
    val question: OnboardingQuestion,
    val progress: Progress,
    val completed: Boolean,
    @SerialName("redirect_to") val redirectTo: String? = null,
    val message: String? = null
)

@Serializable
data class Progress(
    val current: Int,
    val total: Int? = null
)

@Serializable
data class OnboardingStatus(
    @SerialName("onboarding_completed") val onboardingCompleted: Boolean,
    @SerialName("session_id") val sessionId: String? = null,
    val progress: OnboardingProgress,
    @SerialName("can_resume") val canResume: Boolean
)

@Serializable
data class OnboardingProgress(
    @SerialName("questions_answered") val questionsAnswered: Int,
    @SerialName("current_question_id") val currentQuestionId: String? = null
)

@Serializable
data class AnswerRequest(
    @SerialName("session_id") val sessionId: String,
    @SerialName("question_id") val questionId: String,
    val answer: @Contextual Any
)

@Serializable
data class ResumeRequest(val session_id: String?)


// --- RECOMMENDATIONS --- //

@Serializable
data class PersonalizedRecommendations(
    val destinations: List<Destination>? = null,
    val offers: List<Offer>? = null,
    val activities: List<Activity>? = null,
    @SerialName("generated_at") val generatedAt: String,
    @SerialName("preferences_used") val preferencesUsed: Map<String, String>
)

@Serializable
data class Destination(
    val id: String,
    val name: String,
    @SerialName("image_url") val imageUrl: String? = null,
    @SerialName("match_score") val matchScore: Double,
    val reason: String,
    val highlights: List<String>,
    @SerialName("estimated_cost") val estimatedCost: EstimatedCost
)

@Serializable
data class EstimatedCost(
    val flight: Double,
    @SerialName("hotel_per_night") val hotelPerNight: Double,
    val currency: String = "USD"
)

@Serializable
data class Activity(
    val id: String,
    val name: String,
    val type: String,
    val destination: String,
    val price: Double,
    @SerialName("match_score") val matchScore: Double,
    val reason: String
)

// --- BOOKING & OFFERS --- //

@Serializable
data class Offer(
    val id: String,
    val type: String,
    val destination: String,
    val price: Double
)

@Serializable
data class OfferComparison(
    @SerialName("offer_id") val offerId: String,
    @SerialName("base_price") val basePrice: Double,
    val taxes: Double,
    val baggage: Double,
    @SerialName("service_fees") val serviceFees: Double,
    val total: Double
)

@Serializable
data class Booking(
    @SerialName("_id") val id: String,
    @SerialName("user_id") val userId: String,
    @SerialName("offer_id") val offerId: String,
    val status: String, 
    @SerialName("payment_details") val paymentDetails: Map<String, String>,
    @SerialName("booking_date") val bookingDate: String,
    @SerialName("confirmation_number") val confirmationNumber: String,
    @SerialName("total_price") val totalPrice: Double,
    @SerialName("createdAt") val createdAt: String,
    @SerialName("updatedAt") val updatedAt: String
)

@Serializable
data class Payment(
    @SerialName("_id") val id: String,
    @SerialName("transaction_id") val transactionId: String,
    @SerialName("user_id") val userId: String,
    val amount: Double,
    @SerialName("payment_status") val paymentStatus: String,
    @SerialName("transaction_date") val transactionDate: String,
    @SerialName("payment_method") val paymentMethod: String,
    @SerialName("createdAt") val createdAt: String,
    @SerialName("updatedAt") val updatedAt: String
)
