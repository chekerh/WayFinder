package tn.esprit.wayfinder.network

import kotlinx.serialization.json.JsonElement
import okhttp3.MultipartBody
import retrofit2.http.*
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

    @Multipart
    @POST("user/profile/upload-image")
    suspend fun uploadProfileImage(@Part image: MultipartBody.Part): UploadProfileImageResponse

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

    @GET("booking")
    suspend fun getBookings(): List<Booking>

    @GET("booking/{id}")
    suspend fun getBookingById(@Path("id") id: String): Booking

    @POST("booking")
    suspend fun createBooking(@Body request: CreateBookingRequest): Booking

    @PUT("booking/{id}")
    suspend fun updateBooking(@Path("id") id: String, @Body request: UpdateBookingRequest): Booking

    @DELETE("booking/{id}")
    suspend fun cancelBooking(@Path("id") id: String): Booking

    // --- PAYMENT --- //
    @GET("payment/history")
    suspend fun getPaymentHistory(): List<Payment>

    @POST("payment/record")
    suspend fun recordPayment(@Body request: Map<String, Any>): Payment

    // --- FLOUCI PAYMENT --- //
    @POST("payment/flouci/create")
    suspend fun createFlouciPayment(@Body request: FlouciPaymentRequest): FlouciPaymentResponse

    @GET("payment/flouci/status/{paymentId}")
    suspend fun getFlouciPaymentStatus(@retrofit2.http.Path("paymentId") paymentId: String): FlouciPaymentStatusResponse

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
    ): ActivityFeedResponse

    // --- DISCUSSION --- //
    @GET("discussion/posts")
    suspend fun getPosts(
        @Query("limit") limit: Int? = null,
        @Query("skip") skip: Int? = null,
        @Query("destination") destination: String? = null
    ): PostsResponse

    @GET("discussion/posts/{id}")
    suspend fun getPost(@Path("id") id: String): DiscussionPost

    @POST("discussion/posts")
    suspend fun createPost(@Body request: CreatePostRequest): DiscussionPost

    @PUT("discussion/posts/{id}")
    suspend fun updatePost(@Path("id") id: String, @Body request: CreatePostRequest): DiscussionPost

    @DELETE("discussion/posts/{id}")
    suspend fun deletePost(@Path("id") id: String): Map<String, String>

    @POST("discussion/posts/{id}/like")
    suspend fun likePost(@Path("id") id: String): DiscussionPost

    @GET("discussion/posts/{id}/comments")
    suspend fun getComments(
        @Path("id") postId: String,
        @Query("limit") limit: Int? = null,
        @Query("skip") skip: Int? = null
    ): CommentsResponse

    @POST("discussion/posts/{id}/comments")
    suspend fun createComment(@Path("id") postId: String, @Body request: CreateCommentRequest): DiscussionComment

    @POST("discussion/comments/{id}/like")
    suspend fun likeComment(@Path("id") commentId: String): DiscussionComment

    @DELETE("discussion/comments/{id}")
    suspend fun deleteComment(@Path("id") commentId: String): Map<String, String>

    // --- FAVORITES --- //
    @GET("favorites")
    suspend fun getFavorites(@Query("type") type: String? = null): List<Favorite>

    @GET("favorites/count")
    suspend fun getFavoriteCount(@Query("type") type: String? = null): FavoriteCountResponse

    @GET("favorites/check/{type}/{id}")
    suspend fun checkFavorite(@Path("type") type: String, @Path("id") id: String): FavoriteCheckResponse

    @POST("favorites")
    suspend fun addFavorite(@Body request: CreateFavoriteRequest): Favorite

    @DELETE("favorites/{type}/{id}")
    suspend fun removeFavorite(@Path("type") type: String, @Path("id") id: String): Map<String, String>

    // --- REVIEWS --- //
    @GET("reviews/{itemType}/{itemId}")
    suspend fun getReviews(@Path("itemType") itemType: String, @Path("itemId") itemId: String): List<Review>

    @GET("reviews/{itemType}/{itemId}/stats")
    suspend fun getReviewStats(@Path("itemType") itemType: String, @Path("itemId") itemId: String): ReviewStatsResponse

    @GET("reviews/check/{itemType}/{itemId}")
    suspend fun checkUserReview(@Path("itemType") itemType: String, @Path("itemId") itemId: String): Review?

    @GET("reviews/user/my-reviews")
    suspend fun getUserReviews(@Query("type") itemType: String? = null): List<Review>

    @POST("reviews")
    suspend fun createReview(@Body request: CreateReviewRequest): Review

    @PUT("reviews/{id}")
    suspend fun updateReview(@Path("id") reviewId: String, @Body request: UpdateReviewRequest): Review

    @DELETE("reviews/{id}")
    suspend fun deleteReview(@Path("id") reviewId: String): Map<String, String>

    // --- ITINERARY --- //
    @GET("itinerary")
    suspend fun getItineraries(@Query("includePublic") includePublic: Boolean = false): List<Itinerary>

    @GET("itinerary/{id}")
    suspend fun getItinerary(@Path("id") id: String): Itinerary

    @POST("itinerary")
    suspend fun createItinerary(@Body request: CreateItineraryRequest): Itinerary

    @PUT("itinerary/{id}")
    suspend fun updateItinerary(@Path("id") id: String, @Body request: UpdateItineraryRequest): Itinerary

    @DELETE("itinerary/{id}")
    suspend fun deleteItinerary(@Path("id") id: String): Map<String, String>

    @POST("itinerary/{id}/days/{dayDate}/activities")
    suspend fun addActivity(
        @Path("id") id: String,
        @Path("dayDate") dayDate: String,
        @Body activity: AddActivityRequest
    ): Itinerary

    @DELETE("itinerary/{id}/days/{dayDate}/activities/{activityIndex}")
    suspend fun removeActivity(
        @Path("id") id: String,
        @Path("dayDate") dayDate: String,
        @Path("activityIndex") activityIndex: Int
    ): Itinerary

    // --- NOTIFICATIONS --- //
    @GET("notifications")
    suspend fun getNotifications(@Query("unreadOnly") unreadOnly: Boolean = false): List<Notification>

    @GET("notifications/unread-count")
    suspend fun getUnreadCount(): UnreadCountResponse

    @POST("notifications")
    suspend fun createNotification(@Body request: CreateNotificationRequest): Notification

    @PUT("notifications/{id}/read")
    suspend fun markAsRead(@Path("id") id: String): Notification

    @PUT("notifications/read-all")
    suspend fun markAllAsRead(): Map<String, String>

    @DELETE("notifications/{id}")
    suspend fun deleteNotification(@Path("id") id: String): Map<String, String>

    // --- SOCIAL --- //
    @POST("social/follow")
    suspend fun followUser(@Body request: FollowUserRequest): FollowResponse

    @POST("social/unfollow")
    suspend fun unfollowUser(@Body request: FollowUserRequest): FollowResponse

    @GET("social/follow-status/{userId}")
    suspend fun checkFollowStatus(@Path("userId") userId: String): FollowStatusResponse

    @GET("social/followers")
    suspend fun getFollowers(@Query("limit") limit: Int = 50, @Query("skip") skip: Int = 0): List<UserPreview>

    @GET("social/following")
    suspend fun getFollowing(@Query("limit") limit: Int = 50, @Query("skip") skip: Int = 0): List<UserPreview>

    @GET("social/follow-counts")
    suspend fun getFollowCounts(): FollowCountsResponse

    @GET("social/follow-counts/{userId}")
    suspend fun getFollowCountsByUserId(@Path("userId") userId: String): FollowCountsResponse

    @POST("social/share-trip")
    suspend fun shareTrip(@Body request: ShareTripRequest): SharedTrip

    @PUT("social/share-trip/{id}")
    suspend fun updateSharedTrip(@Path("id") id: String, @Body request: UpdateSharedTripRequest): SharedTrip

    @DELETE("social/share-trip/{id}")
    suspend fun deleteSharedTrip(@Path("id") id: String): Map<String, String>

    @GET("social/share-trip/{id}")
    suspend fun getSharedTrip(@Path("id") id: String): SharedTrip

    @GET("social/user/{userId}/shared-trips")
    suspend fun getUserSharedTrips(@Path("userId") userId: String, @Query("limit") limit: Int = 20, @Query("skip") skip: Int = 0): List<SharedTrip>

    @GET("social/feed")
    suspend fun getSocialFeed(@Query("limit") limit: Int = 20, @Query("skip") skip: Int = 0): List<SharedTrip>

    @POST("social/share-trip/{id}/like")
    suspend fun likeSharedTrip(@Path("id") id: String): LikeResponse

    // --- SEARCH HISTORY --- //
    @POST("search-history")
    suspend fun recordSearch(@Body request: CreateSearchHistoryRequest): SearchHistory

    @GET("search-history/recent")
    suspend fun getRecentSearches(@Query("type") searchType: String? = null, @Query("limit") limit: Int = 20, @Query("skip") skip: Int = 0): List<SearchHistory>

    @GET("search-history/saved")
    suspend fun getSavedSearches(@Query("type") searchType: String? = null, @Query("limit") limit: Int = 50, @Query("skip") skip: Int = 0): List<SearchHistory>

    @POST("search-history/{id}/save")
    suspend fun saveSearch(@Path("id") id: String, @Body request: SaveSearchRequest): SearchHistory

    @POST("search-history/{id}/unsave")
    suspend fun unsaveSearch(@Path("id") id: String): SearchHistory

    @DELETE("search-history/{id}")
    suspend fun deleteSearchHistory(@Path("id") id: String): Map<String, String>

    @DELETE("search-history/recent/clear")
    suspend fun clearRecentSearches(@Query("type") searchType: String? = null): Map<String, String>

    @GET("search-history/stats")
    suspend fun getSearchStats(): SearchStatsResponse

    // --- PRICE ALERTS --- //
    @POST("price-alerts")
    suspend fun createPriceAlert(@Body request: CreatePriceAlertRequest): PriceAlert

    @GET("price-alerts")
    suspend fun getPriceAlerts(@Query("activeOnly") activeOnly: Boolean = false): List<PriceAlert>

    @GET("price-alerts/{id}")
    suspend fun getPriceAlert(@Path("id") id: String): PriceAlert

    @PUT("price-alerts/{id}")
    suspend fun updatePriceAlert(@Path("id") id: String, @Body request: UpdatePriceAlertRequest): PriceAlert

    @DELETE("price-alerts/{id}")
    suspend fun deletePriceAlert(@Path("id") id: String): Map<String, String>

    @POST("price-alerts/{id}/deactivate")
    suspend fun deactivatePriceAlert(@Path("id") id: String): PriceAlert

    // --- TRAVEL TIPS --- //
    @GET("travel-tips")
    suspend fun getTravelTips(
        @Query("destinationId") destinationId: String,
        @Query("category") category: String? = null,
        @Query("limit") limit: Int = 10
    ): List<TravelTip>

    @GET("travel-tips/generate/{destinationId}")
    suspend fun generateTravelTips(
        @Path("destinationId") destinationId: String,
        @Query("destinationName") destinationName: String,
        @Query("city") city: String? = null,
        @Query("country") country: String? = null
    ): List<TravelTip>

    @POST("travel-tips")
    suspend fun createTravelTip(@Body request: CreateTravelTipRequest): TravelTip

    @POST("travel-tips/{tipId}/helpful")
    suspend fun markTipHelpful(@Path("tipId") tipId: String): TravelTip

    @GET("travel-tips/{tipId}")
    suspend fun getTravelTipById(@Path("tipId") tipId: String): TravelTip
}
