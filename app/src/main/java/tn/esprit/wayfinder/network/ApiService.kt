package tn.esprit.wayfinder.network

import kotlinx.serialization.json.JsonElement
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Query
import tn.esprit.wayfinder.models.*

interface ApiService {

    // --- AUTH --- //
    @POST("auth/register")
    suspend fun register(@Body request: SignUpRequest): SignUpResponse

    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): LoginResponse

    // --- USER --- //
    @GET("user/profile")
    suspend fun getProfile(): User

    @PUT("user/profile")
    suspend fun updateProfile(@Body request: UpdateProfileRequest): User

    // --- BOOKING --- //
    @GET("booking/offers")
    suspend fun getOffers(
        @Query("destination") destination: String?,
        @Query("dates") dates: String?,
        @Query("type") type: String?
    ): List<Offer>

    @GET("booking/compare")
    suspend fun getOfferComparison(@Query("offer_id") offerId: String): OfferComparison

    @POST("booking/confirm")
    suspend fun confirmBooking(@Body request: Map<String, Any>): Booking

    @GET("booking/history")
    suspend fun getBookingHistory(): List<Booking>

    // --- PAYMENT --- //
    @GET("payment/history")
    suspend fun getPaymentHistory(): List<Payment>

    @POST("payment/record")
    suspend fun recordPayment(@Body request: Map<String, Any>): Payment

    // --- ONBOARDING --- //
    @POST("onboarding/start")
    suspend fun startOnboarding(): OnboardingResponse

    @POST("onboarding/answer")
    suspend fun submitAnswer(@Body answer: AnswerRequest): OnboardingResponse

    @GET("onboarding/status")
    suspend fun getOnboardingStatus(): OnboardingStatus

    @POST("onboarding/resume")
    suspend fun resumeOnboarding(@Body request: ResumeRequest): OnboardingResponse

    // --- RECOMMENDATIONS --- //
    @GET("recommendations/personalized")
    suspend fun getPersonalizedRecommendations(
        @Query("type") type: String = "all",
        @Query("limit") limit: Int = 10
    ): PersonalizedRecommendations

    @GET("recommendations/regenerate")
    suspend fun regenerateRecommendations(): PersonalizedRecommendations

    // --- CATALOG --- //
    @GET("catalog/recommended")
    suspend fun getRecommendedFlights(
        @Query("originLocationCode") originLocationCode: String? = null,
        @Query("destinationLocationCode") destinationLocationCode: String? = null,
        @Query("departureDate") departureDate: String? = null,
        @Query("returnDate") returnDate: String? = null,
        @Query("adults") adults: Int? = null,
        @Query("travelClass") travelClass: String? = null,
        @Query("currencyCode") currencyCode: String? = null,
        @Query("maxResults") maxResults: Int? = null,
        @Query("maxPrice") maxPrice: Double? = null
    ): RecommendedFlightsResponse

    @GET("catalog/explore")
    suspend fun getExploreOffers(
        @Query("origin") origin: String? = null,
        @Query("destination") destination: String? = null,
        @Query("dateFrom") dateFrom: String? = null,
        @Query("dateTo") dateTo: String? = null,
        @Query("budget") budget: Int? = null,
        @Query("limit") limit: Int? = null
    ): ExploreOffersResponse

    @GET("catalog/activities")
    suspend fun getActivities(
        @Query("city") city: String? = null,
        @Query("themes") themes: String? = null,
        @Query("limit") limit: Int? = null,
        @Query("radiusMeters") radiusMeters: Int? = null
    ): JsonElement
}
